package com.kolodkingal.enhanced.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Makes the mod's key mappings with a {@link KeyContext} and a {@link KeyMod} (port P0, PLAN2 3.3), the loader's way
 * (NeoForge: {@code new KeyMapping(name, context, modifier, type, keyCode, category)}). Safe in static initialisers:
 * the loader's services are found on first use, nothing has to be installed first. Register the mapping as before
 * ({@link ClientRegistrar#keyMapping}). Client only.
 */
public final class Keys {
    private Keys() {
    }

    /** A key mapping read in {@code context}, pressed with {@code modifier} held. */
    public static KeyMapping newKey(String name, KeyContext context, KeyMod modifier, InputConstants.Type type,
                                    int keyCode, String category) {
        return ClientServices.get().newKey(name, context, modifier, type, keyCode, category);
    }

    /** A key mapping read in {@code context}, with no modifier. */
    public static KeyMapping newKey(String name, KeyContext context, InputConstants.Type type, int keyCode,
                                    String category) {
        return newKey(name, context, KeyMod.NONE, type, keyCode, category);
    }
}
