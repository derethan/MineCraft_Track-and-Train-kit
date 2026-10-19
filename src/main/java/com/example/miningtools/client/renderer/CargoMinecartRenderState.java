package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CargoMinecartRenderState extends MinecartRenderState {
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isLogFilter = false;
    public int storedLogCount = 0;
}
