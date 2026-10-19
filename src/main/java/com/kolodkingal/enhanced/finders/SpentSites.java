package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The stone sites that can no longer give their stone to anyone (3.7, lane L5; SavedData {@code stone_sites_spent} per
 * level: kind to the set of start chunks): the Stone Locator skips them. Three hooks mark a site spent: the Avengers
 * Tower's cradle emptied ({@code TesseractCradleBlock.take}), the End tunnel's vault chest rolled ({@code
 * SiteStoneLootFunction}), a Hydra outpost's vault chest first opened (the loot function {@code enhanced:mark_spent},
 * {@link MarkSpentFunction}). The site is the structure whose piece holds {@code where}. Stubs of the 3.7 skeleton
 * until lane L5 fills them: nothing is recorded.
 */
public final class SpentSites {

    /** The site of {@code kind} that holds {@code where} has given its stone away for good. */
    public static void mark(Level level, StoneKind kind, BlockPos where) {
    }

    /** Whether the site of {@code kind} whose structure starts in the chunk {@code startChunk} is spent. */
    public static boolean isSpent(Level level, StoneKind kind, long startChunk) {
        return false;
    }

    private SpentSites() {
    }
}
