Add-Type -AssemblyName System.Drawing

function Scan-Regions($path) {
    $bmp = New-Object System.Drawing.Bitmap($path)
    Write-Host "Scanning $path ($($bmp.Width)x$($bmp.Height)):"
    
    # 16x16 grid of blocks
    $gw = [int]($bmp.Width / 16)
    $gh = [int]($bmp.Height / 16)
    
    for ($gy = 0; $gy -lt 16; $gy++) {
        $row = ""
        for ($gx = 0; $gx -lt 16; $gx++) {
            $hasOpaque = $false
            $px = $gx * $gw + [int]($gw/2)
            $py = $gy * $gh + [int]($gh/2)
            $col = $bmp.GetPixel($px, $py)
            if ($col.A -gt 10) {
                $hasOpaque = $true
            }
            if ($hasOpaque) { $row += "#" } else { $row += "." }
        }
        Write-Host "  [$([string]::Format('{0:d2}', $gy))] $row"
    }
    $bmp.Dispose()
}

Scan-Regions "src\main\resources\assets\miningtools\textures\entity\wood_cart.png"
Write-Host ""
Scan-Regions "src\main\resources\assets\miningtools\textures\entity\container_cart_green.png"
Write-Host ""
Scan-Regions "src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png"
