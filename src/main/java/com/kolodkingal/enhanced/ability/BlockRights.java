package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * One rule book for every power of the mod that changes blocks in someone's name (3.1, spec 5.1): Wanda's, Billy's, the
 * Phoenix's and Magneto's block telekinesis, Build Magic, the Magnetic Pull. Each asks the same questions a hand would:
 * the arena of a boss fight, spawn protection, the world border, Adventure mode, and claim mods (which listen to the
 * block events). Tommy's acceleration and Loki's ice keep their own calls.
 *
 * <p>{@code player} is whoever acts: the holder of a lifted block, the caster of a spell (a fake player of his profile
 * when Build Magic works for him while he is away).
 */
public final class BlockRights {

    /**
     * What the game itself says, with no event: the player may build at all (not Adventure or Spectator), may touch this
     * cell (spawn protection, the world border) and is not barred from it in his game mode.
     */
    public static boolean vanillaRights(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return player.mayBuild() && level.mayInteract(player, pos)
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer());
    }

    /**
     * Asks everyone who listens (claim mods, the arena of a boss fight) whether the player may break {@code old} at
     * {@code pos}: posts one {@link BlockEvent.BreakEvent}.
     *
     * @return true if nobody cancelled it
     */
    public static boolean postBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState old) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, old, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /**
     * Asks everyone who listens whether the player may place a block at {@code pos}: posts one {@link
     * BlockEvent.EntityPlaceEvent} for the cell as it is now, as Loki's ice does.
     *
     * @return true if nobody cancelled it
     */
    public static boolean postPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return !EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
    }

    /**
     * Whether the player may break the block at {@code pos} (it is {@code old}): no boss fight protects the cell, the game
     * lets him, and no listener cancels the break.
     */
    public static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState old) {
        if (BossFights.protects(level, pos)) {
            return false; // the arena of a boss fight stays as it is
        }
        return vanillaRights(player, level, pos) && postBreak(player, level, pos, old);
    }

    /**
     * Whether the player may place a block at {@code pos}: no boss fight protects the cell, the game lets him, and no
     * listener cancels the placing.
     */
    public static boolean mayPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (BossFights.protects(level, pos)) {
            return false;
        }
        return vanillaRights(player, level, pos) && postPlace(player, level, pos);
    }

    /**
     * The area rule of the two mass edits of 3.1 (Build Magic and the Magnetic Pull), asked before any event: true if the
     * cell is refused because a boss fight protects it, it lies in the arena of any running fight ({@code
     * build_magic.allArenasProtected}, Thanos's unprotected arena included), the clients hide the ground there (under
     * Ultron's island in the sky), or it lies in a Hex of someone else who has not allowed the player ({@code
     * build_magic.hexIsAClaim}).
     */
    public static boolean massEditRefuses(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (BossFights.protects(level, pos)) {
            return true;
        }
        if (BuildConfig.ALL_ARENAS_PROTECTED.get() && BossFights.inAnyArena(level, pos)) {
            return true;
        }
        if (BossFights.hidesGround(level, pos)) {
            return true;
        }
        return BuildConfig.HEX_IS_A_CLAIM.get() && inAStrangersHex(player, level, pos);
    }

    /** Whether the cell lies in a Hex whose owner is not the player and who has not allowed him in. */
    private static boolean inAStrangersHex(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Vec3 centre = Vec3.atCenterOf(pos);
        for (Hex hex : HexManager.data(level).hexes()) {
            if (hex.contains(centre) && !hex.owner().equals(player.getUUID()) && !hex.allowed().contains(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private BlockRights() {
    }
}
