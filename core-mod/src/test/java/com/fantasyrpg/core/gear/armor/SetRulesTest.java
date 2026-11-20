package com.fantasyrpg.core.gear.armor;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 계열 방어구 세트 효과 (claude/세트효과_초안.md) */
class SetRulesTest {
    @Test
    @DisplayName("판정: 2세트 = 높은 2개 중 낮은 등급, 4세트 = 가장 낮은 등급, 1부위는 꺼짐")
    void worn() {
        SetRules.Worn one = SetRules.worn(List.of(5));
        assertFalse(one.two());
        SetRules.Worn three = SetRules.worn(Arrays.asList(2, 6, 4));
        assertEquals(4, three.grade2());
        assertFalse(three.four());
        SetRules.Worn four = SetRules.worn(Arrays.asList(6, 3, 6, 5));
        assertEquals(6, four.grade2());
        assertEquals(3, four.grade4());
        assertEquals(4, four.pieces());
    }

    @Test
    @DisplayName("크기 배율: 1~3급 ×1, 4~6급 ×1.5, 7~10급 ×2, 시간 값은 그대로")
    void band() {
        assertEquals(1.0, SetRules.band(3));
        assertEquals(1.5, SetRules.band(4));
        assertEquals(1.5, SetRules.band(6));
        assertEquals(2.0, SetRules.band(10));
        assertEquals(0.0, SetRules.band(0));
        var m = java.util.Map.of("reflect_ratio", 0.2, "reflect_cooldown", 40.0);
        assertEquals(0.3, SetRules.value(m, "reflect_ratio", 5), 1e-9);
        assertEquals(40.0, SetRules.value(m, "reflect_cooldown", 5), 1e-9);
        assertEquals(0.0, SetRules.value(m, "reflect_ratio", 0), 1e-9);
    }

    @Test
    @DisplayName("배포 데이터: 5계열 모두 읽히고, 피해 감소는 고룡 화염(≤0.5)·기사 흡수 체력뿐")
    void shipped() throws IOException {
        for (ArmorFamily f : ArmorFamily.values()) {
            String t = Files.readString(Path.of("src/main/resources/data/fantasyrpg/armor_sets/" + f.id() + ".json"), StandardCharsets.UTF_8);
            SetRules.SetDef d = SetRules.parse(f.id(), JsonParser.parseString(t).getAsJsonObject());
            assertFalse(d.two().isEmpty(), f.id());
            assertFalse(d.four().isEmpty(), f.id());
            for (String k : d.two().keySet()) assertFalse(k.contains("damage_reduction"), f.id() + " " + k);
            for (String k : d.four().keySet()) assertFalse(k.contains("damage_reduction"), f.id() + " " + k);
        }
        String dragon = Files.readString(Path.of("src/main/resources/data/fantasyrpg/armor_sets/dragon.json"), StandardCharsets.UTF_8);
        SetRules.SetDef d = SetRules.parse("dragon", JsonParser.parseString(dragon).getAsJsonObject());
        assertTrue(SetRules.value(d.two(), "fire_reduction", 10) <= 0.5);
    }

    @Test
    @DisplayName("잘못된 데이터 거부: two·four 없음, 음수, 모르는 항목")
    void reject() {
        assertThrows(IllegalArgumentException.class, () -> SetRules.parse("x", JsonParser.parseString("{\"two\":{}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> SetRules.parse("x", JsonParser.parseString("{\"two\":{\"a\":-1},\"four\":{}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> SetRules.parse("x", JsonParser.parseString("{\"two\":{},\"four\":{},\"six\":{}}").getAsJsonObject()));
    }
}
