package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The character of a hero's walk and sprint (see the table of the animation design): how much the arms swing walking and
 * sprinting, how far the legs reach in a sprint, the lean, the twist of the shoulders and the bob. {@link RunPoses} scales
 * the vanilla swing by it. Pure (no client class), so the tests can check that every name the Hero Creator offers
 * ({@code AnimCatalog.RUN_CHARACTERS}) is one of these (3.4: a custom hero walks with any of them).
 */
public enum RunCharacter {
    DEFAULT(1.0F, 1.20F, 1.10F, 6, 4, 0.015F),
    MAGNETO(0.85F, 1.0F, 1.0F, 4, 3, 0.015F),
    WANDA(0.80F, 1.10F, 1.05F, 5, 4, 0.015F),
    VISION(0.60F, 0.80F, 1.0F, 3, 2, 0.010F),
    WHITE_VISION(0.30F, 0.35F, 1.0F, 0, 0, 0.0F),
    BILLY(1.0F, 1.20F, 1.10F, 6, 4, 0.025F),
    LOKI(0.75F, 1.15F, 1.10F, 7, 5, 0.015F),
    THOR(1.15F, 1.30F, 1.15F, 10, 6, 0.030F),
    /** The Phoenix: an unhurried walk with relaxed hands, a shade calmer than the default. */
    PHOENIX(0.90F, 1.15F, 1.05F, 5, 4, 0.015F),
    /** Spider-Man: agile and light, between Billy and Quicksilver, with a sprint that leans into it. */
    SPIDER(1.05F, 1.25F, 1.20F, 9, 5, 0.020F),
    /** Spider-Man in the Black Eyes: a predatory lope, longer in the stride and lower. */
    SPIDER_SURGE(1.10F, 1.40F, 1.30F, 16, 7, 0.030F),
    /** Thanos: heavy and wide, between Thor and Magneto; the bob of a stride that shakes the ground. */
    THANOS(0.90F, 1.15F, 1.15F, 9, 5, 0.030F),
    /** Ultron: a stiff, deliberate march, the arms hardly swinging, no lean, almost no bob. */
    ULTRON(0.55F, 0.70F, 1.0F, 2, 2, 0.008F),
    /** Apocalypse: an unhurried, upright god's walk: little arm swing and almost no bounce. */
    APOCALYPSE(0.55F, 0.80F, 0.90F, 3, 2, 0.008F),
    /** Cyclops: a field leader's run, purposeful and squared, a little lean into the sprint. */
    CYCLOPS(0.90F, 1.15F, 1.10F, 8, 4, 0.016F),
    /** Odin: an old king's stately stride, the arms swinging little, upright even in a sprint, a slow heavy bob. */
    ODIN(0.55F, 0.75F, 0.95F, 2, 2, 0.012F),
    /** The young Charles: an easy, unhurried walk with a little swing. */
    XAVIER(0.85F, 1.10F, 1.05F, 5, 3, 0.012F),
    /** The old professor rolls in his chair: no swing of the arms, no lean, no bob (the chair carries him). */
    XAVIER_SEATED(0.0F, 0.0F, 1.0F, 0, 0, 0.0F),
    /** Agatha: a witch's measured, self-assured stride, the hands swinging little, the sprint a hurried glide. */
    AGATHA(0.70F, 0.95F, 1.0F, 4, 3, 0.012F),
    /** 3.5: Heimdall, the sentinel: a heavy, upright stride in his armour, the arms swinging little, a steady bob. */
    HEIMDALL(0.65F, 0.95F, 1.05F, 3, 2, 0.020F),
    /** Hela (3.5): an upright glide, the arms hardly swinging, the sprint long and smooth, no bounce. */
    HELA(0.45F, 0.70F, 1.10F, 3, 2, 0.006F),
    /** Doctor Strange (3.6): upright and quick, the arms swinging little, the cloak doing the rest. */
    DOCTOR_STRANGE(0.70F, 1.00F, 1.15F, 4, 3, 0.012F),
    /**
     * The Hulk (3.6): a heavy lope, the arms swinging wide, a deep lean into the sprint, a bob that shakes the ground.
     */
    HULK(1.15F, 1.50F, 1.15F, 12, 7, 0.040F),
    // ===== wolverine (3.7) =====
    /** Wolverine (3.7): a low, forward-leaning sprint, the arms held back, a quick hard bob. */
    WOLVERINE(1.0F, 0.75F, 1.30F, 16, 5, 0.025F),
    // ===== deadpool (3.7) =====
    /** Deadpool (3.7): a jaunty jog, the arms swinging loose, a bounce in the step and a swagger of the shoulders. */
    DEADPOOL(1.10F, 1.30F, 1.15F, 7, 7, 0.030F)
    // ===== captain (3.8) =====
    // ===== red_hulk (3.8) =====
    // ===== iron_man (3.8) =====
    // (Lane L3. Each starts with a comma, so the regions of 3.8 join in any order.)
    ,
    /** Iron Man (3.8): heavy, metallic steps in a suit; a short arm swing, a firm bob (lighter in the Mark 5). */
    IRON_MAN(0.80F, 1.05F, 1.05F, 9, 3, 0.032F)
    // ===== falcon (3.8) =====
    // (Lane L5. Each starts with a comma, so the regions of 3.8 join in any order.)
    ,
    /** Falcon (3.8): a light, quick run, a soldier's, the arms swinging easily, a little lean, a light bob. */
    FALCON(0.95F, 1.20F, 1.20F, 8, 4, 0.018F)
    // ===== love (3.8) =====
    // (3.8: each lane appends its hero's run here and routes it in of(); until then they run as anyone.)
    ;

    public final float walkArms;
    public final float sprintArms;
    public final float sprintLegs;
    public final float lean;
    public final float twist;
    public final float bob;

    RunCharacter(float walkArms, float sprintArms, float sprintLegs, float lean, float twist, float bob) {
        this.walkArms = walkArms;
        this.sprintArms = sprintArms;
        this.sprintLegs = sprintLegs;
        this.lean = lean;
        this.twist = twist;
        this.bob = bob;
    }

    /** The character of a built-in hero (the default for those without one: the speedsters, a custom hero's own). */
    public static RunCharacter of(Power power, boolean whiteVision) {
        return switch (power) {
            case MAGNETO -> MAGNETO;
            case SCARLET_WITCH -> WANDA;
            case VISION -> whiteVision ? WHITE_VISION : VISION;
            case BILLY -> BILLY;
            case LOKI -> LOKI;
            case THOR -> THOR;
            case PHOENIX -> PHOENIX;
            case SPIDER_MAN -> SPIDER;
            case THANOS -> THANOS;
            case ULTRON -> ULTRON;
            case APOCALYPSE -> APOCALYPSE;
            case CYCLOPS -> CYCLOPS;
            case ODIN -> ODIN;
            case PROFESSOR_X -> XAVIER;
            case AGATHA -> AGATHA;
            case HEIMDALL -> HEIMDALL;
            case HELA -> HELA; // (3.5)
            case DOCTOR_STRANGE -> DOCTOR_STRANGE; // (3.6)
            case HULK -> HULK; // (3.6; Bruce Banner walks as anyone: RunPoses)
            case WOLVERINE -> WOLVERINE; // (3.7)
            case DEADPOOL -> DEADPOOL; // (3.7)
            case IRON_MAN -> IRON_MAN; // (3.8)
            case FALCON -> FALCON; // (3.8)
            default -> DEFAULT;
        };
    }

    /** The character of a name of {@code AnimCatalog.RUN_CHARACTERS} ({@code "thor"}), or null for an unknown name. */
    @Nullable
    public static RunCharacter byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
