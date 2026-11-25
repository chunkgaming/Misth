package com.mist.client;

import com.mist.client.config.MistConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class CommandKeysConfig {
        public static List<CommandBoxEntry> boxes = createDefaultBoxes();

        public static List<CommandBoxEntry> createDefaultBoxes() {
                List<CommandBoxEntry> defaults = new ArrayList<>();
                defaults.add(new CommandBoxEntry("Spawn", "/spawn", -1));
                defaults.add(new CommandBoxEntry("Leave", "/leave", -1));
                return defaults;
        }

        public static void load() {
                MistConfig.load();
        }

        public static void save() {
                MistConfig.save();
        }

        public static void executeCommand(String raw) {
                Minecraft c = Minecraft.getInstance();
                if (c.player == null || c.getConnection() == null || raw == null) return;
                String t = raw.trim();
                if (t.isEmpty()) return;
                if (t.startsWith("/")) c.getConnection().sendCommand(t.substring(1));
                else c.getConnection().sendChat(t);
        }
}
