package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.attach.AttachKey;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.1 (step S of the spec, 18.1): the shared pieces every lane builds on exist and answer as 3.0 did, or
 * as the spec fixes them. Each test that keeps state or changes a config value has a batch of its own ({@code v31_skeleton*}).
 */
public final class V31SkeletonTests {

    // ------------------------------------------------------------------------------------------------------------
    // One rule book for blocks (S5, S6) and the new questions to the boss fights (S7)

    /**
     * {@link BlockRights#mayBreak} keeps the rules of 3.0's {@code BlockLifting.mayBreak}: a protected arena, Adventure
     * mode, the world border and a cancelled break event (which also stands in for spawn protection and a claim mod: the
     * GameTest server has no spawn protection) each refuse it. {@link BlockRights#mayPlace} and the two posts ask the
     * listeners exactly once each.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_rights")
    public static void blockRightsKeepTheOldRules(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(3.5, FLOOR, 3.5);
        BlockPos inside = helper.absolutePos(new BlockPos(10, 1, 10));
        BlockPos outside = helper.absolutePos(new BlockPos(19, 1, 19));
        BlockState floor = level.getBlockState(inside);
        check(BlockRights.mayBreak(player, level, inside, floor) && BlockRights.mayPlace(player, level, inside),
                "with nothing in the way, a Survival player may break and place");

        // A protected arena.
        BossFrameworkTests.start(f, player, Containment.NONE, true);
        check(!BlockRights.mayBreak(player, level, inside, floor) && !BlockRights.mayPlace(player, level, inside),
                "not inside a protected arena");
        check(BlockRights.mayBreak(player, level, outside, floor), "but outside it");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
        check(BlockRights.mayBreak(player, level, inside, floor), "and inside it once the fight is over");

        // Adventure mode.
        player.setGameMode(GameType.ADVENTURE);
        check(!BlockRights.vanillaRights(player, level, inside) && !BlockRights.mayBreak(player, level, inside, floor)
                && !BlockRights.mayPlace(player, level, inside), "not in Adventure mode");
        player.setGameMode(GameType.SURVIVAL);

        // The world border, shrunk round the middle of the floor.
        WorldBorder border = level.getWorldBorder();
        double size = border.getSize();
        double centreX = border.getCenterX();
        double centreZ = border.getCenterZ();
        f.onClose(() -> {
            border.setCenter(centreX, centreZ);
            border.setSize(size);
        });
        Vec3 middle = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        border.setCenter(middle.x, middle.z);
        border.setSize(6.0);
        check(!BlockRights.mayBreak(player, level, outside, floor) && !BlockRights.mayPlace(player, level, outside),
                "not outside the world border");
        check(BlockRights.mayBreak(player, level, inside, floor), "but inside it");
        border.setCenter(centreX, centreZ);
        border.setSize(size);

        // The listeners: each post asks once; a cancelled event refuses.
        int[] breaks = {0};
        int[] places = {0};
        boolean[] cancel = {false};
        Consumer<BlockEvent.BreakEvent> breakListener = event -> {
            breaks[0]++;
            event.setCanceled(cancel[0]);
        };
        Consumer<BlockEvent.EntityPlaceEvent> placeListener = event -> {
            places[0]++;
            event.setCanceled(cancel[0]);
        };
        EventHub.Registration breakRegistration = EventHub.of(BlockEvent.BreakEvent.class).register(Priority.NORMAL, false,
                breakListener);
        EventHub.Registration placeRegistration = EventHub.of(BlockEvent.EntityPlaceEvent.class).register(Priority.NORMAL, false,
                placeListener);
        f.onClose(() -> {
            breakRegistration.close();
            placeRegistration.close();
        });
        check(BlockRights.postBreak(player, level, inside, floor) && breaks[0] == 1 && places[0] == 0, "postBreak posts one break event");
        check(BlockRights.postPlace(player, level, inside) && places[0] == 1 && breaks[0] == 1, "postPlace posts one place event");
        check(BlockRights.mayPlace(player, level, inside) && places[0] == 2 && breaks[0] == 1, "mayPlace posts exactly one place event");
        cancel[0] = true;
        check(!BlockRights.mayBreak(player, level, inside, floor) && breaks[0] == 2, "a cancelled break refuses mayBreak");
        check(!BlockRights.mayPlace(player, level, inside) && places[0] == 3, "a cancelled place refuses mayPlace");
        check(!BlockRights.postBreak(player, level, inside, floor) && !BlockRights.postPlace(player, level, inside),
                "and both posts say so");
        helper.succeed();
    }

    /**
     * The area rule of the two mass edits, with {@code build_magic.allArenasProtected} on (the default): the cylinder of any
     * running fight is refused, protected or not; outside it nothing is.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_mass")
    public static void massEditRefusesArenasHiddenGroundAndHexes(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(3.5, FLOOR, 3.5);
        BlockPos inside = helper.absolutePos(new BlockPos(10, 2, 10));
        BlockPos outside = helper.absolutePos(new BlockPos(19, 2, 19));
        check(!BlockRights.massEditRefuses(player, level, inside), "with no fight, nothing is refused");

        BossFrameworkTests.start(f, player, Containment.NONE, false);
        check(!BossFights.protects(level, inside), "a Thanos-like arena protects nothing");
        check(BlockRights.massEditRefuses(player, level, inside), "but a mass edit keeps out of it");
        check(!BlockRights.massEditRefuses(player, level, outside), "and not outside it");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);

        BossFrameworkTests.start(f, player, Containment.NONE, true);
        check(BlockRights.massEditRefuses(player, level, inside), "a protected arena is refused");
        check(!BlockRights.massEditRefuses(player, level, outside), "and not outside it");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
        check(!BlockRights.massEditRefuses(player, level, inside), "the fight over, nothing is refused");
        helper.succeed();
    }

    /**
     * With {@code build_magic.allArenasProtected} off, an unprotected arena is open to the mass edits and a protected one
     * still is not; with {@code boss.protectArena} off too, the ground a fight hides is still refused, and only that cell.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_mass_open")
    public static void massEditRefusesArenasHiddenGroundAndHexesWithTheSwitchOff(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.config(BuildConfig.ALL_ARENAS_PROTECTED, false);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(3.5, FLOOR, 3.5);
        BlockPos inside = helper.absolutePos(new BlockPos(10, 2, 10));

        BossFrameworkTests.start(f, player, Containment.NONE, false);
        check(BossFights.inAnyArena(level, inside) && !BlockRights.massEditRefuses(player, level, inside),
                "with the switch off, an unprotected arena is open");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);

        BossFrameworkTests.start(f, player, Containment.NONE, true);
        check(BlockRights.massEditRefuses(player, level, inside), "a protected arena is refused either way");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);

        f.config(BossConfig.PROTECT_ARENA, false);
        BlockPos hidden = helper.absolutePos(new BlockPos(10, 1, 10));
        TestFight hiding = new TestFight(level, BossKind.ULTRON, BossFrameworkTests.arena(f, Containment.NONE, true)) {
            @Override
            protected boolean hidesGround(BlockPos pos) {
                return pos.equals(hidden);
            }
        };
        BossFrameworkTests.start(f, BossKind.ULTRON, player, hiding);
        check(!BossFights.protects(level, hidden), "with boss.protectArena off, the arena protects nothing");
        check(BossFights.hidesGround(level, hidden) && BlockRights.massEditRefuses(player, level, hidden),
                "but the ground it hides is refused");
        check(!BossFights.hidesGround(level, hidden.east()) && !BlockRights.massEditRefuses(player, level, hidden.east()),
                "and the cell next to it is not");
        helper.succeed();
    }

    /**
     * With {@code build_magic.hexIsAClaim} on, a cell in someone else's Hex is refused to a stranger, never to its owner,
     * and no longer once the owner has allowed him in.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_hex")
    public static void massEditRefusesAStrangersHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(BuildConfig.HEX_IS_A_CLAIM, true);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = f.player(10.5, FLOOR, 10.5);
        ServerPlayer stranger = f.player(3.5, FLOOR, 3.5);
        Hex hex = new Hex(UUID.randomUUID(), owner.getUUID(), owner.getGameProfile().getName(),
                helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 5, Set.of(), false, level.getGameTime(), false);
        ModAttachments.HEXES.set(level, HexManager.data(level).with(hex));
        f.onClose(() -> ModAttachments.HEXES.set(level, HexManager.data(level).without(hex.id())));
        BlockPos inside = helper.absolutePos(new BlockPos(10, 2, 10));
        BlockPos outside = helper.absolutePos(new BlockPos(18, 2, 18));

        check(BlockRights.massEditRefuses(stranger, level, inside), "a stranger is refused inside her Hex");
        check(!BlockRights.massEditRefuses(stranger, level, outside), "not outside it");
        check(!BlockRights.massEditRefuses(owner, level, inside), "and she never is");
        ModAttachments.HEXES.set(level, HexManager.data(level).update(hex.id(), h -> h.withAllowed(stranger.getUUID(), true)));
        check(!BlockRights.massEditRefuses(stranger, level, inside), "once she allows him, he is not refused");
        helper.succeed();
    }

    /**
     * The questions the lanes ask the boss fights: with no fight every answer is no; with one, its cylinder, a box across
     * its edge, its fighter, the drop it keeps for him, and what it locks.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_facade", timeoutTicks = 40)
    public static void theNewFacadeAnswers(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer stranger = f.player(20.5, FLOOR, 20.5);
        BlockPos inside = helper.absolutePos(new BlockPos(10, 1, 10));
        BlockPos outside = helper.absolutePos(new BlockPos(20, 1, 20));
        BoundingBox acrossTheEdge = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(16, 1, 10)), helper.absolutePos(new BlockPos(20, 3, 10)));
        BoundingBox beyond = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(19, 1, 10)), helper.absolutePos(new BlockPos(20, 3, 20)));
        ItemEntity other = new ItemEntity(level, inside.getX() + 0.5, inside.getY() + 1.5, inside.getZ() + 0.5, new ItemStack(Items.IRON_INGOT));
        level.addFreshEntity(other);

        check(!BossFights.inAnyArena(level, inside) && !BossFights.overlapsAny(level, acrossTheEdge) && !BossFights.isKeptDrop(other)
                && !BossFights.isParticipant(fighter) && !BossFights.hidesGround(level, inside) && !BossFights.isLocked(fighter),
                "with no fight, every answer is no");

        TestFight fight = new TestFight(level, BossKind.ULTRON, BossFrameworkTests.arena(f, Containment.NONE, true)) {
            @Override
            protected boolean locks(Entity entity) {
                return entity == fighter;
            }
        };
        fight.arena().keepDrops(true);
        BossFrameworkTests.start(f, BossKind.ULTRON, fighter, fight);
        check(BossFights.inAnyArena(level, inside) && !BossFights.inAnyArena(level, outside), "its cylinder, and only its cylinder");
        check(BossFights.overlapsAny(level, acrossTheEdge) && !BossFights.overlapsAny(level, beyond), "a box across its edge, not one beyond it");
        check(BossFights.isParticipant(fighter) && !BossFights.isParticipant(stranger), "its fighter, not a stranger");
        check(BossFights.isLocked(fighter) && !BossFights.isLocked(stranger), "what it locks");
        check(!BossFights.hidesGround(level, inside), "it hides no ground");
        helper.runAfterDelay(3, () -> {
            fighter.getInventory().add(new ItemStack(Items.DIAMOND));
            fighter.kill();
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(fighter.blockPosition()).inflate(4),
                    item -> item.getItem().is(Items.DIAMOND));
            check(drops.size() == 1 && BossFights.isKeptDrop(drops.get(0)), "the drop of a fighter who died in the arena is kept");
            check(!BossFights.isKeptDrop(other), "an item that lay there is not");
            check(BossFights.isParticipant(fighter), "he is still a participant, dead");
            other.discard();
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Billy's ranks (S8, B11)

    /**
     * Every number of Billy's ten ranks grows from the five of his first (D-B 8.2, 3.2: ranks 1 to 4 are those of 3.1), and
     * practice makes the rank.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_ranks")
    public static void billysRanksComeFromFiveNumbers(GameTestHelper helper) {
        int[] meter = {512, 1024, 2048, 4096, 6144, 8192, 12288, 16384, 24576, 32768};
        double[] refill = {16, 24, 32, 40, 56, 72, 96, 128, 160, 192};
        int[] side = {24, 32, 40, 48, 56, 64, 72, 80, 88, 96};
        int[] undos = {10, 12, 14, 16, 18, 20, 22, 24, 26, 28};
        int[] repeat = {8, 12, 16, 20, 24, 28, 32, 36, 40, 44};
        int[] brush = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        check(BillyRanks.MAX_RANK == 10 && BillyRanks.maxRank() == 10, "ten ranks, and the config's default lets him reach the tenth");
        for (int rank = 1; rank <= 10; rank++) {
            int i = rank - 1;
            check(BillyRanks.meter(rank) == meter[i], "rank " + rank + ": a meter of " + BillyRanks.meter(rank));
            assertNear(BillyRanks.refill(rank), refill[i], 1.0E-9, "rank " + rank + ": the refill a second");
            check(BillyRanks.side(rank) == side[i] && BillyRanks.reach(rank) == side[i],
                    "rank " + rank + ": side " + BillyRanks.side(rank) + ", reach " + BillyRanks.reach(rank));
            check(BillyRanks.undoSteps(rank) == undos[i], "rank " + rank + ": " + BillyRanks.undoSteps(rank) + " undos");
            check(BillyRanks.repeatMax(rank) == repeat[i], "rank " + rank + ": " + BillyRanks.repeatMax(rank) + " copies");
            check(BillyRanks.brush(rank) == brush[i], "rank " + rank + ": a brush of " + BillyRanks.brush(rank));
            check(BillyRanks.magicSelect(rank) == meter[i], "rank " + rank + ": a double click selects as much as the meter holds");
            check(BillyRanks.rank(BuildData.EMPTY.withRank(rank)) == rank, "rank " + rank + " applies");
        }
        check(BillyRanks.rankFor(0) == 1 && BillyRanks.rankFor(1999) == 1 && BillyRanks.rankFor(2000) == 2 && BillyRanks.rankFor(7999) == 2
                && BillyRanks.rankFor(8000) == 3 && BillyRanks.rankFor(29999) == 3 && BillyRanks.rankFor(30000) == 4,
                "practice makes the rank: 2,000, 8,000, 30,000");
        long[] practice = {0, 2000, 8000, 30000, 60000, 120000, 220000, 380000, 600000, 1000000};
        for (int rank = 1; rank <= 10; rank++) {
            check(BillyRanks.practiceFor(rank) == practice[rank - 1] && BillyRanks.rankFor(practice[rank - 1]) == rank,
                    "rank " + rank + " needs " + practice[rank - 1] + " blocks of practice");
            check(rank == 1 || BillyRanks.rankFor(practice[rank - 1] - 1) == rank - 1, "one block short of rank " + rank + " is rank " + (rank - 1));
            if (rank > 1) {
                check(BillyRanks.meter(rank) > BillyRanks.meter(rank - 1) && BillyRanks.refill(rank) > BillyRanks.refill(rank - 1)
                        && BillyRanks.side(rank) > BillyRanks.side(rank - 1) && BillyRanks.undoSteps(rank) > BillyRanks.undoSteps(rank - 1)
                        && BillyRanks.repeatMax(rank) > BillyRanks.repeatMax(rank - 1) && BillyRanks.brush(rank) > BillyRanks.brush(rank - 1),
                        "every number of rank " + rank + " is more than the rank before");
            }
        }
        check(BillyRanks.rankFor(Long.MAX_VALUE) == 10, "the most practice is rank 10");
        check(BillyRanks.nameKey(4).equals("hud.enhanced.build.rank.4") && BillyRanks.nameKey(10).equals("hud.enhanced.build.rank.10"),
                "the fourth rank is Wiccan's, the tenth has a name key too");
        check(BillyRanks.meter(10) < 100_000 && BillyRanks.side(10) < 256 && BillyRanks.reach(10) < 256 && BillyRanks.brush(10) < 16,
                "rank 10 is strong but still under the Scarlet Witch's limitless numbers (a selection of 100,000, a reach of 256, a brush of 16)");

        // A Billy of 3.1 at rank 4 with 30,000 blocks of practice keeps his rank, and 3.2 only adds the way on from there.
        BuildData old = BuildData.EMPTY.withRank(4).withPractice(30000L);
        check(BillyRanks.rank(old) == 4 && BillyMagic.practise(old, 0L, 29999L).data().rank() == 4 && !BillyMagic.practise(old, 0L, 29999L).rankedUp(),
                "a rank-4 Billy of 3.1 stays at rank 4 until his practice reaches 60,000");
        BuildData grown = BillyMagic.practise(old, 5L, 30000L).data();
        check(BillyMagic.practise(old, 5L, 30000L).rankedUp() && grown.rank() == 5 && BillyMagic.meter(grown, 5L) == 6144,
                "at 60,000 he becomes an Enchanter, and his meter is full at 6,144");

        // The meter refills by game time, up to its top, and a player who never built has it full.
        check(BillyRanks.meterNow(BuildData.EMPTY, 0L) == 512 && BillyRanks.meterNow(BuildData.EMPTY, 1_000_000L) == 512,
                "a Billy who never built has a full meter");
        BuildData spent = BuildData.EMPTY.withMeter(100.0, 1000L);
        assertNear(BillyRanks.meterNow(spent, 1000L), 100.0, 1.0E-9, "the meter right after a spell");
        assertNear(BillyRanks.meterNow(spent, 1020L), 116.0, 1.0E-9, "one second later: 16 more");
        check(BillyRanks.meterNow(spent, 1000L + 20 * 60) == 512, "never above its top");
        assertNear(BillyRanks.meterNow(spent.withRank(2), 1020L), 124.0, 1.0E-9, "a Spellcaster refills 24 a second");
        helper.succeed();
    }

    /** With {@code build_magic_billy.maxRank} 1 he stays an Apprentice whatever he practises or learned. */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_rank_cap")
    public static void billysRanksStopAtMaxRank(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(BuildConfig.BILLY_MAX_RANK, 1);
        check(BillyRanks.maxRank() == 1, "the cap is read live");
        check(BillyRanks.rankFor(0) == 1 && BillyRanks.rankFor(30000) == 1 && BillyRanks.rankFor(Long.MAX_VALUE) == 1,
                "rank 1 at any practice");
        BuildData wiccan = BuildData.EMPTY.withRank(4);
        check(BillyRanks.rank(wiccan) == 1 && BillyRanks.meterNow(wiccan, 0L) == 512,
                "a Wiccan under the cap casts as an Apprentice (and his rank is kept for when it is raised)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The synced data (S9, B13)

    /**
     * {@code BUILD_DATA} syncs to its holder alone and {@code BUILD_VIEW} to whoever sees him too; both codecs give back
     * what they were given; only the meter, the rank, the practice and the pocket are saved; and a player saved before
     * 3.1 reads as {@link BuildData#EMPTY}: rank 1, a full meter.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_sync")
    public static void buildDataAndViewSyncAndOldWorldsLoad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(9.5, FLOOR, 10.5, Power.BILLY);
        ServerPlayer wanda = f.player(12.5, FLOOR, 10.5, Power.SCARLET_WITCH);

        check(!ModAttachments.BUILD_DATA.has(billy) && ModAttachments.BUILD_DATA.get(billy).equals(BuildData.EMPTY)
                && ModAttachments.BUILD_VIEW.get(billy).equals(BuildView.EMPTY), "a player who never built reads as empty");
        check(BillyRanks.rank(BuildData.EMPTY) == 1 && BillyRanks.meterNow(BuildData.EMPTY, helper.getLevel().getGameTime()) == 512,
                "empty is rank 1 with a full meter");

        BuildData data = BuildData.EMPTY.withMeter(123.5, 4000L).withRank(3).withPractice(9000L).withPocket(17, new Vec3i(7, 5, 9))
                .withMomNear(true).withHistory(4, 1).withWork(true, 2, 640).withPauseUntil(4010L).withInstant(true);
        check(roundTrip(BuildData.STREAM_CODEC, data).equals(data), "the data's stream codec");
        BuildView view = new BuildView(true, Spell.COPY, Optional.of(new BuildView.Box(new BlockPos(5, 1, 5), new BlockPos(-3, 9, 2))));
        check(view.box().get().min().equals(new BlockPos(-3, 1, 2)) && view.box().get().max().equals(new BlockPos(5, 9, 5)),
                "a box is kept by its low and high corners");
        check(roundTrip(BuildView.STREAM_CODEC, view).equals(view) && roundTrip(BuildView.STREAM_CODEC, BuildView.EMPTY).equals(BuildView.EMPTY),
                "the view's stream codec");
        BuildData saved = new BuildData(123.5, 4000L, 3, 9000L, 17, new Vec3i(7, 5, 9), false, 0, 0, false, 0, 0, 0L, false);
        Tag nbt = BuildData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        check(BuildData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow().equals(saved), "only the meter, rank, practice and pocket are saved");
        check(BuildData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow().equals(BuildData.EMPTY), "nothing saved reads as empty");

        // A player saved with it, and the same player as 3.0 saved him (without it), loaded again.
        ModAttachments.BUILD_DATA.set(billy, data);
        ModAttachments.BUILD_VIEW.set(billy, view);
        CompoundTag withIt = billy.saveWithoutId(new CompoundTag());
        withIt.remove("UUID");
        check(withIt.getCompound(TestKit.attachmentsKey()).contains(Enhanced.id("build_data").toString())
                && !withIt.getCompound(TestKit.attachmentsKey()).contains(Enhanced.id("build_view").toString()),
                "the data is saved with the player, the view is not");
        CompoundTag before31 = withIt.copy();
        before31.getCompound(TestKit.attachmentsKey()).remove(Enhanced.id("build_data").toString());
        ServerPlayer reloaded = f.player(8.5, FLOOR, 8.5);
        reloaded.load(withIt);
        check(ModAttachments.BUILD_DATA.get(reloaded).equals(saved), "the saved part comes back: " + ModAttachments.BUILD_DATA.get(reloaded));
        check(ModAttachments.BUILD_VIEW.get(reloaded).equals(BuildView.EMPTY), "build mode is never saved");
        ServerPlayer old = f.player(8.5, FLOOR, 12.5);
        old.load(before31);
        check(ModAttachments.BUILD_DATA.get(old).equals(BuildData.EMPTY), "a player saved before 3.1 reads as empty: "
                + ModAttachments.BUILD_DATA.get(old));

        // Who is sent what, once Wanda sees Billy. (The first sight of a player sends all his synced data, whoever it is
        // for: NeoForge's pairing does not ask the attachment; every change after it goes only where the attachment says.)
        see(helper.getLevel(), wanda, billy);
        TestKit.sentPackets(billy);
        TestKit.sentPackets(wanda);
        ModAttachments.BUILD_DATA.set(billy, data.withHistory(5, 0));
        check(synced(TestKit.sentPackets(billy), ModAttachments.BUILD_DATA, billy.getId()), "Billy is sent his own data");
        check(!synced(TestKit.sentPackets(wanda), ModAttachments.BUILD_DATA, billy.getId()), "nobody else is");
        ModAttachments.BUILD_VIEW.set(billy, view.withActive(false));
        check(synced(TestKit.sentPackets(billy), ModAttachments.BUILD_VIEW, billy.getId()), "Billy is sent his view");
        check(synced(TestKit.sentPackets(wanda), ModAttachments.BUILD_VIEW, billy.getId()), "and so is whoever sees him");
        helper.succeed();
    }

    /**
     * What {@code watcher}'s client would do to see {@code seen} (nothing ticks a test player's connection): stand where
     * it stands, take the chunks the server has for it, say each batch arrived, and look round.
     */
    private static void see(ServerLevel level, ServerPlayer watcher, ServerPlayer seen) {
        level.getChunkSource().move(watcher);
        long chunk = seen.chunkPosition().toLong();
        for (int batch = 0; batch < 100 && watcher.connection.chunkSender.isPending(chunk); batch++) {
            watcher.connection.chunkSender.sendNextChunks(watcher);
            watcher.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
        }
        level.getChunkSource().move(watcher);
        check(level.getChunkSource().chunkMap.getPlayersWatching(seen).contains(watcher), "the watcher sees the player");
    }

    /** Whether {@code packets} hold a sync of {@code key} for the entity {@code entityId}. */
    private static boolean synced(List<Object> packets, AttachKey<?> key, int entityId) {
        return packets.stream().anyMatch(packet -> TestHooks.get().isAttachmentSync(packet, key, entityId));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The telekinesis seams (S10), the new rays (S11) and Billy's crown (S12)

    /** {@code Telekinesis.lastGripEnd} is the game time of a set-down, of a throw, and of a grip lost to distance. */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_grip", timeoutTicks = 40)
    public static void lastGripEndIsRecorded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(6.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        check(Telekinesis.lastGripEnd(wanda) == Telekinesis.NEVER, "no grip of hers has ended yet");

        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 10.5));
        Telekinesis.hold(wanda, pig, Telekinesis.Mode.WANDA);
        wanda.setShiftKeyDown(true);
        Telekinesis.press(wanda, Telekinesis.Mode.WANDA);
        long setDown = level.getGameTime();
        check(!Telekinesis.isHolding(wanda) && Telekinesis.lastGripEnd(wanda) == setDown, "a set-down: " + Telekinesis.lastGripEnd(wanda));
        wanda.setShiftKeyDown(false);

        helper.runAfterDelay(2, () -> {
            Telekinesis.hold(wanda, pig, Telekinesis.Mode.WANDA);
            TestKit.lookAt(wanda, pig);
            Telekinesis.press(wanda, Telekinesis.Mode.WANDA);
            long thrown = level.getGameTime();
            check(thrown > setDown && !Telekinesis.isHolding(wanda) && Telekinesis.lastGripEnd(wanda) == thrown,
                    "a throw: " + Telekinesis.lastGripEnd(wanda));
            pig.discard();
        });
        helper.runAfterDelay(4, () -> {
            Pig other = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 12.5));
            Telekinesis.hold(wanda, other, Telekinesis.Mode.WANDA);
            Vec3 at = wanda.position();
            wanda.setPos(at.x, at.y + 60.0, at.z); // far out of reach (48 blocks), in the same chunks
            PowerTicker.tick(wanda);
            wanda.setPos(at.x, at.y, at.z);
            long lost = level.getGameTime();
            check(!Telekinesis.isHolding(wanda) && Telekinesis.lastGripEnd(wanda) == lost, "a grip lost to distance: "
                    + Telekinesis.lastGripEnd(wanda) + " at " + lost);
            other.discard();
            helper.succeed();
        });
    }

    /**
     * Magneto's aim finds a rail past tall grass and over a carpet and a bottom slab, and stops at a stone wall; sight goes
     * over a carpet and a slab, not through stone; the picking ray finds a poppy where the ordinary one finds the grass.
     */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_rays")
    public static void raycastsSeeRailsAndStopAtWalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5);
        BlockPos rail = helper.absolutePos(new BlockPos(9, 2, 10));
        BlockPos wall = helper.absolutePos(new BlockPos(8, 2, 10));
        level.setBlock(rail, Blocks.RAIL.defaultBlockState(), Block.UPDATE_ALL);
        Vec3 onTheRail = Vec3.atLowerCornerOf(rail).add(0.5, 0.0625, 0.5);
        TestKit.lookAt(magneto, onTheRail);

        BlockHitResult aim = Raycasts.metalBlock(magneto, 24.0, 1.0F);
        check(aim.getType() == HitResult.Type.BLOCK && aim.getBlockPos().equals(rail), "Magneto's aim finds the rail: " + aim.getBlockPos());
        check(!Raycasts.block(magneto, 24.0, 1.0F).getBlockPos().equals(rail), "where the ordinary ray passes through it");
        check(Raycasts.inSight(magneto, rail) && Raycasts.inSight(magneto, Vec3.atCenterOf(rail)), "he sees the rail across the air");

        BlockPos grass = helper.absolutePos(new BlockPos(4, 2, 10));
        level.setBlock(grass.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(grass, Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(grass.above(), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(helper.absolutePos(new BlockPos(6, 2, 10)), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(helper.absolutePos(new BlockPos(7, 2, 10)), Blocks.WHITE_CARPET.defaultBlockState(), Block.UPDATE_ALL);
        aim = Raycasts.metalBlock(magneto, 24.0, 1.0F);
        check(aim.getType() == HitResult.Type.BLOCK && aim.getBlockPos().equals(rail),
                "past tall grass and over a bottom slab and a carpet: " + aim.getBlockPos());
        check(Raycasts.inSight(magneto, rail), "he still sees it over the slab and the carpet");

        level.setBlock(wall, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        aim = Raycasts.metalBlock(magneto, 24.0, 1.0F);
        check(aim.getType() == HitResult.Type.BLOCK && aim.getBlockPos().equals(wall), "a stone block stops the aim: " + aim.getBlockPos());
        check(!Raycasts.inSight(magneto, rail) && !Raycasts.inSight(magneto, Vec3.atCenterOf(rail)), "and the sight");
        check(Raycasts.metalBlock(magneto, 3.0, 1.0F).getType() == HitResult.Type.MISS, "short of anything: a miss");

        // The picking ray.
        ServerPlayer builder = f.player(5.5, FLOOR, 12.5);
        for (int x = 4; x <= 6; x++) {
            for (int z = 14; z <= 15; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 1, z)), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        BlockPos poppy = helper.absolutePos(new BlockPos(5, 2, 14));
        level.setBlock(poppy, Blocks.POPPY.defaultBlockState(), Block.UPDATE_ALL);
        // A flower stands a little off the middle of its cell: aim at the flower itself.
        TestKit.lookAt(builder, Vec3.atLowerCornerOf(poppy).add(level.getBlockState(poppy).getShape(level, poppy).bounds().getCenter()));
        BlockHitResult picked = Raycasts.outline(builder, 24.0, 1.0F);
        BlockHitResult clicked = Raycasts.block(builder, 24.0, 1.0F);
        check(picked.getType() == HitResult.Type.BLOCK && picked.getBlockPos().equals(poppy), "the picking ray finds the poppy: " + picked.getBlockPos());
        check(clicked.getType() == HitResult.Type.BLOCK && level.getBlockState(clicked.getBlockPos()).is(Blocks.GRASS_BLOCK),
                "the ordinary ray the grass behind it: " + level.getBlockState(clicked.getBlockPos()));
        helper.succeed();
    }

    /** Billy's crown stays on while his {@code BuildView} is active; out of build mode it goes when its time is over. */
    @EnhancedTest(template = ARENA, batch = "v31_skeleton_crown", timeoutTicks = 160)
    public static void billysCrownFollowsBuildView(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer billy = f.player(10.5, FLOOR, 10.5, Power.BILLY);
        PowerData data = Powers.get(billy);
        BillyAbilities.tick(billy, data);
        check(!data.is(Live.CROWNED), "no crown before he uses his magic");

        ModAttachments.BUILD_VIEW.set(billy, BuildView.EMPTY.withActive(true));
        BillyAbilities.tick(billy, data);
        check(data.is(Live.CROWNED), "build mode on: the crown forms");
        helper.runAfterDelay(70, () -> {
            BillyAbilities.tick(billy, data);
            check(data.is(Live.CROWNED), "and stays as long as he builds");

            AbilityHandler.onKey(billy, 5, true); // The Noise: a spell, so the crown has its three seconds
            ModAttachments.BUILD_VIEW.set(billy, BuildView.EMPTY);
            BillyAbilities.tick(billy, data);
            check(data.is(Live.CROWNED), "build mode off right after a spell: the crown keeps its time");
        });
        helper.runAfterDelay(70 + BillyAbilities.CROWN_TICKS + 1, () -> {
            BillyAbilities.tick(billy, data);
            check(!data.is(Live.CROWNED), "and goes when its time is over");
            AbilityHandler.onKey(billy, 5, true);
            helper.succeed();
        });
    }

    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        T decoded = codec.decode(buf);
        check(buf.readableBytes() == 0, "the codec read everything it wrote (" + buf.readableBytes() + " bytes left)");
        return decoded;
    }
}
