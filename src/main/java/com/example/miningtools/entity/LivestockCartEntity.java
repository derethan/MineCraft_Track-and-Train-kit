package com.example.miningtools.entity;

import net.minecraft.core.BlockPos;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

import com.example.miningtools.MiningTools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Authentic Railroad Livestock Car (Stock Car).
 * Designed for agricultural livestock and entity transport.
 * Features 2 stalls (front and rear) allowing it to comfortably hold up to 2 entities,
 * with open-slatted walls so cargo can see out and be seen.
 */
public class LivestockCartEntity extends AbstractMinecart {

    public LivestockCartEntity(EntityType<? extends LivestockCartEntity> type, Level level) {
        super(type, level);
    }

    public LivestockCartEntity(Level level, double x, double y, double z) {
        super(MiningTools.LIVESTOCK_CART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.LIVESTOCK_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.LIVESTOCK_CART_ITEM.get());
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
    public boolean isRideable() {
        return true;
    }

    public boolean canFitMorePassengers() {
        return this.getPassengers().size() < 2;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.canFitMorePassengers();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int count = this.getPassengers().size();
                String statusStr = "Occupancy: " + count + " / 2 Animals";
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, "Livestock Cart", statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }

        Level level = this.level();
        if (!level.isClientSide()) {
            // Check if player is holding a lead with a leashed mob nearby
            if (this.canFitMorePassengers()) {
                List<Mob> leashedMobs = level.getEntitiesOfClass(
                        Mob.class,
                        this.getBoundingBox().inflate(7.0D),
                        m -> m.getLeashHolder() == player && m.getBbWidth() <= 1.5F
                );
                if (!leashedMobs.isEmpty()) {
                    Mob mob = leashedMobs.get(0);
                    mob.dropLeash();
                    mob.startRiding(this);
                    return InteractionResult.CONSUME;
                }
            }

            // Normal player mount
            if (this.canFitMorePassengers()) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
        }

        return InteractionResult.SUCCESS;
    }



    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            int index = this.getPassengers().indexOf(passenger);
            int total = this.getPassengers().size();

            // Stall separation: index 0 sits in the front bay, index 1 sits in the rear bay
            double offset = 0.0D;
            if (total > 1) {
                offset = (index == 0) ? 0.38D : -0.38D;
            }

            float yaw = this.getYRot();
            double rad = Math.toRadians(yaw);
            double forwardX = -Math.sin(rad);
            double forwardZ = Math.cos(rad);

            double px = this.getX() + forwardX * offset;
            double pz = this.getZ() + forwardZ * offset;
            double py = this.getY() + 0.22D;

            moveFunction.accept(passenger, px, py, pz);
        }
    }

    @Override
    public void activateMinecart(ServerLevel level, int x, int y, int z, boolean receivingPower) {
        if (receivingPower) {
            BlockPos railPos = new BlockPos(x, y, z);
            if (level.getBlockState(railPos.below()).is(MiningTools.ENTITY_LOADER.get())) {
                return; // Never eject passengers while positioned over an Entity Loader!
            }
            if (this.isVehicle()) {
                this.ejectPassengers();
            }
        }
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
    public net.minecraft.core.BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }

    @Override
    public void push(Entity entity) {
        TrainChainingUtil.pushCollidingEntity(this, entity);
    }

    @Override
    public void tick() {
        if (!TrainChainingUtil.shouldSkipVanillaServerTick(this)) {
            super.tick();
        }
        TrainChainingUtil.tickConsistMovement(this);
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        return TrainChainingUtil.applyRollingStockSlowdown(this, deltaMovement);
    }
}