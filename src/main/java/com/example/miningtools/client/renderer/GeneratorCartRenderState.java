package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;

public class GeneratorCartRenderState extends MinecartRenderState {
    public boolean isGenerating = false;
    public float burnPercent = 0.0F;
    public float waterPercent = 0.0F;
    public float energyPercent = 0.0F;
}
