package com.mist.client.module;

import com.mist.client.config.MistConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudModule extends Module {
        protected int x;
        protected int y;
        protected float scale = 1.0f;

        protected HudModule(String name, String description, int defaultX, int defaultY, boolean defaultEnabled) {
                super(name, description, Category.HUD, defaultEnabled);
                this.x = defaultX;
                this.y = defaultY;
        }

        public int getX() {
                return x;
        }

        public int getY() {
                return y;
        }

        public float getScale() {
                return scale;
        }

        public void setPosition(int x, int y) {
                this.x = x;
                this.y = y;
        }

        public void setScale(float scale) {
                this.scale = Math.max(0.5f, Math.min(2.5f, Math.round(scale * 20.0f) / 20.0f));
        }

        public void saveLayout() {
                MistConfig.save();
        }

        /** Base unscaled width in GUI pixels. */
        public abstract int getBaseWidth(Font font);

        /** Base unscaled height in GUI pixels. */
        public abstract int getBaseHeight(Font font);

        public int getScaledWidth(Font font) {
                return Math.max(8, Math.round(getBaseWidth(font) * scale));
        }

        public int getScaledHeight(Font font) {
                return Math.max(8, Math.round(getBaseHeight(font) * scale));
        }

        public int getClampedX(Font font) {
                int guiW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                return Math.max(0, Math.min(x, guiW - getScaledWidth(font)));
        }

        public int getClampedY(Font font) {
                int guiH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                return Math.max(0, Math.min(y, guiH - getScaledHeight(font)));
        }

        public boolean contains(double px, double py, Font font) {
                int cx = getClampedX(font);
                int cy = getClampedY(font);
                int w = getScaledWidth(font);
                int h = getScaledHeight(font);
                return px >= cx && px < cx + w && py >= cy && py < cy + h;
        }

        public final void renderHud(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                if (!isEditorPreview && !isEnabled()) return;
                if (!isEditorPreview && !shouldRenderInGame()) return;

                int cx = getClampedX(font);
                int cy = getClampedY(font);
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(cx, cy);
                pose.scale(scale, scale);
                renderContent(graphics, font, isEditorPreview);
                pose.popMatrix();
        }

        public boolean shouldRenderInGame() {
                return true;
        }

        /** Renders the HUD element at (0, 0) in local scaled space. */
        protected abstract void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview);
}
