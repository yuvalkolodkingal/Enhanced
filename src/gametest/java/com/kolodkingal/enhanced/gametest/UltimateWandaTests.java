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
        check(UltimateWanda.multiverseMenu(data), "H: her Multiverse menu");
        UltimateWanda.onLookKey(player, data, 1);
        check(!UltimateWanda.ultimateMenu(data) && !UltimateWanda.multiverseMenu(data), "H again: the Scarlet Witch's");
        UltimateWanda.onLookKey(player, data, 1);
        check(UltimateWanda.ultimateMenu(data), "and again: her own");
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

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void sneakRChargesAGiantChaosBeamThroughEverything(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 3.5, 10.5);
        PowerData data = Powers.get(player);
        for (int i = 0; i < 3 && (UltimateWanda.ultimateMenu(data) || UltimateWanda.multiverseMenu(data)); i++) {
            UltimateWanda.onLookKey(player, data, 1);
        }
        check(!UltimateWanda.ultimateMenu(data) && !UltimateWanda.multiverseMenu(data), "the Scarlet Witch's menu");
        net.minecraft.core.BlockPos wall = new net.minecraft.core.BlockPos(7, (int) FLOOR + 1, 10);
        helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(wall.above(), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(12.5, FLOOR, 10.5));
        TestKit.lookAt(player, helper.absoluteVec(new Vec3(12.5, FLOOR + 1.62, 10.5)));
        AbilityHandler.onKey(player, 1, true, true);
        check(com.kolodkingal.enhanced.ability.ChaosCannon.charging(player) && data.is(Live.BEAM_CHARGING), "Sneak + R: the charge gathers");
        helper.runAfterDelay(com.kolodkingal.enhanced.ability.ChaosCannon.CHARGE_TICKS + 2, () -> {
            AbilityHandler.onKey(player, 1, false, true);
            check(data.is(Live.BEAM_FIRING), "released: the giant beam fires");
            check(helper.getBlockState(wall).isAir() && helper.getBlockState(wall.above()).isAir(), "it erases the wall in its path");
            check(!husk.isAlive(), "and destroys what is behind it");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herOwnEyeOfAgamottoRevealsBindsAndBanishes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 3.5, 10.5);
        ultimateMenu(player);
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(8.5, FLOOR, 10.5));
        husk.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 600));
        // (3.19.2) all round her, 50 blocks out: even behind her
        TestKit.lookAt(player, player.position().add(-5, 1.6, 0));
        float health = husk.getHealth();
        press(player, 5, false);
        check(com.kolodkingal.enhanced.ability.UltimateEye.LIGHT_RANGE == 50.0 && com.kolodkingal.enhanced.ability.UltimateEye.SIGHT_RANGE == 50.0,
                "its range is 50 blocks");
        check(Powers.get(player).is(Live.WANDA_EYE_OPEN), "the red Eye opens on her chest");
        check(!husk.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "N, the Light of Agamotto: nothing stays hidden");
        check(husk.hasEffect(ModEffects.SPELLBOUND), "her foe is held in its light");
        check(!husk.isAlive() || husk.getHealth() < health, "and the dead burn in it");
        husk.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
        if (husk.isAlive()) {
            press(player, 5, true);
            check(husk.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "Sneak + N, the All-Seeing Eye: she sees them all");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void herShieldLetsNothingIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        ultimateMenu(player);
        AbilityHandler.onKey(player, 4, true, true); // (held: the shield stays while B is down)
        helper.runAfterDelay(WandaAbilitiesAccess.windUp() + 3, () -> {
            check(com.kolodkingal.enhanced.ability.UltimateShield.up(player), "Sneak + B: her shield is up");
            Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(11.5, FLOOR, 10.5));
            com.kolodkingal.enhanced.ability.UltimateShield.tick(player);
            check(husk.distanceTo(player) > com.kolodkingal.enhanced.ability.WandaAbilities.SHIELD_RADIUS * UltimateWanda.SHIELD_SCALE,
                    "nobody stays inside it: " + husk.distanceTo(player));
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(husk), 10.0F);
            player.hurt(player.damageSources().indirectMagic(husk, husk), 10.0F);
            assertNear(player.getHealth(), health, 1.0E-4, "no blow and no power gets in");
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
            com.kolodkingal.enhanced.ability.UltimateShield.tick(player);
            check(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "no poison holds on her");
            check(Powers.get(player).chaosShieldActive(), "and it never breaks");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herHexHasNoSizeLimitUnlessTheServerSetsOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        f.onClose(() -> com.kolodkingal.enhanced.hex.HexManager.dissolveAllOwnedBy(player.server, player.getUUID()));
        com.kolodkingal.enhanced.hex.HexManager.create(player);
        java.util.function.Supplier<Integer> radius = () -> com.kolodkingal.enhanced.hex.HexManager
                .ownedBy(helper.getLevel(), player.getUUID()).orElseThrow().radius();
        com.kolodkingal.enhanced.hex.HexManager.handleAction(player, com.kolodkingal.enhanced.hex.HexAction.SET_RADIUS,
                java.util.Optional.empty(), 5000);
        check(radius.get() == 5000, "the Ultimate's Hex grows past the Scarlet Witch's 96: " + radius.get());
        com.kolodkingal.enhanced.hex.HexManager.handleAction(player, com.kolodkingal.enhanced.hex.HexAction.SET_RADIUS,
                java.util.Optional.empty(), Integer.MAX_VALUE);
        check(radius.get() == com.kolodkingal.enhanced.hex.HexManager.WORLD_LIMIT, "as far as the world border: " + radius.get());
        f.config(com.kolodkingal.enhanced.EnhancedConfig.ULTIMATE_HEX_MAX_RADIUS, 300);
        com.kolodkingal.enhanced.hex.HexManager.handleAction(player, com.kolodkingal.enhanced.hex.HexAction.SET_RADIUS,
                java.util.Optional.empty(), 5000);
        check(radius.get() == 300, "unless the server sets a limit: " + radius.get());
        // (dissolved before the level ticks: a Hex this wide would hold the tests beside it)
        com.kolodkingal.enhanced.hex.HexManager.dissolveAllOwnedBy(player.server, player.getUUID());
        helper.succeed();
    }

    /** (3.19.4) B in her Multiverse menu: the Chaos Energy Eruption hurts everyone in reach at least ten hearts, through armour. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void herChaosEruptionHitsForAtLeastTenHearts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        multiverseMenu(player);
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(16.5, FLOOR, 10.5));
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        husk.setHealth(1000.0F);
        husk.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
        husk.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET));
        check(UltimateWanda.ERUPTION_DAMAGE >= 20.0F, "at least ten hearts before she is any stronger");
        press(player, 4, false);
        float lost = 1000.0F - husk.getHealth();
        check(lost >= 20.0F, "B: the eruption takes at least ten hearts through netherite: " + lost);
        check(Powers.get(player).chaosShieldActive() == false, "and B alone is not her Chaos Shield");
        check(UltimateWanda.eruption(player) == -1, "then it has a short recharge");
        helper.succeed();
    }

    private static void multiverseMenu(ServerPlayer player) {
        PowerData data = Powers.get(player);
        for (int i = 0; i < 3 && !UltimateWanda.multiverseMenu(data); i++) {
            UltimateWanda.onLookKey(player, data, 1);
        }
        check(UltimateWanda.multiverseMenu(data), "H turns to her Multiverse menu");
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void sheBanishesToTheNexusAndOnlyHerPortalLeadsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = ultimate(f, 3.5, 10.5);
        multiverseMenu(player);
        ServerPlayer victim = f.player(9.5, FLOOR, 10.5, Power.NONE);
        f.onClose(() -> com.kolodkingal.enhanced.nexus.Nexus.setBanished(victim, false));
        if (com.kolodkingal.enhanced.nexus.Nexus.level(player.server) == null) {
            AsgardTestSupport.makeLevel(player.server, com.kolodkingal.enhanced.nexus.Nexus.KEY, "/data/enhanced/dimension/nexus.json");
        }
        check(com.kolodkingal.enhanced.nexus.Nexus.level(player.server) != null, "the world has its Nexus");
        TestKit.lookAt(player, victim);
        check(UltimateWanda.banish(player), "R, her whisper, reaches whom she looks at");
        check(com.kolodkingal.enhanced.nexus.Nexus.in(victim), "R, her whisper: a flash of red and he is in the Nexus");
        check(com.kolodkingal.enhanced.nexus.Nexus.banished(victim), "banished");
        check(victim.getY() >= com.kolodkingal.enhanced.nexus.Nexus.GROUND, "standing on its island: " + victim.position());
        net.minecraft.server.level.ServerLevel nexus = (net.minecraft.server.level.ServerLevel) victim.level();
        check(!nexus.getBlockState(net.minecraft.core.BlockPos.containing(0, com.kolodkingal.enhanced.nexus.Nexus.GROUND, 0)).isAir(),
                "the island of the Nexus is built");
        net.minecraft.server.level.ServerLevel home = helper.getLevel();
        Vec3 aside = helper.absoluteVec(new Vec3(14.5, FLOOR, 16.5));
        victim.teleportTo(home, aside.x, aside.y, aside.z, 0.0F, 0.0F);
        check(com.kolodkingal.enhanced.nexus.Nexus.in(victim), "no power takes him out");
        check(!com.kolodkingal.enhanced.nexus.Nexus.leaveThrough(victim, victim.getUUID(), home), "nor anyone's portal");
        // dead, or out some other way: he is taken straight back
        com.kolodkingal.enhanced.nexus.Nexus.setBanished(victim, false);
        victim.teleportTo(home, aside.x, aside.y, aside.z, 0.0F, 0.0F);
        com.kolodkingal.enhanced.nexus.Nexus.setBanished(victim, true);
        victim.tickCount = 0;
        com.kolodkingal.enhanced.nexus.Nexus.tick(victim);
        check(com.kolodkingal.enhanced.nexus.Nexus.in(victim), "found outside, he is brought back to the Nexus");
        // the void gives him back
        victim.teleportTo(nexus, 0.5, 2.0, 0.5, 0.0F, 0.0F);
        com.kolodkingal.enhanced.nexus.Nexus.tick(victim);
        check(victim.getY() > com.kolodkingal.enhanced.nexus.Nexus.GROUND, "falling off the island brings him back onto it");
        check(com.kolodkingal.enhanced.nexus.Nexus.leaveThrough(victim, player.getUUID(), home), "her red portal leads him out");
        com.kolodkingal.enhanced.nexus.Nexus.crossed(victim);
        check(!com.kolodkingal.enhanced.nexus.Nexus.banished(victim), "and he is free");
        victim.teleportTo(home, aside.x, aside.y, aside.z, 0.0F, 0.0F);
        check(!com.kolodkingal.enhanced.nexus.Nexus.in(victim), "free, he may go");
        // a creature too
        net.minecraft.world.entity.monster.Pillager pillager = TestKit.spawnWithNoFreeWill(helper, EntityType.PILLAGER,
                new Vec3(3.5, FLOOR, 4.5));
        TestKit.lookAt(player, pillager);
        helper.runAfterDelay(UltimateWanda.BANISH_COOLDOWN + 1, () -> {
            press(player, 1, false);
            check(helper.getLevel().getEntity(pillager.getUUID()) == null, "a creature is banished too");
            // Sneak + R: a red portal into the Nexus
            press(player, 1, true);
            com.kolodkingal.enhanced.strange.SlingPortals.Pair pair = com.kolodkingal.enhanced.strange.SlingPortals.of(player.getUUID());
            check(pair != null && pair.farDimension() == com.kolodkingal.enhanced.nexus.Nexus.KEY, "Sneak + R opens her portal into the Nexus");
            com.kolodkingal.enhanced.strange.SlingPortals.closeOwnedBy(player.server, player.getUUID());
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void asTheUltimateHerSlingRingReachesOtherUniverses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = ultimate(f, 10.5, 10.5);
        check(com.kolodkingal.enhanced.multiverse.UniverseTravel.cosmic(player), "Ultimate Wanda's Sling Ring opens ways to other universes");
        check(!com.kolodkingal.enhanced.multiverse.UniverseTravel.cosmic(wanda(f, 3.5, 3.5, false)), "the Scarlet Witch's does not");
        helper.succeed();
    }

    private static final class WandaAbilitiesAccess {
        static int windUp() {
            return com.kolodkingal.enhanced.ability.WandaAbilities.windUpTicks();
        }
    }
}
