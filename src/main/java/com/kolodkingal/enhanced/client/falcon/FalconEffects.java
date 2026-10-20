package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.kolodkingal.enhanced.client.render.FalconActing;
import com.kolodkingal.enhanced.falcon.FalconAbilities;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Falcon's effects on the client (3.8): the drawers of every {@link FalconFxPayload} kind (registered from the client
 * setup; each one also hands the payload to {@link FalconActing} for the gesture it stands for), the creatures Redwing
 * marked for the local player (they glow red through walls for him alone, {@link EntityGlow}), Redwing's last zaps
 * (drawn as beams by {@link RedwingRenderer}), and the nozzles' flames of everyone flying with the wings.
 */
public final class FalconEffects {
    /** The creatures Redwing marked for the local player: id → client game time the mark ends. */
    private static final Map<Integer, Long> MARKS = new HashMap<>();
    /** When each player's Wing Shield went up (client game time). */
    private static final Map<Integer, Long> SHIELD_UP = new HashMap<>();
    /** Each Redwing's last zap: its id → the zap. */
    private static final Map<Integer, Zap> ZAPS = new HashMap<>();
    /** How long (ticks) a zap's beam shows. */
    public static final int ZAP_TICKS = 5;

    /** A zap of Redwing: from it to its target, at that time. */
    public record Zap(Vec3 from, Vec3 to, long at) {
    }

    /** Registers the drawers and the glow of the marks (once, from the client setup). */
    public static void setup() {
        for (int kind = FalconFxPayload.BLAST; kind <= FalconFxPayload.MARK; kind++) {
            FalconFx.register(kind, FalconEffects::receive);
        }
        EntityGlow.register(FalconEffects::marked, e -> FalconAbilities.RED);
    }

