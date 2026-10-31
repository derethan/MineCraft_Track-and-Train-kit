Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

# Print details of log1 UV in 128-space:
# u=0, v=40, size was 24 x 5 x 5:
# dz = 5, dx = 24, dy = 5
# Top face: u = u0 + dz = 5..29, v = v0 = 40..45
# Bottom face: u = 29..53, v = 40..45
# Left face (end): u = 0..5, v = 45..50
# Front face (side): u = 5..29, v = 45..50
# Right face (end): u = 29..34, v = 45..50
# Back face (side): u = 34..58, v = 45..50

Write-Output "Checking original 24x5x5 UV mapping for log1:"
Write-Output ("Top face (u=5..29, v=40..45): " + ($bmp.GetPixel(5*8, 40*8).A) + ", " + ($bmp.GetPixel(28*8+7, 44*8+7).A))
Write-Output ("Left end (u=0..5, v=45..50): " + ($bmp.GetPixel(0*8, 45*8).A) + ", " + ($bmp.GetPixel(4*8+7, 49*8+7).A))
Write-Output ("Right end (u=29..34, v=45..50): " + ($bmp.GetPixel(29*8, 45*8).A) + ", " + ($bmp.GetPixel(33*8+7, 49*8+7).A))

# Now check where 24x4x4 maps:
# dz = 4, dx = 24, dy = 4
# Top face: u = u0 + dz = 4..28, v = v0 = 40..44 -> Notice u=4 has alpha:
Write-Output "Checking 24x4x4 mapping for log1:"
Write-Output ("u=4, v=40 (Top face column 0 in 24x4x4): alpha = " + ($bmp.GetPixel(4*8+4, 40*8+4).A))
Write-Output ("u=3, v=40: alpha = " + ($bmp.GetPixel(3*8+4, 40*8+4).A))

$bmp.Dispose()
