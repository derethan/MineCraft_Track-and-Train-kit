package com.example.miningtools.block.entity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.CargoTransferBlock;
import com.example.miningtools.entity.AggregateHopperCartEntity;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class CargoTransferBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int BUFFER_SIZE = 9;
    public static final int BUFFER_CAPACITY = BUFFER_SIZE * 64; // 576 items
    private static final int[] SLOTS = IntStream.range(0, BUFFER_SIZE).toArray();

    // Transfer direction modes (matching FluidLoaderBlockEntity)
    public static final int MODE_UNLOAD = 0; // Cart -> Station Buffer
    public static final int MODE_LOAD = 1;   // Station Buffer -> Cart / Locomotive
    public static final int MODE_AUTO = 2;   // Bidirectional

    // Hold / Signal completion modes (matching FluidLoaderBlockEntity)
    public static final int REDSTONE_WHEN_EMPTY = 0; // Hold until cart is empty (0 matching items)
    public static final int REDSTONE_WHEN_FULL = 1;  // Hold until cart/loco is full (100%)
    public static final int REDSTONE_WHEN_DONE = 2;  // Partial / Idle: hold only while items actively move
    public static final int REDSTONE_DISABLED = 3;   // Never depower rail

    private final NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);

    private int transferMode;
    private int redstoneMode;
    private String itemWhitelist = "";
    private String itemBlacklist = "";
    private String cartWhitelist = "";
    private String cartBlacklist = "";

    private int transferCooldown = 0;
    private @Nullable UUID releasedCartId = null;
    private int releaseGraceTicks = 0;

    // Synced status for GUI gauges & labels
    private int stationItemCount = 0;
    private int cartItemCount = 0;
    private int cartItemCapacity = 0;
    private boolean cartDetected = false;
    private boolean holdingCart = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CargoTransferBlockEntity.this.stationItemCount;
                case 1 -> BUFFER_CAPACITY;
                case 2 -> CargoTransferBlockEntity.this.getTransferMode();
                case 3 -> CargoTransferBlockEntity.this.redstoneMode;
                case 4 -> CargoTransferBlockEntity.this.cartItemCount;
                case 5 -> CargoTransferBlockEntity.this.cartItemCapacity;
                case 6 -> CargoTransferBlockEntity.this.cartDetected ? 1 : 0;
                case 7 -> CargoTransferBlockEntity.this.holdingCart ? 1 : 0;
                case 8 -> CargoTransferBlockEntity.this.getStationRole();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> CargoTransferBlockEntity.this.setTransferMode(value);
                case 3 -> CargoTransferBlockEntity.this.setRedstoneMode(value);
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public CargoTransferBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.CARGO_TRANSFER_BLOCK_ENTITY.get(), pos, state);
        if (state.getBlock() instanceof CargoTransferBlock ctb) {
            if (ctb.isCoalFueler()) {
                this.transferMode = MODE_LOAD;
                this.redstoneMode = REDSTONE_WHEN_FULL;
            } else if (ctb.isUnloader()) {
                this.transferMode = MODE_UNLOAD;
                this.redstoneMode = REDSTONE_WHEN_EMPTY;
            } else {
                this.transferMode = MODE_LOAD;
                this.redstoneMode = REDSTONE_WHEN_FULL;
            }
        } else {
            this.transferMode = MODE_LOAD;
            this.redstoneMode = REDSTONE_WHEN_FULL;
        }
    }

    public int getStationRole() {
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof CargoTransferBlock ctb) {
            return ctb.getStationRole();
        }
        return CargoTransferBlock.ROLE_LOADER;
    }

    public boolean isUnloader() {
        return this.getStationRole() == CargoTransferBlock.ROLE_UNLOADER;
    }

    public boolean isCoalFueler() {
        return this.getStationRole() == CargoTransferBlock.ROLE_COAL_FUELER;
    }

    public int getTransferMode() {
        return this.isCoalFueler() ? MODE_LOAD : this.transferMode;
    }

    public void setTransferMode(int mode) {
        if (this.isCoalFueler()) {
            this.transferMode = MODE_LOAD;
        } else {
            int next = Math.floorMod(mode, 3);
            this.transferMode = next;
            if (next == MODE_UNLOAD && this.redstoneMode == REDSTONE_WHEN_FULL) {
                this.redstoneMode = REDSTONE_WHEN_EMPTY;
            } else if (next == MODE_LOAD && this.redstoneMode == REDSTONE_WHEN_EMPTY) {
                this.redstoneMode = REDSTONE_WHEN_FULL;
            }
        }
        this.setChanged();
    }

    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        if (this.isCoalFueler()) {
            int m = Math.floorMod(mode, 4);
            if (m == REDSTONE_WHEN_EMPTY) {
                m = REDSTONE_WHEN_FULL;
            }
            this.redstoneMode = m;
        } else {
            this.redstoneMode = Math.floorMod(mode, 4);
        }
        this.setChanged();
    }

    public String getItemWhitelist() {
        return this.itemWhitelist;
    }

    public void setItemWhitelist(String val) {
        this.itemWhitelist = val != null ? val.trim() : "";
        this.setChanged();
    }

    public String getItemBlacklist() {
        return this.itemBlacklist;
    }

    public void setItemBlacklist(String val) {
        this.itemBlacklist = val != null ? val.trim() : "";
        this.setChanged();
    }

    public String getCartWhitelist() {
        return this.cartWhitelist;
    }

    public void setCartWhitelist(String val) {
        this.cartWhitelist = val != null ? val.trim() : "";
        this.setChanged();
    }

    public String getCartBlacklist() {
        return this.cartBlacklist;
    }

    public void setCartBlacklist(String val) {
        this.cartBlacklist = val != null ? val.trim() : "";
        this.setChanged();
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return switch (this.getStationRole()) {
            case CargoTransferBlock.ROLE_COAL_FUELER -> Component.translatable("block.miningtools.coal_fueler");
            case CargoTransferBlock.ROLE_UNLOADER -> Component.translatable("block.miningtools.cargo_unloader");
            default -> Component.translatable("block.miningtools.cargo_loader");
        };
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CargoTransferMenu(containerId, inventory, this, this.dataAccess);
    }

    public static boolean isCoalFuelItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.COALS) || stack.is(Items.COAL_BLOCK) || com.example.miningtools.entity.BasicLocomotiveEntity.isBurnableFuel(stack));
    }

    public boolean matchesItemFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (this.isCoalFueler() && !isCoalFuelItem(stack)) {
            return false;
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String fullId = id != null ? id.toString().toLowerCase(Locale.ROOT) : "";
        String path = id != null ? id.getPath().toLowerCase(Locale.ROOT) : "";
        String display = stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);

        if (!this.itemBlacklist.isBlank()) {
            for (String token : this.itemBlacklist.split("[,;\\n]+")) {
                String t = token.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    if (fullId.contains(t) || path.contains(t) || display.contains(t)) {
                        return false;
                    }
                }
            }
        }

        if (!this.itemWhitelist.isBlank()) {
            boolean anyToken = false;
            for (String token : this.itemWhitelist.split("[,;\\n]+")) {
                String t = token.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    anyToken = true;
                    if (fullId.contains(t) || path.contains(t) || display.contains(t)) {
                        return true;
                    }
                }
            }
            if (anyToken) {
                return false;
            }
        }

        return true;
    }

    private static int getUsableCartSlotCount(Container cartContainer) {
        if (cartContainer instanceof BasicLocomotiveEntity) {
            return BasicLocomotiveEntity.INVENTORY_SIZE; // 3 coal slots
        }
        if (cartContainer instanceof TunnelBoreEntity) {
            return 1; // Slot 0 is fuel slot
        }
        if (cartContainer instanceof CargoMinecartEntity) {
            return CargoMinecartEntity.STORAGE_SLOTS;
        }
        if (cartContainer instanceof AggregateHopperCartEntity) {
            return AggregateHopperCartEntity.STORAGE_SLOTS;
        }
        return cartContainer.getContainerSize();
    }

    private static boolean canPlaceIntoTargetSlot(Container cartContainer, int slot, ItemStack stack, boolean coalFueler) {
        if (coalFueler) {
            if (cartContainer instanceof BasicLocomotiveEntity) {
                return slot >= 0 && slot < BasicLocomotiveEntity.INVENTORY_SIZE && stack.is(ItemTags.COALS);
            }
            if (cartContainer instanceof TunnelBoreEntity) {
                return slot == TunnelBoreEntity.FUEL_SLOT && (stack.is(ItemTags.COALS) || stack.is(Items.COAL_BLOCK));
            }
            return false;
        }
        return cartContainer.canPlaceItem(slot, stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CargoTransferBlockEntity be) {
        if (be.releaseGraceTicks > 0) {
            be.releaseGraceTicks--;
            if (be.releaseGraceTicks == 0) {
                be.releasedCartId = null;
            }
        }
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
        }

        // Update station buffer count for GUI gauge
        int bufCount = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack st = be.items.get(i);
            if (!st.isEmpty()) {
                bufCount += st.getCount();
            }
        }
        be.stationItemCount = bufCount;

        boolean coalFueler = be.isCoalFueler();
        int mode = be.getTransferMode();

        // Auto push/pull with container directly below (e.g., chest or hopper below station)
        if (level.getGameTime() % 4 == 0) {
            if (!coalFueler && mode == MODE_UNLOAD) {
                be.tryPushBufferToNeighbor(level, pos.below());
            } else {
                be.tryPullNeighborToBuffer(level, pos.below());
            }
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(1.1D, 0.65D, 1.1D);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> {
                    if (!c.isAlive() || c.isRemoved() || !(c instanceof Container)) {
                        return false;
                    }
                    if (coalFueler) {
                        // Dedicated Coaling Station ONLY targets coal-burning locomotives & tunnel bores!
                        return (c instanceof BasicLocomotiveEntity) || (c instanceof TunnelBoreEntity);
                    } else {
                        // Freight Cargo Loader & Unloader NEVER touch any locomotive or tunnel bore!
                        return !(c instanceof BasicLocomotiveEntity)
                                && !(c instanceof DieselLocomotiveEntity)
                                && !(c instanceof ElectricLocomotiveEntity)
                                && !(c instanceof TunnelBoreEntity)
                                && !(c instanceof MinecartFurnace);
                    }
                }
        );

        AbstractMinecart targetCart = null;
        for (AbstractMinecart candidate : carts) {
            if (be.releasedCartId != null && be.releasedCartId.equals(candidate.getUUID())) {
                continue;
            }
            if (TrainChainingUtil.matchesCartNameFilter(candidate, be.cartWhitelist, be.cartBlacklist)) {
                targetCart = candidate;
                break;
            }
        }

        if (targetCart == null) {
            be.cartDetected = false;
            be.holdingCart = false;
            be.cartItemCount = 0;
            be.cartItemCapacity = 0;
            setRailPowerState(level, pos, state, true);
            return;
        }

        be.cartDetected = true;
        Container cartContainer = (Container) targetCart;
        int slotLimit = getUsableCartSlotCount(cartContainer);

        // Compute cart item count, capacity, and transfer readiness
        int totalItems = 0;
        int totalCap = Math.max(1, slotLimit * 64);
        boolean hasMatchingItemsInCart = false;
        boolean canUnloadMoveNow = false;
        boolean isCartCompletelyFull = true;
        boolean canLoadMoveNow = false;

        for (int i = 0; i < slotLimit; i++) {
            ItemStack st = cartContainer.getItem(i);
            if (st.isEmpty()) {
                isCartCompletelyFull = false;
            } else {
                totalItems += st.getCount();
                if (st.getCount() < Math.min(st.getMaxStackSize(), cartContainer.getMaxStackSize())) {
                    isCartCompletelyFull = false;
                }
                if (!coalFueler && be.matchesItemFilter(st)) {
                    hasMatchingItemsInCart = true;
                    if (be.canInsertIntoBuffer(st)) {
                        canUnloadMoveNow = true;
                    }
                }
            }
        }

        be.cartItemCount = totalItems;
        be.cartItemCapacity = totalCap;

        boolean doUnload = !coalFueler && (mode == MODE_UNLOAD || (mode == MODE_AUTO && hasMatchingItemsInCart && totalItems > 0));
        boolean doLoad = coalFueler || mode == MODE_LOAD || (mode == MODE_AUTO && !doUnload);

        if (doLoad) {
            for (int b = 0; b < BUFFER_SIZE; b++) {
                ItemStack bufStack = be.items.get(b);
                if (!bufStack.isEmpty() && be.matchesItemFilter(bufStack)) {
                    if (canInsertIntoCart(cartContainer, slotLimit, bufStack, coalFueler)) {
                        canLoadMoveNow = true;
                        break;
                    }
                }
            }
        }

        // Perform item transfer if cooldown ready
        boolean movedThisCycle = false;
        if (be.transferCooldown <= 0) {
            if (doUnload && canUnloadMoveNow) {
                movedThisCycle = be.transferFromCartToBuffer(cartContainer, slotLimit);
            } else if (doLoad && canLoadMoveNow) {
                movedThisCycle = be.transferFromBufferToCart(cartContainer, slotLimit, coalFueler);
                if (movedThisCycle && targetCart instanceof BasicLocomotiveEntity loco && !loco.hasFuel()) {
                    loco.tryRefuel();
                }
            }
            if (movedThisCycle) {
                be.transferCooldown = 4; // 4 ticks per batch (up to 8 items)
                be.setChanged();
            }
        }

        // Re-evaluate cart counts after transfer
        totalItems = 0;
        hasMatchingItemsInCart = false;
        canUnloadMoveNow = false;
        isCartCompletelyFull = true;
        canLoadMoveNow = false;

        for (int i = 0; i < slotLimit; i++) {
            ItemStack st = cartContainer.getItem(i);
            if (st.isEmpty()) {
                isCartCompletelyFull = false;
            } else {
                totalItems += st.getCount();
                if (st.getCount() < Math.min(st.getMaxStackSize(), cartContainer.getMaxStackSize())) {
                    isCartCompletelyFull = false;
                }
                if (!coalFueler && be.matchesItemFilter(st)) {
                    hasMatchingItemsInCart = true;
                    if (be.canInsertIntoBuffer(st)) {
                        canUnloadMoveNow = true;
                    }
                }
            }
        }
        if (doLoad) {
            for (int b = 0; b < BUFFER_SIZE; b++) {
                ItemStack bufStack = be.items.get(b);
                if (!bufStack.isEmpty() && be.matchesItemFilter(bufStack)) {
                    if (canInsertIntoCart(cartContainer, slotLimit, bufStack, coalFueler)) {
                        canLoadMoveNow = true;
                        break;
                    }
                }
            }
        }
        be.cartItemCount = totalItems;

        boolean shouldHoldCart = false;
        if (be.redstoneMode != REDSTONE_DISABLED) {
            if (be.redstoneMode == REDSTONE_WHEN_EMPTY) {
                shouldHoldCart = hasMatchingItemsInCart;
            } else if (be.redstoneMode == REDSTONE_WHEN_FULL) {
                shouldHoldCart = !isCartCompletelyFull;
            } else if (be.redstoneMode == REDSTONE_WHEN_DONE) {
                shouldHoldCart = doUnload ? canUnloadMoveNow : canLoadMoveNow;
            }
        }

        if (shouldHoldCart) {
            be.holdingCart = true;
            setRailPowerState(level, pos, state, false);
            TrainChainingUtil.freezeConsistFromBrakingRail(targetCart);
        } else {
            boolean wasHolding = be.holdingCart || !state.getValue(CargoTransferBlock.POWERED);
            be.holdingCart = false;
            be.releasedCartId = targetCart.getUUID();
            be.releaseGraceTicks = 35;
            setRailPowerState(level, pos, state, true);
            if (wasHolding) {
                TrainChainingUtil.releaseConsistFromBrakingRail(targetCart);
            }
        }
    }

    private static void setRailPowerState(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(CargoTransferBlock.POWERED) && state.getValue(CargoTransferBlock.POWERED) != powered) {
            BlockState newState = state.setValue(CargoTransferBlock.POWERED, powered);
            level.setBlock(pos, newState, 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }

    private boolean canInsertIntoBuffer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack slot = this.items.get(i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canInsertIntoCart(Container cartContainer, int slotLimit, ItemStack stack, boolean coalFueler) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < slotLimit; i++) {
            if (!canPlaceIntoTargetSlot(cartContainer, i, stack, coalFueler)) continue;
            ItemStack slot = cartContainer.getItem(i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < Math.min(slot.getMaxStackSize(), cartContainer.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private boolean transferFromCartToBuffer(Container cartContainer, int slotLimit) {
        int remainingToMove = 8;
        boolean movedAny = false;

        for (int i = 0; i < slotLimit && remainingToMove > 0; i++) {
            ItemStack inCart = cartContainer.getItem(i);
            if (inCart.isEmpty() || !this.matchesItemFilter(inCart)) continue;

            int toAttempt = Math.min(remainingToMove, inCart.getCount());
            ItemStack probe = inCart.copyWithCount(toAttempt);
            int inserted = insertIntoBuffer(probe);
            if (inserted > 0) {
                cartContainer.removeItem(i, inserted);
                cartContainer.setChanged();
                remainingToMove -= inserted;
                movedAny = true;
            }
        }
        return movedAny;
    }

    private boolean transferFromBufferToCart(Container cartContainer, int slotLimit, boolean coalFueler) {
        int remainingToMove = 8;
        boolean movedAny = false;

        for (int b = 0; b < BUFFER_SIZE && remainingToMove > 0; b++) {
            ItemStack inBuf = this.items.get(b);
            if (inBuf.isEmpty() || !this.matchesItemFilter(inBuf)) continue;

            int toAttempt = Math.min(remainingToMove, inBuf.getCount());
            ItemStack probe = inBuf.copyWithCount(toAttempt);
            int inserted = insertIntoCartContainer(cartContainer, slotLimit, probe, coalFueler);
            if (inserted > 0) {
                inBuf.shrink(inserted);
                if (inBuf.isEmpty()) {
                    this.items.set(b, ItemStack.EMPTY);
                }
                cartContainer.setChanged();
                remainingToMove -= inserted;
                movedAny = true;
            }
        }
        return movedAny;
    }

    private int insertIntoBuffer(ItemStack stack) {
        int original = stack.getCount();
        ItemStack rem = stack.copy();

        // Pass 1: merge into existing stacks
        for (int i = 0; i < BUFFER_SIZE && !rem.isEmpty(); i++) {
            ItemStack slot = this.items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rem)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space > 0) {
                    int add = Math.min(space, rem.getCount());
                    slot.grow(add);
                    rem.shrink(add);
                }
            }
        }
        // Pass 2: empty slots
        for (int i = 0; i < BUFFER_SIZE && !rem.isEmpty(); i++) {
            ItemStack slot = this.items.get(i);
            if (slot.isEmpty()) {
                this.items.set(i, rem.copy());
                rem.setCount(0);
                break;
            }
        }
        return original - rem.getCount();
    }

    private static int insertIntoCartContainer(Container cartContainer, int slotLimit, ItemStack stack, boolean coalFueler) {
        int original = stack.getCount();
        ItemStack rem = stack.copy();

        // Pass 1: merge into existing stacks
        for (int i = 0; i < slotLimit && !rem.isEmpty(); i++) {
            if (!canPlaceIntoTargetSlot(cartContainer, i, rem, coalFueler)) continue;
            ItemStack slot = cartContainer.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rem)) {
                int max = Math.min(slot.getMaxStackSize(), cartContainer.getMaxStackSize());
                int space = max - slot.getCount();
                if (space > 0) {
                    int add = Math.min(space, rem.getCount());
                    ItemStack updated = slot.copy();
                    updated.grow(add);
                    cartContainer.setItem(i, updated);
                    rem.shrink(add);
                }
            }
        }
        // Pass 2: empty slots
        for (int i = 0; i < slotLimit && !rem.isEmpty(); i++) {
            if (!canPlaceIntoTargetSlot(cartContainer, i, rem, coalFueler)) continue;
            ItemStack slot = cartContainer.getItem(i);
            if (slot.isEmpty()) {
                int max = Math.min(rem.getMaxStackSize(), cartContainer.getMaxStackSize());
                int add = Math.min(max, rem.getCount());
                cartContainer.setItem(i, rem.copyWithCount(add));
                rem.shrink(add);
            }
        }
        return original - rem.getCount();
    }

    private void tryPushBufferToNeighbor(Level level, BlockPos targetPos) {
        BlockEntity neighborBe = level.getBlockEntity(targetPos);
        if (neighborBe instanceof Container neighborContainer) {
            int limit = neighborContainer.getContainerSize();
            for (int b = 0; b < BUFFER_SIZE; b++) {
                ItemStack inBuf = this.items.get(b);
                if (inBuf.isEmpty()) continue;
                int toMove = Math.min(4, inBuf.getCount());
                int inserted = insertIntoCartContainer(neighborContainer, limit, inBuf.copyWithCount(toMove), false);
                if (inserted > 0) {
                    inBuf.shrink(inserted);
                    if (inBuf.isEmpty()) this.items.set(b, ItemStack.EMPTY);
                    neighborContainer.setChanged();
                    this.setChanged();
                    return;
                }
            }
        }
    }

    private void tryPullNeighborToBuffer(Level level, BlockPos sourcePos) {
        BlockEntity neighborBe = level.getBlockEntity(sourcePos);
        if (neighborBe instanceof Container neighborContainer) {
            int limit = neighborContainer.getContainerSize();
            for (int i = 0; i < limit; i++) {
                ItemStack st = neighborContainer.getItem(i);
                if (st.isEmpty() || !this.matchesItemFilter(st)) continue;
                int toMove = Math.min(4, st.getCount());
                int inserted = this.insertIntoBuffer(st.copyWithCount(toMove));
                if (inserted > 0) {
                    neighborContainer.removeItem(i, inserted);
                    neighborContainer.setChanged();
                    this.setChanged();
                    return;
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        return BUFFER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack st : this.items) {
            if (!st.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return this.matchesItemFilter(itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return !this.isCoalFueler();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("TransferMode", this.getTransferMode());
        output.putInt("RedstoneMode", this.redstoneMode);
        output.putString("ItemWhitelist", this.itemWhitelist);
        output.putString("ItemBlacklist", this.itemBlacklist);
        output.putString("CartWhitelist", this.cartWhitelist);
        output.putString("CartBlacklist", this.cartBlacklist);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        int defaultTransfer = (this.isCoalFueler() || !this.isUnloader()) ? MODE_LOAD : MODE_UNLOAD;
        int defaultRedstone = (this.isCoalFueler() || !this.isUnloader()) ? REDSTONE_WHEN_FULL : REDSTONE_WHEN_EMPTY;
        this.transferMode = this.isCoalFueler() ? MODE_LOAD : input.getIntOr("TransferMode", defaultTransfer);
        this.redstoneMode = input.getIntOr("RedstoneMode", defaultRedstone);
        this.itemWhitelist = input.getStringOr("ItemWhitelist", "");
        this.itemBlacklist = input.getStringOr("ItemBlacklist", "");
        this.cartWhitelist = input.getStringOr("CartWhitelist", "");
        this.cartBlacklist = input.getStringOr("CartBlacklist", "");
    }
}
