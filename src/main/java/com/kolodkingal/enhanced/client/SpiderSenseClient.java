package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.network.SpiderSensePayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * What the local Spider-Man remembers of his spider-sense (DESIGN section 14): the dangers the server named, which are
 * outlined through walls in white-blue for a moment ({@link EntityGlow}), the direction of the worst of them for the
 * arrow on his screen ({@link SpiderSenseOverlay}), and when he last felt it (the HUD's DANGER). With the Black Eyes on
 * every hostile creature within the doubled range is outlined, for the whole surge, red-tinted if it is aggressive: the
 * sensory overload.
 */
public final class SpiderSenseClient {
    /** The outline of a danger lasts this many ticks (a second and a half). */
    public static final int OUTLINE_TICKS = 30;
    /** The arrow on the edge of the screen shows this long. */
    public static final int ARROW_TICKS = 20;
    /** The HUD says DANGER for this long after a warning. */
    public static final int DANGER_TICKS = 20;

    /** The outline of a threat; of a hostile creature seen by the Black Eyes, and of one that is aggressive. */
    public static final int THREAT_COLOR = 0xDDEEFF;
    public static final int AGGRESSIVE_COLOR = 0xFF8A8A;

    private static final Set<Integer> THREATS = new HashSet<>();
    private static long outlineUntil = Long.MIN_VALUE;
    private static long warnedAt = Long.MIN_VALUE / 2;
    private static boolean strong;
    /** The worst danger's place, to point an arrow at. */
    private static Vec3 strongest;
    /**
     * Whether the local hero's sense is on, and the Black Eyes, and how far the overload sees (squared), kept each
     * tick.
     */
    private static boolean active;
    private static boolean overload;
    private static double overloadRangeSquared;

    /** The server names the dangers: outline them, remember where the worst is, and start the ripples. */
    public static void add(SpiderSensePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        THREATS.clear();
        THREATS.addAll(payload.ids());
        outlineUntil = now + OUTLINE_TICKS;
        warnedAt = now;
        strong = payload.strong();
        strongest = null;
        double nearest = Double.MAX_VALUE;
        for (int id : payload.ids()) {
            Entity entity = mc.level.getEntity(id);
            if (entity != null && entity.distanceToSqr(player) < nearest) {
                nearest = entity.distanceToSqr(player);
                strongest = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            }
        }
    }

    /** Once per client tick: whether the sense is on and the Black Eyes widen it. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        active = false;
        overload = false;
        if (player == null || mc.level == null) {
            return;
        }
        PowerData data = Powers.get(player);
        active = data.power() == Power.SPIDER_MAN && data.toggle(Toggle.SPIDER_SENSE);
        overload = active && data.is(Live.BLACK_EYES);
        if (overload) {
            double range = EnhancedConfig.SPEC.isLoaded() ? SpiderConfig.SPIDER_SENSE_RANGE.get() * SpiderConfig.BLACK_EYES_SENSE_FACTOR.get() : 24.0;
            overloadRangeSquared = range * range;
        }
        long now = mc.level.getGameTime();
        if (now > outlineUntil && !THREATS.isEmpty()) {
            THREATS.clear();
        }
        if (now - warnedAt > ARROW_TICKS) {
            strongest = null;
        }
    }

    /** Whether the local hero sees this creature outlined through the walls. */
    public static boolean reveals(Entity entity) {
        if (!active) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || entity == player) {
            return false;
        }
        if (!THREATS.isEmpty() && THREATS.contains(entity.getId()) && mc.level != null && mc.level.getGameTime() <= outlineUntil) {
            return true;
        }
        return overload && entity instanceof Enemy && entity.isAlive() && entity.distanceToSqr(player) <= overloadRangeSquared;
    }

    /** The colour the outline of a creature is drawn in. */
    public static int color(Entity entity) {
        return overload && entity instanceof Mob mob && mob.isAggressive() ? AGGRESSIVE_COLOR : THREAT_COLOR;
    }

    /** Whether the sense warned him within the last second. */
    public static boolean danger(long now) {
        return now - warnedAt <= DANGER_TICKS && now >= warnedAt;
    }

    /** The age in ticks of the last warning (large when there was none lately). */
    public static float age(float now) {
        return now - warnedAt;
    }

    /** Whether the last warning was a strong one (a hit less than 12 ticks away). */
    public static boolean strong() {
        return strong;
    }

    /** Where the worst of the dangers is, or null once the arrow's time is over. */
    public static Vec3 strongest() {
        return strongest;
    }

    /** Whether the Black Eyes' darkening of the screen should show. */
    public static boolean overload() {
        return overload;
    }

    public static void clear() {
        THREATS.clear();
        outlineUntil = Long.MIN_VALUE;
        warnedAt = Long.MIN_VALUE / 2;
        strongest = null;
        active = false;
        overload = false;
    }

    private SpiderSenseClient() {
    }
}
