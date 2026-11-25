package com.mist.client.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mist.Mist;
import com.mist.client.CommandBoxEntry;
import com.mist.client.CommandKeysConfig;
import com.mist.client.module.HudModule;
import com.mist.client.module.Module;
import com.mist.client.module.ModuleManager;
import com.mist.client.module.setting.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Unified JSON configuration stored at {@code .minecraft/config/mist.json}.
 * Persists all module enabled states, keybinds, settings, HUD X/Y/Scale coordinates,
 * and Command Keys macro entries.
 */
public final class MistConfig {
        private static final Gson GSON = new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .create();

        private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(Mist.MOD_ID + ".json");
        private static boolean loading = false;

        private MistConfig() {
        }

        public static void load() {
                loading = true;
                try {
                        if (!Files.exists(CONFIG_PATH)) {
                                CommandKeysConfig.boxes = CommandKeysConfig.createDefaultBoxes();
                                save();
                                return;
                        }

                        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                                JsonElement rootEl = JsonParser.parseReader(reader);
                                if (rootEl == null || !rootEl.isJsonObject()) {
                                        CommandKeysConfig.boxes = CommandKeysConfig.createDefaultBoxes();
                                        save();
                                        return;
                                }
                                JsonObject root = rootEl.getAsJsonObject();

                                // Load Command Keys macro boxes
                                if (root.has("commandBoxes") && root.get("commandBoxes").isJsonArray()) {
                                        Type listType = new TypeToken<ArrayList<CommandBoxEntry>>() {}.getType();
                                        List<CommandBoxEntry> loadedBoxes = GSON.fromJson(root.get("commandBoxes"), listType);
                                        CommandKeysConfig.boxes = (loadedBoxes != null && !loadedBoxes.isEmpty())
                                                        ? loadedBoxes
                                                        : CommandKeysConfig.createDefaultBoxes();
                                } else {
                                        CommandKeysConfig.boxes = CommandKeysConfig.createDefaultBoxes();
                                }

                                // Load Modules
                                if (root.has("modules") && root.get("modules").isJsonObject()) {
                                        JsonObject modulesObj = root.getAsJsonObject("modules");
                                        for (Module module : ModuleManager.getModules()) {
                                                if (!modulesObj.has(module.getName()) || !modulesObj.get(module.getName()).isJsonObject()) {
                                                        continue;
                                                }
                                                JsonObject modObj = modulesObj.getAsJsonObject(module.getName());
                                                if (modObj.has("enabled")) {
                                                        module.setEnabledFromConfig(modObj.get("enabled").getAsBoolean());
                                                }
                                                if (modObj.has("keyCode")) {
                                                        module.setKeyCodeFromConfig(modObj.get("keyCode").getAsInt());
                                                }
                                                if (module instanceof HudModule hud && modObj.has("hud") && modObj.get("hud").isJsonObject()) {
                                                        JsonObject hudObj = modObj.getAsJsonObject("hud");
                                                        int x = hudObj.has("x") ? hudObj.get("x").getAsInt() : hud.getX();
                                                        int y = hudObj.has("y") ? hudObj.get("y").getAsInt() : hud.getY();
                                                        float s = hudObj.has("scale") ? hudObj.get("scale").getAsFloat() : hud.getScale();
                                                        hud.setPosition(x, y);
                                                        hud.setScale(s);
                                                }
                                                if (modObj.has("settings") && modObj.get("settings").isJsonObject()) {
                                                        JsonObject settingsObj = modObj.getAsJsonObject("settings");
                                                        for (Setting<?> setting : module.getSettings()) {
                                                                if (!settingsObj.has(setting.getName())) continue;
                                                                JsonElement valEl = settingsObj.get(setting.getName());
                                                                readSettingValue(setting, valEl);
                                                        }
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        Mist.LOGGER.error("Failed to load {}", CONFIG_PATH, e);
                        if (CommandKeysConfig.boxes.isEmpty()) {
                                CommandKeysConfig.boxes = CommandKeysConfig.createDefaultBoxes();
                        }
                } finally {
                        loading = false;
                }
        }

        private static void readSettingValue(Setting<?> setting, JsonElement el) {
                try {
                        if (setting instanceof BooleanSetting bs && el.isJsonPrimitive()) {
                                bs.setValue(el.getAsBoolean());
                        } else if (setting instanceof NumberSetting ns && el.isJsonPrimitive()) {
                                ns.setValue(el.getAsDouble());
                        } else if (setting instanceof ColorSetting cs && el.isJsonPrimitive()) {
                                cs.setValue(el.getAsInt());
                        } else if (setting instanceof DropdownSetting ds && el.isJsonPrimitive()) {
                                ds.setValue(el.getAsString());
                        } else if (setting instanceof StringListSetting sls && el.isJsonArray()) {
                                List<String> list = new ArrayList<>();
                                for (JsonElement item : el.getAsJsonArray()) {
                                        if (item.isJsonPrimitive()) list.add(item.getAsString());
                                }
                                sls.setValue(list);
                        }
                } catch (Exception ignored) {
                }
        }

        public static void save() {
                if (loading) return;
                JsonObject root = new JsonObject();
                JsonObject modulesObj = new JsonObject();

                for (Module module : ModuleManager.getModules()) {
                        JsonObject modObj = new JsonObject();
                        modObj.addProperty("enabled", module.isEnabled());
                        modObj.addProperty("keyCode", module.getKeyCode());

                        if (module instanceof HudModule hud) {
                                JsonObject hudObj = new JsonObject();
                                hudObj.addProperty("x", hud.getX());
                                hudObj.addProperty("y", hud.getY());
                                hudObj.addProperty("scale", hud.getScale());
                                modObj.add("hud", hudObj);
                        }

                        if (!module.getSettings().isEmpty()) {
                                JsonObject settingsObj = new JsonObject();
                                for (Setting<?> setting : module.getSettings()) {
                                        if (setting instanceof BooleanSetting bs) {
                                                settingsObj.addProperty(setting.getName(), bs.isEnabled());
                                        } else if (setting instanceof NumberSetting ns) {
                                                settingsObj.addProperty(setting.getName(), ns.getValue());
                                        } else if (setting instanceof ColorSetting cs) {
                                                settingsObj.addProperty(setting.getName(), cs.getColor());
                                        } else if (setting instanceof DropdownSetting ds) {
                                                settingsObj.addProperty(setting.getName(), ds.getValue());
                                        } else if (setting instanceof StringListSetting sls) {
                                                JsonArray arr = new JsonArray();
                                                for (String s : sls.getValue()) arr.add(s);
                                                settingsObj.add(setting.getName(), arr);
                                        }
                                }
                                modObj.add("settings", settingsObj);
                        }

                        modulesObj.add(module.getName(), modObj);
                }

                root.add("modules", modulesObj);
                root.add("commandBoxes", GSON.toJsonTree(CommandKeysConfig.boxes));

                try {
                        Files.createDirectories(CONFIG_PATH.getParent());
                        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                                GSON.toJson(root, writer);
                        }
                } catch (IOException e) {
                        Mist.LOGGER.error("Failed to save {}", CONFIG_PATH, e);
                }
        }
}
