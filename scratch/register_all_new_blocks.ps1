$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Update CartRenameOpenPayload.java and CartRenameScreen.java
$openPayloadPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\network\CartRenameOpenPayload.java"
$openPayloadContent = @'
package com.example.miningtools.network;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CartRenameOpenPayload(int entityId, String currentName, String typeTitle, String statusLine) implements CustomPacketPayload {
    public static final Type<CartRenameOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cart_rename_open"));

    public static final StreamCodec<ByteBuf, CartRenameOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CartRenameOpenPayload::entityId,
            ByteBufCodecs.stringUtf8(32),
            CartRenameOpenPayload::currentName,
            ByteBufCodecs.stringUtf8(64),
            CartRenameOpenPayload::typeTitle,
            ByteBufCodecs.stringUtf8(128),
            CartRenameOpenPayload::statusLine,
            CartRenameOpenPayload::new
    );

    public static @Nullable Consumer<CartRenameOpenPayload> CLIENT_HANDLER = null;

    @Override
    public Type<CartRenameOpenPayload> type() {
        return TYPE;
    }

    public static void handle(CartRenameOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
'@
[System.IO.File]::WriteAllText($openPayloadPath, $openPayloadContent, $utf8NoBom)

$crsPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CartRenameScreen.java"
$crsContent = [System.IO.File]::ReadAllText($crsPath, $utf8NoBom)
if (-not $crsContent.Contains("open(com.example.miningtools.network.CartRenameOpenPayload")) {
    $oldOpen = @'
    public static void openForCart(AbstractMinecart cart, String statusLine) {
        Minecraft mc = Minecraft.getInstance();
        String existingName = cart.getCustomName() != null ? cart.getCustomName().getString().trim() : "";
        String typeTitle = cart.getType().getDescription().getString();
        mc.setScreen(new CartRenameScreen(cart.getId(), existingName, typeTitle, statusLine));
    }
'@
    $newOpen = @'
    public static void open(com.example.miningtools.network.CartRenameOpenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new CartRenameScreen(payload.entityId(), payload.currentName(), payload.typeTitle(), payload.statusLine()));
    }

    public static void openForCart(AbstractMinecart cart, String statusLine) {
        Minecraft mc = Minecraft.getInstance();
        String existingName = cart.getCustomName() != null ? cart.getCustomName().getString().trim() : "";
        String typeTitle = cart.getType().getDescription().getString();
        mc.setScreen(new CartRenameScreen(cart.getId(), existingName, typeTitle, statusLine));
    }
'@
    $crsContent = $crsContent.Replace($oldOpen, $newOpen)
    [System.IO.File]::WriteAllText($crsPath, $crsContent, $utf8NoBom)
}

# 2. Create CargoTransferSyncPayload.java (S2C) and update CargoTransferMenu/Block/BlockEntity/Payload/Screen
$syncPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\network\CargoTransferSyncPayload.java"
$syncContent = @'
package com.example.miningtools.network;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CargoTransferSyncPayload(
        boolean unloader,
        int loadConditionMode,
        String itemWhitelist,
        String itemBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<CargoTransferSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cargo_transfer_sync"));

    public static final StreamCodec<ByteBuf, CargoTransferSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            CargoTransferSyncPayload::unloader,
            ByteBufCodecs.VAR_INT,
            CargoTransferSyncPayload::loadConditionMode,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::itemWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::itemBlacklist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferSyncPayload::cartBlacklist,
            CargoTransferSyncPayload::new
    );

    public static @Nullable Consumer<CargoTransferSyncPayload> CLIENT_HANDLER = null;

    @Override
    public Type<CargoTransferSyncPayload> type() {
        return TYPE;
    }

    public static void handle(CargoTransferSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (CLIENT_HANDLER != null) {
                CLIENT_HANDLER.accept(payload);
            }
        });
    }
}
'@
[System.IO.File]::WriteAllText($syncPath, $syncContent, $utf8NoBom)

