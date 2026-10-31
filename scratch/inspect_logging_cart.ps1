Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

# In 1024x1024 image, each UV unit is 8x8 pixels.
# Let's inspect where all non-empty regions are in logging_cart.png
$grid = @{}
for ($v = 0; $v -lt 128; $v++) {
    for ($u = 0; $u -lt 128; $u++) {
        $nonEmpty = $false
        for ($py = 0; $py -lt 8; $py++) {
            for ($px = 0; $px -lt 8; $px++) {
                if ($bmp.GetPixel($u*8+$px, $v*8+$py).A -gt 10) {
                    $nonEmpty = $true
                    break
                }
            }
            if ($nonEmpty) { break }
        }
        if ($nonEmpty) {
            $grid["$u,$v"] = $true
        }
    }
}

Write-Output "Found non-empty cells in 128x128 grid: $($grid.Count)"
# Let's see what is at V >= 80
$v80 = 0
foreach ($k in $grid.Keys) {
    $parts = $k.Split(',')
    $v = [int]$parts[1]
    if ($v -ge 80) { $v80++ }
}
Write-Output "Cells with V >= 80: $v80"

$bmp.Dispose()
