package com.example.miningtools.client.model;

import com.example.miningtools.client.renderer.CargoMinecartRenderState;

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
public class LoggingCartModel extends EntityModel<CargoMinecartRenderState> {
    private final ModelPart log1;
    private final ModelPart log2;
    private final ModelPart log3;
    private final ModelPart log4;
    private final ModelPart log5;

    private final ModelPart lumber1;
    private final ModelPart lumber2;
    private final ModelPart lumber3;
    private final ModelPart lumber4;
    private final ModelPart lumber5;

    public LoggingCartModel(ModelPart root) {
        super(root);
        this.log1 = root.getChild("log1");
        this.log2 = root.getChild("log2");
        this.log3 = root.getChild("log3");
        this.log4 = root.getChild("log4");
        this.log5 = root.getChild("log5");

        this.lumber1 = root.getChild("lumber1");
        this.lumber2 = root.getChild("lumber2");
        this.lumber3 = root.getChild("lumber3");
        this.lumber4 = root.getChild("lumber4");
        this.lumber5 = root.getChild("lumber5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Flatbed Deck & Undercarriage Frame
        root.addOrReplaceChild(
                "chassis",
                CubeListBuilder.create()
                        // Diamond-plate flatbed deck (24 x 2 x 16)
                        .texOffs(0, 0).addBox(-12.0F, 2.0F, -8.0F, 24.0F, 2.0F, 16.0F)
                        // Undercarriage center beam (18 x 2 x 8)
                        .texOffs(0, 20).addBox(-9.0F, 4.0F, -4.0F, 18.0F, 2.0F, 8.0F)
                        // Front Coupler / Hitch (2 x 2 x 4)
                        .texOffs(0, 32).addBox(-14.0F, 3.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // Back Coupler / Hitch (2 x 2 x 4)
                        .texOffs(0, 32).addBox(12.0F, 3.0F, -2.0F, 2.0F, 2.0F, 4.0F)
                        // 4 Rail Wheels (Flanged steel wheels tucked under the deck)
                        .texOffs(54, 20).addBox(-8.0F, 3.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(68, 20).addBox(4.0F, 3.0F, -7.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(82, 20).addBox(-8.0F, 3.0F, 5.0F, 4.0F, 4.0F, 2.0F)
                        .texOffs(96, 20).addBox(4.0F, 3.0F, 5.0F, 4.0F, 4.0F, 2.0F),
                PartPose.ZERO
        );

        // 2. Upright Side Stakes (4 vertical posts: 2 x 12 x 2)
        root.addOrReplaceChild(
                "stakes",
                CubeListBuilder.create()
                        // Stake 1: Front Left
                        .texOffs(80, 0).addBox(-8.0F, -10.0F, -8.1F, 2.0F, 12.0F, 2.0F)
                        // Stake 2: Back Left
                        .texOffs(88, 0).addBox(6.0F, -10.0F, -8.1F, 2.0F, 12.0F, 2.0F)
                        // Stake 3: Front Right
                        .texOffs(96, 0).addBox(-8.0F, -10.0F, 6.1F, 2.0F, 12.0F, 2.0F)
                        // Stake 4: Back Right
                        .texOffs(104, 0).addBox(6.0F, -10.0F, 6.1F, 2.0F, 12.0F, 2.0F),
                PartPose.ZERO
        );

        // 3. The 5 Round Logs (24 x 5 x 5 each)
        // Bottom Row - Log 1 (Left)
        root.addOrReplaceChild(
                "log1",
                CubeListBuilder.create().texOffs(0, 40).addBox(-12.0F, -3.0F, -7.5F, 24.0F, 5.0F, 5.0F),
                PartPose.ZERO
        );

        // Bottom Row - Log 2 (Center)
        root.addOrReplaceChild(
                "log2",
                CubeListBuilder.create().texOffs(64, 40).addBox(-12.0F, -3.0F, -2.5F, 24.0F, 5.0F, 5.0F),
                PartPose.ZERO
        );

        // Bottom Row - Log 3 (Right)
        root.addOrReplaceChild(
                "log3",
                CubeListBuilder.create().texOffs(0, 54).addBox(-12.0F, -3.0F, 2.5F, 24.0F, 5.0F, 5.0F),
                PartPose.ZERO
        );

        // Top Row - Log 4 (Left Upper Groove)
        root.addOrReplaceChild(
                "log4",
                CubeListBuilder.create().texOffs(64, 54).addBox(-12.0F, -7.5F, -5.0F, 24.0F, 5.0F, 5.0F),
                PartPose.ZERO
        );

        // Top Row - Log 5 (Right Upper Groove)
        root.addOrReplaceChild(
                "log5",
                CubeListBuilder.create().texOffs(0, 68).addBox(-12.0F, -7.5F, 0.0F, 24.0F, 5.0F, 5.0F),
                PartPose.ZERO
        );

        // 4. The 5 Milled Lumber Stacks (2x6 boards)
        // Bottom Row - Lumber 1 (Left bundle: 24 x 2.5 x 6)
        root.addOrReplaceChild(
                "lumber1",
                CubeListBuilder.create().texOffs(0, 80).addBox(-12.0F, -0.5F, -6.0F, 24.0F, 2.5F, 6.0F),
                PartPose.ZERO
        );

        // Bottom Row - Lumber 2 (Right bundle: 24 x 2.5 x 6)
        root.addOrReplaceChild(
                "lumber2",
                CubeListBuilder.create().texOffs(64, 80).addBox(-12.0F, -0.5F, 0.0F, 24.0F, 2.5F, 6.0F),
                PartPose.ZERO
        );

        // Middle Row - Lumber 3 (Center wide bundle: 24 x 3 x 11)
        root.addOrReplaceChild(
                "lumber3",
                CubeListBuilder.create().texOffs(0, 92).addBox(-12.0F, -3.5F, -5.5F, 24.0F, 3.0F, 11.0F),
                PartPose.ZERO
        );

        // Top Row - Lumber 4 (Left bundle: 24 x 3 x 5)
        root.addOrReplaceChild(
                "lumber4",
                CubeListBuilder.create().texOffs(0, 108).addBox(-12.0F, -6.5F, -5.0F, 24.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        // Top Row - Lumber 5 (Right bundle: 24 x 3 x 5)
        root.addOrReplaceChild(
                "lumber5",
                CubeListBuilder.create().texOffs(64, 108).addBox(-12.0F, -6.5F, 0.0F, 24.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(CargoMinecartRenderState state) {
        super.setupAnim(state);
        int count = state.storedCount;

        if (state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.LOG) {
            this.log1.visible = count >= 1;
            this.log2.visible = count >= 128;
            this.log3.visible = count >= 256;
            this.log4.visible = count >= 512;
            this.log5.visible = count >= 1024;

            this.lumber1.visible = false;
            this.lumber2.visible = false;
            this.lumber3.visible = false;
            this.lumber4.visible = false;
            this.lumber5.visible = false;
        } else if (state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.WOOD) {
            this.log1.visible = false;
            this.log2.visible = false;
            this.log3.visible = false;
            this.log4.visible = false;
            this.log5.visible = false;

            this.lumber1.visible = count >= 1;
            this.lumber2.visible = count >= 128;
            this.lumber3.visible = count >= 256;
            this.lumber4.visible = count >= 512;
            this.lumber5.visible = count >= 1024;
        } else {
            // Unfiltered / Empty / None: Always render as an empty flatbed
            this.log1.visible = false;
            this.log2.visible = false;
            this.log3.visible = false;
            this.log4.visible = false;
            this.log5.visible = false;

            this.lumber1.visible = false;
            this.lumber2.visible = false;
            this.lumber3.visible = false;
            this.lumber4.visible = false;
            this.lumber5.visible = false;
        }
    }
}
