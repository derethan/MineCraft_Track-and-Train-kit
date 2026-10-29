$path = 'src/main/java/com/example/miningtools/client/model/LoggingCartModel.java'
$lines = [System.IO.File]::ReadAllLines($path)

$newLumberLines = @"
        // 4. The 5 Milled Lumber Stacks (2x6 boards, elevated 1.5 units to rest atop pallets)
        // Bottom Row - Lumber 1 (Left bundle: 24 x 2.5 x 6)
        root.addOrReplaceChild(
                "lumber1",
                CubeListBuilder.create().texOffs(0, 80).addBox(-12.0F, -2.0F, -6.0F, 24.0F, 2.5F, 6.0F),
                PartPose.ZERO
        );

        // Bottom Row - Lumber 2 (Right bundle: 24 x 2.5 x 6)
        root.addOrReplaceChild(
                "lumber2",
                CubeListBuilder.create().texOffs(64, 80).addBox(-12.0F, -2.0F, 0.0F, 24.0F, 2.5F, 6.0F),
                PartPose.ZERO
        );

        // Middle Row - Lumber 3 (Center wide bundle: 24 x 3 x 11)
        root.addOrReplaceChild(
                "lumber3",
                CubeListBuilder.create().texOffs(0, 92).addBox(-12.0F, -5.0F, -5.5F, 24.0F, 3.0F, 11.0F),
                PartPose.ZERO
        );

        // Top Row - Lumber 4 (Left bundle: 24 x 3 x 5)
        root.addOrReplaceChild(
                "lumber4",
                CubeListBuilder.create().texOffs(0, 108).addBox(-12.0F, -8.0F, -5.0F, 24.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        // Top Row - Lumber 5 (Right bundle: 24 x 3 x 5)
        root.addOrReplaceChild(
                "lumber5",
                CubeListBuilder.create().texOffs(64, 108).addBox(-12.0F, -8.0F, 0.0F, 24.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        // 4b. Tiny Wooden Shipping Pallets (resting on flatbed deck under plank stacks)
        CubeListBuilder palletBuilder = CubeListBuilder.create();
        // Pallet 1 (Front: X from -10.5 to -1.5)
        // 4 Top Slats (spaced across X)
        palletBuilder.texOffs(72, 88).addBox(-10.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(-7.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(-4.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(-2.0F, 0.5F, -6.0F, 1.0F, 0.4F, 12.0F);
        // 3 Stringers / Spacers (leaving forklift tine pockets)
        palletBuilder.texOffs(72, 102).addBox(-10.5F, 0.9F, -5.8F, 9.5F, 0.8F, 1.4F);
        palletBuilder.texOffs(72, 102).addBox(-10.5F, 0.9F, -0.7F, 9.5F, 0.8F, 1.4F);
        palletBuilder.texOffs(72, 102).addBox(-10.5F, 0.9F, 4.4F, 9.5F, 0.8F, 1.4F);
        // 3 Bottom Runner Boards
        palletBuilder.texOffs(72, 106).addBox(-10.5F, 1.7F, -5.8F, 9.5F, 0.3F, 1.4F);
        palletBuilder.texOffs(72, 106).addBox(-10.5F, 1.7F, -0.7F, 9.5F, 0.3F, 1.4F);
        palletBuilder.texOffs(72, 106).addBox(-10.5F, 1.7F, 4.4F, 9.5F, 0.3F, 1.4F);

        // Pallet 2 (Rear: X from 1.5 to 10.5)
        // 4 Top Slats (spaced across X)
        palletBuilder.texOffs(72, 88).addBox(1.5F, 0.5F, -6.0F, 1.0F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(3.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(6.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        palletBuilder.texOffs(72, 88).addBox(9.5F, 0.5F, -6.0F, 1.5F, 0.4F, 12.0F);
        // 3 Stringers / Spacers
        palletBuilder.texOffs(72, 102).addBox(1.5F, 0.9F, -5.8F, 9.5F, 0.8F, 1.4F);
        palletBuilder.texOffs(72, 102).addBox(1.5F, 0.9F, -0.7F, 9.5F, 0.8F, 1.4F);
        palletBuilder.texOffs(72, 102).addBox(1.5F, 0.9F, 4.4F, 9.5F, 0.8F, 1.4F);
        // 3 Bottom Runner Boards
        palletBuilder.texOffs(72, 106).addBox(1.5F, 1.7F, -5.8F, 9.5F, 0.3F, 1.4F);
        palletBuilder.texOffs(72, 106).addBox(1.5F, 1.7F, -0.7F, 9.5F, 0.3F, 1.4F);
        palletBuilder.texOffs(72, 106).addBox(1.5F, 1.7F, 4.4F, 9.5F, 0.3F, 1.4F);

        root.addOrReplaceChild("pallets", palletBuilder, PartPose.ZERO);
"@

$newAnimLines = @"
        boolean isLog = state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.LOG;
        boolean isWood = state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.WOOD;
        boolean isOre = state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.ORE;
        boolean isIngot = state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.INGOT;
        boolean isContainer = state.filterType == com.example.miningtools.entity.CargoMinecartEntity.FilterType.NONE && count > 0;

        this.shippingContainer.visible = isContainer;
        this.stakes.visible = isLog;
        this.pallets.visible = isWood && count >= 1;

        this.log1.visible = isLog && count >= 1;
        this.log2.visible = isLog && count >= 128;
        this.log3.visible = isLog && count >= 256;
        this.log4.visible = isLog && count >= 512;
        this.log5.visible = isLog && count >= 1024;

        this.lumber1.visible = isWood && count >= 1;
        this.lumber2.visible = isWood && count >= 128;
        this.lumber3.visible = isWood && count >= 256;
        this.lumber4.visible = isWood && count >= 512;
        this.lumber5.visible = isWood && count >= 1024;
"@

$result = [System.Collections.Generic.List[string]]::new()
$inLumber = $false
$inAnim = $false

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    if ($line.Contains("// 4. The 5 Milled Lumber Stacks")) {
        $inLumber = $true
        $result.Add($newLumberLines)
        continue
    }
    if ($inLumber) {
        if ($line.Contains("// 5. The 5 Ore Pile Layers")) {
            $inLumber = $false
            $result.Add($line)
        }
        continue
    }
    if ($line.Contains("boolean isLog = state.filterType")) {
        $inAnim = $true
        $result.Add($newAnimLines)
        continue
    }
    if ($inAnim) {
        if ($line.Contains("this.ore1.visible = isOre")) {
            $inAnim = $false
            $result.Add($line)
        }
        continue
    }
    $result.Add($line)
}

[System.IO.File]::WriteAllLines($path, $result)
Write-Output "Successfully updated LoggingCartModel.java"
