package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.TrackPaverEntity;
import com.example.miningtools.menu.TrackPaverMenu;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class TrackPaverScreen extends AbstractContainerScreen<TrackPaverMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/track_paver.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;
    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;
    private @Nullable Button lockButton;

    public TrackPaverScreen(TrackPaverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.track_paver").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Heavy Track & Road Paver")) {
                currentVal = titleStr;
            }
        }

        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Paver Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Paver Name...").withStyle(ChatFormatting.DARK_GRAY));
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
                }).bounds(this.leftPos + 88, this.topPos + 14, 42, 17).build()
        );

        this.reverseButton = this.addRenderableWidget(
                Button.builder(this.getReverseButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 132, this.topPos + 14, 36, 17).build()
        );

        this.lockButton = this.addRenderableWidget(
                Button.builder(this.getLockButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(this.leftPos + 88, this.topPos + 33, 80, 17).build()
        );
    }

    private Component getToggleButtonLabel() {
        int mode = this.menu.getRunMode();
        if (mode == TrackPaverEntity.MODE_FORWARD) {
            return Component.literal("Halt").withStyle(ChatFormatting.RED);
        }
        return Component.literal("Start").withStyle(ChatFormatting.GREEN);
    }

    private Component getReverseButtonLabel() {
        int mode = this.menu.getRunMode();
        if (mode == TrackPaverEntity.MODE_REVERSE) {
            return Component.literal("Rev").withStyle(ChatFormatting.YELLOW);
        }
        return Component.literal("Rev").withStyle(ChatFormatting.GRAY);
    }

    private Component getLockButtonLabel() {
        int lock = this.menu.getCardinalLock();
        return switch (lock) {
            case TrackPaverEntity.LOCK_NORTH -> Component.literal("Lock: N").withStyle(ChatFormatting.AQUA);
            case TrackPaverEntity.LOCK_SOUTH -> Component.literal("Lock: S").withStyle(ChatFormatting.AQUA);
            case TrackPaverEntity.LOCK_WEST -> Component.literal("Lock: W").withStyle(ChatFormatting.AQUA);
            case TrackPaverEntity.LOCK_EAST -> Component.literal("Lock: E").withStyle(ChatFormatting.AQUA);
            default -> Component.literal("Lock: Off").withStyle(ChatFormatting.GRAY);
        };
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
        if (this.lockButton != null) {
            this.lockButton.setMessage(this.getLockButtonLabel());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(this.font, Component.literal("Paving Material"), 8, 44, 0x404040, false);
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
                    xo + 36,
                    yo + 21 + 14 - litHeight,
                    14,
                    litHeight
            );
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
