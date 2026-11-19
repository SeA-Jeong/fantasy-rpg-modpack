package com.fantasyrpg.core.gear.heirloom;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 유산 무기 (기획 v0.4 6.1, 2026-11-19 결정: 진행이 각성·강화 한계를 열고 재료로 등급을 올림) */
class HeirloomRulesTest {

    private static HeirloomSettings shipped() throws Exception {
        String text = Files.readString(Path.of("src/main/resources/data/fantasyrpg/heirloom/settings.json"), StandardCharsets.UTF_8);
        return HeirloomSettings.parse(JsonParser.parseString(text).getAsJsonObject());
    }

    @Test
    @DisplayName("강화 한계: 잠든 동안 1급, 깨어나면 진행 등급(1~10)")
    void cap() {
        assertEquals(1, HeirloomRules.cap(4, false));
        assertEquals(1, HeirloomRules.cap(1, true));
        assertEquals(4, HeirloomRules.cap(4, true));
        assertEquals(10, HeirloomRules.cap(12, true));
    }

    @Test
    @DisplayName("특성 고르기: 각성 단계가 1을 넘을 때마다 1개 — 오버월드 6급에서 5개")
    void picks() {
        assertEquals(0, HeirloomRules.picksTotal(6, false));
        assertEquals(0, HeirloomRules.picksTotal(1, true));
        assertEquals(5, HeirloomRules.picksTotal(6, true));
        assertEquals(2, HeirloomRules.picksLeft(4, true, Map.of("attack", 1)));
        assertEquals(0, HeirloomRules.picksLeft(2, true, Map.of("guard", 1)));
        assertEquals(HeirloomRules.PickResult.NONE_LEFT, HeirloomRules.canPick("attack", 2, true, Map.of("guard", 1)));
        assertEquals(HeirloomRules.PickResult.UNKNOWN_DIRECTION, HeirloomRules.canPick("fire", 6, true, Map.of()));
        assertEquals(HeirloomRules.PickResult.OK, HeirloomRules.canPick("finesse", 3, true, Map.of("spell", 1)));
    }

    @Test
    @DisplayName("강화 판정: 잠듦·한계·최고 등급·재료 부족")
    void enhance() throws Exception {
        HeirloomSettings s = shipped();
        assertEquals(HeirloomRules.EnhanceResult.NOT_AWAKENED, HeirloomRules.canEnhance(1, 3, false, s, 99));
        assertEquals(HeirloomRules.EnhanceResult.AT_CAP, HeirloomRules.canEnhance(3, 3, true, s, 99));
        assertEquals(HeirloomRules.EnhanceResult.NOT_ENOUGH, HeirloomRules.canEnhance(1, 3, true, s, 7));
        assertEquals(HeirloomRules.EnhanceResult.OK, HeirloomRules.canEnhance(1, 3, true, s, 8));
        assertEquals(HeirloomRules.EnhanceResult.MAX, HeirloomRules.canEnhance(10, 10, true, s, 99));
    }

    @Test
    @DisplayName("배포 수치: 4방향 특성, 2~10급 강화 비용 9단계, 특성 합산")
    void shippedValues() throws Exception {
        HeirloomSettings s = shipped();
        assertEquals(4, s.traits().size());
        assertEquals(9, s.enhance().size());
        assertEquals(8, s.enhance().get(2).count());
        assertEquals(0.12, s.bonus(Map.of("attack", 3), "physical_bonus"), 1e-9);
        assertEquals(0.04, s.bonus(Map.of("finesse", 2, "attack", 1), "crit_chance"), 1e-9);
        assertEquals(0.0, s.bonus(Map.of("attack", 3), "magic_bonus"), 1e-9);
    }

    @Test
    @DisplayName("각성 선물: 진행 등급이 오르면 새로 열린 등급 강화 재료 — 2~6급 합계가 오버월드 강화에 딱 맞음")
    void stageUpGift() throws Exception {
        HeirloomSettings s = shipped();
        assertEquals(null, HeirloomRules.stageUpGift(1, s));
        assertEquals("minecraft:iron_ingot", HeirloomRules.stageUpGift(2, s).item());
        int iron = 0, diamond = 0;
        for (int g = 2; g <= 6; g++) {
            HeirloomSettings.Cost c = HeirloomRules.stageUpGift(g, s);
            if (c.item().equals("minecraft:iron_ingot")) iron += c.count();
            else diamond += c.count();
        }
        assertEquals(24, iron);
        assertEquals(24, diamond);
    }

    @Test
    @DisplayName("잘못된 설정 거부: 모르는 방향, 특성 값 50% 초과, 강화 목표 1급")
    void invalid() {
        assertThrows(IllegalArgumentException.class, () -> HeirloomSettings.parse(JsonParser.parseString(
                "{\"traits\":{\"fire\":{\"x\":0.1}},\"enhance\":[]}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> HeirloomSettings.parse(JsonParser.parseString(
                "{\"traits\":{\"attack\":{\"physical_bonus\":0.9}},\"enhance\":[]}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> HeirloomSettings.parse(JsonParser.parseString(
                "{\"traits\":{},\"enhance\":[{\"to\":1,\"item\":\"a\",\"count\":1}]}").getAsJsonObject()));
    }
}
