package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Ultimate Source Code Vision on fire (3.17, Ultimate Mode only): while he flies or uses a power, his whole body is wrapped
 * in living flames of his own, like the Human Torch's, never Minecraft's fire overlay. With Ultimate Mode off nothing of
 * this is drawn: he is the Source Code Vision of 3.16. Drawn with the other energy effects ({@link WorldEffectsRenderer},
 * additive), by every client, for every such Vision:
 * <ul>
 *   <li>(on the model itself, {@link VisionFlameLayer}: the molten body, its cracks of fire, the sheath of flame;)</li>
 *   <li>tongues of flame growing out of the body on every side (front, back, both flanks, the arms, the legs, the head),
 *   each one bending away from where he flies and up, flickering in length and swaying, in three layers (the outer orange
 *   {@link #OUTER}, the gold {@link #INNER}, a pale core);</li>
 *   <li>a trail of fire streaming behind him, fading and narrowing;</li>
 *   <li>embers and a little smoke shed from his body, a crackle while he burns and a whoosh as he lights.</li>
 * </ul>
 * Only light: nothing is set burning and he never takes fire damage (the server knows nothing of it). In first person it is
 * subtle: a few faint tongues low round him and his trail, nothing over the view.
 */
public final class VisionFlameFx {
    /** The flames' colours: the Source Code's own (his look's colour and glow). */
    public static final int OUTER = 0xFF5A14;
    public static final int INNER = 0xFFB43C;
    static final int CORE = 0xFFF0C8;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int TRAIL = 16;

    /** Per player (entity id): his trail, when he lit, his last crackle, the last tick his embers were shed. */
    private static final class Burn {
        final Vec3[] trail = new Vec3[TRAIL];
        int count;
        long lastTick = Long.MIN_VALUE;
        long litAt;
        long crackleAt;
    }

    private static final Map<Integer, Burn> BURNING = new HashMap<>();

    private VisionFlameFx() {
    }

    /**
     * Whether this player burns now: Ultimate Mode on, Vision as his Source Code (the Solar Jewel's Vision), and he flies
     * or uses a power (his beam, Phase, Density, the jewel's beams).
     */
    public static boolean burning(AbstractClientPlayer player, PowerData data) {
        if (data.power() != Power.VISION || !"source_code".equals(data.variant()) || data.disguise()
                || !com.kolodkingal.enhanced.ultimate.Ultimate.on()) {
            return false;
        }
        boolean flying = player instanceof LocalPlayer lp ? lp.getAbilities().flying : data.flying();
        return flying || data.beamActive() || data.visionPhased() || data.dense()
                || data.is(com.kolodkingal.enhanced.power.Live.SOLAR_EYES)
                || data.is(com.kolodkingal.enhanced.power.Live.SOLAR_GATHERING)
                || data.is(com.kolodkingal.enhanced.power.Live.SOLAR_OVERLOAD);
    }

    /** Forgets the players who stopped burning (or left), once a frame. */
    public static void forgetCold(ClientLevel level) {
        BURNING.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer p)
                || !burning(p, com.kolodkingal.enhanced.power.Powers.get(p)));
    }

    public static void clear() {
        BURNING.clear();
    }

    /** One burning Vision's flames this frame. */
    public static void render(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                              float partialTick, float time, boolean firstPerson) {
        ClientLevel level = (ClientLevel) player.level();
        Burn burn = BURNING.computeIfAbsent(player.getId(), id -> {
            Burn b = new Burn();
            b.litAt = level.getGameTime();
            return b;
        });
        Vec3 feetWorld = player.getPosition(partialTick);
        long tick = level.getGameTime();
        if (tick != burn.lastTick) {
            onTick(level, player, burn, feetWorld, tick, firstPerson);
            burn.lastTick = tick;
        }
        Vec3 feet = feetWorld.subtract(camera);
        Vec3 vel = player.getDeltaMovement();
        double speed = vel.length();
        Vec3 back = speed > 1.0E-3 ? vel.scale(-1.0 / speed) : new Vec3(0, -1, 0);
        // The body leans into his flight (the flying pose), so the flames follow the line of his body.
        float lean = (float) Mth.clamp(speed / 0.6, 0.0, 0.85);
        Vec3 axis = UP.scale(1.0 - lean).add(back.scale(-lean));
        axis = axis.lengthSqr() < 1.0E-6 ? UP : axis.normalize();
        Vec3 side = axis.cross(back);
        if (side.lengthSqr() < 1.0E-6) {
            float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        }
        side = side.normalize();
        float h = player.getBbHeight();
        // The way the flames stream: away from where he flies, and up.
        Vec3 stream = back.scale(Mth.clamp(speed * 2.2, 0.35, 1.6)).add(0, 0.65, 0).normalize();
        float fade = Mth.clamp((tick - burn.litAt + partialTick) / 8.0F, 0.0F, 1.0F); // they catch over a few ticks
        float alpha = (firstPerson ? 0.32F : 1.0F) * fade;
        float eye = player.getEyeHeight();
        int seedBase = player.getId() * 7919;
        // The body's own cross: across his shoulders (side) and through his chest (front), both square to its line.
        Vec3 front = axis.cross(side).normalize();
        // The tongues: out of the skin on every side of him.
        int tongues = firstPerson ? 24 : 56;
        for (int i = 0; i < tongues; i++) {
            float t = (i * 0.61803F + 0.07F) % 1.0F;                    // spread from his feet to his head
            float ang = i * 2.39996F;                                    // and round him (the golden angle)
            if (firstPerson && t * h > eye - 0.9F) {
                continue; // nothing over the view
            }
            float rad = girth(t);
            Vec3 out = side.scale(Mth.cos(ang)).add(front.scale(Mth.sin(ang) * 0.62));
            Vec3 base = feet.add(axis.scale(h * (0.0 + 1.02 * t))).add(out.scale(rad));
            float phase = time * (0.55F + 0.07F * (i % 5)) + i * 1.7F;
            float flick = 0.55F + 0.45F * noise(seedBase + i, time * 0.35F);
            float len = (0.5F + 0.4F * (1.0F - Math.abs(t - 0.55F))) * flick * (1.0F + lean * 1.4F);
            Vec3 sway = side.scale(0.12 * Mth.sin(phase)).add(axis.scale(0.05 * Mth.cos(phase * 1.3F)));
            // (a tongue leaves the body outward, then the wind of his flight and the heat take it)
            Vec3 dir = stream.add(out.scale(0.55)).normalize();
            Vec3 mid = base.add(dir.scale(len * 0.5)).add(sway);
            Vec3 tip = base.add(dir.scale(len * 0.55)).add(stream.scale(len * 0.5)).add(sway.scale(2.2))
                    .add(0, 0.08 * Mth.sin(phase * 1.7F), 0);
            float w = (0.2F + 0.1F * flick) * (firstPerson ? 0.7F : 1.0F);
            tongue(pose, vc, base, mid, tip, w * 1.6F, OUTER, 0.26F * alpha);
            tongue(pose, vc, base, mid, tip.subtract(stream.scale(len * 0.25)), w, INNER, 0.34F * alpha);
            tongue(pose, vc, base, base.add(dir.scale(len * 0.3)), base.add(dir.scale(len * 0.5)), w * 0.45F, CORE,
                    0.3F * alpha);
            // (white-yellow at the flame's edge, as fire is brightest where it meets the air)
            tongue(pose, vc, mid, tip, tip.add(stream.scale(len * 0.35)), w * 0.3F, CORE, 0.4F * alpha);
        }
        if (!firstPerson) {
            // the crown of flame over his head
            Vec3 head = feet.add(axis.scale(h * 0.98));
            for (int k = 0; k < 5; k++) {
                float phase = time * 0.6F + k * 1.3F;
                Vec3 dir = stream.add(side.scale(0.35 * Mth.sin(phase))).normalize();
                float len = 0.45F + 0.2F * noise(seedBase + 100 + k, time * 0.4F);
                tongue(pose, vc, head, head.add(dir.scale(len * 0.5)), head.add(dir.scale(len)), 0.3F, OUTER, 0.35F * alpha);
                tongue(pose, vc, head, head.add(dir.scale(len * 0.4)), head.add(dir.scale(len * 0.7)), 0.18F, INNER,
                        0.6F * alpha);
            }
        }
        trail(pose, vc, burn, camera, feet.add(axis.scale(h * 0.5)), h, alpha, time, firstPerson);
    }

    /** How far the body's surface is from its middle line at {@code t} (0 his feet, 1 the top of his head). */
    private static float girth(float t) {
        if (t < 0.45F) {
            return 0.26F;                                   // the legs
        }
        if (t < 0.78F) {
            return 0.44F;                                   // the chest and the arms
        }
        return Mth.lerp((t - 0.78F) / 0.22F, 0.36F, 0.3F);  // the shoulders up to the head
    }

    /** A tongue of flame: base, bend and tip, wide at the base and pointed at the tip. */
    private static void tongue(Matrix4f pose, VertexConsumer vc, Vec3 base, Vec3 mid, Vec3 tip, float width, int rgb, float a) {
        EnergyGeometry.ribbon(pose, vc, base, mid, width, width * 0.6F, EnergyGeometry.argb(rgb, a),
                EnergyGeometry.argb(rgb, a * 0.8F));
        EnergyGeometry.ribbon(pose, vc, mid, tip, width * 0.6F, 0.0F, EnergyGeometry.argb(rgb, a * 0.8F),
                EnergyGeometry.argb(rgb, 0.0F));
    }

    /** The trail of fire behind him, from the last tick's places (newest first), narrowing and fading. */
    private static void trail(Matrix4f pose, VertexConsumer vc, Burn burn, Vec3 camera, Vec3 now, float h, float alpha,
                              float time, boolean firstPerson) {
        Vec3 prev = now;
        for (int k = 0; k < burn.count; k++) {
            Vec3 p = burn.trail[k].subtract(camera).add(0, h * 0.5, 0);
            float f = 1.0F - (float) k / TRAIL;
            float w = (firstPerson ? 0.35F : 0.7F) * f;
            float wob = 0.06F * Mth.sin(time * 0.9F + k);
            Vec3 a = prev.add(0, wob, 0);
            EnergyGeometry.ribbon(pose, vc, a, p, w, w * 0.85F, EnergyGeometry.argb(OUTER, 0.32F * f * alpha),
                    EnergyGeometry.argb(OUTER, 0.26F * f * alpha));
            EnergyGeometry.ribbon(pose, vc, a, p, w * 0.45F, w * 0.4F, EnergyGeometry.argb(INNER, 0.5F * f * alpha),
                    EnergyGeometry.argb(INNER, 0.42F * f * alpha));
            prev = p;
        }
    }

    /** Once a tick: the trail moves on, embers and smoke are shed, he crackles (and whooshes as he lights). */
    private static void onTick(ClientLevel level, AbstractClientPlayer player, Burn burn, Vec3 feet, long tick,
                               boolean firstPerson) {
        System.arraycopy(burn.trail, 0, burn.trail, 1, TRAIL - 1);
        burn.trail[0] = feet;
        burn.count = Math.min(TRAIL, burn.count + 1);
        RandomSource random = level.random;
        float h = player.getBbHeight();
        Vec3 vel = player.getDeltaMovement();
        int embers = firstPerson ? 1 : 3;
        for (int i = 0; i < embers; i++) {
            double x = feet.x + (random.nextDouble() - 0.5) * 0.7;
            double y = feet.y + random.nextDouble() * h;
            double z = feet.z + (random.nextDouble() - 0.5) * 0.7;
            int color = random.nextBoolean() ? INNER : OUTER;
            level.addParticle(new EnergyParticleOptions(color, 0.45F + random.nextFloat() * 0.35F), x, y, z,
                    -vel.x * 0.3 + (random.nextDouble() - 0.5) * 0.04, 0.03 + random.nextDouble() * 0.04,
                    -vel.z * 0.3 + (random.nextDouble() - 0.5) * 0.04);
        }
        if (!firstPerson && tick % 3 == 0) {
            level.addParticle(ParticleTypes.SMOKE, feet.x + (random.nextDouble() - 0.5) * 0.5, feet.y + h * 0.6,
                    feet.z + (random.nextDouble() - 0.5) * 0.5, -vel.x * 0.2, 0.04, -vel.z * 0.2);
        }
        if (tick - burn.litAt == 0) {
            level.playLocalSound(feet.x, feet.y + 1, feet.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.3F, false);
        }
        if (tick >= burn.crackleAt) {
            level.playLocalSound(feet.x, feet.y + 1, feet.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                    firstPerson ? 0.35F : 0.6F, 0.9F + random.nextFloat() * 0.3F, false);
            if (vel.length() > 0.5) {
                level.playLocalSound(feet.x, feet.y + 1, feet.z, SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.25F,
                        1.4F + random.nextFloat() * 0.3F, false);
            }
            burn.crackleAt = tick + 24 + random.nextInt(16);
        }
    }

    /** Smooth noise in [-1, 1] from a seed and a running phase (a few sines of unrelated frequencies). */
    private static float noise(int seed, float phase) {
        float s = seed * 0.37F;
        return (Mth.sin(phase + s) * 0.5F + Mth.sin(phase * 2.31F + s * 1.7F) * 0.3F + Mth.sin(phase * 4.13F + s * 2.9F) * 0.2F);
    }
}
