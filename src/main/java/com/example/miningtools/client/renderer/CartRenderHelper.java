package com.example.miningtools.client.renderer;

import com.example.miningtools.util.TrainChainingUtil;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CartRenderHelper {
    public static void updateRailRenderState(AbstractMinecart entity, MinecartRenderState state) {
        Vec3 pos = TrainChainingUtil.getRailPosForEntity(entity, state.x, state.y, state.z);
        if (pos != null) {
            state.posOnRail = pos;
            Direction facing = entity.getMotionDirection();
            if (facing.getAxis().isVertical()) {
                facing = Direction.fromYRot(entity.getYRot());
            }
            if (facing.getAxis().isVertical()) {
                facing = Direction.NORTH;
            }
            double radius = 0.35D;
            Vec3 p0 = TrainChainingUtil.getRailPosOffs(entity, state.x, state.y, state.z, radius, facing);
            Vec3 p1 = TrainChainingUtil.getRailPosOffs(entity, state.x, state.y, state.z, -radius, facing);
            if (p0 == null) p0 = pos;
            if (p1 == null) p1 = pos;
            state.frontPos = p0;
            state.backPos = p1;

            Vec3 direction = p1.subtract(p0);
            if (direction.lengthSqr() > 1.0E-6D) {
                direction = direction.normalize();
                state.yRot = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                state.xRot = (float) (Math.atan(direction.y) * 73.0);
            }
        }
    }
}
