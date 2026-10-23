package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseBrain;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.2, the weakened Apocalypse (requirement 1): the master factor {@code apocalypse_boss.powerFactor} (default 0.65)
 * scales the damage he and his Horsemen deal, the number of his lances, Horsemen and tornadoes, and slows his attacks
 * (the gap and every cooldown last 1/factor times as long); the defaults the lead called unfair are lower. The other
 * Apocalypse tests run with the factor at 1.0 (see {@link ApocalypseTestSupport#fixture}), so these tests have their own
 * batches and set the factor themselves.
 */
public final class AbwApocalypseTests {
    private static final int WARM = 2;

    /** The master factor is 0.65 by default, in the range 0.1 to 1.0, and the retuned defaults are what the notes say. */
    @EnhancedTest(template = ARENA, batch = "abw_apocalypse_defaults")
    public static void defaultsAreWeakerThanIn31(GameTestHelper helper) {
        assertNear(ApocalypseBossConfig.POWER_FACTOR.getDefault(), 0.65, 1.0E-9, "the default factor");
        check(ApocalypseBossConfig.POWER_FACTOR.getSpec().test(0.1) && ApocalypseBossConfig.POWER_FACTOR.getSpec().test(1.0), "0.1 and 1.0 are allowed");
        check(!ApocalypseBossConfig.POWER_FACTOR.getSpec().test(0.05) && !ApocalypseBossConfig.POWER_FACTOR.getSpec().test(1.5), "outside 0.1 to 1.0 is not");
        assertNear(ApocalypseBossConfig.SHIELD_STRENGTH.getDefault(), 100.0, 1.0E-9, "shield 1 is a third weaker (was 150)");
        assertNear(ApocalypseBossConfig.SHIELD_GROWTH.getDefault(), 1.2, 1.0E-9, "later shields grow less (was 1.25)");
        assertNear(ApocalypseBossConfig.SHIELD_PER_EXTRA_PLAYER.getDefault(), 0.3, 1.0E-9, "and less per extra fighter (was 0.35)");
        check(ApocalypseBossConfig.WINDOW_TICKS.getDefault() == 240 && ApocalypseBossConfig.STAGGER_TICKS.getDefault() == 80, "longer windows and staggers");
        assertNear(ApocalypseShieldConfig.SHIELD_REGEN_FRACTION.getDefault(), 0.02, 1.0E-9, "the shield knits slower (was 0.03)");
        check(ApocalypseShieldConfig.SHIELD_REGEN_DELAY_TICKS.getDefault() == 200, "and later (was 160)");
        assertNear(ApocalypseBossConfig.BACKHAND_DAMAGE.getDefault(), 12.0, 1.0E-9, "the Backhand (was 16)");
        assertNear(ApocalypseBossConfig.BOULDER_DAMAGE.getDefault(), 9.0, 1.0E-9, "the Boulder (was 11)");
        assertNear(ApocalypseBossConfig.RING_DAMAGE.getDefault(), 10.0, 1.0E-9, "the Cairo ring (was 12)");
        assertNear(ApocalypseBossConfig.HORSEMAN_DAMAGE_BONUS.getDefault(), 3.0, 1.0E-9, "the Horsemen (was +4)");
        assertNear(ApocalypseBossConfig.HORSEMAN_HEALTH_FACTOR.getDefault(), 1.5, 1.0E-9, "and their health (was x2)");
        helper.succeed();
    }

    /** The helpers: damage times the factor, counts rounded up (never below one, zero stays zero), pauses divided by it. */
    @EnhancedTest(template = ARENA, batch = "abw_apocalypse_helpers")
    public static void helpersApplyTheFactor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(ApocalypseBossConfig.POWER_FACTOR, 0.65);
        assertNear(ApocalypseBossConfig.dealt(10.0), 6.5, 1.0E-4, "10 damage becomes 6.5");
        check(ApocalypseBossConfig.count(5) == 4 && ApocalypseBossConfig.count(3) == 2 && ApocalypseBossConfig.count(4) == 3
                && ApocalypseBossConfig.count(1) == 1 && ApocalypseBossConfig.count(0) == 0, "5 lances become 4, 3 tornadoes 2, 4 Horsemen 3, 1 stays 1");
        check(ApocalypseBossConfig.slower(30) == 46 && ApocalypseBossConfig.slower(20) == 31, "a gap of 30 ticks lasts 46, 20 lasts 31");
        f.config(ApocalypseBossConfig.POWER_FACTOR, 1.0);
        assertNear(ApocalypseBossConfig.dealt(10.0), 10.0, 1.0E-6, "factor 1.0: the 3.1 damage");
        check(ApocalypseBossConfig.count(5) == 5 && ApocalypseBossConfig.slower(30) == 30, "and the 3.1 counts and pace");
        f.config(ApocalypseBossConfig.POWER_FACTOR, 0.1);
        check(ApocalypseBossConfig.count(9) == 1 && ApocalypseBossConfig.count(1) == 1 && ApocalypseBossConfig.slower(30) == 300, "at 0.1: one of each, ten times the pause");
        helper.succeed();
    }

    /** The Backhand does the damage times the factor: 12 at 1.0 (the new default), 6 at 0.5, 7.8 at 0.65. */
    @EnhancedTest(template = ARENA, batch = "abw_apocalypse_backhand", timeoutTicks = 100)
    public static void backhandDamageScalesWithTheFactor(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), hero, 10.5, FLOOR, 8.5);
        TestKit.lookAt(hero, boss);
        helper.runAfterDelay(WARM, () -> {
            float[] health = {hero.getHealth()};
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "he swings at factor 1.0");
            helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                assertNear(health[0] - hero.getHealth(), 12.0, 1.0E-3, "factor 1.0: the default 12");
                d.f().config(ApocalypseBossConfig.POWER_FACTOR, 0.5);
                hero.invulnerableTime = 0;
                health[0] = hero.getHealth();
                boss.brain().clearCooldowns();
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "and again at 0.5");
                helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                    assertNear(health[0] - hero.getHealth(), 6.0, 1.0E-3, "factor 0.5: half of it");
                    d.f().config(ApocalypseBossConfig.POWER_FACTOR, 0.65);
                    hero.invulnerableTime = 0;
                    health[0] = hero.getHealth();
                    boss.brain().clearCooldowns();
                    check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "and at the default 0.65");
                    helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                        assertNear(health[0] - hero.getHealth(), 7.8, 1.0E-3, "factor 0.65: 7.8");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** At the default factor the fan has four lances of 3.9, not five of 6; the attack then rests 1/0.65 times as long. */
    @EnhancedTest(template = ARENA, batch = "abw_apocalypse_lances", timeoutTicks = 100)
    public static void lancesAreFewerAndWeaker(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.NONE);
        ApocalypseEntity boss = d.boss();
        d.f().config(ApocalypseBossConfig.POWER_FACTOR, 0.65);
        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.LANCES), "he begins the Lances");
            helper.runAfterDelay(ApocalypseAttack.LANCES.tellTicks() + 2, () -> {
                List<SandLanceEntity> lances = d.level().getEntitiesOfClass(SandLanceEntity.class, boss.getBoundingBox().inflate(60.0));
                check(lances.size() == 4, "four lances fly: " + lances.size());
                for (SandLanceEntity lance : lances) {
                    assertNear(lance.damage(), 3.9, 1.0E-3, "of 3.9 each");
                }
                long now = d.level().getGameTime();
                long rest = ApocalypseBossConfig.slower(ApocalypseAttack.LANCES.cooldown());
                check(rest == 92, "a cooldown of 60 ticks rests 92: " + rest);
                check(!boss.brain().isReady(ApocalypseAttack.LANCES, now + 60), "not ready 60 ticks later");
                check(boss.brain().isReady(ApocalypseAttack.LANCES, now + 100), "ready after the longer rest");
                check(ApocalypseBrain.gap(1) == 30, "the base gap table is unchanged");
                helper.succeed();
            });
        });
    }

    /** The number of Horsemen: min(ceil(max x factor), 1 + fighters), and their added damage scales too. */
    @EnhancedTest(template = ARENA, batch = "abw_apocalypse_horsemen")
    public static void fewerHorsemen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(ApocalypseBossConfig.POWER_FACTOR, 1.0);
        check(Horsemen.countFor(1) == 2 && Horsemen.countFor(3) == 4 && Horsemen.countFor(6) == 4, "3.1: 2, 4, 4");
        f.config(ApocalypseBossConfig.POWER_FACTOR, 0.65);
        check(Horsemen.countFor(1) == 2 && Horsemen.countFor(3) == 3 && Horsemen.countFor(6) == 3, "0.65: 2, 3, 3");
        f.config(ApocalypseBossConfig.HORSEMEN_MAX, 0);
        check(Horsemen.countFor(3) == 0, "a max of zero still means none");
        helper.succeed();
    }
}
