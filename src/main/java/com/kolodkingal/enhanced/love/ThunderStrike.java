package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Love's Thunder Strike (3.8, PLAN 10.6; Sneak + R): a bolt of lightning falls on the block she looks at within {@code
 * [love] strikeRange} (48): vanilla's bolt as a picture only ({@code setVisualOnly}), and {@code strikeDamage} (8,
 * {@code love_lightning}) to every creature within {@code strikeRadius} (2) of where it lands, by the game's rules of
 * who may hurt whom. Blocks catch fire only with {@code strikeSetsFire} and {@code mobGriefing}. It rests {@code
 * strikeCooldown} ticks, on its own clock (the Axe Throw on the same key keeps its own short one).
 */
public final class ThunderStrike {
    /** Player → when the strike may fall again. */
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    /** Ticks until her next strike (0: ready). */
    public static long cooldown(ServerPlayer player) {
        Long at = READY_AT.get(player.getUUID());
        return at == null ? 0 : Math.max(0, at - player.level().getGameTime());
    }

    /**
     * The strike, where she looks.
     *
     * @return whether it fell
     */
    public static boolean strike(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (cooldown(player) > 0) {
            return false;
        }
        int range = LoveAbilities.cfg(EternityConfig.LOVE_STRIKE_RANGE, 48);
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.enhanced.love.strike_nothing")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        Vec3 at = hit.getLocation();
        READY_AT.put(player.getUUID(), now + LoveAbilities.cfg(EternityConfig.LOVE_STRIKE_COOLDOWN, 100));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            bolt.setCause(player);
            level.addFreshEntity(bolt);
        }
        float damage = (float) LoveAbilities.cfg(EternityConfig.LOVE_STRIKE_DAMAGE, 8.0);
        double radius = LoveAbilities.cfg(EternityConfig.LOVE_STRIKE_RADIUS, 2.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1.0),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 middle = target.position().add(0, target.getBbHeight() * 0.5, 0);
            if (target.getBoundingBox().distanceToSqr(at) > radius * radius
                    && middle.distanceToSqr(at) > radius * radius) {
                continue;
            }
            if (AreaTargets.mayHurt(player, target, true)) {
                target.hurt(ModDamageTypes.source(level, ModDamageTypes.LOVE_LIGHTNING, player, player), damage);
            }
        }
        if (LoveAbilities.cfg(EternityConfig.LOVE_STRIKE_SETS_FIRE, false)
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            BlockPos fire = hit.getBlockPos().relative(hit.getDirection());
            if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, hit.getDirection())) {
                level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.2, at.z, 30, 0.6, 0.3, 0.6, 0.4);
        level.playSound(null, at.x, at.y, at.z, ModSounds.LOVE_THUNDER_STRIKE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 96,
                new EternityFxPayload(EternityFxPayload.THUNDER_STRIKE, player.getId(), BlockPos.containing(at), 0));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    static void forget(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
    }

    static void clear() {
        READY_AT.clear();
    }

    private ThunderStrike() {
    }
}
