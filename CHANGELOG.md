# Changelog

All notable changes to the **Track and Train Pack (Mining Tools)** mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.2] - 2026-10-25

### Added
- **Turret Cart Whitelist & Blacklist Targeting Controls**:
  - Added an integrated Whitelist / Blacklist toggle button and text input box directly in the Turret Cart GUI with full server synchronization and NBT persistence.
  - Allows whitelisting friendly players and allies (comma-separated, e.g. `Steve, Alex`) to safeguard them from turret fire and protect line-of-sight friendly fire.
  - In Whitelist mode, automatically targets hostile mobs and non-whitelisted players when a player list is configured.
  - In Blacklist mode, allows explicitly blacklisting players or specific entity types for turret defense.
- **2x Turret Cart Engagement Range**:
  - Doubled effective turret detection and firing radius from 16.0 blocks to **32.0 blocks**.
- **JourneyMap Live Train Name Displays & Far-Distance Map Persistence**:
  - Live train names and custom cart names are now rendered directly above the train icon markers on JourneyMap with high-contrast, shadowed text badges.
  - Added real-time server-side train position broadcasting (`TrainMapSyncPayload`) so trains and consists remain visible and moving across the entire world map, eliminating train markers disappearing when traveling beyond client render distance.
- **Random Freight Container Colors for Cargo Carts**:
  - Introduced randomized shipping container paint schemes (Intermodal Ocean Blue, Freight Crimson Red, and Military Forest Green) across Cargo Carts while maintaining authentic Caboose-style flanged steel wheels.

