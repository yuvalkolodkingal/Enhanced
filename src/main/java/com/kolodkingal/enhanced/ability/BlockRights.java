package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.asgard.AsgardRules;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.strange.MirrorDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * One rule book for every power of the mod that changes blocks in someone's name (3.1, spec 5.1): Wanda's, Billy's, the
 * Phoenix's and Magneto's block telekinesis, Build Magic, the Magnetic Pull. Each asks the same questions a hand would:
 * the arena of a boss fight, spawn protection, the world border, Adventure mode, and claim mods (which listen to the
 * block events). Tommy's acceleration and Loki's ice keep their own calls.
 *
 * <p>{@code player} is whoever acts: the holder of a lifted block, the caster of a spell (a fake player of his profile
 * when Build Magic works for him while he is away).
 *
 * <p>3.3: no block of a Hex is changed by a power from outside its wall, by anyone Wanda has not let in ({@link
 * #behindTheWall}): every question here asks it first.
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
        return !Platform.get().hooks().blockBreakEventCancelled(level, pos, old, player);
    }

    /**
     * Asks everyone who listens whether the player may place a block at {@code pos}: posts one {@link
     * BlockEvent.EntityPlaceEvent} for the cell as it is now, as Loki's ice does.
     *
     * @return true if nobody cancelled it
     */
    public static boolean postPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return !Platform.get().hooks().blockPlaceCancelled(player, level, pos, Direction.UP);
    }

    /**
     * Whether the player may break the block at {@code pos} (it is {@code old}): no boss fight protects the cell, the game
     * lets him, and no listener cancels the break.
     */
    public static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState old) {
        // (3.5: the protected city of Asgard is kept as a boss arena is; 3.6: so is what a Mirror Dimension holds.)
        if (BossFights.protects(level, pos) || AsgardRules.protectsCity(player, level, pos)
                || behindTheWall(player, level, pos) || MirrorDimension.protects(player, level, pos)) {
            return false; // the arena of a boss fight stays as it is; the Hex keeps its own
        }
        return vanillaRights(player, level, pos) && postBreak(player, level, pos, old);
    }

    /**
     * 3.6: whether a power's blast may break the block at {@code pos} in the player's name: the Power Stone's craters
     * ({@code [power_stone] terrainDamage}) and the blocks the Hulk breaks. It needs the gamerule {@code mobGriefing}
     * on and everything {@link #mayBreak} asks (a boss arena, the city of Asgard, a Hex, a Mirror Dimension, spawn
     * protection, claims). The caller's own config (on or off) comes first.
     */
    public static boolean mayBlast(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && mayBreak(player, level, pos,
                level.getBlockState(pos));
    }

    /**
     * Whether the player may place a block at {@code pos}: no boss fight protects the cell, the game lets him, and no
     * listener cancels the placing.
     */
    public static boolean mayPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (BossFights.protects(level, pos) || AsgardRules.protectsCity(player, level, pos)
                || behindTheWall(player, level, pos) || MirrorDimension.protects(player, level, pos)) {
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
        if (BossFights.protects(level, pos) || AsgardRules.protectsCity(player, level, pos)
                || MirrorDimension.protects(player, level, pos)) {
            return true;
        }
        if (BuildConfig.ALL_ARENAS_PROTECTED.get() && BossFights.inAnyArena(level, pos)) {
            return true;
        }
        if (BossFights.hidesGround(level, pos)) {
            return true;
        }
        return inAStrangersHex(player, level, pos, BuildConfig.HEX_IS_A_CLAIM.get());
    }

    /**
     * 3.3: whether the cell lies in a Hex that keeps the player out while he is outside its wall (a power from outside changes
     * no block of it: Build Magic, block telekinesis, the Magnetic Pull, Tommy's acceleration, Loki's ice...). A fake player of
     * an absent caster stands nowhere near: it is outside.
     */
    public static boolean behindTheWall(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return inAStrangersHex(player, level, pos, false);
    }

    /**
     * Whether the cell lies in a Hex whose owner is not the player and who has not allowed him in, and he is outside it (or,
     * with {@code evenInside}, wherever he is: {@code build_magic.hexIsAClaim}).
     */
    private static boolean inAStrangersHex(ServerPlayer player, ServerLevel level, BlockPos pos, boolean evenInside) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 actor = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Hex hex : HexManager.data(level).hexes()) {
            if (hex.contains(centre) && !hex.owner().equals(player.getUUID()) && !hex.allowed().contains(player.getUUID())
                    && (evenInside || !hex.contains(actor) || player.level() != level)) {
                return true;
            }
        }
        return false;
    }

    private BlockRights() {
    }
}
