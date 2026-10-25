$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -ReferencedAssemblies "System.Drawing" -Path "scratch/TextureUtils.cs", "scratch/GenerateTrueHdLabPbr.cs"

$standaloneDir = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR"
$zipPath = "$PSScriptRoot/../resourcepacks/Track_and_Train_HD_PBR.zip"
$srcAssets = "$PSScriptRoot/../src/main/resources/assets/miningtools"
$curseForgePacks = "C:/Users/swack/curseforge/minecraft/Instances/26.3/resourcepacks"

if (Test-Path $standaloneDir) {
    Remove-Item -Recurse -Force $standaloneDir
}
New-Item -ItemType Directory -Force -Path $standaloneDir | Out-Null

Write-Host "Processing authentic textures into HD LabPBR 1.3 at: $standaloneDir"
$gen = New-Object TrueHdLabPbrGenerator (Resolve-Path $srcAssets).Path, (Resolve-Path $standaloneDir).Path
$gen.ProcessAll()

# Create clean ZIP with forward-slash paths
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}
Write-Host "Creating clean zip archive at: $zipPath"

$resolvedDir = (Resolve-Path $standaloneDir).Path
$zipArchive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
$files = Get-ChildItem -Recurse -File $resolvedDir
foreach ($file in $files) {
    $relativePath = $file.FullName.Substring($resolvedDir.Length).TrimStart('\', '/').Replace('\', '/')
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relativePath, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
}
$zipArchive.Dispose()

$count = (Get-ChildItem -Recurse -File $standaloneDir).Count
Write-Host "Generated $count files in resource pack!"

# Copy directly to active CurseForge instance
if (Test-Path $curseForgePacks) {
    Write-Host "Deploying to CurseForge instance: $curseForgePacks/Track_and_Train_HD_PBR.zip"
    Copy-Item -Force $zipPath "$curseForgePacks/Track_and_Train_HD_PBR.zip"
}

Write-Host "Deployment complete!"
