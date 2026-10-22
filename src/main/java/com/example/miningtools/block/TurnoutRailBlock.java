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

public class TurnoutRailBlock extends BaseRailBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CURVE_LEFT = BooleanProperty.create("curve_left");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_SOUTH, RailShape.EAST_WEST,
            RailShape.SOUTH_EAST, RailShape.SOUTH_WEST,
            RailShape.NORTH_WEST, RailShape.NORTH_EAST);

    public TurnoutRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CURVE_LEFT, false)
                .setValue(POWERED, false)
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    public static RailShape getStraightShape(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    public static RailShape getCurvedShape(Direction facing, boolean curveLeft) {
        return switch (facing) {
            case NORTH -> curveLeft ? RailShape.SOUTH_WEST : RailShape.SOUTH_EAST;
            case SOUTH -> curveLeft ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
            case EAST -> curveLeft ? RailShape.NORTH_WEST : RailShape.SOUTH_WEST;
            case WEST -> curveLeft ? RailShape.SOUTH_EAST : RailShape.NORTH_EAST;
            default -> RailShape.NORTH_SOUTH;
        };
    }

    public static RailShape calculateShape(Direction facing, boolean curveLeft, boolean powered) {
        return powered ? getCurvedShape(facing, curveLeft) : getStraightShape(facing);
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
        Boolean switchState = TrackSwitchBlock.getAdjacentSwitchState(context.getLevel(), context.getClickedPos());
        boolean powered = switchState != null ? switchState : context.getLevel().hasNeighborSignal(context.getClickedPos());
        boolean curveLeft = false;
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluid.getType() == Fluids.WATER;
        RailShape shape = calculateShape(facing, curveLeft, powered);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(CURVE_LEFT, curveLeft)
                .setValue(POWERED, powered)
                .setValue(SHAPE, shape)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        if (level.isClientSide()) {
            return;
        }
        Boolean switchState = TrackSwitchBlock.getAdjacentSwitchState(level, pos);
        boolean hasSignal = switchState != null ? switchState : level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            Direction facing = state.getValue(FACING);
            boolean curveLeft = state.getValue(CURVE_LEFT);
            RailShape newShape = calculateShape(facing, curveLeft, hasSignal);
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
        boolean curveLeft = state.getValue(CURVE_LEFT);
        boolean powered = state.getValue(POWERED);

        if (player.isShiftKeyDown()) {
            curveLeft = !curveLeft;
            RailShape newShape = calculateShape(facing, curveLeft, powered);
            level.setBlock(pos, state.setValue(CURVE_LEFT, curveLeft).setValue(SHAPE, newShape), 3);
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.sendOverlayMessage(Component.literal("Turnout Branch: " + (curveLeft ? "Left" : "Right")));
        } else {
            powered = !powered;
            RailShape newShape = calculateShape(facing, curveLeft, powered);
            level.setBlock(pos, state.setValue(POWERED, powered).setValue(SHAPE, newShape), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, powered ? 0.7F : 0.5F);
            player.sendOverlayMessage(Component.literal("Turnout Switch: " + (powered ? "Divergent (Curved)" : "Mainline (Straight)")));
        }
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
        boolean curveLeft = state.getValue(CURVE_LEFT);
        boolean powered = state.getValue(POWERED);
        return state.setValue(FACING, newFacing).setValue(SHAPE, calculateShape(newFacing, curveLeft, powered));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        Direction newFacing = mirror.mirror(facing);
        boolean curveLeft = !state.getValue(CURVE_LEFT);
        boolean powered = state.getValue(POWERED);
        return state.setValue(FACING, newFacing).setValue(CURVE_LEFT, curveLeft).setValue(SHAPE, calculateShape(newFacing, curveLeft, powered));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CURVE_LEFT, POWERED, SHAPE, WATERLOGGED);
    }
}