# Update CargoTransferBlock.java useWithoutItem
$blockPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\CargoTransferBlock.java"
$blockContent = [System.IO.File]::ReadAllText($blockPath, $utf8NoBom)
$oldUse = @'
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(transferBe, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeBoolean(transferBe.isUnloader());
                    buf.writeVarInt(transferBe.getLoadConditionMode());
                    buf.writeUtf(transferBe.getItemWhitelist(), 128);
                    buf.writeUtf(transferBe.getItemBlacklist(), 128);
                    buf.writeUtf(transferBe.getCartWhitelist(), 128);
                    buf.writeUtf(transferBe.getCartBlacklist(), 128);
                });
            }
        }
        return InteractionResult.SUCCESS;
    }
'@
$newUse = @'
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoTransferBlockEntity transferBe && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(transferBe);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new com.example.miningtools.network.CargoTransferSyncPayload(
                                transferBe.isUnloader(),
                                transferBe.getLoadConditionMode(),
                                transferBe.getItemWhitelist(),
                                transferBe.getItemBlacklist(),
                                transferBe.getCartWhitelist(),
                                transferBe.getCartBlacklist()
                        )
                );
            }
        }
        return InteractionResult.SUCCESS;
    }
'@
$blockContent = $blockContent.Replace($oldUse, $newUse)
[System.IO.File]::WriteAllText($blockPath, $blockContent, $utf8NoBom)

# Update CargoTransferBlockEntity.java createMenu & dataAccess
$bePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\block\entity\CargoTransferBlockEntity.java"
$beContent = [System.IO.File]::ReadAllText($bePath, $utf8NoBom)
$oldCreateMenu = @'
    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CargoTransferMenu(
                containerId,
                inventory,
                this,
                this.dataAccess,
                this.getBlockPos(),
                this.isUnloader(),
                this.loadConditionMode,
                this.itemWhitelist,
                this.itemBlacklist,
                this.cartWhitelist,
                this.cartBlacklist
        );
    }
'@
$newCreateMenu = @'
    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CargoTransferMenu(containerId, inventory, this, this.dataAccess);
    }
'@
$beContent = $beContent.Replace($oldCreateMenu, $newCreateMenu)

