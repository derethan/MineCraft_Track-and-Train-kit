Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

for ($v = 78; $v -lt 128; $v += 4) {
    $row = "V=${v}: "
    for ($u = 0; $u -lt 128; $u += 4) {
        $a = $bmp.GetPixel($u*8+2, $v*8+2).A
        if ($a -eq 0) { $row += "." } else { $row += "#" }
    }
    Write-Output $row
}

$bmp.Dispose()
