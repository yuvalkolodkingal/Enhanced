package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.client.render.PhoenixMath;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client side of Mind Sense (Sneak + R), the Phoenix's ping like a small Cerebro. A ring of violet light spreads from
 * her at two blocks a tick and every mind it passes glows through the walls on her screen, and keeps glowing until the
 * ping ends: players white-violet, hostile creatures pink, everything else violet. Everyone sees the ring of motes;
 * only she gets the glows (a source of {@link EntityGlow}, so the two mixins are never touched).
 *
 * <p>The server only says that she is sensing ({@link Live#MIND_SENSING}, for as long as the ping lasts); the client
 * remembers where she stood and when the bit rose, and works out the rest.
 */
public final class MindSense {
    /** The glow of a player's mind. */
    public static final int PLAYER_GLOW = 0xE9E0FF;
    /** The glow of a mind that means harm (a hostile creature). */
    public static final int HOSTILE_GLOW = 0xFF5C8A;
    /** The glow of every other mind. */
    public static final int MIND_GLOW = 0xA98BFF;

    /** Where a Phoenix stood when her ping began (the middle of her body) and the game time. */
    private record Ping(Vec3 origin, long start) {
    }

    private static final Map<Integer, Ping> PINGS = new HashMap<>();
    /** Who was sensing on the last tick (to find the bit rising). */
    private static final Set<Integer> WAS_SENSING = new HashSet<>();

    /** Whether the local Phoenix sees this creature glow right now: a mind the ring has passed. */
    public static boolean reveals(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || entity == me || PINGS.isEmpty()) {
            return false;
        }
        Ping ping = PINGS.get(me.getId());
        if (ping == null) {
            return false;
        }
        PowerData data = Powers.get(me);
        if (data.power() != Power.PHOENIX || !data.is(Live.MIND_SENSING)) {
            return false;
        }
        if (!PhoenixAbilities.hasMind(entity) || PhoenixAbilities.isShielded(entity)) {
            return false;
        }
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        return PhoenixMath.senseReaches(at.distanceTo(ping.origin()), PhoenixConfig.senseRange(), mc.level.getGameTime() - ping.start());
    }

    /** The colour a sensed mind glows in. */
    public static int color(Entity entity) {
        return entity instanceof Player ? PLAYER_GLOW : entity instanceof Enemy ? HOSTILE_GLOW : MIND_GLOW;
    }

    /** Once a tick: finds the pings that begin and sparks the ring of those that spread. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            stop();
            return;
        }
        long now = level.getGameTime();
        float thin = EnhancedClientConfig.flightEffects() ? 1.0F : 0.1F;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Set<Integer> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (data.power() != Power.PHOENIX) {
                continue;
            }
            int id = player.getId();
            seen.add(id);
            boolean sensing = data.is(Live.MIND_SENSING);
            if (sensing && !WAS_SENSING.contains(id)) {
                PINGS.put(id, new Ping(player.position().add(0, player.getBbHeight() * 0.5, 0), now));
            } else if (!sensing) {
                PINGS.remove(id);
            }
            if (sensing) {
                WAS_SENSING.add(id);
            } else {
                WAS_SENSING.remove(id);
            }
            Ping ping = PINGS.get(id);
            if (ping != null && !player.isInvisible() && ping.origin().distanceTo(camera) < 140.0) {
                ring(level, ping, now - ping.start(), player.getYRot(), thin);
            }
        }
        PINGS.keySet().retainAll(seen);
        WAS_SENSING.retainAll(seen);
    }

    /**
     * The ring of motes at the radius the ping has reached (two blocks a tick): twenty on the horizontal ring and a few
     * on a faint vertical ring in the plane she faces. It stops when the ring has spread over the whole range.
     */
    private static void ring(ClientLevel level, Ping ping, long age, float yaw, float thin) {
        float range = PhoenixConfig.senseRange();
        if (age < 0 || age > PhoenixMath.senseRingTicks(range)) {
            return;
        }
        float radius = PhoenixMath.senseRadius(range, age);
        RandomSource random = level.random;
        int count = Math.round(28 * thin);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            level.addParticle(new EnergyParticleOptions(PhoenixAbilities.PSY_MAIN, 1.6F), ping.origin().x + Math.cos(angle) * radius,
                    ping.origin().y - 0.4, ping.origin().z + Math.sin(angle) * radius, 0.0, 0.004, 0.0);
        }
        double facing = Math.toRadians(yaw);
        double dx = -Math.sin(facing);
        double dz = Math.cos(facing);
        int vertical = Math.round(6 * thin);
        for (int i = 0; i < vertical; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double along = Math.cos(angle) * radius;
            level.addParticle(new EnergyParticleOptions(PhoenixAbilities.PSY_CORE, 1.3F), ping.origin().x + dx * along,
                    ping.origin().y + Math.sin(angle) * radius, ping.origin().z + dz * along, 0.0, 0.0, 0.0);
        }
    }

    /** Forget every ping (the client left the world). */
    public static void stop() {
        PINGS.clear();
        WAS_SENSING.clear();
    }

    private MindSense() {
    }
}
