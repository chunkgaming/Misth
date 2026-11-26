package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
        @Shadow
        protected abstract void setRotation(float yRot, float xRot);

        @Redirect(
                        method = "alignWithEntity",
                        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V")
        )
        private void mist$overrideCameraRotation(Camera instance, float yRot, float xRot) {
                if (ModuleManager.FREELOOK != null && ModuleManager.FREELOOK.isControllingCamera()) {
                        this.setRotation(
                                        ModuleManager.FREELOOK.getRenderYaw(yRot),
                                        ModuleManager.FREELOOK.getRenderPitch(xRot)
                        );
                } else {
                        this.setRotation(yRot, xRot);
                }
        }
}
