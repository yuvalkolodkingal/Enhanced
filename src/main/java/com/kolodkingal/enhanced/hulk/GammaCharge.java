package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sneak + B: the Gamma Charge (3.6). Shoulder first, {@code chargeDistance} blocks over as many ticks along his look
 * (level): every creature in his path takes {@code chargeDamage} and is thrown aside. He stops at a wall, unless the
 * terrain rules let him through it ({@link HulkTerrain}: glass in {@code soft}, more in {@code all}).
 */
public final class GammaCharge implements HulkAbilities.Move {
    /** How far to the side his path reaches past his own width, and how hard it throws aside. */
    static final double REACH = 0.8;
    static final double ASIDE = 1.6;
    static final double LIFT = 0.35;

    private final Vec3 dir;
    private final int distance;
    private final IntSet hit = new IntOpenHashSet();
    private boolean stopped;
    /** The charge's damage, fixed when it begins. */
    private float amount = -1.0F;

    private GammaCharge(Vec3 dir, int distance) {
        this.dir = dir;
        this.distance = distance;
    }

    /** Sneak + B pressed: the charge begins. */
    public static boolean start(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_GAMMA_CHARGE, now)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
        data.startCooldown(HeroAbility.HULK_GAMMA_CHARGE, now, HulkAbilities.cfg(HulkConfig.CHARGE_COOLDOWN, 140));
        HulkAbilities.startMove(player, HulkAbilities.state(player.getUUID()), new GammaCharge(dir,
                HulkAbilities.cfg(HulkConfig.CHARGE_DISTANCE, 12)));
        ServerLevel level = player.serverLevel();
        HulkAbilities.sound(level, player.position(), ModSounds.HULK_CHARGE.get(), 1.4F, 1.0F);
        HulkAbilities.send(level, player, HulkFxPayload.CHARGE, List.of(player.position(), player.position().add(dir)),
                0);
        return true;
    }

    @Override
    public Live live() {
        return Live.HULK_CHARGING;
    }

    @Override
    public boolean tick(ServerPlayer player, PowerData data, int t) {
        if (stopped || t >= distance) {
            return true;
        }
        ServerLevel level = player.serverLevel();
        Direction facing = Direction.getNearest(dir.x, 0, dir.z);
        BlockPos feet = BlockPos.containing(player.position().add(dir.scale(player.getBbWidth() * 0.5)));
        if (!HulkTerrain.chargeThrough(player, feet, facing)) {
            stop(player, level);
            return true;
        }
        Vec3 from = player.position();
        Vec3 next = from.add(dir);
        AABB moved = player.getBoundingBox().move(dir);
        if (!level.noCollision(player, moved)) {
            // Up a step if one is in the way; else the wall stops him.
            AABB up = moved.move(0, 1.0, 0);
            if (level.noCollision(player, up)) {
                next = next.add(0, 1.0, 0);
            } else {
                stop(player, level);
                return true;
            }
        }
        player.setDeltaMovement(next.subtract(from).add(0, -0.08, 0));
        player.hurtMarked = true;
        if (amount < 0.0F) {
            amount = HulkAbilities.damage(player, data, HulkAbilities.cfg(HulkConfig.CHARGE_DAMAGE, 6.0));
        }
        AABB path = player.getBoundingBox().expandTowards(dir.scale(1.5)).inflate(REACH, 0.2, REACH);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> e != player
                && !hit.contains(e.getId())
                && HulkAbilities.mayHurt(player, e))) {
            hit.add(e.getId());
            if (HulkAbilities.hurt(player, e, ModDamageTypes.HULK_CHARGE, amount, player)) {
                // Aside: away from his line, to whichever side it stands.
                Vec3 rel = e.position().subtract(from);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                double s = Math.signum(rel.dot(side));
                Vec3 push = side.scale(s == 0 ? 1 : s).add(dir.scale(0.5));
                HulkAbilities.push(e, e.position().subtract(push), ASIDE, LIFT, player);
            }
        }
        return false;
    }

    private void stop(ServerPlayer player, ServerLevel level) {
        stopped = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        HulkAbilities.sound(level, player.position(), ModSounds.HULK_SLAM.get(), 1.0F, 0.7F);
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 1.0, 0.2));
        player.hurtMarked = true;
    }

    boolean stopped() {
        return stopped;
    }
}
