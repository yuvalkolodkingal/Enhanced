package com.kolodkingal.enhanced.client.render;

/**
 * A sustained pose that lasts while a synced state holds (a beam is on, something is held): it has an eased weight and
 * its own pose rows in {@link Stances}. Ambient stances are applied first; aimed and holding ones after, so they win where
 * they overlap (and their arms count as occupied for the swing restore).
 */
public enum Stance {
    MAG_FIELD(4, 5, false),
    VISION_PHASED(4, 5, false),
    VISION_DENSE(3, 5, false),
    LOKI_FROST(5, 6, false),
    THOR_UNLEASHED(4, 6, false),
    NOISE(6, 6, false),
    WANDA_READY(3, 8, false),
    WANDA_HEX_CONTROL(3, 4, false),
    THOR_BIFROST(5, 3, false),

    THOR_WAIT(2, 3, true),
    TK_HOLD(2.5F, 4, true),
    TK_STRAIN(6, 8, true),
    MAG_SHRAPNEL(1.5F, 3, true),
    MAG_CATCH(1, 3, true),
    BILLY_FREEZE(2, 4, true),
    VISION_BEAM(2, 3, true),
    BEAM_WANDA(1.2F, 3, true),
    SHIELD_WANDA(1.5F, 3, true),
    THOR_BEAM(1, 3, true),
    /** 3.2: a continuous beam (Billy's Hex Beam, or any other source: {@code Live.BEAMING}): one arm out along the look, the other braced. */
    BEAM_HAND(1, 3, true),
    /** 3.2: Billy's Hex Barrier is up: the off forearm raised forward holding the plate, the other arm braced back, the body turned a little. */
    BARRIER_BILLY(1.5F, 3, true),
    TOMMY_WHIRL(3, 5, true),

    // The stances of 3.0, in one region per hero (the // ===== lines), each lane's own. Whatever the order here, the
    // ambient ones are applied first and the aimed and holding ones after (see Stances).
    // ===== phoenix =====
    /** Phoenix: Mind Reading is on, the head tilted as if listening. */
    PHOENIX_READ(6, 6, false),
    /** Phoenix: the Burst gathers, a V of arms thrown up and out. */
    PHOENIX_CHARGE(1.5F, 4, true),

    // ===== spider =====
    /** Spider-Man clings to a wall: the arms reach up, the knees go out, the body faces the wall. */
    SPIDER_CLING_WALL(3, 5, false),
    /** He hangs from a ceiling: the body is flipped over, feet to the ceiling. */
    SPIDER_CLING_CEILING(6, 8, false),
    /** The Black Eyes: a predatory hunch, hands hanging forward and clawed. */
    SPIDER_SURGE(4, 8, false),
    /** Instant Kill: a menacing stillness. */
    SPIDER_KILL(3, 6, false),
    /** His spider-sense tingles: the guard goes up. */
    SPIDER_ALERT(1.5F, 5, false),
    /** A zip: the web arm points at the anchor, the body is turned to it. */
    SPIDER_ZIP(1.5F, 4, true),
    /** A swing: the web arm is up along the web, the knees tucked. */
    SPIDER_SWING(2, 5, true),

    // ===== thanos =====
    /** Thanos: the Reality Veil is up, the Gauntlet hand held out as if it held a thin film. */
    THANOS_VEIL(4, 5, false),
    /** Thanos: the Soul Drain holds, the Gauntlet arm along the look and the legs braced. */
    THANOS_DRAIN(2, 4, true),

    // ===== ultron =====
    /** Ultron: R is held, both palms raised along the look and firing plasma in turn. */
    ULTRON_FIRE(1, 3, true),
    /** Ultron: the Overcharge builds, one palm raised along the look and the other hand bracing its wrist. */
    ULTRON_CHARGE(2, 3, true),

    // ===== apocalypse =====
    /** Apocalypse's Shield: both forearms raised before the chest, palms out. */
    APOC_SHIELD(1.5F, 3.0F, true),
    /** The Sandstorm: both arms flung wide and up, the head back. */
    APOC_STORM(3.0F, 5.0F, true),

