package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Jim Hammond's fire that is not the body's own (3.17): the flames and sparks that stream from every part of his burning body
 * opposite to the way he moves ({@link #tick}: from the points of the body the model was drawn at, {@link HandTracker}: head,
 * shoulders, hands, feet; so they follow the tilt of flight and every pose), and the Nova Flame's light: the energy gathering
 * round his chest, then the shell of fire racing out from him ({@link #nova}, drawn with the other beams).
 */
public final class JimFx {
    private static final Map<Integer, float[]> NOVA = new HashMap<>();

    private JimFx() {
    }

    public static void clear() {
        NOVA.clear();
    }

    /** Every client tick: the flames streaming from every burning body in view. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!JimFlameLayer.burning(data) || player.isInvisible()) {
                continue;
            }
            Vec3 velocity = player.getDeltaMovement();
            // the fire streams back from him: opposite to his motion, and a little up
            Vec3 trail = velocity.scale(-0.35).add(0, 0.03, 0);
            float partial = 1.0F;
            boolean own = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
            Vec3[] points = {
                    HandTracker.forehead(player, partial), HandTracker.shoulder(player, HumanoidArm.RIGHT, partial),
                    HandTracker.shoulder(player, HumanoidArm.LEFT, partial), HandTracker.hand(player, HumanoidArm.RIGHT, partial),
                    HandTracker.hand(player, HumanoidArm.LEFT, partial), HandTracker.foot(player, HumanoidArm.RIGHT, partial),
                    HandTracker.foot(player, HumanoidArm.LEFT, partial)};
            Vec3 base = player.position();
            for (int i = 0; i < points.length; i++) {
                Vec3 at = points[i];
                if (at == null) {
                    // (a body not drawn lately, or the player's own in first person: only the feet's flames, down where he stands)
                    if (i < 5) {
                        continue;
                    }
                    at = base.add(0, 0.1, 0);
                }
                if (level.random.nextInt(2) == 0) {
                    level.addParticle(ParticleTypes.FLAME, at.x + rnd(level, 0.12), at.y + rnd(level, 0.12), at.z + rnd(level, 0.12),
                            trail.x + rnd(level, 0.02), trail.y + 0.02, trail.z + rnd(level, 0.02));
                }
                if (level.random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, trail.x * 0.6, 0.04, trail.z * 0.6);
                }
            }
        }
    }

    private static double rnd(ClientLevel level, double spread) {
        return (level.random.nextDouble() - 0.5) * 2.0 * spread;
    }

    // ---- the Nova Flame ----

    /** The Nova Flame's light for this player this frame: gathering, then the blast. */
    static void nova(Minecraft mc, AbstractClientPlayer player, PowerData data, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                     float partialTick, float time) {
        float[] seen = NOVA.computeIfAbsent(player.getId(), k -> new float[]{-1, -1});
        boolean gathering = data.is(Live.JIM_NOVA_CHARGING);
        boolean blast = data.is(Live.JIM_NOVA_BLAST);
        if (!gathering) {
            seen[0] = -1;
        }
        if (!blast) {
            seen[1] = -1;
        }
        Vec3 centre = player.getPosition(partialTick).add(0, player.getBbHeight() * 0.55, 0);
        Vec3 a = centre.subtract(camera);
        if (gathering) {
            if (seen[0] < 0) {
                seen[0] = time;
            }
            float full = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_CHARGE_TICKS.get() : 60;
            float t = Mth.clamp((time - seen[0]) / full, 0.0F, 1.0F);
            ChargedBeamFx.gatherAround(pose, vc, a, new Vec3(0, 1, 0), ChargedBeamFx.FIRE, t, time, 1.6F);
        }
        if (blast) {
            if (seen[1] < 0) {
                seen[1] = time;
            }
            float t = Mth.clamp((time - seen[1]) / JimAbilitiesTicks.BLAST, 0.0F, 1.0F);
            float radius = (EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_RADIUS.get().floatValue() : 12.0F) * (1.0F - (1.0F - t) * (1.0F - t));
            float fade = 1.0F - t;
            // three shells of fire racing out: a flat ring along the ground, and two upright
            int glow = ChargedBeamFx.FIRE.glow();
            int core = ChargedBeamFx.FIRE.core();
            EnergyGeometry.ring(pose, vc, a, new Vec3(0, 1, 0), radius, 0.35F + 0.5F * fade, 64, EnergyGeometry.argb(glow, 0.55F * fade));
            EnergyGeometry.ring(pose, vc, a, new Vec3(0, 1, 0), radius * 0.8F, 0.2F, 64, EnergyGeometry.argb(core, 0.7F * fade));
            EnergyGeometry.ring(pose, vc, a, new Vec3(1, 0, 0), radius * 0.9F, 0.25F, 64, EnergyGeometry.argb(glow, 0.4F * fade));
            EnergyGeometry.ring(pose, vc, a, new Vec3(0, 0, 1), radius * 0.9F, 0.25F, 64, EnergyGeometry.argb(glow, 0.4F * fade));
            ChargedBeamFx.flareAt(pose, vc, a, (1.0F - t) * 3.0F + 0.3F, ChargedBeamFx.FIRE);
        }
    }

    /** The length of the blast's picture in ticks (the server's {@code JimAbilities.NOVA_BLAST_TICKS}). */
    private static final class JimAbilitiesTicks {
        static final float BLAST = 16.0F;
    }
}
