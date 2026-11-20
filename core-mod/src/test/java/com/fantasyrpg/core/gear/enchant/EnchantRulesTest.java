package com.fantasyrpg.core.gear.enchant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 바닐라 인챈트 정리 (정책 P2·P2-1, claude/바닐라인챈트_정리_초안.md) */
class EnchantRulesTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    private static JsonObject json(String rel) throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve(rel), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    @DisplayName("보호: 보호 III 4부위 36%, 특수 보호 II 4부위 48%, 최대치를 넘는 레벨은 최대치만큼")
    void protection() {
        assertEquals(36.0, EnchantRules.percent(4 * EnchantRules.points(3, 0.75, 3)), 1e-9);
        assertEquals(48.0, EnchantRules.percent(4 * EnchantRules.points(2, 1.5, 2)), 1e-9);
        assertEquals(EnchantRules.points(3, 0.75, 3), EnchantRules.points(4, 0.75, 3), 1e-9);   // 보호 IV = III
        assertEquals(EnchantRules.points(2, 1.5, 2), EnchantRules.points(4, 1.5, 2), 1e-9);
        assertEquals(80.0, EnchantRules.percent(25), 1e-9);                                       // 바닐라 상한
        assertEquals(22, EnchantRules.DISABLED.size());
        for (String k : EnchantRules.KEPT) assertFalse(EnchantRules.disabled(k), k);
    }

    @Test
    @DisplayName("데이터: 전투 인챈트 22종은 효과가 비고, 보호 4종은 최대 레벨·상한이 규칙과 같음")
    void definitions() throws IOException {
        for (String id : EnchantRules.DISABLED) {
            JsonObject e = json("minecraft/enchantment/" + id + ".json");
            assertTrue(e.getAsJsonObject("effects").size() == 0, id + " 효과가 남음");
            assertTrue(e.has("description") && e.has("max_level") && e.has("supported_items"), id + " 필수 항목");
        }
        check("protection", 3, 0.75);
        check("fire_protection", 2, 1.5);
        check("blast_protection", 2, 1.5);
        check("projectile_protection", 2, 1.5);
    }

    private static void check(String id, int max, double per) throws IOException {
        JsonObject e = json("minecraft/enchantment/" + id + ".json");
        assertEquals(max, e.get("max_level").getAsInt(), id);
        JsonObject v = e.getAsJsonObject("effects").getAsJsonArray("minecraft:damage_protection").get(0).getAsJsonObject()
                .getAsJsonObject("effect").getAsJsonObject("value");
        assertEquals("minecraft:clamped", v.get("type").getAsString(), id);
        assertEquals(per * max, v.get("max").getAsDouble(), 1e-9, id);
        assertEquals(per, v.getAsJsonObject("value").get("base").getAsDouble(), 1e-9, id);
        assertEquals(per, v.getAsJsonObject("value").get("per_level_above_first").getAsDouble(), 1e-9, id);
    }

    @Test
    @DisplayName("데이터: 획득 태그 5개에 전투 인챈트 없음(통째로 바꿈), 보호·내구성은 남음, 지우기 태그 = 22종")
    void tags() throws IOException {
        for (String t : new String[]{"in_enchanting_table", "on_mob_spawn_equipment", "on_traded_equipment", "on_random_loot", "tradeable"}) {
            JsonObject o = json("minecraft/tags/enchantment/" + t + ".json");
            assertTrue(o.get("replace").getAsBoolean(), t);
            Set<String> vals = new HashSet<>();
            for (JsonElement v : o.getAsJsonArray("values")) vals.add(v.getAsString());
            for (String id : EnchantRules.DISABLED) assertFalse(vals.contains("minecraft:" + id), t + " 에 " + id);
            assertFalse(vals.stream().anyMatch(v -> v.startsWith("#")), t + " 에 다른 태그 참조(전투 인챈트가 섞일 수 있음)");
            assertTrue(vals.contains("minecraft:protection") && vals.contains("minecraft:unbreaking"), t);
        }
        JsonArray d = json("fantasyrpg/tags/enchantment/disabled_combat.json").getAsJsonArray("values");
        Set<String> ids = new HashSet<>();
        for (JsonElement v : d) ids.add(v.getAsString().substring("minecraft:".length()));
        assertEquals(new HashSet<>(EnchantRules.DISABLED), ids);
    }
}
