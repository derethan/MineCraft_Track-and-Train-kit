$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -ReferencedAssemblies "System.Drawing" -Path "scratch/TextureUtils.cs", "scratch/PbrBuffer.cs", "scratch/TextureGenerator.cs"

$standaloneDir = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR"
$zipPath = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR.zip"
$srcAssets = "$PSScriptRoot/../src/main/resources/assets/miningtools"
$curseForgePacks = "C:/Users/swack/curseforge/minecraft/Instances/26.3/resourcepacks"

if (Test-Path $standaloneDir) {
    Remove-Item -Recurse -Force $standaloneDir
}
New-Item -ItemType Directory -Force -Path $standaloneDir | Out-Null

Write-Host "Generating HD textures into: $standaloneDir"
$gen = New-Object TextureGenerator (Resolve-Path $standaloneDir).Path, (Resolve-Path $srcAssets).Path
$gen.GenerateAll()

# Create ZIP archive with FORWARD SLASHES (/) so Minecraft and Java ZipFile can read all entries
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}
Write-Host "Packaging zip with normalized forward-slash entry paths at: $zipPath"

$resolvedDir = (Resolve-Path $standaloneDir).Path
$zipArchive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
$files = Get-ChildItem -Recurse -File $resolvedDir
foreach ($file in $files) {
    $relativePath = $file.FullName.Substring($resolvedDir.Length).TrimStart('\', '/').Replace('\', '/')
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relativePath, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
}
$zipArchive.Dispose()

$count = (Get-ChildItem -Recurse -File $standaloneDir).Count
Write-Host "Successfully generated $count resource pack files!"

# Copy to user's active CurseForge instance
if (Test-Path $curseForgePacks) {
    Write-Host "Deploying updated zip to CurseForge instance: $curseForgePacks"
    Copy-Item -Force $zipPath "$curseForgePacks/Track_and_Train_HD_PBR.zip"
}
