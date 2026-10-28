Add-Type -AssemblyName System.Drawing

$packDir = "resourcepacks\Track_and_Train_HD_PBR\assets\miningtools\textures\entity"

Get-ChildItem $packDir -Filter "*.png" | ForEach-Object {
    $bmp = New-Object System.Drawing.Bitmap($_.FullName)
    
    # Check if image is mostly transparent or what
    $opaqueCount = 0
    $sampleTotal = 100
    for ($i = 0; $i -lt $sampleTotal; $i++) {
        $rx = [int](($i * 37) % $bmp.Width)
        $ry = [int](($i * 59) % $bmp.Height)
        if ($bmp.GetPixel($rx, $ry).A -gt 10) { $opaqueCount++ }
    }
    
    Write-Host "$($_.Name) : $($bmp.Width)x$($bmp.Height), opaque test: $opaqueCount/$sampleTotal"
    $bmp.Dispose()
}
