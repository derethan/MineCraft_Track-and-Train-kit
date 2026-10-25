package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.GeneratorMinecartMenu;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class GeneratorMinecartScreen extends AbstractContainerScreen<GeneratorMinecartMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/gui/basic_locomotive.png");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;

    public GeneratorMinecartScreen(GeneratorMinecartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.generator_cart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Generator Cart")) {
                currentVal = titleStr;
            }
        }

        this.nameBox = new EditBox(this.font, this.leftPos + 48, this.topPos - 18, 90, 14, Component.literal("Cart Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(currentVal);
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
        if (event.key() == InputConstants.KEY_ESCAPE) {
            this.onClose();
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Name plate header
        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xEE1E293B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFFF59E0B);
        graphics.text(this.font, Component.literal("Gen:"), xo + 5, yo - 15, 0xFFFEF3C7, true);

        // Base GUI container texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Water Tank Column (Left side, x=10, y=20, w=14, h=52)
        int tankX = xo + 10;
        int tankY = yo + 20;
        int tankW = 14;
        int tankH = 52;
        graphics.fill(tankX, tankY, tankX + tankW, tankY + tankH, 0xFF0B192C);
        float waterPct = this.menu.getWaterPercent();
        int waterFillH = Mth.clamp((int) (tankH * waterPct), 0, tankH);
        if (waterFillH > 0) {
            graphics.fill(tankX + 1, tankY + tankH - waterFillH + 1, tankX + tankW - 1, tankY + tankH - 1, 0xFF0284C7);
        }
        graphics.outline(tankX, tankY, tankW, tankH, 0xFF38BDF8);

        // 2. RF / Energy Storage Column (Right side, x=152, y=20, w=14, h=52)
        int energyX = xo + 152;
        int energyY = yo + 20;
        int energyW = 14;
        int energyH = 52;
        graphics.fill(energyX, energyY, energyX + energyW, energyY + energyH, 0xFF180A0A);
        float energyPct = this.menu.getEnergyPercent();
        int energyFillH = Mth.clamp((int) (energyH * energyPct), 0, energyH);
        if (energyFillH > 0) {
            int col = this.menu.isGenerating() ? 0xFFEF4444 : 0xFFB91C1C;
            graphics.fill(energyX + 1, energyY + energyH - energyFillH + 1, energyX + energyW - 1, energyY + energyH - 1, col);
        }
        graphics.outline(energyX, energyY, energyW, energyH, 0xFFEF4444);

        // 3. Flame indicator above fuel slot (x=80, y=26, w=16, h=14)
        int flameX = xo + 80;
        int flameY = yo + 26;
        int flameW = 16;
        int flameH = 14;
        graphics.fill(flameX, flameY, flameX + flameW, flameY + flameH, 0xFF1C1917);
        float burnPct = this.menu.getBurnPercent();
        if (burnPct > 0.0F) {
            int fillH = Mth.clamp((int) (flameH * burnPct), 0, flameH);
            graphics.fill(flameX + 1, flameY + flameH - fillH + 1, flameX + flameW - 1, flameY + flameH - 1, 0xFFF97316);
            graphics.text(this.font, Component.literal("🔥"), flameX + 3, flameY + 2, 0xFFFFEDD5, false);
        } else {
            graphics.text(this.font, Component.literal("·"), flameX + 6, flameY + 2, 0xFF78716C, false);
        }
        graphics.outline(flameX, flameY, flameW, flameH, 0xFF78716C);

        // Status text below slots
        Component status;
        int statusColor;
        if (this.menu.isGenerating()) {
            status = Component.literal("⚡ GENERATING (" + this.menu.getGenerationRate() + " FE/t)").withStyle(ChatFormatting.GREEN);
            statusColor = 0xFF15803D;
        } else if (this.menu.getFluidAmount() <= 0) {
            status = Component.literal("⚠ OUT OF WATER").withStyle(ChatFormatting.AQUA);
            statusColor = 0xFF0284C7;
        } else if (this.menu.getBurnTime() <= 0) {
            status = Component.literal("⚠ OUT OF FUEL").withStyle(ChatFormatting.GOLD);
            statusColor = 0xFFD97706;
        } else {
            status = Component.literal("IDLE (BUFFER FULL)").withStyle(ChatFormatting.GRAY);
            statusColor = 0xFF64748B;
        }
        graphics.text(this.font, status, xo + 48, yo + 64, statusColor, false);

        // Tooltips
        if (mouseX >= tankX && mouseX <= tankX + tankW && mouseY >= tankY && mouseY <= tankY + tankH) {
            Component tip = Component.literal("Water: " + this.menu.getFluidAmount() + " / " + this.menu.getWaterCapacity() + " mB")
                    .withStyle(ChatFormatting.AQUA);
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        } else if (mouseX >= energyX && mouseX <= energyX + energyW && mouseY >= energyY && mouseY <= energyY + energyH) {
            Component tip = Component.literal("RF Energy: " + this.menu.getEnergyStored() + " / " + this.menu.getEnergyCapacity() + " FE")
                    .withStyle(ChatFormatting.RED);
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        } else if (mouseX >= flameX && mouseX <= flameX + flameW && mouseY >= flameY && mouseY <= flameY + flameH) {
            Component tip = Component.literal("Burn Time: " + this.menu.getBurnTime() + " ticks")
                    .withStyle(ChatFormatting.GOLD);
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        }
    }
}
