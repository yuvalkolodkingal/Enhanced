package com.kolodkingal.enhanced.client.render;

/**
 * Every one-shot gesture of the heroes: a short timeline of poses played once by an edge in the synced data (a take-off, a
 * landing, a cast), blended over the poses below it. The timing of each is here; its keys are in {@link Gestures}
 * (a gesture without keys there does nothing, so the list can grow ahead of the poses).
 *
 * <p>A gesture's weight rises with an ease-out cubic over {@link #attack} ticks, holds, and falls with an ease-in-out
 * sine over the last {@link #release} ticks of {@link #dur}. At most one gesture of each {@link Group} plays at a time:
 * starting another in the same group fades the first out over 3 ticks. A {@link #force} gesture is not muted by the swing
 * restore (it replaces the vanilla punch).
 */
public enum GestureId {
    LOOK_CHANGE(Group.CAST, 10, 2, 5, false),

    TAKEOFF_MAGNETO(Group.TAKEOFF, 18, 2, 8, true),
    TAKEOFF_WANDA(Group.TAKEOFF, 20, 2, 8, true),
    TAKEOFF_VISION(Group.TAKEOFF, 14, 2, 6, true),
    TAKEOFF_BILLY(Group.TAKEOFF, 22, 3, 8, true),
    TAKEOFF_THOR(Group.TAKEOFF, 28, 2, 8, true),

    BRAKE(Group.BRAKE, 12, 2, 6, false),
    BRAKE_THOR(Group.BRAKE, 12, 2, 6, true),

    LAND_SOFT(Group.LAND, 12, 1, 5, false),
    LAND_HERO(Group.LAND, 26, 2, 9, true),
    /** The arrival of the Bifrost: the same landing, the weapon planted (bare fists to the ground without one). */
    THOR_ARRIVAL(Group.LAND, 26, 2, 9, true),

    RUN_SKID(Group.RUN, 10, 1, 5, false),

    /** Telekinesis has a style per hero: the grab and the fling of Magneto, the Scarlet Witch and Billy. */
    TK_GRAB_MAGNETO(Group.TK, 12, 2, 6, false),
    TK_GRAB_WANDA(Group.TK, 12, 2, 6, false),
    TK_GRAB_BILLY(Group.TK, 12, 2, 6, false),
    TK_RELEASE(Group.TK, 9, 1, 6, false),
    TK_FLING_MAGNETO(Group.TK, 10, 1, 5, true),
    TK_FLING_WANDA(Group.TK, 10, 1, 5, true),
    TK_FLING_BILLY(Group.TK, 10, 1, 5, true),

    MAG_SHRAPNEL_START(Group.CAST, 6, 1, 3, false),
    MAG_DISARM(Group.CAST, 14, 1, 6, false),
    MAG_SHIELD_ON(Group.CAST, 18, 3, 7, false),
    MAG_SHIELD_OFF(Group.CAST, 10, 1, 5, false),

    WANDA_BEAM_KICK(Group.CAST, 8, 1, 4, true),
    WANDA_SHIELD_RAISE(Group.CAST, 18, 2, 3, true),
    WANDA_SHIELD_SNAP(Group.CAST, 8, 1, 3, true),
    WANDA_SHIELD_DROP(Group.CAST, 10, 1, 5, false),
    WANDA_HEX_RISE(Group.CAST, 44, 6, 8, false),
    WANDA_HEX_DISSOLVE(Group.CAST, 16, 2, 6, false),
    WANDA_HEX_TWEAK(Group.CAST, 14, 2, 5, false),
    WANDA_HEX_GROW(Group.CAST, 16, 2, 6, false),
    WANDA_HEX_SHRINK(Group.CAST, 16, 2, 6, false),

    QS_GOGGLES(Group.CAST, 16, 2, 6, false),
    QS_START(Group.CAST, 8, 1, 4, false),
    QS_BLITZ(Group.CAST, 12, 1, 4, true),
    QS_SNATCH(Group.CAST, 12, 1, 4, false),
    QS_WALKMAN(Group.CAST, 8, 1, 3, false),

    VISION_FOCUS(Group.CAST, 12, 2, 5, false),
    VISION_PHASE_IN(Group.CAST, 16, 3, 5, false),
    VISION_PHASE_OUT(Group.CAST, 10, 1, 4, false),
    VISION_DENSE_ON(Group.CAST, 14, 2, 5, false),
    VISION_DENSE_OFF(Group.CAST, 8, 1, 4, false),
    VISION_DISGUISE(Group.CAST, 12, 2, 4, false),

    TOMMY_READY(Group.CAST, 8, 1, 3, false),
    TOMMY_WHIRL_START(Group.CAST, 6, 1, 3, false),
    TOMMY_BLITZ(Group.CAST, 34, 2, 8, true),
    TOMMY_VIBRATE(Group.CAST, 10, 1, 4, false),
    TOMMY_ACCEL(Group.CAST, 12, 1, 5, false),

