package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * What En Sabah Nur's sand and portals look like (DESIGN section 17, 5.2): the waves that roll along the ground and the
 * rings of the Cairo Collapse, the amber rings that mark where spikes will erupt and the cones that do, the tornadoes (a
 * whirling funnel of sand that drifts toward its victim), the lanes and lines and rings of every tell, the violet portals,
 * the storm the hero raises round himself, the stripe of the Entomb, the shock of the armour falling away, his death in sand
 * or in the Phoenix's fire and the vortex of his Escape. The server sends <em>one</em> {@link ApocalypseFxPayload} for each
 * when it begins; this class keeps the effects (at most {@link #MAX_EFFECTS}), simulates them in step with the server (a wave's
 * front is {@code speed x age} blocks from where it began) and draws them every frame from the {@code AFTER_PARTICLES} stage;
 * the particles they leave are laid once a tick. What the boss's own state shows (the lines of a lance, the lane of a wave
 * before it locks) is {@link BossTells}; the dome of a shield is {@link ShieldFx}; the arena is {@link ArenaFx}.
 *
 * <p>Nothing here changes the world; every shape is made of the shapes in {@link SandShapes}, and an effect is dropped
 * the moment it is over, or farther than {@link #MAX_DISTANCE} from the camera.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SandFx {
    /** The swirling sand texture of the funnels and the walls (four frames, animated). */
    public static final ResourceLocation SWIRL = Enhanced.id("textures/entity/sand_swirl.png");

    private static final int MAX_EFFECTS = 160;
    /** Nothing is drawn farther from the camera than this. */
    private static final double MAX_DISTANCE = 128.0;
    private static final double STEP = 0.6;
    private static final int RING_SEGMENTS = 72;
    private static final Vec3 UP = SandShapes.UP;
    /** The funnel of a tornado: its height, and the rings and segments it is drawn with. */
    private static final double FUNNEL_HEIGHT = 9.0;

    /** One effect, begun by a payload. */
    private static final class Fx {
        final byte kind;
        final int id;
        final Vec3 a;
        Vec3 b;
        int c;
        final int d;
        final long start;
        final int life;
        final long seed;
        final ResourceKey<Level> dimension;
        /** A wave's ground, sampled every {@link #STEP} blocks as it is needed, and how many are known; its limit. */
        double[] ground = new double[0];
        int filled;
        double limit = Double.MAX_VALUE;
        /** A tornado: where it was, where it is going, and when it last reported. */
        Vec3 from;
        long moved;
        long lastSeen;
        /** Which of the three boulders a line of the barrage starts from. */
        int index;
        /** Whether the particles that mark the start have been laid. */
        boolean started;

        Fx(byte kind, int id, Vec3 a, Vec3 b, int c, int d, long start, int life, long seed, ResourceKey<Level> dimension) {
            this.kind = kind;
            this.id = id;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.start = start;
            this.life = life;
            this.seed = seed;
            this.dimension = dimension;
        }

        double speed() {
            return Math.max(0.05, c / 100.0);
        }
    }

    private static final List<Fx> EFFECTS = new ArrayList<>();
    /**
     * 3.5: the colour a custom hero's pick chose for the effect being made, or -1 (the sand, gold and violet of
     * Apocalypse).
     */
    private static int tint = -1;
    /** 3.5: a grey copy of the sand swirl, for a storm in a chosen colour. */
    private static final ResourceLocation SWIRL_GREY = Enhanced.id("textures/entity/custom/sand_swirl_grey.png");

    /** One of Apocalypse's colours, or it recoloured to the colour chosen for the effect being made. */
    private static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /** The colour chosen for an effect a custom hero's ability made (the pick of the hero who made it), or -1. */
    private static int tintOf(ClientLevel level, Fx fx) {
        HeroAbility row = switch (fx.kind) {
            case ApocalypseFxPayload.WAVE, ApocalypseFxPayload.SPIKES -> HeroAbility.APOCALYPSE_SAND;
            case ApocalypseFxPayload.PORTAL -> HeroAbility.APOCALYPSE_PORTAL;
            case ApocalypseFxPayload.ENTOMB -> HeroAbility.APOCALYPSE_ENTOMB;
            default -> null;
        };
        return row == null ? -1 : AbilityColors.chosen(level.getEntity(fx.id), row);
    }
    /** The tornadoes by their hazard's id: one effect each, moved by every report. */
    private static final Map<Integer, Fx> TORNADOES = new HashMap<>();
    /** The storms the heroes raise round themselves, by the hero's entity id: the tick they end at, their radius x 10, the tick they began. */
    private static final Map<Integer, long[]> STORMS = new HashMap<>();
    /** The cocoons seen last tick, to see them break (their sand falls where they were). */
    private static final Map<Integer, Vec3> COCOONS = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------
    // Payloads

    /** The payload of an effect arrived: remember it (the handler of {@link ApocalypseFxPayload}). */
    public static void handle(ApocalypseFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (p.kind()) {
            case ApocalypseFxPayload.ARENA_ON -> {
                ArenaFx.on(p.id(), p.a(), p.c(), p.d());
                return;
            }
            case ApocalypseFxPayload.ARENA_OFF -> {
                ArenaFx.off();
                return;
            }
            case ApocalypseFxPayload.RITUAL -> {
                ArenaFx.ritual(p.a());
                return;
            }
            case ApocalypseFxPayload.SHIELD_HIT -> {
                ShieldFx.hit(p.id(), p.a());
                return;
            }
            case ApocalypseFxPayload.SHIELD_BREAK -> {
                shieldBroke(level, p);
                return;
            }
            case ApocalypseFxPayload.SHIELD_REFORM -> {
                ShieldFx.reform(p.id());
                return;
            }
            case ApocalypseFxPayload.SANDSTORM -> {
                if (p.c() <= 0) {
                    STORMS.remove(p.id());
                } else {
                    STORMS.put(p.id(), new long[]{now + p.c(), p.d(), now});
                }
                return;
            }
            case ApocalypseFxPayload.TORNADO -> {
                tornado(level, p, now);
                return;
            }
            case ApocalypseFxPayload.WAVE_TELL -> {
                return; // the lane of a wave is drawn from the boss's own state (BossTells), locked or not
            }
            case ApocalypseFxPayload.STREAK -> {
                if (!(level.getEntity(p.id()) instanceof ApocalypseEntity boss) || boss.state() == ApocalypseState.TELL_LANCE) {
                    return; // the lines of the lances come from the state, like the lane of the wave
                }
            }
            default -> {
            }
        }
        int life = switch (p.kind()) {
            case ApocalypseFxPayload.WAVE -> (int) Math.ceil(p.d() / Math.max(0.05, p.c() / 100.0)) + 18 + 6;
            case ApocalypseFxPayload.SPIKE_TELL -> p.c() + 8;
            case ApocalypseFxPayload.STREAK -> p.c() + 2;
            case ApocalypseFxPayload.RING_TELL -> p.c() + 6;
            case ApocalypseFxPayload.TORNADO_TELL -> p.c() + 6;
            case ApocalypseFxPayload.SPIKES -> 24;
            case ApocalypseFxPayload.RING -> (int) Math.ceil(p.d() / Math.max(0.05, p.c() / 100.0)) + 16;
            case ApocalypseFxPayload.PORTAL -> p.c() + 8;
            case ApocalypseFxPayload.ARMOUR_FALL -> 26;
            case ApocalypseFxPayload.DEATH_SAND, ApocalypseFxPayload.DEATH_PHOENIX -> p.c() + 10;
            case ApocalypseFxPayload.ESCAPE -> p.c() + 12;
            case ApocalypseFxPayload.ENTOMB -> p.d() + 8;
            default -> 10;
        };
        if (EFFECTS.size() >= MAX_EFFECTS) {
            EFFECTS.remove(0);
        }
        Fx fx = new Fx(p.kind(), p.id(), p.a(), p.b(), p.c(), p.d(), now, life, level.random.nextLong(), level.dimension());
        if (p.kind() == ApocalypseFxPayload.STREAK) {
            for (Fx other : EFFECTS) {
                if (other.kind == ApocalypseFxPayload.STREAK && other.id == p.id() && other.start == now) {
                    fx.index++;
                }
            }
        }
        if (p.kind() == ApocalypseFxPayload.RING_TELL && level.getEntity(p.id()) instanceof ApocalypseEntity boss && boss.state() != ApocalypseState.TELL_MIND_CRUSH) {
            fx.limit = -1.0; // the ripples of the Cairo Collapse, not the Mind Crush
        }
        EFFECTS.add(fx);
        if (p.kind() == ApocalypseFxPayload.ARMOUR_FALL) {
            ArenaFx.shake(0.9F, 14, p.a());
        }
    }

    /** A tornado reported: a new one begins, or the one that is drifting is given its next place. */
    private static void tornado(ClientLevel level, ApocalypseFxPayload p, long now) {
        Fx known = TORNADOES.get(p.id());
        if (known == null || level.getGameTime() - known.lastSeen > 12) {
            Fx fx = new Fx(p.kind(), p.id(), p.a(), p.b(), p.c(), p.d(), now, Integer.MAX_VALUE, level.random.nextLong(), level.dimension());
            fx.from = p.a();
            fx.moved = now;
            fx.lastSeen = now;
            fx.b = p.a();
            TORNADOES.put(p.id(), fx);
            return;
        }
        known.from = tornadoAt(known, now, 0.0F);
        known.b = p.a();
        known.moved = now;
        known.lastSeen = now;
        known.c = p.c();
    }

    /** The shield of an entity broke: the shards fly from its middle. */
    private static void shieldBroke(ClientLevel level, ApocalypseFxPayload p) {
        Entity entity = level.getEntity(p.id());
        Vec3 centre = entity == null ? p.a() : entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        double radius = entity instanceof ApocalypseEntity ? ApocalypseShield.RADIUS : ApocalypseGuard.radius();
        ShieldFx.broke(p.id(), centre, radius);
        for (int i = 0; i < 24; i++) {
            Vec3 d = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.3, level.random.nextDouble() - 0.5).normalize();
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? c(SandPalette.GOLD_CORE) : c(SandPalette.GOLD), 1.2F), centre.x + d.x * radius,
                    centre.y + d.y * radius, centre.z + d.z * radius, d.x * 0.1, d.y * 0.1, d.z * 0.1);
        }
        ArenaFx.shake(0.5F, 8, centre);
    }

    /** Where a tornado is, between its last report and the next: it was at {@code from} and goes to {@code b} over four ticks. */
    private static Vec3 tornadoAt(Fx e, long now, float partial) {
        float u = Mth.clamp((now + partial - e.moved) / 4.0F, 0.0F, 1.0F);
        return e.from.lerp(e.b, u);
    }

    /** The ground under a point of an effect: found by looking down from {@code hint}, as the server does. */
    private static double ground(ClientLevel level, double x, double hint, double z) {
        return SandControl.groundAt(level, x, hint + 1.0, z);
    }

    public static void clear() {
        EFFECTS.clear();
        TORNADOES.clear();
        STORMS.clear();
        COCOONS.clear();
        ShieldFx.clear();
        ArenaFx.clear();
    }

    /** How many effects are alive (for the tests of the clean-up). */
    public static int count() {
        return EFFECTS.size() + TORNADOES.size() + STORMS.size();
    }

    /** Whether a hero's storm is raging round the entity {@code id}, and its radius, or 0. */
    public static float stormAround(int id, long now) {
        long[] storm = STORMS.get(id);
        return storm == null || storm[0] < now ? 0.0F : storm[1] / 10.0F;
    }

    /**
     * The radius of the storm whose reach holds the point (any hero's, or a tornado of the boss), or 0: the client blots its
     * sight out inside one.
     */
    public static float stormAt(ClientLevel level, Vec3 point) {
        long now = level.getGameTime();
        for (Map.Entry<Integer, long[]> storm : STORMS.entrySet()) {
            Entity hero = level.getEntity(storm.getKey());
            if (hero != null && storm.getValue()[0] >= now && hero.position().distanceTo(point) < storm.getValue()[1] / 10.0) {
                return storm.getValue()[1] / 10.0F;
            }
        }
        return 0.0F;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick: the particles they leave, and the ones that have ended

    /** How many particles to lay for each one asked for, from the game's particle option. */
    static float density() {
        return switch (Minecraft.getInstance().options.particles().get()) {
            case ALL -> 1.0F;
            case DECREASED -> 0.5F;
            case MINIMAL -> 0.15F;
        };
    }

    private static void sand(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        // Half of them the sand block's own (which the game darkens), half a tinted mote, so the sand reads as sand.
        if (tint >= 0) {
            // (3.5: sand of a chosen colour: motes of it.)
            int rgb = c(level.random.nextBoolean() ? SandPalette.MAIN : SandPalette.SHADE);
            level.addParticle(new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F),
                    0.9F), x, y, z, vx, vy, vz);
            return;
        }
        level.addParticle(level.random.nextBoolean() ? SandParticles.GRAIN : SandParticles.mote(), x, y, z, vx, vy, vz);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            if (count() > 0) {
                clear();
            }
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        long now = level.getGameTime();
        float density = density();
        RandomSource random = level.random;
        EFFECTS.removeIf(fx -> !fx.dimension.equals(level.dimension()) || now - fx.start > fx.life);
        TORNADOES.values().removeIf(fx -> now - fx.lastSeen > 10 || !fx.dimension.equals(level.dimension()));
        STORMS.values().removeIf(storm -> storm[0] < now);
        for (Fx fx : EFFECTS) {
            tint = tintOf(level, fx);
            tick(level, fx, now, density, random);
        }
        tint = -1;
        for (Fx fx : TORNADOES.values()) {
            tornadoParticles(level, fx, now, density, random);
        }
        storms(level, now, density, random);
        BossTells.tick(level, now, density, random);
        cocoons(level, density, random);
    }

    private static void tick(ClientLevel level, Fx fx, long now, float density, RandomSource random) {
        float age = now - fx.start;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() != null && mc.getCameraEntity().position().distanceToSqr(fx.a) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }
        switch (fx.kind) {
            case ApocalypseFxPayload.WAVE -> {
                double speed = fx.speed();
                double front = Math.min(Math.min(fx.d, fx.limit), speed * age);
                if (speed * age <= Math.min(fx.d, fx.limit) + 1.0) {
                    Vec3 dir = fx.b;
                    Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                    Vec3 at = fx.a.add(dir.scale(front));
                    double y = ground(level, at.x, fx.a.y, at.z);
                    // Sand pours off the crest and a plume of dust trails behind it.
                    for (int i = 0; i < Math.round(4 * density); i++) {
                        double across = (random.nextDouble() - 0.5) * 3.6;
                        sand(level, at.x + side.x * across, y + 0.5 + random.nextDouble() * 0.8, at.z + side.z * across, dir.x * 0.1, 0.06 + random.nextDouble() * 0.08,
                                dir.z * 0.1);
                    }
                    if (random.nextFloat() < 0.5F * density) {
                        double across = (random.nextDouble() - 0.5) * 3.0;
                        Vec3 behind = fx.a.add(dir.scale(Math.max(0.0, front - 2.0)));
                        level.addParticle(SandParticles.haze(1.6F), behind.x + side.x * across, y + 0.3, behind.z + side.z * across, 0.0, 0.03, 0.0);
                    }
                }
            }
            case ApocalypseFxPayload.SPIKES -> {
                if (!fx.started) {
                    fx.started = true;
                    double radius = fx.c / 10.0;
                    for (int i = 0; i < Math.round(26 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double r = radius * random.nextDouble();
                        sand(level, fx.a.x + Math.cos(angle) * r, fx.a.y + 0.2, fx.a.z + Math.sin(angle) * r, Math.cos(angle) * 0.08, 0.25 + random.nextDouble() * 0.2,
                                Math.sin(angle) * 0.08);
                    }
                    ArenaFx.shake(0.25F, 5, fx.a);
                }
            }
            case ApocalypseFxPayload.TORNADO_TELL -> {
                double radius = fx.d / 10.0;
                float u = age / Math.max(1, fx.c);
                for (int i = 0; i < Math.round(5 * density); i++) {
                    double angle = age * 0.35 + random.nextDouble() * Mth.TWO_PI;
                    double r = radius * (0.4 + 0.6 * random.nextDouble());
                    sand(level, fx.a.x + Math.cos(angle) * r, fx.a.y + 0.1, fx.a.z + Math.sin(angle) * r, -Math.sin(angle) * 0.06, 0.1 + 0.25 * u,
                            Math.cos(angle) * 0.06);
                }
            }
            case ApocalypseFxPayload.RING_TELL -> {
                if (fx.limit >= 0.0 && level.getEntity(fx.id) instanceof ApocalypseEntity boss) {
                    // The Mind Crush: white sparks run along the ring as it closes.
                    double r = SandMath.contracting(age, fx.c, fx.d);
                    Vec3 centre = boss.position().add(0.0, boss.getBbHeight() * 0.85, 0.0);
                    for (int i = 0; i < Math.round(8 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? 0xF4F4F4 : c(SandPalette.VIOLET), 0.9F), centre.x + Math.cos(angle) * r,
                                centre.y, centre.z + Math.sin(angle) * r, -Math.cos(angle) * 0.1, 0.0, -Math.sin(angle) * 0.1);
                    }
                } else if (fx.limit < 0.0 && level.getEntity(fx.id) instanceof ApocalypseEntity boss) {
                    // The Cairo Collapse: the ground ripples outward from him.
                    double r = fx.d * Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
                    for (int i = 0; i < Math.round(6 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        sand(level, boss.getX() + Math.cos(angle) * r, boss.getY() + 0.1, boss.getZ() + Math.sin(angle) * r, 0.0, 0.08, 0.0);
                    }
                }
            }
            case ApocalypseFxPayload.RING -> {
                double front = Math.min(fx.d, fx.speed() * age);
                if (fx.speed() * age <= fx.d + 1.0) {
                    for (int i = 0; i < Math.round(8 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double gx = fx.a.x + Math.cos(angle) * front;
                        double gz = fx.a.z + Math.sin(angle) * front;
                        sand(level, gx, ground(level, gx, fx.a.y, gz) + 0.5, gz, 0.0, 0.1 + random.nextDouble() * 0.08, 0.0);
                    }
                }
            }
            case ApocalypseFxPayload.PORTAL -> {
                if (age < fx.c) {
                    Vec3 centre = portalCentre(level, fx.a, fx.d / 10.0);
                    double radius = fx.d / 10.0;
                    for (int i = 0; i < Math.round(2 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? c(SandPalette.VIOLET) : c(SandPalette.VIOLET_CORE), 0.9F),
                                centre.x + Math.cos(angle) * radius, centre.y + Math.sin(angle) * radius, centre.z + (random.nextDouble() - 0.5) * 0.3,
                                -Math.cos(angle) * 0.04, -Math.sin(angle) * 0.04, 0.0);
                    }
                }
            }
            case ApocalypseFxPayload.ARMOUR_FALL -> {
                if (age == 0 || age == ApocalypseModel.PLATES_FLY_AT) {
                    int count = Math.round((age == 0 ? 70 : 40) * density);
                    for (int i = 0; i < count; i++) {
                        Vec3 d = new Vec3(random.nextDouble() - 0.5, random.nextDouble() * 0.6, random.nextDouble() - 0.5).normalize();
                        level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? c(SandPalette.VIOLET) : c(SandPalette.GOLD), 1.3F), fx.a.x, fx.a.y + 1.4, fx.a.z,
                                d.x * 0.35, d.y * 0.3 + 0.1, d.z * 0.35);
                    }
                    if (age > 0) {
                        ArenaFx.shake(0.5F, 8, fx.a);
                    }
                }
            }
            case ApocalypseFxPayload.DEATH_SAND, ApocalypseFxPayload.DEATH_PHOENIX -> death(level, fx, age, density, random);
            case ApocalypseFxPayload.ESCAPE -> {
                if (level.getEntity(fx.id) instanceof ApocalypseEntity boss && age < fx.c) {
                    float grow = Mth.clamp(age / fx.c, 0.0F, 1.0F);
                    Vec3 centre = boss.position().add(0.0, boss.getBbHeight() + 1.2, 0.0);
                    double radius = 0.8 + 2.7 * grow;
                    for (int i = 0; i < Math.round((2 + 4 * grow) * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? c(SandPalette.VIOLET) : c(SandPalette.VIOLET_CORE), 1.0F),
                                centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius, -Math.cos(angle) * 0.08, 0.02, -Math.sin(angle) * 0.08);
                    }
                }
            }
            case ApocalypseFxPayload.ENTOMB -> {
                // The stripe has reached the victim: sand climbs it.
                float u = age / Math.max(1, fx.d);
                if (level.getEntity(fx.c) instanceof Entity victim && u >= 0.4F && u <= 1.4F) {
                    for (int i = 0; i < Math.round(5 * density); i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double h = victim.getBbHeight() * Mth.clamp(u - 0.4F, 0.0F, 1.0F) * random.nextDouble();
                        sand(level, victim.getX() + Math.cos(angle) * 0.5, victim.getY() + h, victim.getZ() + Math.sin(angle) * 0.5, 0.0, 0.05, 0.0);
                    }
                }
            }
            default -> {
            }
        }
    }

    /** His end: sand rising from the feet up, or fire eating the body from the surface in. */
    private static void death(ClientLevel level, Fx fx, float age, float density, RandomSource random) {
        if (!(level.getEntity(fx.id) instanceof ApocalypseEntity boss)) {
            return;
        }
        float p = Mth.clamp((age - ApocalypseModel.DISSOLVE_FROM) / (ApocalypseModel.DISSOLVE_TO - ApocalypseModel.DISSOLVE_FROM), 0.0F, 1.0F);
        if (age < ApocalypseModel.DISSOLVE_FROM || age > ApocalypseModel.DISSOLVE_TO + 20.0F) {
            return;
        }
        double height = 3.0;
        boolean fire = fx.kind == ApocalypseFxPayload.DEATH_PHOENIX;
        for (int i = 0; i < Math.round(10 * density); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = 0.2 + random.nextDouble() * 0.6;
            double y = fire ? random.nextDouble() * height : Mth.clamp(p * height + (random.nextDouble() - 0.5) * 0.8, 0.0, height);
            double x = boss.getX() + Math.cos(angle) * r;
            double z = boss.getZ() + Math.sin(angle) * r;
            if (fire) {
                int colour = switch (random.nextInt(4)) {
                    case 0 -> c(SandPalette.FIRE_CORE);
                    case 1 -> c(SandPalette.FIRE_GOLD);
                    case 2 -> c(SandPalette.FIRE_MAIN);
                    default -> c(SandPalette.FIRE_EMBER);
                };
                level.addParticle(new EnergyParticleOptions(colour, 1.2F), x, boss.getY() + y, z, Math.cos(angle) * 0.03, 0.06 + random.nextDouble() * 0.06,
                        Math.sin(angle) * 0.03);
                if (i % 3 == 0) {
                    level.addParticle(ParticleTypes.FLAME, x, boss.getY() + y, z, 0.0, 0.05, 0.0);
                }
            } else {
                sand(level, x, boss.getY() + y, z, Math.cos(angle) * 0.02, 0.07 + random.nextDouble() * 0.05, Math.sin(angle) * 0.02);
            }
        }
    }

    private static void tornadoParticles(ClientLevel level, Fx fx, long now, float density, RandomSource random) {
        Vec3 at = tornadoAt(fx, now, 0.0F);
        double radius = fx.d / 10.0;
        double base = ground(level, at.x, at.y, at.z);
        for (int i = 0; i < Math.round(6 * density); i++) {
            double h = random.nextDouble() * FUNNEL_HEIGHT * 0.8;
            double r = Mth.lerp(h / FUNNEL_HEIGHT, 1.0, radius);
            double angle = now * 0.4 + h * 0.8 + random.nextDouble();
            sand(level, at.x + Math.cos(angle) * r, base + h, at.z + Math.sin(angle) * r, -Math.sin(angle) * 0.25, 0.08, Math.cos(angle) * 0.25);
        }
        for (int i = 0; i < Math.round(2 * density); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            level.addParticle(SandParticles.haze(1.8F), at.x + Math.cos(angle) * radius * 0.8, base + 0.2, at.z + Math.sin(angle) * radius * 0.8, 0.0, 0.02, 0.0);
        }
    }

    /** The storms round the heroes: a whirl of sand and dust that follows the hero. */
    private static void storms(ClientLevel level, long now, float density, RandomSource random) {
        for (Map.Entry<Integer, long[]> storm : STORMS.entrySet()) {
            Entity hero = level.getEntity(storm.getKey());
            if (hero == null) {
                continue;
            }
            tint = AbilityColors.chosen(hero, HeroAbility.APOCALYPSE_SANDSTORM);
            double radius = storm.getValue()[1] / 10.0;
            for (int i = 0; i < Math.round(10 * density); i++) {
                double h = random.nextDouble() * 4.0;
                double r = radius * (0.35 + 0.65 * random.nextDouble());
                double angle = now * 0.5 + h * 0.9 + random.nextDouble() * 2.0;
                sand(level, hero.getX() + Math.cos(angle) * r, hero.getY() + h, hero.getZ() + Math.sin(angle) * r, -Math.sin(angle) * 0.3, 0.05, Math.cos(angle) * 0.3);
            }
        }
        tint = -1;
    }

    /** A cocoon that is gone: its sand falls where it was. */
    private static void cocoons(ClientLevel level, float density, RandomSource random) {
        Map<Integer, Vec3> seen = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof com.kolodkingal.enhanced.entity.SandCocoonEntity) {
                seen.put(entity.getId(), entity.position());
            }
        }
        for (Map.Entry<Integer, Vec3> before : COCOONS.entrySet()) {
            if (!seen.containsKey(before.getKey())) {
                Vec3 at = before.getValue();
                for (int i = 0; i < Math.round(30 * density); i++) {
                    sand(level, at.x + (random.nextDouble() - 0.5) * 1.0, at.y + random.nextDouble() * 2.0, at.z + (random.nextDouble() - 0.5) * 1.0,
                            (random.nextDouble() - 0.5) * 0.1, -0.05, (random.nextDouble() - 0.5) * 0.1);
                }
            }
        }
        COCOONS.clear();
        COCOONS.putAll(seen);
        // The grains a lance sheds as it flies.
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof com.kolodkingal.enhanced.entity.SandLanceEntity && random.nextFloat() < density) {
                sand(level, entity.getX(), entity.getY() + 0.15, entity.getZ(), (random.nextDouble() - 0.5) * 0.02, -0.01, (random.nextDouble() - 0.5) * 0.02);
            } else if (entity instanceof com.kolodkingal.enhanced.entity.SandBoulderEntity && random.nextFloat() < density * 0.7F) {
                sand(level, entity.getX() + (random.nextDouble() - 0.5) * 0.5, entity.getY() + 0.3, entity.getZ() + (random.nextDouble() - 0.5) * 0.5, 0.0, -0.02, 0.0);
            }
        }
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
        boolean bosses = !ApocalypseBosses.all().isEmpty();
        if (EFFECTS.isEmpty() && TORNADOES.isEmpty() && STORMS.isEmpty() && !bosses) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        long now = level.getGameTime();
        float time = now + partial;
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float calm = EnhancedClientConfig.reducedMotion() ? 0.6F : 1.0F;

        // 1: the sand that lies on the ground (see-through).
        VertexConsumer flat = buffers.getBuffer(SandRenderTypes.FLAT);
        for (Fx fx : EFFECTS) {
            if (visible(fx.a, camera)) {
                tint = tintOf(level, fx);
                flatEffect(level, fx, now, partial, pose, flat, camera, time, calm);
            }
        }
        tint = -1;
        BossTells.renderFlat(level, now, partial, pose, flat, camera, time, calm);
        buffers.endBatch(SandRenderTypes.FLAT);

        // 2: the funnels of the tornadoes and the storms (sand texture, see-through).
        VertexConsumer funnels = buffers.getBuffer(SandRenderTypes.textured(SWIRL));
        for (Fx fx : TORNADOES.values()) {
            if (visible(fx.b, camera)) {
                tornado(level, fx, now, partial, pose, funnels, camera, time);
            }
        }
        for (Map.Entry<Integer, long[]> storm : STORMS.entrySet()) {
            Entity hero = level.getEntity(storm.getKey());
            if (hero != null && storm.getValue()[0] >= now && visible(hero.position(), camera)
                    && AbilityColors.chosen(hero, HeroAbility.APOCALYPSE_SANDSTORM) < 0) {
                storm(level, hero, storm.getValue(), now, partial, pose, funnels, camera, time);
            }
        }
        buffers.endBatch(SandRenderTypes.textured(SWIRL));
        // (3.5: a storm in a chosen colour: the grey swirl tinted.)
        VertexConsumer tinted = buffers.getBuffer(SandRenderTypes.textured(SWIRL_GREY));
        for (Map.Entry<Integer, long[]> storm : STORMS.entrySet()) {
            Entity hero = level.getEntity(storm.getKey());
            tint = AbilityColors.chosen(hero, HeroAbility.APOCALYPSE_SANDSTORM);
            if (hero != null && tint >= 0 && storm.getValue()[0] >= now && visible(hero.position(), camera)) {
                storm(level, hero, storm.getValue(), now, partial, pose, tinted, camera, time);
            }
        }
        tint = -1;
        buffers.endBatch(SandRenderTypes.textured(SWIRL_GREY));

        // 3: what glows (additive): the lines of the tells, the portals, the shock, the fire.
        VertexConsumer beams = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Fx fx : EFFECTS) {
            if (visible(fx.a, camera)) {
                tint = tintOf(level, fx);
                glowEffect(level, fx, now, partial, pose, beams, camera, time, calm);
            }
        }
        tint = -1;
        BossTells.renderGlow(level, now, partial, pose, beams, camera, time, calm);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    private static boolean visible(Vec3 at, Vec3 camera) {
        return at.distanceToSqr(camera) < MAX_DISTANCE * MAX_DISTANCE;
    }

    /** The ground height of an effect's lane, sampled as far as it is needed (the lane of a wave, the spikes of a ring). */
    private static double laneGround(ClientLevel level, Fx fx, int index) {
        if (fx.ground.length <= index) {
            fx.ground = java.util.Arrays.copyOf(fx.ground, Math.max(index + 8, fx.ground.length * 2));
        }
        while (fx.filled <= index) {
            Vec3 at = fx.a.add(fx.b.scale(fx.filled * STEP));
            double hint = fx.filled == 0 ? fx.a.y : fx.ground[fx.filled - 1];
            double g = ground(level, at.x, hint, at.z);
            if (fx.filled > 0 && Math.abs(g - fx.ground[fx.filled - 1]) > 2.0 && fx.limit == Double.MAX_VALUE) {
                fx.limit = (fx.filled - 1) * STEP; // it breaks up where the ground steps by more than two blocks
            }
            fx.ground[fx.filled++] = g;
        }
        return fx.ground[index];
    }

    private static void flatEffect(ClientLevel level, Fx fx, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        float age = now - fx.start + partial;
        switch (fx.kind) {
            case ApocalypseFxPayload.WAVE -> wave(level, fx, age, pose, vc, camera, calm);
            case ApocalypseFxPayload.RING -> ring(level, fx, age, pose, vc, camera, calm);
            case ApocalypseFxPayload.SPIKE_TELL -> {
                // An amber ring (and a faint disc) on the ground that pulses faster as the time runs out.
                double radius = fx.d / 10.0;
                float u = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
                float pulse = 0.5F + 0.5F * Mth.sin(age * (0.45F + 1.5F * u));
                double y = ground(level, fx.a.x, fx.a.y, fx.a.z) + 0.04;
                Vec3 c = new Vec3(fx.a.x, y, fx.a.z).subtract(camera);
                float fade = 1.0F - AnimMath.ss(fx.c, fx.c + 6, age);
                SandShapes.flatDisc(pose, vc, c, radius, 32, argb(c(SandPalette.GOLD_EDGE), (0.12F + 0.16F * pulse) * fade * calm),
                        argb(c(SandPalette.GOLD_EDGE), (0.20F + 0.20F * pulse) * fade * calm));
                SandShapes.flatRing(pose, vc, c.add(0.0, 0.01, 0.0), radius, 0.22 + 0.1 * pulse, 40, argb(c(SandPalette.GOLD), (0.55F + 0.35F * pulse) * fade * calm));
            }
            case ApocalypseFxPayload.TORNADO_TELL -> {
                double radius = fx.d / 10.0;
                float u = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
                double y = ground(level, fx.a.x, fx.a.y, fx.a.z) + 0.05;
                Vec3 c = new Vec3(fx.a.x, y, fx.a.z).subtract(camera);
                float fade = (1.0F - AnimMath.ss(fx.c, fx.c + 6, age)) * calm;
                SandShapes.flatDisc(pose, vc, c, radius, 32, argb(c(SandPalette.DEEP), (0.1F + 0.3F * u) * fade), argb(c(SandPalette.SHADE), (0.15F + 0.3F * u) * fade));
                SandShapes.flatRing(pose, vc, c.add(0.0, 0.01, 0.0), radius, 0.2, 40, argb(c(SandPalette.LIGHT), (0.4F + 0.4F * u) * fade));
                // Three arms of the whirl turn on the ground.
                for (int arm = 0; arm < 3; arm++) {
                    double angle = age * 0.3 + arm * Mth.TWO_PI / 3.0;
                    SandShapes.lane(pose, vc, c.add(0.0, 0.02, 0.0), new Vec3(Math.cos(angle), 0.0, Math.sin(angle)), radius, 0.3,
                            argb(c(SandPalette.LIGHT), 0.0F), argb(c(SandPalette.LIGHT), 0.55F * u * fade));
                }
            }
            case ApocalypseFxPayload.SPIKES -> spikes(level, fx, age, pose, vc, camera, calm);
            case ApocalypseFxPayload.RING_TELL -> {
                if (fx.limit < 0.0 && level.getEntity(fx.id) instanceof ApocalypseEntity boss) {
                    // The ground ripples outward from him: three faint rings that run out to the edge, 16 ticks before the wave.
                    float u = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
                    for (int k = 0; k < 3; k++) {
                        float uk = Mth.clamp(u * 1.3F - k * 0.12F, 0.0F, 1.0F);
                        if (uk <= 0.0F || uk >= 1.0F) {
                            continue;
                        }
                        Vec3 c = new Vec3(boss.getX(), fx.a.y + 0.06, boss.getZ()).subtract(camera);
                        SandShapes.flatRing(pose, vc, c, fx.d * uk, 0.5, RING_SEGMENTS, argb(c(SandPalette.LIGHT), 0.5F * (1.0F - uk) * calm));
                    }
                }
            }
            case ApocalypseFxPayload.ENTOMB -> {
                // The hero's grasp: a stripe of sand crawls along the ground from his feet to the victim's.
                float u = Mth.clamp(age / Math.max(1, fx.d), 0.0F, 1.0F);
                Vec3 to = fx.a.lerp(fx.b, u);
                Vec3 flatDir = new Vec3(to.x - fx.a.x, 0.0, to.z - fx.a.z);
                double length = flatDir.length();
                if (length > 0.05) {
                    float fade = 1.0F - AnimMath.ss(fx.d, fx.d + 8, age);
                    SandShapes.lane(pose, vc, fx.a.add(0.0, 0.06, 0.0).subtract(camera), flatDir.scale(1.0 / length), length, 0.6, argb(c(SandPalette.SHADE), 0.2F * fade),
                            argb(c(SandPalette.LIGHT), 0.8F * fade));
                }
            }
            case ApocalypseFxPayload.DEATH_SAND -> {
                // The heap he leaves: a mound that grows as he comes apart and sinks away after.
                if (level.getEntity(fx.id) instanceof ApocalypseEntity boss) {
                    float p = Mth.clamp((age - ApocalypseModel.DISSOLVE_FROM) / (ApocalypseModel.DISSOLVE_TO - ApocalypseModel.DISSOLVE_FROM), 0.0F, 1.0F);
                    float fade = 1.0F - AnimMath.ss(fx.c - 10, fx.c + 10, age);
                    if (p > 0.0F) {
                        Vec3 base = boss.position().add(0.0, 0.02, 0.0).subtract(camera);
                        SandShapes.cone(pose, vc, base, 1.9 * p, 0.9 * p, 24, argb(c(SandPalette.SHADE), 0.85F * fade), argb(c(SandPalette.LIGHT), 0.9F * fade));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** A wave: a ridge of sand rolling along the ground, its crest at the front; breaking up where it ends. */
    private static void wave(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float calm) {
        double speed = fx.speed();
        double length = fx.d;
        double run = speed * age;
        // The ground ahead is read first, so the end of the lane (a step of more than two blocks) is known.
        int need = (int) Math.min(length / STEP + 2, (run / STEP) + 3);
        laneGround(level, fx, Math.max(0, need));
        double reach = Math.min(length, fx.limit);
        double front = Math.min(reach, run);
        // Past the end the ridge collapses: it sinks as the tail catches up.
        double collapse = run > reach ? Mth.clamp((run - reach) / SandMath.RIDGE_LENGTH, 0.0, 1.0) : 0.0;
        double crest = 1.1 * (1.0 - collapse);
        if (crest <= 0.02 || front <= 0.0) {
            return;
        }
        Vec3 dir = fx.b;
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
        double tail = Math.max(0.0, front - SandMath.RIDGE_LENGTH);
        int first = (int) Math.floor(tail / STEP);
        int last = (int) Math.floor(front / STEP);
        SandShapes.Slice previous = null;
        float previousFade = 0.0F;
        for (int i = first; i <= last + 1; i++) {
            double dist = Math.min(i * STEP, front + 0.4);
            double behind = Math.max(0.0, front - dist);
            double ahead = dist > front ? 1.0 : 0.0;
            double ground = laneGround(level, fx, Math.min(i, fx.filled - 1)) + 0.02;
            double h = ahead > 0.0 ? 0.0 : SandMath.ridge(behind, crest);
            Vec3 base = fx.a.add(dir.scale(dist)).subtract(camera);
            base = new Vec3(base.x, ground - camera.y, base.z);
            SandShapes.Slice slice = new SandShapes.Slice(base, side, 4.0, h);
            float fade = (float) Mth.clamp(1.0 - behind / SandMath.RIDGE_LENGTH, 0.0, 1.0) * calm;
            if (previous != null) {
                SandShapes.ridge(pose, vc, previous, slice, previousFade, fade, 0.0F);
            }
            previous = slice;
            previousFade = fade;
        }
        // Where it breaks the wave sinks into a mound that fades: nothing more to draw.
    }

    /** The great ring of the Cairo Collapse: a circular ridge sweeping out from his feet. */
    private static void ring(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float calm) {
        double front = fx.speed() * age;
        double maxRadius = fx.d;
        if (front <= 0.1) {
            return;
        }
        double radius = Math.min(front, maxRadius);
        double crest = 1.1 * (1.0 - Mth.clamp((front - maxRadius) / 3.0, 0.0, 1.0));
        if (crest <= 0.02) {
            return;
        }
        // The ground is read every few ticks round the ring (the ridge follows it).
        if (fx.ground.length != RING_SEGMENTS) {
            fx.ground = new double[RING_SEGMENTS];
            java.util.Arrays.fill(fx.ground, fx.a.y);
        }
        boolean read = (int) age % 3 == 0 || !fx.started;
        fx.started = true;
        SandShapes.Slice first = null;
        SandShapes.Slice previous = null;
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            int k = i % RING_SEGMENTS;
            double angle = Mth.TWO_PI * k / RING_SEGMENTS;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = fx.a.x + cos * radius;
            double z = fx.a.z + sin * radius;
            if (read) {
                fx.ground[k] = ground(level, x, fx.ground[k], z);
            }
            Vec3 base = new Vec3(x - camera.x, fx.ground[k] + 0.02 - camera.y, z - camera.z);
            SandShapes.Slice slice = new SandShapes.Slice(base, new Vec3(cos, 0.0, sin), 1.5, crest);
            if (previous != null) {
                SandShapes.ridge(pose, vc, previous, slice, 0.85F * calm, 0.85F * calm, 0.0F);
            }
            previous = slice;
            if (first == null) {
                first = slice;
            }
        }
    }

    /** The six cones of a ring of spikes: up in four ticks, settling, sinking. */
    private static void spikes(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float calm) {
        double radius = fx.c / 10.0;
        float rise = SandMath.spike(age);
        if (rise <= 0.01F) {
            return;
        }
        double gy = ground(level, fx.a.x, fx.a.y, fx.a.z);
        for (int i = 0; i < Math.max(1, fx.d); i++) {
            Vec3 at = SandMath.cone(fx.a, radius, i);
            Vec3 base = new Vec3(at.x - camera.x, gy - camera.y, at.z - camera.z);
            double height = 2.6 * SandMath.coneScale(i) * rise;
            SandShapes.cone(pose, vc, base, radius * (i == 0 ? 0.42 : 0.3), height, 10, argb(c(SandPalette.SHADE), 0.9F * calm), argb(c(SandPalette.LIGHT), 0.95F * calm));
        }
    }

    /** A tornado's funnel, whirling: wide at the top, narrow where it meets the ground, with dust at its foot. */
    private static void tornado(ClientLevel level, Fx fx, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time) {
        Vec3 at = tornadoAt(fx, now, partial);
        double radius = fx.d / 10.0;
        double base = ground(level, at.x, at.y, at.z);
        float born = AnimMath.ss(0.0F, 12.0F, now + partial - fx.start);
        float dying = 1.0F - AnimMath.ss(6.0F, 10.0F, now + partial - fx.lastSeen);
        float fade = born * dying;
        Vec3 b = new Vec3(at.x - camera.x, base - camera.y, at.z - camera.z);
        float scroll = time * 0.045F;
        SandShapes.funnel(pose, vc, b, 0.9, radius, FUNNEL_HEIGHT, 28, 8, 3.0F, scroll, time * 0.05F, c(SandPalette.LIGHT), 0.55F * fade, 0.28F * fade);
        SandShapes.funnel(pose, vc, b, 0.6, radius * 0.7, FUNNEL_HEIGHT * 0.85, 20, 6, 2.0F, -scroll * 1.6F, -time * 0.08F, c(SandPalette.MAIN), 0.6F * fade, 0.3F * fade);
    }

    /** The hero's storm: a funnel of sand round him that follows him, and a dust disc on the ground. */
    private static void storm(ClientLevel level, Entity hero, long[] storm, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time) {
        double radius = storm[1] / 10.0;
        Vec3 at = hero.getPosition(partial);
        Vec3 b = new Vec3(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        // It rises over ten ticks and settles over the last eight.
        float fade = AnimMath.ss(0.0F, 8.0F, storm[0] - now - partial) * AnimMath.ss(0.0F, 10.0F, now + partial - storm[2]);
        float scroll = time * 0.06F;
        SandShapes.funnel(pose, vc, b, radius * 0.45, radius, 6.0, 32, 6, 4.0F, scroll, time * 0.07F, c(SandPalette.LIGHT), 0.5F * fade, 0.18F * fade);
        SandShapes.funnel(pose, vc, b, radius * 0.35, radius * 0.8, 5.0, 24, 5, 3.0F, -scroll * 1.5F, -time * 0.1F, c(SandPalette.MAIN), 0.55F * fade, 0.2F * fade);
    }

    private static void glowEffect(ClientLevel level, Fx fx, long now, float partial, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        float age = now - fx.start + partial;
        switch (fx.kind) {
            case ApocalypseFxPayload.WAVE_TELL, ApocalypseFxPayload.SPIKE_TELL -> {
                // (drawn flat)
            }
            case ApocalypseFxPayload.STREAK -> streak(level, fx, age, pose, vc, camera);
            case ApocalypseFxPayload.RING_TELL -> {
                if (fx.limit >= 0.0 && level.getEntity(fx.id) instanceof ApocalypseEntity boss) {
                    double r = SandMath.contracting(age, fx.c, fx.d);
                    Vec3 c = boss.getPosition(partial).add(0.0, boss.getBbHeight() * 0.85, 0.0).subtract(camera);
                    float u = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
                    float fade = (1.0F - AnimMath.ss(fx.c, fx.c + 6, age)) * calm;
                    SandShapes.flatRing(pose, vc, c, r, 0.35 + 0.4 * u, 96, argb(c(SandPalette.VIOLET), (0.25F + 0.4F * u) * fade));
                    SandShapes.flatRing(pose, vc, c.add(0.0, 0.01, 0.0), r, 0.08, 96, argb(0xF4F4F4, (0.5F + 0.4F * u) * fade));
                    SandShapes.flatRing(pose, vc, c, Math.min(fx.d, r + 1.2), 0.12, 96, argb(c(SandPalette.VIOLET_CORE), 0.25F * fade));
                }
            }
            case ApocalypseFxPayload.PORTAL -> portal(level, fx, age, pose, vc, camera, time, calm);
            case ApocalypseFxPayload.ARMOUR_FALL -> {
                // The shock: a ring of violet and gold runs out from his feet, and a column of light stands on him for a moment.
                float u = Mth.clamp(age / 12.0F, 0.0F, 1.0F);
                Vec3 c = new Vec3(fx.a.x, fx.a.y + 0.1, fx.a.z).subtract(camera);
                float fade = (1.0F - u) * calm;
                if (age < 14.0F) {
                    SandShapes.flatRing(pose, vc, c, 0.5 + 8.0 * AnimMath.easeOutCubic(u), 0.6, 64, argb(c(SandPalette.VIOLET), 0.55F * fade),
                            argb(c(SandPalette.GOLD), 0.7F * fade));
                    SandShapes.column(pose, vc, c, 0.9 * (1.0 - u * 0.5), 6.0, 20, argb(c(SandPalette.GOLD_CORE), 0.45F * fade), argb(c(SandPalette.VIOLET), 0.0F));
                }
            }
            case ApocalypseFxPayload.DEATH_PHOENIX -> firebird(level, fx, age, pose, vc, camera, time);
            case ApocalypseFxPayload.ESCAPE -> escape(level, fx, age, pose, vc, partial, camera, time, calm);
            case ApocalypseFxPayload.WAVE -> {
                // A pale haze over the crest, so the ridge reads against bright sand.
            }
            default -> {
            }
        }
    }

    /** Lines to the targets of the barrage (from his orbiting boulders) and the glint of the grasp (from his hand). */
    private static void streak(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera) {
        if (!(level.getEntity(fx.id) instanceof ApocalypseEntity boss)) {
            return;
        }
        float u = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
        Vec3 from;
        Vec3 to = fx.b;
        if (boss.state() == ApocalypseState.TELL_BOULDERS) {
            from = SandMath.boulder(boss.position(), boss.getBbHeight(), Math.min(SandMath.BOULDER_TELL, boss.stateTime(0.0F)), fx.index % 3);
        } else {
            from = BossTells.palm(boss);
            if (level.getEntity(fx.d) instanceof Entity target) {
                to = target.position().add(0.0, target.getBbHeight() * 0.65, 0.0);
            }
        }
        Vec3 a = from.subtract(camera);
        Vec3 b = to.subtract(camera);
        EnergyGeometry.ribbon(pose, vc, a, b, 0.0F, 0.14F, argb(c(SandPalette.GOLD_EDGE), 0.0F), argb(c(SandPalette.GOLD_EDGE), 0.25F + 0.4F * u));
        EnergyGeometry.ribbon(pose, vc, a, b, 0.0F, 0.05F, argb(c(SandPalette.GOLD_CORE), 0.0F), argb(c(SandPalette.GOLD_CORE), 0.35F + 0.5F * u));
    }

    /** The centre of an upright portal standing on the ground near {@code at}: its lowest point is on the ground. */
    private static Vec3 portalCentre(ClientLevel level, Vec3 at, double radius) {
        double ground = SandControl.groundAt(level, at.x, at.y + 1.0, at.z);
        return new Vec3(at.x, ground + Math.max(radius * 0.9, 1.0), at.z);
    }

    /** A portal: a ring of swirling violet shards on a disc of light, standing upright and turned to the camera, opening and closing. */
    private static void portal(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time, float calm) {
        double radius = fx.d / 10.0;
        float open = AnimMath.ss(0.0F, 6.0F, age) * (1.0F - AnimMath.ss(fx.c, fx.c + 8, age));
        if (open <= 0.01F) {
            return;
        }
        List<Vec3> ends = new ArrayList<>();
        ends.add(fx.a);
        if (fx.b.distanceToSqr(fx.a) > 1.0) {
            ends.add(fx.b); // the exit of a step opens too
        }
        for (Vec3 end : ends) {
            Vec3 centre = portalCentre(level, end, radius);
            Vec3 toCamera = camera.subtract(centre);
            Vec3 facing = new Vec3(toCamera.x, 0.0, toCamera.z);
            facing = facing.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : facing.normalize();
            double r = radius * open;
            Vec3 c = centre.subtract(camera);
            SandShapes.portalRing(pose, vc, c, facing, r, 24, age * 0.18, time * 0.3, i -> argb(i % 3 == 0 ? c(SandPalette.VIOLET_CORE) : c(SandPalette.VIOLET), 0.8F * open * calm));
            SandShapes.portalRing(pose, vc, c, facing, r * 0.62, 16, -age * 0.27, time * 0.4 + 2.0, i -> argb(i % 2 == 0 ? c(SandPalette.VIOLET_BODY) : c(SandPalette.VIOLET), 0.55F * open * calm));
            // The disc: a bright core fading to violet at the rim.
            Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
            for (int i = 0; i < 24; i++) {
                double t0 = Mth.TWO_PI * i / 24;
                double t1 = Mth.TWO_PI * (i + 1) / 24;
                vertex(pose, vc, c, argb(c(SandPalette.VIOLET_CORE), 0.5F * open * calm));
                vertex(pose, vc, c, argb(c(SandPalette.VIOLET_CORE), 0.5F * open * calm));
                vertex(pose, vc, c.add(right.scale(Math.cos(t1) * r * 0.95)).add(0.0, Math.sin(t1) * r * 0.95, 0.0), argb(c(SandPalette.VIOLET_BODY), 0.12F * open * calm));
                vertex(pose, vc, c.add(right.scale(Math.cos(t0) * r * 0.95)).add(0.0, Math.sin(t0) * r * 0.95, 0.0), argb(c(SandPalette.VIOLET_BODY), 0.12F * open * calm));
            }
        }
    }

    /** His Escape: a violet vortex that opens over his head and grows, a column of light rising out of it. */
    private static void escape(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, float partial, Vec3 camera, float time, float calm) {
        if (!(level.getEntity(fx.id) instanceof ApocalypseEntity boss)) {
            return;
        }
        float grow = Mth.clamp(age / Math.max(1, fx.c), 0.0F, 1.0F);
        float fade = 1.0F - AnimMath.ss(fx.c, fx.c + 12, age);
        Vec3 centre = boss.getPosition(partial).add(0.0, boss.getBbHeight() + 1.2, 0.0).subtract(camera);
        double radius = 0.8 + 2.7 * grow;
        SandShapes.flatRing(pose, vc, centre, radius, 0.3, 48, argb(c(SandPalette.VIOLET), 0.8F * fade * calm));
        SandShapes.flatRing(pose, vc, centre.add(0.0, 0.02, 0.0), radius * 0.6, 0.2, 40, argb(c(SandPalette.VIOLET_CORE), 0.7F * fade * calm));
        SandShapes.flatDisc(pose, vc, centre, radius, 32, argb(c(SandPalette.VIOLET_CORE), 0.45F * fade * calm), argb(c(SandPalette.VIOLET_BODY), 0.1F * fade * calm));
        for (int arm = 0; arm < 4; arm++) {
            double a0 = time * 0.2 + arm * Mth.HALF_PI;
            Vec3 previous = null;
            for (int i = 0; i <= 12; i++) {
                double r = radius * (1.0 - i / 12.0 * 0.9);
                double a = a0 + i * 0.35;
                Vec3 p = centre.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
                if (previous != null) {
                    EnergyGeometry.ribbon(pose, vc, previous, p, 0.07F, argb(c(SandPalette.VIOLET), 0.6F * fade * calm), argb(c(SandPalette.VIOLET_CORE), 0.8F * fade * calm));
                }
                previous = p;
            }
        }
        SandShapes.column(pose, vc, centre, 0.3 + 0.9 * grow, 14.0, 16, argb(c(SandPalette.VIOLET_CORE), 0.35F * fade * calm), argb(c(SandPalette.VIOLET), 0.0F));
    }

    /** The Phoenix's fire finishing him: a bird of flame rises behind him as his body burns away. */
    private static void firebird(ClientLevel level, Fx fx, float age, Matrix4f pose, VertexConsumer vc, Vec3 camera, float time) {
        if (!(level.getEntity(fx.id) instanceof ApocalypseEntity boss) || age < 20.0F) {
            return;
        }
        float rise = AnimMath.ss(20.0F, 60.0F, age);
        float fade = 1.0F - AnimMath.ss(fx.c - 10, fx.c + 6, age);
        float yaw = Mth.DEG_TO_RAD * boss.yBodyRot;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vec3 anchor = boss.position().add(forward.scale(-1.2)).add(0.0, 0.6, 0.0).subtract(camera);
        // The body of the bird climbs, the wings are swept out behind it, the tail trails below; each stroke is gold, orange and ember.
        double size = 2.6 * rise;
        float flap = 0.85F + 0.15F * Mth.sin(time * 0.35F);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 previous = anchor.add(0.0, size * 0.9, 0.0);
            for (int i = 1; i <= 8; i++) {
                double t = i / 8.0;
                Vec3 p = anchor.add(right.scale(side * size * 1.7 * t * flap)).add(0.0, size * (0.9 + 0.9 * Math.sin(t * 2.2) - 0.35 * t), 0.0)
                        .add(forward.scale(-0.25 * size * t));
                float f = fade * (1.0F - 0.35F * (float) t);
                EnergyGeometry.ribbon(pose, vc, previous, p, (float) (0.5 * size * (1.0 - t * 0.8)), argb(c(SandPalette.FIRE_EMBER), 0.3F * f), argb(c(SandPalette.FIRE_MAIN), 0.4F * f));
                EnergyGeometry.ribbon(pose, vc, previous, p, (float) (0.22 * size * (1.0 - t * 0.8)), argb(c(SandPalette.FIRE_GOLD), 0.5F * f), argb(c(SandPalette.FIRE_CORE), 0.6F * f));
                previous = p;
            }
        }
        Vec3 head = anchor.add(0.0, size * 2.4, 0.0);
        Vec3 bodyBottom = anchor.add(0.0, size * 0.2, 0.0);
        EnergyGeometry.ribbon(pose, vc, bodyBottom, head, (float) (0.45 * size), argb(c(SandPalette.FIRE_MAIN), 0.35F * fade), argb(c(SandPalette.FIRE_GOLD), 0.5F * fade));
        EnergyGeometry.ribbon(pose, vc, bodyBottom, head, (float) (0.18 * size), argb(c(SandPalette.FIRE_GOLD), 0.6F * fade), argb(c(SandPalette.FIRE_CORE), 0.8F * fade));
        // The tail: three streamers.
        for (int k = -1; k <= 1; k++) {
            Vec3 tip = anchor.add(right.scale(k * size * 0.35)).add(0.0, -size * 0.9, 0.0).add(forward.scale(-0.3 * size));
            EnergyGeometry.ribbon(pose, vc, bodyBottom, tip, (float) (0.22 * size), 0.0F, argb(c(SandPalette.FIRE_MAIN), 0.5F * fade), argb(c(SandPalette.FIRE_EMBER), 0.0F));
        }
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    private SandFx() {
    }
}
