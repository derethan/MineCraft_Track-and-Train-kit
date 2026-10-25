$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\moddev\artifacts\minecraft-patched-26.3.0.48-beta-sources.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$e = $zip.GetEntry('net/minecraft/world/entity/vehicle/minecart/AbstractMinecartContainer.java')
if ($e) {
    $reader = [System.IO.StreamReader]::new($e.Open())
    $content = $reader.ReadToEnd()
    $reader.Close()
    Write-Host $content
}
$zip.Dispose()
