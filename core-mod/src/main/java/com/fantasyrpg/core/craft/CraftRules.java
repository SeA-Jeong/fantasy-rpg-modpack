package com.fantasyrpg.core.craft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * 확장 작업대·방어구 제작 규칙 (순수 계산, Minecraft 없이 테스트).
 * 기준: 프로젝트 문서 claude/작업대_방어구제작_초안.md v1 (2026-11-20 사용자 결정: 추천안 전부 —
 * 모양 없는 조합, JEI 표시, 지역 소재는 챕터 보스 + 그 지역 필드보스, 유산 2~6급 재료 교체). 수치는 모두 [초안] 데이터.
 *
 * <p>조합은 모양이 없다: 작업대 크기 안에서 칸마다 아이템 1개, 재료 종류별 개수가 정확히 맞아야 한다(남는 칸 없음).
 */
public final class CraftRules {
    private CraftRules() {
    }

    /** 재료 한 줄: 아이템 id 또는 태그(#로 시작) × 개수. id 가 "core" 면 그 계열 핵심 부품 자리 */
    public record Entry(String id, int count) {
        public boolean isTag() {
            return id.startsWith("#");
        }
    }

    /** 작업대 조합식: 작업대 크기(3·4·5), 필요한 진행 등급, 재료, 결과 */
    public record GridRecipe(String name, int grid, int grade, List<Entry> ingredients, String result, int resultCount) {
        public int slots() {
            int n = 0;
            for (Entry e : ingredients) n += e.count();
            return n;
        }
    }

