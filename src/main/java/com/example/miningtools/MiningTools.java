package com.example.miningtools;

import org.slf4j.Logger;

import com.example.miningtools.block.BufferStopBlock;
import com.example.miningtools.block.SurveyorsChargeBlock;
import com.example.miningtools.item.RemoteDetonatorItem;
import com.example.miningtools.block.ChunkLoaderBlock;
import com.example.miningtools.block.ControlRailBlock;
import com.example.miningtools.block.BrakingRailBlock;
import com.example.miningtools.block.CargoTransferBlock;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.network.CargoTransferFilterPayload;
import com.example.miningtools.network.CargoTransferSyncPayload;
import com.example.miningtools.network.FluidLoaderFilterPayload;
import com.example.miningtools.network.FluidLoaderSyncPayload;
import com.example.miningtools.network.CartRenameOpenPayload;
import com.example.miningtools.network.CartRenamePayload;
import com.example.miningtools.block.ElectricRailBlock;
import com.example.miningtools.block.EntityLoaderBlock;
import com.example.miningtools.block.EntityUnloaderBlock;
import com.example.miningtools.block.FluidLoaderBlock;
import com.example.miningtools.block.HornRailBlock;
import com.example.miningtools.block.JunctionRailBlock;
import com.example.miningtools.block.TrackSwitchBlock;
import com.example.miningtools.block.TurnoutRailBlock;
import com.example.miningtools.block.WyeRailBlock;
import com.example.miningtools.block.entity.ChunkLoaderBlockEntity;
import com.example.miningtools.block.entity.ElectricRailBlockEntity;
import com.example.miningtools.block.entity.EntityLoaderBlockEntity;
import com.example.miningtools.block.entity.EntityUnloaderBlockEntity;
import com.example.miningtools.block.entity.FluidLoaderBlockEntity;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity;
import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.entity.CabooseMinecartEntity;
import com.example.miningtools.entity.AggregateHopperCartEntity;
import com.example.miningtools.entity.CargoMinecartEntity;
import com.example.miningtools.item.AggregateHopperCartItem;
import com.example.miningtools.menu.AggregateHopperCartMenu;
import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.entity.LargeLiquidMinecartEntity;
import com.example.miningtools.entity.LiquidMinecartEntity;
import com.example.miningtools.entity.LivestockCartEntity;
import com.example.miningtools.entity.TunnelBoreEntity;
import com.example.miningtools.entity.GeneratorMinecartEntity;
import com.example.miningtools.item.GeneratorMinecartItem;
import com.example.miningtools.menu.GeneratorMinecartMenu;
import com.example.miningtools.entity.TurretCartEntity;
import com.example.miningtools.item.TurretCartItem;
import com.example.miningtools.menu.TurretCartMenu;
import com.example.miningtools.item.BasicLocomotiveItem;
import com.example.miningtools.item.CabooseItem;
import com.example.miningtools.item.CargoMinecartItem;
import com.example.miningtools.item.CrowbarItem;
import com.example.miningtools.item.DieselLocomotiveItem;
import com.example.miningtools.item.ElectricLocomotiveItem;
import com.example.miningtools.item.LargeLiquidMinecartItem;
import com.example.miningtools.item.LiquidMinecartItem;
import com.example.miningtools.item.LivestockCartItem;
import com.example.miningtools.item.TunnelBoreItem;
import com.example.miningtools.item.TrackPaverItem;
import com.example.miningtools.entity.TrackPaverEntity;
import com.example.miningtools.menu.TrackPaverMenu;
import com.example.miningtools.menu.BasicLocomotiveMenu;
import com.example.miningtools.menu.CargoMinecartMenu;
import com.example.miningtools.menu.DieselLocomotiveMenu;
import com.example.miningtools.menu.ElectricLocomotiveMenu;
import com.example.miningtools.menu.FluidLoaderMenu;
import com.example.miningtools.menu.TunnelBoreMenu;
import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.network.LocomotiveRenamePayload;
import com.example.miningtools.network.TrackSwitchSyncPayload;
import com.example.miningtools.network.TrackSwitchUpdatePayload;
import com.example.miningtools.network.ChunkLoaderSyncPayload;
import com.example.miningtools.network.ChunkLoaderUpdatePayload;
import com.example.miningtools.network.EntityTransferSyncPayload;
import com.example.miningtools.network.EntityTransferUpdatePayload;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.example.miningtools.network.LocomotiveControlPayload;
import com.example.miningtools.network.TrainLinkSyncPayload;
import com.example.miningtools.util.TrainChainingUtil;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(MiningTools.MODID)
public class MiningTools {
    public static final String MODID = "miningtools";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LOCOMOTIVE_WHISTLE = SOUND_EVENTS.register(
            "locomotive_whistle",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "locomotive_whistle"))
    );

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

    // Control Rail Block & Item
    public static final DeferredBlock<ControlRailBlock> CONTROL_RAIL = BLOCKS.registerBlock(
            "control_rail",
            ControlRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> CONTROL_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "control_rail",
            CONTROL_RAIL
    );

    // Braking / Holding Rail Block & Item
    public static final DeferredBlock<BrakingRailBlock> BRAKING_RAIL = BLOCKS.registerBlock(
            "braking_rail",
            BrakingRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> BRAKING_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "braking_rail",
            BRAKING_RAIL
    );

    // Horn Rail Block & Item
    public static final DeferredBlock<HornRailBlock> HORN_RAIL = BLOCKS.registerBlock(
            "horn_rail",
            HornRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> HORN_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "horn_rail",
            HORN_RAIL
    );


    // Turnout Rail Block & Item
    public static final DeferredBlock<TurnoutRailBlock> TURNOUT_RAIL = BLOCKS.registerBlock(
            "turnout_rail",
            TurnoutRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> TURNOUT_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "turnout_rail",
            TURNOUT_RAIL
    );

    // Wye Rail Block & Item
    public static final DeferredBlock<WyeRailBlock> WYE_RAIL = BLOCKS.registerBlock(
            "wye_rail",
            WyeRailBlock::new,
            p -> p.noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> WYE_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "wye_rail",
            WYE_RAIL
    );

    // Track Switch & Signal Block, Item & BlockEntity
    public static final DeferredBlock<TrackSwitchBlock> TRACK_SWITCH = BLOCKS.registerBlock(
            "track_switch",
            TrackSwitchBlock::new,
            p -> p.strength(1.5F).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 7)
    );
    public static final DeferredItem<BlockItem> TRACK_SWITCH_ITEM = ITEMS.registerSimpleBlockItem(
            "track_switch",
            TRACK_SWITCH
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrackSwitchBlockEntity>> TRACK_SWITCH_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "track_switch",
            () -> new BlockEntityType<>(TrackSwitchBlockEntity::new, TRACK_SWITCH.get())
    );

    // Fluid Loader, Fluid Unloader & Diesel Locomotive Fueler Blocks, Items, BlockEntity & Menu
    public static final DeferredBlock<FluidLoaderBlock> FLUID_LOADER = BLOCKS.registerBlock(
            "fluid_loader",
            p -> new FluidLoaderBlock(FluidLoaderBlock.ROLE_LOADER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> FLUID_LOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "fluid_loader",
            FLUID_LOADER
    );
    public static final DeferredBlock<FluidLoaderBlock> FLUID_UNLOADER = BLOCKS.registerBlock(
            "fluid_unloader",
            p -> new FluidLoaderBlock(FluidLoaderBlock.ROLE_UNLOADER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> FLUID_UNLOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "fluid_unloader",
            FLUID_UNLOADER
    );
    public static final DeferredBlock<FluidLoaderBlock> DIESEL_FUELER = BLOCKS.registerBlock(
            "diesel_fueler",
            p -> new FluidLoaderBlock(FluidLoaderBlock.ROLE_DIESEL_FUELER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> DIESEL_FUELER_ITEM = ITEMS.registerSimpleBlockItem(
            "diesel_fueler",
            DIESEL_FUELER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidLoaderBlockEntity>> FLUID_LOADER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "fluid_loader",
            () -> new BlockEntityType<>(FluidLoaderBlockEntity::new, FLUID_LOADER.get(), FLUID_UNLOADER.get(), DIESEL_FUELER.get())
    );
    public static final DeferredHolder<MenuType<?>, MenuType<FluidLoaderMenu>> FLUID_LOADER_MENU = MENUS.register(
            "fluid_loader",
            () -> new MenuType<>(FluidLoaderMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Cargo Loader, Cargo Unloader & Locomotive Coaling Station Blocks, Items, BlockEntity & Menu
    public static final DeferredBlock<CargoTransferBlock> CARGO_LOADER = BLOCKS.registerBlock(
            "cargo_loader",
            p -> new CargoTransferBlock(CargoTransferBlock.ROLE_LOADER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> CARGO_LOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "cargo_loader",
            CARGO_LOADER
    );
    public static final DeferredBlock<CargoTransferBlock> CARGO_UNLOADER = BLOCKS.registerBlock(
            "cargo_unloader",
            p -> new CargoTransferBlock(CargoTransferBlock.ROLE_UNLOADER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> CARGO_UNLOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "cargo_unloader",
            CARGO_UNLOADER
    );
    public static final DeferredBlock<CargoTransferBlock> COAL_FUELER = BLOCKS.registerBlock(
            "coal_fueler",
            p -> new CargoTransferBlock(CargoTransferBlock.ROLE_COAL_FUELER, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> COAL_FUELER_ITEM = ITEMS.registerSimpleBlockItem(
            "coal_fueler",
            COAL_FUELER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoTransferBlockEntity>> CARGO_TRANSFER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "cargo_transfer",
            () -> new BlockEntityType<>(CargoTransferBlockEntity::new, CARGO_LOADER.get(), CARGO_UNLOADER.get(), COAL_FUELER.get())
    );
    public static final DeferredHolder<MenuType<?>, MenuType<CargoTransferMenu>> CARGO_TRANSFER_MENU = MENUS.register(
            "cargo_transfer",
            () -> new MenuType<>(CargoTransferMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Entity Loader Block, Item & BlockEntity
    public static final DeferredBlock<EntityLoaderBlock> ENTITY_LOADER = BLOCKS.registerBlock(
            "entity_loader",
            EntityLoaderBlock::new,
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> ENTITY_LOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "entity_loader",
            ENTITY_LOADER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityLoaderBlockEntity>> ENTITY_LOADER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "entity_loader",
            () -> new BlockEntityType<>(EntityLoaderBlockEntity::new, ENTITY_LOADER.get())
    );

    // Entity Unloader Block, Item & BlockEntity
    public static final DeferredBlock<EntityUnloaderBlock> ENTITY_UNLOADER = BLOCKS.registerBlock(
            "entity_unloader",
            EntityUnloaderBlock::new,
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> ENTITY_UNLOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "entity_unloader",
            ENTITY_UNLOADER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityUnloaderBlockEntity>> ENTITY_UNLOADER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "entity_unloader",
            () -> new BlockEntityType<>(EntityUnloaderBlockEntity::new, ENTITY_UNLOADER.get())
    );

    // Chunk Loader Block, Item & BlockEntity
    public static final DeferredBlock<ChunkLoaderBlock> CHUNK_LOADER = BLOCKS.registerBlock(
            "chunk_loader",
            ChunkLoaderBlock::new,
            p -> p.strength(3.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(ChunkLoaderBlock.ACTIVE) ? 12 : 0)
    );
    public static final DeferredItem<BlockItem> CHUNK_LOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "chunk_loader",
            CHUNK_LOADER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "chunk_loader",
            () -> new BlockEntityType<>(ChunkLoaderBlockEntity::new, CHUNK_LOADER.get())
    );

    // Buffer Stop (Stop Block) Block & Item
    public static final DeferredBlock<BufferStopBlock> BUFFER_STOP = BLOCKS.registerBlock(
            "buffer_stop",
            BufferStopBlock::new,
            p -> p.strength(3.5F).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
    );
    public static final DeferredItem<BlockItem> BUFFER_STOP_ITEM = ITEMS.registerSimpleBlockItem(
            "buffer_stop",
            BUFFER_STOP
    );

    // Surveyor's Charge Block & Item
    public static final DeferredBlock<SurveyorsChargeBlock> SURVEYORS_CHARGE = BLOCKS.registerBlock(
            "surveyors_charge",
            SurveyorsChargeBlock::new,
            p -> p.strength(0.5F).sound(SoundType.WOOD)
    );
    public static final DeferredItem<BlockItem> SURVEYORS_CHARGE_ITEM = ITEMS.registerSimpleBlockItem(
            "surveyors_charge",
            SURVEYORS_CHARGE
    );

    // Remote Detonator Item
    public static final DeferredItem<RemoteDetonatorItem> REMOTE_DETONATOR = ITEMS.registerItem(
            "remote_detonator",
            RemoteDetonatorItem::new,
            p -> p.stacksTo(1)
    );

    // Tunnel Bore Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<TunnelBoreEntity>> TUNNEL_BORE_ENTITY = ENTITY_TYPES.register(
            "tunnel_bore",
            key -> EntityType.Builder.<TunnelBoreEntity>of(TunnelBoreEntity::new, MobCategory.MISC)
                    .sized(2.8F, 2.75F)
                    .passengerAttachments(2.05F)
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

    // Track and Road Paver Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<TrackPaverEntity>> TRACK_PAVER_ENTITY = ENTITY_TYPES.register(
            "track_paver",
            key -> EntityType.Builder.<TrackPaverEntity>of(TrackPaverEntity::new, MobCategory.MISC)
                    .sized(2.8F, 2.0F)
                    .passengerAttachments(1.35F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<TrackPaverItem> TRACK_PAVER_ITEM = ITEMS.registerItem(
            "track_paver",
            TrackPaverItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<TrackPaverMenu>> TRACK_PAVER_MENU = MENUS.register(
            "track_paver",
            () -> new MenuType<>(TrackPaverMenu::new, FeatureFlags.DEFAULT_FLAGS)
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

    // Diesel Locomotive Entity, Item & Menu (Canadian National #3087)
    public static final DeferredHolder<EntityType<?>, EntityType<DieselLocomotiveEntity>> DIESEL_LOCOMOTIVE_ENTITY = ENTITY_TYPES.register(
            "diesel_locomotive",
            key -> EntityType.Builder.<DieselLocomotiveEntity>of(DieselLocomotiveEntity::new, MobCategory.MISC)
                    .sized(0.98F, 1.35F)
                    .passengerAttachments(0.25F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<DieselLocomotiveItem> DIESEL_LOCOMOTIVE_ITEM = ITEMS.registerItem(
            "diesel_locomotive",
            DieselLocomotiveItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<DieselLocomotiveMenu>> DIESEL_LOCOMOTIVE_MENU = MENUS.register(
            "diesel_locomotive",
            () -> new MenuType<>(DieselLocomotiveMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Cargo Minecart Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<CargoMinecartEntity>> CARGO_MINECART_ENTITY = ENTITY_TYPES.register(
            "cargo_minecart",
            key -> EntityType.Builder.<CargoMinecartEntity>of(CargoMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<CargoMinecartItem> CARGO_MINECART_ITEM = ITEMS.registerItem(
            "cargo_minecart",
            CargoMinecartItem::new,
            p -> p.stacksTo(1)
    );

    // Livestock Cart Entity & Item
    public static final DeferredHolder<EntityType<?>, EntityType<LivestockCartEntity>> LIVESTOCK_CART_ENTITY = ENTITY_TYPES.register(
            "livestock_cart",
            key -> EntityType.Builder.<LivestockCartEntity>of(LivestockCartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 1.1F)
                    .passengerAttachments(0.22F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<LivestockCartItem> LIVESTOCK_CART_ITEM = ITEMS.registerItem(
            "livestock_cart",
            LivestockCartItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<CargoMinecartMenu>> CARGO_MINECART_MENU = MENUS.register(
            "cargo_minecart",
            () -> new MenuType<>(CargoMinecartMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Aggregate Hopper Cart Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<AggregateHopperCartEntity>> HOPPER_CART_ENTITY = ENTITY_TYPES.register(
            "hopper_cart",
            key -> EntityType.Builder.<AggregateHopperCartEntity>of(AggregateHopperCartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.85F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<AggregateHopperCartItem> HOPPER_CART_ITEM = ITEMS.registerItem(
            "hopper_cart",
            AggregateHopperCartItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<AggregateHopperCartMenu>> HOPPER_CART_MENU = MENUS.register(
            "hopper_cart",
            () -> new MenuType<>(AggregateHopperCartMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Liquid Minecart Entity & Item
    public static final DeferredHolder<EntityType<?>, EntityType<LiquidMinecartEntity>> LIQUID_MINECART_ENTITY = ENTITY_TYPES.register(
            "liquid_minecart",
            key -> EntityType.Builder.<LiquidMinecartEntity>of(LiquidMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.75F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<LiquidMinecartItem> LIQUID_MINECART_ITEM = ITEMS.registerItem(
            "liquid_minecart",
            LiquidMinecartItem::new,
            p -> p.stacksTo(1)
    );

    // Large Liquid Minecart Entity & Item (16,000 mB / 16 Buckets)
    public static final DeferredHolder<EntityType<?>, EntityType<LargeLiquidMinecartEntity>> LARGE_LIQUID_MINECART_ENTITY = ENTITY_TYPES.register(
            "large_liquid_minecart",
            key -> EntityType.Builder.<LargeLiquidMinecartEntity>of(LargeLiquidMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.75F)
                    .passengerAttachments(0.1875F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<LargeLiquidMinecartItem> LARGE_LIQUID_MINECART_ITEM = ITEMS.registerItem(
            "large_liquid_minecart",
            LargeLiquidMinecartItem::new,
            p -> p.stacksTo(1)
    );

    // Arrow Turret Cart Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<TurretCartEntity>> TURRET_CART_ENTITY = ENTITY_TYPES.register(
            "turret_cart",
            key -> EntityType.Builder.<TurretCartEntity>of(TurretCartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .passengerAttachments(0.35F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<TurretCartItem> TURRET_CART_ITEM = ITEMS.registerItem(
            "turret_cart",
            TurretCartItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<TurretCartMenu>> TURRET_CART_MENU = MENUS.register(
            "turret_cart",
            () -> new MenuType<>(TurretCartMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Steam Dynamo Generator Cart (Water + Coal -> RF Power)
    public static final DeferredHolder<EntityType<?>, EntityType<GeneratorMinecartEntity>> GENERATOR_MINECART_ENTITY = ENTITY_TYPES.register(
            "generator_cart",
            key -> EntityType.Builder.<GeneratorMinecartEntity>of(GeneratorMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .passengerAttachments(0.35F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<GeneratorMinecartItem> GENERATOR_MINECART_ITEM = ITEMS.registerItem(
            "generator_cart",
            GeneratorMinecartItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMinecartMenu>> GENERATOR_MINECART_MENU = MENUS.register(
            "generator_cart",
            () -> new MenuType<>(GeneratorMinecartMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Caboose Minecart Entity & Item (Train Chunk Anchor)
    public static final DeferredHolder<EntityType<?>, EntityType<CabooseMinecartEntity>> CABOOSE_ENTITY = ENTITY_TYPES.register(
            "caboose",
            key -> EntityType.Builder.<CabooseMinecartEntity>of(CabooseMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 1.2F)
                    .passengerAttachments(0.35F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<CabooseItem> CABOOSE_ITEM = ITEMS.registerItem(
            "caboose",
            CabooseItem::new,
            p -> p.stacksTo(1)
    );

    // Electric Rail Block, Item & BlockEntity
    public static final DeferredBlock<ElectricRailBlock> ELECTRIC_RAIL = BLOCKS.registerBlock(
            "electric_rail",
            ElectricRailBlock::new,
            p -> p.noCollision().strength(0.8F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> ELECTRIC_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "electric_rail",
            ELECTRIC_RAIL
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricRailBlockEntity>> ELECTRIC_RAIL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "electric_rail",
            () -> new BlockEntityType<>(ElectricRailBlockEntity::new, ELECTRIC_RAIL.get())
    );

    // Electric Bullet Locomotive Entity, Item & Menu
    public static final DeferredHolder<EntityType<?>, EntityType<ElectricLocomotiveEntity>> ELECTRIC_LOCOMOTIVE_ENTITY = ENTITY_TYPES.register(
            "electric_locomotive",
            key -> EntityType.Builder.<ElectricLocomotiveEntity>of(ElectricLocomotiveEntity::new, MobCategory.MISC)
                    .sized(0.98F, 1.25F)
                    .passengerAttachments(2.05F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
    );

    public static final DeferredItem<ElectricLocomotiveItem> ELECTRIC_LOCOMOTIVE_ITEM = ITEMS.registerItem(
            "electric_locomotive",
            ElectricLocomotiveItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredItem<CrowbarItem> CROWBAR_ITEM = ITEMS.registerItem(
            "crowbar",
            CrowbarItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricLocomotiveMenu>> ELECTRIC_LOCOMOTIVE_MENU = MENUS.register(
            "electric_locomotive",
            () -> new MenuType<>(ElectricLocomotiveMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Creative Mode Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINING_TOOLS_TAB = CREATIVE_MODE_TABS.register(
            "miningtools_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.miningtools"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> BASIC_LOCOMOTIVE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CROWBAR_ITEM.get());
                        output.accept(JUNCTION_RAIL_ITEM.get());
                        output.accept(CONTROL_RAIL_ITEM.get());
                        output.accept(BRAKING_RAIL_ITEM.get());
                        output.accept(HORN_RAIL_ITEM.get());
                        output.accept(TURNOUT_RAIL_ITEM.get());
                        output.accept(WYE_RAIL_ITEM.get());
                        output.accept(TRACK_SWITCH_ITEM.get());
                        output.accept(CARGO_LOADER_ITEM.get());
                        output.accept(CARGO_UNLOADER_ITEM.get());
                        output.accept(FLUID_LOADER_ITEM.get());
                        output.accept(FLUID_UNLOADER_ITEM.get());
                        output.accept(COAL_FUELER_ITEM.get());
                        output.accept(DIESEL_FUELER_ITEM.get());
                        output.accept(ENTITY_LOADER_ITEM.get());
                        output.accept(ENTITY_UNLOADER_ITEM.get());
                        output.accept(CHUNK_LOADER_ITEM.get());
                        output.accept(BUFFER_STOP_ITEM.get());
                        output.accept(TUNNEL_BORE_ITEM.get());
                        output.accept(TRACK_PAVER_ITEM.get());
                        output.accept(BASIC_LOCOMOTIVE_ITEM.get());
                        output.accept(DIESEL_LOCOMOTIVE_ITEM.get());
                        output.accept(ELECTRIC_LOCOMOTIVE_ITEM.get());
                        output.accept(CARGO_MINECART_ITEM.get());
                        output.accept(HOPPER_CART_ITEM.get());
                        output.accept(LIVESTOCK_CART_ITEM.get());
                        output.accept(LIQUID_MINECART_ITEM.get());
                        output.accept(LARGE_LIQUID_MINECART_ITEM.get());
                        output.accept(CABOOSE_ITEM.get());
                        output.accept(TURRET_CART_ITEM.get());
                        output.accept(GENERATOR_MINECART_ITEM.get());
                        output.accept(ELECTRIC_RAIL_ITEM.get());
                        output.accept(SURVEYORS_CHARGE_ITEM.get());
                        output.accept(REMOTE_DETONATOR.get());
                    })
                    .build()
    );

    public MiningTools(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                FLUID_LOADER_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler(side)
        );
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                LIQUID_MINECART_ENTITY.get(),
                (entity, side) -> entity.getFluidHandler()
        );
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                LARGE_LIQUID_MINECART_ENTITY.get(),
                (entity, side) -> entity.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ELECTRIC_RAIL_BLOCK_ENTITY.get(),
                (be, side) -> be.getEnergyHandler(side)
        );
        event.registerEntity(
                Capabilities.Energy.ENTITY,
                ELECTRIC_LOCOMOTIVE_ENTITY.get(),
                (entity, side) -> entity.getEnergyHandler(side)
        );
        event.registerEntity(
                Capabilities.Fluid.ENTITY,
                GENERATOR_MINECART_ENTITY.get(),
                (entity, side) -> entity.getFluidHandler()
        );
        event.registerEntity(
                Capabilities.Energy.ENTITY,
                GENERATOR_MINECART_ENTITY.get(),
                (entity, side) -> entity.getEnergyHandler(side)
        );
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                LocomotiveControlPayload.TYPE,
                LocomotiveControlPayload.STREAM_CODEC,
                LocomotiveControlPayload::handle
        );
        registrar.playToServer(
                LocomotiveRenamePayload.TYPE,
                LocomotiveRenamePayload.STREAM_CODEC,
                LocomotiveRenamePayload::handle
        );
        registrar.playToServer(
                TrackSwitchUpdatePayload.TYPE,
                TrackSwitchUpdatePayload.STREAM_CODEC,
                TrackSwitchUpdatePayload::handle
        );
        registrar.playToClient(
                TrackSwitchSyncPayload.TYPE,
                TrackSwitchSyncPayload.STREAM_CODEC,
                TrackSwitchSyncPayload::handle
        );
        registrar.playToClient(
                TrainLinkSyncPayload.TYPE,
                TrainLinkSyncPayload.STREAM_CODEC,
                TrainLinkSyncPayload::handle
        );
        registrar.playToClient(
                EntityTransferSyncPayload.TYPE,
                EntityTransferSyncPayload.STREAM_CODEC,
                EntityTransferSyncPayload::handle
        );
        registrar.playToServer(
                EntityTransferUpdatePayload.TYPE,
                EntityTransferUpdatePayload.STREAM_CODEC,
                EntityTransferUpdatePayload::handle
        );


        registrar.playToClient(
                ChunkLoaderSyncPayload.TYPE,
                ChunkLoaderSyncPayload.STREAM_CODEC,
                ChunkLoaderSyncPayload::handle
        );
        registrar.playToServer(
                ChunkLoaderUpdatePayload.TYPE,
                ChunkLoaderUpdatePayload.STREAM_CODEC,
                ChunkLoaderUpdatePayload::handle
        );
        registrar.playToServer(
                CartRenamePayload.TYPE,
                CartRenamePayload.STREAM_CODEC,
                CartRenamePayload::handle
        );
        registrar.playToClient(
                CartRenameOpenPayload.TYPE,
                CartRenameOpenPayload.STREAM_CODEC,
                CartRenameOpenPayload::handle
        );
        registrar.playToServer(
                CargoTransferFilterPayload.TYPE,
                CargoTransferFilterPayload.STREAM_CODEC,
                CargoTransferFilterPayload::handle
        );
        registrar.playToClient(
                CargoTransferSyncPayload.TYPE,
                CargoTransferSyncPayload.STREAM_CODEC,
                CargoTransferSyncPayload::handle
        );
        registrar.playToServer(
                FluidLoaderFilterPayload.TYPE,
                FluidLoaderFilterPayload.STREAM_CODEC,
                FluidLoaderFilterPayload::handle
        );
        registrar.playToClient(
                FluidLoaderSyncPayload.TYPE,
                FluidLoaderSyncPayload.STREAM_CODEC,
                FluidLoaderSyncPayload::handle
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mining Tools common setup initialized");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CROWBAR_ITEM);
            event.accept(JUNCTION_RAIL_ITEM);
            event.accept(CONTROL_RAIL_ITEM);
            event.accept(BRAKING_RAIL_ITEM);
            event.accept(HORN_RAIL_ITEM);
            event.accept(TURNOUT_RAIL_ITEM);
            event.accept(WYE_RAIL_ITEM);
            event.accept(TRACK_SWITCH_ITEM);
            event.accept(CARGO_LOADER_ITEM);
            event.accept(CARGO_UNLOADER_ITEM);
            event.accept(FLUID_LOADER_ITEM);
            event.accept(FLUID_UNLOADER_ITEM);
            event.accept(COAL_FUELER_ITEM);
            event.accept(DIESEL_FUELER_ITEM);
            event.accept(ENTITY_LOADER_ITEM);
            event.accept(ENTITY_UNLOADER_ITEM);
            event.accept(CHUNK_LOADER_ITEM);
            event.accept(BUFFER_STOP_ITEM);
            event.accept(TUNNEL_BORE_ITEM);
            event.accept(TRACK_PAVER_ITEM);
            event.accept(BASIC_LOCOMOTIVE_ITEM);
            event.accept(ELECTRIC_LOCOMOTIVE_ITEM);
            event.accept(CARGO_MINECART_ITEM);
            event.accept(HOPPER_CART_ITEM);
            event.accept(LIVESTOCK_CART_ITEM);
            event.accept(LIQUID_MINECART_ITEM);
            event.accept(LARGE_LIQUID_MINECART_ITEM);
            event.accept(CABOOSE_ITEM);
            event.accept(TURRET_CART_ITEM);
            event.accept(GENERATOR_MINECART_ITEM);
            event.accept(ELECTRIC_RAIL_ITEM);
            event.accept(SURVEYORS_CHARGE_ITEM);
            event.accept(REMOTE_DETONATOR);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(CROWBAR_ITEM.get())) {
            if (event.getTarget() instanceof AbstractMinecart cart) {
                event.setCanceled(true);
                event.setCancellationResult(CrowbarItem.handleCartInteraction(
                        event.getEntity(),
                        event.getHand(),
                        stack,
                        cart,
                        event.getLevel()
                ));
            }
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractMinecart cart) {
            UUID p = TrainChainingUtil.getCartParentUUID(cart);
            UUID c = TrainChainingUtil.getCartChildUUID(cart);
            if (p != null || c != null) {
                if (event.getEntity() instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(
                            sp,
                            new TrainLinkSyncPayload(cart.getUUID(), java.util.Optional.ofNullable(p), java.util.Optional.ofNullable(c))
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AbstractMinecart cart && event.getLevel() instanceof ServerLevel sl) {
            if (cart.getRemovalReason() != null && cart.getRemovalReason().shouldDestroy()) {
                TrainChainingUtil.unlinkCart(cart, sl);
            }
        }
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {


        LOGGER.info("Mining Tools server starting");
    }
}
