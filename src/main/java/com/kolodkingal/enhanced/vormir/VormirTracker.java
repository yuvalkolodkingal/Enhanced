package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who pushed whom, for the Soul Stone's sacrifice at the Vormir Cliff (3.6, the plan's 6.1). The shared push helpers
 * ({@code StonePowers.shove} and every power built on it: beams, splashes, rings, columns, the Hulk's pushes) and the
 * telekinetic grip's release call {@link #credit}; a hit (melee, a projectile, a pet's bite, a wind charge's blast) is
 * read from the game's events ({@link VormirEvents}).
 *
 * <p>Three records, all forgotten when the server stops, and a creature's when it leaves (a logout, another level):
 * <ul>
 * <li>the <b>credit</b>: the last push each creature took, and when. A player's push (or his pet's, or his shot's)
 * names him; a creature's push (a zombie's blow, a boss's shove) names no one and so takes the credit away: the last
 * push decides. A creature's own jump or fall is no push and changes nothing;</li>
 * <li>who <b>stands on an edge</b> (on the ground, his feet in a cliff's {@code EDGE_BOX}): the cliffs players are in
 * are looked at every tick;</li>
 * <li>the <b>departure</b>: when one who stood on an edge is no longer on the ground (pushed, thrown, jumped, dropped),
 * the tick, the height and the cliff, with the credit he carried then if it is no older than {@code pushWindowTicks}
 * (what hits him in the air afterwards changes nothing). {@link Sacrifice} reads it when he dies.</li>
 * </ul>
 */
public final class VormirTracker {
    /** How often (ticks) the cliff each player is in is looked up again. */
    static final int SITE_PERIOD = 10;

    /** The last push of a creature: by whom (null: by a creature, which gives no one the credit) and when. */
    public record Credit(@Nullable UUID pusher, long tick) {
    }

    /** A creature left a cliff's edge: where, when, from what height, carrying what credit (null: none). */
    public record Departure(VormirSite site, long tick, double y, @Nullable Credit credit) {
    }

    private static final Map<UUID, Credit> CREDITS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, VormirSite>> ON_EDGE = new HashMap<>();
    private static final Map<UUID, Departure> DEPARTURES = new HashMap<>();
    /** The cliff each player is in (looked up every {@link #SITE_PERIOD} ticks). */
    private static final Map<UUID, VormirSite> PLAYER_SITES = new HashMap<>();

    /**
     * {@code pusher} has just pushed, thrown or dropped {@code victim} (a power that pushes without damage, a
     * telekinetic grip letting go). A pusher that is not a player (nor a player's pet or shot) gives no one the credit;
     * none at all (null) changes nothing.
     */
    public static void credit(LivingEntity victim, @Nullable Entity pusher) {
        if (pusher == null || victim.level().isClientSide || pusher == victim) {
            return;
        }
        UUID who = creditor(pusher);
        if (victim.getUUID().equals(who)) {
            return; // (his own pet, his own shot: no push)
        }
        long now = victim.level().getGameTime();
        Credit credit = new Credit(who, now);
        CREDITS.put(victim.getUUID(), credit);
        // (One who left the edge with no push and is still in the air — lifted by a telekinetic grip, or jumped — and
        // is dropped or knocked now: his fall begins now, and it is this push's.)
        Departure gone = DEPARTURES.get(victim.getUUID());
        if (who != null && gone != null && !victim.onGround()
                && (gone.credit() == null || gone.credit().pusher() == null)
                && now >= gone.tick() && now - gone.tick() <= deathWindowTicks()) {
            double y = Math.max(gone.y(), victim.getY());
            DEPARTURES.put(victim.getUUID(), new Departure(gone.site(), now, y, credit));
        }
    }

    /**
     * The player a push is credited to: the player himself, the owner of a pet or a summon, the owner of a shot (and of
     * a shot's owner's pet); null for a creature of no player.
     */
    @Nullable
    static UUID creditor(Entity pusher) {
        Entity who = pusher;
        for (int depth = 0; depth < 3 && who != null; depth++) {
            if (who instanceof Player player) {
                return player.getUUID();
            }
            if (who instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
                return pet.getOwnerUUID();
            }
            who = who instanceof Projectile shot ? shot.getOwner() : null;
        }
        return null;
    }

    /** The last push the creature took, or null. */
    @Nullable
    public static Credit lastCredit(LivingEntity victim) {
        return CREDITS.get(victim.getUUID());
    }

    /** When and where the creature last left a cliff's edge, or null. */
    @Nullable
    public static Departure departure(LivingEntity victim) {
        return DEPARTURES.get(victim.getUUID());
    }

    /** Whether the creature stood on a cliff's edge at the last look. */
    public static boolean onEdge(LivingEntity entity) {
        Map<UUID, VormirSite> standing = ON_EDGE.get(entity.level().dimension());
        return standing != null && standing.containsKey(entity.getUUID());
    }

    /** The cliff the player was in at the last look, or null. */
    @Nullable
    public static VormirSite siteOf(ServerPlayer player) {
        return PLAYER_SITES.get(player.getUUID());
    }

    // ---- every tick ----

    /** The level's look at its cliffs' edges (after the level's tick). */
    static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % SITE_PERIOD == 0) {
            for (ServerPlayer player : level.players()) {
                VormirSite site = VormirSites.at(level, player.blockPosition());
                if (site != null) {
                    PLAYER_SITES.put(player.getUUID(), site);
                } else {
                    PLAYER_SITES.remove(player.getUUID());
                }
            }
        }
        Set<VormirSite> sites = new LinkedHashSet<>(VormirSites.extra(level));
        for (ServerPlayer player : level.players()) {
            VormirSite site = PLAYER_SITES.get(player.getUUID());
            if (site != null && site.dimension() == level.dimension()) {
                sites.add(site);
            }
        }
        Map<UUID, VormirSite> before = ON_EDGE.getOrDefault(level.dimension(), Map.of());
        Map<UUID, VormirSite> standing = new HashMap<>();
        for (VormirSite site : sites) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, site.edgeSpace(),
                    e -> e.isAlive() && site.standsOnEdge(e))) {
                standing.put(entity.getUUID(), site);
            }
        }
        for (Map.Entry<UUID, VormirSite> was : before.entrySet()) {
            if (standing.containsKey(was.getKey())) {
                continue;
            }
            // (Off the edge: walked away on his feet, or in the air: pushed, thrown, jumped.)
            if (level.getEntity(was.getKey()) instanceof LivingEntity entity && entity.isAlive()
                    && !entity.onGround()) {
                depart(entity, was.getValue(), now);
            }
        }
        if (standing.isEmpty()) {
            ON_EDGE.remove(level.dimension());
        } else {
            ON_EDGE.put(level.dimension(), standing);
        }
        // (One carried off the edge by a telekinetic grip has not begun his fall yet: it begins when he is let go.)
        for (Map.Entry<UUID, Departure> gone : DEPARTURES.entrySet()) {
            Departure d = gone.getValue();
            if (d.site().dimension() == level.dimension() && level.getEntity(gone.getKey()) instanceof LivingEntity held
                    && Telekinesis.isHeld(held)) {
                gone.setValue(new Departure(d.site(), now, Math.max(d.y(), held.getY()), d.credit()));
            }
        }
        if (now % 20 == 0) {
            expire(now);
        }
    }

    /** The creature has left the edge of {@code site} now: what it carries is kept for its death. */
    static void depart(LivingEntity entity, VormirSite site, long now) {
        Credit credit = CREDITS.get(entity.getUUID());
        if (credit != null && (credit.tick() > now || now - credit.tick() > pushWindowTicks())) {
            credit = null;
        }
        DEPARTURES.put(entity.getUUID(), new Departure(site, now, entity.getY(), credit));
    }

    private static void expire(long now) {
        long keepCredits = Math.max(400, pushWindowTicks()) + 20;
        CREDITS.values().removeIf(c -> now - c.tick() > keepCredits || c.tick() > now);
        long keepDepartures = deathWindowTicks() + 20;
        DEPARTURES.values().removeIf(d -> now - d.tick() > keepDepartures || d.tick() > now);
    }

    // ---- forgetting ----

    /** The creature is gone (a logout, another level, its death has been judged): every record of it. */
    public static void forget(UUID id) {
        CREDITS.remove(id);
        DEPARTURES.remove(id);
        PLAYER_SITES.remove(id);
        for (Iterator<Map<UUID, VormirSite>> it = ON_EDGE.values().iterator(); it.hasNext(); ) {
            Map<UUID, VormirSite> standing = it.next();
            standing.remove(id);
            if (standing.isEmpty()) {
                it.remove();
            }
        }
    }

    /** The server stopped: everything. */
    public static void clear() {
        CREDITS.clear();
        DEPARTURES.clear();
        PLAYER_SITES.clear();
        ON_EDGE.clear();
    }

    // ---- the config (read when asked; its defaults where it is not loaded) ----

    static int pushWindowTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? SoulStoneConfig.PUSH_WINDOW_TICKS.get() : 100;
    }

    static int deathWindowTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? SoulStoneConfig.DEATH_WINDOW_TICKS.get() : 200;
    }

    private VormirTracker() {
    }
}
