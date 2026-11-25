package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.NumberSetting;

public class LowShieldModule extends Module {
        public final NumberSetting yOffset = addSetting(new NumberSetting(
                        "Y-Offset",
                        "Amount to lower the first-person shield model.",
                        0.25, 0.0, 0.80, 0.01
        ));

        public LowShieldModule() {
                super("Low Shield", "Lowers the first-person shield model when held and blocking.", Category.RENDER, false);
        }
}
