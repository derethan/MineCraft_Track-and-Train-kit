Add-Type -AssemblyName System.Drawing

function Inspect-Image($path) {
    if (-not (Test-Path $path)) {
        Write-Host "File not found: $path"
        return
    }
    $bmp = New-Object System.Drawing.Bitmap($path)
    Write-Host "File: $path"
    Write-Host "Size: $($bmp.Width) x $($bmp.Height)"
    
    # Check (0,0) to (10,10)
    $sample1 = $bmp.GetPixel(5, 5)
    Write-Host "Pixel at (5,5): Alpha=$($sample1.A), R=$($sample1.R), G=$($sample1.G), B=$($sample1.B)"
    
    # Check container at (10, 50)
    $sample2 = $bmp.GetPixel([int]($bmp.Width * 0.1), [int]($bmp.Height * 0.45))
    Write-Host "Pixel at (10%, 45%): Alpha=$($sample2.A), R=$($sample2.R), G=$($sample2.G), B=$($sample2.B)"

    $bmp.Dispose()
}

Write-Host "--- MOD VANILLA TEXTURES ---"
Inspect-Image "src\main\resources\assets\miningtools\textures\entity\wood_cart.png"
Inspect-Image "src\main\resources\assets\miningtools\textures\entity\container_cart_green.png"
Inspect-Image "src\main\resources\assets\miningtools\textures\entity\hopper_cart_coal.png"

Write-Host "`n--- RESOURCE PACK TEXTURES ---"
Inspect-Image "resourcepacks\Track_and_Train_HD_PBR\assets\miningtools\textures\entity\container_cart_green.png"
Inspect-Image "resourcepacks\Track_and_Train_HD_PBR\assets\miningtools\textures\entity\hopper_cart_coal.png"
