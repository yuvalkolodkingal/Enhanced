package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.RedHulkGestures;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.redhulk.Anger;
import com.kolodkingal.enhanced.redhulk.EnergyAbsorb;
import com.kolodkingal.enhanced.redhulk.HeatWave;
import com.kolodkingal.enhanced.redhulk.RedHulkAbilities;
import com.kolodkingal.enhanced.redhulk.RedHulkState;
import com.kolodkingal.enhanced.redhulk.RossForm;
import com.kolodkingal.enhanced.redhulk.SuperJump;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Red Hulk (3.8, lane L2; PLAN 4 and 14): the growth to 1.5 times after the awakening and its refusal in a two-high
 * tunnel (Ross stays Ross), the body (+12 attack, -15 % speed, 44 health), fire and lava harmless (Ross not), anger
 * from the damage taken and dealt and its cooling, the burst at 100 (three husks hurt and burning, anger spent), the
 * blocks it lights only with mobGriefing and burstSetsFire, the Ground Smash (8, launched 0.9), the Heat Wave, the Roar,
 * Grab &amp; Slam (5 + 5 + 7), the Super Jump's heights by the game's physics and its one fall-safe landing, the Bound,
 * the early burst refused below 50 with no cooldown, the Energy Absorb (fire, an explosion, lightning and a beam feed his
 * anger and do no harm; a husk's blow still hurts), the Transform both ways and Ross's refused keys, Ross at 100, the
 * custom hero's Ross, his preset, and the pure tables of his animation. Batches {@code red_hulk} and {@code
 * red_hulk_grief} (the game rule and the config, one test).
 */
public final class RedHulkTests {
    private static final String BATCH = "red_hulk";
    private static final String BATCH_GRIEF = "red_hulk_grief";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A Red Hulk at full size (his growth settled at once) and full health. */
    static ServerPlayer redHulk(TestKit.Fixture f, double x, double z) {
        ServerPlayer red = f.player(x, FLOOR, z, Power.RED_HULK);
        f.onClose(() -> HeroLifecycle.onLogout(red));
        red.doTick();
        RossForm.settle(red);
        red.setHealth(red.getMaxHealth());
        return red;
    }

