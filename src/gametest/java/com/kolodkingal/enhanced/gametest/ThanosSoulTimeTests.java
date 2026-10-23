package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.ThanosTests.cooldownLeft;
import static com.kolodkingal.enhanced.gametest.ThanosTests.lost;
import static com.kolodkingal.enhanced.gametest.ThanosTests.press;
import static com.kolodkingal.enhanced.gametest.ThanosTests.pressSneaking;
import static com.kolodkingal.enhanced.gametest.ThanosTests.ready;
import static com.kolodkingal.enhanced.gametest.ThanosTests.shield;
import static com.kolodkingal.enhanced.gametest.ThanosTests.thanos;
import static com.kolodkingal.enhanced.gametest.ThanosTests.told;
import static com.kolodkingal.enhanced.gametest.ThanosTests.zombie;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Thanos, the hero, continued: the Soul Stone's Drain and Sight, the Time Stone's Rewind and Lock, what ends when he
 * does (his life, his session, his dimension, his power), the Gauntlet's ceremony, and the hero beside the boss.
 */
public final class ThanosSoulTimeTests {

    // ============================================================================================================
    // B, the Soul Stone

    /** Locks a drain on the zombie and says so: the state is set and nothing has been taken yet. */
    private static void drain(ServerPlayer thanos) {
        AbilityHandler.onKey(thanos, 4, true);
    }

