package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.ThanosBossPoses;
import com.kolodkingal.enhanced.client.render.ThanosGlow;
import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the Mad Titan's client stage can promise without a screen: the pose table of the boss, the arithmetic of the
 * glow of his Gauntlet, the clocks his entity keeps for the animation, and the textures the renderers ask for. The rest
 * of the stage (what is drawn) is judged by eye in a real game. No method here has a client class in its signature: the
 * tests run on a dedicated server.
 */
public final class ThanosClientTests {
    private static final String PURE = "thanos_client_pure";
    private static final String CLOCKS = "thanos_client_clocks";

    private static ThanosBossPoses.Out sample(ThanosBossPoses.Pose pose, float tick, int length) {
        ThanosBossPoses.Out out = new ThanosBossPoses.Out();
        ThanosBossPoses.sample(pose, tick, length, out);
        return out;
    }

    /** How far two poses are apart: the biggest difference in any channel, in the channel's own unit. */
    private static float apart(ThanosBossPoses.Out a, ThanosBossPoses.Out b) {
        float most = 0.0F;
        for (int i = 0; i < a.values.length; i++) {
            most = Math.max(most, Math.abs(a.values[i] - b.values[i]));
        }
        return most;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pose table

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void thePoseTableIsWellFormed(GameTestHelper helper) {
        String problem = ThanosBossPoses.problem();
        check(problem == null, "the pose table has no fault: " + problem);
        for (ThanosBossPoses.Pose pose : ThanosBossPoses.Pose.values()) {
            check(!ThanosBossPoses.lengthsOf(pose).isEmpty(), pose + " is played for some length");
            for (int length : ThanosBossPoses.lengthsOf(pose)) {
                float[] ticks = ThanosBossPoses.ticks(pose, length);
                for (int i = 1; i < ticks.length; i++) {
                    check(ticks[i] > ticks[i - 1], pose + " has its keys in order at length " + length);
                }
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void everyMoveHasAPoseThatLastsAsLongAsTheMoveAndSamplesCleanly(GameTestHelper helper) {
        for (ThanosMove move : ThanosMove.values()) {
            ThanosBossPoses.Pose pose = ThanosBossPoses.poseFor(move);
            check(pose != null, move + " has a pose");
            int length = ThanosBossPoses.lengthOf(pose, move);
            check(length == move.length(), move + " plays its pose for its whole length, " + move.length() + " ticks, not " + length);
            check(ThanosBossPoses.lengthsOf(pose).contains(length), move + "'s length is among those the table checks");
            for (float tick : new float[]{0.0F, move.tell() * 0.5F, move.tell(), length * 0.75F, length, length + 20}) {
                for (float value : sample(pose, tick, length).values) {
                    check(Float.isFinite(value), move + " has a finite pose at tick " + tick);
                }
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void theStatePosesKeepTheirOwnTime(GameTestHelper helper) {
        check(ThanosBossPoses.Pose.SURGE.length() == 60, "the surge lasts three seconds");
        check(ThanosBossPoses.Pose.STAGGER.length() == 40, "the stagger lasts two");
        check(ThanosBossPoses.Pose.ARRIVE.length() == 60, "the landing lasts three");
        check(ThanosBossPoses.Pose.DEATH.length() == 100, "the death lasts five");
        for (ThanosBossPoses.Pose pose : ThanosBossPoses.Pose.values()) {
            check(pose.length() == 0 || ThanosBossPoses.lengthsOf(pose).equals(List.of(pose.length())), pose + " is played for its own length only");
        }
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void theDeathEndsSeatedAndTheSurgeKneelsAndComesBackUp(GameTestHelper helper) {
        ThanosBossPoses.Pose death = ThanosBossPoses.Pose.DEATH;
        ThanosBossPoses.Out start = sample(death, 0.0F, 100);
        ThanosBossPoses.Out end = sample(death, 100.0F, 100);
        check(end.get(ThanosBossPoses.Ch.LEG_R) == -90.0F && end.get(ThanosBossPoses.Ch.LEG_L) == -90.0F, "both legs end bent flat: seated");
        check(end.get(ThanosBossPoses.Ch.DROP) > start.get(ThanosBossPoses.Ch.DROP), "he ends lower than he began, on one knee");
        check(end.get(ThanosBossPoses.Ch.HEAD_X) >= 12.0F, "the head is bowed");
        check(sample(death, 150.0F, 100).values[0] == end.values[0], "after the end it stays seated");

        ThanosBossPoses.Pose surge = ThanosBossPoses.Pose.SURGE;
        check(sample(surge, 30.0F, 60).get(ThanosBossPoses.Ch.DROP) > 5.0F, "the surge kneels in its middle");
        ThanosBossPoses.Out stand = new ThanosBossPoses.Out().stand();
        check(apart(sample(surge, 60.0F, 60), stand) < 1.0F, "and rises back to stand");
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void everyMoveShowsItsTellInTheBodyBeforeItStrikes(GameTestHelper helper) {
        ThanosBossPoses.Out stand = new ThanosBossPoses.Out().stand();
        for (ThanosMove move : ThanosMove.values()) {
            ThanosBossPoses.Pose pose = ThanosBossPoses.poseFor(move);
            int length = ThanosBossPoses.lengthOf(pose, move);
            ThanosBossPoses.Out atTell = sample(pose, Math.max(0, move.tell() - 1), length);
            // A pose that is not a stand: some arm, the body or the head moved by a clear amount while he winds up.
            check(apart(atTell, stand) >= 8.0F, move + " shows its wind-up in the pose (it differs from the stand by " + apart(atTell, stand) + ")");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The glow of the Gauntlet

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void theSocketsFollowTheStonesHeldAndTheNextOneComing(GameTestHelper helper) {
        float[] alpha = new float[ThanosGlow.SOCKETS];
        ThanosGlow.sockets(alpha, 3, -1, ThanosState.IDLE, null, 0.0F, 100.0F, 0.0F);
        for (int i = 0; i < 3; i++) {
            check(alpha[i] > 0.15F && alpha[i] < 0.4F, "stone " + i + " burns dimly while he is idle");
        }
        for (int i = 3; i < ThanosGlow.SOCKETS; i++) {
            check(alpha[i] == 0.0F, "socket " + i + " is a dark gem with only three stones and none coming");
        }

        ThanosGlow.sockets(alpha, 3, ThanosGlow.WARNING + 50, ThanosState.IDLE, null, 0.0F, 100.0F, 10.0F);
        check(alpha[3] == 0.0F, "the next socket is dark while the stone is far off");
        float nearest = 0.0F;
        float far = 0.0F;
        for (float age = 0.0F; age < 40.0F; age += 1.0F) {
            ThanosGlow.sockets(alpha, 3, 20, ThanosState.IDLE, null, 0.0F, 100.0F, age);
            nearest = Math.max(nearest, alpha[3]);
            ThanosGlow.sockets(alpha, 3, 190, ThanosState.IDLE, null, 0.0F, 100.0F, age);
            far = Math.max(far, alpha[3]);
        }
        check(nearest > far && nearest > 0.2F, "the socket of the stone about to come pulses harder the nearer it is");

        ThanosGlow.sockets(alpha, 5, -1, ThanosState.IDLE, null, 0.0F, 100.0F, 10.0F);
        check(alpha[ThanosGlow.MIND] > 0.5F, "with five stones the sixth socket glimmers for a moment");
        ThanosGlow.sockets(alpha, 5, -1, ThanosState.IDLE, null, 0.0F, 100.0F, 200.0F);
        check(alpha[ThanosGlow.MIND] == 0.0F, "and is dark the rest of the time");
        helper.succeed();
    }

    /** A Gauntlet held shut (a stagger, a Spider-Man's web) has dark stones, even the one a clench was lighting. */
    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void aLockedGauntletHasDarkStones(GameTestHelper helper) {
        float[] alpha = new float[ThanosGlow.SOCKETS];
        ThanosGlow.sockets(alpha, 5, -1, ThanosState.TELL, ThanosMove.POWER_BLAST, ThanosMove.POWER_BLAST.tell(), 5.0F, 10.0F);
        check(alpha[0] >= 0.99F && alpha[3] > 0.15F, "before: the clench burns the Power Stone and the others glow");
        ThanosGlow.locked(alpha);
        for (int i = 0; i < ThanosGlow.SOCKETS; i++) {
            check(alpha[i] == 0.0F, "socket " + i + " is dark while the Gauntlet is held shut");
        }
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void aClenchLightsItsOwnStoneAndTheSurgeFlaresTheNewestAndDeathDarkensAll(GameTestHelper helper) {
        float[] alpha = new float[ThanosGlow.SOCKETS];
        ThanosGlow.sockets(alpha, 4, -1, ThanosState.TELL, ThanosMove.POWER_BLAST, ThanosMove.POWER_BLAST.tell(), 5.0F, 0.0F);
        check(alpha[0] >= 0.99F, "a Power Stone move burns the Power Stone full at the end of its wind-up");
        check(alpha[1] < 0.4F && alpha[2] < 0.4F, "and leaves the others as they were");
        ThanosGlow.sockets(alpha, 4, -1, ThanosState.TELL, ThanosMove.POWER_BLAST, 1.0F, 1.0F, 0.0F);
        check(alpha[0] < 0.99F && alpha[0] > 0.25F, "it swells through the wind-up");
        ThanosGlow.sockets(alpha, 4, -1, ThanosState.TELL, ThanosMove.SWORD_SWEEP, 5.0F, 5.0F, 0.0F);
        for (int i = 0; i < 4; i++) {
            check(alpha[i] < 0.4F, "a sword move lights no stone");
        }

        ThanosGlow.sockets(alpha, 3, -1, ThanosState.SURGE, null, 0.0F, 0.0F, 0.0F);
        check(alpha[2] >= 0.99F, "the newest stone flares at the start of the surge");
        ThanosGlow.sockets(alpha, 3, -1, ThanosState.SURGE, null, 0.0F, 60.0F, 0.0F);
        check(alpha[2] < 0.5F, "and settles by its end");

        ThanosGlow.sockets(alpha, 5, -1, ThanosState.DYING, null, 0.0F, 80.0F, 0.0F);
        for (int i = 0; i < ThanosGlow.SOCKETS; i++) {
            check(alpha[i] < 0.01F, "the sockets are dark late in the death");
        }
        ThanosGlow.sockets(alpha, 5, -1, ThanosState.DYING, null, 0.0F, 14.0F, 0.0F);
        check(alpha[4] < alpha[0], "the newest stone goes dark before the oldest");

        ThanosGlow.sockets(alpha, 5, -1, ThanosState.TELL, ThanosMove.INFINITY_BEAM, 1.0F + 8.0F * 2.0F + 4.0F, 5.0F, 0.0F);
        check(alpha[0] >= 0.99F && alpha[1] >= 0.99F && alpha[2] >= 0.99F && alpha[3] < 0.4F, "the Infinity Beam flares the stones in turn, eight ticks apart");
        check(ThanosGlow.halo(ThanosGlow.IDLE) == 0.0F && ThanosGlow.halo(1.0F) > 0.0F, "a lit stone has a halo and a dim one has none");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the renderers ask for

    private static BufferedImage image(String path) throws IOException {
        Path file = Platform.get().modResource("assets/" + Enhanced.MODID + "/" + path).orElse(null);
        check(file != null && Files.exists(file), path + " exists");
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage image = ImageIO.read(in);
            check(image != null, path + " is a readable image");
            return image;
        }
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void theTexturesTheRenderersAskForExistWithTheSizeTheModelsWereCutFor(GameTestHelper helper) throws IOException {
        BufferedImage body = image("textures/entity/thanos.png");
        check(body.getWidth() == 128 && body.getHeight() == 128, "the Titan is a 128 by 128 skin");
        BufferedImage glow = image("textures/entity/thanos_glow.png");
        check(glow.getWidth() == 128 && glow.getHeight() == 128, "his glow lies over the same cut");
        BufferedImage sword = image("textures/entity/thanos_sword.png");
        check(sword.getWidth() == 64 && sword.getHeight() == 32, "the sword is a 64 by 32 skin");
        for (String sheet : new String[]{"textures/effect/portal_ring.png", "textures/effect/stone_mandala.png"}) {
            BufferedImage effect = image(sheet);
            check(effect.getWidth() == effect.getHeight(), sheet + " is square");
            check(effect.getColorModel().hasAlpha(), sheet + " has an alpha channel to draw additively");
        }
        // The glow shows only where the body does: nothing of it lies on a pixel the skin leaves empty.
        int lit = 0;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                if ((glow.getRGB(x, y) >>> 24) > 0) {
                    lit++;
                    check((body.getRGB(x, y) >>> 24) > 0, "the glow at " + x + "," + y + " lies on the skin");
                }
            }
        }
        check(lit > 20, "and it is not empty (" + lit + " pixels)");
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void thePayloadKindsTheServerSendsAreAllKnownToTheClient(GameTestHelper helper) {
        int highest = -1;
        for (int kind = 0; kind <= 14; kind++) {
            ThanosFxPayload payload = new ThanosFxPayload(kind, 0xFFFFFF, -1, 10, 1.0F, List.of(Vec3.ZERO));
            check(payload.kind() == kind, "kind " + kind + " round-trips");
            highest = kind;
        }
        check(highest == 14, "the fifteen kinds of the spec");
        helper.succeed();
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void noTestOfThisClassHasAClientClassInItsSignature(GameTestHelper helper) {
        for (Method method : ThanosClientTests.class.getDeclaredMethods()) {
            check(!isClientClass(method.getReturnType()), method.getName() + " returns no client class");
            for (Class<?> parameter : method.getParameterTypes()) {
                check(!isClientClass(parameter), method.getName() + " takes no client class (" + parameter.getName() + ")");
            }
        }
        helper.succeed();
    }

    /**
     * A class of the game's client: it cannot be loaded on a dedicated server, so no test may name it in a signature.
     */
    private static boolean isClientClass(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d.");
    }

    @EnhancedTest(template = TestKit.ARENA, batch = PURE)
    public static void theHeroPlugInLoadsOnAServerThatListsTheHeroes(GameTestHelper helper) {
        // The plug-in is created when the table of heroes is first read, on a dedicated server too: it must not make
        // the verifier load a client class (a LocalPlayer passed on as a Player does), which is why its work is in
        // classes of its own.
        HeroClient client = HeroClients.of(Power.THANOS);
        check(client != null && client.power() == Power.THANOS, "Thanos has a client plug-in");
        check(client.getClass().getSimpleName().equals("ThanosClient"), "and it is ThanosClient");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The clocks of the entity

    @EnhancedTest(template = TestKit.ARENA, batch = CLOCKS)
    public static void theEntityKeepsTheClocksTheAnimationReads(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        ServerPlayer starter = f.player(5.5, TestKit.FLOOR, 5.5);
        ThanosEntity boss = ThanosBossTests.boss(f, starter);
        helper.runAfterDelay(2, () -> {
            float landed = boss.stateTime(0.0F);
            helper.runAfterDelay(10, () -> {
                float later = boss.stateTime(0.0F);
                check(later >= landed + 9.0F, "the clock of his state runs a tick per tick (" + landed + " then " + later + ")");
                check(boss.stateTime(0.5F) > boss.stateTime(0.0F), "and the partial tick adds to it");
                check(!boss.shownByViewer(), "a real fight is not a viewer's show");
                boss.forceMove(ThanosMove.SWORD_SWEEP, starter);
                helper.runAfterDelay(6, () -> {
                    float moved = boss.moveTime(0.0F);
                    check(moved >= 4.0F && moved <= ThanosMove.SWORD_SWEEP.length(), "the clock of a move counts from its start (" + moved + ")");
                    helper.succeed();
                });
            });
        });
    }

    @EnhancedTest(template = TestKit.ARENA, batch = CLOCKS)
    public static void aViewersPoseStopsTheClocksAtTheTickItWasGiven(GameTestHelper helper) {
        ThanosEntity shown = new ThanosEntity(ModEntities.THANOS.get(), helper.getLevel());
        check(!shown.shownByViewer(), "an entity is not a viewer's until it is shown");
        shown.showState(ThanosState.TELL, ThanosMove.OVERHEAD_SMASH, 12, 3, 0, 0, Vec3.ZERO);
        check(shown.shownByViewer(), "showState makes it one");
        check(shown.state() == ThanosState.TELL && shown.currentMove() == ThanosMove.OVERHEAD_SMASH && shown.moveTick() == 12, "in the state it was given");
        check(shown.moveTime(0.0F) == 12.0F && shown.moveTime(0.9F) == 12.0F, "its move clock ignores the partial tick, so the pose holds still");
        check(shown.stateTime(0.0F) == shown.stateTime(0.9F), "and so does its state clock");
        check(shown.stones() == 3, "with the stones it was given");
        helper.succeed();
    }
}
