package com.fantasyrpg.core.gear.weapon;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.combat.CombatManager;
import com.fantasyrpg.core.combat.CombatSettings;
import com.fantasyrpg.core.gear.armor.ArmorStatTable;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 직업 무기 6종 등록과, 근접 무기·지팡이의 공격력·공격 속도 속성(데이터의 W·attack_speed).
 * 공격력 속성은 직업 없는 플레이어(바닐라 경로)와 툴팁에 쓰인다. 직업 플레이어의 피해는 RPG 계산.
 */
public final class WeaponRegistry {
    private WeaponRegistry() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FantasyRpgCore.MOD_ID);
    private static final Map<WeaponType, DeferredItem<? extends Item>> BY_TYPE = new EnumMap<>(WeaponType.class);

    /** 바닐라 무기와 같은 수정자 ID (툴팁에서 기본 공격력으로 표시됨) */
    private static final ResourceLocation BASE_DAMAGE = ResourceLocation.withDefaultNamespace("base_attack_damage");
    private static final ResourceLocation BASE_SPEED = ResourceLocation.withDefaultNamespace("base_attack_speed");

    static {
        for (WeaponType t : WeaponType.values()) {
            if (t == WeaponType.BOW) {
                BY_TYPE.put(t, ITEMS.register(t.itemName(), () -> new ArcherBowItem(new Item.Properties().stacksTo(1))));
            } else {
                BY_TYPE.put(t, ITEMS.register(t.itemName(), () -> new ClassWeaponItem(t, new Item.Properties().stacksTo(1))));
            }
        }
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static Item item(WeaponType t) {
        return BY_TYPE.get(t).get();
    }

    public static ItemStack stack(WeaponType t, int tier) {
        ItemStack s = new ItemStack(item(t));
        s.set(GearRegistry.GEAR_TIER.get(), ArmorStatTable.clampTier(tier));
        return s;
    }

    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ClassWeaponItem weapon)) return;
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (s == null) return;
        CombatSettings.ClassAttack a = s.forClass(weapon.type().rpgClass().id());
        if (a == null) return;
        removeAll(event, Attributes.ATTACK_DAMAGE);
        removeAll(event, Attributes.ATTACK_SPEED);
        // 플레이어 기본 공격력 1 + 수정자 = W
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_DAMAGE,
                s.power(GearRegistry.tierOf(stack)) - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (a.attackSpeed() > 0) {
            // 플레이어 기본 공격 속도 4 + 수정자 = attack_speed
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_SPEED,
                    a.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }

    private static void removeAll(ItemAttributeModifierEvent event, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ItemAttributeModifiers.Entry e : event.getModifiers()) {
            if (e.attribute().equals(attr)) ids.add(e.modifier().id());
        }
        for (ResourceLocation id : ids) event.removeModifier(attr, id);
    }
}
