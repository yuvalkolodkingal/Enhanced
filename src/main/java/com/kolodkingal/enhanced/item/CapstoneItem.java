package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Capstone. It does nothing by itself: planted on sand by day (Sneak and right-click) it wakes the Apocalypse boss
 * (the Apocalypse lane builds the ritual). (DESIGN section 17.)
 */
public class CapstoneItem extends Item {
    public CapstoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.capstone.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.capstone.desc2").withStyle(ChatFormatting.YELLOW));
    }
}
