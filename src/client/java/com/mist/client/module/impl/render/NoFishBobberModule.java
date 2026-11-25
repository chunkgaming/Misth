package com.mist.client.module.impl.render;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.DropdownSetting;

public class NoFishBobberModule extends Module {
        public final DropdownSetting hideMode = addSetting(new DropdownSetting(
                        "Hide Mode",
                        "Choose whether to hide all fishing bobbers or only other players' bobbers.",
                        "All Bobbers",
                        "All Bobbers", "Others Only"
        ));

        public NoFishBobberModule() {
                super("No Fish Bobber", "Prevents fishing hook bobber entities from rendering in the world.", Category.RENDER, false);
        }
}
