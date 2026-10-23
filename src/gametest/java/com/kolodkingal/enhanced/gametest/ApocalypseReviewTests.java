package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.ApocalypseAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Log;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.duel;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the adversarial review of the Apocalypse lane found and fixed: the end of the arena reaches a player who went out of
 * range, an operator in Creative does not make the fight bigger, a Sandstorm slows a lance as it slows an arrow, Quicksilver
 * Time slows his tells and his Horsemen's tricks (not only their walk) until he adapts, and a tornado keeps to the arena.
 * Each has a batch of its own, as every test of server-wide state does.
 */
public final class ApocalypseReviewTests {

    @EnhancedTest(template = ARENA, batch = "apocalypse_review_off", timeoutTicks = 100)
    public static void theEndOfTheArenaIsToldToEveryoneInTheDimension(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer near = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ServerPlayer far = f.player(4.5, FLOOR, 4.5);
        // Gone far out of range while the fight lasted: farther than the 96 blocks an effect is sent to, and than the wall's reach.
        Vec3 away = far.position().add(600.0, 0.0, 0.0);
        far.setNoGravity(true);
        far.moveTo(away.x, away.y, away.z, 0.0F, 0.0F);
        Log nearLog = new Log(near);
        Log farLog = new Log(far);
        farLog.drain().clear();
        ApocalypseFight fight = ApocalypseTestSupport.start(f, near);
        helper.runAfterDelay(3, () -> {
            nearLog.drain();
            farLog.drain();
            check(!nearLog.effects(ApocalypseFxPayload.ARENA_ON).isEmpty(), "the one near was told the arena stands");
            check(farLog.effects(ApocalypseFxPayload.ARENA_ON).isEmpty(), "the one far away was not (he will be if he comes into view)");
            nearLog.clear();
            farLog.clear();
            BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
            check(fight.isFinished(), "the fight is let go");
            nearLog.drain();
            farLog.drain();
            check(!nearLog.effects(ApocalypseFxPayload.ARENA_OFF).isEmpty(), "the one near is told it is over");
            check(!farLog.effects(ApocalypseFxPayload.ARENA_OFF).isEmpty(), "and so is the one who went far away: he keeps no wall of a fight that is gone");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_review_count", timeoutTicks = 100)
    public static void aCreativePlayerIsInTheLedgerButNotInTheNumbers(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerPlayer starter = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ApocalypseFight fight = ApocalypseTestSupport.start(f, starter);
        ServerPlayer first = f.player(14.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer second = f.player(6.5, FLOOR, 10.5, GameType.CREATIVE);
        f.player(10.5, FLOOR, 14.5); // a friend who fights
        helper.runAfterDelay(4, () -> {
            check(fight.isParticipant(first.getUUID()) && fight.isParticipant(second.getUUID()) && fight.participants().size() == 4,
                    "everyone who came near is in the ledger: " + fight.participants().size());
            check(fight.fighterCount() == 2, "but two fight: " + fight.fighterCount());
            fight.forceState(ApocalypseFight.Stage.FIGHT, 1);
            ApocalypseEntity boss = ApocalypseTestSupport.boss(fight);
            assertNear(boss.designMax(), ApocalypseEntity.designHealthFor(2), 1.0E-3, "his health is that of two");
            assertNear(boss.shield().max(), ApocalypseShield.strengthFor(1, 2), 1.0E-3, "and so is his shield");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_review_storm", timeoutTicks = 100)
    public static void aSandstormSlowsALanceToHalfAndItStaysSlow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5, Power.APOCALYPSE);
        hero.setHealth(hero.getMaxHealth());
        ServerPlayer shooter = f.player(3.5, FLOOR, 3.5); // a lance a player shot is a player's own: the orphan sweep leaves it alone
        hero.setShiftKeyDown(true);
        AbilityHandler.onKey(hero, 5, true);
        hero.setShiftKeyDown(false);
        check(Powers.get(hero).is(Live.SANDSTORM), "the storm is up");
        SandLanceEntity inside = lance(level, shooter, helper.absoluteVec(new Vec3(12.5, FLOOR + 1.0, 10.5)));
        SandLanceEntity outside = lance(level, shooter, helper.absoluteVec(new Vec3(19.5, FLOOR + 1.0, 10.5)));
        PowerTicker.tick(hero);
        check(ModAttachments.PERSISTENT_DATA.get(inside).getBoolean(ApocalypseAbilities.SLOWED_TAG),
                "the lance in the storm is marked slowed");
        check(!ModAttachments.PERSISTENT_DATA.get(outside).getBoolean(ApocalypseAbilities.SLOWED_TAG),
                "the one beyond it is not");
        every(helper, 4, tick -> {
            if (tick == 3) {
                // The lance flies at its own speed every tick (the air takes nothing from it): the slowing must survive that.
                assertNear(inside.getDeltaMovement().length(), 0.95, 0.05, "it flies at half its speed, tick after tick");
                assertNear(outside.getDeltaMovement().length(), 1.9, 0.05, "the one beyond the storm at its full speed");
                return true;
            }
            return false;
        });
    }

    private static SandLanceEntity lance(ServerLevel level, ServerPlayer owner, Vec3 at) {
        SandLanceEntity lance = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
        lance.launch(owner, at, new Vec3(0.0, 1.0, 0.0), 1.9F, 6.0F);
        level.addFreshEntity(lance);
        return lance;
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_review_slow", timeoutTicks = 150)
    public static void quicksilverTimeSlowsHisTellsAndTheTricksOfHisHorsemen(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.QUICKSILVER);
        ServerPlayer speedster = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        AbilityHandler.onKey(speedster, 1, true);
        AbilityHandler.onKey(speedster, 1, false);
        PowerTicker.tick(speedster);
        check(Powers.get(speedster).slowMo(), "Quicksilver Time is on");
        SlowMotion.clear(); // (the zones are cached for the tick, and were cached before the time was on)
        Husk war = d.fight().horsemen().spawn(level, Horsemen.Kind.WAR, helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5)));
        war.setNoAi(true);
        Set<UUID> volley = new HashSet<>();
        every(helper, 140, tick -> {
            PowerTicker.tick(speedster); // (the world is slowed for as long as he keeps the time)
            for (IronShrapnelEntity shard : shards(level, war)) {
                volley.add(shard.getUUID());
            }
            if (tick == 3) {
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.LANCES), "a Lance attack begins");
            }
            if (tick == 33) {
                // Thirty ticks on, a tell of 14 ticks would long have fired; slowed fivefold it is at its sixth.
                check(boss.brain().attack() == ApocalypseAttack.LANCES, "his tell is still going, slowed with the rest: " + boss.brain().attack());
                int ticks = boss.brain().attackTicks();
                check(ticks >= 4 && ticks <= 8, "at about a fifth of the pace: " + ticks);
            }
            if (tick == 75) {
                check(volley.isEmpty(), "the War Horseman's volley is not fired yet: its tell is slowed as well");
            }
            if (tick == 135) {
                check(volley.size() == 3, "but it comes, three shards: " + volley.size());
                return true;
            }
            return false;
        });
    }

    private static List<IronShrapnelEntity> shards(ServerLevel level, Husk husk) {
        return level.getEntitiesOfClass(IronShrapnelEntity.class, new AABB(husk.blockPosition()).inflate(40.0), shard -> shard.getOwner() == husk);
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_review_tornado", timeoutTicks = 120)
    public static void aTornadoFollowsAVictimOnlyAsFarAsTheArena(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ServerLevel level = d.level();
        put(d.f(), d.hero(), 0.5, FLOOR, 0.5);
        ServerPlayer onlooker = d.f().player(19.5, FLOOR, 10.5); // a bystander beyond the arena's edge, nearer to the tornado than the hero is
        onlooker.setNoGravity(true);
        // (The arena of this test is a circle of 4 blocks round (5.5, 10.5); the floor is too small for the real one.)
        Vec3 centre = helper.absoluteVec(new Vec3(5.5, FLOOR, 10.5));
        Vec3 start = helper.absoluteVec(new Vec3(8.5, FLOOR, 10.5));
        double bound = 4.0;
        SandHazards.Tornado free = SandHazards.tornado(level, d.boss(), d.fight().id(), start, 3.2, 160, 0.0F);
        SandHazards.Tornado kept = SandHazards.tornado(level, d.boss(), d.fight().id(), start, 3.2, 160, 0.0F, centre, bound);
        every(helper, 80, tick -> {
            if (tick == 79) {
                double freeReach = Math.hypot(free.position().x - centre.x, free.position().z - centre.z);
                double keptReach = Math.hypot(kept.position().x - centre.x, kept.position().z - centre.z);
                check(freeReach > bound + 2.0, "a tornado with no bound follows him out of the arena: " + freeReach);
                check(keptReach <= bound + 1.0E-6 && keptReach > 3.0, "one of the fight stops at the wall (" + bound + "): " + keptReach);
                return true;
            }
            return false;
        });
    }

    private ApocalypseReviewTests() {
    }
}
