# 🚂 Track and Train Pack (Track & Train Kit)

**Heavy Rail Logistics, High-Speed Locomotives, Automated Trackside Facilities, Subterranean Excavation, and Geological Prospecting for Minecraft 26.3 (NeoForge)**

---

## 📖 Overview

**Track and Train Pack** transforms Minecraft's minecart and rail system into a complete heavy-haul railway logistics and automated mining network. 

Whether you need to haul massive freight trains across long distances, automate livestock transit, transport bulk liquids with automated locomotive tender siphoning, haul loose bulk aggregates with authentic 3D visual heaps, generate and distribute Forge Energy (FE/RF) via mobile steam dynamo generator cars, drill 3×3 subterranean tunnels automatically, or prospect underground ore and oil deposits with seismic charges—**Track and Train Pack** provides the locomotives, specialty tracks, and smart trackside machinery to get the job done.

> 📖 **Need a complete walkthrough, mechanics guide, or crafting recipes?** Check out the comprehensive **[Track and Train Pack Wiki & Walkthrough](WIKI.md)**!

---

## 🌟 Key Highlights

* 🚂 **3 Distinct Locomotives**: Coal-fired Steam Locomotive, CN Diesel Locomotive, and High-Speed Electric Bullet Train with realistic multi-gear transmissions, headlights, and horns.
* ⚡ **Generator Train Cart**: Steam dynamo / RF generator wagon that consumes coal and water to generate Forge Energy (FE/RF), powering coupled electric trains, trackside cables, and nearby machines wirelessly.
* 🔗 **Universal Consist Coupling & MU Consists**: Link dozens of carts together using the Railroad Crowbar—even without a locomotive! Distribute lead, mid-train, and pusher locomotives to multiply pulling capacity, or shunt strings of cars by hand.
* 📦 **Dynamic Freight & Tankers**: Adaptive Cargo Carts that visually transform (Logs, Shipping Containers in random Red/Green/Blue colorways, Ores, Ingots), 16-bucket Large Liquid Tankers with automatic diesel fuel tender siphoning, and Loose Aggregate Hopper Cars with dynamic 3D bulk material mounds and smooth metallic finishes.
* 🏹 **Tactical Arrow Turret Cart**: Defense cart with 2× engagement range (32 blocks), dual bow mounts, in-GUI Player Whitelist / Blacklist protection, crisp tactical woodland camo livery, and Caboose-matching flanged steel wheels.
* 🗺️ **Live JourneyMap Tracking**: Displays custom locomotive and railcar names directly above train map markers and features server-synced far-distance persistence so trains never vanish off the map across long distances.
* 💥 **Geological Prospecting & Boreholes**: Detonate Surveyor's Charges wirelessly via the Remote Detonator to drill a clean 1×1 borehole to bedrock and receive a full-depth cylindrical column survey tallying ores, minerals, and subterranean oil deposits.
* 🚜 **Heavy Machinery**: Automated 3×3 Tunnel Bore for subterranean excavation and the **Heavy Track & Road Paver** (AP1055F) that crawls at 1 m/s replacing a 1×3 wide strip with selected paver blocks while preserving tracks underneath.
* 🛤️ **Specialty Switch & Power Rails**: Station Delay Rails (Timed Station Dwell & Automatic Consist Launch), Braking / Holding Rails, Coupler Rails (Automated Consist Coupling & Decoupling), Speed Control Rails, 4-way Junction Rails, 3-way Wye Junctions, Turnout Switches, Control Rails, Horn Rails, and Electric Third Rails.
* 📡 **Smart Track Automation & Fueling**: Automated Item Loaders/Unloaders, Fluid Loaders/Unloaders, dedicated Locomotive Fuelers (Diesel & Coal), Entity Loaders/Unloaders, and Train-Name Signal Switches.
* 🏷️ **In-GUI Custom Cart Naming**: Name every locomotive and custom railcar directly in its GUI to automate routing, filtering, and depot switching.
* ⚓ **Zero-Decoupling Chunk Anchors**: Both mobile (Anchor Caboose, placeable anywhere in the train) and stationary (Industrial Chunk Anchor) chunk loaders that keep automation and trains ticking across chunk borders.
* 📖 **Railroader's Field Manual**: Interactive in-game engineering handbook styled after our industrial heavy binder theme, detailing every locomotive, cart, track, switch, station, machine, and tool with complete mechanics, operations, pro tips, and interactive crafting recipes.
* 🧭 **Live HUD Dashboard**: On-screen speedometer showing blocks/sec, km/h, heading, throttle gear, and fuel reserves.

---

## 🚂 Locomotives & Motive Power

Every locomotive features dynamic directional headlights (level 15 lighting), interactive horns, a multi-gear transmission, and a dedicated interactive cockpit GUI.

### 1. Basic Steam Locomotive
The quintessential iron horse for industrial mining and freight operations.
* **Fuel**: Consumes any valid burnable fuel item (coal, charcoal, wood logs, planks, wooden slabs, sticks, blaze rods, lava buckets, coal blocks, etc.), with burn durations scaling proportionally to standard furnace burn times.
* **Speed Profile**: 5 throttle gears with a calibrated **1.0 m/s (0.05 b/t) Crawl Speed** in Gear 1 for precise switching and coupling, scaling up to 0.35+ blocks/tick (~7 m/s or 25 km/h).
* **Cruise Control**: Gear 3 runs at **50% of maximum speed** (~0.175 b/t).
* **Pulling Capacity**: Pulls up to **5 carts** with zero drag penalty.
* **Whistle & Cowcatcher**: Sounds a loud steam whistle that scatters mobs off the tracks and pushes obstacles out of the way.

### 2. CN Diesel Locomotive (#3087)
A heavy-duty mainline diesel road-switcher modeled after Canadian National (CN) locomotives.
* **Speed**: **2× Top Speed** of the steam locomotive (0.70 blocks/tick or ~50 km/h).
* **Calibrated Gears**:
  - **Gear 1 (Crawl)**: 1.0 m/s (0.05 b/t) for precise coupling.
  - **Gear 2 (Slow Yard)**: **2.25 m/s (0.1125 b/t)** calibrated for safe siding and yard switching maneuvers.
  - **Gear 3 (Cruise)**: **50% of top speed (0.35 b/t)** for fuel-efficient long-distance cruising.
  - **Gear 4 (Express)**: 80% of top speed (0.56 b/t).
  - **Gear 5 (High-Ball)**: 100% full throttle (0.70 b/t).
* **Fuel**: Liquid diesel fuel. Equipped with an internal **16,000 mB (16-bucket)** fuel tank with a golden-yellow fuel gauge and bucket draining slot.
* **Fuel Compatibility**: Native support for **Industrial Refinery Diesel**, **Ultimate Car Mod Bio-Diesel**, **Logistics Bio-Fuel**, and all fluids tagged `#c:diesel`, `#c:biofuel`, or `#c:biodiesel`.
* **Automated Fuel Tender Siphoning**: Coupling a Liquid Minecart or Large Liquid Minecart behind or ahead of the locomotive turns the tanker into an automated fuel tender wagon, siphoning up to 100 mB/tick to keep the engine running across continental distances.
* **Pulling Capacity**: Pulls up to **10 carts** with zero drag penalty.
* **Multiple-Unit (MU) Chaining**: Chain multiple diesel locomotives together (front, middle, or rear) to pull massive 20–30+ car trains at maximum speed!

