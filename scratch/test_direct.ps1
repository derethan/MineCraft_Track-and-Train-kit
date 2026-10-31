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
            # Force Alpha to 255 to guarantee solid opacity
            $solidColor = [System.Drawing.Color]::FromArgb(255, $c.R, $c.G, $c.B)
            $destBmp.SetPixel(($dstX + $dx), ($dstY + $dy), $solidColor)
        }
    }
}

# Test it on srcTopBark:
$srcBmp = New-Object System.Drawing.Bitmap('scratch/logging_cart_backup.png')
$destBmp = New-Object System.Drawing.Bitmap(1024, 1024, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

Resample-Face $srcBmp 40 320 192 40 $destBmp 32 320 192 32

$bad = 0
for ($y = 320; $y -lt (320 + 32); $y++) {
    for ($x = 32; $x -lt (32 + 192); $x++) {
        if ($destBmp.GetPixel($x, $y).A -ne 255) {
            $bad++
        }
    }
}
Write-Output "Bad pixels with direct pixel sampling: $bad"

$destBmp.Dispose()
$srcBmp.Dispose()
