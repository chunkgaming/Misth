package com.mist.client.module.impl.hud;

import com.mist.client.module.HudModule;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.ColorSetting;
import com.mist.client.module.setting.DropdownSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class PotionEffectTimerModule extends HudModule {
        private static final int ICON_SIZE = 16;
        private static final int ROW_HEIGHT = 18;
        private static final int PADDING = 4;

        public final BooleanSetting showBackground = addSetting(new BooleanSetting(
                        "Show Background",
                        "Draw a dark background behind active potion effects.",
                        true
        ));
        public final ColorSetting textColor = addSetting(new ColorSetting(
                        "Text Color",
                        "Text color for effect name and timer.",
                        0xFFFFFFFF
        ));
        public final DropdownSetting alignment = addSetting(new DropdownSetting(
                        "Alignment",
                        "Align potion icons and text to the Left or Right.",
                        "Left",
                        "Left", "Right"
        ));

        public PotionEffectTimerModule() {
                super("Potion Effect Timer", "Displays active status effects with icon, level, and MM:SS countdown.", 10, 40, false);
        }

        private record DisplayEffect(Holder<MobEffect> effect, String label, String timeText) {
        }

        private List<DisplayEffect> collectEffects(boolean isEditorPreview) {
                Minecraft mc = Minecraft.getInstance();
                List<DisplayEffect> list = new ArrayList<>();
                if (mc.player != null) {
                        Collection<MobEffectInstance> active = mc.player.getActiveEffects();
                        for (MobEffectInstance inst : active) {
                                String name = inst.getEffect().value().getDisplayName().getString();
                                String amp = toRoman(inst.getAmplifier() + 1);
                                String label = amp.isEmpty() ? name : name + " " + amp;
                                String time = inst.isInfiniteDuration() ? "**:**" : formatTicks(inst.getDuration());
                                list.add(new DisplayEffect(inst.getEffect(), label, time));
                        }
                }
                if (list.isEmpty() && isEditorPreview) {
                        list.add(new DisplayEffect(MobEffects.SPEED, "Speed II", "02:45"));
                        list.add(new DisplayEffect(MobEffects.STRENGTH, "Strength I", "01:12"));
                }
                return list;
        }

        @Override
        public boolean shouldRenderInGame() {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null && !mc.player.getActiveEffects().isEmpty();
        }

        @Override
        public int getBaseWidth(Font font) {
                List<DisplayEffect> effects = collectEffects(true);
                int maxW = 90;
                for (DisplayEffect e : effects) {
                        int rowW = ICON_SIZE + 6 + font.width(e.label() + " " + e.timeText()) + PADDING * 2;
                        if (rowW > maxW) maxW = rowW;
                }
                return maxW;
        }

        @Override
        public int getBaseHeight(Font font) {
                List<DisplayEffect> effects = collectEffects(true);
                return Math.max(ROW_HEIGHT, effects.size() * ROW_HEIGHT + PADDING * 2);
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                List<DisplayEffect> effects = collectEffects(isEditorPreview);
                if (effects.isEmpty()) return;

                int totalW = getBaseWidth(font);
                int totalH = effects.size() * ROW_HEIGHT + PADDING * 2;
                if (showBackground.isEnabled()) {
                        graphics.fill(0, 0, totalW, totalH, 0x88101218);
                }

                boolean rightAlign = alignment.is("Right");
                int yOffset = PADDING;

                for (DisplayEffect e : effects) {
                        String fullText = e.label() + " " + e.timeText();
                        int textW = font.width(fullText);
                        Identifier sprite = Hud.getMobEffectSprite(e.effect());

                        if (rightAlign) {
                                int iconX = totalW - PADDING - ICON_SIZE;
                                int textX = iconX - 4 - textW;
                                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, iconX, yOffset, ICON_SIZE, ICON_SIZE);
                                graphics.text(font, fullText, textX, yOffset + 4, textColor.getColor(), true);
                        } else {
                                int iconX = PADDING;
                                int textX = iconX + ICON_SIZE + 4;
                                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, iconX, yOffset, ICON_SIZE, ICON_SIZE);
                                graphics.text(font, fullText, textX, yOffset + 4, textColor.getColor(), true);
                        }
                        yOffset += ROW_HEIGHT;
                }
        }

        private static String formatTicks(int ticks) {
                int totalSeconds = Math.max(0, ticks / 20);
                int mins = totalSeconds / 60;
                int secs = totalSeconds % 60;
                return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
        }

        private static String toRoman(int level) {
                return switch (level) {
                        case 1 -> "";
                        case 2 -> "II";
                        case 3 -> "III";
                        case 4 -> "IV";
                        case 5 -> "V";
                        default -> String.valueOf(level);
                };
        }
}
