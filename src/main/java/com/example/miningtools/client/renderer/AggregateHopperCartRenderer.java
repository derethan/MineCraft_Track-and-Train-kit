package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.AggregateHopperCartModel;
import com.example.miningtools.entity.AggregateHopperCartEntity;
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
public class AggregateHopperCartRenderer extends AbstractMinecartRenderer<AggregateHopperCartEntity, AggregateHopperCartRenderState> {
    private static final Identifier TEXTURE_EMPTY =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_empty.png");
    private static final Identifier TEXTURE_COAL =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_coal.png");
    private static final Identifier TEXTURE_GRAVEL =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_gravel.png");
    private static final Identifier TEXTURE_SAND =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_sand.png");
    private static final Identifier TEXTURE_RED_SAND =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_red_sand.png");
    private static final Identifier TEXTURE_DIRT =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_dirt.png");
    private static final Identifier TEXTURE_ORE =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/hopper_cart_ore.png");

    private final AggregateHopperCartModel hopperModel;

    public AggregateHopperCartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CHEST_MINECART);
        this.hopperModel = new AggregateHopperCartModel(context.bakeLayer(MiningToolsClient.HOPPER_CART_LAYER));
    }

    @Override
    public AggregateHopperCartRenderState createRenderState() {
        return new AggregateHopperCartRenderState();
    }

    @Override
    public void extractRenderState(AggregateHopperCartEntity entity, AggregateHopperCartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        CartRenderHelper.updateRailRenderState(entity, state);
        state.storedCount = entity.getStoredCount();
        state.aggregateType = entity.getAggregateType();
        state.filterItem = entity.getFilterItem();
    }

    @Override
    public void submit(AggregateHopperCartRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
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

        Identifier texture;
        if (state.storedCount <= 0) {
            texture = TEXTURE_EMPTY;
        } else {
            texture = switch (state.aggregateType) {
                case COAL -> TEXTURE_COAL;
                case SAND -> TEXTURE_SAND;
                case RED_SAND -> TEXTURE_RED_SAND;
                case DIRT -> TEXTURE_DIRT;
                case RAW_ORE -> TEXTURE_ORE;
                default -> TEXTURE_GRAVEL;
            };
        }

        submitNodeCollector.submitModel(
                this.hopperModel,
                state,
                poseStack,
                texture,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        poseStack.popPose();
    }
}
