Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
$w = $base.Width
$h = $base.Height

# Palette colors from the base rail
$C_OUTLINE = [System.Drawing.Color]::FromArgb(255, 35, 36, 44)       # #23242c
$C_SHADOW  = [System.Drawing.Color]::FromArgb(255, 71, 81, 94)       # #47515e
$C_BODY    = [System.Drawing.Color]::FromArgb(255, 103, 115, 130)    # #677382
$C_LIGHT   = [System.Drawing.Color]::FromArgb(255, 156, 168, 178)    # #9ca8b2
$C_GLINT   = [System.Drawing.Color]::FromArgb(255, 228, 232, 236)    # #e4e8ec

$WOOD_DARK = [System.Drawing.Color]::FromArgb(255, 58, 32, 22)
$WOOD_MED  = [System.Drawing.Color]::FromArgb(255, 160, 92, 54)
$WOOD_LIGHT= [System.Drawing.Color]::FromArgb(255, 226, 158, 98)
$WOOD_HILITE=[System.Drawing.Color]::FromArgb(255, 244, 195, 145)

$GREEN_HI = [System.Drawing.Color]::FromArgb(255, 74, 222, 128)
$GREEN_MD = [System.Drawing.Color]::FromArgb(255, 22, 163, 74)
$GREEN_DK = [System.Drawing.Color]::FromArgb(255, 20, 83, 45)

$BRASS_HI = [System.Drawing.Color]::FromArgb(255, 254, 240, 138)
$BRASS_MD = [System.Drawing.Color]::FromArgb(255, 234, 179, 8)
$BRASS_DK = [System.Drawing.Color]::FromArgb(255, 161, 98, 7)

$RED_HI   = [System.Drawing.Color]::FromArgb(255, 248, 113, 113)
$RED_MD   = [System.Drawing.Color]::FromArgb(255, 220, 38, 38)
$RED_DK   = [System.Drawing.Color]::FromArgb(255, 153, 27, 27)

$White = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)

$ox = 16
$oy = 37

# Helper function to classify steel vs wood
function IsSteelColor($c) {
    if ($c.A -lt 80) { return $false }
    $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
    $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
    return ($isSteel -or $isOutline)
}

# -------------------------------------------------------------
# 1. PERFECT TURNOUT RAIL (RIGHT)
# -------------------------------------------------------------
$turnout = New-Object System.Drawing.Bitmap 128, 128

