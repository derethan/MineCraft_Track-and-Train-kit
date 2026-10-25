package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.network.CartRenamePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class CartRenameScreen extends Screen {
    private static final int WINDOW_WIDTH = 216;
    private static final int WINDOW_HEIGHT = 128;

    private final int entityId;
    private String cartName;
    private final String typeTitle;
    private final String statusLine;

    private int leftPos;
    private int topPos;
    private @Nullable EditBox nameBox;

    public CartRenameScreen(int entityId, String currentName, String typeTitle, String statusLine) {
        super(Component.literal(typeTitle));
        this.entityId = entityId;
        this.cartName = currentName != null ? currentName : "";
        this.typeTitle = typeTitle;
        this.statusLine = statusLine != null ? statusLine : "";
    }

    public static void open(com.example.miningtools.network.CartRenameOpenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new CartRenameScreen(payload.entityId(), payload.currentName(), payload.typeTitle(), payload.statusLine()));
    }

    public static void openForCart(AbstractMinecart cart, String statusLine) {
        Minecraft mc = Minecraft.getInstance();
        String existingName = cart.getCustomName() != null ? cart.getCustomName().getString().trim() : "";
        String typeTitle = cart.getType().getDescription().getString();
        mc.gui.setScreen(new CartRenameScreen(cart.getId(), existingName, typeTitle, statusLine));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        int xo = this.leftPos;
        int yo = this.topPos;

        this.nameBox = new EditBox(this.font, xo + 14, yo + 52, WINDOW_WIDTH - 68, 18, Component.literal("Cart Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Cart Name...").withStyle(ChatFormatting.DARK_GRAY));
        this.nameBox.setValue(this.cartName);
        this.nameBox.setResponder(val -> {
            this.cartName = val;
            ClientPacketDistributor.sendToServer(new CartRenamePayload(this.entityId, val));
        });
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(
                Button.builder(Component.literal("Set"), btn -> {
                    if (this.nameBox != null) {
                        ClientPacketDistributor.sendToServer(new CartRenamePayload(this.entityId, this.nameBox.getValue()));
                        this.nameBox.setFocused(false);
                    }
                }).bounds(xo + WINDOW_WIDTH - 50, yo + 52, 36, 18).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> this.onClose())
                        .bounds(xo + (WINDOW_WIDTH - 80) / 2, yo + 94, 80, 20)
                        .build()
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                ClientPacketDistributor.sendToServer(new CartRenamePayload(this.entityId, this.nameBox.getValue()));
                this.nameBox.setFocused(false);
                return true;
            }
            if (this.nameBox.keyPressed(event) || this.nameBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.nameBox != null) {
            ClientPacketDistributor.sendToServer(new CartRenamePayload(this.entityId, this.nameBox.getValue()));
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.fill(xo - 2, yo - 2, xo + WINDOW_WIDTH + 2, yo + WINDOW_HEIGHT + 2, 0xCC0D1117);
        graphics.fill(xo, yo, xo + WINDOW_WIDTH, yo + WINDOW_HEIGHT, 0xF21E222A);
        graphics.outline(xo, yo, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4B5362);
        graphics.fill(xo + 1, yo + 1, xo + WINDOW_WIDTH - 1, yo + 3, 0xFF38BDF8);

        graphics.text(this.font, Component.literal(this.typeTitle).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), xo + 14, yo + 10, 0xFFFFFFFF, true);
        if (!this.statusLine.isEmpty()) {
            graphics.text(this.font, Component.literal(this.statusLine).withStyle(ChatFormatting.GRAY), xo + 14, yo + 24, 0xFFCCCCCC, false);
        }
        graphics.text(this.font, Component.literal("Cart Name (for Loaders & Switches):").withStyle(ChatFormatting.YELLOW), xo + 14, yo + 39, 0xFFE6EDF3, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
