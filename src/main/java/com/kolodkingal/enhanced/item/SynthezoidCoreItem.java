package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A synthetic vibranium body waiting for a mind. Hold it while touching the Mind Stone (use Loki's Scepter) and the
 * stone moves into it: you become Vision.
 */
public class SynthezoidCoreItem extends Item {
    public SynthezoidCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.synthezoid_core.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.synthezoid_core.desc2").withStyle(ChatFormatting.GOLD));
    }
}
