package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.ability.Telekinesis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The ways the heroes reach a boss or one of its minions without knowing which boss it is: implemented by the boss and
 * minion <em>entities</em>, called from one line in each hero's code. Every hook does what a creature without the
 * interface would get by default.
 */
public interface BossHooks {
    /**
     * How many times slower Quicksilver Time makes this entity, given what the zone does to everything ({@code
     * zoneFactor}, the config's slow-motion factor): 0 means not slowed at all, 1 not slowed more than ordinary time.
     */
    default int slowFactor(int zoneFactor) {
        return zoneFactor;
    }

    /**
     * Magneto's Armor & Weapon Strip reached this entity.
     *
     * @return true if it was handled here and the normal strip must be skipped
     */
    default boolean onMagnetoStrip(ServerPlayer magneto) {
        return false;
    }

    /** A Spider-Man's web shot reached this web-immune entity (Thanos locks the Gauntlet). */
    default void onWebShot(ServerPlayer spider) {
    }

    /** Lightning reached this entity (Apocalypse's Escape is interrupted, a Sentry is stunned). */
    default void onLightning(@Nullable ServerPlayer thor, float amount) {
    }

    // ---- telekinesis (3.1): a boss is held as a boss (D-T 4) ----

    /**
     * Why telekinesis in {@code mode} may not take hold of this entity now: the key of the message its holder reads (an
     * invulnerable state: {@code message.enhanced.telekinesis.boss_untouchable}; Apocalypse's shield against a hero who is
     * not mind power: {@code boss_shield}), or null if it may. A pure query: it changes, plays and says nothing, because
     * every press's raycast and every tick of a grip ask it ({@link #onGripRefused} shows a refusal, once).
     */
    @Nullable
    default String gripRefusal(ServerPlayer holder, Telekinesis.Mode mode) {
        return null;
    }

    /**
     * A press of telekinesis at this entity was refused with {@code key} ({@link #gripRefusal}'s answer): what the refusal
     * shows, once per press (Apocalypse's golden ripple for {@code boss_shield}).
     */
    default void onGripRefused(ServerPlayer holder, String key) {
    }

    /**
     * How long telekinesis may hold this entity before it tears free, in ticks, before the form's factor (Thanos's sword:
     * {@code thanos_boss.swordHoldTicks}; a boss answers his fight's {@code holdTicks}). 0: no limit of its own.
     */
    default int holdTicks() {
        return 0;
    }

    /**
     * Telekinesis took hold of this entity: it drops what it was doing (Ultron his attack, Thanos his move, Apocalypse his
     * brain's plan; Thanos's sword stops flying home).
     */
    default void onGripped(ServerPlayer holder) {
    }

    /**
     * A grip on this entity ended without a throw: set down, slipped, lost, the holder gone, or it tore free at its hold
     * limit ({@code brokeFree}). Thanos's sword flies home; a meteor or an orb fizzles out.
     */
    default void onLetGo(ServerPlayer holder, boolean brokeFree) {
    }

    /**
     * Whether this entity is metal to Magneto (Ultron's vibranium, Thanos's gold armour and the Gauntlet, Apocalypse while
     * his armour stands, Thanos's sword): {@code Metal.isMagnetic} and {@code Metal.isMetalBody} ask it.
     */
    default boolean isMetal() {
        return false;
    }

    /**
     * Its holder throws this entity with {@code velocity}: true if it handled the throw itself (Thanos's sword, meteor or
     * orb turns on its maker), and the ordinary flight is skipped.
     */
    default boolean throwBack(ServerPlayer thrower, Vec3 velocity) {
        return false;
    }
}
