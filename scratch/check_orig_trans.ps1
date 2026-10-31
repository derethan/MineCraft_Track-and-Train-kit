Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('scratch/logging_cart_backup.png')

# Check origLeftEnd for log1: X=0..40, Y=360..400
$transInOrigLeft = 0
for ($y = 360; $y -lt 400; $y++) {
    for ($x = 0; $x -lt 40; $x++) {
        if ($bmp.GetPixel($x, $y).A -lt 250) {
            $transInOrigLeft++
        }
    }
}
Write-Output "Transparent pixels in original log1 left end (0..40, 360..400): $transInOrigLeft"

# Check origTopBark for log1: X=40..232, Y=320..360
$transInOrigTop = 0
for ($y = 320; $y -lt 360; $y++) {
    for ($x = 40; $x -lt 232; $x++) {
        if ($bmp.GetPixel($x, $y).A -lt 250) {
            $transInOrigTop++
        }
    }
}
Write-Output "Transparent pixels in original log1 top bark (40..232, 320..360): $transInOrigTop"

$bmp.Dispose()
