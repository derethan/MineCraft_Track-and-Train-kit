$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Fix CartRenameScreen.java
$crsPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CartRenameScreen.java"
$crsContent = [System.IO.File]::ReadAllText($crsPath, $utf8NoBom)
$crsContent = $crsContent.Replace("mc.gui.setScreen(", "mc.setScreen(")
[System.IO.File]::WriteAllText($crsPath, $crsContent, $utf8NoBom)

# 2. Create CartRenameOpenPayload.java
$openPayloadPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\network\CartRenameOpenPayload.java"
$openPayloadContent = @'
package com.example.miningtools.network;

import com.example.miningtools.MiningTools;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CartRenameOpenPayload(int entityId, String currentName, String typeTitle, String statusLine) implements CustomPacketPayload {
    public static final Type<CartRenameOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MiningTools.MODID, "cart_rename_open"));

    public static final StreamCodec<ByteBuf, CartRenameOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CartRenameOpenPayload::entityId,
            ByteBufCodecs.stringUtf8(32),
            CartRenameOpenPayload::currentName,
            ByteBufCodecs.stringUtf8(64),
            CartRenameOpenPayload::typeTitle,
            ByteBufCodecs.stringUtf8(128),
            CartRenameOpenPayload::statusLine,
            CartRenameOpenPayload::new
    );

    @Override
    public Type<CartRenameOpenPayload> type() {
        return TYPE;
    }
}
'@
[System.IO.File]::WriteAllText($openPayloadPath, $openPayloadContent, $utf8NoBom)
Write-Host "Created CartRenameOpenPayload.java"

# 3. Add getContainer() to CargoMinecartMenu, TurretCartMenu, TunnelBoreMenu
$cargoMenuPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\menu\CargoMinecartMenu.java"
$cargoMenuContent = [System.IO.File]::ReadAllText($cargoMenuPath, $utf8NoBom)
if (-not $cargoMenuContent.Contains("getContainer()")) {
    $cargoMenuContent = $cargoMenuContent.Replace("    @Override`n    public boolean stillValid(Player player) {", "    public Container getContainer() {`n        return this.container;`n    }`n`n    @Override`n    public boolean stillValid(Player player) {")
    $cargoMenuContent = $cargoMenuContent.Replace("    @Override`r`n    public boolean stillValid(Player player) {", "    public Container getContainer() {`r`n        return this.container;`r`n    }`r`n`r`n    @Override`r`n    public boolean stillValid(Player player) {")
    [System.IO.File]::WriteAllText($cargoMenuPath, $cargoMenuContent, $utf8NoBom)
    Write-Host "Updated CargoMinecartMenu.java"
}

$turretMenuPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\menu\TurretCartMenu.java"
$turretMenuContent = [System.IO.File]::ReadAllText($turretMenuPath, $utf8NoBom)
if (-not $turretMenuContent.Contains("getContainer()")) {
    $turretMenuContent = $turretMenuContent.Replace("    @Override`n    public boolean stillValid(Player player) {", "    public Container getContainer() {`n        return this.container;`n    }`n`n    @Override`n    public boolean stillValid(Player player) {")
    $turretMenuContent = $turretMenuContent.Replace("    @Override`r`n    public boolean stillValid(Player player) {", "    public Container getContainer() {`r`n        return this.container;`r`n    }`r`n`r`n    @Override`r`n    public boolean stillValid(Player player) {")
    [System.IO.File]::WriteAllText($turretMenuPath, $turretMenuContent, $utf8NoBom)
    Write-Host "Updated TurretCartMenu.java"
}

$boreMenuPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\menu\TunnelBoreMenu.java"
$boreMenuContent = [System.IO.File]::ReadAllText($boreMenuPath, $utf8NoBom)
if (-not $boreMenuContent.Contains("getContainer()")) {
    $boreMenuContent = $boreMenuContent.Replace("    public boolean isLit() {", "    public Container getContainer() {`n        return this.container;`n    }`n`n    public boolean isLit() {")
    [System.IO.File]::WriteAllText($boreMenuPath, $boreMenuContent, $utf8NoBom)
    Write-Host "Updated TunnelBoreMenu.java"
}

