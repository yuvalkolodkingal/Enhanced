package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.JimFireballs;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * Jim Hammond (3.17): the Source Code makes him (and only for a player who is not Vision), his six abilities (the Fire Beam
 * and its magma, fire immunity, the durable body that heals fast, fireballs, the Nova Flame, the burning body), his looks, and
 * his place in the Hero Creator. Server-safe: no client class is loaded.
 */
public final class JimTests {
    /** {@code ticks} server ticks of his powers (nothing ticks a player without a client), then {@code after}. */
    private static void run(GameTestHelper helper, ServerPlayer player, int ticks, Runnable after) {
        for (int i = 1; i <= ticks; i++) {
            helper.runAfterDelay(i, () -> {
                player.tickCount++;
                PowerTicker.tick(player);
            });
        }
        helper.runAfterDelay(ticks + 1, after);
    }

    private static ServerPlayer jim(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.JIM_HAMMOND);
    }

    // ---- how you become him ----

    @EnhancedTest(template = ARENA)
    public static void theSourceCodeMakesAPlayerWithNoHeroJimAndIsUsedUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.JIM_HAMMOND), "he has become Jim Hammond");
        check(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the item is used up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void theSourceCodeMakesAnotherHeroJimToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5, Power.MAGNETO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.JIM_HAMMOND), "Magneto is replaced");
        check(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the item is used up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aServerCanKeepItFromReplacingAHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.SOURCE_CODE_REPLACES_HERO, false);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5, Power.MAGNETO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.MAGNETO), "he stays Magneto");
        check(!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "and keeps the item");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void visionsOwnUseIsUnchanged(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(8.5, FLOOR, 8.5, Power.VISION);
        vision.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOURCE_CODE.get()));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check(Powers.is(vision, Power.VISION) && !vision.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "red Vision is still Vision and is not given Jim");
        Powers.setVariant(vision, Variants.get(Power.VISION, "white_vision"));
        ModItems.SOURCE_CODE.get().use(vision.level(), vision, InteractionHand.MAIN_HAND);
        check(Powers.is(vision, Power.VISION) && "source_code".equals(Powers.get(vision).variant()), "White Vision becomes Source Code Vision");
        helper.succeed();
    }

    // ---- the Fire Beam (continuous) ----

    /** A Jim whose solar charge does not refill during the test (the config is global: the tests that use this have a batch of their own). */
    private static ServerPlayer jimWithNoSun(TestKit.Fixture f, double x, double z) {
        f.config(JimConfig.SOLAR_FILL_SUN, 0);
        f.config(JimConfig.SOLAR_FILL_SHADE, 0);
        return jim(f, x, z);
    }

    @EnhancedTest(template = ARENA, batch = "jim_beam_drain")
    public static void theFireBeamIsContinuousHurtsOverTimeAndDrainsTheSolarCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jimWithNoSun(f, 3.5, 10.5);
        helper.setBlock(new BlockPos(10, (int) FLOOR + 1, 10), Blocks.OAK_PLANKS);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        look(jim, TestKit.EAST, 0.0F);
        int before = solar(jim);
        AbilityHandler.onKey(jim, 1, true);
        check(Powers.get(jim).is(Live.BEAM_FIRING), "R held: the beam burns at once (no charge-up)");
        run(helper, jim, 24, () -> {
            check(Powers.get(jim).is(Live.BEAM_FIRING), "and still burns while held");
            float lost = zombie.getMaxHealth() - zombie.getHealth();
            check(lost >= 1.5 * JimConfig.BEAM_DAMAGE.get() || !zombie.isAlive(), "it hurt the zombie more than once (over time): " + lost);
            check(zombie.getRemainingFireTicks() > 0 || !zombie.isAlive(), "and set it alight");
            check(before - solar(jim) == JimConfig.BEAM_COST_PER_SECOND.get() * 24 / 20,
                    "it drained the solar charge by the cost per second: " + (before - solar(jim)));
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, (int) FLOOR + 1, 10))).is(Blocks.FIRE),
                    "it burns the ground where it strikes: " + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, (int) FLOOR + 1, 10))));
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))).is(Blocks.OAK_PLANKS),
                    "and never makes magma or takes a block");
            AbilityHandler.onKey(jim, 1, false);
            check(!Powers.get(jim).is(Live.BEAM_FIRING), "let go: it stops");
            check(Powers.get(jim).cooldownUntil(Powers.get(jim).slot(HeroAbility.JIM_BEAM)) > helper.getLevel().getGameTime(), "and a short cooldown runs");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "jim_beam_empty")
    public static void theFireBeamStopsWhenTheSolarChargeRunsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jimWithNoSun(f, 3.5, 10.5);
        look(jim, TestKit.EAST, 0.0F);
        Powers.get(jim).setIronCharge(JimConfig.BEAM_MIN_CHARGE.get() + 30);
        AbilityHandler.onKey(jim, 1, true);
        check(Powers.get(jim).is(Live.BEAM_FIRING), "with enough to begin it burns");
        run(helper, jim, 30, () -> {
            check(!Powers.get(jim).is(Live.BEAM_FIRING), "when the charge runs out it stops by itself");
            check(solar(jim) >= 0 && solar(jim) < JimConfig.BEAM_COST_PER_SECOND.get() / 20 + 1, "having spent it all: " + solar(jim));
            AbilityHandler.onKey(jim, 1, true);
            check(!Powers.get(jim).is(Live.BEAM_FIRING), "and it cannot begin again with so little");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "mob_griefing_off_jim")
    public static void withMobGriefingOffTheFireBeamPlacesNoFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HulkTests.mobGriefing(f, false);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        helper.setBlock(new BlockPos(10, (int) FLOOR + 1, 10), Blocks.STONE);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 1, true);
        run(helper, jim, 12, () -> {
            AbilityHandler.onKey(jim, 1, false);
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, (int) FLOOR + 1, 10))).is(Blocks.STONE), "the block stays");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, (int) FLOOR + 1, 10))).isAir(), "and no fire");
            helper.succeed();
        });
    }

    // ---- fire immunity, the body, the burning body ----

    @EnhancedTest(template = ARENA)
    public static void fireCannotHurtHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        PowerTicker.tick(jim);
        jim.setRemainingFireTicks(200);
        PowerTicker.tick(jim);
        check(!jim.isOnFire(), "he does not burn");
        check(jim.hasEffect(MobEffects.FIRE_RESISTANCE), "fire resistance is kept up");
        float health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().lava(), 10.0F);
        jim.hurt(helper.getLevel().damageSources().inFire(), 10.0F);
        check(jim.getHealth() >= health, "lava and fire do nothing: " + jim.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void hisBodyIsAlmostAsDurableAsVisionsAndHealsFast(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(6.5, FLOOR, 8.5, Power.VISION);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        PowerTicker.tick(vision);
        PowerTicker.tick(jim);
        check(jim.getMaxHealth() > 20.0F && jim.getMaxHealth() < vision.getMaxHealth(), "more health than a player, a little less than Vision: "
                + jim.getMaxHealth() + " / " + vision.getMaxHealth());
        check(jim.getAttributeValue(Attributes.ARMOR) > 0 && jim.getAttributeValue(Attributes.ARMOR) < vision.getAttributeValue(Attributes.ARMOR),
                "armour a little less than Vision's");
        jim.setHealth(jim.getMaxHealth() - 10.0F);
        float start = jim.getHealth();
        run(helper, jim, 60, () -> {
            float healed = jim.getHealth() - start;
            // a normal player heals one health in four seconds (80 ticks): five times as fast is one in 16 ticks: 60 ticks, three
            check(healed >= 3.0F, "he healed " + healed + " in 3 seconds");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void hisBodyBurnsWhileHeFliesAndAttackersCatchFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 8.5));
        PowerTicker.tick(jim);
        check(!Powers.get(jim).is(Live.JIM_FLAMING), "on the ground he does not burn");
        zombie.setRemainingFireTicks(0);
        float health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        check(!zombie.isOnFire() && jim.getHealth() < health, "a blow on the ground hurts him and sets nobody alight");
        TestKit.clearSpawnInvulnerability(jim);
        jim.invulnerableTime = 0;
        jim.getAbilities().flying = true;
        PowerTicker.tick(jim);
        check(Powers.get(jim).is(Live.JIM_FLAMING), "flying: his whole body burns");
        health = jim.getHealth();
        jim.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
        check(jim.getHealth() < health, "the blow still hurts him");
        check(zombie.isOnFire() || zombie.getRemainingFireTicks() > 0, "and the one who struck him burns");
        // an arrow is not a blow by hand
        Zombie other = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 8.5));
        other.setRemainingFireTicks(0);
        jim.invulnerableTime = 0;
        var arrow = TestKit.spawn(helper, EntityType.ARROW, new Vec3(11.5, FLOOR + 1, 9.5));
        jim.hurt(helper.getLevel().damageSources().arrow(arrow, other), 1.0F);
        check(other.getRemainingFireTicks() <= 0, "an arrow is not a blow by hand");
        // the human disguise puts the flames out
        AbilityHandler.onKey(jim, 5, true);
        PowerTicker.tick(jim);
        check(Powers.get(jim).disguise() && !Powers.get(jim).is(Live.JIM_FLAMING), "disguised as a human he does not burn");
        helper.succeed();
    }

    // ---- fireballs and the Nova Flame ----

    @EnhancedTest(template = ARENA)
    public static void aFireballIsThrownWithAWindUpThenHurtsBurnsAndLightsTheGround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 3, true);
        AbilityHandler.onKey(jim, 3, false);
        check(Powers.get(jim).is(Live.JIM_THROW), "the throw begins: every client sees it and plays the wind-up");
        check(JimFireballs.inFlight(jim) == 0, "the ball is still in his hand");
        run(helper, jim, JimFireballs.THROW_DELAY + 1, () -> {
            check(JimFireballs.inFlight(jim) == 1, "released: the fireball is in the air");
            run(helper, jim, 30, () -> {
                check(JimFireballs.inFlight(jim) == 0, "it struck");
                check(zombie.getHealth() < zombie.getMaxHealth(), "it hurt the zombie");
                check(zombie.getRemainingFireTicks() > 0, "and set it alight");
                check(!Powers.get(jim).is(Live.JIM_THROW), "and the throw's state is over");
                helper.succeed();
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = "jim_nova_a")
    public static void theNovaFlameWorksLikeTheChargedBeamATapIsWeakAFullOneStrongThenALongCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 10);
        f.config(JimConfig.NOVA_TAP_TICKS, 3);
        ServerPlayer tap = jim(f, 3.5, 4.5);
        ServerPlayer full = jim(f, 3.5, 15.5);
        Zombie zt = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 4.5));
        Zombie zf = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 15.5));
        zt.setRemainingFireTicks(0);
        zf.setRemainingFireTicks(0);
        look(tap, TestKit.EAST, 0.0F);
        look(full, TestKit.EAST, 0.0F);
        float health = full.getHealth();
        AbilityHandler.onKey(tap, 4, true);
        AbilityHandler.onKey(tap, 4, false);
        check(zt.getMaxHealth() - zt.getHealth() >= JimConfig.NOVA_MIN_DAMAGE.get() - 1 || !zt.isAlive(), "a quick tap still hurts: " + zt.getHealth());
        check(zt.isAlive() && zt.getHealth() > 0, "but it is weak: the zombie lives");
        AbilityHandler.onKey(full, 4, true);
        check(Powers.get(full).is(Live.JIM_NOVA_CHARGING), "B held: he gathers his energy");
        run(helper, full, 12, () -> {
            check(zf.isAlive(), "nothing yet while it charges");
            AbilityHandler.onKey(full, 4, false);
            check(!zf.isAlive(), "released at full charge, the beam kills the zombie that a tap only hurt");
            check(full.getHealth() >= health, "never him");
            long left = Powers.get(full).cooldownUntil(Powers.get(full).slot(HeroAbility.JIM_NOVA)) - helper.getLevel().getGameTime();
            check(left > 20 * 60, "then a long cooldown: " + left);
            check(Powers.get(full).is(Live.JIM_NOVA_BLAST), "and everyone sees the beam");
            AbilityHandler.onKey(full, 4, true);
            check(!Powers.get(full).is(Live.JIM_NOVA_CHARGING), "which cannot be begun again until it has passed");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void theNovaBeamIsStrongerAndWiderThanTheFireBeamAndGrowsWithTheCharge(GameTestHelper helper) {
        check(JimConfig.NOVA_DAMAGE.get() > 5 * JimConfig.BEAM_DAMAGE.get(), "a full Nova hit beats a second of the Fire Beam");
        check(JimConfig.NOVA_DAMAGE.get() > JimConfig.NOVA_MIN_DAMAGE.get(), "and a full charge beats a tap");
        check(JimConfig.NOVA_RANGE.get() > JimConfig.BEAM_RANGE.get() && JimConfig.NOVA_RANGE.get() > JimConfig.NOVA_MIN_RANGE.get(), "more reach, growing with the charge");
        check(JimConfig.NOVA_WIDTH.get() >= 2.4 * JimConfig.BEAM_WIDTH.get(), "much wider");
        check(JimConfig.NOVA_BURN_RADIUS_MAX.get() > JimConfig.NOVA_BURN_RADIUS_MIN.get(), "its burning band grows with the charge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "jim_nova_b")
    public static void aChargedNovaMakesMagmaAlongThePathAndATapDoesNot(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 10);
        f.config(JimConfig.NOVA_TAP_TICKS, 3);
        ServerPlayer tap = jim(f, 3.5, 4.5);
        ServerPlayer full = jim(f, 3.5, 15.5);
        look(tap, TestKit.EAST, 0.0F);
        look(full, TestKit.EAST, 0.0F);
        BlockPos tapGround = helper.absolutePos(new BlockPos(9, (int) FLOOR - 1, 4));
        BlockPos fullGround = helper.absolutePos(new BlockPos(9, (int) FLOOR - 1, 15));
        AbilityHandler.onKey(tap, 4, true);
        AbilityHandler.onKey(tap, 4, false);
        AbilityHandler.onKey(full, 4, true);
        run(helper, full, 12, () -> {
            AbilityHandler.onKey(full, 4, false);
            check(!helper.getLevel().getBlockState(tapGround).is(Blocks.MAGMA_BLOCK), "a tap makes no magma");
            check(helper.getLevel().getBlockState(fullGround).is(Blocks.MAGMA_BLOCK),
                    "a charged one turns the ground along its path to magma: " + helper.getLevel().getBlockState(fullGround));
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "jim_nova_c")
    public static void aCreatureBesideTheNovaBeamBurnsAndTheBandGrowsWithTheCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 10);
        f.config(JimConfig.NOVA_TAP_TICKS, 3);
        ServerPlayer tap = jim(f, 3.5, 4.5);
        ServerPlayer full = jim(f, 3.5, 12.5);
        // three blocks beside the line of each beam: outside a tap's band (2), inside a full charge's (4)
        Zombie besideTap = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 7.5));
        Zombie besideFull = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 15.5));
        besideTap.setRemainingFireTicks(0);
        besideFull.setRemainingFireTicks(0);
        look(tap, TestKit.EAST, 0.0F);
        look(full, TestKit.EAST, 0.0F);
        float fullHealth = full.getHealth();
        float tapHealth = tap.getHealth();
        AbilityHandler.onKey(tap, 4, true);
        AbilityHandler.onKey(tap, 4, false);
        check(besideTap.getRemainingFireTicks() <= 0 && besideTap.getHealth() == besideTap.getMaxHealth(), "a tap's band does not reach three blocks");
        AbilityHandler.onKey(full, 4, true);
        run(helper, full, 12, () -> {
            AbilityHandler.onKey(full, 4, false);
            check(besideFull.getRemainingFireTicks() > 0, "a full charge sets alight a creature three blocks beside its line");
            check(full.getHealth() >= fullHealth && full.getRemainingFireTicks() <= 0 && tap.getHealth() >= tapHealth, "never the shooter");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "mob_griefing_off_jim_nova")
    public static void withMobGriefingOffTheNovaPlacesNothingButStillBurnsCreatures(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 10);
        HulkTests.mobGriefing(f, false);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        Zombie beside = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 13.5));
        beside.setRemainingFireTicks(0);
        helper.setBlock(new BlockPos(18, (int) FLOOR + 1, 10), Blocks.STONE);
        look(jim, TestKit.EAST, 0.0F);
        BlockPos ground = helper.absolutePos(new BlockPos(9, (int) FLOOR - 1, 10));
        net.minecraft.world.level.block.state.BlockState groundBefore = helper.getLevel().getBlockState(ground);
        float health = jim.getHealth();
        AbilityHandler.onKey(jim, 4, true);
        run(helper, jim, 12, () -> {
            AbilityHandler.onKey(jim, 4, false);
            check(helper.getLevel().getBlockState(ground) == groundBefore, "no magma with mobGriefing off");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, (int) FLOOR, 10))).isAir(), "no fire placed");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(18, (int) FLOOR + 1, 10))).is(Blocks.STONE), "and the explosion takes no block");
            check(beside.getRemainingFireTicks() > 0, "but the creature beside the beam still burns");
            check(jim.getHealth() >= health, "and he is unhurt");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void sourceCodeVisionsFlightFireIsOnByDefault(GameTestHelper helper) {
        check(com.kolodkingal.enhanced.EnhancedConfig.SOURCE_CODE_FLIGHT_FIRE.get(), "vision.source_code_flight_fire is on by default");
        helper.succeed();
    }

    // ---- the solar charge ----

    private static int solar(ServerPlayer p) {
        return Powers.get(p).ironCharge();
    }

    @EnhancedTest(template = ARENA)
    public static void jimStartsFullOfSolarCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        check(solar(jim) == JimConfig.SOLAR_CAPACITY.get(), "full when he becomes Jim: " + solar(jim));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "jim_solar_time")
    public static void solarChargeFillsInTheSunNotAtNightNorUnderARoof(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        net.minecraft.server.level.ServerLevel level = f.level();
        long before = level.getDayTime();
        level.setDayTime(6000L);
        f.onClose(() -> level.setDayTime(before));
        ServerPlayer jim = jim(f, 8.5, 8.5);
        BlockPos above = helper.absolutePos(new BlockPos(8, (int) FLOOR + 3, 8));
        for (int dy = 0; dy < 8; dy++) {
            helper.getLevel().setBlock(above.above(dy), Blocks.AIR.defaultBlockState(), 3);
        }
        Powers.get(jim).setIronCharge(0);
        run(helper, jim, 10, () -> {
            check(solar(jim) == 10 * JimConfig.SOLAR_FILL_SUN.get(), "in the open sun it fills at the sun rate: " + solar(jim));
            // a roof: only the shade rate
            helper.getLevel().setBlock(above, Blocks.STONE.defaultBlockState(), 3);
            run(helper, jim, 3, () -> { // (the sky is told of the roof within a tick or two)
                Powers.get(jim).setIronCharge(0);
                run(helper, jim, 10, () -> {
                    check(solar(jim) == 10 * JimConfig.SOLAR_FILL_SHADE.get(), "under a roof only the shade rate: " + solar(jim));
                    helper.getLevel().setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                    level.setDayTime(18000L);
                    Powers.get(jim).setIronCharge(0);
                    run(helper, jim, 10, () -> {
                        check(solar(jim) == 0, "none at night: " + solar(jim));
                        helper.succeed();
                    });
                });
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = "jim_solar_cap")
    public static void solarChargeNeverPassesItsCapacityAndFlightCostsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.SOLAR_CAPACITY, 300);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        Powers.get(jim).setIronCharge(300);
        AbilityHandler.onKey(jim, 2, true);
        run(helper, jim, 20, () -> {
            check(Powers.get(jim).flying(), "he flies");
            check(solar(jim) <= 300, "never over the capacity: " + solar(jim));
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void theNovaStartsAtTheJewelOfTheLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 8.5, 8.5);
        PowerData data = Powers.get(jim);
        Powers.setVariant(jim, Variants.get(Power.JIM_HAMMOND, "android"));
        check(!com.kolodkingal.enhanced.ability.JimAbilities.novaStartsAtHead(data), "android: from the chest diamond");
        Powers.setVariant(jim, Variants.get(Power.JIM_HAMMOND, "classic"));
        check(com.kolodkingal.enhanced.ability.JimAbilities.novaStartsAtHead(data), "classic: from the forehead jewel");
        data.setDisguise(true);
        check(!com.kolodkingal.enhanced.ability.JimAbilities.novaStartsAtHead(data), "human disguise: from the chest centre");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "jim_solar_spend")
    public static void eachAttackSpendsSolarCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.NOVA_CHARGE_TICKS, 5);
        f.config(JimConfig.NOVA_TAP_TICKS, 2);
        ServerPlayer ball = jimWithNoSun(f, 3.5, 4.5);
        ServerPlayer tap = jim(f, 3.5, 10.5);
        ServerPlayer full = jim(f, 3.5, 15.5);
        look(ball, TestKit.EAST, 0.0F);
        look(tap, TestKit.EAST, 0.0F);
        look(full, TestKit.EAST, 0.0F);
        int cap = JimConfig.SOLAR_CAPACITY.get();
        AbilityHandler.onKey(ball, 3, true);
        AbilityHandler.onKey(ball, 3, false);
        check(solar(ball) == cap - JimConfig.FIREBALL_COST.get(), "a fireball cost its price: " + solar(ball));
        AbilityHandler.onKey(tap, 4, true);
        AbilityHandler.onKey(tap, 4, false);
        check(cap - solar(tap) == JimConfig.NOVA_MIN_COST.get(), "a tap of the Nova cost a tap's price: " + (cap - solar(tap)));
        AbilityHandler.onKey(full, 4, true);
        run(helper, full, 8, () -> {
            AbilityHandler.onKey(full, 4, false);
            check(cap - solar(full) == JimConfig.NOVA_COST.get(), "a full charge cost the full price: " + (cap - solar(full)));
            check(JimConfig.NOVA_COST.get() > JimConfig.NOVA_MIN_COST.get() && JimConfig.NOVA_COST.get() > 4 * JimConfig.FIREBALL_COST.get(),
                    "dearer than a tap, and a lot more than a fireball");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void theNovaNeedsAHighMinimumToBegin(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        look(jim, TestKit.EAST, 0.0F);
        Powers.get(jim).setIronCharge(JimConfig.NOVA_MIN_CHARGE.get() - 1);
        AbilityHandler.onKey(jim, 4, true);
        check(!Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "just under the minimum: he cannot even gather it");
        Powers.get(jim).setIronCharge(JimConfig.NOVA_MIN_CHARGE.get() + 1);
        AbilityHandler.onKey(jim, 4, true);
        check(Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "with enough he gathers it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aHeldFireballChargesBiggerAndDearerAndFliesAsABodyInTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        look(jim, TestKit.EAST, 0.0F);
        int cap = JimConfig.SOLAR_CAPACITY.get();
        check(JimFireballs.charge(0) == 0.0F && JimFireballs.charge(JimConfig.FIREBALL_CHARGE_TICKS.get()) == 1.0F, "the charge grows to full");
        check(JimFireballs.size(1.0F) > JimFireballs.size(0.0F), "a charged ball is bigger");
        AbilityHandler.onKey(jim, 3, true);
        check(Powers.get(jim).is(Live.JIM_FIREBALL_CHARGING), "V held: the ball grows in his hand");
        check(solar(jim) == cap, "charging costs nothing yet");
        run(helper, jim, JimConfig.FIREBALL_CHARGE_TICKS.get() + 2, () -> {
            AbilityHandler.onKey(jim, 3, false);
            check(!Powers.get(jim).is(Live.JIM_FIREBALL_CHARGING) && Powers.get(jim).is(Live.JIM_THROW), "let go: thrown");
            check(solar(jim) == cap - 2 * JimConfig.FIREBALL_COST.get(), "a full charge costs twice the price: " + solar(jim));
            run(helper, jim, JimFireballs.THROW_DELAY + 1, () -> {
                check(JimFireballs.inFlight(jim) == 1, "in the air");
                check(!helper.getLevel().getEntitiesOfClass(com.kolodkingal.enhanced.entity.JimFireballEntity.class,
                        jim.getBoundingBox().inflate(40)).isEmpty(), "every client can see it: it has a body in the world");
                helper.succeed();
            });
        });
    }

    @EnhancedTest(template = ARENA)
    public static void anAttackIsRefusedWhenTheChargeIsEmpty(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        look(jim, TestKit.EAST, 0.0F);
        Powers.get(jim).setIronCharge(0);
        AbilityHandler.onKey(jim, 3, true);
        AbilityHandler.onKey(jim, 3, false);
        check(!Powers.get(jim).is(Live.JIM_THROW) && JimFireballs.inFlight(jim) == 0, "no fireball");
        AbilityHandler.onKey(jim, 1, true);
        check(!Powers.get(jim).is(Live.BEAM_FIRING), "no beam");
        AbilityHandler.onKey(jim, 4, true);
        check(!Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "no Nova");
        check(solar(jim) == 0, "and nothing was spent");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "jim_solar_off")
    public static void withTheSolarSwitchOffAttacksAreFree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(JimConfig.SOLAR_CHARGE, false);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        look(jim, TestKit.EAST, 0.0F);
        Powers.get(jim).setIronCharge(0);
        AbilityHandler.onKey(jim, 3, true);
        AbilityHandler.onKey(jim, 3, false);
        check(Powers.get(jim).is(Live.JIM_THROW), "the fireball is thrown with an empty meter");
        AbilityHandler.onKey(jim, 1, true);
        check(Powers.get(jim).is(Live.BEAM_FIRING), "the beam burns");
        run(helper, jim, 10, () -> {
            AbilityHandler.onKey(jim, 1, false);
            AbilityHandler.onKey(jim, 4, true);
            check(Powers.get(jim).is(Live.JIM_NOVA_CHARGING), "and the Nova gathers");
            check(solar(jim) == 0, "nothing is spent or gained");
            helper.succeed();
        });
    }

    // ---- what they kill drops cooked ----

    private static int dropped(GameTestHelper helper, Vec3 at, net.minecraft.world.item.Item item) {
        int n = 0;
        for (net.minecraft.world.entity.item.ItemEntity e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(6.0))) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private static net.minecraft.world.entity.animal.Cow cow(GameTestHelper helper, Vec3 at) {
        net.minecraft.world.entity.animal.Cow cow = TestKit.spawnWithNoFreeWill(helper, EntityType.COW, at);
        cow.setRemainingFireTicks(0);
        return cow;
    }

    @EnhancedTest(template = ARENA)
    public static void whatJimsFireAndFistsKillDropsCooked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(new Vec3(12.5, FLOOR, 4.5));
        net.minecraft.world.entity.animal.Cow byFire = cow(helper, new Vec3(12.5, FLOOR, 4.5));
        byFire.hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level, com.kolodkingal.enhanced.registry.ModDamageTypes.SOLAR_BEAM, jim, jim), 200.0F);
        net.minecraft.world.entity.animal.Cow byFist = cow(helper, new Vec3(12.5, FLOOR, 15.5));
        Vec3 at2 = helper.absoluteVec(new Vec3(12.5, FLOOR, 15.5));
        byFist.hurt(level.damageSources().playerAttack(jim), 200.0F);
        check(dropped(helper, at, net.minecraft.world.item.Items.COOKED_BEEF) >= 1 && dropped(helper, at, net.minecraft.world.item.Items.BEEF) == 0,
                "a cow killed by Jim's fire drops cooked beef only");
        check(dropped(helper, at2, net.minecraft.world.item.Items.COOKED_BEEF) >= 1 && dropped(helper, at2, net.minecraft.world.item.Items.BEEF) == 0,
                "and by his fist too");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void whatJimsContinuousBeamKillsDropsCooked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jim = jim(f, 3.5, 10.5);
        Vec3 at = helper.absoluteVec(new Vec3(8.5, FLOOR, 10.5));
        net.minecraft.world.entity.animal.Cow cow = cow(helper, new Vec3(8.5, FLOOR, 10.5));
        look(jim, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(jim, 1, true);
        run(helper, jim, 60, () -> {
            AbilityHandler.onKey(jim, 1, false);
            check(!cow.isAlive(), "the beam killed the cow");
            check(dropped(helper, at, net.minecraft.world.item.Items.COOKED_BEEF) >= 1 && dropped(helper, at, net.minecraft.world.item.Items.BEEF) == 0,
                    "and it dropped cooked beef");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void whatSourceCodeVisionKillsDropsCookedButAMindBeamKillIsRaw(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(3.5, FLOOR, 10.5, Power.VISION);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        Powers.setVariant(vision, Variants.get(Power.VISION, "source_code"));
        Vec3 solarAt = helper.absoluteVec(new Vec3(12.5, FLOOR, 4.5));
        cow(helper, new Vec3(12.5, FLOOR, 4.5)).hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level,
                com.kolodkingal.enhanced.registry.ModDamageTypes.SOLAR_BEAM, vision, vision), 200.0F);
        Vec3 mindAt = helper.absoluteVec(new Vec3(12.5, FLOOR, 15.5));
        cow(helper, new Vec3(12.5, FLOOR, 15.5)).hurt(com.kolodkingal.enhanced.registry.ModDamageTypes.source(level,
                com.kolodkingal.enhanced.registry.ModDamageTypes.MIND_STONE_BEAM, vision, vision), 200.0F);
        check(dropped(helper, solarAt, net.minecraft.world.item.Items.COOKED_BEEF) >= 1 && dropped(helper, solarAt, net.minecraft.world.item.Items.BEEF) == 0,
                "a cow killed by Source Code Vision's solar beam drops cooked beef");
        check(dropped(helper, mindAt, net.minecraft.world.item.Items.BEEF) >= 1 && dropped(helper, mindAt, net.minecraft.world.item.Items.COOKED_BEEF) == 0,
                "a cow killed by a normal Vision beam drops raw beef");
        helper.succeed();
    }

    // ---- looks, the Creator ----

    @EnhancedTest(template = ARENA)
    public static void hisLooksAreSkinsOfTheRightSizeWithACape(GameTestHelper helper) throws IOException {
        check(Variants.of(Power.JIM_HAMMOND).stream().map(v -> v.id()).toList().equals(java.util.List.of("android", "classic", "human", "own")),
                "android, classic, human, own");
        int[][] sizes = {{128, 128}, {64, 64}, {64, 64}, {64, 32}};
        String[] names = {"jim_android", "jim_classic", "jim_human", "jim_android_cape"};
        for (int i = 0; i < names.length; i++) {
            try (InputStream in = JimTests.class.getResourceAsStream("/assets/enhanced/textures/entity/suit/" + names[i] + ".png")) {
                check(in != null, names[i] + " is in the jar");
                var image = ImageIO.read(in);
                check(image.getWidth() == sizes[i][0] && image.getHeight() == sizes[i][1], names[i] + " is " + sizes[i][0] + "x" + sizes[i][1]
                        + ": " + image.getWidth() + "x" + image.getHeight());
            }
        }
        check(Variants.get(Power.JIM_HAMMOND, "android").cape() != null && Variants.get(Power.JIM_HAMMOND, "classic").cape() == null,
                "the android has the white cape");
        // the fire: the glow, and the two soft flame textures that scroll over the sheath
        String[] sets = {"glow", "flow_0", "flow_1"};
        int[][] fireSizes = {{64, 64}, {64, 128}, {64, 128}};
        for (int k = 0; k < sets.length; k++) {
            try (InputStream in = JimTests.class.getResourceAsStream("/assets/enhanced/textures/entity/jim/" + sets[k] + ".png")) {
                check(in != null, sets[k] + " is in the jar");
                var image = ImageIO.read(in);
                check(image.getWidth() == fireSizes[k][0] && image.getHeight() == fireSizes[k][1], sets[k] + " size: " + image.getWidth() + "x" + image.getHeight());
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void heIsInTheHeroCreatorWithAllHisAbilities(GameTestHelper helper) {
        check(com.kolodkingal.enhanced.custom.HeroLooks.heroes().contains(Power.JIM_HAMMOND), "listed as a hero to become");
        check(HeroAbility.of(Power.JIM_HAMMOND).size() == 7, "seven rows: " + HeroAbility.of(Power.JIM_HAMMOND).size());
        for (HeroAbility a : HeroAbility.of(Power.JIM_HAMMOND)) {
            check(com.kolodkingal.enhanced.custom.Catalog.byId(a.id()) == a, a.id() + " is in the catalogue");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aCustomHeroWithHisFireImmunityIsImmune(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer custom = CustomKitSupport.hero(f, 8.5, 8.5, CustomKitSupport.def().pick("jim_hammond.fire_immunity", 5).build());
        CustomKitSupport.tickN(custom, 2);
        custom.setRemainingFireTicks(100);
        CustomKitSupport.tickN(custom, 1);
        check(!custom.isOnFire(), "the pick works for a hero of the creator");
        helper.succeed();
    }
}
