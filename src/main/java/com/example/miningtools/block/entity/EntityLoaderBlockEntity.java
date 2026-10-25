package com.example.miningtools.block.entity;

import java.util.Comparator;
import java.util.List;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.EntityLoaderBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntityLoaderBlockEntity extends AbstractEntityTransferBlockEntity {

    public EntityLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.ENTITY_LOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntityLoaderBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.85, 0.5, 0.85);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive()
                        && !c.isRemoved()
                        && !(c instanceof BasicLocomotiveEntity)
                        && !(c instanceof DieselLocomotiveEntity)
                        && !(c instanceof ElectricLocomotiveEntity)
                        && (c instanceof LivestockCartEntity lc ? lc.canFitMorePassengers() : c.getPassengers().isEmpty())
        );

        if (carts.isEmpty()) {
            setRailPower(level, pos, state, true);
            return;
        }

        AbstractMinecart targetCart = carts.get(0);

        AABB searchBox = new AABB(pos).inflate(be.getRange());
        List<Entity> candidates = level.getEntities(
                (Entity) null,
                searchBox,
                e -> e.isAlive()
                        && !e.isRemoved()
                        && e != targetCart
                        && !e.isPassenger()
                        && !e.isVehicle()
                        && !(e instanceof AbstractMinecart)
                        && be.matchesFilters(e)
        );

        if (candidates.isEmpty()) {
            boolean wasHolding = !state.getValue(EntityLoaderBlock.POWERED);
            setRailPower(level, pos, state, true);
            if (wasHolding) {
                TrainChainingUtil.releaseConsistFromBrakingRail(targetCart);
            }
            return;
        }

        // Hold cart on rail while loading candidates
        setRailPower(level, pos, state, false);
        TrainChainingUtil.freezeConsistFromBrakingRail(targetCart);

        if (be.cooldown > 0) {
            return;
        }

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        Entity chosen = candidates.get(0);

        chosen.teleportTo(targetCart.getX(), targetCart.getY(), targetCart.getZ());
        boolean mounted = chosen.startRiding(targetCart);

        if (mounted) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetCart.getX(), targetCart.getY() + 0.5, targetCart.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            be.cooldown = 15;

            boolean stillHasRoom = (targetCart instanceof LivestockCartEntity lc)
                    ? lc.canFitMorePassengers()
                    : targetCart.getPassengers().isEmpty();
            if (!stillHasRoom || candidates.size() <= 1) {
                setRailPower(level, pos, state, true);
                TrainChainingUtil.releaseConsistFromBrakingRail(targetCart);
            }
        }
    }

    private static void setRailPower(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(EntityLoaderBlock.POWERED) && state.getValue(EntityLoaderBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(EntityLoaderBlock.POWERED, powered), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }
}