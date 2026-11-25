package com.mist.client.module;

import com.mist.client.config.MistConfig;
import com.mist.client.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
        private final String name;
        private final String description;
        private final Category category;
        private boolean enabled;
        private int keyCode = -1;
        private final List<Setting<?>> settings = new ArrayList<>();

        protected Module(String name, String description, Category category, boolean defaultEnabled) {
                this.name = name;
                this.description = description;
                this.category = category;
                this.enabled = defaultEnabled;
        }

        protected <T extends Setting<?>> T addSetting(T setting) {
                this.settings.add(setting);
                return setting;
        }

        public String getName() {
                return name;
        }

        public String getDescription() {
                return description;
        }

        public Category getCategory() {
                return category;
        }

        public boolean isEnabled() {
                return enabled;
        }

        public void setEnabled(boolean enabled) {
                if (this.enabled == enabled) return;
                this.enabled = enabled;
                if (enabled) {
                        onEnable();
                } else {
                        onDisable();
                }
                MistConfig.save();
        }

        /** Sets enabled state silently during config loading without triggering a save loop. */
        public void setEnabledFromConfig(boolean enabled) {
                this.enabled = enabled;
        }

        public void toggle() {
                setEnabled(!this.enabled);
        }

        public int getKeyCode() {
                return keyCode;
        }

        public void setKeyCode(int keyCode) {
                this.keyCode = keyCode;
                MistConfig.save();
        }

        public void setKeyCodeFromConfig(int keyCode) {
                this.keyCode = keyCode;
        }

        public List<Setting<?>> getSettings() {
                return settings;
        }

        public void onEnable() {
        }

        public void onDisable() {
        }

        public void onTick(Minecraft client) {
        }
}
