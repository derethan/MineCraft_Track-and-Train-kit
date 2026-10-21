package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class WyeRailBlock extends BaseRailBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.SOUTH_EAST, RailShape.SOUTH_WEST,
            RailShape.NORTH_WEST, RailShape.NORTH_EAST);

    public WyeRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(SHAPE, RailShape.SOUTH_WEST)
                .setValue(WATERLOGGED, false));
    }

    public static RailShape getLeftBranch(Direction facing) {
        return switch (facing) {
            case NORTH -> RailShape.SOUTH_WEST;
            case SOUTH -> RailShape.NORTH_EAST;
            case EAST -> RailShape.NORTH_WEST;
            case WEST -> RailShape.SOUTH_EAST;
            default -> RailShape.SOUTH_WEST;
        };
    }

    public static RailShape getRightBranch(Direction facing) {
        return switch (facing) {
            case NORTH -> RailShape.SOUTH_EAST;
            case SOUTH -> RailShape.NORTH_WEST;
            case EAST -> RailShape.SOUTH_WEST;
            case WEST -> RailShape.NORTH_EAST;
            default -> RailShape.SOUTH_EAST;
        };
    }

    public static RailShape calculateShape(Direction facing, boolean powered) {
        return powered ? getRightBranch(facing) : getLeftBranch(facing);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isFlexibleRail(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        return state.getValue(SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluid.getType() == Fluids.WATER;
        RailShape shape = calculateShape(facing, powered);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, powered)
                .setValue(SHAPE, shape)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        if (level.isClientSide()) {
            return;
        }
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            Direction facing = state.getValue(FACING);
            RailShape newShape = calculateShape(facing, hasSignal);
            BlockState newState = state.setValue(POWERED, hasSignal).setValue(SHAPE, newShape);
            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, hasSignal ? 0.7F : 0.5F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = state.getValue(FACING);
        boolean powered = !state.getValue(POWERED);
        RailShape newShape = calculateShape(facing, powered);
        level.setBlock(pos, state.setValue(POWERED, powered).setValue(SHAPE, newShape), 3);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, powered ? 0.7F : 0.5F);
        player.sendOverlayMessage(Component.literal("Wye Switch: " + (powered ? "Right Branch" : "Left Branch")));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return this.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(FACING);
        Direction newFacing = rotation.rotate(facing);
        boolean powered = state.getValue(POWERED);
        return state.setValue(FACING, newFacing).setValue(SHAPE, calculateShape(newFacing, powered));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        Direction newFacing = mirror.mirror(facing);
        boolean powered = !state.getValue(POWERED); // mirror flips left/right branches!
        return state.setValue(FACING, newFacing).setValue(POWERED, powered).setValue(SHAPE, calculateShape(newFacing, powered));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SHAPE, WATERLOGGED);
    }
}
