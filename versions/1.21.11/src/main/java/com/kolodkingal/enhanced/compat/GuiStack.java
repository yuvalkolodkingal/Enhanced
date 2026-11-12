package com.kolodkingal.enhanced.compat;
//#norenames

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

/**
 * 1.21.11 only: a GUI's pose kept in a variable by the mod's code ({@code PoseStack pose = g.pose()}) is a 2D matrix
 * stack from 1.21.6; this keeps 1.21.1's calls on it (in the screen's plane, see {@link GuiPose}).
 */
public final class GuiStack {
    private final Matrix3x2fStack stack;

    private GuiStack(Matrix3x2fStack stack) {
        this.stack = stack;
    }

    public static GuiStack of(Matrix3x2fStack stack) {
        return new GuiStack(stack);
    }

    public Matrix3x2fStack stack() {
        return stack;
    }

    public void pushPose() {
        stack.pushMatrix();
    }

    public void popPose() {
        stack.popMatrix();
    }

    public void translate(float x, float y, float z) {
        stack.translate(x, y);
    }

    public void translate(double x, double y, double z) {
        stack.translate((float) x, (float) y);
    }

    public void scale(float x, float y, float z) {
        stack.scale(x, y);
    }

    public void mulPose(Quaternionfc q) {
        GuiPose.mulPose(stack, q);
    }

    /** An affine matrix's part in the screen's plane (a GUI's shear). */
    public void mulPose(Matrix4fc m) {
        stack.mul(new Matrix3x2f(m.m00(), m.m01(), m.m10(), m.m11(), m.m30(), m.m31()));
    }
}
