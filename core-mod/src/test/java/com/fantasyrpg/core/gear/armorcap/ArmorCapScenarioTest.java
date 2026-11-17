package com.fantasyrpg.core.gear.armorcap;

import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Mod;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Op;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docs/TESTING.md v0.2 게임 테스트 중 "수치" 부분을 Minecraft 없이 재현한다.
 *
 * <p>게임에서는 플레이어 방어 속성에 장비 수정자가 붙고, ArmorCapService가 최종 곱셈 수정자를 하나 더 붙인 뒤
 * 바닐라 갑옷 공식으로 피해가 줄어든다. 여기서는 같은 순서를 순수 계산으로 따라간다.
 * 바닐라 공식(1.21.1 CombatRules.getDamageAfterAbsorb, 파쇄 인챈트 없음)과 방어력 정수 내림(getArmorValue)은
 * 이 테스트 안에 옮겨 적었다. 이 테스트가 통과해도 게임 안의 틱 갱신·재접속·차원 이동 같은 동작은 검증하지 않는다.
 */
class ArmorCapScenarioTest {

    private static final double EPS = 1e-3;

    // 바닐라 1.21.1 갑옷 수치 (흉갑 기준이 아니라 4부위 합)
    private static final double[] IRON = {2, 6, 5, 2};
    private static final double[] IRON_T = {0, 0, 0, 0};

