package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.hulkbuster.Hulkbuster;
import com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterFrameEntity;
import com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterMoves;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * The Hulkbuster (3.8, lane L4, PLAN 6.4, 14): used under the open sky Veronica's frame falls, lands, and after 80
 * ticks the suit is on and he is 3.5×; under a roof she cannot reach him; put on the chest slot by hand the 40-tick
 * version plays; no room in the middle of the growth aborts it (his size again, out of any block); worn, explosions do
 * nothing and knockback resistance is full; the punches (6 × 4 + 10), the grab and the throw (10 when it lands),
 * Veronica's arm (+50 %, only under the sky), H takes it off (his size, the item in the inventory), and a death shrinks
 * him at once. Batch {@code hulkbuster}.
 */
public final class HulkbusterTests {
    private static final String BATCH = "hulkbuster";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Iron Man with an Arc Reactor in his pocket (the classic suits' power), full health (one tick). */
    static ServerPlayer ironMan(TestKit.Fixture f, double x, double z) {
        ServerPlayer tony = f.player(x, FLOOR, z, Power.IRON_MAN);
        f.onClose(() -> HeroLifecycle.onLogout(tony));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        tony.doTick();
        tony.setHealth(tony.getMaxHealth());
        return tony;
    }

    /**
     * The Hulkbuster put on by hand and its suit-up skipped (H) a tick later: worn and grown; then {@code then} (two
     * ticks on).
     */
    static void hulkbuster(GameTestHelper helper, TestKit.Fixture f, double x, double z,
            java.util.function.Consumer<ServerPlayer> then) {
        ServerPlayer tony = ironMan(f, x, z);
        tony.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get()));
        step(helper, 1, () -> {
            check(IronSuits.state(tony).phaseKind() == SuitKind.HULKBUSTER, "the suit-up began");
            IronSuits.onSuitKey(tony, 1);
            step(helper, 1, () -> {
                check(Hulkbuster.onChest(tony) && !IronSuits.state(tony).inPhase(), "worn");
                assertNear(tony.getScale(), 1.0 + Hulkbuster.scaleBonus(), 0.01, "3.5 times his size");
                check(IronSuits.powered(tony), "and working");
                then.accept(tony);
            }, tony);
        }, tony);
    }

    /** Takes the barrier ceiling off the arena (the sky is open), and puts it back when the test ends. */
    static void openTheSky(GameTestHelper helper, TestKit.Fixture f) {
        fill(helper, -1, 11, -1, 21, 11, 21, Blocks.AIR);
        f.onClose(() -> fill(helper, -1, 11, -1, 21, 11, 21, Blocks.BARRIER));
    }

    static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(Husk e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    static List<HulkbusterFrameEntity> frames(ServerPlayer player) {
        return player.level().getEntitiesOfClass(HulkbusterFrameEntity.class,
                player.getBoundingBox().inflate(8.0, 60.0, 8.0));
    }

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

    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Putting it on and taking it off

    /**
     * Used under the open sky: the frame falls and lands; after 80 ticks the suit is on and he is 3.5 times his size.
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void veronicaDropsTheFrameUnderTheOpenSky(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer tony = ironMan(f, 6.5, 10.5);
        look(tony, EAST, 0.0F);
        ItemStack suit = new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get());
        tony.setItemInHand(InteractionHand.MAIN_HAND, suit);
        suit.use(helper.getLevel(), tony, InteractionHand.MAIN_HAND);
        check(IronSuits.state(tony).phaseKind() == SuitKind.HULKBUSTER, "the suit-up began");
        check(frames(tony).size() == 1 && frames(tony).get(0).getY() > tony.getY() + 30, "the frame high above");
        int[] t = {0};
        until(helper, 100, () -> {
            t[0]++;
            if (t[0] == 35) {
                HulkbusterFrameEntity frame = frames(tony).isEmpty() ? null : frames(tony).get(0);
                check(frame != null && frame.stage() == HulkbusterFrameEntity.OPENING
                        && Math.abs(frame.getY() - tony.getY()) < 1.5, "it landed and opens");
            }
            if (t[0] < 79) {
                check(tony.getScale() < 3.49F, "not grown yet at " + t[0]);
            }
            return !IronSuits.state(tony).inPhase();
        }, () -> {
            check(t[0] >= 79 && t[0] <= 81, "the whole suit-up took 80 ticks: " + t[0]);
            check(Hulkbuster.onChest(tony), "the suit is on");
            assertNear(tony.getScale(), 3.5, 0.01, "3.5 times his size");
            step(helper, 2, () -> {
                check(frames(tony).isEmpty(), "the frame is gone");
                helper.succeed();
            }, tony);
        }, tony);
    }

    /** Under a roof Veronica cannot reach him: nothing begins, the suit stays in his hand. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void underARoofVeronicaCannotReachHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        fill(helper, 3, 9, 7, 12, 9, 14, Blocks.STONE);
        ServerPlayer tony = ironMan(f, 6.5, 10.5);
        look(tony, EAST, 0.0F);
        ItemStack suit = new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get());
        tony.setItemInHand(InteractionHand.MAIN_HAND, suit);
        TestKit.sentPackets(tony);
        suit.use(helper.getLevel(), tony, InteractionHand.MAIN_HAND);
        check(!IronSuits.state(tony).inPhase(), "no suit-up");
        check(!Hulkbuster.onChest(tony) && tony.getMainHandItem().is(ModItems.IRON_MAN_HULKBUSTER.get()),
                "still in his hand");
        check(frames(tony).isEmpty(), "no frame");
        helper.succeed();
    }

    /** Put on the chest slot by hand (under the arena's roof): the 40-tick version. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void byHandTheShortVersion(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = ironMan(f, 10.5, 10.5);
        tony.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get()));
        int[] t = {0};
        until(helper, 60, () -> {
            t[0]++;
            return t[0] > 1 && !IronSuits.state(tony).inPhase();
        }, () -> {
            check(t[0] >= 39 && t[0] <= 42, "40 ticks: " + t[0]);
            assertNear(tony.getScale(), 3.5, 0.01, "grown");
            helper.succeed();
        }, tony);
    }

    /** A block appears over him during the growth: the suit-up is aborted, he is his size, the suit comes off. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void noRoomAbortsTheGrowth(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer tony = ironMan(f, 6.5, 10.5);
        look(tony, EAST, 0.0F);
        ItemStack suit = new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get());
        tony.setItemInHand(InteractionHand.MAIN_HAND, suit);
        suit.use(helper.getLevel(), tony, InteractionHand.MAIN_HAND);
        int[] t = {0};
        until(helper, 100, () -> {
            t[0]++;
            if (t[0] == 60) {
                fill(helper, 4, 6, 7, 12, 6, 14, Blocks.STONE);
            }
            return !IronSuits.state(tony).inPhase();
        }, () -> {
            check(t[0] < 80, "aborted before its end: " + t[0]);
            assertNear(tony.getScale(), 1.0, 0.01, "his own size");
            check(!Hulkbuster.onChest(tony), "the suit came off");
            check(tony.getInventory().contains(s -> s.is(ModItems.IRON_MAN_HULKBUSTER.get())), "into the inventory");
            check(tony.level().noCollision(tony, tony.getBoundingBox().deflate(1.0E-3)), "out of any block");
            helper.succeed();
        }, tony);
    }

    /** H takes it off: after the 40-tick take-off he is his size and the suit is in his inventory. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hTakesItOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 10.5, 10.5, tony -> {
            step(helper, 41, () -> {
                IronSuits.onSuitKey(tony, 1);
                check(IronSuits.state(tony).phaseKind() == SuitKind.HULKBUSTER, "the take-off began");
                until(helper, 60, () -> !IronSuits.state(tony).inPhase(), () -> {
                    assertNear(tony.getScale(), 1.0, 0.01, "his own size");
                    check(!Hulkbuster.onChest(tony), "the suit is off");
                    check(tony.getInventory().contains(s -> s.is(ModItems.IRON_MAN_HULKBUSTER.get())),
                            "in the inventory");
                    helper.succeed();
                }, tony);
            }, tony);
        });
    }

    /** He dies in it: his own size at once. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void deathShrinksHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 10.5, 10.5, tony -> {
            tony.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            check(!tony.isAlive(), "dead");
            assertNear(Hulkbuster.current(tony), 0.0, 1.0E-6, "the growth is gone");
            assertNear(tony.getScale(), 1.0, 0.01, "his own size");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    /** Worn: a creeper's blast does nothing; knockback resistance is full. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void explosionImmuneAndUnmovable(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 10.5, 10.5, tony -> {
            assertNear(tony.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0, 0.01, "full knockback resistance");
            var creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(13.5, FLOOR, 10.5));
            Vec3 at = creeper.position();
            helper.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.NONE);
            check(tony.getHealth() == tony.getMaxHealth(), "the blast did nothing: " + tony.getHealth());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The moves

    /** R: five punches at the creature in front, 6 four times and a last 10. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void thePunchesSixFourTimesAndTen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 6.5, 10.5, tony -> {
            look(tony, EAST, 0.0F);
            Husk husk = husk(helper, 9.5, 10.5);
            husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            press(tony, 1);
            step(helper, 16, () -> {
                assertNear(lost(husk), 6.0 * 4 + 10.0, 0.01, "6 x 4 + 10");
                helper.succeed();
            }, tony);
        });
    }

    /** B grabs the creature in front, B again throws it; it takes 10 when it lands. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 140)
    public static void grabAndThrowLandsTen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 4.5, 10.5, tony -> {
            look(tony, EAST, 0.0F);
            Husk husk = husk(helper, 7.5, 10.5);
            press(tony, 4);
            check(HulkbusterMoves.held(tony) == husk && Powers.get(tony).is(Live.IRON_GRABBING), "grabbed");
            step(helper, 5, () -> {
                check(husk.getY() > tony.getY() + 1.0, "held up in his fist");
                press(tony, 4);
                check(HulkbusterMoves.held(tony) == null, "thrown");
                until(helper, 80, () -> lost(husk) > 0.0F, () -> {
                    assertNear(lost(husk), 10.0, 0.01, "10 when it lands");
                    check(husk.getX() > tony.getX() + 4.0, "away along his look");
                    helper.succeed();
                }, tony);
            }, tony);
        });
    }

    /** N under the sky: Veronica's arm repairs half the suit; under the roof she cannot reach him. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void veronicasArmRepairsHalfAndNeedsTheSky(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        hulkbuster(helper, f, 4.5, 4.5, roofed -> {
            ItemStack roofedSuit = roofed.getItemBySlot(EquipmentSlot.CHEST);
            roofedSuit.setDamageValue(2000);
            TestKit.sentPackets(roofed);
            press(roofed, 5);
            check(roofedSuit.getDamageValue() == 2000, "under the roof: nothing");
            openTheSky(helper, f);
            hulkbuster(helper, f, 14.5, 14.5, tony -> {
                ItemStack suit = tony.getItemBySlot(EquipmentSlot.CHEST);
                suit.setDamageValue(2000);
                press(tony, 5);
                step(helper, 22, () -> {
                    check(roofedSuit.getDamageValue() == 2000, "under the roof: still nothing");
                    int repaired = 2000 - suit.getDamageValue();
                    assertNear(repaired, suit.getMaxDamage() * 0.5, 1.0, "half of it repaired");
                    helper.succeed();
                }, tony, roofed);
            });
        });
    }
}
