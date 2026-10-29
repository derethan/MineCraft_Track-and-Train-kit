# Changelog

All notable changes to the Track and Train Pack mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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