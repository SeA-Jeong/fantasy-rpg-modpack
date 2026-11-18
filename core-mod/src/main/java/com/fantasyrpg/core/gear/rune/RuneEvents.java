package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 룬석 관련 이벤트.
 * <ul>
 *   <li>모루: 룬석이 끼워진 갑옷은 오른쪽 칸(합치는 재료)에 넣지 못한다 → 룬석이 사라지지 않게</li>
 *   <li>툴팁: 갑옷에는 "룬석 칸 n/m"과 끼운 룬석, 룬석에는 끼울 수 있는 부위 (인챈트 줄과 따로)</li>
 * </ul>
 * 숫돌·인챈트 테이블은 룬석 컴포넌트를 건드리지 않으므로 따로 처리하지 않는다.
 */
public final class RuneEvents {
    private RuneEvents() {
    }

    public static void onAnvil(AnvilUpdateEvent event) {
        if (!RuneRegistry.socketsOf(event.getRight()).isEmpty()) event.setCanceled(true);
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tip = event.getToolTip();
        if (stack.getItem() instanceof FamilyArmorItem) {
            int count = RuneRegistry.socketCount(stack);
            List<RuneSockets.Entry> runes = RuneRegistry.socketsOf(stack).runes();
            if (count == 0 && runes.isEmpty()) return;
            int at = Math.min(1, tip.size());
            tip.add(at++, Component.translatable("tooltip.fantasyrpg.sockets", runes.size(), count)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            for (int i = 0; i < runes.size(); i++) {
                RuneType type = RuneType.byId(runes.get(i).rune());
                if (type == null) continue;
                MutableComponent line = Component.literal("  ")
                        .append(RuneRegistry.runeStack(type, runes.get(i).grade()).getHoverName());
                if (i >= count) line.append(Component.translatable("tooltip.fantasyrpg.socket_inactive"));
                tip.add(at++, line.withStyle(i >= count ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY));
            }
        } else if (stack.getItem() instanceof RuneItem rune && rune.type() != null) {
            RuneDefinition def = RuneRegistry.def(rune.type());
            if (def == null) return;
            MutableComponent parts = Component.empty();
            boolean first = true;
            for (ArmorPart p : ArmorPart.values()) {
                if (!def.fits(p)) continue;
                if (!first) parts.append(", ");
                parts.append(Component.translatable("armor.fantasyrpg.part." + p.itemSuffix()));
                first = false;
            }
            tip.add(Math.min(1, tip.size()), Component.translatable("tooltip.fantasyrpg.rune_parts", parts)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
