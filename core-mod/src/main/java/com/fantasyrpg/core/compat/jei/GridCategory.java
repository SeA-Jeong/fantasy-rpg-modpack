package com.fantasyrpg.core.compat.jei;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.craft.CraftRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI 분류 "작업대 조합 (모양 없음)": 칸 배치는 아무렇게나 — 재료 개수만 맞으면 됨 */
public class GridCategory implements IRecipeCategory<GridCategory.Display> {
    /** @param table true 면 일반 제작대(1급 계열 갑옷 만들기) */
    public record Display(int grid, int grade, List<List<ItemStack>> inputs, ItemStack output, boolean table) {
    }

    public static final RecipeType<Display> TYPE = RecipeType.create(FantasyRpgCore.MOD_ID, "grid", Display.class);
    private static final int W = 150, H = 104, OUT_X = 122, OUT_Y = 36;

    private final IDrawable background, icon;

    public GridCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(W, H);
        this.icon = gui.createDrawableItemStack(new ItemStack(CraftRegistry.BENCH_5_ITEM.get()));
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.fantasyrpg.grid");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    // JEI 판에 따라 크기를 묻는 방법이 달라 둘 다 둠 (@Override 없이)
    public IDrawable getBackground() {
        return background;
    }

    public int getWidth() {
        return W;
    }

    public int getHeight() {
        return H;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display d, IFocusGroup focuses) {
        int g = d.grid();
        for (int i = 0; i < d.inputs().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % g) * 18, 1 + (i / g) * 18).addItemStacks(d.inputs().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(d.output());
    }

    @Override
    public void draw(Display d, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        int n = d.grid();
        for (int i = 0; i < n * n; i++) {
            int x = (i % n) * 18, y = (i / n) * 18;
            g.fill(x, y, x + 18, y + 18, 0xFF373737);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        }
        g.fill(OUT_X - 1, OUT_Y - 1, OUT_X + 17, OUT_Y + 17, 0xFF373737);
        g.fill(OUT_X, OUT_Y, OUT_X + 16, OUT_Y + 16, 0xFF8B8B8B);
        var font = Minecraft.getInstance().font;
        g.drawString(font, "→", OUT_X - 18, OUT_Y + 4, 0xFF404040, false);
        Component note = d.table() ? Component.translatable("jei.fantasyrpg.grid.note_table")
                : Component.translatable("jei.fantasyrpg.grid.note", n, n, d.grade());
        g.drawString(font, note, 0, H - 10, 0xFF404040, false);
    }
}
