package com.kolodkingal.enhanced.compat;
//#norenames

import org.joml.Matrix3x2fStack;
import org.joml.Quaternionfc;

/**
 * 1.21.11 only: from 1.21.6 the GUI is drawn in 2D: {@code GuiGraphics.pose()} is a 2D matrix stack. The mod's GUI
 * moves, scales and turns in the screen's plane only; these take 1.21.1's three-axis calls and keep the plane's part.
 */
public final class GuiPose {
    private GuiPose() {
    }

    public static void translate(Matrix3x2fStack pose, float x, float y, float z) {
        pose.translate(x, y);
    }

    public static void translate(Matrix3x2fStack pose, double x, double y, double z) {
        pose.translate((float) x, (float) y);
    }

    public static void scale(Matrix3x2fStack pose, float x, float y, float z) {
        pose.scale(x, y);
    }

    /** A turn about the screen's normal (what a GUI's {@code Axis.ZP.rotation(...)} was). */
    public static void mulPose(Matrix3x2fStack pose, Quaternionfc q) {
        pose.rotate((float) (2.0 * Math.atan2(q.z(), q.w())));
    }
}
