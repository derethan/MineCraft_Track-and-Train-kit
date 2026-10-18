package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.BasicLocomotiveRenderState;

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
public class BasicLocomotiveModel extends EntityModel<BasicLocomotiveRenderState> {
    public BasicLocomotiveModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Exact box structure from Railcraft ModelLocomotiveSteamSolid with 8.0 rotation offset
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        // Base plate (23 x 2 x 16)
                        .texOffs(1, 23).addBox(-12.0F, 3.0F, -8.0F, 23.0F, 2.0F, 16.0F)
                        // Running board / Frame (25 x 2 x 18)
                        .texOffs(1, 1).addBox(-13.0F, 1.0F, -9.0F, 25.0F, 2.0F, 18.0F)
                        // Boiler (16 x 11 x 14)
                        .texOffs(67, 38).addBox(-12.0F, -10.0F, -7.0F, 16.0F, 11.0F, 14.0F)
                        // Driver Cab (7 x 12 x 16)
                        .texOffs(81, 8).addBox(4.0F, -11.0F, -8.0F, 7.0F, 12.0F, 16.0F)
                        // Cowcatcher / Front Grill (3 x 5 x 12)
                        .texOffs(1, 43).addBox(-14.0F, 0.0F, -6.0F, 3.0F, 5.0F, 12.0F)
                        // Smokestack / Chimney (4 x 6 x 4)
                        .texOffs(49, 43).addBox(-9.0F, -16.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                        // Steam Dome (6 x 2 x 6)
                        .texOffs(23, 43).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 64);
    }
}