### 3. Electric Bullet Train
Sleek, aerodynamic, ultra-high-speed electric rail transit.
* **Speed**: **5× Speed** of the steam locomotive (1.75 blocks/tick or ~126 km/h).
* **Cruise Control**: Gear 3 runs at 50% top speed (0.875 b/t).
* **Power**: Runs on Forge Energy (FE/RF) with a **100,000 FE** onboard battery.
* **Electric Rail Pickup**: Draws power directly through trackside **Electric Rails** without stopping, or recharges using battery items in its GUI.
* **Pulling Capacity**: Pulls up to **10 carts** at lightning speeds.

### 4. Creative Executive Diesel Locomotive
A dedicated prestige locomotive built exclusively for Creative Mode builders and test yards.
* **Visual Styling**: Royal Cobalt and Deep Midnight Blue livery with brilliant Gold chevron striping.
* **Infinite Motive Power**: Never runs out of fuel, never requires refueling, and never consumes fluid from inventory or tender carts.
* **Complete Controls**: Full multi-gear transmission (Gears 1–5), instant keybinding ignition, horn, directional headlights, and heavy hauling power.
* **Creative Exclusive**: Accessible directly through the Creative Inventory tabs (no survival crafting recipe required).

---

## 🔗 Train Mechanics, Physics & Railroad Crowbar

* **Railroad Crowbar**: Right-click to link any two carts or locomotives together into an unbroken train. Shift + Right-click to uncouple.
* **Locomotive-Free Consist Coupling**: Rolling stock can be coupled into trains with the Crowbar even without a locomotive attached. Moving or gravity-rolling any linked car moves the entire consist at exact coupler spacing.
* **Authentic Slope Kinematics & Rail Pinning**: Rebuilt using Mojang's exact rail slope geometry (`exit.getY() * 0.5`, `yD * 2.0`, vertical slope offset `+0.5` / `+1.0`), eliminating cart sinking through rails or blocks beneath tracks. Cars pitch smoothly at 45° on mountain grades without spinning or separating.
* **Yard Shunting Rules**: To prevent accidental nudging, players can only push unpowered rolling stock or loose consists when **sprinting** or **sneaking** (holding Shift without a Crowbar).
* **Solid Bumper Collisions & Rapid Halting**: Overrides vanilla's collision cancellation bug (`vec3.dot(vec31) < 0.8`), ensuring uncoupled cars make solid bumper contact. Post-bump rapid braking actively dampens momentum, bringing colliding loose carts to a clean halt shortly after impact.
* **Distributed Locomotive Traction**: Each locomotive adds its pull allowance to the train. When pulling more carts than the combined allowance, trains realistically experience drag slowdown.
* **Heavy-Rail Cowcatcher Deflection**: Operating trains deflect mobs and players safely aside without stalling, freezing, or bouncing the train.
* **Non-Inverting Reverse**: Reversing train direction shifts motion into reverse without disorienting 180° visual cart flips.

---

## 📦 Rolling Stock & Freight

### Adaptive Cargo Minecart
A heavy-duty freight wagon with 27 storage slots, authentic Caboose-matching flanged steel wheels, and dynamic content detection (plus an optional **Filter Slot**). The cart's 3D model automatically adapts to its cargo:
* **Log Mode**: Four vertical timber stakes loaded with stacked round logs (automatically rendered whenever the cart holds only logs).
* **Plank / Pallet Mode**: Bundles of milled lumber neatly strapped atop dual 3D wooden shipping pallets complete with top slats, stringers, and fork pockets (automatically rendered whenever the cart holds only planks).
* **Container Mode**: Heavy enclosed shipping container (sea can) featuring dynamic container colors (**Industrial Red**, **Forest Green**, or **Deep Blue**) rendered for mixed or general cargo.
* **Ore Mode**: Raw unrefined ores mounded high.
* **Ingot Mode**: Stacked precious metal ingots.

### Loose Aggregate Hopper Car (Hopper Minecart)
A heavy-duty open-top hopper car designed for hauling bulk loose aggregates (modeled after North American TIMX and Herzog ballast hoppers) with Caboose-matching flanged steel wheels:
* **Smooth Metallic Finish**: Sleek, uniform solid industrial steel finish (`#737A84`) across all bulk material variants.
* **Dynamic 3D Heap Mounds**: Dynamically renders 3 mounded heaps inside the container when hauling loose bulk materials (Coal heaps, Gravel/ballast heaps, yellow Sand heaps, Red Sand heaps, Dirt/clay heaps, and raw metal Ore heaps including all raw ores, block ore versions, and tin).
* **Empty Chute View**: Renders as a hollow container displaying internal sloped discharge chutes when cleared.
* **Filter Slot**: Dedicated hopper filter slot locks acceptance to the specified aggregate type.
* **Track-Aligned Placement**: Automatically aligns initial yaw to railway tracks upon placement.

### Generator Train Cart (Mobile Steam Dynamo)
A heavy-duty industrial steam dynamo and electrical generation wagon with authentic flanged steel wheels and a 3D generator model (under 1.5 blocks tall):
* **Inputs**: Accepts solid furnace fuels (Coal, Charcoal, Coal Blocks, Blaze Rods, Logs) and water (internal 8,000 mB reservoir).
* **Generation Rate**: Produces **100 FE/t** (configurable) stored in an internal **100,000 FE** battery buffer.
* **Dynamic Throttling**: Automatically preserves fuel by halting combustion whenever the internal battery buffer is saturated or water is empty.
* **Consist Powering**: Automatically feeds Forge Energy directly into coupled Electric Bullet Trains in the consist.
* **Adjacent & Trackside Transfer**: Transmits energy directly into adjacent power cables, conduits, and machines beside the rail.
* **Wireless Broadcast**: Wirelessly radiates power to nearby energy receivers/cables within a configurable radius (default 3 blocks, up to 16 blocks).
* **Water Siphoning & Item Charging**: Siphons water continuously from coupled Liquid Minecart tenders and charges battery items placed in GUI slot 3.

### Livestock Cart (Railroad Stock Car)
An authentic North American railroad stock car modeled to scale with rolling stock:
* **Flicker-Free Model Geometry**: Overhauled model structure eliminating coplanar face Z-fighting on slats, doorways, and bulkheads, paired with Caboose-matching flanged steel wheels.
* **Dual Stall Capacity**: Comfortably accommodates up to **2 entities** simultaneously across distinct front and rear stalls.
* **Open Slatted Wooden Construction**: Built with horizontal wooden slats, keeping livestock well-ventilated and clearly visible as the train travels.
* **Leash Hitching**: Right-clicking the cart while leading an animal on a Lead instantly unleashes and boards the animal inside.
* **Smart Facility Compatibility**: Integrates directly with the **Trackside Entity Loader** and **Trackside Entity Unloader**.
* **Activator Rail Ejection**: Rolling over a powered Activator Rail gently dismounts passengers at destination pens.

### Liquid Minecarts (Small & Large)
Specialized tanker wagons designed to safely transport fluids across rail lines.
* **Small Liquid Minecart**: 8,000 mB (8 Buckets) capacity.
* **Large Liquid Minecart**: 16,000 mB (16 Buckets) heavy rail tanker.
* **Fuel Tender Capability**: Automatically acts as a fuel tender when coupled to the CN Diesel Locomotive or water tender for the Generator Cart.
* Seamlessly loads and unloads via the **Trackside Fluid Loader & Unloader**.

