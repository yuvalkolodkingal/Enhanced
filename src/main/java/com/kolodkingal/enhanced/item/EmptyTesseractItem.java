package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Tesseract without its stone (3.6): a hollow, clear cube left when the Space Stone is crafted out of it (the
 * recipe {@code enhanced:space_stone}). Decoration, with no power; the Space Stone and the empty Tesseract craft the
 * Tesseract again.
 */
public class EmptyTesseractItem extends Item {
    public EmptyTesseractItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.empty_tesseract.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.empty_tesseract.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
