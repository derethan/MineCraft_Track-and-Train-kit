Add-Type -AssemblyName System.Drawing

$srcBmp = New-Object System.Drawing.Bitmap('scratch/logging_cart_backup.png')
$destBmp = New-Object System.Drawing.Bitmap(1024, 1024, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

$g = [System.Drawing.Graphics]::FromImage($destBmp)
$g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBilinear
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

$ia = New-Object System.Drawing.Imaging.ImageAttributes
$ia.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::Clamp)

# Let's test drawing top bark: src (40, 320, 192, 40) to dst (32, 320, 192, 32)
$dst = New-Object System.Drawing.Rectangle(32, 320, 192, 32)
$g.DrawImage($srcBmp, $dst, 40, 320, 192, 40, [System.Drawing.GraphicsUnit]::Pixel, $ia)

$bad = 0
for ($y = 320; $y -lt (320 + 32); $y++) {
    for ($x = 32; $x -lt (32 + 192); $x++) {
        if ($destBmp.GetPixel($x, $y).A -lt 255) {
            $bad++
        }
    }
}
Write-Output "Bad pixels with WrapMode.Clamp and SourceCopy: $bad (out of $(192*32))"

$g.Dispose()
$ia.Dispose()
$destBmp.Dispose()
$srcBmp.Dispose()
