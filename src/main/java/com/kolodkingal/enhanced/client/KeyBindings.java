package com.kolodkingal.enhanced.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
