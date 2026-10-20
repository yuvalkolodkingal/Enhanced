package com.kolodkingal.enhanced.ironman.hulkbuster;

import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.SuitMoves;
import com.kolodkingal.enhanced.ironman.nano.NanoBeams;
import com.kolodkingal.enhanced.ironman.nano.NanoTech;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Hulkbuster's moves (3.8, lane L4, PLAN 6.4):
 * <ul>
 * <li>R <b>Rapid Punch Combo</b>: 5 punches in 15 ticks at the creature in front (reach {@link #PUNCH_REACH} from his
 * middle): {@code punchDamage} 6 each, the last {@code punchFinalDamage} 10 and a knockback of {@code
 * punchFinalKnockback} 2.5 ({@code hulkbuster_punch}); cooldown 60.</li>
 * <li>G slow flight (lane L3's flight framework, {@code flightSpeed} 0.06).</li>
 * <li>V <b>Heavy Unibeam</b> (held ≤ 60): 2 wide, range 32, {@code unibeamDamage} 8 every 10 ticks to every creature
 * along it; {@code unibeamEnergyPerSecond} 10 ({@link NanoBeams}).</li>
 * <li>B <b>Grab & Throw</b>: he grabs the creature in front within {@code grabRange} 5 (not a boss, not a {@code
 * #enhanced:grab_immune} one, none wider than 3 blocks; a player only with PvP) and holds it in his right fist up to
 * {@code grabMaxTicks} 60 ({@link Live#IRON_GRABBING}: he cannot fly); B again (or the time) throws it {@code
 * throwDistance} 12 blocks along his look: when it lands it takes {@code throwDamage} 10, and so does what it lands
 * on (within 2); cooldown 160 from the throw.</li>
 * <li>N <b>Veronica</b> sends a new arm ({@link Veronica#callArm}).</li>
 * </ul>
 */
public final class HulkbusterMoves implements SuitMoves {
    public static final HulkbusterMoves INSTANCE = new HulkbusterMoves();
    /** The combo: its punches, the ticks between two, its reach (from his middle). */
    public static final int PUNCHES = 5;
    public static final int PUNCH_GAP = 3;
    public static final double PUNCH_REACH = 5.0;
    /** The heavy Unibeam's numbers the plan gives without an option. */
    static final int UNIBEAM_RANGE = 32;
    static final double UNIBEAM_WIDTH = 2.0;
    static final int UNIBEAM_MAX_TICKS = 60;
    /** What may not be grabbed: wider than this. */
    public static final double GRAB_MAX_WIDTH = 3.0;
    /** A thrown creature's flight is watched this long at most; what it lands on within this radius is hit too. */
    static final int THROW_WATCH_TICKS = 60;
    static final double LANDING_RADIUS = 2.0;

    private static final Map<UUID, Moves> MOVES = new HashMap<>();

    /** One wearer's moves under way. */
    static final class Moves {
        long punchSince;
        int punches;
        int grabbed = -1;
        long grabSince;
        boolean grabbedNoAi;
        /** The creatures thrown and still flying. */
        final List<Thrown> thrown = new ArrayList<>();
    }

    /** A creature in flight: its id, when it was thrown, and whether it had no AI of its own before the grab. */
    record Thrown(int id, long at, boolean noAi) {
    }

    static Moves moves(ServerPlayer player) {
        return MOVES.computeIfAbsent(player.getUUID(), k -> new Moves());
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> {
                if (down) {
                    punch(player, data);
                }
            }
            case 3 -> {
                if (down) {
                    NanoBeams.start(player, data, unibeam());
                } else {
                    NanoBeams.stop(player, data);
                }
            }
            case 4 -> {
                if (down) {
                    if (data.is(Live.IRON_GRABBING)) {
                        throwIt(player, data);
                    } else {
                        grab(player, data);
                    }
                }
            }
            case 5 -> {
                if (down) {
                    Veronica.callArm(player, data);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        Moves m = MOVES.get(player.getUUID());
        if (m != null) {
            punchTick(player, m, now);
            holdTick(player, data, m, now);
        }
        NanoBeams.tick(player, data, unibeam());
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        NanoBeams.stop(player, data);
        Moves m = MOVES.get(player.getUUID());
        if (m != null) {
            m.punchSince = 0L;
            release(player, data, m);
        }
        data.set(Live.IRON_GRABBING, false);
        Veronica.cancel(player.getUUID());
    }

    static NanoBeams.Spec unibeam() {
        return new NanoBeams.Spec(NanoBeams.HULKBUSTER_UNIBEAM, AvengersBeams.HULKBUSTER_UNIBEAM, UNIBEAM_RANGE,
                UNIBEAM_WIDTH, (float) NanoTech.cfg(IronManConfig.HULKBUSTER_UNIBEAM_DAMAGE, 8.0),
                NanoTech.cfg(IronManConfig.HULKBUSTER_UNIBEAM_ENERGY_PER_SECOND, 10.0), UNIBEAM_MAX_TICKS, true);
    }

    // ---- R: the punches ----

    /** R: the combo begins (its first punch at once). */
    public static boolean punch(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        Moves m = moves(player);
        if (m.punchSince != 0L || !data.isReady(HeroAbility.IRON_MAN_REPULSOR, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.IRON_MAN_REPULSOR, now, NanoTech.cfg(IronManConfig.HULKBUSTER_PUNCH_COOLDOWN,
                60));
        m.punchSince = now;
        m.punches = 0;
        punchTick(player, m, now);
        return true;
    }

    private static void punchTick(ServerPlayer player, Moves m, long now) {
        if (m.punchSince == 0L) {
            return;
        }
        while (m.punches < PUNCHES && now - m.punchSince >= (long) m.punches * PUNCH_GAP) {
            land(player, m.punches);
            m.punches++;
        }
        if (m.punches >= PUNCHES) {
            m.punchSince = 0L;
        }
    }

    /** One punch of the combo ({@code index} 0-4; the last is the heavy one). */
    @Nullable
    static LivingEntity land(ServerPlayer player, int index) {
        boolean last = index == PUNCHES - 1;
        ServerLevel level = player.serverLevel();
        player.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        LivingEntity target = NanoTech.inFront(player, Math.max(1.0, PUNCH_REACH - player.getBbWidth() * 0.5));
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(2.0));
        if (target != null) {
            float damage = (float) (last ? NanoTech.cfg(IronManConfig.HULKBUSTER_PUNCH_FINAL_DAMAGE, 10.0)
                    : NanoTech.cfg(IronManConfig.HULKBUSTER_PUNCH_DAMAGE, 6.0));
            if (NanoTech.hurt(player, target, ModDamageTypes.HULKBUSTER_PUNCH, damage)) {
                NanoTech.push(target, player.position(), last ? NanoTech.cfg(
                        IronManConfig.HULKBUSTER_PUNCH_FINAL_KNOCKBACK, 2.5) : 0.25, last ? 0.4 : 0.05, player);
            }
            at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        }
        Vec3 fist = NanoTech.palm(player, index % 2 == 0);
        NanoTech.send(level, player, IronFxPayload.PUNCH, List.of(fist, at), index);
        NanoTech.sound(level, at, ModSounds.HULKBUSTER_PUNCH.get(), last ? 1.4F : 1.0F, last ? 0.8F : 1.0F);
        return target;
    }

    // ---- B: the grab and the throw ----

    /** Whether the creature may be grabbed: no boss, nothing grab-immune or wider than 3 blocks. */
    public static boolean grabbable(LivingEntity target) {
        return !NanoTech.isBoss(target) && !target.getType().is(ModTags.GRAB_IMMUNE)
                && target.getBbWidth() <= GRAB_MAX_WIDTH;
    }

    /** B: he grabs the creature in front. */
    @Nullable
    public static LivingEntity grab(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.IRON_MAN_SHIELD, now)) {
            return null;
        }
        double reach = NanoTech.cfg(IronManConfig.HULKBUSTER_GRAB_RANGE, 5.0);
        LivingEntity target = NanoTech.inFront(player, Math.max(1.0, reach - player.getBbWidth() * 0.5));
        if (target == null || !grabbable(target) || target.isPassenger() || target.isVehicle()) {
            return null;
        }
        Moves m = moves(player);
        m.grabbed = target.getId();
        m.grabSince = now;
        m.grabbedNoAi = target instanceof Mob mob && mob.isNoAi();
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
        data.set(Live.IRON_GRABBING, true);
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.GRAB, List.of(target.position()), target.getId());
        NanoTech.sound(level, target.position(), ModSounds.HULKBUSTER_GRAB.get(), 1.2F, 1.0F);
        holdAt(player, target);
        return target;
    }

    /** Where the grabbed creature hangs: in his right fist, before him. */
    static Vec3 fist(ServerPlayer player, LivingEntity held) {
        float scale = player.getScale();
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 side = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        return player.position().add(facing.scale(0.55 * scale + held.getBbWidth() * 0.5))
                .add(side.scale(0.25 * scale)).add(0.0, 0.9 * scale - held.getBbHeight() * 0.5, 0.0);
    }

    private static void holdAt(ServerPlayer player, LivingEntity held) {
        Vec3 at = fist(player, held);
        held.teleportTo(at.x, at.y, at.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0.0F;
        held.hurtMarked = true;
    }

    private static void holdTick(ServerPlayer player, PowerData data, Moves m, long now) {
        if (m.grabbed >= 0) {
            Entity e = player.level().getEntity(m.grabbed);
            if (!(e instanceof LivingEntity held) || !held.isAlive()) {
                release(player, data, m);
            } else if (now - m.grabSince >= NanoTech.cfg(IronManConfig.HULKBUSTER_GRAB_MAX_TICKS, 60)) {
                throwIt(player, data);
            } else {
                holdAt(player, held);
            }
        }
    }

    /** The thrown creatures still flying: each that lands (or after {@link #THROW_WATCH_TICKS}) takes its blow. */
    private static void thrownTick(ServerPlayer player, Moves m, long now) {
        Iterator<Thrown> it = m.thrown.iterator();
        while (it.hasNext()) {
            Thrown t = it.next();
            Entity e = player.level().getEntity(t.id());
            if (!(e instanceof LivingEntity flying) || !flying.isAlive()) {
                it.remove();
                continue;
            }
            long age = now - t.at();
            if (age >= 3 && (flying.onGround() || flying.horizontalCollision || flying.isInWater())
                    || age >= THROW_WATCH_TICKS) {
                it.remove();
                if (flying instanceof Mob mob) {
                    mob.setNoAi(t.noAi());
                }
                landed(player, flying);
            }
        }
    }

    /**
     * B again (or the time is up): the creature flies {@code throwDistance} along his look.
     *
     * @return the creature thrown, or null when he held none
     */
    @Nullable
    public static LivingEntity throwIt(ServerPlayer player, PowerData data) {
        Moves m = moves(player);
        Entity e = m.grabbed < 0 ? null : player.level().getEntity(m.grabbed);
        boolean noAi = m.grabbedNoAi;
        m.grabbed = -1;
        data.set(Live.IRON_GRABBING, false);
        if (!(e instanceof LivingEntity held) || !held.isAlive()) {
            return null;
        }
        long now = player.level().getGameTime();
        data.startCooldown(HeroAbility.IRON_MAN_SHIELD, now, NanoTech.cfg(IronManConfig.HULKBUSTER_GRAB_COOLDOWN,
                160));
        if (held instanceof Mob mob) {
            mob.setNoAi(false); // (it flies: a creature without its AI would hang in the air; given back on landing)
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        // About throwDistance blocks of flight from his fist's height (air drag 0.91 a tick, gravity 0.08).
        double speed = NanoTech.cfg(IronManConfig.HULKBUSTER_THROW_DISTANCE, 12.0) / 9.0;
        held.setDeltaMovement(dir.scale(speed).add(0.0, 0.35 + Math.max(0.0, look.y) * 0.5, 0.0));
        held.hurtMarked = true;
        held.hasImpulse = true;
        m.thrown.add(new Thrown(held.getId(), now, noAi));
        player.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.THROW, List.of(held.position()), held.getId());
        NanoTech.sound(level, held.position(), ModSounds.HULKBUSTER_THROW.get(), 1.2F, 1.0F);
        return held;
    }

    /** A thrown creature lands: it takes the throw's damage, and so does what it lands on. */
    static void landed(ServerPlayer player, LivingEntity thrown) {
        float damage = (float) NanoTech.cfg(IronManConfig.HULKBUSTER_THROW_DAMAGE, 10.0);
        NanoTech.hurt(player, thrown, ModDamageTypes.HULKBUSTER_PUNCH, damage);
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, thrown.getBoundingBox()
                .inflate(LANDING_RADIUS), e -> e != thrown && NanoTech.mayHurt(player, e))) {
            NanoTech.hurt(player, e, ModDamageTypes.HULKBUSTER_PUNCH, damage);
        }
        Vec3 at = thrown.position();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 12, 0.6, 0.1, 0.6,
                0.05);
        NanoTech.sound(level, at, ModSounds.HULKBUSTER_PUNCH.get(), 1.2F, 0.7F);
    }

    /** What he holds is let go where it is (the suit stopped, he left). */
    private static void release(ServerPlayer player, PowerData data, Moves m) {
        if (m.grabbed >= 0 && player.level().getEntity(m.grabbed) instanceof Mob mob) {
            mob.setNoAi(m.grabbedNoAi);
        }
        m.grabbed = -1;
        data.set(Live.IRON_GRABBING, false);
    }

    /** The creature he holds now, or null. */
    @Nullable
    public static LivingEntity held(ServerPlayer player) {
        Moves m = MOVES.get(player.getUUID());
        return m == null || m.grabbed < 0 ? null : player.level().getEntity(m.grabbed) instanceof LivingEntity l ? l
                : null;
    }

    static void forget(ServerPlayer player) {
        Moves m = MOVES.remove(player.getUUID());
        if (m == null) {
            return;
        }
        if (m.grabbed >= 0 && player.level().getEntity(m.grabbed) instanceof Mob mob) {
            mob.setNoAi(m.grabbedNoAi);
        }
        for (Thrown t : m.thrown) {
            if (player.level().getEntity(t.id()) instanceof Mob mob) {
                mob.setNoAi(t.noAi());
            }
        }
    }

    static void clear() {
        MOVES.clear();
    }

    /** Every tick of every Hulkbuster wearer, worked or not: Veronica's arm, the thrown creatures' landing. */
    static void wearerTick(ServerPlayer player, PowerData data, long now) {
        Veronica.tick(player, now);
        Moves m = MOVES.get(player.getUUID());
        if (m != null && !m.thrown.isEmpty()) {
            thrownTick(player, m, now);
        }
    }
}
