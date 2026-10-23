package com.kolodkingal.enhanced.platform;

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
import org.jetbrains.annotations.Nullable;

/**
 * The loader's hooks that the mod fires itself, so that other mods (and the mod's own handlers) see what the mod does as
 * if a player did it. Each returns whether the action was cancelled. Also the loader's answers to questions vanilla
 * cannot answer alone (what counts as a shield, which fluids drown).
 */
public interface LoaderHooks {
    /**
     * Whether placing a block at {@code pos} by {@code placer} is cancelled (protection mods, the mod's own rules): the
     * block-place event, with a snapshot of the block there now.
     */
    boolean blockPlaceCancelled(@Nullable Entity placer, Level level, BlockPos pos, Direction against);

    /** Whether {@code player} breaking the block at {@code pos} is cancelled (the block-break event). */
    boolean blockBreakCancelled(Level level, GameType gameType, ServerPlayer player, BlockPos pos, BlockState state);

    /**
     * Whether the loader's plain block-break event for {@code player} breaking {@code state} at {@code pos} is
     * cancelled (claim mods, the mod's own break rules): only the event, posted to every listener, unlike
     * {@link #blockBreakCancelled} (the loader's whole pre-break check, which also refuses a sword in Creative and
     * sends block updates). NeoForge: {@code NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(...)).isCanceled()}.
     */
    boolean blockBreakEventCancelled(Level level, BlockPos pos, BlockState state, Player player);

    /** Whether a projectile's impact is cancelled (the projectile-impact event); call it before acting on the hit. */
    boolean projectileImpactCancelled(Projectile projectile, HitResult hit);

    /** Finishes a mob's spawn as the loader does (its spawn event, then {@code Mob#finalizeSpawn}). */
    @Nullable
    SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                    @Nullable SpawnGroupData data);

    /** Whether an ender pearl's teleport of {@code player} to {@code x, y, z} is cancelled (the pearl-teleport event). */
    boolean enderPearlTeleportCancelled(ServerPlayer player, double x, double y, double z, ThrownEnderpearl pearl,
                                        float attackDamage, HitResult hit);

    /**
     * Whether {@code stack} is a shield that blocks (vanilla's, the Vibranium Shield, other mods' shields). NeoForge:
     * {@code canPerformAction(ItemAbilities.SHIELD_BLOCK)}; false for an empty stack.
     */
    boolean isShield(ItemStack stack);

    /**
     * Whether the fluid at {@code entity}'s eyes is one it can drown in (water; other mods' fluids by their own rule).
     * In water this already answers false for an entity that breathes there ({@code canBreatheUnderwater}: a zombie,
     * a fish). NeoForge: {@code getEyeInFluidType()} is not air and {@code canDrownInFluidType} of it.
     */
    boolean drownsInFluidAtEyes(LivingEntity entity);
}
