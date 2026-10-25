$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\moddev\artifacts\minecraft-patched-26.3.0.48-beta-sources.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
foreach ($e in $zip.Entries) {
    if ($e.FullName.EndsWith(".java") -and $e.FullName -match "minecart") {
        $reader = [System.IO.StreamReader]::new($e.Open())
        $content = $reader.ReadToEnd()
        $reader.Close()
        if ($content -match "applyNaturalSlowdown") {
            Write-Host $e.FullName
        }
    }
}
$zip.Dispose()
