package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Gungnir, the spear of the Allfather (3.5): taken from its stand in the vault under the throne room by one who stood
 * against Hela, bound to him ({@code ModDataComponents.OWNER} and {@code BOND}). A melee spear for anyone (8 damage, a
 * block more reach); its owner, whatever his power, may open the Bifrost with it (use: the screen; Sneak + use: to where he
 * looks); anyone else only wields it. Never breaks; fire and lava cannot hurt it.
 *
 * <p>The bond: each new Gungnir of a player (from the stand, or called to Odin's hand when his own was lost) carries his
 * bond count of that moment ({@code AsgardProgress.gungnirBond}); a copy whose bond is lower than its owner's crumbles the
 * next time it ticks, in an inventory or lying on the ground (only while its owner is on the server, who alone knows his
 * bond). So there is only ever one Gungnir of each owner. A Gungnir with no owner (an operator's {@code /give}) is bound to
 * nobody and never crumbles.
 */
public class GungnirItem extends SwordItem {
    private static final ResourceLocation REACH = Enhanced.id("gungnir_reach");

    public GungnirItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    /** 8 attack damage at a spear's speed, and one block more reach than a hand. */
    public static ItemAttributeModifiers createAttributes() {
        return SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.8F).withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(REACH, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
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

    /** A new Gungnir bound to {@code owner} with the bond {@code bond}. */
    public static ItemStack bound(Player owner, int bond) {
        ItemStack stack = new ItemStack(ModItems.GUNGNIR.get());
        stack.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(owner));
        stack.set(ModDataComponents.BOND.get(), bond);
        return stack;
    }

    /**
     * Whether this copy's bond is broken: it has an owner and a bond, its owner is on the server, and his bond has moved on
     * since it was made. Server only.
     */
    public static boolean stale(ItemStack stack, @Nullable ServerLevel level) {
        if (level == null || !isGungnir(stack)) {
            return false;
        }
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        Integer bond = stack.get(ModDataComponents.BOND.get());
        if (owner == null || bond == null) {
            return false;
        }
        ServerPlayer holder = level.getServer().getPlayerList().getPlayer(owner.id());
        return holder != null && bond < ModAttachments.ASGARD.get(holder).gungnirBond();
    }

    /** Use: for its owner, the Bifrost screen (Sneak: the Bifrost to where he looks); for anyone else nothing. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return isOwnGungnir(player, stack) ? Bifrost.useKey(level, player, hand) : InteractionResultHolder.pass(stack);
    }

    /** A copy whose bond is broken crumbles in the inventory that holds it. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && entity.tickCount % 10 == 0 && stale(stack, server)) {
            crumble(server, entity.getX(), entity.getY() + 1.0, entity.getZ());
            stack.setCount(0);
            if (entity instanceof Player holder) {
                holder.displayClientMessage(Component.translatable("message.enhanced.odin.gungnir_crumbles").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    /** A copy whose bond is broken crumbles where it lies. */
    // TODO(port seam): NeoForge IItemExtension#onEntityItemUpdate; a platform hook is requested (P0 L1 notes).
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel server && entity.tickCount % 10 == 0 && stale(stack, server)) {
            crumble(server, entity.getX(), entity.getY() + 0.2, entity.getZ());
            entity.discard();
            return true;
        }
        return false;
    }

    private static void crumble(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.ASH, x, y, z, 24, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, x, y, z, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.gungnir.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        if (owner != null) {
            tooltip.add(Component.translatable("item.enhanced.mjolnir.bound", owner.name()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.enhanced.gungnir.bifrost").withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
