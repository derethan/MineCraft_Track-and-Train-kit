$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\moddev\artifacts\minecraft-patched-26.3.0.48-beta-sources.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$entry = $zip.GetEntry('net/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior.java')
if ($entry) {
    $reader = [System.IO.StreamReader]::new($entry.Open())
    $content = $reader.ReadToEnd()
    $reader.Close()
    $lines = $content -split '\r?\n'
    for ($i = 405; $i -lt 415; $i++) {
        Write-Host ($i.ToString() + ": " + $lines[$i])
    }
}
$zip.Dispose()
