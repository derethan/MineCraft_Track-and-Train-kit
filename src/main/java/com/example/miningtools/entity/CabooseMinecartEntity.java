package com.example.miningtools.entity;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Train Caboose with integrated Chunk Anchor capabilities.
 * As long as a train includes this Caboose, all chunks surrounding the caboose,
 * any connected locomotives, and intermediate cars are actively kept loaded and ticking
 * via the level's forced chunk system.
 */
public class CabooseMinecartEntity extends AbstractMinecart {
    private static final EntityDataAccessor<Boolean> DATA_ANCHOR_ACTIVE =
            SynchedEntityData.defineId(CabooseMinecartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FORCED_CHUNKS =
            SynchedEntityData.defineId(CabooseMinecartEntity.class, EntityDataSerializers.INT);

    private int tickCounter = 0;
    private @Nullable ChunkPos lastChunkPos = null;

    public CabooseMinecartEntity(EntityType<? extends CabooseMinecartEntity> type, Level level) {
        super(type, level);
    }

    public CabooseMinecartEntity(Level level, double x, double y, double z) {
        super(MiningTools.CABOOSE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANCHOR_ACTIVE, true);
        builder.define(DATA_FORCED_CHUNKS, 0);
    }

    public boolean isAnchorActive() {
        return this.entityData.get(DATA_ANCHOR_ACTIVE);
    }

    public void setAnchorActive(boolean active) {
        this.entityData.set(DATA_ANCHOR_ACTIVE, active);
        if (!active && this.level() instanceof ServerLevel serverLevel) {
            this.releaseAllForcedChunks(serverLevel);
        }
    }

    public int getForcedChunkCount() {
        return this.entityData.get(DATA_FORCED_CHUNKS);
    }

    @Override
    public BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }

    @Override
    public void tick() {
        if (!TrainChainingUtil.shouldSkipVanillaServerTick(this)) {
            super.tick();
        }
        TrainChainingUtil.tickConsistMovement(this);

        if (this.level().isClientSide()) {
            // Visual client effects: gentle transparent stove smoke rising from the rooftop chimney
            if (this.random.nextInt(4) == 0) {
                float yRot = this.getYRot();
                double rad = Math.toRadians(yRot + 90.0);
                double chimneyForward = -0.62;
                double chimneyLateral = 0.22;

                double cx = this.getX() + Math.cos(rad) * chimneyForward - Math.sin(rad) * chimneyLateral;
                double cz = this.getZ() + Math.sin(rad) * chimneyForward + Math.cos(rad) * chimneyLateral;
                double cy = this.getY() + 1.25;

                this.level().addParticle(ParticleTypes.SMOKE, cx, cy, cz, 0.0, 0.02, 0.0);
            }
            return;
        }

        // Server-side Chunk Anchor Maintenance (immediately on chunk boundary crossing or every 10 ticks)
        this.tickCounter++;
        if (this.isAnchorActive() && !this.isRemoved()) {
            ChunkPos currentChunk = this.chunkPosition();
            if (this.tickCounter == 1 || !currentChunk.equals(this.lastChunkPos) || this.tickCounter % 10 == 0) {
                this.lastChunkPos = currentChunk;
                this.updateForcedChunks();
            }
        }
    }

    private void updateForcedChunks() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Set<ChunkPos> desiredChunks = TrainChainingUtil.computeConsistForcedChunks(this);
        TrainChainingUtil.updateEntityForcedChunks(serverLevel, this, desiredChunks);
        this.entityData.set(DATA_FORCED_CHUNKS, desiredChunks.size());
    }

    private void releaseAllForcedChunks(ServerLevel serverLevel) {
        TrainChainingUtil.releaseEntityForcedChunks(serverLevel, this);
        this.lastChunkPos = null;
        this.entityData.set(DATA_FORCED_CHUNKS, 0);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel serverLevel) {
            this.releaseAllForcedChunks(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("AnchorActive", this.isAnchorActive());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setAnchorActive(input.getBooleanOr("AnchorActive", true));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive()) {
            // Shift right click opens Caboose Rename & Chunk Anchor GUI
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int count = this.getForcedChunkCount();
                String statusStr = "Chunk Anchor: ACTIVE (" + count + " Chunks Forced)";
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, "Train Caboose", statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Normal right-click: player rides inside the caboose cupola
        if (!this.level().isClientSide()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.CABOOSE_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.CABOOSE_ITEM.get());
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 0;
    }




    @Override
    public boolean canCollideWith(Entity entity) {
        if (TrainChainingUtil.isConnectedInSameTrain(this, entity)) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    @Override
    public boolean canBeCollidedWith(@org.jspecify.annotations.Nullable Entity other) {
        if (other != null && TrainChainingUtil.isConnectedInSameTrain(this, other)) {
            return false;
        }
        return super.canBeCollidedWith(other);
    }

    @Override
    public void push(Entity entity) {
        TrainChainingUtil.pushCollidingEntity(this, entity);
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        return TrainChainingUtil.applyRollingStockSlowdown(this, deltaMovement);
    }
}