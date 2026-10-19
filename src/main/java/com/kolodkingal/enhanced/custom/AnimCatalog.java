package com.kolodkingal.enhanced.custom;

import java.util.List;

/**
 * The animation choices a custom hero's definition may name (3.4), as plain lower-case strings so that the server, the
 * rules and the tests read them without a client class: each gesture or stance is the lower-case name of a
 * {@code GestureId} or {@code Stance} constant. The editor offers them (names: {@code anim.enhanced.gesture.<id>},
 * {@code .stance.<id>}, {@code .flight.<style>}, {@code .run.<char>}, {@code .speed_run.<s>}, {@code .idle.<i>}); the
 * client's {@code CustomAnimRoutes} plays them. Besides these, every choice may be {@link CustomHero#AUTO}, and a move or a
 * stance {@link CustomHero#NONE}.
 */
public final class AnimCatalog {
    /** The editor's "moves": the gestures a binding may play (Appendix B of the plan). */
    public static final List<String> GESTURES = List.of(
            "billy_bolt", "speed_time", "thor_summon", "cyclops_rally", "thanos_slam", "ultron_slam", "xavier_blast",
            "thanos_step", "loki_dagger", "loki_cast", "phoenix_burst", "apoc_sand_wave", "agatha_drain", "ultron_army",
            "ultron_hack", "speed_dash", "qs_blitz", "agatha_runes", "spider_shoot", "cyclops_concussion", "cyclops_leap",
            "tommy_boom", "apoc_bestow", "phoenix_psionic_cast", "thanos_pull", "thanos_timelock", "thanos_rewind",
            "billy_freeze_cast", "phoenix_mind_cast", "ultron_blast", "wanda_beam_kick", "agatha_hex",
            // 3.5 (the creator lane): the moves of the Sneak variants and the rest of the heroes' keys.
            "mag_pull", "mag_disarm", "qs_snatch", "qs_barrage", "tommy_blitz", "tommy_vibrate", "tommy_accel",
                    "loki_fan",
            "thanos_blast", "thanos_warp", "thanos_sight", "apoc_entomb", "apoc_portal", "cyclops_ricochet",
                    "xavier_freeze",
            "xavier_cerebro", "ultron_upload", "spider_burst");

    /** Stances a held power may take. */
    public static final List<String> STANCES_HOLD = List.of(
            "beam_hand", "beam_wanda", "vision_beam", "thor_beam", "ultron_fire", "cyclops_blast", "thanos_drain",
            "shield_wanda", "apoc_shield", "agatha_shield", "barrier_billy", "billy_freeze", "phoenix_charge", "apoc_storm",
            "tk_strain", "spider_swing", "mag_shrapnel");

    /** Stances a switched-on power may take. */
    public static final List<String> STANCES_TOGGLE = List.of(
            "mag_field", "vision_phased", "vision_dense", "loki_frost", "thor_unleashed", "noise", "phoenix_read",
            "spider_surge", "spider_kill", "thanos_veil", "cyclops_unleashed", "spider_alert", "xavier_trance");

    /** Flight styles: the built-in fliers' profiles, take-offs and flight effects, or the game's plain flight. */
    public static final List<String> FLIGHT_STYLES = List.of(
            "plain", "magneto", "wanda", "vision", "white_vision", "billy", "thor", "phoenix", "ultron", "apocalypse", "agatha",
            // 3.6
            "doctor_strange");

    /** Run characters (how the walk and sprint look). */
    public static final List<String> RUN_CHARACTERS = List.of(
            "default", "magneto", "wanda", "vision", "white_vision", "billy", "loki", "thor", "phoenix", "spider", "thanos",
            "ultron", "apocalypse", "cyclops", "xavier", "xavier_seated", "agatha",
            // 3.5
            "heimdall", "odin", "hela",
            // 3.6
            "doctor_strange", "hulk",
            // 3.7 (lane L3)
            "deadpool");

    /** Speedster runs: Quicksilver in his Time, Quicksilver's Super Speed, Tommy's. */
    public static final List<String> SPEED_RUNS = List.of("qs_time", "qs_speed", "tommy");

    /** Idle stances (at rest on the ground): none, or one of the heroes' ambient stances. */
    public static final List<String> IDLES = List.of("none", "calm", "ready", "regal", "heavy", "jotun", "crackle", "menacing");

    /** Whether {@code move} is a value a binding's move may have. */
    public static boolean isMove(String move) {
        return CustomHero.AUTO.equals(move) || CustomHero.NONE.equals(move) || GESTURES.contains(move);
    }

    /** Whether {@code stance} may be the stance of a power that acts as {@code act} (held or switched on only). */
    public static boolean isStance(String stance, PowerModule.Act act) {
        if (CustomHero.AUTO.equals(stance) || CustomHero.NONE.equals(stance)) {
            return true;
        }
        return act == PowerModule.Act.HOLD ? STANCES_HOLD.contains(stance)
                : act == PowerModule.Act.TOGGLE && STANCES_TOGGLE.contains(stance);
    }

    private AnimCatalog() {
    }
}
