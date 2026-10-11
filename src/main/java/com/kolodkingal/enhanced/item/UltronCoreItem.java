package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ultron's Core, the reward of the Ultron fight. It does nothing by itself: Loki's Scepter in the other hand makes a
 * player without powers Ultron, as it made Vision (see ScepterRituals). (DESIGN section 16.)
 */
public class UltronCoreItem extends Item {
    public UltronCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.ultron_core.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.ultron_core.desc2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.ultron_core.desc3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
