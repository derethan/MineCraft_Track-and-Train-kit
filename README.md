# ðŸš‚ Track and Train Pack (Track & Train Kit)

**Heavy Rail Logistics, High-Speed Locomotives, Automated Trackside Facilities, Subterranean Excavation, and Geological Prospecting for Minecraft 26.3 (NeoForge)**

---

## ðŸ“– Overview

**Track and Train Pack** transforms Minecraft's minecart and rail system into a complete heavy-haul railway logistics and automated mining network. 

Whether you need to haul massive freight trains across long distances, automate livestock transit, transport bulk liquids with automated locomotive tender siphoning, haul loose bulk aggregates with authentic 3D visual heaps, drill 3Ã—3 subterranean tunnels automatically, or prospect underground ore and oil deposits with seismic chargesâ€”**Track and Train Pack** provides the locomotives, specialty tracks, and smart trackside machinery to get the job done.

> ðŸ“– **Need a complete walkthrough, mechanics guide, or crafting recipes?** Check out the comprehensive **[Track and Train Pack Wiki & Walkthrough](WIKI.md)**!

---

## ðŸŒŸ Key Highlights

* ðŸš‚ **3 Distinct Locomotives**: Coal-fired Steam Locomotive, CN Diesel Locomotive, and High-Speed Electric Bullet Train with realistic multi-gear transmissions, headlights, and horns.
* ðŸ”— **Universal Consist Coupling & MU Consists**: Link dozens of carts together using the Railroad Crowbarâ€”even without a locomotive! Distribute lead, mid-train, and pusher locomotives to multiply pulling capacity, or shunt strings of cars by hand.
* ðŸ“¦ **Dynamic Freight & Tankers**: Adaptive Cargo Carts that visually transform (Logs, Shipping Container, Ores, Ingots), 16-bucket Large Liquid Tankers with automatic diesel fuel tender siphoning, and Loose Aggregate Hopper Cars with dynamic 3D bulk material mounds.
* ðŸ’¥ **Geological Prospecting & Boreholes**: Detonate Surveyor's Charges wirelessly via the Remote Detonator to drill a clean 1Ã—1 borehole to bedrock and receive a full-depth cylindrical column survey tallying ores, minerals, and subterranean oil deposits.
* ðŸšœ **Automated Tunnel Bore**: Drills a 3Ã—3 tunnel through stone and deepslate, clears loose gravel/sand safely, bridges chasms with ballast, and places its own rail tracks automatically.
* ðŸ›¤ï¸ **Specialty Switch & Power Rails**: 4-way Junction Rails, 3-way Wye Junctions, Turnout Switches, Control Rails, Horn Rails, and Electric Third Rails.
* ðŸ“¡ **Smart Track Automation**: Automated Entity Loaders, Entity Unloaders, Fluid Stations, and Train-Name Signal Switches.
* âš“ **Zero-Decoupling Chunk Anchors**: Both mobile (Anchor Caboose, placeable anywhere in the train) and stationary (Industrial Chunk Anchor) chunk loaders that keep automation and trains ticking across chunk borders.
* ðŸ§­ **Live HUD Dashboard**: On-screen speedometer showing blocks/sec, km/h, heading, throttle gear, and fuel reserves.

---

## ðŸš‚ Locomotives & Motive Power

Every locomotive features dynamic directional headlights (level 15 lighting), interactive horns, a multi-gear transmission, and a dedicated interactive cockpit GUI.

### 1. Basic Steam Locomotive
The quintessential iron horse for industrial mining and freight operations.
* **Fuel**: Consumes coal or charcoal (default: 80 seconds per coal).
* **Speed**: 5 throttle gears with a calibrated **1.0 m/s (0.05 b/t) Crawl Speed** in Gear 1 for precise switching and coupling, scaling up to 0.35+ blocks/tick (7 m/s).
* **Pulling Capacity**: Pulls up to **5 carts** with zero drag penalty.
* **Whistle & Cowcatcher**: Sounds a loud steam whistle that scatters mobs off the tracks and pushes obstacles out of the way.

