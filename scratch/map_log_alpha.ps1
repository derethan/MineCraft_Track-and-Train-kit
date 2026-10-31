Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

for ($v = 40; $v -lt 78; $v += 1) {
    $row = ("V={0:D2}: " -f $v)
    for ($u = 0; $u -lt 128; $u += 2) {
        $alpha = $bmp.GetPixel($u * 8 + 4, $v * 8 + 4).A
        if ($alpha -eq 0) {
            $row += "."
        } elseif ($alpha -lt 255) {
            $row += "?"
        } else {
            $row += "#"
        }
    }
    Write-Output $row
}
$bmp.Dispose()
