package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.Arrays;

/**
 * 1.21.11 only: the mod's own GUI shapes (rings, slanted lines, gradients) were drawn into the GUI's buffer with the GUI
 * pose's matrix (1.21.1). From 1.21.6 the GUI is a list of elements drawn at the frame's end; this is one element: what
 * is drawn into it (in the GUI's coordinates, {@link #IDENTITY} as the matrix) is drawn with the GUI's pose as it was
 * when the element was made, flat coloured.
 */
public final class GuiQuads implements VertexConsumer, GuiElementRenderState {
    /** The matrix the mod's code draws with: the pose is the element's. */
    public static final Matrix4f IDENTITY = new Matrix4f();
    private final Matrix3x2fc pose;
    @Nullable
    private final ScreenRectangle scissor;
    private float[] xy = new float[64];
    private int[] colors = new int[32];
    private int count;
    private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

    private GuiQuads(GuiGraphics g) {
        this.pose = new Matrix3x2f(g.pose());
        this.scissor = g.peekScissorStack();
    }

    /** A new element of {@code g}'s, already submitted: draw into it at once. */
    public static GuiQuads of(GuiGraphics g) {
        GuiQuads quads = new GuiQuads(g);
        g.submitGuiElementRenderState(quads);
        return quads;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (count * 2 + 2 > xy.length) {
            xy = Arrays.copyOf(xy, xy.length * 2);
            colors = Arrays.copyOf(colors, colors.length * 2);
        }
        xy[count * 2] = x;
        xy[count * 2 + 1] = y;
        colors[count] = -1;
        count++;
        Vector2f p = pose.transformPosition(x, y, new Vector2f());
        minX = Math.min(minX, p.x);
        minY = Math.min(minY, p.y);
        maxX = Math.max(maxX, p.x);
        maxY = Math.max(maxY, p.y);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return setColor((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
    }

    @Override
    public VertexConsumer setColor(int argb) {
        if (count > 0) {
            colors[count - 1] = argb;
        }
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return this;
    }

    @Override
    public void buildVertices(VertexConsumer out) {
        for (int i = 0; i < count; i++) {
            out.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colors[i]);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    @Nullable
    public ScreenRectangle scissorArea() {
        return scissor;
    }

    @Override
    @Nullable
    public ScreenRectangle bounds() {
        if (count == 0) {
            return null;
        }
        ScreenRectangle area = new ScreenRectangle((int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX - Math.floor(minX)) + 1, (int) Math.ceil(maxY - Math.floor(minY)) + 1);
        return scissor != null ? scissor.intersection(area) : area;
    }
}
