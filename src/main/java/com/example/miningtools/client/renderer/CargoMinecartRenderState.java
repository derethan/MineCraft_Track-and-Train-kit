package com.example.miningtools.client.renderer;

import com.example.miningtools.entity.CargoMinecartEntity;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CargoMinecartRenderState extends MinecartRenderState {
    public ItemStack filterItem = ItemStack.EMPTY;
    public CargoMinecartEntity.FilterType filterType = CargoMinecartEntity.FilterType.NONE;
    public int storedCount = 0;
}
