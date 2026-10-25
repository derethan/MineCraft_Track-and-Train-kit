$jar = 'C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\moddev\artifacts\minecraft-patched-26.3.0.48-beta-sources.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
foreach ($name in @('net/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior.java', 'net/minecraft/world/entity/vehicle/minecart/NewMinecartBehavior.java')) {
    $e = $zip.GetEntry($name)
    if ($e) {
        $reader = [System.IO.StreamReader]::new($e.Open())
        $content = $reader.ReadToEnd()
        $reader.Close()
        $lines = $content -split '\r?\n'
        for ($i = 0; $i -lt $lines.Length; $i++) {
            if ($lines[$i] -match 'applyNaturalSlowdown') {
                Write-Host ($name + " Line " + ($i+1) + ": " + $lines[$i])
            }
        }
    }
}
$zip.Dispose()
