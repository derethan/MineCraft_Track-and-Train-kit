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

$ox = 16
$oy = 37

function CreateBaseCanvas() {
    $canvas = New-Object System.Drawing.Bitmap 128, 128
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.Clear([System.Drawing.Color]::Transparent)
    
    # Copy base straight track
    for ($x = 0; $x -lt $w; $x++) {
        for ($y = 0; $y -lt $h; $y++) {
            $c = $base.GetPixel($x, $y)
            if ($c.A -gt 50) {
                $canvas.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
    return $canvas
}

# -------------------------------------------------------------
# 1. TURNOUT RAIL (RIGHT)
# -------------------------------------------------------------
$turnout = New-Object System.Drawing.Bitmap 128, 128
# Draw branch ties (SW)
for ($x = 0; $x -lt 48; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $turnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $turnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Extend Tie 1 to the left for Switch Stand mounting:
# Tie 1 top surface is at canvas x=25..31, y=48..52
for ($ex = 19; $ex -le 27; $ex++) {
    $ey = 46 + [int](($ex - 19) * 0.5)
    $turnout.SetPixel($ex, $ey, $WOOD_LIGHT)
    $turnout.SetPixel($ex, $ey + 1, $WOOD_MED)
    $turnout.SetPixel($ex, $ey + 2, $WOOD_DARK)
}
# Draw branch rails (SW branch)
for ($x = 0; $x -le 46; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $turnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw Mainline rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $turnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Switch Stand grounded on extended tie at x=20, y=46
# Iron base plate
$turnout.SetPixel(19, 46, $C_OUTLINE); $turnout.SetPixel(20, 46, $C_BODY); $turnout.SetPixel(21, 46, $C_OUTLINE)
$turnout.SetPixel(19, 47, $C_OUTLINE); $turnout.SetPixel(20, 47, $C_SHADOW); $turnout.SetPixel(21, 47, $C_OUTLINE)
# Vertical mast
for ($sy = 34; $sy -le 45; $sy++) {
    $turnout.SetPixel(20, $sy, [System.Drawing.Color]::FromArgb(255, 30, 32, 38))
}
# Green switch target (lantern + arrow banner)
for ($tx = 18; $tx -le 22; $tx++) {
    for ($ty = 30; $ty -le 34; $ty++) {
        $turnout.SetPixel($tx, $ty, $GREEN_MD)
    }
}
$turnout.SetPixel(20, 31, $GREEN_HI); $turnout.SetPixel(20, 32, $GREEN_HI)
# Arrow indicator pointing SW
$turnout.SetPixel(18, 33, [System.Drawing.Color]::White)
$turnout.SetPixel(17, 34, [System.Drawing.Color]::White)
$turnout.SetPixel(19, 34, [System.Drawing.Color]::White)

# Switch throw rod between rails at Tie 1/2
for ($ry = 50; $ry -le 59; $ry++) {
    $turnout.SetPixel(36, $ry, $C_OUTLINE)
}

$turnout.Save("src/main/resources/assets/miningtools/textures/item/turnout_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$turnout.Dispose()
Write-Output "turnout_rail.png saved"


# -------------------------------------------------------------
# 2. LEFT TURNOUT RAIL
# -------------------------------------------------------------
$leftTurnout = New-Object System.Drawing.Bitmap 128, 128
# Draw branch ties (NE branch: x in 48..96 of flipped track)
for ($x = 48; $x -lt $w; $x++) {
    for ($y = 0; $y -le 35; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Extend Tie 4 to the right for Left Switch Stand
for ($ex = 98; $ex -le 106; $ex++) {
    $ey = 72 - [int](($ex - 98) * 0.5)
    $leftTurnout.SetPixel($ex, $ey, $WOOD_LIGHT)
    $leftTurnout.SetPixel($ex, $ey + 1, $WOOD_MED)
    $leftTurnout.SetPixel($ex, $ey + 2, $WOOD_DARK)
}
# Draw branch rails (NE branch)
for ($x = 48; $x -lt $w; $x++) {
    for ($y = 0; $y -le 35; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw Mainline rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $leftTurnout.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Switch Stand grounded on extended tie at x=105, y=69
# Base plate
$leftTurnout.SetPixel(104, 69, $C_OUTLINE); $leftTurnout.SetPixel(105, 69, $C_BODY); $leftTurnout.SetPixel(106, 69, $C_OUTLINE)
$leftTurnout.SetPixel(104, 70, $C_OUTLINE); $leftTurnout.SetPixel(105, 70, $C_SHADOW); $leftTurnout.SetPixel(106, 70, $C_OUTLINE)
# Vertical mast
for ($sy = 57; $sy -le 68; $sy++) {
    $leftTurnout.SetPixel(105, $sy, [System.Drawing.Color]::FromArgb(255, 30, 32, 38))
}
# Green switch target (banner)
for ($tx = 103; $tx -le 107; $tx++) {
    for ($ty = 53; $ty -le 57; $ty++) {
        $leftTurnout.SetPixel($tx, $ty, $GREEN_MD)
    }
}
$leftTurnout.SetPixel(105, 54, $GREEN_HI); $leftTurnout.SetPixel(105, 55, $GREEN_HI)
# Arrow indicator pointing NE
$leftTurnout.SetPixel(107, 54, [System.Drawing.Color]::White)
$leftTurnout.SetPixel(106, 53, [System.Drawing.Color]::White)
$leftTurnout.SetPixel(108, 55, [System.Drawing.Color]::White)

# Switch throw rod between rails at Tie 4
for ($ry = 70; $ry -le 79; $ry++) {
    $leftTurnout.SetPixel(80, $ry, $C_OUTLINE)
}

$leftTurnout.Save("src/main/resources/assets/miningtools/textures/item/left_turnout_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$leftTurnout.Dispose()
Write-Output "left_turnout_rail.png saved"


# -------------------------------------------------------------
# 3. WYE RAIL (3-Way Y Switch)
# -------------------------------------------------------------
$wye = New-Object System.Drawing.Bitmap 128, 128
# Draw ties for SW branch, NE branch, and mainline
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $wye.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $wye.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw SW branch rails and NE branch rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $wye.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}
# Draw mainline rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                # Leave gaps at frogs
                $tx = $x + $ox; $ty = $y + $oy
                $isTopFrog = ($tx -ge 63 -and $tx -le 65 -and $ty -ge 50 -and $ty -le 52)
                $isBotFrog = ($tx -ge 63 -and $tx -le 65 -and $ty -ge 74 -and $ty -le 76)
                if ($isTopFrog -or $isBotFrog) {
                    $wye.SetPixel($tx, $ty, $C_OUTLINE)
                } else {
                    $wye.SetPixel($tx, $ty, $c)
                }
            }
        }
    }
}
# Wye Center Indicator: Octagonal Redstone Directional Lantern on Tie 3 center (x=64, y=64)
# Amber/Red directional prism indicating 3-way split
for ($lx = -3; $lx -le 3; $lx++) {
    for ($ly = -3; $ly -le 3; $ly++) {
        if ([Math]::Abs($lx) + [Math]::Abs($ly) -le 4) {
            $wye.SetPixel(64 + $lx, 64 + $ly, $RED_DK)
        }
    }
}
for ($lx = -2; $lx -le 2; $lx++) {
    for ($ly = -2; $ly -le 2; $ly++) {
        if ([Math]::Abs($lx) + [Math]::Abs($ly) -le 3) {
            $wye.SetPixel(64 + $lx, 64 + $ly, $RED_MD)
        }
    }
}
$wye.SetPixel(64, 63, $RED_HI)
$wye.SetPixel(64, 64, [System.Drawing.Color]::White)
# 3 split markers (Y-indicator)
$wye.SetPixel(63, 65, [System.Drawing.Color]::White)
$wye.SetPixel(65, 65, [System.Drawing.Color]::White)

$wye.Save("src/main/resources/assets/miningtools/textures/item/wye_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$wye.Dispose()
Write-Output "wye_rail.png saved"


# -------------------------------------------------------------
# 4. HORN RAIL (UPGRADED BRASS CHIME HORNS & SOUND WAVES)
# -------------------------------------------------------------
$horn = CreateBaseCanvas
# Mounted on Tie 3 center (canvas x=61, y=64):
# Locomotive air horn compressor base (dark cast iron)
for ($hx = 58; $hx -le 64; $hx++) {
    for ($hy = 64; $hy -le 67; $hy++) {
        $horn.SetPixel($hx, $hy, [System.Drawing.Color]::FromArgb(255, 30, 32, 36))
    }
}
$horn.SetPixel(61, 65, [System.Drawing.Color]::FromArgb(255, 60, 65, 75))

# Twin Polished Brass Trumpet Horns facing forward/down-track (pointing towards SE):
# Horn 1 (upper trumpet):
# Pipe stem
$horn.SetPixel(61, 62, $BRASS_DK); $horn.SetPixel(62, 62, $BRASS_MD); $horn.SetPixel(63, 62, $BRASS_HI)
$horn.SetPixel(64, 63, $BRASS_MD); $horn.SetPixel(65, 63, $BRASS_HI)
# Flared bell mouth
$horn.SetPixel(66, 62, $BRASS_HI); $horn.SetPixel(66, 63, $BRASS_MD); $horn.SetPixel(66, 64, $BRASS_DK)
$horn.SetPixel(67, 61, $BRASS_HI); $horn.SetPixel(67, 62, [System.Drawing.Color]::White); $horn.SetPixel(67, 63, $BRASS_MD); $horn.SetPixel(67, 64, $BRASS_DK)

# Horn 2 (lower trumpet):
# Pipe stem
$horn.SetPixel(59, 66, $BRASS_DK); $horn.SetPixel(60, 66, $BRASS_MD); $horn.SetPixel(61, 66, $BRASS_HI)
$horn.SetPixel(62, 67, $BRASS_MD); $horn.SetPixel(63, 67, $BRASS_HI)
# Flared bell mouth
$horn.SetPixel(64, 66, $BRASS_HI); $horn.SetPixel(64, 67, $BRASS_MD); $horn.SetPixel(64, 68, $BRASS_DK)
$horn.SetPixel(65, 65, $BRASS_HI); $horn.SetPixel(65, 66, [System.Drawing.Color]::White); $horn.SetPixel(65, 67, $BRASS_MD); $horn.SetPixel(65, 68, $BRASS_DK)

# Acoustic sound wave arcs emitting from the horn bells (bright golden/white acoustic arcs):
# Wave 1 (inner arc):
$C_WAVE1 = [System.Drawing.Color]::FromArgb(240, 250, 204, 21)
$horn.SetPixel(70, 60, $C_WAVE1)
$horn.SetPixel(71, 61, $C_WAVE1)
$horn.SetPixel(71, 62, [System.Drawing.Color]::White)
$horn.SetPixel(71, 63, $C_WAVE1)
$horn.SetPixel(70, 64, $C_WAVE1)
$horn.SetPixel(69, 65, $C_WAVE1)
$horn.SetPixel(68, 66, $C_WAVE1)

# Wave 2 (outer arc):
$C_WAVE2 = [System.Drawing.Color]::FromArgb(200, 253, 224, 71)
$horn.SetPixel(74, 58, $C_WAVE2)
$horn.SetPixel(75, 59, $C_WAVE2)
$horn.SetPixel(76, 61, [System.Drawing.Color]::White)
$horn.SetPixel(76, 62, [System.Drawing.Color]::White)
$horn.SetPixel(75, 64, $C_WAVE2)
$horn.SetPixel(74, 66, $C_WAVE2)
$horn.SetPixel(73, 67, $C_WAVE2)

$horn.Save("src/main/resources/assets/miningtools/textures/item/horn_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$horn.Dispose()
Write-Output "horn_rail.png saved"

$base.Dispose()
