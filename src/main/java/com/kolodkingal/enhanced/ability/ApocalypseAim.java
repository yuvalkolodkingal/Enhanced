package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What a key of the hero needs of where he is and what he looks at: the Wave needs ground to pull the sand from, the Entomb a
 * creature to close on, the Portal a block to open at. The abilities ask before they act, and so does his animation (a client
 * asks about its own player, so that his arms do not act what the server will refuse): one test for both, so they cannot drift
 * apart. Safe on a client: the config is read only when it has loaded.
 */
public final class ApocalypseAim {
    /** How far below him the ground may be for the Wave. */
    public static final double GROUND_REACH = 6.0;

    /** Whether the Wave has ground in reach: a levitating hero casts it at the ground beneath, but with none there is no sand to pull. */
    public static boolean waveHasGround(Player player) {
        double ground = SandControl.groundAt(player.level(), player.getX(), player.getY() + 0.5, player.getZ());
        return player.getY() - ground <= GROUND_REACH;
    }

    /** The creature the Entomb would close on: the first one he looks at within range (null for none). */
    public static EntityHitResult entombTarget(Player player) {
        int range = EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.ENTOMB_RANGE.get() : 24;
        return Raycasts.entity(player, range, 0.3, e -> e instanceof LivingEntity && e != player && !e.isSpectator() && !(e instanceof ArmorStand), 1.0F);
    }

    /** The block the Portal would open at: the first solid block he looks at within range (a miss for none). */
    public static BlockHitResult portalTarget(Player player) {
        int range = EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.PORTAL_RANGE.get() : 40;
        return Raycasts.block(player, range, 1.0F);
    }

    /** Whether there is a block to open the Portal at. */
    public static boolean portalHasPlace(Player player) {
        return portalTarget(player).getType() != HitResult.Type.MISS;
    }

    private ApocalypseAim() {
    }
}
