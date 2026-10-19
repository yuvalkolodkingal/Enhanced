package com.kolodkingal.enhanced.hydra;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Hydra energy cell (3.6, the plan's 15.7): a glowing blue cell that soldiers drop and Hydra's chests hold. It
 * repairs the Hydra blaster in an anvil ({@link HydraBlasterItem#isValidRepairItem}); nothing else is made of it.
 */
public class HydraEnergyCellItem extends Item {
    public HydraEnergyCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.enhanced.hydra_energy_cell.desc").withStyle(ChatFormatting.GRAY));
    }
}
