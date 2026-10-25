package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TurretCartEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

public class TurretCartMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = TurretCartEntity.TOTAL_SLOTS; // 11
    public static final int BOW_SLOT_0 = 0;
    public static final int BOW_SLOT_1 = 1;
    public static final int ARROW_SLOTS_START = TurretCartEntity.FIRST_ARROW_SLOT; // 2
    public static final int ARROW_SLOTS_COUNT = TurretCartEntity.ARROW_SLOTS; // 9

    private final Container container;

    // Client-side constructor
    public TurretCartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    // Server-side constructor
    public TurretCartMenu(int containerId, Inventory playerInventory, Container container) {
        super(MiningTools.TURRET_CART_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        this.container = container;
        container.startOpen(playerInventory.player);

        // Slot 0 & 1: Bow Mounts
        this.addSlot(new Slot(container, BOW_SLOT_0, 62, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BowItem;
            }
        });

        this.addSlot(new Slot(container, BOW_SLOT_1, 98, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BowItem;
            }
        });

        // Slots 2..10: Arrow Magazine (9 slots)
        for (int i = 0; i < ARROW_SLOTS_COUNT; i++) {
            this.addSlot(new Slot(container, ARROW_SLOTS_START + i, 8 + i * 18, 48) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ArrowItem;
                }
            });
        }

        // Player Inventory (3 rows of 9 slots at Y = 84, hotbar at Y = 142)
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex < CONTAINER_SIZE) {
                // Moving from Turret Cart to Player inventory
                if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from Player inventory to Turret Cart
                if (stack.getItem() instanceof BowItem) {
                    // Try bow slots first
                    if (!this.moveItemStackTo(stack, BOW_SLOT_0, BOW_SLOT_1 + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getItem() instanceof ArrowItem) {
                    // Try arrow magazine slots
                    if (!this.moveItemStackTo(stack, ARROW_SLOTS_START, ARROW_SLOTS_START + ARROW_SLOTS_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
