package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The Road's light and sound: blue-green energy, amethyst chimes (all sounds are ones the game and the mod already have). */
public final class RoadFx {

    /** A column of the Road's light where somebody goes down into it ({@code rising} false) or comes up out of it. */
    public static void column(ServerLevel level, Vec3 at, boolean rising) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 40; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.2 + random.nextDouble() * 0.5;
            int color = i % 3 == 0 ? WitchesRoad.LIGHT_CORE : WitchesRoad.LIGHT;
            level.sendParticles(new EnergyParticleOptions(color, 1.4F),
                    at.x + Math.cos(angle) * radius, at.y + random.nextDouble() * 1.8, at.z + Math.sin(angle) * radius,
                    0, 0, rising ? 1 : -1, 0, 0.05 + random.nextDouble() * 0.15);
        }
    }

    /** Going in or coming out: the column of light and the sound of the Road. */
    public static void travel(ServerLevel level, Vec3 at, boolean arriving) {
        column(level, at, arriving);
        level.playSound(null, at.x, at.y, at.z, ModSounds.ROAD_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, arriving ? 1.1F : 0.9F);
    }

    /** A checkpoint is reached: a chime, and light round the player. */
    public static void checkpoint(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        sparkle(level, at, 24);
    }

    /** The player is put back after a fall: a small flash where he lands. */
    public static void putBack(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        sparkle(level, at, 14);
    }

    /** A trial is won: a chord of chimes. */
    public static void won(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, ModSounds.WITCHES_ROAD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        for (int i = 0; i < 3; i++) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F + i * 0.25F);
        }
        sparkle(level, at, 60);
    }

    public static void sparkle(ServerLevel level, Vec3 at, int count) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            int color = i % 3 == 0 ? WitchesRoad.LIGHT_CORE : WitchesRoad.LIGHT;
            level.sendParticles(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.5F),
                    at.x + (random.nextDouble() - 0.5) * 1.6, at.y + 0.1 + random.nextDouble() * 1.8, at.z + (random.nextDouble() - 0.5) * 1.6,
                    0, 0, 1, 0, 0.03 + random.nextDouble() * 0.08);
        }
    }

    /** The opening of an open gate shimmers. */
    public static void gate(ServerLevel level, AABB zone) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            level.sendParticles(new EnergyParticleOptions(i == 0 ? WitchesRoad.LIGHT_CORE : WitchesRoad.LIGHT, 1.2F),
                    zone.minX + random.nextDouble() * zone.getXsize(), zone.minY + random.nextDouble() * zone.getYsize(),
                    zone.minZ + random.nextDouble() * zone.getZsize(), 0, 0, 1, 0, 0.02);
        }
        if (random.nextInt(6) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, zone.getCenter().x, zone.getCenter().y, zone.getCenter().z, 1, 0.5, 0.8, 0.5, 0.01);
        }
    }

    private RoadFx() {
    }
}
