package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.util.TrainChainingUtil;

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

public class DieselLocomotiveMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 2;
    public static final int DATA_COUNT = 10;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public DieselLocomotiveMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public DieselLocomotiveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.DIESEL_LOCOMOTIVE_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Diesel Fuel Container Input (canister/bucket)
        this.addSlot(new Slot(container, 0, 75, 53));
        // Slot 1: Empty Container Output
        this.addSlot(new Slot(container, 1, 95, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory & hotbar
        this.addStandardInventorySlots(playerInventory, 8, 84);

        // Data slots
        this.addDataSlots(data);
    }

    public int getFluidAmount() {
        int low = this.data.get(0) & 0xFFFF;
        int high = this.data.get(1) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getFluidCapacity() {
        int low = this.data.get(8) & 0xFFFF;
        int high = this.data.get(9) & 0xFFFF;
        int cap = (high << 16) | low;
        return cap > 0 ? cap : 16000;
    }

    public float getFluidPercent() {
        int cap = this.getFluidCapacity();
        return cap <= 0 ? 0.0F : Mth.clamp((float) this.getFluidAmount() / (float) cap, 0.0F, 1.0F);
    }

    public boolean isRunning() {
        return this.data.get(2) != 0;
    }

    public int getFacingIndex() {
        return this.data.get(3);
    }

    public int getSpeedLevel() {
        return this.data.get(4);
    }

    public boolean isHeadlightEnabled() {
        return this.data.get(5) != 0;
    }

    public int getEntityId() {
        return this.data.get(6);
    }

    public boolean isReversed() {
        return this.data.get(7) != 0;
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            boolean next = !this.isRunning();
            this.data.set(2, next ? 1 : 0);
            if (this.container instanceof DieselLocomotiveEntity loco) {
                TrainChainingUtil.syncTrainConsist(loco, next, loco.getSpeedLevel(), loco.isReversed());
            }
            return true;
        } else if (id == 1) {
            // Reverse Direction without model rotation
            boolean nextReversed = !this.isReversed();
            this.data.set(7, nextReversed ? 1 : 0);
            if (this.container instanceof DieselLocomotiveEntity loco) {
                TrainChainingUtil.syncTrainConsist(loco, loco.isRunning(), loco.getSpeedLevel(), nextReversed);
            }
            return true;
        } else if (id == 2) {
            // Speed down
            int current = this.getSpeedLevel();
            if (current > 1) {
                int next = current - 1;
                this.data.set(4, next);
                if (this.container instanceof DieselLocomotiveEntity loco) {
                    TrainChainingUtil.syncTrainConsist(loco, loco.isRunning(), next, loco.isReversed());
                }
            }
            return true;
        } else if (id == 3) {
            // Speed up
            int current = this.getSpeedLevel();
            if (current < 5) {
                int next = current + 1;
                this.data.set(4, next);
                if (this.container instanceof DieselLocomotiveEntity loco) {
                    TrainChainingUtil.syncTrainConsist(loco, loco.isRunning(), next, loco.isReversed());
                }
            }
            return true;
        } else if (id == 4) {
            // Horn
            if (this.container instanceof DieselLocomotiveEntity loco) {
                loco.blowHorn(player);
            }
            return true;
        } else if (id == 5) {
            // Toggle Headlight
            int next = this.isHeadlightEnabled() ? 0 : 1;
            this.data.set(5, next);
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
            ItemStack current = slot.getItem();
            itemstack = current.copy();

            if (index < CONTAINER_SIZE) {
                if (!this.moveItemStackTo(current, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
