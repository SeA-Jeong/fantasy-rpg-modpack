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
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 숙련(4×4)·장인(5×5) 작업대. 쓸 수 있는 조건: 작업대 단계(메인 챕터 2개·6개 완료, MagicCityApi.craftGrid) */
public class GridBenchBlock extends Block {
    /** 앞면 방향 (2026-11-21, 작업대 그림 v0.6): 놓을 때 플레이어를 바라봄. 이전 월드의 작업대는 북쪽 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final int size;

    public GridBenchBlock(int size, Properties props) {
        super(props);
        this.size = size;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
