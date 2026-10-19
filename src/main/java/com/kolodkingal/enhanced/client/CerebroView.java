package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.power.Power;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * What Cerebro shows Professor X (3.3, {@link CerebroPayload}): every mind it found is a marker in the world, seen through
 * everything (a diamond in the hero's colour, the name and how far), set at most {@link #MARKER_DISTANCE} blocks away
 * in its direction so a mind a thousand blocks off still has its marker on screen; {@link XavierOverlay} draws the blue
 * of Cerebro over the screen and the list. A marker moves smoothly from one report to the next (once a second).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class CerebroView {
    /** The furthest a marker is drawn (blocks); a mind further off is drawn this far away in its direction. */
    static final double MARKER_DISTANCE = 40.0;

    private static List<CerebroPayload.Mind> minds = List.of();
    private static List<CerebroPayload.Mind> before = List.of();
    private static long receivedAt;
    private static long until;

    /** 3.7: whether the last report was the lab's amplified Cerebro (lane L5 draws the sphere of stars from it). */
    private static boolean amplified;
    /** 3.7: the stone sites of the last report (lane L5's diamonds on the sphere and in the list). */
    private static List<CerebroPayload.Site> sites = List.of();

    /** A report of Cerebro (3.7: with the amplified flag and the stone sites, which only lane L5 shows). */
    public static void accept(CerebroPayload payload) {
        amplified = payload.amplified();
        sites = List.copyOf(payload.sites());
        accept(payload.minds(), payload.ticksLeft());
    }

    /** Whether the search is the lab's amplified Cerebro (3.7), as his client last heard. */
    public static boolean amplified() {
        return active() && amplified;
    }

    /** The stone sites the amplified Cerebro marks (3.7), or none. */
    public static List<CerebroPayload.Site> sites() {
        return active() ? sites : List.of();
    }

    public static void accept(List<CerebroPayload.Mind> found, int ticksLeft) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        before = minds;
        minds = List.copyOf(found);
        receivedAt = now;
        until = now + ticksLeft;
    }

    /** Whether Cerebro is searching (as his client last heard). */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getGameTime() < until;
    }

    /** The minds of the last report, the nearest first. */
    public static List<CerebroPayload.Mind> minds() {
        return active() ? minds : List.of();
    }

    /** The ticks Cerebro has left. */
    public static long ticksLeft() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : Math.max(0, until - mc.level.getGameTime());
    }

    /** Where a mind is now, between the last two reports (the same name in both glides; a new one stands). */
    static Vec3 position(CerebroPayload.Mind mind, float sinceReport) {
        float f = Mth.clamp(sinceReport / 20.0F, 0.0F, 1.0F);
        for (CerebroPayload.Mind old : before) {
            if (old.name().equals(mind.name())) {
                return old.pos().lerp(mind.pos(), f);
            }
        }
        return mind.pos();
    }

    /** Where the marker of a mind at {@code at} is drawn, seen from {@code eye}: never further than {@link #MARKER_DISTANCE}. */
    static Vec3 marker(Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        double len = d.length();
        return len <= MARKER_DISTANCE ? at : eye.add(d.scale(MARKER_DISTANCE / len));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float since = mc.level.getGameTime() - receivedAt + pt;
        float fade = Mth.clamp((until - mc.level.getGameTime() - pt) / 10.0F, 0.0F, 1.0F);
        Camera view = event.getCamera();
        Vec3 camera = view.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        Power[] powers = Power.values();
        for (CerebroPayload.Mind mind : minds) {
            Vec3 where = position(mind, since).add(0.0, 1.0, 0.0);
            Vec3 at = marker(camera, where).subtract(camera);
            double distance = where.distanceTo(camera);
            Power power = mind.power() >= 0 && mind.power() < powers.length ? powers[mind.power()] : Power.NONE;
            int rgb = power.hasPowers() ? power.titleColor() : 0xE8EEFF;
            float scale = 0.025F * (float) Math.max(1.0, at.length() / 8.0);
            PoseStack stack = new PoseStack();
            stack.translate(at.x, at.y, at.z);
            stack.mulPose(view.rotation());
            stack.scale(scale, -scale, scale);
            Matrix4f matrix = stack.last().pose();
            Component name = power.hasPowers()
                    ? Component.translatable("hud.enhanced.xavier.cerebro.mind_hero", mind.name(), power.displayName(), Math.round(distance))
                    : Component.translatable("hud.enhanced.xavier.cerebro.mind", mind.name(), Math.round(distance));
            Component diamond = Component.literal("◆");
            int a = Math.max(5, Math.round(230 * fade)) << 24;
            font.drawInBatch(diamond, -font.width(diamond) / 2.0F, -10.0F, a | rgb, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH,
                    0, LightTexture.FULL_BRIGHT);
            font.drawInBatch(name, -font.width(name) / 2.0F, 0.0F, a | rgb, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH,
                    Math.round(0x50 * fade) << 24, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        minds = List.of();
        before = List.of();
        until = 0;
    }

    private CerebroView() {
    }
}
