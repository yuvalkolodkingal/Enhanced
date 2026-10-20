package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The fixed points of the Shrine of the Cosmic Entities (3.8, PLAN 10.2) in its template {@code eternity_shrine/shrine}
 * (47 x 34 x 47), template-relative and unturned (x east, z south): the round plaza (radius 22) on layer {@link
 * #FLOOR}, one stands on layer 3; Eternity's altar in the middle with Eternity's statue north of it, the stair and the
 * lectern south, and the eight cosmic entities round the ring (radius 18) facing the middle, Master Order and Lord
 * Chaos across from each other. The 3.8 skeleton's placeholder template is the plaza disc with these points' blocks;
 * lane L8 builds the shrine (its generator {@code SCRATCH/v38/art/l8/shrine.py}) and moves a point only with a note and
 * the test.
 */
public final class ShrinePlan {
    public static final ResourceLocation TEMPLATE = Enhanced.id("eternity_shrine/shrine");
    public static final Vec3i SIZE = new Vec3i(47, 34, 47);
    /** The plaza's layer, at ground level (the structure's {@code floor}); one stands on the layer above. */
    public static final int FLOOR = 2;
    /** The plaza's middle. */
    public static final BlockPos CENTER = new BlockPos(23, 3, 23);
    /** Eternity's altar. */
    public static final BlockPos ALTAR = new BlockPos(23, 3, 23);
    /** The foot of Eternity's seated statue, behind the altar (north). */
    public static final BlockPos ETERNITY_STATUE = new BlockPos(23, 3, 14);
    /** The lectern with "The Cosmic Entities", by the stair. */
    public static final BlockPos LECTERN = new BlockPos(26, 3, 31);
    /** The foot of the stair up to the dais (south). */
    public static final BlockPos STAIRS = new BlockPos(23, 3, 30);

    /**
     * A statue of the ring: its id (its sign's {@code sign.enhanced.shrine.<id>}) and its plinth's middle.
     *
     * @param id    {@code infinity}, {@code death}...
     * @param plinth the middle of its plinth, on the plaza
     */
    public record Statue(String id, BlockPos plinth) {
    }

    /** The eight statues, round the ring from north-east, clockwise. */
    public static final List<Statue> STATUES = List.of(
            new Statue("infinity", new BlockPos(30, 3, 6)),
            new Statue("death", new BlockPos(40, 3, 16)),
            new Statue("master_order", new BlockPos(40, 3, 30)),
            new Statue("galactus", new BlockPos(30, 3, 40)),
            new Statue("living_tribunal", new BlockPos(16, 3, 40)),
            new Statue("arishem", new BlockPos(6, 3, 30)),
            new Statue("lord_chaos", new BlockPos(6, 3, 16)),
            new Statue("oblivion", new BlockPos(16, 3, 6)));

    private ShrinePlan() {
    }
}
