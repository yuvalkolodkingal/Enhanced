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

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enhanced.id(name)));
    }

    private ModSounds() {
    }
}