### Arrow Turret Cart
An armored rolling-stock defense wagon styled with a crisp, ultra-high-resolution tactical woodland camouflage livery, Caboose-matching flanged steel wheels, and heavy armor plating to protect trains from hostiles and raiders:
* **2× Extended Engagement Range**: Scans and acquires targets within a **32-block radius** (doubled from 16m), raining rapid projectile defense from long range.
* **Dual Weapon Slots**: Mount up to **2 Bows** into dedicated gun ports on the rotating turret.
* **9-Slot Ammo Magazine**: Carries up to 9 stacks of arrows (standard, tipped, or spectral arrows).
* **Interactive Player Whitelist / Blacklist**: In-GUI filter toggle button (`[Whitelist]` / `[Blacklist]`) and comma-separated player name input box. Whitelist allies and friendly players so the turret protects them while shooting enemies, or blacklist specific rival players for perimeter defense!
* **Target Filtering**: Automatically ignores peaceful animals, villagers, and passenger mobs on trains.
* **Dynamic Turret Rotation**: The armored cupola smoothly tracks and swivels towards active targets.

### Anchor Caboose
The ultimate train-protection device attached to your consist.
* **Universal Placement**: Can now be placed at the front, middle, or rear of the train consist!
* **Mobile Chunk Loading**: Forces a 3×3 chunk perimeter around itself **plus** keeps all chunks occupied by the entire train loaded in memory as it travels.
* **Prevents Runaway Decoupling**: Eliminates trains freezing or separating across chunk boundaries.
* **Atmospheric Visuals**: Features an authentic stove pipe chimney puffing smoke particles.

---

## 💥 Geological Prospecting & Survey System

### Surveyor's Charge
A specialized seismic explosive for subterranean prospecting and rapid vertical shaft excavation:
* **Full-Depth Cylindrical Column Survey**: Scans a cylindrical column (default 10-block radius) from the charge placement level all the way down to bedrock (`level.getMinY()`).
* **Crude Oil & Fluid Detection**: Aggregates and reports oil fluids and source blocks (`#c:oil`, `#c:crude_oil`, petroleum, diesel, fuel).
* **Borehole Blasting**: Detonation cleanly excavates a 1×1 vertical shaft to bedrock (with configurable item drops or clean destruction).
* **Survey Chat Telemetry**: Sends a comprehensive ore and resource tally directly into your chat.

### Remote Detonator
A handheld wireless detonator keyed to trigger Surveyor's Charges:
* **Wireless Activation**: Right-click to detonate all Surveyor's Charges within a 12-block radius.
* **Instant Relay**: Relays survey telemetry directly to the triggering player.

---

## 🚜 Heavy Machinery: The Tunnel Bore & Track Paver

### 1. Automated Tunnel Bore
An automated subterranean excavator for carving out subway lines and railway corridors.
* **3×3 Excavation**: Automatically drills a 3-block-wide by 3-block-tall tunnel directly forward.
* **Top-Down Drilling**: Breaks blocks from top to bottom so sand and gravel fall safely into the drill path rather than suffocating the machine.
* **Automated Track Laying**: Automatically places tracks ahead of itself on solid ground from its 9 dedicated track slots.
* **Ballast Inventory & Auto Gap-Bridging**: Features a dedicated 9-slot Ballast Inventory accepting **gravel, sand, cobblestone, and stone**. When encountering chasms, potholes, or lava pools, it automatically places ballast to fill the void, beds the track foundation, and continues drilling without stopping!
* **Safety Sensors**: Automatically halts if it encounters bedrock, chasms exceeding ballast reserves, or runs out of track or fuel.
* **Bi-Directional Operation**: Forward drilling mode and slow, safe reversing mode that reverses trailing cars seamlessly.

### 2. Heavy Track & Road Paver (AP1055F)
A heavy industrial caterpillar track paver machine inspired by the CAT AP1055F track paver (~2.75 blocks wide, 2.0 blocks high, 3.0 blocks long):
* **Work Mode & Dual-Speed Transits**: Toggle between **Work Mode** (constructs a 3-wide road strip at a regulated 1.0 m/s) and **Transport Mode** (suspends block replacement and adjusts travel speed to **2.0 m/s** for rapid transit and repositioning).
* **Physical Collision & Terrain Step Safety**: Solid physical collision box prevents clipping; smoothly climbs and descends 1-block terrain steps while safely halting before 2+ block vertical obstacles, cliffs, and low ceilings.
* **Toggleable Work Light**: High-intensity floodlight illuminating the surrounding workspace (Level 15 light) via GUI button or `L` key.
* **3x3 Ghost Hologram Alignment Grid**: In-world holographic wireframe grid projected over the 3x3 footprint in front of the machine, showing the exact blocks that will be replaced along its trajectory to effortlessly line up target work areas (toggleable via GUI or `Shift + H`).
* **Universal Fuel Compatibility**: Accepts all burnable furnace fuels (coal, charcoal, wood, blaze rods, lava buckets, etc.) in its fuel hopper.
* **Calibrated 1.0 m/s Paving Speed**: Crawls forward at an exact 1.0 m/s (20 ticks per block step), replacing blocks in a 1-block deep by 3-block wide strip across its rear screed.
* **Flush Ground Alignment**: Sits firmly flush against terrain and rail ties without visual hovering off the ground whether operating on-rail or traversing cross-country.
* **Accurate Idle HUD & Audio**: Speedometer accurately displays 0.0 m/s when out of fuel or stopped, and engine sound smoothly mutes when idle or powered off.
* **Track Preservation Under Rails**: When paving under existing rail lines, it safely replaces foundation blocks beneath the track **without breaking, disturbing, or popping the rails off**!
* **On-Rails & Off-Rails Traversal**: Does not require tracks to run! Drives smoothly over solid terrain with 1.0-block climbing step-up, but automatically snaps to rail center and orientation when placed on or driven over tracks.
* **Cardinal Direction Lock**: GUI toggle allows locking travel strictly to **North, South, East, or West** to lay dead-straight road and railbed corridors without angular drift, or unlocking for manual steering.
* **Manual & Automated Operations**: Mount the elevated right-side operator station to drive manually (WASD steering, R for reverse, Space to toggle auto-cruise, C for cockpit GUI), or engage automated forward paving mode.
* **3-Tile Pattern Programming & Material Hopper**: 3 dedicated roadbed pattern filter slots (Left, Center beneath track, Right) to program custom multi-material roadbeds (e.g. curb-ballast-curb or hazard stripes) + dedicated Fuel slot + 27 bulk material storage slots holding up to 1,728 paving blocks.

### 3. Track Layer Cart (`track_layer_cart`)
A compact, rugged, industrial 1-block machine built with a custom monolithic 3D model (armored unibody chassis, protective overhead roll-cage canopy, recessed operator cockpit, front track guide chute, rear articulated retrieval claw, and flanged wheels):
* **Dual Work Modes**:
  - **Track Laying Mode**: Snaps to minetrack and places 1 piece of track from its inventory down in front of itself onto sturdy ground as it rolls forward, paving railway lines effortlessly. Automatically climbs and descends 1-block terrain steps, placing linked slope rails up hills and down valleys!
  - **Track Removing Mode**: Drives along existing railway lines (including ascending and descending slopes) and cleanly retrieves the track behind it after traveling over it, picking up rails and placing them directly into its inventory.
