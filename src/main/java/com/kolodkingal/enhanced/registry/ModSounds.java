package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own sound effects (all original, synthesized) and Quicksilver's song. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Enhanced.MODID);

    /**
     * "Sweet Dreams (Are Made of This)", which Quicksilver's Walkman played during Quicksilver Time until 3.5; since 3.5 his
     * own song ({@link #MUSIC_QUICKSILVER}) is the default, and this one the client option quicksilverSong = sweet_dreams. A
     * player (config/enhanced/music) or the world (its music folder) can play another song instead (see SpeedsterMusic).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICKSILVER_THEME = register("quicksilver_theme");

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN = register("awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAOS_BEAM = register("chaos_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAOS_BEAM_START = register("chaos_beam_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_RISE = register("hex_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_CROSS = register("hex_cross");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_GRAB = register("magnet_grab");
    /** The Magnetic Pull (3.1) setting off: the grab's own file, deeper and slower (pitch 0.6 in sounds.json). */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_PULL = register("magnet_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_HUM = register("magnet_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHRAPNEL_FIRE = register("shrapnel_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICKSILVER_WHOOSH = register("quicksilver_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICKSILVER_DASH = register("quicksilver_dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> VISION_PHASE = register("vision_phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_STONE_BEAM = register("mind_stone_beam");

    // Thor
    public static final DeferredHolder<SoundEvent, SoundEvent> MJOLNIR_THROW = register("mjolnir_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> MJOLNIR_RETURN = register("mjolnir_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> MJOLNIR_IMPACT = register("mjolnir_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOD_OF_THUNDER = register("god_of_thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIFROST_CHARGE = register("bifrost_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIFROST_ARRIVE = register("bifrost_arrive");

    // Tommy
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIRLWIND = register("whirlwind");
    public static final DeferredHolder<SoundEvent, SoundEvent> VIBRATE = register("vibrate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOLECULAR_BURST = register("molecular_burst");

    // Billy and the Witches' Road
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_BOLT = register("hex_bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FREEZE_SPELL = register("freeze_spell");
    public static final DeferredHolder<SoundEvent, SoundEvent> WITCHES_ROAD = register("witches_road");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_TRAVEL = register("road_travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_NOISE = register("the_noise");

    // Loki
    public static final DeferredHolder<SoundEvent, SoundEvent> LOKI_DAGGER = register("loki_dagger");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOKI_ILLUSION = register("loki_illusion");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOKI_DISSOLVE = register("loki_dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOKI_VANISH = register("loki_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOKI_SHAPESHIFT = register("loki_shapeshift");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROST_GIANT = register("frost_giant");
    public static final DeferredHolder<SoundEvent, SoundEvent> TESSERACT = register("tesseract");

    // The voices of the Infinity Stones (see InfinityStone): a use sound and a quiet hum for each. The Space and Mind
    // Stones are in the mod; the other four are registered for the Thanos boss (the names are <stone>_stone and
    // <stone>_stone_hum, as the files and sounds.json have them).
    public static final DeferredHolder<SoundEvent, SoundEvent> SPACE_STONE = register("space_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPACE_STONE_HUM = register("space_stone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_STONE = register("mind_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_STONE_HUM = register("mind_stone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_STONE = register("power_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_STONE_HUM = register("power_stone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> REALITY_STONE = register("reality_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> REALITY_STONE_HUM = register("reality_stone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_STONE = register("soul_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_STONE_HUM = register("soul_stone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_STONE = register("time_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_STONE_HUM = register("time_stone_hum");

    // The sounds of 3.0, one region per hero (the // ===== lines), each lane's own: its events here, its entries in
    // sounds.json and its subtitles in the lang files (DataTests checks all three). The names start with the hero's
    // prefixes so they never collide: phoenix_ mind_ psionic_ | web_ spider_ leg_ instant_kill_ black_eyes_ |
    // thanos_ gauntlet_ soul_ time_ reality_ (not the stone voices above, which are <stone>_stone and
    // <stone>_stone_hum) | ultron_ sentry_ | apocalypse_ sand_ entomb essence_.

    // ===== phoenix =====
    /** The Burst gathers (3 s); the Ember's ritual plays it too. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PHOENIX_CHARGE = register("phoenix_charge");
    /** The Burst goes off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PHOENIX_BURST = register("phoenix_burst");
    /** The flames round her while she charges (a loop that follows her). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PHOENIX_BLAZE = register("phoenix_blaze");
    /** Mind Reading switched on, or a new mind found. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_READ = register("mind_read");
    /** The Mind Sense ping. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_SENSE = register("mind_sense");
    /** A creature is taken over. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MIND_CONTROL = register("mind_control");
    /** The Psionic Field closes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PSIONIC_FIELD = register("psionic_field");

    // ===== spider =====
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_SHOT = register("web_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_ZIP = register("web_zip");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_BURST = register("web_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_SPLAT = register("web_splat");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_STICK = register("web_stick");
    /** The creak of a taut web while he swings (a loop). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WEB_SWING = register("web_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIDER_SENSE = register("spider_sense");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_EYES_ON = register("black_eyes_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_EYES_OFF = register("black_eyes_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIDER_LEGS = register("spider_legs");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEG_STRIKE = register("leg_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> INSTANT_KILL_ON = register("instant_kill_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIDER_BITE = register("spider_bite");

    // ===== thanos =====

    // The summoning and the arrival
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_SUMMON = register("thanos_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_BELL = register("thanos_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_ARRIVE = register("thanos_arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_ROAR = register("thanos_roar");

    // The Titan himself
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_STEP = register("thanos_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_SWING = register("thanos_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_HIT = register("thanos_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_HURT = register("thanos_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_DEATH = register("thanos_death");

    // The Gauntlet and the stones
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNTLET_CLENCH = register("gauntlet_clench");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_STONE_SURGE = register("thanos_stone_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_POWER_BLAST = register("thanos_power_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_INFINITY_CHARGE = register("thanos_infinity_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_INFINITY_BEAM = register("thanos_infinity_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_METEOR_WHISTLE = register("thanos_meteor_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_BARRIER_UP = register("thanos_barrier_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_REWIND = register("time_rewind");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_DRAIN = register("soul_drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_WORLD = register("soul_world");
    public static final DeferredHolder<SoundEvent, SoundEvent> REALITY_POP = register("reality_pop");

    // The music of the fight: the layer each stone adds (the six stones' own voices are above)
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_THEME_1 = register("thanos_theme_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_THEME_2 = register("thanos_theme_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_THEME_3 = register("thanos_theme_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_THEME_4 = register("thanos_theme_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> THANOS_THEME_5 = register("thanos_theme_5");

    // ===== ultron =====
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_AWAKEN = register("ultron_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_THEME = register("ultron_theme");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_BOLT = register("ultron_bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_OVERCHARGE = register("ultron_overcharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_SERVO = register("ultron_servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_BEAM = register("ultron_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_GRAVITY = register("ultron_gravity");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_LIFT_START = register("ultron_lift_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_RUMBLE = register("ultron_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_UPLOAD = register("ultron_upload");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_SPEAK = register("ultron_speak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTRY_HOVER = register("sentry_hover");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTRY_DEATH = register("sentry_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_TRANSFER = register("ultron_transfer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTRON_DEATH = register("ultron_death");

    // ===== apocalypse =====
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_AWAKEN = register("apocalypse_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_PORTAL = register("apocalypse_portal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAND_WAVE = register("sand_wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAND_SPIKE = register("sand_spike");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAND_STORM_LOOP = register("sand_storm_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTOMB = register("entomb");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTOMB_BREAK = register("entomb_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_UP = register("apocalypse_shield_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_HIT = register("apocalypse_shield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_BREAK = register("apocalypse_shield_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_MIND_CRUSH = register("apocalypse_mind_crush");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_SPEAK = register("apocalypse_speak");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_BESTOW = register("apocalypse_bestow");
    public static final DeferredHolder<SoundEvent, SoundEvent> ESSENCE_TRANSFER = register("essence_transfer");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_THEME = register("apocalypse_theme");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_THEME_FINAL = register("apocalypse_theme_final");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_DEATH = register("apocalypse_death");

    // ===== 3.1 build =====
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_ON_WANDA = register("build_on_wanda");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_ON_BILLY = register("build_on_billy");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_CAST_WANDA = register("build_cast_wanda");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_CAST_BILLY = register("build_cast_billy");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_HUM = register("build_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_FIZZLE = register("build_fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_UNDO = register("build_undo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUILD_RANK_UP = register("build_rank_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> APOCALYPSE_CRIT = register("apocalypse_crit");

    // ===== 3.3 world: the music of the Witches' Road, one arrangement for each stage (a resource pack can replace any) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_ENTRANCE = register("music.road.entrance");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_PARKOUR = register("music.road.parkour");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_COPY = register("music.road.copy");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_BOAT = register("music.road.boat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_ENDING = register("music.road.ending");
    // ===== 3.5 music: the Road's version for the choice, and the speedsters' own Walkman songs (original synth-pop) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> ROAD_MUSIC_CHOICE = register("music.road.choice");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_QUICKSILVER = register("music.quicksilver");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SPEED = register("music.speed");
    // ===== cyclops (3.3) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_OPTIC_START = register("cyclops_optic_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_OPTIC_LOOP = register("cyclops_optic_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_UNLEASHED_START = register("cyclops_unleashed_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_UNLEASHED_LOOP = register("cyclops_unleashed_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_RICOCHET = register("cyclops_ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_CONCUSSION = register("cyclops_concussion");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_LEAP = register("cyclops_leap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_RALLY = register("cyclops_rally");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYCLOPS_VISOR = register("cyclops_visor");
    // ===== 3.3 custom heroes (the Hero Creator's sounds and the custom powers') =====
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_AWAKEN = register("custom_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_BLAST = register("custom_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_ZAP = register("custom_zap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_IMPACT = register("custom_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_HEAL = register("custom_heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SHIFT = register("custom_shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_WHOOSH = register("custom_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_TELEPORT = register("custom_teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SHIELD = register("custom_shield");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SPELL = register("custom_spell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SCREAM = register("custom_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> HERO_CREATOR_OPEN = register("hero_creator_open");
    // ===== professor_x (3.3) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_AWAKEN = register("xavier_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_BLAST = register("xavier_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_THOUGHTS = register("xavier_thoughts");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_LINK = register("xavier_link");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_UNLINK = register("xavier_unlink");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_BACKLASH = register("xavier_backlash");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_FREEZE = register("xavier_freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> XAVIER_CEREBRO = register("xavier_cerebro");
    // ===== agatha (3.3) =====
    /** The Darkhold's whispers rise round the one who reads it, and Agatha wakes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_AWAKEN = register("agatha_awaken");
    /** The Power Drain: a hollow, sucking swell drawn into her hands. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_DRAIN = register("agatha_drain");
    /** The Rune Trap is laid: a low choir chord and a ring of chimes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_RUNES = register("agatha_runes");
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_SHIELD_UP = register("agatha_shield_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_SHIELD_HIT = register("agatha_shield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> AGATHA_SHIELD_BREAK = register("agatha_shield_break");

    // ===== the Mutant Cure (3.3.1) =====
    /** The cure goes in: the hiss of the injector and a hollow tone falling away. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUTANT_CURE = register("mutant_cure");

    // ===== asgard (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_ASGARD = register("music.asgard");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_ASGARD_LOOP = register("ambient.asgard.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_ASGARD_ADDITIONS = register("ambient.asgard.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASGARD_FALLS = register("asgard_falls");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIFROST_MECHANISM_SPIN = register("bifrost_mechanism_spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_STONE_CALL = register("rune_stone_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRONE_SIT = register("throne_sit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORONATION = register("coronation");
    public static final DeferredHolder<SoundEvent, SoundEvent> MURAL_CRACK = register("mural_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_BREAK = register("seal_break");

    // ===== heimdall (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_SLASH = register("heimdall_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_LUNGE = register("heimdall_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_SIGHT_ON = register("heimdall_sight_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_SIGHT_OFF = register("heimdall_sight_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_FARSIGHT = register("heimdall_farsight");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_WARD_UP = register("heimdall_ward_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_WARD_DOWN = register("heimdall_ward_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEIMDALL_CALL = register("heimdall_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIFROST_STRIKE = register("bifrost_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOFUND_RETURN = register("hofund_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATCH_START = register("watch_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATCH_WIN = register("watch_win");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATCH_LOSE = register("watch_lose");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHADOW_STRUCK = register("shadow_struck");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_WATCH = register("music.watch");

    // ===== odin (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> GUNGNIR_BLAST = register("gungnir_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUNGNIR_THROW = register("gungnir_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUNGNIR_CATCH = register("gungnir_catch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINFORCE_SHIELD_UP = register("odinforce_shield_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINFORCE_SHIELD_DOWN = register("odinforce_shield_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINFORCE_HEAL = register("odinforce_heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> BANISH = register("banish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINFORCE_WAVE = register("odinforce_wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_CAW = register("raven_caw");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_FLAP = register("raven_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODIN_COMMAND = register("odin_command");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINSLEEP_START = register("odinsleep_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ODINSLEEP_END = register("odinsleep_end");

    // ===== hela (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_ARRIVE = register("hela_arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_VOICE = register("hela_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> NECROSWORD_THROW = register("necrosword_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> NECROSWORD_HIT = register("necrosword_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADE_WALL = register("blade_wall");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADE_RAIN = register("blade_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADE_STORM = register("blade_storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_SHADOW_STEP = register("hela_shadow_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_CHANNEL = register("hela_channel");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_CHANNEL_BREAK = register("hela_channel_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_FORM_ON = register("hela_form_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_FORM_OFF = register("hela_form_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_GRASP = register("hela_grasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> MJOLNIR_CAUGHT = register("mjolnir_caught");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELA_DEFEAT = register("hela_defeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> BERSERKER_RISE = register("berserker_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> BERSERKER_AMBIENT = register("berserker_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BERSERKER_HURT = register("berserker_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BERSERKER_DEATH = register("berserker_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIS_GROWL = register("fenris_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIS_HOWL = register("fenris_howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIS_BITE = register("fenris_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIS_HURT = register("fenris_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENRIS_DEATH = register("fenris_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HELA_1 = register("music.hela_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HELA_2 = register("music.hela_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HELA_3 = register("music.hela_3");

    // ===== asgardians (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_TALK_LOW = register("npc_talk_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_TALK_MID = register("npc_talk_mid");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_TALK_HIGH = register("npc_talk_high");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_SPAR_CLASH = register("npc_spar_clash");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_BOW = register("npc_bow");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_DOWN = register("npc_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> EINHERJAR_SUMMON = register("einherjar_summon");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enhanced.id(name)));
    }

    private ModSounds() {
    }
}
