Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/wood_cart.png')

$logs = @(
    @{ name = "log1"; u0 = 0;  v0 = 40 },
    @{ name = "log2"; u0 = 64; v0 = 40 },
    @{ name = "log3"; u0 = 0;  v0 = 54 },
    @{ name = "log4"; u0 = 64; v0 = 54 },
    @{ name = "log5"; u0 = 0;  v0 = 68 }
)

$totalMissing = 0
foreach ($log in $logs) {
    $u0 = $log.u0
    $v0 = $log.v0
    $name = $log.name

    $faces = @(
        @{ name = "UP";    x = ($u0+4)*8;  y = $v0*8;     w = 192; h = 32 },
        @{ name = "DOWN";  x = ($u0+28)*8; y = $v0*8;     w = 192; h = 32 },
        @{ name = "WEST";  x = $u0*8;      y = ($v0+4)*8; w = 32;  h = 32 },
        @{ name = "NORTH"; x = ($u0+4)*8;  y = ($v0+4)*8; w = 192; h = 32 },
        @{ name = "EAST";  x = ($u0+28)*8; y = ($v0+4)*8; w = 32;  h = 32 },
        @{ name = "SOUTH"; x = ($u0+32)*8; y = ($v0+4)*8; w = 192; h = 32 }
    )

    foreach ($f in $faces) {
        $bad = 0
        for ($py = $f.y; $py -lt ($f.y + $f.h); $py++) {
            for ($px = $f.x; $px -lt ($f.x + $f.w); $px++) {
                if ($bmp.GetPixel($px, $py).A -lt 250) {
                    $bad++
                }
            }
        }
        if ($bad -gt 0) {
            Write-Output "[$name] $($f.name) has $bad transparent pixels!"
            $totalMissing += $bad
        }
    }
}

if ($totalMissing -eq 0) {
    Write-Output "PERFECT: All 6 faces of all 5 logs in wood_cart.png have 0 transparent pixels!"
} else {
    Write-Output "Total missing pixels: $totalMissing"
}

$bmp.Dispose()
