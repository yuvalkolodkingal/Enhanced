package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.SpeedStrikes;
import com.kolodkingal.enhanced.config.SpeedConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The speedsters of 3.3: slow time and running as two abilities (H3: Tommy's new slow time; Quicksilver's are in
 * {@link QuicksilverTests}), and the speed strikes (H4): the Dash Strike (both), Quicksilver's Pebble Barrage and
 * Tommy's Sonic Boom, as modules ({@link SpeedStrikes}) and on their keys.
 */
public final class SpeedStrikeTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    private static void pressSneaking(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    /** A zombie (a Husk: no burning in the sun) with no armour and plenty of health, so the numbers are as written. */
    private static Zombie target(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        zombie.setHealth(100.0F);
        return zombie;
    }

    // ------------------------------------------------------------------------------------------------------------
    // H3: Tommy's slow time

    @EnhancedTest(template = ARENA, batch = "speed_tommy_time", timeoutTicks = 100)
    public static void tommysSlowTimeIsAKeyOfItsOwn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.SLOW_MOTION_RADIUS, 5);
        ServerPlayer tommy = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        PowerData data = Powers.get(tommy);
        check(!data.toggle(Toggle.TOMMY_TIME) && data.toggle(Toggle.SUPER_SPEED), "slow time starts off, Super Speed on");

        press(tommy, 1);
        PowerTicker.tick(tommy);
        SlowMotion.clear();
        check(data.toggle(Toggle.TOMMY_TIME) && data.slowMo(), "R turns his slow time on");
        check(SlowMotion.isInZone(helper.getLevel(), tommy.position()) && SlowMotion.zoneOwner(helper.getLevel(), tommy.position()) == Power.TOMMY,
                "it slows the world around him, and the zone is his (its tint is teal)");
        check(data.toggle(Toggle.SUPER_SPEED), "his Super Speed is left as it was");

        // Together: he runs in his own slowed time; and the running costs the run's price, not both.
        float before = tommy.getFoodData().getExhaustionLevel();
        tommy.setSprinting(true);
        for (int i = 0; i < 10; i++) {
            PowerTicker.tick(tommy);
        }
        check(data.is(Live.SPEEDING) && data.slowMo(), "running and slow time together");
        double run = EnhancedConfig.TOMMY_EXHAUSTION.get() * 10;
        check(Math.abs(tommy.getFoodData().getExhaustionLevel() - before - run) < 0.005, "running costs the run's price only");
        tommy.setSprinting(false);
        before = tommy.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 10; i++) {
            PowerTicker.tick(tommy);
        }
        double idle = SpeedConfig.TOMMY_TIME_EXHAUSTION.get() * 10;
        check(Math.abs(tommy.getFoodData().getExhaustionLevel() - before - idle) < 0.005, "standing in his slow time costs its own price");

        // Alone: G turns the running off, slow time stays.
        press(tommy, 2);
        tommy.setSprinting(true);
        PowerTicker.tick(tommy);
        check(!data.toggle(Toggle.SUPER_SPEED) && !data.is(Live.SPEEDING) && data.slowMo(), "Super Speed off, his slow time on");

        // Starving ends it.
        tommy.getFoodData().setFoodLevel(0);
        PowerTicker.tick(tommy);
        check(!data.toggle(Toggle.TOMMY_TIME) && !data.slowMo(), "starving ends his slow time");
        press(tommy, 1);
        check(!data.toggle(Toggle.TOMMY_TIME), "and he can't start it hungry");

        // Losing the power ends it.
        tommy.getFoodData().setFoodLevel(20);
        press(tommy, 1);
        PowerTicker.tick(tommy);
        check(data.slowMo(), "on again");
        Powers.setPower(tommy, Power.NONE);
        PowerTicker.tick(tommy);
        check(!Powers.get(tommy).slowMo(), "a Tommy who is no longer Tommy slows nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // H4: the Dash Strike

    @EnhancedTest(template = ARENA, batch = "speed_dash")
    public static void theDashStrikeHitsWhatIsInItsPathAndStopsAtAWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(3.5, FLOOR, 10.5, Power.QUICKSILVER);
        look(peter, EAST, 0.0F);
        Zombie inPath = target(helper, 7.5, 10.5);
        Zombie aside = target(helper, 7.5, 16.5);
        // A wall at x 12: the dash ends before it.
        for (int y = 2; y <= 4; y++) {
            for (int z = 8; z <= 13; z++) {
                helper.setBlock(new BlockPos(12, y, z), Blocks.STONE);
            }
        }
        pressSneaking(peter, 2);
        Vec3 at = helper.relativeVec(peter.position());
        check(at.x > 10.5 && at.x <= 11.7 + 1.0E-6, "Sneak + G dashes him along his look, up to the wall: " + at);
        check(helper.getLevel().noCollision(peter, peter.getBoundingBox().deflate(1.0E-4)), "and never into it");
        check(inPath.getHealth() <= 100.0F - SpeedConfig.DASH_DAMAGE.get().floatValue() + 1.0E-3F, "the zombie in his path is struck: " + inPath.getHealth());
        check(aside.getHealth() == 100.0F, "the one off the path is not");
        check(!Powers.get(peter).isReady(2, helper.getLevel().getGameTime()), "the dash cools down on G's line");
        check(Powers.get(peter).superSpeed() == false, "and G's Super Speed was not switched by it");

        // Face to the wall: no room, no dash, no cooldown.
        ServerPlayer tommy = f.player(11.2, FLOOR, 9.5, Power.TOMMY);
        look(tommy, EAST, 0.0F);
        Vec3 before = tommy.position();
        pressSneaking(tommy, 2);
        check(tommy.position().equals(before) && Powers.get(tommy).isReady(2, helper.getLevel().getGameTime()), "no room: nothing happens");
        check(Powers.get(tommy).toggle(Toggle.SUPER_SPEED), "Tommy's Super Speed is still on (Sneak + G is the dash, not the toggle)");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "speed_dash_pvp")
    public static void theStrikesSparePlayersWithPvpOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(3.5, FLOOR, 10.5, Power.QUICKSILVER);
        ServerPlayer friend = f.player(7.5, FLOOR, 10.5);
        look(peter, EAST, 0.0F);
        float health = friend.getHealth();
        pressSneaking(peter, 2);
        check(friend.getHealth() == health, "PvP is off: the dash does not hurt a player");
        ServerPlayer tommy = f.player(10.5, FLOOR, 4.5, Power.TOMMY);
        ServerPlayer bystander = f.player(12.5, FLOOR, 4.5);
        pressSneaking(tommy, 3);
        check(bystander.getHealth() == bystander.getMaxHealth(), "nor does the Sonic Boom");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // H4: the Pebble Barrage

    @EnhancedTest(template = ARENA, batch = "speed_barrage")
    public static void thePebbleBarrageRiddlesWhatHeLooksAt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(3.5, FLOOR, 10.5, Power.QUICKSILVER);
        Zombie zombie = target(helper, 10.5, 10.5);
        lookAt(peter, zombie);
        press(peter, 5);
        float taken = 100.0F - zombie.getHealth();
        int pebbles = SpeedConfig.BARRAGE_PEBBLES.get();
        float each = SpeedConfig.BARRAGE_DAMAGE.get().floatValue();
        check(taken >= each * pebbles * 0.5F, "N: most of the " + pebbles + " pebbles hit the zombie 7 blocks away (" + taken + " damage)");
        check(taken <= each * pebbles + 1.0E-3F, "and no more than all of them");
        check(!Powers.get(peter).isReady(5, helper.getLevel().getGameTime()), "the barrage cools down");
        press(peter, 5);
        check(100.0F - zombie.getHealth() == taken, "and can't be thrown again at once");

        // The pattern is the same every time: a second Quicksilver gets the same hits.
        Zombie other = target(helper, 10.5, 4.5);
        ServerPlayer second = f.player(3.5, FLOOR, 4.5, Power.QUICKSILVER);
        lookAt(second, other);
        int hits = SpeedStrikes.barrage(second, SpeedStrikes.Style.of(Power.QUICKSILVER), pebbles, each, 24);
        check(hits >= pebbles / 2 && Math.abs((100.0F - other.getHealth()) - hits * each) < 1.0E-3F, "the module says how many hit: " + hits);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // H4: the Sonic Boom

    @EnhancedTest(template = ARENA, batch = "speed_boom", timeoutTicks = 40)
    public static void theSonicBoomThrowsEverythingAround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        Zombie near = target(helper, 12.5, 10.5);
        Zombie edge = target(helper, 10.5, 15.0);
        Zombie far = target(helper, 10.5, 19.5);
        double radius = SpeedConfig.BOOM_RADIUS.get();
        check(radius < 9.0, "sanity: the far zombie is out of it");
        pressSneaking(tommy, 3);
        PowerData data = Powers.get(tommy);
        check(data.is(Live.SONIC_BOOM), "Sneak + V: the boom, and every client sees it");
        check(near.getHealth() < 100.0F && edge.getHealth() < 100.0F, "the zombies within it are hurt");
        check(100.0F - near.getHealth() > 100.0F - edge.getHealth(), "the nearer the harder");
        check(far.getHealth() == 100.0F, "the far one is not");
        check(near.getDeltaMovement().x > 0.2, "and the near one is thrown away from him: " + near.getDeltaMovement());
        check(!data.isReady(3, helper.getLevel().getGameTime()), "it cools down on V's line");
        press(tommy, 3);
        check(!data.is(Live.WHIRLWIND), "the whirlwind waits for the same cooldown");
        f.everyTick(SpeedStrikes.BOOM_FLAG_TICKS + 3, tick -> {
            PowerTicker.tick(tommy);
            return tick > SpeedStrikes.BOOM_FLAG_TICKS && !data.is(Live.SONIC_BOOM);
        });
    }

    private SpeedStrikeTests() {
    }
}
