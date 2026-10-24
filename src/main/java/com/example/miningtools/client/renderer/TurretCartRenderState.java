package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurretCartRenderState extends MinecartRenderState {
    public float turretYaw = 0.0F;
    public boolean hasBow0 = false;
    public boolean hasBow1 = false;
}
