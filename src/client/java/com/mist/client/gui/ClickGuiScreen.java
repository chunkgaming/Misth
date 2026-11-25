package com.mist.client.gui;

import com.mist.client.CommandMenuScreen;
import com.mist.client.config.MistConfig;
import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.ModuleManager;
import com.mist.client.module.impl.utility.CommandKeysModule;
import com.mist.client.module.setting.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified ClickGUI for all MIST modules:
 * - Category tabs (All, Combat, Render, HUD, Utility)
 * - Search bar
 * - Prominent "Edit HUD" button
 * - Left-click toggles module, right-click or gear icon opens Settings panel
 */
public class ClickGuiScreen extends Screen {
        private static final int WIN_W = 460;
        private static final int WIN_H = 290;
        private static final int HEADER_H = 30;
        private static final int TAB_H = 22;

        private final Screen parent;
        private int wx, wy;
        private Category selectedCategory = null; // null = All
        private Module selectedModule = null;
        private double moduleScroll = 0.0;
        private double settingsScroll = 0.0;
        private boolean listeningForKeybind = false;

        // Active slider tracking
        private NumberSetting draggingSlider = null;
        private ColorSetting draggingColor = null;
        private int draggingColorChannel = -1; // 0=R,1=G,2=B,3=A

        private EditBox searchBox;
        private EditBox stringListInput;

        public ClickGuiScreen(Screen parent) {
                super(Component.literal("MIST ClickGUI"));
                this.parent = parent;
        }

        @Override
        protected void init() {
                this.wx = (this.width - WIN_W) / 2;
                this.wy = (this.height - WIN_H) / 2;

                this.searchBox = new EditBox(this.font, wx + 10, wy + HEADER_H + 5, 160, 16, Component.literal("Search"));
                this.searchBox.setHint(Component.literal("Search modules...").withStyle(ChatFormatting.DARK_GRAY));
                this.searchBox.setMaxLength(32);
                this.addRenderableWidget(this.searchBox);

                this.stringListInput = new EditBox(this.font, wx + 226, wy + WIN_H - 28, 170, 16, Component.literal("Add word"));
                this.stringListInput.setHint(Component.literal("Add trigger word...").withStyle(ChatFormatting.DARK_GRAY));
                this.stringListInput.setMaxLength(40);
                this.stringListInput.setVisible(false);
                this.addRenderableWidget(this.stringListInput);
        }

        private List<Module> getFilteredModules() {
                String q = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
                List<Module> result = new ArrayList<>();
                for (Module m : ModuleManager.getModules()) {
                        if (selectedCategory != null && m.getCategory() != selectedCategory) continue;
                        if (!q.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(q)
                                        && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                                continue;
                        }
                        result.add(m);
                }
                return result;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(0, 0, this.width, this.height, 0x77000000);

                // Main window
                graphics.fill(wx, wy, wx + WIN_W, wy + WIN_H, 0xEC14161D);
                graphics.outline(wx, wy, WIN_W, WIN_H, 0xFF2E3446);

                // Header
                graphics.fill(wx, wy, wx + WIN_W, wy + HEADER_H, 0xFF1C202B);
                graphics.text(this.font, Component.literal("MIST").withStyle(ChatFormatting.BOLD), wx + 12, wy + 10, 0xFF58A6FF, false);
                graphics.text(this.font, "v1.0", wx + 42, wy + 10, 0xFF8B949E, false);

                // Prominent "Edit HUD" button
                int hudBtnW = 74, hudBtnH = 18;
                int hudBtnX = wx + WIN_W - hudBtnW - 10, hudBtnY = wy + 6;
                boolean hudHover = isInside(mouseX, mouseY, hudBtnX, hudBtnY, hudBtnW, hudBtnH);
                graphics.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, hudHover ? 0xFF2E5B88 : 0xFF23384E);
                graphics.outline(hudBtnX, hudBtnY, hudBtnW, hudBtnH, 0xFF58A6FF);
                graphics.centeredText(this.font, "Edit HUD", hudBtnX + hudBtnW / 2, hudBtnY + 5, 0xFFFFFFFF);

