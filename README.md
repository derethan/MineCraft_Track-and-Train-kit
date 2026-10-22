# Mining Tools

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.3.0.48--beta-orange.svg)](https://neoforged.net/)
[![Version](https://img.shields.io/badge/Version-1.0.1-blue.svg)](https://github.com/)

**Mining Tools** is an industrial railway and excavation mod for **Minecraft 26.3 (NeoForge)**. Inspired by classic railway engineering and heavy mining machinery, Mining Tools introduces powerful steam locomotives, automated 3×3 tunnel-boring machines, adaptive freight carts, intelligent specialty switch rails, and a live HUD speedometer.

---

## Table of Contents

- [Features Overview](#features-overview)
- [Rolling Stock & Machinery](#rolling-stock--machinery)
  - [Basic Locomotive](#basic-locomotive)
  - [Tunnel Bore](#tunnel-bore)
  - [Cargo Minecart](#cargo-minecart)
- [Specialty Rails](#specialty-rails)
  - [Control Rail](#control-rail)
  - [Turnout Rail](#turnout-rail)
  - [Wye Rail](#wye-rail)
  - [Junction Rail](#junction-rail)
- [HUD Speedometer & Telemetry](#hud-speedometer--telemetry)
- [Train Coupling & Compatibility](#train-coupling--compatibility)
- [Crafting Recipes](#crafting-recipes)
- [Configuration](#configuration)
- [Building & Installation](#building--installation)

---

## Features Overview

- 🚂 **Basic Locomotive**: A coal-burning steam engine capable of pulling and pushing trains with 5 selectable speed levels, dynamic directional headlights, and an interactive steam whistle.
- 🚜 **Tunnel Bore**: A heavy multi-block excavator that automatically bores 3×3 tunnels through terrain, clears gravity blocks safely, and lays track forward as it advances.
- 📦 **Cargo Minecart**: High-capacity freight cart (27 slots) featuring dynamic 3D visuals that transform based on configured cargo filters (Logs, Lumber, Ores, Ingots, or Shipping Container).
- 🛤️ **Specialty Switch & Junction Rails**:
  - **Control Rail**: Redstone-activated locomotive station and dispatch rail.
  - **Turnout Rail**: Mainline/divergent switch rail toggled via Redstone or manual interaction.
  - **Wye Rail**: 3-way Y-junction switch track with momentum-aware path routing and in-place rotation.
  - **Junction Rail**: 4-way flat perpendicular cross intersection (+) allowing trains to cross without collisions or unwanted turns.
- 🧭 **In-Game HUD Dashboard**: On-screen speedometer displaying blocks/sec (m/s), km/h, compass heading, active throttle gear, and locomotive fuel reserve.
- 🔗 **Train Integration**: Native compatibility with train chaining systems (such as *Minecart Trains Fork*), featuring calibrated hitch offsets and rear couplers.

---

## Rolling Stock & Machinery

### Basic Locomotive

The **Basic Locomotive** provides mainline motive power to haul freight and passenger trains across your rail network.

- **Fuel**: Consumes coal or charcoal (default: 1,600 ticks / 80 seconds per piece).
- **Speed Levels**: 5 adjustable speed gears (configurable from slow yard shunting up to 0.35+ blocks/tick).
- **Dynamic Headlight**: Projects a movable beam of light (light level 15) ahead of the train along track vectors.
- **Steam Whistle / Horn**: Sounds an audible locomotive horn, emits steam/smoke particle bursts, and panics/scares nearby mobs within 12 blocks off the tracks.
- **Cowcatcher / Mob Clearing**: Moving locomotives push stray entities off the rails, preventing carts from stalling on livestock or monsters.
- **Rail Automation**:
  - Activator Rails trigger the whistle automatically.
  - Control Rails automatically halt or start the engine.

#### Controls (Keyboard & GUI)

When riding a Locomotive or any coupled cart in a train:

| Key Binding | Default Key | Action |
| :--- | :---: | :--- |
| **Throttle Up / Start** | `W` (Forward) | Ignites fuel and starts locomotive movement |
| **Brake / Stop** | `S` (Backward) | Cuts throttle and brings the train to a halt |
| **Gear Down** | `A` (Left) | Decreases target speed level (1 to 5) |
| **Gear Up** | `D` (Right) | Increases target speed level (1 to 5) |
| **Reverse** | `R` | Reverses locomotive travel direction |
| **Whistle / Horn** | `H` | Sounds steam whistle and clears track of mobs |
| **Headlight** | `L` | Toggles dynamic front headlight on / off |

*All controls are also accessible via interactive buttons inside the Locomotive GUI.*

---

### Tunnel Bore

The **Tunnel Bore** is an industrial excavation machine designed for automated subterranean rail construction.

- **3×3 Excavation**: Automatically clears a 3-block-wide by 3-block-tall tunnel directly along its heading.
- **Top-Down Drilling**: Mines each cross-section layer from top to bottom so gravity blocks (gravel, sand) collapse safely into the drill path rather than suffocating the machine.
- **Auto-Track Laying**: Automatically places rails from its internal inventory onto solid ground ahead of itself before stepping forward.
- **Safety System**: Automatically halts if it encounters unbreakable blocks (e.g., Bedrock), liquid hazards, a pit without floor support, or runs out of track/fuel.
- **Inventory & Fuel**:
  - **Slot 0**: Fuel slot (Coal / Charcoal).
  - **Slots 1–9**: Rail storage buffer (holds standard rails, powered rails, etc.).
- **Animated Model**: Features an animated rotating cutter head, dual steam smokestacks, and drilling impact particles.
- **Coupler Hitch**: Sits on rails with a rear hitch offset calibrated for pulling trailing cargo wagons.

---

### Cargo Minecart

The **Cargo Minecart** offers heavy-duty bulk transport with integrated item filtering and adaptive visuals.

- **Storage Capacity**: 27 standard inventory storage slots + 1 dedicated **Filter Slot**.
- **Adaptive Visual Appearance**:
  - **Log Filter**: Accepts tree logs (`#minecraft:logs`); renders with a specialized logging cart frame and strapped timber load.
  - **Wood Filter**: Accepts 6-sided bark wood/hyphae blocks; renders as an open lumber cart.
  - **Ore Filter**: Accepts raw ore chunks and ore blocks (`#c:ores`, `#c:raw_materials`); renders with heavy iron ore bins.
  - **Ingot Filter**: Accepts refined ingots and nuggets (`#c:ingots`, `#c:nuggets`); renders with reinforced strongbox containers.
  - **Unfiltered / General Cargo**: When loaded with general items without a filter, renders with an industrial corrugated shipping container visual.
  - **Empty**: Classic open-bed cargo minecart.
- **Smart Filter Locking**: Prevents removing or changing an active filter while conflicting items remain inside the cart.
- **Automation Ready**: Implements standard hopper/automation interfaces (`WorldlyContainer`) for automatic loading and unloading depots.

---

## Specialty Rails

| Rail Block | Description | Usage / Switching Mechanics |
| :--- | :--- | :--- |
| **Control Rail** | Locomotive dispatch & holding track | **Unpowered**: Halts arriving locomotives.<br>**Powered**: Fires up and dispatches stopped locomotives. |
| **Turnout Rail** | Mainline / Divergent switch track | **Redstone / Right-Click**: Toggles between Mainline (straight) and Divergent (curved).<br>**Shift + Right-Click**: Swaps curve direction (Left ↔ Right). |
| **Wye Rail** | 3-way Y-junction switch track | **Momentum-Aware Routing**: Straight-through traffic passes uninterrupted; stem traffic routes onto the active branch.<br>**Redstone / Right-Click**: Swaps active branch (Left ↔ Right).<br>**Shift + Right-Click**: Rotates orientation clockwise in-place. |
| **Junction Rail** | 4-way perpendicular cross intersection (+) | Allows intersecting North-South and East-West rail lines to cross perpendicularly without carts derailing or turning. |

---

## HUD Speedometer & Telemetry

Whenever a player rides a locomotive, minecart, or train wagon, an industrial cockpit HUD appears on screen:

- **Vehicle Identifier & Compass**: Shows vehicle name and cardinal heading (`N`, `NE`, `E`, etc.), including an asterisk indicator (`*`) when headlights are engaged.
- **Real-Time Speed**: Measured in both **m/s (blocks per second)** and **km/h**.
- **Dynamic Speed Bar**: Segmented color gradient bar indicating current acceleration and reference velocity.
- **Locomotive Telemetry**:
  - **Gear / Speed Level**: Active target speed level (`L1` through `L5`).
  - **Fuel Gauge**: Visual fuel percentage and coal stock indicators.

---

## Train Coupling & Compatibility

Mining Tools is designed with native support for multi-cart train mechanics:

- **Minecart Trains Fork Compatibility**: Fully integrated with train chaining and coupling systems.
- **Anchor Offsets**: Locomotives, cargo carts, and the tunnel bore define calibrated hitch points (`getRearAnchorPos()` / `getFrontAnchorPos()`) to guarantee realistic chain spacing without clipping or model collision.
- **Train-Wide Controls**: Players seated in any coupled carriage within a train can control the lead locomotive using throttle hotkeys.

---

## Crafting Recipes

### Basic Locomotive
```
[ Iron Ingot ] [ Redstone / Lamp ] [ Iron Ingot ]
[ Iron Ingot ] [     Furnace     ] [ Iron Ingot ]
[ Iron Ingot ] [     Minecart    ] [ Iron Ingot ]
```
*(Also crafts with a single Redstone/Lamp in top-center slot)*

### Tunnel Bore
```
[ Iron Pickaxe ] [   Iron Block   ] [ Iron Pickaxe ]
[  Iron Ingot  ] [Furnace Minecart] [  Iron Ingot  ]
[  Iron Ingot  ] [ Chest Minecart ] [  Iron Ingot  ]
```

### Cargo Minecart
*Shapeless recipe:*
```
[ Minecart ] + [ Chest ] + [ Iron Ingot ]
```

### Control Rail (Yields 6)
```
[ Iron Ingot ] [            ] [ Iron Ingot ]
[ Iron Ingot ] [    Coal    ] [ Iron Ingot ]
[ Iron Ingot ] [  Redstone  ] [ Iron Ingot ]
```

### Turnout Rail (Yields 4)
```
[ Rail ] [       ] [ Rail ]
[      ] [ Lever ] [      ]
[ Rail ] [       ] [ Rail ]
```

### Wye Rail (Yields 4)
```
[ Rail ] [                ] [ Rail ]
[      ] [ Redstone Torch ] [      ]
[ Rail ] [                ] [ Rail ]
```

### Junction Rail (Yields 4)
```
[ Iron Ingot ] [            ] [    Rail    ]
[            ] [    Rail    ] [            ]
[    Rail    ] [            ] [ Iron Ingot ]
```

---

## Configuration

Configuration is handled via NeoForge's configuration system in `config/miningtools.common.toml` or directly in-game via the Mods menu:

```toml
[locomotive]
    # Duration in game ticks that each piece of coal or charcoal burns (default: 1600 ticks = 80 seconds)
    # Range: 20 ~ 72000
    coalBurnTicks = 1600

    # Maximum travel speed of the Basic Locomotive at Speed Level 5 in blocks per tick (vanilla max is 0.4)
    # Range: 0.05 ~ 1.0
    maxSpeed = 0.35

    # Acceleration rate of the Basic Locomotive when starting up
    # Range: 0.005 ~ 0.2
    acceleration = 0.04
```

---

## Building & Installation

### Requirements
- **Minecraft**: `26.3`
- **Mod Loader**: [NeoForge](https://neoforged.net/) (`26.3.0.48-beta` or compatible)
- **Java**: Java 21+

### Building from Source
Clone the repository and run the Gradle wrapper:

```bash
# Refresh dependencies and build JAR
./gradlew build

# Launch client development environment
./gradlew runClient
```

The compiled mod JAR will be output to `build/libs/`. Place the JAR into your Minecraft `.minecraft/mods` directory alongside NeoForge.
