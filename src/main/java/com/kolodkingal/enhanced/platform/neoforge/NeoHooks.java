package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.platform.LoaderHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

/** The loader hooks on NeoForge: {@code EventHooks}, {@code CommonHooks}, the item abilities and fluid types. */
final class NeoHooks implements LoaderHooks {
    @Override
    public boolean blockPlaceCancelled(@Nullable Entity placer, Level level, BlockPos pos, Direction against) {
        return EventHooks.onBlockPlace(placer, BlockSnapshot.create(level.dimension(), level, pos), against);
    }

    @Override
    public boolean blockBreakCancelled(Level level, GameType gameType, ServerPlayer player, BlockPos pos,
                                       BlockState state) {
        return CommonHooks.fireBlockBreak(level, gameType, player, pos, state).isCanceled();
    }

    @Override
    public boolean blockBreakEventCancelled(Level level, BlockPos pos, BlockState state, Player player) {
        return NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }

    @Override
    public boolean projectileImpactCancelled(Projectile projectile, HitResult hit) {
        return EventHooks.onProjectileImpact(projectile, hit);
    }

    @Override
    public @Nullable SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty,
                                                     MobSpawnType type, @Nullable SpawnGroupData data) {
        return EventHooks.finalizeMobSpawn(mob, level, difficulty, type, data);
    }

    @Override
    public boolean enderPearlTeleportCancelled(ServerPlayer player, double x, double y, double z,
                                               ThrownEnderpearl pearl, float attackDamage, HitResult hit) {
        return EventHooks.onEnderPearlLand(player, x, y, z, pearl, attackDamage, hit).isCanceled();
    }

    @Override
    public boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    @Override
    public boolean drownsInFluidAtEyes(LivingEntity entity) {
        FluidType eyes = entity.getEyeInFluidType();
        return !eyes.isAir() && entity.canDrownInFluidType(eyes);
    }
}
