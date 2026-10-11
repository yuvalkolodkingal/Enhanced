package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.fixture;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.moveTo;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.start;
import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The arena of a boss fight: what it protects, how it keeps the fighters in (the field, the wall, the tractor), its
 * teleport gate, and that the mod's own abilities ask it before they change a block.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossArenaTests {

    private static BlockPos at(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Protection

    @GameTest(template = ARENA, batch = "boss_arena_events")
    public static void aProtectedArenaCancelsEveryEventThatWouldChangeItsBlocks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(4.5, FLOOR, 4.5);
        TestFight fight = start(f, BossKind.ULTRON, player, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
        BlockPos in = at(helper, 12, 2, 10);
        BlockPos out = at(helper, 20, 2, 10);
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        check(BossFights.protects(level, in) && !BossFights.protects(level, out), "the cylinder protects what is inside it");

        for (BlockPos pos : List.of(in, out)) {
            boolean inside = pos == in;
            BlockEvent.BreakEvent broke = new BlockEvent.BreakEvent(level, pos, dirt, player);
            NeoForge.EVENT_BUS.post(broke);
            BlockEvent.EntityPlaceEvent placed = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos), dirt, player);
            NeoForge.EVENT_BUS.post(placed);
            BlockEvent.FluidPlaceBlockEvent flowed = new BlockEvent.FluidPlaceBlockEvent(level, pos, pos.above(), Blocks.COBBLESTONE.defaultBlockState());
            NeoForge.EVENT_BUS.post(flowed);
            PistonEvent.Pre pushed = new PistonEvent.Pre(level, pos, Direction.UP, PistonEvent.PistonMoveType.EXTEND);
            NeoForge.EVENT_BUS.post(pushed);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(inside ? 12.5 : 20.5, FLOOR, 12.5));
            LivingDestroyBlockEvent ate = new LivingDestroyBlockEvent(zombie, pos, dirt);
            NeoForge.EVENT_BUS.post(ate);
            EntityMobGriefingEvent griefed = new EntityMobGriefingEvent(level, zombie);
            NeoForge.EVENT_BUS.post(griefed);
            MobSpawnEvent.PositionCheck spawned = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
            NeoForge.EVENT_BUS.post(spawned);
            String where = inside ? "inside" : "outside";
            check(broke.isCanceled() == inside, "breaking a block " + where);
            check(placed.isCanceled() == inside, "placing a block " + where);
            check(flowed.isCanceled() == inside, "stone made by water and lava " + where);
            check(pushed.isCanceled() == inside, "a piston " + where);
            check(ate.isCanceled() == inside, "a mob breaking a block " + where);
            check(griefed.canGrief() == !inside, "mob griefing " + where);
            check(spawned.getResult() == (inside ? MobSpawnEvent.PositionCheck.Result.FAIL : MobSpawnEvent.PositionCheck.Result.DEFAULT),
                    "a natural spawn " + where);
        }

        // A bucket is stopped where it would pour; any other item is none of the arena's business.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(in), Direction.UP, in, false);
        PlayerInteractEvent.RightClickBlock bucket = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, in, hit);
        NeoForge.EVENT_BUS.post(bucket);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        PlayerInteractEvent.RightClickBlock stick = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, in, hit);
        NeoForge.EVENT_BUS.post(stick);
        check(bucket.isCanceled() && !stick.isCanceled(), "a bucket is stopped, a stick is not");
        check("message.enhanced.boss.protected".equals(lastActionBarKey(player)), "and the player is told why");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_arena_explosion", timeoutTicks = 40)
    public static void explosionsInAProtectedArenaBreakNothingUntilTheFightIsOver(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(4.5, FLOOR, 4.5);
        start(f, BossKind.APOCALYPSE, player, BossFrameworkTests.fight(f, BossKind.APOCALYPSE, Containment.NONE, true));
        List<BlockPos> blocks = List.of(at(helper, 10, 2, 10), at(helper, 11, 2, 10), at(helper, 10, 2, 11));
        blocks.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState()));
        Vec3 centre = Vec3.atCenterOf(at(helper, 10, 2, 10)).add(0, 0.5, 0);

        level.explode(null, centre.x, centre.y, centre.z, 3.0F, Level.ExplosionInteraction.TNT);
        check(blocks.stream().allMatch(pos -> level.getBlockState(pos).is(Blocks.DIRT)), "the explosion hurts and breaks nothing");

        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
        level.explode(null, centre.x, centre.y, centre.z, 3.0F, Level.ExplosionInteraction.TNT);
        check(blocks.stream().noneMatch(pos -> level.getBlockState(pos).is(Blocks.DIRT)), "after the fight it breaks them");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_arena_switches")
    public static void protectionFollowsTheArenaTheConfigAndTheHeightOfTheCylinder(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(4.5, FLOOR, 4.5);
        TestFight fight = start(f, BossKind.ULTRON, player, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
        BlockPos in = at(helper, 12, 2, 10);
        check(BossFights.protects(level, in), "protected");
        f.config(BossConfig.PROTECT_ARENA, false);
        check(!BossFights.protects(level, in), "unless the server switches it off");
        f.config(BossConfig.PROTECT_ARENA, true);
        fight.arena().vertical(in.getY() + 3, in.getY() + 10);
        check(!BossFights.protects(level, in) && BossFights.protects(level, in.above(4)), "the cylinder has its own height");
        BossFights.clear();

        TestFight open = start(f, BossKind.THANOS, player, BossFrameworkTests.fight(f, BossKind.THANOS, Containment.NONE, false));
        check(!BossFights.protects(level, in) && open.arena().gates() == false, "Thanos's arena protects nothing and holds nobody");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The mod's own abilities ask the arena

    @GameTest(template = ARENA, batch = "boss_arena_abilities", timeoutTicks = 80)
    public static void wandasLiftingTommysAccelerationAndLokisIceLeaveAProtectedArenaAlone(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = f.player(7.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer tommy = f.player(7.5, FLOOR, 12.5, Power.TOMMY);
        ServerPlayer loki = f.player(10.5, FLOOR, 5.5, Power.LOKI);
        TestFight fight = start(f, BossKind.ULTRON, wanda, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
        BlockPos lift = at(helper, 10, 2, 10);
        BlockPos shake = at(helper, 10, 2, 12);
        level.setBlockAndUpdate(lift, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(shake, Blocks.DIRT.defaultBlockState());
        TestKit.lookAt(wanda, Vec3.atCenterOf(lift));
        TestKit.lookAt(tommy, Vec3.atCenterOf(shake));

        check(BlockLifting.tryLift(wanda) && level.getBlockState(lift).is(Blocks.DIRT), "Wanda cannot tear a block out of the arena");
        AbilityHandler.onKey(tommy, 5, true);
        check(level.getBlockState(shake).is(Blocks.DIRT), "Tommy cannot shake one apart");
        check("message.enhanced.boss.protected".equals(lastActionBarKey(tommy)), "and is told why");

        // Loki's Frost Giant freezes still water under his feet; in the arena the water stays water.
        AbilityHandler.onKey(loki, 5, true);
        AbilityHandler.onKey(loki, 5, false);
        BlockPos water = at(helper, 10, 1, 6);
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        check(BossFights.protects(level, water), "the water is in the arena");

        helper.runAfterDelay(2, () -> {
            loki.setOnGround(true);
            PowerTicker.tick(loki);
            check(level.getBlockState(water).is(Blocks.WATER), "no ice forms in the arena");
            BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
            check(BlockLifting.tryLift(wanda) && !level.getBlockState(lift).is(Blocks.DIRT), "the fight over, Wanda can lift it");
            AbilityHandler.onKey(tommy, 5, true);
            check(!level.getBlockState(shake).is(Blocks.DIRT), "and Tommy can shake the other apart");
            PowerTicker.tick(loki);
            check(level.getBlockState(water).is(Blocks.FROSTED_ICE), "and the ice forms where the fight was");
            Telekinesis.clear();
            LokiAbilities.clear();
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Containment

    @GameTest(template = ARENA, batch = "boss_arena_field", timeoutTicks = 100)
    public static void theFieldRescuesAFighterWhoStraysWithADamageThatCannotKill(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer weak = f.player(10.5, FLOOR, 8.5);
        ServerPlayer creative = f.player(10.5, FLOOR, 12.5, GameType.CREATIVE);
        ServerPlayer spectator = f.player(12.5, FLOOR, 12.5, GameType.SPECTATOR);
        ServerPlayer faraway = f.player(1.5, FLOOR, 1.5);
        TestFight fight = start(f, BossKind.ULTRON, fighter, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.FIELD, false));
        weak.setHealth(1.5F);
        fighter.setHealth(10.0F);
        helper.runAfterDelay(3, () -> {
            moveTo(helper, fighter, 17.5, 10.5);
            moveTo(helper, weak, 10.5, 17.5);
            moveTo(helper, creative, 3.5, 10.5);
            moveTo(helper, spectator, 10.5, 3.5);
            check(fight.arena().distance(fighter.position()) > fight.arena().containRadius(), "the fighter stands outside the field");
        });
        helper.runAfterDelay(14, () -> {
            check(fight.arena().distance(fighter.position()) <= fight.arena().containRadius() - 2.0, "the fighter was brought back, three blocks inside the edge");
            assertNear(fighter.getHealth(), 8.0, 0.01, "at the price of 2 health");
            check(weak.getHealth() >= 1.0F && weak.isAlive(), "a damage that cannot kill: " + weak.getHealth());
            check(fighter.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and a few seconds of Slowness");
            check(fight.arena().distance(creative.position()) <= fight.arena().containRadius() && creative.getHealth() == 20.0F,
                    "a Creative fighter is brought back, unhurt");
            check(fight.arena().distance(spectator.position()) > fight.arena().containRadius(), "a Spectator is none of its business");
            check("message.enhanced.boss.pushed".equals(lastActionBarKey(fighter)), "and the fighter is told");
            moveTo(helper, fighter, 17.5, 10.5);
            moveTo(helper, faraway, 1.5, 1.5);
            helper.runAfterDelay(12, () -> {
                check(fight.arena().distance(fighter.position()) > fight.arena().containRadius(), "the second time within five seconds nothing happens");
                check(fight.arena().distance(faraway.position()) > fight.arena().joinRadius(), "and somebody beyond the join radius is left alone");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, batch = "boss_arena_ending", timeoutTicks = 100)
    public static void theArenaHoldsItsFightersWhileTheEndingLasts(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        TestFight fight = BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.FIELD, false);
        fight.windDownNeeded = 80;
        start(f, BossKind.ULTRON, fighter, fight);
        helper.runAfterDelay(3, () -> {
            fight.end(BossEnd.ABORT);
            moveTo(helper, fighter, 17.5, 10.5);
        });
        helper.runAfterDelay(20, () -> {
            check(fight.isOver() && !fight.isFinished(), "the fight is winding down");
            check(fight.arena().distance(fighter.position()) <= fight.arena().containRadius(),
                    "and the field still brings a fighter back (Ultron's island is lowered with everybody on it)");
            helper.succeed();
        });
    }

    /**
     * The real arena (not the test's, whose ground is the floor of the test) puts a stray fighter on the ground of
     * the column it brings them to, and its tickets load the chunks round it. The test world is flat, and the arena
     * stands beside the structure, on open ground.
     */
    @GameTest(template = ARENA, batch = "boss_arena_ground", timeoutTicks = 160)
    public static void theRealFieldPutsAFighterOnTheGroundAndItsTicketsLoadTheChunks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        Vec3 centre = helper.absoluteVec(new Vec3(48.5, FLOOR, 10.5));
        TestFight fight = new TestFight(level, BossKind.APOCALYPSE, new BossArena(level, centre, 8.0,
                Containment.FIELD, false).containAt(6.0).joinWithin(9.0).tractorDepth(0.0));
        start(f, BossKind.APOCALYPSE, fighter, fight);
        helper.runAfterDelay(30, () -> {
            check(level.hasChunk(BlockPos.containing(centre).getX() >> 4, BlockPos.containing(centre).getZ() >> 4), "the arena's chunks are loaded");
            Vec3 stray = centre.add(7.0, 0.0, 0.0);
            fighter.teleportTo(level, stray.x, stray.y + 1.0, stray.z, 0.0F, 0.0F);
        });
        f.everyTick(120, tick -> {
            if (tick < 45) {
                return false;
            }
            check(fight.arena().distance(fighter.position()) <= fight.arena().containRadius() - 2.0, "the fighter was brought back inside");
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(fighter.getX()), (int) Math.floor(fighter.getZ()));
            assertNear(fighter.getY(), ground, 0.01, "and stands on the ground there, not on the floor of the test");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_arena_wall", timeoutTicks = 100)
    public static void theWallPushesBackOncePerSecondAndStopsFlightAtTheTop(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer flier = f.player(10.5, FLOOR, 8.5);
        TestFight fight = start(f, BossKind.APOCALYPSE, fighter, BossFrameworkTests.fight(f, BossKind.APOCALYPSE, Containment.WALL, false));
        double top = helper.absoluteVec(BossFrameworkTests.CENTRE).y + 4.0;
        fight.arena().vertical(helper.absoluteVec(BossFrameworkTests.CENTRE).y - 20.0, top);
        fighter.setHealth(10.0F);
        helper.runAfterDelay(3, () -> {
            moveTo(helper, fighter, 17.5, 10.5);
            Vec3 high = helper.absoluteVec(new Vec3(10.5, FLOOR + 6.0, 8.5));
            flier.teleportTo(helper.getLevel(), high.x, high.y, high.z, 0.0F, 0.0F);
        });
        helper.runAfterDelay(14, () -> {
            double distance = fight.arena().distance(fighter.position());
            check(distance <= fight.arena().containRadius() && distance > 3.0, "pushed back to the inside of the wall, not to the middle: " + distance);
            assertNear(fighter.getHealth(), 9.0, 0.01, "for 1 health");
            check(!fighter.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "with no Slowness");
            check(flier.getY() <= top, "and a flier is stopped at the top of the cylinder: " + (flier.getY() - top));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "boss_arena_none", timeoutTicks = 60)
    public static void withoutContainmentNobodyIsHeldAndTheTractorLiftsWhoSinksTooDeep(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer sunk = f.player(12.5, FLOOR, 10.5);
        TestFight fight = start(f, BossKind.THANOS, fighter, BossFrameworkTests.fight(f, BossKind.THANOS, Containment.NONE, false));
        fight.arena().tractorDepth(2.0);
        helper.runAfterDelay(3, () -> {
            moveTo(helper, fighter, 17.5, 10.5);
            Vec3 below = helper.absoluteVec(new Vec3(12.5, FLOOR - 4.0, 10.5));
            sunk.teleportTo(helper.getLevel(), below.x, below.y, below.z, 0.0F, 0.0F);
        });
        helper.runAfterDelay(14, () -> {
            check(fight.arena().distance(fighter.position()) > fight.arena().containRadius(), "nobody holds a fighter in without containment");
            assertNear(sunk.getY(), helper.absoluteVec(BossFrameworkTests.CENTRE).y, 0.01, "but the tractor lifts the one who sank to the surface");
            check(sunk.getHealth() == 20.0F, "without a scratch");
            check("message.enhanced.boss.lifted".equals(lastActionBarKey(sunk)), "and says so");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The teleport gate

    @GameTest(template = ARENA, batch = "boss_arena_gate")
    public static void aFighterInsideTheArenaCannotTeleportOutOfItNorToAnotherDimension(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        ServerPlayer stranger = f.player(1.5, FLOOR, 1.5);
        TestFight fight = start(f, BossKind.ULTRON, fighter, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.FIELD, false));
        Vec3 inside = helper.absoluteVec(new Vec3(12.5, FLOOR, 10.5));
        Vec3 outside = helper.absoluteVec(new Vec3(19.5, FLOOR, 19.5));
        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "the test server has a Nether");

        check(BossFights.allowsTeleport(level, fighter, inside), "inside is allowed");
        check(!BossFights.allowsTeleport(level, fighter, outside), "out of the cylinder is not");
        check(!BossFights.allowsTeleport(nether, fighter, inside), "nor is another dimension");
        check(BossFights.allowsTeleport(level, stranger, outside), "somebody else's teleports are their own");

        check(!HexManager.onTeleport(level, fighter, outside), "the Hex gate, where every mod teleport and Ender Pearl goes, refuses");
        check("message.enhanced.boss.held".equals(lastActionBarKey(fighter)), "and says why");
        check(HexManager.onTeleport(level, fighter, inside) && HexManager.onTeleport(level, stranger, outside), "and allows the rest");

        EntityTeleportEvent.ChorusFruit fruit = new EntityTeleportEvent.ChorusFruit(fighter, outside.x, outside.y, outside.z);
        NeoForge.EVENT_BUS.post(fruit);
        EntityTeleportEvent.ChorusFruit nearby = new EntityTeleportEvent.ChorusFruit(fighter, inside.x, inside.y, inside.z);
        NeoForge.EVENT_BUS.post(nearby);
        check(fruit.isCanceled() && !nearby.isCanceled(), "a Chorus Fruit cannot take the fighter out");
        EntityTravelToDimensionEvent portal = new EntityTravelToDimensionEvent(fighter, Level.NETHER);
        NeoForge.EVENT_BUS.post(portal);
        EntityTravelToDimensionEvent strangerPortal = new EntityTravelToDimensionEvent(stranger, Level.NETHER);
        NeoForge.EVENT_BUS.post(strangerPortal);
        check(portal.isCanceled() && !strangerPortal.isCanceled(), "nor can a portal");

        // Whoever is already outside the cylinder (but near enough to fight) is free to go.
        moveTo(helper, stranger, 19.0, 10.5);
        helper.runAfterDelay(3, () -> {
            check(fight.isParticipant(stranger.getUUID()) && !fight.arena().contains(stranger.position()), "the stranger fights from outside the cylinder");
            check(BossFights.allowsTeleport(level, stranger, outside), "and may teleport");
            BossFights.clear();
            check(BossFights.allowsTeleport(level, fighter, outside), "once the fight is over, everybody may");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "boss_arena_no_gate")
    public static void anArenaWithoutContainmentGatesNothing(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        start(f, BossKind.THANOS, fighter, BossFrameworkTests.fight(f, BossKind.THANOS, Containment.NONE, false));
        Vec3 outside = helper.absoluteVec(new Vec3(19.5, FLOOR, 19.5));
        check(BossFights.allowsTeleport(level, fighter, outside) && BossFights.allowsTeleport(level, fighter, outside.add(1000, 0, 0)),
                "Thanos's fighters teleport as they like");
        check(HexManager.onTeleport(level, fighter, outside), "through the Hex gate too");
        EntityTravelToDimensionEvent portal = new EntityTravelToDimensionEvent(fighter, Level.NETHER);
        NeoForge.EVENT_BUS.post(portal);
        check(!portal.isCanceled(), "and by portal");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drops

    @GameTest(template = ARENA, batch = "boss_arena_drops", timeoutTicks = 40)
    public static void whatAFighterDropsWhenTheyDieInAnArenaThatKeepsDropsLastsUntilTheFightIsOver(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer fighter = f.player(10.5, FLOOR, 10.5);
        TestFight fight = BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true);
        fight.arena().keepDrops(true);
        start(f, BossKind.ULTRON, fighter, fight);
        helper.runAfterDelay(3, () -> {
            fighter.getInventory().add(new ItemStack(Items.DIAMOND));
            fighter.kill();
            AABB around = new AABB(fighter.blockPosition()).inflate(4);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Items.DIAMOND));
            check(drops.size() == 1 && drops.get(0).getAge() < -30000, "the drops will not despawn while the fight lasts");
            BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
            int age = drops.get(0).getAge();
            check(age > -6100 && age < -5900, "and get five more minutes when it ends: " + age);
            helper.succeed();
        });
    }

    private BossArenaTests() {
    }
}
