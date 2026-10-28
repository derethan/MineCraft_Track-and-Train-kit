Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\wood_cart.png")

# Let's inspect rows 0 to 200 in wood_cart.png
# and see where the flatbed is
Write-Host "wood_cart non-empty regions in Y=0..200:"
for ($y = 0; $y -lt 200; $y += 16) {
    for ($x = 0; $x -lt $bmp.Width; $x += 32) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.A -gt 10) {
            Write-Host "  Pixel at ($x, $y): Alpha=$($p.A), RGB=($($p.R), $($p.G), $($p.B))"
        }
    }
}

$bmp.Dispose()
