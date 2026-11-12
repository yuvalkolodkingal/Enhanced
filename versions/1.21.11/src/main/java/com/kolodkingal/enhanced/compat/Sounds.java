package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 1.21.11 only: 1.21.1's {@code player.playNotifySound}: a sound only this player hears (gone from 1.21.6). */
public final class Sounds {
    private Sounds() {
    }

    public static void notify(Player player, @Nullable SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (sound == null) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                    sp.getX(), sp.getY(), sp.getZ(), volume, pitch, sp.getRandom().nextLong()));
        } else if (player.level().isClientSide()) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, source, volume, pitch, false);
        }
    }
}
