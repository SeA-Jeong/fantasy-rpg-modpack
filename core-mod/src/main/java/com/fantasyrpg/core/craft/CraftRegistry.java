package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.FantasyRpgCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 확장 작업대 2종, 공통 부품 7종, 지역 소재 5종, 계열 핵심 부품 5종, 작업대 화면, 1급 계열 갑옷 만들기 조합.
 * 기준: claude/작업대_방어구제작_초안.md v1. 그림은 바닐라 그림 임시.
 */
public final class CraftRegistry {
    private CraftRegistry() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FantasyRpgCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyRpgCore.MOD_ID);

    public static final DeferredBlock<GridBenchBlock> BENCH_4 = BLOCKS.register("workbench_4x4",
            () -> new GridBenchBlock(4, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredBlock<GridBenchBlock> BENCH_5 = BLOCKS.register("workbench_5x5",
            () -> new GridBenchBlock(5, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));
    public static final DeferredItem<BlockItem> BENCH_4_ITEM = ITEMS.registerSimpleBlockItem(BENCH_4);
    public static final DeferredItem<BlockItem> BENCH_5_ITEM = ITEMS.registerSimpleBlockItem(BENCH_5);

    /** 재료 아이템 (id → 등록) — 순서대로 창작 탭에 */
    public static final Map<String, DeferredItem<Item>> MATERIALS = new LinkedHashMap<>();
    /** 공통 부품 */
    public static final List<String> PARTS = List.of("reinforced_alloy", "gear_frame", "reinforced_lining", "arcane_circuit",
            "precision_part", "armor_plate", "mana_conductor");
    /** 지역 소재 (보스 꾸러미) */
    public static final List<String> REGIONAL = List.of("sanctum_crystal", "dwarf_steel", "dragon_scale", "moonsilver_thread", "ancient_glyphstone");
    /** 계열 핵심 부품 */
    /** 룬석 공통 재료 (2026-11-21, claude/룬석_재료드롭_초안.md) */
    public static final List<String> RUNE_MATS = List.of("rune_ore");
    /** 성향 각인석 (2026-11-21, claude/방어구성향_초안.md) — affinity_<성향> */
    public static final List<String> AFFINITY_STONES = List.of("affinity_attack", "affinity_spell", "affinity_guard", "affinity_finesse");
    public static final List<String> CORES = List.of("sanctum_plate", "dwarf_steel_plate", "dragonscale_plate", "spirit_weave", "rune_frame");

    static {
        for (String id : PARTS) MATERIALS.put(id, ITEMS.registerSimpleItem(id));
        for (String id : REGIONAL) MATERIALS.put(id, ITEMS.registerSimpleItem(id));
        for (String id : CORES) MATERIALS.put(id, ITEMS.registerSimpleItem(id));
        for (String id : RUNE_MATS) MATERIALS.put(id, ITEMS.registerSimpleItem(id));
        for (String id : AFFINITY_STONES) MATERIALS.put(id, ITEMS.registerSimpleItem(id));
    }

    /** 성향 각인석이면 그 성향, 아니면 null */
    public static com.fantasyrpg.core.gear.armor.AffinityRules.Affinity stoneAffinity(ItemStack s) {
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        if (!key.startsWith(FantasyRpgCore.MOD_ID + ":affinity_")) return null;
        return com.fantasyrpg.core.gear.armor.AffinityRules.Affinity.byId(key.substring((FantasyRpgCore.MOD_ID + ":affinity_").length()));
    }

    public static final DeferredHolder<MenuType<?>, MenuType<GridCraftMenu>> MENU_4 = MENUS.register("workbench_4x4",
            () -> new MenuType<>((id, inv) -> new GridCraftMenu(4, id, inv, ContainerLevelAccess.NULL), FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<GridCraftMenu>> MENU_5 = MENUS.register("workbench_5x5",
            () -> new MenuType<>((id, inv) -> new GridCraftMenu(5, id, inv, ContainerLevelAccess.NULL), FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FamilyArmorCreateRecipe>> ARMOR_CREATE =
            SERIALIZERS.register("family_armor_create", () -> new SimpleCraftingRecipeSerializer<>(FamilyArmorCreateRecipe::new));
    /** 계열 갑옷 수리 (2026-11-21) */
    /** 성향 재조율 (2026-11-21) */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FamilyArmorRetuneRecipe>> ARMOR_RETUNE =
            SERIALIZERS.register("family_armor_retune", () -> new SimpleCraftingRecipeSerializer<>(FamilyArmorRetuneRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FamilyArmorRepairRecipe>> ARMOR_REPAIR =
            SERIALIZERS.register("family_armor_repair", () -> new SimpleCraftingRecipeSerializer<>(FamilyArmorRepairRecipe::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("crafting", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fantasyrpg.crafting"))
            .icon(() -> new ItemStack(BENCH_5_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(BENCH_4_ITEM.get());
                out.accept(BENCH_5_ITEM.get());
                for (DeferredItem<Item> it : MATERIALS.values()) out.accept(it.get());
            })
            .build());

    public static MenuType<GridCraftMenu> menuType(int size) {
        return size >= 5 ? MENU_5.get() : MENU_4.get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        SERIALIZERS.register(modBus);
        TABS.register(modBus);
    }
}
