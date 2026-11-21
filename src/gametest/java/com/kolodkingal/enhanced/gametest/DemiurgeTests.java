package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.DemiurgeConfig;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.ultimate.DemiurgeGifts;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Billy the Demiurge (3.18.1, {@link Demiurge}): only with Ultimate (off by default; refused without; granted works; taken
 * back he is plain Billy at once); forty health, a netherite set's protection and faster healing; his powers hit harder and
 * are drawn in his blue; his barrier wraps his whole body; Magic Types step through the witches' and sorcerers' magic and
 * his keys become theirs; and his gift of magic ({@link DemiurgeGifts}) makes a player with no hero a caster of magic only,
 * refuses a player who has a hero, and is taken back.
 */
public final class DemiurgeTests {
    private static final String BATCH = "demiurge";

    private static ServerPlayer billy(TestKit.Fixture f, boolean ultimate) {
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, Power.BILLY);
        if (ultimate) {
            Ultimate.grant(player, true);
        }
        PowerTicker.tick(player);
        return player;
    }

    private static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    private static void sneakN(ServerPlayer player) {
        AbilityHandler.onKey(player, 5, true, true);
        AbilityHandler.onKey(player, 5, false, true);
    }

    private static ServerPlayer demiurge(TestKit.Fixture f) {
        ServerPlayer player = billy(f, true);
        sneakN(player);
        PowerTicker.tick(player);
        check(Demiurge.is(data(player)), "granted Ultimate, Sneak + N makes Billy the Demiurge");
        return player;
    }

    private static int index(String id, String option) {
        for (int i = 0; i < DemiurgeGifts.CATALOG.size(); i++) {
            DemiurgeGifts.Gift g = DemiurgeGifts.CATALOG.get(i);
            if (g.id().equals(id) && (option == null || g.option().equals(option))) {
                return i;
            }
        }
        throw new AssertionError("no gift " + id);
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyWithUltimateWhichIsOffByDefault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(!UltimateConfig.ENABLED.getDefault() && !Ultimate.on(), "the Ultimate switch is off by default, and here");
        ServerPlayer player = billy(f, false);
        sneakN(player);
        PowerTicker.tick(player);
        check(!Demiurge.is(data(player)) && !Demiurge.allowed(player), "without Ultimate Sneak + N refuses");
        check(!Demiurge.cycle(player, data(player), 1), "and there are no Magic Types for plain Billy");
        Ultimate.grant(player, true);
        sneakN(player);
        PowerTicker.tick(player);
        check(Demiurge.is(data(player)) && data(player).is(Live.BILLY_DEMIURGE), "granted, he becomes Billy the Demiurge");
        Ultimate.grant(player, false);
        PowerTicker.tick(player);
        check(!Demiurge.is(data(player)), "taken back, he is plain Billy again at once");
        assertNear(player.getMaxHealth(), 20.0, 1.0E-6, "with a player's own health");
        // the switch for everyone works the same way
        f.config(UltimateConfig.ENABLED, true);
        sneakN(player);
        PowerTicker.tick(player);
        check(Demiurge.is(data(player)), "with the switch on for everyone, he may");
        f.config(UltimateConfig.ENABLED, false);
        PowerTicker.tick(player);
        check(!Demiurge.is(data(player)), "and the switch turned off turns him back");
        // Sneak + N again lets him go
        ServerPlayer again = demiurge(f);
        sneakN(again);
        check(!Demiurge.is(data(again)), "Sneak + N lets the Demiurge go");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void fortyHealthNetheriteArmourAndFasterHealing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = demiurge(f);
        assertNear(player.getMaxHealth(), 40.0, 1.0E-6, "twenty hearts");
        check(player.getAttributeValue(Attributes.ARMOR) >= 20.0, "a netherite set's armour: " + player.getAttributeValue(Attributes.ARMOR));
        check(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12.0, "and its toughness");
        check(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.4 - 1.0E-6, "and its knockback resistance");
        f.config(DemiurgeConfig.REGEN_INTERVAL_TICKS, 1);
        player.setHealth(10.0F);
        PowerTicker.tick(player);
        check(player.getHealth() >= 12.0F - 1.0E-4F, "he heals on top of a player's own: " + player.getHealth());
        check(player.mayFly(), "he flies");
        assertNear(player.getAbilities().getFlyingSpeed(), DemiurgeConfig.FLIGHT_SPEED.get(), 1.0E-4, "faster than Billy");
        sneakN(player);
        PowerTicker.tick(player);
        assertNear(player.getMaxHealth(), 20.0, 1.0E-6, "plain Billy again: his own health");
        check(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) < 12.0, "and no netherite");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPowersHitHarderAndShineInHisBlue(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        Ultimate.grant(plain, true);
        PowerTicker.tick(plain);
        ServerPlayer player = demiurge(f);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 12.5));
        DamageSource magic = player.damageSources().indirectMagic(player, player);
        DamageSource fist = player.damageSources().playerAttack(player);
        double factor = DemiurgeConfig.DAMAGE_FACTOR.get();
        check(factor > 1.0, "his powers are stronger by default");
        assertNear(Demiurge.outgoing(zombie, magic, 6.0F), 6.0 * factor, 1.0E-4, "his magic hits " + factor + " times as hard");
        assertNear(Demiurge.outgoing(zombie, fist, 6.0F), 6.0, 1.0E-4, "his bare fist as it was");
        assertNear(Demiurge.outgoing(zombie, plain.damageSources().indirectMagic(plain, plain), 6.0F), 6.0, 1.0E-4,
                "plain Billy's as it was");
        float before = zombie.getHealth();
        zombie.hurt(magic, 4.0F);
        check(before - zombie.getHealth() >= 4.0F * factor - 0.5F, "a real hurt of his comes to more: " + (before - zombie.getHealth()));
        check(AbilityColors.chosen(player, HeroAbility.BILLY_HEX_BEAM) == Demiurge.BLUE, "his beam in the Demiurge's blue");
        check(AbilityColors.chosen(plain, HeroAbility.BILLY_HEX_BEAM) == -1, "plain Billy's in his own");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisBarrierWrapsHisWholeBody(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        PowerTicker.tick(plain);
        ServerPlayer player = demiurge(f);
        TestKit.look(player, 0.0F, 0.0F);
        TestKit.look(plain, 0.0F, 0.0F);
        Vec3 behind = BillyBarrier.body(player).subtract(player.getLookAngle().scale(2.0));
        Vec3 behindPlain = BillyBarrier.body(plain).subtract(plain.getLookAngle().scale(2.0));
        check(BillyBarrier.covers(player, behind), "the Demiurge's barrier covers him from behind");
        check(!BillyBarrier.covers(plain, behindPlain), "Billy's plate does not");
        check(BillyBarrier.maxHealth(player) > BillyBarrier.maxHealth(plain), "and it is stronger");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void magicTypesStepThroughTheSorcerersAndTheKeysAreTheirs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = demiurge(f);
        PowerData data = data(player);
        check(Demiurge.keysOf(data) == Power.BILLY && data.borrowed() == Power.NONE, "his own magic first");
        List<Power> seen = new ArrayList<>();
        for (int i = 0; i < Demiurge.MAGIC_TYPES.size(); i++) {
            check(Demiurge.cycle(player, data, 1), "H as the Demiurge steps on");
            seen.add(Demiurge.keysOf(data(player)));
        }
        check(seen.equals(List.of(Power.SCARLET_WITCH, Power.DOCTOR_STRANGE, Power.AGATHA, Power.LOKI, Power.BILLY)),
                "the Scarlet Witch, Doctor Strange, Agatha, Loki, then his own: " + seen);
        Demiurge.cycle(player, data(player), -1);
        check(Demiurge.keysOf(data(player)) == Power.LOKI, "Sneak + H steps back");
        Demiurge.setType(player, data(player), 1);
        data = data(player);
        check(data.borrowed() == Power.SCARLET_WITCH && data.has(HeroAbility.WANDA_CHAOS_SHIELD)
                && !data.has(HeroAbility.BILLY_HEX_BEAM), "with Wanda's magic he has her abilities in place of his");
        check(Demiurge.is(data) && Powers.power(player) == Power.BILLY, "and is still Billy the Demiurge");
        AbilityHandler.onKey(player, 4, true, false);
        PowerTicker.tick(player);
        check(data(player).chaosShieldActive() && !BillyBarrier.owns(data(player)), "B raises the Chaos Shield, not his barrier");
        AbilityHandler.onKey(player, 4, false, false);
        PowerTicker.tick(player);
        check(!data(player).chaosShieldActive(), "and it comes down");
        Demiurge.setType(player, data(player), 2);
        check(data(player).has(HeroAbility.STRANGE_WHIP) && data(player).acts(Power.DOCTOR_STRANGE), "Doctor Strange's magic");
        // Sneak + N is the magic type's while it is another's: he stays the Demiurge
        sneakN(player);
        PowerTicker.tick(player);
        check(Demiurge.is(data(player)), "with another's magic Sneak + N is that hero's, he stays the Demiurge");
        // without Ultimate Magic Types refuse, and he is plain Billy again
        Ultimate.grant(player, false);
        Demiurge.cycle(player, data(player), 1);
        check(!Demiurge.is(data(player)) && data(player).borrowed() == Power.NONE && data(player).has(HeroAbility.BILLY_HEX_BEAM),
                "Ultimate gone: the Magic Types refuse, he is plain Billy with his own magic");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGiftMakesAPlayerWithNoHeroACasterOfMagicOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer giver = demiurge(f);
        ServerPlayer target = f.player(14.5, FLOOR, 14.5);
        PowerTicker.tick(target);
        check(Powers.power(target) == Power.NONE, "the target has no hero");
        // the catalogue is magic only
        for (DemiurgeGifts.Gift g : DemiurgeGifts.CATALOG) {
            check(g.witch() != null || g.entry() != null, g.id() + " is a witch of a book or in the Hero Creator's catalogue");
            Power hero = g.hero();
            check(g.witch() != null || (hero == null ? List.of("magic", "earth", "nature", "flight").contains(g.id())
                    : DemiurgeGifts.SORCERERS.contains(hero)),
                    g.id() + " is magic");
        }
        check(DemiurgeGifts.CATALOG.size() >= 30, "every witch's and sorcerer's ability: " + DemiurgeGifts.CATALOG.size());
        List<Integer> picks = List.of(index(PowerModule.MAGIC.id(), "bolts"), index("wanda.chaos_beam", null),
                index("strange.whip", null), index("agatha.shield", null), index("loki.daggers", null), index("billy.noise", null));
        check(DemiurgeGifts.parse(DemiurgeGifts.format(picks)).equals(picks), "the screen's picks travel as text");
        String refused = DemiurgeGifts.give(giver, target, picks);
        check(refused == null, "given: " + refused);
        CustomHero hero = CustomHeroes.hero(target);
        check(Powers.power(target) == Power.CUSTOM && DemiurgeGifts.gifted(target), "the target is a custom hero of the gift");
        check(CustomHero.LOOK_OWN.equals(hero.look()), "in his own skin");
        // (3.18.1) a witch of the gift starts at level 1 of the witch levels: some of the gift now, all of it at level 5
        check(com.kolodkingal.enhanced.spellbook.WitchLevels.level(target) == 1 && hero.powers().size() < picks.size(),
                "she starts at level 1 with part of the gift: " + hero.powers().size());
        com.kolodkingal.enhanced.spellbook.WitchLevels.setLevel(target, "gift", 5);
        hero = CustomHeroes.hero(target);
        check(DemiurgeGifts.gifted(target), "still a witch of the gift at level 5");
        check(hero.powers().size() == picks.size(), "at level 5, every power given: " + hero.powers().size());
        PowerTicker.tick(target);
        check(data(target).has(HeroAbility.WANDA_CHAOS_BEAM) && data(target).has(HeroAbility.STRANGE_WHIP), "the powers work for him");
        // a player who has a hero is refused
        ServerPlayer thor = f.player(16.5, FLOOR, 8.5, Power.THOR);
        check("message.enhanced.demiurge.gift.has_hero".equals(DemiurgeGifts.give(giver, thor, picks)) && Powers.power(thor) == Power.THOR,
                "a player who has a hero keeps it");
        check("message.enhanced.demiurge.gift.has_hero".equals(DemiurgeGifts.give(giver, target, picks)), "and so does a gifted one");
        // only the Demiurge, and only with Ultimate
        ServerPlayer other = f.player(6.5, FLOOR, 14.5);
        ServerPlayer plain = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        Ultimate.grant(plain, true);
        check("message.enhanced.demiurge.gift.not_demiurge".equals(DemiurgeGifts.give(plain, other, picks)), "plain Billy cannot give");
        Ultimate.grant(giver, false);
        check("message.enhanced.demiurge.gift.no_ultimate".equals(DemiurgeGifts.give(giver, other, picks)), "nor the Demiurge without Ultimate");
        check(Powers.power(other) == Power.NONE, "the other stays as he was");
        // taken back: no power, and his own custom hero back
        Ultimate.grant(giver, true);
        Demiurge.enter(giver, data(giver));
        check(DemiurgeGifts.takeBack(giver, target) && Powers.power(target) == Power.NONE, "the Demiurge takes it back");
        check(!DemiurgeGifts.gifted(target) && ModAttachments.CUSTOM_HERO.get(target).isEmpty(), "and the player is himself again");
        // an operator's command path needs Ultimate too
        check(DemiurgeGifts.give(null, other, picks) != null, "without Ultimate on, an operator's gift is refused too");
        f.config(UltimateConfig.ENABLED, true);
        check(DemiurgeGifts.give(null, other, picks) == null && DemiurgeGifts.gifted(other), "with it on, an operator may give it");
        helper.succeed();
    }

    /** (3.18.1) As the Demiurge his Build Magic has no limit (plain Billy keeps his rank's), and his powers hit 8 times as hard. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisBuildMagicHasNoLimitAndHisPowersHitFourTimesHarder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = billy(f, false);
        com.kolodkingal.enhanced.build.Limits limits = com.kolodkingal.enhanced.build.Limits.of(plain);
        check(limits != null && limits.limited(), "plain Billy's Build Magic has his rank's limits");
        ServerPlayer player = demiurge(f);
        limits = com.kolodkingal.enhanced.build.Limits.of(player);
        check(limits != null && limits.billy() && !limits.limited(), "the Demiurge's has none");
        assertNear(com.kolodkingal.enhanced.config.DemiurgeConfig.DAMAGE_FACTOR.getDefault(), 8.0, 1.0E-9,
                "his powers hit 8 times as hard by default (four times the 2 of before)");
        assertNear(com.kolodkingal.enhanced.config.DemiurgeConfig.BARRIER_HEALTH_FACTOR.getDefault(), 8.0, 1.0E-9,
                "his barrier has 8 times its health by default");
        helper.succeed();
    }

    /** (3.18.1) He may give a whole witch of a book (at level 1), Odin's and Hela's magic too; and he bears all six stones. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void heGivesAWitchOrAnyMagicAndBearsEveryStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = demiurge(f);
        int witch = index(DemiurgeGifts.WITCH + "green", null);
        check(DemiurgeGifts.CATALOG.stream().anyMatch(g -> g.hero() == Power.ODIN)
                && DemiurgeGifts.CATALOG.stream().anyMatch(g -> g.hero() == Power.HELA), "Odin's and Hela's magic may be given");
        check(DemiurgeGifts.CATALOG.stream().anyMatch(g -> "nature".equals(g.id()) && "roots".equals(g.option())), "and Root Grasp");
        ServerPlayer other = f.player(12.5, FLOOR, 10.5, Power.NONE);
        check(DemiurgeGifts.give(player, other, List.of(witch)) == null, "he gives the Green Witch");
        check(com.kolodkingal.enhanced.spellbook.Spellbook.of(other) == com.kolodkingal.enhanced.spellbook.Spellbook.GREEN,
                "the player is the Green Witch");
        check(DemiurgeGifts.takeBack(player, other) && Powers.power(other) == Power.NONE, "and he takes her magic back");
        for (com.kolodkingal.enhanced.thanos.StoneKind kind : com.kolodkingal.enhanced.thanos.StoneKind.values()) {
            check(com.kolodkingal.enhanced.stones.StoneRules.mayUse(player, kind), "the Demiurge uses the " + kind + " stone");
        }
        check(com.kolodkingal.enhanced.stones.StoneRules.mayWield(player), "and wields the Gauntlet");
        ServerPlayer plain = billy(f, false);
        check(!com.kolodkingal.enhanced.stones.StoneRules.mayUse(plain, com.kolodkingal.enhanced.thanos.StoneKind.POWER),
                "plain Billy does not use the Power Stone");
        helper.succeed();
    }

    /** (3.18.1) With the Scarlet Witch's magic, the Demiurge's Chaos Shield forms and stops a blow; and slowed time never slows him. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hisChaosShieldWorksWithHerMagic(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = demiurge(f);
        Demiurge.setType(player, data(player), 1);
        AbilityHandler.onKey(player, 4, true, false);
        check(data(player).chaosShieldActive(), "B raises her Chaos Shield");
        int wait = com.kolodkingal.enhanced.EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 4;
        for (int t = 1; t <= wait; t++) {
            int at = t;
            helper.runAfterDelay(t, () -> {
                PowerTicker.tick(player);
                check(data(player).chaosShieldActive(), "the shield is still held at tick " + at);
            });
        }
        helper.runAfterDelay(wait + 1, () -> {
            check(com.kolodkingal.enhanced.ability.WandaAbilities.hasShieldUp(player), "the Chaos Shield is complete");
            net.minecraft.world.entity.monster.Zombie zombie = TestKit.spawnWithNoFreeWill(helper,
                    net.minecraft.world.entity.EntityType.ZOMBIE, new net.minecraft.world.phys.Vec3(11.5, FLOOR, 10.5));
            float before = player.getHealth();
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().mobAttack(zombie), 6.0F);
            check(player.getHealth() >= before - 1.0E-4F, "the shield stops a zombie's blow: " + before + " -> " + player.getHealth());
            check(com.kolodkingal.enhanced.ability.SlowMotion.playerFactor(player) == 1, "slowed time never slows the Demiurge");
            helper.succeed();
        });
    }
}
