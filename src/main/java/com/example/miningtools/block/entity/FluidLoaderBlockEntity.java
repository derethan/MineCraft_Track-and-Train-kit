package com.example.miningtools.block.entity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.FluidLoaderBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.menu.FluidLoaderMenu;
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
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class FluidLoaderBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME; // 16,000 mB
    public static final int TRANSFER_RATE = 100; // 100 mB per tick = 2,000 mB/sec

    public static final int MODE_UNLOAD = 0; // Empty Cart -> Station
    public static final int MODE_LOAD = 1;   // Station -> Fill Cart / Locomotive
    public static final int MODE_AUTO = 2;   // Bidirectional

    public static final int REDSTONE_WHEN_EMPTY = 0; // Hold until cart is empty
    public static final int REDSTONE_WHEN_FULL = 1;  // Hold until cart/loco is full
    public static final int REDSTONE_WHEN_DONE = 2;  // Hold while actively transferring (partial)
    public static final int REDSTONE_DISABLED = 3;   // Never depower rail

    public static final int CONTAINER_SIZE = 2;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int[] SLOTS = new int[]{SLOT_INPUT, SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, TANK_CAPACITY) {
        @Override
        public boolean isValid(int slot, FluidResource resource) {
            FluidStack probe = resource.toStack(1);
            if (FluidLoaderBlockEntity.this.isDieselFueler() && !DieselLocomotiveEntity.isDieselFuel(probe)) {
                return false;
            }
            return FluidLoaderBlockEntity.this.matchesFluidFilter(probe);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousStack) {
            super.onContentsChanged(index, previousStack);
            FluidLoaderBlockEntity.this.setChanged();
        }
    };

    private int transferMode;
    private int redstoneMode;
    private String fluidWhitelist = "";
    private String fluidBlacklist = "";
    private String cartWhitelist = "";
    private String cartBlacklist = "";

    private @Nullable UUID releasedCartId = null;
    private int releaseGraceTicks = 0;

    // Ephemeral status for GUI syncing
    private int cartFluidAmount = 0;
    private int cartFluidCapacity = 0;
    private boolean cartPresent = false;
    private boolean holdingCart = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FluidLoaderBlockEntity.this.tank.getAmountAsInt(0);
                case 1 -> TANK_CAPACITY;
                case 2 -> FluidLoaderBlockEntity.this.getTransferMode();
                case 3 -> FluidLoaderBlockEntity.this.redstoneMode;
                case 4 -> FluidLoaderBlockEntity.this.cartFluidAmount;
                case 5 -> FluidLoaderBlockEntity.this.cartFluidCapacity;
                case 6 -> FluidLoaderBlockEntity.this.cartPresent ? 1 : 0;
                case 7 -> FluidLoaderBlockEntity.this.holdingCart ? 1 : 0;
                case 8 -> FluidLoaderBlockEntity.this.getStationRole();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> FluidLoaderBlockEntity.this.setTransferMode(value);
                case 3 -> FluidLoaderBlockEntity.this.setRedstoneMode(value);
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public FluidLoaderBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.FLUID_LOADER_BLOCK_ENTITY.get(), pos, blockState);
        if (blockState.getBlock() instanceof FluidLoaderBlock flb) {
            if (flb.isDieselFueler()) {
                this.transferMode = MODE_LOAD;
                this.redstoneMode = REDSTONE_WHEN_FULL;
            } else if (flb.isDefaultUnloader()) {
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
        if (state.getBlock() instanceof FluidLoaderBlock flb) {
            return flb.getStationRole();
        }
        return FluidLoaderBlock.ROLE_LOADER;
    }

    public boolean isDieselFueler() {
        return this.getStationRole() == FluidLoaderBlock.ROLE_DIESEL_FUELER;
    }

    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        return this.tank;
    }

    public FluidStacksResourceHandler getInternalTank() {
        return this.tank;
    }

    public FluidStack getStoredFluid() {
        return FluidUtil.getStack(this.tank, 0);
    }

    public int getTransferMode() {
        return this.isDieselFueler() ? MODE_LOAD : this.transferMode;
    }

    public void setTransferMode(int mode) {
        if (this.isDieselFueler()) {
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
        if (this.isDieselFueler()) {
            // For Diesel Fueler: cycle between On Full (1), When Done (2), and Disabled (3)
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

    public String getFluidWhitelist() {
        return this.fluidWhitelist;
    }

    public void setFluidWhitelist(String val) {
        this.fluidWhitelist = val != null ? val.trim() : "";
        this.setChanged();
    }

    public String getFluidBlacklist() {
        return this.fluidBlacklist;
    }

    public void setFluidBlacklist(String val) {
        this.fluidBlacklist = val != null ? val.trim() : "";
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

    public boolean matchesFluidFilter(FluidResource resource) {
        if (resource == null || resource.isEmpty()) return false;
        return this.matchesFluidFilter(resource.toStack(1));
    }

    public boolean matchesFluidFilter(FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (this.isDieselFueler() && !DieselLocomotiveEntity.isDieselFuel(stack)) {
            return false;
        }

        Identifier id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        String fullId = id != null ? id.toString().toLowerCase(Locale.ROOT) : "";
        String path = id != null ? id.getPath().toLowerCase(Locale.ROOT) : "";
        String display = stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);

        if (!this.fluidBlacklist.isBlank()) {
            for (String token : this.fluidBlacklist.split("[,;\\n]+")) {
                String t = token.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    if (fullId.contains(t) || path.contains(t) || display.contains(t)) {
                        return false;
                    }
                }
            }
        }

        if (!this.fluidWhitelist.isBlank()) {
            boolean anyToken = false;
            for (String token : this.fluidWhitelist.split("[,;\\n]+")) {
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

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public Component getDisplayName() {
        return switch (this.getStationRole()) {
            case FluidLoaderBlock.ROLE_DIESEL_FUELER -> Component.translatable("block.miningtools.diesel_fueler");
            case FluidLoaderBlock.ROLE_UNLOADER -> Component.translatable("block.miningtools.fluid_unloader");
            default -> Component.translatable("block.miningtools.fluid_loader");
        };
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidLoaderMenu(containerId, inventory, this, this.dataAccess);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidLoaderBlockEntity blockEntity) {
        if (blockEntity.releaseGraceTicks > 0) {
            blockEntity.releaseGraceTicks--;
            if (blockEntity.releaseGraceTicks == 0) {
                blockEntity.releasedCartId = null;
            }
        }

        // 1. Process bucket input/output slots
        blockEntity.tickBucketSlots();

        // 2. Scan for target vehicle on the track above
        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(1.1, 0.65, 1.1);
        List<Entity> entities = level.getEntities((Entity) null, trackBox, e -> e.isAlive() && !e.isRemoved());

        boolean dieselFueler = blockEntity.isDieselFueler();
        Entity targetCart = null;
        ResourceHandler<FluidResource> cartFluidHandler = null;

        for (Entity ent : entities) {
            if (blockEntity.releasedCartId != null && blockEntity.releasedCartId.equals(ent.getUUID())) {
                continue;
            }

            if (dieselFueler) {
                // Dedicated Diesel Locomotive Fueling Station ONLY targets DieselLocomotiveEntity
                if (ent instanceof DieselLocomotiveEntity dieselLoco) {
                    if (TrainChainingUtil.matchesCartNameFilter(dieselLoco, blockEntity.cartWhitelist, blockEntity.cartBlacklist)) {
                        targetCart = dieselLoco;
                        cartFluidHandler = dieselLoco.getFluidHandler();
                        break;
                    }
                }
            } else {
                // Freight Fluid Loader / Unloader NEVER touches any locomotive or tunnel bore!
                if (ent instanceof DieselLocomotiveEntity
                        || ent instanceof BasicLocomotiveEntity
                        || ent instanceof ElectricLocomotiveEntity
                        || ent instanceof TunnelBoreEntity) {
                    continue;
                }
                if (ent instanceof AbstractMinecart mc) {
                    if (!TrainChainingUtil.matchesCartNameFilter(mc, blockEntity.cartWhitelist, blockEntity.cartBlacklist)) {
                        continue;
                    }
                }
                ResourceHandler<FluidResource> handler = Capabilities.Fluid.ENTITY.getCapability(ent, Direction.DOWN);
                if (handler == null && ent instanceof LiquidMinecartEntity lme) {
                    handler = lme.getFluidHandler();
                }
                if (handler != null) {
                    targetCart = ent;
                    cartFluidHandler = handler;
                    break;
                }
            }
        }

        if (cartFluidHandler != null && targetCart != null) {
            blockEntity.cartPresent = true;
            blockEntity.cartFluidAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidCapacity = cartFluidHandler.getCapacityAsInt(0, cartFluidHandler.getResource(0));
            if (blockEntity.cartFluidCapacity <= 0) {
                blockEntity.cartFluidCapacity = TANK_CAPACITY;
            }

            int mode = blockEntity.getTransferMode();
            boolean movedAny = false;

            if (!dieselFueler && (mode == MODE_UNLOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount > 0))) {
                int moved = ResourceHandlerUtil.move(
                        cartFluidHandler,
                        blockEntity.tank,
                        blockEntity::matchesFluidFilter,
                        TRANSFER_RATE,
                        null
                );
                if (moved > 0) {
                    movedAny = true;
                }
            } else if (mode == MODE_LOAD || dieselFueler || (mode == MODE_AUTO && blockEntity.cartFluidAmount == 0)) {
                int moved = ResourceHandlerUtil.move(
                        blockEntity.tank,
                        cartFluidHandler,
                        blockEntity::matchesFluidFilter,
                        TRANSFER_RATE,
                        null
                );
                if (moved > 0) {
                    movedAny = true;
                }
            }

            if (movedAny) {
                if (targetCart instanceof LiquidMinecartEntity lme) {
                    lme.syncFluidData();
                } else if (targetCart instanceof DieselLocomotiveEntity dle) {
                    dle.syncFluidData();
                }
            }

            int newCartAmount = cartFluidHandler.getAmountAsInt(0);
            FluidResource cartRes = cartFluidHandler.getResource(0);
            blockEntity.cartFluidAmount = newCartAmount;
            boolean cartFull = newCartAmount >= blockEntity.cartFluidCapacity || ResourceHandlerUtil.isFull(cartFluidHandler);

            // Check whether remaining fluid in cart matches filter (for unload mode)
            boolean hasMatchingFluidInCart = newCartAmount > 0 && blockEntity.matchesFluidFilter(cartRes);

            boolean shouldHold = false;
            if (blockEntity.redstoneMode != REDSTONE_DISABLED) {
                if (blockEntity.redstoneMode == REDSTONE_WHEN_EMPTY) {
                    shouldHold = hasMatchingFluidInCart;
                } else if (blockEntity.redstoneMode == REDSTONE_WHEN_FULL) {
                    shouldHold = !cartFull;
                } else if (blockEntity.redstoneMode == REDSTONE_WHEN_DONE) {
                    shouldHold = movedAny;
                }
            }

            if (shouldHold) {
                blockEntity.holdingCart = true;
                setPoweredState(level, pos, state, false);
                if (targetCart instanceof AbstractMinecart mc) {
                    TrainChainingUtil.freezeConsistFromBrakingRail(mc);
                }
            } else {
                boolean wasHolding = blockEntity.holdingCart || !state.getValue(FluidLoaderBlock.POWERED);
                blockEntity.holdingCart = false;
                blockEntity.releasedCartId = targetCart.getUUID();
                blockEntity.releaseGraceTicks = 35;
                setPoweredState(level, pos, state, true);
                if (wasHolding && targetCart instanceof AbstractMinecart mc) {
                    TrainChainingUtil.releaseConsistFromBrakingRail(mc);
                }
            }
        } else {
            blockEntity.cartPresent = false;
            blockEntity.holdingCart = false;
            blockEntity.cartFluidAmount = 0;
            blockEntity.cartFluidCapacity = 0;
            setPoweredState(level, pos, state, true);
        }
    }

    private static void setPoweredState(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(FluidLoaderBlock.POWERED) && state.getValue(FluidLoaderBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(FluidLoaderBlock.POWERED, powered), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.above(), state.getBlock());
        }
    }

    private void tickBucketSlots() {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return;
        }

        var itemAccess = ItemAccess.forStack(input.copyWithCount(1));
        var inputHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
        if (inputHandler != null) {
            // Allow draining filled fluid buckets into the station tank in any mode if the fluid matches
            if (!ResourceHandlerUtil.isEmpty(inputHandler)) {
                int moved = ResourceHandlerUtil.move(
                        inputHandler,
                        this.tank,
                        this::matchesFluidFilter,
                        FluidType.BUCKET_VOLUME,
                        null
                );
                if (moved > 0) {
                    ItemStack emptyContainer = itemAccess.getResource().toStack();
                    input.shrink(1);
                    insertOutputItem(emptyContainer);
                    this.setChanged();
                    return;
                }
            }

            // Allow filling empty buckets from the station tank (unless Diesel Fueler)
            if (!this.isDieselFueler() && ResourceHandlerUtil.isEmpty(inputHandler) && this.tank.getAmountAsInt(0) >= FluidType.BUCKET_VOLUME) {
                int moved = ResourceHandlerUtil.move(
                        this.tank,
                        inputHandler,
                        this::matchesFluidFilter,
                        FluidType.BUCKET_VOLUME,
                        null
                );
                if (moved > 0) {
                    ItemStack filledContainer = itemAccess.getResource().toStack();
                    input.shrink(1);
                    insertOutputItem(filledContainer);
                    this.setChanged();
                }
            }
        }
    }

    private void insertOutputItem(ItemStack result) {
        if (result.isEmpty()) return;
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, result);
        } else if (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize()) {
            output.grow(result.getCount());
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(0).isEmpty() && this.items.get(1).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack res = ContainerHelper.removeItem(this.items, slot, count);
        if (!res.isEmpty()) this.setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
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
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.tank.serialize(output);
        output.putInt("TransferMode", this.getTransferMode());
        output.putInt("RedstoneMode", this.redstoneMode);
        output.putString("FluidWhitelist", this.fluidWhitelist);
        output.putString("FluidBlacklist", this.fluidBlacklist);
        output.putString("CartWhitelist", this.cartWhitelist);
        output.putString("CartBlacklist", this.cartBlacklist);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.tank.deserialize(input);
        int defaultTransfer = (this.isDieselFueler() || this.getStationRole() == FluidLoaderBlock.ROLE_LOADER) ? MODE_LOAD : MODE_UNLOAD;
        int defaultRedstone = (this.isDieselFueler() || this.getStationRole() == FluidLoaderBlock.ROLE_LOADER) ? REDSTONE_WHEN_FULL : REDSTONE_WHEN_EMPTY;
        this.transferMode = this.isDieselFueler() ? MODE_LOAD : input.getIntOr("TransferMode", defaultTransfer);
        this.redstoneMode = input.getIntOr("RedstoneMode", defaultRedstone);
        this.fluidWhitelist = input.getStringOr("FluidWhitelist", "");
        this.fluidBlacklist = input.getStringOr("FluidBlacklist", "");
        this.cartWhitelist = input.getStringOr("CartWhitelist", "");
        this.cartBlacklist = input.getStringOr("CartBlacklist", "");
    }
}
