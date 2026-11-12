package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * 26.3 only: 1.21.1's {@code Font.drawInBatch} into buffers. 26.3's text in the level is submitted
 * ({@code submitText}); it is submitted to the collector of the {@link SubmitBuffers} it is drawn into.
 */
public final class TextDraw {
    private TextDraw() {
    }

    public static void drawInBatch(Font font, String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                                   MultiBufferSource buffers, Font.DisplayMode mode, int background, int light) {
        drawInBatch(font, Component.literal(text).getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, background, light);
    }

    public static void drawInBatch(Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                                   MultiBufferSource buffers, Font.DisplayMode mode, int background, int light) {
        drawInBatch(font, text.getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, background, light);
    }

    public static void drawInBatch(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow,
                                   Matrix4f matrix, MultiBufferSource buffers, Font.DisplayMode mode, int background, int light) {
        PoseStack pose = new PoseStack();
        pose.last().pose().set(matrix);
        SubmitBuffers.submit(buffers, c -> c.submitText(pose, x, y, text, shadow, mode, light, color, background, 0));
    }
}
