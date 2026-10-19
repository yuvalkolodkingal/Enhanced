package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.ScentPayload;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * N: the Sniff, and Sneak + N: Track (3.7), his senses. The Sniff: he lifts his head and sniffs, and every living
 * creature within {@code sniffRadius} 48 glows through walls for him for {@code sniffTicks} 200, in its scent's colour
 * (monsters red, animals green, villagers and other folk yellow, players in their hero's colour, a poisoned creature
 * purple); the scent trails of the players and monsters near him (where they walked in the last {@code trailSeconds}
 * 60, sampled every second) drift as faint motes for him alone ({@link ScentPayload}). Cooldown {@code sniffCooldown}
 * 300.
 *
 * <p>Track ({@link Toggle#SCENT_TRACK}): he picks the creature or player he looks at (within {@link #TRACK_PICK}, in
 * sight), or the last one that hurt him within a minute, and for {@code trackTicks} 1200 a scent trail leads from him
 * to it (a mote every 2 blocks, sent every second, through walls), his HUD showing the way and the distance. It ends
 * when the scent is lost (it dies, leaves his dimension, goes farther than {@code trackRange} 256), when the time is up
 * or when he presses again; then the cooldown {@code trackCooldown} 300. A tracked player is told "Wolverine has your
 * scent." ({@code trackTellsTarget}).
 */
public final class Senses {
    /** How far Track may pick its target by sight. */
    public static final double TRACK_PICK = 64.0;
    /** How long ago (ticks) the one who hurt him may be for Track to take it. */
    public static final int LAST_HURT = 1200;
    /** Trails are sampled every this many ticks; a trail point of Track every {@link #TRAIL_STEP} blocks. */
    public static final int SAMPLE = 20;
    public static final double TRAIL_STEP = 2.0;
    /** How far round a Wolverine the trails are recorded. */
    static final double RECORD = 64.0;

    /** Where each player and monster near a Wolverine walked (by entity id), sampled every second. */
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private static final class Trail {
        final ArrayDeque<Vec3> points = new ArrayDeque<>();
        ResourceKey<Level> dimension;
        long lastSample;

        Trail(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    // ---- the Sniff ----

    /** N: the Sniff. Returns what he smelled (null when he could not: the cooldown). */
    @Nullable
    public static List<ScentPayload.Scent> sniff(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.WOLVERINE_SENSES) || !data.isReady(HeroAbility.WOLVERINE_SENSES, now)) {
            return null;
        }
        data.startCooldown(HeroAbility.WOLVERINE_SENSES, now, WolverineAbilities.cfg(WolverineConfig.SNIFF_COOLDOWN,
                300));
        List<LivingEntity> found = smelled(player);
        List<ScentPayload.Scent> scents = new ArrayList<>();
        for (LivingEntity e : found) {
            if (scents.size() >= ScentPayload.MAX_SCENTS) {
                break;
            }
            scents.add(new ScentPayload.Scent(e.getId(), kindOf(e)));
        }
        List<ScentPayload.Trail> trails = new ArrayList<>();
        for (LivingEntity e : found) {
            if (trails.size() >= ScentPayload.MAX_TRAILS) {
                break;
            }
            if (!(e instanceof Player) && !(e instanceof Enemy)) {
                continue;
            }
            List<Vec3> points = trailOf(e);
            if (points.size() >= 2) {
                trails.add(new ScentPayload.Trail(points));
            }
        }
        PacketDistributor.sendToPlayer(player, new ScentPayload(scents, trails,
                WolverineAbilities.cfg(WolverineConfig.SNIFF_TICKS, 200)));
        ServerLevel level = player.serverLevel();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_SNIFF.get(), 0.8F, 1.0F);
        WolverineAbilities.send(level, player, WolverineFxPayload.SNIFF, List.of(player.getEyePosition()),
                scents.size());
        return scents;
    }

    /** Every living creature within the Sniff's radius of him (not himself), the nearest first. */
    public static List<LivingEntity> smelled(ServerPlayer player) {
        double radius = WolverineAbilities.cfg(WolverineConfig.SNIFF_RADIUS, 48);
        List<LivingEntity> found = new ArrayList<>(player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive() && !e.isSpectator()
                        && e.distanceTo(player) <= radius));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return found;
    }

    /** The colour of a creature's scent (a kind of {@link ScentPayload}). */
    public static byte kindOf(LivingEntity e) {
        if (e.hasEffect(MobEffects.POISON)) {
            return ScentPayload.POISONED;
        }
        if (e instanceof Player) {
            return ScentPayload.PLAYER;
        }
        if (e instanceof Enemy) {
            return ScentPayload.MONSTER;
        }
        if (e instanceof Npc || e instanceof AsgardianEntity) {
            return ScentPayload.VILLAGER;
        }
        return ScentPayload.ANIMAL;
    }

    /** The recorded trail of a creature (the oldest point first), at most {@link ScentPayload#MAX_TRAIL_POINTS}. */
    static List<Vec3> trailOf(LivingEntity e) {
        Trail trail = TRAILS.get(e.getId());
        if (trail == null || trail.dimension != e.level().dimension()) {
            return List.of();
        }
        List<Vec3> all = new ArrayList<>(trail.points);
        all.add(e.position());
        int max = ScentPayload.MAX_TRAIL_POINTS;
        if (all.size() <= max) {
            return all;
        }
        List<Vec3> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            out.add(all.get((int) Math.round(i * (all.size() - 1) / (double) (max - 1))));
        }
        return out;
    }

    // ---- Track ----

    /** Sneak + N: Track begins (on the one he looks at, or the last one that hurt him), or ends. */
    public static boolean toggleTrack(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.WOLVERINE_TRACK)) {
            return false;
        }
        WolverineAbilities.State state = WolverineAbilities.state(player.getUUID());
        if (state.tracked != null) {
            endTrack(player, data, state, true);
            return true;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.WOLVERINE_TRACK, now)) {
            return false;
        }
        LivingEntity target = pick(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.wolverine.no_scent")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        return track(player, data, target);
    }

    /** Track begins on {@code target}. */
    public static boolean track(ServerPlayer player, PowerData data, LivingEntity target) {
        WolverineAbilities.State state = WolverineAbilities.state(player.getUUID());
        long now = player.level().getGameTime();
        state.tracked = target.getUUID();
        state.trackUntil = now + WolverineAbilities.cfg(WolverineConfig.TRACK_TICKS, 1200);
        data.setToggle(Toggle.SCENT_TRACK, true);
        ServerLevel level = player.serverLevel();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_TRACK.get(), 0.8F, 1.0F);
        WolverineAbilities.send(level, player, WolverineFxPayload.TRACK_ON, List.of(target.position()),
                target.getId());
        player.displayClientMessage(Component.translatable("message.enhanced.wolverine.tracking",
                target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        if (target instanceof ServerPlayer prey && WolverineAbilities.cfg(WolverineConfig.TRACK_TELLS_TARGET, true)) {
            prey.displayClientMessage(Component.translatable("message.enhanced.wolverine.tracked")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        follow(player, state, target);
        return true;
    }

    /** The one Track would take: the creature he looks at, or the last one that hurt him lately. */
    @Nullable
    static LivingEntity pick(ServerPlayer player) {
        EntityHitResult hit = Raycasts.entity(player, TRACK_PICK, 0.6, e -> e instanceof LivingEntity living
                && living.isAlive() && !living.isSpectator(), 1.0F);
        if (hit != null && hit.getEntity() instanceof LivingEntity seen && player.hasLineOfSight(seen)) {
            return seen;
        }
        LivingEntity last = player.getLastHurtByMob();
        if (last != null && last.isAlive() && last.level() == player.level()
                && player.tickCount - player.getLastHurtByMobTimestamp() <= LAST_HURT) {
            return last;
        }
        return null;
    }

    /** Track ends: the switch goes off, and (unless the power ends) the cooldown starts. */
    static void endTrack(ServerPlayer player, PowerData data, WolverineAbilities.State state, boolean cooldown) {
        if (state.tracked == null) {
            data.setToggle(Toggle.SCENT_TRACK, false);
            return;
        }
        state.tracked = null;
        state.trackUntil = 0;
        state.lastTrail = List.of();
        data.setToggle(Toggle.SCENT_TRACK, false);
        WolverineAbilities.send(player.serverLevel(), player, WolverineFxPayload.TRACK_OFF, List.of(player.position()),
                0);
        if (cooldown) {
            data.startCooldown(HeroAbility.WOLVERINE_TRACK, player.level().getGameTime(),
                    WolverineAbilities.cfg(WolverineConfig.TRACK_COOLDOWN, 300));
        }
    }

    /** Sends him the trail to the tracked one: a point every {@link #TRAIL_STEP} blocks, the target's place last. */
    private static void follow(ServerPlayer player, WolverineAbilities.State state, LivingEntity target) {
        Vec3 from = player.position().add(0, 0.2, 0);
        Vec3 to = target.position().add(0, 0.2, 0);
        Vec3 way = to.subtract(from);
        double distance = way.length();
        List<Vec3> points = new ArrayList<>();
        if (distance > 1.0E-3) {
            Vec3 step = way.scale(TRAIL_STEP / distance);
            for (int i = 1; i < ScentPayload.MAX_TRAIL_POINTS && i * TRAIL_STEP < distance; i++) {
                points.add(from.add(step.scale(i)));
            }
        }
        points.add(to);
        state.lastTrail = List.copyOf(points);
        PacketDistributor.sendToPlayer(player, new ScentPayload(
                List.of(new ScentPayload.Scent(target.getId(), ScentPayload.TRACKED)),
                List.of(new ScentPayload.Trail(state.lastTrail)), SAMPLE + 10));
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data, WolverineAbilities.State state, long now) {
        if (data.has(HeroAbility.WOLVERINE_SENSES) && now % SAMPLE == 0) {
            record(player, now);
        }
        if (state.tracked == null) {
            if (data.toggle(Toggle.SCENT_TRACK)) {
                data.setToggle(Toggle.SCENT_TRACK, false); // (a switch left on by a login: there is no scent)
            }
            return;
        }
        Entity found = player.serverLevel().getEntity(state.tracked);
        int range = WolverineAbilities.cfg(WolverineConfig.TRACK_RANGE, 256);
        if (!(found instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                || target.distanceTo(player) > range) {
            player.displayClientMessage(Component.translatable("message.enhanced.wolverine.scent_lost")
                    .withStyle(ChatFormatting.GRAY), true);
            endTrack(player, data, state, true);
            return;
        }
        if (now >= state.trackUntil) {
            endTrack(player, data, state, true);
            return;
        }
        if ((now - state.trackUntil) % SAMPLE == 0) {
            follow(player, state, target);
        }
    }

    /** Samples where the players and monsters round him are (each at most once a second, whoever samples it). */
    private static void record(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();
        int keep = WolverineAbilities.cfg(WolverineConfig.TRAIL_SECONDS, 60);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RECORD),
                e -> e.isAlive() && (e instanceof Player || e instanceof Enemy))) {
            Trail trail = TRAILS.computeIfAbsent(e.getId(), k -> new Trail(level.dimension()));
            if (trail.dimension != level.dimension()) {
                trail.points.clear();
                trail.dimension = level.dimension();
            }
            if (trail.lastSample == now) {
                continue;
            }
            trail.lastSample = now;
            Vec3 last = trail.points.peekLast();
            if (last == null || last.distanceToSqr(e.position()) > 0.25) {
                trail.points.addLast(e.position());
            }
            while (trail.points.size() > keep) {
                trail.points.removeFirst();
            }
        }
        if (now % 600 == 0) {
            long stale = (long) keep * SAMPLE + 200;
            Iterator<Map.Entry<Integer, Trail>> it = TRAILS.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue().lastSample > stale) {
                    it.remove();
                }
            }
        }
    }

    /** Whom the player is tracking (null: nobody). */
    @Nullable
    public static UUID tracked(Player player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        return state == null ? null : state.tracked;
    }

    /** The last trail Track sent him (for the tests). */
    public static List<Vec3> lastTrail(Player player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        return state == null ? List.of() : state.lastTrail;
    }

    static void clear() {
        TRAILS.clear();
    }

    private Senses() {
    }
}
