package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreDef;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.fantasyrpg.core.skilltree.SkillValues;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 6직업 1~2단 스킬 (claude/전사마법사_스킬_초안.md, claude/4직업_스킬_초안.md): 모드 jar 기본 데이터 검사와 순수 계산.
 */
class ClassSkillsTest {
    private static final Path DATA = Path.of("src/main/resources/data/fantasyrpg");

    private static Map<String, SkillNode> load(String cls) throws IOException {
        Map<String, SkillNode> out = new HashMap<>();
        try (var files = Files.list(DATA.resolve("skill_nodes").resolve(cls))) {
            for (Path f : files.toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                SkillNode n = SkillNode.parse(cls, o);
                out.put(n.id(), n);
            }
        }
        return out;
    }

    /** 효과마다 코드가 읽는 수치 키 */
    private static final Map<String, List<String>> REQUIRED = Map.ofEntries(
            Map.entry(SkillIds.W_HEAVY_STRIKE, List.of("physical", "range", "stun_ticks", "empower_threshold", "empower_cost", "empower_mult")),
            Map.entry(SkillIds.W_SWORD_MASTERY, List.of("basic_physical_bonus", "hit_core_bonus", "third_hit_physical")),
            Map.entry(SkillIds.W_MANA_SLASH, List.of("physical", "magic", "range", "angle", "mark_ticks", "burst_magic")),
            Map.entry(SkillIds.W_SPELLBLADE, List.of("basic_magic", "high_core", "high_core_mult", "cooldown_cut")),
            Map.entry(SkillIds.W_INDOMITABLE, List.of("duration_ticks", "reduction", "cast_core", "hit_core", "end_physical", "end_radius")),
            Map.entry(SkillIds.W_FIGHTING_SPIRIT, List.of("hurt_core", "low_health", "low_core", "low_cooldown_ticks", "low_absorption", "absorption_ticks")),
            Map.entry(SkillIds.W_FLURRY, List.of("physical", "hits", "interval_ticks", "range", "refund_core")),
            Map.entry(SkillIds.W_COMBO_SENSE, List.of("crit_chance", "after_skill_ticks", "after_skill_bonus", "crit_core")),
            Map.entry(SkillIds.M_SOLID_BOLTS, List.of("physical", "bolts", "interval_ticks", "range", "knockback", "pierce")),
            Map.entry(SkillIds.M_BOLT_TRAINING, List.of("bolt_physical", "bolt_cooldown_ticks", "extra_every")),
            Map.entry(SkillIds.M_ARCANE_BURST, List.of("magic", "range", "radius", "echo_delay", "echo_ratio")),
            Map.entry(SkillIds.M_ARCANE_INSIGHT, List.of("skill_magic_bonus", "max_core_bonus", "spell_hit_core")),
            Map.entry(SkillIds.M_BARRIER_SHOCK, List.of("magic", "radius", "knockback", "absorption", "absorption_ticks", "slow_ticks", "reflect_magic", "reflect_interval")),
            Map.entry(SkillIds.M_MANA_WARD, List.of("ratio", "mana_per_hp", "low_health", "low_mult", "auto_barrier")),
            Map.entry(SkillIds.M_BLINK, List.of("distance", "afterimage_magic", "afterimage_radius")),
            Map.entry(SkillIds.M_SPELL_CHAIN, List.of("window_ticks", "cooldown_cut", "chain_core", "chain_count", "chain_bonus")),
            Map.entry(SkillIds.K_CHARGE, List.of("physical", "distance", "knockback", "stun_ticks", "empower_threshold", "empower_cost", "empower_mult", "empower_distance_mult")),
            Map.entry(SkillIds.K_SPEAR_MASTERY, List.of("basic_physical_bonus", "pierce_ratio", "after_charge_ticks", "after_charge_bonus")),
            Map.entry(SkillIds.K_MANA_SPEAR, List.of("physical", "magic", "range", "slow_ticks", "end_magic", "end_radius")),
            Map.entry(SkillIds.K_RUNE_ENGRAVE, List.of("basic_magic", "multi_hit_core", "cooldown_cut")),
            Map.entry(SkillIds.K_PERFECT_GUARD, List.of("duration_ticks", "reduction", "front_angle", "parry_ticks", "parry_core", "parry_stun_ticks", "parry_cooldown_ratio", "counter_physical")),
            Map.entry(SkillIds.K_STEADFAST, List.of("hurt_core", "knockback_core", "high_core_guard", "high_core_reduction")),
            Map.entry(SkillIds.K_RIPOSTE, List.of("physical", "range", "parry_window_ticks", "parry_mult", "hits")),
            Map.entry(SkillIds.K_BATTLE_RHYTHM, List.of("crit_chance", "after_skill_ticks", "after_skill_bonus")),
            Map.entry(SkillIds.R_VITAL_FLURRY, List.of("physical", "range", "hits", "per_mark_bonus", "behind_crit", "full_marks", "full_hits")),
            Map.entry(SkillIds.R_VITALS, List.of("basic_physical_bonus", "behind_bonus", "behind_core")),
            Map.entry(SkillIds.R_SHADOW_BLADES, List.of("knives", "spread", "range", "physical", "magic", "hit_core", "shadow_ticks", "shadow_bonus")),
            Map.entry(SkillIds.R_SHADOW_EDGE, List.of("basic_magic", "high_core", "high_core_mult", "cooldown_cut")),
            Map.entry(SkillIds.R_EVADE, List.of("evade_ticks", "leap", "evade_core", "counter_physical", "evade_cooldown_ratio")),
            Map.entry(SkillIds.R_NIMBLE, List.of("hurt_speed", "hurt_speed_ticks", "low_health", "low_cooldown_ticks", "low_evade_ticks", "low_absorption", "absorption_ticks")),
            Map.entry(SkillIds.R_CHAIN_MARK, List.of("physical", "range", "mark_gain", "bonus_threshold", "bonus_marks", "kill_reset_ticks")),
            Map.entry(SkillIds.R_VITAL_SENSE, List.of("crit_chance", "crit_core", "crit_mult_core", "crit_mult_bonus")),
            Map.entry(SkillIds.A_PIERCING_SHOT, List.of("physical", "range", "stun_ticks", "empower_threshold", "empower_cost", "empower_mult")),
            Map.entry(SkillIds.A_PRECISION, List.of("arrow_bonus", "far_distance", "far_bonus", "third_hit_physical")),
            Map.entry(SkillIds.A_ELEMENTAL_ARROW, List.of("magic", "range", "radius", "slow_ticks", "field_magic", "field_pulses", "field_interval")),
            Map.entry(SkillIds.A_ELEMENTAL_FLETCH, List.of("arrow_magic", "high_core", "high_core_mult", "cooldown_cut")),
            Map.entry(SkillIds.A_RETREAT_SHOT, List.of("physical", "range", "angle", "knockback", "cast_core", "leap", "slow_ticks", "guard_ticks", "guard_reduction")),
            Map.entry(SkillIds.A_KEEP_DISTANCE, List.of("hurt_core", "low_health", "low_cooldown_ticks", "low_speed", "low_speed_ticks", "low_absorption", "absorption_ticks")),
            Map.entry(SkillIds.A_RAPID_FIRE, List.of("physical", "shots", "interval_ticks", "range", "refund_core")),
            Map.entry(SkillIds.A_MARKSMAN, List.of("crit_chance", "after_skill_ticks", "after_skill_bonus", "crit_core")),
            Map.entry(SkillIds.C_SMITE, List.of("physical", "magic", "range", "splash_radius", "splash_ratio", "stun_ticks", "empower_threshold", "empower_cost", "empower_mult", "empower_radius")),
            Map.entry(SkillIds.C_MACE_MASTERY, List.of("basic_physical_bonus", "hit_core_bonus", "third_hit_stun")),
            Map.entry(SkillIds.C_HOLY_LIGHT, List.of("magic", "range", "radius", "undead_bonus", "heal_per_hit", "heal_cap", "echo_ratio", "echo_delay")),
            Map.entry(SkillIds.C_HOLY_ENGRAVE, List.of("basic_magic", "undead_bonus", "cooldown_cut")),
            Map.entry(SkillIds.C_SANCTUARY, List.of("duration_ticks", "reduction", "core_per_damage", "end_magic", "end_radius", "end_heal_ratio")),
            Map.entry(SkillIds.C_FAITH_ARMOR, List.of("hurt_core", "low_health", "low_cooldown_ticks", "low_heal", "low_absorption", "absorption_ticks")),
            Map.entry(SkillIds.C_PURIFY, List.of("physical", "magic", "range", "purge_all", "purge_core", "per_purge_magic")),
            Map.entry(SkillIds.C_HOLY_RHYTHM, List.of("crit_chance", "after_skill_ticks", "after_skill_bonus", "crit_core")));