* **Calibrated 1.5 blocks/s Work Speed**: When operating in Laying or Removing Mode, travels at an exact governed speed of **1.5 blocks/s** (0.075 blocks/tick) for safe, dependable track deployment and retrieval.
* **Normal Transport Mode**: When work mode is toggled OFF, acts as a normal minecart—rolling freely, responding to powered rails, slopes, and pulling engines, reaching full standard minecart speeds with driver propulsion.
* **Fully Driveable & Autonomous**:
  - Mount directly to steer and drive with `W` (Forward), `S` (Stop/Brake), `A`/`D` (Steer Left/Right), `R` (Reverse direction), `H` (Cycle Laying / Removing / Transport modes), and `C` (Open GUI).
  - Features an interactive GUI dashboard to monitor stored rails, switch travel direction, toggle between Laying, Removing, or Transport modes, and set custom vehicle names.
* **Consist Material & Resource Sharing**:
  - Carts that use material (coal/solid fuel, diesel fluid, tracks, ballast, paver blocks) can now receive from and place into carts they are linked to in the train consist!
  - In Laying Mode, the Track Layer Cart automatically pulls rails from connected supply carts (like a linked Cargo Minecart) when low on track.
  - In Removing Mode, it automatically deposits collected rails into connected storage wagons if its internal inventory fills up.
* **Large Track Capacity**: 1 Pattern / Preferred Rail Slot + 27 bulk track storage slots (holding up to 1,728 rail pieces).

---

## 📡 Track Automation, Facilities & Map Tracking

### Trackside Cargo Loader & Unloader
Placed directly underneath tracks to automate solid freight transfer:
* **Cargo Loader (`cargo_loader`)**: Automatically loads items from its 9-slot internal buffer into passing Cargo Minecarts and Hopper Cars. Fully connects with modded item pipes, hoppers, and automated logistics.
* **Cargo Unloader (`cargo_unloader`)**: Empties items from passing freight carts into its internal buffer for item pipe, conduit, or hopper extraction.
* **Advanced Filtering**: Features an Item Filter slot with **Whitelist / Blacklist** toggles and a custom Train/Cart Name filter.
* **Threshold & Delay Control**: Configurable to hold carts until **100% Full / Empty**, allow partial transfers, and configure a post-transfer release delay (0s, 1s, 2s, 3s, 4s, 5s, 10s) to keep trains held while items trickle in from pipes before departing.
* **Braking Rail Synergy & Single-Car Isolation**: Automatically depowers overhead Braking Rails to hold the cart stationary until transfer is complete! Precisely targets only the cart directly occupying the station rail, never accidentally loading or unloading adjacent coupled cars.

### Dedicated Locomotive Fuelers (Fuel Isolation)
Specialized trackside fueling stations that separate locomotive refueling from freight logistics:
* **Diesel Locomotive Fueler (`diesel_fueler`)**: Exclusively pumps diesel/biofuel into Diesel Locomotives. Connects with fluid pipes, buckets, and canisters. Will never fill general cargo liquid tankers.
* **Steam Locomotive Coal Fueler (`coal_fueler`)**: Exclusively feeds coal and furnace fuel into Steam Locomotives. Connects with item pipes and hoppers for automatic coal delivery. Will never deposit coal into freight carts.
* **Fuel Protection**: Standard cargo and fluid stations strictly ignore locomotive operational fuel tanks, preventing freight depots from draining locomotive fuel.

### Trackside Fluid Loader & Unloader
Placed underneath tracks to pump fluids into or drain fluids from Liquid Minecarts.
* **16,000 mB Buffer Tank**: Connects with fluid pipes, buckets, and canisters.
* **Redstone & Braking Integration**: Holds carts via Braking Rails and emits pulses when transfers are finished.

### In-GUI Custom Cart & Train Naming & JourneyMap Integration
All locomotives and rolling stock carts feature editable name fields inside their cockpits and GUIs:
* **Live JourneyMap Integration**: Locomotive and railcar names appear rendered cleanly above their train map markers on JourneyMap!
* **Far-Distance Server Synchronization**: Train coordinates and heading are broadcast by the server, ensuring trains remain permanently visible and tracked across great distances without vanishing outside client render radius.
* **Depot Automation**: Name trains (e.g. `"Ore Express"`, `"Coal Tender #1"`, `"Shuttle-South"`) to integrate directly with the **Track Signal Switch** for automatic turnout switching and route dispatching.
* **Station Filtering**: Filters cargo and fluid loaders to only service designated train names.

### Trackside Entity Loader & Unloader
Placed directly under track to automate livestock, villager, and mob transit:
* **Entity Loader**: Boards nearby entities into empty minecarts/stock cars with configurable radius (1–16) and Whitelist/Blacklist/Age filters.
* **Entity Unloader**: Safely dismounts passengers and teleports them to adjacent solid ground away from the tracks.

### Track Signal Switch
A mechanical trackside switch stand with an animated signal lantern and arm.
* **Automated Multi-Criteria Routing**: Define routing rules conditioned on **Train Name**, **Travel Direction** (North, South, East, West, or All), **Cargo Status** (Loaded, Empty, or Any), and **Locomotive Fuel Level** (Low/Adequate thresholds: `< 25%`, `< 50%`, `> 25%`, `> 50%`).
* **Bidirectional & Wye Loop Conflict Resolution**: Allows returning and reversing trains (such as unloaded ore trains heading back from spur tracks) to automatically set the opposite switch state instead of getting trapped or misrouted.
* **Refueling Siding Automation**: Trains with low fuel or battery reserves automatically divert into coaling stations, diesel fuelers, or charging sidings while fully fueled trains continue along the mainline.
* **Interactive Configuration GUI**: Sneak-right-click to open an expanded dashboard featuring one-click condition cycle buttons, visual criteria badges (`[Dir]`, `[Cargo]`, `[Fuel]`), sensitivity range controls, and manual track toggles.
* **Manual & Redstone Modes**: Can also be toggled manually or via redstone wire.

### Industrial Chunk Anchor
A stationary, heavy-duty chunk loader block for mines, quarries, mob farms, and factories.
* **Configurable Area**: Cycle between **1×1 Chunk (Single)**, **3×3 Chunks (9 Total)**, or **5×5 Chunks (25 Total)**.
* **Redstone Modes**: Toggle between **Always Active** and **Redstone Required**.
* **Visual Status**: Glowing cyan energy core, green status LED, light level 12, and portal particles.

### Railway Buffer Stop (Stop Block)
A heavy-duty end-of-track railway barrier modeled after authentic industrial buffer stops:
* **True Rail Clamping**: Dual A-frame structural steel legs clamp onto incoming tracks at standard rail gauge.
* **Heavy Timber Bumper & Pads**: Massive rustic creosote beam with metal end straps, dual vulcanized rubber impact bumper pads, and a red/white reflective center warning plate.
* **Safe Physical Halting & Anti-Bunching**: Stops incoming minecarts, freight cars, and locomotives immediately on contact with zero clipping, zero bouncy derailing, and zero bunching/telescoping even when pushed from the rear by a locomotive.
* **Full Consist Auto-Shutdown & Smooth Departure**: When struck by any cart in a train or by a running locomotive or tunnel bore, it safely cuts throttle and stops all engines across the consist. Once stopped, trains can immediately engage departure throttle and pull away smoothly without false shutdowns or inching.
* **Terminal Redstone Detection**: Emits a continuous redstone signal (strength 15) while a train or cart is resting against the buffer, perfect for terminal station automation and signaling.
* **Crowbar Reorient**: Right-click or Sneak+Right-Click with the Railroad Crowbar to quickly rotate facing direction.

