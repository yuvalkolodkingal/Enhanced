package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.sokovia.SokoviaClient;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronState;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The particles of the Ultron fight: what each effect of {@link UltronEffects} sheds while it runs (the scrap that
 * spirals into his silhouette, the motes of the Pull's ring, the dust of a shockwave, the smoke of a wreck) and what the
 * bodies themselves give off by their state (the thrust under a Dive, the steam of a stagger, the vortex round a raised
 * fist, a Sentry's thrusters, the dust of the Key rising and the column of blue motes from it, the sparkle on the rim of
 * Sokovia). All of it is the mod's one particle ({@link EnergyParticleOptions}) and vanilla ones; the counts are capped
 * and thinned when the client's flight effects are off.
 */
final class UltronParticles {
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();

    /** How much of the usual particles is wanted: all of it, or half with the client's flight effects off. */
    private static float density() {
        return EnhancedClientConfig.flightEffects() ? 1.0F : 0.5F;
    }

    private static boolean chance(RandomSource random, float probability) {
        return random.nextFloat() < probability * density();
    }

    private static void mote(ClientLevel level, int colour, float size, double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(new EnergyParticleOptions(colour, size), x, y, z, vx, vy, vz);
    }

    private static void mote(ClientLevel level, int colour, float size, Vec3 at, double vx, double vy, double vz) {
        mote(level, colour, size, at.x, at.y, at.z, vx, vy, vz);
    }

    /** What the effect sheds this tick (and, the first time it is seen, its one burst). */
    static void tick(ClientLevel level, UltronEffects.Effect e, long now) {
        RandomSource random = level.random;
        float age = (now - e.start);
        float u = Mth.clamp(age / Math.max(1, e.ticks), 0.0F, 1.0F);
        boolean first = !e.burst;
        e.burst = true;
        switch (e.kind) {
            case UltronFxPayload.ASSEMBLE -> assemble(level, e, u, random);
            case UltronFxPayload.FLASH -> {
                if (first) {
                    int count = (int) ((8 + e.radius * 2) * density());
                    for (int i = 0; i < count; i++) {
                        Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6, random.nextGaussian()).normalize();
                        level.addParticle(ParticleTypes.END_ROD, e.at.x, e.at.y, e.at.z, dir.x * 0.25, dir.y * 0.25, dir.z * 0.25);
                    }
                }
            }
            case UltronFxPayload.BEAMS_FIRE -> beamEnds(level, e, random);
            case UltronFxPayload.DEBRIS_RING -> {
                if (age >= e.ticks - 12) {
                    for (int i = 0; i < 2; i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double r = Math.sqrt(random.nextDouble()) * e.radius;
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), e.at.x + Math.cos(angle) * r,
                                e.at.y + 11.0, e.at.z + Math.sin(angle) * r, 0.0, -1.1, 0.0);
                    }
                }
            }
            case UltronFxPayload.PULL_RING -> pullRing(level, e, u, random);
            case UltronFxPayload.SPAWN_PILLAR -> spawnPillar(level, e, u, age, random);
            case UltronFxPayload.SHOCKWAVE -> {
                if (first) {
                    dustRing(level, e.at, e.radius * 0.7F, 20, 0.28);
                }
            }
            case UltronFxPayload.WRECK -> wreck(level, e, age, random);
            case UltronFxPayload.LIFT_RING -> liftRing(level, e, u, random);
            case UltronFxPayload.TREMOR -> tremor(level, e, random);
            case UltronFxPayload.DEATH -> death(level, e, u, random);
            case UltronFxPayload.PULSE -> {
                if (first) {
                    Entity body = e.entityId < 0 ? null : level.getEntity(e.entityId);
                    Vec3 chest = body == null ? e.at.add(0, 1.5, 0) : UltronEffects.chestOf(body, 0.0F);
                    for (int i = 0; i < 8 * density(); i++) {
                        mote(level, UltronEffects.c(UltronEffects.PLASMA_EDGE), 1.0F, chest, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12);
                    }
                }
            }
            case UltronFxPayload.GRAVITY_SLAM -> gravitySlam(level, e, u, first, random);
            case UltronFxPayload.OVERCHARGE_FLASH -> {
                if (first) {
                    Vec3 dir = e.to.normalize();
                    for (int i = 0; i < 14 * density(); i++) {
                        Vec3 v = dir.add(random.nextGaussian() * 0.18, random.nextGaussian() * 0.18, random.nextGaussian() * 0.18).scale(0.5 + random.nextDouble() * 0.6);
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.at.x, e.at.y, e.at.z, v.x, v.y, v.z);
                    }
                    for (int i = 0; i < 8 * density(); i++) {
                        Vec3 v = dir.scale(0.4 + random.nextDouble() * 0.3);
                        mote(level, UltronEffects.c(UltronEffects.PLASMA_GOLD), 1.3F, e.at, v.x, v.y, v.z);
                    }
                }
            }
            case UltronFxPayload.NETWORK_FLASH -> {
                if (first) {
                    for (int i = 0; i < 6 * density(); i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.at.x, e.at.y, e.at.z, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15);
                    }
                }
            }
            case UltronFxPayload.DRONE_ASSEMBLE -> droneAssemble(level, e, u, random);
            case UltronFxPayload.HACK_BEAM -> {
                if (first) {
                    // The Sentry it reaches for flickers blue, white and red until the hack is done.
                    Entity nearest = null;
                    double best = 9.0;
                    for (Entity candidate : level.entitiesForRendering()) {
                        if (candidate instanceof UltronSentryEntity && candidate.position().add(0, candidate.getBbHeight() * 0.5, 0).distanceToSqr(e.to) < best) {
                            best = candidate.position().add(0, candidate.getBbHeight() * 0.5, 0).distanceToSqr(e.to);
                            nearest = candidate;
                        }
                    }
                    if (nearest != null) {
                        UltronAnimation.flicker(nearest.getId(), now + e.ticks);
                    }
                }
            }
            default -> {
            }
        }
    }

    // ---- what one effect sheds ----

    /** Scrap iron, iron blocks and deepslate chips spiral in and up to the silhouette. */
    private static void assemble(ClientLevel level, UltronEffects.Effect e, float u, RandomSource random) {
        int count = (int) (7 * density());
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI + u * 12.0;
            double r = 2.8 * (1.0 - u) + 0.5 + random.nextDouble() * 0.6;
            double height = random.nextDouble() * e.radius * Math.min(1.0, u * 1.3 + 0.1);
            double x = e.at.x + Math.cos(angle) * r;
            double z = e.at.z + Math.sin(angle) * r;
            // Toward the axis and round it: they are drawn in.
            double vx = -Math.cos(angle) * 0.14 - Math.sin(angle) * 0.09;
            double vz = -Math.sin(angle) * 0.14 + Math.cos(angle) * 0.09;
            ParticleOptions scrap = switch (random.nextInt(3)) {
                case 0 -> new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET));
                case 1 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState());
                default -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
            };
            level.addParticle(scrap, x, e.at.y + 0.2 + height, z, vx, 0.05, vz);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.at.x + random.nextGaussian() * 0.5, e.at.y + random.nextDouble() * e.radius * u, e.at.z + random.nextGaussian() * 0.5, 0, 0.05, 0);
        }
    }

    /** Sparks where the beams end: on the first block they meet. */
    private static void beamEnds(ClientLevel level, UltronEffects.Effect e, RandomSource random) {
        if (random.nextInt(2) != 0) {
            return;
        }
        Entity boss = level.getEntity(e.entityId);
        if (boss == null) {
            return;
        }
        Vec3 origin = boss.position().add(0, boss.getBbHeight() * 0.62, 0);
        Vec3 direction = e.at.subtract(origin).normalize();
        // The middle of the fan ends on the locked point or the first block short of it.
        Vec3 end = e.at;
        for (int i = 0; i < 3 * density(); i++) {
            double fan = Math.toRadians(random.nextDouble() * 2.0 * UltronAttack.BEAM_FAN_DEGREES - UltronAttack.BEAM_FAN_DEGREES);
            Vec3 point = origin.add(UltronEffects.rotateY(direction, fan).scale(origin.distanceTo(end)));
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, random.nextGaussian() * 0.1, 0.1 + random.nextDouble() * 0.15, random.nextGaussian() * 0.1);
            mote(level, UltronEffects.c(UltronEffects.PLASMA_MAIN), 1.0F, point, random.nextGaussian() * 0.06, 0.05, random.nextGaussian() * 0.06);
        }
    }

    /** A ring of motes, drawn in as the Pull's ring contracts. */
    private static void pullRing(ClientLevel level, UltronEffects.Effect e, float u, RandomSource random) {
        float eased = u * u * (3.0F - 2.0F * u);
        float r = Mth.lerp(eased, e.radius, 3.0F);
        int count = (int) (14 * density());
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double x = e.at.x + Math.cos(angle) * r;
            double z = e.at.z + Math.sin(angle) * r;
            double pull = 0.06 + 0.08 * u;
            mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.0F, x, e.at.y + 0.15, z, -Math.cos(angle) * pull - Math.sin(angle) * 0.04, 0.02, -Math.sin(angle) * pull + Math.cos(angle) * 0.04);
        }
    }

    /** Light climbing the pillar, and at the end the dirt that the Sentry's emerging throws up. */
    private static void spawnPillar(ClientLevel level, UltronEffects.Effect e, float u, float age, RandomSource random) {
        if (chance(random, 1.0F)) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.1F, e.at.x + Math.cos(angle) * 0.4, e.at.y + 0.1, e.at.z + Math.sin(angle) * 0.4, 0.0, 0.18 + random.nextDouble() * 0.2, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.at.x, e.at.y + random.nextDouble() * 3.0, e.at.z, random.nextGaussian() * 0.1, 0.1, random.nextGaussian() * 0.1);
        }
        if (age >= e.ticks - 1) {
            BlockState ground = groundState(level, e.at);
            for (int i = 0; i < 12 * density(); i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), e.at.x, e.at.y + 0.2, e.at.z, random.nextGaussian() * 0.15, 0.25 + random.nextDouble() * 0.25, random.nextGaussian() * 0.15);
            }
        }
    }

    /** A ring of the ground's own dust flung out from a point. */
    private static void dustRing(ClientLevel level, Vec3 at, float radius, int count, double speed) {
        RandomSource random = level.random;
        BlockState ground = groundState(level, at);
        for (int i = 0; i < count * density(); i++) {
            double angle = Mth.TWO_PI * i / Math.max(1, count * density()) + random.nextDouble() * 0.2;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + Math.cos(angle) * radius * 0.3, at.y + 0.15, at.z + Math.sin(angle) * radius * 0.3,
                    Math.cos(angle) * speed, 0.12 + random.nextDouble() * 0.1, Math.sin(angle) * speed);
        }
        for (int i = 0; i < 4 * density(); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            level.addParticle(ParticleTypes.POOF, at.x + Math.cos(angle) * radius * 0.3, at.y + 0.2, at.z + Math.sin(angle) * radius * 0.3, Math.cos(angle) * speed * 0.6, 0.02, Math.sin(angle) * speed * 0.6);
        }
    }

    /** The block under a point, or dirt where there is none to show. */
    private static BlockState groundState(ClientLevel level, Vec3 at) {
        BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        return state.isAir() || state.liquid() ? DIRT : state;
    }

    /** Smoke and the odd spark off the wreck of a body. */
    private static void wreck(ClientLevel level, UltronEffects.Effect e, float age, RandomSource random) {
        if (((int) age) % 3 == 0 && chance(random, 1.0F)) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, e.at.x + random.nextGaussian() * 0.5, e.at.y + 0.4, e.at.z + random.nextGaussian() * 0.5, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(9) == 0 && age < e.ticks - 40) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.at.x + random.nextGaussian() * 0.6, e.at.y + 0.3, e.at.z + random.nextGaussian() * 0.6, random.nextGaussian() * 0.1, 0.12, random.nextGaussian() * 0.1);
        }
    }

    /** Motes rising from the ring of the warning. */
    private static void liftRing(ClientLevel level, UltronEffects.Effect e, float u, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        int count = (int) (5 * density());
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double x = e.at.x + Math.cos(angle) * e.radius;
            double z = e.at.z + Math.sin(angle) * e.radius;
            if (mc.player != null && mc.player.distanceToSqr(x, mc.player.getY(), z) > 40.0 * 40.0) {
                continue;
            }
            mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.0F, x, e.at.y + 0.2, z, 0.0, 0.3 + 0.5 * u * random.nextDouble(), 0.0);
        }
    }

    /** Pieces of the ground shaken loose across the arena. */
    private static void tremor(ClientLevel level, UltronEffects.Effect e, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 3 * density(); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = Math.sqrt(random.nextDouble()) * e.radius;
            double x = e.at.x + Math.cos(angle) * r;
            double z = e.at.z + Math.sin(angle) * r;
            if (mc.player != null && mc.player.distanceToSqr(x, mc.player.getY(), z) > 36.0 * 36.0) {
                continue;
            }
            Vec3 at = new Vec3(x, e.at.y, z);
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, groundState(level, at)), x, e.at.y + 0.2, z, random.nextGaussian() * 0.05, 0.08 + random.nextDouble() * 0.1, random.nextGaussian() * 0.05);
        }
    }

    /** The last body: sparks round it, smoke from the chest, and toward the end motes of light. */
    private static void death(ClientLevel level, UltronEffects.Effect e, float u, RandomSource random) {
        Entity body = level.getEntity(e.entityId);
        Vec3 base = body == null ? e.at : body.position();
        double height = body == null ? 3.0 : body.getBbHeight();
        for (int i = 0; i < 3 * density(); i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, base.x + random.nextGaussian() * 0.6, base.y + random.nextDouble() * height, base.z + random.nextGaussian() * 0.6,
                    random.nextGaussian() * 0.12, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.x + random.nextGaussian() * 0.4, base.y + height * 0.62, base.z + random.nextGaussian() * 0.4, 0.0, 0.05, 0.0);
        }
        if (u > 0.4F) {
            for (int i = 0; i < 2 * density(); i++) {
                mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.3F, base.x, base.y + height * 0.62, base.z, random.nextGaussian() * 0.25, random.nextGaussian() * 0.25, random.nextGaussian() * 0.25);
            }
        }
    }

    /** The slam's wave front: motes at the edge of the ring as it runs out, and a ring of the ground's dust at the start. */
    private static void gravitySlam(ClientLevel level, UltronEffects.Effect e, float u, boolean first, RandomSource random) {
        float eased = 1.0F - (1.0F - u) * (1.0F - u);
        float r = Math.max(0.4F, e.radius * eased);
        int count = (int) (16 * density());
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            mote(level, i % 3 == 0 ? UltronEffects.c(UltronEffects.GRAV_CORE) : UltronEffects.c(UltronEffects.GRAV_MAIN), 1.0F, e.at.x + Math.cos(angle) * r, e.at.y + 0.12, e.at.z + Math.sin(angle) * r,
                    Math.cos(angle) * 0.1, 0.03, Math.sin(angle) * 0.1);
        }
        if (first) {
            dustRing(level, e.at, e.radius * 0.8F, 24, 0.32);
            for (int i = 0; i < 6 * density(); i++) {
                level.addParticle(ParticleTypes.POOF, e.at.x, e.at.y + 0.3, e.at.z, random.nextGaussian() * 0.1, 0.1, random.nextGaussian() * 0.1);
            }
        }
    }

    /** Sparks gather into the shape of a Sentry. */
    private static void droneAssemble(ClientLevel level, UltronEffects.Effect e, float u, RandomSource random) {
        int colour = UltronEffects.blend(0x45B4F5, UltronEffects.c(UltronEffects.NET_RED), u);
        for (int i = 0; i < 3 * density(); i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6, random.nextGaussian()).normalize().scale(1.5);
            Vec3 from = e.at.add(0, 0.85, 0).add(dir);
            mote(level, colour, 1.0F, from, -dir.x * 0.13, -dir.y * 0.13, -dir.z * 0.13);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the bodies give off

    /** Every body in sight: what its state sheds, and the key and the island. */
    static void tickBodies(ClientLevel level, long now) {
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = level.random;
        for (Entity entity : level.entitiesForRendering()) {
            if (mc.player != null && entity.distanceToSqr(mc.player) > 96.0 * 96.0) {
                continue;
            }
            if (entity instanceof UltronEntity boss) {
                boss(level, boss, now, random);
            } else if (entity instanceof UltronSentryEntity sentry) {
                sentry(level, sentry, now, random);
            } else if (entity instanceof VibraniumKeyEntity key) {
                key(level, key, now, random);
            }
        }
        island(level, random);
    }

    private static void boss(ClientLevel level, UltronEntity boss, long now, RandomSource random) {
        float partial = 0.0F;
        float t = boss.stateAge(partial);
        double scale = boss.body().hitboxScale();
        Vec3 feet = boss.position();
        double height = boss.getBbHeight();
        int tick = (int) t;
        // The anti-gravity palms and soles, always: a faint pale-blue motes under his feet.
        if (boss.tickCount % 3 == 0 && chance(random, 1.0F) && boss.state() != UltronState.AWAKENING) {
            mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 0.8F, feet.x + (random.nextDouble() - 0.5) * 0.9 * scale, feet.y + 0.1, feet.z + (random.nextDouble() - 0.5) * 0.9 * scale, 0.0, -0.03, 0.0);
        }
        switch (boss.state()) {
            case VOLLEY -> {
                // The muzzle of each burst: amber motes leave both palms on the tick the bolts do.
                int bursts = boss.bodyIndex() >= 2 ? 1 : 3;
                for (int b = 0; b < bursts; b++) {
                    if (tick == UltronAttack.VOLLEY.tell() + UltronAttack.BURST_GAP * b) {
                        for (int side = 0; side < 2; side++) {
                            Vec3 palm = UltronTracker.palm(boss, side == 0, 0.0F);
                            if (palm != null) {
                                for (int i = 0; i < 5 * density(); i++) {
                                    mote(level, i % 2 == 0 ? UltronEffects.c(UltronEffects.PLASMA_GOLD) : UltronEffects.c(UltronEffects.PLASMA_MAIN), 1.1F, palm, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
                                }
                            }
                        }
                    }
                }
            }
            case DIVE -> {
                // The thrust under him while he rises, hangs and dives; dust off the ground if it is close.
                if (t < UltronAttack.DIVE.tell() + 12) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int i = 0; i < 2 * density(); i++) {
                            mote(level, i == 0 ? UltronEffects.c(UltronEffects.GRAV_CORE) : UltronEffects.c(UltronEffects.GRAV_MAIN), 1.4F, feet.x + side * 0.4 * scale, feet.y + 0.1, feet.z, random.nextGaussian() * 0.03, -0.25 - random.nextDouble() * 0.1, random.nextGaussian() * 0.03);
                        }
                    }
                    double ground = feet.y;
                    for (int d = 1; d <= 5; d++) {
                        if (!level.getBlockState(BlockPos.containing(feet.x, feet.y - d + 0.1, feet.z)).isAir()) {
                            ground = feet.y - d + 1.0;
                            break;
                        }
                    }
                    if (ground < feet.y - 0.0 && feet.y - ground < 5.0) {
                        BlockState state = groundState(level, new Vec3(feet.x, ground, feet.z));
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), feet.x + random.nextGaussian() * 0.8, ground + 0.1, feet.z + random.nextGaussian() * 0.8, random.nextGaussian() * 0.15, 0.05, random.nextGaussian() * 0.15);
                    }
                }
            }
            case STAGGER -> {
                // Steam from the shoulders, a spark from a joint.
                for (int side = -1; side <= 1; side += 2) {
                    if (chance(random, 0.7F)) {
                        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, feet.x + side * 0.8 * scale, feet.y + height * 0.72, feet.z, 0.0, 0.05, 0.0);
                    }
                }
                if (random.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x + random.nextGaussian() * 0.5, feet.y + random.nextDouble() * height, feet.z + random.nextGaussian() * 0.5, 0, 0.05, 0);
                }
            }
            case PULL -> {
                // The vortex round the raised fist.
                Vec3 fist = UltronTracker.palm(boss, true, partial);
                if (fist != null && t > 3.0F && t < UltronAttack.PULL_PUNCH) {
                    for (int i = 0; i < 4 * density(); i++) {
                        double angle = boss.tickCount * 0.7 + i * 1.57;
                        double r = 0.55 + 0.25 * Math.sin(boss.tickCount * 0.3 + i);
                        Vec3 at = fist.add(Math.cos(angle) * r, (i % 3) * 0.12 - 0.1, Math.sin(angle) * r);
                        mote(level, i % 2 == 0 ? UltronEffects.c(UltronEffects.GRAV_CORE) : UltronEffects.c(UltronEffects.GRAV_MAIN), 1.0F, at, -Math.sin(angle) * 0.14, 0.03, Math.cos(angle) * 0.14);
                    }
                }
                if (tick == UltronAttack.PULL_PUNCH) {
                    dustRing(level, new Vec3(feet.x, groundBelow(level, feet), feet.z), 4.0F, 14, 0.2);
                }
            }
            case CALL -> {
                // Red and white motes rise from his chest as the Core pulses.
                if (chance(random, 1.0F)) {
                    Vec3 chest = UltronEffects.chestOf(boss, partial);
                    mote(level, random.nextBoolean() ? UltronEffects.c(UltronEffects.NET_RED) : UltronEffects.c(UltronEffects.NET_WHITE), 1.0F, chest, random.nextGaussian() * 0.04, 0.1, random.nextGaussian() * 0.04);
                }
            }
            case LIFT_CHANNEL -> {
                for (int side = 0; side < 2; side++) {
                    Vec3 palm = UltronTracker.palm(boss, side == 0, partial);
                    if (palm != null && chance(random, 1.0F)) {
                        double angle = boss.tickCount * 0.5 + side * Math.PI;
                        mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.2F, palm.add(Math.cos(angle) * 0.3, 0, Math.sin(angle) * 0.3), 0.0, 0.18, 0.0);
                    }
                }
            }
            case TRANSFER, DEATH -> {
                // The plates shudder and spit sparks; red and white data motes come off him.
                for (int i = 0; i < 2 * density(); i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x + random.nextGaussian() * 0.6 * scale, feet.y + random.nextDouble() * height, feet.z + random.nextGaussian() * 0.6 * scale,
                            random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1);
                }
                if (chance(random, 1.0F)) {
                    mote(level, random.nextBoolean() ? UltronEffects.c(UltronEffects.NET_RED) : UltronEffects.c(UltronEffects.NET_WHITE), 0.9F, feet.x + random.nextGaussian() * 0.5, feet.y + random.nextDouble() * height, feet.z + random.nextGaussian() * 0.5, 0.0, 0.05, 0.0);
                }
            }
            case PUNCH -> {
                if (tick == UltronAttack.PUNCH.tell()) {
                    Vec3 fist = UltronTracker.palm(boss, true, partial);
                    if (fist != null) {
                        for (int i = 0; i < 6 * density(); i++) {
                            level.addParticle(ParticleTypes.ELECTRIC_SPARK, fist.x, fist.y, fist.z, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2);
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    private static double groundBelow(ClientLevel level, Vec3 from) {
        for (int d = 0; d <= 8; d++) {
            if (!level.getBlockState(BlockPos.containing(from.x, from.y - d - 0.1, from.z)).isAir()) {
                return from.y - d;
            }
        }
        return from.y;
    }

    private static void sentry(ClientLevel level, UltronSentryEntity sentry, long now, RandomSource random) {
        Vec3 feet = sentry.position();
        UltronSentryEntity.Act act = sentry.act();
        // Thrusters under it, always, in a hot pale blue.
        if (act != UltronSentryEntity.Act.ASSEMBLING && act != UltronSentryEntity.Act.STUNNED && sentry.tickCount % 2 == 0 && chance(random, 1.0F)) {
            mote(level, 0xBFEFFF, 0.7F, feet.x + random.nextGaussian() * 0.1, feet.y + 0.15, feet.z + random.nextGaussian() * 0.1, 0.0, -0.12, 0.0);
        }
        if (act == UltronSentryEntity.Act.STUNNED && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x + random.nextGaussian() * 0.3, feet.y + random.nextDouble() * 1.6, feet.z + random.nextGaussian() * 0.3, 0.0, 0.05, 0.0);
        }
        if (act == UltronSentryEntity.Act.ASSEMBLING) {
            for (int i = 0; i < 2 * density(); i++) {
                Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian()).normalize().scale(1.2);
                Vec3 from = feet.add(0, 0.8, 0).add(dir);
                mote(level, 0x45B4F5, 0.9F, from, -dir.x * 0.12, -dir.y * 0.12, -dir.z * 0.12);
            }
        }
        if (sentry.isHacked() && sentry.tickCount < 12) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x + random.nextGaussian() * 0.3, feet.y + random.nextDouble() * 1.6, feet.z + random.nextGaussian() * 0.3, 0.0, 0.05, 0.0);
        }
    }

    /** The Key: the dirt it throws up as it rises, and the column of blue motes while it glows. */
    private static void key(ClientLevel level, VibraniumKeyEntity key, long now, RandomSource random) {
        if (key.emergedAt() < 0) {
            return;
        }
        float rise = key.emergence(now);
        float power = key.glow(now);
        Vec3 at = key.position();
        if (rise > 0.0F && rise < 1.0F) {
            BlockState ground = groundState(level, at);
            for (int i = 0; i < 4 * density(); i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = 1.2 + random.nextDouble() * 1.4;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + Math.cos(angle) * r, at.y + 0.2, at.z + Math.sin(angle) * r, Math.cos(angle) * 0.06, 0.18 + random.nextDouble() * 0.12, Math.sin(angle) * 0.06);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.POOF, at.x + random.nextGaussian() * 1.2, at.y + 0.3, at.z + random.nextGaussian() * 1.2, 0.0, 0.03, 0.0);
            }
        }
        if (power > 0.3F) {
            for (int i = 0; i < 2 * density(); i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = random.nextDouble() * 0.5;
                mote(level, i == 0 ? UltronEffects.c(UltronEffects.GRAV_CORE) : UltronEffects.c(UltronEffects.GRAV_MAIN), 1.2F, at.x + Math.cos(angle) * r, at.y + 2.6, at.z + Math.sin(angle) * r, 0.0, 0.35 + random.nextDouble() * 0.2, 0.0);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.END_ROD, at.x, at.y + 2.7, at.z, random.nextGaussian() * 0.02, 0.3 + random.nextDouble() * 0.2, random.nextGaussian() * 0.02);
            }
        }
    }

    /** The island in the sky: a mote on its rim every few blocks, rising six; and a column of light from the pit. */
    private static void island(ClientLevel level, RandomSource random) {
        SokoviaClient.View view = SokoviaClient.view(0.0F);
        Minecraft mc = Minecraft.getInstance();
        if (view == null || mc.player == null || !(view.up() || view.moving())) {
            return;
        }
        SokoviaShape.Spec spec = view.spec();
        double cx = view.centre().getX() + 0.5;
        double cz = view.centre().getZ() + 0.5;
        double top = view.centre().getY() + view.offset();
        // The rim: about every four blocks round, a mote of the anti-gravity palette that climbs six blocks.
        int points = Math.max(12, (int) (Mth.TWO_PI * spec.radius() / 4.0));
        for (int i = 0; i < points; i++) {
            if (random.nextInt(10) != 0) {
                continue;
            }
            double angle = Mth.TWO_PI * i / points + random.nextDouble() * 0.1;
            double rim = SokoviaShape.rim(spec, angle);
            double x = cx + Math.cos(angle) * rim;
            double z = cz + Math.sin(angle) * rim;
            if (mc.player.distanceToSqr(x, mc.player.getY(), z) > 24.0 * 24.0 + Math.pow(Math.abs(mc.player.getY() - top), 2)) {
                continue;
            }
            mote(level, UltronEffects.c(UltronEffects.GRAV_MAIN), 1.1F, x, top + 0.2, z, 0.0, 0.55
                    + random.nextDouble() * 0.15, 0.0);
        }
        // The pit: the hole the island left, with a column of blue-white motes rising out of it.
        if (view.up() && mc.player.distanceToSqr(cx, mc.player.getY(), cz) < 64.0 * 64.0) {
            for (int i = 0; i < 2 * density(); i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = random.nextDouble() * 1.2;
                mote(level, i == 0 ? UltronEffects.c(UltronEffects.GRAV_CORE) : UltronEffects.c(UltronEffects.GRAV_MAIN), 1.3F, cx + Math.cos(angle) * r, view.centre().getY() - spec.skin() + 0.5, cz + Math.sin(angle) * r, 0.0, 0.6 + random.nextDouble() * 0.2, 0.0);
            }
        }
    }

    private UltronParticles() {
    }
}
