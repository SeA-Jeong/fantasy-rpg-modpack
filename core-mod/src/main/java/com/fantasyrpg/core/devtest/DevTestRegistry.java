package com.fantasyrpg.core.devtest;

import com.fantasyrpg.core.FantasyRpgCore;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트 장비 등록 (게임 시작 때 한 번, 목록의 항목마다 아이템 하나).
 * 기존 장비와 다른 레지스터·재질·창작 탭을 쓴다. 재질은 방어력·강도·인챈트 0, 수리 재료 없음.
 */
public final class DevTestRegistry {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, FantasyRpgCore.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FantasyRpgCore.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyRpgCore.MOD_ID);
    private static final Map<String, DeferredItem<DevTestArmorItem>> BY_ID = new LinkedHashMap<>();

    private DevTestRegistry() {
    }

    static void register(IEventBus modBus, DevTestManifest manifest) {
        DeferredHolder<ArmorMaterial, ArmorMaterial> material = MATERIALS.register("devtest", () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
                }),
                0,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "devtest"))),
                0.0F,
                0.0F));
        for (DevTestManifest.Entry e : manifest.entries()) {
            BY_ID.put(e.id(), ITEMS.register(e.id(), () -> new DevTestArmorItem(material, typeOf(e.slot()), e, manifest.namespace(),
                    new Item.Properties().stacksTo(1))));
        }
        TABS.register("devtest", () -> CreativeModeTab.builder()
                .title(Component.literal("판타지 RPG 개발 테스트 장비"))
                .icon(() -> BY_ID.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.ARMOR_STAND) : new ItemStack(BY_ID.values().iterator().next().get()))
                .displayItems((params, out) -> BY_ID.values().forEach(h -> out.accept(h.get())))
                .build());
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    public static Map<String, DeferredItem<DevTestArmorItem>> items() {
        return Collections.unmodifiableMap(BY_ID);
    }

    static ArmorItem.Type typeOf(String slot) {
        return switch (slot) {
            case "helmet" -> ArmorItem.Type.HELMET;
            case "leggings" -> ArmorItem.Type.LEGGINGS;
            case "boots" -> ArmorItem.Type.BOOTS;
            default -> ArmorItem.Type.CHESTPLATE;
        };
    }
}
