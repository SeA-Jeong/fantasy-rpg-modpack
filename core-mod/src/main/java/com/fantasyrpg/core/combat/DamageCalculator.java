package com.fantasyrpg.core.combat;

/**
 * 기획 v0.4 4장의 피해 공식. Minecraft 클래스에 의존하지 않는 순수 계산.
 *
 * <pre>
 * Reff,t = max(0, Rt − Pt)
 * Dp = W × Cp × (1+Sp) × (1 − Reff,p)
 * Dm = W × Cm × (1+Sm) × (1 − Reff,m)
 * D  = (Dp + Dm) × Kcrit × F
 * </pre>
 *
 * 치명타 발생 여부(확률 판정)는 호출하는 쪽에서 결정하고, 결과 배율만 입력으로 넘긴다.
 */
public final class DamageCalculator {
    private DamageCalculator() {
    }

    /** 실효 저항: 관통을 %p로 차감하고 0 미만은 0으로 막는다 (4.2장). */
    public static double effectiveResist(double resist, double penetration) {
        return Math.max(0.0, resist - penetration);
    }

    public static DamageBreakdown calculate(DamageInput in, Resistances target) {
        double physicalBase = in.weaponPower() * in.physicalCoef() * (1.0 + in.physicalBonus());
        double magicBase = in.weaponPower() * in.magicCoef() * (1.0 + in.magicBonus());

        double reffP = effectiveResist(target.physical(), in.physicalPen());
        double reffM = effectiveResist(target.magic(), in.magicPen());

        // 강화가 음수여서 성분이 음수가 되는 경우 피해 0으로 처리 (회복으로 바뀌지 않게)
        double dp = Math.max(0.0, physicalBase * (1.0 - reffP));
        double dm = Math.max(0.0, magicBase * (1.0 - reffM));

        double total = (dp + dm) * in.critMultiplier() * in.finalModifier();
        return new DamageBreakdown(physicalBase, magicBase, reffP, reffM, dp, dm,
                in.critMultiplier(), in.finalModifier(), total);
    }

    public static double damage(DamageInput in, Resistances target) {
        return calculate(in, target).total();
    }
}
