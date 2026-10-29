package com.example.miningtools.block.entity;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Speed Control Rail Block Entity:
 * Detects arriving locomotives and train consists, setting their speed/gear
 * and optional travel direction mode. Supports selective filtering by train name.
 */
public class SpeedControlRailBlockEntity extends BlockEntity {

    public static final int DIR_KEEP = 0;
    public static final int DIR_FORWARD = 1;
    public static final int DIR_REVERSE = 2;

    private int targetGear = 3; // 0 = Stop / Neutral, 1-5 = Speed gears 1 to 5
    private int directionMode = DIR_KEEP; // 0 = Keep, 1 = Force Forward, 2 = Force Reverse
    private String trainNameFilter = ""; // Empty = all trains

    private @Nullable UUID lastTriggeredLocoUUID = null;
    private int cooldown = 0;

    public SpeedControlRailBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.SPEED_CONTROL_RAIL_BLOCK_ENTITY.get(), pos, state);
    }

    public int getTargetGear() {
        return this.targetGear;
    }

    public void setTargetGear(int gear) {
        this.targetGear = Mth.clamp(gear, 0, 5);
        this.setChanged();
    }

    public int getDirectionMode() {
        return this.directionMode;
    }

    public void setDirectionMode(int mode) {
        this.directionMode = Mth.clamp(mode, 0, 2);
        this.setChanged();
    }

    public String getTrainNameFilter() {
        return this.trainNameFilter != null ? this.trainNameFilter : "";
    }

    public void setTrainNameFilter(String filter) {
        this.trainNameFilter = filter != null ? filter.trim() : "";
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpeedControlRailBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        AABB trackBox = new AABB(pos).inflate(0.35, 0.8, 0.35);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved()
        );

        if (carts.isEmpty()) {
            be.lastTriggeredLocoUUID = null;
            return;
        }

        // Find locomotive on track or connected in consist
        AbstractMinecart loco = null;
        for (AbstractMinecart c : carts) {
            if (TrainChainingUtil.isLocomotive(c)) {
                loco = c;
                break;
            }
        }
        if (loco == null) {
            for (AbstractMinecart c : carts) {
                for (AbstractMinecart member : TrainChainingUtil.getConnectedConsist(c)) {
                    if (TrainChainingUtil.isLocomotive(member)) {
                        loco = member;
                        break;
                    }
                }
                if (loco != null) break;
            }
        }

        if (loco == null) {
            return;
        }

        // Prevent repeated activations while train rolls over or parks on rail
        if (loco.getUUID().equals(be.lastTriggeredLocoUUID)) {
            return;
        }

        if (be.cooldown > 0) {
            return;
        }

        be.lastTriggeredLocoUUID = loco.getUUID();
        be.cooldown = 20;

        // Train name filtering
        String filter = be.getTrainNameFilter().trim();
        if (!filter.isEmpty() && !filter.equals("*")) {
            String locoName = TrainChainingUtil.getCartIndividualName(loco);
            String trainName = BasicLocomotiveEntity.resolveTrainName(loco);
            boolean matches = TrainChainingUtil.matchesCartNameFilter(loco, filter, null)
                    || locoName.equalsIgnoreCase(filter)
                    || trainName.equalsIgnoreCase(filter);
            if (!matches) {
                return;
            }
        }

        // Apply speed gear & direction
        int gear = be.getTargetGear();
        int dirMode = be.getDirectionMode();

        boolean setReversed = loco instanceof BasicLocomotiveEntity bl ? bl.isReversed() :
                              loco instanceof DieselLocomotiveEntity dl ? dl.isReversed() :
                              loco instanceof ElectricLocomotiveEntity el ? el.isReversed() : false;

        if (dirMode == DIR_FORWARD) {
            setReversed = false;
        } else if (dirMode == DIR_REVERSE) {
            setReversed = true;
        }

        if (loco instanceof BasicLocomotiveEntity bl) {
            if (gear <= 0) {
                bl.setRunning(false);
                bl.setDeltaMovement(Vec3.ZERO);
            } else {
                bl.setRunning(true);
                bl.setSpeedLevel(gear);
            }
            bl.setReversed(setReversed);
            TrainChainingUtil.syncTrainConsist(bl, bl.isRunning(), bl.getSpeedLevel(), setReversed);
        } else if (loco instanceof DieselLocomotiveEntity dl) {
            if (gear <= 0) {
                dl.setRunning(false);
                dl.setDeltaMovement(Vec3.ZERO);
            } else {
                dl.setRunning(true);
                dl.setSpeedLevel(gear);
            }
            dl.setReversed(setReversed);
            TrainChainingUtil.syncTrainConsist(dl, dl.isRunning(), dl.getSpeedLevel(), setReversed);
        } else if (loco instanceof ElectricLocomotiveEntity el) {
            if (gear <= 0) {
                el.setRunning(false);
                el.setDeltaMovement(Vec3.ZERO);
            } else {
                el.setRunning(true);
                el.setSpeedLevel(gear);
            }
            el.setReversed(setReversed);
            TrainChainingUtil.syncTrainConsist(el, el.isRunning(), el.getSpeedLevel(), setReversed);
        }

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TargetGear", this.targetGear);
        output.putInt("DirectionMode", this.directionMode);
        output.putString("TrainNameFilter", this.getTrainNameFilter());
        if (this.lastTriggeredLocoUUID != null) {
            output.putString("LastTriggeredLoco", this.lastTriggeredLocoUUID.toString());
        }
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.targetGear = input.getInt("TargetGear").orElse(3);
        this.directionMode = input.getInt("DirectionMode").orElse(DIR_KEEP);
        this.trainNameFilter = input.getString("TrainNameFilter").orElse("");
        String uuidStr = input.getString("LastTriggeredLoco").orElse(null);
        if (uuidStr != null && !uuidStr.isBlank()) {
            try {
                this.lastTriggeredLocoUUID = UUID.fromString(uuidStr);
            } catch (Throwable ignored) {
                this.lastTriggeredLocoUUID = null;
            }
        } else {
            this.lastTriggeredLocoUUID = null;
        }
        this.cooldown = input.getInt("Cooldown").orElse(0);
    }
}