# 4. Update CargoMinecartScreen.java
$cargoScreenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\CargoMinecartScreen.java"
$cargoScreenContent = @'
package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.CargoMinecartMenu;
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
public class CargoMinecartScreen extends AbstractContainerScreen<CargoMinecartMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/gui/cargo_minecart.png");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;

    public CargoMinecartScreen(CargoMinecartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.cargo_minecart").getString();
            String defaultContainer = Component.translatable("container.miningtools.cargo_minecart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals(defaultContainer)) {
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
        Component filterLabel = Component.translatable("container.miningtools.cargo_minecart.filter");
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
'@
[System.IO.File]::WriteAllText($cargoScreenPath, $cargoScreenContent, $utf8NoBom)
Write-Host "Updated CargoMinecartScreen.java"

# 5. Update AggregateHopperCartScreen.java
$hopperScreenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\AggregateHopperCartScreen.java"
$hopperScreenContent = @'
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
'@
[System.IO.File]::WriteAllText($hopperScreenPath, $hopperScreenContent, $utf8NoBom)
Write-Host "Updated AggregateHopperCartScreen.java"

# 6. Update TurretCartScreen.java
$turretScreenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\TurretCartScreen.java"
$turretScreenContent = @'
package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.MiningTools;
import com.example.miningtools.menu.TurretCartMenu;
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
public class TurretCartScreen extends AbstractContainerScreen<TurretCartMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MiningTools.MODID, "textures/gui/turret_cart.png");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;

    public TurretCartScreen(TurretCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.turret_cart").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Turret Cart")) {
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

        graphics.fill(xo, yo - 22, xo + this.imageWidth, yo - 1, 0xE621252B);
        graphics.outline(xo, yo - 22, this.imageWidth, 21, 0xFF4B5362);
        graphics.text(this.font, Component.literal("Cart:"), xo + 5, yo - 15, 0xFFE6EDF3, true);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}
'@
[System.IO.File]::WriteAllText($turretScreenPath, $turretScreenContent, $utf8NoBom)
Write-Host "Updated TurretCartScreen.java"

# 7. Update TunnelBoreScreen.java
$boreScreenPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\client\gui\TunnelBoreScreen.java"
$boreScreenContent = @'
package com.example.miningtools.client.gui;

import org.jspecify.annotations.Nullable;

import com.example.miningtools.menu.TunnelBoreMenu;
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
public class TunnelBoreScreen extends AbstractContainerScreen<TunnelBoreMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("miningtools", "textures/gui/tunnel_bore.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    private @Nullable EditBox nameBox;
    private boolean initializedName = false;
    private boolean suppressResponder = false;
    private @Nullable Button toggleButton;
    private @Nullable Button reverseButton;

    public TunnelBoreScreen(TunnelBoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        String currentVal = this.nameBox != null ? this.nameBox.getValue() : "";
        if (currentVal.isEmpty() && !this.initializedName) {
            this.initializedName = true;
            String defaultName = Component.translatable("entity.miningtools.tunnel_bore").getString();
            String titleStr = this.title.getString();
            if (!titleStr.equals(defaultName) && !titleStr.equals("Tunnel Bore")) {
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

        this.toggleButton = this.addRenderableWidget(
                Button.builder(this.getToggleButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }).bounds(this.leftPos + 106, this.topPos + 16, 62, 17).build()
        );

        this.reverseButton = this.addRenderableWidget(
                Button.builder(this.getReverseButtonLabel(), button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                }).bounds(this.leftPos + 106, this.topPos + 35, 62, 17).build()
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
    protected void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(this.getToggleButtonLabel());
        }
        if (this.reverseButton != null) {
            this.reverseButton.setMessage(this.getReverseButtonLabel());
        }
    }

    private Component getToggleButtonLabel() {
        return this.menu.isForward()
                ? Component.literal("Stop").withStyle(ChatFormatting.RED)
                : Component.literal("Forward").withStyle(ChatFormatting.GREEN);
    }

    private Component getReverseButtonLabel() {
        return this.menu.isReversing()
                ? Component.literal("Stop").withStyle(ChatFormatting.RED)
                : Component.literal("Reverse").withStyle(ChatFormatting.GOLD);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        Component statusText;
        int statusColor;
        if (this.menu.isForward()) {
            statusText = Component.literal("Forward");
            statusColor = 0xFF1E7A1E;
        } else if (this.menu.isReversing()) {
            statusText = Component.literal("Reversing");
            statusColor = 0xFFD4A017;
        } else {
            statusText = Component.literal("Stopped");
            statusColor = 0xFF9E2020;
        }
        graphics.text(this.font, statusText, 8, 22, statusColor, false);
        graphics.text(this.font, Component.literal("Tracks & Ballast"), 8, 43, 0x404040, false);
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

        if (this.menu.isLit()) {
            int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    14 - litProgressHeight,
                    xo + 81,
                    yo + 37 + 14 - litProgressHeight,
                    14,
                    litProgressHeight
            );
        }
    }
}
'@
[System.IO.File]::WriteAllText($boreScreenPath, $boreScreenContent, $utf8NoBom)
Write-Host "Updated TunnelBoreScreen.java"

