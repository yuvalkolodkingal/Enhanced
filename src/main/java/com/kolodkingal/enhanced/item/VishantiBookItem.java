package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.vishanti.VishantiBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Book of the Vishanti, the old tome of the sorcerers of Kamar-Taj: the spells of Oshtur, Hoggoth and Agamotto. Only a
 * sorcerer (Loki, the Scarlet Witch or Billy) can read it; in their hands it saves places and teleports to them, and
 * raises a ward round a base that no monster can enter, the larger the stronger the sorcerer. Use opens the book, Sneak +
 * use goes to the base (see {@link VishantiBook}). Anyone else only sees runes swim before their eyes.
 */
public class VishantiBookItem extends Item {
    public VishantiBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return VishantiBook.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.book_of_the_vishanti.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.book_of_the_vishanti.sorcerers").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.book_of_the_vishanti.places").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.book_of_the_vishanti.ward").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.book_of_the_vishanti.use").withStyle(ChatFormatting.AQUA));
    }
}
