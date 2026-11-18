package com.fantasyrpg.core.gear.armor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 계열 방어구 수치표 (claude/장비등급제한_설계.md v3, 2026-11-18 확정).
 * 데이터 파일 검사는 모드 jar에 들어가는 기본값 core-mod/src/main/resources/data/fantasyrpg/armor_stats/*.json 을 읽는다.
 */
class ArmorStatTableTest {
    private static final Path DATA = Path.of("src/main/resources/data/fantasyrpg/armor_stats");
    private static final String[] FAMILIES = {"knight", "dwarf", "dragon", "elf", "arcane"};

    /** 같은 단계 바닐라 갑옷 (원래 수치): 1급 사슬, 2급 철, 3급 다이아, 7급 네더라이트 */
    private static final Map<Integer, int[]> VANILLA = Map.of(
            1, new int[]{12, 0}, 2, new int[]{15, 0}, 3, new int[]{20, 8}, 7, new int[]{20, 12});

    private static ArmorStatTable load(String family) throws IOException {
        String text = Files.readString(DATA.resolve(family + ".json"), StandardCharsets.UTF_8);
        return ArmorStatParser.parse(JsonParser.parseString(text).getAsJsonObject());
    }

    @Test
    @DisplayName("부위 배분 3:8:6:3 — 30 → 5/12/9/4, 12 → 2/5/3/2, 1 → 0/1/0/0, 합계 보존")
    void split() {
        assertArrayEquals(new int[]{5, 12, 9, 4}, PartSplit.split(30));
        assertArrayEquals(new int[]{2, 5, 3, 2}, PartSplit.split(12));
        assertArrayEquals(new int[]{0, 1, 0, 0}, PartSplit.split(1));
        assertArrayEquals(new int[]{2, 5, 4, 2}, PartSplit.split(13));
        for (int total = 0; total <= 40; total++) {
            int[] s = PartSplit.split(total);
            assertEquals(total, s[0] + s[1] + s[2] + s[3], "합계 " + total);
        }
        assertArrayEquals(new double[]{0.11, 0.28, 0.21, 0.1}, PartSplit.splitHundredths(0.70), 1e-12);
    }

    @Test
    @DisplayName("데이터 5계열: 읽기·검사 통과, 문서 표와 같은 10급 값")
    void dataFilesLoad() throws IOException {
        for (String f : FAMILIES) {
            ArmorStatTable t = load(f);
            assertEquals(f, t.family());
            assertEquals(10, t.tiers().size());
        }
        assertEquals(new ArmorStatTable.Tier(10, 30, 16, 0.35), load("knight").tier(10));
        assertEquals(new ArmorStatTable.Tier(10, 30, 16, 0.40), load("dwarf").tier(10));
        assertEquals(new ArmorStatTable.Tier(10, 30, 15, 0.30), load("dragon").tier(10));
        assertEquals(new ArmorStatTable.Tier(10, 24, 12, 0.0), load("elf").tier(10));
        assertEquals(load("elf").tiers(), load("arcane").tiers());
    }

    @Test
    @DisplayName("기사·드워프는 같은 단계 바닐라보다 방어력·강도 모두 높고, 고룡은 방어력이 높고 강도는 같거나 높다")
    void heavyBeatsVanilla() throws IOException {
        for (String f : new String[]{"knight", "dwarf", "dragon"}) {
            ArmorStatTable t = load(f);
            boolean elemental = f.equals("dragon"); // 중갑-속성: 강도가 중갑보다 1 낮다 (3급 21/8 = 다이아 강도 8, 7급 26/12 = 네더라이트 강도 12)
            for (Map.Entry<Integer, int[]> v : VANILLA.entrySet()) {
                ArmorStatTable.Tier tier = t.tier(v.getKey());
                assertTrue(tier.armor() > v.getValue()[0], f + " " + v.getKey() + "급 방어력");
                if (elemental) {
                    assertTrue(tier.toughness() >= v.getValue()[1], f + " " + v.getKey() + "급 강도");
                } else {
                    assertTrue(tier.toughness() > v.getValue()[1], f + " " + v.getKey() + "급 강도");
                }
            }
        }
    }

