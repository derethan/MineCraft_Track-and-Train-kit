package com.example.miningtools.entity;

import net.minecraft.core.BlockPos;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;

public class LargeLiquidMinecartEntity extends LiquidMinecartEntity {
    public static final int LARGE_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME; // 16,000 mB (16 Buckets)

    public LargeLiquidMinecartEntity(EntityType<? extends LargeLiquidMinecartEntity> type, Level level) {
        super(type, level);
    }

    public LargeLiquidMinecartEntity(Level level, double x, double y, double z) {
        super(MiningTools.LARGE_LIQUID_MINECART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public int getCapacity() {
        return LARGE_TANK_CAPACITY;
    }

    @Override
    public String getCartDisplayName() {
        return "Large Liquid Minecart";
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.LARGE_LIQUID_MINECART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.LARGE_LIQUID_MINECART_ITEM.get());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (TrainChainingUtil.isConnectedInSameTrain(this, entity)) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    @Override
    public void push(Entity entity) {
        if (TrainChainingUtil.isConnectedInSameTrain(this, entity)) {
            return;
        }
        super.push(entity);
    }

    @Override
    public BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }
}