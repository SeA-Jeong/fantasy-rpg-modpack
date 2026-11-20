package com.fantasyrpg.core.compat.jei;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.craft.CraftData;
import com.fantasyrpg.core.craft.CraftRegistry;
import com.fantasyrpg.core.craft.CraftRules;
import com.fantasyrpg.core.craft.GridCrafting;
import com.fantasyrpg.core.gear.armor.ArmorFamily;
import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JEI: 4×4·5×5 작업대 조합식, 계열 갑옷 승급(2~6급), 1급 계열 갑옷 만들기를 "작업대 조합" 분류로 보여 줌.
 * JEI가 없으면 이 클래스는 로드되지 않는다. 전용 서버에 접속한 클라이언트는 조합식이 서버 동기화(DataSync)로 늦게 오므로,
 * JEI 가 먼저 떴으면 받은 뒤 {@link #refresh()} 로 다시 넣는다 (2026-11-21).
 */
@JeiPlugin
public class FantasyJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GridCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private static List<ItemStack> stacks(CraftRules.Entry e) {
        if (e.isTag()) {
            return Arrays.asList(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(e.id().substring(1)))).getItems());
        }
        return List.of(GridCrafting.stackOf(e.id(), 1));
    }

    private static void addEntries(List<List<ItemStack>> in, List<CraftRules.Entry> entries) {
        for (CraftRules.Entry e : entries) {
            List<ItemStack> s = stacks(e);
            for (int i = 0; i < e.count(); i++) in.add(s);
        }
    }

    private static mezz.jei.api.runtime.IJeiRuntime runtime;
    /** 지금 JEI 에 들어 있는 조합 (다시 넣을 때 숨김) */
    private static List<GridCategory.Display> current = List.of();

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        current = displays();
        registration.addRecipes(GridCategory.TYPE, current);
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** 서버 데이터를 받은 뒤 (전용 서버): JEI 가 이미 떠 있으면 예전 것을 숨기고 새로 넣음 */
    public static void refresh() {
        if (runtime == null) return;
        var rm = runtime.getRecipeManager();
        if (!current.isEmpty()) rm.hideRecipes(GridCategory.TYPE, current);
        current = displays();
        if (!current.isEmpty()) rm.addRecipes(GridCategory.TYPE, current);
    }

    private static List<GridCategory.Display> displays() {
        List<GridCategory.Display> list = new ArrayList<>();
        for (CraftRules.GridRecipe r : CraftData.recipes()) {
            List<List<ItemStack>> in = new ArrayList<>();
            addEntries(in, r.ingredients());
            list.add(new GridCategory.Display(r.grid(), r.grade(), in, GridCrafting.resultOf(r), false));
        }
        CraftRules.ArmorTable t = CraftData.armor();
        if (t != null) {
            for (ArmorFamily f : ArmorFamily.values()) {
                for (ArmorPart part : ArmorPart.values()) {
                    List<CraftRules.Entry> create = t.createCost(f.id(), part.itemSuffix());
                    CraftRules.ArmorTable.Create c = t.create().get(f.id());
                    if (create != null && c != null) {
                        List<List<ItemStack>> in = new ArrayList<>();
                        ItemStack base = GridCrafting.stackOf("minecraft:" + c.base() + "_" + part.itemSuffix(), 1);
                        if (!base.is(Items.AIR)) in.add(List.of(base));
                        addEntries(in, create);
                        list.add(new GridCategory.Display(3, 1, in, GearRegistry.stack(f, part, 1), true));
                    }
                    for (int tier = 2; tier <= CraftRules.MAX_UPGRADE; tier++) {
                        List<CraftRules.Entry> cost = t.upgradeCost(f.id(), part.itemSuffix(), tier);
                        if (cost == null) continue;
                        List<List<ItemStack>> in = new ArrayList<>();
                        in.add(List.of(GearRegistry.stack(f, part, tier - 1)));
                        addEntries(in, cost);
                        list.add(new GridCategory.Display(CraftRules.gridForTier(tier), tier, in, GearRegistry.stack(f, part, tier), false));
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CraftRegistry.BENCH_4_ITEM.get()), GridCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(CraftRegistry.BENCH_5_ITEM.get()), GridCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), GridCategory.TYPE);
    }
}
