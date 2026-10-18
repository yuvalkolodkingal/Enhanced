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
    /** Magneto's Magnetic Pull (Sneak + R holding nothing): the arms flung wide, then both fists drawn in to the chest. */
    MAG_PULL(Group.CAST, 20, 2, 7, false),

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
    /** Spider-Man: the thwip. The arm snaps to the aim, the wrist flicks, the other arm balances (a Web Shot). */
    SPIDER_SHOOT(Group.CAST, 8, 1, 4, true),
    /** The arms gather across the chest in a crouch, then are flung wide as he straightens (the Web Burst). */
    SPIDER_BURST(Group.CAST, 20, 3, 6, true),
    /** The arm shoots out at the anchor and the body tips toward it; hands over to the stance SPIDER_ZIP. */
    SPIDER_ZIP_START(Group.CAST, 10, 1, 4, true),
    /** The arm is thrown up at the anchor; hands over to the stance SPIDER_SWING. */
    SPIDER_SWING_START(Group.CAST, 10, 1, 4, true),
    /** Both hands up and spread, fingers flexing (Wall Crawl on), and shaken loose again (off). */
    SPIDER_CRAWL_ON(Group.CAST, 8, 1, 3, false),
    SPIDER_CRAWL_OFF(Group.CAST, 6, 1, 3, false),
    /** A finger to the temple: the spider-sense switched on or off. */
    SPIDER_SENSE_TOGGLE(Group.CAST, 8, 1, 3, false),
    /** The sense tingles: the guard goes up; hands over to the stance SPIDER_ALERT. */
    SPIDER_ALERT_START(Group.CAST, 12, 1, 5, false),
    /** The shoulders roll back and the arms open as the Iron Spider's legs unfold, and fold again. */
    SPIDER_LEGS_OUT(Group.CAST, 16, 2, 6, false),
    SPIDER_LEGS_IN(Group.CAST, 10, 1, 4, false),
    /** Instant Kill: the head drops, the arms go to the claw; hands over to the stance SPIDER_KILL. */
    SPIDER_KILL_ON(Group.CAST, 14, 2, 5, false),
    /** The Black Eyes: he clutches his head, shudders and snaps up; hands over to the stance SPIDER_SURGE. */
    SPIDER_SURGE_ON(Group.CAST, 22, 2, 8, false),
    /** The superhero landing after a fast fall: a fist and a toe to the ground (the animator's own edge starts it). */
    SPIDER_LAND(Group.LAND, 22, 2, 8, true),
    /** He pushes off a wall or a ceiling (started by the animator's own edge). */
    SPIDER_LEAP(Group.CAST, 10, 1, 4, true),
    /** A zip ends on a wall and the hands grab it (started by the animator's own edge). */
    SPIDER_ARRIVE(Group.CAST, 12, 1, 5, true),

    // ===== thanos =====
    /** Thanos: the Power Blast, the Gauntlet thrust at the aim (R). */
    THANOS_BLAST(Group.CAST, 10, 1, 5, true),
    /** Thanos: the Titan Slam, the Gauntlet raised overhead and driven into the ground (Sneak + R). */
    THANOS_SLAM(Group.CAST, 22, 2, 8, true),
    /** Thanos: the Portal Step, the Gauntlet draws a circle in the air (G). */
    THANOS_STEP(Group.CAST, 12, 1, 5, false),
    /** Thanos: the Space Pull, the palm forward and then dragged back (Sneak + G). */
    THANOS_PULL(Group.CAST, 16, 2, 6, true),
    /** Thanos: the Transmute, a flick of the Gauntlet toward the creature, fingers spread (V). */
    THANOS_WARP(Group.CAST, 14, 1, 5, true),
    /** Thanos: the Reality Veil closes round him, and opens again (Sneak + V). */
    THANOS_VEIL_ON(Group.CAST, 16, 2, 6, false),
    THANOS_VEIL_OFF(Group.CAST, 10, 1, 4, false),
    /** Thanos: the chain of the Soul Drain lashes out (hold B). */
    THANOS_DRAIN_CAST(Group.CAST, 12, 1, 5, true),
    /** Thanos: the Soul Sight, the Gauntlet to the eye and swept across (Sneak + B). */
    THANOS_SIGHT(Group.CAST, 14, 2, 5, false),
    /** Thanos: the Rewind, the palm up and trembling and then drawn back (N). */
    THANOS_REWIND(Group.CAST, 22, 2, 8, false),
    /** Thanos: the Time Lock, both palms pressing down in a stamp (Sneak + N). */
    THANOS_TIMELOCK(Group.CAST, 20, 2, 8, false),

    // ===== ultron =====
    /** Ultron: the anti-gravity palms push the air down and out as he rises; the arms settle on the hover. */
    TAKEOFF_ULTRON(Group.TAKEOFF, 18, 2, 8, true),
    /** Ultron: R goes down, both palms snap up along the look and the first bolt leaves. */
    ULTRON_FIRE_START(Group.CAST, 8, 1, 4, false),
    /** Ultron: the Overcharge gathers, a palm rises and the other hand braces its wrist. */
    ULTRON_CHARGE_START(Group.CAST, 12, 3, 4, false),
    /** Ultron: the Overcharge leaves the palm, the arm kicks up and the body is shoved back (the cooldown of R starts). */
    ULTRON_BLAST(Group.CAST, 12, 1, 5, true),
    /** Ultron: Gravity Pull, an open palm thrust at the creature and drawn back as it is hauled in (the payload of its effect). */
    ULTRON_PULL(Group.CAST, 16, 2, 6, true),
    /** Ultron: Gravity Slam, both arms raised over the head and brought down with the stamp (the payload of its effect). */
    ULTRON_SLAM(Group.CAST, 22, 3, 8, true),
    /** Ultron: Upload and Second Life, the arms drawn in, the head glitching, then thrown wide as he becomes data. */
    ULTRON_UPLOAD(Group.CAST, 20, 2, 6, false),
    /** Ultron: the Hack, one hand held out, trembling, as the other machine is overwritten. */
    ULTRON_HACK(Group.CAST, 22, 2, 6, false),
    /** Ultron: the Sentry Army, both arms raised and swept out like a conductor's. */
    ULTRON_ARMY(Group.CAST, 22, 2, 8, false),
    /** Ultron: Command, one arm thrown out at where the drones are sent (the payload of its mark). */
    ULTRON_COMMAND(Group.CAST, 14, 2, 5, false),

    // ===== apocalypse =====
    /** He rises as if lifted: the arms swept up a little, then settled on the hover pose. */
    TAKEOFF_APOC(Group.TAKEOFF, 22, 3, 8, true),
    /** The arms drawn back and low, then thrown forward and down, palms to the ground. */
    APOC_SAND_WAVE(Group.CAST, 18, 2, 6, false),
    /** The main arm reaches at the target and the hand closes. */
    APOC_ENTOMB(Group.CAST, 26, 3, 8, false),
    /** The arms spread wide, then drawn across the body as he steps through. */
    APOC_PORTAL(Group.CAST, 20, 2, 6, false),
    /** The hands drawn up to the chest and opened, palms out (played at the speed of the shield's wind-up). */
    APOC_SHIELD_RAISE(Group.CAST, 15, 2, 5, true),
    /** The shield is let go of: the arms fall. */
    APOC_SHIELD_DROP(Group.CAST, 8, 1, 4, false),
    /** The hand held out, palm up, to the ally; the other hand on the chest. */
    APOC_BESTOW(Group.CAST, 22, 2, 7, false),
    /** The arms gather, then are flung wide and up into the stance of the storm. */
    APOC_STORM_START(Group.CAST, 18, 2, 6, true),

    // ===== cyclops (3.3) =====
    /** Two fingers snap to the visor and the bolt leaves; the hand drops (the Ricochet Shot). */
    CYCLOPS_RICOCHET(Group.CAST, 14, 1, 5, false),
    /** Both hands to the visor, the body thrown back by the wide blast. */
    CYCLOPS_CONCUSSION(Group.CAST, 16, 1, 6, true),
    /** A crouch, the arms swung back, then flung up and forward as the blast at the ground throws him up. */
    CYCLOPS_LEAP(Group.CAST, 18, 1, 7, true),
    /** A fist raised high over his head: "To me, X-Men!" */
    CYCLOPS_RALLY(Group.CAST, 24, 3, 8, false),
    /** The hand to the visor, then pulled down and away with the glasses. */
    CYCLOPS_GLASSES_OFF(Group.CAST, 24, 2, 8, false),
    /** The hand brings the glasses up to his face and lets go. */
    CYCLOPS_GLASSES_ON(Group.CAST, 12, 2, 5, false),
    // ===== professor_x (3.3) =====
    /** Professor X's Mental Blast: two fingers to his temple, the head pushed a little forward. */
    XAVIER_BLAST(Group.CAST, 16, 2, 5, false),
    /** Freeze Minds: two fingers to his temple, the other hand held out, palm open. */
    XAVIER_FREEZE(Group.CAST, 24, 3, 6, false),
    /** His mind leaves for another body: the fingers go to the temple and the head bows, into the trance. */
    XAVIER_LINK(Group.CAST, 14, 2, 4, false),
    /** Cerebro: both hands lift the helmet onto his head and stay at its sides. */
    XAVIER_CEREBRO(Group.CAST, 18, 2, 5, false),
    /** Hear Thoughts on: the fingers brush the temple, the head tilts to listen. */
    XAVIER_THOUGHTS_ON(Group.CAST, 14, 2, 5, false),
    /** Hear Thoughts off: a small shake of the head. */
    XAVIER_THOUGHTS_OFF(Group.CAST, 10, 1, 4, false),
    // ===== agatha (3.3) =====
    /** Agatha rises like a witch lifted by her own magic: the arms swept up and open, then settled on the hover pose. */
    TAKEOFF_AGATHA(Group.TAKEOFF, 20, 2, 8, true),
    /** The Purple Beam begins (R): the hand drawn back to the shoulder, then thrust out along the look. */
    AGATHA_BEAM(Group.CAST, 10, 1, 4, false),
    /** The hex mark or the mind glimpse (Sneak + R): the hand raised, the fingers flicked open at the target. */
    AGATHA_HEX(Group.CAST, 14, 2, 5, false),
    /** The Power Drain (V): both hands thrust at the target, clawed, and dragged back to the chest. */
    AGATHA_DRAIN(Group.CAST, 22, 2, 7, true),
    /** The Witch's Shield forms (B): the hands drawn in, then pushed out into the shield's stance. */
    AGATHA_SHIELD_RAISE(Group.CAST, 10, 2, 4, true),
    /** The shield is let go of: the arms fall. */
    AGATHA_SHIELD_DROP(Group.CAST, 8, 1, 4, false),
    /** The Rune Trap (N): the hand draws a circle in the air, then both palms are driven down at the ground. */
    AGATHA_RUNES(Group.CAST, 24, 2, 8, true)
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
