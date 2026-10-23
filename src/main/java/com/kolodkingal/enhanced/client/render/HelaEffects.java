package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.asgard.AsgardFx;
import com.kolodkingal.enhanced.hela.HelaFight;
import com.kolodkingal.enhanced.hela.HelaFx;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.FMLClientSetupEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * What the clients draw of Hela's moments (3.5): the drawer of her lane's kinds of {@link AsgardFxPayload} (400-499, see
 * {@link HelaFx}), the lasting pictures on the floor (the burning circle of a Blade Rain, the ring of a Blade Storm sweeping
 * out, the green-gold threads of her channel, the smoke pouring from the seal), the bursts of smoke and light, and her
 * own boss bar ({@link #drawBar}: black with a green line, the phase's name; Fenris's bar under it in grey-red). A payload
 * whose doer is a player plays the hero's gesture for it ({@link HelaActing#onFx}).
 */
@Subscriber(side = Side.CLIENT)
public final class HelaEffects {
    /** A picture that lasts: what kind, where, from when to when, and its number. */
    private record Lasting(int kind, int entityId, List<Vec3> points, long start, long end, int arg) {
    }

    private static final List<Lasting> LASTING = new ArrayList<>();
    private static final int RING_SEGMENTS = 64;

    /** Registers the drawer of each of her kinds, once, when the client sets up. */
    @Subscriber(side = Side.CLIENT)
    public static final class Setup {
        @Subscribe
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(HelaEffects::register);
        }

        private Setup() {
        }
    }

    static void register() {
        for (int kind = HelaFx.VOLLEY; kind <= HelaFx.RAISE; kind++) {
            AsgardFx.register(kind, HelaEffects::receive);
        }
        for (String key : List.of("boss.enhanced.hela", "boss.enhanced.hela.phase2", "boss.enhanced.hela.phase3")) {
            BossBarOverride.register(key, (g, bar, x, y) -> drawBar(g, bar, x, y, false));
        }
        BossBarOverride.register("boss.enhanced.hela.fenris", (g, bar, x, y) -> drawBar(g, bar, x, y, true));
    }

    /** One of her moments arrives. */
    public static void receive(AsgardFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || p.points().isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Vec3 at = p.points().get(0);
        RandomSource random = level.random;
        switch (p.kind()) {
            case HelaFx.VOLLEY, HelaFx.THROW -> {
                for (Vec3 point : p.points()) {
                    burst(level, point, 4, 0.12, HelaRules.PALE, 0.5F);
                }
            }
            case HelaFx.WALL -> {
                for (Vec3 point : p.points()) {
                    level.addParticle(ParticleTypes.SMOKE, point.x, point.y + 0.05, point.z, 0.0, 0.02, 0.0);
                }
            }
            case HelaFx.SHADOW_STEP, HelaFx.DASH -> {
                for (Vec3 point : p.points()) {
                    smoke(level, point.add(0.0, 1.0, 0.0), 24, 0.45);
                    burst(level, point.add(0.0, 1.0, 0.0), 10, 0.4, HelaRules.GREEN, 0.7F);
                }
                if (p.kind() == HelaFx.DASH && p.points().size() > 1) {
                    Vec3 to = p.points().get(1);
                    for (int i = 0; i <= 16; i++) {
                        Vec3 q = at.lerp(to, i / 16.0).add(0.0, 1.0, 0.0);
                        level.addParticle(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 0.0, 0.01, 0.0);
                    }
                }
            }
            case HelaFx.RAIN -> LASTING.add(new Lasting(p.kind(), p.entityId(), p.points(), now, now + 24, p.arg()));
            case HelaFx.STORM -> LASTING.add(new Lasting(p.kind(), p.entityId(), p.points(), now, now + 44, p.arg()));
            case HelaFx.CHANNEL -> {
                LASTING.removeIf(l -> l.kind() == HelaFx.CHANNEL && l.entityId() == p.entityId());
                if (p.arg() > 0) {
                    LASTING.add(new Lasting(p.kind(), p.entityId(), p.points(), now, now + p.arg(), p.arg()));
                }
            }
            case HelaFx.CHANNEL_BREAK -> {
                burst(level, at, 40, 0.6, 0xE8C060, 1.0F);
                burst(level, at, 30, 0.5, HelaRules.PALE, 0.9F);
            }
            case HelaFx.ARRIVE -> LASTING.add(new Lasting(p.kind(), p.entityId(), p.points(), now, now + p.arg(), p.arg()));
            case HelaFx.DISSOLVE -> LASTING.add(new Lasting(p.kind(), p.entityId(), p.points(), now, now + p.arg(), p.arg()));
            case HelaFx.CRUSH -> {
                for (int i = 0; i < 30; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, random.nextGaussian() * 0.4, random.nextGaussian() * 0.4,
                            random.nextGaussian() * 0.4);
                }
                burst(level, at, 16, 0.3, HelaRules.GREEN, 1.0F);
            }
            case HelaFx.RISE, HelaFx.FENRIS -> {
                int n = p.kind() == HelaFx.FENRIS ? 40 : 16;
                smoke(level, at.add(0.0, 0.3, 0.0), n, p.kind() == HelaFx.FENRIS ? 1.0 : 0.5);
                burst(level, at.add(0.0, 0.2, 0.0), n / 2, 0.6, HelaRules.GREEN, 0.7F);
            }
            case HelaFx.BURST -> {
                float r = p.arg() / 10.0F;
                for (int i = 0; i < 24; i++) {
                    double a = i * Mth.TWO_PI / 24;
                    level.addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.8F), at.x + Math.cos(a) * r, at.y + 0.1, at.z + Math.sin(a) * r,
                            0.0, 0.05, 0.0);
                }
            }
            case HelaFx.CRUMBLE -> {
                smoke(level, at, 6, 0.15);
                burst(level, at, 5, 0.15, HelaRules.GREEN, 0.6F);
            }
            case HelaFx.FORM -> {
                burst(level, at, p.arg() > 0 ? 30 : 12, 0.5, p.arg() > 0 ? HelaRules.PALE : HelaRules.GREEN, 0.9F);
                smoke(level, at, p.arg() > 0 ? 16 : 8, 0.4);
            }
            case HelaFx.RAISE -> smoke(level, at.add(0.0, 0.2, 0.0), 12, 1.2);
            default -> {
            }
        }
        HelaActing.onFx(p);
    }

    private static void burst(ClientLevel level, Vec3 at, int n, double spread, int colour, float size) {
        RandomSource r = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(new EnergyParticleOptions(colour, size), at.x + r.nextGaussian() * spread * 0.4, at.y + r.nextGaussian() * spread * 0.4,
                    at.z + r.nextGaussian() * spread * 0.4, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05);
        }
    }

    private static void smoke(ClientLevel level, Vec3 at, int n, double spread) {
        RandomSource r = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, at.x + r.nextGaussian() * spread * 0.5, at.y + r.nextGaussian() * spread * 0.6,
                    at.z + r.nextGaussian() * spread * 0.5, r.nextGaussian() * 0.02, 0.02 + r.nextDouble() * 0.03, r.nextGaussian() * 0.02);
        }
    }

    /** Once per client tick: the smoke of the pictures that pour (the arrival, a dissolving), and the end of the old ones. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            LASTING.clear();
            return;
        }
        long now = level.getGameTime();
        LASTING.removeIf(l -> now > l.end() + 20);
        for (Lasting l : LASTING) {
            if (now > l.end()) {
                continue;
            }
            float t = now - l.start();
            Vec3 at = l.points().get(0);
            switch (l.kind()) {
                case HelaFx.ARRIVE -> {
                    // Smoke pours from the seal down to the dais, and gathers there into her shape; the boom of the sound at
                    // 5.2 s is a flash of green light.
                    Vec3 to = l.points().size() > 1 ? l.points().get(1) : at;
                    for (int i = 0; i < 3; i++) {
                        double k = level.random.nextDouble();
                        Vec3 q = at.lerp(to.add(0.0, 1.0, 0.0), k);
                        level.addParticle(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 0.0, -0.01, 0.0);
                    }
                    if (t > 40) {
                        smoke(level, to.add(0.0, 1.0, 0.0), 2, 0.8);
                    }
                    if ((int) t == HelaFight.BOOM_AT) {
                        burst(level, to.add(0.0, 1.2, 0.0), 60, 1.6, HelaRules.PALE, 1.2F);
                        smoke(level, to.add(0.0, 0.5, 0.0), 40, 2.4);
                    }
                }
                case HelaFx.DISSOLVE -> {
                    Entity e = level.getEntity(l.entityId());
                    Vec3 q = e != null ? e.position() : at;
                    smoke(level, q.add(0.0, 1.0, 0.0), 3, 0.7);
                    if (level.random.nextInt(2) == 0) {
                        burst(level, q.add(0.0, 1.0, 0.0), 1, 0.7, HelaRules.GREEN, 0.6F);
                    }
                }
                case HelaFx.CHANNEL -> {
                    Entity e = level.getEntity(l.entityId());
                    if (e != null && level.random.nextInt(2) == 0) {
                        double a = level.random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(new EnergyParticleOptions(0xE8C060, 0.6F), e.getX() + Math.cos(a) * 4.0, e.getY() + 0.1,
                                e.getZ() + Math.sin(a) * 4.0, -Math.cos(a) * 0.12, 0.08, -Math.sin(a) * 0.12);
                    }
                }
                default -> {
                }
            }
        }
    }

    public static void clear() {
        LASTING.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The pictures on the floor

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LASTING.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Lasting l : LASTING) {
            float t = time - l.start();
            if (time > l.end() + 6) {
                continue;
            }
            Vec3 o = l.points().get(0).subtract(camera).add(0.0, 0.04, 0.0);
            switch (l.kind()) {
                case HelaFx.RAIN -> rainCircle(pose, vc, o, l.arg() / 10.0F, t, l.end() - l.start());
                case HelaFx.STORM -> stormRing(pose, vc, o, l.arg() / 10.0F, t);
                case HelaFx.CHANNEL -> {
                    Entity e = level.getEntity(l.entityId());
                    if (e != null) {
                        threads(pose, vc, e.getPosition(partial).subtract(camera), t, l.arg());
                    }
                }
                default -> {
                }
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** How much of a lasting picture shows: in over 4 ticks, out over 6 after its end. */
    static float fade(float t, float length) {
        return Mth.clamp(t / 4.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((t - length) / 6.0F, 0.0F, 1.0F));
    }

    /** The Blade Rain's mark: a burning ring with an inner one, pulsing faster as the blades are about to fall. */
    private static void rainCircle(Matrix4f pose, VertexConsumer vc, Vec3 o, float radius, float t, float length) {
        float a = fade(t, length);
        float pulse = 0.6F + 0.4F * Mth.sin(t * (0.4F + t * 0.03F));
        flatRing(pose, vc, o, radius, 0.22F, EnergyGeometry.argb(HelaRules.GREEN, a * pulse));
        flatRing(pose, vc, o, radius * 0.55F, 0.12F, EnergyGeometry.argb(HelaRules.PALE, a * 0.7F * pulse));
        for (int i = 0; i < 6; i++) {
            double ang = i * Mth.TWO_PI / 6 + t * 0.02;
            Vec3 in = o.add(Math.cos(ang) * radius * 0.55, 0, Math.sin(ang) * radius * 0.55);
            Vec3 out = o.add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
            flatLine(pose, vc, in, out, 0.08F, EnergyGeometry.argb(HelaRules.GREEN, a * 0.6F));
        }
    }

    /** The Blade Storm: a ring sweeping out from her, a crown of blade glints standing on it. */
    private static void stormRing(Matrix4f pose, VertexConsumer vc, Vec3 o, float radius, float t) {
        float k = Mth.clamp(t / 40.0F, 0.0F, 1.0F);
        float r = radius * k;
        float a = (1.0F - Mth.clamp((t - 40.0F) / 6.0F, 0.0F, 1.0F)) * 0.85F;
        flatRing(pose, vc, o, r, 0.5F, EnergyGeometry.argb(HelaRules.GREEN, a));
        int blades = 28;
        for (int i = 0; i < blades; i++) {
            double ang = i * Mth.TWO_PI / blades;
            Vec3 foot = o.add(Math.cos(ang) * r, 0.0, Math.sin(ang) * r);
            Vec3 side = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang)).scale(0.08);
            int c = EnergyGeometry.argb(HelaRules.PALE, a);
            int top = EnergyGeometry.argb(HelaRules.PALE, 0.0F);
            vertex(pose, vc, foot.add(side), c);
            vertex(pose, vc, foot.subtract(side), c);
            vertex(pose, vc, foot.add(0.0, 1.1, 0.0), top);
            vertex(pose, vc, foot.add(0.0, 1.1, 0.0), top);
        }
    }

    /** The channel: green-gold threads rising from a circle on the floor to her chest, waving. */
    private static void threads(Matrix4f pose, VertexConsumer vc, Vec3 feet, float t, int length) {
        float a = fade(t, length);
        Vec3 chest = feet.add(0.0, 1.4, 0.0);
        int n = 10;
        for (int i = 0; i < n; i++) {
            double ang = i * Mth.TWO_PI / n + t * 0.03;
            Vec3 foot = feet.add(Math.cos(ang) * 4.5, 0.05, Math.sin(ang) * 4.5);
            Vec3 prev = foot;
            for (int s = 1; s <= 8; s++) {
                double k = s / 8.0;
                Vec3 q = foot.lerp(chest, k).add(0.0, Math.sin(k * Math.PI) * 0.8, 0.0)
                        .add(Math.sin(t * 0.3 + i + s) * 0.08, 0.0, Math.cos(t * 0.27 + i * 2 + s) * 0.08);
                int c = EnergyGeometry.argb(i % 2 == 0 ? 0xE8C060 : HelaRules.GREEN, a * (0.4F + 0.5F * (float) k));
                EnergyGeometry.ribbon(pose, vc, prev, q, 0.05F, c, c);
                prev = q;
            }
        }
        flatRing(pose, vc, feet.add(0.0, 0.05, 0.0), 4.5F, 0.15F, EnergyGeometry.argb(0xE8C060, a * 0.6F));
    }

    private static void flatRing(Matrix4f pose, VertexConsumer vc, Vec3 o, float radius, float width, int colour) {
        float in = Math.max(0.0F, radius - width * 0.5F);
        float out = radius + width * 0.5F;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double t0 = i * Mth.TWO_PI / RING_SEGMENTS;
            double t1 = (i + 1) * Mth.TWO_PI / RING_SEGMENTS;
            vertex(pose, vc, o.add(Math.cos(t0) * in, 0, Math.sin(t0) * in), colour);
            vertex(pose, vc, o.add(Math.cos(t0) * out, 0, Math.sin(t0) * out), colour);
            vertex(pose, vc, o.add(Math.cos(t1) * out, 0, Math.sin(t1) * out), colour);
            vertex(pose, vc, o.add(Math.cos(t1) * in, 0, Math.sin(t1) * in), colour);
        }
    }

    private static void flatLine(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, int colour) {
        Vec3 dir = b.subtract(a);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width * 0.5);
        vertex(pose, vc, a.add(side), colour);
        vertex(pose, vc, a.subtract(side), colour);
        vertex(pose, vc, b.subtract(side), colour);
        vertex(pose, vc, b.add(side), colour);
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Her bar

    /**
     * Her bar: a black bar with a thin bright green line along its top, the health in deep green with a lighter edge (gold
     * while the bar flashes in her channel: the server turns it yellow), ten notches; Fenris's under it, red-grey. Her name
     * over it, as the vanilla bar has.
     */
    static int drawBar(GuiGraphics g, LerpingBossEvent bar, int x, int y, boolean fenris) {
        Minecraft mc = Minecraft.getInstance();
        int w = 182;
        int h = fenris ? 4 : 6;
        float p = Mth.clamp(bar.getProgress(), 0.0F, 1.0F);
        boolean flash = bar.getColor() == net.minecraft.world.BossEvent.BossBarColor.YELLOW;
        int frame = 0xFF0A0F0C;
        int empty = 0xFF1B231E;
        int fill = fenris ? 0xFF7A2A2A : flash ? 0xFFD9B347 : 0xFF2E7D4F;
        int edge = fenris ? 0xFFC05050 : flash ? 0xFFFFE7A3 : 0xFF9CF2B5;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, frame);
        g.fill(x, y, x + w, y + h, empty);
        int filled = Math.round(w * p);
        g.fill(x, y, x + filled, y + h, fill);
        g.fill(x, y, x + filled, y + 1, edge);
        if (filled > 0) {
            g.fill(x + filled - 1, y, x + filled, y + h, edge);
        }
        if (!fenris) {
            for (int i = 1; i < 10; i++) {
                int nx = x + w * i / 10;
                g.fill(nx, y + h - 2, nx + 1, y + h, frame);
            }
        }
        var name = bar.getName();
        int tw = mc.font.width(name);
        g.drawString(mc.font, name, x + w / 2 - tw / 2, y - 9, fenris ? 0xE0B0B0 : 0x9CF2B5);
        return h + 12;
    }

    private HelaEffects() {
    }
}
