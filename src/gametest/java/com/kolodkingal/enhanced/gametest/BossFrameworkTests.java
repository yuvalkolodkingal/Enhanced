package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestArena;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestLane;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The boss framework: how a fight starts, who joins it, how it is lost, won, abandoned and wound down, who is rewarded
 * and how the reward reaches them. A {@link TestFight} stands in for the boss, so none of this needs a real one. Every
 * test has a batch of its own: a fight is server-wide state, and one fight may run at a time.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossFrameworkTests {
    /** The middle of the test floor: every arena is a circle round it. */
    static final Vec3 CENTRE = new Vec3(10.5, FLOOR, 10.5);

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A fixture with the server's announcements off (a fight would fill the chat of the test server). */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        return f;
    }

    /** An arena round the middle of the floor: protected radius 8, the field or wall at 6, players join within 9. */
    static BossArena arena(TestKit.Fixture f, Containment containment, boolean protectBlocks) {
        return new TestArena(f.level(), f.helper().absoluteVec(CENTRE), 8.0, containment, protectBlocks)
                .containAt(6.0).joinWithin(9.0).tractorDepth(0.0);
    }

    static TestFight fight(TestKit.Fixture f, BossKind kind, Containment containment, boolean protectBlocks) {
        return new TestFight(f.level(), kind, arena(f, containment, protectBlocks));
    }

    /** Registers a lane for the kind that makes this fight, and starts it the way the rituals do. */
    static TestFight start(TestKit.Fixture f, BossKind kind, ServerPlayer starter, TestFight fight) {
        BossFights.register(kind, new TestLane(kind, p -> fight));
        check(BossFights.start(kind, starter, false), "the " + kind + " fight starts");
        return fight;
    }

    static TestFight start(TestKit.Fixture f, ServerPlayer starter, Containment containment, boolean protectBlocks) {
        return start(f, BossKind.ULTRON, starter, fight(f, BossKind.ULTRON, containment, protectBlocks));
    }

    /** Moves a player to a spot of the test floor. */
    static void moveTo(GameTestHelper helper, ServerPlayer player, double x, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, FLOOR, z));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0.0F, 0.0F);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = BossFight.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Starting a fight

    @GameTest(template = ARENA, batch = "boss_no_lane")
    public static void aBossWithNoLaneRefusesPolitely(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        for (BossKind kind : BossKind.values()) {
            BossFights.unregister(kind);
            check(!BossFights.start(kind, player, true), kind + " has no lane: not even an operator can wake it");
            check("message.enhanced.boss.unavailable".equals(lastActionBarKey(player)), "and the player is told so");
        }
        check(!BossFights.anyActive(helper.getLevel().getServer()), "no fight runs");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_start_rules")
    public static void aStartMeetsTheSharedRulesBeforeTheLaneAndBeforeAnythingIsUsedUp(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        TestFight ultron = fight(f, BossKind.ULTRON, Containment.NONE, false);
        TestLane lane = new TestLane(BossKind.ULTRON, p -> ultron);
        BossFights.register(BossKind.ULTRON, lane);

        // The lane's own checks come last, and a refusal makes nothing.
        lane.refusal = Component.translatable("message.enhanced.test.refusal");
        check(!BossFights.start(BossKind.ULTRON, player, false) && lane.refusals == 1 && lane.creates == 0, "the lane refuses");
        check("message.enhanced.test.refusal".equals(lastActionBarKey(player)), "and its reason is what the player reads");
        lane.refusal = null;
        lane.returnsNull = true;
        check(!BossFights.start(BossKind.ULTRON, player, false) && !BossFights.anyActive(server), "a lane that makes nothing starts nothing");
        lane.returnsNull = false;

        // Peaceful: the boss would vanish at once. The lane is not even asked.
        Difficulty difficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.PEACEFUL, true);
        int asked = lane.refusals;
        boolean peaceful = BossFights.start(BossKind.ULTRON, player, true);
        server.setDifficulty(difficulty, true);
        check(!peaceful && "message.enhanced.boss.peaceful".equals(lastActionBarKey(player)) && lane.refusals == asked,
                "Peaceful refuses, even by force, before the lane is asked");

        // One at a time, unless the server says more.
        check(BossFights.start(BossKind.ULTRON, player, false) && lane.lastForce == false, "the first fight starts");
        TestFight thanos = fight(f, BossKind.THANOS, Containment.NONE, false);
        TestLane thanosLane = new TestLane(BossKind.THANOS, p -> thanos);
        BossFights.register(BossKind.THANOS, thanosLane);
        check(!BossFights.start(BossKind.THANOS, player, true) && thanosLane.refusals == 0, "a second is refused, by force too");
        check("message.enhanced.boss.busy".equals(lastActionBarKey(player)), "with the reason");
        f.config(BossConfig.MAX_ACTIVE_FIGHTS, 2);
        check(BossFights.start(BossKind.THANOS, player, false), "unless maxActiveFights allows two");
        check(BossFights.all(server).size() == 2 && BossFights.active(helper.getLevel()) == ultron, "both are running, the oldest first");
        BossFights.clear();

        // The rest after a fight: a start waits, an operator's does not.
        BossFights.rest(server, BossKind.APOCALYPSE, server.overworld().getGameTime() + 2400);
        TestFight apocalypse = fight(f, BossKind.APOCALYPSE, Containment.NONE, false);
        TestLane apocalypseLane = new TestLane(BossKind.APOCALYPSE, p -> apocalypse);
        BossFights.register(BossKind.APOCALYPSE, apocalypseLane);
        check(BossFights.restLeft(server, BossKind.APOCALYPSE) > 2300, "the boss rests");
        check(!BossFights.start(BossKind.APOCALYPSE, player, false) && apocalypseLane.refusals == 0, "so a ritual is refused");
        check("message.enhanced.boss.resting".equals(lastActionBarKey(player)), "and told how long");
        check(BossFights.start(BossKind.APOCALYPSE, player, true) && apocalypseLane.lastForce, "an operator's start is not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_begin")
    public static void aFightBeginsWithItsStarterTheBarsAndTheTickets(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer starter = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, starter, Containment.NONE, true);

        check(fight.log.equals(List.of("begin")) && BossFights.active(level) == fight && fight.starter().equals(starter.getUUID()),
                "the lane is told, and the fight is the dimension's");
        check(fight.participants().size() == 1 && fight.isParticipant(starter.getUUID()) && fight.isMember(starter), "the starter is its first participant");
        check(fight.bars().health().getPlayers().contains(starter) && fight.bars().health().getColor().getName().equals("red"),
                "the bars are shown to them");
        ChunkPos middle = new ChunkPos(BlockPos.containing(helper.absoluteVec(CENTRE)));
        check(level.getChunkSource().chunkMap.getDistanceManager().shouldForceTicks(middle.toLong()), "the arena's chunks are held and ticking");
        check(fight.arena().protects(BlockPos.containing(helper.absoluteVec(CENTRE))), "and its blocks are protected");

        f.everyTick(20, tick -> {
            if (fight.ticks < 5) {
                return false;
            }
            check(fight.age() >= 5, "the fight ticks with its level");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Participants and the ledger

    @GameTest(template = ARENA, batch = "boss_ledger")
    public static void playersJoinByDistanceAndTheLedgerCountsWhatTheyDo(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer starter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer near = f.player(14.5, FLOOR, 10.5);
        ServerPlayer creative = f.player(6.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer spectator = f.player(10.5, FLOOR, 14.5, GameType.SPECTATOR);
        ServerPlayer far = f.player(1.5, FLOOR, 1.5);
        TestFight fight = start(f, starter, Containment.NONE, false);

        helper.runAfterDelay(3, () -> {
            check(fight.isParticipant(near.getUUID()) && fight.isParticipant(creative.getUUID()), "whoever comes near joins, Creative too");
            check(!fight.isParticipant(spectator.getUUID()) && !fight.isParticipant(far.getUUID()), "a Spectator and a stranger do not");
            fight.credit(starter, BossFight.DAMAGE, 10.0F);
            fight.credit(starter, "shield", 4.0F);
            fight.credit(near, BossFight.DAMAGE, 3.0F);
            fight.credit(far, BossFight.DAMAGE, 99.0F);
            BossFight.Participant a = fight.participant(starter.getUUID());
            check(a.damage() == 10.0F && a.counter("shield") == 4.0F && a.counter("nothing") == 0.0F, "the ledger keeps named counters");
            check(fight.participant(near.getUUID()).damage() == 3.0F && fight.participant(far.getUUID()) == null, "and ignores strangers");
            check(a.ticksPresent() >= 1 && a.secondsPresent() == a.ticksPresent() / 20, "it counts the time they are there");

            fight.lastBlow(near);
            fight.lastBlow(starter);
            check(a.struckLastBlow() && !fight.participant(near.getUUID()).struckLastBlow(), "one player struck the last blow");

            moveTo(helper, far, 8.5, 8.5);
            int before = fight.participant(starter.getUUID()).ticksPresent();
            moveTo(helper, starter, 1.5, 1.5);
            helper.runAfterDelay(4, () -> {
                check(fight.isParticipant(far.getUUID()), "a stranger who walks in joins");
                check(fight.participant(starter.getUUID()).ticksPresent() <= before + 1, "and whoever leaves stops counting");
                check(fight.isParticipant(starter.getUUID()), "yet stays a participant");
                BossFights.stopAll(helper.getLevel().getServer(), StopReason.COMMAND);
                fight.credit(near, BossFight.DAMAGE, 50.0F);
                check(fight.participant(near.getUUID()).damage() == 3.0F, "nothing is credited after the fight ended");
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Losing, abandoning and timing out

    @GameTest(template = ARENA, batch = "boss_wipe", timeoutTicks = 120)
    public static void aFightIsWipedWhenEveryFighterIsDeadForWipeSeconds(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 1);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        ServerPlayer b = f.player(12.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        helper.runAfterDelay(3, () -> {
            a.kill();
            check(!fight.isOver(), "one fighter down is no wipe");
            helper.runAfterDelay(30, () -> check(!fight.isOver(), "while another stands"));
            helper.runAfterDelay(35, b::kill);
        });
        f.everyTick(110, tick -> {
            if (fight.result() == null) {
                return false;
            }
            check(fight.result() == BossEnd.WIPE && fight.log.contains("end:WIPE"), "the fight ended as a wipe");
            check(fight.log.stream().noneMatch(line -> line.startsWith("reward")), "nobody is rewarded");
            return true;
        });
    }

    /**
     * A wipe is told to the fallen: when everyone is dead, nobody stands near the bars any more (the dead leave the
     * level a second after they die), and the line still reaches every participant.
     */
    @GameTest(template = ARENA, batch = "boss_wipe_told", timeoutTicks = 160)
    public static void theFallenAreToldOfTheWipe(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 3);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        ServerPlayer b = f.player(12.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        helper.runAfterDelay(3, () -> {
            check(fight.isParticipant(b.getUUID()), "both fight");
            a.kill();
            b.kill();
            TestKit.sentPackets(a);
            TestKit.sentPackets(b);
        });
        List<Object> toA = new java.util.ArrayList<>();
        List<Object> toB = new java.util.ArrayList<>();
        f.everyTick(150, tick -> {
            if (tick <= 3) {
                return false;
            }
            // (What a connected client's tick does: the dead are taken out of the level after their death.)
            for (ServerPlayer dead : List.of(a, b)) {
                if (!dead.isRemoved()) {
                    dead.doTick();
                }
            }
            toA.addAll(TestKit.sentPackets(a));
            toB.addAll(TestKit.sentPackets(b));
            if (tick == 50) {
                check(!fight.isOver(), "the wipe takes its time");
                check(a.isRemoved() && b.isRemoved() && !f.level().players().contains(a), "the dead have left the level");
                check(fight.audience().contains(a) && fight.audience().contains(b),
                        "but they still hear the boss from the death screen (his last line over a wipe is for them)");
            }
            if (!fight.isOver()) {
                return false;
            }
            check(fight.result() == BossEnd.WIPE, "a wipe: " + fight.result());
            check(told(toA, "message.enhanced.boss.end.wipe") && told(toB, "message.enhanced.boss.end.wipe"),
                    "both fallen fighters read that everyone has fallen");
            return true;
        });
    }

    /** Whether a chat line (not the action bar) with that translation key is among the packets. */
    private static boolean told(List<Object> packets, String key) {
        return packets.stream().anyMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat
                && !chat.overlay() && chat.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals(key));
    }

    /**
     * A fighter who walks out of the arena alive has not fallen: the fight is not wiped after the wipe time, and if
     * nobody comes back it is abandoned (the boss loses interest), with its own words.
     */
    @GameTest(template = ARENA, batch = "boss_walk_away", timeoutTicks = 200)
    public static void walkingAwayAliveIsAnAbandonmentNotAWipe(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 1);
        f.config(BossConfig.ABANDON_SECONDS, 5);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        // (The real arenas let go of a fighter some way beyond their join radius; this one is small enough to walk out of.)
        fight.arena().abandonBeyond(4.0);
        helper.runAfterDelay(3, () -> moveTo(helper, a, 1.5, 1.5));
        helper.runAfterDelay(4, () -> {
            check(fight.isParticipant(a.getUUID()) && !fight.present().contains(a) && fight.fighters().contains(a),
                    "he is out of the arena, alive");
            TestKit.sentPackets(a);
        });
        helper.runAfterDelay(60, () -> check(!fight.isOver(), "three times the wipe time later, walking away is no wipe: " + fight.result()));
        List<Object> toA = new java.util.ArrayList<>();
        f.everyTick(190, tick -> {
            if (tick > 4) {
                toA.addAll(TestKit.sentPackets(a));
            }
            if (fight.result() == null) {
                return false;
            }
            check(tick >= 100, "not before the abandon time: " + tick);
            check(fight.result() == BossEnd.ABANDON, "nobody near: abandoned, not " + fight.result());
            check(told(toA, "message.enhanced.boss.end.abandon") && !told(toA, "message.enhanced.boss.end.wipe"),
                    "he reads that nobody is left near, not that everyone has fallen");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_wipe_creative", timeoutTicks = 140)
    public static void creativePlayersCannotDieSoAFightOfOnlyThemNeverWipes(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 1);
        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        TestFight fight = start(f, creative, Containment.NONE, false);
        helper.runAfterDelay(60, () -> {
            check(!fight.isOver(), "a Creative fighter alone never wipes the fight");
            ServerPlayer mortal = f.player(12.5, FLOOR, 10.5);
            helper.runAfterDelay(3, () -> {
                check(fight.isParticipant(mortal.getUUID()), "a survival player joins");
                mortal.kill();
                helper.runAfterDelay(30, () -> {
                    check(fight.result() == BossEnd.WIPE, "and when they die, the Creative one does not keep the fight alive");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "boss_abandon", timeoutTicks = 120)
    public static void aFightIsAbandonedWhenNobodyIsNearForAbandonSeconds(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ABANDON_SECONDS, 1);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        // (The arena's abandon radius is 64 in a real fight; this one is made small so the player can walk out of it.)
        fight.arena().abandonBeyond(4.0);
        helper.runAfterDelay(5, () -> check(!fight.isOver(), "a player at the centre keeps it"));
        helper.runAfterDelay(6, () -> moveTo(helper, a, 1.5, 1.5));
        f.everyTick(100, tick -> {
            if (fight.result() == null) {
                return false;
            }
            check(fight.result() == BossEnd.ABANDON, "nobody near: abandoned, not " + fight.result());
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_time_out", timeoutTicks = 60)
    public static void aFightThatLastsTooLongIsEnded(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.MAX_FIGHT_MINUTES, 1);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        helper.runAfterDelay(3, () -> check(!fight.isOver(), "a minute has not passed"));
        helper.runAfterDelay(4, () -> setField(fight, "age", 1198L));
        f.everyTick(40, tick -> {
            if (fight.result() == null) {
                return false;
            }
            check(fight.result() == BossEnd.TIME_OUT && fight.age() >= 1200, "after maxFightMinutes it times out");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_peaceful", timeoutTicks = 60)
    public static void aFightEndsWhenTheWorldGoesPeacefulAndTheItemComesBack(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty difficulty = server.getWorldData().getDifficulty();
        f.onClose(() -> server.setDifficulty(difficulty, true));
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, a, Containment.NONE, false);
        fight.refundOnFailure(new ItemStack(Items.NETHER_STAR));
        helper.runAfterDelay(3, () -> server.setDifficulty(Difficulty.PEACEFUL, true));
        f.everyTick(30, tick -> {
            if (fight.result() == null) {
                return false;
            }
            server.setDifficulty(difficulty, true);
            check(fight.result() == BossEnd.PEACEFUL, "Peaceful ends the fight");
            check(a.getInventory().contains(new ItemStack(Items.NETHER_STAR)), "and gives back what the ritual used up");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Refunds follow the ending

    @GameTest(template = ARENA, batch = "boss_refund")
    public static void theRitualItemComesBackOnlyWhenTheEndingIsNotThePlayersDoing(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        for (BossEnd why : BossEnd.values()) {
            TestFight fight = start(f, a, Containment.NONE, false);
            ItemStack star = new ItemStack(Items.NETHER_STAR);
            fight.refundOnFailure(star);
            a.getInventory().clearContent();
            fight.end(why);
            boolean returned = a.getInventory().contains(new ItemStack(Items.NETHER_STAR));
            boolean waiting = !BossGifts.pending(server, a.getUUID()).isEmpty();
            if (why == BossEnd.SERVER_STOPPING) {
                check(!returned && waiting, why + ": the item waits as a gift, saved with the world");
                BossGifts.deliver(a);
            } else {
                check(returned == why.refunds() && !waiting, why + ": refunded " + returned + " but the ending says " + why.refunds());
            }
            check(why.refunds() == (why == BossEnd.WITHDRAW || why == BossEnd.COMMAND || why == BossEnd.PEACEFUL || why == BossEnd.SERVER_STOPPING),
                    why + " refunds as the specs say");
            check(why.rewards() == (why == BossEnd.VICTORY), "only a victory rewards");
            BossFights.clear();
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Winding down and letting go

    @GameTest(template = ARENA, batch = "boss_wind_down", timeoutTicks = 200)
    public static void anEndedFightStaysProtectedUntilTheLaneIsDone(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.THANOS, Containment.NONE, true);
        fight.windDownNeeded = 40;
        start(f, BossKind.THANOS, a, fight);
        net.minecraft.core.BlockPos middle = BlockPos.containing(helper.absoluteVec(CENTRE));
        ChunkPos chunk = new ChunkPos(middle);
        var stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5)));
        var marked = fight.spawn(stand);

        helper.runAfterDelay(3, () -> {
            fight.end(BossEnd.ABORT);
            fight.end(BossEnd.WIPE);
            check(fight.isOver() && !fight.isFinished() && fight.result() == BossEnd.ABORT, "the first ending is the one that counts");
            check(fight.log.stream().filter(line -> line.startsWith("end")).count() == 1, "and the lane is told once");
            check(BossFights.protects(level, middle) && BossFights.active(level) == fight, "the arena stays protected");
            check(level.getChunkSource().chunkMap.getDistanceManager().shouldForceTicks(chunk.toLong()), "and held");
            check(fight.bars().health().getPlayers().contains(a), "and the bars stay");
        });
        helper.runAfterDelay(25, () -> check(!fight.isFinished() && fight.windDownTicks() > 15, "winding down takes the lane's time"));
        f.everyTick(100, tick -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(fight.finishedCalls == 1 && fight.log.get(fight.log.size() - 1).equals("finished:false"), "the lane is told once, not forced");
            check(!BossFights.protects(level, middle) && BossFights.active(level) == null, "then the arena is free");
            check(!level.getChunkSource().chunkMap.getDistanceManager().shouldForceTicks(chunk.toLong()), "the chunks are let go");
            check(fight.bars().health().getPlayers().isEmpty(), "the bars are gone");
            check(marked.isRemoved(), "and every entity the fight marked");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_wind_down_stuck", timeoutTicks = 60)
    public static void aLaneThatNeverFinishesIsFinishedByForce(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.ULTRON, Containment.NONE, true);
        fight.windDownNeeded = Integer.MAX_VALUE;
        start(f, BossKind.ULTRON, a, fight);
        helper.runAfterDelay(2, () -> {
            fight.end(BossEnd.ABORT);
            setField(fight, "windDownTicks", BossFight.MAX_WIND_DOWN_TICKS - 3);
        });
        f.everyTick(40, tick -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(BossFights.all(helper.getLevel().getServer()).isEmpty(), "after five minutes the framework lets it go");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_stop", timeoutTicks = 100)
    public static void anOperatorsStopWindsDownAndASecondStopOrTheServerStoppingPutsItAllBack(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.ULTRON, Containment.NONE, true);
        fight.windDownNeeded = 1000;
        start(f, BossKind.ULTRON, a, fight);
        BossFights.stopAll(server, StopReason.COMMAND);
        check(fight.result() == BossEnd.COMMAND && !fight.isFinished(), "a stop ends the fight and lets it wind down");
        BossFights.stopAll(server, StopReason.COMMAND);
        check(fight.isFinished() && fight.forced && fight.finishedCalls == 1, "a second stop puts everything back at once");
        check(BossFights.all(server).isEmpty(), "and nothing is left");

        TestFight stopping = fight(f, BossKind.THANOS, Containment.NONE, true);
        stopping.windDownNeeded = 1000;
        start(f, BossKind.THANOS, a, stopping);
        stopping.refundOnFailure(new ItemStack(Items.TOTEM_OF_UNDYING));
        BossFights.stopAll(server, StopReason.SERVER_STOPPING);
        check(stopping.result() == BossEnd.SERVER_STOPPING && stopping.isFinished() && stopping.forced, "the server stopping puts it back at once");
        check(BossGifts.pending(server, a.getUUID()).size() == 1, "and keeps the refund as a gift, saved with the world");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_crash", timeoutTicks = 60)
    public static void aLaneThatThrowsEndsItsFightInsteadOfBreakingTheServer(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = new TestFight(f.level(), BossKind.APOCALYPSE, arena(f, Containment.NONE, true)) {
            @Override
            protected void onTick() {
                throw new IllegalStateException("a bug in a boss lane");
            }
        };
        start(f, BossKind.APOCALYPSE, a, fight);
        f.everyTick(20, tick -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(fight.result() == BossEnd.ABORT && fight.forced, "the fight was aborted and let go");
            check(!BossFights.anyActive(helper.getLevel().getServer()), "and does not tick again");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_rest", timeoutTicks = 60)
    public static void aBossRestsAfterAFightTheLaneAsksAndTheRestIsSaved(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.THANOS, Containment.NONE, false);
        fight.rest = 600;
        start(f, BossKind.THANOS, a, fight);
        fight.end(BossEnd.WIPE);
        long left = BossFights.restLeft(server, BossKind.THANOS);
        check(left > 590 && left <= 600, "the lane's rest begins when the fight ends (" + left + ")");
        check(BossFights.restLeft(server, BossKind.ULTRON) == 0, "and only for that boss");
        check(server.overworld().getData(ModAttachments.BOSS_DATA).restUntil(BossKind.THANOS) > 0, "and is part of the saved data");
        helper.succeed();
    }

    private BossFrameworkTests() {
    }
}
