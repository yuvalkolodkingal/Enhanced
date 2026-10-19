package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * V: the Super Leap, and Sneak + V: the Bound (3.6). The Super Leap: he crouches and launches about 25 blocks up toward
 * the point he looks at (up to {@code leapRange}; at the sky, 32 ahead), and past the top he falls fast, steered at the
 * point; where he lands everything within {@code leapRadius} takes {@code leapDamage} and is thrown, a ring of dust
 * runs out, and he takes no fall damage. The Bound is the travelling leap of the films: long and low ({@code
 * boundDistance} forward, 6 up), no blow where it lands, again every {@code boundCooldown} ticks.
 *
 * <p>His flight follows a path: every tick his motion is set to carry him to the path's next point (the server's word
 * on a player's motion), so the leap lands where it was aimed whatever the client, the load or a test does; a wall or a
 * ceiling stops him and he falls from there.
 */
public final class SuperLeap implements HulkAbilities.Move {
    /** The crouch before the launch (ticks): the Super Leap's and the Bound's. */
    static final int CROUCH = 5;
    static final int BOUND_CROUCH = 2;
    /** The height of the Super Leap's arc above the higher end, and the Bound's. */
    static final double LEAP_HEIGHT = 25.0;
    static final double BOUND_HEIGHT = 6.0;
    /** Looking at the sky: this far ahead. */
    static final double SKY_AHEAD = 32.0;
    /** The share of the flight's time spent rising (the fall is faster). */
    static final double RISE_SHARE = 0.62;
    /** The landing's throw. */
    static final double LAND_PUSH = 1.0;
    static final double LAND_LIFT = 0.4;
    /** He is given this many ticks past the path to touch the ground before he is landed wherever he is. */
    static final int GRACE = 40;

    private final boolean bound;
    private final Vec3 target;
    private Vec3 start = Vec3.ZERO;
    private int flight;
    private boolean landed;

    private SuperLeap(boolean bound, Vec3 target) {
        this.bound = bound;
        this.target = target;
    }

