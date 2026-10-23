# 🚂 Mining Tools (Track & Train Kit)

**Heavy Rail Logistics, High-Speed Locomotives, Automated Trackside Facilities, and Subterranean Excavation for Minecraft 26.3 (NeoForge)**

---

## 📖 Overview

**Mining Tools** transforms Minecraft's minecart and rail system into a complete heavy-haul railway logistics and automated mining network. 

Whether you need to haul massive freight trains across long distances, automate livestock and mob transit, transport bulk liquids, drill 3×3 subterranean tunnels automatically, or keep your automated quarries loaded 24/7—**Mining Tools** provides the locomotives, specialty tracks, and smart trackside machinery to get the job done.

---

## 🌟 Key Highlights

* 🚂 **3 Distinct Locomotives**: Coal-fired Steam Locomotive, CN Diesel Locomotive, and High-Speed Electric Bullet Train.
* 🔗 **Realistic Train Chaining & MU Consists**: Link dozens of carts together using the Railroad Crowbar. Distribute lead, mid-train, and pusher locomotives to multiply train pulling capacity!
* 🚜 **Automated Tunnel Bore**: Drills a 3×3 tunnel through stone and deepslate, clears loose gravel/sand safely, and places its own rail tracks automatically.
* 📦 **Dynamic Freight & Tankers**: Adaptive Cargo Carts that visually change (Logs, Shipping Container, Ores, Ingots) and Large 16-Bucket Liquid Tankers.
* 🛤️ **Specialty Switch & Power Rails**: 4-way Junction Rails, 3-way Wye Junctions, Turnout Switches, Control Rails, Horn Rails, and Electric Third Rails.
* 📡 **Smart Track Automation**: Automated Entity Loaders, Entity Unloaders, Fluid Stations, and Train-Name Signal Switches.
* ⚓ **Zero-Decoupling Chunk Anchors**: Both mobile (Anchor Caboose) and stationary (Industrial Chunk Anchor) chunk loaders that keep automation and trains ticking across chunk borders.
* 🧭 **Live HUD Dashboard**: On-screen speedometer showing blocks/sec, km/h, heading, throttle gear, and fuel reserves.

---

## 🚂 Locomotives & Motive Power

Every locomotive features dynamic directional headlights (level 15 lighting), interactive horns, a multi-gear transmission, and a dedicated interactive cockpit GUI.

### 1. Basic Steam Locomotive
The quintessential iron horse for industrial mining and freight operations.
* **Fuel**: Consumes coal or charcoal (default: 80 seconds per coal).
* **Speed**: 5 throttle gears (configurable up to 0.35+ blocks/tick).
* **Pulling Capacity**: Pulls up to **5 carts** with zero drag penalty.
* **Whistle & Cowcatcher**: Sounds a loud steam whistle that scatters mobs off the tracks and pushes obstacles out of the way.

### 2. CN Diesel Locomotive (#3087)
A heavy-duty mainline diesel road-switcher modeled after Canadian National (CN) locomotives.
* **Speed**: **2× Top Speed** of the steam locomotive (0.70 blocks/tick).
* **Fuel**: Liquid diesel fuel. Equipped with an internal **16,000 mB (16-bucket)** fuel tank with a golden-yellow fuel gauge and bucket draining slot.
* **Fuel Compatibility**: Native support for **Industrial Refinery Diesel**, **Ultimate Car Mod Bio-Diesel**, **Logistics Bio-Fuel**, and all fluids tagged `#c:diesel`, `#c:biofuel`, or `#c:biodiesel`.
* **Pulling Capacity**: Pulls up to **10 carts** with zero drag penalty.
* **Multiple-Unit (MU) Chaining**: Chain multiple diesel locomotives together (front, middle, or rear) to pull massive 20–30+ car trains at maximum speed!

### 3. Electric Bullet Train
Sleek, aerodynamic, ultra-high-speed electric rail transit.
* **Speed**: **5× Speed** of the steam locomotive (1.75 blocks/tick).
* **Power**: Runs on Forge Energy (FE/RF) with a **100,000 FE** onboard battery.
* **Electric Rail Pickup**: Draws power directly through trackside **Electric Rails** without stopping, or recharges using battery items in its GUI.
* **Pulling Capacity**: Pulls up to **10 carts** at lightning speeds.

---

## 🔗 Train Mechanics & Railroad Crowbar

* **Railroad Crowbar**: Right-click to link any two carts or locomotives together into an unbroken train. Shift + Right-click to uncouple.
* **Distributed Locomotive Traction**: Each locomotive adds its pull allowance to the train. When pulling more carts than the combined allowance, trains realistically experience drag slowdown—incentivizing realistic multi-engine freight consists!
* **Non-Inverting Reverse**: Reversing train direction shifts motion into reverse without disorienting 180° visual cart flips.

---