    /** 바닐라 CombatRules.getDamageAfterAbsorb (1.21.1, 파쇄 0). float 연산을 그대로 따른다. */
    static float vanillaAfterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float f1 = clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - f1 / 25.0F);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static double cap(List<Double> caps, int grade) {
        return ArmorCapDefaults.capFor(caps, grade);
    }

    private static double[] arr(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** 장비 4부위를 ADD_VALUE 수정자로 만든다. */
    private static List<Mod> gear(String name, double[] values) {
        List<Mod> mods = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) mods.add(new Mod(name + i, Op.ADD_VALUE, values[i]));
        }
        return mods;
    }

    /** ArmorCapService.apply 와 같은 순서: plan → (필요하면) 최종 곱셈 수정자 → refine → 결과값. */
    private static double applied(List<Mod> others, double cap, boolean integerFloor, double max) {
        Plan plan = ArmorCapMath.plan(0.0, others, Map.of(), cap, 0.0, max);
        if (!plan.needed()) return ArmorCapMath.clamp(plan.rawPre(), 0.0, max);
        double k = ArmorCapMath.refine(plan.k(), plan.target(), integerFloor, candidate -> {
            List<Mod> withOurs = new ArrayList<>(others);
            withOurs.add(new Mod("fantasyrpg:grade_limit", Op.ADD_MULTIPLIED_TOTAL, candidate));
            return ArmorCapMath.clamp(ArmorCapMath.calc(0.0, withOurs), 0.0, max);
        });
        List<Mod> withOurs = new ArrayList<>(others);
        withOurs.add(new Mod("fantasyrpg:grade_limit", Op.ADD_MULTIPLIED_TOTAL, k));
        double value = ArmorCapMath.clamp(ArmorCapMath.calc(0.0, withOurs), 0.0, max);
        assertTrue(ArmorCapMath.accepted(value, plan.target(), integerFloor), "보정 후 목표 미달: " + value + " / " + plan.target());
        return value;
    }

    /** 플레이어가 /damage 10 mob_attack 를 받았을 때 피해량. 방어력은 바닐라처럼 정수 내림. */
    private static float hit10(List<Mod> armorMods, List<Mod> toughMods, int grade) {
        double armor = applied(armorMods, cap(ArmorCapDefaults.ARMOR_CAPS, grade), true, 30.0);
        double tough = applied(toughMods, cap(ArmorCapDefaults.TOUGHNESS_CAPS, grade), false, 20.0);
        return vanillaAfterArmor(10.0F, (float) Math.floor(armor), (float) tough);
    }

    @Test
    @DisplayName("#1·#2 1급 + 철 풀세트: 방어력 15 → 12, 피해 7.2")
    void grade1Iron() {
        assertEquals(12.0, Math.floor(applied(gear("iron", IRON), cap(ArmorCapDefaults.ARMOR_CAPS, 1), true, 30.0)), EPS);
        assertEquals(7.2, hit10(gear("iron", IRON), gear("iron_t", IRON_T), 1), EPS);
    }

    @Test
    @DisplayName("#3·#4 2급 + 철 풀세트: 보정 없음 15, 피해 6.0")
    void grade2Iron() {
        Plan p = ArmorCapMath.plan(0.0, gear("iron", IRON), Map.of(), cap(ArmorCapDefaults.ARMOR_CAPS, 2), 0.0, 30.0);
        assertFalse(p.needed());
        assertEquals(6.0, hit10(gear("iron", IRON), gear("iron_t", IRON_T), 2), EPS);
    }

    @Test
    @DisplayName("#5 조정된 다이아 흉갑 7/1 (설정 기본값)")
    void diamondChestTuning() {
        assertEquals(7.0, ArmorCapDefaults.DIAMOND_ARMOR.get(1), EPS);
        assertEquals(1.0, ArmorCapDefaults.DIAMOND_TOUGHNESS.get(1), EPS);
    }

    @Test
    @DisplayName("#6 3급 + 조정 다이아 18/2: 보정 없음, 피해 4.4")
    void grade3Diamond() {
        assertEquals(4.4, hit10(gear("dia", arr(ArmorCapDefaults.DIAMOND_ARMOR)), gear("dia_t", arr(ArmorCapDefaults.DIAMOND_TOUGHNESS)), 3), EPS);
    }

    @Test
    @DisplayName("#7 6급 + 네더라이트 20/6: 강도 6 → 4, 피해 3.333")
    void grade6Netherite() {
        double tough = applied(gear("neth_t", arr(ArmorCapDefaults.NETHERITE_TOUGHNESS)), cap(ArmorCapDefaults.TOUGHNESS_CAPS, 6), false, 20.0);
        assertEquals(4.0, tough, EPS);
        assertEquals(3.333, hit10(gear("neth", arr(ArmorCapDefaults.NETHERITE_ARMOR)), gear("neth_t", arr(ArmorCapDefaults.NETHERITE_TOUGHNESS)), 6), EPS);
    }

    @Test
    @DisplayName("#8 7급(덮어쓰기) + 네더라이트: 보정 없음, 피해 3.143")
    void grade7Netherite() {
        assertEquals(3.143, hit10(gear("neth", arr(ArmorCapDefaults.NETHERITE_ARMOR)), gear("neth_t", arr(ArmorCapDefaults.NETHERITE_TOUGHNESS)), 7), EPS);
    }

    @Test
    @DisplayName("#9 1급 + 네더라이트: 12/0, 피해 7.2")
    void grade1Netherite() {
        assertEquals(7.2, hit10(gear("neth", arr(ArmorCapDefaults.NETHERITE_ARMOR)), gear("neth_t", arr(ArmorCapDefaults.NETHERITE_TOUGHNESS)), 1), EPS);
    }

    @Test
    @DisplayName("#10 1급 + 철 + 장신구 +3: 원래 18 → 12 (장신구도 상한에 포함)")
    void grade1IronRing() {
        List<Mod> mods = gear("iron", IRON);
        mods.add(new Mod("fantasyrpg:test_ring", Op.ADD_VALUE, 3.0));
        Plan p = ArmorCapMath.plan(0.0, mods, Map.of(), cap(ArmorCapDefaults.ARMOR_CAPS, 1), 0.0, 30.0);
        assertEquals(18.0, p.rawPre(), EPS);
        assertEquals(12.0, Math.floor(applied(mods, cap(ArmorCapDefaults.ARMOR_CAPS, 1), true, 30.0)), EPS);
    }

    @Test
    @DisplayName("#11 1급 + 철 + 최종 ×1.5: 원래 22.5 → 12, 피해 7.2")
    void grade1IronMultiplier() {
        List<Mod> mods = gear("iron", IRON);
        mods.add(new Mod("fantasyrpg:test_mult", Op.ADD_MULTIPLIED_TOTAL, 0.5));
        Plan p = ArmorCapMath.plan(0.0, mods, Map.of(), cap(ArmorCapDefaults.ARMOR_CAPS, 1), 0.0, 30.0);
        assertEquals(22.5, p.rawPre(), EPS);
        assertEquals(7.2, hit10(mods, gear("iron_t", IRON_T), 1), EPS);
    }

    @Test
    @DisplayName("#12 같은 틱 +10 후 피해: 상한 적용 시 7.2, 늦으면 2.0 (두 값 모두 계산으로 확인)")
    void sameTick() {
        List<Mod> mods = gear("iron", IRON);
        mods.add(new Mod("fantasyrpg:test_same_tick", Op.ADD_VALUE, 10.0));
        assertEquals(7.2, hit10(mods, gear("iron_t", IRON_T), 1), EPS);
        assertEquals(2.0, vanillaAfterArmor(10.0F, 25.0F, 0.0F), EPS); // 갱신 실패 시 기대값(문서 근거)
    }

    @Test
    @DisplayName("#17 맨몸: 보정 없음")
    void naked() {
        Plan p = ArmorCapMath.plan(0.0, List.of(), Map.of(), cap(ArmorCapDefaults.ARMOR_CAPS, 1), 0.0, 30.0);
        assertFalse(p.needed());
        assertEquals(0.0, p.target(), EPS);
    }
}
