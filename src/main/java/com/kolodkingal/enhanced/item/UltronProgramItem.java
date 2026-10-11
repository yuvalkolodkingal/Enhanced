package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Ultron Program. It does nothing by itself: Loki's Scepter in the other hand, Sneak and right-click on open ground
 * wakes the Ultron boss (see ScepterRituals). (DESIGN section 16.)
 */
public class UltronProgramItem extends Item {
    public UltronProgramItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.ultron_program.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.ultron_program.desc2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.ultron_program.desc3").withStyle(ChatFormatting.GRAY));
    }
}
