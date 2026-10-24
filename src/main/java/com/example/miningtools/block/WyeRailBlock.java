package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

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
import net.minecraft.world.level.block.RailState;
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
import net.minecraft.world.phys.Vec3;

public class WyeRailBlock extends BaseRailBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_SOUTH, RailShape.EAST_WEST,
            RailShape.SOUTH_EAST, RailShape.SOUTH_WEST,
            RailShape.NORTH_WEST, RailShape.NORTH_EAST);

    private static final ThreadLocal<Boolean> ALIGNING = ThreadLocal.withInitial(() -> false);

    public WyeRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(SHAPE, calculateShape(Direction.NORTH, false))
                .setValue(WATERLOGGED, false));
    }

    public static Direction getStemDirection(Direction facing) {
        return facing.getOpposite();
    }

    public static RailShape getStraightShape(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    public static RailShape getStemStraightShape(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
    }

    public static RailShape getLeftBranch(Direction facing) {
        return switch (facing) {
            case NORTH -> RailShape.SOUTH_WEST;
            case EAST -> RailShape.NORTH_WEST;
            case SOUTH -> RailShape.NORTH_EAST;
            case WEST -> RailShape.SOUTH_EAST;
            default -> RailShape.SOUTH_WEST;
        };
    }

    public static RailShape getRightBranch(Direction facing) {
        return switch (facing) {
            case NORTH -> RailShape.SOUTH_EAST;
            case EAST -> RailShape.SOUTH_WEST;
            case SOUTH -> RailShape.NORTH_WEST;
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
    public boolean isValidRailShape(RailShape shape) {
        return shape == RailShape.NORTH_SOUTH || shape == RailShape.EAST_WEST
                || shape == RailShape.SOUTH_EAST || shape == RailShape.SOUTH_WEST
                || shape == RailShape.NORTH_WEST || shape == RailShape.NORTH_EAST;
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        if (cart != null) {
            Direction facing = state.getValue(FACING);
            boolean powered = state.getValue(POWERED);
            Direction stemDir = getStemDirection(facing);
            Direction leftDir = facing.getCounterClockWise();
            Direction rightDir = facing.getClockWise();

            RailShape straightShape = getStraightShape(facing);
            RailShape leftBranch = getLeftBranch(facing);
            RailShape rightBranch = getRightBranch(facing);
            RailShape activeBranch = powered ? rightBranch : leftBranch;

            // Compute relative position from block center
            double relX = cart.getX() - (pos.getX() + 0.5);
            double relZ = cart.getZ() - (pos.getZ() + 0.5);
            double offsetStem = relX * stemDir.getStepX() + relZ * stemDir.getStepZ();
            double offsetLeft = relX * leftDir.getStepX() + relZ * leftDir.getStepZ();
            double offsetRight = relX * rightDir.getStepX() + relZ * rightDir.getStepZ();

            Vec3 movement = cart.getDeltaMovement();
            double velStem = stemDir.getStepX() * movement.x + stemDir.getStepZ() * movement.z;
            double velLeft = leftDir.getStepX() * movement.x + leftDir.getStepZ() * movement.z;
            double velRight = rightDir.getStepX() * movement.x + rightDir.getStepZ() * movement.z;

            // 1. If cart is on the stem side (heading in or riding the curved branch towards stem),
            // or moving towards the junction from the stem (velStem < -1.0E-4):
            if (offsetStem > 0.05 || velStem < -1.0E-4) {
                return activeBranch;
            }

            // 2. Moving along mainline towards rightDir (entered from leftDir side):
            if (velRight > 1.0E-4 || (Math.abs(velLeft) <= 1.0E-4 && offsetLeft > 0.05)) {
                // If switch is set to LEFT (!powered), cart matches active direction and follows curve to stem (south)
                // If switch is set to RIGHT (powered), cart does not match and continues straight through
                return (!powered) ? leftBranch : straightShape;
            }

            // 3. Moving along mainline towards leftDir (entered from rightDir side):
            if (velLeft > 1.0E-4 || (Math.abs(velRight) <= 1.0E-4 && offsetRight > 0.05)) {
                // If switch is set to RIGHT (powered), cart matches active direction and follows curve to stem (south)
                // If switch is set to LEFT (!powered), cart does not match and continues straight through
                return powered ? rightBranch : straightShape;
            }

            // 4. Stationary fallback:
            if (cart instanceof BasicLocomotiveEntity loco) {
                Direction travel = loco.getTravelDirection();
                if (travel == leftDir) {
                    return powered ? rightBranch : straightShape;
                } else if (travel == rightDir) {
                    return (!powered) ? leftBranch : straightShape;
                }
                return activeBranch;
            }
            if (cart instanceof DieselLocomotiveEntity dloco) {
                Direction travel = dloco.getTravelDirection();
                if (travel == leftDir) {
                    return powered ? rightBranch : straightShape;
                } else if (travel == rightDir) {
                    return (!powered) ? leftBranch : straightShape;
                }
                return activeBranch;
            }
            if (cart instanceof ElectricLocomotiveEntity eloco) {
                Direction travel = eloco.getTravelDirection();
                if (travel == leftDir) {
                    return powered ? rightBranch : straightShape;
                } else if (travel == rightDir) {
                    return (!powered) ? leftBranch : straightShape;
                }
                return activeBranch;
            }

            return activeBranch;
        }
        return state.getValue(SHAPE);
    }

    @Override
    protected BlockState updateDir(Level level, BlockPos pos, BlockState state, boolean first) {
        if (level.isClientSide()) {
            return state;
        }
        return alignTNeighbors(level, pos, state, first);
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        if (!level.isClientSide()) {
            alignTNeighbors(level, pos, state, false);
        }
    }

    private BlockState alignTNeighbors(Level level, BlockPos pos, BlockState state, boolean force) {
        if (ALIGNING.get()) {
            return state;
        }
        try {
            ALIGNING.set(true);
            Direction facing = state.getValue(FACING);
            Direction stemDir = getStemDirection(facing);
            Direction leftDir = facing.getCounterClockWise();
            Direction rightDir = facing.getClockWise();
            RailShape mainlineShape = getStraightShape(facing);
            RailShape stemShape = getStemStraightShape(facing);

            // 1. Align left and right mainline neighbors
            BlockState mainlineState = state.setValue(SHAPE, mainlineShape);
            level.setBlock(pos, mainlineState, Block.UPDATE_CLIENTS);
            alignNeighborOnAxis(level, pos.relative(leftDir), mainlineShape, force);
            alignNeighborOnAxis(level, pos.relative(rightDir), mainlineShape, force);

            // 2. Align stem neighbor
            BlockState stemState = state.setValue(SHAPE, stemShape);
            level.setBlock(pos, stemState, Block.UPDATE_CLIENTS);
            alignNeighborOnAxis(level, pos.relative(stemDir), stemShape, force);

            // Restore initial state
            RailShape actualShape = calculateShape(facing, state.getValue(POWERED));
            BlockState restored = state.setValue(SHAPE, actualShape);
            level.setBlock(pos, restored, Block.UPDATE_CLIENTS);
            return restored;
        } finally {
            ALIGNING.set(false);
        }
    }

    private void alignNeighborOnAxis(Level level, BlockPos neighborPos, RailShape targetShape, boolean force) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!BaseRailBlock.isRail(neighborState)
                || neighborState.getBlock() instanceof JunctionRailBlock
                || neighborState.getBlock() instanceof WyeRailBlock
                || neighborState.getBlock() instanceof TurnoutRailBlock) {
            return;
        }
        BaseRailBlock railBlock = (BaseRailBlock) neighborState.getBlock();
        RailShape currentShape = railBlock.getRailDirection(neighborState, level, neighborPos, null);

        boolean alreadyAligned = (targetShape == RailShape.NORTH_SOUTH)
                ? (currentShape == RailShape.NORTH_SOUTH || currentShape == RailShape.ASCENDING_NORTH || currentShape == RailShape.ASCENDING_SOUTH)
                : (currentShape == RailShape.EAST_WEST || currentShape == RailShape.ASCENDING_EAST || currentShape == RailShape.ASCENDING_WEST);

        if (!alreadyAligned || force) {
            new RailState(level, neighborPos, neighborState)
                    .place(level.hasNeighborSignal(neighborPos), force, targetShape);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Boolean switchState = TrackSwitchBlock.getAdjacentSwitchState(context.getLevel(), context.getClickedPos());
        boolean powered = switchState != null ? switchState : context.getLevel().hasNeighborSignal(context.getClickedPos());
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
        if (level.isClientSide() || ALIGNING.get()) {
            return;
        }
        Boolean switchState = TrackSwitchBlock.getAdjacentSwitchState(level, pos);
        boolean hasSignal = switchState != null ? switchState : level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            Direction facing = state.getValue(FACING);
            RailShape newShape = calculateShape(facing, hasSignal);
            BlockState newState = state.setValue(POWERED, hasSignal).setValue(SHAPE, newShape);
            level.setBlock(pos, newState, 3);
            alignTNeighbors(level, pos, newState, false);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, hasSignal ? 0.7F : 0.5F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = state.getValue(FACING);
        if (player.isShiftKeyDown()) {
            Direction newFacing = facing.getClockWise();
            RailShape newShape = calculateShape(newFacing, state.getValue(POWERED));
            BlockState newState = state.setValue(FACING, newFacing).setValue(SHAPE, newShape);
            level.setBlock(pos, newState, 3);
            alignTNeighbors(level, pos, newState, true);
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.sendOverlayMessage(Component.literal("Wye Facing: " + newFacing.getName()));
            return InteractionResult.SUCCESS;
        }
        boolean powered = !state.getValue(POWERED);
        RailShape newShape = calculateShape(facing, powered);
        BlockState newState = state.setValue(POWERED, powered).setValue(SHAPE, newShape);
        level.setBlock(pos, newState, 3);
        alignTNeighbors(level, pos, newState, false);
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
