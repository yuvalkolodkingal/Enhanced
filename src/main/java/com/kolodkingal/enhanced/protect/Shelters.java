package com.kolodkingal.enhanced.protect;

import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.vishanti.Ward;
import com.kolodkingal.enhanced.vishanti.WardManager;
import com.kolodkingal.enhanced.vishanti.WardStore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The walls that keep powers out (3.3), asked in one place by every power of the mod:
 * <ul>
 *     <li>the wall of Wanda's <b>Hex</b>: nothing a power does crosses it from outside (a projectile, a beam, a blast, a grip, a
 *     pull, an effect, a block changed...) unless its maker may cross it too ({@link #hexTrusts}: Wanda, whoever she let in,
 *     creatures if she let creatures in; never a boss);</li>
 *     <li>a <b>ward</b> of the Book of the Vishanti: what a monster, a boss or a boss's minion launches ({@link #hostile}) and dark
 *     magic ({@link DarkMagic}) stop at its edge; the powers of players pass;</li>
 *     <li>Billy's <b>Hex Barrier</b>: whatever comes at him from the front stops at the plate; only chaos and dark magic wear
 *     it down ({@link BillyBarrier});</li>
 *     <li>the Scarlet Witch's <b>Chaos Shield</b> (beams only; the rest of its rules are {@link WandaAbilities}').</li>
 * </ul>
 * A power that starts inside a wall's sphere is never stopped by it: the walls keep out, they do not keep in.
 *
 * <p>The geometry and the judgement here run on both sides (a client draws a beam to where the server stops it): the Hexes are
 * synced with the level, the wards a client was told of are kept in {@link #CLIENT_WARDS}.
 */
public final class Shelters {
    /** Which wall stopped a line of power. */
    public enum Wall {
        CHAOS_SHIELD, HEX_BARRIER, HEX, WARD
    }

    /** The wards each dimension's client was told of ({@link #acceptWards}). Client only; empty on a server. */
    private static final Map<ResourceKey<Level>, List<Ward>> CLIENT_WARDS = new HashMap<>();
    /** When each wall last sparked where it stopped something, so a stream of hits sparks a few times a second. */
    private static final Map<UUID, Long> LAST_FLASH = new HashMap<>();

    // ---- who is behind a power ----

    /**
     * Whoever is behind a power: the owner of a projectile, of a lit TNT, of a lingering cloud or of fangs (followed back a
     * few steps: an arrow shot back by Magneto is his), the thrower of a thrown block; anything else is its own maker.
     */
    @Nullable
    public static Entity maker(@Nullable Entity entity) {
        for (int i = 0; entity != null && i < 4; i++) {
            Entity owner = entity instanceof TraceableEntity traced ? traced.getOwner()
                    : entity instanceof TelekineticBlockEntity block && block.thrower() != null ? entity.level().getPlayerByUUID(block.thrower())
                    : null;
            if (owner == null || owner == entity) {
                return entity;
            }
            entity = owner;
        }
        return entity;
    }

    // ---- the Hex ----

    /**
     * Whether the wall of this Hex lets a power of {@code source} through: its maker ({@link #maker}) may cross the wall into
     * the Hex (Wanda, a Creative player, a player she allowed and their pets, creatures if she allows creatures, the Vision
     * of her Hex). A power with no maker (a dispenser's arrow, a lightning bolt nobody called) is kept out.
     */
    public static boolean hexTrusts(Hex hex, @Nullable Entity source) {
        Entity maker = maker(source);
        return maker instanceof LivingEntity living && HexManager.judge(hex, living) == HexManager.Verdict.PASS;
    }

    /** The Hex whose wall keeps a power of {@code source} at {@code from} out of {@code at}, or null. */
    @Nullable
    public static Hex hexAgainst(Level level, @Nullable Entity source, Vec3 from, Vec3 at) {
        List<Hex> hexes = HexManager.data(level).hexes();
        if (hexes.isEmpty()) {
            return null;
        }
        for (Hex hex : hexes) {
            if (hex.contains(at) && !hex.contains(from) && !hexTrusts(hex, source)) {
                return hex;
            }
        }
        return null;
    }

    // ---- the wards ----

    /**
     * Whether a ward keeps out what {@code source} launches: its maker is a monster, a boss or a minion of one, or something a
     * boss fight made (a sand lance, a meteor). A player's power, and a pet's, is never kept out.
     */
    public static boolean hostile(@Nullable Entity source) {
        Entity maker = maker(source);
        if (maker == null || maker instanceof Player) {
            return false;
        }
        if (maker instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            return false; // someone's: a tamed wolf, a hero's Sentry
        }
        return maker.getType().getCategory() == MobCategory.MONSTER || maker.getType().is(ModTags.BOSSES)
                || maker.getType().is(ModTags.BOSS_MINIONS) || BossFights.fightOf(maker) != null || BossFights.isFightEntity(maker);
    }

    /** The wards that stand in this level: the server's store, or what this client was told of. */
    public static List<Ward> wards(Level level) {
        if (level instanceof ServerLevel server) {
            WardStore store = WardStore.peek(server);
            return store == null || store.isEmpty() || !WardManager.wardsOn() ? List.of() : store.wards();
        }
        return CLIENT_WARDS.getOrDefault(level.dimension(), List.of());
    }

    /** Client: the wards the server told of for one dimension (the domes of {@code WardSyncPayload}). */
    public static void acceptWards(ResourceKey<Level> dimension, List<Ward> wards) {
        CLIENT_WARDS.put(dimension, List.copyOf(wards));
    }

    /** Client: forget every ward (logging out). */
    public static void forgetWards() {
        CLIENT_WARDS.clear();
    }

    /** The ward that keeps a power of {@code source} at {@code from} out of {@code at} (a hostile or dark one), or null. */
    @Nullable
    public static Ward wardAgainst(Level level, @Nullable Entity source, Vec3 from, Vec3 at, boolean dark) {
        if (!dark && !hostile(source)) {
            return null;
        }
        for (Ward ward : wards(level)) {
            if (ward.contains(at) && !ward.contains(from)) {
                return ward;
            }
        }
        return null;
    }

    // ---- one point, one creature, one block ----

    /**
     * Whether a wall (a Hex's, a ward's) keeps a power of {@code source}, coming from {@code from}, out of the point {@code at}.
     * Nothing is paid and nothing is shown: what a power asks when it looks for targets.
     */
    public static boolean keepsOut(Level level, @Nullable Entity source, Vec3 from, Vec3 at, boolean dark) {
        return hexAgainst(level, source, from, at) != null || wardAgainst(level, source, from, at, dark) != null;
    }

    /** {@link #keepsOut(Level, Entity, Vec3, Vec3, boolean)} for a creature (its middle), and Billy's raised barrier in front of him. */
    public static boolean shelters(@Nullable Entity source, Vec3 from, Entity target) {
        if (target == source) {
            return false;
        }
        if (keepsOut(target.level(), source, from, middle(target), DarkMagic.casts(source))) {
            return true;
        }
        return target instanceof Player billy && source != billy && BillyBarrier.isUp(billy) && BillyBarrier.covers(billy, from);
    }

    /** {@link #shelters(Entity, Vec3, Entity)} from where {@code source} stands (its middle). */
    public static boolean shelters(Entity source, Entity target) {
        return shelters(source, middle(source), target);
    }

    /**
     * A power of {@code source} reaches for {@code target} (a grip, a pull, a curse, a blow that is not damage): whether a wall
     * stops it. A Hex's or a ward's wall sparks where the power met it; Billy's barrier pays {@code cost} of its health for it (any
     * power of the Scarlet Witch shatters it). Server.
     *
     * @return true if it is stopped (the power must leave the target alone)
     */
    public static boolean stops(@Nullable Entity source, Vec3 from, Entity target, float cost) {
        if (target == source || !(target.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 at = middle(target);
        boolean dark = DarkMagic.casts(source);
        Hex hex = hexAgainst(level, source, from, at);
        if (hex != null) {
            flash(level, hex.id(), entry(hex.center(), hex.radius(), from, at), HexManager.wallColor(hex));
            return true;
        }
        Ward ward = wardAgainst(level, source, from, at, dark);
        if (ward != null) {
            flash(level, ward.owner(), entry(ward.center(), ward.radius(), from, at), ward.color());
            return true;
        }
        if (target instanceof ServerPlayer billy && source != billy && BillyBarrier.isUp(billy) && BillyBarrier.covers(billy, from)) {
            BillyBarrier.stopPower(billy, source, from, cost);
            return true;
        }
        return false;
    }

    /** {@link #stops(Entity, Vec3, Entity, float)} from where {@code source} stands. */
    public static boolean stops(Entity source, Entity target, float cost) {
        return stops(source, middle(source), target, cost);
    }

    /**
     * Whether a wall keeps a power of {@code source} at {@code from} from changing the block at {@code pos} (a Hex's: no block of
     * a Hex is changed from outside by anyone she has not let in; a ward's: no monster's or boss's attack breaks into it).
     */
    public static boolean keepsOut(Level level, @Nullable Entity source, Vec3 from, BlockPos pos) {
        return keepsOut(level, source, from, Vec3.atCenterOf(pos), false);
    }

    // ---- a line of power: a beam, a ray, a projectile's next move ----

    /**
     * A wall that a line of power runs into: which one, whose (the Scarlet Witch or Billy holding it; null for a Hex or a ward),
     * where on it the line ends, and its colour (for the sparks where it stops).
     */
    public record Stop(Wall wall, @Nullable Player owner, Vec3 point, int color, UUID id) {
    }

    /**
     * The first wall that a line of power from {@code from} to {@code to} runs into, or null: a complete Chaos Shield, a raised
     * Hex Barrier from the front, the wall of a Hex that does not trust {@code firer}, a ward if {@code firer} is hostile or the
     * power is {@code dark}. A line that starts inside a sphere is not stopped by it. Both sides.
     *
     * @param firer the one whose power it is (never stopped by its own shield or barrier)
     * @param dark  whether the power is dark magic (a ward stops it)
     */
    @Nullable
    public static Stop across(Level level, @Nullable Entity firer, Vec3 from, Vec3 to, float partialTick, boolean dark) {
        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length < 1.0E-6) {
            return null;
        }
        Stop nearest = null;
        double best = length;
        WandaAbilities.ShieldStop chaos = WandaAbilities.chaosShieldAcross(level, firer, from, to, partialTick);
        if (chaos != null) {
            best = chaos.point().distanceTo(from);
            nearest = new Stop(Wall.CHAOS_SHIELD, chaos.owner(), chaos.point(),
                    AbilityColors.glow(chaos.owner(), AbilityColors.shield(chaos.owner()), WandaAbilities.glow(chaos.owner())), chaos.owner().getUUID());
        }
        // 3.6: a Shield of the Seraphim (Doctor Strange's, lane L3) ends the beam at its disc; it pays nothing.
        com.kolodkingal.enhanced.strange.SeraphimShield.Crossing seraphim =
                com.kolodkingal.enhanced.strange.SeraphimShield.across(level, firer, from, to, partialTick);
        if (seraphim != null && seraphim.at() < best) {
            best = seraphim.at();
            nearest = new Stop(Wall.CHAOS_SHIELD, null, seraphim.point(),
                    com.kolodkingal.enhanced.strange.StrangeAbilities.ORANGE, seraphim.owner().getUUID());
        }
        AABB reach = new AABB(from, to).inflate(BillyBarrier.distance() + BillyBarrier.height());
        for (Player billy : level.getEntitiesOfClass(Player.class, reach, p -> p != firer && !p.isSpectator() && BillyBarrier.isUp(p))) {
            Vec3 hit = plateCrossing(billy, from, to, partialTick);
            if (hit != null && hit.distanceTo(from) < best) {
                best = hit.distanceTo(from);
                nearest = new Stop(Wall.HEX_BARRIER, billy, hit,
                        AbilityColors.color(billy, HeroAbility.BILLY_HEX_BARRIER, BillyAbilities.MAIN),
                        billy.getUUID());
            }
        }
        for (Hex hex : HexManager.data(level).hexes()) {
            double at = enters(hex.center(), hex.radius(), from, path, length);
            if (at >= 0 && at < best && !hexTrusts(hex, firer)) {
                best = at;
                nearest = new Stop(Wall.HEX, null, from.add(path.scale(at / length)), HexManager.wallColor(hex), hex.id());
            }
        }
        if (dark || hostile(firer)) {
            for (Ward ward : wards(level)) {
                double at = ward.awake() ? enters(ward.center(), ward.radius(), from, path, length) : -1;
                if (at >= 0 && at < best) {
                    best = at;
                    nearest = new Stop(Wall.WARD, null, from.add(path.scale(at / length)), ward.color(), ward.owner());
                }
            }
        }
        return nearest;
    }

    /**
     * {@link #across} for what moves through the world (a projectile, a thrown block, a lit TNT): only the walls that keep out,
     * a Hex's and a ward's (Billy's barrier and the Chaos Shield catch projectiles themselves, and pay for them).
     */
    @Nullable
    public static Stop wallAcross(Level level, Entity missile, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length < 1.0E-6) {
            return null;
        }
        Stop nearest = null;
        double best = length;
        for (Hex hex : HexManager.data(level).hexes()) {
            double at = enters(hex.center(), hex.radius(), from, path, length);
            if (at >= 0 && at <= best && !hexTrusts(hex, missile)) {
                best = at;
                nearest = new Stop(Wall.HEX, null, from.add(path.scale(at / length)), HexManager.wallColor(hex), hex.id());
            }
        }
        if (DarkMagic.is(missile) || hostile(missile)) {
            for (Ward ward : wards(level)) {
                double at = ward.awake() ? enters(ward.center(), ward.radius(), from, path, length) : -1;
                if (at >= 0 && at <= best) {
                    best = at;
                    nearest = new Stop(Wall.WARD, null, from.add(path.scale(at / length)), ward.color(), ward.owner());
                }
            }
        }
        return nearest;
    }

    /**
     * Where a player's aim (or any ray of a power) from {@code from} to {@code to} ends at the wall of a Hex that keeps {@code
     * source} out: {@code to} itself if no such wall is in the way. What every ray of {@code Raycasts} asks, so nothing behind
     * the wall can be picked, grabbed or struck from outside. Both sides.
     */
    public static Vec3 hexClip(Level level, @Nullable Entity source, Vec3 from, Vec3 to) {
        List<Hex> hexes = HexManager.data(level).hexes();
        if (hexes.isEmpty()) {
            return to;
        }
        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length < 1.0E-6) {
            return to;
        }
        double best = length;
        for (Hex hex : hexes) {
            double at = enters(hex.center(), hex.radius(), from, path, length);
            if (at >= 0 && at < best && !hexTrusts(hex, source)) {
                best = at;
            }
        }
        // (A hair short of the wall, so what the ray finds there is outside it.)
        return best >= length ? to : from.add(path.scale(Math.max(0.0, best - 0.05) / length));
    }

    /**
     * How far along a line from {@code from} (outside the sphere) it first meets the sphere, or -1 if it does not within
     * {@code length} (or starts inside).
     */
    static double enters(Vec3 center, double radius, Vec3 from, Vec3 path, double length) {
        Vec3 f = from.subtract(center);
        double outside = f.lengthSqr() - radius * radius;
        if (outside < 0) {
            return -1; // it starts inside: the wall keeps out, it does not keep in
        }
        Vec3 dir = path.scale(1.0 / length);
        double along = f.dot(dir);
        double discriminant = along * along - outside;
        if (discriminant < 0) {
            return -1;
        }
        double at = -along - Math.sqrt(discriminant);
        return at >= 0 && at <= length ? at : -1;
    }

    /** Where the line from {@code from} to {@code at} meets the sphere (or {@code at} if it does not cross it). */
    static Vec3 entry(Vec3 center, double radius, Vec3 from, Vec3 at) {
        Vec3 path = at.subtract(from);
        double length = path.length();
        double t = length < 1.0E-6 ? -1 : enters(center, radius, from, path, length);
        return t < 0 ? at : from.add(path.scale(t / length));
    }

    /**
     * Where a line from {@code from} to {@code to} crosses the plate of Billy's raised barrier from the front, or null: the plate
     * is the rectangle he holds ({@link BillyBarrier#frame}), a little larger than drawn so a beam at its rim does not slip past.
     */
    @Nullable
    public static Vec3 plateCrossing(Player billy, Vec3 from, Vec3 to, float partialTick) {
        Vec3 body = billy.getPosition(partialTick).add(0, billy.getBbHeight() * 0.55, 0);
        BillyBarrier.Frame plate = BillyBarrier.frame(body, billy.getViewVector(partialTick));
        double before = from.subtract(plate.center()).dot(plate.forward());
        double after = to.subtract(plate.center()).dot(plate.forward());
        if (before <= 0 || after > 0 || !BillyBarrier.covers(billy, from)) {
            return null; // it starts behind the plate, or never reaches it
        }
        Vec3 hit = from.add(to.subtract(from).scale(before / (before - after)));
        Vec3 off = hit.subtract(plate.center());
        double halfWidth = BillyBarrier.width() * 0.5 + 0.25;
        double halfHeight = BillyBarrier.height() * 0.5 + 0.25;
        return Math.abs(off.dot(plate.right())) <= halfWidth && Math.abs(off.dot(plate.up())) <= halfHeight ? hit : null;
    }

    // ---- what is seen ----

    /** Sparks of the wall's colour where it stopped something (at most four times a second per wall), and a soft fizz. */
    public static void flash(ServerLevel level, UUID wall, Vec3 at, int color) {
        long now = level.getGameTime();
        Long last = LAST_FLASH.get(wall);
        if (last != null && now - last < 5 && now >= last) {
            return;
        }
        LAST_FLASH.put(wall, now);
        level.sendParticles(new EnergyParticleOptions(color, 1.4F), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.08);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.7F);
    }

    /** {@link #flash(ServerLevel, UUID, Vec3, int)} for a stop of {@link #across}. */
    public static void flash(ServerLevel level, Stop stop) {
        flash(level, stop.id(), stop.point(), stop.color());
    }

    /** The middle of an entity's body. */
    public static Vec3 middle(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    /** Server stop: forget the sparks' pace. */
    public static void clear() {
        LAST_FLASH.clear();
    }

    private Shelters() {
    }
}
