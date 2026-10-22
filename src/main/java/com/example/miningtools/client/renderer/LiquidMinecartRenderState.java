package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LiquidMinecartRenderState extends MinecartRenderState {
    public String fluidName = "";
    public int fluidAmount = 0;
    public float fillRatio = 0.0F;
}
