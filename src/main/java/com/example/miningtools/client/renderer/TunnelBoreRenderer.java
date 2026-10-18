package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.TunnelBoreModel;
import com.example.miningtools.entity.TunnelBoreEntity;
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
public class TunnelBoreRenderer extends EntityRenderer<TunnelBoreEntity, TunnelBoreRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/tunnel_bore.png");

    private final TunnelBoreModel model;

    public TunnelBoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.98F;
        this.model = new TunnelBoreModel(context.bakeLayer(MiningToolsClient.TUNNEL_BORE_LAYER));
    }

    @Override
    public TunnelBoreRenderState createRenderState() {
        return new TunnelBoreRenderState();
    }

    @Override
    public void extractRenderState(TunnelBoreEntity entity, TunnelBoreRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.facing = entity.getFacing();
        state.active = entity.isActive();
        state.drilling = entity.isDrilling();
        state.boreHeadRotation = entity.isDrilling()
                ? entity.boreHeadRotation + partialTicks * 0.25F
                : entity.boreHeadRotation;
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
    }

    @Override
    public void submit(TunnelBoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot() + 90.0F);
        // Align model so the cutter head sits flush with the front mining face (dist = 2)
        poseStack.translate(-1.65F, 0.0F, 0.0F);

        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F * state.hurtDir);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
