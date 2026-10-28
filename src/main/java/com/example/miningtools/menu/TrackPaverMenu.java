package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.TrackPaverEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;

public class TrackPaverMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = TrackPaverEntity.INVENTORY_SIZE;
    public static final int DATA_COUNT = 7;

    public static final int SLOT_PAVER_BLOCK = TrackPaverEntity.SLOT_PAVER_BLOCK;
    public static final int SLOT_FUEL = TrackPaverEntity.SLOT_FUEL;
    public static final int SLOT_MATERIAL_START = TrackPaverEntity.SLOT_MATERIAL_START;
    public static final int SLOT_MATERIAL_COUNT = TrackPaverEntity.SLOT_MATERIAL_COUNT;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public TrackPaverMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public TrackPaverMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.TRACK_PAVER_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Paver Pattern / Target Block Slot (Accepts any placeable full block)
        this.addSlot(new Slot(container, SLOT_PAVER_BLOCK, 54, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 1: Fuel Slot (Any burnable fuel)
        this.addSlot(new Slot(container, SLOT_FUEL, 18, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BasicLocomotiveEntity.isBurnableFuel(stack);
            }
        });

        // Slots 2..28: 27 Material Inventory Slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = SLOT_MATERIAL_START + col + row * 9;
                this.addSlot(new Slot(container, index, 8 + col * 18, 54 + row * 18));
            }
        }

        // Player Inventory (3 rows of 9)
        this.addStandardInventorySlots(playerInventory, 8, 122);

        this.addDataSlots(data);
    }

    public int getFuel() {
        return this.data.get(0);
    }

    public int getFuelTotal() {
        return this.data.get(1);
    }

    public int getRunMode() {
        return this.data.get(2);
    }

    public int getCardinalLock() {
        return this.data.get(3);
    }

    public boolean isPaving() {
        return this.isWorkMode();
    }

    public boolean isWorkMode() {
        return this.data.get(4) != 0;
    }

    public boolean isWorkLightEnabled() {
        return this.data.get(5) != 0;
    }

    public boolean isHologramEnabled() {
        return this.data.get(6) != 0;
    }

    public int getBurnProgressScaled(int pixels) {
        int total = this.getFuelTotal();
        if (total <= 0) total = 1600;
        int fuel = this.getFuel();
        return (fuel > 0) ? (fuel * pixels / total) : 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            int nextMode = (this.getRunMode() == TrackPaverEntity.MODE_FORWARD)
                    ? TrackPaverEntity.MODE_STOPPED
                    : TrackPaverEntity.MODE_FORWARD;
            if (this.container instanceof TrackPaverEntity paver) {
                if (nextMode != TrackPaverEntity.MODE_STOPPED && paver.isPaving() && !paver.hasPaverMaterial()) {
                    paver.notifyOutOfMaterial(player);
                    this.data.set(2, TrackPaverEntity.MODE_STOPPED);
                    return true;
                }
            }
            this.data.set(2, nextMode);
            return true;
        } else if (id == 1) {
            // Toggle Reverse
            int nextMode = (this.getRunMode() == TrackPaverEntity.MODE_REVERSE)
                    ? TrackPaverEntity.MODE_STOPPED
                    : TrackPaverEntity.MODE_REVERSE;
            if (this.container instanceof TrackPaverEntity paver) {
                if (nextMode != TrackPaverEntity.MODE_STOPPED && paver.isPaving() && !paver.hasPaverMaterial()) {
                    paver.notifyOutOfMaterial(player);
                    this.data.set(2, TrackPaverEntity.MODE_STOPPED);
                    return true;
                }
            }
            this.data.set(2, nextMode);
            return true;
        } else if (id == 2) {
            // Cycle Cardinal Lock: OFF (0) -> NORTH (1) -> SOUTH (2) -> WEST (3) -> EAST (4) -> OFF (0)
            int current = this.getCardinalLock();
            int next = (current + 1) % 5;
            this.data.set(3, next);
            return true;
        } else if (id == 3) {
            // Toggle Work Mode On / Off
            this.data.set(4, this.isWorkMode() ? 0 : 1);
            return true;
        } else if (id == 4) {
            // Toggle Work Light On / Off
            this.data.set(5, this.isWorkLightEnabled() ? 0 : 1);
            return true;
        } else if (id == 5) {
            // Toggle Hologram Grid On / Off
            this.data.set(6, this.isHologramEnabled() ? 0 : 1);
            return true;
        }
        return false;
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
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < CONTAINER_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(slotStack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (BasicLocomotiveEntity.isBurnableFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (slotStack.getItem() instanceof BlockItem) {
                    if (this.slots.get(SLOT_PAVER_BLOCK).getItem().isEmpty()) {
                        ItemStack single = slotStack.copyWithCount(1);
                        this.slots.get(SLOT_PAVER_BLOCK).set(single);
                        slotStack.shrink(1);
                    } else if (!this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
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
