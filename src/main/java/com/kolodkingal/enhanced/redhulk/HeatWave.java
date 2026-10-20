package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * G: the Heat Wave (3.8, PLAN 4.4). He spreads his arms and a cone of heat ({@code heatLength} long, {@code heatAngle}
 * wide) rolls out along his look: {@code heatDamage} ({@code red_hulk_burst}) and {@code heatFireSeconds} of burning to
 * every creature in it he may hurt. With {@code heatMeltsSnow} and the game rule {@code mobGriefing} (and where a hand
 * of his could break), the snow layers in the cone melt away and its ice turns to water.
 */
public final class HeatWave {
    private HeatWave() {
    }

    /** Whether {@code point} lies in the cone from {@code eye} along {@code look} ({@code length}, half-angle). */
    static boolean inCone(Vec3 eye, Vec3 look, Vec3 point, double length, double halfAngleDegrees) {
        Vec3 to = point.subtract(eye);
        double along = to.dot(look);
        if (along <= 0.0 || along > length) {
            return false;
        }
        double cos = along / Math.max(1.0E-6, to.length());
        return cos >= Math.cos(Math.toRadians(halfAngleDegrees));
    }

    /** G: the wave; returns the creatures it burned (empty when it is not ready). */
    public static List<LivingEntity> wave(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_HEAT_WAVE, now)) {
            return List.of();
        }
        data.startCooldown(HeroAbility.RED_HULK_HEAT_WAVE, now, RedHulkAbilities.cfg(RedHulkConfig.HEAT_COOLDOWN,
                120));
        double length = RedHulkAbilities.cfg(RedHulkConfig.HEAT_LENGTH, 10.0);
        double half = RedHulkAbilities.cfg(RedHulkConfig.HEAT_ANGLE, 50.0) * 0.5;
        float damage = (float) RedHulkAbilities.cfg(RedHulkConfig.HEAT_DAMAGE, 5.0);
        int fire = RedHulkAbilities.cfg(RedHulkConfig.HEAT_FIRE_SECONDS, 4) * 20;
        Vec3 eye = player.getEyePosition().subtract(0, player.getBbHeight() * 0.25, 0);
        Vec3 look = player.getLookAngle();
        List<LivingEntity> burned = new ArrayList<>();
        AABB box = new AABB(eye, eye).inflate(length + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                && RedHulkAbilities.mayHurt(player, e))) {
            Vec3 middle = e.position().add(0, e.getBbHeight() * 0.5, 0);
            if (!inCone(eye, look, middle, length + e.getBbWidth() * 0.5, half + 8.0)
                    && !inCone(eye, look, e.position(), length, half + 8.0)) {
                continue;
            }
            if (RedHulkAbilities.hurt(player, e, ModDamageTypes.RED_HULK_BURST, damage, player)) {
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), fire));
                burned.add(e);
            }
        }
        if (RedHulkAbilities.cfg(RedHulkConfig.HEAT_MELTS_SNOW, true)) {
            melt(player, eye, look, length, half);
        }
        RedHulkAbilities.sound(level, player.position(), ModSounds.RED_HULK_HEAT_WAVE.get(), 1.6F, 1.0F);
        RedHulkAbilities.send(level, player, RedHulkFxPayload.HEAT_WAVE, List.of(eye, eye.add(look)),
                (int) Math.round(length));
        return burned;
    }

    /** The snow and ice in the cone (the game rule and his rights asked for every cell). */
    static int melt(ServerPlayer player, Vec3 eye, Vec3 look, double length, double half) {
        ServerLevel level = player.serverLevel();
        int melted = 0;
        BlockPos origin = BlockPos.containing(eye);
        int r = (int) Math.ceil(length);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 3, r))) {
            BlockState state = level.getBlockState(pos);
            boolean snow = state.is(Blocks.SNOW);
            boolean ice = state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
            if (!snow && !ice || !inCone(eye, look, pos.getCenter(), length, half + 6.0)
                    || !BlockRights.mayBlast(player, level, pos)) {
                continue;
            }
            level.setBlock(pos, snow ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), 3);
            melted++;
        }
        return melted;
    }
}
