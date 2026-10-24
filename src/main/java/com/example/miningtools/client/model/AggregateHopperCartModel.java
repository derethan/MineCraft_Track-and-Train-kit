package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.AggregateHopperCartRenderState;

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
public class AggregateHopperCartModel extends EntityModel<AggregateHopperCartRenderState> {
    private final ModelPart mound1;
    private final ModelPart mound2;
    private final ModelPart mound3;

    public AggregateHopperCartModel(ModelPart root) {
        super(root);
        this.mound1 = root.getChild("mound1");
        this.mound2 = root.getChild("mound2");
        this.mound3 = root.getChild("mound3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Undercarriage Frame & Wheels
        root.addOrReplaceChild(
                "chassis",
                CubeListBuilder.create()
                        // Center undercarriage draft sill beam (20 x 2 x 6)
                        .texOffs(0, 0).addBox(-10.0F, 3.0F, -3.0F, 20.0F, 2.0F, 6.0F)
                        // Front Coupler / Bumper (2 x 2 x 4)
                        .texOffs(0, 10).addBox(-14.0F, 2.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // Back Coupler / Bumper (2 x 2 x 4)
                        .texOffs(0, 10).addBox(12.0F, 2.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // 4 Steel Rail Wheels
                        .texOffs(54, 0).addBox(-8.0F, 2.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(68, 0).addBox(4.0F, 2.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(82, 0).addBox(-8.0F, 2.0F, 5.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(96, 0).addBox(4.0F, 2.0F, 5.0F, 4.0F, 4.0F, 2.0F),
                PartPose.ZERO
        );

        // 2. Hopper Tub (Walls & Sloped Discharge Bottom)
        root.addOrReplaceChild(
                "hopper_body",
                CubeListBuilder.create()
                        // Bottom Floor Plate (20 x 1 x 10)
                        .texOffs(0, 18).addBox(-10.0F, 2.0F, -5.0F, 20.0F, 1.0F, 10.0F)
                        // Left Wall (24 x 10 x 2)
                        .texOffs(0, 30).addBox(-12.0F, -8.0F, -7.0F, 24.0F, 10.0F, 2.0F)
                        // Right Wall (24 x 10 x 2)
                        .texOffs(0, 44).addBox(-12.0F, -8.0F, 5.0F, 24.0F, 10.0F, 2.0F)
                        // Front End Wall / Bulkhead (2 x 10 x 10)
                        .texOffs(54, 30).addBox(-12.0F, -8.0F, -5.0F, 2.0F, 10.0F, 10.0F)
                        // Back End Wall / Bulkhead (2 x 10 x 10)
                        .texOffs(78, 30).addBox(10.0F, -8.0F, -5.0F, 2.0F, 10.0F, 10.0F)
                        // Left External Ribs (4 vertical structural stiffeners)
                        .texOffs(0, 60).addBox(-7.0F, -8.0F, -8.0F, 1.5F, 10.0F, 1.0F)
                        .texOffs(0, 60).addBox(-2.0F, -8.0F, -8.0F, 1.5F, 10.0F, 1.0F)
                        .texOffs(0, 60).addBox(3.0F, -8.0F, -8.0F, 1.5F, 10.0F, 1.0F)
                        // Right External Ribs (4 vertical structural stiffeners)
                        .texOffs(0, 60).addBox(-7.0F, -8.0F, 7.0F, 1.5F, 10.0F, 1.0F)
                        .texOffs(0, 60).addBox(-2.0F, -8.0F, 7.0F, 1.5F, 10.0F, 1.0F)
                        .texOffs(0, 60).addBox(3.0F, -8.0F, 7.0F, 1.5F, 10.0F, 1.0F),
                PartPose.ZERO
        );

        // 3. Three Heaped Mounds of Loose Aggregate (Visible when loaded)
        // Mound 1 (Front heap)
        root.addOrReplaceChild(
                "mound1",
                CubeListBuilder.create()
                        .texOffs(0, 75).addBox(-9.5F, -11.0F, -4.5F, 6.5F, 4.0F, 9.0F)
                        .texOffs(32, 75).addBox(-8.5F, -12.5F, -3.5F, 4.5F, 2.0F, 7.0F),
                PartPose.ZERO
        );

        // Mound 2 (Center heap - highest peak)
        root.addOrReplaceChild(
                "mound2",
                CubeListBuilder.create()
                        .texOffs(0, 90).addBox(-3.5F, -11.5F, -4.5F, 7.0F, 4.5F, 9.0F)
                        .texOffs(34, 90).addBox(-2.5F, -13.5F, -3.5F, 5.0F, 2.5F, 7.0F),
                PartPose.ZERO
        );

        // Mound 3 (Rear heap)
        root.addOrReplaceChild(
                "mound3",
                CubeListBuilder.create()
                        .texOffs(60, 75).addBox(3.0F, -11.0F, -4.5F, 6.5F, 4.0F, 9.0F)
                        .texOffs(92, 75).addBox(4.0F, -12.5F, -3.5F, 4.5F, 2.0F, 7.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(AggregateHopperCartRenderState state) {
        super.setupAnim(state);
        boolean hasItems = state.storedCount > 0;
        this.mound1.visible = hasItems;
        this.mound2.visible = hasItems;
        this.mound3.visible = hasItems;
    }
}
