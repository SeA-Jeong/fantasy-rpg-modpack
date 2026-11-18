package com.fantasyrpg.core.skilltree;

import com.fantasyrpg.core.classcore.CoreDef;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 기획 v0.4 부록 A-2 (스킬 해금·포인트 경계) + 2026-11-18 변경(노드 하나 되돌리기) + 공통 패시브·초기화권·코어.
 */
class SkillTreeRulesTest {
    private static final int[] LEVEL = {0, 1, 1, 20, 30, 40, 60};
    private static final int[] PRE = {0, 0, 0, 5, 10, 15, 20};
    private static final int[] MAX = {0, 5, 5, 5, 5, 1, 3};
    private static final SkillNode.Kind[] KIND = {null, SkillNode.Kind.ACTIVE, SkillNode.Kind.PASSIVE,
            SkillNode.Kind.ACTIVE, SkillNode.Kind.PASSIVE, SkillNode.Kind.ULTIMATE, SkillNode.Kind.MASTERY};

    /** 전사 4방향 × 6칸 (기획 2.1 표) + 혼합 노드 1개 */
    private static Map<String, SkillNode> tree() {
        Map<String, SkillNode> m = new HashMap<>();
        for (SkillNode.Direction d : SkillNode.Direction.values()) {
            for (int t = 1; t <= 6; t++) {
                String id = d.id() + "_" + t;
                m.put(id, new SkillNode(id, "warrior", id, Set.of(d), t, KIND[t], MAX[t], LEVEL[t], PRE[t], ""));
            }
        }
        m.put("mixed", new SkillNode("mixed", "warrior", "mixed", EnumSet.of(SkillNode.Direction.ATTACK, SkillNode.Direction.SPELL),
                0, SkillNode.Kind.PASSIVE, 1, 1, 0, ""));
        return m;
    }

    private static Map<String, Integer> fill(String dir, int upToTier, int lastRank) {
        Map<String, Integer> r = new HashMap<>();
        for (int t = 1; t < upToTier; t++) r.put(dir + "_" + t, MAX[t]);
        if (lastRank > 0) r.put(dir + "_" + upToTier, lastRank);
        return r;
    }