$oldDataAccess = @'
            return switch (index) {
                case 0 -> CargoTransferBlockEntity.this.loadConditionMode;
                case 1 -> CargoTransferBlockEntity.this.cartDetected ? 1 : 0;
                case 2 -> CargoTransferBlockEntity.this.holdingCart ? 1 : 0;
                case 3 -> CargoTransferBlockEntity.this.cartFillPct;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CargoTransferBlockEntity.this.setLoadConditionMode(value);
                case 1 -> CargoTransferBlockEntity.this.cartDetected = (value != 0);
                case 2 -> CargoTransferBlockEntity.this.holdingCart = (value != 0);
                case 3 -> CargoTransferBlockEntity.this.cartFillPct = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
'@
$newDataAccess = @'
            return switch (index) {
                case 0 -> CargoTransferBlockEntity.this.loadConditionMode;
                case 1 -> CargoTransferBlockEntity.this.cartDetected ? 1 : 0;
                case 2 -> CargoTransferBlockEntity.this.holdingCart ? 1 : 0;
                case 3 -> CargoTransferBlockEntity.this.cartFillPct;
                case 4 -> CargoTransferBlockEntity.this.isUnloader() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CargoTransferBlockEntity.this.setLoadConditionMode(value);
                case 1 -> CargoTransferBlockEntity.this.cartDetected = (value != 0);
                case 2 -> CargoTransferBlockEntity.this.holdingCart = (value != 0);
                case 3 -> CargoTransferBlockEntity.this.cartFillPct = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
'@
$beContent = $beContent.Replace($oldDataAccess, $newDataAccess)
[System.IO.File]::WriteAllText($bePath, $beContent, $utf8NoBom)

# Update CargoTransferMenu.java
$menuPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\menu\CargoTransferMenu.java"
$menuContent = @'
package com.example.miningtools.menu;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CargoTransferMenu extends AbstractContainerMenu {
    public static final int BUFFER_SIZE = CargoTransferBlockEntity.BUFFER_SIZE; // 9
    public static final int DATA_COUNT = 5;

    private final Container container;
    private final ContainerData data;

    // Client constructor
    public CargoTransferMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BUFFER_SIZE), new SimpleContainerData(DATA_COUNT));
    }

    // Server constructor
    public CargoTransferMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MiningTools.CARGO_TRANSFER_MENU.get(), containerId);
        checkContainerSize(container, BUFFER_SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);

        // 9 Buffer slots in a single row at y = 22
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(container, col, 8 + col * 18, 22));
        }

        // Player Inventory (3 rows at y = 140, hotbar at y = 198)
        this.addStandardInventorySlots(playerInventory, 8, 140);

        this.addDataSlots(data);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getLoadConditionMode() {
        return this.data.get(0);
    }

    public void setClientLoadConditionMode(int mode) {
        this.data.set(0, mode);
    }

    public boolean isCartDetected() {
        return this.data.get(1) != 0;
    }

    public boolean isHoldingCart() {
        return this.data.get(2) != 0;
    }

    public int getCartFillPct() {
        return this.data.get(3);
    }

    public boolean isUnloader() {
        return this.data.get(4) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < BUFFER_SIZE) {
                if (!this.moveItemStackTo(stackInSlot, BUFFER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, BUFFER_SIZE, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
'@
[System.IO.File]::WriteAllText($menuPath, $menuContent, $utf8NoBom)

# Update CargoTransferFilterPayload.java
$payloadPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\network\CargoTransferFilterPayload.java"
$payloadContent = @'
package com.example.miningtools.network;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CargoTransferFilterPayload(
        int loadConditionMode,
        String itemWhitelist,
        String itemBlacklist,
        String cartWhitelist,
        String cartBlacklist
) implements CustomPacketPayload {

    public static final Type<CargoTransferFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cargo_transfer_filter"));

    public static final StreamCodec<ByteBuf, CargoTransferFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CargoTransferFilterPayload::loadConditionMode,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::itemBlacklist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartWhitelist,
            ByteBufCodecs.stringUtf8(128),
            CargoTransferFilterPayload::cartBlacklist,
            CargoTransferFilterPayload::new
    );

    @Override
    public Type<CargoTransferFilterPayload> type() {
        return TYPE;
    }

    public static void handle(CargoTransferFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof CargoTransferMenu menu
                    && menu.getContainer() instanceof CargoTransferBlockEntity transferBe
                    && menu.stillValid(player)) {
                transferBe.setLoadConditionMode(payload.loadConditionMode());
                transferBe.setItemWhitelist(payload.itemWhitelist());
                transferBe.setItemBlacklist(payload.itemBlacklist());
                transferBe.setCartWhitelist(payload.cartWhitelist());
                transferBe.setCartBlacklist(payload.cartBlacklist());
            }
        });
    }
}
'@
[System.IO.File]::WriteAllText($payloadPath, $payloadContent, $utf8NoBom)

