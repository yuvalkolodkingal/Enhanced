package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.CosmicSpider;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.config.CosmicSpiderConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.SlingRingActionPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.strange.SlingPortals;
import com.kolodkingal.enhanced.strange.SlingRingActions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Cosmic Spider-Man (3.18, {@link CosmicSpider}): he becomes cosmic only with a full meter (170), the meter drains and never
 * recharges while he is, at 0 he is plain Spider-Man again and it fills up, and only full again lets him back in; while
 * cosmic he has a netherite set's protection, three Hulks' fists, flight and double healing, his blasts hurt, dark magic
 * hurts him more, his portals open without a Sling Ring and his Matter Restructuring is Billy's Build Magic, weaker.
 */
public final class CosmicSpiderTests {
    private static final String BATCH = "cosmic_spider";

    private static ServerPlayer spider(TestKit.Fixture f) {
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, Power.SPIDER_MAN);
        com.kolodkingal.enhanced.ultimate.Ultimate.grant(player, true); // (Ultimate is off by default: these tests are granted it)
        PowerTicker.tick(player);
        return player;
    }

    private static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    private static void sneakN(ServerPlayer player) {
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, 5, true);
        AbilityHandler.onKey(player, 5, false);
        player.setShiftKeyDown(false);
    }

    private static ServerPlayer cosmic(TestKit.Fixture f) {
        ServerPlayer player = spider(f);
        CosmicSpider.setEnergy(player, data(player), CosmicSpider.max());
        sneakN(player);
        PowerTicker.tick(player);
        check(CosmicSpider.isCosmic(data(player)), "a full meter and Sneak + N make him cosmic");
        return player;
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void heIsCosmicOnlyWithUltimateWhichIsOffByDefault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(!com.kolodkingal.enhanced.config.UltimateConfig.ENABLED.getDefault(), "the Ultimate switch is off by default");
        check(!com.kolodkingal.enhanced.ultimate.Ultimate.on(), "and off here");
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, Power.SPIDER_MAN);
        PowerTicker.tick(player);
        CosmicSpider.setEnergy(player, data(player), 170.0);
        sneakN(player);
        check(!CosmicSpider.isCosmic(data(player)) && !CosmicSpider.allowed(player), "without Ultimate a full meter does nothing");
        com.kolodkingal.enhanced.ultimate.Ultimate.grant(player, true);
        sneakN(player);
        check(CosmicSpider.isCosmic(data(player)), "granted Ultimate, he becomes Cosmic Spider-Man");
        com.kolodkingal.enhanced.ultimate.Ultimate.grant(player, false);
        PowerTicker.tick(player);
        check(!CosmicSpider.isCosmic(data(player)), "and when it is taken back he is plain Spider-Man again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void flyingHeShootsWebsWithGEvenAtTheOpenSky(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = cosmic(f);
        player.getAbilities().flying = true;
        player.setXRot(-89.0F); // straight up: nothing to hit
        AbilityHandler.onKey(player, 2, true);
        AbilityHandler.onKey(player, 2, false);
        check(data(player).is(Live.CASTING), "flying and aiming at the sky, G still shoots a web");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void heBecomesCosmicOnlyWithAFullMeter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f);
        assertNear(CosmicSpider.max(), 170.0, 1.0E-9, "the meter holds 170");
        CosmicSpider.setEnergy(player, data(player), 169.0);
        sneakN(player);
        check(!CosmicSpider.isCosmic(data(player)), "169 of 170 is not enough");
        CosmicSpider.setEnergy(player, data(player), 170.0);
        sneakN(player);
        check(CosmicSpider.isCosmic(data(player)) && data(player).is(Live.SPIDER_COSMIC), "170 is");
        PowerTicker.tick(player);
        check(player.mayFly(), "cosmic, he flies");
        assertNear(player.getAbilities().getFlyingSpeed(), CosmicSpiderConfig.FLIGHT_SPEED.get(), 1.0E-4, "at the cosmic speed");
        // Sneak + N again: plain Spider-Man, and the meter as it was.
        CosmicSpider.setEnergy(player, data(player), 120.0);
        double left = CosmicSpider.energy(data(player));
        sneakN(player);
        PowerTicker.tick(player);
        check(!CosmicSpider.isCosmic(data(player)) && !player.mayFly(), "Sneak + N lets it go: no more flight");
        check(CosmicSpider.energy(data(player)) >= left && CosmicSpider.energy(data(player)) < left + 0.2,
                "the meter keeps what was left, and begins to fill: " + CosmicSpider.energy(data(player)));
        sneakN(player);
        check(!CosmicSpider.isCosmic(data(player)), "and he cannot go back before it is full again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMeterDrainsThenHeRevertsAndItRefills(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CosmicSpiderConfig.DRAIN_PER_SECOND, 20.0);
        f.config(CosmicSpiderConfig.REFILL_PER_SECOND, 100.0);
        ServerPlayer player = cosmic(f);
        double before = CosmicSpider.energy(data(player));
        for (int i = 0; i < 10; i++) {
            PowerTicker.tick(player);
        }
        double after = CosmicSpider.energy(data(player));
        assertNear(before - after, 10.0, 0.05, "20 a second drains 1 a tick, and nothing comes back while cosmic");
        CosmicSpider.setEnergy(player, data(player), 1.5);
        PowerTicker.tick(player);
        PowerTicker.tick(player);
        check(!CosmicSpider.isCosmic(data(player)), "at 0 he is plain Spider-Man again");
        check(CosmicSpider.energy(data(player)) < 10.0, "the meter is near empty");
        sneakN(player);
        check(!CosmicSpider.isCosmic(data(player)), "an empty meter does not let him back");
        // 100 a second: 5 a tick, so 34 ticks fill it.
        for (int i = 0; i < 40; i++) {
            PowerTicker.tick(player);
        }
        check(CosmicSpider.full(data(player)), "the meter fills again while he is plain: " + CosmicSpider.energy(data(player)));
        sneakN(player);
        check(CosmicSpider.isCosmic(data(player)), "full again, he may be cosmic again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void cosmicHeHasNetheriteArmourHulkFistsAndHeals(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CosmicSpiderConfig.REGEN_INTERVAL_TICKS, 4);
        ServerPlayer plain = spider(f);
        assertNear(plain.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-9, "the homemade suit has no armour");
        ServerPlayer player = cosmic(f);
        assertNear(player.getAttributeValue(Attributes.ARMOR), 20.0, 1.0E-9, "armour of a full netherite set");
        assertNear(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0, 1.0E-9, "its toughness");
        assertNear(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.4, 1.0E-9, "its knockback resistance");
        // The Hulk's fist at 100% rage is (1 + 7) x 1.3 = 10.4: three times that.
        assertNear(CosmicSpider.fistDamage(), 31.2, 1.0E-6, "three Hulks at full rage");
        assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 31.2, 1.0E-6, "his bare fist hits for it");
        player.setHealth(10.0F);
        plain.setHealth(10.0F);
        f.everyTick(12, tick -> {
            PowerTicker.tick(player);
            PowerTicker.tick(plain);
            if (tick < 12) {
                return false;
            }
            check(player.getHealth() >= 11.9F, "cosmic, he heals on top of a player's own healing: " + player.getHealth());
            assertNear(plain.getHealth(), 10.0, 1.0E-6, "plain, he has only a player's own");
            // He lets go: his suit's own numbers come back.
            sneakN(player);
            PowerTicker.tick(player);
            assertNear(player.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-9, "the armour goes with the cosmic power");
            assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 3.0, 1.0E-6, "and the fists are Spider-Man's again");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void darkMagicHurtsHimMore(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = cosmic(f);
        DamageSource dark = ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM);
        DamageSource plain = helper.getLevel().damageSources().magic();
        check(CosmicSpider.isDark(dark) && !CosmicSpider.isDark(plain), "the Darkhold's beam is dark magic, plain magic is not");
        assertNear(CosmicSpider.onIncomingDamage(player, dark, 4.0F), 6.0, 1.0E-6, "dark magic: half again");
        assertNear(CosmicSpider.onIncomingDamage(player, plain, 4.0F), 4.0, 1.0E-6, "anything else as it is");
        // Through the game's own damage: the same blow of dark magic takes more of him than of a plain Spider-Man.
        ServerPlayer other = spider(f);
        player.invulnerableTime = 0;
        other.invulnerableTime = 0;
        float a = player.getHealth();
        float b = other.getHealth();
        player.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM), 4.0F);
        other.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM), 4.0F);
        float lostCosmic = a - player.getHealth();
        float lostPlain = b - other.getHealth();
        check(lostCosmic > 0.0F, "the dark blow hurts him: " + lostCosmic);
        check(!CosmicSpider.isCosmic(Powers.get(other)) && lostPlain > 0.0F, "and the plain one too: " + lostPlain);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hisBlastsHurtAndCostEnergy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = cosmic(f);
        Zombie husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(16.5, FLOOR, 10.5));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        husk.setHealth(1000.0F);
        TestKit.lookAt(player, husk.position().add(0, 1.0, 0));
        double before = CosmicSpider.energy(data(player));
        AbilityHandler.onKey(player, 1, true);
        check(ContinuousBeam.isFiring(player) && CosmicSpider.beam(player) == 1, "R held fires the Cosmic Blast from his hand");
        float[] hurt = new float[1];
        f.everyTick(30, tick -> {
            TestKit.lookAt(player, husk.position().add(0, 1.0, 0));
            PowerTicker.tick(player);
            if (tick == 12) {
                check(husk.getHealth() < 1000.0F, "the blast hurts: " + husk.getHealth());
                double spent = before - CosmicSpider.energy(data(player));
                check(spent > 12 * (0.75 / 20.0) + 0.5, "and costs more than the drain alone: " + spent);
                AbilityHandler.onKey(player, 1, false);
                check(!ContinuousBeam.isFiring(player), "letting go ends it");
                hurt[0] = husk.getHealth();
                player.setShiftKeyDown(true);
                AbilityHandler.onKey(player, 1, true);
                player.setShiftKeyDown(false);
                check(CosmicSpider.beam(player) == 2 && !data(player).is(Live.BEAMING), "Sneak + R: the eyes, no arm out");
            }
            if (tick == 26) {
                check(husk.getHealth() < hurt[0], "the Cosmic Eyes hurt too: " + husk.getHealth());
                AbilityHandler.onKey(player, 1, false);
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPortalsNeedNoRingAndCostEnergy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = cosmic(f);
        check(!SlingRingActions.isSorcerer(player) && !SlingRingActions.carries(player), "no sorcerer, no ring");
        SlingRingActions.handle(player, SlingRingActionPayload.SAVE, -1, "home");
        check(PlaceBook.Kind.SLING_RING.data(player).places().size() == 1, "cosmic, he saves a place");
        player.teleportTo(player.getX() + 4, player.getY(), player.getZ());
        double before = CosmicSpider.energy(data(player));
        SlingRingActions.handle(player, SlingRingActionPayload.TRAVEL, 0, "home");
        check(SlingPortals.of(player.getUUID()) != null, "a portal pair opens to it");
        assertNear(before - CosmicSpider.energy(data(player)), CosmicSpiderConfig.PORTAL_ENERGY.get(), 0.05, "for its cosmic cost");
        check(CosmicSpider.portalWait(player) > 0, "and he waits for the next one");
        SlingPortals.closeOwnedBy(player.server, player.getUUID());
        check(CosmicSpider.nextWorld(Level.OVERWORLD) == Level.NETHER && CosmicSpider.nextWorld(Level.NETHER) == Level.END
                && CosmicSpider.nextWorld(Level.END) == Level.OVERWORLD, "Sneak + B steps through the worlds");
        // Plain again, the screen's buttons do nothing for him.
        sneakN(player);
        SlingRingActions.handle(player, SlingRingActionPayload.SAVE, -1, "away");
        check(PlaceBook.Kind.SLING_RING.data(player).places().size() == 1, "plain, he has no ring to save with");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theChangeLookKeySwitchesHisTwoCosmicLooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = cosmic(f);
        String suit = Powers.variant(player).id();
        check(!CosmicSpider.universe(data(player)), "he starts in the starfield suit");
        CosmicSpider.switchLook(player, data(player));
        check(CosmicSpider.universe(data(player)), "H while cosmic: Captain Universe's look");
        check(Powers.variant(player).id().equals(suit), "his own suit is not changed by it");
        sneakN(player);
        check(CosmicSpider.universe(data(player)), "the choice is kept when he lets the power go");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void matterRestructuringIsBillysBuildMagicWeaker(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f);
        check(Limits.of(player) == null, "plain Spider-Man has no Build Magic");
        player = cosmic(f);
        Limits limits = Limits.of(player);
        check(limits != null && limits.billy() && limits.limited(), "cosmic, he has Billy's Build Magic with Billy's limits");
        Limits billy = Limits.billy(1, Limits.world(player.level()));
        assertNear(limits.factor(), 0.667, 1.0E-9, "at two thirds");
        check(limits.reach() == Math.round(billy.reach() * 0.667) && limits.side() == Math.round(billy.side() * 0.667),
                "two thirds of Billy's reach and side: " + limits.reach() + "/" + billy.reach());
        check(HeroAbility.SPIDER_COSMIC.parent() == HeroAbility.SPIDER_BLACK_EYES && HeroAbility.SPIDER_COSMIC.isVariant()
                && "spider.cosmic".equals(HeroAbility.SPIDER_COSMIC.id()), "the Hero Creator's row: Sneak + the Black Eyes");
        helper.succeed();
    }
}
