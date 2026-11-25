package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.NumberSetting;

public class LowTotemModule extends Module {
        public final NumberSetting yOffset = addSetting(new NumberSetting(
                        "Y-Offset",
                        "Amount to lower the held Totem of Undying without affecting pop animation.",
                        0.25, 0.0, 0.80, 0.01
        ));

        public LowTotemModule() {
                super("Low Totem", "Lowers the first-person Totem of Undying model when held.", Category.RENDER, false);
        }
}