---

## 🛤️ Specialty Switch & Power Rails

* **Braking / Holding Rail**: Clamps and freezes passing or resting carts and locomotives when unpowered (zero velocity). Releases brakes and gives a launch impulse when powered.
* **Junction Rail**: True 4-way perpendicular flat cross rail (+). Minecarts and pushed/pulled train consists cross over perpendicularly without turning or derailing, maintaining momentum and approach heading straight through the intersection.
* **Turnout Rails (Left & Right)**: Dedicated mainline/divergent switch tracks (`Turnout Rail (Right)` and `Turnout Rail (Left)`) toggled via redstone, Track Signal Switches, or hand/crowbar interaction, with dynamic articulated switch blades. Dedicated left and right track blocks ensure immediate, predictable routing upon placement, and can be converted 1:1 in crafting grids. Sneak + right-click inverts branch direction; right-click with Railroad Crowbar rotates track facing 90° clockwise.
* **Wye Rail**: Triangular 3-way junction rail with visual active-branch indication, full 3-way directional routing (straight mainline pass-through alongside active branch routing), and dynamic top mainline rail completion for seamless straight-through traffic in both switch states.
* **Locomotive Control Rail**: Redstone-powered dispatch rail that automatically starts, halts, or reverses approaching locomotives.
* **Locomotive Horn Rail**: Pressure-sensitive rail that triggers locomotive whistles and horns (ideal for level crossings and tunnel entrances).
* **Coupler Rail**: Toggleable specialty track that automatically couples or uncouples locomotives and rolling stock carts upon arrival, with single-trigger trip protection.
* **Speed Control Rail**: Specialty track with an interactive configuration GUI to set locomotive speed/gears (0 = Stop, 1–5 = Forward/Reverse gears), direction modes (Keep, Force Forward, Force Reverse, or Invert/Switch coming direction), and selective train name filtering. Only activates when a locomotive touches the rail, ignoring other carts in a consist.
* **Electric Rail**: Conductive third rail that transmits Forge Energy (FE) along your network to charge Electric Locomotives on the fly.

---

## ⌨️ Controls & Keybindings

When seated inside a locomotive or coupled cart:

| Action | Default Key | Description |
| :--- | :---: | :--- |
| **Throttle Up / Start** | `W` | Engages engine and starts locomotive forward |
| **Brake / Stop** | `S` | Disengages throttle and applies brakes |
| **Gear Down** | `A` | Decreases target speed level (Gears 1–5) |
| **Gear Up** | `D` | Increases target speed level (Gears 1–5) |
| **Reverse** | `R` | Reverses locomotive travel direction |
| **Horn / Whistle** | `H` | Sounds horn, scares nearby mobs off track |
| **Headlight** | `L` | Toggles dynamic directional headlight |
| **Vehicle GUI** | `C` | Opens the vehicle dashboard, naming menu, and fuel inventory |

---

## 🔨 Crafting Overview

| Item | Primary Ingredients |
| :--- | :--- |
| **Railroad Crowbar** | Iron Ingots + Iron Nugget |
| **Basic Locomotive** | Furnace + Minecart + Iron Ingots + Redstone |
| **CN Diesel Locomotive** | Iron Blocks + Glass + Redstone Block + Bucket + Minecart |
| **Electric Bullet Train** | Iron Block + Copper Block + Redstone Block + Tinted Glass + Minecart |
| **Tunnel Bore** | Iron Pickaxes + Iron Block + Furnace Minecart + Chest Minecart + Iron Ingots |
| **Heavy Track & Road Paver** | Iron Shovels + Iron Block + Blast Furnace + Chest Minecart + Iron Ingots |
| **Track Layer Cart** | Iron Ingots + Iron Blocks + Piston + Rail + Chest Minecart |
| **Cargo Minecart** | Chest + Minecart + Iron Ingot |
| **Loose Aggregate Hopper Car** | Iron Ingots + Hopper + Chest Minecart |
| **Generator Train Cart** | Copper Ingots + Redstone + Iron Ingots + Blast Furnace + Minecart |
| **Livestock Cart** | Wooden Planks + Wooden Fences + Lead + Minecart |
| **Liquid Minecart (8-Bucket)** | Copper Ingot + Bucket + Iron Ingots + Minecart |
| **Large Liquid Minecart (16-Bucket)** | Liquid Minecart + Bucket + Iron Ingots |
| **Arrow Turret Cart** | Bows + Dispenser + Iron Ingots + Minecart |
| **Anchor Caboose** | Ender Pearl + Lantern + Iron Ingots + Minecart |
| **Braking / Holding Rail (Yields 6)** | Iron Ingots + Cobblestone + Stick + Redstone |
| **Junction Rail (Yields 4)** | Standard Rails + Iron Ingot |
| **Turnout Rail - Right (Yields 4)** | Standard Rails + Lever (Center) |
| **Turnout Rail - Left (Yields 4)** | Standard Rails + Lever (Left) or 1:1 from Right Turnout |
| **Wye Rail (Yields 4)** | Standard Rails + Redstone Torch |
| **Electric Third Rail (Yields 6)** | Copper Ingots + Redstone Dust + Sticks |
| **Locomotive Control Rail (Yields 6)**| Iron Ingots + Coal + Redstone Dust |
| **Locomotive Horn Rail (Yields 6)** | Iron Ingots + Gold Ingot + Redstone Dust |
| **Coupler Rail (Yields 6)** | Iron Ingots + Iron Chain + Redstone Dust |
| **Speed Control Rail (Yields 6)** | Iron Ingots + Clock + Redstone Dust |
| **Creative Executive Diesel Locomotive** | *Creative Mode Only (No crafting recipe needed)* |
| **Trackside Cargo Loader** | Iron Ingots + Hopper + Chest + Redstone Dust |
| **Trackside Cargo Unloader** | Iron Ingots + Chest + Hopper + Redstone Dust |
| **Diesel Locomotive Fueler** | Iron Ingots + Bucket + Gold Ingots + Hopper + Redstone Dust |
| **Steam Locomotive Coal Fueler** | Iron Ingots + Blast Furnace + Coal Blocks + Hopper + Redstone Dust |
| **Trackside Fluid Loader** | Iron Ingots + Piston + Copper Ingots + Hopper + Redstone Dust |
| **Trackside Fluid Unloader** | Iron Ingots + Bucket + Copper Ingots + Hopper + Redstone Dust |
| **Trackside Entity Loader** | Iron Ingots + Lead + Copper Ingots + Hopper + Redstone Dust |
| **Trackside Entity Unloader** | Iron Ingots + Activator Rail + Copper Ingots + Hopper + Redstone Dust |
| **Track Signal Switch (Yields 2)** | Iron Ingots + Redstone Torch + Lever |
| **Railway Buffer Stop (Yields 2)** | Iron Ingots + Wooden Log + Redstone Dust + Iron Bars |
| **Railway Block Signal (Yields 2)** | Iron Ingots + Redstone Dust + Green Dye |
| **Signal Tuner Radio** | Iron Ingots + Redstone Torch + Copper Ingot |
| **Industrial Chunk Anchor** | Iron Ingots + Obsidian + Gold Ingots + Ender Pearl + Redstone Block |
| **Surveyor's Charge** | TNT + Redstone Dust + Arrow |
| **Remote Detonator** | Lever + Redstone Dust + Iron Ingot |

