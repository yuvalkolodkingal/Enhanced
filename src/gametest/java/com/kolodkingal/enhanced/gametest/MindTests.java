package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.mind.MindBuilder;
import com.kolodkingal.enhanced.mind.MindNpcEntity;
import com.kolodkingal.enhanced.mind.MindPalace;
import com.kolodkingal.enhanced.mind.MindProgress;
import com.kolodkingal.enhanced.mind.MindRealm;
import com.kolodkingal.enhanced.mind.MindTalk;
import com.kolodkingal.enhanced.nano.HortonManorPiece;
import com.kolodkingal.enhanced.nano.NanoUltron;
import com.kolodkingal.enhanced.nano.NanoUltronEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.EarnedLooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Vision's mind and the nanotech (3.12): the Mind Palace takes a still Vision in after five seconds (and moving breaks
 * it off); his island has its floors, its elevator on every one and its people; the robots' gate opens only for human
 * answers; the Human Torch's talk ends with the Source Code, after which the floors below are rock and their people
 * gone; White Vision earns the Source Code look and Change Look skips it until then; every line of the talks has its
 * words in English and Hebrew; the Horton manor's piece builds the storage; the storage wakes Nano Ultron or gives
 * Ultron his nanotech body; the nano form lets energy through but not blows. Batch {@code mind}.
 */
public final class MindTests {
    private static final String BATCH = "mind";

    static ServerLevel ensureMind(MinecraftServer server) {
        ServerLevel existing = server.getLevel(MindRealm.LEVEL);
        if (existing != null) {
            return existing;
        }
        return AsgardTestSupport.makeLevel(server, MindRealm.LEVEL, "/data/enhanced/dimension/vision_mind.json");
    }

