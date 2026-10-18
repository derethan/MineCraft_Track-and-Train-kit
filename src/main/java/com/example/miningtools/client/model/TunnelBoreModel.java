package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.TunnelBoreRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TunnelBoreModel extends EntityModel<TunnelBoreRenderState> {
    private final ModelPart boreHead;
    private final ModelPart furnaceActive;
    private final ModelPart furnaceIdle;

    public TunnelBoreModel(ModelPart root) {
        super(root);
        this.boreHead = root.getChild("bore_head");
        this.furnaceActive = root.getChild("furnace_active");
        this.furnaceIdle = root.getChild("furnace_idle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Base plate (80 x 2 x 16)
        root.addOrReplaceChild(
                "base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-40.0F, 3.0F, -8.0F, 80.0F, 2.0F, 16.0F),
                PartPose.ZERO
        );

        // Rear engine / ballast compartment (42 x 24 x 24)
        root.addOrReplaceChild(
                "rear",
                CubeListBuilder.create().texOffs(0, 18).addBox(6.0F, -21.0F, -12.0F, 42.0F, 24.0F, 24.0F),
                PartPose.ZERO
        );

        // Central boiler / furnace box (24 x 32 x 32)
        root.addOrReplaceChild(
                "furnace_box",
                CubeListBuilder.create().texOffs(99, 112).addBox(-18.0F, -29.0F, -16.0F, 24.0F, 32.0F, 32.0F),
                PartPose.ZERO
        );

        // Furnace idle side grates
        root.addOrReplaceChild(
                "furnace_idle",
                CubeListBuilder.create().texOffs(0, 66)
                        .addBox(-14.0F, -19.0F, 16.0F, 16.0F, 16.0F, 2.0F)
                        .addBox(-14.0F, -19.0F, -18.0F, 16.0F, 16.0F, 2.0F),
                PartPose.ZERO
        );

        // Furnace active glowing firebox doors
        root.addOrReplaceChild(
                "furnace_active",
                CubeListBuilder.create().texOffs(37, 66)
                        .addBox(-14.0F, -19.0F, 16.0F, 16.0F, 16.0F, 2.0F)
                        .addBox(-14.0F, -19.0F, -18.0F, 16.0F, 16.0F, 2.0F),
                PartPose.ZERO
        );

        // Dual smokestacks (4 x 16 x 4 each)
        root.addOrReplaceChild(
                "stack",
                CubeListBuilder.create().texOffs(74, 66)
                        .addBox(-8.0F, -35.0F, 13.0F, 4.0F, 16.0F, 4.0F)
                        .addBox(-8.0F, -35.0F, -17.0F, 4.0F, 16.0F, 4.0F),
                PartPose.ZERO
        );

        // Heavy forward cutting shroud (32 x 43 x 48)
        root.addOrReplaceChild(
                "bore_frame",
                CubeListBuilder.create().texOffs(96, 18).addBox(-50.0F, -40.0F, -24.0F, 32.0F, 43.0F, 48.0F),
                PartPose.ZERO
        );

        // Rotating Iron Bore Head (2 x 32 x 32 disc with cutting teeth)
        root.addOrReplaceChild(
                "bore_head",
                CubeListBuilder.create().texOffs(59, 78).addBox(-52.0F, -16.0F, -16.0F, 2.0F, 32.0F, 32.0F),
                PartPose.offset(0.0F, -18.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(TunnelBoreRenderState state) {
        super.setupAnim(state);
        this.boreHead.xRot = state.boreHeadRotation;
        this.furnaceActive.visible = state.active;
        this.furnaceIdle.visible = !state.active;
    }
}
