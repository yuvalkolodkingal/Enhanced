package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.2, Wanda's unlimited Chaos Shield (requirement 4a, {@code scarlet_witch.shieldUnlimited}, default true): it costs no
 * hunger, has no strength to wear down, never shatters, never runs out of time and needs no recharge; with the option off
 * the old rules apply. It still stops exactly the damage it stops today. (All the other shield tests run with the option
 * off: {@link TestKit#fixture} switches it off.) Every test has a batch of its own.
 */
public final class AbwShieldTests {
    /** Presses B and runs {@code then} when the shield is complete. */
    private static void raise(TestKit.Fixture f, ServerPlayer wanda, Runnable then) {
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "B starts the shield");
        f.helper().runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 1, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the shield is complete after its wind-up");
            then.run();
        });
    }

    private static void arrowBlow(GameTestHelper helper, Skeleton shooter, ServerPlayer victim, float damage) {
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        victim.invulnerableTime = 0;
        victim.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), damage);
    }

    private static Skeleton harmlessSkeleton(GameTestHelper helper) {
        Skeleton skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(15.5, FLOOR, 10.5));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setNoAi(true);
        return skeleton;
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents t ? t.getKey() : component.getString();
    }

    /** No food needed, no exhaustion, no time limit, no recharge after it drops. */
    @EnhancedTest(template = ARENA, batch = "abw_shield_unlimited_cost", timeoutTicks = 260)
    public static void unlimitedShieldCostsNothingAndNeverRunsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        wanda.getFoodData().setFoodLevel(0);
        check(WandaAbilities.canAffordShield(wanda), "an empty stomach does not matter");
        AbilityHandler.onKey(wanda, 4, true);
        check(data.chaosShieldActive(), "so B raises it even with no food");
        check(WandaAbilities.maxShieldTicks() == Integer.MAX_VALUE && Float.isInfinite(WandaAbilities.maxShieldStrength()),
                "its strength and time are endless");
        wanda.causeFoodExhaustion(0.0F);
        f.everyTick(250, tick -> {
            PowerTicker.tick(wanda);
            check(data.chaosShieldActive(), "still up at tick " + tick + " (the old limit was 160)");
            if (tick < 250) {
                return false;
            }
            check(WandaAbilities.hasShieldUp(wanda), "complete");
            check(wanda.getFoodData().getExhaustionLevel() == 0.0F, "it spent no hunger: " + wanda.getFoodData().getExhaustionLevel());
            AbilityHandler.onKey(wanda, 4, false);
            check(!data.chaosShieldActive(), "B released: it drops");
            check(data.isReady(4, helper.getLevel().getGameTime()), "and nothing has to recharge");
            AbilityHandler.onKey(wanda, 4, true);
            check(data.chaosShieldActive(), "it can be raised again at once");
            return true;
        });
    }

    /** It stops what it stops today and is never worn: arrows, a creature's blow, a hundred points of them. */
    @EnhancedTest(template = ARENA, batch = "abw_shield_unlimited_stops", timeoutTicks = 100)
    public static void unlimitedShieldStopsArrowsAndBlowsWithoutWearing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        ServerPlayer wanda = f.player(14.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        Skeleton skeleton = harmlessSkeleton(helper);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(13.3, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setNoAi(true);
        raise(f, wanda, () -> {
            float strength = data.shieldStrength();
            for (int i = 0; i < 10; i++) {
                arrowBlow(helper, skeleton, wanda, 10.0F);
            }
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "ten arrows of 10 (100 points; the old shield held 30) do nothing");
            wanda.invulnerableTime = 0;
            wanda.hurt(helper.getLevel().damageSources().mobAttack(zombie), 100.0F);
            assertNear(20.0F - wanda.getHealth(), 0.0, 1.0E-4, "and a creature's blow of 100 does not touch her");
            assertNear(data.shieldStrength(), strength, 1.0E-6, "the strength is never worn down");
            check(data.chaosShieldActive() && WandaAbilities.hasShieldUp(wanda), "and it did not break");
            // Explosions are what they were: only a quarter gets through.
            wanda.invulnerableTime = 0;
            wanda.hurt(helper.getLevel().damageSources().explosion(null, null), 8.0F);
            assertNear(20.0F - wanda.getHealth(), 2.0, 1.0E-4, "an explosion of 8 still lets a quarter (2) through, as today");
            check(data.chaosShieldActive(), "and it still stands");
            // Damage that bypasses invulnerability is not stopped, as today.
            wanda.invulnerableTime = 0;
            float before = wanda.getHealth();
            wanda.hurt(helper.getLevel().damageSources().genericKill(), 3.0F);
            assertNear(before - wanda.getHealth(), 3.0, 1.0E-4, "the void-kind of damage is not stopped, as today");
            helper.succeed();
        });
    }

    /** The HUD says it is unlimited. */
    @EnhancedTest(template = ARENA, batch = "abw_shield_unlimited_hud")
    public static void unlimitedShieldShowsInfinityOnTheHud(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        ServerPlayer wanda = f.player(5.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        long now = helper.getLevel().getGameTime();
        data.raiseChaosShield(now - 100, 15, 30.0F);
        Component line = ShieldStyles.hudLine(data, now, WandaAbilities.maxShieldStrength(), WandaAbilities.maxShieldTicks());
        check(line != null && key(line).equals("hud.enhanced.shield_unlimited"), "the line is the infinity sign: " + line);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, false);
        Component old = ShieldStyles.hudLine(data, now, WandaAbilities.maxShieldStrength(), WandaAbilities.maxShieldTicks());
        check(old != null && key(old).equals("hud.enhanced.shield_strength"), "with the option off it shows strength and seconds again: " + old);
        helper.succeed();
    }

    /** With the option off the old rules apply: hunger is needed, strength wears down, it shatters and has to recharge. */
    @EnhancedTest(template = ARENA, batch = "abw_shield_limited", timeoutTicks = 100)
    public static void withTheOptionOffTheOldRulesApply(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, false);
        ServerPlayer wanda = f.player(14.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        wanda.getFoodData().setFoodLevel(0);
        check(!WandaAbilities.canAffordShield(wanda), "no food, no shield");
        AbilityHandler.onKey(wanda, 4, true);
        check(!data.chaosShieldActive(), "B does nothing on an empty stomach");
        wanda.getFoodData().setFoodLevel(20);
        check(WandaAbilities.maxShieldTicks() == 160 && WandaAbilities.maxShieldStrength() == 30.0F, "160 ticks and 30 points again");
        Skeleton skeleton = harmlessSkeleton(helper);
        raise(f, wanda, () -> {
            arrowBlow(helper, skeleton, wanda, 10.0F);
            assertNear(data.shieldStrength(), 20.0, 1.0E-4, "an arrow of 10 wears it by 10");
            arrowBlow(helper, skeleton, wanda, 25.0F);
            check(!data.chaosShieldActive(), "the arrow that takes the rest shatters it");
            check(!data.isReady(4, helper.getLevel().getGameTime()), "and it has to recharge");
            helper.succeed();
        });
    }

    /** The boss keeps its rules: the Mind Crush is not stopped by the Chaos Shield, unlimited or not. */
    @EnhancedTest(template = ARENA, batch = "abw_shield_unlimited_boss", timeoutTicks = 100)
    public static void unlimitedShieldDoesNotStopTheMindCrush(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 2, Power.SCARLET_WITCH);
        d.f().config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), wanda, 5.5, FLOOR, 10.5);
        AbilityHandler.onKey(wanda, 4, true);
        d.f().helper().runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 2, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the shield is up");
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH), "he begins the Mind Crush");
            helper.runAfterDelay(ApocalypseAttack.MIND_CRUSH.tellTicks() + 3, () -> {
                check(wanda.hasEffect(MobEffects.CONFUSION), "the Mind Crush gets through the shield, as it does today");
                check(Powers.get(wanda).chaosShieldActive(), "and the shield stands");
                helper.succeed();
            });
        });
    }
}
