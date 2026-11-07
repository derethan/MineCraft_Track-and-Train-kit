package com.example.miningtools.block.entity;

import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.EntityUnloaderBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.TunnelPaverEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntityUnloaderBlockEntity extends AbstractEntityTransferBlockEntity {

    public EntityUnloaderBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.ENTITY_UNLOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntityUnloaderBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.35, 0.5, 0.35);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved()
        );

        if (carts.isEmpty()) {
            setRailPower(level, pos, state, true);
            return;
        }

        double cx = trackPos.getX() + 0.5D;
        double cz = trackPos.getZ() + 0.5D;
        AbstractMinecart closestCart = null;
        double closestDistSq = Double.MAX_VALUE;
        for (AbstractMinecart c : carts) {
            double dx = c.getX() - cx;
            double dz = c.getZ() - cz;
            double dSq = dx * dx + dz * dz;
            if (dSq < closestDistSq) {
                closestDistSq = dSq;
                closestCart = c;
            }
        }

        if (closestCart == null || closestDistSq > 0.85D * 0.85D) {
            setRailPower(level, pos, state, true);
            return;
        }

        boolean isValidTarget = !(closestCart instanceof BasicLocomotiveEntity)
                && !(closestCart instanceof DieselLocomotiveEntity)
                && !(closestCart instanceof ElectricLocomotiveEntity)
                && !(closestCart instanceof TunnelBoreEntity)
                && !(closestCart instanceof TunnelPaverEntity)
                && !closestCart.getPassengers().isEmpty();

        if (!isValidTarget) {
            setRailPower(level, pos, state, true);
            return;
        }

        AbstractMinecart cart = closestCart;
        List<Entity> passengers = List.copyOf(cart.getPassengers());
        Entity matchingPassenger = null;
        int matchingCount = 0;

        for (Entity passenger : passengers) {
            if (be.matchesFilters(passenger)) {
                if (matchingPassenger == null) {
                    matchingPassenger = passenger;
                }
                matchingCount++;
            }
        }

        if (matchingPassenger == null) {
            boolean wasHolding = !state.getValue(EntityUnloaderBlock.POWERED);
            setRailPower(level, pos, state, true);
            if (wasHolding) {
                TrainChainingUtil.releaseConsistFromBrakingRail(cart);
            }
            return;
        }

        // Depower rail above and hold consist until all matching passengers are unloaded
        setRailPower(level, pos, state, false);
        TrainChainingUtil.freezeConsistFromBrakingRail(cart);

        if (be.cooldown > 0) {
            return;
        }

        matchingPassenger.stopRiding();
        BlockPos safePos = findSafeUnloadPosition(level, trackPos, be.getRange());
        matchingPassenger.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, safePos.getX() + 0.5, safePos.getY() + 0.5, safePos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
        }
        be.cooldown = 15;

        if (matchingCount <= 1) {
            setRailPower(level, pos, state, true);
            TrainChainingUtil.releaseConsistFromBrakingRail(cart);
        }
    }

    private static void setRailPower(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(EntityUnloaderBlock.POWERED) && state.getValue(EntityUnloaderBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(EntityUnloaderBlock.POWERED, powered), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }

    public static BlockPos findSafeUnloadPosition(Level level, BlockPos trackPos, int range) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos target = trackPos.relative(dir);
            if (isSafeLanding(level, target)) {
                return target;
            }
        }

        int maxR = Math.min(range, 8);
        for (int r = 2; r <= maxR; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) == r || Math.abs(dz) == r) {
                        for (int dy = -1; dy <= 2; dy++) {
                            BlockPos target = trackPos.offset(dx, dy, dz);
                            if (isSafeLanding(level, target)) {
                                return target;
                            }
                        }
                    }
                }
            }
        }

        return trackPos.relative(Direction.EAST);
    }

    private static boolean isSafeLanding(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        if (BaseRailBlock.isRail(feet) || BaseRailBlock.isRail(ground)) {
            return false;
        }
        boolean feetPassable = feet.isAir() || feet.canBeReplaced();
        boolean headPassable = head.isAir() || head.canBeReplaced();
        boolean groundSolid = ground.isFaceSturdy(level, pos.below(), Direction.UP);

        return feetPassable && headPassable && groundSolid;
    }
}