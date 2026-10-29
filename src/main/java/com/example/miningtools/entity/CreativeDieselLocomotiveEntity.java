package com.example.miningtools.entity;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.Config;
import com.example.miningtools.MiningTools;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Creative Diesel Locomotive:
 * Infinite fuel, creative-only prestige executive locomotive.
 * Never requires refueling, never drains fuel tanks or tender carts.
 */
public class CreativeDieselLocomotiveEntity extends DieselLocomotiveEntity {

    public CreativeDieselLocomotiveEntity(EntityType<? extends CreativeDieselLocomotiveEntity> type, Level level) {
        super(type, level);
    }

    public CreativeDieselLocomotiveEntity(Level level, double x, double y, double z) {
        this(MiningTools.CREATIVE_DIESEL_LOCOMOTIVE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public boolean hasFuel() {
        return true;
    }

    @Override
    public int getFluidAmount() {
        return this.getCapacity();
    }

    @Override
    public String getFluidName() {
        return "Creative Fuel";
    }

    @Override
    public float getFluidFillRatio() {
        return 1.0F;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.CREATIVE_DIESEL_LOCOMOTIVE_ITEM.get();
    }

    @Override
    protected void drainFuelItemSlot() {
        // Creative mode: no fuel container draining required
    }

    @Override
    protected void siphonFuelFromTenderCarts() {
        // Creative mode: no siphon required
    }

    @Override
    protected void tickFuelAndMovement() {
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState railState = this.level().getBlockState(railPos);
        boolean onRails = BaseRailBlock.isRail(railState);

        if (railState.is(MiningTools.CONTROL_RAIL.get()) || railState.is(MiningTools.BRAKING_RAIL.get())) {
            boolean powered = railState.getValue(PoweredRailBlock.POWERED);
            if (powered) {
                if (!this.isRunning() && !TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
                    this.startLocomotiveFromRail();
                }
            } else {
                if (this.isRunning()) {
                    this.stopLocomotiveFromRail();
                }
            }
        } else if (this.isRunning() && TrainChainingUtil.isConsistHeldByBrakingRail(this)) {
            this.stopLocomotiveFromRail();
        }

        // Infinite fuel: locomotive remains operational when running is toggled
        if (this.isRunning() && onRails) {
            this.setActive(true);
            if (TrainChainingUtil.hasActiveLeadLocomotiveAhead(this)) {
                return;
            }

            TrainChainingUtil.TrainConsistStats stats = TrainChainingUtil.calculateConsistStats(this);
            double baseSpeed = this.getLocomotiveSpeed();
            double maxSpeed = baseSpeed * stats.speedFactor;
            double accel = Config.DIESEL_LOCOMOTIVE_ACCELERATION.get();

            if (stats.speedFactor <= 0.0D) {
                maxSpeed = 0.0D;
            }

            Direction travelDir = this.getTravelDirection();
            Vec3 currentDelta = super.getDeltaMovement();
            double currentSpeed = currentDelta.length();
            double nextSpeed = currentSpeed + (maxSpeed - currentSpeed) * accel;
            double newVx = travelDir.getStepX() * nextSpeed;
            double newVz = travelDir.getStepZ() * nextSpeed;
            this.setDeltaMovement(newVx, currentDelta.y, newVz);
        } else {
            this.setActive(false);
        }
    }
}
