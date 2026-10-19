package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.CargoMinecartEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CargoMinecartMenu extends AbstractContainerMenu {
    public static final int STORAGE_SIZE = CargoMinecartEntity.STORAGE_SLOTS; // 27
    public static final int FILTER_SLOT = CargoMinecartEntity.FILTER_SLOT;   // 27
    public static final int TOTAL_SIZE = CargoMinecartEntity.TOTAL_SLOTS;    // 28

    private final Container container;

    // Client-side constructor
    public CargoMinecartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_SIZE));
    }

    // Server-side constructor
    public CargoMinecartMenu(int containerId, Inventory playerInventory, Container container) {
        super(MiningTools.CARGO_MINECART_MENU.get(), containerId);
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
                if (!CargoMinecartEntity.isValidFilterCandidate(stack)) {
                    return false;
                }
                if (container instanceof CargoMinecartEntity cart) {
                    if (cart.hasStoredItems() && !cart.getFilterItem().isEmpty()) {
                        return false;
                    }
                    return !cart.hasConflictingItems(stack);
                }
                return true;
            }

            @Override
            public boolean mayPickup(Player player) {
                if (container instanceof CargoMinecartEntity cart) {
                    return !cart.hasStoredItems();
                }
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory & Hotbar (3 rows at y=104, hotbar at y=162)
        this.addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId == FILTER_SLOT && this.container instanceof CargoMinecartEntity cart) {
            if (cart.hasStoredItems()) {
                if (!cart.getFilterItem().isEmpty()) {
                    player.sendSystemMessage(
                            Component.translatable("message.miningtools.cargo_minecart.filter_locked").withStyle(ChatFormatting.RED)
                    );
                    return;
                }
            }
            ItemStack carried = this.getCarried();
            if (CargoMinecartEntity.isValidFilterCandidate(carried) && cart.hasConflictingItems(carried)) {
                player.sendSystemMessage(
                        Component.translatable("message.miningtools.cargo_minecart.conflict").withStyle(ChatFormatting.RED)
                );
                return;
            }
        }
        super.clicked(slotId, button, containerInput, player);
    }

    public CargoMinecartEntity.FilterType getFilterType() {
        if (this.container instanceof CargoMinecartEntity cart) {
            return cart.getFilterType();
        }
        ItemStack filter = this.container.getItem(FILTER_SLOT);
        if (CargoMinecartEntity.isLogItem(filter)) return CargoMinecartEntity.FilterType.LOG;
        if (CargoMinecartEntity.isWoodItem(filter)) return CargoMinecartEntity.FilterType.WOOD;
        return CargoMinecartEntity.FilterType.NONE;
    }

    public boolean isLogFilterActive() {
        return getFilterType() == CargoMinecartEntity.FilterType.LOG;
    }

    public boolean isWoodFilterActive() {
        return getFilterType() == CargoMinecartEntity.FilterType.WOOD;
    }

    public ItemStack getFilterItem() {
        return this.container.getItem(FILTER_SLOT);
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

            if (slotIndex == FILTER_SLOT) {
                // Moving filter item from filter slot to player inventory
                if (this.container instanceof CargoMinecartEntity cart && cart.hasStoredItems()) {
                    player.sendSystemMessage(
                            Component.translatable("message.miningtools.cargo_minecart.filter_locked").withStyle(ChatFormatting.RED)
                    );
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, TOTAL_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < TOTAL_SIZE) {
                // Moving from cart storage slots (0..26) to player inventory
                if (!this.moveItemStackTo(stackInSlot, TOTAL_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to cart storage slots 0..26
                if (this.container instanceof CargoMinecartEntity cart) {
                    CargoMinecartEntity.FilterType filterType = cart.getFilterType();
                    if (filterType == CargoMinecartEntity.FilterType.LOG && !CargoMinecartEntity.isLogItem(stackInSlot)) {
                        return ItemStack.EMPTY;
                    }
                    if (filterType == CargoMinecartEntity.FilterType.WOOD && !CargoMinecartEntity.isWoodItem(stackInSlot)) {
                        return ItemStack.EMPTY;
                    }
                }
                if (!this.moveItemStackTo(stackInSlot, 0, STORAGE_SIZE, false)) {
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
