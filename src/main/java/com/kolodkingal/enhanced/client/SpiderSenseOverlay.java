package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * What Spider-Man's spider-sense does to his screen (DESIGN section 14): a thin white-blue frame at the edge that fades
 * over twelve ticks, three ripples spreading from the top centre (six for a strong warning), and a small chevron on the
 * edge pointing at the worst of the dangers for a second. With the Black Eyes on, the edges darken and a slow heartbeat
 * pulses (the pulse is off with reduced motion, only the darkening stays). It tells him; it never changes what happens.
 */
public class SpiderSenseOverlay implements LayeredDraw.Layer {
    /** The frame and the ripples last this many ticks. */
    private static final float FADE_TICKS = 12.0F;
    /** A ripple spreads from this radius to that (pixels), and starts this many ticks after the one before it. */
    private static final float RIPPLE_FROM = 20.0F;
    private static final float RIPPLE_TO = 80.0F;
    private static final float RIPPLE_GAP = 3.0F;
    private static final int RIPPLE_CORE = 0xDDEEFF;
    private static final int RIPPLE_RING = 0x8EC5FF;
    /** The heartbeat of the Black Eyes: 70 beats a minute, a pulse of 0.35 s. */
    private static final float BEAT_TICKS = 20.0F * 60.0F / 70.0F;
    private static final float PULSE_TICKS = 7.0F;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.SPIDER_MAN) {
            return;
        }
        float time = mc.level.getGameTime() + deltaTracker.getGameTimeDeltaPartialTick(false);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (SpiderSenseClient.overload()) {
            vignette(graphics, w, h, time);
        }
        if (!data.toggle(Toggle.SPIDER_SENSE)) {
            return;
        }
        float age = SpiderSenseClient.age(time);
        boolean strong = SpiderSenseClient.strong();
        if (age >= 0.0F && age < FADE_TICKS) {
            frame(graphics, w, h, 1.0F - age / FADE_TICKS, strong);
            ripples(graphics, w, age, strong);
        }
        Vec3 target = SpiderSenseClient.strongest();
        if (target != null && age >= 0.0F && age < SpiderSenseClient.ARROW_TICKS) {
            chevron(graphics, mc, w, h, target, 1.0F - age / SpiderSenseClient.ARROW_TICKS, strong);
        }
    }

    /** A thin white-blue frame at the edge of the screen, softest towards the middle. */
    private static void frame(GuiGraphics graphics, int w, int h, float fade, boolean strong) {
        int depth = strong ? 12 : 9;
        edges(graphics, w, h, depth, depth, RIPPLE_CORE, (strong ? 0.65F : 0.45F) * fade);
    }

    /**
     * A soft shade along the four edges of the screen, {@code alpha} at the edge and none {@code top} pixels in from
     * the top and bottom and {@code side} pixels in from the sides (the sides are drawn as strips: a gradient across is
     * not something the game's drawing does).
     */
    private static void edges(GuiGraphics graphics, int w, int h, int top, int side, int rgb, float alpha) {
        int edge = argb(rgb, alpha);
        int none = argb(rgb, 0.0F);
        graphics.fillGradient(0, 0, w, top, edge, none);
        graphics.fillGradient(0, h - top, w, h, none, edge);
        int strips = Math.min(side, 24);
        for (int i = 0; i < strips; i++) {
            float k = 1.0F - (float) i / strips;
            int from = Math.round((float) side * i / strips);
            int to = Math.round((float) side * (i + 1) / strips);
            int color = argb(rgb, alpha * k * k);
            graphics.fill(from, top, to, h - top, color);
            graphics.fill(w - to, top, w - from, h - top, color);
        }
    }

    /** Three ripples (six if strong) spreading from the top centre, a few ticks apart, fading as they grow. */
    private static void ripples(GuiGraphics graphics, int w, float age, boolean strong) {
        int rings = strong ? 6 : 3;
        for (int i = 0; i < rings; i++) {
            float start = (strong ? i / 2 : i) * RIPPLE_GAP;
            float t = (age - start) / FADE_TICKS;
            if (t < 0.0F || t >= 1.0F) {
                continue;
            }
            float radius = Mth.lerp(t, RIPPLE_FROM, RIPPLE_TO) + (strong && i % 2 == 1 ? 7.0F : 0.0F);
            float alpha = 0.7F * (1.0F - t) * (strong ? 1.0F : 0.85F);
            arc(graphics, w / 2, 0, radius, argb(i % 2 == 0 ? RIPPLE_CORE : RIPPLE_RING, alpha));
        }
    }

    /** A downward arc of dots on a circle round a centre at the top of the screen. */
    private static void arc(GuiGraphics graphics, int cx, int cy, float radius, int color) {
        int dots = Math.max(12, (int) (radius * 1.3F));
        for (int i = 0; i <= dots; i++) {
            float angle = Mth.lerp((float) i / dots, 0.18F * Mth.PI, 0.82F * Mth.PI);
            int x = cx + Math.round(Mth.cos(angle) * radius * 1.6F);
            int y = cy + Math.round(Mth.sin(angle) * radius);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    /** A small chevron on a ring round the middle of the screen, pointing out to the danger. */
    private static void chevron(GuiGraphics graphics, Minecraft mc, int w, int h, Vec3 target, float fade, boolean strong) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 away = target.subtract(camera.getPosition());
        // The bearing against the camera's own: 0 straight ahead, positive to the right.
        float bearing = (float) (Mth.wrapDegrees(Math.atan2(-away.x, away.z) * Mth.RAD_TO_DEG - camera.getYRot()));
        float angle = -bearing * Mth.DEG_TO_RAD;
        float radius = Math.min(w, h) * 0.5F - 26.0F;
        float x = w * 0.5F + Mth.sin(angle) * radius * 1.4F;
        float y = h * 0.5F - Mth.cos(angle) * radius;
        x = Mth.clamp(x, 14.0F, w - 14.0F);
        y = Mth.clamp(y, 14.0F, h - 14.0F);
        int color = argb(strong ? 0xFFFFFF : RIPPLE_CORE, 0.9F * fade);
        int shade = argb(RIPPLE_RING, 0.7F * fade);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotation(angle));
        for (int row = 0; row < 6; row++) {
            graphics.fill(-row - 1, row, -row + 1, row + 2, shade);
            graphics.fill(row - 1, row, row + 1, row + 2, shade);
            graphics.fill(-row, row, -row + 1, row + 1, color);
            graphics.fill(row, row, row + 1, row + 1, color);
        }
        graphics.pose().popPose();
    }

    /**
     * The Black Eyes: the edges of the screen darken, and throb with a slow heartbeat (the throb is off with reduced
     * motion).
     */
    private static void vignette(GuiGraphics graphics, int w, int h, float time) {
        float alpha = 0.25F;
        if (!EnhancedClientConfig.reducedMotion()) {
            float beat = time % BEAT_TICKS;
            float pulse = Math.max(0.0F, 1.0F - beat / PULSE_TICKS);
            float second = Math.max(0.0F, 1.0F - Math.abs(beat - 3.2F) / 2.5F) * 0.6F;
            alpha += 0.12F * Math.max(pulse * pulse, second * second);
        }
        edges(graphics, w, h, h / 4, w / 6, 0x0A0305, alpha);
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }
}
