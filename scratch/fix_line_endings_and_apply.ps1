$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Fix CartRenameScreen.java to use mc.gui.setScreen
$crsPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CartRenameScreen.java"
$crsContent = [System.IO.File]::ReadAllText($crsPath, $utf8NoBom)
$crsContent = $crsContent.Replace("mc.setScreen(", "mc.gui.setScreen(")
[System.IO.File]::WriteAllText($crsPath, $crsContent, $utf8NoBom)
Write-Host "Fixed CartRenameScreen.java"

# 2. Normalize CRLF -> LF on target files so here-string replacements match 100%
$filesToNormalize = @(
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\util\TrainChainingUtil.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\BasicLocomotiveEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\DieselLocomotiveEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\ElectricLocomotiveEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LiquidMinecartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LivestockCartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\CabooseMinecartEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\FluidLoaderBlockEntity.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\MiningTools.java",
    "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\MiningToolsClient.java"
)

foreach ($f in $filesToNormalize) {
    $txt = [System.IO.File]::ReadAllText($f, $utf8NoBom).Replace("`r`n", "`n")
    [System.IO.File]::WriteAllText($f, $txt, $utf8NoBom)
}

# Re-run the update scripts now that all target files and scripts use LF!
& "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch\update_braking_and_locos.ps1"
& "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch\fix_caboose.ps1"
& "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch\upgrade_fluid_and_entity_loaders.ps1"
& "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch\register_all_new_blocks.ps1"
