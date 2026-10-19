package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Deadpool's speech bubbles (3.7): a line of his is written over his head, comic-style, black words on a white bubble
 * with a little tail, facing the camera like a name tag, in the language of the client that reads it; it pops in, stays
 * three seconds and fades. One bubble per Deadpool (a new line replaces the old). Long lines wrap. The client's {@code
 * deadpoolLines} option hides them. (The way {@code ThoughtBubbles} writes thoughts over heads.)
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class LineBubbles {
    /** How long (ticks) a bubble shows, and its pop in and fade out. */
    static final int LIFE = 60;
    static final float FADE = 6.0F;
    /** The widest a bubble's line is (pixels of the font) before it wraps. */
    static final int WRAP = 150;
    private static final int PAPER = 0xF4F2EA;
    private static final int INK = 0x1A1414;

    private record Said(Component text, long at) {
    }

    private static final Map<Integer, Said> SAID = new HashMap<>();

    /** A line of the player with that entity id, said now. */
    public static void say(int entityId, Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !EnhancedClientConfig.deadpoolLines()) {
            return;
        }
        SAID.put(entityId, new Said(text, mc.level.getGameTime()));
    }

    /** How visible a bubble is (0..1), {@code age} ticks after it was said. */
    static float alpha(float age) {
        return Math.min(Mth.clamp(age / 2.0F, 0.0F, 1.0F), Mth.clamp((LIFE - age) / FADE, 0.0F, 1.0F));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SAID.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Camera view = event.getCamera();
        Vec3 camera = view.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        Iterator<Map.Entry<Integer, Said>> it = SAID.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Said> e = it.next();
            float age = now - e.getValue().at() + pt;
            if (age > LIFE || age < -20) {
                it.remove();
                continue;
            }
            Entity entity = mc.level.getEntity(e.getKey());
            if (entity == null || entity.isInvisible() || entity == view.getEntity() && !view.isDetached()) {
                continue;
            }
            float a = alpha(age);
            if (a <= 0.02F) {
                continue;
            }
            List<FormattedCharSequence> lines = font.split(e.getValue().text(), WRAP);
            int width = 0;
            for (FormattedCharSequence line : lines) {
                width = Math.max(width, font.width(line));
            }
            float pop = age < 3.0F ? 0.8F + 0.2F * Mth.clamp(age / 3.0F, 0.0F, 1.0F) : 1.0F;
            double lift = entity.getBbHeight() + 0.55 + 0.11 * lines.size();
            Vec3 at = entity.getPosition(pt).add(0.0, lift, 0.0).subtract(camera);
            PoseStack stack = new PoseStack();
            stack.translate(at.x, at.y, at.z);
            stack.mulPose(view.rotation());
            float scale = 0.025F * pop * (float) Math.max(1.0, at.length() / 8.0);
            stack.scale(scale, -scale, scale);
            Matrix4f matrix = stack.last().pose();
            int paper = Math.round(a * 235.0F) << 24 | PAPER;
            int ink = Math.max(5, Math.round(a * 255.0F)) << 24 | INK;
            int faint = Math.max(5, Math.round(a * 90.0F)) << 24 | INK;
            int lineHeight = font.lineHeight + 1;
            float top = -lineHeight * lines.size();
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                float x = -font.width(line) / 2.0F;
                float y = top + i * lineHeight;
                // The bubble as a name tag is drawn: the white band (and faint words) seen through everything, which
                // writes no depth, then the words on it.
                font.drawInBatch(line, x, y, faint, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, paper,
                        LightTexture.FULL_BRIGHT);
                font.drawInBatch(line, x, y, ink, false, matrix, buffers, Font.DisplayMode.NORMAL, 0,
                        LightTexture.FULL_BRIGHT);
            }
            // The tail, pointing down at his head.
            FormattedCharSequence tail = Component.literal("▼").getVisualOrderText();
            font.drawInBatch(tail, -font.width(tail) / 2.0F, 0.0F, Math.round(a * 235.0F) << 24 | PAPER, false,
                    matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SAID.clear();
    }

    private LineBubbles() {
    }
}
