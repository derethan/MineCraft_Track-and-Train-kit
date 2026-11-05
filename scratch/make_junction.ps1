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

$canvas = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::Transparent)

# Both tracks centered at (16, 37)
$ox = 16
$oy = 37

# 1. Fill diamond center foundation ties
for ($dx = -12; $dx -le 12; $dx++) {
    for ($dy = -6; $dy -le 6; $dy++) {
        # Check if inside isometric diamond
        if (([Math]::Abs($dx) / 14.0 + [Math]::Abs($dy) / 7.0) -le 1.0) {
            $canvas.SetPixel(64 + $dx, 64 + $dy, $WOOD_LIGHT)
        }
    }
}

# 2. Draw Track A ties and Track B ties
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

for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
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

# 3. Draw Track B rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
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

# 4. Draw Track A rails, but with clean diamond frogs!
# Track A rails
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $targetX = $x + $ox
                $targetY = $y + $oy
                
                # Check if this pixel is in one of the 4 diamond frog flangeway gaps
                # Top frog: around (64, 52)
                # Bottom frog: around (64, 75)
                # Left frog: around (45, 64)
                # Right frog: around (83, 64)
                $isTopGap    = ($targetX -ge 63 -and $targetX -le 65 -and $targetY -ge 50 -and $targetY -le 52)
                $isBottomGap = ($targetX -ge 63 -and $targetX -le 65 -and $targetY -ge 74 -and $targetY -le 76)
                $isLeftGap   = ($targetX -ge 44 -and $targetX -le 46 -and $targetY -ge 63 -and $targetY -le 65)
                $isRightGap  = ($targetX -ge 82 -and $targetX -le 84 -and $targetY -ge 63 -and $targetY -le 65)
                
                if ($isTopGap -or $isBottomGap -or $isLeftGap -or $isRightGap) {
                    # Flangeway groove: darker outline color
                    $canvas.SetPixel($targetX, $targetY, $C_OUTLINE)
                } else {
                    $canvas.SetPixel($targetX, $targetY, $c)
                }
            }
        }
    }
}

# 5. Add inner check/guard rails for all 4 frogs
$guardRails = @(
    # Top frog inner guard
    @(62, 54), @(63, 54), @(64, 54), @(65, 54), @(66, 54),
    # Bottom frog inner guard
    @(62, 72), @(63, 72), @(64, 72), @(65, 72), @(66, 72),
    # Left frog inner guard
    @(47, 62), @(47, 63), @(47, 64), @(47, 65), @(47, 66),
    # Right frog inner guard
    @(81, 62), @(81, 63), @(81, 64), @(81, 65), @(81, 66)
)
foreach ($pt in $guardRails) {
    $canvas.SetPixel($pt[0], $pt[1], $C_BODY)
    $canvas.SetPixel($pt[0], $pt[1] + 1, $C_OUTLINE)
}

$destPath = "src/main/resources/assets/miningtools/textures/item/junction_rail.png"
$canvas.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
$base.Dispose()
Write-Output "Junction Rail created at $destPath"
