# PowerShell Deploy & Purge Helper for Track and Train Pack
param(
    [string]$ModsDir = "C:\Users\swack\curseforge\minecraft\Instances\26.3\mods"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $scriptDir
$buildLibsDir = Join-Path $projectDir "build\libs"

if (-not (Test-Path $buildLibsDir)) {
    Write-Error "Build directory not found: $buildLibsDir"
    exit 1
}

$latestJar = Get-ChildItem -Path $buildLibsDir -Filter "track and trains-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $latestJar) {
    Write-Error "No 'track and trains-*.jar' found in $buildLibsDir. Run './gradlew build' first."
    exit 1
}

Write-Host "Found newest mod jar: $($latestJar.Name) ($($latestJar.Length) bytes)" -ForegroundColor Cyan

if (-not (Test-Path $ModsDir)) {
    Write-Error "Target mods directory not found: $ModsDir"
    exit 1
}

# Purge all old track and trains jars in the target mods directory
$existingJars = Get-ChildItem -Path $ModsDir -Filter "*track and trains*.jar"
foreach ($jar in $existingJars) {
    Write-Host "Purging existing jar: $($jar.Name)" -ForegroundColor Yellow
    Remove-Item -Path $jar.FullName -Force
}

# Also check for any miningtools*.jar just in case
$legacyJars = Get-ChildItem -Path $ModsDir -Filter "*miningtools*.jar"
foreach ($jar in $legacyJars) {
    Write-Host "Purging legacy jar: $($jar.Name)" -ForegroundColor Yellow
    Remove-Item -Path $jar.FullName -Force
}

# Copy the newest jar
$destPath = Join-Path $ModsDir $latestJar.Name
Copy-Item -Path $latestJar.FullName -Destination $destPath -Force
Write-Host "Copied $($latestJar.Name) to $ModsDir" -ForegroundColor Green

Write-Host "Auto-purge and update completed successfully!" -ForegroundColor Green
