package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
        @ModifyVariable(
                        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
                        at = @At("HEAD"),
                        argsOnly = true
        )
        private Component mist$pingIncomingChat(Component message) {
                if (ModuleManager.CHAT_PINGING != null) {
                        return ModuleManager.CHAT_PINGING.processIncomingMessage(message);
                }
                return message;
        }
}