    // ===== cyclops (3.3) =====
    /** Cyclops's Optic Blast: two fingers held to the visor, the other arm braced, the feet planted. */
    CYCLOPS_BLAST(1.5F, 3.0F, true),
    /** The glasses are off and the giant beam pours out: both hands at his face, straining, braced against it. */
    CYCLOPS_UNLEASHED(2.0F, 4.0F, true),
    // ===== odin (3.5) =====
    /** The Odinforce Shield: Gungnir planted upright before him in the main hand, the other hand open toward the dome. */
    ODIN_SHIELD(1.5F, 3.0F, true),
    /** The Odinsleep: kneeling on one knee, the head bowed, both hands resting on the spear. */
    ODIN_SLEEP(3.0F, 4.0F, true),
    /** At rest with Gungnir: both hands on the upright spear, the head high (the king's bearing). */
    ODIN_REGAL(8.0F, 4.0F, false),
    // ===== professor_x (3.3) =====
    /** The old Professor X sits in his wheelchair: the legs forward on the footrest, the forearms on the armrests. */
    XAVIER_SEATED(4.0F, 4.0F, false),
    /** His mind is in another body: two fingers at his temple, the head bowed, still. */
    XAVIER_TRANCE(2.0F, 4.0F, true),
    /** Cerebro's helmet is on: both hands at its sides. */
    XAVIER_CEREBRO(2.0F, 4.0F, true),
    // ===== agatha (3.3) =====
    /** Agatha's Witch's Shield: both hands held out before her, fingers spread, a little apart. */
    AGATHA_SHIELD(1.5F, 3.0F, true),
    // ===== heimdall (3.5) =====
    /** Heimdall's Guardian's Ward: both hands on the pommel of the blade planted before him, the head bowed a little. */
    HEIMDALL_WARD(2.0F, 4.0F, true),
    /** Heimdall's Far Sight: the head forward, the other hand shading the eyes as he looks to the edge of the realms. */
    HEIMDALL_FARSIGHT(3.0F, 4.0F, true),
    /** Heimdall at his watch (the Sight open, standing still): both hands resting on the sword, upright. */
    HEIMDALL_WATCH(8.0F, 6.0F, false),
    // ===== hela (3.5) =====
    /** Hela with her form on: a necrosword in each hand, held low and out, the points turned outward. */
    HELA_BLADES(3.0F, 5.0F, false),
    /** Death's Grasp: one hand held out before her, fingers spread and clawed, the other drawn back. */
    HELA_GRASP(1.5F, 3.0F, true),
    // ===== strange (3.6) =====
    /** Doctor Strange while a spell lasts: both hands up before him, the fingers in the spell mudra. */
    STRANGE_CASTING(2.0F, 4.0F, true),
    /** The Shield of the Seraphim held: the off palm out along the look, the other hand circling beside it. */
    STRANGE_SHIELD(1.5F, 3.0F, true),
    /** Hovering on the Cloak of Levitation: the legs together, a slight crouch, the hands relaxed and a little open. */
    STRANGE_LEVITATE(4.0F, 4.0F, false),
    // ===== hulk (3.6) =====
    /** The Hulk at rest: hunched, the shoulders up, the arms hanging wide, the knuckles low; a slow heavy breath. */
    HULK_IDLE(5.0F, 4.0F, false),
    /** The Hulk enraged: the arms out and forward, the fists clenched, the chest heaving. */
    HULK_ENRAGED(3.0F, 6.0F, false),
    /** In the air on a leap: the knees tucked, the arms up. */
    HULK_AIR(2.0F, 3.0F, true),
    // ===== healing (3.7) =====
    // ===== wolverine (3.7) =====
    /** Wolverine with the claws out: a low guard, the fists a little forward and out. */
    WOLVERINE_CLAWS(3.0F, 4.0F, false),
    /** In the Berserker Rage: hunched, the claws wide, the chest heaving. */
    WOLVERINE_BERSERK(3.0F, 6.0F, false),
    /** Claw Climb on a wall: the claws hooking in turn above his head. */
    WOLVERINE_CLIMBING(2.0F, 3.0F, true),
    // ===== deadpool (3.7) =====
    /** Deadpool's Parry: both arms forward, the two swords spinning before him (DeadpoolGearLayer spins them). */
    DEADPOOL_SWORDS(1.5F, 3.0F, true),
    /** Deadpool with his pistols drawn: both arms up along the look. */
    DEADPOOL_GUNS(1.5F, 4.0F, true)
    // ===== captain (3.8) =====
    // ===== red_hulk (3.8) =====
    // ===== iron_man (3.8) =====
    // (Lane L3. Each starts with a comma, so the regions of 3.8 join in any order.)
    ,
    /** A suit goes on or comes off: the arms held a little out, still (the suit assembles round him). */
    IRON_SUITING(2.0F, 3.0F, true),
    /** The Unibeam burns: the chest out, the arms back. */
    IRON_UNIBEAM(2.0F, 3.0F, true),
    /** The Mark 1's Flamethrower burns: both forearms forward. */
    IRON_FLAME(1.5F, 3.0F, true),
    /** The Mark 1's Brace: arms crossed before the chest, knees bent. */
    IRON_BRACING(1.5F, 3.0F, true)
    // ===== nanotech (3.8) =====
    // (Lane L4, after lane L3's; NanoActing.)
    ,
    /** A nanotech shield is up: the left arm before him behind it. */
    NANO_SHIELD(1.5F, 3.0F, true),
    /** The nano blade or the energy blades are out: a low guard, the fists out. */
    NANO_BLADES(2.0F, 3.0F, false),
    /** The Mark 50's nano cannon charges: both forearms forward, merged. */
    NANO_CANNON(1.5F, 3.0F, true),
    /** The Hulkbuster holds what he grabbed up in his right fist. */
    HULKBUSTER_GRAB(1.5F, 3.0F, true)
    // ===== suit_art (3.8) =====
    // ===== falcon (3.8) =====
    // ===== love (3.8) =====
    ;

    public static final Stance[] VALUES = values();
    public static final int COUNT = VALUES.length;

    /** Time constants (ticks) of the weight rising and falling. */
    public final float tauIn;
    public final float tauOut;
    /** Aimed and holding stances: their arms are occupied, so the swing of an attack is mostly muted. */
    public final boolean aimed;

    Stance(float tauIn, float tauOut, boolean aimed) {
        this.tauIn = tauIn;
        this.tauOut = tauOut;
        this.aimed = aimed;
    }
}
