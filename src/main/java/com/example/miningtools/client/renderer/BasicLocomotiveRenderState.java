package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BasicLocomotiveRenderState extends EntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean active = false;
    public boolean lit = false;
    public float hurtTime = 0.0F;
    public int hurtDir = 1;
    public float damageTime = 0.0F;
}
