Add-Type -AssemblyName System.Drawing

function Check-TruckArea($name) {
    $bmp = New-Object System.Drawing.Bitmap("src\main\resources\assets\miningtools\textures\entity\$name")
    Write-Host "$name ($($bmp.Width)x$($bmp.Height)):"
    # texOffs(0, 0): underframe
    $pFrame = $bmp.GetPixel(30, 10)
    Write-Host "  Frame at (30, 10): ($($pFrame.R), $($pFrame.G), $($pFrame.B))"
    # texOffs(54, 20): sideframe bracket
    $pSide = $bmp.GetPixel(55 * 8, 22 * 8)
    Write-Host "  Sideframe at (55, 22): ($($pSide.R), $($pSide.G), $($pSide.B))"
    # texOffs(80, 20): wheel
    $pWheel = $bmp.GetPixel(82 * 8, 22 * 8)
    Write-Host "  Wheel at (82, 22): ($($pWheel.R), $($pWheel.G), $($pWheel.B))"
    $bmp.Dispose()
}

Check-TruckArea "wood_cart.png"
Check-TruckArea "hopper_cart_coal.png"
Check-TruckArea "generator_cart.png"
