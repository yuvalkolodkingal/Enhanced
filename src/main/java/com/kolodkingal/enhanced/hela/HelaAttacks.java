package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The blades of Hela (3.5), as the boss and the hero both cast them: the fan of necroswords, the wall and the ring of
 * spikes, the rain. Each is made of entities ({@link NecroswordEntity}, {@link BladeSpikeEntity}) that hurt only what
 * their maker may hurt ({@link HelaRules#canHurt}), and each tells the clients ({@link HelaFx}) so that the cracks, the
 * circle and the gesture are seen.
 */
public final class HelaAttacks {
    /** The spikes of a wall stand this far apart. */
    public static final double WALL_STEP = 1.0;
    /** The extra delay of each spike after the one before it (the wall runs out from her). */
    public static final int WALL_RIPPLE = 1;

    // ---------------------------------------------------------------------------------------------------------
    // The fan

    /**
     * Where the {@code count} blades of a volley hang before they fly: in an arc behind and over the caster's shoulders
     * (her body turned by {@code yaw}), the middle one highest.
     */
    public static List<Vec3> fanPoints(Vec3 feet, float yaw, float height, int count) {
        List<Vec3> points = new ArrayList<>();
        double rad = Math.toRadians(yaw);
        Vec3 back = new Vec3(Math.sin(rad), 0.0, -Math.cos(rad)).scale(0.7);
        Vec3 right = new Vec3(-Math.cos(rad), 0.0, -Math.sin(rad));
        for (int i = 0; i < count; i++) {
            double u = count == 1 ? 0.0 : (i / (count - 1.0)) * 2.0 - 1.0;
            double side = u * (0.6 + 0.25 * count);
            double up = height * 0.92 + 0.45 * (1.0 - u * u);
            points.add(feet.add(back).add(right.scale(side)).add(0.0, up, 0.0));
        }
        return points;
    }

    /**
     * The volley: one blade from each point, flying at {@code target} (each aimed a little apart, so the fan spreads by
     * {@code spreadDegrees} between neighbours over its length).
     */
    public static List<NecroswordEntity> volley(ServerLevel level, LivingEntity owner, List<Vec3> from, Vec3 target, float spreadDegrees,
                                                float damage) {
        List<NecroswordEntity> blades = new ArrayList<>();
        int count = from.size();
        for (int i = 0; i < count; i++) {
            Vec3 start = from.get(i);
            Vec3 dir = target.subtract(start);
            if (dir.lengthSqr() < 1.0E-4) {
                dir = owner.getLookAngle();
            }
            double u = count == 1 ? 0.0 : i - (count - 1) / 2.0;
            dir = dir.normalize().yRot((float) Math.toRadians(-u * spreadDegrees));
            NecroswordEntity blade = NecroswordEntity.throwFrom(level, owner, start, dir, damage);
            level.addFreshEntity(blade);
            blades.add(blade);
        }
        level.playSound(null, owner.getX(), owner.getY() + 1.5, owner.getZ(), ModSounds.NECROSWORD_THROW.get(), SoundSource.HOSTILE, 1.3F,
                0.95F + level.random.nextFloat() * 0.1F);
        return blades;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The wall and the ring

    /**
     * The feet of a wall's spikes: from {@code start} (a step ahead of the caster) along {@code direction} (flat),
     * {@code length} of them a block apart, each set on the ground of its column; the wall stops at a wall of the world
     * (a block in the way at the height of the spike).
     */
    public static List<Vec3> wallPoints(Level level, Vec3 start, Vec3 direction, int length) {
        List<Vec3> points = new ArrayList<>();
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return points;
        }
        flat = flat.normalize();
        Vec3 last = start;
        for (int i = 0; i < length; i++) {
            Vec3 at = start.add(flat.scale(1.0 + i * WALL_STEP));
            BlockHitResult hit = level.clip(new ClipContext(last.add(0, 0.6, 0), at.add(0, 0.6, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (hit.getType() != HitResult.Type.MISS) {
                break;
            }
            Vec3 feet = HelaRules.onGround(level, at.add(0.0, 0.6, 0.0));
            if (Math.abs(feet.y - start.y) > 3.0) {
                break; // (off a ledge, or into a pit: the wall runs along the floor she stands on)
            }
            points.add(feet);
            last = at;
        }
        return points;
    }

    /** The wall: a spike at each point, the first rising after {@code delay}, each next {@link #WALL_RIPPLE} later. */
    public static List<BladeSpikeEntity> wall(ServerLevel level, LivingEntity owner, List<Vec3> points, int delay, int stand, float damage) {
        List<BladeSpikeEntity> spikes = new ArrayList<>();
        Vec3 first = points.isEmpty() ? owner.position() : points.get(0);
        float yaw = points.size() < 2 ? owner.getYRot()
                : (float) Math.toDegrees(Math.atan2(-(points.get(1).x - first.x), points.get(1).z - first.z));
        for (int i = 0; i < points.size(); i++) {
            Vec3 at = points.get(i);
            float tilt = (i % 2 == 0 ? 1 : -1) * 9.0F;
            BladeSpikeEntity spike = BladeSpikeEntity.at(level, owner, at, yaw + tilt, delay + i * WALL_RIPPLE, stand, damage, 0);
            level.addFreshEntity(spike);
            spikes.add(spike);
        }
        HelaFx.send(level, HelaFx.WALL, owner.getId(), points, delay);
        level.playSound(null, first.x, first.y, first.z, ModSounds.BLADE_WALL.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        return spikes;
    }

    /** The feet of {@code count} spikes in a ring of {@code radius} round {@code centre}, each on its ground. */
    public static List<Vec3> ringPoints(Level level, Vec3 centre, double radius, int count) {
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = Mth.TWO_PI * i / count;
            points.add(HelaRules.onGround(level, centre.add(Math.cos(a) * radius, 0.6, Math.sin(a) * radius)));
        }
        return points;
    }

    /** The Blade Burst: a ring of spikes round the caster, all rising together after {@code delay}. */
    public static List<BladeSpikeEntity> ring(ServerLevel level, LivingEntity owner, double radius, int count, int delay, int stand, float damage) {
        List<BladeSpikeEntity> spikes = new ArrayList<>();
        Vec3 centre = owner.position();
        for (Vec3 at : ringPoints(level, centre, radius, count)) {
            float yaw = (float) Math.toDegrees(Math.atan2(-(at.x - centre.x), at.z - centre.z));
            BladeSpikeEntity spike = BladeSpikeEntity.at(level, owner, at, yaw, delay, stand, damage, 1);
            level.addFreshEntity(spike);
            spikes.add(spike);
        }
        HelaFx.send(level, HelaFx.BURST, owner.getId(), centre, (int) Math.round(radius * 10.0));
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.BLADE_WALL.get(), SoundSource.HOSTILE, 1.4F, 1.15F);
        return spikes;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The rain

    /**
     * The Blade Rain: {@code count} blades hang over a circle of {@code radius} round {@code centre} for {@code hang}
     * ticks (the circle burns green on the ground), then fall: one in the middle, the rest on two rings.
     */
    public static List<NecroswordEntity> rain(ServerLevel level, LivingEntity owner, Vec3 centre, double radius, int count, int hang, float damage) {
        List<NecroswordEntity> blades = new ArrayList<>();
        Vec3 ground = HelaRules.onGround(level, centre.add(0.0, 0.6, 0.0));
        for (int i = 0; i < count; i++) {
            Vec3 mark;
            if (i == 0) {
                mark = ground;
            } else {
                boolean outer = i % 2 == 0;
                double r = radius * (outer ? 0.85 : 0.45);
                double a = Mth.TWO_PI * i / (count - 1) + (outer ? 0.3 : 0.0);
                mark = ground.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
            }
            NecroswordEntity blade = NecroswordEntity.rainAt(level, owner, mark, hang + (i % 3), damage);
            level.addFreshEntity(blade);
            blades.add(blade);
        }
        HelaFx.send(level, HelaFx.RAIN, owner.getId(), ground, (int) Math.round(radius * 10.0));
        level.playSound(null, ground.x, ground.y, ground.z, ModSounds.BLADE_RAIN.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        return blades;
    }

    private HelaAttacks() {
    }
}
