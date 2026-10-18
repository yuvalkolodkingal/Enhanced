package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.odin.GungnirStand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Gungnir's stand in Odin's vault (3.5, {@code CityPlan.GUNGNIR_STAND}): clicking it asks {@link GungnirStand#use} (lane L3:
 * Gungnir for the worthy). The 3.5 skeleton registers it with that hook; lane L1 owns the block (its block entity and
 * renderer, which show the spear).
 */
public class GungnirStandBlock extends Block {
    public GungnirStandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return GungnirStand.use(player, pos);
    }
}
