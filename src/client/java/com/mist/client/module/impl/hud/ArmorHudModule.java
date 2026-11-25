package com.mist.client.module.impl.hud;

import com.mist.client.module.HudModule;
import com.mist.client.module.setting.BooleanSetting;
import com.mist.client.module.setting.DropdownSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudModule extends HudModule {
        private static final int SLOT_SIZE = 18;

        public final DropdownSetting orientation = addSetting(new DropdownSetting(
                        "Orientation",
                        "Display armor slots horizontally or vertically.",
                        "Horizontal",
                        "Horizontal", "Vertical"
        ));
        public final DropdownSetting durabilityDisplay = addSetting(new DropdownSetting(
                        "Durability Display",
                        "How item durability is displayed.",
                        "Percentage",
                        "Percentage", "Exact Numbers", "Bar", "Off"
        ));
        public final BooleanSetting showHeldItems = addSetting(new BooleanSetting(
                        "Show Held Items",
                        "Include main-hand and off-hand items alongside armor.",
                        true
        ));

        public ArmorHudModule() {
                super("Armor HUD", "Renders equipped armor and optional held items with durability.", 10, 120, false);
        }

        private List<ItemStack> collectItems(boolean isEditorPreview) {
                Minecraft mc = Minecraft.getInstance();
                List<ItemStack> items = new ArrayList<>();
                if (mc.player != null) {
                        ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
                        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                        ItemStack legs = mc.player.getItemBySlot(EquipmentSlot.LEGS);
                        ItemStack feet = mc.player.getItemBySlot(EquipmentSlot.FEET);
                        if (!head.isEmpty()) items.add(head);
                        if (!chest.isEmpty()) items.add(chest);
                        if (!legs.isEmpty()) items.add(legs);
                        if (!feet.isEmpty()) items.add(feet);

                        if (showHeldItems.isEnabled()) {
                                if (!mc.player.getMainHandItem().isEmpty()) items.add(mc.player.getMainHandItem());
                                if (!mc.player.getOffhandItem().isEmpty()) items.add(mc.player.getOffhandItem());
                        }
                }
                if (items.isEmpty() && isEditorPreview) {
                        items.add(new ItemStack(Items.DIAMOND_HELMET));
                        items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                        items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
                        items.add(new ItemStack(Items.DIAMOND_BOOTS));
                }
                return items;
        }

        @Override
        public boolean shouldRenderInGame() {
                return !collectItems(false).isEmpty();
        }

        @Override
        public int getBaseWidth(Font font) {
                List<ItemStack> items = collectItems(true);
                if (orientation.is("Vertical")) {
                        return durabilityDisplay.is("Off") || durabilityDisplay.is("Bar") ? 20 : 54;
                }
                return Math.max(20, items.size() * 26);
        }

        @Override
        public int getBaseHeight(Font font) {
                List<ItemStack> items = collectItems(true);
                if (orientation.is("Vertical")) {
                        return Math.max(20, items.size() * 20);
                }
                return durabilityDisplay.is("Off") || durabilityDisplay.is("Bar") ? 20 : 28;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, boolean isEditorPreview) {
                List<ItemStack> items = collectItems(isEditorPreview);
                if (items.isEmpty()) return;

                boolean vertical = orientation.is("Vertical");
                String durMode = durabilityDisplay.getValue();

                for (int i = 0; i < items.size(); i++) {
                        ItemStack stack = items.get(i);
                        int ix = vertical ? 2 : i * 26 + 4;
                        int iy = vertical ? i * 20 + 2 : 2;

                        graphics.item(stack, ix, iy);
                        if ("Bar".equals(durMode)) {
                                graphics.itemBar(stack, ix, iy);
                        } else if (!"Off".equals(durMode) && stack.isDamageableItem()) {
                                int max = stack.getMaxDamage();
                                int remaining = Math.max(0, max - stack.getDamageValue());
                                float ratio = max > 0 ? (float) remaining / max : 1.0f;
                                int color = getDurabilityColor(ratio);
                                String label = "Percentage".equals(durMode)
                                                ? Math.round(ratio * 100.0f) + "%"
                                                : String.valueOf(remaining);

                                if (vertical) {
                                        graphics.text(font, label, ix + 18, iy + 4, color, true);
                                } else {
                                        var pose = graphics.pose();
                                        pose.pushMatrix();
                                        pose.translate(ix + 8, iy + 17);
                                        pose.scale(0.75f, 0.75f);
                                        graphics.centeredText(font, label, 0, 0, color);
                                        pose.popMatrix();
                                }
                        }
                }
        }

        private static int getDurabilityColor(float ratio) {
                if (ratio > 0.6f) return 0xFF55FF55;
                if (ratio > 0.25f) return 0xFFFFFF55;
                return 0xFFFF5555;
        }
}
