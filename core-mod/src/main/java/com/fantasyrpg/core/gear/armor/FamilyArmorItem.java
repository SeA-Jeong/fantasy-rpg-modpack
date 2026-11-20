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

    /**
     * 파손 (2026-11-21): 내구도가 1 남으면 더 닳지 않고 부서지지도 않는다. 대신 방어력·룬석·세트 효과가 꺼지고,
     * 제작대에서 수리 재료(armor_crafting/costs.json rework.repair)로 고친다. 등급·룬석·인챈트는 그대로 남는다.
     */
    public static boolean isBroken(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max > 1 && stack.getDamageValue() >= max - 1;
    }

    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
                                                                             java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        int left = stack.getMaxDamage() - 1 - stack.getDamageValue();
        int applied = Math.max(0, Math.min(amount, left));
        if (applied > 0 && applied == left && entity instanceof net.minecraft.world.entity.player.Player p && !p.level().isClientSide()) {
            p.displayClientMessage(Component.translatable("message.fantasyrpg.armor_broken", stack.getHoverName())
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        return applied;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = Component.translatable("item.fantasyrpg.tiered", super.getName(stack), GearRegistry.tierOf(stack));
        String aff = stack.get(GearRegistry.AFFINITY.get());
        if (aff == null) return base;                                                             // 균형은 표시 안 함
        return Component.empty().append(base).append(Component.literal(" · "))
                .append(Component.translatable("affinity.fantasyrpg." + aff).withStyle(AffinityTooltip.color(aff)));
    }
}
