package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.gear.armor.AffinityRules.Affinity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** 방어구 성향 툴팁 (2026-11-21): 성향 이름, 이 부위 옵션(등급 구간 배율 적용), 공명 조건 */
public final class AffinityTooltip {
    private AffinityTooltip() {}

    public static ChatFormatting color(String id) {
        return switch (Affinity.byId(id)) {
            case ATTACK -> ChatFormatting.RED;
            case SPELL -> ChatFormatting.BLUE;
            case GUARD -> ChatFormatting.GREEN;
            case FINESSE -> ChatFormatting.YELLOW;
            case BALANCE -> ChatFormatting.GRAY;
        };
    }

    public static void add(List<Component> tip, ItemStack stack) {
        Affinity a = AffinityEffects.of(stack);
        AffinityRules.Settings s = AffinityEffects.settings();
        Map<String, Double> o = s.options().get(a);
        if (o == null) return;
        double mult = s.band()[AffinityRules.bandIndex(GearRegistry.tierOf(stack))];
        int at = Math.min(1, tip.size());
        tip.add(at++, Component.translatable("tooltip.fantasyrpg.affinity", Component.translatable("affinity.fantasyrpg." + a.id()))
                .withStyle(color(a.id())));
        for (var e : o.entrySet()) {
            double v = e.getValue() * mult;
            String shown = e.getKey().equals("max_health") || e.getKey().equals("skill_haste")
                    ? String.format("%.1f", v) : String.format("%.1f%%", v * 100);
            tip.add(at++, Component.literal("  ").append(Component.translatable("affinity.fantasyrpg.key." + e.getKey(), shown))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (a.hasDirection()) tip.add(at, Component.translatable("tooltip.fantasyrpg.affinity_resonance", s.resonancePieces(),
                Component.translatable("direction.fantasyrpg." + a.id()), s.resonanceSp(), s.resonanceMult()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
