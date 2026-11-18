package com.fantasyrpg.core.skilltree;

/**
 * 무료 전체 초기화권 (claude/스킬트리_설계.md 4절). Minecraft 비의존.
 * 받은 수는 저장하지 않고 진행 상태로 계산하고, 쓴 수만 저장한다(중복 지급 없음).
 * <ul>
 *   <li>처음 직업 선택: 1</li>
 *   <li>5×5 작업대 해금(메인 챕터 완료 6개): 1</li>
 *   <li>네더 진입·노드 수치 변경 업데이트: 다음 단계</li>
 *   <li>엔딩 이후: 자유 무료 (magiccity 엔딩 API 이후)</li>
 * </ul>
 */
public final class ResetRules {
    /** 5×5 작업대가 열리는 메인 챕터 완료 수 (통합문서 5절) */
    public static final int GRID5_COMPLETIONS = 6;

    private ResetRules() {
    }

    public static int freeResetsEarned(boolean hasClass, int completed) {
        return (hasClass ? 1 : 0) + (completed >= GRID5_COMPLETIONS ? 1 : 0);
    }

    public static int freeResetsAvailable(boolean hasClass, int completed, int used, boolean afterEnding) {
        if (afterEnding) return Integer.MAX_VALUE;
        return Math.max(0, freeResetsEarned(hasClass, completed) - used);
    }
}
