package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.FluidLoaderBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FluidLoaderMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = FluidLoaderBlockEntity.CONTAINER_SIZE; // 2
    public static final int DATA_COUNT = 8;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public FluidLoaderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public FluidLoaderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.FLUID_LOADER_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Bucket Input slot
        this.addSlot(new Slot(container, FluidLoaderBlockEntity.SLOT_INPUT, 88, 59));

        // Slot 1: Bucket Output slot
        this.addSlot(new Slot(container, FluidLoaderBlockEntity.SLOT_OUTPUT, 142, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory & Hotbar (3 rows at y=84, hotbar at y=142)
        this.addStandardInventorySlots(playerInventory, 8, 84);

        // Data slots
        this.addDataSlots(data);
    }

    public int getStationFluidAmount() {
        return this.data.get(0);
    }

    public int getStationCapacity() {
        int cap = this.data.get(1);
        return cap <= 0 ? FluidLoaderBlockEntity.TANK_CAPACITY : cap;
    }

    public int getTransferMode() {
        return this.data.get(2);
    }

    public int getRedstoneMode() {
        return this.data.get(3);
    }

    public int getCartFluidAmount() {
        return this.data.get(4);
    }

    public int getCartFluidCapacity() {
        int cap = this.data.get(5);
        return cap <= 0 ? FluidLoaderBlockEntity.TANK_CAPACITY : cap;
    }

    public boolean isCartPresent() {
        return this.data.get(6) != 0;
    }

    public boolean isRedstonePowered() {
        return this.data.get(7) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Cycle Transfer Mode: UNLOAD -> LOAD -> AUTO -> UNLOAD
            int current = getTransferMode();
            this.data.set(2, (current + 1) % 3);
            return true;
        } else if (id == 1) {
            // Cycle Redstone Mode: WHEN_EMPTY -> WHEN_FULL -> WHEN_DONE -> DISABLED -> WHEN_EMPTY
            int current = getRedstoneMode();
            this.data.set(3, (current + 1) % 4);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (slotIndex < CONTAINER_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to machine input slot
                if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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