    @Test
    @DisplayName("부위 값 예: 기사 10급 방어 5/12/9/4·강도 2/7/5/2, 엘프 3급 3/7/6/3·1/2/2/1")
    void partValues() throws IOException {
        ArmorStatTable knight = load("knight");
        int[] armor = new int[4];
        int[] tough = new int[4];
        for (ArmorPart p : ArmorPart.values()) {
            armor[p.ordinal()] = knight.partArmor(10, p);
            tough[p.ordinal()] = knight.partToughness(10, p);
        }
        assertArrayEquals(new int[]{5, 12, 9, 4}, armor);
        assertArrayEquals(new int[]{2, 7, 5, 2}, tough);
        ArmorStatTable elf = load("elf");
        assertEquals(3, elf.partArmor(3, ArmorPart.HEAD));
        assertEquals(7, elf.partArmor(3, ArmorPart.CHEST));
        assertEquals(2, elf.partToughness(3, ArmorPart.LEGS));
        // 범위 밖 등급은 1~10으로 맞춘다
        assertEquals(knight.tier(10), knight.tier(99));
        assertEquals(knight.tier(1), knight.tier(0));
    }

    @Test
    @DisplayName("받는 피해: 바닐라 원래 수치 다이아 20/8·네더라이트 20/12와 계열 갑옷 비교 (문서 2-3 표)")
    void damageTable() {
        assertEquals(3.0, ArmorDamageMath.taken(10, 20, 8), 1e-3);
        assertEquals(8.0, ArmorDamageMath.taken(20, 20, 8), 1e-3);
        assertEquals(7.2, ArmorDamageMath.taken(20, 20, 12), 1e-3);
        assertEquals(20.8, ArmorDamageMath.taken(40, 20, 12), 1e-3);
        assertEquals(4.0, ArmorDamageMath.taken(20, 26, 13), 1e-3);
        assertEquals(10.59, ArmorDamageMath.taken(40, 26, 13), 1e-2);
        assertEquals(8.0, ArmorDamageMath.taken(40, 30, 16), 1e-3);
        assertEquals(12.0, ArmorDamageMath.taken(60, 30, 16), 1e-3);
        assertEquals(31.2, ArmorDamageMath.taken(60, 24, 12), 1e-3);
        // 게임 안 확인값(2026-11-18): 철 15/0 → 6.0, 사슬 12/0 → 7.2
        assertEquals(6.0, ArmorDamageMath.taken(10, 15, 0), 1e-3);
        assertEquals(7.2, ArmorDamageMath.taken(10, 12, 0), 1e-3);
    }

    private static JsonObject table(String tiersJson) {
        return JsonParser.parseString("{\"family\":\"x\",\"tiers\":[" + tiersJson + "]}").getAsJsonObject();
    }

    private static String rows(int armor10, int toughness10) {
        StringBuilder sb = new StringBuilder();
        for (int t = 1; t <= 10; t++) {
            if (t > 1) sb.append(',');
            int a = t == 10 ? armor10 : 10 + t;
            int g = t == 10 ? toughness10 : t;
            sb.append("{\"tier\":").append(t).append(",\"armor\":").append(a).append(",\"toughness\":").append(g).append('}');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("검사: 방어력 30 초과·강도 20 초과·등급 누락·중복·감소는 거부")
    void rejectsBadData() {
        ArmorStatParser.parse(table(rows(30, 20)));
        assertThrows(IllegalArgumentException.class, () -> ArmorStatParser.parse(table(rows(31, 12))));
        assertThrows(IllegalArgumentException.class, () -> ArmorStatParser.parse(table(rows(30, 21))));
        assertThrows(IllegalArgumentException.class, () -> ArmorStatParser.parse(table(rows(18, 12))));
        String nine = rows(30, 12).substring(0, rows(30, 12).lastIndexOf(",{"));
        assertThrows(IllegalArgumentException.class, () -> ArmorStatParser.parse(table(nine)));
        assertThrows(IllegalArgumentException.class,
                () -> ArmorStatParser.parse(table(nine + ",{\"tier\":9,\"armor\":19,\"toughness\":9}")));
        assertThrows(IllegalArgumentException.class,
                () -> ArmorStatParser.parse(table(nine + ",{\"tier\":10,\"armor\":30,\"toughness\":12,\"knockback\":0.005}")));
    }
}
