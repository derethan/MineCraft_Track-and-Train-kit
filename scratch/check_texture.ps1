Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/wood_cart.png')
for ($v = 0; $v -lt 128; $v += 8) {
    $row = ("V={0:D3}: " -f $v)
    for ($u = 0; $u -lt 128; $u += 8) {
        $found = $false
        for ($dy = 0; $dy -lt 64; $dy += 16) {
            for ($dx = 0; $dx -lt 64; $dx += 16) {
                if ($bmp.GetPixel($u * 8 + $dx, $v * 8 + $dy).A -gt 10) {
                    $found = $true
                }
            }
        }
        if ($found) { $row += '#' } else { $row += '.' }
    }
    Write-Output $row
}
$bmp.Dispose()