                // Category tabs
                int tabX = wx + 178;
                int tabY = wy + HEADER_H + 5;
                String[] tabLabels = {"All", "Combat", "Render", "HUD", "Utility"};
                Category[] tabCats = {null, Category.COMBAT, Category.RENDER, Category.HUD, Category.UTILITY};
                for (int i = 0; i < tabLabels.length; i++) {
                        int tw = this.font.width(tabLabels[i]) + 10;
                        boolean active = selectedCategory == tabCats[i];
                        boolean hov = isInside(mouseX, mouseY, tabX, tabY, tw, 16);
                        graphics.fill(tabX, tabY, tabX + tw, tabY + 16, active ? 0xFF2D3A52 : (hov ? 0xFF232836 : 0xFF1A1D27));
                        graphics.outline(tabX, tabY, tw, 16, active ? 0xFF58A6FF : 0xFF2E3446);
                        graphics.centeredText(this.font, tabLabels[i], tabX + tw / 2, tabY + 4, active ? 0xFFFFFFFF : 0xFF9BA3B4);
                        tabX += tw + 4;
                }

                // Divider line
                int bodyTop = wy + HEADER_H + TAB_H + 4;
                graphics.fill(wx + 1, bodyTop - 2, wx + WIN_W - 1, bodyTop - 1, 0xFF2E3446);

                // Left pane: Module list (width 205)
                int leftW = 205;
                int listX = wx + 8;
                int listH = WIN_H - (bodyTop - wy) - 8;
                graphics.fill(wx + leftW + 6, bodyTop, wx + leftW + 7, wy + WIN_H - 6, 0xFF2E3446);

                List<Module> modules = getFilteredModules();
                int rowH = 26, rowGap = 4;
                int maxModScroll = Math.max(0, modules.size() * (rowH + rowGap) - listH);
                moduleScroll = Math.max(0.0, Math.min(maxModScroll, moduleScroll));

                graphics.enableScissor(listX - 2, bodyTop, wx + leftW + 4, wy + WIN_H - 6);
                for (int i = 0; i < modules.size(); i++) {
                        Module m = modules.get(i);
                        int ry = bodyTop + i * (rowH + rowGap) - (int) moduleScroll;
                        if (ry + rowH < bodyTop || ry > wy + WIN_H - 6) continue;

                        boolean isSelected = m == selectedModule;
                        boolean hov = isInside(mouseX, mouseY, listX, ry, leftW - 8, rowH);
                        int bg = m.isEnabled() ? (hov ? 0xFF243B32 : 0xFF1B2E27) : (hov ? 0xFF242936 : 0xFF1A1E28);
                        int brd = isSelected ? 0xFF58A6FF : (m.isEnabled() ? 0xFF3FB950 : 0xFF2E3446);
                        graphics.fill(listX, ry, listX + leftW - 8, ry + rowH, bg);
                        graphics.outline(listX, ry, leftW - 8, rowH, brd);

                        // Status dot
                        graphics.fill(listX + 6, ry + 10, listX + 10, ry + 14, m.isEnabled() ? 0xFF3FB950 : 0xFF6E7681);
                        String mName = this.font.plainSubstrByWidth(m.getName(), leftW - 44);
                        graphics.text(this.font, mName, listX + 15, ry + 5, m.isEnabled() ? 0xFFFFFFFF : 0xFFB0B8C5, false);
                        graphics.text(this.font, m.getCategory().getDisplayName(), listX + 15, ry + 15, 0xFF7D8590, false);

                        // Gear icon button on right
                        int gx = listX + leftW - 28, gy = ry + 5;
                        boolean gHov = isInside(mouseX, mouseY, gx, gy, 16, 16);
                        graphics.fill(gx, gy, gx + 16, gy + 16, gHov ? 0xFF333B4D : 0xFF14161D);
                        graphics.centeredText(this.font, "⚙", gx + 8, gy + 4, gHov || isSelected ? 0xFF58A6FF : 0xFF8B949E);
                }
                graphics.disableScissor();

                // Right pane: Selected module settings
                int rightX = wx + leftW + 14;
                int rightW = WIN_W - leftW - 22;
                boolean hasStringList = false;

