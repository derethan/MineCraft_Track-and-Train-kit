# Changelog

All notable changes to the Track and Train Pack mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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