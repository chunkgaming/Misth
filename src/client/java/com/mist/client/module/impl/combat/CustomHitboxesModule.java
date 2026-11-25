package com.mist.client.module.impl.combat;

import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.ColorSetting;
import com.mist.client.module.setting.NumberSetting;

public class CustomHitboxesModule extends Module {
        public final ColorSetting normalColor = addSetting(new ColorSetting(
                        "Normal Color",
                        "RGBA color for entity hitboxes.",
                        0xFFFFFFFF
        ));
        public final ColorSetting hoveredColor = addSetting(new ColorSetting(
                        "Hovered Color",
                        "RGBA color when your crosshair raycast intersects the entity.",
                        0xFFFF4444
        ));
        public final NumberSetting lineWidth = addSetting(new NumberSetting(
                        "Line Width",
                        "Stroke width of rendered hitbox lines.",
                        2.0, 0.5, 6.0, 0.5
        ));
        public final BooleanSetting disableEyeHeightLine = addSetting(new BooleanSetting(
                        "Disable Eye Height Line",
                        "Hides the red eye-level box on entities.",
                        false
        ));
        public final BooleanSetting disableLookVector = addSetting(new BooleanSetting(
                        "Disable Look Vector",
                        "Hides the blue facing-direction line.",
                        false
        ));
        public final BooleanSetting playersOnly = addSetting(new BooleanSetting(
                        "Players Only",
                        "Only render custom hitboxes for player entities.",
                        false
        ));

        public CustomHitboxesModule() {
                super("Custom Hitboxes", "Customizable entity hitboxes that work without F3+B.", Category.COMBAT, false);
        }
}
