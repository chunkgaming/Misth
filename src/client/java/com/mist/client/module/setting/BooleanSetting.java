package com.mist.client.module.setting;

public class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String name, String description, boolean defaultValue) {
                super(name, description, defaultValue);
        }

        public boolean isEnabled() {
                return Boolean.TRUE.equals(value);
        }

        public void toggle() {
                this.value = !isEnabled();
        }
}