    @Test
    @DisplayName("6직업 각 8노드: 4방향 × 1단 액티브·2단 패시브, 5랭크, 효과 id·수치 키 모두 있음, 효과 48개 모두 다름")
    void nodeData() throws IOException {
        Set<String> effects = new HashSet<>();
        for (String cls : List.of("warrior", "mage", "knight", "rogue", "archer", "cleric")) {
            Map<String, SkillNode> nodes = load(cls);
            assertEquals(8, nodes.size(), cls);
            for (SkillNode.Direction d : SkillNode.Direction.values()) {
                for (int tier = 1; tier <= 2; tier++) {
                    final int t = tier;
                    SkillNode n = nodes.values().stream().filter(x -> x.directions().equals(Set.of(d)) && x.tier() == t).findFirst().orElseThrow();
                    assertEquals(t == 1 ? SkillNode.Kind.ACTIVE : SkillNode.Kind.PASSIVE, n.kind(), n.id());
                    assertEquals(5, n.maxRank(), n.id());
                    assertEquals(1, n.requiredLevel(), n.id());
                    assertEquals(0, n.requiredDirectionSp(), n.id());
                    assertTrue(REQUIRED.containsKey(n.effect()), n.id() + " 효과 " + n.effect());
                    for (String k : REQUIRED.get(n.effect())) assertTrue(n.values().has(k), n.id() + " 수치 " + k);
                    assertFalse(n.description().isEmpty(), n.id());
                    if (t == 1) assertTrue(n.cooldownTicks(1) > 0, n.id() + " 쿨타임");
                    effects.add(n.effect());
                }
            }
        }
        assertEquals(48, effects.size());
        assertEquals(REQUIRED.keySet(), effects);
    }