    /** Ticks the players once a server tick until {@code done} (at most {@code max}), then runs {@code then}. */
    static void until(GameTestHelper helper, int max, BooleanSupplier done, Runnable then, ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, BooleanSupplier done, Runnable then,
            ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                p.doTick();
            }
            if (done.getAsBoolean()) {
                then.run();
            } else if (tick >= max) {
                throw new GameTestAssertException("not done within " + max + " ticks");
            } else {
                untilStep(helper, tick + 1, max, done, then, players);
            }
        });
    }

    /** Ticks the players for {@code n} ticks, then runs {@code then}. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    static void down(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
    }

    static void up(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    static double scale(ServerPlayer player) {
        return player.getAttributeValue(Attributes.SCALE);
    }

    static float anger(ServerPlayer player) {
        return Anger.of(player);
    }

    static void mobGriefing(TestKit.Fixture f, boolean on) {
        var level = f.level();
        boolean old = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        f.onClose(() -> level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old, level.getServer()));
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(on, level.getServer());
    }

    /** The fire blocks within the arena (relative y 2 to 4). */
    static int fires(GameTestHelper helper) {
        int n = 0;
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 2; y <= 4; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BaseFireBlock) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    static void putOut(GameTestHelper helper) {
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 2; y <= 4; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (helper.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
                        helper.setBlock(pos, Blocks.AIR);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body and the forms

    /** A new Red Hulk grows to 1.5 times over his change; then the body: +12 attack, -15 % speed, 44 health. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void heGrowsToOneAndAHalfWithHisBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = f.player(10.5, FLOOR, 10.5, Power.RED_HULK);
        f.onClose(() -> HeroLifecycle.onLogout(red));
        red.doTick();
        check(RossForm.changing(red) && data(red).is(Live.RED_HULK_TRANSFORMING), "he grows into his size");
        until(helper, 60, () -> !RossForm.changing(red), () -> {
            assertNear(scale(red), 1.5, 1.0E-6, "1.5 times");
            assertNear(red.getAttributeValue(Attributes.ATTACK_DAMAGE), 13.0, 1.0E-6, "Strength IV: +12");
            assertNear(red.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * 0.85, 1.0E-6, "Slowness I: -15 %");
            assertNear(red.getMaxHealth(), 44.0, 1.0E-6, "44 health");
            check(red.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.8 - 1.0E-6, "hard to push");
            check(!data(red).toggle(Toggle.ROSS_FORM), "he is Red Hulk");
            helper.succeed();
        }, red);
    }

    /** In a two-high tunnel there is no room to grow: the new Red Hulk stays Ross, N is refused, anger 100 waits. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void inATunnelRossStaysRoss(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(new BlockPos(10 + dx, 4, 10 + dz), Blocks.STONE);
            }
        }
        ServerPlayer red = f.player(10.5, FLOOR, 10.5, Power.RED_HULK);
        f.onClose(() -> HeroLifecycle.onLogout(red));
        red.doTick();
        check(data(red).toggle(Toggle.ROSS_FORM) && !RossForm.changing(red), "no room: he stays Ross");
        assertNear(scale(red), 1.0, 1.0E-6, "Ross's size");
        press(red, 5, false);
        check(data(red).toggle(Toggle.ROSS_FORM) && !RossForm.changing(red), "N is refused");
        Anger.setAnger(red, 100.0F, true);
        step(helper, 5, () -> {
            check(data(red).toggle(Toggle.ROSS_FORM) && anger(red) >= 99.0F, "his anger waits, full");
            assertNear(scale(red), 1.0, 1.0E-6, "never inside a block");
            helper.succeed();
        }, red);
    }

    /** N: Red Hulk shrinks into Ross (no body, keys refused) and grows back. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theTransformBothWaysAndRossKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        press(red, 5, false);
        check(RossForm.changing(red), "he shrinks");
        until(helper, 50, () -> !RossForm.changing(red), () -> {
            check(data(red).toggle(Toggle.ROSS_FORM), "he is Ross");
            assertNear(scale(red), 1.0, 1.0E-6, "Ross's size");
            assertNear(red.getMaxHealth(), 20.0, 1.0E-6, "Ross's health");
            press(red, 1, false);
            check(data(red).isReady(HeroAbility.RED_HULK_SMASH, red.level().getGameTime()), "R is refused to Ross");
            until(helper, 60, () -> data(red).isReady(HeroAbility.RED_HULK_TRANSFORM, red.level().getGameTime()),
                    () -> {
                        press(red, 5, false);
                        check(RossForm.changing(red) && !data(red).toggle(Toggle.ROSS_FORM), "he grows back");
                        until(helper, 50, () -> !RossForm.changing(red), () -> {
                            assertNear(scale(red), 1.5, 1.0E-6, "Red Hulk again");
                            helper.succeed();
                        }, red);
                    }, red);
        }, red);
    }

    /** Ross at anger 100 turns into Red Hulk. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void rossAtAHundredTurnsRed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        press(red, 5, false);
        RossForm.settle(red);
        check(data(red).toggle(Toggle.ROSS_FORM), "Ross");
        red.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        check(anger(red) > 0.0F && anger(red) < 6.0F, "Ross's anger rises at half the rate: " + anger(red));
        Anger.setAnger(red, 100.0F, true);
        step(helper, 3, () -> {
            check(RossForm.changing(red) && !data(red).toggle(Toggle.ROSS_FORM), "at 100 he turns into Red Hulk");
            helper.succeed();
        }, red);
    }

    /** Fire, lava and a magma floor do Red Hulk nothing; Ross burns as anyone. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void fireAndLavaDoNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        float health = red.getHealth();
        var sources = helper.getLevel().damageSources();
        red.hurt(sources.inFire(), 4.0F);
        red.invulnerableTime = 0;
        red.hurt(sources.lava(), 4.0F);
        red.invulnerableTime = 0;
        red.hurt(sources.hotFloor(), 4.0F);
        red.invulnerableTime = 0;
        red.hurt(sources.onFire(), 4.0F);
        assertNear(red.getHealth(), health, 1.0E-6, "fire-proof");
        press(red, 5, false);
        RossForm.settle(red);
        red.invulnerableTime = 0;
        red.hurt(sources.lava(), 4.0F);
        check(red.getHealth() < red.getMaxHealth(), "Ross is not");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Anger

    /** +3 per health taken, +1 per health dealt; it cools 4 a second after 100 ticks out of the fight. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void angerFillsAndCools(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        float before = red.getHealth();
        red.hurt(helper.getLevel().damageSources().generic(), 4.0F);
        float taken = before - red.getHealth();
        check(taken > 0.0F, "it hurt");
        assertNear(anger(red), taken * 3.0, 0.01, "3 per health taken");
        Anger.setAnger(red, 0.0F, true);
        Husk husk = husk(helper, 12.5, 10.5);
        husk.hurt(helper.getLevel().damageSources().playerAttack(red), 4.0F);
        assertNear(anger(red), lost(husk), 0.01, "1 per health dealt");
        Anger.setAnger(red, 50.0F, true);
        step(helper, 95, () -> {
            check(anger(red) >= 49.99F, "no cooling before 100 ticks: " + anger(red));
            step(helper, 40, () -> {
                check(anger(red) < 50.0F - 4.0F && anger(red) > 50.0F - 12.0F, "about 4 a second: " + anger(red));
                helper.succeed();
            }, red);
        }, red);
    }

    /** At 100 the burst: three husks within 6 hurt and burning, anger spent; the advancement. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void atAHundredTheBurst(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        List<Husk> husks = List.of(husk(helper, 12.5, 10.5), husk(helper, 10.5, 14.5), husk(helper, 6.5, 10.5));
        Husk far = husk(helper, 19.5, 19.5);
        Anger.setAnger(red, 100.0F, true);
        step(helper, 14, () -> {
            for (Husk h : husks) {
                check(lost(h) >= 5.0F - 1.0E-3 && lost(h) <= 10.0F + 1.0E-3, "the ring hurts: " + lost(h));
                check(h.getRemainingFireTicks() > 0, "and sets it burning");
            }
            check(lost(far) == 0.0F, "not beyond 6");
            check(anger(red) < 5.0F, "anger spent: " + anger(red));
            helper.succeed();
        }, red);
    }

    /**
     * The burst lights blocks only with the game rule mobGriefing and {@code burstSetsFire}: on and on → fire; the rule
     * off → none; the option off → none.
     */
    @EnhancedTest(template = ARENA, batch = BATCH_GRIEF, timeoutTicks = 200)
    public static void theBurstLightsBlocksOnlyWithMobGriefingAndTheOption(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        mobGriefing(f, true);
        f.config(RedHulkConfig.BURST_SETS_FIRE, true);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Anger.setAnger(red, 100.0F, true);
        step(helper, 12, () -> {
            check(fires(helper) > 0, "mobGriefing and the option: the ground burns");
            putOut(helper);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, helper.getLevel()
                    .getServer());
            Anger.setAnger(red, 100.0F, true);
            step(helper, 12, () -> {
                check(fires(helper) == 0, "no mobGriefing: no fire");
                helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel()
                        .getServer());
                RedHulkConfig.BURST_SETS_FIRE.set(false);
                Anger.setAnger(red, 100.0F, true);
                step(helper, 12, () -> {
                    check(fires(helper) == 0, "the option off: no fire");
                    helper.succeed();
                }, red);
            }, red);
        }, red);
    }

    /** B below 50 anger: "Not angry enough… yet." and no cooldown; from 50 the burst now, scaled. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theEarlyBurstNeedsFifty(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk husk = husk(helper, 12.5, 10.5);
        Anger.setAnger(red, 40.0F, true);
        press(red, 4, false);
        long now = red.level().getGameTime();
        check(data(red).isReady(HeroAbility.RED_HULK_BURST, now), "refused: no cooldown spent");
        check(anger(red) >= 39.9F, "the anger kept");
        Anger.setAnger(red, 60.0F, true);
        press(red, 4, false);
        check(!data(red).isReady(HeroAbility.RED_HULK_BURST, now), "from 50: the burst, and its cooldown");
        check(anger(red) < 1.0F, "anger spent");
        step(helper, 12, () -> {
            check(lost(husk) > 0.0F && lost(husk) <= 6.0F + 1.0E-3, "scaled by 60/100: " + lost(husk));
            helper.succeed();
        }, red);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The moves

    /** R: the Ground Smash hits a husk on the ground for 8 and launches it 0.9. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theSmashHitsForEightAndLaunches(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk husk = husk(helper, 13.5, 10.5);
        press(red, 1, false);
        until(helper, 20, () -> lost(husk) > 0.0F, () -> {
            assertNear(lost(husk), 8.0, 0.01, "8");
            check(husk.getDeltaMovement().y >= 0.9 - 1.0E-3, "launched 0.9: " + husk.getDeltaMovement().y);
            check(!data(red).isReady(HeroAbility.RED_HULK_SMASH, red.level().getGameTime()), "cooldown");
            helper.succeed();
        }, red);
    }

    /** Sneak + R: Grab &amp; Slam, 5 + 5 + 7. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void grabAndSlam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk husk = husk(helper, 12.5, 10.5);
        lookAt(red, husk);
        press(red, 1, true);
        check(HulkAbilities.moving(red), "the grab");
        step(helper, 1, () -> {
            check(data(red).is(Live.RED_HULK_GRABBING), "his state");
            until(helper, 60, () -> !HulkAbilities.moving(red), () -> {
                assertNear(lost(husk), 17.0, 0.01, "5 + 5 + 7");
                helper.succeed();
            }, red);
        }, red);
    }

    /** G: the Heat Wave burns the husk in front (5, burning) and not the one behind. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHeatWaveBurnsWhatIsInFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk front = husk(helper, 15.5, 10.5);
        Husk behind = husk(helper, 5.5, 10.5);
        lookAt(red, front);
        List<LivingEntity> burned = HeatWave.wave(red, data(red));
        check(burned.contains(front) && !burned.contains(behind), "the cone");
        assertNear(lost(front), 5.0, 0.01, "5");
        check(front.getRemainingFireTicks() > 0 && lost(behind) == 0.0F, "burning in front, nothing behind");
        helper.succeed();
    }

    /** Sneak + G: the Roar frightens a husk and adds 10 anger. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRoarFrightens(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk husk = husk(helper, 14.5, 10.5);
        press(red, 2, true);
        check(husk.hasEffect(ModEffects.FRIGHTENED), "frightened");
        assertNear(anger(red), 10.0, 0.01, "+10 anger");
        helper.succeed();
    }

    /** The Super Jump's heights by the game's physics: no charge 8, a full one 30. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theSuperJumpHeightsFollowTheCharge(GameTestHelper helper) {
        assertNear(SuperJump.height(SuperJump.charge(0)), 8.0, 1.0E-9, "no charge: 8");
        assertNear(SuperJump.height(SuperJump.charge(30)), 30.0, 1.0E-9, "a full charge: 30");
        assertNear(SuperJump.height(SuperJump.charge(15)), 19.0, 1.0E-9, "half: 19");
        assertNear(SuperJump.height(SuperJump.charge(90)), 30.0, 1.0E-9, "never more");
        for (double h : new double[]{8.0, 19.0, 30.0}) {
            assertNear(SuperJump.apex(SuperJump.launchSpeed(h)), h, 0.05, "the launch rises " + h);
        }
        check(SuperJump.launchSpeed(30.0) > SuperJump.launchSpeed(8.0), "faster for higher");
        helper.succeed();
    }

    /** V held then released: the launch at the charge's speed, the next fall damage cancelled once. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theJumpLaunchesAndItsLandingIsSafeOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        down(red, 3, false);
        step(helper, 35, () -> {
            check(data(red).is(Live.RED_HULK_JUMP_CHARGING), "charging");
            up(red, 3);
            check(red.getDeltaMovement().y >= SuperJump.launchSpeed(29.0), "a full launch: "
                    + red.getDeltaMovement().y);
            check(Anger.get(red).has(RedHulkState.FALL_SAFE), "his landing is guarded");
            float health = red.getHealth();
            red.hurt(helper.getLevel().damageSources().fall(), 10.0F);
            assertNear(red.getHealth(), health, 1.0E-6, "the landing does no harm");
            check(!Anger.get(red).has(RedHulkState.FALL_SAFE), "once");
            red.invulnerableTime = 0;
            red.hurt(helper.getLevel().damageSources().fall(), 10.0F);
            check(red.getHealth() < health, "a second fall hurts");
            helper.succeed();
        }, red);
    }

    /** Sneak + V: the Bound flies him (the Hulk's), and it waits its cooldown. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theBound(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        press(red, 3, true);
        check(HulkAbilities.moving(red), "the Bound flies");
        check(!data(red).isReady(HeroAbility.RED_HULK_BOUND, red.level().getGameTime()), "its cooldown");
        step(helper, 1, () -> {
            check(data(red).is(Live.RED_HULK_LEAPING), "leaping");
            helper.succeed();
        }, red);
    }

    /** Sneak + B held: fire, an explosion, lightning and a beam feed his anger and do no harm; a husk's blow hurts. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theAbsorbSoaksEnergyNotBlows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer red = redHulk(f, 10.5, 10.5);
        Husk husk = husk(helper, 12.5, 10.5);
        down(red, 4, true);
        check(EnergyAbsorb.active(red) && data(red).is(Live.RED_HULK_ABSORBING), "absorbing");
        float health = red.getHealth();
        var level = helper.getLevel();
        var sources = level.damageSources();
        red.hurt(sources.inFire(), 4.0F);
        check(anger(red) >= 8.0F - 0.01F, "fire feeds him: " + anger(red));
        red.invulnerableTime = 0;
        red.hurt(sources.explosion(null, null), 5.0F);
        red.invulnerableTime = 0;
        red.hurt(sources.lightningBolt(), 5.0F);
        red.invulnerableTime = 0;
        red.hurt(ModDamageTypes.source(level, ModDamageTypes.UNIBEAM, husk, husk), 5.0F);
        assertNear(red.getHealth(), health, 1.0E-6, "no harm");
        assertNear(anger(red), 8.0 + 30.0, 0.05, "2 per health: " + anger(red));
        red.invulnerableTime = 0;
        red.hurt(sources.mobAttack(husk), 5.0F);
        check(red.getHealth() < health, "a blow still hurts");
        up(red, 4);
        check(!EnergyAbsorb.active(red), "let go");
        check(!data(red).isReady(HeroAbility.RED_HULK_ABSORB, red.level().getGameTime()), "its cooldown");
        helper.succeed();
    }

    /** A custom hero with red_hulk.transform: N is his Human Form (every row off but N), and back. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCustomHerosRossIsHisHumanForm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = CustomKitSupport.hero(f, 10.5, 10.5, CustomKitSupport.def().pick("red_hulk.smash", 3)
                .key(1, "red_hulk.smash").pick("red_hulk.transform", 3).key(5, "red_hulk.transform").build());
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        PowerData data = data(hero);
        check(data.has(HeroAbility.RED_HULK_SMASH), "his row");
        CustomKitSupport.tap(hero, 5, false);
        check(data.toggle(Toggle.HUMAN_FORM) && data.toggle(Toggle.ROSS_FORM), "Ross: his Human Form");
        check(!data.has(HeroAbility.RED_HULK_SMASH) && data.has(HeroAbility.RED_HULK_TRANSFORM), "only N stays");
        long now = hero.level().getGameTime();
        data.startCooldown(HeroAbility.RED_HULK_TRANSFORM, now - 1000, 1);
        CustomKitSupport.tap(hero, 5, false);
        check(!data.toggle(Toggle.HUMAN_FORM) && data.has(HeroAbility.RED_HULK_SMASH), "N again: his powers back");
        helper.succeed();
    }

    /** His preset reads, is his hero's, and fits 60 points with no note. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPresetFitsSixtyPoints(GameTestHelper helper) throws java.io.IOException {
        java.nio.file.Path file = net.neoforged.fml.ModList.get().getModFileById(Enhanced.MODID).getFile()
                .findResource("assets", Enhanced.MODID, "hero_presets", "red_hulk.json");
        String json = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        HeroPresets.Preset preset = HeroPresets.read("red_hulk", json);
        check(preset != null && preset.like() == Power.RED_HULK, "the preset reads, like Red Hulk");
        CustomHeroRules.Result r = CustomHeroRules.sanitize(preset.hero(), CustomHeroRules.Limits.DEFAULT
                .withBudget(HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        check(CustomHeroRules.cost(r.hero()) <= HeroPresets.MAX_POINTS, "60 points at most: "
                + CustomHeroRules.cost(r.hero()));
        helper.succeed();
    }

    /** The pure tables: his gestures have keys, the routes, the rows, his run. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisAnimationTables(GameTestHelper helper) {
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("RED_HULK_")) {
                Gestures.Def def = Gestures.get(id);
                check(def != null && def.problems().isEmpty(), id + " has keys");
                check(GestureRoutes.startedElsewhere(id), id + " is started");
            }
        }
        check(GestureRoutes.toggle(Power.RED_HULK, Toggle.ROSS_FORM, true) == GestureId.RED_HULK_ROSS,
                "Ross straightens his suit");
        check(RedHulkGestures.gestureOf(RedHulkFxPayload.BURST, 6) == GestureId.RED_HULK_BURST
                && RedHulkGestures.gestureOf(RedHulkFxPayload.SMASH, 0) == GestureId.HULK_SMASH
                && RedHulkGestures.gestureOf(RedHulkFxPayload.SMASH, 6) == null, "the payloads' gestures");
        for (Live live : List.of(Live.RED_HULK_TRANSFORMING, Live.RED_HULK_JUMP_CHARGING, Live.RED_HULK_LEAPING,
                Live.RED_HULK_HEATED, Live.RED_HULK_ABSORBING, Live.RED_HULK_GRABBING)) {
            check(GestureRoutes.live(Power.RED_HULK, live, true) != null || GestureRoutes.reason(live) != null,
                    live + " is acted or explained");
        }
        check(HeroAbilityAnims.shown(HeroAbility.RED_HULK_BURST, true) == GestureId.RED_HULK_ABSORB, "the variant");
        check(RunCharacter.of(Power.RED_HULK, false) == RunCharacter.RED_HULK, "his run");
        check(RedHulkAbilities.RED == Power.RED_HULK.color, "his red");
        helper.succeed();
    }
}
