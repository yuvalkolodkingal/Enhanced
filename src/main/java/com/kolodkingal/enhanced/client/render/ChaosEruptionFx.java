package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (3.19.2) The eruption of chaos energy when the Scarlet Witch becomes the Sorceress Supreme (Ultimate Wanda): for four
 * seconds, all round her out to thirty blocks, great columns of glowing red energy burst up out of the ground and shoot
 * into the high sky, bolts of it fall from far above, streaks of it fly in at her from every side, and a column of light
 * stands on her while rings of it run out over the ground. Drawn as light (camera-facing ribbons, additive, so where
 * they cross they burn brighter), not as particles, so it is huge and seen from far away; sparks fly along with it.
 * {@link SorcererFx} ticks and draws it; it starts by itself on every client that sees her change.
 */
public final class ChaosEruptionFx {
    /** How long it lasts (ticks), and how far round her it reaches (blocks). */
    public static final int DURATION = 84;
    public static final double REACH = 30.0;
    static final int RED = 0xFF1E3C;
    static final int DEEP = 0xC00820;
    static final int LIGHT = 0xFFD8DE;
    private static final int PILLARS = 40;
    private static final int BOLTS = 26;
    private static final int STREAKS = 56;
    /** Seen from at most this far (blocks). */
    private static final double SEEN = 192.0;

    private record Pillar(double x, double ground, double z, float delay, float width, float height, float sway) {
    }

    private record Bolt(Vec3 from, Vec3 offset, float delay, float width, long seed) {
    }

    private record Streak(Vec3 dir, float distance, float delay, float width) {
    }

    private static final class Eruption {
        final int entity;
        final Vec3 centre;
        final long start;
        final long seed;
        final List<Pillar> pillars = new ArrayList<>();
        final List<Bolt> bolts = new ArrayList<>();
        final List<Streak> streaks = new ArrayList<>();
        final boolean[] struck = new boolean[BOLTS];

        Eruption(int entity, Vec3 centre, long start, long seed) {
            this.entity = entity;
            this.centre = centre;
            this.start = start;
            this.seed = seed;
        }
    }

    private static final List<Eruption> ERUPTIONS = new ArrayList<>();
    /** Whether each player (entity id) was Ultimate Wanda at the last tick. */
    private static final Map<Integer, Boolean> WAS = new HashMap<>();

    private ChaosEruptionFx() {
    }

    public static void clear() {
        ERUPTIONS.clear();
        WAS.clear();
    }

    /** Whether an eruption is going on now (tests, and the draw's early out). */
    public static boolean active() {
        return !ERUPTIONS.isEmpty();
    }

