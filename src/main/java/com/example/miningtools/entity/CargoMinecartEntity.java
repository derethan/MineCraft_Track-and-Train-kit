package com.example.miningtools.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import net.minecraft.core.BlockPos;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.CargoMinecartMenu;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

public class CargoMinecartEntity extends AbstractMinecartContainer implements WorldlyContainer {
    public enum FilterType {
        NONE,
        LOG,
        WOOD,
        ORE,
        INGOT
    }

    public static final TagKey<Item> C_ORES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));
    public static final TagKey<Item> C_RAW_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "raw_materials"));
    public static final TagKey<Item> C_INGOTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots"));
    public static final TagKey<Item> C_NUGGETS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "nuggets"));

    public static final int STORAGE_SLOTS = 27;
    public static final int FILTER_SLOT = 27;
    public static final int TOTAL_SLOTS = 28;

    private static final int[] STORAGE_SLOT_INDICES = IntStream.range(0, STORAGE_SLOTS).toArray();

    private static final EntityDataAccessor<ItemStack> DATA_FILTER_ITEM =
            SynchedEntityData.defineId(CargoMinecartEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_STORED_COUNT =
            SynchedEntityData.defineId(CargoMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_CARGO_TYPE =
            SynchedEntityData.defineId(CargoMinecartEntity.class, EntityDataSerializers.STRING);

    public CargoMinecartEntity(EntityType<? extends CargoMinecartEntity> type, Level level) {
        super(type, level);
    }

    public CargoMinecartEntity(Level level, double x, double y, double z) {
        super(MiningTools.CARGO_MINECART_ENTITY.get(), level);
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
        builder.define(DATA_CARGO_TYPE, FilterType.NONE.name());
    }

    @Override
    protected Item getDropItem() {
        return MiningTools.CARGO_MINECART_ITEM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MiningTools.CARGO_MINECART_ITEM.get());
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
        if (!this.level().isClientSide()) {
            this.updateStoredCount();
        }
    }

    public FilterType getFilterType() {
        ItemStack filter = getFilterItem();
        if (filter.isEmpty()) {
            return FilterType.NONE;
        }
        if (isLogItem(filter)) {
            return FilterType.LOG;
        }
        if (isPlankOrWoodItem(filter)) {
            return FilterType.WOOD;
        }
        if (isOreItem(filter)) {
            return FilterType.ORE;
        }
        if (isIngotOrNuggetItem(filter)) {
            return FilterType.INGOT;
        }
        return FilterType.NONE;
    }

    public FilterType getEffectiveCargoType() {
        try {
            return FilterType.valueOf(this.entityData.get(DATA_CARGO_TYPE));
        } catch (Throwable ignored) {
            return FilterType.NONE;
        }
    }

    public int getStoredCount() {
        return this.entityData.get(DATA_STORED_COUNT);
    }

    public boolean isLogFilterActive() {
        return getFilterType() == FilterType.LOG;
    }

    public boolean isWoodFilterActive() {
        return getFilterType() == FilterType.WOOD;
    }

    public boolean isOreFilterActive() {
        return getFilterType() == FilterType.ORE;
    }

    public boolean isIngotFilterActive() {
        return getFilterType() == FilterType.INGOT;
    }

    /**
     * Checks if an item is a true Log (tree trunks, logs, stems).
     */
    public static boolean isLogItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.LOGS)) {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
            return !path.endsWith("_wood") && !path.endsWith("_hyphae");
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        return path.endsWith("_log") || path.contains("_log_") || path.contains("stem");
    }

    /**
     * Checks if an item is Planks, wooden slabs, or 6-sided bark wood.
     */
    public static boolean isPlankOrWoodItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_SLABS)) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase();
        if (path.contains("plank") || path.contains("lumber")) {
            return true;
        }
        if (stack.is(ItemTags.LOGS) && (path.endsWith("_wood") || path.endsWith("_hyphae"))) {
            return true;
        }
        return false;
    }

    public static boolean isWoodItem(ItemStack stack) {
        return isPlankOrWoodItem(stack);
    }

    /**
     * Checks if an item is an Ore block or Raw metal chunk (including tin).
     */
    public static boolean isOreItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(C_ORES) || stack.is(C_RAW_MATERIALS)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        if (path.contains("tin") && (path.contains("ore") || path.contains("raw") || path.contains("block"))) {
            return true;
        }
        return path.endsWith("_ore") || path.startsWith("raw_") || path.contains("_raw_");
    }

    /**
     * Checks if an item is a refined Ingot or Nugget.
     */
    public static boolean isIngotOrNuggetItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(C_INGOTS) || stack.is(C_NUGGETS)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        return path.endsWith("_ingot") || path.endsWith("_nugget");
    }

    public static boolean isValidFilterCandidate(ItemStack stack) {
        return isLogItem(stack) || isPlankOrWoodItem(stack) || isOreItem(stack) || isIngotOrNuggetItem(stack);
    }

    /**
     * Checks if the 27 storage slots contain any items.
     */
    public boolean hasStoredItems() {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (!this.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the 27 storage slots contain any items that conflict with the given filter.
     */
    public boolean hasConflictingItems(ItemStack filterCandidate) {
        if (filterCandidate.isEmpty()) {
            return false;
        }
        if (isLogItem(filterCandidate)) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stored = this.getItem(i);
                if (!stored.isEmpty() && !isLogItem(stored)) {
                    return true;
                }
            }
        } else if (isPlankOrWoodItem(filterCandidate)) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stored = this.getItem(i);
                if (!stored.isEmpty() && !isPlankOrWoodItem(stored)) {
                    return true;
                }
            }
        } else if (isOreItem(filterCandidate)) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stored = this.getItem(i);
                if (!stored.isEmpty() && !isOreItem(stored)) {
                    return true;
                }
            }
        } else if (isIngotOrNuggetItem(filterCandidate)) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stored = this.getItem(i);
                if (!stored.isEmpty() && !isIngotOrNuggetItem(stored)) {
                    return true;
                }
            }
        }
        return false;
    }

    public FilterType calculateEffectiveCargoType() {
        ItemStack filter = getFilterItem();
        if (!filter.isEmpty()) {
            if (isLogItem(filter)) return FilterType.LOG;
            if (isPlankOrWoodItem(filter)) return FilterType.WOOD;
            if (isOreItem(filter)) return FilterType.ORE;
            if (isIngotOrNuggetItem(filter)) return FilterType.INGOT;
            return FilterType.NONE;
        }

        // When no filter is set, dynamically inspect stored contents:
        int nonEmptyCount = 0;
        boolean allLogs = true;
        boolean allPlanks = true;
        boolean allOres = true;
        boolean allIngots = true;

        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                nonEmptyCount++;
                if (!isLogItem(stack)) allLogs = false;
                if (!isPlankOrWoodItem(stack)) allPlanks = false;
                if (!isOreItem(stack)) allOres = false;
                if (!isIngotOrNuggetItem(stack)) allIngots = false;
            }
        }

        if (nonEmptyCount == 0) {
            return FilterType.NONE;
        }
        if (allLogs) {
            return FilterType.LOG;
        }
        if (allPlanks) {
            return FilterType.WOOD;
        }
        if (allOres) {
            return FilterType.ORE;
        }
        if (allIngots) {
            return FilterType.INGOT;
        }
        // Mixed or general cargo
        return FilterType.NONE;
    }

    public void updateStoredCount() {
        FilterType effective = calculateEffectiveCargoType();
        this.entityData.set(DATA_CARGO_TYPE, effective.name());

        int count = 0;
        FilterType filterType = getFilterType();
        if (filterType == FilterType.LOG) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = this.getItem(i);
                if (isLogItem(stack)) {
                    count += stack.getCount();
                }
            }
        } else if (filterType == FilterType.WOOD) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = this.getItem(i);
                if (isPlankOrWoodItem(stack)) {
                    count += stack.getCount();
                }
            }
        } else if (filterType == FilterType.ORE) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = this.getItem(i);
                if (isOreItem(stack)) {
                    count += stack.getCount();
                }
            }
        } else if (filterType == FilterType.INGOT) {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = this.getItem(i);
                if (isIngotOrNuggetItem(stack)) {
                    count += stack.getCount();
                }
            }
        } else {
            // Count all items in storage
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = this.getItem(i);
                if (!stack.isEmpty()) {
                    count += stack.getCount();
                }
            }
        }
        this.entityData.set(DATA_STORED_COUNT, count);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Block removing/overwriting the filter when storage has items
        if (slot == FILTER_SLOT && hasStoredItems() && !getFilterItem().isEmpty() && stack.isEmpty()) {
            return;
        }
        super.setItem(slot, stack);
        if (slot == FILTER_SLOT) {
            setFilterItem(stack);
        }
        if (!this.level().isClientSide()) {
            updateStoredCount();
        }
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        // Filter cannot be removed while items are in storage
        if (slot == FILTER_SLOT && hasStoredItems()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = super.removeItem(slot, count);
        if (slot == FILTER_SLOT) {
            setFilterItem(this.getItem(FILTER_SLOT));
        }
        if (!this.level().isClientSide()) {
            updateStoredCount();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // Filter cannot be removed while items are in storage
        if (slot == FILTER_SLOT && hasStoredItems()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = super.removeItemNoUpdate(slot);
        if (slot == FILTER_SLOT) {
            setFilterItem(this.getItem(FILTER_SLOT));
        }
        if (!this.level().isClientSide()) {
            updateStoredCount();
        }
        return result;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FILTER_SLOT) {
            if (!isValidFilterCandidate(stack)) {
                return false;
            }
            if (hasStoredItems() && !getFilterItem().isEmpty()) {
                return false;
            }
            return !hasConflictingItems(stack);
        }
        if (slot >= 0 && slot < STORAGE_SLOTS) {
            FilterType filterType = getFilterType();
            if (filterType == FilterType.LOG) {
                return isLogItem(stack);
            } else if (filterType == FilterType.WOOD) {
                return isWoodItem(stack);
            } else if (filterType == FilterType.ORE) {
                return isOreItem(stack);
            } else if (filterType == FilterType.INGOT) {
                return isIngotOrNuggetItem(stack);
            }
            return true;
        }
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!this.level().isClientSide()) {
            this.updateStoredCount();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        // Automation / hoppers can only access storage slots 0..26, never the filter slot
        return STORAGE_SLOT_INDICES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < STORAGE_SLOTS && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < STORAGE_SLOTS;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CargoMinecartMenu(containerId, inventory, this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getItemInHand(hand).is(MiningTools.CROWBAR_ITEM.get())) {
            return InteractionResult.PASS;
        }
        return this.interactWithContainerVehicle(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFilterItem(this.getItem(FILTER_SLOT));
        updateStoredCount();
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