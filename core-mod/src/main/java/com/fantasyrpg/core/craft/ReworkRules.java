package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.craft.CraftRules.Entry;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 계열 갑옷 분해·계열 전환 (2026-11-21). Minecraft 비의존. 수치는 armor_crafting/costs.json 의 "rework" [초안].
 * <ul>
 *   <li>분해: 작업대에 갑옷 1개 + 분해 도구(숫돌) → 1급 만들기 재료와 2~지금 등급 승급 재료 합계 × 비율(내림).
 *       태그 재료는 돌려주지 않음. 룬석이 끼워져 있으면 안 됨(먼저 빼기). 인챈트는 사라짐</li>
 *   <li>계열 전환: 갑옷 1개 + 바꿀 계열의 1급 만들기 재료 + (4급부터) 그 계열 핵심 부품 × (등급 − 3) → 같은 부위·같은 등급의
 *       다른 계열 갑옷. 인챈트·룬석·이름 그대로. 경갑 전용 룬석이 있으면 중갑으로는 안 됨</li>
 *   <li>수리 (2026-11-21): 파손(내구도 1)되거나 닳은 갑옷 + 수리 재료 → 내구도 가득, 나머지 그대로. 일반 제작대.
 *       1~3급은 강화합금 × 등급, 4급부터는 그 계열 핵심 부품 × (등급 − 3)</li>
 * </ul>
 */
public final class ReworkRules {
    private ReworkRules() {}

    public record Settings(String dismantleTool, double dismantleRatio, int convertCoreFromTier, String repairItem, int repairCoreFromTier) {
        public static final Settings DEFAULT = new Settings("minecraft:grindstone", 0.5, 4, "fantasyrpg:reinforced_alloy", 4);

        public Settings {
            if (dismantleRatio < 0 || dismantleRatio > 1) throw new IllegalArgumentException("dismantle_ratio 는 0~1");
            if (convertCoreFromTier < 1) throw new IllegalArgumentException("convert_core_from_tier 는 1 이상");
            if (repairCoreFromTier < 1) throw new IllegalArgumentException("repair_core_from_tier 는 1 이상");
        }

        public static Settings parse(JsonObject root) {
            if (root == null || !root.has("rework")) return DEFAULT;
            JsonObject o = root.getAsJsonObject("rework");
            return new Settings(o.has("dismantle_tool") ? o.get("dismantle_tool").getAsString() : DEFAULT.dismantleTool,
                    o.has("dismantle_ratio") ? o.get("dismantle_ratio").getAsDouble() : DEFAULT.dismantleRatio,
                    o.has("convert_core_from_tier") ? o.get("convert_core_from_tier").getAsInt() : DEFAULT.convertCoreFromTier,
                    o.has("repair_item") ? o.get("repair_item").getAsString() : DEFAULT.repairItem,
                    o.has("repair_core_from_tier") ? o.get("repair_core_from_tier").getAsInt() : DEFAULT.repairCoreFromTier);
        }
    }

    /** 분해로 돌려받는 아이템 id → 개수 (0개는 뺌, 순서 = 처음 나온 순) */
    public static Map<String, Integer> dismantle(ArmorTable t, Settings s, String family, String part, int tier) {
        Map<String, Integer> spent = new LinkedHashMap<>();
        List<Entry> create = t.createCost(family, part);
        if (create != null) for (Entry e : create) if (!e.isTag()) spent.merge(e.id(), e.count(), Integer::sum);
        for (int k = 2; k <= Math.min(tier, CraftRules.MAX_UPGRADE); k++) {
            List<Entry> c = t.upgradeCost(family, part, k);
            if (c == null) continue;
            for (Entry e : c) if (!e.isTag()) spent.merge(e.id(), e.count(), Integer::sum);
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : spent.entrySet()) {
            int n = (int) Math.floor(e.getValue() * s.dismantleRatio() + 1e-9);
            if (n > 0) out.put(e.getKey(), n);
        }
        return out;
    }

    /** 계열 전환 재료 (갑옷 자신은 빼고). 바꿀 계열이 같거나 표에 없으면 null */
    public static List<Entry> convertCost(ArmorTable t, Settings s, String from, String to, String part, int tier) {
        if (from.equals(to)) return null;
        List<Entry> create = t.createCost(to, part);
        if (create == null) return null;
        List<Entry> out = new ArrayList<>(create);
        if (tier >= s.convertCoreFromTier()) {
            String core = t.core().get(to);
            if (core == null) return null;
            out.add(new Entry(core, tier - s.convertCoreFromTier() + 1));
        }
        return List.copyOf(out);
    }

    /** 수리 재료 (갑옷 자신은 빼고). 핵심 부품이 없는 계열이면 null */
    public static List<Entry> repairCost(ArmorTable t, Settings s, String family, int tier) {
        if (tier < s.repairCoreFromTier()) return List.of(new Entry(s.repairItem(), Math.max(1, tier)));
        String core = t.core().get(family);
        if (core == null) return null;
        return List.of(new Entry(core, tier - s.repairCoreFromTier() + 1));
    }

    /** 계열 전환 작업대 크기: 1~3급 4×4, 4급부터 5×5 */
    public static int convertGrid(int tier) {
        return tier <= 3 ? 4 : 5;
    }
}
