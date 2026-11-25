package com.mist.client.module.impl.combat;

import com.mist.client.MistClient;
import com.mist.client.module.Category;
import com.mist.client.module.Module;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.DropdownSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FreelookModule extends Module {
        public final BooleanSetting invertPitch = addSetting(new BooleanSetting(
                        "Invert Pitch",
                        "Invert vertical camera movement while freelooking.",
                        false
        ));
        public final DropdownSetting mode = addSetting(new DropdownSetting(
                        "Mode",
                        "Hold keybind or Toggle keybind to activate freelook.",
                        "Hold",
                        "Hold", "Toggle"
        ));

        private boolean active = false;
        private boolean returning = false;
        private float cameraYaw = 0.0f;
        private float cameraPitch = 0.0f;
        private CameraType previousPerspective = CameraType.FIRST_PERSON;
        private boolean wasKeyDown = false;
        private long returnStartMs = 0L;
        private static final long RETURN_DURATION_MS = 140L;

        public FreelookModule() {
                super("Freelook", "Rotate camera 360 degrees around player without changing movement direction.", Category.COMBAT, true);
                setKeyCodeFromConfig(GLFW.GLFW_KEY_LEFT_ALT);
        }

        public boolean isActive() {
                return isEnabled() && active;
        }

        public boolean isControllingCamera() {
                return isEnabled() && (active || returning);
        }

        @Override
        public void onDisable() {
                if (active) {
                        deactivate(false);
                }
                returning = false;
                wasKeyDown = false;
        }

        @Override
        public void onTick(Minecraft client) {
                if (client.player == null || client.gui.screen() != null) {
                        if (active && mode.is("Hold")) {
                                deactivate(true);
                        }
                        wasKeyDown = false;
                        return;
                }

                boolean keyDown = isActivationKeyDown(client);
                if (mode.is("Hold")) {
                        if (keyDown && !active) {
                                activate(client);
                        } else if (!keyDown && active) {
                                deactivate(true);
                        }
                } else {
                        if (keyDown && !wasKeyDown) {
                                if (active) {
                                        deactivate(true);
                                } else {
                                        activate(client);
                                }
                        }
                }
                wasKeyDown = keyDown;
        }

        private boolean isActivationKeyDown(Minecraft client) {
                if (MistClient.freelookKey != null && MistClient.freelookKey.isDown()) {
                        return true;
                }
                int code = getKeyCode();
                return code > 0 && InputConstants.isKeyDown(client.getWindow(), code);
        }

        private void activate(Minecraft client) {
                if (client.player == null) return;
                active = true;
                returning = false;
                cameraYaw = client.player.getYRot();
                cameraPitch = client.player.getXRot();
                previousPerspective = client.options.getCameraType();
                if (previousPerspective == CameraType.FIRST_PERSON) {
                        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
        }

        private void deactivate(boolean smooth) {
                active = false;
                Minecraft client = Minecraft.getInstance();
                if (smooth) {
                        returning = true;
                        returnStartMs = System.currentTimeMillis();
                } else {
                        returning = false;
                }
                if (client.options != null && previousPerspective != null) {
                        client.options.setCameraType(previousPerspective);
                }
        }

        public void applyMouseDelta(double deltaX, double deltaY) {
                cameraYaw += (float) (deltaX * 0.15);
                float pitchDelta = (float) (deltaY * 0.15);
                if (invertPitch.isEnabled()) {
                        pitchDelta = -pitchDelta;
                }
                cameraPitch = Mth.clamp(cameraPitch + pitchDelta, -90.0f, 90.0f);
        }

        public float getRenderYaw(float entityYaw) {
                if (active) return cameraYaw;
                if (returning) {
                        float t = (System.currentTimeMillis() - returnStartMs) / (float) RETURN_DURATION_MS;
                        if (t >= 1.0f) {
                                returning = false;
                                return entityYaw;
                        }
                        return Mth.rotLerp(t, cameraYaw, entityYaw);
                }
                return entityYaw;
        }

        public float getRenderPitch(float entityPitch) {
                if (active) return cameraPitch;
                if (returning) {
                        float t = (System.currentTimeMillis() - returnStartMs) / (float) RETURN_DURATION_MS;
                        if (t >= 1.0f) {
                                returning = false;
                                return entityPitch;
                        }
                        return Mth.lerp(t, cameraPitch, entityPitch);
                }
                return entityPitch;
        }
}
