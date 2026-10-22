package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.ElectricLocomotiveRenderState;

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
public class ElectricLocomotiveModel extends EntityModel<ElectricLocomotiveRenderState> {
    public ElectricLocomotiveModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // High-speed aerodynamic bullet train body
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        // 1. Aerodynamic chassis / low skirt (28 x 3 x 16)
                        .texOffs(0, 0).addBox(-14.0F, 2.0F, -8.0F, 28.0F, 3.0F, 16.0F)
                        // 2. Main aerodynamic passenger/machinery cabin (22 x 10 x 14)
                        .texOffs(0, 20).addBox(-6.0F, -8.0F, -7.0F, 20.0F, 10.0F, 14.0F)
                        // 3. Tapered aerodynamic driver cab / windshield section (7 x 9 x 13)
                        .texOffs(0, 46).addBox(-13.0F, -7.0F, -6.5F, 7.0F, 9.0F, 13.0F)
                        // 4. Streamlined pointed bullet nose cone (8 x 7 x 11)
                        .texOffs(42, 46).addBox(-19.0F, -5.0F, -5.5F, 6.0F, 7.0F, 11.0F)
                        // 5. Aerodynamic nose tip wedge (4 x 5 x 8)
                        .texOffs(68, 46).addBox(-23.0F, -3.0F, -4.0F, 4.0F, 5.0F, 8.0F)
                        // 6. High-intensity LED twin headlight bar embedded in nose tip (0.5 x 2 x 6)
                        .texOffs(94, 46).addBox(-23.3F, -2.0F, -3.0F, 0.5F, 2.0F, 6.0F)
                        // 7. Aerodynamic roof air-conditioning & fairing pod (16 x 2 x 8)
                        .texOffs(0, 70).addBox(-4.0F, -10.0F, -4.0F, 16.0F, 2.0F, 8.0F)
                        // 8. Pantograph base on rear roof (6 x 1 x 6)
                        .texOffs(50, 70).addBox(5.0F, -11.0F, -3.0F, 6.0F, 1.0F, 6.0F)
                        // 9. Pantograph lower arm frame (1 x 4 x 4)
                        .texOffs(76, 70).addBox(7.5F, -15.0F, -2.0F, 1.0F, 4.0F, 4.0F)
                        // 10. Pantograph carbon collector head bar (2 x 0.8 x 14)
                        .texOffs(50, 80).addBox(7.0F, -15.8F, -7.0F, 2.0F, 1.0F, 14.0F)
                        // 11. Undercarriage bogie wheels - Front (8 x 3 x 14)
                        .texOffs(0, 96).addBox(-12.0F, 5.0F, -7.0F, 8.0F, 3.0F, 14.0F)
                        // 12. Undercarriage bogie wheels - Rear (8 x 3 x 14)
                        .texOffs(46, 96).addBox(5.0F, 5.0F, -7.0F, 8.0F, 3.0F, 14.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }
}
