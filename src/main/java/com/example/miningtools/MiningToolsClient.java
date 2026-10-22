package com.example.miningtools;

import com.example.miningtools.client.gui.BasicLocomotiveScreen;
import com.example.miningtools.client.gui.CargoMinecartScreen;
import com.example.miningtools.client.gui.ElectricLocomotiveScreen;
import com.example.miningtools.client.gui.FluidLoaderScreen;
import com.example.miningtools.client.gui.SpeedometerOverlay;
import com.example.miningtools.client.gui.TrackSwitchScreen;
import com.example.miningtools.client.gui.TunnelBoreScreen;
import com.example.miningtools.client.model.BasicLocomotiveModel;
import com.example.miningtools.client.model.ElectricLocomotiveModel;
import com.example.miningtools.client.model.LiquidMinecartModel;
import com.example.miningtools.client.model.LoggingCartModel;
import com.example.miningtools.client.model.TunnelBoreModel;
import com.example.miningtools.client.renderer.BasicLocomotiveRenderer;
import com.example.miningtools.client.renderer.CargoMinecartRenderer;
import com.example.miningtools.client.renderer.ElectricLocomotiveRenderer;
import com.example.miningtools.client.renderer.LiquidMinecartRenderer;
import com.example.miningtools.client.renderer.TunnelBoreRenderer;

import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.network.TrackSwitchSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MiningTools.MODID, dist = Dist.CLIENT)
public class MiningToolsClient {
    public static final ModelLayerLocation TUNNEL_BORE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "tunnel_bore"), "main");

    public static final ModelLayerLocation BASIC_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "basic_locomotive"), "main");

    public static final ModelLayerLocation LOGGING_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "logging_cart"), "main");

    public static final ModelLayerLocation LIQUID_MINECART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "liquid_minecart"), "main");

    public static final ModelLayerLocation ELECTRIC_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "electric_locomotive"), "main");

    public static final KeyMapping.Category MINING_TOOLS_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MiningTools.MODID, "controls"));

    public static final KeyMapping KEY_REVERSE = new KeyMapping(
            "key.miningtools.reverse",
            InputConstants.KEY_R,
            MINING_TOOLS_CATEGORY
    );

    public static final KeyMapping KEY_HORN = new KeyMapping(
            "key.miningtools.horn",
            InputConstants.KEY_H,
            MINING_TOOLS_CATEGORY
    );

    public static final KeyMapping KEY_LIGHT = new KeyMapping(
            "key.miningtools.light",
            InputConstants.KEY_L,
            MINING_TOOLS_CATEGORY
    );

    private static boolean wasUpPressed = false;
    private static boolean wasDownPressed = false;
    private static boolean wasLeftPressed = false;
    private static boolean wasRightPressed = false;
    private static boolean wasReversePressed = false;
    private static boolean wasHornPressed = false;
    private static boolean wasLightPressed = false;

    public MiningToolsClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerLayers);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerMenuScreens);
        modEventBus.addListener(this::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(SpeedometerOverlay::onRenderGui);
        TrackSwitchSyncPayload.CLIENT_HANDLER = TrackSwitchScreen::open;
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        MiningTools.LOGGER.info("Mining Tools client setup initialized");
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TUNNEL_BORE_LAYER, TunnelBoreModel::createBodyLayer);
        event.registerLayerDefinition(BASIC_LOCOMOTIVE_LAYER, BasicLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(LOGGING_CART_LAYER, LoggingCartModel::createBodyLayer);
        event.registerLayerDefinition(LIQUID_MINECART_LAYER, LiquidMinecartModel::createBodyLayer);
        event.registerLayerDefinition(ELECTRIC_LOCOMOTIVE_LAYER, ElectricLocomotiveModel::createBodyLayer);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MiningTools.TUNNEL_BORE_ENTITY.get(), TunnelBoreRenderer::new);
        event.registerEntityRenderer(MiningTools.BASIC_LOCOMOTIVE_ENTITY.get(), BasicLocomotiveRenderer::new);
        event.registerEntityRenderer(MiningTools.CARGO_MINECART_ENTITY.get(), CargoMinecartRenderer::new);
        event.registerEntityRenderer(MiningTools.LIQUID_MINECART_ENTITY.get(), LiquidMinecartRenderer::new);
        event.registerEntityRenderer(MiningTools.ELECTRIC_LOCOMOTIVE_ENTITY.get(), ElectricLocomotiveRenderer::new);
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MiningTools.TUNNEL_BORE_MENU.get(), TunnelBoreScreen::new);
        event.register(MiningTools.BASIC_LOCOMOTIVE_MENU.get(), BasicLocomotiveScreen::new);
        event.register(MiningTools.CARGO_MINECART_MENU.get(), CargoMinecartScreen::new);
        event.register(MiningTools.FLUID_LOADER_MENU.get(), FluidLoaderScreen::new);
        event.register(MiningTools.ELECTRIC_LOCOMOTIVE_MENU.get(), ElectricLocomotiveScreen::new);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_REVERSE);
        event.register(KEY_HORN);
        event.register(KEY_LIGHT);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        SpeedometerOverlay.onClientTick(mc);
        if (mc.player == null || mc.level == null) return;

        if (mc.gui.screen() != null) {
            wasUpPressed = false;
            wasDownPressed = false;
            wasLeftPressed = false;
            wasRightPressed = false;
            wasReversePressed = false;
            wasHornPressed = false;
            wasLightPressed = false;
            return;
        }

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) {
            wasUpPressed = false;
            wasDownPressed = false;
            wasLeftPressed = false;
            wasRightPressed = false;
            wasReversePressed = false;
            wasHornPressed = false;
            wasLightPressed = false;
            return;
        }

        BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
        ElectricLocomotiveEntity elecLoco = ElectricLocomotiveEntity.getConnectedLocomotive(vehicle);
        if ((loco == null || loco.isRemoved()) && (elecLoco == null || elecLoco.isRemoved())) {
            wasUpPressed = false;
            wasDownPressed = false;
            wasLeftPressed = false;
            wasRightPressed = false;
            wasReversePressed = false;
            wasHornPressed = false;
            wasLightPressed = false;
            return;
        }

        boolean isUp = mc.options.keyUp.isDown();
        boolean isDown = mc.options.keyDown.isDown();
        boolean isLeft = mc.options.keyLeft.isDown();
        boolean isRight = mc.options.keyRight.isDown();
        boolean isReverse = KEY_REVERSE.isDown();
        boolean isHorn = KEY_HORN.isDown();
        boolean isLight = KEY_LIGHT.isDown();

        if (isUp && !wasUpPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_START));
        }
        if (isDown && !wasDownPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_STOP));
        }
        if (isLeft && !wasLeftPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_SPEED_DOWN));
        }
        if (isRight && !wasRightPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_SPEED_UP));
        }
        if (isReverse && !wasReversePressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_REVERSE));
        }
        if (isHorn && !wasHornPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_HORN));
        }
        if (isLight && !wasLightPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_TOGGLE_LIGHT));
        }

        wasUpPressed = isUp;
        wasDownPressed = isDown;
        wasLeftPressed = isLeft;
        wasRightPressed = isRight;
        wasReversePressed = isReverse;
        wasHornPressed = isHorn;
        wasLightPressed = isLight;
    }
}
