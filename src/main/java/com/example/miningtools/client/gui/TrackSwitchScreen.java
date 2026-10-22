package com.example.miningtools.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.TrackSwitchBlockEntity;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.SwitchRule;
import com.example.miningtools.network.TrackSwitchSyncPayload;
import com.example.miningtools.network.TrackSwitchUpdatePayload;
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
public class TrackSwitchScreen extends Screen {
    private static final int WINDOW_WIDTH = 236;
    private static final int WINDOW_HEIGHT = 200;
    private static final int VISIBLE_ROWS = 4;

    private final BlockPos blockPos;
    private boolean currentOpen;
    private int detectRange;
    private final List<SwitchRule> rules;
    private int scrollOffset = 0;

    private int leftPos;
    private int topPos;

    private @Nullable EditBox trainNameBox;
    private @Nullable Button manualSwitchButton;
    private @Nullable Button rangeDownButton;
    private @Nullable Button rangeUpButton;
    private @Nullable Button scrollUpButton;
    private @Nullable Button scrollDownButton;

    private final Button[] rowStateButtons = new Button[VISIBLE_ROWS];
    private final Button[] rowDeleteButtons = new Button[VISIBLE_ROWS];

    public TrackSwitchScreen(BlockPos blockPos, boolean currentOpen, int detectRange, List<SwitchRule> rules) {
        super(Component.translatable("gui.miningtools.track_switch.title"));
        this.blockPos = blockPos;
        this.currentOpen = currentOpen;
        this.detectRange = Mth.clamp(detectRange, TrackSwitchBlockEntity.MIN_DETECT_RANGE, TrackSwitchBlockEntity.MAX_DETECT_RANGE);
        this.rules = new ArrayList<>(rules);
    }

    public static void open(TrackSwitchSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new TrackSwitchScreen(
                payload.pos(),
                payload.currentOpen(),
                payload.detectRange(),
                payload.rules()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        String prevText = this.trainNameBox != null ? this.trainNameBox.getValue() : "";

        // Manual switch state toggle button
        this.manualSwitchButton = this.addRenderableWidget(
                Button.builder(this.getManualSwitchLabel(), btn -> {
                    this.currentOpen = !this.currentOpen;
                    this.refreshWidgetsState();
                    this.syncToServer();
                }).bounds(this.leftPos + 8, this.topPos + 20, 114, 16).build()
        );

        // Detection range [-] and [+] buttons
        this.rangeDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("-"), btn -> {
                    if (this.detectRange > TrackSwitchBlockEntity.MIN_DETECT_RANGE) {
                        this.detectRange--;
                        this.refreshWidgetsState();
                        this.syncToServer();
                    }
                }).bounds(this.leftPos + 170, this.topPos + 20, 16, 16).build()
        );

