package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the tests of the Mad Titan's moves share: a fixture on Normal difficulty, fighters who survive a long fight
 * (a thousand health, so a test reads the damage as the health they lost), and the arithmetic of what a blow should do.
 * Thanos stands in the middle of the floor ({@link ThanosBossTests#CENTRE}), passive and arrived.
 */
final class ThanosMoveSupport {
    static final float MAX = 1000.0F;

    /** A boss fixture: the small arena, the announcements off, Normal difficulty (damage to players is not scaled). */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = ThanosBossTests.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty difficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.NORMAL, true);
        f.onClose(() -> server.setDifficulty(difficulty, true));
        // A fighter who is ticked heals a little every half second: a test reads the damage as the health he lost.
        GameRules.BooleanValue regeneration = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        boolean regenerates = regeneration.get();
        regeneration.set(false, server);
        f.onClose(() -> regeneration.set(regenerates, server));
        return f;
    }

    /** A fighter with a thousand health, {@code dx} east and {@code dz} south of the middle of the floor. */
    static ServerPlayer fighter(TestKit.Fixture f, double dx, double dz) {
        return fighter(f, dx, dz, Power.NONE);
    }

    static ServerPlayer fighter(TestKit.Fixture f, double dx, double dz, Power power) {
        ServerPlayer player = f.player(ThanosBossTests.CENTRE.x + dx, FLOOR, ThanosBossTests.CENTRE.z + dz, power);
        return tough(player);
    }

    static ServerPlayer tough(ServerPlayer player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX);
        player.setHealth(MAX);
        return player;
    }

    /** How much health the creature has lost since it was made tough. */
    static float lost(LivingEntity creature) {
        return creature instanceof ServerPlayer ? MAX - creature.getHealth() : creature.getMaxHealth() - creature.getHealth();
    }

    /** What a blow of {@code base} damage does at the moment: the number times his damage scale. */
    static float blow(ThanosEntity boss, double base) {
        return (float) (base * boss.damageScale());
    }

    /** Checks a number against another within a tolerance of one half. */
    static void near(float actual, float expected, String what) {
        TestKit.assertNear(actual, expected, 0.6, what);
    }

    /** A point of the arena, in the world, from its test coordinates. */
    static Vec3 at(TestKit.Fixture f, double x, double y, double z) {
        return f.helper().absoluteVec(new Vec3(x, y, z));
    }

    /** A point in the middle of the floor, {@code dx} east and {@code dz} south of it, at the height {@code y} of the floor. */
    static Vec3 spot(TestKit.Fixture f, double dx, double y, double dz) {
        return at(f, ThanosBossTests.CENTRE.x + dx, y, ThanosBossTests.CENTRE.z + dz);
    }

    static void sure(boolean condition, String message) {
        check(condition, message);
    }

    private ThanosMoveSupport() {
    }
}
