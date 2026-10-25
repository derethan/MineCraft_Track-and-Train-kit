package com.example.miningtools.block;

import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Braking / Holding Rail Block:
 * - When UNPOWERED (POWERED = false): Freezes any cart or train consist on this rail in place,
 *   halting connected locomotives and zeroing cart velocity.
 * - When POWERED (POWERED = true): Releases the brake and resumes connected locomotives or gives
 *   standalone carts a gentle launch impulse along the track.
 */
public class BrakingRailBlock extends PoweredRailBlock {

    public BrakingRailBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
    }

    @Override
    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean searchForward, int distance, RailShape shape) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) && !state.is(MiningTools.CONTROL_RAIL.get())) {
            return false;
        }
        return super.isSameRailWithPower(level, pos, searchForward, distance, shape);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof AbstractMinecart cart) {
            if (!state.getValue(POWERED)) {
                TrainChainingUtil.freezeConsistFromBrakingRail(cart);
            }
        }
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        boolean isPowered = state.getValue(POWERED);
        boolean shouldPower = level.hasNeighborSignal(pos)
                || this.findPoweredRailSignal(level, pos, state, true, 0)
                || this.findPoweredRailSignal(level, pos, state, false, 0);
        if (shouldPower != isPowered) {
            level.setBlock(pos, state.setValue(POWERED, shouldPower), 3);
            level.updateNeighborsAt(pos.below(), this);
            if (state.getValue(this.getShapeProperty()).isSlope()) {
                level.updateNeighborsAt(pos.above(), this);
            }
            if (!level.isClientSide()) {
                AABB box = new AABB(pos).inflate(0.3, 0.8, 0.3);
                List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, box);
                for (AbstractMinecart cart : carts) {
                    if (shouldPower) {
                        TrainChainingUtil.releaseConsistFromBrakingRail(cart);
                    } else {
                        TrainChainingUtil.freezeConsistFromBrakingRail(cart);
                    }
                }
            }
        }
    }
}
