package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.fantasyrpg.core.gear.armor.ArmorStatParser;
import com.fantasyrpg.core.gear.armor.ArmorStatTable;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 룬석 규칙 (claude/계열방어구_상위옵션_초안.md 3장). 데이터는 모드 jar 기본값
 * core-mod/src/main/resources/data/fantasyrpg/runes/*.json, armor_stats/*.json 을 읽는다.
 */
class RuneRulesTest {
    private static final Path DATA = Path.of("src/main/resources/data/fantasyrpg");

    private static Map<RuneType, RuneDefinition> loadAll() throws IOException {
        Map<RuneType, RuneDefinition> out = new EnumMap<>(RuneType.class);
        for (RuneType t : RuneType.values()) {
            String text = Files.readString(DATA.resolve("runes/" + t.id() + ".json"), StandardCharsets.UTF_8);
            RuneDefinition d = RuneDefinitionParser.parse(JsonParser.parseString(text).getAsJsonObject());
            assertEquals(t, d.type());
            out.put(t, d);
        }
        return out;
    }

    @Test
    @DisplayName("데이터 8종: 문서 값·부위 (생명 투구·흉갑 +1~4, 질풍 신발 +2~5%·경갑 +1%p, 등반 +0.5)")
    void dataFiles() throws IOException {
        Map<RuneType, RuneDefinition> d = loadAll();
        assertEquals(4.0, d.get(RuneType.LIFE).value(4, false));
        assertEquals(1.0, d.get(RuneType.LIFE).value(1, false));
        assertTrue(d.get(RuneType.LIFE).fits(ArmorPart.HEAD));
        assertTrue(d.get(RuneType.LIFE).fits(ArmorPart.CHEST));
        assertFalse(d.get(RuneType.LIFE).fits(ArmorPart.FEET));
        assertEquals(2.0, d.get(RuneType.GUARD).value(4, false));
        assertEquals(0.10, d.get(RuneType.STEADY).value(4, false), 1e-12);
        assertEquals(0.05, d.get(RuneType.GALE).value(4, false), 1e-12);
        assertEquals(0.06, d.get(RuneType.GALE).value(4, true), 1e-12);
        assertEquals("add_multiplied_base", d.get(RuneType.GALE).operation());
        assertTrue(d.get(RuneType.GALE).fits(ArmorPart.FEET));
        assertEquals(0.5, d.get(RuneType.CLIMB).value(1, false));
        assertEquals(0.5, d.get(RuneType.CLIMB).value(4, false));
    }

    @Test
    @DisplayName("소켓 수: 1~3급 1칸, 4~6급 2칸, 7~10급 3칸 (5계열 모두)")
    void socketCounts() throws IOException {
        for (String f : new String[]{"knight", "dwarf", "dragon", "elf", "arcane"}) {
            String text = Files.readString(DATA.resolve("armor_stats/" + f + ".json"), StandardCharsets.UTF_8);
            ArmorStatTable t = ArmorStatParser.parse(JsonParser.parseString(text).getAsJsonObject());
            for (int tier = 1; tier <= 10; tier++) {
                int expected = tier <= 3 ? 1 : tier <= 6 ? 2 : 3;
                assertEquals(expected, t.tier(tier).sockets(), f + " " + tier + "급");
            }
        }
    }

    @Test
    @DisplayName("끼우기: 부위가 맞아야 하고 빈 칸이 있어야 함. 갑옷 등급과 룬석 등급은 무관")
    void insert() throws IOException {
        RuneDefinition life = loadAll().get(RuneType.LIFE);
        assertEquals(RuneRules.InsertResult.OK, RuneRules.canInsert(life, ArmorPart.HEAD, 0, 1));
        assertEquals(RuneRules.InsertResult.NO_FREE_SOCKET, RuneRules.canInsert(life, ArmorPart.HEAD, 1, 1));
        assertEquals(RuneRules.InsertResult.WRONG_PART, RuneRules.canInsert(life, ArmorPart.FEET, 0, 3));
    }

