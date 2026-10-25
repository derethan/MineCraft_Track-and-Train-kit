$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\Config.java"
$content = [System.IO.File]::ReadAllText($path)

$target1 = "    public static final ModConfigSpec.ConfigValue<List<? extends String>> SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS;"
$replacement1 = @"
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS;

    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_DRAG_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_BUMP_TRANSFER;
    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_BUMP_DAMPING;
"@

$target2 = "        BUILDER.pop();`r`n    }`r`n`r`n    public static final ModConfigSpec SPEC = BUILDER.build();"
if (-not $content.Contains($target2)) {
    $target2 = "        BUILDER.pop();`n    }`n`n    public static final ModConfigSpec SPEC = BUILDER.build();"
}

$replacement2 = @"
        BUILDER.pop();

        BUILDER.push("rolling_stock");

        ROLLING_STOCK_DRAG_COEFFICIENT = BUILDER
                .comment("Drag/friction coefficient applied to rolling stock carts per tick when unpowered or rolling free.",
                         "0.94 = 6% speed loss per tick. Lower numbers stop carts faster and eliminate jitter; 0.98 is vanilla.")
                .defineInRange("dragCoefficient", 0.94D, 0.70D, 0.999D);

        ROLLING_STOCK_BUMP_TRANSFER = BUILDER
                .comment("Ratio of relative momentum transferred between uncoupled carts when bumping on tracks.",
                         "Default 0.40 provides gentle shunting bumps without setting carts flying down the track.")
                .defineInRange("bumpTransferRatio", 0.40D, 0.05D, 1.0D);

        ROLLING_STOCK_BUMP_DAMPING = BUILDER
                .comment("Kinetic energy retention factor applied to colliding carts after a bump.",
                         "Default 0.85 dampens impact energy to prevent continuous acceleration.")
                .defineInRange("bumpDamping", 0.85D, 0.10D, 1.0D);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
"@

if ($content.Contains($target1)) {
    $content = $content.Replace($target1, $replacement1)
    $content = $content.Replace($target2, $replacement2)
    [System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Config.java updated successfully."
} else {
    Write-Error "Target not found in Config.java"
}
