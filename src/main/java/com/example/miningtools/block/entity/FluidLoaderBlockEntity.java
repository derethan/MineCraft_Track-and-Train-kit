package com.example.miningtools.block.entity;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.FluidLoaderBlock;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.menu.FluidLoaderMenu;

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
    public static final int MODE_LOAD = 1;   // Station -> Fill Cart
    public static final int MODE_AUTO = 2;   // Bidirectional

    public static final int REDSTONE_WHEN_EMPTY = 0; // Pulse when cart is empty
    public static final int REDSTONE_WHEN_FULL = 1;  // Pulse when cart is full
    public static final int REDSTONE_WHEN_DONE = 2;  // Pulse when transfer operation finishes
    public static final int REDSTONE_DISABLED = 3;   // No redstone output

    public static final int CONTAINER_SIZE = 2;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int[] SLOTS = new int[]{SLOT_INPUT, SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, TANK_CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousStack) {
            super.onContentsChanged(index, previousStack);
            FluidLoaderBlockEntity.this.setChanged();
        }
    };

    private int transferMode = MODE_UNLOAD;
    private int redstoneMode = REDSTONE_WHEN_EMPTY;
    private int pulseTicks = 0;

    // Ephemeral status for GUI syncing
    private int cartFluidAmount = 0;
    private int cartFluidCapacity = 0;
    private boolean cartPresent = false;
    private boolean lastCartWasTransferring = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FluidLoaderBlockEntity.this.tank.getAmountAsInt(0);
                case 1 -> TANK_CAPACITY;
                case 2 -> FluidLoaderBlockEntity.this.transferMode;
                case 3 -> FluidLoaderBlockEntity.this.redstoneMode;
                case 4 -> FluidLoaderBlockEntity.this.cartFluidAmount;
                case 5 -> FluidLoaderBlockEntity.this.cartFluidCapacity;
                case 6 -> FluidLoaderBlockEntity.this.cartPresent ? 1 : 0;
                case 7 -> (FluidLoaderBlockEntity.this.pulseTicks > 0) ? 1 : 0;
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
            return 8;
        }
    };

    public FluidLoaderBlockEntity(BlockPos pos, BlockState blockState) {
        super(MiningTools.FLUID_LOADER_BLOCK_ENTITY.get(), pos, blockState);
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
        return this.transferMode;
    }

    public void setTransferMode(int mode) {
        this.transferMode = Math.floorMod(mode, 3);
        this.setChanged();
    }

    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        this.redstoneMode = Math.floorMod(mode, 4);
        this.setChanged();
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.miningtools.fluid_loader");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidLoaderMenu(containerId, inventory, this, this.dataAccess);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidLoaderBlockEntity blockEntity) {
        // 1. Process bucket input/output slots
        blockEntity.tickBucketSlots();

        // 2. Scan for minecart on the track above
        BlockPos trackPos = pos.above();
        AABB trackBox = new AABB(trackPos).inflate(0.5, 0.5, 0.5);
        List<Entity> entities = level.getEntities((Entity) null, trackBox, e -> e.isAlive() && !e.isRemoved());

        Entity targetCart = null;
        ResourceHandler<FluidResource> cartFluidHandler = null;

        for (Entity ent : entities) {
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

        if (cartFluidHandler != null) {
            blockEntity.cartPresent = true;
            blockEntity.cartFluidAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidCapacity = cartFluidHandler.getCapacityAsInt(0, cartFluidHandler.getResource(0));
            if (blockEntity.cartFluidCapacity <= 0) {
                blockEntity.cartFluidCapacity = TANK_CAPACITY;
            }

            int mode = blockEntity.transferMode;
            boolean movedAny = false;

            if (mode == MODE_UNLOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount > 0)) {
                // Drain cart into station buffer
                int moved = ResourceHandlerUtil.move(cartFluidHandler, blockEntity.tank, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            } else if (mode == MODE_LOAD || (mode == MODE_AUTO && blockEntity.cartFluidAmount == 0)) {
                // Fill cart from station buffer
                int moved = ResourceHandlerUtil.move(blockEntity.tank, cartFluidHandler, r -> true, TRANSFER_RATE, null);
                if (moved > 0) {
                    movedAny = true;
                }
            }

            // Update cart amount after move
            int newCartAmount = cartFluidHandler.getAmountAsInt(0);
            blockEntity.cartFluidAmount = newCartAmount;

            // Check redstone completion trigger
            boolean justFinished = false;
            if (blockEntity.redstoneMode == REDSTONE_WHEN_EMPTY) {
                if (newCartAmount == 0 && (movedAny || blockEntity.lastCartWasTransferring)) {
                    justFinished = true;
                }
            } else if (blockEntity.redstoneMode == REDSTONE_WHEN_FULL) {
                if (ResourceHandlerUtil.isFull(cartFluidHandler) && (movedAny || blockEntity.lastCartWasTransferring)) {
                    justFinished = true;
                }
            } else if (blockEntity.redstoneMode == REDSTONE_WHEN_DONE) {
                if (!movedAny && blockEntity.lastCartWasTransferring) {
                    justFinished = true;
                }
            }

            if (justFinished && blockEntity.pulseTicks <= 0) {
                blockEntity.triggerRedstonePulse(level, pos, state);
            }

            blockEntity.lastCartWasTransferring = movedAny;
        } else {
            blockEntity.cartPresent = false;
            blockEntity.cartFluidAmount = 0;
            blockEntity.cartFluidCapacity = 0;
            blockEntity.lastCartWasTransferring = false;
        }

        // 3. Handle redstone pulse countdown
        if (blockEntity.pulseTicks > 0) {
            blockEntity.pulseTicks--;
            if (blockEntity.pulseTicks == 0) {
                // Pulse expired -> turn off redstone power
                setPoweredState(level, pos, state, false);
            }
        }
    }

    private void triggerRedstonePulse(Level level, BlockPos pos, BlockState state) {
        this.pulseTicks = 25; // 1.25 seconds pulse
        setPoweredState(level, pos, state, true);
    }

    private static void setPoweredState(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.is(MiningTools.FLUID_LOADER.get()) && state.getValue(FluidLoaderBlock.POWERED) != powered) {
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
            if (this.transferMode != MODE_LOAD && !ResourceHandlerUtil.isEmpty(inputHandler)) {
                // Drain item into tank
                int moved = ResourceHandlerUtil.move(inputHandler, this.tank, r -> true, FluidType.BUCKET_VOLUME, null);
                if (moved > 0) {
                    ItemStack emptyContainer = itemAccess.getResource().toStack();
                    input.shrink(1);
                    insertOutputItem(emptyContainer);
                    this.setChanged();
                    return;
                }
            }

            // In LOAD mode: try to fill empty container from internal tank
            if (this.transferMode != MODE_UNLOAD && ResourceHandlerUtil.isEmpty(inputHandler) && this.tank.getAmountAsInt(0) >= FluidType.BUCKET_VOLUME) {
                int moved = ResourceHandlerUtil.move(this.tank, inputHandler, r -> true, FluidType.BUCKET_VOLUME, null);
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
        output.putInt("TransferMode", this.transferMode);
        output.putInt("RedstoneMode", this.redstoneMode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.tank.deserialize(input);
        this.transferMode = input.getIntOr("TransferMode", MODE_UNLOAD);
        this.redstoneMode = input.getIntOr("RedstoneMode", REDSTONE_WHEN_EMPTY);
    }
}
