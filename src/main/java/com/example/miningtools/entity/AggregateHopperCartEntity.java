package com.example.miningtools.entity;

import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.AggregateHopperCartMenu;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class AggregateHopperCartEntity extends AbstractMinecartContainer implements WorldlyContainer {

    public enum AggregateType {
        NONE,
        COAL,
        GRAVEL,
        SAND,
        RED_SAND,
        DIRT,
        RAW_ORE
    }

    public static final TagKey<Item> C_ORES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));
    public static final TagKey<Item> C_RAW_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "raw_materials"));

    public static final int STORAGE_SLOTS = 27;
    public static final int FILTER_SLOT = 27;
    public static final int TOTAL_SLOTS = 28;

    private static final int[] STORAGE_SLOT_INDICES = IntStream.range(0, STORAGE_SLOTS).toArray();

    private static final EntityDataAccessor<ItemStack> DATA_FILTER_ITEM =
            SynchedEntityData.defineId(AggregateHopperCartEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_STORED_COUNT =
            SynchedEntityData.defineId(AggregateHopperCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_AGGREGATE_TYPE =
            SynchedEntityData.defineId(AggregateHopperCartEntity.class, EntityDataSerializers.STRING);

    public AggregateHopperCartEntity(EntityType<? extends AggregateHopperCartEntity> type, Level level) {
        super(type, level);
    }

    public AggregateHopperCartEntity(Level level, double x, double y, double z) {
        super(MiningTools.HOPPER_CART_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FILTER_ITEM, ItemStack.EMPTY);
        builder.define(DATA_STORED_COUNT, 0);
        builder.define(DATA_AGGREGATE_TYPE, AggregateType.NONE.name());
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.HOPPER_CART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.HOPPER_CART_ITEM.get());
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 0;
    }

    public ItemStack getFilterItem() {
        return this.entityData.get(DATA_FILTER_ITEM);
    }

    public void setFilterItem(ItemStack stack) {
        this.entityData.set(DATA_FILTER_ITEM, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        this.updateStoredCountAndType();
    }

    public AggregateType getAggregateType() {
        try {
            return AggregateType.valueOf(this.entityData.get(DATA_AGGREGATE_TYPE));
        } catch (Throwable ignored) {
            return AggregateType.NONE;
        }
    }

    public int getStoredCount() {
        return this.entityData.get(DATA_STORED_COUNT);
    }

    public static AggregateType detectType(ItemStack stack) {
        if (stack.isEmpty()) {
            return AggregateType.NONE;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return AggregateType.NONE;
        }
        String path = id.getPath().toLowerCase();

        if (stack.is(ItemTags.COALS) || path.contains("coal") || path.contains("charcoal")) {
            return AggregateType.COAL;
        }
        if (path.contains("red_sand")) {
            return AggregateType.RED_SAND;
        }
        if (path.contains("sand") || path.contains("sandstone")) {
            return AggregateType.SAND;
        }
        if (path.contains("gravel") || path.contains("flint") || path.contains("cobble")
                || path.contains("andesite") || path.contains("diorite") || path.contains("granite")
                || path.contains("tuff") || path.contains("basalt") || path.contains("stone")) {
            return AggregateType.GRAVEL;
        }
        if (path.contains("dirt") || path.contains("mud") || path.contains("clay") || path.contains("podzol")) {
            return AggregateType.DIRT;
        }
        if (stack.is(C_RAW_MATERIALS) || stack.is(C_ORES) || path.contains("raw_") || path.contains("ore")) {
            return AggregateType.RAW_ORE;
        }
        return AggregateType.GRAVEL;
    }

    public boolean hasStoredItems() {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (!this.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasConflictingItems(ItemStack filterStack) {
        if (filterStack.isEmpty()) {
            return false;
        }
        AggregateType targetType = detectType(filterStack);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack inCart = this.getItem(i);
            if (!inCart.isEmpty() && detectType(inCart) != targetType) {
                return true;
            }
        }
        return false;
    }

    public void updateStoredCountAndType() {
        int count = 0;
        AggregateType dominantType = AggregateType.NONE;

        ItemStack filter = getFilterItem();
        if (!filter.isEmpty()) {
            dominantType = detectType(filter);
        }

        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                count += stack.getCount();
                if (dominantType == AggregateType.NONE) {
                    dominantType = detectType(stack);
                }
            }
        }

        this.entityData.set(DATA_STORED_COUNT, count);
        this.entityData.set(DATA_AGGREGATE_TYPE, count > 0 ? dominantType.name() : (filter.isEmpty() ? AggregateType.NONE.name() : dominantType.name()));
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == FILTER_SLOT) {
            return true;
        }

        ItemStack filter = getFilterItem();
        if (!filter.isEmpty()) {
            return detectType(stack) == detectType(filter);
        }

        if (hasStoredItems()) {
            AggregateType current = getAggregateType();
            return current == AggregateType.NONE || detectType(stack) == current;
        }

        return true;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        if (index == FILTER_SLOT) {
            setFilterItem(stack);
        } else {
            updateStoredCountAndType();
        }
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = super.removeItem(index, count);
        if (!result.isEmpty()) {
            if (index == FILTER_SLOT) {
                setFilterItem(this.getItem(FILTER_SLOT));
            } else {
                updateStoredCountAndType();
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack result = super.removeItemNoUpdate(index);
        if (!result.isEmpty()) {
            if (index == FILTER_SLOT) {
                setFilterItem(ItemStack.EMPTY);
            } else {
                updateStoredCountAndType();
            }
        }
        return result;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }
        return this.interactWithContainerVehicle(player);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new AggregateHopperCartMenu(id, playerInventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STORAGE_SLOT_INDICES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return index < STORAGE_SLOTS && canPlaceItem(index, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index < STORAGE_SLOTS;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFilterItem(this.getItem(FILTER_SLOT));
        updateStoredCountAndType();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
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

    public BlockPos getCurrentBlockPosOrRailBelow() {
        return TrainChainingUtil.getCurrentBlockPosOrRailBelow(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            updateStoredCountAndType();
        }
    }
}
