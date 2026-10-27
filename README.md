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
* 🛤️ **Specialty Switch & Power Rails**: Braking / Holding Rails, 4-way Junction Rails, 3-way Wye Junctions, Turnout Switches, Control Rails, Horn Rails, and Electric Third Rails.
* 📡 **Smart Track Automation & Fueling**: Automated Item Loaders/Unloaders, Fluid Loaders/Unloaders, dedicated Locomotive Fuelers (Diesel & Coal), Entity Loaders/Unloaders, and Train-Name Signal Switches.
* 🏷️ **In-GUI Custom Cart Naming**: Name every locomotive and custom railcar directly in its GUI to automate routing, filtering, and depot switching.
* ⚓ **Zero-Decoupling Chunk Anchors**: Both mobile (Anchor Caboose, placeable anywhere in the train) and stationary (Industrial Chunk Anchor) chunk loaders that keep automation and trains ticking across chunk borders.
* 🧭 **Live HUD Dashboard**: On-screen speedometer showing blocks/sec, km/h, heading, throttle gear, and fuel reserves.

---

## 🚂 Locomotives & Motive Power

Every locomotive features dynamic directional headlights (level 15 lighting), interactive horns, a multi-gear transmission, and a dedicated interactive cockpit GUI.

### 1. Basic Steam Locomotive
The quintessential iron horse for industrial mining and freight operations.
* **Fuel**: Consumes coal, charcoal, or furnace fuel (default: 80 seconds per coal).
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
A heavy-duty freight wagon with 27 storage slots, authentic Caboose-matching flanged steel wheels, and an intelligent **Filter Slot**. The cart's 3D model automatically adapts to its cargo:
* **Log Mode**: Four vertical timber stakes loaded with stacked wooden logs.
* **Container Mode**: Heavy enclosed shipping container (stakes retract) featuring dynamic container colors (**Industrial Red**, **Forest Green**, or **Deep Blue**).
* **Ore Mode**: Raw unrefined ores mounded high.
* **Ingot Mode**: Stacked precious metal ingots.

### Loose Aggregate Hopper Car (Hopper Minecart)
A heavy-duty open-top hopper car designed for hauling bulk loose aggregates (modeled after North American TIMX and Herzog ballast hoppers) with Caboose-matching flanged steel wheels:
* **Smooth Metallic Finish**: Sleek, uniform solid industrial steel finish (`#737A84`) across all bulk material variants.
* **Dynamic 3D Heap Mounds**: Dynamically renders 3 mounded heaps inside the container when hauling loose bulk materials (Coal heaps, Gravel/ballast heaps, yellow Sand heaps, Red Sand heaps, Dirt/clay heaps, and raw metal Ore heaps).
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
* **Calibrated 1.0 m/s Paving Speed**: Crawls forward at an exact 1.0 m/s (20 ticks per block step), replacing blocks in a 1-block deep by 3-block wide strip across its rear screed.
* **Flush Ground Alignment**: Sits firmly flush against terrain and rail ties without visual hovering off the ground whether operating on-rail or traversing cross-country.
* **Accurate Idle HUD & Audio**: Speedometer accurately displays 0.0 m/s when out of fuel or stopped, and engine sound smoothly mutes when idle or powered off.
* **Track Preservation Under Rails**: When paving under existing rail lines, it safely replaces foundation blocks beneath the track **without breaking, disturbing, or popping the rails off**!
* **On-Rails & Off-Rails Traversal**: Does not require tracks to run! Drives smoothly over solid terrain with 1.0-block climbing step-up, but automatically snaps to rail center and orientation when placed on or driven over tracks.
* **Cardinal Direction Lock**: GUI toggle allows locking travel strictly to **North, South, East, or West** to lay dead-straight road and railbed corridors without angular drift, or unlocking for manual steering.
* **Manual & Automated Operations**: Mount the elevated right-side operator station to drive manually (WASD steering, R for reverse, Space to toggle auto-cruise, C for cockpit GUI), or engage automated forward paving mode.
* **Large Material Hopper**: Dedicated Paver Pattern / Target Block Slot (Slot 0) + Fuel Slot (Slot 1) + 27 bulk material storage slots (Slots 2–28) holding up to 1,728 paving blocks.

