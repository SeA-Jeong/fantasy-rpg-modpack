package com.fantasyrpg.core.gear.weapon;

import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * 궁수 활. 바닐라 활과 같이 당겨 쏘고, 직업 플레이어가 쏜 화살은 {@code combat/CombatEvents}가 RPG 피해로 바꾼다.
 * 등급은 {@code fantasyrpg:gear_tier}.
 */
public class ArcherBowItem extends BowItem {
    public ArcherBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.fantasyrpg.tiered", super.getName(stack), GearRegistry.tierOf(stack));
    }
}
