package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.ironman.IronManAbilities;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitControl;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitItemState;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitPhases;
import com.kolodkingal.enhanced.item.ArcReactorItem;
import com.kolodkingal.enhanced.item.IronSuitItem;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Iron Man's core (3.8, lane L3): the Arc Reactor held 40 ticks (not 39) makes Iron Man (a Hulk's size gone, the
 * reactor kept); the Nano Arc Reactor put on the chest does too; only Iron Man wears a suit; a suit with a reactor is
 * powered, without it not (armour on, flight off); flight and its speed per suit (the Mark 1 boosts instead); the
 * energy drained by flight, recharged on the ground after the delay, and empty ending flight with Slow Falling; H off
 * and on from the inventory, the Mark 5's suitcase; the phases (UP Mark 3 exactly 50 ticks, DOWN 25, damage halved in
 * UP only, the 40-tick settling, H skipping, death completing silently, a new watcher receiving the phase); Magneto and
 * the suits; a custom hero's suit row. Batches {@code iron_core} and {@code iron_suitup}.
 */
public final class IronCoreTests {
    private static final String BATCH = "iron_core";
    private static final String SUITUP = "iron_suitup";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Iron Man with nothing on, gone when the test ends. */
    static ServerPlayer tony(TestKit.Fixture f, double x, double z) {
        ServerPlayer tony = f.player(x, FLOOR, z, Power.IRON_MAN);
        f.onClose(() -> HeroLifecycle.onLogout(tony));
        tony.setHealth(tony.getMaxHealth());
        return tony;
    }