---

## 📡 Track Automation, Facilities & Map Tracking

### Trackside Cargo Loader & Unloader
Placed directly underneath tracks to automate solid freight transfer:
* **Cargo Loader (`cargo_loader`)**: Automatically loads items from its 9-slot internal buffer into passing Cargo Minecarts and Hopper Cars.
* **Cargo Unloader (`cargo_unloader`)**: Empties items from passing freight carts into its internal buffer for piping or hopper extraction.
* **Advanced Filtering**: Features an Item Filter slot with **Whitelist / Blacklist** toggles and a custom Train/Cart Name filter.
* **Threshold Control**: Configurable to hold carts until **100% Full / Empty** or allow partial transfers.
* **Braking Rail Synergy**: Automatically depowers overhead Braking Rails to hold the cart stationary until transfer is complete!

### Dedicated Locomotive Fuelers (Fuel Isolation)
Specialized trackside fueling stations that separate locomotive refueling from freight logistics:
* **Diesel Locomotive Fueler (`diesel_fueler`)**: Exclusively pumps diesel/biofuel into Diesel Locomotives. Will never fill general cargo liquid tankers.
* **Steam Locomotive Coal Fueler (`coal_fueler`)**: Exclusively feeds coal and furnace fuel into Steam Locomotives. Will never deposit coal into freight carts.
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
* **Automated Routing**: Define routing rules based on train names (e.g. send "Freight-North" onto side tracks automatically).
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
* **Safe Physical Halting**: Stops incoming minecarts, freight cars, and locomotives immediately on contact with zero clipping and zero bouncy derailing.
* **Engine Auto-Shutdown**: When struck by a running locomotive or tunnel bore, it safely cuts throttle and stops the engine while emitting an authentic impact clatter and spark particles.
* **Terminal Redstone Detection**: Emits a continuous redstone signal (strength 15) while a train or cart is resting against the buffer, perfect for terminal station automation and signaling.
* **Crowbar Reorient**: Right-click or Sneak+Right-Click with the Railroad Crowbar to quickly rotate facing direction.

---

## 🛤️ Specialty Switch & Power Rails

* **Braking / Holding Rail**: Clamps and freezes passing or resting carts and locomotives when unpowered (zero velocity). Releases brakes and gives a launch impulse when powered.
* **Junction Rail**: True 4-way perpendicular flat cross rail (+). Minecarts cross over perpendicularly without turning or derailing.
* **Turnout Rail**: Mainline/divergent switch track toggled via redstone or hand/crowbar interaction, with dynamic animated switch blades.
* **Wye Rail**: Triangular 3-way junction rail with visual active-branch indication and smart directional routing.
* **Locomotive Control Rail**: Redstone-powered dispatch rail that automatically starts, halts, or reverses approaching locomotives.
* **Locomotive Horn Rail**: Pressure-sensitive rail that triggers locomotive whistles and horns (ideal for level crossings and tunnel entrances).
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
| **Turnout Rail (Yields 4)** | Standard Rails + Lever |
| **Wye Rail (Yields 4)** | Standard Rails + Redstone Torch |
| **Electric Third Rail (Yields 6)** | Copper Ingots + Redstone Dust + Sticks |
| **Locomotive Control Rail (Yields 6)**| Iron Ingots + Coal + Redstone Dust |
| **Locomotive Horn Rail (Yields 6)** | Iron Ingots + Gold Ingot + Redstone Dust |
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
| **Industrial Chunk Anchor** | Iron Ingots + Obsidian + Gold Ingots + Ender Pearl + Redstone Block |
| **Surveyor's Charge** | TNT + Redstone Dust + Arrow |
| **Remote Detonator** | Lever + Redstone Dust + Iron Ingot |

---

## 📋 Changelog & Releases

For detailed release notes across versions, check out **[CHANGELOG.md](CHANGELOG.md)**.
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
2. Drop `track and trains-1.0.3.jar` into your `.minecraft/mods` folder.
3. *(Optional)* Compatible with **Jade**, **JEI**, **Industrial Refinery**, and **Ultimate Car Mod**.
