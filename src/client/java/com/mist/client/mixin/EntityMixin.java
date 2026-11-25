package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
        @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
        private void mist$freelookTurn(double yRot, double xRot, CallbackInfo ci) {
                if ((Object) this instanceof LocalPlayer && ModuleManager.FREELOOK != null && ModuleManager.FREELOOK.isActive()) {
                        ModuleManager.FREELOOK.applyMouseDelta(yRot, xRot);
                        ci.cancel();
                }
        }
}
