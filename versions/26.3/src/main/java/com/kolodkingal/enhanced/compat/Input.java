package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.Minecraft;

/** 26.3 only: input that 1.21.1 read from GLFW (26.3's window is SDL's). */
public final class Input {
    private Input() {
    }

    /** Whether a mouse button (InputConstants.MOUSE_BUTTON_*: 26.3 counts from 1, as SDL does) is held. */
    public static boolean mouseDown(int button) {
        var mouse = Minecraft.getInstance().mouseHandler;
        return switch (button) {
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT -> mouse.isLeftPressed();
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE -> mouse.isMiddlePressed();
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT -> mouse.isRightPressed();
            default -> false;
        };
    }
}
