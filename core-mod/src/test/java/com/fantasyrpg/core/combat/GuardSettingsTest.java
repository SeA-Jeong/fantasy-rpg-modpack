package com.fantasyrpg.core.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 기사 기본 가드·패리 (기획 v0.4 5.3, 2026-11-19 결정, claude/가드_프리셋_초안.md 1절) */
class GuardSettingsTest {

    private static GuardSettings shipped() throws Exception {
        String text = Files.readString(Path.of("src/main/resources/data/fantasyrpg/combat/guard.json"), StandardCharsets.UTF_8);
        return GuardSettings.parse(JsonParser.parseString(text).getAsJsonObject());
    }

    @Test
    @DisplayName("배포 수치: 감소 30%, 패리 6틱(기획 5.3 시험값, 5~8 범위), 재무장 20틱, 스킬 완벽 방어 1랭크(50%)보다 약함")
    void shippedValues() throws Exception {
        GuardSettings g = shipped();
        assertEquals(0.3, g.reduction(), 1e-9);
        assertEquals(6, g.parryTicks());
        assertTrue(g.parryTicks() >= 5 && g.parryTicks() <= 8);
        assertEquals(20, g.rearmTicks());
        assertTrue(g.reduction() < 0.5);
        assertEquals(7.0, g.guarded(10.0), 1e-9);
    }

    @Test
    @DisplayName("패리: 들어 올린 뒤 6틱 안이면 성공, 7틱이면 실패")
    void parryWindow() throws Exception {
        GuardSettings g = shipped();
        assertTrue(g.isParry(100, 100, -1000));
        assertTrue(g.isParry(106, 100, -1000));
        assertFalse(g.isParry(107, 100, -1000));
    }

    @Test
    @DisplayName("연타 방지: 직전에 들어 올린 지 20틱이 안 됐으면 패리 없음")
    void rearm() throws Exception {
        GuardSettings g = shipped();
        assertFalse(g.isParry(100, 100, 90));   // 0.5초 만에 다시 올림
        assertTrue(g.isParry(100, 100, 80));    // 정확히 1초
    }

    @Test
    @DisplayName("잘못된 수치 거부: 감소 95% 초과, 패리 틱 음수")
    void invalid() {
        JsonObject o = JsonParser.parseString("{\"reduction\":0.99,\"parry_ticks\":6}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> GuardSettings.parse(o));
        JsonObject o2 = JsonParser.parseString("{\"reduction\":0.3,\"parry_ticks\":-1}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> GuardSettings.parse(o2));
    }
}
