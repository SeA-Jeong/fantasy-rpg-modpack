package com.fantasyrpg.core.craft;

import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 4×4·5×5 작업대 화면. 모양 없는 조합(칸마다 1개씩 소비), 결과는 서버가 계산.
 * 조합식은 있지만 진행 등급이 모자라면 결과를 비우고 필요한 등급을 화면에 알림.
 */
public class GridCraftMenu extends AbstractContainerMenu {
    public final int size;
    private final CraftingContainer craft;
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    /** 0 = 문제없음, N = 진행 N급 필요 */
    private final DataSlot needGrade = DataSlot.standalone();
    /** 결과와 함께 줄 것 (분해, 서버만) */
    private List<ItemStack> extras = List.of();

    public static final int GRID_X = 8, GRID_Y = 18;

    public static int resultX(int size) {
        return GRID_X + 18 * size + 30;
    }

    public static int resultY(int size) {
        return GRID_Y + 9 * size - 8;
    }

    public static int invY(int size) {
        return GRID_Y + 18 * size + 14;
    }

    public static int height(int size) {
        return invY(size) + 82;
    }

    public GridCraftMenu(int size, int id, Inventory inv, ContainerLevelAccess access) {
        super(CraftRegistry.menuType(size), id);
        this.size = size;
        this.access = access;
        this.player = inv.player;
        this.craft = new TransientCraftingContainer(this, size, size);
        addSlot(new ResultSlot(resultX(size), resultY(size)));
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) addSlot(new Slot(craft, c + r * size, GRID_X + c * 18, GRID_Y + r * 18));
        int iy = invY(size);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, iy + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, iy + 58));
        addDataSlot(needGrade);
        access.execute(GridBenchBlock::opened);                          // 작업대 작동 모습 (2026-11-22)
    }

    public int needGrade() {
        return needGrade.get();
    }

    private int gridEnd() {
        return 1 + size * size;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != craft || !(player instanceof ServerPlayer sp)) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < craft.getContainerSize(); i++) items.add(craft.getItem(i));
        GridCrafting.Result r = GridCrafting.find(size, items);
        ItemStack out = r.out();
        if (!out.isEmpty() && MagicCityApi.grade(sp) < r.needGrade()) {
            needGrade.set(r.needGrade());
            out = ItemStack.EMPTY;
            extras = List.of();
        } else {
            needGrade.set(0);
            extras = r.extras();
        }
        result.setItem(0, out);
        setRemoteSlot(0, out);
        sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, out));
    }

    /** 결과를 가져가면 칸마다 1개씩 소비 */
    private class ResultSlot extends Slot {
        ResultSlot(int x, int y) {
            super(result, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            stack.onCraftedBy(p.level(), p, stack.getCount());
            for (ItemStack ex : extras) {                                                      // 분해: 나머지 재료 (2026-11-21)
                ItemStack give = ex.copy();
                if (!p.getInventory().add(give)) p.drop(give, false);
            }
            extras = List.of();
            for (int i = 0; i < craft.getContainerSize(); i++) {
                if (!craft.getItem(i).isEmpty()) craft.removeItem(i, 1);
            }
            slotsChanged(craft);
            super.onTake(p, stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack st = slot.getItem();
            copy = st.copy();
            int gridEnd = gridEnd(), invEnd = gridEnd + 27, hotEnd = invEnd + 9;
            if (index == 0) {
                if (!moveItemStackTo(st, gridEnd, hotEnd, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(st, copy);
            } else if (index >= gridEnd) {
                if (!moveItemStackTo(st, 1, gridEnd, false)) {
                    if (index < invEnd) {
                        if (!moveItemStackTo(st, invEnd, hotEnd, false)) return ItemStack.EMPTY;
                    } else if (!moveItemStackTo(st, gridEnd, invEnd, false)) return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(st, gridEnd, hotEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, st);
            if (index == 0) p.drop(st, false);
        }
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, craft));
        access.execute(GridBenchBlock::closed);
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, size >= 5 ? CraftRegistry.BENCH_5.get() : CraftRegistry.BENCH_4.get());
    }
}
