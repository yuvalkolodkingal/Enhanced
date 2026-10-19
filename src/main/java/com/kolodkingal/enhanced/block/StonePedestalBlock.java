package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.config.StonesConfig.Uniqueness;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The stone pedestal (3.6, the plan's 4.6): it holds one Infinity Stone at a site (the Sanctum's Time Stone, Asgard's
 * vault's Reality Stone), floating and turning above it (its renderer; Asgard's red stone is the Aether, a swirling
 * orb). Unbreakable; it never drops its stone. Right-click:
 *
 * <ul> <li>one who may hold the stone ({@link StoneRules#mayHold}) and whom the ledger lets take it ({@link
 * StoneLedger#mayTake}) gets it in his hand (or his inventory): the pedestal is empty for him ({@code player} mode:
 * still full for everyone else) or for everyone ({@code world} mode);</li> <li>one who may not hold it: "Only a god,
 * the Scarlet Witch or the Phoenix can take the Reality Stone." (the Time Stone: "... or a sorcerer.");</li> <li>one
 * who has taken the same stone before ({@code player} mode): "You have already taken the Time Stone from a place like
 * this."; in {@code world} mode, while someone has it: "This stone is already in someone's hands."</li> </ul>
 *
 * A Creative player puts a stone on an empty pedestal by using the stone on it (map makers).
 */
public class StonePedestalBlock extends BaseEntityBlock {
    public static final MapCodec<StonePedestalBlock> CODEC = simpleCodec(StonePedestalBlock::new);
    /** Which stone the pedestal shows ({@code none} when it is empty for everyone). */
    public static final EnumProperty<Stone> STONE = EnumProperty.create("stone", Stone.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** A foot, a shaft and a top plate (the stone floats over the plate, at 1.45). */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 12, 13),
            Block.box(1, 12, 1, 15, 15, 15));

    /** The stone of a pedestal ({@code none}, or one of the six by {@code StoneKind} name). */
    public enum Stone implements StringRepresentable {
        NONE, REALITY, MIND, TIME, POWER, SPACE, SOUL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The stone kind ({@code null} for none). */
        @Nullable
        public StoneKind kind() {
            return this == NONE ? null : StoneKind.valueOf(name());
        }

        public static Stone of(StoneKind kind) {
            return valueOf(kind.name());
        }
    }

    public StonePedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STONE, Stone.NONE).setValue(FACING, Direction.NORTH));
    }

    /** The stone of a pedestal's state, or null. */
    @Nullable
    public static StoneKind kind(BlockState state) {
        return state.hasProperty(STONE) ? state.getValue(STONE).kind() : null;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STONE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StonePedestalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.STONE_PEDESTAL.get(),
                StonePedestalBlockEntity::serverTick);
    }

    /** A Creative player sets a stone on an empty pedestal. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof InfinityStoneItem stone) || state.getValue(STONE) != Stone.NONE
                || !player.isCreative()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(STONE, Stone.of(stone.kind)), Block.UPDATE_ALL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        StoneKind kind = kind(state);
        if (kind == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        take(server, (ServerLevel) level, pos, state, kind);
        return InteractionResult.CONSUME;
    }

    /**
     * The player asks the pedestal for its stone: refused (said why) or given.
     *
     * @return whether he got it
     */
    public static boolean take(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, StoneKind kind) {
        if (!(level.getBlockEntity(pos) instanceof StonePedestalBlockEntity pedestal)) {
            return false;
        }
        if (!StoneRules.mayHold(player, kind)) {
            String key = kind == StoneKind.TIME ? "message.enhanced.stones.pedestal_refuse_time"
                    : "message.enhanced.stones.pedestal_refuse";
            player.displayClientMessage(Component.translatable(key, kind.displayName())
                    .withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        String refusal = pedestal.gone() ? "message.enhanced.stones.one_per_world"
                : StoneLedger.refusal(player, kind);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal, kind.displayName())
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ItemStack stone = new ItemStack(ModItems.stone(kind));
        if (StoneLedger.isSiteStone(kind)) {
            StoneLedger.stamp(stone, player.getUUID());
            StoneLedger.taken(player, kind);
        }
        pedestal.taken(player.getUUID(), StoneLedger.mode(kind) == Uniqueness.WORLD);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        } else if (!player.getInventory().add(stone)) {
            player.drop(stone, false);
        }
        Vec3 at = Vec3.atCenterOf(pos).add(0.0, 0.95, 0.0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.PEDESTAL_TAKE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        kind.voice().use(level, at, InfinityStone.UNDER, 1.0F);
        level.sendParticles(new EnergyParticleOptions(kind.color(), 1.2F), at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.08);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 48.0,
                new StoneFxPayload(StoneFxPayload.PEDESTAL_TAKE, player.getId(), List.of(at), kind.ordinal()));
        player.displayClientMessage(Component.translatable("message.enhanced.stones.taken", kind.displayName())
                .withStyle(s -> s.withColor(kind.color())), true);
        return true;
    }
}
