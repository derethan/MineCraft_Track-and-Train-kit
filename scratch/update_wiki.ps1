$wikiContent = @'
# 🚂 Mining Tools (Track & Train Kit) — Official Wiki & Walkthrough

Welcome to the comprehensive player manual, engineering walkthrough, and crafting compendium for **Mining Tools** on Minecraft 26.3 (NeoForge).

---

## 📑 Table of Contents

1. [Quick-Start Guide: Your First Train](#1-quick-start-guide-your-first-train)
2. [Locomotives & Motive Power](#2-locomotives--motive-power)
   - [Basic Steam Locomotive](#basic-steam-locomotive)
   - [CN Diesel Locomotive (#3087)](#cn-diesel-locomotive-3087)
   - [Diesel Fuel Tender Siphoning](#diesel-fuel-tender-siphoning)
   - [Electric Bullet Train](#electric-bullet-train)
   - [Locomotive Controls & HUD Dashboard](#locomotive-controls--hud-dashboard)
3. [Train Chaining, Physics & Railroad Crowbar](#3-train-chaining-physics--railroad-crowbar)
   - [Coupling & Uncoupling Mechanics](#coupling--uncoupling-mechanics)
   - [Universal Consist Kinematics & Manual Yard Shunting](#universal-consist-kinematics--manual-yard-shunting)
   - [Solid Bumper Collisions & Anti-Phasing Physics](#solid-bumper-collisions--anti-phasing-physics)
   - [Multi-Block Slope Tracking & Continuous Descent](#multi-block-slope-tracking--continuous-descent)
   - [Universal Caboose Placement](#universal-caboose-placement)
   - [Traction, Drag & Multiple-Unit (MU) Consists](#traction-drag--multiple-unit-mu-consists)
4. [Rolling Stock Catalog](#4-rolling-stock-catalog)
   - [Adaptive Cargo Minecart](#adaptive-cargo-minecart)
   - [Loose Aggregate Hopper Car (Hopper Minecart)](#loose-aggregate-hopper-car-hopper-minecart)
   - [Livestock Cart (Railroad Stock Car)](#livestock-cart-railroad-stock-car)
   - [Liquid Minecarts (Small & Large)](#liquid-minecarts-small--large)
   - [Arrow Turret Cart](#arrow-turret-cart)
   - [Anchor Caboose](#anchor-caboose)
5. [Specialty Switch & Power Rails](#5-specialty-switch--power-rails)
   - [Junction Rail (4-Way Cross)](#junction-rail-4-way-cross)
   - [Turnout Rail (T-Switch)](#turnout-rail-t-switch)
   - [Wye Rail (Triangular 3-Way Switch)](#wye-rail-triangular-3-way-switch)
   - [Locomotive Control Rail & Horn Rail](#locomotive-control-rail--horn-rail)
   - [Electric Rail (Third-Rail Power)](#electric-rail-third-rail-power)
6. [Trackside Automation Facilities](#6-trackside-automation-facilities)
   - [Trackside Entity Loader & Unloader](#trackside-entity-loader--unloader)
   - [Trackside Fluid Station](#trackside-fluid-station)
   - [Mechanical Track Signal Switch Stand](#mechanical-track-signal-switch-stand)
   - [Railway Buffer Stop (Stop Block)](#railway-buffer-stop-stop-block)
   - [Industrial Chunk Anchor](#industrial-chunk-anchor)
7. [Heavy Machinery: The Automated Tunnel Bore](#7-heavy-machinery-the-automated-tunnel-bore)
   - [Excavation, Ballast Bridging & Track Laying](#excavation-ballast-bridging--track-laying)
8. [Geological Prospecting & Survey System](#8-geological-prospecting--survey-system)
   - [Surveyor's Charge](#surveyors-charge)
   - [Remote Detonator](#remote-detonator)
   - [Configuration Options](#configuration-options)
9. [Complete Crafting Recipe Compendium](#9-complete-crafting-recipe-compendium)
   - [Locomotives & Motive Power](#locomotives--motive-power-1)
   - [Rolling Stock & Defense Carts](#rolling-stock--defense-carts)
   - [Specialty Rails & Tools](#specialty-rails--tools)
   - [Trackside Facilities & Heavy Machinery](#trackside-facilities--heavy-machinery)
   - [Geological Prospecting & Surveying Tools](#geological-prospecting--surveying-tools)
10. [Version History & Changelog](#10-version-history--changelog)

---

## 1. Quick-Start Guide: Your First Train

```mermaid
flowchart LR
    A["Forge Railroad Crowbar"] --> B["Craft Locomotive & Fuel"]
    B --> C["Place on Rails & Board"]
    C --> D["Couple Freight / Defense Cars"]
    D --> E["Accelerate with W Key"]
```

1. **Craft a Railroad Crowbar**: The essential tool for all railway engineers (`1 Iron Ingot + 1 Iron Nugget`).
2. **Build a Locomotive**: Start with the **Basic Steam Locomotive** (`Furnace + Minecart + Iron Ingots + Redstone`).
3. **Lay Tracks**: Standard rails, powered rails, or custom **Control Rails**.
4. **Assemble the Consist**: Place your locomotive in front, followed by your rolling stock (e.g., Cargo Carts, Hopper Cars, Livestock Carts, Turret Carts, or an Anchor Caboose).
5. **Couple the Cars**: Hold the **Railroad Crowbar** in hand, right-click the Locomotive, then right-click the cart directly behind it. Repeat down the length of the train. Cars can also be coupled together freely without a locomotive!
6. **Fuel Up & Board**: Right-click the locomotive to board, press `C` to open the fuel inventory, deposit Coal or Charcoal, and tap `W` to throttle up!

---

## 2. Locomotives & Motive Power

Every locomotive in Mining Tools features realistic acceleration physics, multi-gear throttle transmissions, dynamic directional lighting (casting real light ahead of the engine), audible warning horns, and a live cockpit HUD.

```
       ______
      /|_||_\`.__
     (   _    _ _\
     =`-(_)--(_)-'  Mining Tools Locomotive Fleet
```

---

### Basic Steam Locomotive
The reliable workhorse of standard industrial railroads.

* **Fuel Source**: Coal, Charcoal, or any solid furnace fuel (default: 80 seconds of burn time per piece of coal).
* **Speed Profile**: 5 forward gears. **Gear 1 is calibrated to a precise Crawl Speed of 1.0 m/s (0.05 blocks/tick)** for precision switching and coupling maneuvers, topping out at Gear 5 at `0.35` blocks/tick (`~7 m/s` or `25 km/h`).
* **Pulling Allowance**: Pulls up to **5 carts** with zero friction penalty. Pulling 6+ carts causes progressive drag slowdown.
* **Cowcatcher**: Deflects and pushes wandering animals and minor obstacles out of the rail path.
* **Steam Whistle**: Emits a loud piercing whistle that causes nearby mobs to flee the tracks.

---

### CN Diesel Locomotive (#3087)
A heavy-haul mainline road-switcher modeled after classic Canadian National diesel engines.

* **Top Speed**: **2× the speed of steam** (`0.70` blocks/tick, `~14 m/s` or `50 km/h`).
* **Fuel Source**: Liquid Diesel Fuel. Holds an internal **16,000 mB (16-bucket)** tank.
  - Native support for `#c:diesel`, `#c:biofuel`, and `#c:biodiesel` (compatible with Industrial Refinery, Ultimate Car Mod, etc.).
  - Equipped with a quick bucket-draining slot directly in its cockpit menu.
* **Pulling Allowance**: Heavy-duty prime mover pulls up to **10 carts** with zero drag penalty.
* **Multiple-Unit (MU) Synergy**: Designed to be coupled with additional diesel engines to haul 20–30+ freight car trains across continental distances.

---

### Diesel Fuel Tender Siphoning
The **CN Diesel Locomotive** features automated fuel tender wagon siphoning:
* **Coupled Tender Detection**: Hitching a **Liquid Minecart** (8,000 mB) or **Large Liquid Minecart** (16,000 mB) directly behind or ahead of the locomotive turns the tanker into a functional fuel tender wagon.
* **Continuous Siphoning**: The locomotive automatically scans its coupled consist and draws compatible diesel, biofuel, or oil from the tanker at up to **100 mB per game tick** whenever internal fuel reserves drop below full capacity.
* **Long-Distance Hauling**: Enables fully automated, long-haul continental rail routes without manual refueling stops.

---

### Electric Bullet Train
Ultra-high-speed, zero-emission passenger and express transit.

* **Top Speed**: **5× the speed of steam** (`1.75` blocks/tick, `~35 m/s` or `126 km/h`).
* **Power Source**: Forge Energy (FE). Equipped with an onboard **100,000 FE** internal battery.
* **Third-Rail Continuous Power**: When traveling along track segments equipped with **Electric Rails**, the pantograph/shoe draws electricity directly from the grid on the fly without stopping!
* **Pulling Allowance**: Pulls up to **10 carts** at maximum velocity.

---

### Locomotive Controls & HUD Dashboard

When seated in a locomotive or any coupled rolling-stock car:

| Action | Keybinding | In-Game Effect |
| :--- | :---: | :--- |
| **Throttle Up / Start** | <kbd>W</kbd> | Engages the engine and throttles up |
| **Brake / Stop** | <kbd>S</kbd> | Cuts throttle and smoothly applies train brakes |
| **Gear Down** | <kbd>A</kbd> | Lowers cruise gear (Gears 1 through 5) |
| **Gear Up** | <kbd>D</kbd> | Raises cruise gear (Gears 1 through 5) |
| **Reverse** | <kbd>R</kbd> | Toggles forward/reverse gear without flipping the model |
| **Horn / Whistle** | <kbd>H</kbd> | Sounds horn/whistle; scares mobs away from track |
| **Headlights** | <kbd>L</kbd> | Toggles high-beam headlights (dynamic block light level 15) |
| **Cockpit GUI** | <kbd>C</kbd> | Opens fuel reserves, battery gauges, and naming menu |

#### HUD Dashboard Indicators
While driving, the lower-left overlay displays:
- **Speed**: Live digital speedometer in `blocks/sec` and `km/h`.
- **Gear**: Current throttle position (`P` = Park/Brake, `1` through `5` = Cruise Gears, `R` = Reverse).
- **Heading**: Cardinal compass direction (`NORTH`, `SOUTH`, `EAST`, `WEST`).
- **Reserves**: Remaining burn ticks (Steam), fuel level in mB (Diesel), or battery charge in FE (Electric).

---

## 3. Train Chaining, Physics & Railroad Crowbar

```
 [Locomotive] <===Chain===> [Cargo Cart] <===Chain===> [Caboose]
```

The **Railroad Crowbar** transforms isolated vanilla minecarts into coordinated, multi-car train consists.

### Coupling & Uncoupling Mechanics
1. **Coupling**:
   - Hold the **Railroad Crowbar** in your main hand.
   - Right-click the first cart (a metal clicking sound confirms selection).
   - Right-click an adjacent cart within coupling distance (approx. 2–3 blocks).
   - A heavy steel chain appears connecting the two cars.
2. **Uncoupling**:
   - Sneak + Right-click (`Shift + Right-Click`) any coupled car with the Crowbar to break the link.
3. **Train Direction & Reversing**:
   - Trains maintain synchronized physics. Pulling in reverse pulls the rear cars backward smoothly without jarring 180° visual rotations.

---

### Universal Consist Kinematics & Manual Yard Shunting
- **Locomotive-Free Consist Coupling**: Rolling stock carts can be coupled together using the Railroad Crowbar even before a locomotive is attached.
- **Synchronized Unpowered Motion**: Pushing or gravity-rolling any individual car in an unpowered consist moves the entire linked train along the rails at exact coupler spacing, allowing players to shunt strings of cars by hand in rail yards.
- **Elastic Hand Pushing**: Players can walk directly into uncoupled rolling stock or consists to push them down the track to assemble trains.

---

### Solid Bumper Collisions & Anti-Phasing Physics
- **Anti-Phasing Collision Engine**: Vanilla Minecraft minecart collision logic suffers from a bug where carts with opposing or unaligned yaw vectors cancel push physics (`vec3.dot(vec31) < 0.8`), causing loose carts to phase straight through each other. Mining Tools overrides this with custom physical bumper separation (`TrainChainingUtil.pushMinecarts`).
- **Momentum Transfer**: When a rolling cart strikes a stationary cart, they make solid bumper contact and exchange along-track momentum smoothly, pushing the stationary cart forward realistically.
- **Coupled Car Bypassing**: Cars that are already coupled in the same consist bypass internal push collisions, ensuring long trains navigate tight switch curves without binding or jittering.

---

### Multi-Block Slope Tracking & Continuous Descent
- **True Continuous Elevation Tracking**: Overcomes vanilla slope clipping by continuously interpolating vertical height across ascending and descending grades. Eliminates cart shaking, bouncing forward/backward, and refusal to roll down slopes.
- **Multi-Block Grade Steps**: Scans 5 vertical blocks ($Y, Y-1, Y-2, Y-3, Y+1$) to smoothly navigate steep mountain switchbacks, stepped descents, and deep spiral mine shafts without getting stuck against ceiling blocks or stalling on inclines.
- **Consist Tracking**: All coupled cars track the exact incline contours of the rails behind the lead car at full operational speed.

---

### Universal Caboose Placement
- **Flexible Consist Positioning**: The **Anchor Caboose** is no longer restricted to the rear of the train! It can now be coupled at the front, middle, or rear of any train consist.
- **Continuous Mobile Chunk Loading**: Regardless of its position in the consist, the Caboose maintains a continuous 3×3 active chunk perimeter around itself and keeps all chunks occupied by the entire linked train loaded simultaneously.

---

### Traction, Drag & Multiple-Unit (MU) Consists

Each locomotive adds its **Pulling Allowance** to the entire train:
- **1 Steam Locomotive**: 5 cars maximum without penalty.
- **2 Steam Locomotives**: 5 + 5 = 10 cars maximum without penalty.
- **1 Diesel Locomotive + 1 Steam Locomotive**: 10 + 5 = 15 cars maximum without penalty.
- **2 Diesel Locomotives**: 10 + 10 = 20 cars maximum without penalty.

> [!TIP]
> Locomotives do **not** all need to be at the front! You can place one engine at the front (lead), one in the middle (mid-train helper), or one at the rear (pusher/banking engine). All connected locomotives automatically share throttle commands and combine their pulling power.

---

## 4. Rolling Stock Catalog

### Adaptive Cargo Minecart
A heavy-duty freight wagon with **27 storage slots** plus a dedicated **Filter Slot**. The cart's 3D exterior model dynamically changes based on what you filter:

| Filter Mode | Items Accepted | 3D Visual Appearance |
| :--- | :--- | :--- |
| **Log Mode** | Any wooden logs (`#minecraft:logs`) | Heavy upright log stakes loaded with rustic timber logs |
| **Container Mode** | General items, tools, ingots, food | Enclosed, corrugated industrial shipping container |
| **Ore Mode** | Raw iron, copper, gold, coal, lapis, redstone | Open hopper filled with towering mounds of raw mineral ores |
| **Ingot Mode** | Refined metal ingots | Neatly stacked precious metal bars |

> [!NOTE]
> To lock a filter, place the target item into the top-right filter slot inside the Cargo Cart GUI. The cart will now reject any non-matching items from hoppers and loaders.

---

### Loose Aggregate Hopper Car (Hopper Minecart)
A heavy-duty open-top hopper car designed for hauling bulk loose aggregates such as ballast gravel, sand, dirt, raw ores, and coal (modeled after North American TIMX and Herzog ballast hoppers).

#### 3D Visual Aggregate Heaps
- **Empty State**: Renders as an authentic hollow open hopper container showing interior sloped bottom discharge chutes.
- **Loaded State**: Dynamically renders 3 mounded aggregate heaps piled inside the container whenever loose materials are loaded.
- **Dynamic Textures**: Automatically selects and renders the exact texture matching the stored aggregate:
  - **Coal**: Rich black coal mounds.
  - **Gravel / Ballast**: Crushed ballast stone heaps (gravel, cobblestone, andesite, diorite, granite, tuff).
  - **Sand**: Clean yellow sand mounds.
  - **Red Sand**: Terracotta red sand heaps.
  - **Dirt**: Soil / clay mounds.
  - **Raw Ore**: Metal raw ore aggregates (`#c:raw_materials`, `#c:ores`).

#### Inventory & Filter Slot
- **27 Storage Slots**: High-capacity bulk cargo storage.
- **Dedicated Filter Slot**: Located with a custom hopper funnel icon. Placing a sample material into this slot locks the car to only accept that aggregate type from hoppers and trackside loaders.
- **Track-Aligned Placement**: Automatically aligns initial yaw to the track upon placement, preventing rotation anomalies on curves or switches.

---

### Livestock Cart (Railroad Stock Car)
An authentic North American railroad stock car modeled to scale with rolling stock:

* **Dual Stall Capacity**: Holds up to **2 entities** simultaneously across independent front and rear stalls.
* **Universal Compatibility**: Carries cows, sheep, pigs, chickens, horses, llamas, wolves, villagers, or players.
* **Slatted Wooden Body**: Built with horizontal open wooden slats, allowing livestock inside to remain clearly visible as the train travels.
* **One-Click Lead Hitching**: Right-click the Livestock Cart while leading any animal with a **Lead** to immediately board the animal inside and retrieve your lead.
* **Automated Ejection**: Rolling over a powered **Activator Rail** or passing through a **Trackside Entity Unloader** safely dismounts both stalls into adjacent destination pens.

---

### Liquid Minecarts (Small & Large)
Rugged railroad tank cars built for pressurized fluid transport across your logistics network.

* **Small Liquid Minecart**: Single horizontal pressure vessel holding **8,000 mB (8 Buckets)**.
* **Large Liquid Minecart**: Heavy-haul DOT-111 style dual-bogie rail tanker holding **16,000 mB (16 Buckets)**.
* **Inspection Glass**: Transparent liquid gauge tube shows exact fluid type and fill level from the outside.
* **Direct Fluid Transfer**: Seamlessly interacts with the **Trackside Fluid Station** or accepts manual filling via buckets.
* **Fuel Tender Capability**: Couples directly to the CN Diesel Locomotive to act as an automated fuel tender wagon.

---

### Arrow Turret Cart
An armored rolling-stock defense wagon styled to match the Caboose and freight wagons, designed to protect trains from mobs during long overnight journeys.

```
       [ Bow 1 ]      [ Bow 2 ]
             \   ____   /
              |=|____|=|  <-- Armored Rotating Cupola
             /|________|\
            O---O----O---O <-- Heavy Chassis
```

* **Dual Bow Mounts**: GUI slots 0 & 1 accept any standard Bow (including enchanted bows).
* **9-Slot Ammo Magazine**: GUI slots 2 through 10 store up to 9 stacks of arrows (standard, tipped, or spectral arrows).
* **Autonomous Target Detection**:
  - Scans within a **16-block spherical radius** for hostile mobs (`Enemy`).
  - Line-of-sight verification prevents wasting arrows on mobs behind solid walls.
  - Alternating staggered fire: Bow 1 and Bow 2 fire alternately every 12 ticks (`0.6s`), delivering a continuous stream of fire.
* **Target Safety Filtering**:
  - **Never** fires at players, villagers, passive farm animals, or tamed pets (`dogs, cats, parrots`).
  - **Never** shoots passengers or mobs riding in train carts.
* **Dynamic Turret Rotation**: The armored cupola on top smoothly swivels and aims directly at active targets on the client.
* **Durability & Ammo Rules**: Consumes 1 arrow and inflicts 1 point of bow durability damage per shot. When a bow breaks, it automatically clears the slot with an audible snap. Disallows arrow pickup to prevent infinite duplication.

---

### Anchor Caboose
The ultimate train-preservation device hitched to your consist.

* **Mobile Chunk Loading**: Keeps a **3×3 chunk zone** centered on itself permanently active in memory as it travels.
* **Consist Anchor**: Keeps every chunk occupied by the entire linked train loaded simultaneously, eliminating runaway cars, desyncs, and trains separating at chunk boundaries.
* **Atmospheric Visuals**: Detailed rear observation porch with handrails and a smoking coal-stove chimney pipe.
* **Universal Coupling**: Can be placed at the front, middle, or rear of the train consist.

---

## 5. Specialty Switch & Power Rails

```
     |                     \   /                   |   /
   --+-- (Junction)          X   (Wye Rail)        |  /  (Turnout)
     |                     / | \                   | /
```

### Junction Rail (4-Way Cross)
A flat, perpendicular cross rail (`+`).
* Unlike vanilla rails which force curves when intersecting, the Junction Rail allows trains coming from North-South and East-West to cross right through each other without turning, slowing down, or derailing.

---

### Turnout Rail (T-Switch)
A mainline straight rail with a divergent turnout branch.
* **Control**: Toggleable by **Redstone signal** or by right-clicking with a **Railroad Crowbar** or bare hand.
* **Animated Switch Blades**: Features movable rail points that visibly slide open and closed to indicate which route is currently set.
* **Non-Derailing Run-Through**: Trains approaching from the trailing frog can safely merge onto the mainline without bouncing.

---

### Wye Rail (Triangular 3-Way Switch)
A triangular junction rail designed for wye tracks, balloon loops, and mainline branches.
* **Directional Logic**:
  - Approaching from the **Stem**: Sent East or West depending on the Redstone state.
  - Approaching from the **Branches**: If the switch matches your incoming direction, you merge smoothly into the stem. If the switch is aligned for the opposite branch, you pass straight through along the mainline.
* **Visual Direction Indicators**: Highlights active guide rails in red/yellow so engineers can see switch alignment from a distance.

---

### Locomotive Control Rail & Horn Rail
Specialty functional tracks embedded directly into the railbed:

* **Locomotive Control Rail**:
  - **Unpowered**: Gently halts approaching trains and holds them stationary (ideal for automated passenger stations and freight loading docks).
  - **Powered**: Launches stopped trains forward at the locomotive's preset cruise gear. Reversing the redstone feed reverses train direction.
* **Locomotive Horn Rail**:
  - Pressure-plate rail track that automatically sounds the whistle/horn of any passing locomotive.
  - Essential for level road crossings, tunnel entrances, and warning track workers.

---

### Electric Rail (Third-Rail Power)
* A high-voltage third-rail track with copper contact conductors.
* Connects directly to Forge Energy (FE) power networks.
* Wirelessly charges **Electric Bullet Trains** at speeds up to **1,000 FE/tick** as they pass overhead, enabling endless electric train networks without battery swapping!

---

## 6. Trackside Automation Facilities

### Trackside Entity Loader & Unloader
Placed directly underneath tracks to automate livestock, villager, and mob transit:

```
    [ Rail Line ]
 -------------------
  [ Entity Loader ]
```

* **Entity Loader**:
  - Automatically boards nearby entities into passing empty minecarts or Livestock Carts.
  - Built-in GUI with configurable search radius (`1–16 blocks`).
  - **Filter Controls**: Whitelist or Blacklist specific mob IDs (e.g. `minecraft:cow`, `minecraft:sheep`), baby/adult filter, or custom nametags.
* **Entity Unloader**:
  - Safely dismounts passengers from passing carts.
  - Intelligently teleports dismounted mobs to adjacent solid ground away from tracks, preventing animals from being struck by trailing trains.

---

### Trackside Fluid Station
Placed directly beneath rails to automate fluid logistics:

* Equipped with an internal **16,000 mB (16-bucket)** buffer reservoir.
* Compatible with fluid pipes, hoppers, canisters, and buckets.
* **Smart Auto-Transfer**: Automatically fills or empties passing **Liquid Minecarts** when parked overhead.
* **Redstone Automation**: Emits a redstone pulse when a tanker car is completely full, empty, or finished pumping.

---

### Mechanical Track Signal Switch Stand
An authentic trackside switch stand with an animated directional lantern and mechanical target arm.

* **Named Train Dispatching**: Set custom routing rules in its GUI (e.g. route any locomotive named `"Ore Express"` onto the siding, while `"Mainline"` continues straight).
* **Signal Integration**: Automatically changes Turnout and Wye rails in response to approaching named trains.

---

### Railway Buffer Stop (Stop Block)
A heavy-duty end-of-track railway barrier modeled after industrial railroad buffer stops:

```
      [ Timber Bumper ]
         /|        |\
        / |========| \  <-- A-Frame Steel Legs
      [Rail]      [Rail]
```

* **Physical Rail Clamping**: Dual A-frame structural steel legs clamp onto incoming tracks at standard rail gauge.
* **Engine Auto-Cutoff**: When struck by a running locomotive or tunnel bore, it safely cuts throttle and stops the engine immediately while emitting authentic impact clatter sounds and sparks.
* **Terminal Redstone Detection**: Emits a continuous redstone signal (strength 15) while a train rests against it, ideal for triggering terminal station lights, uncouplers, or depot doors.
* **Crowbar Reorient**: Right-click with the Crowbar to rotate its facing direction.

---

### Industrial Chunk Anchor
A stationary, heavy-duty chunk loader block for factories, automated quarries, tree farms, and station hubs:

* **Configurable Radius**: Cycle between **1×1 Chunk (Single)**, **3×3 Chunks (9 Chunks)**, or **5×5 Chunks (25 Chunks)**.
* **Redstone Modes**: Toggle between **Always Active** and **Redstone Required**.
* **Visual Status**: Glowing cyan energy core, green status LED, and subtle portal particles.

---

## 7. Heavy Machinery: The Automated Tunnel Bore

The **Tunnel Bore** is a massive automated subterranean excavation machine that carves 3×3 subway tunnels through solid mountain rock.

```
       [ Top-Down Excavator Head ]
       |  [Stone]  [Stone]  [Stone]  |
       |  [Stone]  [AIR]    [Stone]  |  ---> 3x3 Tunnel Path
       |  [Solid]  [Rail]   [Solid]  |
```

### Key Engineering Features:

1. **3×3 Frontal Excavation**:
   - Pulverizes stone, deepslate, granite, ores, and dirt in a 3-block-wide by 3-block-tall cross-section.
   - Harvested block drops are deposited directly into the bore's internal storage inventory.
2. **Top-Down Drilling Sequence**:
   - Breaks overhead blocks first before drilling lower blocks.
   - When drilling through loose sand or gravel pockets, falling blocks collapse directly into the drill head and are cleared without suffocating the machine.
3. **Automated Track Laying**:
   - Automatically places rails from its **9-slot dedicated Track Inventory** onto solid ground directly beneath its rear bogies as it advances.
4. **Ballast Inventory & Auto Gap-Bridging**:
   - Features a dedicated **9-slot Ballast Inventory** accepting **gravel, sand, cobblestone, and stone**.
   - When the bore reaches an air gap, cave pothole, chasm, ravine, or liquid pool beneath its path, it **automatically places ballast blocks to bed a solid foundation**, lays the rail on top, and continues drilling without stopping!
5. **Safety Sensors**:
   - Automatically stops if it encounters unbreakable bedrock, a void deeper than available ballast reserves, or runs out of track/fuel.
6. **Bi-Directional Operation**:
   - Features forward drilling mode and slow, safe reversing mode that reverses trailing cars seamlessly.

---

## 8. Geological Prospecting & Survey System

### Surveyor's Charge
A specialized seismic explosive designed for subterranean resource prospecting and rapid vertical borehole excavation.
- **Crafting Recipe (Shapeless)**:
  - 1 Redstone Dust + 1 TNT + 1 Arrow
- **Detonation Triggers**:
  - Direct Redstone Signal (lever, button, pressure plate, wire).
  - Wireless trigger via **Remote Detonator**.
- **Blast Mechanics**:
  - Self-destructs instantly with a loud explosion sound and smoke plumes.
  - Blasts a clean 1×1 vertical borehole directly downward from its placement coordinate until bedrock or indestructible blocks (`destroySpeed < 0.0F`) are reached.
  - Configurable option to drop excavated borehole blocks as items or cleanly destroy them to eliminate item lag.
- **Geological Survey Scan & Oil Detection**:
  - Scans a cylindrical column of configurable horizontal radius (default 10 blocks) extending from the charge's placement level all the way down to bedrock.
  - Detects and counts all oil fluids and oil blocks (`#c:oil`, `#c:crude_oil`, and registry identifiers containing `crude`, `oil`, `petroleum`, `diesel`, or `fuel`).
  - Filters out common mundane blocks by default (stone, deepslate, dirt, sand, gravel, lava, water, etc.) while tallying and sorting all ores, minerals, and valuable deposits by quantity.
  - Transmits a comprehensive **Geological Survey Report** directly to the player in chat.

---

### Remote Detonator
A handheld wireless detonator keyed to trigger Surveyor's Charges.
- **Crafting Recipe (Shapeless)**:
  - 1 Redstone Dust + 1 Lever + 1 Iron Ingot
- **Operation**:
  - Right-click anywhere to detonate all Surveyor's Charges within a 12-block radius.
  - Immediately relays each charge's geological survey report directly into your chat.

---

### Configuration Options
Located in `config/miningtools-common.toml`:
- `surveyor_charge.radius` (Default: 10): Horizontal radius for the geological column scan from charge level down to bedrock.
- `surveyor_charge.dropBoreholeBlocks` (Default: false): Toggle item drops from the vertical borehole shaft.
- `surveyor_charge.remoteDetonatorRange` (Default: 12.0): Maximum activation distance for the Remote Detonator.
- `surveyor_charge.ignoredBlocks`: List of block IDs to omit from the survey tally (e.g. `["minecraft:stone", "minecraft:lava", ...]`).
- `surveyor_charge.extraReportBlocks`: List of specific modded block IDs to explicitly display in the report.

---

## 9. Complete Crafting Recipe Compendium

All recipes are crafted in a standard 3×3 Crafting Table:

### Locomotives & Motive Power

#### Basic Steam Locomotive
```
[ . ] [ R ] [ . ]    R = Redstone Lamp or Redstone Dust
[ I ] [ F ] [ I ]    I = Iron Ingot
[ I ] [ M ] [ I ]    F = Furnace, M = Minecart
```

#### CN Diesel Locomotive (#3087)
```
[ . ] [ G ] [ . ]    G = Glass
[ I ] [ R ] [ B ]    I = Iron Block, R = Redstone Block, B = Bucket
[ . ] [ M ] [ . ]    M = Minecart
```

#### Electric Bullet Train
```
[ . ] [ G ] [ . ]    G = Tinted Glass
[ I ] [ R ] [ C ]    I = Iron Block, R = Redstone Block, C = Copper Block
[ . ] [ M ] [ . ]    M = Minecart
```

---

### Rolling Stock & Defense Carts

#### Adaptive Cargo Minecart *(Shapeless)*
```
[ Minecart ] + [ Chest ] + [ Iron Ingot ]
```

#### Loose Aggregate Hopper Car
```
[ I ] [ . ] [ I ]    I = Iron Ingot
[ I ] [ H ] [ I ]    H = Hopper
[ . ] [ C ] [ . ]    C = Chest Minecart
```

#### Livestock Cart (Railroad Stock Car)
```
[ P ] [ P ] [ P ]    P = Wooden Planks (Any)
[ F ] [ L ] [ F ]    F = Wooden Fence (Any), L = Lead
[ P ] [ M ] [ P ]    M = Minecart
```

#### Liquid Minecart (8-Bucket Tanker)
```
[ . ] [ C ] [ . ]    C = Copper Ingot
[ I ] [ B ] [ I ]    I = Iron Ingot, B = Bucket
[ . ] [ M ] [ . ]    M = Minecart
```

#### Large Liquid Minecart (16-Bucket Tanker)
```
[ . ] [ I ] [ . ]    I = Iron Ingot
[ L ] [ B ] [ L ]    L = Liquid Minecart, B = Bucket
[ . ] [ I ] [ . ]
```

#### Arrow Turret Cart
```
[ B ] [ I ] [ B ]    B = Bow
[ I ] [ D ] [ I ]    I = Iron Ingot, D = Dispenser
[ I ] [ M ] [ I ]    M = Minecart
```

#### Anchor Caboose
```
[ . ] [ L ] [ . ]    L = Lantern
[ I ] [ E ] [ I ]    I = Iron Ingot, E = Ender Pearl
[ . ] [ M ] [ . ]    M = Minecart
```

---

### Specialty Rails & Tools

#### Railroad Crowbar
```
[ . ] [ I ] [ N ]    I = Iron Ingot
[ . ] [ I ] [ . ]    N = Iron Nugget
[ I ] [ . ] [ . ]
```

#### Junction Rail (Yields 4)
```
[ I ] [ . ] [ R ]    I = Iron Ingot
[ . ] [ R ] [ . ]    R = Standard Rail
[ R ] [ . ] [ I ]
```

#### Turnout Rail / T-Switch (Yields 4)
```
[ R ] [ . ] [ R ]    R = Standard Rail
[ . ] [ L ] [ . ]    L = Lever
[ R ] [ . ] [ R ]
```

#### Wye Rail / Y-Switch (Yields 4)
```
[ R ] [ . ] [ R ]    R = Standard Rail
[ . ] [ T ] [ . ]    T = Redstone Torch
[ R ] [ . ] [ R ]
```

#### Electric Third Rail (Yields 6)
```
[ C ] [ . ] [ S ]    C = Copper Ingot
[ C ] [ R ] [ C ]    R = Redstone Dust, S = Stick
[ C ] [ . ] [ S ]
```

#### Locomotive Control Rail (Yields 6)
```
[ I ] [ . ] [ I ]    I = Iron Ingot
[ I ] [ C ] [ I ]    C = Coal
[ I ] [ R ] [ I ]    R = Redstone Dust
```

#### Locomotive Horn Rail (Yields 6)
```
[ I ] [ . ] [ I ]    I = Iron Ingot
[ I ] [ G ] [ I ]    G = Gold Ingot
[ I ] [ R ] [ I ]    R = Redstone Dust
```

---

### Trackside Facilities & Heavy Machinery

#### Railway Buffer Stop / Stop Block (Yields 2)
```
[ I ] [ L ] [ I ]    I = Iron Ingot, L = Wooden Log (Any)
[ I ] [ R ] [ I ]    R = Redstone Dust
[ I ] [ B ] [ I ]    B = Iron Bars
```

#### Trackside Entity Loader
```
[ I ] [ L ] [ I ]    I = Iron Ingot, L = Lead
[ C ] [ H ] [ C ]    C = Copper Ingot, H = Hopper
[ I ] [ R ] [ I ]    R = Redstone Dust
```

#### Trackside Entity Unloader
```
[ I ] [ A ] [ I ]    I = Iron Ingot, A = Activator Rail
[ C ] [ H ] [ C ]    C = Copper Ingot, H = Hopper
[ I ] [ R ] [ I ]    R = Redstone Dust
```

#### Trackside Fluid Station
```
[ I ] [ P ] [ I ]    I = Iron Ingot, P = Piston
[ C ] [ H ] [ C ]    C = Copper Ingot, H = Hopper
[ I ] [ R ] [ I ]    R = Redstone Dust
```

#### Mechanical Track Switch Stand (Yields 2)
```
[ . ] [ R ] [ . ]    R = Redstone Torch
[ I ] [ L ] [ I ]    L = Lever, I = Iron Ingot
[ . ] [ I ] [ . ]
```

#### Industrial Chunk Anchor
```
[ I ] [ O ] [ I ]    I = Iron Ingot, O = Obsidian
[ G ] [ E ] [ G ]    G = Gold Ingot, E = Ender Pearl
[ I ] [ R ] [ I ]    R = Redstone Block
```

#### Automated Tunnel Bore
```
[ P ] [ B ] [ P ]    P = Iron Pickaxe, B = Iron Block
[ I ] [ F ] [ I ]    F = Furnace Minecart, I = Iron Ingot
[ I ] [ C ] [ I ]    C = Chest Minecart
```

---

### Geological Prospecting & Surveying Tools

#### Surveyor's Charge *(Shapeless)*
```
[ Redstone Dust ] + [ TNT ] + [ Arrow ]
```

#### Remote Detonator *(Shapeless)*
```
[ Redstone Dust ] + [ Lever ] + [ Iron Ingot ]
```

---

## 10. Version History & Changelog

For the complete, version-by-version release history and technical changes, see **[CHANGELOG.md](CHANGELOG.md)**.

### Summary of Recent Updates (v1.0.1):
- **Loose Aggregate Hopper Car**: 3D visual bulk aggregate heap mounds (Coal, Sand, Red Sand, Gravel/Ballast, Dirt, Raw Ore), hollow empty chutes, and filter GUI.
- **Surveyor's Charge & Remote Detonator**: Full-depth cylindrical column survey down to bedrock with crude oil detection, borehole blasting, and wireless detonation.
- **Diesel Fuel Tender Siphoning**: CN Diesel Locomotive siphons fuel from directly coupled liquid tanker cars.
- **Universal Consist Coupling & Kinematics**: Couple rolling stock without a locomotive, shunt unpowered consists by hand, and place Cabooses anywhere in the train.
- **Physics & Anti-Phasing**: Resolved cart phasing bugs, continuous slope elevation tracking, and smooth momentum transfer.
- **Recipe Codec Fix**: Server-compatible recipe parsing for 1.21.4 / 26.3 NeoForge.

---
*Mining Tools — Built for heavy rail engineering and automated logistics.*
'@

$path = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\WIKI.md"
[System.IO.File]::WriteAllText($path, $wikiContent, [System.Text.UTF8Encoding]::new($false))
Write-Host "WIKI.md updated successfully."
