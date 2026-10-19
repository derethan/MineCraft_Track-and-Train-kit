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

    public LoggingCartModel(ModelPart root) {
        super(root);
        this.log1 = root.getChild("log1");
        this.log2 = root.getChild("log2");
        this.log3 = root.getChild("log3");
        this.log4 = root.getChild("log4");
        this.log5 = root.getChild("log5");
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

        // 2. Upright Wooden Side Stakes (4 vertical posts: 2 x 12 x 2)
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

        // 3. The 5 Oak Logs (24 x 5 x 5 each)
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

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(CargoMinecartRenderState state) {
        super.setupAnim(state);
        int count = state.storedLogCount;
        // 0 logs: empty flatbed
        // 1+ logs (at least 1 stack or item): log1 visible
        // 128+ logs (2+ stacks): log2 visible
        // 256+ logs (4+ stacks): log3 visible (bottom row full)
        // 512+ logs (8+ stacks): log4 visible
        // 1024+ logs (16+ stacks): all 5 logs visible (full pyramid)
        this.log1.visible = count >= 1;
        this.log2.visible = count >= 128;
        this.log3.visible = count >= 256;
        this.log4.visible = count >= 512;
        this.log5.visible = count >= 1024;
    }
}
