package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** The mod's own sound effects (all original, synthesized) and Quicksilver's song. */
public final class ModSounds {
    public static final Registrar<SoundEvent> SOUNDS = Registrars.create(Registries.SOUND_EVENT, Enhanced.MODID);

    /**
     * "Sweet Dreams (Are Made of This)", which Quicksilver's Walkman played during Quicksilver Time until 3.5; since 3.5 his
     * own song ({@link #MUSIC_QUICKSILVER}) is the default, and this one the client option quicksilverSong = sweet_dreams. A
     * player (config/enhanced/music) or the world (its music folder) can play another song instead (see SpeedsterMusic).
     */
    public static final RegEntry<SoundEvent, SoundEvent> QUICKSILVER_THEME = register("quicksilver_theme");

    public static final RegEntry<SoundEvent, SoundEvent> AWAKEN = register("awaken");
    public static final RegEntry<SoundEvent, SoundEvent> CHAOS_BEAM = register("chaos_beam");
    public static final RegEntry<SoundEvent, SoundEvent> CHAOS_BEAM_START = register("chaos_beam_start");
    public static final RegEntry<SoundEvent, SoundEvent> HEX_RISE = register("hex_rise");
    public static final RegEntry<SoundEvent, SoundEvent> HEX_CROSS = register("hex_cross");
    public static final RegEntry<SoundEvent, SoundEvent> MAGNET_GRAB = register("magnet_grab");
    /** The Magnetic Pull (3.1) setting off: the grab's own file, deeper and slower (pitch 0.6 in sounds.json). */
    public static final RegEntry<SoundEvent, SoundEvent> MAGNET_PULL = register("magnet_pull");
    public static final RegEntry<SoundEvent, SoundEvent> MAGNET_HUM = register("magnet_hum");
    public static final RegEntry<SoundEvent, SoundEvent> SHRAPNEL_FIRE = register("shrapnel_fire");
    public static final RegEntry<SoundEvent, SoundEvent> QUICKSILVER_WHOOSH = register("quicksilver_whoosh");
    public static final RegEntry<SoundEvent, SoundEvent> QUICKSILVER_DASH = register("quicksilver_dash");
    public static final RegEntry<SoundEvent, SoundEvent> VISION_PHASE = register("vision_phase");
    public static final RegEntry<SoundEvent, SoundEvent> MIND_STONE_BEAM = register("mind_stone_beam");

    // Thor
    public static final RegEntry<SoundEvent, SoundEvent> MJOLNIR_THROW = register("mjolnir_throw");
    public static final RegEntry<SoundEvent, SoundEvent> MJOLNIR_RETURN = register("mjolnir_return");
    public static final RegEntry<SoundEvent, SoundEvent> MJOLNIR_IMPACT = register("mjolnir_impact");
    public static final RegEntry<SoundEvent, SoundEvent> GOD_OF_THUNDER = register("god_of_thunder");
    public static final RegEntry<SoundEvent, SoundEvent> BIFROST_CHARGE = register("bifrost_charge");
    public static final RegEntry<SoundEvent, SoundEvent> BIFROST_ARRIVE = register("bifrost_arrive");

    // Tommy
    public static final RegEntry<SoundEvent, SoundEvent> WHIRLWIND = register("whirlwind");
    public static final RegEntry<SoundEvent, SoundEvent> VIBRATE = register("vibrate");
    public static final RegEntry<SoundEvent, SoundEvent> MOLECULAR_BURST = register("molecular_burst");

    // Billy and the Witches' Road
    public static final RegEntry<SoundEvent, SoundEvent> HEX_BOLT = register("hex_bolt");
    public static final RegEntry<SoundEvent, SoundEvent> FREEZE_SPELL = register("freeze_spell");
    public static final RegEntry<SoundEvent, SoundEvent> WITCHES_ROAD = register("witches_road");
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_TRAVEL = register("road_travel");
    public static final RegEntry<SoundEvent, SoundEvent> THE_NOISE = register("the_noise");

    // Loki
    public static final RegEntry<SoundEvent, SoundEvent> LOKI_DAGGER = register("loki_dagger");
    public static final RegEntry<SoundEvent, SoundEvent> LOKI_ILLUSION = register("loki_illusion");
    public static final RegEntry<SoundEvent, SoundEvent> LOKI_DISSOLVE = register("loki_dissolve");
    public static final RegEntry<SoundEvent, SoundEvent> LOKI_VANISH = register("loki_vanish");
    public static final RegEntry<SoundEvent, SoundEvent> LOKI_SHAPESHIFT = register("loki_shapeshift");
    public static final RegEntry<SoundEvent, SoundEvent> FROST_GIANT = register("frost_giant");
    public static final RegEntry<SoundEvent, SoundEvent> TESSERACT = register("tesseract");

    // The voices of the Infinity Stones (see InfinityStone): a use sound and a quiet hum for each. The Space and Mind
    // Stones are in the mod; the other four are registered for the Thanos boss (the names are <stone>_stone and
    // <stone>_stone_hum, as the files and sounds.json have them).
    public static final RegEntry<SoundEvent, SoundEvent> SPACE_STONE = register("space_stone");
    public static final RegEntry<SoundEvent, SoundEvent> SPACE_STONE_HUM = register("space_stone_hum");
    public static final RegEntry<SoundEvent, SoundEvent> MIND_STONE = register("mind_stone");
    public static final RegEntry<SoundEvent, SoundEvent> MIND_STONE_HUM = register("mind_stone_hum");
    public static final RegEntry<SoundEvent, SoundEvent> POWER_STONE = register("power_stone");
    public static final RegEntry<SoundEvent, SoundEvent> POWER_STONE_HUM = register("power_stone_hum");
    public static final RegEntry<SoundEvent, SoundEvent> REALITY_STONE = register("reality_stone");
    public static final RegEntry<SoundEvent, SoundEvent> REALITY_STONE_HUM = register("reality_stone_hum");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_STONE = register("soul_stone");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_STONE_HUM = register("soul_stone_hum");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_STONE = register("time_stone");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_STONE_HUM = register("time_stone_hum");

