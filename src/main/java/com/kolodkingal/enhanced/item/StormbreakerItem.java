package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Stormbreaker, forged in Nidavellir in the heart of a dying star, its handle grown from Groot's arm. Unlike Mjolnir it
 * carries no enchantment of worthiness: anyone can wield it, as an axe that hits harder than any other, and anyone who
 * holds it can call the Bifrost (use it; see {@link ThorAbilities#useWeapon}). In Thor's hands it is also thrown, called
 * back, flown with and struck with lightning, like Mjolnir. In the films the axe was a bit stronger than the hammer,
 * and so it is here: a fifth stronger than Mjolnir in everything they share (the blow, the throw, the lightning beam
 * through it). Never breaks; fire and lava can't hurt it.
 */
public class StormbreakerItem extends AxeItem {
    public StormbreakerItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    /** 12 attack damage at axe speed (a Netherite Axe does 10): a fifth more than Mjolnir's 10. */
    public static ItemAttributeModifiers createAttributes() {
        return AxeItem.createAttributes(Tiers.NETHERITE, 7.0F, -3.0F);
    }

    /** Use: the Bifrost screen (Sneak: the Bifrost to where you look). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ThorAbilities.useWeapon(level, player, hand);
    }

    /** A dropped Stormbreaker never despawns (but {@code /give}'s look-alike still vanishes). */
    // TODO(port seam): NeoForge IItemExtension#onEntityItemUpdate; a platform hook is requested (P0 L1 notes).
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && entity.tickCount == 1 && !ThorAbilities.isFakeDrop(entity)) {
            entity.setUnlimitedLifetime();
        }
        return false;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.stormbreaker.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.stormbreaker.stronger").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.thor_weapon.bifrost").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
