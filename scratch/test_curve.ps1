Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
$w = $base.Width
$h = $base.Height

# Canvas 128x128
$bmp = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Transparent)

# Draw base straight track at (16, 37)
for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 50) {
            $bmp.SetPixel($x + 16, $y + 37, $c)
        }
    }
}

# The flipped track is in the SW-to-NE diagonal (-0.5 slope)
# Let's take the bottom-left half of the flipped track (which heads SW)
# Flipped track at (16, 37):
# Center is x=64, y=64.
# A right turnout from NW branches off towards SW (bottom-left)!
# Let's see: from Tie 3 (center), track branches towards SW!
# In the flipped track, the branch towards SW is from x=16..64, y=64..92!
for ($x = 0; $x -lt 48; $x++) {
    for ($y = 25; $y -lt $h; $y++) {
        $c = $base.GetPixel($w - 1 - $x, $y)
        if ($c.A -gt 50) {
            # Let's blend/draw it into canvas at (x + 16, y + 37)
            $curr = $bmp.GetPixel($x + 16, $y + 37)
            if ($curr.A -lt 50) {
                $bmp.SetPixel($x + 16, $y + 37, $c)
            }
        }
    }
}

$bmp.Save("scratch_curve_idea.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
$base.Dispose()
Write-Output "Curve idea saved"
