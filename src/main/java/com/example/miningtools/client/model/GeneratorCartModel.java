package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.GeneratorCartRenderState;

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
public class GeneratorCartModel extends EntityModel<GeneratorCartRenderState> {
    private final ModelPart root;

    public GeneratorCartModel(ModelPart root) {
        super(root);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Chassis Underframe, Couplers & Wheels
        root.addOrReplaceChild(
                "chassis",
                CubeListBuilder.create()
                        // Heavy Underframe Deck (24 x 2 x 16)
                        .texOffs(0, 0).addBox(-12.0F, 2.0F, -8.0F, 24.0F, 2.0F, 16.0F)
                        // Center Sill Draft Beam
                        .texOffs(0, 20).addBox(-10.0F, 4.0F, -3.0F, 20.0F, 2.0F, 6.0F)
                        // Front Coupler Hitch
                        .texOffs(54, 20).addBox(-15.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F)
                        // Rear Coupler Hitch
                        .texOffs(54, 20).addBox(12.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F)
                        // 4 Flanged Rail Wheels
                        .texOffs(72, 20).addBox(-8.0F, 3.0F, -7.5F, 4.0F, 4.0F, 2.0F)
                        .texOffs(84, 20).addBox(4.0F, 3.0F, -7.5F, 4.0F, 4.0F, 2.0F)
                        .texOffs(96, 20).addBox(-8.0F, 3.0F, 5.5F, 4.0F, 4.0F, 2.0F)
                        .texOffs(108, 20).addBox(4.0F, 3.0F, 5.5F, 4.0F, 4.0F, 2.0F)
                        // Side Running Boards / Footboards
                        .texOffs(0, 30).addBox(-11.0F, 2.5F, -9.0F, 22.0F, 1.0F, 1.0F)
                        .texOffs(0, 30).addBox(-11.0F, 2.5F, 8.0F, 22.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        // 2. Heavy Boiler & Firebox (Coal combustion & water steam)
        root.addOrReplaceChild(
                "boiler",
                CubeListBuilder.create()
                        // Firebox Housing at rear
                        .texOffs(0, 34).addBox(2.0F, -8.0F, -7.0F, 9.0F, 10.0F, 14.0F)
                        // Firebox Stokehole Access Door
                        .texOffs(48, 34).addBox(11.0F, -5.0F, -3.0F, 1.0F, 6.0F, 6.0F)
                        // Horizontal Cylindrical Steam Boiler Tank
                        .texOffs(0, 60).addBox(-5.0F, -9.0F, -6.0F, 8.0F, 9.0F, 12.0F)
                        // Riveted Brass Boiler Reinforcement Bands
                        .texOffs(42, 60).addBox(-4.0F, -9.5F, -6.5F, 1.0F, 10.0F, 13.0F)
                        .texOffs(42, 60).addBox(0.5F, -9.5F, -6.5F, 1.0F, 10.0F, 13.0F)
                        // Exhaust Chimney / Smokestack
                        .texOffs(74, 60).addBox(4.0F, -16.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(92, 60).addBox(3.5F, -17.0F, -2.5F, 5.0F, 1.0F, 5.0F)
                        // Steam Dome / Safety Relief Valve
                        .texOffs(74, 75).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 3.0F, 4.0F)
                        // Water Level Sight Gauge on Side
                        .texOffs(92, 75).addBox(-1.0F, -7.0F, 6.0F, 2.0F, 6.0F, 1.0F),
                PartPose.ZERO
        );

        // 3. Steam Dynamo Turbine Generator (RF Generation)
        root.addOrReplaceChild(
                "dynamo",
                CubeListBuilder.create()
                        // Main Generator Housing / Stator Casing
                        .texOffs(0, 83).addBox(-11.0F, -7.0F, -6.5F, 6.0F, 9.0F, 13.0F)
                        // Cooling Fins / Ribbed Armature Shell
                        .texOffs(40, 83).addBox(-10.5F, -6.5F, -6.0F, 5.0F, 8.0F, 12.0F)
                        // Copper Commutator & Armature Shaft Front Cap
                        .texOffs(80, 83).addBox(-12.5F, -4.0F, -3.0F, 2.0F, 5.0F, 6.0F)
                        // Steam Turbine Drive Steam Supply Manifold Pipe
                        .texOffs(98, 83).addBox(-6.0F, -7.5F, -2.0F, 2.0F, 3.0F, 4.0F),
                PartPose.ZERO
        );

        // 4. Electrical Terminal Box, High-Voltage Insulators & Electrodes
        root.addOrReplaceChild(
                "electrical",
                CubeListBuilder.create()
                        // Transformer / Cable Terminal Junction Box
                        .texOffs(0, 107).addBox(-10.0F, -11.0F, -4.0F, 4.0F, 4.0F, 8.0F)
                        // Dual Ceramic Insulator Pylons
                        .texOffs(26, 107).addBox(-9.0F, -15.0F, -3.0F, 2.0F, 4.0F, 2.0F)
                        .texOffs(36, 107).addBox(-9.0F, -15.0F, 1.0F, 2.0F, 4.0F, 2.0F)
                        // Copper High-Voltage Spark / Arc Terminals
                        .texOffs(46, 107).addBox(-9.5F, -16.5F, -3.5F, 3.0F, 2.0F, 3.0F)
                        .texOffs(46, 107).addBox(-9.5F, -16.5F, 0.5F, 3.0F, 2.0F, 3.0F)
                        // External Power Cable Connection Port (Front-Right Face)
                        .texOffs(60, 107).addBox(-10.0F, -4.0F, -8.0F, 4.0F, 4.0F, 2.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }
}
