package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.SolarJewelConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solar Jewel Vision's light (3.16), drawn with the other beams ({@link WorldEffectsRenderer}, additive):
 * <ul>
 *   <li>the Solar Beam: a golden beam from the jewel, a white-hot core, two strands of sunlight twisting round it, and a
 *   turning sunburst at his brow;</li>
 *   <li>the eye beams: two thin red-orange beams of heat from his eyes, shimmering, meeting where he looks;</li>
 *   <li>the Overload: while the light gathers, gold rings close in on his brow, sparks spiral into it and the jewel
 *   swells to a small sun; then one great beam, three strands coiling round it and rings of light racing along it,
 *   fading as the jewel empties;</li>
 *   <li>by day under the open sky, while the jewel is not full, motes of sunlight drift down into it.</li>
 * </ul>
 */
public final class SolarJewelFx {
    /** The jewel's gold: the glow, the white-hot core, the bolts. */
    static final int GOLD = 0xFFB52E;
    static final int CORE = 0xFFFCEB;
    static final int SPARK = 0xFFE27A;
    /** The heat of the eye beams. */
    static final int HEAT = 0xFF3C12;
    static final int HEAT_CORE = 0xFFE0B0;
    static final int HEAT_BOLT = 0xFF8A24;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** When each player's Overload began gathering and firing, as this client saw it (game time with the partial tick). */
    private static final Map<Integer, float[]> OVERLOAD = new HashMap<>();

    private SolarJewelFx() {
    }

    /** All of one Solar Jewel Vision's light this frame (the beam itself too, in place of the Mind Stone's). */
    static void render(Minecraft mc, AbstractClientPlayer player, PowerData data, Matrix4f pose, VertexConsumer vc,
                       Vec3 camera, float partialTick, float time) {
        Vec3 jewel = WorldEffectsRenderer.foreheadPosition(mc, player, partialTick);
        Vec3 look = player.getViewVector(partialTick);
        if (data.beamActive() && !data.visionPhased()) {
            solarBeam(mc, player, jewel, look, pose, vc, camera, partialTick, time);
        }
        if (data.is(Live.SOLAR_EYES)) {
            eyeBeams(mc, player, jewel, look, pose, vc, camera, partialTick, time);
        }
        overload(mc, player, data, jewel, look, pose, vc, camera, partialTick, time);
        if (!player.isInvisible() && !data.is(Live.SOLAR_OVERLOAD)) {
            sunMotes(mc, player, data, jewel, pose, vc, camera, time);
        }
    }

    // ---- the frame of the look ----

    private static Vec3 right(Vec3 look) {
        Vec3 r = look.cross(UP);
        return r.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : r.normalize();
    }

