package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.client.KeyBindings;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Build Magic's ten key mappings (D-B 14.6, spec 4.6), registered with the hero keys in {@code ClientSetup.onRegisterKeys}
 * under "Enhanced Powers":
 *
 * <ul>
 *     <li>{@link #MAGIC} (Y, in game): tap, build mode on or off; hold 6 ticks, the Spell Wheel. It does something only for
 *     a Scarlet Witch or Billy.</li>
 *     <li>{@link #UNDO} (Ctrl + Z) and {@link #REDO} (Ctrl + Y) in the {@link Context#WITCH} context: for a Scarlet Witch or
 *     Billy, in build mode or out of it (R42: "undo always one key away").</li>
 *     <li>{@link #MORE} (Left Alt, held: the second knob with the scroll wheel), {@link #TURN} (Ctrl + R), {@link #FLIP}
 *     (Ctrl + F), {@link #COPY} (Ctrl + C), {@link #CUT} (Ctrl + X), {@link #PASTE} (Ctrl + V) and {@link #VANISH}
 *     (Delete) in the {@link Context#BUILD} context: only while build mode is on.</li>
 * </ul>
 *
 * <p>Why the Ctrl shortcuts never press a hero key: with Ctrl held, NeoForge's lookup ({@code KeyMappingLookup.getAll})
 * collects only the active mappings bound with Ctrl, and falls back to the plain key only when it found none. So Ctrl + Z
 * of a witch reaches Undo and never the Vision order on Z, and in build mode Ctrl + R never fires the beam or the bolt and
 * Ctrl + V never presses telekinesis; out of build mode (and for every other hero) the build mappings are inactive and each
 * key is what it was, Ctrl held or not. The mouse buttons and the wheel are vanilla's, taken through the input events (lane
 * BC, D-B 11.10).
 *
 * <p>The Controls screen marks the seven Ctrl shortcuts in red, and the keys they share (Sprint on Left Control, F, C, X,
 * and the hero keys R, V, Z and Y): it compares keys whatever the contexts. The marks are display only (spec 4.6).
 */
public final class BuildKeys {
    /** Y: build mode on or off (tap), the Spell Wheel (hold). */
    public static final KeyMapping MAGIC = new KeyMapping("key.enhanced.build_magic", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, KeyBindings.CATEGORY);
    /** Left Alt, held: the scroll wheel turns the second knob (a style, a shape, a turn, a gap). */
    public static final KeyMapping MORE = create("build_more", Context.BUILD, KeyModifier.NONE, GLFW.GLFW_KEY_LEFT_ALT);
    /** Ctrl + Z: undo (D-B 12.2), in build mode or out of it. */
    public static final KeyMapping UNDO = create("build_undo", Context.WITCH, KeyModifier.CONTROL, GLFW.GLFW_KEY_Z);
    /** Ctrl + Y: redo, in build mode or out of it. */
    public static final KeyMapping REDO = create("build_redo", Context.WITCH, KeyModifier.CONTROL, GLFW.GLFW_KEY_Y);
    /** Ctrl + R: turn a ghost a quarter clockwise. */
    public static final KeyMapping TURN = create("build_turn", Context.BUILD, KeyModifier.CONTROL, GLFW.GLFW_KEY_R);
    /** Ctrl + F: flip a ghost along the way you look. */
    public static final KeyMapping FLIP = create("build_flip", Context.BUILD, KeyModifier.CONTROL, GLFW.GLFW_KEY_F);
    /** Ctrl + C: copy the box (the spell becomes Copy). */
    public static final KeyMapping COPY = create("build_copy", Context.BUILD, KeyModifier.CONTROL, GLFW.GLFW_KEY_C);
    /** Ctrl + X: pick the box up (the spell becomes Move). */
    public static final KeyMapping CUT = create("build_cut", Context.BUILD, KeyModifier.CONTROL, GLFW.GLFW_KEY_X);
    /** Ctrl + V: the pocket's ghost back. */
    public static final KeyMapping PASTE = create("build_paste", Context.BUILD, KeyModifier.CONTROL, GLFW.GLFW_KEY_V);
    /** Delete: vanish everything in the box (with its hold). */
    public static final KeyMapping VANISH = create("build_vanish", Context.BUILD, KeyModifier.NONE, GLFW.GLFW_KEY_DELETE);

    /** The ten, in the order of the spec's table. */
    public static final KeyMapping[] ALL = {MAGIC, MORE, UNDO, REDO, TURN, FLIP, COPY, CUT, PASTE, VANISH};

    /** When the build mappings are read. */
    public enum Context implements IKeyConflictContext {
        /**
         * No screen open and build mode on. It conflicts with itself and with {@code IN_GAME}, so the Controls screen shows
         * a real clash when a build key is rebound onto a hero key, and a build key without Ctrl (Delete, Left Alt) still
         * works while Ctrl or Alt is held.
         */
        BUILD {
            @Override
            public boolean isActive() {
                return KeyConflictContext.IN_GAME.isActive() && BuildClient.buildMode();
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return other == this || other == KeyConflictContext.IN_GAME;
            }
        },
        /**
         * No screen open and the player is a Scarlet Witch or Billy, in build mode or out of it (R42). It conflicts with
         * itself, {@link #BUILD} and {@code IN_GAME}.
         */
        WITCH {
            @Override
            public boolean isActive() {
                LocalPlayer player = Minecraft.getInstance().player;
                return KeyConflictContext.IN_GAME.isActive() && player != null && BuildClient.isBuilder(Powers.power(player));
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return other == this || other == BUILD || other == KeyConflictContext.IN_GAME;
            }
        }
    }

    private static KeyMapping create(String name, IKeyConflictContext context, KeyModifier modifier, int key) {
        return new KeyMapping("key.enhanced." + name, context, modifier, InputConstants.Type.KEYSYM, key, KeyBindings.CATEGORY);
    }

    private BuildKeys() {
    }
}
