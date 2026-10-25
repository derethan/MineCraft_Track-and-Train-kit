package com.example.miningtools.compat.journeymap;

import java.util.HashMap;
import java.util.Map;

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
import com.example.miningtools.entity.TrackPaverEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.TurretCartEntity;
import com.example.miningtools.network.TrainMapSyncPayload;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * JourneyMap client plugin that displays live moving train markers and train names on the
 * JourneyMap minimap, fullscreen map, and webmap, persisting markers without disappearing at distance.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyMapTrainPlugin implements IClientPlugin {

    private static IClientAPI jmAPI;
    private static final Map<Integer, MarkerOverlay> activeMarkers = new HashMap<>();
    private static int tickCounter = 0;

    private static final Identifier ICON_LOCOMOTIVE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/basic_locomotive.png");
    private static final Identifier ICON_DIESEL = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/diesel_locomotive.png");
    private static final Identifier ICON_ELECTRIC = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/electric_locomotive.png");
    private static final Identifier ICON_CABOOSE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/caboose.png");
    private static final Identifier ICON_BORE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/tunnel_bore.png");
    private static final Identifier ICON_PAVER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/track_paver.png");
    private static final Identifier ICON_TURRET = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/turret_cart.png");
    private static final Identifier ICON_TANKER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/large_liquid_minecart.png");
    private static final Identifier ICON_CARGO = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/cargo_minecart.png");
    private static final Identifier ICON_HOPPER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/hopper_cart.png");
    private static final Identifier ICON_LIVESTOCK = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/livestock_cart.png");
    private static final Identifier ICON_GENERATOR = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/generator_cart.png");
    private static final Identifier ICON_MINECART = Identifier.fromNamespaceAndPath("minecraft", "textures/item/minecart.png");

    @Override
    public String getModId() {
        return MiningTools.MODID;
    }

    @Override
    public void initialize(IClientAPI api) {
        jmAPI = api;
        MiningTools.LOGGER.info("JourneyMap Train Plugin initialized successfully!");
        NeoForge.EVENT_BUS.register(this);
    }

    public static void handleServerMapSync(TrainMapSyncPayload payload) {
        if (jmAPI == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (TrainMapSyncPayload.TrainMapEntry entry : payload.entries()) {
            int id = entry.entityId();
            if (entry.isRemoved()) {
                MarkerOverlay removed = activeMarkers.remove(id);
                if (removed != null) {
                    jmAPI.remove(removed);
                }
                continue;
            }

            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension()));
            MarkerOverlay marker = activeMarkers.get(id);
            if (marker == null) {
                Identifier iconId = getIconForType(entry.iconType());
                MapImage mapImage = new MapImage(iconId, 16, 16);
                mapImage.setDisplayWidth(20);
                mapImage.setDisplayHeight(20);
                mapImage.centerAnchors();
                mapImage.setRotation(entry.yRot());

                marker = new MarkerOverlay(MiningTools.MODID, entry.pos(), mapImage);
                marker.setDimension(dimKey);
                marker.setTitle(entry.name());
                marker.setOverlayGroupName("Trains");

                TextProperties textProps = new TextProperties();
                textProps.setColor(0xFFFFFF);
                textProps.setBackgroundColor(0x000000);
                textProps.setBackgroundOpacity(0.65F);
                textProps.setFontShadow(true);
                textProps.setOffsetY(-20);
                marker.setTextProperties(textProps);
                marker.setLabel(entry.name());

                try {
                    jmAPI.show(marker);
                    activeMarkers.put(id, marker);
                } catch (Exception e) {
                    MiningTools.LOGGER.warn("Failed to show train marker in JourneyMap: {}", e.getMessage());
                }
            } else {
                marker.setPoint(entry.pos());
                marker.setDimension(dimKey);
                marker.setTitle(entry.name());
                marker.setLabel(entry.name());
                marker.getIcon().setRotation(entry.yRot());
                marker.flagForRerender();
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (jmAPI == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!activeMarkers.isEmpty()) {
                activeMarkers.clear();
                jmAPI.removeAll(MiningTools.MODID);
            }
            return;
        }

        // Update every 4 ticks (~5 times per second) for smooth tracking
        if (++tickCounter % 4 != 0) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractMinecart cart) {
                int id = cart.getId();
                if (cart.isRemoved() || !cart.isAlive()) {
                    MarkerOverlay removed = activeMarkers.remove(id);
                    if (removed != null) {
                        jmAPI.remove(removed);
                    }
                    continue;
                }

                Identifier iconId = getIconForEntity(cart);
                if (iconId == null) continue;

                BlockPos pos = cart.blockPosition();
                String trainName = getBestTrainName(cart);
                MarkerOverlay marker = activeMarkers.get(id);

                if (marker == null) {
                    MapImage mapImage = new MapImage(iconId, 16, 16);
                    mapImage.setDisplayWidth(20);
                    mapImage.setDisplayHeight(20);
                    mapImage.centerAnchors();
                    mapImage.setRotation((int) cart.getYRot());

                    marker = new MarkerOverlay(MiningTools.MODID, pos, mapImage);
                    marker.setDimension(cart.level().dimension());
                    marker.setTitle(trainName);
                    marker.setOverlayGroupName("Trains");

                    TextProperties textProps = new TextProperties();
                    textProps.setColor(0xFFFFFF);
                    textProps.setBackgroundColor(0x000000);
                    textProps.setBackgroundOpacity(0.65F);
                    textProps.setFontShadow(true);
                    textProps.setOffsetY(-20);
                    marker.setTextProperties(textProps);
                    marker.setLabel(trainName);

                    try {
                        jmAPI.show(marker);
                        activeMarkers.put(id, marker);
                    } catch (Exception e) {
                        MiningTools.LOGGER.warn("Failed to show train marker in JourneyMap: {}", e.getMessage());
                    }
                } else {
                    marker.setPoint(pos);
                    marker.setDimension(cart.level().dimension());
                    marker.setTitle(trainName);
                    marker.setLabel(trainName);
                    marker.getIcon().setRotation((int) cart.getYRot());
                    marker.flagForRerender();
                }
            }
        }
    }

    public static String getBestTrainName(AbstractMinecart cart) {
        if (cart instanceof BasicLocomotiveEntity loco && !loco.getTrainName().isBlank()) return loco.getTrainName();
        if (cart instanceof DieselLocomotiveEntity diesel && !diesel.getTrainName().isBlank()) return diesel.getTrainName();
        if (cart instanceof ElectricLocomotiveEntity elec && !elec.getTrainName().isBlank()) return elec.getTrainName();
        if (cart instanceof TrackPaverEntity paver && !paver.getTrainName().isBlank()) return paver.getTrainName();
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
        } else if (cart instanceof TrackPaverEntity) {
            return ICON_PAVER;
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
        } else if (BasicLocomotiveEntity.getConnectedLocomotive(cart) != null || ElectricLocomotiveEntity.getConnectedLocomotive(cart) != null) {
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
            case "paver" -> ICON_PAVER;
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
