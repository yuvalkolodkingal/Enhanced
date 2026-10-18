package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * The effects of flight, derived on every client from what it already knows about every player (no packet):
 * <ul>
 *     <li>trails laid along the path travelled each tick, so there are no gaps at sprint speed, from the places the
 *     model really has hands, feet, shoulders and a weapon ({@link HandTracker});</li>
 *     <li>ribbons drawn through the recent history of those places, a style per hero (crimson waves, golden threads, field
 *     lines, a double helix, lightning), and white wind streaks at speed;</li>
 *     <li>the take-off burst, the landing shockwave, the sonic-boom ring, and the crackle of a whirling weapon;</li>
 *     <li>a ribbon behind the thrown weapon.</li>
 * </ul>
 * A hero who was not drawn this frame gets nothing (off-screen heroes cost nothing); beyond 48 blocks the particles halve,
 * beyond 96 there is nothing. The switches {@link EnhancedClientConfig#flightEffects} and
 * {@link EnhancedClientConfig#reducedMotion} thin or still it.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class FlightEffects {
    /** Where the effects come from: indices of a hero's tracked points. */
    static final int HAND_R = 0;
    static final int HAND_L = 1;
    static final int FOOT_R = 2;
    static final int FOOT_L = 3;
    static final int SHOULDER_R = 4;
    static final int SHOULDER_L = 5;
    static final int FOREHEAD = 6;
    static final int WEAPON = 7;
    static final int SOURCES = 8;
    /** Ticks of history kept for each source. */
    static final int HISTORY = 8;
    private static final double TELEPORT = 6.0;
    private static final int MAX_RINGS_PER_PLAYER = 3;
    private static final int MAX_RINGS = 24;
    private static final int MAX_CRACKS = 12;
    private static final int RING_SEGMENTS = 24;
    private static final int BLUE = 0x4DB8FF;
    private static final int WHITE = 0xFFFFFF;

    /** The recent positions of one hero's sources, one per client tick, newest first. */
    static final class Fx {
        final Vec3[][] hist = new Vec3[SOURCES][HISTORY];
        final int[] count = new int[SOURCES];
        final Vec3[] live = new Vec3[SOURCES];
        boolean spinning;
        long lastNugget;
        long lastGlint;
        float lastPhase;
        long tick = -1;
    }

    private record Ring(int owner, Vec3 center, Vec3 normal, double start, float life, float r0, float r1, float width, int rgb, float alpha) {
    }

    private record Crack(Vec3 from, Vec3 to, double start, float life, long seed) {
    }

    private static final Map<Integer, Fx> FX = new HashMap<>();
    private static final Map<Integer, Vec3[]> THROWN = new HashMap<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Crack> CRACKS = new ArrayList<>();
    private static double shakeStart = Double.NEGATIVE_INFINITY;
    private static Vec3 shakeCenter = Vec3.ZERO;
    private static float shakeStrength;

    // ====================================================================================================================
    // Tick: trails along the path
    // ====================================================================================================================

    /** Called once per client tick, after the animation has been advanced. */
    public static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        long now = level.getGameTime();
        FX.keySet().removeIf(id -> !(level.getEntity(id) instanceof AbstractClientPlayer));
        RINGS.removeIf(r -> now - r.start() > r.life() + 2);
        CRACKS.removeIf(c -> now - c.start() > c.life() + 2);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        boolean thin = !EnhancedClientConfig.flightEffects();
        for (AbstractClientPlayer player : level.players()) {
            HeroAnimState st = HeroAnimator.peek(player);
            if (st == null || st.wVanilla > 0.0F || player.isInvisible() || player.isSpectator()) {
                FX.remove(player.getId());
                continue;
            }
            double distance = player.position().distanceTo(camera);
            if (distance > 96.0) {
                continue;
            }
            Fx fx = FX.computeIfAbsent(player.getId(), id -> new Fx());
            boolean local = player == mc.player && mc.options.getCameraType().isFirstPerson();
            gather(fx, player, st, local, mc, now);
            float density = (distance > 48.0 ? 0.5F : 1.0F) * (thin ? 0.1F : 1.0F);
            if (st.wAir > 0.3F) {
                flightParticles(level, player, st, fx, density, local);
            }
            if (st.weaponKind > 0 && st.wSpin > 0.3F) {
                weaponCrackle(level, player, st, fx, density);
            } else {
                fx.spinning = false;
            }
            if (st.power == Power.QUICKSILVER || st.power == Power.TOMMY || st.kitSources.contains(Power.QUICKSILVER)
                    || st.kitSources.contains(Power.TOMMY)) {
                footSplashes(level, player, st, fx);
            }
        }
        // Thrown weapons: a ribbon of the last six positions.
        THROWN.keySet().removeIf(id -> !(level.getEntity(id) instanceof ThrownWeaponEntity));
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ThrownWeaponEntity weapon) {
                Vec3[] trail = THROWN.computeIfAbsent(weapon.getId(), id -> new Vec3[6]);
                if (trail[0] != null && trail[0].distanceTo(weapon.position()) > TELEPORT) {
                    java.util.Arrays.fill(trail, null);
                }
                System.arraycopy(trail, 0, trail, 1, trail.length - 1);
                trail[0] = weapon.position().add(0.0, 0.3, 0.0);
            }
        }
    }

    /** Pushes this tick's position of every source into its history, from the points the model was drawn with. */
    private static void gather(Fx fx, AbstractClientPlayer player, HeroAnimState st, boolean local, Minecraft mc, long now) {
        if (fx.tick == now) {
            return;
        }
        fx.tick = now;
        Vec3[] pts = new Vec3[SOURCES];
        if (local) {
            // No body is drawn in first person: the feet are where the player is, the hands where the effects leave them.
            pts[FOOT_R] = player.position();
            pts[FOOT_L] = player.position();
            Vec3 hand = WorldEffectsRenderer.handPosition(mc, player, 1.0F);
            pts[HAND_R] = hand;
            pts[HAND_L] = hand;
        } else {
            pts[HAND_R] = HandTracker.hand(player, HumanoidArm.RIGHT, 0.0F);
            pts[HAND_L] = HandTracker.hand(player, HumanoidArm.LEFT, 0.0F);
            pts[FOOT_R] = HandTracker.foot(player, HumanoidArm.RIGHT, 0.0F);
            pts[FOOT_L] = HandTracker.foot(player, HumanoidArm.LEFT, 0.0F);
            pts[SHOULDER_R] = HandTracker.shoulder(player, HumanoidArm.RIGHT, 0.0F);
            pts[SHOULDER_L] = HandTracker.shoulder(player, HumanoidArm.LEFT, 0.0F);
            pts[FOREHEAD] = HandTracker.forehead(player, 0.0F);
            pts[WEAPON] = HandTracker.weaponHead(player, 0.0F);
        }
        for (int i = 0; i < SOURCES; i++) {
            Vec3 p = pts[i];
            Vec3[] ring = fx.hist[i];
            if (p == null) {
                fx.count[i] = 0;
                continue;
            }
            if (fx.count[i] > 0 && ring[0].distanceTo(p) > TELEPORT) {
                fx.count[i] = 0;
            }
            System.arraycopy(ring, 0, ring, 1, HISTORY - 1);
            ring[0] = p;
            fx.count[i] = Math.min(fx.count[i] + 1, HISTORY);
        }
    }

    /** The point a source was at {@code back} ticks ago (0 = this tick), or null. */
    static Vec3 at(Fx fx, int source, int back) {
        return fx.count[source] > back ? fx.hist[source][back] : null;
    }

    /**
     * Lays particles evenly along the path a source travelled since the last tick, so there are no gaps at any speed.
     * Returns how many were laid.
     */
    static int lay(ClientLevel level, Fx fx, int source, double spacing, int cap, double jitter, int budget, float density,
                           java.util.function.IntFunction<ParticleOptions> options, double drift) {
        Vec3 cur = at(fx, source, 0);
        Vec3 prev = at(fx, source, 1);
        if (cur == null || prev == null || budget <= 0) {
            return 0;
        }
        double d = prev.distanceTo(cur);
        RandomSource random = level.random;
        int n = Mth.clamp((int) Math.round(d / spacing), 1, cap);
        n = Math.min(n, budget);
        int laid = 0;
        Vec3 motion = cur.subtract(prev);
        for (int i = 1; i <= n; i++) {
            if (density < 1.0F && random.nextFloat() > density) {
                continue;
            }
            Vec3 p = prev.lerp(cur, i / (double) n);
            level.addParticle(options.apply(i), p.x + (random.nextDouble() - 0.5) * jitter, p.y + (random.nextDouble() - 0.5) * jitter,
                    p.z + (random.nextDouble() - 0.5) * jitter, -0.1 * motion.x + (random.nextDouble() - 0.5) * 0.01,
                    -0.1 * motion.y + drift, -0.1 * motion.z + (random.nextDouble() - 0.5) * 0.01);
            laid++;
        }
        return laid;
    }

    /** The particles of a hero in the air. */
    private static void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, Fx fx, float density, boolean local) {
        RandomSource random = level.random;
        // 3.4: whose flight it is (a custom hero's style), in his own colours where the hero's were his.
        Power power = st.flightAs;
        int wandaMain = CustomActing.tint(player, Power.SCARLET_WITCH.color, false);
        int wandaGlow = CustomActing.tint(player, Power.SCARLET_WITCH.glow, true);
        boolean moving = st.sH > 0.05F;
        boolean sprint = st.s >= 1.8F;
        int budget = sprint ? 12 : 6;
        long now = level.getGameTime();
        switch (power) {
            case SCARLET_WITCH -> {
                for (int source : new int[]{HAND_R, HAND_L}) {
                    if (moving) {
                        budget -= lay(level, fx, source, 0.35, 3, 0.15, budget, density,
                                i -> new EnergyParticleOptions(i % 3 == 2 ? wandaGlow : wandaMain, 1.0F), 0.0);
                    } else if (now % 3 == 0 && at(fx, source, 0) != null && random.nextFloat() < density) {
                        Vec3 p = at(fx, source, 0);
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? wandaMain : wandaGlow, 1.0F),
                                p.x + (random.nextDouble() - 0.5) * 0.15, p.y + (random.nextDouble() - 0.5) * 0.15, p.z + (random.nextDouble() - 0.5) * 0.15,
                                0, 0.01, 0);
                    }
                }
                if (sprint) {
                    for (int source : new int[]{FOOT_R, FOOT_L}) {
                        budget -= lay(level, fx, source, 0.5, 1, 0.1, budget, density, i -> new EnergyParticleOptions(wandaMain, 1.0F), 0.0);
                    }
                }
            }
            case VISION -> {
                boolean white = st.flightWhite;
                int main = white ? 0x8FDFFF : CustomActing.tint(player, Power.VISION.color, false);
                int accent = white ? 0xE8FBFF : CustomActing.tint(player, Power.VISION.glow, true);
                for (int source : new int[]{SHOULDER_R, SHOULDER_L}) {
                    if (moving) {
                        budget -= lay(level, fx, source, 0.8, 2, 0.12, budget, density, i -> new EnergyParticleOptions(i % 2 == 0 ? main : accent, 0.8F), 0.0);
                    } else if (now % 2 == 0 && at(fx, source, 0) != null && random.nextFloat() < density) {
                        Vec3 p = at(fx, source, 0);
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? main : accent, 0.8F),
                                p.x + (random.nextDouble() - 0.5) * 0.2, p.y + (random.nextDouble() - 0.5) * 0.2, p.z + (random.nextDouble() - 0.5) * 0.2, 0, 0.008, 0);
                    }
                }
            }
            case MAGNETO -> {
                if (!moving) {
                    // The haze of metal dust under his feet.
                    for (int source : new int[]{FOOT_R, FOOT_L}) {
                        Vec3 p = at(fx, source, 0);
                        if (p != null && random.nextFloat() < density) {
                            level.addParticle(new EnergyParticleOptions(CustomActing.tint(player, Power.MAGNETO.glow, true), 0.9F),
                                    p.x + (random.nextDouble() - 0.5) * 0.5,
                                    p.y - 0.05, p.z + (random.nextDouble() - 0.5) * 0.5, 0, -0.03, 0);
                        }
                    }
                } else {
                    for (int source : new int[]{FOOT_R, FOOT_L, HAND_R, HAND_L}) {
                        budget -= lay(level, fx, source, 0.6, 2, 0.15, budget, density,
                                i -> new EnergyParticleOptions(i % 2 == 0 ? CustomActing.tint(player, Power.MAGNETO.color, false)
                                        : CustomActing.tint(player, Power.MAGNETO.glow, true), 0.9F), 0.0);
                    }
                }
                if (sprint && now - fx.lastNugget >= 3 && at(fx, HAND_R, 0) != null) {
                    fx.lastNugget = now;
                    Vec3 p = at(fx, HAND_R, 0);
                    level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET)), p.x, p.y, p.z,
                            (random.nextDouble() - 0.5) * 0.1, 0.02, (random.nextDouble() - 0.5) * 0.1);
                }
            }
            case BILLY -> {
                for (int source : new int[]{HAND_R, HAND_L, FOOT_R, FOOT_L}) {
                    budget -= lay(level, fx, source, 0.5, 2, 0.2, budget, density, i -> new EnergyParticleOptions(
                            i % 3 == 2 ? 0xCFEFFF : (i % 5 == 0 ? 0x2F8CFF : CustomActing.tint(player, Power.BILLY.color, false)), 0.8F), 0.005);
                }
                if (sprint && now - fx.lastGlint >= 5 && at(fx, HAND_L, 0) != null) {
                    fx.lastGlint = now;
                    Vec3 p = at(fx, HAND_L, 0);
                    level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0.01, 0);
                }
            }
            case THOR -> {
                for (int source : new int[]{FOOT_R, FOOT_L, WEAPON, HAND_R, HAND_L}) {
                    if (source == HAND_R || source == HAND_L) {
                        if (!moving) {
                            continue;
                        }
                    }
                    budget -= lay(level, fx, source, 0.30, 4, 0.12, budget, density,
                            i -> new EnergyParticleOptions(i % 2 == 0 ? BLUE : Power.THOR.glow, 0.7F), 0.0);
                }
                Vec3 boot = at(fx, FOOT_R, 0);
                if (moving && boot != null && budget > 0) {
                    for (int i = 0; i < 1 && random.nextFloat() < density; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, boot.x + (random.nextDouble() - 0.5) * 0.3, boot.y + (random.nextDouble() - 0.5) * 0.3,
                                boot.z + (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1,
                                (random.nextDouble() - 0.5) * 0.1);
                    }
                }
            }
            default -> {
                HeroClient hero = HeroClients.of(power);
                if (hero != null) {
                    hero.flightParticles(level, player, st, fx, density, local);
                }
            }
        }
        // First person: the air streams past the camera.
        if (local && st.s >= 1.3F && EnhancedClientConfig.flightEffects() && !EnhancedClientConfig.reducedMotion()) {
            streamPast(level, player, density);
        }
    }

    /** Three white motes per tick on a ring around the view axis ahead of the camera, streaming past (first person). */
    private static void streamPast(ClientLevel level, AbstractClientPlayer player, float density) {
        RandomSource random = level.random;
        Vec3 look = player.getLookAngle();
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        Vec3 above = right.cross(look).normalize();
        Vec3 motion = player.position().subtract(new Vec3(player.xo, player.yo, player.zo));
        for (int i = 0; i < 3; i++) {
            if (random.nextFloat() > density) {
                continue;
            }
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = 1.2 + random.nextDouble() * 1.8;
            Vec3 p = player.getEyePosition().add(look.scale(3.0 + random.nextDouble() * 2.0))
                    .add(right.scale(Math.cos(angle) * radius)).add(above.scale(Math.sin(angle) * radius));
            level.addParticle(new EnergyParticleOptions(WHITE, 0.2F), p.x, p.y, p.z, -0.8 * motion.x, -0.8 * motion.y, -0.8 * motion.z);
        }
    }

    /** Sparks flying off the head of a whirling weapon, along its path, and a low whoosh as the spin starts. */
    private static void weaponCrackle(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, Fx fx, float density) {
        Vec3 head = at(fx, WEAPON, 0);
        Vec3 before = at(fx, WEAPON, 1);
        if (!fx.spinning) {
            fx.spinning = true;
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.5F, 0.7F, false);
        }
        if (head == null || before == null) {
            return;
        }
        RandomSource random = level.random;
        Vec3 tangent = head.subtract(before);
        double speed = tangent.length();
        tangent = speed > 1.0E-4 ? tangent.scale(1.0 / speed) : Vec3.ZERO;
        for (int i = 0; i < 2 && random.nextFloat() < density; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, head.x, head.y, head.z, tangent.x * 0.25 + (random.nextDouble() - 0.5) * 0.1,
                    tangent.y * 0.25 + (random.nextDouble() - 0.5) * 0.1, tangent.z * 0.25 + (random.nextDouble() - 0.5) * 0.1);
        }
        if (level.getGameTime() % 2 == 0 && random.nextFloat() < density) {
            level.addParticle(new EnergyParticleOptions(BLUE, 0.7F), head.x, head.y, head.z, 0.0, 0.0, 0.0);
        }
    }

    /** A speedster's feet strike water: a small splash at the striking foot (the phase crossing a multiple of pi). */
    private static void footSplashes(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, Fx fx) {
        float phase = st.runPhase;
        boolean strike = Math.floor(phase / Mth.PI) != Math.floor(fx.lastPhase / Mth.PI);
        fx.lastPhase = phase;
        if (!strike || st.wRun < 0.5F || player.isInWater()) {
            return;
        }
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        if (!level.getFluidState(below).is(FluidTags.WATER)) {
            return;
        }
        boolean right = Math.cos(phase) > 0.0;
        Vec3 foot = at(fx, right ? FOOT_R : FOOT_L, 0);
        if (foot == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.SPLASH, foot.x, below.getY() + 1.0, foot.z, (level.random.nextDouble() - 0.5) * 0.2, 0.15, (level.random.nextDouble() - 0.5) * 0.2);
        }
    }

    // ====================================================================================================================
    // Bursts, rings, impacts
    // ====================================================================================================================

    private static void addRing(ClientLevel level, int owner, Vec3 center, Vec3 normal, double delay, float life, float r0, float r1, float width,
                                int rgb, float alpha) {
        if (!EnhancedClientConfig.flightEffects()) {
            return;
        }
        float a = EnhancedClientConfig.reducedMotion() ? alpha * 0.5F : alpha;
        int mine = 0;
        for (Ring r : RINGS) {
            if (r.owner() == owner) {
                mine++;
            }
        }
        if (mine >= MAX_RINGS_PER_PLAYER) {
            for (int i = 0; i < RINGS.size(); i++) {
                if (RINGS.get(i).owner() == owner) {
                    RINGS.remove(i);
                    break;
                }
            }
        }
        if (RINGS.size() >= MAX_RINGS) {
            RINGS.remove(0);
        }
        RINGS.add(new Ring(owner, center, normal, level.getGameTime() + delay, life, r0, r1, width, rgb, a));
    }

    /** A ring for another effect (a gesture's flash): same limits as the flight rings. */
    static void ring(ClientLevel level, int owner, Vec3 center, Vec3 normal, float life, float r0, float r1, float width, int rgb, float alpha) {
        addRing(level, owner, center, normal, 0, life, r0, r1, width, rgb, alpha);
    }

    private static void addCrack(ClientLevel level, Vec3 from, Vec3 to, float life, long seed) {
        if (!EnhancedClientConfig.flightEffects()) {
            return;
        }
        if (CRACKS.size() >= MAX_CRACKS) {
            CRACKS.remove(0);
        }
        CRACKS.add(new Crack(from, to, level.getGameTime(), life, seed));
    }

    private static void sound(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    /** A burst of motes leaving a point outward and up. */
    private static void burst(ClientLevel level, Vec3 at, int count, double radial, double up, java.util.function.IntFunction<ParticleOptions> options) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = radial * (0.6 + random.nextDouble() * 0.7);
            level.addParticle(options.apply(i), at.x, at.y + 0.05, at.z, Math.cos(angle) * speed, up * (0.5 + random.nextDouble()), Math.sin(angle) * speed);
        }
    }

    /** The take-off: a ring on the ground at the feet and a burst in the hero's own colours. */
    static void takeoff(AbstractClientPlayer player, HeroAnimState st, Power power) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        Vec3 feet = player.position();
        boolean thor = power == Power.THOR;
        // (3.4: a custom hero takes off in his own colours, in the manner of his flight style.)
        int color = CustomActing.tint(player, power.color, false);
        int glow = CustomActing.tint(player, power.glow, true);
        addRing(level, player.getId(), feet.add(0, 0.05, 0), new Vec3(0, 1, 0), 0, 9, 0.4F, thor ? 3.2F : 2.4F, 0.10F, color, 0.8F);
        float density = EnhancedClientConfig.flightEffects() ? 1.0F : 0.1F;
        burst(level, feet, Math.round(12 * density), 0.16, 0.15, i -> new EnergyParticleOptions(i % 2 == 0 ? color : glow, 1.0F));
        if (thor) {
            RandomSource random = level.random;
            for (int i = 0; i < Math.round(8 * density); i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x, feet.y + 0.1, feet.z, (random.nextDouble() - 0.5) * 0.4, 0.1 + random.nextDouble() * 0.2,
                        (random.nextDouble() - 0.5) * 0.4);
            }
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                addCrack(level, feet.add(0, 0.04, 0), feet.add(Math.cos(angle) * 2.0, 0.04, Math.sin(angle) * 2.0), 5, random.nextLong());
            }
            sound(level, feet, ModSounds.QUICKSILVER_WHOOSH.get(), 0.5F, 0.6F);
        } else if (power == Power.MAGNETO) {
            burst(level, feet, Math.round(10 * density), 0.12, 0.05, i -> new EnergyParticleOptions(color, 0.9F));
        }
    }

    /** A gentle touchdown: a small ring and a puff of dust off the ground. */
    static void softLanding(AbstractClientPlayer player, HeroAnimState st, float fall) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        Vec3 feet = player.position();
        addRing(level, player.getId(), feet.add(0, 0.05, 0), new Vec3(0, 1, 0), 0, 7, 0.3F, 1.6F, 0.08F, WHITE, 0.5F);
        dust(level, player, feet, 8, 0.4, 0.08, 0.04);
    }

    /** The hero landing and the Bifrost's arrival: rings, dust, cracks in the ground, a thud and a shake of the camera. */
    static void impact(AbstractClientPlayer player, HeroAnimState st, float strength) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 feet = player.position();
        RandomSource random = level.random;
        addRing(level, player.getId(), feet.add(0, 0.05, 0), new Vec3(0, 1, 0), 0, 12, 0.5F, 4.5F * strength, 0.16F, BLUE, 0.9F);
        addRing(level, player.getId(), feet.add(0, 0.06, 0), new Vec3(0, 1, 0), 0, 16, 0.3F, 3.0F * strength, 0.10F, WHITE, 0.5F);
        dust(level, player, feet, 20, 0.5, 0.2, 0.12);
        float density = EnhancedClientConfig.flightEffects() ? 1.0F : 0.1F;
        burst(level, feet, Math.round(14 * density), 0.22, 0.14, i -> new EnergyParticleOptions(i % 2 == 0 ? BLUE : Power.THOR.glow, 1.0F));
        for (int i = 0; i < 6; i++) {
            double angle = i * Mth.TWO_PI / 6 + random.nextDouble() * 0.4;
            addCrack(level, feet.add(0, 0.04, 0), feet.add(Math.cos(angle) * 2.5 * strength, 0.04, Math.sin(angle) * 2.5 * strength), 6, random.nextLong());
        }
        sound(level, feet, ModSounds.MJOLNIR_IMPACT.get(), 0.8F, strength > 1.0F ? 0.7F : 0.8F);
        if (!EnhancedClientConfig.reducedMotion()) {
            shakeStart = level.getGameTime();
            shakeCenter = feet;
            shakeStrength = strength;
        }
    }

    /** A dense Vision lands like a statue: one small golden ring and a few motes of dust. */
    static void denseLanding(AbstractClientPlayer player, HeroAnimState st) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 feet = player.position();
        addRing(level, player.getId(), feet.add(0, 0.05, 0), new Vec3(0, 1, 0), 0, 8, 0.3F, 1.4F, 0.08F,
                CustomActing.tint(player, Power.VISION.color, false), 0.8F);
        dust(level, player, feet, 6, 0.3, 0.06, 0.03);
    }

    /** Block dust off the ground below, thrown outward. */
    private static void dust(ClientLevel level, AbstractClientPlayer player, Vec3 at, int count, double radius, double radial, double up) {
        BlockState below = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
        if (below.isAir() || !EnhancedClientConfig.flightEffects()) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = radius * random.nextDouble();
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), at.x + Math.cos(angle) * r, at.y + 0.1, at.z + Math.sin(angle) * r,
                    Math.cos(angle) * radial, up + random.nextDouble() * 0.05, Math.sin(angle) * radial);
        }
    }

    /** The sonic boom: two rings across the flight path and a puff of cloud, as the hero breaks the sound barrier. */
    static void boom(AbstractClientPlayer player, HeroAnimState st, Power power) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        Vec3 velocity = player.position().subtract(new Vec3(player.xo, player.yo, player.zo));
        if (velocity.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = velocity.normalize();
        Vec3 center = player.position().add(0, 0.9, 0).add(dir.scale(0.2));
        addRing(level, player.getId(), center, dir, 0, 14, 0.6F, 7.0F, 0.20F, WHITE, 0.7F);
        addRing(level, player.getId(), center, dir, 3, 12, 0.4F, 5.0F, 0.12F, power.color, 0.8F);
        // Cloud flung out across the path.
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = dir.cross(u).normalize();
        RandomSource random = level.random;
        int n = EnhancedClientConfig.flightEffects() ? 20 : 2;
        for (int i = 0; i < n; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 radial = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            level.addParticle(ParticleTypes.CLOUD, center.x, center.y, center.z, radial.x * 0.25, radial.y * 0.25, radial.z * 0.25);
        }
        sound(level, center, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 0.8F, 0.7F);
    }

    // ====================================================================================================================
    // Render: ribbons, rings, cracks, streaks, the weapon's trail
    // ====================================================================================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !EnhancedClientConfig.flightEffects() || (FX.isEmpty() && RINGS.isEmpty() && CRACKS.isEmpty() && THROWN.isEmpty())) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);

        for (var entry : FX.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            HeroAnimState st = HeroAnimator.peek(player);
            if (st == null || st.wVanilla > 0.0F) {
                continue;
            }
            double distance = player.position().distanceTo(camera);
            if (distance > 64.0) {
                continue;
            }
            Fx fx = entry.getValue();
            updateLive(fx, player, partialTick);
            if (st.wAir > 0.3F) {
                ribbons(pose, vc, camera, player, st, fx, time);
                if (st.s >= 1.3F && !EnhancedClientConfig.reducedMotion()) {
                    streaks(pose, vc, camera, player, st, fx);
                }
                if (st.flightAs == Power.THOR && st.s >= 1.0F) {
                    torsoArcs(pose, vc, camera, player, partialTick, time);
                }
            }
            if (st.weaponKind > 0 && st.wSpin > 0.3F) {
                weaponBolt(pose, vc, camera, player, fx, time);
            }
        }
        for (Ring ring : RINGS) {
            double age = time - ring.start();
            if (age < 0 || age > ring.life()) {
                continue;
            }
            float t = (float) (age / ring.life());
            float r = AnimMath.lerp(AnimMath.easeOutCubic(t), ring.r0(), ring.r1());
            float alpha = ring.alpha() * (1.0F - t) * (1.0F - t);
            EnergyGeometry.ring(pose, vc, ring.center().subtract(camera), ring.normal(), r, ring.width(), RING_SEGMENTS, EnergyGeometry.argb(ring.rgb(), alpha));
        }
        for (Crack crack : CRACKS) {
            double age = time - crack.start();
            if (age < 0 || age > crack.life()) {
                continue;
            }
            float fade = 1.0F - (float) (age / crack.life());
            Vec3 a = crack.from().subtract(camera);
            Vec3 b = crack.to().subtract(camera);
            EnergyGeometry.bolt(pose, vc, a, b, 0.07F, 0.18F, EnergyGeometry.argb(BLUE, 0.9F * fade), crack.seed());
            EnergyGeometry.bolt(pose, vc, a, b, 0.025F, 0.18F, EnergyGeometry.argb(WHITE, fade), crack.seed());
        }
        for (var entry : THROWN.entrySet()) {
            Vec3[] trail = entry.getValue();
            Entity weapon = level.getEntity(entry.getKey());
            if (weapon == null || trail[0] == null) {
                continue;
            }
            Vec3 head = weapon.getPosition(partialTick).add(0.0, 0.3, 0.0);
            polyline(pose, vc, camera, head, trail, trail.length, 6.0, 0.12F, BLUE, 0.6F, 0.0F, 0.0F, time);
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** This frame's position of each source: the tracked point, falling back to the last tick's. */
    private static void updateLive(Fx fx, AbstractClientPlayer player, float partialTick) {
        fx.live[HAND_R] = orElse(HandTracker.hand(player, HumanoidArm.RIGHT, partialTick), at(fx, HAND_R, 0));
        fx.live[HAND_L] = orElse(HandTracker.hand(player, HumanoidArm.LEFT, partialTick), at(fx, HAND_L, 0));
        fx.live[FOOT_R] = orElse(HandTracker.foot(player, HumanoidArm.RIGHT, partialTick), at(fx, FOOT_R, 0));
        fx.live[FOOT_L] = orElse(HandTracker.foot(player, HumanoidArm.LEFT, partialTick), at(fx, FOOT_L, 0));
        fx.live[SHOULDER_R] = orElse(HandTracker.shoulder(player, HumanoidArm.RIGHT, partialTick), at(fx, SHOULDER_R, 0));
        fx.live[SHOULDER_L] = orElse(HandTracker.shoulder(player, HumanoidArm.LEFT, partialTick), at(fx, SHOULDER_L, 0));
        fx.live[FOREHEAD] = orElse(HandTracker.forehead(player, partialTick), at(fx, FOREHEAD, 0));
        fx.live[WEAPON] = orElse(HandTracker.weaponHead(player, partialTick), at(fx, WEAPON, 0));
    }

    private static Vec3 orElse(Vec3 a, Vec3 b) {
        return a != null ? a : b;
    }

    /** The ribbons of a hero: a style each, laid through the recent history of their sources. */
    private static void ribbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st, Fx fx, float time) {
        float sprint = st.s >= 1.8F ? 1.4F : 1.0F;
        long id = player.getId();
        // 3.4: whose flight it is (a custom hero's style), in his own colours where the hero's were his.
        switch (st.flightAs) {
            case SCARLET_WITCH -> {
                int[] hands = {HAND_R, HAND_L};
                int wanda = CustomActing.tint(player, Power.SCARLET_WITCH.color, false);
                for (int i = 0; i < hands.length; i++) {
                    source(pose, vc, camera, fx, hands[i], 3.5 * sprint, 0.07F, wanda, 0.55F, 0.10F, i * 0.9F, 0, time);
                }
            }
            case VISION -> {
                boolean white = st.flightWhite;
                int color = white ? 0x8FDFFF : CustomActing.tint(player, Power.VISION.color, false);
                source(pose, vc, camera, fx, SHOULDER_R, 3.0, 0.05F, color, 0.40F, 0.0F, 0, 0, time);
                source(pose, vc, camera, fx, SHOULDER_L, 3.0, 0.05F, color, 0.40F, 0.0F, 0, 0, time);
                if (st.s >= 1.8F) {
                    source(pose, vc, camera, fx, FOREHEAD, 4.0, 0.10F, white ? 0xE8FBFF : CustomActing.tint(player, Power.VISION.glow, true), 0.5F,
                            0.0F, 0, 0, time);
                }
            }
            case MAGNETO -> {
                // Field lines from the hands, in a gentle spiral.
                int field = CustomActing.tint(player, Power.MAGNETO.glow, true);
                source(pose, vc, camera, fx, HAND_R, 2.5, 0.03F, field, 0.30F, 0.0F, 0, 0.08F, time);
                source(pose, vc, camera, fx, HAND_L, 2.5, 0.03F, field, 0.30F, 0.0F, 0, -0.08F, time);
            }
            case BILLY -> {
                // A double helix around the path of each hand.
                helix(pose, vc, camera, fx, HAND_R, 3.0, time);
                helix(pose, vc, camera, fx, HAND_L, 3.0, time);
            }
            case THOR -> {
                // Lightning from the boots along their path, writhing (3.3: it jumped to a new shape every other tick).
                for (int source : new int[]{FOOT_R, FOOT_L}) {
                    Vec3 head = fx.live[source];
                    if (head == null || fx.count[source] < 2) {
                        continue;
                    }
                    long seed = (id + source) * 7919L;
                    Vec3 prev = head;
                    double length = 0.0;
                    for (int k = 0; k < fx.count[source] && length < 4.0; k++) {
                        Vec3 next = fx.hist[source][k];
                        length += prev.distanceTo(next);
                        float fade = (float) Math.max(0.0, 1.0 - length / 4.0);
                        EnergyGeometry.bolt(pose, vc, prev.subtract(camera), next.subtract(camera), 0.04F, 0.12F,
                                EnergyGeometry.argb(BLUE, 0.8F * fade),
                                seed + k, time / 2.0F);
                        EnergyGeometry.bolt(pose, vc, prev.subtract(camera), next.subtract(camera), 0.015F, 0.12F,
                                EnergyGeometry.argb(WHITE, 0.8F * fade),
                                seed + k, time / 2.0F);
                        prev = next;
                    }
                }
            }
            default -> {
                HeroClient hero = HeroClients.of(st.flightAs);
                if (hero != null) {
                    hero.flightRibbons(pose, vc, camera, player, st, fx, time);
                }
            }
        }
    }

    /** One ribbon from a source through its history. */
    static void source(Matrix4f pose, VertexConsumer vc, Vec3 camera, Fx fx, int source, double length, float width, int rgb, float alpha,
                               float wobble, float phase, float spiral, float time) {
        Vec3 head = fx.live[source];
        if (head == null || fx.count[source] < 1) {
            return;
        }
        polyline(pose, vc, camera, head, fx.hist[source], fx.count[source], length, width, rgb, alpha, wobble, phase, time);
        if (spiral != 0.0F) {
            // The field lines turn about their path.
            polyline(pose, vc, camera, head.add(0, spiral * Mth.sin(time * 0.5F), 0), fx.hist[source], fx.count[source], length * 0.8, width, rgb, alpha * 0.7F,
                    wobble, phase + 2.0F, time);
        }
    }

    /**
     * A ribbon along a path: from {@code head} back through {@code points} for {@code length} blocks, narrowing and fading to
     * nothing, with an optional wave across it.
     */
    static void polyline(Matrix4f pose, VertexConsumer vc, Vec3 camera, Vec3 head, Vec3[] points, int count, double length, float width, int rgb,
                                 float alpha, float wobble, float phase, float time) {
        Vec3 prev = head;
        double along = 0.0;
        for (int k = 0; k < count && along < length; k++) {
            Vec3 next = points[k];
            if (next == null) {
                break;
            }
            double step = prev.distanceTo(next);
            if (step < 1.0E-3) {
                continue;
            }
            double t0 = along / length;
            double t1 = Math.min(1.0, (along + step) / length);
            along += step;
            Vec3 a = prev;
            Vec3 b = next;
            if (wobble != 0.0F) {
                Vec3 dir = b.subtract(a).normalize();
                Vec3 side = dir.cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
                a = a.add(side.scale(wobble * Math.sin(time * 0.45 + phase + (k) * 0.9) * t0));
                b = b.add(side.scale(wobble * Math.sin(time * 0.45 + phase + (k + 1) * 0.9) * t1));
            }
            EnergyGeometry.ribbon(pose, vc, a.subtract(camera), b.subtract(camera), (float) (width * (1.0 - t0)), (float) (width * (1.0 - t1)),
                    EnergyGeometry.argb(rgb, (float) (alpha * (1.0 - t0))), EnergyGeometry.argb(rgb, (float) (alpha * (1.0 - t1))));
            prev = next;
        }
    }

    /** Billy's trail: two strands of light turning about the path, half a turn every one and a half blocks. */
    private static void helix(Matrix4f pose, VertexConsumer vc, Vec3 camera, Fx fx, int source, double length, float time) {
        Vec3 head = fx.live[source];
        if (head == null || fx.count[source] < 2) {
            return;
        }
        Vec3 prevA = null;
        Vec3 prevB = null;
        Vec3 prev = head;
        double along = 0.0;
        for (int k = 0; k < fx.count[source] && along < length; k++) {
            Vec3 next = fx.hist[source][k];
            Vec3 dir = prev.subtract(next);
            if (dir.lengthSqr() < 1.0E-6) {
                continue;
            }
            dir = dir.normalize();
            Vec3 u = dir.cross(new Vec3(0, 1, 0));
            u = u.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : u.normalize();
            Vec3 v = dir.cross(u).normalize();
            double angle = Math.PI * along / 1.5 + time * 0.2;
            Vec3 offset = u.scale(Math.cos(angle) * 0.35).add(v.scale(Math.sin(angle) * 0.35));
            Vec3 a = prev.add(offset);
            Vec3 b = prev.subtract(offset);
            float t = (float) (along / length);
            if (prevA != null) {
                EnergyGeometry.ribbon(pose, vc, prevA.subtract(camera), a.subtract(camera), 0.04F * (1 - t), 0.04F * (1 - t),
                        EnergyGeometry.argb(0x5CC8FF, 0.5F * (1 - t)), EnergyGeometry.argb(0x5CC8FF, 0.5F * (1 - t)));
                EnergyGeometry.ribbon(pose, vc, prevB.subtract(camera), b.subtract(camera), 0.04F * (1 - t), 0.04F * (1 - t),
                        EnergyGeometry.argb(0xCFEFFF, 0.5F * (1 - t)), EnergyGeometry.argb(0xCFEFFF, 0.5F * (1 - t)));
            }
            prevA = a;
            prevB = b;
            along += prev.distanceTo(next);
            prev = next;
        }
    }

    /** Thin white air lines from the hands and feet at speed. */
    private static void streaks(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st, Fx fx) {
        double length = 1.5 + 2.5 * ss(1.3F, 2.3F, st.s);
        float alpha = 0.45F * ss(1.3F, 1.8F, st.s);
        int glow = CustomActing.tint(player, st.power.glow, true);
        int rgb = ((int) (0xFF * 0.7 + ((glow >> 16) & 0xFF) * 0.3) << 16) | ((int) (0xFF * 0.7 + ((glow >> 8) & 0xFF) * 0.3) << 8)
                | (int) (0xFF * 0.7 + (glow & 0xFF) * 0.3);
        for (int source : new int[]{HAND_R, HAND_L, FOOT_R, FOOT_L}) {
            Vec3 head = fx.live[source];
            if (head != null && fx.count[source] > 1) {
                polyline(pose, vc, camera, head, fx.hist[source], fx.count[source], length, 0.035F, rgb, alpha, 0.0F, 0.0F, 0.0F);
            }
        }
    }

    /** Short arcs of lightning crawling over a fast Thor's torso, a new pair every other tick,
            fading in and out (3.3). */
    private static void torsoArcs(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, float partialTick, float time) {
        Vec3 feet = player.getPosition(partialTick);
        EnergyGeometry.crossfade(player.getId() * 31L, time / 2.0F, (random, weight) -> {
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double y = 0.5 + random.nextDouble() * 1.0;
                Vec3 a = BodyLean.follow(player, feet.add(Math.cos(angle) * 0.3, y, Math.sin(angle) * 0.3),
                        partialTick).subtract(camera);
                Vec3 b = a.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6,
                        (random.nextDouble() - 0.5) * 0.6);
                EnergyGeometry.bolt(pose, vc, a, b, 0.03F, 0.1F, EnergyGeometry.argb(BLUE, 0.8F * weight),
                        random.nextLong());
                EnergyGeometry.bolt(pose, vc, a, b, 0.012F, 0.1F, EnergyGeometry.argb(WHITE, 0.9F * weight),
                        random.nextLong());
            }
        });
    }

    /**
     * A short bolt from the whirling weapon's head to a point half a block away, a new one every other tick, fading in
     * and out (3.3).
     */
    private static void weaponBolt(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, Fx fx, float time) {
        Vec3 head = fx.live[WEAPON];
        if (head == null) {
            return;
        }
        EnergyGeometry.crossfade(player.getId() * 31L + 7L, time / 2.0F, (random, weight) -> {
            Vec3 end = head.add((random.nextDouble() - 0.5) * 1.0, (random.nextDouble() - 0.5) * 1.0,
                    (random.nextDouble() - 0.5) * 1.0);
            EnergyGeometry.bolt(pose, vc, head.subtract(camera), end.subtract(camera), 0.02F, 0.10F,
                    EnergyGeometry.argb(BLUE, 0.8F * weight),
                    random.nextLong());
            EnergyGeometry.bolt(pose, vc, head.subtract(camera), end.subtract(camera), 0.01F, 0.10F,
                    EnergyGeometry.argb(WHITE, 0.9F * weight),
                    random.nextLong());
        });
    }

    /** A hard landing shakes the camera: a quick roll that dies away, strongest near the impact. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        double age = level.getGameTime() + (double) event.getPartialTick() - shakeStart;
        if (age < 0 || age > 8) {
            return;
        }
        double distance = event.getCamera().getPosition().distanceTo(shakeCenter);
        if (distance > 12.0) {
            return;
        }
        float t = (float) (age / 8.0);
        event.setRoll(event.getRoll() + (float) (1.2 * shakeStrength * (1.0 - distance / 12.0) * Math.sin(2.4 * age) * (1.0 - t)));
    }

    public static void clear() {
        FX.clear();
        THROWN.clear();
        RINGS.clear();
        CRACKS.clear();
        shakeStart = Double.NEGATIVE_INFINITY;
    }

    private FlightEffects() {
    }
}