    @Test
    @DisplayName("초안 표의 대표 수치 (강격 2.4, 마력 베기 마법 1.75, 불굴 35%, 비전 폭발 반경 4, 점멸 8초·10칸)")
    void draftNumbers() throws IOException {
        Map<String, SkillNode> w = load("warrior");
        Map<String, SkillNode> m = load("mage");
        SkillNode strike = w.get("fantasyrpg:warrior/attack_1");
        assertEquals(1.6, strike.value("physical", 1), 1e-9);
        assertEquals(2.4, strike.value("physical", 5), 1e-9);
        assertEquals(120, strike.cooldownTicks(3));
        assertEquals(20, strike.cost(5), 1e-9);
        assertEquals(1.75, w.get("fantasyrpg:warrior/spell_1").value("magic", 5), 1e-9);
        assertEquals(0.4, w.get("fantasyrpg:warrior/spell_1").value("physical", 2), 1e-9);
        assertEquals(0.35, w.get("fantasyrpg:warrior/guard_1").value("reduction", 5), 1e-9);
        assertEquals(0, w.get("fantasyrpg:warrior/guard_1").cost(1), 1e-9); // 기획 3.2: 코어 없이 쓰는 행동
        assertEquals(4, w.get("fantasyrpg:warrior/finesse_1").value("hits", 5), 1e-9);
        assertEquals(4, m.get("fantasyrpg:mage/spell_1").value("radius", 3), 1e-9);
        assertEquals(3, m.get("fantasyrpg:mage/spell_1").value("radius", 2), 1e-9);
        assertEquals(160, m.get("fantasyrpg:mage/finesse_1").cooldownTicks(3));
        assertEquals(200, m.get("fantasyrpg:mage/finesse_1").cooldownTicks(2));
        assertEquals(10, m.get("fantasyrpg:mage/finesse_1").value("distance", 5), 1e-9);
        // 연계 감각 최대 +5%p 는 기교 트리 상한 +15%p 안
        assertTrue(w.get("fantasyrpg:warrior/finesse_2").value("crit_chance", 5) <= 0.15);
    }

