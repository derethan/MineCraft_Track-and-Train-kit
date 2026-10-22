package com.example.miningtools.client.gui;

import java.util.Locale;

import com.example.miningtools.Config;
import com.example.miningtools.entity.BasicLocomotiveEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@OnlyIn(Dist.CLIENT)
public class SpeedometerOverlay {
    private static final int HISTORY_SIZE = 6;
    private static final Vec3[] POS_HISTORY = new Vec3[HISTORY_SIZE];
    private static int historyIndex = 0;
    private static int historyCount = 0;
    private static int lastVehicleId = -1;
    private static float smoothedSpeedBps = 0.0F;

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        Entity vehicle = mc.player.getRootVehicle();
        if (vehicle == mc.player) {
            vehicle = mc.player.getVehicle();
        }
        if (vehicle == null || vehicle.isRemoved()) {
            reset();
            return;
        }

        Vec3 currentPos = new Vec3(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        if (vehicle.getId() != lastVehicleId) {
            lastVehicleId = vehicle.getId();
            for (int i = 0; i < HISTORY_SIZE; i++) {
                POS_HISTORY[i] = currentPos;
            }
            historyIndex = 0;
            historyCount = 1;
            smoothedSpeedBps = 0.0F;
            return;
        }

        int oldestIdx = historyCount < HISTORY_SIZE ? 0 : historyIndex;
        Vec3 oldestPos = POS_HISTORY[oldestIdx];
        POS_HISTORY[historyIndex] = currentPos;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) {
            historyCount++;
        }

        int ticksElapsed = Math.max(1, historyCount - 1);
        double dx = currentPos.x - oldestPos.x;
        double dy = currentPos.y - oldestPos.y;
        double dz = currentPos.z - oldestPos.z;

        // Include vertical component when on rails or flying; use horizontal on ground/water mounts so jumping in place doesn't spike speed
        boolean includeVertical = (vehicle instanceof AbstractMinecart) || !vehicle.onGround();
        double dist = includeVertical ? Math.sqrt(dx * dx + dy * dy + dz * dz) : Math.hypot(dx, dz);

        float windowBps = (float) ((dist / ticksElapsed) * 20.0);

        // Also check immediate delta movement for client-controlled mounts
        Vec3 delta = vehicle.getDeltaMovement();
        float deltaBps = (float) ((includeVertical ? delta.length() : delta.horizontalDistance()) * 20.0);

        float targetBps = Math.max(windowBps, deltaBps * 0.5F);
        if (windowBps < 0.05F && deltaBps < 0.05F) {
            targetBps = 0.0F;
        }

        BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
        if (loco != null && !loco.isActive() && windowBps < 0.08F) {
            targetBps = 0.0F;
        }

