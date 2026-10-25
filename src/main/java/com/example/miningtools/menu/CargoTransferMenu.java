package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.CargoTransferBlock;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;

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
    public static final int DATA_COUNT = 9;

    private final Container container;
    private final ContainerData data;

    // Client constructor
    public CargoTransferMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BUFFER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server constructor
    public CargoTransferMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.CARGO_TRANSFER_MENU.get(), containerId);
        checkContainerSize(container, BUFFER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);

        // 9 Buffer slots in a single row at y = 56
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(container, col, 8 + col * 18, 56) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (CargoTransferMenu.this.isCoalFueler()) {
                        return CargoTransferBlockEntity.isCoalFuelItem(stack);
                    }
                    return true;
                }
            });
        }

        // Player Inventory (3 rows at y = 140, hotbar at y = 198)
        this.addStandardInventorySlots(playerInventory, 8, 140);

        this.addDataSlots(data);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getStationItemCount() {
        return this.data.get(0);
    }

    public int getStationCapacity() {
        int cap = this.data.get(1);
        return cap <= 0 ? CargoTransferBlockEntity.BUFFER_CAPACITY : cap;
    }

    public int getTransferMode() {
        return this.data.get(2);
    }

    public int getRedstoneMode() {
        return this.data.get(3);
    }

    public int getCartItemCount() {
        return this.data.get(4);
    }

    public int getCartItemCapacity() {
        int cap = this.data.get(5);
        return cap <= 0 ? 576 : cap;
    }

    public boolean isCartDetected() {
        return this.data.get(6) != 0;
    }

    public boolean isHoldingCart() {
        return this.data.get(7) != 0;
    }

    public int getStationRole() {
        return this.data.get(8);
    }

    public boolean isCoalFueler() {
        return this.getStationRole() == CargoTransferBlock.ROLE_COAL_FUELER;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            if (!this.isCoalFueler()) {
                int current = getTransferMode();
                this.data.set(2, (current + 1) % 3);
            }
            return true;
        } else if (id == 1) {
            int current = getRedstoneMode();
            int next = (current + 1) % 4;
            if (this.isCoalFueler() && next == CargoTransferBlockEntity.REDSTONE_WHEN_EMPTY) {
                next = CargoTransferBlockEntity.REDSTONE_WHEN_FULL;
            }
            this.data.set(3, next);
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
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < BUFFER_SIZE) {
                if (!this.moveItemStackTo(stackInSlot, BUFFER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (this.isCoalFueler() && !CargoTransferBlockEntity.isCoalFuelItem(stackInSlot)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, 0, BUFFER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
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
