package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.LiquidMinecartRenderState;

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
public class LiquidMinecartModel extends EntityModel<LiquidMinecartRenderState> {
    private final ModelPart tankMain;
    private final ModelPart tankDome;

    public LiquidMinecartModel(ModelPart root) {
        super(root);
        this.tankMain = root.getChild("tank_main");
        this.tankDome = root.getChild("tank_dome");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Flatbed Deck & Undercarriage Frame (Matches CargoMinecart exactly)
        root.addOrReplaceChild(
                "chassis",
                CubeListBuilder.create()
                        // Diamond-plate flatbed deck (24 x 2 x 16)
                        .texOffs(0, 0).addBox(-12.0F, 2.0F, -8.0F, 24.0F, 2.0F, 16.0F)
                        // Undercarriage center beam (18 x 2 x 8)
                        .texOffs(0, 20).addBox(-9.0F, 4.0F, -4.0F, 18.0F, 2.0F, 8.0F)
                        // Front Coupler / Hitch (2 x 2 x 4)
                        .texOffs(0, 32).addBox(-14.0F, 3.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // Back Coupler / Hitch (2 x 2 x 4)
                        .texOffs(0, 32).addBox(12.0F, 3.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // 4 Rail Wheels (Flanged steel wheels tucked under the deck)
                        .texOffs(54, 20).addBox(-8.0F, 3.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(68, 20).addBox(4.0F, 3.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(82, 20).addBox(-8.0F, 3.0F, 5.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(96, 20).addBox(4.0F, 3.0F, 5.0F, 4.0F, 4.0F, 2.0F),
                PartPose.ZERO
        );

        // 2. Heavy Saddle Cradle & Hold-Down Straps
        root.addOrReplaceChild(
                "cradle",
                CubeListBuilder.create()
                        // Front wooden/steel saddle support
                        .texOffs(64, 40).addBox(-8.0F, 0.5F, -7.0F, 3.0F, 2.0F, 14.0F)
                        // Back wooden/steel saddle support
                        .texOffs(64, 40).addBox(5.0F, 0.5F, -7.0F, 3.0F, 2.0F, 14.0F)
                        // Front strap
                        .texOffs(80, 0).addBox(-7.5F, -9.0F, -6.2F, 2.0F, 10.0F, 0.5F)
                        .texOffs(80, 0).addBox(-7.5F, -9.0F, 5.7F, 2.0F, 10.0F, 0.5F)
                        .texOffs(80, 10).addBox(-7.5F, -9.2F, -6.2F, 2.0F, 0.5F, 12.4F)
                        // Back strap
                        .texOffs(88, 0).addBox(5.5F, -9.0F, -6.2F, 2.0F, 10.0F, 0.5F)
                        .texOffs(88, 0).addBox(5.5F, -9.0F, 5.7F, 2.0F, 10.0F, 0.5F)
                        .texOffs(88, 10).addBox(5.5F, -9.2F, -6.2F, 2.0F, 0.5F, 12.4F),
                PartPose.ZERO
        );

        // 3. Main Cylindrical Tank Boiler & End Caps
        root.addOrReplaceChild(
                "tank_main",
                CubeListBuilder.create()
                        // Main boiler tank cylinder (20 x 9.5 x 12)
                        .texOffs(0, 40).addBox(-10.0F, -8.5F, -6.0F, 20.0F, 9.5F, 12.0F)
                        // Front rounded dome end cap (2 x 7.5 x 10)
                        .texOffs(0, 64).addBox(-11.5F, -7.5F, -5.0F, 2.0F, 7.5F, 10.0F)
                        // Back rounded dome end cap (2 x 7.5 x 10)
                        .texOffs(24, 64).addBox(9.5F, -7.5F, -5.0F, 2.0F, 7.5F, 10.0F),
                PartPose.ZERO
        );

        // 4. Tank Top Manhole Dome & Pressure Valve
        root.addOrReplaceChild(
                "tank_dome",
                CubeListBuilder.create()
                        // Manhole access dome (5 x 2 x 5)
                        .texOffs(0, 84).addBox(-2.5F, -10.5F, -2.5F, 5.0F, 2.0F, 5.0F)
                        // Top inspection hatch lid (4 x 0.5 x 4)
                        .texOffs(20, 84).addBox(-2.0F, -11.0F, -2.0F, 4.0F, 0.5F, 4.0F)
                        // Pressure relief valve / breather pipe
                        .texOffs(36, 84).addBox(4.0F, -10.5F, -1.0F, 1.5F, 2.0F, 2.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LiquidMinecartRenderState state) {
        super.setupAnim(state);
    }
}
