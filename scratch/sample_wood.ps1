Add-Type -AssemblyName System.Drawing
$woodBmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/wood_cart.png')
Write-Output "Sampling wood_cart.png at lumber area (V=80):"
for ($y = 640; $y -lt 680; $y += 10) {
    $c = $woodBmp.GetPixel(100, $y)
    Write-Output "y=$y R=$($c.R) G=$($c.G) B=$($c.B) A=$($c.A)"
}
$woodBmp.Dispose()
