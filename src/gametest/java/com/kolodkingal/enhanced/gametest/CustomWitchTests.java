package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.Thoughts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Sorcery;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.CustomKitSupport.def;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.press;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.tap;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.tickN;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The witches' abilities in a custom hero's hands (3.4, lane L1): every row of the Scarlet Witch, Billy and Agatha on a key
 * that is not its home key does what the hero's own does, with its cooldown on that key (scaled by the strength); the Sneak
 * variants come through their Sneak cells (Billy's Hex Barrier held and let go, Agatha's hex mark, the Hex dissolved); a
 * shared field answers only the hero whose ability holds it (Agatha's shield is not a Chaos Shield, Billy's upkeep leaves
 * Agatha's beam alone); The Noise hears thoughts and makes a sorcerer; Build Magic is Billy's for a custom hero with it;
 * the Hex of a custom hero rises, is controlled, dissolves with Sneak and when he loses it; death puts everything out;
 * the damage grows with the strength. Quick tests share {@code custom_witch}; a test with PvP, a config change or a Hex has
 * a batch of its own.
 */
public final class CustomWitchTests {
    private static final String BATCH = "custom_witch";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer hero(TestKit.Fixture f, double x, double z, CustomKitSupport.Def def) {
        ServerPlayer player = CustomKitSupport.hero(f, x, z, def.build());
        player.getFoodData().setFoodLevel(20);
        return player;
    }

    private static Pig pig(GameTestHelper helper, double x, double z) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(x, FLOOR, z));
    }

    /** A hostile creature that stands still and does not burn in the sun. */
    private static Zombie husk(GameTestHelper helper, double x, double z) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
    }

    private static long now(GameTestHelper helper) {
        return helper.getLevel().getGameTime();
    }

    /** The ticks left on key {@code column}'s cooldown. */
    private static long left(GameTestHelper helper, ServerPlayer player, int column) {
        return Powers.get(player).cooldownUntil(column) - now(helper);
    }

    private static boolean ready(GameTestHelper helper, ServerPlayer player, int column) {
        return Powers.get(player).isReady(column, now(helper));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Scarlet Witch

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theChaosBeamOnGBurnsAndDizzies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 3.5, 10.5, def().pick("wanda.chaos_beam", 5).key(2, "wanda.chaos_beam"));
        PowerData data = Powers.get(player);
        Pig pig = pig(helper, 12.5, 10.5);
        lookAt(player, pig);
        press(player, 2, false, true);
        check(data.beamActive(), "G held: the Chaos Beam burns");
        tickN(player, 1);
        assertNear(pig.getMaxHealth() - pig.getHealth(), EnhancedConfig.CHAOS_BEAM_DAMAGE.get(), 1.0E-4, "a hit of the beam (strength 5)");
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.CHAOS_BEAM) && last.getEntity() == player, "the Chaos Beam's own damage: " + last);
        check(pig.hasEffect(MobEffects.CONFUSION), "and its Nausea");
        press(player, 2, false, false);
        check(!data.beamActive(), "G released: it stops");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDamageGrowsWithTheStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer weak = hero(f, 3.5, 4.5, def().pick("wanda.chaos_beam", 2).key(1, "wanda.chaos_beam"));
        ServerPlayer strong = hero(f, 3.5, 16.5, def().pick("wanda.chaos_beam", 9).key(1, "wanda.chaos_beam"));
        Pig a = pig(helper, 12.5, 4.5);
        Pig b = pig(helper, 12.5, 16.5);
        a.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        a.setHealth(40.0F);
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        b.setHealth(40.0F);
        lookAt(weak, a);
        lookAt(strong, b);
        press(weak, 1, false, true);
        press(strong, 1, false, true);
        tickN(weak, 1);
        tickN(strong, 1);
        float base = EnhancedConfig.CHAOS_BEAM_DAMAGE.get().floatValue();
        float weakHit = a.getMaxHealth() - a.getHealth();
        float strongHit = b.getMaxHealth() - b.getHealth();
        assertNear(weakHit, base * 0.7, 1.0E-3, "strength 2: 0.7 of the beam");
        assertNear(strongHit, base * 1.4, 1.0E-3, "strength 9: 1.4 of it");
        check(weakHit < strongHit, "strength 2 does less than 9");
        press(weak, 1, false, false);
        press(strong, 1, false, false);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWitchesTelekinesisOnOtherKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = hero(f, 3.5, 4.5, def().pick("wanda.telekinesis", 5).key(1, "wanda.telekinesis"));
        ServerPlayer billy = hero(f, 3.5, 16.5, def().pick("billy.telekinesis", 5).key(5, "billy.telekinesis"));
        Pig a = pig(helper, 9.5, 4.5);
        Pig b = pig(helper, 9.5, 16.5);
        lookAt(wanda, a);
        lookAt(billy, b);
        press(wanda, 1, false, true);
        press(billy, 5, false, true);
        check(Telekinesis.isHolding(wanda) && Powers.get(wanda).heldEntityId() == a.getId(), "R: her telekinesis holds the pig");
        check(Telekinesis.isHolding(billy) && Powers.get(billy).heldEntityId() == b.getId(), "N: his holds the other");
        Telekinesis.release(wanda, false);
        Telekinesis.release(billy, false);
        helper.succeed();
    }

    /** The Chaos Shield with its old rules (not unlimited), so that its recharge shows on the custom key. */
    @EnhancedTest(template = ARENA, batch = "custom_witch_shield", timeoutTicks = 60)
    public static void theChaosShieldOnVStopsBlowsAndRechargesOnV(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, false);
        ServerPlayer player = hero(f, 10.5, 10.5, def().pick("wanda.chaos_shield", 5).key(3, "wanda.chaos_shield"));
        PowerData data = Powers.get(player);
        press(player, 3, false, true);
        check(data.chaosShieldActive() && !WandaAbilities.hasShieldUp(player), "V: the shield forms, and stops nothing yet");
        helper.runAfterDelay(WandaAbilities.windUpTicks() + 1, () -> {
            check(WandaAbilities.hasShieldUp(player), "after its wind-up it is up");
            check(!AgathaShield.isUp(player) && !BillyBarrier.isUp(player), "a Chaos Shield (not Agatha's, not a barrier)");
            float health = player.getHealth();
            Pig attacker = pig(helper, 11.5, 10.5);
            check(!player.hurt(helper.getLevel().damageSources().mobAttack(attacker), 4.0F) && player.getHealth() == health,
                    "a blow is stopped");
            assertNear(data.shieldStrength(), EnhancedConfig.CHAOS_SHIELD_STRENGTH.get() - 4.0, 1.0E-3, "and taken off its strength");
            press(player, 3, false, false);
            check(!data.chaosShieldActive(), "V released: it drops");
            assertNear(left(helper, player, 3), EnhancedConfig.CHAOS_SHIELD_COOLDOWN.get(), 0.0, "its recharge is on V");
            check(ready(helper, player, 4), "not on B, her home key");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "custom_witch_hex", timeoutTicks = 40)
    public static void theHexOfACustomHeroRisesIsControlledAndDissolves(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        CustomHero witch = def().pick("wanda.hex", 5).key(1, "wanda.hex").build();
        check("wanda.hex#alt".equals(witch.binding(1, true)), "its dissolving sits on Sneak + R by itself: " + witch.binding(1, true));
        ServerPlayer player = CustomKitSupport.hero(f, 10.5, 10.5, witch);
        tap(player, 1, false);
        Optional<Hex> hex = HexManager.ownedBy(helper.getLevel(), player.getUUID());
        check(hex.isPresent(), "R raises his Hex");
        HexManager.handleAction(player, HexAction.SET_RADIUS, Optional.empty(), 12);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).orElseThrow().radius() == 12, "he controls it (the owner's screen)");
        tap(player, 1, true);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isEmpty(), "Sneak + R dissolves it");

        tap(player, 1, false);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isPresent(), "raised again");
        CustomKitSupport.become(player, def().pick("agatha.runes", 5).key(1, "agatha.runes").build());
        HexManager.tickLevel(helper.getLevel());
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isEmpty(), "made into a hero without the Hex: it dissolves");

        CustomKitSupport.become(player, witch);
        tap(player, 1, false);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isPresent(), "raised once more");
        Powers.setPower(player, Power.NONE);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isEmpty(), "losing the power dissolves it");

        CustomKitSupport.become(player, witch);
        tap(player, 1, false);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isPresent(), "and once more");
        player.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        check(HexManager.ownedBy(helper.getLevel(), player.getUUID()).isEmpty(), "his death dissolves it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Billy

    @EnhancedTest(template = ARENA, batch = "custom_witch_beam", timeoutTicks = 60)
    public static void theHexBeamOnNBurnsUntilLetGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, true);
        ServerPlayer player = hero(f, 3.5, 10.5, def().pick("billy.hex_beam", 5).key(5, "billy.hex_beam"));
        PowerData data = Powers.get(player);
        Pig pig = pig(helper, 10.5, 10.5);
        lookAt(player, pig);
        press(player, 5, false, true);
        check(data.is(Live.BEAMING) && ContinuousBeam.isFiring(player), "N held: the Hex Beam burns");
        tickN(player, 20);
        check(pig.getHealth() < pig.getMaxHealth(), "it hurts what the crosshair is on");
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.HEX_BEAM) && last.getEntity() == player, "with the Hex Beam's damage: " + last);
        press(player, 5, false, false);
        check(!data.is(Live.BEAMING) && !ContinuousBeam.isFiring(player), "N released: it stops");
        helper.succeed();
    }

    /** With {@code billy.beam} off the row is the Hex Bolt: it strikes, and its short recharge is on the custom key. */
    @EnhancedTest(template = ARENA, batch = "custom_witch_bolt")
    public static void theHexBoltOnNStrikesAndRechargesOnN(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, false);
        ServerPlayer player = hero(f, 3.5, 10.5, def().pick("billy.hex_beam", 5).key(5, "billy.hex_beam"));
        Pig pig = pig(helper, 10.5, 10.5);
        lookAt(player, pig);
        tap(player, 5, false);
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.HEX_BOLT) && last.getEntity() == player, "N: a Hex Bolt strikes: " + last);
        assertNear(left(helper, player, 5), BillyAbilities.HEX_BOLT_COOLDOWN, 0.0, "its recharge is on N");
        check(ready(helper, player, 1), "not on R, Billy's home key");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void freezeOnGSpellbindsAndRechargesOnG(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 4.5, 10.5, def().pick("billy.freeze", 5).key(2, "billy.freeze"));
        look(player, -90.0F, 0.0F);
        Zombie ahead = husk(helper, 10.5, 10.5);
        Zombie behind = husk(helper, 1.5, 10.5);
        tap(player, 2, false);
        check(ahead.hasEffect(ModEffects.SPELLBOUND), "the one in front of him is spellbound");
        check(!behind.hasEffect(ModEffects.SPELLBOUND), "the one behind him is not");
        assertNear(left(helper, player, 2), BillyAbilities.FREEZE_COOLDOWN, 0.0, "the recharge is on G");
        check(ready(helper, player, 4), "not on B, Billy's home key");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theHexBarrierIsHeldOnSneakGAndLetGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, def().pick("billy.freeze", 5).key(2, "billy.freeze"));
        PowerData data = Powers.get(player);
        check("billy.freeze#alt".equals(CustomKitSupport.kit(player).hero().binding(2, true)), "the barrier sits on Sneak + G");
        press(player, 2, true, true);
        player.setShiftKeyDown(false);
        check(data.chaosShieldActive() && BillyBarrier.owns(data), "Sneak + G: the barrier forms");
        helper.runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(player), "complete after its wind-up");
            check(!WandaAbilities.hasShieldUp(player) && !AgathaShield.isUp(player), "a barrier, not a dome");
            press(player, 2, false, false);
            check(!data.chaosShieldActive() && !BillyBarrier.isUp(player), "G let go (without Sneak): the barrier comes down");
            assertNear(left(helper, player, 2), EnhancedConfig.BILLY_BARRIER_COOLDOWN.get(), 0.0, "its recharge is on G");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void freezeWithoutItsVariantRaisesNoBarrier(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 4.5, 10.5, def().pick("billy.freeze", 5, HeroAbility.MAIN).key(2, "billy.freeze"));
        PowerData data = Powers.get(player);
        check(CustomKitSupport.kit(player).hero().binding(2, true).isEmpty(), "Sneak + G is free");
        check(!BillyBarrier.owns(data), "no barrier is his");
        look(player, -90.0F, 0.0F);
        Zombie ahead = husk(helper, 10.5, 10.5);
        tap(player, 2, true);
        check(!data.chaosShieldActive(), "Sneak + G raises no barrier");
        check(ahead.hasEffect(ModEffects.SPELLBOUND), "it freezes, as G does");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theNoiseAloneHearsThoughtsAndMakesASorcerer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 4.5, 10.5, def().pick("billy.noise", 5).key(1, "billy.noise"));
        ServerPlayer other = hero(f, 16.5, 10.5, def().pick("agatha.drain", 5).key(1, "agatha.drain"));
        PowerData data = Powers.get(player);
        check(!Thoughts.listens(player, data), "off at first");
        tap(player, 1, false);
        check(data.toggle(Toggle.THE_NOISE) && Thoughts.listens(player, data), "R: The Noise is on and he hears thoughts");
        check(Thoughts.hearsAs(data) == Power.BILLY, "with Billy's range");
        pig(helper, 8.5, 10.5);
        check(!Thoughts.around(player, Thoughts.hearsAs(data)).isEmpty(), "the pig's thoughts reach him");
        check(Sorcery.isSorcerer(player), "and he is a sorcerer (the Hex Runes hold him)");
        check(!Thoughts.listens(other, Powers.get(other)), "a custom hero without it hears nothing");
        tap(player, 1, false);
        check(!Thoughts.listens(player, data), "R again: off");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void buildMagicIsBillysForACustomHeroWithIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer builder = hero(f, 4.5, 10.5, def().pick("billy.build_magic", 5).pick("billy.noise", 5).key(1, "billy.noise"));
        ServerPlayer plain = hero(f, 16.5, 10.5, def().pick("billy.noise", 5).key(1, "billy.noise"));
        Limits limits = Limits.of(builder);
        check(limits != null && limits.billy() && limits.instantMax() == 0, "Billy's rules (the meter, no Instant): " + limits);
        check(Limits.of(plain) == null, "without the module, no Build Magic");
        BuildMagic.setMode(builder, true);
        BuildMagic.setMode(plain, true);
        BuildSession session = BuildSession.existing(builder.getUUID());
        check(session != null && session.mode(), "Y: build mode on");
        BuildSession none = BuildSession.existing(plain.getUUID());
        check(none == null || !none.mode(), "and not for the other");
        CustomKitSupport.become(builder, def().pick("billy.noise", 5).key(1, "billy.noise").build());
        BuildSession after = BuildSession.existing(builder.getUUID());
        check(after == null || !after.mode(), "made into a hero without it: build mode ends");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Agatha

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDrainOnRDrinksALifeAndRechargesOnR(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 4.5, def().pick("agatha.drain", 5).key(1, "agatha.drain"));
        Zombie zombie = husk(helper, 10.5, 12.5);
        lookAt(player, zombie);
        player.setHealth(10.0F);
        tap(player, 1, false);
        assertNear(zombie.getMaxHealth() - zombie.getHealth(), AgathaConfig.DRAIN_DAMAGE.get(), 0.6, "its life is drawn out");
        assertNear(player.getHealth(), 10.0 + AgathaConfig.DRAIN_HEAL.get(), 1.0E-4, "into him");
        assertNear(left(helper, player, 1), AgathaConfig.DRAIN_COOLDOWN.get(), 0.0, "the recharge is on R");
        check(ready(helper, player, AgathaAbilities.DRAIN_SLOT), "not on V, her home key");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "custom_witch_pvp")
    public static void theDrainTakesAHerosPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = hero(f, 10.5, 4.5, def().pick("agatha.drain", 5).key(4, "agatha.drain"));
        ServerPlayer billy = f.player(10.5, FLOOR, 12.5, Power.BILLY);
        lookAt(player, billy);
        tap(player, 4, false);
        check(billy.hasEffect(ModEffects.DRAINED), "B: the sorcerer's power is drawn out");
        assertNear(left(helper, player, 4), AgathaConfig.DRAIN_COOLDOWN.get(), 0.0, "the recharge is on B");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void thePurpleBeamOnVAndTheHexMarkOnSneakV(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 4.5, def().pick("agatha.purple_beam", 5).key(3, "agatha.purple_beam"));
        Pig pig = pig(helper, 10.5, 14.5);
        lookAt(player, pig);
        press(player, 3, false, true);
        check(ContinuousBeam.isFiring(player) && AgathaAbilities.firing(player), "V held: her Purple Beam burns");
        tickN(player, 1);
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.DARKHOLD_BEAM), "with the Darkhold's damage: " + last);
        press(player, 3, false, false);
        check(!ContinuousBeam.isFiring(player) && !AgathaAbilities.firing(player), "V released: it ends");
        Zombie zombie = husk(helper, 10.5, 12.5);
        lookAt(player, zombie);
        tap(player, 3, true);
        check(zombie.hasEffect(MobEffects.GLOWING) && DarkholdBook.markerOf(zombie, now(helper)) != null, "Sneak + V: the hex mark");
        check(Powers.get(player).is(Live.CASTING) && !ContinuousBeam.isFiring(player), "his hand rises (and no beam)");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRunesOnGAndTheirRechargeScales(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer normal = hero(f, 4.5, 10.5, def().pick("agatha.runes", 5).key(2, "agatha.runes"));
        ServerPlayer strong = hero(f, 16.5, 10.5, def().pick("agatha.runes", 10).key(2, "agatha.runes"));
        look(normal, 0.0F, 90.0F);
        look(strong, 0.0F, 90.0F);
        tap(normal, 2, false);
        tap(strong, 2, false);
        check(AgathaAbilities.trapOf(normal) != null && AgathaAbilities.trapOf(strong) != null, "G lays the circle of runes");
        int cooldown = AgathaConfig.RUNE_COOLDOWN.get();
        assertNear(left(helper, normal, 2), cooldown, 0.0, "strength 5: the hero's recharge, on G");
        assertNear(left(helper, strong, 2), Math.round(cooldown / 1.5F), 1.0, "strength 10: two thirds of it");
        check(ready(helper, normal, AgathaAbilities.RUNE_SLOT), "nothing on N, her home key");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "custom_witch_agatha_shield", timeoutTicks = 60)
    public static void agathasShieldIsNoChaosShield(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, def().pick("agatha.shield", 5).key(1, "agatha.shield"));
        PowerData data = Powers.get(player);
        press(player, 1, false, true);
        check(data.chaosShieldActive(), "R: her shield forms");
        helper.runAfterDelay(AgathaShield.windUpTicks() + 2, () -> {
            check(AgathaShield.isUp(player), "complete after its wind-up");
            check(!WandaAbilities.hasShieldUp(player) && !BillyBarrier.isUp(player), "not a Chaos Shield, not a barrier");
            float health = player.getHealth();
            check(!player.hurt(helper.getLevel().damageSources().generic(), 10.0F) && player.getHealth() == health, "a blow is stopped");
            assertNear(data.shieldStrength(), AgathaShield.maxStrength() - 10.0F, 1.0E-4, "by Agatha's rules: off its strength");
            press(player, 1, false, false);
            check(!data.chaosShieldActive(), "R let go: down");
            assertNear(left(helper, player, 1), AgathaConfig.SHIELD_COOLDOWN.get(), 0.0, "her recharge, on R");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herBodyOnlyWithItsRowAndByItsStrength(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer without = hero(f, 4.5, 4.5, def().pick("agatha.drain", 5).key(1, "agatha.drain"));
        ServerPlayer with = hero(f, 4.5, 16.5, def().pick("agatha.body", 5).pick("agatha.drain", 5).key(1, "agatha.drain"));
        ServerPlayer strong = hero(f, 16.5, 16.5, def().pick("agatha.body", 10).pick("agatha.drain", 5).key(1, "agatha.drain"));
        for (ServerPlayer p : List.of(without, with, strong)) {
            tickN(p, 1);
        }
        double bonus = AgathaConfig.BODY_HEALTH_BONUS.get();
        assertNear(without.getMaxHealth(), 20.0, 1.0E-4, "her drain alone gives no body");
        assertNear(with.getMaxHealth(), 20.0 + bonus, 1.0E-4, "her body at strength 5: her health");
        assertNear(strong.getMaxHealth(), 20.0 + bonus * 1.5, 1.0E-3, "at strength 10: half as much more");
        Pig caster = pig(helper, 6.5, 16.5);
        with.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.HEX_BOLT, caster, caster), 1.0F);
        check(caster.hasEffect(MobEffects.POISON), "magic used on him poisons whoever used it");
        Pig other = pig(helper, 6.5, 4.5);
        without.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.HEX_BOLT, other, other), 1.0F);
        check(!other.hasEffect(MobEffects.POISON), "not without her body");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shared fields, death, names

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void billysUpkeepLeavesAgathasBeamAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 4.5, def().pick("billy.noise", 5).key(1, "billy.noise")
                .pick("agatha.purple_beam", 5).key(2, "agatha.purple_beam"));
        Pig pig = pig(helper, 10.5, 14.5);
        lookAt(player, pig);
        press(player, 2, false, true);
        tickN(player, 12);
        check(ContinuousBeam.isFiring(player) && AgathaAbilities.firing(player), "Billy's tick does not take the beam over");
        DamageSource last = pig.getLastDamageSource();
        check(last != null && last.is(ModDamageTypes.DARKHOLD_BEAM), "it is still Agatha's beam that burns: " + last);
        press(player, 2, false, false);
        check(!ContinuousBeam.isFiring(player), "and hers to stop");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void deathPutsEverythingOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = hero(f, 10.5, 10.5, def().pick("wanda.chaos_beam", 5).key(1, "wanda.chaos_beam")
                .pick("agatha.runes", 5).key(2, "agatha.runes").pick("billy.freeze", 5).key(3, "billy.freeze"));
        PowerData data = Powers.get(player);
        look(player, 0.0F, 90.0F);
        tap(player, 2, false);
        press(player, 1, false, true);
        press(player, 3, true, true);
        check(data.beamActive() && AgathaAbilities.trapOf(player) != null && data.chaosShieldActive() && BillyBarrier.owns(data),
                "beam, runes and barrier are all up");
        player.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        check(player.isDeadOrDying(), "he died");
        check(!data.beamActive(), "the beam goes out");
        check(AgathaAbilities.trapOf(player) == null, "the runes fade");
        check(!data.chaosShieldActive(), "the barrier falls");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyWitchRowHasItsNamesInBothLanguages(GameTestHelper helper) throws IOException {
        List<String> missing = new ArrayList<>();
        for (String name : List.of("en_us", "he_il")) {
            Path file = Platform.get().modResource("assets/" + Enhanced.MODID + "/lang/" + name + ".json")
                    .orElseThrow();
            JsonObject lang = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (HeroAbility row : HeroAbility.values()) {
                if (row.hero != Power.SCARLET_WITCH && row.hero != Power.BILLY && row.hero != Power.AGATHA) {
                    continue;
                }
                List<String> keys = new ArrayList<>(List.of(row.nameKey(), row.descKey()));
                if (row.hasAlt()) {
                    keys.add(row.altKey());
                }
                for (String key : keys) {
                    if (!lang.has(key)) {
                        missing.add(name + ": " + key);
                    }
                }
            }
        }
        check(missing.isEmpty(), "every witch row is named: " + missing);
        helper.succeed();
    }

    private CustomWitchTests() {
    }
}
