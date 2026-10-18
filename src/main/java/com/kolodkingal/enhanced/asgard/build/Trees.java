package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The trees of Asgard (3.5): golden-leaved trees and blossom trees (our own round shapes: a pale trunk, a crown of
 * leaves that never fall) at fixed, hashed spots of the gardens, the houses' and east districts and the wild land; never
 * on a building, a street, a channel or a terrace's edge.
 */
public final class Trees implements CityBuilder {
    public static final Trees INSTANCE = new Trees();

    /** One tree: its foot (on the ground at {@code ground}), trunk height, crown radius and kind (0 gold, 1 blossom, 2 green). */
    public record Tree(int x, int ground, int z, int trunk, int crown, int kind) {
        BoundingBox box() {
            return new BoundingBox(x - crown, ground + 1, z - crown, x + crown, ground + trunk + crown + 1, z + crown);
        }

        @Nullable
        BlockState at(int px, int py, int pz) {
            int dx = px - x;
            int dz = pz - z;
            int dy = py - ground;
            if (dx == 0 && dz == 0 && dy >= 1 && dy <= trunk) {
                return Palette.log(kind == 1 ? Blocks.CHERRY_LOG : Blocks.STRIPPED_BIRCH_LOG);
            }
            double cy = trunk + 0.5;
            double d = Math.sqrt(dx * dx + (dy - cy) * (dy - cy) * 1.6 + dz * dz);
            if (dy > trunk - crown / 2 && d <= crown + 0.3 && (dx != 0 || dz != 0 || dy > trunk)) {
                if (d > crown - 0.7 && Math.floorMod(AsgardTerrain.hash(px * 31 + py, pz), 5) == 0) {
                    return null;
                }
                return Palette.leaves(kind == 0 ? ModBlocks.ASGARD_GOLDEN_LEAVES.get() : kind == 1 ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES);
            }
            return null;
        }
    }

    public static final List<Tree> TREES;
    private static final Map<Long, List<Tree>> BY_CHUNK = new HashMap<>();
    private static final int CELL = 9;

    static {
        List<Tree> trees = new ArrayList<>();
        List<BoundingBox> keepOut = new ArrayList<>();
        for (CityPlan.Piece p : CityPlan.pieces()) {
            keepOut.add(p.box().inflatedBy(3));
        }
        for (int cx = -30; cx <= 30; cx++) {
            for (int cz = -30; cz <= 30; cz++) {
                long h = AsgardTerrain.hash(cx * 7 + 3, cz * 13 + 5);
                if (h % 3 != 0) {
                    continue;
                }
                int x = cx * CELL + (int) ((h >> 8) % (CELL - 2)) + 1;
                int z = cz * CELL + (int) ((h >> 16) % (CELL - 2)) + 1;
                AsgardTerrain.Column c = AsgardTerrain.column(x, z);
                if (c.kind() != AsgardTerrain.Kind.LAND || c.top() < CityPlan.SEA_LEVEL + 2) {
                    continue;
                }
                if (c.pad() != null && c.pad().surface() != CityPlan.Surface.GRASS) {
                    continue;
                }
                if (GoldenRoad.onStreet(x, z) || Math.hypot(x, z) > AsgardTerrain.edgeRadius(x, z) - AsgardTerrain.CLIFF_WIDTH - 3) {
                    continue;
                }
                boolean flat = true;
                for (int dx = -2; dx <= 2 && flat; dx++) {
                    for (int dz = -2; dz <= 2 && flat; dz++) {
                        AsgardTerrain.Column n = AsgardTerrain.column(x + dx, z + dz);
                        flat = n.kind() == AsgardTerrain.Kind.LAND && Math.abs(n.top() - c.top()) <= 1 && n.pad() == c.pad();
                    }
                }
                if (!flat || Math.abs(x) <= CityPlan.ROAD_PAD_HALF_WIDTH + 3 && z > -20 && z < 440) {
                    continue;
                }
                int kind = c.pad() != null && c.pad().name().equals("gardens") ? (int) ((h >> 24) % 2) : (int) ((h >> 24) % 3);
                Tree tree = new Tree(x, c.top(), z, 4 + (int) ((h >> 28) % 3), 2 + (int) ((h >> 32) % 2), kind);
                BoundingBox box = tree.box();
                boolean clear = true;
                for (BoundingBox k : keepOut) {
                    if (k.intersects(box)) {
                        clear = false;
                        break;
                    }
                }
                for (Waterways.Channel ch : Waterways.CHANNELS) {
                    BoundingBox cb = new BoundingBox(Math.min(ch.x0(), ch.x1()) - 3, 0, Math.min(ch.z0(), ch.z1()) - 3,
                            Math.max(ch.x0(), ch.x1()) + 3, 200, Math.max(ch.z0(), ch.z1()) + 3);
                    clear &= !cb.intersects(box);
                }
                for (CityPlan.Bench bench : CityPlan.benches()) {
                    clear &= !box.inflatedBy(1).isInside(bench.pos());
                }
                if (clear) {
                    trees.add(tree);
                }
            }
        }
        TREES = List.copyOf(trees);
        for (Tree t : TREES) {
            BoundingBox b = t.box();
            for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) {
                for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) {
                    BY_CHUNK.computeIfAbsent(ChunkPos.asLong(x, z), k -> new ArrayList<>()).add(t);
                }
            }
        }
    }

    @Override
    public BoundingBox box() {
        return new BoundingBox(-290, 60, -290, 290, 100, 290);
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        for (Tree t : BY_CHUNK.getOrDefault(ChunkPos.asLong(x >> 4, z >> 4), List.of())) {
            BlockState s = t.at(x, y, z);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    @Override
    public void place(CityCanvas canvas) {
        for (Tree t : BY_CHUNK.getOrDefault(canvas.chunk().getPos().toLong(), List.of())) {
            BoundingBox b = t.box();
            for (int x = Math.max(b.minX(), canvas.minX()); x <= Math.min(b.maxX(), canvas.minX() + 15); x++) {
                for (int z = Math.max(b.minZ(), canvas.minZ()); z <= Math.min(b.maxZ(), canvas.minZ() + 15); z++) {
                    for (int y = b.minY(); y <= b.maxY(); y++) {
                        BlockState s = t.at(x, y, z);
                        if (s != null && canvas.get(x, y, z).isAir()) {
                            canvas.set(x, y, z, s);
                        }
                    }
                }
            }
        }
    }

    private Trees() {
    }
}
