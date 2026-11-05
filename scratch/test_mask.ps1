Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
$w = $base.Width
$h = $base.Height

# Check straight track rail pixels
# Let's find pixels that belong to rails vs ties.
# Rail pixels have grayish colors (R, G, B are close to each other, like R~G~B)
# Tie pixels have wood colors (R > G > B, e.g. R ~ 1.3-1.6 * B)

$railMask = New-Object 'bool[,]' $w, $h

for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 100) {
            # Check if it's rail or tie
            # Wood has high R, moderate G, low B. For example R=226, G=158, B=98
            # Steel has R ~ G ~ B, e.g. R=100..230, G=105..235, B=115..240
            $isSteel = ($c.B -gt 70) -and ([Math]::Abs($c.R - $c.B) -lt 30) -and ([Math]::Abs($c.G - $c.B) -lt 25)
            # Or outline of rail
            $isOutline = ($c.R -lt 55 -and $c.G -lt 55 -and $c.B -lt 65 -and ($c.B -ge $c.R))
            if ($isSteel -or $isOutline) {
                $railMask[$x, $y] = $true
            }
        }
    }
}

# Create a visualization of rail mask vs tie mask
$vis = New-Object System.Drawing.Bitmap 128, 128
$g = [System.Drawing.Graphics]::FromImage($vis)
$g.Clear([System.Drawing.Color]::Transparent)

for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        if ($railMask[$x, $y]) {
            $vis.SetPixel($x + 16, $y + 37, [System.Drawing.Color]::Blue)
        } elseif ($base.GetPixel($x, $y).A -gt 100) {
            $vis.SetPixel($x + 16, $y + 37, [System.Drawing.Color]::Brown)
        }
    }
}
$vis.Save("scratch_vis_mask.png", [System.Drawing.Imaging.ImageFormat]::Png)
$vis.Dispose()
$base.Dispose()
Write-Output "Mask saved"