                if (selectedModule == null) {
                        graphics.centeredText(this.font, "Right-click or click ⚙", rightX + rightW / 2, bodyTop + 70, 0xFF8B949E);
                        graphics.centeredText(this.font, "to configure a module", rightX + rightW / 2, bodyTop + 84, 0xFF8B949E);
                        stringListInput.setVisible(false);
                } else {
                        graphics.text(this.font, Component.literal(selectedModule.getName()).withStyle(ChatFormatting.BOLD), rightX, bodyTop + 2, 0xFFFFFFFF, false);
                        graphics.textWithWordWrap(this.font, Component.literal(selectedModule.getDescription()), rightX, bodyTop + 14, rightW, 0xFF8B949E);

                        int sy = bodyTop + 36 - (int) settingsScroll;
                        graphics.enableScissor(rightX - 2, bodyTop + 34, rightX + rightW + 2, wy + WIN_H - 6);

                        // Keybind row
                        String kbText = listeningForKeybind
                                        ? "> Press Key (ESC to Clear) <"
                                        : "Keybind: " + getKeyName(selectedModule.getKeyCode());
                        boolean kbHov = isInside(mouseX, mouseY, rightX, sy, rightW, 18);
                        graphics.fill(rightX, sy, rightX + rightW, sy + 18, listeningForKeybind ? 0xFF3B2E1E : (kbHov ? 0xFF282E3E : 0xFF1E2330));
                        graphics.outline(rightX, sy, rightW, 18, listeningForKeybind ? 0xFFD29922 : 0xFF3A4256);
                        graphics.centeredText(this.font, kbText, rightX + rightW / 2, sy + 5, 0xFFFFFFFF);
                        sy += 24;

                        // Special button for Command Keys macro editor
                        if (selectedModule instanceof CommandKeysModule) {
                                boolean cmHov = isInside(mouseX, mouseY, rightX, sy, rightW, 20);
                                graphics.fill(rightX, sy, rightX + rightW, sy + 20, cmHov ? 0xFF2E5B88 : 0xFF23384E);
                                graphics.outline(rightX, sy, rightW, 20, 0xFF58A6FF);
                                graphics.centeredText(this.font, "Open Command Keys Editor", rightX + rightW / 2, sy + 6, 0xFFFFFFFF);
                                sy += 26;
                        }

                        for (Setting<?> s : selectedModule.getSettings()) {
                                if (s instanceof BooleanSetting bs) {
                                        graphics.text(this.font, s.getName(), rightX, sy + 4, 0xFFE6EDF3, false);
                                        int bx = rightX + rightW - 28;
                                        graphics.fill(bx, sy + 2, bx + 28, sy + 14, bs.isEnabled() ? 0xFF238636 : 0xFF2B313E);
                                        graphics.outline(bx, sy + 2, 28, 12, bs.isEnabled() ? 0xFF3FB950 : 0xFF484F58);
                                        graphics.centeredText(this.font, bs.isEnabled() ? "ON" : "OFF", bx + 14, sy + 4, 0xFFFFFFFF);
                                        sy += 20;
                                } else if (s instanceof DropdownSetting ds) {
                                        graphics.text(this.font, s.getName(), rightX, sy + 4, 0xFFE6EDF3, false);
                                        int bw = Math.min(115, this.font.width(ds.getValue()) + 12);
                                        int bx = rightX + rightW - bw;
                                        boolean hov = isInside(mouseX, mouseY, bx, sy + 1, bw, 15);
                                        graphics.fill(bx, sy + 1, bx + bw, sy + 16, hov ? 0xFF2D3548 : 0xFF202532);
                                        graphics.outline(bx, sy + 1, bw, 15, 0xFF3A4256);
                                        graphics.centeredText(this.font, ds.getValue(), bx + bw / 2, sy + 4, 0xFF58A6FF);
                                        sy += 21;
                                } else if (s instanceof NumberSetting ns) {
                                        String valStr = String.format(Locale.ROOT, "%.2f", ns.getValue());
                                        graphics.text(this.font, s.getName(), rightX, sy + 2, 0xFFE6EDF3, false);
                                        graphics.text(this.font, valStr, rightX + rightW - this.font.width(valStr), sy + 2, 0xFF58A6FF, false);
                                        int slY = sy + 13;
                                        graphics.fill(rightX, slY, rightX + rightW, slY + 5, 0xFF232836);
                                        double ratio = (ns.getValue() - ns.getMin()) / Math.max(0.0001, ns.getMax() - ns.getMin());
                                        int fw = (int) Math.round(ratio * rightW);
                                        graphics.fill(rightX, slY, rightX + fw, slY + 5, 0xFF58A6FF);
                                        sy += 24;
                                } else if (s instanceof ColorSetting cs) {
                                        graphics.text(this.font, s.getName(), rightX, sy + 2, 0xFFE6EDF3, false);
                                        graphics.fill(rightX + rightW - 18, sy + 1, rightX + rightW, sy + 11, cs.getColor());
                                        graphics.outline(rightX + rightW - 18, sy + 1, 18, 10, 0xFFFFFFFF);
                                        sy += 13;
                                        int[] vals = {cs.getRed(), cs.getGreen(), cs.getBlue(), cs.getAlpha()};
                                        int[] barCols = {0xFFFF4444, 0xFF44FF44, 0xFF4488FF, 0xFFCCCCCC};
                                        String[] chNames = {"R", "G", "B", "A"};
                                        for (int c = 0; c < 4; c++) {
                                                graphics.text(this.font, chNames[c], rightX, sy, barCols[c], false);
                                                int cx = rightX + 12, cw = rightW - 12;
                                                graphics.fill(cx, sy + 2, cx + cw, sy + 6, 0xFF232836);
                                                int fw = Math.round((vals[c] / 255.0f) * cw);
                                                graphics.fill(cx, sy + 2, cx + fw, sy + 6, barCols[c]);
                                                sy += 10;
                                        }
                                        sy += 4;
                                } else if (s instanceof StringListSetting sls) {
                                        hasStringList = true;
                                        graphics.text(this.font, s.getName() + ":", rightX, sy + 2, 0xFFE6EDF3, false);
                                        sy += 14;
                                        List<String> items = sls.getValue();
                                        for (int idx = 0; idx < items.size(); idx++) {
                                                String word = items.get(idx);
                                                graphics.fill(rightX, sy, rightX + rightW, sy + 14, 0xFF1D222E);
                                                graphics.text(this.font, word, rightX + 4, sy + 3, 0xFFE6EDF3, false);
                                                graphics.centeredText(this.font, "x", rightX + rightW - 8, sy + 3, 0xFFFF5555);
                                                sy += 16;
                                        }
                                        sy += 24;
                                }
                        }
                        graphics.disableScissor();

                        if (hasStringList) {
                                stringListInput.setVisible(true);
                                int addX = rightX + rightW - 34, addY = wy + WIN_H - 28;
                                graphics.fill(addX, addY, addX + 34, addY + 16, 0xFF238636);
                                graphics.centeredText(this.font, "Add", addX + 17, addY + 4, 0xFFFFFFFF);
                        } else {
                                stringListInput.setVisible(false);
                        }
                }