# Update CargoTransferScreen.java
$screenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CargoTransferScreen.java"
$screenContent = @'
package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.network.CargoTransferFilterPayload;
import com.example.miningtools.network.CargoTransferSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class CargoTransferScreen extends AbstractContainerScreen<CargoTransferMenu> {

    private static String cachedItemWhite = "";
    private static String cachedItemBlack = "";
    private static String cachedCartWhite = "";
    private static String cachedCartBlack = "";

    private int conditionMode;
    private boolean suppressResponder = false;
    private @Nullable Button conditionButton;
    private @Nullable EditBox itemWhiteBox;
    private @Nullable EditBox itemBlackBox;
    private @Nullable EditBox cartWhiteBox;
    private @Nullable EditBox cartBlackBox;

    public CargoTransferScreen(CargoTransferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 6;
        this.inventoryLabelY = 129;
        this.conditionMode = menu.getLoadConditionMode();
    }

    public static void onSyncPayload(CargoTransferSyncPayload payload) {
        cachedItemWhite = payload.itemWhitelist() != null ? payload.itemWhitelist() : "";
        cachedItemBlack = payload.itemBlacklist() != null ? payload.itemBlacklist() : "";
        cachedCartWhite = payload.cartWhitelist() != null ? payload.cartWhitelist() : "";
        cachedCartBlack = payload.cartBlacklist() != null ? payload.cartBlacklist() : "";

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof CargoTransferScreen screen) {
            screen.applySyncedFilters(payload.loadConditionMode(), cachedItemWhite, cachedItemBlack, cachedCartWhite, cachedCartBlack);
        }
    }

    public void applySyncedFilters(int mode, String iw, String ib, String cw, String cb) {
        this.suppressResponder = true;
        this.conditionMode = mode;
        if (this.conditionButton != null) {
            this.conditionButton.setMessage(this.getConditionButtonLabel());
        }
        if (this.itemWhiteBox != null) this.itemWhiteBox.setValue(iw);
        if (this.itemBlackBox != null) this.itemBlackBox.setValue(ib);
        if (this.cartWhiteBox != null) this.cartWhiteBox.setValue(cw);
        if (this.cartBlackBox != null) this.cartBlackBox.setValue(cb);
        this.suppressResponder = false;
    }

    @Override
    protected void init() {
        super.init();
        int xo = this.leftPos;
        int yo = this.topPos;

        this.conditionMode = this.menu.getLoadConditionMode();

        this.conditionButton = this.addRenderableWidget(
                Button.builder(this.getConditionButtonLabel(), btn -> {
                    this.conditionMode = (this.conditionMode + 1) % 2;
                    this.menu.setClientLoadConditionMode(this.conditionMode);
                    btn.setMessage(this.getConditionButtonLabel());
                    this.sendUpdateToServer();
                }).bounds(xo + 8, yo + 43, 160, 16).build()
        );

        this.suppressResponder = true;

        this.itemWhiteBox = new EditBox(this.font, xo + 8, yo + 71, 78, 14, Component.literal("Item Whitelist"));
        this.itemWhiteBox.setMaxLength(128);
        this.itemWhiteBox.setHint(Component.literal("e.g. coal, ore").withStyle(ChatFormatting.DARK_GRAY));
        this.itemWhiteBox.setValue(cachedItemWhite);
        this.itemWhiteBox.setResponder(v -> {
            cachedItemWhite = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.itemWhiteBox);

        this.itemBlackBox = new EditBox(this.font, xo + 90, yo + 71, 78, 14, Component.literal("Item Blacklist"));
        this.itemBlackBox.setMaxLength(128);
        this.itemBlackBox.setHint(Component.literal("e.g. cobblestone").withStyle(ChatFormatting.DARK_GRAY));
        this.itemBlackBox.setValue(cachedItemBlack);
        this.itemBlackBox.setResponder(v -> {
            cachedItemBlack = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.itemBlackBox);

        this.cartWhiteBox = new EditBox(this.font, xo + 8, yo + 99, 78, 14, Component.literal("Cart Whitelist"));
        this.cartWhiteBox.setMaxLength(128);
        this.cartWhiteBox.setHint(Component.literal("Cart/Train name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartWhiteBox.setValue(cachedCartWhite);
        this.cartWhiteBox.setResponder(v -> {
            cachedCartWhite = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.cartWhiteBox);

        this.cartBlackBox = new EditBox(this.font, xo + 90, yo + 99, 78, 14, Component.literal("Cart Blacklist"));
        this.cartBlackBox.setMaxLength(128);
        this.cartBlackBox.setHint(Component.literal("Exclude name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartBlackBox.setValue(cachedCartBlack);
        this.cartBlackBox.setResponder(v -> {
            cachedCartBlack = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.cartBlackBox);

        this.suppressResponder = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int syncedMode = this.menu.getLoadConditionMode();
        if (syncedMode != this.conditionMode) {
            this.conditionMode = syncedMode;
            if (this.conditionButton != null) {
                this.conditionButton.setMessage(this.getConditionButtonLabel());
            }
        }
    }

    private Component getConditionButtonLabel() {
        if (this.menu.isUnloader()) {
            return this.conditionMode == CargoTransferBlockEntity.CONDITION_FULL_COMPLETE
                    ? Component.literal("Hold Until: Full Empty (0 Items)").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Hold Until: Partial / Idle").withStyle(ChatFormatting.AQUA);
        } else {
            return this.conditionMode == CargoTransferBlockEntity.CONDITION_FULL_COMPLETE
                    ? Component.literal("Hold Until: Full Load (100%)").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Hold Until: Partial / Idle").withStyle(ChatFormatting.AQUA);
        }
    }

    private void sendUpdateToServer() {
        ClientPacketDistributor.sendToServer(new CargoTransferFilterPayload(
                this.conditionMode,
                this.itemWhiteBox != null ? this.itemWhiteBox.getValue() : "",
                this.itemBlackBox != null ? this.itemBlackBox.getValue() : "",
                this.cartWhiteBox != null ? this.cartWhiteBox.getValue() : "",
                this.cartBlackBox != null ? this.cartBlackBox.getValue() : ""
        ));
    }

    private boolean isAnyBoxFocused() {
        return (this.itemWhiteBox != null && this.itemWhiteBox.isFocused())
                || (this.itemBlackBox != null && this.itemBlackBox.isFocused())
                || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused())
                || (this.cartBlackBox != null && this.cartBlackBox.isFocused());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.isAnyBoxFocused()) {
                if (this.itemWhiteBox != null) this.itemWhiteBox.setFocused(false);
                if (this.itemBlackBox != null) this.itemBlackBox.setFocused(false);
                if (this.cartWhiteBox != null) this.cartWhiteBox.setFocused(false);
                if (this.cartBlackBox != null) this.cartBlackBox.setFocused(false);
                return true;
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        if (this.isAnyBoxFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.sendUpdateToServer();
                if (this.itemWhiteBox != null) this.itemWhiteBox.setFocused(false);
                if (this.itemBlackBox != null) this.itemBlackBox.setFocused(false);
                if (this.cartWhiteBox != null) this.cartWhiteBox.setFocused(false);
                if (this.cartBlackBox != null) this.cartBlackBox.setFocused(false);
                return true;
            }
            if ((this.itemWhiteBox != null && this.itemWhiteBox.isFocused() && (this.itemWhiteBox.keyPressed(event) || this.itemWhiteBox.canConsumeInput()))
                    || (this.itemBlackBox != null && this.itemBlackBox.isFocused() && (this.itemBlackBox.keyPressed(event) || this.itemBlackBox.canConsumeInput()))
                    || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused() && (this.cartWhiteBox.keyPressed(event) || this.cartWhiteBox.canConsumeInput()))
                    || (this.cartBlackBox != null && this.cartBlackBox.isFocused() && (this.cartBlackBox.keyPressed(event) || this.cartBlackBox.canConsumeInput()))) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE6EDF3, true);

        String statusStr;
        int statusColor;
        if (this.menu.isHoldingCart()) {
            statusStr = "HOLDING (" + this.menu.getCartFillPct() + "%)";
            statusColor = 0xFFFFAA00;
        } else if (this.menu.isCartDetected()) {
            statusStr = "READY (" + this.menu.getCartFillPct() + "%)";
            statusColor = 0xFF55FF55;
        } else {
            statusStr = "RAIL POWERED";
            statusColor = 0xFF8B949E;
        }
        int sw = this.font.width(statusStr);
        graphics.text(this.font, Component.literal(statusStr), this.imageWidth - sw - 8, 6, statusColor, true);

        graphics.text(this.font, Component.literal("Item Whitelist:"), 8, 61, 0xFF55FF55, false);
        graphics.text(this.font, Component.literal("Item Blacklist:"), 90, 61, 0xFFFF5555, false);
        graphics.text(this.font, Component.literal("Cart Whitelist:"), 8, 89, 0xFF55FFFF, false);
        graphics.text(this.font, Component.literal("Cart Blacklist:"), 90, 89, 0xFFFFAA00, false);

        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFCCCCCC, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.fill(xo - 2, yo - 2, xo + this.imageWidth + 2, yo + this.imageHeight + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + this.imageWidth, yo + this.imageHeight, 0xF221252B);
        graphics.outline(xo, yo, this.imageWidth, this.imageHeight, 0xFF4B5362);

        int accent = this.menu.isUnloader() ? 0xFFF59E0B : 0xFF10B981;
        graphics.fill(xo + 1, yo + 1, xo + this.imageWidth - 1, yo + 3, accent);

        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, xo + 7 + col * 18, yo + 21);
        }

        graphics.fill(xo + 6, yo + 124, xo + this.imageWidth - 6, yo + 125, 0xFF3E4451);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, xo + 7 + col * 18, yo + 139 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, xo + 7 + col * 18, yo + 197);
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 16, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + 16, 0xFF373737);
    }
}
'@
[System.IO.File]::WriteAllText($screenPath, $screenContent, $utf8NoBom)

