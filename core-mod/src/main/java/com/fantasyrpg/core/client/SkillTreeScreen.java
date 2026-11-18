package com.fantasyrpg.core.client;

import com.fantasyrpg.core.skilltree.PassiveNode;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.fantasyrpg.core.skilltree.SkillService.ActionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스킬트리 화면 (K). 탭 2개: 직업 트리 / 공통 패시브.
 * <ul>
 *   <li>직업 트리: 방향 4열 × 단계 6줄. 왼쪽 클릭 투자, Shift+왼쪽 클릭 1랭크 되돌리기,
 *       오른쪽 클릭으로 노드를 집은 뒤 아래 장착칸 클릭으로 장착, 장착칸 오른쪽 클릭으로 해제</li>
 *   <li>공통 패시브: 줄마다 왼쪽 클릭 투자, Shift+왼쪽 클릭 되돌리기</li>
 * </ul>
 * 화면은 요청만 보내고 결과는 서버 동기화로 다시 그린다. 모양·배치는 [초안].
 */
public final class SkillTreeScreen extends Screen {
    private static final int CELL_W = 92;
    private static final int CELL_H = 26;
    private static final int NODE_W = 42;
    private static final int NODE_H = 20;
    private static final SkillNode.Direction[] COLUMNS = SkillNode.Direction.values();

