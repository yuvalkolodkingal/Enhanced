package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.item.HofundItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Hofund socket (3.5), in the middle of Heimdall's Observatory ({@code CityPlan.OBSERVATORY_SOCKET}): a gold drum
 * in which the client shows Hofund (when no player near it holds one). A player with no Hofund who touches it is told
 * that the sword is Heimdall's to give.
 */
public class HofundSocketBlock extends BaseEntityBlock {
    public static final MapCodec<HofundSocketBlock> CODEC = simpleCodec(HofundSocketBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(1, 0, 1, 15, 2, 15));

    public HofundSocketBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && !HofundItem.isHofund(player.getMainHandItem()) && !HofundItem.isHofund(player.getOffhandItem())) {
            player.displayClientMessage(Component.translatable("message.enhanced.asgard.socket").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HofundSocketBlockEntity(pos, state);
    }
}
