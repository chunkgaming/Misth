package com.mist.client.module.impl.hud;

import com.mist.client.module.HudModule;
import com.mist.client.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

public class NumericHealthModule extends HudModule {
        private static final int PADDING = 3;

        public final BooleanSetting showAbsorption = addSetting(new BooleanSetting(
                        "Show Absorption",
                        "Display golden absorption hearts alongside health (e.g. 18.5 + 4.0).",
                        true
        ));
        public final BooleanSetting colorByHealth = addSetting(new BooleanSetting(
                        "Color by Health %",
                        "Color text green/yellow/red based on current health percentage.",
                        true
        ));

        public NumericHealthModule() {
                super("Numeric Health", "Displays exact numeric health and absorption on the HUD.", 10, 160, false);
        }

        private String buildText() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) {
                        return showAbsorption.isEnabled() ? "20.0 + 4.0" : "20.0";
                }
                float hp = mc.player.getHealth();
                float abs = mc.player.getAbsorptionAmount();
                String hpStr = String.format(Locale.ROOT, "%.1f", hp);
                if (showAbsorption.isEnabled() && abs > 0.0f) {
                        return hpStr + " + " + String.format(Locale.ROOT, "%.1f", abs);
                }
                return hpStr;
        }

        private int getHealthColor() {
                if (!colorByHealth.isEnabled()) return 0xFFFFFFFF;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return 0xFF55FF55;
                float max = Math.max(1.0f, mc.player.getMaxHealth());
                float pct = mc.player.getHealth() / max;
                if (pct > 0.6f) return 0xFF55FF55;
                if (pct > 0.3f) return 0xFFFFFF55;
                return 0xFFFF5555;
        }

        @Override
        public int getBaseWidth(Font font) {
                return font.width(buildText()) + PADDING * 2;
        }

        @Override
        public int getBaseHeight(Font font) {
                return font.lineHeight + PADDING * 2;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                String text = buildText();
                int w = font.width(text) + PADDING * 2;
                int h = font.lineHeight + PADDING * 2;
                graphics.fill(0, 0, w, h, 0x80000000);
                graphics.text(font, text, PADDING, PADDING, getHealthColor(), true);
        }
}
