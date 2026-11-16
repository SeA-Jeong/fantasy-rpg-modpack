package com.fantasyrpg.core.resist;

import com.fantasyrpg.core.combat.Resistances;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResistanceTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    @DisplayName("데이터팩 형식 견본과 같은 JSON을 읽는다")
    void parseExample() {
        MobResistanceProfile p = MobResistanceParser.parse(json("""
                { "entity": "fantasyrpg:corrupted_spirit_lord", "chapter": 7, "role": "chapter_boss",
                  "max_health": 600, "resist": { "physical": 0.25, "magic": 0.15 } }"""));
        assertEquals("fantasyrpg:corrupted_spirit_lord", p.entityId());
        assertEquals(MobRole.CHAPTER_BOSS, p.role());
        assertEquals(7, p.chapter());
        assertEquals(0.25, p.resist().physical(), 1e-9);
        assertEquals(0.15, p.resist().magic(), 1e-9);
    }

    @Test
    @DisplayName("생략한 필드는 기본값(일반 몬스터, 저항 0)")
    void parseDefaults() {
        MobResistanceProfile p = MobResistanceParser.parse(json("{ \"entity\": \"minecraft:zombie\" }"));
        assertEquals(MobRole.NORMAL, p.role());
        assertEquals(Resistances.NONE, p.resist());
    }

    @Test
    void parseRejectsMissingEntityAndBadRange() {
        assertThrows(IllegalArgumentException.class, () -> MobResistanceParser.parse(json("{}")));
        assertThrows(IllegalArgumentException.class, () -> MobResistanceParser.parse(
                json("{ \"entity\": \"a:b\", \"resist\": { \"physical\": 1.5 } }")));
    }

    @Test
    @DisplayName("5.1장: 챕터 보스 저항 차이 15%p / 20%p 경계")
    void bossGap() {
        assertEquals(ResistanceRules.GapCheck.OK, ResistanceRules.checkBossGap(
                MobRole.CHAPTER_BOSS, new Resistances(0.25, 0.10), 0.15, 0.20));
        assertEquals(ResistanceRules.GapCheck.EXCEPTION_RANGE, ResistanceRules.checkBossGap(
                MobRole.CHAPTER_BOSS, new Resistances(0.40, 0.20), 0.15, 0.20));
        assertEquals(ResistanceRules.GapCheck.VIOLATION, ResistanceRules.checkBossGap(
                MobRole.CHAPTER_BOSS, new Resistances(0.40, 0.10), 0.15, 0.20));
    }

    @Test
    @DisplayName("챕터 보스가 아니면 저항 차이가 커도 허용 (선택 던전·정예)")
    void nonBossUnlimited() {
        assertEquals(ResistanceRules.GapCheck.OK, ResistanceRules.checkBossGap(
                MobRole.ELITE, new Resistances(0.60, 0.0), 0.15, 0.20));
    }
}
