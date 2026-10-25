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

$builtJar = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\build\libs\track and trains-1.0.1.jar"
if (-not (Test-Path $builtJar)) {
    $builtJar = (Get-ChildItem -Path "build\libs" -Filter "*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
}

Write-Host "Built JAR found at: $builtJar"

$dest1 = "C:\Users\swack\curseforge\minecraft\Instances\26.3\mods\track and trains-1.0.1.jar"
$dest2 = "C:\Users\swack\Desktop\New folder (2)\track and trains-1.0.1.jar"
$dest3 = "C:\Users\swack\Desktop\track and trains-1.0.1.jar"

if (Test-Path "C:\Users\swack\curseforge\minecraft\Instances\26.3\mods") {
    Copy-Item -Path $builtJar -Destination $dest1 -Force
    Write-Host "Deployed to $dest1"
}

if (Test-Path "C:\Users\swack\Desktop\New folder (2)") {
    Copy-Item -Path $builtJar -Destination $dest2 -Force
    Write-Host "Deployed to $dest2"
}

Copy-Item -Path $builtJar -Destination $dest3 -Force
Write-Host "Deployed to $dest3"

Write-Host "Deployment complete."
