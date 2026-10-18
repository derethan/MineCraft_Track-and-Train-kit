package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.menu.BasicLocomotiveMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BasicLocomotiveScreen extends AbstractContainerScreen<BasicLocomotiveMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/basic_locomotive.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;
    private @Nullable Button speedDownButton;
    private @Nullable Button speedUpButton;

    public BasicLocomotiveScreen(BasicLocomotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        // Start / Stop toggle button
        this.toggleButton = this.addRenderableWidget(
                Button.builder(this.getToggleButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 120, this.topPos + 20, 48, 20).build()
        );

        // Reverse Direction button
        this.reverseButton = this.addRenderableWidget(
                Button.builder(Component.literal("Reverse"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 120, this.topPos + 44, 48, 20).build()
        );

        // Speed decrease button [-]
        this.speedDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("-"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(this.leftPos + 8, this.topPos + 49, 18, 18).build()
        );

        // Speed increase button [+]
        this.speedUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("+"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                    }
                }).bounds(this.leftPos + 30, this.topPos + 49, 18, 18).build()
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        int speed = this.menu.getSpeedLevel();
        if (this.speedDownButton != null) {
            this.speedDownButton.active = speed > 1;
        }
        if (this.speedUpButton != null) {
            this.speedUpButton.active = speed < 5;
        }
    }

    private Component getToggleButtonLabel() {
        return this.menu.isRunning()
                ? Component.literal("Stop").withStyle(ChatFormatting.RED)
                : Component.literal("Start").withStyle(ChatFormatting.GREEN);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);

        // Direction & engine status
        Direction facing = Direction.from2DDataValue(this.menu.getFacingIndex());
        Component facingText = Component.literal("Facing: " + facing.getName().toUpperCase());
        graphics.text(this.font, facingText, 8, 17, 0x404040, false);

        Component statusText = this.menu.isRunning()
                ? Component.literal("[Running]").withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal("[Stopped]").withStyle(ChatFormatting.DARK_RED);
        int statusColor = this.menu.isRunning() ? 0xFF1E7A1E : 0xFF9E2020;
        graphics.text(this.font, statusText, 72, 17, statusColor, false);

        // Selected Speed header and current value above speed selector buttons
        graphics.text(this.font, Component.literal("Selected Speed:"), 8, 27, 0x505050, false);

        int speedLevel = this.menu.getSpeedLevel();
        String speedName = switch (speedLevel) {
            case 1 -> "Crawl";
            case 2 -> "Slow";
            case 3 -> "Normal";
            case 4 -> "Fast";
            case 5 -> "Max";
            default -> "Normal";
        };
        Component speedText = Component.literal(speedName).withStyle(ChatFormatting.DARK_BLUE);
        int textWidth = this.font.width(speedText);
        int textX = 8 + (40 - textWidth) / 2;
        graphics.text(this.font, speedText, textX, 38, 0xFF154360, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Render burning flame icon
        if (this.menu.isLit()) {
            int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    14 - litProgressHeight,
                    xo + 81,
                    yo + 31 + 14 - litProgressHeight,
                    14,
                    litProgressHeight
            );
        }
    }
}
