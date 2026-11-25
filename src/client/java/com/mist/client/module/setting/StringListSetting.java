package com.mist.client.module.setting;

import java.util.ArrayList;
import java.util.List;

public class StringListSetting extends Setting<List<String>> {
        public StringListSetting(String name, String description, List<String> defaultValue) {
                super(name, description, new ArrayList<>(defaultValue));
        }

        @Override
        public void setValue(List<String> newValue) {
                this.value = newValue != null ? new ArrayList<>(newValue) : new ArrayList<>();
        }

        public void addEntry(String entry) {
                if (entry != null && !entry.isBlank() && !value.contains(entry.trim())) {
                        value.add(entry.trim());
                }
        }

        public void removeEntry(int index) {
                if (index >= 0 && index < value.size()) {
                        value.remove(index);
                }
        }
}