# 3. Update MiningTools.java
$mtPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\MiningTools.java"
$mtContent = [System.IO.File]::ReadAllText($mtPath, $utf8NoBom)

if (-not $mtContent.Contains("import com.example.miningtools.block.BrakingRailBlock;")) {
    $newImports = @'
import com.example.miningtools.block.BrakingRailBlock;
import com.example.miningtools.block.CargoTransferBlock;
import com.example.miningtools.block.entity.CargoTransferBlockEntity;
import com.example.miningtools.menu.CargoTransferMenu;
import com.example.miningtools.network.CargoTransferFilterPayload;
import com.example.miningtools.network.CargoTransferSyncPayload;
import com.example.miningtools.network.CartRenameOpenPayload;
import com.example.miningtools.network.CartRenamePayload;
'@
    $mtContent = $mtContent.Replace("import com.example.miningtools.block.ControlRailBlock;", "import com.example.miningtools.block.ControlRailBlock;`r`n" + $newImports)
}

if (-not $mtContent.Contains("BRAKING_RAIL")) {
    $oldControlRail = @'
    public static final DeferredItem<BlockItem> CONTROL_RAIL_ITEM = ITEMS.registerSimpleBlockItem(
            "control_rail",
            CONTROL_RAIL
    );
'@
    $newControlAndBraking = @'
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
'@
    $mtContent = $mtContent.Replace($oldControlRail, $newControlAndBraking)
}

