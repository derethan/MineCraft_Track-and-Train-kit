package com.example.miningtools.guide;

public enum GuideCategory {
    ALL("All Contents", 0xFFE2E8F0),
    LOCOMOTIVES("Locomotives", 0xFFEF4444),
    ROLLING_STOCK("Rolling Stock", 0xFFF59E0B),
    TRACKS("Tracks & Switches", 0xFF38BDF8),
    LOGISTICS("Stations & Logistics", 0xFF10B981),
    HEAVY_MACHINERY("Heavy Machinery", 0xFFF97316),
    TOOLS("Tools & Signals", 0xFFA855F7);

    private final String displayName;
    private final int color;

    GuideCategory(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
