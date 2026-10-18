package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The crater of a {@link CraterStructure}. Everything is worked out from the column's place and the crater's seed, so
 * every chunk builds its part of it the same whatever order the chunks are generated in:
 * <ul>
 *     <li>the bowl: within the rim (its radius wobbles a little round the circle) the ground is cut down to a bowl
 *     {@code depth} deep in the middle; its floor is burnt rock, black and grey in the middle (blackstone, basalt, tuff,
 *     a few glowing magma blocks), gravel and tuff towards the rim, with lumps of rubble, and two campfires still
 *     smoking;</li>
 *     <li>the rim: the ground thrown up round it, up to two blocks high, of the ground's own kind (sand in the desert,
 *     coarse dirt and gravel elsewhere), scorched at the top;</li>
 *     <li>the scorch marks: burnt streaks running out from the rim to more than twice its radius, and a ring of scorched
 *     ground round it (in the sand, the lightning has fused some of it to glass);</li>
 *     <li>the core ({@code enhanced:mjolnir}: a Mjolnir nobody owns) in the middle of the floor.</li>
 * </ul>
 * It is built after the trees and plants (step {@code top_layer_modification}): what stood where the bowl is, is gone.
 */
public class CraterPiece extends StructurePiece {
    private static final BlockState[] CENTER = {Blocks.BLACKSTONE.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.BASALT.defaultBlockState()};
    private static final BlockState[] MIDDLE = {Blocks.BLACKSTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
            Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
            Blocks.BASALT.defaultBlockState()};
    private static final BlockState[] OUTER = {Blocks.GRAVEL.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState()};
    private static final BlockState[] SCORCH = {Blocks.GRAVEL.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState()};
    private static final BlockState[] RUBBLE = {Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.BLACKSTONE_SLAB.defaultBlockState(), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState(), Blocks.BASALT.defaultBlockState()};

    private final BlockPos center;
    private final int radius;
    private final int depth;
    private final long seed;
    private final BlockState core;
    /** Phases of the wobble of the rim, and the scorch marks: their angles and lengths (as many radii past the rim). */
    private final double[] phases = new double[3];
    private final double[] rayAngles;
    private final double[] rayLengths;

