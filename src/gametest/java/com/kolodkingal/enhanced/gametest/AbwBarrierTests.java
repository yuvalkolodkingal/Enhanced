package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * 3.2, Billy's Hex Barrier (Sneak + B held; {@code billy.barrier*}): a directional plate of blue magic that stops
 * projectiles, blows and part of explosions coming from the front arc ({@code barrierArc}, 60 degrees either side of his
 * look) and nothing from the sides or the back; limited on purpose (a strength to wear down, a time to hold, a recharge
 * after it comes down or breaks, hunger, a slower walk) and growing with his Build Magic rank; it follows the Chaos Shield's
 * damage rules, so no boss move that the shield does not stop is stopped by it. Billy stands at (10.5, 8.5) looking south
 * (+z): "in front" is z greater than his.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class AbwBarrierTests {
    private static ServerPlayer billy(TestKit.Fixture f) {
        ServerPlayer billy = f.player(10.5, FLOOR, 8.5, Power.BILLY);
        billy.getFoodData().setFoodLevel(20);
        return billy;
    }

    /** Sneak + B down, then up with Sneak (as a player's fingers do): the barrier forms; {@code then} runs once it is complete. */
    private static void raise(TestKit.Fixture f, ServerPlayer billy, Runnable then) {
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        billy.setShiftKeyDown(false);
        check(Powers.get(billy).chaosShieldActive(), "Sneak + B starts the barrier");
        f.helper().runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(billy), "the barrier is complete after its wind-up");
            then.run();
        });
    }

    private static void arrowBlow(GameTestHelper helper, ServerPlayer victim, Vec3 from, float damage) {
        Skeleton shooter = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(1.5, FLOOR, 1.5));
        shooter.setNoAi(true);
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        arrow.setPos(from);
        victim.invulnerableTime = 0;
        victim.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), damage);
        arrow.discard();
    }

    private static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.setNoAi(true);
        return husk;
    }

    private static Vec3 at(GameTestHelper helper, double x, double y, double z) {
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static float strength(ServerPlayer billy) {
        return Powers.get(billy).shieldStrength();
    }

    /** Front: stopped and paid for. Side, back: they pass whole. */
    @GameTest(template = ARENA, batch = "abw_barrier_arc", timeoutTicks = 60)
    public static void barrierStopsWhatComesFromTheFrontOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        raise(f, billy, () -> {
            float full = strength(billy);
            assertNear(full, EnhancedConfig.BILLY_BARRIER_STRENGTH.get(), 1.0E-3, "a fresh barrier has the first rank's strength");
            // an arrow from the front
            arrowBlow(helper, billy, at(helper, 10.5, FLOOR + 1.0, 14.5), 5.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "an arrow from the front is stopped");
            assertNear(full - strength(billy), 5.0, 1.0E-3, "and the barrier pays what it would have done");
            // from the back, and from the side: whole
            arrowBlow(helper, billy, at(helper, 10.5, FLOOR + 1.0, 2.5), 3.0F);
            assertNear(20.0F - billy.getHealth(), 3.0, 1.0E-4, "an arrow from behind goes through");
            arrowBlow(helper, billy, at(helper, 16.5, FLOOR + 1.0, 8.5), 2.0F);
            assertNear(20.0F - billy.getHealth(), 5.0, 1.0E-4, "and one from the side");
            assertNear(full - strength(billy), 5.0, 1.0E-3, "which did not wear it");
            // the edge of the arc: 59 degrees from his look is covered, 61 is not
            double r = 5.0;
            arrowBlow(helper, billy, at(helper, 10.5 + r * Math.sin(Math.toRadians(59.0)), FLOOR + 1.0, 8.5 + r * Math.cos(Math.toRadians(59.0))), 1.0F);
            assertNear(20.0F - billy.getHealth(), 5.0, 1.0E-4, "59 degrees round is still in front of him");
            arrowBlow(helper, billy, at(helper, 10.5 + r * Math.sin(Math.toRadians(61.0)), FLOOR + 1.0, 8.5 + r * Math.cos(Math.toRadians(61.0))), 1.0F);
            assertNear(20.0F - billy.getHealth(), 6.0, 1.0E-4, "61 degrees round is not");
            check(BillyBarrier.isUp(billy), "it still stands");
            helper.succeed();
        });
    }

    /** A creature's blow: stopped from the front and the attacker thrown back; from behind it lands. */
    @GameTest(template = ARENA, batch = "abw_barrier_melee", timeoutTicks = 60)
    public static void barrierStopsABlowFromTheFrontAndThrowsTheAttackerBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        Husk front = husk(helper, 10.5, 10.0);
        Husk back = husk(helper, 10.5, 6.5);
        raise(f, billy, () -> {
            float full = strength(billy);
            billy.invulnerableTime = 0;
            check(!billy.hurt(helper.getLevel().damageSources().mobAttack(front), 6.0F), "the blow from the front does not land");
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "he is not touched");
            assertNear(full - strength(billy), 6.0, 1.0E-3, "the barrier paid for it");
            check(front.getDeltaMovement().z > 0.2, "and the attacker is thrown back, away from him: " + front.getDeltaMovement());
            billy.invulnerableTime = 0;
            billy.hurt(helper.getLevel().damageSources().mobAttack(back), 4.0F);
            assertNear(20.0F - billy.getHealth(), 4.0, 1.0E-4, "a blow from behind lands whole");
            assertNear(full - strength(billy), 6.0, 1.0E-3, "and costs the barrier nothing");
            helper.succeed();
        });
    }

    /** Explosions in front: half gets through (billy.barrierExplosionFactor); behind: all. */
    @GameTest(template = ARENA, batch = "abw_barrier_explosion", timeoutTicks = 60)
    public static void barrierTakesHalfOfAnExplosionInFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        Husk front = husk(helper, 10.5, 12.0);
        Husk back = husk(helper, 10.5, 4.0);
        raise(f, billy, () -> {
            float full = strength(billy);
            billy.invulnerableTime = 0;
            billy.hurt(helper.getLevel().damageSources().explosion(front, null), 8.0F);
            assertNear(20.0F - billy.getHealth(), 4.0, 1.0E-3, "half of an explosion in front gets through");
            assertNear(full - strength(billy), 4.0, 1.0E-3, "the other half cost the barrier");
            billy.invulnerableTime = 0;
            float before = billy.getHealth();
            billy.hurt(helper.getLevel().damageSources().explosion(back, null), 6.0F);
            assertNear(before - billy.getHealth(), 6.0, 1.0E-3, "one behind him hits in full");
            helper.succeed();
        });
    }

    /** Limited: the blow that takes the last strength breaks it (and is stopped), B recharges for 20 seconds, and it can't be raised meanwhile. */
    @GameTest(template = ARENA, batch = "abw_barrier_break", timeoutTicks = 60)
    public static void barrierBreaksAndRecharges(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        PowerData data = Powers.get(billy);
        raise(f, billy, () -> {
            arrowBlow(helper, billy, at(helper, 10.5, FLOOR + 1.0, 14.5), 30.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the arrow that breaks it is stopped too");
            check(!data.chaosShieldActive() && !BillyBarrier.isUp(billy), "the barrier broke");
            long left = data.cooldownUntil(4) - helper.getLevel().getGameTime();
            check(left >= 395 && left <= 400, "and B recharges for 20 seconds: " + left);
            arrowBlow(helper, billy, at(helper, 10.5, FLOOR + 1.0, 14.5), 4.0F);
            assertNear(20.0F - billy.getHealth(), 4.0, 1.0E-4, "without it the next arrow lands");
            billy.setShiftKeyDown(true);
            AbilityHandler.onKey(billy, 4, true);
            check(!data.chaosShieldActive(), "and it cannot be raised while B recharges");
            helper.succeed();
        });
    }

    /** It runs out of time (5 seconds at the first rank), a shorter recharge follows; letting go lowers it at once; before it formed there is no recharge. */
    @GameTest(template = ARENA, batch = "abw_barrier_time", timeoutTicks = 160)
    public static void barrierRunsOutAndLettingGoLowersIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        PowerData data = Powers.get(billy);
        // released before it formed: nothing to recharge
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        AbilityHandler.onKey(billy, 4, false);
        check(!data.chaosShieldActive(), "B released: it is down");
        check(data.isReady(4, helper.getLevel().getGameTime()), "and a barrier that never formed costs no recharge");
        raise(f, billy, () -> {
            long raisedAt = data.shieldFormedAt();
            f.everyTick(115, tick -> {
                PowerTicker.tick(billy);
                long now = helper.getLevel().getGameTime();
                if (now < raisedAt + EnhancedConfig.BILLY_BARRIER_DURATION.get() - 1) {
                    check(data.chaosShieldActive(), "still up at " + (now - raisedAt) + " ticks");
                    return false;
                }
                if (data.chaosShieldActive()) {
                    return false;
                }
                long left = data.cooldownUntil(4) - now;
                check(left > 190 && left <= 200, "it ran out of time: B recharges 10 seconds: " + left);
                data.startCooldown(4, now, -(int) left); // ready again
                billy.setShiftKeyDown(true);
                AbilityHandler.onKey(billy, 4, true);
                billy.setShiftKeyDown(false);
                check(data.chaosShieldActive(), "raised again");
                AbilityHandler.onKey(billy, 4, false);
                check(!data.chaosShieldActive(), "letting go lowers it at once");
                check(data.isReady(4, now), "(it had not formed yet, so no recharge follows)");
                return true;
            });
        });
    }

    /** Hunger: it costs some a second and cannot be raised (or held) on an empty stomach. */
    @GameTest(template = ARENA, batch = "abw_barrier_hunger", timeoutTicks = 60)
    public static void barrierCostsHungerAndSlowsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        PowerData data = Powers.get(billy);
        billy.getFoodData().setFoodLevel(0);
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        billy.setShiftKeyDown(false);
        check(!data.chaosShieldActive(), "on an empty stomach it cannot be raised");
        billy.getFoodData().setFoodLevel(20);
        double walk = billy.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        raise(f, billy, () -> {
            float exhaustion = billy.getFoodData().getExhaustionLevel();
            for (int i = 0; i < 20; i++) {
                PowerTicker.tick(billy);
            }
            check(billy.getFoodData().getExhaustionLevel() > exhaustion, "holding it burns hunger");
            double slowed = billy.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            assertNear(slowed, walk * EnhancedConfig.BILLY_BARRIER_SPEED_FACTOR.get(), 1.0E-4, "he walks at 0.6 of his speed while it is up");
            billy.getFoodData().setFoodLevel(0);
            PowerTicker.tick(billy);
            check(!data.chaosShieldActive(), "starving brings it down");
            assertNear(billy.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), walk, 1.0E-4, "and he walks as usual again");
            helper.succeed();
        });
    }

    /** Strength and time grow evenly with the rank, from the first to the highest. */
    @GameTest(template = ARENA, batch = "abw_barrier_rank")
    public static void barrierGrowsWithBuildMagicRank(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(BuildConfig.BILLY_MAX_RANK, 4);
        float strength = 0.0F;
        int ticks = 0;
        for (int rank = 1; rank <= 4; rank++) {
            BuildData build = BuildData.EMPTY.withRank(rank);
            double progress = (rank - 1) / 3.0;
            assertNear(BillyBarrier.maxStrength(build), 20.0 * (1.0 + 2.0 * progress), 1.0E-3, "strength at rank " + rank);
            assertNear(BillyBarrier.maxTicks(build), 100.0 * (1.0 + progress), 1.0, "time at rank " + rank);
            check(BillyBarrier.maxStrength(build) > strength && BillyBarrier.maxTicks(build) >= ticks, "it grows with every rank");
            strength = BillyBarrier.maxStrength(build);
            ticks = BillyBarrier.maxTicks(build);
        }
        assertNear(BillyBarrier.maxStrength(BuildData.EMPTY.withRank(4)), 60.0, 1.0E-3, "three times as strong at the top");
        ServerPlayer billy = f.player(10.5, FLOOR, 8.5, Power.BILLY);
        billy.setData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA, BuildData.EMPTY.withRank(4));
        billy.getFoodData().setFoodLevel(20);
        raise(f, billy, () -> {
            assertNear(strength(billy), 60.0, 1.0E-3, "and a barrier raised at the top rank has it");
            helper.succeed();
        });
    }

    /** The HUD line is the shield's: forming, then strength and seconds; the option off does nothing. */
    @GameTest(template = ARENA, batch = "abw_barrier_hud", timeoutTicks = 60)
    public static void hudAndSwitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        raise(f, billy, () -> {
            PowerData data = Powers.get(billy);
            long now = helper.getLevel().getGameTime();
            Component line = ShieldStyles.hudLine(data, now, BillyBarrier.maxStrength(BuildData.EMPTY), BillyBarrier.maxTicks(BuildData.EMPTY));
            check(line != null && line.getContents() instanceof TranslatableContents t && t.getKey().equals("hud.enhanced.shield_strength"), "strength and seconds: " + line);
            AbilityHandler.onKey(billy, 4, false);
            f.config(EnhancedConfig.BILLY_BARRIER, false);
            billy.setShiftKeyDown(true);
            AbilityHandler.onKey(billy, 4, true);
            check(!data.chaosShieldActive(), "with billy.barrier off the key raises nothing");
            helper.succeed();
        });
    }

    /** Plain B is still Freeze: with no Sneak it does not raise a barrier. */
    @GameTest(template = ARENA, batch = "abw_barrier_freeze", timeoutTicks = 40)
    public static void plainBIsStillFreeze(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        Husk target = husk(helper, 10.5, 12.0);
        lookAt(billy, target);
        AbilityHandler.onKey(billy, 4, true);
        AbilityHandler.onKey(billy, 4, false);
        check(!Powers.get(billy).chaosShieldActive(), "B alone raises no barrier");
        check(target.hasEffect(com.kolodkingal.enhanced.registry.ModEffects.SPELLBOUND), "it freezes what is in front of him, as before");
        helper.succeed();
    }

    /** Boss fairness: his Backhand is a blow like any (stopped from the front, whole from behind); the Mind Crush and the Entomb are not stopped. */
    @GameTest(template = ARENA, batch = "abw_barrier_boss", timeoutTicks = 120)
    public static void barrierFollowsTheChaosShieldsRulesAgainstTheBoss(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        ServerPlayer billy = d.hero();
        ApocalypseEntity boss = d.boss();
        billy.getFoodData().setFoodLevel(20);
        put(d.f(), billy, 8.5, FLOOR, 10.5);
        TestKit.lookAt(billy, boss);
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        billy.setShiftKeyDown(false);
        helper.runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 2, () -> {
            check(BillyBarrier.isUp(billy), "the barrier is up");
            check(SandControl.entombRefusal(billy) == null, "no entomb is refused for it (only a complete Chaos Shield is skipped)");
            float health = billy.getHealth();
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "he swings");
            helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                assertNear(health - billy.getHealth(), 0.0, 1.0E-3, "his fist from the front is stopped by the plate");
                check(strength(billy) < 1000.0F && strength(billy) < 20.0F, "and it paid for it: " + strength(billy));
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH), "the Mind Crush");
                helper.runAfterDelay(ApocalypseAttack.MIND_CRUSH.tellTicks() + 3, () -> {
                    check(billy.hasEffect(MobEffects.CONFUSION), "is not stopped by the barrier");
                    helper.succeed();
                });
            });
        });
    }
}
