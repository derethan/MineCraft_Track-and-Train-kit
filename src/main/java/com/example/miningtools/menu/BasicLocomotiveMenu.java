package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;

import net.minecraft.tags.ItemTags;
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

public class BasicLocomotiveMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 3;
    public static final int DATA_COUNT = 7;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public BasicLocomotiveMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public BasicLocomotiveMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.BASIC_LOCOMOTIVE_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // 3 Fuel Slots (Coal / Charcoal only)
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(container, i, 62 + i * 18, 49) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ItemTags.COALS);
                }
            });
        }

        // Standard Player Inventory Slots (3 rows of 9 + 1 hotbar row)
        this.addStandardInventorySlots(playerInventory, 8, 84);

        // Data slots for fuel, fuelTotal, running, facing, speedLevel, headlight
        this.addDataSlots(data);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public float getLitProgress() {
        int total = this.data.get(1);
        if (total <= 0) {
            total = 1600;
        }
        return Mth.clamp((float) this.data.get(0) / (float) total, 0.0F, 1.0F);
    }

    public boolean isRunning() {
        return this.data.get(2) != 0;
    }

    public int getFacingIndex() {
        return this.data.get(3);
    }

    public int getSpeedLevel() {
        int level = this.data.get(4);
        return level <= 0 ? 3 : level;
    }

    public boolean isHeadlightEnabled() {
        return this.data.get(5) != 0;
    }

    public int getEntityId() {
        return this.data.get(6);
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            int nextState = this.isRunning() ? 0 : 1;
            this.data.set(2, nextState);
            return true;
        } else if (id == 1) {
            // Reverse direction
            int currentFacing = this.data.get(3);
            this.data.set(3, (currentFacing + 2) % 4);
            return true;
        } else if (id == 2) {
            // Speed down
            int current = this.getSpeedLevel();
            if (current > 1) {
                this.data.set(4, current - 1);
            }
            return true;
        } else if (id == 3) {
            // Speed up
            int current = this.getSpeedLevel();
            if (current < 5) {
                this.data.set(4, current + 1);
            }
            return true;
        } else if (id == 4) {
            // Blow horn
            if (this.container instanceof BasicLocomotiveEntity loco) {
                loco.blowHorn(player);
            }
            return true;
        } else if (id == 5) {
            // Toggle headlight
            int nextLight = this.isHeadlightEnabled() ? 0 : 1;
            this.data.set(5, nextLight);
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
                // Moving from locomotive to player inventory
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to locomotive (only coals allowed)
                if (stackInSlot.is(ItemTags.COALS)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, CONTAINER_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
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