---

## 📋 Changelog & Releases

For detailed release notes across versions, check out **[CHANGELOG.md](CHANGELOG.md)**.
* **v1.1.31**:
  - **3D Isometric Pixel-Art Item Icons for Switch, Junction, Control & Electric Rails**:
    - Converted item icons for Junction Rail, Locomotive Control Rail, Turnout Rail (Right), Turnout Rail (Left), Wye Rail, and Electric Rail to 32×32 3D isometric pixel-art sprites with wooden sleeper ties, raised steel rails, and distinctive central apparatus/frog detailing.
    - Updated item model JSONs (`control_rail`, `electric_rail`, `junction_rail`, `turnout_rail`, `left_turnout_rail`, `wye_rail`) to use the dedicated 32×32 3D pixel-art item textures.
* **v1.1.30**:
  - **Railroader's Field Manual Item Icon Overhaul**:
    - Replaced the 16×16 black guidebook icon with a custom 32×32 pixel-art crimson red railway engineering binder with reinforced spine staples, polished brass corner brackets, cream paper edging, dangling gold bookmark ribbon, and an embossed golden locomotive & track insignia.
* **v1.1.29**:
  - **3D Blockbench Rail Models Overhaul**:
    - Replaced flat 2D vanilla `rail_flat` block models for Locomotive Horn Rail, Coupler Rail, Speed Control Rail, and Station Delay Rail with full 3D HD Blockbench models matching Braking/Holding Rail, Turnout Rails, and Electric Rails.
    - Added 3D central apparatus modules: cast-iron tachometer housing (Speed Control Rail), dual brass trumpet horns with active acoustic soundwaves (Horn Rail), heavy shunting knuckles with glowing green/red indicators (Coupler Rail), and octagonal brass chronometer with amber dwell glow (Delay Rail).
    - Upgraded block textures and added 7 dedicated 128×128 HD device textures in `cmodels/`, plus full 3D slope models for all elevation changes.
* **v1.1.28**:
  - **Track Paver 3-Tile Pattern Programming**:
    - Added 3 roadbed pattern filter slots (Left, Center beneath track, and Right) to the Heavy Track & Road Paver to lay customized 3-tile roadbed designs (e.g. curb-ballast-curb or hazard stripes).
    - Leaving outer pattern slots empty leaves existing terrain untouched for 1-wide or 2-wide roadbeds; leaving all 3 empty falls back to bulk storage.
    - Added automated inventory migration for existing world saves (`PaverVersion` = 0 -> 1).
    - Reorganized paver GUI with dedicated Fuel column, side-by-side pattern slots, status buttons, and interactive hover tooltips.
    - Updated the Railroader's Field Manual with 3-tile paver pattern configuration instructions.
* **v1.1.27**:
  - **Standardized Rail Block Textures & 3D Isometric Item Icons**:
    - Re-textured Speed Control Rail, Locomotive Horn Rail (idle & active), Coupler Rail (couple & decouple), and Station Delay Rail (idle & holding) with matching steel rail and sleeper ties geometry.
    - Upgraded item icons for Braking & Holding Rail, Locomotive Horn Rail, Coupler Rail, and Station Delay Rail to 32×32 3D isometric pixel-art icons matching the trains and rolling stock.
* **v1.1.25**:
  - **Station Delay Rail (`delay_rail`)**:
    - Added an interactive dwell timer rail that holds trains and consists for a set duration before automatically launching them onward.
    - Features in-GUI timer toggle, duration configuration (1s to 3600s) with 3s/5s/10s/15s/30s/60s quick presets, departure direction mode (maintain heading, reverse, or cardinal direction), selective train name filtering, and dynamic GUI countdown display.
    - Clamps brakes across entire consists during station dwell and provides clean launch impulse on release with trailing wagon cooldown prevention.
    - Documented in the **Railroader's Field Manual** under `Tracks & Switches`.
* **v1.1.23**:
  - **Railroader's Field Manual (`guide_book`)**: Added the official comprehensive in-game engineering handbook styled after our industrial heavy binder theme. Features dark industrial slate aesthetics, riveted steel borders, brass brackets, 7 category tabs, dynamic topic counters, overview manuals with pro-tips, and interactive recipe cards with item tooltips.
  - **Block Drop Loot Tables Fix**: Added missing block loot tables for Speed Control Rail, Coupler Rail, and Surveyor's Seismic Charge so they reliably drop their corresponding item when harvested.
* **v1.1.22**:
  - **Multi-Criteria Directional Track Switch Routing**: Added Travel Direction filtering (N/S/E/W), Freight Cargo state filtering (Loaded/Empty), and Locomotive Fuel/Battery threshold filtering (<25%, <50%) to Track Signal Switch rules, plus an expanded 264x224 configuration GUI with condition badges.
* **v1.1.21**:
  - **Speed Control Rail Approach & Obstacle Detection Fix**: Added speed control and coupler rails to vanilla rail and pickaxe tags, eliminated false-positive obstacle shutdowns, and added instantaneous directional velocity application.
* **v1.1.20**:
  - **Dedicated Left Turnout Rail**: Added a standalone `Turnout Rail (Left)` block and item that branches left upon placement. Turnout Rails are now distinctly available as `Turnout Rail (Right)` and `Turnout Rail (Left)`, and can be converted back and forth 1:1 in any crafting grid.
  - **Switch & Redstone Integration**: Left Turnout Rails automatically link with adjacent Track Signal Switches and respond to redstone power, dynamically switching articulated points to the left divergent curve.
  - **Railroad Crowbar Interaction**: Added dedicated crowbar interaction handling (`useOn`) on all turnout rails.
* **v1.1.19**:
  - **Turnout Rail Sneak Invert Fix**: Sneak + Right-clicking a Turnout Rail with an empty hand (or crowbar) now immediately inverts the curve branch between Left and Right, allowing players to easily configure left-curving turnouts. Crowbar right-clicking rotates facing 90° clockwise.
  - **Speed Control Rail Direction Invert**: Added a new "Direction: Invert / Switch" mode to the Speed Control Rail GUI that switches locomotive direction from whatever it entered at (reversing forward to reverse or vice versa).
* **v1.1.18**:
  - **Station Adjacent Car Isolation Fix**: Loaders and unloaders now strictly target only the cart centered directly over their rail block. When a caboose, locomotive, or passenger car sits on the station, neighboring coupled cars are completely ignored.
  - **Configurable Train Release Delay**: Added an in-GUI Release Delay toggle (0s to 10s, default 3s) across cargo loaders, coaling stations, and fluid fuelers to hold trains until external pipes finish feeding items/fluids.
* **v1.1.16**:
  - **Speed Control Rail Locomotive-Only Activation**: Speed Control Rails now only activate when an actual locomotive directly touches the track. Other carts in a consist (freight cars, tank cars, chest carts, passenger cars, cabooses, etc.) are ignored when passing over the rail, preventing premature or unintended activations.
* **v1.1.15**:
  - **Track Layer Cart Position Snapping Fix**: Resolved an issue where moving the Track Layer Cart forward in Transport/Off mode and switching back to Laying or Removing Mode snapped the cart backward to where work mode was originally disengaged. Rail positions are now continuously tracked during transport motion and synchronized upon mode switching.
