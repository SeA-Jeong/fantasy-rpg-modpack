package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.rune.RuneRegistry;
import com.fantasyrpg.core.gear.rune.RuneRules;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 계열 방어구의 방어력·방어 강도·넉백 저항을 수치표({@link ArmorStatManager})와 아이템 등급으로 붙이고,
 * 끼운 룬석의 능력치를 더한다(소켓 수를 넘는 룬석·부위가 맞지 않는 룬석은 효과 없음).
 *
 * <p>재질 기본 수정자(값 0)를 지우고 부위별 고정 ID {@code fantasyrpg:armor/<부위>}로 넣는다.
 * 수정자는 아이템 능력치를 조회할 때마다 다시 만들므로 /reload 로 표를 바꾸면 바로 반영된다.
 * 받는 피해는 바닐라 방어 공식이 그대로 계산한다(방어력 최대 30, 강도 최대 20).
 */
public final class FamilyArmorAttributes {
    private FamilyArmorAttributes() {
    }

    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FamilyArmorItem item)) return;
        ArmorStatTable table = ArmorStatManager.INSTANCE.get(item.family());
        if (table == null) return; // 표 없음(전용 서버 클라이언트 툴팁 등): 재질 기본값 0 그대로
        if (FamilyArmorItem.isBroken(stack)) return; // 파손: 방어력·룬석 없음 (재질 기본값 0, 2026-11-21)

        int tier = GearRegistry.tierOf(stack);
        ArmorPart part = item.part();
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(item.getEquipmentSlot());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "armor/" + part.itemSuffix());

        replace(event, Attributes.ARMOR, id, table.partArmor(tier, part), slot);
        replace(event, Attributes.ARMOR_TOUGHNESS, id, table.partToughness(tier, part), slot);
        replace(event, Attributes.KNOCKBACK_RESISTANCE, id, table.partKnockback(tier, part), slot);

        // 룬석: 칸마다 고유 ID fantasyrpg:rune/<부위>/<칸번호> (같은 룬석을 여러 칸에 끼우면 그대로 더해짐)
        boolean light = "light".equals(table.armorClass());
        for (RuneRules.Active a : RuneRules.active(RuneRegistry.socketsOf(stack).toRules(),
                table.tier(tier).sockets(), part, RuneRegistry::def)) {
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                    .getHolder(ResourceLocation.parse(a.def().attribute())).orElse(null);
            if (attribute == null) continue;
            ResourceLocation runeId = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID,
                    "rune/" + part.itemSuffix() + "/" + a.slot());
            event.addModifier(attribute, new AttributeModifier(runeId, a.def().value(a.grade(), light),
                    operation(a.def().operation())), slot);
        }
    }

    private static AttributeModifier.Operation operation(String name) {
        return switch (name) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static void replace(ItemAttributeModifierEvent event, Holder<Attribute> attribute, ResourceLocation id,
                                double amount, EquipmentSlotGroup slot) {
        List<ResourceLocation> old = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : event.getModifiers()) {
            if (entry.attribute().equals(attribute)) old.add(entry.modifier().id());
        }
        for (ResourceLocation o : old) event.removeModifier(attribute, o);
        if (amount > 0) {
            event.addModifier(attribute, new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), slot);
        }
    }
}
