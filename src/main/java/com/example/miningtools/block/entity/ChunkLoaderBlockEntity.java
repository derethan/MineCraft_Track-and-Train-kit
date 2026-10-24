package com.example.miningtools.block.entity;

import java.util.HashSet;
import java.util.Set;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.ChunkLoaderBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChunkLoaderBlockEntity extends BlockEntity {
    public static final int RADIUS_1X1 = 0; // 1 chunk (current)
    public static final int RADIUS_3X3 = 1; // 9 chunks (3x3 grid, same as Caboose)
    public static final int RADIUS_5X5 = 2; // 25 chunks (5x5 grid)

    private int radius = RADIUS_3X3;
    private boolean requiresRedstone = false;
    private boolean active = true;

    private final Set<ChunkPos> currentlyForced = new HashSet<>();
    private int tickCounter = 0;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.CHUNK_LOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public int getRadius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = Mth.clamp(radius, RADIUS_1X1, RADIUS_5X5);
        this.setChanged();
        if (this.level instanceof ServerLevel sl) {
            this.updateForcedChunks(sl);
        }
    }

    public boolean requiresRedstone() {
        return this.requiresRedstone;
    }

    public void setRequiresRedstone(boolean req) {
        this.requiresRedstone = req;
        this.setChanged();
        if (this.level instanceof ServerLevel sl) {
            this.updateForcedChunks(sl);
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public int getForcedChunkCount() {
        return this.currentlyForced.size();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        be.tickCounter++;
        if (be.tickCounter % 20 == 1 || be.tickCounter == 1) {
            be.updateForcedChunks(sl);
        }
    }

    public void updateForcedChunks(ServerLevel serverLevel) {
        boolean shouldBeActive = !this.requiresRedstone || serverLevel.hasNeighborSignal(this.worldPosition);

        if (this.active != shouldBeActive) {
            this.active = shouldBeActive;
            BlockState currentState = serverLevel.getBlockState(this.worldPosition);
            if (currentState.is(MiningTools.CHUNK_LOADER.get()) && currentState.getValue(ChunkLoaderBlock.ACTIVE) != this.active) {
                serverLevel.setBlock(this.worldPosition, currentState.setValue(ChunkLoaderBlock.ACTIVE, this.active), 3);
            }
        }

        Set<ChunkPos> desiredChunks = new HashSet<>();
        if (this.active) {
            BlockPos pos = this.worldPosition;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int dx = -this.radius; dx <= this.radius; dx++) {
                for (int dz = -this.radius; dz <= this.radius; dz++) {
                    desiredChunks.add(new ChunkPos(cx + dx, cz + dz));
                }
            }
        }

        // Unforce chunks no longer desired
        for (ChunkPos held : new HashSet<>(this.currentlyForced)) {
            if (!desiredChunks.contains(held)) {
                try {
                    serverLevel.setChunkForced(held.x(), held.z(), false);
                } catch (Throwable ignored) {
                }
                this.currentlyForced.remove(held);
            }
        }

        // Force desired chunks
        for (ChunkPos desired : desiredChunks) {
            if (!this.currentlyForced.contains(desired)) {
                try {
                    serverLevel.setChunkForced(desired.x(), desired.z(), true);
                } catch (Throwable ignored) {
                }
                this.currentlyForced.add(desired);
            }
        }
    }

    public void releaseAllForcedChunks(ServerLevel serverLevel) {
        for (ChunkPos held : this.currentlyForced) {
            try {
                serverLevel.setChunkForced(held.x(), held.z(), false);
            } catch (Throwable ignored) {
            }
        }
        this.currentlyForced.clear();
        this.active = false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel sl) {
            this.releaseAllForcedChunks(sl);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Radius", this.radius);
        output.putBoolean("RequiresRedstone", this.requiresRedstone);
        output.putBoolean("Active", this.active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.radius = input.getIntOr("Radius", RADIUS_3X3);
        this.requiresRedstone = input.getBooleanOr("RequiresRedstone", false);
        this.active = input.getBooleanOr("Active", true);
    }
}
