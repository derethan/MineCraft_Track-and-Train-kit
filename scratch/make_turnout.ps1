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

# 128x128 canvas
$canvas = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::Transparent)

$ox = 16
$oy = 37

# 1. Draw branch ties (from flipped track SW branch)
# The SW branch is x in 0..48 of flipped track (x=49..96 of base track)
for ($x = 0; $x -lt 48; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $canvas.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}

# 2. Draw mainline ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $canvas.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}

# 3. Connect the junction ties between mainline and branch (Tie 3 and Tie 4 area)
# Fill in wood between x=46..54, y=66..72 to make a solid switch tie bed
for ($tx = 44; $tx -le 54; $tx++) {
    for ($ty = 65; $ty -le 71; $ty++) {
        if ($canvas.GetPixel($tx, $ty).A -eq 0) {
            $canvas.SetPixel($tx, $ty, $WOOD_LIGHT)
        }
    }
}

# 4. Draw branch rails (SW branch from flipped track)
# We only want the branch part up to where it meets the straight rails
for ($x = 0; $x -le 46; $x++) {
    for ($y = 20; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $canvas.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}

# 5. Draw Mainline rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $canvas.SetPixel($x + $ox, $y + $oy, $c)
            }
        }
    }
}

# 6. Smooth the curve and add switch blades and frog:
# At the frog (around x=45, y=64 and x=62, y=60):
# Add frog point V and flangeway groove:
$canvas.SetPixel(46, 63, $C_OUTLINE)
$canvas.SetPixel(46, 64, $C_OUTLINE)
$canvas.SetPixel(47, 63, $C_BODY)

# Curved switch blade from mainline top rail curving towards branch:
# Mainline top rail at x=34 is y=8+37 = 45
# Curve connecting (34, 46) to (44, 53)
$bladePts = @(
    @(36, 46), @(37, 47), @(38, 48), @(39, 49), @(40, 50),
    @(41, 51), @(42, 52), @(43, 53), @(44, 54), @(45, 55)
)
foreach ($pt in $bladePts) {
    $canvas.SetPixel($pt[0], $pt[1] - 1, $C_GLINT)
    $canvas.SetPixel($pt[0], $pt[1], $C_BODY)
    $canvas.SetPixel($pt[0], $pt[1] + 1, $C_OUTLINE)
}

# Add switch throw-bar between rails at Tie 2 (around x=38, y=51):
for ($ty = 47; $ty -le 55; $ty++) {
    $canvas.SetPixel(38, $ty, $C_OUTLINE)
}

# 7. Add Switch Stand (classic railroad ground switch stand with green arrow/disk):
# Place on extended tie side (around x=24, y=41):
# Base
$canvas.SetPixel(23, 42, $C_OUTLINE)
$canvas.SetPixel(24, 42, $C_OUTLINE)
$canvas.SetPixel(25, 42, $C_OUTLINE)
$canvas.SetPixel(24, 41, [System.Drawing.Color]::FromArgb(255, 60, 60, 65))

# Vertical shaft
for ($sy = 30; $sy -le 40; $sy++) {
    $canvas.SetPixel(24, $sy, [System.Drawing.Color]::FromArgb(255, 40, 42, 48))
}

# Green right-turn target at top (shows green arrow / disk)
$GREEN_HI = [System.Drawing.Color]::FromArgb(255, 74, 222, 128)
$GREEN_MD = [System.Drawing.Color]::FromArgb(255, 22, 163, 74)
$GREEN_DK = [System.Drawing.Color]::FromArgb(255, 20, 83, 45)

# Target disk (circle / diamond)
$canvas.SetPixel(24, 26, $GREEN_DK)
$canvas.SetPixel(23, 27, $GREEN_MD); $canvas.SetPixel(24, 27, $GREEN_HI); $canvas.SetPixel(25, 27, $GREEN_MD)
$canvas.SetPixel(22, 28, $GREEN_MD); $canvas.SetPixel(23, 28, $GREEN_HI); $canvas.SetPixel(24, 28, $GREEN_HI); $canvas.SetPixel(25, 28, $GREEN_HI); $canvas.SetPixel(26, 28, $GREEN_MD)
$canvas.SetPixel(23, 29, $GREEN_MD); $canvas.SetPixel(24, 29, $GREEN_HI); $canvas.SetPixel(25, 29, $GREEN_MD)
$canvas.SetPixel(24, 30, $GREEN_DK)

# Target arrow shaft pointing right
$canvas.SetPixel(26, 28, [System.Drawing.Color]::White)
$canvas.SetPixel(27, 28, [System.Drawing.Color]::White)
$canvas.SetPixel(28, 28, [System.Drawing.Color]::White)
$canvas.SetPixel(27, 27, [System.Drawing.Color]::White)
$canvas.SetPixel(27, 29, [System.Drawing.Color]::White)

$destPath = "src/main/resources/assets/miningtools/textures/item/turnout_rail.png"
$canvas.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
$base.Dispose()
Write-Output "Turnout Rail created at $destPath"
