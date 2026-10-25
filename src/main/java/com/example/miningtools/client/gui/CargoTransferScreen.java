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
import net.minecraft.util.Mth;
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

    private boolean suppressResponder = false;
    private @Nullable Button modeButton;
    private @Nullable Button redstoneButton;
    private @Nullable EditBox itemWhiteBox;
    private @Nullable EditBox itemBlackBox;
    private @Nullable EditBox cartWhiteBox;
    private @Nullable EditBox cartBlackBox;

    public CargoTransferScreen(CargoTransferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 6;
        this.inventoryLabelY = 129;
    }

    public static void onSyncPayload(CargoTransferSyncPayload payload) {
        cachedItemWhite = payload.itemWhitelist() != null ? payload.itemWhitelist() : "";
        cachedItemBlack = payload.itemBlacklist() != null ? payload.itemBlacklist() : "";
        cachedCartWhite = payload.cartWhitelist() != null ? payload.cartWhitelist() : "";
        cachedCartBlack = payload.cartBlacklist() != null ? payload.cartBlacklist() : "";

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof CargoTransferScreen screen) {
            screen.applySyncedFilters(cachedItemWhite, cachedItemBlack, cachedCartWhite, cachedCartBlack);
        }
    }

    public void applySyncedFilters(String iw, String ib, String cw, String cb) {
        this.suppressResponder = true;
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

        // Transfer Mode Toggle Button (matches FluidLoaderScreen)
        this.modeButton = this.addRenderableWidget(
                Button.builder(this.getModeButtonLabel(), btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(xo + 66, yo + 17, 102, 16).build()
        );

        // Redstone / Hold Mode Toggle Button (matches FluidLoaderScreen)
        this.redstoneButton = this.addRenderableWidget(
                Button.builder(this.getRedstoneButtonLabel(), btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(xo + 66, yo + 35, 102, 16).build()
        );

        this.suppressResponder = true;

        this.itemWhiteBox = new EditBox(this.font, xo + 8, yo + 85, 78, 14, Component.literal("Item Whitelist"));
        this.itemWhiteBox.setMaxLength(128);
        this.itemWhiteBox.setHint(Component.literal(this.menu.isCoalFueler() ? "coal, charcoal" : "e.g. log, ore").withStyle(ChatFormatting.DARK_GRAY));
        this.itemWhiteBox.setValue(cachedItemWhite);
        this.itemWhiteBox.setResponder(v -> {
            cachedItemWhite = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.itemWhiteBox);

        this.itemBlackBox = new EditBox(this.font, xo + 90, yo + 85, 78, 14, Component.literal("Item Blacklist"));
        this.itemBlackBox.setMaxLength(128);
        this.itemBlackBox.setHint(Component.literal("e.g. cobblestone").withStyle(ChatFormatting.DARK_GRAY));
        this.itemBlackBox.setValue(cachedItemBlack);
        this.itemBlackBox.setResponder(v -> {
            cachedItemBlack = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.itemBlackBox);

        this.cartWhiteBox = new EditBox(this.font, xo + 8, yo + 111, 78, 14, Component.literal("Cart Whitelist"));
        this.cartWhiteBox.setMaxLength(128);
        this.cartWhiteBox.setHint(Component.literal("Cart/Train name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartWhiteBox.setValue(cachedCartWhite);
        this.cartWhiteBox.setResponder(v -> {
            cachedCartWhite = v;
            if (!this.suppressResponder) this.sendUpdateToServer();
        });
        this.addRenderableWidget(this.cartWhiteBox);

        this.cartBlackBox = new EditBox(this.font, xo + 90, yo + 111, 78, 14, Component.literal("Cart Blacklist"));
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
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.getModeButtonLabel());
            this.modeButton.active = !this.menu.isCoalFueler();
        }
        if (this.redstoneButton != null) {
            this.redstoneButton.setMessage(this.getRedstoneButtonLabel());
        }
    }

    private Component getModeButtonLabel() {
        if (this.menu.isCoalFueler()) {
            return Component.literal("Mode: Refuel Coal").withStyle(ChatFormatting.GOLD);
        }
        int mode = this.menu.getTransferMode();
        return switch (mode) {
            case CargoTransferBlockEntity.MODE_LOAD -> Component.literal("Mode: Load (Fill)").withStyle(ChatFormatting.AQUA);
            case CargoTransferBlockEntity.MODE_AUTO -> Component.literal("Mode: Auto").withStyle(ChatFormatting.GOLD);
            default -> Component.literal("Mode: Unload (Drain)").withStyle(ChatFormatting.GREEN);
        };
    }

    private Component getRedstoneButtonLabel() {
        int rMode = this.menu.getRedstoneMode();
        return switch (rMode) {
            case CargoTransferBlockEntity.REDSTONE_WHEN_FULL -> Component.literal("Hold: Until Full").withStyle(ChatFormatting.GOLD);
            case CargoTransferBlockEntity.REDSTONE_WHEN_DONE -> Component.literal("Hold: Partial/Idle").withStyle(ChatFormatting.AQUA);
            case CargoTransferBlockEntity.REDSTONE_DISABLED -> Component.literal("Hold: Disabled").withStyle(ChatFormatting.DARK_GRAY);
            default -> Component.literal("Hold: Until Empty").withStyle(ChatFormatting.RED);
        };
    }

    private void sendUpdateToServer() {
        ClientPacketDistributor.sendToServer(new CargoTransferFilterPayload(
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
        super.extractLabels(graphics, mouseX, mouseY);

        boolean fueler = this.menu.isCoalFueler();

        String statusStr;
        int statusColor;
        if (this.menu.isHoldingCart()) {
            statusStr = "HOLDING";
            statusColor = 0xFFD97706;
        } else if (this.menu.isCartDetected()) {
            statusStr = fueler ? "LOCO READY" : "CART READY";
            statusColor = 0xFF2E7D32;
        } else {
            statusStr = "RAIL POWERED";
            statusColor = 0xFF555555;
        }
        int sw = this.font.width(statusStr);
        graphics.text(this.font, Component.literal(statusStr), this.imageWidth - sw - 6, 6, statusColor, false);

        graphics.text(this.font, Component.literal(fueler ? "Coal Whitelist:" : "Item Whitelist:"), 8, 75, 0xFF1B5E20, false);
        graphics.text(this.font, Component.literal(fueler ? "Coal Blacklist:" : "Item Blacklist:"), 90, 75, 0xFFB71C1C, false);
        graphics.text(this.font, Component.literal(fueler ? "Train Whitelist:" : "Cart Whitelist:"), 8, 102, 0xFF0D47A1, false);
        graphics.text(this.font, Component.literal(fueler ? "Train Blacklist:" : "Cart Blacklist:"), 90, 102, 0xFFE65100, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Draw main GUI panel background (matching FluidLoaderScreen)
        graphics.fill(xo, yo, xo + this.imageWidth, yo + this.imageHeight, 0xFFC6C6C6);
        graphics.outline(xo, yo, this.imageWidth, this.imageHeight, 0xFF373737);
        graphics.fill(xo + 1, yo + 1, xo + this.imageWidth - 1, yo + 2, 0xFFFFFFFF);
        graphics.fill(xo + 1, yo + 1, xo + 2, yo + this.imageHeight - 1, 0xFFFFFFFF);

        // 1. Station Buffer Gauge (x=12, y=17, w=22, h=34)
        int bufX = xo + 12;
        int bufY = yo + 17;
        int bufW = 22;
        int bufH = 34;
        graphics.fill(bufX, bufY, bufX + bufW, bufY + bufH, 0xFF1B1B1B);
        graphics.outline(bufX, bufY, bufW, bufH, 0xFF373737);

        int bufCount = this.menu.getStationItemCount();
        int bufCap = this.menu.getStationCapacity();
        float bufRatio = Mth.clamp((float) bufCount / (float) Math.max(1, bufCap), 0.0F, 1.0F);
        int bufFillH = Math.round((bufH - 2) * bufRatio);
        if (bufFillH > 0) {
            int fillColor = this.menu.isCoalFueler() ? 0xFF424242 : 0xFF1976D2;
            int hiColor = this.menu.isCoalFueler() ? 0xFF9E9E9E : 0xFF64B5F6;
            graphics.fill(bufX + 1, bufY + bufH - 1 - bufFillH, bufX + bufW - 1, bufY + bufH - 1, fillColor);
            graphics.fill(bufX + 2, bufY + bufH - 1 - bufFillH, bufX + 4, bufY + bufH - 1, hiColor);
        }

        // 2. Cart / Locomotive Gauge (x=38, y=17, w=22, h=34)
        int cartX = xo + 38;
        int cartY = yo + 17;
        int cartW = 22;
        int cartH = 34;
        graphics.fill(cartX, cartY, cartX + cartW, cartY + cartH, 0xFF1B1B1B);
        graphics.outline(cartX, cartY, cartW, cartH, 0xFF373737);

        if (this.menu.isCartDetected()) {
            int cartCount = this.menu.getCartItemCount();
            int cartCap = this.menu.getCartItemCapacity();
            float cartRatio = Mth.clamp((float) cartCount / (float) Math.max(1, cartCap), 0.0F, 1.0F);
            int cartFillH = Math.round((cartH - 2) * cartRatio);
            if (cartFillH > 0) {
                graphics.fill(cartX + 1, cartY + cartH - 1 - cartFillH, cartX + cartW - 1, cartY + cartH - 1, 0xFF388E3C);
                graphics.fill(cartX + 2, cartY + cartH - 1 - cartFillH, cartX + 4, cartY + cartH - 1, 0xFF81C784);
            }
        } else {
            graphics.fill(cartX + 2, cartY + cartH / 2 - 1, cartX + cartW - 2, cartY + cartH / 2 + 1, 0xFF555555);
        }

        // 3. 9 Buffer Slots at y=55
        for (int col = 0; col < 9; col++) {
            drawSlotBox(graphics, xo + 7 + col * 18, yo + 55);
        }

        // 4. Player Inventory Slots (3 rows of 9 at x=7, y=139)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(graphics, xo + 7 + col * 18, yo + 139 + row * 18);
            }
        }
        // Hotbar Slots (1 row of 9 at x=7, y=197)
        for (int col = 0; col < 9; col++) {
            drawSlotBox(graphics, xo + 7 + col * 18, yo + 197);
        }
    }

    private static void drawSlotBox(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        int xo = this.leftPos;
        int yo = this.topPos;

        // Hover over Station Buffer Gauge
        if (mouseX >= xo + 12 && mouseX <= xo + 34 && mouseY >= yo + 17 && mouseY <= yo + 51) {
            int count = this.menu.getStationItemCount();
            int cap = this.menu.getStationCapacity();
            String label = this.menu.isCoalFueler() ? "Station Coal Buffer: " : "Station Buffer: ";
            Component text = Component.literal(label + count + " / " + cap + " items").withStyle(ChatFormatting.AQUA);
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }

        // Hover over Cart / Locomotive Gauge
        if (mouseX >= xo + 38 && mouseX <= xo + 60 && mouseY >= yo + 17 && mouseY <= yo + 51) {
            Component text;
            if (this.menu.isCartDetected()) {
                int count = this.menu.getCartItemCount();
                int cap = this.menu.getCartItemCapacity();
                String label = this.menu.isCoalFueler() ? "Locomotive Coal: " : "Cart Cargo: ";
                text = Component.literal(label + count + " / " + cap + " items").withStyle(ChatFormatting.GREEN);
            } else {
                String noneMsg = this.menu.isCoalFueler()
                        ? "No coal locomotive/bore detected on track above (ignores freight carts)"
                        : "No cargo freight cart detected on track above (ignores locomotives)";
                text = Component.literal(noneMsg).withStyle(ChatFormatting.GRAY);
            }
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
