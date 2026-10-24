package com.example.miningtools.client.renderer;

import com.example.miningtools.entity.AggregateHopperCartEntity;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AggregateHopperCartRenderState extends MinecartRenderState {
    public int storedCount = 0;
    public AggregateHopperCartEntity.AggregateType aggregateType = AggregateHopperCartEntity.AggregateType.NONE;
    public ItemStack filterItem = ItemStack.EMPTY;
}
