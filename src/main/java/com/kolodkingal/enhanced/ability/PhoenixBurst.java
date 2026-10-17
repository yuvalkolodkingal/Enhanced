package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The waves of the Phoenix Burst. A Burst is a sphere of cosmic fire that grows from where she hovered at
 * {@code burstWaveSpeed} blocks per tick up to its radius and hurts every creature it reaches once, so a creature 20
 * blocks away is hit 20 ticks after one at 1 block. It never breaks a block; on flammable surfaces (only with
 * mobGriefing on and {@code burstSetsFire}) it may light a few fires, which vanilla then lets burn out.
 *
 * <p>The damage type is {@code enhanced:phoenix_burst}: it goes through armour (so a Netherite zombie takes all of it)
 * and is not fire (so a Blaze takes it), is Phoenix mind power (it breaks Apocalypse's shield) and does not go through
 * a phased Vision. Who it may hurt is the shared rule of area attacks ({@link AreaTargets#mayHurt}): hostile creatures,
 * creatures after her, the bosses, and players with PvP on; the others only if {@code burstHurtsPassives} says so. Never
 * herself, her pets, her team, her hosts, Creative or Spectator players.
 */
public final class PhoenixBurst {
    /** A creature at the edge of the wave takes this much of the damage of one near the centre. */
    public static final float EDGE_FACTOR = 0.6F;
    /** Fires are tried for this many random columns of the wave's footprint per tick. */
    private static final int FIRE_TRIES = 6;
    /** The wave is above ground: the footprint of a fire is searched this far above and below its centre. */
    private static final int FIRE_REACH_UP = 4;
    private static final int FIRE_REACH_DOWN = 6;

    /** One wave on its way. */
    private static final class Wave {
        final UUID owner;
        final ResourceKey<Level> dimension;
        final Vec3 centre;
        final long start;
        final float radius;
        final float damage;
        final float charge;
        final IntSet hit = new IntOpenHashSet();
        int fires;

        Wave(UUID owner, ResourceKey<Level> dimension, Vec3 centre, long start, float radius, float damage, float charge) {
            this.owner = owner;
            this.dimension = dimension;
            this.centre = centre;
            this.start = start;
            this.radius = radius;
            this.damage = damage;
            this.charge = charge;
        }
    }

    private static final List<Wave> WAVES = new ArrayList<>();

    /** The radius of a Burst of this charge (0 to 1). */
    public static float radius(float charge) {
        return Mth.lerp(Mth.clamp(charge, 0.0F, 1.0F), PhoenixConfig.BURST_MIN_RADIUS.get(), PhoenixConfig.BURST_MAX_RADIUS.get());
    }

    /** The damage at the centre of a Burst of this charge. */
    public static float damage(float charge) {
        return Mth.lerp(Mth.clamp(charge, 0.0F, 1.0F), PhoenixConfig.BURST_MIN_DAMAGE.get().floatValue(),
                PhoenixConfig.BURST_MAX_DAMAGE.get().floatValue());
    }

    /** Lets a wave go from {@code centre}, as strong as the charge (0 to 1) made it. It starts to spread this tick. */
    public static void start(ServerPlayer owner, Vec3 centre, float charge) {
        WAVES.add(new Wave(owner.getUUID(), owner.level().dimension(), centre, owner.level().getGameTime(), radius(charge), damage(charge), charge));
    }

    /** How many waves are on their way (a test waits for it to be 0). */
    public static int active() {
        return WAVES.size();
    }

    /** Forget every wave (the server stopped, or a test ends). */
    public static void clear() {
        WAVES.clear();
    }

    /** Every level tick: the waves of this level spread another step. */
    public static void tickLevel(ServerLevel level) {
        if (WAVES.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Wave> it = WAVES.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            if (wave.dimension != level.dimension()) {
                continue;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(wave.owner);
            if (owner == null || owner.level() != level) {
                it.remove(); // she left the server or the dimension: the fire goes out with her
                continue;
            }
            double reach = Math.min(wave.radius, PhoenixConfig.BURST_WAVE_SPEED.get() * (now - wave.start + 1));
            spread(level, owner, wave, reach);
            if (reach >= wave.radius) {
                it.remove();
            }
        }
    }

    /** The wave has grown to {@code reach} blocks: everything new inside it is burned once. */
    private static void spread(ServerLevel level, ServerPlayer owner, Wave wave, double reach) {
        AABB box = new AABB(wave.centre, wave.centre).inflate(reach);
        List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, box, e -> !wave.hit.contains(e.getId()) && e.isAlive()
                && bodyCentre(e).distanceToSqr(wave.centre) <= reach * reach);
        inside.sort(Comparator.comparingDouble(e -> bodyCentre(e).distanceToSqr(wave.centre)));
        for (LivingEntity target : inside) {
            wave.hit.add(target.getId());
            if (!PhoenixAbilities.isHers(owner, target) && AreaTargets.mayHurt(owner, target, PhoenixConfig.BURST_HURTS_PASSIVES.get())) {
                burn(level, owner, wave, target);
            }
        }
        if (PhoenixConfig.BURST_SETS_FIRE.get() && wave.fires < PhoenixConfig.BURST_MAX_FIRE_BLOCKS.get()
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            kindle(level, owner, wave, reach);
        }
    }

    private static Vec3 bodyCentre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    /** One creature in the wave: the damage (with the falloff, the cover and the players' share), the push and the fire. */
    private static void burn(ServerLevel level, ServerPlayer owner, Wave wave, LivingEntity target) {
        Vec3 at = bodyCentre(target);
        double distance = at.distanceTo(wave.centre);
        float cover = Explosion.getSeenPercent(wave.centre, target);
        if (cover <= 0.0F) {
            return; // stone between them and the Phoenix: the wave does not get there
        }
        float half = wave.radius * 0.5F;
        float falloff = distance <= half ? 1.0F : Mth.lerp((float) ((distance - half) / Math.max(half, 1.0E-3)), 1.0F, EDGE_FACTOR);
        float amount = wave.damage * Mth.clamp(falloff, EDGE_FACTOR, 1.0F) * cover;
        if (target instanceof Player) {
            amount *= PhoenixConfig.BURST_PLAYER_FACTOR.get().floatValue();
        }
        // The listeners hear of it before anything refuses it (a phased Vision, a shield).
        PhoenixPower.touched(owner, target, PhoenixPower.Kind.BURST, amount);
        boolean landed = target.hurt(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, owner, owner), amount);
        if (!landed && Shelters.shelters(owner, wave.centre, target)) {
            return; // 3.3: Billy's barrier faced the wave and stopped all of it (its push and its fire too)
        }

        double push = PhoenixConfig.BURST_KNOCKBACK.get() * cover;
        if (push > 0.0 && !target.getType().is(ModTags.PIN_IMMUNE) && !MjolnirPin.isPinned(target)) {
            target.knockback(push, wave.centre.x - at.x, wave.centre.z - at.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.25 * cover, 0));
            target.hurtMarked = true;
        }
        float seconds = Mth.lerp(wave.charge, 0.4F, 1.0F) * PhoenixConfig.BURST_FIRE_SECONDS.get();
        if (seconds > 0.0F && !target.fireImmune()) {
            target.igniteForSeconds(seconds);
        }
    }

    /**
     * Sets a few fires where the wave passes over something that burns: random columns of its footprint, the first
     * flammable surface from {@code FIRE_REACH_UP} above its centre down, fire in the air above it. Never in a boss
     * arena, never where building is not allowed (spawn protection, claims), at most {@code burstMaxFireBlocks}.
     */
    private static void kindle(ServerLevel level, ServerPlayer owner, Wave wave, double reach) {
        int max = PhoenixConfig.BURST_MAX_FIRE_BLOCKS.get();
        for (int i = 0; i < FIRE_TRIES && wave.fires < max; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double r = Math.sqrt(level.random.nextDouble()) * reach;
            int x = Mth.floor(wave.centre.x + Math.cos(angle) * r);
            int z = Mth.floor(wave.centre.z + Math.sin(angle) * r);
            int top = Mth.floor(wave.centre.y) + FIRE_REACH_UP;
            for (int y = top; y >= top - FIRE_REACH_UP - FIRE_REACH_DOWN; y--) {
                BlockPos floor = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(floor);
                if (state.isAir()) {
                    continue;
                }
                BlockPos above = floor.above();
                if (state.isFlammable(level, floor, Direction.UP) && level.isEmptyBlock(above)
                        && !BossFights.protects(level, above) && level.mayInteract(owner, above) && !BlockRights.behindTheWall(owner, level, above)) {
                    BlockState fire = BaseFireBlock.getState(level, above);
                    if (fire.canSurvive(level, above) && level.setBlockAndUpdate(above, fire)) {
                        wave.fires++;
                    }
                }
                break; // the first solid thing from above is the surface
            }
        }
    }

    private PhoenixBurst() {
    }
}
