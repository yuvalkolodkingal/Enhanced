package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The spellbooks' two thrown spells: the Ethereal Orb (the Book of Ethereal Witchcraft, the Magic power's "orb": a slow
 * ball of purple energy that bursts where it strikes and hurts what stands round that point) and the clod of earth (the
 * Book of Green Witchcraft, the Earth power's "throw": it strikes the first creature in its way and knocks it back).
 * Each flies on the server as Jim Hammond's fireballs do: a point with a velocity, drawn by the particles the server sends,
 * stopped by the first block or creature, by a Hex wall or a Chaos Shield. Neither changes a block.
 */
public final class SpellShots {
    /** What kind of spell flies. */
    public enum Kind {
        ORB, CLOD
    }

    /**
     * One spell in flight.
     *
     * @param power  its damage
     * @param reach  the orb: the radius of its burst; the clod: how hard it knocks back
     */
    private static final class Shot {
        final Kind kind;
        Vec3 pos;
        Vec3 velocity;
        double flown;
        final float power;
        final double reach;
        final int color;
        final int glow;

        Shot(Kind kind, Vec3 pos, Vec3 velocity, float power, double reach, int color, int glow) {
            this.kind = kind;
            this.pos = pos;
            this.velocity = velocity;
            this.power = power;
            this.reach = reach;
            this.color = color;
            this.glow = glow;
        }
    }

    /** How far a spell flies before it fades (blocks). */
    public static final double RANGE = 40.0;
    /** Blocks a tick: the orb drifts, the clod is thrown hard and falls. */
    public static final double ORB_SPEED = 1.1;
    public static final double CLOD_SPEED = 1.6;
    /** How much a clod falls each tick (blocks a tick, per tick). */
    public static final double CLOD_GRAVITY = 0.035;

    private static final Map<UUID, List<Shot>> SHOTS = new HashMap<>();

    private SpellShots() {
    }

    /** A spell leaves the caster's hand, where he looks. */
    public static void launch(ServerPlayer player, Kind kind, float power, double reach, int color, int glow) {
        Vec3 look = player.getLookAngle();
        Vec3 from = Shots.hand(player);
        double speed = kind == Kind.ORB ? ORB_SPEED : CLOD_SPEED;
        Vec3 velocity = look.scale(speed);
        if (kind == Kind.CLOD) {
            velocity = velocity.add(0, 0.08, 0); // (thrown a little upwards, as a hand throws)
        }
        SHOTS.computeIfAbsent(player.getUUID(), u -> new ArrayList<>()).add(new Shot(kind, from, velocity, power, reach, color, glow));
        ServerLevel level = player.serverLevel();
        if (kind == Kind.ORB) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.9F, 1.4F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    /** How many spells of this kind this player has in the air (for the tests). */
    public static int inFlight(ServerPlayer player, Kind kind) {
        List<Shot> list = SHOTS.get(player.getUUID());
        return list == null ? 0 : (int) list.stream().filter(s -> s.kind == kind).count();
    }

    /** Moves this player's spells of {@code kind} on by one tick (called from the tick of the power that threw them). */
    public static void tick(ServerPlayer player, Kind kind) {
        List<Shot> list = SHOTS.get(player.getUUID());
        if (list == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Iterator<Shot> it = list.iterator();
        while (it.hasNext()) {
            Shot shot = it.next();
            // 3.18.0: in slowed time a shot moves on every Nth tick only, a whole tick's way (so it hits as hard)
            if (shot.kind == kind && !com.kolodkingal.enhanced.ability.SlowMotion.skipsPoint(level, shot.pos,
                    System.identityHashCode(shot)) && !step(player, level, shot)) {
                it.remove();
            }
        }
        if (list.isEmpty()) {
            SHOTS.remove(player.getUUID());
        }
    }

    /** One tick of a spell, in two halves; false when it struck something or faded. */
    private static boolean step(ServerPlayer player, ServerLevel level, Shot shot) {
        for (int half = 0; half < 2; half++) {
            Vec3 from = shot.pos;
            Vec3 to = from.add(shot.velocity.scale(0.5));
            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            if (Shots.hexCut(level, from, stop) != null) {
                burst(player, level, shot, stop, null, false);
                return false;
            }
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, from, stop, 1.0F);
            if (shield != null) {
                WandaAbilities.pay(shield, player, shot.power);
                burst(player, level, shot, shield.point(), null, false);
                return false;
            }
            EntityHitResult entity = Raycasts.entityBetween(level, player, from, stop, orb(shot) ? 0.45 : 0.3, WandaAbilities.BEAM_TARGET);
            if (entity != null) {
                burst(player, level, shot, entity.getLocation(), entity.getEntity(), true);
                return false;
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                burst(player, level, shot, block.getLocation(), null, true);
                return false;
            }
            shot.pos = to;
            shot.flown += shot.velocity.length() * 0.5;
            if (shot.flown > RANGE) {
                burst(player, level, shot, shot.pos, null, false);
                return false;
            }
        }
        if (shot.kind == Kind.CLOD) {
            shot.velocity = shot.velocity.add(0, -CLOD_GRAVITY, 0);
        }
        draw(level, shot);
        return true;
    }

    private static boolean orb(Shot shot) {
        return shot.kind == Kind.ORB;
    }

    /** The spell as it flies: a glowing ball of purple light, or a clod of earth crumbling as it goes. */
    private static void draw(ServerLevel level, Shot shot) {
        Vec3 p = shot.pos;
        if (shot.kind == Kind.ORB) {
            // (a dense ball, drawn at both halves of the tick's flight so it reads as one glowing orb, not a dotted line)
            Vec3 back = p.subtract(shot.velocity.scale(0.5));
            for (Vec3 at : new Vec3[]{back, p}) {
                level.sendParticles(new EnergyParticleOptions(shot.color, 2.8F), at.x, at.y, at.z, 8, 0.14, 0.14, 0.14, 0.0);
                level.sendParticles(new EnergyParticleOptions(shot.glow, 1.6F), at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.0);
        } else {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), p.x, p.y, p.z,
                    6, 0.15, 0.15, 0.15, 0.05);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), p.x, p.y, p.z,
                    3, 0.1, 0.1, 0.1, 0.0);
            level.sendParticles(new EnergyParticleOptions(shot.color, 0.9F), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /**
     * The spell strikes at {@code at} ({@code target}: the creature it hit, or null). The orb hurts the creature it hit in
     * full and what stands round the point a little less; the clod hurts the one it hit and throws it back.
     * {@code real}: it struck something (not a fade at the end of its range, a shield or a Hex wall).
     */
    private static void burst(ServerPlayer player, ServerLevel level, Shot shot, Vec3 at, Entity target, boolean real) {
        if (shot.kind == Kind.ORB) {
            if (real) {
                if (target instanceof LivingEntity living && AreaTargets.mayHurt(player, living, true)) {
                    Shots.hurt(player, living, shot.power, ModDamageTypes.HERO_MAGIC);
                }
                for (LivingEntity e : Shots.area(player, at, shot.reach)) {
                    if (e != target) {
                        Shots.hurt(player, e, shot.power * 0.6F, ModDamageTypes.HERO_MAGIC);
                        Shots.push(e, at, 0.4, 0.15);
                    }
                }
            }
            for (int i = 0; i < 40; i++) {
                Vec3 dir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.7, level.random.nextGaussian()).normalize();
                level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? shot.glow : shot.color, 1.6F), at.x, at.y, at.z,
                        0, dir.x, dir.y, dir.z, Math.max(0.2, shot.reach * 0.15));
            }
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.05);
            Shots.ring(level, at.add(0, -0.1, 0), shot.reach, shot.color);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 0.8F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, 0.6F);
            return;
        }
        if (real && target instanceof LivingEntity living && AreaTargets.mayHurt(player, living, true)) {
            Shots.hurt(player, living, shot.power);
            Vec3 push = shot.velocity.normalize();
            living.setDeltaMovement(living.getDeltaMovement().add(push.x * shot.reach, 0.3 + shot.reach * 0.15, push.z * shot.reach));
            living.hurtMarked = true;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), at.x, at.y, at.z,
                30, 0.35, 0.35, 0.35, 0.15);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), at.x, at.y, at.z,
                14, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(new EnergyParticleOptions(shot.color, 1.2F), at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.4F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** The caster's spells of this kind fade (he lost the power). */
    public static void forget(UUID player, Kind kind) {
        List<Shot> list = SHOTS.get(player);
        if (list != null) {
            list.removeIf(s -> s.kind == kind);
            if (list.isEmpty()) {
                SHOTS.remove(player);
            }
        }
    }

    /** The caster left: none of his spells is left. */
    public static void forget(UUID player) {
        SHOTS.remove(player);
    }

    /** The server stopped. */
    public static void clear() {
        SHOTS.clear();
    }
}
