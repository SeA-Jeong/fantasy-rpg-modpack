package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.craft.CraftRules.Entry;
import com.fantasyrpg.core.craft.CraftRules.GridRecipe;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 작업대 칸 → 결과 찾기 (계열 갑옷 승급 먼저, 그다음 작업대 조합식). 진행 등급 판정은 화면(메뉴)에서 */
public final class GridCrafting {
    private GridCrafting() {
    }

    /** 결과와 필요한 진행 등급 */
    public record Result(ItemStack out, int needGrade) {
        public static final Result NONE = new Result(ItemStack.EMPTY, 0);
    }

    public static boolean accepts(Entry e, ItemStack s) {
        if (e.isTag()) return s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(e.id().substring(1))));
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(e.id());
    }

    public static ItemStack stackOf(String id, int count) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return new ItemStack(it, count);
    }

    /** @param bench 작업대 크기 (4·5) */
    public static Result find(int bench, List<ItemStack> grid) {
        List<ItemStack> filled = new ArrayList<>();
        for (ItemStack s : grid) if (!s.isEmpty()) filled.add(s);
        if (filled.isEmpty()) return Result.NONE;
        Result up = upgrade(bench, filled);
        if (up != null) return up;
        for (GridRecipe r : CraftData.recipes()) {
            if (r.grid() > bench) continue;
            if (CraftRules.matches(r.ingredients(), filled, GridCrafting::accepts)) return new Result(stackOf(r.result(), r.resultCount()), r.grade());
        }
        return Result.NONE;
    }

    /** 계열 갑옷 1개 + 승급 재료 → 등급 +1 (인챈트·장식·이름·염색·룬석 그대로, 내구도 가득) */
    private static Result upgrade(int bench, List<ItemStack> filled) {
        ArmorTable t = CraftData.armor();
        if (t == null) return null;
        ItemStack armor = null;
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack s : filled) {
            if (s.getItem() instanceof FamilyArmorItem) {
                if (armor != null) return null;
                armor = s;
            } else rest.add(s);
        }
        if (armor == null) return null;
        FamilyArmorItem fa = (FamilyArmorItem) armor.getItem();
        int target = GearRegistry.tierOf(armor) + 1;
        if (target > CraftRules.MAX_UPGRADE || CraftRules.gridForTier(target) > bench) return null;
        List<Entry> cost = t.upgradeCost(fa.family().id(), fa.part().itemSuffix(), target);
        if (cost == null || !CraftRules.matches(cost, rest, GridCrafting::accepts)) return null;
        ItemStack out = armor.copyWithCount(1);
        out.set(GearRegistry.GEAR_TIER.get(), target);
        out.remove(DataComponents.DAMAGE);
        return new Result(out, target);
    }
}
