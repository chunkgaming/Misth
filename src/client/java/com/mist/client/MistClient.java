package com.mist.client;

import com.mist.Mist;
import com.mist.client.config.MistConfig;
import com.mist.client.gui.ClickGuiScreen;
import com.mist.client.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Unified client entrypoint for MIST:
 * - Initializes ModuleManager & loads unified config/mist.json
 * - Registers ClickGUI keybind (default: Right Shift) and Freelook keybind
 * - Tracks vanish state from server chat messages and resets on join/disconnect
 * - Registers unified HUD renderer
 */
public class MistClient implements ClientModInitializer {
        public static volatile boolean isVanished = false;

        public static KeyMapping openMenuKey;
        public static KeyMapping freelookKey;

        @Override
        public void onInitializeClient() {
                ModuleManager.init();
                MistConfig.load();

                KeyMapping.Category category = KeyMapping.Category.register(Mist.id("main"));
                openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                                "key.mist.open_menu",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_RSHIFT,
                                category
                ));
                freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                                "key.mist.freelook",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_LMENU,
                                category
                ));

                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        boolean activelyPlaying = client.player != null && client.canInterruptScreen() && client.mouseHandler.isMouseGrabbed();
                        while (openMenuKey.consumeClick()) {
                                if (activelyPlaying) {
                                        client.setScreenAndShow(new ClickGuiScreen(null));
                                }
                        }
                        ModuleManager.onTick(client);
                });

                // Preserve existing Vanish HUD server message detection
                ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleServerMessage(message));

                ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> isVanished = false);
                ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> isVanished = false);

                // Unified HUD element rendering
                HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Mist.id("hud"), ModuleManager::renderHuds);
        }

        private static void handleServerMessage(Component message) {
                String raw = ChatFormatting.stripFormatting(message.getString());
                if (raw == null || raw.isEmpty()) {
                        return;
                }
                String text = raw.toLowerCase(Locale.ROOT);
                String username = Minecraft.getInstance().getUser().getName().toLowerCase(Locale.ROOT);

                String vanishEnabled = "vanish for " + username + ": enabled";
                String vanishDisabled = "vanish for " + username + ": disabled";

                if (text.contains(vanishEnabled) || text.contains("you're now spectating the game.")) {
                        isVanished = true;
                } else if (text.contains(vanishDisabled)
                                || text.contains("you're no longer specating the game.")
                                || text.contains("you're no longer spectating the game.")) {
                        isVanished = false;
                }
        }
}
