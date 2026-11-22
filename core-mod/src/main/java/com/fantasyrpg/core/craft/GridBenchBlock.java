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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.util.RandomSource;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.phys.BlockHitResult;

/** 숙련(4×4)·장인(5×5) 작업대. 쓸 수 있는 조건: 작업대 단계(메인 챕터 2개·6개 완료, MagicCityApi.craftGrid) */
public class GridBenchBlock extends Block {
    /** 앞면 방향 (2026-11-21, 작업대 그림 v0.6): 놓을 때 플레이어를 바라봄. 이전 월드의 작업대는 북쪽 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 작동 중 (2026-11-22): 누군가 이 작업대 화면을 열고 있으면 true → 작동 모델·빛 7·입자 */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final int size;
    /** 서버: 작업대 자리마다 열린 화면 수 */
    private static final Map<Level, Map<BlockPos, Integer>> OPEN = new WeakHashMap<>();

    public GridBenchBlock(int size, Properties props) {
        super(props);
        this.size = size;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    /** 화면을 열 때(서버) */
    public static void opened(Level level, BlockPos pos) {
        OPEN.computeIfAbsent(level, l -> new HashMap<>()).merge(pos.immutable(), 1, Integer::sum);
        setActive(level, pos, true);
    }

    /** 화면을 닫을 때(서버) — 마지막 사람이 닫으면 꺼짐 */
    public static void closed(Level level, BlockPos pos) {
        Map<BlockPos, Integer> m = OPEN.get(level);
        int left = m == null ? 0 : m.merge(pos.immutable(), -1, Integer::sum);
        if (m != null && left <= 0) m.remove(pos);
        if (left <= 0) setActive(level, pos, false);
    }

    private static void setActive(Level level, BlockPos pos, boolean on) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof GridBenchBlock && s.getValue(ACTIVE) != on) level.setBlock(pos, s.setValue(ACTIVE, on), 3);
    }

    /** 작동 중일 때 윗면에 룬 먼지·불꽃 (클라이언트, 사용자 제공 그림 v0.6) */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(ACTIVE)) return;
        var dust = size >= 5 ? com.fantasyrpg.core.fx.RpgParticles.BENCH5_DUST.get() : com.fantasyrpg.core.fx.RpgParticles.BENCH4_DUST.get();
        var spark = size >= 5 ? com.fantasyrpg.core.fx.RpgParticles.BENCH5_SPARK.get() : com.fantasyrpg.core.fx.RpgParticles.BENCH4_SPARK.get();
        for (int i = 0; i < 3; i++) {
            level.addParticle(i == 0 ? spark : dust, pos.getX() + 0.15 + r.nextDouble() * 0.7, pos.getY() + 1.02, pos.getZ() + 0.15 + r.nextDouble() * 0.7,
                    0, 0.015 + r.nextDouble() * 0.02, 0);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(ACTIVE, false);
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
