package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.LargeLiquidMinecartModel;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LargeLiquidMinecartRenderer extends AbstractMinecartRenderer<LargeLiquidMinecartEntity, LiquidMinecartRenderState> {
    private static final Identifier TEXTURE_LIQUID_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/liquid_cart.png");

    private final LargeLiquidMinecartModel largeCartModel;

    public LargeLiquidMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CHEST_MINECART);
        this.largeCartModel = new LargeLiquidMinecartModel(context.bakeLayer(MiningToolsClient.LARGE_LIQUID_MINECART_LAYER));
    }

    @Override
    public LiquidMinecartRenderState createRenderState() {
        return new LiquidMinecartRenderState();
    }

    @Override
    public void extractRenderState(LargeLiquidMinecartEntity entity, LiquidMinecartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fluidName = entity.getFluidName();
        state.fluidAmount = entity.getFluidAmount();
        state.fillRatio = entity.getFillRatio();
    }

    @Override
    public void submit(LiquidMinecartRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();

        long seed = state.offsetSeed;
        float offsetX = (((float)(seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetY = (((float)(seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetZ = (((float)(seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(offsetX, offsetY, offsetZ);

        if (state.isNewRender) {
            poseStack.rotateDegrees(Axis.YP, state.yRot);
            poseStack.rotateDegrees(Axis.ZP, -state.xRot);
            poseStack.translate(0.0F, 0.375F, 0.0F);
        } else {
            double entityX = state.x;
            double entityY = state.y;
            double entityZ = state.z;
            float xRot = state.xRot;
            float rotation = state.yRot;
            if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
                Vec3 frontPos = state.frontPos;
                Vec3 backPos = state.backPos;
                poseStack.translate(state.posOnRail.x - entityX, (frontPos.y + backPos.y) / 2.0 - entityY, state.posOnRail.z - entityZ);
                Vec3 direction = backPos.add(-frontPos.x, -frontPos.y, -frontPos.z);
                if (direction.length() != 0.0) {
                    direction = direction.normalize();
                    rotation = (float)(Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                    xRot = (float)(Math.atan(direction.y) * 73.0);
                }
            }
            poseStack.translate(0.0F, 0.375F, 0.0F);
            poseStack.rotateDegrees(Axis.YP, 180.0F - rotation);
            poseStack.rotateDegrees(Axis.ZP, -xRot);
        }

        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir);
        }

        poseStack.scale(-0.14F, -0.14F, 0.14F);

        submitNodeCollector.submitModel(
                this.largeCartModel,
                state,
                poseStack,
                TEXTURE_LIQUID_CART,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        poseStack.popPose();
    }
}
