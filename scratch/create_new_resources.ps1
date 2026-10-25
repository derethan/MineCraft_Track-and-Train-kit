$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Drawing

$resRoot = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\resources"

# 1. Blockstates
$brakingBs = [System.IO.File]::ReadAllText("$resRoot\assets\miningtools\blockstates\control_rail.json", $utf8NoBom)
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\blockstates\braking_rail.json", $brakingBs, $utf8NoBom)

$cargoLoaderBs = @'
{
  "variants": {
    "facing=north": { "model": "miningtools:block/cargo_loader", "y": 0 },
    "facing=east":  { "model": "miningtools:block/cargo_loader", "y": 90 },
    "facing=south": { "model": "miningtools:block/cargo_loader", "y": 180 },
    "facing=west":  { "model": "miningtools:block/cargo_loader", "y": 270 }
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\blockstates\cargo_loader.json", $cargoLoaderBs, $utf8NoBom)

$cargoUnloaderBs = @'
{
  "variants": {
    "facing=north": { "model": "miningtools:block/cargo_unloader", "y": 0 },
    "facing=east":  { "model": "miningtools:block/cargo_unloader", "y": 90 },
    "facing=south": { "model": "miningtools:block/cargo_unloader", "y": 180 },
    "facing=west":  { "model": "miningtools:block/cargo_unloader", "y": 270 }
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\blockstates\cargo_unloader.json", $cargoUnloaderBs, $utf8NoBom)

# 2. Block Models for cargo_loader & cargo_unloader
$cargoLoaderModel = @'
{
  "parent": "minecraft:block/cube",
  "textures": {
    "particle": "miningtools:block/cargo_loader_side",
    "up": "miningtools:block/cargo_loader_top",
    "down": "miningtools:block/cargo_loader_bottom",
    "north": "miningtools:block/cargo_loader_front",
    "south": "miningtools:block/cargo_loader_side",
    "east": "miningtools:block/cargo_loader_side",
    "west": "miningtools:block/cargo_loader_side"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\models\block\cargo_loader.json", $cargoLoaderModel, $utf8NoBom)

$cargoUnloaderModel = @'
{
  "parent": "minecraft:block/cube",
  "textures": {
    "particle": "miningtools:block/cargo_unloader_side",
    "up": "miningtools:block/cargo_unloader_top",
    "down": "miningtools:block/cargo_unloader_bottom",
    "north": "miningtools:block/cargo_unloader_front",
    "south": "miningtools:block/cargo_unloader_side",
    "east": "miningtools:block/cargo_unloader_side",
    "west": "miningtools:block/cargo_unloader_side"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\models\block\cargo_unloader.json", $cargoUnloaderModel, $utf8NoBom)

# 3. Item Models & 1.21.4+ Client Items JSONs
$brakingItemModel = @'
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "miningtools:block/braking_rail"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\models\item\braking_rail.json", $brakingItemModel, $utf8NoBom)

$cargoLoaderItemModel = @'
{
  "parent": "miningtools:block/cargo_loader"
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\models\item\cargo_loader.json", $cargoLoaderItemModel, $utf8NoBom)

$cargoUnloaderItemModel = @'
{
  "parent": "miningtools:block/cargo_unloader"
}
'@
[System.IO.File]::WriteAllText("$resRoot\assets\miningtools\models\item\cargo_unloader.json", $cargoUnloaderItemModel, $utf8NoBom)

foreach ($name in @("braking_rail", "cargo_loader", "cargo_unloader")) {
    $clientItem = @"
{
  "model": {
    "type": "minecraft:model",
    "model": "miningtools:item/$name"
  }
}
"@
    [System.IO.File]::WriteAllText("$resRoot\assets\miningtools\items\$name.json", $clientItem, $utf8NoBom)
}

# 4. Generate 16x16 Block Textures for braking_rail, cargo_loader, and cargo_unloader
$texDir = "$resRoot\assets\miningtools\textures\block"

# Copy control_rail item texture and tint brake shoes crimson-amber for braking_rail.png
$ctrlBmp = New-Object System.Drawing.Bitmap("$texDir\control_rail.png")
$brakeBmp = New-Object System.Drawing.Bitmap(16, 16)
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = $ctrlBmp.GetPixel($x, $y)
        if ($x -in @(4, 5, 10, 11) -and $y -in @(3, 4, 11, 12) -and $c.A -gt 0) {
            # Brake shoe clamp pads along inner rail
            $brakeBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 210, 65, 30))
        } else {
            $brakeBmp.SetPixel($x, $y, $c)
        }
    }
}
$brakeBmp.Save("$texDir\braking_rail.png", [System.Drawing.Imaging.ImageFormat]::Png)
$ctrlBmp.Dispose()
$brakeBmp.Dispose()

function New-StationTexture([string]$path, [string]$role, [bool]$isUnloader) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    $baseDark = [System.Drawing.Color]::FromArgb(255, 42, 46, 54)
    $baseMid  = [System.Drawing.Color]::FromArgb(255, 58, 64, 74)
    $border   = [System.Drawing.Color]::FromArgb(255, 28, 31, 36)
    $accent   = if ($isUnloader) { [System.Drawing.Color]::FromArgb(255, 235, 145, 25) } else { [System.Drawing.Color]::FromArgb(255, 35, 185, 120) }
    $chute    = [System.Drawing.Color]::FromArgb(255, 22, 24, 28)

    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) {
                $bmp.SetPixel($x, $y, $border)
            } elseif (($x + $y) % 2 -eq 0) {
                $bmp.SetPixel($x, $y, $baseMid)
            } else {
                $bmp.SetPixel($x, $y, $baseDark)
            }
        }
    }

    if ($role -eq "top") {
        # Hopper chute grate on top face
        for ($y = 3; $y -le 12; $y++) {
            for ($x = 3; $x -le 12; $x++) {
                if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) {
                    $bmp.SetPixel($x, $y, $accent)
                } elseif ($x -in @(6, 9) -or $y -in @(6, 9)) {
                    $bmp.SetPixel($x, $y, $baseMid)
                } else {
                    $bmp.SetPixel($x, $y, $chute)
                }
            }
        }
    } elseif ($role -eq "front" -or $role -eq "side") {
        # Hazard/status stripe & directional arrow
        for ($x = 2; $x -le 13; $x++) {
            $bmp.SetPixel($x, 2, $accent)
            $bmp.SetPixel($x, 13, $accent)
        }
        # Center viewport / chute indicator
        for ($y = 5; $y -le 10; $y++) {
            for ($x = 5; $x -le 10; $x++) {
                $bmp.SetPixel($x, $y, $chute)
            }
        }
        # Arrow in center (up arrow for Loader, down arrow for Unloader)
        if ($isUnloader) {
            $bmp.SetPixel(7, 6, $accent); $bmp.SetPixel(8, 6, $accent)
            $bmp.SetPixel(7, 7, $accent); $bmp.SetPixel(8, 7, $accent)
            $bmp.SetPixel(6, 8, $accent); $bmp.SetPixel(7, 8, $accent); $bmp.SetPixel(8, 8, $accent); $bmp.SetPixel(9, 8, $accent)
            $bmp.SetPixel(7, 9, $accent); $bmp.SetPixel(8, 9, $accent)
        } else {
            $bmp.SetPixel(7, 6, $accent); $bmp.SetPixel(8, 6, $accent)
            $bmp.SetPixel(6, 7, $accent); $bmp.SetPixel(7, 7, $accent); $bmp.SetPixel(8, 7, $accent); $bmp.SetPixel(9, 7, $accent)
            $bmp.SetPixel(7, 8, $accent); $bmp.SetPixel(8, 8, $accent)
            $bmp.SetPixel(7, 9, $accent); $bmp.SetPixel(8, 9, $accent)
        }
    } elseif ($role -eq "bottom") {
        for ($y = 4; $y -le 11; $y++) {
            for ($x = 4; $x -le 11; $x++) {
                $bmp.SetPixel($x, $y, $chute)
            }
        }
    }

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

foreach ($r in @("top", "front", "side", "bottom")) {
    New-StationTexture "$texDir\cargo_loader_$r.png" $r $false
    New-StationTexture "$texDir\cargo_unloader_$r.png" $r $true
}

# 5. Loot Tables
foreach ($name in @("braking_rail", "cargo_loader", "cargo_unloader")) {
    $lt = @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ],
      "entries": [
        {
          "type": "minecraft:item",
          "name": "miningtools:$name"
        }
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "miningtools:blocks/$name"
}
"@
    [System.IO.File]::WriteAllText("$resRoot\data\miningtools\loot_table\blocks\$name.json", $lt, $utf8NoBom)
}

