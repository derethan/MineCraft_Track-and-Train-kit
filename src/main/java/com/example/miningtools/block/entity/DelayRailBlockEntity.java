package com.example.miningtools.block.entity;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.DelayRailBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Station Delay Rail Block Entity:
 * Manages an automated dwell timer that safely halts approaching trains for
 * a configurable duration before releasing them onto their route.
 */
public class DelayRailBlockEntity extends BlockEntity {

    public static final int DIR_KEEP = 0;
    public static final int DIR_FORWARD = 1;
    public static final int DIR_REVERSE = 2;
    public static final int DIR_INVERT = 3;

    private boolean timerEnabled = true;
    private int delaySeconds = 5;
    private int releaseDirection = DIR_KEEP;
    private String trainNameFilter = "";

    private int remainingTicks = 0;
    private @Nullable UUID heldCartUUID = null;
    private int departCooldown = 0;
    private @Nullable UUID lastDepartedCartUUID = null;

    public DelayRailBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.DELAY_RAIL_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isTimerEnabled() {
        return this.timerEnabled;
    }

    public void setTimerEnabled(boolean enabled) {
        this.timerEnabled = enabled;
        this.setChanged();
        if (!enabled && this.heldCartUUID != null && this.level != null && !this.level.isClientSide()) {
            this.remainingTicks = 0; // Immediately release if disabled in GUI
        }
    }

    public int getDelaySeconds() {
        return this.delaySeconds;
    }

    public void setDelaySeconds(int seconds) {
        this.delaySeconds = Mth.clamp(seconds, 1, 300);
        this.setChanged();
    }

    public int getReleaseDirection() {
        return this.releaseDirection;
    }

    public void setReleaseDirection(int mode) {
        this.releaseDirection = Mth.clamp(mode, 0, 3);
        this.setChanged();
    }

    public String getTrainNameFilter() {
        return this.trainNameFilter != null ? this.trainNameFilter : "";
    }

    public void setTrainNameFilter(String filter) {
        this.trainNameFilter = filter != null ? filter.trim() : "";
        this.setChanged();
    }

