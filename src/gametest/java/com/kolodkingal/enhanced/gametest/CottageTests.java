package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardTrades;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.cottage.CottageDialogue;
import com.kolodkingal.enhanced.cottage.CottageEvents;
import com.kolodkingal.enhanced.cottage.CottageGoals;
import com.kolodkingal.enhanced.cottage.CottageKeeper;
import com.kolodkingal.enhanced.cottage.CottageProgress;
import com.kolodkingal.enhanced.cottage.CottageTrades;
import com.kolodkingal.enhanced.cottage.Cottages;
import com.kolodkingal.enhanced.cottage.Scratchy;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.world.CottagePlan;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Agatha's Cottage (3.7, lane L6; the plan's 9 and 13): the template's points, the keeper (one of each per cottage,
 * again after {@code /kill}, Scratchy, the Road door), one of each with a player who is Agatha (her NPC away and back,
 * Nicky's
 * "Mom!", the lines that name her), the dialogue table in both languages, the trades, Nicky's gift and Rio's blessing
 * once a day, Señor Scratchy's run-off and quest, residents never killed by a player, and their day by the hour.
 * Batches {@code cottage}, {@code cottage_keeper} and {@code cottage_one_each} (a player who is Agatha is seen
 * server-wide). The keeper's cottages here are fake records drawn small inside the arena ({@link #cottage}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CottageTests {
    static final String BATCH = "cottage";
    static final String KEEPER = "cottage_keeper";
    static final String ONE_EACH = "cottage_one_each";
    private static final AtomicLong KEYS = new AtomicLong(-(1L << 60));

    private CottageTests() {
    }

    // ---- a cottage drawn small in the arena ----

    /**
     * A fake cottage in the arena (its record removed when the test ends): the table's four chairs, three beds, the
     * porch and the bench (stairs), the pot (a cauldron), the Road door, and the homes on the arena's floor.
     */
    static Cottages.Cottage cottage(TestKit.Fixture fx) {
        GameTestHelper h = fx.helper();
        ServerLevel level = fx.level();
        int y = (int) FLOOR;
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        java.util.function.BiFunction<Integer, Integer, BlockPos> at = (x, z) -> o.offset(x, y, z);
        List<BlockPos> seats = List.of(at.apply(6, 5), at.apply(9, 5), at.apply(6, 6), at.apply(9, 6));
        List<Direction> seatBacks = List.of(Direction.WEST, Direction.EAST, Direction.WEST, Direction.EAST);
        for (int i = 0; i < 4; i++) {
            level.setBlockAndUpdate(seats.get(i), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,
                    seatBacks.get(i)));
        }
        level.setBlockAndUpdate(at.apply(7, 5), Blocks.SPRUCE_SLAB.defaultBlockState());
        List<BlockPos> beds = List.of(at.apply(3, 15), at.apply(6, 15), at.apply(9, 15));
        for (BlockPos head : beds) {
            BlockState bed = Blocks.PURPLE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
            level.setBlock(head.south(), bed.setValue(BedBlock.PART, BedPart.FOOT), 18);
            level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 18);
        }
        List<BlockPos> porch = List.of(at.apply(12, 3), at.apply(14, 3));
        List<BlockPos> bench = List.of(at.apply(12, 18), at.apply(13, 18));
        for (BlockPos p : List.of(porch.get(0), porch.get(1), bench.get(0), bench.get(1))) {
            level.setBlockAndUpdate(p, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,
                    Direction.NORTH));
        }
        BlockPos pot = at.apply(2, 6);
        level.setBlockAndUpdate(pot, Blocks.WATER_CAULDRON.defaultBlockState());
        BlockPos door = at.apply(10, 10);
        level.setBlockAndUpdate(door, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState());
        Cottages.Points points = new Cottages.Points(at.apply(3, 3), at.apply(10, 3), at.apply(17, 3), at.apply(17, 17),
                door, at.apply(10, 1), pot, at.apply(3, 6), at.apply(3, 8), at.apply(17, 10), at.apply(14, 14),
                at.apply(17, 14), seats, beds, porch, bench, List.of(at.apply(16, 6), at.apply(18, 6)),
                new BoundingBox(o.getX(), o.getY(), o.getZ(), o.getX() + 20, o.getY() + 8, o.getZ() + 20));
        long key = KEYS.getAndDecrement();
        Cottages store = Cottages.get(level);
        Cottages.Cottage cottage = store.register(key, points);
        fx.onClose(() -> {
            for (Role role : Cottages.ROLES) {
                UUID id = cottage.resident(role);
                if (id != null && level.getEntity(id) != null) {
                    level.getEntity(id).discard();
                }
            }
            if (cottage.scratchy() != null && level.getEntity(cottage.scratchy()) != null) {
                level.getEntity(cottage.scratchy()).discard();
            }
            WitchesRoad.removeDoor(level, door);
            store.remove(key);
        });
        return cottage;
    }

    /** Whether the entities round a cottage's homes are loaded (the keeper waits for that). */
    static boolean loaded(ServerLevel level, Cottages.Cottage cottage) {
        Cottages.Points points = cottage.points();
        for (BlockPos p : List.of(points.agathaHome(), points.nickyHome(), points.rioHome(), points.hutch())) {
            ChunkPos c = new ChunkPos(p);
            for (int dx = -CottageKeeper.AREA; dx <= CottageKeeper.AREA; dx++) {
                for (int dz = -CottageKeeper.AREA; dz <= CottageKeeper.AREA; dz++) {
                    if (!level.areEntitiesLoaded(ChunkPos.asLong(c.x + dx, c.z + dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** A stray resident (no cottage) standing in the arena at (x, z). */
    static AsgardianEntity resident(TestKit.Fixture fx, Role role, double x, double z) {
        AsgardianEntity npc = ModEntities.COTTAGE_RESIDENT.get().create(fx.level());
        npc.setRole(role, 0);
        Vec3 at = fx.helper().absoluteVec(new Vec3(x, FLOOR, z));
        npc.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        check(fx.level().addFreshEntity(npc), "the resident joins");
        fx.onClose(npc::discard);
        return npc;
    }

    static boolean saidInChat(ServerPlayer player, String key) {
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay()
                    && mentions(chat.content(), key)) {
                return true;
            }
        }
        return false;
    }

    static boolean mentions(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents text && text.getKey().equals(key)) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (mentions(sibling, key)) {
                return true;
            }
        }
        return false;
    }

    // ---- the template ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theTemplateHoldsEveryPointAndChest(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation file = Enhanced.id("structure/" + CottagePlan.TEMPLATE.getPath() + ".nbt");
        CompoundTag root;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(file).open()) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, String> blocks = new HashMap<>();
        List<String> loot = new ArrayList<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            blocks.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    palette.getCompound(b.getInt("state")).getString("Name"));
            if (b.getCompound("nbt").contains("LootTable")) {
                loot.add(b.getCompound("nbt").getString("LootTable"));
            }
        }
        check("minecraft:water_cauldron".equals(blocks.get(CottagePlan.POT)), "the pot");
        for (BlockPos p : List.of(CottagePlan.STOVE, CottagePlan.HERBS, CottagePlan.GREENHOUSE, CottagePlan.YARD,
                CottagePlan.HUTCH_GATE, CottagePlan.GARDEN.get(0), CottagePlan.GARDEN.get(1), CottagePlan.GARDEN.get(2),
                CottagePlan.GARDEN.get(3))) {
            String below = blocks.getOrDefault(p.below(), "minecraft:air");
            check(blocks.getOrDefault(p, "minecraft:air").equals("minecraft:air") || blocks.get(p).contains("grass"),
                    "one stands at " + p + ": " + blocks.get(p));
            check(!below.equals("minecraft:air"), "on something at " + p);
        }
        for (BlockPos p : List.of(CottagePlan.PORCH_CHAIRS.get(0), CottagePlan.PORCH_CHAIRS.get(1),
                CottagePlan.BENCH.get(0), CottagePlan.BENCH.get(1))) {
            check(blocks.getOrDefault(p, "").endsWith("_stairs"), "a seat at " + p);
        }
        for (String table : List.of("kitchen", "cellar", "nicky", "study")) {
            check(loot.contains("enhanced:chests/agatha_cottage/" + table), "the " + table + " chest");
        }
        check(server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, Enhanced.id("chests/agatha_cottage/cellar")))
                != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "the cellar's loot loads");
        check(blocks.values().stream().noneMatch(n -> n.contains("darkhold")), "the Darkhold is never here");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = KEEPER, timeoutTicks = 200)
    public static void aRealCottageMapsItsPointsInEveryTurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).atY(220);
        List<BoundingBox> placed = new ArrayList<>();
        try {
            int i = 0;
            for (Rotation rotation : Rotation.values()) {
                BlockPos center = base.offset(40 * i++, 0, 0);
                TemplateSitePiece piece = new TemplateSitePiece(level.getStructureManager(), CottagePlan.TEMPLATE,
                        Holder.direct(new StructureProcessorList(List.of())), rotation, center, CottagePlan.SIZE,
                        CottagePlan.FLOOR);
                StructureTemplate template = level.getStructureManager().getOrCreate(CottagePlan.TEMPLATE);
                template.placeInWorld(level, piece.templatePosition(), piece.templatePosition(), piece.placeSettings(),
                        level.getRandom(), 2);
                placed.add(piece.getBoundingBox());
                Cottages.Points p = CottageKeeper.points(piece);
                check(WitchesRoad.isDoor(level.getBlockState(p.roadDoor())), rotation + ": the Road door");
                check(level.getBlockState(p.gate()).getBlock()
                        instanceof net.minecraft.world.level.block.FenceGateBlock,
                        rotation + ": the gate");
                check(level.getBlockState(p.pot()).is(Blocks.WATER_CAULDRON), rotation + ": the pot");
                for (BlockPos seat : p.seats()) {
                    check(level.getBlockState(seat).getBlock() instanceof StairBlock, rotation + ": a chair " + seat);
                }
                for (BlockPos bed : p.beds()) {
                    BlockState state = level.getBlockState(bed);
                    check(state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD,
                            rotation + ": a bed's head " + bed);
                }
                for (Role role : Cottages.ROLES) {
                    check(level.getBlockState(p.home(role)).getCollisionShape(level, p.home(role)).isEmpty()
                            && !level.getBlockState(p.home(role).below()).isAir(), rotation + ": " + role + "'s home");
                }
                check(p.box().isInside(p.hutch()) && p.box().isInside(p.gate()), rotation + ": the box holds them");
            }
        } finally {
            for (BoundingBox box : placed) {
                for (BlockPos q : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                        box.maxZ())) {
                    level.setBlock(q, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                for (Entity e : level.getEntitiesOfClass(Entity.class, net.minecraft.world.phys.AABB.of(box).inflate(1),
                        e -> !(e instanceof ServerPlayer))) {
                    e.discard();
                }
            }
        }
        helper.succeed();
    }

    /**
     * Agatha's study chest (Eilon's wish): the Darkhold and 3 to 5 enchanted books from a good pool at levels 2 to 4.
     * The Darkhold has no one-per-world rule (it is crafted, and every Agatha is given one), so every chest holds it.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void agathasStudyChestHoldsTheDarkholdAndGoodBooks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        check(server.getRecipeManager().byKey(Enhanced.id("darkhold")).isPresent(),
                "the Darkhold is crafted: no one-per-world rule to follow");
        net.minecraft.world.level.storage.loot.LootTable table = server.reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        Enhanced.id("chests/agatha_cottage/study")));
        net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams
                .Builder(level).withParameter(
                        net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        Set<String> pool = Set.of("minecraft:protection", "minecraft:unbreaking", "minecraft:sharpness",
                "minecraft:feather_falling", "minecraft:efficiency", "minecraft:fire_protection");
        for (long seed = 1; seed <= 12; seed++) {
            List<net.minecraft.world.item.ItemStack> items = table.getRandomItems(params, seed);
            long darkholds = items.stream().filter(s -> s.is(com.kolodkingal.enhanced.registry.ModItems.DARKHOLD.get()))
                    .mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
            check(darkholds == 1, "one Darkhold in every study chest: " + darkholds);
            int books = 0;
            for (net.minecraft.world.item.ItemStack stack : items) {
                if (!stack.is(Items.ENCHANTED_BOOK)) {
                    continue;
                }
                books++;
                var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
                check(stored != null && stored.size() == 1, "one enchantment a book");
                for (var entry : stored.entrySet()) {
                    String id = entry.getKey().unwrapKey().map(k -> k.location().toString()).orElse("?");
                    check(pool.contains(id), "a book from the good pool: " + id);
                    check(entry.getIntValue() >= 2 && entry.getIntValue() <= 4, id + " at level 2 to 4: "
                            + entry.getIntValue());
                }
            }
            check(books >= 3 && books <= 5, "3 to 5 enchanted books: " + books);
        }
        helper.succeed();
    }

    // ---- the keeper ----

    @GameTest(template = ARENA, batch = KEEPER, timeoutTicks = 120)
    public static void theKeeperMakesOneOfEachOnceAndAgainAfterAKill(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Cottages.Cottage cottage = cottage(fx);
        ServerLevel level = fx.level();
        fx.everyTick(100, tick -> {
            if (!loaded(level, cottage)) {
                return false;
            }
            List<Entity> made = CottageKeeper.keep(level, cottage);
            check(made.size() == 4, "Agatha, Nicky, Rio and Scratchy: " + made.size());
            for (Role role : Cottages.ROLES) {
                AsgardianEntity npc = CottageKeeper.resident(level, cottage, role);
                check(npc != null && npc.getType() == ModEntities.COTTAGE_RESIDENT.get() && npc.role() == role,
                        role + " lives here, a cottage resident");
                check(npc.slot().equals(Cottages.slot(cottage.key(), role)), role + "'s slot names the cottage");
            }
            Rabbit scratchy = Scratchy.find(level, cottage);
            check(scratchy != null && scratchy.getVariant() == Rabbit.Variant.BROWN && Scratchy.is(scratchy)
                    && scratchy.isPersistenceRequired(), "Señor Scratchy, brown, kept");
            check(CottageKeeper.keep(level, cottage).isEmpty(), "not twice");
            AsgardianEntity nicky = CottageKeeper.resident(level, cottage, Role.NICKY);
            UUID first = nicky.getUUID();
            AsgardianEntity copy = ModEntities.COTTAGE_RESIDENT.get().create(level);
            copy.setRole(Role.NICKY, 0);
            copy.setHome(Cottages.slot(cottage.key(), Role.NICKY), cottage.points().nickyHome(), 16);
            copy.moveTo(Vec3.atBottomCenterOf(cottage.points().nickyHome()));
            check(CottageEvents.refuses(level, copy) && !level.addFreshEntity(copy), "a second Nicky is refused");
            nicky.kill();
            check(!nicky.isAlive(), "/kill takes him (only the void and /kill do)");
            List<Entity> again = CottageKeeper.keep(level, cottage);
            AsgardianEntity back = CottageKeeper.resident(level, cottage, Role.NICKY);
            check(again.size() == 1 && back != null && !back.getUUID().equals(first), "Nicky is made again: " + again);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void residentsNeverDieToAPlayer(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity agatha = resident(fx, Role.AGATHA, 7.5, 5.5);
        AsgardianEntity nicky = resident(fx, Role.NICKY, 3.5, 5.5);
        float full = agatha.getHealth();
        check(!agatha.hurt(fx.level().damageSources().playerAttack(player), 50.0F), "a player's blow is refused");
        check(agatha.getHealth() == full && !agatha.isDown(), "she is whole");
        check(nicky.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) == 0.75,
                "Nicky is a kid (scale 0.75)");
        nicky.hurt(fx.level().damageSources().generic(), 500.0F);
        check(nicky.isAlive() && nicky.isDown(), "other harm only knocks him down: he rises later");
        check(!agatha.role().warrior() && !nicky.role().warrior(), "nobody fights");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = KEEPER, timeoutTicks = 120)
    public static void theirDayPutsEachInTheRightPlace(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Cottages.Cottage cottage = cottage(fx);
        ServerLevel level = fx.level();
        fx.everyTick(100, tick -> {
            if (!loaded(level, cottage)) {
                return false;
            }
            CottageKeeper.keep(level, cottage);
            Cottages.Points p = cottage.points();
            Map<Role, Set<CottageGoals.Activity>> morning = Map.of(
                    Role.AGATHA, Set.of(CottageGoals.Activity.HERBS, CottageGoals.Activity.STIR),
                    Role.NICKY, Set.of(CottageGoals.Activity.FEED),
                    Role.RIO, Set.of(CottageGoals.Activity.GREENHOUSE, CottageGoals.Activity.GARDEN));
            for (Role role : Cottages.ROLES) {
                AsgardianEntity npc = CottageKeeper.resident(level, cottage, role);
                CottageGoals.Activity a = CottageGoals.settle(npc, cottage, 1000L);
                check(morning.get(role).contains(a), role + " in the morning: " + a);
                check(npc.blockPosition().distManhattan(p.home(role)) < 40, role + " stays at the cottage");
                if (role == Role.NICKY) {
                    check(npc.blockPosition().equals(p.hutchGate()), "Nicky feeds Scratchy at the hutch");
                }
            }
            for (int i = 0; i < 6; i++) {
                AsgardianEntity rio = CottageKeeper.resident(level, cottage, Role.RIO);
                rio.setState(NpcState.IDLE, 0);
                CottageGoals.Activity a = CottageGoals.settle(rio, cottage, 5000L);
                check(a != CottageGoals.Activity.DINNER && a != CottageGoals.Activity.BED, "Rio's day: " + a);
                check(a != CottageGoals.Activity.GARDEN || p.garden().contains(rio.blockPosition()), "in the garden");
            }
            for (Role role : Cottages.ROLES) {
                AsgardianEntity npc = CottageKeeper.resident(level, cottage, role);
                npc.setState(NpcState.IDLE, 0);
            }
            for (Role role : Cottages.ROLES) {
                AsgardianEntity npc = CottageKeeper.resident(level, cottage, role);
                check(CottageGoals.settle(npc, cottage, 11500L) == CottageGoals.Activity.DINNER, role + " at dinner");
                check(npc.state() == NpcState.SITTING && npc.blockPosition().equals(p.seat(role)),
                        role + " sits on his chair at the table: " + npc.blockPosition());
            }
            for (Role role : Cottages.ROLES) {
                AsgardianEntity npc = CottageKeeper.resident(level, cottage, role);
                npc.setState(NpcState.IDLE, 0);
                check(CottageGoals.settle(npc, cottage, 18000L) == CottageGoals.Activity.BED, role + " goes to bed");
                check(npc.state() == NpcState.SLEEPING && npc.position().distanceTo(Vec3.atCenterOf(p.bed(role))) < 1.6,
                        role + " sleeps in his bed");
            }
            check(CottageGoals.Phase.at(23500L) == CottageGoals.Phase.MORNING
                    && CottageGoals.Phase.at(12000L) == CottageGoals.Phase.DUSK, "the parts of the day");
            check(CottageGoals.pick(Role.NICKY, CottageGoals.Phase.DAY, true, level.getRandom())
                    != CottageGoals.Activity.PLAY, "rain keeps Nicky in");
            return true;
        });
    }

    // ---- one of each: a player who is Agatha ----

    @GameTest(template = ARENA, batch = ONE_EACH, timeoutTicks = 120)
    public static void aPlayerWhoIsAgathaSendsHerAwayAndNickyCallsHerMom(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Cottages.Cottage cottage = cottage(fx);
        ServerLevel level = fx.level();
        CottageDialogue.register();
        fx.everyTick(100, tick -> {
            if (!loaded(level, cottage)) {
                return false;
            }
            check(CottageKeeper.keep(level, cottage).size() == 4, "the family and Scratchy");
            AsgardianEntity agatha = CottageKeeper.resident(level, cottage, Role.AGATHA);
            AsgardianEntity nicky = CottageKeeper.resident(level, cottage, Role.NICKY);
            ServerPlayer friend = fx.player(9.5, FLOOR, 4.5);
            check(AsgardTalk.begin(friend, nicky), "a friend talks to Nicky");
            check(AsgardTalk.tryChoose(friend, nicky.getId(), "cottage_about_mom")
                    && "mom".equals(AsgardTalk.currentNode(friend)), "nobody is Agatha: Mom is making soup");
            AsgardTalk.end(friend);
            ServerPlayer mom = fx.player(10.5, FLOOR, 4.5, Power.AGATHA);
            check(CottageKeeper.agathaAway(level.getServer()), "a player is Agatha: the cottage's Agatha is away");
            check(CottageKeeper.keep(level, cottage).isEmpty() && agatha.isRemoved()
                    && cottage.resident(Role.AGATHA) == null, "she left (in a violet shimmer), made nobody");
            AsgardianEntity copy = ModEntities.COTTAGE_RESIDENT.get().create(level);
            copy.setRole(Role.AGATHA, 0);
            copy.setHome(Cottages.slot(cottage.key(), Role.AGATHA), cottage.points().agathaHome(), 16);
            copy.moveTo(Vec3.atBottomCenterOf(cottage.points().agathaHome()));
            check(!level.addFreshEntity(copy), "her coming back while a player is Agatha is refused");
            check(CottageKeeper.resident(level, cottage, Role.NICKY) != null
                    && CottageKeeper.resident(level, cottage, Role.RIO) != null, "Nicky and Rio stay");
            nicky.setState(NpcState.IDLE, 0);
            TestKit.sentPackets(mom);
            CottageKeeper.quick(level, cottage, mom);
            check(saidInChat(mom, "message.enhanced.cottage.mom_home"), "Nicky: Mom! You're home!");
            check(CottageKeeper.greeted(mom, cottage), "once per visit");
            nicky.setState(NpcState.IDLE, 0);
            CottageKeeper.quick(level, cottage, mom);
            check(!saidInChat(mom, "message.enhanced.cottage.mom_home"), "not twice in one visit");
            check(AsgardTalk.begin(friend, nicky) && AsgardTalk.tryChoose(friend, nicky.getId(), "cottage_about_mom")
                    && "mom_player".equals(AsgardTalk.currentNode(friend)), "the friend asks: Mom is the player now");
            AsgardDialogue.Ctx ctx = AsgardTalk.context(friend, nicky, Role.NICKY);
            check(AsgardDialogue.argsOf(ctx, "mom_player").equals(List.of(mom.getGameProfile().getName())),
                    "the line names her");
            AsgardTalk.end(friend);
            nicky.setState(NpcState.IDLE, 0);
            check(AsgardTalk.begin(mom, nicky) && "greeting_mom".equals(AsgardTalk.currentNode(mom)),
                    "Nicky talks to her as his mom");
            check(AsgardTalk.tryChoose(mom, nicky.getId(), "cottage_about_mom")
                    && "mom_you".equals(AsgardTalk.currentNode(mom)), "Mom, that's you!");
            AsgardTalk.end(mom);
            AsgardianEntity rio = CottageKeeper.resident(level, cottage, Role.RIO);
            rio.moveTo(mom.getX() + 1.5, mom.getY(), mom.getZ());
            check(AsgardTalk.begin(mom, rio) && "greeting_agatha".equals(AsgardTalk.currentNode(mom)),
                    "Rio: There you are. The soup is still warm.");
            AsgardTalk.end(mom);
            Powers.setPower(mom, Power.NONE);
            check(!CottageKeeper.agathaAway(level.getServer()), "nobody is Agatha now");
            List<Entity> back = CottageKeeper.keep(level, cottage);
            AsgardianEntity home = CottageKeeper.resident(level, cottage, Role.AGATHA);
            check(back.size() == 1 && home != null && !cottage.has(Cottages.AGATHA_AWAY), "Agatha comes home");
            return true;
        });
    }

    // ---- talk, trades, gifts ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyLineAndAnswerIsInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        CottageDialogue.register();
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> missing = new ArrayList<>();
        int cottageKeys = 0;
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = JsonParser.parseString(Files.readString(mod.findResource("assets", Enhanced.MODID, "lang",
                    name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : AsgardDialogue.langKeys()) {
                if (!lang.has(key)) {
                    missing.add(name + ":" + key);
                } else if (lang.get(key).getAsString().length() > 140) {
                    missing.add(name + ":" + key + " (longer than 140)");
                }
                if (key.startsWith("npc.enhanced.agatha.") || key.startsWith("npc.enhanced.nicky.")
                        || key.startsWith("npc.enhanced.rio.") || key.startsWith("npc.enhanced.option.cottage_")) {
                    cottageKeys++;
                }
            }
            for (String key : List.of("entity.enhanced.senor_scratchy", "item.enhanced.toy_broom",
                    "sign.enhanced.cottage.line1", "sign.enhanced.cottage.line2", "sign.enhanced.cottage.line3",
                    "sign.enhanced.cottage.gate", "message.enhanced.cottage.two_moms")) {
                if (!lang.has(key)) {
                    missing.add(name + ":" + key);
                }
            }
        }
        check(missing.isEmpty(), "missing: " + missing);
        check(cottageKeys >= 2 * 70, "the family's lines and answers are in the table: " + cottageKeys / 2);
        for (Role role : Cottages.ROLES) {
            check(AsgardDialogue.find(role, "greeting") != null && AsgardDialogue.find(role, "greeting_first") != null,
                    role + " greets");
            for (AsgardDialogue.Node node : AsgardDialogue.nodes(role)) {
                check(node.options().size() <= 4, role + "/" + node.key() + " offers at most three at a time");
            }
        }
        check(AsgardDialogue.find(Role.RIO, "who") != null && AsgardDialogue.find(Role.AGATHA, "scratchy_gone") != null,
                "Rio's one line about who she is; Agatha's Scratchy quest");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void agathaAndRioTradeKindThings(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        MerchantOffers agatha = AsgardTrades.offers(Role.AGATHA);
        MerchantOffers rio = AsgardTrades.offers(Role.RIO);
        check(agatha.size() == 7 && rio.size() == 14 && AsgardTrades.offers(Role.NICKY).isEmpty(),
                "Agatha 7 offers, Rio 14, Nicky none: " + agatha.size() + "/" + rio.size());
        for (MerchantOffer offer : agatha) {
            check(offer.getBaseCostA().is(Items.EMERALD), "paid in emeralds");
        }
        for (MerchantOffer offer : rio) {
            check(offer.getBaseCostA().is(Items.EMERALD), "paid in emeralds");
        }
        check(agatha.stream().anyMatch(o -> o.getResult().is(Items.POTION)
                && o.getResult().get(DataComponents.POTION_CONTENTS).is(Potions.HEALING)), "a potion of Healing");
        check(agatha.stream().anyMatch(o -> o.getResult().is(Items.ENCHANTED_BOOK) && o.getCostB().is(Items.BOOK)),
                "an enchanted book for a book");
        check(CottageTrades.soup().has(DataComponents.SUSPICIOUS_STEW_EFFECTS), "Agatha's soup heals");
        check(rio.stream().anyMatch(o -> o.getResult().is(Items.BONE_MEAL) && o.getResult().getCount() == 8),
                "bone meal x8");
        AsgardianEntity npc = resident(fx, Role.RIO, 5.5, 5.5);
        check(npc.getOffers().size() == 14, "Rio's own offers");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void nickyGivesAGiftAndRioABlessingOnceADay(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        CottageDialogue.register();
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity nicky = resident(fx, Role.NICKY, 7.5, 5.5);
        AsgardianEntity rio = resident(fx, Role.RIO, 3.5, 5.5);
        long day = Math.floorDiv(fx.level().getDayTime(), 24000L);
        check(AsgardTalk.begin(player, nicky) && "greeting_first".equals(AsgardTalk.currentNode(player)),
                "Nicky's first hello");
        check(player.getData(ModAttachments.COTTAGE).has(CottageProgress.MET_NICKY), "they have met");
        int before = player.getInventory().items.stream().mapToInt(s -> s.getCount()).sum();
        check(AsgardTalk.tryChoose(player, nicky.getId(), "cottage_gift")
                && "gift".equals(AsgardTalk.currentNode(player)),
                "a gift");
        int after = player.getInventory().items.stream().mapToInt(s -> s.getCount()).sum();
        check(after > before, "he has it");
        check(player.getData(ModAttachments.COTTAGE).giftsOn(day) == 1, "one today");
        AsgardTalk.end(player);
        nicky.setState(NpcState.IDLE, 0);
        check(AsgardTalk.begin(player, nicky) && AsgardTalk.tryChoose(player, nicky.getId(), "cottage_gift")
                && "gift_done".equals(AsgardTalk.currentNode(player)), "only one a day");
        AsgardTalk.end(player);
        player.setData(ModAttachments.COTTAGE, player.getData(ModAttachments.COTTAGE).withLastGiftDay(day - 1));
        nicky.setState(NpcState.IDLE, 0);
        check(AsgardTalk.begin(player, nicky) && AsgardTalk.tryChoose(player, nicky.getId(), "cottage_gift")
                && "gift".equals(AsgardTalk.currentNode(player)), "a new day, a new gift");
        AsgardTalk.end(player);
        check(AsgardTalk.begin(player, rio) && AsgardTalk.offered(player).contains("cottage_blessing"),
                "Rio may bless");
        check(AsgardTalk.tryChoose(player, rio.getId(), "cottage_blessing") && player.hasEffect(MobEffects.REGENERATION)
                && player.getEffect(MobEffects.REGENERATION).getAmplifier() == 0, "Regeneration I");
        AsgardTalk.end(player);
        rio.setState(NpcState.IDLE, 0);
        check(AsgardTalk.begin(player, rio) && !AsgardTalk.offered(player).contains("cottage_blessing"),
                "one blessing a day");
        check(AsgardTalk.tryChoose(player, rio.getId(), "cottage_chat")
                && AsgardTalk.tryChoose(player, rio.getId(), "cottage_who")
                && "who".equals(AsgardTalk.currentNode(player)), "who she is, only when asked");
        AsgardTalk.end(player);
        helper.succeed();
    }

    // ---- Señor Scratchy ----

    @GameTest(template = ARENA, batch = KEEPER, timeoutTicks = 160)
    public static void scratchyRunsOffAndTheQuestBringsHimHomeOnce(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Cottages.Cottage cottage = cottage(fx);
        ServerLevel level = fx.level();
        CottageDialogue.register();
        BlockPos hutch = cottage.points().hutch();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.getChunk((hutch.getX() >> 4) + dx, (hutch.getZ() >> 4) + dz);
            }
        }
        fx.everyTick(150, tick -> {
            if (!loaded(level, cottage)) {
                return false;
            }
            CottageKeeper.keep(level, cottage);
            Rabbit scratchy = Scratchy.find(level, cottage);
            check(scratchy != null && scratchy.distanceToSqr(Vec3.atBottomCenterOf(hutch)) < 4.0, "at his hutch");
            long day = Math.floorDiv(level.getDayTime(), 24000L) + 5;
            check(!Scratchy.morning(level, cottage, day * 24000L + 1000L, level.getRandom(), 0.0, false)
                    && !cottage.scratchyGone(), "chance 0: he stays");
            check(Scratchy.morning(level, cottage, (day + 1) * 24000L + 1000L, level.getRandom(), 1.0, false)
                    && cottage.scratchyGone(), "chance 1: he runs off");
            double away = Math.sqrt(scratchy.distanceToSqr(Vec3.atBottomCenterOf(hutch)));
            check(away >= Scratchy.RUN_MIN - 2 && away <= Scratchy.RUN_MAX + 2, "30 to 60 blocks away: " + away);
            check(!Scratchy.morning(level, cottage, (day + 2) * 24000L + 1000L, level.getRandom(), 1.0, false),
                    "not again while he is out");
            ServerPlayer player = fx.player(16.5, FLOOR, 16.5);
            AsgardianEntity agatha = CottageKeeper.resident(level, cottage, Role.AGATHA);
            agatha.setState(NpcState.IDLE, 0);
            agatha.moveTo(player.getX() + 2, player.getY(), player.getZ());
            check(AsgardTalk.begin(player, agatha) && "scratchy_gone".equals(AsgardTalk.currentNode(player)),
                    "Agatha: Señor Scratchy is gone again");
            check(AsgardTalk.tryChoose(player, agatha.getId(), "cottage_scratchy_yes")
                    && player.getData(ModAttachments.COTTAGE).has(CottageProgress.SCRATCHY_QUEST), "I'll find him!");
            AsgardTalk.end(player);
            CottageKeeper.quick(level, cottage, player);
            check(cottage.scratchyGone(), "far away he is not home");
            long run = cottage.gone();
            scratchy.teleportTo(hutch.getX() + 2.5, hutch.getY(), hutch.getZ() + 0.5);
            CottageKeeper.quick(level, cottage, player);
            check(!cottage.scratchyGone(), "within 4 of the hutch: home");
            check(player.getInventory().countItem(Items.EMERALD) == 3, "3 emeralds");
            check(player.getInventory().items.stream().anyMatch(s -> s.is(Items.POTION)
                    && s.get(DataComponents.POTION_CONTENTS).is(Potions.LUCK)), "and a potion of Luck");
            CottageProgress progress = player.getData(ModAttachments.COTTAGE);
            check(progress.scratchyRun() == run && !progress.has(CottageProgress.SCRATCHY_QUEST), "the run is done");
            cottage.setGone(run);
            check(Scratchy.comeHome(level, cottage, scratchy).isEmpty()
                    && player.getInventory().countItem(Items.EMERALD) == 3,
                    "once per run-off");
            Rabbit copy = net.minecraft.world.entity.EntityType.RABBIT.create(level);
            copy.getPersistentData().putLong(Scratchy.TAG, cottage.key());
            copy.moveTo(Vec3.atBottomCenterOf(hutch));
            check(!level.addFreshEntity(copy), "no second Scratchy");
            return true;
        });
    }

    // ---- the cellar's Road door ----

    @GameTest(template = ARENA, batch = KEEPER, timeoutTicks = 120)
    public static void theCellarDoorJoinsTheRoadAndWandaWalksIt(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Cottages.Cottage cottage = cottage(fx);
        ServerLevel level = fx.level();
        BlockPos door = cottage.points().roadDoor();
        WitchesRoad.removeDoor(level, door);
        check(!WitchesRoad.doors(level).doors().contains(door), "a door set by generation is not on the Road");
        fx.everyTick(100, tick -> {
            if (!loaded(level, cottage)) {
                return false;
            }
            CottageKeeper.keep(level, cottage);
            check(WitchesRoad.doors(level).doors().contains(door) && cottage.has(Cottages.DOOR_REGISTERED),
                    "the keeper adds the cellar door to the Road");
            BlockPos other = fx.helper().absolutePos(new BlockPos(18, (int) FLOOR, 3));
            level.setBlockAndUpdate(other, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState());
            fx.onClose(() -> {
                WitchesRoad.removeDoor(level, other);
                level.setBlockAndUpdate(other, Blocks.AIR.defaultBlockState());
            });
            ServerPlayer wanda = fx.player(10.5, FLOOR, 11.5, Power.SCARLET_WITCH);
            WitchesRoad.use(level, door, wanda);
            check(wanda.blockPosition().distManhattan(other) <= 1, "Wanda steps through to the next door: "
                    + wanda.blockPosition() + " / " + other);
            return true;
        });
    }
}
