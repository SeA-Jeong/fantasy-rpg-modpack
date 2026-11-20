package com.fantasyrpg.core.client;

import com.fantasyrpg.core.craft.GridCraftMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 4×4·5×5 작업대 화면 (그림 파일 없이 칸을 직접 그림) */
public class GridCraftScreen extends AbstractContainerScreen<GridCraftMenu> {
    public GridCraftScreen(GridCraftMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = GridCraftMenu.height(menu.size);
        this.inventoryLabelY = GridCraftMenu.invY(menu.size) - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        for (Slot s : menu.slots) {
            int sx = x + s.x, sy = y + s.y;
            boolean result = s.index == 0;
            int pad = result ? 4 : 0;
            g.fill(sx - 1 - pad, sy - 1 - pad, sx + 17 + pad, sy + 17 + pad, 0xFF373737);
            g.fill(sx - pad, sy - pad, sx + 17 + pad, sy + 17 + pad, 0xFFFFFFFF);
            g.fill(sx - pad, sy - pad, sx + 16 + pad, sy + 16 + pad, 0xFF8B8B8B);
        }
        int ax = x + GridCraftMenu.resultX(menu.size) - 24, ay = y + GridCraftMenu.resultY(menu.size) + 4;
        g.drawString(font, "→", ax, ay, 0xFF404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int need = menu.needGrade();
        if (need > 0) {
            Component msg = Component.translatable("screen.fantasyrpg.bench.need_grade", need);
            int rx = GridCraftMenu.resultX(menu.size) + 8 - font.width(msg) / 2;
            g.drawString(font, msg, Math.max(4, Math.min(rx, imageWidth - 4 - font.width(msg))), GridCraftMenu.resultY(menu.size) + 26, 0xFFAA0000, false);
        }
    }
}
