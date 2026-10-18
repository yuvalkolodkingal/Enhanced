package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.asgard.RuneStones;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A Bifrost rune stone (3.5), in the middle of a Bifrost site in the Overworld: unbreakable, no item. Right-click: the
 * player calls on Heimdall and, if the stone is ready, the Bifrost takes him, and everyone in its circle, to Asgard
 * ({@link RuneStones#use}). Its runes glimmer now and then.
 */
public class BifrostRuneStoneBlock extends BaseEntityBlock {
    public static final MapCodec<BifrostRuneStoneBlock> CODEC = simpleCodec(BifrostRuneStoneBlock::new);

    public BifrostRuneStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            RuneStones.use(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 0.2 + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BifrostRuneStoneBlockEntity(pos, state);
    }
}
