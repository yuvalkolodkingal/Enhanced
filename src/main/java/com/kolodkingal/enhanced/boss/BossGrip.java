package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
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
 * <p>{@link #refusal} is the one answer to "may this telekinesis take hold of it now?" that {@code Telekinesis.canLift}
 * and the press's refusal ask. It is a pure query: it changes, plays and says nothing, because every press's raycast asks
 * it of every entity in the ray's box, and every tick of a grip asks it again.
 */
public final class BossGrip {

    public static final String UNTOUCHABLE = "message.enhanced.telekinesis.boss_untouchable";
    public static final String TOO_POWERFUL = "message.enhanced.telekinesis.boss_too_powerful";
    public static final String REGRIP = "message.enhanced.telekinesis.boss_regrip";

    /** Boss UUID → the game time from which telekinesis may take hold of him again. */
    private static final Map<UUID, Long> REGRIP_UNTIL = new HashMap<>();

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
            if ((e instanceof WitherBoss wither && wither.getInvulnerableTicks() > 0)
                    || (e instanceof Warden warden && (warden.hasPose(Pose.DIGGING) || warden.hasPose(Pose.EMERGING)))) {
                return UNTOUCHABLE; // the Wither gathering his strength, the Warden in the ground (its own check is private)
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

    /** Whether {@code e} is the boss of a running fight (Ultron's body, Thanos, Apocalypse), not one of his minions. */
    public static boolean isFightBoss(Entity e) {
        return e.getType().is(ModTags.BOSSES) && BossFights.fightOf(e) != null;
    }

    /** Whether {@code e} is a vanilla boss held as a boss (the Wither, the Warden, the Elder Guardian, R37). */
    public static boolean isVanillaBoss(Entity e) {
        return e.getType().is(ModTags.HELD_AS_BOSSES);
    }

    /** How many ticks are left before telekinesis may take hold of this boss again (0: it may). */
    public static long regripLeft(Entity e) {
        Long until = REGRIP_UNTIL.get(e.getUUID());
        return until == null ? 0 : Math.max(0, until - e.level().getGameTime());
    }

    /** Forgets every boss's regrip time: for the server's stop and for tests. */
    public static void clear() {
        REGRIP_UNTIL.clear();
    }

    private BossGrip() {
    }
}