        if (targetBps == 0.0F && smoothedSpeedBps < 0.15F) {
            smoothedSpeedBps = 0.0F;
        } else {
            smoothedSpeedBps = Mth.lerp(0.35F, smoothedSpeedBps, targetBps);
        }
    }

    private static void reset() {
        lastVehicleId = -1;
        historyCount = 0;
        historyIndex = 0;
        smoothedSpeedBps = 0.0F;
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Entity vehicle = mc.player.getRootVehicle();
        if (vehicle == mc.player) {
            vehicle = mc.player.getVehicle();
        }
        if (vehicle == null || vehicle.isRemoved()) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = mc.font;

        BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
        boolean hasLoco = loco != null;

        int panelW = hasLoco ? 112 : 98;
        int panelH = hasLoco ? 48 : 35;
        int x = 6;
        int y = graphics.guiHeight() - panelH - 6;

        // Outer shadow & panel background
        graphics.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, 0xCC101216);
        graphics.fill(x, y, x + panelW, y + panelH, 0xD81A1D24);
        graphics.outline(x, y, panelW, panelH, 0xFF3A4150);

        // Top accent line
        int accentColor = hasLoco
                ? (loco.isActive() ? 0xFF2ECC71 : 0xFFE67E22)
                : 0xFF3498DB;
        graphics.fill(x + 1, y + 1, x + panelW - 1, y + 2, accentColor);

        // Row 1: Vehicle name & Compass heading
        String heading = getCompassDirection(hasLoco ? loco.getFacing().toYRot() : vehicle.getYRot());
        if (hasLoco && loco.isHeadlightEnabled()) {
            heading = "* " + heading;
        }
        int headingW = font.width(heading);

        String title = (hasLoco && !loco.getTrainName().isBlank())
                ? loco.getTrainName()
                : vehicle.getDisplayName().getString();
        int maxTitleW = panelW - headingW - 10;
        if (font.width(title) > maxTitleW) {
            title = font.plainSubstrByWidth(title, Math.max(12, maxTitleW - 6)) + "..";
        }

        graphics.text(font, title, x + 4, y + 4, 0xFFD0D7DE, true);
        int headingColor = (hasLoco && loco.isHeadlightEnabled()) ? 0xFFFFD166 : 0xFF8B949E;
        graphics.text(font, heading, x + panelW - headingW - 4, y + 4, headingColor, true);

        // Row 2: Speed in m/s (blocks/s) and km/h
        float bps = Math.max(0.0F, smoothedSpeedBps);
        float kmh = bps * 3.6F;

        String bpsText = String.format(Locale.ROOT, "%.1f m/s", bps);
        String kmhText = String.format(Locale.ROOT, "%.1f km/h", kmh);

        graphics.text(font, bpsText, x + 4, y + 15, 0xFFFFFFFF, true);
        int kmhW = font.width(kmhText);
        graphics.text(font, kmhText, x + panelW - kmhW - 4, y + 15, 0xFF79C0FF, true);

        // Row 3: Mini speed bar
        int barX = x + 4;
        int barY = y + 26;
        int barW = panelW - 8;
        int barH = 5;

        float maxRefBps = getMaxReferenceSpeedBps(vehicle, loco);
        float speedRatio = Mth.clamp(bps / Math.max(1.0F, maxRefBps), 0.0F, 1.0F);
        int fillW = Math.round(barW * speedRatio);

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0D1117);
        graphics.outline(barX, barY, barW, barH, 0xFF30363D);

        if (fillW > 0) {
            int barColor = getSpeedBarColor(speedRatio);
            graphics.fill(barX + 1, barY + 1, barX + Math.min(barW - 1, fillW), barY + barH - 1, barColor);
        }

        // Quarter tick marks on the speed bar
        for (int i = 1; i <= 3; i++) {
            int tickX = barX + (barW * i) / 4;
            graphics.fill(tickX, barY + 1, tickX + 1, barY + barH - 1, 0x55FFFFFF);
        }

        // If connected to a locomotive, draw target gear marker on the speed bar & Row 4 (Gear + Fuel)
        if (hasLoco) {
            int gear = loco.getSpeedLevel();
            float gearRatio = switch (gear) {
                case 1 -> 0.15F;
                case 2 -> 0.30F;
                case 3 -> 0.50F;
                case 4 -> 0.75F;
                default -> 1.00F;
            };
            int markerX = barX + Math.clamp(Math.round(barW * gearRatio), 1, barW - 2);
            graphics.fill(markerX, barY, markerX + 1, barY + barH, 0xFFFFFFFF);

            // Row 4: Gear status on left, Fuel gauge on right
            int row4Y = y + 35;
            String gearLabel = loco.isActive()
                    ? ("G" + gear + " " + BasicLocomotiveEntity.getSpeedName(gear))
                    : ("G" + gear + " [STOP]");
            int gearColor = loco.isActive() ? 0xFF56D364 : 0xFFF85149;
            graphics.text(font, gearLabel, x + 4, row4Y, gearColor, true);

            // Fuel indicator on the right
            int fuelPct = loco.getFuelPercent();
            int coalCount = loco.getCoalCount();
            String fuelText = coalCount > 0 ? ("x" + coalCount) : (fuelPct + "%");
            int fuelTextW = font.width(fuelText);
            int fuelTextX = x + panelW - fuelTextW - 4;
            int fuelTextColor = (loco.isLit() || coalCount > 0) ? 0xFFFFAB40 : 0xFF8B949E;
            graphics.text(font, fuelText, fuelTextX, row4Y, fuelTextColor, true);

            // Mini fuel bar next to fuel text
            int fBarW = 20;
            int fBarH = 5;
            int fBarX = fuelTextX - fBarW - 3;
            int fBarY = row4Y + 2;
            if (fBarX > x + font.width(gearLabel) + 6) {
                graphics.fill(fBarX, fBarY, fBarX + fBarW, fBarY + fBarH, 0xFF0D1117);
                graphics.outline(fBarX, fBarY, fBarW, fBarH, 0xFF30363D);
                int fFillW = Math.round((fBarW - 2) * (Mth.clamp(fuelPct, 0, 100) / 100.0F));
                if (fFillW > 0) {
                    int fColor = fuelPct > 25 ? 0xFFFF9800 : 0xFFF44336;
                    graphics.fill(fBarX + 1, fBarY + 1, fBarX + 1 + fFillW, fBarY + fBarH - 1, fColor);
                }
            }
        }
    }

    private static float getMaxReferenceSpeedBps(Entity vehicle, BasicLocomotiveEntity loco) {
        if (loco != null) {
            double cfgMax = 0.35D;
            try {
                cfgMax = Config.LOCOMOTIVE_MAX_SPEED.get();
            } catch (Throwable ignored) {
            }
            return (float) (cfgMax * 20.0D);
        }
        if (vehicle instanceof AbstractMinecart) {
            return 8.0F;
        }
        if (vehicle instanceof AbstractBoat) {
            return Math.max(10.0F, smoothedSpeedBps);
        }
        return 14.5F;
    }

    private static int getSpeedBarColor(float ratio) {
        if (ratio < 0.5F) {
            return 0xFF2EA043; // Green
        } else if (ratio < 0.85F) {
            return 0xFFD29922; // Gold / Amber
        } else {
            return 0xFFF85149; // High-speed Red-Orange
        }
    }

    private static String getCompassDirection(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        int index = Math.round(normalized / 45.0F) % 8;
        return switch (index) {
            case 0 -> "S";
            case 1 -> "SW";
            case 2 -> "W";
            case 3 -> "NW";
            case 4 -> "N";
            case 5 -> "NE";
            case 6 -> "E";
            case 7 -> "SE";
            default -> "N";
        };
    }
}
