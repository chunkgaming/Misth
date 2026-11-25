package com.mist.client.module.impl.utility;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Locale;

public class ChatPingingModule extends Module {
        public final StringListSetting triggerWords = addSetting(new StringListSetting(
                        "Custom Trigger Words",
                        "Words that trigger a chat ping (your current IGN is always included automatically).",
                        List.of("admin", "mod", "staff")
        ));
        public final BooleanSetting ignoreSelf = addSetting(new BooleanSetting(
                        "Ignore Self",
                        "Do not ping on messages sent by yourself.",
                        true
        ));
        public final ColorSetting highlightColor = addSetting(new ColorSetting(
                        "Highlight Color",
                        "Color applied to matching incoming chat messages.",
                        0xFFFFAA00
        ));
        public final DropdownSetting notificationSound = addSetting(new DropdownSetting(
                        "Notification Sound",
                        "Client-side sound played when a ping matches.",
                        "Experience Orb",
                        "Experience Orb", "Bell", "Note Pling", "Amethyst Chime", "Level Up"
        ));
        public final NumberSetting volume = addSetting(new NumberSetting(
                        "Volume",
                        "Notification sound volume.",
                        1.0, 0.1, 2.0, 0.1
        ));
        public final NumberSetting pitch = addSetting(new NumberSetting(
                        "Pitch",
                        "Notification sound pitch.",
                        1.2, 0.5, 2.0, 0.05
        ));

        public ChatPingingModule() {
                super("Chat Pinging", "Highlights incoming chat messages mentioning your IGN or trigger words and plays a sound.", Category.UTILITY, false);
        }

        public Component processIncomingMessage(Component message) {
                if (!isEnabled() || message == null) return message;
                Minecraft mc = Minecraft.getInstance();
                String raw = ChatFormatting.stripFormatting(message.getString());
                if (raw == null || raw.isBlank()) return message;

                String lower = raw.toLowerCase(Locale.ROOT);
                String ign = mc.getUser().getName();
                String ignLower = ign.toLowerCase(Locale.ROOT);

                if (ignoreSelf.isEnabled()) {
                        // Common chat formats: "<IGN> msg", "[Rank] IGN: msg", "IGN: msg"
                        if (lower.startsWith("<" + ignLower + ">") || lower.startsWith(ignLower + ":") || lower.contains(" " + ignLower + ":")) {
                                return message;
                        }
                }

                boolean matched = lower.contains(ignLower);
                if (!matched) {
                        for (String word : triggerWords.getValue()) {
                                if (word != null && !word.isBlank() && lower.contains(word.toLowerCase(Locale.ROOT))) {
                                        matched = true;
                                        break;
                                }
                        }
                }

                if (!matched) return message;

                if (mc.player != null) {
                        mc.player.playSound(resolveSound(), volume.asFloat(), pitch.asFloat());
                }

                int rgb = highlightColor.getColor() & 0x00FFFFFF;
                return message.copy().withStyle(Style.EMPTY.withColor(rgb));
        }

        private SoundEvent resolveSound() {
                return switch (notificationSound.getValue()) {
                        case "Bell" -> SoundEvents.BELL_BLOCK;
                        case "Note Pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
                        case "Amethyst Chime" -> SoundEvents.AMETHYST_BLOCK_CHIME;
                        case "Level Up" -> SoundEvents.PLAYER_LEVELUP;
                        default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                };
        }
}