* **v1.1.12**:
  - **Smooth Train Departure from Buffer Stops**: Resolved an issue where trains stopped at a buffer stop would repeatedly shut down or only inch forward when trying to drive away. Added directional motion and consist travel detection (`isCartMovingAway`) across `BufferStopBlock` and `TrainChainingUtil`, allowing trains to start up and pull away cleanly without triggering emergency stops.
* **v1.1.11**:
  - **Railway Buffer Stop Collision & Consist-Wide Stopping**: Buffer Stops now stop the entire train consist whenever any minecart (pushed freight car, lead car, trailing car, or locomotive) hits the buffer stop, immediately cutting engine throttles, stopping tunnel bores, zeroing velocities, and powering terminal redstone.
  - **Anti-Bunching & Obstacle Collision Fix When Pushing**: Resolved an issue where pushing a train from the opposite end into an immovable object (buffer stops, stone walls, dead ends) caused cars to telescope and get tangled. Implemented obstacle collision detection in `TrainChainingUtil` with instant tick rollback that preserves uncompressed cart spacing and shuts down all locomotives across the consist.
* **v1.1.10**:
  - **Junction Rail (Cross Track) Pushing & Travel Direction Fix**: Fixed inverted yaw angle logic on Junction Rails and implemented multi-stage approach direction detection (active velocity, previous tick movement, approach vector relative to junction center, and consist coupling alignment). Updated `TrainChainingUtil` so trains pushed from the rear by a locomotive maintain their forward travel heading across cross intersections instead of veering sideways.
* **v1.1.9**:
  - **Pipe Connectivity for Trackside Cargo & Coal Loaders**: Registered item handler capabilities (`Capabilities.Item.BLOCK`) across Trackside Cargo Loaders, Trackside Cargo Unloaders, and Locomotive Coaling Stations, enabling seamless item pipe connections and automation with mods like Pipez, Mekanism, Thermal, and LaserIO.
  - **Entity Automation Capabilities**: Registered `Capabilities.Item.ENTITY` and `Capabilities.Item.ENTITY_AUTOMATION` on freight carts and locomotives for complete modded automation interoperability.
* **v1.1.8**:
  - **Wye Rail Model & Gap Fix**: Added the missing top straight rail segment dynamically to both Wye switch modes (`wye_rail_right` and `wye_rail_left`), ensuring the continuous straight mainline path is complete and visually seamless in both switch states.
  - **Turnout Track Rotation**: Sneak + Right-click now rotates the Turnout Rail clockwise (matching Wye Rail behavior). Full 360° rotation cycles branch direction, allowing full configuration with bare hands.
  - **Crowbar Turnout Branch Toggle**: Right-clicking a Turnout Rail with the Railroad Crowbar directly toggles divergent branch direction (Left vs. Right) with audible feedback and action bar notification.
  - **Turnout Track Switch Animation**: Overhauled the Turnout Rail visual switch indicator and throw bar to visibly articulate and animate between Normal Mainline (Straight) and Divergent (Curved) switch states, rotating the indicator target 90° and shifting the switch rod.
* **v1.1.6**:
  - **Track Layer Cart Climbing & Descending Slopes**: Implemented automatic 1-block terrain step climbing and descending in Laying Mode, slope tracking in Removing Mode, steering controls (`A`/`D`), and enhanced transport mode propulsion along railway gradients.
* **v1.1.5**:
  - **Track Paver Transport Mode Speed Calibration**: Calibrated Track Paver Transport Mode max travel speed to 2.0 m/s (10 ticks/block) in both forward and reverse, updating HUD speedometer scaling and in-game action bar messages.
* **v1.1.4**:
  - **Cargo Cart Log Texture UV & End Face Transparency Fix**: Realigned texture face regions in `logging_cart.png` and `wood_cart.png` to perfectly match the 4×4 box UV layout, fixing transparent diagonal gaps and flickering on the log ends.
* **v1.1.3**:
  - **Cargo Cart Log Stacking Pole Clipping Fix**: Calibrated round log dimensions to 4×4 cross-section (12.0 units total width) and adjusted nesting groove depths so logs fit squarely between side stakes without clipping through the vertical poles.
* **v1.1.2**:
  - **Dynamic Log & Plank Pallet Cargo Visuals**: Cargo carts now automatically detect held items without requiring a filter—displaying timber stakes with round log stacks when hauling only logs, and stacked lumber bundles atop dual 3D wooden shipping pallets with fork pockets when hauling only planks. Mixed items render as colorful sea can shipping containers.
  - **Comprehensive Raw Ores & Block Ore Version Support**: Added full support for all raw ores and block ore versions (including tin, raw metal blocks, and deepslate/nether ores) to the Hopper Cart with strict non-bulk aggregate filtering and prioritized ore evaluation.
* **v1.1.1**:
  - **Speed & Gear Control Rail Recipe Fix**: Corrected ingredient keys to use direct item identifier strings instead of legacy nested item objects, fixing the recipe parsing crash on world and server startup.
* **v1.1.0**:
  - **Creative Executive Diesel Locomotive**: Creative-only diesel locomotive featuring infinite fuel and Royal Cobalt & Gold livery.
  - **Speed & Gear Control Rail**: Real-time configuration GUI to set locomotive speed/gears (Gears 0 to 5) and direction with selective Train Name filtering.
  - **3D Pixel-Art Icon Consistency**: Crafted 32×32 3D isometric icons matching the Track Layer Cart style for all newly added equipment.
* **v1.0.13**:
  - **Railway Block Signal Icon Fix**: Created a detailed 32×32 3D pixel-art icon for the Railway Block Signal, replacing the blank transparent placeholder with an authentic steel mast, relay box, and 3-aspect signal head.
* **v1.0.12**:
  - **Coupler Rail Recipe Identifier Fix**: Corrected chain ingredient from `minecraft:chain` to `minecraft:iron_chain` for Minecraft 26.3 compatibility, fixing the world load recipe registry error.
* **v1.0.11**:
  - **High-Resolution 3D Pixel-Art Icon Overhaul**: Upgraded all 13 train and railcar item icons (Steam, Diesel, Electric, Caboose, Cargo, Hopper, Livestock, Tankers, Turret, Generator, Tunnel Bore, and Track Paver) to a unified, rich 32×32 pixel-art style matching the Track Layer Cart with authentic details, flanged steel wheels, rails, and crisp outlines.
* **v1.0.10**:
  - **Coupler Rail (`coupler_rail`)**: New specialty track block supporting toggleable **Couple Mode** (scans 16 blocks and couples nearest cart/consist to arriving locomotive) and **Decouple Mode** (disconnects locomotive from its directly attached trailing or pushed car).
  - **Single Execution Per Trip**: Locomotive arrivals execute coupling/uncoupling strictly once per trip, ensuring parked or stationary locomotives never repeatedly trigger while waiting on the rail.
  - **Manual & Redstone Switching**: Right-click to toggle mode manually; redstone power inverts/toggles mode for automated switching in yards.
  - **Entity Loader Passenger Ejection Fix**: Prevented Entity Loader stations from powering overhead activator rails and guarded Livestock Carts from ejecting passengers while resting over loader blocks.
* **v1.0.9**:
  - **Track Layer Cart Item Model & Held Model Fix**: Added missing 1.21.4+ item model definition (`assets/miningtools/items/track_layer_cart.json`) and upgraded item texture to a detailed 32×32 pixel-art sprite, fixing the missing purple/black cube fallback in inventories and when held in hand.
