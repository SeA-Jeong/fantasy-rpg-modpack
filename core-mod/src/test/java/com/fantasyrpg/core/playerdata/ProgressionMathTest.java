package com.fantasyrpg.core.playerdata;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 레벨·경험치·SP (claude/플레이어성장_설계.md, 통합문서 5절, 연동작업지침 8절).
 * 데이터는 모드 jar 기본값 core-mod/src/main/resources/data/fantasyrpg/progression/*.json.
 */
class ProgressionMathTest {
    private static final Path DATA = Path.of("src/main/resources/data/fantasyrpg/progression");

    private static ProgressionSettings load() throws IOException {
        ProgressionSettings s = ProgressionSettings.DEFAULT.withLeveling(JsonParser.parseString(
                Files.readString(DATA.resolve("leveling.json"), StandardCharsets.UTF_8)).getAsJsonObject());
        return s.withMobXp(JsonParser.parseString(
                Files.readString(DATA.resolve("mob_xp.json"), StandardCharsets.UTF_8)).getAsJsonObject());
    }

    @Test
    @DisplayName("곡선 [초안]: need(1)=100, need(10)=280, need(59)=1260, Lv.60 0, 합계 40,120, 데이터 = 문서 값")
    void curve() throws IOException {
        ProgressionSettings s = load();
        assertEquals(100, s.need(1));
        assertEquals(280, s.need(10));
        assertEquals(1260, s.need(59));
        assertEquals(0, s.need(60));
        assertEquals(40120, s.totalTo(60));
        assertEquals(1620, s.totalTo(10));
        assertEquals(400, s.chapterClearXpPerGrade());
        assertEquals(10, s.killXp("minecraft:zombie", true));
        assertEquals(0, s.killXp("minecraft:cow", false));
    }

    @Test
    @DisplayName("경험치: 여러 레벨을 한 번에 올리고 남은 경험치 유지")
    void levelUps() throws IOException {
        ProgressionSettings s = load();
        ProgressionMath.Gain g = ProgressionMath.addXp(new ProgressionMath.LevelState(1, 0), 250, 60, s);
        // 100(1→2) + 120(2→3) = 220, 남은 30
        assertEquals(new ProgressionMath.LevelState(3, 30), g.state());
        assertEquals(2, g.levelsGained());
        assertEquals(0, g.discarded());
    }

    @Test
    @DisplayName("레벨 상한: Lv.10에서 막히면 need(10)=280까지만 보관, 상한이 20으로 오르면 바로 Lv.11")
    void capAndHeld() throws IOException {
        ProgressionSettings s = load();
        ProgressionMath.Gain g = ProgressionMath.addXp(new ProgressionMath.LevelState(1, 0), 1620 + 780, 10, s);
        assertEquals(new ProgressionMath.LevelState(10, 280), g.state());
        assertEquals(9, g.levelsGained());
        assertEquals(500, g.discarded());
        ProgressionMath.Gain open = ProgressionMath.applyCap(g.state(), 20, s);
        assertEquals(new ProgressionMath.LevelState(11, 0), open.state());
        assertEquals(1, open.levelsGained());
        // 상한이 그대로면 아무 변화 없음
        assertEquals(g.state(), ProgressionMath.applyCap(g.state(), 10, s).state());
    }

    @Test
    @DisplayName("Lv.60에서는 경험치가 쌓이지 않음, 상한은 60을 넘지 않음")
    void maxLevel() throws IOException {
        ProgressionSettings s = load();
        ProgressionMath.Gain g = ProgressionMath.addXp(new ProgressionMath.LevelState(1, 0), 100_000, 60, s);
        assertEquals(new ProgressionMath.LevelState(60, 0), g.state());
        assertEquals(59, g.levelsGained());
        assertEquals(60, ProgressionMath.effectiveCap(999, s));
        assertEquals(1, ProgressionMath.effectiveCap(0, s));
    }

    @Test
    @DisplayName("SP: 완료 보너스 목표 0~13 → 0,0,1,1,…,6,6, 합계 1 + 59 + 6 = 66")
    void sp() {
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6};
        for (int c = 0; c <= 13; c++) assertEquals(expected[c], ProgressionMath.bonusTarget(c), "완료 " + c);
        assertEquals(0, ProgressionMath.bonusTarget(-1));
        assertEquals(66, ProgressionMath.spTotal(true, 60, 6));
        assertEquals(1, ProgressionMath.spTotal(true, 1, 0));
        assertEquals(59, ProgressionMath.spTotal(false, 60, 0));
    }

    @Test
    @DisplayName("챕터 최초 완료 경험치 = 등급 × 400, 직업 id 6종")
    void chapterXpAndClasses() throws IOException {
        ProgressionSettings s = load();
        assertEquals(400, ProgressionMath.chapterClearXp(1, s));
        assertEquals(2400, ProgressionMath.chapterClearXp(6, s));
        assertEquals(RpgClass.CLERIC, RpgClass.byId("cleric"));
        assertEquals(RpgClass.WARRIOR, RpgClass.byId("warrior"));
        assertNull(RpgClass.byId(""));
        assertEquals(6, RpgClass.values().length);
    }

    @Test
    @DisplayName("장 퀘스트 중간 단계 경험치 [초안]: 지금 등급 × 50 (1급 50, 6급 300), 중간 단계가 아니면 0, 데이터 = 문서 값")
    void midStepXp() throws IOException {
        ProgressionSettings s = load();
        assertEquals(50, s.midStepXpPerGrade());
        assertEquals(50, ProgressionMath.midStepXp(true, 1, s));
        assertEquals(300, ProgressionMath.midStepXp(true, 6, s));
        assertEquals(50, ProgressionMath.midStepXp(true, 0, s));
        assertEquals(0, ProgressionMath.midStepXp(false, 6, s));
        // 한 장 완료 경험치(등급 × 400)의 1/8
        assertEquals(ProgressionMath.chapterClearXp(3, s) / 8, ProgressionMath.midStepXp(true, 3, s));
    }
}
