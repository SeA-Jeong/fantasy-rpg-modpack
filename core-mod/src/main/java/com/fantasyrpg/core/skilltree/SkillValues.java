package com.fantasyrpg.core.skilltree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 노드의 랭크별 수치 (claude/전사마법사_스킬_초안.md). Minecraft 비의존.
 * 값은 숫자 하나(모든 랭크 같음) 또는 길이 max_rank 배열(1랭크부터).
 *
 * <pre>
 * "cooldown_ticks": 120, "cost": 20,
 * "values": { "physical": [1.6, 1.8, 2.0, 2.2, 2.4], "stun_ticks": 10 }
 * </pre>
 */
public final class SkillValues {
    public static final String COOLDOWN = "cooldown_ticks";
    public static final String COST = "cost";
    public static final SkillValues EMPTY = new SkillValues(Map.of());

    private final Map<String, double[]> values;

    private SkillValues(Map<String, double[]> values) {
        this.values = Map.copyOf(values);
    }

    public static SkillValues of(Map<String, double[]> values) {
        return new SkillValues(values);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Set<String> keys() {
        return values.keySet();
    }

    /** 랭크(1부터)의 값. 랭크 0 이하는 1랭크 값, 배열보다 크면 마지막 값. 없는 키는 fallback */
    public double get(String key, int rank, double fallback) {
        double[] v = values.get(key);
        if (v == null || v.length == 0) return fallback;
        int i = Math.max(0, Math.min(v.length - 1, rank - 1));
        return v[i];
    }

    public static SkillValues parse(JsonObject json, int maxRank) {
        Map<String, double[]> out = new HashMap<>();
        for (String top : new String[]{COOLDOWN, COST}) {
            if (json.has(top)) {
                JsonElement e = json.get(top);
                // 예전 견본 형식 "cost": {"core": ..., "amount": 20}
                if (e.isJsonObject() && e.getAsJsonObject().has("amount")) e = e.getAsJsonObject().get("amount");
                out.put(top, read(top, e, maxRank));
            }
        }
        if (json.has("values")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("values").entrySet()) {
                out.put(e.getKey(), read(e.getKey(), e.getValue(), maxRank));
            }
        }
        return new SkillValues(out);
    }

    private static double[] read(String key, JsonElement e, int maxRank) {
        double[] v;
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            if (a.size() != maxRank) throw new IllegalArgumentException("'" + key + "' 배열 길이 " + a.size() + " ≠ max_rank " + maxRank);
            v = new double[a.size()];
            for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsDouble();
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            v = new double[]{e.getAsDouble()};
        } else {
            throw new IllegalArgumentException("'" + key + "'는 숫자나 숫자 배열이어야 합니다");
        }
        for (double d : v) {
            if (!Double.isFinite(d) || d < 0) throw new IllegalArgumentException("'" + key + "'에 음수·잘못된 값");
        }
        return v;
    }
}
