package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroPresets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Hero Creator's presets as this client has them (3.4): read from the resources ({@link HeroPresets#path}) the first
 * time the Start tab is shown, in the order of {@link HeroPresets#ids()}. A preset that does not read is left out
 * (logged).
 */
public final class PresetBook {
    private static List<HeroPresets.Preset> presets;

    public static synchronized List<HeroPresets.Preset> all() {
        if (presets == null) {
            List<HeroPresets.Preset> list = new ArrayList<>();
            for (String id : HeroPresets.ids()) {
                ResourceLocation path = Enhanced.id(HeroPresets.path(id));
                try (InputStream in = Minecraft.getInstance().getResourceManager().open(path)) {
                    HeroPresets.Preset preset = HeroPresets.read(id, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    if (preset != null) {
                        list.add(preset);
                    } else {
                        Enhanced.LOGGER.warn("The Hero Creator's preset {} is not a hero", id);
                    }
                } catch (Exception e) {
                    Enhanced.LOGGER.warn("The Hero Creator's preset {} could not be read", id, e);
                }
            }
            presets = Collections.unmodifiableList(list);
        }
        return presets;
    }

    private PresetBook() {
    }
}
