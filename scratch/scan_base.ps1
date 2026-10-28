Add-Type -AssemblyName System.Drawing

function Scan-File($path) {
    $bmp = New-Object System.Drawing.Bitmap($path)
    Write-Host "Scanning $path ($($bmp.Width)x$($bmp.Height)):"
    $gw = [int]($bmp.Width / 16)
    $gh = [int]($bmp.Height / 16)
    for ($gy = 0; $gy -lt 16; $gy++) {
        $row = ""
        for ($gx = 0; $gx -lt 16; $gx++) {
            $px = $gx * $gw + [int]($gw/2)
            $py = $gy * $gh + [int]($gh/2)
            $col = $bmp.GetPixel($px, $py)
            if ($col.A -gt 10) { $row += "#" } else { $row += "." }
        }
        Write-Host "  [$([string]::Format('{0:d2}', $gy))] $row"
    }
    $bmp.Dispose()
}

Scan-File "src\main\resources\assets\miningtools\textures\entity\caboose.png"
Scan-File "src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png"
Scan-File "src\main\resources\assets\miningtools\textures\entity\diesel_locomotive.png"
