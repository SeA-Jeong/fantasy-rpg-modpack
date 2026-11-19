package com.fantasyrpg.core.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 장착 프리셋 하나 (기획 v0.4 7.1, 2026-11-19: 장착칸 5개 + 활성 마스터리). 빈 문자열 = 비어 있음.
 */
public record SkillPreset(List<String> slots, String mastery) {
    public static final SkillPreset EMPTY = new SkillPreset(Collections.nCopies(RpgPlayerData.SLOT_COUNT, ""), "");

    public static final Codec<SkillPreset> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots", EMPTY.slots()).forGetter(SkillPreset::slots),
            Codec.STRING.optionalFieldOf("mastery", "").forGetter(SkillPreset::mastery)
    ).apply(i, SkillPreset::new));

    public SkillPreset {
        List<String> s = new ArrayList<>(slots);
        while (s.size() < RpgPlayerData.SLOT_COUNT) s.add("");
        slots = List.copyOf(s.subList(0, RpgPlayerData.SLOT_COUNT));
        if (mastery == null) mastery = "";
    }

    public boolean isEmpty() {
        return mastery.isEmpty() && slots.stream().allMatch(String::isEmpty);
    }
}
