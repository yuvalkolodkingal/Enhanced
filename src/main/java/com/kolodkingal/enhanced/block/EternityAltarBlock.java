package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Eternity's altar (3.8, PLAN 10.3): at the centre of the Shrine of the Cosmic Entities and, as the wishing stone, in
 * Eternity's realm. An obsidian-and-purpur pedestal with a starry top.
 *
 * <ul>
 * <li>In the shrine (any world but the realm): right-clicked by one who holds a key of the Bifrost ({@code
 * #enhanced:bifrost_keys}: Mjolnir, Stormbreaker, Hofund, his own Gungnir) in either hand, the key's copy floats over
 * it while it charges and its holder is taken to Eternity ({@link EternityAltarBlockEntity}). Without a key: "The altar
 * waits for a key of the Bifrost". Refused to a boss fight's participant and inside an arena; one traveller at a
 * time.</li>
 * <li>In the realm: it opens the wish screen again.</li> </ul>
 */
public class EternityAltarBlock extends BaseEntityBlock {
    public static final MapCodec<EternityAltarBlock> CODEC = simpleCodec(EternityAltarBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /**
     * Placed by a template (a generated shrine) or the realm: unbreakable in Survival (Creative breaks it). A placed
     * one (the creative tab's, for builders) breaks as any stone block.
     */
    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");
    /** The pedestal: a wide foot, a narrower column, a wide top (the model's boxes). */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 11, 13),
            Block.box(0, 11, 0, 16, 15, 16));

    public EternityAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(GENERATED, false));
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
        builder.add(FACING, GENERATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A generated one does not break in Survival. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(GENERATED) && !player.isCreative() ? 0.0F : super.getDestroyProgress(state, player, level,
                pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EternityAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ETERNITY_ALTAR.get(),
                EternityAltarBlockEntity::serverTick);
    }

    // ---- the key ----

    /** Whether the stack opens the altar for the player: a key of the Bifrost (Gungnir only his own). */
    public static boolean isKey(Player player, ItemStack stack) {
        if (!stack.is(ModTags.BIFROST_KEYS)) {
            return false;
        }
        return !stack.is(ModItems.GUNGNIR.get()) || GungnirItem.isOwnGungnir(player, stack);
    }

    /** The hand that holds a key (the main hand first), or null. */
    @Nullable
    public static InteractionHand keyHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (isKey(player, player.getItemInHand(hand))) {
                return hand;
            }
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // A key in either hand is the altar's business (the key's own use, the Bifrost screen, does not open).
        if (keyHand(player) == null && !(level.dimension() == EternityRealm.LEVEL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer server) {
            use(server, (ServerLevel) level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            use(server, (ServerLevel) level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The server's side of a click: the wishing stone's screen in the realm, else the way in. */
    public static void use(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.isSpectator()) {
            return;
        }
        if (level.dimension() == EternityRealm.LEVEL) {
            EternityRealm.openScreen(player);
            return;
        }
        InteractionHand hand = keyHand(player);
        if (hand == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.eternity.needs_key")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        if (BossFights.isParticipant(player) || BossFights.inAnyArena(level, pos)) {
            player.displayClientMessage(Component.translatable("message.enhanced.boss.held"), true);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof EternityAltarBlockEntity altar)) {
            return;
        }
        if (!altar.begin(player, player.getItemInHand(hand))) {
            player.displayClientMessage(Component.translatable("message.enhanced.eternity.busy")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }
}
