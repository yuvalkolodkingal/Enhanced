package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * "Whosoever holds this hammer, if he be worthy, shall possess the power of Thor." Who is worthy of Mjolnir: Thor
 * himself; Vision (who lifted it in Age of Ultron); anyone who has proven themselves a hero by saving a village from a
 * raid (the Hero of the Village advancement); and players in Creative or Spectator mode. The server config can make
 * everyone worthy.
 * <p>
 * But each Mjolnir has one owner: the first worthy player to lift it claims it (see {@link #claim}), and from then on
 * only they can lift it, however worthy anyone else is. Creative and Spectator players are the exception, so
 * operators can move things; they never take it over. The server config can switch ownership off.
 */
public final class Worthiness {
    public static final ResourceLocation HERO_OF_THE_VILLAGE = ResourceLocation.withDefaultNamespace("adventure/hero_of_the_village");

    public static boolean isWorthy(Player player) {
        Power power = Powers.power(player);
        if (power == Power.THOR || power == Power.VISION || player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (EnhancedConfig.MJOLNIR_ANYONE_WORTHY.get()) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementHolder hero = serverPlayer.server.getAdvancements().get(HERO_OF_THE_VILLAGE);
            return hero != null && serverPlayer.getAdvancements().getOrStartProgress(hero).isDone();
        }
        // Clients don't know other players' advancements: the server decides, the client only predicts.
        return false;
    }

    // ---- whose Mjolnir it is ----

    /** Who claimed this Mjolnir, or null if nobody has (yet). */
    @Nullable
    public static MjolnirOwner ownerOf(ItemStack mjolnir) {
        return mjolnir.get(ModDataComponents.OWNER.get());
    }

    /**
     * The owner who keeps this player from lifting the Mjolnir, or null if its owner has no say here: it is nobody's
     * yet, or the player's own, or the player is in Creative or Spectator mode, or ownership is switched off.
     */
    @Nullable
    public static MjolnirOwner ownerKeepingFrom(Player player, ItemStack mjolnir) {
        MjolnirOwner owner = ownerOf(mjolnir);
        if (owner == null || owner.is(player) || player.isCreative() || player.isSpectator() || !EnhancedConfig.MJOLNIR_OWNER_ONLY.get()) {
            return null;
        }
        return owner;
    }

    /**
     * Whether the player may lift this Mjolnir (hold it, pick it up, lift it off the ground): they are worthy, and it
     * is nobody else's.
     */
    public static boolean canLift(Player player, ItemStack mjolnir) {
        return isWorthy(player) && ownerKeepingFrom(player, mjolnir) == null;
    }

    /**
     * Whether the Mjolnir is the player's to hold as Thor: theirs, or, with ownership off, anyone's. (An operator in
     * Creative who is moving someone else's hammer isn't worthy of it.)
     */
    public static boolean isTheirs(Player player, ItemStack mjolnir) {
        if (!EnhancedConfig.MJOLNIR_OWNER_ONLY.get()) {
            return true;
        }
        MjolnirOwner owner = ownerOf(mjolnir);
        return owner != null && owner.is(player);
    }

    /**
     * Whether Thor can call this Mjolnir to his hand: he may lift it, and it is not another player's. Unlike
     * {@link #canLift} that leaves out the Creative and Spectator exception: an operator can move anyone's hammer by
     * hand, but his call never takes it from its owner.
     */
    public static boolean canCall(Player thor, ItemStack mjolnir) {
        return canLift(thor, mjolnir) && (ownerOf(mjolnir) == null || isTheirs(thor, mjolnir));
    }

    /**
     * An unclaimed Mjolnir becomes the player's, for good, if they are worthy to lift it. Returns true if it was
     * claimed just now (the stack itself is changed, so call this on the very stack the player ends up with).
     */
    public static boolean claim(Player player, ItemStack mjolnir) {
        if (!mjolnir.is(ModItems.MJOLNIR.get()) || ownerOf(mjolnir) != null || !EnhancedConfig.MJOLNIR_OWNER_ONLY.get()
                || player.isSpectator() || !isWorthy(player)) {
            return false;
        }
        mjolnir.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(player));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("message.enhanced.mjolnir.claimed").withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    private Worthiness() {
    }
}
