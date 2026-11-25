package com.mist.client.module.setting;

public class ColorSetting extends Setting<Integer> {
        public ColorSetting(String name, String description, int defaultArgb) {
                super(name, description, defaultArgb);
        }

        public int getColor() {
                return value;
        }

        public int getAlpha() {
                return (value >>> 24) & 0xFF;
        }

        public int getRed() {
                return (value >>> 16) & 0xFF;
        }

        public int getGreen() {
                return (value >>> 8) & 0xFF;
        }

        public int getBlue() {
                return value & 0xFF;
        }

        public void setChannel(int alpha, int red, int green, int blue) {
                int a = Math.max(0, Math.min(255, alpha));
                int r = Math.max(0, Math.min(255, red));
                int g = Math.max(0, Math.min(255, green));
                int b = Math.max(0, Math.min(255, blue));
                this.value = (a << 24) | (r << 16) | (g << 8) | b;
        }
}
