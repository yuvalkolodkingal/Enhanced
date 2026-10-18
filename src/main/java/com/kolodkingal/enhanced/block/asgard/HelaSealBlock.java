package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.hela.HelaLane;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hela's Seal (3.5), behind the Throne of Asgard ({@code CityPlan.HELA_SEAL}): unbreakable; clicking it (twice) calls Hela
 * ({@link HelaLane#onSeal}). The 3.5 skeleton registers it with that one hook, so lane L4 can test the call; lane L1 owns
 * the block (its look, the mural around it).
 */
public class HelaSealBlock extends Block {
    public HelaSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            HelaLane.onSeal(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
