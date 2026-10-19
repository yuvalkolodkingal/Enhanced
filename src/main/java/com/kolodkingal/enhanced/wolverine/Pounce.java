package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * V: the Pounce, and Sneak + V: the Feral Leap (3.7), the leap Eilon asked for. The Pounce: he picks the creature he
 * looks at within {@code pounceRange} 16 (in sight, one he may hurt), crouches {@link #CROUCH} ticks, and flies onto it
 * in a {@link #FLIGHT}-tick arc that follows it; on contact he stabs it for {@code pounceDamage} 12 ({@code
 * wolverine_pounce}) and pins it {@code pouncePinTicks} 20 (Slowness IV, no jumping; a boss takes the damage only), and
 * lands on his feet beside it. With no prey in sight it is a plain leap to where he looks, without the stab. The Feral
 * Leap is the long low running leap: {@link #FERAL_DISTANCE} ahead, {@link #FERAL_HEIGHT} up. No fall damage from
 * either.
 *
 * <p>His flight follows a path: every tick his motion is set to carry him to the path's next point (the server's word on
 * a player's motion, as the Hulk's leap), so he lands where it was aimed whatever the client, the load or a test does;
 * a wall stops him and he drops from there.
 */
public final class Pounce {
    /** The crouch before the pounce (ticks), and its flight. */
    public static final int CROUCH = 4;
    public static final int FLIGHT = 12;
    /** The Feral Leap: its flight, how far, how high. */
    public static final int FERAL_FLIGHT = 14;
    public static final double FERAL_DISTANCE = 10.0;
    public static final double FERAL_HEIGHT = 2.2;
    /** How close (blocks between the boxes) he must come to strike. */
    public static final double CONTACT = 0.45;
    /** He is given this many ticks past the path to touch the ground. */
    static final int GRACE = 30;
    /** The pinned creature's slowness (IV). */
    static final int PIN_AMPLIFIER = 3;

    /** A leap under way. */
    static final class Leap {
        private final boolean pounce;
        @Nullable
        private final LivingEntity prey;
        private final Vec3 aim;
        private Vec3 start = Vec3.ZERO;
        private boolean struck;

        Leap(boolean pounce, @Nullable LivingEntity prey, Vec3 aim) {
            this.pounce = pounce;
            this.prey = prey;
            this.aim = aim;
        }

        /** Whether it is the Pounce (its crouch and flight are {@code Live.WOLVERINE_POUNCING}). */
        boolean pounce() {
            return pounce;
        }

        int crouch() {
            return pounce ? CROUCH : 0;
        }

        int flight() {
            return pounce ? FLIGHT : FERAL_FLIGHT;
        }

        /** Where the flight ends now: beside the prey (it moves), or the aimed point. */
        Vec3 destination(ServerPlayer player) {
            if (prey == null || !prey.isAlive() || prey.level() != player.level()) {
                return aim;
            }
            Vec3 at = prey.position();
            Vec3 back = start.subtract(at);
            back = new Vec3(back.x, 0, back.z);
            double gap = prey.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + 0.1;
            return back.lengthSqr() < 1.0E-6 ? at : at.add(back.normalize().scale(gap));
        }

        /** The point of the path at {@code s} (0 at the launch, 1 at the end): straight across, an arc up. */
        Vec3 path(Vec3 end, double s) {
            double distance = Math.hypot(end.x - start.x, end.z - start.z);
            double height = pounce ? 1.2 + distance * 0.12 : FERAL_HEIGHT;
            double x = Mth.lerp(s, start.x, end.x);
            double z = Mth.lerp(s, start.z, end.z);
            double y = Mth.lerp(s, start.y, end.y) + 4.0 * height * s * (1.0 - s);
            return new Vec3(x, y, z);
        }

        /** The prey (null for a plain leap). */
        @Nullable
        LivingEntity prey() {
            return prey;
        }

    }

    /** V: the Pounce onto the creature he looks at, or a plain leap where he looks. */
    public static boolean pounce(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        WolverineAbilities.State state = WolverineAbilities.state(player.getUUID());
        if (!data.has(HeroAbility.WOLVERINE_POUNCE) || !data.isReady(HeroAbility.WOLVERINE_POUNCE, now)
                || state.leap != null) {
            return false;
        }
        int range = WolverineAbilities.cfg(WolverineConfig.POUNCE_RANGE, 16);
        LivingEntity prey = prey(player, range);
        Vec3 aim = prey != null ? prey.position() : aim(player, range);
        data.startCooldown(HeroAbility.WOLVERINE_POUNCE, now, WolverineAbilities.cfg(WolverineConfig.POUNCE_COOLDOWN,
                100));
        begin(player, state, new Leap(true, prey, aim));
        return true;
    }

