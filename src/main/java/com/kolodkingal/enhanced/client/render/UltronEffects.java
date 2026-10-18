package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every effect of the Ultron fight that the server sends one payload for ({@link UltronFxPayload}): the tell decals laid
 * on the ground (the Dive's ring, the Debris, the Call's pillars, the Pull's contracting ring), the aim lines and the
 * ten ribbons of the Finger Beams, the Volley's glint, the rings of the shockwaves, the data lines of a transfer or an
 * upload, the scrap that assembles him at the ritual, the wreck of a body, the flash and the pulse, the warning ring of
 * the lift, the shake of the Tremor, and the hero's own: the Gravity Slam and Pull, the Overcharge, the network, the
 * drones that assemble, the Hack and the Command.
 *
 * <p>The rule of the fight is that every blow has a tell, and here is where most tells are drawn: nothing that can hurt
 * is ever drawn <em>after</em> it lands and not before. The decals are red-orange for danger and pale blue for the Pull and
 * for the Sentries' light; the beams are the plasma amber with a red edge. Geometry is additive and drawn after the
 * particles; the particles of each effect are {@link UltronParticles}'. {@link #receive} is what the payload calls.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class UltronEffects {
    // The plasma of Prime, the danger red, the anti-gravity blue, the network.
    static final int PLASMA_CORE = 0xFFF3D6;
    static final int PLASMA_GOLD = 0xFFD27A;
    static final int PLASMA_MAIN = 0xFFA23A;
    static final int PLASMA_EDGE = 0xE5322B;
    static final int PLASMA_HALO = 0xFF7A1A;
    static final int DANGER = 0xFF4A2B;
    static final int DANGER_GLOW = 0xFF8A4C;
    static final int GRAV_CORE = 0xEAF8FF;
    static final int GRAV_MAIN = 0x9FD8FF;
    static final int GRAV_FRINGE = 0x5FA8E0;
    static final int NET_WHITE = 0xF4F4F4;
    static final int NET_RED = 0xFF3B30;
    static final int NET_DARK = 0x7A1410;

    /** One effect that is running: what it is, where, and when it began. */
    static final class Effect {
        final int kind;
        final int entityId;
        final Vec3 at;
        final Vec3 to;
        final float radius;
        final int ticks;
        final long start;
        final ResourceKey<Level> dimension;
        /** Whether its one-time burst of particles has gone off. */
        boolean burst;

        Effect(int kind, int entityId, Vec3 at, Vec3 to, float radius, int ticks, long start, ResourceKey<Level> dimension) {
            this.kind = kind;
            this.entityId = entityId;
            this.at = at;
            this.to = to;
            this.radius = radius;
            this.ticks = ticks;
            this.start = start;
            this.dimension = dimension;
        }

        /** How many ticks it has run (with the partial tick). */
        float age(long now, float partial) {
            return (now - start) + partial;
        }

        /** How far through its life it is, 0 to 1. */
        float progress(long now, float partial) {
            return Mth.clamp(age(now, partial) / Math.max(1, ticks), 0.0F, 1.0F);
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    /** When each caster's Finger Beams aim began (the aim is resent every five ticks): the lines grow brighter as it holds. */
    private static final Map<Integer, Long> AIM_SINCE = new HashMap<>();

    /** The payload arrived (on the main thread). */
    public static void receive(UltronFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (payload.kind() == UltronFxPayload.BEAMS_AIM) {
            EFFECTS.removeIf(e -> e.kind == UltronFxPayload.BEAMS_AIM && e.entityId == payload.entityId());
            AIM_SINCE.putIfAbsent(payload.entityId(), now);
        }
        if (payload.kind() == UltronFxPayload.BEAMS_FIRE) {
            AIM_SINCE.remove(payload.entityId());
        }
        EFFECTS.add(new Effect(payload.kind(), payload.entityId(), payload.at(), payload.to(), payload.radius(), payload.ticks(), now, level.dimension()));
        // (The caster of the hero's Pull, Slam and Command acts it.)
        UltronActing.onFx(payload);
    }

    /** Forgets every effect (the client left the world or changed dimension). */
    public static void clear() {
        EFFECTS.clear();
        AIM_SINCE.clear();
    }

    /** How long an effect is kept: its ticks, and a moment more for those that are resent. */
    private static int life(Effect e) {
        return switch (e.kind) {
            case UltronFxPayload.BEAMS_AIM -> e.ticks + 2;
            default -> e.ticks;
        };
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick: the particles of each effect

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> e.dimension != level.dimension() || now - e.start > life(e) || now < e.start);
        AIM_SINCE.values().removeIf(since -> now - since > 400);
        if (mc.isPaused()) {
            return;
        }
        for (Effect e : EFFECTS) {
            tint = tintOf(level, e);
            UltronParticles.tick(level, e, now);
        }
        tint = -1;
        UltronParticles.tickBodies(level, now);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every frame: the geometry

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || EFFECTS.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer light = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        List<Effect> scrap = null;
        for (Effect e : EFFECTS) {
            if (e.dimension != level.dimension() || e.at.distanceToSqr(camera) > 160.0 * 160.0 && e.kind != UltronFxPayload.LIFT_RING) {
                continue;
            }
            draw(level, e, now, partial, camera, pose, light);
            tint = -1;
            if (e.kind == UltronFxPayload.WRECK) {
                (scrap == null ? scrap = new ArrayList<>() : scrap).add(e);
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        if (scrap != null) {
            VertexConsumer solid = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
            for (Effect e : scrap) {
                wreckPile(level, e, now, partial, camera, pose, solid);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        }
    }

    /** 3.5: the colour a custom hero's pick chose for the effect being drawn, or -1 (Ultron's own). */
    static int tint = -1;

    /** One of Ultron's colours, or it recoloured to the colour chosen for the effect being drawn. */
    static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /**
     * The colour chosen for an effect of a custom hero's ability (the caster's pick of it), or -1: Ultron's own
     * effects.
     */
    static int tintOf(ClientLevel level, Effect e) {
        HeroAbility row = switch (e.kind) {
            case UltronFxPayload.GRAVITY_PULL -> HeroAbility.ULTRON_GRAVITY;
            case UltronFxPayload.GRAVITY_SLAM -> HeroAbility.ULTRON_SLAM;
            case UltronFxPayload.OVERCHARGE_FLASH -> HeroAbility.ULTRON_OVERCHARGE;
            case UltronFxPayload.HACK_BEAM -> HeroAbility.ULTRON_HACK;
            case UltronFxPayload.COMMAND_MARK, UltronFxPayload.DRONE_ASSEMBLE -> HeroAbility.ULTRON_ARMY;
            case UltronFxPayload.NETWORK_FLASH, UltronFxPayload.DATA_LINE -> HeroAbility.ULTRON_UPLOAD;
            default -> null;
        };
        if (row == null || e.entityId < 0) {
            return -1;
        }
        Entity caster = level.getEntity(e.entityId);
        if (caster instanceof OwnableEntity drone && !(caster instanceof Player)) {
            caster = drone.getOwner();
        }
        return AbilityColors.chosen(caster, row == HeroAbility.ULTRON_ARMY && e.kind == UltronFxPayload.COMMAND_MARK
                ? HeroAbility.ULTRON_COMMAND : row);
    }

    private static void draw(ClientLevel level, Effect e, long now, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        tint = tintOf(level, e);
        float age = e.age(now, partial);
        float u = e.progress(now, partial);
        float time = now + partial;
        switch (e.kind) {
            case UltronFxPayload.ASSEMBLE -> assemble(level, e, u, age, camera, pose, vc);
            case UltronFxPayload.FLASH -> {
                float grow = Mth.clamp(age / Math.max(2.0F, e.ticks * 0.4F), 0.0F, 1.0F);
                float fade = 1.0F - u;
                UltronGeometry.flare(pose, vc, e.at.subtract(camera), e.radius * (0.15F + 0.85F * grow), fade, c(PLASMA_HALO), c(PLASMA_GOLD), 0xFFFFFF);
            }
            case UltronFxPayload.GLINT -> glint(level, e, u, partial, camera, pose, vc);
            case UltronFxPayload.BEAMS_AIM -> beamsAim(level, e, now, time, partial, camera, pose, vc);
            case UltronFxPayload.BEAMS_FIRE -> beamsFire(level, e, age, u, time, partial, camera, pose, vc);
            case UltronFxPayload.DIVE_RING -> diveRing(level, e, age, u, time, camera, pose, vc);
            case UltronFxPayload.DEBRIS_RING -> debrisRing(level, e, age, u, time, camera, pose, vc);
            case UltronFxPayload.PULL_RING -> pullRing(level, e, u, time, camera, pose, vc);
            case UltronFxPayload.SPAWN_PILLAR -> spawnPillar(level, e, u, time, camera, pose, vc);
            case UltronFxPayload.SHOCKWAVE -> shockwave(level, e, u, c(GRAV_MAIN), 0xFFE0B0, camera, pose, vc);
            case UltronFxPayload.DATA_LINE -> dataLine(e.at, e.to, e.ticks, age, time, camera, pose, vc, 1.0F);
            case UltronFxPayload.WRECK -> {
                // The heat of the wreck, a red glow on the ground that cools.
                UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
                float cool = 1.0F - u;
                UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, 1.6F, 16, EnergyGeometry.argb(c(NET_RED), 0.10F * cool),
                        EnergyGeometry.argb(c(NET_DARK), 0.0F));
            }
            case UltronFxPayload.LIFT_RING -> liftRing(level, e, age, u, time, camera, pose, vc);
            case UltronFxPayload.DEATH -> death(level, e, u, partial, camera, pose, vc);
            case UltronFxPayload.PULSE -> pulse(level, e, u, partial, camera, pose, vc);
            case UltronFxPayload.GRAVITY_SLAM -> gravitySlam(level, e, u, camera, pose, vc);
            case UltronFxPayload.GRAVITY_PULL -> gravityPull(e, age, u, time, camera, pose, vc);
            case UltronFxPayload.OVERCHARGE_FLASH -> overchargeFlash(e, u, camera, pose, vc);
            case UltronFxPayload.NETWORK_FLASH -> networkFlash(e, age, u, camera, pose, vc);
            case UltronFxPayload.DRONE_ASSEMBLE -> droneAssemble(level, e, u, camera, pose, vc);
            case UltronFxPayload.HACK_BEAM -> hackBeam(e, age, u, time, camera, pose, vc);
            case UltronFxPayload.COMMAND_MARK -> commandMark(e, age, u, camera, pose, vc);
            default -> {
                // TREMOR is the camera's; the rest do nothing.
            }
        }
    }

    // ---- the ritual and the flashes ----

    /** The standing silhouette that scrap assembles into: built from the feet up as the ritual goes on. */
    private static void assemble(ClientLevel level, Effect e, float u, float age, Vec3 camera, Matrix4f pose,
            VertexConsumer vc) {
        float built = Mth.clamp(u * 1.15F, 0.0F, 1.0F);
        // (3.3) Timed with the partial tick (it was the game time alone: a jump every tick) and slower.
        float flicker = 0.7F + 0.3F * Mth.sin(age * 0.6F);
        UltronGeometry.silhouette(pose, vc, e.at.subtract(camera), e.radius, 0.0F, built, EnergyGeometry.argb(c(GRAV_MAIN), 0.22F * flicker * (0.4F + 0.6F * u)));
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, 1.7F - 0.5F * u, 0.14F, 40, EnergyGeometry.argb(c(GRAV_MAIN), 0.5F * u + 0.1F));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, 2.6F * (1.0F - u) + 0.4F, 0.1F, 40, EnergyGeometry.argb(c(NET_WHITE), 0.3F * u));
    }

    // ---- the Volley and the Finger Beams ----

    /** The Volley's tell: a thin amber glint from each palm to what he is aimed at, brightening to the first burst. */
    private static void glint(ClientLevel level, Effect e, float u, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Entity boss = level.getEntity(e.entityId);
        Vec3 chest = e.at;
        Vec3 target = e.to;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 palm = boss != null ? UltronTracker.palm(boss, side < 0, partial) : null;
            if (palm == null) {
                Vec3 forward = target.subtract(chest).normalize();
                Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
                palm = chest.add(right.scale(side * 0.7)).add(forward.scale(0.5));
            }
            Vec3 a = palm.subtract(camera);
            Vec3 b = target.subtract(camera);
            float alpha = 0.35F + 0.65F * u;
            EnergyGeometry.ribbon(pose, vc, a, b, 0.09F, 0.09F, EnergyGeometry.argb(c(PLASMA_HALO), 0.18F * alpha), EnergyGeometry.argb(c(PLASMA_HALO), 0.1F * alpha));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.03F, 0.03F, EnergyGeometry.argb(c(PLASMA_GOLD), 0.9F * alpha), EnergyGeometry.argb(c(PLASMA_GOLD), 0.5F * alpha));
            UltronGeometry.flare(pose, vc, a, 0.22F * alpha, 0.9F, c(PLASMA_HALO), c(PLASMA_GOLD), 0xFFFFFF);
        }
    }

    /** The ten fingertips of the body, from the tracker or (out of sight of a frame) from the server's own estimate. */
    static Vec3[] fingertips(Entity caster, Vec3 toward, float partial) {
        Vec3[] tracked = UltronTracker.fingertips(caster, partial);
        if (tracked != null) {
            return tracked;
        }
        double scale = caster instanceof UltronEntity boss ? boss.body().hitboxScale() : 1.0;
        Vec3 chest = caster.getPosition(partial).add(0, caster.getBbHeight() * 0.62, 0);
        Vec3 forward = toward.subtract(chest);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : forward.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3[] tips = new Vec3[UltronTracker.FINGERS];
        for (int hand = 0; hand < 2; hand++) {
            int side = hand == 0 ? -1 : 1;
            Vec3 palm = chest.add(right.scale(side * 0.7 * scale)).add(forward.scale(0.5 * scale));
            for (int finger = 0; finger < 5; finger++) {
                tips[hand * 5 + finger] = palm.add(forward.scale(0.45 * scale)).add(right.scale((finger - 2) * 0.06 * scale));
            }
        }
        return tips;
    }

    /**
     * The Beams' tell: ten dotted red-orange aim lines run from the fingertips and converge on a point at the target's feet,
     * with a mark on the ground there, brighter the longer the aim holds. At the end of the tell the point locks and the
     * lines give way to the beams themselves.
     */
    private static void beamsAim(ClientLevel level, Effect e, long now, float time, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Entity boss = level.getEntity(e.entityId);
        if (boss == null) {
            return;
        }
        long since = AIM_SINCE.getOrDefault(e.entityId, e.start);
        float held = Mth.clamp((now - since + partial) / UltronAttack.BEAMS.tell(), 0.0F, 1.0F);
        float strength = 0.35F + 0.65F * held;
        Vec3[] tips = fingertips(boss, e.at, partial);
        Vec3 point = e.at.add(0, 0.1, 0);
        for (int k = 0; k < tips.length; k++) {
            Vec3 a = tips[k].subtract(camera);
            Vec3 b = point.subtract(camera);
            UltronGeometry.dotted(pose, vc, a, b, 0.05F, 0.35F, time * 0.35F + k * 0.13F, EnergyGeometry.argb(c(DANGER_GLOW), 0.9F * strength));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.02F, 0.02F, EnergyGeometry.argb(c(DANGER), 0.18F * strength), EnergyGeometry.argb(c(DANGER), 0.35F * strength));
        }
        // The mark where they meet: rings that tighten as the lock comes, a cross, a faint disc.
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float pulse = 1.0F + 0.08F * Mth.sin(time * 0.9F);
        float r = (1.5F - 0.6F * held) * pulse;
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r, 0.14F, 36, EnergyGeometry.argb(c(DANGER), 0.85F * strength));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r * 0.45F, 0.1F, 24, EnergyGeometry.argb(c(DANGER_GLOW), 0.8F * strength));
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2.0 + Math.PI / 4.0;
            UltronGeometry.line(pose, vc, camera, ground, e.at.x + Math.cos(angle) * r * 0.6, e.at.z + Math.sin(angle) * r * 0.6,
                    e.at.x + Math.cos(angle) * (r + 0.5), e.at.z + Math.sin(angle) * (r + 0.5), 0.1F, EnergyGeometry.argb(c(DANGER), 0.9F * strength),
                    EnergyGeometry.argb(c(DANGER), 0.5F * strength));
        }
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, r, 28, EnergyGeometry.argb(c(DANGER), 0.14F * strength), EnergyGeometry.argb(c(DANGER), 0.04F * strength));
    }

    /** The beams: ten ribbons fan ten degrees either side of the locked line from the fingertips, each ending on the first block. */
    private static void beamsFire(ClientLevel level, Effect e, float age, float u, float time, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Entity boss = level.getEntity(e.entityId);
        if (boss == null) {
            return;
        }
        float alpha = Mth.clamp(age / 2.0F, 0.0F, 1.0F) * Mth.clamp((e.ticks - age) / 4.0F, 0.0F, 1.0F);
        if (alpha <= 0.0F) {
            return;
        }
        Vec3 lock = e.at;
        Vec3[] tips = fingertips(boss, lock, partial);
        // The line of the beams runs to where he locked, at the height of his chest, as the server's rays do: from the
        // fingertip toward the locked point (the target's feet).
        for (int k = 0; k < tips.length; k++) {
            Vec3 origin = tips[k];
            double fan = tips.length == 1 ? 0.0 : -UltronAttack.BEAM_FAN_DEGREES + 2.0 * UltronAttack.BEAM_FAN_DEGREES * k / (tips.length - 1);
            Vec3 direction = rotateY(lock.subtract(origin).normalize(), Math.toRadians(fan));
            Vec3 end = origin.add(direction.scale(UltronAttack.BEAM_REACH));
            HitResult block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            if (block.getType() != HitResult.Type.MISS) {
                end = block.getLocation();
            }
            float flicker = 0.85F + 0.15F * Mth.sin(time * 3.1F + k * 1.7F);
            Vec3 a = origin.subtract(camera);
            Vec3 b = end.subtract(camera);
            float taper = 0.0F;
            EnergyGeometry.ribbon(pose, vc, a, b, 0.30F, 0.34F, EnergyGeometry.argb(c(PLASMA_HALO), 0.22F * alpha * flicker), EnergyGeometry.argb(c(PLASMA_HALO), 0.30F * alpha * flicker));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.11F, 0.13F, EnergyGeometry.argb(c(PLASMA_MAIN), 0.8F * alpha * flicker), EnergyGeometry.argb(c(PLASMA_EDGE), 0.7F * alpha * flicker));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.04F, 0.05F, EnergyGeometry.argb(c(PLASMA_CORE), 0.95F * alpha), EnergyGeometry.argb(c(PLASMA_CORE), 0.8F * alpha));
            UltronGeometry.flare(pose, vc, b, 0.34F * flicker, alpha, c(PLASMA_HALO), c(PLASMA_GOLD), c(PLASMA_CORE));
        }
    }

    /** A vector turned about the vertical axis (as the server turns each ray of the fan). */
    static Vec3 rotateY(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ---- the tells laid on the ground ----

    /**
     * The Dive's ring: the area of the landing is a faint red disc with a bright rim, and a second ring shrinks toward the
     * middle over the ticks of the tell, so the time to leave is the size of the ring. It does not follow anyone.
     */
    private static void diveRing(ClientLevel level, Effect e, float age, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float area = e.radius + 0.5F;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.8F);
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, area, 40, EnergyGeometry.argb(c(DANGER), 0.10F + 0.06F * pulse),
                EnergyGeometry.argb(c(DANGER), 0.22F + 0.08F * pulse));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, area, 0.28F, 48, EnergyGeometry.argb(c(DANGER), 0.7F));
        // The timer: from the rim to the middle.
        float shrink = Mth.lerp(AnimMath.ss(0.0F, 1.0F, u), e.radius, 0.4F);
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, shrink, 0.3F, 48, EnergyGeometry.argb(c(DANGER_GLOW), 0.95F));
        // Eight marks pointing in: the eye reads "leave".
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0 + time * 0.05;
            UltronGeometry.line(pose, vc, camera, ground, e.at.x + Math.cos(angle) * (area + 0.6), e.at.z + Math.sin(angle) * (area + 0.6),
                    e.at.x + Math.cos(angle) * (area - 0.2), e.at.z + Math.sin(angle) * (area - 0.2), 0.16F, EnergyGeometry.argb(c(DANGER_GLOW), 0.0F),
                    EnergyGeometry.argb(c(DANGER_GLOW), 0.85F));
        }
    }

    /** A piece of Sokovia is going to fall here: a red circle that fills as the time runs out. */
    private static void debrisRing(ClientLevel level, Effect e, float age, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float pulse = 0.5F + 0.5F * Mth.sin(time * (0.5F + 1.2F * u));
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, e.radius, 32, EnergyGeometry.argb(c(DANGER), 0.06F + 0.1F * u + 0.04F * pulse),
                EnergyGeometry.argb(c(DANGER), 0.14F + 0.14F * u));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, e.radius, 0.24F, 40, EnergyGeometry.argb(c(DANGER), 0.55F + 0.35F * pulse));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, Math.max(0.3F, e.radius * u), 0.2F, 32, EnergyGeometry.argb(c(DANGER_GLOW), 0.85F));
    }

    /** The Pull's ring of anti-gravity contracts from the edge of his reach to where he stands. */
    private static void pullRing(ClientLevel level, Effect e, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float eased = u * u * (3.0F - 2.0F * u);
        float r = Mth.lerp(eased, e.radius, 3.0F);
        float alpha = 0.35F + 0.5F * u;
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r, 0.35F, 64, EnergyGeometry.argb(c(GRAV_MAIN), alpha));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r + 1.2F, 0.18F, 64, EnergyGeometry.argb(c(GRAV_FRINGE), alpha * 0.55F));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r + 2.6F, 0.1F, 64, EnergyGeometry.argb(c(GRAV_FRINGE), alpha * 0.3F));
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, 3.0F, 28, EnergyGeometry.argb(c(GRAV_MAIN), 0.06F + 0.08F * u), EnergyGeometry.argb(c(GRAV_MAIN), 0.12F + 0.1F * u));
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0 + time * 0.06;
            UltronGeometry.line(pose, vc, camera, ground, e.at.x + Math.cos(angle) * (r + 1.0), e.at.z + Math.sin(angle) * (r + 1.0),
                    e.at.x + Math.cos(angle) * (r - 0.4), e.at.z + Math.sin(angle) * (r - 0.4), 0.12F, EnergyGeometry.argb(c(GRAV_CORE), 0.0F),
                    EnergyGeometry.argb(c(GRAV_CORE), 0.7F * alpha));
        }
    }

    /** Where a Sentry will come out of the ground: cracks run out, a ring pulses, a pillar of pale blue light builds. */
    private static void spawnPillar(ClientLevel level, Effect e, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float grow = AnimMath.ss(0.0F, 0.5F, u);
        long seed = e.start * 31L + (long) (e.at.x * 7) + (long) (e.at.z * 13);
        for (int i = 0; i < 6; i++) {
            UltronGeometry.crack(pose, vc, camera, ground, e.at.x, e.at.z, i * Math.PI / 3.0 + (seed % 7) * 0.1, 1.5F * grow, 0.1F,
                    EnergyGeometry.argb(c(GRAV_CORE), 0.9F), seed + i);
        }
        float pulse = 0.5F + 0.5F * Mth.sin(time * 1.1F);
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, 0.8F + 0.25F * pulse, 0.14F, 24, EnergyGeometry.argb(c(GRAV_MAIN), 0.4F + 0.5F * u));
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, 0.9F, 20, EnergyGeometry.argb(c(GRAV_MAIN), 0.2F * u), EnergyGeometry.argb(c(GRAV_MAIN), 0.04F));
        float rise = AnimMath.ss(0.2F, 1.0F, u);
        float height = 6.0F * rise;
        UltronGeometry.pillar(pose, vc, camera, e.at.x, e.at.y, e.at.z, 0.4F, height, 14, EnergyGeometry.argb(c(GRAV_FRINGE), 0.35F + 0.4F * u),
                EnergyGeometry.argb(c(GRAV_FRINGE), 0.0F));
        UltronGeometry.pillar(pose, vc, camera, e.at.x, e.at.y, e.at.z, 0.16F, height, 10, EnergyGeometry.argb(c(GRAV_CORE), 0.45F + 0.5F * u),
                EnergyGeometry.argb(c(GRAV_CORE), 0.0F));
        if (u > 0.7F) {
            UltronGeometry.flare(pose, vc, e.at.add(0, 0.4, 0).subtract(camera), 0.9F * (u - 0.7F) / 0.3F, 0.8F, c(GRAV_FRINGE), c(GRAV_MAIN), c(GRAV_CORE));
        }
    }

    /** A ring runs out from where the ground was struck. */
    private static void shockwave(ClientLevel level, Effect e, float u, int main, int hot, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float eased = 1.0F - (1.0F - u) * (1.0F - u);
        float r = Math.max(0.3F, e.radius * eased);
        float fade = 1.0F - u;
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r, 0.6F * fade + 0.2F, 56, EnergyGeometry.argb(hot, 0.75F * fade));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r * 0.8F, 0.4F * fade + 0.1F, 56, EnergyGeometry.argb(main, 0.5F * fade));
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, r, 36, EnergyGeometry.argb(main, 0.0F), EnergyGeometry.argb(main, 0.12F * fade));
    }

    /** The warning of the lift: a ring of light at the rim of the island on the ground, brighter as the moment comes. */
    private static void liftRing(ClientLevel level, Effect e, float age, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float alpha = 0.3F + 0.5F * u;
        int segments = Math.max(48, (int) (e.radius * 5.0F));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, e.radius, 0.35F, segments, EnergyGeometry.argb(c(GRAV_MAIN), alpha));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, e.radius - 0.5F, 0.12F, segments, EnergyGeometry.argb(c(GRAV_CORE), alpha * 0.8F));
        // A wave of light runs in from the rim every second.
        float wave = (age % 20.0F) / 20.0F;
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, e.radius * (1.0F - wave * 0.15F), 0.2F, segments, EnergyGeometry.argb(c(GRAV_FRINGE), 0.4F * (1.0F - wave)));
    }

    // ---- the body ----

    /** The last body falls: a slow blue-white flash from the chest that grows to the end. */
    private static void death(ClientLevel level, Effect e, float u, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Entity body = level.getEntity(e.entityId);
        Vec3 chest = body == null ? e.at.add(0, 1.5, 0) : chestOf(body, partial);
        float grow = AnimMath.ss(0.3F, 1.0F, u);
        UltronGeometry.flare(pose, vc, chest.subtract(camera), 0.4F + 3.4F * grow * grow, 0.9F * grow, c(GRAV_FRINGE), c(GRAV_MAIN), 0xFFFFFF);
    }

    /** The eyes flash or the core pulses: a small glow at the chest for a moment. */
    private static void pulse(ClientLevel level, Effect e, float u, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Entity body = e.entityId < 0 ? null : level.getEntity(e.entityId);
        Vec3 chest = body == null ? e.at.add(0, 1.5, 0) : chestOf(body, partial);
        float beat = Mth.sin(u * Mth.PI);
        UltronGeometry.flare(pose, vc, chest.subtract(camera), 0.5F + 0.9F * beat, 0.8F * beat, c(PLASMA_EDGE), 0xFF8A5C, 0xFFFFFF);
    }

    /** The middle of the chest of a body, tracked if it was just drawn. */
    static Vec3 chestOf(Entity body, float partial) {
        Vec3 tracked = UltronTracker.chest(body, partial);
        return tracked != null ? tracked : body.getPosition(partial).add(0, body.getBbHeight() * 0.62, 0);
    }

    /** The wreck of a destroyed body: a heap of dark plates and parts, fading out over the last forty ticks. */
    private static void wreckPile(ClientLevel level, Effect e, long now, float partial, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        float age = e.age(now, partial);
        float alpha = Mth.clamp((e.ticks - age) / 40.0F, 0.0F, 1.0F);
        if (alpha <= 0.0F) {
            return;
        }
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        RandomSource random = RandomSource.create(e.start * 977L + (long) (e.at.x * 31) + (long) (e.at.z * 17));
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = 1.3 * Math.pow(random.nextDouble(), 0.7);
            float size = 0.1F + random.nextFloat() * 0.26F;
            float yaw = random.nextFloat() * Mth.TWO_PI;
            int shade = 0x30 + random.nextInt(0x40);
            int colour = random.nextInt(9) == 0 ? 0x8A5A3A : shade << 16 | (shade + 4) << 8 | (shade + 8);
            double x = e.at.x + Math.cos(angle) * dist;
            double z = e.at.z + Math.sin(angle) * dist;
            float y = ground.at(x, z);
            float stack = random.nextFloat() * 0.18F;
            UltronGeometry.box(pose, vc, new Vec3(x - camera.x, y + size * 0.5F + stack - camera.y, z - camera.z), size, size * 0.5F, size * (0.6F + random.nextFloat() * 0.6F), yaw,
                    EnergyGeometry.argb(colour, 0.96F * alpha));
        }
    }

    // ---- data and the network ----

    /**
     * White and red square motes running along a line, with scanline streaks: a transfer, an upload, the Hack. {@code
     * strength} scales how bright it is.
     */
    private static void dataLine(Vec3 from, Vec3 to, int ticks, float age, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc, float strength) {
        Vec3 a = from.subtract(camera);
        Vec3 b = to.subtract(camera);
        double length = a.distanceTo(b);
        if (length < 0.1) {
            return;
        }
        float life = Mth.clamp(Math.min(age / 3.0F, (ticks - age) / 4.0F), 0.0F, 1.0F) * strength;
        EnergyGeometry.ribbon(pose, vc, a, b, 0.03F, 0.03F, EnergyGeometry.argb(c(NET_DARK), 0.3F * life), EnergyGeometry.argb(c(NET_DARK), 0.3F * life));
        int count = Mth.clamp((int) (length * 1.6), 6, 42);
        for (int i = 0; i < count; i++) {
            float t = ((i + 0.5F) / count + time * 0.07F + (i % 3) * 0.013F) % 1.0F;
            Vec3 p = a.lerp(b, t);
            int colour = i % 3 == 0 ? c(NET_RED) : c(NET_WHITE);
            UltronGeometry.square(pose, vc, p, i % 4 == 0 ? 0.2F : 0.12F, EnergyGeometry.argb(colour, 0.9F * life));
            if (i % 5 == 0) {
                // A scanline streak across the line.
                Vec3 side = b.subtract(a).cross(p).normalize().scale(0.35);
                EnergyGeometry.ribbon(pose, vc, p.subtract(side), p.add(side), 0.012F, 0.012F, EnergyGeometry.argb(i % 2 == 0 ? c(NET_RED) : c(NET_DARK), 0.7F * life),
                        EnergyGeometry.argb(i % 2 == 0 ? c(NET_RED) : c(NET_DARK), 0.7F * life));
            }
        }
    }

    // ---- the hero ----

    /** Gravity Slam: a ring of anti-gravity runs out from his feet to the end of its reach, and behind it a fainter one. */
    private static void gravitySlam(ClientLevel level, Effect e, float u, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        UltronGeometry.Ground ground = UltronGeometry.ground(level, e.at.y);
        float eased = 1.0F - (1.0F - u) * (1.0F - u);
        float r = Math.max(0.4F, e.radius * eased);
        float fade = 1.0F - u * u;
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r, 0.5F, 56, EnergyGeometry.argb(c(GRAV_MAIN), 0.85F * fade));
        UltronGeometry.ring(pose, vc, camera, ground, e.at.x, e.at.z, r * 0.7F, 0.3F, 48, EnergyGeometry.argb(c(GRAV_FRINGE), 0.5F * fade));
        UltronGeometry.disc(pose, vc, camera, ground, e.at.x, e.at.z, r, 32, EnergyGeometry.argb(c(GRAV_CORE), 0.18F * fade), EnergyGeometry.argb(c(GRAV_MAIN), 0.04F * fade));
    }

    /** Gravity Pull: the target is wrapped in a contracting ring, and dashes of pale blue run from it to his palm. */
    private static void gravityPull(Effect e, float age, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Vec3 palm = e.at.subtract(camera);
        Vec3 target = e.to.subtract(camera);
        float fade = 1.0F - u * 0.6F;
        for (int i = -1; i <= 1; i++) {
            Vec3 offset = new Vec3(0, 0.12 * i, 0);
            UltronGeometry.dotted(pose, vc, target.add(offset), palm.add(offset), 0.07F, 0.3F, -time * 0.6F + i * 0.3F, EnergyGeometry.argb(c(GRAV_MAIN), 0.9F * fade));
        }
        EnergyGeometry.ribbon(pose, vc, target, palm, 0.03F, 0.03F, EnergyGeometry.argb(c(GRAV_FRINGE), 0.35F * fade), EnergyGeometry.argb(c(GRAV_CORE), 0.55F * fade));
        Vec3 direction = palm.subtract(target);
        float r = Mth.lerp(u, 1.3F, 0.3F);
        EnergyGeometry.ring(pose, vc, target, direction, r, 0.12F, 20, EnergyGeometry.argb(c(GRAV_MAIN), 0.8F * fade));
        EnergyGeometry.ring(pose, vc, target, direction, r * 1.5F, 0.08F, 20, EnergyGeometry.argb(c(GRAV_FRINGE), 0.5F * fade));
        UltronGeometry.flare(pose, vc, palm, 0.28F * (1.0F - u), 0.9F, c(GRAV_FRINGE), c(GRAV_MAIN), c(GRAV_CORE));
    }

    /** The heavy blast leaves the palm: a flash of white-orange and a short spray of streaks along the line of fire. */
    private static void overchargeFlash(Effect e, float u, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Vec3 palm = e.at.subtract(camera);
        float fade = 1.0F - u;
        // (Seen from the palm itself, in the caster's own first person, the flash would fill the screen: it shrinks with how near the eye is.)
        float near = Mth.clamp((float) palm.length() / 3.0F, 0.2F, 1.0F);
        UltronGeometry.flare(pose, vc, palm, e.radius * near * (0.4F + 0.6F * u), fade * (0.5F + 0.5F * near), c(PLASMA_HALO), c(PLASMA_GOLD), 0xFFFFFF);
        Vec3 dir = e.to.normalize();
        for (int i = 0; i < 5; i++) {
            RandomSource random = RandomSource.create(i * 977L + (long) (e.at.x * 13));
            Vec3 spread = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(0.5);
            Vec3 end = palm.add(dir.add(spread).normalize().scale(1.5 + 2.5 * u));
            EnergyGeometry.ribbon(pose, vc, palm, end, 0.1F * fade, 0.02F, EnergyGeometry.argb(c(PLASMA_GOLD), 0.8F * fade), EnergyGeometry.argb(c(PLASMA_HALO), 0.0F));
        }
    }

    /** A burst of the network: square motes of white and red fly out, and scanline streaks cross it. */
    private static void networkFlash(Effect e, float age, float u, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Vec3 centre = e.at.subtract(camera);
        float fade = 1.0F - u;
        RandomSource random = RandomSource.create(Double.doubleToLongBits(e.at.x * 31 + e.at.z) ^ e.start);
        for (int i = 0; i < 16; i++) {
            Vec3 dir = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.4, random.nextDouble() - 0.5).normalize();
            double reach = e.radius * (0.5 + 0.5 * random.nextDouble()) * (0.2 + 0.8 * u);
            UltronGeometry.square(pose, vc, centre.add(dir.scale(reach)), 0.16F * fade + 0.04F, EnergyGeometry.argb(i % 3 == 0 ? c(NET_RED) : c(NET_WHITE), 0.95F * fade));
        }
        for (int i = 0; i < 4; i++) {
            double y = (i - 1.5) * 0.28 * e.radius * 0.5;
            Vec3 side = new Vec3(1, 0, 0);
            Vec3 a = centre.add(0, y, 0).subtract(side.scale(e.radius * 0.9));
            Vec3 b = centre.add(0, y, 0).add(side.scale(e.radius * 0.9));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.015F, 0.015F, EnergyGeometry.argb(i % 2 == 0 ? c(NET_RED) : c(NET_DARK), 0.8F * fade), EnergyGeometry.argb(i % 2 == 0 ? c(NET_RED) : c(NET_DARK), 0.0F));
        }
        UltronGeometry.flare(pose, vc, centre, e.radius * 0.5F * fade, 0.7F, c(NET_DARK), c(NET_RED), c(NET_WHITE));
    }

    /** A drone forms: a Sentry's silhouette built from the feet up, blue turning red as it is finished. */
    private static void droneAssemble(ClientLevel level, Effect e, float u, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        int colour = blend(0x45B4F5, c(NET_RED), u);
        UltronGeometry.silhouette(pose, vc, e.at.subtract(camera), 1.7F, 0.0F, Math.min(1.0F, u * 1.2F), EnergyGeometry.argb(colour, 0.3F * (1.0F - u * 0.5F)));
    }

    /** The Hack reaches out: data runs along a curve from his open hand to the Sentry, and the Sentry's eyes flicker. */
    private static void hackBeam(Effect e, float age, float u, float time, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Vec3 a = e.at;
        Vec3 b = e.to;
        Vec3 mid = a.lerp(b, 0.5).add(0, 0.6, 0);
        // A gentle curve through the middle, drawn as two lines.
        dataLine(a, mid, e.ticks, age, time, camera, pose, vc, 0.8F + 0.2F * u);
        dataLine(mid, b, e.ticks, age, time + 5.0F, camera, pose, vc, 0.8F + 0.2F * u);
        UltronGeometry.flare(pose, vc, a.subtract(camera), 0.3F, 0.7F, c(NET_DARK), c(NET_RED), c(NET_WHITE));
    }

    /** A pale ring of anti-gravity round the creature the drones are sent at, or round him as they gather. */
    private static void commandMark(Effect e, float age, float u, Vec3 camera, Matrix4f pose, VertexConsumer vc) {
        Vec3 centre = e.at.subtract(camera);
        float r = Mth.lerp(u, e.radius * 1.6F, e.radius * 0.7F);
        float fade = 1.0F - u * u;
        EnergyGeometry.ring(pose, vc, centre, new Vec3(0, 1, 0), r, 0.1F, 24, EnergyGeometry.argb(c(GRAV_MAIN), 0.9F * fade));
        EnergyGeometry.ring(pose, vc, centre, new Vec3(0, 1, 0), r * 1.4F, 0.06F, 24, EnergyGeometry.argb(c(GRAV_FRINGE), 0.5F * fade));
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2.0 + age * 0.4;
            UltronGeometry.square(pose, vc, centre.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r), 0.12F, EnergyGeometry.argb(c(GRAV_CORE), 0.9F * fade));
        }
    }

    /** A colour a fraction of the way from one to another. */
    static int blend(int from, int to, float t) {
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return r << 16 | g << 8 | b;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The shake of the Tremor

    /** The ground shakes while the Tremor (and the rise of a lift that cannot happen) lasts: roll and pitch of under a degree. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (EnhancedClientConfig.reducedMotion() || EFFECTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        long now = mc.level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        for (Effect e : EFFECTS) {
            if (e.kind != UltronFxPayload.TREMOR) {
                continue;
            }
            float age = e.age(now, partial);
            // A swell: up over the first second, steady, then down over the last.
            float swell = Mth.clamp(Math.min(age / 20.0F, (e.ticks - age) / 20.0F), 0.0F, 1.0F);
            double dx = camera.x - e.at.x;
            double dz = camera.z - e.at.z;
            double beyond = Math.sqrt(dx * dx + dz * dz) - e.radius;
            if (beyond > 48.0 || swell <= 0.0F) {
                continue;
            }
            float near = (float) (1.0 - Math.max(0.0, beyond) / 48.0);
            float amount = 0.7F * swell * near * near;
            double time = now + partial;
            event.setRoll(event.getRoll() + amount * (float) (Math.sin(time * 0.9) + 0.5 * Math.sin(time * 2.3 + 1.0)));
            event.setPitch(event.getPitch() + 0.6F * amount * (float) Math.sin(time * 1.3 + 2.0));
        }
    }

    /** Whether something is running (for the tests of the numbers). */
    static int running() {
        return EFFECTS.size();
    }

    private UltronEffects() {
    }
}
