package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.AggregateHopperCartEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AggregateHopperCartMenu extends AbstractContainerMenu {
    public static final int STORAGE_SIZE = AggregateHopperCartEntity.STORAGE_SLOTS; // 27
    public static final int FILTER_SLOT = AggregateHopperCartEntity.FILTER_SLOT;   // 27
    public static final int TOTAL_SIZE = AggregateHopperCartEntity.TOTAL_SLOTS;    // 28

    private final Container container;

    // Client-side constructor
    public AggregateHopperCartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SIZE));
    }

    // Server-side constructor
    public AggregateHopperCartMenu(int containerId, Inventory playerInventory, Container container) {
        super(MiningTools.HOPPER_CART_MENU.get(), containerId);
        checkContainerSize(container, TOTAL_SIZE);
        this.container = container;
        container.startOpen(playerInventory.player);

        // Slots 0..26: 3 rows of 9 storage slots at (x=8, y=36)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                this.addSlot(new Slot(container, slotIndex, 8 + col * 18, 36 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(this.getSlotIndex(), stack);
                    }
                });
            }
        }

        // Slot 27: Filter slot at (x=152, y=14)
        this.addSlot(new Slot(container, FILTER_SLOT, 152, 14) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!AggregateHopperCartEntity.isValidAggregate(stack)) {
                    return false;
                }
                if (container instanceof AggregateHopperCartEntity cart) {
                    if (cart.hasStoredItems() && !cart.getFilterItem().isEmpty()) {
                        return false;
                    }
                    return !cart.hasConflictingItems(stack);
                }
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (3 rows of 9) at y=104
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }

        // Player Hotbar (9 slots) at y=162
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }
    }

    public Container getContainer() {
        return this.container;
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

            if (index < STORAGE_SIZE) {
                // From Cart storage to player inventory
                if (!this.moveItemStackTo(slotStack, TOTAL_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == FILTER_SLOT) {
                // From filter slot to player inventory
                if (!this.moveItemStackTo(slotStack, TOTAL_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory into cart storage
                if (this.container.canPlaceItem(0, slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, STORAGE_SIZE, false)) {
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
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
