package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The bond of a Hofund with the one Heimdall gave it to (3.5): its {@code OWNER} and its {@code BOND} (his
 * {@link AsgardProgress#hofundBond} when it was made). Each new Hofund of his has a bond one higher; a copy whose bond is
 * lower than its owner's newest crumbles to golden dust the next time it ticks (in an inventory or on the ground), so a
 * lost Hofund is replaced, never duplicated. A Hofund with no owner (a creative copy) has no bond and never crumbles.
 */
public final class HofundBond {

    /** A new Hofund bound to {@code player}: his bond goes up by one, and every older copy of his will crumble. */
    public static ItemStack bind(ServerPlayer player) {
        AsgardProgress progress = player.getData(ModAttachments.ASGARD);
        int bond = progress.hofundBond() + 1;
        player.setData(ModAttachments.ASGARD, progress.withHofundBond(bond));
        ItemStack stack = new ItemStack(ModItems.HOFUND.get());
        stack.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(player));
        stack.set(ModDataComponents.BOND.get(), bond);
        return stack;
    }

    /**
     * Gives {@code player} a new Hofund of his own: into the main hand if it is empty, else anywhere in his inventory, else
     * at his feet. Returns the stack given.
     */
    public static ItemStack give(ServerPlayer player) {
        ItemStack stack = bind(player);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
        return stack;
    }

    /** Whether a Hofund bound to this player is anywhere in his inventory (either hand included). */
    public static boolean hasOwn(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (HofundItem.isOwnHofund(player, stack)) {
                return true;
            }
        }
        return HofundItem.isOwnHofund(player, player.getOffhandItem());
    }

    /** Whether either hand holds a Hofund bound to this player. */
    public static boolean holdsOwn(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (HofundItem.isOwnHofund(player, player.getItemInHand(hand))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this copy is stale: it has an owner whose newest bond ({@code ownerBond}) is above its own. Pure: the
     * owner's bond is the caller's to find.
     */
    public static boolean stale(ItemStack stack, int ownerBond) {
        Integer bond = stack.get(ModDataComponents.BOND.get());
        return stack.has(ModDataComponents.OWNER.get()) && bond != null && bond < ownerBond;
    }

    /** The owner of a bound copy if he is online on this server, or null. */
    @Nullable
    public static ServerPlayer owner(MinecraftServer server, ItemStack stack) {
        MjolnirOwner owner = stack.get(ModDataComponents.OWNER.get());
        return owner == null ? null : server.getPlayerList().getPlayer(owner.id());
    }

    /**
     * Whether a copy should crumble now: its owner is online and has a newer one. (An owner who is away cannot be asked;
     * the copy waits until he is back.)
     */
    public static boolean shouldCrumble(MinecraftServer server, ItemStack stack) {
        ServerPlayer owner = owner(server, stack);
        return owner != null && stale(stack, owner.getData(ModAttachments.ASGARD).hofundBond());
    }

    /** The golden dust a crumbling copy leaves. */
    public static void dust(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 14, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private HofundBond() {
    }
}
