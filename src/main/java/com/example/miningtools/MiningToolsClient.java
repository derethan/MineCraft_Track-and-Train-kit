package com.example.miningtools;

import com.example.miningtools.client.gui.BasicLocomotiveScreen;
import com.example.miningtools.client.gui.TunnelBoreScreen;
import com.example.miningtools.client.model.BasicLocomotiveModel;
import com.example.miningtools.client.model.TunnelBoreModel;
import com.example.miningtools.client.renderer.BasicLocomotiveRenderer;
import com.example.miningtools.client.renderer.TunnelBoreRenderer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MiningTools.MODID, dist = Dist.CLIENT)
public class MiningToolsClient {
    public static final ModelLayerLocation TUNNEL_BORE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "tunnel_bore"), "main");

    public static final ModelLayerLocation BASIC_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "basic_locomotive"), "main");

    public MiningToolsClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerLayers);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerMenuScreens);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        MiningTools.LOGGER.info("Mining Tools client setup initialized");
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TUNNEL_BORE_LAYER, TunnelBoreModel::createBodyLayer);
        event.registerLayerDefinition(BASIC_LOCOMOTIVE_LAYER, BasicLocomotiveModel::createBodyLayer);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MiningTools.TUNNEL_BORE_ENTITY.get(), TunnelBoreRenderer::new);
        event.registerEntityRenderer(MiningTools.BASIC_LOCOMOTIVE_ENTITY.get(), BasicLocomotiveRenderer::new);
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MiningTools.TUNNEL_BORE_MENU.get(), TunnelBoreScreen::new);
        event.register(MiningTools.BASIC_LOCOMOTIVE_MENU.get(), BasicLocomotiveScreen::new);
    }
}
