package com.fantasyrpg.core.gear.vanilla;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 바닐라 방어구 조정값과, 그 값으로 받는 피해를 Minecraft 없이 계산한다 (docs/TESTING.md v0.2-r).
 * 2026-11-18 방어 등급 상한을 없앴으므로 입은 갑옷 수치가 그대로 적용된다.
 * 바닐라 공식(1.21.1 CombatRules.getDamageAfterAbsorb, 파쇄 인챈트 없음)과 방어력 정수 내림은 테스트 안에 옮겨 적었다.
 */
class VanillaArmorDamageTest {
    private static final double EPS = 1e-3;

    /** 바닐라 CombatRules.getDamageAfterAbsorb (1.21.1, 파쇄 0). float 연산을 그대로 따른다. */
    static float afterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float f1 = Math.max(armor * 0.2F, Math.min(armor - damage / f, 20.0F));
        return damage * (1.0F - f1 / 25.0F);
    }

    private static double sum(List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).sum();
    }

    /** 플레이어가 /damage 10 mob_attack 를 받았을 때 피해량. 방어력은 바닐라처럼 정수 내림 */
    private static float hit10(double armor, double toughness) {
        return afterArmor(10.0F, (float) Math.floor(armor), (float) toughness);
    }

    @Test
    @DisplayName("조정값 합계: 다이아 18/2, 네더라이트 20/6 (부위 3:8:6:3 배분)")
    void defaults() {
        assertEquals(18.0, sum(VanillaArmorDefaults.DIAMOND_ARMOR));
        assertEquals(2.0, sum(VanillaArmorDefaults.DIAMOND_TOUGHNESS));
        assertEquals(20.0, sum(VanillaArmorDefaults.NETHERITE_ARMOR));
        assertEquals(6.0, sum(VanillaArmorDefaults.NETHERITE_TOUGHNESS));
        assertEquals(List.of(3.0, 7.0, 5.0, 3.0), VanillaArmorDefaults.DIAMOND_ARMOR);
        assertEquals(List.of(0.0, 1.0, 1.0, 0.0), VanillaArmorDefaults.DIAMOND_TOUGHNESS);
    }

    @Test
    @DisplayName("상한 없음: 피해 10 → 맨몸 10, 가죽 9.2, 철 6.0, 다이아 4.4, 네더라이트 3.143 (진행 등급과 무관)")
    void damageWithoutCap() {
        assertEquals(10.0, hit10(0, 0), EPS);
        assertEquals(9.2, hit10(7, 0), EPS);
        assertEquals(7.2, hit10(12, 0), EPS);
        assertEquals(6.0, hit10(15, 0), EPS);
        assertEquals(4.4, hit10(sum(VanillaArmorDefaults.DIAMOND_ARMOR), sum(VanillaArmorDefaults.DIAMOND_TOUGHNESS)), EPS);
        assertEquals(3.142857, hit10(sum(VanillaArmorDefaults.NETHERITE_ARMOR), sum(VanillaArmorDefaults.NETHERITE_TOUGHNESS)), EPS);
    }

    @Test
    @DisplayName("게임 안 확인(2026-11-18)과 같은 공식: 15/0 → 6.0, 18/2 → 4.4, 20/4 → 3.333, 12/0 → 7.2")
    void matchesObserved() {
        // 상한이 있던 v0.2 게임 확인에서 실제로 관찰한 남은 체력 14.0·15.6·16.666666·12.799999 와 같은 공식
        assertEquals(6.0, hit10(15, 0), EPS);
        assertEquals(4.4, hit10(18, 2), EPS);
        assertEquals(3.333, hit10(20, 4), EPS);
        assertEquals(7.2, hit10(12, 0), EPS);
    }
}