### 2. CN Diesel Locomotive (#3087)
A heavy-duty mainline diesel road-switcher modeled after Canadian National (CN) locomotives.
* **Speed**: **2Ã— Top Speed** of the steam locomotive (0.70 blocks/tick).
* **Fuel**: Liquid diesel fuel. Equipped with an internal **16,000 mB (16-bucket)** fuel tank with a golden-yellow fuel gauge and bucket draining slot.
* **Fuel Compatibility**: Native support for **Industrial Refinery Diesel**, **Ultimate Car Mod Bio-Diesel**, **Logistics Bio-Fuel**, and all fluids tagged `#c:diesel`, `#c:biofuel`, or `#c:biodiesel`.
* **Automated Fuel Tender Siphoning**: Coupling a Liquid Minecart or Large Liquid Minecart behind or ahead of the locomotive turns the tanker into an automated fuel tender wagon, siphoning up to 100 mB/tick to keep the engine running across continental distances.
* **Pulling Capacity**: Pulls up to **10 carts** with zero drag penalty.
* **Multiple-Unit (MU) Chaining**: Chain multiple diesel locomotives together (front, middle, or rear) to pull massive 20â€“30+ car trains at maximum speed!

### 3. Electric Bullet Train
Sleek, aerodynamic, ultra-high-speed electric rail transit.
* **Speed**: **5Ã— Speed** of the steam locomotive (1.75 blocks/tick).
* **Power**: Runs on Forge Energy (FE/RF) with a **100,000 FE** onboard battery.
* **Electric Rail Pickup**: Draws power directly through trackside **Electric Rails** without stopping, or recharges using battery items in its GUI.
* **Pulling Capacity**: Pulls up to **10 carts** at lightning speeds.

---

## ðŸ”— Train Mechanics, Physics & Railroad Crowbar

* **Railroad Crowbar**: Right-click to link any two carts or locomotives together into an unbroken train. Shift + Right-click to uncouple.
* **Locomotive-Free Consist Coupling**: Rolling stock can be coupled into trains with the Crowbar even without a locomotive attached. Moving or gravity-rolling any linked car moves the entire consist at exact coupler spacing.
* **Solid Bumper Collisions & Anti-Phasing**: Overrides vanilla's collision cancellation bug (`vec3.dot(vec31) < 0.8`), ensuring uncoupled cars make solid bumper contact and exchange along-track momentum rather than passing through each other.
* **Continuous Slope Elevation Tracking**: Overcomes vanilla slope clipping with continuous height interpolation, eliminating cart bouncing, shaking, or ceiling jams on mountain inclines.
* **Distributed Locomotive Traction**: Each locomotive adds its pull allowance to the train. When pulling more carts than the combined allowance, trains realistically experience drag slowdown.
* **Heavy-Rail Cowcatcher Deflection**: Operating trains deflect mobs and players safely aside without stalling, freezing, or bouncing the train.
* **Non-Inverting Reverse**: Reversing train direction shifts motion into reverse without disorienting 180Â° visual cart flips.

---

## ðŸšƒ Rolling Stock & Freight

### Adaptive Cargo Minecart
A heavy-duty freight wagon with 27 storage slots and an intelligent **Filter Slot**. The cart's 3D model automatically adapts to its cargo:
* **Log Mode**: Four vertical timber stakes loaded with stacked wooden logs.
* **Container Mode**: Heavy enclosed shipping container (stakes retract).
* **Ore Mode**: Raw unrefined ores mounded high.
* **Ingot Mode**: Stacked precious metal ingots.

