Add-Type -AssemblyName System.Drawing

$cleanRailPath = "c:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\scratch_clean_rail.png"
if (-not (Test-Path $cleanRailPath)) {
    Write-Error "Clean rail not found"
    exit 1
}

$baseBmp = [System.Drawing.Bitmap]::FromFile($cleanRailPath)
# Base rail is 97x55
Write-Output "Loaded base rail: $($baseBmp.Width)x$($baseBmp.Height)"
$baseBmp.Dispose()
