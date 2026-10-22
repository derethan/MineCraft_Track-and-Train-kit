package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.ElectricLocomotiveEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ElectricLocomotiveMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 1;
    public static final int DATA_COUNT = 10;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public ElectricLocomotiveMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public ElectricLocomotiveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.ELECTRIC_LOCOMOTIVE_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // 1 Battery / Energy Item Slot
        this.addSlot(new Slot(container, 0, 80, 53));

        // Player inventory & hotbar
        this.addStandardInventorySlots(playerInventory, 8, 84);

        // Data slots
        this.addDataSlots(data);
    }

    public int getEnergyStored() {
        int low = this.data.get(0) & 0xFFFF;
        int high = this.data.get(1) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getEnergyCapacity() {
        int low = this.data.get(2) & 0xFFFF;
        int high = this.data.get(3) & 0xFFFF;
        int cap = (high << 16) | low;
        return cap > 0 ? cap : 100000;
    }

    public float getEnergyPercent() {
        int cap = this.getEnergyCapacity();
        return cap <= 0 ? 0.0F : Mth.clamp((float) this.getEnergyStored() / (float) cap, 0.0F, 1.0F);
    }

    public boolean isRunning() {
        return this.data.get(4) != 0;
    }

    public int getFacingIndex() {
        return this.data.get(5);
    }

    public int getSpeedLevel() {
        return this.data.get(6);
    }

    public boolean isHeadlightEnabled() {
        return this.data.get(7) != 0;
    }

    public boolean isPoweredFromRail() {
        return this.data.get(8) != 0;
    }

    public int getEntityId() {
        return this.data.get(9);
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            boolean next = !this.isRunning();
            this.data.set(4, next ? 1 : 0);
            return true;
        } else if (id == 1) {
            // Reverse Direction
            net.minecraft.core.Direction current = net.minecraft.core.Direction.from2DDataValue(this.getFacingIndex());
            this.data.set(5, current.getOpposite().get2DDataValue());
            return true;
        } else if (id == 2) {
            // Speed down
            int current = this.getSpeedLevel();
            if (current > 1) {
                this.data.set(6, current - 1);
            }
            return true;
        } else if (id == 3) {
            // Speed up
            int current = this.getSpeedLevel();
            if (current < 5) {
                this.data.set(6, current + 1);
            }
            return true;
        } else if (id == 4) {
            // Horn
            if (this.container instanceof ElectricLocomotiveEntity loco) {
                loco.blowHorn(player);
            }
            return true;
        } else if (id == 5) {
            // Toggle Headlight
            int next = this.isHeadlightEnabled() ? 0 : 1;
            this.data.set(7, next);
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
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < CONTAINER_SIZE) {
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, CONTAINER_SIZE, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
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
