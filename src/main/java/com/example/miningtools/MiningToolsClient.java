package com.example.miningtools;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.example.miningtools.client.gui.BasicLocomotiveScreen;
import com.example.miningtools.client.gui.AggregateHopperCartScreen;
import com.example.miningtools.client.gui.CargoMinecartScreen;
import com.example.miningtools.client.model.AggregateHopperCartModel;
import com.example.miningtools.client.renderer.AggregateHopperCartRenderer;
import com.example.miningtools.client.gui.DieselLocomotiveScreen;
import com.example.miningtools.client.gui.ChunkLoaderScreen;
import com.example.miningtools.client.gui.EntityTransferScreen;
import com.example.miningtools.client.gui.ElectricLocomotiveScreen;
import com.example.miningtools.client.gui.FluidLoaderScreen;
import com.example.miningtools.client.gui.CargoTransferScreen;
import com.example.miningtools.client.gui.CartRenameScreen;
import com.example.miningtools.network.CargoTransferSyncPayload;
import com.example.miningtools.network.FluidLoaderSyncPayload;
import com.example.miningtools.network.CartRenameOpenPayload;
import com.example.miningtools.client.gui.SpeedometerOverlay;
import com.example.miningtools.client.gui.TrackSwitchScreen;
import com.example.miningtools.client.gui.TunnelBoreScreen;
import com.example.miningtools.client.gui.TrackPaverScreen;
import com.example.miningtools.client.model.TrackPaverModel;
import com.example.miningtools.client.renderer.TrackPaverRenderer;
import com.example.miningtools.client.gui.GeneratorMinecartScreen;
import com.example.miningtools.client.model.GeneratorCartModel;
import com.example.miningtools.client.renderer.GeneratorCartRenderer;
import com.example.miningtools.client.gui.TurretCartScreen;
import com.example.miningtools.client.model.TurretCartModel;
import com.example.miningtools.client.renderer.TurretCartRenderer;
import com.example.miningtools.client.model.BasicLocomotiveModel;
import com.example.miningtools.client.model.CabooseModel;
import com.example.miningtools.client.model.DieselLocomotiveModel;
import com.example.miningtools.client.model.ElectricLocomotiveModel;
import com.example.miningtools.client.model.LargeLiquidMinecartModel;
import com.example.miningtools.client.model.LiquidMinecartModel;
import com.example.miningtools.client.model.LivestockCartModel;
import com.example.miningtools.client.model.LoggingCartModel;
import com.example.miningtools.client.model.TunnelBoreModel;
import com.example.miningtools.client.renderer.BasicLocomotiveRenderer;
import com.example.miningtools.client.renderer.CabooseRenderer;
import com.example.miningtools.client.renderer.CargoMinecartRenderer;
import com.example.miningtools.client.renderer.LivestockCartRenderer;
import com.example.miningtools.client.renderer.DieselLocomotiveRenderer;
import com.example.miningtools.client.renderer.ElectricLocomotiveRenderer;
import com.example.miningtools.client.renderer.LargeLiquidMinecartRenderer;
import com.example.miningtools.client.renderer.LiquidMinecartRenderer;
import com.example.miningtools.client.renderer.TunnelBoreRenderer;

