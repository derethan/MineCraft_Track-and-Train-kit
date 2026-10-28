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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
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
        state.workLight = entity.isWorkLightEnabled();
        state.showHologram = entity.isHologramEnabled();
        state.isWorkMode = entity.isWorkMode();
        if (state.workLight) {
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        }
        if (state.showHologram) {
            Direction forward = entity.getFacing();
            Direction left = forward.getCounterClockWise();
            Direction right = forward.getClockWise();
            BlockPos currentPos = entity.blockPosition();
            BlockPos stepPos = currentPos;
            int idx = 0;
            for (int step = 1; step <= 3; step++) {
                BlockPos nextPos = stepPos.relative(forward);
                BlockState nextFeet = entity.level().getBlockState(nextPos);
                BlockState nextHead = entity.level().getBlockState(nextPos.above());
                if (nextFeet.isSolid() && !nextHead.isSolid()) {
                    nextPos = nextPos.above();
                } else if (!nextFeet.isSolid() && !entity.level().getBlockState(nextPos.below()).isSolid()) {
                    BlockPos downPos = nextPos.below();
                    if (entity.level().getBlockState(downPos).isSolid() || entity.level().getBlockState(downPos.below()).isSolid()) {
                        nextPos = downPos;
                    }
                }
                stepPos = nextPos;
                BlockPos groundCenter = nextPos.below();
                BlockPos[] row = new BlockPos[] {
                    groundCenter.relative(left),
                    groundCenter,
                    groundCenter.relative(right)
                };
                for (BlockPos colPos : row) {
                    BlockPos finalPos = colPos;
                    if (entity.level().getBlockState(finalPos.above()).isSolid()) {
                        finalPos = finalPos.above();
                    } else if (!entity.level().getBlockState(finalPos).isSolid() && entity.level().getBlockState(finalPos.below()).isSolid()) {
                        finalPos = finalPos.below();
                    }
                    if (idx < 9) {
                        state.hologramPositions[idx++] = finalPos;
                    }
                }
            }
        }
    }

    @Override
    public void submit(TrackPaverRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();

        poseStack.translate(0.0F, state.onRails ? 0.0625F : 0.0F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());

        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F * state.hurtDir);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        // Render 3x3 Ghost Hologram Alignment Grid in front of paver
        if (state.showHologram) {
            int holoColor = state.isWorkMode ? 0xE000E5FF : 0xE0FFAA00;
            for (int i = 0; i < 9; i++) {
                BlockPos bp = state.hologramPositions[i];
                if (bp == null) continue;
                poseStack.pushPose();
                double dx = (double) bp.getX() - state.x;
                double dy = (double) bp.getY() - state.y;
                double dz = (double) bp.getZ() - state.z;
                poseStack.translate(dx, dy, dz);
                submitNodeCollector.submitShapeOutline(poseStack, Shapes.block(), RenderTypes.linesTranslucent(), holoColor, 2.5F, false);
                poseStack.popPose();
            }
        }
    }
}
