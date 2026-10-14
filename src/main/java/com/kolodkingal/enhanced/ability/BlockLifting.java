package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Telekinesis tearing a block out of the ground (Wanda, Billy, the Phoenix; Magneto for metal), and the rule for setting
 * one down where it lands. Wanda and the Phoenix reach {@code scarlet_witch.blockLiftRange} (24 blocks) and lift up to
 * {@code scarlet_witch.blockLiftMaxHardness} (5); Billy, younger, has limits of his own ({@code billy.*}: 16 blocks,
 * hardness 3, and a moment between two blocks); Magneto tears out only metal ({@code magneto.*}: 24 blocks, hardness 50),
 * aims past grass and carpets at a rail or a pressure plate ({@link Raycasts#metalBlock}), and takes a metal door out
 * whole: the lifted block carries its lower half and draws the upper one ({@link #isDoor}).
 */
public final class BlockLifting {
    private static final String PROTECTED = "message.enhanced.telekinesis.protected";

    /** @return true if Wanda lifted a block or was told why not */
    public static boolean tryLift(ServerPlayer player) {
        return tryLift(player, Telekinesis.Mode.WANDA);
    }

    /**
     * Tears out the block the player looks at, to be held in {@code mode} (the hero's own telekinesis), within the hero's
     * reach and hardness, if the game and the claims let him break it. In a boss's protected arena the block is only
     * borrowed, if the arena lends it ({@link BossFights#lendRefusal}): the hole mends and the block never drops anything
     * ({@code ArenaLoans}); the ground hidden under Ultron's island in the sky gives nothing. Billy waits {@code
     * billy.blockLiftCooldown} between two blocks (a press while his magic recharges is used up by the message). Magneto
     * takes only a metal block; at any other he is told there is no metal, and at one that is fixed in place (a bell, a
     * hopper) whether his Magnetic Pull would take it. A metal door comes out whole, whichever half he aims at: both its
     * cells must be his to break, and an arena never lends one (two cells cannot be one loan).
     *
     * @return true if a block was lifted, or the player was told why not; false if no block is in reach
     */
    public static boolean tryLift(ServerPlayer player, Telekinesis.Mode mode) {
        boolean billy = mode == Telekinesis.Mode.BILLY;
        boolean magneto = mode == Telekinesis.Mode.MAGNETO;
        BlockHitResult hit = magneto
                ? Raycasts.metalBlock(player, EnhancedConfig.MAGNETO_BLOCK_LIFT_RANGE.get(), 1.0F)
                : Raycasts.block(player, billy ? EnhancedConfig.BILLY_BLOCK_LIFT_RANGE.get() : EnhancedConfig.BLOCK_LIFT_RANGE.get(), 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (billy && !BillyAbilities.blockLiftReady(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.recharging"), true);
            return true;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = magneto ? doorBottom(level, hit.getBlockPos()) : hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        // A door's upper half leaves with it: what is asked of the cell is asked of both.
        BlockPos top = isDoor(state) ? pos.above() : null;
        String refused = magneto ? metalRefusal(level, pos, state) : null;
        if (refused == null && !canLift(level, pos, state, mode)) {
            refused = "message.enhanced.telekinesis.too_strong";
        }
        if (refused == null && (BossFights.hidesGround(level, pos) || (top != null && BossFights.hidesGround(level, top)))) {
            // The ground the clients hide under Ultron's island: nothing is taken from it, whatever the arena's config says.
            refused = PROTECTED;
        }
        if (refused == null && BossFights.protects(level, pos)) {
            // A boss's protected arena lends its block (D-T 5.2): its own rules first, then the rights a hand would need, the
            // break event heard by the claims (the arena lets this one cell through). A door is never lent.
            refused = BossFights.lendRefusal(player, pos);
            if (refused == null && !(BlockRights.vanillaRights(player, level, pos)
                    && BossFights.lending(pos, () -> BlockRights.postBreak(player, level, pos, state)))) {
                refused = PROTECTED;
            }
            if (refused == null) {
                TelekineticBlockEntity borrowed = BossFights.lend(player, pos, state);
                if (borrowed != null) {
                    take(player, borrowed, state, mode);
                    return true;
                }
                refused = PROTECTED;
            }
        }
        if (refused == null && !(BlockRights.mayBreak(player, level, pos, state)
                && (top == null || BlockRights.mayBreak(player, level, top, level.getBlockState(top))))) {
            refused = PROTECTED;
        }
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused), true);
            return true;
        }

        // Plays the break sound and particles, leaves water behind for waterlogged blocks, drops nothing. A door's upper half
        // follows by vanilla's shape rule, and its loot is only the lower half's: nothing drops.
        level.destroyBlock(pos, false, player);
        TelekineticBlockEntity entity = new TelekineticBlockEntity(level, pos, heldState(state), player);
        level.addFreshEntity(entity);
        take(player, entity, state, mode);
        return true;
    }

    /** The lifted block is held: piglins whose gold it was are angered, and Billy's magic needs its moment. */
    private static void take(ServerPlayer player, TelekineticBlockEntity entity, BlockState state, Telekinesis.Mode mode) {
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) {
            PiglinAi.angerNearbyPiglins(player, false); // as breaking their gold by hand does
        }
        Telekinesis.hold(player, entity, mode);
        if (mode == Telekinesis.Mode.BILLY) {
            BillyAbilities.markBlockLift(player);
        }
    }

    /**
     * The state a block of the world is held as: never waterlogged (the water stays behind), a piston retracted (its head
     * breaks away when the base goes, and drops nothing; it is set down retracted), a lantern as it hung, a door by its
     * lower half.
     */
    public static BlockState heldState(BlockState state) {
        BlockState held = state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
        return held.getBlock() instanceof PistonBaseBlock ? held.setValue(PistonBaseBlock.EXTENDED, false) : held;
    }

    /**
     * Whether this is the lower half of a door: held, it stands for the whole door, whose upper half is drawn above it and
     * set down with it (D-M 3.4).
     */
    public static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    /** The upper half of a held door ({@link #isDoor}). */
    public static BlockState doorTop(BlockState door) {
        return door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
    }

    /**
     * The state a lifted block takes where it lands at {@code pos}: a hanging lantern that has nothing to hang from there
     * stands on the ground if it can (D-M 3.4); anything else as it was held.
     */
    public static BlockState landingState(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LanternBlock && state.getValue(LanternBlock.HANGING) && !state.canSurvive(level, pos)) {
            BlockState standing = state.setValue(LanternBlock.HANGING, false);
            if (standing.canSurvive(level, pos)) {
                return standing;
            }
        }
        return state;
    }

    /** Whether Wanda's telekinesis could tear this block out (her hardness, {@code scarlet_witch.blockLiftMaxHardness}). */
    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state) {
        return canLift(level, pos, state, Telekinesis.Mode.WANDA);
    }

    /**
     * Whether the telekinesis of {@code mode} could tear this block out: a whole block of the world (no fluid, no block
     * entity, not {@code telekinesis_immovable}, not half of a door or a bed, something solid to hold) no harder than the
     * hero's limit (Billy {@code billy.blockLiftMaxHardness}, Wanda and the Phoenix {@code
     * scarlet_witch.blockLiftMaxHardness}). Magneto's is a metal block ({@link Metal#isMetalBlock}) up to {@code
     * magneto.blockLiftMaxHardness}, its outline is enough to hold (a rail, a pressure plate), and a door is his whole: its
     * lower half at {@code pos}, with its upper half above.
     */
    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state, Telekinesis.Mode mode) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity() || state.is(ModTags.TELEKINESIS_IMMOVABLE)) {
            return false;
        }
        // Two-block-tall things (doors, tall flowers) and beds would be torn in half; Magneto takes a metal door out whole.
        if ((state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART))
                && !(mode == Telekinesis.Mode.MAGNETO && isWholeDoor(level, pos, state))) {
            return false;
        }
        if (mode == Telekinesis.Mode.MAGNETO) {
            // Only metal, and a block he can hold by its outline is enough (a rail, a pressure plate).
            if (!Metal.isMetalBlock(state) || state.getShape(level, pos).isEmpty()) {
                return false;
            }
        } else if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        double maxHardness = switch (mode) {
            case BILLY -> EnhancedConfig.BILLY_BLOCK_LIFT_MAX_HARDNESS.get();
            case MAGNETO -> EnhancedConfig.MAGNETO_BLOCK_LIFT_MAX_HARDNESS.get();
            case WANDA, JEAN -> EnhancedConfig.BLOCK_LIFT_MAX_HARDNESS.get();
        };
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= maxHardness;
    }

    /**
     * The cell a door Magneto aims at is lifted by: its lower half's, when {@code pos} is the upper half of a whole door
     * (the lifted block carries the door by its lower half); otherwise {@code pos} itself.
     */
    private static BlockPos doorBottom(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            if (isDoor(below) && below.is(state.getBlock())) {
                return pos.below();
            }
        }
        return pos;
    }

    /** Whether {@code state} at {@code pos} is the lower half of a door whose upper half stands above it. */
    private static boolean isWholeDoor(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState above = level.getBlockState(pos.above());
        return isDoor(state) && above.is(state.getBlock()) && above.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    /**
     * Why Magneto's R leaves this block where it is, before its hardness is asked (D-M 3.3): no metal in it ({@code
     * telekinesis.no_metal}), or a metal block fixed in place, which the lifted block could not carry whole (its items, its
     * block entity): it holds items ({@code fixed_metal_full}), his Magnetic Pull would take it whole ({@code
     * fixed_metal}), or neither ({@code fixed_metal_off}). Null for a metal block without a block entity.
     */
    @Nullable
    private static String metalRefusal(ServerLevel level, BlockPos pos, BlockState state) {
        if (!Metal.isMetalBlock(state)) {
            return "message.enhanced.telekinesis.no_metal";
        }
        if (!state.hasBlockEntity()) {
            return null;
        }
        if (MagnetPull.holdsItems(level, pos)) {
            return "message.enhanced.telekinesis.fixed_metal_full";
        }
        return MagnetPull.wouldTakeBuilt(level, pos, state)
                ? "message.enhanced.telekinesis.fixed_metal" : "message.enhanced.telekinesis.fixed_metal_off";
    }

    /**
     * Whether a lifted block that was set down (not thrown) may become a block again at {@code pos}, where it landed
     * (D-T 2.3), asked in this order:
     * <ol>
     *     <li>room: no creature, player or other entity that blocks building stands in the block's shape, so set-down
     *     blocks never encase anyone;</li>
     *     <li>the arenas: no boss fight protects the cell (an arena takes back only the blocks it lent);</li>
     *     <li>the rights of {@code thrower}, who let go of it: the game lets him place a block there (spawn protection, the
     *     world border, Adventure) and no claim cancels the placing;</li>
     *     <li>with nobody to ask ({@code thrower} null: he logged out holding it, or it was saved in a chunk and he is not
     *     online) nobody's rights are assumed: it never becomes a block.</li>
     * </ol>
     * Refused, it drops as an item where it landed (its loot, or the block itself when its loot is empty, R44).
     */
    public static boolean mayPlace(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer thrower) {
        if (!level.isUnobstructed(state, pos, CollisionContext.empty())) {
            return false;
        }
        // BlockRights.mayPlace asks the arenas first, then the game and the claims.
        return thrower != null && BlockRights.mayPlace(thrower, level, pos);
    }

    private BlockLifting() {
    }
}
