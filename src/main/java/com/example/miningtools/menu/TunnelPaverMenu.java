package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.TunnelPaverEntity;

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

public class TunnelPaverMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = TunnelPaverEntity.INVENTORY_SIZE;
    public static final int DATA_COUNT = 6;

    public static final int PATTERN_SLOTS_COUNT = TunnelPaverEntity.PATTERN_SLOTS_COUNT;
    public static final int SLOT_PATTERN_LEFT_BOTTOM = TunnelPaverEntity.SLOT_PATTERN_LEFT_BOTTOM;
    public static final int SLOT_PATTERN_LEFT_MID = TunnelPaverEntity.SLOT_PATTERN_LEFT_MID;
    public static final int SLOT_PATTERN_LEFT_TOP = TunnelPaverEntity.SLOT_PATTERN_LEFT_TOP;
    public static final int SLOT_PATTERN_TOP_LEFT = TunnelPaverEntity.SLOT_PATTERN_TOP_LEFT;
    public static final int SLOT_PATTERN_TOP_CENTER = TunnelPaverEntity.SLOT_PATTERN_TOP_CENTER;
    public static final int SLOT_PATTERN_TOP_RIGHT = TunnelPaverEntity.SLOT_PATTERN_TOP_RIGHT;
    public static final int SLOT_PATTERN_RIGHT_TOP = TunnelPaverEntity.SLOT_PATTERN_RIGHT_TOP;
    public static final int SLOT_PATTERN_RIGHT_MID = TunnelPaverEntity.SLOT_PATTERN_RIGHT_MID;
    public static final int SLOT_PATTERN_RIGHT_BOTTOM = TunnelPaverEntity.SLOT_PATTERN_RIGHT_BOTTOM;

    public static final int SLOT_FUEL = TunnelPaverEntity.SLOT_FUEL;
    public static final int SLOT_MATERIAL_START = TunnelPaverEntity.SLOT_MATERIAL_START;
    public static final int SLOT_MATERIAL_COUNT = TunnelPaverEntity.SLOT_MATERIAL_COUNT;

    // Arch slot coordinates: (x, y) for each of the 9 pattern slots (0..8)
    public static final int[][] PATTERN_SLOT_COORDS = new int[][] {
            { 26, 49 }, // 0: Left Wall Bottom
            { 26, 31 }, // 1: Left Wall Middle
            { 26, 13 }, // 2: Left Wall Top
            { 44, 13 }, // 3: Top Ceiling Left
            { 62, 13 }, // 4: Top Ceiling Center
            { 80, 13 }, // 5: Top Ceiling Right
            { 98, 13 }, // 6: Right Wall Top
            { 98, 31 }, // 7: Right Wall Middle
            { 98, 49 }  // 8: Right Wall Bottom
    };

    private final Container container;
    private final ContainerData data;

    // Client-side constructor
    public TunnelPaverMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server-side constructor
    public TunnelPaverMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.TUNNEL_PAVER_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slots 0..8: 9 Arch Pattern Filter Slots (3 Left Wall, 3 Top Ceiling, 3 Right Wall)
        for (int i = 0; i < PATTERN_SLOTS_COUNT; i++) {
            int sx = PATTERN_SLOT_COORDS[i][0];
            int sy = PATTERN_SLOT_COORDS[i][1];
            this.addSlot(new Slot(container, i, sx, sy) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Slot 9: Fuel Slot (Any burnable fuel)
        this.addSlot(new Slot(container, SLOT_FUEL, 8, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BasicLocomotiveEntity.isBurnableFuel(stack);
            }
        });

        // Slots 10..36: 27 Material Inventory Slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = SLOT_MATERIAL_START + col + row * 9;
                this.addSlot(new Slot(container, index, 8 + col * 18, 78 + row * 18));
            }
        }

        // Player Inventory (3 rows of 9 + hotbar)
        this.addStandardInventorySlots(playerInventory, 8, 144);

        this.addDataSlots(data);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getFuel() {
        return this.data.get(0);
    }

    public int getFuelTotal() {
        return this.data.get(1);
    }

    public int getRunMode() {
        return this.data.get(2);
    }

    public boolean isPaving() {
        return this.isWorkMode();
    }

    public boolean isWorkMode() {
        return this.data.get(3) != 0;
    }

    public boolean isWorkLightEnabled() {
        return this.data.get(4) != 0;
    }

    public boolean isHologramEnabled() {
        return this.data.get(5) != 0;
    }

    public int getBurnProgressScaled(int pixels) {
        int total = this.getFuelTotal();
        if (total <= 0) total = 1600;
        int fuel = this.getFuel();
        return (fuel > 0) ? (fuel * pixels / total) : 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle Start / Stop
            int nextMode = (this.getRunMode() == TunnelPaverEntity.MODE_FORWARD)
                    ? TunnelPaverEntity.MODE_STOPPED
                    : TunnelPaverEntity.MODE_FORWARD;
            if (this.container instanceof TunnelPaverEntity paver) {
                if (nextMode != TunnelPaverEntity.MODE_STOPPED && paver.isPaving() && !paver.hasPaverMaterial()) {
                    paver.notifyOutOfMaterial(player);
                    this.data.set(2, TunnelPaverEntity.MODE_STOPPED);
                    return true;
                }
            }
            this.data.set(2, nextMode);
            return true;
        } else if (id == 1) {
            // Toggle Reverse
            int nextMode = (this.getRunMode() == TunnelPaverEntity.MODE_REVERSE)
                    ? TunnelPaverEntity.MODE_STOPPED
                    : TunnelPaverEntity.MODE_REVERSE;
            if (this.container instanceof TunnelPaverEntity paver) {
                if (nextMode != TunnelPaverEntity.MODE_STOPPED && paver.isPaving() && !paver.hasPaverMaterial()) {
                    paver.notifyOutOfMaterial(player);
                    this.data.set(2, TunnelPaverEntity.MODE_STOPPED);
                    return true;
                }
            }
            this.data.set(2, nextMode);
            return true;
        } else if (id == 3) {
            // Toggle Work Mode On / Off
            this.data.set(3, this.isWorkMode() ? 0 : 1);
            return true;
        } else if (id == 4) {
            // Toggle Work Light On / Off
            this.data.set(4, this.isWorkLightEnabled() ? 0 : 1);
            return true;
        } else if (id == 5) {
            // Toggle Hologram Grid On / Off
            this.data.set(5, this.isHologramEnabled() ? 0 : 1);
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
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < CONTAINER_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(slotStack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (BasicLocomotiveEntity.isBurnableFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (slotStack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BaseRailBlock)) {
                    boolean placedInPattern = false;
                    for (int patternSlot = 0; patternSlot < PATTERN_SLOTS_COUNT; patternSlot++) {
                        if (this.slots.get(patternSlot).getItem().isEmpty()) {
                            ItemStack single = slotStack.copyWithCount(1);
                            this.slots.get(patternSlot).set(single);
                            slotStack.shrink(1);
                            placedInPattern = true;
                            break;
                        }
                    }
                    if (!placedInPattern && !this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_MATERIAL_START, CONTAINER_SIZE, false)) {
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
