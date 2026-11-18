package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.ArmorPart;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 룬석 한 종류의 효과. Minecraft 비의존.
 *
 * @param type 룬석 종류
 * @param attribute 능력치 id (예: minecraft:generic.max_health)
 * @param operation add_value / add_multiplied_base / add_multiplied_total
 * @param parts 끼울 수 있는 부위
 * @param values 하급·중급·상급·최고급 값 (4개)
 * @param lightBonus 경갑(class "light")에 끼웠을 때 더하는 값 (질풍 +0.01 등, 없으면 0)
 */
public record RuneDefinition(RuneType type, String attribute, String operation, Set<ArmorPart> parts,
                             double[] values, double lightBonus) {
    public static final int MIN_GRADE = 1;
    public static final int MAX_GRADE = 4;
    public static final List<String> OPERATIONS = List.of("add_value", "add_multiplied_base", "add_multiplied_total");

    public RuneDefinition {
        parts = Set.copyOf(parts);
        values = values.clone();
    }

    public static int clampGrade(int grade) {
        return Math.max(MIN_GRADE, Math.min(MAX_GRADE, grade));
    }

    public boolean fits(ArmorPart part) {
        return parts.contains(part);
    }

    /** 등급별 값. 경갑이면 lightBonus를 더한다. */
    public double value(int grade, boolean lightArmor) {
        return values[clampGrade(grade) - 1] + (lightArmor ? lightBonus : 0.0);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RuneDefinition d && type == d.type && attribute.equals(d.attribute)
                && operation.equals(d.operation) && parts.equals(d.parts)
                && Arrays.equals(values, d.values) && lightBonus == d.lightBonus;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + Arrays.hashCode(values);
    }
}
