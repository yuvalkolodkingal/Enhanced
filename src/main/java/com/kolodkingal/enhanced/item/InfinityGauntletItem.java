package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Infinity Gauntlet, the reward of the Thanos fight: hold right-click for three seconds with no power to become
 * Thanos. (DESIGN section 15.)
 */
public class InfinityGauntletItem extends AwakeningItem {
    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc2").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
