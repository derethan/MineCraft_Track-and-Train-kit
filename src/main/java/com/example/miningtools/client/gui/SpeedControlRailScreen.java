package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.SpeedControlRailBlockEntity;
import com.example.miningtools.network.SpeedControlRailSyncPayload;
import com.example.miningtools.network.SpeedControlRailUpdatePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class SpeedControlRailScreen extends Screen {
    private static final int WINDOW_WIDTH = 236;
    private static final int WINDOW_HEIGHT = 186;

    private final BlockPos blockPos;
    private int targetGear;
    private int directionMode;
    private String trainNameFilter;

    private int leftPos;
    private int topPos;

    private final Button[] gearButtons = new Button[6];
    private @Nullable Button dirButton;
    private @Nullable EditBox filterBox;

    public SpeedControlRailScreen(BlockPos blockPos, int targetGear, int directionMode, String trainNameFilter) {
        super(Component.literal("Speed Control Rail"));
        this.blockPos = blockPos;
        this.targetGear = targetGear;
        this.directionMode = directionMode;
        this.trainNameFilter = trainNameFilter != null ? trainNameFilter : "";
    }

    public static void open(SpeedControlRailSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new SpeedControlRailScreen(
                payload.pos(),
                payload.targetGear(),
                payload.directionMode(),
                payload.trainNameFilter()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        int xo = this.leftPos;
        int yo = this.topPos;

        // Gear selection buttons (0 = Stop, 1..5 = Gears)
        int btnW = 32;
        int btnH = 20;
        int gap = 3;
        int startX = xo + 14;
        int gearY = yo + 44;

        for (int i = 0; i <= 5; i++) {
            final int gear = i;
            String label = gear == 0 ? "0 (Stop)" : String.valueOf(gear);
            int width = gear == 0 ? 46 : 28;
            gearButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.literal(label), btn -> {
                        this.targetGear = gear;
                        updateGearButtonStyles();
                        this.syncToServer();
                    }).bounds(startX, gearY, width, btnH).build()
            );
            startX += width + gap;
        }
        updateGearButtonStyles();

        // Direction mode button
        int dirY = yo + 84;
        this.dirButton = this.addRenderableWidget(
                Button.builder(this.getDirectionButtonLabel(), btn -> {
                    this.directionMode = (this.directionMode + 1) % 3;
                    if (this.dirButton != null) {
                        this.dirButton.setMessage(this.getDirectionButtonLabel());
                    }
                    this.syncToServer();
                }).bounds(xo + 14, dirY, WINDOW_WIDTH - 28, 20).build()
        );

        // Train Name Filter edit box
        int filterY = yo + 124;
        this.filterBox = new EditBox(this.font, xo + 14, filterY, WINDOW_WIDTH - 28, 18, Component.literal("Train Filter"));
        this.filterBox.setMaxLength(32);
        this.filterBox.setHint(Component.literal("All Trains (* or empty)").withStyle(ChatFormatting.DARK_GRAY));
        this.filterBox.setValue(this.trainNameFilter);
        this.filterBox.setResponder(val -> {
            this.trainNameFilter = val;
            this.syncToServer();
        });
        this.addRenderableWidget(this.filterBox);

        // Done button
        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(xo + (WINDOW_WIDTH - 80) / 2, yo + 154, 80, 20)
                        .build()
        );
    }

    private void updateGearButtonStyles() {
        for (int i = 0; i <= 5; i++) {
            if (gearButtons[i] != null) {
                if (i == this.targetGear) {
                    gearButtons[i].setMessage(Component.literal(i == 0 ? "★ Stop" : "★ " + i).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                } else {
                    gearButtons[i].setMessage(Component.literal(i == 0 ? "Stop" : String.valueOf(i)).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    private Component getDirectionButtonLabel() {
        return switch (this.directionMode) {
            case SpeedControlRailBlockEntity.DIR_FORWARD -> Component.literal("Direction: Force Forward").withStyle(ChatFormatting.GREEN);
            case SpeedControlRailBlockEntity.DIR_REVERSE -> Component.literal("Direction: Force Reverse").withStyle(ChatFormatting.AQUA);
            default -> Component.literal("Direction: Keep Current").withStyle(ChatFormatting.YELLOW);
        };
    }

    private void syncToServer() {
        ClientPacketDistributor.sendToServer(new SpeedControlRailUpdatePayload(
                this.blockPos,
                this.targetGear,
                this.directionMode,
                this.trainNameFilter
        ));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (this.filterBox != null && this.filterBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.filterBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.syncToServer();
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Panel background
        graphics.fill(xo - 2, yo - 2, xo + WINDOW_WIDTH + 2, yo + WINDOW_HEIGHT + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + WINDOW_WIDTH, yo + WINDOW_HEIGHT, 0xF21E222A);
        graphics.outline(xo, yo, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4B5362);

        // Accent top bar (Vibrant Amber / Gold)
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, 0xFFF59E0B);

        // Title
        graphics.text(this.font, Component.literal("Speed & Gear Control Rail").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), xo + 14, yo + 8, 0xFFFFFFFF, true);

        // Subtitle / Pos
        String sub = String.format("Pos: [%d, %d, %d]", this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        graphics.text(this.font, Component.literal(sub).withStyle(ChatFormatting.DARK_GRAY), xo + 14, yo + 20, 0xFF888888, false);

        // Labels
        graphics.text(this.font, Component.literal("Target Gear / Speed:").withStyle(ChatFormatting.WHITE), xo + 14, yo + 33, 0xFFE0E0E0, false);
        graphics.text(this.font, Component.literal("Direction Setting:").withStyle(ChatFormatting.WHITE), xo + 14, yo + 72, 0xFFE0E0E0, false);
        graphics.text(this.font, Component.literal("Train Filter (Locomotive Name):").withStyle(ChatFormatting.WHITE), xo + 14, yo + 112, 0xFFE0E0E0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
