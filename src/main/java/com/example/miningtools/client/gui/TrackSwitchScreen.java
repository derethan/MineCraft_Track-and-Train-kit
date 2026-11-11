package com.example.miningtools.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.block.entity.TrackSwitchBlockEntity;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.CargoCondition;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.DirectionFilter;
import com.example.miningtools.block.entity.TrackSwitchBlockEntity.FuelCondition;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class TrackSwitchScreen extends Screen {
    private static final int WINDOW_WIDTH = 264;
    private static final int WINDOW_HEIGHT = 224;
    private static final int VISIBLE_ROWS = 4;

    private final BlockPos blockPos;
    private boolean currentOpen;
    private int detectRange;
    private int lockTicksRemaining;
    private final List<SwitchRule> rules;
    private int scrollOffset = 0;

    private DirectionFilter selectedDirection = DirectionFilter.ANY;
    private CargoCondition selectedCargo = CargoCondition.ANY;
    private FuelCondition selectedFuel = FuelCondition.ANY;

    private int leftPos;
    private int topPos;

    private @Nullable EditBox trainNameBox;
    private @Nullable EditBox delayBox;
    private @Nullable Button manualSwitchButton;
    private @Nullable Button rangeDownButton;
    private @Nullable Button rangeUpButton;
    private @Nullable Button scrollUpButton;
    private @Nullable Button scrollDownButton;

    private @Nullable Button dirCycleButton;
    private @Nullable Button cargoCycleButton;
    private @Nullable Button fuelCycleButton;

    private final Button[] rowStateButtons = new Button[VISIBLE_ROWS];
    private final Button[] rowDeleteButtons = new Button[VISIBLE_ROWS];

    public TrackSwitchScreen(BlockPos blockPos, boolean currentOpen, int detectRange, int lockTicksRemaining, List<SwitchRule> rules) {
        super(Component.translatable("gui.miningtools.track_switch.title"));
        this.blockPos = blockPos;
        this.currentOpen = currentOpen;
        this.detectRange = Mth.clamp(detectRange, TrackSwitchBlockEntity.MIN_DETECT_RANGE, TrackSwitchBlockEntity.MAX_DETECT_RANGE);
        this.lockTicksRemaining = Math.max(0, lockTicksRemaining);
        this.rules = new ArrayList<>(rules);
    }

    public TrackSwitchScreen(BlockPos blockPos, boolean currentOpen, int detectRange, List<SwitchRule> rules) {
        this(blockPos, currentOpen, detectRange, 0, rules);
    }

    public static void open(TrackSwitchSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new TrackSwitchScreen(
                payload.pos(),
                payload.currentOpen(),
                payload.detectRange(),
                payload.lockTicksRemaining(),
                payload.rules()
        ));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        String prevText = this.trainNameBox != null ? this.trainNameBox.getValue() : "";
        String prevDelay = this.delayBox != null ? this.delayBox.getValue() : "0";

        // Manual switch state toggle button
        this.manualSwitchButton = this.addRenderableWidget(
                Button.builder(this.getManualSwitchLabel(), btn -> {
                    this.currentOpen = !this.currentOpen;
                    this.lockTicksRemaining = 0;
                    this.refreshWidgetsState();
                    this.syncToServer();
                }).bounds(this.leftPos + 8, this.topPos + 20, 126, 16).build()
        );

        // Detection range [-] and [+] buttons
        this.rangeDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("-"), btn -> {
                    if (this.detectRange > TrackSwitchBlockEntity.MIN_DETECT_RANGE) {
                        this.detectRange--;
                        this.refreshWidgetsState();
                        this.syncToServer();
                    }
                }).bounds(this.leftPos + 188, this.topPos + 20, 16, 16).build()
        );

        this.rangeUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("+"), btn -> {
                    if (this.detectRange < TrackSwitchBlockEntity.MAX_DETECT_RANGE) {
                        this.detectRange++;
                        this.refreshWidgetsState();
                        this.syncToServer();
                    }
                }).bounds(this.leftPos + 236, this.topPos + 20, 16, 16).build()
        );

        // Train Name input box
        this.trainNameBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 49, 90, 16, Component.literal("Train Name"));
        this.trainNameBox.setMaxLength(32);
        this.trainNameBox.setHint(Component.literal("Train Name...").withStyle(ChatFormatting.DARK_GRAY));
        this.trainNameBox.setValue(prevText);
        this.addRenderableWidget(this.trainNameBox);

        // Lock Delay input box (seconds)
        this.delayBox = new EditBox(this.font, this.leftPos + 102, this.topPos + 49, 40, 16, Component.literal("Lock Delay"));
        this.delayBox.setMaxLength(3);
        this.delayBox.setValue(prevDelay);
        this.delayBox.setHint(Component.literal("0s").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.delayBox);

        // [+ Open] button
        this.addRenderableWidget(
                Button.builder(Component.literal("+ Open").withStyle(ChatFormatting.GREEN), btn -> {
                    this.addRuleFromInput(true);
                }).bounds(this.leftPos + 146, this.topPos + 48, 54, 18).build()
        );

        // [+ Close] button
        this.addRenderableWidget(
                Button.builder(Component.literal("+ Close").withStyle(ChatFormatting.RED), btn -> {
                    this.addRuleFromInput(false);
                }).bounds(this.leftPos + 204, this.topPos + 48, 52, 18).build()
        );

        // Condition cycle buttons (Direction, Cargo, Fuel)
        this.dirCycleButton = this.addRenderableWidget(
                Button.builder(this.getDirectionLabel(), btn -> {
                    this.selectedDirection = this.selectedDirection.next();
                    this.refreshWidgetsState();
                }).bounds(this.leftPos + 8, this.topPos + 69, 76, 16).build()
        );

        this.cargoCycleButton = this.addRenderableWidget(
                Button.builder(this.getCargoLabel(), btn -> {
                    this.selectedCargo = this.selectedCargo.next();
                    this.refreshWidgetsState();
                }).bounds(this.leftPos + 88, this.topPos + 69, 80, 16).build()
        );

        this.fuelCycleButton = this.addRenderableWidget(
                Button.builder(this.getFuelLabel(), btn -> {
                    this.selectedFuel = this.selectedFuel.next();
                    this.refreshWidgetsState();
                }).bounds(this.leftPos + 172, this.topPos + 69, 84, 16).build()
        );

        // Scroll Up / Down buttons
        this.scrollUpButton = this.addRenderableWidget(
                Button.builder(Component.literal("^"), btn -> {
                    if (this.scrollOffset > 0) {
                        this.scrollOffset--;
                        this.refreshWidgetsState();
                    }
                }).bounds(this.leftPos + 222, this.topPos + 89, 16, 12).build()
        );

        this.scrollDownButton = this.addRenderableWidget(
                Button.builder(Component.literal("v"), btn -> {
                    if (this.scrollOffset < Math.max(0, this.rules.size() - VISIBLE_ROWS)) {
                        this.scrollOffset++;
                        this.refreshWidgetsState();
                    }
                }).bounds(this.leftPos + 240, this.topPos + 89, 16, 12).build()
        );

        // 4 visible rule rows
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int rowIdx = i;
            int rowY = this.topPos + 105 + i * 20;

            this.rowStateButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.empty(), btn -> {
                        int ruleIndex = this.scrollOffset + rowIdx;
                        if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                            SwitchRule old = this.rules.get(ruleIndex);
                            this.rules.set(ruleIndex, new SwitchRule(
                                    old.trainName(),
                                    old.direction(),
                                    old.cargo(),
                                    old.fuel(),
                                    !old.openState(),
                                    old.delaySeconds()
                            ));
                            this.refreshWidgetsState();
                            this.syncToServer();
                        }
                    }).bounds(this.leftPos + 174, rowY + 1, 56, 16).build()
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
                    }).bounds(this.leftPos + 234, rowY + 1, 20, 16).build()
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

    private int getDelayFromBox() {
        if (this.delayBox == null) {
            return 0;
        }
        String s = this.delayBox.getValue().replaceAll("[^0-9]", "").trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Mth.clamp(Integer.parseInt(s), TrackSwitchBlockEntity.MIN_DELAY_SECONDS, TrackSwitchBlockEntity.MAX_DELAY_SECONDS);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void setDelayInBox(int sec) {
        if (this.delayBox != null) {
            int clamped = Mth.clamp(sec, TrackSwitchBlockEntity.MIN_DELAY_SECONDS, TrackSwitchBlockEntity.MAX_DELAY_SECONDS);
            this.delayBox.setValue(String.valueOf(clamped));
        }
    }

    private void addRuleFromInput(boolean openState) {
        if (this.trainNameBox == null) {
            return;
        }
        String name = this.trainNameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        int delay = this.getDelayFromBox();

        // If rule with same name and matching conditions already exists, update its switch state and delay
        for (int i = 0; i < this.rules.size(); i++) {
            SwitchRule existing = this.rules.get(i);
            if (existing.trainName().equalsIgnoreCase(name)
                    && existing.direction() == this.selectedDirection
                    && existing.cargo() == this.selectedCargo
                    && existing.fuel() == this.selectedFuel) {
                this.rules.set(i, new SwitchRule(name, this.selectedDirection, this.selectedCargo, this.selectedFuel, openState, delay));
                this.trainNameBox.setValue("");
                this.setDelayInBox(0);
                this.refreshWidgetsState();
                this.syncToServer();
                return;
            }
        }

        if (this.rules.size() < TrackSwitchBlockEntity.MAX_RULES) {
            this.rules.add(new SwitchRule(name, this.selectedDirection, this.selectedCargo, this.selectedFuel, openState, delay));
            this.scrollOffset = Math.max(0, this.rules.size() - VISIBLE_ROWS);
            this.trainNameBox.setValue("");
            this.setDelayInBox(0);
            this.refreshWidgetsState();
            this.syncToServer();
        }
    }

    private Component getManualSwitchLabel() {
        if (this.lockTicksRemaining > 0) {
            int sec = (this.lockTicksRemaining + 19) / 20;
            return this.currentOpen
                    ? Component.literal("Track: OPEN (" + sec + "s)").withStyle(ChatFormatting.YELLOW)
                    : Component.literal("Track: CLOSED (" + sec + "s)").withStyle(ChatFormatting.YELLOW);
        }
        return this.currentOpen
                ? Component.literal("Track: OPEN").withStyle(ChatFormatting.GREEN)
                : Component.literal("Track: CLOSED").withStyle(ChatFormatting.RED);
    }

    private Component getDirectionLabel() {
        return Component.literal(this.selectedDirection == DirectionFilter.ANY
                ? "Dir: All"
                : ("Dir: " + this.selectedDirection.getShortLabel()));
    }

    private Component getCargoLabel() {
        return Component.literal(this.selectedCargo.getShortLabel());
    }

    private Component getFuelLabel() {
        return Component.literal(this.selectedFuel.getShortLabel());
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
        if (this.dirCycleButton != null) {
            this.dirCycleButton.setMessage(this.getDirectionLabel());
        }
        if (this.cargoCycleButton != null) {
            this.cargoCycleButton.setMessage(this.getCargoLabel());
        }
        if (this.fuelCycleButton != null) {
            this.fuelCycleButton.setMessage(this.getFuelLabel());
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
    public void tick() {
        super.tick();
        if (this.lockTicksRemaining > 0) {
            this.lockTicksRemaining--;
            if (this.lockTicksRemaining % 20 == 0 || this.lockTicksRemaining == 0) {
                this.refreshWidgetsState();
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int xo = this.leftPos;
            int yo = this.topPos;
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int ruleIndex = this.scrollOffset + i;
                if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                    int rowY = yo + 105 + i * 20;
                    if (mouseX >= xo + 8 && mouseX < xo + 172 && mouseY >= rowY && mouseY <= rowY + 18) {
                        SwitchRule rule = this.rules.get(ruleIndex);
                        if (this.trainNameBox != null) {
                            this.trainNameBox.setValue(rule.trainName());
                        }
                        this.setDelayInBox(rule.delaySeconds());
                        this.selectedDirection = rule.direction();
                        this.selectedCargo = rule.cargo();
                        this.selectedFuel = rule.fuel();
                        this.refreshWidgetsState();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.delayBox != null && this.delayBox.isMouseOver(mouseX, mouseY)) {
            int current = this.getDelayFromBox();
            this.setDelayInBox(current + (scrollY > 0 ? 1 : -1));
            return true;
        }
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
        if (this.delayBox != null && this.delayBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.addRuleFromInput(true);
                return true;
            }
            if (event.key() == InputConstants.KEY_UP) {
                int delta = event.hasShiftDown() ? 5 : 1;
                this.setDelayInBox(this.getDelayFromBox() + delta);
                return true;
            }
            if (event.key() == InputConstants.KEY_DOWN) {
                int delta = event.hasShiftDown() ? 5 : 1;
                this.setDelayInBox(this.getDelayFromBox() - delta);
                return true;
            }
            if (this.delayBox.keyPressed(event) || this.delayBox.canConsumeInput()) {
                return true;
            }
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
        int listY = yo + 103;
        int listW = WINDOW_WIDTH - 12;
        int listH = VISIBLE_ROWS * 20 + 2;
        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF13161C);
        graphics.outline(listX, listY, listW, listH, 0xFF30363D);

        // Individual rule row backgrounds
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int ruleIndex = this.scrollOffset + i;
            if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                SwitchRule rule = this.rules.get(ruleIndex);
                int rowY = yo + 105 + i * 20;
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
        graphics.text(this.font, Component.literal("Sense:"), xo + 144, yo + 24, 0xFFB1BAC4, true);
        String rangeStr = this.detectRange + "m";
        int rW = this.font.width(rangeStr);
        graphics.text(this.font, rangeStr, xo + 206 + (28 - rW) / 2, yo + 24, 0xFF79C0FF, true);

        // Input section labels
        graphics.text(this.font, Component.literal("Train Name:"), xo + 8, yo + 38, 0xFFB1BAC4, true);
        graphics.text(this.font, Component.literal("Lock (s):"), xo + 102, yo + 38, 0xFFB1BAC4, true);

        // Routing List header
        String listHeader = "Train Routing List (" + this.rules.size() + "):";
        graphics.text(this.font, listHeader, xo + 8, yo + 91, 0xFFE6EDF3, true);

        // Rule rows text and badges
        if (this.rules.isEmpty()) {
            String emptyMsg = "No train rules yet. Add a train rule above!";
            int eW = this.font.width(emptyMsg);
            graphics.text(this.font, emptyMsg, xo + (WINDOW_WIDTH - eW) / 2, yo + 138, 0xFF6E7681, false);
        } else {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int ruleIndex = this.scrollOffset + i;
                if (ruleIndex >= 0 && ruleIndex < this.rules.size()) {
                    SwitchRule rule = this.rules.get(ruleIndex);
                    int rowY = yo + 105 + i * 20;

                    // Train name with index
                    String label = (ruleIndex + 1) + ". " + rule.trainName();
                    if (this.font.width(label) > 58) {
                        label = this.font.plainSubstrByWidth(label, 52) + "..";
                    }
                    graphics.text(this.font, label, xo + 14, rowY + 5, 0xFFF0F6FC, true);

                    // Compact criteria badges
                    int badgeX = xo + 74;

                    // 1. Direction badge
                    if (rule.direction() != DirectionFilter.ANY) {
                        String b = rule.direction().getShortLabel();
                        int bw = this.font.width(b) + 4;
                        graphics.fill(badgeX, rowY + 3, badgeX + bw, rowY + 15, 0xFF1D4ED8);
                        graphics.outline(badgeX, rowY + 3, bw, 12, 0xFF3B82F6);
                        graphics.text(this.font, b, badgeX + 2, rowY + 5, 0xFFFFFFFF, true);
                        badgeX += bw + 2;
                    }

                    // 2. Cargo badge
                    if (rule.cargo() != CargoCondition.ANY) {
                        String b = rule.cargo() == CargoCondition.LOADED ? "FULL" : "MT";
                        int bw = this.font.width(b) + 4;
                        int bg = rule.cargo() == CargoCondition.LOADED ? 0xFFB45309 : 0xFF6D28D9;
                        int border = rule.cargo() == CargoCondition.LOADED ? 0xFFF59E0B : 0xFFA78BFA;
                        graphics.fill(badgeX, rowY + 3, badgeX + bw, rowY + 15, bg);
                        graphics.outline(badgeX, rowY + 3, bw, 12, border);
                        graphics.text(this.font, b, badgeX + 2, rowY + 5, 0xFFFFFFFF, true);
                        badgeX += bw + 2;
                    }

                    // 3. Fuel badge
                    if (rule.fuel() != FuelCondition.ANY) {
                        String b = switch (rule.fuel()) {
                            case LESS_THAN_25 -> "<25%";
                            case LESS_THAN_50 -> "<50%";
                            case GREATER_THAN_25 -> ">25%";
                            case GREATER_THAN_50 -> ">50%";
                            default -> "";
                        };
                        int bw = this.font.width(b) + 4;
                        graphics.fill(badgeX, rowY + 3, badgeX + bw, rowY + 15, 0xFFC2410C);
                        graphics.outline(badgeX, rowY + 3, bw, 12, 0xFFFB923C);
                        graphics.text(this.font, b, badgeX + 2, rowY + 5, 0xFFFFFFFF, true);
                        badgeX += bw + 2;
                    }

                    // 4. Lock Delay badge
                    if (rule.delaySeconds() > 0) {
                        String b = rule.delaySeconds() + "s";
                        int bw = this.font.width(b) + 4;
                        graphics.fill(badgeX, rowY + 3, badgeX + bw, rowY + 15, 0xFF0E7490);
                        graphics.outline(badgeX, rowY + 3, bw, 12, 0xFF06B6D4);
                        graphics.text(this.font, b, badgeX + 2, rowY + 5, 0xFFFFFFFF, true);
                        badgeX += bw + 2;
                    }

                    // Fallback badge if all are ANY and delay is 0
                    if (rule.direction() == DirectionFilter.ANY && rule.cargo() == CargoCondition.ANY && rule.fuel() == FuelCondition.ANY && rule.delaySeconds() == 0) {
                        String b = "ALL";
                        int bw = this.font.width(b) + 4;
                        graphics.fill(badgeX, rowY + 3, badgeX + bw, rowY + 15, 0xFF374151);
                        graphics.outline(badgeX, rowY + 3, bw, 12, 0xFF4B5563);
                        graphics.text(this.font, b, badgeX + 2, rowY + 5, 0xFFD1D5DB, true);
                    }

                    // Row hover tooltip
                    if (mouseX >= xo + 8 && mouseX < xo + 172 && mouseY >= rowY && mouseY <= rowY + 18) {
                        String lockDesc = rule.delaySeconds() > 0 ? (rule.delaySeconds() + "s locked on activation") : "None (instant / unlocked)";
                        graphics.setTooltipForNextFrame(
                                Component.literal("Rule #" + (ruleIndex + 1) + ": " + rule.trainName() + "\n"
                                        + "Switch: " + (rule.openState() ? "OPEN (Switched)" : "CLOSED (Normal)") + "\n"
                                        + "Lock Delay: " + lockDesc + "\n"
                                        + "Heading: " + rule.direction().getSerializedName() + "\n"
                                        + "Cargo: " + rule.cargo().getSerializedName() + "\n"
                                        + "Fuel: " + rule.fuel().getSerializedName() + "\n"
                                        + "§7(Click to load into editor)"),
                                mouseX, mouseY
                        );
                    }
                }
            }
        }

        // Tooltips for condition buttons
        if (this.dirCycleButton != null && this.dirCycleButton.isMouseOver(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    Component.literal("Filter: Train Travel Direction\nCycles: All -> North -> South -> East -> West"),
                    mouseX, mouseY
            );
        } else if (this.cargoCycleButton != null && this.cargoCycleButton.isMouseOver(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    Component.literal("Filter: Freight Cargo Status\nCycles: Any -> Loaded -> Empty"),
                    mouseX, mouseY
            );
        } else if (this.fuelCycleButton != null && this.fuelCycleButton.isMouseOver(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    Component.literal("Filter: Locomotive Fuel/Battery Level\nCycles: Any -> <25% -> <50% -> >25% -> >50%"),
                    mouseX, mouseY
            );
        } else if (this.delayBox != null && this.delayBox.isMouseOver(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    Component.literal("Route Lock Delay (seconds):\nHow long the track stays locked to this position when this route activates.\n0 = Instant / No lock.\nScroll mouse wheel or use arrow keys to adjust."),
                    mouseX, mouseY
            );
        } else if (this.trainNameBox != null && this.trainNameBox.isMouseOver(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    Component.literal("Train Name Filter:\nEnter exact train name, prefix wildcard (e.g. Ore*), or * for all trains."),
                    mouseX, mouseY
            );
        } else if (this.manualSwitchButton != null && this.manualSwitchButton.isMouseOver(mouseX, mouseY) && this.lockTicksRemaining > 0) {
            int sec = (this.lockTicksRemaining + 19) / 20;
            graphics.setTooltipForNextFrame(
                    Component.literal("Track switch is currently LOCKED by an active train route.\nRemaining lock time: " + sec + "s\nClick to manually unlock/toggle."),
                    mouseX, mouseY
            );
        }

        // Footer tip
        graphics.text(this.font, Component.literal("Tip: Click rule to edit • Scroll over delay"), xo + 8, yo + WINDOW_HEIGHT - 18, 0xFF8B949E, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
