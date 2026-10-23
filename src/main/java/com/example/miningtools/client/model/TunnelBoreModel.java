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

        // 1. ROTATING BORE HEAD (Circular cutting wheel with teeth and roller discs)
        // Pivots around central drilling axis (0, -18, 0)
        root.addOrReplaceChild(
                "bore_head",
                CubeListBuilder.create()
                        // Main circular cutting wheel disc (diameter 36, thickness 3)
                        .texOffs(0, 0)
                        .addBox(-53.0F, -18.0F, -18.0F, 3.0F, 36.0F, 36.0F)
                        // Center hub cone (diameter 10, thickness 2)
                        .texOffs(80, 0)
                        .addBox(-55.0F, -5.0F, -5.0F, 2.0F, 10.0F, 10.0F)
                        // 3D Scraper teeth around rim (red bits)
                        .texOffs(106, 0)
                        .addBox(-55.0F, -19.0F, -3.0F, 2.0F, 4.0F, 6.0F)
                        .addBox(-55.0F, 15.0F, -3.0F, 2.0F, 4.0F, 6.0F)
                        .addBox(-55.0F, -3.0F, -19.0F, 2.0F, 6.0F, 4.0F)
                        .addBox(-55.0F, -3.0F, 15.0F, 2.0F, 6.0F, 4.0F)
                        // 3D Disc cutters (dark roller wheels)
                        .texOffs(106, 20)
                        .addBox(-54.5F, -12.0F, -12.0F, 2.0F, 5.0F, 5.0F)
                        .addBox(-54.5F, -12.0F, 7.0F, 2.0F, 5.0F, 5.0F)
                        .addBox(-54.5F, 7.0F, -12.0F, 2.0F, 5.0F, 5.0F)
                        .addBox(-54.5F, 7.0F, 7.0F, 2.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -18.0F, 0.0F)
        );

        // 2. ROUNDED CYLINDRICAL TBM SHIELD BODY (Industrial Grey with Bold Red Stripe)
        // Horizontal cylinder facet (56 long, 26 high, 40 wide)
        root.addOrReplaceChild(
                "shield_horizontal",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -31.0F, -20.0F, 56.0F, 26.0F, 40.0F),
                PartPose.ZERO
        );

        // Vertical cylinder cross facet (56 long, 40 high, 26 wide)
        root.addOrReplaceChild(
                "shield_vertical",
                CubeListBuilder.create().texOffs(0, 142)
                        .addBox(-50.0F, -38.0F, -13.0F, 56.0F, 40.0F, 26.0F),
                PartPose.ZERO
        );

        // Corner bevels forming the 12-sided rounded circular cylinder shield
        root.addOrReplaceChild(
                "shield_bevels",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -36.0F, -18.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -36.0F, 12.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -6.0F, -18.0F, 56.0F, 6.0F, 6.0F)
                        .addBox(-50.0F, -6.0F, 12.0F, 56.0F, 6.0F, 6.0F),
                PartPose.ZERO
        );

        // Raised collar ring directly behind cutterhead
        root.addOrReplaceChild(
                "shield_collar",
                CubeListBuilder.create().texOffs(0, 74)
                        .addBox(-50.0F, -32.0F, -20.5F, 3.0F, 28.0F, 41.0F)
                        .addBox(-50.0F, -39.0F, -13.5F, 3.0F, 42.0F, 27.0F),
                PartPose.ZERO
        );

        // 3. REAR MACHINERY & SERVICE GANTRY (Deep Industrial Black with Operator Station)
        root.addOrReplaceChild(
                "gantry_machinery",
                CubeListBuilder.create().texOffs(130, 0)
                        // Main black machinery housing
                        .addBox(6.0F, -22.0F, -13.0F, 36.0F, 24.0F, 26.0F)
                        // Operator control console on roof
                        .addBox(8.0F, -26.0F, -6.0F, 5.0F, 4.0F, 12.0F)
                        // Operator bucket seat
                        .texOffs(106, 20)
                        .addBox(13.0F, -28.0F, -5.0F, 5.0F, 6.0F, 10.0F),
                PartPose.ZERO
        );

        // Dual industrial exhaust silencers / cooling stacks (Black)
        root.addOrReplaceChild(
                "exhaust_stacks",
                CubeListBuilder.create().texOffs(180, 52)
                        .addBox(24.0F, -32.0F, -9.0F, 6.0F, 10.0F, 5.0F)
                        .addBox(24.0F, -32.0F, 4.0F, 6.0F, 10.0F, 5.0F),
                PartPose.ZERO
        );

        // 4. FURNACE FIREBOX VIEWPORTS (Active & Idle)
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

        // 5. RAIL UNDERCARRIAGE, WHEELS & BOGIES (Black)
        root.addOrReplaceChild(
                "undercarriage",
                CubeListBuilder.create().texOffs(0, 218)
                        // Main heavy steel base deck
                        .addBox(-44.0F, 2.0F, -9.0F, 86.0F, 2.0F, 18.0F)
                        // Front bogie frame
                        .addBox(-30.0F, 0.0F, -10.0F, 14.0F, 3.0F, 20.0F)
                        // Rear bogie frame
                        .addBox(16.0F, 0.0F, -10.0F, 14.0F, 3.0F, 20.0F)
                        // Flanged steel track wheels
                        .addBox(-28.0F, 1.0F, -11.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(-28.0F, 1.0F, 9.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(18.0F, 1.0F, -11.0F, 10.0F, 3.0F, 2.0F)
                        .addBox(18.0F, 1.0F, 9.0F, 10.0F, 3.0F, 2.0F)
                        // Rear tow coupler
                        .addBox(42.0F, -1.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.ZERO
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
