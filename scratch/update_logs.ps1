$path = 'src/main/java/com/example/miningtools/client/model/LoggingCartModel.java'
$lines = [System.IO.File]::ReadAllLines($path)

$newLogLines = @"
        // 3. The 5 Round Logs (24 x 4 x 4 each, calibrated to fit between side stakes without clipping)
        // Bottom Row - Log 1 (Left: Z from -6.0 to -2.0)
        root.addOrReplaceChild(
                "log1",
                CubeListBuilder.create().texOffs(0, 40).addBox(-12.0F, -2.0F, -6.0F, 24.0F, 4.0F, 4.0F),
                PartPose.ZERO
        );

        // Bottom Row - Log 2 (Center: Z from -2.0 to 2.0)
        root.addOrReplaceChild(
                "log2",
                CubeListBuilder.create().texOffs(64, 40).addBox(-12.0F, -2.0F, -2.0F, 24.0F, 4.0F, 4.0F),
                PartPose.ZERO
        );

        // Bottom Row - Log 3 (Right: Z from 2.0 to 6.0)
        root.addOrReplaceChild(
                "log3",
                CubeListBuilder.create().texOffs(0, 54).addBox(-12.0F, -2.0F, 2.0F, 24.0F, 4.0F, 4.0F),
                PartPose.ZERO
        );

        // Top Row - Log 4 (Left Upper Groove: Z from -4.0 to 0.0)
        root.addOrReplaceChild(
                "log4",
                CubeListBuilder.create().texOffs(64, 54).addBox(-12.0F, -5.5F, -4.0F, 24.0F, 4.0F, 4.0F),
                PartPose.ZERO
        );

        // Top Row - Log 5 (Right Upper Groove: Z from 0.0 to 4.0)
        root.addOrReplaceChild(
                "log5",
                CubeListBuilder.create().texOffs(0, 68).addBox(-12.0F, -5.5F, 0.0F, 24.0F, 4.0F, 4.0F),
                PartPose.ZERO
        );
"@

$result = [System.Collections.Generic.List[string]]::new()
$inLogs = $false

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    if ($line.Contains("// 3. The 5 Round Logs")) {
        $inLogs = $true
        $result.Add($newLogLines)
        continue
    }
    if ($inLogs) {
        if ($line.Contains("// 4. The 5 Milled Lumber Stacks")) {
            $inLogs = $false
            $result.Add($line)
        }
        continue
    }
    $result.Add($line)
}

[System.IO.File]::WriteAllLines($path, $result)
Write-Output "Successfully updated logs in LoggingCartModel.java"
