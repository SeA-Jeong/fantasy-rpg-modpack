package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.FantasyRpgCore;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 계열 방어구 등록: 등급 컴포넌트, 방어구 재질, 계열×부위 아이템 20종, 시험용 창작 탭.
 *
 * <p>재질의 방어력·강도는 0이다. 실제 능력치는 {@link FamilyArmorAttributes}가 수치표와 등급으로 붙인다.
 * 그림은 전용 그림이 나올 때까지 바닐라 갑옷 그림을 빌려 쓴다({@link ArmorFamily#placeholderLook()}).
 */
public final class GearRegistry {
    private GearRegistry() {
    }

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, FantasyRpgCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyRpgCore.MOD_ID);

    /** 장비 등급 1~10 ({@code fantasyrpg:gear_tier}). 없으면 1급으로 본다. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GEAR_TIER =
            COMPONENTS.register("gear_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(ArmorStatTable.MIN_TIER, ArmorStatTable.MAX_TIER))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 내구도 배수 (다이아와 같은 33). 계열별 내구도는 [미정] */
    private static final int DURABILITY_MULTIPLIER = 33;
    private static final int ENCHANTABILITY = 10;

    private static final Map<ArmorFamily, DeferredHolder<ArmorMaterial, ArmorMaterial>> MATERIAL_BY_FAMILY =
            new EnumMap<>(ArmorFamily.class);
    private static final Map<ArmorFamily, Map<ArmorPart, DeferredItem<FamilyArmorItem>>> ITEM_BY_FAMILY =
            new EnumMap<>(ArmorFamily.class);

    static {
        for (ArmorFamily family : ArmorFamily.values()) {
            DeferredHolder<ArmorMaterial, ArmorMaterial> material = MATERIALS.register(family.id(), () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                        for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
                    }),
                    ENCHANTABILITY,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace(family.placeholderLayer()))),
                    0.0F,
                    0.0F));
            MATERIAL_BY_FAMILY.put(family, material);

            Map<ArmorPart, DeferredItem<FamilyArmorItem>> parts = new EnumMap<>(ArmorPart.class);
            for (ArmorPart part : ArmorPart.values()) {
                ArmorItem.Type type = typeOf(part);
                parts.put(part, ITEMS.register(family.id() + "_" + part.itemSuffix(), () -> new FamilyArmorItem(
                        material, type, family, part,
                        new Item.Properties().durability(type.getDurability(DURABILITY_MULTIPLIER)))));
            }
            ITEM_BY_FAMILY.put(family, parts);
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("gear", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fantasyrpg.gear"))
            .icon(() -> stack(ArmorFamily.KNIGHT, ArmorPart.CHEST, 10))
            .displayItems((params, output) -> {
                // 시험용: 계열마다 1~10급 4부위
                for (ArmorFamily family : ArmorFamily.values()) {
                    for (int tier = ArmorStatTable.MIN_TIER; tier <= ArmorStatTable.MAX_TIER; tier++) {
                        for (ArmorPart part : ArmorPart.values()) output.accept(stack(family, part, tier));
                    }
                }
            })
            .build());

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    public static ArmorItem.Type typeOf(ArmorPart part) {
        return switch (part) {
            case HEAD -> ArmorItem.Type.HELMET;
            case CHEST -> ArmorItem.Type.CHESTPLATE;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
        };
    }

    public static FamilyArmorItem item(ArmorFamily family, ArmorPart part) {
        return ITEM_BY_FAMILY.get(family).get(part).get();
    }

    /** 등급을 정한 계열 방어구 한 개 */
    public static ItemStack stack(ArmorFamily family, ArmorPart part, int tier) {
        ItemStack s = new ItemStack(item(family, part));
        s.set(GEAR_TIER.get(), ArmorStatTable.clampTier(tier));
        return s;
    }

    /** 아이템의 등급 (없으면 1) */
    public static int tierOf(ItemStack stack) {
        Integer t = stack.get(GEAR_TIER.get());
        return t == null ? ArmorStatTable.MIN_TIER : ArmorStatTable.clampTier(t);
    }
}
