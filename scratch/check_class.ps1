$classFile = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\classes\java\main\com\example\miningtools\util\TrainChainingUtil.class'
$bytes = [System.IO.File]::ReadAllBytes($classFile)
$str = [System.Text.Encoding]::ASCII.GetString($bytes)
if ($str.Contains('applyRollingStockSlowdown')) {
    Write-Host 'CLASS CONTAINS applyRollingStockSlowdown'
} else {
    Write-Host 'CLASS DOES NOT CONTAIN applyRollingStockSlowdown'
}
