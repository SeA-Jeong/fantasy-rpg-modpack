package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.gear.armor.AffinityEffects;
import com.fantasyrpg.core.gear.armor.AffinityRules;
import com.fantasyrpg.core.gear.armor.AffinityRules.Affinity;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 일반 제작대(모양 없음): 계열 갑옷 1개 + 같은 성향 각인석 N개 → 그 성향으로 (2026-11-21, claude/방어구성향_초안.md).
 * N = 등급 구간(1~3급 1, 4~6급 2, 7~10급 3, armor_affinity/settings.json retune_stones).
 * 이미 그 성향이면 균형으로 되돌린다. 등급·룬석·인챈트·내구도·이름은 그대로.
 */
public class FamilyArmorRetuneRecipe extends CustomRecipe {
    public FamilyArmorRetuneRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static ItemStack result(List<ItemStack> filled) {
        ItemStack armor = null;
        Affinity stone = null;
        int stones = 0;
        for (ItemStack s : filled) {
            if (s.getItem() instanceof FamilyArmorItem) {
                if (armor != null) return ItemStack.EMPTY;
                armor = s;
                continue;
            }
            Affinity a = CraftRegistry.stoneAffinity(s);
            if (a == null || (stone != null && stone != a)) return ItemStack.EMPTY;
            stone = a;
            stones++;
        }
        if (armor == null || stone == null) return ItemStack.EMPTY;
        if (stones != AffinityRules.retuneStones(AffinityEffects.settings(), GearRegistry.tierOf(armor))) return ItemStack.EMPTY;
        ItemStack out = armor.copyWithCount(1);
        if (AffinityEffects.of(armor) == stone) out.remove(GearRegistry.AFFINITY.get());
        else out.set(GearRegistry.AFFINITY.get(), stone.id());
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
        return CraftRegistry.ARMOR_RETUNE.get();
    }
}
