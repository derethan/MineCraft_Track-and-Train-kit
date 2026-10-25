$blueprint = @'
# COMPRESSED TECHNICAL BLUEPRINT: Track-and-Train-Kit (Mining Tools)
# Save State for Antigravity Session Resumption

## 1. Project Metadata & Environment
- **Mod Name / ID**: miningtools ("MineCraft Track-and-Train kit")
- **Mod Version**: 1.0.1
- **Platform**: NeoForge (Minecraft 26.3 / 1.21.4 Mojang mappings)
- **Repository Root**: C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit
- **JDK Path**: C:\Users\swack\AppData\Local\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon
- **Build Command**:
  cmd /c "set JAVA_HOME=C:\Users\swack\AppData\Local\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon&& cd /d C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit&& gradlew.bat jar --no-daemon"
- **Deploy Destinations**:
  1. C:\Users\swack\curseforge\minecraft\Instances\26.3\mods\track and trains-1.0.1.jar
  2. C:\Users\swack\Desktop\New folder (2)\track and trains-1.0.1.jar

---

## 2. Critical Developer Guidelines & Gotchas
1. **Always Check and Correct User Spelling**: Maintain strict spelling adherence in responses and documentation (e.g. "Surveyor's Charge", "Remote Detonator", "borehole", "loose", "through", "connect", "each other", "radius", "column", "bedrock", "signal", etc.).
2. **UTF-8 WITHOUT BOM**: javac fails with illegal character: '\ufeff' if files contain a UTF-8 BOM. When generating or editing Java files in PowerShell, always use [System.Text.UTF8Encoding]::new($false).
3. **PowerShell Variable Expansion**: Do not pass complex inline scripts containing $ or double quotes via cmd /c powershell -Command "..." because Windows CLI strips $. Always save scripts into scratch/*.ps1 and run via powershell -ExecutionPolicy Bypass -File "path".
4. **Recipe Categories & Formats**: Vanilla 1.21.4 CraftingBookCategory enum only accepts "building", "equipment", "misc", or "redstone". Setting category to "transportation" crashes world loading. Shapeless recipes MUST use an "ingredients" list with 1-9 elements.
5. **Documentation Sync**: When making custom content changes, always keep CHANGELOG.md, WIKI.md, README.md, CURSEFORGE.md, and conversation walkthroughs updated.

---

## 3. Fleet & Rolling Stock Architecture

### Locomotives (All with Solid Collision, 1 m/s Crawl Gear 1, Headlights, Reverse)
- **Steam Locomotive (BasicLocomotiveEntity)**:
  - Fuel: Coal/charcoal/burnables. 5 gears + reverse. Crawl speed: 0.05 b/t (1.0 m/s). Top speed: 0.35 b/t (~7 m/s).
  - Train whistle (scares mobs), headlight block light level 15, HUD speedometer, reverse toggle without flipping entity model.
- **Diesel Locomotive (DieselLocomotiveEntity)**:
  - Canadian National #3087 road diesel model.
  - Fuel: 16,000 mB internal tank (Diesel, biodiesel, kerosene, petrol, oil tags; lava fallback).
  - Automated Fuel Tender Siphoning: Automatically detects coupled Liquid Minecarts or Large Liquid Minecarts and siphons up to 100 mB/tick to stay fueled.
  - Nathan horn sound, 5 gears + reverse (1.0 m/s crawl), pulls 10 cars baseline before drag.
- **Electric Locomotive (ElectricLocomotiveEntity)**:
  - High-speed bullet train streamliner.
  - Power: 100,000 FE internal battery. Automatically charges on-the-fly when on ElectricRailBlock.
  - Top speed: 1.75 b/t (35 m/s, 126 km/h). 1.0 m/s crawl gear 1. Pulls 10 cars.

### Rolling Stock & Specialized Carts
- **Loose Aggregate Hopper Car (AggregateHopperCartEntity)**:
  - Real-world ballast hopper car modeled after TIMX/Herzog hoppers.
  - Dynamic visual aggregate heaps: Coal mounds, Gravel/ballast heaps, yellow Sand heaps, Red Sand heaps, Dirt/clay heaps, and raw metal Ore heaps.
  - Renders empty hollow container with sloped interior bulkheads when emptied.
  - 27 general storage slots + 1 dedicated filter slot with custom hopper funnel icon.
  - Track-aligned yaw on placement.
- **Arrow Turret Cart (TurretCartEntity)**:
  - Armored wagon chassis, rotating turret mount. 2 bow slots + 9 arrow slots.
  - Defends train: scans 16-block radius for hostile mobs (Enemy, alive, line of sight, not train passenger), damages bows by 1 durability, consumes arrows, disallowed pickup.
- **Cargo Minecart (CargoMinecartEntity)**:
  - 27-slot freight wagon + 1 Filter slot. Adaptive 3D model: Logs (stakes), Container (enclosed shipping box), Ore (mounded raw ore), Ingots (stacked bars).
- **Livestock Cart (LivestockCartEntity)**:
  - Scale railroad stock car with slatted wooden walls. Dual-stall capacity for 2 living entities (animals/villagers). Leash right-click hitching.
- **Tank Cars**:
  - Short Tanker (LiquidMinecartEntity - 8,000 mB).
  - Heavy DOT-111 Tanker (LargeLiquidMinecartEntity - 16,000 mB).
  - Directly couples to Diesel Locomotive as an automated fuel tender wagon.
- **Caboose (CabooseMinecartEntity)**:
  - Rear marker lanterns, cupola observation deck, onboard active 3x3 chunk loader.
  - Universal placement: can be placed at front, middle, or rear of consist.
- **Tunnel Bore (TunnelBoreEntity)**:
  - Massive multi-block front excavator with rotating 3x3 cutter head.
  - Multi-inventory: Fuel, Rails, and Gravel/Sand bridge builder inventory (places gravel/sand to bridge air gaps when digging).

---

## 4. Train Chaining & Physics Engine (TrainChainingUtil.java)
- **Railroad Crowbar**: Right-click cart A then cart B to couple with heavy steel chain. Shift+Right-Click to uncouple.
- **Coupler Radiuses**: Heavy Diesel (1.18m), Tanker DOT-111 (1.12m), Hopper Cart (0.92m), Caboose (0.95m), Livestock/Turret/Cargo/Electric (0.88m), Steam (0.78m), Small Tanker (0.75m), Vanilla (0.50m).
- **Universal Consist Kinematics (Locomotive-Free Coupling)**:
  - Rolling stock can couple into consists without requiring a locomotive.
  - TrainChainingUtil.tickConsistMovement(this) synchronizes all unpowered linked cars once per tick: pushing any cart by hand or rolling via gravity moves the entire consist at exact coupler spacing.
- **Minecart-to-Minecart Bumper Collision (Anti-Phasing)**:
  - TrainChainingUtil.pushMinecarts: unlinked carts colliding on rails make physical contact at their coupler radii, separate cleanly, and transfer along-track momentum.
  - Bypasses vanilla's angle-dot-product cancellation bug (vec3.dot(vec31) < 0.8), ensuring loose carts never phase through each other.
- **Continuous Slope Traversal & Elevation Interpolation**:
  - Height interpolation continuously tracks slope elevation without artificial hops or ceiling clipping.
  - Unlinked rolling stock restores vanilla player interaction and momentum transfer so players can easily push cars by hand.

---

## 5. Track Infrastructure & Switch Rails
- **Turnout Rail**: Animated switch blade points toggled via redstone or crowbar.
- **Wye Rail**: 3-way Y junction with active branch visual indicator.
- **Junction Rail**: 4-way perpendicular (+) crossing rail.
- **Control Rail**: Redstone-powered speed boost and brake.
- **Horn Rail**: Triggers authentic locomotive horn/whistle when crossed.
- **Buffer Stop (Stop Block)**: Clamps to rail, timber beam + rubber bumper pads, safely halts trains, auto-cuts engine, outputs redstone power 15 while cart rests against it.
- **Automation Blocks**: Trackside Entity Loader, Entity Unloader, Fluid Station, Signal Switch, Industrial Chunk Anchor.

---

## 6. Geological Prospecting & Survey System
- **Surveyor's Charge (SurveyorsChargeBlock)**:
  - **Crafting Recipe**: 1 Redstone Dust + 1 TNT + 1 Arrow (shapeless, "redstone" category).
  - **Detonation Trigger**: Triggered by redstone signal or wirelessly via Remote Detonator.
  - **Borehole Excavation**: Blasts a clean 1x1 vertical borehole straight down from its position until hitting bedrock or indestructible blocks (destroySpeed < 0.0F). Configurable block dropping.
  - **Geological Survey Scan**: Scans a cylindrical column defined by the horizontal radius (default 10 blocks) extending from the charge's placement level all the way down to bedrock (dimension minY). Tallies mineral/block quantities sorted descending.
  - **Oil Deposit Detection**: Automatically detects and tallies crude oil / fuel fluids (#c:oil, #c:crude_oil, registry path match) and oil blocks, highlighting total deposits and source reservoirs in gold.
  - **Config Options (Config.java)**:
    - radius: Scan radius in blocks (default 10, range 1 to 32).
    - dropBoreholeBlocks: Whether blasted borehole blocks drop as items (default false).
    - ignoredBlocks: Configurable list of ignored common blocks (stone, dirt, deepslate, gravel, sand, lava, etc.).
    - extraReportBlocks: Configurable list of custom/modded blocks to explicitly force-include in survey reports.
    - remoteDetonatorRange: Maximum remote detonation range (default 12.0 blocks).
  - **Report Delivery**: Outputs a formatted geological report directly to the triggering player in chat.
- **Remote Detonator (RemoteDetonatorItem)**:
  - **Crafting Recipe**: 1 Redstone Dust + 1 Lever + 1 Iron Ingot (shapeless, "redstone" category).
  - **Operation**: Right-click to detonate all Surveyor's Charges within 12 blocks (remoteDetonatorRange), immediately streaming geological survey reports back to the player.

---

## 7. Build, Packaging & Verification
- Compile command: gradlew.bat jar --no-daemon
- Verified: Recipe JSONs conform to 1.21.4 Mojang codec requirements.
- Deployed to active CurseForge instance and Desktop mod folder.
'@

$blueprintPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\mod_blueprint.txt"
[System.IO.File]::WriteAllText($blueprintPath, $blueprint, [System.Text.UTF8Encoding]::new($false))
Write-Host "mod_blueprint.txt updated successfully."
