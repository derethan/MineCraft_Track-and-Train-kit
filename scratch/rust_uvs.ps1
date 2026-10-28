Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png")

# Let's check where the rust pixels are located exactly in pixel coordinates (X, Y)
# and UV coordinates (u, v = x/8, y/8)
Write-Host "Rust bounding boxes in hopper_cart_coal.png:"
$rustBoxes = @()

for ($v = 0; $v -lt 128; $v++) {
    for ($u = 0; $u -lt 128; $u++) {
        $p = $bmp.GetPixel($u * 8 + 4, $v * 8 + 4)
        if ($p.R -gt 90 -and $p.G -lt 85 -and $p.B -lt 65) {
            Write-Host "  Rust at UV ($u, $v) -> Pixel ($($u*8), $($v*8)): RGB=($($p.R), $($p.G), $($p.B))"
        }
    }
}

$bmp.Dispose()
