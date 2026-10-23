package com.example.miningtools.block.entity;

import java.util.Comparator;
import java.util.List;

import com.example.miningtools.MiningTools;
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
            return;
        }

        // Check for an empty minecart on the rail above
        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.6, 0.4, 0.6);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved() && (c instanceof LivestockCartEntity lc ? lc.canFitMorePassengers() : c.getPassengers().isEmpty()) && !TrainChainingUtil.isLeadCart(c)
        );

        if (carts.isEmpty()) {
            return;
        }

        AbstractMinecart targetCart = carts.get(0);

        // Search for entities within configured range
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
            return;
        }

        // Pick closest candidate
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        Entity chosen = candidates.get(0);

        // Mount into cart
        chosen.teleportTo(targetCart.getX(), targetCart.getY(), targetCart.getZ());
        boolean mounted = chosen.startRiding(targetCart);

        if (mounted) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetCart.getX(), targetCart.getY() + 0.5, targetCart.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            be.cooldown = 20; // 1 second cooldown
        }
    }
}
