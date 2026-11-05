package com.example.miningtools.guide;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record LayerSymbol(char symbol, String name, int color, @Nullable Supplier<ItemStack> itemSupplier) {
    public ItemStack getItemStack() {
        return itemSupplier != null ? itemSupplier.get() : ItemStack.EMPTY;
    }
}
