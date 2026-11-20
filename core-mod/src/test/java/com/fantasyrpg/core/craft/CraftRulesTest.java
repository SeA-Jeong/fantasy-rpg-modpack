package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.craft.CraftRules.Entry;
import com.fantasyrpg.core.craft.CraftRules.GridRecipe;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 작업대·방어구 제작 규칙 (claude/작업대_방어구제작_초안.md) */
class CraftRulesTest {
    private static final Path DATA = Path.of("src/main/resources/data/fantasyrpg");

    private static ArmorTable armor() throws IOException {
        return CraftRules.parseArmor(JsonParser.parseString(Files.readString(DATA.resolve("armor_crafting/costs.json"), StandardCharsets.UTF_8)).getAsJsonObject());
    }

    private static List<GridRecipe> recipes() throws IOException {
        List<GridRecipe> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA.resolve("grid_recipes"))) {
            for (Path p : s.sorted().toList()) {
                String name = p.getFileName().toString().replace(".json", "");
                out.add(CraftRules.parseRecipe(name, JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject()));
            }
        }
        return out;
    }

    private static boolean same(Entry e, String item) {
        return e.id().equals(item);
    }

    @Test
    @DisplayName("모양 없는 조합: 종류별 개수가 정확히 맞아야 함 (모자라도·남아도 안 됨, 순서 상관없음)")
    void matching() {
        List<Entry> ing = List.of(new Entry("iron", 2), new Entry("copper", 1));
        assertTrue(CraftRules.matches(ing, List.of("copper", "iron", "iron"), CraftRulesTest::same));
        assertTrue(CraftRules.matches(ing, List.of("iron", "copper", "iron"), CraftRulesTest::same));
        assertFalse(CraftRules.matches(ing, List.of("iron", "copper"), CraftRulesTest::same));
        assertFalse(CraftRules.matches(ing, List.of("iron", "iron", "iron"), CraftRulesTest::same));
        assertFalse(CraftRules.matches(ing, List.of("iron", "iron", "copper", "gold"), CraftRulesTest::same));
        // 겹치는 재료(태그)도 맞는 배정을 찾음: "아무 금속" 1 + 철 1 ← [철, 구리]
        List<Entry> overlap = List.of(new Entry("#metal", 1), new Entry("iron", 1));
        assertTrue(CraftRules.matches(overlap, List.of("iron", "copper"),
                (e, it) -> e.isTag() ? (it.equals("iron") || it.equals("copper")) : e.id().equals(it)));
    }

    @Test
    @DisplayName("부위 비율: 흉갑 8 · 각반 7 · 투구 5 · 장화 4, 올림, 최소 1")
    void partScaling() {
        assertEquals(4, CraftRules.scaled(4, "chestplate"));
        assertEquals(4, CraftRules.scaled(4, "leggings"));
        assertEquals(3, CraftRules.scaled(4, "helmet"));
        assertEquals(2, CraftRules.scaled(4, "boots"));
        assertEquals(1, CraftRules.scaled(1, "boots"));
        assertEquals(4, CraftRules.gridForTier(3));
        assertEquals(5, CraftRules.gridForTier(4));
    }

    @Test
    @DisplayName("배포 방어구 제작표: 5계열 × 4부위 × 2~7급 승급과 1급 만들기가 모두 있고 작업대 칸 안에 들어감")
    void shippedArmor() throws IOException {
        ArmorTable t = armor();
        assertEquals(5, t.weight().size());
        for (String f : List.of("knight", "dwarf", "dragon", "elf", "arcane")) {
            for (String part : List.of("helmet", "chestplate", "leggings", "boots")) {
                assertNotNull(t.createCost(f, part), f + " " + part + " 1급");
                for (int tier = 2; tier <= CraftRules.MAX_UPGRADE; tier++) {
                    List<Entry> c = t.upgradeCost(f, part, tier);
                    assertNotNull(c, f + " " + part + " " + tier);
                    int slots = 1;
                    for (Entry e : c) {
                        assertFalse(e.id().equals("core"));
                        slots += e.count();
                    }
                    int g = CraftRules.gridForTier(tier);
                    assertTrue(slots <= g * g, f + " " + part + " " + tier + "급 " + slots + "칸");
                }
            }
        }
        // 기사 6급 흉갑 = 성역 장갑판 4 + 장갑판 4 + 정밀 2 + 골조 1
        List<Entry> k6 = t.upgradeCost("knight", "chestplate", 6);
        assertEquals("fantasyrpg:sanctum_plate", k6.get(0).id());
        assertEquals(4, k6.get(0).count());
        // 7급은 아직 없음 (네더)
        // 7급(오버월드 최대, 2026-11-21) = 성역 장갑판 5 + 장갑판 5 + 정밀 3 + 골조 2
        assertEquals(5, t.upgradeCost("knight", "chestplate", 7).get(0).count());
        assertEquals(null, t.upgradeCost("knight", "chestplate", 8));
    }

    @Test
    @DisplayName("배포 작업대 조합식: 모두 읽히고, 결과가 겹치지 않고, 재료로 쓰는 우리 부품은 모두 만드는 법이 있거나 보스 소재")
    void shippedRecipes() throws IOException {
        List<GridRecipe> rs = recipes();
        assertEquals(23, rs.size());   // 부품 13 + 중급 룬석 8 (직업 룬석 2 포함) + 중급·상급 연마재 2 (2026-11-21)
        Set<String> results = new HashSet<>();
        for (GridRecipe r : rs) assertTrue(results.add(r.resultKey()), "결과 겹침: " + r.resultKey());
        for (GridRecipe r : rs) results.add(r.result());
        Set<String> drops = Set.of("fantasyrpg:sanctum_crystal", "fantasyrpg:dwarf_steel", "fantasyrpg:dragon_scale",
                "fantasyrpg:moonsilver_thread", "fantasyrpg:ancient_glyphstone", "fantasyrpg:workbench_4x4",
                "fantasyrpg:rune_ore");   // 룬 원석 = 몬스터·보스 드롭
        Map<String, Integer> gradeOf = new HashMap<>();
        for (GridRecipe r : rs) gradeOf.put(r.result(), r.grade());
        for (GridRecipe r : rs) {
            for (Entry e : r.ingredients()) {
                if (!e.id().startsWith("fantasyrpg:")) continue;
                assertTrue(results.contains(e.id()) || drops.contains(e.id()), r.name() + " 재료 " + e.id() + " 얻을 곳 없음");
                // 재료를 만드는 등급이 결과 등급보다 높으면 안 됨
                if (gradeOf.containsKey(e.id())) assertTrue(gradeOf.get(e.id()) <= r.grade(), r.name() + " ← " + e.id());
            }
        }
        ArmorTable t = armor();
        for (Map<Integer, List<Entry>> byTier : t.upgrade().values()) {
            for (var en : byTier.entrySet()) {
                for (Entry e : en.getValue()) {
                    if (e.id().equals("core")) continue;
                    assertTrue(results.contains(e.id()), "승급 재료 " + e.id() + " 만드는 법 없음");
                    assertTrue(gradeOf.get(e.id()) <= en.getKey(), en.getKey() + "급 승급 재료 " + e.id() + " 가 더 높은 등급");
                }
            }
        }
        for (String core : t.core().values()) assertTrue(results.contains(core), core);
    }

    @Test
    @DisplayName("잘못된 데이터 거부: 작업대 크기, 칸 수 초과, 개수 0")
    void invalid() {
        assertThrows(IllegalArgumentException.class, () -> CraftRules.parseRecipe("x", JsonParser.parseString(
                "{\"grid\":6,\"ingredients\":[{\"item\":\"a\"}],\"result\":{\"item\":\"b\"}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> CraftRules.parseRecipe("x", JsonParser.parseString(
                "{\"grid\":3,\"ingredients\":[{\"item\":\"a\",\"count\":10}],\"result\":{\"item\":\"b\"}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> CraftRules.parseRecipe("x", JsonParser.parseString(
                "{\"grid\":4,\"ingredients\":[{\"item\":\"a\",\"count\":0}],\"result\":{\"item\":\"b\"}}").getAsJsonObject()));
    }
}
