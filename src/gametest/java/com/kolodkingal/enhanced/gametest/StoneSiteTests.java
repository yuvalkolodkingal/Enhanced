package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.block.TesseractCradleBlock;
import com.kolodkingal.enhanced.config.StoneSitesConfig;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.world.CarvedSiteStructure;
import com.kolodkingal.enhanced.world.NetherSiteStructure;
import com.kolodkingal.enhanced.world.SealProcessor;
import com.kolodkingal.enhanced.world.SitePoints;
import com.kolodkingal.enhanced.world.SiteSwitch;
import com.kolodkingal.enhanced.world.StoneSitePlans;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.kolodkingal.enhanced.world.TemplateSiteStructure;
import com.kolodkingal.enhanced.world.TowerSiteStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The stones' four sites (3.6, lane L4, the plan's section 4): the Sanctum Sanctorum, Avengers Tower, the End tunnel
 * and the Vormir Cliff. Their data, their templates (sizes, the size budget, real blocks only, the fixed points of
 * {@link StoneSitePlans}, every door in a whole frame, no room open to the outside), their loot, how each kind finds
 * its place (the Nether's cavern floor, the End island's edge, the switches of {@code [stone_sites]}), and each built
 * for real far from the arenas, chunk by chunk as the world does, within its time budget. Batches {@code stone_sites}
 * (the data and the templates), {@code stone_sites_gen} (building and generating) and {@code stone_sites_cfg} (a switch
 * turned off).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class StoneSiteTests {
    static final String BATCH = "stone_sites";
    static final String GEN = "stone_sites_gen";
    static final String CFG = "stone_sites_cfg";

    /** The budget of one template's file, and of the four sites' together (the plan's 4). */
    static final int TEMPLATE_KB = 200;
    static final int ALL_KB = 600;
    /** The time budget of building one chunk of a site (the worst chunk) and of its average chunk, in ms. */
    static final double CHUNK_WORST_MS = 400;
    static final double CHUNK_AVERAGE_MS = 120;

    // ------------------------------------------------------------------------------------------------ templates

    /** A template as its file has it: its size, its blocks (absent = void), the block entities' data, its entities. */
    record Raw(Vec3i size, Map<BlockPos, BlockState> blocks, Map<BlockPos, CompoundTag> data,
               List<CompoundTag> entities, List<CompoundTag> palette, int bytes) {
        BlockState at(BlockPos p) {
            return blocks.get(p);
        }

        BlockState at(int x, int y, int z) {
            return blocks.get(new BlockPos(x, y, z));
        }

        boolean inside(BlockPos p) {
            return p.getX() >= 0 && p.getY() >= 0 && p.getZ() >= 0 && p.getX() < size.getX() && p.getY() < size.getY()
                    && p.getZ() < size.getZ();
        }
    }

    static Raw raw(MinecraftServer server, ResourceLocation template) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(template.getNamespace(),
                "structure/" + template.getPath() + ".nbt");
        try (InputStream in = server.getResourceManager().getResource(file).orElseThrow().open()) {
            byte[] bytes = in.readAllBytes();
            CompoundTag root = NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes),
                    NbtAccounter.unlimitedHeap());
            ListTag sizeTag = root.getList("size", Tag.TAG_INT);
            Vec3i size = new Vec3i(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
            List<CompoundTag> palette = new ArrayList<>();
            List<BlockState> states = new ArrayList<>();
            for (Tag t : root.getList("palette", Tag.TAG_COMPOUND)) {
                palette.add((CompoundTag) t);
                states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) t));
            }
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            Map<BlockPos, CompoundTag> data = new HashMap<>();
            for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag b = (CompoundTag) t;
                ListTag pos = b.getList("pos", Tag.TAG_INT);
                BlockPos p = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
                blocks.put(p, states.get(b.getInt("state")));
                if (b.contains("nbt")) {
                    data.put(p, b.getCompound("nbt"));
                }
            }
            List<CompoundTag> entities = new ArrayList<>();
            for (Tag t : root.getList("entities", Tag.TAG_COMPOUND)) {
                entities.add(((CompoundTag) t).getCompound("nbt"));
            }
            return new Raw(size, blocks, data, entities, palette, bytes.length);
        } catch (IOException e) {
            throw new AssertionError("the template " + template + " cannot be read: " + e);
        }
    }

    /** How far the tower's top part reaches out past its base on each side (3.6.1: 4). */
    static final int TOWER_PAD = (StoneSitePlans.Tower.TOP_SIZE.getX() - StoneSitePlans.Tower.BASE_SIZE.getX()) / 2;

    /**
     * The whole tower in the top part's frame (33 x 114 x 33): the base and the mid centred in it, 32 layers apart, 4
     * in from its sides; {@link #inTower} turns a point of the base's frame into it.
     */
    static Raw tower(MinecraftServer server) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Map<BlockPos, CompoundTag> data = new HashMap<>();
        List<CompoundTag> entities = new ArrayList<>();
        int bytes = 0;
        for (int i = 0; i < 3; i++) {
            Raw part = raw(server, StoneSitePlans.Tower.PARTS.get(i));
            int off = (StoneSitePlans.Tower.TOP_SIZE.getX() - part.size().getX()) / 2;
            BlockPos shift = new BlockPos(off, StoneSitePlans.Tower.PART_Y[i], off);
            part.blocks().forEach((p, s) -> blocks.put(p.offset(shift), s));
            part.data().forEach((p, d) -> data.put(p.offset(shift), d));
            entities.addAll(part.entities());
            bytes += part.bytes();
        }
        Vec3i top = StoneSitePlans.Tower.TOP_SIZE;
        return new Raw(new Vec3i(top.getX(), StoneSitePlans.Tower.PART_Y[2] + top.getY(), top.getZ()), blocks, data,
                entities, List.of(), bytes);
    }

    /** The ladder's column in the tower's base frame, and its first layer (over the lobby's floor). */
    static final BlockPos TOWER_LADDER = new BlockPos(3, 0, 12);
    static final int TOWER_LADDER_FROM = 3;
    /** A place to stand in each of the tower's rooms, in its whole frame (3.6.1). */
    static final Map<String, BlockPos> TOWER_ROOMS = Map.ofEntries(Map.entry("lobby", new BlockPos(12, 3, 20)),
            Map.entry("Hawkeye", new BlockPos(12, 14, 20)), Map.entry("Black Widow", new BlockPos(12, 20, 17)),
            Map.entry("Hulk", new BlockPos(12, 26, 19)), Map.entry("Thor", new BlockPos(12, 38, 15)),
            Map.entry("Spider-Man", new BlockPos(12, 44, 16)), Map.entry("Scarlet Witch", new BlockPos(12, 50, 17)),
            Map.entry("lab", new BlockPos(12, 56, 17)), Map.entry("Iron Man", new BlockPos(12, 62, 10)),
            Map.entry("meeting room", new BlockPos(12, 74, 22)), Map.entry("penthouse", new BlockPos(16, 83, 15)),
            Map.entry("Captain America", new BlockPos(7, 83, 9)), Map.entry("observation", new BlockPos(14, 90, 19)));

    /** A point of the tower's base frame in the whole tower's frame ({@link #tower}). */
    static BlockPos inTower(BlockPos base) {
        return base.offset(TOWER_PAD, 0, TOWER_PAD);
    }

    /** What a flood from outside goes through: void, air, anything with no collision, and the small things. */
    static boolean passes(BlockState s) {
        return s == null || s.isAir() || s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty()
                || s.is(BlockTags.WOOL_CARPETS) || s.is(BlockTags.CANDLES) || s.is(Blocks.LANTERN)
                || s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.CHAIN) || s.is(Blocks.END_ROD) || s.is(Blocks.LADDER);
    }

    /**
     * The rooms of {@code inside} that a flood from the template's faces reaches (through void, air and anything with
     * no collision; closed doors stop it, and so do the {@code openings}, a doorway with no door): a hole in a wall,
     * as the 3.3 Mutant School had round its door. Empty is right.
     */
    static List<BlockPos> openRooms(Raw t, List<BlockPos> inside, Set<BlockPos> openings) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Vec3i s = t.size();
        for (int x = 0; x < s.getX(); x++) {
            for (int y = 0; y < s.getY(); y++) {
                for (int z = 0; z < s.getZ(); z++) {
                    if (x == 0 || y == 0 || z == 0 || x == s.getX() - 1 || y == s.getY() - 1 || z == s.getZ() - 1) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!openings.contains(p) && passes(t.at(p)) && seen.add(p)) {
                            queue.add(p);
                        }
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (t.inside(n) && !openings.contains(n) && passes(t.at(n)) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        return inside.stream().filter(seen::contains).toList();
    }

    /** The doors with a hole beside them, over them or under them (the other leaf of a double door is no hole). */
    static List<String> brokenDoors(Raw t) {
        List<String> broken = new ArrayList<>();
        t.blocks().forEach((p, s) -> {
            if (!(s.getBlock() instanceof DoorBlock) || s.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                return;
            }
            Direction facing = s.getValue(DoorBlock.FACING);
            List<BlockPos> frame = new ArrayList<>(List.of(p.below(), p.above(2)));
            for (Direction side : List.of(facing.getClockWise(), facing.getCounterClockWise())) {
                frame.add(p.relative(side));
                frame.add(p.above().relative(side));
            }
            for (BlockPos f : frame) {
                BlockState there = t.at(f);
                if (!(there != null && there.getBlock() instanceof DoorBlock) && passes(there)) {
                    broken.add(p + " (" + facing + ") at " + f + ": " + there);
                }
            }
        });
        return broken;
    }

    static List<String> chestTables(Raw t) {
        List<String> tables = new ArrayList<>();
        t.blocks().forEach((p, s) -> {
            if (s.is(Blocks.CHEST) || s.is(Blocks.BARREL)) {
                CompoundTag d = t.data().get(p);
                tables.add(d == null ? "" : d.getString("LootTable"));
            }
        });
        return tables;
    }

    static long count(List<String> list, String what) {
        return list.stream().filter(what::equals).count();
    }

    // ------------------------------------------------------------------------------------------------ data

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFourSitesAndTheirSetsAreData(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var structures = access.registryOrThrow(Registries.STRUCTURE);
        var sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        check(structures.get(Enhanced.id("sanctum")) instanceof TemplateSiteStructure s
                && s.template().equals(StoneSitePlans.Sanctum.TEMPLATE) && s.floor() == StoneSitePlans.Sanctum.FLOOR
                && s.ground().equals("highest"),
                "the Sanctum is a template site on its floor 3, on its highest ground");
        check(structures.get(Enhanced.id("mutant_school")) instanceof TemplateSiteStructure school
                && school.ground().equals("median"), "3.3's sites keep the median");
        check(structures.get(Enhanced.id("avengers_tower")) instanceof TowerSiteStructure t
                && t.parts().equals(StoneSitePlans.Tower.PARTS) && t.partHeight() == StoneSitePlans.Tower.PART_Y[1]
                && t.floor() == StoneSitePlans.Tower.FLOOR && t.ground().equals("highest"),
                "the tower stacks its three parts 32 apart, floor 2, on its highest ground");
        check(SiteSwitch.ground("highest", new int[]{60, 61, 61, 62, 62, 62, 63, 63, 64}) == 64
                && SiteSwitch.ground("median", new int[]{60, 61, 61, 62, 62, 62, 63, 63, 64}) == 62,
                "highest: no ground round the door stands over it; median: 3.3's rule");
        check(structures.get(Enhanced.id("end_tunnel")) instanceof CarvedSiteStructure c
                && c.template().equals(StoneSitePlans.EndTunnel.TEMPLATE), "the End tunnel is carved");
        check(structures.get(Enhanced.id("vormir_cliff")) instanceof NetherSiteStructure n
                && n.template().equals(StoneSitePlans.Vormir.TEMPLATE), "the Vormir Cliff is a Nether site");
        record Spread(String set, int spacing, int separation, int salt) {
        }
        for (Spread spread : List.of(new Spread("sanctums", 48, 16, 20161104),
                new Spread("avengers_towers", 64, 24, 20120504), new Spread("end_tunnels", 28, 10, 20140801),
                new Spread("vormir_cliffs", 20, 8, 20180427))) {
            StructureSet set = sets.get(Enhanced.id(spread.set()));
            check(set != null && set.placement() instanceof RandomSpreadStructurePlacement r
                    && r.spacing() == spread.spacing() && r.separation() == spread.separation(),
                    spread.set() + " is spread " + spread.spacing() + "/" + spread.separation());
        }
        record Biomes4(String site, List<ResourceKey<Biome>> has, List<ResourceKey<Biome>> hasNot) {
        }
        var biomes = access.registryOrThrow(Registries.BIOME);
        for (Biomes4 b : List.of(
                new Biomes4("sanctum", List.of(Biomes.PLAINS, Biomes.CHERRY_GROVE, Biomes.TAIGA),
                        List.of(Biomes.DESERT, Biomes.OCEAN, Biomes.NETHER_WASTES)),
                new Biomes4("avengers_tower", List.of(Biomes.PLAINS, Biomes.MEADOW, Biomes.BIRCH_FOREST),
                        List.of(Biomes.TAIGA, Biomes.OCEAN, Biomes.THE_END)),
                new Biomes4("end_tunnel", List.of(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS),
                        List.of(Biomes.THE_END, Biomes.SMALL_END_ISLANDS, Biomes.PLAINS)),
                new Biomes4("vormir_cliff", List.of(Biomes.SOUL_SAND_VALLEY, Biomes.BASALT_DELTAS),
                        List.of(Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.PLAINS)))) {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, Enhanced.id("has_structure/" + b.site()));
            for (var key : b.has()) {
                check(biomes.getHolderOrThrow(key).is(tag), b.site() + " is built in " + key.location());
            }
            for (var key : b.hasNot()) {
                check(!biomes.getHolderOrThrow(key).is(tag), b.site() + " is never built in " + key.location());
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTemplatesHaveThePlansSizesAndStayInTheirBudget(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        record Sized(ResourceLocation template, Vec3i size) {
        }
        int total = 0;
        for (Sized s : List.of(new Sized(StoneSitePlans.Sanctum.TEMPLATE, StoneSitePlans.Sanctum.SIZE),
                new Sized(StoneSitePlans.Tower.BASE, StoneSitePlans.Tower.BASE_SIZE),
                new Sized(StoneSitePlans.Tower.MID, StoneSitePlans.Tower.MID_SIZE),
                new Sized(StoneSitePlans.Tower.TOP, StoneSitePlans.Tower.TOP_SIZE),
                new Sized(StoneSitePlans.EndTunnel.TEMPLATE, StoneSitePlans.EndTunnel.SIZE),
                new Sized(StoneSitePlans.Vormir.TEMPLATE, StoneSitePlans.Vormir.SIZE))) {
            Raw t = raw(server, s.template());
            check(t.size().equals(s.size()), s.template() + " is " + s.size() + ": " + t.size());
            check(server.getStructureManager().getOrCreate(s.template()).getSize().equals(s.size()),
                    s.template() + " loads with its size");
            check(t.bytes() <= TEMPLATE_KB * 1024, s.template() + " is within " + TEMPLATE_KB + " KB: " + t.bytes());
            check(t.blocks().size() > 1000, s.template() + " is the real site, not the skeleton's marker");
            total += t.bytes();
        }
        check(total <= ALL_KB * 1024, "the four sites are within " + ALL_KB + " KB together: " + total);
        helper.succeed();
    }

    /** Every palette entry of the four sites names a block of the game with properties it has (none dropped). */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyBlockOfTheSitesIsARealBlock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (ResourceLocation template : List.of(StoneSitePlans.Sanctum.TEMPLATE, StoneSitePlans.Tower.BASE,
                StoneSitePlans.Tower.MID, StoneSitePlans.Tower.TOP, StoneSitePlans.EndTunnel.TEMPLATE,
                StoneSitePlans.Vormir.TEMPLATE)) {
            for (CompoundTag entry : raw(server, template).palette()) {
                ResourceLocation id = ResourceLocation.parse(entry.getString("Name"));
                check(BuiltInRegistries.BLOCK.containsKey(id), template + ": no such block " + id);
                Block block = BuiltInRegistries.BLOCK.get(id);
                CompoundTag props = entry.getCompound("Properties");
                for (String key : props.getAllKeys()) {
                    Property<?> property = block.getStateDefinition().getProperty(key);
                    check(property != null && property.getValue(props.getString(key)).isPresent(),
                            template + ": " + id + " has no " + key + "=" + props.getString(key));
                }
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ holes

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void everyDoorOfTheSitesHasAWholeFrame(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        int doors = 0;
        for (Raw t : List.of(raw(server, StoneSitePlans.Sanctum.TEMPLATE), tower(server),
                raw(server, StoneSitePlans.EndTunnel.TEMPLATE), raw(server, StoneSitePlans.Vormir.TEMPLATE))) {
            List<String> broken = brokenDoors(t);
            check(broken.isEmpty(), "a door with a hole round it: " + broken);
            doors += (int) t.blocks().values().stream().filter(s -> s.getBlock() instanceof DoorBlock).count();
        }
        check(doors >= 12, "the doors were looked at (the Sanctum's, the tower's): " + doors);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void noRoomOfTheSitesIsOpenToTheOutside(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<BlockPos> sanctum = List.of(new BlockPos(5, 4, 4), new BlockPos(11, 4, 5), new BlockPos(17, 4, 4),
                new BlockPos(11, 1, 8), new BlockPos(6, 10, 5), new BlockPos(11, 10, 4), new BlockPos(17, 10, 6),
                new BlockPos(11, 16, 8), new BlockPos(11, 20, 6));
        check(openRooms(raw(server, StoneSitePlans.Sanctum.TEMPLATE), sanctum, Set.of()).isEmpty(),
                "the Sanctum is shut: " + openRooms(raw(server, StoneSitePlans.Sanctum.TEMPLATE), sanctum, Set.of()));
        // the tower (its whole frame): the lobby, the rooms of the six and the others, Cap's closet, the core, the fin
        List<BlockPos> tower = new ArrayList<>(TOWER_ROOMS.values());
        tower.addAll(List.of(new BlockPos(7, 50, 16), new BlockPos(10, 97, 16), new BlockPos(7, 38, 19)));
        check(openRooms(tower(server), tower, Set.of()).isEmpty(),
                "the tower is shut: " + openRooms(tower(server), tower, Set.of()));
        // the End tunnel: shut but for its arch
        Set<BlockPos> arch = new HashSet<>();
        for (int x = 6; x <= 8; x++) {
            for (int y = 9; y <= 12; y++) {
                arch.add(new BlockPos(x, y, 2));
            }
        }
        List<BlockPos> tunnel = List.of(new BlockPos(7, 9, 4), new BlockPos(9, 5, 16), new BlockPos(4, 2, 27),
                new BlockPos(7, 1, 38), new BlockPos(4, 1, 43));
        Raw end = raw(server, StoneSitePlans.EndTunnel.TEMPLATE);
        check(openRooms(end, tunnel, Set.of()).size() == tunnel.size(), "the tunnel is open at its arch");
        check(openRooms(end, tunnel, arch).isEmpty(), "and nowhere else: " + openRooms(end, tunnel, arch));
        // the Vormir Cliff's pit: shut but for the chasm's mouth and the tunnel's
        Set<BlockPos> mouths = new HashSet<>();
        for (int x = 12; x <= 20; x++) {
            for (int z = 12; z <= 20; z++) {
                mouths.add(new BlockPos(x, StoneSitePlans.Vormir.SUMMIT.getY(), z));
            }
        }
        for (int y = 0; y < 12; y++) {
            for (int z = 15; z <= 17; z++) {
                mouths.add(new BlockPos(0, y, z));
            }
        }
        List<BlockPos> pit = List.of(new BlockPos(16, 5, 17), new BlockPos(5, 2, 16));
        check(openRooms(raw(server, StoneSitePlans.Vormir.TEMPLATE), pit, mouths).isEmpty(),
                "the pit and its tunnel have no other way out: "
                        + openRooms(raw(server, StoneSitePlans.Vormir.TEMPLATE), pit, mouths));
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ fixed points

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSanctumsPedestalHoldsTheTimeStone(GameTestHelper helper) {
        Raw t = raw(helper.getLevel().getServer(), StoneSitePlans.Sanctum.TEMPLATE);
        BlockState pedestal = t.at(StoneSitePlans.Sanctum.PEDESTAL);
        check(pedestal != null && pedestal.is(ModBlocks.STONE_PEDESTAL.get())
                        && pedestal.getValue(StonePedestalBlock.STONE) == StonePedestalBlock.Stone.TIME,
                "the Time Stone's pedestal is at " + StoneSitePlans.Sanctum.PEDESTAL + ": " + pedestal);
        check(t.blocks().values().stream().filter(s -> s.is(ModBlocks.STONE_PEDESTAL.get())).count() == 1,
                "one pedestal");
        BlockPos under = StoneSitePlans.Sanctum.PEDESTAL.below();
        check(!passes(t.at(under)), "it stands on the top floor: " + t.at(under));
        for (int up = 1; up <= 2; up++) {
            check(passes(t.at(StoneSitePlans.Sanctum.PEDESTAL.above(up))), "room over it for the stone");
        }
        // the Window of Worlds in the front wall behind it, of light blue, white and yellow glass
        int glass = 0;
        for (int x = 7; x <= 15; x++) {
            for (int y = 15; y <= 23; y++) {
                BlockState s = t.at(x, y, 15);
                if (s != null && (s.is(Blocks.LIGHT_BLUE_STAINED_GLASS) || s.is(Blocks.WHITE_STAINED_GLASS)
                        || s.is(Blocks.YELLOW_STAINED_GLASS))) {
                    glass++;
                }
            }
        }
        check(glass >= 9, "the Window of Worlds: " + glass);
        List<String> tables = chestTables(t);
        check(count(tables, "enhanced:chests/sanctum/library") == 2 && count(tables, "enhanced:chests/sanctum/relics")
                == 1 && count(tables, "enhanced:chests/sanctum/ward_room") == 1 && tables.size() == 4,
                "two library chests, the relics, the ward room: " + tables);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTowersCradleIsOnItsRoofAndItsPartsAlign(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Raw top = raw(server, StoneSitePlans.Tower.TOP);
        BlockState cradle = top.at(StoneSitePlans.Tower.CRADLE_IN_TOP);
        check(cradle != null && cradle.is(ModBlocks.TESSERACT_CRADLE.get())
                        && cradle.getValue(TesseractCradleBlock.FULL),
                "the full cradle is at " + StoneSitePlans.Tower.CRADLE_IN_TOP + " of the top: " + cradle);
        Raw whole = tower(server);
        BlockPos at = inTower(StoneSitePlans.Tower.CRADLE);
        check(whole.at(at) == cradle, "and at " + StoneSitePlans.Tower.CRADLE + " of the tower");
        check(whole.blocks().values().stream().filter(s -> s.is(ModBlocks.TESSERACT_CRADLE.get())).count() == 1,
                "one cradle");
        check(passes(whole.at(at.north())) && !passes(whole.at(at.below())), "it can be reached on the roof");
        // the core's ladder goes on from the lobby to the roof across the three parts
        for (int y = TOWER_LADDER_FROM; y <= StoneSitePlans.Tower.CRADLE.getY(); y++) {
            BlockState s = whole.at(inTower(TOWER_LADDER).atY(y));
            check(s != null && s.is(Blocks.LADDER), "the ladder at layer " + y + ": " + s);
        }
        // the floors meet: each part's west wall stands on the one below
        for (int y : new int[]{31, 32, 63, 64}) {
            check(!passes(whole.at(5, y, 16)), "the west wall runs on through the joint at " + y);
        }
        List<String> tables = chestTables(whole);
        for (String t : List.of("lab", "lounge", "armory")) {
            check(count(tables, "enhanced:chests/avengers_tower/" + t) >= 1, "a chest " + t + ": " + tables);
        }
        check(tables.stream().allMatch(t -> t.startsWith("enhanced:chests/avengers_tower/")),
                "all its chests: " + tables);
        check(whole.data().values().stream().anyMatch(d -> d.toString().contains("sign.enhanced.avengers_tower")),
                "the sign with the tower's name");
        helper.succeed();
    }

    /**
     * 3.6.1: a room for each of the first six Avengers, its door with the hero's name over it, and Captain America's
     * is a closet: a bed and room to stand beside it, no more.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theTowerHasARoomForEachOfTheSixAndCapsIsTiny(GameTestHelper helper) {
        Raw whole = tower(helper.getLevel().getServer());
        Map<BlockPos, String> signs = new HashMap<>();
        whole.data().forEach((p, d) -> {
            if (d.contains("front_text")) {
                ListTag lines = d.getCompound("front_text").getList("messages", Tag.TAG_STRING);
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    text.append(lines.getString(i));
                }
                signs.put(p, text.toString());
            }
        });
        for (String name : List.of("IRON MAN", "Cap's room", "THOR", "HULK", "BLACK WIDOW", "HAWKEYE")) {
            BlockPos sign = signs.entrySet().stream().filter(e -> e.getValue().contains(name)).map(Map.Entry::getKey)
                    .findFirst().orElse(null);
            check(sign != null, "a sign with " + name + ": " + signs.values());
            boolean door = false;
            for (BlockPos p : BlockPos.betweenClosed(sign.offset(-2, -3, -2), sign.offset(2, 0, 2))) {
                door |= whole.at(p) != null && whole.at(p).getBlock() instanceof DoorBlock;
            }
            check(door, name + "'s sign is by a door: " + sign);
        }
        // Cap's room: what can be walked into from its middle without opening its door
        Set<BlockPos> closet = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(TOWER_ROOMS.get("Captain America")));
        boolean bed = false;
        while (!queue.isEmpty() && closet.size() < 50) {
            BlockPos p = queue.poll();
            if (!closet.add(p)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockState n = whole.at(p.relative(d));
                bed |= n != null && n.is(BlockTags.BEDS);
                if (passes(n) && !closet.contains(p.relative(d))) {
                    queue.add(p.relative(d));
                }
            }
        }
        check(closet.size() <= 8 && bed, "Cap's room is a closet with a bed (2 x 2 x 2 at most): " + closet.size());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theEndVaultsChestIsOnItsDais(GameTestHelper helper) {
        Raw t = raw(helper.getLevel().getServer(), StoneSitePlans.EndTunnel.TEMPLATE);
        BlockPos chest = StoneSitePlans.EndTunnel.VAULT_CHEST;
        check(t.at(chest) != null && t.at(chest).is(Blocks.CHEST)
                        && t.data().get(chest).getString("LootTable").equals("enhanced:chests/end_tunnel/vault"),
                "the vault chest at " + chest + ": " + t.at(chest));
        check(t.at(chest.above()) == null || t.at(chest.above()).isAir(), "nothing over it: it opens");
        check(StoneSitePlans.EndTunnel.VAULT.isInside(chest), "inside the vault");
        List<String> tables = chestTables(t);
        check(count(tables, "enhanced:chests/end_tunnel/ruin") == 2 && tables.size() == 3, "two ruins: " + tables);
        check(t.entities().stream().filter(e -> e.getString("id").equals("minecraft:shulker")).count() == 1
                && t.entities().size() == 1, "one guard: a shulker");
        // the vault's walls are purpur and obsidian, a ring of crying obsidian in its floor
        BoundingBox v = StoneSitePlans.EndTunnel.VAULT;
        check(t.at(v.minX(), v.minY() + 3, v.getCenter().getZ() - 3).is(Blocks.OBSIDIAN), "obsidian walls");
        check(t.blocks().values().stream().filter(s -> s.is(Blocks.CRYING_OBSIDIAN)).count() >= 12, "the ring");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theVormirCliffsPointsHold(GameTestHelper helper) {
        Raw t = raw(helper.getLevel().getServer(), StoneSitePlans.Vormir.TEMPLATE);
        BoundingBox edge = StoneSitePlans.Vormir.EDGE_BOX;
        BoundingBox pit = StoneSitePlans.Vormir.PIT_BOX;
        int floor = StoneSitePlans.Vormir.SUMMIT.getY();
        // the edge: a floor under the strip, the lip of one slab at the chasm's side, room to stand behind it
        for (int x = edge.minX(); x <= edge.maxX(); x++) {
            check(!passes(t.at(x, floor, edge.minZ())) && !passes(t.at(x, floor, edge.maxZ())),
                    "the edge's floor at " + x);
            BlockState lip = t.at(x, floor + 1, edge.minZ());
            check(lip != null && lip.getBlock() instanceof SlabBlock, "the lip at " + x + ": " + lip);
            check(passes(t.at(x, floor + 1, edge.maxZ())) && passes(t.at(x, floor + 2, edge.maxZ())),
                    "room to stand at the edge at " + x);
        }
        // the chasm: air all the way down from the summit to the pit's floor, inside PIT_BOX
        for (int x = pit.minX(); x <= pit.maxX(); x++) {
            for (int z = pit.minZ(); z <= pit.maxZ(); z++) {
                BlockState bottom = t.at(x, StoneSitePlans.Vormir.PIT_FLOOR, z);
                check(bottom != null && bottom.is(Blocks.SOUL_SOIL),
                        "the pit's floor at " + x + ", " + z + ": " + bottom);
                for (int y = StoneSitePlans.Vormir.PIT_FLOOR + 2; y <= floor; y++) {
                    check(passes(t.at(x, y, z)), "the chasm is open at " + x + ", " + y + ", " + z);
                }
            }
        }
        check(pit.minY() <= StoneSitePlans.Vormir.PIT_FLOOR + 1 && pit.maxY() >= floor - 10, "PIT_BOX spans the fall");
        // the summit's other sides are walled: nobody falls into the chasm but over the lip
        for (int z = pit.minZ(); z <= pit.maxZ(); z++) {
            check(!passes(t.at(pit.minX() - 1, floor + 1, z)) && !passes(t.at(pit.maxX() + 1, floor + 1, z)),
                    "the chasm's side walls at " + z);
        }
        // the pool, the keeper's place, the pit's chest, the keeper
        BlockPos pool = StoneSitePlans.Vormir.POOL;
        check(passes(t.at(pool)) && t.at(pool.below()).is(Blocks.ORANGE_STAINED_GLASS)
                && t.at(pool.below(2)).is(Blocks.SHROOMLIGHT), "the pool: orange glass over shroomlight");
        BlockPos home = StoneSitePlans.Vormir.STONEKEEPER_HOME;
        check(passes(t.at(home)) && passes(t.at(home.above())) && !passes(t.at(home.below())), "the keeper's place");
        // 3.6.1: the edge's box holds feet standing on the lip (a top slab), and the held stone's pedestal has a place
        check(edge.maxY() >= floor + 2, "feet on the lip's top are on the edge");
        BlockPos pedestal = StoneSitePlans.Vormir.PEDESTAL;
        check(t.at(pedestal) == null || t.at(pedestal).isAir(), "the pedestal's place is free: " + t.at(pedestal));
        check(!passes(t.at(pedestal.below())) && Math.abs(pedestal.getX() - pool.getX()) <= 3
                && Math.abs(pedestal.getZ() - pool.getZ()) <= 3, "on the summit's floor, by the pool");
        check(t.entities().stream().filter(e -> e.getString("id").equals("enhanced:stonekeeper")).count() == 1,
                "the Stonekeeper stands there");
        check(count(chestTables(t), "enhanced:chests/vormir/pit") == 1, "the pit's chest");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ loot

    private static List<ItemStack> roll(GameTestHelper helper, String table, RandomSource random) {
        ServerLevel level = helper.getLevel();
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Enhanced.id("chests/" + table)));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
                helper.absoluteVec(new Vec3(10.5, 2, 10.5))).create(LootContextParamSets.CHEST);
        return loot.getRandomItems(params, random);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSitesLootIsThePlans(GameTestHelper helper) {
        RandomSource random = RandomSource.create(2016L);
        int rings = 0;
        int books = 0;
        int cures = 0;
        int totems = 0;
        int shells = 0;
        for (int i = 0; i < 300; i++) {
            for (String table : List.of("sanctum/library", "sanctum/relics", "sanctum/ward_room",
                    "avengers_tower/lab", "avengers_tower/lounge", "avengers_tower/armory", "end_tunnel/ruin",
                    "vormir/pit")) {
                List<ItemStack> stacks = roll(helper, table, random);
                check(!stacks.isEmpty(), table + " has loot");
                check(stacks.stream().noneMatch(s -> s.is(ModTags.INFINITY_STONES) || s.is(ModItems.THE_ORB.get())),
                        table + " never holds a stone: the sites' stones are their pedestals' and the vault's");
                rings += count(stacks, ModItems.SLING_RING.get());
                books += count(stacks, ModItems.BOOK_OF_THE_VISHANTI.get());
                cures += count(stacks, ModItems.MUTANT_CURE.get());
                totems += count(stacks, net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
            }
            List<ItemStack> vault = roll(helper, "end_tunnel/vault", random);
            check(count(vault, ModItems.THE_ORB.get()) + count(vault, net.minecraft.world.item.Items.PAPER) == 1,
                    "the vault gives the Orb (or the note that it is gone), once: " + vault);
            check(vault.size() >= 4, "and its treasure: " + vault);
            shells += count(vault, net.minecraft.world.item.Items.SHULKER_SHELL);
        }
        check(rings > 50 && rings < 130, "a Sling Ring in about 30% of the libraries: " + rings);
        check(books > 20 && books < 80, "the Book of the Vishanti in about 15%: " + books);
        check(cures > 3 && cures < 35, "the Mutant Cure in about 5% of the labs: " + cures);
        check(totems > 12 && totems < 55, "a totem in about 10% of the pits: " + totems);
        check(shells > 50 && shells < 130, "a shulker shell in about 30% of the vaults: " + shells);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ finding a place

    /** The Nether site: the first floor with room over it, low enough for the whole cliff under the roof. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theNetherSiteStandsOnlyInATallCavern(GameTestHelper helper) {
        BlockState rack = Blocks.NETHERRACK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        int height = StoneSitePlans.Vormir.SIZE.getY();
        // netherrack to 40, air above to the roof at 120
        check(NetherSiteStructure.floor(y -> y <= 40 || y >= 127 ? rack : air, 33, 120, 8, height).orElse(-1) == 40,
                "a cavern floor at 40 takes the cliff");
        // the floor at 50: the cliff would pass the roof
        check(NetherSiteStructure.floor(y -> y <= 50 || y >= 127 ? rack : air, 33, 120, 8, height).isEmpty(),
                "a floor at 50 is too high for 74 layers");
        // a low cave first (5 of air), then the real floor
        check(NetherSiteStructure.floor(y -> y <= 34 || (y >= 40 && y <= 42) || y >= 127 ? rack : air, 33, 120, 8,
                height).orElse(-1) == 42, "a low cave is passed over: " + NetherSiteStructure.floor(
                y -> y <= 34 || (y >= 40 && y <= 42) || y >= 127 ? rack : air, 33, 120, 8, height));
        // lava is no floor, and no air
        check(NetherSiteStructure.floor(y -> y <= 36 ? rack : y <= 38 ? lava : y >= 127 ? rack : air, 33, 120, 8,
                height).isEmpty(), "a lava lake over the rock is no floor");
        // a cavern 20 high over the floor: with 20 of headroom it is taken, with 24 the next floor up is looked for
        check(NetherSiteStructure.floor(y -> y <= 40 || y >= 61 ? rack : air, 33, 120, 20, height).orElse(-1) == 40,
                "twenty of air over the floor is enough for headroom 20");
        check(NetherSiteStructure.floor(y -> y <= 40 || y >= 61 ? rack : air, 33, 120, 24, height).isEmpty(),
                "not for 24");
        // the side room: open air round the site, no fluid
        check(NetherSiteStructure.open(y -> y <= 41 ? rack : air, 43, 52), "open round about");
        check(!NetherSiteStructure.open(y -> y <= 41 || y >= 50 ? rack : air, 43, 52), "a wall of rock round about");
        check(!NetherSiteStructure.open(y -> y <= 41 ? rack : y <= 44 ? lava : air, 43, 52), "a lava lake round about");
        helper.succeed();
    }

    /** The carved site: the island's edge, and the island under the vault. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCarvedSiteFindsTheEdgeAndRefusesATinyIsland(GameTestHelper helper) {
        check(CarvedSiteStructure.edge(d -> d <= 10 ? 70 : -1, 70, 24, 8).orElse(-1) == 10,
                "the edge is the last column before the void");
        check(CarvedSiteStructure.edge(d -> 70 - d / 4, 70, 24, 8).isEmpty(), "a gentle slope has no edge within 24");
        check(CarvedSiteStructure.edge(d -> d <= 4 ? 70 : 60, 70, 24, 8).orElse(-1) == 4, "a cliff of ten is an edge");
        BoundingBox vault = StoneSitePlans.EndTunnel.VAULT;
        BlockState stone = Blocks.END_STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // a big island: all End stone from 0 to 70
        double big = CarvedSiteStructure.islandShare(vault, p -> p.offset(1000, 50, 1000),
                (x, z) -> y -> y <= 70 ? stone : air);
        check(big >= 0.9, "a big island holds the vault: " + big);
        // a tiny island: stone only within 3 blocks of one point
        double tiny = CarvedSiteStructure.islandShare(vault, p -> p.offset(1000, 50, 1000),
                (x, z) -> y -> Math.abs(x - 1007) + Math.abs(z - 1040) <= 3 && y <= 70 ? stone : air);
        check(tiny < 0.9, "a tiny island does not: " + tiny);
        // an island too thin under the vault (stone only from 54 up)
        double thin = CarvedSiteStructure.islandShare(vault, p -> p.offset(1000, 50, 1000),
                (x, z) -> y -> y >= 54 && y <= 70 ? stone : air);
        check(thin < 0.9, "nor a thin one: " + thin);
        helper.succeed();
    }

    /** The cradle: an empty hand takes the Tesseract (once: the cradle is empty after), a Tesseract puts it back. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCradleGivesItsTesseractOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(10, 2, 10));
        level.setBlock(at, ModBlocks.TESSERACT_CRADLE.get().defaultBlockState().setValue(TesseractCradleBlock.FULL,
                true), 3);
        net.minecraft.server.level.ServerPlayer player = f.player(10.5, 2, 9.5);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false);
        level.getBlockState(at).useWithoutItem(level, player, hit);
        check(player.getMainHandItem().is(ModItems.TESSERACT.get()), "the Tesseract is in his hand");
        check(!level.getBlockState(at).getValue(TesseractCradleBlock.FULL), "the cradle is empty");
        level.getBlockState(at).useWithoutItem(level, player, hit);
        check(player.getInventory().countItem(ModItems.TESSERACT.get()) == 1, "an empty cradle gives nothing more");
        level.getBlockState(at).useItemOn(player.getMainHandItem(), level, player,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        check(level.getBlockState(at).getValue(TesseractCradleBlock.FULL) && player.getMainHandItem().isEmpty(),
                "held to the empty cradle, the Tesseract is set back in it");
        check(ModBlocks.TESSERACT_CRADLE.get().defaultBlockState().getDestroySpeed(level, at) < 0,
                "the cradle cannot be broken");
        helper.succeed();
    }

    /** The seal: lava beside or over a template's air becomes stone; lava under it, or beside its rock, is left. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSealWallsTheWorldsLavaOffTheSitesAir(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos air = helper.absolutePos(new BlockPos(10, 4, 10));
        BlockPos rock = helper.absolutePos(new BlockPos(14, 4, 10));
        for (BlockPos p : List.of(air.east(), air.above(), air.below(), rock.east())) {
            level.setBlock(p, Blocks.LAVA.defaultBlockState(), 2);
        }
        List<StructureTemplate.StructureBlockInfo> blocks = List.of(
                new StructureTemplate.StructureBlockInfo(air, Blocks.AIR.defaultBlockState(), null),
                new StructureTemplate.StructureBlockInfo(rock, Blocks.BLACKSTONE.defaultBlockState(), null));
        StructurePlaceSettings settings = new StructurePlaceSettings().setBoundingBox(
                BoundingBox.fromCorners(air.offset(-3, -3, -3), rock.offset(3, 3, 3)));
        List<StructureTemplate.StructureBlockInfo> out = new SealProcessor(Blocks.BLACKSTONE.defaultBlockState())
                .finalizeProcessing(level, BlockPos.ZERO, BlockPos.ZERO, blocks, blocks, settings);
        Set<BlockPos> sealed = new HashSet<>();
        out.stream().skip(2).forEach(i -> sealed.add(i.pos()));
        check(sealed.equals(Set.of(air.east(), air.above())), "the lava beside and over the air is sealed, no more: "
                + sealed);
        for (BlockPos p : List.of(air.east(), air.above(), air.below(), rock.east())) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ building

    /** The ms each chunk took to build, and the worst. */
    record Timing(int chunks, double averageMs, double worstMs) {
    }

    /**
     * Builds a piece chunk by chunk, as the world's generation does, timing each chunk's part. The time is the server
     * thread's own CPU time (where the JVM measures it): a wall clock also counted the garbage collector's pauses and
     * the other threads of a loaded machine (3.6 integration: one full run had a chunk of the End tunnel at 557 ms
     * against its usual 20-60).
     */
    static Timing build(ServerLevel level, List<? extends StructurePiece> pieces) {
        BoundingBox all = null;
        for (StructurePiece p : pieces) {
            all = all == null ? p.getBoundingBox() : BoundingBox.encapsulatingBoxes(List.of(all, p.getBoundingBox()))
                    .orElseThrow();
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        double total = 0;
        double worst = 0;
        int n = 0;
        for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
            for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                level.getChunk(cx, cz);
                BoundingBox box = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ());
                long start = cpuNanos();
                for (StructurePiece piece : pieces) {
                    if (piece.getBoundingBox().intersects(box)) {
                        piece.postProcess(level, level.structureManager(), generator,
                                RandomSource.create(cx * 31L + cz), box, chunk, all.getCenter());
                    }
                }
                double ms = (cpuNanos() - start) / 1e6;
                total += ms;
                worst = Math.max(worst, ms);
                n++;
            }
        }
        return new Timing(n, total / n, worst);
    }

    /** The current thread's CPU time (ns), or the wall clock where the JVM does not measure it. */
    static long cpuNanos() {
        java.lang.management.ThreadMXBean threads = java.lang.management.ManagementFactory.getThreadMXBean();
        return threads.isCurrentThreadCpuTimeSupported() ? threads.getCurrentThreadCpuTime() : System.nanoTime();
    }

    static void withinBudget(String site, Timing t) {
        Enhanced.LOGGER.info("[stone_sites] {} built in {} chunks: {} ms a chunk on average, the worst {} ms", site,
                t.chunks(), String.format(java.util.Locale.ROOT, "%.2f", t.averageMs()),
                String.format(java.util.Locale.ROOT, "%.2f", t.worstMs()));
        check(t.worstMs() <= CHUNK_WORST_MS && t.averageMs() <= CHUNK_AVERAGE_MS, site + " builds within its chunk "
                + "budget (" + CHUNK_AVERAGE_MS + " ms a chunk, " + CHUNK_WORST_MS + " the worst): " + t);
    }

    static Holder<StructureProcessorList> processors(ServerLevel level, String name) {
        return level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, Enhanced.id(name)));
    }

    static int ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    /** A Sanctum turned a quarter round: the pedestal where the turn puts it, the chests, the door. */
    @GameTest(template = ARENA, batch = GEN, timeoutTicks = 400)
    public static void aSanctumIsBuiltTurnedWithItsTimeStone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager manager = level.getStructureManager();
        int x = 300_008;
        int z = 100_008;
        BlockPos center = new BlockPos(x, ground(level, x, z), z);
        TemplateSitePiece piece = new TemplateSitePiece(manager, StoneSitePlans.Sanctum.TEMPLATE,
                processors(level, "sanctum"), Rotation.CLOCKWISE_90, center, StoneSitePlans.Sanctum.SIZE,
                StoneSitePlans.Sanctum.FLOOR);
        Timing t = build(level, List.of(piece));
        BlockPos pedestal = SitePoints.toWorld(piece, StoneSitePlans.Sanctum.PEDESTAL);
        BlockState there = level.getBlockState(pedestal);
        check(there.is(ModBlocks.STONE_PEDESTAL.get())
                && there.getValue(StonePedestalBlock.STONE) == StonePedestalBlock.Stone.TIME,
                "the pedestal is where the turn puts it: " + pedestal + " " + there);
        check(pedestal.getY() == center.getY() + 13, "thirteen over the ground");
        BlockPos door = SitePoints.toWorld(piece, new BlockPos(11, StoneSitePlans.Sanctum.FLOOR + 1, 15));
        check(level.getBlockState(door).getBlock() instanceof DoorBlock,
                "the front door: " + level.getBlockState(door));
        check(level.getBlockState(door).getValue(DoorBlock.FACING) == Direction.EAST,
                "turned with the house (it faced north): " + level.getBlockState(door));
        BlockPos chest = SitePoints.toWorld(piece, new BlockPos(2, StoneSitePlans.Sanctum.FLOOR + 1, 13));
        check(level.getBlockEntity(chest) instanceof ChestBlockEntity c && c.saveWithoutMetadata(level.registryAccess())
                .getString("LootTable").equals("enhanced:chests/sanctum/relics"), "the relics chest: "
                + level.getBlockState(chest));
        withinBudget("the Sanctum", t);
        helper.succeed();
    }

    /** A tower built for real: its three parts meet, the cradle is where the tower's frame says. */
    @GameTest(template = ARENA, batch = GEN, timeoutTicks = 400)
    public static void aTowerIsBuiltWithItsPartsAlignedAndItsTesseract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TowerSiteStructure tower = (TowerSiteStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(Enhanced.id("avengers_tower"));
        int x = 300_008;
        int z = 100_408;
        BlockPos center = new BlockPos(x, ground(level, x, z), z);
        List<TemplateSitePiece> pieces = tower.pieces(level.getStructureManager(), Rotation.COUNTERCLOCKWISE_90,
                center);
        check(pieces.size() == 3, "three parts");
        Timing t = build(level, pieces);
        BlockPos byTop = SitePoints.toWorld(pieces.get(2), StoneSitePlans.Tower.CRADLE_IN_TOP);
        BlockPos byBase = SitePoints.toWorld(pieces.get(0), StoneSitePlans.Tower.CRADLE);
        check(byTop.equals(byBase), "the top part lies where the tower's frame says: " + byTop + " " + byBase);
        BlockState cradle = level.getBlockState(byTop);
        check(cradle.is(ModBlocks.TESSERACT_CRADLE.get()) && cradle.getValue(TesseractCradleBlock.FULL),
                "the Tesseract's cradle on the roof: " + cradle);
        check(byTop.getY() == center.getY() - StoneSitePlans.Tower.FLOOR + StoneSitePlans.Tower.CRADLE.getY(),
                StoneSitePlans.Tower.CRADLE.getY() + " over the base's layer 0: " + byTop);
        for (int y = TOWER_LADDER_FROM; y <= StoneSitePlans.Tower.CRADLE.getY(); y++) {
            BlockPos p = SitePoints.toWorld(pieces.get(0), TOWER_LADDER.atY(y));
            check(level.getBlockState(p).is(Blocks.LADDER),
                    "the ladder goes on at " + y + ": " + level.getBlockState(p));
        }
        withinBudget("Avengers Tower", t);
        helper.succeed();
    }

    /** The Vormir Cliff, unturned: every fixed point is its piece's corner plus the plan's offset. */
    @GameTest(template = ARENA, batch = GEN, timeoutTicks = 400)
    public static void aVormirCliffIsBuiltAtThePlansOffsets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        NetherSiteStructure cliff = (NetherSiteStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(Enhanced.id("vormir_cliff"));
        int x = 300_008;
        int z = 100_808;
        BlockPos center = new BlockPos(x, ground(level, x, z) + 1, z);
        TemplateSitePiece piece = cliff.piece(level.getStructureManager(), center, StoneSitePlans.Vormir.SIZE);
        Timing t = build(level, List.of(piece));
        BlockPos corner = new BlockPos(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(),
                piece.getBoundingBox().minZ());
        check(piece.getRotation() == Rotation.NONE && corner.equals(piece.templatePosition()), "unturned");
        check(level.getBlockState(corner.offset(16, StoneSitePlans.Vormir.PIT_FLOOR, 16)).is(Blocks.SOUL_SOIL),
                "the pit's floor");
        BlockPos pool = corner.offset(StoneSitePlans.Vormir.POOL);
        check(level.getBlockState(pool).isAir() && level.getBlockState(pool.below()).is(Blocks.ORANGE_STAINED_GLASS),
                "the pool");
        BoundingBox edge = StoneSitePlans.Vormir.EDGE_BOX;
        check(level.getBlockState(corner.offset(16, edge.minY() + 1, edge.minZ())).getBlock() instanceof SlabBlock,
                "the lip");
        check(level.getBlockState(corner.offset(16, edge.minY() + 1, edge.minZ())).getValue(SlabBlock.TYPE)
                == SlabType.TOP, "a slab at knee height");
        for (int y = StoneSitePlans.Vormir.PIT_FLOOR + 2; y <= StoneSitePlans.Vormir.SUMMIT.getY(); y++) {
            check(level.getBlockState(corner.offset(16, y, 16)).isAir(), "the chasm at " + y);
        }
        check(level.getBlockState(corner.offset(StoneSitePlans.Vormir.STONEKEEPER_HOME).below()).isSolid(),
                "the keeper's floor");
        withinBudget("the Vormir Cliff", t);
        helper.succeed();
    }

    /** The End tunnel turned half round: the vault chest where the turn puts it, the shulker on guard. */
    @GameTest(template = ARENA, batch = GEN, timeoutTicks = 400)
    public static void anEndTunnelIsBuiltTurnedWithItsVault(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CarvedSiteStructure tunnel = (CarvedSiteStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(Enhanced.id("end_tunnel"));
        int x = 300_008;
        int z = 101_208;
        BlockPos center = new BlockPos(x, ground(level, x, z) + 20, z);
        TemplateSitePiece piece = tunnel.piece(level.getStructureManager(), Rotation.CLOCKWISE_180, center,
                StoneSitePlans.EndTunnel.SIZE);
        Timing t = build(level, List.of(piece));
        BlockPos chest = SitePoints.toWorld(piece, StoneSitePlans.EndTunnel.VAULT_CHEST);
        check(level.getBlockEntity(chest) instanceof ChestBlockEntity c && c.saveWithoutMetadata(level.registryAccess())
                .getString("LootTable").equals("enhanced:chests/end_tunnel/vault"), "the vault chest: " + chest);
        BlockPos entrance = SitePoints.toWorld(piece, new BlockPos(7, 8, 0));
        check(entrance.getY() == center.getY() && entrance.getZ() > chest.getZ(),
                "turned half round, the entrance on the surface layer faces south: " + entrance + " " + chest);
        withinBudget("the End tunnel", t);
        helper.succeed();
    }

    /**
     * Each kind of site finds its places as /locate does: in the regions of its structure set, at the chunk the spread
     * picks, the structure is asked where it stands (the overworld's two on the test world's flat ground; the Vormir
     * Cliff in the real Nether, the tunnel in the real End, at least one of many regions).
     */
    @GameTest(template = ARENA, batch = GEN, timeoutTicks = 1200)
    public static void eachSiteFindsItsPlacesAsLocateDoes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        record Kind(String structure, String set, ResourceKey<Level> dimension, int regions, int fromRegion) {
        }
        for (Kind kind : List.of(new Kind("sanctum", "sanctums", Level.OVERWORLD, 4, 2000),
                new Kind("avengers_tower", "avengers_towers", Level.OVERWORLD, 4, 2000),
                new Kind("vormir_cliff", "vormir_cliffs", Level.NETHER, 64, 3),
                new Kind("end_tunnel", "end_tunnels", Level.END, 64, 4))) {
            ServerLevel level = server.getLevel(kind.dimension());
            check(level != null, kind.dimension() + " exists in the test world");
            int found = found(level, kind.structure(), kind.set(), kind.fromRegion(), kind.regions());
            Enhanced.LOGGER.info("[stone_sites] {}: {} of {} regions in {}", kind.structure(), found, kind.regions(),
                    kind.dimension().location());
            check(found >= 1, kind.structure() + " stands somewhere in " + kind.regions() + " regions of "
                    + kind.dimension().location());
        }
        helper.succeed();
    }

    /** In how many of {@code regions} regions (a square from region {@code from}) the structure finds its place. */
    static int found(ServerLevel level, String structure, String setName, int from, int regions) {
        var access = level.registryAccess();
        Structure site = access.registryOrThrow(Registries.STRUCTURE).get(Enhanced.id(structure));
        StructureSet set = access.registryOrThrow(Registries.STRUCTURE_SET).get(Enhanced.id(setName));
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        int side = (int) Math.ceil(Math.sqrt(regions));
        int found = 0;
        for (int i = 0; i < regions; i++) {
            int rx = from + i % side;
            int rz = from + i / side;
            ChunkPos chunk = spread.getPotentialStructureChunk(level.getSeed(), rx * spread.spacing(),
                    rz * spread.spacing());
            StructureStart start = site.generate(access, generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level,
                    biome -> true);
            if (start.isValid()) {
                found++;
            }
        }
        return found;
    }

    /** {@code [stone_sites] sanctums = false}: a Sanctum finds no place in new chunks; true: it does again. */
    @GameTest(template = ARENA, batch = CFG, timeoutTicks = 400)
    public static void aSiteSwitchedOffFindsNoPlace(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        check(found(level, "sanctum", "sanctums", 2000, 2) >= 1, "with the switch on, the Sanctum stands");
        f.config(StoneSitesConfig.SANCTUMS, false);
        check(found(level, "sanctum", "sanctums", 2000, 2) == 0, "with sanctums = false, never");
        f.config(StoneSitesConfig.AVENGERS_TOWERS, false);
        check(found(level, "avengers_tower", "avengers_towers", 2000, 2) == 0, "with avengersTowers = false, never");
        f.config(StoneSitesConfig.SANCTUMS, true);
        check(found(level, "sanctum", "sanctums", 2000, 2) >= 1, "and on again, it does");
        helper.succeed();
    }

    private StoneSiteTests() {
    }
}
