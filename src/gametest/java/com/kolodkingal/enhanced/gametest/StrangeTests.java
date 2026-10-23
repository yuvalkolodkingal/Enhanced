package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKeys;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.network.SlingRingActionPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.TimeRewind;
import com.kolodkingal.enhanced.strange.AstralBodyEntity;
import com.kolodkingal.enhanced.strange.AstralProjection;
import com.kolodkingal.enhanced.strange.CrimsonBands;
import com.kolodkingal.enhanced.strange.EldritchWhip;
import com.kolodkingal.enhanced.strange.EyeOfAgamotto;
import com.kolodkingal.enhanced.strange.EyeTime;
import com.kolodkingal.enhanced.strange.MirrorDimension;
import com.kolodkingal.enhanced.strange.SeraphimShield;
import com.kolodkingal.enhanced.strange.SlingPortalEntity;
import com.kolodkingal.enhanced.strange.SlingPortals;
import com.kolodkingal.enhanced.strange.SlingRingActions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * Doctor Strange (3.6, lane L3): the Eye of Agamotto (the awakening of the powerless only, the Eye kept and owned, a
 * sorcerer's time, a god's closed Eye, Y without the Eye), every key of his (the whip's combo and pull, the grapple, the
 * Shield against an arrow, the Dome for a friend, the Bands on a zombie and on a boss, the portals, the Astral
 * Projection, the Mirror Dimension), his body, his custom rows (R1 on another key, R3 the variant, the Y row) and the
 * Sling Ring. Quick tests share the batch {@code strange}; the portals {@code strange_portal} (their life shortened:
 * {@code strange_portal_cfg}); the spirit {@code
 * strange_astral}; the Mirror and the arenas (server-wide state, a config) {@code strange_mirror}; the tests that hurt
 * players {@code strange_pvp}.
 */
