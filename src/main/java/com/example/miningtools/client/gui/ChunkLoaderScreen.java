package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.ChunkLoaderBlockEntity;
import com.example.miningtools.network.ChunkLoaderSyncPayload;
import com.example.miningtools.network.ChunkLoaderUpdatePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ChunkLoaderScreen extends Screen {
    private static final int WINDOW_WIDTH = 220;
    private static final int WINDOW_HEIGHT = 175;

    private final BlockPos blockPos;
    private int radius;
    private boolean requiresRedstone;
    private boolean active;
    private int forcedChunkCount;

    private int leftPos;
    private int topPos;

    private @Nullable Button radiusButton;
    private @Nullable Button redstoneButton;
    private @Nullable Button doneButton;

    public ChunkLoaderScreen(BlockPos blockPos, int radius, boolean requiresRedstone, boolean active, int forcedChunkCount) {
        super(Component.translatable("gui.miningtools.chunk_loader.title"));
        this.blockPos = blockPos;
        this.radius = radius;
        this.requiresRedstone = requiresRedstone;
        this.active = active;
        this.forcedChunkCount = forcedChunkCount;
    }

    public static void open(ChunkLoaderSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new ChunkLoaderScreen(
                payload.pos(),
                payload.radius(),
                payload.requiresRedstone(),
                payload.active(),
                payload.forcedChunkCount()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        int xo = this.leftPos;
        int yo = this.topPos;

        // Radius cycle button (1x1 -> 3x3 -> 5x5)
        this.radiusButton = this.addRenderableWidget(
                Button.builder(this.getRadiusButtonLabel(), btn -> {
                    this.radius = (this.radius + 1) % 3;
                    if (this.radiusButton != null) {
                        this.radiusButton.setMessage(this.getRadiusButtonLabel());
                    }
                    this.syncToServer();
                }).bounds(xo + 14, yo + 62, WINDOW_WIDTH - 28, 20).build()
        );

        // Redstone Mode toggle button
        this.redstoneButton = this.addRenderableWidget(
                Button.builder(this.getRedstoneButtonLabel(), btn -> {
                    this.requiresRedstone = !this.requiresRedstone;
                    if (this.redstoneButton != null) {
                        this.redstoneButton.setMessage(this.getRedstoneButtonLabel());
                    }
                    this.syncToServer();
                }).bounds(xo + 14, yo + 90, WINDOW_WIDTH - 28, 20).build()
        );

        // Done button
        this.doneButton = this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(xo + (WINDOW_WIDTH - 80) / 2, yo + 140, 80, 20)
                        .build()
        );
    }

    private Component getRadiusButtonLabel() {
        return switch (this.radius) {
            case ChunkLoaderBlockEntity.RADIUS_1X1 -> Component.literal("Area: 1x1 Chunk (Single)").withStyle(ChatFormatting.AQUA);
            case ChunkLoaderBlockEntity.RADIUS_5X5 -> Component.literal("Area: 5x5 Chunks (25 Total)").withStyle(ChatFormatting.LIGHT_PURPLE);
            default -> Component.literal("Area: 3x3 Chunks (9 Total)").withStyle(ChatFormatting.GREEN);
        };
    }

    private Component getRedstoneButtonLabel() {
        if (this.requiresRedstone) {
            return Component.literal("Redstone: Required").withStyle(ChatFormatting.RED);
        } else {
            return Component.literal("Redstone: Always Active").withStyle(ChatFormatting.GREEN);
        }
    }

    private void syncToServer() {
        ClientPacketDistributor.sendToServer(new ChunkLoaderUpdatePayload(
                this.blockPos,
                this.radius,
                this.requiresRedstone
        ));
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

        // Background panel
        graphics.fill(xo - 2, yo - 2, xo + WINDOW_WIDTH + 2, yo + WINDOW_HEIGHT + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + WINDOW_WIDTH, yo + WINDOW_HEIGHT, 0xF21E222A);
        graphics.outline(xo, yo, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4B5362);

        // Accent top bar (cyan / ender teal)
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, 0xFF06B6D4);

        // Title
        graphics.text(this.font, Component.literal("Industrial Chunk Anchor").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), xo + 12, yo + 8, 0xFFFFFFFF, true);

        // Subtitle / Location details
        int chunkX = this.blockPos.getX() >> 4;
        int chunkZ = this.blockPos.getZ() >> 4;
        String chunkInfo = String.format("Chunk [%d, %d]  Pos [%d, %d, %d]", chunkX, chunkZ, this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        graphics.text(this.font, Component.literal(chunkInfo).withStyle(ChatFormatting.DARK_GRAY), xo + 12, yo + 22, 0xFF888888, false);

        // Status line
        int chunkTotal = switch (this.radius) {
            case ChunkLoaderBlockEntity.RADIUS_1X1 -> 1;
            case ChunkLoaderBlockEntity.RADIUS_5X5 -> 25;
            default -> 9;
        };
        Component statusComp;
        if (this.active) {
            statusComp = Component.literal("● ACTIVE - " + chunkTotal + " Chunks Force Loaded").withStyle(ChatFormatting.GREEN);
        } else {
            statusComp = Component.literal("○ INACTIVE (Needs Redstone Signal)").withStyle(ChatFormatting.RED);
        }
        graphics.text(this.font, statusComp, xo + 14, yo + 42, 0xFFFFFFFF, false);

        // Footer note
        graphics.text(this.font, Component.literal("Keeps mines & machines loaded").withStyle(ChatFormatting.DARK_GRAY), xo + 14, yo + 120, 0xFF666666, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
