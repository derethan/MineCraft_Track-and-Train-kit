package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.DieselLocomotiveEntity;
import com.example.miningtools.menu.DieselLocomotiveMenu;
import com.example.miningtools.network.LocomotiveRenamePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
public class DieselLocomotiveScreen extends AbstractContainerScreen<DieselLocomotiveMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/diesel_locomotive.png");

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

    public DieselLocomotiveScreen(DieselLocomotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedNameFromEntity) {
            String defaultName = Component.translatable("entity.miningtools.diesel_locomotive").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName)) {
                currentVal = titleStr;
            }
        }

        if (this.minecraft != null && this.minecraft.level != null) {
            Entity ent = this.minecraft.level.getEntity(this.menu.getEntityId());
            if (ent instanceof DieselLocomotiveEntity loco) {
                String existing = loco.getTrainName();
                if (!existing.isEmpty()) {
                    currentVal = existing;
                    this.initializedNameFromEntity = true;
                }
            }
        }

        // Train Name text box
        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Train Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(currentVal);
        this.nameBox.setResponder(newVal -> {
            if (!this.suppressResponder && this.nameBox != null && this.nameBox.isFocused()) {
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
                }).bounds(this.leftPos + 118, this.topPos + 16, 50, 16).build()
        );

        // Reverse Direction button
        this.reverseButton = this.addRenderableWidget(
                Button.builder(Component.literal("Reverse"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 34, 50, 16).build()
        );

        // Horn button
        this.hornButton = this.addRenderableWidget(
                Button.builder(Component.literal("Horn"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
                    }
                }).bounds(this.leftPos + 118, this.topPos + 52, 24, 16).build()
        );

        // Headlight toggle button
        this.lightButton = this.addRenderableWidget(
                Button.builder(this.getLightButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 5);
                    }
                }).bounds(this.leftPos + 144, this.topPos + 52, 24, 16).build()
        );

        // Speed decrement button [-]
        this.speedDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("-"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                }).bounds(this.leftPos + 8, this.topPos + 60, 16, 16).build()
        );

        // Speed increment button [+]
        this.speedUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("+"), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                    }
                }).bounds(this.leftPos + 26, this.topPos + 60, 16, 16).build()
        );
    }

    private Component getToggleButtonLabel() {
        return this.menu.isRunning() ? Component.literal("Stop") : Component.literal("Start");
    }

    private Component getLightButtonLabel() {
        return this.menu.isHeadlightEnabled() ? Component.literal("On") : Component.literal("Off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.nameBox != null && !this.nameBox.isFocused() && this.minecraft != null && this.minecraft.level != null) {
            Entity ent = this.minecraft.level.getEntity(this.menu.getEntityId());
            if (ent instanceof DieselLocomotiveEntity loco) {
                String currentServerName = loco.getTrainName();
                if (!currentServerName.equals(this.nameBox.getValue())) {
                    this.suppressResponder = true;
                    this.nameBox.setValue(currentServerName);
                    this.suppressResponder = false;
                }
            }
        }
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        if (this.lightButton != null) {
            this.lightButton.setMessage(this.getLightButtonLabel());
        }
        if (this.speedDownButton != null) {
            this.speedDownButton.active = this.menu.getSpeedLevel() > 1;
        }
        if (this.speedUpButton != null) {
            this.speedUpButton.active = this.menu.getSpeedLevel() < 5;
        }
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x203040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // Top Left: Diesel Fuel warning badge
        Component fuelBadge;
        int badgeColor;
        if (this.menu.getFluidAmount() > 0) {
            fuelBadge = Component.literal("⛽ DIESEL").withStyle(ChatFormatting.GOLD);
            badgeColor = 0xFFD97706;
        } else {
            fuelBadge = Component.literal("⚠ EMPTY TANK").withStyle(ChatFormatting.RED);
            badgeColor = 0xFFDC2626;
        }
        graphics.text(this.font, fuelBadge, 8, 17, badgeColor, false);

        // Center: Engine status and Heading
        Component statusText = this.menu.isRunning()
                ? Component.literal("[Running]").withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal("[Stopped]").withStyle(ChatFormatting.DARK_RED);
        int statusColor = this.menu.isRunning() ? 0xFF1E7A1E : 0xFF9E2020;
        graphics.text(this.font, statusText, 62, 17, statusColor, false);

        Direction facing = Direction.from2DDataValue(this.menu.getFacingIndex());
        String dirMode = this.menu.isReversed() ? "REV" : "FWD";
        Component facingText = Component.literal(facing.getName().toUpperCase() + " [" + dirMode + "]");
        graphics.text(this.font, facingText, 62, 28, 0x404040, false);

        // Slot Labels above the two fuel slots
        graphics.text(this.font, Component.literal("Fuel In"), 63, 39, 0x606060, false);
        graphics.text(this.font, Component.literal("Empty"), 94, 39, 0x606060, false);

        // Speed header and current value on the left
        graphics.text(this.font, Component.literal("Speed:"), 8, 40, 0x505050, false);

        int speedLevel = this.menu.getSpeedLevel();
        String speedName = DieselLocomotiveEntity.getSpeedName(speedLevel);
        Component speedText = Component.literal(speedName).withStyle(ChatFormatting.RED);
        graphics.text(this.font, speedText, 8, 49, 0xFFD3202A, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Top Train Name bar background (CN Red theme)
        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE6D3202A);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal("CN 3087:"), xo + 5, yo - 15, 0xFFFFFFFF, true);

        // Main GUI texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Diesel fuel tank level bar: Top Left of GUI, shrunk (width 44, height 9 at xo + 8, yo + 28)
        int barX = xo + 8;
        int barY = yo + 28;
        int barW = 44;
        int barH = 9;

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF18181B); // Dark background
        float pct = this.menu.getFluidPercent();
        int fillW = Mth.clamp((int) (barW * pct), 0, barW);
        if (fillW > 0) {
            graphics.fill(barX + 1, barY + 1, barX + fillW - 1, barY + barH - 1, 0xFFEAB308); // Golden amber diesel fuel
        }
        graphics.outline(barX, barY, barW, barH, 0xFF52525B);

        // Fuel percentage text inside bar
        String pctStr = Math.round(pct * 100.0F) + "%";
        int pctW = this.font.width(pctStr);
        graphics.text(this.font, Component.literal(pctStr), barX + (barW - pctW) / 2, barY + 1, 0xFFFFFFFF, true);

        // Tooltip over fuel bar
        if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
            int stored = this.menu.getFluidAmount();
            int cap = this.menu.getFluidCapacity();
            graphics.setTooltipForNextFrame(Component.literal("Diesel Fuel Tank: " + stored + " / " + cap + " mB"), mouseX, mouseY);
        }

        // Tooltips over fuel container slots
        if (mouseX >= xo + 66 && mouseX <= xo + 84 && mouseY >= yo + 49 && mouseY <= yo + 67) {
            graphics.setTooltipForNextFrame(Component.literal("Fuel Container Input (Bucket / Canister)"), mouseX, mouseY);
        } else if (mouseX >= xo + 94 && mouseX <= xo + 112 && mouseY >= yo + 49 && mouseY <= yo + 67) {
            graphics.setTooltipForNextFrame(Component.literal("Empty Container Return"), mouseX, mouseY);
        }
    }
}
