Add-Type -AssemblyName System.Drawing

function Find-Text($file, $minLum) {
    $bmp = New-Object System.Drawing.Bitmap($file)
    Write-Host "Finding high-contrast text in $file ($($bmp.Width)x$($bmp.Height)):"
    
    # Scan for near-white or light text pixels on dark background
    for ($y = 0; $y -lt $bmp.Height; $y += 8) {
        $minX = -1; $maxX = -1
        for ($x = 0; $x -lt $bmp.Width; $x += 4) {
            $p = $bmp.GetPixel($x, $y)
            $lum = 0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B
            if ($lum -gt $minLum -and $p.A -gt 200) {
                if ($minX -eq -1) { $minX = $x }
                $maxX = $x
            }
        }
        if ($minX -ne -1 -and ($maxX - $minX) -lt 800) {
            Write-Host "  Y=$y : X=$minX..$maxX"
        }
    }
    $bmp.Dispose()
}

Find-Text "src\main\resources\assets\miningtools\textures\entity\caboose.png" 200
Write-Host ""
Find-Text "src\main\resources\assets\miningtools\textures\entity\container_cart_green.png" 180
