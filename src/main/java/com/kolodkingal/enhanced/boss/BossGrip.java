package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A boss in the grip of telekinesis (3.1, D-T 4): the three villains of the fights, and the Wither, the Warden and the
 * Elder Guardian ({@code enhanced:held_as_bosses}, which have no fight, R37), are held <em>as bosses</em> by every form, for
 * a short time, and then nobody holds them again for {@code boss.regripTicks}.
 *
 * <ul>
 *     <li><b>Who may take him.</b> {@link #refusal} is the one answer to "may this telekinesis take hold of it now?" that
 *     {@code Telekinesis.canLift} and the press's refusal ask. It is a pure query: it changes, plays and says nothing,
 *     because every press's raycast asks it of every entity in the ray's box, and every tick of a grip asks the boss's
 *     part of it again ({@link #slips}).</li>
 *     <li><b>How long.</b> {@link #holdLimit}: his fight's {@code holdTicks} (a vanilla boss {@code boss.vanillaHoldTicks})
 *     times the form's factor; {@link #BREAKING_TICKS} before it the holder is told he is breaking free ({@link
 *     #struggle}), and at it he breaks free ({@link #breakFree}).</li>
 *     <li><b>Then nobody, for a while.</b> {@link #letGo} starts his regrip time when the grip ends, counted from when he
 *     can act again: a thrown boss is still for {@code boss.thrownStillTicks} of his flight ({@link #stills}), and his
 *     regrip time starts after it.</li>
 * </ul>
 *
 * <p>Everything here is the server thread's, and forgotten when the server stops ({@link #clear}).
 */
public final class BossGrip {

    public static final String GRABBED = "message.enhanced.telekinesis.boss_grabbed";
    public static final String BREAKING = "message.enhanced.telekinesis.boss_breaking";
    public static final String BROKE_FREE = "message.enhanced.telekinesis.boss_broke_free";
    public static final String UNTOUCHABLE = "message.enhanced.telekinesis.boss_untouchable";
    public static final String TOO_POWERFUL = "message.enhanced.telekinesis.boss_too_powerful";
    public static final String SHIELD = "message.enhanced.telekinesis.boss_shield";
    public static final String REGRIP = "message.enhanced.telekinesis.boss_regrip";

    /** A boss's hold limit is never shorter than this, whatever the factors say. */
    public static final int MIN_HOLD_TICKS = 10;
    /** The holder is told the boss is breaking free this many ticks before the limit (a limit over 30 only). */
    public static final int BREAKING_TICKS = 20;

    /** Boss UUID → the game time from which telekinesis may take hold of him again. */
    private static final Map<UUID, Long> REGRIP_UNTIL = new HashMap<>();
    /** Boss UUID → the game time until which a throw keeps him still. */
    private static final Map<UUID, Long> STILL_UNTIL = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Who may take him

    /**
     * Why telekinesis in {@code mode} may not take hold of {@code e} now: the key of the line its holder reads, or null if
     * nothing about bosses stands in the way (D-T 4.2, spec 5.2 refusal step 5), the first that applies of:
     * <ol>
     *     <li>whatever a fight holds where it is (the Sokovia lift carries it) cannot be taken: {@link #UNTOUCHABLE};</li>
     *     <li>a fight's boss with {@code boss.telekinesisHoldsBosses} off, or a vanilla boss with {@code
     *     boss.telekinesisHoldsVanillaBosses} off: {@link #TOO_POWERFUL};</li>
     *     <li>the boss's own answer ({@link BossHooks#gripRefusal}: an invulnerable state, Apocalypse's shield against a hero
     *     who is not mind power); for a vanilla boss, a Wither whose spawn invulnerability runs or a Warden digging or
     *     emerging: {@link #UNTOUCHABLE};</li>
     *     <li>his regrip time ({@link #regripLeft}): {@link #REGRIP}, for every player.</li>
     * </ol>
     * Whether the form could take {@code e} at all (Magneto only metal) is not asked here: the caller asks it first.
     */
    @Nullable
    public static String refusal(Player holder, Entity e, Telekinesis.Mode mode) {
        if (BossFights.isLocked(e)) {
            return UNTOUCHABLE;
        }
        if (isVanillaBoss(e)) {
            if (!BossConfig.TELEKINESIS_HOLDS_VANILLA_BOSSES.get()) {
                return TOO_POWERFUL;
            }
            if (vanillaUntouchable(e)) {
                return UNTOUCHABLE;
            }
            return regripLeft(e) > 0 ? REGRIP : null;
        }
        if (isFightBoss(e) && !BossConfig.TELEKINESIS_HOLDS_BOSSES.get()) {
            return TOO_POWERFUL;
        }
        if (e instanceof BossHooks hooks && holder instanceof ServerPlayer player) {
            String refused = hooks.gripRefusal(player, mode);
            if (refused != null) {
                return refused;
            }
        }
        return regripLeft(e) > 0 ? REGRIP : null;
    }

    /** A Wither gathering his strength, or a Warden in the ground (its own check is package-private). */
    private static boolean vanillaUntouchable(Entity e) {
        return (e instanceof WitherBoss wither && wither.getInvulnerableTicks() > 0)
                || (e instanceof Warden warden && (warden.hasPose(Pose.DIGGING) || warden.hasPose(Pose.EMERGING)));
    }

    /**
     * Whether a grip on {@code e} slips this tick (D-T 4.3): the lift locks it, or it became untouchable (its own {@link
     * BossHooks#gripRefusal} is no longer null: Ultron's lift warning or a transfer, Apocalypse's set piece or his shield
     * re-forming, Thanos's death; a vanilla boss's invulnerable state). The grip then ends at once, with no message beyond
     * what the fight shows. The switches and the regrip time are not asked: they only refuse a new grip.
     */
    public static boolean slips(ServerPlayer holder, Entity e, Telekinesis.Mode mode) {
        if (BossFights.isLocked(e)) {
            return true;
        }
        if (isVanillaBoss(e)) {
            return vanillaUntouchable(e);
        }
        return e instanceof BossHooks hooks && hooks.gripRefusal(holder, mode) != null;
    }

    /** Whether {@code e} is the boss of a running fight (Ultron's body, Thanos, Apocalypse), not one of his minions. */
    public static boolean isFightBoss(Entity e) {
        return e.getType().is(ModTags.BOSSES) && BossFights.fightOf(e) != null;
    }

    /** Whether {@code e} is a vanilla boss held as a boss (the Wither, the Warden, the Elder Guardian, R37). */
    public static boolean isVanillaBoss(Entity e) {
        return e.getType().is(ModTags.HELD_AS_BOSSES);
    }

    /** Whether {@code e} is held as a boss: a fight's boss or a vanilla boss. They have a regrip time; their shots do not. */
    public static boolean isBoss(Entity e) {
        return isFightBoss(e) || isVanillaBoss(e);
    }

    // ------------------------------------------------------------------------------------------------------------
    // How long

    /**
     * How many ticks telekinesis in {@code mode} may hold {@code e} before it tears free (0: no limit). A boss: his fight's
     * {@code holdTicks} (asked through {@link BossHooks#holdTicks}; a vanilla boss {@code boss.vanillaHoldTicks}) times the
     * form's factor ({@code boss.billyHoldFactor}, {@code boss.magnetoHoldFactor}, 1 for Wanda and the Phoenix), never under
     * {@link #MIN_HOLD_TICKS}. A shot of a fight with a limit of its own (Thanos's sword, {@code swordHoldTicks}): its
     * limit, times Magneto's factor when he holds it; Billy's factor is for the bosses only (R8).
     */
    public static int holdLimit(Entity e, Telekinesis.Mode mode) {
        if (isVanillaBoss(e)) {
            return bossLimit(BossConfig.VANILLA_HOLD_TICKS.get(), mode);
        }
        if (!(e instanceof BossHooks hooks) || hooks.holdTicks() <= 0) {
            return 0;
        }
        if (isFightBoss(e)) {
            return bossLimit(hooks.holdTicks(), mode);
        }
        double factor = mode == Telekinesis.Mode.MAGNETO ? BossConfig.MAGNETO_HOLD_FACTOR.get() : 1.0;
        return Math.max(1, (int) Math.round(hooks.holdTicks() * factor));
    }

    private static int bossLimit(int ticks, Telekinesis.Mode mode) {
        double factor = switch (mode) {
            case BILLY -> BossConfig.BILLY_HOLD_FACTOR.get();
            case MAGNETO -> BossConfig.MAGNETO_HOLD_FACTOR.get();
            case WANDA, JEAN -> 1.0;
        };
        return Math.max(MIN_HOLD_TICKS, (int) Math.round(ticks * factor));
    }

    /**
     * Telekinesis just took hold of {@code e}: it drops what it was doing ({@link BossHooks#onGripped}), and the holder of a
     * boss reads that he is in the grip ({@link #GRABBED}).
     */
    public static void gripped(ServerPlayer holder, Entity e) {
        if (e instanceof BossHooks hooks) {
            hooks.onGripped(holder);
        }
        if (isBoss(e)) {
            holder.displayClientMessage(Component.translatable(GRABBED, e.getName()), true);
        }
    }

    /**
     * One tick of a boss's struggle in the grip, {@code ticksHeld} into a hold of {@code limit}: when the limit is over 30,
     * {@link #BREAKING_TICKS} before it the holder reads that he is breaking free (once), and every 5 ticks from then six
     * sparks burst round him for everyone near.
     */
    public static void struggle(ServerPlayer holder, Entity e, int ticksHeld, int limit) {
        if (!isBoss(e) || limit <= 30) {
            return;
        }
        int left = limit - ticksHeld;
        if (left == BREAKING_TICKS) {
            holder.displayClientMessage(Component.translatable(BREAKING, e.getName()), true);
        }
        if (left > 0 && left <= BREAKING_TICKS && left % 5 == 0 && e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 6,
                    e.getBbWidth() * 0.6, e.getBbHeight() * 0.4, e.getBbWidth() * 0.6, 0.2);
        }
    }

    /**
     * {@code e} tore free at the end of its hold (the grip has already ended, see {@link #letGo}). A boss breaks it with a
     * crack ({@code SHIELD_BREAK}, low) and a burst of the holder's colour thrown outward from him, and the holder reads
     * that he broke free; a shot only flies off (its own {@link BossHooks#onLetGo} says so).
     */
    public static void breakFree(ServerPlayer holder, Entity e, int color) {
        if (!isBoss(e) || !(e.level() instanceof ServerLevel level)) {
            return;
        }
        double y = e.getY() + e.getBbHeight() * 0.5;
        level.playSound(null, e.getX(), y, e.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
        RandomSource random = e.getRandom();
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double up = random.nextDouble() * 0.6 - 0.2;
            // count 0: the "spread" is the particle's own velocity, so each spark flies outward on its own line
            level.sendParticles(new EnergyParticleOptions(color, 1.4F), e.getX(), y, e.getZ(), 0,
                    Math.cos(angle), up, Math.sin(angle), 0.5);
        }
        holder.displayClientMessage(Component.translatable(BROKE_FREE, e.getName()), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Then nobody, for a while

    /**
     * A grip on {@code e} ended: set down, slipped, lost, the holder gone, broke free ({@code brokeFree}), or thrown
     * ({@code thrown}). A boss's regrip time starts when he can act again (D-T 4.3): thrown, he is still for {@code
     * boss.thrownStillTicks} and nobody holds him for {@code boss.regripTicks} after that; otherwise nobody holds him for
     * {@code boss.regripTicks} from now. A grip that ended without a throw tells the entity ({@link BossHooks#onLetGo}: the
     * sword flies home).
     */
    public static void letGo(ServerPlayer holder, Entity e, boolean thrown, boolean brokeFree) {
        long now = e.level().getGameTime();
        if (isBoss(e)) {
            REGRIP_UNTIL.values().removeIf(until -> until <= now);
            STILL_UNTIL.values().removeIf(until -> until <= now);
            int still = thrown ? BossConfig.THROWN_STILL_TICKS.get() : 0;
            if (still > 0) {
                STILL_UNTIL.put(e.getUUID(), now + still);
            } else {
                STILL_UNTIL.remove(e.getUUID());
            }
            REGRIP_UNTIL.put(e.getUUID(), now + still + BossConfig.REGRIP_TICKS.get());
        }
        if (!thrown && e instanceof BossHooks hooks) {
            hooks.onLetGo(holder, brokeFree);
        }
    }

    /** How many ticks are left before telekinesis may take hold of this boss again (0: it may). */
    public static long regripLeft(Entity e) {
        Long until = REGRIP_UNTIL.get(e.getUUID());
        return until == null ? 0 : Math.max(0, until - e.level().getGameTime());
    }

    /**
     * Whether {@code e} does nothing now (D-T 4.4): telekinesis holds it, or it was thrown less than {@code
     * boss.thrownStillTicks} ago and its flight has not ended. A boss asks it every tick before he acts.
     */
    public static boolean stills(Entity e) {
        if (Telekinesis.isHeld(e)) {
            return true;
        }
        Long until = STILL_UNTIL.get(e.getUUID());
        return until != null && e.level().getGameTime() < until;
    }

    /** The flight of a throw ended (it landed, slammed or was stopped): he may act again at once. */
    public static void landed(Entity e) {
        STILL_UNTIL.remove(e.getUUID());
    }

    /** {@code e} is dying (Thanos's long death): no still and no flight of a throw; nothing may pause it. */
    public static void forget(Entity e) {
        STILL_UNTIL.remove(e.getUUID());
        Telekinesis.forgetFlight(e);
    }

    /** Forgets every boss's regrip and still: for the server's stop and for tests. */
    public static void clear() {
        REGRIP_UNTIL.clear();
        STILL_UNTIL.clear();
    }

    private BossGrip() {
    }
}
