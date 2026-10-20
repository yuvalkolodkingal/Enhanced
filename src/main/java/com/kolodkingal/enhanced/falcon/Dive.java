package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * V: the Dive, and Sneak + V: the Boost Climb (3.8, PLAN 8.4). The Dive goes only from the air, at least {@code
 * diveMinHeight} 4 above the ground ("Dive from the air!" otherwise): he folds his wings and dives at the creature he
 * looks at within {@code diveRange} 32 (it is followed as it moves), or at the spot he looks at, {@code diveSpeed} 1.8
 * blocks a tick ({@code Live.FALCON_DIVING}); on contact {@code diveDamage} 10 to it and {@code diveSplash} 3 to the
 * creatures within {@link #SPLASH_RADIUS} of it ({@code falcon_dive}), knockback {@link #KNOCKBACK}, no harm to him (he
 * bounces up and hovers on). The Boost Climb: the nozzles roar and he shoots straight up {@code climbHeight} 24 blocks
 * in {@link #CLIMB_TICKS} ticks (from the ground too; a ceiling stops him below it), wings swept back, and hovers at
 * the top. No fall damage from either.
 *
 * <p>His flight follows a path: every tick his motion is set to carry him on (the server's word on a player's motion,
 * as Wolverine's Pounce), so it goes where it was aimed whatever the client, the load or a test does.
 */
public final class Dive {
    /** The Boost Climb's flight (ticks). */
    public static final int CLIMB_TICKS = 20;
    /** The splash round the creature the dive hits, and its push. */
    public static final double SPLASH_RADIUS = 2.0;
    public static final double KNOCKBACK = 1.4;
    /** How close (blocks between the boxes) he must come to strike. */
    static final double CONTACT = 0.5;
    /** The ticks of grace past the dive's longest flight before it gives up. */
    static final int GRACE = 10;
    /** No fall damage for this long after a dive or a climb. */
    static final int FALL_GUARD = 60;

    /** A dive or a climb under way. */
    static final class Flight {
        final boolean climb;
        @Nullable
        final LivingEntity prey;
        Vec3 aim;
        final Vec3 start;
        final long startAt;
        final int ticks;

        Flight(boolean climb, @Nullable LivingEntity prey, Vec3 aim, Vec3 start, long startAt, int ticks) {
            this.climb = climb;
            this.prey = prey;
            this.aim = aim;
            this.start = start;
            this.startAt = startAt;
            this.ticks = ticks;
        }
    }

    /** V: the Dive at what he looks at (only from high enough in the air). */
    public static boolean dive(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_DIVE;
        long now = player.level().getGameTime();
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        if (!data.has(row) || !data.isReady(row, now) || state.flight != null) {
            return false;
        }
        double minHeight = FalconAbilities.cfg(FalconConfig.DIVE_MIN_HEIGHT, 4.0);
        if (player.onGround() || player.isInWater() || heightAboveGround(player, minHeight + 1.0) < minHeight) {
            FalconAbilities.say(player, "message.enhanced.falcon.dive_from_air");
            return false;
        }
        int range = FalconAbilities.cfg(FalconConfig.DIVE_RANGE, 32);
        LivingEntity prey = prey(player, range);
        Vec3 aim = prey != null ? prey.getBoundingBox().getCenter() : spot(player, range);
        double speed = FalconAbilities.cfg(FalconConfig.DIVE_SPEED, 1.8);
        int ticks = (int) Math.ceil(range / Math.max(0.2, speed)) + GRACE;
        data.startCooldown(row, now, FalconAbilities.cfg(FalconConfig.DIVE_COOLDOWN, 100));
        begin(player, state, new Flight(false, prey, aim, player.position(), now, ticks));
        data.set(Live.FALCON_DIVING, true);
        ServerLevel level = player.serverLevel();
        FalconAbilities.sound(level, player.position(), ModSounds.FALCON_DIVE.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.DIVE_START, List.of(player.position(), aim),
                prey != null ? prey.getId() : -1);
        return true;
    }

    /** Sneak + V: the Boost Climb, straight up. */
    public static boolean climb(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_BOOST_CLIMB;
        long now = player.level().getGameTime();
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        if (!FalconAbilities.hasVariant(data, row) || state.flight != null) {
            return false;
        }
        HeroAbility slot = FalconAbilities.slotRow(data, row);
        if (!data.isReady(slot, now)) {
            return false;
        }
        double height = FalconAbilities.cfg(FalconConfig.CLIMB_HEIGHT, 24.0);
        // (A ceiling stops him below it: his head stays out of the block.)
        Vec3 feet = player.position();
        BlockHitResult roof = player.level().clip(new ClipContext(feet.add(0, player.getBbHeight(), 0),
                feet.add(0, player.getBbHeight() + height, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        if (roof.getType() != HitResult.Type.MISS) {
            height = Math.max(0.0, roof.getLocation().y - player.getBbHeight() - feet.y - 0.05);
        }
        data.startCooldown(slot, now, FalconAbilities.cfg(FalconConfig.CLIMB_COOLDOWN, 60));
        Vec3 aim = feet.add(0, height, 0);
        begin(player, state, new Flight(true, null, aim, feet, now, CLIMB_TICKS));
        ServerLevel level = player.serverLevel();
        FalconAbilities.sound(level, feet, ModSounds.FALCON_CLIMB.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.CLIMB, List.of(feet, aim), 0);
        return true;
    }

    private static void begin(ServerPlayer player, FalconAbilities.State state, Flight flight) {
        state.flight = flight;
        state.fallGuardUntil = flight.startAt + flight.ticks + FALL_GUARD;
        FalconWings.endGlide(player, false);
        Abilities abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
            player.onUpdateAbilities();
        }
    }

    /** How far (blocks) the ground is below his feet, looking no further than {@code max}. */
    static double heightAboveGround(Player player, double max) {
        Vec3 feet = player.position();
        BlockHitResult hit = player.level().clip(new ClipContext(feet, feet.add(0, -max, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? max : feet.y - hit.getLocation().y;
    }

    /** The creature the Dive would take: the one he looks at within {@code range}, in sight, that he may hurt. */
    @Nullable
    static LivingEntity prey(ServerPlayer player, double range) {
        EntityHitResult hit = Raycasts.entity(player, range, 0.6, e -> e instanceof LivingEntity living
                && FalconAbilities.mayHurt(player, living), 1.0F);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity prey) || !player.hasLineOfSight(prey)) {
            return null;
        }
        return prey;
    }

    /** The spot he looks at within {@code range} (the block's face), or that far along his look. */
    static Vec3 spot(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    static void tick(ServerPlayer player, PowerData data, FalconAbilities.State state, long now) {
        Flight flight = state.flight;
        if (flight == null) {
            return;
        }
        int t = (int) (now - flight.startAt);
        player.fallDistance = 0.0F;
        if (flight.climb) {
            climbTick(player, data, state, flight, t);
            return;
        }
        LivingEntity prey = flight.prey;
        if (prey != null && prey.isAlive() && prey.level() == player.level()) {
            flight.aim = prey.getBoundingBox().getCenter();
        }
        if (touching(player, prey)) {
            strike(player, data, state, flight);
            return;
        }
        Vec3 middle = player.getBoundingBox().getCenter();
        Vec3 to = flight.aim.subtract(middle);
        double distance = to.length();
        boolean stopped = t > 1 && (player.onGround() || player.horizontalCollision || player.isInWater());
        if (t >= flight.ticks || stopped || distance < 0.6) {
            if (prey != null && prey.isAlive()
                    && player.getBoundingBox().inflate(CONTACT + 0.8).intersects(prey.getBoundingBox())) {
                strike(player, data, state, flight);
            } else {
                land(player, data, state, false, null);
            }
            return;
        }
        double speed = FalconAbilities.cfg(FalconConfig.DIVE_SPEED, 1.8);
        player.setDeltaMovement(to.scale(Math.min(speed, distance) / distance));
        player.hurtMarked = true;
    }

    private static void climbTick(ServerPlayer player, PowerData data, FalconAbilities.State state, Flight flight,
                                  int t) {
        // (A ceiling met on the way, a head bumped: he stops there.)
        if (t >= flight.ticks || t > 1 && player.verticalCollision && !player.onGround()) {
            state.flight = null;
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            hoverOn(player, data);
            return;
        }
        // Fast off the ground, easing into the top.
        double s = Mth.clamp((t + 1) / (double) flight.ticks, 0.0, 1.0);
        double eased = 1.0 - (1.0 - s) * (1.0 - s);
        double y = Mth.lerp(eased, flight.start.y, flight.aim.y);
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x * 0.3, y - player.getY(), m.z * 0.3);
        player.hurtMarked = true;
    }

    /** Whether he has reached the prey (their boxes within {@link #CONTACT}). */
    static boolean touching(ServerPlayer player, @Nullable LivingEntity prey) {
        return prey != null && prey.isAlive() && prey.level() == player.level()
                && player.getBoundingBox().inflate(CONTACT).intersects(prey.getBoundingBox());
    }

    /** The hit: the damage, the splash and the push; then he bounces up off it. */
    private static void strike(ServerPlayer player, PowerData data, FalconAbilities.State state, Flight flight) {
        LivingEntity prey = flight.prey;
        ServerLevel level = player.serverLevel();
        if (prey != null) {
            FalconAbilities.hurt(player, prey, ModDamageTypes.FALCON_DIVE,
                    FalconAbilities.cfg(FalconConfig.DIVE_DAMAGE, 10.0), player);
            FalconAbilities.push(player, prey, flight.start, KNOCKBACK);
            double splash = FalconAbilities.cfg(FalconConfig.DIVE_SPLASH, 3.0);
            AABB round = prey.getBoundingBox().inflate(SPLASH_RADIUS);
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, round, e -> e != prey
                    && e != player && FalconAbilities.mayHurt(player, e))) {
                if (near.getBoundingBox().getCenter().distanceTo(prey.getBoundingBox().getCenter())
                        <= SPLASH_RADIUS + near.getBbWidth() * 0.5) {
                    FalconAbilities.hurt(player, near, ModDamageTypes.FALCON_DIVE, splash, player);
                    FalconAbilities.push(player, near, prey.position(), KNOCKBACK * 0.5);
                }
            }
        }
        land(player, data, state, true, prey);
    }

    /** The end of the dive: the boom of the hit (or nothing), a bounce up, and he hovers on if he may fly. */
    private static void land(ServerPlayer player, PowerData data, FalconAbilities.State state, boolean struck,
                             @Nullable LivingEntity prey) {
        state.flight = null;
        state.fallGuardUntil = player.level().getGameTime() + FALL_GUARD;
        data.set(Live.FALCON_DIVING, false);
        ServerLevel level = player.serverLevel();
        Vec3 at = prey != null ? prey.getBoundingBox().getCenter() : player.position();
        player.setDeltaMovement(0, struck ? 0.55 : 0.0, 0);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        if (struck) {
            FalconAbilities.sound(level, at, ModSounds.FALCON_DIVE_HIT.get(), 1.0F, 1.0F);
        }
        FalconAbilities.send(level, player, FalconFxPayload.DIVE_HIT, List.of(at), struck ? 1 : 0);
        if (!player.onGround()) {
            hoverOn(player, data);
        }
    }

    /** Falcon goes on flying where he is (the wings spread again). */
    private static void hoverOn(ServerPlayer player, PowerData data) {
        if (data.power() == Power.FALCON && player.mayFly() && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    /** His dive or climb ends at once (death, a dimension, the power lost). */
    static void end(ServerPlayer player, PowerData data) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        if (state != null && state.flight != null) {
            state.flight = null;
            player.fallDistance = 0.0F;
        }
        data.set(Live.FALCON_DIVING, false);
    }

    /** Whether the player is in a dive or a climb of his now (server side). */
    public static boolean flying(Player player) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        return state != null && state.flight != null;
    }

    /** Whether his own dive or climb spares him the fall's damage now. */
    static boolean fallGuarded(Player player) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        return state != null && (state.flight != null || player.level().getGameTime() <= state.fallGuardUntil);
    }

    private Dive() {
    }
}
