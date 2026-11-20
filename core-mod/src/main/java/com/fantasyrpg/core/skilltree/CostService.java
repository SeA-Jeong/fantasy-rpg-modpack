package com.fantasyrpg.core.skilltree;

import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 재료 비용 내기 (2026-11-21). 인벤토리에서 세어 모자라면 거부하고 무엇이 모자란지 알린다. 창작 모드는 무료.
 */
public final class CostService {
    private CostService() {}

    public static CostRules.Table table() {
        return com.fantasyrpg.core.skills.SkillSettings.INSTANCE.costs();
    }

    /** 비용 표시 (예: "에메랄드 ×16, 다이아몬드 ×2"), 무료면 null */
    public static MutableComponent describe(Map<String, Integer> need) {
        if (need.isEmpty()) return null;
        MutableComponent out = Component.empty();
        boolean first = true;
        for (var e : need.entrySet()) {
            if (!first) out.append(", ");
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()));
            out.append(new ItemStack(it).getHoverName()).append(" ×" + e.getValue());
            first = false;
        }
        return out;
    }

    /** 내기. times = 몇 번 분량(노드 되돌리기 랭크 수 등). 성공하면 true */
    public static boolean pay(ServerPlayer p, List<CostRules.Cost> costs, int times) {
        Map<String, Integer> need = CostRules.amounts(costs, MagicCityApi.grade(p), times);
        if (need.isEmpty() || p.isCreative()) return true;
        Inventory inv = p.getInventory();
        Map<String, Integer> have = new HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            have.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        Map<String, Integer> lack = CostRules.missing(need, have);
        if (!lack.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.fantasyrpg.cost.missing", describe(lack)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        for (var e : need.entrySet()) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()));
            int left = e.getValue();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(it)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        inv.setChanged();
        p.sendSystemMessage(Component.translatable("message.fantasyrpg.cost.paid", describe(need)).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
