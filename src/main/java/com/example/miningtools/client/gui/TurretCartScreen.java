package com.example.miningtools.client.gui;

import java.util.Comparator;
import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.entity.TurretCartEntity;
import com.example.miningtools.menu.TurretCartMenu;
import com.example.miningtools.network.LocomotiveRenamePayload;
import com.example.miningtools.network.TurretCartFilterPayload;
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
public class TurretCartScreen extends AbstractContainerScreen<TurretCartMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/gui/turret_cart.png");

    private @Nullable EditBox nameBox;
    private @Nullable EditBox filterBox;
    private @Nullable Button modeButton;
    private @Nullable TurretCartEntity cachedCart;

    private boolean initializedName = false;
    private boolean suppressResponder = false;

    private int filterMode = TurretCartEntity.FILTER_MODE_WHITELIST;
    private String whitelist = "";
    private String blacklist = "";

    public TurretCartScreen(TurretCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            this.cachedCart = this.minecraft.level.getEntitiesOfClass(TurretCartEntity.class, this.minecraft.player.getBoundingBox().inflate(8.0D))
                    .stream()
                    .min(Comparator.comparingDouble(c -> c.distanceToSqr(this.minecraft.player)))
                    .orElse(null);
        }

        if (this.cachedCart != null) {
            this.filterMode = this.cachedCart.getFilterMode();
            this.whitelist = this.cachedCart.getWhitelist();
            this.blacklist = this.cachedCart.getBlacklist();
        }

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.turret_cart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Turret Cart")) {
                currentVal = titleStr;
            }
        }

        // Cart Name Row (Y = topPos - 36)
        this.nameBox = new EditBox(this.font, this.leftPos + 40, this.topPos - 36, 96, 14, Component.literal("Cart Name"));
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
                }).bounds(this.leftPos + 139, this.topPos - 36, 33, 14).build()
        );

        // Filter Mode Toggle Button (Y = topPos - 19)
        this.modeButton = this.addRenderableWidget(
                Button.builder(this.getModeButtonText(), button -> {
                    this.filterMode = (this.filterMode == TurretCartEntity.FILTER_MODE_WHITELIST)
                            ? TurretCartEntity.FILTER_MODE_BLACKLIST
                            : TurretCartEntity.FILTER_MODE_WHITELIST;
                    button.setMessage(this.getModeButtonText());
                    this.updateFilterBoxText();
                    this.sendFilterUpdate();
                }).bounds(this.leftPos + 5, this.topPos - 19, 58, 15).build()
        );

        // Filter EditBox for Players / Entities (Y = topPos - 18)
        this.filterBox = new EditBox(this.font, this.leftPos + 66, this.topPos - 18, 105, 14, Component.literal("Filter Box"));
        this.filterBox.setMaxLength(256);
        this.updateFilterBoxText();
        this.filterBox.setResponder(newVal -> {
            if (!this.suppressResponder) {
                if (this.filterMode == TurretCartEntity.FILTER_MODE_WHITELIST) {
                    this.whitelist = newVal;
                } else {
                    this.blacklist = newVal;
                }
                this.sendFilterUpdate();
            }
        });
        this.addRenderableWidget(this.filterBox);
    }

    private Component getModeButtonText() {
        if (this.filterMode == TurretCartEntity.FILTER_MODE_WHITELIST) {
            return Component.literal("Whitelist").withStyle(ChatFormatting.GREEN);
        } else {
            return Component.literal("Blacklist").withStyle(ChatFormatting.RED);
        }
    }

    private void updateFilterBoxText() {
        if (this.filterBox == null) return;
        this.suppressResponder = true;
        if (this.filterMode == TurretCartEntity.FILTER_MODE_WHITELIST) {
            this.filterBox.setHint(Component.literal("Whitelist players (Steve, Alex)").withStyle(ChatFormatting.DARK_GRAY));
            this.filterBox.setValue(this.whitelist);
        } else {
            this.filterBox.setHint(Component.literal("Blacklist targets (Steve, Alex)").withStyle(ChatFormatting.DARK_GRAY));
            this.filterBox.setValue(this.blacklist);
        }
        this.suppressResponder = false;
    }

    private void sendFilterUpdate() {
        if (this.cachedCart != null && !this.cachedCart.isRemoved()) {
            ClientPacketDistributor.sendToServer(new TurretCartFilterPayload(
                    this.cachedCart.getId(),
                    this.filterMode,
                    this.whitelist,
                    this.blacklist
            ));
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos) {
        if (mouseX >= leftPos && mouseX < leftPos + this.imageWidth && mouseY >= topPos - 44 && mouseY < topPos) {
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
            if (this.filterBox != null && this.filterBox.isFocused()) {
                this.filterBox.setFocused(false);
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
        if (this.filterBox != null && this.filterBox.isFocused()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                this.sendFilterUpdate();
                this.filterBox.setFocused(false);
                return true;
            }
            if (this.filterBox.keyPressed(event) || this.filterBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(this.font, Component.literal("Bows"), 62, 9, 0x404040, false);
        graphics.text(this.font, Component.literal("Arrows"), 8, 37, 0x404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.fill(xo, yo - 42, xo + this.imageWidth, yo - 1, 0xE621252B);
        graphics.outline(xo, yo - 42, this.imageWidth, 41, 0xFF4B5362);
        graphics.text(this.font, Component.literal("Cart:"), xo + 5, yo - 33, 0xFFE6EDF3, true);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}
