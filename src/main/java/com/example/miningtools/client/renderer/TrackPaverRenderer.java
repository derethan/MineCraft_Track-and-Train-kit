package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.TrackPaverModel;
import com.example.miningtools.entity.TrackPaverEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackPaverRenderer extends EntityRenderer<TrackPaverEntity, TrackPaverRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/track_paver.png");

    private final TrackPaverModel model;

    public TrackPaverRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.25F;
        this.model = new TrackPaverModel(context.bakeLayer(MiningToolsClient.TRACK_PAVER_LAYER));
    }

    @Override
    public TrackPaverRenderState createRenderState() {
        return new TrackPaverRenderState();
    }

    @Override
    public void extractRenderState(TrackPaverEntity entity, TrackPaverRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.facing = entity.getFacing();
        state.yRot = entity.getYRot();
        state.active = entity.isActive();
        state.paving = entity.isPaving();
        state.onRails = entity.isOnRails();
        state.treadRotation = entity.treadRotation;
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
    }

    @Override
    public void submit(TrackPaverRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());

        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F * state.hurtDir);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
