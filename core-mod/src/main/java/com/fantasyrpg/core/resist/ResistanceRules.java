package com.fantasyrpg.core.resist;

import com.fantasyrpg.core.combat.Resistances;

/**
 * 필수 챕터 보스 저항 차이 기준 (기획 v0.4 5.1장).
 * 기본 상한 15%p, 예외 상한 20%p. 상한값은 설정 파일에서 받는다.
 */
public final class ResistanceRules {
    private static final double EPS = 1e-9;

    private ResistanceRules() {
    }

    public enum GapCheck {
        /** 기본 상한 이내 */
        OK,
        /** 기본 상한 초과, 예외 상한 이내 — 경고 */
        EXCEPTION_RANGE,
        /** 예외 상한 초과 — 기획 위반 */
        VIOLATION
    }

    public static GapCheck checkBossGap(MobRole role, Resistances resist, double defaultLimit, double exceptionLimit) {
        if (role != MobRole.CHAPTER_BOSS) {
            return GapCheck.OK;
        }
        double gap = resist.gap();
        if (gap <= defaultLimit + EPS) {
            return GapCheck.OK;
        }
        if (gap <= exceptionLimit + EPS) {
            return GapCheck.EXCEPTION_RANGE;
        }
        return GapCheck.VIOLATION;
    }
}
