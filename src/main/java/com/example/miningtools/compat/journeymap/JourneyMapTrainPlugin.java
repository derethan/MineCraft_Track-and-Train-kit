package com.example.miningtools.compat.journeymap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.AggregateHopperCartEntity;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CabooseMinecartEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.GeneratorMinecartEntity;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.entity.TrackLayerCartEntity;
import com.example.miningtools.entity.TrackPaverEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.TunnelPaverEntity;
import com.example.miningtools.entity.TurretCartEntity;
import com.example.miningtools.network.ChunkMapSyncPayload;
import com.example.miningtools.network.TrainMapSyncPayload;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygonWithHoles;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.api.v2.common.option.BooleanOption;
import journeymap.api.v2.common.option.OptionCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * JourneyMap client plugin that displays:
 * 1) Live moving train markers and train names on the JourneyMap minimap, fullscreen map, and webmap,
 *    persisting markers by persistent UUID without duplicating or leaving ghost trains across chunk reloads.
 * 2) Color-coded force-loaded chunk overlays for both stationary Industrial Chunk Anchors (Cyan/Teal)
 *    and mobile Rail Chunk Anchors / Anchor Cabooses / Active Consists (Gold/Amber), with hover titles,
 *    labels, and a JourneyMap Fullscreen toolbar toggle button.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyMapTrainPlugin implements IClientPlugin {

    private record MarkerState(
            BlockPos pos,
            int yRot,
            ResourceKey<Level> dimension,
            String name,
            Identifier iconId,
            boolean isLoco
    ) {}

    private record ChunkAnchorState(
            String ownerKey,
            ResourceKey<Level> dimension,
            int anchorType,
            BlockPos centerPos,
            String title,
            Set<Long> packedChunks
    ) {}

    private static IClientAPI jmAPI;
    private static final Map<UUID, MarkerOverlay> activeMarkers = new HashMap<>();
    private static final Map<UUID, MarkerState> activeStates = new HashMap<>();
    private static final Map<UUID, Long> recentlyRemovedUUIDs = new HashMap<>();

    // Chunk Anchor Overlay state
    private static final Map<String, List<PolygonOverlay>> activeChunkPolygons = new HashMap<>();
    private static final Map<String, MarkerOverlay> activeChunkAnchorMarkers = new HashMap<>();
    private static final Map<String, ChunkAnchorState> activeChunkStates = new HashMap<>();
    private static boolean showChunkOverlays = true;
    private static BooleanOption optShowChunkOverlays;
    private static BooleanOption optShowOnMinimap;

    private static int tickCounter = 0;

    private static final Identifier ICON_LOCOMOTIVE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/basic_locomotive.png");
    private static final Identifier ICON_DIESEL = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/diesel_locomotive.png");
    private static final Identifier ICON_ELECTRIC = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/electric_locomotive.png");
    private static final Identifier ICON_CABOOSE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/caboose.png");
    private static final Identifier ICON_BORE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/tunnel_bore.png");
    private static final Identifier ICON_TUNNEL_PAVER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/tunnel_paver.png");
    private static final Identifier ICON_PAVER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/track_paver.png");
    private static final Identifier ICON_TRACK_LAYER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/track_layer_cart.png");
    private static final Identifier ICON_TURRET = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/turret_cart.png");
    private static final Identifier ICON_TANKER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/large_liquid_minecart.png");
    private static final Identifier ICON_CARGO = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/cargo_minecart.png");
    private static final Identifier ICON_HOPPER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/hopper_cart.png");
    private static final Identifier ICON_LIVESTOCK = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/livestock_cart.png");
    private static final Identifier ICON_GENERATOR = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/generator_cart.png");
    private static final Identifier ICON_MINECART = Identifier.fromNamespaceAndPath("minecraft", "textures/item/minecart.png");
    private static final Identifier ICON_CHUNK_LOADER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/block/chunk_loader_top_on.png");

    // Color palette for Chunk Overlays
    private static final int STATIC_ANCHOR_STROKE = 0x06B6D4; // Industrial Cyan
    private static final int STATIC_ANCHOR_FILL = 0x22D3EE;   // Bright Teal Fill
    private static final int RAIL_ANCHOR_STROKE = 0xF59E0B;   // Railway Gold / Amber
    private static final int RAIL_ANCHOR_FILL = 0xFBBF24;     // Warm Gold Fill

    @Override
    public String getModId() {
        return MiningTools.MODID;
    }

    @Override
    public void initialize(IClientAPI api) {
        jmAPI = api;
        MiningTools.LOGGER.info("JourneyMap Train & Chunk Anchor Plugin initialized successfully!");
        NeoForge.EVENT_BUS.register(this);

        try {
            ClientEventRegistry.OPTIONS_REGISTRY_EVENT.subscribe(MiningTools.MODID, event -> {
                try {
                    OptionCategory category = new OptionCategory(
                            MiningTools.MODID,
                            "Track and Train Pack",
                            "Configure Train Markers and Force-Loaded Chunk Anchor Overlays on JourneyMap"
                    );
                    optShowChunkOverlays = new BooleanOption(
                            category,
                            "showChunkAnchorOverlays",
                            "Show Force-Loaded Chunk Anchors",
                            true
                    );
                    optShowOnMinimap = new BooleanOption(
                            category,
                            "showChunkAnchorsOnMinimap",
                            "Show Chunk Anchors on Minimap",
                            false
                    );
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            FullscreenEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(MiningTools.MODID, event -> {
                try {
                    showChunkOverlays = getOptionSafe(optShowChunkOverlays, showChunkOverlays);
                    IThemeButton btn = event.getThemeButtonDisplay().addThemeToggleButton(
                            "Chunk Anchors: ON",
                            "Chunk Anchors: OFF",
                            ICON_CABOOSE,
                            showChunkOverlays,
                            b -> {
                                b.toggle();
                                boolean toggled = Boolean.TRUE.equals(b.getToggled());
                                setChunkOverlaysVisible(toggled);
                            }
                    );
                    btn.setTooltip(
                            "Toggle Force-Loaded Chunk Anchors",
                            "Cyan: Stationary Industrial Chunk Anchors",
                            "Gold: Mobile Rail Chunk Anchors & Active Trains"
                    );
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            ClientEventRegistry.MAPPING_EVENT.subscribe(MiningTools.MODID, event -> {
                if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) {
                    clearAllMarkers();
                } else if (event.getStage() == MappingEvent.Stage.MAPPING_STARTED && jmAPI != null) {
                    showChunkOverlays = getOptionSafe(optShowChunkOverlays, showChunkOverlays);
                    for (MarkerOverlay marker : activeMarkers.values()) {
                        try {
                            marker.flagForRerender();
                            jmAPI.show(marker);
                        } catch (Exception ignored) {
                        }
                    }
                    if (showChunkOverlays) {
                        showAllCachedChunkOverlays();
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean getOptionSafe(BooleanOption option, boolean fallback) {
        if (option == null) return fallback;
        try {
            Boolean val = option.get();
            return val != null ? val : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static void setChunkOverlaysVisible(boolean visible) {
        showChunkOverlays = visible;
        if (optShowChunkOverlays != null) {
            try {
                optShowChunkOverlays.set(visible);
            } catch (Throwable ignored) {
            }
        }
        if (jmAPI == null) return;
        if (visible) {
            showAllCachedChunkOverlays();
        } else {
            hideAllChunkOverlaysFromMap();
        }
    }

    private static void showAllCachedChunkOverlays() {
        if (jmAPI == null) return;
        boolean includeMinimap = getOptionSafe(optShowOnMinimap, false);
        for (List<PolygonOverlay> polys : activeChunkPolygons.values()) {
            for (PolygonOverlay poly : polys) {
                try {
                    if (includeMinimap) {
                        poly.setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap, Context.UI.Webmap);
                    } else {
                        poly.setActiveUIs(Context.UI.Fullscreen, Context.UI.Webmap);
                    }
                    poly.flagForRerender();
                    jmAPI.show(poly);
                } catch (Exception ignored) {
                }
            }
        }
        for (MarkerOverlay marker : activeChunkAnchorMarkers.values()) {
            try {
                marker.flagForRerender();
                jmAPI.show(marker);
            } catch (Exception ignored) {
            }
        }
    }

    private static void hideAllChunkOverlaysFromMap() {
        if (jmAPI == null) return;
        for (List<PolygonOverlay> polys : activeChunkPolygons.values()) {
            for (PolygonOverlay poly : polys) {
                try {
                    jmAPI.remove(poly);
                } catch (Exception ignored) {
                }
            }
        }
        for (MarkerOverlay marker : activeChunkAnchorMarkers.values()) {
            try {
                jmAPI.remove(marker);
            } catch (Exception ignored) {
            }
        }
    }

    private static void clearAllMarkers() {
        activeMarkers.clear();
        activeStates.clear();
        recentlyRemovedUUIDs.clear();
        activeChunkPolygons.clear();
        activeChunkAnchorMarkers.clear();
        activeChunkStates.clear();
        if (jmAPI != null) {
            try {
                jmAPI.removeAll(MiningTools.MODID);
            } catch (Exception ignored) {
            }
        }
    }

    private static void removeMarker(UUID uuid) {
        activeStates.remove(uuid);
        MarkerOverlay removed = activeMarkers.remove(uuid);
        if (removed != null && jmAPI != null) {
            try {
                jmAPI.remove(removed);
            } catch (Exception ignored) {
            }
        }
    }

    private static void removeChunkAnchorOverlay(String ownerKey) {
        activeChunkStates.remove(ownerKey);
        List<PolygonOverlay> polys = activeChunkPolygons.remove(ownerKey);
        if (polys != null && jmAPI != null) {
            for (PolygonOverlay poly : polys) {
                try {
                    jmAPI.remove(poly);
                } catch (Exception ignored) {
                }
            }
        }
        MarkerOverlay marker = activeChunkAnchorMarkers.remove(ownerKey);
        if (marker != null && jmAPI != null) {
            try {
                jmAPI.remove(marker);
            } catch (Exception ignored) {
            }
        }
    }

    private static ChunkPos unpackChunkPos(long packed) {
        return new ChunkPos((int) packed, (int) (packed >>> 32));
    }

    private static void upsertChunkAnchorOverlay(ChunkMapSyncPayload.ChunkAnchorEntry entry) {
        if (jmAPI == null) return;

        String ownerKey = entry.ownerKey();
        List<Long> packedList = entry.packedChunks();
        if (packedList == null || packedList.isEmpty()) {
            removeChunkAnchorOverlay(ownerKey);
            return;
        }

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension()));
        Set<Long> packedSet = new HashSet<>(packedList);
        ChunkAnchorState newState = new ChunkAnchorState(
                ownerKey,
                dimKey,
                entry.anchorType(),
                entry.centerPos().immutable(),
                entry.title(),
                packedSet
        );

        ChunkAnchorState prevState = activeChunkStates.get(ownerKey);
        if (newState.equals(prevState) && activeChunkPolygons.containsKey(ownerKey)) {
            return;
        }

        // Remove previous polygons & static marker for this owner before rebuilding
        removeChunkAnchorOverlay(ownerKey);
        activeChunkStates.put(ownerKey, newState);

        List<ChunkPos> chunkCoords = new ArrayList<>(packedSet.size());
        for (long packed : packedSet) {
            chunkCoords.add(unpackChunkPos(packed));
        }

        boolean isStatic = (entry.anchorType() == ChunkMapSyncPayload.TYPE_STATIC_ANCHOR);
        int strokeColor = isStatic ? STATIC_ANCHOR_STROKE : RAIL_ANCHOR_STROKE;
        int fillColor = isStatic ? STATIC_ANCHOR_FILL : RAIL_ANCHOR_FILL;

        ShapeProperties shapeProps = new ShapeProperties()
                .setStrokeColor(strokeColor)
                .setStrokeOpacity(0.90F)
                .setStrokeWidth(2.5F)
                .setStrokePosition(ShapeProperties.StrokePosition.INSIDE)
                .setFillColor(fillColor)
                .setFillOpacity(isStatic ? 0.28F : 0.22F);

        TextProperties textProps = new TextProperties()
                .setColor(isStatic ? 0x67E8F9 : 0xFDE68A)
                .setBackgroundColor(0x0D1117)
                .setBackgroundOpacity(0.75F)
                .setFontShadow(true);

        boolean includeMinimap = getOptionSafe(optShowOnMinimap, false);

        List<MapPolygonWithHoles> mergedPolygons = PolygonHelper.createChunksPolygon(chunkCoords, entry.centerPos().getY());
        List<PolygonOverlay> builtOverlays = new ArrayList<>(mergedPolygons.size());

        int index = 0;
        for (MapPolygonWithHoles polyWithHoles : mergedPolygons) {
            PolygonOverlay polyOverlay = new PolygonOverlay(
                    MiningTools.MODID,
                    dimKey,
                    shapeProps,
                    polyWithHoles
            );
            polyOverlay.setOverlayGroupName(isStatic ? "Static Chunk Anchors" : "Rail Chunk Anchors");
            polyOverlay.setTitle(entry.title());
            polyOverlay.setTextProperties(textProps);
            polyOverlay.setDisplayOrder(isStatic ? 50 : 45);
            if (includeMinimap) {
                polyOverlay.setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap, Context.UI.Webmap);
            } else {
                polyOverlay.setActiveUIs(Context.UI.Fullscreen, Context.UI.Webmap);
            }
            if (index == 0) {
                // Concise center badge on the primary polygon
                String badge = isStatic
                        ? "⚓ Anchor (" + packedSet.size() + "c)"
                        : "⚓ Rail (" + packedSet.size() + "c)";
                polyOverlay.setLabel(badge);
            }
            builtOverlays.add(polyOverlay);
            if (showChunkOverlays) {
                try {
                    jmAPI.show(polyOverlay);
                } catch (Exception e) {
                    MiningTools.LOGGER.warn("Failed to show chunk polygon in JourneyMap: {}", e.getMessage());
                }
            }
            index++;
        }
        activeChunkPolygons.put(ownerKey, builtOverlays);

        // Also place a center icon marker for stationary Industrial Chunk Anchors
        if (isStatic) {
            MapImage icon = new MapImage(ICON_CHUNK_LOADER, 16, 16);
            icon.setDisplayWidth(16);
            icon.setDisplayHeight(16);
            icon.centerAnchors();

            MarkerOverlay centerMarker = new MarkerOverlay(MiningTools.MODID, entry.centerPos().immutable(), icon);
            centerMarker.setDimension(dimKey);
            centerMarker.setTitle(entry.title());
            centerMarker.setOverlayGroupName("Static Chunk Anchors");
            if (includeMinimap) {
                centerMarker.setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap, Context.UI.Webmap);
            } else {
                centerMarker.setActiveUIs(Context.UI.Fullscreen, Context.UI.Webmap);
            }

            activeChunkAnchorMarkers.put(ownerKey, centerMarker);
            if (showChunkOverlays) {
                try {
                    jmAPI.show(centerMarker);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void handleChunkMapSync(ChunkMapSyncPayload payload) {
        if (jmAPI == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (optShowChunkOverlays != null) {
            boolean optVal = getOptionSafe(optShowChunkOverlays, showChunkOverlays);
            if (optVal != showChunkOverlays) {
                setChunkOverlaysVisible(optVal);
            }
        }

        Set<String> snapshotKeys = new HashSet<>();
        for (ChunkMapSyncPayload.ChunkAnchorEntry entry : payload.entries()) {
            snapshotKeys.add(entry.ownerKey());
            upsertChunkAnchorOverlay(entry);
        }

        // Remove any chunk anchor overlays that are no longer active on the server
        for (String existingKey : new ArrayList<>(activeChunkStates.keySet())) {
            if (!snapshotKeys.contains(existingKey)) {
                removeChunkAnchorOverlay(existingKey);
            }
        }
    }

    private static void upsertMarker(
            UUID uuid,
            BlockPos pos,
            int yRot,
            ResourceKey<Level> dimKey,
            String name,
            Identifier iconId,
            boolean isLoco
    ) {
        if (jmAPI == null) return;

        MarkerOverlay marker = activeMarkers.get(uuid);
        MarkerState prevState = activeStates.get(uuid);
        MarkerState newState = new MarkerState(pos.immutable(), yRot, dimKey, name, iconId, isLoco);

        if (marker == null || prevState == null || !prevState.iconId().equals(iconId)) {
            if (marker != null) {
                try {
                    jmAPI.remove(marker);
                } catch (Exception ignored) {
                }
            }

            MapImage mapImage = new MapImage(iconId, 16, 16);
            mapImage.setDisplayWidth(20);
            mapImage.setDisplayHeight(20);
            mapImage.centerAnchors();
            mapImage.setRotation(yRot);

            marker = new MarkerOverlay(MiningTools.MODID, pos.immutable(), mapImage);
            marker.setDimension(dimKey);
            marker.setTitle(name);
            marker.setOverlayGroupName("Trains");
            applyLabelProperties(marker, name, isLoco);

            try {
                jmAPI.show(marker);
                activeMarkers.put(uuid, marker);
                activeStates.put(uuid, newState);
            } catch (Exception e) {
                MiningTools.LOGGER.warn("Failed to show train marker in JourneyMap: {}", e.getMessage());
            }
        } else {
            boolean existsInJm = false;
            try {
                existsInJm = jmAPI.exists(marker);
            } catch (Exception ignored) {
            }

            if (!existsInJm || !prevState.equals(newState)) {
                marker.setPoint(pos.immutable());
                marker.setDimension(dimKey);
                marker.setTitle(name);
                applyLabelProperties(marker, name, isLoco);
                marker.getIcon().setRotation(yRot);
                marker.flagForRerender();
                try {
                    jmAPI.show(marker);
                } catch (Exception ignored) {
                }
                activeStates.put(uuid, newState);
            }
        }
    }

    private static void applyLabelProperties(MarkerOverlay marker, String name, boolean isLoco) {
        if (isLoco) {
            if (marker.getTextProperties() == null) {
                TextProperties textProps = new TextProperties();
                textProps.setColor(0xFFFFFF);
                textProps.setBackgroundColor(0x000000);
                textProps.setBackgroundOpacity(0.65F);
                textProps.setFontShadow(true);
                textProps.setOffsetY(-20);
                marker.setTextProperties(textProps);
            } else {
                TextProperties textProps = marker.getTextProperties();
                textProps.setColor(0xFFFFFF);
                textProps.setBackgroundColor(0x000000);
                textProps.setBackgroundOpacity(0.65F);
                textProps.setFontShadow(true);
                textProps.setOffsetY(-20);
            }
            marker.setLabel(name);
        } else {
            marker.setLabel(null);
        }
    }

    public static void handleServerMapSync(TrainMapSyncPayload payload) {
        if (jmAPI == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        recentlyRemovedUUIDs.entrySet().removeIf(e -> e.getValue() < gameTime);

        Set<UUID> snapshotUUIDs = payload.fullSnapshot() ? new HashSet<>() : null;

        for (TrainMapSyncPayload.TrainMapEntry entry : payload.entries()) {
            UUID uuid = entry.uuid();
            if (entry.isRemoved()) {
                removeMarker(uuid);
                recentlyRemovedUUIDs.put(uuid, gameTime + 100L);
                continue;
            }

            recentlyRemovedUUIDs.remove(uuid);
            if (snapshotUUIDs != null) {
                snapshotUUIDs.add(uuid);
            }

            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension()));
            Identifier iconId = getIconForType(entry.iconType());
            boolean isLoco = isLocomotiveType(entry.iconType());

            upsertMarker(uuid, entry.pos(), entry.yRot(), dimKey, entry.name(), iconId, isLoco);
        }

        // Authoritative reconciliation: remove any ghost markers no longer present on the server
        if (snapshotUUIDs != null) {
            for (UUID existingUuid : new ArrayList<>(activeMarkers.keySet())) {
                if (!snapshotUUIDs.contains(existingUuid)) {
                    removeMarker(existingUuid);
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (jmAPI == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!activeMarkers.isEmpty() || !activeChunkStates.isEmpty()) {
                clearAllMarkers();
            }
            return;
        }

        // Update every 4 ticks (~5 times per second) for smooth local interpolation
        if (++tickCounter % 4 != 0) return;

        long gameTime = mc.level.getGameTime();
        recentlyRemovedUUIDs.entrySet().removeIf(e -> e.getValue() < gameTime);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractMinecart cart) {
                UUID uuid = cart.getUUID();
                if (cart.isRemoved() || !cart.isAlive()) {
                    removeMarker(uuid);
                    continue;
                }
                if (recentlyRemovedUUIDs.containsKey(uuid)) {
                    continue;
                }

                Identifier iconId = getIconForEntity(cart);
                if (iconId == null) continue;

                BlockPos pos = cart.blockPosition();
                String trainName = getBestTrainName(cart);
                boolean isLoco = isLocomotive(cart);

                upsertMarker(uuid, pos, (int) cart.getYRot(), cart.level().dimension(), trainName, iconId, isLoco);
            }
        }
    }

    public static boolean isLocomotive(AbstractMinecart cart) {
        return cart instanceof BasicLocomotiveEntity
                || cart instanceof DieselLocomotiveEntity
                || cart instanceof ElectricLocomotiveEntity;
    }

    public static boolean isLocomotiveType(String type) {
        return "locomotive".equals(type)
                || "diesel".equals(type)
                || "electric".equals(type);
    }

    public static String getBestTrainName(AbstractMinecart cart) {
        if (cart instanceof BasicLocomotiveEntity loco && !loco.getTrainName().isBlank()) return loco.getTrainName();
        if (cart instanceof DieselLocomotiveEntity diesel && !diesel.getTrainName().isBlank()) return diesel.getTrainName();
        if (cart instanceof ElectricLocomotiveEntity elec && !elec.getTrainName().isBlank()) return elec.getTrainName();
        if (cart instanceof TrackPaverEntity paver && !paver.getTrainName().isBlank()) return paver.getTrainName();
        if (cart instanceof TunnelPaverEntity tp && !tp.getTrainName().isBlank()) return tp.getTrainName();
        if (cart instanceof TrackLayerCartEntity tl && !tl.getTrainName().isBlank()) return tl.getTrainName();
        if (cart.getCustomName() != null) return cart.getCustomName().getString();
        return cart.getDisplayName().getString();
    }

    private static Identifier getIconForEntity(AbstractMinecart cart) {
        if (cart instanceof BasicLocomotiveEntity) {
            return ICON_LOCOMOTIVE;
        } else if (cart instanceof DieselLocomotiveEntity) {
            return ICON_DIESEL;
        } else if (cart instanceof ElectricLocomotiveEntity) {
            return ICON_ELECTRIC;
        } else if (cart instanceof CabooseMinecartEntity) {
            return ICON_CABOOSE;
        } else if (cart instanceof TunnelBoreEntity) {
            return ICON_BORE;
        } else if (cart instanceof TunnelPaverEntity) {
            return ICON_TUNNEL_PAVER;
        } else if (cart instanceof TrackPaverEntity) {
            return ICON_PAVER;
        } else if (cart instanceof TrackLayerCartEntity) {
            return ICON_TRACK_LAYER;
        } else if (cart instanceof TurretCartEntity) {
            return ICON_TURRET;
        } else if (cart instanceof AggregateHopperCartEntity) {
            return ICON_HOPPER;
        } else if (cart instanceof LivestockCartEntity) {
            return ICON_LIVESTOCK;
        } else if (cart instanceof GeneratorMinecartEntity) {
            return ICON_GENERATOR;
        } else if (cart instanceof LargeLiquidMinecartEntity || cart instanceof LiquidMinecartEntity) {
            return ICON_TANKER;
        } else if (cart instanceof CargoMinecartEntity) {
            return ICON_CARGO;
        } else if (BasicLocomotiveEntity.getConnectedLocomotive(cart) != null
                || DieselLocomotiveEntity.getConnectedLocomotive(cart) != null
                || ElectricLocomotiveEntity.getConnectedLocomotive(cart) != null) {
            return ICON_MINECART;
        }
        return null;
    }

    public static Identifier getIconForType(String type) {
        return switch (type) {
            case "diesel" -> ICON_DIESEL;
            case "electric" -> ICON_ELECTRIC;
            case "caboose" -> ICON_CABOOSE;
            case "bore" -> ICON_BORE;
            case "tunnel_paver" -> ICON_TUNNEL_PAVER;
            case "paver" -> ICON_PAVER;
            case "track_layer" -> ICON_TRACK_LAYER;
            case "turret" -> ICON_TURRET;
            case "hopper" -> ICON_HOPPER;
            case "livestock" -> ICON_LIVESTOCK;
            case "generator" -> ICON_GENERATOR;
            case "tanker" -> ICON_TANKER;
            case "cargo" -> ICON_CARGO;
            case "cart" -> ICON_MINECART;
            default -> ICON_LOCOMOTIVE;
        };
    }
}
