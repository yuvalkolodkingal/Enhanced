package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.gametest.UltronTestSupport.Log;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.LiftHook;
import com.kolodkingal.enhanced.ultron.LiftHooks;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronBodies;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronLane;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronSounds;
import com.kolodkingal.enhanced.ultron.UltronState;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.boss;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.fixture;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheIsland;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.run;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.sentryAt;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.start;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * The story of the fight: the awakening and its timeline, an upload that moves the boss, Eilon's finish rule, the lift
 * at fifty-five percent and the seam it leaves for the Sokovia stage, who counts and who is rewarded, every way the
 * fight ends, the two boss bars, and what a fighter drops when they fall.
 */
public final class UltronFightTests {

    // ------------------------------------------------------------------------------------------------------------
    // The awakening

    @EnhancedTest(template = ARENA, batch = "ultron_awakening", timeoutTicks = 260)
    public static void awakeningTimeline(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        Log log = new Log(player);
        UltronFight fight = start(f, player);
        AABB bounds = helper.getBounds();
        check(fight.phase() == UltronFight.Phase.AWAKENING && fight.boss() == null && fight.sentries().isEmpty(), "the fight begins in the awakening, with nobody there");
        check(!fight.bars().health().isVisible(), "and no health bar yet");
        Vec3 centre = fight.island().center();

        int[] appearedAt = {-1};
        int[] groundAt = {-1};
        f.everyTick(240, tick -> {
            log.drain();
            int p = fight.phaseTicks();
            UltronFight.Phase phase = fight.phase();
            if (phase == UltronFight.Phase.AWAKENING) {
                check(level.getEntitiesOfClass(UltronBoltEntity.class, bounds).isEmpty(), "nothing is shot in the awakening");
                if (p >= 1 && p < 49) {
                    check(fight.boss() == null && level.getEntitiesOfClass(UltronSentryEntity.class, bounds).isEmpty() && level.getEntitiesOfClass(UltronEntity.class, bounds).isEmpty(),
                            "before tick 50 there is only the scrap: no body, no Sentry (tick " + p + ")");
                    check(!fight.bars().health().isVisible(), "and no bar");
                }
                if (p == 4) {
                    check(!log.effects(UltronFxPayload.ASSEMBLE).isEmpty(), "the scrap spirals up (the assembling effect was sent)");
                    check(log.heard(UltronSounds.AWAKEN.event()), "with its sound");
                }
                if (p == 58 || p == 59) {
                    check(!log.said("speech.enhanced.ultron.awaken1"), "he has not spoken yet at " + p);
                }
                UltronEntity boss = fight.boss();
                if (boss != null && appearedAt[0] < 0) {
                    appearedAt[0] = p;
                    check(p >= 50 && p <= 52, "the body appears at tick 50, at " + p);
                    check(boss.isShielded() && boss.state() == UltronState.AWAKENING && !boss.brain().isEnabled(), "shielded, assembling, his mind off");
                    check(!boss.hurt(level.damageSources().generic(), 50.0F) && boss.getHealth() == boss.getMaxHealth(), "nothing hurts him");
                    assertNear(boss.getX(), centre.x, 0.1, "at the middle (x)");
                    assertNear(boss.getZ(), centre.z, 0.1, "(z)");
                    check(boss.getY() > centre.y + 2.4 && boss.getY() < centre.y + 3.6, "three blocks up: " + (boss.getY() - centre.y));
                    List<UltronSentryEntity> sentries = fight.sentries();
                    check(sentries.size() == 6, "with six Sentries: " + sentries.size());
                    for (UltronSentryEntity sentry : sentries) {
                        double ring = Math.hypot(sentry.getX() - centre.x, sentry.getZ() - centre.z);
                        check(Math.abs(ring - 6.0) < 0.6 && sentry.isAssembling() && sentry.eyes() == UltronSentryEntity.Eyes.BLUE, "each on the ring of six, assembling, its eyes dark");
                    }
                    check(fight.bars().health().isVisible() && fight.bars().second().isVisible(), "the bars appear with the body");
                    check(!log.effects(UltronFxPayload.FLASH).isEmpty(), "in a flash");
                }
                if (boss != null && p >= 62 && p <= 98) {
                    check(log.said("speech.enhanced.ultron.awaken1"), "at tick 60 he speaks");
                    check(boss.state() == UltronState.IDLE && boss.isShielded(), "his eyes are lit, he is still untouchable");
                    check(!log.said("speech.enhanced.ultron.awaken2"), "and has not said the second line");
                    for (UltronSentryEntity sentry : fight.sentries()) {
                        check(sentry.eyes() == UltronSentryEntity.Eyes.BLUE, "the Sentries' eyes are still dark at " + p);
                    }
                }
                if (boss != null && p >= 102 && p <= 118) {
                    check(log.said("speech.enhanced.ultron.awaken2"), "at tick 100 the second line");
                    for (UltronSentryEntity sentry : fight.sentries()) {
                        check(sentry.eyes() == UltronSentryEntity.Eyes.RED && sentry.getTarget() == null, "the Sentries' eyes are red and they still wait");
                    }
                }
                check(p <= 121, "the awakening lasts 120 ticks");
            } else if (phase == UltronFight.Phase.GROUND) {
                UltronEntity boss = boss(fight);
                if (groundAt[0] < 0) {
                    groundAt[0] = tick;
                    check(appearedAt[0] > 0, "the body had appeared");
                    check(!boss.isShielded() && boss.state() == UltronState.IDLE && boss.brain().isEnabled(), "the fight is on: he can be hurt, and he thinks");
                    check(fight.sentries().size() == 6, "six Sentries");
                }
                if (tick > groundAt[0] + 40) {
                    check(fight.sentries().stream().anyMatch(s -> s.getTarget() == player), "and the Sentries are let loose: they hunt");
                    return true;
                }
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // An upload

    @EnhancedTest(template = ARENA, batch = "solo_upload_moves_the_boss", timeoutTicks = 360)
    public static void uploadMovesTheBoss(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        Log log = new Log(player);
        UltronFight fight = onTheIsland(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        put(f, boss, 10.5, FLOOR + 3.0, 10.5);
        UltronSentryEntity near = sentryAt(f, fight, 15.5, 10.5);
        UltronSentryEntity far = sentryAt(f, fight, 3.5, 10.5);
        near.setHeld(true);
        far.setHeld(true);
        UltronSentryEntity assembling = UltronSentryEntity.army(level, f.helper().absoluteVec(new Vec3(11.5, FLOOR + 1.0, 12.5)), true);
        fight.spawn(assembling);
        fight.refresh();
        assembling.setHeld(true);
        Vec3 nearAt = near.position();
        Vec3 bodyAt = boss.position();

        boss.setHealth(1.0F);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().generic(), 5.0F);
        log.drain();
        check(boss.isAlive() && fight.isTransferring() && fight.uploads() == 1, "the blow that should have destroyed him began an upload");
        check(boss.isShielded() && boss.state() == UltronState.TRANSFER && !boss.brain().isEnabled(), "he is untouchable and still");
        check(!boss.hurt(level.damageSources().generic(), 5.0F), "nothing hurts him now");
        check(fight.onBodyDestroyed(boss, null, null) && fight.uploads() == 1, "a second lethal blow does not start a second upload");
        List<UltronFxPayload> lines = log.effects(UltronFxPayload.DATA_LINE);
        check(lines.size() == 1 && lines.get(0).to().distanceTo(nearAt.add(0, near.getBbHeight() * 0.6, 0)) < 0.2,
                "the data runs to the nearest Sentry that is whole, not the one still assembling");

        int[] movedAt = {-1};
        boolean[] doneFirst = {false};
        f.everyTick(340, tick -> {
            log.drain();
            if (!doneFirst[0]) {
                if (tick == 20) {
                    check(barKey(fight) .equals("boss.enhanced.ultron.upload") && fight.bars().health().getColor() == BossEvent.BossBarColor.WHITE, "the bar says 'uploading', in white");
                }
                if (tick < 38) {
                    check(boss.position().distanceTo(bodyAt) < 0.05 && boss.bodyIndex() == 0 && near.isAlive(), "for forty ticks nothing changes (tick " + tick + ")");
                }
                if (tick >= 41 && movedAt[0] < 0) {
                    movedAt[0] = tick;
                    check(boss.bodyIndex() == 1, "he is in the second body");
                    assertNear(boss.getMaxHealth(), 480.0, 0.01, "with 480 health");
                    assertNear(boss.getHealth(), 480.0, 0.01, "all of it");
                    check(boss.position().distanceTo(nearAt) < 0.6, "where the Sentry stood: " + boss.position());
                    check(near.isRemoved() && far.isAlive() && assembling.isAlive(), "that Sentry became him; the others are as they were");
                    check(boss.isShielded() && boss.state() == UltronState.TRANSFER, "he is still untouchable for a while");
                    check(boss.getBbHeight() < 3.4F - 0.2F, "and smaller");
                }
                if (tick > 41 && tick < 78) {
                    check(boss.isShielded() && fight.isTransferring(), "the upload takes eighty ticks (tick " + tick + ")");
                }
                if (tick >= 83) {
                    check(!fight.isTransferring() && !boss.isShielded() && boss.state() == UltronState.IDLE && boss.brain().isEnabled(), "after eighty ticks he fights on");
                    check(log.said("speech.enhanced.ultron.transfer1"), "with a line");
                    check(barKey(fight).equals("boss.enhanced.ultron.body") && Integer.valueOf(2).equals(argOf(fight.bars().health().getName())), "the bar names his second body");
                    // The second upload: into the Sentry that was assembling.
                    boss.setHealth(1.0F);
                    boss.invulnerableTime = 0;
                    boss.hurt(level.damageSources().generic(), 5.0F);
                    check(fight.isTransferring() && fight.uploads() == 2, "a second upload");
                    doneFirst[0] = true;
                    movedAt[0] = tick;
                }
                return false;
            }
            if (tick >= movedAt[0] + 42) {
                check(boss.bodyIndex() == 2, "the third body");
                assertNear(boss.getMaxHealth(), 384.0, 0.01, "384 health");
                check(assembling.isRemoved() && far.isAlive(), "into the nearest: the one that had finished assembling");
                return tick >= movedAt[0] + 85 ? finish(fight) : false;
            }
            return false;
        });
    }

    private static boolean finish(UltronFight fight) {
        check(!fight.isTransferring(), "and the second upload ended too");
        return true;
    }

    private static String barKey(UltronFight fight) {
        return fight.bars().health().getName().getContents() instanceof TranslatableContents t ? t.getKey() : "?";
    }

    private static Object argOf(Component name) {
        return name.getContents() instanceof TranslatableContents t && t.getArgs().length > 0 ? t.getArgs()[0] : null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Eilon's finish rule

    /**
     * What happens when a body is destroyed, in every case: before the island is up nothing; after it, the nearest
     * Sentry that is whole takes him in; with none and some budget left the rest of the Sentries come and he jumps; with
     * none and no budget, or after {@code maxUploads}, he dies; and a Vision near him cuts the network. Each case is
     * its own fight, played to the instant of the blow; all of the cases are tried and all that fail are reported.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_finish")
    public static void theFinishRule(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        Log log = new Log(player);
        DamageSource blow = level.damageSources().generic();
        List<String> failed = new ArrayList<>();

        attempt(failed, "1. before the island is up", () -> {
            BossFights.clear();
            UltronFight fight = onTheGround(f, player);
            calm(fight);
            UltronEntity boss = boss(fight);
            sentryAt(f, fight, 14.5, 10.5).setHeld(true);
            boss.setHealth(1.0F);
            boss.invulnerableTime = 0;
            boss.hurt(blow, 1000.0F);
            check(boss.isAlive() && !fight.isTransferring() && !fight.isDying() && fight.uploads() == 0, "on the ground a lethal blow does nothing");
            check(boss.getHealth() >= boss.getMaxHealth() * 0.1F - 0.01F, "his health stays on its floor of a tenth: " + boss.getHealth());
        });

        attempt(failed, "2. with Sentries", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            put(f, boss, 10.5, FLOOR + 3.0, 10.5);
            UltronSentryEntity near = held(sentryAt(f, fight, 14.5, 10.5));
            UltronSentryEntity far = held(sentryAt(f, fight, 4.5, 10.5));
            UltronSentryEntity closest = UltronSentryEntity.army(level, f.helper().absoluteVec(new Vec3(11.5, FLOOR + 1.0, 11.5)), true);
            fight.spawn(closest);
            closest.setHeld(true);
            fight.refresh();
            log.drain();
            int lines = log.effects(UltronFxPayload.DATA_LINE).size();
            lethal(fight, boss, blow);
            log.drain();
            check(fight.isTransferring() && fight.uploads() == 1 && !fight.isDying(), "with Sentries he uploads");
            List<UltronFxPayload> sent = log.effects(UltronFxPayload.DATA_LINE);
            check(sent.size() == lines + 1, "one data line was drawn");
            UltronFxPayload line = sent.get(sent.size() - 1);
            check(line.to().distanceTo(near.position().add(0, near.getBbHeight() * 0.6, 0)) < 0.2, "into the nearest whole Sentry, not the one assembling beside him");
            check(near.isAlive() && far.isAlive() && fight.sentries().size() == 3, "(it is taken at the end of the upload)");
        });

        attempt(failed, "3. no Sentries, budget left", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            check(fight.sentries().isEmpty() && fight.budgetLeft() == 20, "no Sentries, a budget of 20");
            lethal(fight, boss, blow);
            check(fight.isTransferring() && fight.uploads() == 1 && fight.sentries().size() == 6 && fight.budgetSpent() == 6, "six Sentries come at once and he jumps into one");
            for (UltronSentryEntity sentry : fight.sentries()) {
                check(sentry.distanceTo(boss) < 3.5 && !sentry.isAssembling(), "round him, whole");
            }
        });

        attempt(failed, "3b. four left in the budget", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            fight.spendBudget(16);
            lethal(fight, boss, blow);
            check(fight.isTransferring() && fight.sentries().size() == 4 && fight.budgetLeft() == 0, "with four left in the budget, four come");
        });

        attempt(failed, "3c. a wave on its way", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            check(fight.callWave(true) && fight.pendingSentries() == 6 && fight.budgetLeft() == 14, "a wave of six is announced");
            lethal(fight, boss, blow);
            check(fight.isTransferring() && fight.sentries().size() == 6 && fight.budgetSpent() == 6 && fight.pendingSentries() == 0,
                    "the wave comes at once, round him, and he jumps into it; it does not rise a second time later");
        });

        attempt(failed, "3d. only a wave on its way is left", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            fight.spendBudget(16);
            check(fight.callWave(true) && fight.pendingSentries() == 4 && fight.budgetLeft() == 0, "the last four are on their way");
            lethal(fight, boss, blow);
            check(fight.isTransferring() && !fight.isDying() && fight.sentries().size() == 4, "they are still to come, so he is not destroyed: they come and he uploads");
            check(fight.budgetSpent() == 20 && fight.pendingSentries() == 0, "and the budget is spent once: " + fight.budgetSpent());
        });

        attempt(failed, "4. no Sentries, no budget", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            fight.spendBudget(20);
            lethal(fight, boss, blow);
            check(fight.isDying() && !fight.isTransferring() && fight.uploads() == 0 && boss.state() == UltronState.DEATH && boss.isShielded(),
                    "with nobody to jump into he dies for good: the long death begins");
        });

        attempt(failed, "5. the limit of uploads", () -> {
            f.config(UltronBossConfig.MAX_UPLOADS, 0);
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            held(sentryAt(f, fight, 14.5, 10.5));
            lethal(fight, boss, blow);
            check(fight.isDying() && !fight.isTransferring(), "at the limit of uploads he is destroyed, with Sentries alive");
            f.config(UltronBossConfig.MAX_UPLOADS, 12);
        });

        ServerPlayer vision = f.player(5.5, FLOOR, 3.5, Power.VISION);
        attempt(failed, "6a. a Vision near, the Sentry far, budget left", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            put(f, boss, 3.5, FLOOR + 3.0, 3.5);
            UltronSentryEntity distant = held(sentryAt(f, fight, 18.5, 18.5));
            check(distant.distanceTo(boss) > 12.0, "(the Sentry is out of reach: " + distant.distanceTo(boss) + ")");
            check(fight.visionNear(boss.position()), "a Vision is near");
            lethal(fight, boss, blow);
            check(fight.isTransferring() && fight.sentries().size() == 7 && fight.budgetSpent() == 6,
                    "the far Sentry is no use: the budget's Sentries come round him and he jumps into one of them");
            check(distant.isAlive(), "the far one is left alone");
        });

        attempt(failed, "6b. a Vision near, the Sentry far, no budget", () -> {
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            put(f, boss, 3.5, FLOOR + 3.0, 3.5);
            fight.spendBudget(20);
            held(sentryAt(f, fight, 18.5, 18.5));
            lethal(fight, boss, blow);
            check(fight.isDying() && !fight.isTransferring(), "with none in reach and no budget, the Vision's cut is his end");
        });

        attempt(failed, "6c. the Vision out of range", () -> {
            put(f, vision, 18.5, FLOOR, 18.5);
            f.config(UltronConfig.VISION_CUT_RANGE, 5);
            UltronFight fight = island(f, player);
            UltronEntity boss = boss(fight);
            put(f, boss, 3.5, FLOOR + 3.0, 3.5);
            fight.spendBudget(20);
            UltronSentryEntity free = held(sentryAt(f, fight, 18.5, 18.5));
            check(!fight.visionNear(boss.position()), "(the Vision is out of range)");
            lethal(fight, boss, blow);
            check(fight.isTransferring() && !fight.isDying() && free.isAlive(), "with the Vision away, a Sentry anywhere will do");
        });

        check(failed.isEmpty(), failed.size() + " of the cases failed: " + failed);
        helper.succeed();
    }

    /** Runs one case of a test, writing down its failure instead of ending the test. */
    private static void attempt(List<String> failed, String name, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException e) {
            failed.add(name + " (" + e.getMessage() + ")");
        }
    }

    /** A fresh fight on the island, quiet. */
    private static UltronFight island(TestKit.Fixture f, ServerPlayer starter) {
        BossFights.clear();
        UltronFight fight = onTheIsland(f, starter);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return fight;
    }

    private static UltronSentryEntity held(UltronSentryEntity sentry) {
        sentry.setHeld(true);
        return sentry;
    }

    /** A blow that would destroy the body, through the real pipeline. */
    private static void lethal(UltronFight fight, UltronEntity boss, DamageSource source) {
        fight.refresh();
        boss.setHealth(1.0F);
        boss.invulnerableTime = 0;
        boss.hurt(source, 5.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The lift

    @EnhancedTest(template = ARENA, batch = "ultron_liftstart", timeoutTicks = 520)
    public static void liftStartsAtFiftyFivePercent(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        ServerPlayer second = f.player(14.5, FLOOR, 10.5);
        tank(second);
        Log log = new Log(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        check(fight.counted() == 1 && boss.getMaxHealth() == 600.0F, "one fighter when he rose: 600 health");
        Vec3 centre = fight.island().center();

        int[] warnAt = {-1};
        int[] islandAt = {-1};
        f.everyTick(500, tick -> {
            log.drain();
            UltronFight.Phase phase = fight.phase();
            if (tick == 3) {
                check(fight.participants().size() == 2, "the second fighter has joined");
                boss.setHealth(boss.getMaxHealth() * 0.551F);
            }
            if (tick == 4) {
                // A Volley is on its way when he drops under the line: he finishes it first.
                check(boss.brain().force(boss, fight, UltronAttack.VOLLEY), "a Volley starts");
            }
            if (tick == 6) {
                boss.setHealth(boss.getMaxHealth() * 0.55F);
            }
            if (tick >= 4 && tick < 30) {
                check(phase == UltronFight.Phase.GROUND, "he finishes what he is doing before the lift (tick " + tick + ")");
            }
            if (phase == UltronFight.Phase.LIFT_WARN && warnAt[0] < 0) {
                warnAt[0] = tick;
                check(tick > 30 && boss.brain().isIdle(), "the lift began when he was idle, at tick " + tick);
                check(boss.isShielded() && boss.state() == UltronState.LIFT_CHANNEL && !boss.brain().isEnabled(), "he channels, shielded");
                check(fight.counted() == 2 && fight.budget() == 28, "the second fighter counts: " + fight.counted() + ", budget " + fight.budget());
                assertNear(boss.getMaxHealth(), 810.0, 0.01, "his health is for two");
                assertNear(boss.getHealth() / boss.getMaxHealth(), 0.55, 0.01, "at the same fraction");
                check(!fight.isLifting() && fight.isTremor(), "with the default lift the island does not rise: the Tremor");
                check(barKey(fight).equals("boss.enhanced.ultron.lift") && fight.bars().health().getColor() == BossEvent.BossBarColor.YELLOW, "the bar counts the lift, in yellow");
            }
            if (warnAt[0] > 0) {
                int p = fight.phaseTicks();
                if (phase == UltronFight.Phase.LIFT_WARN) {
                    if (p == 3) {
                        check(log.toldOnActionBar("message.enhanced.ultron.lift.warn"), "the countdown is on the action bar");
                        check(log.said("message.enhanced.ultron.tremor"), "the players are told the ground shakes and nothing rises");
                        check(!log.effects(UltronFxPayload.LIFT_RING).isEmpty(), "the ring of light is drawn");
                        check(log.said("speech.enhanced.ultron.lift_warn"), "and he says what he is about to do");
                    }
                    if (p == 98) {
                        check(fight.key() == null, "the key has not risen yet");
                    }
                    if (p == 104) {
                        check(log.actionBarCount("message.enhanced.ultron.lift.warn") == 2, "the countdown was told twice: at the start and halfway");
                        Object[] seconds = log.lastActionBarArgs("message.enhanced.ultron.lift.warn");
                        check(seconds != null && Integer.valueOf(5).equals(seconds[0]), "the second time it says five seconds");
                        VibraniumKeyEntity key = fight.key();
                        check(key != null && key.isAlive(), "at tick 100 the Vibranium Key rises");
                        check(!fight.isOver() && fight.phase() == UltronFight.Phase.LIFT_WARN, "the warning goes on");
                    }
                    check(boss.isShielded() && !boss.brain().isEnabled(), "he is shielded for all of it");
                }
                if (phase == UltronFight.Phase.LIFT_LOCK) {
                    check(p <= 21, "the lock takes 20 ticks");
                    check(Math.hypot(boss.getX() - centre.x, boss.getZ() - centre.z) < 1.0, "he has floated to the middle: " + boss.position());
                }
                if (phase == UltronFight.Phase.TREMOR) {
                    check(boss.isShielded(), "still shielded in the Tremor");
                }
                if (phase == UltronFight.Phase.ISLAND && islandAt[0] < 0) {
                    islandAt[0] = tick;
                    check(tick - warnAt[0] >= 200 + 20 + 40 - 1 && tick - warnAt[0] <= 200 + 20 + 40 + 3, "the island phase began " + (tick - warnAt[0]) + " ticks after the warning");
                    check(fight.canFall() && !boss.isShielded() && boss.state() == UltronState.IDLE && boss.brain().isEnabled(), "he is freed, and can fall now");
                    check(log.said("speech.enhanced.ultron.island"), "he says his line");
                    check(fight.key() != null, "the key stays");
                    return true;
                }
            }
            return false;
        });
    }

    /** What the Sokovia stage will plug into: every call, in order, with its arguments, and the config that turns it off. */
    @EnhancedTest(template = ARENA, batch = "ultron_hook", timeoutTicks = 700)
    public static void theLiftHookIsCalledInOrder(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronBossConfig.LIFT_TICKS, 40);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        Recorder lift = new Recorder();
        LiftHook old = LiftHooks.install(lift);
        f.onClose(() -> LiftHooks.install(old));

        // The ritual asks the hook, and a refusal is shown and costs nothing.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        TestKit.look(player, 0.0F, 90.0F);
        UltronLane lane = (UltronLane) BossFights.lane(BossKind.ULTRON);
        check(lane.refusal(player, false) == null && lift.calls.equals(List.of("refusal")), "the hook is asked at the ritual: " + lift.calls);
        lift.calls.clear();
        lift.refusal = Component.translatable("message.enhanced.ultron.refuse.sky");
        Component said = lane.refusal(player, false);
        check(said == lift.refusal, "and its refusal is the ritual's");
        check(lane.refusal(player, true) == null, "an operator's start does not ask");
        lift.refusal = null;
        lift.calls.clear();

        // The config can turn the lift off before the hook is asked; the hook can decline.
        for (int variant = 0; variant < 3; variant++) {
            BossFights.clear();
            if (variant == 0) {
                f.config(UltronBossConfig.LIFT_ENABLED, false);
            } else if (variant == 1) {
                f.config(UltronBossConfig.LIFT_ENABLED, true);
                f.config(UltronBossConfig.LIFT_MODE, UltronBossConfig.LiftMode.TREMOR);
            } else {
                f.config(UltronBossConfig.LIFT_MODE, UltronBossConfig.LiftMode.SKY_ISLAND);
                lift.rises = false;
            }
            UltronFight fight = onTheGround(f, player);
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            boolean asked = lift.calls.contains("warn");
            check(variant == 2 ? asked : !asked, "variant " + variant + ": the hook " + (asked ? "was" : "was not") + " asked");
            check(!fight.isLifting() && fight.isTremor(), "variant " + variant + ": the Tremor");
            lift.calls.clear();
        }
        lift.rises = true;
        BossFights.clear();
        lift.calls.clear(); // (letting the last fight go told the hook to finish)

        // The full run: warn, lock, rise until it says so, the island phase, then the descent after the victory.
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        fight.forceState(UltronFight.Phase.LIFT_WARN);
        check(lift.calls.equals(List.of("warn")) && fight.isLifting() && !fight.isTremor(), "the hook was asked once and the island will rise: " + lift.calls);
        check(!fight.hexExempt(), "(the Hex lets nobody through yet)");
        int[] lockedAt = {-1};
        int[] victoryAt = {-1};
        f.everyTick(680, tick -> {
            UltronFight.Phase phase = fight.phase();
            if (phase == UltronFight.Phase.LIFT_LOCK && lockedAt[0] < 0) {
                lockedAt[0] = tick;
                check(lift.calls.equals(List.of("warn", "lock")), "lock was called once the warning was over: " + lift.calls);
                check(fight.hexExempt(), "and the Hex lets the fight's members through");
            }
            if (phase == UltronFight.Phase.RISING) {
                check(fight.hexExempt(), "during the rise too");
            }
            if (phase == UltronFight.Phase.ISLAND && victoryAt[0] < 0) {
                check(lift.riseTicks.size() >= 29 && lift.riseTicks.get(0) == 1 && lift.riseTicks.get(lift.riseTicks.size() - 1) >= 30,
                        "rise was called every tick until it answered true: " + lift.riseTicks.size() + " calls, last " + lift.riseTicks.get(lift.riseTicks.size() - 1));
                check(fight.canFall() && fight.isLifting() && !fight.isTremor() && fight.hexExempt(), "the island is up, everyone is still carried");
                check(fight.key() != null, "the key is there");
                victoryAt[0] = tick;
                fight.end(BossEnd.VICTORY);
                check(fight.key().poweredDownAt() >= 0, "the key powers down at the end");
            }
            if (victoryAt[0] > 0) {
                int n = tick - victoryAt[0];
                if (n < 98) {
                    check(!lift.calls.contains("beginDescent") && fight.boss() != null, "the pause lasts 100 ticks, with the body still there (" + n + ")");
                }
                if (n > 103) {
                    check(lift.calls.indexOf("beginDescent") == lift.calls.lastIndexOf("beginDescent") && lift.calls.contains("beginDescent"),
                            "beginDescent was called once, after the pause: " + lift.calls);
                    check(fight.boss() == null, "with the body gone");
                }
                if (fight.isFinished()) {
                    check(lift.descents >= 10 && lift.calls.get(lift.calls.size() - 1).equals("finish:false"), "descend ran until it answered true, then finish(false): " + lift.calls);
                    check(!fight.hexExempt(), "the Hex is the Hex again");
                    check(fight.island().islandOffset() == 0.0, "and the island is on the ground");
                    return true;
                }
            }
            return false;
        });
    }

    /** A lift that writes down what it was asked. */
    private static final class Recorder implements LiftHook {
        final List<String> calls = new ArrayList<>();
        final List<Integer> riseTicks = new ArrayList<>();
        Component refusal;
        boolean rises = true;
        int descents;

        @Override
        public Component refusal(ServerLevel level, BlockPos centre, int radius) {
            calls.add("refusal");
            return refusal;
        }

        @Override
        public boolean warn(UltronFight fight) {
            calls.add("warn");
            return rises;
        }

        @Override
        public void lock(UltronFight fight) {
            calls.add("lock");
        }

        @Override
        public boolean rise(UltronFight fight, int tick) {
            riseTicks.add(tick);
            return tick >= 30;
        }

        @Override
        public void beginDescent(UltronFight fight) {
            calls.add("beginDescent");
        }

        @Override
        public boolean descend(UltronFight fight, int tick) {
            descents++;
            return tick >= 10;
        }

        @Override
        public void finish(UltronFight fight, boolean forced) {
            calls.add("finish:" + forced);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who counts, who is rewarded

    @EnhancedTest(template = ARENA, batch = "ultron_rewards", timeoutTicks = 120)
    public static void participantsAndRewards(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer a = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ServerPlayer b = f.player(12.5, FLOOR, 10.5);
        ServerPlayer c = f.player(8.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer d = f.player(10.5, FLOOR, 13.5);
        tank(a);
        UltronFight fight = start(f, a);
        f.helper().runAfterDelay(3, () -> {
            check(fight.participants().size() == 4, "the four who were near joined: " + fight.participants().size());
            fight.forceState(UltronFight.Phase.GROUND);
            calm(fight);
            UltronEntity boss = boss(fight);
            check(fight.counted() == 4, "four count");
            assertNear(boss.designHealth(), 600.0 * (1.0 + 0.35 * 3), 0.01, "his health is for four: 1230");
            assertNear(boss.getMaxHealth(), 1024.0, 0.01, "of which a creature can hold 1024 (the blows are made smaller for the rest)");
            check(fight.budget() == 20 + 8 * 3 && fight.maxAlive() == 8 + 3 * 3, "and so are the budget (44) and the Sentries alive at once (17)");
            for (int count = 1; count <= 8; count++) {
                check(UltronBodies.counted(count) == Math.min(count, 6), "at most six count");
            }

            // a does damage, b only stands there, c (Creative) does damage, d strikes the last blow without any.
            boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
            boss.invulnerableTime = 0;
            boss.hurt(level.damageSources().playerAttack(a), 10.0F);
            assertNear(fight.participant(a.getUUID()).damage(), 10.0 * 1024.0 / 1230.0, 0.01, "a's blow went in the ledger, as the health it took off a body that holds 1024 of 1230");
            fight.credit(c, BossFight.DAMAGE, 50.0F);
            fight.lastBlow(d);
            fight.end(BossEnd.VICTORY);
            check(cores(a) == 1, "a, who did damage, has a Core");
            check(cores(d) == 1, "d, who struck the last blow, has a Core");
            check(cores(b) == 0, "b, who only stood there, has none");
            check(cores(c) == 0, "c, in Creative, has none");
            helper.succeed();
        });
    }

    private static int cores(ServerPlayer player) {
        return player.getInventory().countItem(ModItems.ULTRON_CORE.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // How it ends

    @EnhancedTest(template = ARENA, batch = "ultron_victory", timeoutTicks = 360)
    public static void theLastBodyFallsAndTheFightIsWon(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        Log log = new Log(player);
        UltronFight fight = onTheIsland(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        fight.spendBudget(fight.budget());
        boss.setHealth(1.0F);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().playerAttack(player), 20.0F);
        check(fight.isDying() && boss.state() == UltronState.DEATH && boss.isShielded(), "the last body begins its long death");
        check(!fight.isOver(), "the fight is not over yet");
        int[] overAt = {-1};
        f.everyTick(340, tick -> {
            log.drain();
            if (!fight.isOver()) {
                check(tick < 80, "the death took too long");
                if (tick == 30) {
                    assertNear(fight.bars().health().getProgress(), 0.5, 0.1, "the bar empties as the plates fall: " + fight.bars().health().getProgress());
                }
                return false;
            }
            if (overAt[0] < 0) {
                overAt[0] = tick;
                check(tick >= 58 && tick <= 64, "the death takes sixty ticks: " + tick);
                check(fight.result() == BossEnd.VICTORY, "the result is a victory");
                check(cores(player) == 1, "the one who struck the last blow has the Core");
                check(fight.participant(player.getUUID()).struckLastBlow(), "and the last blow is in the ledger");
                check(log.said("message.enhanced.ultron.victory"), "the players are told");
                check(log.said("speech.enhanced.ultron.final"), "he says his last words");
            }
            int n = tick - overAt[0];
            if (n < 95) {
                check(!fight.isFinished(), "after the victory there is a pause (" + n + ")");
            }
            if (fight.isFinished()) {
                check(n >= 98, "the pause was 100 ticks: " + n);
                check(fight.boss() == null && BossFights.fightOf(boss) == null, "the body is gone and belongs to nothing");
                AABB bounds = helper.getBounds();
                check(level.getEntitiesOfClass(UltronEntity.class, bounds).isEmpty() && level.getEntitiesOfClass(UltronSentryEntity.class, bounds).isEmpty()
                        && level.getEntitiesOfClass(UltronBoltEntity.class, bounds).isEmpty(), "and nothing of the fight is left");
                check(cores(player) == 1, "still one Core");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "ultron_wipe", timeoutTicks = 300)
    public static void aWipeSendsHimAwayWithoutAReward(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 1);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, player);
        UltronEntity boss = boss(fight);
        check(fight.sentries().size() == 6, "six Sentries");
        player.kill();
        check(!player.isAlive(), "the only fighter is dead");
        int[] overAt = {-1};
        f.everyTick(280, tick -> {
            if (!fight.isOver()) {
                check(tick < 60, "the wipe never came");
                return false;
            }
            if (overAt[0] < 0) {
                overAt[0] = tick;
                check(fight.result() == BossEnd.WIPE, "a wipe");
                check(boss.isShielded() && boss.state() == UltronState.TRANSFER && !boss.brain().isEnabled(), "he uploads himself away");
                check(level.getEntitiesOfClass(UltronSentryEntity.class, helper.getBounds()).isEmpty(), "and his army with him");
                check(cores(player) == 0, "nobody is rewarded");
            }
            if (tick - overAt[0] < 55) {
                check(!fight.isFinished(), "he takes a while to leave");
            }
            if (fight.isFinished()) {
                check(tick - overAt[0] >= 58, "after 60 ticks: " + (tick - overAt[0]));
                check(level.getEntitiesOfClass(UltronEntity.class, helper.getBounds()).isEmpty(), "the body is gone");
                return true;
            }
            return false;
        });
    }

    /**
     * While the island is up his army stays on it. Found in the game: Sentries that drifted past the rim, and his body
     * after a fighter down on the ground lured them, sank to the real ground and could never climb back (they hover at a
     * height over the ground beneath them), and the fight on the island went on without them. Now a fighter who is not
     * on the island is not hunted, and a body or Sentry that ends up under the island is brought back onto it.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_tether", timeoutTicks = 120)
    public static void theArmyStaysOnTheIsland(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer below = f.player(MIDDLE.x + 3.0, FLOOR, MIDDLE.z);
        tank(below);
        UltronFight fight = onTheIsland(f, below);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.brain().setEnabled(false);
        double floor = f.helper().absoluteVec(new Vec3(0, FLOOR, 0)).y;
        // The island stands six blocks up. (No blocks are needed for what is tested: the arena is told where it is.)
        fight.island().raiseIsland(6.0, floor + 6.0);
        ServerPlayer above = f.player(MIDDLE.x - 3.0, FLOOR + 6.0, MIDDLE.z);
        tank(above);
        UltronSentryEntity sunk = sentryAt(f, fight, MIDDLE.x + 2.0, MIDDLE.z + 2.0);
        sunk.setHeld(true);
        put(f, boss, MIDDLE.x, FLOOR + 1.0, MIDDLE.z - 2.0);
        check(sunk.getY() < floor + 3.0 && boss.getY() < floor + 3.0, "(the Sentry and the body are down under the island)");
        f.everyTick(100, tick -> {
            if (tick < UltronFight.TETHER_INTERVAL + 2) {
                return false;
            }
            fight.refresh();
            check(!fight.isTargetable(below) && fight.isTargetable(above), "only the fighter on the island is hunted");
            check(fight.targets().contains(above) && !fight.targets().contains(below), "and only they are the army's targets");
            check(sunk.isAlive() && sunk.getY() >= floor + 6.0, "the Sentry under the island was brought back onto it: y " + (sunk.getY() - floor));
            check(boss.getY() >= floor + 6.0, "and so was his body: y " + (boss.getY() - floor));
            check(Math.hypot(sunk.getX() - fight.island().center().x, sunk.getZ() - fight.island().center().z) <= fight.armyReach(0.0) + 0.01,
                    "inside the island's rim");
            fight.island().landIsland();
            fight.refresh();
            check(fight.isTargetable(below), "with the island down the fighter on the ground is hunted again");
            return true;
        });
    }

    /**
     * The fallen hear how it ends. In the game a player who dies leaves the level's list a second later (the body is
     * removed, {@code LivingEntity.tickDeath}), and with it the boss bars; a wipe comes fifteen seconds after the last one
     * fell, so it must still reach them on their death screens.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_wipe_told", timeoutTicks = 200)
    public static void aWipeIsToldToTheFallen(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 3);
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        Log log = new Log(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        player.kill();
        // (What the game does to a fallen player after twenty ticks; a test player is never ticked, so the test does it.)
        player.remove(Entity.RemovalReason.KILLED);
        check(!f.level().players().contains(player), "the fallen player has left the level's list");
        f.everyTick(180, tick -> {
            if (!fight.isOver()) {
                check(tick < 90, "the wipe never came");
                return false;
            }
            check(fight.result() == BossEnd.WIPE, "a wipe");
            check(!fight.bars().viewers().contains(player), "(the bars were taken from the fallen player)");
            log.drain();
            check(log.said("speech.enhanced.ultron.wipe"), "the fallen fighter hears his last line");
            check(log.said(BossEnd.WIPE.messageKey()), "and is told the fight is lost");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "ultron_stop", timeoutTicks = 300)
    public static void anOperatorStopsItAndTheProgramComesBack(GameTestHelper helper) throws Exception {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, player);
        fight.refundOnFailure(new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        check(player.getInventory().countItem(ModItems.ULTRON_PROGRAM.get()) == 0, "(the Program was used up)");
        check(run(player, "enhanced boss stop") == 1, "stop");
        check(fight.isOver() && fight.result() == BossEnd.COMMAND, "an operator's ending");
        check(player.getInventory().countItem(ModItems.ULTRON_PROGRAM.get()) == 1, "the Program is back in the starter's hands");
        check(cores(player) == 0, "and no Core");
        f.everyTick(200, tick -> {
            if (fight.isFinished()) {
                check(level.getEntitiesOfClass(UltronEntity.class, helper.getBounds()).isEmpty() && level.getEntitiesOfClass(UltronSentryEntity.class, helper.getBounds()).isEmpty(),
                        "after its pause nothing of the fight is left");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "ultron_abort", timeoutTicks = 300)
    public static void killingTheBodyEndsTheFightWithoutAReward(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, player);
        fight.refundOnFailure(new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        UltronEntity boss = boss(fight);
        boss.kill();
        check(!boss.isAlive(), "/kill really kills him, even on the ground");
        f.everyTick(240, tick -> {
            if (fight.isFinished()) {
                check(fight.result() == BossEnd.ABORT, "the fight was aborted: " + fight.result());
                check(cores(player) == 0 && player.getInventory().countItem(ModItems.ULTRON_PROGRAM.get()) == 0, "no reward, no refund");
                check(level.getEntitiesOfClass(UltronSentryEntity.class, helper.getBounds(), UltronSentryEntity::isAlive).isEmpty(), "his army is gone");
                return true;
            }
            return false;
        });
    }

    /**
     * An operator's /kill (or the void) takes his body while he is moving into a Sentry: the fight was waiting for the
     * body to arrive, and without it would wait for ever, holding its fighters. It ends as an abort, like any other loss
     * of the body.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_abort_upload", timeoutTicks = 300)
    public static void killingTheBodyInTheMiddleOfAnUploadEndsTheFight(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheIsland(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronSentryEntity vessel = sentryAt(f, fight, 15.5, 10.5);
        vessel.setHeld(true);
        boss.setHealth(1.0F);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().generic(), 5.0F);
        check(fight.isTransferring(), "he is moving into the Sentry");
        boss.kill();
        check(!boss.isAlive(), "/kill takes him even now");
        f.everyTick(240, tick -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(fight.result() == BossEnd.ABORT, "the fight was aborted: " + fight.result());
            check(cores(player) == 0, "no reward");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "ultron_server_stop")
    public static void theServerStoppingPutsEverythingBackAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        Recorder lift = new Recorder();
        LiftHook old = LiftHooks.install(lift);
        f.onClose(() -> LiftHooks.install(old));
        UltronFight fight = onTheGround(f, player);
        check(level.getEntitiesOfClass(UltronEntity.class, helper.getBounds()).size() == 1, "he is there");
        BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
        check(fight.isFinished() && lift.calls.contains("finish:true"), "the fight is finished, the lift told to put everything back now: " + lift.calls);
        check(level.getEntitiesOfClass(UltronEntity.class, helper.getBounds(), ALIVE).isEmpty()
                && level.getEntitiesOfClass(UltronSentryEntity.class, helper.getBounds(), ALIVE).isEmpty(), "and nothing of it is left in the world");
        helper.succeed();
    }

    private static final java.util.function.Predicate<net.minecraft.world.entity.Entity> ALIVE = net.minecraft.world.entity.Entity::isAlive;

    // ------------------------------------------------------------------------------------------------------------
    // The bars

    @EnhancedTest(template = ARENA, batch = "ultron_bars", timeoutTicks = 400)
    public static void bossBars(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = start(f, player);
        ServerBossEvent health = fight.bars().health();
        ServerBossEvent sentries = fight.bars().second();
        check(!health.isVisible() && !sentries.isVisible(), "no bars before the body appears");
        check(fight.bars().health().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_6 && fight.bars().second().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_20,
                "the health bar is notched in six, the Sentries' in twenty");

        int[] stage = {0};
        int[] since = {0};
        f.everyTick(380, tick -> {
            since[0]++;
            switch (stage[0]) {
                case 0 -> {
                    fight.forceState(UltronFight.Phase.GROUND);
                    calm(fight);
                    stage[0] = 1;
                    since[0] = 0;
                }
                case 1 -> {
                    if (since[0] == 3) {
                        UltronEntity boss = boss(fight);
                        check(health.isVisible() && sentries.isVisible(), "both bars are up");
                        check(barKey(fight).equals("boss.enhanced.ultron") && health.getColor() == BossEvent.BossBarColor.RED, "'Ultron', in red");
                        assertNear(health.getProgress(), 1.0, 1.0E-3, "full");
                        check(sentriesKey(sentries).equals("boss.enhanced.ultron.sentries") && argOf(sentries.getName()).equals(26), "the Sentries: six here and twenty to come: " + argOf(sentries.getName()));
                        assertNear(sentries.getProgress(), 1.0, 1.0E-3, "full");
                        boss.setHealth(boss.getMaxHealth() * 0.6F);
                        fight.sentries().get(0).kill();
                        fight.sentries().get(1).kill();
                        stage[0] = 2;
                        since[0] = 0;
                    }
                }
                case 2 -> {
                    if (since[0] == 3) {
                        assertNear(health.getProgress(), 0.6, 0.01, "the health bar follows his health");
                        check(argOf(sentries.getName()).equals(24), "two Sentries fell: 24 left: " + argOf(sentries.getName()));
                        assertNear(sentries.getProgress(), 24.0 / 26.0, 0.01, "and the second bar with them");
                        fight.forceState(UltronFight.Phase.LIFT_WARN);
                        stage[0] = 3;
                        since[0] = 0;
                    }
                }
                case 3 -> {
                    if (since[0] == 40) {
                        check(barKey(fight).equals("boss.enhanced.ultron.lift") && health.getColor() == BossEvent.BossBarColor.YELLOW, "the lift: the bar is yellow");
                        assertNear(health.getProgress(), 40.0 / 200.0 * 0.5, 0.02, "and fills as the warning runs: " + health.getProgress());
                        fight.forceState(UltronFight.Phase.ISLAND);
                        UltronEntity boss = boss(fight);
                        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
                        for (UltronSentryEntity sentry : fight.sentries()) {
                            sentry.setHeld(true);
                        }
                        calm(fight);
                        boss.setHealth(1.0F);
                        boss.invulnerableTime = 0;
                        boss.hurt(level.damageSources().generic(), 5.0F);
                        stage[0] = 4;
                        since[0] = 0;
                    }
                }
                case 4 -> {
                    if (since[0] == 20) {
                        check(barKey(fight).equals("boss.enhanced.ultron.upload") && health.getColor() == BossEvent.BossBarColor.WHITE, "an upload: white");
                        assertNear(health.getProgress(), 20.0 / 80.0, 0.05, "which fills in eighty ticks: " + health.getProgress());
                    }
                    if (since[0] == 90) {
                        check(barKey(fight).equals("boss.enhanced.ultron.body") && argOf(health.getName()).equals(2) && health.getColor() == BossEvent.BossBarColor.RED,
                                "then his second body, in red");
                        assertNear(health.getProgress(), 1.0, 1.0E-3, "with all its health");
                        UltronEntity boss = boss(fight);
                        fight.spendBudget(fight.budgetLeft());
                        for (UltronSentryEntity sentry : new ArrayList<>(fight.sentries())) {
                            sentry.discard();
                        }
                        boss.setHealth(1.0F);
                        boss.invulnerableTime = 0;
                        boss.hurt(level.damageSources().generic(), 5.0F);
                        check(fight.isDying(), "the last body is dying");
                        stage[0] = 5;
                        since[0] = 0;
                    }
                }
                default -> {
                    if (since[0] == 30) {
                        check(barKey(fight).equals("boss.enhanced.ultron"), "in his death the bar says 'Ultron'");
                        assertNear(health.getProgress(), 0.5, 0.1, "and empties: " + health.getProgress());
                        check(argOf(sentries.getName()).equals(0), "no Sentries left");
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static String sentriesKey(BossEvent bar) {
        return bar.getName().getContents() instanceof TranslatableContents t ? t.getKey() : "?";
    }

    // ------------------------------------------------------------------------------------------------------------
    // The arena

    @EnhancedTest(template = ARENA, batch = "ultron_arena")
    public static void theArenaIsTheGroundAndMore(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, player);
        var arena = fight.island();
        int radius = UltronBossConfig.ISLAND_RADIUS.get();
        check(arena.islandRadius() == radius && fight.islandRadius() == radius, "the island's radius is the config's: " + radius);
        assertNear(arena.radius(), radius + 12.0, 1.0E-6, "the arena is the island and a margin of 12");
        assertNear(arena.containRadius(), radius + 12.0, 1.0E-6, "the field holds at the same radius");
        assertNear(arena.joinRadius(), radius + 24.0, 1.0E-6, "whoever comes within 24 of the rim joins");
        check(arena.ticketRadius() == radius + 24 && arena.ticketDistance() == 3, "the region is held loaded out to there");
        check(arena.keepsDrops() && arena.gates(), "it keeps drops and gates teleports");
        check(arena.containment() == com.kolodkingal.enhanced.boss.BossArena.Containment.FIELD, "its edge is a field");
        check(arena.islandOffset() == 0.0, "the island is on the ground");
        Vec3 centre = arena.center();
        check(BossFights.protects(level, BlockPos.containing(centre).below()) && BossFights.protects(level, BlockPos.containing(centre.add(radius + 10, 0, 0))),
                "nothing inside it can be broken or placed");
        check(!BossFights.protects(level, BlockPos.containing(centre.add(radius + 13, 0, 0))), "and nothing outside it is protected");
        check(BossFights.allowsTeleport(level, player, centre.add(radius + 10, 0, 0)) && !BossFights.allowsTeleport(level, player, centre.add(radius + 13, 0, 0)),
                "a fighter can teleport inside it and not out of it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drops

    @EnhancedTest(template = ARENA, batch = "ultron_drops", timeoutTicks = 60)
    public static void droppedItemsDontDespawn(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer fighter = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        UltronFight fight = onTheGround(f, fighter);
        check(fight.arena().keepsDrops(), "the arena keeps what falls in it");
        helper.runAfterDelay(3, () -> {
            fighter.getInventory().add(new ItemStack(Items.DIAMOND));
            fight.sentries().get(0).kill();
            fight.sentries().get(1).kill();
            fighter.kill();
            AABB around = new AABB(fighter.blockPosition()).inflate(5);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Items.DIAMOND));
            check(drops.size() == 1 && drops.get(0).getAge() < -30000, "what a fighter drops when they die will not despawn while the fight lasts");
            check(level.getEntitiesOfClass(ItemEntity.class, helper.getBounds(), item -> !item.getItem().is(Items.DIAMOND)).isEmpty()
                    && level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, helper.getBounds()).isEmpty(), "and nothing else drops: a Sentry leaves no loot and no experience");
            BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
            int age = drops.get(0).getAge();
            check(age > -6100 && age < -5900, "five more minutes after it ends: " + age);
            helper.succeed();
        });
    }

    private UltronFightTests() {
    }
}
