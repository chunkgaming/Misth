package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
        @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
        private static void mist$cancelWaterOverlay(Minecraft minecraft, PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
                if (ModuleManager.CLEAR_WATER != null && ModuleManager.CLEAR_WATER.isEnabled()) {
                        ci.cancel();
                }
        }

        @Inject(method = "submitFire", at = @At("HEAD"))
        private static void mist$offsetFireOverlay(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
                if (ModuleManager.LOW_FIRE != null && ModuleManager.LOW_FIRE.isEnabled()) {
                        poseStack.translate(0.0F, ModuleManager.LOW_FIRE.fireHeightOffset.asFloat(), 0.0F);
                }
        }
}
