Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

Write-Output ("Bitmap size: " + $bmp.Width + "x" + $bmp.Height)
# Scale factor from 128x128 texture space to 1024x1024
# 1024 / 128 = 8 pixels per texture unit.

# Check log1: texOffs(0, 40)
# In 1024x1024, (u=0..64, v=40..54) -> (x=0..512, y=320..432)
# Check for transparency in that region:
$transparentPixels = 0
for ($y = 320; $y -lt 432; $y++) {
    for ($x = 0; $x -lt 512; $x++) {
        if ($bmp.GetPixel($x, $y).A -lt 250) {
            $transparentPixels++
        }
    }
}
Write-Output "Transparent pixels in log1 UV region (0..64, 40..54): $transparentPixels"

# Check log2: texOffs(64, 40)
$transparentPixels2 = 0
for ($y = 320; $y -lt 432; $y++) {
    for ($x = 512; $x -lt 1024; $x++) {
        if ($bmp.GetPixel($x, $y).A -lt 250) {
            $transparentPixels2++
        }
    }
}
Write-Output "Transparent pixels in log2 UV region (64..128, 40..54): $transparentPixels2"

$bmp.Dispose()
