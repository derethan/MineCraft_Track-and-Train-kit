Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('C:\Users\swack\curseforge\minecraft\Instances\26.3\resourcepacks\Track_and_Train_HD_PBR.zip')
Write-Host "Total entries in zip: $($zip.Entries.Count)"
$zip.Entries | Where-Object { 
    $_.FullName -like '*diesel*' -or 
    $_.FullName -like '*electric*' -or 
    $_.FullName -like '*caboose*' -or 
    $_.FullName -like '*liquid*' -or 
    $_.FullName -like '*generator*' -or 
    $_.FullName -like '*turret*' -or 
    $_.FullName -like '*hopper*' -or 
    $_.FullName -like '*container*'
} | Select-Object FullName, Length | Format-Table -AutoSize
$zip.Dispose()
