$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -ReferencedAssemblies "System.Drawing" -Path "scratch/TextureUtils.cs", "scratch/True2kPbrEngine.cs"

$standaloneDir = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR"
$zipPath = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR.zip"
$srcAssets = "$PSScriptRoot/../src/main/resources/assets/miningtools"
$curseForgePacks = "C:/Users/swack/curseforge/minecraft/Instances/26.3/resourcepacks"

if (Test-Path $standaloneDir) {
    Remove-Item -Recurse -Force $standaloneDir
}
New-Item -ItemType Directory -Force -Path $standaloneDir | Out-Null

Write-Host "Synthesizing 2K Super HD LabPBR Textures at: $standaloneDir"
$engine = New-Object True2kPbrEngine (Resolve-Path $srcAssets).Path, (Resolve-Path $standaloneDir).Path
$engine.Generate2kPack()

# Create clean ZIP with forward-slash paths
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}
Write-Host "Creating clean 2K zip archive at: $zipPath"

$resolvedDir = (Resolve-Path $standaloneDir).Path
$zipArchive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
$files = Get-ChildItem -Recurse -File $resolvedDir
foreach ($file in $files) {
    $relativePath = $file.FullName.Substring($resolvedDir.Length).TrimStart('\', '/').Replace('\', '/')
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relativePath, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
}
$zipArchive.Dispose()

$count = (Get-ChildItem -Recurse -File $standaloneDir).Count
Write-Host "Generated $count files in 2K resource pack!"

# Copy directly to active CurseForge instance
if (Test-Path $curseForgePacks) {
    Write-Host "Deploying 2K pack to CurseForge instance: $curseForgePacks/Track_and_Train_HD_PBR.zip"
    Copy-Item -Force $zipPath "$curseForgePacks/Track_and_Train_HD_PBR.zip"
}

Write-Host "2K Deployment complete!"