# 8. Update LiquidMinecartEntity.java, LivestockCartEntity.java, CabooseMinecartEntity.java
$liqPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LiquidMinecartEntity.java"
$liqContent = [System.IO.File]::ReadAllText($liqPath, $utf8NoBom)
$oldLiqInteract = @'
        // Show fluid tank status overlay
        FluidStack stored = getStoredFluid();
        Component msg;
        if (stored.isEmpty()) {
            msg = Component.literal(getCartDisplayName() + ": Empty (0 / " + getCapacity() + " mB)");
        } else {
            String fluidDesc = stored.getHoverName().getString();
            msg = Component.literal(getCartDisplayName() + ": " + fluidDesc + " (" + stored.getAmount() + " / " + getCapacity() + " mB)");
        }
        player.sendOverlayMessage(msg);
        return InteractionResult.SUCCESS;
'@

$newLiqInteract = @'
        FluidStack stored = getStoredFluid();
        String statusStr = stored.isEmpty()
                ? "Tank: Empty (0 / " + getCapacity() + " mB)"
                : "Tank: " + stored.getHoverName().getString() + " (" + stored.getAmount() + " / " + getCapacity() + " mB)";

        if (player.isShiftKeyDown()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, getCartDisplayName(), statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Show fluid tank status overlay
        Component msg = Component.literal(getCartDisplayName() + " - " + statusStr);
        player.sendOverlayMessage(msg);
        return InteractionResult.SUCCESS;
'@
$liqContent = $liqContent.Replace($oldLiqInteract, $newLiqInteract)
[System.IO.File]::WriteAllText($liqPath, $liqContent, $utf8NoBom)
Write-Host "Updated LiquidMinecartEntity.java"

$livePath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\LivestockCartEntity.java"
$liveContent = [System.IO.File]::ReadAllText($livePath, $utf8NoBom)
$oldLiveInteract = @'
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
'@
$newLiveInteract = @'
        if (player.isSecondaryUseActive()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                String statusStr = "Passengers: " + this.getPassengers().size() + " / 2 Stalls";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, "Livestock Cart", statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }
'@
$liveContent = $liveContent.Replace($oldLiveInteract, $newLiveInteract)
[System.IO.File]::WriteAllText($livePath, $liveContent, $utf8NoBom)
Write-Host "Updated LivestockCartEntity.java"

$cabPath = "C:\Users\swack\Documents\GitHub\MineCraft_Track-and-Train-kit\src\main\java\com\example\miningtools\entity\CabooseMinecartEntity.java"
$cabContent = [System.IO.File]::ReadAllText($cabPath, $cab8NoBom)
$oldCabInteract = @'
        if (player.isSecondaryUseActive()) {
            // Shift right click displays chunk anchor status
            if (!this.level().isClientSide()) {
                int count = this.getForcedChunkCount();
                Component msg = Component.literal("Caboose Chunk Anchor: ACTIVE (" + count + " Chunks Forced - Train Protected)");
                player.sendOverlayMessage(msg);
            }
            return InteractionResult.SUCCESS;
        }
'@
$newCabInteract = @'
        if (player.isSecondaryUseActive()) {
            // Shift right click opens Caboose Rename & Chunk Anchor GUI
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int count = this.getForcedChunkCount();
                String statusStr = "Chunk Anchor: ACTIVE (" + count + " Chunks Forced)";
                String existing = this.getCustomName() != null ? this.getCustomName().getString().trim() : "";
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.example.miningtools.network.CartRenameOpenPayload(this.getId(), existing, "Train Caboose", statusStr)
                );
            }
            return InteractionResult.SUCCESS;
        }
'@
$cabContent = [System.IO.File]::ReadAllText($cabPath, $utf8NoBom)
$cabContent = $cabContent.Replace($oldCabInteract, $newCabInteract)
[System.IO.File]::WriteAllText($cabPath, $cabContent, $utf8NoBom)
Write-Host "Updated CabooseMinecartEntity.java"
