package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Gungnir, the spear of the Allfather (3.5): taken from its stand in the vault under the throne room by one who stood
 * against Hela, bound to him ({@code ModDataComponents.OWNER} and {@code BOND}). A melee spear for anyone (8 damage); its
 * owner may open the Bifrost with it (use: the screen; Sneak + use: to where he looks). Never breaks.
 *
 * <p>The 3.5 skeleton writes this minimal class so that the Bifrost's keys are final ({@link #isOwnGungnir}); lane L3 owns
 * and fills it (the reach, the throw, the bond, the model).
 */
public class GungnirItem extends SwordItem {
    public GungnirItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    /** 8 attack damage at a spear's speed. */
    public static ItemAttributeModifiers createAttributes() {
        return SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.8F);
    }

    /** Whether the stack is a Gungnir (anyone's). */
    public static boolean isGungnir(ItemStack stack) {
        return stack.is(ModItems.GUNGNIR.get());
    }

    /** Whether the stack is a Gungnir bound to this player. */
    public static boolean isOwnGungnir(Player player, ItemStack stack) {
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        return isGungnir(stack) && owner != null && owner.is(player);
    }

    /** Use: for its owner, the Bifrost screen (Sneak: the Bifrost to where he looks); for anyone else nothing. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return isOwnGungnir(player, stack) ? Bifrost.useKey(level, player, hand) : InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.gungnir.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
