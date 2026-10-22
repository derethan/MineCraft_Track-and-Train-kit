package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.CabooseRenderState;

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
 * North American Wide-Vision Steel Caboose Model (Chunk Anchor).
 * Features a detailed cabin, rooftop cupola, front/rear end porches with railings,
 * brake handwheel, stove chimney, marker lanterns, and 8 flanged steel wheels.
 */
@OnlyIn(Dist.CLIENT)
public class CabooseModel extends EntityModel<CabooseRenderState> {
    public CabooseModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        // 1. Main Cabin Body (144 x 48 x 80) - Red Walls
                        .texOffs(0, 0).addBox(-72.0F, -30.0F, -40.0F, 144.0F, 48.0F, 80.0F)
                        // Overhanging Cabin Roof (152 x 4 x 84) - Black Steel
                        .texOffs(500, 0).addBox(-76.0F, -34.0F, -42.0F, 152.0F, 4.0F, 84.0F)

                        // 2. Rooftop Observation Cupola (48 x 20 x 56) - Red Walls
                        .texOffs(0, 136).addBox(-24.0F, -54.0F, -28.0F, 48.0F, 20.0F, 56.0F)
                        // Cupola Overhanging Roof Cap (52 x 3 x 60) - Black Steel
                        .texOffs(500, 100).addBox(-26.0F, -57.0F, -30.0F, 52.0F, 3.0F, 60.0F)

                        // 3. Front & Rear Porch Platforms (24 x 4 x 72)
                        .texOffs(216, 136).addBox(-96.0F, 18.0F, -36.0F, 24.0F, 4.0F, 72.0F)
                        .texOffs(416, 136).addBox(72.0F, 18.0F, -36.0F, 24.0F, 4.0F, 72.0F)

                        // 4. Porch Safety Railings & End Screens
                        // Front end railing
                        .texOffs(0, 220).addBox(-96.0F, 2.0F, -36.0F, 2.0F, 16.0F, 72.0F)
                        // Rear end railing
                        .texOffs(0, 220).addBox(94.0F, 2.0F, -36.0F, 2.0F, 16.0F, 72.0F)
                        // Front side grab rails
                        .texOffs(0, 220).addBox(-94.0F, 2.0F, -36.0F, 22.0F, 16.0F, 2.0F)
                        .texOffs(0, 220).addBox(-94.0F, 2.0F, 34.0F, 22.0F, 16.0F, 2.0F)
                        // Rear side grab rails
                        .texOffs(0, 220).addBox(72.0F, 2.0F, -36.0F, 22.0F, 16.0F, 2.0F)
                        .texOffs(0, 220).addBox(72.0F, 2.0F, 34.0F, 22.0F, 16.0F, 2.0F)

                        // 5. Brake Handwheel on Rear Porch
                        .texOffs(0, 220).addBox(88.0F, 4.0F, 14.0F, 2.0F, 12.0F, 12.0F)

                        // 6. Center Sill Underframe (192 x 8 x 20)
                        .texOffs(0, 270).addBox(-96.0F, 20.0F, -10.0F, 192.0F, 8.0F, 20.0F)

                        // 7. Coupler Boxes
                        .texOffs(0, 310).addBox(-108.0F, 22.0F, -4.0F, 12.0F, 6.0F, 8.0F)
                        .texOffs(50, 310).addBox(96.0F, 22.0F, -4.0F, 12.0F, 6.0F, 8.0F)

                        // 8. Stove Chimney Pipe
                        .texOffs(100, 310).addBox(-48.0F, -50.0F, 14.0F, 8.0F, 16.0F, 8.0F)
                        .texOffs(100, 310).addBox(-50.0F, -52.0F, 12.0F, 12.0F, 2.0F, 12.0F)

                        // 9. Rear Marker Lanterns
                        .texOffs(140, 310).addBox(70.0F, 0.0F, -39.0F, 6.0F, 8.0F, 6.0F)
                        .texOffs(140, 310).addBox(70.0F, 0.0F, 33.0F, 6.0F, 8.0F, 6.0F)

                        // 10. Front Bogie Truck (Pivot X = -50)
                        .texOffs(0, 360).addBox(-58.0F, 28.0F, -24.0F, 16.0F, 6.0F, 48.0F)
                        .texOffs(152, 360).addBox(-74.0F, 32.0F, -43.0F, 48.0F, 8.0F, 2.0F)
                        .texOffs(152, 380).addBox(-74.0F, 32.0F, 41.0F, 48.0F, 8.0F, 2.0F)
                        // Axle 1 (X = -62)
                        .texOffs(272, 360).addBox(-70.0F, 32.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(316, 360).addBox(-70.0F, 32.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 270).addBox(-63.0F, 39.0F, -37.0F, 2.0F, 2.0F, 74.0F)
                        // Axle 2 (X = -38)
                        .texOffs(360, 360).addBox(-46.0F, 32.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(404, 360).addBox(-46.0F, 32.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 270).addBox(-39.0F, 39.0F, -37.0F, 2.0F, 2.0F, 74.0F)

                        // 11. Rear Bogie Truck (Pivot X = 50)
                        .texOffs(0, 410).addBox(42.0F, 28.0F, -24.0F, 16.0F, 6.0F, 48.0F)
                        .texOffs(152, 410).addBox(26.0F, 32.0F, -43.0F, 48.0F, 8.0F, 2.0F)
                        .texOffs(152, 430).addBox(26.0F, 32.0F, 41.0F, 48.0F, 8.0F, 2.0F)
                        // Axle 3 (X = 38)
                        .texOffs(272, 384).addBox(30.0F, 32.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(316, 384).addBox(30.0F, 32.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 270).addBox(37.0F, 39.0F, -37.0F, 2.0F, 2.0F, 74.0F)
                        // Axle 4 (X = 62)
                        .texOffs(360, 384).addBox(54.0F, 32.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(404, 384).addBox(54.0F, 32.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 270).addBox(61.0F, 39.0F, -37.0F, 2.0F, 2.0F, 74.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 1024, 1024);
    }
}
