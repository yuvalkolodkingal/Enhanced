package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.block.GammaEmitterBlock;
import com.kolodkingal.enhanced.block.GammaEmitterBlockEntity;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.HulkGestures;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.hulk.GammaCharge;
import com.kolodkingal.enhanced.hulk.GammaEmitterRecipe;
import com.kolodkingal.enhanced.hulk.GammaExposure;
import com.kolodkingal.enhanced.hulk.GrabSmash;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.hulk.HulkHooks;
import com.kolodkingal.enhanced.hulk.HulkRage;
import com.kolodkingal.enhanced.hulk.HulkState;
import com.kolodkingal.enhanced.hulk.HulkTerrain;
import com.kolodkingal.enhanced.hulk.SuperLeap;
import com.kolodkingal.enhanced.hulk.ThunderClap;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EventHub;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The Hulk (3.6, lane L7): the Gamma Emitter's accident (60 ticks in the beam, not 59; a hero refused, a Hulk healed,
 * redstone), Bruce Banner (no keys, no body), the transformation both ways (the scale grows, health 40 at the end),
 * Switch Hulk (and the Brand New Day factor), no room to grow, anger and the inhibitor; every key (Smash to everyone on
 * the ground and nobody in the air, Grab Smash 4 + 4 + 6 and a boss's punch, the Clap's throw, fire and arrow, the
 * Roar, the Super Leap's landing, the Bound, the Boulder, the Charge and the wall), the rage, the terrain in every mode
 * (and mobGriefing, and a protected place), the push the Vormir Cliff credits, the Gauntlet, the custom hero's rows
 * (the Human Form, the body's strength, the SCALE conflict), and the pure tables of his animation. Batches: {@code
 * hulk}, {@code hulk_forms}, {@code hulk_pvp}, {@code gamma}, and one per terrain mode ({@code hulk_terrain*}: a config
 * or a gamerule).
 */
public final class HulkTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A Hulk in {@code look} at full health, settled in it (one tick). */
    static ServerPlayer hulk(TestKit.Fixture f, double x, double z, String look) {
        ServerPlayer hulk = f.player(x, FLOOR, z, Power.HULK);
        Powers.setVariant(hulk, Variants.get(Power.HULK, look));
        hulk.doTick();
        hulk.setHealth(hulk.getMaxHealth());
        f.onClose(() -> HeroLifecycle.onLogout(hulk));
        return hulk;
    }

    /**
     * Ticks the players once a server tick for {@code n} ticks (their moves run on the game's time, so each needs a
     * tick of its own), then runs {@code then} on the tick after.
     */
    /**
     * Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}. One
     * entry at a time in the test's schedule (each tick schedules the next): the schedule is a map iterated while its
     * entries run, so a callback that adds many breaks the iteration ({@code ApocalypseTestSupport.every}).
     */
    static void until(GameTestHelper helper, int max, java.util.function.BooleanSupplier done, Runnable then,
            ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, java.util.function.BooleanSupplier done,
            Runnable then,
                                  ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                p.doTick();
            }
            if (done.getAsBoolean()) {
                then.run();
            } else if (tick >= max) {
                throw new net.minecraft.gametest.framework.GameTestAssertException("not done within " + max + " ticks");
            } else {
                untilStep(helper, tick + 1, max, done, then, players);
            }
        });
    }

    /**
     * Ticks the players once a server tick for {@code n} ticks (their moves run on the game's time, so each needs a
     * tick of its own), then runs {@code then} on the tick after (one entry at a time in the schedule, as {@link
     * #until}).
     */
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

    /** A husk with no armour and 200 health that stands where it is put (no AI, unless {@code free}). */
    static Husk husk(GameTestHelper helper, double x, double y, double z, boolean free) {
        Husk husk = free ? TestKit.spawn(helper, EntityType.HUSK, new Vec3(x, y, z))
                : TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, y, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static Husk husk(GameTestHelper helper, double x, double z) {
        return husk(helper, x, FLOOR, z, false);
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

    static void rage(ServerPlayer player, int rage) {
        ModAttachments.HULK.set(player, HulkRage.get(player).withRage(rage));
    }

    /** Cancels every block break in {@code cells} while the test runs (a claim, as another mod would). */
    static void protect(TestKit.Fixture f, Set<BlockPos> cells) {
        Consumer<BlockEvent.BreakEvent> listener = e -> {
            if (cells.contains(e.getPos())) {
                e.setCanceled(true);
            }
        };
        EventHub.Registration registration = EventHub.of(BlockEvent.BreakEvent.class).register(listener);
        f.onClose(registration::close);
    }

    static void mobGriefing(TestKit.Fixture f, boolean on) {
        var level = f.level();
        boolean old = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        f.onClose(() -> level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old, level.getServer()));
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(on, level.getServer());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Gamma Emitter

    /** A Gamma Emitter at (4, 2, 10) facing east: its beam runs over x 5 to 10. */
    static GammaEmitterBlockEntity emitter(GameTestHelper helper) {
        BlockPos rel = new BlockPos(4, 2, 10);
        helper.setBlock(rel, ModBlocks.GAMMA_EMITTER.get().defaultBlockState().setValue(GammaEmitterBlock.FACING,
                Direction.EAST));
        return (GammaEmitterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
    }

    /**
     * A powerless player in the beam becomes the Hulk after exactly 60 ticks in it (not 59); the first change follows.
     */
    @EnhancedTest(template = ARENA, batch = "gamma", timeoutTicks = 260)
    public static void theBeamMakesAPowerlessPlayerTheHulkAfterSixtyTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        GammaEmitterBlockEntity emitter = emitter(helper);
        ServerPlayer bruce = f.player(7.5, FLOOR, 10.5);
        f.onClose(() -> HeroLifecycle.onLogout(bruce));
        check(emitter.start(null), "it starts");
        check(!emitter.start(null), "not twice");
        int[] last = {-1};
        f.everyTick(240, t -> {
            if (Powers.power(bruce) == Power.NONE) {
                last[0] = GammaExposure.exposure(bruce);
                check(last[0] < GammaExposure.exposureTicks(), "still powerless after " + last[0]
                        + " ticks in the beam");
                return false;
            }
            check(last[0] == GammaExposure.exposureTicks() - 1, "he became the Hulk on the 60th tick, after "
                    + last[0]);
            check(Powers.power(bruce) == Power.HULK && HulkForms.changing(bruce),
                    "the Hulk, and the first change at once");
            check("ragnarok".equals(HulkAbilities.look(data(bruce))), "into the Ragnarok Hulk");
            return true;
        });
    }

    /** Another hero in the beam stays himself; a Hulk in it is healed; a redstone signal starts it; it rests after. */
    @EnhancedTest(template = ARENA, batch = "gamma", timeoutTicks = 200)
    public static void theBeamRefusesAHeroHealsAHulkAndAnswersRedstone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        GammaEmitterBlockEntity emitter = emitter(helper);
        ServerPlayer thor = f.player(6.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer hulk = f.player(8.5, FLOOR, 10.5, Power.HULK);
        f.onClose(() -> HeroLifecycle.onLogout(hulk));
        f.onClose(() -> HeroLifecycle.onLogout(thor));
        hulk.setHealth(20.0F);
        helper.setBlock(new BlockPos(4, 2, 9), Blocks.REDSTONE_BLOCK);
        check(emitter.phase() == GammaEmitterBlockEntity.WARMUP, "the redstone started it");
        helper.runAfterDelay(GammaEmitterBlockEntity.warmupTicks() + GammaEmitterBlockEntity.beamTicks() + 4, () -> {
            check(Powers.power(thor) == Power.THOR, "Thor is Thor");
            check(hulk.getHealth() >= 25.0F, "the Hulk healed: " + hulk.getHealth());
            check(emitter.phase() == GammaEmitterBlockEntity.IDLE, "the beam is over");
            check(!emitter.start(null), "it rests");
            check(GammaEmitterRecipe.craftable(), "the recipe is on by default");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Banner and the transformation

    /** Bruce Banner: every key refused (no cooldown), no body, his armour drawn (human); only N answers. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms")
    public static void bannerHasNoPowersButN(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bruce = hulk(f, 5.5, 5.5, "banner");
        check(HulkForms.isHuman(bruce), "Banner is human");
        assertNear(scale(bruce), 1.0, 1.0E-6, "his size");
        assertNear(bruce.getMaxHealth(), 20.0, 1.0E-6, "his health");
        long now = helper.getLevel().getGameTime();
        for (int key = 1; key <= 4; key++) {
            press(bruce, key, false);
            press(bruce, key, true);
            check(data(bruce).isReady(key, now), "key " + key + " is refused without a cooldown");
        }
        press(bruce, 5, true);
        check(!HulkForms.changing(bruce), "Switch Hulk is refused to Banner");
        check(!StoneRules.mayWield(bruce), "Banner does not wield the Gauntlet");
        helper.succeed();
    }

    /** N: Banner grows into the Hulk over 40 ticks (health 40 at the end), and N calms him back. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms", timeoutTicks = 300)
    public static void nGrowsAndShrinks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bruce = hulk(f, 10.5, 10.5, "banner");
        press(bruce, 5, false);
        check(HulkForms.changing(bruce) && data(bruce).is(Live.HULK_TRANSFORMING), "the change begins");
        step(helper, 20, () -> {
            double half = scale(bruce);
            check(half > 1.05 && half < 1.45, "halfway he is half grown: " + half);
            until(helper, 60, () -> !HulkForms.changing(bruce), () -> {
                assertNear(scale(bruce), 1.5, 1.0E-4, "the Ragnarok Hulk's size");
                assertNear(bruce.getMaxHealth(), 40.0, 1.0E-4, "the Hulk's health");
                check(!HulkForms.isHuman(bruce) && !HulkForms.changing(bruce), "the Hulk");
                check(StoneRules.mayWield(bruce), "the Hulk may wield the Gauntlet");
                data(bruce).shiftCooldowns(helper.getLevel().getGameTime(), 200);
                press(bruce, 5, false);
                check(HulkForms.changing(bruce), "he calms down");
                until(helper, 80, () -> !HulkForms.changing(bruce), () -> {
                    check(HulkForms.isHuman(bruce), "Banner again");
                    assertNear(scale(bruce), 1.0, 1.0E-4, "his size again");
                    assertNear(bruce.getMaxHealth(), 20.0, 1.0E-4, "his health again");
                    helper.succeed();
                }, bruce);
            }, bruce);
        }, bruce);
    }

    /** Sneak + N: Ragnarok into Brand New Day (bigger) and back; the Change Look key's step plays the change too. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms", timeoutTicks = 200)
    public static void sneakNSwitchesTheHulks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 10.5, 10.5, "ragnarok");
        press(hulk, 5, true);
        until(helper, 60, () -> !HulkForms.changing(hulk), () -> {
            check("brand_new_day".equals(HulkAbilities.look(data(hulk))), "Brand New Day");
            assertNear(scale(hulk), 1.75, 1.0E-4, "his size (" + hulk.getAttribute(Attributes.SCALE).getModifiers()
                    + ", "
                    + data(hulk).variant() + ", changing " + HulkForms.changing(hulk) + ")");
            assertNear(HulkAbilities.lookFactor(data(hulk)), 1.15, 1.0E-6, "and his damage factor");
            // The Change Look key from Brand New Day steps to Banner: the calming down plays.
            Powers.cycleVariant(hulk, 1);
            until(helper, 5, () -> HulkForms.changing(hulk), () -> {
                check("brand_new_day".equals(HulkAbilities.look(data(hulk))), "he calms down through the change");
                until(helper, 80, () -> !HulkForms.changing(hulk), () -> {
                    check(HulkForms.isHuman(hulk), "Banner at the end");
                    helper.succeed();
                }, hulk);
            }, hulk);
        }, hulk);
    }

    /** Under a two-high ceiling there is no room to grow: N is refused, and anger waits; with room, it happens. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms", timeoutTicks = 200)
    public static void noRoomNoHulk(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bruce = hulk(f, 10.5, 10.5, "banner");
        BlockPos roof = new BlockPos(10, 4, 10);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(roof.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        press(bruce, 5, false);
        check(!HulkForms.changing(bruce) && HulkForms.isHuman(bruce), "no room to Hulk out");
        ModAttachments.HULK.set(bruce, HulkRage.get(bruce).withAnger(100));
        step(helper, 10, () -> {
            check(!HulkForms.changing(bruce) && HulkForms.isHuman(bruce), "his anger waits");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.setBlock(roof.offset(dx, 0, dz), Blocks.AIR);
                }
            }
            ModAttachments.HULK.set(bruce, HulkRage.get(bruce).withAnger(100));
            step(helper, 6, () -> {
                check(HulkForms.changing(bruce) || !HulkForms.isHuman(bruce), "with room, his anger Hulks him out");
                helper.succeed();
            }, bruce);
        }, bruce);
    }

    /** Banner's anger rises when he is hurt, and at 100 he Hulks out by himself; with a Gamma Inhibitor he does not. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms", timeoutTicks = 200)
    public static void angerHulksOutUnlessInhibited(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bruce = hulk(f, 5.5, 5.5, "banner");
        bruce.hurt(helper.getLevel().damageSources().generic(), 4.0F);
        check(HulkRage.get(bruce).anger() >= 20, "two hearts make him angry: " + HulkRage.get(bruce).anger());
        check(HulkRage.bpm(0) == HulkRage.BPM_REST && HulkRage.bpm(100) == HulkRage.BPM_TOP,
                "his heart rate runs 70 to 200");
        ServerPlayer calm = hulk(f, 14.5, 14.5, "banner");
        calm.getInventory().add(new ItemStack(ModItems.GAMMA_INHIBITOR.get()));
        ModAttachments.HULK.set(bruce, HulkRage.get(bruce).withAnger(100));
        ModAttachments.HULK.set(calm, HulkRage.get(calm).withAnger(100));
        step(helper, 6, () -> {
            check(HulkForms.changing(bruce), "at 100 he Hulks out by himself");
            check(!HulkForms.changing(calm) && HulkForms.isHuman(calm), "the inhibitor keeps him human");
            helper.succeed();
        }, bruce, calm);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The keys

    /** Hulk Smash: 7 to three creatures on the ground; nothing to a bat in the air nor to one in mid-jump. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void smashHurtsEveryoneOnTheGround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 10.5, 10.5, "ragnarok");
        Husk a = husk(helper, 12.5, 10.5);
        Husk b = husk(helper, 10.5, 14.0);
        Husk c = husk(helper, 6.0, 10.5);
        Husk jumper = husk(helper, 10.5, FLOOR + 1.3, 7.5, false);
        jumper.setNoGravity(true);
        Bat bat = TestKit.spawnWithNoFreeWill(helper, EntityType.BAT, new Vec3(13.5, FLOOR + 2.0, 12.5));
        ServerPlayer friend = f.player(8.5, FLOOR, 12.5);
        press(hulk, 1, false);
        step(helper, 16, () -> {
            for (Husk h : new Husk[]{a, b, c}) {
                assertNear(lost(h), 7.0, 0.01, "a husk on the ground takes 7");
            }
            check(lost(jumper) == 0.0F, "one in mid-jump is untouched");
            check(bat.isAlive() && lost(bat) == 0.0F, "a bat in the air is untouched");
            check(lost(friend) == 0.0F, "PvP off spares a player");
            check(hulk.getUUID().equals(HulkAbilities.lastPushedBy(a)),
                    "his push is his (the Vormir Cliff credits him)");
            helper.succeed();
        }, hulk);
    }

    /** The Brand New Day Hulk hits harder: Smash 7 x 1.15 = 8.05; and rage adds up to 30 %. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void brandNewDayAndRageHitHarder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer bnd = hulk(f, 5.5, 5.5, "brand_new_day");
        Husk a = husk(helper, 7.5, 5.5);
        ServerPlayer angry = hulk(f, 15.5, 15.5, "ragnarok");
        Husk b = husk(helper, 17.5, 15.5);
        rage(angry, 100);
        press(bnd, 1, false);
        press(angry, 1, false);
        step(helper, 12, () -> {
            assertNear(lost(a), 8.05, 0.01, "Brand New Day: 8.05");
            assertNear(lost(b), 9.1, 0.03, "full rage: 7 x 1.3");
            helper.succeed();
        }, bnd, angry);
    }

    /** Rage falls out of a fight; at 50 he is enraged and Nausea slides off him. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void rageFallsAndEnragedIgnoresNausea(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        rage(hulk, 60);
        step(helper, 1, () -> {
            check(data(hulk).is(Live.HULK_ENRAGED), "enraged at 60");
            hulk.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
            hulk.addEffect(new MobEffectInstance(ModEffects.CONFUSED, 100));
            check(!hulk.hasEffect(MobEffects.CONFUSION) && !hulk.hasEffect(ModEffects.CONFUSED), "Hulk too angry");
        }, hulk);
        step(helper, 40, () -> {
            check(HulkRage.get(hulk).rage() <= 57, "it falls out of a fight: " + HulkRage.get(hulk).rage());
            int before = HulkRage.get(hulk).rage();
            hulk.invulnerableTime = 0;
            hulk.hurt(helper.getLevel().damageSources().generic(), 4.0F);
            check(HulkRage.get(hulk).rage() > before, "and rises when he is hurt");
            helper.succeed();
        }, hulk);
    }

    /** Grab Smash: a husk slammed left, right, left (4 + 4 + 6); a boss takes one heavy punch (8) instead. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void grabSmashSlamsThriceAndPunchesABoss(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        Husk victim = husk(helper, 5.5, 8.0);
        lookAt(hulk, victim);
        press(hulk, 1, true);
        step(helper, 1, () -> {
            check(data(hulk).is(Live.HULK_GRABBING), "he holds it");
            step(helper, 32, () -> {
                assertNear(lost(victim), 14.0, 0.02, "4 + 4 + 6");
                ServerPlayer other = hulk(f, 14.5, 14.5, "ragnarok");
                ElderGuardian boss = TestKit.spawnWithNoFreeWill(helper, EntityType.ELDER_GUARDIAN,
                        new Vec3(14.5, FLOOR, 17.5));
                lookAt(other, boss);
                LivingEntity punched = GrabSmash.start(other, data(other));
                check(punched == boss && !data(other).is(Live.HULK_GRABBING), "a boss is punched, not grabbed");
                assertNear(lost(boss), 8.0, 0.02, "one heavy punch of 8");
                helper.succeed();
            }, hulk);
        }, hulk);
    }

    /** The Thunder Clap: 6 and a throw of 8 blocks or more; fire is put out; an arrow in the wave is turned back. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void thunderClapThrowsFarDousesAndTurnsArrows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 2.5, 10.5, "ragnarok");
        look(hulk, EAST, 0.0F);
        Husk thrown = husk(helper, 5.5, FLOOR, 10.5, true);
        Husk burning = husk(helper, 6.5, 11.5);
        burning.setRemainingFireTicks(200);
        helper.setBlock(new BlockPos(7, 2, 9), Blocks.FIRE);
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(7.5, FLOOR + 1.0, 10.5));
        arrow.moveTo(at.x, at.y, at.z);
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(-0.05, 0, 0);
        helper.getLevel().addFreshEntity(arrow);
        double startX = thrown.getX();
        check(ThunderClap.start(hulk, data(hulk)), "the clap");
        step(helper, 16, () -> {
            assertNear(lost(thrown), 6.0, 0.01, "6 damage");
            check(!burning.isOnFire(), "the burning one is put out");
            check(!helper.getBlockState(new BlockPos(7, 2, 9)).is(Blocks.FIRE), "the fire is out");
            check(arrow.getDeltaMovement().x > 0.0 && arrow.getOwner() == hulk, "the arrow flies back, his");
            helper.runAfterDelay(10, () -> {
                check(thrown.getX() - startX >= 8.0, "thrown far: " + (thrown.getX() - startX));
                helper.succeed();
            });
        }, hulk);
    }

    /** The Roar frightens a monster (it runs from him) and weakens it; a boss is not frightened; rage +20. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 120)
    public static void roarFrightensButNotABoss(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 6.5, 10.5, "ragnarok");
        Husk husk = husk(helper, 9.5, FLOOR, 10.5, true);
        ElderGuardian boss = TestKit.spawnWithNoFreeWill(helper, EntityType.ELDER_GUARDIAN, new Vec3(6.5, FLOOR, 14.5));
        double before = husk.distanceTo(hulk);
        HulkAbilities.roar(hulk, data(hulk));
        check(husk.hasEffect(ModEffects.FRIGHTENED) && husk.hasEffect(MobEffects.WEAKNESS),
                "the husk is frightened and weak");
        check(!boss.hasEffect(ModEffects.FRIGHTENED), "a boss is not");
        check(HulkRage.get(hulk).rage() >= 20, "his rage rose");
        helper.runAfterDelay(50, () -> {
            check(husk.distanceTo(hulk) > before + 1.0, "it ran: " + before + " -> " + husk.distanceTo(hulk));
            helper.succeed();
        });
    }

    /** The Super Leap lands within 2 of where it was aimed, hurts round the landing (6 in radius 4), no fall damage. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 200)
    public static void superLeapLandsOnTarget(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 3.5, 3.5, "ragnarok");
        Vec3 target = helper.absoluteVec(new Vec3(15.5, FLOOR, 15.5));
        Husk near = husk(helper, 17.5, 15.5);
        Husk far = husk(helper, 4.5, 17.5);
        float health = hulk.getHealth();
        SuperLeap.leapTo(hulk, target);
        boolean[] airborne = {false};
        f.everyTick(160, t -> {
            hulk.doTick();
            airborne[0] |= hulk.getY() > target.y + 5.0;
            if (t < 3 || SuperLeap.of(hulk) != null) {
                return false;
            }
            check(airborne[0], "he flew high");
            double miss = Math.hypot(hulk.getX() - target.x, hulk.getZ() - target.z);
            check(miss <= 2.0, "within 2 of the target: " + miss);
            assertNear(lost(near), 6.0, 0.01, "6 where he lands");
            check(lost(far) == 0.0F, "nothing far from it");
            check(hulk.getHealth() >= health, "no fall damage");
            return true;
        });
    }

    /** The Bound: a long low leap forward. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 120)
    public static void boundGoesFarAndLow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 2.5, 2.5, "ragnarok");
        look(hulk, -45.0F, 0.0F);
        Vec3 start = hulk.position();
        check(SuperLeap.bound(hulk, data(hulk)), "the bound");
        double[] top = {start.y};
        f.everyTick(100, t -> {
            hulk.doTick();
            top[0] = Math.max(top[0], hulk.getY());
            if (t < 3 || SuperLeap.of(hulk) != null) {
                return false;
            }
            double travelled = Math.hypot(hulk.getX() - start.x, hulk.getZ() - start.z);
            check(travelled >= 14.0, "far: " + travelled);
            check(top[0] - start.y <= 8.0, "and low: " + (top[0] - start.y));
            return true;
        });
    }

    /** The Boulder Throw: 8 to the creature hit, 4 splash beside it. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 120)
    public static void boulderHitsAndSplashes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 3.5, 10.5, "ragnarok");
        Husk hit = husk(helper, 9.5, 10.5);
        Husk beside = husk(helper, 9.5, 11.9);
        lookAt(hulk, hit);
        check(HulkAbilities.rip(hulk, data(hulk)), "the boulder is torn up");
        step(helper, HulkAbilities.RIP_TICKS + 1, () -> helper.runAfterDelay(20, () -> {
            assertNear(lost(hit), 8.0, 0.01, "8 to the one it hits");
            assertNear(lost(beside), 4.0, 0.01, "4 to the one beside");
            helper.succeed();
        }), hulk);
    }

    /** The Gamma Charge: 6 to a creature in his path; a stone wall stops him. */
    @EnhancedTest(template = ARENA, batch = "hulk", timeoutTicks = 100)
    public static void gammaChargeHitsAndAWallStopsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 3.5, 10.5, "ragnarok");
        look(hulk, EAST, 0.0F);
        Husk husk = husk(helper, 6.5, 10.5);
        for (int z = 8; z <= 13; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(11, y, z), Blocks.STONE);
            }
        }
        check(GammaCharge.start(hulk, data(hulk)), "the charge");
        step(helper, 16, () -> {
            assertNear(lost(husk), 6.0, 0.01, "6 to the one in his path");
            double front = helper.absolutePos(new BlockPos(11, 2, 10)).getX();
            check(hulk.getX() + hulk.getBbWidth() * 0.5 <= front + 0.01, "the wall stopped him: "
                    + (hulk.getX() - front));
            check(helper.getBlockState(new BlockPos(11, 2, 10)).is(Blocks.STONE), "and stands");
            helper.succeed();
        }, hulk);
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP

    /** With PvP on, the Smash hurts a player on the ground. */
    @EnhancedTest(template = ARENA, batch = "hulk_pvp", timeoutTicks = 60)
    public static void smashHurtsAPlayerWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer hulk = hulk(f, 10.5, 10.5, "ragnarok");
        ServerPlayer foe = f.player(12.5, FLOOR, 10.5);
        press(hulk, 1, false);
        step(helper, 12, () -> {
            check(lost(foe) > 0.0F, "the player was hurt");
            helper.succeed();
        }, hulk);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The terrain

    /**
     * {@code soft} (the default): the Smash breaks glass near him, never stone; the charge goes through a glass wall.
     */
    @EnhancedTest(template = ARENA, batch = "hulk_terrain", timeoutTicks = 100)
    public static void softBreaksGlassNotStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.GLASS);
        helper.setBlock(new BlockPos(5, 2, 6), Blocks.STONE);
        press(hulk, 1, false);
        ServerPlayer charger = hulk(f, 3.5, 14.5, "ragnarok");
        look(charger, EAST, 0.0F);
        for (int z = 13; z <= 16; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.GLASS);
            }
        }
        GammaCharge.start(charger, data(charger));
        step(helper, 14, () -> {
            check(helper.getBlockState(new BlockPos(6, 2, 5)).isAir(), "the glass shattered");
            check(helper.getBlockState(new BlockPos(5, 2, 6)).is(Blocks.STONE), "the stone stands");
            check(charger.getX() > helper.absolutePos(new BlockPos(9, 2, 14)).getX(), "through the glass");
            helper.succeed();
        }, hulk, charger);
    }

    /** {@code none}: nothing breaks. */
    @EnhancedTest(template = ARENA, batch = "hulk_terrain_none", timeoutTicks = 60)
    public static void noneBreaksNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HulkConfig.BLOCK_BREAKING, HulkConfig.BlockBreaking.NONE);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.GLASS);
        press(hulk, 1, false);
        step(helper, 10, () -> {
            check(helper.getBlockState(new BlockPos(6, 2, 5)).is(Blocks.GLASS), "the glass stands");
            helper.succeed();
        }, hulk);
    }

    /** {@code all}: the Smash makes a small crater in stone. */
    @EnhancedTest(template = ARENA, batch = "hulk_terrain_all", timeoutTicks = 60)
    public static void allMakesACrater(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HulkConfig.BLOCK_BREAKING, HulkConfig.BlockBreaking.ALL);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(9, 2, 5), Blocks.STONE);
        press(hulk, 1, false);
        step(helper, 10, () -> {
            check(helper.getBlockState(new BlockPos(6, 2, 5)).isAir(), "the stone beside him is smashed");
            check(helper.getBlockState(new BlockPos(9, 2, 5)).is(Blocks.STONE), "the crater is small");
            check(!HulkTerrain.breakable(Blocks.OAK_DOOR.defaultBlockState(), HulkConfig.BlockBreaking.ALL,
                    HulkTerrain.Kind.SMASH)
                    && !HulkTerrain.breakable(Blocks.OBSIDIAN.defaultBlockState(), HulkConfig.BlockBreaking.ALL,
                            HulkTerrain.Kind.SMASH)
                    && !HulkTerrain.breakable(Blocks.WHEAT.defaultBlockState(), HulkConfig.BlockBreaking.ALL,
                            HulkTerrain.Kind.SMASH),
                    "never a door, obsidian or crops");
            helper.succeed();
        }, hulk);
    }

    /** With mobGriefing off nothing breaks; with it on, a protected cell (a claim) still does not. */
    @EnhancedTest(template = ARENA, batch = "hulk_terrain_grief", timeoutTicks = 100)
    public static void mobGriefingAndProtectionStopHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        mobGriefing(f, false);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.GLASS);
        press(hulk, 1, false);
        step(helper, 10, () -> {
            check(helper.getBlockState(new BlockPos(6, 2, 5)).is(Blocks.GLASS), "no mobGriefing: the glass stands");
            mobGriefing(f, true);
            Set<BlockPos> claim = new HashSet<>();
            claim.add(helper.absolutePos(new BlockPos(6, 2, 5)));
            protect(f, claim);
            data(hulk).shiftCooldowns(helper.getLevel().getGameTime(), 200);
            press(hulk, 1, false);
            step(helper, 10, () -> {
                check(helper.getBlockState(new BlockPos(6, 2, 5)).is(Blocks.GLASS), "a claim: the glass stands");
                helper.succeed();
            }, hulk);
        }, hulk);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Gauntlet's cost, the custom hero

    /** His Snap burns his arm (Weakness II, the arm's timer). */
    @EnhancedTest(template = ARENA, batch = "hulk")
    public static void theSnapBurnsHisArm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hulk = hulk(f, 5.5, 5.5, "ragnarok");
        HulkHooks.onSnap(hulk);
        check(HulkHooks.armBurned(hulk) && hulk.hasEffect(MobEffects.WEAKNESS), "the arm is burned");
        helper.succeed();
    }

    /** A custom hero's Human Form: N turns every module off (its armour goes) and back. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms", timeoutTicks = 100)
    public static void customHumanFormTurnsHisPowersOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("hulk.transform", 3).key(5, "hulk.transform")
                .pick("durability", 5, "armor").pick("hulk.body", 3).build();
        ServerPlayer hero = CustomKitSupport.hero(f, 5.5, 5.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        CustomKitSupport.tickN(hero, 2);
        double armour = hero.getAttributeValue(Attributes.ARMOR);
        check(armour > 0.0 && scale(hero) > 1.0, "armour and size: " + armour + ", " + scale(hero));
        CustomKitSupport.tap(hero, 5, false);
        CustomKitSupport.tickN(hero, 2);
        check(data(hero).toggle(Toggle.HUMAN_FORM), "human");
        check(hero.getAttributeValue(Attributes.ARMOR) == 0.0 && scale(hero) == 1.0, "no armour, no size");
        check(!data(hero).has(HeroAbility.HULK_BODY) && data(hero).has(HeroAbility.HULK_TRANSFORM),
                "only the Transform");
        data(hero).shiftCooldowns(helper.getLevel().getGameTime(), 400);
        CustomKitSupport.tap(hero, 5, false);
        CustomKitSupport.tickN(hero, 2);
        check(!data(hero).toggle(Toggle.HUMAN_FORM) && hero.getAttributeValue(Attributes.ARMOR) == armour, "back");
        helper.succeed();
    }

    /** {@code hulk.body} scales by its strength; it and a Shapeshift (grow) both want the scale: the rules keep one. */
    @EnhancedTest(template = ARENA, batch = "hulk_forms")
    public static void customBodyScalesAndTheScaleIsOnes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer weak = CustomKitSupport.hero(f, 4.5, 4.5, CustomKitSupport.def().pick("hulk.body", 1).build());
        ServerPlayer strong = CustomKitSupport.hero(f, 12.5, 12.5, CustomKitSupport.def().pick("hulk.body", 5).build());
        f.onClose(() -> HeroLifecycle.onLogout(weak));
        f.onClose(() -> HeroLifecycle.onLogout(strong));
        CustomKitSupport.tickN(weak, 1);
        CustomKitSupport.tickN(strong, 1);
        check(scale(strong) > scale(weak) && scale(weak) > 1.0, "strength scales the body: " + scale(weak) + " < "
                + scale(strong));
        check(strong.getMaxHealth() > weak.getMaxHealth(), "and its health");
        CustomHero both = CustomKitSupport.def().pick("hulk.body", 3).pick("shapeshift", 3, "grow").raw();
        CustomHero judged = CustomHeroRules.sanitize(both, CustomHeroRules.Limits.DEFAULT).hero();
        check(judged.powers().size() == 1, "one owner of the scale: " + judged.powers());
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pure tables

    /** His rows' moves, his routes, his gestures' keys, his run, his preset. */
    @EnhancedTest(template = ARENA, batch = "hulk")
    public static void theAnimationTablesAndThePreset(GameTestHelper helper) {
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("HULK_")) {
                check(Gestures.get(id) != null && Gestures.get(id).problems().isEmpty(), id + " is well keyed");
                check(GestureRoutes.startedElsewhere(id), id + " is started by his payloads");
            }
        }
        check(HulkGestures.gestureOf(HulkFxPayload.SMASH, 0) == GestureId.HULK_SMASH, "the Smash's press");
        check(HulkGestures.gestureOf(HulkFxPayload.SMASH, 6) == null, "the impact is no new gesture");
        check(HulkGestures.gestureOf(HulkFxPayload.TRANSFORM_OUT, 40) == GestureId.HULK_OUT, "Hulk out");
        check(GestureRoutes.toggle(Power.HULK, Toggle.HUMAN_FORM, true) == GestureId.HULK_CALM,
                "the Human Form's calm");
        check(HeroAbilityAnims.shown(HeroAbility.HULK_SMASH, true) == GestureId.HULK_GRAB_SMASH, "the row's variant");
        check(RunCharacter.of(Power.HULK, false) == RunCharacter.HULK, "his run");
        check(HeroPresets.HEROES.contains("hulk"), "his preset is listed");
        check(GrabSmash.class.getSimpleName().length() > 0 && HulkState.FIRST_HULK.equals("ragnarok"),
                "his first look");
        check(!StoneRules.mayWield(f(helper)), "a powerless player never wields");
        helper.succeed();
    }

    private static ServerPlayer f(GameTestHelper helper) {
        return TestKit.fixture(helper).player(1.5, FLOOR, 1.5);
    }

    private HulkTests() {
    }
}
