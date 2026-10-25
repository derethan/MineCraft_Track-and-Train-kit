$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\resources"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Write-JsonFile($relPath, $content) {
    $full = Join-Path $root $relPath
    $dir = Split-Path -Parent $full
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($full, $content.Replace("`r`n", "`n"), $utf8NoBom)
}

function Save-StationTextures($prefix, $accentHex, $innerHex, $arrowUp) {
    $texDir = Join-Path $root "assets\miningtools\textures\block"
    $accent = [System.Drawing.ColorTranslator]::FromHtml($accentHex)
    $inner  = [System.Drawing.ColorTranslator]::FromHtml($innerHex)
    $dark   = [System.Drawing.Color]::FromArgb(255, 38, 42, 48)
    $mid    = [System.Drawing.Color]::FromArgb(255, 64, 70, 80)
    $light  = [System.Drawing.Color]::FromArgb(255, 96, 104, 118)
    $rivet  = [System.Drawing.Color]::FromArgb(255, 140, 148, 160)

    # 1. Bottom
    $bmpBottom = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = $mid
            if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $dark }
            elseif (($x -eq 2 -or $x -eq 13) -and ($y -eq 2 -or $y -eq 13)) { $c = $rivet }
            $bmpBottom.SetPixel($x, $y, $c)
        }
    }
    $bmpBottom.Save((Join-Path $texDir "${prefix}_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpBottom.Dispose()

    # 2. Top
    $bmpTop = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = $mid
            if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $dark }
            elseif ($x -ge 3 -and $x -le 12 -and $y -ge 3 -and $y -le 12) {
                if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) { $c = $accent }
                else { $c = $inner }
            }
            $bmpTop.SetPixel($x, $y, $c)
        }
    }
    $bmpTop.Save((Join-Path $texDir "${prefix}_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpTop.Dispose()

    # 3. Side & Front
    foreach ($face in @("side", "front")) {
        $bmp = New-Object System.Drawing.Bitmap(16, 16)
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $c = $mid
                if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $dark }
                elseif ($y -eq 2 -or $y -eq 13) { $c = $accent }
                elseif ($x -ge 4 -and $x -le 11 -and $y -ge 4 -and $y -le 11) {
                    $c = $dark
                    if ($face -eq "front") {
                        if ($arrowUp) {
                            if (($y -eq 5 -and $x -ge 7 -and $x -le 8) -or
                                ($y -eq 6 -and $x -ge 6 -and $x -le 9) -or
                                ($y -eq 7 -and $x -ge 5 -and $x -le 10) -or
                                ($y -ge 8 -and $y -le 10 -and $x -ge 7 -and $x -le 8)) {
                                $c = $accent
                            }
                        } else {
                            if (($y -ge 5 -and $y -le 7 -and $x -ge 7 -and $x -le 8) -or
                                ($y -eq 8 -and $x -ge 5 -and $x -le 10) -or
                                ($y -eq 9 -and $x -ge 6 -and $x -le 9) -or
                                ($y -eq 10 -and $x -ge 7 -and $x -le 8)) {
                                $c = $accent
                            }
                        }
                    } else {
                        if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9) {
                            $c = $inner
                        }
                    }
                }
                $bmp.SetPixel($x, $y, $c)
            }
        }
        $bmp.Save((Join-Path $texDir "${prefix}_${face}.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
    }
}

# Generate distinct 16x16 pixel art textures
# 1. fluid_unloader (Cyan/Amber drain down arrow)
Save-StationTextures "fluid_unloader" "#00ACC1" "#006064" $false
# 2. coal_fueler (Fiery Orange-Gold & Dark Coal firebox up arrow)
Save-StationTextures "coal_fueler" "#FF6F00" "#212121" $true
# 3. diesel_fueler (CN Yellow-Amber & Red diesel pump up arrow)
Save-StationTextures "diesel_fueler" "#FBC02D" "#B71C1C" $true

foreach ($id in @("fluid_unloader", "coal_fueler", "diesel_fueler")) {
    Write-JsonFile "assets\miningtools\blockstates\${id}.json" @"
{
  "variants": {
    "facing=north": { "model": "miningtools:block/${id}", "y": 0 },
    "facing=east":  { "model": "miningtools:block/${id}", "y": 90 },
    "facing=south": { "model": "miningtools:block/${id}", "y": 180 },
    "facing=west":  { "model": "miningtools:block/${id}", "y": 270 }
  }
}
"@

    Write-JsonFile "assets\miningtools\models\block\${id}.json" @"
{
  "parent": "minecraft:block/cube",
  "textures": {
    "particle": "miningtools:block/${id}_side",
    "up": "miningtools:block/${id}_top",
    "down": "miningtools:block/${id}_bottom",
    "north": "miningtools:block/${id}_front",
    "south": "miningtools:block/${id}_side",
    "east": "miningtools:block/${id}_side",
    "west": "miningtools:block/${id}_side"
  }
}
"@

    Write-JsonFile "assets\miningtools\models\item\${id}.json" @"
{
  "parent": "miningtools:block/${id}"
}
"@

    Write-JsonFile "assets\miningtools\items\${id}.json" @"
{
  "model": {
    "type": "minecraft:model",
    "model": "miningtools:item/${id}"
  }
}
"@

    Write-JsonFile "data\miningtools\loot_table\blocks\${id}.json" @"
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
          "name": "miningtools:${id}"
        }
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "miningtools:blocks/${id}"
}
"@
}

# Recipes
Write-JsonFile "data\miningtools\recipe\fluid_unloader.json" @"
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:bucket",
    "C": "minecraft:copper_ingot",
    "H": "minecraft:hopper",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "IBI",
    "CHC",
    "IRI"
  ],
  "result": {
    "count": 1,
    "id": "miningtools:fluid_unloader"
  }
}
"@

Write-JsonFile "data\miningtools\recipe\coal_fueler.json" @"
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "key": {
    "I": "minecraft:iron_ingot",
    "F": "minecraft:blast_furnace",
    "C": "minecraft:coal_block",
    "H": "minecraft:hopper",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "IFI",
    "CHC",
    "IRI"
  ],
  "result": {
    "count": 1,
    "id": "miningtools:coal_fueler"
  }
}
"@

Write-JsonFile "data\miningtools\recipe\diesel_fueler.json" @"
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:bucket",
    "G": "minecraft:gold_ingot",
    "H": "minecraft:hopper",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "IBI",
    "GHG",
    "IRI"
  ],
  "result": {
    "count": 1,
    "id": "miningtools:diesel_fueler"
  }
}
"@

Write-Host "Generated all assets, models, loot tables, and recipes for fluid_unloader, coal_fueler, and diesel_fueler."
