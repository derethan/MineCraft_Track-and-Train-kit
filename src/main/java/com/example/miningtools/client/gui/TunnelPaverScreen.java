package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.TunnelPaverEntity;
import com.example.miningtools.menu.TunnelPaverMenu;
import com.example.miningtools.network.LocomotiveRenamePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class TunnelPaverScreen extends AbstractContainerScreen<TunnelPaverMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/tunnel_paver.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    private static final String[] PATTERN_TOOLTIPS = new String[] {
            "Left Wall (Bottom) Filter: Block placed at bottom-left tunnel wall (empty = skip)",
            "Left Wall (Middle) Filter: Block placed at mid-left tunnel wall (empty = skip)",
            "Left Wall (Top) Filter: Block placed at top-left tunnel wall (empty = skip)",
            "Ceiling (Left) Filter: Block placed at left block of row above (empty = skip)",
            "Ceiling (Center) Filter: Block placed at center block of row above (empty = skip)",
            "Ceiling (Right) Filter: Block placed at right block of row above (empty = skip)",
            "Right Wall (Top) Filter: Block placed at top-right tunnel wall (empty = skip)",
            "Right Wall (Middle) Filter: Block placed at mid-right tunnel wall (empty = skip)",
            "Right Wall (Bottom) Filter: Block placed at bottom-right tunnel wall (empty = skip)"
    };

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;
    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;
    private @Nullable Button workButton;
    private @Nullable Button lightButton;
    private @Nullable Button hologramButton;

    public TunnelPaverScreen(TunnelPaverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 226);
        this.inventoryLabelY = 134;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.tunnel_paver").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Tunnel Paver") && !titleStr.equals("Tunnel Wall & Ceiling Paver")) {
                currentVal = titleStr;
            }
        }

        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Paver Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Tunnel Paver Name...").withStyle(ChatFormatting.DARK_GRAY));
        this.suppressResponder = true;
        this.nameBox.setValue(currentVal);
        this.suppressResponder = false;
        this.nameBox.setResponder(newVal -> {
            if (!this.suppressResponder) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(newVal));
            }
        });
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(
                Button.builder(Component.literal("Set"), button -> {
                    if (this.nameBox != null) {
                        ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                        this.nameBox.setFocused(false);
                    }
                }).bounds(this.leftPos + 139, this.topPos - 18, 33, 14).build()
        );

        // Row 1: Start/Halt and Reverse
        this.toggleButton = this.addRenderableWidget(
                Button.builder(this.getToggleButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 13, 26, 12).build()
        );

        this.reverseButton = this.addRenderableWidget(
                Button.builder(this.getReverseButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 145, this.topPos + 13, 25, 12).build()
        );

        // Row 2: Work Mode
        this.workButton = this.addRenderableWidget(
                Button.builder(this.getWorkButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 27, 52, 12).build()
        );

        // Row 3: Work Light
        this.lightButton = this.addRenderableWidget(
                Button.builder(this.getLightButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 41, 52, 12).build()
        );

        // Row 4: Hologram Guide
        this.hologramButton = this.addRenderableWidget(
                Button.builder(this.getHologramButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 5);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 55, 52, 12).build()
        );
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos) {
        if (mouseX >= leftPos && mouseX < leftPos + this.imageWidth && mouseY >= topPos - 24 && mouseY < topPos) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, leftPos, topPos);
    }

    private Component getToggleButtonLabel() {
        int mode = this.menu.getRunMode();
        if (mode == TunnelPaverEntity.MODE_FORWARD) {
            return Component.literal("Halt").withStyle(ChatFormatting.RED);
        }
        return Component.literal("Go").withStyle(ChatFormatting.GREEN);
    }

    private Component getReverseButtonLabel() {
        int mode = this.menu.getRunMode();
        if (mode == TunnelPaverEntity.MODE_REVERSE) {
            return Component.literal("Rev").withStyle(ChatFormatting.YELLOW);
        }
        return Component.literal("Rev").withStyle(ChatFormatting.GRAY);
    }

    private Component getWorkButtonLabel() {
        return this.menu.isWorkMode()
                ? Component.literal("Work: ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("Work: OFF").withStyle(ChatFormatting.GOLD);
    }

    private Component getLightButtonLabel() {
        return this.menu.isWorkLightEnabled()
                ? Component.literal("Light: ON").withStyle(ChatFormatting.GOLD)
                : Component.literal("Light: OFF").withStyle(ChatFormatting.GRAY);
    }

    private Component getHologramButtonLabel() {
        return this.menu.isHologramEnabled()
                ? Component.literal("Holo: ON").withStyle(ChatFormatting.AQUA)
                : Component.literal("Holo: OFF").withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        if (this.reverseButton != null) {
            this.reverseButton.setMessage(this.getReverseButtonLabel());
        }
        if (this.workButton != null) {
            this.workButton.setMessage(this.getWorkButtonLabel());
        }
        if (this.lightButton != null) {
            this.lightButton.setMessage(this.getLightButtonLabel());
        }
        if (this.hologramButton != null) {
            this.hologramButton.setMessage(this.getHologramButtonLabel());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, Component.literal("Fuel"), 6, 5, 0x404040, false);
        graphics.text(this.font, Component.literal("Arch Pattern (L / Top / R)"), 26, 4, 0x404040, false);
        graphics.text(this.font, Component.literal("Paving Material"), 8, 68, 0x404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE621252B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFF4B5362);
        graphics.text(this.font, Component.literal("Cart:"), xo + 5, yo - 15, 0xFFE6EDF3, true);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.getFuel() > 0) {
            int litHeight = this.menu.getBurnProgressScaled(14);
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    14 - litHeight,
                    xo + 9,
                    yo + 36 + 14 - litHeight,
                    14,
                    litHeight
            );
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Hover over Fuel Slot
        if (mouseX >= xo + 7 && mouseX <= xo + 25 && mouseY >= yo + 16 && mouseY <= yo + 34) {
            if (this.hoveredSlot == null || this.hoveredSlot.getItem().isEmpty()) {
                graphics.setTooltipForNextFrame(Component.literal("Fuel Slot: Insert burnable fuel (Coal, Charcoal, Logs, etc.)").withStyle(ChatFormatting.GOLD), mouseX, mouseY);
                return;
            }
        }

        // Hover over Burn Progress Flame
        if (mouseX >= xo + 8 && mouseX <= xo + 24 && mouseY >= yo + 35 && mouseY <= yo + 51) {
            int fuel = this.menu.getFuel();
            int total = this.menu.getFuelTotal();
            Component text = fuel > 0
                    ? Component.literal("Combustion Fuel: " + fuel + " / " + total + " ticks").withStyle(ChatFormatting.GOLD)
                    : Component.literal("Engine Cold (No Fuel Active)").withStyle(ChatFormatting.GRAY);
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
            return;
        }

        // Hover over any of the 9 Pattern Filter Slots
        for (int i = 0; i < TunnelPaverMenu.PATTERN_SLOTS_COUNT; i++) {
            int sx = xo + TunnelPaverMenu.PATTERN_SLOT_COORDS[i][0] - 1;
            int sy = yo + TunnelPaverMenu.PATTERN_SLOT_COORDS[i][1] - 1;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                if (this.hoveredSlot == null || this.hoveredSlot.getItem().isEmpty()) {
                    graphics.setTooltipForNextFrame(Component.literal(PATTERN_TOOLTIPS[i]).withStyle(ChatFormatting.AQUA), mouseX, mouseY);
                }
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.nameBox != null && this.nameBox.isFocused()) {
                this.nameBox.setFocused(false);
                return true;
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                this.nameBox.setFocused(false);
                return true;
            }
            if (this.nameBox.keyPressed(event) || this.nameBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