    private boolean passiveTab;
    private String held = "";
    private int left;
    private int top;
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x, int y, int w, int h, String kind, String id, int slot) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public SkillTreeScreen() {
        super(Component.translatable("screen.fantasyrpg.skill_tree"));
    }

    @Override
    protected void init() {
        left = (width - CELL_W * COLUMNS.length) / 2;
        top = 40;
        addRenderableWidget(Button.builder(Component.translatable("screen.fantasyrpg.tab.class"), b -> passiveTab = false)
                .bounds(left, 6, 80, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fantasyrpg.tab.passive"), b -> passiveTab = true)
                .bounds(left + 84, 6, 80, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fantasyrpg.full_reset"),
                        b -> FantasyRpgClient.send(ActionType.FULL_RESET, "", 0))
                .bounds(left + CELL_W * COLUMNS.length - 90, 6, 90, 16).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        hits.clear();
        if (!ClientRpgState.received || ClientRpgState.rpgClass.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.fantasyrpg.no_class"), width / 2, height / 2, 0xFFFF8080);
            return;
        }
        List<Component> tooltip = passiveTab ? renderPassives(g, mx, my) : renderTree(g, mx, my);
        if (tooltip != null && !tooltip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component c : tooltip) lines.add(c.getVisualOrderText());
            g.renderTooltip(font, lines, mx, my);
        }
    }

    private List<Component> renderTree(GuiGraphics g, int mx, int my) {
        g.drawString(font, Component.translatable("screen.fantasyrpg.sp",
                ClientRpgState.spTotal - ClientRpgState.spSpent, ClientRpgState.spTotal, ClientRpgState.freeResets), left, 26, 0xFFFFFFFF);

        for (int c = 0; c < COLUMNS.length; c++) {
            g.drawCenteredString(font, Component.translatable("direction.fantasyrpg." + COLUMNS[c].name().toLowerCase(java.util.Locale.ROOT)),
                    left + c * CELL_W + CELL_W / 2, top - 2, 0xFFE8C66A);
        }
        // 칸마다 노드 모으기 (혼합 노드는 첫 방향 열)
        Map<Long, List<SkillNode>> cells = new HashMap<>();
        for (SkillNode n : ClientRpgState.nodes.values()) {
            int col = n.directions().iterator().next().ordinal();
            cells.computeIfAbsent(((long) col << 8) | n.tier(), k -> new ArrayList<>()).add(n);
        }
        List<Component> tip = null;
        for (int t = 1; t <= 6; t++) {
            int rowY = top + 8 + (t - 1) * CELL_H;
            g.drawString(font, t + "", left - 10, rowY + 6, 0xFF808080);
            for (int c = 0; c < COLUMNS.length; c++) {
                List<SkillNode> list = cells.getOrDefault(((long) c << 8) | t, List.of());
                for (int i = 0; i < list.size() && i < 2; i++) {
                    SkillNode n = list.get(i);
                    int x = left + c * CELL_W + 3 + i * (NODE_W + 4);
                    drawNode(g, n, x, rowY);
                    Hit h = new Hit(x, rowY, NODE_W, NODE_H, "node", n.id(), -1);
                    hits.add(h);
                    if (h.contains(mx, my)) tip = nodeTooltip(n);
                }
            }
        }
        // 장착칸
        int slotY = top + 8 + 6 * CELL_H + 8;
        g.drawString(font, Component.translatable(held.isEmpty() ? "screen.fantasyrpg.slots" : "screen.fantasyrpg.slots_held",
                held), left, slotY, 0xFFFFFFFF);
        String[] keys = {"Z", "X", "C", "V", "B"};
        for (int i = 0; i < 5; i++) {
            int x = left + i * 50;
            int y = slotY + 11;
            String id = ClientRpgState.slots.get(i);
            g.fill(x, y, x + 46, y + 20, 0xA0000000);
            g.renderOutline(x, y, 46, 20, i == 4 ? 0xFFE8C66A : 0xFF808080);
            SkillNode n = ClientRpgState.nodes.get(id);
            g.drawCenteredString(font, keys[i] + (n == null ? "" : " " + shortName(n.name(), 4)), x + 23, y + 6, 0xFFFFFFFF);
            Hit h = new Hit(x, y, 46, 20, "slot", id, i);
            hits.add(h);
            if (h.contains(mx, my) && n != null) tip = nodeTooltip(n);
        }
        g.drawString(font, Component.translatable("screen.fantasyrpg.help"), left, slotY + 36, 0xFF909090);
        return tip;
    }

    private void drawNode(GuiGraphics g, SkillNode n, int x, int y) {
        int rank = ClientRpgState.ranks.getOrDefault(n.id(), 0);
        boolean locked = ClientRpgState.level < n.requiredLevel();
        int border = n.id().equals(held) ? 0xFF55FF55
                : n.kind() == SkillNode.Kind.ULTIMATE ? 0xFFE8C66A
                : n.directions().size() > 1 ? 0xFFC080FF
                : rank > 0 ? 0xFFFFFFFF : 0xFF606060;
        g.fill(x, y, x + NODE_W, y + NODE_H, locked ? 0xC0200000 : rank > 0 ? 0xC0203040 : 0xC0101010);
        g.renderOutline(x, y, NODE_W, NODE_H, border);
        g.drawString(font, shortName(n.name(), 3), x + 2, y + 2, locked ? 0xFF808080 : 0xFFFFFFFF, false);
        g.drawString(font, rank + "/" + n.maxRank(), x + 2, y + 11, 0xFFAAAAAA, false);
    }

    private static String shortName(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    private List<Component> nodeTooltip(SkillNode n) {
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(n.name()).withStyle(net.minecraft.ChatFormatting.GOLD));
        t.add(Component.translatable("screen.fantasyrpg.node.kind." + n.kind().name().toLowerCase(java.util.Locale.ROOT))
                .append(" · " + n.tier()).append(Component.translatable("screen.fantasyrpg.node.tier")));
        t.add(Component.translatable("screen.fantasyrpg.node.rank", ClientRpgState.ranks.getOrDefault(n.id(), 0), n.maxRank()));
        t.add(Component.translatable("screen.fantasyrpg.node.level", n.requiredLevel()));
        if (n.requiredDirectionSp() > 0) {
            t.add(Component.translatable(n.directions().size() > 1 ? "screen.fantasyrpg.node.mixed_sp" : "screen.fantasyrpg.node.dir_sp",
                    n.requiredDirectionSp()));
        }
        if (SkillNodeEffectHint.isEmpty(n)) t.add(Component.translatable("screen.fantasyrpg.node.no_effect").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        return t;
    }

    private List<Component> renderPassives(GuiGraphics g, int mx, int my) {
        g.drawString(font, Component.translatable("screen.fantasyrpg.passive_points",
                ClientRpgState.passivePoints - ClientRpgState.passiveSpent, ClientRpgState.passivePoints), left, 26, 0xFFFFFFFF);
        int y = top + 4;
        List<Component> tip = null;
        for (PassiveNode n : ClientRpgState.passives.values()) {
            int rank = ClientRpgState.passiveRanks.getOrDefault(n.id(), 0);
            int w = CELL_W * COLUMNS.length;
            g.fill(left, y, left + w, y + 18, rank > 0 ? 0xC0203040 : 0xC0101010);
            g.renderOutline(left, y, w, 18, 0xFF606060);
            g.drawString(font, n.name(), left + 4, y + 5, 0xFFFFFFFF, false);
            g.drawString(font, rank + "/" + n.maxRank(), left + w - 30, y + 5, 0xFFAAAAAA, false);
            Hit h = new Hit(left, y, w, 18, "passive", n.id(), -1);
            hits.add(h);
            if (h.contains(mx, my)) {
                tip = List.of(Component.literal(n.name()),
                        Component.translatable("screen.fantasyrpg.passive.effect", n.attribute(), n.perRank()),
                        Component.translatable("screen.fantasyrpg.passive.grade", n.requiredGrade()));
            }
            y += 21;
        }
        if (ClientRpgState.passives.isEmpty()) {
            g.drawString(font, Component.translatable("screen.fantasyrpg.passive_none"), left, y, 0xFF909090);
        }
        g.drawString(font, Component.translatable("screen.fantasyrpg.passive_help"), left, y + 14, 0xFF909090);
        return tip;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (!h.contains(mx, my)) continue;
            boolean shift = hasShiftDown();
            switch (h.kind()) {
                case "node" -> {
                    if (button == 0) FantasyRpgClient.send(shift ? ActionType.REFUND : ActionType.INVEST, h.id(), 0);
                    else if (button == 1) held = held.equals(h.id()) ? "" : h.id();
                }
                case "slot" -> {
                    if (button == 0 && !held.isEmpty()) {
                        FantasyRpgClient.send(ActionType.EQUIP, held, h.slot());
                        held = "";
                    } else if (button == 1) {
                        FantasyRpgClient.send(ActionType.UNEQUIP, "", h.slot());
                    }
                }
                case "passive" -> {
                    if (button == 0) FantasyRpgClient.send(shift ? ActionType.PASSIVE_REFUND : ActionType.PASSIVE_INVEST, h.id(), 0);
                }
                default -> {
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (FantasyRpgClient.OPEN_TREE.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** 효과 등록 여부는 서버만 안다. 클라이언트는 데이터의 effect 칸이 비었는지만 본다 */
    private static final class SkillNodeEffectHint {
        static boolean isEmpty(SkillNode n) {
            return n.effect() == null || n.effect().isEmpty();
        }
    }
}
