package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.BasicLocomotiveModel;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BasicLocomotiveRenderer extends EntityRenderer<BasicLocomotiveEntity, BasicLocomotiveRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/basic_locomotive.png");

    private final BasicLocomotiveModel model;

    public BasicLocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.65F;
        this.model = new BasicLocomotiveModel(context.bakeLayer(MiningToolsClient.BASIC_LOCOMOTIVE_LAYER));
    }

    @Override
    public BasicLocomotiveRenderState createRenderState() {
        return new BasicLocomotiveRenderState();
    }

    @Override
    public void extractRenderState(BasicLocomotiveEntity entity, BasicLocomotiveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.facing = entity.getFacing();
        state.active = entity.isActive();
        state.lit = entity.isLit();
        state.headlightActive = entity.isHeadlightActive();
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);

        Vec3 pos = entity.getRailPos(state.x, state.y, state.z);
        if (pos != null) {
            state.posOnRail = pos;
            Vec3 p0 = entity.getRailPosOffs(state.x, state.y, state.z, 0.35F);
            Vec3 p1 = entity.getRailPosOffs(state.x, state.y, state.z, -0.35F);
            if (p0 == null) {
                p0 = pos;
            }
            if (p1 == null) {
                p1 = pos;
            }
            state.frontPos = p0;
            state.backPos = p1;

            Vec3 diff = p0.subtract(p1);
            if (diff.lengthSqr() > 1.0E-6D) {
                double dot = diff.x * state.facing.getStepX() + diff.z * state.facing.getStepZ();
                Vec3 forwardDir = (dot >= -0.2D ? diff : diff.reverse()).normalize();
                state.pitch = (float) (Math.atan(forwardDir.y) * 73.0);
                state.trackYaw = (float) (Mth.atan2(forwardDir.z, forwardDir.x) * 180.0 / Math.PI) - 90.0F;
            } else {
                state.pitch = 0.0F;
                state.trackYaw = state.facing.toYRot();
            }
        } else {
            state.posOnRail = null;
            state.frontPos = null;
            state.backPos = null;
            state.pitch = 0.0F;
            state.trackYaw = state.facing.toYRot();
        }
    }

    @Override
    public void submit(BasicLocomotiveRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();

        if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
            poseStack.translate(
                    state.posOnRail.x - state.x,
                    (state.frontPos.y + state.backPos.y) / 2.0 - state.y,
                    state.posOnRail.z - state.z
            );
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.trackYaw + 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.pitch);

        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F * state.hurtDir);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
