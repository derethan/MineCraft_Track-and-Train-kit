package com.example.miningtools.client.model;

import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Shared helper for building authentic 2-axle railroad bogie trucks (matching the Caboose design).
 * Features cast steel sideframe brackets below the frame, two small flanged wheels per side
 * on each truck (8 wheels total per cart), center bolster mounts, and steel cross-axles.
 * Positioned so the bottom of each wheel rests precisely on the rails at Y = 7.0F.
 */
@OnlyIn(Dist.CLIENT)
public class TruckModelHelper {

    public static CubeListBuilder addStandardBogieTrucks(CubeListBuilder builder) {
        return addStandardBogieTrucks(builder, -7.0F, 7.0F);
    }

    public static CubeListBuilder addStandardBogieTrucks(CubeListBuilder builder, float frontPivotX, float rearPivotX) {
        addSingleTruck(builder, frontPivotX);
        addSingleTruck(builder, rearPivotX);
        return builder;
    }

    private static void addSingleTruck(CubeListBuilder builder, float pivotX) {
        // 1. Center bolster mount connecting to undercarriage frame
        builder.texOffs(104, 20).addBox(pivotX - 1.0F, 3.6F, -5.5F, 2.0F, 1.2F, 11.0F)

        // 2. Cast steel sideframe brackets (with spring packet & journal boxes)
        // Left sideframe (outside the wheels at Z = -6.8F to -6.2F)
        .texOffs(54, 20).addBox(pivotX - 3.7F, 4.4F, -6.8F, 7.4F, 1.6F, 0.6F)
        // Right sideframe (outside the wheels at Z = 6.2F to 6.8F)
        .texOffs(54, 20).addBox(pivotX - 3.7F, 4.4F, 6.2F, 7.4F, 1.6F, 0.6F)

        // 3. Axle 1 (Front wheel of truck, offset -2.0F from pivot)
        // Left wheel (centered at Z = -6.0F)
        .texOffs(80, 20).addBox(pivotX - 3.2F, 4.6F, -6.4F, 2.4F, 2.4F, 0.8F)
        // Right wheel (centered at Z = 6.0F)
        .texOffs(80, 20).addBox(pivotX - 3.2F, 4.6F, 5.6F, 2.4F, 2.4F, 0.8F)
        // Axle bar connecting wheels
        .texOffs(104, 20).addBox(pivotX - 2.4F, 5.4F, -5.6F, 0.8F, 0.8F, 11.2F)

        // 4. Axle 2 (Rear wheel of truck, offset +2.0F from pivot)
        // Left wheel
        .texOffs(80, 20).addBox(pivotX + 0.8F, 4.6F, -6.4F, 2.4F, 2.4F, 0.8F)
        // Right wheel
        .texOffs(80, 20).addBox(pivotX + 0.8F, 4.6F, 5.6F, 2.4F, 2.4F, 0.8F)
        // Axle bar connecting wheels
        .texOffs(104, 20).addBox(pivotX + 1.6F, 5.4F, -5.6F, 0.8F, 0.8F, 11.2F);
    }
}