* **v1.0.8**:
  - **Track Layer Cart**: Compact, heavy-duty industrial railway vehicle featuring a custom monolithic 3D unibody model with roll-cage canopy, front guide chute, rear retriever claw, and flanged steel wheels.
  - **Track Laying & Removing Modes**: Snaps to rail and deploys track pieces ahead of itself in Laying Mode, or retrieves track behind it into its inventory in Removing Mode.
  - **Calibrated 1.5 blocks/s Work Speed**: Governed 1.5 blocks/s (0.075 blocks/tick) travel speed during work modes; rolls freely at standard minecart speeds in normal transport mode.
  - **Full Driving Controls & GUI**: Mountable and steerable with keyboard controls (`W`, `S`, `R`, `H`, `C`) and interactive cockpit GUI with direction/mode toggles and custom naming.
  - **Consist Material & Resource Sharing**: Locomotive engines, generator wagons, pavers, bores, and track layers can now dynamically draw and deposit fuel, fluids, and materials to/from coupled carts across the entire train consist.
* **v1.0.7**:
  - **Server Recipe Parsing Fix**: Fixed recipe ingredient syntax and category definitions for Railway Block Signal and Signal Tuner to resolve server data pack loading errors on Minecraft 26.3.
* **v1.0.6**:
  - **Railway Block Signal & Interlocking**: Added 3-aspect steel mast signal posts with dynamic track circuit occupancy detection and analog redstone output.
  - **Signal Tuner Radio**: Handheld wireless radio tool to pair signals across any distance into Block Sections and bind turnout switches.
  - **Automated Passing Sidings**: Automatically diverts oncoming trains into passing loops when the single-track section ahead is occupied, holding them until clear.
  - **Fail-Safe Stopping & Shunting Exemption**: Safely stops trains at Red signals while permitting slow-speed shunting (Gear 1 crawl / Shift key) for wagon coupling in yards.
  - **Proper Item Models & Textures**: Full 1.21.4+ item definitions, models, and clean 16x16 pixel art icons for signals and tuners.
* **v1.0.5**:
  - **Universal Fuel Support**: Basic Locomotives now accept any valid burnable fuel item (wood, planks, sticks, blaze rods, lava buckets, coal blocks, and modded fuels) with proportional burn durations and bucket remainder preservation.
  - **Track Paver Collision & Terrain Safety**: Added a full physical collision box and refined terrain collision checks to safely climb or descend 1-block steps while halting before 2+ block obstacles, cliffs, and overhangs.
  - **Track Paver Work Light**: Added a toggleable high-intensity work floodlight (Level 15) illuminating the work area around the paver (GUI button and `L` hotkey).
  - **Track Paver Work Mode & Dual-Speed**: Toggle between **Work Mode** (paving at 1.0 m/s) and **Transport Mode** (no paving, max speed 5.0 m/s) with dynamic speedometer HUD adaptation.
  - **Track Paver 3x3 Ghost Hologram Grid**: Projected 3x3 alignment wireframe grid showing the exact blocks that will be replaced ahead across all terrain contours.
* **v1.0.4**:
  - **Heavy Track & Road Paver Auto-Shutdown**: Halts crawler movement and cuts engine activity immediately upon exhausting the filter block material in its bulk inventory; stops mid-strip to eliminate unpaved gaps; alerts operators with action bar notices.
  - **Automated Version Bumping**: Build script tracks mod source fingerprints (`.last_source_hash`) and auto-increments the patch version on build/jar whenever mod files are modified.
* **v1.0.3**:
  - **JourneyMap Name Displays**: On-map name badges are now exclusively shown for locomotives (Steam, Diesel, Electric), keeping rolling stock icons clean while retaining hover tooltips.
  - **Junction Rail Visual Continuity**: Extended the 3 middle wooden ties horizontally across the full track width in both the 3D block model and 2D texture.
  - **Bogie Truck & Wheel Consistency**: Standardized Hopper, Cargo, Turret, Generator, Livestock, Liquid Tanker, and Logging carts to Caboose-matching 2-axle bogie trucks and wheel scaling.
* **v1.0.2**:
  - **Turret Cart Overhaul**: Added in-GUI Player Whitelist / Blacklist protection mode to whitelist friendly players or blacklist rival players; doubled engagement range to 32 blocks (2×); applied crisp, non-blurry 1024×1024 woodland camo livery and Caboose-matching flanged steel wheels.
  - **JourneyMap Tracking & Train Names**: Displays custom train and railcar names directly above markers on JourneyMap; implemented server-to-client train map synchronization (`TrainMapSyncPayload`) so trains never disappear off the map when farther than client render distance.
  - **Rolling Stock Texture Standardizations**: Hopper Cars updated to a clean, solid smooth metallic finish (`#737A84`) across all 7 bulk variants; Cargo Carts now roll with dynamic Red, Green, or Blue container paint schemes; updated all rolling stock wheels (Hoppers, Cargo, Livestock, Generator, Ore, Ingot, Wood, Logging) to match the Caboose's flanged steel wheels.
  - **Livestock Cart Z-Fighting Fix**: Eliminated model coplanar flickering across doorways, slats, and end bulkheads.
  - **Track Paver Polishing**: Adjusted renderer translation eliminating ground hovering; calibrated idle speedometer to display 0.0 m/s when stopped or out of fuel; silenced engine sound when idle.
  - **Steam Locomotive Visual Polish**: Removed roof whistle ornament and repositioned front headlight bracket/housing upward to align with the boiler door circular lens.
* **v1.0.1**: Added Heavy Track & Road Paver (AP1055F crawler paver with 1 m/s 1×3 paving, track preservation, cardinal lock & off-rail support), Generator Train Cart (steam dynamo FE generator), Braking / Holding Rail, Trackside Cargo Loaders & Unloaders, dedicated Locomotive Fueling Stations (Diesel & Coal Fuelers), In-GUI Cart Naming, Diesel Gear 2 (2.25 m/s) calibration, 50% Cruise standard, yard shunting push restrictions & post-bump rapid braking, authentic slope kinematics & rail pinning overhaul, Loose Aggregate Hopper Car (with dynamic 3D bulk material mounds), Surveyor's Charge & Remote Detonator (with borehole blasting & full-depth column survey down to bedrock), CN Diesel fuel tender siphoning, universal consist coupling, and NeoForge 1.21.4 server recipe codec fixes.
* **v1.0.0**: Initial release for Minecraft 26.3.

---

## ⚙️ Configuration

Track and Train Pack features extensive configuration via `config/miningtools-common.toml`:
* Locomotive coal burn durations, speed curves, acceleration rates, and pull allowances.
* Diesel tank capacities, burn rates, and top speeds.
* Electric battery capacity, FE/tick consumption, and electric rail transfer rates.
* Generator Cart energy generation rate, battery capacity, water tank capacity, transfer rate, and wireless broadcast radius.
* Rolling stock drag coefficients, bumper momentum transfer, and collision damping.
* Surveyor's charge column radius, ignored blocks, ore reporting lists, and borehole drops.

---

## 📦 Installation & Dependencies

1. Ensure **Minecraft 26.3** and **NeoForge** are installed.
2. Drop `track and trains-1.1.31.jar` into your `.minecraft/mods` folder.
3. *(Optional)* Compatible with **Jade**, **JEI**, **Industrial Refinery**, and **Ultimate Car Mod**.
