package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Ultimate Wanda, the Sorceress Supreme (3.19, {@link UltimateWanda}): only with Ultimate; forty health and netherite; her own
 * menu on H and her hair on Sneak + H; R takes the universe she stands in, B lights the runes that take the powers of everyone
 * under them and make her stronger; no recharge; nothing freezes her and the Hex Runes don't hold her; she comes back a second
 * after she dies; V takes a body with no time limit, and her own body is guarded meanwhile.
 */
public final class UltimateWandaTests {
    private static final String BATCH = "ultimate_wanda";

    private static ServerPlayer wanda(TestKit.Fixture f, double x, double z, boolean ultimate) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.SCARLET_WITCH);
        // (the tests run side by side: each lets go only of what its own Wanda holds)
        f.onClose(() -> UltimateWanda.onLogout(player));
        if (ultimate) {
            Ultimate.grant(player, true);
        }
        PowerTicker.tick(player);
        return player;
    }

    private static void press(ServerPlayer player, int key, boolean sneak) {
        AbilityHandler.onKey(player, key, true, sneak);
        AbilityHandler.onKey(player, key, false, sneak);
    }

    private static ServerPlayer ultimate(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = wanda(f, x, z, true);
        press(player, 2, true);
        PowerTicker.tick(player);
        check(UltimateWanda.is(Powers.get(player)), "granted Ultimate, Sneak + G makes her Ultimate Wanda");
        return player;
    }

    private static void ultimateMenu(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (!UltimateWanda.ultimateMenu(data)) {
            UltimateWanda.onLookKey(player, data, 1);
        }
        check(UltimateWanda.ultimateMenu(data), "H opens her own menu");
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyWithUltimateAndHerBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = wanda(f, 10.5, 10.5, false);
        press(plain, 2, true);
        PowerTicker.tick(plain);
        check(!UltimateWanda.is(Powers.get(plain)), "without Ultimate Sneak + G refuses");
        Ultimate.grant(plain, true);
        press(plain, 2, true);
        PowerTicker.tick(plain);
        PowerData data = Powers.get(plain);
        check(UltimateWanda.is(data) && data.is(Live.WANDA_ULTIMATE), "granted, she becomes Ultimate Wanda");
        assertNear(plain.getMaxHealth(), 40.0, 1.0E-6, "twenty hearts");
        check(plain.getAttributeValue(Attributes.ARMOR) >= 20.0, "a netherite set's armour");
        check(plain.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12.0, "and its toughness");
        assertNear(UltimateWanda.flightSpeed(Power.SCARLET_WITCH), Power.SCARLET_WITCH.flightSpeed() * 1.6F, 1.0E-5, "she flies faster");
        Ultimate.grant(plain, false);
        PowerTicker.tick(plain);
        check(!UltimateWanda.is(Powers.get(plain)), "taken back, she is the Scarlet Witch again at once");
        assertNear(plain.getMaxHealth(), 20.0, 1.0E-6, "with a player's own health");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herMenuHerHairAndNoRecharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        PowerData data = Powers.get(player);
        check(UltimateWanda.ultimateMenu(data), "she starts in her own menu");
        UltimateWanda.onLookKey(player, data, 1);
        check(!UltimateWanda.ultimateMenu(data), "H: the Scarlet Witch's");
        UltimateWanda.onLookKey(player, data, 1);
        check(UltimateWanda.ultimateMenu(data), "H again: her own");
        check(!UltimateWanda.comicsHair(data), "her hair is the films' ginger");
        UltimateWanda.onLookKey(player, data, -1);
        check(UltimateWanda.comicsHair(data) && data.toggle(Toggle.WANDA_COMICS_HAIR), "Sneak + H: the comics' dark hair");
        UltimateWanda.onLookKey(player, data, -1);
        check(!UltimateWanda.comicsHair(data), "and back");
        long now = helper.getLevel().getGameTime();
        data.startCooldown(1, now, 1000);
        check(data.isReady(1, now) && data.isReady(4, now), "infinite chaos magic: nothing ever recharges");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herUniverseHerRunesAndWhatTheyTake(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 6.5, 6.5);
        ServerPlayer billy = f.player(12.5, FLOOR, 12.5, Power.BILLY);
        PowerTicker.tick(billy);
        ultimateMenu(player);
        press(player, 4, false);
        check(UltimateWanda.held(player.getUUID()) == 0 && !UltimateWanda.underRunes(billy), "no universe, no runes");
        int universe = UltimateWanda.universeOf(player);
        press(player, 1, false);
        check(player.getUUID().equals(UltimateWanda.holder(universe)) && UltimateWanda.held(player.getUUID()) == 1,
                "R: the universe she stands in is hers");
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(3.5, FLOOR, 3.5));
        float before = UltimateWanda.outgoing(husk, husk.damageSources().indirectMagic(player, player), 2.0F);
        assertNear(before, 16.0, 1.0E-4, "her powers hit eight times as hard");
        press(player, 4, false);
        check(UltimateWanda.underRunes(billy), "B: the runes burn in her universe's sky");
        player.tickCount = 20;
        PowerTicker.tick(player);
        check(billy.hasEffect(ModEffects.DRAINED), "Billy's powers are taken");
        check(UltimateWanda.stolen(player.getUUID()) == 1, "and counted");
        float after = UltimateWanda.outgoing(husk, husk.damageSources().indirectMagic(player, player), 2.0F);
        check(after > before, "each one whose powers she took makes her stronger: " + after);
        check(!player.hasEffect(ModEffects.DRAINED), "her own stay");
        press(player, 4, false);
        check(!UltimateWanda.underRunes(billy), "B again: the runes go out");
        press(player, 1, false);
        check(UltimateWanda.holder(universe) == null, "R again: the universe is let go");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void nothingFreezesHerAndTheHexRunesDontHoldHer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        player.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200));
        player.addEffect(new MobEffectInstance(ModEffects.PETRIFIED, 200));
        player.setTicksFrozen(200);
        PowerTicker.tick(player);
        check(!player.hasEffect(ModEffects.SPELLBOUND) && !player.hasEffect(ModEffects.PETRIFIED), "no spell or stone holds her");
        check(player.getTicksFrozen() == 0, "and no cold");
        check(com.kolodkingal.enhanced.protect.HexRunes.silencing(player) == null, "the Hex Runes don't silence her");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void sheComesBackASecondAfterSheDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        player.hurt(player.damageSources().generic(), 1000.0F);
        check(player.isAlive(), "her death is refused");
        player.hurt(player.damageSources().generic(), 1000.0F);
        check(player.isAlive(), "and nothing hurts her while she comes back");
        helper.runAfterDelay(UltimateWanda.REVIVE_TICKS + 4, () -> {
            PowerTicker.tick(player);
            check(player.isAlive(), "she is alive");
            assertNear(player.getHealth(), player.getMaxHealth(), 1.0E-4, "back in red energy, with all her health");
            assertNear(player.getMaxHealth(), 40.0, 1.0E-6, "still the Ultimate");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sheTakesABodyWithNoTimeLimitAndHerOwnIsGuarded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 3.5, 10.5);
        f.onClose(() -> MindLink.end(player.server, player.getUUID(), MindLink.End.LET_GO));
        ultimateMenu(player);
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(14.5, FLOOR, 10.5));
        TestKit.lookAt(player, husk);
        press(player, 3, false);
        check(MindLink.isHost(husk) && MindLink.isLinked(player), "V: her mind is in the husk, eleven blocks away");
        check(UltimateWanda.shielded(player), "her own body is shielded");
        float health = player.getHealth();
        player.hurt(player.damageSources().generic(), 10.0F);
        assertNear(player.getHealth(), health, 1.0E-4, "nothing hurts it");
        for (int i = 0; i < 5; i++) {
            PowerTicker.tick(player);
        }
        check(MindLink.isLinked(player), "and there is no time limit");
        press(player, 3, false);
        check(!MindLink.isLinked(player) && !MindLink.isHost(husk), "V again: she is back");
        helper.succeed();
    }
}
