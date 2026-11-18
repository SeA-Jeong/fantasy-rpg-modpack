package com.fantasyrpg.core.gear.armor;

import java.util.List;

/**
 * 한 계열의 1~10급 세트 합계 표와 부위별 값. Minecraft 비의존.
 * 기준: claude/장비등급제한_설계.md v3 1-2·1-4 (2026-11-18 확정).
 *
 * @param family 계열 id (knight, dwarf, dragon, elf, arcane)
 * @param armorClass heavy / light 등 (표시·검사용)
 * @param tiers 1급부터 순서대로 10개
 */
public record ArmorStatTable(String family, String armorClass, List<Tier> tiers) {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 10;
    /** 바닐라 방어력 속성 최대값 */
    public static final int MAX_ARMOR = 30;
    /** 바닐라 방어 강도 속성 최대값 */
    public static final int MAX_TOUGHNESS = 20;

    /** 4부위 세트 합계 */
    public record Tier(int tier, int armor, int toughness, double knockback) {
    }

    public ArmorStatTable {
        tiers = List.copyOf(tiers);
    }

    /** 범위를 벗어난 등급은 1~10으로 맞춘다. */
    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    public Tier tier(int tier) {
        return tiers.get(clampTier(tier) - 1);
    }

    public int partArmor(int tier, ArmorPart part) {
        return PartSplit.split(tier(tier).armor())[part.ordinal()];
    }

    public int partToughness(int tier, ArmorPart part) {
        return PartSplit.split(tier(tier).toughness())[part.ordinal()];
    }

    public double partKnockback(int tier, ArmorPart part) {
        return PartSplit.splitHundredths(tier(tier).knockback())[part.ordinal()];
    }
}
