package com.example.miningtools;

import org.slf4j.Logger;

import com.example.miningtools.block.JunctionRailBlock;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.item.BasicLocomotiveItem;
import com.example.miningtools.item.TunnelBoreItem;
import com.example.miningtools.menu.BasicLocomotiveMenu;
import com.example.miningtools.menu.TunnelBoreMenu;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MiningTools.MODID)
public class MiningTools {
    public static final String MODID = "miningtools";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Junction Rail Block & Item
    public static final DeferredBlock<JunctionRailBlock> JUNCTION_RAIL = BLOCKS.registerBlock(
            "junction_rail",
            JunctionRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> JUNCTION_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "junction_rail",
            JUNCTION_RAIL
    );

    // Tunnel Bore Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<TunnelBoreEntity>> TUNNEL_BORE_ENTITY = ENTITY_TYPES.register(
            "tunnel_bore",
            key -> EntityType.Builder.<TunnelBoreEntity>of(TunnelBoreEntity::new, MobCategory.MISC)
                    .sized(2.8F, 2.75F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<TunnelBoreItem> TUNNEL_BORE_ITEM = ITEMS.registerItem(
            "tunnel_bore",
            TunnelBoreItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<TunnelBoreMenu>> TUNNEL_BORE_MENU = MENUS.register(
            "tunnel_bore",
            () -> new MenuType<>(TunnelBoreMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Basic Locomotive Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<BasicLocomotiveEntity>> BASIC_LOCOMOTIVE_ENTITY = ENTITY_TYPES.register(
            "basic_locomotive",
            key -> EntityType.Builder.<BasicLocomotiveEntity>of(BasicLocomotiveEntity::new, MobCategory.MISC)
                    .sized(0.98F, 1.25F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<BasicLocomotiveItem> BASIC_LOCOMOTIVE_ITEM = ITEMS.registerItem(
            "basic_locomotive",
            BasicLocomotiveItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<BasicLocomotiveMenu>> BASIC_LOCOMOTIVE_MENU = MENUS.register(
            "basic_locomotive",
            () -> new MenuType<>(BasicLocomotiveMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Creative Mode Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINING_TOOLS_TAB = CREATIVE_MODE_TABS.register(
            "miningtools_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.miningtools"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> BASIC_LOCOMOTIVE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(JUNCTION_RAIL_ITEM.get());
                        output.accept(TUNNEL_BORE_ITEM.get());
                        output.accept(BASIC_LOCOMOTIVE_ITEM.get());
                    })
                    .build()
    );

    public MiningTools(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mining Tools common setup initialized");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(JUNCTION_RAIL_ITEM);
            event.accept(TUNNEL_BORE_ITEM);
            event.accept(BASIC_LOCOMOTIVE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Mining Tools server starting");
    }
}
