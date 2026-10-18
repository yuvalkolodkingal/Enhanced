package com.kolodkingal.enhanced.asgard.build;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The spires of Odin's palace (3.5): the great spire behind the throne room (to y 204) and four lesser ones, each an
 * octagonal tower that narrows in five tiers, a gold band and a gold ledge at the top of each tier, gold ribs at its
 * corners, slits of blue glass on its faces, and a gold needle on top. Hollow (nobody goes in).
 */
public final class PalaceSpires {
    /** One spire: centre, base radius, base height and the needle's tip. */
    public record Spire(int cx, int cz, int radius, int base, int tip) implements CityBuilder {
        private static final double[] TIER_START = {0.0, 0.34, 0.58, 0.76, 0.88};
        private static final double[] TIER_RADIUS = {1.0, 0.78, 0.58, 0.40, 0.24};
        /** The tiers end at this fraction of the height; the needle rises above. */
        private static final double TOWER = 0.94;

        @Override
        public BoundingBox box() {
            return new BoundingBox(cx - radius - 1, base, cz - radius - 1, cx + radius + 1, tip, cz + radius + 1);
        }

        private int tierTop(int tier) {
            double end = tier + 1 < TIER_START.length ? TIER_START[tier + 1] : TOWER;
            return base + (int) Math.round(end * (tip - base)) - 1;
        }

        @Nullable
        @Override
        public BlockState at(int x, int y, int z) {
            if (y < base || y > tip) {
                return null;
            }
            int dx = x - cx;
            int dz = z - cz;
            double h = (double) (y - base) / (tip - base);
            if (h >= TOWER) {
                if (dx == 0 && dz == 0) {
                    return y == tip ? Palette.trim() : Palette.goldPillar();
                }
                return null;
            }
            int tier = 0;
            while (tier + 1 < TIER_START.length && h >= TIER_START[tier + 1]) {
                tier++;
            }
            double rr = radius * TIER_RADIUS[tier];
            double d = Math.max(Math.max(Math.abs(dx), Math.abs(dz)), (Math.abs(dx) + Math.abs(dz)) / 1.4142);
            int top = tierTop(tier);
            if (y == top && d <= rr + 1.0) {
                return d > rr ? Palette.gold() : Palette.polished();
            }
            if (d > rr || d <= rr - 1.0) {
                return null;
            }
            if (y >= top - 1) {
                return Palette.trim();
            }
            boolean corner = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1 && Math.abs(dx) > 1;
            if (corner) {
                return Palette.goldPillar();
            }
            boolean face = Math.abs(dx) <= 0 || Math.abs(dz) <= 0;
            int k = (y - base) % 7;
            if (face && k >= 2 && k <= 5) {
                return Palette.glass();
            }
            return (y - base) % 14 == 0 ? Palette.gold() : Palette.bricks();
        }
    }

    /** The five spires: the great one north of the throne room, two at the back corners, two at the sides. */
    public static final List<Spire> SPIRES = List.of(
            new Spire(0, -152, 12, 82, 204),
            new Spire(-42, -150, 8, 82, 168),
            new Spire(42, -150, 8, 82, 168),
            new Spire(-58, -112, 7, 82, 150),
            new Spire(58, -112, 7, 82, 150));

    private PalaceSpires() {
    }
}
