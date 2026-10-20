package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * V: the Super Jump (3.8, PLAN 4.4), the leap of Brave New World. Held, he crouches and charges ({@code
 * Live.RED_HULK_JUMP_CHARGING}, up to {@code jumpChargeTicks}); released, he is launched almost straight up, {@link
 * #height} blocks ({@code jumpMinHeight} with no charge to {@code jumpMaxHeight} with a full one), drifting a little
 * along his look. The next fall damage is cancelled ({@link RedHulkState#FALL_SAFE}: one landing); where he lands the
 * ground shakes ({@code jumpLandDamage} to the creatures within {@code jumpLandRadius}) and the cooldown begins.
 *
 * <p>The launch is a motion the server gives him; the client flies it with the game's own physics, so the speed is
 * found from the height through those physics ({@link #launchSpeed}: gravity 0.08 a tick, the air's drag 0.98).
 */
public final class SuperJump {
    /** The game's gravity on a player and the air's drag, per tick. */
    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.98;
    /** The forward drift at a full charge (blocks a tick). */
    static final double DRIFT = 0.18;
    /** He is in the air at most this long before the jump is called over (a stuck player, water, a ledge). */
    static final int MAX_AIR = 400;
    /** The fall guard outlives the landing by this much, then lapses if no fall came to use it. */
    static final int FALL_GRACE = 10;

    private final long chargeStart;
    private long launchedAt = -1L;
    private int air;
    private double height;

    private SuperJump(long chargeStart) {
        this.chargeStart = chargeStart;
    }

    /** The height of a jump charged {@code charge} (0-1). */
    public static double height(double charge) {
        double min = RedHulkAbilities.cfg(RedHulkConfig.JUMP_MIN_HEIGHT, 8.0);
        double max = RedHulkAbilities.cfg(RedHulkConfig.JUMP_MAX_HEIGHT, 30.0);
        return min + (max - min) * Mth.clamp(charge, 0.0, 1.0);
    }

    /** The charge (0-1) of a jump held {@code ticks}. */
    public static double charge(long ticks) {
        return Mth.clamp(ticks / (double) Math.max(1, RedHulkAbilities.cfg(RedHulkConfig.JUMP_CHARGE_TICKS, 30)),
                0.0, 1.0);
    }

    /** How high a launch at {@code speed} (blocks a tick, up) rises by the game's physics. */
    public static double apex(double speed) {
        double y = 0.0;
        double v = speed;
        for (int i = 0; i < 400 && v > 0.0; i++) {
            y += v;
            v = (v - GRAVITY) * DRAG;
        }
        return y;
    }

    /** The upward speed that rises exactly {@code height} blocks by the game's physics. */
    public static double launchSpeed(double height) {
        double lo = 0.0;
        double hi = 6.0;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) * 0.5;
            if (apex(mid) < height) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) * 0.5;
    }

    /** V pressed: he crouches and begins to charge (on the ground only). */
    public static boolean charge(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_SUPER_JUMP, now) || !HulkAbilities.grounded(player)) {
            return false;
        }
        RedHulkAbilities.state(player.getUUID()).jump = new SuperJump(now);
        RedHulkAbilities.sound(level, player.position(), ModSounds.RED_HULK_JUMP_CHARGE.get(), 1.0F, 1.0F);
        RedHulkAbilities.send(level, player, RedHulkFxPayload.JUMP_CHARGE, List.of(player.position()),
                RedHulkAbilities.cfg(RedHulkConfig.JUMP_CHARGE_TICKS, 30));
        return true;
    }

    /** Whether he is still charging (not yet launched). */
    boolean charging() {
        return launchedAt < 0L;
    }

    /** The charge this jump has now (0-1). */
    double chargeNow(long now) {
        return charge(now - chargeStart);
    }

    /** The height it was launched to (0 while charging). */
    public double height() {
        return height;
    }

    /** V released: the launch. */
    void release(ServerPlayer player, PowerData data) {
        if (!charging()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        double charge = chargeNow(now);
        height = height(charge);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize().scale(DRIFT * (0.4 + 0.6 * charge));
        player.setDeltaMovement(flat.x, launchSpeed(height), flat.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        launchedAt = now;
        Anger.set(player, Anger.get(player).with(RedHulkState.FALL_SAFE, true));
        RedHulkAbilities.state(player.getUUID()).fallSafeUntil = Long.MIN_VALUE;
        RedHulkAbilities.sound(level, player.position(), ModSounds.RED_HULK_JUMP.get(), 1.6F,
                (float) (1.1 - 0.2 * charge));
        RedHulkAbilities.send(level, player, RedHulkFxPayload.JUMP, List.of(player.position()), (int) Math.round(
                height));
    }

    /** One tick; whether the jump is over. */
    boolean tick(ServerPlayer player, PowerData data, RedHulkAbilities.State st, long now) {
        if (charging()) {
            // (Pushed off the ground while he crouches: no jump.)
            return !HulkAbilities.grounded(player) && now - chargeStart > 2;
        }
        air++;
        if (air > 3 && (player.onGround() || player.isInWater()) || air >= MAX_AIR) {
            land(player, data, st, now);
            return true;
        }
        return false;
    }

    /** He lands: the ground shakes round him, the cooldown begins, the fall guard lasts a little longer. */
    private void land(ServerPlayer player, PowerData data, RedHulkAbilities.State st, long now) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        double radius = RedHulkAbilities.cfg(RedHulkConfig.JUMP_LAND_RADIUS, 3.0);
        float damage = (float) RedHulkAbilities.cfg(RedHulkConfig.JUMP_LAND_DAMAGE, 4.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1.0, 3.0,
                radius + 1.0), e -> e != player && RedHulkAbilities.mayHurt(player, e))) {
            if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) - e.getBbWidth() * 0.5 > radius
                    || Math.abs(e.getY() - at.y) > 3.0) {
                continue;
            }
            if (RedHulkAbilities.hurt(player, e, ModDamageTypes.RED_HULK_SMASH, damage, player)) {
                HulkAbilities.push(e, at, 0.6, 0.4, player);
            }
        }
        st.fallSafeUntil = now + FALL_GRACE;
        data.startCooldown(HeroAbility.RED_HULK_SUPER_JUMP, now, RedHulkAbilities.cfg(RedHulkConfig.JUMP_COOLDOWN,
                60));
        RedHulkAbilities.sound(level, at, ModSounds.RED_HULK_LAND.get(), 1.8F, 0.9F);
        RedHulkAbilities.send(level, player, RedHulkFxPayload.LAND, List.of(at), (int) Math.ceil(radius));
    }
}
