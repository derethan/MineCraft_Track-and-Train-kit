package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.FluidLoaderBlockEntity;
import com.example.miningtools.menu.FluidLoaderMenu;
import com.example.miningtools.network.FluidLoaderFilterPayload;
import com.example.miningtools.network.FluidLoaderSyncPayload;
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
public class FluidLoaderScreen extends AbstractContainerScreen<FluidLoaderMenu> {
    private static String cachedFluidWhite = "";
    private static String cachedFluidBlack = "";
    private static String cachedCartWhite = "";
    private static String cachedCartBlack = "";

    private boolean suppressResponder = false;
    private @Nullable Button modeButton;
    private @Nullable Button redstoneButton;
    private @Nullable EditBox fluidWhiteBox;
    private @Nullable EditBox fluidBlackBox;
    private @Nullable EditBox cartWhiteBox;
    private @Nullable EditBox cartBlackBox;

    public FluidLoaderScreen(FluidLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 6;
        this.inventoryLabelY = 129;
    }

    public static void onSyncPayload(FluidLoaderSyncPayload payload) {
        cachedFluidWhite = payload.fluidWhitelist() != null ? payload.fluidWhitelist() : "";
        cachedFluidBlack = payload.fluidBlacklist() != null ? payload.fluidBlacklist() : "";
        cachedCartWhite = payload.cartWhitelist() != null ? payload.cartWhitelist() : "";
        cachedCartBlack = payload.cartBlacklist() != null ? payload.cartBlacklist() : "";

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof FluidLoaderScreen screen) {
            screen.applySyncedFilters(cachedFluidWhite, cachedFluidBlack, cachedCartWhite, cachedCartBlack);
        }
    }

    public void applySyncedFilters(String fw, String fb, String cw, String cb) {
        this.suppressResponder = true;
        if (this.fluidWhiteBox != null) this.fluidWhiteBox.setValue(fw);
        if (this.fluidBlackBox != null) this.fluidBlackBox.setValue(fb);
        if (this.cartWhiteBox != null) this.cartWhiteBox.setValue(cw);
        if (this.cartBlackBox != null) this.cartBlackBox.setValue(cb);
        this.suppressResponder = false;
    }

    @Override
    protected void init() {
        super.init();
        int xo = this.leftPos;
        int yo = this.topPos;

        // Transfer Mode Toggle Button
        this.modeButton = this.addRenderableWidget(
                Button.builder(this.getModeButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(xo + 66, yo + 18, 102, 16).build()
        );

        // Redstone / Hold Mode Toggle Button
        this.redstoneButton = this.addRenderableWidget(
                Button.builder(this.getRedstoneButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(xo + 66, yo + 36, 102, 16).build()
        );

        this.suppressResponder = true;

        this.fluidWhiteBox = new EditBox(this.font, xo + 8, yo + 85, 78, 14, Component.literal("Fluid Whitelist"));
        this.fluidWhiteBox.setMaxLength(128);
        this.fluidWhiteBox.setHint(Component.literal("e.g. water, oil").withStyle(ChatFormatting.DARK_GRAY));
        this.fluidWhiteBox.setValue(cachedFluidWhite);
        this.fluidWhiteBox.setResponder(v -> {
            cachedFluidWhite = v;
            if (!this.suppressResponder) this.sendFiltersToServer();
        });
        this.addRenderableWidget(this.fluidWhiteBox);

        this.fluidBlackBox = new EditBox(this.font, xo + 90, yo + 85, 78, 14, Component.literal("Fluid Blacklist"));
        this.fluidBlackBox.setMaxLength(128);
        this.fluidBlackBox.setHint(Component.literal("e.g. lava").withStyle(ChatFormatting.DARK_GRAY));
        this.fluidBlackBox.setValue(cachedFluidBlack);
        this.fluidBlackBox.setResponder(v -> {
            cachedFluidBlack = v;
            if (!this.suppressResponder) this.sendFiltersToServer();
        });
        this.addRenderableWidget(this.fluidBlackBox);

        this.cartWhiteBox = new EditBox(this.font, xo + 8, yo + 111, 78, 14, Component.literal("Cart Whitelist"));
        this.cartWhiteBox.setMaxLength(128);
        this.cartWhiteBox.setHint(Component.literal("Cart/Train name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartWhiteBox.setValue(cachedCartWhite);
        this.cartWhiteBox.setResponder(v -> {
            cachedCartWhite = v;
            if (!this.suppressResponder) this.sendFiltersToServer();
        });
        this.addRenderableWidget(this.cartWhiteBox);

        this.cartBlackBox = new EditBox(this.font, xo + 90, yo + 111, 78, 14, Component.literal("Cart Blacklist"));
        this.cartBlackBox.setMaxLength(128);
        this.cartBlackBox.setHint(Component.literal("Exclude name").withStyle(ChatFormatting.DARK_GRAY));
        this.cartBlackBox.setValue(cachedCartBlack);
        this.cartBlackBox.setResponder(v -> {
            cachedCartBlack = v;
            if (!this.suppressResponder) this.sendFiltersToServer();
        });
        this.addRenderableWidget(this.cartBlackBox);

        this.suppressResponder = false;
    }

    private void sendFiltersToServer() {
        ClientPacketDistributor.sendToServer(new FluidLoaderFilterPayload(
                this.fluidWhiteBox != null ? this.fluidWhiteBox.getValue() : "",
                this.fluidBlackBox != null ? this.fluidBlackBox.getValue() : "",
                this.cartWhiteBox != null ? this.cartWhiteBox.getValue() : "",
                this.cartBlackBox != null ? this.cartBlackBox.getValue() : ""
        ));
    }

    private boolean isAnyBoxFocused() {
        return (this.fluidWhiteBox != null && this.fluidWhiteBox.isFocused())
                || (this.fluidBlackBox != null && this.fluidBlackBox.isFocused())
                || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused())
                || (this.cartBlackBox != null && this.cartBlackBox.isFocused());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.isAnyBoxFocused()) {
                if (this.fluidWhiteBox != null) this.fluidWhiteBox.setFocused(false);
                if (this.fluidBlackBox != null) this.fluidBlackBox.setFocused(false);
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
                this.sendFiltersToServer();
                if (this.fluidWhiteBox != null) this.fluidWhiteBox.setFocused(false);
                if (this.fluidBlackBox != null) this.fluidBlackBox.setFocused(false);
                if (this.cartWhiteBox != null) this.cartWhiteBox.setFocused(false);
                if (this.cartBlackBox != null) this.cartBlackBox.setFocused(false);
                return true;
            }
            if ((this.fluidWhiteBox != null && this.fluidWhiteBox.isFocused() && (this.fluidWhiteBox.keyPressed(event) || this.fluidWhiteBox.canConsumeInput()))
                    || (this.fluidBlackBox != null && this.fluidBlackBox.isFocused() && (this.fluidBlackBox.keyPressed(event) || this.fluidBlackBox.canConsumeInput()))
                    || (this.cartWhiteBox != null && this.cartWhiteBox.isFocused() && (this.cartWhiteBox.keyPressed(event) || this.cartWhiteBox.canConsumeInput()))
                    || (this.cartBlackBox != null && this.cartBlackBox.isFocused() && (this.cartBlackBox.keyPressed(event) || this.cartBlackBox.canConsumeInput()))) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.getModeButtonLabel());
            this.modeButton.active = !this.menu.isDieselFueler();
        }
        if (this.redstoneButton != null) {
            this.redstoneButton.setMessage(this.getRedstoneButtonLabel());
        }
    }

    private Component getModeButtonLabel() {
        if (this.menu.isDieselFueler()) {
            return Component.literal("Mode: Refuel Diesel").withStyle(ChatFormatting.GOLD);
        }
        int mode = this.menu.getTransferMode();
        return switch (mode) {
            case FluidLoaderBlockEntity.MODE_LOAD -> Component.literal("Mode: Load (Fill)").withStyle(ChatFormatting.AQUA);
            case FluidLoaderBlockEntity.MODE_AUTO -> Component.literal("Mode: Auto").withStyle(ChatFormatting.GOLD);
            default -> Component.literal("Mode: Unload (Drain)").withStyle(ChatFormatting.GREEN);
        };
    }

    private Component getRedstoneButtonLabel() {
        int rMode = this.menu.getRedstoneMode();
        return switch (rMode) {
            case FluidLoaderBlockEntity.REDSTONE_WHEN_FULL -> Component.literal("Hold: Until Full").withStyle(ChatFormatting.GOLD);
            case FluidLoaderBlockEntity.REDSTONE_WHEN_DONE -> Component.literal("Hold: Partial/Idle").withStyle(ChatFormatting.AQUA);
            case FluidLoaderBlockEntity.REDSTONE_DISABLED -> Component.literal("Hold: Disabled").withStyle(ChatFormatting.DARK_GRAY);
            default -> Component.literal("Hold: Until Empty").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        boolean fueler = this.menu.isDieselFueler();

        // Active Hold / Rail Power indicator in top-right
        String statusStr;
        int statusColor;
        if (this.menu.isHoldingCart()) {
            statusStr = "HOLDING";
            statusColor = 0xFFD97706;
        } else if (this.menu.isCartPresent()) {
            statusStr = fueler ? "LOCO READY" : "CART READY";
            statusColor = 0xFF2E7D32;
        } else {
            statusStr = "RAIL POWERED";
            statusColor = 0xFF555555;
        }
        int sw = this.font.width(statusStr);
        graphics.text(this.font, Component.literal(statusStr), this.imageWidth - sw - 6, 6, statusColor, false);

        graphics.text(this.font, Component.literal("Fluid Whitelist:"), 8, 75, 0xFF1B5E20, false);
        graphics.text(this.font, Component.literal("Fluid Blacklist:"), 90, 75, 0xFFB71C1C, false);
        graphics.text(this.font, Component.literal(fueler ? "Train Whitelist:" : "Cart Whitelist:"), 8, 102, 0xFF0D47A1, false);
        graphics.text(this.font, Component.literal(fueler ? "Train Blacklist:" : "Cart Blacklist:"), 90, 102, 0xFFE65100, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Draw main GUI panel background
        graphics.fill(xo, yo, xo + this.imageWidth, yo + this.imageHeight, 0xFFC6C6C6);
        graphics.outline(xo, yo, this.imageWidth, this.imageHeight, 0xFF373737);
        graphics.fill(xo + 1, yo + 1, xo + this.imageWidth - 1, yo + 2, 0xFFFFFFFF);
        graphics.fill(xo + 1, yo + 1, xo + 2, yo + this.imageHeight - 1, 0xFFFFFFFF);

        // 1. Station Buffer Tank Gauge (x=12, y=18, w=22, h=54)
        int tankX = xo + 12;
        int tankY = yo + 18;
        int tankW = 22;
        int tankH = 54;
        graphics.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF1B1B1B);
        graphics.outline(tankX, tankY, tankW, tankH, 0xFF373737);

        int bufAmount = this.menu.getStationFluidAmount();
        int bufCap = this.menu.getStationCapacity();
        float bufRatio = Mth.clamp((float) bufAmount / (float) Math.max(1, bufCap), 0.0F, 1.0F);
        int bufFillH = Math.round((tankH - 2) * bufRatio);
        if (bufFillH > 0) {
            int fillColor = this.menu.isDieselFueler() ? 0xFFD97706 : 0xFF1976D2;
            int hiColor = this.menu.isDieselFueler() ? 0xFFFBBF24 : 0xFF64B5F6;
            graphics.fill(tankX + 1, tankY + tankH - 1 - bufFillH, tankX + tankW - 1, tankY + tankH - 1, fillColor);
            graphics.fill(tankX + 2, tankY + tankH - 1 - bufFillH, tankX + 4, tankY + tankH - 1, hiColor);
        }

        // 2. Cart / Locomotive Tank Gauge (x=38, y=18, w=22, h=54)
        int cartX = xo + 38;
        int cartY = yo + 18;
        int cartW = 22;
        int cartH = 54;
        graphics.fill(cartX, cartY, cartX + cartW, cartY + cartH, 0xFF1B1B1B);
        graphics.outline(cartX, cartY, cartW, cartH, 0xFF373737);

        if (this.menu.isCartPresent()) {
            int cartAmount = this.menu.getCartFluidAmount();
            int cartCap = this.menu.getCartFluidCapacity();
            float cartRatio = Mth.clamp((float) cartAmount / (float) Math.max(1, cartCap), 0.0F, 1.0F);
            int cartFillH = Math.round((cartH - 2) * cartRatio);
            if (cartFillH > 0) {
                graphics.fill(cartX + 1, cartY + cartH - 1 - cartFillH, cartX + cartW - 1, cartY + cartH - 1, 0xFF388E3C);
                graphics.fill(cartX + 2, cartY + cartH - 1 - cartFillH, cartX + 4, cartY + cartH - 1, 0xFF81C784);
            }
        } else {
            graphics.fill(cartX + 2, cartY + cartH / 2 - 1, cartX + cartW - 2, cartY + cartH / 2 + 1, 0xFF555555);
        }

        // 3. Bucket slot backgrounds (Slot 0: x=87, y=56; Slot 1: x=141, y=56)
        drawSlotBox(graphics, xo + 87, yo + 56);
        drawSlotBox(graphics, xo + 141, yo + 56);

        // Arrow between bucket slots
        int arrowX = xo + 112;
        int arrowY = yo + 64;
        graphics.fill(arrowX, arrowY, arrowX + 18, arrowY + 2, 0xFF555555);
        graphics.fill(arrowX + 14, arrowY - 3, arrowX + 16, arrowY + 5, 0xFF555555);
        graphics.fill(arrowX + 16, arrowY - 1, arrowX + 18, arrowY + 3, 0xFF555555);

        // 4. Player Inventory Slots (3 rows of 9 at x=7, y=139)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBox(graphics, xo + 7 + c * 18, yo + 139 + r * 18);
            }
        }

        // Hotbar Slots (1 row of 9 at x=7, y=197)
        for (int c = 0; c < 9; c++) {
            drawSlotBox(graphics, xo + 7 + c * 18, yo + 197);
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

        // Hover over Buffer Tank
        if (mouseX >= xo + 12 && mouseX <= xo + 34 && mouseY >= yo + 18 && mouseY <= yo + 72) {
            int amount = this.menu.getStationFluidAmount();
            int cap = this.menu.getStationCapacity();
            String label = this.menu.isDieselFueler() ? "Diesel Station Tank: " : "Station Buffer: ";
            Component text = Component.literal(label + amount + " / " + cap + " mB").withStyle(ChatFormatting.AQUA);
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }

        // Hover over Cart / Locomotive Tank
        if (mouseX >= xo + 38 && mouseX <= xo + 60 && mouseY >= yo + 18 && mouseY <= yo + 72) {
            Component text;
            if (this.menu.isCartPresent()) {
                int amount = this.menu.getCartFluidAmount();
                int cap = this.menu.getCartFluidCapacity();
                String label = this.menu.isDieselFueler() ? "Locomotive Diesel: " : "Cart Fluid: ";
                text = Component.literal(label + amount + " / " + cap + " mB").withStyle(ChatFormatting.GREEN);
            } else {
                String noneMsg = this.menu.isDieselFueler()
                        ? "No Diesel Locomotive detected on track above (ignores freight carts)"
                        : "No liquid freight cart detected on track above (ignores locomotives)";
                text = Component.literal(noneMsg).withStyle(ChatFormatting.GRAY);
            }
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