### Loose Aggregate Hopper Car (Hopper Minecart)
A heavy-duty open-top hopper car designed for hauling bulk loose aggregates (modeled after North American TIMX and Herzog ballast hoppers):
* **Dynamic 3D Heap Mounds**: Dynamically renders 3 mounded heaps inside the container when hauling loose bulk materials (Coal heaps, Gravel/ballast heaps, yellow Sand heaps, Red Sand heaps, Dirt/clay heaps, and raw metal Ore heaps).
* **Empty Chute View**: Renders as a hollow container displaying internal sloped discharge chutes when cleared.
* **Filter Slot**: Dedicated hopper filter slot locks acceptance to the specified aggregate type.
* **Track-Aligned Placement**: Automatically aligns initial yaw to railway tracks upon placement.

### Livestock Cart (Railroad Stock Car)
An authentic North American railroad stock car modeled to scale with rolling stock:
* **Dual Stall Capacity**: Comfortably accommodates up to **2 entities** simultaneously across distinct front and rear stalls.
* **Open Slatted Wooden Construction**: Built with horizontal wooden slats, keeping livestock well-ventilated and clearly visible as the train travels.
* **Leash Hitching**: Right-clicking the cart while leading an animal on a Lead instantly unleashes and boards the animal inside.
* **Smart Facility Compatibility**: Integrates directly with the **Trackside Entity Loader** and **Trackside Entity Unloader**.
* **Activator Rail Ejection**: Rolling over a powered Activator Rail gently dismounts passengers at destination pens.

### Liquid Minecarts (Small & Large)
Specialized tanker wagons designed to safely transport fluids across rail lines.
* **Small Liquid Minecart**: 8,000 mB (8 Buckets) capacity.
* **Large Liquid Minecart**: 16,000 mB (16 Buckets) heavy rail tanker.
* **Fuel Tender Capability**: Automatically acts as a fuel tender when coupled to the CN Diesel Locomotive.
* Seamlessly loads and unloads via the **Trackside Fluid Station**.

### Arrow Turret Cart
An armored rolling-stock defense wagon styled after the Caboose and heavy freight cars to protect trains from hostiles.
* **Dual Weapon Slots**: Mount up to **2 Bows** into dedicated gun ports on the rotating turret.
* **9-Slot Ammo Magazine**: Carries up to 9 stacks of arrows (standard, tipped, or spectral arrows).
* **Automated Hostile Defense**: Scans within a **16-block radius** for hostile mobs (`Enemy`) and fires alternate staggered volleys from its dual bow mounts.
* **Target Filtering**: Automatically ignores players, animals, villagers, and passenger mobs on trains.
* **Dynamic Turret Rotation**: The armored cupola smoothly tracks and swivels towards active targets.

### Anchor Caboose
The ultimate train-protection device attached to your consist.
* **Universal Placement**: Can now be placed at the front, middle, or rear of the train consist!
* **Mobile Chunk Loading**: Forces a 3Ã—3 chunk perimeter around itself **plus** keeps all chunks occupied by the entire train loaded in memory as it travels.
* **Prevents Runaway Decoupling**: Eliminates trains freezing or separating across chunk boundaries.
* **Atmospheric Visuals**: Features an authentic stove pipe chimney puffing smoke particles.

---

## ðŸ’¥ Geological Prospecting & Survey System

### Surveyor's Charge
A specialized seismic explosive for subterranean prospecting and rapid vertical shaft excavation:
* **Full-Depth Cylindrical Column Survey**: Scans a cylindrical column (default 10-block radius) from the charge placement level all the way down to bedrock.
* **Crude Oil & Fluid Detection**: Aggregates and reports oil fluids and source blocks (`#c:oil`, `#c:crude_oil`, petroleum, diesel, fuel).
* **Borehole Blasting**: Detonation cleanly excavates a 1Ã—1 vertical shaft to bedrock (with configurable item drops or clean destruction).
* **Survey Chat Telemetry**: Sends a comprehensive ore and resource tally directly into your chat.

### Remote Detonator
A handheld wireless detonator keyed to trigger Surveyor's Charges:
* **Wireless Activation**: Right-click to detonate all Surveyor's Charges within a 12-block radius.
* **Instant Relay**: Relays survey telemetry directly to the triggering player.

---

## ðŸšœ Heavy Machinery: The Tunnel Bore

