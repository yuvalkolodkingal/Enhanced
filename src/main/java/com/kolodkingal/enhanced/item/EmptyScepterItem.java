package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Loki's Scepter with its gem dark (3.6): what is left when the Mind Stone is crafted out of it (the recipe {@code
 * enhanced:mind_stone}), a plain melee weapon with the Scepter's damage (7) and speed, which confuses nobody and is in
 * no ritual of the Scepter's table. The Mind Stone and the empty Scepter craft Loki's Scepter again.
 */
public class EmptyScepterItem extends Item {
    public EmptyScepterItem(Properties properties) {
        super(properties);
    }

    /** A hit counts as a weapon's (the statistic), and does nothing more. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.empty_scepter.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.empty_scepter.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
