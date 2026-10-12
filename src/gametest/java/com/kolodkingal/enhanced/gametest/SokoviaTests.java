package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.network.SokoviaLiftPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultron.SokoviaJob;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaLift;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.GROUND;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.LIFT;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_X;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_Z;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.PAD;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.SURFACE;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.Script;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.blocksInTheSky;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.client;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.groundHash;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.in;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.journals;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.pad;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.rel;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.boss;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * The Sokovia lift in its Sky Island mode: the island is a copy built in the sky, everything on the ground disc rides
 * up with it, and the real ground is never edited. Every test here runs on the Sokovia pad (a 43 x 42 x 43 field of
 * natural ground with open sky over it); the island is made smaller than the real one (9 blocks across the rim, 16 up)
 * so that it fits.
 *
 * <p>The players are fake: nothing moves them but the server, so a test plays the part of a connected client by moving
 * each one by the velocity the server last gave them ({@link #client}), which is all a real client does while the lift
 * has them. Each fight test has a batch of its own: a fight is server-wide state.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SokoviaTests {
    // ------------------------------------------------------------------------------------------------------------
    // The whole lift

    /**
     * The whole thing, with the real defaults for everything but the size: from the warning to the island and back. The
     * island is placed in slices that never exceed the budget, in exactly the blocks of the template, the ground is
     * never touched, the sky is clean again after the descent, and everybody is where they were.
     */
    @GameTest(template = PAD, batch = "ultron_lift_full", timeoutTicks = 900)
    public static void theIslandRisesAndLandsExactly(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 500);
        ServerLevel level = f.level();
        ServerPlayer a = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer b = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0, GameType.CREATIVE);
        tank(a);
        long ground = groundHash(helper);
        check(blocksInTheSky(helper) == 0, "the sky over the pad is empty");
        UltronFight fight = onTheGround(f, a);
        calm(fight);
        Vec3 aStart = rel(helper, a);
        Vec3 bStart = rel(helper, b);
        SokoviaTemplate[] template = new SokoviaTemplate[1];
        Vec3[] aOnTheIsland = new Vec3[2];
        boolean[] descentSeen = {false};
        List<SokoviaJob> jobs = new ArrayList<>();
        int[] skyBlocks = {0};
        int[] most = {0};
        List<String> trace = new ArrayList<>();
        Script script = new Script();
        script.step("the second fighter joins", () -> {
            check(fight.participants().size() == 2, "both fighters are in the fight");
            return true;
        });
        script.then("the warning", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            check(fight.isLifting() && !fight.isTremor() && lift != null, "the island will rise");
            check(lift.step() == SokoviaLift.Step.WARNED, "the warning changed nothing yet");
            check(lift.template() == null && journals(level).isEmpty(), "there is no template and no journal yet");
            check(groundHash(helper) == ground && blocksInTheSky(helper) == 0, "the world is as it was");
        });
        script.then("the lock", () -> {
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            check(lift.step() == SokoviaLift.Step.LOCKED, "locked");
            template[0] = lift.template();
            check(template[0] != null && template[0].size() > 500, "the island is copied: " + template[0].size() + " blocks");
            check(lift.carried().contains(a.getUUID()) && lift.carried().contains(b.getUUID()) && lift.carried().contains(boss(fight).getUUID()),
                    "the fighters and the boss are carried");
            check(Powers.get(a).is(Live.LIFTED) && a.isNoGravity(), "a is locked");
            check(groundHash(helper) == ground && blocksInTheSky(helper) == 0, "nothing moved yet");
        });
        script.step("the rise", () -> {
            client(a, b);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            trace.add(lift.step() + ":" + String.format("%.2f", rel(helper, a).y));
            if (lift.job() != null && !jobs.contains(lift.job())) {
                jobs.add(lift.job());
            }
            int now = blocksInTheSky(helper);
            most[0] = Math.max(most[0], now - skyBlocks[0]);
            skyBlocks[0] = now;
            if (lift.step() == SokoviaLift.Step.RISING) {
                check(now == 0, "nothing is placed while it rises");
                assertNear(rel(helper, a).y - aStart.y, lift.altitude(), 2.0, "the fighter rides the island");
            }
            return lift.step() == SokoviaLift.Step.PLACED && fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("the island is up", () -> {
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            check(!jobs.isEmpty() && jobs.stream().allMatch(SokoviaJob::isDone), "the placing is done");
            check(most[0] <= 500 && most[0] > 0, "no tick placed more than the budget: the most was " + most[0]);
            check(jobs.get(0).ticks() >= template[0].size() / 500, "and it took " + jobs.get(0).ticks() + " slices");
            check(skyBlocks[0] == template[0].size(), "the sky holds exactly the island: " + skyBlocks[0] + " of " + template[0].size());
            BlockPos shift = new BlockPos(0, LIFT, 0);
            for (int i = 0; i < template[0].size(); i++) {
                BlockPos pos = new BlockPos(template[0].x(i), template[0].y(i), template[0].z(i)).offset(shift);
                check(level.getBlockState(pos) == template[0].state(i), "block " + i + " of the island is in the sky at " + pos.toShortString());
            }
            assertNear(rel(helper, a).y, aStart.y + LIFT, 0.3, "a stands on the island " + trace.subList(Math.max(0, trace.size() - 12), trace.size()));
            assertNear(rel(helper, a).x, aStart.x, 0.3, "a has not moved sideways");
            assertNear(rel(helper, b).y, bStart.y + LIFT, 0.3, "b stands on the island");
            check(!a.isNoGravity() && !Powers.get(a).is(Live.LIFTED) && a.fallDistance == 0.0F, "a is free");
            check(a.getHealth() == 1000.0F, "and was not hurt");
            check(rel(helper, boss(fight)).y > GROUND + LIFT, "the boss is up there too: " + rel(helper, boss(fight)));
            check(fight.island().isIslandUp() && fight.island().islandOffset() == LIFT, "the arena knows the island is up");
            check(journals(level).size() == 1, "one journal is on disk");
            check(lift.header() != null && lift.header().state().equals(SokoviaJournal.PASTED), "and it says the island is placed");
            check(groundHash(helper) == ground, "the ground was never touched");
            fight.end(BossEnd.VICTORY);
        });
        script.step("the descent", () -> {
            client(a, b);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            if (lift != null && lift.step() == SokoviaLift.Step.DESCENT_LOCK && !descentSeen[0]) {
                // (The boss pushed them about a little while they stood on the island: they come down where they stand
                // now.)
                descentSeen[0] = true;
                aOnTheIsland[0] = rel(helper, a);
                aOnTheIsland[1] = rel(helper, b);
                check(Powers.get(a).is(Live.LIFTED) && Powers.get(b).is(Live.LIFTED), "everyone is locked again");
            }
            if (lift != null && lift.step() == SokoviaLift.Step.FALLING) {
                check(Powers.get(a).is(Live.LIFTED), "a is carried down");
            }
            return fight.isFinished();
        });
        script.then("the island is gone", () -> {
            check(groundHash(helper) == ground, "the ground is exactly as it was");
            check(blocksInTheSky(helper) == 0, "the sky is empty again: " + blocksInTheSky(helper));
            check(descentSeen[0], "the descent was seen");
            assertNear(rel(helper, a).y, aOnTheIsland[0].y - LIFT, 0.3, "a is back on the ground");
            assertNear(rel(helper, a).x, aOnTheIsland[0].x, 0.3, "at the same place");
            assertNear(rel(helper, a).z, aOnTheIsland[0].z, 0.3, "and the same");
            assertNear(rel(helper, b).y, aOnTheIsland[1].y - LIFT, 0.3, "b is back on the ground");
            assertNear(rel(helper, a).y, aStart.y, 0.3, "which is where a began");
            check(!a.isNoGravity() && !Powers.get(a).is(Live.LIFTED) && a.fallDistance == 0.0F, "nobody is locked");
            check(a.getHealth() > 900.0F, "and the lift hurt nobody: " + a.getHealth());
            check(journals(level).isEmpty(), "the journal is deleted");
            check(fight.island().islandOffset() == 0.0 && !fight.island().isIslandUp(), "the arena is the ground's again");
            check(SokoviaLift.of(fight) == null, "the lift is gone");
        });
        script.run(f, 880);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The kick counter

    private static final Field FLOATING;

    static {
        try {
            FLOATING = ServerGamePacketListenerImpl.class.getDeclaredField("clientIsFloating");
            FLOATING.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A survival player with no permission to fly hangs in the air for the 200 ticks of a slow rise and is not kicked.
     */
    @GameTest(template = PAD, batch = "ultron_lift_kick", timeoutTicks = 700)
    public static void aCarriedPlayerIsNotKickedForFloating(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.LIFT_TICKS, 200);
        ServerPlayer player = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(player);
        check(!player.getAbilities().mayfly && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "a survival player who may not fly");
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        int[] carriedTicks = {0};
        int[] highest = {0};
        Script script = new Script();
        script.then("the lock", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            TestKit.sentPackets(player);
        });
        script.step("200 ticks in the air", () -> {
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            boolean carried = lift != null && lift.carried().contains(player.getUUID());
            // What the server does to every connected player every tick, and what the client reports while it floats.
            try {
                FLOATING.setBoolean(player.connection, carried && player.getY() > helper.absoluteVec(new Vec3(0, GROUND + 1.0, 0)).y);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            player.connection.tick();
            client(player);
            if (carried) {
                carriedTicks[0]++;
                highest[0] = Math.max(highest[0], player.connection.aboveGroundTickCount);
            }
            for (Object packet : TestKit.sentPackets(player)) {
                check(!(packet instanceof ClientboundDisconnectPacket), "the player was kicked after " + carriedTicks[0] + " ticks");
            }
            return lift != null && lift.step() == SokoviaLift.Step.PLACED;
        });
        script.then("never near the limit", () -> {
            check(carriedTicks[0] >= 200, "the player was in the air for " + carriedTicks[0] + " ticks");
            check(highest[0] <= 2, "the counter never rose above " + highest[0]);
        });
        script.run(f, 650);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who is carried

    /**
     * A fighter, a Creative player, a flying hero, the boss, his Sentries and a hero's drone are carried: after the
     * rise each stands at the same place over the pad, 16 blocks up, unhurt and unlocked. A cow, a villager and an item
     * on the ground stay on the ground, as they were.
     */
    @GameTest(template = PAD, batch = "ultron_lift_carry", timeoutTicks = 500)
    public static void everythingOnItIsCarriedAndTheRestStays(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        // (A drone dissolves when its master is not Ultron: the fighter who starts the fight is the hero.)
        ServerPlayer a = f.player(MIDDLE_X, GROUND, MIDDLE_Z, Power.ULTRON);
        ServerPlayer b = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0, GameType.CREATIVE);
        ServerPlayer c = f.player(MIDDLE_X + 3.0, GROUND, MIDDLE_Z - 3.0);
        c.getAbilities().mayfly = true;
        c.getAbilities().flying = true;
        c.onUpdateAbilities();
        tank(a);
        UltronFight fight = onTheGround(f, a);
        calm(fight);
        UltronSentryEntity drone = UltronSentryEntity.army(level, helper.absoluteVec(new Vec3(MIDDLE_X + 5.0, GROUND + 1.5, MIDDLE_Z)), false);
        drone.setOwner(a.getUUID());
        drone.setHeld(true);
        level.addFreshEntity(drone);
        Cow cow = helper.spawn(EntityType.COW, new Vec3(MIDDLE_X + 6.0, GROUND, MIDDLE_Z + 5.0));
        cow.setNoAi(true);
        ItemEntity item = new ItemEntity(level, helper.absoluteVec(new Vec3(MIDDLE_X - 6.0, GROUND, MIDDLE_Z - 5.0)).x,
                helper.absoluteVec(new Vec3(MIDDLE_X - 6.0, GROUND, MIDDLE_Z - 5.0)).y, helper.absoluteVec(new Vec3(MIDDLE_X - 6.0, GROUND, MIDDLE_Z - 5.0)).z,
                new ItemStack(Items.DIAMOND, 3));
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoGravity(true);
        level.addFreshEntity(item);
        List<Entity> riders = new ArrayList<>(List.of(a, b, c, drone, boss(fight)));
        riders.addAll(fight.sentries());
        check(fight.sentries().size() == 2, "two Sentries stand with him");
        List<Vec3> before = riders.stream().map(e -> rel(helper, e)).toList();
        List<Boolean> gravity = riders.stream().map(Entity::isNoGravity).toList();
        Vec3 cowAt = cow.position();
        Vec3 itemAt = item.position();
        int itemAge = item.getAge();
        Script script = new Script();
        script.step("the second fighters join", () -> fight.participants().size() == 3);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            for (Entity rider : riders) {
                check(lift.carried().contains(rider.getUUID()), rider.getName().getString() + " #" + rider.getId() + " (" + rider.getType() + ") is carried: "
                        + lift.carried().size() + " carried, alive " + rider.isAlive());
            }
            check(!lift.carried().contains(cow.getUUID()) && !lift.carried().contains(item.getUUID()), "the cow and the item are not");
        });
        script.step("the rise", () -> {
            client(a, b, c);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("up there", () -> {
            for (int i = 0; i < riders.size(); i++) {
                Entity rider = riders.get(i);
                Vec3 now = rel(helper, rider);
                double tolerance = rider instanceof ServerPlayer ? 0.3 : 0.6;
                assertNear(now.x, before.get(i).x, tolerance, rider.getName().getString() + " x");
                assertNear(now.z, before.get(i).z, tolerance, rider.getName().getString() + " z");
                assertNear(now.y, before.get(i).y + LIFT, tolerance, rider.getName().getString() + " y");
                check(rider.isNoGravity() == gravity.get(i), rider.getName().getString() + " has its gravity as it had it");
                check(rider.fallDistance == 0.0F, rider.getName().getString() + " has not fallen");
            }
            for (ServerPlayer player : List.of(a, b, c)) {
                check(!Powers.get(player).is(Live.LIFTED), player.getGameProfile().getName() + " is unlocked");
            }
            check(a.getHealth() == 1000.0F && c.getHealth() == c.getMaxHealth() && b.getHealth() == b.getMaxHealth(), "nobody was hurt");
            check(c.getAbilities().flying && c.getAbilities().mayfly, "the flyer still flies");
            check(cow.position().equals(cowAt) && cow.isNoAi(), "the cow is where it was");
            check(item.position().equals(itemAt) && item.getItem().getCount() == 3 && item.getAge() >= itemAge, "so is the item, and its stack");
            check(drone.getOwnerUUID() != null && !drone.isNoAi(), "the drone is still the hero's and has its will");
        });
        script.run(f, 480);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The set-down

    /**
     * Nobody is left inside a block: a player against a tree trunk and one under leaves are set down where they stood,
     * and one who would be inside a block that was put in the way is moved up to free space, at most three blocks.
     */
    @GameTest(template = PAD, batch = "ultron_lift_stuck", timeoutTicks = 400)
    public static void nobodyIsSetDownInsideABlock(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        // A tree: a trunk three high with leaves round its top, and a canopy over the spot where a second player
        // stands.
        for (int y = 6; y <= 8; y++) {
            helper.setBlock(new BlockPos(25, y, 21), Blocks.OAK_LOG);
        }
        for (int x = 23; x <= 27; x++) {
            for (int z = 19; z <= 23; z++) {
                helper.setBlock(new BlockPos(x, 8, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
        }
        helper.setBlock(new BlockPos(25, 8, 21), Blocks.OAK_LOG);
        for (int x = 24; x <= 27; x++) {
            helper.setBlock(new BlockPos(x, 8, 25), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        }
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer trunk = f.player(24.5, GROUND, 21.5);
        ServerPlayer canopy = f.player(25.5, GROUND, 25.5);
        ServerPlayer stuck = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Script script = new Script();
        script.step("everyone joins", () -> fight.participants().size() == 4);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the rise", () -> {
            client(starter, trunk, canopy, stuck);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            if (lift != null && lift.step() == SokoviaLift.Step.RISING && lift.altitude() > 10.0 && level.getBlockState(abs(helper, 17, (int) GROUND + LIFT, 21)).isAir()) {
                // Somebody puts a block where the fourth player will be set down: two high, right on top of the
                // island's surface.
                helper.setBlock(new BlockPos(17, (int) GROUND + LIFT, 21), Blocks.STONE);
                helper.setBlock(new BlockPos(17, (int) GROUND + 1 + LIFT, 21), Blocks.STONE);
            }
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("nobody is inside a block", () -> {
            for (ServerPlayer player : List.of(starter, trunk, canopy, stuck)) {
                check(level.noCollision(player, player.getBoundingBox().deflate(0.001)), player.getGameProfile().getName() + " is free of the blocks at "
                        + rel(helper, player));
            }
            assertNear(rel(helper, trunk).y, GROUND + LIFT, 0.3, "the one against the trunk is where they stood");
            assertNear(rel(helper, canopy).y, GROUND + LIFT, 0.3, "so is the one under the leaves");
            double moved = rel(helper, stuck).y - (GROUND + LIFT);
            check(moved >= 1.9 && moved <= 3.1, "the one that would have been inside stone was moved up " + moved + " blocks");
        });
        script.run(f, 380);
    }

    /**
     * A fighter who is not on the island when it locks is brought in to the nearest point of the rim; a spectator is
     * not a fighter and is left alone.
     */
    @GameTest(template = PAD, batch = "ultron_lift_stray", timeoutTicks = 300)
    public static void aFighterOutsideTheRimIsBroughtIn(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer stray = f.player(MIDDLE_X + 17.0, GROUND, MIDDLE_Z + 2.0);
        ServerPlayer watcher = f.player(MIDDLE_X - 17.0, GROUND, MIDDLE_Z, GameType.SPECTATOR);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Vec3 watcherAt = rel(helper, watcher);
        Script script = new Script();
        script.step("the stray joins", () -> fight.isParticipant(stray.getUUID()) && !fight.isParticipant(watcher.getUUID()));
        script.then("the lock", () -> {
            check(Math.hypot(rel(helper, stray).x - MIDDLE_X, rel(helper, stray).z - MIDDLE_Z) > 15.0, "the stray is far from the island");
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            Vec3 now = rel(helper, stray);
            double distance = Math.hypot(now.x - MIDDLE_X, now.z - MIDDLE_Z);
            double rim = SokoviaShape.rim(SokoviaLift.of(fight).spec(), Math.atan2(now.z - MIDDLE_Z, now.x - MIDDLE_X));
            check(distance <= rim - 1.0 && distance >= rim - 3.0, "the stray was set just inside the rim (" + rim + "): " + distance + " from the middle");
            assertNear(now.y, GROUND, 0.5, "on the ground there");
            check(SokoviaLift.of(fight).carried().contains(stray.getUUID()), "and is carried");
            check(rel(helper, watcher).equals(watcherAt) && !SokoviaLift.of(fight).carried().contains(watcher.getUUID()), "the spectator was not touched");
        });
        script.step("the rise", () -> {
            client(starter, stray);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("on the island", () -> {
            assertNear(rel(helper, stray).y, GROUND + LIFT, 0.3, "the stray stands on the island");
            check(Math.hypot(rel(helper, stray).x - MIDDLE_X, rel(helper, stray).z - MIDDLE_Z) <= 11.0, "inside its rim");
        });
        script.run(f, 280);
    }

    // ------------------------------------------------------------------------------------------------------------
    // While it is up

    /** Brings a fight to the moment the island is placed, with the given fighters on the ground to be carried. */
    private static Script toTheIsland(UltronFight fight, ServerPlayer... fighters) {
        Script script = new Script();
        script.step("the fighters join", () -> fight.participants().size() == fighters.length);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island rises", () -> {
            client(fighters);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        return script;
    }

    /**
     * Whoever leaves the island is put back on it: a player 8 blocks past the rim, one 3 under the cone's tip and one
     * 85 above its top, each at the island's surface, three blocks inside the rim, with at most 2 damage and at least
     * half a heart left; and at most once every five seconds.
     */
    @GameTest(template = PAD, batch = "ultron_lift_field", timeoutTicks = 600)
    public static void theFieldKeepsEveryoneOnTheIsland(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer wide = f.player(MIDDLE_X - 5.0, GROUND, MIDDLE_Z);
        ServerPlayer deep = f.player(MIDDLE_X + 5.0, GROUND, MIDDLE_Z);
        ServerPlayer high = f.player(MIDDLE_X, GROUND, MIDDLE_Z + 5.0);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        double surface = GROUND + LIFT;
        int[] since = {0};
        Script script = toTheIsland(fight, starter, wide, deep, high);
        script.then("they are hurt a little and sent out", () -> {
            for (ServerPlayer player : List.of(wide, deep, high)) {
                player.setHealth(3.0F);
            }
            put(f, wide, MIDDLE_X + 20.0, surface, MIDDLE_Z - 2.0);
            put(f, deep, MIDDLE_X, GROUND + LIFT - 16.0, MIDDLE_Z);
            put(f, high, MIDDLE_X, surface + 100.0, MIDDLE_Z + 5.0);
            check(rel(helper, deep).y < 12.0, "the second is under the island's cone");
        });
        script.step("they are brought back", () -> {
            since[0]++;
            for (ServerPlayer player : List.of(wide, deep, high)) {
                Vec3 at = rel(helper, player);
                boolean back = Math.hypot(at.x - MIDDLE_X, at.z - MIDDLE_Z) <= 9.0 && Math.abs(at.y - surface) < 1.0;
                check(since[0] <= 12 || back, player.getGameProfile().getName() + " was not put back on the island: " + at);
            }
            return since[0] > 12;
        });
        script.then("without being killed", () -> {
            for (ServerPlayer player : List.of(wide, deep, high)) {
                check(player.getHealth() >= 1.0F && player.getHealth() <= 3.0F && 3.0F - player.getHealth() <= UltronBossConfig.RESCUE_DAMAGE.get().floatValue() + 0.01F,
                        player.getGameProfile().getName() + " has " + player.getHealth() + " health");
                check(player.fallDistance == 0.0F, "and has not fallen");
            }
            // Out again at once: not taken back before five seconds have passed.
            put(f, wide, MIDDLE_X + 20.0, surface, MIDDLE_Z - 2.0);
            since[0] = 0;
        });
        script.step("only once in five seconds", () -> {
            since[0]++;
            Vec3 at = rel(helper, wide);
            if (since[0] <= 60) {
                check(Math.hypot(at.x - MIDDLE_X, at.z - MIDDLE_Z) > 15.0, "the second rescue came too soon, after " + since[0] + " ticks");
                return false;
            }
            return Math.hypot(at.x - MIDDLE_X, at.z - MIDDLE_Z) <= 9.0;
        });
        script.run(f, 580);
    }

    /**
     * The arena's protection reaches the island in the sky: nothing in the cylinder can be broken, placed, flooded or
     * pushed there, nothing spawns, and nothing is protected beyond it.
     */
    @GameTest(template = PAD, batch = "ultron_lift_protect", timeoutTicks = 400)
    public static void theIslandInTheSkyIsProtected(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Script script = toTheIsland(fight, starter);
        script.then("the events", () -> {
            BlockPos on = abs(helper, 21, SURFACE + LIFT, 21);
            BlockPos above = on.above(3);
            BlockPos beyond = abs(helper, 21 + 25, SURFACE + LIFT, 21);
            check(BossFights.protects(level, on) && BossFights.protects(level, above)
                    && BossFights.protects(level, abs(helper, 21, SURFACE, 21)), "the island, the air over it and the ground below are protected");
            check(!BossFights.protects(level, beyond), "a block 25 away is not");
            for (BlockPos pos : List.of(on, above, beyond)) {
                boolean inside = pos != beyond;
                BlockEvent.BreakEvent broke = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), starter);
                NeoForge.EVENT_BUS.post(broke);
                BlockEvent.EntityPlaceEvent placed = new BlockEvent.EntityPlaceEvent(
                        BlockSnapshot.create(level.dimension(), level, pos), Blocks.DIRT.defaultBlockState(), starter);
                NeoForge.EVENT_BUS.post(placed);
                PistonEvent.Pre pushed = new PistonEvent.Pre(level, pos, Direction.UP,
                        PistonEvent.PistonMoveType.EXTEND);
                NeoForge.EVENT_BUS.post(pushed);
                check(broke.isCanceled() == inside, "breaking " + (inside ? "in" : "beyond") + " the island");
                check(placed.isCanceled() == inside, "placing " + (inside ? "in" : "beyond") + " the island");
                check(pushed.isCanceled() == inside, "a piston " + (inside ? "in" : "beyond") + " the island");
            }
        });
        script.run(f, 380);
    }

    /**
     * The pit's tractor: a fighter who comes to stand on the ground under the island for two seconds is carried up and
     * set down on it, at most {@code rejoinLimit} times.
     */
    @GameTest(template = PAD, batch = "ultron_lift_tractor", timeoutTicks = 900)
    public static void aFighterWhoIsLeftBelowIsTakenUpAtMostTwice(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.REJOIN_LIMIT, 2);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        ServerPlayer[] late = new ServerPlayer[1];
        int[] waited = {0};
        int[] trips = {0};
        double[] liftedAt = {Double.NaN};
        Script script = new Script();
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            late[0] = f.player(MIDDLE_X + 5.0, GROUND, MIDDLE_Z - 3.0); // (arrives after the lock: not carried)
        });
        script.step("the island rises", () -> {
            client(starter, late[0]);
            check(!SokoviaLift.of(fight).carried().contains(late[0].getUUID()) || fight.phase() != UltronFight.Phase.RISING, "the latecomer is not on the island");
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("left on the ground", () -> {
            check(rel(helper, late[0]).y < GROUND + 1.0, "the latecomer is on the ground");
            waited[0] = 0;
        });
        for (int trip = 1; trip <= 3; trip++) {
            int number = trip;
            script.step("trip " + number, () -> {
                client(starter, late[0]);
                waited[0]++;
                if (number <= 2) {
                    if (SokoviaLift.of(fight).carries(late[0].getUUID()) && Double.isNaN(liftedAt[0])) {
                        liftedAt[0] = rel(helper, late[0]).x;
                    }
                    boolean up = rel(helper, late[0]).y > GROUND + LIFT - 1.0 && !SokoviaLift.of(fight).carries(late[0].getUUID());
                    if (up) {
                        check(waited[0] >= 40 && waited[0] <= 40 + 80, "taken up after " + waited[0] + " ticks");
                        assertNear(rel(helper, late[0]).x, liftedAt[0], 0.5, "set down where they stood");
                        liftedAt[0] = Double.NaN;
                        check(SokoviaLift.of(fight).islanders().contains(late[0].getUUID()), "now an islander");
                        // (They fell, and came back from the dead: an islander no more.)
                        SokoviaLift.onDeath(late[0]);
                        put(f, late[0], MIDDLE_X + 5.0, GROUND, MIDDLE_Z - 3.0);
                        waited[0] = 0;
                        trips[0]++;
                        return true;
                    }
                    return false;
                }
                // The third time nothing happens.
                check(rel(helper, late[0]).y < GROUND + 1.0, "the third trip was made");
                return waited[0] > 130;
            });
        }
        script.then("two trips", () -> check(trips[0] == 2, "two trips: " + trips[0]));
        script.run(f, 880);
    }

    /**
     * A fighter the pit's tractor is still carrying up when the island goes down (the fight was won while they stood
     * under it, after they died up there) is put on the island at once and comes down with it, and is free when it lands:
     * they fall again when they step off a block. (Had the tractor kept them, the island's lock would have been put on
     * top of the tractor's and taken its "no gravity" for their own, and they would have floated for good.)
     */
    @GameTest(template = PAD, batch = "ultron_lift_tractor_descent", timeoutTicks = 700)
    public static void aFighterTheTractorCarriesWhenTheIslandGoesDownComesDownWithItAndIsFree(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer fallen = f.player(MIDDLE_X + 3.0, GROUND, MIDDLE_Z + 3.0);
        tank(starter);
        tank(fallen);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        int[] waited = {0};
        boolean[] towed = {false};
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is up", () -> {
            client(starter, fallen);
            return fight.phase() == UltronFight.Phase.ISLAND && in(fight, SokoviaLift.Step.PLACED);
        });
        script.then("the fight is won", () -> {
            fight.setNextDebrisAt(Long.MAX_VALUE / 2); // (nothing falls on them: the test's client never stops one who was knocked)
            fight.end(BossEnd.VICTORY);
        });
        // (The island goes down 100 ticks after the win; the tractor takes one who stood under it for 40 ticks, in 60.)
        script.step("a while into the pause", () -> {
            client(starter, fallen);
            return ++waited[0] >= 30;
        });
        script.then("one died up there and stands under it", () -> {
            SokoviaLift.onDeath(fallen);
            put(f, fallen, MIDDLE_X + 3.0, GROUND, MIDDLE_Z + 3.0);
        });
        script.step("the island goes down with them, and lands", () -> {
            client(starter, fallen);
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            if (lift != null && lift.step() == SokoviaLift.Step.PLACED) {
                towed[0] = lift.carries(fallen.getUUID()) && !lift.carried().contains(fallen.getUUID());
            } else if (lift != null && lift.step() == SokoviaLift.Step.DESCENT_LOCK) {
                check(towed[0], "(the tractor had them when the island began to go down)");
                check(lift.carried().contains(fallen.getUUID()) && rel(helper, fallen).y > GROUND + LIFT - 1.0,
                        "they are on the island, and it carries them: " + rel(helper, fallen));
            }
            return fight.isFinished();
        });
        script.then("they are down, and free", () -> {
            assertNear(rel(helper, fallen).y, GROUND, 0.01, "they are on the ground");
            assertNear(rel(helper, fallen).x, MIDDLE_X + 3.0, 0.5, "under where they stood");
            check(!fallen.isNoGravity() && !Powers.get(fallen).is(Live.LIFTED), "and free: noGravity " + fallen.isNoGravity());
            check(!SokoviaLift.isCarried(fallen), "nothing carries them");
        });
        script.run(f, 680);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Being carried

    /**
     * A carried player is immune to everything but what bypasses invulnerability (the void's damage still reaches
     * them), and for 40 ticks after they are set down to the fight's creatures but not to another player.
     */
    @GameTest(template = PAD, batch = "ultron_lift_immune", timeoutTicks = 400)
    public static void aCarriedPlayerCannotBeHurtExceptByTheVoid(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.pvp(true);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer rival = f.player(MIDDLE_X + 14.0, GROUND, MIDDLE_Z, GameType.SURVIVAL);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        int[] graceStart = {0};
        Script script = new Script();
        script.step("the rival joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("while it rises", () -> {
            client(starter, rival);
            if (in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 3.0) {
                float health = starter.getHealth();
                check(!starter.hurt(level.damageSources().generic(), 20.0F) && starter.getHealth() == health, "a blow does nothing");
                check(!starter.hurt(level.damageSources().fall(), 20.0F) && !starter.hurt(level.damageSources().lava(), 20.0F), "nor a fall, nor lava");
                check(!starter.hurt(level.damageSources().mobAttack(fight.sentries().get(0)), 20.0F), "nor a Sentry");
                check(!starter.hurt(level.damageSources().playerAttack(rival), 20.0F), "nor another player");
                starter.hurt(level.damageSources().fellOutOfWorld(), 20.0F);
                check(starter.getHealth() < health, "but the void reaches them: " + starter.getHealth());
                return true;
            }
            return false;
        });
        script.step("and set down", () -> {
            client(starter, rival);
            if (fight.phase() == UltronFight.Phase.ISLAND && graceStart[0] == 0) {
                graceStart[0] = (int) level.getGameTime();
            }
            return graceStart[0] != 0;
        });
        script.then("the fight's creatures cannot hurt them for a while, another player can", () -> {
            starter.setHealth(starter.getMaxHealth());
            float health = starter.getHealth();
            check(SokoviaLift.inGrace(starter), "they are in grace");
            check(!starter.hurt(level.damageSources().mobAttack(fight.sentries().get(0)), 20.0F) && starter.getHealth() == health, "a Sentry does nothing");
            check(!starter.hurt(level.damageSources().mobAttack(boss(fight)), 20.0F), "nor Ultron");
            check(starter.hurt(level.damageSources().playerAttack(rival), 5.0F) && starter.getHealth() < health, "another player does");
        });
        script.step("the grace runs out", () -> {
            client(starter, rival);
            return level.getGameTime() - graceStart[0] > SokoviaLift.GRACE_TICKS + 2;
        });
        script.then("and then they can be hurt", () -> {
            starter.setHealth(starter.getMaxHealth());
            float health = starter.getHealth();
            check(!SokoviaLift.inGrace(starter), "the grace is over");
            check(starter.hurt(level.damageSources().mobAttack(fight.sentries().get(0)), 20.0F) && starter.getHealth() < health, "a Sentry hurts");
        });
        script.run(f, 380);
    }

    /**
     * The clients are told in order: the rise, the end of the mesh (the island is real, the ground stays hidden), the
     * descent twice (first hanging, then falling), and the end that shows everything again; each says the same island.
     */
    @GameTest(template = PAD, batch = "ultron_lift_payloads", timeoutTicks = 800)
    public static void theClientsAreToldTheLiftInOrder(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        List<SokoviaLiftPayload> told = new ArrayList<>();
        long[] lockedAt = {0};
        Script script = new Script();
        script.then("the lift begins", () -> {
            TestKit.sentPackets(starter);
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            lockedAt[0] = f.level().getGameTime();
        });
        script.step("the island rises and the fight is won", () -> {
            client(starter);
            collect(starter, told);
            if (fight.phase() == UltronFight.Phase.ISLAND && !fight.isOver()) {
                fight.end(BossEnd.VICTORY);
            }
            return fight.isOver();
        });
        script.step("the island comes down", () -> {
            client(starter);
            collect(starter, told);
            return fight.isFinished();
        });
        script.then("the payloads", () -> {
            collect(starter, told);
            List<Integer> phases = told.stream().map(SokoviaLiftPayload::phase).toList();
            check(phases.equals(List.of(SokoviaLiftPayload.RISE, SokoviaLiftPayload.END, SokoviaLiftPayload.LOWER, SokoviaLiftPayload.LOWER, SokoviaLiftPayload.END)),
                    "in order: " + phases);
            SokoviaLiftPayload rise = told.get(0);
            check(rise.start() == lockedAt[0] + UltronFight.LOCK_TICKS, "the rise starts when the lock is over: " + rise.start() + " and " + (lockedAt[0] + UltronFight.LOCK_TICKS));
            check(rise.radius() == 9 && rise.height() == LIFT && rise.coneDepth() == 6 && rise.skin() == 4 && rise.cover() == 8 && rise.ticks() == 40 && rise.hide(),
                    "with the numbers of the island: " + rise);
            check(rise.centre().equals(BlockPos.containing(fight.arena().center())), "round the middle of the fight");
            check(told.stream().allMatch(p -> p.seed() == rise.seed() && p.centre().equals(rise.centre())), "all of one island");
            check(told.get(1).hide() && !told.get(4).hide(), "the ground stays hidden while the island is up and is shown again at the end");
            check(told.get(2).start() == Long.MAX_VALUE && told.get(3).start() < Long.MAX_VALUE && told.get(3).start() > told.get(1).start(),
                    "the descent hangs first, then falls");
        });
        script.run(f, 780);
    }

    private static void collect(ServerPlayer player, List<SokoviaLiftPayload> into) {
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SokoviaLiftPayload payload) {
                into.add(payload);
            }
        }
    }

    /**
     * A player who comes within sight of the island's key at any moment is told what the island is doing: the rise
     * while it rises, the end (ground hidden) while it is up, the descent while it comes down.
     */
    @GameTest(template = PAD, batch = "ultron_lift_tracking", timeoutTicks = 900)
    public static void aPlayerWhoArrivesLateSeesTheMesh(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer latecomer = f.player(MIDDLE_X + 15.0, GROUND, MIDDLE_Z + 15.0, GameType.CREATIVE);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Script script = new Script();
        script.then("the warning", () -> fight.forceState(UltronFight.Phase.LIFT_WARN));
        script.step("the key rises", () -> fight.key() != null);
        script.then("the lock", () -> fight.forceState(UltronFight.Phase.LIFT_LOCK));
        script.step("the island rises", () -> {
            client(starter, latecomer);
            return in(fight, SokoviaLift.Step.RISING);
        });
        script.then("a player sees the key while it rises", () -> {
            TestKit.sentPackets(latecomer);
            NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(latecomer, fight.key()));
            List<SokoviaLiftPayload> told = new ArrayList<>();
            collect(latecomer, told);
            check(told.size() == 1 && told.get(0).phase() == SokoviaLiftPayload.RISE && told.get(0).bossId() == fight.key().getId(), "told the rise, with the key: " + told);
        });
        script.step("the island is up", () -> {
            client(starter, latecomer);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("and while it is up", () -> {
            TestKit.sentPackets(latecomer);
            NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(latecomer, fight.key()));
            List<SokoviaLiftPayload> told = new ArrayList<>();
            collect(latecomer, told);
            check(told.size() == 1 && told.get(0).phase() == SokoviaLiftPayload.END && told.get(0).hide(), "told the island is up and the ground is hidden: " + told);
            // A player who sees something else is told nothing.
            NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(latecomer, boss(fight)));
            collect(latecomer, told);
            check(told.size() == 1, "(only the key tells)");
        });
        script.run(f, 880);
    }

    /**
     * The fight ends (an operator, a defeat) before the island is placed: nobody is left hanging; the island is not
     * placed; everyone is on the ground where they were taken from, in the next tick.
     */
    @GameTest(template = PAD, batch = "ultron_lift_abort", timeoutTicks = 300)
    public static void aFightThatEndsWhileTheIslandRisesSetsEveryoneDown(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer other = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 3.0);
        tank(starter);
        long ground = groundHash(helper);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Vec3[] where = new Vec3[2];
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            where[0] = rel(helper, starter);
            where[1] = rel(helper, other);
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island rises", () -> {
            client(starter, other);
            return in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 8.0;
        });
        script.then("the fight is ended by an operator", () -> {
            check(rel(helper, starter).y > where[0].y + 6.0, "the fighter is well up");
            fight.end(BossEnd.COMMAND);
        });
        script.step("everyone is set down at once", () -> {
            client(starter, other);
            return !fight.isFinished() && in(fight, SokoviaLift.Step.DONE);
        });
        script.then("on the ground, free", () -> {
            for (int i = 0; i < 2; i++) {
                ServerPlayer player = i == 0 ? starter : other;
                assertNear(rel(helper, player).y, where[i].y, 0.3, "player " + i + " is back where they were taken from");
                assertNear(rel(helper, player).x, where[i].x, 0.3, "and sideways");
                check(!player.isNoGravity() && !Powers.get(player).is(Live.LIFTED), "and free");
            }
            check(blocksInTheSky(helper) == 0 && groundHash(helper) == ground && journals(level).isEmpty(), "nothing was placed and nothing written");
            check(!fight.island().isIslandUp() && fight.island().islandOffset() == 0.0, "the arena is the ground's");
        });
        script.step("the fight winds down and finishes", () -> fight.isFinished());
        script.run(f, 280);
    }

    /**
     * The fight ends in the ten seconds of the warning (a wipe, Peaceful, an operator's stop): nothing was copied and
     * nothing is carried, so the descent that follows has nothing to lower and the fight is over when its ending is,
     * not after the five minutes the framework would otherwise wait (the arena would hold its fighters all that time and
     * no other boss could be called).
     */
    @GameTest(template = PAD, batch = "ultron_lift_warn_end", timeoutTicks = 300)
    public static void aFightThatEndsDuringTheWarningIsOverWithItsEnding(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        long ground = groundHash(helper);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        fight.forceState(UltronFight.Phase.LIFT_WARN);
        check(fight.isLifting() && in(fight, SokoviaLift.Step.WARNED), "the warning has begun and the island will rise");
        fight.end(BossEnd.WIPE);
        f.everyTick(200, tick -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(tick <= UltronFight.UPLOAD_AWAY_TICKS + 5, "the fight was over with its ending (at tick " + tick + "), not held for minutes");
            check(blocksInTheSky(helper) == 0 && groundHash(helper) == ground && journals(f.level()).isEmpty(), "nothing was changed or written");
            check(SokoviaLift.of(fight) == null && !fight.island().isIslandUp(), "the lift is gone");
            return true;
        });
    }

    /**
     * A fighter who logs out while the island rises and logs in again a moment later is put back among it and carried
     * on; one who logs in after the island is up is where they were saved, on it.
     */
    @GameTest(template = PAD, batch = "ultron_lift_login", timeoutTicks = 500)
    public static void aFighterWhoLogsBackInIsPutBackWithTheIsland(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer visitor = f.player(MIDDLE_X + 4.0, GROUND, MIDDLE_Z - 3.0);
        tank(starter);
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is rising", () -> {
            client(starter, visitor);
            return in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 6.0;
        });
        script.then("the visitor logs out", () -> {
            SokoviaLift.onLogout(visitor);
            check(!SokoviaLift.of(fight).carried().contains(visitor.getUUID()), "no longer carried");
            check(!visitor.isNoGravity() && !Powers.get(visitor).is(Live.LIFTED), "and given their gravity back for the save");
        });
        script.step("the island rises on", () -> {
            client(starter);
            return in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 12.0;
        });
        script.then("and logs in again, up in the sky", () -> {
            double altitude = SokoviaLift.of(fight).altitude();
            // (Where they were saved: the fight's creatures may have pushed them about while they were away.)
            Vec3 saved = rel(helper, visitor);
            SokoviaRecovery.onLogin(visitor);
            SokoviaLift.onLogin(visitor);
            check(SokoviaLift.of(fight).carried().contains(visitor.getUUID()) && visitor.isNoGravity() && Powers.get(visitor).is(Live.LIFTED), "carried again");
            assertNear(rel(helper, visitor).y, GROUND + altitude, 1.5, "at the height of the island now");
            assertNear(rel(helper, visitor).x, saved.x, 1.5, "and not sideways");
            assertNear(rel(helper, visitor).z, saved.z, 1.5, "in either direction");
        });
        script.step("the island is up", () -> {
            client(starter, visitor);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("with both on it", () -> {
            assertNear(rel(helper, visitor).y, GROUND + LIFT, 0.5, "the visitor stands on the island");
            assertNear(rel(helper, starter).y, GROUND + LIFT, 0.5, "so does the starter");
            check(SokoviaLift.of(fight).islanders().contains(visitor.getUUID()), "both are islanders");
        });
        script.run(f, 480);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Tremor: the fallback when the island cannot rise

    /** The lift is switched off. */
    @GameTest(template = PAD, batch = "ultron_lift_tremor_off", timeoutTicks = 300)
    public static void withTheLiftOffTheGroundOnlyShakes(GameTestHelper helper) {
        tremor(helper, f -> f.config(UltronBossConfig.LIFT_ENABLED, false), 0);
    }

    /** The mode is Tremor. */
    @GameTest(template = PAD, batch = "ultron_lift_tremor_mode", timeoutTicks = 300)
    public static void inTremorModeTheGroundOnlyShakes(GameTestHelper helper) {
        tremor(helper, f -> f.config(UltronBossConfig.LIFT_MODE, UltronBossConfig.LiftMode.TREMOR), 0);
    }

    /** The cut-ground mode is not in this release. */
    @GameTest(template = PAD, batch = "ultron_lift_tremor_cut", timeoutTicks = 300)
    public static void theCutGroundModeIsNotThereAndTheGroundOnlyShakes(GameTestHelper helper) {
        tremor(helper, f -> f.config(UltronBossConfig.LIFT_MODE, UltronBossConfig.LiftMode.CUT_GROUND), 0);
    }

    /** The sky over the island is not empty when the warning begins. */
    @GameTest(template = PAD, batch = "ultron_lift_tremor_sky", timeoutTicks = 300)
    public static void withSomethingInTheSkyTheGroundOnlyShakes(GameTestHelper helper) {
        tremor(helper, f -> helper.setBlock(new BlockPos(21, SURFACE + LIFT + 3, 21), Blocks.STONE), 1);
    }

    /**
     * Whatever makes the island unable to rise, the fight goes on as the Tremor: the phases run, nothing in the world
     * changes (but what the test put there), nobody is locked or carried, and the island phase begins on the ground.
     */
    private static void tremor(GameTestHelper helper, Consumer<TestKit.Fixture> why, int blocksInTheSkyBefore) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer other = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 3.0);
        tank(starter);
        why.accept(f);
        long ground = groundHash(helper);
        check(blocksInTheSky(helper) == blocksInTheSkyBefore, "(the sky as the case wants it)");
        UltronFight fight = onTheGround(f, starter);
        calm(fight);
        Vec3 at = rel(helper, starter);
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the warning", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            check(!fight.isLifting() && fight.isTremor(), "the island will not rise");
            check(SokoviaLift.of(fight) == null, "the lift keeps nothing");
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the ground shakes", () -> {
            check(!Powers.get(starter).is(Live.LIFTED) && !starter.isNoGravity() && !Powers.get(other).is(Live.LIFTED), "nobody is locked");
            check(groundHash(helper) == ground && blocksInTheSky(helper) == blocksInTheSkyBefore, "nothing in the world changes");
            assertNear(rel(helper, starter).y, at.y, 0.01, "nobody is carried");
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("and the fight goes on at ground level", () -> {
            check(fight.isTremor() && fight.canFall() && !fight.island().isIslandUp() && fight.island().islandOffset() == 0.0, "on the ground");
            check(journals(level).isEmpty(), "with no journal");
        });
        script.run(f, 280);
    }
}
