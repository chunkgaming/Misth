package com.mist.client.module.impl.utility;

import com.mist.client.CommandBoxEntry;
import com.mist.client.CommandKeysConfig;
import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

public class CommandKeysModule extends Module {
        private final Set<Integer> heldKeys = new HashSet<>();

        public CommandKeysModule() {
                super("Command Keys", "Execute custom command/chat macros via keybinds or the macro menu.", Category.UTILITY, true);
        }

        @Override
        public void onDisable() {
                heldKeys.clear();
        }

        @Override
        public void onTick(Minecraft client) {
                boolean activelyPlaying = client.player != null && client.canInterruptScreen() && client.mouseHandler.isMouseGrabbed();
                if (!activelyPlaying) {
                        heldKeys.clear();
                        return;
                }

                Window window = client.getWindow();
                for (CommandBoxEntry box : CommandKeysConfig.boxes) {
                        if (box.keyCode <= 0) continue;
                        if (InputConstants.isKeyDown(window, box.keyCode)) {
                                if (heldKeys.add(box.keyCode)) {
                                        CommandKeysConfig.executeCommand(box.command);
                                }
                        } else {
                                heldKeys.remove(box.keyCode);
                        }
                }
        }
}
