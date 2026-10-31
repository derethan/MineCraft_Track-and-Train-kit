Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

# In 1024x1024:
# For log1 at (u0=0, v0=40):
# Old tree ring 1 (left): X = 0..40, Y = 360..400
# Old tree ring 2 (right): X = 232..272, Y = 360..400
# Old top bark: X = 40..232, Y = 320..360
# Old bottom bark: X = 232..424, Y = 320..360
# Old front bark: X = 40..232, Y = 360..400
# Old back bark: X = 272..464, Y = 360..400

Write-Output "Tree ring 1 size in pixels: 40x40 (5x5 units in 128-space)"
Write-Output "Tree ring 1 center pixel (20, 380):"
$c = $bmp.GetPixel(20, 380)
Write-Output "R=$($c.R), G=$($c.G), B=$($c.B), A=$($c.A)"

Write-Output "Bark sample pixel (100, 340):"
$c = $bmp.GetPixel(100, 340)
Write-Output "R=$($c.R), G=$($c.G), B=$($c.B), A=$($c.A)"

$bmp.Dispose()
