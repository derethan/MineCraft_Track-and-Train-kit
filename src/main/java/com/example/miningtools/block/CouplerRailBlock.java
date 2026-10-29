package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CouplerRailBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Coupler Rail Block:
 * A toggleable specialty railway track designed for train consist management.
 * - Couple Mode (COUPLE_MODE = true): Automatically couples the nearest unlinked cart (or consist)
 *   within 16 blocks to an arriving locomotive.
 * - Decouple Mode (COUPLE_MODE = false): Automatically disconnects the locomotive from its closest
 *   linked trailing or pushed cart.
 * - Right-click to toggle mode manually; redstone power inverts/toggles mode for automated switching.
 * - Prevents repeated activations: when a locomotive stops or parks on the track, it only executes
 *   ONCE per trip until the locomotive leaves the track piece.
 */
public class CouplerRailBlock extends BaseRailBlock implements EntityBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty COUPLE_MODE = BooleanProperty.create("couple_mode");

    public CouplerRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(POWERED, false)
                .setValue(COUPLE_MODE, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hasSignal = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return super.getStateForPlacement(context)
                .setValue(POWERED, hasSignal)
                .setValue(COUPLE_MODE, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CouplerRailBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.COUPLER_RAIL_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> CouplerRailBlockEntity.serverTick(lvl, pos, st, (CouplerRailBlockEntity) be);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean currentMode = state.getValue(COUPLE_MODE);
            boolean newMode = !currentMode;
            level.setBlock(pos, state.setValue(COUPLE_MODE, newMode), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, newMode ? 1.2F : 0.8F);

            Component msg;
            if (newMode) {
                msg = Component.literal("[Coupler Rail] Mode: COUPLE (Links nearest cart within 16 blocks to locomotive)")
                        .withStyle(ChatFormatting.GREEN);
            } else {
                msg = Component.literal("[Coupler Rail] Mode: DECOUPLE (Disconnects locomotive from closest linked cart)")
                        .withStyle(ChatFormatting.GOLD);
            }

            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(msg, true);
            } else {
                player.sendSystemMessage(msg);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide()) {
            boolean shouldPower = level.hasNeighborSignal(pos);
            if (shouldPower != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, shouldPower), 3);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> switch (state.getValue(SHAPE)) {
                case ASCENDING_EAST -> state.setValue(SHAPE, RailShape.ASCENDING_WEST);
                case ASCENDING_WEST -> state.setValue(SHAPE, RailShape.ASCENDING_EAST);
                case ASCENDING_NORTH -> state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
                case ASCENDING_SOUTH -> state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
                default -> state;
            };
            case COUNTERCLOCKWISE_90 -> switch (state.getValue(SHAPE)) {
                case NORTH_SOUTH -> state.setValue(SHAPE, RailShape.EAST_WEST);
                case EAST_WEST -> state.setValue(SHAPE, RailShape.NORTH_SOUTH);
                case ASCENDING_EAST -> state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
                case ASCENDING_WEST -> state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
                case ASCENDING_NORTH -> state.setValue(SHAPE, RailShape.ASCENDING_WEST);
                case ASCENDING_SOUTH -> state.setValue(SHAPE, RailShape.ASCENDING_EAST);
                default -> state;
            };
            case CLOCKWISE_90 -> switch (state.getValue(SHAPE)) {
                case NORTH_SOUTH -> state.setValue(SHAPE, RailShape.EAST_WEST);
                case EAST_WEST -> state.setValue(SHAPE, RailShape.NORTH_SOUTH);
                case ASCENDING_EAST -> state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
                case ASCENDING_WEST -> state.setValue(SHAPE, RailShape.ASCENDING_EAST);
                case ASCENDING_NORTH -> state.setValue(SHAPE, RailShape.ASCENDING_EAST);
                case ASCENDING_SOUTH -> state.setValue(SHAPE, RailShape.ASCENDING_WEST);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        RailShape railShape = state.getValue(SHAPE);
        return switch (mirror) {
            case LEFT_RIGHT -> switch (railShape) {
                case ASCENDING_NORTH -> state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
                case ASCENDING_SOUTH -> state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
                default -> super.mirror(state, mirror);
            };
            case FRONT_BACK -> switch (railShape) {
                case ASCENDING_EAST -> state.setValue(SHAPE, RailShape.ASCENDING_WEST);
                case ASCENDING_WEST -> state.setValue(SHAPE, RailShape.ASCENDING_EAST);
                default -> super.mirror(state, mirror);
            };
            default -> super.mirror(state, mirror);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, COUPLE_MODE, WATERLOGGED);
    }
}
