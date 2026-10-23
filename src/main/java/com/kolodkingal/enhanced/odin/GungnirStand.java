package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.block.asgard.GungnirStandBlockEntity;
import com.kolodkingal.enhanced.config.GungnirConfig;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Gungnir's stand in the vault (3.5): the block is lane L1's ({@code gungnir_stand}, which shows the spear), what it does is
 * here. A player who was rewarded for beating Hela ({@code AsgardProgress.HELA_VICTOR}) takes Gungnir from it, bound to
 * him (its bond is his {@code gungnirBond} + 1, so any older copy of his crumbles), and is marked {@code GUNGNIR_TAKEN}; the
 * stand keeps showing the spear, one for each worthy player. Once taken, the stand gives it again only to its owner who has
 * lost his (a new bond: the lost one crumbles if it is ever found). Everyone else is refused.
 *
 * <p>3.6.1 ({@code [gungnir] onePerWorld}, on by default): only ONE player in the world ever takes Gungnir from the
 * stand. The first who takes it (not in Creative) is the world's owner ({@code AsgardState.gungnirOwner}); after him
 * the stand is empty for everyone else, even other victors over Hela ({@link Taken#CLAIMED}); the owner still takes a
 * new one there if he loses his (the bond rule as before). Creative players are not counted: they may take one, and
 * it makes nobody the owner. The stand's block entity tells the clients whose it is, so it shows the spear only to
 * those who could take it.
 */
public final class GungnirStand {

    /** What the stand did for a player. */
    public enum Taken {
        /** He took Gungnir. */
        TAKEN,
        /** He had lost his, and a new one of his came from the stand. */
        RETURNED,
        /** He already has his. */
        ALREADY,
        /** He did not stand against Hela. */
        UNWORTHY,
        /** 3.6.1: Gungnir is another player's: the stand is empty for him. */
        CLAIMED
    }

    /** A player right-clicks the stand at {@code pos} (both sides; the server decides). */
    public static InteractionResult use(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer server) {
            take(server, pos);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    /** The server's side of a click on the stand at {@code pos}. */
    public static Taken take(ServerPlayer player, BlockPos pos) {
        AsgardProgress progress = ModAttachments.ASGARD.get(player);
        if (!progress.has(AsgardProgress.HELA_VICTOR)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.stand_unworthy").withStyle(ChatFormatting.GOLD), true);
            return Taken.UNWORTHY;
        }
        UUID owner = owner(player.server);
        boolean counted = onePerWorld() && !player.isCreative();
        if (counted && owner != null && !owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.stand_claimed", ownerName(player.server, owner))
                    .withStyle(ChatFormatting.GOLD), true);
            return Taken.CLAIMED;
        }
        boolean again = progress.has(AsgardProgress.GUNGNIR_TAKEN);
        if (again && OdinAbilities.hasOwnGungnir(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.stand_already"), true);
            return Taken.ALREADY;
        }
        if (giveTo(player) && player.serverLevel().getBlockEntity(pos) instanceof GungnirStandBlockEntity stand) {
            stand.ownerChanged();
        }
        Vec3 at = Vec3.atCenterOf(pos).add(0, 1.0, 0);
        player.serverLevel().playSound(null, at.x, at.y, at.z, ModSounds.GUNGNIR_CATCH.get(), SoundSource.PLAYERS, 1.3F, 0.8F);
        OdinAbilities.send(player.serverLevel(), player, OdinAbilities.FX_GUNGNIR_GIVEN, List.of(at, player.position().add(0, 1.0, 0)), 1);
        player.displayClientMessage(Component.translatable(again ? "message.enhanced.odin.stand_returned" : "message.enhanced.odin.stand_taken")
                .withStyle(ChatFormatting.GOLD), false);
        return again ? Taken.RETURNED : Taken.TAKEN;
    }

    /**
     * 3.8: gives the player his own Gungnir, bound to him (its bond his next: any older copy of his crumbles), marks
     * him {@code GUNGNIR_TAKEN}, and records him as the world's bearer when {@code onePerWorld} is on, he is not in
     * Creative and nobody is yet ({@code AsgardState.setGungnirOwner}): what the stand does after its checks, and what
     * Eternity's wish does (lane L8, after its own check of {@link #owner}). The checks are the caller's.
     *
     * @return whether he became the world's bearer just now
     */
    public static boolean giveTo(ServerPlayer player) {
        boolean bearer = onePerWorld() && !player.isCreative() && owner(player.server) == null;
        if (bearer) {
            AsgardState.get(player.server).setGungnirOwner(player.getUUID());
        }
        int bond = OdinAbilities.nextBond(player);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.GUNGNIR_TAKEN));
        ItemStack spear = GungnirItem.bound(player, bond);
        OdinAbilities.give(player, spear);
        return bearer;
    }

    /** The world's Gungnir owner while {@code onePerWorld} is on, or null (none yet, or every victor may take one). */
    @Nullable
    public static UUID owner(MinecraftServer server) {
        return onePerWorld() ? AsgardState.get(server).gungnirOwner() : null;
    }

    /** The owner's name (online, or as the server last knew him). */
    private static String ownerName(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        var cache = server.getProfileCache();
        return cache == null ? "?" : cache.get(owner).map(GameProfile::getName).orElse("?");
    }

    /** {@code [gungnir] onePerWorld} (true where the config is not loaded). */
    public static boolean onePerWorld() {
        return !EnhancedConfig.SPEC.isLoaded() || GungnirConfig.ONE_PER_WORLD.get();
    }

    private GungnirStand() {
    }
}
