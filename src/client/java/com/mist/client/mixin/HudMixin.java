package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
        @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
        private void mist$renderCustomCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
                if (ModuleManager.CUSTOM_CROSSHAIR != null && ModuleManager.CUSTOM_CROSSHAIR.isEnabled()) {
                        ModuleManager.CUSTOM_CROSSHAIR.renderAtCenter(graphics);
                        ci.cancel();
                }
        }
}
