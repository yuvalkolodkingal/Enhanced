package com.kolodkingal.enhanced.asgard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The rules of Asgard (3.5): the protected city, the void catch, the soft edge, no spawn point, keeping the inventory. The
 * 3.5 skeleton writes the one question every power that changes a block asks ({@code BlockRights}); lane L1 owns this
 * class and fills it (the protected boxes of {@code CityPlan}, and the events).
 */
public final class AsgardRules {

    /**
     * Whether the block at {@code pos} belongs to the protected city of Asgard ({@code asgard.protectCity}) and may not be
     * changed by {@code editor} (null: no player, an explosion or a creature). Operators in Creative may edit.
     */
    public static boolean protectsCity(@Nullable Player editor, ServerLevel level, BlockPos pos) {
        return false;
    }

    private AsgardRules() {
    }
}
