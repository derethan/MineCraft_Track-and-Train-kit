Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

# Let's see what is drawn for log1 in the original 5x5x24 texture:
# Top row (V = 40..45, height 5):
# U = 0..5 (empty: alpha=0)
# U = 5..29 (Top face: width 24, height 5)
# U = 29..53 (Bottom face: width 24, height 5)
# U = 53..64 (empty: alpha=0)
# Bottom row (V = 45..50, height 5):
# U = 0..5 (Left end face: width 5, height 5)
# U = 5..29 (Front face: width 24, height 5)
# U = 29..34 (Right end face: width 5, height 5)
# U = 34..58 (Back face: width 24, height 5)
# U = 58..64 (empty: alpha=0)

Write-Output "Checking row 40..45 for U=0..58:"
for ($v = 40; $v -lt 50; $v++) {
    $row = "V=${v}: "
    for ($u = 0; $u -lt 60; $u += 2) {
        $a = $bmp.GetPixel($u*8+4, $v*8+4).A
        if ($a -eq 0) { $row += "." } else { $row += "#" }
    }
    Write-Output $row
}

$bmp.Dispose()
