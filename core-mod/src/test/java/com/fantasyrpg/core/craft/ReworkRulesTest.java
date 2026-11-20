package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.craft.CraftRules.Entry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 분해·계열 전환 (2026-11-21) */
class ReworkRulesTest {
    private static JsonObject json() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/fantasyrpg/armor_crafting/costs.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    @DisplayName("배포 설정: 숫돌, 50%, 핵심 부품은 4급부터, 수리 강화합금·4급부터 핵심 부품")
    void settings() throws IOException {
        assertEquals(ReworkRules.Settings.DEFAULT, ReworkRules.Settings.parse(json()));
    }

    @Test
    @DisplayName("분해: 기사 흉갑 1급 = 철 1 (금 1×0.5 내림 0은 빠짐), 2급 = 철 1 + 강화합금 2 (골조 1×0.5 = 0)")
    void dismantle() throws IOException {
        ArmorTable t = CraftRules.parseArmor(json());
        ReworkRules.Settings s = ReworkRules.Settings.DEFAULT;
        assertEquals(Map.of("minecraft:iron_ingot", 1), ReworkRules.dismantle(t, s, "knight", "chestplate", 1));
        assertEquals(Map.of("minecraft:iron_ingot", 1, "fantasyrpg:reinforced_alloy", 2), ReworkRules.dismantle(t, s, "knight", "chestplate", 2));
        // 6급은 4~6급 핵심 부품 2+3+4 = 9 → 4
        assertEquals(Integer.valueOf(4), ReworkRules.dismantle(t, s, "knight", "chestplate", 6).get("fantasyrpg:sanctum_plate"));
    }

    @Test
    @DisplayName("수리: 1~3급 강화합금 × 등급, 4급부터 그 계열 핵심 부품 × (등급 − 3)")
    void repair() throws IOException {
        ArmorTable t = CraftRules.parseArmor(json());
        ReworkRules.Settings s = ReworkRules.Settings.parse(json());
        assertEquals(List.of(new Entry("fantasyrpg:reinforced_alloy", 1)), ReworkRules.repairCost(t, s, "knight", 1));
        assertEquals(List.of(new Entry("fantasyrpg:reinforced_alloy", 3)), ReworkRules.repairCost(t, s, "elf", 3));
        assertEquals(List.of(new Entry("fantasyrpg:sanctum_plate", 1)), ReworkRules.repairCost(t, s, "knight", 4));
        assertEquals(List.of(new Entry("fantasyrpg:rune_frame", 3)), ReworkRules.repairCost(t, s, "arcane", 6));
    }

    @Test
    @DisplayName("계열 전환: 1~3급은 바꿀 계열 1급 재료만, 4급부터 핵심 부품 × (등급 − 3), 같은 계열은 없음")
    void convert() throws IOException {
        ArmorTable t = CraftRules.parseArmor(json());
        ReworkRules.Settings s = ReworkRules.Settings.DEFAULT;
        List<Entry> low = ReworkRules.convertCost(t, s, "knight", "elf", "chestplate", 3);
        assertEquals(t.createCost("elf", "chestplate"), low);
        List<Entry> high = ReworkRules.convertCost(t, s, "knight", "elf", "chestplate", 5);
        assertEquals(new Entry("fantasyrpg:spirit_weave", 2), high.get(high.size() - 1));
        assertNull(ReworkRules.convertCost(t, s, "knight", "knight", "chestplate", 5));
        assertEquals(4, ReworkRules.convertGrid(3));
        assertEquals(5, ReworkRules.convertGrid(4));
    }
}
