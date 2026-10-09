package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Helpers for reading and changing a player's power. */
public final class Powers {

    public static PowerData get(Player player) {
        return player.getData(ModAttachments.POWER);
    }

    public static Power power(Player player) {
        return get(player).power();
    }

    public static boolean is(Player player, Power power) {
        return power(player) == power;
    }

    /** Pushes the player's power data to every client that can see them, if anything changed. */
    public static void syncIfDirty(ServerPlayer player) {
        PowerData data = get(player);
        if (data.isDirty()) {
            data.clearDirty();
            player.syncData(ModAttachments.POWER);
        }
    }

    /**
     * Gives (or removes, with {@link Power#NONE}) a power. Cleans up whatever the old power was doing.
     */
    public static void setPower(ServerPlayer player, Power power) {
        PowerData data = get(player);
        Power old = data.power();
        if (old == power) {
            return;
        }
        Telekinesis.release(player, false);
        if (old == Power.SCARLET_WITCH) {
            HexManager.dissolveAllOwnedBy(player.server, player.getUUID());
        }
        data.setPowerRaw(power);
        PowerTicker.applyPassives(player, data);
        syncIfDirty(player);
    }

    /**
     * The dramatic moment a player becomes a hero: title on screen, a burst of energy, a sound everyone nearby
     * hears and a message to the whole server.
     */
    public static void awaken(ServerPlayer player, Power power) {
        setPower(player, power);
        ServerLevel level = player.serverLevel();

        Component title = power.displayName().copy().withStyle(s -> s.withColor(power.color).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("power.enhanced." + power.getSerializedName() + ".awaken").withStyle(ChatFormatting.GRAY)));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), power.color, 120, 1.6);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), power.glow, 60, 0.8);

        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("power.enhanced." + power.getSerializedName() + ".broadcast", player.getDisplayName())
                        .withStyle(s -> s.withColor(power.color)), false);
    }

    /** A sphere of energy particles, sent to everyone nearby. */
    public static void burst(ServerLevel level, double x, double y, double z, int color, int count, double spread) {
        level.sendParticles(new EnergyParticleOptions(color, 1.6F), x, y, z, count, spread, spread, spread, 0.15);
    }

    private Powers() {
    }
}
