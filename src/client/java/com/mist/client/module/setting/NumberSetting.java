package com.mist.client.module.setting;

public class NumberSetting extends Setting<Double> {
        private final double min;
        private final double max;
        private final double step;

        public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
                super(name, description, defaultValue);
                this.min = min;
                this.max = max;
                this.step = step;
        }

        public double getMin() {
                return min;
        }

        public double getMax() {
                return max;
        }

        public double getStep() {
                return step;
        }

        public float asFloat() {
                return value.floatValue();
        }

        public int asInt() {
                return (int) Math.round(value);
        }

        @Override
        public void setValue(Double newValue) {
                if (newValue == null) return;
                double clamped = Math.max(min, Math.min(max, newValue));
                if (step > 0) {
                        clamped = Math.round(clamped / step) * step;
                        clamped = Math.max(min, Math.min(max, clamped));
                }
                this.value = clamped;
        }
}
