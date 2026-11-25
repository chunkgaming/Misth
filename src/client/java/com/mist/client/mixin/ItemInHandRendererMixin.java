package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
        @Inject(method = "submitArmWithItem", at = @At("HEAD"))
        private void mist$offsetHeldItems(
                        AbstractClientPlayer player,
                        float partialTick,
                        float xRot,
                        InteractionHand hand,
                        float swingProgress,
                        ItemStack stack,
                        float equipProgress,
                        PoseStack poseStack,
                        SubmitNodeCollector collector,
                        int packedLight,
                        CallbackInfo ci
        ) {
                if (stack.is(Items.SHIELD) && ModuleManager.LOW_SHIELD != null && ModuleManager.LOW_SHIELD.isEnabled()) {
                        poseStack.translate(0.0F, -ModuleManager.LOW_SHIELD.yOffset.asFloat(), 0.0F);
                } else if (stack.is(Items.TOTEM_OF_UNDYING) && ModuleManager.LOW_TOTEM != null && ModuleManager.LOW_TOTEM.isEnabled()) {
                        poseStack.translate(0.0F, -ModuleManager.LOW_TOTEM.yOffset.asFloat(), 0.0F);
                }
        }
}
