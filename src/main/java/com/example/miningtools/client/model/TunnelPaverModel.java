package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.TunnelPaverRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TunnelPaverModel extends EntityModel<TunnelPaverRenderState> {
    private final ModelPart distributorDrum;
    private final ModelPart archScreed;
    private final ModelPart furnaceActive;
    private final ModelPart furnaceIdle;

    public TunnelPaverModel(ModelPart root) {
        super(root);
        this.distributorDrum = root.getChild("distributor_drum");
        this.archScreed = root.getChild("arch_screed");
        this.furnaceActive = root.getChild("furnace_active");
        this.furnaceIdle = root.getChild("furnace_idle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. ROTATING MASONRY DISTRIBUTOR DRUM & ERECTOR ARMS
        // Pivots around central axis (0, -18, 0)
        root.addOrReplaceChild(
                "distributor_drum",
                CubeListBuilder.create()
                        // Central rotating rotary feeder hub
                        .texOffs(80, 0)
                        .addBox(-55.0F, -7.0F, -7.0F, 4.0F, 14.0F, 14.0F)
                        // Horizontal & vertical hydraulic segment erector arms
                        .texOffs(106, 0)
                        .addBox(-54.0F, -19.0F, -3.0F, 3.0F, 38.0F, 6.0F)
                        .addBox(-54.0F, -3.0F, -19.0F, 3.0F, 6.0F, 38.0F)
                        // Corner compaction rollers
                        .texOffs(106, 20)
                        .addBox(-54.5F, -14.0F, -14.0F, 2.0F, 6.0F, 6.0F)
                        .addBox(-54.5F, -14.0F, 8.0F, 2.0F, 6.0F, 6.0F)
                        .addBox(-54.5F, 8.0F, -14.0F, 2.0F, 6.0F, 6.0F)
                        .addBox(-54.5F, 8.0F, 8.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -18.0F, 0.0F)
        );

        // 2. FULL-ARCH HYDRAULIC WALL & CEILING SCREED PLATES (Left Wall, Top Ceiling, Right Wall)
        root.addOrReplaceChild(
                "arch_screed",
                CubeListBuilder.create()
                        // Top Ceiling Paving Screed Plate (spans full 3-block roof arch)
                        .texOffs(0, 0)
                        .addBox(-55.0F, -42.0F, -21.0F, 10.0F, 4.0F, 42.0F)
                        // Left Wall Paving Screed Wing (spans 3-block vertical left wall)
                        .texOffs(0, 0)
                        .addBox(-55.0F, -38.0F, -23.0F, 10.0F, 38.0F, 4.0F)
                        // Right Wall Paving Screed Wing (spans 3-block vertical right wall)
                        .texOffs(0, 0)
                        .addBox(-55.0F, -38.0F, 19.0F, 10.0F, 38.0F, 4.0F)
                        // Hydraulic stabilizing rams behind screed plates
                        .texOffs(106, 20)
                        .addBox(-46.0F, -41.0F, -14.0F, 8.0F, 3.0F, 4.0F)
                        .addBox(-46.0F, -41.0F, 10.0F, 8.0F, 3.0F, 4.0F)
                        .addBox(-46.0F, -28.0F, -22.0F, 8.0F, 4.0F, 3.0F)
                        .addBox(-46.0F, -10.0F, -22.0F, 8.0F, 4.0F, 3.0F)
                        .addBox(-46.0F, -28.0F, 19.0F, 8.0F, 4.0F, 3.0F)
                        .addBox(-46.0F, -10.0F, 19.0F, 8.0F, 4.0F, 3.0F),
                PartPose.ZERO
        );

        // 3. ROUNDED CYLINDRICAL SHIELD BODY (CAT Yellow & Steel Grey with Hazard Stripes)
        root.addOrReplaceChild(
                "shield_horizontal",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -31.0F, -20.0F, 56.0F, 26.0F, 40.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "shield_vertical",
                CubeListBuilder.create().texOffs(0, 142)
                        .addBox(-50.0F, -38.0F, -13.0F, 56.0F, 40.0F, 26.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "shield_bevels",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -36.0F, -18.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -36.0F, 12.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -6.0F, -18.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -6.0F, 12.0F, 56.0F, 6.0F, 6.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "shield_collar",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -32.0F, -20.5F, 3.0F, 28.0F, 41.0F)
                        .addBox(-50.0F, -39.0F, -13.5F, 3.0F, 42.0F, 27.0F),
                PartPose.ZERO
        );

        // 4. REAR MACHINERY & SERVICE GANTRY (Deep Industrial Black with Operator Station)
        root.addOrReplaceChild(
                "gantry_machinery",
                CubeListBuilder.create().texOffs(130, 0)
                        .addBox(6.0F, -22.0F, -13.0F, 36.0F, 24.0F, 26.0F)
                        .addBox(8.0F, -26.0F, -6.0F, 5.0F, 4.0F, 12.0F)
                        .texOffs(106, 20)
                        .addBox(13.0F, -28.0F, -5.0F, 5.0F, 6.0F, 10.0F),
                PartPose.ZERO
        );

        // Dual industrial exhaust silencers / cooling stacks
        root.addOrReplaceChild(
                "exhaust_stacks",
                CubeListBuilder.create().texOffs(180, 52)
                        .addBox(24.0F, -32.0F, -9.0F, 6.0F, 10.0F, 5.0F)
                        .addBox(24.0F, -32.0F, 4.0F, 6.0F, 10.0F, 5.0F),
                PartPose.ZERO
        );

        // 5. FURNACE FIREBOX VIEWPORTS (Active & Idle)
        root.addOrReplaceChild(
                "furnace_idle",
                CubeListBuilder.create().texOffs(210, 64)
                        .addBox(14.0F, -16.0F, 13.0F, 14.0F, 12.0F, 1.0F)
                        .addBox(14.0F, -16.0F, -14.0F, 14.0F, 12.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "furnace_active",
                CubeListBuilder.create().texOffs(210, 80)
                        .addBox(14.0F, -16.0F, 13.0F, 14.0F, 12.0F, 1.0F)
                        .addBox(14.0F, -16.0F, -14.0F, 14.0F, 12.0F, 1.0F),
                PartPose.ZERO
        );

        // 6. RAIL UNDERCARRIAGE, WHEELS & BOGIES
        root.addOrReplaceChild(
                "undercarriage",
                CubeListBuilder.create().texOffs(0, 218)
                        .addBox(-44.0F, 2.0F, -9.0F, 86.0F, 2.0F, 18.0F)
                        .addBox(-30.0F, 0.0F, -10.0F, 14.0F, 3.0F, 20.0F)
                        .addBox(16.0F, 0.0F, -10.0F, 14.0F, 3.0F, 20.0F)
                        .addBox(-28.0F, 1.0F, -11.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(-28.0F, 1.0F, 9.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(18.0F, 1.0F, -11.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(18.0F, 1.0F, 9.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(42.0F, -1.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(TunnelPaverRenderState state) {
        super.setupAnim(state);
        this.distributorDrum.xRot = state.rollerRotation;
        if (state.active && state.isWorkMode) {
            this.archScreed.x = Mth.sin(state.rollerRotation * 2.0F) * 0.6F;
        } else {
            this.archScreed.x = 0.0F;
        }
        this.furnaceActive.visible = state.active;
        this.furnaceIdle.visible = !state.active;
    }
}
