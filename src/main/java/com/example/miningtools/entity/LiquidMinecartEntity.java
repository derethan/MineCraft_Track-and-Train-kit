package com.example.miningtools.entity;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class LiquidMinecartEntity extends AbstractMinecart {
    public static final int TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME; // 16,000 mB

    private static final EntityDataAccessor<String> DATA_FLUID_NAME =
            SynchedEntityData.defineId(LiquidMinecartEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_FLUID_AMOUNT =
            SynchedEntityData.defineId(LiquidMinecartEntity.class, EntityDataSerializers.INT);

    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, TANK_CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousStack) {
            super.onContentsChanged(index, previousStack);
            LiquidMinecartEntity.this.syncFluidData();
        }
    };

    public LiquidMinecartEntity(EntityType<? extends LiquidMinecartEntity> type, Level level) {
        super(type, level);
    }

    public LiquidMinecartEntity(Level level, double x, double y, double z) {
        super(MiningTools.LIQUID_MINECART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLUID_NAME, "");
        builder.define(DATA_FLUID_AMOUNT, 0);
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    public FluidStack getStoredFluid() {
        return FluidUtil.getStack(this.tank, 0);
    }

    public String getFluidName() {
        return this.entityData.get(DATA_FLUID_NAME);
    }

    public int getFluidAmount() {
        return this.entityData.get(DATA_FLUID_AMOUNT);
    }

    public float getFillRatio() {
        return (float) getFluidAmount() / (float) TANK_CAPACITY;
    }

    public void syncFluidData() {
        if (!this.level().isClientSide()) {
            FluidStack stack = FluidUtil.getStack(this.tank, 0);
            if (stack.isEmpty()) {
                this.entityData.set(DATA_FLUID_NAME, "");
                this.entityData.set(DATA_FLUID_AMOUNT, 0);
            } else {
                Identifier id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                this.entityData.set(DATA_FLUID_NAME, id != null ? id.toString() : "");
                this.entityData.set(DATA_FLUID_AMOUNT, stack.getAmount());
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.blockPosition(), this.tank, null);
            if (success) {
                this.syncFluidData();
                return InteractionResult.SUCCESS;
            }
        }

        // Show fluid tank status overlay
        FluidStack stored = getStoredFluid();
        Component msg;
        if (stored.isEmpty()) {
            msg = Component.literal("Liquid Cart: Empty (0 / " + TANK_CAPACITY + " mB)");
        } else {
            String fluidDesc = stored.getHoverName().getString();
            msg = Component.literal("Liquid Cart: " + fluidDesc + " (" + stored.getAmount() + " / " + TANK_CAPACITY + " mB)");
        }
        player.sendOverlayMessage(msg);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.LIQUID_MINECART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.LIQUID_MINECART_ITEM.get());
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.tank.serialize(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.tank.deserialize(input);
        syncFluidData();
    }
}
