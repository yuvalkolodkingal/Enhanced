package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.serum.SerumServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Super Soldier Serum (3.8, PLAN 2.1): a glass vial of glowing blue liquid in a steel cage. Used (either hand), it
 * opens the choice of Captain America, Red Hulk or Falcon ({@link SerumServer#open}); it is used up only by a choice,
 * never by opening, and the server checks the choice again ({@link SerumServer#choose}).
 */
public class SuperSoldierSerumItem extends Item {
    public SuperSoldierSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            SerumServer.open(server, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The Vita-Ray blue liquid glows. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.enhanced.special_soldier_serum.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
