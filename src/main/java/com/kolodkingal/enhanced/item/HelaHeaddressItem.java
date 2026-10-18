package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Hela's Headdress (3.5): the reward of the fight against Hela; held for three seconds by a player with no power, it makes
 * him Hela. The 3.5 skeleton registers this minimal item (so that the realm's rules can keep it on death and the lanes can
 * name it); lane L4 owns and fills it (the awakening).
 */
public class HelaHeaddressItem extends Item {
    public HelaHeaddressItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.hela_headdress.desc").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }
}
