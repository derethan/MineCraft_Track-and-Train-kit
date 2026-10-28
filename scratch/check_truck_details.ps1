Add-Type -AssemblyName System.Drawing

function Check-TruckDetails($name) {
    $bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\$name")
    Write-Host "=== $name ==="
    # Wheel at texOffs(80, 20) -> (640, 160) to (704, 192)
    $minR = 255; $maxR = 0
    for ($y = 160; $y -lt 192; $y++) {
        for ($x = 640; $x -lt 704; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.A -gt 10) {
                if ($p.R -lt $minR) { $minR = $p.R }
                if ($p.R -gt $maxR) { $maxR = $p.R }
            }
        }
    }
    Write-Host "  Wheel R range: $minR to $maxR"
    $bmp.Dispose()
}

Check-TruckDetails "wood_cart.png"
Check-TruckDetails "generator_cart.png"
Check-TruckDetails "caboose.png"
