package com.kolodkingal.enhanced.mind;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The projector at the back of Vision's cinema (3.12): used, it shows the title card on the screen (a painting), and
 * used again it puts it away. Anywhere else it does nothing.
 */
public class CinemaProjectorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CinemaProjectorBlock> CODEC = simpleCodec(CinemaProjectorBlock::new);

    public CinemaProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.dimension() == MindRealm.LEVEL) {
            BlockPos screen = MindBuilder.screen(MindRealm.origin(MindRealm.plotAt(pos)));
            Painting shown = MindPaintings.find(server, screen, MindPaintings.CINEMA);
            if (shown != null) {
                shown.discard();
                server.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
            } else {
                MindPaintings.place(server, screen, Direction.EAST, MindPaintings.CINEMA);
                server.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.8F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
