Add-Type -AssemblyName System.Drawing
$diff = New-Object System.Drawing.Bitmap 'resourcepacks/Track_and_Train_HD_PBR/assets/miningtools/textures/entity/diesel_locomotive.png'
$norm = New-Object System.Drawing.Bitmap 'resourcepacks/Track_and_Train_HD_PBR/assets/miningtools/textures/entity/diesel_locomotive_n.png'
$spec = New-Object System.Drawing.Bitmap 'resourcepacks/Track_and_Train_HD_PBR/assets/miningtools/textures/entity/diesel_locomotive_s.png'

Write-Host "Diesel Locomotive Diffuse: $($diff.Width)x$($diff.Height)"
Write-Host "Normal: $($norm.Width)x$($norm.Height)"
Write-Host "Specular: $($spec.Width)x$($spec.Height)"

$minH = 255; $maxH = 0
$minSm = 255; $maxSm = 0
$metals = @{}
$emCount = 0

for ($y = 0; $y -lt $diff.Height; $y += 8) {
    for ($x = 0; $x -lt $diff.Width; $x += 8) {
        $c = $diff.GetPixel($x, $y)
        if ($c.A -eq 0) { continue }
        $n = $norm.GetPixel($x, $y)
        $s = $spec.GetPixel($x, $y)
        
        $h = $n.A
        $sm = $s.R
        $met = $s.G

        if ($h -lt $minH) { $minH = $h }
        if ($h -gt $maxH) { $maxH = $h }
        if ($sm -lt $minSm) { $minSm = $sm }
        if ($sm -gt $maxSm) { $maxSm = $sm }
        $metals[$met] = 1
        if ($s.A -lt 255) { $emCount++ }
    }
}

Write-Host "Height dynamic range: $minH to $maxH"
Write-Host "Smoothness dynamic range: $minSm to $maxSm"
Write-Host "Active Metal IDs: $($metals.Keys -join ', ')"
Write-Host "Emissive samples detected: $emCount"

$diff.Dispose()
$norm.Dispose()
$spec.Dispose()
