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
$res = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($res)
$g.Clear([System.Drawing.Color]::Transparent)

# Track A at offset (16, 37)
# Track B is Track A flipped horizontally, also centered at (16, 37) (since 128 - 97 - 16 = 15, let's say 15 or 16)
$offXA = 16
$offYA = 37
$offXB = 15
$offYB = 37

# First, draw ties for both tracks!
# For Track A ties
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                $res.SetPixel($x + $offXA, $y + $offYA, $c)
            }
        }
    }
}

# For Track B ties (flipped horizontally)
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if (-not ($isSteel -or $isOutline)) {
                # Merge tie if not already set or blend
                $curr = $res.GetPixel($x + $offXB, $y + $offYB)
                if ($curr.A -eq 0) {
                    $res.SetPixel($x + $offXB, $y + $offYB, $c)
                } else {
                    # Darken slightly in overlap region for shadow/joint
                    $res.SetPixel($x + $offXB, $y + $offYB, $c)
                }
            }
        }
    }
}

# Now draw both sets of rails!
# Where rails cross, we need frog flangeways
# Let's collect rail pixels of Track A and Track B
$railA = @{}
$railB = @{}

for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $railA["$($x + $offXA),$($y + $offYA)"] = $c
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
            if ($isSteel -or $isOutline) {
                $railB["$($x + $offXB),$($y + $offYB)"] = $c
            }
        }
    }
}

# First draw rail B
foreach ($key in $railB.Keys) {
    $parts = $key.Split(',')
    $px = [int]$parts[0]
    $py = [int]$parts[1]
    $res.SetPixel($px, $py, $railB[$key])
}

# Now draw rail A
foreach ($key in $railA.Keys) {
    $parts = $key.Split(',')
    $px = [int]$parts[0]
    $py = [int]$parts[1]
    
    # If it's an intersection point, blend as frog
    if ($railB.ContainsKey($key)) {
        # Intersection frog: use top highlight or outline
        $res.SetPixel($px, $py, $railA[$key])
    } else {
        $res.SetPixel($px, $py, $railA[$key])
    }
}

# Add corner guard rails inside the diamond!
# Diamond center is around x=64, y=64.
# The 4 frog points are around:
# Left frog: ~ (48, 64)
# Right frog: ~ (79, 64)
# Top frog: ~ (64, 56)
# Bottom frog: ~ (64, 72)
# Inside the diamond, guard rails guide the wheels through the frogs.
# Let's draw diamond center steel frog plates / check rails
$guardPairs = @(
    # Top-left inner check rail
    @(58, 62), @(59, 62), @(60, 61), @(61, 61),
    # Top-right inner check rail
    @(66, 61), @(67, 61), @(68, 62), @(69, 62),
    # Bottom-left inner check rail
    @(58, 66), @(59, 66), @(60, 67), @(61, 67),
    # Bottom-right inner check rail
    @(66, 67), @(67, 67), @(68, 66), @(69, 66)
)
foreach ($pt in $guardPairs) {
    $res.SetPixel($pt[0], $pt[1], $C_BODY)
    $res.SetPixel($pt[0], $pt[1] + 1, $C_OUTLINE)
}

$res.Save("scratch_junction_test.png", [System.Drawing.Imaging.ImageFormat]::Png)
$res.Dispose()
$base.Dispose()
Write-Output "Junction saved"
