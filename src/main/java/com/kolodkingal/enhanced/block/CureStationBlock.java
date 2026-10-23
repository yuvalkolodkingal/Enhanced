package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CureConfig;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Hank's Cure Station in the lab's medical bay (3.7, lane L4): a white bench with a glass dispenser. Right-clicked with
 * a glass bottle, it fills it: one Mutant Cure (the bottle used up), once per player per {@code [school_lab]
 * cureStationCooldown} (1200 ticks; the time is kept on the player), and only while {@code [school_lab] cureStation}
 * and {@code [mutant_cure] enabled}. With anything else it says what the Cure does and that it wants a bottle.
 */
public class CureStationBlock extends Block {
    public static final MapCodec<CureStationBlock> CODEC = simpleCodec(CureStationBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** The player's data key of the game time he last got a cure from a station. */
    public static final String LAST = "enhanced_cure_station";

    private static final Map<Direction, VoxelShape> SHAPE = LabShapes.turned(new double[]{0, 0, 0, 16, 10, 16},
            new double[]{3, 10, 3, 13, 16, 13});

    public CureStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    static boolean works() {
        return !EnhancedConfig.SPEC.isLoaded() || SchoolLabConfig.CURE_STATION.get() && CureConfig.ENABLED.get();
    }

    static int cooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? SchoolLabConfig.CURE_STATION_COOLDOWN.get() : 1200;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        fill(level, pos, player, hand);
        return ItemInteractionResult.CONSUME;
    }

    /** Fills the bottle in {@code hand} with a Mutant Cure, if the station may; true if it did. */
    public static boolean fill(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!works()) {
            player.displayClientMessage(Component.translatable("message.enhanced.school_lab.cure_station_off"), true);
            return false;
        }
        CompoundTag data = ModAttachments.PERSISTENT_DATA.get(player);
        long now = level.getGameTime();
        if (data.contains(LAST) && now - data.getLong(LAST) < cooldown() && now >= data.getLong(LAST)) {
            long seconds = (cooldown() - (now - data.getLong(LAST)) + 19) / 20;
            player.displayClientMessage(Component.translatable("message.enhanced.school_lab.cure_station_wait",
                    seconds), true);
            return false;
        }
        data.putLong(LAST, now);
        ItemStack cure = new ItemStack(ModItems.MUTANT_CURE.get());
        ItemStack bottle = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            bottle.shrink(1);
        }
        if (bottle.isEmpty()) {
            player.setItemInHand(hand, cure);
        } else if (!player.getInventory().add(cure)) {
            player.drop(cure, false);
        }
        level.playSound(null, pos, ModSounds.CURE_STATION.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.enhanced.school_lab.cure_station_filled"), true);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.enhanced.school_lab.cure_station"), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
