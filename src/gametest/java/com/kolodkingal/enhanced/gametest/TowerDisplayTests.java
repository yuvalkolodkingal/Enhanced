package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.SuitDisplayBlock;
import com.kolodkingal.enhanced.block.SuitDisplayBlockEntity;
import com.kolodkingal.enhanced.block.TechCaseBlock;
import com.kolodkingal.enhanced.block.TechCaseBlockEntity;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.tower.Armoury;
import com.kolodkingal.enhanced.tower.TowerRefit;
import com.kolodkingal.enhanced.tower.TowerRefits;
import com.kolodkingal.enhanced.world.StoneSitePlans;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.kolodkingal.enhanced.world.TowerPlan;
import com.kolodkingal.enhanced.world.TowerSiteStructure;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Avengers Tower's armoury (3.8, lane L6, PLAN 9 and 14): the templates hold the displays and cases at {@link
 * TowerPlan}'s points with their suits, items and rules; a display or case gives one per player, Cap's case only to
 * Captain America; the generated ones do not break in Survival; the tower stays in its 3 x 3 chunks however it is
 * turned. {@code tower_refit}: the run-time refit of a 3.6.1 tower (its templates kept as test resources, {@code
 * avengers_tower_v361/}): the armoury set down only where the 3.6.1 blocks stand, a player's block and armour stand
 * left (and counted), the stands and the frame removed on their spots, a second pass changing nothing, {@code force},
 * the takers kept, a 3.6.0-like tower left alone, the player's tick finding a tower by its start and recording it,
 * the command's answers, and the refit's time.
 */
public final class TowerDisplayTests {
    static final String BATCH = "tower_displays";
    static final String REFIT = "tower_refit";
    /** The 3.6.1 tower's templates (test resources). */
    static final List<ResourceLocation> V361 = List.of(Enhanced.id("avengers_tower_v361/base"),
            Enhanced.id("avengers_tower_v361/mid"), Enhanced.id("avengers_tower_v361/top"));

    // ------------------------------------------------------------------------------------------------ the templates

    /** Every point of the plan holds its block in the templates (the whole tower's frame is the model's). */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void everyArmouryPointHoldsItsBlockInTheTemplates(GameTestHelper helper) {
        StoneSiteTests.Raw whole = StoneSiteTests.tower(helper.getLevel().getServer());
        for (Map.Entry<SuitKind, BlockPos> e : TowerPlan.SUIT_DISPLAYS.entrySet()) {
            BlockPos p = e.getValue();
            BlockState lower = whole.at(p);
            BlockState upper = whole.at(p.above());
            check(lower != null && lower.is(ModBlocks.SUIT_DISPLAY.get())
                    && lower.getValue(SuitDisplayBlock.HALF) == DoubleBlockHalf.LOWER
                    && lower.getValue(SuitDisplayBlock.GENERATED)
                    && lower.getValue(SuitDisplayBlock.FACING) == Direction.SOUTH, e.getKey() + "'s display: " + lower);
            check(upper != null && upper.is(ModBlocks.SUIT_DISPLAY.get())
                    && upper.getValue(SuitDisplayBlock.HALF) == DoubleBlockHalf.UPPER, "its upper half: " + upper);
            CompoundTag data = whole.data().get(p);
            check(data != null && data.getString("suit").equals(e.getKey().id()), "it shows the " + e.getKey()
                    + ": " + data);
            check(TowerPlan.inTemplate(p).part() == 1 && TowerPlan.inTemplate(p.above()).part() == 1,
                    "both halves in the mid template");
        }
        BoundingBox bay = TowerPlan.HULKBUSTER_BAY;
        BlockPos hulkbuster = TowerPlan.SUIT_DISPLAYS.get(SuitKind.HULKBUSTER);
        for (BlockPos p : BlockPos.betweenClosed(bay.minX(), bay.minY(), bay.minZ(), bay.maxX(), bay.maxY(),
                bay.maxZ())) {
            if (!p.equals(hulkbuster) && !p.equals(hulkbuster.above())) {
                check(whole.at(p) != null && whole.at(p).isAir(), "the Hulkbuster's bay is clear at " + p + ": "
                        + whole.at(p));
            }
        }
        for (TowerPlan.Case c : TowerPlan.CASE_ITEMS) {
            BlockState state = whole.at(c.pos());
            check(state != null && state.is(ModBlocks.TECH_CASE.get()) && state.getValue(TechCaseBlock.GENERATED),
                    "a case at " + c.pos() + ": " + state);
            CompoundTag data = whole.data().get(c.pos());
            check(data != null && data.getCompound("shown").getString("id").equals(c.item().toString())
                    && data.getString("rule").equals(c.captainOnly() ? "captain_only" : "anyone"),
                    "the case at " + c.pos() + " shows " + c.item() + ": " + data);
        }
        check(whole.at(TowerPlan.CARTOGRAPHY_TABLE).is(Blocks.CARTOGRAPHY_TABLE), "the cartography table moved");
        check(whole.at(TowerPlan.ARC_REACTOR_CASE.below()).is(Blocks.SMOOTH_QUARTZ_SLAB), "the reactors on the desk");
        check(whole.at(TowerPlan.SERUM_CASE.below()).is(Blocks.IRON_BLOCK), "the serum on the lab's workbench");
        // the workshop's decorative stands and Cap's frame are gone (the other rooms' stay)
        long stands = whole.entities().stream().filter(t -> t.getString("id").equals("minecraft:armor_stand")).count();
        long frames = whole.entities().stream().filter(t -> t.getString("id").equals("minecraft:item_frame")).count();
        check(stands == 4 && frames == 12, "4 stands and 12 frames left in the other rooms: " + stands + ", " + frames);
        CompoundTag sign = whole.data().get(TowerPlan.LOBBY_SIGN);
        check(sign != null && sign.toString().contains("sign.enhanced.tower.workshop_hint")
                && sign.toString().contains("sign.enhanced.avengers_tower"), "the reception sign points the way");
        helper.succeed();
    }