    @Test
    @DisplayName("직업 룬석: 순환 투구·흉갑 +5/8/12/16%, 가속 투구·각반 +2/4/6/8, 경갑(엘프·마도)에만")
    void classRunes() throws IOException {
        Map<RuneType, RuneDefinition> d = loadAll();
        RuneDefinition cycle = d.get(RuneType.CYCLE), haste = d.get(RuneType.HASTE);
        assertEquals("fantasyrpg:core_regen", cycle.attribute());
        assertEquals(0.16, cycle.value(4, true), 1e-12);
        assertEquals(0.05, cycle.value(1, false), 1e-12);
        assertEquals("fantasyrpg:skill_haste", haste.attribute());
        assertEquals(8.0, haste.value(4, true));
        assertTrue(cycle.fits(ArmorPart.CHEST));
        assertTrue(haste.fits(ArmorPart.LEGS));
        assertFalse(haste.fits(ArmorPart.CHEST));
        assertTrue(cycle.lightOnly());
        assertFalse(d.get(RuneType.LIFE).lightOnly());
        assertEquals(RuneRules.InsertResult.LIGHT_ONLY, RuneRules.canInsert(haste, ArmorPart.HEAD, 0, 2, false));
        assertEquals(RuneRules.InsertResult.OK, RuneRules.canInsert(haste, ArmorPart.HEAD, 0, 2, true));
        assertEquals(RuneRules.InsertResult.WRONG_PART, RuneRules.canInsert(haste, ArmorPart.CHEST, 0, 2, true));
        assertEquals(RuneRules.InsertResult.OK, RuneRules.canInsert(d.get(RuneType.LIFE), ArmorPart.HEAD, 0, 1, false));
    }

    @Test
    @DisplayName("합계 상한 없음: 10급 투구·흉갑 6칸 생명 최고급 = 체력 +24, 칸 수를 넘는 룬석은 효과 없음")
    void stackingNoCap() throws IOException {
        Map<RuneType, RuneDefinition> d = loadAll();
        List<RuneRules.Socketed> three = List.of(
                new RuneRules.Socketed(RuneType.LIFE, 4), new RuneRules.Socketed(RuneType.LIFE, 4),
                new RuneRules.Socketed(RuneType.LIFE, 4));
        double total = 0;
        for (ArmorPart part : new ArmorPart[]{ArmorPart.HEAD, ArmorPart.CHEST}) {
            for (RuneRules.Active a : RuneRules.active(three, 3, part, d::get)) total += a.def().value(a.grade(), false);
        }
        assertEquals(24.0, total);
        // 소켓 2칸으로 줄면 세 번째는 효과 없음, 칸 번호는 1부터
        List<RuneRules.Active> two = RuneRules.active(three, 2, ArmorPart.HEAD, d::get);
        assertEquals(2, two.size());
        assertEquals(1, two.get(0).slot());
        assertEquals(2, two.get(1).slot());
    }

    @Test
    @DisplayName("빼기는 마지막부터, 합성 3개 → +1등급, 연마는 같은 등급 연마재로 +1, 최고급은 더 못 올림")
    void extractCombinePolish() {
        List<RuneRules.Socketed> s = RuneRules.insert(
                RuneRules.insert(List.of(), new RuneRules.Socketed(RuneType.LIFE, 1)),
                new RuneRules.Socketed(RuneType.GUARD, 2));
        List<RuneRules.Socketed> after = RuneRules.removeLast(s);
        assertEquals(List.of(new RuneRules.Socketed(RuneType.LIFE, 1)), after);
        assertEquals(List.of(), RuneRules.removeLast(List.of()));

        assertEquals(2, RuneRules.combineResult(1, 3));
        assertEquals(4, RuneRules.combineResult(3, 3));
        assertEquals(0, RuneRules.combineResult(4, 3));
        assertEquals(0, RuneRules.combineResult(1, 2));

        assertEquals(2, RuneRules.polishResult(1, 1));
        assertEquals(4, RuneRules.polishResult(3, 3));
        assertEquals(0, RuneRules.polishResult(2, 1));
        assertEquals(0, RuneRules.polishResult(4, 3));
    }

    @Test
    @DisplayName("검사: 값 3개·값 감소·모르는 부위·모르는 연산은 거부")
    void rejects() {
        String ok = "{\"rune\":\"life\",\"attribute\":\"minecraft:generic.max_health\",\"operation\":\"add_value\","
                + "\"parts\":[\"helmet\"],\"values\":[1,2,3,4]}";
        RuneDefinitionParser.parse(JsonParser.parseString(ok).getAsJsonObject());
        for (String bad : new String[]{
                ok.replace("[1,2,3,4]", "[1,2,3]"),
                ok.replace("[1,2,3,4]", "[1,3,2,4]"),
                ok.replace("\"helmet\"", "\"gloves\""),
                ok.replace("add_value", "multiply"),
                ok.replace("\"life\"", "\"abyss\"")}) {
            assertThrows(IllegalArgumentException.class,
                    () -> RuneDefinitionParser.parse(JsonParser.parseString(bad).getAsJsonObject()), bad);
        }
    }
}
