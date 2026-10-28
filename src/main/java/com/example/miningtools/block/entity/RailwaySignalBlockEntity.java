package com.example.miningtools.block.entity;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.BrakingRailBlock;
import com.example.miningtools.block.RailwaySignalBlock;
import com.example.miningtools.block.SignalAspect;
import com.example.miningtools.block.TrackSwitchBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RailwaySignalBlockEntity extends BlockEntity {

    private @Nullable BlockPos linkedPartnerSignal = null;
    private @Nullable BlockPos linkedSwitchPos = null;
    private boolean passingSidingMode = true;
    private @Nullable SignalAspect manualOverride = null;
    private int checkCooldown = 0;

    public RailwaySignalBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.RAILWAY_SIGNAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    public @Nullable BlockPos getLinkedPartnerSignal() {
        return this.linkedPartnerSignal;
    }

    public void setLinkedPartnerSignal(@Nullable BlockPos pos) {
        this.linkedPartnerSignal = pos;
        this.setChanged();
    }

    public @Nullable BlockPos getLinkedSwitchPos() {
        return this.linkedSwitchPos;
    }

    public void setLinkedSwitchPos(@Nullable BlockPos pos) {
        this.linkedSwitchPos = pos;
        this.setChanged();
    }

    public boolean isPassingSidingMode() {
        return this.passingSidingMode;
    }

    public void setPassingSidingMode(boolean passingSidingMode) {
        this.passingSidingMode = passingSidingMode;
        this.setChanged();
    }

    public @Nullable SignalAspect getManualOverride() {
        return this.manualOverride;
    }

    public void cycleManualOverride() {
        if (this.manualOverride == null) {
            this.manualOverride = SignalAspect.RED;
        } else if (this.manualOverride == SignalAspect.RED) {
            this.manualOverride = SignalAspect.YELLOW;
        } else if (this.manualOverride == SignalAspect.YELLOW) {
            this.manualOverride = SignalAspect.GREEN;
        } else if (this.manualOverride == SignalAspect.GREEN) {
            this.manualOverride = SignalAspect.SHUNT;
        } else {
            this.manualOverride = null;
        }
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RailwaySignalBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.checkCooldown--;
        if (entity.checkCooldown > 0) return;
        entity.checkCooldown = 4;

        SignalAspect computedAspect = SignalAspect.GREEN;

        // 1. Block Section Occupancy check
        if (entity.linkedPartnerSignal != null) {
            BlockPos partner = entity.linkedPartnerSignal;
            int minX = Math.min(pos.getX(), partner.getX()) - 2;
            int maxX = Math.max(pos.getX(), partner.getX()) + 2;
            int minY = Math.min(pos.getY(), partner.getY()) - 2;
            int maxY = Math.max(pos.getY(), partner.getY()) + 3;
            int minZ = Math.min(pos.getZ(), partner.getZ()) - 2;
            int maxZ = Math.max(pos.getZ(), partner.getZ()) + 2;
            AABB sectionBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

            List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, sectionBounds, Entity::isAlive);
            boolean occupied = false;
            for (AbstractMinecart cart : carts) {
                BlockPos cartPos = cart.blockPosition();
                BlockState cartState = level.getBlockState(cartPos);
                BlockState belowState = level.getBlockState(cartPos.below());
                if (BaseRailBlock.isRail(cartState) || BaseRailBlock.isRail(belowState)) {
                    occupied = true;
                    break;
                }
            }

            if (occupied) {
                computedAspect = SignalAspect.RED;
            } else {
                computedAspect = SignalAspect.GREEN;
            }

            // Passing siding: divert oncoming trains to siding loop when block section is occupied
            if (entity.passingSidingMode && entity.linkedSwitchPos != null && level.isLoaded(entity.linkedSwitchPos)) {
                BlockState swState = level.getBlockState(entity.linkedSwitchPos);
                if (swState.getBlock() instanceof TrackSwitchBlock) {
                    boolean shouldOpen = occupied;
                    if (swState.getValue(TrackSwitchBlock.OPEN) != shouldOpen) {
                        level.setBlock(entity.linkedSwitchPos, swState.setValue(TrackSwitchBlock.OPEN, shouldOpen), 3);
                    }
                }
            }
        }

        SignalAspect finalAspect = (entity.manualOverride != null) ? entity.manualOverride : computedAspect;
        if (state.getValue(RailwaySignalBlock.ASPECT) != finalAspect) {
            level.setBlock(pos, state.setValue(RailwaySignalBlock.ASPECT, finalAspect), 3);
        }

        // 2. Control nearby BrakingRailBlocks
        Direction facing = state.getValue(RailwaySignalBlock.FACING);
        for (Direction dir : Direction.values()) {
            BlockPos railCheck = pos.relative(dir);
            BlockState checkState = level.getBlockState(railCheck);
            if (checkState.getBlock() instanceof BrakingRailBlock) {
                boolean shouldPower = (finalAspect != SignalAspect.RED);
                if (checkState.getValue(BrakingRailBlock.POWERED) != shouldPower) {
                    level.setBlock(railCheck, checkState.setValue(BrakingRailBlock.POWERED, shouldPower), 3);
                }
            }
        }

        // 3. Train Stop Enforcement directly in front of signal
        if (finalAspect == SignalAspect.RED) {
            BlockPos frontPos = pos.relative(facing.getOpposite());
            AABB stopBounds = new AABB(frontPos).inflate(1.2, 1.2, 1.2);
            List<AbstractMinecart> approachingCarts = level.getEntitiesOfClass(AbstractMinecart.class, stopBounds, Entity::isAlive);

            for (AbstractMinecart cart : approachingCarts) {
                boolean isShunting = false;
                for (Entity passenger : cart.getPassengers()) {
                    if (passenger instanceof Player p && p.isShiftKeyDown()) {
                        isShunting = true;
                        break;
                    }
                }

                // If not shunting and cart is in motion, safely halt it
                if (!isShunting && cart.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) {
                    cart.setDeltaMovement(cart.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
                    if (cart.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3D) {
                        cart.setDeltaMovement(Vec3.ZERO);
                    }

                    if (cart instanceof BasicLocomotiveEntity loco) {
                        loco.setSpeedLevel(0);
                    } else if (cart instanceof DieselLocomotiveEntity loco) {
                        loco.setSpeedLevel(0);
                    } else if (cart instanceof ElectricLocomotiveEntity loco) {
                        loco.setSpeedLevel(0);
                    }

                    for (Entity passenger : cart.getPassengers()) {
                        if (passenger instanceof Player player) {
                            player.sendOverlayMessage(Component.literal("[Signal] Danger (Red): Track Block Occupied Ahead").withStyle(ChatFormatting.RED));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("PassingSiding", this.passingSidingMode);
        output.putInt("ManualOverride", this.manualOverride == null ? -1 : this.manualOverride.getIndex());

        if (this.linkedPartnerSignal != null) {
            output.putBoolean("HasPartner", true);
            output.putInt("PartnerX", this.linkedPartnerSignal.getX());
            output.putInt("PartnerY", this.linkedPartnerSignal.getY());
            output.putInt("PartnerZ", this.linkedPartnerSignal.getZ());
        } else {
            output.putBoolean("HasPartner", false);
        }

        if (this.linkedSwitchPos != null) {
            output.putBoolean("HasSwitch", true);
            output.putInt("SwitchX", this.linkedSwitchPos.getX());
            output.putInt("SwitchY", this.linkedSwitchPos.getY());
            output.putInt("SwitchZ", this.linkedSwitchPos.getZ());
        } else {
            output.putBoolean("HasSwitch", false);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.passingSidingMode = input.getBooleanOr("PassingSiding", true);
        int mo = input.getIntOr("ManualOverride", -1);
        this.manualOverride = (mo >= 0 && mo <= 3) ? SignalAspect.fromIndex(mo) : null;

        if (input.getBooleanOr("HasPartner", false)) {
            int px = input.getIntOr("PartnerX", 0);
            int py = input.getIntOr("PartnerY", 0);
            int pz = input.getIntOr("PartnerZ", 0);
            this.linkedPartnerSignal = new BlockPos(px, py, pz);
        } else {
            this.linkedPartnerSignal = null;
        }

        if (input.getBooleanOr("HasSwitch", false)) {
            int sx = input.getIntOr("SwitchX", 0);
            int sy = input.getIntOr("SwitchY", 0);
            int sz = input.getIntOr("SwitchZ", 0);
            this.linkedSwitchPos = new BlockPos(sx, sy, sz);
        } else {
            this.linkedSwitchPos = null;
        }
    }
}
