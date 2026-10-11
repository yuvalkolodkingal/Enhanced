package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * An item that makes whoever holds it a hero by being held: hold right-click for {@link #holdTicks} ticks (the arms
 * drawn as for a bow) and, if nothing interrupts it, the item is used up and the player awakens as {@link #power}.
 * The Phoenix Ember, the Infinity Gauntlet and the Essence of En Sabah Nur are all of this kind, so they feel alike.
 *
 * <p>Someone who already has a power is turned away at the first click, before the hold begins and with nothing used
 * up. Letting go early, swapping slots or dying changes nothing. In Creative the item is kept. (Ultron's Core is not
 * of this kind: the Mind Stone gives Ultron life, as it gave Vision, so it is a ritual of Loki's Scepter.)
 */
public abstract class AwakeningItem extends Item {
    /** How long the three awakening items are held, unless a config says otherwise: three seconds. */
    public static final int DEFAULT_HOLD_TICKS = 60;

    protected AwakeningItem(Properties properties) {
        super(properties);
    }

    /** The hero this item makes. */
    public abstract Power power();

    /** How many ticks right-click is held to awaken. */
    public int holdTicks() {
        return DEFAULT_HOLD_TICKS;
    }

    /** What a hero is told when they try: by default that their body already holds a power. */
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.awakening.already_powered");
    }

    /** Makes {@code player} the hero (a subclass may choose the look, or add its own moment). */
    protected void awaken(ServerPlayer player) {
        Powers.awaken(player, power());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Powers.power(player).hasPowers()) {
            if (!level.isClientSide) {
                player.displayClientMessage(alreadyPowered(), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return holdTicks();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || Powers.power(player).hasPowers()) {
            return stack;
        }
        awaken(player);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return stack;
    }
}
