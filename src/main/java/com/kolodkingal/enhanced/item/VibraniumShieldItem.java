package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Captain America's Vibranium Shield (3.8, PLAN 3.3): a shield anyone may block with; for Captain America it blocks
 * everything from the front at once, is never disabled by an axe, bounces projectiles back, and he throws it (R): see
 * {@code captain/ShieldRules} and {@code captain/CaptainEvents}. It never wears out (the Unbreakable component), so it
 * takes no Unbreaking and no repair; it does not burn. Drawn by our own round model ({@code
 * client/captain/ShieldItemRenderer}) in the hand, raised, in the GUI and on the ground; on his back by {@code
 * CapShieldLayer}.
 */
public class VibraniumShieldItem extends ShieldItem {
    public VibraniumShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.enhanced.vibranium_shield.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
