package com.fantasyrpg.core.skilltree;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * 공통 패시브 노드 (직업과 무관한 별도 트리, 별도 포인트 — claude/스킬트리_설계.md 3절). Minecraft 비의존.
 *
 * <pre>
 * { "id": "fantasyrpg:passive/vitality", "name": "체력 단련", "max_rank": 3, "required_grade": 1,
 *   "attribute": "minecraft:generic.max_health", "operation": "add_value", "per_rank": 2 }
 * </pre>
 */
public record PassiveNode(String id, String name, int maxRank, int requiredGrade, String attribute, String operation,
                          double perRank) {
    public static final List<String> OPERATIONS = List.of("add_value", "add_multiplied_base", "add_multiplied_total");
    /** 등급마다 받는 공통 패시브 포인트 */
    public static final int POINTS_PER_GRADE = 2;
    /** 지금 포인트를 주는 최고 등급 (네더 7~10급은 미정) */
    public static final int MAX_POINT_GRADE = 6;

    public enum Result { OK, UNKNOWN_NODE, GRADE, NO_POINTS, MAX_RANK, NOT_INVESTED }

    /** 공통 패시브 포인트 = 2 × 진행 등급 (1~6급) */
    public static int points(int grade) {
        return POINTS_PER_GRADE * Math.max(0, Math.min(MAX_POINT_GRADE, grade));
    }

    public static Result canInvest(String nodeId, int grade, Map<String, Integer> ranks, Map<String, PassiveNode> nodes) {
        PassiveNode n = nodes.get(nodeId);
        if (n == null) return Result.UNKNOWN_NODE;
        if (grade < n.requiredGrade()) return Result.GRADE;
        if (points(grade) - SkillTreeRules.spent(ranks) < 1) return Result.NO_POINTS;
        if (ranks.getOrDefault(nodeId, 0) >= n.maxRank()) return Result.MAX_RANK;
        return Result.OK;
    }

    public static PassiveNode parse(JsonObject json) {
        String id = json.get("id").getAsString().trim();
        int maxRank = json.get("max_rank").getAsInt();
        if (maxRank < 1 || maxRank > 10) throw new IllegalArgumentException(id + ": max_rank 1~10");
        int grade = json.has("required_grade") ? json.get("required_grade").getAsInt() : 1;
        String attr = json.get("attribute").getAsString().trim();
        if (!attr.contains(":")) throw new IllegalArgumentException(id + ": 능력치 id에 이름공간이 없음");
        String op = json.get("operation").getAsString().trim();
        if (!OPERATIONS.contains(op)) throw new IllegalArgumentException(id + ": 연산 " + op);
        double per = json.get("per_rank").getAsDouble();
        if (!Double.isFinite(per)) throw new IllegalArgumentException(id + ": per_rank");
        String name = json.has("name") ? json.get("name").getAsString() : id;
        return new PassiveNode(id, name, maxRank, grade, attr, op, per);
    }
}
