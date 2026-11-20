package com.fantasyrpg.core.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 장착 프리셋 하나 (기획 v0.4 7.1, 2026-11-19: 장착칸 5개 + 활성 마스터리). 빈 문자열 = 비어 있음.
 * 2026-11-21 빌드 프리셋: ranks(노드 → 랭크, SP 배분)도 저장. 비어 있으면 예전 장착 프리셋.
 */
public record SkillPreset(List<String> slots, String mastery, java.util.Map<String, Integer> ranks) {
    public static final SkillPreset EMPTY = new SkillPreset(Collections.nCopies(RpgPlayerData.SLOT_COUNT, ""), "", java.util.Map.of());

    public SkillPreset(List<String> slots, String mastery) {
        this(slots, mastery, java.util.Map.of());
    }

    public static final Codec<SkillPreset> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots", EMPTY.slots()).forGetter(SkillPreset::slots),
            Codec.STRING.optionalFieldOf("mastery", "").forGetter(SkillPreset::mastery),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("ranks", java.util.Map.of()).forGetter(SkillPreset::ranks)
    ).apply(i, SkillPreset::new));

    public SkillPreset {
        List<String> s = new ArrayList<>(slots);
        while (s.size() < RpgPlayerData.SLOT_COUNT) s.add("");
        slots = List.copyOf(s.subList(0, RpgPlayerData.SLOT_COUNT));
        if (mastery == null) mastery = "";
        ranks = ranks == null ? java.util.Map.of() : java.util.Map.copyOf(ranks);
    }

    public boolean isEmpty() {
        return mastery.isEmpty() && ranks.isEmpty() && slots.stream().allMatch(String::isEmpty);
    }
}
