package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;

public class TntTimerModule extends Module {
        public TntTimerModule() {
                super("TNT Timer", "Renders floating countdown timer above primed TNT entities.", Category.RENDER, false);
        }

        public static int getFuseColor(float seconds) {
                if (seconds > 2.0f) {
                        return 0xFF55FF55; // Green
                } else if (seconds > 1.0f) {
                        return 0xFFFFFF55; // Yellow
                } else {
                        return 0xFFFF5555; // Red
                }
        }
}
