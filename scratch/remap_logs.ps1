Add-Type -AssemblyName System.Drawing

function Remap-LogTexture($imagePath) {
    $bytes = [System.IO.File]::ReadAllBytes($imagePath)
    $ms = New-Object System.IO.MemoryStream(,$bytes)
    $srcImg = [System.Drawing.Image]::FromStream($ms)
    $srcBmp = New-Object System.Drawing.Bitmap($srcImg)
    $destBmp = New-Object System.Drawing.Bitmap($srcImg)

    $g = [System.Drawing.Graphics]::FromImage($destBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $logs = @(
        @{ u0 = 0;  v0 = 40 },  # log1
        @{ u0 = 64; v0 = 40 },  # log2
        @{ u0 = 0;  v0 = 54 },  # log3
        @{ u0 = 64; v0 = 54 },  # log4
        @{ u0 = 0;  v0 = 68 }   # log5
    )

    foreach ($log in $logs) {
        $u0_px = [int]($log.u0 * 8)
        $v0_px = [int]($log.v0 * 8)

        # Original coordinates (for 5x5x24)
        $srcLeftEnd    = New-Object System.Drawing.Rectangle(($u0_px + 0),   ($v0_px + 40), 40,  40)
        $srcTopBark    = New-Object System.Drawing.Rectangle(($u0_px + 40),  ($v0_px + 0),  192, 40)
        $srcBottomBark = New-Object System.Drawing.Rectangle(($u0_px + 232), ($v0_px + 0),  192, 40)
        $srcFrontBark  = New-Object System.Drawing.Rectangle(($u0_px + 40),  ($v0_px + 40), 192, 40)
        $srcRightEnd   = New-Object System.Drawing.Rectangle(($u0_px + 232), ($v0_px + 40), 40,  40)
        $srcBackBark   = New-Object System.Drawing.Rectangle(($u0_px + 272), ($v0_px + 40), 192, 40)

        # Make copy of original face bitmaps from srcBmp
        $bmpLeftEnd    = $srcBmp.Clone($srcLeftEnd,    $srcBmp.PixelFormat)
        $bmpTopBark    = $srcBmp.Clone($srcTopBark,    $srcBmp.PixelFormat)
        $bmpBottomBark = $srcBmp.Clone($srcBottomBark, $srcBmp.PixelFormat)
        $bmpFrontBark  = $srcBmp.Clone($srcFrontBark,  $srcBmp.PixelFormat)
        $bmpRightEnd   = $srcBmp.Clone($srcRightEnd,   $srcBmp.PixelFormat)
        $bmpBackBark   = $srcBmp.Clone($srcBackBark,   $srcBmp.PixelFormat)

        # Clear the old log area in destBmp
        # Total area for this log is u0..u0+64, v0..v0+14 (512x112 px)
        for ($cy = $v0_px; $cy -lt ($v0_px + 112); $cy++) {
            for ($cx = $u0_px; $cx -lt ($u0_px + 512); $cx++) {
                if ($cx -lt 1024 -and $cy -lt 1024) {
                    $destBmp.SetPixel($cx, $cy, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                }
            }
        }

        # Target coordinates (for 4x4x24):
        # 1. UP (Top Bark): X = u0_px + 32, Y = v0_px + 0, W = 192, H = 32
        $dstTopBark = New-Object System.Drawing.Rectangle(($u0_px + 32), ($v0_px + 0), 192, 32)
        $g.DrawImage($bmpTopBark, $dstTopBark)

        # 2. DOWN (Bottom Bark): X = u0_px + 224, Y = v0_px + 0, W = 192, H = 32
        $dstBottomBark = New-Object System.Drawing.Rectangle(($u0_px + 224), ($v0_px + 0), 192, 32)
        $g.DrawImage($bmpBottomBark, $dstBottomBark)

        # 3. WEST (Left End - tree rings): X = u0_px + 0, Y = v0_px + 32, W = 32, H = 32
        $dstLeftEnd = New-Object System.Drawing.Rectangle(($u0_px + 0), ($v0_px + 32), 32, 32)
        $g.DrawImage($bmpLeftEnd, $dstLeftEnd)

        # 4. NORTH (Front Bark): X = u0_px + 32, Y = v0_px + 32, W = 192, H = 32
        $dstFrontBark = New-Object System.Drawing.Rectangle(($u0_px + 32), ($v0_px + 32), 192, 32)
        $g.DrawImage($bmpFrontBark, $dstFrontBark)

        # 5. EAST (Right End - tree rings): X = u0_px + 224, Y = v0_px + 32, W = 32, H = 32
        $dstRightEnd = New-Object System.Drawing.Rectangle(($u0_px + 224), ($v0_px + 32), 32, 32)
        $g.DrawImage($bmpRightEnd, $dstRightEnd)

        # 6. SOUTH (Back Bark): X = u0_px + 256, Y = v0_px + 32, W = 192, H = 32
        $dstBackBark = New-Object System.Drawing.Rectangle(($u0_px + 256), ($v0_px + 32), 192, 32)
        $g.DrawImage($bmpBackBark, $dstBackBark)

        # Clean up cloned bitmaps
        $bmpLeftEnd.Dispose()
        $bmpTopBark.Dispose()
        $bmpBottomBark.Dispose()
        $bmpFrontBark.Dispose()
        $bmpRightEnd.Dispose()
        $bmpBackBark.Dispose()
    }

    $g.Dispose()
    $srcBmp.Dispose()
    $srcImg.Dispose()
    $ms.Dispose()

    # Save remap
    $destBmp.Save($imagePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $destBmp.Dispose()
    Write-Output "Successfully remapped log textures in: $imagePath"
}

Remap-LogTexture "src/main/resources/assets/miningtools/textures/entity/logging_cart.png"
Remap-LogTexture "src/main/resources/assets/miningtools/textures/entity/wood_cart.png"
