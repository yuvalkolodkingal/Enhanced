package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Essence of En Sabah Nur, the reward of the Apocalypse fight: hold right-click for three seconds with no power to
 * become Apocalypse. (DESIGN section 17.)
 */
public class EssenceItem extends AwakeningItem {
    public EssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.essence.desc").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.enhanced.essence.desc2").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
