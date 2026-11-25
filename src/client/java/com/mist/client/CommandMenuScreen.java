package com.mist.client;

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

import java.util.Collections;
import java.util.List;

/**
 * Command Keys macro configuration & execution menu.
 * Vanish HUD positioning has been moved to the unified HudEditorScreen.
 */
public class CommandMenuScreen extends Screen {
        private final Screen parent;
        private static final Component TITLE_TEXT = Component.literal("COMMAND KEYS").withStyle(ChatFormatting.BOLD);
        private static final Component REORDER_TITLE = Component.literal("REORDER MACROS").withStyle(ChatFormatting.BOLD);
        private static final Component CREATE_TITLE = Component.literal("Create Command Box").withStyle(ChatFormatting.BOLD);
        private static final Component EDIT_TITLE = Component.literal("Edit Command Box").withStyle(ChatFormatting.BOLD);

        private static final int PANEL_WIDTH = 348, PANEL_HEIGHT = 232, HEADER_HEIGHT = 26;
        private static final int COLS = 2, BOX_WIDTH = 150, BOX_HEIGHT = 38, GAP = 10;

        private int panelX, panelY, maxScroll = 0, editingIndex = -1, lastClickedIndex = -1;
        private double scrollOffset = 0.0;
        private boolean draggingScrollbar = false, modalOpen = false, isCreatingNew = false, listeningForKey = false;
        private int tempKeyCode = -1;
        private String cachedTempKeyLabel = "Keybind: None";
        private long lastClickTime = 0L;

        // Reorder Mode State (Command Boxes only)
        private boolean reorderMode = false;
        private int draggingBoxIndex = -1;
        private int selectedSwapIndex = -1;
        private boolean didDragSwap = false;

        private EditBox nameField, commandField;

        public CommandMenuScreen(Screen parent) {
                super(Component.literal("MIST Command Keys"));
                this.parent = parent;
        }

        @Override
        protected void init() {
                this.panelX = (this.width - PANEL_WIDTH) / 2;
                this.panelY = Math.min(this.height - PANEL_HEIGHT - 12, (this.height - PANEL_HEIGHT) / 2 + 18);
                int mx = (this.width - 220) / 2, my = (this.height - 165) / 2;

                this.nameField = new EditBox(this.font, mx + 14, my + 36, 192, 18, Component.literal("Name"));
                this.nameField.setMaxLength(32);
                this.nameField.setVisible(modalOpen);
                this.addRenderableWidget(this.nameField);

                this.commandField = new EditBox(this.font, mx + 14, my + 74, 192, 18, Component.literal("Command"));
                this.commandField.setMaxLength(256);
                this.commandField.setVisible(modalOpen);
                this.addRenderableWidget(this.commandField);
        }

        private void updateTempKeyLabel() {
                if (tempKeyCode <= 0) cachedTempKeyLabel = "Keybind: None";
                else cachedTempKeyLabel = "Keybind: " + InputConstants.Type.KEYSYM.getOrCreate(tempKeyCode).getDisplayName().getString();
        }

        private void setFieldsEditable(boolean e) {
                if (this.nameField != null) this.nameField.setEditable(e);
                if (this.commandField != null) this.commandField.setEditable(e);
        }

        private void openModal(int index, boolean creating) {
                this.modalOpen = true;
                this.isCreatingNew = creating;
                this.editingIndex = index;
                this.listeningForKey = false;
                setFieldsEditable(true);
                if (creating) {
                        this.nameField.setValue("");
                        this.commandField.setValue("/");
                        this.tempKeyCode = -1;
                } else {
                        CommandBoxEntry entry = CommandKeysConfig.boxes.get(index);
                        this.nameField.setValue(entry.name);
                        this.commandField.setValue(entry.command);
                        this.tempKeyCode = entry.keyCode;
                }
                updateTempKeyLabel();
                this.nameField.setVisible(true);
                this.commandField.setVisible(true);
                this.setFocused(this.nameField);
                this.nameField.setFocused(true);
                this.commandField.setFocused(false);
        }

        private void closeModal() {
                this.modalOpen = false;
                this.listeningForKey = false;
                setFieldsEditable(true);
                this.nameField.setVisible(false);
                this.commandField.setVisible(false);
                this.setFocused(null);
        }

