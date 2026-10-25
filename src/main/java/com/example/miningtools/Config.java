package com.example.miningtools;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

// Configuration class using NeoForge's config APIs.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue LOCOMOTIVE_COAL_BURN_TICKS;
    public static final ModConfigSpec.DoubleValue LOCOMOTIVE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue LOCOMOTIVE_ACCELERATION;
    public static final ModConfigSpec.IntValue STEAM_LOCOMOTIVE_PULL_CAPACITY;

    public static final ModConfigSpec.DoubleValue DIESEL_LOCOMOTIVE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue DIESEL_LOCOMOTIVE_ACCELERATION;
    public static final ModConfigSpec.IntValue DIESEL_LOCOMOTIVE_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue DIESEL_LOCOMOTIVE_FUEL_PER_TICK;
    public static final ModConfigSpec.IntValue DIESEL_LOCOMOTIVE_PULL_CAPACITY;

    public static final ModConfigSpec.DoubleValue ELECTRIC_LOCOMOTIVE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue ELECTRIC_LOCOMOTIVE_ACCELERATION;
    public static final ModConfigSpec.IntValue ELECTRIC_LOCOMOTIVE_BATTERY_CAPACITY;
    public static final ModConfigSpec.IntValue ELECTRIC_LOCOMOTIVE_FE_PER_TICK;
    public static final ModConfigSpec.IntValue ELECTRIC_LOCOMOTIVE_PULL_CAPACITY;

    public static final ModConfigSpec.IntValue ELECTRIC_RAIL_CAPACITY;
    public static final ModConfigSpec.IntValue ELECTRIC_RAIL_TRANSFER_RATE;

    public static final ModConfigSpec.IntValue SURVEYOR_CHARGE_RADIUS;
    public static final ModConfigSpec.BooleanValue SURVEYOR_CHARGE_DROP_BOREHOLE_BLOCKS;
    public static final ModConfigSpec.DoubleValue REMOTE_DETONATOR_RANGE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SURVEYOR_CHARGE_IGNORED_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS;

    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_DRAG_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_BUMP_TRANSFER;
    public static final ModConfigSpec.DoubleValue ROLLING_STOCK_BUMP_DAMPING;

    public static final ModConfigSpec.IntValue GENERATOR_CART_ENERGY_RATE;
    public static final ModConfigSpec.IntValue GENERATOR_CART_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue GENERATOR_CART_WATER_CAPACITY;
    public static final ModConfigSpec.IntValue GENERATOR_CART_TRANSFER_RATE;
    public static final ModConfigSpec.IntValue GENERATOR_CART_BROADCAST_RADIUS;

    static {
        BUILDER.push("locomotive");

        LOCOMOTIVE_COAL_BURN_TICKS = BUILDER
                .comment("Duration in game ticks that each piece of coal or charcoal burns in the Basic Locomotive.",
                         "1600 ticks = 80 seconds (standard furnace burn time). Lower numbers burn faster; higher numbers burn slower.")
                .defineInRange("coalBurnTicks", 1600, 20, 72000);

        LOCOMOTIVE_MAX_SPEED = BUILDER
                .comment("Maximum travel speed of the Basic Locomotive at Speed Level 5 (blocks per tick). Vanilla max is 0.4.")
                .defineInRange("maxSpeed", 0.35D, 0.05D, 1.0D);

        LOCOMOTIVE_ACCELERATION = BUILDER
                .comment("Acceleration rate of the Basic Locomotive when starting up.")
                .defineInRange("acceleration", 0.04D, 0.005D, 0.2D);

        STEAM_LOCOMOTIVE_PULL_CAPACITY = BUILDER
                .comment("Number of unpowered train carts a Steam Locomotive can pull without drag speed penalty.")
                .defineInRange("pullCapacity", 5, 1, 50);

        BUILDER.pop();

        BUILDER.push("diesel_locomotive");

        DIESEL_LOCOMOTIVE_MAX_SPEED = BUILDER
                .comment("Maximum travel speed of the CN Diesel Locomotive at Speed Level 5 (blocks per tick). Default 0.70 is 2x the Steam Locomotive max speed of 0.35.")
                .defineInRange("maxSpeed", 0.70D, 0.1D, 3.0D);

        DIESEL_LOCOMOTIVE_ACCELERATION = BUILDER
                .comment("Acceleration rate of the Diesel Locomotive.")
                .defineInRange("acceleration", 0.05D, 0.005D, 0.25D);

        DIESEL_LOCOMOTIVE_TANK_CAPACITY = BUILDER
                .comment("Internal fuel tank capacity in mB (millibuckets) of the Diesel Locomotive. 16000 mB = 16 Buckets.")
                .defineInRange("tankCapacity", 16000, 1000, 128000);

        DIESEL_LOCOMOTIVE_FUEL_PER_TICK = BUILDER
                .comment("Fuel consumption in mB per tick at maximum speed (Speed Level 5).")
                .defineInRange("fuelPerTick", 1, 1, 100);

        DIESEL_LOCOMOTIVE_PULL_CAPACITY = BUILDER
                .comment("Number of unpowered train carts a Diesel Locomotive can pull without drag speed penalty.")
                .defineInRange("pullCapacity", 10, 1, 100);

        BUILDER.pop();

        BUILDER.push("electric_locomotive");

        ELECTRIC_LOCOMOTIVE_MAX_SPEED = BUILDER
                .comment("Maximum travel speed of the Electric Bullet Train at Speed Level 5 (blocks per tick). Default 1.75 is 5x the Steam Locomotive max speed of 0.35.")
                .defineInRange("maxSpeed", 1.75D, 0.2D, 5.0D);

        ELECTRIC_LOCOMOTIVE_ACCELERATION = BUILDER
                .comment("Acceleration rate of the Electric Bullet Train when starting up.")
                .defineInRange("acceleration", 0.06D, 0.01D, 0.3D);

        ELECTRIC_LOCOMOTIVE_BATTERY_CAPACITY = BUILDER
                .comment("Internal battery capacity in FE of the Electric Bullet Train.")
                .defineInRange("batteryCapacity", 100000, 1000, 10000000);

        ELECTRIC_LOCOMOTIVE_FE_PER_TICK = BUILDER
                .comment("Energy consumption in FE per tick at max speed (Level 5). Lower levels consume proportionately less.")
                .defineInRange("energyPerTick", 40, 1, 2000);

        ELECTRIC_LOCOMOTIVE_PULL_CAPACITY = BUILDER
                .comment("Number of unpowered train carts an Electric Locomotive can pull without drag speed penalty.")
                .defineInRange("pullCapacity", 10, 1, 100);

        BUILDER.pop();

        BUILDER.push("electric_rail");

        ELECTRIC_RAIL_CAPACITY = BUILDER
                .comment("Energy capacity in FE for each Electric Rail block.")
                .defineInRange("capacity", 10000, 500, 1000000);

        ELECTRIC_RAIL_TRANSFER_RATE = BUILDER
                .comment("Maximum FE per tick transferred into or between adjacent electric rails.")
                .defineInRange("transferRate", 2000, 100, 50000);

        BUILDER.pop();

        BUILDER.push("generator_cart");

        GENERATOR_CART_ENERGY_RATE = BUILDER
                .comment("Energy generated in FE per tick while the Generator Cart is actively burning coal with water.")
                .defineInRange("energyRate", 100, 10, 5000);

        GENERATOR_CART_ENERGY_CAPACITY = BUILDER
                .comment("Internal battery/energy buffer capacity in FE of the Generator Cart.")
                .defineInRange("energyCapacity", 100000, 1000, 10000000);

        GENERATOR_CART_WATER_CAPACITY = BUILDER
                .comment("Internal water tank capacity in mB (millibuckets) of the Generator Cart.")
                .defineInRange("waterCapacity", 8000, 1000, 64000);

        GENERATOR_CART_TRANSFER_RATE = BUILDER
                .comment("Maximum FE per tick transferred into adjacent energy blocks, cables, or connected electric locomotives.")
                .defineInRange("transferRate", 500, 10, 10000);

        GENERATOR_CART_BROADCAST_RADIUS = BUILDER
                .comment("Radius in blocks around the Generator Cart to wirelessly power nearby energy receivers/cables.")
                .defineInRange("broadcastRadius", 3, 1, 16);

        BUILDER.pop();

        BUILDER.push("surveyor_charge");

        SURVEYOR_CHARGE_RADIUS = BUILDER
                .comment("Horizontal detection radius in blocks for the column scanned from the Surveyor's Charge down to bedrock.")
                .defineInRange("radius", 10, 1, 32);

        SURVEYOR_CHARGE_DROP_BOREHOLE_BLOCKS = BUILDER
                .comment("Whether blocks blasted in the 1x1 vertical borehole are dropped as items (true) or destroyed (false).")
                .define("dropBoreholeBlocks", false);

        REMOTE_DETONATOR_RANGE = BUILDER
                .comment("Maximum range in blocks from which the Remote Detonator can trigger Surveyor's Charges.")
                .defineInRange("remoteDetonatorRange", 12.0D, 1.0D, 64.0D);

        SURVEYOR_CHARGE_IGNORED_BLOCKS = BUILDER
                .comment("List of block registry IDs to ignore in the Surveyor's Charge report.",
                         "Common terrain blocks are ignored by default so only minerals, ores, structures, and unusual blocks are listed.")
                .defineListAllowEmpty("ignoredBlocks",
                        () -> List.of(
                                "minecraft:air",
                                "minecraft:cave_air",
                                "minecraft:void_air",
                                "minecraft:stone",
                                "minecraft:cobblestone",
                                "minecraft:smooth_stone",
                                "minecraft:deepslate",
                                "minecraft:cobbled_deepslate",
                                "minecraft:dirt",
                                "minecraft:coarse_dirt",
                                "minecraft:grass_block",
                                "minecraft:podzol",
                                "minecraft:sand",
                                "minecraft:red_sand",
                                "minecraft:sandstone",
                                "minecraft:red_sandstone",
                                "minecraft:gravel",
                                "minecraft:water",
                                "minecraft:lava",
                                "minecraft:bedrock",
                                "minecraft:netherrack",
                                "minecraft:basalt",
                                "minecraft:blackstone",
                                "minecraft:andesite",
                                "minecraft:diorite",
                                "minecraft:granite",
                                "minecraft:tuff",
                                "minecraft:calcite",
                                "minecraft:dripstone_block",
                                "miningtools:surveyors_charge"
                        ),
                        () -> "",
                        obj -> obj instanceof String);

        SURVEYOR_CHARGE_EXTRA_REPORT_BLOCKS = BUILDER
                .comment("List of extra block registry IDs (e.g. from other mods like modid:custom_ore) to explicitly include in the report even if they would otherwise be ignored.")
                .defineListAllowEmpty("extraReportBlocks",
                        List::of,
                        () -> "",
                        obj -> obj instanceof String);

        BUILDER.pop();

        BUILDER.push("rolling_stock");

        ROLLING_STOCK_DRAG_COEFFICIENT = BUILDER
                .comment("Drag/friction coefficient applied to rolling stock carts per tick when unpowered or rolling free.",
                         "0.88 = 12% speed loss per tick. Lower numbers stop carts faster and eliminate jitter; 0.98 is vanilla.")
                .defineInRange("dragCoefficient", 0.88D, 0.50D, 0.999D);

        ROLLING_STOCK_BUMP_TRANSFER = BUILDER
                .comment("Ratio of relative momentum transferred between uncoupled carts when bumping on tracks.",
                         "Default 0.25 provides gentle shunting bumps without setting carts flying down the track.")
                .defineInRange("bumpTransferRatio", 0.25D, 0.05D, 1.0D);

        ROLLING_STOCK_BUMP_DAMPING = BUILDER
                .comment("Kinetic energy retention factor applied to colliding carts after a bump.",
                         "Default 0.30 dampens impact energy so both carts come to a stop shortly after bumping.")
                .defineInRange("bumpDamping", 0.30D, 0.10D, 1.0D);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
