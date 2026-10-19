package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The sound of two of the stones' sites (3.6), heard by whoever is inside one (in its pieces' boxes): the Sanctum's
 * ambience (clocks, far chimes: {@code enhanced:ambient.sanctum}) and the End tunnel's hum ({@code
 * enhanced:end_tunnel_hum}), played to that player alone, again every {@link #PERIOD} ticks (the sounds are made to
 * last about that long). The Vormir Cliff's wind is lane L5's.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SiteAmbience {
    /** How often the ambience is played again (and how often a player is checked): ten seconds. */
    public static final int PERIOD = 200;
    public static final ResourceKey<Structure> SANCTUM = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("sanctum"));
    public static final ResourceKey<Structure> END_TUNNEL = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("end_tunnel"));

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % PERIOD != 0
                || player.isSpectator()) {
            return;
        }
        Holder<SoundEvent> sound = soundAt(player.serverLevel(), player);
        if (sound != null) {
            player.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT, player.getX(),
                    player.getY() + 1.0, player.getZ(), 0.7F, 1.0F, player.getRandom().nextLong()));
        }
    }

    /** The ambience of the site {@code player} stands in, or null. */
    public static Holder<SoundEvent> soundAt(ServerLevel level, ServerPlayer player) {
        if (level.dimension() == Level.OVERWORLD && inside(level, player, SANCTUM)) {
            return ModSounds.AMBIENT_SANCTUM;
        }
        if (level.dimension() == Level.END && inside(level, player, END_TUNNEL)) {
            return ModSounds.END_TUNNEL_HUM;
        }
        return null;
    }

    private static boolean inside(ServerLevel level, ServerPlayer player, ResourceKey<Structure> key) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        return structure != null && level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure)
                .isValid();
    }

    private SiteAmbience() {
    }
}
