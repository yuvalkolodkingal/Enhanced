package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Wanda (or Jean Grey) tearing a block out of the ground. */
public final class BlockLifting {

    /** @return true if a block was lifted */
    public static boolean tryLift(ServerPlayer player) {
        return tryLift(player, Telekinesis.Mode.WANDA);
    }

    /** @return true if a block was lifted, and is now held in {@code mode} (the hero's own telekinesis) */
    public static boolean tryLift(ServerPlayer player, Telekinesis.Mode mode) {
        double range = EnhancedConfig.BLOCK_LIFT_RANGE.get();
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!canLift(level, pos, state)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.too_strong"), true);
            return true;
        }
        if (!mayBreak(player, level, pos, state)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.protected"), true);
            return true;
        }

        BlockState lifted = state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
        // Plays the break sound and particles, leaves water behind for waterlogged blocks, drops nothing.
        level.destroyBlock(pos, false, player);
        TelekineticBlockEntity entity = new TelekineticBlockEntity(level, pos, lifted, player);
        level.addFreshEntity(entity);
        Telekinesis.hold(player, entity, mode);
        return true;
    }

    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity() || state.is(ModTags.TELEKINESIS_IMMOVABLE)) {
            return false;
        }
        // Two-block-tall things (doors, tall flowers) and beds would be torn in half.
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) {
            return false;
        }
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= EnhancedConfig.BLOCK_LIFT_MAX_HARDNESS.get();
    }

    /**
     * Respects spawn protection, Adventure mode, world borders and claim mods (through the block-break event), and the
     * arena of a boss fight.
     */
    private static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (BossFights.protects(level, pos)) {
            return false; // the arena of a boss fight stays as it is
        }
        if (!player.mayBuild() || !level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    private BlockLifting() {
    }
}