    /** Iron Man already in a classic suit (no suit-up), an Arc Reactor in his pocket, one tick on. */
    static ServerPlayer suited(TestKit.Fixture f, double x, double z, SuitKind kind) {
        ServerPlayer tony = tony(f, x, z);
        ItemStack suit = new ItemStack(IronSuits.item(kind));
        if (kind == SuitKind.MARK_5) {
            suit.set(ModDataComponents.SUIT.get(), SuitItemState.UNFOLDED);
        }
        tony.setItemSlot(EquipmentSlot.CHEST, suit);
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        IronSuits.setState(tony, IronSuits.state(tony).withSuit(kind));
        tony.doTick();
        return tony;
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    static IronManState state(ServerPlayer player) {
        return IronSuits.state(player);
    }

    /** H (Sneak: -1). */
    static void h(ServerPlayer player, boolean sneak) {
        IronSuits.onSuitKey(player, sneak ? -1 : 1);
    }

    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

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

    /** Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}. */
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

    /** Ticks the players for exactly {@code n} server ticks (n at least 1), then runs {@code then} in the last. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n, () -> ++count[0] >= n, then, players);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Becoming Iron Man

    /** Held 40 ticks (not 39) the Arc Reactor makes a Hulk Iron Man: his size is gone, the reactor kept. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theArcReactorMakesIronManAfterFortyTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bruce = f.player(5.5, FLOOR, 5.5, Power.HULK);
        f.onClose(() -> HeroLifecycle.onLogout(bruce));
        Powers.setVariant(bruce, Variants.get(Power.HULK, HulkAbilities.RAGNAROK));
        bruce.doTick();
        check(bruce.getAttributeValue(Attributes.SCALE) > 1.2, "the Hulk is big");
        ItemStack reactor = new ItemStack(ModItems.ARC_REACTOR.get());
        bruce.setItemInHand(InteractionHand.MAIN_HAND, reactor);
        check(ArcReactorItem.holdTicks() == 40, "two seconds");
        reactor.use(helper.getLevel(), bruce, InteractionHand.MAIN_HAND);
        check(bruce.isUsingItem(), "the hold begins");
        int[] ticks = {0};
        until(helper, 60, () -> {
            ticks[0]++;
            if (ticks[0] < 40) {
                check(Powers.power(bruce) == Power.HULK, "still the Hulk after " + ticks[0] + " ticks");
            }
            return Powers.power(bruce) == Power.IRON_MAN;
        }, () -> {
            check(ticks[0] == 40, "Iron Man on the 40th tick, not " + ticks[0]);
            bruce.doTick();
            assertNear(bruce.getAttributeValue(Attributes.SCALE), 1.0, 1.0E-6, "the Hulk's size is gone");
            check(bruce.getMainHandItem().is(ModItems.ARC_REACTOR.get()), "the reactor is kept");
            TestKit.sentPackets(bruce);
            bruce.getMainHandItem().use(helper.getLevel(), bruce, InteractionHand.MAIN_HAND);
            check(!bruce.isUsingItem() && told(bruce, "message.enhanced.iron_man.full_power"),
                    "as Iron Man it only glows");
            helper.succeed();
        }, bruce);
    }

    /** The Nano Arc Reactor put on the chest slot makes a powerless player Iron Man at once. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theNanoReactorOnTheChestMakesIronMan(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(5.5, FLOOR, 5.5);
        f.onClose(() -> HeroLifecycle.onLogout(peter));
        peter.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NANO_ARC_REACTOR.get()));
        peter.doTick();
        check(Powers.power(peter) == Power.IRON_MAN, "wearing it makes him Iron Man");
        check(IronSuits.nanoReactorOn(peter) && IronSuits.hasReactor(peter), "and it is his reactor");
        helper.succeed();
    }

    /** A powerless player cannot put a suit on (told why); Iron Man and a Creative builder can. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyIronManWearsASuit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(3.5, FLOOR, 3.5);
        ItemStack suit = new ItemStack(ModItems.IRON_MAN_MARK_3.get());
        IronSuitItem item = (IronSuitItem) suit.getItem();
        check(!item.canEquip(suit, EquipmentSlot.CHEST, plain), "not on a powerless player");
        plain.setItemInHand(InteractionHand.MAIN_HAND, suit);
        TestKit.sentPackets(plain);
        suit.use(helper.getLevel(), plain, InteractionHand.MAIN_HAND);
        check(plain.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "using it puts nothing on");
        check(told(plain, "message.enhanced.iron_man.only_iron_man"), "told: use an Arc Reactor first");
        ServerPlayer tony = tony(f, 9.5, 3.5);
        check(item.canEquip(suit, EquipmentSlot.CHEST, tony), "Iron Man may");
        ServerPlayer builder = f.player(15.5, FLOOR, 3.5, GameType.CREATIVE);
        check(item.canEquip(suit, EquipmentSlot.CHEST, builder), "so may a builder");
        ItemStack housing = new ItemStack(ModItems.IRON_MAN_MARK_85.get());
        check(!((IronSuitItem) housing.getItem()).canEquip(housing, EquipmentSlot.CHEST, tony),
                "a nanotech housing is never worn as an item");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Powered, flight, energy

    /** A suit and a reactor: powered (armour on, flight on). The reactor dropped: unpowered, flight off, armour on. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aSuitWorksOnlyWithAReactor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        check(IronSuits.powered(tony) && state(tony).has(IronManState.POWERED), "powered, and lit for everyone");
        assertNear(tony.getAttributeValue(Attributes.ARMOR), 16.0, 1.0E-6, "the Mark 3's armour, the whole set");
        assertNear(tony.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2.0, 1.0E-6, "and its toughness");
        check(tony.mayFly(), "he may fly");
        tony.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.ARC_REACTOR.get()), 1,
                tony.inventoryMenu.getCraftSlots());
        step(helper, 2, () -> {
            check(!IronSuits.powered(tony) && !state(tony).has(IronManState.POWERED), "no reactor: dark");
            check(!tony.mayFly(), "no flight");
            assertNear(tony.getAttributeValue(Attributes.ARMOR), 16.0, 1.0E-6, "the armour still protects");
            TestKit.sentPackets(tony);
            press(tony, 1, false);
            check(told(tony, "message.enhanced.iron_man.no_reactor"), "a key says why");
            helper.succeed();
        }, tony);
    }

    /** Flight and its speed per suit: Mark 3 0.10, Mark 5 0.08; the Mark 1 does not fly (G is its boost). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void flightIsPerSuit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer three = suited(f, 3.5, 3.5, SuitKind.MARK_3);
        ServerPlayer five = suited(f, 9.5, 3.5, SuitKind.MARK_5);
        ServerPlayer one = suited(f, 15.5, 3.5, SuitKind.MARK_1);
        assertNear(three.getAbilities().getFlyingSpeed(), 0.10, 1.0E-4, "the Mark 3's speed");
        assertNear(five.getAbilities().getFlyingSpeed(), 0.08, 1.0E-4, "the Mark 5's speed");
        check(three.mayFly() && five.mayFly(), "both fly");
        check(!one.mayFly() && IronSuits.powered(one), "the Mark 1 works but cannot fly");
        press(one, 2, false);
        check(data(one).is(Live.IRON_DASHING), "its G is the Rocket Boost");
        ServerPlayer bare = tony(f, 15.5, 12.5);
        TestKit.sentPackets(bare);
        press(bare, 2, false);
        check(!bare.getAbilities().flying && told(bare, "message.enhanced.iron_man.no_suit"), "unsuited: no suit");
        press(bare, 1, false);
        check(told(bare, "message.enhanced.iron_man.no_suit"), "every key says it");
        helper.succeed();
    }

    /** Flight drains 0.6 a second (Mark 3); on the ground it recharges 3 a second after 20 quiet ticks. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void energyDrainsInFlightAndRechargesOnTheGround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        SuitEnergy.set(tony, 50.0);
        tony.getAbilities().flying = true;
        tony.onUpdateAbilities();
        step(helper, 20, () -> {
            assertNear(SuitEnergy.get(tony), 50.0 - 0.6, 0.005, "a second of flight");
            tony.getAbilities().flying = false;
            tony.onUpdateAbilities();
            float landed = SuitEnergy.get(tony);
            step(helper, 18, () -> {
                assertNear(SuitEnergy.get(tony), landed, 1.0E-4, "nothing back within the delay");
                step(helper, 22, () -> {
                    float back = SuitEnergy.get(tony) - landed;
                    check(back > 0.15F * 18 && back < 0.15F * 26, "3 a second on the ground: " + back);
                    helper.succeed();
                }, tony);
            }, tony);
        }, tony);
    }

    /** The meter running dry in flight: the flight ends, with Slow Falling for a soft landing. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void emptyEnergyEndsFlightWithSlowFalling(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        SuitEnergy.set(tony, 0.05);
        tony.getAbilities().flying = true;
        tony.onUpdateAbilities();
        until(helper, 10, () -> !tony.getAbilities().flying, () -> {
            check(SuitEnergy.empty(tony), "empty");
            check(tony.hasEffect(MobEffects.SLOW_FALLING), "a soft landing");
            step(helper, 1, () -> {
                check(!tony.mayFly(), "no flight while empty");
                TestKit.sentPackets(tony);
                press(tony, 4, false);
                check(told(tony, "message.enhanced.iron_man.energy_empty"), "moves are refused");
                helper.succeed();
            }, tony);
        }, tony);
    }

    // ------------------------------------------------------------------------------------------------------------
    // H and the phases

    /** H takes a classic suit off (DOWN, 25 ticks): the suit goes into the inventory. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 200)
    public static void hTakesTheSuitOffIntoTheInventory(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        h(tony, false);
        check(state(tony).phase() == IronManState.DOWN && data(tony).is(Live.IRON_SUITING), "the take-off plays");
        check(state(tony).phaseTicks() == 25, "25 ticks: half the suit-up");
        int[] ticks = {0};
        until(helper, 40, () -> {
            ticks[0]++;
            if (ticks[0] < 25) {
                check(tony.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.IRON_MAN_MARK_3.get()),
                        "still on at tick " + ticks[0]);
            }
            return !state(tony).inPhase();
        }, () -> {
            check(ticks[0] == 25, "off on the 25th tick, not " + ticks[0]);
            check(tony.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "the chest is free");
            check(IronSuits.carries(tony, SuitKind.MARK_3), "the suit is in his inventory");
            check(state(tony).worn() == null && !data(tony).is(Live.IRON_SUITING), "unsuited");
            helper.succeed();
        }, tony);
    }

    /**
     * Unsuited, H suits him up from the inventory: the Mark 3 over the Mark 1 (the last worn first); the UP of the Mark
     * 3 lasts exactly 50 ticks: powered at 50, not 49; the suit is on the chest from its start.
     */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 200)
    public static void hSuitsUpAndTheMark3TakesFiftyTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_1.get()));
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        check(SuitControl.nextClassic(tony) == SuitKind.MARK_3, "the best: the Mark 3");
        IronSuits.setState(tony, state(tony).withLast(SuitKind.MARK_1));
        check(SuitControl.nextClassic(tony) == SuitKind.MARK_1, "but the last worn first");
        IronSuits.setState(tony, state(tony).withLast(SuitKind.MARK_3));
        h(tony, false);
        check(tony.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.IRON_MAN_MARK_3.get()),
                "on the chest from the start");
        check(state(tony).phase() == IronManState.UP && state(tony).phaseTicks() == 50, "the UP, 50 ticks");
        int[] ticks = {0};
        until(helper, 70, () -> {
            ticks[0]++;
            if (ticks[0] < 50) {
                check(!IronSuits.powered(tony), "not powered at tick " + ticks[0]);
            }
            return IronSuits.powered(tony);
        }, () -> {
            check(ticks[0] == 50, "powered on the 50th tick, not " + ticks[0]);
            check(!data(tony).is(Live.IRON_SUITING), "the phase is over");
            helper.succeed();
        }, tony);
    }

    /** The Mark 5's suitcase: used, it unfolds onto the chest; H folds it back into his hand. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 300)
    public static void theSuitcaseUnfoldsAndFoldsBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        ItemStack suitcase = new ItemStack(ModItems.IRON_MAN_MARK_5.get());
        check(IronSuitItem.folded(suitcase), "a suitcase in the hand");
        tony.setItemInHand(InteractionHand.MAIN_HAND, suitcase);
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        suitcase.use(helper.getLevel(), tony, InteractionHand.MAIN_HAND);
        ItemStack chest = tony.getItemBySlot(EquipmentSlot.CHEST);
        check(chest.is(ModItems.IRON_MAN_MARK_5.get()) && !IronSuitItem.folded(chest), "unfolded on the chest");
        check(tony.getMainHandItem().isEmpty(), "out of the hand");
        check(state(tony).phase() == IronManState.UP && state(tony).phaseTicks() == 40, "its 40-tick suit-up");
        until(helper, 60, () -> IronSuits.powered(tony), () -> step(helper, 40, () -> {
            h(tony, false);
            check(state(tony).phase() == IronManState.DOWN && state(tony).phaseTicks() == 20, "folding: 20 ticks");
            until(helper, 30, () -> !state(tony).inPhase(), () -> {
                ItemStack hand = tony.getMainHandItem();
                check(hand.is(ModItems.IRON_MAN_MARK_5.get()) && IronSuitItem.folded(hand), "a suitcase in his hand");
                check(tony.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "the chest is free");
                helper.succeed();
            }, tony);
        }, tony), tony);
    }

    /** During UP a blow is halved; after it, and during DOWN, it is whole. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 200)
    public static void theSuitUpHalvesDamageOnlyDuringUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        h(tony, false);
        float before = tony.getHealth();
        tony.hurt(tony.damageSources().magic(), 4.0F);
        assertNear(before - tony.getHealth(), 2.0, 1.0E-4, "halved during UP");
        tony.setHealth(tony.getMaxHealth());
        until(helper, 60, () -> IronSuits.powered(tony), () -> {
            tony.invulnerableTime = 0;
            float full = tony.getHealth();
            tony.hurt(tony.damageSources().magic(), 4.0F);
            assertNear(full - tony.getHealth(), 4.0, 1.0E-4, "whole once on");
            tony.setHealth(tony.getMaxHealth());
            step(helper, 40, () -> {
                h(tony, false);
                check(state(tony).phase() == IronManState.DOWN, "taking it off");
                tony.invulnerableTime = 0;
                float h = tony.getHealth();
                tony.hurt(tony.damageSources().magic(), 4.0F);
                assertNear(h - tony.getHealth(), 4.0, 1.0E-4, "whole during DOWN");
                helper.succeed();
            }, tony);
        }, tony);
    }

    /** A new phase within 40 ticks of the last one's end is refused ("still settling"); after them it begins. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 300)
    public static void aNewPhaseWaitsFortyTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        h(tony, false);
        until(helper, 60, () -> !state(tony).inPhase(), () -> {
            TestKit.sentPackets(tony);
            h(tony, false);
            check(!state(tony).inPhase(), "refused right after");
            check(told(tony, "message.enhanced.iron_man.settling"), "still settling");
            step(helper, 39, () -> {
                h(tony, false);
                check(!state(tony).inPhase(), "refused at 39");
                step(helper, 1, () -> {
                    h(tony, false);
                    check(state(tony).phase() == IronManState.DOWN, "begins at 40");
                    helper.succeed();
                }, tony);
            }, tony);
        }, tony);
    }

    /** H during UP skips it to its end: powered at once, and the protection is over that very tick. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 100)
    public static void hDuringUpSkipsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        h(tony, false);
        step(helper, 5, () -> {
            check(state(tony).phase() == IronManState.UP, "the UP plays");
            h(tony, false);
            check(!state(tony).inPhase() && IronSuits.powered(tony), "skipped: on and working");
            float before = tony.getHealth();
            tony.invulnerableTime = 0;
            tony.hurt(tony.damageSources().magic(), 4.0F);
            assertNear(before - tony.getHealth(), 4.0, 1.0E-4, "no protection any more");
            helper.succeed();
        }, tony);
    }

    /** Death mid-UP completes it silently: the suit is on, the phase gone. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 100)
    public static void deathMidUpCompletesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        h(tony, false);
        step(helper, 5, () -> {
            HeroLifecycle.onDeath(tony);
            check(!state(tony).inPhase() && !data(tony).is(Live.IRON_SUITING), "the phase is over");
            check(tony.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.IRON_MAN_MARK_3.get()), "the suit is on");
            helper.succeed();
        }, tony);
    }

    /** A player who starts watching him mid-phase is sent the phase (the attachment's first sync), at its tick. */
    @EnhancedTest(template = ARENA, batch = SUITUP, timeoutTicks = 100)
    public static void aNewWatcherReceivesThePhase(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = tony(f, 5.5, 5.5);
        tony.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        h(tony, false);
        long start = state(tony).phaseStart();
        step(helper, 10, () -> {
            ServerPlayer watcher = f.player(9.5, FLOOR, 5.5);
            List<Object> packets = TestHooks.get().initialSyncPackets(tony, watcher);
            check(packets.stream().anyMatch(p -> TestHooks.get().isAttachmentSync(p, ModAttachments.IRON_MAN,
                    tony.getId())), "his state is in the first sync");
            ServerPlayer copy = f.player(12.5, FLOOR, 5.5);
            packets.forEach(p -> TestHooks.get().receiveSync(p, copy));
            IronManState seen = ModAttachments.IRON_MAN.get(copy);
            check(seen.phase() == IronManState.UP && seen.phaseStart() == start && seen.phaseKind() == SuitKind.MARK_3,
                    "the watcher draws the UP from its start: " + seen);
            helper.succeed();
        }, tony);
    }

    // ------------------------------------------------------------------------------------------------------------
    // With the rest of the mod

    /** Magneto can take a powered Mark 3 (iron), not a deployed Mark 85 (nanites). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void magnetoTakesAMark3NotAMark85(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer three = suited(f, 3.5, 3.5, SuitKind.MARK_3);
        check(Metal.isMagnetic(three), "a powered Mark 3 is iron");
        ServerPlayer nano = tony(f, 9.5, 3.5);
        nano.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NANO_ARC_REACTOR.get()));
        nano.getInventory().add(new ItemStack(ModItems.IRON_MAN_MARK_85.get()));
        IronSuits.setState(nano, state(nano).withNanoSuit(SuitKind.MARK_85).with(IronManState.NANO_OUT, true)
                .withSuit(SuitKind.MARK_85));
        nano.doTick();
        check(IronSuits.worn(nano) == SuitKind.MARK_85 && IronSuits.powered(nano), "the Mark 85 is out and works");
        check(!Metal.isMagnetic(nano), "nanites are not iron");
        assertNear(nano.getAttributeValue(Attributes.ARMOR), 20.0, 1.0E-6, "a netherite set's armour, applied");
        nano.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        step(helper, 2, () -> {
            check(IronSuits.worn(nano) == null && !state(nano).has(IronManState.NANO_OUT),
                    "the reactor off: retracted");
            assertNear(nano.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "its armour gone with it");
            helper.succeed();
        }, nano);
    }

    /** Another hero set on him: the suit stops (items stay); his flight and lights are off. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void anotherHeroStopsTheSuit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        check(IronSuits.powered(tony), "powered");
        Powers.setPower(tony, Power.THOR);
        step(helper, 1, () -> {
            check(!IronSuits.powered(tony) && !state(tony).has(IronManState.POWERED), "off");
            check(tony.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.IRON_MAN_MARK_3.get()), "the item stays");
            helper.succeed();
        }, tony);
    }

    /** A custom hero's suit row: +8 armour and +4 toughness (no item), and the meter. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCustomHerosSuitRow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("iron_man.suit", 3).build();
        ServerPlayer hero = CustomKitSupport.hero(f, 5.5, 5.5, def);
        hero.doTick();
        float k = data(hero).bodyScale(HeroAbility.IRON_MAN_SUIT);
        check(k > 0.0F, "the row is his");
        assertNear(hero.getAttributeValue(Attributes.ARMOR), IronManAbilities.CUSTOM_SUIT_ARMOR * k, 1.0E-4,
                "the suit's armour");
        assertNear(hero.getAttributeValue(Attributes.ARMOR_TOUGHNESS), IronManAbilities.CUSTOM_SUIT_TOUGHNESS * k,
                1.0E-4, "and toughness");
        check(SuitEnergy.get(hero) > 0.0F, "the meter");
        helper.succeed();
    }

    /** His preset reads, is his hero's, and fits 60 points with no note (the integrator lists it in HEROES). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPresetFitsSixtyPoints(GameTestHelper helper) throws java.io.IOException {
        java.nio.file.Path file = Platform.get().modResource("assets/" + Enhanced.MODID
                + "/hero_presets/iron_man.json").orElseThrow();
        String json = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        com.kolodkingal.enhanced.custom.HeroPresets.Preset preset =
                com.kolodkingal.enhanced.custom.HeroPresets.read("iron_man", json);
        check(preset != null && preset.like() == Power.IRON_MAN, "the preset reads, like Iron Man");
        com.kolodkingal.enhanced.custom.CustomHeroRules.Result r = com.kolodkingal.enhanced.custom.CustomHeroRules
                .sanitize(preset.hero(), com.kolodkingal.enhanced.custom.CustomHeroRules.Limits.DEFAULT.withBudget(
                        com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        check(com.kolodkingal.enhanced.custom.CustomHeroRules.cost(r.hero())
                <= com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS, "60 points at most");
        helper.succeed();
    }

    /** The phases' lengths are the plan's (Appendix B). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void thePhaseLengthsAreThePlans(GameTestHelper helper) {
        int[] up = {60, 50, 40, 30, 24, 80};
        int[] down = {30, 25, 20, 15, 12, 40};
        for (SuitKind kind : SuitKind.VALUES) {
            check(SuitPhases.length(kind, true) == up[kind.ordinal()], kind + " UP");
            check(SuitPhases.length(kind, false) == down[kind.ordinal()], kind + " DOWN");
        }
        helper.succeed();
    }

    private IronCoreTests() {
    }
}
