package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.DieselLocomotiveRenderState;

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
 * Authentic Canadian National (CN #3087) 4,400 HP Road Freight Diesel-Electric Locomotive.
 * Features red safety cab, white diagonal cowl stripe, black hood with CN logo,
 * roof dynamic brake blister, flared rear radiators, twin ditch lights, and 6-axle heavy trucks.
 */
@OnlyIn(Dist.CLIENT)
public class DieselLocomotiveModel extends EntityModel<DieselLocomotiveRenderState> {
    public DieselLocomotiveModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        // 1. Heavy Walkway Deck & Chassis Frame (38 x 2 x 16)
                        .texOffs(0, 0).addBox(-19.0F, 2.0F, -8.0F, 38.0F, 2.0F, 16.0F)

                        // 2. Front Red Safety Nose / Short Hood (7 x 8 x 12) - Solid Red
                        .texOffs(0, 22).addBox(-18.0F, -6.0F, -6.0F, 7.0F, 8.0F, 12.0F)

                        // 3. Wide Crew Comfort Cab with Windshields (7 x 10 x 14)
                        .texOffs(0, 46).addBox(-11.0F, -8.0F, -7.0F, 7.0F, 10.0F, 14.0F)

                        // 4. Continuous Long Machinery Engine Hood (17 x 9 x 12) - Seamless Solid Matte Black
                        .texOffs(0, 72).addBox(-4.0F, -7.0F, -6.0F, 17.0F, 9.0F, 12.0F)

                        // 5. Flared Rear Radiator Section (5 x 9 x 14) - Solid Matte Black
                        .texOffs(60, 72).addBox(13.0F, -7.0F, -7.0F, 5.0F, 9.0F, 14.0F)

                        // 6. Dynamic Brake Blister (6 x 2 x 8) & Exhaust Stack (2 x 2.2 x 2) - Anti-flicker embedded base
                        .texOffs(0, 96).addBox(-2.0F, -8.8F, -4.0F, 6.0F, 2.0F, 8.0F)
                        .texOffs(30, 96).addBox(5.0F, -9.0F, -1.0F, 2.0F, 2.2F, 2.0F)

                        // 7. Front 3-Axle Heavy Freight Truck (11 x 4 x 14)
                        .texOffs(42, 96).addBox(-16.0F, 4.0F, -7.0F, 11.0F, 4.0F, 14.0F)

                        // 8. Rear 3-Axle Heavy Freight Truck (11 x 4 x 14)
                        .texOffs(42, 96).addBox(5.0F, 4.0F, -7.0F, 11.0F, 4.0F, 14.0F)

                        // 9. Low-slung Central Diesel Fuel Tank (10 x 4 x 12)
                        .texOffs(0, 110).addBox(-5.0F, 4.0F, -6.0F, 10.0F, 4.0F, 12.0F)

                        // 10. Front Snow Plow / Pilot (1 x 5 x 14)
                        .texOffs(80, 22).addBox(-19.5F, 1.0F, -7.0F, 1.0F, 5.0F, 14.0F)

                        // 11. High-intensity Twin Headlights on Nose (0.5 x 2 x 4)
                        .texOffs(84, 26).addBox(-18.5F, -5.0F, -2.0F, 0.5F, 2.0F, 4.0F)

                        // 12. Ditch Lights on Front Walkway Pilot (0.5 x 2 x 2 each)
                        .texOffs(84, 36).addBox(-19.2F, 0.0F, -5.0F, 0.5F, 2.0F, 2.0F)
                        .texOffs(94, 36).addBox(-19.2F, 0.0F, 3.0F, 0.5F, 2.0F, 2.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }
}
