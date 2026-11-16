package com.kolodkingal.enhanced.platform.client;

/**
 * (3.16) The mouse's buttons by number. The mod's screens were written against 1.21.1's (GLFW's) numbers: left 0, right
 * 1, middle 2, then 3 and 4. 26.1 moved to SDL's: left 1, middle 2, right 3, then 4 and 5, so on 26.3 a left click came in
 * as "1" and every screen that waits for 0 ignored it. The 26.3 renames turn the event's number into 1.21.1's where a
 * screen reads it ({@link #legacy}), and back where a screen hands one to vanilla ({@link #modern}); on every other
 * version neither is called.
 */
public final class MouseButtons {
    private MouseButtons() {
    }

    /** 1.21.1's number of a button 26.1 numbers {@code sdl}. */
    public static int legacy(int sdl) {
        return switch (sdl) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> sdl > 3 ? sdl - 1 : sdl;
        };
    }

    /** 26.1's number of a button 1.21.1 numbers {@code glfw}. */
    public static int modern(int glfw) {
        return switch (glfw) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 2;
            default -> glfw > 2 ? glfw + 1 : glfw;
        };
    }
}
