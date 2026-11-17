package com.fantasyrpg.core.gear.armorcap;

import com.fantasyrpg.core.RpgConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 바닐라 다이아몬드·네더라이트 갑옷의 방어력·방어 강도를 조정한다 (다이아 18/2, 네더라이트 20/6).
 *
 * <p>아이템을 새 아이템으로 바꾸지 않는다. 아이템 능력치를 조회할 때({@link ItemAttributeModifierEvent}) 해당 부위
 * 수정자의 값만 바꾼다. 수정자의 id·연산·장착 칸은 원래 것을 그대로 쓴다. 철 갑옷(15/0)은 바닐라와 같아서 건드리지 않는다.
 * 넉백 저항은 바꾸지 않는다.
 *
 * <p>착용자 전체 상한({@link ArmorCapService})과는 따로 동작한다. 여기서 바꾼 값이 상한 계산의 입력이 되므로 두 번 깎이지 않는다.
 */
public final class VanillaArmorTuning {
    private VanillaArmorTuning() {
    }

    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (!RpgConfig.flag(RpgConfig.VANILLA_TUNING_ENABLED, true)) return;
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();

        int slot = slotIndex(item, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        double[] armor;
        double[] toughness;
        if (slot >= 0) {
            armor = RpgConfig.numbers(RpgConfig.DIAMOND_ARMOR, RpgConfig.DEFAULT_DIAMOND_ARMOR);
            toughness = RpgConfig.numbers(RpgConfig.DIAMOND_TOUGHNESS, RpgConfig.DEFAULT_DIAMOND_TOUGHNESS);
        } else {
            slot = slotIndex(item, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
            if (slot < 0) return;
            armor = RpgConfig.numbers(RpgConfig.NETHERITE_ARMOR, RpgConfig.DEFAULT_NETHERITE_ARMOR);
            toughness = RpgConfig.numbers(RpgConfig.NETHERITE_TOUGHNESS, RpgConfig.DEFAULT_NETHERITE_TOUGHNESS);
        }
        replaceAmount(event, Attributes.ARMOR, armor[slot]);
        replaceAmount(event, Attributes.ARMOR_TOUGHNESS, toughness[slot]);
    }

    /** 투구 0, 흉갑 1, 각반 2, 장화 3. 해당 없으면 -1. */
    private static int slotIndex(Item item, Item helmet, Item chest, Item legs, Item boots) {
        if (item == helmet) return 0;
        if (item == chest) return 1;
        if (item == legs) return 2;
        if (item == boots) return 3;
        return -1;
    }

    /** 해당 속성의 ADD_VALUE 수정자 값을 바꾼다. id·장착 칸은 그대로. */
    private static void replaceAmount(ItemAttributeModifierEvent event, Holder<Attribute> attribute, double amount) {
        List<ItemAttributeModifiers.Entry> targets = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : event.getModifiers()) {
            if (entry.attribute().equals(attribute)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                targets.add(entry);
            }
        }
        for (ItemAttributeModifiers.Entry entry : targets) {
            AttributeModifier old = entry.modifier();
            if (old.amount() == amount) continue;
            event.removeModifier(attribute, old.id());
            event.addModifier(attribute, new AttributeModifier(old.id(), amount, old.operation()), entry.slot());
        }
    }
}
