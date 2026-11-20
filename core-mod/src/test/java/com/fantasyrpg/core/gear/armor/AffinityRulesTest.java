package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.gear.armor.AffinityRules.Affinity;
import com.fantasyrpg.core.gear.armor.AffinityRules.Piece;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 방어구 성향 (2026-11-21, claude/방어구성향_초안.md) */
class AffinityRulesTest {
    private static AffinityRules.Settings shipped() throws IOException {
        return AffinityRules.parse(JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/data/fantasyrpg/armor_affinity/settings.json"), StandardCharsets.UTF_8)).getAsJsonObject());
    }

    @Test
    @DisplayName("배포 설정: 5성향, 구간 ×1/1.5/2, 공명 4부위 + SP 10 → ×1.25, 재조율 각인석 1/2/3")
    void settings() throws IOException {
        AffinityRules.Settings s = shipped();
        assertEquals(5, s.options().size());
        assertEquals(0.02, s.options().get(Affinity.ATTACK).get("physical_bonus"), 1e-12);
        assertEquals(1, AffinityRules.retuneStones(s, 3));
        assertEquals(2, AffinityRules.retuneStones(s, 4));
        assertEquals(3, AffinityRules.retuneStones(s, 10));
    }

    @Test
    @DisplayName("합계: 공격 4부위 2급 = 물리 +8%, 공명(공격 SP 10)이면 +10%, 5급이면 ×1.5")
    void total() throws IOException {
        AffinityRules.Settings s = shipped();
        List<Piece> four = List.of(new Piece(Affinity.ATTACK, 2), new Piece(Affinity.ATTACK, 2), new Piece(Affinity.ATTACK, 2), new Piece(Affinity.ATTACK, 2));
        assertEquals(0.08, AffinityRules.total(s, four, Map.of(Affinity.ATTACK, 9)).get("physical_bonus"), 1e-9);
        assertEquals(0.10, AffinityRules.total(s, four, Map.of(Affinity.ATTACK, 10)).get("physical_bonus"), 1e-9);
        List<Piece> five = List.of(new Piece(Affinity.ATTACK, 5), new Piece(Affinity.ATTACK, 5), new Piece(Affinity.ATTACK, 5), new Piece(Affinity.ATTACK, 5));
        assertEquals(0.12, AffinityRules.total(s, five, Map.of()).get("physical_bonus"), 1e-9);
    }

    @Test
    @DisplayName("섞어 입기·균형: 수호 2 + 기교 2는 공명 없음, 균형은 SP 가 많아도 공명 없음")
    void mixed() throws IOException {
        AffinityRules.Settings s = shipped();
        Map<String, Double> t = AffinityRules.total(s, List.of(new Piece(Affinity.GUARD, 1), new Piece(Affinity.GUARD, 1),
                new Piece(Affinity.FINESSE, 1), new Piece(Affinity.FINESSE, 1)), Map.of(Affinity.GUARD, 24, Affinity.FINESSE, 24));
        assertEquals(2.0, t.get("max_health"), 1e-9);
        assertEquals(0.02, t.get("crit_chance"), 1e-9);
        assertEquals(2.0, t.get("skill_haste"), 1e-9);
        assertFalse(AffinityRules.resonates(s, Affinity.BALANCE, 4, 99));
        assertTrue(AffinityRules.resonates(s, Affinity.SPELL, 4, 10));
        assertEquals(Affinity.BALANCE, Affinity.byId(null));
        assertEquals(Affinity.SPELL, Affinity.byId("spell"));
    }
}
