package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.asgard.AsgardFx;
import com.kolodkingal.enhanced.client.screen.BifrostScreen;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.heimdall.HeimdallAbilities;
import com.kolodkingal.enhanced.heimdall.HeimdallFx;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * What everyone near Heimdall sees him do (3.5), drawn from his lane's kinds of {@link AsgardFxPayload}
 * ({@link HeimdallFx}): the golden arc of each blow of Hofund, the streak of the lunge, the column of rainbow light of the
 * Bifrost Strike with its ring on the ground, the golden pillars of the Einherjar called, the flash of Hofund returning and
 * of a shadow struck; and, every frame, the Guardian's Ward's golden dome round whoever holds it up (from the synced
 * {@link Live#HEIMDALL_WARD}). Each kind also starts its gesture on the caster. The Bifrost screen's Send mode and the
 * Watch's shadows are handed on to {@link BifrostScreen} and {@link HeimdallSightView}.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HeimdallEffects {
    /** The seven colours of the Bifrost, from red to violet. */
    private static final int[] RAINBOW = {0xFF5A5A, 0xFFA54A, 0xFFF06A, 0x7CFF8A, 0x5FD2FF, 0x7F7CFF, 0xD98CFF};
    private static final int ARC_TICKS = 7;
    private static final int LUNGE_TICKS = 8;
    private static final int STRIKE_TICKS = 24;
    private static final int PILLAR_TICKS = 22;
    private static final int DOME_SEGMENTS = 28;
    private static final int DOME_RINGS = 8;

    private record Arc(Vec3 center, Vec3 look, int step, long start) {
    }

    private record Streak(Vec3 from, Vec3 to, long start) {
    }

    private record Column(Vec3 at, float radius, long start) {
    }

    private record Pillar(Vec3 at, long start) {
    }

    private static final List<Arc> ARCS = new ArrayList<>();
    private static final List<Streak> STREAKS = new ArrayList<>();
    private static final List<Column> COLUMNS = new ArrayList<>();
    private static final List<Pillar> PILLARS = new ArrayList<>();

    /** A colour of the Bifrost (0 red ... 6 violet). */
    public static int rainbow(int i) {
        return RAINBOW[Math.floorMod(i, RAINBOW.length)];
    }

    /** Registers the drawers of Heimdall's kinds (once, at client setup). */
    static void register() {
        for (int kind = HeimdallFx.SLASH; kind <= HeimdallFx.SHADOW_STRUCK; kind++) {
            AsgardFx.register(kind, HeimdallEffects::receive);
        }
    }

    static void receive(AsgardFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Entity who = p.entityId() < 0 ? null : level.getEntity(p.entityId());
        List<Vec3> pts = p.points();
        switch (p.kind()) {
            case HeimdallFx.SLASH -> {
                if (pts.size() >= 2) {
                    ARCS.add(new Arc(pts.get(0), pts.get(1), p.arg(), now));
                }
            }
            case HeimdallFx.LUNGE -> {
                if (pts.size() >= 2) {
                    STREAKS.add(new Streak(pts.get(0), pts.get(1), now));
                }
            }
            case HeimdallFx.STRIKE -> {
                if (!pts.isEmpty()) {
                    COLUMNS.add(new Column(pts.get(0), p.arg() / 10.0F, now));
                    burst(level, pts.get(0), 40, 0.35);
                }
                play(who, GestureId.HEIMDALL_STRIKE);
            }
            case HeimdallFx.SEND -> play(who, GestureId.HEIMDALL_SEND);
            case HeimdallFx.CALL -> {
                for (Vec3 at : pts) {
                    PILLARS.add(new Pillar(at, now));
                    burst(level, at.add(0, 1, 0), 12, 0.15);
                }
                play(who, GestureId.HEIMDALL_CALL);
            }
            case HeimdallFx.RETURN -> {
                if (who != null) {
                    burst(level, who.position().add(0, 1.2, 0), 18, 0.2);
                }
                play(who, GestureId.HEIMDALL_RETURN);
            }
            case HeimdallFx.OPEN_SEND -> BifrostScreen.openSend(p.entityId());
            case HeimdallFx.WATCH -> HeimdallSightView.watch(pts, p.arg());
            case HeimdallFx.SHADOW_STRUCK -> {
                for (Vec3 at : pts) {
                    RandomSource r = level.random;
                    for (int i = 0; i < 20; i++) {
                        level.addParticle(i % 2 == 0 ? ParticleTypes.LARGE_SMOKE : new EnergyParticleOptions(0x3FBF6F, 0.6F),
                                at.x + r.nextGaussian() * 0.2, at.y + 0.3 + r.nextGaussian() * 0.2, at.z + r.nextGaussian() * 0.2,
                                r.nextGaussian() * 0.04, 0.03 + r.nextDouble() * 0.04, r.nextGaussian() * 0.04);
                    }
                    burst(level, at.add(0, 0.3, 0), 10, 0.12);
                }
            }
            default -> {
            }
        }
    }

    private static void play(Entity who, GestureId id) {
        if (who instanceof Player player) {
            HeroAnimator.play(player, id, 0.0F);
        }
    }

    /** Golden and rainbow motes thrown out from a point. */
    private static void burst(ClientLevel level, Vec3 at, int count, double speed) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            int c = i % 3 == 0 ? HeimdallAbilities.LIGHT : i % 3 == 1 ? HeimdallAbilities.GOLD : rainbow(i);
            level.addParticle(new EnergyParticleOptions(c, 0.7F), at.x, at.y, at.z,
                    r.nextGaussian() * speed, r.nextDouble() * speed, r.nextGaussian() * speed);
        }
    }

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        ARCS.removeIf(a -> now - a.start() > ARC_TICKS);
        STREAKS.removeIf(s -> now - s.start() > LUNGE_TICKS);
        COLUMNS.removeIf(c -> now - c.start() > STRIKE_TICKS);
        PILLARS.removeIf(p -> now - p.start() > PILLAR_TICKS);
        // The light still falling from the column, and the Ward's sparks where it stands.
        for (Column c : COLUMNS) {
            if (now - c.start() < 10) {
                RandomSource r = level.random;
                double a = r.nextDouble() * Math.PI * 2;
                double d = r.nextDouble() * c.radius();
                level.addParticle(new EnergyParticleOptions(rainbow(r.nextInt(7)), 0.6F), c.at().x + Math.cos(a) * d, c.at().y + 0.1,
                        c.at().z + Math.sin(a) * d, 0, 0.12 + r.nextDouble() * 0.1, 0);
            }
        }
        for (AbstractClientPlayer p : level.players()) {
            if (warded(p) && level.random.nextInt(3) == 0) {
                RandomSource r = level.random;
                double a = r.nextDouble() * Math.PI * 2;
                double up = r.nextDouble() * Math.PI * 0.5;
                double rad = HeimdallAbilities.WARD_RADIUS;
                level.addParticle(new EnergyParticleOptions(HeimdallAbilities.LIGHT, 0.4F), p.getX() + Math.cos(a) * Math.cos(up) * rad,
                        p.getY() + Math.sin(up) * rad, p.getZ() + Math.sin(a) * Math.cos(up) * rad, 0, 0.01, 0);
            }
        }
    }

    static void clear() {
        ARCS.clear();
        STREAKS.clear();
        COLUMNS.clear();
        PILLARS.clear();
    }

    private static boolean warded(Player p) {
        PowerData data = Powers.get(p);
        return data.is(Live.HEIMDALL_WARD) && data.has(HeroAbility.HEIMDALL_WARD);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        List<AbstractClientPlayer> warded = new ArrayList<>();
        for (AbstractClientPlayer p : level.players()) {
            if (warded(p)) {
                warded.add(p);
            }
        }
        if (ARCS.isEmpty() && STREAKS.isEmpty() && COLUMNS.isEmpty() && PILLARS.isEmpty() && warded.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Arc a : ARCS) {
            arc(pose, vc, a, camera, time);
        }
        for (Streak s : STREAKS) {
            streak(pose, vc, s, camera, time);
        }
        for (Column c : COLUMNS) {
            column(pose, vc, c, camera, time);
        }
        for (Pillar p : PILLARS) {
            pillar(pose, vc, p, camera, time);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        if (!warded.isEmpty()) {
            VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
            for (AbstractClientPlayer p : warded) {
                dome(pose, veil, p.getPosition(partial).subtract(camera), time);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        }
    }

    /** A blow: a crescent of gold sweeping across the arc before him, its edge white-hot, fading as it goes. */
    private static void arc(Matrix4f pose, VertexConsumer vc, Arc a, Vec3 camera, float time) {
        float age = time - a.start();
        float u = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        float fade = 1.0F - Mth.clamp((age - 3.0F) / 4.0F, 0.0F, 1.0F);
        if (fade <= 0.0F) {
            return;
        }
        double yaw = Math.atan2(a.look().z, a.look().x);
        double half = Math.toRadians(HeimdallAbilities.SLASH_ANGLE * 0.5);
        // The first blow sweeps right to left and down, the second left to right, the third falls from overhead.
        boolean down = a.step() == 3;
        double dir = a.step() == 2 ? 1.0 : -1.0;
        int segments = 16;
        double r = HeimdallAbilities.SLASH_RADIUS * 0.85;
        Vec3 o = a.center().subtract(camera);
        Vec3 prevIn = null;
        Vec3 prevOut = null;
        for (int i = 0; i <= segments; i++) {
            double f = (double) i / segments;
            if (f > u) {
                break;
            }
            Vec3 in;
            Vec3 out;
            if (down) {
                // A vertical crescent along the look, from above his head down to the ground before him.
                double pitch = Math.PI * 0.55 - f * Math.PI * 0.75;
                Vec3 fwd = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                in = o.add(fwd.scale(Math.cos(pitch) * r * 0.55)).add(0, Math.sin(pitch) * r * 0.55, 0);
                out = o.add(fwd.scale(Math.cos(pitch) * r)).add(0, Math.sin(pitch) * r, 0);
            } else {
                double t = yaw - dir * half + dir * 2 * half * f;
                double drop = 0.6 - f * 0.9;
                in = o.add(Math.cos(t) * r * 0.55, drop * 0.6, Math.sin(t) * r * 0.55);
                out = o.add(Math.cos(t) * r, drop, Math.sin(t) * r);
            }
            if (prevIn != null) {
                float lead = 1.0F - (float) Math.abs(u - f) * 2.0F;
                int edge = EnergyGeometry.argb(0xFFFFFF, 0.55F * fade * Math.max(0.2F, lead));
                int body = EnergyGeometry.argb(HeimdallAbilities.GOLD, 0.0F);
                quad(pose, vc, prevOut, out, in, prevIn, edge, edge, body, body);
            }
            prevIn = in;
            prevOut = out;
        }
    }

    /** The lunge: a golden streak along the dash. */
    private static void streak(Matrix4f pose, VertexConsumer vc, Streak s, Vec3 camera, float time) {
        float age = time - s.start();
        float fade = 1.0F - Mth.clamp(age / LUNGE_TICKS, 0.0F, 1.0F);
        Vec3 a = s.from().add(0, 1.0, 0).subtract(camera);
        Vec3 b = s.to().add(0, 1.0, 0).subtract(camera);
        EnergyGeometry.ribbon(pose, vc, a, b, 0.05F, 0.35F, EnergyGeometry.argb(HeimdallAbilities.GOLD, 0.0F),
                EnergyGeometry.argb(HeimdallAbilities.LIGHT, 0.8F * fade));
    }

    /** The Bifrost Strike: seven bands of colour falling from the sky into a ring on the ground. */
    private static void column(Matrix4f pose, VertexConsumer vc, Column c, Vec3 camera, float time) {
        float age = time - c.start();
        float grow = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float fade = 1.0F - Mth.clamp((age - 8.0F) / (STRIKE_TICKS - 8.0F), 0.0F, 1.0F);
        if (fade <= 0.0F) {
            return;
        }
        Vec3 ground = c.at().subtract(camera);
        double height = 60.0;
        Vec3 top = ground.add(0, height * grow, 0);
        float width = 1.4F + 0.4F * Mth.sin(time * 0.9F);
        for (int i = 0; i < RAINBOW.length; i++) {
            double a = time * 0.25 + i * Math.PI * 2 / RAINBOW.length;
            Vec3 off = new Vec3(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35);
            EnergyGeometry.ribbon(pose, vc, top.add(off), ground.add(off), width * 0.5F, width, EnergyGeometry.argb(RAINBOW[i], 0.0F),
                    EnergyGeometry.argb(RAINBOW[i], 0.55F * fade));
        }
        EnergyGeometry.ribbon(pose, vc, top, ground, 0.3F, 0.7F, EnergyGeometry.argb(0xFFFFFF, 0.2F * fade),
                EnergyGeometry.argb(0xFFFFFF, 0.9F * fade));
        // The ring where it lands, spreading to the strike's radius.
        float r = c.radius() * Mth.clamp(age / 5.0F, 0.1F, 1.0F);
        int segs = 32;
        for (int k = 0; k < segs; k++) {
            double t0 = k * Math.PI * 2 / segs;
            double t1 = (k + 1) * Math.PI * 2 / segs;
            int col = EnergyGeometry.argb(RAINBOW[k % RAINBOW.length], 0.7F * fade);
            Vec3 a0 = ground.add(Math.cos(t0) * r, 0.05, Math.sin(t0) * r);
            Vec3 a1 = ground.add(Math.cos(t1) * r, 0.05, Math.sin(t1) * r);
            Vec3 b0 = ground.add(Math.cos(t0) * (r - 0.35), 0.05, Math.sin(t0) * (r - 0.35));
            Vec3 b1 = ground.add(Math.cos(t1) * (r - 0.35), 0.05, Math.sin(t1) * (r - 0.35));
            quad(pose, vc, a0, a1, b1, b0, col, col, col, col);
        }
    }

    /** An Einherjar's coming: a pillar of gold that rises and fades. */
    private static void pillar(Matrix4f pose, VertexConsumer vc, Pillar p, Vec3 camera, float time) {
        float age = time - p.start();
        float fade = 1.0F - Mth.clamp((age - 6.0F) / (PILLAR_TICKS - 6.0F), 0.0F, 1.0F);
        Vec3 a = p.at().subtract(camera);
        Vec3 b = a.add(0, 2.6 + age * 0.15, 0);
        EnergyGeometry.ribbon(pose, vc, a, b, 1.1F, 0.2F, EnergyGeometry.argb(HeimdallAbilities.LIGHT, 0.8F * fade),
                EnergyGeometry.argb(HeimdallAbilities.GOLD, 0.0F));
    }

    /** The Guardian's Ward: a dome of gold round him, its ribs shimmering. */
    private static void dome(Matrix4f pose, VertexConsumer vc, Vec3 base, float time) {
        double r = HeimdallAbilities.WARD_RADIUS;
        for (int ring = 0; ring < DOME_RINGS; ring++) {
            double p0 = Math.PI * 0.5 * ring / DOME_RINGS;
            double p1 = Math.PI * 0.5 * (ring + 1) / DOME_RINGS;
            for (int s = 0; s < DOME_SEGMENTS; s++) {
                double t0 = Math.PI * 2 * s / DOME_SEGMENTS;
                double t1 = Math.PI * 2 * (s + 1) / DOME_SEGMENTS;
                float shimmer = 0.5F + 0.5F * Mth.sin(time * 0.15F + s * 0.9F + ring * 1.3F);
                boolean rib = s % 4 == 0;
                int c = EnergyGeometry.argb(rib ? HeimdallAbilities.LIGHT : HeimdallAbilities.GOLD, (rib ? 0.22F : 0.08F) + 0.06F * shimmer);
                Vec3 a = base.add(Math.cos(t0) * Math.cos(p0) * r, Math.sin(p0) * r, Math.sin(t0) * Math.cos(p0) * r);
                Vec3 b = base.add(Math.cos(t1) * Math.cos(p0) * r, Math.sin(p0) * r, Math.sin(t1) * Math.cos(p0) * r);
                Vec3 cc = base.add(Math.cos(t1) * Math.cos(p1) * r, Math.sin(p1) * r, Math.sin(t1) * Math.cos(p1) * r);
                Vec3 d = base.add(Math.cos(t0) * Math.cos(p1) * r, Math.sin(p1) * r, Math.sin(t0) * Math.cos(p1) * r);
                quad(pose, vc, a, b, cc, d, c, c, c, c);
            }
        }
    }

    private static void quad(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(ca);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(cb);
        vc.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(cc);
        vc.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(cd);
    }

    private HeimdallEffects() {
    }
}
