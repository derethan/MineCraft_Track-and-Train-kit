Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\caboose.png")

# Let's crop and inspect the text area of caboose
# In Picture 2, the caboose has "CHUNK ANCHOR" centered on the side
Write-Host "Caboose size: $($bmp.Width) x $($bmp.Height)"

# Let's find the bounding box of the white text "CHUNK ANCHOR"
for ($y = 80; $y -lt 140; $y += 2) {
    $line = ""
    for ($x = 20; $x -lt 450; $x += 4) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.R -gt 180 -and $p.G -gt 180 -and $p.B -gt 180) {
            $line += "#"
        } else {
            $line += "."
        }
    }
    if ($line -match "#") {
        Write-Host "Y=$y : $line"
    }
}

$bmp.Dispose()
