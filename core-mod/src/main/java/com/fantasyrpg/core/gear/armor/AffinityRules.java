package com.fantasyrpg.core.gear.armor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 방어구 성향 (2026-11-21, claude/방어구성향_초안.md, 기획 v0.4 6.3). Minecraft 비의존.
 * 갑옷 한 부위마다 성향 하나(공격·주문·수호·기교·균형). 부위마다 부가 옵션, 등급 구간 배율,
 * 같은 성향 N부위 + 그 트리 방향 SP ≥ 기준이면 "성향 공명" 배율. 수치는 armor_affinity/settings.json [초안].
 */
public final class AffinityRules {
    private AffinityRules() {}

    public enum Affinity {
        ATTACK, SPELL, GUARD, FINESSE, BALANCE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** 없거나 모르는 값이면 균형 */
        public static Affinity byId(String id) {
            if (id == null) return BALANCE;
            for (Affinity a : values()) if (a.id().equals(id)) return a;
            return BALANCE;
        }

        /** 같은 이름의 스킬 트리 방향이 있는지 (균형은 없음) */
        public boolean hasDirection() {
            return this != BALANCE;
        }
    }

    /** 입은 부위 하나 */
    public record Piece(Affinity affinity, int tier) {}

    /**
     * @param options    성향 → (효과 키 → 부위당 값). 키: physical_bonus·magic_bonus·skill_magic_bonus·max_health·crit_chance·skill_haste
     * @param band       등급 구간 배율 [1~3급, 4~6급, 7~10급]
     * @param retune     재조율 각인석 수 [1~3급, 4~6급, 7~10급]
     */
    public record Settings(Map<Affinity, Map<String, Double>> options, double[] band, int resonancePieces, int resonanceSp,
                           double resonanceMult, int[] retune) {
        public static final Settings DEFAULT = new Settings(defaults(), new double[]{1, 1.5, 2}, 4, 10, 1.25, new int[]{1, 2, 3});

        private static Map<Affinity, Map<String, Double>> defaults() {
            Map<Affinity, Map<String, Double>> m = new EnumMap<>(Affinity.class);
            m.put(Affinity.ATTACK, Map.of("physical_bonus", 0.02));
            m.put(Affinity.SPELL, Map.of("skill_magic_bonus", 0.02));
            m.put(Affinity.GUARD, Map.of("max_health", 1.0));
            m.put(Affinity.FINESSE, Map.of("crit_chance", 0.01, "skill_haste", 1.0));
            m.put(Affinity.BALANCE, Map.of("physical_bonus", 0.01, "magic_bonus", 0.01));
            return m;
        }
    }

    public static final List<String> KEYS = List.of("physical_bonus", "magic_bonus", "skill_magic_bonus", "max_health", "crit_chance", "skill_haste");

    public static Settings parse(JsonObject o) {
        Map<Affinity, Map<String, Double>> opt = new EnumMap<>(Affinity.class);
        JsonObject oo = o.getAsJsonObject("options");
        for (Affinity a : Affinity.values()) {
            if (!oo.has(a.id())) throw new IllegalArgumentException("성향 " + a.id() + " 옵션 없음");
            Map<String, Double> one = new LinkedHashMap<>();
            for (var e : oo.getAsJsonObject(a.id()).entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                if (!KEYS.contains(e.getKey())) throw new IllegalArgumentException(a.id() + ": 모르는 효과 " + e.getKey());
                double v = e.getValue().getAsDouble();
                if (v < 0) throw new IllegalArgumentException(a.id() + "." + e.getKey() + " 는 0 이상");
                one.put(e.getKey(), v);
            }
            opt.put(a, Map.copyOf(one));
        }
        double[] band = nums(o.getAsJsonArray("band"));
        int[] retune = new int[3];
        JsonArray r = o.getAsJsonArray("retune_stones");
        if (band.length != 3 || r == null || r.size() != 3) throw new IllegalArgumentException("band·retune_stones 는 3개 (1~3급, 4~6급, 7~10급)");
        for (int i = 0; i < 3; i++) {
            retune[i] = r.get(i).getAsInt();
            if (retune[i] < 1) throw new IllegalArgumentException("retune_stones 는 1 이상");
        }
        JsonObject res = o.getAsJsonObject("resonance");
        int pieces = res.get("pieces").getAsInt(), sp = res.get("direction_sp").getAsInt();
        double mult = res.get("mult").getAsDouble();
        if (pieces < 1 || pieces > 4 || sp < 0 || mult < 1) throw new IllegalArgumentException("resonance: pieces 1~4, direction_sp ≥ 0, mult ≥ 1");
        return new Settings(opt, band, pieces, sp, mult, retune);
    }

    private static double[] nums(JsonArray a) {
        double[] out = new double[a.size()];
        int i = 0;
        for (JsonElement e : a) out[i++] = e.getAsDouble();
        return out;
    }

    /** 등급 구간: 1~3급 0, 4~6급 1, 7~10급 2 */
    public static int bandIndex(int tier) {
        return tier <= 3 ? 0 : tier <= 6 ? 1 : 2;
    }

    public static int retuneStones(Settings s, int tier) {
        return s.retune()[bandIndex(tier)];
    }

    /** 공명: 같은 성향 부위 수와 그 방향 SP */
    public static boolean resonates(Settings s, Affinity a, int pieces, int directionSp) {
        return a.hasDirection() && pieces >= s.resonancePieces() && directionSp >= s.resonanceSp();
    }

    /**
     * 입은 부위(파손 제외)의 성향 옵션 합계.
     * @param directionSp 트리 방향(attack·spell·guard·finesse) → 그 방향에 쓴 SP
     */
    public static Map<String, Double> total(Settings s, List<Piece> worn, Map<Affinity, Integer> directionSp) {
        Map<Affinity, Integer> count = new EnumMap<>(Affinity.class);
        for (Piece p : worn) count.merge(p.affinity(), 1, Integer::sum);
        Map<String, Double> out = new LinkedHashMap<>();
        for (Piece p : worn) {
            Map<String, Double> o = s.options().get(p.affinity());
            if (o == null) continue;
            double mult = s.band()[bandIndex(p.tier())];
            if (resonates(s, p.affinity(), count.getOrDefault(p.affinity(), 0), directionSp.getOrDefault(p.affinity(), 0)))
                mult *= s.resonanceMult();
            for (var e : o.entrySet()) out.merge(e.getKey(), e.getValue() * mult, Double::sum);
        }
        return out;
    }
}
