package com.fantasyrpg.core.gear.armor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * 계열 방어구 한 부위. 등급은 아이템이 아니라 {@code fantasyrpg:gear_tier} 컴포넌트에 있다.
 * 이름 뒤에 "(N급)"을 붙인다.
 */
public final class FamilyArmorItem extends ArmorItem {
    private final ArmorFamily family;
    private final ArmorPart part;

    public FamilyArmorItem(Holder<ArmorMaterial> material, Type type, ArmorFamily family, ArmorPart part, Properties properties) {
        super(material, type, properties);
        this.family = family;
        this.part = part;
    }

    public ArmorFamily family() {
        return family;
    }

    public ArmorPart part() {
        return part;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.fantasyrpg.tiered", super.getName(stack), GearRegistry.tierOf(stack));
    }
}
