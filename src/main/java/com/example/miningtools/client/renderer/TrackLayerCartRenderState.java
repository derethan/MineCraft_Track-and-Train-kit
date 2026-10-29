package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackLayerCartRenderState extends MinecartRenderState {
    public int workMode = 0;
    public boolean active = false;
    public float rollerRotation = 0.0F;
}
