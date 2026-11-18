package com.fantasyrpg.core.skilltree;

import java.util.HashMap;
import java.util.Map;

/**
 * 스킬트리 투자·되돌리기 판정 (기획 v0.4 2장·7.1, 부록 A-2, claude/스킬트리_설계.md). Minecraft 비의존.
 *
 * <ul>
 *   <li>투자: 직업 일치, 레벨 ≥ 요구, 남은 SP ≥ 1, 랭크 &lt; 최대, 같은 방향 선행 SP(자신·혼합 제외) ≥ 요구,
 *       혼합 노드는 두 방향 각 8SP 이상</li>
 *   <li>노드 하나 되돌리기(1랭크): 되돌린 뒤 찍혀 있는 모든 노드의 선행 조건이 여전히 맞아야 함</li>
 *   <li>쓴 SP = 랭크 합</li>
 * </ul>
 */
public final class SkillTreeRules {
    private SkillTreeRules() {
    }

    public enum Result { OK, UNKNOWN_NODE, WRONG_CLASS, LEVEL, NO_SP, MAX_RANK, PREREQUISITE, MIXED_PREREQUISITE, NOT_INVESTED, BREAKS_OTHERS }

    public static int spent(Map<String, Integer> ranks) {
        int sum = 0;
        for (int r : ranks.values()) sum += Math.max(0, r);
        return sum;
    }

    /** 방향에 쓴 SP (혼합 노드 제외). exclude 노드의 랭크는 빼고 센다 */
    public static int spentInDirection(SkillNode.Direction dir, Map<String, Integer> ranks, Map<String, SkillNode> nodes,
                                       String exclude) {
        int sum = 0;
        for (Map.Entry<String, Integer> e : ranks.entrySet()) {
            if (e.getKey().equals(exclude)) continue;
            SkillNode n = nodes.get(e.getKey());
            if (n == null || n.mixed()) continue;
            if (n.direction() == dir) sum += Math.max(0, e.getValue());
        }
        return sum;
    }

    /** 랭크가 이미 있는 노드가 지금 상태에서 선행 조건을 만족하는지 (레벨은 보지 않음) */
    static boolean prerequisitesMet(SkillNode n, Map<String, Integer> ranks, Map<String, SkillNode> nodes) {
        if (n.mixed()) {
            for (SkillNode.Direction d : n.directions()) {
                if (spentInDirection(d, ranks, nodes, n.id()) < SkillNode.MIXED_REQUIREMENT) return false;
            }
            return true;
        }
        return spentInDirection(n.direction(), ranks, nodes, n.id()) >= n.requiredDirectionSp();
    }

    public static Result canInvest(String nodeId, String playerClass, int level, int spTotal,
                                   Map<String, Integer> ranks, Map<String, SkillNode> nodes) {
        SkillNode n = nodes.get(nodeId);
        if (n == null) return Result.UNKNOWN_NODE;
        if (!n.classId().equals(playerClass)) return Result.WRONG_CLASS;
        if (level < n.requiredLevel()) return Result.LEVEL;
        if (spTotal - spent(ranks) < 1) return Result.NO_SP;
        if (ranks.getOrDefault(nodeId, 0) >= n.maxRank()) return Result.MAX_RANK;
        if (!prerequisitesMet(n, ranks, nodes)) return n.mixed() ? Result.MIXED_PREREQUISITE : Result.PREREQUISITE;
        return Result.OK;
    }

    /** 노드 하나 1랭크 되돌리기 가능 여부 */
    public static Result canRefund(String nodeId, Map<String, Integer> ranks, Map<String, SkillNode> nodes) {
        if (!nodes.containsKey(nodeId)) return Result.UNKNOWN_NODE;
        int r = ranks.getOrDefault(nodeId, 0);
        if (r <= 0) return Result.NOT_INVESTED;
        Map<String, Integer> after = new HashMap<>(ranks);
        if (r == 1) after.remove(nodeId); else after.put(nodeId, r - 1);
        for (Map.Entry<String, Integer> e : after.entrySet()) {
            SkillNode other = nodes.get(e.getKey());
            if (other == null || e.getValue() <= 0) continue;
            if (!prerequisitesMet(other, after, nodes)) return Result.BREAKS_OTHERS;
        }
        return Result.OK;
    }

    public static Map<String, Integer> withRank(Map<String, Integer> ranks, String nodeId, int delta) {
        Map<String, Integer> out = new HashMap<>(ranks);
        int r = out.getOrDefault(nodeId, 0) + delta;
        if (r <= 0) out.remove(nodeId); else out.put(nodeId, r);
        return out;
    }
}
