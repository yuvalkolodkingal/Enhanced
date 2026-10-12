package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
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
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseReviewTests {

    @GameTest(template = ARENA, batch = "apocalypse_review_off", timeoutTicks = 100)
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

    @GameTest(template = ARENA, batch = "apocalypse_review_count", timeoutTicks = 100)
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

    @GameTest(template = ARENA, batch = "apocalypse_review_tornado", timeoutTicks = 120)
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
