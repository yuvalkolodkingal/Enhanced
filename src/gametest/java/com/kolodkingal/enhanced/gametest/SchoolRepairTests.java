package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.world.SchoolRepair;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * {@code /enhanced structure repair_school} (3.5): a Mutant School built as 3.3.0 built it (its front tower carved into
 * the hall's south wall) is mended to what 3.3.1 builds, at the school's turn, touching only the 76 changed blocks and
 * keeping what a player put there. The school is built far from the arenas (100,000 blocks out), where nothing else is.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolRepairTests {
    static final String BATCH = "school_repair";
    /** The school's middle, at ground level (the template's floor layer 6 lies here). */
    static final BlockPos CENTER = new BlockPos(100_408, 60, 100_808);

    /** The 76 changes: 71 blocks of the hall's wall round the door, a ladder of 5 up the tower. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theRepairKnowsTheSeventySixChangedBlocks(GameTestHelper helper) {
        List<SchoolRepair.Change> changes = SchoolRepair.changes();
        check(changes.size() == 76, "76 blocks differ between the 3.3.0 and 3.3.1 templates: " + changes.size());
        check(changes.stream().map(SchoolRepair.Change::pos).distinct().count() == 76, "each at a position of its own");
        check(changes.stream().filter(c -> c.after().is(Blocks.STONE_BRICKS)).count() == 71
                && changes.stream().filter(c -> c.after().is(Blocks.LADDER)).count() == 5, "71 stone bricks and 5 ladder rungs");
        check(changes.stream().allMatch(c -> c.pos().getX() >= 20 && c.pos().getX() <= 26 && c.pos().getZ() >= 27 && c.pos().getZ() <= 28),
                "all round the front door (x 20..26, z 27..28)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void anOldSchoolIsRepairedRoundItsDoorAndAPlayersBlockIsKept(GameTestHelper helper) throws CommandSyntaxException {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        StructureTemplate template = level.getStructureManager().getOrCreate(Enhanced.id("mutant_school/mansion"));
        Vec3i size = template.getSize();
        Holder<StructureProcessorList> processors = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, Enhanced.id("mutant_school")));
        TemplateSitePiece piece = new TemplateSitePiece(level.getStructureManager(), Enhanced.id("mutant_school/mansion"), processors,
                Rotation.CLOCKWISE_90, CENTER, size, 6);
        build(level, piece);
        // The door area as 3.3.1 builds it (template x 18..28, y 5..23, z 25..30), with the school's own weathering
        Map<BlockPos, BlockState> fresh = new HashMap<>();
        for (BlockPos local : BlockPos.betweenClosed(18, 5, 25, 28, 23, 30)) {
            BlockPos at = SchoolRepair.worldPos(piece, local);
            fresh.put(at.immutable(), level.getBlockState(at));
        }
        for (SchoolRepair.Change change : SchoolRepair.changes()) {
            BlockPos at = SchoolRepair.worldPos(piece, change.pos());
            check(SchoolRepair.isRepaired(level.getBlockState(at), SchoolRepair.turned(piece, change.after())),
                    "the template builds the 3.3.1 block at " + change.pos() + ": " + level.getBlockState(at));
        }
        // ... made old: the 3.3.0 blocks (open wall, the tower's floors and the roof's eaves through it, no ladder)
        for (SchoolRepair.Change change : SchoolRepair.changes()) {
            level.setBlock(SchoolRepair.worldPos(piece, change.pos()), SchoolRepair.turned(piece, change.before()),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        // a player has walled up one spot of the hole with glass, and set a crafting table on the porch by the door (not a
        // changed block, next to the ladder's foot)
        BlockPos glass = SchoolRepair.worldPos(piece, new BlockPos(23, 13, 27));
        level.setBlock(glass, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        BlockPos porch = SchoolRepair.worldPos(piece, new BlockPos(24, 7, 28));
        check(fresh.get(porch).isAir(), "the porch by the door is open: " + fresh.get(porch));
        level.setBlock(porch, Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        // the school's start, as the world keeps it in the chunk of its middle
        Structure school = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SchoolRepair.SCHOOL);
        ChunkPos middle = new ChunkPos(CENTER);
        ChunkAccess chunk = level.getChunk(middle.x, middle.z);
        Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());
        chunk.setStartForStructure(school, new StructureStart(school, middle, 0, new PiecesContainer(List.of(piece))));
        try {
            check(SchoolRepair.nearest(level, CENTER, 16).orElse(null) == piece, "the school is found by its start");
            List<Component> said = new ArrayList<>();
            // too far: over 100 blocks east of the school, looking 32 round
            int none = run(server, level, CENTER.offset(130, 0, 0), "enhanced structure repair_school 32", said);
            check(none == 0 && said.size() == 1 && says(said.get(0), "commands.enhanced.structure.repair_school.none"),
                    "no school within 32 blocks: " + said);
            check(SchoolRepair.same(level.getBlockState(SchoolRepair.worldPos(piece, new BlockPos(22, 12, 27))), Blocks.AIR.defaultBlockState()),
                    "and nothing was touched");
            said.clear();
            // from some 37 blocks off its north side, with the default reach
            int repaired = run(server, level, CENTER.offset(0, 0, -60), "enhanced structure repair_school", said);
            check(repaired == 75, "75 blocks repaired (one is the player's): " + repaired);
            check(said.size() == 1 && says(said.get(0), "commands.enhanced.structure.repair_school.done")
                    && arg(said.get(0), 3).equals(75) && arg(said.get(0), 4).equals(1), "the message counts them: " + said);
            check(level.getBlockState(glass).is(Blocks.GLASS), "the player's glass is kept: " + level.getBlockState(glass));
            check(level.getBlockState(porch).is(Blocks.CRAFTING_TABLE), "the crafting table on the porch is not touched");
            for (Map.Entry<BlockPos, BlockState> e : fresh.entrySet()) {
                if (e.getKey().equals(glass) || e.getKey().equals(porch)) {
                    continue;
                }
                check(level.getBlockState(e.getKey()).equals(e.getValue()), "the door area is as 3.3.1 builds it at " + e.getKey()
                        + ": " + level.getBlockState(e.getKey()) + " instead of " + e.getValue());
            }
            // again: nothing left to do but the player's spot
            said.clear();
            check(run(server, level, CENTER, "enhanced structure repair_school", said) == 0
                    && says(said.get(0), "commands.enhanced.structure.repair_school.nothing") && arg(said.get(0), 4).equals(1),
                    "a second run repairs nothing and still names the player's block: " + said);
            // the player takes his glass away: the old air is back, so the last block is mended too
            level.setBlock(glass, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            said.clear();
            check(run(server, level, CENTER, "enhanced structure repair_school", said) == 1, "the last block is set: " + said);
            check(level.getBlockState(glass).equals(fresh.get(glass)), "as the template has it: " + level.getBlockState(glass));
            said.clear();
            check(run(server, level, CENTER, "enhanced structure repair_school", said) == 0
                    && says(said.get(0), "commands.enhanced.structure.repair_school.already"), "then the school is whole: " + said);
        } finally {
            chunk.setAllStarts(starts);
        }
        helper.succeed();
    }

    private static int run(MinecraftServer server, ServerLevel level, BlockPos at, String command, List<Component> said)
            throws CommandSyntaxException {
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                said.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = new CommandSourceStack(sink, Vec3.atCenterOf(at), Vec2.ZERO, level, 2, "test", Component.literal("test"),
                server, null);
        return server.getCommands().getDispatcher().execute(command, source);
    }

    /** Whether the message is that translation (a failure comes red, as the sibling of an empty text). */
    private static boolean says(Component text, String key) {
        return text.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                || text.getSiblings().stream().anyMatch(sibling -> says(sibling, key));
    }

    private static Object arg(Component text, int i) {
        return ((TranslatableContents) text.getContents()).getArgs()[i];
    }

    /** Builds a piece chunk by chunk, as the world's generation does (as in WorldStructureTests). */
    private static void build(ServerLevel level, TemplateSitePiece piece) {
        BoundingBox box = piece.getBoundingBox();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                level.getChunk(cx, cz);
                BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                        level.getMaxBuildHeight(), chunk.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), generator, RandomSource.create(cx * 31L + cz), chunkBox, chunk, box.getCenter());
            }
        }
    }

    private SchoolRepairTests() {
    }
}