        private int getViewportTop() { return panelY + HEADER_HEIGHT + 8; }
        private int getViewportBottom() { return panelY + PANEL_HEIGHT - 8; }
        private int getViewportHeight() { return getViewportBottom() - getViewportTop(); }

        private void recalculateScrollBounds() {
                int totalRows = (CommandKeysConfig.boxes.size() + COLS - 1) / COLS;
                int plusSpace = reorderMode ? 4 : 30;
                int contentHeight = totalRows * (BOX_HEIGHT + GAP) + plusSpace;
                this.maxScroll = Math.max(0, contentHeight - getViewportHeight());
                this.scrollOffset = Math.max(0.0, Math.min(this.maxScroll, this.scrollOffset));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                recalculateScrollBounds();
                graphics.fill(0, 0, this.width, this.height, 0x77000000);

                // Main Panel & Header
                graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE614161D);
                graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, reorderMode ? 0xFFD29922 : 0xFF2E3446);
                graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, reorderMode ? 0xFF262018 : 0xFF1C202B);
                if (reorderMode) {
                        graphics.fill(panelX + 1, panelY + HEADER_HEIGHT - 1, panelX + PANEL_WIDTH - 1, panelY + HEADER_HEIGHT, 0xFFD29922);
                }
                graphics.text(this.font, reorderMode ? REORDER_TITLE : TITLE_TEXT, panelX + 12, panelY + 9, reorderMode ? 0xFFFFD866 : 0xFFFFFFFF, false);

                // Top-Right Toggle Button: "✥" in normal mode, "Done" in reorder mode
                int btnW = reorderMode ? 48 : 22;
                int btnH = 18;
                int btnX = panelX + PANEL_WIDTH - btnW - 8;
                int btnY = panelY + 4;
                boolean btnHover = !modalOpen && isInside(mouseX, mouseY, btnX, btnY, btnW, btnH);
                int btnBg = reorderMode ? (btnHover ? 0xFF3E301B : 0xFF2E2415) : (btnHover ? 0xFF2D3548 : 0xFF232836);
                int btnBrd = reorderMode ? 0xFFD29922 : (btnHover ? 0xFF58A6FF : 0xFF3A4256);
                graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
                graphics.outline(btnX, btnY, btnW, btnH, btnBrd);
                drawCenteredText(graphics, reorderMode ? "Done" : "✥", btnX + btnW / 2, btnY + 5, reorderMode ? 0xFFFFD866 : 0xFFFFFFFF);

                int viewTop = getViewportTop(), viewBottom = getViewportBottom(), viewHeight = getViewportHeight();
                int gridStartX = panelX + 14, scrollY = (int) Math.round(this.scrollOffset);
                boolean mouseInView = !modalOpen && isInside(mouseX, mouseY, panelX, viewTop, PANEL_WIDTH, viewHeight);

                graphics.enableScissor(panelX + 1, viewTop, panelX + PANEL_WIDTH - 1, viewBottom);
                List<CommandBoxEntry> boxes = CommandKeysConfig.boxes;

                for (int i = 0; i < boxes.size(); i++) {
                        CommandBoxEntry box = boxes.get(i);
                        int bx = gridStartX + (i % COLS) * (BOX_WIDTH + GAP);
                        int by = viewTop + (i / COLS) * (BOX_HEIGHT + GAP) - scrollY;
                        if (by + BOX_HEIGHT < viewTop || by > viewBottom) continue;

                        boolean isBeingDragged = reorderMode && (i == draggingBoxIndex);
                        boolean isSwapSelected = reorderMode && (i == selectedSwapIndex);
                        boolean hovered = mouseInView && isInside(mouseX, mouseY, bx, by, BOX_WIDTH, BOX_HEIGHT);
                        boolean clicked = !reorderMode && (i == lastClickedIndex) && (System.currentTimeMillis() - lastClickTime < 150);

                        int bgCol = clicked ? 0xFF2E5B88 : (isBeingDragged ? 0x66161922 : (isSwapSelected ? 0xFF33291A : (hovered ? 0xFF252B3B : 0xFF1D212D)));
                        int brdCol = (isBeingDragged || isSwapSelected) ? 0xFFD29922 : (hovered ? (reorderMode ? 0xFFE3B341 : 0xFF58A6FF) : 0xFF303646);

                        graphics.fill(bx, by, bx + BOX_WIDTH, by + BOX_HEIGHT, bgCol);
                        graphics.outline(bx, by, BOX_WIDTH, BOX_HEIGHT, brdCol);

                        if (reorderMode) {
                                String slotTag = "#" + (i + 1) + " ";
                                int tagW = this.font.width(slotTag);
                                graphics.text(this.font, slotTag, bx + 7, by + 6, 0xFFD29922, false);

                                String tName = this.font.plainSubstrByWidth(box.name, BOX_WIDTH - 48 - tagW);
                                if (!tName.equals(box.cachedSourceName)) {
                                        box.cachedSourceName = tName;
                                        box.cachedBoldName = Component.literal(tName).withStyle(ChatFormatting.BOLD);
                                }
                                graphics.text(this.font, box.cachedBoldName, bx + 7 + tagW, by + 6, 0xFFFFFFFF, false);

                                int lBtnX = bx + BOX_WIDTH - 32, rBtnX = bx + BOX_WIDTH - 16, nBtnY = by + 5;
                                boolean lHov = hovered && isInside(mouseX, mouseY, lBtnX, nBtnY, 13, 12) && i > 0;
                                boolean rHov = hovered && isInside(mouseX, mouseY, rBtnX, nBtnY, 13, 12) && i < boxes.size() - 1;
                                graphics.fill(lBtnX, nBtnY, lBtnX + 13, nBtnY + 12, lHov ? 0xFF3B4358 : 0xFF161922);
                                graphics.fill(rBtnX, nBtnY, rBtnX + 13, nBtnY + 12, rHov ? 0xFF3B4358 : 0xFF161922);
                                drawCenteredText(graphics, "<", lBtnX + 6, nBtnY + 2, i > 0 ? (lHov ? 0xFFFFD866 : 0xFF8B949E) : 0xFF3A3F4B);
                                drawCenteredText(graphics, ">", rBtnX + 7, nBtnY + 2, i < boxes.size() - 1 ? (rHov ? 0xFFFFD866 : 0xFF8B949E) : 0xFF3A3F4B);

                                var pose = graphics.pose();
                                pose.pushMatrix();
                                pose.translate(bx + 7, by + 23);
                                pose.scale(0.75f, 0.75f);
                                graphics.text(this.font, ":: Drag or click to swap", 0, 0, 0xFF8892B0, false);
                                pose.popMatrix();
                        } else {
                                String badge = box.getKeyBadge();
                                int bw = badge.isEmpty() ? 0 : this.font.width(badge) + 6;
                                String tName = this.font.plainSubstrByWidth(box.name, BOX_WIDTH - 14 - bw);
                                if (!tName.equals(box.cachedSourceName)) {
                                        box.cachedSourceName = tName;
                                        box.cachedBoldName = Component.literal(tName).withStyle(ChatFormatting.BOLD);
                                }
                                graphics.text(this.font, box.cachedBoldName, bx + 8, by + 6, 0xFFFFFFFF, false);
                                if (!badge.isEmpty()) graphics.text(this.font, badge, bx + BOX_WIDTH - bw, by + 6, 0xFF58A6FF, false);

                                var pose = graphics.pose();
                                pose.pushMatrix();
                                pose.translate(bx + 8, by + 23);
                                pose.scale(0.78f, 0.78f);
                                graphics.text(this.font, this.font.plainSubstrByWidth(box.command, (int) ((BOX_WIDTH - 16) / 0.78f)), 0, 0, 0xFF8B949E, false);
                                pose.popMatrix();
                        }
                }

                if (!reorderMode) {
                        int plusY = viewTop + ((boxes.size() + COLS - 1) / COLS) * (BOX_HEIGHT + GAP) - scrollY;
                        int plusW = COLS * BOX_WIDTH + GAP;
                        if (plusY + 26 >= viewTop && plusY <= viewBottom) {
                                boolean pHov = mouseInView && isInside(mouseX, mouseY, gridStartX, plusY, plusW, 26);
                                graphics.fill(gridStartX, plusY, gridStartX + plusW, plusY + 26, pHov ? 0xFF23382E : 0xFF192420);
                                graphics.outline(gridStartX, plusY, plusW, 26, pHov ? 0xFF3FB950 : 0xFF2B4434);
                                drawCenteredText(graphics, "+", gridStartX + plusW / 2, plusY + 9, 0xFF3FB950);
                        }
                }

                if (reorderMode && draggingBoxIndex >= 0 && draggingBoxIndex < boxes.size() && didDragSwap) {
                        CommandBoxEntry dBox = boxes.get(draggingBoxIndex);
                        int fx = mouseX - BOX_WIDTH / 2;
                        int fy = mouseY - BOX_HEIGHT / 2;
                        graphics.fill(fx, fy, fx + BOX_WIDTH, fy + BOX_HEIGHT, 0xE6282218);
                        graphics.outline(fx, fy, BOX_WIDTH, BOX_HEIGHT, 0xFFFFD866);
                        graphics.text(this.font, dBox.cachedBoldName, fx + 8, fy + 14, 0xFFFFD866, false);
                }

                graphics.disableScissor();

                if (this.maxScroll > 0) {
                        int tx = panelX + PANEL_WIDTH - 8;
                        graphics.fill(tx, viewTop, tx + 4, viewBottom, 0xFF101218);
                        int th = Math.max(24, (int) ((float) (viewHeight * viewHeight) / (viewHeight + this.maxScroll)));
                        int ty = viewTop + (int) ((this.scrollOffset / this.maxScroll) * (viewHeight - th));
                        boolean tHov = !modalOpen && (draggingScrollbar || isInside(mouseX, mouseY, tx - 2, viewTop, 8, viewHeight));
                        graphics.fill(tx, ty, tx + 4, ty + th, tHov ? 0xFF58A6FF : 0xFF424B61);
                }

                if (modalOpen) {
                        graphics.fill(0, 0, this.width, this.height, 0x88000000);
                        int mx = (this.width - 220) / 2, my = (this.height - 165) / 2;
                        graphics.fill(mx, my, mx + 220, my + 165, 0xFF181B24);
                        graphics.outline(mx, my, 220, 165, 0xFF58A6FF);
                        graphics.text(this.font, isCreatingNew ? CREATE_TITLE : EDIT_TITLE, mx + 14, my + 10, 0xFFFFFFFF, false);
                        graphics.text(this.font, "Name:", mx + 14, my + 25, 0xFF8B949E, false);
                        graphics.text(this.font, "Command:", mx + 14, my + 63, 0xFF8B949E, false);
                        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

                        int kbY = my + 102; btnY = my + 132;
                        boolean kbHov = isInside(mouseX, mouseY, mx + 14, kbY, 192, 20);
                        graphics.fill(mx + 14, kbY, mx + 206, kbY + 20, listeningForKey ? 0xFF3B2E1E : (kbHov ? 0xFF282E3E : 0xFF202532));
                        graphics.outline(mx + 14, kbY, 192, 20, listeningForKey ? 0xFFD29922 : 0xFF3A4256);
                        drawCenteredText(graphics, listeningForKey ? "> Press Key (ESC to Clear) <" : cachedTempKeyLabel, mx + 110, kbY + 6, 0xFFFFFFFF);

                        graphics.fill(mx + 14, btnY, mx + 74, btnY + 20, isInside(mouseX, mouseY, mx + 14, btnY, 60, 20) ? 0xFF2EA043 : 0xFF238636);
                        drawCenteredText(graphics, "Save", mx + 44, btnY + 6, 0xFFFFFFFF);
                        graphics.fill(mx + 80, btnY, mx + 140, btnY + 20, isInside(mouseX, mouseY, mx + 80, btnY, 60, 20) ? 0xFF373E4D : 0xFF2B313E);
                        drawCenteredText(graphics, "Cancel", mx + 110, btnY + 6, 0xFFFFFFFF);
                        if (!isCreatingNew) {
                                graphics.fill(mx + 146, btnY, mx + 206, btnY + 20, isInside(mouseX, mouseY, mx + 146, btnY, 60, 20) ? 0xFFDA3633 : 0xFFB62324);
                                drawCenteredText(graphics, "Delete", mx + 176, btnY + 6, 0xFFFFFFFF);
                        }
                }
        }

        private int getBoxIndexAt(double mx, double my) {
                int vt = getViewportTop(), vb = getViewportBottom();
                if (my < vt || my > vb) return -1;
                int gx = panelX + 14, sy = (int) Math.round(this.scrollOffset);
                List<CommandBoxEntry> boxes = CommandKeysConfig.boxes;
                for (int i = 0; i < boxes.size(); i++) {
                        int bx = gx + (i % COLS) * (BOX_WIDTH + GAP);
                        int by = vt + (i / COLS) * (BOX_HEIGHT + GAP) - sy;
                        if (isInside(mx, my, bx, by, BOX_WIDTH, BOX_HEIGHT)) return i;
                }
                return -1;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
                if (!modalOpen && this.maxScroll > 0) {
                        this.scrollOffset = Math.max(0.0, Math.min(this.maxScroll, this.scrollOffset - v * 22.0));
                        return true;
                }
                return super.mouseScrolled(mx, my, h, v);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
                if (this.draggingScrollbar && this.maxScroll > 0) {
                        int vt = getViewportTop(), vh = getViewportHeight();
                        int th = Math.max(24, (int) ((float) (vh * vh) / (vh + this.maxScroll)));
                        double ratio = (e.y() - vt - th / 2.0) / Math.max(1, vh - th);
                        this.scrollOffset = Math.max(0.0, Math.min(this.maxScroll, ratio * this.maxScroll));
                        return true;
                }
                if (this.reorderMode && this.draggingBoxIndex >= 0) {
                        this.didDragSwap = true;
                        int hoverIdx = getBoxIndexAt(e.x(), e.y());
                        if (hoverIdx >= 0 && hoverIdx != this.draggingBoxIndex) {
                                CommandBoxEntry moved = CommandKeysConfig.boxes.remove(this.draggingBoxIndex);
                                CommandKeysConfig.boxes.add(hoverIdx, moved);
                                this.draggingBoxIndex = hoverIdx;
                                this.selectedSwapIndex = -1;
                                CommandKeysConfig.save();
                        }
                        return true;
                }
                return super.mouseDragged(e, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent e) {
                this.draggingScrollbar = false;
                this.draggingBoxIndex = -1;
                this.didDragSwap = false;
                return super.mouseReleased(e);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
                double mouseX = event.x(), mouseY = event.y();
                int button = event.button();

                if (modalOpen) {
                        if (listeningForKey) { listeningForKey = false; setFieldsEditable(true); return true; }
                        int mx = (this.width - 220) / 2, my = (this.height - 165) / 2, kbY = my + 102, btnY = my + 132;
                        if (isInside(mouseX, mouseY, mx + 14, kbY, 192, 20)) { setFieldsEditable(false); listeningForKey = true; return true; }
                        if (isInside(mouseX, mouseY, mx + 14, btnY, 60, 20)) {
                                String n = this.nameField.getValue().trim().isEmpty() ? "Command" : this.nameField.getValue().trim();
                                String c = this.commandField.getValue().trim();
                                if (isCreatingNew) CommandKeysConfig.boxes.add(new CommandBoxEntry(n, c, tempKeyCode));
                                else if (editingIndex >= 0 && editingIndex < CommandKeysConfig.boxes.size()) {
                                        CommandBoxEntry entry = CommandKeysConfig.boxes.get(editingIndex);
                                        entry.name = n; entry.command = c; entry.keyCode = tempKeyCode;
                                        entry.invalidateCache();
                                }
                                CommandKeysConfig.save(); closeModal(); return true;
                        }
                        if (isInside(mouseX, mouseY, mx + 80, btnY, 60, 20)) { closeModal(); return true; }
                        if (!isCreatingNew && isInside(mouseX, mouseY, mx + 146, btnY, 60, 20)) {
                                if (editingIndex >= 0 && editingIndex < CommandKeysConfig.boxes.size()) {
                                        CommandKeysConfig.boxes.remove(editingIndex); CommandKeysConfig.save();
                                }
                                closeModal(); return true;
                        }
                        setFieldsEditable(true);
                        return super.mouseClicked(event, bl);
                }

                if (!isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
                        this.onClose();
                        return true;
                }

                int btnW = reorderMode ? 48 : 22;
                int btnX = panelX + PANEL_WIDTH - btnW - 8, btnY = panelY + 4;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mouseX, mouseY, btnX, btnY, btnW, 18)) {
                        this.reorderMode = !this.reorderMode;
                        this.draggingBoxIndex = -1;
                        this.selectedSwapIndex = -1;
                        return true;
                }

                int vt = getViewportTop(), vb = getViewportBottom();
                if (this.maxScroll > 0 && isInside(mouseX, mouseY, panelX + PANEL_WIDTH - 10, vt, 10, getViewportHeight())) {
                        this.draggingScrollbar = true; return true;
                }
                if (mouseY < vt || mouseY > vb) return true;

                List<CommandBoxEntry> boxes = CommandKeysConfig.boxes;
                int gx = panelX + 14, sy = (int) Math.round(this.scrollOffset);

                for (int i = 0; i < boxes.size(); i++) {
                        int bx = gx + (i % COLS) * (BOX_WIDTH + GAP), by = vt + (i / COLS) * (BOX_HEIGHT + GAP) - sy;
                        if (isInside(mouseX, mouseY, bx, by, BOX_WIDTH, BOX_HEIGHT)) {
                                if (reorderMode) {
                                        int lBtnX = bx + BOX_WIDTH - 32, rBtnX = bx + BOX_WIDTH - 16, nBtnY = by + 5;
                                        if (isInside(mouseX, mouseY, lBtnX, nBtnY, 13, 12) && i > 0) {
                                                Collections.swap(boxes, i, i - 1);
                                                CommandKeysConfig.save();
                                                return true;
                                        }
                                        if (isInside(mouseX, mouseY, rBtnX, nBtnY, 13, 12) && i < boxes.size() - 1) {
                                                Collections.swap(boxes, i, i + 1);
                                                CommandKeysConfig.save();
                                                return true;
                                        }
                                        if (selectedSwapIndex == -1) {
                                                selectedSwapIndex = i;
                                        } else if (selectedSwapIndex == i) {
                                                selectedSwapIndex = -1;
                                        } else {
                                                Collections.swap(boxes, selectedSwapIndex, i);
                                                selectedSwapIndex = -1;
                                                CommandKeysConfig.save();
                                        }
                                        draggingBoxIndex = i;
                                        didDragSwap = false;
                                        return true;
                                } else {
                                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                                lastClickedIndex = i; lastClickTime = System.currentTimeMillis();
                                                CommandKeysConfig.executeCommand(boxes.get(i).command); return true;
                                        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                                                openModal(i, false); return true;
                                        }
                                }
                        }
                }

                if (!reorderMode) {
                        int plusY = vt + ((boxes.size() + COLS - 1) / COLS) * (BOX_HEIGHT + GAP) - sy;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInside(mouseX, mouseY, gx, plusY, COLS * BOX_WIDTH + GAP, 26)) {
                                openModal(-1, true); return true;
                        }
                }
                return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
                int k = event.key();
                if (modalOpen) {
                        if (listeningForKey) {
                                tempKeyCode = (k == GLFW.GLFW_KEY_ESCAPE) ? -1 : k;
                                updateTempKeyLabel(); listeningForKey = false; return true;
                        }
                        if (k == GLFW.GLFW_KEY_ESCAPE) { closeModal(); return true; }
                        return super.keyPressed(event);
                }
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                        if (reorderMode) {
                                reorderMode = false;
                                selectedSwapIndex = -1;
                                draggingBoxIndex = -1;
                                return true;
                        }
                        this.onClose(); return true;
                }
                return super.keyPressed(event);
        }

        @Override
        public void onClose() {
                Minecraft.getInstance().setScreenAndShow(this.parent);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }

        private void drawCenteredText(GuiGraphicsExtractor g, String t, int cx, int y, int col) {
                g.text(this.font, t, cx - this.font.width(t) / 2, y, col, false);
        }

        private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
                return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
}
