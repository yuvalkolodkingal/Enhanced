package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Gungnir's stand in the vault (3.5): the block is lane L1's ({@code gungnir_stand}, which shows the spear), what it does is
 * here. A player who was rewarded for beating Hela ({@code AsgardProgress.HELA_VICTOR}) takes Gungnir from it, bound to
 * him (its bond is his {@code gungnirBond} + 1, so any older copy of his crumbles), and is marked {@code GUNGNIR_TAKEN}; the
 * stand keeps showing the spear, one for each worthy player. Once taken, the stand gives it again only to its owner who has
 * lost his (a new bond: the lost one crumbles if it is ever found). Everyone else is refused.
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
        UNWORTHY
    }

    /** A player right-clicks the stand at {@code pos} (both sides; the server decides). */
    public static InteractionResult use(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer server) {
            take(server, pos);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /** The server's side of a click on the stand at {@code pos}. */
    public static Taken take(ServerPlayer player, BlockPos pos) {
        AsgardProgress progress = player.getData(ModAttachments.ASGARD);
        if (!progress.has(AsgardProgress.HELA_VICTOR)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.stand_unworthy").withStyle(ChatFormatting.GOLD), true);
            return Taken.UNWORTHY;
        }
        boolean again = progress.has(AsgardProgress.GUNGNIR_TAKEN);
        if (again && OdinAbilities.hasOwnGungnir(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.stand_already"), true);
            return Taken.ALREADY;
        }
        int bond = OdinAbilities.nextBond(player);
        player.setData(ModAttachments.ASGARD, player.getData(ModAttachments.ASGARD).with(AsgardProgress.GUNGNIR_TAKEN));
        ItemStack spear = GungnirItem.bound(player, bond);
        OdinAbilities.give(player, spear);
        Vec3 at = Vec3.atCenterOf(pos).add(0, 1.0, 0);
        player.serverLevel().playSound(null, at.x, at.y, at.z, ModSounds.GUNGNIR_CATCH.get(), SoundSource.PLAYERS, 1.3F, 0.8F);
        OdinAbilities.send(player.serverLevel(), player, OdinAbilities.FX_GUNGNIR_GIVEN, List.of(at, player.position().add(0, 1.0, 0)), 1);
        player.displayClientMessage(Component.translatable(again ? "message.enhanced.odin.stand_returned" : "message.enhanced.odin.stand_taken")
                .withStyle(ChatFormatting.GOLD), false);
        return again ? Taken.RETURNED : Taken.TAKEN;
    }

    private GungnirStand() {
    }
}
