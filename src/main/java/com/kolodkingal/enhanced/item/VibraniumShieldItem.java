package com.kolodkingal.enhanced.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Captain America's Vibranium Shield (3.8, PLAN 3.3): a shield anyone may block with; for Captain America it blocks
 * everything from the front at once, is never disabled by an axe, bounces projectiles back, and he throws it (R): see
 * {@code captain/ShieldRules} and {@code captain/CaptainEvents}. It never wears out (the Unbreakable component), so it
 * takes no Unbreaking and no repair; it does not burn. Drawn by our own round model ({@code
 * client/captain/ShieldItemRenderer}) in the hand, raised, in the GUI and on the ground; on his back by {@code
 * CapShieldLayer}.
 */
public class VibraniumShieldItem extends ShieldItem {
    public VibraniumShieldItem(Properties properties) {
        super(properties);
    }

    //#if >=1.21.5
    //$$ /**
    //$$  * What makes it a shield from 1.21.5 on (vanilla's own shield's blocks_attacks): raised with the use key, it blocks
    //$$  * from the front after a quarter of a second, as vanilla's does; Captain America's is up at once and blocks
    //$$  * everything ({@code captain/ShieldRules}). It never wears out, so the item damage never applies.
    //$$  */
    //$$ public static net.minecraft.world.item.component.BlocksAttacks blocking() {
    //$$     return new net.minecraft.world.item.component.BlocksAttacks(0.25F, 1.0F,
    //$$             java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
    //$$             new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
    //$$             java.util.Optional.of(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD),
    //$$             java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
    //$$             java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK));
    //$$ }
    //$$
    //#endif
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.enhanced.vibranium_shield.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
