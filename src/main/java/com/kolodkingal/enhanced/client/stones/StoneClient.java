package com.kolodkingal.enhanced.client.stones;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.stones.StoneBeams;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The client's side of the stones' powers (3.6, lane L2): the beams' drawers ({@link StoneBeamFx}), the drawers of the
 * powers' {@link StoneFxPayload} kinds, the arm layer ({@link StoneArmLayer}: the Power Stone's veins, the Time
 * Stone's mandala), and the screen's edge for the holder: a red ripple when the Reality Stone turns a projectile, a
 * faint green while his Slow Time runs, a blue flash when the Space Stone sets him down in another realm. Everything
 * fades with the frame's time; nothing is drawn with the HUD hidden (F1).
 */
public final class StoneClient {
    /** When (game time) the last bubble popped at the local player, his last Rewind, his last arrival. */
    private static float bubbleAt = -1000.0F;
    private static float rewindAt = -1000.0F;
    private static float arrivedAt = -1000.0F;

    /** How long each pulse at the screen's edge lasts (ticks). */
    private static final float BUBBLE_TICKS = 12.0F;
    private static final float REWIND_TICKS = 20.0F;
    private static final float ARRIVE_TICKS = 24.0F;

    /** The setup on the mod bus: drawers, the arm layer, the screen's edge. */
    @EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(StoneClient::register);
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new StoneArmLayer(renderer));
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterGui(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Enhanced.id("stone_edges"), StoneClient::drawEdges);
        }
    }

    static void register() {
        StoneBeams.init();
        BeamRenderer.registerDrawer(StoneBeams.MIND, StoneBeamFx.MIND);
        BeamRenderer.registerDrawer(StoneBeams.POWER, StoneBeamFx.POWER);
        StoneFx.register(StoneFxPayload.BUBBLE, p -> {
            if (local(p.entityId())) {
                bubbleAt = now();
            }
        });
        StoneFx.register(StoneFxPayload.REWIND, p -> {
            if (local(p.entityId())) {
                rewindAt = now();
            }
            trail(p, StoneKind.TIME.color());
        });
        StoneFx.register(StoneFxPayload.SLOW_ON, p -> ring(p, StoneKind.TIME.color()));
        StoneFx.register(StoneFxPayload.SLOW_OFF, p -> ring(p, StoneKind.TIME.core()));
        StoneFx.register(StoneFxPayload.POWER_BLAST, p -> flash(p, StoneKind.POWER.color()));
        StoneFx.register(StoneFxPayload.REALM_DEPART, p -> flash(p, StoneKind.SPACE.color()));
        StoneFx.register(StoneFxPayload.REALM_ARRIVE, p -> {
            if (local(p.entityId())) {
                arrivedAt = now();
            }
            flash(p, StoneKind.SPACE.color());
        });
    }

    private static boolean local(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    private static float now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0.0F : mc.level.getGameTime();
    }

    /** A white flash and a burst of the stone's colour at each point. */
    private static void flash(StoneFxPayload payload, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (Vec3 p : payload.points()) {
            level.addParticle(ParticleTypes.FLASH, p.x, p.y + 0.5, p.z, 0, 0, 0);
            for (int i = 0; i < 12; i++) {
                double a = Mth.TWO_PI * i / 12;
                level.addParticle(new EnergyParticleOptions(color, 1.2F), p.x, p.y + 0.6, p.z, Math.cos(a) * 0.25,
                        0.05, Math.sin(a) * 0.25);
            }
        }
    }

    /** A ring of the stone's colour spreading on the ground round the first point (the zone opening or closing). */
    private static void ring(StoneFxPayload payload, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        Vec3 c = payload.points().get(0);
        double speed = Math.max(4, payload.arg()) / 20.0;
        for (int i = 0; i < 40; i++) {
            double a = Mth.TWO_PI * i / 40;
            level.addParticle(new EnergyParticleOptions(color, 1.0F), c.x, c.y + 0.2, c.z, Math.cos(a) * speed, 0.0,
                    Math.sin(a) * speed);
        }
    }

    /** Green afterimages along the way the Rewind took him back. */
    private static void trail(StoneFxPayload payload, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().size() < 2) {
            return;
        }
        Vec3 a = payload.points().get(0);
        Vec3 b = payload.points().get(1);
        for (int i = 0; i <= 24; i++) {
            Vec3 p = a.lerp(b, i / 24.0);
            for (int y = 0; y < 4; y++) {
                level.addParticle(new EnergyParticleOptions(color, 0.9F), p.x, p.y + 0.3 + y * 0.45, p.z, 0, 0.01, 0);
            }
        }
    }

    // ---- the screen's edge ----

    static void drawEdges(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }
        AbstractClientPlayer player = mc.player;
        float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        float bubble = 1.0F - (time - bubbleAt) / BUBBLE_TICKS;
        if (bubble > 0.0F) {
            edges(g, StoneKind.REALITY.color(), 0.35F * bubble, 0.12F + 0.04F * Mth.sin(time * 1.3F));
        }
        if (Powers.get(player).is(Live.TIME_SLOWING)) {
            edges(g, StoneKind.TIME.color(), 0.14F + 0.04F * Mth.sin(time * 0.2F), 0.1F);
        }
        float rewind = 1.0F - (time - rewindAt) / REWIND_TICKS;
        if (rewind > 0.0F) {
            edges(g, StoneKind.TIME.color(), 0.4F * rewind, 0.18F);
        }
        float arrive = 1.0F - (time - arrivedAt) / ARRIVE_TICKS;
        if (arrive > 0.0F) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), argb(0xBFE6FF, 0.6F * arrive * arrive));
            edges(g, StoneKind.SPACE.color(), 0.5F * arrive, 0.2F);
        }
    }

    /** A soft band of {@code color} at the four edges of the screen, {@code depth} of its size deep. */
    private static void edges(GuiGraphics g, int color, float alpha, float depth) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int dx = (int) (w * depth * 0.6F);
        int dy = (int) (h * depth);
        int solid = argb(color, alpha);
        int clear = argb(color, 0.0F);
        g.fillGradient(0, 0, w, dy, solid, clear);
        g.fillGradient(0, h - dy, w, h, clear, solid);
        // (the sides: thin vertical strips, each a step fainter toward the middle)
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float a = alpha * (1.0F - (float) i / steps);
            int x0 = dx * i / steps;
            int x1 = dx * (i + 1) / steps;
            g.fill(x0, 0, x1, h, argb(color, a));
            g.fill(w - x1, 0, w - x0, h, argb(color, a));
        }
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private StoneClient() {
    }
}
