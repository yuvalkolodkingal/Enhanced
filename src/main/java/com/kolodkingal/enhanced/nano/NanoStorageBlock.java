package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The nanotech storage of the Horton manor (3.12): a low console with a ring of spokes. Touched by anyone but Ultron,
 * it wakes Nano Ultron (one at a time); touched by a player who is Ultron, it gives him his nanotech body.
 */
public class NanoStorageBlock extends Block {
    public static final MapCodec<NanoStorageBlock> CODEC = simpleCodec(NanoStorageBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    public NanoStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            if (Powers.is(sp, Power.ULTRON)) {
                NanoUltron.upgrade(sp);
            } else if (NanoUltronEntity.near(server, pos) != null) {
                sp.displayClientMessage(Component.translatable("message.enhanced.nano_ultron.already"), true);
            } else {
                NanoUltronEntity boss = ModEntities.NANO_ULTRON.get().create(server);
                if (boss != null) {
                    boss.moveTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, sp.getYRot() + 180.0F, 0.0F);
                    boss.setHome(pos);
                    boss.setTarget(sp);
                    server.addFreshEntity(boss);
                    server.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.4F);
                    sp.displayClientMessage(Component.translatable("message.enhanced.nano_ultron.wake"), false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
