Add-Type -AssemblyName System.Drawing

$filePath = "src/main/resources/assets/miningtools/textures/entity/wood_cart.png"
$bytes = [System.IO.File]::ReadAllBytes($filePath)
$ms = New-Object System.IO.MemoryStream(,$bytes)
$srcBmp = [System.Drawing.Image]::FromStream($ms)
$bmp = New-Object System.Drawing.Bitmap($srcBmp)
$srcBmp.Dispose()
$ms.Dispose()

$rnd = New-Object System.Random(42)

# Wood palette for pallets: slightly weathered natural pine / oak
$baseR = 195; $baseG = 162; $baseB = 122

function GetWoodColor($x, $y, $grainDir, $isEdge) {
    $noise = ($rnd.NextDouble() - 0.5) * 16.0
    $grain = [Math]::Sin(($x * 0.2) + ($y * 0.1)) * 12.0
    if ($grainDir -eq "Y") {
        $grain = [Math]::Sin(($y * 0.2) + ($x * 0.1)) * 12.0
    }
    $r = [Math]::Max(0, [Math]::Min(255, [int]($baseR + $grain + $noise)))
    $g = [Math]::Max(0, [Math]::Min(255, [int]($baseG + $grain * 0.9 + $noise * 0.9)))
    $b = [Math]::Max(0, [Math]::Min(255, [int]($baseB + $grain * 0.7 + $noise * 0.7)))

    if ($isEdge) {
        $r = [int]($r * 0.75)
        $g = [int]($g * 0.75)
        $b = [int]($b * 0.75)
    }
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

# Paint pallet slat area: UV (72..108, 88..102) -> X: 576..864, Y: 704..816
for ($y = 704; $y -lt 816; $y++) {
    for ($x = 576; $x -lt 864; $x++) {
        $isEdge = ($x % 64 -eq 0) -or ($x % 64 -eq 63) -or ($y % 32 -eq 0) -or ($y % 32 -eq 31)
        # Add occasional nail head near slat ends
        if (($x % 64 -eq 8 -or $x % 64 -eq 56) -and ($y % 32 -eq 16)) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 60, 60, 65))
            $bmp.SetPixel($x+1, $y, [System.Drawing.Color]::FromArgb(255, 50, 50, 55))
        } else {
            $color = GetWoodColor $x $y "X" $isEdge
            $bmp.SetPixel($x, $y, $color)
        }
    }
}

# Paint pallet stringer and runner area: UV (72..108, 102..112) -> X: 576..864, Y: 816..896
for ($y = 816; $y -lt 896; $y++) {
    for ($x = 576; $x -lt 864; $x++) {
        $isEdge = ($x % 32 -eq 0) -or ($x % 32 -eq 31) -or ($y % 16 -eq 0) -or ($y % 16 -eq 15)
        $color = GetWoodColor $x $y "Y" $isEdge
        $bmp.SetPixel($x, $y, $color)
    }
}

$bmp.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Successfully painted pallet textures to wood_cart.png"
