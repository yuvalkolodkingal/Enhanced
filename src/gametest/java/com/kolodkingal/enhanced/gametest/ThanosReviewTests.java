package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosState;
import java.nio.file.NoSuchFileException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fighter;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fixture;

/**
 * What the in-game review of the Thanos lane found, each pinned by a test of its own: a Hex that must keep him in (or out)
 * even when the stuck rule would take him through its wall, a Titan who never lands, and the titles that were wider than
 * a screen. (The target that he held for sixty idle ticks instead of sixty ticks is in {@link ThanosFightTests}.)
 */
public final class ThanosReviewTests {

    private static boolean inside(Hex hex, ThanosEntity boss) {
        return hex.contains(boss.position().add(0.0, boss.getBbHeight() * 0.5, 0.0));
    }

    /**
     * A Thanos inside a Hex cannot leave it (the Hex is a perfect arena): the stuck rule, which frees him to a spot beside
     * the one he hunts, must ask the wall like any teleport of his, so a hunted fighter outside the Hex does not get him
     * out.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_review_hex_in", timeoutTicks = 40)
    public static void theStuckRuleDoesNotTakeHimOutOfAHex(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = fighter(f, 2, 2, Power.SCARLET_WITCH);
        ServerPlayer hunted = fighter(f, 9, 9);
        ThanosEntity boss = ThanosBossTests.boss(f, wanda);
        Hex hex = HexManager.create(wanda);
        check(inside(hex, boss), "he stands inside the Hex");
        check(!hex.contains(hunted.position().add(0.0, 0.9, 0.0)), "and the one he hunts stands outside it");
        boss.forceUnstick(hunted);
        check(inside(hex, boss), "the wall kept him in: he is at " + boss.position());
        helper.succeed();
    }

    /** And one outside a Hex is not put inside it by the same rule. */
    @EnhancedTest(template = ARENA, batch = "thanos_review_hex_out", timeoutTicks = 40)
    public static void theStuckRuleDoesNotTakeHimIntoAHex(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = fighter(f, 9, 9, Power.SCARLET_WITCH);
        ThanosEntity boss = ThanosBossTests.boss(f, wanda);
        Hex hex = HexManager.create(wanda);
        check(!inside(hex, boss), "he stands outside the Hex");
        Vec3 before = boss.position();
        boss.forceUnstick(wanda);
        check(!inside(hex, boss), "the wall kept him out: he is at " + boss.position());
        check(boss.position().distanceTo(before) < 0.5, "and he did not move at all");
        helper.succeed();
    }

    /** Where there is no wall the same rule still frees him beside the one he hunts. */
    @EnhancedTest(template = ARENA, batch = "thanos_review_unstick", timeoutTicks = 40)
    public static void withNoWallTheStuckRuleStillFreesHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer hunted = fighter(f, 8, 8);
        ThanosEntity boss = ThanosBossTests.boss(f, hunted);
        Vec3 before = boss.position();
        boss.forceUnstick(hunted);
        check(boss.position().distanceTo(before) > 4.0, "he moved");
        check(boss.position().distanceTo(hunted.position()) < 7.0, "to a spot beside the one he hunts: " + boss.position().distanceTo(hunted.position()));
        helper.succeed();
    }

    /**
     * A Thanos the portal never lets down (nothing holds him up here, but a pool, a cobweb or a block put in the column
     * during the countdown can) counts as landed after {@link ThanosEntity#LAND_TIMEOUT_TICKS}, so that his arrival
     * (invulnerable) does not last until the fight times out.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_review_land", timeoutTicks = 140)
    public static void aThanosWhoNeverLandsCountsAsLandedAfterTheDeadline(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer starter = fighter(f, 6, 6);
        Vec3 spot = f.helper().absoluteVec(ThanosBossTests.CENTRE);
        ThanosFight fight = ThanosFight.create(f.level(), spot, spot, 0, 5.0);
        com.kolodkingal.enhanced.boss.BossFights.begin(fight, starter);
        ThanosEntity boss = fight.boss();
        check(boss != null, "the fight made him");
        boss.setNoGravity(true);
        f.everyTick(130, t -> {
            if (t == 5) {
                check(!boss.hasLanded() && boss.state() == ThanosState.ARRIVING, "he hangs in the portal: " + boss.state());
                boss.forceFallTicks(ThanosEntity.LAND_TIMEOUT_TICKS - 1);
                return false;
            }
            if (t == 8) {
                check(boss.hasLanded(), "the deadline counts him as landed");
                boss.setNoGravity(false);
                return false;
            }
            if (t >= 8 + ThanosEntity.ARRIVE_TICKS + 5) {
                check(boss.state() != ThanosState.ARRIVING, "and his arrival ends: " + boss.state());
                return true;
            }
            return false;
        });
    }

    /** A Titan who falls into water (where he floats or sinks, but is not on the ground) has landed too. */
    @EnhancedTest(template = ARENA, batch = "thanos_review_water", timeoutTicks = 60)
    public static void aThanosWhoFallsIntoWaterHasLanded(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer starter = fighter(f, 6, 6);
        Vec3 spot = f.helper().absoluteVec(ThanosBossTests.CENTRE);
        ThanosFight fight = ThanosFight.create(f.level(), spot, spot, 0, 0.0);
        com.kolodkingal.enhanced.boss.BossFights.begin(fight, starter);
        ThanosEntity boss = fight.boss();
        check(boss != null, "the fight made him");
        boss.setNoGravity(true);
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(boss.position());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    f.level().setBlock(at.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
                }
            }
        }
        f.onClose(() -> {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 4; dy++) {
                        f.level().setBlock(at.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        });
        f.everyTick(30, t -> {
            if (t < 3) {
                return false;
            }
            check(boss.hasLanded(), "in the water he counts as landed");
            return true;
        });
    }

    private static JsonObject lang(String name) throws IOException {
        String path = "assets/" + Enhanced.MODID + "/lang/" + name + ".json";
        Path file = Platform.get().modResource(path).orElseThrow(() -> new NoSuchFileException(path));
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /**
     * A title is drawn four times the size of the text, a subtitle twice (and the game's scale of a 1080p window makes
     * the screen only 480 of them wide): the title of the reward was clipped at both ends, and so were the call's subtitle
     * and the awakening's. The longest text that fits every screen is a dozen or so characters of title and about thirty
     * of subtitle; this holds the lane's texts to that, in both languages.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_review_data")
    public static void theTitlesFitTheScreenAtEveryScale(GameTestHelper helper) throws IOException {
        for (String language : new String[]{"en_us", "he_il"}) {
            JsonObject lang = lang(language);
            for (String key : new String[]{"message.enhanced.thanos.summon.title", "message.enhanced.thanos.reward"}) {
                int length = lang.get(key).getAsString().length();
                check(length <= 14, language + " " + key + " is a title and has " + length + " characters");
            }
            for (String key : new String[]{"message.enhanced.thanos.summon.subtitle", "power.enhanced.thanos.awaken", "message.enhanced.thanos.stone.subtitle"}) {
                int length = lang.get(key).getAsString().length();
                check(length <= 32, language + " " + key + " is a subtitle and has " + length + " characters");
            }
        }
        helper.succeed();
    }

    private ThanosReviewTests() {
    }
}
