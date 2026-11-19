package com.fantasyrpg.core.client;

import com.fantasyrpg.core.network.RpgNetwork;
import com.fantasyrpg.core.playerdata.RpgClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 직업 선택 화면 (서장 NPC). 6직업 카드: 이름·주력 무기·코어·한 줄 설명·추천 플레이.
 * 카드를 누르고 "이 길로 정한다" → 확인 창 → 서버에 선택 요청. 판정(무직인지, NPC 가 화면을 열어 줬는지)은 서버.
 * 글은 언어 파일 {@code class.fantasyrpg.<직업>.weapon|core|desc|play}.
 */
public final class ClassSelectScreen extends Screen {
    private static final int GAP = 6, CARD_H = 84;
    private RpgClass selected;
    private Button confirm;

    public ClassSelectScreen() {
        super(Component.translatable("screen.fantasyrpg.class_select"));
    }

    /** 서버 패킷으로 열기 */
    public static void open() {
        Minecraft.getInstance().setScreen(new ClassSelectScreen());
    }

    private int cardW() {
        return Math.min(140, (width - 20 - GAP * 2) / 3);
    }

    private int left() {
        return (width - (cardW() * 3 + GAP * 2)) / 2;
    }

    private int top() {
        return Math.max(26, (height - (CARD_H * 2 + GAP + 60)) / 2 + 20);
    }

    @Override
    protected void init() {
        int y = top() + CARD_H * 2 + GAP + 8;
        confirm = addRenderableWidget(Button.builder(Component.translatable("screen.fantasyrpg.class_select.confirm"), b -> askConfirm())
                .bounds(width / 2 - 124, y, 120, 20).build());
        confirm.active = selected != null;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 + 4, y, 120, 20).build());
    }

    private void askConfirm() {
        if (selected == null) return;
        RpgClass c = selected;
        Component name = Component.translatable("class.fantasyrpg." + c.id());
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                PacketDistributor.sendToServer(new RpgNetwork.ChooseClass(c.id()));
                minecraft.setScreen(null);
            } else {
                minecraft.setScreen(this);
            }
        }, Component.translatable("screen.fantasyrpg.class_select.ask", name),
                Component.translatable("screen.fantasyrpg.class_select.warn")));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            RpgClass hit = cardAt(mx, my);
            if (hit != null) {
                selected = hit;
                confirm.active = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private RpgClass cardAt(double mx, double my) {
        RpgClass[] all = RpgClass.values();
        for (int i = 0; i < all.length; i++) {
            int x = left() + (i % 3) * (cardW() + GAP), y = top() + (i / 3) * (CARD_H + GAP);
            if (mx >= x && mx < x + cardW() && my >= y && my < y + CARD_H) return all[i];
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, top() - 18, 0xFFFFD27F);
        RpgClass[] all = RpgClass.values();
        RpgClass hover = cardAt(mx, my);
        int w = cardW();
        for (int i = 0; i < all.length; i++) {
            RpgClass c = all[i];
            int x = left() + (i % 3) * (w + GAP), y = top() + (i / 3) * (CARD_H + GAP);
            boolean sel = c == selected;
            g.fill(x, y, x + w, y + CARD_H, sel ? 0xE0303A20 : c == hover ? 0xD0202830 : 0xC0101010);
            g.renderOutline(x, y, w, CARD_H, sel ? 0xFFFFD27F : 0xFF505050);
            String k = "class.fantasyrpg." + c.id();
            g.drawString(font, Component.translatable(k), x + 5, y + 5, sel ? 0xFFFFD27F : 0xFFFFFFFF);
            g.drawString(font, Component.translatable("screen.fantasyrpg.class_select.gear",
                    Component.translatable(k + ".weapon"), Component.translatable(k + ".core")), x + 5, y + 17, 0xFF9FC8FF, false);
            int ly = y + 30;
            ly = wrap(g, Component.translatable(k + ".desc"), x + 5, ly, w - 10, 0xFFDDDDDD, 3);
            wrap(g, Component.translatable(k + ".play"), x + 5, ly + 2, w - 10, 0xFF909090, 2);
        }
        if (selected == null) {
            g.drawCenteredString(font, Component.translatable("screen.fantasyrpg.class_select.help"), width / 2, top() + CARD_H * 2 + GAP + 32, 0xFF909090);
        }
    }

    private int wrap(GuiGraphics g, Component text, int x, int y, int w, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, w);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