    private static boolean firstPerson(Minecraft mc, AbstractClientPlayer player) {
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    // ---- R: the Solar Beam ----

    private static void solarBeam(Minecraft mc, AbstractClientPlayer player, Vec3 jewel, Vec3 look, Matrix4f pose,
                                  VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        double range = EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_BEAM_RANGE.get() : 32;
        Vec3 target = Raycasts.any(player, range, WandaAbilities.BEAM_TARGET, partialTick).getLocation();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player,
                player.getEyePosition(partialTick), target, partialTick);
        Vec3 end = shield != null ? shield.point() : target;
        Vec3 a = jewel.subtract(camera);
        Vec3 b = end.subtract(camera);
        float flicker = 0.88F + 0.12F * Mth.sin(time * 1.9F) * Mth.cos(time * 0.7F);
        float near = (float) Mth.clamp(a.length() / 1.5, 0.25, 1.0);
        // the glow, broad and soft, then brighter, then the white-hot core
        EnergyGeometry.ribbon(pose, vc, a, b, 0.16F * near, 0.70F * flicker, EnergyGeometry.argb(GOLD, 0.10F), EnergyGeometry.argb(GOLD, 0.26F));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.08F * near, 0.34F * flicker, EnergyGeometry.argb(GOLD, 0.45F), EnergyGeometry.argb(GOLD, 0.55F));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.03F * near, 0.11F, EnergyGeometry.argb(CORE, 0.85F), EnergyGeometry.argb(CORE, 0.9F));
        // two strands of sunlight coiling round it
        helix(pose, vc, a, b, 0.18F * near, 0.28F, 2, time * 0.35F, 0.025F, EnergyGeometry.argb(SPARK, 0.7F));
        long seed = player.getId() * 7349L;
        for (int k = 0; k < 2; k++) {
            EnergyGeometry.bolt(pose, vc, a, b, 0.025F, 0.22F, EnergyGeometry.argb(SPARK, 0.6F), seed + k * 104729L, time / 2.0F);
        }
        sunburst(pose, vc, a, b.subtract(a).normalize(), 0.30F * near, time, 0.9F);
        flare(pose, vc, b, 0.6F + 0.2F * Mth.sin(time * 2.1F), GOLD, CORE);
    }

    /** A sun at {@code center}: rays turning in the plane across {@code axis}, each breathing in and out. */
    private static void sunburst(Matrix4f pose, VertexConsumer vc, Vec3 center, Vec3 axis, float size, float time, float alpha) {
        Vec3 r = right(axis);
        Vec3 u = r.cross(axis).normalize();
        int rays = 12;
        float turn = time * 0.06F;
        for (int i = 0; i < rays; i++) {
            float angle = turn + i * Mth.TWO_PI / rays;
            float len = size * (i % 2 == 0 ? 1.0F : 0.6F) * (0.8F + 0.2F * Mth.sin(time * 0.4F + i * 1.7F));
            Vec3 dir = r.scale(Mth.cos(angle)).add(u.scale(Mth.sin(angle)));
            EnergyGeometry.ribbon(pose, vc, center, center.add(dir.scale(len)), len * 0.16F, 0.0F,
                    EnergyGeometry.argb(SPARK, 0.8F * alpha), EnergyGeometry.argb(GOLD, 0.0F));
        }
        EnergyGeometry.ring(pose, vc, center, axis, size * 0.45F, size * 0.08F, 20, EnergyGeometry.argb(CORE, 0.6F * alpha));
    }

    /** {@code strands} coils of light wound round the line a-b, turning with {@code phase}. */
    private static void helix(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float radiusA, float radiusB, int strands,
                              float phase, float width, int color) {
        Vec3 axis = b.subtract(a);
        double length = axis.length();
        if (length < 0.1) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 r = right(dir);
        Vec3 u = r.cross(dir).normalize();
        int steps = Mth.clamp((int) (length * 3), 8, 160);
        for (int s = 0; s < strands; s++) {
            List<Vec3> points = new ArrayList<>(steps + 1);
            for (int i = 0; i <= steps; i++) {
                float t = i / (float) steps;
                float angle = phase * 6.0F + t * (float) length * 1.6F + s * Mth.TWO_PI / strands;
                float radius = Mth.lerp(t, radiusA, radiusB);
                points.add(a.add(axis.scale(t)).add(r.scale(Mth.cos(angle) * radius)).add(u.scale(Mth.sin(angle) * radius)));
            }
            EnergyGeometry.polyline(pose, vc, points, width, color);
        }
    }

    /** A burst where light strikes: two crossed ribbons facing the camera. */
    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 at, float size, int glow, int core) {
        Vec3 toCamera = at.scale(-1).normalize();
        Vec3 side = right(toCamera);
        Vec3 up = side.cross(toCamera).normalize();
        EnergyGeometry.ribbon(pose, vc, at.subtract(side.scale(size)), at.add(side.scale(size)), size, EnergyGeometry.argb(glow, 0.65F), EnergyGeometry.argb(glow, 0.65F));
        EnergyGeometry.ribbon(pose, vc, at.subtract(up.scale(size)), at.add(up.scale(size)), size * 0.5F, EnergyGeometry.argb(core, 0.6F), EnergyGeometry.argb(core, 0.6F));
    }

    // ---- N: the eye beams ----

    private static void eyeBeams(Minecraft mc, AbstractClientPlayer player, Vec3 jewel, Vec3 look, Matrix4f pose,
                                 VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        double range = EnhancedConfig.SPEC.isLoaded() ? SolarJewelConfig.EYE_BEAM_RANGE.get() : 28;
        Vec3 end = Raycasts.any(player, range, WandaAbilities.BEAM_TARGET, partialTick).getLocation().subtract(camera);
        Vec3 r = right(look);
        Vec3 u = r.cross(look).normalize();
        Vec3 left;
        Vec3 rightEye;
        if (firstPerson(mc, player)) {
            // from the lower corners of the view, so both are seen meeting ahead
            Vec3 eye = player.getEyePosition(partialTick).add(look.scale(0.8)).subtract(u.scale(0.12));
            left = eye.subtract(r.scale(0.22));
            rightEye = eye.add(r.scale(0.22));
        } else {
            // the eyes: a little below the jewel, either side
            Vec3 face = jewel.subtract(u.scale(0.11)).subtract(look.scale(0.02));
            left = face.subtract(r.scale(0.075));
            rightEye = face.add(r.scale(0.075));
        }
        float flicker = 0.8F + 0.2F * Mth.sin(time * 3.1F);
        long seed = player.getId() * 31337L;
        int i = 0;
        for (Vec3 eye : new Vec3[]{left, rightEye}) {
            Vec3 a = eye.subtract(camera);
            float near = (float) Mth.clamp(a.length() / 1.5, 0.3, 1.0);
            EnergyGeometry.ribbon(pose, vc, a, end, 0.05F * near, 0.22F * flicker, EnergyGeometry.argb(HEAT, 0.18F), EnergyGeometry.argb(HEAT, 0.32F));
            EnergyGeometry.ribbon(pose, vc, a, end, 0.02F * near, 0.07F, EnergyGeometry.argb(HEAT_CORE, 0.75F), EnergyGeometry.argb(HEAT_CORE, 0.8F));
            // the shimmer of heat along it
            EnergyGeometry.bolt(pose, vc, a, end, 0.012F, 0.08F, EnergyGeometry.argb(HEAT_BOLT, 0.7F), seed + i * 977L, time / 1.5F);
            // a glint at each eye
            EnergyGeometry.ring(pose, vc, a, look, 0.03F * near, 0.025F, 10, EnergyGeometry.argb(HEAT_CORE, 0.8F));
            i++;
        }
        flare(pose, vc, end, 0.35F + 0.12F * Mth.sin(time * 4.0F), HEAT, HEAT_CORE);
    }

    // ---- Sneak + R: the Overload ----

    private static void overload(Minecraft mc, AbstractClientPlayer player, PowerData data, Vec3 jewel, Vec3 look,
                                 Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        boolean gathering = data.is(Live.SOLAR_GATHERING);
        boolean firing = data.is(Live.SOLAR_OVERLOAD);
        int id = player.getId();
        if (!gathering && !firing) {
            OVERLOAD.remove(id);
            return;
        }
        float[] at = OVERLOAD.computeIfAbsent(id, k -> new float[]{time, -1.0F});
        if (firing && at[1] < 0) {
            at[1] = time;
        }
        Vec3 a = jewel.subtract(camera);
        if (gathering && !firing) {
            float ticks = EnhancedConfig.SPEC.isLoaded() ? SolarJewelConfig.OVERLOAD_CHARGE_TICKS.get() : 30;
            float t = Mth.clamp((time - at[0]) / ticks, 0.0F, 1.0F);
            gather(pose, vc, a, look, t, time, player.getId());
            return;
        }
        float ticks = EnhancedConfig.SPEC.isLoaded() ? SolarJewelConfig.OVERLOAD_FIRE_TICKS.get() : 24;
        float t = Mth.clamp((time - at[1]) / ticks, 0.0F, 1.0F);
        double range = EnhancedConfig.SPEC.isLoaded() ? SolarJewelConfig.OVERLOAD_RANGE.get() : 64;
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = player.level().clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation().subtract(camera);
        greatBeam(pose, vc, a, end, t, time);
    }

    /** The light gathering into the jewel: {@code t} from 0 (it begins) to 1 (it fires). */
    private static void gather(Matrix4f pose, VertexConsumer vc, Vec3 jewel, Vec3 look, float t, float time, int seed) {
        // three rings closing in on his brow, one after the other, each brighter as it shrinks
        for (int k = 0; k < 3; k++) {
            float p = (t * 1.6F + k / 3.0F) % 1.0F;
            float radius = Mth.lerp(p, 1.8F, 0.08F);
            float alpha = Mth.sin(p * Mth.PI) * (0.4F + 0.6F * t);
            EnergyGeometry.ring(pose, vc, jewel, look, radius, 0.05F + 0.04F * p, 40, EnergyGeometry.argb(SPARK, alpha));
        }
        // sparks spiralling in from all round him
        int sparks = 28;
        for (int i = 0; i < sparks; i++) {
            float phase = (time * 0.05F + i * 0.618F) % 1.0F;
            float dist = (1.0F - phase) * 2.2F;
            float yaw = i * 2.399F + phase * 3.0F;
            float pitch = (i * 0.37F) % 1.0F * Mth.PI - Mth.HALF_PI;
            Vec3 dir = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch));
            Vec3 from = jewel.add(dir.scale(dist));
            Vec3 to = jewel.add(dir.scale(Math.max(0.0, dist - 0.25F)));
            EnergyGeometry.ribbon(pose, vc, from, to, 0.012F, 0.03F, EnergyGeometry.argb(GOLD, 0.0F),
                    EnergyGeometry.argb(CORE, 0.85F * t + 0.15F));
        }
        // the jewel swells to a little sun
        float glow = 0.05F + 0.22F * t + 0.03F * Mth.sin(time * 1.3F);
        flare(pose, vc, jewel, glow * 2.2F, GOLD, CORE);
        sunburst(pose, vc, jewel, look, 0.25F + 0.6F * t, time * (1.0F + 3.0F * t), 0.6F + 0.4F * t);
    }

    /** The Overload's beam: {@code t} from 0 (it bursts out) to 1 (the jewel is empty). */
    private static void greatBeam(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float t, float time) {
        // it bursts out at once and fades as the light runs out
        float strength = (t < 0.1F ? t / 0.1F : 1.0F) * (1.0F - Mth.clamp((t - 0.7F) / 0.3F, 0.0F, 1.0F));
        float flicker = 0.9F + 0.1F * Mth.sin(time * 2.7F);
        float w = (0.6F + 0.4F * flicker) * strength;
        EnergyGeometry.ribbon(pose, vc, a, b, 0.5F * w, 1.6F * w, EnergyGeometry.argb(GOLD, 0.14F * strength), EnergyGeometry.argb(GOLD, 0.24F * strength));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.28F * w, 0.9F * w, EnergyGeometry.argb(GOLD, 0.4F * strength), EnergyGeometry.argb(GOLD, 0.5F * strength));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.12F * w, 0.38F * w, EnergyGeometry.argb(CORE, 0.85F * strength), EnergyGeometry.argb(CORE, 0.9F * strength));
        helix(pose, vc, a, b, 0.35F * w, 1.1F * w, 3, time * 0.5F, 0.05F * w + 0.01F, EnergyGeometry.argb(SPARK, 0.75F * strength));
        // rings of light racing out along it
        Vec3 axis = b.subtract(a);
        double length = axis.length();
        if (length > 0.5) {
            Vec3 dir = axis.scale(1.0 / length);
            for (int k = 0; k < 6; k++) {
                float p = (time * 0.12F + k / 6.0F) % 1.0F;
                Vec3 c = a.add(axis.scale(p));
                float radius = Mth.lerp(p, 0.4F, 1.6F) * w;
                EnergyGeometry.ring(pose, vc, c, dir, radius, 0.08F * w, 32, EnergyGeometry.argb(CORE, 0.5F * strength * (1.0F - p)));
            }
        }
        sunburst(pose, vc, a, axis.normalize(), 0.8F * w, time * 3.0F, strength);
        flare(pose, vc, b, (1.4F + 0.4F * Mth.sin(time * 1.9F)) * w, GOLD, CORE);
    }

    // ---- the jewel drinking in the sun ----

    /** By day under the open sky, while the jewel is not full, motes of sunlight drift down into it. */
    private static void sunMotes(Minecraft mc, AbstractClientPlayer player, PowerData data, Vec3 jewel, Matrix4f pose,
                                 VertexConsumer vc, Vec3 camera, float time) {
        if (!EnhancedConfig.SPEC.isLoaded() || data.ironCharge() >= SolarJewelConfig.MAX_CHARGE.get()
                || player.getAbilities().instabuild) {
            return;
        }
        var level = player.level();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.dimensionType().hasSkyLight() || !level.isDay() || !level.canSeeSky(head) || level.isRainingAt(head)) {
            return;
        }
        Vec3 j = jewel.subtract(camera);
        // in first person they would fill the view: a few, further out
        boolean own = firstPerson(mc, player);
        int motes = own ? 5 : 12;
        for (int i = 0; i < motes; i++) {
            float phase = (time * 0.025F + i * 0.618F) % 1.0F;
            float height = (1.0F - phase) * (own ? 3.0F : 2.4F);
            float swirl = i * 2.399F + phase * 4.0F;
            float radius = (1.0F - phase) * (own ? 1.4F : 0.9F);
            Vec3 p = j.add(Mth.cos(swirl) * radius, height, Mth.sin(swirl) * radius);
            Vec3 next = j.add(Mth.cos(swirl + 0.3F) * radius * 0.85F, height * 0.85F, Mth.sin(swirl + 0.3F) * radius * 0.85F);
            float alpha = Mth.sin(phase * Mth.PI) * 0.75F;
            EnergyGeometry.ribbon(pose, vc, p, next, 0.012F, 0.03F, EnergyGeometry.argb(GOLD, 0.0F), EnergyGeometry.argb(SPARK, alpha));
        }
    }
}
