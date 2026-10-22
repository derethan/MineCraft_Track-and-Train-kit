package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CabooseRenderState extends MinecartRenderState {
    public boolean isAnchoring = true;
    public int forcedChunkCount = 0;
}
