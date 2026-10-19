package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.SpaceRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * V: the Teleport Belt (3.7). He blinks to the block he looks at ({@code blinkRange} 24): onto a safe two-high spot on
 * top of it, else the nearest within {@link #SEARCH} (never into a wall or a fluid, never across a Hex's wall, a boss
 * arena's edge or a Road trial's: {@link HexManager#onTeleport}); a red-black flicker and a "zzt". The belt <b>glitches</b>
 * now and then ({@code glitchChance}): he lands {@link #GLITCH_MIN}-{@link #GLITCH_MAX} blocks off (still on a safe
 * spot), with a joke. Nowhere to land: nothing happens and V waits for nothing.
 *
 * <p>Sneak + V: the Drop-In. He teleports {@code dropInHeight} above the creature he looks at ({@code dropInRange}, in
 * sight, with room above) and plunges sword first, the server carrying him down {@link #PLUNGE} blocks a tick: {@code
 * dropInDamage} where he lands on it, a ring of dust, no fall damage.
 */
public final class TeleportBelt {
    /** How far round the spot looked at a safe one is searched (blocks). */
    public static final int SEARCH = 3;
    /** How far off a glitch lands him (blocks). */
    public static final double GLITCH_MIN = 3.0;
    public static final double GLITCH_MAX = 6.0;
    /** How many places a glitch tries. */
    static final int GLITCH_TRIES = 24;
    /** The Drop-In's plunge (blocks a tick), the longest it lasts, and the guard from fall damage after it. */
    public static final double PLUNGE = 1.6;
    static final int DROP_MAX_TICKS = 40;
    static final int FALL_GUARD = 30;

    /** A Drop-In under way: onto whom, and when it began. */
    record Drop(int target, long start) {
    }

    /** V: the blink. */
    static void blink(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_BELT)) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.DEADPOOL_BELT, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos spot = aimed(player, DeadpoolAbilities.cfg(DeadpoolConfig.BLINK_RANGE, 24));
        if (spot == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.no_spot")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean glitched = false;
        if (level.random.nextDouble() < DeadpoolAbilities.cfg(DeadpoolConfig.GLITCH_CHANCE, 0.10)) {
            BlockPos off = glitch(level, player, spot, level.random);
            if (off != null) {
                spot = off;
                glitched = true;
            }
        }
        if (!go(player, Vec3.atBottomCenterOf(spot), glitched)) {
            return;
        }
        data.startCooldown(HeroAbility.DEADPOOL_BELT, now, DeadpoolAbilities.cfg(DeadpoolConfig.BLINK_COOLDOWN, 40));
        if (glitched) {
            DeadpoolLines.say(player, DeadpoolLines.Group.GLITCH, true);
        }
    }

    /**
     * The safe spot the blink takes him to: on top of the block he looks at (or before the face he looks at, dropped to
     * the ground below it), else the nearest safe spot within {@link #SEARCH}; looking at nothing, the ground below the
     * end of the range. Null if there is none.
     */
    @Nullable
    static BlockPos aimed(ServerPlayer player, int range) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        BlockPos wanted;
        if (hit.getType() == HitResult.Type.MISS) {
            wanted = ground(level, BlockPos.containing(hit.getLocation()), range);
            if (wanted == null) {
                return null;
            }
        } else if (hit.getDirection() == Direction.UP) {
            wanted = hit.getBlockPos().above();
        } else {
            BlockPos before = hit.getBlockPos().relative(hit.getDirection());
            BlockPos down = ground(level, before, 4);
            wanted = down != null ? down : before;
        }
        return nearestSafe(level, player, wanted, SEARCH);
    }

    /** The first standing place at or below {@code from}, at most {@code depth} down (the feet's block), or null. */
    @Nullable
    static BlockPos ground(ServerLevel level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos at = from.mutable();
        for (int i = 0; i <= depth && at.getY() > level.getMinBuildHeight(); i++, at.move(Direction.DOWN)) {
            BlockState below = level.getBlockState(at.below());
            if (!below.getCollisionShape(level, at.below()).isEmpty() || !below.getFluidState().isEmpty()) {
                return at.immutable();
            }
        }
        return null;
    }

    /** The nearest place within {@code radius} of {@code wanted} where he stands safely, or null. */
    @Nullable
    static BlockPos nearestSafe(ServerLevel level, ServerPlayer player, BlockPos wanted, int radius) {
        List<BlockPos> places = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(wanted.offset(-radius, -radius, -radius),
                wanted.offset(radius, radius, radius))) {
            places.add(p.immutable());
        }
        places.sort(Comparator.comparingDouble(p -> p.distSqr(wanted)));
        for (BlockPos p : places) {
            if (p.distSqr(wanted) <= radius * radius + 0.5 && safe(level, player, p)) {
                return p;
            }
        }
        return null;
    }

    /** Whether he stands safely with his feet in {@code feet}: solid harmless ground, room for him, no fluid. */
    public static boolean safe(ServerLevel level, Entity traveller, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet) || !SpaceRealms.safe(level, feet)) {
            return false;
        }
        AABB body = traveller.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(feet));
        return level.noCollision(traveller, body) && !level.containsAnyLiquid(body);
    }

    /**
     * Where a glitch lands him instead of {@code spot}: a safe spot {@link #GLITCH_MIN}-{@link #GLITCH_MAX} blocks off it
     * (horizontally), tried at random; null when there is none.
     */
    @Nullable
    static BlockPos glitch(ServerLevel level, ServerPlayer player, BlockPos spot, RandomSource random) {
        for (int i = 0; i < GLITCH_TRIES; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = GLITCH_MIN + random.nextDouble() * (GLITCH_MAX - GLITCH_MIN);
            BlockPos column = BlockPos.containing(spot.getX() + 0.5 + Math.cos(angle) * distance, spot.getY(),
                    spot.getZ() + 0.5 + Math.sin(angle) * distance);
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos feet = column.above(dy);
                double off = Math.sqrt(Math.pow(feet.getX() - spot.getX(), 2) + Math.pow(feet.getZ() - spot.getZ(), 2));
                if (off >= GLITCH_MIN && off <= GLITCH_MAX && safe(level, player, feet)) {
                    return feet;
                }
            }
        }
        return null;
    }

    /** The jump itself (refused at a Hex's wall, an arena's edge...): the flicker out, the flicker in. */
    static boolean go(ServerPlayer player, Vec3 to, boolean glitched) {
        ServerLevel level = player.serverLevel();
        if (!HexManager.onTeleport(level, player, to)) {
            return false;
        }
        Vec3 from = player.position();
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.BLINK_OUT, List.of(from), 0);
        player.stopRiding();
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        DeadpoolAbilities.send(level, player, glitched ? DeadpoolFxPayload.GLITCH : DeadpoolFxPayload.BLINK_IN,
                List.of(to, from), 0);
        DeadpoolAbilities.sound(level, from, ModSounds.DEADPOOL_BLINK.get(), 0.8F, 1.0F);
        DeadpoolAbilities.sound(level, to, glitched ? ModSounds.DEADPOOL_GLITCH.get() : ModSounds.DEADPOOL_BLINK.get(),
                0.8F, 1.15F);
        return true;
    }

    /** Sneak + V: the Drop-In. */
    static void dropIn(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_DROP_IN)) {
            return;
        }
        long now = player.level().getGameTime();
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        if (!data.isReady(HeroAbility.DEADPOOL_DROP_IN, now) || state.drop != null) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, DeadpoolAbilities.cfg(DeadpoolConfig.DROP_IN_RANGE, 32), 0.3,
                e -> e instanceof LivingEntity l && l.isAlive() && AreaTargets.mayHurt(player, l, false), 1.0F);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.no_target")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 above = above(player, target, DeadpoolAbilities.cfg(DeadpoolConfig.DROP_IN_HEIGHT, 8));
        if (above == null || !go(player, above, false)) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.no_room")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        state.drop = new Drop(target.getId(), now);
        state.fallGuardUntil = now + DROP_MAX_TICKS + FALL_GUARD;
        data.startCooldown(HeroAbility.DEADPOOL_DROP_IN, now, DeadpoolAbilities.cfg(DeadpoolConfig.DROP_IN_COOLDOWN,
                160));
        DeadpoolAbilities.send(player.serverLevel(), player, DeadpoolFxPayload.DROP_IN, List.of(above), 0);
    }

    /** The highest place, at most {@code height} over the creature's head, where he fits; null if there is no room. */
    @Nullable
    static Vec3 above(ServerPlayer player, LivingEntity target, int height) {
        ServerLevel level = player.serverLevel();
        double top = target.getY() + target.getBbHeight();
        for (int h = height; h >= 2; h--) {
            Vec3 at = new Vec3(target.getX(), target.getY() + h, target.getZ());
            if (at.y < top + 0.5) {
                break;
            }
            AABB body = player.getDimensions(Pose.STANDING).makeBoundingBox(at);
            if (level.noCollision(player, body) && !level.containsAnyLiquid(body)) {
                return at;
            }
        }
        return null;
    }

    /** Every tick of his: the plunge of a Drop-In under way. */
    static void tick(ServerPlayer player, PowerData data, DeadpoolAbilities.State state, long now) {
        Drop drop = state.drop;
        if (drop == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Entity found = level.getEntity(drop.target());
        LivingEntity target = found instanceof LivingEntity l && l.isAlive() ? l : null;
        double floor = target != null ? target.getY() + target.getBbHeight() : Double.NEGATIVE_INFINITY;
        double step = PLUNGE;
        AABB body = player.getBoundingBox();
        AABB moved = body.move(0.0, -step, 0.0);
        boolean blocked = !level.noCollision(player, moved);
        double y = player.getY() - step;
        if (target != null) {
            // Over him: steered onto his head.
            double dx = Mth.clamp(target.getX() - player.getX(), -0.5, 0.5);
            double dz = Mth.clamp(target.getZ() - player.getZ(), -0.5, 0.5);
            if (y <= floor) {
                player.teleportTo(player.getX() + dx, floor, player.getZ() + dz);
                player.resetFallDistance();
                land(player, target, now, state);
                return;
            }
            if (!blocked) {
                player.teleportTo(player.getX() + dx, y, player.getZ() + dz);
            }
        } else if (!blocked) {
            player.teleportTo(player.getX(), y, player.getZ());
        }
        player.resetFallDistance();
        if (blocked || now - drop.start() > DROP_MAX_TICKS) {
            land(player, null, now, state);
        }
    }

    /** The landing: {@code dropInDamage} to the one he lands on, a ring of dust, a thud. */
    static void land(ServerPlayer player, @Nullable LivingEntity target, long now, DeadpoolAbilities.State state) {
        state.drop = null;
        state.fallGuardUntil = now + FALL_GUARD;
        ServerLevel level = player.serverLevel();
        if (target != null) {
            DeadpoolAbilities.hurt(player, target, ModDamageTypes.DEADPOOL_KATANA,
                    DeadpoolAbilities.cfg(DeadpoolConfig.DROP_IN_DAMAGE, 8.0), player);
        }
        Vec3 at = player.position();
        BlockPos below = BlockPos.containing(at).below();
        BlockState ground = level.getBlockState(below);
        for (int i = 0; i < 16; i++) {
            double angle = i * Mth.TWO_PI / 16.0;
            double x = at.x + Math.cos(angle) * 0.8;
            double z = at.z + Math.sin(angle) * 0.8;
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, at.y + 0.1, z, 2,
                        0.05, 0.05, 0.05, 0.1);
            } else {
                level.sendParticles(ParticleTypes.POOF, x, at.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.02);
            }
        }
        DeadpoolAbilities.sound(level, at, ModSounds.DEADPOOL_DROP_IN.get(), 1.0F, 1.0F);
    }

    /** Whether his fall is guarded right now (a Drop-In's plunge or landing). */
    static boolean fallGuarded(ServerPlayer player) {
        DeadpoolAbilities.State state = DeadpoolAbilities.peek(player.getUUID());
        long now = player.level().getGameTime();
        return state != null && (state.drop != null || now <= state.fallGuardUntil);
    }

    private TeleportBelt() {
    }
}
