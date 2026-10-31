Add-Type -AssemblyName System.Drawing

function Resample-Face($srcBmp, $srcX, $srcY, $srcW, $srcH, $destBmp, $dstX, $dstY, $dstW, $dstH) {
    for ($dy = 0; $dy -lt $dstH; $dy++) {
        $sy = [int][Math]::Floor(($dy * $srcH) / $dstH)
        if ($sy -ge $srcH) { $sy = $srcH - 1 }
        $actualY = $srcY + $sy

        for ($dx = 0; $dx -lt $dstW; $dx++) {
            $sx = [int][Math]::Floor(($dx * $srcW) / $dstW)
            if ($sx -ge $srcW) { $sx = $srcW - 1 }
            $actualX = $srcX + $sx

            $c = $srcBmp.GetPixel($actualX, $actualY)
            $solidColor = [System.Drawing.Color]::FromArgb(255, $c.R, $c.G, $c.B)
            $destBmp.SetPixel(($dstX + $dx), ($dstY + $dy), $solidColor)
        }
    }
}

function Remap-AllLogs($srcOriginalPath, $targetImagePath) {
    $srcBmp = New-Object System.Drawing.Bitmap($srcOriginalPath)

    # Read target image (to preserve existing chassis / pallets / whatever else is on it)
    $bytes = [System.IO.File]::ReadAllBytes($targetImagePath)
    $ms = New-Object System.IO.MemoryStream(,$bytes)
    $targetImg = [System.Drawing.Image]::FromStream($ms)
    $destBmp = New-Object System.Drawing.Bitmap($targetImg)
    $targetImg.Dispose()
    $ms.Dispose()

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

        # Clear this log's UV box first
        for ($cy = $v0_px; $cy -lt ($v0_px + 80); $cy++) {
            for ($cx = $u0_px; $cx -lt ($u0_px + 512); $cx++) {
                if ($cx -lt 1024 -and $cy -lt 1024) {
                    $destBmp.SetPixel($cx, $cy, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                }
            }
        }

        # 1. UP (Top Bark): src (u0+40, v0, 192, 40) -> dst (u0+32, v0, 192, 32)
        Resample-Face $srcBmp ($u0_px + 40) ($v0_px + 0) 192 40 $destBmp ($u0_px + 32) ($v0_px + 0) 192 32

        # 2. DOWN (Bottom Bark): src (u0+232, v0, 192, 40) -> dst (u0+224, v0, 192, 32)
        Resample-Face $srcBmp ($u0_px + 232) ($v0_px + 0) 192 40 $destBmp ($u0_px + 224) ($v0_px + 0) 192 32

        # 3. WEST (Left End - tree rings): src (u0, v0+40, 40, 40) -> dst (u0, v0+32, 32, 32)
        Resample-Face $srcBmp ($u0_px + 0) ($v0_px + 40) 40 40 $destBmp ($u0_px + 0) ($v0_px + 32) 32 32

        # 4. NORTH (Front Bark): src (u0+40, v0+40, 192, 40) -> dst (u0+32, v0+32, 192, 32)
        Resample-Face $srcBmp ($u0_px + 40) ($v0_px + 40) 192 40 $destBmp ($u0_px + 32) ($v0_px + 32) 192 32

        # 5. EAST (Right End - tree rings): src (u0+232, v0+40, 40, 40) -> dst (u0+224, v0+32, 32, 32)
        Resample-Face $srcBmp ($u0_px + 232) ($v0_px + 40) 40 40 $destBmp ($u0_px + 224) ($v0_px + 32) 32 32

        # 6. SOUTH (Back Bark): src (u0+272, v0+40, 192, 40) -> dst (u0+256, v0+32, 192, 32)
        Resample-Face $srcBmp ($u0_px + 272) ($v0_px + 40) 192 40 $destBmp ($u0_px + 256) ($v0_px + 32) 192 32

        # Bleed/Padding: Clamp-pad the outer edges by 2 pixels to completely prevent mipmap edge bleeding
        # Top edge of row 0 (Y = v0_px - 1 .. -2)
        for ($pad = 1; $pad -le 2; $pad++) {
            $padY = $v0_px - $pad
            if ($padY -ge 0) {
                for ($px = ($u0_px + 32); $px -lt ($u0_px + 416); $px++) {
                    $c = $destBmp.GetPixel($px, $v0_px)
                    $destBmp.SetPixel($px, $padY, $c)
                }
            }
        }
        # Bottom edge of row 1 (Y = v0_px + 64 .. + 65)
        for ($pad = 0; $pad -lt 2; $pad++) {
            $padY = $v0_px + 64 + $pad
            if ($padY -lt 1024) {
                for ($px = $u0_px; $px -lt ($u0_px + 448); $px++) {
                    $c = $destBmp.GetPixel($px, ($v0_px + 63))
                    $destBmp.SetPixel($px, $padY, $c)
                }
            }
        }
        # Left edge of LeftEnd (X = u0_px - 1 .. -2)
        for ($pad = 1; $pad -le 2; $pad++) {
            $padX = $u0_px - $pad
            if ($padX -ge 0) {
                for ($py = ($v0_px + 32); $py -lt ($v0_px + 64); $py++) {
                    $c = $destBmp.GetPixel($u0_px, $py)
                    $destBmp.SetPixel($padX, $py, $c)
                }
            }
        }
        # Right edge of BackBark (X = u0_px + 448 .. + 449)
        for ($pad = 0; $pad -lt 2; $pad++) {
            $padX = $u0_px + 448 + $pad
            if ($padX -lt 1024) {
                for ($py = ($v0_px + 32); $py -lt ($v0_px + 64); $py++) {
                    $c = $destBmp.GetPixel(($u0_px + 447), $py)
                    $destBmp.SetPixel($padX, $py, $c)
                }
            }
        }
    }

    $destBmp.Save($targetImagePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $destBmp.Dispose()
    $srcBmp.Dispose()
    Write-Output "Successfully updated: $targetImagePath"
}

Remap-AllLogs "scratch/logging_cart_backup.png" "src/main/resources/assets/miningtools/textures/entity/logging_cart.png"
Remap-AllLogs "scratch/logging_cart_backup.png" "src/main/resources/assets/miningtools/textures/entity/wood_cart.png"
