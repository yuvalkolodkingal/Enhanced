package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.captain.Assemble;
import com.kolodkingal.enhanced.captain.CaptainCombo;
import com.kolodkingal.enhanced.captain.CaptainState;
import com.kolodkingal.enhanced.captain.ShieldCharge;
import com.kolodkingal.enhanced.captain.ShieldRules;
import com.kolodkingal.enhanced.captain.ShieldThrow;
import com.kolodkingal.enhanced.captain.ThrownShieldEntity;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Captain America and the Vibranium Shield (3.8, lane L1, PLAN 3): his body (Speed II, Strength II, health, jump, fall;
 * worthy of Mjolnir), Resistance I (a zombie's blow cut by 20 %), the shield (a creeper's explosion from the front
 * blocked whole for him, not for a powerless holder who has just raised it, never from behind; an axe never disables
 * it; an arrow from the front bounces back), the throw (8 to a husk, back to the off hand; a full hand: the inventory;
 * the thrower gone: it drops), the ricochet (three husks, 8, 7, 6), the slam (6 within 4), the charge (8 blocks; a wall
 * stops it, unhurt), the bash (a stun of 30 ticks), Assemble (a villager and a player, not a zombie), Inspire (6), the
 * combo (4, 4, 7), the flip kick, a custom hero's throw of a vanilla shield (it comes back, no ricochet), and with PvP
 * off no player hurt (with it on, yes). Batches {@code captain} and {@code captain_pvp} (PvP is server-wide).
 */
