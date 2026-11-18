package com.fantasyrpg.core.client;

import com.fantasyrpg.core.skilltree.SkillNode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 왼쪽 아래 HUD: 직업·레벨, 경험치 막대, 코어 막대, 장착칸 5개(Z X C V B).
 * 직업이 없거나 F1(화면 숨기기) 상태면 그리지 않는다. 표시 위치·모양은 [초안].
 */
public final class RpgHud {
    private RpgHud() {
    }

    private static final int BAR_W = 90;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !ClientRpgState.received || ClientRpgState.rpgClass.isEmpty()) return;
        Font font = mc.font;
        int x = 4;
        int y = g.guiHeight() - 62;

        Component title = Component.translatable("class.fantasyrpg." + ClientRpgState.rpgClass)
                .append(" Lv." + ClientRpgState.level);
        g.drawString(font, title, x, y, 0xFFE8C66A);
        y += 11;

        // 경험치
        int need = ClientRpgState.need;
        float xpRatio = need <= 0 ? 1f : Math.min(1f, ClientRpgState.xp / (float) need);
        bar(g, x, y, xpRatio, 0xFF6ACB5A);
        y += 6;

        // 코어
        if (ClientRpgState.coreMax > 0) {
            bar(g, x, y, ClientRpgState.coreValue / ClientRpgState.coreMax, 0xFF5AA0E8);
            g.drawString(font, String.valueOf((int) ClientRpgState.coreValue), x + BAR_W + 3, y - 2, 0xFFB0D0FF);
            y += 6;
        }

        // 장착칸
        y += 2;
        String[] keys = {"Z", "X", "C", "V", "B"};
        for (int i = 0; i < 5; i++) {
            int sx = x + i * 22;
            String nodeId = i < ClientRpgState.slots.size() ? ClientRpgState.slots.get(i) : "";
            boolean ultimate = i == 4;
            g.fill(sx, y, sx + 20, y + 20, 0xA0000000);
            g.renderOutline(sx, y, 20, 20, ultimate ? 0xFFE8C66A : 0xFF808080);
            if (!nodeId.isEmpty()) {
                SkillNode n = ClientRpgState.nodes.get(nodeId);
                String label = n == null ? "?" : n.name().substring(0, Math.min(2, n.name().length()));
                double left = ClientRpgState.cooldownLeft(nodeId);
                if (left > 0) {
                    g.fill(sx + 1, y + 1, sx + 19, y + 19, 0xB0000000);
                    String t = left >= 1 ? String.valueOf((int) Math.ceil(left)) : String.format("%.1f", left);
                    g.drawCenteredString(font, t, sx + 10, y + 6, 0xFFFF8080);
                } else {
                    g.drawCenteredString(font, label, sx + 10, y + 6, 0xFFFFFFFF);
                }
            }
            g.drawString(font, shortKey(i, keys[i]), sx + 1, y + 21, 0xFFAAAAAA, true);
        }
    }

    private static String shortKey(int i, String fallback) {
        String s = FantasyRpgClient.SLOTS[i].getTranslatedKeyMessage().getString();
        return s.length() <= 2 ? s.toUpperCase(java.util.Locale.ROOT) : fallback;
    }

    private static void bar(GuiGraphics g, int x, int y, float ratio, int color) {
        g.fill(x, y, x + BAR_W, y + 4, 0xA0000000);
        g.fill(x, y, x + Math.round(BAR_W * Math.max(0f, Math.min(1f, ratio))), y + 4, color);
    }
}
