package com.kolodkingal.enhanced.client.magic;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicFx;
import com.kolodkingal.enhanced.magic.MagicFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultimate.UltimateForm;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The light of 3.17's magic, drawn with the other energy effects ({@code WorldEffectsRenderer}, additive): the shapes
 * the server sends ({@link MagicFxPayload}: shockwaves, bolts, circles, domes, portals, trails, hands, corridors...),
 * and, from every player's synced state, a chanting caster's runes, a broom of light, and an ultimate form's aura
 * ({@link UltimateForm.Aura}) with the awakening's column of light.
 */
public final class MagicFxRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /** The shapes being drawn, with the game time each began. */
    private static final List<Active> ACTIVE = new ArrayList<>();

    private record Active(MagicFxPayload fx, long start) {
        float age(float time) {
            return time - start;
        }
    }

    private MagicFxRenderer() {
    }

    /** A shape from the server (or the order to take one down). */
    public static void add(MagicFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (payload.kind() == MagicFx.CANCEL) {
            ACTIVE.removeIf(a -> a.fx().id() == payload.id());
            return;
        }
        if (ACTIVE.size() > 256) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new Active(payload, mc.level.getGameTime()));
    }

    public static void clear() {
        ACTIVE.clear();
        com.kolodkingal.enhanced.client.render.VisionFlameFx.clear();
        DemiurgeFx.clear();
    }

    /** Draws everything for this frame. */
    public static void render(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                              float partialTick, float time) {
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            float age = a.age(time);
            if (age > a.fx().duration() + 1 || age < -40) {
                it.remove();
                continue;
            }
            draw(level, a.fx(), Mth.clamp(age / Math.max(1, a.fx().duration()), 0.0F, 1.0F), age, pose, vc, camera,
                    partialTick, time);
        }
        com.kolodkingal.enhanced.client.render.VisionFlameFx.forgetCold(level);
        if (com.kolodkingal.enhanced.ultimate.NexusBeing.heldOnClient(level)) {
            DemiurgeFx.nexusSky(mc, pose, vc, partialTick, time);
        }
        for (AbstractClientPlayer player : level.players()) {
            if (player.isInvisible() && player != mc.player) {
                continue;
            }
            PowerData data = Powers.get(player);
            data.setBorrowed(com.kolodkingal.enhanced.ultimate.Demiurge.borrowed(player)); // (the Demiurge's Magic types)
            boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            if (com.kolodkingal.enhanced.magic.Magic.isDemiurge(player)) {
                DemiurgeFx.render(mc, player, data, pose, vc, camera, partialTick, time, firstPerson);
            }
            if (com.kolodkingal.enhanced.ultimate.NexusBeing.is(player)) {
                DemiurgeFx.nexus(player, pose, vc, camera, partialTick, time, firstPerson);
            }
            // (Source Code Vision burns while he flies: his own living flames, never the game's fire)
            if (com.kolodkingal.enhanced.client.render.VisionFlameFx.burning(player, data)) {
                com.kolodkingal.enhanced.client.render.VisionFlameFx.render(mc, player, pose, vc, camera, partialTick, time,
                        firstPerson);
            }
            if (data.is(Live.CHANTING)) {
                chant(player, pose, vc, camera, partialTick, time, firstPerson);
            }
            if (data.is(Live.BROOM) && !firstPerson) {
                broom(player, pose, vc, camera, partialTick, time);
            }
            if (data.is(Live.ULTIMATE) || data.is(Live.ULTIMATE_AWAKENING)) {
                UltimateForm form = UltimateForm.of(player);
                if (form != null) {
                    aura(player, form, data.is(Live.ULTIMATE_AWAKENING), pose, vc, camera, partialTick, time, firstPerson);
                }
            }
        }
    }

    // ==================================================================================================================
    // The shapes of the server

    private static void draw(ClientLevel level, MagicFxPayload fx, float t, float age, Matrix4f pose, VertexConsumer vc,
                             Vec3 camera, float partialTick, float time) {
        int c = fx.color();
        int g = fx.color2() > 0xFF ? fx.color2() : 0xFFFFFF;
        Vec3 o = fx.origin().subtract(camera);
        float fade = edgeFade(age, fx.duration());
        Entity entity = fx.entity() >= 0 ? level.getEntity(fx.entity()) : null;
        switch (fx.kind()) {
            case MagicFx.RING -> {
                float ease = 1.0F - (1.0F - t) * (1.0F - t);
                float r = fx.size() * ease;
                float a = 1.0F - t;
                Vec3 at = o.add(0, 0.15, 0);
                EnergyGeometry.ring(pose, vc, at, UP, r, 0.45F * a + 0.05F, 48, EnergyGeometry.argb(c, 0.55F * a));
                EnergyGeometry.ring(pose, vc, at, UP, r * 0.92F, 0.12F, 48, EnergyGeometry.argb(g, 0.8F * a));
                EnergyGeometry.ring(pose, vc, at, UP, r * 0.6F, 0.2F, 32, EnergyGeometry.argb(c, 0.3F * a));
                for (int i = 0; i < 16; i++) {
                    double ang = i * Math.PI / 8 + t;
                    Vec3 base = at.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
                    EnergyGeometry.ribbon(pose, vc, base, base.add(0, 1.6 * a, 0), 0.18F * a, 0.0F,
                            EnergyGeometry.argb(c, 0.6F * a), EnergyGeometry.argb(g, 0.0F));
                }
            }
            case MagicFx.BOLTS -> {
                long seed = fx.origin().hashCode() * 31L + (long) (age / 2);
                for (Vec3 p : fx.points()) {
                    Vec3 at = p.subtract(camera);
                    Vec3 top = at.add(0, 16, 0);
                    float a = (1.0F - t) * (age % 3 < 2 ? 1.0F : 0.6F);
                    EnergyGeometry.bolt(pose, vc, top, at, 0.45F, 1.6F, EnergyGeometry.argb(c, 0.35F * a), seed);
                    EnergyGeometry.bolt(pose, vc, top, at, 0.14F, 1.6F, EnergyGeometry.argb(g, 0.95F * a), seed);
                    EnergyGeometry.ring(pose, vc, at.add(0, 0.1, 0), UP, 0.6F + 1.6F * t, 0.2F, 20, EnergyGeometry.argb(c, 0.7F * a));
                    seed += 7919L;
                }
            }
            case MagicFx.BEAMS -> {
                float a = (1.0F - t);
                for (Vec3 p : fx.points()) {
                    Vec3 at = p.subtract(camera);
                    float pulse = 0.85F + 0.15F * Mth.sin(time * 1.3F);
                    EnergyGeometry.ribbon(pose, vc, o, at, 0.5F * pulse, EnergyGeometry.argb(c, 0.3F * a), EnergyGeometry.argb(c, 0.3F * a));
                    EnergyGeometry.ribbon(pose, vc, o, at, 0.12F, EnergyGeometry.argb(g, 0.9F * a), EnergyGeometry.argb(g, 0.9F * a));
                    EnergyGeometry.ring(pose, vc, at, at.subtract(o), 0.5F, 0.12F, 16, EnergyGeometry.argb(g, 0.7F * a));
                }
            }
            case MagicFx.DRAIN -> {
                for (Vec3 p : fx.points()) {
                    Vec3 at = p.subtract(camera);
                    long seed = (long) (p.x * 100) ^ (long) (p.z * 100);
                    EnergyGeometry.bolt(pose, vc, at, o, 0.22F, 0.5F, EnergyGeometry.argb(c, 0.5F * fade), seed, age / 4.0F);
                    for (int k = 0; k < 4; k++) {
                        float f = ((age * 0.06F) + k / 4.0F) % 1.0F;
                        Vec3 m = at.add(o.subtract(at).scale(f));
                        EnergyGeometry.ring(pose, vc, m, o.subtract(at), 0.12F, 0.08F, 8, EnergyGeometry.argb(g, 0.9F * fade));
                    }
                }
            }
            case MagicFx.BIND -> {
                float h = fx.size() <= 0 ? 1.8F : fx.size();
                for (Vec3 p : fx.points()) {
                    Vec3 at = p.subtract(camera);
                    for (int k = 0; k < 3; k++) {
                        float y = h * (0.2F + 0.32F * k);
                        float tilt = Mth.sin(time * 0.15F + k * 2.1F) * 0.25F;
                        Vec3 n = new Vec3(tilt, 1, tilt * 0.6);
                        float r = 0.55F + 0.08F * Mth.sin(time * 0.4F + k);
                        EnergyGeometry.ring(pose, vc, at.add(0, y, 0), n, r, 0.14F, 20, EnergyGeometry.argb(c, 0.75F * fade));
                        EnergyGeometry.ring(pose, vc, at.add(0, y, 0), n, r, 0.05F, 20, EnergyGeometry.argb(g, 0.9F * fade));
                    }
                }
            }
            case MagicFx.VORTEX -> {
                float r0 = fx.size();
                for (int arm = 0; arm < 5; arm++) {
                    List<Vec3> pts = new ArrayList<>();
                    for (int i = 0; i <= 24; i++) {
                        float f = i / 24.0F;
                        float ang = time * 0.35F + arm * Mth.TWO_PI / 5 + f * 9.0F;
                        float r = r0 * (1.0F - f * 0.85F);
                        pts.add(o.add(Mth.cos(ang) * r, 0.2 + f * 3.0, Mth.sin(ang) * r));
                    }
                    EnergyGeometry.polyline(pose, vc, pts, 0.16F, EnergyGeometry.argb(arm % 2 == 0 ? c : g, 0.65F * fade));
                }
                EnergyGeometry.ring(pose, vc, o.add(0, 0.1, 0), UP, r0, 0.2F, 40, EnergyGeometry.argb(c, 0.5F * fade));
            }
            case MagicFx.CIRCLE -> circle(pose, vc, o.add(0, 0.06, 0), fx.size(), c, g, time, fade);
            case MagicFx.SIGIL -> sigil(pose, vc, o.add(0, 0.07, 0), fx.size(), c, g, time, fade);
            case MagicFx.DOME -> dome(pose, vc, o, fx.size(), c, g, time, fade);
            case MagicFx.PORTAL -> {
                Vec3 n = fx.points().isEmpty() ? new Vec3(0, 0, 1) : fx.points().get(0);
                portal(pose, vc, o, n, fx.size() * Math.min(1.0F, age / 10.0F), c, g, time, fade);
            }
            case MagicFx.TRAIL -> {
                List<Vec3> pts = new ArrayList<>();
                for (Vec3 p : fx.points()) {
                    pts.add(p.subtract(camera).add(0, 0.15, 0));
                }
                EnergyGeometry.polyline(pose, vc, pts, 0.18F, EnergyGeometry.argb(c, 0.55F * fade));
                if (pts.size() > 1) {
                    int n = pts.size() - 1;
                    for (int k = 0; k < 3; k++) {
                        float f = ((age * 0.025F) + k / 3.0F) % 1.0F;
                        int i = Math.min(n - 1, (int) (f * n));
                        float in = f * n - i;
                        Vec3 m = pts.get(i).add(pts.get(i + 1).subtract(pts.get(i)).scale(in));
                        EnergyGeometry.ring(pose, vc, m, UP, 0.35F, 0.1F, 14, EnergyGeometry.argb(g, 0.9F * fade));
                    }
                }
            }
            case MagicFx.AWAKEN -> {
                Vec3 base = entity != null ? entity.getPosition(partialTick).subtract(camera) : o;
                column(pose, vc, base, c, g, time, 1.0F - t);
            }
            case MagicFx.HAND -> {
                Vec3 dir = fx.points().isEmpty() ? new Vec3(0, 0, 1) : fx.points().get(0).normalize();
                hand(pose, vc, o.add(dir.scale(fx.size() * (0.3 + 0.7 * t))).add(0, 1.2, 0), dir, fx.size() / 4.0F, c, g, 1.0F - t);
            }
            case MagicFx.DISKS -> {
                if (entity != null) {
                    Vec3 at = entity.getPosition(partialTick).subtract(camera).add(0, 1.0, 0);
                    for (int i = 0; i < 3; i++) {
                        float ang = time * 0.3F + i * Mth.TWO_PI / 3;
                        Vec3 d = at.add(Mth.cos(ang) * fx.size(), 0.15 * Mth.sin(time * 0.2F + i), Mth.sin(ang) * fx.size());
                        disk(pose, vc, d, 0.5F, c, g, time * 1.4F, fade);
                    }
                }
            }
            case MagicFx.HALLWAY -> {
                if (entity != null) {
                    Vec3 target = entity.getPosition(partialTick).subtract(camera);
                    hallway(pose, vc, o, target, c, g, time, fade);
                }
            }
            case MagicFx.GRAVITY -> {
                boolean rising = fx.color2() == 1;
                for (int k = 0; k < 4; k++) {
                    float f = ((age * (rising ? 0.03F : 0.09F)) + k / 4.0F) % 1.0F;
                    float y = rising ? f * 4.5F : (1.0F - f) * 4.5F;
                    EnergyGeometry.ring(pose, vc, o.add(0, y, 0), UP, fx.size() * (rising ? 1.0F : 0.6F + 0.4F * f), 0.12F, 40,
                            EnergyGeometry.argb(k % 2 == 0 ? c : 0xFFFFFF, 0.5F * fade * (rising ? 1.0F - f : f)));
                }
                EnergyGeometry.ring(pose, vc, o.add(0, 0.08, 0), UP, fx.size(), 0.25F, 48, EnergyGeometry.argb(c, 0.45F * fade));
            }
            case MagicFx.SHATTER -> {
                RandomSource random = RandomSource.create(fx.origin().hashCode());
                for (int i = 0; i < 14; i++) {
                    Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6, random.nextGaussian()).normalize();
                    float len = fx.size() * (0.6F + random.nextFloat() * 0.6F) * Math.min(1.0F, t * 3.0F);
                    EnergyGeometry.bolt(pose, vc, o, o.add(d.scale(len)), 0.08F, 0.35F, EnergyGeometry.argb(g, 0.9F * (1.0F - t)),
                            random.nextLong());
                    Vec3 shard = o.add(d.scale(fx.size() * 0.4 + t * fx.size()));
                    EnergyGeometry.ribbon(pose, vc, shard, shard.add(d.scale(0.4)), 0.2F, EnergyGeometry.argb(c, 0.7F * (1.0F - t)),
                            EnergyGeometry.argb(g, 0.2F * (1.0F - t)));
                }
            }
            case MagicFx.STARBURST -> {
                RandomSource random = RandomSource.create(fx.origin().hashCode() + 17);
                for (int i = 0; i < 24; i++) {
                    Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                    float len = fx.size() * t;
                    EnergyGeometry.ribbon(pose, vc, o.add(d.scale(len * 0.4)), o.add(d.scale(len)), 0.25F * (1.0F - t), 0.0F,
                            EnergyGeometry.argb(i % 3 == 0 ? g : c, 0.85F * (1.0F - t)), EnergyGeometry.argb(c, 0.0F));
                }
            }
            default -> {
            }
        }
    }

    /** Lasting shapes fade in over their first 8 ticks and out over their last 12. */
    private static float edgeFade(float age, int duration) {
        float in = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        float out = Mth.clamp((duration - age) / 12.0F, 0.0F, 1.0F);
        return in * out;
    }

    // ---- lasting shapes ----

    private static void circle(Matrix4f pose, VertexConsumer vc, Vec3 o, float r, int c, int g, float time, float fade) {
        float pulse = 0.8F + 0.2F * Mth.sin(time * 0.2F);
        EnergyGeometry.ring(pose, vc, o, UP, r, 0.18F, 64, EnergyGeometry.argb(c, 0.7F * fade * pulse));
        EnergyGeometry.ring(pose, vc, o, UP, r, 0.05F, 64, EnergyGeometry.argb(g, 0.9F * fade));
        EnergyGeometry.ring(pose, vc, o, UP, r * 0.82F, 0.08F, 56, EnergyGeometry.argb(c, 0.6F * fade));
        // the runes between the two rings, turning
        float turn = time * 0.01F;
        for (int i = 0; i < 18; i++) {
            float a = turn + i * Mth.TWO_PI / 18;
            Vec3 d = new Vec3(Mth.cos(a), 0, Mth.sin(a));
            Vec3 side = new Vec3(-d.z, 0, d.x);
            Vec3 m = o.add(d.scale(r * 0.91));
            EnergyGeometry.ribbon(pose, vc, m.add(side.scale(0.12)), m.subtract(side.scale(0.12)).add(d.scale(0.1)), 0.05F,
                    EnergyGeometry.argb(g, 0.8F * fade), EnergyGeometry.argb(g, 0.8F * fade));
        }
        // a six-pointed star inside, turning the other way
        star(pose, vc, o, r * 0.8F, 6, 2, -turn * 0.7F, EnergyGeometry.argb(c, 0.55F * fade));
        // motes rising off the rim
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8 + time * 0.02F;
            float f = ((time * 0.02F) + i * 0.13F) % 1.0F;
            Vec3 m = o.add(Mth.cos(a) * r, f * 1.6, Mth.sin(a) * r);
            EnergyGeometry.ring(pose, vc, m, UP, 0.06F, 0.05F, 6, EnergyGeometry.argb(g, 0.8F * fade * (1.0F - f)));
        }
    }

    private static void sigil(Matrix4f pose, VertexConsumer vc, Vec3 o, float r, int c, int g, float time, float fade) {
        float turn = time * 0.03F;
        EnergyGeometry.ring(pose, vc, o, UP, r, 0.1F, 48, EnergyGeometry.argb(c, 0.7F * fade));
        star(pose, vc, o, r, 7, 3, turn, EnergyGeometry.argb(g, 0.75F * fade));
        star(pose, vc, o, r * 0.45F, 5, 2, -turn * 1.5F, EnergyGeometry.argb(c, 0.8F * fade));
        EnergyGeometry.ring(pose, vc, o, UP, r * 0.45F, 0.06F, 32, EnergyGeometry.argb(g, 0.6F * fade));
    }

    /** A star polygon {n/step} on the ground. */
    private static void star(Matrix4f pose, VertexConsumer vc, Vec3 o, float r, int n, int step, float turn, int argb) {
        for (int i = 0; i < n; i++) {
            float a0 = turn + i * Mth.TWO_PI / n;
            float a1 = turn + ((i + step) % n) * Mth.TWO_PI / n;
            EnergyGeometry.ribbon(pose, vc, o.add(Mth.cos(a0) * r, 0, Mth.sin(a0) * r), o.add(Mth.cos(a1) * r, 0, Mth.sin(a1) * r),
                    0.07F, argb, argb);
        }
    }

    private static void dome(Matrix4f pose, VertexConsumer vc, Vec3 o, float r, int c, int g, float time, float fade) {
        for (int k = 0; k < 6; k++) {
            float lat = k * (Mth.HALF_PI / 6.0F);
            float y = r * Mth.sin(lat);
            float rr = r * Mth.cos(lat);
            EnergyGeometry.ring(pose, vc, o.add(0, y, 0), UP, rr, 0.07F, 48, EnergyGeometry.argb(k == 0 ? g : c, 0.4F * fade));
        }
        float turn = time * 0.01F;
        for (int m = 0; m < 10; m++) {
            float lon = turn + m * Mth.TWO_PI / 10;
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 10; k++) {
                float lat = k * (Mth.HALF_PI / 10.0F);
                pts.add(o.add(Mth.cos(lon) * r * Mth.cos(lat), r * Mth.sin(lat), Mth.sin(lon) * r * Mth.cos(lat)));
            }
            EnergyGeometry.polyline(pose, vc, pts, 0.06F, EnergyGeometry.argb(c, 0.35F * fade));
        }
        // a glimmer running round it
        float f = (time * 0.01F) % 1.0F;
        float lat = f * Mth.HALF_PI;
        EnergyGeometry.ring(pose, vc, o.add(0, r * Mth.sin(lat), 0), UP, r * Mth.cos(lat), 0.12F, 48,
                EnergyGeometry.argb(g, 0.6F * fade));
    }

    private static void portal(Matrix4f pose, VertexConsumer vc, Vec3 o, Vec3 normal, float r, int c, int g, float time, float fade) {
        Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : normal.normalize();
        Vec3 center = o.add(0, r + 0.1, 0);
        EnergyGeometry.ring(pose, vc, center, n, r, 0.22F, 48, EnergyGeometry.argb(c, 0.75F * fade));
        EnergyGeometry.ring(pose, vc, center, n, r, 0.07F, 48, EnergyGeometry.argb(g, 0.95F * fade));
        Vec3 u = n.cross(UP);
        if (u.lengthSqr() < 1.0E-6) {
            u = new Vec3(1, 0, 0);
        }
        u = u.normalize();
        Vec3 v = n.cross(u).normalize();
        // sparks thrown off the rim, as the Sling Ring's
        for (int i = 0; i < 16; i++) {
            float a = time * 0.25F + i * Mth.TWO_PI / 16;
            Vec3 rim = center.add(u.scale(Mth.cos(a) * r)).add(v.scale(Mth.sin(a) * r));
            Vec3 tangent = u.scale(-Mth.sin(a)).add(v.scale(Mth.cos(a)));
            EnergyGeometry.ribbon(pose, vc, rim, rim.add(tangent.scale(0.5)).add(u.scale(Mth.cos(a) * 0.15)).add(v.scale(Mth.sin(a) * 0.15)),
                    0.08F, 0.0F, EnergyGeometry.argb(g, 0.9F * fade), EnergyGeometry.argb(c, 0.0F));
        }
        // the swirl inside
        for (int arm = 0; arm < 3; arm++) {
            List<Vec3> pts = new ArrayList<>();
            for (int i = 0; i <= 16; i++) {
                float f = i / 16.0F;
                float a = -time * 0.15F + arm * Mth.TWO_PI / 3 + f * 5.0F;
                float rr = r * (1.0F - f) * 0.92F;
                pts.add(center.add(u.scale(Mth.cos(a) * rr)).add(v.scale(Mth.sin(a) * rr)));
            }
            EnergyGeometry.polyline(pose, vc, pts, 0.1F, EnergyGeometry.argb(c, 0.4F * fade));
        }
    }

    private static void column(Matrix4f pose, VertexConsumer vc, Vec3 base, int c, int g, float time, float a) {
        EnergyGeometry.ribbon(pose, vc, base, base.add(0, 14, 0), 1.6F * a, 0.2F * a, EnergyGeometry.argb(c, 0.35F * a),
                EnergyGeometry.argb(c, 0.0F));
        EnergyGeometry.ribbon(pose, vc, base, base.add(0, 10, 0), 0.4F * a, 0.05F, EnergyGeometry.argb(g, 0.8F * a),
                EnergyGeometry.argb(g, 0.0F));
        for (int k = 0; k < 3; k++) {
            float f = ((time * 0.04F) + k / 3.0F) % 1.0F;
            EnergyGeometry.ring(pose, vc, base.add(0, f * 2.4, 0), UP, 1.3F - 0.5F * f, 0.12F, 28,
                    EnergyGeometry.argb(k % 2 == 0 ? c : g, 0.8F * a * (1.0F - f)));
        }
    }

    private static void hand(Matrix4f pose, VertexConsumer vc, Vec3 palm, Vec3 dir, float s, int c, int g, float a) {
        Vec3 side = dir.cross(UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        EnergyGeometry.ring(pose, vc, palm, dir, 1.0F * s, 0.25F, 24, EnergyGeometry.argb(c, 0.7F * a));
        EnergyGeometry.ring(pose, vc, palm, dir, 0.7F * s, 0.08F, 24, EnergyGeometry.argb(g, 0.9F * a));
        for (int f = 0; f < 5; f++) {
            float spread = (f - 2) * 0.45F;
            Vec3 knuckle = palm.add(side.scale(spread * s)).add(UP.scale(0.6 * s));
            Vec3 tip = knuckle.add(UP.scale((f == 0 ? 0.9 : 1.5) * s)).add(dir.scale(0.6 * s)).add(side.scale(spread * 0.4 * s));
            if (f == 0) {
                knuckle = palm.add(side.scale(-1.0 * s));
                tip = knuckle.add(side.scale(-0.8 * s)).add(UP.scale(0.8 * s)).add(dir.scale(0.5 * s));
            }
            EnergyGeometry.ribbon(pose, vc, knuckle, tip, 0.45F * s, 0.25F * s, EnergyGeometry.argb(c, 0.6F * a), EnergyGeometry.argb(g, 0.7F * a));
        }
    }

    private static void disk(Matrix4f pose, VertexConsumer vc, Vec3 at, float r, int c, int g, float spin, float fade) {
        EnergyGeometry.ring(pose, vc, at, UP, r, 0.1F, 20, EnergyGeometry.argb(c, 0.8F * fade));
        EnergyGeometry.ring(pose, vc, at, UP, r * 0.55F, 0.06F, 16, EnergyGeometry.argb(g, 0.9F * fade));
        for (int i = 0; i < 4; i++) {
            float a = spin + i * Mth.HALF_PI;
            EnergyGeometry.ribbon(pose, vc, at, at.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), 0.07F, EnergyGeometry.argb(g, 0.8F * fade),
                    EnergyGeometry.argb(c, 0.4F * fade));
        }
    }

    private static void hallway(Matrix4f pose, VertexConsumer vc, Vec3 from, Vec3 to, int c, int g, float time, float fade) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1.0E-3) {
            return;
        }
        Vec3 dir = d.scale(1.0 / len);
        Vec3 side = dir.cross(UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        int frames = 10;
        for (int i = 0; i < frames; i++) {
            float f = ((time * 0.02F) + i / (float) frames) % 1.0F;
            Vec3 center = to.add(dir.scale(-len * (1.0F - f) * 0.9)).add(0, 1.2, 0);
            float w = 1.4F;
            float h = 1.6F;
            Vec3 a = center.add(side.scale(w)).add(0, -h, 0);
            Vec3 b = center.add(side.scale(-w)).add(0, -h, 0);
            Vec3 cc = center.add(side.scale(-w)).add(0, h, 0);
            Vec3 dd = center.add(side.scale(w)).add(0, h, 0);
            int col = EnergyGeometry.argb(i % 2 == 0 ? c : g, 0.5F * fade * (0.3F + 0.7F * f));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.07F, col, col);
            EnergyGeometry.ribbon(pose, vc, b, cc, 0.07F, col, col);
            EnergyGeometry.ribbon(pose, vc, cc, dd, 0.07F, col, col);
            EnergyGeometry.ribbon(pose, vc, dd, a, 0.07F, col, col);
        }
    }

    // ==================================================================================================================
    // From the players' state

    private static Vec3 body(AbstractClientPlayer player, Vec3 camera, float partialTick) {
        return player.getPosition(partialTick).subtract(camera);
    }

    private static float yaw(AbstractClientPlayer player, float partialTick) {
        return Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
    }

    /** A chanting caster: runes circling both hands, a circle forming at the feet. */
    private static void chant(AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick,
                              float time, boolean firstPerson) {
        int c = Magic.casterColor(player);
        int g = Magic.casterGlow(player);
        Vec3 feet = body(player, camera, partialTick);
        float yaw = yaw(player, partialTick);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        float h = player.getBbHeight();
        if (!firstPerson) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 hand = feet.add(0, h * 0.62, 0).add(fwd.scale(0.45)).add(side.scale(0.38 * s));
                float spin = time * 0.3F * s;
                EnergyGeometry.ring(pose, vc, hand, new Vec3(Mth.cos(spin) * 0.4, 1, Mth.sin(spin) * 0.4), 0.22F, 0.05F, 12,
                        EnergyGeometry.argb(g, 0.85F));
                EnergyGeometry.ring(pose, vc, hand, fwd, 0.3F, 0.03F, 12, EnergyGeometry.argb(c, 0.6F));
            }
        }
        sigil(pose, vc, feet.add(0, 0.05, 0), 1.2F, c, g, time, 0.6F + 0.2F * Mth.sin(time * 0.3F));
    }

    /** A broom of light under a rider (Hexenbesen): the stick along him, the bristles fanned behind. */
    private static void broom(AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        int c = Magic.casterColor(player);
        int g = Magic.casterGlow(player);
        Vec3 feet = body(player, camera, partialTick);
        float yaw = yaw(player, partialTick);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 seat = feet.add(0, 0.55 + 0.04 * Mth.sin(time * 0.25F), 0);
        Vec3 front = seat.add(fwd.scale(1.1)).add(0, 0.12, 0);
        Vec3 back = seat.add(fwd.scale(-0.8)).add(0, -0.08, 0);
        EnergyGeometry.ribbon(pose, vc, back, front, 0.16F, EnergyGeometry.argb(c, 0.55F), EnergyGeometry.argb(c, 0.55F));
        EnergyGeometry.ribbon(pose, vc, back, front, 0.05F, EnergyGeometry.argb(g, 0.95F), EnergyGeometry.argb(g, 0.95F));
        EnergyGeometry.ring(pose, vc, back, fwd, 0.1F, 0.05F, 8, EnergyGeometry.argb(g, 0.9F));
        for (int i = 0; i < 9; i++) {
            float spread = (i - 4) / 4.0F;
            float wob = 0.05F * Mth.sin(time * 0.6F + i);
            Vec3 tip = back.add(fwd.scale(-0.75)).add(side.scale(spread * 0.32)).add(0, spread * spread * -0.05 + wob, 0);
            EnergyGeometry.ribbon(pose, vc, back, tip, 0.06F, 0.02F, EnergyGeometry.argb(g, 0.85F), EnergyGeometry.argb(c, 0.3F));
        }
        // a trail of motes behind
        for (int k = 0; k < 5; k++) {
            float f = ((time * 0.08F) + k / 5.0F) % 1.0F;
            Vec3 m = back.add(fwd.scale(-0.8 - f * 2.0)).add(0, -f * 0.2, 0);
            EnergyGeometry.ring(pose, vc, m, UP, 0.07F * (1.0F - f), 0.05F, 6, EnergyGeometry.argb(c, 0.7F * (1.0F - f)));
        }
    }

    /** An ultimate form's aura, eased in while it awakens. */
    private static void aura(AbstractClientPlayer player, UltimateForm form, boolean awakening, Matrix4f pose,
                             VertexConsumer vc, Vec3 camera, float partialTick, float time, boolean firstPerson) {
        int c = form.color(player);
        int g = form.glow(player);
        Vec3 feet = body(player, camera, partialTick);
        float h = player.getBbHeight();
        Vec3 mid = feet.add(0, h * 0.5, 0);
        Vec3 head = feet.add(0, h * 0.92, 0);
        float yaw = yaw(player, partialTick);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        float a = awakening ? 0.6F + 0.4F * Mth.sin(time * 0.5F) : 1.0F;
        if (awakening) {
            column(pose, vc, feet, c, g, time, 0.9F);
        }
        float s = h / 1.8F;
        float dim = firstPerson ? 0.35F : 1.0F;
        a *= dim;
        long seed = player.getId() * 7919L;
        switch (form.aura) {
            case SHARDS -> {
                for (int i = 0; i < 12; i++) {
                    float ang = time * 0.12F + i * Mth.TWO_PI / 12;
                    float tilt = (i % 3 - 1) * 0.5F;
                    Vec3 p = mid.add(Mth.cos(ang) * 1.4 * s, Mth.sin(ang) * tilt * s, Mth.sin(ang) * 1.4 * s);
                    Vec3 q = p.add(Mth.cos(ang + 0.6F) * 0.3, 0.1, Mth.sin(ang + 0.6F) * 0.3);
                    EnergyGeometry.ribbon(pose, vc, p, q, 0.14F, 0.02F, EnergyGeometry.argb(g, 0.85F * a), EnergyGeometry.argb(c, 0.4F * a));
                }
            }
            case RUNES -> {
                EnergyGeometry.ring(pose, vc, mid, new Vec3(0.2, 1, 0), 1.1F * s, 0.08F, 36, EnergyGeometry.argb(c, 0.7F * a));
                EnergyGeometry.ring(pose, vc, mid.add(0, 0.4 * s, 0), new Vec3(-0.25, 1, 0.2), 0.9F * s, 0.06F, 32,
                        EnergyGeometry.argb(g, 0.7F * a));
                for (int i = 0; i < 12; i++) {
                    float ang = time * 0.05F + i * Mth.TWO_PI / 12;
                    Vec3 p = mid.add(Mth.cos(ang) * 1.1 * s, 0.22 * Mth.cos(ang) * s, Mth.sin(ang) * 1.1 * s);
                    EnergyGeometry.ribbon(pose, vc, p.add(0, -0.12, 0), p.add(0, 0.12, 0), 0.08F, EnergyGeometry.argb(g, 0.9F * a),
                            EnergyGeometry.argb(g, 0.9F * a));
                }
                crown(pose, vc, head.add(0, 0.3 * s, 0), 0.32F * s, c, g, time, a);
            }
            case SPEED -> {
                for (int i = 0; i < 6; i++) {
                    float base = time * 0.55F + i * 1.7F;
                    float y = (float) (0.2 + (i % 3) * 0.6) * s;
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k < 7; k++) {
                        float ang = base + k * 0.18F;
                        pts.add(feet.add(Mth.cos(ang) * 1.0 * s, y + k * 0.03, Mth.sin(ang) * 1.0 * s));
                    }
                    EnergyGeometry.polyline(pose, vc, pts, 0.07F, EnergyGeometry.argb(i % 2 == 0 ? g : c, 0.7F * a));
                }
            }
            case SOLAR -> {
                Vec3 brow = head.add(fwd.scale(0.3 * s)).add(0, 0.05, 0);
                for (int i = 0; i < 14; i++) {
                    float ang = time * 0.05F + i * Mth.TWO_PI / 14;
                    float len = (i % 2 == 0 ? 0.9F : 0.5F) * s * (0.85F + 0.15F * Mth.sin(time * 0.3F + i));
                    Vec3 dir = side.scale(Mth.cos(ang)).add(UP.scale(Mth.sin(ang)));
                    EnergyGeometry.ribbon(pose, vc, brow, brow.add(dir.scale(len)), len * 0.18F, 0.0F, EnergyGeometry.argb(g, 0.8F * a),
                            EnergyGeometry.argb(c, 0.0F));
                }
                EnergyGeometry.ring(pose, vc, mid, UP, 1.2F * s, 0.1F, 36, EnergyGeometry.argb(c, 0.5F * a));
            }
            case STARS -> {
                // the Demiurge's star form: a white star's halo and rays, stars in orbit, a pale column
                Vec3 halo = head.add(fwd.scale(-0.35 * s));
                EnergyGeometry.ring(pose, vc, halo, fwd, 0.85F * s, 0.1F, 40, EnergyGeometry.argb(g, 0.9F * a));
                EnergyGeometry.ring(pose, vc, halo, fwd, 0.62F * s, 0.05F, 36, EnergyGeometry.argb(c, 0.8F * a));
                for (int i = 0; i < 16; i++) {
                    float ang = time * 0.02F + i * Mth.TWO_PI / 16;
                    Vec3 dir = side.scale(Mth.cos(ang)).add(UP.scale(Mth.sin(ang)));
                    float len = (i % 2 == 0 ? 1.6F : 1.1F) * s;
                    EnergyGeometry.ribbon(pose, vc, halo.add(dir.scale(0.9 * s)), halo.add(dir.scale(0.9 * s + len)), 0.12F, 0.0F,
                            EnergyGeometry.argb(g, 0.7F * a), EnergyGeometry.argb(c, 0.0F));
                }
                for (int i = 0; i < 10; i++) {
                    float ang = time * (0.04F + i * 0.004F) + i * 2.4F;
                    float tilt = Mth.sin(i * 1.3F) * 0.7F;
                    Vec3 p = mid.add(Mth.cos(ang) * 1.7 * s, Mth.sin(ang) * tilt * s + 0.2, Mth.sin(ang) * 1.7 * s);
                    float tw = 0.18F + 0.06F * Mth.sin(time * 0.5F + i);
                    EnergyGeometry.ribbon(pose, vc, p.add(-tw, 0, 0), p.add(tw, 0, 0), 0.05F, EnergyGeometry.argb(g, 0.95F * a), EnergyGeometry.argb(g, 0.95F * a));
                    EnergyGeometry.ribbon(pose, vc, p.add(0, -tw, 0), p.add(0, tw, 0), 0.05F, EnergyGeometry.argb(g, 0.95F * a), EnergyGeometry.argb(g, 0.95F * a));
                }
                EnergyGeometry.ribbon(pose, vc, feet, feet.add(0, h * 1.4, 0), 1.1F * s, 0.4F, EnergyGeometry.argb(c, 0.18F * a),
                        EnergyGeometry.argb(g, 0.0F));
            }
            case TIMELINES -> {
                for (int i = 0; i < 6; i++) {
                    float ang = i * Mth.TWO_PI / 6 + time * 0.01F;
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k <= 12; k++) {
                        float f = k / 12.0F;
                        float r = (0.5F + f * 1.4F) * s;
                        float wav = 0.2F * Mth.sin(time * 0.1F + f * 6 + i);
                        pts.add(feet.add(Mth.cos(ang + wav) * r, f * h * 1.3, Mth.sin(ang + wav) * r));
                    }
                    EnergyGeometry.polyline(pose, vc, pts, 0.07F, EnergyGeometry.argb(i % 2 == 0 ? c : g, 0.7F * a));
                    Vec3 tip = pts.get(pts.size() - 1);
                    EnergyGeometry.ring(pose, vc, tip, UP, 0.1F, 0.06F, 8, EnergyGeometry.argb(g, 0.9F * a));
                }
            }
            case LIGHTNING -> {
                long step = (long) (time / 2);
                for (int i = 0; i < 4; i++) {
                    float ang = (float) ((seed + i * 2654435761L + step * 40503L) % 628) / 100.0F;
                    Vec3 from = feet.add(Mth.cos(ang) * 0.6 * s, 0.1, Mth.sin(ang) * 0.6 * s);
                    Vec3 to = head.add(Mth.cos(ang + 1.0F) * 0.4 * s, 0.6 * s, Mth.sin(ang + 1.0F) * 0.4 * s);
                    EnergyGeometry.bolt(pose, vc, from, to, 0.08F, 0.35F, EnergyGeometry.argb(g, 0.9F * a), seed + i + step * 13L);
                }
                if (((long) time / 3) % 7 == 0) {
                    EnergyGeometry.bolt(pose, vc, head, head.add(0, 10, 0), 0.12F, 1.2F, EnergyGeometry.argb(c, 0.7F * a), seed + step);
                }
                EnergyGeometry.ring(pose, vc, feet.add(0, 0.1, 0), UP, 1.0F * s, 0.1F, 28, EnergyGeometry.argb(c, 0.5F * a));
            }
            case FLAME -> {
                for (int i = 0; i < 12; i++) {
                    float ang = i * Mth.TWO_PI / 12 + time * 0.03F;
                    float lick = (0.9F + 0.5F * Mth.sin(time * 0.45F + i * 1.9F)) * s;
                    Vec3 base = feet.add(Mth.cos(ang) * 0.55 * s, 0.05, Mth.sin(ang) * 0.55 * s);
                    EnergyGeometry.ribbon(pose, vc, base, base.add(Mth.cos(ang) * 0.1, lick * 1.3, Mth.sin(ang) * 0.1), 0.3F * s, 0.0F,
                            EnergyGeometry.argb(c, 0.6F * a), EnergyGeometry.argb(g, 0.0F));
                }
                wings(pose, vc, feet.add(0, h * 0.72, 0), fwd, side, s, c, g, time, a, true);
            }
            case MIND -> {
                for (int k = 0; k < 3; k++) {
                    float f = ((time * 0.03F) + k / 3.0F) % 1.0F;
                    EnergyGeometry.ring(pose, vc, head, UP, (0.4F + 1.4F * f) * s, 0.08F, 32, EnergyGeometry.argb(k == 0 ? g : c, 0.8F * a * (1.0F - f)));
                }
            }
            case STONES -> {
                int[] stones = {0xFFD24A, 0x3F7BFF, 0xFF3A3A, 0x9B4DEB, 0xFF8A1F, 0x3FD06A};
                for (int i = 0; i < 6; i++) {
                    float ang = time * 0.07F + i * Mth.TWO_PI / 6;
                    Vec3 p = mid.add(Mth.cos(ang) * 1.2 * s, 0.25 * Mth.sin(ang * 2) * s, Mth.sin(ang) * 1.2 * s);
                    EnergyGeometry.ring(pose, vc, p, p.subtract(mid), 0.12F, 0.12F, 10, EnergyGeometry.argb(stones[i], 0.95F * a));
                    EnergyGeometry.ring(pose, vc, p, p.subtract(mid), 0.24F, 0.05F, 10, EnergyGeometry.argb(stones[i], 0.45F * a));
                }
            }
            case NANO -> {
                for (int i = 0; i < 6; i++) {
                    float a0 = time * 0.06F + i * Mth.TWO_PI / 6;
                    float a1 = a0 + Mth.TWO_PI / 6 * 0.8F;
                    Vec3 p0 = mid.add(Mth.cos(a0) * 1.0 * s, 0, Mth.sin(a0) * 1.0 * s);
                    Vec3 p1 = mid.add(Mth.cos(a1) * 1.0 * s, 0, Mth.sin(a1) * 1.0 * s);
                    EnergyGeometry.ribbon(pose, vc, p0, p1, 0.12F, EnergyGeometry.argb(c, 0.7F * a), EnergyGeometry.argb(g, 0.7F * a));
                }
                for (int k = 0; k < 8; k++) {
                    float f = ((time * 0.04F) + k / 8.0F) % 1.0F;
                    float ang = k * 2.2F;
                    Vec3 m = feet.add(Mth.cos(ang) * 0.7 * s, f * h * 1.2, Mth.sin(ang) * 0.7 * s);
                    EnergyGeometry.ribbon(pose, vc, m, m.add(0, 0.12, 0), 0.06F, EnergyGeometry.argb(g, 0.9F * a * (1.0F - f)), EnergyGeometry.argb(g, 0.0F));
                }
            }
            case SAND -> {
                for (int arm = 0; arm < 2; arm++) {
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k <= 20; k++) {
                        float f = k / 20.0F;
                        float ang = time * 0.2F + arm * Mth.PI + f * 10.0F;
                        float r = (1.3F - 0.7F * f) * s;
                        pts.add(feet.add(Mth.cos(ang) * r, f * h * 1.2, Mth.sin(ang) * r));
                    }
                    EnergyGeometry.polyline(pose, vc, pts, 0.12F, EnergyGeometry.argb(arm == 0 ? c : g, 0.55F * a));
                }
            }
            case SHADOW -> {
                for (int i = 0; i < 9; i++) {
                    float ang = i * Mth.TWO_PI / 9;
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k <= 8; k++) {
                        float f = k / 8.0F;
                        float sway = 0.25F * Mth.sin(time * 0.15F + f * 4 + i);
                        pts.add(feet.add(Mth.cos(ang) * (0.7 + sway) * s, f * h * 1.1, Mth.sin(ang) * (0.7 + sway) * s));
                    }
                    EnergyGeometry.polyline(pose, vc, pts, 0.1F, EnergyGeometry.argb(i % 2 == 0 ? g : c, 0.45F * a));
                }
                EnergyGeometry.ring(pose, vc, feet.add(0, 0.05, 0), UP, 1.1F * s, 0.3F, 36, EnergyGeometry.argb(c, 0.4F * a));
            }
            case HALO -> {
                Vec3 halo = head.add(fwd.scale(-0.3 * s)).add(0, 0.1, 0);
                EnergyGeometry.ring(pose, vc, halo, fwd, 0.55F * s, 0.1F, 32, EnergyGeometry.argb(g, 0.9F * a));
                for (int i = 0; i < 16; i++) {
                    float ang = time * 0.015F + i * Mth.TWO_PI / 16;
                    Vec3 dir = side.scale(Mth.cos(ang)).add(UP.scale(Mth.sin(ang)));
                    EnergyGeometry.ribbon(pose, vc, halo.add(dir.scale(0.6 * s)), halo.add(dir.scale((i % 2 == 0 ? 1.4 : 1.0) * s)),
                            0.1F, 0.0F, EnergyGeometry.argb(c, 0.7F * a), EnergyGeometry.argb(g, 0.0F));
                }
            }
            case GAMMA -> {
                long step = (long) (time / 3);
                for (int i = 0; i < 8; i++) {
                    float a0 = i * Mth.TWO_PI / 8;
                    float a1 = a0 + Mth.TWO_PI / 8;
                    EnergyGeometry.bolt(pose, vc, feet.add(Mth.cos(a0) * 1.3 * s, 0.08, Mth.sin(a0) * 1.3 * s),
                            feet.add(Mth.cos(a1) * 1.3 * s, 0.08, Mth.sin(a1) * 1.3 * s), 0.1F, 0.2F, EnergyGeometry.argb(c, 0.75F * a), seed + i + step);
                }
                for (int i = 0; i < 3; i++) {
                    EnergyGeometry.bolt(pose, vc, feet.add(0, 0.3 * s + i * 0.4 * s, 0), mid.add(side.scale((i - 1) * 0.3 * s)).add(0, 0.5 * s, 0),
                            0.05F, 0.25F, EnergyGeometry.argb(g, 0.8F * a), seed + 101 + i + step);
                }
            }
            case FERAL -> {
                long step = (long) (time / 6);
                RandomSource random = RandomSource.create(seed + step);
                float f = (time % 6) / 6.0F;
                for (int k = 0; k < 3; k++) {
                    float ang = random.nextFloat() * Mth.TWO_PI;
                    Vec3 center = mid.add(Mth.cos(ang) * 1.0 * s, (random.nextFloat() - 0.5) * s, Mth.sin(ang) * 1.0 * s);
                    Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                    Vec3 off = dir.cross(UP).normalize().scale(0.12);
                    for (int claw = -1; claw <= 1; claw++) {
                        Vec3 cs = center.add(off.scale(claw));
                        EnergyGeometry.ribbon(pose, vc, cs.add(dir.scale(-0.5)), cs.add(dir.scale(0.5)), 0.06F, 0.0F,
                                EnergyGeometry.argb(g, 0.9F * a * (1.0F - f)), EnergyGeometry.argb(c, 0.0F));
                    }
                }
            }
            case WINGS -> wings(pose, vc, feet.add(0, h * 0.72, 0), fwd, side, s, c, g, time, a, false);
        }
    }

    private static void crown(Matrix4f pose, VertexConsumer vc, Vec3 at, float r, int c, int g, float time, float a) {
        EnergyGeometry.ring(pose, vc, at, UP, r, 0.06F, 24, EnergyGeometry.argb(c, 0.85F * a));
        for (int i = 0; i < 7; i++) {
            float ang = i * Mth.TWO_PI / 7 + time * 0.01F;
            Vec3 p = at.add(Mth.cos(ang) * r, 0, Mth.sin(ang) * r);
            EnergyGeometry.ribbon(pose, vc, p, p.add(Mth.cos(ang) * 0.12, (i % 2 == 0 ? 0.45 : 0.28), Mth.sin(ang) * 0.12), 0.07F, 0.0F,
                    EnergyGeometry.argb(g, 0.9F * a), EnergyGeometry.argb(c, 0.0F));
        }
    }

    private static void wings(Matrix4f pose, VertexConsumer vc, Vec3 back, Vec3 fwd, Vec3 side, float s, int c, int g,
                              float time, float a, boolean fire) {
        Vec3 root = back.add(fwd.scale(-0.25 * s));
        float flap = 0.25F * Mth.sin(time * 0.12F);
        for (int w = -1; w <= 1; w += 2) {
            for (int i = 0; i < 7; i++) {
                float f = i / 6.0F;
                float len = (1.0F + 1.2F * (1.0F - Math.abs(f - 0.35F))) * s;
                Vec3 dir = side.scale(w * (0.6 + 0.5 * f)).add(UP.scale(0.9 - 1.2 * f + flap)).add(fwd.scale(-0.35));
                Vec3 tip = root.add(dir.normalize().scale(len));
                int col = EnergyGeometry.argb(fire ? (i % 2 == 0 ? c : g) : g, (fire ? 0.6F : 0.7F) * a);
                EnergyGeometry.ribbon(pose, vc, root, tip, 0.22F * s, 0.02F, col, EnergyGeometry.argb(c, 0.15F * a));
            }
        }
    }
}