public final class CaptainTests {
    private static final String BATCH = "captain";
    private static final String BATCH_PVP = "captain_pvp";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Captain America at full health, his shield in the off hand, the main hand empty (one tick). */
    static ServerPlayer cap(TestKit.Fixture f, double x, double z) {
        ServerPlayer cap = f.player(x, FLOOR, z, Power.CAPTAIN_AMERICA);
        f.onClose(() -> HeroLifecycle.onLogout(cap));
        cap.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.VIBRANIUM_SHIELD.get()));
        cap.doTick();
        cap.setHealth(cap.getMaxHealth());
        return cap;
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A husk with no armour and 200 health that stands where it is put (no blow moves it). */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(net.minecraft.world.entity.LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    /**
     * Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}; one
     * entry at a time in the test's schedule.
     */
    static void until(GameTestHelper helper, int max, BooleanSupplier done, Runnable then, ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, BooleanSupplier done, Runnable then,
            ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                if (!p.isRemoved()) {
                    p.doTick();
                }
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

    /** Ticks the players once a server tick for {@code n} ticks, then runs {@code then}. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    static boolean shieldIn(ServerPlayer player, InteractionHand hand) {
        return player.getItemInHand(hand).is(ModItems.VIBRANIUM_SHIELD.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    /** Speed II, Strength II, 26 health, a better jump and fall, quicker blows; worthy of Mjolnir. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theSuperSoldiersBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 3.5, 3.5);
        ServerPlayer plain = f.player(8.5, FLOOR, 3.5);
        plain.doTick();
        assertNear(cap.getMaxHealth(), 26.0, 1.0E-4, "health");
        assertNear(cap.getAttributeValue(Attributes.ATTACK_DAMAGE), plain.getAttributeValue(Attributes.ATTACK_DAMAGE)
                + 6.0, 1.0E-4, "Strength II");
        assertNear(cap.getAttributeValue(Attributes.MOVEMENT_SPEED), plain.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * 1.4, 1.0E-4, "Speed II");
        assertNear(cap.getAttributeValue(Attributes.JUMP_STRENGTH), plain.getAttributeValue(Attributes.JUMP_STRENGTH)
                + 0.15, 1.0E-4, "the jump");
        assertNear(cap.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), plain.getAttributeValue(
                Attributes.SAFE_FALL_DISTANCE) + 4.0, 1.0E-4, "the fall");
        assertNear(cap.getAttributeValue(Attributes.ATTACK_SPEED), plain.getAttributeValue(Attributes.ATTACK_SPEED)
                + 0.5, 1.0E-4, "quicker blows");
        check(cap.getActiveEffects().isEmpty(), "attributes, never effects");
        check(Traits.has(cap, Trait.WORTHY) && Worthiness.isWorthy(cap), "Cap lifts Mjolnir");
        check(!Worthiness.isWorthy(plain), "not everyone");
        // The power taken away: the body goes with it.
        Powers.setPower(cap, Power.NONE);
        assertNear(cap.getMaxHealth(), 20.0, 1.0E-4, "health back");
        assertNear(cap.getAttributeValue(Attributes.MOVEMENT_SPEED), plain.getAttributeValue(Attributes.MOVEMENT_SPEED),
                1.0E-4, "speed back");
        helper.succeed();
    }

    /** Resistance I: a zombie's blow of 10 takes 8 from him (and 10 from anyone else). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void resistanceCutsAZombiesBlow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 3.5, 3.5);
        cap.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ServerPlayer plain = f.player(8.5, FLOOR, 3.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 8.5));
        float before = cap.getHealth();
        cap.hurt(cap.damageSources().mobAttack(zombie), 10.0F);
        assertNear(before - cap.getHealth(), 8.0, 0.01, "20 % off");
        float plainBefore = plain.getHealth();
        plain.hurt(plain.damageSources().mobAttack(zombie), 10.0F);
        assertNear(plainBefore - plain.getHealth(), 10.0, 0.01, "the whole blow on anyone else");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The shield

    /** A creeper blows up before him: his raised shield (raised this very tick) takes all of it. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShieldBlocksAnExplosionFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 6.5, 10.5);
        look(cap, EAST, 0.0F);
        cap.startUsingItem(InteractionHand.OFF_HAND);
        check(ShieldRules.guarding(cap) && !cap.isBlocking(), "up at once (vanilla's would wait 5 ticks)");
        Creeper creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(9.0, FLOOR, 10.5));
        float before = cap.getHealth();
        Vec3 at = creeper.position();
        helper.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.NONE);
        creeper.discard();
        assertNear(cap.getHealth(), before, 1.0E-4, "nothing gets through");
        helper.succeed();
    }

    /** A powerless player raising the same shield this tick is not yet guarded: vanilla's shield waits 5 ticks. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPowerlessHolderGetsVanillasShare(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(6.5, FLOOR, 10.5);
        plain.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.VIBRANIUM_SHIELD.get()));
        look(plain, EAST, 0.0F);
        plain.startUsingItem(InteractionHand.OFF_HAND);
        check(!ShieldRules.guarding(plain), "not Captain America");
        Creeper creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(9.0, FLOOR, 10.5));
        float before = plain.getHealth();
        Vec3 at = creeper.position();
        helper.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.NONE);
        creeper.discard();
        check(plain.getHealth() < before, "he is hurt: " + (before - plain.getHealth()));
        helper.succeed();
    }

    /** From behind, the shield takes nothing (only his Resistance cuts the blow). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShieldNeverBlocksFromBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 6.5, 10.5);
        look(cap, -EAST, 0.0F);
        cap.startUsingItem(InteractionHand.OFF_HAND);
        Creeper creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(9.0, FLOOR, 10.5));
        float before = cap.getHealth();
        Vec3 at = creeper.position();
        helper.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.NONE);
        creeper.discard();
        check(cap.getHealth() < before, "hurt from behind: " + (before - cap.getHealth()));
        helper.succeed();
    }

    /** A vindicator's axe on his raised shield: no harm, the shield stays up and is never disabled. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anAxeNeverDisablesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 5.5, 5.5);
        look(cap, EAST, 0.0F);
        cap.startUsingItem(InteractionHand.OFF_HAND);
        Vindicator vindicator = TestKit.spawnWithNoFreeWill(helper, EntityType.VINDICATOR, new Vec3(7.0, FLOOR, 5.5));
        vindicator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        check(vindicator.canDisableShield(), "an axe can disable a shield");
        float before = cap.getHealth();
        cap.hurt(cap.damageSources().mobAttack(vindicator), 8.0F);
        cap.doTick();
        assertNear(cap.getHealth(), before, 1.0E-4, "no harm");
        check(!cap.getCooldowns().isOnCooldown(ModItems.VIBRANIUM_SHIELD.get()), "not disabled");
        check(cap.isUsingItem() && ShieldRules.guarding(cap), "still up");
        helper.succeed();
    }

    /** An arrow shot at him from the front bounces back toward the skeleton that shot it. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anArrowFromTheFrontBouncesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 4.5, 10.5);
        look(cap, EAST, 0.0F);
        cap.startUsingItem(InteractionHand.OFF_HAND);
        var skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(14.5, FLOOR, 10.5));
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 from = helper.absoluteVec(new Vec3(8.5, FLOOR + 1.3, 10.5));
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(-1.0, 0.0, 0.0, 1.5F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        float before = cap.getHealth();
        until(helper, 20, () -> arrow.getDeltaMovement().x > 0.0, () -> {
            check(arrow.getOwner() == cap, "his now");
            check(arrow.getDeltaMovement().length() > 0.5, "bounced back fast: " + arrow.getDeltaMovement());
            assertNear(cap.getHealth(), before, 1.0E-4, "unhurt");
            arrow.discard();
            helper.succeed();
        }, cap);
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the throw and the ricochet

    /** R: the shield leaves his off hand, strikes a husk for 8, and is back in the off hand within 40 ticks. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void theThrowHitsAndComesBackToTheOffHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 4.5, 10.5);
        Husk husk = husk(helper, 10.5, 10.5);
        lookAt(cap, husk);
        press(cap, 1, false);
        ThrownShieldEntity shield = ShieldThrow.flying(cap);
        check(shield != null && cap.getOffhandItem().isEmpty(), "it flies; the hand is empty");
        check(ModAttachments.CAPTAIN.get(cap).has(CaptainState.OFF_HAND), "from the off hand");
        press(cap, 1, false);
        check(ShieldThrow.flying(cap) == shield, "no second throw while it flies");
        until(helper, 40, () -> shieldIn(cap, InteractionHand.OFF_HAND), () -> {
            assertNear(lost(husk), 8.0, 1.0E-4, "8");
            check(shield.isRemoved() && ShieldThrow.flying(cap) == null, "caught");
            check(!data(cap).isReady(HeroAbility.CAPTAIN_SHIELD_THROW, helper.getLevel().getGameTime()),
                    "the cooldown starts at the catch");
            helper.succeed();
        }, cap);
    }

    /** His off hand full when it comes back: it goes into his inventory. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void aFullHandSendsItToTheInventory(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 4.5, 10.5);
        look(cap, EAST, 0.0F);
        Husk husk = husk(helper, 9.5, 10.5);
        lookAt(cap, husk);
        press(cap, 1, false);
        ThrownShieldEntity shield = ShieldThrow.flying(cap);
        check(shield != null, "it flies");
        cap.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        until(helper, 40, shield::isRemoved, () -> {
            check(cap.getOffhandItem().is(Items.STICK), "the stick stays");
            check(cap.getInventory().contains(new ItemStack(ModItems.VIBRANIUM_SHIELD.get())), "in his inventory");
            helper.succeed();
        }, cap);
    }

    /** He leaves while it flies: it drops where it is, as an item. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theThrowerGoneItDrops(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 4.5, 10.5);
        look(cap, EAST, 0.0F);
        press(cap, 1, false);
        ThrownShieldEntity shield = ShieldThrow.flying(cap);
        check(shield != null, "it flies");
        helper.runAfterDelay(2, () -> {
            HeroLifecycle.onLogout(cap);
            helper.getLevel().getServer().getPlayerList().remove(cap);
        });
        helper.runAfterDelay(6, () -> {
            check(shield.isRemoved(), "no longer in flight");
            List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    shield.getBoundingBox().inflate(4.0), e -> e.getItem().is(ModItems.VIBRANIUM_SHIELD.get()));
            check(dropped.size() == 1, "it lies on the ground: " + dropped.size());
            dropped.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    /** Sneak + R: the shield strikes three husks in turn, 8, 7, 6, and comes home. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRicochetStrikesThree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 2.5, 10.5);
        Husk first = husk(helper, 6.5, 10.5);
        Husk second = husk(helper, 10.5, 12.5);
        Husk third = husk(helper, 13.5, 9.5);
        lookAt(cap, first);
        press(cap, 1, true);
        ThrownShieldEntity shield = ShieldThrow.flying(cap);
        check(shield != null && shield.ricochet(), "a ricochet");
        until(helper, 80, () -> shieldIn(cap, InteractionHand.OFF_HAND), () -> {
            assertNear(lost(first), 8.0, 1.0E-4, "the first 8");
            assertNear(lost(second), 7.0, 1.0E-4, "the second 7");
            assertNear(lost(third), 6.0, 1.0E-4, "the third 6");
            check(shield.strikes().equals(List.of(8.0F, 7.0F, 6.0F)), "in that order: " + shield.strikes());
            check(!data(cap).isReady(HeroAbility.CAPTAIN_RICOCHET, helper.getLevel().getGameTime()), "its cooldown");
            helper.succeed();
        }, cap);
    }

    /** A custom hero with the Shield Throw throws a plain shield: it comes back to his hand, and never ricochets. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void aCustomHeroThrowsAPlainShield(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("captain.shield_throw", 3, "both").key(3, "captain.shield_throw")
                .build();
        ServerPlayer hero = CustomKitSupport.hero(f, 4.5, 10.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        hero.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
        Husk husk = husk(helper, 9.5, 10.5);
        Husk beside = husk(helper, 12.5, 11.5);
        lookAt(hero, husk);
        check(ShieldThrow.start(hero, data(hero), true) || ShieldThrow.start(hero, data(hero), false), "it flies");
        ThrownShieldEntity shield = ShieldThrow.flying(hero);
        check(shield != null && !shield.ricochet() && shield.shield().is(Items.SHIELD), "a plain shield, no ricochet");
        until(helper, 40, () -> hero.getMainHandItem().is(Items.SHIELD), () -> {
            check(lost(husk) > 0.0F && lost(beside) == 0.0F, "one struck, no bounce");
            helper.succeed();
        }, hero);
    }

    // ------------------------------------------------------------------------------------------------------------
    // G, V: the slam, the leap, the charge, the bash

    /** G: the slam's shockwave hurts the two husks within 4 for 6, not the one at 6. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theSlamHitsWithinFour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        Husk near = husk(helper, 13.0, 10.5);
        Husk other = husk(helper, 10.5, 7.5);
        Husk far = husk(helper, 4.5, 10.5);
        press(cap, 2, false);
        check(data(cap).is(Live.CAP_SLAMMING), "in the air");
        until(helper, 40, () -> !data(cap).is(Live.CAP_SLAMMING), () -> {
            assertNear(lost(near), 6.0, 1.0E-4, "6 at 2.5");
            assertNear(lost(other), 6.0, 1.0E-4, "6 at 3");
            check(lost(far) == 0.0F, "nothing at 6");
            check(cap.getHealth() == cap.getMaxHealth(), "no fall damage");
            helper.succeed();
        }, cap);
    }

    /** V: the charge runs 8 blocks (unhindered) in about 12 ticks. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theChargeRunsEightBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 3.5, 10.5);
        look(cap, EAST, 0.0F);
        Vec3 start = cap.position();
        press(cap, 3, false);
        check(ShieldCharge.charging(cap) && data(cap).is(Live.CAP_CHARGING), "charging");
        until(helper, 30, () -> !ShieldCharge.charging(cap), () -> {
            double run = Math.hypot(cap.getX() - start.x, cap.getZ() - start.z);
            check(run >= 7.0 && run <= 9.0, "8 blocks: " + run);
            check(!data(cap).is(Live.CAP_CHARGING), "done");
            helper.succeed();
        }, cap);
    }

    /** A wall 4 blocks ahead stops the charge; he is not hurt; a husk in the way is struck and thrown. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aWallStopsTheCharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int y = 2; y <= 4; y++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.STONE);
            }
        }
        ServerPlayer cap = cap(f, 3.5, 4.5);
        look(cap, EAST, 0.0F);
        Vec3 start = cap.position();
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 4.5));
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        float health = cap.getHealth();
        press(cap, 3, false);
        until(helper, 30, () -> !ShieldCharge.charging(cap), () -> {
            double run = cap.getX() - start.x;
            check(run < 4.5, "stopped at the wall: " + run);
            check(cap.getHealth() >= health, "unhurt");
            check(zombie.getHealth() < zombie.getMaxHealth(), "the zombie struck");
            helper.succeed();
        }, cap);
    }

    /** Sneak + V: the bash strikes the husk in front for 5 and stuns it 30 ticks (Slowness IV, no jump). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBashStuns(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 5.5, 5.5);
        Husk husk = husk(helper, 7.5, 5.5);
        lookAt(cap, husk);
        press(cap, 3, true);
        assertNear(lost(husk), 5.0, 1.0E-4, "5");
        MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slow != null && slow.getAmplifier() == 3 && slow.getDuration() == 30, "Slowness IV for 30: " + slow);
        check(ShieldCharge.stunned(husk) && husk.getAttributeValue(Attributes.JUMP_STRENGTH) == 0.0, "no jump");
        helper.runAfterDelay(32, () -> {
            check(!ShieldCharge.stunned(husk) && husk.getAttributeValue(Attributes.JUMP_STRENGTH) > 0.0,
                    "the jump back");
            helper.succeed();
        });
    }

    /** Sneak + G: a long running leap forward; no fall damage when he lands. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void theSuperSoldierLeap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 3.5, 10.5);
        look(cap, EAST, 0.0F);
        Vec3 start = cap.position();
        float health = cap.getHealth();
        press(cap, 2, true);
        step(helper, 30, () -> {
            double run = cap.getX() - start.x;
            check(run >= 4.0, "far: " + run);
            check(cap.getHealth() >= health, "no fall damage");
            helper.succeed();
        }, cap);
    }

    // ------------------------------------------------------------------------------------------------------------
    // B, N: Assemble, Inspire, the combo, the flip kick

    /** B: a villager and a player near him get Strength I and Speed I for 400 ticks (and he); a zombie does not. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void assembleRalliesAllies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        ServerPlayer friend = f.player(14.5, FLOOR, 10.5);
        Villager villager = TestKit.spawnWithNoFreeWill(helper, EntityType.VILLAGER, new Vec3(10.5, FLOOR, 14.5));
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 10.5));
        int count = Assemble.assemble(cap, data(cap));
        check(count == 3, "him, the player and the villager: " + count);
        for (net.minecraft.world.entity.LivingEntity e : List.of(cap, friend, villager)) {
            MobEffectInstance strength = e.getEffect(MobEffects.DAMAGE_BOOST);
            check(strength != null && strength.getAmplifier() == 0 && strength.getDuration() == 400
                    && e.hasEffect(MobEffects.MOVEMENT_SPEED), e.getName().getString() + " rallied");
        }
        check(!zombie.hasEffect(MobEffects.DAMAGE_BOOST), "not the zombie");
        check(Assemble.assemble(cap, data(cap)) == -1, "its cooldown");
        helper.succeed();
    }

    /** Sneak + B: Inspire heals a hurt friend 6 and lifts his Weakness. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void inspireHeals(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        ServerPlayer friend = f.player(13.5, FLOOR, 10.5);
        friend.setHealth(10.0F);
        friend.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        press(cap, 4, true);
        assertNear(friend.getHealth(), 16.0, 1.0E-4, "healed 6");
        check(!friend.hasEffect(MobEffects.WEAKNESS), "no longer weak");
        helper.succeed();
    }

    /** N: two punches and a spinning kick, 4, 4, 7, at least 8 ticks apart. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theComboIsFourFourSeven(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 5.5, 5.5);
        Husk husk = husk(helper, 7.5, 5.5);
        lookAt(cap, husk);
        check(CaptainCombo.punch(cap, data(cap)) == 0, "the first");
        assertNear(lost(husk), 4.0, 1.0E-4, "4");
        check(CaptainCombo.punch(cap, data(cap)) == -1, "too soon");
        helper.runAfterDelay(8, () -> {
            check(CaptainCombo.punch(cap, data(cap)) == 1, "the second");
            assertNear(lost(husk), 8.0, 1.0E-4, "4 more");
        });
        helper.runAfterDelay(16, () -> {
            check(CaptainCombo.punch(cap, data(cap)) == 2, "the kick");
            assertNear(lost(husk), 15.0, 1.0E-4, "7 more");
        });
        helper.runAfterDelay(50, () -> {
            check(CaptainCombo.punch(cap, data(cap)) == 0, "after a pause, the first again");
            helper.succeed();
        });
    }

    /** Sneak + N: the flip kick strikes the husk in front for 5; he lands well back, unhurt. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theFlipKick(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        look(cap, EAST, 0.0F);
        Husk husk = husk(helper, 12.5, 10.5);
        Vec3 start = cap.position();
        float health = cap.getHealth();
        press(cap, 5, true);
        assertNear(lost(husk), 5.0, 1.0E-4, "5");
        step(helper, 25, () -> {
            double back = start.x - cap.getX();
            check(back >= 1.5 && back <= 5.0, "he lands back: " + back);
            check(cap.getHealth() >= health, "unhurt");
            helper.succeed();
        }, cap);
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP

    /** PvP off: his throw, slam and bash pass a player by. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void withoutPvpNoPlayerIsHurt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        ServerPlayer other = f.player(12.5, FLOOR, 10.5);
        float health = other.getHealth();
        lookAt(cap, other);
        press(cap, 3, true);
        press(cap, 2, false);
        press(cap, 1, false);
        until(helper, 40, () -> shieldIn(cap, InteractionHand.OFF_HAND) && !data(cap).is(Live.CAP_SLAMMING), () -> {
            assertNear(other.getHealth(), health, 1.0E-4, "unhurt");
            helper.succeed();
        }, cap);
    }

    /** PvP on: the bash and the slam hurt a player. */
    @EnhancedTest(template = ARENA, batch = BATCH_PVP, timeoutTicks = 60)
    public static void withPvpHisBlowsHurtAPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer cap = cap(f, 10.5, 10.5);
        ServerPlayer other = f.player(12.5, FLOOR, 10.5);
        float health = other.getHealth();
        lookAt(cap, other);
        press(cap, 3, true);
        check(other.getHealth() < health, "the bash");
        helper.succeed();
    }

    /** The shield is drawn on his back by everyone (a synced flag) while he carries it but does not hold it. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShieldOnHisBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = cap(f, 5.5, 5.5);
        check(!ModAttachments.CAPTAIN.get(cap).has(CaptainState.BACK), "in his hand: not on his back");
        ItemStack shield = cap.getOffhandItem();
        cap.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        cap.getInventory().setItem(20, shield);
        step(helper, 1, () -> {
            check(ModAttachments.CAPTAIN.get(cap).has(CaptainState.BACK), "in his inventory: on his back");
            helper.succeed();
        }, cap);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pure tables

    /** His gestures' keys and routes, his states' reasons, his rows' moves, his run, his payloads' gestures. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theAnimationTables(GameTestHelper helper) {
        int count = 0;
        for (com.kolodkingal.enhanced.client.render.GestureId id
                : com.kolodkingal.enhanced.client.render.GestureId.values()) {
            if (id.name().startsWith("CAP_")) {
                count++;
                var def = com.kolodkingal.enhanced.client.render.Gestures.get(id);
                check(def != null && def.problems().isEmpty(), id + " is well keyed");
                check(com.kolodkingal.enhanced.client.render.GestureRoutes.startedElsewhere(id),
                        id + " is started by his payloads");
            }
        }
        check(count == 15, "fifteen gestures: " + count);
        for (Live live : List.of(Live.CAP_CHARGING, Live.CAP_SLAMMING)) {
            String reason = com.kolodkingal.enhanced.client.render.GestureRoutes.reason(live);
            check(reason != null && !reason.startsWith("pending"), live + " is no longer pending: " + reason);
        }
        check(com.kolodkingal.enhanced.client.render.CaptainGestures.gestureOf(
                com.kolodkingal.enhanced.network.CaptainFxPayload.THROW, 1)
                == com.kolodkingal.enhanced.client.render.GestureId.CAP_THROW_OFF
                && com.kolodkingal.enhanced.client.render.CaptainGestures.gestureOf(
                com.kolodkingal.enhanced.network.CaptainFxPayload.SLAM, 2)
                == com.kolodkingal.enhanced.client.render.GestureId.CAP_SLAM_DOWN
                && com.kolodkingal.enhanced.client.render.CaptainGestures.gestureOf(
                com.kolodkingal.enhanced.network.CaptainFxPayload.BLOCK, 0) == null, "the payloads' gestures");
        check(com.kolodkingal.enhanced.client.custom.HeroAbilityAnims.shown(HeroAbility.CAPTAIN_COMBO, true)
                == com.kolodkingal.enhanced.client.render.GestureId.CAP_FLIP_KICK, "the variant");
        check(com.kolodkingal.enhanced.client.render.RunCharacter.of(Power.CAPTAIN_AMERICA, false)
                == com.kolodkingal.enhanced.client.render.RunCharacter.CAPTAIN, "his run");
        helper.succeed();
    }

    /** His preset reads, is his hero's, and fits 60 points with no note (the integrator lists it in HEROES). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPresetFitsSixtyPoints(GameTestHelper helper) throws java.io.IOException {
        java.nio.file.Path file = net.neoforged.fml.ModList.get().getModFileById(Enhanced.MODID).getFile()
                .findResource("assets", Enhanced.MODID, "hero_presets", "captain_america.json");
        String json = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        com.kolodkingal.enhanced.custom.HeroPresets.Preset preset =
                com.kolodkingal.enhanced.custom.HeroPresets.read("captain_america", json);
        check(preset != null && preset.like() == Power.CAPTAIN_AMERICA, "the preset reads, like Captain America");
        com.kolodkingal.enhanced.custom.CustomHeroRules.Result r = com.kolodkingal.enhanced.custom.CustomHeroRules
                .sanitize(preset.hero(), com.kolodkingal.enhanced.custom.CustomHeroRules.Limits.DEFAULT.withBudget(
                        com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        check(com.kolodkingal.enhanced.custom.CustomHeroRules.cost(r.hero())
                <= com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS, "60 points at most: "
                + com.kolodkingal.enhanced.custom.CustomHeroRules.cost(r.hero()));
        helper.succeed();
    }

    private CaptainTests() {
    }
}
