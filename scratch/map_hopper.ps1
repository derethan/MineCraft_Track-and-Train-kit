Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png")
Write-Host "Hopper coal: $($bmp.Width)x$($bmp.Height)"

# Check UV regions in 128x128 space (scale = 8)
# Underframe draft beam: texOffs(0, 0), addBox(-10, 3, -3, 20, 2, 6)
# Couplers: texOffs(0, 10), addBox(-14, 2, -2, 2, 2, 4)
# Wheels / Trucks:
# In old model: texOffs(54, 0), (68, 0), (82, 0), (96, 0)
# In new model: TruckModelHelper at texOffs(54, 20), (80, 20), (104, 20)

# Check the old wheel area: U=54..110, V=0..16 -> X=432..880, Y=0..128
for ($gy = 0; $gy -lt 16; $gy++) {
    $row = ""
    for ($gx = 0; $gx -lt 16; $gx++) {
        $px = $gx * 64 + 32
        $py = $gy * 64 + 32
        $p = $bmp.GetPixel($px, $py)
        # Check if rust: R > 90, G < 80, B < 65
        if ($p.R -gt 90 -and $p.G -lt 80 -and $p.B -lt 65) {
            $row += "R" # Rust
        } elseif ($p.R -lt 40 -and $p.G -lt 40 -and $p.B -lt 40) {
            $row += "B" # Black/Dark
        } elseif ($p.R -gt 150 -and $p.G -gt 150 -and $p.B -gt 150) {
            $row += "S" # Silver
        } else {
            $row += "."
        }
    }
    Write-Host "  [$([string]::Format('{0:d2}', $gy))] $row"
}

$bmp.Dispose()
