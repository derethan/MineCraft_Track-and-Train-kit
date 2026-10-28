Add-Type -AssemblyName System.Drawing

function Inspect-Hopper($path) {
    $bmp = New-Object System.Drawing.Bitmap($path)
    Write-Host "Hopper $path ($($bmp.Width)x$($bmp.Height)):"
    # Check lower frame & wheels area.
    # In AggregateHopperCartModel:
    # Underframe draft beam is texOffs(0, 0)
    # Wheels were texOffs(54, 0), (68, 0), (82, 0), (96, 0) in vanilla
    # Or in our new model: TruckModelHelper at texOffs(54, 20), (80, 20), (104, 20)
    
    $scale = $bmp.Width / 128.0
    
    # Check texOffs(0, 0)
    $cUnderframe = $bmp.GetPixel([int](10 * $scale), [int](2 * $scale))
    Write-Host "  Underframe at (10, 2): R=$($cUnderframe.R), G=$($cUnderframe.G), B=$($cUnderframe.B)"
    
    # Check texOffs(54, 0)
    $cWheelOld = $bmp.GetPixel([int](56 * $scale), [int](2 * $scale))
    Write-Host "  Old wheel area (56, 2): R=$($cWheelOld.R), G=$($cWheelOld.G), B=$($cWheelOld.B)"
    
    # Check texOffs(54, 20)
    $cTruckNew = $bmp.GetPixel([int](56 * $scale), [int](21 * $scale))
    Write-Host "  New truck area (56, 21): R=$($cTruckNew.R), G=$($cTruckNew.G), B=$($cTruckNew.B)"
    
    $bmp.Dispose()
}

Inspect-Hopper "src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png"
Inspect-Hopper "resourcepacks\Track_and_Train_HD_PBR\assets\miningtools\textures\entity\hopper_cart_coal.png"
