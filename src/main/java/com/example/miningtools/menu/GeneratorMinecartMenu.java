package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.GeneratorMinecartEntity;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class GeneratorMinecartMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 4;
    public static final int DATA_COUNT = 11;

    public static final int FUEL_SLOT = 0;
    public static final int WATER_IN_SLOT = 1;
    public static final int BUCKET_OUT_SLOT = 2;
    public static final int ENERGY_ITEM_SLOT = 3;

    private final Container container;
    private final ContainerData data;

    public GeneratorMinecartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    public GeneratorMinecartMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.GENERATOR_MINECART_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        // Slot 0: Fuel Slot (Center)
        this.addSlot(new Slot(container, FUEL_SLOT, 80, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GeneratorMinecartEntity.getFuelBurnDuration(stack) > 0;
            }
        });

        // Slot 1: Water In Container Slot (Left top)
        this.addSlot(new Slot(container, WATER_IN_SLOT, 34, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.is(Items.WATER_BUCKET)) return true;
                FluidStack fluid = FluidUtil.getFirstStackContained(stack);
                return fluid != null && !fluid.isEmpty() && fluid.getFluid() == Fluids.WATER;
            }
        });

        // Slot 2: Empty Bucket Out Slot (Left bottom)
        this.addSlot(new Slot(container, BUCKET_OUT_SLOT, 34, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Battery / Energy Item Slot (Right)
        this.addSlot(new Slot(container, ENERGY_ITEM_SLOT, 126, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Energy.ITEM) != null;
            }
        });

        // Player Inventory (27 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getFluidAmount() {
        int low = this.data.get(0) & 0xFFFF;
        int high = this.data.get(1) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getWaterCapacity() {
        int low = this.data.get(2) & 0xFFFF;
        int high = this.data.get(3) & 0xFFFF;
        int cap = (high << 16) | low;
        return cap > 0 ? cap : 8000;
    }

    public float getWaterPercent() {
        int cap = this.getWaterCapacity();
        return cap <= 0 ? 0.0F : Mth.clamp((float) this.getFluidAmount() / (float) cap, 0.0F, 1.0F);
    }

    public int getEnergyStored() {
        int low = this.data.get(4) & 0xFFFF;
        int high = this.data.get(5) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getEnergyCapacity() {
        int low = this.data.get(6) & 0xFFFF;
        int high = this.data.get(7) & 0xFFFF;
        int cap = (high << 16) | low;
        return cap > 0 ? cap : 100000;
    }

    public float getEnergyPercent() {
        int cap = this.getEnergyCapacity();
        return cap <= 0 ? 0.0F : Mth.clamp((float) this.getEnergyStored() / (float) cap, 0.0F, 1.0F);
    }

    public int getBurnTime() {
        return this.data.get(8);
    }

    public int getBurnTotal() {
        return this.data.get(9);
    }

    public float getBurnPercent() {
        int total = this.getBurnTotal();
        if (total <= 0) return 0.0F;
        return Mth.clamp((float) this.getBurnTime() / (float) total, 0.0F, 1.0F);
    }

    public int getGenerationRate() {
        return this.data.get(10);
    }

    public boolean isGenerating() {
        return this.getGenerationRate() > 0;
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

            if (index < CONTAINER_SIZE) {
                // Move from cart into player inventory
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player into cart
                if (GeneratorMinecartEntity.getFuelBurnDuration(stackInSlot) > 0) {
                    if (!this.moveItemStackTo(stackInSlot, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(Items.WATER_BUCKET) || (FluidUtil.getFirstStackContained(stackInSlot) != null && FluidUtil.getFirstStackContained(stackInSlot).getFluid() == Fluids.WATER)) {
                    if (!this.moveItemStackTo(stackInSlot, WATER_IN_SLOT, WATER_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ItemAccess.forStack(stackInSlot.copyWithCount(1)).getCapability(Capabilities.Energy.ITEM) != null) {
                    if (!this.moveItemStackTo(stackInSlot, ENERGY_ITEM_SLOT, ENERGY_ITEM_SLOT + 1, false)) {
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
