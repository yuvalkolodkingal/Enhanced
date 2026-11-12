package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 26.3 only: 26.3 has no {@code MultiBufferSource}: everything is submitted to a {@code SubmitNodeCollector}. The mod's
 * renderers were written to draw into buffers per render type, so they draw into this, and {@link SubmitBuffers}
 * submits what they drew. {@link #immediate()} stands for 1.21.1's {@code renderBuffers().bufferSource()}: in a level
 * stage it is the buffers of that stage (compat.SubmitBuffers#stage).
 */
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);

    /** The buffers the mod draws into in a level stage (1.21.1's {@code renderBuffers().bufferSource()}). */
    static BufferSource immediate() {
        return SubmitBuffers.stage();
    }

    /** Buffers that are drawn when {@link #endBatch} is called (here: submitted). */
    interface BufferSource extends MultiBufferSource {
        void endBatch();

        void endBatch(RenderType type);

        default void endLastBatch() {
        }
    }
}
