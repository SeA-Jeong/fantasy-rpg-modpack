package com.fantasyrpg.core.gear.rune;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 룬 원석·하급 룬석 몬스터 드롭 (claude/룬석_재료드롭_초안.md 5절) */
class RuneDropRulesTest {
    private static RuneDropRules.Settings shipped() throws IOException {
        String t = Files.readString(Path.of("src/main/resources/data/fantasyrpg/rune_drops/settings.json"), StandardCharsets.UTF_8);
        return RuneDropRules.parse(JsonParser.parseString(t).getAsJsonObject());
    }

    @Test
    @DisplayName("배포 설정: 원석 5%, 하급 룬석 0.5%, 종류 8개(직업 룬석 포함) 모두 실제 룬석")
    void shippedSettings() throws IOException {
        RuneDropRules.Settings s = shipped();
        assertEquals(0.05, s.oreChance(), 1e-9);
        assertEquals(0.005, s.runeChance(), 1e-9);
        assertEquals(8, s.runeTypes().size());
        for (String t : s.runeTypes()) assertNotNull(RuneType.byId(t), t);
    }

    @Test
    @DisplayName("굴림: 확률보다 작을 때만, 종류는 고르게 (끝값 포함)")
    void roll() throws IOException {
        RuneDropRules.Settings s = shipped();
        assertTrue(RuneDropRules.ore(s, 0.049));
        assertFalse(RuneDropRules.ore(s, 0.05));
        assertNull(RuneDropRules.rune(s, 0.006, 0.5));
        assertEquals("life", RuneDropRules.rune(s, 0.001, 0.0));
        assertEquals("haste", RuneDropRules.rune(s, 0.001, 0.9999999));
    }

    @Test
    @DisplayName("잘못된 설정 거부: 확률 범위 밖, 종류 없이 룬석 확률")
    void reject() {
        assertThrows(IllegalArgumentException.class, () -> RuneDropRules.parse(j("{\"ore_chance\":1.5}")));
        assertThrows(IllegalArgumentException.class, () -> RuneDropRules.parse(j("{\"rune_chance\":0.1}")));
        assertEquals(0, RuneDropRules.parse(j("{}")).oreChance());
    }

    private static JsonObject j(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }
}
