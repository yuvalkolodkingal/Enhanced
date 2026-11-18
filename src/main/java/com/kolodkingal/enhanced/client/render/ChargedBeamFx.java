package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.ChargedBeamConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * (3.17) What a charged beam looks like ({@code ability.ChargedBeam}): while {@link Live#BEAM_CHARGING} is on, rings of
 * light close in on where the beam will leave (the Solar Jewel of Source Code Vision, Vision's forehead, Jim Hammond's
 * brow), sparks spiral in and a glow swells with the charge; when {@link Live#BEAM_FIRING} is on, the beam itself, as wide
 * and as far as its charge made it, fading over its few ticks. The charge is counted here from the tick the state came on
 * (the server's number is not sent: the state is all the clients get), with the same numbers the server reads.
 */
public final class ChargedBeamFx {
    /** A beam's colours: the wide glow, the hot core, the sparks and bolts. */
    record Colors(int glow, int core, int spark) {
    }

    static final Colors MIND = new Colors(0xF5D77A, 0xFFF6D5, 0xF2C66A);
    static final Colors FIRE = new Colors(0xFF4A10, 0xFFE9A0, 0xFF9A1E);
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** When each player's charge began (game time), and the charge it had when it last was seen. */
    private static final Map<Integer, float[]> SEEN = new HashMap<>();

    private ChargedBeamFx() {
    }

    /** The key whose line of the HUD shows the charge: B for Vision. */
    public static int hudKey(Power power) {
        return power == Power.VISION ? 4 : power == Power.JIM_HAMMOND ? 1 : -1;
    }

    private static int chargeTicks(Power power) {
        if (power == Power.JIM_HAMMOND) {
            return EnhancedConfig.SPEC.isLoaded() ? com.kolodkingal.enhanced.config.JimConfig.BEAM_CHARGE_TICKS.get() : 40;
        }
        return EnhancedConfig.SPEC.isLoaded() ? ChargedBeamConfig.CHARGE_TICKS.get() : 50;
    }

    private static double range(Power power, float charge) {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return 32;
        }
        if (power == Power.JIM_HAMMOND) {
            return Mth.lerp(charge, com.kolodkingal.enhanced.config.JimConfig.BEAM_MIN_RANGE.get(),
                    Math.max(com.kolodkingal.enhanced.config.JimConfig.BEAM_MIN_RANGE.get(), com.kolodkingal.enhanced.config.JimConfig.BEAM_MAX_RANGE.get()));
        }
        return Mth.lerp(charge, ChargedBeamConfig.MIN_RANGE.get(), Math.max(ChargedBeamConfig.MIN_RANGE.get(), ChargedBeamConfig.MAX_RANGE.get()));
    }

    /** How charged the player's beam is at this time: 0 (just begun, or a tap) to 1 (full). */
    static float charge(AbstractClientPlayer player, PowerData data, float time) {
        float[] seen = SEEN.computeIfAbsent(player.getId(), k -> new float[]{-1, 0, -1, 0});
        if (data.is(Live.BEAM_CHARGING)) {
            if (seen[0] < 0) {
                seen[0] = time;
            }
            seen[1] = Mth.clamp((time - seen[0]) / Math.max(1, chargeTicks(data.power())), 0.0F, 1.0F);
            return seen[1];
        }
        seen[0] = -1;
        return seen[1];
    }

    /** The HUD's line while the beam charges ("Charging 63%"), or null. */
    public static Component hudLine(AbstractClientPlayer player, PowerData data, long now) {
        if (!data.is(Live.BEAM_CHARGING)) {
            return null;
        }
        float charge = charge(player, data, now);
        int percent = Math.round(charge * 100.0F);
        return Component.translatable("hud.enhanced.charged", percent)
                .withStyle(percent >= 100 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
    }

    /** Forgets the players (a world was left). */
    public static void clear() {
        SEEN.clear();
    }

    // ---- drawing ----

    /** One player's charge build-up and fired beam this frame; {@code origin} is where the beam leaves (world position). */
    static void render(Minecraft mc, AbstractClientPlayer player, PowerData data, Vec3 origin, Colors colors, Matrix4f pose,
                       VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        float charge = charge(player, data, time);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 a = origin.subtract(camera);
        if (data.is(Live.BEAM_CHARGING)) {
            gather(mc, player, pose, vc, camera, a, look, colors, charge, time, partialTick);
            SEEN.get(player.getId())[2] = -1;
            return;
        }
        float[] seen = SEEN.get(player.getId());
        if (!data.is(Live.BEAM_FIRING)) {
            if (seen != null) {
                seen[2] = -1;
            }
            return;
        }
        if (seen == null) {
            seen = new float[]{-1, 0.5F, -1, 0};
            SEEN.put(player.getId(), seen);
        }
        if (seen[2] < 0) {
            seen[2] = time;
        }
        float t = Mth.clamp((time - seen[2]) / 8.0F, 0.0F, 1.0F);
        fired(player, data.power(), pose, vc, camera, a, origin, look, colors, seen[1], t, time, partialTick);
    }

    private static void gather(Minecraft mc, AbstractClientPlayer player, Matrix4f pose, VertexConsumer vc, Vec3 camera, Vec3 a,
                               Vec3 look, Colors c, float charge, float time, float partialTick) {
        gatherAround(pose, vc, a, look, c, charge, time, 1.0F);
        if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
            // first person: the brow is above the view, so the build-up is also drawn closing in on the aim, a few blocks ahead
            Vec3 ahead = player.getEyePosition(partialTick).add(look.scale(3.0)).subtract(camera);
            for (int k = 0; k < 2; k++) {
                float p = (time * (0.03F + 0.05F * charge) + k * 0.5F) % 1.0F;
                EnergyGeometry.ring(pose, vc, ahead, look, Mth.lerp(p, 0.5F, 0.05F) * (0.5F + charge), 0.02F, 28,
                        EnergyGeometry.argb(c.spark, Mth.sin(p * Mth.PI) * (0.25F + 0.5F * charge)));
            }
            flare(pose, vc, ahead, 0.02F + 0.1F * charge, c);
        }
    }

    /**
     * Rings closing in on {@code a} (camera-relative, facing {@code look}), sparks spiralling in and a glow swelling with the
     * charge; {@code scale} 1 is a beam's brow, more is the Nova Flame's whole body.
     */
    static void gatherAround(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 look, Colors c, float charge, float time, float scale) {
        for (int k = 0; k < 3; k++) {
            float p = (time * (0.03F + 0.05F * charge) + k / 3.0F) % 1.0F;
            float radius = Mth.lerp(p, 1.1F, 0.06F) * (0.55F + 0.45F * charge) * scale;
            float alpha = Mth.sin(p * Mth.PI) * (0.35F + 0.65F * charge);
            EnergyGeometry.ring(pose, vc, a, look, radius, 0.035F + 0.03F * p, 32, EnergyGeometry.argb(c.spark, alpha));
        }
        int sparks = 10 + (int) (18 * charge);
        for (int i = 0; i < sparks; i++) {
            float phase = (time * 0.05F + i * 0.618F) % 1.0F;
            float dist = (1.0F - phase) * (0.5F + 1.3F * charge) * scale;
            float yaw = i * 2.399F + phase * 3.0F;
            float pitch = (i * 0.37F) % 1.0F * Mth.PI - Mth.HALF_PI;
            Vec3 dir = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch));
            Vec3 from = a.add(dir.scale(dist));
            Vec3 to = a.add(dir.scale(Math.max(0.0, dist - 0.2F * scale)));
            EnergyGeometry.ribbon(pose, vc, from, to, 0.01F, 0.025F, EnergyGeometry.argb(c.glow, 0.0F),
                    EnergyGeometry.argb(c.core, 0.8F * charge + 0.2F));
        }
        flare(pose, vc, a, (0.05F + 0.28F * charge + 0.02F * Mth.sin(time * 1.4F)) * scale, c);
    }

    static void flareAt(Matrix4f pose, VertexConsumer vc, Vec3 at, float size, Colors c) {
        flare(pose, vc, at, size, c);
    }

    private static void fired(AbstractClientPlayer player, Power power, Matrix4f pose, VertexConsumer vc, Vec3 camera, Vec3 a, Vec3 origin,
                              Vec3 look, Colors c, float charge, float t, float time, float partialTick) {
        double range = range(power, charge);
        Vec3 end = Raycasts.block(player, range, partialTick).getLocation();
        Vec3 b = end.subtract(camera);
        float strength = 1.0F - t * t;
        float w = (0.3F + 0.7F * charge) * strength;
        float near = (float) Mth.clamp(a.length() / 1.5, 0.25, 1.0);
        float flicker = 0.9F + 0.1F * Mth.sin(time * 2.7F);
        EnergyGeometry.ribbon(pose, vc, a, b, 0.18F * w * near, 0.9F * w * flicker, EnergyGeometry.argb(c.glow, 0.12F * strength), EnergyGeometry.argb(c.glow, 0.28F * strength));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.09F * w * near, 0.45F * w, EnergyGeometry.argb(c.glow, 0.45F * strength), EnergyGeometry.argb(c.glow, 0.55F * strength));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.035F * w * near, 0.14F * w, EnergyGeometry.argb(c.core, 0.85F * strength), EnergyGeometry.argb(c.core, 0.9F * strength));
        long seed = player.getId() * 7349L;
        for (int k = 0; k < 2; k++) {
            EnergyGeometry.bolt(pose, vc, a, b, 0.025F, 0.2F * w + 0.05F, EnergyGeometry.argb(c.spark, 0.7F * strength), seed + k * 104729L, time / 2.0F);
        }
        flare(pose, vc, b, (0.5F + 0.9F * charge) * strength + 0.05F, c);
    }

    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 at, float size, Colors c) {
        Vec3 toCamera = at.scale(-1).normalize();
        Vec3 side = toCamera.cross(UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCamera).normalize();
        EnergyGeometry.ribbon(pose, vc, at.subtract(side.scale(size)), at.add(side.scale(size)), size, EnergyGeometry.argb(c.glow, 0.65F), EnergyGeometry.argb(c.glow, 0.65F));
        EnergyGeometry.ribbon(pose, vc, at.subtract(up.scale(size)), at.add(up.scale(size)), size * 0.5F, EnergyGeometry.argb(c.core, 0.6F), EnergyGeometry.argb(c.core, 0.6F));
    }
}