    /** Sneak + V: the Feral Leap, long and low along his look. */
    public static boolean feralLeap(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        WolverineAbilities.State state = WolverineAbilities.state(player.getUUID());
        if (!data.has(HeroAbility.WOLVERINE_FERAL_LEAP) || !data.isReady(HeroAbility.WOLVERINE_FERAL_LEAP, now)
                || state.leap != null) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 ahead = player.position().add(flat.scale(FERAL_DISTANCE));
        Level level = player.level();
        // (A wall ahead: he leaps to it.)
        BlockHitResult wall = level.clip(new ClipContext(player.position().add(0, 0.6, 0), ahead.add(0, 0.6, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            ahead = wall.getLocation().subtract(flat.scale(player.getBbWidth())).add(0, -0.6, 0);
        }
        Vec3 aim = ground(level, BlockPos.containing(ahead.x, player.getY() + 1.5, ahead.z), 12);
        data.startCooldown(HeroAbility.WOLVERINE_FERAL_LEAP, now, WolverineAbilities.cfg(
                WolverineConfig.FERAL_LEAP_COOLDOWN, 30));
        begin(player, state, new Leap(false, null, aim));
        return true;
    }

    private static void begin(ServerPlayer player, WolverineAbilities.State state, Leap leap) {
        leap.start = player.position();
        state.leap = leap;
        state.leapStart = player.level().getGameTime();
        state.fallGuardUntil = state.leapStart + 400;
        if (!leap.pounce) {
            launch(player, leap);
        }
    }

    private static void launch(ServerPlayer player, Leap leap) {
        ServerLevel level = player.serverLevel();
        leap.start = player.position();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_POUNCE.get(), leap.pounce ? 1.1F : 0.8F,
                leap.pounce ? 1.0F : 1.15F);
        WolverineAbilities.send(level, player, WolverineFxPayload.POUNCE, List.of(player.position(),
                leap.destination(player)), leap.pounce ? 0 : 1);
    }

    /** The creature the Pounce would take: the one he looks at within {@code range}, in sight, that he may hurt. */
    @Nullable
    static LivingEntity prey(ServerPlayer player, double range) {
        EntityHitResult hit = Raycasts.entity(player, range, 0.6, e -> e instanceof LivingEntity living
                && WolverineAbilities.mayHurt(player, living), 1.0F);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity prey) || !player.hasLineOfSight(prey)) {
            return null;
        }
        return prey;
    }

    /** Where a plain leap along his look lands: the ground where he looks (up to {@code range}), or that far ahead. */
    static Vec3 aim(ServerPlayer player, double range) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos pos = hit.getBlockPos();
            BlockPos above = hit.getDirection() == Direction.DOWN ? pos.below() : pos.relative(hit.getDirection());
            return ground(level, above, 8);
        }
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 ahead = player.position().add(flat.scale(range * 0.75));
        return ground(level, BlockPos.containing(ahead.x, player.getY() + 2, ahead.z), 24);
    }

    /** The top of the first solid ground at or below {@code from} (within {@code depth}), or {@code from} itself. */
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

    static void tick(ServerPlayer player, PowerData data, WolverineAbilities.State state, long now) {
        pins(player, state, now);
        Leap leap = state.leap;
        if (leap == null) {
            return;
        }
        int t = (int) (now - state.leapStart);
        if (t < leap.crouch()) {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.hurtMarked = true;
            return;
        }
        int flight = t - leap.crouch();
        if (flight == 0 && leap.pounce) {
            launch(player, leap);
        }
        if (leap.pounce && !leap.struck && touching(player, leap.prey)) {
            strike(player, data, state, leap);
            return;
        }
        int ticks = leap.flight();
        if (flight > 2 && (player.onGround() || player.isInWater()) && flight > ticks / 2) {
            finish(player, data, state, leap);
            return;
        }
        if (flight >= ticks + GRACE) {
            finish(player, data, state, leap);
            return;
        }
        if (flight < ticks) {
            Vec3 next = leap.path(leap.destination(player), (flight + 1) / (double) ticks);
            Vec3 motion = next.subtract(player.position());
            if (player.horizontalCollision && flight > 2) {
                motion = new Vec3(0, Math.min(motion.y, 0), 0);
            }
            player.setDeltaMovement(motion);
        } else {
            player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, -0.5), 0);
        }
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
    }

    /** Whether he has reached the prey (their boxes within {@link #CONTACT}). */
    static boolean touching(ServerPlayer player, @Nullable LivingEntity prey) {
        return prey != null && prey.isAlive() && prey.level() == player.level()
                && player.getBoundingBox().inflate(CONTACT).intersects(prey.getBoundingBox());
    }

    /** The end of the flight: the stab if the prey is beside him, then the landing. */
    private static void finish(ServerPlayer player, PowerData data, WolverineAbilities.State state, Leap leap) {
        if (leap.pounce && !leap.struck && leap.prey != null && leap.prey.isAlive()
                && player.getBoundingBox().inflate(CONTACT + 0.6).intersects(leap.prey.getBoundingBox())) {
            strike(player, data, state, leap);
            return;
        }
        land(player, state, leap, false);
    }

    /** The stab: the damage, the pin, and he lands beside it. */
    private static void strike(ServerPlayer player, PowerData data, WolverineAbilities.State state, Leap leap) {
        LivingEntity prey = leap.prey;
        leap.struck = true;
        ServerLevel level = player.serverLevel();
        float damage = (float) WolverineAbilities.cfg(WolverineConfig.POUNCE_DAMAGE, 12.0)
                * WolverineAbilities.rageFactor(player);
        Claws.popOut(player, data);
        if (prey != null && WolverineAbilities.hurt(player, prey, ModDamageTypes.WOLVERINE_POUNCE, damage)
                && !WolverineAbilities.isBoss(prey)) {
            int pin = WolverineAbilities.cfg(WolverineConfig.POUNCE_PIN_TICKS, 20);
            if (pin > 0) {
                prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, pin, PIN_AMPLIFIER), player);
                state.pinned.put(prey.getUUID(), level.getGameTime() + pin);
            }
            WolverineAbilities.push(prey, player.position(), 0.05, 0.0, player);
        }
        if (prey != null) {
            Vec3 beside = leap.destination(player);
            Vec3 delta = new Vec3(beside.x - player.getX(), Math.max(0, prey.getY() - player.getY()),
                    beside.z - player.getZ());
            if (level.noCollision(player, player.getBoundingBox().move(delta))) {
                player.teleportTo(player.getX() + delta.x, player.getY() + delta.y, player.getZ() + delta.z);
            }
            Vec3 at = prey.position().add(0, prey.getBbHeight() * 0.6, 0);
            WolverineAbilities.send(level, player, WolverineFxPayload.POUNCE_HIT, List.of(at), 1);
            if (ClawSlash.metal(prey)) {
                WolverineAbilities.sound(level, at, ModSounds.WOLVERINE_SLASH_HIT_METAL.get(), 1.0F, 0.9F);
            }
        }
        land(player, state, leap, true);
    }

    /** He lands on his feet: no fall damage, the thud of adamantium. */
    private static void land(ServerPlayer player, WolverineAbilities.State state, Leap leap, boolean struck) {
        state.leap = null;
        state.lastStruck = struck;
        ServerLevel level = player.serverLevel();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        state.fallGuardUntil = level.getGameTime() + 10;
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_LAND.get(), 0.9F, 1.0F);
        WolverineAbilities.sound(level, player.position(), ModSounds.ADAMANTIUM_CLANK.get(), 0.5F, 0.9F);
        if (!struck) {
            WolverineAbilities.send(level, player, WolverineFxPayload.POUNCE_HIT, List.of(player.position()), 0);
        }
    }

    /** The creatures his pounce pinned cannot jump until the pin ends. */
    private static void pins(ServerPlayer player, WolverineAbilities.State state, long now) {
        if (state.pinned.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> it = state.pinned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            Entity entity = player.serverLevel().getEntity(e.getKey());
            if (!(entity instanceof LivingEntity pinned) || !pinned.isAlive() || now > e.getValue()) {
                it.remove();
                continue;
            }
            Vec3 m = pinned.getDeltaMovement();
            if (m.y > 0.0) {
                pinned.setDeltaMovement(m.x * 0.2, 0.0, m.z * 0.2);
                pinned.hurtMarked = true;
            }
        }
    }

    /** Whether the creature is pinned by a pounce of anyone's now. */
    public static boolean pinned(ServerPlayer wolverine, LivingEntity creature) {
        WolverineAbilities.State state = WolverineAbilities.peek(wolverine.getUUID());
        Long until = state == null ? null : state.pinned.get(creature.getUUID());
        return until != null && wolverine.level().getGameTime() <= until;
    }

    /** His leap ends at once (death, a dimension, the power lost). */
    static void end(ServerPlayer player, WolverineAbilities.State state) {
        if (state.leap != null) {
            state.leap = null;
            player.fallDistance = 0.0F;
        }
        state.pinned.clear();
    }

    /** Whether the player is in a leap of his now (server side). */
    public static boolean leaping(Player player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        return state != null && state.leap != null;
    }

    /** Whether the player's last leap struck its prey (for the tests). */
    public static boolean struck(Player player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        return state != null && state.lastStruck;
    }

    /** Whether his own leap spares him the fall's damage now. */
    static boolean fallGuarded(Player player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        long now = player.level().getGameTime();
        return state != null && (state.leap != null || now <= state.fallGuardUntil);
    }

    private Pounce() {
    }
}