# Draw branch ties (SW branch, x in 0..42 of flipped track)
for ($x = 0; $x -le 42; $x++) {
    for ($y = 22; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $turnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Draw mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $turnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Extended Tie 1 for Switch Stand
for ($ex = 18; $ex -le 26; $ex++) {
    $ey = 46 + [int](($ex - 18) * 0.5)
    $turnout.SetPixel($ex, $ey, $WOOD_LIGHT)
    $turnout.SetPixel($ex, $ey + 1, $WOOD_MED)
    $turnout.SetPixel($ex, $ey + 2, $WOOD_DARK)
}

# Draw branch rails (SW branch), stopping cleanly before the straight bottom rail
for ($x = 0; $x -le 38; $x++) {
    for ($y = 22; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if (IsSteelColor $c) {
            $turnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Draw frog curve from mainline top rail to branch bottom rail
$frogCurve = @(
    @(39, 48), @(40, 49), @(41, 51), @(42, 53), @(43, 55),
    @(44, 57), @(45, 60), @(46, 62), @(47, 65), @(48, 67),
    @(49, 70), @(50, 72), @(51, 75), @(52, 77)
)
foreach ($pt in $frogCurve) {
    $turnout.SetPixel($pt[0], $pt[1] - 1, $C_GLINT)
    $turnout.SetPixel($pt[0], $pt[1], $C_LIGHT)
    $turnout.SetPixel($pt[0], $pt[1] + 1, $C_BODY)
    $turnout.SetPixel($pt[0], $pt[1] + 2, $C_OUTLINE)
}

# Draw Mainline rails on top
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if (IsSteelColor $c) {
            $turnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Switch Stand (grounded on extended tie 1 at x=19, y=47)
# Base
$turnout.SetPixel(18, 47, $C_OUTLINE); $turnout.SetPixel(19, 47, $C_BODY); $turnout.SetPixel(20, 47, $C_OUTLINE)
$turnout.SetPixel(18, 48, $C_OUTLINE); $turnout.SetPixel(19, 48, $C_SHADOW); $turnout.SetPixel(20, 48, $C_OUTLINE)
# Mast
for ($sy = 33; $sy -le 46; $sy++) {
    $turnout.SetPixel(19, $sy, [System.Drawing.Color]::FromArgb(255, 30, 32, 38))
}
# Green switch target
for ($tx = 16; $tx -le 22; $tx++) {
    for ($ty = 27; $ty -le 32; $ty++) {
        $turnout.SetPixel($tx, $ty, $GREEN_MD)
    }
}
$turnout.SetPixel(19, 28, $GREEN_HI); $turnout.SetPixel(19, 29, $GREEN_HI); $turnout.SetPixel(19, 30, $GREEN_HI)
# Sharp white arrow pointing right (turnout indicator)
$turnout.SetPixel(18, 29, $White)
$turnout.SetPixel(19, 29, $White)
$turnout.SetPixel(20, 29, $White)
$turnout.SetPixel(21, 29, $White)
$turnout.SetPixel(20, 28, $White)
$turnout.SetPixel(20, 30, $White)

$turnout.Save("src/main/resources/assets/miningtools/textures/item/turnout_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$turnout.Dispose()
Write-Output "turnout_rail.png saved"


# -------------------------------------------------------------
# 2. PERFECT LEFT TURNOUT RAIL
# -------------------------------------------------------------
$leftTurnout = New-Object System.Drawing.Bitmap 128, 128

# Draw branch ties (NE branch, x in 54..96 of flipped track)
for ($x = 54; $x -lt $w; $x++) {
    for ($y = 0; $y -le 35; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Draw mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Extend Tie 4 for Left Switch Stand
for ($ex = 96; $ex -le 104; $ex++) {
    $ey = 72 - [int](($ex - 96) * 0.5)
    $leftTurnout.SetPixel($ex, $ey, $WOOD_LIGHT)
    $leftTurnout.SetPixel($ex, $ey + 1, $WOOD_MED)
    $leftTurnout.SetPixel($ex, $ey + 2, $WOOD_DARK)
}

# Draw branch rails (NE branch)
for ($x = 56; $x -lt $w; $x++) {
    for ($y = 0; $y -le 35; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if (IsSteelColor $c) {
            $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Draw Mainline rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if (IsSteelColor $c) {
            $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Switch Stand on extended Tie 4 at x=103, y=69
$leftTurnout.SetPixel(102, 69, $C_OUTLINE); $leftTurnout.SetPixel(103, 69, $C_BODY); $leftTurnout.SetPixel(104, 69, $C_OUTLINE)
$leftTurnout.SetPixel(102, 70, $C_OUTLINE); $leftTurnout.SetPixel(103, 70, $C_SHADOW); $leftTurnout.SetPixel(104, 70, $C_OUTLINE)
for ($sy = 55; $sy -le 68; $sy++) {
    $leftTurnout.SetPixel(103, $sy, [System.Drawing.Color]::FromArgb(255, 30, 32, 38))
}
for ($tx = 100; $tx -le 106; $tx++) {
    for ($ty = 49; $ty -le 54; $ty++) {
        $leftTurnout.SetPixel($tx, $ty, $GREEN_MD)
    }
}
$leftTurnout.SetPixel(103, 50, $GREEN_HI); $leftTurnout.SetPixel(103, 51, $GREEN_HI); $leftTurnout.SetPixel(103, 52, $GREEN_HI)
# White left arrow `<`
$leftTurnout.SetPixel(104, 51, $White)
$leftTurnout.SetPixel(103, 51, $White)
$leftTurnout.SetPixel(102, 51, $White)
$leftTurnout.SetPixel(101, 51, $White)
$leftTurnout.SetPixel(102, 50, $White)
$leftTurnout.SetPixel(102, 52, $White)

$leftTurnout.Save("src/main/resources/assets/miningtools/textures/item/left_turnout_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$leftTurnout.Dispose()
Write-Output "left_turnout_rail.png saved"


# -------------------------------------------------------------
# 3. PERFECT WYE RAIL (TRUE 3-WAY Y TRACK)
# -------------------------------------------------------------
# Stem from SE (x in 48..96 of base track)
# Branch 1 to NW (x in 0..48 of base track)
# Branch 2 to SW (x in 0..48 of flipped track)
# (NO top-right NE arm -> forms an unmistakable 3-way Y!)
$wye = New-Object System.Drawing.Bitmap 128, 128

# 1. Ties for Stem (SE) and Branch 1 (NW) - full mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $wye.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}
# 2. Ties for Branch 2 (SW, only x in 0..48 of flipped track)
for ($x = 0; $x -le 48; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100 -and -not (IsSteelColor $c)) {
            $wye.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# 3. Rails for Branch 2 (SW)
for ($x = 0; $x -le 48; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if (IsSteelColor $c) {
            $wye.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# 4. Rails for Mainline (NW and SE)
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if (IsSteelColor $c) {
            $tx = $x + $ox; $ty = $y + $oy
            # Bottom frog flangeway gap at (64, 75)
            $isBotFrog = ($tx -ge 63 -and $tx -le 65 -and $ty -ge 74 -and $ty -le 76)
            if ($isBotFrog) {
                $wye.SetPixel($tx, $ty, $C_OUTLINE)
            } else {
                $wye.SetPixel($tx, $ty, $c)
            }
        }
    }
}

# 5. Central 3-Way Indicator: Directional Redstone Y-Lantern at Tie 3 center (64, 64)
# Cast iron casing
for ($lx = -4; $lx -le 4; $lx++) {
    for ($ly = -3; $ly -le 3; $ly++) {
        if ([Math]::Abs($lx) + [Math]::Abs($ly) -le 5) {
            $wye.SetPixel(64 + $lx, 64 + $ly, [System.Drawing.Color]::FromArgb(255, 35, 36, 42))
        }
    }
}
# Glowing redstone face
for ($lx = -3; $lx -le 3; $lx++) {
    for ($ly = -2; $ly -le 2; $ly++) {
        if ([Math]::Abs($lx) + [Math]::Abs($ly) -le 4) {
            $wye.SetPixel(64 + $lx, 64 + $ly, $RED_MD)
        }
    }
}
$wye.SetPixel(64, 64, $RED_HI)
# White glowing Y-glyph
$wye.SetPixel(64, 65, $White)
$wye.SetPixel(64, 64, $White)
$wye.SetPixel(63, 63, $White)
$wye.SetPixel(65, 63, $White)

$wye.Save("src/main/resources/assets/miningtools/textures/item/wye_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$wye.Dispose()
Write-Output "wye_rail.png saved"


# -------------------------------------------------------------
# 4. PERFECT HORN RAIL (PROMINENT BRASS HORNS & DYNAMIC SOUND WAVES)
# -------------------------------------------------------------
$horn = New-Object System.Drawing.Bitmap 128, 128
# Copy base straight track
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 50) {
            $horn.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Compressor bracket on Tie 3 (x=57..63, y=63..67)
for ($bx = 56; $bx -le 63; $bx++) {
    for ($by = 63; $by -le 67; $by++) {
        $horn.SetPixel($bx, $by, [System.Drawing.Color]::FromArgb(255, 32, 34, 40))
    }
}
$horn.SetPixel(59, 64, [System.Drawing.Color]::FromArgb(255, 70, 75, 85))
$horn.SetPixel(60, 64, [System.Drawing.Color]::FromArgb(255, 70, 75, 85))

# Triple Brass Chime Horns facing SE:
# Top chime horn:
$horn.SetPixel(60, 59, $BRASS_DK); $horn.SetPixel(61, 59, $BRASS_MD); $horn.SetPixel(62, 59, $BRASS_HI)
$horn.SetPixel(63, 60, $BRASS_MD); $horn.SetPixel(64, 60, $BRASS_HI)
$horn.SetPixel(65, 59, $BRASS_HI); $horn.SetPixel(65, 60, $BRASS_MD); $horn.SetPixel(65, 61, $BRASS_DK)
$horn.SetPixel(66, 58, $BRASS_HI); $horn.SetPixel(66, 59, $White); $horn.SetPixel(66, 60, $BRASS_MD); $horn.SetPixel(66, 61, $BRASS_DK)

# Center main chime horn (large):
$horn.SetPixel(61, 62, $BRASS_DK); $horn.SetPixel(62, 62, $BRASS_MD); $horn.SetPixel(63, 62, $BRASS_HI); $horn.SetPixel(64, 62, $BRASS_HI)
$horn.SetPixel(65, 63, $BRASS_MD); $horn.SetPixel(66, 63, $BRASS_HI)
$horn.SetPixel(67, 62, $BRASS_HI); $horn.SetPixel(67, 63, $BRASS_MD); $horn.SetPixel(67, 64, $BRASS_DK)
$horn.SetPixel(68, 61, $BRASS_HI); $horn.SetPixel(68, 62, $White); $horn.SetPixel(68, 63, $BRASS_MD); $horn.SetPixel(68, 64, $BRASS_DK)

# Bottom chime horn:
$horn.SetPixel(58, 65, $BRASS_DK); $horn.SetPixel(59, 65, $BRASS_MD); $horn.SetPixel(60, 65, $BRASS_HI)
$horn.SetPixel(61, 66, $BRASS_MD); $horn.SetPixel(62, 66, $BRASS_HI)
$horn.SetPixel(63, 65, $BRASS_HI); $horn.SetPixel(63, 66, $BRASS_MD); $horn.SetPixel(63, 67, $BRASS_DK)
$horn.SetPixel(64, 64, $BRASS_HI); $horn.SetPixel(64, 65, $White); $horn.SetPixel(64, 66, $BRASS_MD); $horn.SetPixel(64, 67, $BRASS_DK)

# Prominent acoustic sound shockwaves radiating along the track:
# Arc 1:
$C_SONIC1 = [System.Drawing.Color]::FromArgb(255, 250, 204, 21)
$horn.SetPixel(71, 57, $C_SONIC1)
$horn.SetPixel(72, 58, $C_SONIC1)
$horn.SetPixel(72, 59, $White)
$horn.SetPixel(73, 60, $White)
$horn.SetPixel(73, 61, $C_SONIC1)
$horn.SetPixel(72, 62, $C_SONIC1)
$horn.SetPixel(71, 63, $C_SONIC1)
$horn.SetPixel(70, 64, $C_SONIC1)
$horn.SetPixel(69, 65, $C_SONIC1)

# Arc 2 (larger):
$C_SONIC2 = [System.Drawing.Color]::FromArgb(255, 254, 240, 138)
$horn.SetPixel(75, 55, $C_SONIC2)
$horn.SetPixel(76, 56, $C_SONIC2)
$horn.SetPixel(77, 57, $White)
$horn.SetPixel(78, 59, $White)
$horn.SetPixel(78, 60, $White)
$horn.SetPixel(77, 62, $C_SONIC2)
$horn.SetPixel(76, 63, $C_SONIC2)
$horn.SetPixel(75, 65, $C_SONIC2)
$horn.SetPixel(74, 66, $C_SONIC2)
$horn.SetPixel(73, 67, $C_SONIC2)

# Arc 3 (fainter outer):
$C_SONIC3 = [System.Drawing.Color]::FromArgb(190, 254, 240, 138)
$horn.SetPixel(80, 53, $C_SONIC3)
$horn.SetPixel(81, 55, $C_SONIC3)
$horn.SetPixel(82, 57, $C_SONIC3)
$horn.SetPixel(83, 59, $White)
$horn.SetPixel(82, 61, $C_SONIC3)
$horn.SetPixel(81, 63, $C_SONIC3)
$horn.SetPixel(80, 65, $C_SONIC3)

$horn.Save("src/main/resources/assets/miningtools/textures/item/horn_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$horn.Dispose()
Write-Output "horn_rail.png saved"

$base.Dispose()
