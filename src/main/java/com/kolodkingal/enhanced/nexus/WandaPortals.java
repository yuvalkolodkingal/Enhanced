package com.kolodkingal.enhanced.nexus;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.UniverseTravel;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import com.kolodkingal.enhanced.strange.SlingPortals;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * (3.19.2) Ultimate Wanda's red portals, V in her Multiverse menu (its screen: the places she saved, and a row of
 * universes): a pair of portals in her red, the near one before her and the far one wherever she chose, open a while for
 * anyone to cross. They reach any universe (616, 10005, 2149, the series' three What Ifs, a new What If, or the one an
 * item renamed at an anvil names), the Nexus between them ({@link Nexus}, Sneak + R too) and every place she saved
 * (Sneak + V saves where she stands; the places are her Sling-Ring book's). Hers are the only portals a banished player can
 * leave the Nexus by.
 */
public final class WandaPortals {
    /** The red of her portals (SlingPortals recolours the orange to it). */
    public static final int TINT = 0xE8102C;
    /** The universes on her screen, in order; then a new What If, then the Nexus. */
    public static final List<Integer> UNIVERSES = List.of(Multiverse.MAIN, Multiverse.XMEN, Multiverse.ZOMBIES, Multiverse.GAMMA,
            Multiverse.HYDRA, Multiverse.ULTRON);
    public static final int WHAT_IF = UNIVERSES.size();
    public static final int NEXUS = WHAT_IF + 1;
    public static final int BUTTONS = NEXUS + 1;
    /** Between two of her pairs (ticks). */
    public static final int COOLDOWN = 20;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private WandaPortals() {
    }

    /** A button of the row of universes ({@code index}: one of {@link #UNIVERSES}, {@link #WHAT_IF} or {@link #NEXUS}). */
    public static boolean world(ServerPlayer player, int index) {
        if (index == NEXUS) {
            return toNexus(player);
        }
        if (index < 0 || index > WHAT_IF) {
            return false;
        }
        int here = Multiverse.universeAt(player.level(), player.blockPosition());
        int universe;
        if (index == WHAT_IF) {
            String named = player.getMainHandItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                    ? player.getMainHandItem().getHoverName().getString() : "";
            universe = named.isBlank() ? 0 : Multiverse.named(named);
            while (universe == 0 || universe == here) {
                universe = Multiverse.fold(player.getRandom().nextInt(Multiverse.CELLS) + 1L);
            }
        } else {
            universe = UNIVERSES.get(index);
        }
        if (universe == here && !Nexus.in(player)) {
            say(player, Component.translatable("message.enhanced.multiverse.here"));
            return false;
        }
        if (Multiverse.levelOf(player.server, universe) == null) {
            say(player, Component.translatable("message.enhanced.multiverse.no_world"));
            return false;
        }
        SpaceRealms.Arrival arrival = UniverseTravel.arrival(player, universe);
        if (arrival == null) {
            say(player, Component.translatable("message.enhanced.stones.space.no_ground"));
            return false;
        }
        Vec3 far = arrival.spot().add(Vec3.directionFromRotation(0.0F, arrival.yaw()).scale(SlingPortals.NEAR));
        if (!SlingPortals.roomFor(arrival.level(), far)) {
            far = arrival.spot();
        }
        if (open(player, arrival.level(), far, arrival.yaw() + 180.0F, Multiverse.name(universe))) {
            UniverseTravel.remember(player);
            return true;
        }
        return false;
    }

    /** Sneak + R, or the Nexus's button: a pair into the Nexus between the universes. */
    public static boolean toNexus(ServerPlayer player) {
        if (Nexus.in(player)) {
            say(player, Component.translatable("message.enhanced.nexus.already"));
            return false;
        }
        ServerLevel nexus = Nexus.ready(player.server);
        if (nexus == null) {
            say(player, Component.translatable("message.enhanced.nexus.no_world"));
            return false;
        }
        Vec3 spot = Nexus.arrival(player.getRandom());
        return open(player, nexus, spot, player.getYRot() + 180.0F, Component.translatable("dimension.enhanced.nexus"));
    }

    /** A place of her list. */
    public static boolean place(ServerPlayer player, int index, String name) {
        BifrostData.Place place = PlaceBook.placeAt(player, PlaceBook.Kind.SLING_RING, index, name);
        if (place == null || PlaceBook.refusesAsgard(player, PlaceBook.Kind.SLING_RING, place.dimension())) {
            return false;
        }
        if (RoadDimension.in(player) || place.dimension() == RoadDimension.KEY) {
            say(player, Component.translatable("message.enhanced.sling_ring.shut"));
            return false;
        }
        ServerLevel there = player.server.getLevel(place.dimension());
        if (there == null) {
            say(player, Component.translatable("message.enhanced.sling_ring.no_world"));
            return false;
        }
        Vec3 far = SlingPortals.fit(there, player, place.pos());
        return open(player, there, far == null ? place.pos() : far, place.yaw(), Component.literal(place.name()));
    }

    /** Opens her pair: before her, and at {@code far} in {@code there}. */
    static boolean open(ServerPlayer player, ServerLevel there, Vec3 far, float farYaw, Component what) {
        if (!UltimateWanda.is(player) || player.isSpectator()) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now < READY_AT.getOrDefault(player.getUUID(), Long.MIN_VALUE)) {
            return false;
        }
        ServerLevel here = player.serverLevel();
        Vec3 near = SlingPortals.nearFeet(player);
        if (!SlingPortals.roomFor(here, near)) {
            say(player, Component.translatable("message.enhanced.multiverse.no_room"));
            return false;
        }
        READY_AT.put(player.getUUID(), now + COOLDOWN);
        SlingPortals.open(player, here, near, player.getYRot(), there, far, farYaw, TINT);
        Vec3 c = near.add(0, 1.2, 0);
        here.sendParticles(new EnergyParticleOptions(TINT, 1.6F), c.x, c.y, c.z, 60, 0.8, 1.0, 0.8, 0.1);
        here.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.displayClientMessage(Component.translatable("message.enhanced.nexus.portal", what).withStyle(s -> s.withColor(TINT)), true);
        return true;
    }

    private static void say(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static void forget(UUID id) {
        READY_AT.remove(id);
    }

    @Nullable
    static Long readyAt(UUID id) {
        return READY_AT.get(id);
    }
}