An automated subterranean excavator for carving out subway lines and railway corridors.
* **3Ã—3 Excavation**: Automatically drills a 3-block-wide by 3-block-tall tunnel directly forward.
* **Top-Down Drilling**: Breaks blocks from top to bottom so sand and gravel fall safely into the drill path rather than suffocating the machine.
* **Automated Track Laying**: Automatically places tracks ahead of itself on solid ground from its 9 dedicated track slots.
* **Ballast Inventory & Auto Gap-Bridging**: Features a dedicated 9-slot Ballast Inventory accepting **gravel, sand, cobblestone, and stone**. When encountering chasms, potholes, or lava pools, it automatically places ballast to fill the void, beds the track foundation, and continues drilling without stopping!
* **Safety Sensors**: Automatically halts if it encounters bedrock, chasms exceeding ballast reserves, or runs out of track or fuel.
* **Bi-Directional Operation**: Forward drilling mode and slow, safe reversing mode that reverses trailing cars seamlessly.

---

## ðŸ“¡ Track Automation & Facilities

### Trackside Entity Loader
Placed directly under any piece of track to automatically board entities into passing empty minecarts.
* **Whitelist & Blacklist**: Filter entity IDs (e.g. `rabbit, cow`), custom nametags, or player usernames.
* **Age Filter**: Toggle between **Both**, **Adults Only**, or **Babies Only**.
* **Detection Range**: Adjustable radius (1 to 16 blocks).

### Trackside Entity Unloader
Placed directly under track to automatically dismount passengers from passing minecarts.
* **Safe Landing**: Automatically teleports dismounted passengers to adjacent solid ground away from the tracks.
* **Filtering**: Same Whitelist, Blacklist, Age Filter, and Range controls.

### Trackside Fluid Station
Placed underneath tracks to pump fluids into or drain fluids from Liquid Minecarts.
* **16,000 mB Buffer Tank**: Connects with fluid pipes, buckets, and canisters.
* **Redstone Automation**: Configurable to emit a pulse when carts are empty, full, or finished transferring.

### Track Signal Switch
A mechanical trackside switch stand with an animated signal lantern and arm.
* **Automated Routing**: Define routing rules based on train names (e.g. send "Freight-North" onto side tracks automatically).
* **Manual & Redstone Modes**: Can also be toggled manually or via redstone wire.

### Industrial Chunk Anchor
A stationary, heavy-duty chunk loader block for mines, quarries, mob farms, and factories.
* **Configurable Area**: Cycle between **1Ã—1 Chunk (Single)**, **3Ã—3 Chunks (9 Total)**, or **5Ã—5 Chunks (25 Total)**.
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

## ðŸ›¤ï¸ Specialty Switch & Power Rails

* **Junction Rail**: True 4-way perpendicular flat cross rail (+). Minecarts cross over perpendicularly without turning or derailing.
* **Turnout Rail**: Mainline/divergent switch track toggled via redstone or hand/crowbar interaction, with dynamic animated switch blades.
* **Wye Rail**: Triangular 3-way junction rail with visual active-branch indication and smart directional routing.
* **Locomotive Control Rail**: Redstone-powered dispatch rail that automatically starts, halts, or reverses approaching locomotives.
* **Locomotive Horn Rail**: Pressure-sensitive rail that triggers locomotive whistles and horns (ideal for level crossings and tunnel entrances).
* **Electric Rail**: Conductive third rail that transmits Forge Energy (FE) along your network to charge Electric Locomotives on the fly.

---

## âŒ¨ï¸ Controls & Keybindings

When seated inside a locomotive or coupled cart:

| Action | Default Key | Description |
| :--- | :---: | :--- |
| **Throttle Up / Start** | `W` | Engages engine and starts locomotive forward |
| **Brake / Stop** | `S` | Disengages throttle and applies brakes |
| **Gear Down** | `A` | Decreases target speed level (Gears 1â€“5) |
| **Gear Up** | `D` | Increases target speed level (Gears 1â€“5) |
| **Reverse** | `R` | Reverses locomotive travel direction |
| **Horn / Whistle** | `H` | Sounds horn, scares nearby mobs off track |
| **Headlight** | `L` | Toggles dynamic directional headlight |
| **Vehicle GUI** | `C` | Opens the vehicle dashboard and fuel inventory |

