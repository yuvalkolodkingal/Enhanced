package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.21.11 only: from 1.21.9 an entity's, a layer's and a block entity's renderer does not draw into a
 * {@link MultiBufferSource}: it submits to a {@code SubmitNodeCollector}, which draws everything later in the frame. The
 * mod's renderers were written to draw (1.21.1's and 1.21.4's way), so they draw into this: every vertex is recorded
 * here as it is given (already moved by the pose, as a {@code MultiBufferSource}'s buffer takes it), per render type and
 * in order, and {@link #flush} submits each render type's vertices once as custom geometry, replayed as they were
 * recorded. What is drawn is the same; it is only drawn later.
 */
public final class SubmitBuffers implements MultiBufferSource {
    private static final PoseStack IDENTITY = new PoseStack();
    private final SubmitNodeCollector collector;
    private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();

    public SubmitBuffers(SubmitNodeCollector collector) {
        this.collector = collector;
    }

    private int outlineColor;

    public SubmitNodeCollector collector() {
        return collector;
    }

    /** The glowing outline's colour of what is drawn (0: none), as 1.21.4's outline buffers drew it. */
    public SubmitBuffers outline(int color) {
        outlineColor = color;
        return this;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return recorders.computeIfAbsent(type, t -> new Recorder());
    }

    /**
     * Runs {@code action} with the collector things drawn into {@code buffers} are submitted to: a {@code SubmitBuffers}'
     * own, or (other buffers: the mod drawing at once, in a level stage) the game's, drawn there and then.
     */
    public static void submit(MultiBufferSource buffers, java.util.function.Consumer<SubmitNodeCollector> action) {
        if (buffers instanceof SubmitBuffers sb) {
            action.accept(sb.collector);
            return;
        }
        net.minecraft.client.renderer.GameRenderer game = net.minecraft.client.Minecraft.getInstance().gameRenderer;
        action.accept(game.getSubmitNodeStorage());
        game.getFeatureRenderDispatcher().renderAllFeatures();
    }

    /** Submits what was drawn (each render type's vertices once) and forgets it, so the buffers can be drawn into again. */
    public void flush() {
        if (recorders.isEmpty()) {
            return;
        }
        for (Map.Entry<RenderType, Recorder> e : recorders.entrySet()) {
            Recorder r = e.getValue();
            r.end();
            if (r.count == 0) {
                continue;
            }
            Recorder copy = r.copy();
            RenderType type = e.getKey();
            boolean lines = type.format().contains(VertexFormatElement.LINE_WIDTH);
            collector.submitCustomGeometry(IDENTITY, type, (pose, vc) -> copy.replay(vc, lines, 0));
            if (outlineColor != 0 && type.outline().isPresent()) {
                int outline = outlineColor;
                collector.submitCustomGeometry(IDENTITY, type.outline().get(), (pose, vc) -> copy.replay(vc, false, outline));
            }
        }
        recorders.clear();
    }

    /** The vertices drawn into one render type: x y z, then colour, uv, overlay, light, normal, line width. */
    static final class Recorder implements VertexConsumer {
        private static final int STRIDE = 12;
        private float[] f = new float[STRIDE * 64];
        private int[] i = new int[4 * 64];
        int count;
        private boolean open;
        // the vertex being built
        private float x, y, z, u, v, nx, ny, nz, width = 2.5F;
        private int color = -1, overlay, light;

        Recorder copy() {
            Recorder r = new Recorder();
            r.f = Arrays.copyOf(f, count * STRIDE);
            r.i = Arrays.copyOf(i, count * 4);
            r.count = count;
            return r;
        }

        void end() {
            if (!open) {
                return;
            }
            open = false;
            if ((count + 1) * STRIDE > f.length) {
                f = Arrays.copyOf(f, f.length * 2);
                i = Arrays.copyOf(i, i.length * 2);
            }
            int b = count * STRIDE;
            f[b] = x;
            f[b + 1] = y;
            f[b + 2] = z;
            f[b + 3] = u;
            f[b + 4] = v;
            f[b + 5] = nx;
            f[b + 6] = ny;
            f[b + 7] = nz;
            f[b + 8] = width;
            int c = count * 4;
            i[c] = color;
            i[c + 1] = overlay;
            i[c + 2] = light;
            count++;
        }

        void replay(VertexConsumer out, boolean lines, int colorOverride) {
            for (int k = 0; k < count; k++) {
                int b = k * STRIDE;
                int c = k * 4;
                out.addVertex(f[b], f[b + 1], f[b + 2]);
                out.setColor(colorOverride != 0 ? colorOverride : i[c]);
                out.setUv(f[b + 3], f[b + 4]);
                out.setOverlay(i[c + 1]);
                out.setLight(i[c + 2]);
                out.setNormal(f[b + 5], f[b + 6], f[b + 7]);
                if (lines) {
                    out.setLineWidth(f[b + 8]);
                }
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            end();
            open = true;
            this.x = x;
            this.y = y;
            this.z = z;
            color = -1;
            u = v = 0.0F;
            overlay = 0;
            light = 0xF000F0;
            nx = 0.0F;
            ny = 1.0F;
            nz = 0.0F;
            width = 2.5F;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            color = argb;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            overlay = u & 65535 | (v & 65535) << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            light = u & 65535 | (v & 65535) << 16;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.width = width;
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                              float nx, float ny, float nz) {
            addVertex(x, y, z);
            this.color = color;
            this.u = u;
            this.v = v;
            this.overlay = overlay;
            this.light = light;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }
}
