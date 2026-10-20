package com.kolodkingal.enhanced.power;

/**
 * Momentary states of the newer heroes (and of whoever Mjolnir pins down) that clients must see (to draw effects and
 * poses, or to stop a pinned player's own movement) but that are never saved: they are recomputed by the server while
 * the power is in use. Kept together as a bit set in {@link PowerData}, a {@code long} since 3.0 (28 bits are used in
 * 3.3, 42 in 3.5, 53 in 3.6, 58 in 3.7) and two {@code long}s since 3.8 (75 states: at most 128). A state's word and
 * mask come from its ordinal ({@link #word}, {@link #mask}); code only ever asks {@code PowerData.is} and {@code set}.
 *
 * <p>The states of 3.0 sit in one region per hero (the {@code // =====} lines), so the five lanes that build those
 * heroes append to their own lines and never to the same one.
 */
public enum Live {
    /** Tommy, or Quicksilver in Quicksilver Time or Super Speed, is running at super speed right now. */
    SPEEDING,
    /** Tommy is spinning a whirlwind around himself. */
    WHIRLWIND,
    /**
     * Thor is firing his lightning beam from his hand (or from the weapon in it): V is held down, and every client
     * draws the beam to whatever he looks at. (The name is older than the beam, which used to call lightning down from
     * the sky.)
     */
    CALLING_LIGHTNING,
    /** The Bifrost's light is gathering around its traveller (Thor, or anyone holding his weapon), about to take them. */
    BIFROST,
    /** Loki is invisible while his duplicates draw every eye: his held items, armour and name tag are hidden too. */
    HIDDEN,
    /** Loki has just thrown a dagger (a brief throwing pose). */
    THROWING,
    /** Casting a spell: Loki's raised, open hand (Vanish, Shapeshift); Billy's hand thrust forward (Hex Bolt). */
    CASTING,
    /** Billy's crown is out: while he flies, and for a moment after each spell. */
    CROWNED,
    /** Billy is holding the creatures in front of him spellbound (both hands raised). */
    FREEZING,
    /**
     * Mjolnir lies on this player and pins them in place: their client stops their movement (any player, not only
     * heroes).
     */
    PINNED,

    // ===== phoenix =====
    /** Phoenix: N is held and the Burst gathers in her. */
    PHOENIX_CHARGING,
    /** Phoenix: the cosmic fire burns around her body (while she charges, and for a moment after the Burst). */
    PHOENIX_BLAZE,
    /** Phoenix: Mind Sense is on, a ring of light spreading from her through walls. */
    MIND_SENSING,
    /** Phoenix: the Psionic Field is closed around her. */
    PHOENIX_PSIONIC,

    // ===== spider =====
    /** Spider-Man clings to a wall. */
    CLINGING_WALL,
    /** Spider-Man hangs from a ceiling. */
    CLINGING_CEILING,
    /** Spider-Man is pulled along a web to the point he aimed at. */
    ZIPPING,
    /** Spider-Man swings on a web. */
    SWINGING,
    /** Spider-Man's spider-sense tingles. */
    SENSING,
    /** Spider-Man's Black Eyes are on. */
    BLACK_EYES,
    /** The Iron Spider's Instant Kill is active. */
    INSTANT_KILL,

    // ===== thanos =====
    /** Thanos drains the soul of whoever he holds with the Soul Stone (B is held). */
    SOUL_DRAINING,
    /** Thanos sees souls through walls with the Soul Stone. */
    SOUL_SIGHT,

    // ===== ultron =====
    /** Ultron builds the Overcharge: a palm raised and glowing, trembling, for the 15 ticks before the blast (Sneak + R). */
    ULTRON_CHARGING,
    /**
     * Sokovia carries this player into the sky: they can't move on their own, like a pinned player (any player in the
     * arena, not only Ultron).
     */
    LIFTED,

    // ===== apocalypse =====
    /** Apocalypse is calling a sandstorm. */
    SANDSTORM,

    // ===== beam (3.2: any source of a continuous beam sets it while it fires; the style is the beam payload's) =====
    /**
     * A continuous beam (Billy's Hex Beam; see {@code ContinuousBeam}) leaves this player's hand: the key is held. Every
     * client draws it from the {@code BeamPayload}s, holds the arm out along the look, and loops its sound.
     */
    BEAMING,