    /** However the tower is turned, every part of it lies in the 3 x 3 chunks round its start's chunk. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theTowerStaysInItsNineChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TowerSiteStructure tower = (TowerSiteStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(Enhanced.id("avengers_tower"));
        ChunkPos start = new ChunkPos(1000, -400);
        BlockPos center = new BlockPos(start.getMiddleBlockX(), 70, start.getMiddleBlockZ());
        for (Rotation rotation : Rotation.values()) {
            for (TemplateSitePiece piece : tower.pieces(level.getStructureManager(), rotation, center)) {
                BoundingBox box = piece.getBoundingBox();
                check((box.minX() >> 4) >= start.x - 1 && (box.maxX() >> 4) <= start.x + 1
                        && (box.minZ() >> 4) >= start.z - 1 && (box.maxZ() >> 4) <= start.z + 1,
                        "turned " + rotation + ", a part " + box + " stays in the chunks round " + start);
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ the blocks

    private static BlockPos display(GameTestHelper helper, int x, int z, SuitKind kind, boolean generated) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        BlockState lower = ModBlocks.SUIT_DISPLAY.get().defaultBlockState().setValue(SuitDisplayBlock.FACING,
                Direction.SOUTH).setValue(SuitDisplayBlock.GENERATED, generated);
        level.setBlock(at, lower, Block.UPDATE_ALL);
        level.setBlock(at.above(), lower.setValue(SuitDisplayBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        ((SuitDisplayBlockEntity) level.getBlockEntity(at)).setKind(kind);
        return at;
    }

    private static BlockPos techCase(GameTestHelper helper, int x, int z, ItemStack shown,
                                     TechCaseBlockEntity.Rule rule, boolean generated) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        level.setBlock(at, ModBlocks.TECH_CASE.get().defaultBlockState().setValue(TechCaseBlock.GENERATED, generated),
                Block.UPDATE_ALL);
        ((TechCaseBlockEntity) level.getBlockEntity(at)).setShown(shown, rule);
        return at;
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            n += s.is(item) ? s.getCount() : 0;
        }
        n += player.getOffhandItem().is(item) ? player.getOffhandItem().getCount() : 0;
        return n;
    }

    /** A player uses a block (the server's use, as a click does). */
    private static void use(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        state.useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos,
                false));
    }

    /** A display gives its suit once to each player: into the empty hand, else the inventory; the upper half too. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aDisplayGivesItsSuitOncePerPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos at = display(helper, 5, 5, SuitKind.MARK_3, true);
        ServerPlayer a = f.player(5.5, 2, 7.5);
        ServerPlayer b = f.player(6.5, 2, 7.5);
        ServerPlayer c = f.player(7.5, 2, 7.5);
        b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        use(a, at);
        check(a.getMainHandItem().is(ModItems.IRON_MAN_MARK_3.get()), "a gets the Mark 3 in his hand: "
                + a.getMainHandItem());
        use(a, at.above());
        check(count(a, ModItems.IRON_MAN_MARK_3.get()) == 1, "a second click gives no second suit");
        check(Armoury.takeSuit(level, at, b) && b.getMainHandItem().is(Items.STICK)
                && count(b, ModItems.IRON_MAN_MARK_3.get()) == 1, "b gets his own, into his inventory");
        use(c, at.above());
        check(count(c, ModItems.IRON_MAN_MARK_3.get()) == 1, "the upper half gives too");
        SuitDisplayBlockEntity be = (SuitDisplayBlockEntity) level.getBlockEntity(at);
        check(be.kind() == SuitKind.MARK_3 && be.taken(a.getUUID()) == 1 && be.taken(b.getUUID()) == 1,
                "the display keeps its suit and its takers");
        // the Hulkbuster's display gives the Hulkbuster
        BlockPos hb = display(helper, 12, 5, SuitKind.HULKBUSTER, true);
        check(Armoury.takeSuit(level, hb, a) && count(a, IronSuits.item(SuitKind.HULKBUSTER)) == 1,
                "the Hulkbuster's display gives a Hulkbuster");
        helper.succeed();
    }

    /** Cap's case refuses a powerless player and gives Captain America his shield, once. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void capsCaseGivesOnlyCaptainAmerica(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos at = techCase(helper, 5, 5, new ItemStack(ModItems.VIBRANIUM_SHIELD.get()),
                TechCaseBlockEntity.Rule.CAPTAIN_ONLY, true);
        ServerPlayer nobody = f.player(5.5, 2, 7.5);
        ServerPlayer cap = f.player(6.5, 2, 7.5, Power.CAPTAIN_AMERICA);
        use(nobody, at);
        check(count(nobody, ModItems.VIBRANIUM_SHIELD.get()) == 0 && nobody.getMainHandItem().isEmpty(),
                "a powerless player is refused");
        use(cap, at);
        check(cap.getMainHandItem().is(ModItems.VIBRANIUM_SHIELD.get()), "Captain America gets it: "
                + cap.getMainHandItem());
        use(cap, at);
        check(count(cap, ModItems.VIBRANIUM_SHIELD.get()) == 1, "once");
        // an open case (the wings): anyone, once each; the lid opens and a close is due
        BlockPos wings = techCase(helper, 9, 5, new ItemStack(ModItems.FALCON_EXO_WINGS.get()),
                TechCaseBlockEntity.Rule.ANYONE, true);
        check(Armoury.takeFromCase(level, wings, nobody) && !Armoury.takeFromCase(level, wings, nobody)
                && Armoury.takeFromCase(level, wings, cap), "the wings: one for each player");
        check(level.getBlockTicks().hasScheduledTick(wings, ModBlocks.TECH_CASE.get()), "the lid will close");
        check(((TechCaseBlockEntity) level.getBlockEntity(wings)).shown().is(ModItems.FALCON_EXO_WINGS.get()),
                "and the wings stay on show");
        helper.succeed();
    }

    /** The tower's displays and cases do not break in Survival; a builder's do; Creative breaks both. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGeneratedDisplaysAndCasesDoNotBreakInSurvival(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos generated = display(helper, 5, 5, SuitKind.MARK_85, true);
        BlockPos placed = display(helper, 7, 5, SuitKind.MARK_85, false);
        BlockPos genCase = techCase(helper, 9, 5, new ItemStack(ModItems.ARC_REACTOR.get()),
                TechCaseBlockEntity.Rule.ANYONE, true);
        BlockPos placedCase = techCase(helper, 11, 5, new ItemStack(ModItems.ARC_REACTOR.get()),
                TechCaseBlockEntity.Rule.ANYONE, false);
        ServerPlayer survival = f.player(8.5, 2, 8.5);
        ServerPlayer creative = f.player(8.5, 2, 9.5, GameType.CREATIVE);
        for (BlockPos p : List.of(generated, generated.above(), genCase)) {
            check(level.getBlockState(p).getDestroyProgress(survival, level, p) == 0.0F,
                    "a generated one does not break in Survival: " + level.getBlockState(p));
            check(level.getBlockState(p).getDestroyProgress(creative, level, p) > 0.0F, "Creative breaks it");
        }
        for (BlockPos p : List.of(placed, placedCase)) {
            check(level.getBlockState(p).getDestroyProgress(survival, level, p) > 0.0F,
                    "a builder's breaks: " + level.getBlockState(p));
        }
        // one half gone, the other goes too
        level.destroyBlock(placed.above(), false);
        check(level.getBlockState(placed).isAir(), "the lower half went with the upper");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ the refit

    /** A 3.6.1 tower (its own templates) built at {@code center}, turned {@code rotation}: its three pieces. */
    static List<TemplateSitePiece> v361(ServerLevel level, BlockPos center, Rotation rotation) {
        Holder<StructureProcessorList> footing = StoneSiteTests.processors(level, "avengers_tower");
        Holder<StructureProcessorList> none = Holder.direct(new StructureProcessorList(List.of()));
        List<TemplateSitePiece> pieces = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pieces.add(new TemplateSitePiece(level.getStructureManager(), V361.get(i), i == 0 ? footing : none,
                    rotation, center, level.getStructureManager().getOrCreate(V361.get(i)).getSize(),
                    StoneSitePlans.Tower.FLOOR - i * StoneSitePlans.Tower.PART_Y[1]));
        }
        StoneSiteTests.build(level, pieces);
        return pieces;
    }

    private static List<ArmorStand> standsAt(ServerLevel level, BlockPos cell) {
        return level.getEntitiesOfClass(ArmorStand.class, new AABB(cell), e -> e.blockPosition().equals(cell));
    }

    private static List<ItemFrame> framesAt(ServerLevel level, BlockPos cell) {
        return level.getEntitiesOfClass(ItemFrame.class, new AABB(cell).inflate(0.5), e -> e.getPos().equals(cell));
    }

    private static boolean signSays(ServerLevel level, BlockPos at, int line, String key) {
        return level.getBlockEntity(at) instanceof SignBlockEntity sign
                && sign.getFrontText().getMessage(line, false).getContents() instanceof TranslatableContents t
                && t.getKey().equals(key);
    }

    /**
     * The refit of a 3.6.1 tower, turned a quarter: the armoury set down where the 3.6.1 blocks stand; a player's
     * block and a player's changed stand left and counted (and no display where his stand stays); the other stands and
     * Cap's frame gone; the reception sign gains its lines; a second pass does nothing; {@code force} puts the
     * player's spot too and keeps the takers; done a step a tick, as the game does it, each step takes at most 2 ms
     * (its code warm: another tower refitted first).
     */
    @EnhancedTest(template = ARENA, batch = REFIT, timeoutTicks = 600)
    public static void theRefitSetsTheArmouryDownInA361Tower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        int x = 300_008;
        int z = 102_008;
        BlockPos center = new BlockPos(x, StoneSiteTests.ground(level, x, z), z);
        f.loadChunksAround(center, 3);
        Rotation rotation = Rotation.CLOCKWISE_90;
        List<TemplateSitePiece> pieces = v361(level, center, rotation);
        BlockPos serum = TowerRefit.world(pieces, TowerPlan.SERUM_CASE);
        BlockPos mark50 = TowerRefit.world(pieces, TowerPlan.SUIT_DISPLAYS.get(SuitKind.MARK_50));
        BlockPos mark3 = TowerRefit.world(pieces, TowerPlan.SUIT_DISPLAYS.get(SuitKind.MARK_3));
        BlockPos frameCell = TowerRefit.world(pieces, TowerPlan.SHIELD_CASE.above());
        // a first tower refitted before the timed one: the game's first refit also loads and compiles its code once
        int wx = 300_408;
        BlockPos warm = new BlockPos(wx, StoneSiteTests.ground(level, wx, z), z);
        f.loadChunksAround(warm, 3);
        List<TemplateSitePiece> first = v361(level, warm, Rotation.NONE);
        helper.runAfterDelay(10, () -> {
            long cold = StoneSiteTests.cpuNanos();
            TowerRefit.Result once = TowerRefit.refit(level, first, false);
            Enhanced.LOGGER.info("[tower_refit] the first refit (cold): {} in {} ms", once, String.format(
                    java.util.Locale.ROOT, "%.3f", (StoneSiteTests.cpuNanos() - cold) / 1e6));
            check(level.getBlockState(TowerRefit.world(pieces, TowerPlan.ARC_REACTOR_CASE)).is(Blocks.BEACON),
                    "a 3.6.1 tower: the beacon on the desk");
            check(standsAt(level, mark3).size() == 1 && framesAt(level, frameCell).size() == 1,
                    "with its stands and Cap's frame");
            check(TowerRefit.isArmouryTower(level, pieces) && TowerRefit.loaded(level, pieces), "known and loaded");
            // a player's changes: dirt where the serum's case goes, a diamond helmet on the Mark 50's stand
            level.setBlock(serum, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            standsAt(level, mark50).get(0).setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            // the refit as the game's tick does it: a step a tick, each within the budget
            TowerRefit.Job job = new TowerRefit.Job(pieces, false);
            double worst = 0;
            double total = 0;
            int steps = 0;
            boolean done = false;
            while (!done && steps < 200) {
                long start = StoneSiteTests.cpuNanos();
                done = job.step(level, TowerRefit.STEP_NANOS);
                double ms = (StoneSiteTests.cpuNanos() - start) / 1e6;
                worst = Math.max(worst, ms);
                total += ms;
                steps++;
            }
            TowerRefit.Result r = job.result();
            Enhanced.LOGGER.info("[tower_refit] the refit of a 3.6.1 tower: {} in {} steps, {} ms, the worst {} ms",
                    r, steps, String.format(java.util.Locale.ROOT, "%.3f", total),
                    String.format(java.util.Locale.ROOT, "%.3f", worst));
            int table = TowerRefit.table(new ArrayList<>()).size();
            check(done && !r.oldDesign() && r.changed() == table - 3 && r.skipped() == 3 && r.removed() == 5
                    && r.kept() == 1, "every block but the player's three (the dirt, the Mark 50's two halves), 4 stands "
                    + "and the frame: " + r + " of " + table);
            check(worst <= 2.0, "each step within 2 ms: " + worst);
            for (Map.Entry<SuitKind, BlockPos> e : TowerPlan.SUIT_DISPLAYS.entrySet()) {
                BlockPos p = TowerRefit.world(pieces, e.getValue());
                BlockState s = level.getBlockState(p);
                if (e.getKey() == SuitKind.MARK_50) {
                    check(s.isAir() && standsAt(level, p).size() == 1, "the player's stand stays, no display");
                    continue;
                }
                check(s.is(ModBlocks.SUIT_DISPLAY.get()) && s.getValue(SuitDisplayBlock.FACING)
                        == rotation.rotate(Direction.SOUTH) && level.getBlockState(p.above()).is(
                        ModBlocks.SUIT_DISPLAY.get()), e.getKey() + "'s display, turned with the tower: " + s);
                check(level.getBlockEntity(p) instanceof SuitDisplayBlockEntity d && d.kind() == e.getKey(),
                        "showing the " + e.getKey());
                check(standsAt(level, p).isEmpty(), "its stand gone");
            }
            for (TowerPlan.Case c : TowerPlan.CASE_ITEMS) {
                BlockPos p = TowerRefit.world(pieces, c.pos());
                if (c.pos().equals(TowerPlan.SERUM_CASE)) {
                    check(level.getBlockState(p).is(Blocks.DIRT), "the player's dirt stays");
                    continue;
                }
                check(level.getBlockEntity(p) instanceof TechCaseBlockEntity box
                        && box.shown().getItemHolder().unwrapKey().orElseThrow().location().equals(c.item())
                        && (box.rule() == TechCaseBlockEntity.Rule.CAPTAIN_ONLY) == c.captainOnly(),
                        "the case of " + c.item());
            }
            check(framesAt(level, frameCell).isEmpty(), "Cap's frame gone");
            check(level.getBlockState(TowerRefit.world(pieces, TowerPlan.CARTOGRAPHY_TABLE)).is(
                    Blocks.CARTOGRAPHY_TABLE), "the cartography table moved");
            check(signSays(level, TowerRefit.world(pieces, TowerPlan.LOBBY_SIGN), 2,
                    "sign.enhanced.tower.workshop_hint"), "the reception sign points the way");
            // someone takes his Mark 3; a second pass does nothing
            ServerPlayer taker = f.player(1.5, 2, 1.5);
            check(Armoury.takeSuit(level, mark3, taker), "a Mark 3 taken");
            TowerRefit.Result again = TowerRefit.refit(level, pieces, false);
            check(again.changed() == 0 && again.removed() == 0 && again.skipped() == 3 && again.kept() == 1
                    && again.already() == table - 3, "a second pass changes nothing: " + again);
            // force: the player's dirt goes (dropped), his stand stays; the takers are kept
            TowerRefit.Result forced = TowerRefit.refit(level, pieces, true);
            check(forced.changed() == 1 && level.getBlockEntity(serum) instanceof TechCaseBlockEntity
                    && standsAt(level, mark50).size() == 1, "force puts the serum's case: " + forced);
            check(((SuitDisplayBlockEntity) level.getBlockEntity(mark3)).taken(taker.getUUID()) == 1,
                    "the takers are kept");
            helper.succeed();
        });
    }

    /** Where the 3.6.1 tower is not (an old design, or nothing), the refit changes nothing. */
    @EnhancedTest(template = ARENA, batch = REFIT)
    public static void theRefitLeavesAnotherTowerAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TowerSiteStructure tower = (TowerSiteStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(Enhanced.id("avengers_tower"));
        int x = 300_008;
        int z = 102_808;
        TestKit.fixture(helper).loadChunksAround(new BlockPos(x, 0, z), 2);
        BlockPos center = new BlockPos(x, StoneSiteTests.ground(level, x, z), z);
        List<TemplateSitePiece> pieces = tower.pieces(level.getStructureManager(), Rotation.NONE, center);
        BlockPos display = TowerRefit.world(pieces, TowerPlan.SUIT_DISPLAYS.get(SuitKind.MARK_1));
        BlockState before = level.getBlockState(display);
        TowerRefit.Result r = TowerRefit.refit(level, pieces, true);
        check(r.oldDesign() && r.changed() == 0 && level.getBlockState(display) == before,
                "no tower of the 3.6.1 design there: nothing touched, even with force: " + r);
        helper.succeed();
    }

    /**
     * The player's tick finds a 3.6.1 tower by its structure start, refits it once and records it; the command tells
     * the record, finds nothing more to do, and finds no tower where there is none.
     */
    @EnhancedTest(template = ARENA, batch = REFIT, timeoutTicks = 600)
    public static void theTickRefitsATowerOnceAndTheCommandTellsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        int x = 300_008;
        int z = 102_408;
        BlockPos center = new BlockPos(x, StoneSiteTests.ground(level, x, z), z);
        f.loadChunksAround(center, 3);
        List<TemplateSitePiece> pieces = v361(level, center, Rotation.NONE);
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TowerRefit.TOWER);
        ChunkPos middle = new ChunkPos(center);
        ChunkAccess startChunk = level.getChunk(middle.x, middle.z);
        Map<Structure, StructureStart> starts = new HashMap<>(startChunk.getAllStarts());
        startChunk.setStartForStructure(structure, new StructureStart(structure, middle, 0,
                new PiecesContainer(new ArrayList<StructurePiece>(pieces))));
        for (int cx = middle.x - 1; cx <= middle.x + 1; cx++) {
            for (int cz = middle.z - 1; cz <= middle.z + 1; cz++) {
                level.getChunk(cx, cz).addReferenceForStructure(structure, middle.toLong());
            }
        }
        TowerRefit.clearCache();
        f.onClose(() -> {
            startChunk.setAllStarts(starts);
            TowerRefits.get(level).forget(middle.toLong());
            TowerRefit.clearCache();
        });
        whenLoaded(helper, level, pieces, 10, () -> {
            check(TowerRefits.get(level).get(middle.toLong()) == null, "not refitted yet");
            List<Component> said = new ArrayList<>();
            run(server, level, center, "enhanced structure refit_tower status", said);
            check(says(said, "commands.enhanced.structure.refit_tower.status.none"), "the command: not yet " + said);
            check(TowerRefit.refitNear(level, center.offset(40, 0, 0)) == 1 && TowerRefit.running(middle.toLong()),
                    "the tick starts the refit of the tower near him");
            for (int i = 0; i < 200 && TowerRefit.running(middle.toLong()); i++) {
                TowerRefit.work(level);
            }
            TowerRefits.Entry entry = TowerRefits.get(level).get(middle.toLong());
            check(entry != null && entry.version() == TowerRefit.VERSION && entry.result().changed() > 30,
                    "and records it: " + entry);
            check(level.getBlockState(TowerRefit.world(pieces, TowerPlan.SUIT_DISPLAYS.get(SuitKind.MARK_85)))
                    .is(ModBlocks.SUIT_DISPLAY.get()), "the Mark 85's display stands");
            check(TowerRefit.refitNear(level, center) == 0, "once only");
            said.clear();
            run(server, level, center, "enhanced structure refit_tower status", said);
            check(says(said, "commands.enhanced.structure.refit_tower.status"), "the command tells it: " + said);
            said.clear();
            run(server, level, center, "enhanced structure refit_tower", said);
            check(says(said, "commands.enhanced.structure.refit_tower.already"), "nothing more to do: " + said);
            said.clear();
            run(server, level, center.offset(400, 0, 0), "enhanced structure refit_tower 16", said);
            check(says(said, "commands.enhanced.structure.refit_tower.none"), "no tower out there: " + said);
            helper.succeed();
        });
    }

    /**
     * Runs {@code body} from tick {@code first} on, once the tower's armoury chunks and their entities are loaded (the
     * refit waits for both; the entities come a few ticks after the chunks, later on a busy machine).
     */
    private static void whenLoaded(GameTestHelper helper, ServerLevel level, List<TemplateSitePiece> pieces, int first,
                                   Runnable body) {
        helper.runAfterDelay(first, () -> {
            if (TowerRefit.loaded(level, pieces)) {
                body.run();
            } else {
                whenLoaded(helper, level, pieces, 1, body);
            }
        });
    }

    private static void run(MinecraftServer server, ServerLevel level, BlockPos at, String command,
                            List<Component> said) {
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
        CommandSourceStack source = new CommandSourceStack(sink, Vec3.atCenterOf(at), Vec2.ZERO, level, 2, "test",
                Component.literal("test"), server, null);
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new AssertionError(command + ": " + e.getMessage());
        }
    }

    /** Whether one of the messages is that translation (a failure comes red, as the sibling of an empty text). */
    private static boolean says(List<Component> said, String key) {
        return said.stream().anyMatch(c -> says(c, key));
    }

    private static boolean says(Component text, String key) {
        return text.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                || text.getSiblings().stream().anyMatch(sibling -> says(sibling, key));
    }

    private TowerDisplayTests() {
    }
}
