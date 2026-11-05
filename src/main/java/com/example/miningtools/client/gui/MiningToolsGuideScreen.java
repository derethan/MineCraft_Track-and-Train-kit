package com.example.miningtools.client.gui;

import com.example.miningtools.guide.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MiningToolsGuideScreen extends Screen {
    private static final int GUI_WIDTH = 346;
    private static final int GUI_HEIGHT = 226;

    private int leftPos;
    private int topPos;

    private GuideCategory selectedCategory = GuideCategory.ALL;
    private @Nullable GuideEntry currentEntry = null;

    // View sub-tab in Entry view: 0 = Overview, 1 = Recipes, etc.
    private int entrySubTab = 0;
    private int selectedLayerIndex = 0;

    // Scrolling offsets
    private int tocScrollOffset = 0;
    private int contentScrollOffset = 0;

    public MiningToolsGuideScreen() {
        super(Component.literal("Railroader's Field Manual"));
    }

    public static void openToEntry(String entryId) {
        MiningToolsGuideScreen screen = new MiningToolsGuideScreen();
        screen.currentEntry = GuideBookRegistry.getEntry(entryId);
        net.minecraft.client.Minecraft.getInstance().setScreenAndShow(screen);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int left = this.leftPos;
        int top = this.topPos;

        // 1. Dark Industrial Slate Binder Outer Shell
        graphics.fill(RenderPipelines.GUI, left - 2, top - 2, left + GUI_WIDTH + 2, top + GUI_HEIGHT + 2, 0xFF0F1115);
        graphics.fill(RenderPipelines.GUI, left, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xFF181B20);

        // Heavy industrial riveted bezel border
        drawBezelBorder(graphics, left, top, GUI_WIDTH, GUI_HEIGHT);

        // Corner brass brackets
        drawCornerBrackets(graphics, left, top, GUI_WIDTH, GUI_HEIGHT);

        // 2. Left Sidebar (Categories)
        int sidebarWidth = 98;
        graphics.fill(RenderPipelines.GUI, left + 4, top + 4, left + sidebarWidth, top + GUI_HEIGHT - 4, 0xFF14161B);
        graphics.fill(RenderPipelines.GUI, left + sidebarWidth, top + 4, left + sidebarWidth + 1, top + GUI_HEIGHT - 4, 0xFF2A2E38);

        // Sidebar Header
        graphics.fill(RenderPipelines.GUI, left + 6, top + 6, left + sidebarWidth - 2, top + 20, 0xFF1D2128);
        graphics.text(this.font, Component.literal("CATEGORIES"), left + 10, top + 9, 0xFF94A3B8, false);

        // Category Buttons
        GuideCategory[] categories = GuideCategory.values();
        int catY = top + 24;
        for (GuideCategory cat : categories) {
            boolean isSelected = (this.selectedCategory == cat && this.currentEntry == null);
            boolean isHovered = mouseX >= left + 6 && mouseX <= left + sidebarWidth - 2 && mouseY >= catY && mouseY <= catY + 18;

            int bgCol = isSelected ? 0xFF2563EB : (isHovered ? 0xFF262C37 : 0xFF181B22);
            graphics.fill(RenderPipelines.GUI, left + 6, catY, left + sidebarWidth - 2, catY + 18, bgCol);

            if (isSelected) {
                // Gold indicator on active tab
                graphics.fill(RenderPipelines.GUI, left + 6, catY, left + 8, catY + 18, 0xFFF59E0B);
            }

            int textCol = isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFF8FAFC : cat.getColor());
            graphics.text(this.font, Component.literal(cat.getDisplayName()), left + 12, catY + 5, textCol, false);
            catY += 21;
        }

        // 3. Right Content Panel
        int contentX = left + sidebarWidth + 3;
        int contentWidth = GUI_WIDTH - sidebarWidth - 7;
        int contentHeight = GUI_HEIGHT - 8;

        if (this.currentEntry == null) {
            renderTableOfContents(graphics, contentX, top + 4, contentWidth, contentHeight, mouseX, mouseY);
        } else {
            renderEntryDetail(graphics, contentX, top + 4, contentWidth, contentHeight, mouseX, mouseY);
        }
    }

    private void renderTableOfContents(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Header
        graphics.fill(RenderPipelines.GUI, x, y, x + width, y + 20, 0xFF1C2027);
        graphics.text(this.font, Component.literal("TABLE OF CONTENTS").withStyle(net.minecraft.ChatFormatting.BOLD), x + 8, y + 6, 0xFFF59E0B, false);

        List<GuideEntry> entries = GuideBookRegistry.getEntriesByCategory(this.selectedCategory);
        String countBadge = entries.size() + " Topics";
        int badgeWidth = font.width(countBadge) + 8;
        graphics.fill(RenderPipelines.GUI, x + width - badgeWidth - 6, y + 4, x + width - 6, y + 17, 0xFF2A303C);
        graphics.text(this.font, Component.literal(countBadge), x + width - badgeWidth - 2, y + 6, 0xFF94A3B8, false);

        // List of Entry Cards
        int cardStartY = y + 24;
        int visibleHeight = height - 26;
        int cardHeight = 28;

        int totalEntriesHeight = entries.size() * (cardHeight + 2);
        int maxScroll = Math.max(0, totalEntriesHeight - visibleHeight);
        this.tocScrollOffset = Math.max(0, Math.min(this.tocScrollOffset, maxScroll));

        for (int i = 0; i < entries.size(); i++) {
            GuideEntry entry = entries.get(i);
            int cardY = cardStartY + i * (cardHeight + 2) - this.tocScrollOffset;

            if (cardY + cardHeight < cardStartY || cardY > y + height) {
                continue; // Clipped
            }

            boolean isHovered = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= cardY && mouseY <= cardY + cardHeight;

            int bgCol = isHovered ? 0xFF282F3B : 0xFF1E222A;
            int borderCol = isHovered ? 0xFF3B82F6 : 0xFF2B323F;

            graphics.fill(RenderPipelines.GUI, x + 4, cardY, x + width - 4, cardY + cardHeight, bgCol);
            graphics.fill(RenderPipelines.GUI, x + 4, cardY, x + width - 4, cardY + 1, borderCol);
            graphics.fill(RenderPipelines.GUI, x + 4, cardY + cardHeight - 1, x + width - 4, cardY + cardHeight, borderCol);
            graphics.fill(RenderPipelines.GUI, x + 4, cardY, x + 5, cardY + cardHeight, borderCol);
            graphics.fill(RenderPipelines.GUI, x + width - 5, cardY, x + width - 4, cardY + cardHeight, borderCol);

            // Item Icon
            ItemStack icon = entry.getIcon();
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 7, cardY + 6);
            }

            // Title & Subtitle
            int titleCol = isHovered ? 0xFFF8FAFC : 0xFFE2E8F0;
            graphics.text(this.font, Component.literal(entry.getTitle()), x + 28, cardY + 4, titleCol, false);
            graphics.text(this.font, Component.literal(entry.getSubtitle()), x + 28, cardY + 15, 0xFF94A3B8, false);

            // Chevron
            graphics.text(this.font, Component.literal(">"), x + width - 14, cardY + 10, isHovered ? 0xFF38BDF8 : 0xFF475569, false);
        }

        // Scrollbar if needed
        if (maxScroll > 0) {
            int scrollbarX = x + width - 3;
            int scrollbarH = Math.max(16, (int) ((visibleHeight / (float) totalEntriesHeight) * visibleHeight));
            int scrollbarY = cardStartY + (int) ((this.tocScrollOffset / (float) maxScroll) * (visibleHeight - scrollbarH));
            graphics.fill(RenderPipelines.GUI, scrollbarX, cardStartY, scrollbarX + 2, cardStartY + visibleHeight, 0xFF1E222A);
            graphics.fill(RenderPipelines.GUI, scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarH, 0xFF3B82F6);
        }
    }

    private void renderEntryDetail(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (this.currentEntry == null) return;
        GuideEntry entry = this.currentEntry;

        // Top Header with Back Button
        graphics.fill(RenderPipelines.GUI, x, y, x + width, y + 22, 0xFF1C2027);

        // Back Button
        boolean backHovered = mouseX >= x + 4 && mouseX <= x + 76 && mouseY >= y + 3 && mouseY <= y + 19;
        int backBg = backHovered ? 0xFF2563EB : 0xFF2A303C;
        graphics.fill(RenderPipelines.GUI, x + 4, y + 3, x + 76, y + 19, backBg);
        graphics.text(this.font, Component.literal("< Contents"), x + 8, y + 6, backHovered ? 0xFFFFFFFF : 0xFFCBD5E1, false);

        // Entry Title in Header
        graphics.text(this.font, Component.literal(entry.getTitle()).withStyle(net.minecraft.ChatFormatting.BOLD), x + 82, y + 6, 0xFFF59E0B, false);

        // Sub-Tabs Bar
        int tabY = y + 24;
        int tabH = 18;
        graphics.fill(RenderPipelines.GUI, x, tabY, x + width, tabY + tabH, 0xFF16191F);

        List<String> tabs = new ArrayList<>();
        if (entry.isMultiblock()) {
            tabs.add("Blocks Required");
            tabs.add("Layer Layout");
            tabs.add("Overview");
        } else {
            tabs.add("Overview");
        }
        if (!entry.getRecipes().isEmpty()) {
            tabs.add("Recipes");
        }

        int tabX = x + 4;
        for (int i = 0; i < tabs.size(); i++) {
            String tabName = tabs.get(i);
            int tw = font.width(tabName) + 12;
            boolean isSelected = (this.entrySubTab == i);
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + tw && mouseY >= tabY && mouseY <= tabY + tabH;

            int tBg = isSelected ? 0xFF2563EB : (isHovered ? 0xFF262C37 : 0xFF1E222A);
            graphics.fill(RenderPipelines.GUI, tabX, tabY + 1, tabX + tw, tabY + tabH - 1, tBg);

            if (isSelected) {
                graphics.fill(RenderPipelines.GUI, tabX, tabY + tabH - 2, tabX + tw, tabY + tabH, 0xFFF59E0B);
            }

            int tCol = isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFF8FAFC : 0xFF94A3B8);
            graphics.text(this.font, Component.literal(tabName), tabX + 6, tabY + 5, tCol, false);
            tabX += tw + 4;
        }

        // Render Tab Content
        int viewY = tabY + tabH + 2;
        int viewHeight = height - (tabH + 26);

        if (entry.isMultiblock()) {
            if (this.entrySubTab == 0) {
                renderRequiredBlocksTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            } else if (this.entrySubTab == 1) {
                renderLayerLayoutTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            } else if (this.entrySubTab == 2) {
                renderOverviewTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            } else {
                renderRecipesTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            }
        } else {
            if (this.entrySubTab == 0) {
                renderOverviewTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            } else {
                renderRecipesTab(graphics, entry, x + 4, viewY, width - 8, viewHeight, mouseX, mouseY);
            }
        }
    }

    private void renderOverviewTab(GuiGraphicsExtractor graphics, GuideEntry entry, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Machine / Item Icon + Subtitle
        graphics.fill(RenderPipelines.GUI, x, y, x + width, y + 28, 0xFF1E222A);
        graphics.item(entry.getIcon(), x + 6, y + 6);
        graphics.text(this.font, Component.literal(entry.getTitle()).withStyle(net.minecraft.ChatFormatting.BOLD), x + 28, y + 5, 0xFFF8FAFC, false);
        graphics.text(this.font, Component.literal(entry.getSubtitle()), x + 28, y + 16, 0xFF38BDF8, false);

        int textY = y + 32 - this.contentScrollOffset;
        int textMaxBottom = y + height - 38;

        // Description Paragraphs
        List<FormattedCharSequence> lines = this.font.split(Component.literal(entry.getDescription()), width - 8);
        int totalTextH = lines.size() * 10;
        int maxScroll = Math.max(0, totalTextH - (height - 70));
        this.contentScrollOffset = Math.max(0, Math.min(this.contentScrollOffset, maxScroll));

        for (FormattedCharSequence line : lines) {
            if (textY >= y + 30 && textY + 9 <= textMaxBottom) {
                graphics.text(this.font, line, x + 4, textY, 0xFFCBD5E1, false);
            }
            textY += 10;
        }

        // Pro-Tip Box at Bottom
        if (entry.getProTips() != null && !entry.getProTips().isEmpty()) {
            int tipBoxY = y + height - 34;
            graphics.fill(RenderPipelines.GUI, x, tipBoxY, x + width, tipBoxY + 34, 0xFF172033);
            graphics.fill(RenderPipelines.GUI, x, tipBoxY, x + 3, tipBoxY + 34, 0xFFF59E0B);
            graphics.text(this.font, Component.literal("PRO TIP:").withStyle(net.minecraft.ChatFormatting.BOLD), x + 8, tipBoxY + 4, 0xFFFCD34D, false);

            List<FormattedCharSequence> tipLines = this.font.split(Component.literal(entry.getProTips()), width - 14);
            int tipTextY = tipBoxY + 14;
            for (FormattedCharSequence line : tipLines) {
                if (tipTextY + 9 > tipBoxY + 34) break;
                graphics.text(this.font, line, x + 8, tipTextY, 0xFF93C5FD, false);
                tipTextY += 9;
            }
        }
    }

    private void renderRequiredBlocksTab(GuiGraphicsExtractor graphics, GuideEntry entry, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<GuideBlockRequirement> reqs = entry.getRequirements();
        if (reqs == null || reqs.isEmpty()) return;

        graphics.fill(RenderPipelines.GUI, x, y, x + width, y + 20, 0xFF1E222A);
        graphics.text(this.font, Component.literal("BILL OF MATERIALS"), x + 6, y + 6, 0xFFF8FAFC, false);

        String totalBadge = "Total: " + entry.getTotalRequiredBlocks() + " Blocks";
        graphics.text(this.font, Component.literal(totalBadge), x + width - font.width(totalBadge) - 8, y + 6, 0xFF10B981, false);

        int itemY = y + 24;
        for (GuideBlockRequirement req : reqs) {
            if (itemY + 22 > y + height) break;

            boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY <= itemY + 20;
            int bgCol = isHovered ? 0xFF2A313E : 0xFF191D24;
            graphics.fill(RenderPipelines.GUI, x, itemY, x + width, itemY + 20, bgCol);

            ItemStack stack = req.getItemStack();
            if (!stack.isEmpty()) {
                graphics.item(stack, x + 4, itemY + 2);
            }

            graphics.text(this.font, Component.literal(req.name()), x + 26, itemY + 6, isHovered ? 0xFFF8FAFC : 0xFFE2E8F0, false);

            String countText = req.count() + "x";
            graphics.text(this.font, Component.literal(countText).withStyle(net.minecraft.ChatFormatting.BOLD), x + width - font.width(countText) - 8, itemY + 6, 0xFFF59E0B, false);

            if (isHovered && !stack.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }

            itemY += 22;
        }
    }

    private void renderLayerLayoutTab(GuiGraphicsExtractor graphics, GuideEntry entry, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<GuideLayer> layers = entry.getLayers();
        if (layers == null || layers.isEmpty()) return;

        if (this.selectedLayerIndex < 0 || this.selectedLayerIndex >= layers.size()) {
            this.selectedLayerIndex = 0;
        }

        int pillX = x;
        int pillH = 16;
        for (int i = 0; i < layers.size(); i++) {
            GuideLayer l = layers.get(i);
            String label = "L" + l.layerIndex();
            int pw = font.width(label) + 10;

            boolean isSelected = (this.selectedLayerIndex == i);
            boolean isHovered = mouseX >= pillX && mouseX <= pillX + pw && mouseY >= y && mouseY <= y + pillH;

            int pBg = isSelected ? 0xFF38BDF8 : (isHovered ? 0xFF2A313E : 0xFF1E222A);
            graphics.fill(RenderPipelines.GUI, pillX, y, pillX + pw, y + pillH, pBg);

            int pCol = isSelected ? 0xFF0F172A : (isHovered ? 0xFFF8FAFC : 0xFF94A3B8);
            graphics.text(this.font, Component.literal(label), pillX + 5, y + 4, pCol, isSelected);

            pillX += pw + 3;
        }

        GuideLayer currentLayer = layers.get(this.selectedLayerIndex);
        graphics.text(this.font, Component.literal(currentLayer.layerName()).withStyle(net.minecraft.ChatFormatting.BOLD), pillX + 6, y + 4, 0xFFFCD34D, false);

        int gridAreaY = y + 20;
        int gridW = currentLayer.getWidth();
        int gridD = currentLayer.getDepth();
        int cellSize = (gridW <= 3 && gridD <= 3) ? 22 : 16;

        int totalGridW = gridW * cellSize;
        int totalGridH = gridD * cellSize;

        int startGridX = x + 8;
        int startGridY = gridAreaY + 12;

        graphics.text(this.font, Component.literal("▼ FRONT"), startGridX, startGridY + totalGridH + 3, 0xFF38BDF8, false);

        String hoveredBlockTooltip = null;

        for (int z = 0; z < gridD; z++) {
            for (int gx = 0; gx < gridW; gx++) {
                char c = currentLayer.getAt(gx, z);
                LayerSymbol sym = currentLayer.legend().get(c);

                int cellX = startGridX + gx * cellSize;
                int cellY = startGridY + z * cellSize;

                boolean cellHovered = mouseX >= cellX && mouseX < cellX + cellSize && mouseY >= cellY && mouseY < cellY + cellSize;

                int cellColor = (sym != null) ? sym.color() : 0xFF282C34;
                graphics.fill(RenderPipelines.GUI, cellX, cellY, cellX + cellSize, cellY + cellSize, cellColor);

                int border = cellHovered ? 0xFFFFFFFF : 0xFF0F172A;
                graphics.fill(RenderPipelines.GUI, cellX, cellY, cellX + cellSize, cellY + 1, border);
                graphics.fill(RenderPipelines.GUI, cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, border);
                graphics.fill(RenderPipelines.GUI, cellX, cellY, cellX + 1, cellY + cellSize, border);
                graphics.fill(RenderPipelines.GUI, cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, border);

                String symStr = String.valueOf(c);
                int symX = cellX + (cellSize - font.width(symStr)) / 2;
                int symY = cellY + (cellSize - 8) / 2;
                graphics.text(this.font, Component.literal(symStr), symX, symY, 0xFFFFFFFF, true);

                if (cellHovered && sym != null) {
                    hoveredBlockTooltip = sym.name();
                }
            }
        }

        int legendX = startGridX + totalGridW + 12;
        int legendY = gridAreaY + 6;
        int legendW = width - (startGridX - x + totalGridW + 16);

        graphics.fill(RenderPipelines.GUI, legendX, legendY, legendX + legendW, legendY + 14, 0xFF1E222A);
        graphics.text(this.font, Component.literal("BLOCK LEGEND"), legendX + 4, legendY + 3, 0xFF94A3B8, false);

        int legY = legendY + 18;
        for (LayerSymbol sym : currentLayer.legend().values()) {
            if (legY + 14 > y + height) break;

            graphics.fill(RenderPipelines.GUI, legendX + 2, legY, legendX + 12, legY + 10, sym.color());
            String text = "[" + sym.symbol() + "] " + sym.name();
            graphics.text(this.font, Component.literal(font.plainSubstrByWidth(text, legendW - 16)), legendX + 16, legY + 1, 0xFFE2E8F0, false);
            legY += 13;
        }

        if (hoveredBlockTooltip != null) {
            graphics.setTooltipForNextFrame(Component.literal(hoveredBlockTooltip), mouseX, mouseY);
        }
    }

    private void renderRecipesTab(GuiGraphicsExtractor graphics, GuideEntry entry, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<GuideRecipe> recipes = entry.getRecipes();
        if (recipes.isEmpty()) return;

        int recY = y - this.contentScrollOffset;
        for (GuideRecipe rec : recipes) {
            if (recY + 38 >= y && recY <= y + height) {
                graphics.fill(RenderPipelines.GUI, x, recY, x + width, recY + 38, 0xFF191D24);
                graphics.fill(RenderPipelines.GUI, x, recY, x + width, recY + 1, 0xFF2A313E);

                // Title
                graphics.text(this.font, Component.literal(rec.title()).withStyle(net.minecraft.ChatFormatting.BOLD), x + 6, recY + 4, 0xFFFCD34D, false);

                // Inputs -> Arrow -> Outputs
                int itemX = x + 8;
                for (ItemStack in : rec.getInputStacks()) {
                    if (!in.isEmpty()) {
                        graphics.item(in, itemX, recY + 16);
                        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= recY + 16 && mouseY <= recY + 32) {
                            graphics.setTooltipForNextFrame(this.font, in, mouseX, mouseY);
                        }
                        itemX += 20;
                    }
                }

                graphics.text(this.font, Component.literal("➔"), itemX + 4, recY + 20, 0xFF38BDF8, false);
                itemX += 20;

                for (ItemStack out : rec.getOutputStacks()) {
                    if (!out.isEmpty()) {
                        graphics.item(out, itemX, recY + 16);
                        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= recY + 16 && mouseY <= recY + 32) {
                            graphics.setTooltipForNextFrame(this.font, out, mouseX, mouseY);
                        }
                        itemX += 20;
                    }
                }

                // Notes
                if (rec.notes() != null) {
                    graphics.text(this.font, Component.literal(font.plainSubstrByWidth(rec.notes(), width - (itemX - x) - 8)), itemX + 8, recY + 20, 0xFF94A3B8, false);
                }
            }

            recY += 42;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        int left = this.leftPos;
        int top = this.topPos;
        int sidebarWidth = 98;

        // 1. Sidebar Category Click
        GuideCategory[] categories = GuideCategory.values();
        int catY = top + 24;
        for (GuideCategory cat : categories) {
            if (mouseX >= left + 6 && mouseX <= left + sidebarWidth - 2 && mouseY >= catY && mouseY <= catY + 18) {
                this.selectedCategory = cat;
                this.currentEntry = null; // Return to Table of Contents
                this.tocScrollOffset = 0;
                this.contentScrollOffset = 0;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                }
                return true;
            }
            catY += 21;
        }

        int contentX = left + sidebarWidth + 3;
        int contentWidth = GUI_WIDTH - sidebarWidth - 7;

        // 2. Table of Contents View Click
        if (this.currentEntry == null) {
            List<GuideEntry> entries = GuideBookRegistry.getEntriesByCategory(this.selectedCategory);
            int cardStartY = top + 28;
            int cardHeight = 28;

            for (int i = 0; i < entries.size(); i++) {
                int cardY = cardStartY + i * (cardHeight + 2) - this.tocScrollOffset;
                if (mouseX >= contentX + 4 && mouseX <= contentX + contentWidth - 4 && mouseY >= cardY && mouseY <= cardY + cardHeight) {
                    this.currentEntry = entries.get(i);
                    this.entrySubTab = 0;
                    this.selectedLayerIndex = 0;
                    this.contentScrollOffset = 0;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                    }
                    return true;
                }
            }
        } else {
            // 3. Entry Detail View Click
            // Back Button (< Contents)
            if (mouseX >= contentX + 4 && mouseX <= contentX + 76 && mouseY >= top + 7 && mouseY <= top + 23) {
                this.currentEntry = null;
                this.contentScrollOffset = 0;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
                return true;
            }

            // Sub-Tabs Click
            int tabY = top + 28;
            int tabH = 18;
            List<String> tabs = new ArrayList<>();
            if (this.currentEntry.isMultiblock()) {
                tabs.add("Blocks Required");
                tabs.add("Layer Layout");
                tabs.add("Overview");
            } else {
                tabs.add("Overview");
            }
            if (!this.currentEntry.getRecipes().isEmpty()) {
                tabs.add("Recipes");
            }

            int tabX = contentX + 4;
            for (int i = 0; i < tabs.size(); i++) {
                String tabName = tabs.get(i);
                int tw = font.width(tabName) + 12;
                if (mouseX >= tabX && mouseX <= tabX + tw && mouseY >= tabY && mouseY <= tabY + tabH) {
                    this.entrySubTab = i;
                    this.contentScrollOffset = 0;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1f));
                    }
                    return true;
                }
                tabX += tw + 4;
            }

            // Layer Pills Click in Layer Layout (for multiblocks)
            if (this.entrySubTab == 1 && this.currentEntry.isMultiblock()) {
                List<GuideLayer> layers = this.currentEntry.getLayers();
                if (layers != null) {
                    int pillX = contentX + 4;
                    int pillY = tabY + tabH + 2;
                    int pillH = 16;
                    for (int i = 0; i < layers.size(); i++) {
                        GuideLayer l = layers.get(i);
                        String label = "L" + l.layerIndex();
                        int pw = font.width(label) + 10;
                        if (mouseX >= pillX && mouseX <= pillX + pw && mouseY >= pillY && mouseY <= pillY + pillH) {
                            this.selectedLayerIndex = i;
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
                            }
                            return true;
                        }
                        pillX += pw + 3;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.currentEntry == null) {
            this.tocScrollOffset = Math.max(0, this.tocScrollOffset - (int) (scrollY * 18));
            return true;
        } else {
            this.contentScrollOffset = Math.max(0, this.contentScrollOffset - (int) (scrollY * 14));
            return true;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.currentEntry != null) {
                this.currentEntry = null; // ESC goes back to Table of Contents first!
                this.contentScrollOffset = 0;
                return true;
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void drawBezelBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        // Steel riveted bezel border
        graphics.fill(RenderPipelines.GUI, x, y, x + w, y + 2, 0xFF374151);
        graphics.fill(RenderPipelines.GUI, x, y + h - 2, x + w, y + h, 0xFF374151);
        graphics.fill(RenderPipelines.GUI, x, y, x + 2, y + h, 0xFF374151);
        graphics.fill(RenderPipelines.GUI, x + w - 2, y, x + w, y + h, 0xFF374151);
    }

    private void drawCornerBrackets(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int brass = 0xFFF59E0B;
        int brassLight = 0xFFFCD34D;
        // Top-Left
        graphics.fill(RenderPipelines.GUI, x + 2, y + 2, x + 8, y + 4, brassLight);
        graphics.fill(RenderPipelines.GUI, x + 2, y + 4, x + 4, y + 8, brass);
        // Top-Right
        graphics.fill(RenderPipelines.GUI, x + w - 8, y + 2, x + w - 2, y + 4, brassLight);
        graphics.fill(RenderPipelines.GUI, x + w - 4, y + 4, x + w - 2, y + 8, brass);
        // Bottom-Left
        graphics.fill(RenderPipelines.GUI, x + 2, y + h - 4, x + 8, y + h - 2, brass);
        graphics.fill(RenderPipelines.GUI, x + 2, y + h - 8, x + 4, y + h - 4, brassLight);
        // Bottom-Right
        graphics.fill(RenderPipelines.GUI, x + w - 8, y + h - 4, x + w - 2, y + h - 2, brass);
        graphics.fill(RenderPipelines.GUI, x + w - 4, y + h - 8, x + w - 2, y + h - 4, brassLight);
    }
}
