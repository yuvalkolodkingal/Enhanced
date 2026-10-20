package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Red Hulk through the Hulk's renderer (3.8, PLAN 4.9): {@code HulkPlayerRenderer.drawsAsHulk} asks {@link
 * #drawsAsHulk} and its texture choice asks {@link #texture} first. The 3.8 skeleton's answers leave the Hulk's
 * renderer as it was (nobody else is drawn as a Hulk); lane L2 fills them (Red Hulk, not Ross, in his skin) and keeps
 * the signatures.
 */
public final class RedHulkLooks {
    /** Whether this player is drawn by the Hulk's renderer as Red Hulk. Skeleton: never. */
    public static boolean drawsAsHulk(PowerData data) {
        return false;
    }

    /** Red Hulk's skin for the Hulk's renderer, or null (the Hulk's own choice). Skeleton: null. */
    @Nullable
    public static ResourceLocation texture(Player player) {
        return null;
    }

    private RedHulkLooks() {
    }
}
