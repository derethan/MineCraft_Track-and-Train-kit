$javaHome = "C:\Users\swack\AppData\Local\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon"
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;" + $env:Path

Set-Location "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit"

Write-Host "Running Gradle clean jar..."
& .\gradlew.bat clean jar --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
    exit $LASTEXITCODE
}

$builtJar = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\libs\miningtools-1.0.1.jar"
Write-Host "Verifying contents of $builtJar..."

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($builtJar)
foreach ($name in @('data/miningtools/recipe/remote_detonator.json', 'data/miningtools/recipe/surveyors_charge.json')) {
    $entry = $zip.GetEntry($name)
    if ($entry) {
        Write-Host "=== VERIFIED ENTRY: $name ==="
        $stream = $entry.Open()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host $reader.ReadToEnd()
        $reader.Close()
    } else {
        Write-Error "ENTRY MISSING: $name"
    }
}
$zip.Dispose()

$dest1 = "C:\Users\swack\curseforge\minecraft\Instances\26.3\mods\track and trains-1.0.1.jar"
$dest2 = "C:\Users\swack\Desktop\New folder (2)\track and trains-1.0.1.jar"
$dest3 = "C:\Users\swack\Desktop\track and trains-1.0.1.jar"

Copy-Item -Path $builtJar -Destination $dest1 -Force
Write-Host "Copied to $dest1"
Copy-Item -Path $builtJar -Destination $dest2 -Force
Write-Host "Copied to $dest2"
Copy-Item -Path $builtJar -Destination $dest3 -Force
Write-Host "Copied to $dest3"

Write-Host "Rebuild, verification, and deployment SUCCESSFUL!"
