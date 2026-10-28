Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png")
Write-Host "Hopper coal size: $($bmp.Width)x$($bmp.Height)"

# In 1024x1024, scale is 8x (if 128x128)
# Let's inspect where rust colors are:
# Search for pixels with R > 100, G < 80, B < 60 or similar rust tones
$rustPixels = 0
for ($y = 0; $y -lt $bmp.Height; $y += 16) {
    for ($x = 0; $x -lt $bmp.Width; $x += 16) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.R -gt 90 -and $p.R -gt ($p.B + 30) -and $p.G -lt 80) {
            Write-Host "Rust found at ($x, $y): R=$($p.R), G=$($p.G), B=$($p.B)"
            $rustPixels++
            if ($rustPixels -gt 10) { break }
        }
    }
    if ($rustPixels -gt 10) { break }
}

$bmp.Dispose()