    public static List<Entry> entries(com.google.gson.JsonArray arr) {
        List<Entry> out = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            String id = o.has("tag") ? "#" + o.get("tag").getAsString() : o.get("item").getAsString();
            int c = o.has("count") ? o.get("count").getAsInt() : 1;
            if (c <= 0) throw new IllegalArgumentException(id + " 개수는 1 이상");
            out.add(new Entry(id, c));
        }
        return out;
    }

    public static GridRecipe parseRecipe(String name, JsonObject o) {
        int grid = o.get("grid").getAsInt();
        if (grid < 3 || grid > 5) throw new IllegalArgumentException("grid 는 3~5: " + grid);
        int grade = o.has("grade") ? o.get("grade").getAsInt() : 1;
        if (grade < 1 || grade > 10) throw new IllegalArgumentException("grade 는 1~10: " + grade);
        List<Entry> in = entries(o.getAsJsonArray("ingredients"));
        JsonObject r = o.getAsJsonObject("result");
        GridRecipe g = new GridRecipe(name, grid, grade, List.copyOf(in), r.get("item").getAsString(), r.has("count") ? r.get("count").getAsInt() : 1);
        if (g.slots() > grid * grid) throw new IllegalArgumentException(name + ": 재료 " + g.slots() + "칸이 " + grid + "×" + grid + " 작업대보다 많음");
        if (g.slots() == 0) throw new IllegalArgumentException(name + ": 재료 없음");
        return g;
    }

    /**
     * 칸의 아이템(빈 칸 제외, 칸마다 1개로 셈)이 재료와 정확히 맞는지.
     * @param accepts (재료 줄, 칸 아이템 id) → 그 줄에 들어가는지 (태그 판정은 바깥에서)
     */
    public static <T> boolean matches(List<Entry> ingredients, List<T> items, BiPredicate<Entry, T> accepts) {
        int need = 0;
        for (Entry e : ingredients) need += e.count();
        if (need != items.size()) return false;
        int[] left = new int[ingredients.size()];
        for (int i = 0; i < left.length; i++) left[i] = ingredients.get(i).count();
        return assign(ingredients, items, accepts, left, 0);
    }

    /** 칸을 하나씩 재료 줄에 배정 (겹치는 태그가 있어도 맞는 배정을 찾도록 되돌아가며) */
    private static <T> boolean assign(List<Entry> ing, List<T> items, BiPredicate<Entry, T> accepts, int[] left, int i) {
        if (i == items.size()) return true;
        T it = items.get(i);
        for (int k = 0; k < ing.size(); k++) {
            if (left[k] == 0 || !accepts.test(ing.get(k), it)) continue;
            left[k]--;
            if (assign(ing, items, accepts, left, i + 1)) return true;
            left[k]++;
        }
        return false;
    }

    // ───────── 방어구 ─────────

    /** 부위별 재료 비율 (흉갑 8 : 각반 7 : 투구 5 : 장화 4, 바닐라 갑옷 재료 비율) */
    public static int partWeight(String part) {
        return switch (part) {
            case "chestplate" -> 8;
            case "leggings" -> 7;
            case "helmet" -> 5;
            case "boots" -> 4;
            default -> throw new IllegalArgumentException("모르는 부위: " + part);
        };
    }

    /** 흉갑 기준 개수를 부위에 맞게 (올림, 최소 1) */
    public static int scaled(int chestCount, String part) {
        return Math.max(1, (chestCount * partWeight(part) + 7) / 8);
    }

    /** 승급 목표 등급에 필요한 작업대 크기 (2·3급 4×4, 4~6급 5×5) */
    public static int gridForTier(int target) {
        return target <= 3 ? 4 : 5;
    }

    /** 이번에 다루는 최고 승급 등급 (7~10급은 네더, 다음 작업) */
    public static final int MAX_UPGRADE = 6;

    /**
     * 방어구 제작표 (armor_crafting/costs.json):
     * weight: 계열 → heavy|light, core: 계열 → 핵심 부품 id,
     * upgrade: heavy|light → 목표 등급 → 흉갑 기준 재료 (id "core" = 그 계열 핵심 부품),
     * create: 계열 → 1급 만들기 {base: 바닐라 갑옷 재질(iron|leather…), items: 추가 재료}
     */
    public record ArmorTable(Map<String, String> weight, Map<String, String> core,
                             Map<String, Map<Integer, List<Entry>>> upgrade, Map<String, Create> create) {
        public record Create(String base, List<Entry> items) {
        }

        /** 계열·부위·목표 등급의 승급 재료 (부위 비율 적용, core 자리는 계열 핵심 부품). 없으면 null */
        public List<Entry> upgradeCost(String family, String part, int target) {
            String w = weight.get(family);
            Map<Integer, List<Entry>> byTier = w == null ? null : upgrade.get(w);
            List<Entry> chest = byTier == null ? null : byTier.get(target);
            if (chest == null) return null;
            List<Entry> out = new ArrayList<>();
            for (Entry e : chest) {
                String id = "core".equals(e.id()) ? core.get(family) : e.id();
                if (id == null) return null;
                out.add(new Entry(id, scaled(e.count(), part)));
            }
            return out;
        }

        /** 1급 만들기 재료 (부위 비율 적용) */
        public List<Entry> createCost(String family, String part) {
            Create c = create.get(family);
            if (c == null) return null;
            List<Entry> out = new ArrayList<>();
            for (Entry e : c.items()) out.add(new Entry(e.id(), scaled(e.count(), part)));
            return out;
        }
    }

    public static ArmorTable parseArmor(JsonObject o) {
        Map<String, String> weight = new LinkedHashMap<>(), core = new LinkedHashMap<>();
        for (var e : o.getAsJsonObject("weight").entrySet()) {
            String w = e.getValue().getAsString();
            if (!w.equals("heavy") && !w.equals("light")) throw new IllegalArgumentException(e.getKey() + ": weight 는 heavy|light");
            weight.put(e.getKey(), w);
        }
        for (var e : o.getAsJsonObject("core").entrySet()) core.put(e.getKey(), e.getValue().getAsString());
        Map<String, Map<Integer, List<Entry>>> upgrade = new LinkedHashMap<>();
        for (var w : o.getAsJsonObject("upgrade").entrySet()) {
            Map<Integer, List<Entry>> byTier = new LinkedHashMap<>();
            for (var t : w.getValue().getAsJsonObject().entrySet()) {
                int tier = Integer.parseInt(t.getKey());
                if (tier < 2 || tier > MAX_UPGRADE) throw new IllegalArgumentException("승급 목표 등급은 2~" + MAX_UPGRADE + ": " + tier);
                List<Entry> list = entries(t.getValue().getAsJsonArray());
                int slots = 1;
                for (Entry en : list) slots += en.count();
                int g = gridForTier(tier);
                if (slots > g * g) throw new IllegalArgumentException(w.getKey() + " " + tier + "급: 재료 " + slots + "칸이 " + g + "×" + g + "보다 많음");
                byTier.put(tier, List.copyOf(list));
            }
            upgrade.put(w.getKey(), byTier);
        }
        Map<String, ArmorTable.Create> create = new LinkedHashMap<>();
        for (var e : o.getAsJsonObject("create").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            List<Entry> items = entries(c.getAsJsonArray("items"));
            int slots = 1;
            for (Entry en : items) slots += en.count();
            if (slots > 9) throw new IllegalArgumentException(e.getKey() + " 1급 만들기 재료가 9칸보다 많음");
            create.put(e.getKey(), new ArmorTable.Create(c.get("base").getAsString(), List.copyOf(items)));
        }
        for (String f : weight.keySet()) if (!core.containsKey(f)) throw new IllegalArgumentException(f + ": 핵심 부품 없음");
        return new ArmorTable(Map.copyOf(weight), Map.copyOf(core), Map.copyOf(upgrade), Map.copyOf(create));
    }
}
