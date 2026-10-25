package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.TrackPaverRenderState;

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
 * 3D Model for the Heavy Track & Road Paver (modeled after CAT AP1055F Track Paver).
 * Sized ~2.75 blocks wide (44 units), ~2.0 blocks tall (32 units), and ~3.0 blocks long (48 units).
 */
@OnlyIn(Dist.CLIENT)
public class TrackPaverModel extends EntityModel<TrackPaverRenderState> {
    private final ModelPart root;
    private final ModelPart leftTreads;
    private final ModelPart rightTreads;
    private final ModelPart screed;

    public TrackPaverModel(ModelPart root) {
        super(root);
        this.root = root;
        this.leftTreads = root.getChild("left_treads");
        this.rightTreads = root.getChild("right_treads");
        this.screed = root.getChild("screed");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. LEFT CRAWLER TREADS & WHEELS (Black industrial rubber/steel track assembly)
        root.addOrReplaceChild(
                "left_treads",
                CubeListBuilder.create()
                        // Main track rubber loop
                        .texOffs(0, 0)
                        .addBox(-21.0F, -10.0F, -20.0F, 6.0F, 10.0F, 40.0F)
                        // Front track idler wheel
                        .texOffs(52, 0)
                        .addBox(-21.5F, -9.0F, -22.0F, 7.0F, 8.0F, 4.0F)
                        // Rear drive sprocket wheel
                        .texOffs(52, 12)
                        .addBox(-21.5F, -9.0F, 18.0F, 7.0F, 8.0F, 4.0F)
                        // Bottom road roller rims
                        .texOffs(74, 0)
                        .addBox(-20.5F, -3.0F, -16.0F, 5.0F, 3.0F, 32.0F),
                PartPose.ZERO
        );

        // 2. RIGHT CRAWLER TREADS & WHEELS
        root.addOrReplaceChild(
                "right_treads",
                CubeListBuilder.create()
                        .texOffs(0, 50)
                        .addBox(15.0F, -10.0F, -20.0F, 6.0F, 10.0F, 40.0F)
                        .texOffs(52, 0)
                        .addBox(14.5F, -9.0F, -22.0F, 7.0F, 8.0F, 4.0F)
                        .texOffs(52, 12)
                        .addBox(14.5F, -9.0F, 18.0F, 7.0F, 8.0F, 4.0F)
                        .texOffs(74, 0)
                        .addBox(15.5F, -3.0F, -16.0F, 5.0F, 3.0F, 32.0F),
                PartPose.ZERO
        );

        // 3. MAIN HEAVY CHASSIS BELLY
        root.addOrReplaceChild(
                "chassis_belly",
                CubeListBuilder.create().texOffs(0, 100)
                        .addBox(-15.0F, -14.0F, -20.0F, 30.0F, 10.0F, 40.0F),
                PartPose.ZERO
        );

        // 4. FRONT RECEIVING HOPPER (CAT Yellow angled dump hopper)
        root.addOrReplaceChild(
                "front_hopper",
                CubeListBuilder.create()
                        // Hopper floor bed
                        .texOffs(100, 50)
                        .addBox(-16.0F, -6.0F, -24.0F, 32.0F, 4.0F, 18.0F)
                        // Left sloped hopper wing/side
                        .texOffs(100, 72)
                        .addBox(-20.0F, -16.0F, -24.0F, 4.0F, 11.0F, 18.0F)
                        // Right sloped hopper wing/side
                        .texOffs(100, 72)
                        .addBox(16.0F, -16.0F, -24.0F, 4.0F, 11.0F, 18.0F)
                        // Front push roller bumper bar
                        .texOffs(100, 101)
                        .addBox(-19.0F, -7.0F, -26.0F, 38.0F, 4.0F, 3.0F),
                PartPose.ZERO
        );

        // 5. ENGINE HOUSING & HOOD (CAT Yellow with black vents and exhaust)
        root.addOrReplaceChild(
                "engine_hood",
                CubeListBuilder.create()
                        // Sloped engine hood
                        .texOffs(144, 0)
                        .addBox(-14.0F, -22.0F, -6.0F, 28.0F, 12.0F, 16.0F)
                        // Sloped cowl transition to hopper
                        .texOffs(144, 28)
                        .addBox(-13.0F, -18.0F, -10.0F, 26.0F, 8.0F, 4.0F)
                        // Headlights on cowl
                        .texOffs(144, 40)
                        .addBox(-11.0F, -17.0F, -11.0F, 3.0F, 3.0F, 2.0F)
                        .addBox(8.0F, -17.0F, -11.0F, 3.0F, 3.0F, 2.0F)
                        // Vertical exhaust smokestack
                        .texOffs(154, 40)
                        .addBox(-6.0F, -34.0F, 0.0F, 3.0F, 13.0F, 3.0F)
                        // Exhaust rain cap
                        .texOffs(166, 40)
                        .addBox(-6.5F, -35.0F, -0.5F, 4.0F, 1.0F, 4.0F),
                PartPose.ZERO
        );

        // 6. OPERATOR DECK & CONTROL STATIONS
        root.addOrReplaceChild(
                "operator_deck",
                CubeListBuilder.create()
                        // Elevated operator floor platform
                        .texOffs(0, 150)
                        .addBox(-16.0F, -16.0F, 8.0F, 32.0F, 2.0F, 14.0F)
                        // Center control dashboard console
                        .texOffs(92, 150)
                        .addBox(-5.0F, -24.0F, 10.0F, 10.0F, 8.0F, 4.0F)
                        // Left operator seat cushion & back
                        .texOffs(120, 150)
                        .addBox(-13.0F, -20.0F, 12.0F, 6.0F, 4.0F, 6.0F)
                        .addBox(-13.0F, -27.0F, 16.0F, 6.0F, 7.0F, 2.0F)
                        // Right operator seat cushion & back (where passenger sits)
                        .texOffs(120, 150)
                        .addBox(7.0F, -20.0F, 12.0F, 6.0F, 4.0F, 6.0F)
                        .addBox(7.0F, -27.0F, 16.0F, 6.0F, 7.0F, 2.0F)
                        // Safety handrails along deck perimeter
                        .texOffs(144, 150)
                        .addBox(-16.0F, -24.0F, 8.0F, 1.0F, 8.0F, 14.0F)
                        .addBox(15.0F, -24.0F, 8.0F, 1.0F, 8.0F, 14.0F)
                        .addBox(-16.0F, -24.0F, 21.0F, 32.0F, 8.0F, 1.0F),
                PartPose.ZERO
        );

        // 7. REAR LEVELING SCREED (The Paving Unit - Spans full 44 units across 3 blocks!)
        root.addOrReplaceChild(
                "screed",
                CubeListBuilder.create()
                        // Heavy leveling screed plate (44 units wide = 2.75 blocks!)
                        .texOffs(0, 166)
                        .addBox(-22.0F, -6.0F, 20.0F, 44.0F, 6.0F, 6.0F)
                        // Left screed end gate / side plate
                        .texOffs(100, 166)
                        .addBox(-23.0F, -8.0F, 18.0F, 2.0F, 8.0F, 9.0F)
                        // Right screed end gate / side plate
                        .texOffs(100, 166)
                        .addBox(21.0F, -8.0F, 18.0F, 2.0F, 8.0F, 9.0F)
                        // Hydraulic tow arms connecting screed to crawler chassis
                        .texOffs(122, 166)
                        .addBox(-18.0F, -8.0F, 2.0F, 2.0F, 2.0F, 18.0F)
                        .addBox(16.0F, -8.0F, 2.0F, 2.0F, 2.0F, 18.0F)
                        // Rear screed technician walkway
                        .texOffs(0, 178)
                        .addBox(-20.0F, -7.0F, 26.0F, 40.0F, 1.0F, 5.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 256, 256);
    }
}
