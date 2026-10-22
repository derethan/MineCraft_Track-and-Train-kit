package com.example.miningtools.block;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity;
import com.example.miningtools.network.TrackSwitchSyncPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

public class TrackSwitchBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            Block.box(5.0, 3.0, 5.0, 11.0, 9.0, 11.0),
            Block.box(3.0, 9.0, 3.0, 13.0, 16.0, 13.0)
    );

    public TrackSwitchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(OPEN, false);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            applySwitchToAdjacentRails(level, pos, state.getValue(OPEN));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrackSwitchBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.TRACK_SWITCH_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> TrackSwitchBlockEntity.serverTick(lvl, pos, st, (TrackSwitchBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(OPEN) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(OPEN) ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TrackSwitchBlockEntity switchBe) {
                PacketDistributor.sendToPlayer(serverPlayer, new TrackSwitchSyncPayload(
                        pos,
                        state.getValue(OPEN),
                        switchBe.getDetectRange(),
                        List.copyOf(switchBe.getRules())
                ));
            }
            return InteractionResult.SUCCESS;
        }

        boolean nextOpen = !state.getValue(OPEN);
        setSwitchState(level, pos, state, nextOpen, true);
        Component msg = Component.literal("Track Switch: " + (nextOpen ? "OPEN (Switched)" : "CLOSED (Normal)"))
                .withStyle(nextOpen ? ChatFormatting.GREEN : ChatFormatting.RED);
        player.sendOverlayMessage(msg);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return this.useWithoutItem(state, level, pos, player, hitResult);
    }

    public static void setSwitchState(Level level, BlockPos pos, BlockState state, boolean open, boolean playSound) {
        if (!state.is(MiningTools.TRACK_SWITCH.get())) {
            return;
        }
        BlockState newState = state.setValue(OPEN, open);
        level.setBlock(pos, newState, 3);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.below(), state.getBlock());
        applySwitchToAdjacentRails(level, pos, open);
        if (playSound) {
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, open ? 0.75F : 0.55F);
        }
    }

    public static void applySwitchToAdjacentRails(Level level, BlockPos switchPos, boolean open) {
        // First pass: cardinal neighbors (horizontal + y-1..y+1)
        boolean switchedAnyCardinal = false;
        for (int dy = -1; dy <= 1; dy++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos targetPos = switchPos.offset(dir.getStepX(), dy, dir.getStepZ());
                if (applySwitchToSingleRail(level, switchPos, targetPos, open)) {
                    switchedAnyCardinal = true;
                }
            }
        }
        if (switchedAnyCardinal) {
            return;
        }

        // Second pass: diagonal neighbors if no cardinal rail was found
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    BlockPos targetPos = switchPos.offset(dx, dy, dz);
                    applySwitchToSingleRail(level, switchPos, targetPos, open);
                }
            }
        }
    }

    private static boolean applySwitchToSingleRail(Level level, BlockPos switchPos, BlockPos railPos, boolean open) {
        BlockState railState = level.getBlockState(railPos);
        if (!BaseRailBlock.isRail(railState)) {
            return false;
        }

        if (railState.getBlock() instanceof TurnoutRailBlock) {
            if (railState.getValue(TurnoutRailBlock.POWERED) != open) {
                Direction facing = railState.getValue(TurnoutRailBlock.FACING);
                boolean curveLeft = railState.getValue(TurnoutRailBlock.CURVE_LEFT);
                RailShape newShape = TurnoutRailBlock.calculateShape(facing, curveLeft, open);
                level.setBlock(railPos, railState.setValue(TurnoutRailBlock.POWERED, open).setValue(TurnoutRailBlock.SHAPE, newShape), 3);
            }
            return true;
        }

        if (railState.getBlock() instanceof WyeRailBlock) {
            if (railState.getValue(WyeRailBlock.POWERED) != open) {
                Direction facing = railState.getValue(WyeRailBlock.FACING);
                RailShape newShape = WyeRailBlock.calculateShape(facing, open);
                level.setBlock(railPos, railState.setValue(WyeRailBlock.POWERED, open).setValue(WyeRailBlock.SHAPE, newShape), 3);
            }
            return true;
        }

        if (railState.getBlock() instanceof PoweredRailBlock) {
            if (railState.getValue(PoweredRailBlock.POWERED) != open) {
                level.setBlock(railPos, railState.setValue(PoweredRailBlock.POWERED, open), 3);
            }
            return true;
        }

        level.neighborChanged(railPos, level.getBlockState(switchPos).getBlock(), null);
        return true;
    }

    public static @Nullable Boolean getAdjacentSwitchState(Level level, BlockPos railPos) {
        // Check cardinal neighbors first
        for (int dy = -1; dy <= 1; dy++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos checkPos = railPos.offset(dir.getStepX(), dy, dir.getStepZ());
                BlockState st = level.getBlockState(checkPos);
                if (st.is(MiningTools.TRACK_SWITCH.get())) {
                    return st.getValue(OPEN);
                }
            }
        }
        // Check diagonal neighbors
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    BlockPos checkPos = railPos.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(checkPos);
                    if (st.is(MiningTools.TRACK_SWITCH.get())) {
                        return st.getValue(OPEN);
                    }
                }
            }
        }
        return null;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }
}
