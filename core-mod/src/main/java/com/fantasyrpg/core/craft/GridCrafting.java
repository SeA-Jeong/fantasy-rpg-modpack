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

    /** 결과와 필요한 진행 등급. extras = 결과를 가져갈 때 함께 주는 것 (분해, 2026-11-21) */
    public record Result(ItemStack out, int needGrade, List<ItemStack> extras) {
        public static final Result NONE = new Result(ItemStack.EMPTY, 0);

        public Result(ItemStack out, int needGrade) {
            this(out, needGrade, List.of());
        }
    }

    public static boolean accepts(Entry e, ItemStack s) {
        if (e.isTag()) return s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(e.id().substring(1))));
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(e.id());
    }

    public static ItemStack stackOf(String id, int count) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return new ItemStack(it, count);
    }

    /** 조합식 결과 (룬석·연마재는 등급을 붙임) */
    public static ItemStack resultOf(CraftRules.GridRecipe r) {
        ItemStack s = stackOf(r.result(), r.resultCount());
        if (r.resultGrade() > 0) s.set(com.fantasyrpg.core.gear.rune.RuneRegistry.RUNE_GRADE.get(), r.resultGrade());
        return s;
    }

    /** @param bench 작업대 크기 (4·5) */
    public static Result find(int bench, List<ItemStack> grid) {
        List<ItemStack> filled = new ArrayList<>();
        for (ItemStack s : grid) if (!s.isEmpty()) filled.add(s);
        if (filled.isEmpty()) return Result.NONE;
        Result up = upgrade(bench, filled);
        if (up != null) return up;
        Result rw = rework(bench, filled);
        if (rw != null) return rw;
        for (GridRecipe r : CraftData.recipes()) {
            if (r.grid() > bench) continue;
            if (CraftRules.matches(r.ingredients(), filled, GridCrafting::accepts)) return new Result(resultOf(r), r.grade());
        }
        return Result.NONE;
    }

    /** 분해(갑옷 + 숫돌) 또는 계열 전환(갑옷 + 다른 계열 재료) */
    private static Result rework(int bench, List<ItemStack> filled) {
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
        if (armor == null || rest.isEmpty()) return null;
        FamilyArmorItem fa = (FamilyArmorItem) armor.getItem();
        int tier = GearRegistry.tierOf(armor);
        ReworkRules.Settings s = CraftData.rework();
        String part = fa.part().itemSuffix();
        // 분해
        if (rest.size() == 1 && accepts(new Entry(s.dismantleTool(), 1), rest.get(0))) {
            if (!com.fantasyrpg.core.gear.rune.RuneRegistry.socketsOf(armor).runes().isEmpty()) return null;   // 룬석 먼저 빼기
            java.util.Map<String, Integer> back = ReworkRules.dismantle(t, s, fa.family().id(), part, tier);
            if (back.isEmpty()) return null;
            List<ItemStack> stacks = new ArrayList<>();
            for (var e : back.entrySet()) {
                ItemStack st = stackOf(e.getKey(), e.getValue());
                if (!st.isEmpty()) stacks.add(st);
            }
            if (stacks.isEmpty()) return null;
            return new Result(stacks.get(0), 1, List.copyOf(stacks.subList(1, stacks.size())));
        }
        // 계열 전환
        if (ReworkRules.convertGrid(tier) > bench) return null;
        for (com.fantasyrpg.core.gear.armor.ArmorFamily to : com.fantasyrpg.core.gear.armor.ArmorFamily.values()) {
            List<Entry> cost = ReworkRules.convertCost(t, s, fa.family().id(), to.id(), part, tier);
            if (cost == null || !CraftRules.matches(cost, rest, GridCrafting::accepts)) continue;
            if (!to.light() && hasLightOnlyRune(armor)) return null;
            ItemStack out = armor.transmuteCopy(GearRegistry.item(to, fa.part()), 1);
            out.remove(DataComponents.DAMAGE);
            return new Result(out, tier);
        }
        return null;
    }

    private static boolean hasLightOnlyRune(ItemStack armor) {
        for (var e : com.fantasyrpg.core.gear.rune.RuneRegistry.socketsOf(armor).runes()) {
            com.fantasyrpg.core.gear.rune.RuneType rt = com.fantasyrpg.core.gear.rune.RuneType.byId(e.rune());
            com.fantasyrpg.core.gear.rune.RuneDefinition d = rt == null ? null : com.fantasyrpg.core.gear.rune.RuneRegistry.def(rt);
            if (d != null && d.lightOnly()) return true;
        }
        return false;
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
