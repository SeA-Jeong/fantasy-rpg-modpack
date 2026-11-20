package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 일반 제작대(3×3, 모양 없음): 닳거나 파손된 계열 갑옷 1개 + 수리 재료 → 내구도 가득 (2026-11-21).
 * 등급·룬석·인챈트·이름은 그대로. 재료는 {@link ReworkRules#repairCost}.
 */
public class FamilyArmorRepairRecipe extends CustomRecipe {
    public FamilyArmorRepairRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static ItemStack result(List<ItemStack> filled) {
        ArmorTable t = CraftData.armor();
        if (t == null || filled.size() < 2) return ItemStack.EMPTY;
        ItemStack armor = null;
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack s : filled) {
            if (s.getItem() instanceof FamilyArmorItem) {
                if (armor != null) return ItemStack.EMPTY;
                armor = s;
            } else rest.add(s);
        }
        if (armor == null || !armor.isDamaged()) return ItemStack.EMPTY;
        FamilyArmorItem fa = (FamilyArmorItem) armor.getItem();
        List<CraftRules.Entry> cost = ReworkRules.repairCost(t, CraftData.rework(), fa.family().id(), GearRegistry.tierOf(armor));
        if (cost == null || !CraftRules.matches(cost, rest, GridCrafting::accepts)) return ItemStack.EMPTY;
        ItemStack out = armor.copyWithCount(1);
        out.remove(DataComponents.DAMAGE);
        return out;
    }

    private static List<ItemStack> filled(CraftingInput input) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) if (!input.getItem(i).isEmpty()) out.add(input.getItem(i));
        return out;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(filled(input)).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result(filled(input));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CraftRegistry.ARMOR_REPAIR.get();
    }
}
