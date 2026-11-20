package com.fantasyrpg.core.skilltree;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 빌드 프리셋 다시 찍기와 재료 비용 (2026-11-21) */
class BuildCostRulesTest {
    private static SkillNode node(String id, int tier, int needSp, int level) {
        return new SkillNode(id, "warrior", id, EnumSet.of(SkillNode.Direction.ATTACK), tier, SkillNode.Kind.PASSIVE, 5, level, needSp, "");
    }

    private static Map<String, SkillNode> nodes() {
        Map<String, SkillNode> m = new HashMap<>();
        m.put("t1", node("t1", 1, 0, 1));
        m.put("t2", node("t2", 2, 3, 1));     // 공격 방향 3 SP 뒤
        m.put("t3", node("t3", 3, 6, 20));    // 6 SP 뒤, Lv.20
        return m;
    }

    @Test
    @DisplayName("다시 찍기: 저장 순서와 상관없이 낮은 단부터, 레벨·SP가 모자라면 그만큼 빠짐")
    void rebuild() {
        Map<String, Integer> target = Map.of("t3", 2, "t2", 3, "t1", 3);
        BuildRules.Rebuilt ok = BuildRules.rebuild(target, "warrior", 30, 20, nodes());
        assertEquals(target, ok.ranks());
        assertEquals(0, ok.missing());
        BuildRules.Rebuilt low = BuildRules.rebuild(target, "warrior", 10, 20, nodes());   // Lv.10 → t3 못 찍음
        assertEquals(Map.of("t1", 3, "t2", 3), low.ranks());
        assertEquals(2, low.missing());
        BuildRules.Rebuilt sp = BuildRules.rebuild(target, "warrior", 30, 5, nodes());     // SP 5 → t1 3 + t2 2
        assertEquals(5, sp.ranks().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(3, sp.missing());
        assertTrue(BuildRules.rebuild(Map.of("gone", 2), "warrior", 30, 20, nodes()).ranks().isEmpty());
    }

    @Test
    @DisplayName("배포 비용 [초안]: 노드 되돌리기 무료, 전체 초기화 에메랄드 8/등급, 빌드 4/등급, 직업 변경 에메랄드 16·다이아 1/등급")
    void shippedCosts() throws IOException {
        String text = Files.readString(Path.of("src/main/resources/data/fantasyrpg/skill_settings/costs.json"), StandardCharsets.UTF_8);
        CostRules.Table t = CostRules.parse(JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("costs"));
        assertTrue(CostRules.amounts(t.nodeRefund(), 3, 1).isEmpty());
        assertEquals(Map.of("minecraft:emerald", 24), CostRules.amounts(t.fullReset(), 3, 1));
        assertEquals(Map.of("minecraft:emerald", 4), CostRules.amounts(t.buildChange(), 1, 1));
        assertEquals(Map.of("minecraft:emerald", 96, "minecraft:diamond", 6), CostRules.amounts(t.classChange(), 6, 1));
        assertEquals(Map.of("minecraft:emerald", 8), CostRules.amounts(t.fullReset(), 0, 1));        // 등급 0은 1로
    }

    @Test
    @DisplayName("모자란 재료: 필요 − 가진 수, 넉넉하면 비어 있음")
    void missing() {
        Map<String, Integer> need = Map.of("a", 5, "b", 2);
        assertEquals(Map.of("a", 2), CostRules.missing(need, Map.of("a", 3, "b", 9)));
        assertTrue(CostRules.missing(need, Map.of("a", 5, "b", 2)).isEmpty());
    }
}
