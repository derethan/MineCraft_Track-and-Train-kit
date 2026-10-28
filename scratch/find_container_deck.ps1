Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\container_cart_green.png")

for ($y = 0; $y -lt 300; $y += 8) {
    $minX = -1
    $maxX = -1
    for ($x = 0; $x -lt $bmp.Width; $x += 4) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.A -gt 10) {
            if ($minX -eq -1) { $minX = $x }
            $maxX = $x
        }
    }
    if ($minX -ne -1) {
        $col = $bmp.GetPixel($minX, $y)
        Write-Host "Y=$y : X=$minX..$maxX, sample=($($col.R),$($col.G),$($col.B),$($col.A))"
    } else {
        Write-Host "Y=$y : EMPTY"
    }
}

$bmp.Dispose()
