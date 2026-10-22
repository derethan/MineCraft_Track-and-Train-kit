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

/**
 * Large Heavy Freight North American DOT-111 / DOT-117 Pressurized Tank Car (16,000 mB / 16 Buckets).
 * 2.1 blocks long with two 2-axle bogie wheel trucks (8 wheels total).
 */
@OnlyIn(Dist.CLIENT)
public class LargeLiquidMinecartModel extends EntityModel<LiquidMinecartRenderState> {
    public LargeLiquidMinecartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        // 1. Main Cylindrical Tank Shell (dx=192, dz=80, dy=56)
                        .texOffs(0, 0).addBox(-96.0F, -42.0F, -40.0F, 192.0F, 56.0F, 80.0F)
                        // Top Curved Shell (dx=192, dz=56, dy=12)
                        .texOffs(0, 140).addBox(-96.0F, -54.0F, -28.0F, 192.0F, 12.0F, 56.0F)
                        // Bottom Curved Shell (dx=192, dz=56, dy=12)
                        .texOffs(0, 212).addBox(-96.0F, 14.0F, -28.0F, 192.0F, 12.0F, 56.0F)
                        // Upper Bevel Transition (dx=192, dz=72, dy=16)
                        .texOffs(0, 284).addBox(-96.0F, -48.0F, -36.0F, 192.0F, 16.0F, 72.0F)
                        // Lower Bevel Transition (dx=192, dz=72, dy=16)
                        .texOffs(0, 376).addBox(-96.0F, 4.0F, -36.0F, 192.0F, 16.0F, 72.0F)

                        // 2. Front Hemispherical End Caps (Facing X-)
                        // Base dome
                        .texOffs(550, 0).addBox(-102.0F, -44.0F, -34.0F, 6.0F, 60.0F, 68.0F)
                        // Mid dome step
                        .texOffs(550, 136).addBox(-106.0F, -37.0F, -26.0F, 4.0F, 46.0F, 52.0F)
                        // Tip dome step
                        .texOffs(550, 244).addBox(-109.0F, -30.0F, -18.0F, 3.0F, 32.0F, 36.0F)

                        // 3. Rear Hemispherical End Caps (Facing X+)
                        // Base dome
                        .texOffs(720, 0).addBox(96.0F, -44.0F, -34.0F, 6.0F, 60.0F, 68.0F)
                        // Mid dome step
                        .texOffs(720, 136).addBox(102.0F, -37.0F, -26.0F, 4.0F, 46.0F, 52.0F)
                        // Tip dome step
                        .texOffs(720, 244).addBox(106.0F, -30.0F, -18.0F, 3.0F, 32.0F, 36.0F)

                        // 4. Center Sill Heavy I-Beam Underframe (224 x 8 x 20)
                        .texOffs(0, 468).addBox(-112.0F, 26.0F, -10.0F, 224.0F, 8.0F, 20.0F)

                        // 5. End Walkways / Platforms with Diamond Safety Tread (20 x 4 x 48)
                        .texOffs(550, 310).addBox(-116.0F, 25.0F, -24.0F, 20.0F, 4.0F, 48.0F)
                        .texOffs(720, 310).addBox(96.0F, 25.0F, -24.0F, 20.0F, 4.0F, 48.0F)

                        // 6. Coupler Knuckle Boxes (12 x 6 x 8)
                        .texOffs(890, 0).addBox(-128.0F, 27.0F, -4.0F, 12.0F, 6.0F, 8.0F)
                        .texOffs(890, 24).addBox(116.0F, 27.0F, -4.0F, 12.0F, 6.0F, 8.0F)

                        // 7. Bolster Cradle Saddles (16 x 8 x 44)
                        .texOffs(550, 382).addBox(-68.0F, 20.0F, -22.0F, 16.0F, 8.0F, 44.0F)
                        .texOffs(700, 382).addBox(52.0F, 20.0F, -22.0F, 16.0F, 8.0F, 44.0F)

                        // 8. Underbody Sump Valve & Brake Reservoir Cylinder
                        .texOffs(850, 382).addBox(-6.0F, 26.0F, -6.0F, 12.0F, 8.0F, 12.0F)
                        .texOffs(850, 410).addBox(14.0F, 28.0F, 10.0F, 28.0F, 8.0F, 8.0F)

                        // 9. Top Dome, Inspection Lid, Grating Catwalk, Handrails & Side Ladder
                        .texOffs(0, 506).addBox(-12.0F, -60.0F, -12.0F, 24.0F, 6.0F, 24.0F)
                        .texOffs(98, 506).addBox(-7.0F, -62.0F, -7.0F, 14.0F, 2.0F, 14.0F)
                        .texOffs(256, 506).addBox(-32.0F, -56.0F, -14.0F, 64.0F, 2.0F, 28.0F)
                        .texOffs(0, 556).addBox(-32.0F, -68.0F, -14.0F, 64.0F, 12.0F, 2.0F)
                        .texOffs(134, 556).addBox(-32.0F, -68.0F, 12.0F, 64.0F, 12.0F, 2.0F)
                        .texOffs(272, 556).addBox(-4.0F, -54.0F, -42.0F, 8.0F, 64.0F, 2.0F)

                        // 10. Front Bogie Truck (Pivot X = -60)
                        // Bolster
                        .texOffs(0, 650).addBox(-68.0F, 34.0F, -24.0F, 16.0F, 6.0F, 48.0F)
                        // Sideframes
                        .texOffs(152, 650).addBox(-84.0F, 38.0F, -43.0F, 48.0F, 8.0F, 2.0F)
                        .texOffs(152, 672).addBox(-84.0F, 38.0F, 41.0F, 48.0F, 8.0F, 2.0F)
                        // Axle 1 (X = -72): Left wheel, Right wheel, Axle bar
                        .texOffs(272, 650).addBox(-80.0F, 38.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(316, 650).addBox(-80.0F, 38.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 468).addBox(-73.0F, 45.0F, -37.0F, 2.0F, 2.0F, 74.0F)
                        // Axle 2 (X = -48): Left wheel, Right wheel, Axle bar
                        .texOffs(360, 650).addBox(-56.0F, 38.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(404, 650).addBox(-56.0F, 38.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 468).addBox(-49.0F, 45.0F, -37.0F, 2.0F, 2.0F, 74.0F)

                        // 11. Rear Bogie Truck (Pivot X = 60)
                        // Bolster
                        .texOffs(0, 720).addBox(52.0F, 34.0F, -24.0F, 16.0F, 6.0F, 48.0F)
                        // Sideframes
                        .texOffs(152, 720).addBox(36.0F, 38.0F, -43.0F, 48.0F, 8.0F, 2.0F)
                        .texOffs(152, 742).addBox(36.0F, 38.0F, 41.0F, 48.0F, 8.0F, 2.0F)
                        // Axle 3 (X = 48): Left wheel, Right wheel, Axle bar
                        .texOffs(272, 674).addBox(40.0F, 38.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(316, 674).addBox(40.0F, 38.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 468).addBox(47.0F, 45.0F, -37.0F, 2.0F, 2.0F, 74.0F)
                        // Axle 4 (X = 72): Left wheel, Right wheel, Axle bar
                        .texOffs(360, 674).addBox(64.0F, 38.0F, -41.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(404, 674).addBox(64.0F, 38.0F, 37.0F, 16.0F, 16.0F, 4.0F)
                        .texOffs(0, 468).addBox(71.0F, 45.0F, -37.0F, 2.0F, 2.0F, 74.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 1024, 1024);
    }
}
