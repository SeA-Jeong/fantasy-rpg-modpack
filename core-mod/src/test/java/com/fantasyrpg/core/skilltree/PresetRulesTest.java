package com.fantasyrpg.core.skilltree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 장착 프리셋 (기획 v0.4 7.1, 2026-11-19 결정: 장착칸 + 활성 마스터리) */
class PresetRulesTest {

    private static Map<String, SkillNode> nodes() {
        Map<String, SkillNode> m = new HashMap<>();
        m.put("a1", node("a1", 1, SkillNode.Kind.ACTIVE));
        m.put("a2", node("a2", 3, SkillNode.Kind.ACTIVE));
        m.put("p1", node("p1", 2, SkillNode.Kind.PASSIVE));
        m.put("u1", node("u1", 5, SkillNode.Kind.ULTIMATE));
        m.put("m1", node("m1", 6, SkillNode.Kind.MASTERY));
        m.put("m2", node("m2", 6, SkillNode.Kind.MASTERY));
        return m;
    }

    private static SkillNode node(String id, int tier, SkillNode.Kind kind) {
        int max = kind == SkillNode.Kind.ULTIMATE ? 1 : kind == SkillNode.Kind.MASTERY ? 3 : 5;
        return new SkillNode(id, "warrior", id, EnumSet.of(SkillNode.Direction.ATTACK), tier, kind, max, 1, 0, "");
    }

    @Test
    @DisplayName("해금: Lv.1 한 개, Lv.20 두 개, Lv.40 세 개 (기획 7.1 기존안)")
    void unlocked() {
        List<Integer> lv = List.of(1, 20, 40);
        assertEquals(1, PresetRules.unlocked(1, lv));
        assertEquals(1, PresetRules.unlocked(19, lv));
        assertEquals(2, PresetRules.unlocked(20, lv));
        assertEquals(3, PresetRules.unlocked(60, lv));
    }

    @Test
    @DisplayName("적용: 배운 노드만, 궁극기는 B칸만, 패시브·중복은 비움")
    void apply() {
        Map<String, Integer> ranks = Map.of("a1", 2, "u1", 1, "p1", 3, "m1", 1);
        // a2 는 배우지 않음, p1 은 패시브, u1 이 액티브 칸, a1 이 두 번, 마지막 칸 궁극기 정상
        PresetRules.Applied a = PresetRules.apply(List.of("a1", "a2", "p1", "a1", "u1"), ranks, nodes());
        assertEquals(List.of("a1", "", "", "", "u1"), a.slots());
        assertEquals(3, a.dropped());
        PresetRules.Applied b = PresetRules.apply(List.of("u1", "", "", "", "a1"), ranks, nodes());
        assertEquals(List.of("", "", "", "", ""), b.slots());
        assertEquals(2, b.dropped());
    }

    @Test
    @DisplayName("마스터리: 배운 다른 마스터리일 때만 바꿀 대상")
    void mastery() {
        Map<String, Integer> ranks = Map.of("m1", 1, "m2", 2, "a1", 1);
        assertTrue(PresetRules.masteryChanges("m2", "m1", ranks, nodes()));
        assertFalse(PresetRules.masteryChanges("m1", "m1", ranks, nodes()));
        assertFalse(PresetRules.masteryChanges("", "m1", ranks, nodes()));
        assertFalse(PresetRules.masteryChanges("a1", "m1", ranks, nodes()));
        assertFalse(PresetRules.masteryChanges("m2", "m1", Map.of("m1", 1), nodes()));
    }
}