    @Test
    @DisplayName("코어: 기세 최대 100·적중 +8·5초 뒤 초당 10 감소, 마나 최대 100·시작 100·초당 5, 비전 이해로 최대 120")
    void cores() throws IOException {
        CoreDef w = CoreDef.parse(JsonParser.parseString(Files.readString(DATA.resolve("class_cores/warrior.json"))).getAsJsonObject());
        CoreDef m = CoreDef.parse(JsonParser.parseString(Files.readString(DATA.resolve("class_cores/mage.json"))).getAsJsonObject());
        assertEquals(8, w.gainFor("basic_attack_hit"), 1e-9);
        assertEquals(50, w.tick(50, 99), 1e-9);
        assertEquals(49.5, w.tick(50, 100), 1e-9);
        assertEquals(100, m.start(), 1e-9);
        assertEquals(50.25, m.tick(50, 0), 1e-9);
        CoreDef bigger = m.withMax(120);
        assertEquals(120, bigger.clamp(500), 1e-9);
        assertEquals(100, m.clamp(500), 1e-9);
    }

    @Test
    @DisplayName("4직업 초안 수치·코어 (기사 패리 6틱·완벽 방어 90%, 도적 표식 최대 5·표식당 +20%, 궁수 관통 2.3, 전투사제 신성력 시작 50·줄지 않음)")
    void fourClasses() throws IOException {
        Map<String, SkillNode> k = load("knight");
        Map<String, SkillNode> r = load("rogue");
        Map<String, SkillNode> a = load("archer");
        Map<String, SkillNode> c = load("cleric");
        assertEquals(6, k.get("fantasyrpg:knight/guard_1").value("parry_ticks", 1), 1e-9);
        assertEquals(0.9, k.get("fantasyrpg:knight/guard_1").value("reduction", 5), 1e-9);
        assertEquals(0, k.get("fantasyrpg:knight/guard_1").cost(1), 1e-9);
        assertEquals(2.2, k.get("fantasyrpg:knight/attack_1").value("physical", 5), 1e-9);
        assertEquals(0.2, r.get("fantasyrpg:rogue/attack_1").value("per_mark_bonus", 1), 1e-9);
        assertEquals(3, r.get("fantasyrpg:rogue/spell_1").value("knives", 3), 1e-9);
        assertEquals(2.3, a.get("fantasyrpg:archer/attack_1").value("physical", 5), 1e-9);
        assertEquals(4, a.get("fantasyrpg:archer/finesse_1").value("shots", 5), 1e-9);
        assertEquals(0.3, c.get("fantasyrpg:cleric/spell_1").value("undead_bonus", 1), 1e-9);
        assertEquals(0.4, c.get("fantasyrpg:cleric/guard_1").value("reduction", 5), 1e-9);
        for (Map<String, SkillNode> m : List.of(k, r, a, c)) {
            for (SkillNode n : m.values()) {
                if (n.values().has("crit_chance")) assertTrue(n.value("crit_chance", 5) <= 0.15, n.id());
            }
        }
        CoreDef rogue = core("rogue");
        assertEquals(5, rogue.max(), 1e-9);
        assertEquals(1, rogue.gainFor("basic_attack_hit"), 1e-9);
        CoreDef cleric = core("cleric");
        assertEquals(50, cleric.start(), 1e-9);
        assertEquals(50, cleric.tick(50, 100000), 1e-9);
        assertEquals(10, core("archer").gainFor("basic_attack_hit"), 1e-9);
        assertEquals(6, core("knight").gainFor("basic_attack_hit"), 1e-9);
    }