    public int getRemainingTicks() {
        return this.remainingTicks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DelayRailBlockEntity be) {
        if (be.departCooldown > 0) {
            be.departCooldown--;
        }

        // 1. If currently holding a train:
        if (be.heldCartUUID != null) {
            Entity ent = level.getEntity(be.heldCartUUID);
            if (!(ent instanceof AbstractMinecart heldCart) || heldCart.isRemoved() || !heldCart.isAlive()
                    || heldCart.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 9.0) {
                // Cart gone or destroyed
                be.heldCartUUID = null;
                be.remainingTicks = 0;
                if (state.getValue(DelayRailBlock.HOLDING)) {
                    level.setBlock(pos, state.setValue(DelayRailBlock.HOLDING, false), 3);
                }
                return;
            }

            // Redstone signal overrides and releases early
            if (level.hasNeighborSignal(pos)) {
                be.remainingTicks = 0;
            }

            if (be.remainingTicks > 0) {
                // Hold firmly in place
                heldCart.setDeltaMovement(Vec3.ZERO);
                TrainChainingUtil.freezeConsistFromBrakingRail(heldCart);

                // Periodic ticking audio / particles
                if (be.remainingTicks % 20 == 0) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.4F, 1.8F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 1, 0.05, 0.02, 0.05, 0.01);
                    }
                }
                be.remainingTicks--;
            }

            if (be.remainingTicks <= 0) {
                // Delay completed: release the train!
                if (be.releaseDirection != DIR_KEEP) {
                    applyReleaseDirection(heldCart, be.releaseDirection);
                }

                TrainChainingUtil.releaseConsistFromBrakingRail(heldCart);

                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 4, 0.1, 0.05, 0.1, 0.02);
                }

                be.lastDepartedCartUUID = heldCart.getUUID();
                be.departCooldown = 60; // 3 seconds grace period to clear track
                be.heldCartUUID = null;

                if (state.getValue(DelayRailBlock.HOLDING)) {
                    level.setBlock(pos, state.setValue(DelayRailBlock.HOLDING, false), 3);
                }
            }
            return;
        }

        // 2. If idle: Ensure holding state is false
        if (state.getValue(DelayRailBlock.HOLDING)) {
            level.setBlock(pos, state.setValue(DelayRailBlock.HOLDING, false), 3);
        }

        // If timer is disabled (pass-through mode) or powered by redstone, allow free transit
        if (!be.timerEnabled || level.hasNeighborSignal(pos)) {
            return;
        }

        // 3. Scan for incoming carts
        AABB trackBox = new AABB(pos).inflate(0.35, 0.8, 0.35);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved()
        );

        if (carts.isEmpty()) {
            return;
        }

        AbstractMinecart cart = carts.get(0);

        // Check depart grace period
        if (be.departCooldown > 0 && be.lastDepartedCartUUID != null) {
            if (cart.getUUID().equals(be.lastDepartedCartUUID)) {
                return;
            }
            Entity lastDep = level.getEntity(be.lastDepartedCartUUID);
            if (lastDep instanceof AbstractMinecart lastDepCart && TrainChainingUtil.isConnectedInSameTrain(cart, lastDepCart)) {
                return;
            }
        }

        // Train name filter check
        String filter = be.getTrainNameFilter().trim();
        if (!filter.isEmpty() && !filter.equals("*")) {
            String cartName = TrainChainingUtil.getCartIndividualName(cart);
            String trainName = BasicLocomotiveEntity.resolveTrainName(cart);
            boolean matches = TrainChainingUtil.matchesCartNameFilter(cart, filter, null)
                    || cartName.equalsIgnoreCase(filter)
                    || trainName.equalsIgnoreCase(filter);
            if (!matches) {
                return; // Let non-matching train pass through
            }
        }

        // CAPTURE TRAIN & BEGIN DELAY
        be.heldCartUUID = cart.getUUID();
        be.remainingTicks = Math.max(1, be.getDelaySeconds()) * 20;

        cart.setDeltaMovement(Vec3.ZERO);
        TrainChainingUtil.freezeConsistFromBrakingRail(cart);

        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
        level.setBlock(pos, state.setValue(DelayRailBlock.HOLDING, true), 3);
    }

    private static void applyReleaseDirection(AbstractMinecart cart, int mode) {
        AbstractMinecart head = TrainChainingUtil.getTrainHead(cart);
        if (head == null || head.isRemoved()) head = cart;

        // Apply direction to any locomotive in consist
        if (head instanceof BasicLocomotiveEntity bl) {
            boolean rev = bl.isReversed();
            if (mode == DIR_FORWARD) rev = false;
            else if (mode == DIR_REVERSE) rev = true;
            else if (mode == DIR_INVERT) rev = !rev;
            bl.setReversed(rev);
            TrainChainingUtil.syncTrainConsist(bl, bl.isRunning(), bl.getSpeedLevel(), rev);
        } else if (head instanceof DieselLocomotiveEntity dl) {
            boolean rev = dl.isReversed();
            if (mode == DIR_FORWARD) rev = false;
            else if (mode == DIR_REVERSE) rev = true;
            else if (mode == DIR_INVERT) rev = !rev;
            dl.setReversed(rev);
            TrainChainingUtil.syncTrainConsist(dl, dl.isRunning(), dl.getSpeedLevel(), rev);
        } else if (head instanceof ElectricLocomotiveEntity el) {
            boolean rev = el.isReversed();
            if (mode == DIR_FORWARD) rev = false;
            else if (mode == DIR_REVERSE) rev = true;
            else if (mode == DIR_INVERT) rev = !rev;
            el.setReversed(rev);
            TrainChainingUtil.syncTrainConsist(el, el.isRunning(), el.getSpeedLevel(), rev);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("TimerEnabled", this.timerEnabled);
        output.putInt("DelaySeconds", this.delaySeconds);
        output.putInt("ReleaseDirection", this.releaseDirection);
        output.putString("TrainNameFilter", this.getTrainNameFilter());
        output.putInt("RemainingTicks", this.remainingTicks);
        if (this.heldCartUUID != null) {
            output.putString("HeldCart", this.heldCartUUID.toString());
        }
        if (this.lastDepartedCartUUID != null) {
            output.putString("LastDepartedCart", this.lastDepartedCartUUID.toString());
        }
        output.putInt("DepartCooldown", this.departCooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.timerEnabled = input.getBooleanOr("TimerEnabled", true);
        this.delaySeconds = input.getInt("DelaySeconds").orElse(5);
        this.releaseDirection = input.getInt("ReleaseDirection").orElse(DIR_KEEP);
        this.trainNameFilter = input.getString("TrainNameFilter").orElse("");
        this.remainingTicks = input.getInt("RemainingTicks").orElse(0);

        String heldStr = input.getString("HeldCart").orElse(null);
        if (heldStr != null && !heldStr.isBlank()) {
            try {
                this.heldCartUUID = UUID.fromString(heldStr);
            } catch (Throwable ignored) {
                this.heldCartUUID = null;
            }
        } else {
            this.heldCartUUID = null;
        }

        String depStr = input.getString("LastDepartedCart").orElse(null);
        if (depStr != null && !depStr.isBlank()) {
            try {
                this.lastDepartedCartUUID = UUID.fromString(depStr);
            } catch (Throwable ignored) {
                this.lastDepartedCartUUID = null;
            }
        } else {
            this.lastDepartedCartUUID = null;
        }

        this.departCooldown = input.getInt("DepartCooldown").orElse(0);
    }
}
