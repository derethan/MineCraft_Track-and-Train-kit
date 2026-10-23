package com.example.miningtools;

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
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
