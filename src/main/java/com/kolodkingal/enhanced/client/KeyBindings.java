package com.kolodkingal.enhanced.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
// TODO(port seam) S1: a common key conflict context (notes/l6.md); NeoForge's until it lands.
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Ability 1-5 keys (default R, G, V, B, N). Each triggers the ability with the same number in the hero's list. The
 * Change Look key (default H) cycles through the hero's looks (Shift+H goes back). The Vision key (default Z) is the
 * Scarlet Witch's order to the Vision her Hex raised: "Vision, that one!" (Shift+Z: hold your post, or follow me).
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.enhanced";

    public static final KeyMapping[] ABILITIES = {
            create(1, GLFW.GLFW_KEY_R),
            create(2, GLFW.GLFW_KEY_G),
            create(3, GLFW.GLFW_KEY_V),
            create(4, GLFW.GLFW_KEY_B),
            create(5, GLFW.GLFW_KEY_N),
    };

    public static final KeyMapping CHANGE_LOOK = new KeyMapping("key.enhanced.change_look", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping VISION_COMMAND = new KeyMapping("key.enhanced.vision_command", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    /**
     * 3.6: in game, like {@code IN_GAME}, but in conflict with no other context (see {@link #STONE_WHEEL}). Declared
     * before it: a static field is set in the order of the class, and the wheel's mapping needs its context.
     */
    public static final IKeyConflictContext STONES = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return KeyConflictContext.IN_GAME.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };

    /**
     * 3.6: the Stone Wheel (default J): a tap picks the next stone of the Infinity Gauntlet in the hand, a hold opens
     * the wheel of its stones (lane L1 reads it). J is also Spider-Man's Web Assist: the wheel's context, {@link
     * #STONES}, conflicts with nothing, so both mappings hear the key and each acts only for its own player (a Gauntlet
     * in the hand, Spider-Man's swing).
     */
    public static final KeyMapping STONE_WHEEL = new KeyMapping("key.enhanced.stone_wheel", KeyBindings.STONES,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    private static KeyMapping create(int ability, int key) {
        return new KeyMapping("key.enhanced.ability_" + ability, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    /** The key for ability 1-5. */
    public static KeyMapping ability(int ability) {
        return ABILITIES[ability - 1];
    }

    private KeyBindings() {
    }
}