import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.TrackPaverEntity;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.network.TrackSwitchSyncPayload;
import com.example.miningtools.network.ChunkLoaderSyncPayload;
import com.example.miningtools.network.EntityTransferSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MiningTools.MODID, dist = Dist.CLIENT)
public class MiningToolsClient {
    public static final ModelLayerLocation TRACK_PAVER_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "track_paver"), "main");

    public static final ModelLayerLocation TUNNEL_BORE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "tunnel_bore"), "main");

    public static final ModelLayerLocation BASIC_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "basic_locomotive"), "main");

    public static final ModelLayerLocation LIVESTOCK_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "livestock_cart"), "main");

    public static final ModelLayerLocation HOPPER_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "hopper_cart"), "main");

    public static final ModelLayerLocation LOGGING_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "logging_cart"), "main");

    public static final ModelLayerLocation LIQUID_MINECART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "liquid_minecart"), "main");

    public static final ModelLayerLocation LARGE_LIQUID_MINECART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "large_liquid_minecart"), "main");

    public static final ModelLayerLocation TURRET_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "turret_cart"), "main");

    public static final ModelLayerLocation GENERATOR_CART_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "generator_cart"), "main");

    public static final ModelLayerLocation CABOOSE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "caboose"), "main");

    public static final ModelLayerLocation ELECTRIC_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "electric_locomotive"), "main");

    public static final ModelLayerLocation DIESEL_LOCOMOTIVE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MiningTools.MODID, "diesel_locomotive"), "main");

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

    public static final KeyMapping KEY_OPEN_GUI = new KeyMapping(
            "key.miningtools.open_gui",
            InputConstants.KEY_C,
            MINING_TOOLS_CATEGORY
    );

    private static boolean wasUpPressed = false;
    private static boolean wasDownPressed = false;
    private static boolean wasLeftPressed = false;
    private static boolean wasRightPressed = false;
    private static boolean wasReversePressed = false;
    private static boolean wasHornPressed = false;
    private static boolean wasLightPressed = false;
    private static boolean wasOpenGuiPressed = false;

    public MiningToolsClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerLayers);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerMenuScreens);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::onModelBakingCompleted);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(SpeedometerOverlay::onRenderGui);
        TrackSwitchSyncPayload.CLIENT_HANDLER = TrackSwitchScreen::open;
        EntityTransferSyncPayload.CLIENT_HANDLER = EntityTransferScreen::open;
        ChunkLoaderSyncPayload.CLIENT_HANDLER = ChunkLoaderScreen::open;
        CartRenameOpenPayload.CLIENT_HANDLER = CartRenameScreen::open;
        CargoTransferSyncPayload.CLIENT_HANDLER = CargoTransferScreen::onSyncPayload;
        FluidLoaderSyncPayload.CLIENT_HANDLER = FluidLoaderScreen::onSyncPayload;
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        MiningTools.LOGGER.info("Mining Tools client setup initialized");
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TUNNEL_BORE_LAYER, TunnelBoreModel::createBodyLayer);
        event.registerLayerDefinition(TRACK_PAVER_LAYER, TrackPaverModel::createBodyLayer);
        event.registerLayerDefinition(BASIC_LOCOMOTIVE_LAYER, BasicLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(LOGGING_CART_LAYER, LoggingCartModel::createBodyLayer);
        event.registerLayerDefinition(HOPPER_CART_LAYER, AggregateHopperCartModel::createBodyLayer);
        event.registerLayerDefinition(LIVESTOCK_CART_LAYER, LivestockCartModel::createBodyLayer);
        event.registerLayerDefinition(LIQUID_MINECART_LAYER, LiquidMinecartModel::createBodyLayer);
        event.registerLayerDefinition(LARGE_LIQUID_MINECART_LAYER, LargeLiquidMinecartModel::createBodyLayer);
        event.registerLayerDefinition(CABOOSE_LAYER, CabooseModel::createBodyLayer);
        event.registerLayerDefinition(TURRET_CART_LAYER, TurretCartModel::createBodyLayer);
        event.registerLayerDefinition(GENERATOR_CART_LAYER, GeneratorCartModel::createBodyLayer);
        event.registerLayerDefinition(ELECTRIC_LOCOMOTIVE_LAYER, ElectricLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(DIESEL_LOCOMOTIVE_LAYER, DieselLocomotiveModel::createBodyLayer);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MiningTools.TUNNEL_BORE_ENTITY.get(), TunnelBoreRenderer::new);
        event.registerEntityRenderer(MiningTools.TRACK_PAVER_ENTITY.get(), TrackPaverRenderer::new);
        event.registerEntityRenderer(MiningTools.BASIC_LOCOMOTIVE_ENTITY.get(), BasicLocomotiveRenderer::new);
        event.registerEntityRenderer(MiningTools.CARGO_MINECART_ENTITY.get(), CargoMinecartRenderer::new);
        event.registerEntityRenderer(MiningTools.HOPPER_CART_ENTITY.get(), AggregateHopperCartRenderer::new);
        event.registerEntityRenderer(MiningTools.LIVESTOCK_CART_ENTITY.get(), LivestockCartRenderer::new);
        event.registerEntityRenderer(MiningTools.LIQUID_MINECART_ENTITY.get(), LiquidMinecartRenderer::new);
        event.registerEntityRenderer(MiningTools.LARGE_LIQUID_MINECART_ENTITY.get(), LargeLiquidMinecartRenderer::new);
        event.registerEntityRenderer(MiningTools.CABOOSE_ENTITY.get(), CabooseRenderer::new);
        event.registerEntityRenderer(MiningTools.TURRET_CART_ENTITY.get(), TurretCartRenderer::new);
        event.registerEntityRenderer(MiningTools.GENERATOR_MINECART_ENTITY.get(), GeneratorCartRenderer::new);
        event.registerEntityRenderer(MiningTools.ELECTRIC_LOCOMOTIVE_ENTITY.get(), ElectricLocomotiveRenderer::new);
        event.registerEntityRenderer(MiningTools.DIESEL_LOCOMOTIVE_ENTITY.get(), DieselLocomotiveRenderer::new);
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MiningTools.TUNNEL_BORE_MENU.get(), TunnelBoreScreen::new);
        event.register(MiningTools.TRACK_PAVER_MENU.get(), TrackPaverScreen::new);
        event.register(MiningTools.BASIC_LOCOMOTIVE_MENU.get(), BasicLocomotiveScreen::new);
        event.register(MiningTools.CARGO_MINECART_MENU.get(), CargoMinecartScreen::new);
        event.register(MiningTools.HOPPER_CART_MENU.get(), AggregateHopperCartScreen::new);
        event.register(MiningTools.FLUID_LOADER_MENU.get(), FluidLoaderScreen::new);
        event.register(MiningTools.CARGO_TRANSFER_MENU.get(), CargoTransferScreen::new);
        event.register(MiningTools.ELECTRIC_LOCOMOTIVE_MENU.get(), ElectricLocomotiveScreen::new);
        event.register(MiningTools.DIESEL_LOCOMOTIVE_MENU.get(), DieselLocomotiveScreen::new);
        event.register(MiningTools.TURRET_CART_MENU.get(), TurretCartScreen::new);
        event.register(MiningTools.GENERATOR_MINECART_MENU.get(), GeneratorMinecartScreen::new);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_REVERSE);
        event.register(KEY_HORN);
        event.register(KEY_LIGHT);
        event.register(KEY_OPEN_GUI);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        SpeedometerOverlay.onClientTick(mc);
        if (mc.player == null || mc.level == null) return;

        if (++fluidPatchCooldown > 100) {
            fluidPatchCooldown = 0;
            patchFluidModels();
        }

        if (mc.gui.screen() != null) {
            wasUpPressed = false;
            wasDownPressed = false;
            wasLeftPressed = false;
            wasRightPressed = false;
            wasReversePressed = false;
            wasHornPressed = false;
            wasLightPressed = false;
            wasOpenGuiPressed = false;
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
            wasOpenGuiPressed = false;
            return;
        }

        BasicLocomotiveEntity loco = BasicLocomotiveEntity.getConnectedLocomotive(vehicle);
        ElectricLocomotiveEntity elecLoco = ElectricLocomotiveEntity.getConnectedLocomotive(vehicle);
        DieselLocomotiveEntity dieselLoco = DieselLocomotiveEntity.getConnectedLocomotive(vehicle);
        boolean isBore = (vehicle instanceof com.example.miningtools.entity.TunnelBoreEntity b && !b.isRemoved());
        boolean isPaver = (vehicle instanceof TrackPaverEntity p && !p.isRemoved());
        boolean hasMenu = (vehicle instanceof net.minecraft.world.MenuProvider);
        if ((loco == null || loco.isRemoved()) && (elecLoco == null || elecLoco.isRemoved()) && (dieselLoco == null || dieselLoco.isRemoved()) && !isBore && !isPaver && !hasMenu) {
            wasUpPressed = false;
            wasDownPressed = false;
            wasLeftPressed = false;
            wasRightPressed = false;
            wasReversePressed = false;
            wasHornPressed = false;
            wasLightPressed = false;
            wasOpenGuiPressed = false;
            return;
        }

        boolean isOpenGui = KEY_OPEN_GUI.isDown();
        if (isOpenGui && !wasOpenGuiPressed) {
            ClientPacketDistributor.sendToServer(new LocomotiveControlPayload(LocomotiveControlPayload.ACTION_OPEN_GUI));
        }
        wasOpenGuiPressed = isOpenGui;

        boolean isLocoOrBoreOrPaver = (loco != null && !loco.isRemoved()) || (elecLoco != null && !elecLoco.isRemoved()) || (dieselLoco != null && !dieselLoco.isRemoved()) || isBore || isPaver;
        if (!isLocoOrBoreOrPaver) {
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

    private static int fluidPatchCooldown = 0;

    private void onModelBakingCompleted(ModelEvent.BakingCompleted event) {
        patchFluidModels();
    }

    public static void patchFluidModels() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getModelManager() == null) return;
            FluidStateModelSet modelSet = mc.getModelManager().getFluidStateModelSet();
            if (modelSet == null) return;

            Field modelByFluidField = FluidStateModelSet.class.getDeclaredField("modelByFluid");
            modelByFluidField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Fluid, FluidModel> map = (Map<Fluid, FluidModel>) modelByFluidField.get(modelSet);

            FluidModel waterModel = modelSet.get(Fluids.WATER.defaultFluidState());
            if (waterModel == null) return;

            Map<Fluid, FluidModel> newMap = (map != null) ? new HashMap<>(map) : new HashMap<>();
            FluidModel dieselModel = new FluidModel(
                    waterModel.layer(),
                    waterModel.stillMaterial(),
                    waterModel.flowingMaterial(),
                    waterModel.overlayMaterial(),
                    state -> 0xFFEAB308 // Golden yellow amber
            );

            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (!newMap.containsKey(fluid)) {
                    Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
                    if (id != null) {
                        String path = id.getPath().toLowerCase();
                        String ns = id.getNamespace().toLowerCase();
                        if (path.contains("diesel") || path.contains("fuel") || path.contains("bio") || path.contains("oil")
                                || ns.contains("refinery") || ns.contains("car") || ns.contains("logistics")) {
                            newMap.put(fluid, dieselModel);
                        }
                    }
                }
            }

            modelByFluidField.set(modelSet, newMap);

            Field missingModelField = FluidStateModelSet.class.getDeclaredField("missingModel");
            missingModelField.setAccessible(true);
            missingModelField.set(modelSet, dieselModel);
        } catch (Throwable t) {
            MiningTools.LOGGER.debug("Could not patch fluid models: {}", t.getMessage());
        }
    }
}