if (-not $mtContent.Contains("CARGO_LOADER")) {
    $oldFluidLoader = @'
    public static final DeferredHolder<MenuType<?>, MenuType<FluidLoaderMenu>> FLUID_LOADER_MENU = MENUS.register(
            "fluid_loader",
            () -> new MenuType<>(FluidLoaderMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
'@
    $newFluidAndCargo = @'
    public static final DeferredHolder<MenuType<?>, MenuType<FluidLoaderMenu>> FLUID_LOADER_MENU = MENUS.register(
            "fluid_loader",
            () -> new MenuType<>(FluidLoaderMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Cargo Loader & Cargo Unloader Blocks, Items, BlockEntity & Menu
    public static final DeferredBlock<CargoTransferBlock> CARGO_LOADER = BLOCKS.registerBlock(
            "cargo_loader",
            p -> new CargoTransferBlock(false, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> CARGO_LOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "cargo_loader",
            CARGO_LOADER
    );
    public static final DeferredBlock<CargoTransferBlock> CARGO_UNLOADER = BLOCKS.registerBlock(
            "cargo_unloader",
            p -> new CargoTransferBlock(true, p),
            p -> p.strength(2.5F).sound(SoundType.METAL)
    );
    public static final DeferredItem<BlockItem> CARGO_UNLOADER_ITEM = ITEMS.registerSimpleBlockItem(
            "cargo_unloader",
            CARGO_UNLOADER
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoTransferBlockEntity>> CARGO_TRANSFER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "cargo_transfer",
            () -> new BlockEntityType<>(CargoTransferBlockEntity::new, CARGO_LOADER.get(), CARGO_UNLOADER.get())
    );
    public static final DeferredHolder<MenuType<?>, MenuType<CargoTransferMenu>> CARGO_TRANSFER_MENU = MENUS.register(
            "cargo_transfer",
            () -> new MenuType<>(CargoTransferMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );
'@
    $mtContent = $mtContent.Replace($oldFluidLoader, $newFluidAndCargo)
}

# Add to Creative Tab
if (-not $mtContent.Contains("output.accept(BRAKING_RAIL_ITEM.get());")) {
    $mtContent = $mtContent.Replace(
        "output.accept(CONTROL_RAIL_ITEM.get());",
        "output.accept(CONTROL_RAIL_ITEM.get());`r`n                        output.accept(BRAKING_RAIL_ITEM.get());"
    )
    $mtContent = $mtContent.Replace(
        "output.accept(FLUID_LOADER_ITEM.get());",
        "output.accept(CARGO_LOADER_ITEM.get());`r`n                        output.accept(CARGO_UNLOADER_ITEM.get());`r`n                        output.accept(FLUID_LOADER_ITEM.get());"
    )
}
if (-not $mtContent.Contains("event.accept(BRAKING_RAIL_ITEM);")) {
    $mtContent = $mtContent.Replace(
        "event.accept(CONTROL_RAIL_ITEM);",
        "event.accept(CONTROL_RAIL_ITEM);`r`n            event.accept(BRAKING_RAIL_ITEM);"
    )
    $mtContent = $mtContent.Replace(
        "event.accept(FLUID_LOADER_ITEM);",
        "event.accept(CARGO_LOADER_ITEM);`r`n            event.accept(CARGO_UNLOADER_ITEM);`r`n            event.accept(FLUID_LOADER_ITEM);"
    )
}

# Register Payloads
if (-not $mtContent.Contains("CartRenamePayload.TYPE")) {
    $oldPayloads = @'
        registrar.playToServer(
                ChunkLoaderUpdatePayload.TYPE,
                ChunkLoaderUpdatePayload.STREAM_CODEC,
                ChunkLoaderUpdatePayload::handle
        );
'@
    $newPayloads = @'
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
'@
    $mtContent = $mtContent.Replace($oldPayloads, $newPayloads)
}

[System.IO.File]::WriteAllText($mtPath, $mtContent, $utf8NoBom)
Write-Host "Updated MiningTools.java"

# 4. Update MiningToolsClient.java
$mtcPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\MiningToolsClient.java"
$mtcContent = [System.IO.File]::ReadAllText($mtcPath, $utf8NoBom)

if (-not $mtcContent.Contains("CargoTransferScreen")) {
    $mtcContent = $mtcContent.Replace(
        "import com.example.miningtools.client.gui.FluidLoaderScreen;",
        "import com.example.miningtools.client.gui.FluidLoaderScreen;`r`nimport com.example.miningtools.client.gui.CargoTransferScreen;`r`nimport com.example.miningtools.client.gui.CartRenameScreen;`r`nimport com.example.miningtools.network.CargoTransferSyncPayload;`r`nimport com.example.miningtools.network.CartRenameOpenPayload;"
    )
    $mtcContent = $mtcContent.Replace(
        "ChunkLoaderSyncPayload.CLIENT_HANDLER = ChunkLoaderScreen::open;",
        "ChunkLoaderSyncPayload.CLIENT_HANDLER = ChunkLoaderScreen::open;`r`n        CartRenameOpenPayload.CLIENT_HANDLER = CartRenameScreen::open;`r`n        CargoTransferSyncPayload.CLIENT_HANDLER = CargoTransferScreen::onSyncPayload;"
    )
    $mtcContent = $mtcContent.Replace(
        "event.register(MiningTools.FLUID_LOADER_MENU.get(), FluidLoaderScreen::new);",
        "event.register(MiningTools.FLUID_LOADER_MENU.get(), FluidLoaderScreen::new);`r`n        event.register(MiningTools.CARGO_TRANSFER_MENU.get(), CargoTransferScreen::new);"
    )
    [System.IO.File]::WriteAllText($mtcPath, $mtcContent, $utf8NoBom)
    Write-Host "Updated MiningToolsClient.java"
}
