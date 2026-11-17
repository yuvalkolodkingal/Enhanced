package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.magic.Grimoires;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicData;
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
 * Cagliostro's grimoire (3.17): the book of Kamar-Taj's library whose pages Kaecilius tore out, the spells of time. A
 * caster (a witch, a sorcerer, Billy, a hero sorcerer) who reads it learns the Chrono Hold, the Time Loop and the Dark
 * Dimension ({@link Grimoires}). Anyone else sees the letters move on the page. Found in the Sanctum's library.
 */
public class CagliostroGrimoireItem extends Item {
    public CagliostroGrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Grimoires.read(player, MagicData.CAGLIOSTRO, stack.getHoverName())) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable(Magic.isCaster(player)
                    ? "message.enhanced.grimoire.known" : "message.enhanced.grimoire.cannot").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.cagliostros_grimoire.desc").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.cagliostros_grimoire.use").withStyle(ChatFormatting.AQUA));
    }
}
