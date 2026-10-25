$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\moddev\artifacts\minecraft-patched-26.3.0.48-beta-sources.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$entry = $zip.GetEntry('net/minecraft/world/entity/vehicle/minecart/AbstractMinecart.java')
if ($entry) {
    $reader = [System.IO.StreamReader]::new($entry.Open())
    $content = $reader.ReadToEnd()
    $reader.Close()
    $lines = $content -split '\r?\n'
    Write-Host "=== Lines 460-480 ==="
    for ($i = 460; $i -lt [Math]::Min(485, $lines.Length); $i++) {
        Write-Host ($i.ToString() + ": " + $lines[$i])
    }
    Write-Host "=== Lines 540-570 ==="
    for ($i = 540; $i -lt [Math]::Min(575, $lines.Length); $i++) {
        Write-Host ($i.ToString() + ": " + $lines[$i])
    }
}
$zip.Dispose()
