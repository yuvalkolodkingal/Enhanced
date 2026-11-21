package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.mixin.AbstractArrowAccessor;
import com.kolodkingal.enhanced.network.SlowedProjectilePayload;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.stones.StoneZones;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Quicksilver Time (and, since 3.3, Tommy's slow time): while it is on, everything near the speedster runs several times
 * slower, whether he runs or not. (Super Speed slows nothing: only {@link PowerData#slowMo()}, which just the slow time
 * of a speedster sets, counts here.)
 *
 * <ul>
 *     <li>Creatures and every other entity (items, falling blocks, TNT, boats, fireballs, wind charges, shulker bullets,
 *     the mod's own missiles, orbs, shields and so on) only get every Nth tick, on both sides; clients draw each real
 *     tick spread over the N ticks ({@link #renderPartialTick}), so they glide and animate at 1/N instead of
 *     stuttering. Their speed is untouched, so they hit as hard as ever and fly on at full pace once out of the zone.</li>
 *     <li>Arrows and thrown projectiles keep ticking but fly at 1/N speed, so they visibly hang in the air. Their air drag
 *     is spread over the slowed time, and they get their full speed back the moment they hit something, so they hit
 *     exactly as hard as they would have.</li>
 *     <li>Shots that fly as points with no entity (Jim Hammond's fireballs, the spellbooks' shots) ask
 *     {@link #skipsPoint}.</li>
 *     <li>Other players can't be slowed tick by tick (their game moves them), so they get Slowness worth about 1/N of
 *     their walking speed ({@link #playerSlowness}).</li>
 * </ul>
 * The zone holder's own things are slowed like anyone's (what he throws hangs in his slowed time); he himself is not.
 * Every way of slowing time is a zone here: Quicksilver Time, Tommy's slow time, a custom hero's with either, the Time
 * Stone's Slow Time (the Infinity Gauntlet, the stone bearers, the Eye of Agamotto, Doctor Strange's Y), all at a tenth
 * by default (3.18.0). Works the same on both sides so clients and server agree on what is slowed.
 */
public final class SlowMotion {
    private static final String SLOWED_TAG = "enhanced:slowed";

    /**
     * The zones of one tick: their centres, whose they are, and (3.6) their radii (a speedster's, or a Time Stone's).
     */
    private record Zones(long gameTime, List<Vec3> centers, List<Player> speedsters, List<Double> radii) {
    }

    private static final Map<Level, Zones> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    /** Projectiles currently flying at reduced speed (server only). */
    private static final Set<Projectile> SLOWED = Collections.newSetFromMap(new WeakHashMap<>());

    public static int factor() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.SLOW_MOTION_FACTOR.get() : 10;
    }

    public static double radius() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.SLOW_MOTION_RADIUS.get() : 32;
    }

    /**
     * Positions of every Quicksilver in this level whose Quicksilver Time is running (cached per tick); 3.6: and of
     * every player whose Time Stone slows time round him ({@link StoneZones}), with its own radius.
     */
    public static List<Vec3> zones(Level level) {
        return zonesOf(level).centers;
    }

    private static Zones zonesOf(Level level) {
        long time = level.getGameTime();
        Zones cached = CACHE.get(level);
        if (cached != null && cached.gameTime == time) {
            return cached;
        }
        List<Vec3> centers = new ArrayList<>(1);
        List<Player> speedsters = new ArrayList<>(1);
        List<Double> radii = new ArrayList<>(1);
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            if (!player.isAlive()) {
                continue;
            }
            if (slowsTime(data)) {
                centers.add(player.position());
                speedsters.add(player);
                radii.add(radius());
            } else if (StoneZones.slows(data)) {
                // 3.6: the Time Stone's zone (lane L2 sets the bit; the seam is here).
                centers.add(player.position());
                speedsters.add(player);
                radii.add(StoneZones.radius());
            } else {
                double custom = customRadius(player);
                if (custom > 0) {
                    // 3.18.0: a custom hero's Time Manipulation, "slow": a zone as a speedster's, with its own radius
                    centers.add(player.position());
                    speedsters.add(player);
                    radii.add(custom);
                }
            }
        }
        Zones zones = new Zones(time, centers, speedsters, radii);
        CACHE.put(level, zones);
        return zones;
    }

    /** 3.18.0: whether any of this level's zones is a speedster's (not only Time Stones'). */
    public static boolean speedsterZoneIn(Level level) {
        for (Player player : zonesOf(level).speedsters()) {
            if (slowsTime(Powers.get(player))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 3.18.0: the radius of a custom hero's slowed time (Time Manipulation picked as "slow" and switched on), or 0. Both
     * sides read the same: the hero's definition and his switches are synced.
     */
    public static double customRadius(Player player) {
        CustomHero.Pick pick = CustomHeroes.pick(player, PowerModule.TIME);
        if (pick == null || !CustomHeroes.state(player).isOn(PowerModule.TIME)
                || !CustomHeroes.has(player, PowerModule.TIME, "slow")) {
            return 0;
        }
        return ModuleStats.SLOW_RADIUS.value(Math.max(1, Math.min(PowerModule.MAX_STRENGTH, pick.strength())));
    }

    /**
     * 3.6: whether this player's own time runs on while the world round him is slowed (a speedster, a Time Stone; 3.18.0:
     * a custom hero's Time Manipulation).
     */
    private static boolean holdsZone(Player player, PowerData data) {
        return slowsTime(data) || StoneZones.slows(data) || customRadius(player) > 0;
    }

    /**
     * Whether this hero's slow time is running: Quicksilver Time, or (3.3) Tommy's slow time; 3.4: or a custom hero's with
     * one of the two.
     */
    public static boolean slowsTime(PowerData data) {
        return slowTimeOf(data) != Power.NONE && data.slowMo();
    }

    /** Whose slow time this player has: Quicksilver's, Tommy's (3.4: a custom hero's with it too), or {@link Power#NONE}. */
    public static Power slowTimeOf(PowerData data) {
        return data.has(HeroAbility.QUICKSILVER_TIME) ? Power.QUICKSILVER : data.has(HeroAbility.TOMMY_TIME) ? Power.TOMMY : Power.NONE;
    }

    /**
     * Whose slow time holds this point: the power of the nearest speedster whose zone it is in (the screen shade and the
     * world's tint take his colours), or {@link Power#NONE}.
     */
    /**
     * 3.5: the player whose slowed time holds {@code pos} (the nearest, as {@link #zoneOwner}), or null: the overlay
     * takes the colour his pick chose.
     */
    @Nullable
    public static Player zoneHolder(Level level, Vec3 pos) {
        double r2 = radius() * radius();
        Player holder = null;
        double best = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double d = player.position().distanceToSqr(pos);
            if (slowsTime(Powers.get(player)) && player.isAlive() && d < r2 && d < best) {
                best = d;
                holder = player;
            }
        }
        return holder;
    }

    public static Power zoneOwner(Level level, Vec3 pos) {
        double r2 = radius() * radius();
        Power owner = Power.NONE;
        double best = Double.MAX_VALUE;
        for (Player player : level.players()) {
            PowerData data = Powers.get(player);
            double d = player.position().distanceToSqr(pos);
            if (slowsTime(data) && player.isAlive() && d < r2 && d < best) {
                best = d;
                owner = slowTimeOf(data);
            }
        }
        return owner;
    }

    /**
     * Whether Quicksilver Time slows what is at {@code pos}: it is within the radius of a running zone, and (3.3) not behind
     * the wall of a Hex that keeps that Quicksilver out.
     */
    public static boolean isInZone(Level level, Vec3 pos) {
        Zones all = zonesOf(level);
        List<Vec3> zones = all.centers();
        if (zones.isEmpty()) {
            return false;
        }
        List<Player> speedsters = all.speedsters();
        for (int i = 0; i < zones.size(); i++) {
            Vec3 center = zones.get(i);
            double r = all.radii().get(i);
            if (center.distanceToSqr(pos) < r * r && !Shelters.keepsOut(level, speedsters.get(i), center.add(0, 1, 0),
                    pos, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many times slower Quicksilver Time makes this entity: the zone's factor, unless it is a boss or a minion that
     * says otherwise ({@link BossHooks#slowFactor}: Ultron's bodies are not slowed at all, Thanos's half).
     */
    private static int factorOf(Entity entity) {
        // 3.6: a Time Stone's zone (and no speedster's) has its own factor (lane L2's StoneZones).
        int zone = StoneZones.factorFor(entity, factor());
        return entity instanceof BossHooks boss ? boss.slowFactor(zone) : zone;
    }

    /**
     * (3.18.1) How many times slower another player is in slowed time: the zone's factor while he stands in one he does not
     * hold himself, else 1. Slowness does not touch a hero's flight, so his flying speed is divided by this
     * ({@code PowerTicker}); his walking gets the matching Slowness ({@link #serverTick}).
     */
    public static int playerFactor(Player player) {
        if (player.isSpectator() || holdsZone(player, Powers.get(player)) || Demiurge.is(Powers.get(player))
                || !isInZone(player.level(), player.position())) {
            return 1;
        }
        return Math.max(1, factorOf(player));
    }

    /**
     * (3.18.3) A player in another's slowed time is slow in the air too: Slowness never touches a jump (the game moves a
     * player in the air at its own pace), so he moved at full speed as soon as he jumped. Called on his own client after
     * each of his ticks (his client moves him): his speed across the ground is held to a jump's at the zone's factor.
     */
    public static void slowAir(Player player) {
        if (player.onGround() || player.getAbilities().flying || player.isInWater() || player.isInLava() || player.isPassenger()) {
            return;
        }
        int factor = playerFactor(player);
        if (factor <= 1) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        double across = Math.sqrt(v.x * v.x + v.z * v.z);
        double cap = 0.3 / factor;
        if (across > cap) {
            double k = cap / across;
            player.setDeltaMovement(v.x * k, v.y, v.z * k);
        }
    }

    /** Whether this entity is slowed by Quicksilver Time at all (players and things they ride are never tick-skipped). */
    private static boolean affected(Entity entity) {
        if (entity instanceof Player || Platform.get().parts().isPart(entity)
                || entity.getControllingPassenger() instanceof Player
                || entity.hasPassenger(e -> e instanceof Player)) {
            return false;
        }
        return isInZone(entity.level(), entity.position()) && factorOf(entity) > 1;
    }

    /** Arrows and thrown things are slowed by scaling their speed instead of skipping ticks, so they glide smoothly. */
    private static boolean slowedBySpeed(Entity entity) {
        return entity instanceof AbstractArrow || entity instanceof ThrowableProjectile;
    }

    /** A trident flying back to its owner (Loyalty) rebuilds its speed every tick, so it can't be slowed. */
    private static boolean returning(Projectile projectile) {
        return projectile instanceof AbstractArrow arrow && arrow.isNoPhysics();
    }

    /** How fast time runs for this entity: 1/N for a slowed projectile, otherwise 1. */
    public static double timeScale(Entity entity) {
        CompoundTag data = ModAttachments.PERSISTENT_DATA.get(entity);
        return data.contains(SLOWED_TAG) ? 1.0 / Math.max(1, data.getCompound(SLOWED_TAG).getInt("Factor")) : 1.0;
    }

    /**
     * Whether this tick of {@code entity} is one of the skipped ones (its zone lets it have every Nth tick only). Both
     * sides, and pure: also asked by the bosses' own ticks (the Horsemen, Apocalypse).
     *
     * <p>3.18.0: the beat is the level's clock (and the entity's id, so the creatures of a zone do not all move in the
     * same tick), not the entity's own tick count: from 26.1 a cancelled tick no longer counts up {@code tickCount}
     * (NeoForge fires the event before {@code Entity.commonTick}), so a beat on it stopped a slowed creature for good.
     * Creatures are skipped on clients too now (before, a client ticked them and only the server's updates were slowed:
     * a mob walked a step, stood, walked a step); {@link #renderPartialTick} spreads each real tick over the whole beat.
     */
    public static boolean skipTick(Entity entity) {
        return skipsAt(entity, entity.level().getGameTime());
    }

    /** {@link #skipTick} at the level's game time {@code time} (the tests walk it through a beat). */
    public static boolean skipsAt(Entity entity, long time) {
        if (slowedBySpeed(entity) || !affected(entity)) {
            return false;
        }
        return Math.floorMod(time + entity.getId(), factorOf(entity)) != 0;
    }

    /**
     * Called from {@code EntityTickEvent.Pre} on both sides.
     *
     * @return true if this tick must be skipped
     */
    public static boolean beforeTick(Entity entity) {
        boolean skip = skipTick(entity);
        //#if <26.3
        if (skip) {
            // (up to 1.21.11 the level counts the tick before the event: a skipped tick is not one of its own, as on
            // 26.x, so its timers, its age and its animations run at 1/N too)
            entity.tickCount--;
        }
        //#endif
        if (skip && entity.level().isClientSide()) {
            Beat beat = BEATS.get(entity);
            if (beat != null) {
                // (up to 1.21.11 the level set the old position to the current one before the event: the entity is
                // drawn from where it was at its last real tick, see renderPartialTick)
                entity.xo = beat.x;
                entity.yo = beat.y;
                entity.zo = beat.z;
                entity.xOld = beat.x;
                entity.yOld = beat.y;
                entity.zOld = beat.z;
                entity.yRotO = beat.yRot;
                entity.xRotO = beat.xRot;
            }
        }
        return skip;
    }

    /**
     * A client's record of a tick-skipped entity's last real tick: where it was before it (the old position that tick
     * left), when it was, and how long its beat is.
     */
    private record Beat(double x, double y, double z, float yRot, float xRot, long at, int factor) {
    }

    /** Client only: the beats of the slowed entities it draws. */
    private static final Map<Entity, Beat> BEATS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Client, as an entity is drawn: the part of its last real tick to draw. A tick-skipped entity moves (and animates)
     * a whole tick at once every Nth tick; drawn with the frame's own partial tick it would jump and stand. Spread over
     * its N ticks it glides at 1/N speed, its limbs and head with it (everything that is drawn between the old and the
     * new value of a tick).
     */
    public static float renderPartialTick(Entity entity, float partialTick) {
        Beat beat = BEATS.isEmpty() ? null : BEATS.get(entity);
        if (beat == null || beat.factor <= 1) {
            return partialTick;
        }
        long since = entity.level().getGameTime() - beat.at;
        return (float) Math.max(0.0, Math.min(1.0, (since + partialTick) / beat.factor));
    }

    /**
     * 3.18.0: the Slowness that leaves another player (whom the game moves, so he can't be tick-skipped) about 1/N of his
     * walking speed: each level takes 15 percent, so a factor of 10 is level VI (amplifier 5, 10 percent left). Never
     * more than that: level VII would leave him nothing, and he must still be able to walk out.
     */
    public static int playerSlowness(int factor) {
        double lost = 1.0 - 1.0 / Math.max(1, factor);
        return Math.max(0, Math.min(5, (int) Math.round(lost / 0.15) - 1));
    }

    /**
     * 3.18.0: how many times slower time runs at {@code pos} (1 outside every zone): a speedster's zone's factor, or a
     * Time Stone's where only one of those holds it. For what is not an entity: particles, and the shots that fly as
     * points (Jim Hammond's fireballs, the spellbooks' shots).
     */
    public static int factorAt(Level level, Vec3 pos) {
        if (!isInZone(level, pos)) {
            return 1;
        }
        double r2 = radius() * radius();
        boolean stone = false;
        for (Player player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }
            PowerData data = Powers.get(player);
            double d = player.position().distanceToSqr(pos);
            if (slowsTime(data) && d < r2) {
                return factor();
            }
            stone |= StoneZones.slows(data) && d < StoneZones.radius() * StoneZones.radius();
        }
        return stone ? StoneZones.factor() : factor();
    }

    /**
     * 3.18.0: whether a point-like shot (no entity of its own) at {@code pos} skips this tick, as a tick-skipped entity
     * does: it moves on every Nth tick only, a whole tick's way, so it hits exactly as it would have. {@code salt}
     * spreads the beats (the shot's own entity's id when it has one, so its body keeps the same beat on the clients).
     */
    public static boolean skipsPoint(Level level, Vec3 pos, int salt) {
        int n = factorAt(level, pos);
        return n > 1 && Math.floorMod(level.getGameTime() + salt, n) != 0;
    }

    /** Server, once per level tick: slow down projectiles entering the zones, restore the ones that left, slow players. */
    public static void serverTick(ServerLevel level) {
        Zones all = zonesOf(level);
        List<Vec3> zones = all.centers();
        for (int z = 0; z < zones.size(); z++) {
            Vec3 center = zones.get(z);
            double radius = all.radii().get(z);
            AABB box = new AABB(center, center).inflate(radius);
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                    p -> slowedBySpeed(p) && !returning(p) && !ModAttachments.PERSISTENT_DATA.get(p).contains(SLOWED_TAG)
                            && !MagnetoAbilities.isHeld(p)
                            && factorOf(p) > 1 && isInZone(level, p.position()))) {
                slow(projectile, factorOf(projectile));
            }
            for (Player player : level.players()) {
                PowerData data = Powers.get(player);
                // (3.18.1) Billy the Demiurge stands outside every slowed time
                boolean speedster = holdsZone(player, data) || Demiurge.is(data);
                if (!speedster && !player.isSpectator() && !player.isCreative() && player.position().distanceToSqr(center) < radius * radius
                        && isInZone(level, player.position())) {
                    int amplifier = playerSlowness(factorOf(player));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, amplifier, true, false, false));
                    // Whatever he rides isn't tick-skipped (he steers it), so it is slowed the same way.
                    if (player.getVehicle() instanceof LivingEntity mount) {
                        mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, amplifier, true, false, false));
                    }
                }
            }
        }
        // Restore projectiles that left every zone (or whose Quicksilver Time ended), and let a Loyalty trident that
        // started flying home go at its own pace.
        if (!SLOWED.isEmpty()) {
            List<Projectile> leaving = new ArrayList<>();
            List<Projectile> returningHome = new ArrayList<>();
            for (Projectile projectile : SLOWED) {
                if (projectile.level() != level) {
                    continue;
                }
                if (projectile.isRemoved() || !isInZone(level, projectile.position())) {
                    leaving.add(projectile);
                } else if (returning(projectile)) {
                    returningHome.add(projectile);
                }
            }
            for (Projectile projectile : leaving) {
                SLOWED.remove(projectile);
                if (!projectile.isRemoved()) {
                    restore(projectile);
                }
            }
            for (Projectile projectile : returningHome) {
                SLOWED.remove(projectile);
                release(projectile, false);
            }
        }
    }

    /**
     * Called from {@code EntityTickEvent.Post} on both sides. Vanilla drag (x0.99) runs every tick, and a slowed
     * projectile takes N ticks per normal tick, so the extra drag is given back to keep its speed (and its hit) the same
     * as without the slow motion. It only runs when the projectile really ticked (not while the game is frozen), and the
     * same way on clients (the server tells them which projectiles are slowed), so their copy stays in step.
     */
    public static void afterTick(Entity entity) {
        if (entity.level().isClientSide() && !slowedBySpeed(entity)) {
            if (affected(entity)) {
                BEATS.put(entity, new Beat(entity.xo, entity.yo, entity.zo, entity.yRotO, entity.xRotO,
                        entity.level().getGameTime(), factorOf(entity)));
            } else if (!BEATS.isEmpty()) {
                BEATS.remove(entity);
            }
        }
        if (!slowedBySpeed(entity)) {
            return;
        }
        Projectile projectile = (Projectile) entity;
        CompoundTag data = ModAttachments.PERSISTENT_DATA.get(projectile);
        if (!data.contains(SLOWED_TAG)) {
            return;
        }
        int n = data.getCompound(SLOWED_TAG).getInt("Factor");
        if (n <= 1 || returning(projectile) || projectile.isInWater() || projectile instanceof AbstractArrow arrow
                && ((AbstractArrowAccessor) arrow).enhanced$isInGround()) {
            return;
        }
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(Math.pow(0.99, 1.0 / n - 1.0)));
    }

    /** Server: a player started seeing a projectile that is already slowed. */
    public static void onStartTracking(Entity entity, ServerPlayer player) {
        if (entity instanceof Projectile projectile
                && ModAttachments.PERSISTENT_DATA.get(projectile).contains(SLOWED_TAG)) {
            int factor = ModAttachments.PERSISTENT_DATA.get(projectile).getCompound(SLOWED_TAG).getInt("Factor");
            Net.sendToPlayer(player, new SlowedProjectilePayload(projectile.getId(), factor));
        }
    }

    /** Client: the server says this projectile is slowed (factor > 1) or flies normally again (0). */
    public static void onSlowedSync(Entity entity, int factor) {
        if (factor > 1) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Factor", factor);
            ModAttachments.PERSISTENT_DATA.get(entity).put(SLOWED_TAG, tag);
        } else {
            ModAttachments.PERSISTENT_DATA.get(entity).remove(SLOWED_TAG);
        }
    }

    private static void slow(Projectile projectile, int factor) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("NoGravity", projectile.isNoGravity());
        tag.putInt("Factor", factor);
        ModAttachments.PERSISTENT_DATA.get(projectile).put(SLOWED_TAG, tag);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(1.0 / factor));
        projectile.hasImpulse = true;
        SLOWED.add(projectile);
        Net.sendToPlayersTrackingEntity(projectile, new SlowedProjectilePayload(projectile.getId(), factor));
    }

    /** Puts a slowed projectile back to full speed. Also used when a slowed projectile is loaded from disk. */
    public static void restore(Projectile projectile) {
        release(projectile, true);
    }

    /**
     * Server: takes a projectile out of the slow motion right away, at full speed. Called when it hits something (so
     * the hit is as strong as without the slow motion) and before something else takes over its flight (Magneto's
     * shield). If it is still in a zone afterwards, the next level tick slows it again.
     */
    public static void unslow(Projectile projectile) {
        if (ModAttachments.PERSISTENT_DATA.get(projectile).contains(SLOWED_TAG)) {
            SLOWED.remove(projectile);
            release(projectile, true);
        }
    }

    private static void release(Projectile projectile, boolean speedUp) {
        CompoundTag tag = ModAttachments.PERSISTENT_DATA.get(projectile).getCompound(SLOWED_TAG);
        ModAttachments.PERSISTENT_DATA.get(projectile).remove(SLOWED_TAG);
        if (!projectile.level().isClientSide()) {
            Net.sendToPlayersTrackingEntity(projectile, new SlowedProjectilePayload(projectile.getId(), 0));
        }
        int factor = Math.max(1, tag.getInt("Factor"));
        projectile.setNoGravity(tag.getBoolean("NoGravity"));
        if (speedUp) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(factor));
        }
        projectile.hasImpulse = true;
    }

    /** A projectile saved to disk while slowed comes back at full speed (it is slowed again if still in a zone). */
    public static void onJoin(Entity entity) {
        if (entity instanceof Projectile projectile && !SLOWED.contains(projectile)
                && ModAttachments.PERSISTENT_DATA.get(projectile).contains(SLOWED_TAG)) {
            restore(projectile);
        }
    }

    public static void clear() {
        SLOWED.clear();
        CACHE.clear();
        BEATS.clear();
    }

    private SlowMotion() {
    }
}