    BILLY_BOLT(Group.CAST, 8, 1, 5, true),
    BILLY_FREEZE_CAST(Group.CAST, 22, 3, 6, false),
    BILLY_NOISE_ON(Group.CAST, 14, 2, 5, false),
    BILLY_NOISE_OFF(Group.CAST, 8, 1, 3, false),

    LOKI_DAGGER(Group.CAST, 8, 1, 4, true),
    LOKI_FAN(Group.CAST, 12, 1, 5, true),
    LOKI_CAST(Group.CAST, 14, 3, 5, false),
    LOKI_FROST_ON(Group.CAST, 20, 3, 6, false),
    LOKI_FROST_OFF(Group.CAST, 12, 1, 5, false),

    THOR_THROW(Group.THOR_ARM, 12, 1, 6, true),
    THOR_SUMMON(Group.THOR_ARM, 10, 2, 5, false),
    THOR_RECALL_DRAW(Group.THOR_ARM, 8, 1, 4, true),
    THOR_CATCH(Group.THOR_ARM, 14, 1, 6, true),
    /** The kick of the lightning beam with the weapon in a hand (strength 1.7) and bare-handed (strength 1). */
    THOR_BEAM_KICK(Group.THOR_ARM, 8, 1, 5, true),
    THOR_BEAM_KICK_BARE(Group.THOR_ARM, 8, 1, 5, true),
    THOR_UNLEASH_ON(Group.CAST, 20, 2, 6, false),
    THOR_UNLEASH_OFF(Group.CAST, 10, 1, 5, false),
    /** Mjolnir bursts into fragments from his raised hand (its weapon "in flight" is a fragment). */
    THOR_SHATTER(Group.THOR_ARM, 22, 2, 8, true),
    /** The fragments come together in his hand. */
    THOR_REFORM(Group.THOR_ARM, 18, 3, 6, false),
    /** He brings Mjolnir down on someone and pins them (started by the server's payload). */
    THOR_PIN(Group.THOR_ARM, 20, 2, 8, true),

    // The gestures of 3.0, in one region per hero (the // ===== lines), each lane's own. A gesture needs its keys in
    // the hero's HeroClient.registerGestures and a route in GestureRoutes (or a line in startedElsewhere).
    // ===== phoenix =====
    /** Phoenix: the wings unfurl, the arms thrown overhead and swept down and out to the hover. */
    TAKEOFF_PHOENIX(Group.TAKEOFF, 24, 3, 8, true),
    /** Phoenix: telekinesis takes hold, an open palm at the thing and the other hand raised a little. */
    TK_GRAB_PHOENIX(Group.TK, 12, 2, 6, false),
    TK_FLING_PHOENIX(Group.TK, 10, 1, 5, true),
    /** Phoenix: two fingers to the temple as Mind Reading begins, a small shake of the head as it ends. */
    PHOENIX_READ_ON(Group.CAST, 14, 2, 5, false),
    PHOENIX_READ_OFF(Group.CAST, 8, 1, 3, false),
    /** Phoenix: Mind Sense, both hands to the temples and then the arms sweep out like a ripple. */
    PHOENIX_SENSE(Group.CAST, 20, 2, 6, false),
    /** Phoenix: Mind Control, fingers to the temple, the host held in the mind. */
    PHOENIX_MIND_CAST(Group.CAST, 16, 2, 6, false),
    /** Phoenix: the Psionic Field, both arms up and then thrown out and down with a push. */
    PHOENIX_PSIONIC_CAST(Group.CAST, 24, 2, 8, true),
    /** Phoenix: the Burst gathers, the arms sweep out and up into the V of the charge. */
    PHOENIX_CHARGE_START(Group.CAST, 14, 2, 5, false),
    /** Phoenix: the Burst goes off, the scream, the V thrown wide open and the back arched (started by the payload). */
    PHOENIX_BURST(Group.CAST, 24, 1, 9, true),

    // ===== spider =====

    // ===== thanos =====

    // ===== ultron =====

    // ===== apocalypse =====
    ;

    /** Gestures of one group never play together. */
    public enum Group {
        TAKEOFF, LAND, BRAKE, TK, CAST, THOR_ARM, RUN
    }

    public final Group group;
    /** Total length in ticks, and the attack and release ticks at its two ends. */
    public final int dur;
    public final int attack;
    public final int release;
    /** Not muted by the swing restore. */
    public final boolean force;

    GestureId(Group group, int dur, int attack, int release, boolean force) {
        this.group = group;
        this.dur = dur;
        this.attack = attack;
        this.release = release;
        this.force = force;
    }
}
