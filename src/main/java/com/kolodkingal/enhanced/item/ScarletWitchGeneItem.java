package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Does nothing on its own. Hold it while touching the Mind Stone (use Loki's Scepter) to awaken as the Scarlet Witch. */
public class ScarletWitchGeneItem extends Item {
    public ScarletWitchGeneItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.scarlet_witch_gene.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.scarlet_witch_gene.desc2").withStyle(ChatFormatting.RED));
    }
}
