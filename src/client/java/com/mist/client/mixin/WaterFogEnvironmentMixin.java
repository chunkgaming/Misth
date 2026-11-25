package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterFogEnvironment.class)
public class WaterFogEnvironmentMixin {
        @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
        private void mist$clearWaterFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
                if (ModuleManager.CLEAR_WATER != null && ModuleManager.CLEAR_WATER.isEnabled()) {
                        cir.setReturnValue(false);
                }
        }
}
