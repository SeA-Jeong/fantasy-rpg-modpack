package com.fantasyrpg.core.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 기본 공격 수치 (데이터 {@code data/fantasyrpg/combat/basic_attack.json}). Minecraft 비의존.
 * 기준: 프로젝트 문서 claude/전투연결_설계.md 3절. 치명타 5%·150%는 사용자 확정, 나머지는 [초안].
 *
 * @param weaponPower 등급 1~10의 무기 위력 W (모든 직업 공통)
 * @param critChance 기본 치명타 확률 (0~1)
 * @param critMultiplier 치명타 배율
 * @param unarmedDamage 맨손 고정 피해
 * @param classes 직업 id → 기본 공격
 */
public record CombatSettings(double[] weaponPower, double critChance, double critMultiplier, double unarmedDamage,
                             Map<String, ClassAttack> classes) {

    /**
     * 직업 기본 공격.
     *
     * @param physical 물리 계수
     * @param magic 마법 계수
     * @param attackSpeed 초당 공격 횟수 (근접 무기 속성, 활은 0)
     * @param drawSeconds 활을 완전히 당기는 시간 (활만, 그 외 0)
     * @param bolt 지팡이 마법 탄 (마법사만, 없으면 null)
     */
    public record ClassAttack(double physical, double magic, double attackSpeed, double drawSeconds, Bolt bolt) {
        /** 완충 기준 초당 피해 (W 1당) */
        public double dpsPerW() {
            if (bolt != null) return (bolt.physical() + bolt.magic()) * 20.0 / bolt.cooldownTicks();
            if (drawSeconds > 0) return (physical + magic) / drawSeconds;
            return (physical + magic) * attackSpeed;
        }
    }

    /** 지팡이 마법 탄: 계수, 사거리(칸), 재사용 대기(틱) */
    public record Bolt(double physical, double magic, double range, int cooldownTicks) {
    }

    public CombatSettings {
        weaponPower = weaponPower.clone();
        classes = Map.copyOf(classes);
    }

    /** 등급의 W. 범위 밖은 1~10으로 맞춤 */
    public double power(int tier) {
        int t = Math.max(1, Math.min(weaponPower.length, tier));
        return weaponPower[t - 1];
    }

    public ClassAttack forClass(String classId) {
        return classes.get(classId);
    }

    public static CombatSettings parse(JsonObject json) {
        JsonArray arr = json.getAsJsonArray("weapon_power");
        if (arr == null || arr.size() != 10) throw new IllegalArgumentException("weapon_power는 1~10급 10개");
        double[] w = new double[10];
        for (int i = 0; i < 10; i++) {
            w[i] = arr.get(i).getAsDouble();
            if (!(w[i] > 0)) throw new IllegalArgumentException((i + 1) + "급 W는 0보다 커야 함");
            if (i > 0 && w[i] < w[i - 1]) throw new IllegalArgumentException((i + 1) + "급 W가 " + i + "급보다 작음");
        }
        JsonObject crit = json.getAsJsonObject("crit");
        double chance = crit.get("chance").getAsDouble();
        double mult = crit.get("multiplier").getAsDouble();
        if (chance < 0 || chance > 1 || mult < 1) throw new IllegalArgumentException("치명타 확률 0~1, 배율 1 이상");
        double unarmed = json.has("unarmed_damage") ? json.get("unarmed_damage").getAsDouble() : 1.0;

        Map<String, ClassAttack> classes = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("classes").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            Bolt bolt = null;
            if (c.has("bolt")) {
                JsonObject b = c.getAsJsonObject("bolt");
                bolt = new Bolt(num(b, "physical"), num(b, "magic"), num(b, "range"), b.get("cooldown_ticks").getAsInt());
                if (bolt.range() <= 0 || bolt.cooldownTicks() <= 0) throw new IllegalArgumentException(e.getKey() + " bolt 사거리·대기 > 0");
            }
            ClassAttack a = new ClassAttack(num(c, "physical"), num(c, "magic"), num(c, "attack_speed"), num(c, "draw_seconds"), bolt);
            if (a.physical() < 0 || a.magic() < 0) throw new IllegalArgumentException(e.getKey() + " 계수는 0 이상");
            classes.put(e.getKey(), a);
        }
        return new CombatSettings(w, chance, mult, unarmed, classes);
    }

    private static double num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsDouble() : 0.0;
    }
}
