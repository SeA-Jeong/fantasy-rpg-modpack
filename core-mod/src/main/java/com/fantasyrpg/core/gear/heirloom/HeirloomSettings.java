package com.fantasyrpg.core.gear.heirloom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 유산 무기 수치 ({@code data/fantasyrpg/heirloom/settings.json}). Minecraft 비의존.
 * 기준: 기획 v0.4 6.1 (2026-11-19 결정), 프로젝트 문서 claude/유산무기_초안.md 3·4절.
 *
 * @param traits 방향 id(attack·spell·guard·finesse) → 단계당 효과 (키: physical_bonus·magic_bonus·damage_reduction·crit_chance)
 * @param enhance 강화 비용: 목표 등급마다 재료 아이템 id·개수 (2~10급)
 * @param stageUpMaterials 각성 단계가 오를 때 새로 열린 등급의 강화 재료를 지급할지 (기획 6.2: 메인 진행만으로 재료 확보)
 */
public record HeirloomSettings(Map<String, Map<String, Double>> traits, Map<Integer, Cost> enhance, boolean stageUpMaterials) {
    public static final List<String> DIRECTIONS = List.of("attack", "spell", "guard", "finesse");

    /** 한 단계 강화 비용: 재료 여러 종류 (2026-11-20 장비 부품으로 교체하면서 여러 종류로) */
    public record Cost(List<Part> parts) {
        public Cost {
            parts = List.copyOf(parts);
            if (parts.isEmpty()) throw new IllegalArgumentException("강화 재료 없음");
        }
    }

    /** 재료 한 종류 */
    public record Part(String item, int count) {}

    public HeirloomSettings {
        traits = Map.copyOf(traits);
        enhance = Map.copyOf(enhance);
        for (String d : traits.keySet()) {
            if (!DIRECTIONS.contains(d)) throw new IllegalArgumentException("모르는 방향: " + d);
        }
        for (var e : traits.entrySet()) {
            for (var v : e.getValue().entrySet()) {
                if (v.getValue() < 0 || v.getValue() > 0.5) throw new IllegalArgumentException(e.getKey() + "." + v.getKey() + " 는 0~0.5");
            }
        }
        for (var e : enhance.entrySet()) {
            if (e.getKey() < 2 || e.getKey() > 10) throw new IllegalArgumentException("강화 목표 등급은 2~10: " + e.getKey());
            for (Part pt : e.getValue().parts()) if (pt.count() <= 0) throw new IllegalArgumentException(e.getKey() + "급 재료 개수는 1 이상");
        }
    }

    public static HeirloomSettings parse(JsonObject o) {
        Map<String, Map<String, Double>> traits = new LinkedHashMap<>();
        JsonObject t = o.getAsJsonObject("traits");
        for (String d : t.keySet()) {
            Map<String, Double> m = new LinkedHashMap<>();
            JsonObject per = t.getAsJsonObject(d);
            for (String k : per.keySet()) m.put(k, per.get(k).getAsDouble());
            traits.put(d, m);
        }
        Map<Integer, Cost> enhance = new LinkedHashMap<>();
        List<JsonElement> list = new ArrayList<>();
        o.getAsJsonArray("enhance").forEach(list::add);
        for (JsonElement e : list) {
            JsonObject c = e.getAsJsonObject();
            List<Part> parts = new ArrayList<>();
            if (c.has("items")) {
                for (JsonElement it : c.getAsJsonArray("items")) {
                    JsonObject io = it.getAsJsonObject();
                    parts.add(new Part(io.get("item").getAsString(), io.get("count").getAsInt()));
                }
            } else {
                parts.add(new Part(c.get("item").getAsString(), c.get("count").getAsInt()));
            }
            enhance.put(c.get("to").getAsInt(), new Cost(parts));
        }
        return new HeirloomSettings(traits, enhance, !o.has("stage_up_materials") || o.get("stage_up_materials").getAsBoolean());
    }

    /** 고른 특성 단계로 합친 효과 (예: physical_bonus) */
    public double bonus(Map<String, Integer> picks, String key) {
        double sum = 0;
        for (var e : picks.entrySet()) {
            Map<String, Double> per = traits.get(e.getKey());
            if (per != null) sum += per.getOrDefault(key, 0.0) * e.getValue();
        }
        return sum;
    }
}
