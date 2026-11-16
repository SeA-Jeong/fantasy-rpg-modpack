package com.fantasyrpg.core.combat;

/**
 * 피해 계산 입력값 (기획 v0.4 4.1장의 기호를 그대로 사용).
 *
 * @param weaponPower    W: 단일 무기 위력 (0 이상)
 * @param physicalCoef   Cp: 스킬 물리 계수 (예: 2.0 = 200%)
 * @param magicCoef      Cm: 스킬 마법 계수
 * @param physicalBonus  Sp: 물리 성분 스킬 강화 (예: 0.1 = +10%)
 * @param magicBonus     Sm: 마법 성분 스킬 강화
 * @param physicalPen    Pp: 물리 관통 %p (0~1)
 * @param magicPen       Pm: 마법 관통 %p (0~1)
 * @param critMultiplier Kcrit: 치명타가 실제로 발생했을 때의 배율, 미발생 시 1
 * @param finalModifier  F: 최종 피해 보정
 */
public record DamageInput(
        double weaponPower,
        double physicalCoef,
        double magicCoef,
        double physicalBonus,
        double magicBonus,
        double physicalPen,
        double magicPen,
        double critMultiplier,
        double finalModifier
) {
    public DamageInput {
        requireNonNegative("weaponPower", weaponPower);
        requireNonNegative("physicalCoef", physicalCoef);
        requireNonNegative("magicCoef", magicCoef);
        requireFinite("physicalBonus", physicalBonus);
        requireFinite("magicBonus", magicBonus);
        Resistances.requireRatio("physicalPen", physicalPen);
        Resistances.requireRatio("magicPen", magicPen);
        requireNonNegative("critMultiplier", critMultiplier);
        requireNonNegative("finalModifier", finalModifier);
    }

    /** 강화·관통·치명타·보정 없이 W와 계수만 지정한 입력. */
    public static DamageInput of(double weaponPower, double physicalCoef, double magicCoef) {
        return new DamageInput(weaponPower, physicalCoef, magicCoef, 0, 0, 0, 0, 1, 1);
    }

    public DamageInput withPenetration(double physical, double magic) {
        return new DamageInput(weaponPower, physicalCoef, magicCoef, physicalBonus, magicBonus,
                physical, magic, critMultiplier, finalModifier);
    }

    public DamageInput withBonus(double physical, double magic) {
        return new DamageInput(weaponPower, physicalCoef, magicCoef, physical, magic,
                physicalPen, magicPen, critMultiplier, finalModifier);
    }

    public DamageInput withCrit(double multiplier) {
        return new DamageInput(weaponPower, physicalCoef, magicCoef, physicalBonus, magicBonus,
                physicalPen, magicPen, multiplier, finalModifier);
    }

    public DamageInput withFinalModifier(double modifier) {
        return new DamageInput(weaponPower, physicalCoef, magicCoef, physicalBonus, magicBonus,
                physicalPen, magicPen, critMultiplier, modifier);
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + "은(는) 유한한 수여야 합니다: " + value);
        }
    }

    private static void requireNonNegative(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(name + "은(는) 0 이상이어야 합니다: " + value);
        }
    }
}
