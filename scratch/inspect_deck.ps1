Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\wood_cart.png")
Write-Host "wood_cart size: $($bmp.Width) x $($bmp.Height)"

# In 1024x1024, scale is 8 (since UV space is 128x128)
# Flatbed deck is texOffs(0, 0), addBox(-12, 2, -8, 24, 2, 16)
# UV width = 2*(16 + 24) = 80 units = 640 pixels
# UV height = 16 + 2 = 18 units = 144 pixels
Write-Host "Deck box in 1024x1024: (0, 0) to (640, 144)"
$deckSample = $bmp.GetPixel(100, 50)
Write-Host "Deck sample pixel at (100, 50): Alpha=$($deckSample.A), R=$($deckSample.R), G=$($deckSample.G), B=$($deckSample.B)"

# Center sill: texOffs(0, 20), addBox(-9, 4, -4, 18, 2, 8)
# UV width = 2*(8 + 18) = 52 units = 416 pixels
# UV height = 8 + 2 = 10 units = 80 pixels
# UV V = 20..30 = 160..240 pixels
$sillSample = $bmp.GetPixel(100, 180)
Write-Host "Sill sample pixel at (100, 180): Alpha=$($sillSample.A), R=$($sillSample.R), G=$($sillSample.G), B=$($sillSample.B)"

$bmp.Dispose()
