package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own sound effects (all original, synthesized) and Quicksilver's theme. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Enhanced.MODID);

    /**
     * What Quicksilver's Walkman plays during Quicksilver Time. The mod ships an original placeholder track; each player
     * can play their own copy of the song from config/enhanced/music (see CustomMusic).
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
    public static final DeferredHolder<SoundEvent, SoundEvent> PHOTON_BLAST = register("photon_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENERGY_RELEASE = register("energy_release");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enhanced.id(name)));
    }

    private ModSounds() {
    }
}
