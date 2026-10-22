package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.ElectricRailBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;

public class ElectricRailBlock extends BaseRailBlock implements EntityBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ElectricRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricRailBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof ElectricRailBlockEntity railBe) {
                ElectricRailBlockEntity.serverTick(lvl, pos, st, railBe);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricRailBlockEntity railBe) {
                int stored = railBe.getEnergyStored();
                int cap = railBe.getCapacity();
                Component msg = Component.literal("⚡ Electric Rail: " + stored + " / " + cap + " FE")
                        .withStyle(stored > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY);
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(msg, true);
                } else {
                    player.sendSystemMessage(msg);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
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
                case ASCENDING_WEST -> state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
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
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }
}
