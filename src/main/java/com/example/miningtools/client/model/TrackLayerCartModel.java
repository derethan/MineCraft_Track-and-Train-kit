package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.TrackLayerCartRenderState;

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
public class TrackLayerCartModel extends EntityModel<TrackLayerCartRenderState> {
    private final ModelPart root;

    public TrackLayerCartModel(ModelPart root) {
        super(root);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Monolithic Armored Chassis Base & Side Skirts
        root.addOrReplaceChild(
                "monolith_hull",
                CubeListBuilder.create()
                        // Central heavy armored unibody hull deck (18 x 3 x 15)
                        .texOffs(0, 0).addBox(-9.0F, 2.0F, -7.5F, 18.0F, 3.0F, 15.0F)
                        // Left angled protective armor side skirt
                        .texOffs(0, 20).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 5.0F, 1.5F)
                        // Right angled protective armor side skirt
                        .texOffs(0, 28).addBox(-8.0F, 0.0F, 6.5F, 16.0F, 5.0F, 1.5F)
                        // Front heavy monolithic buffer beam
                        .texOffs(36, 20).addBox(-11.0F, 1.0F, -7.0F, 2.0F, 4.0F, 14.0F)
                        // Rear heavy monolithic buffer beam
                        .texOffs(36, 20).addBox(9.0F, 1.0F, -7.0F, 2.0F, 4.0F, 14.0F)
                        // Front coupler hitch
                        .texOffs(70, 20).addBox(-13.0F, 2.5F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // Rear coupler hitch
                        .texOffs(70, 20).addBox(11.0F, 2.5F, -2.0F, 2.0F, 2.0F, 4.0F),
                PartPose.ZERO
        );

        // 2. Monolithic Cockpit & Overhead Roll-Cage
        root.addOrReplaceChild(
                "cockpit",
                CubeListBuilder.create()
                        // Roll-cage 4 vertical corner columns
                        .texOffs(0, 36).addBox(-3.0F, -10.0F, -6.5F, 1.5F, 12.0F, 1.5F)
                        .texOffs(0, 36).addBox(-3.0F, -10.0F, 5.0F, 1.5F, 12.0F, 1.5F)
                        .texOffs(0, 36).addBox(4.0F, -10.0F, -6.5F, 1.5F, 12.0F, 1.5F)
                        .texOffs(0, 36).addBox(4.0F, -10.0F, 5.0F, 1.5F, 12.0F, 1.5F)
                        // Overhead monolithic protective roof plate
                        .texOffs(8, 36).addBox(-4.0F, -11.0F, -7.0F, 9.5F, 1.5F, 14.0F)
                        // Control dashboard pedestal
                        .texOffs(46, 36).addBox(-5.0F, -2.0F, -4.0F, 2.0F, 4.0F, 8.0F)
                        // Dual throttle & reverse control levers
                        .texOffs(68, 36).addBox(-5.2F, -4.0F, -2.5F, 0.5F, 2.5F, 1.0F)
                        .texOffs(68, 36).addBox(-5.2F, -4.0F, 1.5F, 0.5F, 2.5F, 1.0F)
                        // Driver padded seat cushion & backrest
                        .texOffs(74, 36).addBox(-1.0F, 0.0F, -3.5F, 5.0F, 2.0F, 7.0F)
                        .texOffs(74, 47).addBox(3.0F, -5.0F, -3.5F, 1.5F, 5.0F, 7.0F),
                PartPose.ZERO
        );

        // 3. Monolithic Track Laying Apparatus (Front)
        root.addOrReplaceChild(
                "track_laying_chute",
                CubeListBuilder.create()
                        // Heavy angled track feed chute & guide throat
                        .texOffs(0, 53).addBox(-10.5F, -3.0F, -5.0F, 3.5F, 5.0F, 10.0F)
                        // Front track alignment press shoes & guide roller
                        .texOffs(28, 53).addBox(-11.5F, 3.5F, -5.5F, 2.0F, 2.0F, 11.0F)
                        // Left work floodlight lantern
                        .texOffs(56, 53).addBox(-11.2F, -1.0F, -6.5F, 1.5F, 2.0F, 2.0F)
                        // Right work floodlight lantern
                        .texOffs(56, 53).addBox(-11.2F, -1.0F, 4.5F, 1.5F, 2.0F, 2.0F),
                PartPose.ZERO
        );

        // 4. Monolithic Track Retriever Claw & Scoop (Rear)
        root.addOrReplaceChild(
                "track_retriever_arm",
                CubeListBuilder.create()
                        // Heavy rear track recovery scoop & intake funnel
                        .texOffs(0, 70).addBox(7.5F, -2.0F, -5.0F, 3.0F, 5.0F, 10.0F)
                        // Twin hydraulic lift actuator cylinders
                        .texOffs(28, 70).addBox(5.0F, -4.0F, -6.0F, 4.0F, 2.0F, 1.5F)
                        .texOffs(28, 70).addBox(5.0F, -4.0F, 4.5F, 4.0F, 2.0F, 1.5F)
                        // Rear magnetic / mechanical guide roller bar
                        .texOffs(42, 70).addBox(9.5F, 3.5F, -5.5F, 2.0F, 2.0F, 11.0F),
                PartPose.ZERO
        );

        // 5. Monolithic Power Generator & Side Exhaust Unit
        root.addOrReplaceChild(
                "engine_unit",
                CubeListBuilder.create()
                        // Side power module with radiator louvers
                        .texOffs(0, 87).addBox(-4.0F, -3.0F, -7.8F, 8.0F, 5.0F, 2.0F)
                        // Vertical heavy steel exhaust pipe
                        .texOffs(22, 87).addBox(2.0F, -12.0F, -7.5F, 1.5F, 9.0F, 1.5F)
                        // Spark arrestor muffler cap
                        .texOffs(30, 87).addBox(1.5F, -13.0F, -8.0F, 2.5F, 1.0F, 2.5F),
                PartPose.ZERO
        );

        // 6. Running Gear (Authentic Flanged Railway Wheels & Axles)
        root.addOrReplaceChild(
                "running_gear",
                CubeListBuilder.create()
                        // Front-left wheel
                        .texOffs(42, 87).addBox(-6.0F, 4.6F, -6.4F, 2.8F, 2.4F, 0.8F)
                        // Front-right wheel
                        .texOffs(42, 87).addBox(-6.0F, 4.6F, 5.6F, 2.8F, 2.4F, 0.8F)
                        // Rear-left wheel
                        .texOffs(42, 87).addBox(3.2F, 4.6F, -6.4F, 2.8F, 2.4F, 0.8F)
                        // Rear-right wheel
                        .texOffs(42, 87).addBox(3.2F, 4.6F, 5.6F, 2.8F, 2.4F, 0.8F)
                        // Front axle connecting wheels
                        .texOffs(52, 87).addBox(-5.0F, 5.4F, -5.6F, 0.8F, 0.8F, 11.2F)
                        // Rear axle connecting wheels
                        .texOffs(52, 87).addBox(4.2F, 5.4F, -5.6F, 0.8F, 0.8F, 11.2F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }
}
