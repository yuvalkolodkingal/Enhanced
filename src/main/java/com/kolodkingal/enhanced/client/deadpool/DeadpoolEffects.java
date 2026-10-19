package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.DeadpoolActing;
import com.kolodkingal.enhanced.deadpool.DeadpoolLines;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * What every client draws of Deadpool's effects (3.7, the {@link DeadpoolFxPayload} kinds): each one first plays its
 * gesture on him ({@link DeadpoolActing#onPayload}), then its flourish: a silver sweep where a cut lands, sparks where
 * the Parry turns something away, a gold muzzle flash, a ricochet's sparks, the red-and-black flicker of the Teleport
 * Belt (and its glitch's scatter), crumbs of a chimichanga; a line becomes a bubble over his head ({@link LineBubbles})
 * and, on his own screen, the action bar. Registered from the client setup.
 */
public final class DeadpoolEffects {
    private static final int SILVER = 0xE6EAF0;
    private static final int RED = 0xC8102E;
    private static final int DARK = 0x2A2730;

    public static void register() {
        for (int kind = DeadpoolFxPayload.SLASH; kind <= DeadpoolFxPayload.EAT; kind++) {
            DeadpoolFx.register(kind, DeadpoolEffects::draw);
        }
    }

    static void draw(DeadpoolFxPayload payload) {
        DeadpoolActing.onPayload(payload);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Entity who = level.getEntity(payload.entityId());
        List<Vec3> points = payload.points();
        RandomSource r = level.random;
        switch (payload.kind()) {
            case DeadpoolFxPayload.SLASH -> {
                if (who != null) {
                    Vec3 front = who.getEyePosition().add(who.getLookAngle().scale(1.6)).add(0.0, -0.3, 0.0);
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 0.0, 0.0, 0.0);
                }
                for (Vec3 p : points) {
                    burst(level, r, p, ParticleTypes.CRIT, 8, 0.25, 0.3);
                    spark(level, r, p, SILVER, 6, 0.2);
                }
            }
            case DeadpoolFxPayload.PARRY_ON, DeadpoolFxPayload.PARRY_OFF -> {
                if (who != null) {
                    spark(level, r, who.getEyePosition().add(who.getLookAngle().scale(0.8)), SILVER, 6, 0.2);
                }
            }
            case DeadpoolFxPayload.DEFLECT -> {
                for (Vec3 p : points) {
                    burst(level, r, p, ParticleTypes.ELECTRIC_SPARK, 10, 0.15, 0.4);
                    spark(level, r, p, SILVER, 8, 0.15);
                }
            }
            case DeadpoolFxPayload.SHOT -> {
                if (!points.isEmpty()) {
                    Vec3 muzzle = points.get(0);
                    Vec3 way = points.size() > 1 ? points.get(1).subtract(muzzle).normalize() : Vec3.ZERO;
                    Vec3 flash = muzzle.add(way.scale(0.35));
                    spark(level, r, flash, 0xFFE27A, payload.arg() == 2 ? 10 : 6, 0.06);
                    level.addParticle(ParticleTypes.SMOKE, flash.x, flash.y, flash.z, way.x * 0.05, 0.02,
                            way.z * 0.05);
                }
            }
            case DeadpoolFxPayload.RICOCHET -> {
                if (!points.isEmpty()) {
                    burst(level, r, points.get(0), ParticleTypes.ELECTRIC_SPARK, 12, 0.1, 0.5);
                    spark(level, r, points.get(0), 0xFFE27A, 6, 0.1);
                }
            }
            case DeadpoolFxPayload.BLINK_OUT, DeadpoolFxPayload.BLINK_IN, DeadpoolFxPayload.GLITCH -> {
                if (!points.isEmpty()) {
                    flicker(level, r, points.get(0), payload.kind() == DeadpoolFxPayload.GLITCH);
                }
            }
            case DeadpoolFxPayload.DROP_IN -> {
                if (!points.isEmpty()) {
                    flicker(level, r, points.get(0), false);
                }
            }
            case DeadpoolFxPayload.TAUNT -> {
                String key = DeadpoolLines.keyOf(payload.arg());
                if (key != null) {
                    Component line = Component.translatable(key);
                    LineBubbles.say(payload.entityId(), line);
                    if (mc.player != null && mc.player.getId() == payload.entityId()
                            && EnhancedClientConfig.deadpoolLines()) {
                        mc.gui.setOverlayMessage(line, false);
                    }
                }
            }
            case DeadpoolFxPayload.EAT -> {
                if (who != null && payload.arg() == 1) {
                    Vec3 mouth = who.getEyePosition().add(who.getLookAngle().scale(0.4)).add(0.0, -0.2, 0.0);
                    ItemParticleOption crumbs = new ItemParticleOption(ParticleTypes.ITEM,
                            new ItemStack(ModItems.CHIMICHANGA.get()));
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(crumbs, mouth.x, mouth.y, mouth.z, (r.nextDouble() - 0.5) * 0.12, 0.08,
                                (r.nextDouble() - 0.5) * 0.12);
                    }
                }
                if (who != null) {
                    spark(level, r, who.position().add(0.0, 1.0, 0.0), 0xA8F2B8, 10, 0.5);
                }
            }
            default -> {
            }
        }
    }

    /** The Teleport Belt's flicker: a column of red and black motes (a glitch scatters them and adds a cyan blink). */
    static void flicker(ClientLevel level, RandomSource r, Vec3 at, boolean glitch) {
        if (!EnhancedClientConfig.flightEffects()) {
            return;
        }
        for (int i = 0; i < 24; i++) {
            double y = r.nextDouble() * 1.9;
            double spread = glitch ? 0.9 : 0.4;
            int color = i % 2 == 0 ? RED : DARK;
            level.addParticle(new EnergyParticleOptions(color, 1.2F), at.x + (r.nextDouble() - 0.5) * spread,
                    at.y + y, at.z + (r.nextDouble() - 0.5) * spread, 0.0, 0.02, 0.0);
        }
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1.0, at.z, 0.0, 0.1, 0.0);
        if (glitch) {
            for (int i = 0; i < 10; i++) {
                level.addParticle(new DustParticleOptions(new Vector3f(0.3F, 0.95F, 1.0F), 1.0F),
                        at.x + (r.nextDouble() - 0.5) * 1.4, at.y + r.nextDouble() * 2.0,
                        at.z + (r.nextDouble() - 0.5) * 1.4, 0.0, 0.0, 0.0);
            }
        }
    }

    static void spark(ClientLevel level, RandomSource r, Vec3 at, int color, int count, double spread) {
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, 0.9F), at.x + (r.nextDouble() - 0.5) * spread,
                    at.y + (r.nextDouble() - 0.5) * spread, at.z + (r.nextDouble() - 0.5) * spread,
                    (r.nextDouble() - 0.5) * 0.1, (r.nextDouble() - 0.5) * 0.1, (r.nextDouble() - 0.5) * 0.1);
        }
    }

    static void burst(ClientLevel level, RandomSource r, Vec3 at,
                      net.minecraft.core.particles.SimpleParticleType type, int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            level.addParticle(type, at.x + (r.nextDouble() - 0.5) * spread, at.y + (r.nextDouble() - 0.5) * spread,
                    at.z + (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * speed,
                    (r.nextDouble() - 0.5) * speed, (r.nextDouble() - 0.5) * speed);
        }
    }

    private DeadpoolEffects() {
    }
}