        this.rangeUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("+"), btn -> {
                    if (this.detectRange < TrackSwitchBlockEntity.MAX_DETECT_RANGE) {
                        this.detectRange++;
                        this.refreshWidgetsState();
                        this.syncToServer();
                    }
                }).bounds(this.leftPos + 212, this.topPos + 20, 16, 16).build()
        );

        // Train Name input box
        this.trainNameBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 53, 120, 16, Component.literal("Train Name"));
        this.trainNameBox.setMaxLength(32);
        this.trainNameBox.setHint(Component.literal("Train Name (or *)...").withStyle(ChatFormatting.DARK_GRAY));
        this.trainNameBox.setValue(prevText);
        this.addRenderableWidget(this.trainNameBox);

        // [+ Open] button
        this.addRenderableWidget(
                Button.builder(Component.literal("+ Open").withStyle(ChatFormatting.GREEN), btn -> {
                    this.addRuleFromInput(true);
                }).bounds(this.leftPos + 132, this.topPos + 52, 46, 18).build()
        );

        // [+ Close] button
        this.addRenderableWidget(
                Button.builder(Component.literal("+ Close").withStyle(ChatFormatting.RED), btn -> {
                    this.addRuleFromInput(false);
                }).bounds(this.leftPos + 181, this.topPos + 52, 47, 18).build()
        );

        // Scroll Up / Down buttons
        this.scrollUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("^"), btn -> {
                    if (this.scrollOffset > 0) {
                        this.scrollOffset--;
                        this.refreshWidgetsState();
                    }
                }).bounds(this.leftPos + 194, this.topPos + 73, 16, 12).build()
        );

        this.scrollDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("v"), btn -> {
                    if (this.scrollOffset < Math.max(0, this.rules.size() - VISIBLE_ROWS)) {
                        this.scrollOffset++;
                        this.refreshWidgetsState();
                    }
                }).bounds(this.leftPos + 212, this.topPos + 73, 16, 12).build()
        );

        // 4 visible rule rows
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int rowIdx = i;
            int rowY = this.topPos + 88 + i * 20;

            this.rowStateButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.empty(), btn -> {
                        int ruleIndex = this.scrollOffset + rowIdx;
                        if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                            SwitchRule old = this.rules.get(ruleIndex);
                            this.rules.set(ruleIndex, new SwitchRule(old.trainName(), !old.openState()));
                            this.refreshWidgetsState();
                            this.syncToServer();
                        }
                    }).bounds(this.leftPos + 144, rowY + 1, 58, 16).build()
            );

            this.rowDeleteButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), btn -> {
                        int ruleIndex = this.scrollOffset + rowIdx;
                        if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                            this.rules.remove(ruleIndex);
                            int maxScroll = Math.max(0, this.rules.size() - VISIBLE_ROWS);
                            if (this.scrollOffset > maxScroll) {
                                this.scrollOffset = maxScroll;
                            }
                            this.refreshWidgetsState();
                            this.syncToServer();
                        }
                    }).bounds(this.leftPos + 205, rowY + 1, 20, 16).build()
            );
        }

        // Done button
        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(this.leftPos + WINDOW_WIDTH - 58, this.topPos + WINDOW_HEIGHT - 24, 50, 18)
                        .build()
        );

        this.setInitialFocus(this.trainNameBox);
        this.refreshWidgetsState();
    }

    private void addRuleFromInput(boolean openState) {
        if (this.trainNameBox == null) {
            return;
        }
        String name = this.trainNameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        // If rule with same name already exists, update its state
        for (int i = 0; i < this.rules.size(); i++) {
            if (this.rules.get(i).trainName().equalsIgnoreCase(name)) {
                this.rules.set(i, new SwitchRule(name, openState));
                this.trainNameBox.setValue("");
                this.refreshWidgetsState();
                this.syncToServer();
                return;
            }
        }
        if (this.rules.size() < TrackSwitchBlockEntity.MAX_RULES) {
            this.rules.add(new SwitchRule(name, openState));
            this.scrollOffset = Math.max(0, this.rules.size() - VISIBLE_ROWS);
            this.trainNameBox.setValue("");
            this.refreshWidgetsState();
            this.syncToServer();
        }
    }

    private Component getManualSwitchLabel() {
        return this.currentOpen
                ? Component.literal("Track: OPEN").withStyle(ChatFormatting.GREEN)
                : Component.literal("Track: CLOSED").withStyle(ChatFormatting.RED);
    }

    private void refreshWidgetsState() {
        if (this.manualSwitchButton != null) {
            this.manualSwitchButton.setMessage(this.getManualSwitchLabel());
        }
        if (this.rangeDownButton != null) {
            this.rangeDownButton.active = this.detectRange > TrackSwitchBlockEntity.MIN_DETECT_RANGE;
        }
        if (this.rangeUpButton != null) {
            this.rangeUpButton.active = this.detectRange < TrackSwitchBlockEntity.MAX_DETECT_RANGE;
        }
        int maxScroll = Math.max(0, this.rules.size() - VISIBLE_ROWS);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);
        if (this.scrollUpButton != null) {
            this.scrollUpButton.active = this.scrollOffset > 0;
        }
        if (this.scrollDownButton != null) {
            this.scrollDownButton.active = this.scrollOffset < maxScroll;
        }

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int ruleIndex = this.scrollOffset + i;
            boolean hasRule = ruleIndex >= 0 && ruleIndex < this.rules.size();
            Button stateBtn = this.rowStateButtons[i];
            Button delBtn = this.rowDeleteButtons[i];
            if (stateBtn != null) {
                stateBtn.visible = hasRule;
                if (hasRule) {
                    boolean isOpen = this.rules.get(ruleIndex).openState();
                    stateBtn.setMessage(isOpen
                            ? Component.literal("OPEN").withStyle(ChatFormatting.GREEN)
                            : Component.literal("CLOSED").withStyle(ChatFormatting.RED));
                }
            }
            if (delBtn != null) {
                delBtn.visible = hasRule;
            }
        }
    }

    private void syncToServer() {
        ClientPacketDistributor.sendToServer(new TrackSwitchUpdatePayload(
                this.blockPos,
                this.currentOpen,
                this.detectRange,
                List.copyOf(this.rules)
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.rules.size() > VISIBLE_ROWS) {
            int maxScroll = Math.max(0, this.rules.size() - VISIBLE_ROWS);
            if (scrollY < 0 && this.scrollOffset < maxScroll) {
                this.scrollOffset++;
                this.refreshWidgetsState();
                return true;
            } else if (scrollY > 0 && this.scrollOffset > 0) {
                this.scrollOffset--;
                this.refreshWidgetsState();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (this.trainNameBox != null && this.trainNameBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.addRuleFromInput(true);
                return true;
            }
            if (this.trainNameBox.keyPressed(event) || this.trainNameBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Main outer panel
        graphics.fill(xo - 2, yo - 2, xo + WINDOW_WIDTH + 2, yo + WINDOW_HEIGHT + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + WINDOW_WIDTH, yo + WINDOW_HEIGHT, 0xF21E222A);
        graphics.outline(xo, yo, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4B5362);

        // Top accent bar matching current switch state
        int accentColor = this.currentOpen ? 0xFF2EA043 : 0xFFF85149;
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, accentColor);

        // List container box
        int listX = xo + 6;
        int listY = yo + 86;
        int listW = WINDOW_WIDTH - 12;
        int listH = VISIBLE_ROWS * 20 + 2;
        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF13161C);
        graphics.outline(listX, listY, listW, listH, 0xFF30363D);

        // Individual rule row backgrounds
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int ruleIndex = this.scrollOffset + i;
            if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                SwitchRule rule = this.rules.get(ruleIndex);
                int rowY = yo + 88 + i * 20;
                graphics.fill(xo + 8, rowY, xo + WINDOW_WIDTH - 8, rowY + 18, 0xFF1C2128);
                int statusStripe = rule.openState() ? 0xFF2EA043 : 0xFFF85149;
                graphics.fill(xo + 8, rowY, xo + 11, rowY + 18, statusStripe);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Title
        graphics.text(this.font, this.title, xo + 8, yo + 7, 0xFFE6EDF3, true);

        // Sensor Range label & current value
        graphics.text(this.font, Component.literal("Sense:"), xo + 130, yo + 24, 0xFFB1BAC4, true);
        String rangeStr = this.detectRange + "m";
        int rW = this.font.width(rangeStr);
        graphics.text(this.font, rangeStr, xo + 188 + (22 - rW) / 2, yo + 24, 0xFF79C0FF, true);

        // Add Train Rule section label
        graphics.text(this.font, Component.literal("Type Train Name & Choose Track State:"), xo + 8, yo + 41, 0xFFB1BAC4, true);

        // Routing List header
        String listHeader = "Train Routing List (" + this.rules.size() + "):";
        graphics.text(this.font, listHeader, xo + 8, yo + 75, 0xFFE6EDF3, true);

        // Rule rows text
        if (this.rules.isEmpty()) {
            String emptyMsg = "No train rules yet. Add a train name above!";
            int eW = this.font.width(emptyMsg);
            graphics.text(this.font, emptyMsg, xo + (WINDOW_WIDTH - eW) / 2, yo + 122, 0xFF6E7681, false);
        } else {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int ruleIndex = this.scrollOffset + i;
                if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                    SwitchRule rule = this.rules.get(ruleIndex);
                    int rowY = yo + 88 + i * 20;
                    String label = (ruleIndex + 1) + ". " + rule.trainName();
                    if (this.font.width(label) > 124) {
                        label = this.font.plainSubstrByWidth(label, 118) + "..";
                    }
                    graphics.text(this.font, label, xo + 15, rowY + 5, 0xFFF0F6FC, true);
                }
            }
        }

        // Footer tip
        graphics.text(this.font, Component.literal("Name trains in Locomotive GUI"), xo + 8, yo + WINDOW_HEIGHT - 19, 0xFF8B949E, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
