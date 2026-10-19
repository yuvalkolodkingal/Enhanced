package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * G: the Thunder Clap (3.6). He spreads his arms wide and claps them together in front of him: a shockwave runs forward
 * in a cone ({@code clapAngle} wide, {@code clapLength} long, 1.4 blocks a tick), and every creature it passes takes
 * {@code clapDamage} and is thrown far ({@code clapKnockback} out, 0.5 up: "it knocks them quite far"). The projectiles
 * in it are turned back, fire in it is put out (blocks and creatures, {@code clapExtinguishes}), players it hits hear a
 * ringing (Slowness II for 2 s), and its glass, panes and snow shatter (by the terrain rules). The Thunder Clap is a
 * classic Hulk move of the comics and the games; ours follows Eilon's words.
 */
public final class ThunderClap implements HulkAbilities.Move {
    /** The ticks his arms are wide before the hands meet. */
    static final int WINDUP = 5;
    /** How fast the wave runs (blocks a tick). */
    static final double SPEED = 1.4;
    /** How far up it throws. */
    static final double LIFT = 0.5;

    private final IntSet hit = new IntOpenHashSet();
    private final IntSet turned = new IntOpenHashSet();
    private Vec3 origin = Vec3.ZERO;
    private Vec3 dir = new Vec3(1, 0, 0);
    /** The wave's damage, fixed when the hands meet. */
    private float amount;

    /** G pressed: the clap begins. */
    public static boolean start(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_CLAP, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.HULK_CLAP, now, HulkAbilities.cfg(HulkConfig.CLAP_COOLDOWN, 100));
        HulkAbilities.startMove(player, HulkAbilities.state(player.getUUID()), new ThunderClap());
        HulkAbilities.send(player.serverLevel(), player, HulkFxPayload.CLAP, List.of(player.position()), 0);
        return true;
    }

    /** The cone's half-angle's cosine. */
    static double halfCos() {
        return Math.cos(Math.toRadians(HulkAbilities.cfg(HulkConfig.CLAP_ANGLE, 50) / 2.0));
    }

    /**
     * Whether a point lies in the cone from {@code origin} along {@code dir} (horizontal), between {@code from} and
     * {@code to}.
     */
    static boolean inCone(Vec3 origin, Vec3 dir, Vec3 point, double from, double to, double halfCos) {
        Vec3 d = point.subtract(origin);
        Vec3 flat = new Vec3(d.x, 0.0, d.z);
        double distance = flat.length();
        if (distance < from || distance > to) {
            return false;
        }
        if (distance < 0.75) {
            return true; // (right at his hands)
        }
        return flat.normalize().dot(dir) >= halfCos && Math.abs(d.y) <= 3.0 + distance * 0.25;
    }

    @Override
    public boolean tick(ServerPlayer player, PowerData data, int t) {
        if (t < WINDUP) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        int length = HulkAbilities.cfg(HulkConfig.CLAP_LENGTH, 14);
        if (t == WINDUP) {
            Vec3 look = player.getLookAngle();
            dir = new Vec3(look.x, 0.0, look.z);
            dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
            origin = player.position().add(0, player.getBbHeight() * 0.45, 0);
            HulkAbilities.sound(level, origin, ModSounds.HULK_CLAP.get(), 2.0F, 0.95F
                    + level.random.nextFloat() * 0.1F);
            HulkAbilities.send(level, player, HulkFxPayload.CLAP, List.of(origin, origin.add(dir)), length);
            amount = HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.CLAP_DAMAGE, 6.0));
        }
        double from = (t - WINDUP) * SPEED;
        double to = Math.min(length, from + SPEED);
        wave(player, data, level, from, to);
        return to >= length;
    }

    /** One tick of the wave: from {@code from} to {@code to} blocks out. */
    private void wave(ServerPlayer player, PowerData data, ServerLevel level, double from, double to) {
        double halfCos = halfCos();
        double push = HulkAbilities.cfg(HulkConfig.CLAP_KNOCKBACK, 3.0);
        boolean douse = HulkAbilities.cfg(HulkConfig.CLAP_EXTINGUISHES, true);
        AABB box = new AABB(origin, origin).inflate(to + 2.0, 4.0 + to * 0.25, to + 2.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                && !hit.contains(e.getId()))) {
            Vec3 middle = e.position().add(0, e.getBbHeight() * 0.5, 0);
            if (!inCone(origin, dir, middle, from - e.getBbWidth() * 0.5, to + e.getBbWidth() * 0.5, halfCos)) {
                continue;
            }
            hit.add(e.getId());
            if (douse && e.isOnFire() && (HulkAbilities.mayHurt(player, e) || !(e instanceof Player))) {
                e.clearFire();
            }
            if (!HulkAbilities.mayHurt(player, e)) {
                continue;
            }
            if (HulkAbilities.hurt(player, e, ModDamageTypes.HULK_CLAP, amount, player)) {
                Vec3 behind = e.position().subtract(dir);
                HulkAbilities.push(e, behind, push, LIFT, player);
                if (e instanceof Player other) {
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), player);
                }
            }
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> !turned.contains(p.getId())
                && p.getOwner() != player)) {
            if (!inCone(origin, dir, projectile.position(), from - 0.5, to + 0.5, halfCos)) {
                continue;
            }
            turned.add(projectile.getId());
            turnBack(player, projectile);
        }
        // Fire, glass and snow along the wave's front.
        int r = (int) Math.ceil(to);
        BlockPos base = BlockPos.containing(origin);
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -2, -r), base.offset(r, 2, r))) {
            Vec3 cell = Vec3.atCenterOf(pos);
            if (!inCone(origin, dir, cell, from, to, halfCos)) {
                continue;
            }
            if (douse && level.getBlockState(pos).getBlock() instanceof BaseFireBlock && level.mayInteract(player,
                    pos)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else {
                HulkTerrain.clap(player, pos.immutable());
            }
        }
    }

    /** A projectile in the wave flies back the way it came, his now. */
    static void turnBack(ServerPlayer player, Projectile projectile) {
        Vec3 velocity = projectile.getDeltaMovement();
        // (A fireball's push follows its own motion: turned round, it pushes the new way.)
        projectile.setDeltaMovement(velocity.scale(-1.0));
        projectile.setOwner(player);
        projectile.hurtMarked = true;
    }
}
