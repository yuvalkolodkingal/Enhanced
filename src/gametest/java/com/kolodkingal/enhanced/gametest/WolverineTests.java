package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.client.render.WolverineGestures;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.item.AdamantiumInjectionItem;
import com.kolodkingal.enhanced.network.ScentPayload;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import com.kolodkingal.enhanced.wolverine.Adamantium;
import com.kolodkingal.enhanced.wolverine.Berserker;
import com.kolodkingal.enhanced.wolverine.ClawClimb;
import com.kolodkingal.enhanced.wolverine.ClawSlash;
import com.kolodkingal.enhanced.wolverine.Claws;
import com.kolodkingal.enhanced.wolverine.InjectionRecipe;
import com.kolodkingal.enhanced.wolverine.Pounce;
import com.kolodkingal.enhanced.wolverine.Senses;
import com.kolodkingal.enhanced.wolverine.WolverineAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
 * Wolverine (3.7, lane L2): the Adamantium Injection (60 ticks, not 59; a hero refused with nothing used; used up; the
 * option that lets it replace a power), his body (health, armour, speed, strength, metal for Magneto, the healing
 * factor's trait), the claws (the switch, the empty hand's 7 and its sweep), the Claw Slash (6/6/9, the gap, the reset,
 * the cross-slash's sweep of three, the push the Vormir Cliff credits), the Spin Slash (7 to three, not to a player
 * without PvP; with it, yes), the Pounce (onto the prey at 12 and the pin; a boss damage only; no prey: a plain leap;
 * no fall damage), the Feral Leap (far and low), the Berserker Rage (+40 %, items refused, the healing doubled, tired
 * after), the Sniff (the kinds of the scents, the radius), Track (a trail to a moving pig, ended by its death, a
 * tracked player told), Claw Climb (up a stone wall, not glass), his weight in water (he sinks, he cannot swim up, a
 * bubble column lifts him), a custom hero's rows on other keys, and the pure tables of his animation. Batches: {@code
 * wolverine}, {@code wolverine_pvp} (PvP and the injection's option: server-wide), {@code wolverine_water}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class WolverineTests {
    private static final String BATCH = "wolverine";
    private static final String BATCH_PVP = "wolverine_pvp";
    private static final String BATCH_WATER = "wolverine_water";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Wolverine at full health, in his body (one tick). */
    static ServerPlayer wolverine(TestKit.Fixture f, double x, double z) {
        ServerPlayer logan = f.player(x, FLOOR, z, Power.WOLVERINE);
        f.onClose(() -> HeroLifecycle.onLogout(logan));
        logan.doTick();
        logan.setHealth(logan.getMaxHealth());
        return logan;
    }

    /**
     * Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}; one
     * entry at a time in the test's schedule (as {@code HulkTests.until}).
     */
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

    /** Ticks the players once a server tick for {@code n} ticks, then runs {@code then}. */
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

    /** A husk with no armour and 200 health that stands where it is put (no blow moves it). */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
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

    /** Whether a message with this key reached the player since the last look (the action bar or the chat). */
    static boolean told(ServerPlayer player, String key) {
        for (Object sent : TestKit.sentPackets(player)) {
            Component text = sent instanceof ClientboundSetActionBarTextPacket bar ? bar.text()
                    : sent instanceof ClientboundSystemChatPacket chat ? chat.content() : null;
            if (text != null && text.getContents() instanceof TranslatableContents t && key.equals(t.getKey())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Adamantium Injection

    /** Held 60 ticks (not 59) the injection makes a powerless player Wolverine, and is used up; his claws come out. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theInjectionAwakensAfterSixtyTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = f.player(5.5, FLOOR, 5.5);
        f.onClose(() -> HeroLifecycle.onLogout(logan));
        ItemStack shot = new ItemStack(ModItems.ADAMANTIUM_INJECTION.get());
        logan.setItemInHand(InteractionHand.MAIN_HAND, shot);
        check(AdamantiumInjectionItem.holdTicks() == 60, "three seconds");
        shot.use(helper.getLevel(), logan, InteractionHand.MAIN_HAND);
        check(logan.isUsingItem(), "the hold begins");
        int[] ticks = {0};
        until(helper, 70, () -> {
            ticks[0]++;
            if (ticks[0] < 60) {
                check(Powers.power(logan) == Power.NONE, "still powerless after " + ticks[0] + " ticks");
            }
            return Powers.power(logan) == Power.WOLVERINE;
        }, () -> {
            check(ticks[0] == 60, "Wolverine on the 60th tick, not " + ticks[0]);
            check(logan.getMainHandItem().isEmpty(), "the injection is used up");
            check(Claws.out(data(logan)), "he wakes with the claws out");
            step(helper, Claws.AWAKENING_TICKS + 2, () -> {
                check(!Claws.out(data(logan)), "and they go back in after two seconds");
                helper.succeed();
            }, logan);
        }, logan);
    }

    /** A hero is refused at the first click: no hold, nothing used up; Deadpool too. Not craftable by default. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theInjectionRefusesAHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power power : new Power[]{Power.THOR, Power.DEADPOOL}) {
            ServerPlayer hero = f.player(power == Power.THOR ? 4.5 : 12.5, FLOOR, 6.5, power);
            f.onClose(() -> HeroLifecycle.onLogout(hero));
            TestKit.sentPackets(hero);
            ItemStack shot = new ItemStack(ModItems.ADAMANTIUM_INJECTION.get());
            hero.setItemInHand(InteractionHand.MAIN_HAND, shot);
            shot.use(helper.getLevel(), hero, InteractionHand.MAIN_HAND);
            check(!hero.isUsingItem(), power + ": no hold");
            check(shot.getCount() == 1 && Powers.power(hero) == power, power + ": nothing used, still himself");
            check(told(hero, power == Power.DEADPOOL ? "message.enhanced.wolverine.injection_deadpool"
                    : "message.enhanced.wolverine.injection_hero"), power + ": told why");
        }
        check(!InjectionRecipe.craftable(), "not craftable by default");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    /** Health 30, armour 4 and toughness 2, heavy, faster, stronger, metal for Magneto, the healing factor's trait. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void hisBodyIsAdamantium(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 5.5, 5.5);
        assertNear(logan.getMaxHealth(), 30.0, 1.0E-4, "health");
        assertNear(logan.getAttributeValue(Attributes.ARMOR), 4.0, 1.0E-4, "armour");
        assertNear(logan.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2.0, 1.0E-4, "toughness");
        assertNear(logan.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.4, 1.0E-4, "heavy");
        assertNear(logan.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.12, 1.0E-4, "+20 % speed");
        assertNear(logan.getAttributeValue(Attributes.ATTACK_DAMAGE), 3.0, 1.0E-4, "+2 strength");
        assertNear(logan.getAttributeValue(Attributes.JUMP_STRENGTH), 0.42 + 0.15, 1.0E-3, "jump");
        check(Metal.isMagnetic(logan), "Magneto lifts him by his adamantium");
        check(Traits.has(logan, Trait.HEALING_FACTOR), "the healing factor");
        // The body goes with the power.
        Powers.setPower(logan, Power.NONE);
        logan.doTick();
        assertNear(logan.getMaxHealth(), 20.0, 1.0E-4, "a cured Wolverine's health");
        assertNear(logan.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-4, "no adamantium left");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The claws

    /** G: the claws out; a blow of the empty hand is 7 and sweeps the one beside; in again: 3; an item hides them. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theClawsMakeTheEmptyHandHitSeven(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 5.5, 5.5);
        Husk a = husk(helper, 7.0, 5.5);
        Husk beside = husk(helper, 7.0, 6.9);
        lookAt(logan, a);
        press(logan, 2, false);
        check(Claws.out(data(logan)) && data(logan).toggle(Toggle.CLAWS_OUT), "snikt");
        TestKit.chargeAttack(logan);
        logan.attack(a);
        assertNear(lost(a), 7.0, 0.01, "1 + 2 + 4");
        assertNear(lost(beside), 2.0, 0.01, "the sweep");
        press(logan, 2, false);
        check(!Claws.out(data(logan)), "shink");
        step(helper, 21, () -> {
            float before = lost(a);
            TestKit.chargeAttack(logan);
            logan.attack(a);
            assertNear(lost(a) - before, 3.0, 0.01, "claws in: 1 + 2");
            press(logan, 2, false);
            logan.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            step(helper, 21, () -> {
                float again = lost(a);
                TestKit.chargeAttack(logan);
                logan.attack(a);
                assertNear(lost(a) - again, 3.0, 0.01, "an item in the hand hides its claws");
                helper.succeed();
            }, logan);
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the Claw Slash and the Spin Slash

    /** 6, 6, 9; too soon is nothing; 30 ticks later the combo starts again; the claws pop out for it. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSlashComboIsSixSixNine(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 5.5, 5.5);
        Husk husk = husk(helper, 7.5, 5.5);
        lookAt(logan, husk);
        check(!Claws.out(data(logan)), "the claws are in");
        check(ClawSlash.slash(logan, data(logan)) == 0, "the first hit");
        check(Claws.out(data(logan)), "they popped out for it");
        assertNear(lost(husk), 6.0, 0.01, "6");
        check(ClawSlash.slash(logan, data(logan)) == -1, "too soon: nothing");
        step(helper, 10, () -> {
            check(ClawSlash.slash(logan, data(logan)) == 1, "the second hit");
            assertNear(lost(husk), 12.0, 0.01, "6 + 6");
            step(helper, 10, () -> {
                check(ClawSlash.slash(logan, data(logan)) == 2, "the cross-slash");
                assertNear(lost(husk), 21.0, 0.01, "6 + 6 + 9");
                check(logan.getUUID().equals(VormirTracker.lastCredit(husk).pusher()),
                        "its push is his (the Vormir Cliff credits him)");
                step(helper, 32, () -> {
                    check(ClawSlash.slash(logan, data(logan)) == 0, "after 30 ticks the combo starts again");
                    helper.succeed();
                }, logan);
            }, logan);
        }, logan);
    }

    /** The cross-slash sweeps the three nearest of the cone; the first two hits strike one. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theCrossSlashSweepsThree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 10.5);
        look(logan, EAST, 0.0F);
        Husk first = husk(helper, 5.0, 10.5);
        Husk second = husk(helper, 5.8, 11.3);
        Husk third = husk(helper, 5.8, 9.7);
        Husk fourth = husk(helper, 6.6, 10.5);
        ClawSlash.slash(logan, data(logan));
        step(helper, 10, () -> {
            ClawSlash.slash(logan, data(logan));
            step(helper, 10, () -> {
                ClawSlash.slash(logan, data(logan));
                assertNear(lost(first), 21.0, 0.01, "the nearest: 6 + 6 + 9");
                assertNear(lost(second), 9.0, 0.01, "the second nearest: the cross only");
                assertNear(lost(third), 9.0, 0.01, "the third nearest: the cross only");
                check(lost(fourth) == 0.0F, "the fourth: no more than three");
                helper.succeed();
            }, logan);
        }, logan);
    }

    /** The Spin Slash: 7 to three husks round him, nothing to a player without PvP; its cooldown starts. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSpinHitsEveryoneRoundHimButAFriend(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 10.5, 10.5);
        Husk a = husk(helper, 12.5, 10.5);
        Husk b = husk(helper, 9.0, 12.0);
        Husk c = husk(helper, 10.5, 8.0);
        Husk far = husk(helper, 16.5, 10.5);
        ServerPlayer friend = f.player(8.5, FLOOR, 9.5);
        List<LivingEntity> struck = ClawSlash.spin(logan, data(logan));
        check(struck != null && struck.size() == 3, "three struck: " + struck);
        for (Husk h : new Husk[]{a, b, c}) {
            assertNear(lost(h), 7.0, 0.01, "7 round him");
        }
        check(lost(far) == 0.0F, "not beyond 3");
        check(lost(friend) == 0.0F, "PvP off spares a player");
        check(ClawSlash.spin(logan, data(logan)) == null, "the cooldown");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Pounce and the Feral Leap

    /** The Pounce lands beside a husk 9 blocks away, stabs it for 12 and pins it; no fall damage. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void thePounceStabsAndPins(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 3.5);
        Husk prey = husk(helper, 12.5, 9.5);
        lookAt(logan, prey);
        float health = logan.getHealth();
        press(logan, 3, false);
        check(Pounce.leaping(logan), "he crouches");
        until(helper, 60, () -> !Pounce.leaping(logan), () -> {
            check(Pounce.struck(logan), "he struck");
            assertNear(lost(prey), 12.0, 0.01, "the stab: 12");
            double gap = Math.hypot(logan.getX() - prey.getX(), logan.getZ() - prey.getZ());
            check(gap <= 1.6, "he landed beside it: " + gap);
            MobEffectInstance slow = prey.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slow != null && slow.getAmplifier() == 3 && Pounce.pinned(logan, prey), "pinned");
            check(logan.getHealth() >= health, "no fall damage");
            check(!data(logan).is(Live.WOLVERINE_POUNCING), "the state is over");
            helper.succeed();
        }, logan);
    }

    /** A boss takes the stab's damage, not the pin. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void aBossIsNotPinned(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 10.5);
        ElderGuardian boss = helper.spawnWithNoFreeWill(EntityType.ELDER_GUARDIAN, new Vec3(10.5, FLOOR, 10.5));
        lookAt(logan, boss);
        check(Pounce.pounce(logan, data(logan)), "the pounce");
        until(helper, 60, () -> !Pounce.leaping(logan), () -> {
            assertNear(lost(boss), 12.0, 0.05, "12 to the boss");
            check(!boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "not pinned");
            helper.succeed();
        }, logan);
    }

    /** No prey in sight: a plain leap to where he looks, without the stab. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void noPreyIsAPlainLeap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 10.5);
        Vec3 aim = helper.absoluteVec(new Vec3(11.5, FLOOR, 10.5));
        lookAt(logan, aim);
        Vec3 start = logan.position();
        check(Pounce.pounce(logan, data(logan)), "the leap");
        until(helper, 60, () -> !Pounce.leaping(logan), () -> {
            check(!Pounce.struck(logan), "nothing to stab");
            double travelled = Math.hypot(logan.getX() - start.x, logan.getZ() - start.z);
            check(travelled >= 6.0, "he leapt: " + travelled);
            helper.succeed();
        }, logan);
    }

    /** The Feral Leap: far and low. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theFeralLeapIsFarAndLow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 10.5);
        look(logan, EAST, 0.0F);
        Vec3 start = logan.position();
        double[] top = {start.y};
        float health = logan.getHealth();
        press(logan, 3, true);
        check(Pounce.leaping(logan), "the leap");
        until(helper, 60, () -> {
            top[0] = Math.max(top[0], logan.getY());
            return !Pounce.leaping(logan);
        }, () -> {
            double travelled = Math.hypot(logan.getX() - start.x, logan.getZ() - start.z);
            check(travelled >= 7.0, "far: " + travelled);
            check(top[0] - start.y <= 4.0 && top[0] - start.y >= 1.0, "and low: " + (top[0] - start.y));
            check(logan.getHealth() >= health, "no fall damage");
            helper.succeed();
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Berserker Rage

    /** The Rage: a slash of 6 is 8.4, items are refused, the claws stay out; at its end he is tired. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRageHitsHarderRefusesItemsAndTires(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 5.5, 5.5);
        Husk husk = husk(helper, 7.5, 5.5);
        lookAt(logan, husk);
        press(logan, 4, false);
        check(Berserker.raging(logan), "the Rage");
        check(Claws.out(data(logan)), "the claws are out");
        ClawSlash.slash(logan, data(logan));
        assertNear(lost(husk), 8.4, 0.01, "6 x 1.4");
        logan.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        logan.getFoodData().setFoodLevel(10);
        logan.startUsingItem(InteractionHand.MAIN_HAND);
        check(!logan.isUsingItem(), "too angry to eat");
        press(logan, 2, false);
        check(Claws.out(data(logan)), "the claws stay out in the Rage");
        // (The Rage made short: it ends in 3 ticks.)
        ModAttachments.WOLVERINE.set(logan, Berserker.get(logan).withBerserkUntil(
                helper.getLevel().getGameTime() + 3));
        step(helper, 6, () -> {
            check(!Berserker.raging(logan), "over");
            check(logan.hasEffect(MobEffects.WEAKNESS), "tired");
            logan.stopUsingItem();
            logan.startUsingItem(InteractionHand.MAIN_HAND);
            check(logan.isUsingItem(), "he may eat again");
            helper.succeed();
        }, logan);
    }

    /** In the Rage he heals at least the factor's always-on rate more than out of it. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRageDoublesTheHealing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer angry = wolverine(f, 4.5, 4.5);
        ServerPlayer calm = wolverine(f, 14.5, 14.5);
        for (ServerPlayer p : new ServerPlayer[]{angry, calm}) {
            p.getFoodData().setFoodLevel(17); // (no natural regeneration)
            p.setHealth(10.0F);
        }
        Berserker.start(angry, data(angry));
        step(helper, 40, () -> {
            float more = (angry.getHealth() - 10.0F) - (calm.getHealth() - 10.0F);
            check(more >= 1.9F, "the Rage heals 1/s more at least: " + more);
            helper.succeed();
        }, angry, calm);
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the Sniff and Track

    /** The Sniff: each creature's kind of scent; the radius is 48 (40 in, 50 out); the cooldown. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSniffKnowsEachScent(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 10.5, 10.5);
        Husk husk = husk(helper, 13.5, 10.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, FLOOR, 10.5));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.5, FLOOR, 14.5));
        Pig poisoned = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(10.5, FLOOR, 6.5));
        poisoned.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        ServerPlayer friend = f.player(15.5, FLOOR, 15.5);
        List<ScentPayload.Scent> scents = Senses.sniff(logan, data(logan));
        check(scents != null, "he sniffs");
        check(kind(scents, husk) == ScentPayload.MONSTER && kind(scents, pig) == ScentPayload.ANIMAL
                && kind(scents, villager) == ScentPayload.VILLAGER && kind(scents, poisoned) == ScentPayload.POISONED
                && kind(scents, friend) == ScentPayload.PLAYER, "each scent its kind: " + scents);
        check(kind(scents, logan) == -1, "not himself");
        double radius = WolverineConfig.SNIFF_RADIUS.get();
        check(40.0 <= radius && 50.0 > radius, "a zombie at 40 is smelled, at 50 not (radius " + radius + ")");
        check(Senses.sniff(logan, data(logan)) == null, "the cooldown");
        helper.succeed();
    }

    private static int kind(List<ScentPayload.Scent> scents, LivingEntity e) {
        for (ScentPayload.Scent s : scents) {
            if (s.entityId() == e.getId()) {
                return s.kind();
            }
        }
        return -1;
    }

    /** Track: a trail to a pig that moves, ended by its death (the switch off, the cooldown); a player is told. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void trackFollowsAPigUntilItDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = wolverine(f, 3.5, 3.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 9.5));
        lookAt(logan, pig);
        press(logan, 5, true);
        check(pig.getUUID().equals(Senses.tracked(logan)) && data(logan).toggle(Toggle.SCENT_TRACK), "tracking");
        Vec3 moved = helper.absoluteVec(new Vec3(16.5, FLOOR, 4.5));
        pig.teleportTo(moved.x, moved.y, moved.z);
        step(helper, 22, () -> {
            List<Vec3> trail = Senses.lastTrail(logan);
            check(!trail.isEmpty() && trail.get(trail.size() - 1).distanceTo(pig.position().add(0, 0.2, 0)) < 0.5,
                    "the trail ends at the pig where it is now: " + trail);
            check(trail.size() >= 2 && trail.get(0).distanceTo(logan.position()) <= 2.5, "and starts at him");
            pig.kill();
            step(helper, 2, () -> {
                check(Senses.tracked(logan) == null && !data(logan).toggle(Toggle.SCENT_TRACK), "it died: over");
                check(!data(logan).isReady(HeroAbility.WOLVERINE_TRACK, helper.getLevel().getGameTime()),
                        "the cooldown after");
                ServerPlayer other = wolverine(f, 15.5, 15.5);
                ServerPlayer prey = f.player(17.5, FLOOR, 17.5);
                TestKit.sentPackets(prey);
                Senses.track(other, data(other), prey);
                check(told(prey, "message.enhanced.wolverine.tracked"), "a tracked player is told");
                helper.succeed();
            }, logan);
        }, logan);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + G: Claw Climb

    /** Up a stone wall by his claws; not up glass; the climb needs the claws. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void clawClimbGoesUpStoneNotGlass(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int y = 2; y <= 9; y++) {
            for (int z = 3; z <= 6; z++) {
                helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
                helper.setBlock(new BlockPos(6, y, z + 8), Blocks.GLASS);
            }
        }
        ServerPlayer stone = wolverine(f, 5.65, 4.5);
        ServerPlayer glass = wolverine(f, 5.65, 12.5);
        for (ServerPlayer p : new ServerPlayer[]{stone, glass}) {
            look(p, EAST, 0.0F);
            check(!Claws.toggleClimb(p, data(p)) || Claws.out(data(p)), "the climb brings the hero's claws out");
            check(ClawClimb.on(p, data(p)), "climbing is on");
            p.zza = 1.0F;
        }
        check(ClawClimb.wall(stone) != null, "his claws bite the stone");
        check(ClawClimb.wall(glass) == null, "not glass");
        double stoneY = stone.getY();
        double glassY = glass.getY();
        step(helper, 20, () -> {
            check(stone.getY() - stoneY >= 1.5, "up the stone wall: " + (stone.getY() - stoneY));
            check(glass.getY() - glassY < 0.1, "not up the glass: " + (glass.getY() - glassY));
            helper.succeed();
        }, stone, glass);
    }

    // ------------------------------------------------------------------------------------------------------------
    // A custom hero

    /** A custom hero with his slash on G and his claws on R: each works on its own key; his body heals and is metal. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aCustomHeroHasHisRowsOnOtherKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("wolverine.slash", 3).key(2, "wolverine.slash")
                .pick("wolverine.claws", 3).key(1, "wolverine.claws").pick("wolverine.body", 3).build();
        ServerPlayer hero = CustomKitSupport.hero(f, 5.5, 5.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        CustomKitSupport.tickN(hero, 1);
        Husk husk = husk(helper, 7.5, 5.5);
        lookAt(hero, husk);
        CustomKitSupport.tap(hero, 1, false);
        check(Claws.out(data(hero)), "R: his claws");
        CustomKitSupport.tap(hero, 2, false);
        check(lost(husk) > 0.0F, "G: his slash: " + lost(husk));
        check(!data(hero).isReady(2, helper.getLevel().getGameTime()) || ClawSlash.combo(hero) == 0,
                "the slash's own key");
        check(hero.getMaxHealth() > 20.0F && Traits.has(hero, Trait.HEALING_FACTOR) && Metal.isMagnetic(hero),
                "his body: health, the healing factor, metal");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pure tables

    /** His gestures' keys and routes, his states' gestures, his rows' moves, his run, his payloads' gestures. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theAnimationTables(GameTestHelper helper) {
        int count = 0;
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("WOLVERINE_")) {
                count++;
                check(Gestures.get(id) != null && Gestures.get(id).problems().isEmpty(), id + " is well keyed");
                check(GestureRoutes.startedElsewhere(id), id + " is started by his payloads, states or switches");
            }
        }
        check(count == 14, "fourteen gestures: " + count);
        check(GestureRoutes.toggle(Power.WOLVERINE, Toggle.CLAWS_OUT, true) == GestureId.WOLVERINE_SNIKT
                && GestureRoutes.toggle(Power.WOLVERINE, Toggle.CLAWS_OUT, false) == GestureId.WOLVERINE_SHINK,
                "snikt and shink");
        check(GestureRoutes.live(Power.WOLVERINE, Live.WOLVERINE_BERSERK, false) == GestureId.WOLVERINE_BERSERK_ROAR
                && GestureRoutes.live(Power.WOLVERINE, Live.WOLVERINE_POUNCING, false)
                == GestureId.WOLVERINE_POUNCE_CROUCH, "his states' gestures");
        check(GestureRoutes.reason(Live.WOLVERINE_BERSERK) == null, "no longer pending");
        check(WolverineGestures.gestureOf(WolverineFxPayload.SLASH, 2) == GestureId.WOLVERINE_SLASH_3
                && WolverineGestures.gestureOf(WolverineFxPayload.POUNCE, 1) == GestureId.WOLVERINE_FERAL_LEAP
                && WolverineGestures.gestureOf(WolverineFxPayload.POUNCE_HIT, 0) == null, "the payloads' gestures");
        check(HeroAbilityAnims.shown(HeroAbility.WOLVERINE_SLASH, true) == GestureId.WOLVERINE_SPIN, "the variant");
        check(RunCharacter.of(Power.WOLVERINE, false) == RunCharacter.WOLVERINE, "his run");
        check(Adamantium.heavy() && WolverineAbilities.SILVER == 0xD9DEE3, "heavy, silver");
        helper.succeed();
    }

    /** His preset reads, is his hero's, and fits 60 points with no note (the integrator lists it in HEROES). */
    @GameTest(template = ARENA, batch = BATCH)
    public static void hisPresetFitsSixtyPoints(GameTestHelper helper) throws java.io.IOException {
        java.nio.file.Path file = net.neoforged.fml.ModList.get().getModFileById(Enhanced.MODID).getFile()
                .findResource("assets", Enhanced.MODID, "hero_presets", "wolverine.json");
        String json = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        com.kolodkingal.enhanced.custom.HeroPresets.Preset preset =
                com.kolodkingal.enhanced.custom.HeroPresets.read("wolverine", json);
        check(preset != null && preset.like() == Power.WOLVERINE, "the preset reads, like Wolverine");
        com.kolodkingal.enhanced.custom.CustomHeroRules.Result r = com.kolodkingal.enhanced.custom.CustomHeroRules
                .sanitize(preset.hero(), com.kolodkingal.enhanced.custom.CustomHeroRules.Limits.DEFAULT.withBudget(
                        com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        check(com.kolodkingal.enhanced.custom.CustomHeroRules.cost(r.hero())
                <= com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS, "60 points at most");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP and the injection's option (server-wide)

    /** With PvP on, the spin and a slash hurt a player. */
    @GameTest(template = ARENA, batch = BATCH_PVP)
    public static void withPvpHisClawsHurtAPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer logan = wolverine(f, 10.5, 10.5);
        ServerPlayer foe = f.player(12.0, FLOOR, 10.5);
        lookAt(logan, foe);
        ClawSlash.slash(logan, data(logan));
        check(lost(foe) > 0.0F, "the slash");
        float after = lost(foe);
        ClawSlash.spin(logan, data(logan));
        check(lost(foe) > after, "the spin");
        helper.succeed();
    }

    /** With {@code heroesRefused} off the injection replaces a hero's power. */
    @GameTest(template = ARENA, batch = BATCH_PVP, timeoutTicks = 120)
    public static void theOptionLetsTheInjectionReplaceAPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(WolverineConfig.INJECTION_HEROES_REFUSED, false);
        ServerPlayer thor = f.player(5.5, FLOOR, 5.5, Power.THOR);
        f.onClose(() -> HeroLifecycle.onLogout(thor));
        ItemStack shot = new ItemStack(ModItems.ADAMANTIUM_INJECTION.get());
        thor.setItemInHand(InteractionHand.MAIN_HAND, shot);
        shot.use(helper.getLevel(), thor, InteractionHand.MAIN_HAND);
        check(thor.isUsingItem(), "the hold begins");
        until(helper, 80, () -> Powers.power(thor) == Power.WOLVERINE, helper::succeed, thor);
    }

    // ------------------------------------------------------------------------------------------------------------
    // His weight in water

    /** In deep water he sinks; he cannot swim up faster than 0.02; a boat would carry him (not tested here). */
    @GameTest(template = ARENA, batch = BATCH_WATER, timeoutTicks = 100)
    public static void heSinksInWater(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        pool(helper, 8, 8, false);
        ServerPlayer logan = wolverine(f, 9.5, 9.5);
        Vec3 at = helper.absoluteVec(new Vec3(9.5, 7.0, 9.5));
        logan.teleportTo(at.x, at.y, at.z);
        double startY = logan.getY();
        step(helper, 15, () -> {
            check(logan.getY() < startY - 1.0, "he sank: " + (startY - logan.getY()));
            logan.setDeltaMovement(0, 0.5, 0);
            check(Adamantium.sink(logan), "his weight pulls him down");
            check(logan.getDeltaMovement().y <= 0.02 - 0.03 + 1.0E-6, "no faster up than 0.02, less his weight: "
                    + logan.getDeltaMovement().y);
            helper.succeed();
        }, logan);
    }

    /** A bubble column lifts him whatever he weighs. */
    @GameTest(template = ARENA, batch = BATCH_WATER, timeoutTicks = 100)
    public static void aBubbleColumnLiftsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        pool(helper, 8, 8, true);
        ServerPlayer logan = wolverine(f, 9.5, 9.5);
        Vec3 at = helper.absoluteVec(new Vec3(9.5, 3.0, 9.5));
        logan.teleportTo(at.x, at.y, at.z);
        check(Adamantium.inBubbles(logan), "in the bubbles");
        double startY = logan.getY();
        step(helper, 15, () -> {
            check(!Adamantium.sink(logan), "the bubbles win");
            check(logan.getY() > startY + 1.0, "lifted: " + (logan.getY() - startY));
            helper.succeed();
        }, logan);
    }

    /**
     * A 3 x 3 pool of water 7 deep round (x, z) (glass walls round it, from the floor up), or a column of rising
     * bubbles over soul sand at its middle.
     */
    private static void pool(GameTestHelper helper, int x0, int z0, boolean bubbles) {
        for (int y = 1; y <= 8; y++) {
            for (int dx = -1; dx <= 3; dx++) {
                for (int dz = -1; dz <= 3; dz++) {
                    BlockPos pos = new BlockPos(x0 + dx, y, z0 + dz);
                    boolean wall = dx == -1 || dx == 3 || dz == -1 || dz == 3;
                    if (wall) {
                        helper.setBlock(pos, Blocks.GLASS);
                    } else if (y == 1) {
                        helper.setBlock(pos, bubbles && dx == 1 && dz == 1 ? Blocks.SOUL_SAND : Blocks.STONE);
                    } else {
                        helper.setBlock(pos, bubbles && dx == 1 && dz == 1 ? Blocks.BUBBLE_COLUMN.defaultBlockState()
                                .setValue(BubbleColumnBlock.DRAG_DOWN, false) : Blocks.WATER.defaultBlockState());
                    }
                }
            }
        }
    }

    private WolverineTests() {
    }
}
