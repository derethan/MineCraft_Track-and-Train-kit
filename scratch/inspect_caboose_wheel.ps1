Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 'src/main/resources/assets/miningtools/textures/entity/caboose.png'
Write-Host "Original Caboose size: $($bmp.Width) x $($bmp.Height)"
Write-Host "Color at (152, 360) [sideframe]: $($bmp.GetPixel(152, 360))"
Write-Host "Color at (272, 360) [wheel]: $($bmp.GetPixel(272, 360))"
Write-Host "Color at (280, 368) [wheel center]: $($bmp.GetPixel(280, 368))"
$bmp.Dispose()
