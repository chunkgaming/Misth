package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;

public class NoNetherFogModule extends Module {
        public NoNetherFogModule() {
                super("No Nether Fog", "Disables thick ambient fog in Nether biomes while preserving render distance.", Category.RENDER, false);
        }
}