    /**
     * Hold B: every ten ticks three that armour and enchantments cannot lessen (a zombie in full netherite with
     * Protection IV loses exactly three), half of it healing him; letting go ends it and starts the cooldown.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_drain", timeoutTicks = 100)
    public static void soulDrainTakesLifeThroughArmourAndHealsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(ThanosConfig.REGEN_PER_SECOND, 0.0);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        thanos.setHealth(20.0F);
        Zombie target = zombie(helper, 8.5, 10.5);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ItemStack piece : List.of(new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS))) {
            piece.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 4);
            target.setItemSlot(((ArmorItem) piece.getItem()).getEquipmentSlot(), piece);
        }
        PowerData data = Powers.get(thanos);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "the chain is lashed on");
        f.everyTick(40, t -> {
            PowerTicker.tick(thanos);
            if (t == 5) {
                check(lost(target) == 0.0F, "nothing is taken before the first beat");
            }
            if (t == 14) {
                assertNear(lost(target), ThanosConfig.DRAIN_DAMAGE_PER_SECOND.get() * 0.5, 0.01, "three, through the armour");
                assertNear(thanos.getHealth(), 20.0 + 3.0 * ThanosConfig.DRAIN_HEAL_FRACTION.get(), 0.01, "and half of it heals him");
                check(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the target is slowed");
            }
            if (t == 24) {
                assertNear(lost(target), 6.0, 0.01, "three more on the next beat");
                AbilityHandler.onKey(thanos, 4, false);
                check(!data.is(Live.SOUL_DRAINING), "letting go ends it");
                check(cooldownLeft(thanos, 4) == ThanosConfig.DRAIN_COOLDOWN.get(), "and starts the cooldown: " + cooldownLeft(thanos, 4));
            }
            if (t == 34) {
                assertNear(lost(target), 6.0, 0.01, "and nothing more is taken");
                return true;
            }
            return false;
        });
    }

    /** A block that comes between ends the chain at once. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_drain_ends")
    public static void aBlockBetweenEndsTheSoulDrain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 8.5, 10.5);
        PowerData data = Powers.get(thanos);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "locked");
        for (int y = 0; y < 4; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.setBlock(new BlockPos(6, (int) FLOOR + y, z), Blocks.STONE);
            }
        }
        PowerTicker.tick(thanos);
        check(!data.is(Live.SOUL_DRAINING), "the chain cannot pass a block");
        check(cooldownLeft(thanos, 4) == ThanosConfig.DRAIN_COOLDOWN.get(), "and the cooldown starts");
        check(lost(target) == 0.0F, "nothing was taken");
        helper.succeed();
    }

    /** Past the chain's slack it parts: a creature that leaves, or a Thanos who is starving or held, ends it. */
    @EnhancedTest(template = ARENA, batch = "solo_the_soul_drain_ends_out_of_range_and_when_he_starves_or_is_held")
    public static void theSoulDrainEndsOutOfRangeAndWhenHeStarvesOrIsHeld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(ThanosConfig.DRAIN_RANGE, 4.0);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        Zombie target = zombie(helper, 6.5, 10.5);
        PowerData data = Powers.get(thanos);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "locked at three blocks");
        Vec3 far = helper.absoluteVec(new Vec3(14.5, FLOOR, 10.5));
        target.teleportTo(far.x, far.y, far.z);
        PowerTicker.tick(thanos);
        check(!data.is(Live.SOUL_DRAINING), "eleven blocks is past the slack: the chain parted");
        // Starving: it does not start at an empty hunger bar.
        target.teleportTo(helper.absoluteVec(new Vec3(6.5, FLOOR, 10.5)).x, far.y, far.z);
        ready(thanos, 4);
        thanos.getFoodData().setFoodLevel(0);
        TestKit.sentPackets(thanos);
        drain(thanos);
        check(!data.is(Live.SOUL_DRAINING) && told(TestKit.sentPackets(thanos), "message.enhanced.thanos.too_hungry"), "it does not start at an empty hunger bar");
        thanos.getFoodData().setFoodLevel(20);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "fed, it starts");
        thanos.getFoodData().setFoodLevel(0);
        PowerTicker.tick(thanos);
        check(!data.is(Live.SOUL_DRAINING), "and an empty bar ends it");
        // Held: the spell ends it.
        thanos.getFoodData().setFoodLevel(20);
        ready(thanos, 4);
        drain(thanos);
        thanos.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100, 0, false, false));
        PowerTicker.tick(thanos);
        check(!data.is(Live.SOUL_DRAINING), "held, he cannot hold the chain");
        helper.succeed();
    }

    /** Sixty damage in a second tears the chain: the next tick finds it parted, with a flinch. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_drain_ends")
    public static void aHardEnoughBlowTearsTheSoulDrain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        zombie(helper, 8.5, 10.5);
        PowerData data = Powers.get(thanos);
        drain(thanos);
        ThanosAbilities.hurt(thanos, 59.0F);
        PowerTicker.tick(thanos);
        check(data.is(Live.SOUL_DRAINING), "fifty-nine does not tear it");
        ThanosAbilities.hurt(thanos, 1.0F);
        PowerTicker.tick(thanos);
        check(!data.is(Live.SOUL_DRAINING), "sixty does");
        check(thanos.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "with a flinch");
        helper.succeed();
    }

    /**
     * A Chaos Shield around the target stops the chain at its surface and loses twelve; a letting go of the hold ends
     * it.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_drain_shield", timeoutTicks = 60)
    public static void aChaosShieldStopsTheSoulDrainAndLosesTwelve(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        ServerPlayer wanda = f.player(8.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        shield(f, wanda, 100.0F);
        PowerData data = Powers.get(thanos);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "locked on");
        f.everyTick(20, t -> {
            PowerTicker.tick(thanos);
            if (t < 14) {
                return false;
            }
            check(!data.is(Live.SOUL_DRAINING), "the chain stopped at the shield");
            assertNear(100.0F - Powers.get(wanda).shieldStrength(), 12.0, 0.01, "and cost it twelve");
            check(wanda.getHealth() == wanda.getMaxHealth(), "she was not touched");
            return true;
        });
    }

    /** B released with no drain running (a Soul Sight press, a spent key) does nothing at all. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_drain_ends")
    public static void letGoOfBWithNoDrainDoesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        AbilityHandler.onKey(thanos, 4, false);
        check(cooldownLeft(thanos, 4) <= 0, "no cooldown for a key that held nothing");
        // Not even in the way of a Spectator or a held Thanos: a release always goes through.
        thanos.setGameMode(GameType.SPECTATOR);
        AbilityHandler.onKey(thanos, 4, false);
        check(cooldownLeft(thanos, 4) <= 0, "nor for a Spectator");
        helper.succeed();
    }

    /**
     * What Loki hides, the Soul Sight shows: duplicates dissolve, he is seen, his shape is his own; the config can keep
     * the tricks.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_sight", timeoutTicks = 260)
    public static void soulSightRevealsAndBreaksLokisTricks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 3.5, 3.5);
        ServerPlayer loki = f.player(14.5, FLOOR, 14.5, Power.LOKI);
        Zombie model = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 18.5));
        lookAt(loki, model);
        press(loki, 3);
        model.discard();
        press(loki, 2);
        PowerData lokiData = Powers.get(loki);
        check(!lokiData.form().isEmpty() && !LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty() && loki.hasEffect(MobEffects.INVISIBILITY),
                "Loki wears a borrowed shape, has duplicates and hides");
        Zombie ghost = zombie(helper, 6.5, 6.5);
        ghost.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false));
        PowerData data = Powers.get(thanos);
        pressSneaking(thanos, 4);
        check(data.is(Live.SOUL_SIGHT), "the Sight is on");
        check(cooldownLeft(thanos, 4) == ThanosConfig.SIGHT_COOLDOWN.get(), "the Sight's cooldown: " + cooldownLeft(thanos, 4));
        check(LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "the duplicates are gone");
        check(!loki.hasEffect(MobEffects.INVISIBILITY) && !lokiData.is(Live.HIDDEN), "Loki is seen again");
        check(lokiData.form().isEmpty(), "and his shape is his own");
        check(ghost.hasEffect(MobEffects.GLOWING), "whoever else is invisible glows for everyone");
        f.everyTick(ThanosConfig.SIGHT_TICKS.get() + 10, t -> {
            PowerTicker.tick(thanos);
            if (t == ThanosConfig.SIGHT_TICKS.get() - 20) {
                check(data.is(Live.SOUL_SIGHT), "the Sight lasts its eight seconds");
            }
            if (t == ThanosConfig.SIGHT_TICKS.get() + 4) {
                check(!data.is(Live.SOUL_SIGHT), "and ends");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "solo_with_the_config_off_the_soul_sight_keeps_lokis_tricks", timeoutTicks = 60)
    public static void withTheConfigOffTheSoulSightKeepsLokisTricks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(ThanosConfig.SIGHT_BREAKS_ILLUSIONS, false);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 3.5, 3.5);
        ServerPlayer loki = f.player(14.5, FLOOR, 14.5, Power.LOKI);
        Zombie model = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 18.5));
        lookAt(loki, model);
        press(loki, 3);
        model.discard();
        press(loki, 2);
        pressSneaking(thanos, 4);
        check(!LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "the duplicates stay");
        check(!Powers.get(loki).form().isEmpty(), "and so does the shape");
        helper.succeed();
    }

    /** In Creative the Sight has no cooldown and costs no hunger. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_sight_keeps")
    public static void inCreativeTheSoulSightHasNoCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 3.5);
        thanos.setGameMode(GameType.CREATIVE);
        pressSneaking(thanos, 4);
        check(Powers.get(thanos).is(Live.SOUL_SIGHT) && cooldownLeft(thanos, 4) <= 0, "the Sight is on and rests for nothing");
        helper.succeed();
    }

    // ============================================================================================================
    // N, the Time Stone

    /**
     * Rewind: he returns to where he was, with the health he had then if it was higher (at most twenty more), sheds the
     * harm that began since and keeps what was already there; it costs hunger and the long cooldown, and the history is
     * spent.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind", timeoutTicks = 60)
    public static void rewindRestoresPositionAndHealthAndUndoesNewHarm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 6.5, 6.5);
        thanos.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2000, 0, false, false));
        thanos.getFoodData().setFoodLevel(20);
        ThanosAbilities.remember(thanos, 100);
        Vec3 there = thanos.position();
        Vec3 elsewhere = helper.absoluteVec(new Vec3(15.5, FLOOR, 15.5));
        thanos.teleportTo(level, elsewhere.x, elsewhere.y, elsewhere.z, 0.0F, 0.0F);
        thanos.setHealth(15.0F);
        thanos.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0, false, false));
        thanos.setRemainingFireTicks(100);
        float exhaustion = thanos.getFoodData().getExhaustionLevel();
        thanos.getFoodData().setFoodLevel(11);
        press(thanos, 5);
        check(thanos.getFoodData().getFoodLevel() == 11, "his food is not rewound");
        assertNear(thanos.getX(), there.x, 0.05, "he is back where he was (x)");
        assertNear(thanos.getZ(), there.z, 0.05, "(z)");
        assertNear(thanos.getHealth(), Math.min(thanos.getMaxHealth(), 15.0 + ThanosConfig.REWIND_MAX_HEAL.get()), 0.01, "his health returned, by at most twenty");
        check(!thanos.hasEffect(MobEffects.POISON), "the poison that began since is gone");
        check(thanos.hasEffect(MobEffects.WEAKNESS), "the weakness he already had is not undone");
        check(!thanos.isOnFire(), "the fire is out");
        check(cooldownLeft(thanos, 5) == ThanosConfig.REWIND_COOLDOWN.get(), "the cooldown: " + cooldownLeft(thanos, 5));
        assertNear(thanos.getFoodData().getExhaustionLevel() - exhaustion, ThanosConfig.REWIND_EXHAUSTION.get(), 0.01, "it costs two food points of hunger");
        check(ThanosAbilities.historyTicks(thanos) == 0, "the history is spent");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind", timeoutTicks = 60)
    public static void rewindWithTooLittleHistoryDoesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 6.5, 6.5);
        ThanosAbilities.remember(thanos, 30);
        Vec3 elsewhere = helper.absoluteVec(new Vec3(15.5, FLOOR, 15.5));
        thanos.teleportTo(f.level(), elsewhere.x, elsewhere.y, elsewhere.z, 0.0F, 0.0F);
        thanos.setHealth(15.0F);
        TestKit.sentPackets(thanos);
        press(thanos, 5);
        check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.nothing_to_rewind"), "'Nothing to rewind'");
        check(thanos.position().distanceTo(elsewhere) < 0.05 && thanos.getHealth() == 15.0F && cooldownLeft(thanos, 5) <= 0, "and it changed and cost nothing");
        helper.succeed();
    }

    /**
     * The history builds as he lives (a snapshot every few ticks, for five seconds) and the Rewind goes back to its
     * oldest.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind_ticks", timeoutTicks = 200)
    public static void theHistoryBuildsAsHeLivesAndItsWindowIsFiveSeconds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 6.5, 6.5);
        Vec3 b = helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5));
        Vec3 c = helper.absoluteVec(new Vec3(15.5, FLOOR, 5.5));
        f.everyTick(130, t -> {
            PowerTicker.tick(thanos);
            if (t == 10) {
                check(ThanosAbilities.historyTicks(thanos) >= 8 && ThanosAbilities.historyTicks(thanos) <= 11, "ten ticks of history: " + ThanosAbilities.historyTicks(thanos));
                thanos.teleportTo(f.level(), b.x, b.y, b.z, 0.0F, 0.0F);
            }
            if (t == 60) {
                thanos.teleportTo(f.level(), c.x, c.y, c.z, 0.0F, 0.0F);
            }
            if (t == 125) {
                int history = ThanosAbilities.historyTicks(thanos);
                check(history >= 100 && history <= 106, "five seconds of history, and no more: " + history);
                press(thanos, 5);
                check(thanos.position().distanceTo(b) < 0.1, "he went back to where he stood five seconds ago: " + thanos.position() + " not " + b);
                return true;
            }
            return false;
        });
    }

    /** The cap on what a rewind heals, and the health he had then as the ceiling. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind", timeoutTicks = 60)
    public static void rewindHealsAtMostTheConfigAndNeverPastTheOldHealth(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 6.5, 6.5);
        thanos.setHealth(30.0F);
        ThanosAbilities.remember(thanos, 100);
        thanos.setHealth(5.0F);
        press(thanos, 5);
        assertNear(thanos.getHealth(), 5.0 + ThanosConfig.REWIND_MAX_HEAL.get(), 0.01, "from five, twenty more");
        ready(thanos, 5);
        thanos.setHealth(25.0F);
        ThanosAbilities.remember(thanos, 100);
        thanos.setHealth(12.0F);
        press(thanos, 5);
        assertNear(thanos.getHealth(), 25.0, 0.01, "but not past the health he had");
        ready(thanos, 5);
        thanos.setHealth(20.0F);
        ThanosAbilities.remember(thanos, 100);
        thanos.setHealth(30.0F);
        press(thanos, 5);
        assertNear(thanos.getHealth(), 30.0, 0.01, "and health he has more of now is not taken away");
        helper.succeed();
    }

    /** A Hex wall refuses the place (the health still returns); Mjolnir's pin refuses all of it. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind_hex", timeoutTicks = 60)
    public static void theHexRefusesThePlaceButNotTheHealthAndThePinRefusesAll(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_MIN_RADIUS, 4);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 4);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 16.5, 12.5);
        ThanosAbilities.remember(thanos, 100);
        ServerPlayer wanda = f.player(16.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        HexManager.create(wanda);
        Vec3 outside = helper.absoluteVec(new Vec3(3.5, FLOOR, 10.5));
        thanos.teleportTo(level, outside.x, outside.y, outside.z, 0.0F, 0.0F);
        thanos.setHealth(10.0F);
        press(thanos, 5);
        check(thanos.position().distanceTo(outside) < 0.05, "the wall refused the old place: he is where he was");
        assertNear(thanos.getHealth(), 30.0, 0.01, "but the health returned");
        check(cooldownLeft(thanos, 5) == ThanosConfig.REWIND_COOLDOWN.get(), "and it was paid for");
        // The pin refuses all of it.
        ready(thanos, 5);
        thanos.setHealth(10.0F);
        ThanosAbilities.remember(thanos, 100);
        Vec3 elsewhere = helper.absoluteVec(new Vec3(6.5, FLOOR, 6.5));
        thanos.teleportTo(level, elsewhere.x, elsewhere.y, elsewhere.z, 0.0F, 0.0F);
        ThanosTests.pin(f, thanos);
        press(thanos, 5);
        check(thanos.getHealth() == 10.0F && cooldownLeft(thanos, 5) <= 0, "Mjolnir's pin refuses the whole Rewind");
        helper.succeed();
    }

    /** The history goes with his life, his session, his dimension and his power: nothing is carried into the next. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_rewind_ends", timeoutTicks = 60)
    public static void theHistoryIsDroppedOnDeathLogoutDimensionAndPowerLoss(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 6.5, 6.5);
        for (String how : List.of("death", "dimension", "logout", "power")) {
            ThanosAbilities.remember(thanos, 100);
            check(ThanosAbilities.historyTicks(thanos) >= 100, how + ": there is a history");
            switch (how) {
                case "death" -> HeroLifecycle.onDeath(thanos);
                case "dimension" -> HeroLifecycle.onChangeDimension(thanos);
                case "logout" -> HeroLifecycle.onLogout(thanos);
                default -> {
                    Powers.setPower(thanos, Power.NONE);
                    Powers.setPower(thanos, Power.THANOS);
                }
            }
            check(ThanosAbilities.historyTicks(thanos) == 0, how + ": the history is gone");
        }
        helper.succeed();
    }

    /**
     * Time Lock: in a dome of six blocks, for five seconds, creatures are locked (a zombie, not his wolf, not a player
     * with PvP off, nor one outside), and an arrow flying through it hangs and crawls and falls again after.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_lock", timeoutTicks = 160)
    public static void timeLockHoldsCreaturesAndHangsProjectiles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        ServerPlayer player = f.player(10.5, FLOOR, 13.5);
        Zombie inside = zombie(helper, 13.5, 10.5);
        Zombie outside = zombie(helper, 10.5, 19.5);
        Wolf wolf = helper.spawn(EntityType.WOLF, new Vec3(10.5, FLOOR, 7.5));
        wolf.setNoAi(true);
        wolf.tame(thanos);
        Arrow arrow = EntityType.ARROW.create(level);
        arrow.setNoGravity(false);
        arrow.moveTo(helper.absoluteVec(new Vec3(7.5, FLOOR + 3.0, 8.5)));
        arrow.setDeltaMovement(0.6, 0.0, 0.0);
        level.addFreshEntity(arrow);
        Arrow floating = EntityType.ARROW.create(level);
        floating.setNoGravity(true);
        floating.moveTo(helper.absoluteVec(new Vec3(7.5, FLOOR + 3.0, 12.5)));
        floating.setDeltaMovement(0.6, 0.0, 0.0);
        level.addFreshEntity(floating);
        pressSneaking(thanos, 5);
        check(cooldownLeft(thanos, 5) == ThanosConfig.TIME_LOCK_COOLDOWN.get(), "the lock's cooldown: " + cooldownLeft(thanos, 5));
        f.everyTick(ThanosConfig.TIME_LOCK_TICKS.get() + 20, t -> {
            PowerTicker.tick(thanos);
            if (t == 10) {
                check(inside.hasEffect(ModEffects.TIME_LOCKED), "the zombie in the dome is locked");
                check(!outside.hasEffect(ModEffects.TIME_LOCKED), "the one outside is not");
                check(!wolf.hasEffect(ModEffects.TIME_LOCKED) && !thanos.hasEffect(ModEffects.TIME_LOCKED), "his wolf and he himself are free");
                check(!player.hasEffect(ModEffects.TIME_LOCKED), "a player with PvP off is free");
                assertNear(inside.getAttributeValue(Attributes.MOVEMENT_SPEED), inside.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 0.4, 0.002,
                        "locked, he moves at four tenths");
                check(arrow.isNoGravity(), "the arrow hangs");
                check(arrow.getDeltaMovement().length() < 0.1, "and crawls: " + arrow.getDeltaMovement().length());
            }
            if (t == ThanosConfig.TIME_LOCK_TICKS.get() + 8) {
                check(!inside.hasEffect(ModEffects.TIME_LOCKED), "the dome closed: the zombie is free");
                check(!arrow.isNoGravity() || arrow.isRemoved(), "and gravity is back on the arrow");
                check(floating.isNoGravity(), "but not on the one that had none of its own");
                return true;
            }
            return false;
        });
    }

    /** A player is locked only with PvP on. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_lock_pvp", timeoutTicks = 60)
    public static void timeLockHoldsPlayersOnlyWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        ServerPlayer player = f.player(10.5, FLOOR, 13.5);
        ServerPlayer creative = f.player(13.5, FLOOR, 10.5, GameType.CREATIVE);
        pressSneaking(thanos, 5);
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t < 8) {
                return false;
            }
            check(player.hasEffect(ModEffects.TIME_LOCKED), "with PvP on the player is locked");
            check(!creative.hasEffect(ModEffects.TIME_LOCKED), "a Creative player never is");
            return true;
        });
    }

    // ============================================================================================================
    // What ends with him

    /**
     * The dome and the chain end with him: when his life, his session, his dimension or his power goes, whoever the
     * dome held is free, the arrow it hung falls, the drain lets go, the sight shuts, the veil is down; and what he
     * turned to stone is flesh again.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_ends", timeoutTicks = 100)
    public static void whatStandsForHimEndsWithHisLifeHisSessionAndHisPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        PowerData data = Powers.get(thanos);
        Zombie inside = zombie(helper, 13.5, 10.5);
        Zombie stone = zombie(helper, 10.5, 15.5);
        Arrow arrow = EntityType.ARROW.create(level);
        arrow.moveTo(helper.absoluteVec(new Vec3(8.5, FLOOR + 3.0, 8.5)));
        arrow.setDeltaMovement(0.2, 0.0, 0.0);
        level.addFreshEntity(arrow);
        pressSneaking(thanos, 5);
        lookAt(thanos, stone);
        ready(thanos, 3);
        press(thanos, 3);
        pressSneaking(thanos, 4);
        TestKit.look(thanos, 0.0F, 0.0F);
        f.everyTick(60, t -> {
            PowerTicker.tick(thanos);
            if (t == 12) {
                check(inside.hasEffect(ModEffects.TIME_LOCKED) && arrow.isNoGravity(), "the dome stands");
                check(stone.hasEffect(ModEffects.PETRIFIED), "the zombie is stone");
                check(data.is(Live.SOUL_SIGHT), "the sight is on");
                HeroLifecycle.onDeath(thanos);
                check(!inside.hasEffect(ModEffects.TIME_LOCKED) && !arrow.isNoGravity(), "his death opens the dome");
                check(!data.is(Live.SOUL_SIGHT) && !data.is(Live.SOUL_DRAINING), "and shuts the sight");
                check(!stone.hasEffect(ModEffects.PETRIFIED), "and what he turned to stone is flesh again");
                ready(thanos, 5);
                pressSneaking(thanos, 5);
                pressSneaking(thanos, 3);
                check(data.toggle(Toggle.REALITY_VEIL), "the veil is drawn");
            }
            if (t == 20) {
                check(inside.hasEffect(ModEffects.TIME_LOCKED), "the dome stands again");
                Powers.setPower(thanos, Power.NONE);
                check(!inside.hasEffect(ModEffects.TIME_LOCKED), "losing the power opens the dome");
                check(!data.toggle(Toggle.REALITY_VEIL), "and drops the veil");
                return true;
            }
            return false;
        });
    }

    /** His session ending ends them too. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_ends_logout", timeoutTicks = 60)
    public static void theDomeEndsWhenHeLogsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 10.5, 10.5);
        Zombie inside = zombie(helper, 13.5, 10.5);
        pressSneaking(thanos, 5);
        f.everyTick(12, t -> {
            PowerTicker.tick(thanos);
            if (t == 8) {
                check(inside.hasEffect(ModEffects.TIME_LOCKED), "the dome stands");
                f.level().getServer().getPlayerList().remove(thanos);
                check(!inside.hasEffect(ModEffects.TIME_LOCKED), "his leaving opens it");
                return true;
            }
            return false;
        });
    }

    /** Held down by the stones' own rule: letting the hold go (a screen, a death) lets the chain go. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_ends_hold")
    public static void releasingTheHoldLetsTheChainGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = thanos(f, 3.5, 10.5);
        zombie(helper, 8.5, 10.5);
        PowerData data = Powers.get(thanos);
        drain(thanos);
        check(data.is(Live.SOUL_DRAINING), "locked");
        PowerTicker.stopHoldAbilities(data);
        check(!data.is(Live.SOUL_DRAINING), "the hold is released");
        PowerTicker.tick(thanos);
        check(cooldownLeft(thanos, 4) == ThanosConfig.DRAIN_COOLDOWN.get(), "and his tick finishes it, with the cooldown");
        helper.succeed();
    }

    // ============================================================================================================
    // The Gauntlet, and the hero beside the boss

    /**
     * The Gauntlet's hold: the five voices in turn, from Power to Time; then a bolt of light that hurts nobody, and he
     * is Thanos.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_gauntlet", timeoutTicks = 60)
    public static void theGauntletLightsTheFiveStonesInTurnAndStrikesWithALightThatHurtsNobody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        InfinityGauntletItem gauntlet = (InfinityGauntletItem) ModItems.INFINITY_GAUNTLET.get();
        ItemStack stack = new ItemStack(gauntlet);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        TestKit.heardSounds(player);
        int hold = gauntlet.holdTicks();
        for (int remaining = hold; remaining >= 1; remaining--) {
            gauntlet.onUseTick(level, player, stack, remaining);
        }
        check(ThanosTests.voices(player).equals(List.of("power", "space", "reality", "soul", "time")), "the five voices, in the order of the stones");
        float health = player.getHealth();
        gauntlet.finishUsingItem(stack, level, player);
        check(Powers.power(player) == Power.THANOS && Powers.variant(player).id().equals("infinity_war"), "he is Thanos in the Infinity War look");
        check(player.getHealth() == health && player.isAlive(), "the bolt hurt nobody");
        check(!level.getEntitiesOfClass(LightningBolt.class, new AABB(player.blockPosition()).inflate(4.0)).isEmpty(), "and it struck where he stood");
        check(player.getMainHandItem().isEmpty(), "the Gauntlet is used up");
        helper.succeed();
    }

    /**
     * The hero beside the boss: his Power Blast is a beam (the Barrier turns it away), his Soul Drain is not a
     * projectile (it passes), and what he does counts in the fight's ledger.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_boss", timeoutTicks = 160)
    public static void theHeroBlastIsRangedForTheBarrierAndTheDrainPassesAndBothAreCredited(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        ServerPlayer thanos = ThanosTests.thanos(f, 4.5, 10.5);
        ThanosEntity boss = ThanosBossTests.boss(f, thanos);
        boss.forceStones(2);
        TestKit.look(thanos, TestKit.EAST, 0.0F);
        lookAt(thanos, boss);
        boss.forceMove(ThanosMove.SPACE_BARRIER, thanos);
        float[] mark = {0};
        f.everyTick(100, t -> {
            PowerTicker.tick(thanos);
            if (t == 12) {
                check(boss.flag(ThanosEntity.FLAG_BARRIER), "the Barrier is up");
                mark[0] = boss.getHealth();
                lookAt(thanos, boss);
                press(thanos, 1);
                check(boss.getHealth() == mark[0], "the Barrier turned the hero's Blast away");
                lookAt(thanos, boss);
                drain(thanos);
            }
            if (t == 30) {
                check(boss.getHealth() < mark[0], "the Soul Drain is no projectile: it passed the Barrier: " + boss.getHealth() + " of " + mark[0]);
                AbilityHandler.onKey(thanos, 4, false);
                check(boss.ledger().getOrDefault(thanos.getUUID(), 0.0F) > 0.0F, "and the damage counts in the ledger");
            }
            if (t == 12 + ThanosBossConfig.BARRIER_TICKS.get() + 6) {
                check(!boss.flag(ThanosEntity.FLAG_BARRIER), "the Barrier fell");
                mark[0] = boss.getHealth();
                ready(thanos, 1);
                lookAt(thanos, boss);
                press(thanos, 1);
                check(boss.getHealth() < mark[0], "without it the Blast hurts him");
                return true;
            }
            return false;
        });
    }

    /**
     * What holds, the Mad Titan shakes off: a Transmute (it cracks and falls), a Pull (he is not drawn anywhere) and a Time
     * Lock (the dome does not slow him): the tags that keep a boss from being held.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_boss_held", timeoutTicks = 160)
    public static void theMadTitanShakesOffTheStonesThatHold(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        ServerPlayer thanos = ThanosTests.thanos(f, 7.5, 10.5);
        ThanosEntity boss = ThanosBossTests.boss(f, thanos);
        Vec3 stood = boss.position();
        lookAt(thanos, boss);
        TestKit.sentPackets(thanos);
        press(thanos, 3);
        pressSneaking(thanos, 2);
        pressSneaking(thanos, 5);
        f.everyTick(40, t -> {
            PowerTicker.tick(thanos);
            if (t < 12) {
                return false;
            }
            check(told(TestKit.sentPackets(thanos), "message.enhanced.thanos.shaken_off"), "'The stone cracks and falls'");
            check(!boss.hasEffect(ModEffects.PETRIFIED), "the Titan is not stone");
            check(!boss.hasEffect(ModEffects.TIME_LOCKED), "and the dome does not slow him");
            check(boss.position().distanceTo(stood) < 0.5, "and he was not drawn anywhere");
            return true;
        });
    }

    /** A hand turned to stone lets go of the spells it held: a Scarlet Witch's beam and shield stop. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_stone_hand", timeoutTicks = 60)
    public static void aPetrifiedHandLetsGoOfHeldSpells(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(wanda);
        data.setBeamActive(true);
        data.setChaosShieldActive(true);
        wanda.addEffect(new MobEffectInstance(ModEffects.PETRIFIED, 100, 0, false, false));
        wanda.doTick();
        check(!data.beamActive() && !data.chaosShieldActive(), "stone cannot hold a beam or a shield up");
        helper.succeed();
    }

    /** Ability keys of another hero's plug-in do nothing for Thanos: his keys reach only his own stones. */
    @EnhancedTest(template = ARENA, batch = "thanos_hero_keys")
    public static void aKeyReachesOnlyTheStonesOfThePlayersOwnPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(10.5, FLOOR, 10.5);
        zombie(helper, 14.5, 10.5);
        TestKit.look(plain, TestKit.EAST, 0.0F);
        for (int ability = 1; ability <= 5; ability++) {
            press(plain, ability);
            pressSneaking(plain, ability);
        }
        PowerData data = Powers.get(plain);
        check(!data.toggle(Toggle.REALITY_VEIL) && !data.is(Live.SOUL_DRAINING) && !data.is(Live.SOUL_SIGHT), "a player with no power has no stones");
        for (int ability = 1; ability <= 5; ability++) {
            check(data.isReady(ability, f.level().getGameTime()), "ability " + ability + " paid nothing");
        }
        ServerPlayer thor = f.player(5.5, FLOOR, 5.5, Power.THOR);
        press(thor, 1);
        check(!Powers.get(thor).toggle(Toggle.REALITY_VEIL), "nor does Thor's key reach the Veil");
        helper.succeed();
    }

    private ThanosSoulTimeTests() {
    }
}