    // ===== cyclops (3.3) =====
    /**
     * Cyclops has taken off his ruby-quartz glasses: the giant red beam pours from his eyes where he looks, and does not
     * stop until they are back on (or he dies or leaves). Every client draws his eyes blazing and his hands at his face.
     */
    CYCLOPS_UNLEASHED,
    // ===== professor_x =====
    /** Professor X is inside another body ({@code MindLink}): his own sits still, two fingers at his temple. */
    MIND_LINKED,
    /**
     * Professor X steers this player's body (any player): their own client stops their keys, as for a pinned player, and
     * the server moves them.
     */
    MIND_CONTROLLED,
    /** Professor X concentrates for a moment (a Mental Blast, Freeze Minds): two fingers to his temple. */
    XAVIER_FOCUS,
    /** Professor X wears Cerebro's helmet and searches every mind far around him. */
    CEREBRO,
    // ===== speed (3.3) =====
    /** A speedster (Quicksilver or Tommy) runs on a wall (up it, or down it). */
    SURFACE_WALL,
    /** A speedster runs along a ceiling, upside down. */
    SURFACE_CEILING,
    /** Tommy's Sonic Boom has just broken around him (a moment, so every client acts it). */
    SONIC_BOOM,
    // ===== heimdall (3.5) =====
    /** Heimdall has planted Hofund: the Guardian's Ward stands round him (B held). */
    HEIMDALL_WARD,
    /** Heimdall looks far with Far Sight (Sneak + G held): every client draws his head tilted, his hand on the hilt. */
    HEIMDALL_FARSIGHT,
    /** Heimdall is in the middle of a Hofund combo (a slash or the lunge). */
    HEIMDALL_SLASHING,
    // ===== odin (3.5) =====
    /** Gungnir is out of Odin's hand, thrown. (His Odinforce Shield uses the shared chaos-shield record.) */
    ODIN_THROWN,
    /** Huginn and Muninn are out, flying for him. */
    ODIN_RAVENS,
    // ===== hela (3.5) =====
    /** Hela's Death's Grasp is held out (N held). */
    HELA_GRASPING,
    /** Hela raises the dead or calls Fenris (both arms rise). */
    HELA_SUMMONING,
    // ===== strange (3.6) =====
    /**
     * Doctor Strange holds up the Shield of the Seraphim (V held): its own bit for the pose; the shield is the shared
     * record.
     */
    STRANGE_SHIELD,
    /** Doctor Strange's spirit is out of his body (the Astral Projection): he is drawn as a pale-blue ghost. */
    STRANGE_ASTRAL,
    /** Doctor Strange's Mirror Dimension stands round him: every client inside sees the world bend. */
    STRANGE_MIRROR,
    /** Doctor Strange is casting (any spell that lasts): his hands up, the fingers in the spell mudra. */
    STRANGE_CASTING,
    // ===== stones (3.6) =====
    /**
     * The Time Stone (or the Eye of Agamotto) slows time round this player: {@code SlowMotion} reads it ({@code
     * StoneZones}).
     */
    TIME_SLOWING,
    /** The Power Stone's beam pours from this player's hand: purple veins crawl up his arm. */
    POWER_VEINS,
    // ===== hulk (3.6) =====
    /**
     * The Hulk is changing (Banner to the Hulk or back, or one Hulk to the other): he cannot be pushed, his keys wait.
     */
    HULK_TRANSFORMING,
    /** The Hulk is in the air on a Super Leap or a Bound. */
    HULK_LEAPING,
    /** The Hulk holds a creature by the legs for the Grab Smash. */
    HULK_GRABBING,
    /** The Hulk charges shoulder first (the Gamma Charge). */
    HULK_CHARGING,
    /** The Hulk's rage is at half or more: his eyes glow gamma green. */
    HULK_ENRAGED,
    // ===== healing (3.7) =====
    /**
     * A healer (Wolverine, Deadpool, a custom hero with the healing factor) is DOWN: a blow that would have killed him
     * left him at 1 health, kneeling, untouchable, until he rises ({@code healing/Downed}).
     */
    HEALING_DOWN,
    // ===== wolverine (3.7) =====
    /** Wolverine's Berserker Rage is on: hunched, claws wide, faster and stronger. */
    WOLVERINE_BERSERK,
    /** Wolverine is in the air on a Pounce, claws forward. */
    WOLVERINE_POUNCING,
    // ===== deadpool (3.7) =====
    /** Deadpool parries (Sneak + R held): his katanas spin in front of him. */
    DEADPOOL_PARRYING,
    /** Deadpool's pistols are drawn (for two seconds after a shot). */
    DEADPOOL_AIMING,
    // ===== captain (3.8) =====
    /** Captain America charges behind his raised shield (V). */
    CAP_CHARGING,
    /** Captain America is in the air on the Shield Slam's leap, the shield about to come down (G). */
    CAP_SLAMMING,
    // ===== red_hulk (3.8) =====
    /** Red Hulk is changing (Ross to Red Hulk or back): he cannot be pushed, his keys wait. */
    RED_HULK_TRANSFORMING,
    /** Red Hulk crouches and charges his Super Jump (V held). */
    RED_HULK_JUMP_CHARGING,
    /** Red Hulk is in the air on a Super Jump or a Bound. */
    RED_HULK_LEAPING,
    /** Red Hulk's anger is at 75 or more: he steams and glows like embers. */
    RED_HULK_HEATED,
    /** Red Hulk absorbs energy (Sneak + B held): fire, lightning, explosions and beams feed his anger. */
    RED_HULK_ABSORBING,
    /** Red Hulk holds a creature for Grab & Slam. */
    RED_HULK_GRABBING,
    // ===== iron_man (3.8) =====
    /** A suit goes on or comes off (the suit-up's phase plays: rooted, the suit assembling or opening). */
    IRON_SUITING,
    /** Iron Man's shield is up: the Mark 50's nano shield or the Mark 85's giant energy shield. */
    IRON_SHIELD,
    /** Iron Man's blades are out: the Mark 50's nano blade or the Mark 85's energy blades. */
    IRON_BLADES,
    /** The Hulkbuster holds a creature for Grab & Throw. */
    IRON_GRABBING,
    /** Iron Man dashes (the Mark 5's Dash Punch, the Mark 1's Rocket Boost). */
    IRON_DASHING,
    /** Iron Man charges the Mark 50's Nano Cannon (N held). */
    IRON_CHARGING,
    // ===== falcon (3.8) =====
    /** Falcon dives at a foe, wings folded. */
    FALCON_DIVING,
    /** Falcon's wings are wrapped in front of him (B held). */
    FALCON_WING_SHIELD,
    /** Falcon glides on his wings (Sneak + G: the elytra's flight). */
    FALCON_GLIDING,
    ;

    /** The states that fit in one word of {@link PowerData}: the first word holds ordinals 0-63, the second 64-127. */
    static final int WORD = 64;

    /** 3.8: which of the two words of {@link PowerData} holds this state (0 for the first 64, 1 after). */
    public int word() {
        return ordinal() / WORD;
    }

    /** 3.8: the state's bit within its word. */
    public long mask() {
        return 1L << (ordinal() % WORD);
    }
}
