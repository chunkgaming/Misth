package com.mist.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public class CommandBoxEntry {
        public String name;
        public String command;
        public int keyCode;

        public transient int cachedKeyCode = Integer.MIN_VALUE;
        public transient String cachedKeyBadge = "";
        public transient String cachedRawKeyName = "None";
        public transient String cachedSourceName = null;
        public transient Component cachedBoldName = Component.empty();

        public CommandBoxEntry() {
                this("New Command", "/say Hello", -1);
        }

        public CommandBoxEntry(String name, String command, int keyCode) {
                this.name = name;
                this.command = command;
                this.keyCode = keyCode;
        }

        public void invalidateCache() {
                this.cachedKeyCode = Integer.MIN_VALUE;
                this.cachedSourceName = null;
        }

        private void ensureKeyCache() {
                if (this.keyCode != this.cachedKeyCode) {
                        this.cachedKeyCode = this.keyCode;
                        if (keyCode <= 0) {
                                this.cachedRawKeyName = "None";
                                this.cachedKeyBadge = "";
                        } else {
                                this.cachedRawKeyName = InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
                                this.cachedKeyBadge = "[" + this.cachedRawKeyName + "]";
                        }
                }
        }

        public String getKeyBadge() {
                ensureKeyCache();
                return this.cachedKeyBadge;
        }
}
