package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.OpenRoadPayload;
import com.kolodkingal.enhanced.network.RoadChoicePayload;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.EndingHall;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadEnding;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.RoadVoice;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.copy.StructureSupply;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The end of the Witches' Road (3.2, step 4): the hall after the last gate, the choice of Billy or Tommy inside the Road, the Gene used
 * up once, the farewell and the way home, and everything that can go wrong on the way (the screen closed, a logout, a death, the place he
 * entered by gone, a second player, a crafted answer). The players of the tests are brought to the hall with the trials counted as won
 * ({@link #finisher}); {@link #theWholeRoadEndsInTheHall} walks the gates of a Road of its own.
 */
public final class RoadEndingTests {
    static final String BATCH = "road_ending";
    private static final int FAREWELL = 100;

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A player with {@code genes} Genes who has won every trial; the Road takes him to its end. */
    private static ServerPlayer finisher(TestKit.Fixture f, MinecraftServer server, double x, double z, int genes) {
        ServerPlayer player = RoadTestSupport.walker(f, x, z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), genes));
        RoadTrials.setCompleted(server, player.getUUID(), RoadChallenges.count());
        check(RoadTrials.enter(player, RoadChallenges.count()), "the Road takes him to its end");
        return player;
    }

    /** For {@code thenWaitUntil}: the Road has asked the question (the light has gathered). */
    private static Runnable asked(ServerPlayer player) {
        return () -> {
            check(RoadEnding.stage(player) == RoadEnding.Stage.ASKING, "the Road has asked " + player.getGameProfile().getName() + ": " + RoadEnding.stage(player));
            check(com.kolodkingal.enhanced.road.RoadMusic.stageOf(player).equals(com.kolodkingal.enhanced.road.RoadMusic.CHOICE),
                    "3.5: the choice has its own version of the Road's song");
            player.hasChangedDimension();
        };
    }

    private static int genes(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Things in the backpack, as they are now (copies). */
    private static List<ItemStack> backpack(ServerPlayer player) {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            all.add(player.getInventory().getItem(i).copy());
        }
        return all;
    }

    private static boolean sameBackpack(List<ItemStack> before, ServerPlayer player) {
        for (int i = 0; i < 36; i++) {
            if (!ItemStack.matches(before.get(i), player.getInventory().getItem(i))) {
                return false;
            }
        }
        return true;
    }

    /** Gives the player a kit that the ending must leave as it is (the Gene stays where it is). */
    private static void kit(ServerPlayer player) {
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        player.getInventory().setItem(17, new ItemStack(Items.DIAMOND, 10));
        player.getInventory().setItem(30, new ItemStack(Items.OAK_LOG, 64));
        player.getInventory().setItem(35, new ItemStack(Items.GOLDEN_APPLE, 3));
        player.experienceLevel = 7;
        player.setHealth(6.0F);
        player.getFoodData().setFoodLevel(5);
    }

    /** The action bar lines the player has been sent (keys). */
    private static List<String> actionBar(List<Object> packets) {
        List<String> keys = new ArrayList<>();
        for (Object packet : packets) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents contents) {
                keys.add(contents.getKey());
            }
        }
        return keys;
    }

    private static List<OpenRoadPayload> screens(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket packet && packet.payload() instanceof OpenRoadPayload)
                .map(p -> (OpenRoadPayload) ((ClientboundCustomPayloadPacket) p).payload()).toList();
    }

    /** What the server does with the message the client sends when the player presses a button on the screen. */
    private static void answer(ServerPlayer player, BlockPos door, Power power) {
        try {
            Method handle = RoadChoicePayload.class.getDeclaredMethod("handle", RoadChoicePayload.class, PacketContext.class);
            handle.setAccessible(true);
            PacketContext context = (PacketContext) Proxy.newProxyInstance(PacketContext.class.getClassLoader(), new Class[]{PacketContext.class},
                    (proxy, method, args) -> method.getName().equals("player") ? player : null);
            handle.invoke(null, new RoadChoicePayload(door, power), context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void run(MinecraftServer server, ServerPlayer as, int permission, String command) {
        server.getCommands().performPrefixedCommand(as.createCommandSourceStack().withPermission(permission), command);
    }

    /** What the player must be like after the ending: out of the Road with a power, nothing of the Road left. */
    private static void checkedOut(GameTestHelper helper, MinecraftServer server, ServerPlayer player, Power power, Vec3 home, int cell, long[] chunks, int genesBefore) {
        ServerLevel road = RoadTestSupport.road(helper);
        check(!RoadDimension.in(player) && !RoadTrials.isInside(player) && player.serverLevel() == helper.getLevel(), "he is out of the Road, in the world he came from");
        check(player.position().distanceTo(home) < 0.01, "exactly where he entered: " + player.position() + " vs " + home);
        check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "in the game mode he entered in");
        check(Powers.is(player, power), "he has the power he chose: " + Powers.power(player));
        check(genes(player) == genesBefore - 1, "exactly one Gene is used up: " + genes(player) + " of " + genesBefore);
        RoadProgress progress = RoadSave.get(server).existing(player.getUUID());
        check(progress != null && progress.completed() == 0 && !progress.inside() && progress.entry() == null && progress.checkpoint() == 0 && progress.isUntouched(),
                "the Road remembers none of his trials");
        check(progress.salt() == 1L, "(the next Road would be dealt other courses)");
        check(RoadTrials.trialOf(player.getUUID()) == null && RoadEnding.stage(player) == null, "no trial, no ending");
        check(player.isAlive() && player.getHealth() == player.getMaxHealth() && player.getFoodData().getFoodLevel() == 20, "healed and fed");
        RoadSweeper.drain(server);
        check(RoadSave.get(server).cell(cell) == null && RoadTestSupport.blocksIn(road, chunks) == 0, "the hall is taken down");
        check(RoadTestSupport.tickets(road, player.getUUID()) == 0, "and nothing of the Road is held");
        check(RoadTrials.refusal(player) != null, "he cannot walk the Road again, as a hero");
    }

    // ------------------------------------------------------------------------------------------------------------
    // The choice

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void roadEndingBillyIsAwakenedAndSentHome(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 3));
        kit(player);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2000, 0));
        RoadTrials.setCompleted(server, player.getUUID(), RoadChallenges.count());
        List<ItemStack> before = backpack(player);
        Vec3 home = player.position();
        int xp = player.experienceLevel;
        check(RoadTrials.enter(player, RoadChallenges.count()), "the Road takes him to its end");
        int[] cell = new int[1];
        long[][] chunks = new long[1][];
        BlockPos[] door = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    cell[0] = trial.cell();
                    chunks[0] = trial.chunks();
                    door[0] = trial.layout().exitDoor();
                    check(trial.challenge().isEnding() && trial.challenge() == RoadChallenges.ending() && trial.challenge() instanceof EndingHall, "he is in the hall");
                    check(RoadEnding.stage(player) == RoadEnding.Stage.ARRIVING && RoadTestSupport.road(helper).getBlockState(door[0]).is(ModBlocks.WITCHES_ROAD_DOOR.get()),
                            "the light gathers; the door lies in the floor");
                    check(com.kolodkingal.enhanced.road.RoadMusic.stageOf(player).equals(EndingHall.ID), "the hall's music while the light gathers");
                    check(trial.layout().gate() == null && trial.layout().finish() == null, "it is no trial: no gate, no finish");
                    check(player.position().distanceTo(trial.layout().start()) < 0.01, "he stands at the start of the hall");
                    check(genes(player) == 3 && Powers.power(player) == Power.NONE, "nothing is spent before he chooses");
                    TestKit.sentPackets(player);
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    List<OpenRoadPayload> screens = screens(TestKit.sentPackets(player));
                    check(screens.size() == 1 && screens.get(0).ending() && !screens.get(0).trials() && screens.get(0).door().equals(door[0])
                            && screens.get(0).done() == RoadChallenges.count() && screens.get(0).total() == RoadChallenges.count(), "the screen opens in the Road: " + screens);
                    check(genes(player) == 3, "still nothing spent");
                    // He answers on the screen: Billy.
                    answer(player, door[0], Power.BILLY);
                    check(Powers.is(player, Power.BILLY), "Billy awakens");
                    check(genes(player) == 2, "one Gene used up, no more");
                    check(RoadDimension.in(player) && RoadTrials.isInside(player) && RoadEnding.isLeaving(player), "he is still in the Road: the farewell");
                    BuildData build = ModAttachments.BUILD_DATA.get(player);
                    check(build.rank() == 1 && build.meter() >= BuildData.FULL, "Billy starts at Build Magic rank 1 with a full meter");
                    check(!HexManager.onTeleport(RoadTestSupport.road(helper), player, player.position().add(500.0, 0.0, 0.0)), "no teleport takes him out early");
                    AbilityHandler.onKey(player, 1, true);
                    AbilityHandler.onKey(player, 1, false);
                    check(RoadDimension.in(player), "no ability does either");
                })
                .thenIdle(FAREWELL / 2)
                .thenExecute(() -> check(RoadDimension.in(player) && Powers.is(player, Power.BILLY), "the Road still says its farewell"))
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    checkedOut(helper, server, player, Power.BILLY, home, cell[0], chunks[0], 3);
                    for (int i = 0; i < 36; i++) {
                        if (!before.get(i).is(ModItems.SCARLET_WITCH_GENE.get())) {
                            check(ItemStack.matches(before.get(i), player.getInventory().getItem(i)), "slot " + i + " is as it was");
                        }
                    }
                    check(player.experienceLevel == xp, "his experience level is kept: " + player.experienceLevel);
                    check(player.getActiveEffects().isEmpty(), "every effect is cleared: " + player.getActiveEffects());
                    List<Object> packets = TestKit.sentPackets(player);
                    List<String> lines = actionBar(packets);
                    check(lines.stream().anyMatch(k -> k.startsWith("message.enhanced.road.voice.farewell.")), "the Road spoke its farewell: " + lines);
                    check(lines.stream().anyMatch(k -> k.startsWith("message.enhanced.road.voice.home.")), "and a last word: " + lines);
                    check(TestKit.soundsIn(packets).stream().anyMatch(s -> s.id().equals(Enhanced.id("awaken"))), "the awakening is heard");
                    check(!TestKit.particlesIn(packets).isEmpty(), "and the light column is seen");
                    check(!RoadTrials.isInside(player), "his keys are his again (the Road ignores them only inside)");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void roadEndingTommyIsAwakenedAndSentHome(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 1));
        kit(player);
        RoadTrials.setCompleted(server, player.getUUID(), RoadChallenges.count());
        Vec3 home = player.position();
        check(RoadTrials.enter(player, RoadChallenges.count()), "the Road takes him to its end");
        int[] cell = new int[1];
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    cell[0] = RoadTestSupport.trial(player).cell();
                    chunks[0] = RoadTestSupport.trial(player).chunks();
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(RoadEnding.choose(player, Power.TOMMY), "he chooses Tommy's road");
                    check(Powers.is(player, Power.TOMMY) && genes(player) == 0, "Tommy, and his only Gene is used up");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    checkedOut(helper, server, player, Power.TOMMY, home, cell[0], chunks[0], 1);
                    check(player.getInventory().getItem(17).is(Items.DIAMOND) && player.getInventory().getItem(17).getCount() == 10, "his diamonds are there");
                    player.doTick();
                    check(Powers.is(player, Power.TOMMY), "and he is Tommy in the world");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = "road_ending_instant", timeoutTicks = 500)
    public static void roadEndingWithNoFarewellSendsHimAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.ENDING_FAREWELL_SECONDS, 0);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        Vec3 home = player.position();
        RoadTrials.setCompleted(server, player.getUUID(), RoadChallenges.count());
        check(RoadTrials.enter(player, RoadChallenges.count()), "the Road takes him to its end");
        int[] cell = new int[1];
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    cell[0] = RoadTestSupport.trial(player).cell();
                    chunks[0] = RoadTestSupport.trial(player).chunks();
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(RoadEnding.choose(player, Power.BILLY), "he chooses");
                    player.hasChangedDimension();
                    checkedOut(helper, server, player, Power.BILLY, home, cell[0], chunks[0], 1);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The screen closed, the way out without choosing

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void closingTheScreenKeepsTheChoiceAndTheGene(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 2);
        BlockPos[] door = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    door[0] = RoadTestSupport.trial(player).layout().exitDoor();
                    TestKit.sentPackets(player);
                    // Too early: the light is still gathering.
                    check(!RoadEnding.reopen(player, true), "the Road is not asking yet");
                    WitchesRoad.use(RoadTestSupport.road(helper), door[0], player);
                    check(screens(TestKit.sentPackets(player)).isEmpty() && RoadDimension.in(player), "the door does not ask before the Road does, and does not take him out either");
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(screens(TestKit.sentPackets(player)).size() == 1, "the screen came by itself");
                    // He closes it (nothing is sent to the server). The state is the same; the door asks again.
                    check(RoadEnding.stage(player) == RoadEnding.Stage.ASKING && genes(player) == 2 && !Powers.power(player).hasPowers(), "closing the screen changes nothing");
                    WitchesRoad.use(RoadTestSupport.road(helper), door[0], player);
                    List<OpenRoadPayload> again = screens(TestKit.sentPackets(player));
                    check(again.size() == 1 && again.get(0).ending(), "the exit door asks again: " + again);
                    check(RoadDimension.in(player), "and one click on it does not take him out");
                    run(server, player, 0, "road choose");
                    check(screens(TestKit.sentPackets(player)).size() == 1, "so does /road choose (anyone)");
                    // Closed once more, he waits a while and answers then.
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    check(RoadEnding.stage(player) == RoadEnding.Stage.ASKING && RoadTrials.trialOf(player.getUUID()) != null, "the question waits");
                    check(RoadEnding.choose(player, Power.TOMMY) && genes(player) == 1, "he chooses: one Gene used up");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void leavingTheRoadWithoutChoosingKeepsTheGeneAndTheDoorOffersTheChoice(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 2);
        Vec3 home = player.position();
        BlockPos door = helper.absolutePos(new BlockPos(10, 2, 14));
        helper.setBlock(new BlockPos(10, 2, 14), ModBlocks.WITCHES_ROAD_DOOR.get());
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    // "Leave the Road" on the screen (the answer none, at the end of the Road).
                    answer(player, door, Power.NONE);
                    player.hasChangedDimension();
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player) && player.position().distanceTo(home) < 0.01, "he is back where he entered");
                    check(genes(player) == 2 && !Powers.power(player).hasPowers(), "with both Genes and no power");
                    RoadProgress progress = RoadSave.get(server).existing(player.getUUID());
                    check(progress != null && progress.completed() == RoadChallenges.count() && !progress.inside(), "the Road remembers he has walked it");
                    check(RoadTrials.doorMode(player) == RoadTrials.DoorMode.CHOOSE, "so the door offers the choice");
                    // At the door: the 3.1 choice, and now the Road forgets.
                    WitchesRoad.open(level, door);
                    check(WitchesRoad.choose(player, door, Power.BILLY) && Powers.is(player, Power.BILLY), "he chooses at the door");
                    check(genes(player) == 1, "one Gene used up");
                    check(RoadSave.get(server).existing(player.getUUID()) == null || RoadSave.get(server).existing(player.getUUID()).isUntouched(), "and the Road has nothing to remember");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Logout, crash, death

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void aLogoutOnTheChoiceScreenResumesAtTheChoice(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 2);
        int[] cell = new int[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    cell[0] = RoadTestSupport.trial(player).cell();
                    // He leaves the game (or the server crashes) with the screen open.
                    RoadTrials.onLogout(player);
                    check(RoadTrials.trialOf(player.getUUID()) == null && RoadEnding.stage(player) == null, "the hall is taken down");
                    RoadSweeper.drain(server);
                    RoadProgress progress = RoadSave.get(server).existing(player.getUUID());
                    check(progress != null && progress.inside() && progress.completed() == RoadChallenges.count() && progress.entry() != null, "the save says he is at the end of the Road");
                    check(genes(player) == 2 && !Powers.power(player).hasPowers(), "nothing was spent");
                    // He logs in again, over the void of the Road.
                    RoadTrials.reconcile(player);
                    check(RoadTrials.isInside(player), "the Road builds the hall again for him");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    check(RoadTrials.trialOf(player.getUUID()).challenge().isEnding() && RoadDimension.in(player), "he is in the hall");
                    TestKit.sentPackets(player);
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(screens(TestKit.sentPackets(player)).size() == 1, "the question comes back");
                    check(RoadEnding.choose(player, Power.BILLY) && Powers.is(player, Power.BILLY) && genes(player) == 1, "and he can choose");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void aLogoutInTheFarewellSendsHimHomeAsHeroWhenHeComesBack(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 2);
        Vec3 home = player.position();
        int[] cell = new int[1];
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    cell[0] = RoadTestSupport.trial(player).cell();
                    chunks[0] = RoadTestSupport.trial(player).chunks();
                    check(RoadEnding.choose(player, Power.TOMMY), "he chooses");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    check(RoadDimension.in(player) && Powers.is(player, Power.TOMMY), "in the farewell");
                    RoadTrials.onLogout(player);
                    RoadSweeper.drain(server);
                    check(RoadTrials.trialOf(player.getUUID()) == null, "the hall is gone");
                    check(genes(player) == 1, "the Gene is spent already");
                    RoadTrials.reconcile(player);
                    player.hasChangedDimension();
                    checkedOut(helper, server, player, Power.TOMMY, home, cell[0], chunks[0], 2);
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void deathAtTheFinishPutsHimBackInTheHallAndTheChoiceStands(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 2);
        Vec3 home = player.position();
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), trial.layout().start().add(3.0, 0.5, -2.0));
                    player.kill();
                    check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "he does not die");
                    check(RoadDimension.in(player) && RoadTrials.trialOf(player.getUUID()) == trial, "he is in the hall still");
                    check(player.position().distanceTo(trial.layout().start()) < 0.01, "back at its start");
                    check(RoadEnding.stage(player) == RoadEnding.Stage.ASKING && genes(player) == 2 && !Powers.power(player).hasPowers(), "the question stands, nothing is lost");
                    // The void is no different.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), trial.layout().start().add(0.0, -40.0, 0.0));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.position().distanceTo(trial.layout().start()) < 0.01, "a fall off the hall puts him back too");
                    check(RoadEnding.choose(player, Power.BILLY), "and he can choose");
                    // And a death in the farewell does not stop it.
                    player.kill();
                    check(player.isAlive() && RoadDimension.in(player) && Powers.is(player, Power.BILLY), "he is not killed in the farewell either");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    check(player.isAlive() && player.position().distanceTo(home) < 0.01 && Powers.is(player, Power.BILLY) && genes(player) == 1, "he comes home alive, as Billy");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Where he comes out

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void ifThePlaceHeEnteredByIsGoneHeComesOutAtTheDoorHeOpened(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 1);
        BlockPos door = helper.absolutePos(new BlockPos(16, 2, 16));
        helper.setBlock(new BlockPos(16, 2, 16), ModBlocks.WITCHES_ROAD_DOOR.get());
        Vec3 entry = player.position();
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    // The place is solid stone now (a house was built where he stood).
                    BlockPos feet = BlockPos.containing(entry);
                    for (int dy = 0; dy <= 9; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                level.setBlock(feet.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 3);
                            }
                        }
                    }
                    RoadSave.get(server).progress(player.getUUID()).setEntry(new RoadProgress.Entry(level.dimension(), entry, 90.0F, 0.0F, GameType.SURVIVAL.getId(), door));
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> check(RoadEnding.choose(player, Power.BILLY), "he chooses"))
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    Vec3 top = new Vec3(door.getX() + 0.5, door.getY() + WitchesRoad.DOOR_HEIGHT, door.getZ() + 0.5);
                    check(player.serverLevel() == level && player.position().distanceTo(top) < 0.01, "he stands on the door he opened: " + player.position() + " vs " + top);
                    check(Powers.is(player, Power.BILLY) && genes(player) == 0, "Billy, with the Gene used");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void ifTheDoorIsGoneTooOrTheWorldHeCameFromIsNotLoadedHeComesOutAtTheWorldsSpawn(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        ServerPlayer gone = finisher(f, server, 6.5, 6.5, 1);
        ServerPlayer vanished = finisher(f, server, 14.5, 14.5, 1);
        BlockPos missing = helper.absolutePos(new BlockPos(16, 2, 4));
        Vec3 entry = gone.position();
        ResourceKey<Level> nowhere = ResourceKey.create(Registries.DIMENSION, Enhanced.id("vanished"));
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(gone))
                .thenWaitUntil(RoadTestSupport.active(vanished))
                .thenExecute(() -> {
                    BlockPos feet = BlockPos.containing(entry);
                    for (int dy = 0; dy <= 9; dy++) {
                        level.setBlock(feet.above(dy), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(feet.above(dy).east(), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(feet.above(dy).west(), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(feet.above(dy).north(), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(feet.above(dy).south(), Blocks.STONE.defaultBlockState(), 3);
                    }
                    // His door was never there (nothing at all stands on it any more), and the other came from a world that is gone.
                    RoadSave.get(server).progress(gone.getUUID()).setEntry(new RoadProgress.Entry(level.dimension(), entry, 0.0F, 0.0F, GameType.SURVIVAL.getId(), missing));
                    RoadSave.get(server).progress(vanished.getUUID()).setEntry(new RoadProgress.Entry(nowhere, new Vec3(5.5, 70.0, 5.5), 0.0F, 0.0F, GameType.SURVIVAL.getId()));
                })
                .thenWaitUntil(asked(gone))
                .thenWaitUntil(asked(vanished))
                .thenExecute(() -> {
                    check(RoadEnding.choose(gone, Power.BILLY) && RoadEnding.choose(vanished, Power.TOMMY), "both choose");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(gone) && !RoadDimension.in(vanished), "both are out"))
                .thenExecute(() -> {
                    gone.hasChangedDimension();
                    vanished.hasChangedDimension();
                    ServerLevel overworld = server.overworld();
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    for (ServerPlayer p : new ServerPlayer[]{gone, vanished}) {
                        check(p.serverLevel() == overworld, "in the overworld");
                        check(Math.abs(p.getX() - (spawn.getX() + 0.5)) < 1.0 && Math.abs(p.getZ() - (spawn.getZ() + 0.5)) < 1.0, "at the world's spawn: " + p.position() + " vs " + spawn);
                        check(overworld.noCollision(p), "in the open, not inside a block");
                    }
                    check(Powers.is(gone, Power.BILLY) && Powers.is(vanished, Power.TOMMY), "with their powers");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 900)
    public static void heComesOutInTheOtherDimensionHeCameFrom(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "the Nether exists");
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 1);
        BlockPos platform = new BlockPos(2, 69, 2);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    for (int x = 1; x <= 3; x++) {
                        for (int z = 1; z <= 3; z++) {
                            nether.setBlock(new BlockPos(x, 69, z), Blocks.NETHERRACK.defaultBlockState(), 3);
                            for (int y = 70; y <= 73; y++) {
                                nether.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                    RoadSave.get(server).progress(player.getUUID()).setEntry(new RoadProgress.Entry(Level.NETHER, new Vec3(2.5, 70.0, 2.5), 45.0F, 5.0F,
                            GameType.SURVIVAL.getId()));
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> check(RoadEnding.choose(player, Power.TOMMY), "he chooses"))
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    check(player.serverLevel() == nether, "he is in the Nether, where he entered");
                    check(player.position().distanceTo(new Vec3(2.5, 70.0, 2.5)) < 0.01 && player.getYRot() == 45.0F, "on the spot: " + player.position());
                    check(Powers.is(player, Power.TOMMY), "as Tommy");
                    // (The test leaves the Nether as it was.)
                    for (int x = 1; x <= 3; x++) {
                        for (int z = 1; z <= 3; z++) {
                            nether.setBlock(new BlockPos(x, 69, z), Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                    RoadTestSupport.put(player, helper.getLevel(), helper.absoluteVec(new Vec3(10.5, 2.0, 10.5)));
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Exactly once, a second player, a crafted answer

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void theGeneIsUsedUpExactlyOnceAndThereIsNoSecondEnding(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 3);
        BlockPos[] door = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    door[0] = RoadTestSupport.trial(player).layout().exitDoor();
                    check(RoadEnding.choose(player, Power.BILLY), "he chooses Billy");
                    // The same tick, the same screen answered again (a double click, a modified client).
                    check(!RoadEnding.choose(player, Power.BILLY) && !RoadEnding.choose(player, Power.TOMMY), "no second answer");
                    answer(player, door[0], Power.TOMMY);
                    answer(player, door[0], Power.BILLY);
                    answer(player, door[0], Power.NONE);
                    check(RoadDimension.in(player) && Powers.is(player, Power.BILLY), "the first answer stands, and \"leave\" does not cut the farewell short");
                    check(genes(player) == 2, "exactly one Gene is gone: " + genes(player));
                    check(!RoadEnding.reopen(player, true), "the screen is not asked again");
                    WitchesRoad.use(RoadTestSupport.road(helper), door[0], player);
                    check(RoadDimension.in(player), "the door does nothing now");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "the farewell is over"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    check(genes(player) == 2 && Powers.is(player, Power.BILLY), "he is home with two Genes");
                    check(!RoadEnding.choose(player, Power.TOMMY) && !WitchesRoad.choose(player, door[0], Power.TOMMY) && Powers.is(player, Power.BILLY) && genes(player) == 2,
                            "a hero cannot choose again, here or at a door");
                    check(RoadTrials.refusal(player) != null && !RoadTrials.enter(player), "and cannot walk the Road");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 800)
    public static void twoPlayersAtTheEndEachHaveTheirOwnHallAndTheirOwnChoice(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = finisher(f, server, 6.5, 6.5, 2);
        ServerPlayer b = finisher(f, server, 14.5, 14.5, 1);
        Vec3 homeA = a.position();
        Vec3 homeB = b.position();
        int[] cells = new int[2];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(a))
                .thenWaitUntil(RoadTestSupport.active(b))
                .thenWaitUntil(asked(a))
                .thenWaitUntil(asked(b))
                .thenExecute(() -> {
                    cells[0] = RoadTestSupport.trial(a).cell();
                    cells[1] = RoadTestSupport.trial(b).cell();
                    check(cells[0] != cells[1] && a.position().distanceTo(b.position()) > 1000, "two halls, far apart");
                    check(RoadEnding.choose(a, Power.BILLY), "the first chooses Billy");
                    check(RoadEnding.stage(b) == RoadEnding.Stage.ASKING && genes(b) == 1 && !Powers.power(b).hasPowers(), "the second is not touched");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(a), "the first is sent home"))
                .thenExecute(() -> {
                    a.hasChangedDimension();
                    check(a.position().distanceTo(homeA) < 0.01 && Powers.is(a, Power.BILLY) && genes(a) == 1, "to his own entry");
                    check(RoadDimension.in(b) && RoadTrials.trialOf(b.getUUID()) != null && RoadEnding.stage(b) == RoadEnding.Stage.ASKING, "while the second waits in his hall");
                    check(RoadEnding.choose(b, Power.TOMMY), "the second chooses Tommy");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(b), "the second is sent home"))
                .thenExecute(() -> {
                    b.hasChangedDimension();
                    check(b.position().distanceTo(homeB) < 0.01 && Powers.is(b, Power.TOMMY) && genes(b) == 0, "to his own entry, with his Gene used");
                    check(Powers.is(a, Power.BILLY) && genes(a) == 1, "and the first is as he was");
                    RoadSweeper.drain(server);
                    check(RoadSave.get(server).cell(cells[0]) == null && RoadSave.get(server).cell(cells[1]) == null, "both halls are gone");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void aCraftedAnswerCannotChooseAnywhereButAtTheEnd(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        BlockPos door = helper.absolutePos(new BlockPos(10, 2, 10));
        // In the world, with a Gene, having walked the Road, but far from any open door.
        ServerPlayer world = RoadTestSupport.walker(f, 4.5, 4.5);
        RoadTrials.setCompleted(server, world.getUUID(), RoadChallenges.count());
        answer(world, door, Power.BILLY);
        check(!Powers.power(world).hasPowers() && genes(world) == 1, "no door, no choice");
        // In a trial.
        ServerPlayer trial = RoadTestSupport.walker(f, 16.5, 4.5);
        check(RoadTrials.enter(trial), "he walks the Road");
        // At the end, but the question is not asked yet / the trials are counted back / he has no Gene.
        ServerPlayer early = finisher(f, server, 4.5, 16.5, 1);
        ServerPlayer undone = finisher(f, server, 10.5, 16.5, 1);
        ServerPlayer empty = finisher(f, server, 16.5, 16.5, 1);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(trial))
                .thenWaitUntil(RoadTestSupport.active(early))
                .thenWaitUntil(RoadTestSupport.active(undone))
                .thenWaitUntil(RoadTestSupport.active(empty))
                .thenExecute(() -> {
                    answer(trial, door, Power.BILLY);
                    check(!Powers.power(trial).hasPowers() && genes(trial) == 1 && RoadTrials.isInside(trial), "in a trial the answer does nothing");
                    check(!RoadEnding.choose(early, Power.BILLY) && genes(early) == 1, "before the Road has asked, nothing");
                    answer(early, door, Power.TOMMY);
                    check(!Powers.power(early).hasPowers() && genes(early) == 1, "(not by message either)");
                })
                .thenWaitUntil(asked(undone))
                .thenWaitUntil(asked(empty))
                .thenExecute(() -> {
                    check(!RoadEnding.choose(undone, Power.MAGNETO) && !RoadEnding.choose(undone, Power.NONE) && !RoadEnding.choose(undone, Power.SCARLET_WITCH), "only Billy or Tommy");
                    RoadSave.get(server).progress(undone.getUUID()).setCompleted(0);
                    check(!RoadEnding.choose(undone, Power.BILLY) && genes(undone) == 1, "his progress does not say he has won every trial: nothing");
                    // The Gene is taken out of his pocket (another mod, a chest): the Road cannot answer.
                    for (int i = 0; i < 36; i++) {
                        if (empty.getInventory().getItem(i).is(ModItems.SCARLET_WITCH_GENE.get())) {
                            empty.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                    }
                    empty.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    check(!RoadEnding.choose(empty, Power.BILLY) && !Powers.power(empty).hasPowers(), "without a Gene there is no answer");
                    check(!RoadEnding.reopen(empty, true), "and the question is not asked");
                })
                .thenExecuteAfter(3, () -> {
                    check(!RoadDimension.in(undone) && !RoadTrials.isInside(undone), "the Road sends home who has not walked it all");
                    check(genes(undone) == 1 && !Powers.power(undone).hasPowers(), "with his Gene");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // No leaving early

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void noBookNoBifrostNoPearlTakesHimOutOfTheHallBeforeTheFarewellEnds(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 1);
        player.getInventory().setItem(4, new ItemStack(Items.ENDER_PEARL, 4));
        player.getInventory().setItem(5, new ItemStack(Items.CHORUS_FRUIT, 4));
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(VishantiBook.shutIn(RoadDimension.KEY), "the Book of the Vishanti is shut in the Road");
                    check(com.kolodkingal.enhanced.darkhold.DarkholdBook.inTheRoad(player), "so is the Darkhold");
                    ServerLevel road = RoadTestSupport.road(helper);
                    check(!HexManager.onTeleport(helper.getLevel(), player, helper.absoluteVec(new Vec3(10.5, 2.0, 10.5))),
                            "the Bifrost, the Tesseract and every leap of a hero ask the same question, and none leads out of the Road");
                    check(!HexManager.onTeleport(road, player, player.position().add(600.0, 0.0, 0.0)), "nor into another cell");
                    check(RoadEnding.choose(player, Power.BILLY), "he chooses");
                    check(!HexManager.onTeleport(helper.getLevel(), player, helper.absoluteVec(new Vec3(10.5, 2.0, 10.5))), "none leads out in the farewell either");
                    AbilityHandler.onKey(player, 1, true);
                    AbilityHandler.onKey(player, 2, true);
                    AbilityHandler.onKey(player, 3, true);
                    check(RoadDimension.in(player) && RoadTrials.isInside(player), "no key does");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "only the farewell takes him out"))
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The operators

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void theOperatorCommandsStillWorkAtTheEnd(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = finisher(f, server, 10.5, 10.5, 1);
        String name = player.getGameProfile().getName();
        ServerPlayer other = RoadTestSupport.walker(f, 16.5, 16.5);
        String otherName = other.getGameProfile().getName();
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    run(server, player, 2, "enhanced road status " + name);
                    run(server, player, 2, "enhanced road cells");
                    run(server, player, 2, "enhanced road layout " + name);
                    run(server, player, 2, "enhanced road skip " + name);
                    check(RoadTrials.trialOf(player.getUUID()) != null && RoadEnding.stage(player) == RoadEnding.Stage.ASKING
                            && RoadSave.get(server).progress(player.getUUID()).completed() == RoadChallenges.count(), "skip has nothing to win at the end");
                    run(server, player, 0, "enhanced road reset " + name);
                    check(RoadTrials.trialOf(player.getUUID()) != null, "a player cannot reset anyone");
                    // An operator enters another player at the end: the number after the last trial.
                    run(server, other, 2, "enhanced road set " + otherName + " 99");
                    run(server, other, 2, "enhanced road enter " + otherName + " " + (RoadChallenges.count() + 1));
                    check(RoadTrials.isInside(other), "/enhanced road enter <p> <last + 1> sends him to the end");
                })
                .thenWaitUntil(RoadTestSupport.active(other))
                .thenExecute(() -> {
                    check(RoadTrials.trialOf(other.getUUID()).challenge().isEnding(), "in the hall");
                    run(server, other, 2, "enhanced road set " + name + " 1");
                })
                .thenExecuteAfter(3, () -> {
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "counted back below the end, the Road sends him home");
                    check(genes(player) == 1 && !Powers.power(player).hasPowers(), "with his Gene and no power");
                    run(server, other, 2, "enhanced road reset " + otherName);
                    check(!RoadDimension.in(other) && !RoadTrials.isInside(other), "reset takes the other out of the hall");
                    RoadProgress progress = RoadSave.get(server).existing(other.getUUID());
                    check(progress == null || progress.completed() == 0, "and forgets his progress");
                    check(genes(other) == 1 && !Powers.power(other).hasPowers(), "nothing was spent");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The whole Road

    /** A Road of two small trials (its own batch: the registry is swapped): the gates lead to the hall, and the hall to the world. */
    @EnhancedTest(template = ARENA, batch = "road_ending_swap", timeoutTicks = 1200)
    public static void theWholeRoadEndsInTheHall(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        f.onClose(RoadChallenges.swap(List.of(new RoadTestSupport.ShortParkour("end_one"), new RoadTestSupport.ShortParkour("end_two"))));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        Vec3 home = player.position();
        check(RoadTrials.enter(player), "he walks the Road");
        RoadTrial[] trials = new RoadTrial[3];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    trials[0] = RoadTestSupport.trial(player);
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trials[0].layout().finish()));
                })
                .thenWaitUntil(() -> check(trials[0].phase() == RoadTrial.Phase.WON, "trial 1 won"))
                .thenExecute(() -> RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trials[0].layout().gate().zone())))
                .thenWaitUntil(() -> {
                    RoadTrial now = RoadTrials.trialOf(player.getUUID());
                    check(now != null && now != trials[0] && now.phase() == RoadTrial.Phase.ACTIVE, "trial 2 is ready");
                    player.hasChangedDimension();
                })
                .thenExecute(() -> {
                    trials[1] = RoadTestSupport.trial(player);
                    check(!trials[1].challenge().isEnding() && genes(player) == 1, "trial 2");
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trials[1].layout().finish()));
                })
                .thenWaitUntil(() -> check(trials[1].phase() == RoadTrial.Phase.WON, "trial 2 won"))
                .thenExecute(() -> {
                    check(RoadSave.get(server).progress(player.getUUID()).completed() == 2 && genes(player) == 1, "two won, the Gene is not spent");
                    TestKit.sentPackets(player);
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trials[1].layout().gate().zone()));
                })
                .thenWaitUntil(() -> {
                    RoadTrial now = RoadTrials.trialOf(player.getUUID());
                    check(now != null && now != trials[1] && now.phase() == RoadTrial.Phase.ACTIVE, "the hall is ready");
                    player.hasChangedDimension();
                })
                .thenExecute(() -> {
                    trials[2] = RoadTestSupport.trial(player);
                    check(trials[2].challenge().isEnding() && RoadDimension.in(player) && RoadTrials.isInside(player), "the last gate leads to the end of the Road");
                    check(trials[2].number() == RoadChallenges.count() && RoadSave.get(server).progress(player.getUUID()).completed() == RoadChallenges.count(), "(not a trial: it is counted after the last)");
                    check(trials[1].phase() == RoadTrial.Phase.CLEARING, "the last trial comes down");
                    check(genes(player) == 1 && !Powers.power(player).hasPowers(), "he still has no power");
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    List<String> lines = actionBar(TestKit.sentPackets(player));
                    check(lines.stream().anyMatch(k -> k.startsWith("message.enhanced.road.voice.ending.")), "the Road spoke at the end: " + lines);
                    check(RoadEnding.choose(player, Power.BILLY), "he chooses Billy");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "he is sent out"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    check(player.position().distanceTo(home) < 0.01 && Powers.is(player, Power.BILLY) && genes(player) == 0, "home, as Billy, the Gene used up");
                    check(player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3, "with his diamonds");
                    RoadSweeper.drain(server);
                    check(RoadSave.get(server).cellCount() == 0 || RoadSave.get(server).cells().stream().noneMatch(c -> c.owner().equals(player.getUUID())), "no cell of his is left");
                })
                .thenSucceed();
    }

    /** The structure trial puts his things aside (a Road of its own too, so a batch of its own); the way on, and the end, give every one of them back. */
    @EnhancedTest(template = ARENA, batch = "road_ending_stash", timeoutTicks = 1200)
    public static void hisThingsComeBackFromTheStructureTrialAndSurviveTheEnd(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        // The structure trial is the last trial of this Road.
        f.onClose(RoadChallenges.swap(List.of(RoadChallenges.byId(StructureCopyChallenge.ID))));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 2));
        kit(player);
        List<ItemStack> before = backpack(player);
        Vec3 home = player.position();
        check(RoadTrials.enter(player), "he walks the Road");
        RoadTrial[] trials = new RoadTrial[2];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    trials[0] = RoadTestSupport.trial(player);
                    check(StructureSupply.hasStash(player, StructureCopyChallenge.ID), "his things are put aside");
                    check(genes(player) == 2, "the Gene stays with him");
                    trials[0].succeed();
                })
                .thenExecuteAfter(2, () -> RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trials[0].layout().gate().zone())))
                .thenWaitUntil(() -> {
                    RoadTrial now = RoadTrials.trialOf(player.getUUID());
                    check(now != null && now != trials[0] && now.phase() == RoadTrial.Phase.ACTIVE && now.challenge().isEnding(), "the hall is ready: " + now
                            + " " + (now == null ? "" : now.challenge().id() + " " + now.phase()) + " first " + trials[0].phase() + " "
                            + RoadSave.get(server).progress(player.getUUID()).completed() + " " + RoadDimension.in(player));
                    player.hasChangedDimension();
                })
                .thenExecute(() -> {
                    check(!StructureSupply.hasStash(player, StructureCopyChallenge.ID), "nothing is put aside any more");
                    for (int i = 0; i < 36; i++) {
                        check(!StructureSupply.isSupply(player.getInventory().getItem(i)), "no block of the trial is left in slot " + i);
                        if (!before.get(i).is(ModItems.SCARLET_WITCH_GENE.get())) {
                            check(ItemStack.matches(before.get(i), player.getInventory().getItem(i)), "slot " + i + " is as it was in the hall");
                        }
                    }
                })
                .thenWaitUntil(asked(player))
                .thenExecute(() -> {
                    check(RoadEnding.choose(player, Power.TOMMY), "he chooses");
                })
                .thenWaitUntil(() -> check(!RoadDimension.in(player), "he is sent out"))
                .thenExecute(() -> {
                    player.hasChangedDimension();
                    check(player.position().distanceTo(home) < 0.01 && Powers.is(player, Power.TOMMY) && genes(player) == 1, "home, as Tommy, one Gene used");
                    for (int i = 0; i < 36; i++) {
                        check(!StructureSupply.isSupply(player.getInventory().getItem(i)), "no block of the trial in slot " + i);
                        if (!before.get(i).is(ModItems.SCARLET_WITCH_GENE.get())) {
                            check(ItemStack.matches(before.get(i), player.getInventory().getItem(i)), "slot " + i + " is as it was at home");
                        }
                    }
                    check(!StructureSupply.hasStash(player, StructureCopyChallenge.ID), "and nothing is put aside");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The words and the options

    private static JsonObject lang(String name) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(RoadEndingTests.class.getResourceAsStream("/assets/enhanced/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everythingTheEndingSaysIsSaidInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> keys = new ArrayList<>(List.of("road.enhanced.challenge." + EndingHall.ID, "message.enhanced.road.ending.title", "message.enhanced.road.ending.wait",
                "message.enhanced.road.ending.not_here", "message.enhanced.road.ending.no_gene", "message.enhanced.road.ending.undone", "message.enhanced.road.ending.done",
                "screen.enhanced.road.ending", "screen.enhanced.road.ending.gene", "screen.enhanced.road.later", "screen.enhanced.road.leave", "screen.enhanced.road.title",
                "screen.enhanced.road.billy", "screen.enhanced.road.billy.desc", "screen.enhanced.road.tommy", "screen.enhanced.road.tommy.desc",
                "message.enhanced.road.walked_all", "message.enhanced.road.walked_all.hint", "power.enhanced.billy.awaken", "power.enhanced.tommy.awaken"));
        for (String event : new String[]{RoadVoice.ENDING, RoadVoice.FAREWELL, RoadVoice.HOME}) {
            for (int variant = 0; variant < RoadVoice.VARIANTS; variant++) {
                keys.add(RoadVoice.key(event, variant));
            }
        }
        for (String key : keys) {
            check(en.has(key) && !en.get(key).getAsString().isBlank(), "English: " + key);
            check(he.has(key) && !he.get(key).getAsString().isBlank(), "Hebrew: " + key);
            check(count(en.get(key).getAsString()) == count(he.get(key).getAsString()), key + " takes the same arguments in both languages");
        }
        for (String event : new String[]{RoadVoice.ENDING, RoadVoice.FAREWELL, RoadVoice.HOME}) {
            check(!en.get(RoadVoice.key(event, 0)).getAsString().equals(en.get(RoadVoice.key(event, 1)).getAsString()), "three different lines for " + event);
        }
        Component title = Component.translatable("message.enhanced.road.ending.title");
        check(en.get("message.enhanced.road.ending.title").getAsString().length() <= 16 && he.get("message.enhanced.road.ending.title").getAsString().length() <= 16,
                "the title is short enough to be drawn whole (" + title.getString() + ")");
        helper.succeed();
    }

    private static int count(String text) {
        int n = 0;
        for (int i = text.indexOf("%s"); i >= 0; i = text.indexOf("%s", i + 2)) {
            n++;
        }
        return n;
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEndingsOptionHasItsDefaultAndTheScreenMessageTravelsIntact(GameTestHelper helper) {
        check(WitchesRoadConfig.ENDING_FAREWELL_SECONDS.get() == 5, "the farewell lasts 5 seconds");
        OpenRoadPayload open = new OpenRoadPayload(new BlockPos(3, 96, -7), false, 3, 3, true);
        io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        OpenRoadPayload.STREAM_CODEC.encode(buf, open);
        check(OpenRoadPayload.STREAM_CODEC.decode(buf).equals(open) && buf.readableBytes() == 0, "the screen's message at the end of the Road");
        check(RoadChallenges.ending().isEnding() && RoadChallenges.stage(RoadChallenges.count()) == RoadChallenges.ending() && RoadChallenges.stage(RoadChallenges.count() + 1) == null
                && RoadChallenges.all().stream().noneMatch(RoadChallenge::isEnding), "the hall comes after the last trial and is no trial");
        helper.succeed();
    }

    /** The door with the trials on, held by a player who is not at the end. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void theExitDoorOfATrialStillLeavesOnTheSecondClick(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    BlockPos door = RoadTestSupport.trial(player).layout().exitDoor();
                    WitchesRoad.use(RoadTestSupport.road(helper), door, player);
                    WitchesRoad.use(RoadTestSupport.road(helper), door, player);
                    check(!RoadDimension.in(player) && genes(player) == 1, "two clicks leave, with the Gene (the hall's door asks instead)");
                    check(!RoadEnding.reopen(player, false), "and /road choose has nothing to say outside the hall");
                })
                .thenSucceed();
    }

    private RoadEndingTests() {
    }
}
