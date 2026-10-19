package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Time Stone's Rewind and the history it goes back into (3.6): moved out of {@code ThanosAbilities} by the 3.6
 * skeleton, behaviour-neutral (Thanos's N calls {@link #rewind} with his numbers; his tick calls {@link #remember}), so
 * that lane L2 can keep the same history for every player who carries a usable Time Stone or the Eye of Agamotto. A
 * snapshot every {@link #SNAPSHOT_EVERY} ticks, for as long as the window. Server only; per player; it dies with his
 * session.
 */
public final class TimeRewind {
    /** A snapshot of him is kept this often (ticks): the Rewind goes back to the oldest one the window holds. */
    public static final int SNAPSHOT_EVERY = 5;
    /** The Rewind needs at least this much history (ticks), two seconds, or there is nothing to undo. */
    public static final int MIN_HISTORY = 40;

    /** The harmful effects the Rewind takes off him if they began in the window. */
    public static final List<Holder<MobEffect>> UNDONE = List.of(MobEffects.POISON, MobEffects.WITHER,
            MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.CONFUSION, MobEffects.BLINDNESS,
            MobEffects.DARKNESS, MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN, MobEffects.LEVITATION,
            ModEffects.PETRIFIED, ModEffects.TIME_LOCKED, ModEffects.SOULWORLD);

    /** Where he was: a point of the history the Rewind goes back into. */
    public record Snapshot(long time, ResourceKey<Level> dimension, Vec3 position, float health,
            Set<Holder<MobEffect>> effects) {
    }

    /**
     * What a Rewind costs and gives: the row whose cooldown it starts, the cooldown (ticks), the hunger, the most
     * health it gives back, the colour of its rings. (Thanos's are {@code ThanosConfig}'s; the stone's {@code
     * [time_stone]}'s.)
     */
    public record Cost(HeroAbility row, int cooldown, float exhaustion, float maxHeal, int color) {
    }

    /** Player -> the history he can go back into. Server thread only. */
    private static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();

    private static Deque<Snapshot> history(ServerPlayer player) {
        return HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
    }

    /** Keeps the history the Rewind goes back into: a snapshot every few ticks, for as long as {@code windowTicks}. */
    public static void remember(ServerPlayer player, long now, long windowTicks) {
        Deque<Snapshot> history = history(player);
        Snapshot last = history.peekLast();
        if (last != null && now - last.time() < SNAPSHOT_EVERY) {
            return;
        }
        history.addLast(snapshot(player, now));
        while (history.size() > 1 && now - history.peekFirst().time() > windowTicks + SNAPSHOT_EVERY) {
            history.removeFirst();
        }
    }

    public static Snapshot snapshot(ServerPlayer player, long time) {
        Set<Holder<MobEffect>> had = new HashSet<>();
        for (Holder<MobEffect> effect : UNDONE) {
            if (player.hasEffect(effect)) {
                had.add(effect);
            }
        }
        return new Snapshot(time, player.level().dimension(), player.position(), player.getHealth(), had);
    }

    /**
     * How much history the Rewind has to go back into, in ticks (0 if none): the age of the oldest snapshot kept. (The
     * HUD of a client cannot see this; it counts for itself.)
     */
    public static int historyTicks(ServerPlayer player) {
        Deque<Snapshot> history = HISTORY.get(player.getUUID());
        Snapshot oldest = history == null ? null : history.peekFirst();
        return oldest == null ? 0 : (int) (player.level().getGameTime() - oldest.time());
    }

    /**
     * Records the player's present state as if it were {@code ago} ticks old, so that a test can have a history without
     * waiting for one.
     */
    public static void rememberAgo(ServerPlayer player, int ago) {
        Deque<Snapshot> history = history(player);
        long time = player.level().getGameTime() - ago;
        Snapshot snapshot = snapshot(player, time);
        List<Snapshot> all = new ArrayList<>(history);
        all.add(snapshot);
        all.sort((a, b) -> Long.compare(a.time(), b.time()));
        history.clear();
        history.addAll(all);
    }

    /**
     * Back to where he was a few seconds ago, with the health he had then if it was higher (as much as {@code cost}
     * allows), and the harmful effects that began since shed. His food, his things and everyone else's state stay as
     * they are. With under two seconds of history there is nothing to rewind. A Hex wall or the arena of a boss refuses
     * the place (the rest still happens); Mjolnir's pin refuses all of it. (The caller has checked the cooldown and the
     * hunger.)
     */
    public static void rewind(ServerPlayer player, PowerData data, Cost cost) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (MjolnirPin.isPinned(player)) {
            // The pin is not rewound away: a hero Mjolnir lies on goes nowhere, and the whole Rewind is refused.
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return;
        }
        Deque<Snapshot> history = history(player);
        Snapshot past = history.peekFirst();
        if (past == null || now - past.time() < MIN_HISTORY) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.nothing_to_rewind"), true);
            return;
        }
        Vec3 from = player.position();
        Vec3 back = null;
        if (past.dimension() == level.dimension() && from.distanceToSqr(past.position()) > 1.0) {
            back = freeSpotNear(player, past.position());
        }
        if (back != null && !HexManager.onTeleport(level, player, back)) {
            back = null; // (a Hex wall or the arena of a boss refuses the place; the rest of the Rewind still happens)
        }
        data.startCooldown(cost.row(), now, cost.cooldown());
        player.causeFoodExhaustion(cost.exhaustion());
        if (past.health() > player.getHealth()) {
            player.setHealth(Math.min(Math.min(past.health(), player.getHealth() + cost.maxHeal()),
                    player.getMaxHealth()));
        }
        for (Holder<MobEffect> effect : UNDONE) {
            if (player.hasEffect(effect) && !past.effects().contains(effect)) {
                player.removeEffect(effect);
            }
        }
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.resetFallDistance();
        ThanosEffects.mandala(level, from, 2.4F, 30, cost.color());
        if (back != null) {
            moveTo(player, level, back);
            ThanosEffects.mandala(level, back, 2.4F, 30, cost.color());
            ThanosEffects.lineParticles(level, from.add(0.0, 1.0, 0.0), back.add(0.0, 1.0, 0.0), cost.color(), 16,
                    0.8F);
        }
        // The history after that moment is a future that no longer happened.
        history.clear();
        history.addLast(snapshot(player, now));
        ThanosSounds.REWIND.play(level, player.position());
        StoneKind.TIME.voice().use(level, player.position(), 0.6F, 1.0F);
    }

    /** The spot he fell from, or the nearest free spot within three blocks of it; null if there is none. */
    @Nullable
    public static Vec3 freeSpotNear(ServerPlayer player, Vec3 wanted) {
        if (Teleports.fits(player, wanted) && !wet(player.serverLevel(), player, wanted)) {
            return wanted;
        }
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Vec3 candidate = wanted.add(dx, dy, dz);
                    double distance = candidate.distanceToSqr(wanted);
                    if (distance < bestDistance && distance <= 9.0 && Teleports.fits(player, candidate)
                            && !wet(player.serverLevel(), player, candidate)) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    /** Whether the body would stand in a liquid at {@code spot}. */
    private static boolean wet(ServerLevel level, Entity traveller, Vec3 spot) {
        return level.containsAnyLiquid(traveller.getBoundingBox().move(spot.subtract(traveller.position())));
    }

    /** Takes the player to {@code spot}, looking where he looked, off whatever he rode, with the fall forgotten. */
    private static void moveTo(ServerPlayer player, ServerLevel level, Vec3 spot) {
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setYHeadRot(player.getYRot());
        player.resetFallDistance();
    }

    /** His history is gone (his death, session, world or power ended). */
    public static void forget(ServerPlayer player) {
        HISTORY.remove(player.getUUID());
    }

    /** The server stopped. */
    public static void clear() {
        HISTORY.clear();
    }

    private TimeRewind() {
    }
}
