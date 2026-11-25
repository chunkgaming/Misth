package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHookRenderer.class)
public class FishingHookRendererMixin {
        @Inject(method = "shouldRender(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
        private void mist$hideFishingBobber(FishingHook hook, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
                if (ModuleManager.NO_FISH_BOBBER == null || !ModuleManager.NO_FISH_BOBBER.isEnabled()) {
                        return;
                }
                if (ModuleManager.NO_FISH_BOBBER.hideMode.is("All Bobbers")) {
                        cir.setReturnValue(false);
                } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (hook.getOwner() != mc.player) {
                                cir.setReturnValue(false);
                        }
                }
        }
}
