package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * Junction Rail (inspired by Railcraft's Junction Track).
 * Acts as a 4-way flat cross intersection (+) allowing minecarts travelling
 * either North-South or East-West to pass straight through the intersection.
 */
public class JunctionRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create(
            "shape",
            RailShape.class,
            RailShape.NORTH_SOUTH,
            RailShape.EAST_WEST
    );

    private static final ThreadLocal<Boolean> ALIGNING = ThreadLocal.withInitial(() -> false);

    public JunctionRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(SHAPE, RailShape.NORTH_SOUTH)
                        .setValue(WATERLOGGED, false)
        );
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
        return shape == RailShape.NORTH_SOUTH || shape == RailShape.EAST_WEST;
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        if (cart != null) {
            Vec3 movement = cart.getDeltaMovement();
            double dx = Math.abs(movement.x);
            double dz = Math.abs(movement.z);
            if (dx > 1.0E-4 || dz > 1.0E-4) {
                return dx > dz ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
            }

            // Fallback to cart yaw angle (matching Railcraft's TrackKitJunction behavior)
            float yaw = cart.getYRot() % 180.0F;
            if (yaw < 0.0F) {
                yaw += 180.0F;
            }
            if (yaw >= 45.0F && yaw <= 135.0F) {
                return RailShape.NORTH_SOUTH;
            }
            return RailShape.EAST_WEST;
        }
        return state.getValue(SHAPE);
    }

    @Override
    protected BlockState updateDir(Level level, BlockPos pos, BlockState state, boolean first) {
        if (level.isClientSide()) {
            return state;
        }
        return alignIntersection(level, pos, state, first);
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        if (!level.isClientSide()) {
            alignIntersection(level, pos, state, false);
        }
    }

    /**
     * Aligns neighboring rails on both the North-South and East-West axes so that
     * all four sides connect straight into this cross intersection without curving.
     */
    private BlockState alignIntersection(Level level, BlockPos pos, BlockState state, boolean force) {
        if (ALIGNING.get()) {
            return state;
        }
        try {
            ALIGNING.set(true);

            // 1. Align North and South neighbors while presenting as NORTH_SOUTH
            BlockState nsState = state.setValue(SHAPE, RailShape.NORTH_SOUTH);
            level.setBlock(pos, nsState, Block.UPDATE_CLIENTS);
            alignNeighborOnAxis(level, pos.north(), RailShape.NORTH_SOUTH, force);
            alignNeighborOnAxis(level, pos.south(), RailShape.NORTH_SOUTH, force);

            // 2. Align East and West neighbors while presenting as EAST_WEST
            BlockState ewState = state.setValue(SHAPE, RailShape.EAST_WEST);
            level.setBlock(pos, ewState, Block.UPDATE_CLIENTS);
            alignNeighborOnAxis(level, pos.east(), RailShape.EAST_WEST, force);
            alignNeighborOnAxis(level, pos.west(), RailShape.EAST_WEST, force);

            // Restore initial state
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            return state;
        } finally {
            ALIGNING.set(false);
        }
    }

    private void alignNeighborOnAxis(Level level, BlockPos neighborPos, RailShape targetShape, boolean force) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!BaseRailBlock.isRail(neighborState) || neighborState.getBlock() instanceof JunctionRailBlock) {
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        RailShape currentShape = state.getValue(SHAPE);
        RailShape newShape = this.rotate(currentShape, rotation);
        return this.isValidRailShape(newShape) ? state.setValue(SHAPE, newShape) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        RailShape currentShape = state.getValue(SHAPE);
        RailShape newShape = this.mirror(currentShape, mirror);
        return this.isValidRailShape(newShape) ? state.setValue(SHAPE, newShape) : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }
}
