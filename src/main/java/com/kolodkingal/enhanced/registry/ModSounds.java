package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Enhanced.MODID);

    /**
     * What Quicksilver's Walkman plays during Quicksilver Time. The mod ships an original placeholder track; a resource
     * pack can replace assets/enhanced/sounds/quicksilver/theme.ogg with any song.
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICKSILVER_THEME = SOUNDS.register("quicksilver_theme",
            () -> SoundEvent.createVariableRangeEvent(Enhanced.id("quicksilver_theme")));

    private ModSounds() {
    }
}
