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
     * What Quicksilver's Walkman plays during Quicksilver Time: "Sweet Dreams (Are Made of This)". A player can play
     * another song instead from config/enhanced/music (see CustomMusic).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICKSILVER_THEME = register("quicksilver_theme");

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN = register("awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAOS_BEAM = register("chaos_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAOS_BEAM_START = register("chaos_beam_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_RISE = register("hex_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEX_CROSS = register("hex_cross");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_GRAB = register("magnet_grab");
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

    // ===== spider =====

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

    // ===== apocalypse =====

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enhanced.id(name)));
    }

    private ModSounds() {
    }
}
