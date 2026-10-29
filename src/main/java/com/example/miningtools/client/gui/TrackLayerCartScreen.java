package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.TrackLayerCartEntity;
import com.example.miningtools.menu.TrackLayerCartMenu;
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
public class TrackLayerCartScreen extends AbstractContainerScreen<TrackLayerCartMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/track_layer_cart.png");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;
    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;
    private @Nullable Button modeButton;

    public TrackLayerCartScreen(TrackLayerCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.track_layer_cart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Track Layer Cart")) {
                currentVal = titleStr;
            }
        }

        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Cart Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Cart Name...").withStyle(ChatFormatting.DARK_GRAY));
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

        this.toggleButton = this.addRenderableWidget(
                Button.builder(this.getToggleButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 40, this.topPos + 18, 42, 16).build()
        );

        this.reverseButton = this.addRenderableWidget(
                Button.builder(this.getReverseButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 84, this.topPos + 18, 32, 16).build()
        );

        this.modeButton = this.addRenderableWidget(
                Button.builder(this.getModeButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 18, 52, 16).build()
        );
    }

    private Component getToggleButtonLabel() {
        boolean running = (this.menu.getRunMode() != TrackLayerCartEntity.RUN_MODE_STOPPED);
        return Component.literal(running ? "Stop" : "Start");
    }

    private Component getReverseButtonLabel() {
        boolean rev = (this.menu.getRunMode() == TrackLayerCartEntity.RUN_MODE_REVERSE);
        return Component.literal(rev ? "Rev" : "Fwd");
    }

    private Component getModeButtonLabel() {
        return switch (this.menu.getWorkMode()) {
            case TrackLayerCartEntity.WORK_MODE_LAY -> Component.literal("Lay");
            case TrackLayerCartEntity.WORK_MODE_REMOVE -> Component.literal("Remove");
            default -> Component.literal("Off");
        };
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.nameBox.setFocused(false);
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                this.nameBox.setFocused(false);
                return true;
            }
            return this.nameBox.keyPressed(event);
        }
        return super.keyPressed(event);
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
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.getModeButtonLabel());
        }

        if (this.nameBox != null && !this.nameBox.isFocused()) {
            String titleStr = this.title.getString();
            String defaultName = Component.translatable("entity.miningtools.track_layer_cart").getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Track Layer Cart")) {
                if (!this.nameBox.getValue().equals(titleStr)) {
                    this.suppressResponder = true;
                    this.nameBox.setValue(titleStr);
                    this.suppressResponder = false;
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        String modeDesc = switch (this.menu.getWorkMode()) {
            case TrackLayerCartEntity.WORK_MODE_LAY -> "Laying (1.5 m/s)";
            case TrackLayerCartEntity.WORK_MODE_REMOVE -> "Removing (1.5 m/s)";
            default -> "Transport Mode";
        };

        int color = switch (this.menu.getWorkMode()) {
            case TrackLayerCartEntity.WORK_MODE_LAY -> 0xFF1E7A1E;
            case TrackLayerCartEntity.WORK_MODE_REMOVE -> 0xFFD4A017;
            default -> 0xFF555555;
        };

        graphics.text(this.font, Component.literal(modeDesc), 40, 38, color, false);
        graphics.text(this.font, Component.literal("Rails: " + this.menu.getStoredRailCount()), 120, 38, 0xFF005577, false);
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
    }
}
