package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.DelayRailBlockEntity;
import com.example.miningtools.network.DelayRailSyncPayload;
import com.example.miningtools.network.DelayRailUpdatePayload;
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
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class DelayRailScreen extends Screen {
    private static final int WINDOW_WIDTH = 246;
    private static final int WINDOW_HEIGHT = 216;

    private final BlockPos blockPos;
    private boolean timerEnabled;
    private int delaySeconds;
    private int releaseDirection;
    private String trainNameFilter;
    private int liveRemainingTicks;

    private int leftPos;
    private int topPos;

    private @Nullable Button toggleTimerBtn;
    private @Nullable Button dirButton;
    private @Nullable EditBox filterBox;

    public DelayRailScreen(BlockPos blockPos, boolean timerEnabled, int delaySeconds, int releaseDirection, String trainNameFilter, int remainingTicks) {
        super(Component.literal("Station Delay Rail"));
        this.blockPos = blockPos;
        this.timerEnabled = timerEnabled;
        this.delaySeconds = delaySeconds;
        this.releaseDirection = releaseDirection;
        this.trainNameFilter = trainNameFilter != null ? trainNameFilter : "";
        this.liveRemainingTicks = remainingTicks;
    }

    public static void open(DelayRailSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new DelayRailScreen(
                payload.pos(),
                payload.timerEnabled(),
                payload.delaySeconds(),
                payload.releaseDirection(),
                payload.trainNameFilter(),
                payload.remainingTicks()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        int xo = this.leftPos;
        int yo = this.topPos;

        // 1. Timer Toggle Button (yo + 34)
        this.toggleTimerBtn = this.addRenderableWidget(
                Button.builder(this.getTimerToggleLabel(), btn -> {
                    this.timerEnabled = !this.timerEnabled;
                    if (this.toggleTimerBtn != null) {
                        this.toggleTimerBtn.setMessage(this.getTimerToggleLabel());
                    }
                    this.syncToServer();
                }).bounds(xo + 14, yo + 34, WINDOW_WIDTH - 28, 20).build()
        );

        // 2. Preset Delay Buttons (yo + 72)
        int[] presets = new int[]{3, 5, 10, 15, 30, 60};
        int btnW = 34;
        int pX = xo + 14;
        for (int p : presets) {
            final int sec = p;
            this.addRenderableWidget(
                    Button.builder(Component.literal(sec + "s"), btn -> {
                        this.delaySeconds = sec;
                        this.syncToServer();
                    }).bounds(pX, yo + 72, btnW, 18).build()
            );
            pX += btnW + 2;
        }

        // 3. Fine-Tune Increment / Decrement Buttons (yo + 94)
        int fW = 48;
        int fX = xo + 14;
        int[] deltas = new int[]{-10, -1, 1, 10};
        String[] deltaLabels = new String[]{"-10s", "-1s", "+1s", "+10s"};
        for (int i = 0; i < deltas.length; i++) {
            final int d = deltas[i];
            this.addRenderableWidget(
                    Button.builder(Component.literal(deltaLabels[i]), btn -> {
                        this.delaySeconds = Mth.clamp(this.delaySeconds + d, 1, 300);
                        this.syncToServer();
                    }).bounds(fX, yo + 94, fW, 18).build()
            );
            fX += fW + 8;
        }

        // 4. Departure Direction Button (yo + 130)
        this.dirButton = this.addRenderableWidget(
                Button.builder(this.getDirectionButtonLabel(), btn -> {
                    this.releaseDirection = (this.releaseDirection + 1) % 4;
                    if (this.dirButton != null) {
                        this.dirButton.setMessage(this.getDirectionButtonLabel());
                    }
                    this.syncToServer();
                }).bounds(xo + 14, yo + 130, WINDOW_WIDTH - 28, 20).build()
        );

        // 5. Train Name Filter EditBox (yo + 164)
        this.filterBox = new EditBox(this.font, xo + 14, yo + 164, WINDOW_WIDTH - 28, 18, Component.literal("Train Filter"));
        this.filterBox.setMaxLength(32);
        this.filterBox.setHint(Component.literal("All Trains (* or empty)").withStyle(ChatFormatting.DARK_GRAY));
        this.filterBox.setValue(this.trainNameFilter);
        this.filterBox.setResponder(val -> {
            this.trainNameFilter = val;
            this.syncToServer();
        });
        this.addRenderableWidget(this.filterBox);

        // 6. Done button (yo + 188)
        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(xo + (WINDOW_WIDTH - 80) / 2, yo + 188, 80, 20)
                        .build()
        );
    }

    private Component getTimerToggleLabel() {
        if (this.timerEnabled) {
            return Component.literal("✔ Timer: ACTIVE (Hold Enabled)").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else {
            return Component.literal("✖ Timer: BYPASSED (Free Transit)").withStyle(ChatFormatting.RED);
        }
    }

    private Component getDirectionButtonLabel() {
        return switch (this.releaseDirection) {
            case DelayRailBlockEntity.DIR_FORWARD -> Component.literal("Departure: Force Forward").withStyle(ChatFormatting.GREEN);
            case DelayRailBlockEntity.DIR_REVERSE -> Component.literal("Departure: Force Reverse").withStyle(ChatFormatting.AQUA);
            case DelayRailBlockEntity.DIR_INVERT -> Component.literal("Departure: Invert / Reverse Heading").withStyle(ChatFormatting.LIGHT_PURPLE);
            default -> Component.literal("Departure: Keep Current Heading").withStyle(ChatFormatting.YELLOW);
        };
    }

    private void syncToServer() {
        ClientPacketDistributor.sendToServer(new DelayRailUpdatePayload(
                this.blockPos,
                this.timerEnabled,
                this.delaySeconds,
                this.releaseDirection,
                this.trainNameFilter
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.liveRemainingTicks > 0) {
            this.liveRemainingTicks--;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (this.filterBox != null && this.filterBox.isFocused()) {
            if (event.key() != InputConstants.KEY_ESCAPE) {
                return super.keyPressed(event);
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

        // Accent top bar (Gold / Amber)
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, 0xFFF59E0B);

        // Header Title
        graphics.text(this.font, Component.literal("Station Delay Rail").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), xo + 14, yo + 7, 0xFFFFFFFF, true);

        // Subtitle / Pos
        String sub = String.format("Pos: [%d, %d, %d]", this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        graphics.text(this.font, Component.literal(sub).withStyle(ChatFormatting.DARK_GRAY), xo + 14, yo + 19, 0xFF888888, false);

        // Live Status Badge at top right
        if (this.liveRemainingTicks > 0) {
            float secLeft = this.liveRemainingTicks / 20.0f;
            String liveText = String.format("Holding: %.1fs", secLeft);
            int badgeW = this.font.width(liveText) + 8;
            graphics.fill(xo + WINDOW_WIDTH - badgeW - 14, yo + 7, xo + WINDOW_WIDTH - 14, yo + 20, 0xFF78350F);
            graphics.text(this.font, Component.literal(liveText).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), xo + WINDOW_WIDTH - badgeW - 10, yo + 9, 0xFFFDE047, false);
        }

        // Section labels
        String delayHeader = "Holding Delay Duration: " + this.delaySeconds + "s";
        graphics.text(this.font, Component.literal(delayHeader).withStyle(ChatFormatting.WHITE), xo + 14, yo + 59, 0xFFE0E0E0, false);
        graphics.text(this.font, Component.literal("Departure Setting:").withStyle(ChatFormatting.WHITE), xo + 14, yo + 118, 0xFFE0E0E0, false);
        graphics.text(this.font, Component.literal("Train Filter (Locomotive / Consist Name):").withStyle(ChatFormatting.WHITE), xo + 14, yo + 152, 0xFFE0E0E0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
