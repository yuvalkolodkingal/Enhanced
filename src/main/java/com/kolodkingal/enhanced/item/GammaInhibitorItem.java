package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Bruce Banner's Gamma Inhibitor (3.6, P2; Spider-Man: Brand New Day): a wrist band that keeps him human however angry
 * he gets. Anywhere in his inventory, his anger never Hulks him out ({@code HulkRage.inhibited}), and a black band is
 * drawn on his left wrist. He still Hulks out with N when he chooses to. Not craftable: given by a command or the
 * creative tab.
 */
public class GammaInhibitorItem extends Item {
    public GammaInhibitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.gamma_inhibitor.desc").withStyle(ChatFormatting.GRAY));
    }
}
