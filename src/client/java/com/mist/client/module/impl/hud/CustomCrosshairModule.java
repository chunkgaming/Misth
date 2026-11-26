package com.mist.client.module.impl.hud;

import com.mist.client.module.HudModule;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.ColorSetting;
import com.mist.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

public class CustomCrosshairModule extends HudModule {
        public final NumberSetting length = addSetting(new NumberSetting(
                        "Length",
                        "Length of each crosshair arm in pixels.",
                        4.0, 1.0, 16.0, 1.0
        ));
        public final NumberSetting thickness = addSetting(new NumberSetting(
                        "Thickness",
                        "Thickness of the crosshair lines in pixels.",
                        1.0, 1.0, 4.0, 1.0
        ));
        public final NumberSetting gap = addSetting(new NumberSetting(
                        "Gap",
                        "Center gap distance before each arm starts.",
                        2.0, 0.0, 10.0, 1.0
        ));
        public final NumberSetting opacity = addSetting(new NumberSetting(
                        "Brightness / Opacity",
                        "Crosshair opacity (0.1 to 1.0).",
                        1.0, 0.1, 1.0, 0.05
        ));
        public final BooleanSetting highlightOnPlayerHover = addSetting(new BooleanSetting(
                        "Highlight on Player Hover",
                        "Change crosshair color when aiming directly at another player.",
                        true
        ));
        public final ColorSetting hoverColor = addSetting(new ColorSetting(
                        "Hover Color",
                        "Color used when aiming at a player.",
                        0xFFFF4444
        ));

        public CustomCrosshairModule() {
                super("Custom Crosshair", "Replaces the vanilla crosshair with a customizable '+' crosshair.", 200, 110, false);
        }

        public void renderAtCenter(GuiGraphicsExtractor graphics) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gui.screen() != null) return;
                int cx = graphics.guiWidth() / 2;
                int cy = graphics.guiHeight() / 2;
                boolean aimingAtPlayer = highlightOnPlayerHover.isEnabled() && mc.crosshairPickEntity instanceof Player;
                drawCrosshair(graphics, cx, cy, aimingAtPlayer);
        }

        public void drawCrosshair(GuiGraphicsExtractor graphics, int cx, int cy, boolean highlighted) {
                int len = Math.max(1, length.asInt());
                int thick = Math.max(1, thickness.asInt());
                int g = Math.max(0, gap.asInt());
                int alpha = Math.max(25, Math.min(255, Math.round(opacity.asFloat() * 255.0f)));

                int color;
                if (highlighted) {
                        int base = hoverColor.getColor();
                        int ha = Math.round(((base >>> 24) & 0xFF) * opacity.asFloat());
                        color = (ha << 24) | (base & 0x00FFFFFF);
                } else {
                        color = (alpha << 24) | 0x00FFFFFF;
                }

                int halfT = thick / 2;
                // Left arm
                graphics.fill(cx - g - len, cy - halfT, cx - g, cy - halfT + thick, color);
                // Right arm
                graphics.fill(cx + g + (thick % 2), cy - halfT, cx + g + (thick % 2) + len, cy - halfT + thick, color);
                // Top arm
                graphics.fill(cx - halfT, cy - g - len, cx - halfT + thick, cy - g, color);
                // Bottom arm
                graphics.fill(cx - halfT, cy + g + (thick % 2), cx - halfT + thick, cy + g + (thick % 2) + len, color);
        }

        @Override
        public boolean shouldRenderInGame() {
                // Rendered at screen center via HudMixin
                return false;
        }

        @Override
        public int getBaseWidth(Font font) {
                return (length.asInt() + gap.asInt()) * 2 + thickness.asInt() + 4;
        }

        @Override
        public int getBaseHeight(Font font) {
                return (length.asInt() + gap.asInt()) * 2 + thickness.asInt() + 4;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                if (isEditorPreview) {
                        int w = getBaseWidth(font);
                        int h = getBaseHeight(font);
                        graphics.fill(0, 0, w, h, 0x55101218);
                        drawCrosshair(graphics, w / 2, h / 2, false);
                }
        }
}
