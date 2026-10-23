package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The door to the Witches' Road (Agatha All Along): a pair of old cellar doors set flat in the floor, outlined in
 * blue-green light. Closed, faint sparks run along the outline now and then; open, the doors are gone and the light pours
 * up out of the floor. What right-clicking it does is up to {@link WitchesRoad}.
 */
public class WitchesRoadDoorBlock extends Block {
    public static final MapCodec<WitchesRoadDoorBlock> CODEC = simpleCodec(WitchesRoadDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** A low slab, 3 pixels tall: you stand on it. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);

    public WitchesRoadDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            WitchesRoad.use(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            WitchesRoad.addDoor(serverLevel, pos);
            if (state.getValue(OPEN)) {
                // Set down open (a command, a structure): it still closes by itself.
                WitchesRoad.scheduleClose(serverLevel, pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && level instanceof ServerLevel serverLevel) {
            WitchesRoad.removeDoor(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** An open door closes again (scheduled when it opened). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WitchesRoad.close(level, pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double top = pos.getY() + WitchesRoad.DOOR_HEIGHT;
        if (state.getValue(OPEN)) {
            for (int i = 0; i < 6; i++) {
                int color = i % 3 == 0 ? WitchesRoad.LIGHT_CORE : WitchesRoad.LIGHT;
                level.addParticle(new EnergyParticleOptions(color, 1.2F), pos.getX() + 0.15 + random.nextDouble() * 0.7, top,
                        pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0, 0.05 + random.nextDouble() * 0.08, 0);
            }
        } else if (random.nextInt(3) == 0) {
            // A faint spark somewhere along the glowing outline.
            double t = random.nextDouble() * 4.0;
            double along = 0.06 + (t - Math.floor(t)) * 0.88;
            double x = t < 1 ? along : t < 2 ? 0.94 : t < 3 ? 1.0 - along : 0.06;
            double z = t < 1 ? 0.06 : t < 2 ? along : t < 3 ? 0.94 : 1.0 - along;
            level.addParticle(new EnergyParticleOptions(WitchesRoad.LIGHT, 0.7F), pos.getX() + x, top + 0.02, pos.getZ() + z,
                    0, 0.01, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.enhanced.witches_road_door.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("block.enhanced.witches_road_door.desc2").withStyle(ChatFormatting.AQUA));
    }
}
