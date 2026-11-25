package com.mist.client.module.setting;

import java.util.List;

public class DropdownSetting extends Setting<String> {
        private final List<String> options;

        public DropdownSetting(String name, String description, String defaultValue, String... options) {
                super(name, description, defaultValue);
                this.options = List.of(options);
        }

        public List<String> getOptions() {
                return options;
        }

        public boolean is(String option) {
                return value != null && value.equalsIgnoreCase(option);
        }

        public void cycle() {
                if (options.isEmpty()) return;
                int idx = options.indexOf(value);
                value = options.get((idx + 1) % options.size());
        }

        @Override
        public void setValue(String newValue) {
                if (newValue != null && options.contains(newValue)) {
                        this.value = newValue;
                }
        }
}
