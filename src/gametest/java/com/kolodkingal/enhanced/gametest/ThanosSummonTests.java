package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.ArenaCheck;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosLane;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSummon;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Calling the Mad Titan: the two items held up together, the refusals, the site check, the countdown and the arrival.
 * The test arena has a barrier roof, which no real site would pass, so the tests that start a fight switch the site check
 * off ({@link ThanosLane#checkSite}) and the check of the site itself is tested on a fake world.
 */
public final class ThanosSummonTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A boss fixture on Normal difficulty with the site check off, a short countdown and the announcement silent. */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty difficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.NORMAL, true);
        f.onClose(() -> server.setDifficulty(difficulty, true));
        boolean check = ThanosLane.checkSite;
        ThanosLane.checkSite = false;
        f.onClose(() -> ThanosLane.checkSite = check);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(ThanosBossConfig.ARENA_RADIUS, 16);
        f.config(ThanosBossConfig.SUMMON_COUNTDOWN_TICKS, 20);
        return f;
    }

    private static Item scepter() {
        return ModItems.LOKIS_SCEPTER.get();
    }

    private static Item tesseract() {
        return ModItems.TESSERACT.get();
    }

    /** Gives the player the two items, the first in the main hand. */
    static void hold(ServerPlayer player, Item main, Item off) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(main));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(off));
    }

    /** The use of the item in the main hand, as the game does it when the player right-clicks. */
    static InteractionResultHolder<ItemStack> use(TestKit.Fixture f, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem().use(f.level(), player, InteractionHand.MAIN_HAND);
    }

    /** The translation key of a message, or null. */
    @Nullable
    static String key(@Nullable Component message) {
        return message != null && message.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
    }

    /** The kinds of the Thanos effects among the packets. */
    static List<Integer> fxKinds(List<Object> packets) {
        List<Integer> kinds = new ArrayList<>();
        for (Object packet : packets) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof ThanosFxPayload fx) {
                kinds.add(fx.kind());
            }
        }
        return kinds;
    }

    private static boolean has(ServerPlayer player, Item item) {
        return player.getInventory().contains(new ItemStack(item)) || player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The hold

    /** Either item in the main hand and the other in the off hand, used: a hold begins and nothing else of the old uses runs. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_start")
    public static void bothItemsAndAHoldStartTheRitual(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer tess = f.player(6.5, FLOOR, 6.5);
        ServerPlayer scep = f.player(14.5, FLOOR, 14.5);
        hold(tess, tesseract(), scepter());
        hold(scep, scepter(), tesseract());
        for (ServerPlayer player : List.of(tess, scep)) {
            Vec3 before = player.position();
            InteractionResultHolder<ItemStack> result = use(f, player);
            check(result.getResult() == InteractionResult.CONSUME, "the use is taken as a hold: " + result.getResult());
            check(player.isUsingItem(), "he holds the item up");
            check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "the Scepter did not explode");
            check(player.position().equals(before), "the Tesseract did not take him anywhere");
            check(Powers.power(player) == Power.NONE, "the Tesseract did not make him Loki");
            check(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 1, "nothing was used up");
        }
        check(BossFights.active(f.level()) == null, "no fight yet: the five seconds are not up");
        helper.succeed();
    }

    /** The old uses are unchanged by the new row: the Scepter and the Gene, the Scepter alone in Creative and in Survival. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_old_uses")
    public static void theOldUsesAreUnchanged(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = f.player(6.5, FLOOR, 6.5);
        hold(wanda, scepter(), ModItems.SCARLET_WITCH_GENE.get());
        use(f, wanda);
        check(Powers.power(wanda) == Power.SCARLET_WITCH, "the Scepter and the Gene still wake the Scarlet Witch: " + Powers.power(wanda));
        check(!wanda.isUsingItem(), "no hold began");
        ServerPlayer creative = f.player(14.5, FLOOR, 6.5, GameType.CREATIVE);
        hold(creative, scepter(), net.minecraft.world.item.Items.STICK);
        use(f, creative);
        check(creative.isAlive() && !creative.isUsingItem(), "the Scepter alone is safe in Creative, and no hold began");
        ServerPlayer mortal = f.player(10.5, FLOOR, 14.5);
        mortal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(scepter()));
        use(f, mortal);
        check(mortal.isDeadOrDying(), "the Scepter alone still tears a Survival player apart");
        check(BossFights.active(f.level()) == null, "no fight began");
        helper.succeed();
    }

    /** Letting go before the end, or swapping an item out of a hand, cancels: no fight is pending and no rest begins. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_cancel", timeoutTicks = 200)
    public static void releasingOrSwappingCancels(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer letGo = f.player(6.5, FLOOR, 6.5);
        ServerPlayer swapper = f.player(14.5, FLOOR, 14.5);
        hold(letGo, scepter(), tesseract());
        hold(swapper, tesseract(), scepter());
        use(f, letGo);
        use(f, swapper);
        check(letGo.isUsingItem() && swapper.isUsingItem(), "both hold");
        String[] swapperKey = {null};
        f.everyTick(125, t -> {
            letGo.doTick();
            swapper.doTick();
            if (t == 30) {
                letGo.releaseUsingItem();
                check(!letGo.isUsingItem(), "he let go");
                swapper.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                TestKit.sentPackets(swapper);
            }
            if (t == 31) {
                check(!swapper.isUsingItem(), "the swapped item ends the hold");
                swapperKey[0] = BossTestSupport.lastActionBarKey(swapper);
            }
            if (t < 125) {
                return false;
            }
            check("message.enhanced.thanos.summon.need_both".equals(swapperKey[0]), "he is told both are needed: " + swapperKey[0]);
            check(BossFights.active(f.level()) == null, "no fight was called");
            check(BossFights.restLeft(f.level().getServer(), BossKind.THANOS) == 0L, "no rest began");
            check(has(letGo, scepter()) && has(letGo, tesseract()), "the items are still his");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The site

    /** A world made of numbers, for the check of the site. */
    private static final class FakeWorld implements ArenaCheck.World {
        int ground = 64;
        int roof = Integer.MAX_VALUE;
        double border = 1.0E7;
        /** Columns that are walled in from the ground up. */
        BiPredicate<Integer, Integer> wall = (x, z) -> false;
        /** Columns with water on top of the ground. */
        BiPredicate<Integer, Integer> water = (x, z) -> false;

        @Override
        public boolean solidGround(int x, int y, int z) {
            if (wall.test(x, z)) {
                return y < ground + 10;
            }
            return y < ground - 1 || y == ground - 1 && !water.test(x, z);
        }

        @Override
        public boolean free(int x, int y, int z) {
            if (y == roof) {
                return false;
            }
            if (wall.test(x, z) && y < ground + 10) {
                return false;
            }
            return y >= ground;
        }

        @Override
        public boolean liquid(int x, int y, int z) {
            return water.test(x, z) && y == ground - 1;
        }

        @Override
        public boolean seesSky(int x, int y, int z) {
            return roof == Integer.MAX_VALUE;
        }

        @Override
        public boolean inBorder(double x, double z) {
            return Math.abs(x) <= border && Math.abs(z) <= border;
        }

        @Override
        public int minY() {
            return 0;
        }

        @Override
        public int maxY() {
            return 256;
        }
    }

    private static ArenaCheck.Verdict verdict(FakeWorld world, BlockPos feet) {
        return ArenaCheck.verdict(world, feet, ThanosBossConfig.SUMMON_SKY_BLOCKS.get(), ThanosBossConfig.SUMMON_OPEN_FRACTION.get(),
                ThanosBossConfig.ARENA_RADIUS.get());
    }

    /** The pure verdicts: open ground and sky, a roof, water underfoot, a closed ring, the world border. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_site")
    public static void arenaCheckVerdicts(GameTestHelper helper) {
        BlockPos feet = new BlockPos(0, 64, 0);
        check(verdict(new FakeWorld(), feet) == ArenaCheck.Verdict.OK, "open ground and sky is fine");

        FakeWorld roofed = new FakeWorld();
        roofed.roof = 68;
        check(verdict(roofed, feet) == ArenaCheck.Verdict.NO_SKY, "a roof above is no sky");

        FakeWorld wet = new FakeWorld();
        wet.water = (x, z) -> x == 0 && z == 0;
        check(verdict(wet, feet) == ArenaCheck.Verdict.NO_GROUND, "water underfoot is no ground");

        FakeWorld airy = new FakeWorld();
        check(verdict(airy, new BlockPos(0, 80, 0)) == ArenaCheck.Verdict.NO_GROUND, "the air is no ground");

        FakeWorld closed = new FakeWorld();
        closed.wall = (x, z) -> x * x + z * z > 100;
        check(verdict(closed, feet) == ArenaCheck.Verdict.TOO_CLOSED, "walled in beyond ten blocks is too closed");

        FakeWorld edge = new FakeWorld();
        edge.border = 100.0;
        check(verdict(edge, new BlockPos(95, 64, 0)) == ArenaCheck.Verdict.BORDER, "the arena would cross the border");
        check(verdict(edge, new BlockPos(0, 64, 0)) == ArenaCheck.Verdict.OK, "the middle of a bordered world is fine");

        // Enough standing room: a third of the sample spots closed is still fine at 0.6.
        FakeWorld halfOpen = new FakeWorld();
        halfOpen.wall = (x, z) -> x > 6;
        ArenaCheck.Verdict part = verdict(halfOpen, feet);
        check(part == ArenaCheck.Verdict.OK || part == ArenaCheck.Verdict.TOO_CLOSED, "a half-open place gives a verdict");
        helper.succeed();
    }

    /** He arrives 14 to 20 blocks from the summoner, where there is standing room, and never outside the world border. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_site")
    public static void theArrivalSpotIsFourteenToTwentyBlocksAway(GameTestHelper helper) {
        BlockPos summoner = new BlockPos(0, 64, 0);
        BlockPos spot = ArenaCheck.arrivalSpot(new FakeWorld(), summoner);
        check(spot != null, "there is a spot in the open");
        double distance = Math.hypot(spot.getX(), spot.getZ());
        check(distance >= 14.0 && distance <= 20.5, "14 to 20 blocks away: " + distance);
        check(spot.getY() == 64, "on the ground");

        FakeWorld walled = new FakeWorld();
        walled.wall = (x, z) -> x * x + z * z > 100;
        check(ArenaCheck.arrivalSpot(walled, summoner) == null, "nowhere to stand in a walled place: he comes down on the summoner's ground");

        FakeWorld edge = new FakeWorld();
        edge.border = 30.0;
        BlockPos near = ArenaCheck.arrivalSpot(edge, new BlockPos(20, 64, 0));
        check(near == null || Math.abs(near.getX()) <= 30 && Math.abs(near.getZ()) <= 30, "never outside the border");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The refusals

    /**
     * Each way the call is turned down: the message the player reads, no fight, and the Scepter does not explode and the
     * Tesseract does not take the player away.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_refusals", timeoutTicks = 80)
    public static void theRefusals(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = f.level().getServer();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        hold(player, scepter(), tesseract());

        // Peaceful.
        server.setDifficulty(Difficulty.PEACEFUL, true);
        expectRefusal(f, player, "message.enhanced.thanos.summon.peaceful");
        server.setDifficulty(Difficulty.NORMAL, true);

        // The server turned the call off.
        f.config(ThanosBossConfig.SUMMON_ENABLED, false);
        expectRefusal(f, player, "message.enhanced.thanos.summon.disabled");
        f.config(ThanosBossConfig.SUMMON_ENABLED, true);

        // A spectator cannot call him.
        ServerPlayer ghost = f.player(14.5, FLOOR, 14.5, GameType.SPECTATOR);
        hold(ghost, scepter(), tesseract());
        check("message.enhanced.thanos.summon.disabled".equals(key(ThanosSummon.refusal(ghost))), "a spectator is refused");

        // The rest after the last fight.
        BossFights.rest(server, BossKind.THANOS, f.level().getGameTime() + 1200L);
        Component resting = ThanosSummon.refusal(player);
        check("message.enhanced.thanos.summon.resting".equals(key(resting)), "the rest is not over: " + key(resting));
        check(resting.getContents() instanceof TranslatableContents t && t.getArgs().length == 1 && "1:00".equals(String.valueOf(t.getArgs()[0])),
                "it says how long: " + resting);
        expectRefusal(f, player, "message.enhanced.thanos.summon.resting");
        BossFights.rest(server, BossKind.THANOS, 0L);
        check(ThanosSummon.refusal(player) == null, "with the rest over he can be called");

        // A summoner Mjolnir lies on.
        f.pvp(true);
        ServerPlayer thor = f.player(6.5, FLOOR, 6.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        ServerPlayer pinned = f.player(6.5, FLOOR, 12.5);
        hold(pinned, scepter(), tesseract());
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, pinned);
        check(MjolnirPin.isPinned(pinned), "he is pinned");
        check("message.enhanced.mjolnir.pinned_cannot".equals(key(ThanosSummon.refusal(pinned))), "a pinned summoner is refused");

        // The site: the arena's barrier roof is no sky.
        ThanosLane.checkSite = true;
        expectRefusal(f, player, "message.enhanced.thanos.summon.no_sky");
        ThanosLane.checkSite = false;

        // A fight is on: he is already here.
        ThanosFight fight = ThanosFight.spawnAt(f.level(), f.helper().absoluteVec(ThanosBossTests.CENTRE), player);
        check(fight.boss() != null, "he is made");
        expectRefusal(f, player, "message.enhanced.thanos.summon.active");

        // The Tesseract without the Scepter is not the call at all.
        ServerPlayer lone = f.player(14.5, FLOOR, 6.5);
        lone.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tesseract()));
        check(!ScepterRituals.callsThanos(lone, InteractionHand.MAIN_HAND), "the Tesseract alone is not the call");
        helper.succeed();
    }

    /** Uses the item in the main hand and checks the refusal: its message in the action bar, no hold, no harm, no fight. */
    private static void expectRefusal(TestKit.Fixture f, ServerPlayer player, String expected) {
        BossFight before = BossFights.active(f.level());
        Vec3 at = player.position();
        TestKit.sentPackets(player);
        use(f, player);
        String shown = BossTestSupport.lastActionBarKey(player);
        check(expected.equals(shown), "the player reads " + expected + " but read " + shown);
        check(!player.isUsingItem(), expected + ": no hold began");
        check(player.isAlive() && player.getHealth() == player.getMaxHealth(), expected + ": the Scepter did not explode");
        check(player.position().equals(at) && Powers.power(player) == Power.NONE, expected + ": the Tesseract did nothing");
        check(BossFights.active(f.level()) == before, expected + ": no fight began");
    }

    // ------------------------------------------------------------------------------------------------------------
    // The call, the countdown and the arrival

    /**
     * The whole call: five seconds of holding, the portal over the landing, twenty ticks of countdown, then he is there,
     * arriving and invulnerable for 60 ticks, and then the fight. Neither item is used up, the arena is round the summoner,
     * and a second call is turned down.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_arrival", timeoutTicks = 420)
    public static void theCountdownThenTheArrival(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer summoner = f.player(10.5, FLOOR, 10.5);
        ServerPlayer other = f.player(16.5, FLOOR, 16.5);
        hold(summoner, scepter(), tesseract());
        hold(other, tesseract(), scepter());
        Vec3 calledAt = summoner.position();
        use(f, summoner);
        List<Object> seen = new ArrayList<>();
        ThanosFight[] fight = {null};
        int[] madeAt = {-1};
        int[] risenAt = {-1};
        f.everyTick(400, t -> {
            summoner.doTick();
            seen.addAll(TestKit.sentPackets(summoner));
            if (fight[0] == null) {
                BossFight active = BossFights.active(f.level());
                if (t < 99) {
                    check(active == null, "nothing starts before the five seconds are up: " + t);
                    return false;
                }
                if (active == null) {
                    return false;
                }
                check(active instanceof ThanosFight, "the call makes his fight");
                fight[0] = (ThanosFight) active;
                check(fight[0].phase() == ThanosFight.Phase.COUNTDOWN && fight[0].boss() == null, "the portal is open and he is not here yet");
                check(fxKinds(seen).contains(ThanosFxPayload.PORTAL_SKY), "the portal in the sky is sent to the players");
                check(fight[0].arena().center().distanceTo(calledAt) < 1.5, "the arena is round the summoner");
                check(summoner.getMainHandItem().is(scepter()) && summoner.getOffhandItem().is(tesseract()), "neither item is used up");
                // A second call, by someone else, is turned down: one Thanos at a time.
                check("message.enhanced.thanos.summon.active".equals(key(ThanosSummon.refusal(other))), "he is already called");
                check(!BossFights.start(BossKind.THANOS, other, false) && BossFights.all(f.level().getServer()).size() == 1, "a second fight does not begin");
                return false;
            }
            ThanosFight thanosFight = fight[0];
            if (thanosFight.boss() == null) {
                check(thanosFight.phase() == ThanosFight.Phase.COUNTDOWN, "the countdown runs");
                return false;
            }
            ThanosEntity boss = thanosFight.boss();
            if (madeAt[0] < 0) {
                madeAt[0] = t;
            }
            if (boss.state() == ThanosState.ARRIVING) {
                float health = boss.getHealth();
                ThanosBossTests.hit(boss, summoner, 100.0F);
                check(boss.getHealth() == health, "a hit changes nothing while he arrives");
                check(thanosFight.phase() == ThanosFight.Phase.ARRIVING, "the fight says he arrives");
                return false;
            }
            if (risenAt[0] < 0) {
                risenAt[0] = t;
                check(t - madeAt[0] >= ThanosEntity.ARRIVE_TICKS, "he arrives for at least " + ThanosEntity.ARRIVE_TICKS + " ticks: " + (t - madeAt[0]));
                check(thanosFight.phase() == ThanosFight.Phase.FIGHT, "then the fight");
                check(thanosFight.bars().health().getPlayers().contains(summoner), "the bar shows the fighters near");
                check(summoner.getMainHandItem().is(scepter()) && summoner.getOffhandItem().is(tesseract()), "the items are still there");
            }
            return true;
        });
    }

    /**
     * The summoner who keeps the use key down after the call (the game asks again every four ticks while it is held) is
     * not told "He is already here" over the portal's own line; once he lets go and presses again, he is, and anyone
     * else is at once.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_held", timeoutTicks = 200)
    public static void theSummonersHeldKeyIsNotToldHeIsAlreadyHere(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.SUMMON_CHARGE_TICKS, 40);
        f.config(ThanosBossConfig.SUMMON_COUNTDOWN_TICKS, 120);
        ServerPlayer summoner = f.player(10.5, FLOOR, 10.5);
        ServerPlayer other = f.player(16.5, FLOOR, 16.5);
        hold(summoner, tesseract(), scepter());
        hold(other, tesseract(), scepter());
        use(f, summoner);
        int[] calledAt = {-1};
        f.everyTick(180, t -> {
            summoner.doTick();
            if (calledAt[0] < 0) {
                if (BossFights.active(f.level()) == null) {
                    return false;
                }
                calledAt[0] = t;
                TestKit.sentPackets(summoner);
                return false;
            }
            int since = t - calledAt[0];
            if (since <= 24 && since % 4 == 0) {
                // The key is still down: the game asks again.
                use(f, summoner);
                check(!summoner.isUsingItem(), "no second hold begins");
                String shown = BossTestSupport.lastActionBarKey(summoner);
                check(!"message.enhanced.thanos.summon.active".equals(shown), "the held key is answered with silence, not " + shown);
            }
            if (since == 4) {
                use(f, other);
                check("message.enhanced.thanos.summon.active".equals(BossTestSupport.lastActionBarKey(other)), "anyone else is told he is already here");
            }
            if (since == 50) {
                // He let go twenty ticks ago, and presses again.
                use(f, summoner);
                String shown = BossTestSupport.lastActionBarKey(summoner);
                check("message.enhanced.thanos.summon.active".equals(shown), "a new press is told he is already here: " + shown);
                return true;
            }
            return false;
        });
    }

    /** Held ten ticks short of the end, nothing has happened; the last tick starts it. */
    @EnhancedTest(template = ARENA, batch = "thanos_summon_charge", timeoutTicks = 200)
    public static void aShortChargeCallsHimBeforeTheItemsRunOut(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.SUMMON_CHARGE_TICKS, 40);
        ServerPlayer summoner = f.player(10.5, FLOOR, 10.5);
        hold(summoner, tesseract(), scepter());
        use(f, summoner);
        f.everyTick(60, t -> {
            summoner.doTick();
            if (t < 38) {
                check(BossFights.active(f.level()) == null, "not before the charge: " + t);
                return false;
            }
            if (BossFights.active(f.level()) == null) {
                return false;
            }
            check(t >= 40 && t <= 45, "the charge of 40 ticks calls him: " + t);
            check(!summoner.isUsingItem(), "the hold ended");
            return true;
        });
    }
}