    @Test
    @DisplayName("A-2: Lv.20 공격력 4SP → 3번 노드 실패(선행 5), 5SP → 성공·이후 방향 6SP")
    void tier3() {
        Map<String, SkillNode> n = tree();
        Map<String, Integer> four = Map.of("attack_1", 4);
        assertEquals(SkillTreeRules.Result.PREREQUISITE, SkillTreeRules.canInvest("attack_3", "warrior", 20, 66, four, n));
        Map<String, Integer> five = Map.of("attack_1", 5);
        assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canInvest("attack_3", "warrior", 20, 66, five, n));
        Map<String, Integer> after = SkillTreeRules.withRank(five, "attack_3", 1);
        assertEquals(6, SkillTreeRules.spentInDirection(SkillNode.Direction.ATTACK, after, n, ""));
        // 레벨 미달
        assertEquals(SkillTreeRules.Result.LEVEL, SkillTreeRules.canInvest("attack_3", "warrior", 19, 66, five, n));
    }

    @Test
    @DisplayName("A-2: Lv.40 주문력 14SP 궁극기 실패, Lv.60 방향 20SP 마스터리 1랭크 성공 = 총 21SP")
    void ultimateAndMastery() {
        Map<String, SkillNode> n = tree();
        Map<String, Integer> fourteen = fill("spell", 3, 4); // 5+5+4
        assertEquals(14, SkillTreeRules.spent(fourteen));
        assertEquals(SkillTreeRules.Result.PREREQUISITE, SkillTreeRules.canInvest("spell_5", "warrior", 40, 66, fourteen, n));
        Map<String, Integer> twenty = fill("attack", 5, 0); // 1~4칸 각 5
        assertEquals(20, SkillTreeRules.spent(twenty));
        assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canInvest("attack_6", "warrior", 60, 66, twenty, n));
        assertEquals(21, SkillTreeRules.spent(SkillTreeRules.withRank(twenty, "attack_6", 1)));
    }

    @Test
    @DisplayName("A-2: Lv.60 세 방향 각 20SP + 마스터리 각 1랭크 = 63SP 가능, SP가 모자라면 실패")
    void threeDirections() {
        Map<String, SkillNode> n = tree();
        Map<String, Integer> r = new HashMap<>();
        for (String d : new String[]{"attack", "spell", "guard"}) r.putAll(fill(d, 5, 0));
        for (String d : new String[]{"attack", "spell", "guard"}) {
            assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canInvest(d + "_6", "warrior", 60, 66, r, n));
            r = SkillTreeRules.withRank(r, d + "_6", 1);
        }
        assertEquals(63, SkillTreeRules.spent(r));
        assertEquals(SkillTreeRules.Result.NO_SP, SkillTreeRules.canInvest("finesse_1", "warrior", 60, 63, r, n));
    }

    @Test
    @DisplayName("A-2 변경: 상위 노드 선행을 깨는 되돌리기 실패, 깨지 않는 되돌리기 성공")
    void refund() {
        Map<String, SkillNode> n = tree();
        Map<String, Integer> r = new HashMap<>(Map.of("attack_1", 5, "attack_3", 1));
        assertEquals(SkillTreeRules.Result.BREAKS_OTHERS, SkillTreeRules.canRefund("attack_1", r, n));
        assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canRefund("attack_3", r, n));
        r.put("attack_2", 1); // 공격력 7SP(자신 제외 선행 6) → attack_1을 1 내려도 3번 선행 5 유지
        assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canRefund("attack_1", r, n));
        assertEquals(SkillTreeRules.Result.NOT_INVESTED, SkillTreeRules.canRefund("spell_1", r, n));
    }

    @Test
    @DisplayName("A-2: 혼합 노드는 두 방향 각 8SP 이상 + 별도 1SP, 7SP면 실패")
    void mixed() {
        Map<String, SkillNode> n = tree();
        Map<String, Integer> r = new HashMap<>(Map.of("attack_1", 5, "attack_2", 3, "spell_1", 5, "spell_2", 3));
        assertEquals(SkillTreeRules.Result.OK, SkillTreeRules.canInvest("mixed", "warrior", 1, 66, r, n));
        r.put("spell_2", 2);
        assertEquals(SkillTreeRules.Result.MIXED_PREREQUISITE, SkillTreeRules.canInvest("mixed", "warrior", 1, 66, r, n));
        assertEquals(SkillTreeRules.Result.WRONG_CLASS, SkillTreeRules.canInvest("attack_1", "mage", 60, 66, Map.of(), n));
    }

    @Test
    @DisplayName("공통 패시브: 포인트 2×등급(1~6급, 최대 12), 등급 조건·최대 랭크·포인트 부족")
    void passive() {
        assertEquals(2, PassiveNode.points(1));
        assertEquals(12, PassiveNode.points(6));
        assertEquals(12, PassiveNode.points(9));
        PassiveNode hp = PassiveNode.parse(JsonParser.parseString("{\"id\":\"hp\",\"max_rank\":3,\"required_grade\":1,"
                + "\"attribute\":\"minecraft:generic.max_health\",\"operation\":\"add_value\",\"per_rank\":2}").getAsJsonObject());
        PassiveNode late = new PassiveNode("late", "late", 1, 3, "minecraft:generic.max_health", "add_value", 1);
        Map<String, PassiveNode> n = Map.of("hp", hp, "late", late);
        assertEquals(PassiveNode.Result.OK, PassiveNode.canInvest("hp", 1, Map.of("hp", 1), n));
        assertEquals(PassiveNode.Result.NO_POINTS, PassiveNode.canInvest("hp", 1, Map.of("hp", 2), n));
        assertEquals(PassiveNode.Result.MAX_RANK, PassiveNode.canInvest("hp", 6, Map.of("hp", 3), n));
        assertEquals(PassiveNode.Result.GRADE, PassiveNode.canInvest("late", 2, Map.of(), n));
    }

    @Test
    @DisplayName("무료 초기화권: 직업 선택 1 + 완료 6개 1, 쓴 만큼 차감, 엔딩 후 무제한")
    void freeResets() {
        assertEquals(0, ResetRules.freeResetsAvailable(false, 0, 0, false));
        assertEquals(1, ResetRules.freeResetsAvailable(true, 5, 0, false));
        assertEquals(2, ResetRules.freeResetsAvailable(true, 6, 0, false));
        assertEquals(1, ResetRules.freeResetsAvailable(true, 6, 1, false));
        assertEquals(0, ResetRules.freeResetsAvailable(true, 0, 3, false));
        assertEquals(Integer.MAX_VALUE, ResetRules.freeResetsAvailable(true, 0, 5, true));
    }

    @Test
    @DisplayName("코어: 기본 공격 적중으로 얻고, 지연 뒤 감소, 자동 회복, 0~최대로 자름, 잘못된 데이터 거부")
    void core() {
        CoreDef momentum = CoreDef.parse(JsonParser.parseString("{\"id\":\"fantasyrpg:momentum\",\"class\":\"warrior\",\"max\":100,"
                + "\"start\":0,\"gain\":{\"basic_attack_hit\":8},\"decay\":{\"delay_ticks\":100,\"per_second\":10}}").getAsJsonObject());
        double v = momentum.add(0, momentum.gainFor("basic_attack_hit"));
        assertEquals(8.0, v);
        assertEquals(8.0, momentum.tick(v, 99));
        assertEquals(7.5, momentum.tick(v, 100), 1e-12);
        assertEquals(100.0, momentum.add(98, 8));
        CoreDef mana = new CoreDef("mana", "mage", 100, 100, Map.of(), 5, 0, 0);
        assertEquals(50.25, mana.tick(50, 0), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> CoreDef.parse(JsonParser.parseString(
                "{\"id\":\"x\",\"class\":\"mage\",\"max\":10,\"start\":20}").getAsJsonObject()));
    }

    @Test
    @DisplayName("노드 파싱: 기획 견본 형식(direction·tier·kind), 혼합 노드는 directions 2개")
    void parse() {
        SkillNode s = SkillNode.parse("warrior", JsonParser.parseString("{\"id\":\"fantasyrpg:warrior/spell_1\",\"name\":\"마력 베기\","
                + "\"direction\":\"spell\",\"tier\":1,\"required_level\":1,\"required_direction_sp\":0,\"max_rank\":5,"
                + "\"kind\":\"active\",\"effect\":\"fantasyrpg:melee_strike\"}").getAsJsonObject());
        assertEquals(SkillNode.Direction.SPELL, s.direction());
        assertEquals("warrior", s.classId());
        assertEquals(SkillNode.Kind.ACTIVE, s.kind());
        SkillNode m = SkillNode.parse("warrior", JsonParser.parseString("{\"id\":\"m\",\"directions\":[\"attack\",\"guard\"],"
                + "\"kind\":\"passive\",\"max_rank\":1}").getAsJsonObject());
        assertEquals(true, m.mixed());
        assertThrows(IllegalArgumentException.class, () -> SkillNode.parse("warrior", JsonParser.parseString(
                "{\"id\":\"x\",\"direction\":\"attack\",\"tier\":7,\"kind\":\"active\",\"max_rank\":5}").getAsJsonObject()));
    }
}
