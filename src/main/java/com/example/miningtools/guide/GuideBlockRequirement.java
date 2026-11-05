package com.example.miningtools.guide;

import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

public record GuideBlockRequirement(Supplier<ItemStack> itemSupplier, String name, int count) {
    public ItemStack getItemStack() {
        ItemStack stack = itemSupplier.get();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(Math.min(count, 64));
        return copy;
    }
}
