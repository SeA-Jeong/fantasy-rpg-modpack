package com.fantasyrpg.core.skilltree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 빌드 프리셋 (SP 배분까지 저장, 2026-11-21). Minecraft 비의존.
 * 적용 = 빈 트리에서 저장한 랭크를 낮은 단부터 다시 찍는다. 지금 레벨·SP·선행 조건으로 못 찍는 만큼은 빠진다.
 */
public final class BuildRules {
    private BuildRules() {}

    /** @param ranks 다시 찍은 결과, @param missing 못 찍은 랭크 수 */
    public record Rebuilt(Map<String, Integer> ranks, int missing) {}

    public static Rebuilt rebuild(Map<String, Integer> target, String playerClass, int level, int spTotal, Map<String, SkillNode> nodes) {
        Map<String, Integer> ranks = new HashMap<>();
        List<String> order = new ArrayList<>(target.keySet());
        order.removeIf(id -> !nodes.containsKey(id));
        order.sort(Comparator.comparingInt((String id) -> nodes.get(id).tier()).thenComparing(id -> id));
        boolean progress = true;
        while (progress) {
            progress = false;
            for (String id : order) {
                while (ranks.getOrDefault(id, 0) < target.get(id)
                        && SkillTreeRules.canInvest(id, playerClass, level, spTotal, ranks, nodes) == SkillTreeRules.Result.OK) {
                    ranks = SkillTreeRules.withRank(ranks, id, 1);
                    progress = true;
                }
            }
        }
        int want = 0, got = 0;
        for (var e : target.entrySet()) want += Math.max(0, e.getValue());
        for (int v : ranks.values()) got += v;
        return new Rebuilt(Map.copyOf(ranks), Math.max(0, want - got));
    }
}
