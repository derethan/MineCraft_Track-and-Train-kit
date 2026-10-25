$files = @(
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\AggregateHopperCartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\CargoMinecartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LiquidMinecartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LivestockCartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\TurretCartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\CabooseMinecartEntity.java"
)

$slowdownMethod = @"

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        return TrainChainingUtil.applyRollingStockSlowdown(this, deltaMovement);
    }
}
"@

foreach ($file in $files) {
    $content = [System.IO.File]::ReadAllText($file)
    if ($content.Contains("protected Vec3 applyNaturalSlowdown")) {
        Write-Host "File already has applyNaturalSlowdown: $([System.IO.Path]::GetFileName($file))"
        continue
    }

    $lastBraceIdx = $content.LastIndexOf('}')
    if ($lastBraceIdx -ne -1) {
        $newContent = $content.Substring(0, $lastBraceIdx) + $slowdownMethod
        [System.IO.File]::WriteAllText($file, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "Added applyNaturalSlowdown to: $([System.IO.Path]::GetFileName($file))"
    } else {
        Write-Error "No closing brace found in: $([System.IO.Path]::GetFileName($file))"
    }
}
