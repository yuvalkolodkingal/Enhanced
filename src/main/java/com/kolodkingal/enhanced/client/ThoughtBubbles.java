package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.ThoughtsPayload;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
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
 * The words of the thoughts Billy and Professor X hear (3.3, {@link ThoughtsPayload}): each one is written over the head
 * it comes from, in italics, as a name tag is (facing the camera, faint through walls, bright where it is seen), in the
 * colour of its mood: the listener's own colour for a calm mind, red for one that means harm, amber for one that is
 * afraid, grey for a mind closed to him. A thought fades in when it is heard, and out if it is not heard again (the
 * server says them every two seconds), and they are only drawn while the listener still hears ({@link TheNoise#active}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ThoughtBubbles {
    /** A thought is drawn this long after it was last heard (ticks); the server repeats them every 40. */
    static final int LIFE = 60;
    /** Fading in and out (ticks). */
    static final float FADE = 8.0F;

    private static final int HOSTILE = 0xFF7A6E;
    private static final int AFRAID = 0xFFD27A;
    private static final int CLOSED = 0xA4AAB4;

    private record Heard(Component text, int mood, long since, long at) {
    }

    private static final Map<Integer, Heard> HEARD = new HashMap<>();

    /** The thoughts of the last message: a new thought replaces the old one of the same creature, a repeat keeps its age. */
    public static void accept(List<ThoughtsPayload.Thought> thoughts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        for (ThoughtsPayload.Thought t : thoughts) {
            Heard old = HEARD.get(t.entityId());
            boolean same = old != null && old.text().equals(t.text());
            HEARD.put(t.entityId(), new Heard(t.text(), t.mood(), same ? old.since() : now, now));
        }
    }

    /** The colour a thought of this mood is written in, for a listener of this colour. */
    static int colour(int mood, int listener) {
        return switch (mood) {
            case ThoughtsPayload.HOSTILE -> HOSTILE;
            case ThoughtsPayload.AFRAID -> AFRAID;
            case ThoughtsPayload.CLOSED -> CLOSED;
            default -> listener;
        };
    }

    /** How visible a thought is (0..1), {@code age} ticks after it began and {@code idle} ticks after it was last heard. */
    static float alpha(float age, float idle) {
        float in = Mth.clamp(age / FADE, 0.0F, 1.0F);
        float out = Mth.clamp((LIFE - idle) / FADE, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || HEARD.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !TheNoise.active() || mc.options.hideGui) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Camera view = event.getCamera();
        Vec3 camera = view.getPosition();
        int listener = Powers.glow(mc.player);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        int background = (int) (mc.options.getBackgroundOpacity(0.35F) * 255.0F) << 24;
        Iterator<Map.Entry<Integer, Heard>> it = HEARD.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Heard> e = it.next();
            Heard heard = e.getValue();
            float idle = now - heard.at() + pt;
            if (idle > LIFE) {
                it.remove();
                continue;
            }
            Entity entity = mc.level.getEntity(e.getKey());
            if (entity == null || entity == view.getEntity() || entity.isInvisible()) {
                continue;
            }
            float a = alpha(now - heard.since() + pt, idle);
            if (a <= 0.02F) {
                continue;
            }
            double lift = entity.getBbHeight() + (entity.shouldShowName() ? 0.75 : 0.5);
            Vec3 at = entity.getPosition(pt).add(0.0, lift, 0.0).subtract(camera);
            PoseStack stack = new PoseStack();
            stack.translate(at.x, at.y, at.z);
            stack.mulPose(view.rotation());
            // As a name tag, but it grows past 6 blocks, so a thought far off is still read.
            float scale = 0.025F * (float) Math.max(1.0, at.length() / 6.0);
            stack.scale(scale, -scale, scale);
            Matrix4f matrix = stack.last().pose();
            Component text = heard.text().copy().withStyle(s -> s.withItalic(true));
            float x = -font.width(text) / 2.0F;
            int rgb = colour(heard.mood(), listener) & 0xFFFFFF;
            int alpha = Math.max(5, Math.round(a * 255.0F));
            int faint = Math.max(5, Math.round(a * 96.0F));
            int back = (int) (Math.max(0x50, (background >>> 24) & 0xFF) * a) << 24;
            font.drawInBatch(text, x, 0.0F, faint << 24 | rgb, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, back,
                    LightTexture.FULL_BRIGHT);
            font.drawInBatch(text, x, 0.0F, alpha << 24 | rgb, false, matrix, buffers, Font.DisplayMode.NORMAL, 0,
                    LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        HEARD.clear();
    }

    private ThoughtBubbles() {
    }
}
