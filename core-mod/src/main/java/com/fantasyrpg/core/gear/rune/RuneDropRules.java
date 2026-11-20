package com.fantasyrpg.core.gear.rune;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 룬 원석·하급 룬석 몬스터 드롭 규칙 (순수 계산, 2026-11-21, claude/룬석_재료드롭_초안.md 5절).
 * 데이터 {@code data/fantasyrpg/rune_drops/settings.json}.
 */
public final class RuneDropRules {
    private RuneDropRules() {}

    public record Settings(double oreChance, double runeChance, List<String> runeTypes) {
        public static final Settings NONE = new Settings(0, 0, List.of());
    }

    public static Settings parse(JsonObject o) {
        double ore = chance(o, "ore_chance"), rune = chance(o, "rune_chance");
        List<String> types = new ArrayList<>();
        if (o.has("rune_types")) for (JsonElement e : o.getAsJsonArray("rune_types")) types.add(e.getAsString());
        if (rune > 0 && types.isEmpty()) throw new IllegalArgumentException("rune_chance 가 있으면 rune_types 가 필요");
        return new Settings(ore, rune, List.copyOf(types));
    }

    private static double chance(JsonObject o, String k) {
        double v = o.has(k) ? o.get(k).getAsDouble() : 0;
        if (!(v >= 0 && v <= 1)) throw new IllegalArgumentException(k + " 는 0~1: " + v);
        return v;
    }

    /** 원석이 나오는지 (r = 0 이상 1 미만 난수) */
    public static boolean ore(Settings s, double r) {
        return r < s.oreChance();
    }

    /** 하급 룬석 종류 (안 나오면 null). r1 = 나올지, r2 = 종류 고르기 */
    public static String rune(Settings s, double r1, double r2) {
        if (r1 >= s.runeChance() || s.runeTypes().isEmpty()) return null;
        int i = Math.min(s.runeTypes().size() - 1, (int) (r2 * s.runeTypes().size()));
        return s.runeTypes().get(i);
    }
}