public final class StrangeTests {
    private static final String BATCH = "strange";
    private static final String PORTAL = "strange_portal";
    private static final String PORTAL_CFG = "strange_portal_cfg";
    private static final String ASTRAL = "strange_astral";
    private static final String MIRROR = "strange_mirror";
    private static final String PVP = "strange_pvp";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer strange(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.DOCTOR_STRANGE);
    }

    private static void press(ServerPlayer player, int ability, boolean sneak) {
        CustomKitSupport.tap(player, ability, sneak);
    }

    private static void ready(ServerPlayer player, int slot) {
        Powers.get(player).startCooldown(slot, player.level().getGameTime(), 0);
    }

    /** A husk that stands still with plenty of health. */
    private static Zombie husk(GameTestHelper helper, double x, double z) {
        Zombie husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return husk;
    }

    private static ItemStack eye(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.EYE_OF_AGAMOTTO.get());
        player.getInventory().add(stack);
        return stack;
    }

    /** Holds the Eye in his main hand for the whole awakening (its use, then its end). */
    private static void holdEye(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        if (player.isUsingItem()) {
            stack.getItem().finishUsingItem(stack, player.level(), player);
            player.stopUsingItem();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Eye of Agamotto

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEyeMakesThePowerlessDoctorStrangeAndStaysHis(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = f.player(4.5, FLOOR, 4.5);
        ItemStack stack = new ItemStack(ModItems.EYE_OF_AGAMOTTO.get());
        check(EyeOfAgamotto.use(peter, stack) == EyeOfAgamotto.Use.AWAKEN, "a powerless player may awaken");
        holdEye(peter, stack);
        check(Powers.power(peter) == Power.DOCTOR_STRANGE, "he holds the Eye 3 s: Doctor Strange");
        check(peter.getMainHandItem().is(ModItems.EYE_OF_AGAMOTTO.get()) && stack.getCount() == 1, "the Eye is not used up");
        MjolnirOwner owner = EyeOfAgamotto.owner(stack);
        check(owner != null && owner.is(peter), "the Eye is his");
        // Another powerless player: the Eye already has a Sorcerer Supreme.
        ServerPlayer other = f.player(8.5, FLOOR, 4.5);
        check(EyeOfAgamotto.use(other, stack) == EyeOfAgamotto.Use.TAKEN, "an owned Eye is taken");
        EyeOfAgamotto.awaken(other, stack);
        check(Powers.power(other) == Power.NONE, "it awakens nobody else");
        // When Strange loses the power the Eye is unowned again.
        Powers.setPower(peter, Power.NONE);
        check(EyeOfAgamotto.owner(peter.getMainHandItem()) == null, "the Eye is unowned when he loses the power");
        check(EyeOfAgamotto.use(other, stack) == EyeOfAgamotto.Use.AWAKEN, "then it may awaken another");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aHeroWhoIsNoSorcererFindsTheEyeClosed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(4.5, FLOOR, 4.5, Power.THOR);
        ItemStack stack = eye(thor);
        check(EyeOfAgamotto.use(thor, stack) == EyeOfAgamotto.Use.CLOSED, "Thor is no sorcerer: closed");
        EyeOfAgamotto.use(thor, stack, false);
        PowerData data = Powers.get(thor);
        check(!data.is(Live.TIME_SLOWING) && !data.toggle(Toggle.TIME_SLOW), "the Eye stays closed for him");
        check(Powers.power(thor) == Power.THOR, "and makes him nothing else");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void billyUsesTheEyeForTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        ItemStack stack = eye(billy);
        check(EyeOfAgamotto.use(billy, stack) == EyeOfAgamotto.Use.SORCERER, "Billy is a sorcerer");
        EyeOfAgamotto.use(billy, stack, false);
        check(Powers.get(billy).is(Live.TIME_SLOWING), "the Eye's Slow Time is on round him");
        EyeOfAgamotto.use(billy, stack, false);
        check(!Powers.get(billy).is(Live.TIME_SLOWING), "and off again");
        check(billy.getCooldowns().isOnCooldown(ModItems.EYE_OF_AGAMOTTO.get()), "the Eye recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void yWithoutTheEyeIsRefusedAndWithItSlowsTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 4.5, 4.5);
        check(HeroKeys.yRow(Powers.get(strange)) == HeroAbility.STRANGE_EYE, "his Y is the Eye");
        press(strange, HeroKeys.Y, false);
        check(!Powers.get(strange).is(Live.TIME_SLOWING), "no Eye with him: Y is refused");
        eye(strange);
        press(strange, HeroKeys.Y, false);
        PowerData data = Powers.get(strange);
        check(data.is(Live.TIME_SLOWING) && data.toggle(Toggle.TIME_SLOW), "with the Eye: time slows round him");
        // The zone holds a zombie near him (the zones are gathered once a tick: look on the next one).
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(6.5, FLOOR, 4.5));
        helper.runAfterDelay(1, () -> {
            check(com.kolodkingal.enhanced.ability.SlowMotion.isInZone(helper.getLevel(), zombie.position()),
                    "the zombie stands in the slowed time");
            press(strange, HeroKeys.Y, false);
            check(!data.is(Live.TIME_SLOWING), "Y again: off");
            check(strange.getCooldowns().isOnCooldown(ModItems.EYE_OF_AGAMOTTO.get()), "and the Eye recharges");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEyesRewindTakesHimBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 4.5, 4.5);
        eye(strange);
        Vec3 before = strange.position();
        TimeRewind.rememberAgo(strange, 60);
        Vec3 away = helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5));
        strange.teleportTo(away.x, away.y, away.z);
        press(strange, HeroKeys.Y, true);
        check(strange.position().distanceTo(before) < 1.0, "Sneak + Y: back where he was");
        check(strange.getCooldowns().isOnCooldown(ModItems.EYE_OF_AGAMOTTO.get()), "the Eye recharges");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the whip

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWhipsComboHurtsAndPulls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 4.5);
        Zombie husk = husk(helper, 10.5, 9.5);
        husk.setNoAi(false);
        float hp = husk.getHealth();
        press(strange, 1, false);
        assertNear(hp - husk.getHealth(), 7.0, 0.01, "the first lash: 7");
        check(husk.getDeltaMovement().z < -0.1, "it is pulled toward him");
        check(EldritchWhip.comboStep(strange.getUUID(), strange.level().getGameTime()) == 1, "the combo's first lash");
        float[] expected = {7.0F, 10.0F};
        for (int lash = 2; lash <= 3; lash++) {
            ready(strange, 1);
            husk.invulnerableTime = 0;
            float before = husk.getHealth();
            press(strange, 1, false);
            assertNear(before - husk.getHealth(), expected[lash - 2], 0.01, "lash " + lash);
        }
        check(husk.getDeltaMovement().y > 0.3, "the third lash lifts it");
        check(EldritchWhip.nextStep(3, 0, 5) == 1 && EldritchWhip.nextStep(1, 0, EldritchWhip.COMBO_RESET + 1) == 1
                && EldritchWhip.nextStep(1, 0, 10) == 2, "the combo: three, and it lapses");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGrapplePullsHimToTheBlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 2.5);
        BlockPos wall = new BlockPos(10, 4, 14);
        helper.setBlock(wall, Blocks.STONE);
        look(strange, 0.0F, -6.0F);
        press(strange, 1, true);
        check(EldritchWhip.swinging(strange.getUUID()), "the whip caught the block: he swings");
        Vec3 v = strange.getDeltaMovement();
        check(v.z > 0.5, "pulled toward the block: " + v);
        check(!Powers.get(strange).isReady(1, strange.level().getGameTime() + EldritchWhip.grappleCooldown() - 5),
                "the grapple's cooldown");
        check(EldritchWhip.fallForgiven(strange.getUUID(), strange.level().getGameTime()), "his fall is forgiven");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Shield and the Dome

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShieldStopsAnArrowFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 4.5);
        CustomKitSupport.press(strange, 3, false, true);
        check(SeraphimShield.isUp(strange) && Powers.get(strange).is(Live.STRANGE_SHIELD), "V held: the Shield stands");
        ServerLevel level = helper.getLevel();
        Arrow arrow = new Arrow(level, strange.getX(), strange.getEyeY() - 0.2, strange.getZ() + 2.4,
                new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(0, 0, -1.2);
        level.addFreshEntity(arrow);
        PowerTicker.tick(strange);
        check(arrow.isRemoved(), "the arrow turns to sparks at the disc");
        float hit = SeraphimShield.harm(strange, level.damageSources().mobAttack(husk(helper, 10.5, 6.5)), 10.0F);
        assertNear(hit, 10.0F * SeraphimShield.FRONT_REDUCTION, 0.01, "a blow from the front loses most of its harm");
        CustomKitSupport.press(strange, 3, false, false);
        check(!Powers.get(strange).is(Live.STRANGE_SHIELD), "V let go: it comes down");
        check(!Powers.get(strange).isReady(3, level.getGameTime() + 5), "and recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDomeShieldsAFriendInside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        ServerPlayer friend = f.player(12.0, FLOOR, 10.5);
        press(strange, 3, true);
        check(SeraphimShield.domeUp(strange.getUUID(), helper.getLevel().getGameTime()), "Sneak + V: the Dome stands");
        check(SeraphimShield.shelters(friend), "the friend is inside it");
        float before = friend.getHealth();
        friend.hurt(helper.getLevel().damageSources().generic(), 8.0F);
        assertNear(before - friend.getHealth(), 4.0, 0.01, "he takes half the harm");
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 11.5));
        check(!SeraphimShield.shelters(zombie), "a monster inside is not sheltered");
        check(!Powers.get(strange).isReady(3, helper.getLevel().getGameTime() + SeraphimShield.domeCooldown() - 5),
                "the Dome's long recharge");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Bands

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBandsBindAZombieFourSecondsAndABossOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 2.5);
        Zombie zombie = husk(helper, 10.5, 8.5);
        TestKit.lookAt(strange, zombie);
        press(strange, 4, false);
        MobEffectInstance bound = zombie.getEffect(ModEffects.SPELLBOUND);
        check(bound != null && bound.getDuration() > 70 && bound.getDuration() <= CrimsonBands.ticks(),
                "the zombie is spellbound 4 s: " + bound);
        assertNear(200.0F - zombie.getHealth(), CrimsonBands.DAMAGE, 0.01, "the bands close with 2 harm");
        check(!Powers.get(strange).isReady(4, helper.getLevel().getGameTime() + 100), "the bands' cooldown");
        Warden boss = TestKit.spawnWithNoFreeWill(helper, EntityType.WARDEN, new Vec3(4.5, FLOOR, 12.5));
        CrimsonBands.bind(strange, boss);
        MobEffectInstance held = boss.getEffect(ModEffects.SPELLBOUND);
        check(CrimsonBands.isBoss(boss) && (held == null ? CrimsonBands.heldTicks(boss) <= CrimsonBands.bossTicks()
                : held.getDuration() <= CrimsonBands.bossTicks()), "a boss is bound for a second only");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // G: the Cloak

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void gLiftsHimOnTheCloakAndSetsHimDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 4.5, 10.5);
        PowerTicker.tick(strange);
        check(strange.mayFly(), "the Cloak lets him fly");
        press(strange, 2, false);
        check(strange.getAbilities().flying, "G: he rises on the Cloak");
        press(strange, 2, false);
        check(!strange.getAbilities().flying, "G again: he lands");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The body

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisBodyAndTheCloaksSoftLanding(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        PowerTicker.tick(strange);
        assertNear(strange.getAttributeValue(Attributes.MAX_HEALTH), 26.0, 0.01, "+6 health");
        assertNear(strange.getAttributeValue(Attributes.ARMOR), 2.0, 0.01, "+2 armour");
        net.neoforged.neoforge.event.entity.living.LivingFallEvent short_ = net.neoforged.neoforge.common.NeoForge.EVENT_BUS
                .post(new net.neoforged.neoforge.event.entity.living.LivingFallEvent(strange, 10.0F, 1.0F));
        check(short_.isCanceled(), "the Cloak catches a fall of 10 blocks");
        net.neoforged.neoforge.event.entity.living.LivingFallEvent long_ = net.neoforged.neoforge.common.NeoForge.EVENT_BUS
                .post(new net.neoforged.neoforge.event.entity.living.LivingFallEvent(strange, 16.0F, 1.0F));
        check(!long_.isCanceled(), "a fall of 16 is not caught whole");
        assertNear(long_.getDistance(), 4.0, 0.01, "a fall of 16 hurts as one of 4");
        Powers.setPower(strange, Power.NONE);
        PowerTicker.tick(strange);
        assertNear(strange.getAttributeValue(Attributes.MAX_HEALTH), 20.0, 0.01, "the body goes with the power");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Hero Creator

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCustomHeroLashesOnHisOwnKeyAndGrapplesOnSneak(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("strange.whip", 5).key(3, "strange.whip").build();
        ServerPlayer hero = CustomKitSupport.hero(f, 10.5, 4.5, def);
        Zombie husk = husk(helper, 10.5, 9.5);
        CustomKitSupport.tap(hero, 3, false);
        check(husk.getHealth() < 200.0F, "R1: the whip lashes from his third key");
        long now = helper.getLevel().getGameTime();
        check(!Powers.get(hero).isReady(3, now) && Powers.get(hero).isReady(1, now), "its cooldown is on that key");
        helper.setBlock(new BlockPos(10, 4, 16), Blocks.STONE);
        look(hero, 0.0F, -5.0F);
        Powers.get(hero).startCooldown(3, now, 0);
        CustomKitSupport.tap(hero, 3, true);
        check(EldritchWhip.swinging(hero.getUUID()), "R3: Sneak + that key is the Whip Grapple");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEyeIsACustomHerosYRowAndOnlyOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero def = CustomKitSupport.def().pick("strange.eye", 5).build();
        ServerPlayer hero = CustomKitSupport.hero(f, 4.5, 4.5, def);
        check(HeroKeys.yRow(Powers.get(hero)) == HeroAbility.STRANGE_EYE, "his Y is the Eye");
        eye(hero);
        CustomKitSupport.tap(hero, HeroKeys.Y, false);
        check(Powers.get(hero).is(Live.TIME_SLOWING), "Y with the Eye slows time for a custom hero too");
        CustomHero both = CustomKitSupport.def().pick("billy.build_magic", 5).pick("strange.eye", 5).raw();
        CustomHeroRules.Result r = CustomHeroRules.sanitize(both, CustomHeroRules.Limits.DEFAULT);
        check(!r.notes().isEmpty() && r.hero().powers().size() == 1, "Build Magic or the Eye: one Y row only");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the portals

    /** Strange at the north of the floor, facing south, looking at the floor near the south. */
    private static ServerPlayer opener(TestKit.Fixture f) {
        ServerPlayer strange = strange(f, 10.5, 2.5);
        look(strange, 0.0F, 10.0F);
        return strange;
    }

    @EnhancedTest(template = ARENA, batch = PORTAL, timeoutTicks = 200)
    public static void aPairCarriesAZombieAndAnItemKeepingTheirSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = opener(f);
        press(strange, 5, false);
        SlingPortals.Pair pair = SlingPortals.of(strange.getUUID());
        check(pair != null, "N: a pair opens");
        ServerLevel level = helper.getLevel();
        SlingPortalEntity near = (SlingPortalEntity) level.getEntity(pair.near());
        SlingPortalEntity far = (SlingPortalEntity) level.getEntity(pair.far());
        check(near != null && far != null, "both rings stand");
        check(far.position().distanceTo(near.position()) > 4.0, "the far ring where he looks");
        Vec3 nc = near.center();
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE,
                helper.relativeVec(nc.add(0, -1.25, -0.6)));
        ItemEntity item = new ItemEntity(level, nc.x + 0.3, nc.y, nc.z - 1.5, new ItemStack(Items.DIAMOND));
        item.setDeltaMovement(0, 0.05, 0.45);
        item.setNoGravity(true);
        level.addFreshEntity(item);
        f.everyTick(60, t -> {
            if (t == 3) {
                zombie.teleportTo(nc.x, nc.y - 1.25, nc.z + 0.6);
            }
            if (t < 6) {
                return false;
            }
            boolean zombieThrough = zombie.position().distanceTo(far.position()) < 2.0;
            boolean itemThrough = item.position().distanceTo(far.center()) < 5.0 && item.getDeltaMovement().z > 0.3;
            return zombieThrough && itemThrough;
        });
    }

    @EnhancedTest(template = ARENA, batch = PORTAL_CFG, timeoutTicks = 260)
    public static void onePairAtATimeAndItsFarChunkIsLetGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StrangeConfig.PORTAL_SECONDS, 2);
        ServerPlayer strange = opener(f);
        press(strange, 5, false);
        SlingPortals.Pair first = SlingPortals.of(strange.getUUID());
        ready(strange, 5);
        press(strange, 5, false);
        SlingPortals.Pair second = SlingPortals.of(strange.getUUID());
        check(first != null && second != null && !first.id().equals(second.id()), "a second pair");
        check(SlingPortals.count() == 1 && !SlingPortals.holdsTicket(first.id()), "the first is closed, its chunk let go");
        f.everyTick(120, t -> t > 5 && SlingPortals.count() == 0 && !SlingPortals.holdsTicket(second.id())
                && helper.getLevel().getEntity(second.near()) == null);
    }

    @EnhancedTest(template = ARENA, batch = PORTAL)
    public static void theRingsPlaneIsCrossedEitherWayWithinItsRadius(GameTestHelper helper) {
        Vec3 c = Vec3.ZERO;
        Vec3 n = new Vec3(0, 0, 1);
        check(SlingPortals.crossing(c, n, new Vec3(0, 0, -0.5), new Vec3(0, 0, 0.5)) != null, "forward through");
        check(SlingPortals.crossing(c, n, new Vec3(0.3, 0, 0.5), new Vec3(0.3, 0, -0.5)) != null, "back through");
        check(SlingPortals.crossing(c, n, new Vec3(2, 0, -0.5), new Vec3(2, 0, 0.5)) == null, "beside the ring");
        check(SlingPortals.crossing(c, n, new Vec3(0, 0, 0.2), new Vec3(0, 0, 0.6)) == null, "not across");
        Vec3 turned = SlingPortals.turn(new Vec3(0, 0, 1), 90);
        assertNear(turned.x, -1.0, 1.0E-6, "a quarter turn of yaw faces west");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Sling Ring

    @EnhancedTest(template = ARENA, batch = PORTAL)
    public static void theSlingRingSavesAndOpensAWayToAnotherWorldForASorcerer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(10.5, FLOOR, 4.5, Power.BILLY);
        billy.getInventory().add(new ItemStack(ModItems.SLING_RING.get()));
        SlingRingActions.handle(billy, SlingRingActionPayload.SAVE, -1, "home");
        BifrostData book = PlaceBook.Kind.SLING_RING.data(billy);
        check(book.places().size() == 1 && book.places().get(0).name().equals("home"), "the ring remembers home");
        // A place in the Nether.
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        check(nether != null, "the Nether is there");
        BlockPos spot = new BlockPos(0, 100, 0);
        nether.setBlockAndUpdate(spot.below(), Blocks.OBSIDIAN.defaultBlockState());
        PlaceBook.Kind.SLING_RING.set(billy, book.with(new BifrostData.Place("nether", Level.NETHER,
                Vec3.atBottomCenterOf(spot), 0.0F, false)));
        int index = PlaceBook.Kind.SLING_RING.data(billy).places().size() - 1;
        SlingRingActions.handle(billy, SlingRingActionPayload.TRAVEL, index, "nether");
        SlingPortals.Pair pair = SlingPortals.of(billy.getUUID());
        check(pair != null && pair.farDimension() == Level.NETHER && pair.nearDimension() == helper.getLevel().dimension(),
                "a pair from here to the Nether");
        check(PlaceBook.cooldown(billy, PlaceBook.Kind.SLING_RING) > 0, "the ring rests");
        SlingPortals.close(helper.getLevel().getServer(), pair.id());
        // Not for one who is no sorcerer; never to Asgard.
        ServerPlayer peter = f.player(4.5, FLOOR, 4.5);
        peter.getInventory().add(new ItemStack(ModItems.SLING_RING.get()));
        SlingRingActions.handle(peter, SlingRingActionPayload.SAVE, -1, "mine");
        check(PlaceBook.Kind.SLING_RING.data(peter).places().isEmpty(), "the ring is just a ring for him");
        PlaceBook.readyAt(billy, PlaceBook.Kind.SLING_RING, 0);
        PlaceBook.Kind.SLING_RING.set(billy, PlaceBook.Kind.SLING_RING.data(billy).with(new BifrostData.Place("asgard",
                AsgardDimension.KEY, Vec3.ZERO, 0.0F, false)));
        int asgard = PlaceBook.Kind.SLING_RING.data(billy).places().size() - 1;
        SlingRingActions.handle(billy, SlingRingActionPayload.TRAVEL, asgard, "asgard");
        check(SlingPortals.of(billy.getUUID()) == null, "the ring cannot reach Asgard");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + B: the Astral Projection

    @EnhancedTest(template = ARENA, batch = ASTRAL)
    public static void theSpiritLeavesItsBodyAndTheBodyCallsItBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        PowerTicker.tick(strange);
        strange.setHealth(20.0F);
        Vec3 where = strange.position();
        press(strange, 4, true);
        AstralBodyEntity body = AstralProjection.bodyOf(strange);
        check(body != null && body.position().distanceTo(where) < 0.01, "his body kneels where he stood");
        check(AstralProjection.isAstral(strange) && VisionAbilities.isPhased(strange), "the spirit passes through walls");
        check(strange.getAbilities().flying, "and floats: it never sinks through the floor");
        Vec3 away = helper.absoluteVec(new Vec3(16.5, FLOOR, 16.5));
        strange.teleportTo(away.x, away.y, away.z);
        strange.hurt(helper.getLevel().damageSources().generic(), 5.0F);
        assertNear(strange.getHealth(), 20.0, 0.01, "the spirit itself takes no harm");
        body.hurt(helper.getLevel().damageSources().generic(), 8.0F);
        check(!AstralProjection.isAstral(strange) && body.isRemoved(), "the body calls him back");
        check(strange.position().distanceTo(where) < 0.6 && !strange.getAbilities().flying,
                "he is back where his body knelt, standing");
        assertNear(strange.getHealth(), 16.0, 0.01, "with half the harm the body took");
        check(!Powers.get(strange).isReady(4, helper.getLevel().getGameTime() + 100), "the projection recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = ASTRAL)
    public static void aSpiritThatLogsOutReturnsToItsBodyFirst(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        Vec3 where = strange.position();
        press(strange, 4, true);
        AstralBodyEntity body = AstralProjection.bodyOf(strange);
        check(body != null, "the body kneels");
        Vec3 away = helper.absoluteVec(new Vec3(16.5, FLOOR, 4.5));
        strange.teleportTo(away.x, away.y, away.z);
        helper.getLevel().getServer().getPlayerList().remove(strange);
        check(strange.position().distanceTo(where) < 0.6, "he left from his body's place");
        check(body.isRemoved() && !Powers.get(strange).toggle(Toggle.STRANGE_ASTRAL), "the body is gone, the switch off");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + N: the Mirror Dimension

    @EnhancedTest(template = ARENA, batch = MIRROR)
    public static void insideTheMirrorAnExplosionBreaksNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StrangeConfig.MIRROR_RADIUS, 6);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        press(strange, 5, true);
        check(MirrorDimension.of(strange.getUUID()) != null && Powers.get(strange).toggle(Toggle.STRANGE_MIRROR),
                "Sneak + N: the Mirror Dimension opens");
        for (int x = 12; x <= 14; x++) {
            helper.setBlock(new BlockPos(x, 2, 12), Blocks.STONE);
        }
        Vec3 blast = helper.absoluteVec(new Vec3(13.5, 2.5, 12.5));
        helper.getLevel().explode(null, blast.x, blast.y, blast.z, 3.0F, Level.ExplosionInteraction.TNT);
        for (int x = 12; x <= 14; x++) {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, 12));
        }
        helper.assertBlockPresent(Blocks.SMOOTH_STONE, new BlockPos(13, 1, 12));
        ServerPlayer other = f.player(12.5, FLOOR, 8.5);
        BlockPos stone = helper.absolutePos(new BlockPos(12, 2, 12));
        check(!BlockRights.mayBreak(other, helper.getLevel(), stone, helper.getLevel().getBlockState(stone)),
                "nobody else breaks a block inside");
        check(MirrorDimension.protects(other, helper.getLevel(), stone)
                && !MirrorDimension.protects(strange, helper.getLevel(), stone), "but he may");
        press(strange, 5, true);
        check(MirrorDimension.of(strange.getUUID()) == null && !Powers.get(strange).toggle(Toggle.STRANGE_MIRROR),
                "Sneak + N again: it closes");
        check(!MirrorDimension.protects(other, helper.getLevel(), stone), "and the world is the real one again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = MIRROR, timeoutTicks = 100)
    public static void aZombieHeHurtInTheMirrorCannotLeave(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StrangeConfig.MIRROR_RADIUS, 6);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        press(strange, 5, true);
        Zombie husk = husk(helper, 10.5, 13.5);
        press(strange, 1, false);
        check(MirrorDimension.holds(husk), "whipped inside: it is held");
        Vec3 out = helper.absoluteVec(new Vec3(10.5, FLOOR, 18.5));
        husk.teleportTo(out.x, out.y, out.z);
        f.everyTick(40, t -> {
            PowerTicker.tick(strange);
            MirrorDimension.Mirror mirror = MirrorDimension.of(strange.getUUID());
            check(mirror != null, "the Mirror still stands at tick " + t);
            boolean in = mirror.contains(husk.position());
            check(in || t < 40, "the zombie is set back inside: it is at " + husk.position() + ", " + mirror);
            return in;
        });
    }

    // (Its own batch: a boss fixture forgets every hero's statics when it closes.)
    @EnhancedTest(template = ARENA, batch = "strange_arena")
    public static void theMirrorAndTheSpiritAreRefusedInABossArena(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        BossArena arena = new BossTestSupport.TestArena(f.level(), helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 8.0,
                BossArena.Containment.NONE, true).containAt(6.0).joinWithin(9.0).tractorDepth(0.0);
        BossTestSupport.TestFight fight = new BossTestSupport.TestFight(f.level(), BossKind.APOCALYPSE, arena);
        BossFights.register(BossKind.APOCALYPSE, new BossTestSupport.TestLane(BossKind.APOCALYPSE, p -> fight));
        check(BossFights.start(BossKind.APOCALYPSE, strange, false), "the test fight starts");
        press(strange, 5, true);
        check(MirrorDimension.of(strange.getUUID()) == null, "no Mirror Dimension in a boss's arena");
        press(strange, 4, true);
        check(!AstralProjection.isAstral(strange), "no spirit out of its body there either");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Players

    @EnhancedTest(template = ARENA, batch = PVP, timeoutTicks = 100)
    public static void aPlayerHeHurtInTheMirrorCannotLeaveWhilePvpIsOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(StrangeConfig.MIRROR_RADIUS, 6);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        ServerPlayer foe = f.player(10.5, FLOOR, 13.5);
        press(strange, 5, true);
        press(strange, 1, false);
        check(foe.getHealth() < 20.0F, "the lash hurt him");
        check(MirrorDimension.holds(foe), "with PvP on he is held");
        Vec3 out = helper.absoluteVec(new Vec3(10.5, FLOOR, 18.5));
        foe.teleportTo(out.x, out.y, out.z);
        f.everyTick(40, t -> {
            PowerTicker.tick(strange);
            MirrorDimension.Mirror mirror = MirrorDimension.of(strange.getUUID());
            return mirror != null && mirror.contains(foe.position());
        });
    }

    @EnhancedTest(template = ARENA, batch = PVP)
    public static void theShieldTurnsAPlayersBlowFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer strange = strange(f, 10.5, 10.5);
        ServerPlayer foe = f.player(10.5, FLOOR, 12.5);
        TestKit.lookAt(foe, strange);
        CustomKitSupport.press(strange, 3, false, true);
        PowerTicker.tick(strange);
        strange.setHealth(20.0F);
        strange.hurt(helper.getLevel().damageSources().playerAttack(foe), 10.0F);
        assertNear(20.0F - strange.getHealth(), 10.0F * SeraphimShield.FRONT_REDUCTION, 0.6,
                "a blow from the front: 40% of it");
        CustomKitSupport.press(strange, 3, false, false);
        helper.succeed();
    }

    private StrangeTests() {
    }
}
