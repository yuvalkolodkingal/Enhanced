package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.heimdall.HofundBond;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Hofund, Heimdall's sword (3.5): the key of the Bifrost. Anyone who holds it can open the Bifrost (use it: the Bifrost
 * screen; Sneak + use: to where he looks), as with Mjolnir and Stormbreaker ({@code asgard.hofundOpensBifrost}); a sword
 * otherwise (9 attack damage, sweeping), and Heimdall deals {@code heimdall.hofundBonus} more with it. Never breaks; fire and
 * lava cannot hurt it.
 *
 * <p>Heimdall gives it to whoever wins the Guardian's Watch, bound to him ({@code ModDataComponents.OWNER} and {@code BOND},
 * see {@link HofundBond}): a powerless owner who holds his own Hofund becomes Heimdall ({@code HeimdallServer}); a copy older
 * than its owner's newest crumbles when it next ticks, in an inventory or on the ground.
 */
public class HofundItem extends SwordItem {
    public HofundItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    /** 9 attack damage at a heavy sword's speed. */
    public static ItemAttributeModifiers createAttributes() {
        return SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.6F);
    }

    /** Whether the stack is a Hofund (anyone's). */
    public static boolean isHofund(ItemStack stack) {
        return stack.is(ModItems.HOFUND.get());
    }

    /** Whether the stack is a Hofund bound to this player. */
    public static boolean isOwnHofund(Player player, ItemStack stack) {
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        return isHofund(stack) && owner != null && owner.is(player);
    }

    /** Use: the Bifrost screen (Sneak: the Bifrost to where you look). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Bifrost.useKey(level, player, hand);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    /** A copy older than its owner's newest crumbles in whatever inventory it is. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level instanceof ServerLevel server && holder.tickCount % 10 == 0 && HofundBond.shouldCrumble(server.getServer(), stack)) {
            HofundBond.dust(server, holder.getX(), holder.getY() + 1.0, holder.getZ());
            stack.setCount(0);
        }
    }

    /** And one lying on the ground. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel server && entity.tickCount % 10 == 0 && HofundBond.shouldCrumble(server.getServer(), stack)) {
            HofundBond.dust(server, entity.getX(), entity.getY() + 0.2, entity.getZ());
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.hofund.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        if (owner != null) {
            tooltip.add(Component.translatable("item.enhanced.hofund.bound", owner.name()).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("item.enhanced.hofund.bifrost").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
