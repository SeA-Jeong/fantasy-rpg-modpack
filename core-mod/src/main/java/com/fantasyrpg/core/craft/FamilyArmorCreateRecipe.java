package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.gear.armor.ArmorFamily;
import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 일반 제작대(3×3, 모양 없음): 바닐라 갑옷 한 부위 + 계열 재료 → 그 계열 1급 갑옷.
 * 인챈트·장식·이름은 그대로 옮기고 내구도는 가득. 재료는 armor_crafting/costs.json 의 create.
 * 성향 각인석 1개를 더 넣으면 그 성향으로 만든다(없으면 균형, 2026-11-21).
 */
public class FamilyArmorCreateRecipe extends CustomRecipe {
    public FamilyArmorCreateRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static ItemStack result(List<ItemStack> filled) {
        ArmorTable t = CraftData.armor();
        if (t == null || filled.size() < 2) return ItemStack.EMPTY;
        com.fantasyrpg.core.gear.armor.AffinityRules.Affinity aff = null;
        List<ItemStack> noStone = new ArrayList<>();
        for (ItemStack s : filled) {
            var a = CraftRegistry.stoneAffinity(s);
            if (a != null) {
                if (aff != null) return ItemStack.EMPTY;   // 각인석은 하나만
                aff = a;
            } else noStone.add(s);
        }
        filled = noStone;
        for (ArmorFamily f : ArmorFamily.values()) {
            ArmorTable.Create c = t.create().get(f.id());
            if (c == null) continue;
            for (ArmorPart part : ArmorPart.values()) {
                String baseId = "minecraft:" + c.base() + "_" + part.itemSuffix();
                ItemStack base = null;
                List<ItemStack> rest = new ArrayList<>();
                for (ItemStack s : filled) {
                    if (base == null && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(baseId)) base = s;
                    else rest.add(s);
                }
                if (base == null) continue;
                if (!CraftRules.matches(t.createCost(f.id(), part.itemSuffix()), rest, GridCrafting::accepts)) continue;
                ItemStack out = GearRegistry.stack(f, part, 1);
                out.applyComponents(base.getComponentsPatch());
                out.set(GearRegistry.GEAR_TIER.get(), 1);
                out.remove(DataComponents.DAMAGE);
                if (aff != null) out.set(GearRegistry.AFFINITY.get(), aff.id());
                return out;
            }
        }
        return ItemStack.EMPTY;
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
        return CraftRegistry.ARMOR_CREATE.get();
    }
}
