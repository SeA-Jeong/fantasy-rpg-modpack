package com.fantasyrpg.core.skilltree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * 스킬트리 노드 한 개 (기획 v0.4 2장, claude/스킬트리_설계.md 2절). Minecraft 비의존.
 *
 * <pre>
 * {
 *   "id": "fantasyrpg:warrior/spell_1", "name": "마력 베기",
 *   "direction": "spell",              // 혼합 노드는 "directions": ["attack", "spell"]
 *   "tier": 1, "required_level": 1, "required_direction_sp": 0,
 *   "max_rank": 5, "kind": "active", "effect": "fantasyrpg:melee_strike"
 * }
 * </pre>
 *
 * @param classId 직업 id (폴더 이름 또는 "class")
 * @param directions 방향 1개(일반) 또는 2개(혼합)
 * @param tier 칸 번호 1~6 (혼합 노드는 0)
 * @param effect 스킬 구현 id (없으면 빈 문자열 = 효과 없음)
 * @param description 화면 설명 (없으면 빈 문자열)
 * @param values 랭크별 수치 ("values" 객체와 최상위 "cooldown_ticks"·"cost"), {@link SkillValues}
 */
public record SkillNode(String id, String classId, String name, Set<Direction> directions, int tier, Kind kind,
                        int maxRank, int requiredLevel, int requiredDirectionSp, String effect,
                        String description, SkillValues values) {

    public enum Direction {
        ATTACK, SPELL, GUARD, FINESSE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Direction parse(String s) {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Kind {
        ACTIVE, PASSIVE, ULTIMATE, MASTERY;

        public static Kind parse(String s) {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        }

        /** 장착칸에 넣는 종류 */
        public boolean equippable() {
            return this == ACTIVE || this == ULTIMATE;
        }
    }

    /** 혼합 노드가 열리는 양방향 각 투자량 */
    public static final int MIXED_REQUIREMENT = 8;

    public SkillNode {
        directions = Set.copyOf(directions);
        if (description == null) description = "";
        if (values == null) values = SkillValues.EMPTY;
    }

    /** 수치 없는 노드 (시험·테스트용) */
    public SkillNode(String id, String classId, String name, Set<Direction> directions, int tier, Kind kind,
                     int maxRank, int requiredLevel, int requiredDirectionSp, String effect) {
        this(id, classId, name, directions, tier, kind, maxRank, requiredLevel, requiredDirectionSp, effect, "", SkillValues.EMPTY);
    }

    public double value(String key, int rank) {
        return values.get(key, rank, 0.0);
    }

    public int cooldownTicks(int rank) {
        return (int) Math.round(values.get(SkillValues.COOLDOWN, rank, 0.0));
    }

    public double cost(int rank) {
        return values.get(SkillValues.COST, rank, 0.0);
    }

    public boolean mixed() {
        return directions.size() == 2;
    }

    /** 일반 노드의 방향 (혼합이면 null) */
    public Direction direction() {
        return mixed() ? null : directions.iterator().next();
    }

    public static SkillNode parse(String classId, JsonObject json) {
        String id = req(json, "id").getAsString().trim();
        String cls = json.has("class") ? json.get("class").getAsString().trim() : classId;
        if (cls == null || cls.isEmpty()) throw new IllegalArgumentException(id + ": 직업이 없습니다");
        Set<Direction> dirs = EnumSet.noneOf(Direction.class);
        if (json.has("directions")) {
            for (JsonElement e : json.getAsJsonArray("directions")) dirs.add(Direction.parse(e.getAsString()));
            if (dirs.size() != 2) throw new IllegalArgumentException(id + ": 혼합 노드는 서로 다른 방향 2개");
        } else {
            dirs.add(Direction.parse(req(json, "direction").getAsString()));
        }
        int tier = json.has("tier") ? json.get("tier").getAsInt() : 0;
        if (dirs.size() == 1 && (tier < 1 || tier > 6)) throw new IllegalArgumentException(id + ": tier는 1~6");
        Kind kind = Kind.parse(req(json, "kind").getAsString());
        int maxRank = req(json, "max_rank").getAsInt();
        if (maxRank < 1 || maxRank > 5) throw new IllegalArgumentException(id + ": max_rank는 1~5");
        int level = json.has("required_level") ? json.get("required_level").getAsInt() : 1;
        int dirSp = json.has("required_direction_sp") ? json.get("required_direction_sp").getAsInt() : 0;
        if (level < 1 || dirSp < 0) throw new IllegalArgumentException(id + ": 요구 레벨 1 이상, 선행 SP 0 이상");
        String name = json.has("name") ? json.get("name").getAsString() : id;
        String effect = json.has("effect") ? json.get("effect").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        SkillValues values;
        try {
            values = SkillValues.parse(json, maxRank);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(id + ": " + e.getMessage());
        }
        return new SkillNode(id, cls, name, dirs, tier, kind, maxRank, level, dirSp, effect, description, values);
    }

    private static JsonElement req(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("'" + key + "' 필드가 없습니다");
        return o.get(key);
    }
}
