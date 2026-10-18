package com.example.miningtools;

import net.neoforged.neoforge.common.ModConfigSpec;

// Configuration class using NeoForge's config APIs.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue LOCOMOTIVE_COAL_BURN_TICKS;
    public static final ModConfigSpec.DoubleValue LOCOMOTIVE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue LOCOMOTIVE_ACCELERATION;

    static {
        BUILDER.push("locomotive");

        LOCOMOTIVE_COAL_BURN_TICKS = BUILDER
                .comment("Duration in game ticks that each piece of coal or charcoal burns in the Basic Locomotive.",
                         "1600 ticks = 80 seconds (standard furnace burn time). Lower numbers burn faster; higher numbers burn slower.")
                .defineInRange("coalBurnTicks", 1600, 20, 72000);

        LOCOMOTIVE_MAX_SPEED = BUILDER
                .comment("Maximum travel speed of the Basic Locomotive on tracks (blocks per tick). Vanilla max is 0.4.")
                .defineInRange("maxSpeed", 0.175D, 0.05D, 1.0D);

        LOCOMOTIVE_ACCELERATION = BUILDER
                .comment("Acceleration rate of the Basic Locomotive when starting up.")
                .defineInRange("acceleration", 0.02D, 0.005D, 0.2D);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