    // The sounds of 3.0, one region per hero (the // ===== lines), each lane's own: its events here, its entries in
    // sounds.json and its subtitles in the lang files (DataTests checks all three). The names start with the hero's
    // prefixes so they never collide: phoenix_ mind_ psionic_ | web_ spider_ leg_ instant_kill_ black_eyes_ |
    // thanos_ gauntlet_ soul_ time_ reality_ (not the stone voices above, which are <stone>_stone and
    // <stone>_stone_hum) | ultron_ sentry_ | apocalypse_ sand_ entomb essence_.

    // ===== phoenix =====
    /** The Burst gathers (3 s); the Ember's ritual plays it too. */
    public static final RegEntry<SoundEvent, SoundEvent> PHOENIX_CHARGE = register("phoenix_charge");
    /** The Burst goes off. */
    public static final RegEntry<SoundEvent, SoundEvent> PHOENIX_BURST = register("phoenix_burst");
    /** The flames round her while she charges (a loop that follows her). */
    public static final RegEntry<SoundEvent, SoundEvent> PHOENIX_BLAZE = register("phoenix_blaze");
    /** Mind Reading switched on, or a new mind found. */
    public static final RegEntry<SoundEvent, SoundEvent> MIND_READ = register("mind_read");
    /** The Mind Sense ping. */
    public static final RegEntry<SoundEvent, SoundEvent> MIND_SENSE = register("mind_sense");
    /** A creature is taken over. */
    public static final RegEntry<SoundEvent, SoundEvent> MIND_CONTROL = register("mind_control");
    /** The Psionic Field closes. */
    public static final RegEntry<SoundEvent, SoundEvent> PSIONIC_FIELD = register("psionic_field");

    // ===== spider =====
    public static final RegEntry<SoundEvent, SoundEvent> WEB_SHOT = register("web_shot");
    public static final RegEntry<SoundEvent, SoundEvent> WEB_ZIP = register("web_zip");
    public static final RegEntry<SoundEvent, SoundEvent> WEB_BURST = register("web_burst");
    public static final RegEntry<SoundEvent, SoundEvent> WEB_SPLAT = register("web_splat");
    public static final RegEntry<SoundEvent, SoundEvent> WEB_STICK = register("web_stick");
    /** The creak of a taut web while he swings (a loop). */
    public static final RegEntry<SoundEvent, SoundEvent> WEB_SWING = register("web_swing");
    public static final RegEntry<SoundEvent, SoundEvent> SPIDER_SENSE = register("spider_sense");
    public static final RegEntry<SoundEvent, SoundEvent> BLACK_EYES_ON = register("black_eyes_on");
    public static final RegEntry<SoundEvent, SoundEvent> BLACK_EYES_OFF = register("black_eyes_off");
    public static final RegEntry<SoundEvent, SoundEvent> SPIDER_LEGS = register("spider_legs");
    public static final RegEntry<SoundEvent, SoundEvent> LEG_STRIKE = register("leg_strike");
    public static final RegEntry<SoundEvent, SoundEvent> INSTANT_KILL_ON = register("instant_kill_on");
    public static final RegEntry<SoundEvent, SoundEvent> SPIDER_BITE = register("spider_bite");

    // ===== thanos =====

    // The summoning and the arrival
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_SUMMON = register("thanos_summon");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_BELL = register("thanos_bell");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_ARRIVE = register("thanos_arrive");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_ROAR = register("thanos_roar");

    // The Titan himself
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_STEP = register("thanos_step");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_SWING = register("thanos_swing");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_HIT = register("thanos_hit");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_HURT = register("thanos_hurt");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_DEATH = register("thanos_death");

    // The Gauntlet and the stones
    public static final RegEntry<SoundEvent, SoundEvent> GAUNTLET_CLENCH = register("gauntlet_clench");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_STONE_SURGE = register("thanos_stone_surge");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_POWER_BLAST = register("thanos_power_blast");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_INFINITY_CHARGE = register("thanos_infinity_charge");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_INFINITY_BEAM = register("thanos_infinity_beam");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_METEOR_WHISTLE = register("thanos_meteor_whistle");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_BARRIER_UP = register("thanos_barrier_up");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_REWIND = register("time_rewind");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_DRAIN = register("soul_drain");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_WORLD = register("soul_world");
    public static final RegEntry<SoundEvent, SoundEvent> REALITY_POP = register("reality_pop");

    // The music of the fight: the layer each stone adds (the six stones' own voices are above)
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_THEME_1 = register("thanos_theme_1");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_THEME_2 = register("thanos_theme_2");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_THEME_3 = register("thanos_theme_3");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_THEME_4 = register("thanos_theme_4");
    public static final RegEntry<SoundEvent, SoundEvent> THANOS_THEME_5 = register("thanos_theme_5");

