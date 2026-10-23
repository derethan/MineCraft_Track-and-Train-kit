package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.entity.ElectricLocomotiveEntity;
import com.example.miningtools.menu.ElectricLocomotiveMenu;
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
public class ElectricLocomotiveScreen extends AbstractContainerScreen<ElectricLocomotiveMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/electric_locomotive.png");

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

    public ElectricLocomotiveScreen(ElectricLocomotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedNameFromEntity) {
            String defaultName = Component.translatable("entity.miningtools.electric_locomotive").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName)) {
                currentVal = titleStr;
            }
        }

        if (this.minecraft != null && this.minecraft.level != null) {
            Entity ent = this.minecraft.level.getEntity(this.menu.getEntityId());
            if (ent instanceof ElectricLocomotiveEntity loco) {
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
    public boolean keyPressed(KeyEvent event) {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                this.nameBox.setFocused(false);
                return true;
            }
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                this.nameBox.setFocused(false);
                return true;
            }
            return this.nameBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    private Component getToggleButtonLabel() {
        return this.menu.isRunning()
                ? Component.literal("Stop").withStyle(ChatFormatting.RED)
                : Component.literal("Start").withStyle(ChatFormatting.DARK_GREEN);
    }

    private Component getLightButtonLabel() {
        return this.menu.isHeadlightEnabled()
                ? Component.literal("💡").withStyle(ChatFormatting.YELLOW)
                : Component.literal("💡").withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        if (this.lightButton != null) {
            this.lightButton.setMessage(this.getLightButtonLabel());
        }

        if (this.nameBox != null && !this.nameBox.isFocused()) {
            if (this.minecraft != null && this.minecraft.level != null) {
                Entity ent = this.minecraft.level.getEntity(this.menu.getEntityId());
                if (ent instanceof ElectricLocomotiveEntity loco) {
                    String actualName = loco.getTrainName();
                    if (!actualName.equals(this.nameBox.getValue())) {
                        this.suppressResponder = true;
                        this.nameBox.setValue(actualName);
                        this.suppressResponder = false;
                    }
                }
            }
        }
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x203040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // Direction & engine status
        Direction facing = Direction.from2DDataValue(this.menu.getFacingIndex());
        String dirMode = this.menu.isReversed() ? "REV" : "FWD";
        Component facingText = Component.literal(facing.getName().toUpperCase() + " [" + dirMode + "]");
        graphics.text(this.font, facingText, 8, 17, 0x404040, false);

        Component statusText = this.menu.isRunning()
                ? Component.literal("[Running]").withStyle(ChatFormatting.DARK_GREEN)
                : Component.literal("[Stopped]").withStyle(ChatFormatting.DARK_RED);
        int statusColor = this.menu.isRunning() ? 0xFF1E7A1E : 0xFF9E2020;
        graphics.text(this.font, statusText, 72, 17, statusColor, false);

        // Speed header and current value
        graphics.text(this.font, Component.literal("Speed:"), 8, 27, 0x505050, false);

        int speedLevel = this.menu.getSpeedLevel();
        String speedName = ElectricLocomotiveEntity.getSpeedName(speedLevel);
        Component speedText = Component.literal(speedName).withStyle(ChatFormatting.BLUE);
        int textWidth = this.font.width(speedText);
        int textX = 8 + (40 - textWidth) / 2;
        graphics.text(this.font, speedText, textX, 38, 0xFF0B5394, false);

        // Power Status Badge
        Component powerBadge;
        int badgeColor;
        if (this.menu.isPoweredFromRail()) {
            powerBadge = Component.literal("⚡ LIVE RAIL").withStyle(ChatFormatting.AQUA);
            badgeColor = 0xFF0284C7;
        } else if (this.menu.getEnergyStored() > 0) {
            powerBadge = Component.literal("🔋 BATTERY").withStyle(ChatFormatting.DARK_GREEN);
            badgeColor = 0xFF15803D;
        } else {
            powerBadge = Component.literal("⚠ NO POWER").withStyle(ChatFormatting.RED);
            badgeColor = 0xFFDC2626;
        }
        graphics.text(this.font, powerBadge, 58, 28, badgeColor, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Top Train Name bar background
        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE61E293B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFF38BDF8);
        graphics.text(this.font, Component.literal("Bullet:"), xo + 5, yo - 15, 0xFFE0F2FE, true);

        // Main GUI texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Battery level bar (width 44, height 12 at xo + 58, yo + 38)
        int barX = xo + 58;
        int barY = yo + 38;
        int barW = 44;
        int barH = 12;

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0F172A); // Dark background
        float pct = this.menu.getEnergyPercent();
        int fillW = Mth.clamp((int) (barW * pct), 0, barW);
        if (fillW > 0) {
            int fillColor = this.menu.isPoweredFromRail() ? 0xFF00E5FF : 0xFF22C55E;
            graphics.fill(barX + 1, barY + 1, barX + fillW - 1, barY + barH - 1, fillColor);
        }
        graphics.outline(barX, barY, barW, barH, 0xFF475569);

        // Battery energy text inside bar
        String pctStr = Math.round(pct * 100.0F) + "%";
        int pctW = this.font.width(pctStr);
        graphics.text(this.font, Component.literal(pctStr), barX + (barW - pctW) / 2, barY + 2, 0xFFFFFFFF, true);

        // Tooltip over battery bar
        if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
            int stored = this.menu.getEnergyStored();
            int cap = this.menu.getEnergyCapacity();
            Component tip = Component.literal("Energy: " + stored + " / " + cap + " FE (" + pctStr + ")")
                    .withStyle(ChatFormatting.AQUA);
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        }

        // Tooltip over battery charging slot (x=80, y=53)
        int slotX = xo + 79;
        int slotY = yo + 52;
        if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
            Component tip = Component.literal("Battery Slot: Place FE energy item to recharge")
                    .withStyle(ChatFormatting.YELLOW);
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        }
    }
}
