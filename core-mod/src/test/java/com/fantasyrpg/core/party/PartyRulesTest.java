package com.fantasyrpg.core.party;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 파티 규칙 (2026-11-21) */
class PartyRulesTest {
    @Test
    @DisplayName("경험치 나누기: 혼자 그대로, 2명 각 60%, 4명 각 40% (보너스 +20%/명, 올림)")
    void xpShare() {
        assertEquals(100, PartyRules.xpShare(100, 1, 0.2));
        assertEquals(60, PartyRules.xpShare(100, 2, 0.2));
        assertEquals(40, PartyRules.xpShare(100, 4, 0.2));
        assertEquals(5, PartyRules.xpShare(7, 2, 0.2));      // 8.4 ÷ 2 = 4.2 → 올림 5
        assertEquals(0, PartyRules.xpShare(0, 3, 0.2));
        assertEquals(1, PartyRules.xpShare(1, 4, 0.0));
    }

    @Test
    @DisplayName("방벽 몫: 감소율 × 비율, 0~1로 자름")
    void buff() {
        assertEquals(0.15, PartyRules.buffReduction(0.3, 0.5), 1e-12);
        assertEquals(1.0, PartyRules.buffReduction(2, 1), 1e-12);
        assertEquals(0.0, PartyRules.buffReduction(-1, 0.5), 1e-12);
    }

    @Test
    @DisplayName("배포 데이터 party.json: 회복 16칸 ×0.5, 방벽 16칸 ×0.5, 경험치 48칸 +0.2, 잘못된 값은 거부")
    void shipped() throws IOException {
        String text = Files.readString(Path.of("src/main/resources/data/fantasyrpg/skill_settings/party.json"), StandardCharsets.UTF_8);
        PartyRules.Settings s = PartyRules.Settings.parse(JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("party"));
        assertEquals(PartyRules.Settings.DEFAULT, s);
        assertThrows(IllegalArgumentException.class, () -> new PartyRules.Settings(16, 1.5, 16, 0.5, 48, 0.2));
    }
}