    /** Every client tick: a new Ultimate Wanda erupts; the eruptions throw their sparks and end. */
    public static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        WAS.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player));
        for (Player player : level.players()) {
            boolean now = UltimateWanda.is(Powers.get(player));
            Boolean before = WAS.put(player.getId(), now);
            if (now && Boolean.FALSE.equals(before) && camera != null && player.distanceToSqr(camera) < SEEN * SEEN) {
                start(level, player);
            }
        }
        long time = level.getGameTime();
        ERUPTIONS.removeIf(e -> time - e.start > DURATION);
        for (Eruption e : ERUPTIONS) {
            sparks(level, e, time - e.start);
        }
    }

    /** Her eruption begins where she stands. */
    public static void start(ClientLevel level, Player player) {
        long seed = level.random.nextLong();
        Eruption e = new Eruption(player.getId(), player.position(), level.getGameTime(), seed);
        RandomSource r = RandomSource.create(seed);
        for (int i = 0; i < PILLARS; i++) {
            double a = Math.PI * 2 * i / PILLARS + r.nextDouble() * 0.4;
            // (more near her than far out, but some at the very edge)
            double d = 2.5 + Math.pow(r.nextDouble(), 0.8) * (REACH - 2.5);
            double x = e.centre.x + Math.cos(a) * d;
            double z = e.centre.z + Math.sin(a) * d;
            e.pillars.add(new Pillar(x, ground(level, e.centre, x, z), z, (float) (d / REACH * 18.0 + r.nextDouble() * 22.0),
                    0.7F + r.nextFloat() * 1.9F, 45.0F + r.nextFloat() * 85.0F, (r.nextFloat() - 0.5F) * 6.0F));
        }
        for (int i = 0; i < BOLTS; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = r.nextDouble() * 45.0;
            Vec3 from = new Vec3(Math.cos(a) * d, 95.0 + r.nextDouble() * 50.0, Math.sin(a) * d);
            double b = r.nextDouble() * Math.PI * 2;
            double near = i % 3 == 0 ? 0.0 : 3.0 + r.nextDouble() * (REACH - 6.0);
            Vec3 offset = new Vec3(Math.cos(b) * near, 0, Math.sin(b) * near);
            e.bolts.add(new Bolt(from, offset, 4.0F + r.nextFloat() * 54.0F, 0.9F + r.nextFloat() * 1.3F, r.nextLong()));
        }
        for (int i = 0; i < STREAKS; i++) {
            // (from all round her, the sky and the horizon, a few from low down)
            double yaw = r.nextDouble() * Math.PI * 2;
            double pitch = Math.asin(-0.25 + r.nextDouble() * 1.2);
            pitch = Mth.clamp(pitch, -0.6, Math.PI / 2 * 0.95);
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            e.streaks.add(new Streak(dir, 28.0F + r.nextFloat() * 26.0F, r.nextFloat() * 60.0F, 0.5F + r.nextFloat() * 0.9F));
        }
        ERUPTIONS.add(e);
        Vec3 c = e.centre;
        level.playLocalSound(c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.55F, false);
        level.playLocalSound(c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6F, 0.5F, false);
    }

    /** Where the ground is under (x, z): the top of the land, unless she is under a roof (then her own feet). */
    private static double ground(ClientLevel level, Vec3 centre, double x, double z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        if (top <= level.getMinBuildHeight() + 1) {
            return centre.y - 2.0; // (the void: from below her)
        }
        if (top > centre.y + 6.0) {
            return centre.y;
        }
        return top;
    }

    private static Vec3 heart(ClientLevel level, Eruption e, float partial) {
        Entity her = level.getEntity(e.entity);
        return her != null ? her.getPosition(partial).add(0, her.getBbHeight() * 0.55, 0) : e.centre.add(0, 1.0, 0);
    }

    /** Big sparks along the columns and round her, and thunder where a bolt strikes. */
    private static void sparks(ClientLevel level, Eruption e, long age) {
        RandomSource r = level.random;
        for (Pillar p : e.pillars) {
            float u = age - p.delay;
            if (u < 0 || u > 26) {
                continue;
            }
            for (int i = 0; i < 2; i++) {
                double y = p.ground + r.nextDouble() * Math.min(p.height, u * 9.0) * 0.5;
                level.addParticle(new EnergyParticleOptions(i == 0 ? RED : LIGHT, 0.7F + r.nextFloat() * 0.6F),
                        p.x + (r.nextDouble() - 0.5) * p.width, y, p.z + (r.nextDouble() - 0.5) * p.width,
                        (r.nextDouble() - 0.5) * 0.2, 0.6 + r.nextDouble() * 0.9, (r.nextDouble() - 0.5) * 0.2);
            }
            if (u < 1) {
                // (the ground bursts where it comes out)
                for (int i = 0; i < 10; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? LIGHT : RED, 0.6F + r.nextFloat() * 0.5F), p.x, p.ground + 0.2,
                            p.z, Math.cos(a) * 0.45, 0.2 + r.nextDouble() * 0.4, Math.sin(a) * 0.45);
                }
            }
        }
        Vec3 heart = heart(level, e, 0.0F);
        if (age < 50) {
            for (int i = 0; i < 10; i++) {
                Vec3 v = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6 + 0.3, r.nextGaussian()).normalize().scale(0.5 + r.nextDouble() * 0.8);
                level.addParticle(new EnergyParticleOptions(i % 4 == 0 ? LIGHT : RED, 0.7F + r.nextFloat() * 0.6F), heart.x, heart.y, heart.z,
                        v.x, v.y, v.z);
            }
        }
        for (int i = 0; i < e.bolts.size(); i++) {
            Bolt b = e.bolts.get(i);
            if (!e.struck[i] && age >= b.delay + 3) {
                e.struck[i] = true;
                Vec3 at = strike(level, e, b, 0.0F);
                if (i % 3 == 0) {
                    level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4F, 0.6F + r.nextFloat() * 0.3F, false);
                }
                for (int k = 0; k < 14; k++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    level.addParticle(new EnergyParticleOptions(k % 3 == 0 ? LIGHT : RED, 0.6F + r.nextFloat() * 0.5F), at.x, at.y + 0.3, at.z,
                            Math.cos(a) * 0.5, 0.3 + r.nextDouble() * 0.5, Math.sin(a) * 0.5);
                }
            }
        }
    }

    /** Where a bolt from the sky lands: on her (the first of every three), or on the ground round her. */
    private static Vec3 strike(ClientLevel level, Eruption e, Bolt b, float partial) {
        if (b.offset.lengthSqr() < 1.0E-4) {
            return heart(level, e, partial);
        }
        double x = e.centre.x + b.offset.x;
        double z = e.centre.z + b.offset.z;
        return new Vec3(x, ground(level, e.centre, x, z), z);
    }

    /** Draws every eruption (camera-relative, into the additive energy buffer). */
    public static void render(ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, boolean herOwnView,
                              Entity viewer) {
        float now = level.getGameTime() + partial;
        for (Eruption e : ERUPTIONS) {
            float t = now - e.start;
            if (t < 0 || t > DURATION || e.centre.distanceToSqr(camera) > SEEN * SEEN) {
                continue;
            }
            boolean own = herOwnView && viewer != null && viewer.getId() == e.entity;
            draw(level, e, pose, vc, camera, partial, t, own);
        }
    }

    private static void draw(ClientLevel level, Eruption e, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partial, float t,
                             boolean own) {
        Vec3 heart = heart(level, e, partial).subtract(camera);
        Vec3 feet = e.centre.subtract(camera);
        float fade = 1.0F - Mth.clamp((t - (DURATION - 16)) / 16.0F, 0.0F, 1.0F);

        // the column of light on her, out of the ground and into the sky (not over her own eyes)
        float rise = ease(Mth.clamp(t / 7.0F, 0.0F, 1.0F));
        float col = fade * (t < 46 ? 1.0F : Math.max(0.0F, 1.0F - (t - 46) / 22.0F));
        if (col > 0.0F && !own) {
            Vec3 top = feet.add(0, 4.0 + 150.0 * rise, 0);
            float pulse = 1.0F + 0.15F * Mth.sin(t * 0.9F);
            column(pose, vc, feet.add(0, -1.0, 0), top, 3.2F * pulse, col);
        }

        // rings of it running out over the ground, one after another
        for (int k = 0; k < 4; k++) {
            float u = t - k * 9.0F;
            if (u < 0 || u > 26) {
                continue;
            }
            float radius = 1.0F + u * (float) (REACH / 24.0);
            float a = fade * (1.0F - u / 26.0F);
            Vec3 at = feet.add(0, 0.15 + k * 0.05, 0);
            EnergyGeometry.ring(pose, vc, at, new Vec3(0, 1, 0), radius, 1.6F, 72, EnergyGeometry.argb(RED, 0.35F * a));
            EnergyGeometry.ring(pose, vc, at, new Vec3(0, 1, 0), radius, 0.6F, 72, EnergyGeometry.argb(RED, 0.8F * a));
            EnergyGeometry.ring(pose, vc, at, new Vec3(0, 1, 0), radius, 0.2F, 72, EnergyGeometry.argb(LIGHT, 0.9F * a));
        }

        // the columns that burst out of the ground and shoot up into the high sky
        for (Pillar p : e.pillars) {
            float u = t - p.delay;
            if (u < 0 || u > 30) {
                continue;
            }
            float up = ease(Mth.clamp(u / 6.0F, 0.0F, 1.0F));
            float leave = ease(Mth.clamp((u - 12.0F) / 18.0F, 0.0F, 1.0F));
            double base = p.ground - 0.5 + p.height * 1.1 * leave;
            double top = p.ground + p.height * up + p.height * 0.6 * leave;
            float a = fade * (u < 2 ? u / 2.0F : 1.0F) * (1.0F - leave * 0.7F);
            Vec3 b = new Vec3(p.x, base, p.z).subtract(camera);
            Vec3 tp = new Vec3(p.x + p.sway * up, top, p.z + p.sway * 0.5 * up).subtract(camera);
            column(pose, vc, b, tp, p.width, a);
            if (u < 10) {
                // (the ground cracks open round it in a ring of light)
                float ra = fade * (1.0F - u / 10.0F);
                EnergyGeometry.ring(pose, vc, new Vec3(p.x, p.ground + 0.1, p.z).subtract(camera), new Vec3(0, 1, 0),
                        p.width * (0.8F + u * 0.35F), 0.35F, 20, EnergyGeometry.argb(LIGHT, 0.8F * ra));
            }
        }

        // bolts of it falling from far above
        for (Bolt bo : e.bolts) {
            float u = t - bo.delay;
            if (u < 0 || u > 12) {
                continue;
            }
            Vec3 to = strike(level, e, bo, partial).subtract(camera);
            Vec3 from = e.centre.add(bo.from).subtract(camera);
            float reach = ease(Mth.clamp(u / 3.0F, 0.0F, 1.0F));
            Vec3 end = from.add(to.subtract(from).scale(reach));
            float a = fade * (u < 3 ? 1.0F : 1.0F - (u - 3) / 9.0F);
            List<Vec3> path = EnergyGeometry.flowingPath(from, end, 7.0, bo.seed, u * 0.5F);
            EnergyGeometry.ribbon(pose, vc, from, end, bo.width * 5.0F, EnergyGeometry.argb(DEEP, 0.0F), EnergyGeometry.argb(RED, 0.25F * a));
            EnergyGeometry.polyline(pose, vc, path, bo.width * 1.8F, EnergyGeometry.argb(RED, 0.75F * a));
            EnergyGeometry.polyline(pose, vc, path, bo.width * 0.55F, EnergyGeometry.argb(LIGHT, 0.95F * a));
            if (u >= 3) {
                // (a flash where it lands)
                float f = a * (1.0F - (u - 3) / 9.0F);
                EnergyGeometry.ring(pose, vc, to.add(0, 0.12, 0), new Vec3(0, 1, 0), 1.0F + (u - 3) * 0.9F, 0.7F, 28,
                        EnergyGeometry.argb(RED, 0.8F * f));
                EnergyGeometry.ribbon(pose, vc, to, to.add(0, 6.0 * f, 0), 2.4F * f, 0.0F, EnergyGeometry.argb(LIGHT, 0.9F * f),
                        EnergyGeometry.argb(RED, 0.0F));
            }
        }

        // streaks of it flying in at her from every side
        for (Streak s : e.streaks) {
            float u = t - s.delay;
            if (u < 0 || u > 14) {
                continue;
            }
            float head = ease(Mth.clamp(u / 12.0F, 0.0F, 1.0F));
            float tail = ease(Mth.clamp((u - 3.5F) / 12.0F, 0.0F, 1.0F));
            Vec3 h = heart.add(s.dir.scale(s.distance * (1.0F - head)));
            Vec3 tl = heart.add(s.dir.scale(s.distance * (1.0F - tail)));
            float a = fade * Mth.clamp(u / 2.0F, 0.0F, 1.0F) * (head > 0.97F ? 0.3F : 1.0F);
            EnergyGeometry.ribbon(pose, vc, h, tl, s.width * 3.0F, 0.0F, EnergyGeometry.argb(RED, 0.35F * a), EnergyGeometry.argb(DEEP, 0.0F));
            EnergyGeometry.ribbon(pose, vc, h, tl, s.width, 0.0F, EnergyGeometry.argb(LIGHT, 0.95F * a), EnergyGeometry.argb(RED, 0.0F));
        }
    }

    /** A column of energy: a wide soft glow, a red body and a pale core, each fading out towards the top. */
    private static void column(Matrix4f pose, VertexConsumer vc, Vec3 bottom, Vec3 top, float width, float alpha) {
        Vec3 mid = bottom.add(top).scale(0.5);
        EnergyGeometry.ribbon(pose, vc, bottom, mid, width * 3.2F, width * 2.6F, EnergyGeometry.argb(DEEP, 0.3F * alpha),
                EnergyGeometry.argb(DEEP, 0.22F * alpha));
        EnergyGeometry.ribbon(pose, vc, mid, top, width * 2.6F, width * 1.2F, EnergyGeometry.argb(DEEP, 0.22F * alpha),
                EnergyGeometry.argb(DEEP, 0.0F));
        EnergyGeometry.ribbon(pose, vc, bottom, mid, width, width * 0.9F, EnergyGeometry.argb(RED, 0.85F * alpha),
                EnergyGeometry.argb(RED, 0.7F * alpha));
        EnergyGeometry.ribbon(pose, vc, mid, top, width * 0.9F, width * 0.4F, EnergyGeometry.argb(RED, 0.7F * alpha),
                EnergyGeometry.argb(RED, 0.0F));
        EnergyGeometry.ribbon(pose, vc, bottom, top, width * 0.32F, width * 0.08F, EnergyGeometry.argb(LIGHT, 0.95F * alpha),
                EnergyGeometry.argb(LIGHT, 0.0F));
    }

    private static float ease(float x) {
        return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
    }
}
