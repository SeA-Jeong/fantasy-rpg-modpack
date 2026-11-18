package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 제작대에서 하는 룬석 조합 4종. 전용 작업대가 나오기 전까지 쓰는 방식이다
 * (claude/계열방어구_상위옵션_초안.md 3-4-1). 칸 위치는 상관없다.
 * <ul>
 *   <li>끼우기: 계열 갑옷 + 룬석 → 빈 칸에 끼운 갑옷</li>
 *   <li>빼기: 룬석이 있는 계열 갑옷 + 추출 도구 → 마지막 룬석을 뺀 갑옷, 추출 도구 자리에 룬석이 남음</li>
 *   <li>합성: 같은 종류·같은 등급 룬석 3개 → 한 등급 위 1개</li>
 *   <li>연마: 룬석 + 같은 등급 연마재 → 한 등급 위</li>
 * </ul>
 */
public final class RuneRecipes {
    private RuneRecipes() {
    }

    /** 비어 있지 않은 칸 (칸 번호, 아이템) */
    private record Slot(int index, ItemStack stack) {
    }

    private static List<Slot> filled(CraftingInput input) {
        List<Slot> out = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty()) out.add(new Slot(i, s));
        }
        return out;
    }

    private static boolean isRune(ItemStack s) {
        return s.getItem() instanceof RuneItem r && r.type() != null;
    }

    private static boolean isAbrasive(ItemStack s) {
        return s.is(RuneRegistry.ABRASIVE.get());
    }

    // ── 끼우기 ──
    public static final class Insert extends CustomRecipe {
        public Insert(CraftingBookCategory category) {
            super(category);
        }

        private static ItemStack[] pair(CraftingInput input) {
            List<Slot> f = filled(input);
            if (f.size() != 2) return null;
            ItemStack a = f.get(0).stack();
            ItemStack b = f.get(1).stack();
            if (a.getItem() instanceof FamilyArmorItem && isRune(b)) return new ItemStack[]{a, b};
            if (b.getItem() instanceof FamilyArmorItem && isRune(a)) return new ItemStack[]{b, a};
            return null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return !result(input).isEmpty();
        }

        private static ItemStack result(CraftingInput input) {
            ItemStack[] p = pair(input);
            if (p == null) return ItemStack.EMPTY;
            FamilyArmorItem armor = (FamilyArmorItem) p[0].getItem();
            RuneType type = ((RuneItem) p[1].getItem()).type();
            RuneDefinition def = RuneRegistry.def(type);
            if (def == null) return ItemStack.EMPTY;
            RuneSockets sockets = RuneRegistry.socketsOf(p[0]);
            if (RuneRules.canInsert(def, armor.part(), sockets.runes().size(), RuneRegistry.socketCount(p[0]))
                    != RuneRules.InsertResult.OK) {
                return ItemStack.EMPTY;
            }
            ItemStack out = p[0].copyWithCount(1);
            out.set(RuneRegistry.SOCKETS.get(), RuneSockets.fromRules(
                    RuneRules.insert(sockets.toRules(), new RuneRules.Socketed(type, RuneRegistry.gradeOf(p[1])))));
            return out;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return result(input);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RuneRegistry.INSERT.get();
        }
    }

    // ── 빼기 ──
    public static final class Extract extends CustomRecipe {
        public Extract(CraftingBookCategory category) {
            super(category);
        }

        /** [갑옷 칸, 추출 도구 칸] 또는 null */
        private static Slot[] pair(CraftingInput input) {
            List<Slot> f = filled(input);
            if (f.size() != 2) return null;
            Slot a = f.get(0);
            Slot b = f.get(1);
            if (a.stack().getItem() instanceof FamilyArmorItem && b.stack().is(RuneRegistry.EXTRACTOR.get())) return new Slot[]{a, b};
            if (b.stack().getItem() instanceof FamilyArmorItem && a.stack().is(RuneRegistry.EXTRACTOR.get())) return new Slot[]{b, a};
            return null;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            Slot[] p = pair(input);
            return p != null && !RuneRegistry.socketsOf(p[0].stack()).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            Slot[] p = pair(input);
            if (p == null) return ItemStack.EMPTY;
            RuneSockets sockets = RuneRegistry.socketsOf(p[0].stack());
            if (sockets.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = p[0].stack().copyWithCount(1);
            out.set(RuneRegistry.SOCKETS.get(), RuneSockets.fromRules(RuneRules.removeLast(sockets.toRules())));
            return out;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> rest = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            Slot[] p = pair(input);
            if (p == null) return rest;
            List<RuneRules.Socketed> list = RuneRegistry.socketsOf(p[0].stack()).toRules();
            if (list.isEmpty()) return rest;
            RuneRules.Socketed last = list.get(list.size() - 1);
            // 추출 도구는 소모되고, 그 칸에 뺀 룬석이 남는다
            rest.set(p[1].index(), RuneRegistry.runeStack(last.type(), last.grade()));
            return rest;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RuneRegistry.EXTRACT.get();
        }
    }

    // ── 합성 ──
    public static final class Combine extends CustomRecipe {
        public Combine(CraftingBookCategory category) {
            super(category);
        }

        private static ItemStack result(CraftingInput input) {
            List<Slot> f = filled(input);
            if (f.size() != RuneRules.COMBINE_COUNT) return ItemStack.EMPTY;
            ItemStack first = f.get(0).stack();
            if (!isRune(first)) return ItemStack.EMPTY;
            RuneType type = ((RuneItem) first.getItem()).type();
            int grade = RuneRegistry.gradeOf(first);
            for (Slot s : f) {
                if (!isRune(s.stack()) || ((RuneItem) s.stack().getItem()).type() != type
                        || RuneRegistry.gradeOf(s.stack()) != grade) {
                    return ItemStack.EMPTY;
                }
            }
            int next = RuneRules.combineResult(grade, f.size());
            return next == 0 ? ItemStack.EMPTY : RuneRegistry.runeStack(type, next);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return !result(input).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return result(input);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= RuneRules.COMBINE_COUNT;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RuneRegistry.COMBINE.get();
        }
    }

    // ── 연마 ──
    public static final class Polish extends CustomRecipe {
        public Polish(CraftingBookCategory category) {
            super(category);
        }

        private static ItemStack result(CraftingInput input) {
            List<Slot> f = filled(input);
            if (f.size() != 2) return ItemStack.EMPTY;
            ItemStack a = f.get(0).stack();
            ItemStack b = f.get(1).stack();
            ItemStack rune = isRune(a) && isAbrasive(b) ? a : isRune(b) && isAbrasive(a) ? b : null;
            ItemStack abrasive = rune == a ? b : a;
            if (rune == null) return ItemStack.EMPTY;
            int next = RuneRules.polishResult(RuneRegistry.gradeOf(rune), RuneRegistry.gradeOf(abrasive));
            return next == 0 ? ItemStack.EMPTY : RuneRegistry.runeStack(((RuneItem) rune.getItem()).type(), next);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return !result(input).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return result(input);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return RuneRegistry.POLISH.get();
        }
    }
}
