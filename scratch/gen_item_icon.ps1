Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap 16, 16
$gfx = [System.Drawing.Graphics]::FromImage($bmp)
$gfx.Clear([System.Drawing.Color]::Transparent)

# Colors
$cBlack = [System.Drawing.Color]::FromArgb(255, 20, 20, 25)
$cDarkSteel = [System.Drawing.Color]::FromArgb(255, 45, 50, 58)
$cMidSteel = [System.Drawing.Color]::FromArgb(255, 75, 82, 92)
$cLightSteel = [System.Drawing.Color]::FromArgb(255, 120, 130, 142)

$cBrass = [System.Drawing.Color]::FromArgb(255, 212, 160, 48)
$cDarkBrass = [System.Drawing.Color]::FromArgb(255, 145, 100, 25)

$cCopper = [System.Drawing.Color]::FromArgb(255, 195, 100, 60)
$cDarkCopper = [System.Drawing.Color]::FromArgb(255, 130, 60, 35)

$cElectric = [System.Drawing.Color]::FromArgb(255, 80, 210, 255)
$cElectricGlow = [System.Drawing.Color]::FromArgb(255, 200, 245, 255)
$cRedIndicator = [System.Drawing.Color]::FromArgb(255, 230, 60, 50)

# Helper
function P($x, $y, $col) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
        $bmp.SetPixel($x, $y, $col)
    }
}

# 1. Wheels and Track rails at bottom
# Left wheel: (3, 14), (4, 14), (3, 13), (4, 13)
P 3 14 $cDarkSteel; P 4 14 $cDarkSteel; P 3 13 $cBlack; P 4 13 $cLightSteel
# Right wheel: (11, 14), (12, 14), (11, 13), (12, 13)
P 11 14 $cDarkSteel; P 12 14 $cDarkSteel; P 11 13 $cBlack; P 12 13 $cLightSteel

# 2. Chassis Frame (Y=12)
for ($x = 1; $x -le 14; $x++) {
    P $x 12 $cDarkSteel
}
P 2 12 $cLightSteel; P 13 12 $cLightSteel
# Coupler hooks
P 0 12 $cBlack; P 15 12 $cBlack

# 3. Boiler / Engine Body (X: 2 to 9, Y: 7 to 11)
for ($y = 8; $y -le 11; $y++) {
    for ($x = 2; $x -le 9; $x++) {
        P $x $y $cMidSteel
    }
}
# Boiler rounded top (Y=7)
for ($x = 3; $x -le 8; $x++) {
    P $x 7 $cLightSteel
}
# Brass bands on boiler
for ($y = 7; $y -le 11; $y++) {
    P 4 $y $cBrass
    P 7 $y $cBrass
}
# Smokestack / Steam vent
P 3 6 $cDarkSteel; P 3 5 $cDarkBrass; P 2 5 $cDarkBrass; P 4 5 $cDarkBrass

# 4. Dynamo / Turbine Housing (X: 10 to 14, Y: 7 to 11)
for ($y = 8; $y -le 11; $y++) {
    for ($x = 10; $x -le 13; $x++) {
        P $x $y $cCopper
    }
}
# Copper coil ribs
for ($y = 8; $y -le 11; $y++) {
    P 11 $y $cDarkCopper
    P 13 $y $cDarkCopper
}

# 5. High-voltage ceramic insulators and Electric Spark (X: 10 to 13, Y: 3 to 6)
P 11 6 $cLightSteel; P 13 6 $cLightSteel
P 11 5 $cLightSteel; P 13 5 $cLightSteel
P 12 4 $cElectric;   P 12 3 $cElectricGlow
P 11 3 $cElectric;   P 13 4 $cElectric

# Fuel gauge / power lamp on side
P 9 10 $cRedIndicator

$outPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\resources\assets\miningtools\textures\item\generator_cart.png"
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$gfx.Dispose()
$bmp.Dispose()
Write-Host "Generated generator_cart.png icon successfully"
