package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.block.WitchesRoadDoorBlock;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.network.AgathaFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.road.RoadDoors;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import com.kolodkingal.enhanced.vishanti.WardManager;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Agatha Harkness (3.3): the Darkhold makes her of a player with no power; her body (40 health, flight), her place among the
 * sorcerers, the Purple Beam and the book's hex mark on her keys, the poison for magic used on her, the Power Drain, the
 * Witch's Shield, the Rune Trap, her dark magic (the tag the ward of the Vishanti keeps out), the Witches' Road, what ends
 * when she loses the power, and her effects' payload. Quick tests share the batch {@code agatha}; a test that changes a
 * setting (PvP, a config option) or walks the Road has a batch of its own.
 */
public final class AgathaTests {
    private static final String BATCH = "agatha";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    private static ServerPlayer agatha(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.AGATHA);
    }

    /** A player holding the Darkhold in the main hand. */
    private static ServerPlayer reader(TestKit.Fixture f, double x, double z, Power power) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        return player;
    }

    /** Use on the book, as a right click. */
    private static void use(ServerPlayer player) {
        player.getMainHandItem().use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
    }

    /** {@code ticks} ticks of a held use of the book (the fake player is not ticked by anyone). */
    private static void read(ServerPlayer player, int from, int ticks) {
        ItemStack stack = player.getUseItem();
        int duration = stack.getUseDuration(player);
        for (int held = from; held < from + ticks && player.isUsingItem(); held++) {
            stack.getItem().onUseTick(player.serverLevel(), player, stack, duration - held);
        }
    }

    /** A hostile creature that stands still (a husk: it does not burn in the sun, which would hurt it in the tests' place). */
    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
    }

    private static Pig pig(GameTestHelper helper, double x, double z) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(x, FLOOR, z));
    }

    private static List<AgathaFxPayload> payloads(ServerPlayer player) {
        List<AgathaFxPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof AgathaFxPayload fx) {
                found.add(fx);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Becoming Agatha: the Darkhold

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPlayerWithNoPowerWhoReadsTheDarkholdBecomesAgatha(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer reader = reader(f, 10.5, 10.5, Power.NONE);
        check(DarkholdBook.awakens(reader) && !DarkholdBook.mayUse(reader), "the book would wake Agatha in someone with no power");
        use(reader);
        check(reader.isUsingItem() && !ContinuousBeam.isFiring(reader), "use is held: a reading, not a beam");
        int total = AgathaConfig.AWAKEN_TICKS.get();
        read(reader, 0, total - 1);
        check(Powers.power(reader) == Power.NONE && AgathaAbilities.isReading(reader), "not before the reading is done");
        read(reader, total - 1, 2);
        check(Powers.power(reader) == Power.AGATHA, "then she is Agatha: " + Powers.power(reader));
        check(Powers.variant(reader).id().equals("wandavision"), "in her first look");
        check(!reader.isUsingItem() && !AgathaAbilities.isReading(reader), "the reading is over");
        check(DarkholdBook.isBook(reader.getMainHandItem()) && reader.getMainHandItem().getCount() == 1, "and the book is kept");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aReadingCutShortChangesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer reader = reader(f, 10.5, 10.5, Power.NONE);
        use(reader);
        read(reader, 0, AgathaConfig.AWAKEN_TICKS.get() / 2);
        check(AgathaAbilities.isReading(reader), "half way through");
        reader.stopUsingItem();
        check(Powers.power(reader) == Power.NONE && !AgathaAbilities.isReading(reader), "let go early: nothing happens");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aHeroWhoReadsItDoesNotBecomeAgatha(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power power : new Power[]{Power.TOMMY, Power.THOR, Power.PHOENIX}) {
            ServerPlayer hero = reader(f, 10.5, 10.5, power);
            check(!DarkholdBook.awakens(hero), power + ": a hero is not woken");
            TestKit.sentPackets(hero);
            use(hero);
            check("message.enhanced.darkhold.not_sorcerer".equals(lastActionBarKey(hero)), power + ": only a sorcerer can read it");
            check(Powers.power(hero) == power && !hero.isUsingItem(), power + ": and stays who they were");
        }
        ServerPlayer loki = reader(f, 10.5, 10.5, Power.LOKI);
        check(!DarkholdBook.awakens(loki) && DarkholdBook.mayUse(loki), "a sorcerer reads it as in 3.2");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "agatha_no_awakening")
    public static void withTheOptionOffTheBookOnlyRefuses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(AgathaConfig.DARKHOLD_AWAKENS, false);
        ServerPlayer reader = reader(f, 10.5, 10.5, Power.NONE);
        check(!DarkholdBook.awakens(reader), "darkholdAwakens off: nobody is woken");
        TestKit.sentPackets(reader);
        use(reader);
        check("message.enhanced.darkhold.not_sorcerer".equals(lastActionBarKey(reader)) && !reader.isUsingItem(), "the 3.2 refusal");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who she is

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herNamesLooksAndKeys(GameTestHelper helper) {
        check(Power.AGATHA.ordinal() == Power.APOCALYPSE.ordinal() + 1, "Agatha comes right after Apocalypse");
        check(Power.byName("agatha") == Power.AGATHA && Power.AGATHA.hasPowers(), "she is a power, found by her name");
        List<Variant> looks = Variants.of(Power.AGATHA);
        check(looks.size() == 3 && looks.get(0).id().equals("wandavision") && looks.get(1).id().equals("all_along")
                && looks.get(2).id().equals("own"), "her looks are WandaVision, Agatha All Along and your own: " + looks);
        check(looks.get(0).slim() && looks.get(1).slim(), "both suits have slim arms");
        check(Power.AGATHA.canFly() && Power.AGATHA.flightAbility() == 2, "she flies on G");
        assertNear(Power.AGATHA.flightSpeed(), AgathaConfig.FLIGHT_SPEED.get(), 1.0E-6, "at her own speed");
        check(Power.AGATHA.abilityKey(3).equals("ability.enhanced.agatha.3"), "her abilities are named by her keys");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheHasFortyHealthAndFlies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        assertNear(player.getMaxHealth(), 20.0 + AgathaConfig.BODY_HEALTH_BONUS.get(), 1.0E-6, "40 health with the default bonus");
        assertNear(player.getMaxHealth(), 40.0, 1.0E-6, "like the other strong heroes");
        check(player.mayFly(), "she may fly");
        assertNear(player.getAbilities().getFlyingSpeed(), AgathaConfig.FLIGHT_SPEED.get(), 1.0E-4, "at her speed");
        Powers.setPower(player, Power.NONE);
        assertNear(player.getMaxHealth(), 20.0, 1.0E-6, "without the power she is as anyone");
        check(!player.mayFly(), "and walks");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheIsASorcerer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        check(WardStrength.isSorcerer(Power.AGATHA) && WardStrength.isSorcerer(player), "the one list of the sorcerers has her");
        check(DarkholdBook.mayUse(player), "she may use the Darkhold in her hand as the other sorcerers do");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        check(VishantiBook.readable(player, false), "and read the Book of the Vishanti");
        check(WardStrength.radius(Power.AGATHA, 0.0) == VishantiConfig.WARD_RADIUS_AGATHA.get(), "her ward is wardRadiusAgatha");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the Purple Beam, and Sneak + R

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herBeamIsTheDarkholdsPurpleBeam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        Pig pig = pig(helper, 10.5, 14.5);
        lookAt(player, pig);
        AbilityHandler.onKey(player, 1, true);
        check(ContinuousBeam.isFiring(player) && AgathaAbilities.firing(player) && Powers.get(player).is(Live.BEAMING),
                "R held: the beam burns, with no book in hand");
        float before = pig.getHealth();
        PowerTicker.tick(player);
        check(pig.getHealth() < before, "it hurts what the crosshair is on");
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.DARKHOLD_BEAM), "with the Darkhold's own damage: " + last);
        AbilityHandler.onKey(player, 1, false);
        check(!ContinuousBeam.isFiring(player) && !Powers.get(player).is(Live.BEAMING) && !AgathaAbilities.firing(player),
                "R released: it ends");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sneakRIsTheDarkholdsHexMark(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        Zombie zombie = zombie(helper, 10.5, 12.5);
        lookAt(player, zombie);
        player.setShiftKeyDown(true);
        press(player, 1);
        player.setShiftKeyDown(false);
        check(zombie.hasEffect(MobEffects.GLOWING) && zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the creature is hex-marked");
        check(DarkholdBook.markerOf(zombie, helper.getLevel().getGameTime()) != null, "by her");
        check(Powers.get(player).is(Live.CASTING) && !ContinuousBeam.isFiring(player), "her hand rises (and no beam)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The poison

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void magicUsedOnHerPoisonsWhoeverUsedIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        check(!DarkholdBook.holds(player), "no book in hand");
        Pig caster = pig(helper, 12.5, 10.5);
        player.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.HEX_BOLT, caster, caster), 1.0F);
        check(caster.hasEffect(MobEffects.POISON), "whoever used magic on her is poisoned");
        Pig other = pig(helper, 8.5, 10.5);
        player.invulnerableTime = 0;
        player.hurt(helper.getLevel().damageSources().mobAttack(other), 1.0F);
        check(!other.hasEffect(MobEffects.POISON), "a plain blow is not magic");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Power Drain

    @EnhancedTest(template = ARENA, batch = "agatha_pvp")
    public static void theDrainTakesAHerosPowerForAWhile(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        ServerPlayer loki = f.player(10.5, FLOOR, 12.5, Power.LOKI);
        lookAt(player, loki);
        player.setHealth(20.0F);
        long now = helper.getLevel().getGameTime();
        press(player, 3);
        MobEffectInstance drained = loki.getEffect(ModEffects.DRAINED);
        check(drained != null && drained.getDuration() == AgathaConfig.DRAIN_TICKS.get(), "Loki's power is drained for drainTicks");
        assertNear(player.getHealth(), 20.0 + AgathaConfig.DRAIN_HEAL.get(), 1.0E-4, "she heals by it");
        check(!Powers.get(player).isReady(AgathaAbilities.DRAIN_SLOT, now), "and the drain recharges");
        PowerData data = Powers.get(loki);
        boolean before = data.toggle(Toggle.FROST_GIANT);
        TestKit.sentPackets(loki);
        press(loki, 5);
        check(data.toggle(Toggle.FROST_GIANT) == before, "no key of his works");
        check("message.enhanced.agatha.you_are_drained".equals(lastActionBarKey(loki)), "and he is told why");
        // A flier comes down while drained, and flies again when it ends.
        ServerPlayer wanda = f.player(4.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        check(wanda.mayFly(), "the Scarlet Witch may fly");
        wanda.addEffect(new MobEffectInstance(ModEffects.DRAINED, 100), player);
        PowerTicker.applyPassives(wanda, Powers.get(wanda));
        check(!wanda.mayFly(), "drained, she may not");
        wanda.removeEffect(ModEffects.DRAINED);
        PowerTicker.applyPassives(wanda, Powers.get(wanda));
        check(wanda.mayFly(), "the power comes back");
        helper.succeed();
    }

    /**
     * 3.5 ({@code agatha.drainOnlySorcerers}, on): only a witch's or sorcerer's power can be drained: Loki, the Scarlet
     * Witch, Billy, another Agatha, and a custom hero with an ability of any of them. Tommy, Thor, a custom hero with only
     * Tommy's speed, and Tommy holding the Darkhold are refused (with the reason), and the refusal costs nothing.
     */
    @EnhancedTest(template = ARENA, batch = "agatha_pvp")
    public static void theDrainTakesOnlyAWitchsOrSorcerersPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        check(AgathaConfig.DRAIN_ONLY_SORCERERS.getDefault(), "on by default");
        ServerPlayer player = agatha(f, 10.5, 2.5);
        record Target(String name, ServerPlayer player, boolean drained) {
        }
        ServerPlayer tommyWithTheBook = f.player(19.5, FLOOR, 12.5, Power.TOMMY);
        tommyWithTheBook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        List<Target> targets = List.of(
                new Target("Loki", f.player(1.5, FLOOR, 12.5, Power.LOKI), true),
                new Target("the Scarlet Witch", f.player(3.5, FLOOR, 12.5, Power.SCARLET_WITCH), true),
                new Target("Billy", f.player(5.5, FLOOR, 12.5, Power.BILLY), true),
                new Target("another Agatha", f.player(7.5, FLOOR, 12.5, Power.AGATHA), true),
                new Target("a custom hero with Loki's daggers", CustomKitSupport.hero(f, 9.5, 12.5,
                        CustomKitSupport.def().pick("loki.daggers", 5).build()), true),
                new Target("a custom hero with Wanda's telekinesis", CustomKitSupport.hero(f, 11.5, 12.5,
                        CustomKitSupport.def().pick("tommy.speed", 5).pick("wanda.telekinesis", 5).build()), true),
                new Target("Tommy", f.player(13.5, FLOOR, 12.5, Power.TOMMY), false),
                new Target("Thor", f.player(15.5, FLOOR, 12.5, Power.THOR), false),
                new Target("a custom hero with Tommy's speed only", CustomKitSupport.hero(f, 17.5, 12.5,
                        CustomKitSupport.def().pick("tommy.speed", 5).build()), false),
                new Target("Tommy holding the Darkhold", tommyWithTheBook, false));
        for (Target target : targets) {
            check(AgathaAbilities.drainable(Powers.get(target.player())) == target.drained(), target.name() + " drainable: " + target.drained());
            Powers.get(player).startCooldown(HeroAbility.AGATHA_DRAIN, helper.getLevel().getGameTime(), 0);
            lookAt(player, target.player());
            TestKit.sentPackets(player);
            press(player, 3);
            long now = helper.getLevel().getGameTime();
            if (target.drained()) {
                check(target.player().hasEffect(ModEffects.DRAINED), target.name() + "'s power is drained");
                check("message.enhanced.agatha.drained".equals(lastActionBarKey(player)), "and she is told so");
            } else {
                check(!target.player().hasEffect(ModEffects.DRAINED), target.name() + "'s power cannot be drained");
                check("message.enhanced.agatha.drain_not_sorcerer".equals(lastActionBarKey(player)),
                        "she is told only a witch's or sorcerer's power can be: " + lastActionBarKey(player));
                check(Powers.get(player).isReady(AgathaAbilities.DRAIN_SLOT, now), "and the refusal costs nothing");
            }
        }
        helper.succeed();
    }

    /** 3.5: with {@code agatha.drainOnlySorcerers} off, the drain takes any hero's power again (the rule of 3.3). */
    @EnhancedTest(template = ARENA, batch = "agatha_drain_any")
    public static void withDrainOnlySorcerersOffAnyHeroIsDrained(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(AgathaConfig.DRAIN_ONLY_SORCERERS, false);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        ServerPlayer tommy = f.player(10.5, FLOOR, 12.5, Power.TOMMY);
        lookAt(player, tommy);
        press(player, 3);
        check(tommy.hasEffect(ModEffects.DRAINED), "Tommy's power is drained");
        PowerData data = Powers.get(tommy);
        boolean before = data.toggle(Toggle.SUPER_SPEED);
        press(tommy, 1);
        check(data.toggle(Toggle.SUPER_SPEED) == before, "no key of his works");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "agatha_pvp")
    public static void theDrainNeedsAPowerToTake(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        ServerPlayer nobody = f.player(10.5, FLOOR, 12.5);
        lookAt(player, nobody);
        TestKit.sentPackets(player);
        press(player, 3);
        check("message.enhanced.agatha.no_power".equals(lastActionBarKey(player)) && !nobody.hasEffect(ModEffects.DRAINED),
                "a player with no power has nothing to drain");
        check(Powers.get(player).isReady(AgathaAbilities.DRAIN_SLOT, helper.getLevel().getGameTime()), "and the refusal costs nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "agatha_no_pvp")
    public static void withPvpOffSheCannotDrainAPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(false);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        ServerPlayer wanda = f.player(10.5, FLOOR, 12.5, Power.SCARLET_WITCH);
        lookAt(player, wanda);
        TestKit.sentPackets(player);
        press(player, 3);
        check("message.enhanced.agatha.drain_refused".equals(lastActionBarKey(player)) && !wanda.hasEffect(ModEffects.DRAINED),
                "PvP off: refused");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDrainDrinksACreaturesLife(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        Zombie zombie = zombie(helper, 10.5, 12.5);
        lookAt(player, zombie);
        player.setHealth(20.0F);
        press(player, 3);
        assertNear(zombie.getMaxHealth() - zombie.getHealth(), AgathaConfig.DRAIN_DAMAGE.get(), 0.6, "its life is drawn out");
        DamageSource last = zombie.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.AGATHA_DRAIN) && last.is(ModTags.DARK_MAGIC), "by dark magic: " + last);
        assertNear(player.getHealth(), 20.0 + AgathaConfig.DRAIN_HEAL.get(), 1.0E-4, "into her");
        List<AgathaFxPayload> fx = payloads(player);
        check(fx.stream().anyMatch(p -> p.kind() == AgathaFxPayload.DRAIN && p.other() == zombie.getId()), "the stream is drawn: " + fx);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Witch's Shield

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theShieldTakesTheBlowsAndShatters(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        int windUp = AgathaShield.windUpTicks();
        f.everyTick(windUp + 10, t -> {
            PowerData data = Powers.get(player);
            if (t == 1) {
                AbilityHandler.onKey(player, AgathaShield.SLOT, true);
                check(data.chaosShieldActive() && !AgathaShield.isUp(player), "B: the shield forms, and stops nothing yet");
                return false;
            }
            if (t < windUp + 3) {
                return false;
            }
            check(AgathaShield.isUp(player), "after the wind-up it is up");
            float health = player.getHealth();
            check(!player.hurt(helper.getLevel().damageSources().generic(), 10.0F) && player.getHealth() == health,
                    "a blow is stopped");
            assertNear(data.shieldStrength(), AgathaShield.maxStrength() - 10.0F, 1.0E-4, "and taken off its strength");
            Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
            arrow.setPos(player.getX() + 2.0, player.getY() + 1.0, player.getZ());
            arrow.setDeltaMovement(-1.0, 0.0, 0.0);
            helper.getLevel().addFreshEntity(arrow);
            PowerTicker.tick(player);
            check(arrow.isRemoved(), "an arrow is turned to sparks before it hits");
            float left = data.shieldStrength();
            player.hurt(helper.getLevel().damageSources().generic(), left + 5.0F);
            check(!data.chaosShieldActive(), "a blow stronger than what is left shatters it");
            assertNear(player.getHealth(), health - 5.0F, 1.0E-3, "and the rest goes through");
            long now = helper.getLevel().getGameTime();
            check(data.cooldownUntil(AgathaShield.SLOT) - now == AgathaConfig.SHIELD_BROKEN_COOLDOWN.get(), "the long recharge");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void lettingGoOfBDropsTheShield(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        PowerData data = Powers.get(player);
        AbilityHandler.onKey(player, AgathaShield.SLOT, true);
        check(data.chaosShieldActive(), "held: up");
        AbilityHandler.onKey(player, AgathaShield.SLOT, false);
        long now = helper.getLevel().getGameTime();
        check(!data.chaosShieldActive() && data.cooldownUntil(AgathaShield.SLOT) - now == AgathaConfig.SHIELD_COOLDOWN.get(),
                "let go: down, the short recharge");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the Rune Trap

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRunesGoWhereSheLooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 10.5);
        look(player, 0.0F, 90.0F);
        Vec3 down = AgathaAbilities.runeCenter(player);
        assertNear(down.y, player.getY(), 0.01, "looking down: on the floor at her feet");
        assertNear(down.x, player.getX(), 0.2, "under her");
        TestKit.sentPackets(player);
        press(player, 5);
        AgathaAbilities.RuneTrap trap = AgathaAbilities.trapOf(player);
        check(trap != null && trap.end() - trap.start() == AgathaConfig.RUNE_TICKS.get(), "N lays the circle for runeTicks");
        assertNear(trap.radius(), AgathaConfig.RUNE_RADIUS.get(), 1.0E-6, "of runeRadius");
        check(payloads(player).stream().anyMatch(p -> p.kind() == AgathaFxPayload.RUNES && p.c() == AgathaConfig.RUNE_TICKS.get()),
                "everyone near is told");
        check(!Powers.get(player).isReady(AgathaAbilities.RUNE_SLOT, helper.getLevel().getGameTime()), "and N recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 160)
    public static void theRunesBindAndBurnTheHostileOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 2.5);
        Zombie zombie = zombie(helper, 10.5, 12.5);
        Pig pig = pig(helper, 12.5, 10.5);
        Zombie outside = zombie(helper, 3.5, 17.5);
        AgathaAbilities.lay(player, helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)));
        int ticks = AgathaConfig.RUNE_TICKS.get();
        f.everyTick(ticks + 6, t -> {
            PowerTicker.tick(player);
            if (t == 2) {
                check(zombie.hasEffect(ModEffects.SPELLBOUND), "a zombie in the circle is bound");
                check(!pig.hasEffect(ModEffects.SPELLBOUND) && !outside.hasEffect(ModEffects.SPELLBOUND), "not the pig, not the zombie outside");
            }
            if (t == 30) {
                DamageSource last = zombie.getLastDamageSource();
                check(zombie.getHealth() < zombie.getMaxHealth() && last != null && last.is(ModDamageTypes.AGATHA_RUNES)
                        && last.is(ModTags.DARK_MAGIC), "and burned by dark magic: " + last);
                check(pig.getHealth() == pig.getMaxHealth(), "the pig is not burned");
            }
            if (t == ticks + 4) {
                check(AgathaAbilities.trapOf(player) == null, "the runes go out after runeTicks");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Dark magic, and the ward of the Vishanti

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herMagicIsDarkMagic(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (var key : List.of(ModDamageTypes.DARKHOLD_BEAM, ModDamageTypes.AGATHA_DRAIN, ModDamageTypes.AGATHA_RUNES)) {
            var holder = registry.getHolderOrThrow(key);
            check(holder.is(ModTags.DARK_MAGIC), key.location() + " is dark magic (enhanced:dark_magic)");
            check(holder.is(ModTags.MAGIC_DAMAGE), key.location() + " is magic (the Darkhold answers it)");
        }
        check(!registry.getHolderOrThrow(ModDamageTypes.HEX_BEAM).is(ModTags.DARK_MAGIC)
                && !registry.getHolderOrThrow(ModDamageTypes.CHAOS_BEAM).is(ModTags.DARK_MAGIC), "chaos magic is not dark magic");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "agatha_ward")
    public static void aWardOfTheVishantiKeepsHerMagicOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(VishantiConfig.WARD_RADIUS_LOKI, 6);
        f.config(VishantiConfig.WARD_COOLDOWN, 0);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = f.player(15.5, FLOOR, 12.5, Power.LOKI);
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        f.onClose(() -> WardManager.removeAll(level.getServer(), loki.getUUID()));
        VishantiBook.savePlace(loki, "Fort", true);
        check(WardManager.protects(level, loki.position()), "Loki's ward stands");
        Pig pig = pig(helper, 10.5, 14.5);
        ServerPlayer player = agatha(f, 10.5, 1.5);
        check(AgathaAbilities.wardStops(player, pig.position()) && !AgathaAbilities.wardStops(player, player.position()),
                "the ward keeps her magic out of it");
        lookAt(player, pig);
        TestKit.sentPackets(player);
        press(player, 3);
        check("message.enhanced.agatha.warded".equals(lastActionBarKey(player)) && pig.getHealth() == pig.getMaxHealth(),
                "the drain does not reach in");
        check(Powers.get(player).isReady(AgathaAbilities.DRAIN_SLOT, level.getGameTime()), "and costs nothing");
        look(player, 0.0F, 90.0F);
        player.teleportTo(loki.getX(), loki.getY(), loki.getZ() - 3.0);
        check(!AgathaAbilities.wardStops(player, pig.position()), "inside the ward herself, nothing comes in from outside");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Witches' Road

    @EnhancedTest(template = ARENA, batch = "witches_road_walk_agatha")
    public static void agathaWalksTheRoadFromDoorToDoor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ModAttachments.ROAD_DOORS.set(level, RoadDoors.EMPTY);
        BlockPos a = new BlockPos(3, 2, 3);
        BlockPos b = new BlockPos(17, 2, 3);
        helper.setBlock(a, ModBlocks.WITCHES_ROAD_DOOR.get());
        helper.setBlock(b, ModBlocks.WITCHES_ROAD_DOOR.get());
        BlockPos doorA = helper.absolutePos(a);
        BlockPos doorB = helper.absolutePos(b);
        ServerPlayer player = agatha(f, 3.5, 5.5);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(doorA).add(0, WitchesRoad.DOOR_HEIGHT, 0), Direction.UP, doorA, false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        Vec3 on = Vec3.atBottomCenterOf(doorB).add(0, WitchesRoad.DOOR_HEIGHT, 0);
        check(player.position().distanceTo(on) < 0.01, "she comes up out of the next door: " + player.position());
        check(level.getBlockState(doorA).getValue(WitchesRoadDoorBlock.OPEN), "the door opened for her");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The end of the power, and the payload

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void losingThePowerPutsEverythingOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = agatha(f, 10.5, 4.5);
        look(player, 0.0F, 0.0F);
        AbilityHandler.onKey(player, 1, true);
        AbilityHandler.onKey(player, AgathaShield.SLOT, true);
        AgathaAbilities.lay(player, player.position());
        PowerData data = Powers.get(player);
        check(ContinuousBeam.isFiring(player) && data.chaosShieldActive() && AgathaAbilities.trapOf(player) != null, "everything at once");
        Powers.setPower(player, Power.NONE);
        check(!ContinuousBeam.isFiring(player) && !data.chaosShieldActive() && AgathaAbilities.trapOf(player) == null
                && !AgathaAbilities.firing(player), "the beam, the shield and the runes are gone with the power");
        check(player.getAttribute(Attributes.MAX_HEALTH).getValue() == 20.0, "and so is her health");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herEffectsPayloadRoundTrips(GameTestHelper helper) {
        AgathaFxPayload sent = new AgathaFxPayload(AgathaFxPayload.RUNES, 42, new Vec3(1.5, 64.0, -3.25), 7, 100, 40);
        ByteBuf buf = Unpooled.buffer();
        AgathaFxPayload.STREAM_CODEC.encode(buf, sent);
        AgathaFxPayload back = AgathaFxPayload.STREAM_CODEC.decode(buf);
        check(back.equals(sent), "the payload comes back whole: " + back);
        check(AgathaFxPayload.TYPE.id().equals(Enhanced.id("agatha_fx")), "under its name");
        helper.succeed();
    }
}