### Fixed
- **Junction Rail Wooden Tie Continuity**:
  - Extended the three middle wooden ties horizontally across the full track width (X=1 to X=15) in the junction rail 3D model ([junction_rail.json](file:///src/main/resources/assets/miningtools/models/block/junction_rail.json)) and 2D block texture ([junction_rail.png](file:///src/main/resources/assets/miningtools/textures/block/junction_rail.png)), ensuring clean visual alignment with the top and bottom ties and removing redundant stub geometry.
- **Track Paver Speedometer & Off-State Calibration**:
  - Fixed the speedometer overlay displaying maximum speed (8.0 m/s / full bar) when the paver was powered off or out of fuel.
  - Calibrated reference max speed for Track Pavers to 1.0 m/s in `SpeedometerOverlay`.
- **Track Paver Persistent Audio Loop**:
  - Fixed Track Paver sound continuously playing and getting louder when off/out of fuel by resetting client and server delta movement to zero and enforcing silence when stopped.
- **Track Paver Ground Hovering**:
  - Fixed Track Paver hovering 6 pixels (0.375 blocks) in mid-air above the terrain by setting ground translation flush to the surface when running off-rails.
- **Livestock Cart Z-Fighting & Flickering**:
  - Fixed visual polygon flickering and Z-fighting across the Livestock Cart model by eliminating overlapping coplanar faces between doorway posts, horizontal wooden slats, end bulkheads, and roof eaves.
- **Steam Locomotive (Basic Locomotive) Roof & Headlight Alignment**:
  - Removed the yellow whistle from the roof of the cab.
  - Elevated the front 3D headlight housing and bracket to align with and cover the circular light texture painted onto the front of the boiler door.

### Changed
- **Texture Overhauls & Caboose-Matched Wheels**:
  - **Turret Cart**: Completely remastered the texture from 128×128 to an ultra-crisp 1024×1024 high-definition military woodland camouflage livery with steel rivets, eliminating blurriness, and added flanged steel wheels matching the Caboose.
  - **Hopper Carts**: Standardized all Hopper Carts (empty, coal, dirt, gravel, ore, red sand, sand) to 1 solid color smooth metallic finish with Caboose-matching flanged steel wheels.
  - **Livestock Cart & Generator Cart**: Updated wheels to match the flanged steel Caboose style.
  - **Cargo Minecarts**: Updated flatbed undercarriage and container variants with Caboose-matching flanged steel wheels.

---

## [1.0.1] - 2026-10-24

### Added
- **Heavy Track & Road Paver (`track_paver`)**:
  - Heavy industrial caterpillar track paver machine inspired by CAT AP1055F heavy equipment (2.75 blocks wide, 2.0 blocks high, 3.0 blocks long).
  - **Calibrated Paving Speed**: Crawls forward at an exact 1.0 m/s (20 ticks per block step), paving a 1-block deep by 3-block wide strip (left, center, right) with the designated paver block.
  - **Track Preservation Under Rails**: Intelligently replaces foundation blocks directly underneath railway tracks without popping, breaking, or disturbing the rails on top!
  - **On-Rails & Off-Rails Traversal**: Operates freely over solid terrain with 1.0-block climbing step-up, and automatically snaps to rail center and orientation if placed on or driven over tracks.
  - **Cardinal Direction Lock**: GUI toggle allows locking travel strictly to North, South, East, or West for perfectly straight highway and railway roadbeds, or unlocking for manual steering.
  - **Manual Operations & Dedicated Hotkeys**: Players can ride in the elevated right-hand operator seat and control the paver using standard WASD hotkeys (`W` = Start/Forward, `S` = Stop/Halt, `A` = Turn Left 90°, `D` = Turn Right 90°, `R` = Reverse, `C` = Cockpit GUI) with in-game action-bar overlay status notifications.
  - **27-Slot Material Hopper**: Dedicated Paver Pattern / Target Block Slot (Slot 0) + Fuel Slot (Slot 1) + 27 bulk material storage slots (Slots 2–28) holding up to 1,728 paving blocks.
  - **Custom Naming**: Full in-GUI naming support integrating with railway yard tracking.
- **Generator Train Cart (`generator_cart`)**:
  - Industrial mobile steam dynamo and Forge Energy (FE/RF) generator cart with a custom 3D compact model (< 1.5 blocks tall).
  - Consumes solid fuel (Coal, Charcoal, Coal Blocks, Blaze Rods, Wood) and water from an internal 8,000 mB reservoir.
  - Generates 100 FE/t (configurable) into an onboard 100,000 FE battery buffer.
  - **Dynamic Fuel Throttling**: Automatically halts fuel consumption and burns cleanly when the internal battery is fully charged or water is depleted.
  - **Direct Consist Powering**: Automatically feeds power to coupled Electric Bullet Trains in the consist.
  - **Trackside & Adjacent Energy Distribution**: Emits energy to adjacent blocks, cables, conduits, and machines beside the rail.
  - **Wireless Proximity Broadcast**: Broadcasts power wirelessly to nearby energy blocks/cables within a configurable radius (default 3 blocks, up to 16 blocks).
  - **Tender Siphoning & Battery Charging**: Siphons water continuously from coupled Liquid Minecart tenders and charges battery items placed in GUI slot 3.
- **Braking / Holding Rail (`braking_rail`)**:
  - Specialty track for station stops, freight depots, and automated siding yards.
  - **Unpowered State**: Firmly clamps and freezes passing or resting carts and train consists in place, zeroing velocity and cutting locomotive drive.
  - **Powered State**: Releases brakes and gently launches resting carts or resumes coupled train travel.
  - Powers and links smoothly with Control Rails and standard powered tracks.
- **Trackside Cargo Loader & Cargo Unloader (`cargo_loader` / `cargo_unloader`)**:
  - Automated trackside solid item transfer stations placed directly beneath rails.
  - Equipped with an internal 9-slot inventory buffer for piping or hopper connections.
  - **Advanced Filtering Controls**: Item filter slot (Whitelist / Blacklist modes) and custom train/cart name filtering.
  - **Transfer Thresholds**: Configurable for Complete Transfer (wait until 100% full/empty) or Partial Transfer.
  - **Braking Rail Synergy**: Automatically depowers overhead/adjacent Braking Rails while loading or unloading is active, holding the cart until transfer is finished.
- **Dedicated Locomotive Fueling Stations (`diesel_fueler` / `coal_fueler`)**:
  - Separated locomotive refueling from general freight logistics.
  - **Diesel Fueler (`diesel_fueler`)**: Pumps diesel/biofuel exclusively into Diesel Locomotives (ignoring general liquid tanker carts).
  - **Coal Fueler (`coal_fueler`)**: Loads coal, charcoal, and solid furnace fuel exclusively into Steam Locomotives (ignoring general cargo minecarts).
  - Regular Cargo and Fluid Loaders/Unloaders strictly ignore locomotive fuel tanks, preventing cargo hubs from draining engine fuel.
- **In-GUI Custom Cart & Locomotive Naming**:
  - Added persistent custom name text fields directly inside the GUIs for all locomotives and rolling stock carts.
  - Cart names seamlessly integrate with Track Signal Switches, Cargo Loaders/Unloaders, and Fluid Loaders/Unloaders for precise automated routing.
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

### Changed
- **Diesel Locomotive Speed Calibration**:
  - Calibrated Gear 2 speed to a realistic **2.25 m/s (0.1125 b/t)** for yard switching.
  - Standardized Cruise mode (Gear 3) to exactly 50% of maximum speed across locomotives.
- **Yard Shunting Safety & Push Rules**:
  - Players can only push unpowered carts along tracks when sprinting or sneaking (holding Shift without a Crowbar), preventing accidental bumps.
  - Post-bump rapid braking: colliding loose carts exchange momentum with active rapid damping (`ROLLING_STOCK_BUMP_DAMPING`), halting both carts shortly after impact.
- Refactored `SurveyorsChargeBlock` scan algorithm from a spherical bounding box to a full-depth vertical cylinder (`dx*dx + dz*dz <= rSq`) extending to `level.getMinY()`.
- Defaulted `minecraft:lava` to ignored survey blocks to prevent cluttering subterranean ore detection reports.
- Modernized Crowbar coupling messages to reflect universal cart placement.

### Fixed
- **Generator Cart Ticking Crash & Energy Transaction Fix**:
  - Fixed `NullPointerException` (`Cannot invoke TransactionContext.depth() because transaction is null`) during generator power production and NBT loading by properly wrapping `SimpleEnergyHandler.insert` inside NeoForge transactions (`Transaction.open(null)`).
- **Generator Cart Dedicated GUI & Slot Alignment**:
  - Replaced the borrowed locomotive GUI with a dedicated, custom-crafted `generator_cart.png` interface (176×166) with pixel-perfect slot boxes and recessed gauge wells.
  - Aligned all interactive slots: Water Input container (x=34, y=22), Water Output bucket (x=34, y=50), Center Fuel (x=80, y=44), and Battery/FE Charging slot (x=126, y=44).
  - Aligned Water Reservoir and RF Energy storage bars with live fill shaders, combustion flame indicator, and centered live status readouts ("⚡ GENERATING", "IDLE (BUFFER FULL)", "⚠ OUT OF WATER", "⚠ OUT OF FUEL").
  - Extended fuel and water bucket slot acceptance and quick-move shift-clicking to support all valid furnace fuels (blaze rods, logs, planks) and water containers.
- **Missing Item Icons & Held Models in 1.21.4**:
  - Added missing Mojang 1.21.4 client item definition files (`assets/miningtools/items/generator_cart.json` and `assets/miningtools/items/track_paver.json`).
  - Resolves purple/black checkerboard fallback icons and missing 3D models in player hands, hotbars, inventory slots, and dropped item stacks.
- **Slope Transition & Downhill Engine Cutoff Fix**:
  - Fixed locomotives shutting off engine drive upon reaching the crest of a slope, going downhill every block, or leveling off onto flat ground at the bottom.
  - Eliminated artificial rail interpolation clamping (`Mth.clamp(progress, 0.0D, 1.0D)`) in `TrainChainingUtil` that clamped carts backward and froze forward progress at block boundaries on slopes.
  - Preserved forward momentum during rail surface snapping: lateral axes are centered on straight and slope tracks while the travel axis progresses smoothly, matching vanilla `OldMinecartBehavior`.
  - Upgraded `isPathBlockedByObstacle` with a comprehensive ±2 vertical tolerance window along the movement direction, eliminating false obstacle detections at slope crests and troughs while reliably detecting dead ends, buffer stops, and obstructed tracks.
  - Refined consist cart movement validation so bounding box contact with slope railbed roadbed blocks does not falsely trip the consist obstacle stall condition.
- **Slope Kinematics & Rail Pinning Overhaul**:
  - Restored authentic Minecraft rail slope geometry (`exit.getY() * 0.5`, `yD * 2.0`, vertical slope offset `+0.5` / `+1.0`), eliminating cart sinking through rails or blocks beneath tracks.
  - Added pre-lift before `cart.move()` to ensure smooth uphill and downhill rail transitions.
  - Removed `noPhysics` clipping hacks and stopped cart terrain phasing.
  - Corrected 45° 3D model slope pitching without spinning or separated coupler spacing on inclines.
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