package com.fantasyrpg.core.gear.armor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * armor_stats JSON → {@link ArmorStatTable}. 검사에 걸리면 {@link IllegalArgumentException}. Minecraft 비의존.
 *
 * <pre>
 * {
 *   "family": "knight",
 *   "class": "heavy",
 *   "tiers": [ {"tier": 1, "armor": 13, "toughness": 1, "knockback": 0.0}, ... 10급까지 ]
 * }
 * </pre>
 * 검사: 1~10급이 모두 한 번씩, 방어력 0~30, 강도 0~20, 넉백 저항 0~1(0.01 단위),
 * 등급이 오를 때 세트 값과 부위 값이 줄지 않음.
 */
public final class ArmorStatParser {
    private ArmorStatParser() {
    }

    public static ArmorStatTable parse(JsonObject json) {
        String family = requireString(json, "family");
        String armorClass = json.has("class") ? json.get("class").getAsString() : "";
        if (!json.has("tiers") || !json.get("tiers").isJsonArray()) {
            throw new IllegalArgumentException("'tiers' 배열이 없습니다");
        }

        ArmorStatTable.Tier[] byTier = new ArmorStatTable.Tier[ArmorStatTable.MAX_TIER];
        for (JsonElement e : json.getAsJsonArray("tiers")) {
            JsonObject o = e.getAsJsonObject();
            int tier = requireInt(o, "tier");
            if (tier < ArmorStatTable.MIN_TIER || tier > ArmorStatTable.MAX_TIER) {
                throw new IllegalArgumentException("등급 " + tier + "은(는) 1~10 밖입니다");
            }
            if (byTier[tier - 1] != null) {
                throw new IllegalArgumentException(tier + "급이 두 번 있습니다");
            }
            int armor = requireInt(o, "armor");
            int toughness = requireInt(o, "toughness");
            double knockback = o.has("knockback") ? o.get("knockback").getAsDouble() : 0.0;
            if (armor < 0 || armor > ArmorStatTable.MAX_ARMOR) {
                throw new IllegalArgumentException(tier + "급 방어력 " + armor + "은(는) 0~30 밖입니다");
            }
            if (toughness < 0 || toughness > ArmorStatTable.MAX_TOUGHNESS) {
                throw new IllegalArgumentException(tier + "급 방어 강도 " + toughness + "은(는) 0~20 밖입니다");
            }
            if (!(knockback >= 0.0 && knockback <= 1.0)) {
                throw new IllegalArgumentException(tier + "급 넉백 저항 " + knockback + "은(는) 0~1 밖입니다");
            }
            if (Math.abs(knockback * 100 - Math.round(knockback * 100)) > 1e-9) {
                throw new IllegalArgumentException(tier + "급 넉백 저항 " + knockback + "은(는) 0.01 단위가 아닙니다");
            }
            byTier[tier - 1] = new ArmorStatTable.Tier(tier, armor, toughness, knockback);
        }

        List<ArmorStatTable.Tier> tiers = new ArrayList<>();
        for (int i = 0; i < byTier.length; i++) {
            if (byTier[i] == null) throw new IllegalArgumentException((i + 1) + "급이 없습니다");
            tiers.add(byTier[i]);
        }
        ArmorStatTable table = new ArmorStatTable(family, armorClass, tiers);
        checkMonotonic(table);
        return table;
    }

    /** 등급이 오를 때 세트·부위 값이 줄면 예외. */
    static void checkMonotonic(ArmorStatTable table) {
        for (int t = ArmorStatTable.MIN_TIER + 1; t <= ArmorStatTable.MAX_TIER; t++) {
            for (ArmorPart part : ArmorPart.values()) {
                if (table.partArmor(t, part) < table.partArmor(t - 1, part)
                        || table.partToughness(t, part) < table.partToughness(t - 1, part)
                        || table.partKnockback(t, part) < table.partKnockback(t - 1, part)) {
                    throw new IllegalArgumentException(t + "급 " + part + " 값이 " + (t - 1) + "급보다 작습니다");
                }
            }
        }
    }

    private static String requireString(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("'" + key + "' 필드가 없습니다");
        return o.get(key).getAsString().trim();
    }

    private static int requireInt(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("'" + key + "' 필드가 없습니다");
        double v = o.get(key).getAsDouble();
        if (v != Math.rint(v)) throw new IllegalArgumentException("'" + key + "' 값 " + v + "은(는) 정수가 아닙니다");
        return (int) v;
    }
}