                super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                if (super.mouseClicked(event, doubleClick)) {
                        return true;
                }
                double mx = event.x(), my = event.y();
                int btn = event.button();

                if (listeningForKeybind) {
                        listeningForKeybind = false;
                        return true;
                }

                if (!isInside(mx, my, wx, wy, WIN_W, WIN_H)) {
                        this.onClose();
                        return true;
                }

                // Edit HUD button
                int hudBtnX = wx + WIN_W - 84, hudBtnY = wy + 6;
                if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, hudBtnX, hudBtnY, 74, 18)) {
                        Minecraft.getInstance().setScreenAndShow(new HudEditorScreen(this));
                        return true;
                }

                // Category tabs
                int tabX = wx + 178, tabY = wy + HEADER_H + 5;
                String[] tabLabels = {"All", "Combat", "Render", "HUD", "Utility"};
                Category[] tabCats = {null, Category.COMBAT, Category.RENDER, Category.HUD, Category.UTILITY};
                for (int i = 0; i < tabLabels.length; i++) {
                        int tw = this.font.width(tabLabels[i]) + 10;
                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, tabX, tabY, tw, 16)) {
                                selectedCategory = tabCats[i];
                                moduleScroll = 0.0;
                                return true;
                        }
                        tabX += tw + 4;
                }

                int bodyTop = wy + HEADER_H + TAB_H + 4;
                int leftW = 205, listX = wx + 8;

                // Left pane module clicks
                if (mx >= listX && mx <= wx + leftW + 4 && my >= bodyTop && my <= wy + WIN_H - 6) {
                        List<Module> modules = getFilteredModules();
                        int rowH = 26, rowGap = 4;
                        for (int i = 0; i < modules.size(); i++) {
                                Module m = modules.get(i);
                                int ry = bodyTop + i * (rowH + rowGap) - (int) moduleScroll;
                                if (isInside(mx, my, listX, ry, leftW - 8, rowH)) {
                                        int gx = listX + leftW - 28, gy = ry + 5;
                                        if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || isInside(mx, my, gx, gy, 16, 16)) {
                                                selectedModule = m;
                                                settingsScroll = 0.0;
                                                listeningForKeybind = false;
                                        } else if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                                m.toggle();
                                        }
                                        return true;
                                }
                        }
                        return true;
                }

                // Right pane settings clicks
                if (selectedModule != null) {
                        int rightX = wx + leftW + 14;
                        int rightW = WIN_W - leftW - 22;
                        int sy = bodyTop + 36 - (int) settingsScroll;

                        // Keybind row
                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX, sy, rightW, 18)) {
                                listeningForKeybind = true;
                                return true;
                        }
                        sy += 24;

                        if (selectedModule instanceof CommandKeysModule) {
                                if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX, sy, rightW, 20)) {
                                        Minecraft.getInstance().setScreenAndShow(new CommandMenuScreen(this));
                                        return true;
                                }
                                sy += 26;
                        }

                        for (Setting<?> s : selectedModule.getSettings()) {
                                if (s instanceof BooleanSetting bs) {
                                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX, sy, rightW, 18)) {
                                                bs.toggle();
                                                MistConfig.save();
                                                return true;
                                        }
                                        sy += 20;
                                } else if (s instanceof DropdownSetting ds) {
                                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX, sy, rightW, 18)) {
                                                ds.cycle();
                                                MistConfig.save();
                                                return true;
                                        }
                                        sy += 21;
                                } else if (s instanceof NumberSetting ns) {
                                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX, sy + 10, rightW, 10)) {
                                                draggingSlider = ns;
                                                updateNumberSlider(mx, rightX, rightW);
                                                return true;
                                        }
                                        sy += 24;
                                } else if (s instanceof ColorSetting cs) {
                                        sy += 13;
                                        for (int c = 0; c < 4; c++) {
                                                int cx = rightX + 12, cw = rightW - 12;
                                                if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, cx, sy, cw, 9)) {
                                                        draggingColor = cs;
                                                        draggingColorChannel = c;
                                                        updateColorSlider(mx, cx, cw);
                                                        return true;
                                                }
                                                sy += 10;
                                        }
                                        sy += 4;
                                } else if (s instanceof StringListSetting sls) {
                                        sy += 14;
                                        List<String> items = sls.getValue();
                                        for (int idx = 0; idx < items.size(); idx++) {
                                                if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, rightX + rightW - 16, sy, 16, 14)) {
                                                        sls.removeEntry(idx);
                                                        MistConfig.save();
                                                        return true;
                                                }
                                                sy += 16;
                                        }
                                        int addX = rightX + rightW - 34, addY = wy + WIN_H - 28;
                                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mx, my, addX, addY, 34, 16)) {
                                                sls.addEntry(stringListInput.getValue());
                                                stringListInput.setValue("");
                                                MistConfig.save();
                                                return true;
                                        }
                                        sy += 24;
                                }
                        }
                }
                return true;
        }

        private void updateNumberSlider(double mx, int rx, int rw) {
                if (draggingSlider == null) return;
                double ratio = Math.max(0.0, Math.min(1.0, (mx - rx) / rw));
                double val = draggingSlider.getMin() + ratio * (draggingSlider.getMax() - draggingSlider.getMin());
                draggingSlider.setValue(val);
        }

        private void updateColorSlider(double mx, int cx, int cw) {
                if (draggingColor == null || draggingColorChannel < 0) return;
                int v = (int) Math.round(Math.max(0.0, Math.min(1.0, (mx - cx) / cw)) * 255.0);
                int r = draggingColor.getRed(), g = draggingColor.getGreen(), b = draggingColor.getBlue(), a = draggingColor.getAlpha();
                switch (draggingColorChannel) {
                        case 0 -> r = v;
                        case 1 -> g = v;
                        case 2 -> b = v;
                        case 3 -> a = v;
                }
                draggingColor.setChannel(a, r, g, b);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
                if (draggingSlider != null) {
                        int rightX = wx + 205 + 14;
                        int rightW = WIN_W - 205 - 22;
                        updateNumberSlider(event.x(), rightX, rightW);
                        return true;
                }
                if (draggingColor != null) {
                        int rightX = wx + 205 + 14;
                        int rightW = WIN_W - 205 - 22;
                        updateColorSlider(event.x(), rightX + 12, rightW - 12);
                        return true;
                }
                return super.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
                if (draggingSlider != null || draggingColor != null) {
                        draggingSlider = null;
                        draggingColor = null;
                        draggingColorChannel = -1;
                        MistConfig.save();
                        return true;
                }
                return super.mouseReleased(event);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
                int bodyTop = wy + HEADER_H + TAB_H + 4;
                int leftW = 205;
                if (mx <= wx + leftW + 6) {
                        moduleScroll = Math.max(0.0, moduleScroll - v * 18.0);
                } else {
                        settingsScroll = Math.max(0.0, settingsScroll - v * 18.0);
                }
                return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
                int k = event.key();
                if (listeningForKeybind && selectedModule != null) {
                        selectedModule.setKeyCode(k == GLFW.GLFW_KEY_ESCAPE ? -1 : k);
                        listeningForKeybind = false;
                        return true;
                }
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                        this.onClose();
                        return true;
                }
                return super.keyPressed(event);
        }

        @Override
        public void onClose() {
                MistConfig.save();
                Minecraft.getInstance().setScreenAndShow(this.parent);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }

        private static String getKeyName(int keyCode) {
                if (keyCode <= 0) return "None";
                return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
        }

        private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
                return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
}
