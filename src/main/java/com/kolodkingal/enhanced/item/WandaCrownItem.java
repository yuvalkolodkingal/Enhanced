package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Wanda's crown. On the Scarlet Witch it transforms her (suit, 40 max health, Diamond-level protection); on anyone
 * else it is just a crown. The bonuses are applied in {@link com.kolodkingal.enhanced.ability.WandaAbilities}.
 */
public class WandaCrownItem extends ArmorItem {
    public WandaCrownItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.wanda_crown.desc").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.wanda_crown.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
