package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.NumberSetting;

public class LowFireModule extends Module {
        public final NumberSetting fireHeightOffset = addSetting(new NumberSetting(
                        "Fire Height Offset",
                        "Vertical offset for the first-person burning fire overlay.",
                        -0.35, -0.80, 0.0, 0.01
        ));

        public LowFireModule() {
                super("Low Fire", "Lowers the first-person burning fire overlay so it doesn't obstruct vision.", Category.RENDER, false);
        }
}
