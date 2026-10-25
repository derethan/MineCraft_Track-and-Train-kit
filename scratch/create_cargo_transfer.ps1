$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. CargoTransferBlock.java
$blockPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\CargoTransferBlock.java"
$blockContent = @'
package com.example.miningtools.block;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cargo Loader & Cargo Unloader block.
 * Placed underneath a Braking/Holding Rail (or Control/Powered Rail).
 * Emits redstone power (POWERED = true) by default when idle or finished so trains pass freely.
 * Depowers (POWERED = false) when holding a matching cart for item loading or unloading.
 */
public class CargoTransferBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private final boolean unloader;

    public CargoTransferBlock(boolean unloader, BlockBehaviour.Properties properties) {
        super(properties);
        this.unloader = unloader;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, true));
    }

    public boolean isUnloader() {
        return this.unloader;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, true);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoTransferBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type == MiningTools.CARGO_TRANSFER_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> CargoTransferBlockEntity.serverTick(lvl, pos, st, (CargoTransferBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(transferBe, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeBoolean(transferBe.isUnloader());
                    buf.writeVarInt(transferBe.getLoadConditionMode());
                    buf.writeUtf(transferBe.getItemWhitelist(), 128);
                    buf.writeUtf(transferBe.getItemBlacklist(), 128);
                    buf.writeUtf(transferBe.getCartWhitelist(), 128);
                    buf.writeUtf(transferBe.getCartBlacklist(), 128);
                });
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        if (!state.is(state.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe) {
                Containers.dropContents(level, pos, transferBe.getItems());
                level.updateNeighborsAt(pos, this);
                level.updateNeighborsAt(pos.above(), this);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
}
'@
[System.IO.File]::WriteAllText($blockPath, $blockContent, $utf8NoBom)
Write-Host "Created CargoTransferBlock.java"

# 2. CargoTransferBlockEntity.java
$bePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\CargoTransferBlockEntity.java"
$beContent = @'
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
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.util.TrainChainingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class CargoTransferBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int BUFFER_SIZE = 9;
    private static final int[] SLOTS = IntStream.range(0, BUFFER_SIZE).toArray();

    // Load / Unload completion condition modes
    public static final int CONDITION_FULL_COMPLETE = 0; // Unloader: wait until cart is completely empty; Loader: wait until cart is completely full
    public static final int CONDITION_PARTIAL_IDLE = 1;  // Hold only while items can actively transfer; release as soon as transfer finishes or buffer/cart is full/empty

    private final NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);

    private int loadConditionMode = CONDITION_FULL_COMPLETE;
    private String itemWhitelist = "";
    private String itemBlacklist = "";
    private String cartWhitelist = "";
    private String cartBlacklist = "";

    private int transferCooldown = 0;
    private @Nullable UUID releasedCartId = null;
    private int releaseGraceTicks = 0;

    // Synced status for GUI
    private boolean cartDetected = false;
    private boolean holdingCart = false;
    private int cartFillPct = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CargoTransferBlockEntity.this.loadConditionMode;
                case 1 -> CargoTransferBlockEntity.this.cartDetected ? 1 : 0;
                case 2 -> CargoTransferBlockEntity.this.holdingCart ? 1 : 0;
                case 3 -> CargoTransferBlockEntity.this.cartFillPct;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CargoTransferBlockEntity.this.setLoadConditionMode(value);
                case 1 -> CargoTransferBlockEntity.this.cartDetected = (value != 0);
                case 2 -> CargoTransferBlockEntity.this.holdingCart = (value != 0);
                case 3 -> CargoTransferBlockEntity.this.cartFillPct = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CargoTransferBlockEntity(BlockPos pos, BlockState state) {
        super(MiningTools.CARGO_TRANSFER_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isUnloader() {
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof CargoTransferBlock ctb) {
            return ctb.isUnloader();
        }
        return state.is(MiningTools.CARGO_UNLOADER.get());
    }

    public int getLoadConditionMode() {
        return this.loadConditionMode;
    }

    public void setLoadConditionMode(int mode) {
        this.loadConditionMode = Math.floorMod(mode, 2);
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
        return Component.translatable(this.isUnloader()
                ? "block.miningtools.cargo_unloader"
                : "block.miningtools.cargo_loader");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CargoTransferMenu(
                containerId,
                inventory,
                this,
                this.dataAccess,
                this.getBlockPos(),
                this.isUnloader(),
                this.loadConditionMode,
                this.itemWhitelist,
                this.itemBlacklist,
                this.cartWhitelist,
                this.cartBlacklist
        );
    }

    public boolean matchesItemFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;

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
        if (cartContainer instanceof CargoMinecartEntity) {
            return CargoMinecartEntity.STORAGE_SLOTS;
        }
        if (cartContainer instanceof AggregateHopperCartEntity) {
            return AggregateHopperCartEntity.STORAGE_SLOTS;
        }
        return cartContainer.getContainerSize();
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

        boolean unloader = be.isUnloader();

        // Auto push/pull with container directly below (e.g., chest or hopper below station)
        if (level.getGameTime() % 4 == 0) {
            if (unloader) {
                be.tryPushBufferToNeighbor(level, pos.below());
            } else {
                be.tryPullNeighborToBuffer(level, pos.below());
            }
        }

        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.95D, 0.65D, 0.95D);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(
                AbstractMinecart.class,
                trackBox,
                c -> c.isAlive()
                        && !c.isRemoved()
                        && c instanceof Container
                        && !(c instanceof BasicLocomotiveEntity)
                        && !(c instanceof DieselLocomotiveEntity)
                        && !(c instanceof ElectricLocomotiveEntity)
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
            be.cartFillPct = 0;
            // Ensure rail above stays powered when idle so incoming trains can enter
            setRailPowerState(level, pos, state, true);
            return;
        }

        be.cartDetected = true;
        Container cartContainer = (Container) targetCart;
        int slotLimit = getUsableCartSlotCount(cartContainer);

        // Compute fill percentage and transfer readiness
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
                if (be.matchesItemFilter(st)) {
                    hasMatchingItemsInCart = true;
                    if (be.canInsertIntoBuffer(st)) {
                        canUnloadMoveNow = true;
                    }
                }
            }
        }

        if (!unloader) {
            for (int b = 0; b < BUFFER_SIZE; b++) {
                ItemStack bufStack = be.items.get(b);
                if (!bufStack.isEmpty() && be.matchesItemFilter(bufStack)) {
                    if (canInsertIntoCart(cartContainer, slotLimit, bufStack)) {
                        canLoadMoveNow = true;
                        break;
                    }
                }
            }
        }

        be.cartFillPct = Math.clamp((totalItems * 100) / totalCap, 0, 100);

        boolean shouldHoldCart;
        if (unloader) {
            if (be.loadConditionMode == CONDITION_FULL_COMPLETE) {
                // Hold cart until all matching items are unloaded from the cart
                shouldHoldCart = hasMatchingItemsInCart;
            } else {
                // Partial mode: hold only while matching items can actively move into the unloader buffer
                shouldHoldCart = canUnloadMoveNow;
            }
        } else {
            if (be.loadConditionMode == CONDITION_FULL_COMPLETE) {
                // Hold cart until cart is completely full
                shouldHoldCart = !isCartCompletelyFull;
            } else {
                // Partial mode: hold only while buffer has matching items that fit in the cart
                shouldHoldCart = canLoadMoveNow;
            }
        }

        if (shouldHoldCart) {
            be.holdingCart = true;
            // Depower the rail above to hold the cart/train in place!
            setRailPowerState(level, pos, state, false);
            TrainChainingUtil.freezeConsistFromBrakingRail(targetCart);

            if (be.transferCooldown <= 0) {
                boolean transferred = unloader
                        ? be.transferFromCartToBuffer(cartContainer, slotLimit)
                        : be.transferFromBufferToCart(cartContainer, slotLimit);
                if (transferred) {
                    be.transferCooldown = 4; // 4 ticks per stack batch (up to 8 items)
                    be.setChanged();
                }
            }
        } else {
            // Transfer condition met! Repower the rail above and release the cart/train!
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

    private static boolean canInsertIntoCart(Container cartContainer, int slotLimit, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < slotLimit; i++) {
            if (!cartContainer.canPlaceItem(i, stack)) continue;
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

    private boolean transferFromBufferToCart(Container cartContainer, int slotLimit) {
        int remainingToMove = 8;
        boolean movedAny = false;

        for (int b = 0; b < BUFFER_SIZE && remainingToMove > 0; b++) {
            ItemStack inBuf = this.items.get(b);
            if (inBuf.isEmpty() || !this.matchesItemFilter(inBuf)) continue;

            int toAttempt = Math.min(remainingToMove, inBuf.getCount());
            ItemStack probe = inBuf.copyWithCount(toAttempt);
            int inserted = insertIntoCartContainer(cartContainer, slotLimit, probe);
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

    private static int insertIntoCartContainer(Container cartContainer, int slotLimit, ItemStack stack) {
        int original = stack.getCount();
        ItemStack rem = stack.copy();

        // Pass 1: merge into existing stacks
        for (int i = 0; i < slotLimit && !rem.isEmpty(); i++) {
            if (!cartContainer.canPlaceItem(i, rem)) continue;
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
            if (!cartContainer.canPlaceItem(i, rem)) continue;
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
                int inserted = insertIntoCartContainer(neighborContainer, limit, inBuf.copyWithCount(toMove));
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
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("LoadConditionMode", this.loadConditionMode);
        output.putString("ItemWhitelist", this.itemWhitelist);
        output.putString("ItemBlacklist", this.itemBlacklist);
        output.putString("CartWhitelist", this.cartWhitelist);
        output.putString("CartBlacklist", this.cartBlacklist);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.loadConditionMode = input.getIntOr("LoadConditionMode", CONDITION_FULL_COMPLETE);
        this.itemWhitelist = input.getStringOr("ItemWhitelist", "");
        this.itemBlacklist = input.getStringOr("ItemBlacklist", "");
        this.cartWhitelist = input.getStringOr("CartWhitelist", "");
        this.cartBlacklist = input.getStringOr("CartBlacklist", "");
    }
}
'@
[System.IO.File]::WriteAllText($bePath, $beContent, $utf8NoBom)
Write-Host "Created CargoTransferBlockEntity.java"

# 3. CargoTransferMenu.java
$menuPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\menu\CargoTransferMenu.java"
$menuContent = @'
package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CargoTransferMenu extends AbstractContainerMenu {
    public static final int BUFFER_SIZE = CargoTransferBlockEntity.BUFFER_SIZE; // 9
    public static final int DATA_COUNT = 4;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final boolean unloader;
    private int loadConditionMode;
    private String itemWhitelist;
    private String itemBlacklist;
    private String cartWhitelist;
    private String cartBlacklist;

    // Client constructor from network buffer
    public CargoTransferMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(BUFFER_SIZE),
                new SimpleContainerData(DATA_COUNT),
                extraData.readBlockPos(),
                extraData.readBoolean(),
                extraData.readVarInt(),
                extraData.readUtf(128),
                extraData.readUtf(128),
                extraData.readUtf(128),
                extraData.readUtf(128)
        );
    }

    // Server constructor
    public CargoTransferMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            ContainerData data,
            BlockPos blockPos,
            boolean unloader,
            int loadConditionMode,
            String itemWhitelist,
            String itemBlacklist,
            String cartWhitelist,
            String cartBlacklist
    ) {
        super(MiningTools.CARGO_TRANSFER_MENU.get(), containerId);
        checkContainerSize(container, BUFFER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos;
        this.unloader = unloader;
        this.loadConditionMode = loadConditionMode;
        this.itemWhitelist = itemWhitelist != null ? itemWhitelist : "";
        this.itemBlacklist = itemBlacklist != null ? itemBlacklist : "";
        this.cartWhitelist = cartWhitelist != null ? cartWhitelist : "";
        this.cartBlacklist = cartBlacklist != null ? cartBlacklist : "";

        container.startOpen(playerInventory.player);

        // 9 Buffer slots in a single row at y = 22
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(container, col, 8 + col * 18, 22));
        }

        // Player Inventory (3 rows at y = 140, hotbar at y = 198)
        this.addStandardInventorySlots(playerInventory, 8, 140);

        this.addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public boolean isUnloader() {
        return this.unloader;
    }

    public int getLoadConditionMode() {
        return this.data.get(0);
    }

    public void setClientLoadConditionMode(int mode) {
        this.loadConditionMode = mode;
        this.data.set(0, mode);
    }

    public boolean isCartDetected() {
        return this.data.get(1) != 0;
    }

    public boolean isHoldingCart() {
        return this.data.get(2) != 0;
    }

    public int getCartFillPct() {
        return this.data.get(3);
    }

    public String getItemWhitelist() {
        return this.itemWhitelist;
    }

    public String getItemBlacklist() {
        return this.itemBlacklist;
    }

    public String getCartWhitelist() {
        return this.cartWhitelist;
    }

    public String getCartBlacklist() {
        return this.cartBlacklist;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < BUFFER_SIZE) {
                if (!this.moveItemStackTo(stackInSlot, BUFFER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, BUFFER_SIZE, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
'@
[System.IO.File]::WriteAllText($menuPath, $menuContent, $utf8NoBom)
Write-Host "Created CargoTransferMenu.java"

# 4. CargoTransferFilterPayload.java
$payloadPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\network\CargoTransferFilterPayload.java"
$payloadContent = @'
package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CargoTransferFilterPayload(
        BlockPos pos,
        int loadConditionMode,
        String itemWhitelist,
        String itemBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<CargoTransferFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cargo_transfer_filter"));

    public static final StreamCodec<ByteBuf, CargoTransferFilterPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CargoTransferFilterPayload::pos,
            ByteBufCodecs.VAR_INT,
            CargoTransferFilterPayload::loadConditionMode,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemBlacklist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartBlacklist,
            CargoTransferFilterPayload::new
    );

    @Override
    public Type<CargoTransferFilterPayload> type() {
        return TYPE;
    }

    public static void handle(CargoTransferFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || player.level() == null) {
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe) {
                transferBe.setLoadConditionMode(payload.loadConditionMode());
                transferBe.setItemWhitelist(payload.itemWhitelist());
                transferBe.setItemBlacklist(payload.itemBlacklist());
                transferBe.setCartWhitelist(payload.cartWhitelist());
                transferBe.setCartBlacklist(payload.cartBlacklist());
            }
        });
    }
}
'@
[System.IO.File]::WriteAllText($payloadPath, $payloadContent, $utf8NoBom)
Write-Host "Created CargoTransferFilterPayload.java"

# 5. CargoTransferScreen.java
$screenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CargoTransferScreen.java"
$screenContent = @'
package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.network.CargoTransferFilterPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class CargoTransferScreen extends AbstractContainerScreen<CargoTransferMenu> {

    private int conditionMode;
    private @Nullable Button conditionButton;
    private @Nullable EditBox itemWhiteBox;
    private @Nullable EditBox itemBlackBox;
    private @Nullable EditBox cartWhiteBox;
    private @Nullable EditBox cartBlackBox;

    public CargoTransferScreen(CargoTransferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 6;
        this.inventoryLabelY = 129;
        this.conditionMode = menu.getLoadConditionMode();
    }

    @Override
    protected void init() {
        super.init();
        int xo = this.leftPos;
        int yo = this.topPos;

        this.conditionMode = this.menu.getLoadConditionMode();

        // Trigger Mode Button (Full vs Partial)
        this.conditionButton = this.addRenderableWidget(
                Button.builder(this.getConditionButtonLabel(), btn -> {
                    this.conditionMode = (this.conditionMode + 1) % 2;
                    this.menu.setClientLoadConditionMode(this.conditionMode);
                    btn.setMessage(this.getConditionButtonLabel());
                    this.sendUpdateToServer();
                }).bounds(xo + 8, yo + 43, 160, 16).build()
        );

        // Item Whitelist & Blacklist (Row 1 at y = 70)
        this.itemWhiteBox = new EditBox(this.font, xo + 8, yo + 71, 78, 14, Component.literal("Item Whitelist"));
        this.itemWhiteBox.setMaxLength(128);
        this.itemWhiteBox.setHint(Component.literal("e.g. coal, ore").withStyle(ChatFormatting.DARK_GRAY));
        this.itemWhiteBox.setValue(this.menu.getItemWhitelist());
        this.itemWhiteBox.setResponder(v -> this.sendUpdateToServer());
        this.addRenderableWidget(this.itemWhiteBox);

        this.itemBlackBox = new EditBox(this.font, xo + 90, yo + 71, 78, 14, Component.literal("Item Blacklist"));
        this.itemBlackBox.setMaxLength(128);
        this.itemBlackBox.setHint(Component.literal("e.g. cobblestone").withStyle(ChatFormatting.DARK_GRAY));
        this.itemBlackBox.setValue(this.menu.getItemBlacklist());
        this.itemBlackBox.setResponder(v -> this.sendUpdateToServer());
        this.addRenderableWidget(this.itemBlackBox);

        // Cart Name Whitelist & Blacklist (Row 2 at y = 98)
        this.cartWhiteBox = new EditBox(this.font, xo + 8, yo + 99, 78, 14, Component.literal("Cart Whitelist"));
        this.cartWhiteBox.setMaxLength(128);
        this.cartWhiteBox.setHint(Component.literal("Cart/Train name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartWhiteBox.setValue(this.menu.getCartWhitelist());
        this.cartWhiteBox.setResponder(v -> this.sendUpdateToServer());
        this.addRenderableWidget(this.cartWhiteBox);

        this.cartBlackBox = new EditBox(this.font, xo + 90, yo + 99, 78, 14, Component.literal("Cart Blacklist"));
        this.cartBlackBox.setMaxLength(128);
        this.cartBlackBox.setHint(Component.literal("Exclude name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartBlackBox.setValue(this.menu.getCartBlacklist());
        this.cartBlackBox.setResponder(v -> this.sendUpdateToServer());
        this.addRenderableWidget(this.cartBlackBox);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int syncedMode = this.menu.getLoadConditionMode();
        if (syncedMode != this.conditionMode) {
            this.conditionMode = syncedMode;
            if (this.conditionButton != null) {
                this.conditionButton.setMessage(this.getConditionButtonLabel());
            }
        }
    }

    private Component getConditionButtonLabel() {
        if (this.menu.isUnloader()) {
            return this.conditionMode == CargoTransferBlockEntity.CONDITION_FULL_COMPLETE
                    ? Component.literal("Hold Until: Full Empty (0 Items)").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Hold Until: Partial / Idle").withStyle(ChatFormatting.AQUA);
        } else {
            return this.conditionMode == CargoTransferBlockEntity.CONDITION_FULL_COMPLETE
                    ? Component.literal("Hold Until: Full Load (100%)").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Hold Until: Partial / Idle").withStyle(ChatFormatting.AQUA);
        }
    }

    private void sendUpdateToServer() {
        ClientPacketDistributor.sendToServer(new CargoTransferFilterPayload(
                this.menu.getBlockPos(),
                this.conditionMode,
                this.itemWhiteBox != null ? this.itemWhiteBox.getValue() : "",
                this.itemBlackBox != null ? this.itemBlackBox.getValue() : "",
                this.cartWhiteBox != null ? this.cartWhiteBox.getValue() : "",
                this.cartBlackBox != null ? this.cartBlackBox.getValue() : ""
        ));
    }

    private boolean isAnyBoxFocused() {
        return (this.itemWhiteBox != null && this.itemWhiteBox.isFocused())
                || (this.itemBlackBox != null && this.itemBlackBox.isFocused())
                || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused())
                || (this.cartBlackBox != null && this.cartBlackBox.isFocused());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.isAnyBoxFocused()) {
                if (this.itemWhiteBox != null) this.itemWhiteBox.setFocused(false);
                if (this.itemBlackBox != null) this.itemBlackBox.setFocused(false);
                if (this.cartWhiteBox != null) this.cartWhiteBox.setFocused(false);
                if (this.cartBlackBox != null) this.cartBlackBox.setFocused(false);
                return true;
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        if (this.isAnyBoxFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.sendUpdateToServer();
                if (this.itemWhiteBox != null) this.itemWhiteBox.setFocused(false);
                if (this.itemBlackBox != null) this.itemBlackBox.setFocused(false);
                if (this.cartWhiteBox != null) this.cartWhiteBox.setFocused(false);
                if (this.cartBlackBox != null) this.cartBlackBox.setFocused(false);
                return true;
            }
            if ((this.itemWhiteBox != null && this.itemWhiteBox.isFocused() && (this.itemWhiteBox.keyPressed(event) || this.itemWhiteBox.canConsumeInput()))
                    || (this.itemBlackBox != null && this.itemBlackBox.isFocused() && (this.itemBlackBox.keyPressed(event) || this.itemBlackBox.canConsumeInput()))
                    || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused() && (this.cartWhiteBox.keyPressed(event) || this.cartWhiteBox.canConsumeInput()))
                    || (this.cartBlackBox != null && this.cartBlackBox.isFocused() && (this.cartBlackBox.keyPressed(event) || this.cartBlackBox.canConsumeInput()))) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title & Status badge
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE6EDF3, true);

        String statusStr;
        int statusColor;
        if (this.menu.isHoldingCart()) {
            statusStr = "HOLDING (" + this.menu.getCartFillPct() + "%)";
            statusColor = 0xFFFFAA00;
        } else if (this.menu.isCartDetected()) {
            statusStr = "READY (" + this.menu.getCartFillPct() + "%)";
            statusColor = 0xFF55FF55;
        } else {
            statusStr = "RAIL POWERED";
            statusColor = 0xFF8B949E;
        }
        int sw = this.font.width(statusStr);
        graphics.text(this.font, Component.literal(statusStr), this.imageWidth - sw - 8, 6, statusColor, true);

        graphics.text(this.font, Component.literal("Item Whitelist:"), 8, 61, 0xFF55FF55, false);
        graphics.text(this.font, Component.literal("Item Blacklist:"), 90, 61, 0xFFFF5555, false);
        graphics.text(this.font, Component.literal("Cart Whitelist:"), 8, 89, 0xFF55FFFF, false);
        graphics.text(this.font, Component.literal("Cart Blacklist:"), 90, 89, 0xFFFFAA00, false);

        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCCCCCC, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Main industrial dark slate panel
        graphics.fill(xo - 2, yo - 2, xo + this.imageWidth + 2, yo + this.imageHeight + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + this.imageWidth, yo + this.imageHeight, 0xF221252B);
        graphics.outline(xo, yo, this.imageWidth, this.imageHeight, 0xFF4B5362);

        // Top accent bar (amber for Unloader, emerald/cyan for Loader)
        int accent = this.menu.isUnloader() ? 0xFFF59E0B : 0xFF10B981;
        graphics.fill(xo + 1, yo + 1, xo + this.imageWidth - 1, yo + 3, accent);

        // Draw 9 buffer slot frames at y = 22
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, xo + 7 + col * 18, yo + 21);
        }

        // Divider above player inventory
        graphics.fill(xo + 6, yo + 124, xo + this.imageWidth - 6, yo + 125, 0xFF3E4451);

        // Draw player inventory slot frames (3 rows at y = 140, hotbar at y = 198)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, xo + 7 + col * 18, yo + 139 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, xo + 7 + col * 18, yo + 197);
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 16, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + 16, 0xFF373737);
    }
}
'@
[System.IO.File]::WriteAllText($screenPath, $screenContent, $utf8NoBom)
Write-Host "Created CargoTransferScreen.java"
