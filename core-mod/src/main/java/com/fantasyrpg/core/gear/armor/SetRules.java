package com.fantasyrpg.core.gear.armor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 계열 방어구 세트 효과 규칙 (순수 계산, claude/세트효과_초안.md — 2026-11-21 사용자 결정 A·B·C 추천안).
 * <ul>
 *   <li>2세트 등급 = 그 계열 부위 중 등급이 높은 2개 중 낮은 등급, 4세트 등급 = 4부위 중 가장 낮은 등급 [확정 2026-11-17]</li>
 *   <li>크기 배율: 세트 등급 1~3급 ×1, 4~6급 ×1.5, 7~10급 ×2</li>
 *   <li>값 이름이 _ticks·_cooldown·_seconds 로 끝나면 시간이라 배율을 곱하지 않음</li>
 *   <li>누가 입어도 같은 효과 (직업 제한 없음)</li>
 * </ul>
 * 데이터 {@code data/fantasyrpg/armor_sets/<계열>.json}: {"two": {값...}, "four": {값...}}.
 */
public final class SetRules {
    private SetRules() {}

    /** 한 계열 세트: 2부위·4부위 값 (배율 전) */
    public record SetDef(String family, Map<String, Double> two, Map<String, Double> four) {}

    /** 입은 상태: 그 계열 부위 수와 2·4세트 등급 (0 = 꺼짐) */
    public record Worn(int pieces, int grade2, int grade4) {
        public boolean two() { return grade2 > 0; }
        public boolean four() { return grade4 > 0; }
    }

    /** 그 계열 부위들의 등급 목록 → 세트 상태 */
    public static Worn worn(List<Integer> tiers) {
        List<Integer> t = new ArrayList<>(tiers);
        t.removeIf(x -> x == null || x < 1);
        t.sort(Collections.reverseOrder());
        int g2 = t.size() >= 2 ? t.get(1) : 0;
        int g4 = t.size() >= 4 ? t.get(t.size() - 1) : 0;
        return new Worn(t.size(), g2, g4);
    }

    /** 세트 등급 → 크기 배율 */
    public static double band(int grade) {
        if (grade <= 0) return 0;
        if (grade <= 3) return 1.0;
        if (grade <= 6) return 1.5;
        return 2.0;
    }

    public static boolean isTime(String key) {
        return key.endsWith("_ticks") || key.endsWith("_cooldown") || key.endsWith("_seconds");
    }

    /** 배율 적용 값 (시간 값은 그대로, 세트가 꺼져 있으면 0) */
    public static double value(Map<String, Double> m, String key, int grade) {
        Double v = m.get(key);
        if (v == null || grade <= 0) return 0;
        return isTime(key) ? v : v * band(grade);
    }

    public static SetDef parse(String family, JsonObject o) {
        for (String k : o.keySet()) if (!k.equals("two") && !k.equals("four") && !k.startsWith("_")) throw new IllegalArgumentException("모르는 항목 '" + k + "'");
        if (!o.has("two") || !o.has("four")) throw new IllegalArgumentException("two·four 가 모두 있어야 함");
        return new SetDef(family, values(o.getAsJsonObject("two")), values(o.getAsJsonObject("four")));
    }

    private static Map<String, Double> values(JsonObject o) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            double v = e.getValue().getAsDouble();
            if (!Double.isFinite(v) || v < 0 || (!isTime(e.getKey()) && v > 10)) throw new IllegalArgumentException(e.getKey() + " = " + v);
            out.put(e.getKey(), v);
        }
        return Map.copyOf(out);
    }
}
