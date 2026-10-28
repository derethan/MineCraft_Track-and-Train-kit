Add-Type -AssemblyName System.Drawing

$entityDir = "src\main\resources\assets\miningtools\textures\entity"

# 1. Update container_cart*.png with flatbed from wood_cart.png
$woodBmp = New-Object System.Drawing.Bitmap(Join-Path $entityDir "wood_cart.png")

$containers = @("container_cart.png", "container_cart_green.png", "container_cart_blue.png", "container_cart_red.png")
foreach ($cName in $containers) {
    $cPath = Join-Path $entityDir $cName
    if (Test-Path $cPath) {
        Write-Host "Updating container texture with flatbed: $cName"
        $cBmp = New-Object System.Drawing.Bitmap($cPath)
        
        # Clone to editable 32bppArgb bitmap
        $newBmp = New-Object System.Drawing.Bitmap($cBmp.Width, $cBmp.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($newBmp)
        
        # Draw existing container
        $g.DrawImage($cBmp, 0, 0)
        
        # Overlay the flatbed deck, undercarriage & trucks (Y = 0 to 319) from wood_cart
        # wood_cart and container_cart are both 1024x1024
        $flatbedRect = New-Object System.Drawing.Rectangle(0, 0, $woodBmp.Width, 319)
        $g.DrawImage($woodBmp, $flatbedRect, $flatbedRect, [System.Drawing.GraphicsUnit]::Pixel)
        
        $g.Dispose()
        $cBmp.Dispose()
        
        # Save updated texture
        $newBmp.Save($cPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $newBmp.Dispose()
        Write-Host "  -> Done $cName"
    }
}

# 2. Update hopper_cart_*.png
# Make lower frame black and copy wheels/trucks from wood_cart.png
$hoppers = @(
    "hopper_cart_coal.png",
    "hopper_cart_dirt.png",
    "hopper_cart_empty.png",
    "hopper_cart_gravel.png",
    "hopper_cart_ore.png",
    "hopper_cart_red_sand.png",
    "hopper_cart_sand.png"
)

$blackBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 24, 26, 28))

foreach ($hName in $hoppers) {
    $hPath = Join-Path $entityDir $hName
    if (Test-Path $hPath) {
        Write-Host "Updating hopper cart texture: $hName"
        $hBmp = New-Object System.Drawing.Bitmap($hPath)
        $newBmp = New-Object System.Drawing.Bitmap($hBmp.Width, $hBmp.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($newBmp)
        
        # Draw original hopper tub and aggregates
        $g.DrawImage($hBmp, 0, 0)
        
        # Clear out old rust across the entire upper section (Y = 0..239)
        $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $clearBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        $g.FillRectangle($clearBrush, 0, 0, $hBmp.Width, 240)
        $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        
        # Repaint the entire lower frame (draft sill beam, couplers, bottom floor plate)
        # texOffs(0, 0), texOffs(0, 10), texOffs(0, 18) all lie within (0, 0) to (480, 239)
        $g.FillRectangle($blackBrush, 0, 0, 480, 240)
        
        # Copy matching trucks & wheels from wood_cart.png (X = 432..1024, Y = 160..240)
        $truckRect = New-Object System.Drawing.Rectangle(432, 160, 592, 80)
        $g.DrawImage($woodBmp, $truckRect, $truckRect, [System.Drawing.GraphicsUnit]::Pixel)
        
        $g.Dispose()
        $hBmp.Dispose()
        
        $newBmp.Save($hPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $newBmp.Dispose()
        Write-Host "  -> Done $hName"
    }
}

$blackBrush.Dispose()
$woodBmp.Dispose()
Write-Host "All vanilla textures updated successfully!"
