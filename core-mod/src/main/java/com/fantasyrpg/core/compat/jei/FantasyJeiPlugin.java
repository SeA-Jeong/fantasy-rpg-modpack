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
 * JEI가 없으면 이 클래스는 로드되지 않는다. 알려진 한계: 전용 서버에 접속한 클라이언트는 조합식 데이터가 없어 비어 보임(혼자 하기는 보임).
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

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
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
        registration.addRecipes(GridCategory.TYPE, list);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CraftRegistry.BENCH_4_ITEM.get()), GridCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(CraftRegistry.BENCH_5_ITEM.get()), GridCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), GridCategory.TYPE);
    }
}
