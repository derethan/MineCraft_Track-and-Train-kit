Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\container_cart_green.png")

for ($y = 416; $y -lt 495; $y += 4) {
    $line = ""
    for ($x = 20; $x -lt 500; $x += 6) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.R -gt 150 -and $p.G -gt 150 -and $p.B -gt 150) {
            $line += "#"
        } else {
            $line += "."
        }
    }
    if ($line -match "#") {
        Write-Host "Y=$y : $line"
    }
}

$bmp.Dispose()
