# Changelog

All notable changes to the Track and Train Pack mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.35] - 2026-11-06

### Changed
- **Renamed to Under Track Paver**:
  - Renamed the paver machine to **Under Track Paver** across item name, entity name, container title, screen title validation, and in-game action bar messages (`message.miningtools.track_paver.*`).
  - Updated the Railroader's Field Manual entries and recipes to reflect the new **Under Track Paver** title and description.
- **Turnout Rail (Right) Crafting Recipe Update**:
  - Re-aligned the crafting recipe for Turnout Rail (Right) so the redstone lever is placed on the middle-right instead of center:
    - Top row: Rail, Blank, Rail
    - Middle row: Blank, Blank, Lever
    - Bottom row: Rail, Blank, Rail
  - Symmetrically matches the Turnout Rail (Left) recipe layout (which positions the lever on the left).

---

## [1.1.34] - 2026-11-05

### Changed
- Mod update

---

## [1.1.34] - 2026-11-05

### Changed
- **Road Paver Rail-Only Operation & Automatic Track Navigation**:
  - Converted the Heavy Track & Road Paver to operate exclusively as a rail cart, aligning with the rail-riding mechanics of the Tunnel Bore without mining terrain blocks or laying rails.
  - Restricted placement to railway tracks (`TrackPaverItem` places exclusively on rails, snapping directly onto the rail centerline with orientation aligned to track layout).
  - Added continuous rail following: smoothly follows straight rails, ascending/descending slopes, and curved tracks in both Forward and Reverse modes, halting safely if an obstacle blocks the path or upon reaching the end of the line.
  - Continues paving the 1-deep by 3-wide foundation strip (center block beneath the rail and left/right shoulder blocks) while preserving all tracks and ties intact.
  - Enhanced train consist reverse handling: added `pushTrailingCartsOnReverse` to prevent trailing railcars from bunching up when the paver reverses, and registered the paver's coupler radius (1.45m) in `TrainChainingUtil`.
  - Streamlined paver GUI: replaced the obsolete off-rail Cardinal Lock button with a full-width **Hologram Guide** button in `TrackPaverScreen` that projects the 3x3 paving footprint along the actual track route.

---

## [1.1.32] - 2026-11-04