    /** A Vision in his own mind (entered at once, not by the Mind Palace). */
    static ServerPlayer visitor(TestKit.Fixture f, GameTestHelper helper) {
        ensureMind(helper.getLevel().getServer());
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        check(MindRealm.enter(vision), "into his mind");
        return vision;
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theMindPalaceTakesAStillVisionInAndOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ensureMind(helper.getLevel().getServer());
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        MindPalace.press(vision);
        helper.onEachTick(() -> MindPalace.tick(vision));
        helper.runAfterDelay(MindPalace.CHANNEL_TICKS - 10, () -> check(!MindRealm.in(vision), "not before five seconds"));
        helper.runAfterDelay(MindPalace.CHANNEL_TICKS + 5, () -> {
            check(MindRealm.in(vision), "in his mind");
            check(MindProgress.of(vision).hasReturn(), "his way back saved");
            MindRealm.leave(vision);
            check(!MindRealm.in(vision) && vision.level() == helper.getLevel(), "back where he was");
            check(vision.position().distanceTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, FLOOR, 8.5))) < 9.0,
                    "near the spot he left");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void movingBreaksTheMindPalaceOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ensureMind(helper.getLevel().getServer());
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        MindPalace.press(vision);
        helper.onEachTick(() -> MindPalace.tick(vision));
        helper.runAfterDelay(20, () -> vision.teleportTo(vision.getX() + 1.0, vision.getY(), vision.getZ()));
        helper.runAfterDelay(MindPalace.CHANNEL_TICKS + 5, () -> {
            check(!MindRealm.in(vision) && !MindPalace.channeling(vision), "moved: still here, nothing pending");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theHouseHasItsFloorsElevatorAndPeople(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = visitor(f, helper);
        ServerLevel mind = vision.serverLevel();
        BlockPos o = MindRealm.origin(MindProgress.of(vision).plot());
        for (int floor : new int[] {MindBuilder.GROUND, MindBuilder.UPPER, MindBuilder.TOP, MindBuilder.BELOW, MindBuilder.BOTTOM}) {
            BlockPos stop = MindBuilder.liftStop(o, floor);
            check(mind.getBlockState(stop).isAir() && mind.getBlockState(stop.above()).isAir(), "room to stand at the stop of " + floor);
            check(mind.getBlockState(o.offset(MindBuilder.HALF_X - 1, floor + 1, 5)).is(ModBlocks.MIND_ELEVATOR.get()),
                    "the elevator's panel on floor " + floor);
        }
        check(mind.getBlockState(o.offset(-14, MindBuilder.GROUND + 1, 5)).is(Blocks.WHITE_CONCRETE), "the cinema's screen");
        check(mind.getBlockState(o.offset(-4, MindBuilder.GROUND + 1, 6)).is(ModBlocks.CINEMA_PROJECTOR.get()), "its projector");
        check(mind.getBlockState(o.offset(13, MindBuilder.GROUND, -10)).is(Blocks.BARREL), "the fridge");
        check(mind.getBlockState(MindBuilder.arrival(o).below()).isSolid(), "ground at the arrival");
        Map<MindNpcEntity.Role, Integer> people = new EnumMap<>(MindNpcEntity.Role.class);
        for (MindNpcEntity npc : mind.getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70))) {
            people.merge(npc.role(), 1, Integer::sum);
        }
        check(people.getOrDefault(MindNpcEntity.Role.DUM_E, 0) == 1 && people.getOrDefault(MindNpcEntity.Role.EDITH, 0) == 1
                && people.getOrDefault(MindNpcEntity.Role.FRIDAY, 0) == 1 && people.getOrDefault(MindNpcEntity.Role.ROBOT, 0) == 3
                && people.getOrDefault(MindNpcEntity.Role.ULTRON, 0) == 1 && people.getOrDefault(MindNpcEntity.Role.TORCH, 0) == 1,
                "everyone in his place: " + people);
        // a second visit makes no one twice
        MindRealm.leave(vision);
        check(MindRealm.enter(vision), "in again");
        int count = mind.getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70)).size();
        check(count == 8, "still eight: " + count);
        MindRealm.leave(vision);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRobotsOpenTheGateOnlyForHumanAnswers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = visitor(f, helper);
        BlockPos o = MindRealm.origin(MindProgress.of(vision).plot());
        ServerLevel mind = vision.serverLevel();
        MindTalk.start(vision, "robot", null, false);
        MindTalk.choose(vision, "0");
        MindTalk.choose(vision, "0"); // the robot's answer
        check(!MindProgress.of(vision).gateOpen() && mind.getBlockState(o.offset(-6, MindBuilder.TOP, 0)).is(Blocks.IRON_BARS),
                "a robot's answer: the gate stays shut");
        MindTalk.choose(vision, "bye");
        MindTalk.start(vision, "robot", null, false);
        for (String option : new String[] {"0", "1", "1", "1", "0"}) {
            MindTalk.choose(vision, option);
        }
        check(MindProgress.of(vision).gateOpen() && mind.getBlockState(o.offset(-6, MindBuilder.TOP, 0)).isAir(), "human: the gate opens");
        MindRealm.leave(vision);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theHumanTorchGivesTheSourceCodeAndTheFloorsBelowGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = visitor(f, helper);
        BlockPos o = MindRealm.origin(MindProgress.of(vision).plot());
        ServerLevel mind = vision.serverLevel();
        MindNpcEntity torch = null;
        for (MindNpcEntity npc : mind.getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70))) {
            if (npc.role() == MindNpcEntity.Role.TORCH) {
                torch = npc;
            }
        }
        check(torch != null && torch.ablaze(), "the Human Torch, in flames");
        MindTalk.start(vision, "torch", torch, true);
        MindTalk.choose(vision, "");
        check(MindTalk.talking(vision), "a forced talk cannot be closed");
        MindTalk.choose(vision, "0");
        MindTalk.choose(vision, "0");
        check(!torch.ablaze(), "the flames go out");
        for (int i = 0; i < 40 && MindTalk.talking(vision); i++) {
            MindTalk.choose(vision, "0");
        }
        check(!MindTalk.talking(vision), "the talk is over");
        check(vision.getInventory().contains(new ItemStack(ModItems.SOURCE_CODE.get())), "he holds the Source Code");
        check(MindProgress.of(vision).torchDone(), "remembered");
        check(mind.getBlockState(o.offset(-15, MindBuilder.BOTTOM, 5)).is(Blocks.STONE), "the workshop is rock");
        check(mind.getBlockState(o.offset(6, MindBuilder.BELOW + 1, 5)).is(Blocks.STONE), "and Ultron's little room");
        check(mind.getEntitiesOfClass(MindNpcEntity.class, new AABB(o).inflate(70, 80, 70),
                n -> n.role() == MindNpcEntity.Role.TORCH || n.role() == MindNpcEntity.Role.ULTRON
                        || n.role() == MindNpcEntity.Role.HORTON).isEmpty(), "Ultron, the Torch and Horton are gone");
        check(vision.getY() >= o.getY() - 0.5, "the elevator took him up");
        MindRealm.leave(vision);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void whiteVisionEarnsTheSourceCodeLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        // not earned: Change Look never lands on it
        for (int i = 0; i < 6; i++) {
            Powers.cycleVariant(vision, 1);
            check(!"source_code".equals(Powers.get(vision).variant()), "skipped while not earned");
        }
        Powers.setVariant(vision, Variants.get(Power.VISION, "vision"));
        vision.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check("vision".equals(Powers.get(vision).variant()), "red Vision cannot take it in");
        Powers.setVariant(vision, Variants.get(Power.VISION, "white_vision"));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check("source_code".equals(Powers.get(vision).variant()), "White Vision becomes Source Code");
        check(EarnedLooks.has(vision, Variants.get(Power.VISION, "source_code")), "earned");
        boolean seen = false;
        for (int i = 0; i < Variants.of(Power.VISION).size(); i++) {
            Powers.cycleVariant(vision, 1);
            seen |= "source_code".equals(Powers.get(vision).variant());
        }
        check(seen, "Change Look reaches it now");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theTalksHaveTheirWords(GameTestHelper helper) throws Exception {
        for (String lang : new String[] {"en_us", "he_il"}) {
            JsonObject words;
            try (InputStream in = MindTests.class.getResourceAsStream("/assets/enhanced/lang/" + lang + ".json")) {
                check(in != null, lang + " is there");
                words = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (String key : MindTalk.keys()) {
                check(words.has(key), lang + " has " + key);
            }
            for (String key : new String[] {"item.enhanced.source_code", "item.enhanced.ultron_hat", "item.enhanced.popcorn",
                    "entity.enhanced.nano_ultron", "variant.enhanced.vision.source_code", "variant.enhanced.ultron.nano",
                    "hero_module.enhanced.vision.mind_palace", "mind.enhanced.elevator.down"}) {
                check(words.has(key), lang + " has " + key);
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHortonManorKeepsTheNanotech(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(16, 40, 16));
        HortonManorPiece piece = new HortonManorPiece(o);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                piece.getBoundingBox(), new ChunkPos(o), o);
        check(level.getBlockState(o.offset(8, 0, 0)).is(ModBlocks.NANO_STORAGE.get()), "the storage in its room");
        check(level.getBlockState(o.offset(0, 0, -9)).is(Blocks.IRON_DOOR), "the front door");
        BoundingBox box = piece.getBoundingBox();
        check(box.isInside(o.offset(8, 0, 0)), "inside its box");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theStorageWakesUltronOrMakesUltronNanotech(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos storage = helper.absolutePos(new BlockPos(10, 2, 10));
        level.setBlockAndUpdate(storage, ModBlocks.NANO_STORAGE.get().defaultBlockState());
        ServerPlayer plain = f.player(10.5, FLOOR, 12.5);
        BlockHitResult hit = new BlockHitResult(storage.getCenter(), Direction.UP, storage, false);
        level.getBlockState(storage).useWithoutItem(level, plain, hit);
        var bosses = level.getEntitiesOfClass(NanoUltronEntity.class, new AABB(storage).inflate(8));
        check(bosses.size() == 1, "Nano Ultron rises");
        level.getBlockState(storage).useWithoutItem(level, plain, hit);
        check(level.getEntitiesOfClass(NanoUltronEntity.class, new AABB(storage).inflate(8)).size() == 1, "one at a time");
        ServerPlayer ultron = f.player(12.5, FLOOR, 12.5, Power.ULTRON);
        level.getBlockState(storage).useWithoutItem(level, ultron, hit);
        check(NanoUltron.is(ultron) && EarnedLooks.has(ultron, Variants.get(Power.ULTRON, "nano")), "Ultron takes the nanotech");
        check(NanoUltron.toggleForm(ultron) && NanoUltron.inForm(ultron), "his nano form");
        check(NanoUltron.shrugsOff(ultron, level.damageSources().playerAttack(plain)), "a blow passes through");
        check(!NanoUltron.shrugsOff(ultron, level.damageSources().magic()), "magic does not");
        bosses.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }
}