    private static CoreDef core(String cls) throws IOException {
        return CoreDef.parse(JsonParser.parseString(Files.readString(DATA.resolve("class_cores/" + cls + ".json"))).getAsJsonObject());
    }

    @Test
    @DisplayName("등 뒤 판정: 대상이 +z를 볼 때 공격자가 −z 쪽이면 뒤, +z 쪽이면 앞")
    void behind() {
        assertTrue(SkillMath.isBehind(0, 1, 0, -2));
        assertFalse(SkillMath.isBehind(0, 1, 0, 2));
        assertTrue(SkillMath.isBehind(0, 1, 1, -0.1));
    }

    @Test
    @DisplayName("부채꼴 판정: 90°면 정면·45° 안은 맞고 뒤·옆 90°는 안 맞음")
    void cone() {
        assertTrue(SkillMath.inCone(0, 3, 0, 1, 90));
        assertTrue(SkillMath.inCone(2, 2, 0, 1, 90)); // 정확히 45°
        assertFalse(SkillMath.inCone(3, 0.1, 0, 1, 90));
        assertFalse(SkillMath.inCone(0, -3, 0, 1, 90));
        assertTrue(SkillMath.inCone(0, 0, 0, 1, 90));
    }

    @Test
    @DisplayName("마력 방벽: 피해 10·비율 12%·마나 충분 → 1.2를 마나 6으로, 마나 3뿐이면 0.6만")
    void manaWard() {
        SkillMath.Ward w = SkillMath.manaWard(10, 0.12, 100, 5);
        assertEquals(1.2, w.absorbed(), 1e-9);
        assertEquals(6.0, w.manaSpent(), 1e-9);
        SkillMath.Ward poor = SkillMath.manaWard(10, 0.12, 3, 5);
        assertEquals(0.6, poor.absorbed(), 1e-9);
        assertEquals(3.0, poor.manaSpent(), 1e-9);
        assertEquals(0, SkillMath.manaWard(10, 0.24, 0, 5).absorbed(), 1e-9);
    }

    @Test
    @DisplayName("주문 연쇄 쿨타임(120틱 −25% = 90), 불굴 감소(10 × 0.65), 연속 적중 수")
    void chainAndReduction() {
        assertEquals(90, SkillMath.chainedCooldown(120, 0.25));
        assertEquals(114, SkillMath.chainedCooldown(120, 0.05));
        assertEquals(6.5, SkillMath.reduced(10, 0.35), 1e-9);
        assertEquals(1.0, SkillMath.reduced(10, 5.0), 1e-9); // 최대 90%
        assertEquals(3, SkillMath.consecutive(true, 2));
        assertEquals(1, SkillMath.consecutive(false, 2));
    }

    @Test
    @DisplayName("랭크 수치 형식: 배열 길이가 max_rank와 다르거나 음수면 거부, 숫자 하나는 모든 랭크 같음")
    void valuesFormat() {
        JsonObject ok = JsonParser.parseString("{\"cooldown_ticks\":100,\"cost\":{\"amount\":20},\"values\":{\"a\":[1,2,3],\"b\":7}}").getAsJsonObject();
        SkillValues v = SkillValues.parse(ok, 3);
        assertEquals(100, v.get(SkillValues.COOLDOWN, 2, 0), 1e-9);
        assertEquals(20, v.get(SkillValues.COST, 1, 0), 1e-9);
        assertEquals(3, v.get("a", 3, 0), 1e-9);
        assertEquals(1, v.get("a", 0, 0), 1e-9);
        assertEquals(7, v.get("b", 3, 0), 1e-9);
        assertEquals(-1, v.get("없음", 1, -1), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> SkillValues.parse(
                JsonParser.parseString("{\"values\":{\"a\":[1,2]}}").getAsJsonObject(), 3));
        assertThrows(IllegalArgumentException.class, () -> SkillValues.parse(
                JsonParser.parseString("{\"values\":{\"a\":-1}}").getAsJsonObject(), 3));
    }
}
