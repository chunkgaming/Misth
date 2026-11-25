package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;

public class ClearLavaModule extends Module {
        public ClearLavaModule() {
                super("Clear Lava", "Removes dense orange fog when submerged in lava.", Category.RENDER, false);
        }
}