    // ===== ultron =====
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_AWAKEN = register("ultron_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_THEME = register("ultron_theme");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_BOLT = register("ultron_bolt");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_OVERCHARGE = register("ultron_overcharge");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_SERVO = register("ultron_servo");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_BEAM = register("ultron_beam");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_GRAVITY = register("ultron_gravity");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_LIFT_START = register("ultron_lift_start");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_RUMBLE = register("ultron_rumble");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_UPLOAD = register("ultron_upload");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_SPEAK = register("ultron_speak");
    public static final RegEntry<SoundEvent, SoundEvent> SENTRY_HOVER = register("sentry_hover");
    public static final RegEntry<SoundEvent, SoundEvent> SENTRY_DEATH = register("sentry_death");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_TRANSFER = register("ultron_transfer");
    public static final RegEntry<SoundEvent, SoundEvent> ULTRON_DEATH = register("ultron_death");

    // ===== apocalypse =====
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_AWAKEN = register("apocalypse_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_PORTAL = register("apocalypse_portal");
    public static final RegEntry<SoundEvent, SoundEvent> SAND_WAVE = register("sand_wave");
    public static final RegEntry<SoundEvent, SoundEvent> SAND_SPIKE = register("sand_spike");
    public static final RegEntry<SoundEvent, SoundEvent> SAND_STORM_LOOP = register("sand_storm_loop");
    public static final RegEntry<SoundEvent, SoundEvent> ENTOMB = register("entomb");
    public static final RegEntry<SoundEvent, SoundEvent> ENTOMB_BREAK = register("entomb_break");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_UP = register("apocalypse_shield_up");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_HIT = register("apocalypse_shield_hit");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_SHIELD_BREAK = register("apocalypse_shield_break");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_MIND_CRUSH = register("apocalypse_mind_crush");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_SPEAK = register("apocalypse_speak");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_BESTOW = register("apocalypse_bestow");
    public static final RegEntry<SoundEvent, SoundEvent> ESSENCE_TRANSFER = register("essence_transfer");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_THEME = register("apocalypse_theme");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_THEME_FINAL = register("apocalypse_theme_final");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_DEATH = register("apocalypse_death");

    // ===== 3.1 build =====
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_ON_WANDA = register("build_on_wanda");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_ON_BILLY = register("build_on_billy");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_CAST_WANDA = register("build_cast_wanda");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_CAST_BILLY = register("build_cast_billy");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_HUM = register("build_hum");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_FIZZLE = register("build_fizzle");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_UNDO = register("build_undo");
    public static final RegEntry<SoundEvent, SoundEvent> BUILD_RANK_UP = register("build_rank_up");
    public static final RegEntry<SoundEvent, SoundEvent> APOCALYPSE_CRIT = register("apocalypse_crit");

    // ===== 3.3 world: the music of the Witches' Road, one arrangement for each stage (a resource pack can replace any) =====
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_ENTRANCE = register("music.road.entrance");
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_PARKOUR = register("music.road.parkour");
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_COPY = register("music.road.copy");
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_BOAT = register("music.road.boat");
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_ENDING = register("music.road.ending");
    // ===== 3.5 music: the Road's version for the choice, and the speedsters' own Walkman songs (original synth-pop) =====
    public static final RegEntry<SoundEvent, SoundEvent> ROAD_MUSIC_CHOICE = register("music.road.choice");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_QUICKSILVER = register("music.quicksilver");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_SPEED = register("music.speed");
    // ===== cyclops (3.3) =====
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_OPTIC_START = register("cyclops_optic_start");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_OPTIC_LOOP = register("cyclops_optic_loop");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_UNLEASHED_START = register("cyclops_unleashed_start");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_UNLEASHED_LOOP = register("cyclops_unleashed_loop");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_RICOCHET = register("cyclops_ricochet");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_CONCUSSION = register("cyclops_concussion");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_LEAP = register("cyclops_leap");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_RALLY = register("cyclops_rally");
    public static final RegEntry<SoundEvent, SoundEvent> CYCLOPS_VISOR = register("cyclops_visor");
    // ===== 3.3 custom heroes (the Hero Creator's sounds and the custom powers') =====
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_AWAKEN = register("custom_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_BLAST = register("custom_blast");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_ZAP = register("custom_zap");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_IMPACT = register("custom_impact");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_HEAL = register("custom_heal");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_SHIFT = register("custom_shift");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_WHOOSH = register("custom_whoosh");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_TELEPORT = register("custom_teleport");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_SHIELD = register("custom_shield");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_SPELL = register("custom_spell");
    public static final RegEntry<SoundEvent, SoundEvent> CUSTOM_SCREAM = register("custom_scream");
    public static final RegEntry<SoundEvent, SoundEvent> HERO_CREATOR_OPEN = register("hero_creator_open");
    // ===== professor_x (3.3) =====
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_AWAKEN = register("xavier_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_BLAST = register("xavier_blast");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_THOUGHTS = register("xavier_thoughts");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_LINK = register("xavier_link");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_UNLINK = register("xavier_unlink");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_BACKLASH = register("xavier_backlash");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_FREEZE = register("xavier_freeze");
    public static final RegEntry<SoundEvent, SoundEvent> XAVIER_CEREBRO = register("xavier_cerebro");
    // ===== agatha (3.3) =====
    /** The Darkhold's whispers rise round the one who reads it, and Agatha wakes. */
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_AWAKEN = register("agatha_awaken");
    /** The Power Drain: a hollow, sucking swell drawn into her hands. */
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_DRAIN = register("agatha_drain");
    /** The Rune Trap is laid: a low choir chord and a ring of chimes. */
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_RUNES = register("agatha_runes");
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_SHIELD_UP = register("agatha_shield_up");
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_SHIELD_HIT = register("agatha_shield_hit");
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_SHIELD_BREAK = register("agatha_shield_break");

    // ===== the Mutant Cure (3.3.1) =====
    /** The cure goes in: the hiss of the injector and a hollow tone falling away. */
    public static final RegEntry<SoundEvent, SoundEvent> MUTANT_CURE = register("mutant_cure");

    // ===== asgard (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_ASGARD = register("music.asgard");
    public static final RegEntry<SoundEvent, SoundEvent> AMBIENT_ASGARD_LOOP = register("ambient.asgard.loop");
    public static final RegEntry<SoundEvent, SoundEvent> AMBIENT_ASGARD_ADDITIONS = register("ambient.asgard.additions");
    public static final RegEntry<SoundEvent, SoundEvent> ASGARD_FALLS = register("asgard_falls");
    public static final RegEntry<SoundEvent, SoundEvent> BIFROST_MECHANISM_SPIN = register("bifrost_mechanism_spin");
    public static final RegEntry<SoundEvent, SoundEvent> RUNE_STONE_CALL = register("rune_stone_call");
    public static final RegEntry<SoundEvent, SoundEvent> THRONE_SIT = register("throne_sit");
    public static final RegEntry<SoundEvent, SoundEvent> CORONATION = register("coronation");
    public static final RegEntry<SoundEvent, SoundEvent> MURAL_CRACK = register("mural_crack");
    public static final RegEntry<SoundEvent, SoundEvent> SEAL_BREAK = register("seal_break");

    // ===== heimdall (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_SLASH = register("heimdall_slash");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_LUNGE = register("heimdall_lunge");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_SIGHT_ON = register("heimdall_sight_on");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_SIGHT_OFF = register("heimdall_sight_off");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_FARSIGHT = register("heimdall_farsight");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_WARD_UP = register("heimdall_ward_up");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_WARD_DOWN = register("heimdall_ward_down");
    public static final RegEntry<SoundEvent, SoundEvent> HEIMDALL_CALL = register("heimdall_call");
    public static final RegEntry<SoundEvent, SoundEvent> BIFROST_STRIKE = register("bifrost_strike");
    public static final RegEntry<SoundEvent, SoundEvent> HOFUND_RETURN = register("hofund_return");
    public static final RegEntry<SoundEvent, SoundEvent> WATCH_START = register("watch_start");
    public static final RegEntry<SoundEvent, SoundEvent> WATCH_WIN = register("watch_win");
    public static final RegEntry<SoundEvent, SoundEvent> WATCH_LOSE = register("watch_lose");
    public static final RegEntry<SoundEvent, SoundEvent> SHADOW_STRUCK = register("shadow_struck");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_WATCH = register("music.watch");

    // ===== odin (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> GUNGNIR_BLAST = register("gungnir_blast");
    public static final RegEntry<SoundEvent, SoundEvent> GUNGNIR_THROW = register("gungnir_throw");
    public static final RegEntry<SoundEvent, SoundEvent> GUNGNIR_CATCH = register("gungnir_catch");
    public static final RegEntry<SoundEvent, SoundEvent> ODINFORCE_SHIELD_UP = register("odinforce_shield_up");
    public static final RegEntry<SoundEvent, SoundEvent> ODINFORCE_SHIELD_DOWN = register("odinforce_shield_down");
    public static final RegEntry<SoundEvent, SoundEvent> ODINFORCE_HEAL = register("odinforce_heal");
    public static final RegEntry<SoundEvent, SoundEvent> BANISH = register("banish");
    public static final RegEntry<SoundEvent, SoundEvent> ODINFORCE_WAVE = register("odinforce_wave");
    public static final RegEntry<SoundEvent, SoundEvent> RAVEN_CAW = register("raven_caw");
    public static final RegEntry<SoundEvent, SoundEvent> RAVEN_FLAP = register("raven_flap");
    public static final RegEntry<SoundEvent, SoundEvent> ODIN_COMMAND = register("odin_command");
    public static final RegEntry<SoundEvent, SoundEvent> ODINSLEEP_START = register("odinsleep_start");
    public static final RegEntry<SoundEvent, SoundEvent> ODINSLEEP_END = register("odinsleep_end");

    // ===== hela (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> HELA_ARRIVE = register("hela_arrive");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_VOICE = register("hela_voice");
    public static final RegEntry<SoundEvent, SoundEvent> NECROSWORD_THROW = register("necrosword_throw");
    public static final RegEntry<SoundEvent, SoundEvent> NECROSWORD_HIT = register("necrosword_hit");
    public static final RegEntry<SoundEvent, SoundEvent> BLADE_WALL = register("blade_wall");
    public static final RegEntry<SoundEvent, SoundEvent> BLADE_RAIN = register("blade_rain");
    public static final RegEntry<SoundEvent, SoundEvent> BLADE_STORM = register("blade_storm");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_SHADOW_STEP = register("hela_shadow_step");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_CHANNEL = register("hela_channel");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_CHANNEL_BREAK = register("hela_channel_break");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_FORM_ON = register("hela_form_on");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_FORM_OFF = register("hela_form_off");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_GRASP = register("hela_grasp");
    public static final RegEntry<SoundEvent, SoundEvent> MJOLNIR_CAUGHT = register("mjolnir_caught");
    public static final RegEntry<SoundEvent, SoundEvent> HELA_DEFEAT = register("hela_defeat");
    public static final RegEntry<SoundEvent, SoundEvent> BERSERKER_RISE = register("berserker_rise");
    public static final RegEntry<SoundEvent, SoundEvent> BERSERKER_AMBIENT = register("berserker_ambient");
    public static final RegEntry<SoundEvent, SoundEvent> BERSERKER_HURT = register("berserker_hurt");
    public static final RegEntry<SoundEvent, SoundEvent> BERSERKER_DEATH = register("berserker_death");
    public static final RegEntry<SoundEvent, SoundEvent> FENRIS_GROWL = register("fenris_growl");
    public static final RegEntry<SoundEvent, SoundEvent> FENRIS_HOWL = register("fenris_howl");
    public static final RegEntry<SoundEvent, SoundEvent> FENRIS_BITE = register("fenris_bite");
    public static final RegEntry<SoundEvent, SoundEvent> FENRIS_HURT = register("fenris_hurt");
    public static final RegEntry<SoundEvent, SoundEvent> FENRIS_DEATH = register("fenris_death");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_HELA_1 = register("music.hela_1");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_HELA_2 = register("music.hela_2");
    public static final RegEntry<SoundEvent, SoundEvent> MUSIC_HELA_3 = register("music.hela_3");

    // ===== asgardians (3.5: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> NPC_TALK_LOW = register("npc_talk_low");
    public static final RegEntry<SoundEvent, SoundEvent> NPC_TALK_MID = register("npc_talk_mid");
    public static final RegEntry<SoundEvent, SoundEvent> NPC_TALK_HIGH = register("npc_talk_high");
    public static final RegEntry<SoundEvent, SoundEvent> NPC_SPAR_CLASH = register("npc_spar_clash");
    public static final RegEntry<SoundEvent, SoundEvent> NPC_BOW = register("npc_bow");
    public static final RegEntry<SoundEvent, SoundEvent> NPC_DOWN = register("npc_down");
    public static final RegEntry<SoundEvent, SoundEvent> EINHERJAR_SUMMON = register("einherjar_summon");

    // ===== stones (3.6: registered by the skeleton with placeholder files; lane L6 makes them; the six voices and
    // hums, time_rewind and reality_pop exist) =====
    public static final RegEntry<SoundEvent, SoundEvent> STONE_REJECT = register("stone_reject");
    public static final RegEntry<SoundEvent, SoundEvent> STONE_SLIP = register("stone_slip");
    public static final RegEntry<SoundEvent, SoundEvent> STONE_GRACE_TICK = register("stone_grace_tick");
    public static final RegEntry<SoundEvent, SoundEvent> REALITY_ILLUSION = register("reality_illusion");
    /** An alias of an existing sound (enhanced:sfx/mind_stone_beam_loop): The Mind Stone's beam hums. */
    public static final RegEntry<SoundEvent, SoundEvent> MIND_BEAM_LOOP = register("mind_beam_loop");
    public static final RegEntry<SoundEvent, SoundEvent> MIND_COMMAND = register("mind_command");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_SLOW_ON = register("time_slow_on");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_SLOW_OFF = register("time_slow_off");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_SLOW_LOOP = register("time_slow_loop");
    public static final RegEntry<SoundEvent, SoundEvent> TIME_CROP = register("time_crop");
    public static final RegEntry<SoundEvent, SoundEvent> POWER_BEAM_LOOP = register("power_beam_loop");
    public static final RegEntry<SoundEvent, SoundEvent> POWER_EXPLOSION = register("power_explosion");
    public static final RegEntry<SoundEvent, SoundEvent> POWER_BOLT = register("power_bolt");
    public static final RegEntry<SoundEvent, SoundEvent> SPACE_STEP = register("space_step");
    public static final RegEntry<SoundEvent, SoundEvent> SPACE_REALM_DEPART = register("space_realm_depart");
    public static final RegEntry<SoundEvent, SoundEvent> SPACE_REALM_ARRIVE = register("space_realm_arrive");
    /** An alias of an existing sound (enhanced:sfx/soul_drain_loop): A soul is drained. */
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_DRAIN_LOOP = register("soul_drain_loop");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_SIGHT = register("soul_sight");
    public static final RegEntry<SoundEvent, SoundEvent> ORB_OPEN = register("orb_open");
    public static final RegEntry<SoundEvent, SoundEvent> PEDESTAL_TAKE = register("pedestal_take");
    public static final RegEntry<SoundEvent, SoundEvent> GAUNTLET_INSERT = register("gauntlet_insert");
    public static final RegEntry<SoundEvent, SoundEvent> GAUNTLET_SELECT = register("gauntlet_select");
    public static final RegEntry<SoundEvent, SoundEvent> SNAP_CHARGE = register("snap_charge");
    public static final RegEntry<SoundEvent, SoundEvent> SNAP = register("snap");
    public static final RegEntry<SoundEvent, SoundEvent> SNAP_DUST = register("snap_dust");

    // ===== strange (3.6: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_AWAKEN = register("strange_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_WHIP = register("strange_whip");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_GRAPPLE = register("strange_grapple");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_SHIELD_UP = register("strange_shield_up");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_SHIELD_DOWN = register("strange_shield_down");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_DOME = register("strange_dome");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_BANDS = register("strange_bands");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_ASTRAL_OUT = register("strange_astral_out");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_ASTRAL_IN = register("strange_astral_in");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_PORTAL_OPEN = register("strange_portal_open");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_PORTAL_CLOSE = register("strange_portal_close");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_PORTAL_PASS = register("strange_portal_pass");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_MIRROR_IN = register("strange_mirror_in");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_MIRROR_OUT = register("strange_mirror_out");
    public static final RegEntry<SoundEvent, SoundEvent> STRANGE_MIRROR_LOOP = register("strange_mirror_loop");
    public static final RegEntry<SoundEvent, SoundEvent> EYE_OPEN = register("eye_open");
    public static final RegEntry<SoundEvent, SoundEvent> EYE_CLOSE = register("eye_close");
    public static final RegEntry<SoundEvent, SoundEvent> CLOAK_FLAP = register("cloak_flap");
    public static final RegEntry<SoundEvent, SoundEvent> SLING_RING_SAVE = register("sling_ring_save");

    // ===== stone_sites (3.6: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> AMBIENT_SANCTUM = register("ambient.sanctum");
    public static final RegEntry<SoundEvent, SoundEvent> END_TUNNEL_HUM = register("end_tunnel_hum");

    // ===== vormir (3.6: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> VORMIR_WIND = register("vormir_wind");
    public static final RegEntry<SoundEvent, SoundEvent> SOUL_SACRIFICE = register("soul_sacrifice");
    public static final RegEntry<SoundEvent, SoundEvent> STONEKEEPER_VOICE = register("stonekeeper_voice");

    // ===== hulk (3.6: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> HULK_TRANSFORM_OUT = register("hulk_transform_out");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_TRANSFORM_IN = register("hulk_transform_in");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_SWITCH = register("hulk_switch");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_ROAR = register("hulk_roar");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_SMASH = register("hulk_smash");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_GRAB = register("hulk_grab");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_SLAM = register("hulk_slam");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_CLAP = register("hulk_clap");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_LEAP = register("hulk_leap");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_LAND = register("hulk_land");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_RIP = register("hulk_rip");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_THROW = register("hulk_throw");
    public static final RegEntry<SoundEvent, SoundEvent> BOULDER_HIT = register("boulder_hit");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_CHARGE = register("hulk_charge");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_STEP = register("hulk_step");
    public static final RegEntry<SoundEvent, SoundEvent> HULK_HURT = register("hulk_hurt");
    public static final RegEntry<SoundEvent, SoundEvent> BANNER_HEARTBEAT = register("banner_heartbeat");
    public static final RegEntry<SoundEvent, SoundEvent> GAMMA_EMITTER_WARMUP = register("gamma_emitter_warmup");
    public static final RegEntry<SoundEvent, SoundEvent> GAMMA_EMITTER_BEAM = register("gamma_emitter_beam");
    public static final RegEntry<SoundEvent, SoundEvent> GAMMA_EXPOSURE = register("gamma_exposure");

    // ===== hydra (3.6: registered by the skeleton with placeholder files; lane L6 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_RIFLE_BURST = register("hydra_rifle_burst");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_BOLT_HIT = register("hydra_bolt_hit");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_SNIPER_AIM = register("hydra_sniper_aim");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_SNIPER_SHOT = register("hydra_sniper_shot");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_PISTOL = register("hydra_pistol");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_BATON = register("hydra_baton");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_SHIELD_BLOCK = register("hydra_shield_block");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_BARK = register("hydra_bark");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_RADIO = register("hydra_radio");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_ALARM = register("hydra_alarm");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_RELOAD = register("hydra_reload");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_HURT = register("hydra_hurt");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_DEATH = register("hydra_death");
    public static final RegEntry<SoundEvent, SoundEvent> HYDRA_BLASTER = register("hydra_blaster");

    // ===== healing (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_DOWN = register("healing_down");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_RISE = register("healing_rise");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_BURST = register("healing_burst");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_OFF = register("healing_off");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_ON = register("healing_on");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_CUT = register("healing_cut");
    public static final RegEntry<SoundEvent, SoundEvent> HEALING_RESCUE = register("healing_rescue");

    // ===== wolverine (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SNIKT = register("wolverine_snikt");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SHINK = register("wolverine_shink");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SLASH = register("wolverine_slash");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SLASH_HIT_METAL =
            register("wolverine_slash_hit_metal");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SPIN = register("wolverine_spin");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_POUNCE = register("wolverine_pounce");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_LAND = register("wolverine_land");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_BERSERK = register("wolverine_berserk");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_GROWL = register("wolverine_growl");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_SNIFF = register("wolverine_sniff");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_TRACK = register("wolverine_track");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_CLIMB = register("wolverine_climb");
    public static final RegEntry<SoundEvent, SoundEvent> ADAMANTIUM_CLANK = register("adamantium_clank");
    public static final RegEntry<SoundEvent, SoundEvent> ADAMANTIUM_INJECTION = register("adamantium_injection");
    public static final RegEntry<SoundEvent, SoundEvent> WOLVERINE_AWAKEN = register("wolverine_awaken");

    // ===== deadpool (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_KATANA = register("deadpool_katana");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_PARRY = register("deadpool_parry");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_DEFLECT = register("deadpool_deflect");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_SHOT = register("deadpool_shot");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_RELOAD = register("deadpool_reload");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_RICOCHET = register("deadpool_ricochet");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_BLINK = register("deadpool_blink");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_GLITCH = register("deadpool_glitch");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_DROP_IN = register("deadpool_drop_in");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_TAUNT = register("deadpool_taunt");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_WHISTLE = register("deadpool_whistle");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_SMOKE = register("deadpool_smoke");
    public static final RegEntry<SoundEvent, SoundEvent> DEADPOOL_AWAKEN = register("deadpool_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> DOGPOOL_BARK = register("dogpool_bark");
    public static final RegEntry<SoundEvent, SoundEvent> DOGPOOL_WHINE = register("dogpool_whine");
    public static final RegEntry<SoundEvent, SoundEvent> DOGPOOL_POOF = register("dogpool_poof");
    public static final RegEntry<SoundEvent, SoundEvent> CHIMICHANGA_MUNCH = register("chimichanga_munch");

    // ===== school_lab (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> SCHOOL_LAB_READY = register("school_lab_ready");
    public static final RegEntry<SoundEvent, SoundEvent> SECRET_SHELF_OPEN = register("secret_shelf_open");
    public static final RegEntry<SoundEvent, SoundEvent> SECRET_SHELF_CLOSE = register("secret_shelf_close");
    public static final RegEntry<SoundEvent, SoundEvent> LIFT_RIDE = register("lift_ride");
    public static final RegEntry<SoundEvent, SoundEvent> LIFT_DING = register("lift_ding");
    public static final RegEntry<SoundEvent, SoundEvent> X_DOOR_OPEN = register("x_door_open");
    public static final RegEntry<SoundEvent, SoundEvent> X_DOOR_CLOSE = register("x_door_close");
    public static final RegEntry<SoundEvent, SoundEvent> WEAPON_X_POD_HUM = register("weapon_x_pod_hum");
    public static final RegEntry<SoundEvent, SoundEvent> WEAPON_X_POD_DONE = register("weapon_x_pod_done");
    public static final RegEntry<SoundEvent, SoundEvent> ADAMANTIUM_RACK_TAKE = register("adamantium_rack_take");
    public static final RegEntry<SoundEvent, SoundEvent> CURE_STATION = register("cure_station");
    public static final RegEntry<SoundEvent, SoundEvent> AMBIENT_SCHOOL_LAB = register("ambient.school_lab");
    public static final RegEntry<SoundEvent, SoundEvent> CEREBRO_CONSOLE_ON = register("cerebro_console_on");
    public static final RegEntry<SoundEvent, SoundEvent> CEREBRO_AMPLIFIED = register("cerebro_amplified");
    public static final RegEntry<SoundEvent, SoundEvent> CEREBRO_CONSOLE_OFF = register("cerebro_console_off");

    // ===== finders (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> LOCATOR_PULSE = register("locator_pulse");
    public static final RegEntry<SoundEvent, SoundEvent> LOCATOR_TICK = register("locator_tick");
    public static final RegEntry<SoundEvent, SoundEvent> LOCATOR_FOUND = register("locator_found");

    // ===== cottage (3.7: registered by the skeleton with placeholder files; lane L7 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> AMBIENT_COTTAGE = register("ambient.cottage");
    public static final RegEntry<SoundEvent, SoundEvent> COTTAGE_POT = register("cottage_pot");
    public static final RegEntry<SoundEvent, SoundEvent> SCRATCHY_FOUND = register("scratchy_found");
    public static final RegEntry<SoundEvent, SoundEvent> RIO_BLESSING = register("rio_blessing");
    public static final RegEntry<SoundEvent, SoundEvent> NICKY_CHEER = register("nicky_cheer");
    public static final RegEntry<SoundEvent, SoundEvent> AGATHA_AWAY = register("agatha_away");

    // ===== serum (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> SERUM_OPEN = register("serum_open");
    public static final RegEntry<SoundEvent, SoundEvent> SERUM_CHOOSE = register("serum_choose");
    public static final RegEntry<SoundEvent, SoundEvent> SERUM_LOCKED = register("serum_locked");

    // ===== captain (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_THROW = register("shield_throw");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_RICOCHET = register("shield_ricochet");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_CATCH = register("shield_catch");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_BLOCK = register("shield_block");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_REFLECT = register("shield_reflect");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_SLAM = register("shield_slam");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_CHARGE = register("shield_charge");
    public static final RegEntry<SoundEvent, SoundEvent> SHIELD_BASH = register("shield_bash");
    public static final RegEntry<SoundEvent, SoundEvent> CAP_ASSEMBLE = register("cap_assemble");
    public static final RegEntry<SoundEvent, SoundEvent> CAP_INSPIRE = register("cap_inspire");
    public static final RegEntry<SoundEvent, SoundEvent> CAP_PUNCH = register("cap_punch");
    public static final RegEntry<SoundEvent, SoundEvent> CAP_FLIP = register("cap_flip");
    public static final RegEntry<SoundEvent, SoundEvent> CAPTAIN_AWAKEN = register("captain_awaken");

    // ===== red_hulk (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_ROAR = register("red_hulk_roar");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_SMASH = register("red_hulk_smash");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_HEAT_WAVE = register("red_hulk_heat_wave");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_JUMP_CHARGE = register("red_hulk_jump_charge");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_JUMP = register("red_hulk_jump");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_LAND = register("red_hulk_land");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_BURST = register("red_hulk_burst");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_STEAM = register("red_hulk_steam");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_ABSORB = register("red_hulk_absorb");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_TRANSFORM = register("red_hulk_transform");
    public static final RegEntry<SoundEvent, SoundEvent> RED_HULK_AWAKEN = register("red_hulk_awaken");

    // ===== iron_man (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> ARC_REACTOR_INSERT = register("arc_reactor_insert");
    public static final RegEntry<SoundEvent, SoundEvent> ARC_REACTOR_HUM = register("arc_reactor_hum");
    public static final RegEntry<SoundEvent, SoundEvent> IRON_MAN_AWAKEN = register("iron_man_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> SUIT_ENERGY_LOW = register("suit_energy_low");
    public static final RegEntry<SoundEvent, SoundEvent> SUIT_ENERGY_EMPTY = register("suit_energy_empty");
    public static final RegEntry<SoundEvent, SoundEvent> REPULSOR_CHARGE = register("repulsor_charge");
    public static final RegEntry<SoundEvent, SoundEvent> REPULSOR_BLAST = register("repulsor_blast");
    public static final RegEntry<SoundEvent, SoundEvent> DOUBLE_BLAST = register("double_blast");
    public static final RegEntry<SoundEvent, SoundEvent> UNIBEAM_START = register("unibeam_start");
    public static final RegEntry<SoundEvent, SoundEvent> UNIBEAM = register("unibeam");
    public static final RegEntry<SoundEvent, SoundEvent> FLARES = register("flares");
    public static final RegEntry<SoundEvent, SoundEvent> MINI_MISSILE_LAUNCH = register("mini_missile_launch");
    public static final RegEntry<SoundEvent, SoundEvent> MINI_MISSILE_BOOM = register("mini_missile_boom");
    public static final RegEntry<SoundEvent, SoundEvent> IRON_THRUSTERS = register("iron_thrusters");
    public static final RegEntry<SoundEvent, SoundEvent> IRON_BOOST = register("iron_boost");
    public static final RegEntry<SoundEvent, SoundEvent> IRON_CRASH = register("iron_crash");
    public static final RegEntry<SoundEvent, SoundEvent> MARK1_FLAMETHROWER = register("mark1_flamethrower");
    public static final RegEntry<SoundEvent, SoundEvent> MARK1_ARM_ROCKET = register("mark1_arm_rocket");
    public static final RegEntry<SoundEvent, SoundEvent> MARK1_CLANK = register("mark1_clank");
    public static final RegEntry<SoundEvent, SoundEvent> MARK3_GANTRY = register("mark3_gantry");
    public static final RegEntry<SoundEvent, SoundEvent> MARK5_SUITCASE_OPEN = register("mark5_suitcase_open");
    public static final RegEntry<SoundEvent, SoundEvent> MARK5_SUITCASE_CLOSE = register("mark5_suitcase_close");
    public static final RegEntry<SoundEvent, SoundEvent> DASH_PUNCH = register("dash_punch");
    public static final RegEntry<SoundEvent, SoundEvent> LASER_BURST = register("laser_burst");
    public static final RegEntry<SoundEvent, SoundEvent> IRON_HUD_BOOT = register("iron_hud_boot");
    public static final RegEntry<SoundEvent, SoundEvent> SUIT_UP_GENERIC = register("suit_up_generic");
    public static final RegEntry<SoundEvent, SoundEvent> SUIT_DOWN_GENERIC = register("suit_down_generic");

    // ===== nanotech (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> NANO_FLOW = register("nano_flow");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_RETRACT = register("nano_retract");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_BLADE_OUT = register("nano_blade_out");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_BLADE_IN = register("nano_blade_in");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_SHIELD = register("nano_shield");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_CANNON_CHARGE = register("nano_cannon_charge");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_CANNON_FIRE = register("nano_cannon_fire");
    public static final RegEntry<SoundEvent, SoundEvent> ENERGY_BLADE_SWING = register("energy_blade_swing");
    public static final RegEntry<SoundEvent, SoundEvent> ENERGY_SHIELD_HIT = register("energy_shield_hit");
    public static final RegEntry<SoundEvent, SoundEvent> LIGHTNING_REFOCUSER = register("lightning_refocuser");
    public static final RegEntry<SoundEvent, SoundEvent> HOMING_LOCK = register("homing_lock");
    public static final RegEntry<SoundEvent, SoundEvent> NANO_DRONES_DEPLOY = register("nano_drones_deploy");
    public static final RegEntry<SoundEvent, SoundEvent> DRONE_LASER = register("drone_laser");
    public static final RegEntry<SoundEvent, SoundEvent> CHEST_LASER = register("chest_laser");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_DROP = register("hulkbuster_drop");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_OPEN = register("hulkbuster_open");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_CLOSE = register("hulkbuster_close");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_STEP = register("hulkbuster_step");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_PUNCH = register("hulkbuster_punch");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_GRAB = register("hulkbuster_grab");
    public static final RegEntry<SoundEvent, SoundEvent> HULKBUSTER_THROW = register("hulkbuster_throw");
    public static final RegEntry<SoundEvent, SoundEvent> VERONICA_ARM = register("veronica_arm");

    // ===== falcon (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_WINGS_OPEN = register("falcon_wings_open");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_WINGS_CLOSE = register("falcon_wings_close");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_THRUST = register("falcon_thrust");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_DIVE = register("falcon_dive");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_DIVE_HIT = register("falcon_dive_hit");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_CLIMB = register("falcon_climb");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_DART = register("falcon_dart");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_ROCKET = register("falcon_rocket");
    public static final RegEntry<SoundEvent, SoundEvent> WING_SHIELD_UP = register("wing_shield_up");
    public static final RegEntry<SoundEvent, SoundEvent> WING_SHIELD_DEFLECT = register("wing_shield_deflect");
    public static final RegEntry<SoundEvent, SoundEvent> WING_SLASH = register("wing_slash");
    public static final RegEntry<SoundEvent, SoundEvent> REDWING_LAUNCH = register("redwing_launch");
    public static final RegEntry<SoundEvent, SoundEvent> REDWING_BUZZ = register("redwing_buzz");
    public static final RegEntry<SoundEvent, SoundEvent> REDWING_ZAP = register("redwing_zap");
    public static final RegEntry<SoundEvent, SoundEvent> REDWING_STRIKE = register("redwing_strike");
    public static final RegEntry<SoundEvent, SoundEvent> REDWING_RETURN = register("redwing_return");
    public static final RegEntry<SoundEvent, SoundEvent> FALCON_AWAKEN = register("falcon_awaken");

    // ===== tower (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> SUIT_DISPLAY_TAKE = register("suit_display_take");
    public static final RegEntry<SoundEvent, SoundEvent> TECH_CASE_OPEN = register("tech_case_open");
    public static final RegEntry<SoundEvent, SoundEvent> TECH_CASE_TAKE = register("tech_case_take");
    public static final RegEntry<SoundEvent, SoundEvent> DISPLAY_REFUSE = register("display_refuse");

    // ===== eternity (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> SHRINE_HUM = register("shrine_hum");
    public static final RegEntry<SoundEvent, SoundEvent> ALTAR_PLACE = register("altar_place");
    public static final RegEntry<SoundEvent, SoundEvent> ALTAR_RISE = register("altar_rise");
    public static final RegEntry<SoundEvent, SoundEvent> ETERNITY_ARRIVE = register("eternity_arrive");
    public static final RegEntry<SoundEvent, SoundEvent> ETERNITY_AMBIENT = register("eternity_ambient");
    public static final RegEntry<SoundEvent, SoundEvent> ETERNITY_VOICE = register("eternity_voice");
    public static final RegEntry<SoundEvent, SoundEvent> WISH_GRANTED = register("wish_granted");
    public static final RegEntry<SoundEvent, SoundEvent> ETERNITY_LEAVE = register("eternity_leave");

    // ===== love (3.8: registered by the skeleton with placeholder files; lane L2 makes them) =====
    public static final RegEntry<SoundEvent, SoundEvent> LOVE_AWAKEN = register("love_awaken");
    public static final RegEntry<SoundEvent, SoundEvent> LOVE_EYE_LASER_START = register("love_eye_laser_start");
    public static final RegEntry<SoundEvent, SoundEvent> LOVE_EYE_LASER = register("love_eye_laser");
    public static final RegEntry<SoundEvent, SoundEvent> LOVE_THUNDER_STRIKE = register("love_thunder_strike");

    private static RegEntry<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enhanced.id(name)));
    }

    private ModSounds() {
    }
}
