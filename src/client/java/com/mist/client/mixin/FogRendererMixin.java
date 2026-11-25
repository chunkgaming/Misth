package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
        @Shadow
        protected abstract FogType getFogType(Camera camera);

        @Inject(method = "setupFog", at = @At("RETURN"))
        private void mist$disableNetherFog(
                        Camera camera,
                        int renderDistanceInChunks,
                        DeltaTracker deltaTracker,
                        float darkenWorldAmount,
                        ClientLevel level,
                        CallbackInfoReturnable<FogData> cir
        ) {
                if (ModuleManager.NO_NETHER_FOG == null || !ModuleManager.NO_NETHER_FOG.isEnabled()) {
                        return;
                }
                if (level != null && level.dimension() == Level.NETHER && getFogType(camera) == FogType.NONE) {
                        FogData data = cir.getReturnValue();
                        if (data != null) {
                                data.environmentalStart = data.renderDistanceStart;
                                data.environmentalEnd = data.renderDistanceEnd;
                        }
                }
        }
}