---

## ðŸ”¨ Crafting Overview

| Item | Primary Ingredients |
| :--- | :--- |
| **Railroad Crowbar** | Iron Ingots + Iron Nugget |
| **Basic Locomotive** | Furnace + Minecart + Iron Ingots + Redstone |
| **CN Diesel Locomotive** | Iron Blocks + Glass + Redstone Block + Bucket + Minecart |
| **Electric Bullet Train** | Iron Block + Copper Block + Redstone Block + Tinted Glass + Minecart |
| **Tunnel Bore** | Iron Pickaxes + Iron Block + Furnace Minecart + Chest Minecart + Iron Ingots |
| **Cargo Minecart** | Chest + Minecart + Iron Ingot |
| **Loose Aggregate Hopper Car** | Iron Ingots + Hopper + Chest Minecart |
| **Livestock Cart** | Wooden Planks + Wooden Fences + Lead + Minecart |
| **Liquid Minecart (8-Bucket)** | Copper Ingot + Bucket + Iron Ingots + Minecart |
| **Large Liquid Minecart (16-Bucket)** | Liquid Minecart + Bucket + Iron Ingots |
| **Arrow Turret Cart** | Bows + Dispenser + Iron Ingots + Minecart |
| **Anchor Caboose** | Ender Pearl + Lantern + Iron Ingots + Minecart |
| **Industrial Chunk Anchor** | Ender Pearl + Obsidian + Gold Ingots + Redstone Block + Iron Ingots |
| **Railway Buffer Stop** | Wooden Log + Iron Bars + Redstone Dust + Iron Ingots (Yields 2) |
| **Entity Loader** | Lead + Hopper + Copper Ingots + Iron Ingots + Redstone Dust |
| **Entity Unloader** | Activator Rail + Hopper + Copper Ingots + Iron Ingots + Redstone Dust |
| **Trackside Fluid Station** | Piston + Hopper + Copper Ingots + Iron Ingots + Redstone Dust |
| **Track Signal Switch** | Redstone Torch + Lever + Iron Ingots (Yields 2) |
| **Surveyor's Charge** | TNT + Redstone Dust + Arrow |
| **Remote Detonator** | Lever + Redstone Dust + Iron Ingot |

---

## ðŸ“‹ Changelog & Releases

For detailed release notes across versions, check out **[CHANGELOG.md](CHANGELOG.md)**.
* **v1.0.1**: Added Loose Aggregate Hopper Car (with dynamic 3D bulk material mounds), Surveyor's Charge & Remote Detonator (with borehole blasting & full-depth column survey down to bedrock), CN Diesel Locomotive fuel tender siphoning, universal consist coupling & manual yard shunting, anti-phasing bumper physics, continuous slope elevation tracking, and NeoForge 1.21.4 server recipe codec fixes.
* **v1.0.0**: Initial release for Minecraft 26.3.

---

## âš™ï¸ Configuration

Track and Train Pack features extensive configuration via `config/miningtools-common.toml`:
* Locomotive coal burn durations and speed curves.
* Diesel tank capacities, burn rates, and top speeds.
* Electric battery capacity, FE/tick consumption, and electric rail transfer rates.
* Pull capacities and drag penalties for each locomotive type.
* Surveyor's charge column radius, ignored blocks, ore reporting lists, and borehole drops.

---

## ðŸ“¦ Installation & Dependencies

1. Ensure **Minecraft 26.3** and **NeoForge** are installed.
2. Drop `track and trains-1.0.1.jar` into your `.minecraft/mods` folder.
3. *(Optional)* Compatible with **Jade**, **JEI**, **Industrial Refinery**, and **Ultimate Car Mod**.