    static void receive(FalconFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        FalconActing.onPayload(payload);
        List<Vec3> points = payload.points();
        Vec3 at = points.isEmpty() ? null : points.get(0);
        RandomSource random = level.random;
        long now = level.getGameTime();
        switch (payload.kind()) {
            case FalconFxPayload.BLAST -> {
                if (at != null) {
                    burst(level, at, FalconAbilities.RED, 4, 0.05, 0.8F);
                }
            }
            case FalconFxPayload.ROCKET -> {
                if (at != null) {
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, random.nextGaussian() * 0.03,
                                0.02, random.nextGaussian() * 0.03);
                    }
                    level.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, 0, 0);
                }
            }
            case FalconFxPayload.DIVE_START -> {
                if (at != null) {
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.CLOUD, at.x + random.nextGaussian() * 0.4, at.y + 1.0
                                + random.nextGaussian() * 0.4, at.z + random.nextGaussian() * 0.4, 0, 0, 0);
                    }
                }
            }
            case FalconFxPayload.DIVE_HIT -> {
                if (at != null && payload.arg() == 1) {
                    level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0, 0, 0);
                    for (int i = 0; i < 12; i++) {
                        level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, random.nextGaussian() * 0.4,
                                random.nextGaussian() * 0.4, random.nextGaussian() * 0.4);
                    }
                    burst(level, at, FalconAbilities.RED, 10, 0.25, 1.0F);
                }
            }
            case FalconFxPayload.CLIMB -> {
                if (at != null) {
                    int n = payload.arg() == 0 ? 16 : 6;
                    for (int i = 0; i < n; i++) {
                        level.addParticle(ParticleTypes.FLAME, at.x + random.nextGaussian() * 0.25, at.y + 0.6,
                                at.z + random.nextGaussian() * 0.25, random.nextGaussian() * 0.04, -0.15,
                                random.nextGaussian() * 0.04);
                        level.addParticle(ParticleTypes.CLOUD, at.x + random.nextGaussian() * 0.3, at.y + 0.3,
                                at.z + random.nextGaussian() * 0.3, 0, -0.05, 0);
                    }
                }
            }
            case FalconFxPayload.SHIELD_ON -> SHIELD_UP.put(payload.entityId(), now);
            case FalconFxPayload.SHIELD_OFF -> SHIELD_UP.remove(payload.entityId());
            case FalconFxPayload.DEFLECT -> {
                if (at != null) {
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                                random.nextGaussian() * 0.25, random.nextGaussian() * 0.25,
                                random.nextGaussian() * 0.25);
                    }
                }
            }
            case FalconFxPayload.SLASH -> {
                if (at != null) {
                    double r = Math.max(1.0, payload.arg() / 10.0);
                    for (int i = 0; i < 16; i++) {
                        double a = Mth.TWO_PI * i / 16.0;
                        level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x + Math.cos(a) * r * 0.7, at.y,
                                at.z + Math.sin(a) * r * 0.7, 0, 0, 0);
                    }
                }
            }
            case FalconFxPayload.REDWING_OUT -> {
                if (at != null) {
                    burst(level, at, FalconAbilities.RED, 8, 0.12, 0.7F);
                }
            }
            case FalconFxPayload.REDWING_IN -> {
                if (at == null) {
                    return;
                }
                if (payload.arg() == 1) {
                    for (int i = 0; i < 14; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                                random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2);
                    }
                } else if (points.size() > 1) {
                    line(level, at, points.get(1), FalconAbilities.RED, 0.4);
                }
            }
            case FalconFxPayload.REDWING_ZAP -> {
                if (points.size() > 1) {
                    ZAPS.put(payload.entityId(), new Zap(at, points.get(1), now));
                    burst(level, points.get(1), FalconAbilities.RED, 6, 0.1, 0.8F);
                }
            }
            case FalconFxPayload.REDWING_STRIKE -> {
                if (at != null && payload.arg() < 0) {
                    level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0, 0, 0);
                    burst(level, at, FalconAbilities.RED, 12, 0.25, 1.0F);
                }
            }
            case FalconFxPayload.MARK -> MARKS.put(payload.entityId(), now + Math.max(1, payload.arg()));
            default -> {
            }
        }
    }

    // ---- what the others ask ----

    /** Whether Redwing has marked this creature for the local player (it glows for him). */
    static boolean marked(Entity entity) {
        Long until = MARKS.get(entity.getId());
        return until != null && entity.level().getGameTime() <= until && entity.isAlive();
    }

    /** When the player's Wing Shield went up (client game time), or -1. */
    public static long shieldUpAt(Player player) {
        Long at = SHIELD_UP.get(player.getId());
        return at == null ? -1L : at;
    }

    /** Redwing's zap to draw now, or null. */
    @Nullable
    public static Zap zap(int droneId, long now) {
        Zap zap = ZAPS.get(droneId);
        return zap != null && now - zap.at() <= ZAP_TICKS ? zap : null;
    }

    /** The player's Redwing as his client sees it (near), or null. */
    @Nullable
    public static RedwingEntity drone(Player player) {
        for (RedwingEntity drone : player.level().getEntitiesOfClass(RedwingEntity.class,
                player.getBoundingBox().inflate(48.0))) {
            if (player.getUUID().equals(drone.ownerId())) {
                return drone;
            }
        }
        return null;
    }

    // ---- every tick ----

    /** Every client tick: the nozzles' flames of everyone flying with the wings; what is long over is forgotten. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        FalconWingsState.tickAll(level);
        FalconLoops.tick(mc);
        MARKS.values().removeIf(until -> until < now || until - now > 2400);
        ZAPS.values().removeIf(zap -> now - zap.at() > 40 || now < zap.at());
        SHIELD_UP.values().removeIf(t -> now - t > 2400 || now < t);
        boolean reduced = mc.options.particles().get() != net.minecraft.client.ParticleStatus.ALL;
        for (Player player : level.players()) {
            if (!FalconWingsState.drawsWings(player) || player.isInvisible()) {
                continue;
            }
            PowerData data = Powers.get(player);
            boolean thrusting = data.flying() || player.isFallFlying() || data.is(Live.FALCON_DIVING)
                    || FalconActing.climb(player, 0.0F) > 0.0F;
            if (!thrusting || player.onGround() || reduced && (now & 1) == 0) {
                continue;
            }
            for (boolean right : new boolean[]{true, false}) {
                Vec3 nozzle = FalconWingsState.nozzle(player, right);
                level.addParticle(ParticleTypes.FLAME, nozzle.x, nozzle.y, nozzle.z,
                        level.random.nextGaussian() * 0.01, -0.06, level.random.nextGaussian() * 0.01);
                if ((now + (right ? 1 : 0)) % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, nozzle.x, nozzle.y - 0.1, nozzle.z, 0, -0.02, 0);
                }
            }
        }
    }

    public static void clear() {
        FalconWingsState.clear();
        FalconLoops.stopAll();
        MARKS.clear();
        SHIELD_UP.clear();
        ZAPS.clear();
    }

    // ---- particles ----

    private static void burst(ClientLevel level, Vec3 at, int color, int n, double spread, float scale) {
        RandomSource random = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(new EnergyParticleOptions(color, scale), at.x, at.y, at.z,
                    random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread);
        }
    }

    private static void line(ClientLevel level, Vec3 from, Vec3 to, int color, double step) {
        Vec3 d = to.subtract(from);
        int n = Math.min(64, Math.max(1, (int) (d.length() / step)));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.addParticle(new EnergyParticleOptions(color, 0.5F), p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private FalconEffects() {
    }
}
