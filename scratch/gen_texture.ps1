Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))

function Fill-Rect($x, $y, $w, $h, $c) {
    $brush = New-Object System.Drawing.SolidBrush $c
    $g.FillRectangle($brush, [int]$x, [int]$y, [int]$w, [int]$h)
    $brush.Dispose()
}

# Base dark steel background for texture regions
Fill-Rect 0 0 128 34 ([System.Drawing.Color]::FromArgb(255, 45, 50, 56))
Fill-Rect 0 34 128 26 ([System.Drawing.Color]::FromArgb(255, 55, 60, 68))
Fill-Rect 0 60 128 23 ([System.Drawing.Color]::FromArgb(255, 40, 44, 50))
Fill-Rect 0 83 128 24 ([System.Drawing.Color]::FromArgb(255, 50, 55, 62))
Fill-Rect 0 107 128 21 ([System.Drawing.Color]::FromArgb(255, 35, 38, 44))

# Underframe deck detailing (0..24)
Fill-Rect 0 0 64 16 ([System.Drawing.Color]::FromArgb(255, 38, 42, 48))
Fill-Rect 16 2 32 12 ([System.Drawing.Color]::FromArgb(255, 30, 33, 38))

# Wheels (72..120, 20..28)
Fill-Rect 72 20 48 8 ([System.Drawing.Color]::FromArgb(255, 80, 85, 92))
Fill-Rect 74 22 4 4 ([System.Drawing.Color]::FromArgb(255, 120, 125, 135))
Fill-Rect 86 22 4 4 ([System.Drawing.Color]::FromArgb(255, 120, 125, 135))
Fill-Rect 98 22 4 4 ([System.Drawing.Color]::FromArgb(255, 120, 125, 135))
Fill-Rect 110 22 4 4 ([System.Drawing.Color]::FromArgb(255, 120, 125, 135))

# Firebox & door (0..48, 34..60)
Fill-Rect 0 34 48 26 ([System.Drawing.Color]::FromArgb(255, 48, 52, 58))
Fill-Rect 48 34 20 18 ([System.Drawing.Color]::FromArgb(255, 35, 38, 42))
# Glowing orange firebox stokehole grate
Fill-Rect 52 38 12 10 ([System.Drawing.Color]::FromArgb(255, 230, 90, 20))
Fill-Rect 54 40 8 6 ([System.Drawing.Color]::FromArgb(255, 255, 180, 40))

# Boiler Drum & Brass Bands (0..74, 60..83)
Fill-Rect 0 60 42 23 ([System.Drawing.Color]::FromArgb(255, 42, 46, 52))
# Brass bands (42..74, 60..83)
Fill-Rect 42 60 32 23 ([System.Drawing.Color]::FromArgb(255, 205, 150, 45))
Fill-Rect 44 62 28 19 ([System.Drawing.Color]::FromArgb(255, 235, 180, 65))

# Chimney / smokestack (74..92, 60..75)
Fill-Rect 74 60 18 15 ([System.Drawing.Color]::FromArgb(255, 32, 35, 40))
Fill-Rect 92 60 16 15 ([System.Drawing.Color]::FromArgb(255, 205, 150, 45))

# Steam dome & Water gauge (74..105, 75..83)
Fill-Rect 74 75 18 8 ([System.Drawing.Color]::FromArgb(255, 205, 150, 45))
# Sight glass (blue water level)
Fill-Rect 92 75 12 8 ([System.Drawing.Color]::FromArgb(255, 40, 150, 225))
Fill-Rect 94 77 8 4 ([System.Drawing.Color]::FromArgb(255, 200, 240, 255))

# Dynamo Generator Housing & Cooling Fins (0..80, 83..107)
Fill-Rect 0 83 40 24 ([System.Drawing.Color]::FromArgb(255, 45, 50, 58))
# Copper cooling fins
Fill-Rect 40 83 40 24 ([System.Drawing.Color]::FromArgb(255, 185, 95, 50))
for ($i = 40; $i -lt 80; $i += 3) {
    Fill-Rect $i 83 1 24 ([System.Drawing.Color]::FromArgb(255, 220, 125, 70))
}
# Commutator & steam pipe (80..115, 83..107)
Fill-Rect 80 83 18 24 ([System.Drawing.Color]::FromArgb(255, 195, 100, 50))
Fill-Rect 98 83 16 24 ([System.Drawing.Color]::FromArgb(255, 175, 85, 45))

# Electrical Terminal Box & Insulators (0..80, 107..128)
Fill-Rect 0 107 26 21 ([System.Drawing.Color]::FromArgb(255, 38, 42, 48))
# Hazard caution stripes on terminal box
for ($x = 0; $x -lt 26; $x += 4) {
    Fill-Rect $x 107 2 6 ([System.Drawing.Color]::FromArgb(255, 240, 185, 30))
}
# Ceramic white insulators (26..46, 107..128)
Fill-Rect 26 107 20 21 ([System.Drawing.Color]::FromArgb(255, 235, 238, 242))
Fill-Rect 28 109 16 17 ([System.Drawing.Color]::FromArgb(255, 215, 220, 225))
# High voltage copper terminals (46..80, 107..128)
Fill-Rect 46 107 34 21 ([System.Drawing.Color]::FromArgb(255, 205, 105, 55))

# Save
$outPath = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\resources\assets\miningtools\textures\entity\generator_cart.png'
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Generator Cart Texture Generated at: $outPath"
