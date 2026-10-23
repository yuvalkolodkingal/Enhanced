package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Darkhold, the book of the dark magic of Mount Wundagore, which corrupts whoever reads it (Agatha Harkness's). A
 * player with no power who holds use on it becomes Agatha (3.3; the book is kept). Held in either hand, it gives a
 * sorcerer (Loki, the Scarlet Witch, Billy, Agatha; anyone, if {@code darkhold.anyoneCanUse}) Agatha's
 * abilities (see {@link DarkholdBook}): hold use fires her purple beam (the use is held like a bow's, so the holder moves
 * slowly while it burns), Sneak + use is the hex mark or the mind glimpse, and magic used on the holder poisons whoever used it.
 */
public class DarkholdItem extends Item {
    /**
     * The purple beam's look: purple magic with a white core (Agatha's, in WandaVision). Registered here so that the server and
     * every client know it as soon as the item is made.
     */
    public static final BeamStyle STYLE = BeamStyles.register(new BeamStyle("darkhold", "beam.enhanced.darkhold",
            0xFFFFFF, 0xF0D8FF, 0xB45CFF, 0x7A2FD6, 0x3A127A, false));

    public DarkholdItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (DarkholdBook.awakens(player)) {
            // (3.3) Someone with no power reads it, held: Agatha wakes in them (see AgathaAbilities.reading).
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer server) {
            if (DarkholdBook.refuse(server)) {
                return InteractionResultHolder.fail(stack);
            }
            if (server.isShiftKeyDown()) {
                DarkholdBook.markOrGlimpse(server);
                return InteractionResultHolder.success(stack);
            }
            player.startUsingItem(hand);
            DarkholdBook.beamStart(server);
            return InteractionResultHolder.consume(stack);
        }
        if (!DarkholdBook.mayUse(player)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.success(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player) {
            if (!DarkholdBook.firing(player) && DarkholdBook.awakens(player)) {
                AgathaAbilities.reading(player, getUseDuration(stack, player) - remaining);
                return;
            }
            DarkholdBook.beamTick(player);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof ServerPlayer player) {
            DarkholdBook.beamStop(player);
        }
    }

    /** However the use ends (the key came up, the book was put away, a death): the beam ends, and a reading cut short fades. */
    // TODO(port seam): NeoForge IItemExtension#onStopUsing; a platform hook is requested (P0 L1 notes).
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof ServerPlayer player) {
            DarkholdBook.beamStop(player);
            AgathaAbilities.stopReading(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.darkhold.desc").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.darkhold.agatha").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.darkhold.sorcerers").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.darkhold.beam").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.darkhold.retaliation").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.darkhold.marks").withStyle(ChatFormatting.GRAY));
    }
}
