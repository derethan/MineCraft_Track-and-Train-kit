Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap('src/main/resources/assets/miningtools/textures/entity/logging_cart.png')

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

    # Check 6 faces for 4x4x24:
    # 1. UP (Top): U = u0+4..u0+28 (X = (u0+4)*8..(u0+28)*8), V = v0..v0+4 (Y = v0*8..(v0+4)*8)
    # 2. DOWN (Bottom): U = u0+28..u0+52, V = v0..v0+4
    # 3. WEST (Left end): U = u0..u0+4, V = v0+4..v0+8
    # 4. NORTH (Front): U = u0+4..u0+28, V = v0+4..v0+8
    # 5. EAST (Right end): U = u0+28..u0+32, V = v0+4..v0+8
    # 6. SOUTH (Back): U = u0+32..u0+56, V = v0+4..v0+8

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
    Write-Output "PERFECT: All 6 faces of all 5 logs have 0 transparent pixels!"
} else {
    Write-Output "Total missing pixels: $totalMissing"
}

$bmp.Dispose()
