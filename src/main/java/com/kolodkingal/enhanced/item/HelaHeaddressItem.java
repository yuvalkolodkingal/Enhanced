package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.hela.HelaAbilities;
import com.kolodkingal.enhanced.hela.HelaFight;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Hela's Headdress (3.5): the reward of the fight against Hela, an {@link AwakeningItem} of {@link Power#HELA}. Held (use
 * held down) for three seconds by a player with no power, black smoke and green light gather on him and he becomes Hela,
 * the headdress growing from the cowl. A headdress of the reward remembers whom it was given to ({@link
 * HelaFight#VICTOR_TAG}): when it reaches him (a gift for a winner who was away) it gives him the right to Gungnir too.
 * Epic and fire-proof (the registry's properties); never lost in Asgard (the realm's rules keep it on death).
 */
public class HelaHeaddressItem extends AwakeningItem {
    public HelaHeaddressItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.HELA;
    }

    @Override
    protected void awaken(ServerPlayer player) {
        super.awaken(player);
        HelaAbilities.awakened(player);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            int total = getUseDuration(stack, entity);
            HelaAbilities.holding(player, total - remaining, total);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            HelaAbilities.stopHolding(player);
        }
    }

    /** A headdress of the reward that reaches its winner gives him the right to Gungnir (once; the mark stays). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(HelaFight.VICTOR_TAG)) {
            UUID winner = data.copyTag().getUUID(HelaFight.VICTOR_TAG);
            if (winner.equals(player.getUUID())) {
                HelaFight.makeVictor(player);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.hela_headdress.desc").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
