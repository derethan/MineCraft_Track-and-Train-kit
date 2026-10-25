$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\libs\miningtools-1.0.1.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
foreach ($name in @('data/miningtools/recipe/remote_detonator.json', 'data/miningtools/recipe/surveyors_charge.json', 'data/miningtools/recipe/cargo_minecart.json')) {
    $entry = $zip.GetEntry($name)
    if ($entry) {
        Write-Host "=== ENTRY: $name"
        $stream = $entry.Open()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host $reader.ReadToEnd()
        $reader.Close()
    } else {
        Write-Host "=== NOT FOUND: $name"
    }
}
$zip.Dispose()
