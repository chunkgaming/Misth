package com.mist.client.gui;

import com.mist.client.config.MistConfig;
import com.mist.client.module.HudModule;
import com.mist.client.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Unified Drag-and-Drop HUD Editor for Vanish HUD, Potion Effect Timer,
 * Armor HUD, Numeric Health, and Custom Crosshair preview.
 */
public class HudEditorScreen extends Screen {
        private static final int SNAP_THRESHOLD = 5;
        private final Screen parent;

        private HudModule selectedHud = null;
        private HudModule draggingHud = null;
        private int grabOffsetX = 0;
        private int grabOffsetY = 0;
        private boolean draggingScaleSlider = false;

        // Active snap guide lines (negative = inactive)
        private int snapGuideX = -1;
        private int snapGuideY = -1;

        public HudEditorScreen(Screen parent) {
                super(Component.literal("MIST HUD Editor"));
                this.parent = parent;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(0, 0, this.width, this.height, 0x66000000);

                // Top instruction bar
                String hint = "Drag HUD elements to move • Scroll or use slider to scale • ESC to save";
                graphics.centeredText(this.font, hint, this.width / 2, 10, 0xFFE6EDF3);

                // Back button (top-right)
                int backW = 78, backH = 18;
                int backX = this.width - backW - 10, backY = 8;
                boolean backHover = isInside(mouseX, mouseY, backX, backY, backW, backH);
                graphics.fill(backX, backY, backX + backW, backY + backH, backHover ? 0xFF2D3548 : 0xFF1C202B);
                graphics.outline(backX, backY, backW, backH, backHover ? 0xFF58A6FF : 0xFF3A4256);
                graphics.centeredText(this.font, "Back", backX + backW / 2, backY + 5, 0xFFFFFFFF);

                // Snap alignment guidelines
                if (draggingHud != null) {
                        if (snapGuideX >= 0) {
                                graphics.fill(snapGuideX, 0, snapGuideX + 1, this.height, 0xAA58A6FF);
                        }
                        if (snapGuideY >= 0) {
                                graphics.fill(0, snapGuideY, this.width, snapGuideY + 1, 0xAA58A6FF);
                        }
                }

                // Render all HUD modules
                List<HudModule> huds = ModuleManager.getHudModules();
                for (HudModule hud : huds) {
                        hud.renderHud(graphics, this.font, true);

                        int hx = hud.getClampedX(this.font);
                        int hy = hud.getClampedY(this.font);
                        int hw = hud.getScaledWidth(this.font);
                        int hh = hud.getScaledHeight(this.font);

                        boolean hovered = hud.contains(mouseX, mouseY, this.font);
                        boolean isSelected = hud == selectedHud;
                        int outline = (hud == draggingHud || isSelected) ? 0xFF58A6FF : (hovered ? 0xFFFFFF55 : 0x66FFFFFF);
                        graphics.outline(hx - 1, hy - 1, hw + 2, hh + 2, outline);

                        // Small label tag above hovered or selected HUD
                        if (hovered || isSelected) {
                                String label = hud.getName() + " (" + String.format("%.2fx", hud.getScale()) + ")";
                                int lw = this.font.width(label) + 6;
                                int lx = Math.max(2, Math.min(this.width - lw - 2, hx));
                                int ly = hy > 14 ? hy - 12 : hy + hh + 2;
                                graphics.fill(lx, ly, lx + lw, ly + 10, 0xCC14161D);
                                graphics.text(this.font, label, lx + 3, ly + 1, isSelected ? 0xFF58A6FF : 0xFFE6EDF3, false);
                        }
                }

                // Bottom Scale Slider bar for selected HUD
                if (selectedHud != null) {
                        int barW = 220, barH = 28;
                        int barX = (this.width - barW) / 2;
                        int barY = this.height - barH - 12;
                        graphics.fill(barX, barY, barX + barW, barY + barH, 0xE614161D);
                        graphics.outline(barX, barY, barW, barH, 0xFF58A6FF);

                        String title = selectedHud.getName() + " Scale: " + String.format("%.2fx", selectedHud.getScale());
                        graphics.text(this.font, title, barX + 10, barY + 4, 0xFFFFFFFF, false);

                        int sx = barX + 10, sy = barY + 17, sw = barW - 20, sh = 5;
                        graphics.fill(sx, sy, sx + sw, sy + sh, 0xFF232836);
                        float ratio = (selectedHud.getScale() - 0.5f) / 2.0f;
                        int fillW = Math.round(ratio * sw);
                        graphics.fill(sx, sy, sx + fillW, sy + sh, 0xFF58A6FF);
                        graphics.fill(sx + fillW - 2, sy - 2, sx + fillW + 2, sy + sh + 2, 0xFFFFFFFF);
                }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mx = event.x(), my = event.y();
                if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
                        return super.mouseClicked(event, doubleClick);
                }

