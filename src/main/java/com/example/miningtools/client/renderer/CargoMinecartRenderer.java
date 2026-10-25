package com.example.miningtools.client.renderer;

import com.example.miningtools.MiningTools;
import com.example.miningtools.MiningToolsClient;
import com.example.miningtools.client.model.LoggingCartModel;
import com.example.miningtools.entity.CargoMinecartEntity;
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
public class CargoMinecartRenderer extends AbstractMinecartRenderer<CargoMinecartEntity, CargoMinecartRenderState> {
    private static final Identifier TEXTURE_LOGGING_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/logging_cart.png");
    private static final Identifier TEXTURE_WOOD_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/wood_cart.png");
    private static final Identifier TEXTURE_ORE_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/ore_cart.png");
    private static final Identifier TEXTURE_INGOT_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/ingot_cart.png");
    private static final Identifier TEXTURE_CONTAINER_CART =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/container_cart.png");
    private static final Identifier TEXTURE_CONTAINER_CART_RED =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/container_cart_red.png");
    private static final Identifier TEXTURE_CONTAINER_CART_GREEN =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/container_cart_green.png");
    private static final Identifier TEXTURE_CONTAINER_CART_BLUE =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/entity/container_cart_blue.png");

    private final LoggingCartModel loggingCartModel;

    public CargoMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CHEST_MINECART);
        this.loggingCartModel = new LoggingCartModel(context.bakeLayer(MiningToolsClient.LOGGING_CART_LAYER));
    }

    @Override
    public CargoMinecartRenderState createRenderState() {
        return new CargoMinecartRenderState();
    }

    @Override
    public void extractRenderState(CargoMinecartEntity entity, CargoMinecartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        CartRenderHelper.updateRailRenderState(entity, state);
        state.filterItem = entity.getFilterItem();
        state.filterType = entity.getFilterType();
        state.storedCount = entity.getStoredCount();
        state.containerColorIndex = Math.abs(entity.getUUID().hashCode()) % 3;
    }

    @Override
    public void submit(CargoMinecartRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
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
        if (state.filterType == CargoMinecartEntity.FilterType.LOG) {
            texture = TEXTURE_LOGGING_CART;
        } else if (state.filterType == CargoMinecartEntity.FilterType.WOOD) {
            texture = TEXTURE_WOOD_CART;
        } else if (state.filterType == CargoMinecartEntity.FilterType.ORE) {
            texture = TEXTURE_ORE_CART;
        } else if (state.filterType == CargoMinecartEntity.FilterType.INGOT) {
            texture = TEXTURE_INGOT_CART;
        } else if (state.storedCount > 0) {
            texture = switch (state.containerColorIndex) {
                case 1 -> TEXTURE_CONTAINER_CART_GREEN;
                case 2 -> TEXTURE_CONTAINER_CART_BLUE;
                default -> TEXTURE_CONTAINER_CART_RED;
            };
        } else {
            texture = TEXTURE_WOOD_CART;
        }

        submitNodeCollector.submitModel(
                this.loggingCartModel,
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
