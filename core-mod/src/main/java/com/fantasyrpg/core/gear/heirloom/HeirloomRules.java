package com.fantasyrpg.core.gear.heirloom;

import java.util.Map;

/**
 * 유산 무기 성장 규칙 (기획 v0.4 6.1, 2026-11-19: 진행이 각성·강화 한계를 열고 재료로 등급을 올림). Minecraft 비의존.
 */
public final class HeirloomRules {
    private HeirloomRules() {}

    public static final int MIN_TIER = 1, MAX_TIER = 10;

    /** 강화 한계: 깨어나기 전에는 1급, 깨어난 뒤에는 진행 등급(1~10) */
    public static int cap(int grade, boolean awakened) {
        return awakened ? Math.max(MIN_TIER, Math.min(MAX_TIER, grade)) : MIN_TIER;
    }

    /** 각성 단계 = 깨어났으면 진행 등급, 아니면 0 */
    public static int stage(int grade, boolean awakened) {
        return awakened ? Math.max(1, grade) : 0;
    }

    /** 고를 수 있는 특성 수 합계: 각성 단계가 1을 넘을 때마다 1개 (2급 → 1개 … 6급 → 5개) */
    public static int picksTotal(int grade, boolean awakened) {
        return Math.max(0, stage(grade, awakened) - 1);
    }

    public static int picksUsed(Map<String, Integer> picks) {
        int n = 0;
        for (int v : picks.values()) n += Math.max(0, v);
        return n;
    }

    public static int picksLeft(int grade, boolean awakened, Map<String, Integer> picks) {
        return Math.max(0, picksTotal(grade, awakened) - picksUsed(picks));
    }

    public enum EnhanceResult { OK, NOT_AWAKENED, AT_CAP, MAX, NO_COST, NOT_ENOUGH }

    /**
     * 한 단계 강화 판정.
     * @param have 가진 재료 개수
     */
    public static EnhanceResult canEnhance(int tier, int grade, boolean awakened, HeirloomSettings s, int have) {
        if (!awakened) return EnhanceResult.NOT_AWAKENED;
        if (tier >= MAX_TIER) return EnhanceResult.MAX;
        if (tier >= cap(grade, true)) return EnhanceResult.AT_CAP;
        HeirloomSettings.Cost c = s.enhance().get(tier + 1);
        if (c == null) return EnhanceResult.NO_COST;
        return have >= c.count() ? EnhanceResult.OK : EnhanceResult.NOT_ENOUGH;
    }

    public enum PickResult { OK, UNKNOWN_DIRECTION, NONE_LEFT }

    public static PickResult canPick(String direction, int grade, boolean awakened, Map<String, Integer> picks) {
        if (!HeirloomSettings.DIRECTIONS.contains(direction)) return PickResult.UNKNOWN_DIRECTION;
        return picksLeft(grade, awakened, picks) > 0 ? PickResult.OK : PickResult.NONE_LEFT;
    }
}
