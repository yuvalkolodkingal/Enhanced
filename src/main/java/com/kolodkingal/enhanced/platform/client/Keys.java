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

    /**
     * Whether {@code key} pressed now is {@code mapping}'s: its context is active, the key is its bound key and its
     * modifier is held (NeoForge: {@code isActiveAndMatches}).
     */
    public static boolean matches(KeyMapping mapping, InputConstants.Key key) {
        return ClientServices.get().matches(mapping, key);
    }

    /** The key {@code mapping} is bound to now (NeoForge: {@code getKey()}). */
    public static InputConstants.Key boundKey(KeyMapping mapping) {
        return ClientServices.get().boundKey(mapping);
    }
}
