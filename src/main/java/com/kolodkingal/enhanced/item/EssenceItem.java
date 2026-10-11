package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Essence of En Sabah Nur, the reward of the Apocalypse fight: hold right-click for three seconds with no power to
 * become Apocalypse (the film's essence transference). Someone who already has a power is turned away at the first click
 * with nothing used up; someone who already is Apocalypse is told so. (DESIGN section 17.)
 */
public class EssenceItem extends AwakeningItem {
    public EssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    /** {@code [apocalypse].essenceRitualTicks}, three seconds unless a server says otherwise. */
    @Override
    public int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.ESSENCE_RITUAL_TICKS.get() : DEFAULT_HOLD_TICKS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.essence.already_powered");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (Powers.is(player, Power.APOCALYPSE)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.enhanced.essence.already_apocalypse"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.essence.desc").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.enhanced.essence.desc2").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
