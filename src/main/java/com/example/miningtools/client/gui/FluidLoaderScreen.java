package com.example.miningtools.client.gui;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.block.entity.FluidLoaderBlockEntity;
import com.example.miningtools.menu.FluidLoaderMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FluidLoaderScreen extends AbstractContainerScreen<FluidLoaderMenu> {
    private @Nullable Button modeButton;
    private @Nullable Button redstoneButton;

    public FluidLoaderScreen(FluidLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        // Transfer Mode Toggle Button
        this.modeButton = this.addRenderableWidget(
                Button.builder(this.getModeButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 66, this.topPos + 18, 102, 16).build()
        );

        // Redstone Mode Toggle Button
        this.redstoneButton = this.addRenderableWidget(
                Button.builder(this.getRedstoneButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 66, this.topPos + 37, 102, 16).build()
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.getModeButtonLabel());
        }
        if (this.redstoneButton != null) {
            this.redstoneButton.setMessage(this.getRedstoneButtonLabel());
        }
    }

    private Component getModeButtonLabel() {
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
            case FluidLoaderBlockEntity.REDSTONE_WHEN_FULL -> Component.literal("Signal: On Full").withStyle(ChatFormatting.GOLD);
            case FluidLoaderBlockEntity.REDSTONE_WHEN_DONE -> Component.literal("Signal: When Done").withStyle(ChatFormatting.AQUA);
            case FluidLoaderBlockEntity.REDSTONE_DISABLED -> Component.literal("Signal: Disabled").withStyle(ChatFormatting.DARK_GRAY);
            default -> Component.literal("Signal: On Empty").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        // Tank header labels
        graphics.text(this.font, Component.literal("Tank"), 14, 8, 0x404040, false);
        graphics.text(this.font, Component.literal("Cart"), 40, 8, 0x404040, false);

        // Active Redstone Output indicator
        if (this.menu.isRedstonePowered()) {
            graphics.text(this.font, Component.literal("* PULSING *").withStyle(ChatFormatting.RED), 68, 56, 0xFFFF0000, false);
        } else if (this.menu.isCartPresent()) {
            graphics.text(this.font, Component.literal("Cart on Track").withStyle(ChatFormatting.DARK_GREEN), 68, 56, 0xFF2E7D32, false);
        } else {
            graphics.text(this.font, Component.literal("Waiting for Cart").withStyle(ChatFormatting.GRAY), 68, 56, 0xFF888888, false);
        }
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

        // 1. Station Buffer Tank Gauge (x=12, y=18, w=22, h=52)
        int tankX = xo + 12;
        int tankY = yo + 18;
        int tankW = 22;
        int tankH = 52;
        graphics.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF1B1B1B);
        graphics.outline(tankX, tankY, tankW, tankH, 0xFF373737);

        int bufAmount = this.menu.getStationFluidAmount();
        int bufCap = this.menu.getStationCapacity();
        float bufRatio = Mth.clamp((float) bufAmount / (float) Math.max(1, bufCap), 0.0F, 1.0F);
        int bufFillH = Math.round((tankH - 2) * bufRatio);
        if (bufFillH > 0) {
            graphics.fill(tankX + 1, tankY + tankH - 1 - bufFillH, tankX + tankW - 1, tankY + tankH - 1, 0xFF1976D2);
            graphics.fill(tankX + 2, tankY + tankH - 1 - bufFillH, tankX + 4, tankY + tankH - 1, 0xFF64B5F6);
        }

        // 2. Cart on Track Gauge (x=38, y=18, w=22, h=52)
        int cartX = xo + 38;
        int cartY = yo + 18;
        int cartW = 22;
        int cartH = 52;
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
            // Draw diagonal stripe or hatched line for "No Cart"
            graphics.fill(cartX + 2, cartY + cartH / 2 - 1, cartX + cartW - 2, cartY + cartH / 2 + 1, 0xFF555555);
        }

        // 3. Bucket slot backgrounds (Slot 0: x=87, y=58; Slot 1: x=141, y=58)
        drawSlotBox(graphics, xo + 87, yo + 58);
        drawSlotBox(graphics, xo + 141, yo + 58);

        // Arrow between bucket slots (x=112 to 134)
        int arrowX = xo + 112;
        int arrowY = yo + 66;
        graphics.fill(arrowX, arrowY, arrowX + 18, arrowY + 2, 0xFF555555);
        graphics.fill(arrowX + 14, arrowY - 3, arrowX + 16, arrowY + 5, 0xFF555555);
        graphics.fill(arrowX + 16, arrowY - 1, arrowX + 18, arrowY + 3, 0xFF555555);

        // 4. Player Inventory Slots (3 rows of 9 at x=7, y=83)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBox(graphics, xo + 7 + c * 18, yo + 83 + r * 18);
            }
        }

        // Hotbar Slots (1 row of 9 at x=7, y=141)
        for (int c = 0; c < 9; c++) {
            drawSlotBox(graphics, xo + 7 + c * 18, yo + 141);
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
        if (mouseX >= xo + 12 && mouseX <= xo + 34 && mouseY >= yo + 18 && mouseY <= yo + 70) {
            int amount = this.menu.getStationFluidAmount();
            int cap = this.menu.getStationCapacity();
            Component text = Component.literal("Station Buffer: " + amount + " / " + cap + " mB").withStyle(ChatFormatting.AQUA);
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }

        // Hover over Cart Tank
        if (mouseX >= xo + 38 && mouseX <= xo + 60 && mouseY >= yo + 18 && mouseY <= yo + 70) {
            Component text;
            if (this.menu.isCartPresent()) {
                int amount = this.menu.getCartFluidAmount();
                int cap = this.menu.getCartFluidCapacity();
                text = Component.literal("Cart Fluid: " + amount + " / " + cap + " mB").withStyle(ChatFormatting.GREEN);
            } else {
                text = Component.literal("No liquid cart detected on track above").withStyle(ChatFormatting.GRAY);
            }
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
