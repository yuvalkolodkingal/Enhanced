package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Billy's Hex Barrier against magic (3.3.1): it has 60 health, and only chaos magic (a power of the Scarlet Witch, the Chaos
 * Beam) and dark magic (the Darkhold's beam, Agatha) wear it down, each by its damage; arrows and blows are stopped and cost
 * it nothing; enough chaos or dark magic shatters it, with its cooldown. Billy stands at (10.5, 8.5) looking south (+z).
 * 3.4: a custom hero whose Hex Barrier it is (billy.freeze with its variant) lives by the same rule.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BarrierMagicTests {
    private static ServerPlayer billy(TestKit.Fixture f) {
        ServerPlayer billy = f.player(10.5, FLOOR, 8.5, Power.BILLY);
        billy.getFoodData().setFoodLevel(20);
        TestKit.look(billy, 0.0F, 0.0F);
        return billy;
    }

    private static void raise(TestKit.Fixture f, ServerPlayer billy, Runnable then) {
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        billy.setShiftKeyDown(false);
        f.helper().runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(billy), "the barrier is complete after its wind-up");
            then.run();
        });
    }

    private static float health(ServerPlayer billy) {
        return Powers.get(billy).shieldStrength();
    }

    private static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.setNoAi(true);
        return husk;
    }

    @GameTest(template = ARENA, batch = "barrier_magic_plain", timeoutTicks = 60)
    public static void anArrowOrABlowLeavesItsHealth(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        Husk front = husk(helper, 10.5, 10.0);
        raise(f, billy, () -> {
            assertNear(health(billy), 60.0, 1.0E-3, "a whole barrier has 60 health");
            Skeleton shooter = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(1.5, FLOOR, 1.5));
            shooter.setNoAi(true);
            Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
            arrow.setPos(helper.absoluteVec(new Vec3(10.5, FLOOR + 1.0, 14.5)));
            billy.invulnerableTime = 0;
            billy.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), 9.0F);
            arrow.discard();
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the arrow is stopped");
            assertNear(health(billy), 60.0, 1.0E-3, "and leaves its health as it was");
            billy.invulnerableTime = 0;
            billy.hurt(helper.getLevel().damageSources().mobAttack(front), 8.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the blow is stopped");
            assertNear(health(billy), 60.0, 1.0E-3, "and leaves its health as it was too");
            check(BillyBarrier.isUp(billy), "it stands");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "barrier_magic_dark", timeoutTicks = 60)
    public static void darkMagicWearsItDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        ServerPlayer agatha = f.player(10.5, FLOOR, 14.5, Power.AGATHA);
        f.pvp(true);
        raise(f, billy, () -> {
            billy.invulnerableTime = 0;
            billy.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM, agatha, agatha), 7.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the Darkhold's beam is stopped");
            assertNear(health(billy), 53.0, 1.0E-3, "and wears it down by its damage");
            // Any power of Agatha's is the Darkhold's magic: her grip that would cost 5.
            check(Shelters.stops(agatha, agatha.getEyePosition(), billy, 5.0F), "Agatha's power is stopped");
            assertNear(health(billy), 48.0, 1.0E-3, "and wears it down too");
            check(BillyBarrier.isUp(billy), "it still stands");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "barrier_magic_chaos", timeoutTicks = 60)
    public static void chaosMagicWearsItDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        ServerPlayer wanda = f.player(10.5, FLOOR, 14.5, Power.SCARLET_WITCH);
        f.pvp(true);
        raise(f, billy, () -> {
            billy.invulnerableTime = 0;
            billy.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 10.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the Chaos Beam is stopped");
            assertNear(health(billy), 50.0, 1.0E-3, "and wears it down by its damage, no longer shattering it at once");
            check(BillyBarrier.isUp(billy), "it still stands");
            BillyBarrier.wear(billy, Powers.get(billy), 6.0F, wanda);
            assertNear(health(billy), 44.0, 1.0E-3, "any power of a Scarlet Witch wears it");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "barrier_magic_shatter", timeoutTicks = 80)
    public static void enoughChaosOrDarkMagicShattersIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = billy(f);
        ServerPlayer wanda = f.player(10.5, FLOOR, 14.5, Power.SCARLET_WITCH);
        Husk caster = husk(helper, 12.5, 14.5);
        f.pvp(true);
        PowerData data = Powers.get(billy);
        raise(f, billy, () -> {
            for (int i = 0; i < 3; i++) {
                billy.invulnerableTime = 0;
                billy.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 15.0F);
            }
            check(BillyBarrier.isUp(billy), "45 of 60: still up");
            TestKit.sentPackets(billy);
            billy.invulnerableTime = 0;
            billy.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 15.0F);
            assertNear(20.0F - billy.getHealth(), 0.0, 1.0E-4, "the hit that breaks it is stopped too");
            check(!BillyBarrier.isUp(billy) && !data.chaosShieldActive(), "chaos magic took the last of it: shattered");
            check("message.enhanced.billy_barrier.chaos".equals(lastActionBarKey(billy)), "chaos magic shattered it");
            long left = data.cooldownUntil(4) - helper.getLevel().getGameTime();
            check(left >= 195 && left <= 200, "the shatter cooldown: " + left);
            // Dark magic: a second barrier, once B is ready again.
            data.startCooldown(4, helper.getLevel().getGameTime(), 0);
            BillyBarrier.keep(billy, 60.0F);
            raise(f, billy, () -> {
                TestKit.sentPackets(billy);
                billy.invulnerableTime = 0;
                billy.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM, caster, caster), 61.0F);
                check(!BillyBarrier.isUp(billy), "enough dark magic shatters it");
                check("message.enhanced.billy_barrier.broken".equals(lastActionBarKey(billy)), "it broke");
                assertNear(BillyBarrier.health(billy), 0.0, 1.0E-3, "nothing is left of it");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, batch = "barrier_magic_custom", timeoutTicks = 60)
    public static void aCustomHerosBarrierKeepsTheSameRule(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // (3.4) Billy's Freeze with its variant on G: the barrier is on Sneak + G, its cooldown on G.
        ServerPlayer hero = CustomKitSupport.hero(f, 10.5, 8.5, CustomKitSupport.def().pick("billy.freeze", 5)
                .key(2, "billy.freeze").build());
        hero.getFoodData().setFoodLevel(20);
        TestKit.look(hero, 0.0F, 0.0F);
        ServerPlayer agatha = f.player(10.5, FLOOR, 14.5, Power.AGATHA);
        ServerPlayer wanda = f.player(12.5, FLOOR, 14.5, Power.SCARLET_WITCH);
        Husk front = husk(helper, 10.5, 10.0);
        f.pvp(true);
        PowerData data = Powers.get(hero);
        CustomKitSupport.press(hero, 2, true, true);
        hero.setShiftKeyDown(false);
        check(BillyBarrier.owns(data) && data.chaosShieldActive(), "Sneak + G: his barrier forms");
        helper.runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(hero), "complete after its wind-up");
            assertNear(health(hero), 60.0, 1.0E-3, "a whole barrier has 60 health");
            hero.invulnerableTime = 0;
            hero.hurt(helper.getLevel().damageSources().mobAttack(front), 8.0F);
            assertNear(20.0F - hero.getHealth(), 0.0, 1.0E-4, "the blow is stopped");
            assertNear(health(hero), 60.0, 1.0E-3, "and costs it nothing");
            hero.invulnerableTime = 0;
            hero.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM, agatha, agatha), 7.0F);
            assertNear(health(hero), 53.0, 1.0E-3, "dark magic wears it down by its damage");
            hero.invulnerableTime = 0;
            hero.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 10.0F);
            assertNear(health(hero), 43.0, 1.0E-3, "chaos magic wears it down, no longer shattering it at once");
            check(BillyBarrier.isUp(hero), "it still stands");
            for (int i = 0; i < 3; i++) {
                hero.invulnerableTime = 0;
                hero.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, wanda, wanda), 15.0F);
            }
            check(!BillyBarrier.isUp(hero) && !data.chaosShieldActive(), "enough chaos magic shatters it");
            long left = data.cooldownUntil(data.slot(HeroAbility.BILLY_FREEZE)) - helper.getLevel().getGameTime();
            check(data.slot(HeroAbility.BILLY_FREEZE) == 2 && left >= 195 && left <= 200, "the shatter cooldown is on G: " + left);
            helper.succeed();
        });
    }

    private BarrierMagicTests() {
    }
}