# 6. Recipes (1.21.4+ format)
$brakingRecipe = @'
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "I": "minecraft:iron_ingot",
    "S": "minecraft:stick",
    "R": "minecraft:redstone",
    "C": "minecraft:cobblestone"
  },
  "pattern": [
    "ICI",
    "ISI",
    "IRI"
  ],
  "result": {
    "count": 6,
    "id": "miningtools:braking_rail"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\data\miningtools\recipe\braking_rail.json", $brakingRecipe, $utf8NoBom)

$cargoLoaderRecipe = @'
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "key": {
    "I": "minecraft:iron_ingot",
    "H": "minecraft:hopper",
    "C": "minecraft:chest",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "IHI",
    "ICI",
    "IRI"
  ],
  "result": {
    "count": 1,
    "id": "miningtools:cargo_loader"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\data\miningtools\recipe\cargo_loader.json", $cargoLoaderRecipe, $utf8NoBom)

$cargoUnloaderRecipe = @'
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "key": {
    "I": "minecraft:iron_ingot",
    "H": "minecraft:hopper",
    "C": "minecraft:chest",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "ICI",
    "IHI",
    "IRI"
  ],
  "result": {
    "count": 1,
    "id": "miningtools:cargo_unloader"
  }
}
'@
[System.IO.File]::WriteAllText("$resRoot\data\miningtools\recipe\cargo_unloader.json", $cargoUnloaderRecipe, $utf8NoBom)

# 7. Update Tags (rails.json & mineable/pickaxe.json)
$railsTag = @'
{
  "replace": false,
  "values": [
    "miningtools:junction_rail",
    "miningtools:control_rail",
    "miningtools:braking_rail",
    "miningtools:horn_rail",
    "miningtools:turnout_rail",
    "miningtools:wye_rail",
    "miningtools:electric_rail"
  ]
}
'@
[System.IO.File]::WriteAllText("$resRoot\data\minecraft\tags\block\rails.json", $railsTag, $utf8NoBom)

$pickaxeTag = @'
{
  "replace": false,
  "values": [
    "miningtools:junction_rail",
    "miningtools:control_rail",
    "miningtools:braking_rail",
    "miningtools:horn_rail",
    "miningtools:turnout_rail",
    "miningtools:wye_rail",
    "miningtools:track_switch",
    "miningtools:cargo_loader",
    "miningtools:cargo_unloader",
    "miningtools:fluid_loader",
    "miningtools:electric_rail",
    "miningtools:entity_loader",
    "miningtools:entity_unloader",
    "miningtools:chunk_loader",
    "miningtools:buffer_stop"
  ]
}
'@
[System.IO.File]::WriteAllText("$resRoot\data\minecraft\tags\block\mineable\pickaxe.json", $pickaxeTag, $utf8NoBom)

# 8. Update en_us.json
$langPath = "$resRoot\assets\miningtools\lang\en_us.json"
$langContent = [System.IO.File]::ReadAllText($langPath, $utf8NoBom)
if (-not $langContent.Contains("block.miningtools.braking_rail")) {
    $newEntries = @'
  "container.miningtools.hopper_cart": "Aggregate Hopper Car",
  "block.miningtools.braking_rail": "Braking & Holding Rail",
  "item.miningtools.braking_rail": "Braking & Holding Rail",
  "block.miningtools.cargo_loader": "Trackside Cargo Loader",
  "item.miningtools.cargo_loader": "Trackside Cargo Loader",
  "block.miningtools.cargo_unloader": "Trackside Cargo Unloader",
  "item.miningtools.cargo_unloader": "Trackside Cargo Unloader"
}
'@
    $langContent = $langContent.Replace('  "container.miningtools.hopper_cart": "Aggregate Hopper Car"' + "`r`n}", $newEntries)
    $langContent = $langContent.Replace('  "container.miningtools.hopper_cart": "Aggregate Hopper Car"' + "`n}", $newEntries)
    [System.IO.File]::WriteAllText($langPath, $langContent, $utf8NoBom)
}
Write-Host "Created all new block/item resources, textures, recipes, loot tables, tags, and lang entries!"
