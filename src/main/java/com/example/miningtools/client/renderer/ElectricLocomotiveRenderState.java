package com.example.miningtools.client.renderer;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElectricLocomotiveRenderState extends EntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean active = false;
    public boolean poweredFromRail = false;
    public boolean headlightActive = false;
    public float hurtTime = 0.0F;
    public int hurtDir = 1;
    public float damageTime = 0.0F;
    public @Nullable Vec3 posOnRail = null;
    public @Nullable Vec3 frontPos = null;
    public @Nullable Vec3 backPos = null;
    public float pitch = 0.0F;
    public float trackYaw = 0.0F;
}
