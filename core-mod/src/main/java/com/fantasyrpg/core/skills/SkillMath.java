package com.fantasyrpg.core.skills;

/**
 * 스킬 효과의 순수 계산 (Minecraft 비의존, JUnit 대상). 기준: claude/전사마법사_스킬_초안.md.
 */
public final class SkillMath {
    /** 스킬 가속 → 재사용 대기 배율: 100 ÷ (100 + 가속). 가속 100 = 절반, 음수는 0으로 (2026-11-21) */
    public static double hasteMult(double haste) {
        return 100.0 / (100.0 + Math.max(0, haste));
    }

    /** 예전 % 감소 r 과 같은 효과를 내는 가속 = 100r ÷ (1 − r) */
    public static double hasteFromRate(double rate) {
        return 100.0 * rate / (1.0 - rate);
    }

    private SkillMath() {
    }

    /**
     * 평면(xz)에서 대상이 시선 기준 부채꼴 안인지.
     *
     * @param dx,dz        나 → 대상
     * @param lookX,lookZ  시선 방향
     * @param angleDegrees 부채꼴 전체 각도 (90 = 좌우 45°)
     */
    public static boolean inCone(double dx, double dz, double lookX, double lookZ, double angleDegrees) {
        double d = Math.hypot(dx, dz);
        double l = Math.hypot(lookX, lookZ);
        if (d < 1e-6) return true; // 겹쳐 있으면 맞음
        if (l < 1e-6) return false;
        double cos = (dx * lookX + dz * lookZ) / (d * l);
        return cos >= Math.cos(Math.toRadians(angleDegrees / 2.0)) - 1e-9;
    }

    /** 마력 방벽: 받을 피해 중 마나로 대신 받을 양과 쓸 마나 */
    public record Ward(double absorbed, double manaSpent) {
    }

    /**
     * @param damage     들어오는 피해
     * @param ratio      마나로 받는 비율 (0~1)
     * @param mana       지금 마나
     * @param manaPerHp  피해 1당 마나
     */
    public static Ward manaWard(double damage, double ratio, double mana, double manaPerHp) {
        if (damage <= 0 || ratio <= 0 || mana <= 0 || manaPerHp <= 0) return new Ward(0, 0);
        double want = damage * Math.min(1.0, ratio);
        double affordable = mana / manaPerHp;
        double absorbed = Math.min(want, affordable);
        return new Ward(absorbed, absorbed * manaPerHp);
    }

    /** 주문 연쇄로 줄어든 쿨타임 (틱, 반올림, 0 이상) */
    public static int chainedCooldown(int baseTicks, double reduction) {
        double r = Math.max(0.0, Math.min(1.0, reduction));
        return (int) Math.max(0, Math.round(baseTicks * (1.0 - r)));
    }

    /** 감소율을 적용한 받는 피해 (불굴). 감소율은 0~0.9로 자름 */
    public static double reduced(double damage, double reduction) {
        return damage * (1.0 - Math.max(0.0, Math.min(0.9, reduction)));
    }

    /**
     * 공격자가 대상 등 뒤(반구)에 있는지.
     *
     * @param lookX,lookZ 대상이 바라보는 방향
     * @param dx,dz       대상 → 공격자
     */
    public static boolean isBehind(double lookX, double lookZ, double dx, double dz) {
        return lookX * dx + lookZ * dz < 0;
    }

    /** 같은 대상 연속 적중 수 갱신. 다른 대상이면 1부터 */
    public static int consecutive(boolean sameTarget, int previous) {
        return sameTarget ? previous + 1 : 1;
    }
}
