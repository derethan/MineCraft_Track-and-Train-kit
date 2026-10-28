Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 'C:\Users\swack\Desktop\janemoose\src\main\resources\assets\janemoose\textures\entity\tractor.png'
$norm = New-Object System.Drawing.Bitmap 'C:\Users\swack\Desktop\janemoose\src\main\resources\assets\janemoose\textures\entity\tractor_n.png'
$spec = New-Object System.Drawing.Bitmap 'C:\Users\swack\Desktop\janemoose\src\main\resources\assets\janemoose\textures\entity\tractor_s.png'

Write-Host "Tractor image size: $($bmp.Width) x $($bmp.Height)"
Write-Host "Normal image size: $($norm.Width) x $($norm.Height)"
Write-Host "Specular image size: $($spec.Width) x $($spec.Height)"

$minH = 255; $maxH = 0
$minSm = 255; $maxSm = 0
$minMet = 255; $maxMet = 0
$metals = @{}
$smooths = @{}
$heights = @{}

for ($y = 0; $y -lt $bmp.Height; $y += 8) {
    for ($x = 0; $x -lt $bmp.Width; $x += 8) {
        $c = $bmp.GetPixel($x, $y)
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
        if ($met -lt $minMet) { $minMet = $met }
        if ($met -gt $maxMet) { $maxMet = $met }

        $metals[$met] = 1
        $smooths[[Math]::Round($sm / 20) * 20] = 1
        $heights[[Math]::Round($h / 20) * 20] = 1
    }
}

Write-Host "Height range: $minH to $maxH. Distinct height buckets: $($heights.Keys -join ', ')"
Write-Host "Smoothness range: $minSm to $maxSm. Distinct smooth buckets: $($smooths.Keys -join ', ')"
Write-Host "Metalness range: $minMet to $maxMet. Distinct metals: $($metals.Keys -join ', ')"

$bmp.Dispose()
$norm.Dispose()
$spec.Dispose()
