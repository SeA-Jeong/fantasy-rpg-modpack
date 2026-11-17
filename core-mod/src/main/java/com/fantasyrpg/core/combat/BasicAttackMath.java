package com.fantasyrpg.core.combat;

/**
 * 기본 공격 입력값 정리 (순수 계산, Minecraft 비의존).
 *
 * <p>Better Combat 연동 실험(①: 바닐라 공격 취소 후 RPG 피해)에서 쓰는 값만 다룬다.
 * 기본 공격 계수·W는 직업·무기 데이터에서 오고, 여기서는 외부에서 읽은 값의 안전 처리만 한다.
 */
public final class BasicAttackMath {

    /** 콤보 배율로 받아들이는 최댓값. Better Combat 무기 속성의 비정상 값 방어용 */
    public static final double MAX_COMBO_MULTIPLIER = 10.0;

    private BasicAttackMath() {
    }

    /**
     * Better Combat 콤보 배율 정리: 값이 없거나(NaN·무한)·0 이하이면 1.0, 상한 {@link #MAX_COMBO_MULTIPLIER}.
     */
    public static double sanitizeCombo(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0.0) return 1.0;
        return Math.min(multiplier, MAX_COMBO_MULTIPLIER);
    }

    /**
     * 공격 충전도(0~1) → 피해 배율. 바닐라 {@code Player.attack}의 {@code 0.2 + 0.8 × c²}을 그대로 쓴다.
     * (새 수치를 만들지 않고 바닐라 체감을 유지하기 위함. 다른 곡선이 필요하면 기획 결정 후 교체)
     */
    public static double chargeScale(double charge) {
        double c = Double.isFinite(charge) ? Math.max(0.0, Math.min(1.0, charge)) : 1.0;
        return 0.2 + 0.8 * c * c;
    }

    /** 기본 공격의 최종 물리 계수 = 기본 계수 × 콤보 배율 × 충전 배율. 음수·비정상 기본 계수는 0 */
    public static double basicCoefficient(double baseCoef, double comboMultiplier, double charge) {
        double base = Double.isFinite(baseCoef) && baseCoef > 0.0 ? baseCoef : 0.0;
        return base * sanitizeCombo(comboMultiplier) * chargeScale(charge);
    }
}
