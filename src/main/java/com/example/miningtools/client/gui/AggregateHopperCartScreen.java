package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.AggregateHopperCartMenu;
import com.example.miningtools.network.LocomotiveRenamePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class AggregateHopperCartScreen extends AbstractContainerScreen<AggregateHopperCartMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/gui/cargo_minecart.png");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;

    public AggregateHopperCartScreen(AggregateHopperCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.hopper_cart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Aggregate Hopper Cart")) {
                currentVal = titleStr;
            }
        }

        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 18, 96, 14, Component.literal("Cart Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Cart Name...").withStyle(ChatFormatting.DARK_GRAY));
        this.suppressResponder = true;
        this.nameBox.setValue(currentVal);
        this.suppressResponder = false;
        this.nameBox.setResponder(newVal -> {
            if (!this.suppressResponder) {
                ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(newVal));
            }
        });
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(
                Button.builder(Component.literal("Set"), button -> {
                    if (this.nameBox != null) {
                        ClientPacketDistributor.sendToServer(new LocomotiveRenamePayload(this.nameBox.getValue()));
                        this.nameBox.setFocused(false);
                    }
                }).bounds(this.leftPos + 139, this.topPos - 18, 33, 14).build()
        );
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component filterLabel = Component.literal("Aggregate Filter");
        int labelWidth = this.font.width(filterLabel);
        graphics.text(this.font, filterLabel, 152 - labelWidth - 4, 19, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE621252B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFF4B5362);
        graphics.text(this.font, Component.literal("Cart:"), xo + 5, yo - 15, 0xFFE6EDF3, true);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}