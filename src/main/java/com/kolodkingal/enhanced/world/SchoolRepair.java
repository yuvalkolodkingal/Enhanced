package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Mends a Mutant School built from the 3.3.0 template, whose front tower had carved away the hall's south wall round the
 * door (fixed in 3.3.1 for schools built after the update). {@code /enhanced structure repair_school [radius]} finds the
 * school nearest the one who runs it and sets down again the 76 blocks in which the 3.3.1 template differs from 3.3.0,
 * turned and mirrored as that school was, through its own processors (so the stone bricks weather as in a new school),
 * and nothing else. A block there that is no longer what the 3.3.0 template put (a player changed it) is left alone and
 * counted; one already right (repaired before, or built from 3.3.1) is counted as such.
 */
public final class SchoolRepair {
    public static final ResourceKey<Structure> SCHOOL = ResourceKey.create(Registries.STRUCTURE, Enhanced.id("mutant_school"));
    public static final int DEFAULT_RADIUS = 128;
    public static final int MAX_RADIUS = 256;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState EAVES = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
    private static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState LADDER = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);

    /**
     * The difference of the two templates ({@code mutant_school/mansion}, 47 x 34 x 44), unturned template positions, a
     * column per entry: "x z" and then "y:" with the 3.3.0 and the 3.3.1 block (A air, P oak planks, S the roof's eaves,
     * deepslate tile stairs facing north, B stone bricks, L a ladder facing west). The wall at z 27 is the hall's south wall
     * round the door; the ladder at x 25, z 28 is the tower's, from the porch floor.
     */
    private static final String[] COLUMNS = {
            "20 27 18:SB",
            "21 27 7:AB 8:AB 9:AB 10:AB 11:PB 12:AB 13:AB 14:AB 15:AB 16:PB 17:AB 18:SB 19:AB 20:AB 21:AB",
            "22 27 9:AB 10:AB 11:PB 12:AB 13:AB 14:AB 15:AB 16:PB 17:AB 18:SB 19:AB 20:AB 21:AB",
            "23 27 9:AB 10:AB 11:PB 12:AB 13:AB 14:AB 15:AB 16:PB 17:AB 18:SB 19:AB 20:AB 21:AB",
            "24 27 9:AB 10:AB 11:PB 12:AB 13:AB 14:AB 15:AB 16:PB 17:AB 18:SB 19:AB 20:AB 21:AB",
            "25 27 7:AB 8:AB 9:AB 10:AB 11:PB 12:AB 13:AB 14:AB 15:AB 16:PB 17:AB 18:SB 19:AB 20:AB 21:AB",
            "26 27 18:SB",
            "25 28 7:AL 8:AL 9:AL 10:AL 11:PL",
    };

    private static final List<Change> CHANGES = parse();

    /** One block of the template that 3.3.1 changed: its template position (unturned), what 3.3.0 and 3.3.1 put there. */
    public record Change(BlockPos pos, BlockState before, BlockState after) {
    }

    /** What a repair did: blocks set down, blocks already right, blocks a player had changed (left alone). */
    public record Result(int repaired, int alreadyRight, int skipped) {
    }

    public static List<Change> changes() {
        return CHANGES;
    }

    private static List<Change> parse() {
        List<Change> out = new ArrayList<>();
        for (String column : COLUMNS) {
            String[] parts = column.split(" ");
            int x = Integer.parseInt(parts[0]);
            int z = Integer.parseInt(parts[1]);
            for (int i = 2; i < parts.length; i++) {
                String[] entry = parts[i].split(":");
                out.add(new Change(new BlockPos(x, Integer.parseInt(entry[0]), z), code(entry[1].charAt(0)), code(entry[1].charAt(1))));
            }
        }
        return List.copyOf(out);
    }

    private static BlockState code(char c) {
        return switch (c) {
            case 'A' -> AIR;
            case 'P' -> PLANKS;
            case 'S' -> EAVES;
            case 'B' -> BRICKS;
            case 'L' -> LADDER;
            default -> throw new IllegalArgumentException("unknown block code " + c);
        };
    }

    /**
     * The piece of the Mutant School nearest {@code pos} (horizontally, to its footprint), if one lies within
     * {@code radius} blocks. Schools are found by their structure starts, so a school from before 3.3.1 is found too.
     */
    public static Optional<TemplateSitePiece> nearest(ServerLevel level, BlockPos pos, int radius) {
        Structure school = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SCHOOL);
        if (school == null) {
            return Optional.empty();
        }
        // a start lies in the chunk of the school's middle, at most half its 47 blocks from its edge
        int reach = (radius + 32) >> 4;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        TemplateSitePiece best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_STARTS, true);
                StructureStart start = chunk == null ? null : chunk.getStartForStructure(school);
                if (start == null || !start.isValid()) {
                    continue;
                }
                for (StructurePiece piece : start.getPieces()) {
                    if (piece instanceof TemplateSitePiece site) {
                        double distance = horizontalDistance(site.getBoundingBox(), pos);
                        if (distance <= radius && distance < bestDistance) {
                            best = site;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static double horizontalDistance(BoundingBox box, BlockPos pos) {
        int dx = Math.max(0, Math.max(box.minX() - pos.getX(), pos.getX() - box.maxX()));
        int dz = Math.max(0, Math.max(box.minZ() - pos.getZ(), pos.getZ() - box.maxZ()));
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    /** Where a template position of the school lies in the world (turned and mirrored as the school was built). */
    public static BlockPos worldPos(TemplateSitePiece piece, BlockPos templatePos) {
        return StructureTemplate.calculateRelativePosition(piece.placeSettings(), templatePos).offset(piece.templatePosition());
    }

    /** A template block as the school's turn and mirror set it down (before its processors). */
    public static BlockState turned(TemplateSitePiece piece, BlockState state) {
        StructurePlaceSettings settings = piece.placeSettings();
        return state.mirror(settings.getMirror()).rotate(settings.getRotation());
    }

    /** Whether a block in the world is the 3.3.1 block, or what the school's processors make of it (weathered bricks). */
    public static boolean isRepaired(BlockState there, BlockState after) {
        if (after.is(Blocks.STONE_BRICKS)) {
            return there.is(Blocks.STONE_BRICKS) || there.is(Blocks.MOSSY_STONE_BRICKS) || there.is(Blocks.CRACKED_STONE_BRICKS);
        }
        return same(there, after);
    }

    /**
     * Whether a block in the world is still the template's: the same block, facing and half (a stair's shape follows its
     * neighbours once built, and rain or water may have changed what is waterlogged).
     */
    public static boolean same(BlockState there, BlockState template) {
        if (template.isAir()) {
            return there.isAir();
        }
        return there.is(template.getBlock())
                && (!template.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || there.getValue(BlockStateProperties.HORIZONTAL_FACING) == template.getValue(BlockStateProperties.HORIZONTAL_FACING))
                && (!template.hasProperty(BlockStateProperties.HALF)
                || there.getValue(BlockStateProperties.HALF) == template.getValue(BlockStateProperties.HALF));
    }

    /** Sets the 3.3.1 blocks of the school down where the 3.3.0 ones still stand; touches nothing else. */
    public static Result repair(ServerLevel level, TemplateSitePiece piece) {
        StructurePlaceSettings own = piece.placeSettings();
        // the school's own turn and processors, without the footing (which would add blocks under these)
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(own.getRotation()).setMirror(own.getMirror())
                .setRotationPivot(own.getRotationPivot());
        for (StructureProcessor processor : own.getProcessors()) {
            if (!(processor instanceof FoundationProcessor)) {
                settings.addProcessor(processor);
            }
        }
        List<StructureTemplate.StructureBlockInfo> wanted = new ArrayList<>();
        int already = 0;
        int skipped = 0;
        for (Change change : CHANGES) {
            BlockState there = level.getBlockState(worldPos(piece, change.pos()));
            if (isRepaired(there, turned(piece, change.after()))) {
                already++;
            } else if (same(there, turned(piece, change.before()))) {
                wanted.add(new StructureTemplate.StructureBlockInfo(change.pos(), change.after(), null));
            } else {
                skipped++;
            }
        }
        List<StructureTemplate.StructureBlockInfo> placed = StructureTemplate.processBlockInfos(level, piece.templatePosition(),
                piece.templatePosition(), settings, wanted, null);
        // whole blocks first, so a ladder finds its wall
        placed.sort(Comparator.comparing(info -> !info.state().isCollisionShapeFullBlock(level, info.pos())));
        for (StructureTemplate.StructureBlockInfo info : placed) {
            BlockState state = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
            level.setBlock(info.pos(), state, Block.UPDATE_ALL);
        }
        return new Result(placed.size(), already, skipped);
    }

    /** {@code structure repair_school [radius]} (and 3.7's {@code school_lab}) under {@code /enhanced} (operators). */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("structure")
                .then(Commands.literal("repair_school")
                        .executes(ctx -> run(ctx.getSource(), DEFAULT_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, MAX_RADIUS))
                                .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                // (3.7: the X-Men lower lab, lane L4's.)
                .then(com.kolodkingal.enhanced.school.LabCommand.node());
    }

    private static int run(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        Vec3 at = source.getPosition();
        Optional<TemplateSitePiece> found = nearest(level, BlockPos.containing(at), radius);
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("commands.enhanced.structure.repair_school.none", radius));
            return 0;
        }
        TemplateSitePiece piece = found.get();
        BlockPos door = worldPos(piece, new BlockPos(23, 7, 27));
        Result result = repair(level, piece);
        String key = result.repaired() > 0 ? "done" : result.skipped() > 0 ? "nothing" : "already";
        Component message = Component.translatable("commands.enhanced.structure.repair_school." + key,
                door.getX(), door.getY(), door.getZ(), result.repaired(), result.skipped());
        source.sendSuccess(() -> message, true);
        return result.repaired();
    }

    private SchoolRepair() {
    }
}
