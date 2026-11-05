Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
$w = $base.Width
$h = $base.Height

# Palette colors
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

# Base straight rail at (16, 37)
$ox = 16
$oy = 37

# First copy the base rail into canvas
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 50) {
            $canvas.SetPixel($x + $ox, $y + $oy, $c)
        }
    }
}

# Now let's add extended switch ties for the right branch!
# Mainline ties are at ~ x=12, 27, 45, 62, 80 in base coordinates.
# In canvas coordinates:
# Tie 2: ~ x = 27 + 16 = 43
# Tie 3: ~ x = 45 + 16 = 61
# Tie 4: ~ x = 62 + 16 = 78
# Tie 5: ~ x = 80 + 16 = 96
# For a right turnout, the branch curves downward (increasing Y).
# Let's extend Tie 3, Tie 4, Tie 5 downward-left (in the tie direction: dx = -2, dy = 1):
# A tie has width ~ 4-5 px along the track, and runs along vector (-2, 1).
# Let's draw extended sleeper sections for Tie 3, Tie 4, Tie 5:

function DrawTieSection($cx, $cy, $len) {
    for ($i = 0; $i -lt $len; $i++) {
        $tx = $cx - ($i * 2)
        $ty = $cy + $i
        
        # Tie cross section (depth 3, width 4)
        for ($dx = 0; $dx -lt 4; $dx++) {
            # Top surface
            $canvas.SetPixel($tx + $dx, $ty, $WOOD_LIGHT)
            $canvas.SetPixel($tx + $dx, $ty - 1, $WOOD_HILITE)
            # Front face / thickness
            $canvas.SetPixel($tx + $dx, $ty + 1, $WOOD_MED)
            $canvas.SetPixel($tx + $dx, $ty + 2, $WOOD_DARK)
        }
    }
}

# Extended ties under the diverging branch:
# Tie 3 end is around (56, 75). Let's extend it by 4 steps:
DrawTieSection 56 75 5
# Tie 4 end is around (73, 83). Let's extend it by 7 steps:
DrawTieSection 73 83 7
# Tie 5 end is around (90, 92). Let's extend it by 9 steps:
DrawTieSection 90 92 8

# Redraw the straight rail on top of any overwritten pixels where needed
# (or we can redraw straight rail afterwards)
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

# Helper to draw a single 3D rail segment at (px, py)
function DrawRailSegment($px, $py) {
    # In the reference image, each rail segment has:
    # y-1: top highlight / glint ($C_LIGHT / $C_GLINT)
    # y: rail head ($C_BODY)
    # y+1: rail web / base ($C_SHADOW)
    # y+2: bottom outline ($C_OUTLINE)
    $canvas.SetPixel($px, $py - 1, $C_GLINT)
    $canvas.SetPixel($px, $py, $C_LIGHT)
    $canvas.SetPixel($px, $py + 1, $C_BODY)
    $canvas.SetPixel($px, $py + 2, $C_SHADOW)
    $canvas.SetPixel($px, $py + 3, $C_OUTLINE)
}

# Now draw the curved diverging rails!
# The branch curves to the right (downwards on the screen).
# Diverging Inner Rail (starts at mainline top rail ~ x=38, y=36, curves down through the middle):
# Points:
$curveInner = @(
    @(42, 38), @(44, 39), @(46, 41), @(48, 43), @(50, 45),
    @(52, 47), @(54, 50), @(56, 53), @(58, 56), @(60, 60),
    @(62, 64), @(64, 68), @(66, 73), @(68, 78), @(70, 83)
)

# Diverging Outer Rail (starts at mainline bottom rail ~ x=50, y=49, curves down-right):
$curveOuter = @(
    @(52, 50), @(54, 51), @(56, 53), @(58, 55), @(60, 58),
    @(62, 61), @(64, 65), @(66, 69), @(68, 73), @(70, 78),
    @(72, 83), @(74, 88), @(76, 94), @(78, 100)
)

# Notice: curveInner crosses the straight bottom rail around x=58..62, y=55..60!
# That crossing is the frog!
foreach ($pt in $curveInner) {
    DrawRailSegment $pt[0] $pt[1]
}
foreach ($pt in $curveOuter) {
    DrawRailSegment $pt[0] $pt[1]
}

# Add Switch Stand on Tie 1 extended side (around x=28, y=46):
# Switch stand: dark iron base, vertical lever rod, green target disk
$canvas.SetPixel(26, 44, $C_OUTLINE)
$canvas.SetPixel(27, 44, $C_OUTLINE)
$canvas.SetPixel(28, 44, $C_OUTLINE)

# Vertical mast
for ($sy = 35; $sy -le 43; $sy++) {
    $canvas.SetPixel(27, $sy, [System.Drawing.Color]::FromArgb(255, 40, 40, 40))
}

# Green switch target disk / banner at top of mast
$C_GREEN_HI = [System.Drawing.Color]::FromArgb(255, 50, 205, 50)
$C_GREEN_MD = [System.Drawing.Color]::FromArgb(255, 34, 139, 34)
for ($gx = 25; $gx -le 29; $gx++) {
    for ($gy = 32; $gy -le 35; $gy++) {
        $canvas.SetPixel($gx, $gy, $C_GREEN_MD)
    }
}
$canvas.SetPixel(27, 33, $C_GREEN_HI)
$canvas.SetPixel(27, 34, $C_GREEN_HI)

# Save test
$canvas.Save("scratch_turnout_test.png", [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
$base.Dispose()
Write-Output "Turnout test saved"