                int backX = this.width - 88, backY = 8;
                if (isInside(mx, my, backX, backY, 78, 18)) {
                        this.onClose();
                        return true;
                }

                if (selectedHud != null) {
                        int barW = 220, barH = 28;
                        int barX = (this.width - barW) / 2;
                        int barY = this.height - barH - 12;
                        if (isInside(mx, my, barX + 8, barY + 13, barW - 16, 13)) {
                                draggingScaleSlider = true;
                                updateScaleFromMouse(mx, barX + 10, barW - 20);
                                return true;
                        }
                }

                List<HudModule> huds = ModuleManager.getHudModules();
                for (int i = huds.size() - 1; i >= 0; i--) {
                        HudModule hud = huds.get(i);
                        if (hud.contains(mx, my, this.font)) {
                                selectedHud = hud;
                                draggingHud = hud;
                                grabOffsetX = (int) mx - hud.getClampedX(this.font);
                                grabOffsetY = (int) my - hud.getClampedY(this.font);
                                return true;
                        }
                }

                selectedHud = null;
                return true;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
                if (draggingScaleSlider && selectedHud != null) {
                        int barX = (this.width - 220) / 2;
                        updateScaleFromMouse(event.x(), barX + 10, 200);
                        return true;
                }

                if (draggingHud != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                        int hw = draggingHud.getScaledWidth(this.font);
                        int hh = draggingHud.getScaledHeight(this.font);
                        int rawX = Math.max(0, Math.min(this.width - hw, (int) event.x() - grabOffsetX));
                        int rawY = Math.max(0, Math.min(this.height - hh, (int) event.y() - grabOffsetY));

                        snapGuideX = -1;
                        snapGuideY = -1;

                        // Snap X to center or edges
                        int centerX = this.width / 2 - hw / 2;
                        if (Math.abs(rawX - centerX) <= SNAP_THRESHOLD) {
                                rawX = centerX;
                                snapGuideX = this.width / 2;
                        } else if (rawX <= SNAP_THRESHOLD) {
                                rawX = 4;
                                snapGuideX = 4;
                        } else if (Math.abs((rawX + hw) - this.width) <= SNAP_THRESHOLD) {
                                rawX = this.width - hw - 4;
                                snapGuideX = this.width - 4;
                        }

                        // Snap Y to center or edges
                        int centerY = this.height / 2 - hh / 2;
                        if (Math.abs(rawY - centerY) <= SNAP_THRESHOLD) {
                                rawY = centerY;
                                snapGuideY = this.height / 2;
                        } else if (rawY <= SNAP_THRESHOLD) {
                                rawY = 4;
                                snapGuideY = 4;
                        } else if (Math.abs((rawY + hh) - this.height) <= SNAP_THRESHOLD) {
                                rawY = this.height - hh - 4;
                                snapGuideY = this.height - 4;
                        }

                        draggingHud.setPosition(rawX, rawY);
                        return true;
                }
                return super.mouseDragged(event, dx, dy);
        }

        private void updateScaleFromMouse(double mx, int sx, int sw) {
                if (selectedHud == null) return;
                float ratio = (float) Math.max(0.0, Math.min(1.0, (mx - sx) / sw));
                selectedHud.setScale(0.5f + ratio * 2.0f);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
                if (draggingHud != null || draggingScaleSlider) {
                        draggingHud = null;
                        draggingScaleSlider = false;
                        snapGuideX = -1;
                        snapGuideY = -1;
                        MistConfig.save();
                        return true;
                }
                return super.mouseReleased(event);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
                for (HudModule hud : ModuleManager.getHudModules()) {
                        if (hud.contains(mx, my, this.font)) {
                                selectedHud = hud;
                                hud.setScale(hud.getScale() + (float) (vertical * 0.1));
                                MistConfig.save();
                                return true;
                        }
                }
                return super.mouseScrolled(mx, my, horizontal, vertical);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                        this.onClose();
                        return true;
                }
                return super.keyPressed(event);
        }

        @Override
        public void removed() {
                MistConfig.save();
                super.removed();
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

        private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
                return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
}
