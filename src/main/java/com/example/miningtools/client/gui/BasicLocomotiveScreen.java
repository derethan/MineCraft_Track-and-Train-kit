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
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
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
        Component statusText = this.menu.isRunning()
                ? Component.literal("Running").withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal("Stopped").withStyle(ChatFormatting.DARK_RED);
        graphics.text(this.font, statusText, 8, 22, this.menu.isRunning() ? 0xFF1E7A1E : 0xFF9E2020, false);

        Direction facing = Direction.from2DDataValue(this.menu.getFacingIndex());
        Component facingText = Component.literal("Facing: " + facing.getName().toUpperCase());
        graphics.text(this.font, facingText, 8, 34, 0x404040, false);
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
