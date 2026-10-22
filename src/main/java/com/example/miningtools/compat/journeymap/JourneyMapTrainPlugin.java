package com.example.miningtools.compat.journeymap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CabooseMinecartEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.TunnelBoreEntity;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * JourneyMap client plugin that displays live moving train markers on the
 * JourneyMap minimap, fullscreen map, and webmap.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyMapTrainPlugin implements IClientPlugin {

    private static IClientAPI jmAPI;
    private static final Map<Integer, MarkerOverlay> activeMarkers = new HashMap<>();
    private static int tickCounter = 0;

    private static final Identifier ICON_LOCOMOTIVE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/basic_locomotive.png");
    private static final Identifier ICON_ELECTRIC = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/electric_locomotive.png");
    private static final Identifier ICON_CABOOSE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/caboose.png");
    private static final Identifier ICON_BORE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/tunnel_bore.png");
    private static final Identifier ICON_TANKER = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/large_liquid_minecart.png");
    private static final Identifier ICON_CARGO = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/item/cargo_minecart.png");
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

        Set<Integer> seenEntityIds = new HashSet<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractMinecart cart && cart.isAlive()) {
                Identifier iconId = getIconForEntity(cart);
                if (iconId == null) continue;

                int id = cart.getId();
                seenEntityIds.add(id);

                BlockPos pos = cart.blockPosition();
                MarkerOverlay marker = activeMarkers.get(id);

                if (marker == null) {
                    MapImage mapImage = new MapImage(iconId, 16, 16);
                    mapImage.setDisplayWidth(20);
                    mapImage.setDisplayHeight(20);
                    mapImage.centerAnchors();
                    mapImage.setRotation((int) cart.getYRot());

                    marker = new MarkerOverlay(MiningTools.MODID, pos, mapImage);
                    marker.setDimension(cart.level().dimension());
                    marker.setTitle(cart.getDisplayName().getString());
                    marker.setOverlayGroupName("Trains");

                    try {
                        jmAPI.show(marker);
                        activeMarkers.put(id, marker);
                    } catch (Exception e) {
                        MiningTools.LOGGER.warn("Failed to show train marker in JourneyMap: {}", e.getMessage());
                    }
                } else {
                    marker.setPoint(pos);
                    marker.setDimension(cart.level().dimension());
                    marker.getIcon().setRotation((int) cart.getYRot());
                    marker.flagForRerender();
                }
            }
        }

        // Clean up markers for entities that have despawned or left render distance
        activeMarkers.entrySet().removeIf(entry -> {
            if (!seenEntityIds.contains(entry.getKey())) {
                jmAPI.remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private static Identifier getIconForEntity(AbstractMinecart cart) {
        if (cart instanceof BasicLocomotiveEntity) {
            return ICON_LOCOMOTIVE;
        } else if (cart instanceof ElectricLocomotiveEntity) {
            return ICON_ELECTRIC;
        } else if (cart instanceof CabooseMinecartEntity) {
            return ICON_CABOOSE;
        } else if (cart instanceof TunnelBoreEntity) {
            return ICON_BORE;
        } else if (cart instanceof LargeLiquidMinecartEntity || cart instanceof LiquidMinecartEntity) {
            return ICON_TANKER;
        } else if (cart instanceof CargoMinecartEntity) {
            return ICON_CARGO;
        } else if (BasicLocomotiveEntity.getConnectedLocomotive(cart) != null || ElectricLocomotiveEntity.getConnectedLocomotive(cart) != null) {
            return ICON_MINECART;
        }
        return null;
    }
}
