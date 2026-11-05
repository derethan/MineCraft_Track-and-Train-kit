package com.example.miningtools.guide;

import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.function.Supplier;

public record GuideRecipe(
        String title,
        List<Supplier<ItemStack>> inputs,
        List<Supplier<ItemStack>> outputs,
        String notes
) {
    public List<ItemStack> getInputStacks() {
        return inputs.stream().map(Supplier::get).toList();
    }

    public List<ItemStack> getOutputStacks() {
        return outputs.stream().map(Supplier::get).toList();
    }
}
