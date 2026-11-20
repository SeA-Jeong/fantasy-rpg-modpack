package com.fantasyrpg.core.craft;

import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 숙련(4×4)·장인(5×5) 작업대. 쓸 수 있는 조건: 작업대 단계(메인 챕터 2개·6개 완료, MagicCityApi.craftGrid) */
public class GridBenchBlock extends Block {
    private final int size;

    public GridBenchBlock(int size, Properties props) {
        super(props);
        this.size = size;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (MagicCityApi.craftGrid(sp) < size) {
                sp.displayClientMessage(Component.translatable("message.fantasyrpg.bench.locked", size, size == 4 ? 2 : 6).withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new GridCraftMenu(size, id, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.fantasyrpg.workbench_" + size + "x" + size)));
        }
        return InteractionResult.CONSUME;
    }
}
