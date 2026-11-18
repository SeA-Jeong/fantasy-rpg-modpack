package com.fantasyrpg.core.playerdata;

/**
 * 레벨·경험치·SP 계산. Minecraft 비의존 (claude/플레이어성장_설계.md, 통합문서 5절).
 *
 * <ul>
 *   <li>레벨 상한(하드 캡) = min(최대 레벨, magiccity 레벨 상한). 챕터 완료는 상한만 연다</li>
 *   <li>상한에 걸리면 경험치는 현재 레벨 바에 need(L)까지만 쌓이고(보관), 나머지는 버린다</li>
 *   <li>상한이 오르면 {@link #applyCap}로 보관 경험치만큼 바로 레벨업</li>
 *   <li>SP 합계 = 직업 선택 1 + (레벨 − 1) + 완료 보너스(최대 6) = 최대 66</li>
 * </ul>
 */
public final class ProgressionMath {
    /** 완료 보너스 SP 최대 */
    public static final int MAX_CLEAR_BONUS = 6;

    private ProgressionMath() {
    }

    /** 레벨·경험치 상태 */
    public record LevelState(int level, int xp) {
    }

    /** 경험치를 더한 결과와 오른 레벨 수 */
    public record Gain(LevelState state, int levelsGained, int discarded) {
    }

    public static int effectiveCap(int magiccityCap, ProgressionSettings s) {
        return Math.max(1, Math.min(s.maxLevel(), magiccityCap));
    }

    /** 경험치 추가. amount가 0 이하면 상한만 다시 적용 */
    public static Gain addXp(LevelState st, long amount, int cap, ProgressionSettings s) {
        int level = Math.max(1, st.level());
        long xp = Math.max(0, st.xp()) + Math.max(0, amount);
        int gained = 0;
        while (level < cap && level < s.maxLevel() && xp >= s.need(level)) {
            xp -= s.need(level);
            level++;
            gained++;
        }
        long kept;
        if (level >= s.maxLevel()) {
            kept = 0;
        } else if (level >= cap) {
            kept = Math.min(xp, s.need(level)); // 보관: 다음 1레벨 분량까지
        } else {
            kept = xp;
        }
        long discarded = xp - kept;
        return new Gain(new LevelState(level, (int) kept), gained, (int) Math.min(Integer.MAX_VALUE, discarded));
    }

    /** 상한이 바뀐 뒤 보관 경험치 적용 */
    public static Gain applyCap(LevelState st, int cap, ProgressionSettings s) {
        return addXp(st, 0, cap, s);
    }

    /** 완료 보너스 SP 목표 = min(6, 완료 수 ÷ 2) */
    public static int bonusTarget(int completed) {
        return Math.min(MAX_CLEAR_BONUS, Math.max(0, completed) / 2);
    }

    /** 받은 SP 합계 */
    public static int spTotal(boolean startSpGranted, int level, int clearBonusGranted) {
        return (startSpGranted ? 1 : 0) + Math.max(0, level - 1) + Math.max(0, clearBonusGranted);
    }

    /** 챕터 최초 완료 경험치 = 완료 뒤 등급 × 값 */
    public static int chapterClearXp(int gradeAfter, ProgressionSettings s) {
        return Math.max(0, gradeAfter) * s.chapterClearXpPerGrade();
    }
}
