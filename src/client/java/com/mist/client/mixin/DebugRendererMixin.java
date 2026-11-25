package com.mist.client.mixin;

import com.mist.client.module.ModuleManager;
import com.mist.client.module.impl.combat.CustomHitboxesModule;
import com.mist.client.module.impl.render.TntTimerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
        @Inject(method = "emitGizmos", at = @At("TAIL"))
        private void mist$emitWorldGizmos(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null || mc.player == null) return;

                boolean hitboxesOn = ModuleManager.CUSTOM_HITBOXES != null && ModuleManager.CUSTOM_HITBOXES.isEnabled();
                boolean tntTimerOn = ModuleManager.TNT_TIMER != null && ModuleManager.TNT_TIMER.isEnabled();
                if (!hitboxesOn && !tntTimerOn) return;

                Vec3 eyePos = mc.player.getEyePosition(partialTick);
                Vec3 viewVec = mc.player.getViewVector(partialTick);
                Vec3 rayEnd = eyePos.add(viewVec.scale(64.0));

                for (Entity entity : mc.level.entitiesForRendering()) {
                        if (entity.isRemoved()) continue;

                        Vec3 lerpedPos = entity.getPosition(partialTick);
                        Vec3 delta = lerpedPos.subtract(entity.position());
                        AABB box = entity.getBoundingBox().move(delta);

                        if (hitboxesOn) {
                                CustomHitboxesModule cfg = ModuleManager.CUSTOM_HITBOXES;
                                boolean isLocalFirstPerson = entity == mc.player && mc.options.getCameraType().isFirstPerson();
                                if (!isLocalFirstPerson && (!cfg.playersOnly.isEnabled() || entity instanceof Player)) {
                                        if (frustum.isVisible(box)) {
                                                boolean hovered = mc.crosshairPickEntity == entity
                                                                || box.inflate(entity.getPickRadius()).clip(eyePos, rayEnd).isPresent();
                                                int color = hovered ? cfg.hoveredColor.getColor() : cfg.normalColor.getColor();
                                                float width = cfg.lineWidth.asFloat();

                                                Gizmos.cuboid(box, GizmoStyle.stroke(color, width));

                                                if (!cfg.disableEyeHeightLine.isEnabled() && entity instanceof LivingEntity) {
                                                        double eyeY = lerpedPos.y + entity.getEyeHeight();
                                                        AABB eyeBox = new AABB(box.minX, eyeY - 0.01, box.minZ, box.maxX, eyeY + 0.01, box.maxZ);
                                                        Gizmos.cuboid(eyeBox, GizmoStyle.stroke(0xFFFF0000, width));
                                                }

                                                if (!cfg.disableLookVector.isEnabled()) {
                                                        Vec3 entEye = lerpedPos.add(0.0, entity.getEyeHeight(), 0.0);
                                                        Vec3 entLook = entity.getViewVector(partialTick);
                                                        Gizmos.line(entEye, entEye.add(entLook.scale(2.0)), 0xFF0000FF, width);
                                                }
                                        }
                                }
                        }

                        if (tntTimerOn && entity instanceof PrimedTnt tnt) {
                                float remainingTicks = Math.max(0.0f, tnt.getFuse() - partialTick);
                                float seconds = remainingTicks / 20.0f;
                                String label = String.format(Locale.ROOT, "%.2fs", seconds);
                                int color = TntTimerModule.getFuseColor(seconds);
                                Vec3 textPos = lerpedPos.add(0.0, tnt.getBbHeight() + 0.5, 0.0);
                                Gizmos.billboardText(label, textPos, TextGizmo.Style.forColorAndCentered(color));
                        }
                }
        }
}
