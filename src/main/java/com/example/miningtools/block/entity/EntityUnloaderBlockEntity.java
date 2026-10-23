package com.example.miningtools.block.entity;

import java.util.List;

import com.example.miningtools.MiningTools;

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
            return;
        }

        // Check for a minecart with passengers on the rail above
        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.6, 0.4, 0.6);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive() && !c.isRemoved() && !c.getPassengers().isEmpty()
        );

        if (carts.isEmpty()) {
            return;
        }

        AbstractMinecart cart = carts.get(0);
        List<Entity> passengers = List.copyOf(cart.getPassengers());

        for (Entity passenger : passengers) {
            if (be.matchesFilters(passenger)) {
                // Dismount
                passenger.stopRiding();

                // Find a safe landing spot nearby within range
                BlockPos safePos = findSafeUnloadPosition(level, trackPos, be.getRange());
                passenger.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.2F);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.POOF, safePos.getX() + 0.5, safePos.getY() + 0.5, safePos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                }
                be.cooldown = 20; // 1 second cooldown
                break;
            }
        }
    }

    public static BlockPos findSafeUnloadPosition(Level level, BlockPos trackPos, int range) {
        // 1. Try immediate horizontal neighbors of the track (left, right, etc.)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos target = trackPos.relative(dir);
            if (isSafeLanding(level, target)) {
                return target;
            }
        }

        // 2. Expand search within range
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

        // Fallback: 1 block to the side of track
        return trackPos.relative(Direction.EAST);
    }

    private static boolean isSafeLanding(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        if (BaseRailBlock.isRail(feet) || BaseRailBlock.isRail(ground)) {
            return false; // Don't dump onto rails
        }
        boolean feetPassable = feet.isAir() || feet.canBeReplaced();
        boolean headPassable = head.isAir() || head.canBeReplaced();
        boolean groundSolid = ground.isFaceSturdy(level, pos.below(), Direction.UP);

        return feetPassable && headPassable && groundSolid;
    }
}
