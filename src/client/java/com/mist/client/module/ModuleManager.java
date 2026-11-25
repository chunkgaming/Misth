package com.mist.client.module;

import com.mist.client.gui.HudEditorScreen;
import com.mist.client.module.impl.combat.*;
import com.mist.client.module.impl.hud.*;
import com.mist.client.module.impl.render.*;
import com.mist.client.module.impl.utility.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

public final class ModuleManager {
        private static final List<Module> MODULES = new ArrayList<>();
        private static final List<HudModule> HUD_MODULES = new ArrayList<>();
        private static final Map<Class<? extends Module>, Module> BY_CLASS = new HashMap<>();
        private static final Set<Integer> HELD_KEYS = new HashSet<>();

        // Direct fast-access references
        public static CommandKeysModule COMMAND_KEYS;
        public static VanishHudModule VANISH_HUD;
        public static ClearWaterModule CLEAR_WATER;
        public static ClearLavaModule CLEAR_LAVA;
        public static NoNetherFogModule NO_NETHER_FOG;
        public static NoFishBobberModule NO_FISH_BOBBER;
        public static LowShieldModule LOW_SHIELD;
        public static LowFireModule LOW_FIRE;
        public static LowTotemModule LOW_TOTEM;
        public static CustomHitboxesModule CUSTOM_HITBOXES;
        public static TntTimerModule TNT_TIMER;
        public static FreelookModule FREELOOK;
        public static PotionEffectTimerModule POTION_TIMER;
        public static ArmorHudModule ARMOR_HUD;
        public static NumericHealthModule NUMERIC_HEALTH;
        public static CustomCrosshairModule CUSTOM_CROSSHAIR;
        public static ChatPingingModule CHAT_PINGING;

        private ModuleManager() {
        }

        public static void init() {
                MODULES.clear();
                HUD_MODULES.clear();
                BY_CLASS.clear();

                // Refactored Existing Modules
                COMMAND_KEYS = register(new CommandKeysModule());
                VANISH_HUD = register(new VanishHudModule());

                // Visual & Environment Tweaks (Render)
                CLEAR_WATER = register(new ClearWaterModule());
                CLEAR_LAVA = register(new ClearLavaModule());
                NO_NETHER_FOG = register(new NoNetherFogModule());
                NO_FISH_BOBBER = register(new NoFishBobberModule());

                // Viewmodel / First-Person Overlay Tweaks (Render)
                LOW_SHIELD = register(new LowShieldModule());
                LOW_FIRE = register(new LowFireModule());
                LOW_TOTEM = register(new LowTotemModule());

                // Combat & Entity Visuals (Combat / Render)
                CUSTOM_HITBOXES = register(new CustomHitboxesModule());
                TNT_TIMER = register(new TntTimerModule());
                FREELOOK = register(new FreelookModule());

                // HUD Elements (HUD)
                POTION_TIMER = register(new PotionEffectTimerModule());
                ARMOR_HUD = register(new ArmorHudModule());
                NUMERIC_HEALTH = register(new NumericHealthModule());
                CUSTOM_CROSSHAIR = register(new CustomCrosshairModule());

                // Chat Utility (Utility)
                CHAT_PINGING = register(new ChatPingingModule());
        }

        private static <T extends Module> T register(T module) {
                MODULES.add(module);
                BY_CLASS.put(module.getClass(), module);
                if (module instanceof HudModule hud) {
                        HUD_MODULES.add(hud);
                }
                return module;
        }

        public static List<Module> getModules() {
                return Collections.unmodifiableList(MODULES);
        }

        public static List<HudModule> getHudModules() {
                return Collections.unmodifiableList(HUD_MODULES);
        }

        @SuppressWarnings("unchecked")
        public static <T extends Module> T get(Class<T> clazz) {
                return (T) BY_CLASS.get(clazz);
        }

        public static void onTick(Minecraft client) {
                boolean activelyPlaying = client.player != null && client.canInterruptScreen() && client.mouseHandler.isMouseGrabbed();
                if (activelyPlaying) {
                        for (Module module : MODULES) {
                                if (module == FREELOOK) continue; // Freelook handles its own Hold/Toggle keybind
                                int code = module.getKeyCode();
                                if (code <= 0) continue;
                                if (InputConstants.isKeyDown(client.getWindow(), code)) {
                                        if (HELD_KEYS.add(code)) {
                                                module.toggle();
                                        }
                                } else {
                                        HELD_KEYS.remove(code);
                                }
                        }
                } else {
                        HELD_KEYS.clear();
                }

                for (Module module : MODULES) {
                        if (module.isEnabled()) {
                                module.onTick(client);
                        }
                }
        }

        public static void renderHuds(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.options.hideGui) return;
                // HudEditorScreen renders previews itself so they appear crisp above the editor backdrop
                if (mc.gui.screen() instanceof HudEditorScreen) return;

                for (HudModule hud : HUD_MODULES) {
                        hud.renderHud(graphics, mc.font, false);
                }
        }
}
