package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.BasicLocomotiveEntity;
import com.example.miningtools.menu.BasicLocomotiveMenu;
import com.example.miningtools.network.LocomotiveRenamePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class BasicLocomotiveScreen extends AbstractContainerScreen<BasicLocomotiveMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/basic_locomotive.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    private @Nullable EditBox nameBox;
    private @Nullable Button setNameButton;
    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;
    private @Nullable Button hornButton;
    private @Nullable Button lightButton;
    private @Nullable Button speedDownButton;
    private @Nullable Button speedUpButton;
    private boolean initializedNameFromEntity = false;
    private boolean suppressResponder = false;

    public BasicLocomotiveScreen(BasicLocomotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedNameFromEntity) {
            String defaultName = Component.translatable("entity.miningtools.basic_locomotive").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName)) {
                currentVal = titleStr;
            }
        }

        // Train Name input box above the main container window
        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Train Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Train Name...").withStyle(ChatFormatting.DARK_GRAY));
        this.suppressResponder = true;
        this.nameBox.setValue(currentVal);
        this.suppressResponder = false;
        this.nameBox.setResponder(newVal -> {
            if (!this.suppressResponder) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(newVal));
            }
        });
        this.addRenderableWidget(this.nameBox);

        // Set Train Name button
        this.setNameButton = this.addRenderableWidget(
                Button.builder(Component.literal("Set"), button -> {
                    if (this.nameBox != null) {
                        ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                        this.nameBox.setFocused(false);
                    }
                }).bounds(this.leftPos + 139, this.topPos - 18, 33, 14).build()
        );

        // Start / Stop toggle button
        this.toggleButton = this.addRenderableWidget(
                Button.builder(this.getToggleButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 120, this.topPos + 16, 48, 16).build()
        );

        // Reverse Direction button
        this.reverseButton = this.addRenderableWidget(
                Button.builder(Component.literal("Reverse"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 120, this.topPos + 34, 48, 16).build()
        );

        // Horn button
        this.hornButton = this.addRenderableWidget(
                Button.builder(Component.literal("Horn"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
                    }
                }).bounds(this.leftPos + 120, this.topPos + 52, 23, 16).build()
        );

        // Headlight toggle button
        this.lightButton = this.addRenderableWidget(
                Button.builder(this.getLightButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 5);
                    }
                }).bounds(this.leftPos + 145, this.topPos + 52, 23, 16).build()
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
        if (!this.initializedNameFromEntity && this.minecraft != null && this.minecraft.level != null) {
            int entityId = this.menu.getEntityId();
            if (entityId > 0) {
                Entity ent = this.minecraft.level.getEntity(entityId);
                if (ent instanceof BasicLocomotiveEntity loco) {
                    this.initializedNameFromEntity = true;
                    if (this.nameBox != null && !this.nameBox.isFocused()) {
                        this.suppressResponder = true;
                        this.nameBox.setValue(loco.getTrainName());
                        this.suppressResponder = false;
                    }
                }
            }
        }
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        if (this.lightButton != null) {
            this.lightButton.setMessage(this.getLightButtonLabel());
        }
        int speed = this.menu.getSpeedLevel();
        if (this.speedDownButton != null) {
            this.speedDownButton.active = speed > 1;
        }
        if (this.speedUpButton != null) {
            this.speedUpButton.active = speed < 5;
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos) {
        if (mouseX >= leftPos && mouseX < leftPos + this.imageWidth && mouseY >= topPos - 24 && mouseY < topPos) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, leftPos, topPos);
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

    private Component getToggleButtonLabel() {
        return this.menu.isRunning()
                ? Component.literal("Stop").withStyle(ChatFormatting.RED)
                : Component.literal("Start").withStyle(ChatFormatting.GREEN);
    }

    private Component getLightButtonLabel() {
        return this.menu.isHeadlightEnabled()
                ? Component.literal("Light").withStyle(ChatFormatting.GOLD)
                : Component.literal("Light").withStyle(ChatFormatting.GRAY);
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
        String speedName = BasicLocomotiveEntity.getSpeedName(speedLevel);
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

        // Top Train Name bar background
        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE621252B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFF4B5362);
        graphics.text(this.font, Component.literal("Train:"), xo + 5, yo - 15, 0xFFE6EDF3, true);

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
