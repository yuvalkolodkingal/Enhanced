package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.strange.EyeOfAgamotto;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Eye of Agamotto (3.6), the Time Stone's vessel, a gold amulet with two lids over a green gem: a powerless player who
 * holds it ({@code awakenTicks}, as an awakening item, but it is not used up) becomes Doctor Strange and the Eye is his;
 * Doctor Strange and every other sorcerer use the Time Stone in it (Slow Time; Sneak, Rewind); anyone else finds it closed.
 * A safe vessel: anyone may carry it. The rules are {@link EyeOfAgamotto}'s. Its model opens (the predicate {@code
 * enhanced:open}) while it is held for the awakening or its holder's time runs slow.
 */
public class EyeOfAgamottoItem extends Item {
    public EyeOfAgamottoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Powers.power(player).hasPowers()) {
            // The awakening: held as the other awakening items are (the server says no to an Eye already bound).
            if (player instanceof ServerPlayer server && EyeOfAgamotto.use(server, stack) == EyeOfAgamotto.Use.TAKEN) {
                EyeOfAgamotto.use(server, stack, false);
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer server) {
            EyeOfAgamotto.use(server, stack, server.isShiftKeyDown());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Set round a site's Time Stone, the Eye carries who took that stone (3.6, integration: the stones' ledger). */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        StoneLedger.carryTaker(stack, player, s -> s.is(ModItems.TIME_STONE.get()));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EyeOfAgamotto.awakenTicks();
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player) {
            EyeOfAgamotto.holdTick(player, getUseDuration(stack, entity) - remaining);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            EyeOfAgamotto.awaken(player, stack);
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            EyeOfAgamotto.inventoryTick(stack, player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.eye_of_agamotto.desc").withStyle(ChatFormatting.GREEN,
                ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.eye_of_agamotto.awaken").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.eye_of_agamotto.use").withStyle(ChatFormatting.AQUA));
        MjolnirOwner owner = EyeOfAgamotto.owner(stack);
        if (owner != null) {
            tooltip.add(Component.translatable("item.enhanced.eye_of_agamotto.owner", owner.name())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
