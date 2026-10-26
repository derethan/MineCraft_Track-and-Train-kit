package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.TurretCartRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurretCartModel extends EntityModel<TurretCartRenderState> {
    private final ModelPart root;
    private final ModelPart turret;

    public TurretCartModel(ModelPart root) {
        super(root);
        this.root = root;
        this.turret = root.getChild("turret");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Fixed Wagon Body & Armored Parapet with Caboose-Style 2-Axle Bogie Trucks
        CubeListBuilder bodyBuilder = CubeListBuilder.create()
                // Heavy Underframe Deck (24 x 2 x 16)
                .texOffs(0, 0).addBox(-12.0F, 2.0F, -8.0F, 24.0F, 2.0F, 16.0F)
                // Center Sill Draft Beam
                .texOffs(0, 20).addBox(-10.0F, 4.0F, -3.0F, 20.0F, 2.0F, 6.0F)
                // Front Coupler Hitch
                .texOffs(0, 20).addBox(-15.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F)
                // Rear Coupler Hitch
                .texOffs(0, 20).addBox(12.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F);

        // Add 2-axle bogie trucks with sideframe brackets and 2 small wheels per side
        TruckModelHelper.addStandardBogieTrucks(bodyBuilder);

        bodyBuilder
                // 2. Armored Wagon Parapet Walls (matching Caboose steel plate style)
                // Front armored bulkhead (X = -12)
                .texOffs(0, 30).addBox(-12.0F, -4.0F, -8.0F, 2.0F, 6.0F, 16.0F)
                // Rear armored bulkhead (X = 10)
                .texOffs(0, 30).addBox(10.0F, -4.0F, -8.0F, 2.0F, 6.0F, 16.0F)
                // Left armored side plating (Z = -8)
                .texOffs(0, 54).addBox(-10.0F, -4.0F, -8.0F, 20.0F, 6.0F, 2.0F)
                // Right armored side plating (Z = 6)
                .texOffs(0, 54).addBox(-10.0F, -4.0F, 6.0F, 20.0F, 6.0F, 2.0F)

                // 3. Heavy Arrow Ammo Chests on Deck
                // Front ammo magazine chest
                .texOffs(46, 30).addBox(-9.0F, -2.0F, -5.0F, 4.0F, 4.0F, 10.0F)
                // Rear ammo magazine chest
                .texOffs(46, 30).addBox(5.0F, -2.0F, -5.0F, 4.0F, 4.0F, 10.0F);

        root.addOrReplaceChild("body", bodyBuilder, PartPose.ZERO);

        // 4. Rotating Armored Turret & Twin Bow Mounts
        root.addOrReplaceChild(
                "turret",
                CubeListBuilder.create()
                        // Turret Bearing Ring (12 x 2 x 12)
                        .texOffs(0, 64).addBox(-6.0F, -2.0F, -6.0F, 12.0F, 2.0F, 12.0F)
                        // Armored Cupola Housing (10 x 6 x 10)
                        .texOffs(0, 80).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 6.0F, 10.0F)
                        // Turret Armored Roof Cap (12 x 1 x 12)
                        .texOffs(0, 98).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F)

                        // Left Bow Mount & Arm (Z = -7.5)
                        .texOffs(46, 64).addBox(-3.0F, -7.0F, -7.5F, 6.0F, 4.0F, 2.0F)
                        .texOffs(46, 72).addBox(-6.0F, -8.0F, -9.0F, 12.0F, 6.0F, 2.0F)
                        .texOffs(46, 82).addBox(3.0F, -6.0F, -8.0F, 4.0F, 2.0F, 1.0F)

                        // Right Bow Mount & Arm (Z = 5.5)
                        .texOffs(46, 64).addBox(-3.0F, -7.0F, 5.5F, 6.0F, 4.0F, 2.0F)
                        .texOffs(46, 72).addBox(-6.0F, -8.0F, 7.0F, 12.0F, 6.0F, 2.0F)
                        .texOffs(46, 82).addBox(3.0F, -6.0F, 7.0F, 4.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(TurretCartRenderState state) {
        super.setupAnim(state);
        // Rotate turret relative to cart orientation
        float relYaw = (state.turretYaw - state.yRot) * Mth.DEG_TO_RAD;
        this.turret.yRot = relYaw;
    }
}
