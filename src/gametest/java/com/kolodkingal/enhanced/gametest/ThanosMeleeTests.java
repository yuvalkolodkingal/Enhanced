package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.MAX;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.blow;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fighter;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fixture;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.lost;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.near;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The sword and the fist: the Sweep, the Smash, the Jab, the thrown sword, the Choke and the Leap. Each move is started
 * with {@code forceMove} and read as damage: a fighter has a thousand health, so what he lost is what he was hit for.
 */
public final class ThanosMeleeTests {

    private static ThanosEntity boss(TestKit.Fixture f, ServerPlayer starter) {
        return ThanosBossTests.boss(f, starter);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Sweep

    /**
     * The Sweep hits what is inside the cone ahead of him for 30 (damage type thanos_sword), breaks a raised Shield for
     * five seconds and throws a rider off; the one behind him is untouched.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_sweep", timeoutTicks = 100)
    public static void theSweepHitsTheConeBreaksShieldsAndThrowsRiders(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer front = fighter(f, 0, 3);
        ServerPlayer side = fighter(f, 2, 2.5);
        ServerPlayer behind = fighter(f, 0, -3);
        ServerPlayer rider = fighter(f, -2, 2);
        ThanosEntity boss = boss(f, front);
        Pig pig = EntityType.PIG.create(level);
        pig.moveTo(ThanosMoveSupport.spot(f, -2, FLOOR, 2));
        level.addFreshEntity(pig);
        rider.startRiding(pig, true);
        front.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        front.startUsingItem(InteractionHand.OFF_HAND);
        boss.forceMove(ThanosMove.SWORD_SWEEP, front);
        f.everyTick(40, t -> {
            front.doTick();
            if (t == 10) {
                check(front.isBlocking(), "the Shield is up before the blow");
            }
            if (t < 14) {
                check(lost(side) == 0.0F, "nothing lands in the wind-up: " + t);
            }
            if (t < 22) {
                return false;
            }
            float damage = blow(boss, ThanosBossConfig.SWORD_DAMAGE.get());
            near(lost(side), damage, "the one in the cone is hit for the sword's damage");
            near(lost(front), damage, "so is the one who raised a Shield: it breaks first");
            check(side.getLastDamageSource() != null && side.getLastDamageSource().is(ModDamageTypes.THANOS_SWORD), "by the sword");
            check(front.getCooldowns().isOnCooldown(Items.SHIELD), "his Shield is out for a while");
            check(lost(behind) == 0.0F, "the one behind him is untouched");
            check(lost(rider) > 0.0F && !rider.isPassenger(), "the rider is thrown from the mount and hurt");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Smash

    /** The Smash hurts the spot ahead of him for 26 and sends a ring that only someone on the ground meets (12). */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_smash", timeoutTicks = 100)
    public static void theSmashStrikesTheSpotAndTheRingIsJumpable(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer under = fighter(f, 0, 4);
        ServerPlayer onTheGround = fighter(f, 5, 4);
        ServerPlayer jumper = fighter(f, -5, 4);
        ThanosEntity boss = boss(f, under);
        Vec3 high = ThanosMoveSupport.spot(f, -5, FLOOR + 3.0, 4);
        jumper.teleportTo(f.level(), high.x, high.y, high.z, 0.0F, 0.0F);
        boss.forceMove(ThanosMove.OVERHEAD_SMASH, under);
        f.everyTick(60, t -> {
            // The jumper hangs in the air for the ring.
            jumper.setDeltaMovement(Vec3.ZERO);
            jumper.setPos(high);
            if (t < 40) {
                return false;
            }
            near(lost(under), blow(boss, ThanosBossConfig.SMASH_DAMAGE.get()), "the one under the blade takes the smash");
            near(lost(onTheGround), blow(boss, ThanosBossConfig.SMASH_RING_DAMAGE.get()), "the one on the ground takes the ring");
            check(lost(jumper) == 0.0F, "the one in the air takes nothing");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Jab

    /** The Jab is two blows, eight ticks apart, each for 18; it does not reach far. */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_jab", timeoutTicks = 100)
    public static void theJabHitsTwiceAndShortOfFarAway(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer close = fighter(f, 0, 2);
        ServerPlayer far = fighter(f, 0, -6);
        ThanosEntity boss = boss(f, close);
        boss.forceMove(ThanosMove.GAUNTLET_JAB, close);
        float single = blow(boss, ThanosBossConfig.JAB_DAMAGE.get());
        f.everyTick(40, t -> {
            if (t == 12) {
                near(lost(close), single, "one blow by the end of the first strike");
            }
            if (t < 24) {
                return false;
            }
            near(lost(close), 2.0F * single, "two blows in all");
            check(close.getLastDamageSource() != null && close.getLastDamageSource().is(ModDamageTypes.THANOS_BLOW), "with the fist, not the sword");
            check(lost(far) == 0.0F, "the one out of reach is untouched");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The thrown sword

    /**
     * The sword flies out and back, hits a creature once on each pass, and while it is away he has no Sweep and no Smash
     * (and nothing to throw).
     */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_throw", timeoutTicks = 260)
    public static void theSwordFliesHitsOncePerPassAndComesHome(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer target = fighter(f, 0, 6);
        ThanosEntity boss = boss(f, target);
        boss.forceMove(ThanosMove.SWORD_THROW, target);
        float one = blow(boss, ThanosBossConfig.SWORD_THROW_DAMAGE.get());
        boolean[] seenAway = {false};
        boolean[] sawOnePass = {false};
        boolean[] checkedWeights = {false};
        Set<ThanosMove> backHome = new HashSet<>();
        f.everyTick(240, t -> {
            if (boss.swordAway()) {
                seenAway[0] = true;
                if (!checkedWeights[0]) {
                    checkedWeights[0] = true;
                    for (int i = 0; i < 300; i++) {
                        ThanosMove pick = boss.pickMove(target);
                        check(pick != ThanosMove.SWORD_SWEEP && pick != ThanosMove.OVERHEAD_SMASH && pick != ThanosMove.SWORD_THROW,
                                "no sword move while the sword is away: " + pick);
                    }
                }
                if (Math.abs(lost(target) - one) < 0.6F) {
                    sawOnePass[0] = true;
                }
                return false;
            }
            if (!seenAway[0]) {
                check(t < 40, "the sword was thrown");
                return false;
            }
            check(sawOnePass[0], "one pass hurt him once");
            near(lost(target), 2.0F * one, "and the way back once more");
            check(f.level().getEntitiesOfClass(ThanosSwordEntity.class, boss.getBoundingBox().inflate(60.0)).isEmpty(), "the sword is back in his hand");
            for (int i = 0; i < 400; i++) {
                ThanosMove pick = boss.pickMove(target);
                if (pick != null) {
                    backHome.add(pick);
                }
            }
            check(backHome.contains(ThanosMove.OVERHEAD_SMASH), "with the sword home the Smash is in his choice again: " + backHome);
            return true;
        });
    }

    /** A Chaos Shield turns the sword back toward him, as it does Mjolnir, and pays what the throw would have done. */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_throw_shield", timeoutTicks = 260)
    public static void aChaosShieldTurnsTheSwordHome(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = fighter(f, 0, 6, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda);
        PowerData data = Powers.get(wanda);
        data.raiseChaosShield(f.level().getGameTime() - 100L, 0, 500.0F);
        boss.forceMove(ThanosMove.SWORD_THROW, wanda);
        float cost = ThanosBossConfig.SWORD_THROW_DAMAGE.get().floatValue();
        boolean[] seenAway = {false};
        f.everyTick(200, t -> {
            PowerTicker.tick(wanda);
            if (boss.swordAway()) {
                seenAway[0] = true;
                return false;
            }
            if (!seenAway[0]) {
                return false;
            }
            check(wanda.getHealth() == wanda.getMaxHealth(), "the sword never touched her");
            TestKit.assertNear(500.0F - data.shieldStrength(), cost, 0.01, "the shield paid what the throw would have done, once");
            check(data.chaosShieldActive(), "and held");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Choke

    /**
     * The Choke takes one victim (a Loki or a Vision before the rest), holds him up for 40 ticks with 4 damage every 10, throws
     * him and hurts him where he lands; the Loki hears his line. The others near him are not touched.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_choke", timeoutTicks = 200)
    public static void theChokeHoldsOneVictimLiftsThrowsAndHurts(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        // (3.5: Loki's body has armor; the numbers here are the choke's own, so his body wears none in this test.)
        f.config(EnhancedConfig.LOKI_BODY_ARMOR, 0.0);
        f.config(EnhancedConfig.LOKI_BODY_TOUGHNESS, 0.0);
        ServerPlayer victim = fighter(f, 0, 2, Power.LOKI);
        ServerPlayer bystander = fighter(f, 1.6, 2.2);
        ThanosEntity boss = boss(f, victim);
        TestKit.sentPackets(victim);
        boss.forceMove(ThanosMove.CHOKE, victim);
        double floor = victim.getY();
        float quarter = blow(boss, ThanosBossConfig.CHOKE_DAMAGE.get()) / 4.0F;
        double[] highest = {0.0};
        boolean[] told = {false};
        f.everyTick(120, t -> {
            victim.doTick();
            for (Object packet : TestKit.sentPackets(victim)) {
                if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.overlay()
                        && chat.content().getContents() instanceof TranslatableContents c && c.getKey().equals("message.enhanced.thanos.loki")) {
                    told[0] = true;
                }
            }
            highest[0] = Math.max(highest[0], victim.getY() - floor);
            if (t == 14 + 20) {
                near(lost(victim), 2.0F * quarter, "four damage every ten ticks (two by now)");
                check(victim.getY() - floor > 0.8, "he is held off the ground: " + (victim.getY() - floor));
            }
            if (boss.currentMove() == ThanosMove.CHOKE) {
                return false;
            }
            check(told[0], "a Loki is told whose hand he is in");
            check(highest[0] > 0.8, "he was lifted");
            near(lost(victim), 4.0F * quarter + 2.0F * quarter, "four small damages and the one where he lands");
            check(lost(bystander) == 0.0F, "the one next to him was never held");
            return true;
        });
    }

    /** 60 damage from another fighter makes him let go, and staggers him; a Creative or phased victim is never taken. */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_choke_break", timeoutTicks = 200)
    public static void anotherFighterCanBreakTheChokeAndTheUntouchableAreNeverTaken(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer victim = fighter(f, 0, 2);
        ServerPlayer rescuer = fighter(f, -6, -6);
        ThanosEntity boss = boss(f, victim);
        boss.forceMove(ThanosMove.CHOKE, victim);
        boolean[] struck = {false};
        f.everyTick(100, t -> {
            victim.doTick();
            if (!struck[0]) {
                if (t == 14 + 10) {
                    check(victim.getY() > f.helper().absoluteVec(new Vec3(0, FLOOR, 0)).y + 0.3, "he is held");
                    struck[0] = true;
                    // The rescuer's blows: well over sixty after the plates.
                    for (int i = 0; i < 4; i++) {
                        ThanosBossTests.hit(boss, rescuer, 80.0F);
                    }
                }
                return false;
            }
            check(boss.state() == ThanosState.STAGGERED, "he is staggered: " + boss.state());
            check(boss.currentMove() == null, "the Choke ended");
            check(lost(victim) < 3.0F * blow(boss, ThanosBossConfig.CHOKE_DAMAGE.get()) / 4.0F, "he was let go early");
            return true;
        });
    }

    /** A Creative player and a phased Vision are not grabbed: both stand in the Choke's cone and nobody is lifted or hurt. */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_choke_never", timeoutTicks = 80)
    public static void theChokeNeverTakesTheUntouchable(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer creative = f.player(ThanosBossTests.CENTRE.x, FLOOR, ThanosBossTests.CENTRE.z + 2.0, net.minecraft.world.level.GameType.CREATIVE);
        ServerPlayer vision = fighter(f, 0.4, 2.4, Power.VISION);
        Powers.get(vision).setVisionPhased(true);
        ThanosEntity boss = boss(f, vision);
        boss.forceMove(ThanosMove.CHOKE, vision);
        double floor = f.helper().absoluteVec(new Vec3(0, FLOOR, 0)).y;
        f.everyTick(70, t -> {
            creative.doTick();
            vision.doTick();
            check(creative.getY() <= floor + 0.01 && vision.getY() <= floor + 0.01, "nobody is lifted: " + t);
            if (boss.currentMove() == ThanosMove.CHOKE) {
                return false;
            }
            check(lost(vision) == 0.0F && creative.getHealth() == creative.getMaxHealth(), "nobody is hurt");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Leap

    /** The Leap crosses the arena to where the target stood and hurts the ground there for 20; those further off are spared. */
    @EnhancedTest(template = ARENA, batch = "thanos_melee_leap", timeoutTicks = 160)
    public static void theLeapLandsWhereHeAimedAndHurtsThere(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer target = fighter(f, 0, 8.5);
        ServerPlayer spared = fighter(f, -8, -3);
        ThanosEntity boss = boss(f, target);
        Vec3 start = boss.position();
        boss.forceMove(ThanosMove.LEAP, target);
        f.everyTick(120, t -> {
            if (boss.currentMove() == ThanosMove.LEAP) {
                return false;
            }
            check(boss.position().distanceTo(start) > 6.0, "he crossed the arena: " + boss.position().distanceTo(start));
            near(lost(target), blow(boss, ThanosBossConfig.LEAP_DAMAGE.get()), "the ground where he came down is hurt");
            check(lost(spared) == 0.0F, "the one far from the landing is spared");
            return true;
        });
    }

    private ThanosMeleeTests() {
    }
}
