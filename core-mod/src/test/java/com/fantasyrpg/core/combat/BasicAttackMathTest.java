package com.fantasyrpg.core.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Better Combat 연동 실험용 기본 공격 입력 정리. */
class BasicAttackMathTest {

    private static final double EPS = 1e-12;

    @Test
    @DisplayName("콤보 배율: 정상값 유지, NaN·무한·0 이하는 1.0, 상한 10")
    void combo() {
        assertEquals(1.2, BasicAttackMath.sanitizeCombo(1.2), EPS);
        assertEquals(0.8, BasicAttackMath.sanitizeCombo(0.8), EPS);
        assertEquals(1.0, BasicAttackMath.sanitizeCombo(Double.NaN), EPS);
        assertEquals(1.0, BasicAttackMath.sanitizeCombo(Double.POSITIVE_INFINITY), EPS);
        assertEquals(1.0, BasicAttackMath.sanitizeCombo(0.0), EPS);
        assertEquals(1.0, BasicAttackMath.sanitizeCombo(-2.0), EPS);
        assertEquals(10.0, BasicAttackMath.sanitizeCombo(50.0), EPS);
    }

    @Test
    @DisplayName("충전 배율: 바닐라 0.2 + 0.8c² (0 → 0.2, 0.5 → 0.4, 1 → 1.0), 범위 밖은 자름")
    void charge() {
        assertEquals(0.2, BasicAttackMath.chargeScale(0.0), EPS);
        assertEquals(0.4, BasicAttackMath.chargeScale(0.5), EPS);
        assertEquals(1.0, BasicAttackMath.chargeScale(1.0), EPS);
        assertEquals(1.0, BasicAttackMath.chargeScale(3.0), EPS);
        assertEquals(0.2, BasicAttackMath.chargeScale(-1.0), EPS);
        assertEquals(1.0, BasicAttackMath.chargeScale(Double.NaN), EPS);
    }

    @Test
    @DisplayName("기본 공격 계수 = 기본 계수 × 콤보 × 충전, 비정상 기본 계수는 0")
    void coefficient() {
        assertEquals(1.0 * 1.3 * 1.0, BasicAttackMath.basicCoefficient(1.0, 1.3, 1.0), EPS);
        assertEquals(0.8 * 1.0 * 0.4, BasicAttackMath.basicCoefficient(0.8, Double.NaN, 0.5), EPS);
        assertEquals(0.0, BasicAttackMath.basicCoefficient(-1.0, 1.3, 1.0), EPS);
        assertEquals(0.0, BasicAttackMath.basicCoefficient(Double.NaN, 1.3, 1.0), EPS);
    }
}
