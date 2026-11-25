package com.mist.client.module.impl.hud;

import com.mist.client.MistClient;
import com.mist.client.module.HudModule;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.ColorSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class VanishHudModule extends HudModule {
        private static final int PADDING = 2;

        public final BooleanSetting showBackground = addSetting(new BooleanSetting("Show Background", "Draw a background box behind the vanish text.", true));
        public final ColorSetting backgroundColor = addSetting(new ColorSetting("Background Color", "Background box color.", 0x90000000));
        public final ColorSetting textColor = addSetting(new ColorSetting("Text Color", "Text color for Vanish HUD.", 0xFF55FF55));
        public final BooleanSetting textShadow = addSetting(new BooleanSetting("Text Shadow", "Draw drop shadow under the text.", true));

        public VanishHudModule() {
                super("Vanish HUD", "Shows when you are vanished, detected from server chat messages.", 5, 5, true);
        }

        private String getDisplayText(boolean isEditorPreview) {
                if (MistClient.isVanished) {
                        return "Vanish: On";
                }
                return isEditorPreview ? "Vanish: Disabled" : "Vanish: On";
        }

        @Override
        public boolean shouldRenderInGame() {
                return MistClient.isVanished;
        }

        @Override
        public int getBaseWidth(Font font) {
                String text = MistClient.isVanished ? "Vanish: On" : "Vanish: Disabled";
                return font.width(text) + PADDING * 2;
        }

        @Override
        public int getBaseHeight(Font font) {
                return font.lineHeight + PADDING * 2;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                String text = getDisplayText(isEditorPreview);
                int w = font.width(text) + PADDING * 2;
                int h = font.lineHeight + PADDING * 2;

                if (showBackground.isEnabled()) {
                        graphics.fill(0, 0, w, h, backgroundColor.getColor());
                }
                graphics.text(font, text, PADDING, PADDING, textColor.getColor(), textShadow.isEnabled());
        }
}
