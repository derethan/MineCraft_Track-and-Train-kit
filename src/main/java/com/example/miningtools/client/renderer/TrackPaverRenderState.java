package com.example.miningtools.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackPaverRenderState extends EntityRenderState {
    public Direction facing = Direction.NORTH;
    public float yRot = 0.0F;
    public boolean active = false;
    public boolean paving = true;
    public boolean onRails = false;
    public float treadRotation = 0.0F;
    public float hurtTime = 0.0F;
    public int hurtDir = 1;
    public float damageTime = 0.0F;
    public boolean workLight = false;
    public boolean showHologram = false;
    public boolean isWorkMode = true;
    public BlockPos[] hologramPositions = new BlockPos[9];
}
