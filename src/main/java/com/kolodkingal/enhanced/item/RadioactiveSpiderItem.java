package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Radioactive Spider. A stub: its bite (seven seconds of sickness, then Spider-Man) is built by the Spider lane.
 * (DESIGN section 14.)
 */
public class RadioactiveSpiderItem extends Item {
    public RadioactiveSpiderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc3").withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
