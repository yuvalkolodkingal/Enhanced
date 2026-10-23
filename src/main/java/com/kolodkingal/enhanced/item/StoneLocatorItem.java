package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.finders.LocatorState;
import com.kolodkingal.enhanced.finders.StoneLocator;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Stone Locator (3.7, lane L5): a gold-and-black disc with a glass lens over a compass needle, tuned to the
 * Infinity Stone of its recipe; its needle points to the nearest site of a stone its holder has not got ({@code
 * enhanced:locator}, {@link LocatorState}; the reading is {@link StoneLocator}'s). Anyone may hold and use it: the
 * stone's energy was read into the device. <b>Use</b>: a spark trail toward the site; <b>Sneak + use</b>: the next
 * stone to look for (every stone he lacks, then each in turn).
 */
public class StoneLocatorItem extends Item implements ItemExt.ReequipAnimation {
    public StoneLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                StoneLocator.cycle(server, stack);
            } else if (!StoneLocator.pulse(server, stack)) {
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** In a hand (server): read every {@link StoneLocator#READ_TICKS} ticks. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player && (selected || player.getOffhandItem() == stack)) {
            StoneLocator.tick(player, stack);
        }
    }


    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LocatorState state = StoneLocator.state(stack);
        state.tuned().ifPresent(kind -> tooltip.add(Component.translatable("item.enhanced.stone_locator.tuned",
                kind.displayName()).withStyle(s -> s.withColor(kind.color()))));
        tooltip.add(Component.translatable("item.enhanced.stone_locator.desc").withStyle(ChatFormatting.GRAY));
        StoneKind looking = state.filter().orElse(null);
        tooltip.add((looking == null ? Component.translatable("item.enhanced.stone_locator.filter.all")
                : Component.translatable("item.enhanced.stone_locator.filter", looking.displayName()))
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.enhanced.stone_locator.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
