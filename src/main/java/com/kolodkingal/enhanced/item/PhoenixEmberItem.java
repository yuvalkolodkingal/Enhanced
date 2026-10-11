package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Phoenix Ember, a spark of the cosmic fire: hold right-click for three seconds with no power to awaken as Jean
 * Grey. Found in chests (the Phoenix lane adds the loot). (DESIGN section 13.)
 */
public class PhoenixEmberItem extends AwakeningItem {
    public PhoenixEmberItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.PHOENIX;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.phoenix_ember.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.phoenix_ember.desc2").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
