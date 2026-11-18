package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.armor.ArmorStatManager;
import com.fantasyrpg.core.gear.armor.ArmorStatTable;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * 룬석 등록: 등급·소켓 컴포넌트, 룬석 6종·연마재·추출 도구, 제작대 조합 4종(끼우기·빼기·합성·연마).
 */
public final class RuneRegistry {
    private RuneRegistry() {
    }

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FantasyRpgCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FantasyRpgCore.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, FantasyRpgCore.MOD_ID);

    /** 룬석·연마재 등급 1~4 ({@code fantasyrpg:rune_grade}). 없으면 1(하급) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUNE_GRADE =
            COMPONENTS.register("rune_grade", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(RuneDefinition.MIN_GRADE, RuneDefinition.MAX_GRADE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 갑옷에 끼운 룬석 ({@code fantasyrpg:sockets}) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RuneSockets>> SOCKETS =
            COMPONENTS.register("sockets", () -> DataComponentType.<RuneSockets>builder()
                    .persistent(RuneSockets.CODEC)
                    .networkSynchronized(RuneSockets.STREAM_CODEC)
                    .build());

    private static final Map<RuneType, DeferredItem<RuneItem>> RUNES = new EnumMap<>(RuneType.class);

    static {
        for (RuneType t : RuneType.values()) {
            RUNES.put(t, ITEMS.register(t.itemName(), () -> new RuneItem(t, new Item.Properties())));
        }
    }

    /** 연마재 (등급 1~3, 같은 등급 룬석을 한 등급 올림) */
    public static final DeferredItem<RuneItem> ABRASIVE =
            ITEMS.register("rune_abrasive", () -> new RuneItem(null, new Item.Properties()));
    /** 추출 도구 (소모품: 마지막에 끼운 룬석을 돌려받음) */
    public static final DeferredItem<Item> EXTRACTOR =
            ITEMS.register("rune_extractor", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RuneRecipes.Insert>> INSERT =
            SERIALIZERS.register("rune_insert", () -> new SimpleCraftingRecipeSerializer<>(RuneRecipes.Insert::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RuneRecipes.Extract>> EXTRACT =
            SERIALIZERS.register("rune_extract", () -> new SimpleCraftingRecipeSerializer<>(RuneRecipes.Extract::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RuneRecipes.Combine>> COMBINE =
            SERIALIZERS.register("rune_combine", () -> new SimpleCraftingRecipeSerializer<>(RuneRecipes.Combine::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RuneRecipes.Polish>> POLISH =
            SERIALIZERS.register("rune_polish", () -> new SimpleCraftingRecipeSerializer<>(RuneRecipes.Polish::new));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        SERIALIZERS.register(modBus);
    }

    public static RuneItem item(RuneType type) {
        return RUNES.get(type).get();
    }

    public static ItemStack runeStack(RuneType type, int grade) {
        ItemStack s = new ItemStack(item(type));
        s.set(RUNE_GRADE.get(), RuneDefinition.clampGrade(grade));
        return s;
    }

    public static ItemStack abrasiveStack(int grade) {
        ItemStack s = new ItemStack(ABRASIVE.get());
        s.set(RUNE_GRADE.get(), Math.max(1, Math.min(RuneRules.MAX_ABRASIVE_GRADE, grade)));
        return s;
    }

    public static int gradeOf(ItemStack stack) {
        Integer g = stack.get(RUNE_GRADE.get());
        return g == null ? RuneDefinition.MIN_GRADE : RuneDefinition.clampGrade(g);
    }

    public static RuneSockets socketsOf(ItemStack stack) {
        RuneSockets s = stack.get(SOCKETS.get());
        return s == null ? RuneSockets.EMPTY : s;
    }

    /** 계열 갑옷의 소켓 수. 수치표가 없거나 계열 갑옷이 아니면 0 */
    public static int socketCount(ItemStack armor) {
        if (!(armor.getItem() instanceof FamilyArmorItem item)) return 0;
        ArmorStatTable table = ArmorStatManager.INSTANCE.get(item.family());
        return table == null ? 0 : table.tier(GearRegistry.tierOf(armor)).sockets();
    }

    /** 룬석 종류의 효과 데이터 (없으면 null) */
    public static RuneDefinition def(RuneType type) {
        return RuneStatManager.INSTANCE.get(type);
    }
}