    /** Where a Super Leap aimed along the player's look lands. */
    public static Vec3 aim(ServerPlayer player) {
        Level level = player.level();
        int range = HulkAbilities.cfg(HulkConfig.LEAP_RANGE, 48);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 point;
        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos pos = hit.getBlockPos();
            BlockPos above = hit.getDirection() == Direction.DOWN ? pos.below() : pos.relative(hit.getDirection());
            point = ground(level, above, 12);
        } else {
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
            Vec3 ahead = player.position().add(flat.scale(SKY_AHEAD));
            point = ground(level, BlockPos.containing(ahead.x, player.getY() + 4, ahead.z), 48);
        }
        return point;
    }

    /**
     * The top of the first solid ground at or below {@code from} (within {@code depth} blocks), or {@code from} itself.
     */
    static Vec3 ground(Level level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 0; i < depth && pos.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            }
            pos.move(Direction.DOWN);
        }
        return Vec3.atBottomCenterOf(from);
    }

    /** V: the Super Leap toward where he looks. */
    public static boolean leap(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_LEAP, now)) {
            return false;
        }
        Vec3 target = aim(player);
        data.startCooldown(HeroAbility.HULK_LEAP, now, HulkAbilities.cfg(HulkConfig.LEAP_COOLDOWN, 120));
        begin(player, new SuperLeap(false, target));
        HulkAbilities.send(player.serverLevel(), player, HulkFxPayload.LEAP, List.of(player.position(), target), 0);
        return true;
    }

    /** V toward a given point (the tests' aim). */
    public static SuperLeap leapTo(ServerPlayer player, Vec3 target) {
        SuperLeap leap = new SuperLeap(false, target);
        begin(player, leap);
        return leap;
    }

    /** Sneak + V: the Bound, long and low along his look. */
    public static boolean bound(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_BOUND, now)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 ahead = player.position().add(flat.scale(HulkAbilities.cfg(HulkConfig.BOUND_DISTANCE, 24)));
        Vec3 target = ground(player.level(), BlockPos.containing(ahead.x, player.getY() + 2, ahead.z), 16);
        data.startCooldown(HeroAbility.HULK_BOUND, now, HulkAbilities.cfg(HulkConfig.BOUND_COOLDOWN, 30));
        begin(player, new SuperLeap(true, target));
        HulkAbilities.send(player.serverLevel(), player, HulkFxPayload.BOUND, List.of(player.position(), target), 0);
        return true;
    }

    private static void begin(ServerPlayer player, SuperLeap leap) {
        leap.start = player.position();
        HulkAbilities.startMove(player, HulkAbilities.state(player.getUUID()), leap);
        HulkEvents.guardFall(player, 400);
        ServerLevel level = player.serverLevel();
        HulkAbilities.sound(level, player.position(), ModSounds.HULK_LEAP.get(), leap.bound ? 0.9F : 1.4F, leap.bound
                ? 1.15F : 1.0F);
    }

    @Override
    public Live live() {
        return Live.HULK_LEAPING;
    }

    /** The ticks of the flight from launch to landing. */
    int flightTicks() {
        double distance = Math.hypot(target.x - start.x, target.z - start.z);
        if (bound) {
            return Mth.clamp((int) Math.round(10 + distance * 0.6), 12, 30);
        }
        return Mth.clamp((int) Math.round(26 + distance * 0.45), 26, 52);
    }

    /** The point of the path at {@code s} (0 at the launch, 1 at the target): straight across, an arc up. */
    Vec3 path(double s) {
        double height = bound ? BOUND_HEIGHT : LEAP_HEIGHT;
        double top = Math.max(start.y, target.y) + height;
        double x = Mth.lerp(s, start.x, target.x);
        double z = Mth.lerp(s, start.z, target.z);
        double y;
        // The rise eases into the top; the fall drops fast from it (gravity x2.5 in look).
        double peak = 0.5;
        if (s <= peak) {
            double u = s / peak;
            y = Mth.lerp(1.0 - (1.0 - u) * (1.0 - u), start.y, top);
        } else {
            double u = (s - peak) / (1.0 - peak);
            y = Mth.lerp(u * u, top, target.y);
        }
        return new Vec3(x, y, z);
    }

    /** The share {@code s} of the path at flight tick {@code t}: the rise takes {@link #RISE_SHARE} of the time. */
    double share(int t, int ticks) {
        double tau = Mth.clamp(t / (double) ticks, 0.0, 1.0);
        return tau <= RISE_SHARE ? 0.5 * tau / RISE_SHARE : 0.5 + 0.5 * (tau - RISE_SHARE) / (1.0 - RISE_SHARE);
    }

    @Override
    public boolean tick(ServerPlayer player, PowerData data, int t) {
        int crouch = bound ? BOUND_CROUCH : CROUCH;
        if (t < crouch) {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.hurtMarked = true;
            return false;
        }
        int ticks = flightTicks();
        flight = t - crouch;
        if (flight == 0) {
            start = player.position();
        }
        if (flight > 2 && (player.onGround() || player.isInWater()) && flight > ticks * 0.5) {
            land(player, data);
            return true;
        }
        if (flight >= ticks + GRACE) {
            land(player, data);
            return true;
        }
        if (flight < ticks) {
            Vec3 next = path(share(flight + 1, ticks));
            Vec3 motion = next.subtract(player.position());
            if (player.horizontalCollision && flight > 2) {
                motion = new Vec3(0, Math.min(motion.y, 0), 0);
            }
            player.setDeltaMovement(motion);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            boolean falling = share(flight + 1, ticks) > 0.5;
            HulkAbilities.setModifier(player, Attributes.GRAVITY, HulkAbilities.LEAP_GRAVITY, 1.5, falling && !bound,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } else {
            // Past the path: he falls the rest of the way, straight down.
            player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, -0.5), 0);
            player.hurtMarked = true;
        }
        return false;
    }

    /** He lands: the blow (not the Bound's), the dust, the cracked ground. */
    private void land(ServerPlayer player, PowerData data) {
        if (landed) {
            return;
        }
        landed = true;
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        HulkEvents.guardFall(player, 10);
        if (bound) {
            HulkAbilities.sound(level, at, ModSounds.HULK_LAND.get(), 0.9F, 1.2F);
            HulkAbilities.send(level, player, HulkFxPayload.LAND, List.of(at), 0);
            return;
        }
        int radius = HulkAbilities.cfg(HulkConfig.LEAP_RADIUS, 4);
        float amount = HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.LEAP_DAMAGE, 6.0));
        for (LivingEntity e : around(player, at, radius)) {
            if (HulkAbilities.hurt(player, e, ModDamageTypes.HULK_LEAP, amount, player)) {
                HulkAbilities.push(e, at, LAND_PUSH, LAND_LIFT, player);
            }
        }
        HulkTerrain.land(player, at);
        HulkAbilities.sound(level, at, ModSounds.HULK_LAND.get(), 1.8F, 0.9F);
        HulkAbilities.send(level, player, HulkFxPayload.LAND, List.of(at), Math.max(1, radius));
    }

    /** The creatures his landing reaches. */
    static List<LivingEntity> around(ServerPlayer player, Vec3 at, double radius) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at)
                .inflate(radius + 1.0, 3.0, radius + 1.0),
                e -> e != player && HulkAbilities.mayHurt(player, e))) {
            if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) - e.getBbWidth() * 0.5 <= radius
                    && Math.abs(e.getY() - at.y) <= 3.0) {
                found.add(e);
            }
        }
        return found;
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        HulkAbilities.setModifier(player, Attributes.GRAVITY, HulkAbilities.LEAP_GRAVITY, 0.0, false,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (!landed) {
            player.fallDistance = 0.0F;
        }
    }

    /** Where it was aimed. */
    public Vec3 target() {
        return target;
    }

    @Nullable
    public static SuperLeap of(net.minecraft.world.entity.player.Player player) {
        return HulkAbilities.move(player) instanceof SuperLeap leap ? leap : null;
    }
}
