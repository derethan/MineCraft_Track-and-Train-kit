Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/wood_cart.png')

$alphaCount = 0
for ($y = 320; $y -lt 640; $y++) {
    for ($x = 0; $x -lt 1024; $x++) {
        if ($bmp.GetPixel($x, $y).A -gt 10) {
            $alphaCount++
        }
    }
}
Write-Output "wood_cart.png pixels in log region (Y=320..640): $alphaCount"

$bmp.Dispose()
