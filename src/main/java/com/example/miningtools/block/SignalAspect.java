package com.example.miningtools.block;

import net.minecraft.util.StringRepresentable;

public enum SignalAspect implements StringRepresentable {
    RED("red", 0),
    YELLOW("yellow", 1),
    GREEN("green", 2),
    SHUNT("shunt", 3);

    private final String name;
    private final int index;

    SignalAspect(String name, int index) {
        this.name = name;
        this.index = index;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getIndex() {
        return this.index;
    }

    public static SignalAspect fromIndex(int index) {
        return switch (index) {
            case 1 -> YELLOW;
            case 2 -> GREEN;
            case 3 -> SHUNT;
            default -> RED;
        };
    }
}
