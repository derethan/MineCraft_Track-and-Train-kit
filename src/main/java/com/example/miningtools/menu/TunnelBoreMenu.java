package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TunnelBoreEntity;

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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;

public class TunnelBoreMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 19;
    public static final int DATA_COUNT = 3;
    public static final int FUEL_SLOT = 0;
    public static final int RAIL_SLOTS_START = 1;
    public static final int RAIL_SLOTS_COUNT = 9;
    public static final int BALLAST_SLOTS_START = 10;
    public static final int BALLAST_SLOTS_COUNT = 9;

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public TunnelBoreMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public TunnelBoreMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.TUNNEL_BORE_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Fuel Slot (Coal / Charcoal)
        this.addSlot(new Slot(container, FUEL_SLOT, 80, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.COALS);
            }
        });

        // Slots 1..9: Track / Rail Slots
        for (int i = 0; i < RAIL_SLOTS_COUNT; i++) {
            this.addSlot(new Slot(container, RAIL_SLOTS_START + i, 8 + i * 18, 54) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BaseRailBlock;
                }
            });
        }

        // Slots 10..18: Ballast Slots (Gravel, Sand, Solid blocks)
        for (int i = 0; i < BALLAST_SLOTS_COUNT; i++) {
            this.addSlot(new Slot(container, BALLAST_SLOTS_START + i, 8 + i * 18, 72) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TunnelBoreEntity.isBallastItem(stack);
                }
            });
        }

        // Player Inventory
        this.addStandardInventorySlots(playerInventory, 8, 102);

        // Synchronize burnTime (0), burnTimeTotal (1), and running (2)
        this.addDataSlots(data);
    }

    public Container getContainer() {
        return this.container;
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

    public int getRunMode() {
        return this.data.get(2);
    }

    public boolean isRunning() {
        return this.getRunMode() != 0;
    }

    public boolean isForward() {
        return this.getRunMode() == 1;
    }

    public boolean isReversing() {
        return this.getRunMode() == 2;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            int nextState = (this.getRunMode() == 1) ? 0 : 1;
            this.data.set(2, nextState);
            return true;
        } else if (id == 1) {
            int nextState = (this.getRunMode() == 2) ? 0 : 2;
            this.data.set(2, nextState);
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
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex < CONTAINER_SIZE) {
                // Moving from Tunnel Bore inventory to Player inventory
                if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from Player inventory to Tunnel Bore
                if (stack.is(ItemTags.COALS)) {
                    // Try fuel slot first
                    if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BaseRailBlock) {
                    // Try track slots
                    if (!this.moveItemStackTo(stack, RAIL_SLOTS_START, RAIL_SLOTS_START + RAIL_SLOTS_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (TunnelBoreEntity.isBallastItem(stack)) {
                    // Try ballast slots (gravel, sand, stone)
                    if (!this.moveItemStackTo(stack, BALLAST_SLOTS_START, BALLAST_SLOTS_START + BALLAST_SLOTS_COUNT, false)) {
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
