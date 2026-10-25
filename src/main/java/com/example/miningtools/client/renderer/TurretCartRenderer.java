package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.TurretCartModel;
import com.example.miningtools.entity.TurretCartEntity;
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
public class TurretCartRenderer extends AbstractMinecartRenderer<TurretCartEntity, TurretCartRenderState> {
    private static final Identifier TEXTURE_TURRET_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/turret_cart.png");

    private final TurretCartModel model;

    public TurretCartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CHEST_MINECART);
        this.model = new TurretCartModel(context.bakeLayer(MiningToolsClient.TURRET_CART_LAYER));
    }

    @Override
    public TurretCartRenderState createRenderState() {
        return new TurretCartRenderState();
    }

    @Override
    public void extractRenderState(TurretCartEntity entity, TurretCartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        CartRenderHelper.updateRailRenderState(entity, state);
        state.turretYaw = Mth.rotLerp(partialTicks, entity.clientTurretYawO, entity.clientTurretYaw);
        state.hasBow0 = entity.hasBowInSlot(0);
        state.hasBow1 = entity.hasBowInSlot(1);
    }

    @Override
    public void submit(TurretCartRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
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

        poseStack.scale(-1.0F, -1.0F, 1.0F);

        submitNodeCollector.submitModel(
                this.model,
                state,
                poseStack,
                TEXTURE_TURRET_CART,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        poseStack.popPose();
    }
}
