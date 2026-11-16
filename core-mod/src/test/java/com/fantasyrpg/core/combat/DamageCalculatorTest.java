package com.fantasyrpg.core.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageCalculatorTest {
    private static final double EPS = 1e-9;

    @Nested
    @DisplayName("부록 A-1. 관통·저항 산술 검증")
    class AppendixA1 {
        @Test
        @DisplayName("W=100, Cp=2.0, Rp=25%, Pp=0 → 150")
        void physicalWithResist() {
            assertEquals(150.0, DamageCalculator.damage(
                    DamageInput.of(100, 2.0, 0), new Resistances(0.25, 0)), EPS);
        }

        @Test
        @DisplayName("W=100, Cp=2.0, Rp=25%, Pp=10%p → 170")
        void physicalWithPenetration() {
            assertEquals(170.0, DamageCalculator.damage(
                    DamageInput.of(100, 2.0, 0).withPenetration(0.10, 0), new Resistances(0.25, 0)), EPS);
        }

        @Test
        @DisplayName("W=100, Cm=2.0, Rm=15%, Pm=0 → 170")
        void magicWithResist() {
            assertEquals(170.0, DamageCalculator.damage(
                    DamageInput.of(100, 0, 2.0), new Resistances(0, 0.15)), EPS);
        }

        @Test
        @DisplayName("W=100, Cp=Cm=1.0, Rp=25%, Rm=15% → 160")
        void hybrid() {
            assertEquals(160.0, DamageCalculator.damage(
                    DamageInput.of(100, 1.0, 1.0), new Resistances(0.25, 0.15)), EPS);
        }

        @Test
        @DisplayName("W=100, Cp=2.0, Rp=10%, Pp=20%p → 실효 저항 0 → 200")
        void penetrationAboveResistDoesNotAmplify() {
            DamageBreakdown b = DamageCalculator.calculate(
                    DamageInput.of(100, 2.0, 0).withPenetration(0.20, 0), new Resistances(0.10, 0));
            assertEquals(0.0, b.effectivePhysicalResist(), EPS);
            assertEquals(200.0, b.total(), EPS);
        }
    }

    @Nested
    @DisplayName("4.3장 계산 예시 표")
    class Section43 {
        @Test
        void resist25and15() {
            Resistances r = new Resistances(0.25, 0.15);
            assertEquals(150.0, DamageCalculator.damage(DamageInput.of(100, 2.0, 0), r), EPS);
            assertEquals(160.0, DamageCalculator.damage(DamageInput.of(100, 1.0, 1.0), r), EPS);
            assertEquals(170.0, DamageCalculator.damage(DamageInput.of(100, 0, 2.0), r), EPS);
        }

        @Test
        void resist40and20() {
            Resistances r = new Resistances(0.40, 0.20);
            assertEquals(120.0, DamageCalculator.damage(DamageInput.of(100, 2.0, 0), r), EPS);
            assertEquals(140.0, DamageCalculator.damage(DamageInput.of(100, 1.0, 1.0), r), EPS);
            assertEquals(160.0, DamageCalculator.damage(DamageInput.of(100, 0, 2.0), r), EPS);
        }

        @Test
        @DisplayName("물리 관통 10%p는 모든 기술의 물리 성분에만 적용 → 170/170/170")
        void physicalPenetrationEqualizes() {
            Resistances r = new Resistances(0.25, 0.15);
            assertEquals(170.0, DamageCalculator.damage(DamageInput.of(100, 2.0, 0).withPenetration(0.10, 0), r), EPS);
            assertEquals(170.0, DamageCalculator.damage(DamageInput.of(100, 1.0, 1.0).withPenetration(0.10, 0), r), EPS);
            assertEquals(170.0, DamageCalculator.damage(DamageInput.of(100, 0, 2.0).withPenetration(0.10, 0), r), EPS);
        }
    }

    @Nested
    @DisplayName("4.1장 계산 순서")
    class Order {
        @Test
        @DisplayName("스킬 강화는 성분별로 저항 전에 곱한다")
        void bonusAppliesPerComponentBeforeResist() {
            // Dp = 100×1×1.2×0.75 = 90, Dm = 100×1×1.0×0.85 = 85
            DamageBreakdown b = DamageCalculator.calculate(
                    DamageInput.of(100, 1.0, 1.0).withBonus(0.2, 0), new Resistances(0.25, 0.15));
            assertEquals(90.0, b.physicalDamage(), EPS);
            assertEquals(85.0, b.magicDamage(), EPS);
            assertEquals(175.0, b.total(), EPS);
        }

        @Test
        @DisplayName("치명타와 최종 보정은 저항 후 합계에 곱한다")
        void critAndFinalAfterResist() {
            // (150) × 1.5 × 1.1 = 247.5
            double d = DamageCalculator.damage(
                    DamageInput.of(100, 2.0, 0).withCrit(1.5).withFinalModifier(1.1), new Resistances(0.25, 0));
            assertEquals(247.5, d, EPS);
        }

        @Test
        @DisplayName("계수 0인 성분은 피해에 기여하지 않는다")
        void zeroCoefficient() {
            DamageBreakdown b = DamageCalculator.calculate(DamageInput.of(100, 2.0, 0), new Resistances(0, 0));
            assertEquals(0.0, b.magicDamage(), EPS);
        }

        @Test
        @DisplayName("음수 강화로 성분이 음수가 되어도 피해는 0 미만이 되지 않는다")
        void negativeBonusClamped() {
            double d = DamageCalculator.damage(DamageInput.of(100, 1.0, 0).withBonus(-2.0, 0), Resistances.NONE);
            assertEquals(0.0, d, EPS);
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class Validation {
        @Test
        void resistOutOfRange() {
            assertThrows(IllegalArgumentException.class, () -> new Resistances(1.2, 0));
            assertThrows(IllegalArgumentException.class, () -> new Resistances(0, -0.1));
        }

        @Test
        void negativeWeaponPower() {
            assertThrows(IllegalArgumentException.class, () -> DamageInput.of(-1, 1, 0));
        }

        @Test
        void penetrationOutOfRange() {
            assertThrows(IllegalArgumentException.class, () -> DamageInput.of(100, 1, 0).withPenetration(1.5, 0));
        }
    }
}
