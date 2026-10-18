package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.block.asgard.BifrostRuneStoneBlockEntity;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * The Bifrost rune stones of the Overworld (3.5): the heart of a Bifrost site ({@code enhanced:bifrost_site}). A player
 * who touches one calls on Heimdall (a line in chat, in his name's colour, for everyone near); if Asgard is open and the
 * stone is ready (it rests {@code asgard.runeStoneCooldown} ticks after each journey, its own time, not the player's),
 * the Bifrost gathers for three seconds and takes him, and everyone inside its circle, to the Observatory. The stone
 * remembers nothing else; where he came from is remembered on his arrival.
 */
public final class RuneStones {
    /** How far the call is heard. */
    public static final double CALL_RANGE = 32.0;

    /** A player uses the stone at {@code pos}. Returns whether the Bifrost began to gather. */
    public static boolean use(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        Component call = Component.translatable("message.enhanced.asgard.rune_call", player.getDisplayName())
                .withStyle(s -> s.withColor(TextColor.fromRgb(player.getTeamColor() == 0xFFFFFF ? 0xFFD86B : player.getTeamColor())));
        for (ServerPlayer near : level.players()) {
            if (near.distanceToSqr(player) <= CALL_RANGE * CALL_RANGE) {
                near.sendSystemMessage(call);
            }
        }
        if (!Bifrost.asgardEnabled()) {
            fizzle(level, pos);
            player.displayClientMessage(Component.translatable("message.enhanced.asgard.rune_fizzle").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof BifrostRuneStoneBlockEntity stone)) {
            return false;
        }
        long now = level.getGameTime();
        if (now < stone.readyAt()) {
            fizzle(level, pos);
            player.displayClientMessage(Component.translatable("message.enhanced.asgard.rune_resting", (stone.readyAt() - now + 19) / 20)
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!Bifrost.open(player, Bifrost.Destination.ASGARD, Bifrost.Source.STONE)) {
            return false;
        }
        stone.setReadyAt(now + cooldown());
        level.playSound(null, pos, ModSounds.RUNE_STONE_CALL.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        return true;
    }

    /** The stone's rest after a journey, in ticks. */
    public static int cooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? AsgardConfig.RUNE_STONE_COOLDOWN.get() : 1200;
    }

    private static void fizzle(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.01);
    }

    private RuneStones() {
    }
}