## 🚃 Rolling Stock & Freight

### Cargo Minecart
A heavy-duty freight wagon with 27 storage slots and an intelligent **Filter Slot**. The cart's 3D model automatically adapts to its cargo:
* **Log Mode**: Four vertical timber stakes loaded with stacked wooden logs.
* **Container Mode**: Heavy enclosed shipping container (stakes retract).
* **Ore Mode**: Raw unrefined ores mounded high.
* **Ingot Mode**: Stacked precious metal ingots.

### Liquid Minecarts (Small & Large)
Specialized tanker wagons designed to safely transport fluids across rail lines.
* **Small Liquid Minecart**: 8,000 mB (8 Buckets) capacity.
* **Large Liquid Minecart**: 16,000 mB (16 Buckets) heavy rail tanker.
* Seamlessly loads and unloads via the **Trackside Fluid Station**.

### Anchor Caboose
The ultimate train-protection device attached to the rear of your consist.
* **Mobile Chunk Loading**: Forces a 3×3 chunk perimeter around itself **plus** keeps all chunks occupied by the entire train loaded in memory as it travels.
* **Prevents Runaway Decoupling**: Eliminates trains freezing or separating across chunk boundaries.
* **Atmospheric Visuals**: Features an authentic stove pipe chimney puffing smoke particles.

---

## 🚜 Heavy Machinery: The Tunnel Bore

An automated subterranean excavator for carving out subway lines and railway corridors.
* **3×3 Excavation**: Automatically drills a 3-block-wide by 3-block-tall tunnel directly forward.
* **Top-Down Drilling**: Breaks blocks from top to bottom so sand and gravel fall safely into the drill path rather than suffocating the machine.
* **Automated Track Laying**: Automatically places tracks ahead of itself on solid ground.
* **Internal Storage**: Stores mined materials and carries extra rails and coal fuel.
* **Safety Sensors**: Automatically halts if it encounters lava, chasms without floor support, unbreakable blocks, or runs out of track.

---

## 📡 Track Automation & Facilities

### Trackside Entity Loader
Placed directly under any piece of track to automatically board entities into passing empty minecarts.
* **Whitelist & Blacklist**: Type entity IDs (e.g. `rabbit, cow`), custom nametags, or player usernames.
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

* **Junction Rail**: True 4-way perpendicular flat cross rail (+). Minecarts cross over perpendicularly without turning or derailing.
* **Turnout Rail**: Mainline/divergent switch track toggled via redstone or crowbar interaction.
* **Wye Rail**: Triangular 3-way junction rail for smooth directional splitting and turnaround loops.
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
| **Vehicle GUI** | `G` | Opens the vehicle dashboard and fuel inventory |

---

## 🔨 Crafting Overview

| Item | Primary Ingredients |
| :--- | :--- |
| **Railroad Crowbar** | Iron Ingots + Red Dye |
| **Basic Locomotive** | Furnace + Minecart + Iron Ingots + Redstone |
| **CN Diesel Locomotive** | Heavy Locomotive Frame + Copper + Glass + Redstone + Iron |
| **Electric Bullet Train** | Iron + Gold + Redstone + Copper + Minecart |
| **Tunnel Bore** | Diamond / Iron Pickaxe + Pistons + Dispenser + Furnace + Iron |
| **Cargo Minecart** | Chest + Minecart + Iron Bars |
| **Liquid Minecart** | Iron Ingots + Minecart + Glass |
| **Anchor Caboose** | Ender Pearl + Obsidian + Minecart + Redstone |
| **Industrial Chunk Anchor** | Ender Pearl + Obsidian + Gold + Redstone Block + Iron |
| **Railway Buffer Stop** | Iron Ingots + Log/Planks + Redstone + Iron Bars (Yields 2) |
| **Entity Loader** | Lead + Hopper + Copper + Iron + Redstone |
| **Entity Unloader** | Activator Rail + Hopper + Copper + Iron + Redstone |
| **Trackside Fluid Station** | Piston + Hopper + Copper + Iron + Redstone |
| **Track Signal Switch** | Lever + Redstone Torch + Smooth Stone + Iron |

---

## ⚙️ Configuration

Mining Tools features extensive configuration via `miningtools-common.toml` or the in-game NeoForge Config Screen:
* Locomotive coal burn durations and speed curves.
* Diesel tank capacities, burn rates, and top speeds.
* Electric battery capacity, FE/tick consumption, and electric rail transfer rates.
* Pull capacities and drag penalties for each locomotive type.

---

## 📦 Installation & Dependencies

1. Ensure **Minecraft 26.3** and **NeoForge** are installed.
2. Drop `miningtools-1.0.1.jar` into your `.minecraft/mods` folder.
3. *(Optional)* Compatible with **Jade**, **JEI**, **Industrial Refinery**, and **Ultimate Car Mod**.
