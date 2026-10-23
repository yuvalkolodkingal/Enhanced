package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sound of two of the stones' sites (3.6), heard by whoever is inside one (in its pieces' boxes): the Sanctum's
 * ambience (clocks, far chimes: {@code enhanced:ambient.sanctum}, a 24-s seamless loop) and the End tunnel's hum
 * ({@code enhanced:end_tunnel_hum}, a 16-s loop). The server looks every {@link #CHECK} ticks where each player is and
 * tells that player alone ({@link StoneFxPayload#SITE_AMBIENCE}, arg {@link #NONE}, {@link #SANCTUM_LOOP} or {@link
 * #END_TUNNEL_LOOP}): when he goes in, again every {@link #KEEP_ALIVE} ticks while he stays, and once when he leaves.
 * The client plays the loop as a looping sound for as long as he is inside and fades it out when he leaves (or when
 * no news came for a while). The Vormir Cliff's wind is lane L5's.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SiteAmbience {
    /** How often a player is checked: a second. */
    public static final int CHECK = 20;
    /** How often a player inside is told again (the client stops a loop that hears nothing for longer). */
    public static final int KEEP_ALIVE = 60;
    /** The site ambience args: none (stop), the Sanctum's loop, the End tunnel's. */
    public static final int NONE = 0;
    public static final int SANCTUM_LOOP = 1;
    public static final int END_TUNNEL_LOOP = 2;
    public static final ResourceKey<Structure> SANCTUM = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("sanctum"));
    public static final ResourceKey<Structure> END_TUNNEL = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("end_tunnel"));

    /** What each player was last told (absent: nothing, i.e. {@link #NONE}). */
    private static final Map<UUID, Integer> TOLD = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK != 0) {
            return;
        }
        int site = player.isSpectator() ? NONE : siteAt(player.serverLevel(), player);
        int told = TOLD.getOrDefault(player.getUUID(), NONE);
        if (site != told || site != NONE && player.tickCount % KEEP_ALIVE == 0) {
            tell(player, site);
        }
    }

    /** Tells {@code player} which loop to play (or {@link #NONE}: stop) and remembers it. */
    static void tell(ServerPlayer player, int site) {
        if (site == NONE) {
            TOLD.remove(player.getUUID());
        } else {
            TOLD.put(player.getUUID(), site);
        }
        Net.sendToPlayer(player, new StoneFxPayload(StoneFxPayload.SITE_AMBIENCE, player.getId(),
                List.of(), site));
    }

    /** What {@code player} was last told (for the tests): a site arg. */
    public static int told(ServerPlayer player) {
        return TOLD.getOrDefault(player.getUUID(), NONE);
    }

    /** The site {@code player} stands in: {@link #SANCTUM_LOOP}, {@link #END_TUNNEL_LOOP} or {@link #NONE}. */
    public static int siteAt(ServerLevel level, ServerPlayer player) {
        if (level.dimension() == Level.OVERWORLD && inside(level, player, SANCTUM)) {
            return SANCTUM_LOOP;
        }
        if (level.dimension() == Level.END && inside(level, player, END_TUNNEL)) {
            return END_TUNNEL_LOOP;
        }
        return NONE;
    }

    /** The ambience of the site {@code player} stands in, or null. */
    @Nullable
    public static Holder<SoundEvent> soundAt(ServerLevel level, ServerPlayer player) {
        return sound(siteAt(level, player));
    }

    /** The loop of a site arg, or null. */
    @Nullable
    public static Holder<SoundEvent> sound(int site) {
        return switch (site) {
            case SANCTUM_LOOP -> ModSounds.AMBIENT_SANCTUM;
            case END_TUNNEL_LOOP -> ModSounds.END_TUNNEL_HUM;
            default -> null;
        };
    }

    private static boolean inside(ServerLevel level, ServerPlayer player, ResourceKey<Structure> key) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        return structure != null && level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure)
                .isValid();
    }

    /** A new world for the client (it stops its sounds): tell it afresh. */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        TOLD.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        TOLD.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TOLD.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        TOLD.clear();
    }

    private SiteAmbience() {
    }
}
