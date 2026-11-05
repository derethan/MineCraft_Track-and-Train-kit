Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
for ($x = 10; $x -lt 90; $x += 10) {
    for ($y = 0; $y -lt $base.Height; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 150 -and $c.R -gt 120 -and $c.G -gt 120 -and $c.B -gt 120) {
            Write-Output "x=$x y=$y color=($($c.R),$($c.G),$($c.B))"
            break
        }
    }
}
$base.Dispose()
