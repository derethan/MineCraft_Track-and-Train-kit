package com.example.miningtools.guide;

import com.example.miningtools.MiningTools;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class GuideBookRegistry {
    private static final List<GuideEntry> ENTRIES = new ArrayList<>();
    private static final Map<String, GuideEntry> BY_ID = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // =========================================================================
        // CATEGORY: LOCOMOTIVES
        // =========================================================================
        register(new GuideEntry(
                "basic_locomotive",
                "Basic Steam Locomotive",
                "Coal-Fired Light Engine • Cruise Control",
                GuideCategory.LOCOMOTIVES,
                () -> new ItemStack(MiningTools.BASIC_LOCOMOTIVE_ITEM.get()),
                "The Basic Steam Locomotive is a versatile coal-fired engine designed for light shunting and short-to-medium range freight operations.\n\n" +
                "• Controls: Mount with Right-Click. Use W to accelerate and S to brake/reverse through Gears (1 to 5, Neutral, and Reverse R1 to R3).\n" +
                "• Horn & Whistle: Press 'H' while seated to sound the atmospheric steam whistle, alerting players and scaring trackside animals.\n" +
                "• Headlight: Press 'L' to toggle luminous headlights for nighttime travel.\n" +
                "• Cab GUI: Press 'C' or sneak-right-click while looking at the engine to open the internal firebox and burn timer interface.\n" +
                "• Fueling: Consumes Coal, Charcoal, Blaze Rods, or Wood. Automatically refuels from adjacent coupled tenders or Coal Fueler stations.",
                "Couple an Aggregate Hopper Cart or Cargo Minecart behind the cab with a Crowbar to create a tender that continuously refuels the locomotive!",
                null,
                null,
                List.of(
                        new GuideRecipe("Basic Locomotive Crafting",
                                List.of(() -> new ItemStack(Items.IRON_INGOT, 4), () -> new ItemStack(Items.FURNACE), () -> new ItemStack(Items.MINECART), () -> new ItemStack(Items.REDSTONE_LAMP)),
                                List.of(() -> new ItemStack(MiningTools.BASIC_LOCOMOTIVE_ITEM.get())),
                                "Standard steam locomotive chassis with front-mounted headlight.")
                )
        ));

        register(new GuideEntry(
                "diesel_locomotive",
                "Diesel Locomotive",
                "Heavy Liquid-Fueled Engine • Long Haul",
                GuideCategory.LOCOMOTIVES,
                () -> new ItemStack(MiningTools.DIESEL_LOCOMOTIVE_ITEM.get()),
                "The Diesel Locomotive is an industrial heavyweight engineered for massive cargo consists, steep grades, and high-speed transcontinental hauls.\n\n" +
                "• Fluid Fuel Tank: Equipped with an internal liquid tank accepting Diesel, Biodiesel, Fuel Oil, or Crude Oil.\n" +
                "• Superior Torque: Easily pulls dozens of fully loaded freight cars without stalling or bogging down.\n" +
                "• Transmission: 5 forward gears reaching speeds up to 18 m/s, plus dual-note electric horn and high-intensity twin headlights.\n" +
                "• Automated Refueling: Seamlessly pulls fuel from parked Diesel Fueler depots or adjacent coupled Liquid Minecarts.",
                "Diesel engines maintain their target cruising gear automatically across hilly terrain without user throttle babysitting.",
                null,
                null,
                List.of(
                        new GuideRecipe("Diesel Locomotive Crafting",
                                List.of(() -> new ItemStack(Items.GLASS), () -> new ItemStack(Items.IRON_BLOCK), () -> new ItemStack(Items.REDSTONE_BLOCK), () -> new ItemStack(Items.BUCKET), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.DIESEL_LOCOMOTIVE_ITEM.get())),
                                "High-compression diesel engine block with heavy cast ballast body.")
                )
        ));

        register(new GuideEntry(
                "electric_locomotive",
                "Electric Locomotive",
                "Third-Rail / Battery Hauler • Zero Emissions",
                GuideCategory.LOCOMOTIVES,
                () -> new ItemStack(MiningTools.ELECTRIC_LOCOMOTIVE_ITEM.get()),
                "The Electric Locomotive offers whisper-quiet operation, instant high-torque acceleration, and supreme energy efficiency.\n\n" +
                "• Power Sources: Picks up electric power continuously from trackside Electric Rails, or draws from coupled Generator Carts or internal energy cells.\n" +
                "• Infinite Range on Electric Track: As long as the locomotive rides on energized Electric Rails, it consumes no fuel items and never runs out of power.\n" +
                "• Cab Controls: Features regenerative electric braking, horn, headlights, and an interactive electric power management screen.",
                "Lay third-rail Electric Tracks along primary trunk lines for infinite automated passenger and cargo express service!",
                null,
                null,
                List.of(
                        new GuideRecipe("Electric Locomotive Crafting",
                                List.of(() -> new ItemStack(Items.TINTED_GLASS), () -> new ItemStack(Items.IRON_BLOCK), () -> new ItemStack(Items.REDSTONE_BLOCK), () -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "copper_block"))), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.ELECTRIC_LOCOMOTIVE_ITEM.get())),
                                "High-efficiency electric traction motors and copper pickup shoes.")
                )
        ));

        // =========================================================================
        // CATEGORY: ROLLING STOCK
        // =========================================================================
        register(new GuideEntry(
                "cargo_minecart",
                "Cargo Minecart",
                "High-Capacity Freight Wagon • 54 Slots",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.CARGO_MINECART_ITEM.get()),
                "The Cargo Minecart provides massive 54-slot double-chest freight storage in an articulated railway chassis.\n\n" +
                "• Consist Chaining: Link behind any locomotive or cart using the Railroader's Crowbar (right-click cart A, then cart B).\n" +
                "• Naming & Routing: Sneak-right-click with a Crowbar to open the Cart Rename GUI. Named wagons allow Cargo Loaders and Track Switches to route them intelligently.\n" +
                "• Automated Transfer: Interacts with Cargo Loaders and Cargo Unloaders to load/unload items at lightning speeds.",
                "Name your freight carts with destination codes (e.g. 'MINE_ALPHA' or 'SMELTER') to trigger automated siding switches!",
                null,
                null,
                List.of(
                        new GuideRecipe("Cargo Minecart Crafting",
                                List.of(() -> new ItemStack(Items.CHEST), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.CARGO_MINECART_ITEM.get())),
                                "Spacious steel-reinforced freight boxcar.")
                )
        ));

        register(new GuideEntry(
                "hopper_cart",
                "Aggregate Hopper Cart",
                "Bulk Gravel & Ore Hopper • Gravity Chute",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.HOPPER_CART_ITEM.get()),
                "The Aggregate Hopper Cart is specifically tailored for handling heavy loose bulk materials: Cobblestone, Gravel, Sand, Ballast, and Raw Ores.\n\n" +
                "• Chute Discharge: Rapidly unloads straight down when parked over hoppers or automated station chutes.\n" +
                "• Trackbed Construction: Automatically feeds gravel and ballast directly into coupled Under Track Pavers and Track Layer Carts.",
                "Couple a Hopper Cart directly behind an Under Track Paver to build hundreds of blocks of continuous trackbed without manual reloading!",
                null,
                null,
                List.of(
                        new GuideRecipe("Aggregate Hopper Cart Crafting",
                                List.of(() -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.HOPPER_CART_ITEM.get())),
                                "High-capacity open hopper for gravel and mineral aggregates.")
                )
        ));

        register(new GuideEntry(
                "liquid_minecart",
                "Liquid Tank Car",
                "16,000 mB Fluid Tanker • Universal Fluids",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.LIQUID_MINECART_ITEM.get()),
                "The Liquid Minecart safely transports up to 16,000 mB (16 buckets) of any liquid, including Lava, Water, Petroleum, Diesel, and Chemical Acids.\n\n" +
                "• Equalization: Adjacent liquid carts coupled together in a train equalize their fluid levels automatically across the consist.\n" +
                "• Fluid Stations: Quickly loads and drains at Fluid Loader and Fluid Unloader gantry stations.\n" +
                "• Engine Supply: Can act as an auxiliary tender to supply fuel directly to coupled Diesel Locomotives.",
                "Link multiple liquid carts in series behind a locomotive to transport an entire lake of oil or lava in a single run!",
                null,
                null,
                List.of(
                        new GuideRecipe("Liquid Tank Car Crafting",
                                List.of(() -> new ItemStack(Items.COPPER_INGOT), () -> new ItemStack(Items.IRON_INGOT, 2), () -> new ItemStack(Items.BUCKET), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.LIQUID_MINECART_ITEM.get())),
                                "Sealed cylindrical fluid tanker on a rail bogie.")
                )
        ));

        register(new GuideEntry(
                "large_liquid_minecart",
                "Heavy Liquid Tank Car",
                "32,000 mB Super-Tanker • Bulk Petrochemicals",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.LARGE_LIQUID_MINECART_ITEM.get()),
                "The Heavy Liquid Tank Car features double the volume of the standard tank wagon, holding 32,000 mB (32 buckets) of pressurized fluids.\n\n" +
                "• Heavy Metallurgy: Reinforced with solid iron plates to withstand extreme pressures and hazardous fluids without rupture.\n" +
                "• Industrial Logistics: Ideal for high-yield transcontinental oil shipping between remote drilling derricks and base refineries.",
                "Use Heavy Liquid Tankers for long-distance runs to maximize fluid payload per locomotive journey.",
                null,
                null,
                List.of(
                        new GuideRecipe("Heavy Liquid Tank Car Crafting",
                                List.of(() -> new ItemStack(MiningTools.LIQUID_MINECART_ITEM.get(), 2), () -> new ItemStack(Items.BUCKET), () -> new ItemStack(Items.IRON_INGOT, 2)),
                                List.of(() -> new ItemStack(MiningTools.LARGE_LIQUID_MINECART_ITEM.get())),
                                "Super-duty reinforced tanker with dual bogie trucks.")
                )
        ));

        register(new GuideEntry(
                "livestock_cart",
                "Livestock Transport Wagon",
                "Multi-Entity Stock Car • Humane Animal Transit",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.LIVESTOCK_CART_ITEM.get()),
                "The Livestock Transport Wagon enables stress-free transport of cows, sheep, pigs, horses, and even villagers across your rail network.\n\n" +
                "• Automatic Loading: Interacts seamlessly with Entity Loader blocks, which draw nearby animals into the cart automatically.\n" +
                "• Gentle Disembarkation: Entity Unloader blocks gently disembark passengers at stock yards without harming them.\n" +
                "• High Capacity: Holds multiple entities simultaneously while preserving their names and leash attachments.",
                "Build automated pastures where animals are automatically loaded at the ranch and shipped to your central base!",
                null,
                null,
                List.of(
                        new GuideRecipe("Livestock Transport Wagon Crafting",
                                List.of(() -> new ItemStack(Items.OAK_PLANKS, 4), () -> new ItemStack(Items.OAK_FENCE, 2), () -> new ItemStack(Items.LEAD), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.LIVESTOCK_CART_ITEM.get())),
                                "Ventilated wooden slatted livestock car.")
                )
        ));

        register(new GuideEntry(
                "generator_cart",
                "Mobile Generator Cart",
                "Solid Fuel Power Station • FE / RF Generation",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.GENERATOR_MINECART_ITEM.get()),
                "The Generator Cart burns solid fuels (coal, charcoal, wood) to generate continuous Redstone Flux / Forge Energy on the rails.\n\n" +
                "• Mobile Third-Rail: Directly powers coupled Electric Locomotives on non-electrified track networks.\n" +
                "• Field Battery: Distributes power to adjacent machinery and electric tools in the field.",
                "Place a Generator Cart directly behind an Electric Locomotive to explore uncharted lands without laying electric rail ahead of time!",
                null,
                null,
                List.of(
                        new GuideRecipe("Generator Cart Crafting",
                                List.of(() -> new ItemStack(Items.COPPER_INGOT, 2), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.BLAST_FURNACE), () -> new ItemStack(Items.IRON_INGOT, 2), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.GENERATOR_MINECART_ITEM.get())),
                                "High-output mobile alternator and combustion generator.")
                )
        ));

        register(new GuideEntry(
                "turret_cart",
                "Armored Sentry Cart",
                "Automated Perimeter Defense • Rapid-Fire Bow",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.TURRET_CART_ITEM.get()),
                "The Armored Sentry Cart mounts an automated defensive turret that scans 360 degrees for hostile monsters and fires rapid projectiles.\n\n" +
                "• Ammunition: Open its inventory to stock arrows, tipped arrows, or fire charges.\n" +
                "• Target Acquisition: Locks onto Creepers, Skeletons, Zombies, and Spiders approaching the train or railroad corridor.\n" +
                "• Convoy Escort: Protects valuable freight convoys passing through unlit tunnels and treacherous mountain passes.",
                "Equip the turret with Tipped Arrows of Slowness or Weakness to immobilize encroaching creeper swarms before they reach the tracks!",
                null,
                null,
                List.of(
                        new GuideRecipe("Armored Sentry Cart Crafting",
                                List.of(() -> new ItemStack(Items.BOW, 2), () -> new ItemStack(Items.DISPENSER), () -> new ItemStack(Items.IRON_INGOT, 3), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.TURRET_CART_ITEM.get())),
                                "Armored chassis with gimbaled autonomous arrow launcher.")
                )
        ));

        register(new GuideEntry(
                "caboose",
                "Railroad Caboose",
                "End-of-Train Safety • Crew Lounge & Lanterns",
                GuideCategory.ROLLING_STOCK,
                () -> new ItemStack(MiningTools.CABOOSE_ITEM.get()),
                "The classic Railroad Caboose provides an end-of-train visual marker, passenger seating, crew cabin workspace, and safety brakes.\n\n" +
                "• Anti-Derailment: Couples to the rear of the train to stabilize trailing momentum and eliminate awkward consist recoil.\n" +
                "• Rear Marker Lights: Illuminates the track behind your train with bright warning lanterns.\n" +
                "• Amenities: Features interior seating, observation windows, and mobile workbench.",
                "Every grand locomotive consist deserves a classic red caboose at the rear for safety and style!",
                null,
                null,
                List.of(
                        new GuideRecipe("Railroad Caboose Crafting",
                                List.of(() -> new ItemStack(Items.LANTERN), () -> new ItemStack(Items.ENDER_PEARL), () -> new ItemStack(Items.IRON_INGOT, 2), () -> new ItemStack(Items.MINECART)),
                                List.of(() -> new ItemStack(MiningTools.CABOOSE_ITEM.get())),
                                "Traditional railroad caboose with observation cupola.")
                )
        ));

        // =========================================================================
        // CATEGORY: TRACKS & SWITCHES
        // =========================================================================
        register(new GuideEntry(
                "junction_rail",
                "Junction Rail",
                "Perpendicular 90° Cross Track • Anti-Collision",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.JUNCTION_RAIL_ITEM.get()),
                "The Junction Rail allows two orthogonal railway lines (North-South and East-West) to cross directly on the same level.\n\n" +
                "• No Misdirection: Carts traveling straight will never accidentally turn or deroute at the crossing.\n" +
                "• Compact Layouts: Eliminates expensive, bulky overpass bridges in tight rail yards and multi-track stations.",
                "Use Junction Rails to design compact grid railway yards and clean intersection crossings.",
                null,
                null,
                List.of(
                        new GuideRecipe("Junction Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 4), () -> new ItemStack(Items.IRON_INGOT)),
                                List.of(() -> new ItemStack(MiningTools.JUNCTION_RAIL_ITEM.get(), 4)),
                                "Crafts 4 perpendicular cross-track junction rails.")
                )
        ));

        register(new GuideEntry(
                "turnout_rail",
                "Turnout Rail (Right)",
                "Switch Track • Straight to Right Branch",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.TURNOUT_RAIL_ITEM.get()),
                "The Turnout Rail (Right) splits a single rail line into a straight mainline and a divergent right-curved branch.\n\n" +
                "• Switchable Points: Driven by redstone signals or linked Track Switch stands to toggle the active route.\n" +
                "• Manual Inversion: Sneak-right-click with an empty hand or Crowbar to invert between left and right branch paths.\n" +
                "• Conversion: Can be converted 1:1 into a Turnout Rail (Left) directly in any crafting grid.",
                "Place a Track Switch stand next to the turnout rail to operate it mechanically with full lever animations!",
                null,
                null,
                List.of(
                        new GuideRecipe("Turnout Rail (Right) Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 4), () -> new ItemStack(Items.LEAD), () -> new ItemStack(Items.LEVER)),
                                List.of(() -> new ItemStack(MiningTools.TURNOUT_RAIL_ITEM.get(), 4)),
                                "Crafts 4 right-turnout switch tracks.")
                )
        ));

        register(new GuideEntry(
                "left_turnout_rail",
                "Turnout Rail (Left)",
                "Switch Track • Straight to Left Branch",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.LEFT_TURNOUT_RAIL_ITEM.get()),
                "The Turnout Rail (Left) is a dedicated switch rail pre-configured to branch off to the left.\n\n" +
                "• Smooth Divergence: Perfectly mirrors the right turnout for building parallel passing sidings and crossovers.\n" +
                "• Signal Interlocking: Responds to Track Signal Switch stands and wireless cab signal routing commands.\n" +
                "• Crafting Conversion: Interchangeable with Turnout Rail (Right) in shapeless crafting.",
                "Combine left and right turnouts to build parallel passing sidings on single-track mainlines!",
                null,
                null,
                List.of(
                        new GuideRecipe("Turnout Rail Conversion",
                                List.of(() -> new ItemStack(MiningTools.TURNOUT_RAIL_ITEM.get())),
                                List.of(() -> new ItemStack(MiningTools.LEFT_TURNOUT_RAIL_ITEM.get())),
                                "Shapeless 1:1 conversion from Right Turnout to Left Turnout.")
                )
        ));

        register(new GuideEntry(
                "wye_rail",
                "Wye Rail (Equilateral)",
                "Symmetrical Y-Split • Reversing Triangles",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.WYE_RAIL_ITEM.get()),
                "The Wye Rail splits incoming straight track into dual symmetrical 45-degree diverging branches.\n\n" +
                "• Reversing Wyes: Used to construct reversing triangles where full trains can reverse travel heading without tight loops.\n" +
                "• Redstone & Switch Control: Alternates between left and right divergent paths when powered or mechanically switched.",
                "Build a 3-track railway wye at end stations to turn locomotives around without loops!",
                null,
                null,
                List.of(
                        new GuideRecipe("Wye Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 4), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.WYE_RAIL_ITEM.get(), 4)),
                                "Crafts 4 symmetrical Y-split wye rails.")
                )
        ));

        register(new GuideEntry(
                "speed_control_rail",
                "Speed Control Rail",
                "Interactive Speed Governor • Selective Routing",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.SPEED_CONTROL_RAIL_ITEM.get()),
                "The Speed Control Rail enforces target speeds on passing locomotives with an interactive configuration GUI.\n\n" +
                "• Interactive GUI: Right-click the track to set target Gears (Neutral / Stop, Gears 1 to 5, and Reverse R1 to R3).\n" +
                "• Direction Mode: Configure whether the speed rule applies in both directions, forward-only, or reverse-only.\n" +
                "• Train Name Filter: Apply speed restrictions selectively (e.g. only enforce low gear on 'FREIGHT_HEAVY' while 'EXPRESS' speeds through).\n" +
                "• Redstone Bypass: Invert or disable the governor with redstone power.",
                "Install a Speed Control Rail set to Gear 1 before sharp yard curves to prevent runaway derailments!",
                null,
                null,
                List.of(
                        new GuideRecipe("Speed Control Rail Crafting",
                                List.of(() -> new ItemStack(Items.POWERED_RAIL, 2), () -> new ItemStack(Items.CLOCK), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.SPEED_CONTROL_RAIL_ITEM.get(), 4)),
                                "Crafts 4 interactive speed control rails.")
                )
        ));

        register(new GuideEntry(
                "delay_rail",
                "Station Delay Rail",
                "Automated Dwell Timer • Station Hold & Launch",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.DELAY_RAIL_ITEM.get()),
                "The Station Delay Rail holds passing trains and locomotives for a precise dwell duration before automatically launching them onward.\n\n" +
                "• Interactive Dwell GUI: Right-click the track to toggle the timer ON/OFF, set the delay time from 1 to 3600 seconds, and use quick preset buttons (3s, 5s, 10s, 15s, 30s, 60s).\n" +
                "• Departure Control: Configure departure heading to maintain arrival momentum, reverse departure, or force launch in a designated cardinal track direction.\n" +
                "• Consist Brakes: Automatically clamps the brakes across all linked cars in a train consist so passenger or freight cars stay securely docked.\n" +
                "• Train Name Filtering: Restrict dwell timer holds only to specific train consist names (e.g. 'PASSENGER_LOCAL') while freight expresses pass through uninterrupted.",
                "Place Station Delay Rails at passenger platforms or freight loading bays to allow complete cargo transfer cycles before the train departs!",
                null,
                null,
                List.of(
                        new GuideRecipe("Station Delay Rail Crafting",
                                List.of(() -> new ItemStack(Items.POWERED_RAIL, 2), () -> new ItemStack(Items.REPEATER), () -> new ItemStack(Items.CLOCK), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.DELAY_RAIL_ITEM.get(), 4)),
                                "Crafts 4 station delay timer rails.")
                )
        ));

        register(new GuideEntry(
                "coupler_rail",
                "Coupler Rail",
                "Automated Hump Yard Shunting • Auto-Couple / Uncouple",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.COUPLER_RAIL_ITEM.get()),
                "The Coupler Rail automates the coupling and uncoupling of rolling stock as trains pass overhead.\n\n" +
                "• Automated Uncoupling: Send a redstone pulse as a train rolls over to instantly release trailing wagons into sorting sidings.\n" +
                "• Automated Coupling: Automatically engages couplers between adjacent cars when pushed together over the track.\n" +
                "• Modern Hump Yards: Build fully automated gravity shunting yards without manual crowbar work.",
                "Pulse the coupler rail with a tripwire hook or detector rail to detach cabooses or cargo wagons automatically at station approaches!",
                null,
                null,
                List.of(
                        new GuideRecipe("Coupler Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 2), () -> new ItemStack(Items.TRIPWIRE_HOOK), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.COUPLER_RAIL_ITEM.get(), 4)),
                                "Crafts 4 automated coupling & uncoupling rails.")
                )
        ));

        register(new GuideEntry(
                "control_rail",
                "Directional Control Rail",
                "One-Way Booster & Hill Hold",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.CONTROL_RAIL_ITEM.get()),
                "The Control Rail accelerates minecarts exclusively in the direction indicated by its glowing arrow chevron markings.\n\n" +
                "• Anti-Rollback: Prevents heavy freight trains from rolling backward down steep mountain grades.\n" +
                "• Redstone Reversal: Powering the rail can invert the thrust direction, functioning as a reversible booster.",
                "Place Control Rails on steep incline slopes to guarantee your heaviest ore trains make it up the mountain!",
                null,
                null,
                List.of(
                        new GuideRecipe("Control Rail Crafting",
                                List.of(() -> new ItemStack(Items.POWERED_RAIL, 2), () -> new ItemStack(Items.REDSTONE_TORCH)),
                                List.of(() -> new ItemStack(MiningTools.CONTROL_RAIL_ITEM.get(), 4)),
                                "Crafts 4 directional one-way booster rails.")
                )
        ));

        register(new GuideEntry(
                "braking_rail",
                "Braking & Holding Rail",
                "Station Decelerator • Soft Parking Stop",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.BRAKING_RAIL_ITEM.get()),
                "The Braking Rail gently decelerates high-speed trains to a safe, controlled stop.\n\n" +
                "• Smooth Deceleration: Eliminates abrupt cart rebounding and violent collisions when entering terminal stations.\n" +
                "• Holding Brake: When unpowered, holds parked trains firmly in place. When powered by redstone, releases the brake and propels the train forward.",
                "Place a Braking Rail directly in front of station platforms for picturesque passenger stops!",
                null,
                null,
                List.of(
                        new GuideRecipe("Braking Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 2), () -> new ItemStack(Items.IRON_INGOT), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.BRAKING_RAIL_ITEM.get(), 4)),
                                "Crafts 4 braking & holding decelerator rails.")
                )
        ));

        register(new GuideEntry(
                "horn_rail",
                "Whistle & Horn Rail",
                "Acoustic Grade Crossing Track",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.HORN_RAIL_ITEM.get()),
                "The Horn Rail triggers the horn or steam whistle of any locomotive that rolls across it automatically.\n\n" +
                "• Grade Crossings: Warns nearby pedestrians and vehicles of approaching express trains.\n" +
                "• Tunnel Entrances: Atmospheric acoustic echo when entering deep subterranean bores.",
                "Place 20 blocks in front of road crossings and bridges to enjoy realistic train whistle alerts!",
                null,
                null,
                List.of(
                        new GuideRecipe("Horn Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 2), () -> new ItemStack(Items.NOTE_BLOCK)),
                                List.of(() -> new ItemStack(MiningTools.HORN_RAIL_ITEM.get(), 4)),
                                "Crafts 4 acoustic horn & whistle rails.")
                )
        ));

        register(new GuideEntry(
                "electric_rail",
                "Third-Rail Electric Track",
                "Continuous High-Voltage Power Track",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.ELECTRIC_RAIL_ITEM.get()),
                "The Electric Rail is a high-conductivity track equipped with energized third-rail contact bars.\n\n" +
                "• Direct Power: Feeds electrical current straight into the pickup shoes of Electric Locomotives.\n" +
                "• Grid Connected: Energize from any adjacent power cable, capacitor bank, or Generator Cart.\n" +
                "• Maintenance Free: Never burns fuel items or emits soot particles.",
                "Electrifying your main passenger line allows trains to run non-stop 24/7 without refueling stops!",
                null,
                null,
                List.of(
                        new GuideRecipe("Electric Rail Crafting",
                                List.of(() -> new ItemStack(Items.RAIL, 4), () -> new ItemStack(Items.COPPER_INGOT), () -> new ItemStack(Items.REDSTONE)),
                                List.of(() -> new ItemStack(MiningTools.ELECTRIC_RAIL_ITEM.get(), 6)),
                                "Crafts 6 energized high-conductivity electric rails.")
                )
        ));

        register(new GuideEntry(
                "buffer_stop",
                "Buffer Stop (Stop Block)",
                "Terminal Track End Barrier • Kinetic Buffer",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.BUFFER_STOP_ITEM.get()),
                "The Buffer Stop is an immovable heavy-steel barrier installed at the end of dead-end sidings and spur lines.\n\n" +
                "• Impact Absorption: Safely halts runaway trains that overshoot station platforms or sidings, preventing them from derailing off cliffs.\n" +
                "• Industrial Aesthetics: Features authentic railroad end-of-line signage and safety striping.",
                "Install at every dead-end track terminus and siding end for absolute safety!",
                null,
                null,
                List.of(
                        new GuideRecipe("Buffer Stop Crafting",
                                List.of(() -> new ItemStack(Items.IRON_INGOT, 5), () -> new ItemStack(Blocks.OAK_LOG), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_BARS)),
                                List.of(() -> new ItemStack(MiningTools.BUFFER_STOP_ITEM.get(), 2)),
                                "Heavy structural steel terminal buffer stop.")
                )
        ));

        register(new GuideEntry(
                "track_switch",
                "Track Switch Stand",
                "Mechanical Switch Lever • Multi-Criteria Routing",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.TRACK_SWITCH_ITEM.get()),
                "The Track Switch Stand is an animated mechanical switch mechanism that controls adjacent Turnout and Wye rails.\n\n" +
                "• Manual & Redstone: Right-click to manually throw the switch points, or toggle via redstone wire.\n" +
                "• Multi-Criteria Automated Routing: Right-click to configure automated routing rules based on Train Name, Lock Delay (seconds), Travel Direction (N/S/E/W), Cargo Status (Loaded/Empty), and Fuel/Battery Level (<25%, <50%).\n" +
                "• Route Lock Duration: Specify how long (in seconds) the track stays locked to that position when a route activates, preventing conflicting trains from throwing the switch under passing railcars.\n" +
                "• Wireless Radio: Sneak-right-click with a Signal Tuner to link and throw the switch wirelessly from inside your locomotive cab!",
                "Configure a rule: 'If Train is Empty -> Divert to Loading Bay with 10s Lock'. The switch will route empty cars and stay locked until clear!",
                null,
                null,
                List.of(
                        new GuideRecipe("Track Switch Stand Crafting",
                                List.of(() -> new ItemStack(Items.REDSTONE_TORCH), () -> new ItemStack(Items.LEVER), () -> new ItemStack(Items.IRON_INGOT, 3)),
                                List.of(() -> new ItemStack(MiningTools.TRACK_SWITCH_ITEM.get(), 2)),
                                "Animated railway switch stand with target banner.")
                )
        ));

        register(new GuideEntry(
                "railway_signal",
                "Railway Block Signal",
                "3-Aspect Optical Signal • Block Interlocking",
                GuideCategory.TRACKS,
                () -> new ItemStack(MiningTools.RAILWAY_SIGNAL_ITEM.get()),
                "The Railway Block Signal prevents disastrous train collisions by dividing lines into protected track blocks.\n\n" +
                "• 3 Aspects: Green = Block Clear (Proceed at full speed), Amber = Caution (Next block occupied), Red = Stop (Block Occupied).\n" +
                "• Automatic Train Detection: Senses rolling stock occupying the monitored block and displays the appropriate signal aspect.\n" +
                "• Cab Radio: Broadcasts current signal aspect to locomotive cabs equipped with Signal Tuners.",
                "Place signals at block intervals to safely run multiple express trains along a single mainline track!",
                null,
                null,
                List.of(
                        new GuideRecipe("Railway Block Signal Crafting",
                                List.of(() -> new ItemStack(Items.IRON_INGOT, 3), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "green_dye")))),
                                List.of(() -> new ItemStack(MiningTools.RAILWAY_SIGNAL_ITEM.get(), 2)),
                                "Multi-aspect optical block signal with sun hood.")
                )
        ));

        // =========================================================================
        // CATEGORY: LOGISTICS & STATIONS
        // =========================================================================
        register(new GuideEntry(
                "cargo_loader",
                "Cargo Loader Station",
                "Automated Item Importer • Consist Filtering",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.CARGO_LOADER_ITEM.get()),
                "The Cargo Loader Station transfers items rapidly from connected chests into parked Cargo Minecarts.\n\n" +
                "• High Throughput: Moves stacks of items in rapid succession.\n" +
                "• Filtering: Supports item whitelist/blacklist and target cart name criteria.\n" +
                "• Departure Conditions: Can hold trains until fully loaded, then emit redstone to dispatch the locomotive.",
                "Set the loader to 'Wait Until Full' to guarantee your freight trains never depart half-empty!",
                null,
                null,
                List.of(
                        new GuideRecipe("Cargo Loader Station Crafting",
                                List.of(() -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.CHEST), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 6)),
                                List.of(() -> new ItemStack(MiningTools.CARGO_LOADER_ITEM.get())),
                                "High-speed automated freight loading station.")
                )
        ));

        register(new GuideEntry(
                "cargo_unloader",
                "Cargo Unloader Station",
                "Automated Item Exporter • Bulk Warehouse Depot",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.CARGO_UNLOADER_ITEM.get()),
                "The Cargo Unloader Station extracts items from arriving Cargo Minecarts into connected warehouse storage.\n\n" +
                "• Rapid Emptying: Empties 54-slot wagons in seconds.\n" +
                "• Smart Release: Emits a redstone pulse when the wagon is completely empty to release the train onto its return journey.",
                "Pair with a Braking Rail to hold the train securely while unloading, then release upon completion!",
                null,
                null,
                List.of(
                        new GuideRecipe("Cargo Unloader Station Crafting",
                                List.of(() -> new ItemStack(Items.CHEST), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 6)),
                                List.of(() -> new ItemStack(MiningTools.CARGO_UNLOADER_ITEM.get())),
                                "Automated freight unloading terminal.")
                )
        ));

        register(new GuideEntry(
                "fluid_loader",
                "Fluid Loader Station",
                "Overhead Tank Car Filler • 10,000 mB Buffer",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.FLUID_LOADER_ITEM.get()),
                "The Fluid Loader Station connects pipe networks to an overhead gantry arm to fill parked Liquid Minecarts.\n\n" +
                "• Universal Transfer: Compatible with all Minecraft and modded fluids (Oil, Fuel, Lava, Water, Chemicals).\n" +
                "• Internal Tank: Features an internal 10,000 mB fluid buffer to maintain constant flow without pipeline backpressure.",
                "Connect fluid pipes directly from your oil refinery outputs to supply departing distribution tankers!",
                null,
                null,
                List.of(
                        new GuideRecipe("Fluid Loader Station Crafting",
                                List.of(() -> new ItemStack(Items.PISTON), () -> new ItemStack(Items.COPPER_INGOT, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.FLUID_LOADER_ITEM.get())),
                                "Overhead articulated fluid loader gantry.")
                )
        ));

        register(new GuideEntry(
                "fluid_unloader",
                "Fluid Unloader Station",
                "Overhead Tank Car Drainer • Pipeline Depot",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.FLUID_UNLOADER_ITEM.get()),
                "The Fluid Unloader Station rapidly drains parked Liquid Minecarts into underground pipe manifolds.\n\n" +
                "• High Discharge Rate: Empties a 16,000 mB tank car in moments.\n" +
                "• Auto-Dispatch: Signals the train to depart as soon as its tanks are dry.",
                "Install at your processing facility to receive raw crude oil shuttles from distant drilling operations!",
                null,
                null,
                List.of(
                        new GuideRecipe("Fluid Unloader Station Crafting",
                                List.of(() -> new ItemStack(Items.BUCKET), () -> new ItemStack(Items.COPPER_INGOT, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.FLUID_UNLOADER_ITEM.get())),
                                "Heavy suction fluid drainage unloader station.")
                )
        ));

        register(new GuideEntry(
                "coal_fueler",
                "Locomotive Coaling Station",
                "Automated Steam Tender Refueler",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.COAL_FUELER_ITEM.get()),
                "The Locomotive Coaling Station automatically detects stopped steam locomotives and tops up their fuel bunkers.\n\n" +
                "• Hands-Free Refueling: Keeps steam locomotives continuously supplied without manual inventory management.\n" +
                "• Fuel Acceptance: Compatible with Coal, Charcoal, Coke, and all burnable fuels.",
                "Build coaling stations along main passenger corridors for quick refuel stops!",
                null,
                null,
                List.of(
                        new GuideRecipe("Coaling Station Crafting",
                                List.of(() -> new ItemStack(Items.BLAST_FURNACE), () -> new ItemStack(Items.COAL_BLOCK, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.COAL_FUELER_ITEM.get())),
                                "Automated overhead locomotive coaling tower.")
                )
        ));

        register(new GuideEntry(
                "diesel_fueler",
                "Diesel Fueling Station",
                "Diesel Pump Depot • Automatic Nozzle",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.DIESEL_FUELER_ITEM.get()),
                "The Diesel Fueling Station connects pipeline diesel supplies directly to parked Diesel Locomotives.\n\n" +
                "• Pressurized Delivery: High-pressure pumping tops off internal engine tanks in seconds.\n" +
                "• Full Cut-Off: Shuts off automatically when the locomotive's tank reaches 100% capacity.",
                "Connect directly to your diesel fuel tanks for zero-maintenance fleet refueling!",
                null,
                null,
                List.of(
                        new GuideRecipe("Diesel Fueling Station Crafting",
                                List.of(() -> new ItemStack(Items.BUCKET), () -> new ItemStack(Items.GOLD_INGOT, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.DIESEL_FUELER_ITEM.get())),
                                "Industrial trackside diesel fuel dispenser pump.")
                )
        ));

        register(new GuideEntry(
                "entity_loader",
                "Entity Boarding Station",
                "Automated Stock Yard Loader",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.ENTITY_LOADER_ITEM.get()),
                "The Entity Boarding Station draws nearby animals and villagers into parked Livestock Carts.\n\n" +
                "• No Leash Required: Smoothly guides mobs into open stock cars without combat or player leads.\n" +
                "• Configurable Area: Scans adjacent corral holding pens.",
                "Automate livestock distribution from breeding farms directly onto outbound transport trains!",
                null,
                null,
                List.of(
                        new GuideRecipe("Entity Boarding Station Crafting",
                                List.of(() -> new ItemStack(Items.LEAD), () -> new ItemStack(Items.COPPER_INGOT, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.ENTITY_LOADER_ITEM.get())),
                                "Stockyard animal boarding station.")
                )
        ));

        register(new GuideEntry(
                "entity_unloader",
                "Entity Disembarking Station",
                "Stock Yard Unloader • Gentle Release",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.ENTITY_UNLOADER_ITEM.get()),
                "The Entity Disembarking Station safely releases animals and passengers from arriving Livestock Carts.\n\n" +
                "• Gentle Ejection: Unloads passengers onto adjacent platforms or pastures without inflicting damage.",
                "Set water canals at the exit chute to guide unloaded animals straight into their barns!",
                null,
                null,
                List.of(
                        new GuideRecipe("Entity Disembarking Station Crafting",
                                List.of(() -> new ItemStack(Items.ACTIVATOR_RAIL), () -> new ItemStack(Items.COPPER_INGOT, 2), () -> new ItemStack(Items.HOPPER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.ENTITY_UNLOADER_ITEM.get())),
                                "Automated stock disembarkation terminal.")
                )
        ));

        register(new GuideEntry(
                "chunk_loader",
                "Railway Chunk Loader",
                "Continuous Long-Distance Transit",
                GuideCategory.LOGISTICS,
                () -> new ItemStack(MiningTools.CHUNK_LOADER_ITEM.get()),
                "The Railway Chunk Loader keeps surrounding world chunks loaded in memory so trains continue moving across infinite distances.\n\n" +
                "• Offline Operation: Trains run on time even when players are elsewhere in the world or offline.\n" +
                "• Configurable Radius: Right-click to adjust active chunk loading radius (1x1 up to 3x3 chunks).",
                "Place chunk loaders at 6-chunk intervals along transcontinental rail lines for seamless automated shipping!",
                null,
                null,
                List.of(
                        new GuideRecipe("Railway Chunk Loader Crafting",
                                List.of(() -> new ItemStack(Items.OBSIDIAN), () -> new ItemStack(Items.GOLD_INGOT, 2), () -> new ItemStack(Items.ENDER_PEARL), () -> new ItemStack(Items.REDSTONE_BLOCK), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.CHUNK_LOADER_ITEM.get())),
                                "Dimensional anchor for continuous rail transit.")
                )
        ));

        // =========================================================================
        // CATEGORY: HEAVY MACHINERY
        // =========================================================================
        register(new GuideEntry(
                "tunnel_bore",
                "Industrial Tunnel Bore",
                "3x3 Subterranean Excavator • Ballast & Track Layer",
                GuideCategory.HEAVY_MACHINERY,
                () -> new ItemStack(MiningTools.TUNNEL_BORE_ITEM.get()),
                "The Industrial Tunnel Bore is a colossal mining machine that excavates clean 3x3 tunnels straight through solid mountains and subterranean bedrock.\n\n" +
                "• Automated Excavation: Breaks stone, dirt, ores, and gravel ahead of its rotating cutter head.\n" +
                "• Bridge Building: Automatically places ballast blocks (gravel, cobblestone) beneath itself to bridge subterranean caverns and lava lakes.\n" +
                "• Rail Laying: Lays continuous track behind itself as it advances, creating finished railway tunnels on the fly.\n" +
                "• Fueling: Powered by coal, lava buckets, or high-grade solid fuels.",
                "Tow multiple Cargo Minecarts behind the Tunnel Bore to collect thousands of mined ores without stopping!",
                null,
                null,
                List.of(
                        new GuideRecipe("Tunnel Bore Crafting",
                                List.of(() -> new ItemStack(Items.IRON_PICKAXE, 2), () -> new ItemStack(Items.IRON_BLOCK), () -> new ItemStack(Items.FURNACE_MINECART), () -> new ItemStack(Items.CHEST_MINECART), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.TUNNEL_BORE_ITEM.get())),
                                "Heavy subterranean drill head and automated railway construction machine.")
                )
        ));

        register(new GuideEntry(
                "tunnel_paver",
                "Tunnel Paver",
                "3x3 Tunnel Wall & Ceiling Arch Liner",
                GuideCategory.HEAVY_MACHINERY,
                () -> new ItemStack(MiningTools.TUNNEL_PAVER_ITEM.get()),
                "The Tunnel Paver is a full-bore construction machine engineered to travel behind a Tunnel Bore and line the surrounding walls and ceiling without disturbing the floor or track beneath itself.\n\n" +
                "• 9-Block Arch Lining: Replaces the 3 blocks on its left wall, the 3 blocks in a row above its roof, and the 3 blocks on its right wall in each step.\n" +
                "• 9-Slot Arch Pattern Programming: Configure individual filter slots arranged in a U-arch (Left Wall Bottom/Mid/Top, Ceiling Left/Center/Right, Right Wall Top/Mid/Bottom) to build custom architectural tunnel linings and lighting strips.\n" +
                "• Preserves Roadbed: Leaves the 3 blocks beneath itself untouched so it pairs seamlessly with the Under Track Paver.",
                "Leave all 9 pattern slots empty to automatically line all walls and the ceiling with any solid block in its 27-slot bulk material bay!",
                null,
                null,
                List.of(
                        new GuideRecipe("Tunnel Paver Crafting",
                                List.of(() -> new ItemStack(Items.STONE_BRICKS, 2), () -> new ItemStack(Items.IRON_BLOCK), () -> new ItemStack(Items.BLAST_FURNACE), () -> new ItemStack(Items.CHEST_MINECART), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.TUNNEL_PAVER_ITEM.get())),
                                "Heavy-duty 3x3 tunnel wall and ceiling arch paving machine.")
                )
        ));

        register(new GuideEntry(
                "track_paver",
                "Under Track Paver",
                "Automated Railway & Roadbed Constructor",
                GuideCategory.HEAVY_MACHINERY,
                () -> new ItemStack(MiningTools.TRACK_PAVER_ITEM.get()),
                "The Under Track Paver grades land, installs uniform ballast roadbeds beneath tracks, and paves structured ground.\n\n" +
                "• 3-Tile Pattern Programming: Configure Left, Center (under track), and Right filter slots to lay custom roadbed patterns (e.g. curb-ballast-curb or hazard stripes).\n" +
                "• Rail-Cart Operation: Snaps directly to rails and follows the track while paving underneath.\n" +
                "• High Productivity: Construct miles of transcontinental ballast roadbed in minutes.",
                "Tether an Aggregate Hopper Cart behind the paver to keep its ballast hopper filled indefinitely!",
                null,
                null,
                List.of(
                        new GuideRecipe("Under Track Paver Crafting",
                                List.of(() -> new ItemStack(Items.IRON_SHOVEL, 2), () -> new ItemStack(Items.IRON_BLOCK), () -> new ItemStack(Items.BLAST_FURNACE), () -> new ItemStack(Items.CHEST_MINECART), () -> new ItemStack(Items.IRON_INGOT, 4)),
                                List.of(() -> new ItemStack(MiningTools.TRACK_PAVER_ITEM.get())),
                                "Automated roadbed and ballast paving machine.")
                        )
        ));

        register(new GuideEntry(
                "track_layer_cart",
                "Track Layer Cart",
                "Lightweight Track Laying Vehicle",
                GuideCategory.HEAVY_MACHINERY,
                () -> new ItemStack(MiningTools.TRACK_LAYER_CART_ITEM.get()),
                "The Track Layer Cart installs tracks automatically from its inventory as it is pushed along the roadbed.\n\n" +
                "• High-Speed Laying: Places rails ahead at the full speed of the pushing locomotive.\n" +
                "• Compact Design: Perfect for fast rail extensions and survey explorations.",
                "Push the Track Layer Cart ahead of your locomotive on surveyed roadbeds to lay mainline tracks at cruising speed!",
                null,
                null,
                List.of(
                        new GuideRecipe("Track Layer Cart Crafting",
                                List.of(() -> new ItemStack(Items.HOPPER_MINECART), () -> new ItemStack(Items.RAIL, 4), () -> new ItemStack(Items.IRON_INGOT, 2)),
                                List.of(() -> new ItemStack(MiningTools.TRACK_LAYER_CART_ITEM.get())),
                                "Rapid rail-laying construction cart.")
                )
        ));

        // =========================================================================
        // CATEGORY: TOOLS & SIGNALS
        // =========================================================================
        register(new GuideEntry(
                "crowbar",
                "Railroader's Crowbar",
                "Essential Railway Multitool • Link / Rename / Dismantle",
                GuideCategory.TOOLS,
                () -> new ItemStack(MiningTools.CROWBAR_ITEM.get()),
                "The Railroader's Crowbar is the quintessential tool for every engineer and conductor.\n\n" +
                "• Consist Coupling: Right-click cart A, then right-click cart B to couple them into a train. Right-click an existing link to decouple.\n" +
                "• Cart Renaming: Sneak-right-click any cart to open the Cart Rename GUI and assign custom names.\n" +
                "• Track Rotation: Right-click rails to rotate their direction or toggle turnout switch branches.\n" +
                "• Instant Dismantle: Sneak-left-click any cart to instantly collect it into your inventory.",
                "Always carry a Crowbar in your hotbar for rapid train coupling, routing, and rail adjustments!",
                null,
                null,
                List.of(
                        new GuideRecipe("Railroader's Crowbar Crafting",
                                List.of(() -> new ItemStack(Items.IRON_INGOT, 3), () -> new ItemStack(Items.IRON_NUGGET)),
                                List.of(() -> new ItemStack(MiningTools.CROWBAR_ITEM.get())),
                                "Forged steel crowbar crafted in a diagonal pattern.")
                )
        ));

        register(new GuideEntry(
                "signal_tuner",
                "Signal Tuner Radio",
                "Wireless Switch & Cab Signal Transceiver",
                GuideCategory.TOOLS,
                () -> new ItemStack(MiningTools.SIGNAL_TUNER_ITEM.get()),
                "The Signal Tuner Radio establishes wireless links between Track Switches, Railway Signals, and locomotive cabs.\n\n" +
                "• Linking Switches: Sneak-right-click a Track Switch stand to tune into its radio frequency.\n" +
                "• Cab Control: While seated in any locomotive, right-click with the tuner to throw linked switches ahead without dismounting!\n" +
                "• Signal Monitoring: Displays the status and aspect of approaching block signals directly on your HUD.",
                "Tune to multiple switches in your main yard to throw complex route alignments directly from your cab seat!",
                null,
                null,
                List.of(
                        new GuideRecipe("Signal Tuner Radio Crafting",
                                List.of(() -> new ItemStack(Items.REDSTONE_TORCH), () -> new ItemStack(Items.COPPER_INGOT), () -> new ItemStack(Items.IRON_INGOT, 2)),
                                List.of(() -> new ItemStack(MiningTools.SIGNAL_TUNER_ITEM.get())),
                                "Handheld wireless radio transceiver for railroad communication.")
                )
        ));

        register(new GuideEntry(
                "surveyors_charge",
                "Surveyor's Seismic Charge",
                "Subterranean Geological Borehole & Acoustic Scan",
                GuideCategory.TOOLS,
                () -> new ItemStack(MiningTools.SURVEYORS_CHARGE_ITEM.get()),
                "The Surveyor's Seismic Charge is a specialized explosive for rapid geological prospect surveying.\n\n" +
                "• Instant Borehole: Blasts a clean 1x1 vertical borehole downwards until reaching bedrock or impenetrable layers.\n" +
                "• Acoustic Resource Scan: Scans surrounding subterranean strata for valuable ores, gemstones, caverns, and deep liquid petroleum pockets.\n" +
                "• Geological Report: Automatically delivers a formatted geological report detailing all detected minerals and oil deposits.",
                "Trigger the charge with a Remote Detonator from a safe observation distance!",
                null,
                null,
                List.of(
                        new GuideRecipe("Surveyor's Seismic Charge Crafting",
                                List.of(() -> new ItemStack(Items.TNT), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.ARROW)),
                                List.of(() -> new ItemStack(MiningTools.SURVEYORS_CHARGE_ITEM.get())),
                                "Directional seismic acoustic charge.")
                )
        ));

        register(new GuideEntry(
                "remote_detonator",
                "Remote Detonator",
                "Wireless Blasting Controller",
                GuideCategory.TOOLS,
                () -> new ItemStack(MiningTools.REMOTE_DETONATOR.get()),
                "The Remote Detonator wirelessly triggers linked Surveyor's Charges and blasting caps from safe distances up to 64 blocks.\n\n" +
                "• Radio Frequency: Right-click a Surveyor's Charge to link, then right-click from afar to detonate.\n" +
                "• Chain Blasting: Can link multiple charges across a prospective mining claim for synchronized seismic surveying.",
                "Link multiple charges across an exploration grid to map vast underground ore fields in seconds!",
                null,
                null,
                List.of(
                        new GuideRecipe("Remote Detonator Crafting",
                                List.of(() -> new ItemStack(Items.LEVER), () -> new ItemStack(Items.REDSTONE), () -> new ItemStack(Items.IRON_INGOT)),
                                List.of(() -> new ItemStack(MiningTools.REMOTE_DETONATOR.get())),
                                "Coded radio transmitter blast plunger.")
                )
        ));

        register(new GuideEntry(
                "guide_book",
                "Railroader's Field Manual",
                "Complete Operational & Engineering Guide",
                GuideCategory.TOOLS,
                () -> new ItemStack(MiningTools.GUIDE_BOOK_ITEM.get()),
                "The Railroader's Field Manual is the definitive guide to all locomotives, rolling stock, track switchgear, and logistics systems.\n\n" +
                "• Instant Reference: Right-click anytime to browse topics by category or review crafting recipes and mechanical operations.\n" +
                "• Navigation: Press ESC while viewing an entry to jump back to the Table of Contents, or click '< Contents' at the top left.\n" +
                "• Universal Companion: An indispensable manual for beginners and veteran railroad superintendents alike.",
                "Keep this book in your inventory whenever laying out new rail lines and complex station yards!",
                null,
                null,
                List.of(
                        new GuideRecipe("Railroader's Field Manual Crafting",
                                List.of(() -> new ItemStack(Items.BOOK), () -> new ItemStack(Items.IRON_INGOT)),
                                List.of(() -> new ItemStack(MiningTools.GUIDE_BOOK_ITEM.get())),
                                "Heavy binder manual bound in industrial slate and brass corners.")
                )
        ));
    }

    private static void register(GuideEntry entry) {
        ENTRIES.add(entry);
        BY_ID.put(entry.getId(), entry);
    }

    public static List<GuideEntry> getAllEntries() {
        init();
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<GuideEntry> getEntriesByCategory(GuideCategory category) {
        init();
        if (category == GuideCategory.ALL) return getAllEntries();
        return ENTRIES.stream().filter(e -> e.getCategory() == category).toList();
    }

    public static GuideEntry getEntry(String id) {
        init();
        return BY_ID.get(id);
    }
}