### Changed
- **Reference-Based 3D Pixel-Art Item Icon Overhaul for All Rails**:
  - Overhauled all 11 rail item icons across the mod using the user-provided 2:1 isometric pixel-art rail baseline, featuring rich wooden sleeper ties with end-grain depth, dark hex tie spikes, and raised steel rail heads with specular glints and shaded web profiles.
  - Implemented bespoke visual apparatus and configurations for every single rail type:
    - **Junction Rail** ([`junction_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/junction_rail.png)): Perpendicular 4-way diamond cross intersection with central crossing frogs, flangeway wheel clearances, unified sleeper foundation, and internal corner guard rails.
    - **Turnout Rail (Right)** ([`turnout_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/turnout_rail.png)): Straight mainline track with diverging right-hand branch curve, tapered switch blade points, switch tie-rod, grounded head-block tie extension, and a classic railroad ground switch stand with green rightward directional arrow.
    - **Turnout Rail (Left)** ([`left_turnout_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/left_turnout_rail.png)): Straight mainline track with diverging left-hand branch curve, switch blade points, tie-rod, extended sleeper tie, and a grounded railroad switch stand with green leftward directional arrow.
    - **Wye Rail** ([`wye_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/wye_rail.png)): True 3-way equilateral Y-junction splitting an incoming line into symmetrical diverging curves with central crossing frog and glowing redstone directional Y-lantern indicator.
    - **Locomotive Control Rail** ([`control_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/control_rail.png)): Centered golden power plates with a dispatch activator casing and glowing redstone crystal.
    - **Electric Rail** ([`electric_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/electric_rail.png)): High-voltage third-rail conductor running along porcelain insulator mounts with energized cyan core and electrical sparks.
    - **Braking & Holding Rail** ([`braking_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/braking_rail.png)): Friction retarder braking shoes along the inner steel rails with pneumatic clamp housing and amber hazard accents.
    - **Coupler Rail** ([`coupler_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/coupler_rail.png)): Heavy cast-iron shunting knuckle apparatus on Tie 3 with emerald green couple (+) and crimson red decouple (x) indicators.
    - **Station Delay Rail** ([`delay_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/delay_rail.png)): Octagonal brass station chronometer clock face on Tie 3 with luminous white dial face, black clock hands, and amber dwell status bezel.
    - **Locomotive Horn Rail** ([`horn_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/horn_rail.png)): Triple polished brass chime locomotive horns pointing down-track on Tie 3 with radiant golden acoustic shockwave arcs.
    - **Speed Control Rail** ([`speed_control_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/speed_control_rail.png)): Heavy cast-iron tachometer housing with graduated green/yellow/red speedometer gauge and red pointer needle.

---

## [1.1.31] - 2026-11-04

### Changed
- **3D Isometric Pixel-Art Item Icons for Remaining Specialty & Switch Rails (32×32)**:
  - Converted the item icons for **Junction Rail** ([`junction_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/junction_rail.png)), **Locomotive Control Rail** ([`control_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/control_rail.png)), **Turnout Rail (Right)** ([`turnout_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/turnout_rail.png)), **Turnout Rail (Left)** ([`left_turnout_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/left_turnout_rail.png)), **Wye Rail** ([`wye_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/wye_rail.png)), and **Electric Rail** ([`electric_rail.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/electric_rail.png)) to 32×32 3D isometric pixel-art sprites matching the rest of the mod's specialty rails and rolling stock.
  - Updated item model JSON definitions (`control_rail.json`, `electric_rail.json`, `junction_rail.json`, `turnout_rail.json`, `left_turnout_rail.json`, and `wye_rail.json`) to render the new 32×32 3D pixel-art item icons.

---

## [1.1.30] - 2026-11-04

### Changed
- **Railroader's Field Manual Item Icon Overhaul**:
  - Upgraded the guidebook item icon ([`guide_book.png`](file:///c:/Users/swack/Documents/GitHub/MineCraft_Track-and-Train-kit/src/main/resources/assets/miningtools/textures/item/guide_book.png)) from a 16×16 black binder to a bespoke 32×32 pixel-art crimson red railway binder.
  - Features rich crimson leather grain shading, heavy reinforced dark leather spine binding with brass staples, polished brass corner brackets, cream paper page blocks, a dangling golden bookmark ribbon, a brass locking clasp, and an embossed golden locomotive & track insignia on the front cover to prevent confusion with the Industrial Refinery guidebook.

---

## [1.1.29] - 2026-11-04

### Changed
- **3D Blockbench Rail Models Overhaul**:
  - Replaced flat 2D vanilla `rail_flat` block models for **Locomotive Horn Rail**, **Coupler Rail**, **Speed Control Rail**, and **Station Delay Rail** with full 3D HD Blockbench models featuring 3D wooden sleeper ties, raised 3D steel rails, cast-iron tie plates, and hex spikes to match **Braking & Holding Rail**, **Turnout Rails**, and **Electric Rails**.
  - Engineered 3D central apparatus modules sitting directly on the ties between the rails for each specialty track:
    - **Speed Control Rail**: 3D cast-iron tachometer housing and speed governor dial.
    - **Horn Rail**: 3D acoustic apparatus with dual brass trumpet horns (with active radiating soundwave shockwaves when powered).
    - **Coupler Rail**: 3D heavy shunting knuckle chassis with glowing emerald green couple arrows (`couple_mode=true`) or crimson red decouple arrows (`couple_mode=false`).
    - **Station Delay Rail**: 3D octagonal marine brass chronometer station clock face (with luminous amber dwell status ring when holding).
  - Added full 3D ascending slope models (`_raised_ne` and `_raised_sw`) for all 4 specialty rails to connect seamlessly across elevation changes.
  - Created 7 dedicated 128×128 HD device textures in `assets/miningtools/textures/block/cmodels/` and upgraded all corresponding block textures to 128×128.

---

## [1.1.28] - 2026-11-04

### Added
- **Track Paver 3-Tile Pattern Programming**:
  - Expanded the Heavy Track & Road Paver (`TrackPaverEntity`) interface and inventory from 29 to 31 slots to introduce 3 dedicated roadbed filter slots: **Left**, **Center** (beneath rail track), and **Right**.
  - Players can now program specific roadbed cross-section patterns (e.g. stone brick curbs on the flanks with gravel ballast under the rail, concrete borders, or hazard striping).
  - Flexible layout programming: leaving an outer filter slot empty preserves existing ground on that side (enabling 1-wide or 2-wide roadbeds).
  - Backward compatibility & material fallback: if all three filter slots are empty, the paver falls back to uniform ballast placement from bulk material storage.
  - Automatic inventory migration: older world saves (`PaverVersion` = 0) seamlessly migrate existing inventories without item loss.
- **Enhanced Paver GUI & Visuals**:
  - Reorganized GUI layout (`track_paver.png`): positioned 3 pattern slots side-by-side (`Pattern (L/C/R)`), moved the fuel slot and combustion burn flame to a dedicated left column, and repositioned action buttons with crisp status labels (`Work ON/OFF`, `Holo ON/OFF`).
  - Added interactive hover tooltips explaining the function of each filter slot, the fuel slot, and remaining combustion ticks.
- **Field Manual Integration**:
  - Updated the Heavy Machinery entry in the Railroader's Field Manual with instructions on configuring 3-tile roadbed patterns.

### Changed
- Consolidated trackside loader/unloader functionality documentation noting in-GUI mode toggles.

---

## [1.1.27] - 2026-11-04

### Changed
- **Standardized Rail Block Textures**:
  - Rebuilt block textures for **Speed Control Rail**, **Locomotive Horn Rail** (idle & active), **Coupler Rail** (couple & decouple modes), and **Station Delay Rail** (idle & active hold) to match standard track geometry, metal rail highlights (`#A4A4A4`/`#686868`), and oak/spruce wood sleeper ties.
  - Implemented crisp central mechanism artwork: tachometer and speed governor on Speed Control Rail, acoustic brass horn trumpets with radiating sound waves on Horn Rail, mechanical shunting knuckles with directional status indicators on Coupler Rail, and octagonal brass chronometer dial with luminous amber dwell holding glow on Station Delay Rail.
- **3D Isometric Pixel-Art Item Icons (32×32)**:
  - Overhauled item icons for **Braking & Holding Rail**, **Locomotive Horn Rail**, **Coupler Rail**, and **Station Delay Rail** to matching 32×32 3D isometric pixel-art style used across locomotives and rolling stock.
  - Updated `braking_rail.json` item model definition to reference dedicated item texture.

---

## [1.1.25] - 2026-11-04

### Added
- **Station Delay Rail (`miningtools:delay_rail`)**:
  - Implemented an interactive station dwell timer track that holds arriving trains and minecart consists for a configurable duration before automatically launching them onward.
  - Interactive GUI accessed by right-clicking the track:
    - **Timer ON/OFF Toggle**: Easily activate or bypass dwell timer operation.
    - **Configurable Dwell Duration**: Hold time adjustable from 1 to 3600 seconds, with one-click preset buttons (`3s`, `5s`, `10s`, `15s`, `30s`, `60s`) and fine-tuning delta buttons (`-10s`, `-1s`, `+1s`, `+10s`).
    - **Departure Direction Control**: Supports `Maintain Heading` (continues along arrival vector), `Reverse Direction` (bounces the train back the way it came), or designated cardinal directions (`North`, `South`, `East`, `West`).
    - **Train Name Filtering**: Apply station delays selectively to specific train consists (e.g. only dwell `PASSENGER_LOCAL` while `EXPRESS_FREIGHT` rolls straight through).
    - **Live Countdown Display**: Dynamic remaining dwell countdown rendered directly in the GUI and visual holding glow on the rail surface.
  - Full train consist clamping: securely holds entire coupled trains (steam, diesel, electric locomotives and trailing rolling stock) during station stops, and launches them reliably when the timer elapses.
  - Includes departure cooldown buffer to prevent trailing wagons from re-triggering the dwell timer as the consist departs.
  - Added dedicated 16x16 pixel-art textures (normal and illuminated/holding), block models, item models, blockstate JSON, recipe, and loot table.
  - Registered in `minecraft:rails` and `minecraft:mineable/pickaxe` block tags.
  - Documented in the **Railroader's Field Manual** under the `Tracks & Switches` category.

---

## [1.1.23] - 2026-11-04

### Added
- **Railroader's Field Manual (`miningtools:guide_book`)**:
  - Implemented the official comprehensive in-game guidebook item and GUI, styled after the industrial engineering binder theme established in the Industrial Refinery mod.
  - Built with a dark industrial slate binder shell, heavy steel bezel borders, polished brass corner brackets, left-hand category tabs with active gold indicators, and authentic UI sound effects.
  - Interactive Table of Contents with dynamic topic counter badges, item icon cards, and smooth mouse scrolling.
  - Rich entry detail view with `< Contents` return button, overview tab with item icons, subtitles, paragraph descriptions, content scrolling, and golden "PRO TIP" callout boxes.
  - Dedicated "Recipes" sub-tab showing exact input items -> arrow -> output items with interactive item tooltips and mechanical crafting notes.
  - Complete coverage across 7 dedicated categories (`Locomotives`, `Rolling Stock`, `Tracks & Switches`, `Stations & Logistics`, `Heavy Machinery`, `Tools & Signals`, `All Contents`) documenting all 42 items, vehicles, machinery, tracks, switchgear, and mechanics in the mod.
  - Added shapeless crafting recipe: 1x Book + 1x Iron Ingot -> Railroader's Field Manual.
  - Added to the Track & Train Creative Tab and vanilla Tools & Utilities tab.

### Fixed
- **Missing Block Drop Loot Tables**:
  - Fixed **Speed Control Rail** disappearing without dropping an item when destroyed or mined.
  - Fixed **Coupler Rail** disappearing without dropping an item when destroyed or mined.
  - Fixed **Surveyor's Seismic Charge** block disappearing without dropping an item when mined without detonation.
  - Created dedicated block drop loot tables (`speed_control_rail.json`, `coupler_rail.json`, `surveyors_charge.json`) with `survives_explosion` conditions to guarantee items drop reliably when harvested.

---

## [1.1.22] - 2026-11-03

### Added
- **Multi-Criteria Directional Track Switch Routing**:
  - Added **Travel Direction Filtering** (`All`, `North`, `South`, `East`, `West`) to Track Signal Switch rules. Resolves the true travel heading of approaching locomotives and consists via velocity and locomotive orientation.
  - Added **Freight Cargo State Filtering** (`Any`, `Loaded`, `Empty`) to Track Signal Switch rules. Detects stored items, bulk aggregates, fluids, and passengers across coupled freight cars, liquid tankers, aggregate hoppers, and livestock cars.
  - Added **Locomotive Fuel / Battery Threshold Filtering** (`Any`, `< 25%`, `< 50%`, `> 25%`, `> 50%`) to Track Signal Switch rules. Unified across Steam/Coal locomotives (burn time and coal reserves), Diesel locomotives (tank level), and Electric locomotives (internal battery charge). Low-fuel trains can automatically divert into refueling sidings while fueled trains remain on mainlines.
  - Added new localized translation strings for Direction, Cargo, and Fuel switch conditions in `en_us.json`.

### Changed
- **Track Signal Switch GUI (`TrackSwitchScreen`)**:
  - Overhauled and widened GUI to 264x224 with clean, responsive cycle buttons for Direction, Cargo, and Fuel conditions.
  - Added compact, color-coded criteria badges to rule rows (`[Dir]`, `[Cargo]`, `[Fuel]`) with comprehensive hover tooltips.
  - Allowed multiple coexisting routing rules for the same train name as long as their travel conditions differ.
- **Rule Specificity Ladder**:
  - Track switches now evaluate rules from most specific (Name + Direction + Cargo + Fuel) down to general fallbacks and wildcards, ensuring predictable routing behavior.

### Fixed
- **Wye Track & Reversing Loop Routing Conflict**:
  - Fixed an issue where trains returning or reversing from a spur track on a Wye junction re-triggered unconditional switch rules, preventing them from returning along their intended mainline direction.

---

## [1.1.21] - 2026-11-03

### Fixed
- **Speed Control Rail Approach Stopping & Tag Recognition**:
  - Registered `miningtools:speed_control_rail` and `miningtools:coupler_rail` into `minecraft:tags/block/rails.json` and `minecraft:tags/block/mineable/pickaxe.json`. Previously, approaching locomotives and rolling stock evaluated the speed control rail as a missing track gap or solid obstacle, triggering emergency consist shutdown.
  - Hardened rail detection with `TrainChainingUtil.isRail()` across all locomotive and rolling stock movement checks (`BasicLocomotiveEntity`, `DieselLocomotiveEntity`, `ElectricLocomotiveEntity`, and `TrainChainingUtil`), preventing false-positive obstacle detection on any rail block.
  - Temporarily suppressed coupled consist collision bounding boxes during locomotive repositioning so reverse and inverted travel never causes the locomotive to bind against its own cars.
  - Speed Control Rails now immediately apply directional velocity in the new travel heading upon activation.

---

## [1.1.20] - 2026-11-03

### Added
- **Dedicated Left Turnout Rail (`Turnout Rail (Left)`)**:
  - Added a dedicated, standalone `Turnout Rail (Left)` (`miningtools:left_turnout_rail`) block and item that branches left by default when placed.
  - Re-labeled the original turnout rail as `Turnout Rail (Right)` (`miningtools:turnout_rail`) for clarity.
  - Added full crafting recipes: craft 4 Left Turnout Rails using rails and a lever, or convert directly 1:1 between Right and Left Turnout Rails in any crafting grid (shapeless).
  - Fully integrated with Track Signal Switches and redstone control: toggling a switch or redstone signal shifts the articulated switch points and switches minecart paths between mainline straight and left divergent curves.
  - Added Railroad Crowbar `useOn` handling on turnout rails for consistent rotation and switch interaction.

---

## [1.1.19] - 2026-11-03

### Fixed
- **Turnout Rail Sneak-Click Branch Inversion**:
  - Restored Sneak + Right-Clicking a Turnout Rail with an empty hand (or crowbar) to immediately toggle and invert the divergent curve branch between Left and Right (`curve_left`).
  - Players can now configure left-curving and right-curving turnout switches bare-handed without facing drift.
  - Right-clicking with the Railroad Crowbar rotates track facing 90° clockwise.

### Added
- **Speed Control Rail Invert/Switch Direction Mode**:
  - Added a new **"Direction: Invert / Switch"** (`DIR_INVERT`) mode to the Speed Control Rail configuration GUI.
  - When a locomotive touches the rail in this mode, it automatically inverts its travel heading from whatever direction it arrived in (i.e. forward becomes reverse, reverse becomes forward), seamlessly switching the travel direction of the consist.

---

## [1.1.18] - 2026-11-03

### Fixed
- **Station Cart Targeting & Adjacent Car Isolation**:
  - Fixed an issue where Cargo Loaders/Unloaders, Fluid Loaders/Unloaders, and Entity Loaders/Unloaders would load or unload a train car beside the car actually resting on the station track (such as transferring into a coupled freight car when a caboose, locomotive, or passenger car is over the loader).
  - Reduced the entity detection bounding box from `inflate(1.1)` to `inflate(0.35)` and implemented precise center-distance tracking so the station only ever interacts with the cart directly occupying its rail block.
  - If the cart directly over the station rail is not an eligible target for transfer, neighboring coupled cars are completely ignored.

### Added
- **Configurable Train Release Delay on Loaders & Unloaders**:
  - Added an interactive **Release Delay** option (`None (0s)`, `1s`, `2s`, `3s`, `4s`, `5s`, `10s`, default 3s) in the GUI of Cargo Loaders, Cargo Unloaders, Locomotive Coaling Stations, and Fluid Loaders/Unloaders.
  - The station now continues holding the train for the configured delay duration after the last item or fluid transfer. This prevents trains from departing prematurely when items/fluids are still trickling in from external pipes, hoppers, or conduits.
  - Any new transfer during the delay window automatically resets the countdown timer.

---

## [1.1.16] - 2026-11-03

### Changed
- **Speed Control Rail Locomotive-Only Activation**:
  - Speed Control Rails now only activate when an actual locomotive (`BasicLocomotiveEntity`, `DieselLocomotiveEntity`, or `ElectricLocomotiveEntity`) directly touches the track.
  - Trailing or pushed non-locomotive carts (freight cars, tank cars, chest carts, passenger cars, cabooses, etc.) within a consist no longer trigger the speed control rail when rolling over it.
  - Consist speed and direction synchronization remains active when the locomotive itself passes over the rail, smoothly updating the full train consist.

---

## [1.1.15] - 2026-11-02

### Fixed
- **Track Layer Cart Position Snapping on Work Mode Switch**:
  - Fixed a position desynchronization bug where moving the Track Layer Cart forward in Transport/Off mode (`WORK_MODE_OFF`) and then switching back to Laying Mode (`WORK_MODE_LAY`) or Removing Mode snapped the cart backward to the location where work mode was originally disengaged.
  - `setWorkMode`, `setRunMode`, and `tick` now actively track and synchronize `currentRailPos` to the cart's actual current rail position (`getCurrentBlockPosOrRailBelow`) during non-work travel and upon mode switching.
  - Automatically clears stale stepping state (`isStepMoving`, `targetRailPos`, and `stepTicks`) whenever work mode changes or when the cart's current rail position diverges from its previous step origin.

---

## [1.1.12] - 2026-11-01

### Fixed
- **Smooth Train Departure from Railway Buffer Stops**:
  - Implemented directional motion and consist travel detection (`isCartMovingAway`) on `BufferStopBlock` and `TrainChainingUtil.checkAndTriggerBufferStop`. Trains resting at or against a buffer stop can now smoothly power up and drive away into the station without repeatedly shutting down or inching.
  - Resolved an issue in `BufferStopBlock.tick` where periodic 10-tick terminal scans unconditionally invoked `stopTrainConsist` on resting minecarts, repeatedly killing engine throttle while attempting to pull away.
  - Updated `stepOn`, `entityInside`, `checkAdjacentCarts`, and `checkAndTriggerBufferStop` to only trigger emergency halting when carts are moving *into* the buffer stop (`dot < -0.05D`), completely bypassing collision stops when moving in the departure direction.
  - Removed an overly aggressive distance check in `TrainChainingUtil.moveConsistCartAlongRails` that could falsely flag startup acceleration as a collision blockage.

---

## [1.1.11] - 2026-11-01

### Fixed
- **Railway Buffer Stop Collision & Consist-Wide Emergency Stopping**:
  - Buffer Stops now safely shut down and stop the entire train consist whenever any car (whether a pushed freight cart, lead car, trailing car, or locomotive) impacts or contacts the buffer stop.
  - Calling `TrainChainingUtil.stopTrainConsist` traverses all connected parent and child carts in the consist, halting delta movements to zero, shutting down all locomotives (`BasicLocomotiveEntity`, `DieselLocomotiveEntity`, and `ElectricLocomotiveEntity`) with `stopLocomotiveFromRail()`, and stopping all active `TunnelBoreEntity` units.
  - Updated the physical collision shape of `BufferStopBlock` to be recessed 2 pixels on its facing direction. Incoming minecarts now reliably intersect the buffer stop block volume upon impact with the timber pads, immediately triggering Minecraft's `entityInside` callback, impact clatter audio, spark particles, and powering terminal redstone (signal strength 15).
  - Added `onPlace` neighbor scanning to ensure buffer stops placed adjacent to resting trains immediately detect the train and emit redstone.
- **Train Consist Anti-Bunching & Obstacle Collision Handling When Pushing**:
  - Fixed an issue where pushing a train consist from the rear into an immovable object (buffer stops, stone walls, dead ends, or solid obstacles) caused the lead carts to stop while the rear locomotive kept pushing, causing all carts to telescope, compress, bunch up, and get tangled.
  - Updated `TrainChainingUtil.moveConsistCartAlongRails` to detect horizontal collision (`cart.horizontalCollision`) and lack of progress against obstacles, returning `false` rather than ignoring the obstruction.
  - Implemented consist-wide rollback in `TrainChainingUtil.handleChainedCartsMovement`: when any cart in a pushed or pulled consist encounters an immovable obstacle, all moved carts in the consist and the locomotive are instantly reverted back to their uncompressed pre-tick positions, zeroing velocities and shutting down all engines without any bunching or tangling.
  - Updated `pushCartFromLocomotiveBumper` and `pushEntitiesInLocomotivePath` to immediately halt the pushing locomotive when unlinked carts ahead encounter an immovable obstacle or buffer stop.

---

## [1.1.10] - 2026-11-01

### Fixed
- **Junction Rail (Cross Track) Consist Pushing & Direction Alignment**:
  - Fixed an inverted yaw angle calculation on `JunctionRailBlock` that caused minecarts facing North or South to evaluate as `EAST_WEST` when their delta movement was zero.
  - Implemented multi-stage approach direction detection for Junction Rails: prioritizes active velocity, previous tick movement (`xo`/`zo`), approach vector relative to the junction center, and chained consist neighbor coupling alignment before falling back to corrected Minecraft yaw.
  - Updated train consist movement in `TrainChainingUtil` (`traceRailLink` and `moveConsistCartAlongRails`) to respect the train's actual push/pull travel direction (`dirX`/`dirZ`) when traversing Junction Rails, ensuring trains pushed from the rear continue straight through cross intersections.
  - Expanded `findRailPath` to search all four exit directions across Junction Rails.

---

## [1.1.9] - 2026-10-31

### Fixed
- **Pipe Connectivity for Trackside Cargo & Coal Loaders**:
  - Registered `Capabilities.Item.BLOCK` on the Trackside Cargo Loader (`cargo_loader`), Trackside Cargo Unloader (`cargo_unloader`), and Locomotive Coaling Station (`coal_fueler`) block entity. Modded item pipes, conduits, and logistic automation systems (such as Pipez, Mekanism, Thermal, and LaserIO) can now directly connect to and transfer items through any accessible face.
  - Sided and directional capability handling respects station role filters: Cargo Loaders accept valid filtered freight items, Cargo Unloaders allow external pipe extraction, and Locomotive Coaling Stations accept valid coal and furnace fuels while preventing unintended fuel drainage.
  - Implemented automatic capability-based transfer for push and pull interactions with containers, pipes, and modded inventories placed directly beneath cargo stations.
  - Registered `Capabilities.Item.ENTITY` and `Capabilities.Item.ENTITY_AUTOMATION` across container minecarts and locomotives (`cargo_minecart`, `hopper_cart`, `basic_locomotive`, `tunnel_bore`, `track_paver`, `track_layer_cart`, and `turret_cart`), enabling direct item handler interaction from external automation.

---

## [1.1.8] - 2026-10-31

### Added
- **Turnout Rail Facing Rotation on Sneak-Use**:
  - Sneak + Right-clicking a Turnout Rail with an empty hand now rotates its horizontal facing 90° clockwise (matching Wye Rail behavior).
  - Completing a full 360° rotation (wrapping around from West back to North) cycles the divergent branch between Left and Right, allowing players to fully configure both facing and curve branch direction with bare hands.
- **Crowbar Quick Branch Switching**:
  - Right-clicking a Turnout Rail while holding the Railroad Crowbar directly toggles the divergent curve branch between Left and Right, complete with wrenching sound effects and action bar notification.

### Changed
- **Turnout Track Switch Animation & Visual Indication**:
  - Overhauled Turnout Rail 3D models (`turnout_rail_straight_right`, `turnout_rail_curved_right`, `turnout_rail_straight_left`, and `turnout_rail_curved_left`) to physically articulate between Normal Mainline (Straight) and Divergent (Curved) modes.
  - The switch target flag now rotates 90° between closed (parallel to the mainline) and open (perpendicular to the mainline) states with active color-coded status textures (`wire_off` vs. `wire_on`).
  - The switch tie rod and point blades now noticeably shift laterally across the track gauge when thrown.

### Fixed
- **Wye Rail Straight Mainline Visual Gap Fix**:
  - Added the missing top straight rail segment dynamically to both Wye switch modes (`wye_rail_right` and `wye_rail_left`), ensuring the straight mainline path across the top is physically continuous and visually seamless in both switch states.
  - When the switch is open (Right Branch mode), the left top rail is rendered to bridge the straight mainline path; when the switch is closed (Left Branch mode), the right top rail is rendered to bridge the straight mainline path.

---

## [1.1.6] - 2026-10-31

### Fixed
- **Track Layer Cart Climbing & Descending Slopes**:
  - Added terrain incline detection and 1-block step climbing/descending in Laying Mode, allowing the Track Layer to lay connected ascending and descending slope tracks automatically up hills and down valleys without stalling or halting.
  - Implemented multi-elevation slope navigation in Removing Mode, allowing the cart to seamlessly traverse and harvest tracks along ascending and descending grades.
  - Added terrain clearance and headroom safety checks for sloped tracks, preventing placement into blocked cliffs or under low ceilings while ensuring smooth clearance.
  - Updated driver controls in Normal Transport Mode to provide active forward and reverse propulsion along rail gradients.
  - Added keyboard steering controls (`A` / `D` keys) for turning the Track Layer left and right.
  - Calibrated trailing consist cars to snap vertically to slope rails when towed by the Track Layer.

---

## [1.1.5] - 2026-10-31

### Changed
- **Track Paver Transport Mode Speed Calibration**:
  - Calibrated Transport Mode maximum forward and reverse speed to **2.0 m/s** (10 ticks per block step, down from 5.0 m/s) for safe, controllable maneuvering and repositioning without runaway speeds.
  - Updated in-game action-bar overlay messages and HUD speedometer scaling to reflect the 2.0 m/s transport speed limit.

---

## [1.1.4] - 2026-10-30

### Fixed
- **Cargo Cart Log Texture UV Alignment & End Face Transparency Fix**:
  - Re-mapped the high-resolution textures in both `logging_cart.png` and `wood_cart.png` to precisely match the 4×4×24 box UV specification for all five log tiers (`log1` through `log5`).
  - Fixed transparent triangular patches on the top surface and missing top-row pixels on log ends caused by the previous UV offset falling into empty transparent space.
  - Applied 100% solid pixel resampling and 2-pixel clamp-padding around face borders, eliminating edge transparency, depth-buffer flickering, and mipmap bleeding at the ends of logs.

---

## [1.1.3] - 2026-10-29

### Fixed
- **Cargo Cart Log Stacking Pole Clipping Fix**:
  - Re-calibrated log geometry in `LoggingCartModel` from 5×5 cross-section to 4×4 cross-section (total bottom width of 12.0 units), perfectly fitting between the inner faces of the upright side stakes (`Z = -6.1` and `Z = 6.1`) with 0.1-unit safety clearance.
  - Adjusted top nesting log positions (`log4` and `log5`) into the lower grooves to prevent any model overlap or visual clipping through the vertical side poles.

---

## [1.1.2] - 2026-10-29

### Added
- **Cargo Cart Dynamic Log & Plank Pallet Displays**:
  - Cargo carts now automatically detect their stored contents without requiring a manual filter:
    - When containing exclusively logs, cargo carts dynamically render authentic multi-tiered round log stacks with upright side stakes instead of sea can shipping containers.
    - When containing exclusively wooden planks, cargo carts dynamically render neat bundles of milled lumber resting on dual 3D wooden shipping pallets complete with top slats, stringers, and forklift pockets instead of sea can shipping containers.
  - Added synchronized cargo visual state (`DATA_CARGO_TYPE`) ensuring rolling carts immediately and smoothly update their visual appearance for all nearby players on servers.
  - Added plank and wooden slab support to Cargo Cart filtering and acceptance validation.
- **Hopper Cart All Raw Ores & Block Ore Version Support (Including Tin)**:
  - Comprehensive ore classification in `AggregateHopperCartEntity` supporting all raw ores (`raw_iron`, `raw_copper`, `raw_gold`, `raw_tin`, modded raw metals) and all block ore versions (`iron_ore`, `deepslate_iron_ore`, `raw_iron_block`, `raw_tin_block`, `tin_ore`, `deepslate_tin_ore`, `coal_ore`, `redstone_ore`, `nether_quartz_ore`, `ancient_debris`, etc.).
  - Prioritized ore evaluation before stone and fuel checks, preventing ores with "stone" or "coal" in their registry paths (such as `redstone_ore` or `coal_ore`) from being misclassified as gravel or coal.
  - Enforced strict aggregate validation ensuring non-bulk items cannot be inserted into hopper carts.

---

## [1.1.1] - 2026-10-29

### Fixed
- **Speed & Gear Control Rail Recipe Format**:
  - Corrected key ingredient definitions in `speed_control_rail.json` to use standard item ID strings (`"minecraft:iron_ingot"`, `"minecraft:redstone"`, `"minecraft:clock"`) instead of legacy nested `{"item": ...}` objects, resolving the `IllegalStateException: Map entry 'I' : Failed to parse either` recipe registry failure on server and world load.

---

## [1.1.0] - 2026-10-29

### Added
- **Creative Executive Diesel Locomotive (`creative_diesel_locomotive`)**:
  - Added a dedicated creative-mode executive diesel locomotive sporting a Royal Cobalt / Midnight Blue body with Brilliant Gold chevron accents.
  - Infinite fuel: operates continuously without requiring fueling, never drains input containers, and bypasses tender cart siphoning.
  - Full support for multi-gear throttle (Gears 1–5), headlights, horn, dynamic lighting, and consist hauling.
  - Exclusive to Creative Mode tabs (no crafting recipe required).
  - Custom 32×32 3D pixel-art icon on rails matching the fleet's high-res styling.
- **Speed & Gear Control Rail (`speed_control_rail`)**:
  - Added an interactive track piece with a real-time configuration GUI opened by right-clicking.
  - Set target locomotive speed/gear (0 = Stop/Neutral, 1–5 = Forward/Reverse gears) as a locomotive or consist travels across the track.
  - Direction setting modes: Keep Current Direction, Force Forward, or Force Reverse.
  - Train Name Filter: assign target speed changes to specific named trains (or leave empty / `*` to affect all rolling stock).
  - Single execution per trip: prevents repetitive activations or sound spam while a train is parked on the rail.
  - Crafted with Iron Ingots, Redstone Dust, and a Clock.
  - High-resolution 32×32 3D item icon with embedded speedometer gauge and tachometer sensor rail texture.

---

## [1.0.13] - 2026-10-29

### Fixed
- **Railway Block Signal Blank Icon Fix**:
  - Replaced the empty transparent placeholder for `railway_signal.png` with a rich 32×32 3D pixel-art icon displaying the concrete foundation plate, steel tubular mast, ladder rungs, side electrical relay cabinet, and 3-aspect signal target head with sun visors and glowing lens.

---

## [1.0.12] - 2026-10-29

### Fixed
- **Coupler Rail Recipe Registry Identifier Fix**:
  - Corrected the chain recipe ingredient in `data/miningtools/recipe/coupler_rail.json` from `minecraft:chain` to `minecraft:iron_chain` to match Minecraft 26.3 vanilla registry naming, resolving the datapack recipe loading crash.

---

## [1.0.11] - 2026-10-29

### Changed
- **High-Resolution 3D Pixel-Art Icons for All Trains & Rolling Stock**:
  - Overhauled all 13 locomotive, heavy machinery, and railcar item icons to a unified 32×32 3D pixel-art style matching the Track Layer Cart, complete with iron rails, wooden ties, flanged wheels, and crisp dark perimeter outlines:
    - **Basic Steam Locomotive**: Boiler straps, brass steam dome, smokestack puffing smoke, cab windows, golden circular headlight, and front pilot cowcatcher.
    - **CN Diesel Locomotive**: Canadian National red-and-black safety livery, white zebra stripes, ditch lights, cooling grilles, and exhaust stack.
    - **Electric Bullet Train**: Aerodynamic white streamline nose cone, cyan speed stripes, tinted windshield, and red roof pantograph.
    - **Anchor Caboose**: Red observation cupola, roof stovepipe chimney puffing smoke, and end platform railings.
    - **Cargo Minecart**: Heavy flatbed with vertical steel stakes loaded with stacked wooden timber logs.
    - **Aggregate Hopper Car**: Industrial metallic steel finish with exterior structural ribs, discharge chutes, and 3 distinct mounded 3D heaps.
    - **Livestock Cart**: Horizontal wooden slatted construction with interior stall gaps, sliding door hardware, and curved metal roof.
    - **Liquid Minecarts (Small & Large)**: Pressurized cylindrical steel tanks with spherical end heads, top walkway railings, manhole domes, and hazard placards.
    - **Arrow Turret Cart**: Tactical woodland camouflage pattern, armored roof cupola, and dual arrow ports with steel tips.
    - **Generator Cart**: Heavy dynamo housing, glowing copper stator coils, radiator fins, brass pressure gauge with needle, and cyan energy buffer LED.
    - **Tunnel Bore**: Heavy armored frame, hazard warning stripes, and massive conical diamond rotary cutting head.
    - **Track Paver (AP1055F)**: Caterpillar safety yellow body, continuous crawler tracks with bogie rollers, front asphalt hopper, and elevated operator canopy.

---

## [1.0.10] - 2026-10-29

### Added
- **Coupler Rail (`coupler_rail`)**:
  - A toggleable specialty railway track block and block entity designed for automated consist switching and yard coupling.
  - **Couple Mode (`couple_mode=true`)**: Automatically scans a 16-block radius around an arriving locomotive and couples the nearest eligible cart or consist end to the locomotive.
  - **Decouple Mode (`couple_mode=false`)**: Automatically uncouples the locomotive from its directly attached trailing or pushed car, leaving the remaining cars in the consist securely linked together.
  - **Single Execution Per Trip**: When a locomotive stops or stays parked on the rail, the block entity triggers connection or disconnection strictly once per trip and stays inactive until the locomotive fully departs the track.
  - **Manual & Redstone Toggling**: Right-clicking the rail toggles between Couple and Decouple modes with action bar feedback; redstone power inverts the mode for automated shunting yards and runaround tracks.
  - **Crafting Recipe**: 6 Iron Ingots + Chain + Redstone Dust yields 6 Coupler Rails.

### Fixed
- **Entity Loader Passenger Ejection Fix**:
  - Prevented Entity Loader blocks from transmitting redstone power upwards into Activator Rails directly above them, preventing loaded passengers from immediately being ejected if the cart remains on the station.
  - Added protection to Livestock Carts to suppress activator rail ejection whenever resting over an Entity Loader block.

---

## [1.0.9] - 2026-10-28

### Fixed
- **Track Layer Cart Item Model & Held Model Resolution**:
  - Added missing 1.21.4+ item model definition (`assets/miningtools/items/track_layer_cart.json`), resolving missing texture/model fallback (purple-and-black checkerboard cube) in inventory, creative tab, GUI, and player hands.
  - Upgraded Track Layer Cart item icon to a crisp, high-resolution 32×32 pixel-art sprite displaying the monolithic yellow armored chassis, overhead roll cage, cockpit controls, front track dispenser chute, rear retrieval claw, and flanged steel wheels on rails.

---

## [1.0.8] - 2026-10-28

### Added
- **Track Layer Cart (`track_layer_cart`)**:
  - Compact 1-block minecart vehicle built with a custom monolithic industrial 3D model featuring an armored unibody hull, protective overhead roll-cage canopy, recessed driver cockpit, front track guide chute, rear articulated track retrieval scoop, and flanged railway wheels.
  - **Track Laying Mode**: Places 1 piece of track from its inventory down in front of itself onto solid ground as it moves forward along the track alignment, paving continuous rail lines automatically.
  - **Track Removing Mode**: Drives along minetrack and picks up each track piece behind itself after traveling over it, storing retrieved rails back into its inventory.
  - **Calibrated Work Mode Speed (1.5 blocks/s)**: Travel speed is governed to an exact, calibrated 1.5 blocks/s (0.075 blocks/tick) in Laying and Removing modes for smooth and reliable track work.
  - **Normal Transport Mode**: When work mode is turned OFF, functions as a normal minecart (free-rolling, responsive to powered rails, slopes, locomotive towing, and manual driving up to standard 8.0 m/s speeds).
  - **Player Driveability & GUI**: Fully steerable while riding (`W` = Forward, `S` = Stop/Brake, `R` = Reverse direction, `H` = Cycle Laying/Removing/Transport modes, `C` = Open GUI) with action-bar status HUD and an interactive dashboard GUI.
  - **Capacity & Crafting**: 28-slot inventory (1 Pattern slot + 27 track storage slots) and shaped crafting recipe using iron ingots, iron blocks, a piston, rail, and a chest minecart.

### Changed
- **Train Consist Material & Resource Sharing**:
  - Carts in a linked train consist that use material (coal/solid fuel, diesel fluid, tracks, ballast, paving blocks) can now automatically draw materials from and deposit materials into linked carts that have room and accept that material.
  - **Track Sharing**: Track Layer Carts draw rails from linked wagons (such as a connected Cargo Minecart) when placing track, and deposit harvested rails into linked storage wagons when removing track.
  - **Solid Fuel Sharing**: Steam Locomotives, Generator Carts, Tunnel Bores, and Track Pavers automatically pull coal and furnace fuels from linked coal tenders and cargo carts when fuel reserves run low.
  - **Diesel Fuel Sharing**: Diesel Locomotives now draw diesel fuel and lava across the entire linked train consist from connected Small or Large Liquid Minecarts.
  - **Ballast & Paving Blocks**: Tunnel Bores and Track Pavers automatically draw ballast and matching paving blocks from linked hopper and cargo carts in the consist.

---

## [1.0.7] - 2026-10-28

### Fixed
- **Recipe JSON Syntax for Railway Signal & Signal Tuner**:
  - Corrected ingredient key mappings from legacy object format to Minecraft 26.3 direct string item identifiers.
  - Corrected recipe category for `signal_tuner` from invalid `tools` to `equipment`, resolving server data pack loading crash on startup.

---

## [1.0.6] - 2026-10-28

### Added
- **Dedicated Railway Block Signaling & Interlocking System**:
  - **Railway Block Signal (`railway_signal`)**: A 3D steel mast signal post with 3-aspect LED lenses (Red, Yellow, Green, and Shunt) indicating track block occupancy. Emits analog redstone signals (Green: 15, Yellow: 8, Shunt: 4, Red: 0).
  - **Signal Tuner Radio (`signal_tuner`)**: Handheld wireless transceiver item used to pair signals into long-distance Block Sections, toggle between Passing Siding and Automatic Block Signal (ABS) modes, and link turnout switches.
  - **Dynamic Track Circuit & Occupancy Detection**: Signals automatically scan rail tracks between paired signals and update aspects to Red when trains or minecarts occupy the section.
  - **Automated Passing Siding Routing**: Automatically throws linked `TrackSwitchBlock` turnouts to divert incoming trains into passing sidings when the main block ahead is occupied, holding them safely on braking rails until opposing trains pass.
  - **Fail-Safe Train Halting at Red Signals**: Smoothly brings trains approaching a Red signal to a stop, zeroing locomotive speed level with action bar notifications.
  - **Shunting & Yard Coupling Protection**: Operators holding Shift or moving at Gear 1 Crawl Speed (1.0 m/s) can safely pass or enter occupied tracks to bump-couple cars without triggering emergency stops.
  - **Complete 1.21.4+ Item Model Definitions & Crisp Textures**: Added dedicated item definition JSONs in `assets/miningtools/items/`, model JSONs in `models/item/`, and 16x16 pixel-art icons in `textures/item/` for flawless rendering in inventory, GUI, and in-hand.
  - **Crafting Recipes**: Added shaped crafting recipes for Railway Block Signal and Signal Tuner Radio.

---

## [1.0.5] - 2026-10-28

### Added
- **Universal Burnable Fuel Support for Basic Locomotive**:
  - Basic Locomotives now accept any valid burnable fuel item (coal, charcoal, wood logs, planks, wooden slabs, sticks, blaze rods, lava buckets, coal blocks, and modded items with the CookingFuel data component).
  - Burn durations scale proportionally with standard furnace burn time and respect the configured coalBurnTicks modifier.
  - Automatically preserves fuel remainder containers (such as returning an empty bucket when burning a lava bucket).
  - Updated Basic Locomotive GUI fuel slots and shift-click quick-move routing to accept all valid fuels.
  - Automated fueling stations now support universal fuel loading into locomotives.
- **Track Paver Physical Collision Box**:
  - Implemented full entity collision bounds (canBeCollidedWith, canCollideWith, isPushable), preventing entities and players from walking straight through the paver's body and allowing standing on its deck.
- **Track Paver Toggleable Work Light**:
  - Added a high-output workspace floodlight illuminating the vehicle surroundings at light level 15.
  - Toggleable via the Paver GUI (Light) and rider hotkey (L key) with server-synced NBT state and automatic cleanup.
- **Track Paver Work Mode & Dual-Speed Controls**:
  - Introduced an integrated Work Mode toggle (Work: ON / Work: OFF) accessible in the GUI and via rider hotkey (H key).
  - **Work Mode (ON)**: Replaces blocks along the 3-wide foundation strip at a regulated construction speed of **1.0 m/s** (20 ticks/block).
  - **Transport Mode (OFF)**: Disables block replacement and increases travel speed to **5.0 m/s** (4 ticks/block) for rapid transit and maneuvering.
  - Updated the HUD speedometer overlay to scale dynamically to 1.0 m/s in Work Mode and 5.0 m/s in Transport Mode.
- **Track Paver 3x3 Ghost Hologram Alignment Grid**:
  - Added an interactive in-world holographic 3x3 wireframe grid projecting over the blocks directly ahead that will be replaced along the paver's trajectory.
  - Automatically adapts to slope elevations, 1-block steps, and terrain contours across all three paving lanes.
  - Renders in vibrant cyan (Work Mode) and amber (Transport Mode) to assist operators in lining up target paving strips.
  - Toggleable in the Paver GUI (Holo) and via rider hotkey (Shift + H).
- **Track Paver Universal Fuel Support**:
  - Extended the Paver's fuel system and GUI slot to accept all burnable fuels.

### Changed
- **Track Paver Terrain & Obstacle Collision**:
  - Maintained smooth 1-block step climbing and 1-block step descent while strictly halting when encountering obstructions or walls 2 blocks or higher, preventing clipping through mountains or terrain.
  - Added safety halts for 2+ block drops (cliffs), low-clearance ceilings/overhangs, and unbreakable bedrock barriers.

---

## [1.0.4] - 2026-10-26

### Changed
- **Heavy Track & Road Paver Auto-Shutdown on Material Exhaustion**:
  - Automatically halts vehicle movement (`MODE_STOPPED`) and shuts down engine activity when the machine runs out of the material set in its filter slot within its inventory.
  - Terminates further strip paving immediately if material runs out mid-placement, preventing incomplete roads and unpaved gaps.
  - Added material availability validation to prevent starting or reversing while out of paving material, and alerts operators and nearby players with an action bar notification (`Track Paver: Out of paving material!`).
- **Automated Version Bumping in Build Script**:
  - Added source hash tracking (`.last_source_hash`) and automatic patch version incrementing to `build.gradle` on build/jar tasks when mod changes are detected.
  - Added explicit versioning tasks: `bumpVersion`, `bumpPatch`, `bumpMinor`, `bumpMajor`, and `checkModChanges`.

---

## [1.0.3] - 2026-10-26

### Changed
- **JourneyMap Railway Radar Entity Labels**:
  - Map radar name labels are now exclusively rendered for powered locomotives (`Steam Locomotive`, `Diesel Locomotive`, `Electric Locomotive`) to keep mini-map radar displays clean and uncluttered.
  - Coupler and trailing carts (`Passenger Car`, `Cargo Car`, `Fluid Tanker Car`, `Hopper Car`, `Caboose`) retain full hover tooltips and directional radar icons while hiding permanent text badges.
- **Junction Rail Visual Continuity**:
  - Extended the 3 middle wooden ties horizontally across the full track gauge width in both the 3D block model (`junction_rail.json`) and 2D block texture (`junction_rail.png`).
  - Restored visual symmetry and eliminated open gaps between branching rails when connecting straight and diagonal railway spurs.

---

## [1.0.2] - 2026-10-25

### Added
- **Heavy Track & Road Paver (CAT AP1055F Inspired)**:
  - Added heavy industrial road paver vehicle entity, item, and creative tab registration.
  - Animated rubber crawler treads synchronized to vehicle forward/reverse motion.
  - Animated engine cooling fan rotating on high-contrast roof housing when active.
  - Material hopper receiving foundation block filters and bulk inventory supply.
  - High-output road laying screed paving 1-block deep by 3-block wide roadbed foundations.
  - Automated track laying on the center strip over freshly smoothed foundations.
  - Interactive Paver Control GUI featuring live fuel gauges, paving status, cardinal lock controls, forward/stop/reverse controls, and fuel/filter/material slots.
- **Speedometer HUD Overlay**:
  - Digital speedometer and RPM tachometer bar displaying live forward speed in m/s (0.0 m/s when idle or out of fuel).
- **Audio Sound Effects**:
  - Industrial diesel engine rumble loop with dynamic pitch modulation based on travel speed and engine load.
  - Hydraulic screed tamping and material laydown sound effects triggered during paving cycles.
- **Jade / Waila Mod Support**:
  - Live tooltip display of fuel burn time, engine running state, paving status, loaded materials count, and lock direction.

### Changed
- **Locomotive Crawl Speeds & Cruise Control**:
  - Gear 1 crawl speed calibrated to an exact 1.0 m/s (0.05 blocks/tick) across all locomotives for precise yard switching and coupling.
  - Cruise control (Gear 3) standardized at 50% max speed across Steam, Diesel, and Electric locomotives.
  - Locomotive pulling strength enhanced to haul up to 5 coupled carts without speed loss or drag penalty.
- **Visual Model Corrections**:
  - Eliminated texture gaps and untextured faces on Locomotive chassis and Paver treads.
  - Adjusted entity bounding boxes and eye heights for comfortable third-person and first-person camera perspectives.

---

## [1.0.1] - 2026-10-20

### Added
- **Advanced Coupling System**:
  - Double-ended couplers supporting train composition reversals and bidirectional shunting.
  - Dynamic inter-cart distance stabilization preventing derailments on sharp curves and switch tracks.
- **Passenger & Freight Rolling Stock**:
  - Passenger Car with 4-player seating and interior lighting.
  - Freight Hopper Car with automatic hopper transfer integration.
  - Fluid Tanker Car holding up to 32,000 mB of fluids with dynamic fill level indicators.
  - Caboose with mobile crafting station and resting bunk.

---

## [1.0.0] - 2026-10-15

### Added
- Initial release of **Track and Train Pack** for Minecraft 26.3 on NeoForge.
- Basic Steam Locomotive, Diesel Locomotive, and Electric Locomotive.
- Wooden, Iron, and High-Speed Ballasted track types.
- Manual and redstone-controlled switch rails and junction crossings.