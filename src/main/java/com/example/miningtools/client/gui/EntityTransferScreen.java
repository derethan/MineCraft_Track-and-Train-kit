package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.AbstractEntityTransferBlockEntity;
import com.example.miningtools.network.EntityTransferSyncPayload;
import com.example.miningtools.network.EntityTransferUpdatePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class EntityTransferScreen extends Screen {
    private static final int WINDOW_WIDTH = 224;
    private static final int WINDOW_HEIGHT = 196;

    private final BlockPos blockPos;
    private final boolean isUnloader;
    private String whitelist;
    private String blacklist;
    private int ageFilter;
    private int range;

    private int leftPos;
    private int topPos;

    private @Nullable EditBox whitelistBox;
    private @Nullable EditBox blacklistBox;
    private @Nullable Button ageButton;
    private @Nullable Button rangeDownButton;
    private @Nullable Button rangeUpButton;
    private @Nullable Button doneButton;

    public EntityTransferScreen(BlockPos blockPos, boolean isUnloader, String whitelist, String blacklist, int ageFilter, int range) {
        super(Component.translatable(isUnloader ? "gui.miningtools.entity_unloader.title" : "gui.miningtools.entity_loader.title"));
        this.blockPos = blockPos;
        this.isUnloader = isUnloader;
        this.whitelist = whitelist != null ? whitelist : "";
        this.blacklist = blacklist != null ? blacklist : "";
        this.ageFilter = ageFilter;
        this.range = Mth.clamp(range, AbstractEntityTransferBlockEntity.MIN_RANGE, AbstractEntityTransferBlockEntity.MAX_RANGE);
    }

    public static void open(EntityTransferSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new EntityTransferScreen(
                payload.pos(),
                payload.isUnloader(),
                payload.whitelist(),
                payload.blacklist(),
                payload.ageFilter(),
                payload.range()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        int xo = this.leftPos;
        int yo = this.topPos;

        // 1. Whitelist input box
        this.whitelistBox = new EditBox(this.font, xo + 12, yo + 38, WINDOW_WIDTH - 24, 18, Component.literal("Whitelist"));
        this.whitelistBox.setMaxLength(256);
        this.whitelistBox.setValue(this.whitelist);
        this.whitelistBox.setHint(Component.literal("e.g. rabbit, cow").withStyle(ChatFormatting.DARK_GRAY));
        this.whitelistBox.setResponder(text -> {
            this.whitelist = text;
            this.syncToServer();
        });
        this.addRenderableWidget(this.whitelistBox);

        // 2. Blacklist input box
        this.blacklistBox = new EditBox(this.font, xo + 12, yo + 80, WINDOW_WIDTH - 24, 18, Component.literal("Blacklist"));
        this.blacklistBox.setMaxLength(256);
        this.blacklistBox.setValue(this.blacklist);
        this.blacklistBox.setHint(Component.literal("e.g. player_name").withStyle(ChatFormatting.DARK_GRAY));
        this.blacklistBox.setResponder(text -> {
            this.blacklist = text;
            this.syncToServer();
        });
        this.addRenderableWidget(this.blacklistBox);

        // 3. Age filter cycle button
        this.ageButton = this.addRenderableWidget(
                Button.builder(this.getAgeButtonLabel(), btn -> {
                    this.ageFilter = (this.ageFilter + 1) % 3;
                    btn.setMessage(this.getAgeButtonLabel());
                    this.syncToServer();
                }).bounds(xo + 12, yo + 120, 110, 20).build()
        );

        // 4. Range controls: [-] [Range: 5] [+]
        this.rangeDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("-"), btn -> {
                    if (this.range > AbstractEntityTransferBlockEntity.MIN_RANGE) {
                        this.range--;
                        this.syncToServer();
                    }
                }).bounds(xo + 154, yo + 120, 20, 20).build()
        );

        this.rangeUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("+"), btn -> {
                    if (this.range < AbstractEntityTransferBlockEntity.MAX_RANGE) {
                        this.range++;
                        this.syncToServer();
                    }
                }).bounds(xo + 192, yo + 120, 20, 20).build()
        );

        // 5. Done button
        this.doneButton = this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(xo + (WINDOW_WIDTH - 80) / 2, yo + 160, 80, 20)
                        .build()
        );
    }

    private Component getAgeButtonLabel() {
        return switch (this.ageFilter) {
            case AbstractEntityTransferBlockEntity.AGE_ADULT_ONLY -> Component.literal("Adults Only").withStyle(ChatFormatting.GREEN);
            case AbstractEntityTransferBlockEntity.AGE_BABY_ONLY -> Component.literal("Babies Only").withStyle(ChatFormatting.YELLOW);
            default -> Component.literal("Age: Both").withStyle(ChatFormatting.WHITE);
        };
    }

    private void syncToServer() {
        ClientPacketDistributor.sendToServer(new EntityTransferUpdatePayload(
                this.blockPos,
                this.whitelist,
                this.blacklist,
                this.ageFilter,
                this.range
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

        // Accent top bar
        int accent = this.isUnloader ? 0xFFEAB308 : 0xFF22C55E;
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, accent);

        // Title
        String titleStr = this.isUnloader ? "Entity Unloader" : "Entity Loader";
        graphics.text(this.font, Component.literal(titleStr).withStyle(ChatFormatting.BOLD, this.isUnloader ? ChatFormatting.GOLD : ChatFormatting.GREEN), xo + 12, yo + 8, 0xFFFFFFFF, true);

        // Field labels
        graphics.text(this.font, Component.literal("Whitelist (allow only):").withStyle(ChatFormatting.AQUA), xo + 12, yo + 26, 0xFFCCCCCC, false);
        graphics.text(this.font, Component.literal("Blacklist (never transfer):").withStyle(ChatFormatting.RED), xo + 12, yo + 68, 0xFFCCCCCC, false);
        graphics.text(this.font, Component.literal("Age:").withStyle(ChatFormatting.GRAY), xo + 12, yo + 109, 0xFFCCCCCC, false);

        // Range text
        String rangeStr = "Range: " + this.range;
        graphics.text(this.font, Component.literal(rangeStr).withStyle(ChatFormatting.WHITE), xo + 140, yo + 109, 0xFFCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
