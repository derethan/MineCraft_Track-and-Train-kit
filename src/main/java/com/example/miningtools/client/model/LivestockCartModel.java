package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.LivestockCartRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Authentic North American Railroad Livestock Car (Stock Car).
 * Features open horizontal wooden slats allowing entities inside to be visible,
 * heavy steel underframe with flanged wheels, sliding center doors,
 * solid end bulkheads with a cast-iron hand brake wheel, and an arched roof with a center roof walk.
 */
@OnlyIn(Dist.CLIENT)
public class LivestockCartModel extends EntityModel<LivestockCartRenderState> {
    public LivestockCartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Heavy Underframe & Floor Deck (24 x 2 x 16)
        CubeListBuilder body = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.0F, 2.0F, -8.0F, 24.0F, 2.0F, 16.0F)
                // Center Sill Draft Beam (20 x 2 x 6)
                .texOffs(0, 20).addBox(-10.0F, 4.0F, -3.0F, 20.0F, 2.0F, 6.0F)
                // Front Coupler Hitch (3 x 2 x 4)
                .texOffs(0, 28).addBox(-15.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F)
                // Rear Coupler Hitch (3 x 2 x 4)
                .texOffs(0, 28).addBox(12.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F);

        // Authentic 2-axle bogie trucks (matching Caboose: sideframe brackets + 2 small wheels per side)
        TruckModelHelper.addStandardBogieTrucks(body);

        body

                        // 2. Open Horizontal Wooden Slats (Left & Right Sides)
                        // Slat 1: Bottom Kickboard (24 x 2 x 1)
                        .texOffs(0, 30).addBox(-12.0F, 0.5F, -8.0F, 24.0F, 2.0F, 1.0F)
                        .texOffs(0, 30).addBox(-12.0F, 0.5F, 7.0F, 24.0F, 2.0F, 1.0F)
                        // Slat 2: Lower Mid Plank (24 x 2 x 1) - [Open gap below: Y = -1.0 to 0.5]
                        .texOffs(0, 34).addBox(-12.0F, -3.0F, -8.0F, 24.0F, 2.0F, 1.0F)
                        .texOffs(0, 34).addBox(-12.0F, -3.0F, 7.0F, 24.0F, 2.0F, 1.0F)
                        // Slat 3: Upper Mid Plank (24 x 2 x 1) - [Open gap below: Y = -4.5 to -3.0]
                        .texOffs(0, 38).addBox(-12.0F, -6.5F, -8.0F, 24.0F, 2.0F, 1.0F)
                        .texOffs(0, 38).addBox(-12.0F, -6.5F, 7.0F, 24.0F, 2.0F, 1.0F)
                        // Slat 4: Top Eave Fascia (24 x 2.5 x 1) - [Open gap below: Y = -8.0 to -6.5]
                        .texOffs(0, 42).addBox(-12.0F, -10.5F, -8.0F, 24.0F, 2.5F, 1.0F)
                        .texOffs(0, 42).addBox(-12.0F, -10.5F, 7.0F, 24.0F, 2.5F, 1.0F)

                        // 3. Heavy Timber Corner Posts & Door Uprights
                        // 4 Corner Posts (2 x 13 x 2 each)
                        .texOffs(0, 48).addBox(-12.02F, -10.5F, -8.1F, 2.02F, 13.0F, 2.0F)
                        .texOffs(8, 48).addBox(10.0F, -10.5F, -8.1F, 2.02F, 13.0F, 2.0F)
                        .texOffs(16, 48).addBox(-12.02F, -10.5F, 6.1F, 2.02F, 13.0F, 2.0F)
                        .texOffs(24, 48).addBox(10.0F, -10.5F, 6.1F, 2.02F, 13.0F, 2.0F)
                        // Left Doorway Posts (1.5 x 13 x 1.2 each)
                        .texOffs(32, 48).addBox(-4.0F, -10.5F, -8.2F, 1.5F, 13.0F, 1.15F)
                        .texOffs(40, 48).addBox(2.5F, -10.5F, -8.2F, 1.5F, 13.0F, 1.15F)
                        // Right Doorway Posts (1.5 x 13 x 1.2 each)
                        .texOffs(32, 48).addBox(-4.0F, -10.5F, 7.05F, 1.5F, 13.0F, 1.15F)
                        .texOffs(40, 48).addBox(2.5F, -10.5F, 7.05F, 1.5F, 13.0F, 1.15F)

                        // 4. Center Sliding Doors & Guide Tracks
                        // Left & Right Top Guide Tracks (10 x 1 x 0.6)
                        .texOffs(0, 72).addBox(-5.0F, -10.8F, -8.4F, 10.0F, 1.0F, 0.6F)
                        .texOffs(0, 72).addBox(-5.0F, -10.8F, 7.8F, 10.0F, 1.0F, 0.6F)
                        // Left & Right Bottom Guide Tracks (10 x 1 x 0.6)
                        .texOffs(0, 72).addBox(-5.0F, 2.05F, -8.4F, 10.0F, 1.0F, 0.6F)
                        .texOffs(0, 72).addBox(-5.0F, 2.05F, 7.8F, 10.0F, 1.0F, 0.6F)
                        // Door Center Vertical Ribs / Hasps
                        .texOffs(30, 72).addBox(-0.5F, -8.0F, -8.4F, 1.0F, 8.0F, 0.6F)
                        .texOffs(30, 72).addBox(-0.5F, -8.0F, 7.8F, 1.0F, 8.0F, 0.6F)

                        // 5. Solid End Bulkhead Walls & Brake Wheel
                        // Front End Wall (1 x 13 x 14)
                        .texOffs(0, 84).addBox(-11.95F, -10.5F, -7.0F, 0.95F, 13.0F, 14.0F)
                        // Rear End Wall (1 x 13 x 14)
                        .texOffs(0, 84).addBox(11.0F, -10.5F, -7.0F, 0.95F, 13.0F, 14.0F)
                        // Rear Brake Wheel Shaft & Handwheel
                        .texOffs(72, 84).addBox(12.0F, -4.5F, 2.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(74, 84).addBox(12.5F, -6.5F, 0.5F, 0.5F, 5.0F, 5.0F)
                        // End Ladder Rungs
                        .texOffs(90, 84).addBox(-12.4F, -9.0F, -5.0F, 0.4F, 11.0F, 3.0F)
                        .texOffs(90, 84).addBox(12.0F, -9.0F, -5.0F, 0.4F, 11.0F, 3.0F)

                        // 6. Arched Roof & Center Roof Walk
                        // Main Roof Curved Crown (24 x 1.5 x 12)
                        .texOffs(0, 102).addBox(-12.0F, -12.0F, -6.0F, 24.0F, 1.5F, 12.0F)
                        // Left Sloping Eave (24 x 1 x 2.5)
                        .texOffs(0, 116).addBox(-12.0F, -11.2F, -8.5F, 24.0F, 1.0F, 2.48F)
                        // Right Sloping Eave (24 x 1 x 2.5)
                        .texOffs(0, 116).addBox(-12.0F, -11.2F, 6.02F, 24.0F, 1.0F, 2.48F)
                        // Center Running Board / Roof Walk (26 x 0.8 x 4)
                        .texOffs(0, 120).addBox(-13.0F, -12.8F, -2.0F, 26.0F, 0.8F, 4.0F);

        root.addOrReplaceChild("body", body, PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }
}
