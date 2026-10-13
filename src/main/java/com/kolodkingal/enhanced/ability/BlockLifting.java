package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Telekinesis tearing a block out of the ground (Wanda, Billy, the Phoenix), and the rule for setting one down where it
 * lands. Wanda and the Phoenix reach {@code scarlet_witch.blockLiftRange} (24 blocks) and lift up to {@code
 * scarlet_witch.blockLiftMaxHardness} (5); Billy, younger, has limits of his own ({@code billy.*}: 16 blocks, hardness 3,
 * and a moment between two blocks).
 */
public final class BlockLifting {

    /** @return true if Wanda lifted a block or was told why not */
    public static boolean tryLift(ServerPlayer player) {
        return tryLift(player, Telekinesis.Mode.WANDA);
    }

    /**
     * Tears out the block the player looks at, to be held in {@code mode} (the hero's own telekinesis), within the hero's
     * reach and hardness, if the game, the arenas and the claims let him break it. Billy waits {@code
     * billy.blockLiftCooldown} between two blocks (a press while his magic recharges is used up by the message).
     *
     * @return true if a block was lifted, or the player was told why not; false if no block is in reach
     */
    public static boolean tryLift(ServerPlayer player, Telekinesis.Mode mode) {
        boolean billy = mode == Telekinesis.Mode.BILLY;
        double range = billy ? EnhancedConfig.BILLY_BLOCK_LIFT_RANGE.get() : EnhancedConfig.BLOCK_LIFT_RANGE.get();
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (billy && !BillyAbilities.blockLiftReady(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.recharging"), true);
            return true;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!canLift(level, pos, state, mode)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.too_strong"), true);
            return true;
        }
        if (!BlockRights.mayBreak(player, level, pos, state)) {
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.protected"), true);
            return true;
        }

        BlockState lifted = state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
        // Plays the break sound and particles, leaves water behind for waterlogged blocks, drops nothing.
        level.destroyBlock(pos, false, player);
        TelekineticBlockEntity entity = new TelekineticBlockEntity(level, pos, lifted, player);
        level.addFreshEntity(entity);
        Telekinesis.hold(player, entity, mode);
        if (billy) {
            BillyAbilities.markBlockLift(player);
        }
        return true;
    }

    /** Whether Wanda's telekinesis could tear this block out (her hardness, {@code scarlet_witch.blockLiftMaxHardness}). */
    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state) {
        return canLift(level, pos, state, Telekinesis.Mode.WANDA);
    }

    /**
     * Whether the telekinesis of {@code mode} could tear this block out: a whole block of the world (no fluid, no block
     * entity, not {@code telekinesis_immovable}, not half of a door or a bed, something solid to hold) no harder than the
     * hero's limit (Billy {@code billy.blockLiftMaxHardness}, Wanda and the Phoenix {@code
     * scarlet_witch.blockLiftMaxHardness}).
     */
    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state, Telekinesis.Mode mode) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity() || state.is(ModTags.TELEKINESIS_IMMOVABLE)) {
            return false;
        }
        // Two-block-tall things (doors, tall flowers) and beds would be torn in half.
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)) {
            return false;
        }
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        double maxHardness = mode == Telekinesis.Mode.BILLY
                ? EnhancedConfig.BILLY_BLOCK_LIFT_MAX_HARDNESS.get() : EnhancedConfig.BLOCK_LIFT_MAX_HARDNESS.get();
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= maxHardness;
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
