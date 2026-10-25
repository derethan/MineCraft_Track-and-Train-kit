$changelogContent = @'
# Changelog

All notable changes to the **Track and Train Pack (Mining Tools)** mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.1] - 2026-10-24

### Added
- **Loose Aggregate Hopper Car (Hopper Minecart)**:
  - Heavy-duty open-top hopper car modeled after North American railroad ballast hoppers (e.g. TIMX, Herzog).
  - Authentic 3D visual mound rendering: dynamically renders 3 heaped mounds inside the container when hauling loose bulk materials.
  - Dynamic material textures: automatically maps visuals to the loaded aggregate (Coal mounds, Gravel/ballast heaps, yellow Sand heaps, Red Sand heaps, Dirt/clay heaps, and raw metal Ore heaps).
  - Container renders as a hollow open hopper container showing internal sloped bottom discharge chutes when emptied.
  - High-capacity 27-slot aggregate inventory with a dedicated **Filter Slot** displaying a custom hopper funnel icon.
  - Track-aligned placement orientation: cart automatically aligns its initial yaw to the railway track upon placement, preventing rotation anomalies on curved or switch tracks.
- **Surveyor's Charge & Remote Detonator**:
  - Subterranean seismic prospecting explosive and handheld wireless detonator.
  - **Full-Depth Cylindrical Column Survey**: Scans a cylindrical column (configurable radius, default 10 blocks) extending from the charge placement level all the way down to bedrock (`level.getMinY()`).
  - **Crude Oil & Fluid Detection**: Detects and aggregates oil fluids and oil source blocks (`#c:oil`, `#c:crude_oil`, and registry identifiers matching `crude`, `oil`, `petroleum`, `diesel`, and `fuel`).
  - **Borehole Blasting**: Detonation cleanly excavates a 1×1 vertical borehole shaft directly downward to bedrock (with configurable block drops or clean destruction to eliminate item lag).
  - **Wireless Remote Detonator**: Handheld remote triggers all Surveyor's Charges within a 12-block radius and relays detailed geological survey chat reports directly to the player.
  - Comprehensive configuration options in `config/miningtools-common.toml` for scan radius, ignored blocks, extra reported ore IDs, and borehole block drops.
- **Diesel Locomotive Fuel Tender Siphoning**:
  - CN Diesel Locomotive (#3087) now actively detects coupled Liquid Minecarts and Large Liquid Minecarts in the consist.
  - Automatically siphons compatible diesel or biofuel at up to 100 mB/tick, turning coupled tankers into long-haul fuel tenders.
- **Universal Consist Kinematics & Free Coupling**:
  - Trains can now be coupled together using the Railroad Crowbar without requiring a locomotive.
  - Moving or pushing any unpowered coupled cart (by hand or gravity) synchronizes and pulls/pushes the entire consist along rails at exact coupler spacing.
  - Universal Caboose coupling: Anchor Cabooses can now be coupled anywhere in a consist (front, middle, or rear) without tail-only restrictions.
- **Bumper-to-Bumper Rail Physics & Collision**:
  - Implemented custom minecart-to-minecart collision separation and momentum transfer (`TrainChainingUtil.pushMinecarts`), bypassing vanilla's `dot < 0.8` yaw cancellation bug.
  - Eliminates loose carts phasing through each other on tracks or when placed at stationary orientations.
  - Continuous slope height interpolation eliminating cart shaking, bouncing, and stalling when transitioning onto sloped rails.

### Changed
- Refactored `SurveyorsChargeBlock` scan algorithm from a spherical bounding box to a full-depth vertical cylinder (`dx*dx + dz*dz <= rSq`) extending to `level.getMinY()`.
- Defaulted `minecraft:lava` to ignored survey blocks to prevent cluttering subterranean ore detection reports.
- Modernized Crowbar coupling messages to reflect universal cart placement.

### Fixed
- Fixed dedicated server registry crash (`IllegalStateException: List is too short: 0, expected range [1-9]`) on NeoForge 1.21.4 / 26.3 by formatting recipe ingredient lists to Mojang codec standards.
- Fixed missing item models and textures for Surveyor's Charge and Remote Detonator.
- Fixed loose aggregate cart passing through stationary carts when pushed.
- Fixed carts shaking forward and backward on sloped rails.

---

## [1.0.0] - Initial Release
- Initial release of Track and Train Pack for Minecraft 26.3 (NeoForge).
- 3 Motive Power classes: Basic Steam Locomotive, CN Diesel Locomotive (#3087), and High-Speed Electric Bullet Train.
- Railroad Crowbar train coupling and Multiple-Unit (MU) distributed traction physics.
- Adaptive Cargo Minecart (Logs, Container, Ore, Ingot models), Livestock Cart, Liquid Minecart, Large Liquid Minecart, Arrow Turret Cart, and Anchor Caboose.
- Automated 3×3 Tunnel Bore with dynamic track and ballast laying.
- Specialty rails: Junction Rail (4-Way), Turnout Switch (T-Switch), Wye Rail (3-Way), Horn Rail, Control Rail, and Electric Third Rail.
- Trackside automation facilities: Entity Loader, Entity Unloader, Fluid Station, Track Signal Switch Stand, Railway Buffer Stop, and Chunk Anchor.
'@

$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\CHANGELOG.md"
[System.IO.File]::WriteAllText($path, $changelogContent, [System.Text.UTF8Encoding]::new($false))
Write-Host "CHANGELOG.md created successfully."
