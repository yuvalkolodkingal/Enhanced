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
 * Magneto's helmet. Protects like a full Netherite set, never breaks, blocks Nausea, and turns whoever first puts
 * it on (without other powers) into Magneto.
 */
public class MagnetoHelmetItem extends ArmorItem {
    public MagnetoHelmetItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.magneto_helmet.desc").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.magneto_helmet.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
