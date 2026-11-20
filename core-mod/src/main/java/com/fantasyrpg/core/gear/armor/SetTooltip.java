package com.fantasyrpg.core.gear.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Map;

/** 계열 갑옷 툴팁의 세트 효과 줄 (지금 입은 상태 기준, 켜진 효과는 초록·꺼진 효과는 회색) */
public final class SetTooltip {
    private SetTooltip() {}

    public static void add(ItemTooltipEvent event, FamilyArmorItem fa) {
        Player p = event.getEntity();
        ArmorFamily f = fa.family();
        SetRules.Worn w = p == null ? null : SetEffects.worn(p).get(f);
        int pieces = w == null ? 0 : w.pieces();
        List<Component> tip = event.getToolTip();
        tip.add(Component.translatable("tooltip.fantasyrpg.set", Component.translatable("set.fantasyrpg." + f.id()), pieces)
                .withStyle(ChatFormatting.GOLD));
        line(tip, f, "two", w != null && w.two(), w == null ? 0 : w.grade2());
        line(tip, f, "four", w != null && w.four(), w == null ? 0 : w.grade4());
    }

    private static void line(List<Component> tip, ArmorFamily f, String which, boolean on, int grade) {
        double band = SetRules.band(grade);
        Component c = Component.translatable("set.fantasyrpg." + f.id() + "." + which);
        if (on && band > 1) c = Component.empty().append(c).append(Component.literal(" (×" + (band % 1 == 0 ? String.valueOf((int) band) : String.valueOf(band)) + ", " + grade + "급)"));
        tip.add(Component.literal(" ").append(c).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }

    /** 테스트·명령용: 계열별 상태 문자열 */
    public static String describe(Map<ArmorFamily, SetRules.Worn> worn) {
        StringBuilder b = new StringBuilder();
        for (var e : worn.entrySet()) b.append(e.getKey().id()).append(' ').append(e.getValue().pieces()).append("/4 ");
        return b.toString().trim();
    }
}
