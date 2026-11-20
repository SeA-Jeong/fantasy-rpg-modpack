package com.fantasyrpg.core.skilltree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 초기화·직업 변경·빌드 바꾸기 재료 비용 (2026-11-21, claude/스킬트리_설계.md 4절). Minecraft 비의존.
 * 한 항목 = 아이템 × (base + per_grade × 진행 등급). 목록이 비면 무료. 수치는 skill_settings/costs.json [초안].
 */
public final class CostRules {
    private CostRules() {}

    public record Cost(String item, int base, int perGrade) {
        public Cost {
            if (item == null || item.isEmpty()) throw new IllegalArgumentException("item 이 없음");
            if (base < 0 || perGrade < 0) throw new IllegalArgumentException("수량은 0 이상");
        }

        public int amount(int grade) {
            return base + perGrade * Math.max(1, grade);
        }
    }

    /** costs.json 의 "costs" 객체 */
    public record Table(List<Cost> nodeRefund, List<Cost> fullReset, List<Cost> buildChange, List<Cost> classChange) {
        public static final Table FREE = new Table(List.of(), List.of(), List.of(), List.of());
    }

    public static Table parse(JsonObject o) {
        return new Table(list(o, "node_refund"), list(o, "full_reset"), list(o, "build_change"), list(o, "class_change"));
    }

    private static List<Cost> list(JsonObject o, String key) {
        if (!o.has(key)) return List.of();
        JsonArray a = o.getAsJsonArray(key);
        java.util.ArrayList<Cost> out = new java.util.ArrayList<>();
        for (JsonElement e : a) {
            JsonObject c = e.getAsJsonObject();
            out.add(new Cost(c.get("item").getAsString(), c.has("base") ? c.get("base").getAsInt() : 0,
                    c.has("per_grade") ? c.get("per_grade").getAsInt() : 0));
        }
        return List.copyOf(out);
    }

    /** 아이템 id → 필요 수 (같은 아이템은 합침, 0개는 뺌) */
    public static Map<String, Integer> amounts(List<Cost> costs, int grade, int times) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Cost c : costs) {
            int n = c.amount(grade) * Math.max(1, times);
            if (n > 0) out.merge(c.item(), n, Integer::sum);
        }
        return out;
    }

    /** 가진 수(have)로 모자란 것 (없으면 빈 맵) */
    public static Map<String, Integer> missing(Map<String, Integer> need, Map<String, Integer> have) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : need.entrySet()) {
            int lack = e.getValue() - have.getOrDefault(e.getKey(), 0);
            if (lack > 0) out.put(e.getKey(), lack);
        }
        return out;
    }
}
