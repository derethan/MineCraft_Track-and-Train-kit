package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TrackLayerCartEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrackLayerCartMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = TrackLayerCartEntity.INVENTORY_SIZE;
    public static final int DATA_COUNT = 5;

    public static final int SLOT_PATTERN = TrackLayerCartEntity.SLOT_PATTERN;
    public static final int SLOT_STORAGE_START = TrackLayerCartEntity.SLOT_STORAGE_START;
    public static final int SLOT_STORAGE_COUNT = TrackLayerCartEntity.SLOT_STORAGE_COUNT;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public TrackLayerCartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public TrackLayerCartMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.TRACK_LAYER_CART_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Pattern / Preferred Track Slot
        this.addSlot(new Slot(container, SLOT_PATTERN, 18, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TrackLayerCartEntity.isRailItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slots 1..27: 3x9 Track Storage Grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = SLOT_STORAGE_START + col + row * 9;
                this.addSlot(new Slot(container, slotIndex, 8 + col * 18, 54 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return TrackLayerCartEntity.isRailItem(stack);
                    }
                });
            }
        }

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 180));
        }

        this.addDataSlots(data);
    }

    public int getWorkMode() {
        return this.data.get(0);
    }

    public int getRunMode() {
        return this.data.get(1);
    }

    public boolean isActive() {
        return this.data.get(2) != 0;
    }

    public int getStoredRailCount() {
        return this.data.get(3);
    }

    public int getFacing2D() {
        return this.data.get(4);
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            int next = (this.getRunMode() == TrackLayerCartEntity.RUN_MODE_STOPPED)
                    ? TrackLayerCartEntity.RUN_MODE_FORWARD
                    : TrackLayerCartEntity.RUN_MODE_STOPPED;
            this.data.set(1, next);
            return true;
        } else if (id == 1) {
            // Toggle Reverse
            int next = (this.getRunMode() == TrackLayerCartEntity.RUN_MODE_REVERSE)
                    ? TrackLayerCartEntity.RUN_MODE_FORWARD
                    : TrackLayerCartEntity.RUN_MODE_REVERSE;
            this.data.set(1, next);
            return true;
        } else if (id == 2) {
            // Cycle Work Mode (LAY -> REMOVE -> OFF)
            int next = (this.getWorkMode() + 1) % 3;
            this.data.set(0, next);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < CONTAINER_SIZE) {
                // Cart inventory to player inventory
                if (!this.moveItemStackTo(slotStack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory to cart inventory
                if (TrackLayerCartEntity.isRailItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_STORAGE_START, CONTAINER_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
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
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
