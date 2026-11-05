Add-Type -AssemblyName System.Drawing

$base = [System.Drawing.Bitmap]::FromFile("c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png")
$w = $base.Width
$h = $base.Height

# Find rail top edges for each X
$topRail = @()
$botRail = @()

for ($x = 0; $x -lt $w; $x++) {
    $yVals = @()
    for ($y = 0; $y -lt $h; $y++) {
        $c = $base.GetPixel($x, $y)
        if ($c.A -gt 150) {
            # Highlight or body of rail
            if ($c.R -gt 90 -and $c.B -gt 100 -and [Math]::Abs($c.R - $c.B) -lt 25) {
                $yVals += $y
            }
        }
    }
    if ($yVals.Count -ge 2) {
        # Group into top rail and bottom rail
        $yMin = $yVals[0]
        $yMax = $yVals[-1]
        if ($yMax - $yMin -gt 6) {
            # Two rails present
            $mid = ($yMin + $yMax) / 2
            $t = $yVals | Where-Object { $_ -lt $mid }
            $b = $yVals | Where-Object { $_ -ge $mid }
            if ($t) { $topRail += [PSCustomObject]@{ X = $x; Y = ($t | Measure-Object -Average).Average } }
            if ($b) { $botRail += [PSCustomObject]@{ X = $x; Y = ($b | Measure-Object -Average).Average } }
        }
    }
}

Write-Output "Top rail start: $($topRail[0].X), $($topRail[0].Y)  end: $($topRail[-1].X), $($topRail[-1].Y)"
Write-Output "Bot rail start: $($botRail[0].X), $($botRail[0].Y)  end: $($botRail[-1].X), $($botRail[-1].Y)"

$base.Dispose()