    public CraterPiece(BlockPos center, int radius, int depth, long seed, BlockState core) {
        super(ModStructures.CRATER_PIECE.get(), 0, box(center, radius, depth));
        this.center = center;
        this.radius = radius;
        this.depth = depth;
        this.seed = seed;
        this.core = core;
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < phases.length; i++) {
            phases[i] = random.nextDouble() * Mth.TWO_PI;
        }
        int rays = 7 + random.nextInt(5);
        rayAngles = new double[rays];
        rayLengths = new double[rays];
        double start = random.nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < rays; i++) {
            rayAngles[i] = start + (i + 0.35 * (random.nextDouble() - 0.5)) * Mth.TWO_PI / rays;
            rayLengths[i] = 0.6 + 0.8 * random.nextDouble();
        }
    }

    public CraterPiece(CompoundTag tag) {
        this(new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ")), tag.getInt("R"), tag.getInt("D"), tag.getLong("Seed"),
                NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Core")));
    }

    private static BoundingBox box(BlockPos center, int radius, int depth) {
        int reach = reach(radius);
        return new BoundingBox(center.getX() - reach, center.getY() - depth - 4, center.getZ() - reach,
                center.getX() + reach, center.getY() + 24, center.getZ() + reach);
    }

    /** How far from the middle the crater's marks go. */
    static int reach(int radius) {
        return Mth.ceil(radius * 2.4) + 1;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("R", radius);
        tag.putInt("D", depth);
        tag.putLong("Seed", seed);
        tag.put("Core", NbtUtils.writeBlockState(core));
    }

    /** Where the core lies: on the floor in the middle of the bowl. */
    public BlockPos corePos() {
        return new BlockPos(center.getX(), center.getY() - depth + 1, center.getZ());
    }

    /** The radius of the rim at this angle: the crater is not quite round. */
    double rim(double angle) {
        return radius * (1.0 + 0.08 * Math.sin(3 * angle + phases[0]) + 0.05 * Math.sin(5 * angle + phases[1])
                + 0.03 * Math.sin(7 * angle + phases[2]));
    }

    /** How strongly a scorch mark burnt the ground {@code dist} blocks out at this angle (0 to 1). */
    double scorch(double angle, double dist, double rim) {
        double past = dist - rim;
        double ring = past < 0 ? 1.0 : Math.exp(-past / 2.2) * 0.85;
        double rays = 0.0;
        for (int i = 0; i < rayAngles.length; i++) {
            double length = rayLengths[i] * radius;
            if (past > length) {
                continue;
            }
            double width = 1.8 * (1.0 - 0.6 * Math.max(0.0, past) / length) + 0.4;
            double off = Mth.wrapDegrees((angle - rayAngles[i]) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * dist;
            rays = Math.max(rays, Math.exp(-(off * off) / (width * width)) * (1.0 - 0.5 * Math.max(0.0, past) / length));
        }
        return Math.max(ring, rays);
    }

    /** A number from 0 to 1 that depends only on the column, the crater and {@code salt}. */
    double noise(int x, int z, int salt) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    private BlockState pick(BlockState[] from, int x, int z, int salt) {
        return from[(int) (noise(x, z, salt) * from.length)];
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int reach = reach(radius);
        for (int x = Math.max(box.minX(), center.getX() - reach); x <= Math.min(box.maxX(), center.getX() + reach); x++) {
            for (int z = Math.max(box.minZ(), center.getZ() - reach); z <= Math.min(box.maxZ(), center.getZ() + reach); z++) {
                int dx = x - center.getX();
                int dz = z - center.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > reach) {
                    continue;
                }
                double angle = Math.atan2(dz, dx);
                double rim = rim(angle);
                if (dist < rim) {
                    bowl(level, box, x, z, dist / rim);
                } else if (dist < rim + 3.5) {
                    rimColumn(level, box, x, z, dist - rim, scorch(angle, dist, rim));
                } else {
                    scorched(level, box, x, z, scorch(angle, dist, rim));
                }
            }
        }
        BlockPos at = corePos();
        if (box.isInside(at)) {
            BlockState state = core;
            if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue((int) (noise(0, 0, 7) * 4)));
            }
            level.setBlock(at.below(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at, state, Block.UPDATE_CLIENTS);
        }
        for (int i = 0; i < 2; i++) {
            double a = noise(i, 1, 11) * Mth.TWO_PI;
            double r = (0.45 + 0.15 * noise(i, 2, 12)) * radius;
            int x = center.getX() + (int) Math.round(Math.cos(a) * r);
            int z = center.getZ() + (int) Math.round(Math.sin(a) * r);
            BlockPos fire = new BlockPos(x, floorY(Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2))
                    / rim(Math.atan2(z - center.getZ(), x - center.getX()))) + 1, z);
            if (box.isInside(fire)) {
                level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                        .setValue(CampfireBlock.SIGNAL_FIRE, false), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The height of the floor of the bowl at {@code r} (0 in the middle, 1 at the rim). */
    int floorY(double r) {
        return center.getY() - (int) Math.round(depth * (1.0 - r * r));
    }

    private void bowl(WorldGenLevel level, BoundingBox box, int x, int z, double r) {
        int floor = floorY(r);
        int top = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, center.getY() + 1);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y > floor; y--) {
            at.setY(y);
            if (!level.getBlockState(at).isAir()) {
                set(level, box, at, Blocks.AIR.defaultBlockState());
            }
        }
        BlockState[] layer = r < 0.32 ? CENTER : r < 0.7 ? MIDDLE : OUTER;
        BlockState surface = pick(layer, x, z, 1);
        if (r < 0.36 && noise(x, z, 2) < 0.07) {
            surface = Blocks.MAGMA_BLOCK.defaultBlockState();
        } else if (r >= 0.32 && r < 0.8 && noise(x, z, 2) < 0.04) {
            surface = Blocks.COAL_BLOCK.defaultBlockState();
        }
        set(level, box, at.setY(floor), surface);
        set(level, box, at.setY(floor - 1), pick(r < 0.5 ? CENTER : MIDDLE, x, z, 3));
        if (r < 0.6) {
            set(level, box, at.setY(floor - 2), Blocks.TUFF.defaultBlockState());
        }
        if (r > 0.25 && r < 0.92 && noise(x, z, 4) < 0.035) {
            set(level, box, at.setY(floor + 1), pick(RUBBLE, x, z, 5));
        }
    }

    private void rimColumn(WorldGenLevel level, BoundingBox box, int x, int z, double past, double burn) {
        int ground = ground(level, x, z);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, ground, z);
        BlockState was = level.getBlockState(at);
        boolean sandy = sandy(was);
        int lift = Math.max(0, (int) Math.round(2.3 * (1.0 - Math.abs(past - 0.9) / 2.6)));
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        for (int y = top; y > ground; y--) {
            at.setY(y);
            if (!level.getBlockState(at).isAir()) {
                set(level, box, at, Blocks.AIR.defaultBlockState());
            }
        }
        for (int k = 1; k <= lift; k++) {
            BlockState fill = sandy ? (was.is(Blocks.RED_SAND) || was.is(BlockTags.TERRACOTTA) ? Blocks.RED_SAND : Blocks.SAND).defaultBlockState()
                    : noise(x, z, 20 + k) < 0.55 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            set(level, box, at.setY(ground + k), fill);
        }
        if (noise(x, z, 6) < 0.25 + 0.6 * burn) {
            set(level, box, at.setY(ground + lift), burnt(was, x, z));
        }
    }

    private void scorched(WorldGenLevel level, BoundingBox box, int x, int z, double burn) {
        if (burn <= 0.05 || noise(x, z, 8) >= burn * 0.9) {
            return;
        }
        int ground = ground(level, x, z);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, ground, z);
        BlockState was = level.getBlockState(at);
        if (!natural(was)) {
            return;
        }
        set(level, box, at, burnt(was, x, z));
        at.setY(ground + 1);
        BlockState above = level.getBlockState(at);
        if (!above.isAir() && above.canBeReplaced()) {
            set(level, box, at, Blocks.AIR.defaultBlockState());
            BlockPos upper = at.above();
            if (level.getBlockState(upper).canBeReplaced() && !level.getBlockState(upper).isAir()) {
                set(level, box, upper, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** What a burnt block of this ground becomes: dark rock and gravel, and in the sand now and then fused glass. */
    private BlockState burnt(BlockState was, int x, int z) {
        if (sandy(was) && noise(x, z, 9) < 0.12) {
            return noise(x, z, 10) < 0.5 ? Blocks.GLASS.defaultBlockState() : Blocks.TINTED_GLASS.defaultBlockState();
        }
        return pick(SCORCH, x, z, 10);
    }

    /** The top block of the ground (not a plant, a log or leaves). */
    private static int ground(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        while (at.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                    && !state.is(Blocks.BAMBOO) && !state.is(Blocks.CACTUS)) {
                break;
            }
            at.move(0, -1, 0);
        }
        return at.getY();
    }

    /** Ground that burns: earth, grass, sand, gravel, clay, terracotta, stone and snow, not what somebody built. */
    private static boolean natural(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY) || state.is(Blocks.STONE) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.DIRT_PATH);
    }

    private static boolean sandy(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE);
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos at, BlockState state) {
        if (box.isInside(at)) {
            level.setBlock(at, state, Block.UPDATE_CLIENTS);
        }
    }
}
