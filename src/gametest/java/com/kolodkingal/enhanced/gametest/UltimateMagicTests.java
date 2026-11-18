package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.magic.Grimoires;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicData;
import com.kolodkingal.enhanced.magic.Spell;
import com.kolodkingal.enhanced.magic.WitchPath;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.VisionQuest;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultimate.Demiurge;
import com.kolodkingal.enhanced.ultimate.NexusBeing;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import com.kolodkingal.enhanced.ultimate.UltimateForm;
import com.kolodkingal.enhanced.ultimate.UltimateForms;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.17: Ultimate Mode and the magic. One switch ({@code [ultimate] enabled}, or the world's own setting) makes Billy the
 * Demiurge (forty health, Netherite's protection, a barrier round his whole body, the magic types) and lets the Scarlet
 * Witch become the Nexus Being (her hurts heal within two seconds, her worlds' runes strip the other heroes, chaos flashes
 * strike her enemies near, no time limit); Source Code Vision runs on the Solar Jewel only while it is on. The witches'
 * paths teach their spells by level, the grimoires theirs when read, and every spell, path and form has its words in
 * English and Hebrew with the same placeholders. Batch {@code ultimate_magic}.
 */
public final class UltimateMagicTests {
    private static final String BATCH = "ultimate_magic";
    /** The test that turns the switch itself runs alone (the tests of a batch run together and share the switch). */
    private static final String SWITCH = "ultimate_switch";

    private UltimateMagicTests() {
    }

    /** Ultimate Mode on for this test (the config, and no world setting in the way), off again when it ends. */
    private static TestKit.Fixture ultimate(GameTestHelper helper, boolean on) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(Ultimate.worldSetting() == null || Ultimate.worldSetting() == on, "no world setting in the way");
        f.config(UltimateConfig.ENABLED, on);
        return f;
    }

    @EnhancedTest(template = ARENA, batch = SWITCH, timeoutTicks = 100)
    public static void oneSwitchMakesEveryHeroUltimate(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, false);
        f.onClose(() -> Ultimate.set(null));
        ServerPlayer vision = f.player(3.5, FLOOR, 3.5, Power.VISION);
        Powers.setVariant(vision, com.kolodkingal.enhanced.power.Variants.get(Power.VISION, "source_code"));
        check(!Ultimate.on() && !VisionQuest.solar(Powers.get(vision)), "off: Source Code Vision has Vision's own powers");
        Ultimate.set(true);
        check(Ultimate.on() && VisionQuest.solar(Powers.get(vision)), "the world's setting: the Solar Jewel is his");
        Ultimate.set(null);
        check(!Ultimate.on(), "the config decides again");
        for (Power p : Power.values()) {
            if (p != Power.NONE) {
                check(UltimateForm.of(p) != null, p + " has an ultimate form");
            }
        }
        check(UltimateForm.of(Power.SCARLET_WITCH) == UltimateForm.SCARLET_WITCH, "Wanda's is the Nexus Being");
        // Billy is the Demiurge while it is on, and only then
        Ultimate.set(true);
        ServerPlayer billy = f.player(6.5, FLOOR, 3.5, Power.BILLY);
        Demiurge.tick(billy);
        Demiurge.cycle(billy);
        Vec3 behind = BillyBarrier.body(billy).subtract(billy.getLookAngle().scale(2.0));
        check(Magic.isDemiurge(billy) && billy.getMaxHealth() == 40.0F && BillyBarrier.covers(billy, behind)
                && Demiurge.borrowed(billy) == Power.SCARLET_WITCH, "on: the Demiurge");
        Ultimate.set(false);
        Demiurge.tick(billy);
        check(!Magic.isDemiurge(billy) && billy.getMaxHealth() == 20.0F, "off: Billy again, ten hearts: " + billy.getMaxHealth());
        check(!BillyBarrier.covers(billy, behind), "and his barrier is a plate in front of him");
        check(Demiurge.borrowed(billy) == Power.NONE && Powers.get(billy).borrowed() == Power.NONE, "no Demiurge, no magic types");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theDemiurgeHasTwentyHeartsAndNetheritesProtection(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer billy = f.player(3.5, FLOOR, 3.5, Power.BILLY);
        Demiurge.tick(billy);
        check(Magic.isDemiurge(billy), "Billy is the Demiurge");
        check(billy.getMaxHealth() == 40.0F, "forty health: " + billy.getMaxHealth());
        check(billy.getArmorValue() >= 20, "Netherite's armour: " + billy.getArmorValue());
        check(AbilityColors.chosen(billy, HeroAbility.BILLY_HEX_BARRIER) == AbilityColors.DEMIURGE_BLUE, "his bright blue");
        Vec3 behind = BillyBarrier.body(billy).subtract(billy.getLookAngle().scale(2.0));
        check(BillyBarrier.covers(billy, behind), "his barrier wraps his whole body (behind him too)");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theDemiurgesMagicTypesGoRoundAndBack(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer billy = f.player(3.5, FLOOR, 3.5, Power.BILLY);
        PowerData data = Powers.get(billy);
        check(Demiurge.borrowed(billy) == Power.NONE && !data.has(HeroAbility.WANDA_CHAOS_BEAM), "his own magic first");
        Demiurge.cycle(billy);
        check(Demiurge.borrowed(billy) == Power.SCARLET_WITCH && data.has(HeroAbility.WANDA_CHAOS_BEAM)
                && data.acts(Power.SCARLET_WITCH), "the Scarlet Witch's abilities are his");
        check(AbilityColors.chosen(billy, HeroAbility.WANDA_CHAOS_BEAM) == AbilityColors.DEMIURGE_BLUE, "in his blue");
        for (int i = 0; i < Demiurge.MAGIC_TYPES.size(); i++) {
            Demiurge.cycle(billy);
        }
        check(Demiurge.borrowed(billy) == Power.NONE && !data.has(HeroAbility.WANDA_CHAOS_BEAM), "and back to his own");
        Demiurge.cycle(billy);
        Demiurge.cycle(billy);
        check(Demiurge.borrowed(billy) == Power.DOCTOR_STRANGE, "Doctor Strange's next");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theNexusBeingHealsHoldsTheWorldAndStripsTheHeroes(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer wanda = f.player(3.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer thor = f.player(8.5, FLOOR, 3.5, Power.THOR);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, 6, 2, 8);
        UltimateForms.awaken(wanda, UltimateForm.SCARLET_WITCH);
        // (nothing ticks a player without a client: the test ticks her form)
        helper.onEachTick(() -> UltimateForms.tick(wanda));
        int awaken = UltimateConfig.AWAKEN_TICKS.get() + 2;
        float[] zombieHealth = new float[1];
        helper.runAfterDelay(awaken, () -> {
            check(UltimateForms.active(wanda) && NexusBeing.is(wanda), "the Nexus Being: active " + UltimateForms.active(wanda)
                    + ", ultimate " + Powers.get(wanda).is(Live.ULTIMATE) + ", awakening " + Powers.get(wanda).is(Live.ULTIMATE_AWAKENING)
                    + ", on " + Ultimate.on() + ", time " + wanda.level().getGameTime());
            check(NexusBeing.worlds(wanda).contains(wanda.level().dimension()), "the world is hers");
            check(NexusBeing.strips(thor) && !NexusBeing.strips(wanda), "her runes strip Thor, not her");
            wanda.setHealth(6.0F);
            zombieHealth[0] = zombie.getHealth();
        });
        helper.runAfterDelay(awaken + 42, () -> {
            check(wanda.getHealth() == wanda.getMaxHealth(), "her hurt healed within two seconds: " + wanda.getHealth());
            check(zombie.getHealth() < zombieHealth[0] || !zombie.isAlive(), "the chaos flashes struck the zombie near");
            check(thor.hasEffect(com.kolodkingal.enhanced.registry.ModEffects.DRAINED), "Thor's powers are stripped");
            check(UltimateForms.damageFactor(wanda) >= 2.0F, "her chaos hits at least twice as hard: "
                    + UltimateForms.damageFactor(wanda));
            UltimateForms.key(wanda, true);
            check(!UltimateForms.active(wanda) && NexusBeing.worlds(wanda).isEmpty() && !NexusBeing.strips(thor),
                    "Sneak + the key ends it: the world is free");
            check(UltimateForms.cooldownSeconds(wanda) == 0, "and it has no cooldown");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void thePathsTeachByLevelAndTheGrimoiresWhenRead(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer witch = f.player(3.5, FLOOR, 3.5);
        Magic.set(witch, MagicData.EMPTY.withCraft(WitchPath.GREEN, 1, false));
        check(Magic.isWitch(witch) && Magic.knows(witch, Spell.VINE_SNARE) && !Magic.knows(witch, Spell.THORN_WALL),
                "a Green Witch of the first level knows the Vine Snare, not yet the Thorn Wall");
        check(!Magic.knows(witch, Spell.QUICK_BREW) && !Magic.knows(witch, Spell.CHRONO_HOLD), "nor another path's, nor a grimoire's");
        check(Grimoires.read(witch, MagicData.CAGLIOSTRO, Component.literal("book")), "she reads Cagliostro's grimoire");
        check(Magic.knows(witch, Spell.CHRONO_HOLD) && !Grimoires.read(witch, MagicData.CAGLIOSTRO, Component.literal("book")),
                "its spells are hers, and a second reading teaches nothing");
        ServerPlayer nobody = f.player(6.5, FLOOR, 3.5);
        check(!Grimoires.read(nobody, MagicData.DARKHOLD, Component.literal("book")), "someone with no magic cannot read it");
        check(ModItems.CAGLIOSTROS_GRIMOIRE.get() != null, "the grimoire is an item");
        ServerPlayer billy = f.player(9.5, FLOOR, 3.5, Power.BILLY);
        int known = 0;
        for (Spell s : Spell.values()) {
            known += Magic.knows(billy, s) ? 1 : 0;
        }
        check(known == Spell.values().length, "the Demiurge knows every spell: " + known + " of " + Spell.values().length);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void anOldSolarJewelSwitchTurnsUltimateModeOn(GameTestHelper helper) throws Exception {
        com.kolodkingal.enhanced.config.core.Toml.Table old = com.kolodkingal.enhanced.config.core.Toml.parse(
                "[vision.solar_jewel]\nenabled = true\nmaxCharge = 900\n");
        com.kolodkingal.enhanced.config.core.ConfigFile.migrate(old);
        check(Boolean.TRUE.equals(old.get(java.util.List.of("ultimate", "enabled"))), "3.16's switch on: Ultimate Mode on");
        com.kolodkingal.enhanced.config.core.Toml.Table off = com.kolodkingal.enhanced.config.core.Toml.parse(
                "[vision.solar_jewel]\nenabled = false\n");
        com.kolodkingal.enhanced.config.core.ConfigFile.migrate(off);
        check(off.get(java.util.List.of("ultimate", "enabled")) == null, "off: nothing changes");
        com.kolodkingal.enhanced.config.core.Toml.Table both = com.kolodkingal.enhanced.config.core.Toml.parse(
                "[vision.solar_jewel]\nenabled = true\n[ultimate]\nenabled = false\n");
        com.kolodkingal.enhanced.config.core.ConfigFile.migrate(both);
        check(Boolean.FALSE.equals(both.get(java.util.List.of("ultimate", "enabled"))), "the new setting wins");
        helper.succeed();
    }

    // ---- with Ultimate Mode off, the heroes are the heroes of 3.16 ----

    @EnhancedTest(template = ARENA, batch = SWITCH, timeoutTicks = 100)
    public static void withTheModeOffNoHeroHasAnythingOfIt(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, false);
        f.onClose(() -> Ultimate.set(null));
        ServerPlayer wanda = f.player(3.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(5.5, FLOOR, 3.5, Power.BILLY);
        ServerPlayer strange = f.player(7.5, FLOOR, 3.5, Power.DOCTOR_STRANGE);
        ServerPlayer spider = f.player(9.5, FLOOR, 3.5, Power.SPIDER_MAN);
        ServerPlayer tony = f.player(3.5, FLOOR, 6.5, Power.IRON_MAN);
        ServerPlayer tommy = f.player(5.5, FLOOR, 6.5, Power.TOMMY);
        ServerPlayer witch = f.player(7.5, FLOOR, 6.5);
        Magic.set(witch, MagicData.EMPTY.withCraft(WitchPath.GREEN, 1, false));
        for (ServerPlayer hero : new ServerPlayer[]{wanda, billy, strange, spider, tony, tommy}) {
            double armour = hero.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double scale = hero.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            float health = hero.getMaxHealth();
            Demiurge.tick(hero);
            UltimateForms.tick(hero);
            com.kolodkingal.enhanced.ultimate.BespokeForms.tick(hero);
            check(!Magic.isCaster(hero), Powers.power(hero) + " casts no spells: no mana, no spell key");
            check(!Grimoires.read(hero, MagicData.DARKHOLD, Component.literal("book")) && !Magic.data(hero).has(MagicData.DARKHOLD),
                    Powers.power(hero) + " learns nothing from a grimoire: the book is the book it was");
            UltimateForms.key(hero, false);
            check(!UltimateForms.active(hero) && !Powers.get(hero).is(Live.ULTIMATE) && !Powers.get(hero).is(Live.ULTIMATE_AWAKENING),
                    Powers.power(hero) + " awakens nothing");
            check(hero.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == armour
                    && hero.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) == scale
                    && hero.getMaxHealth() == health, Powers.power(hero) + "'s body is unchanged");
            check(UltimateForms.damageFactor(hero) == 1.0F, Powers.power(hero) + " hits as hard as ever");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.flightFactor(hero) == 1.0F
                    && com.kolodkingal.enhanced.ultimate.BespokeForms.runHunger(hero) == 1.0F
                    && !com.kolodkingal.enhanced.ultimate.BespokeForms.flies(hero)
                    && !com.kolodkingal.enhanced.ultimate.BespokeForms.timeProof(hero)
                    && !com.kolodkingal.enhanced.ultimate.BespokeForms.piercesShields(hero.damageSources().playerAttack(hero)),
                    "nothing of the forms on " + Powers.power(hero));
            check(Powers.get(hero).acting() == Powers.power(hero) && Powers.get(hero).borrowed() == Power.NONE, "his keys are his own");
        }
        check(!Magic.isDemiurge(billy) && !Magic.mindscapeOpen(billy), "Billy is Billy: no Demiurge, no Mindscape");
        // (Tommy's one new look is there to choose; the look he starts in is the one he always started in)
        check("halloween".equals(com.kolodkingal.enhanced.power.Variants.get(Power.TOMMY, "").id()), "Tommy's default look is unchanged");
        check("visionquest".equals(com.kolodkingal.enhanced.power.Variants.get(Power.TOMMY, "visionquest").id()), "and VisionQuest is a look of his");
        check(Magic.isCaster(witch) && Magic.knows(witch, Spell.VINE_SNARE), "a witch is a witch with the mode off too");
        Ultimate.set(true);
        check(Magic.isCaster(billy) && Magic.isCaster(strange) && Magic.isCaster(wanda), "on: the Demiurge and the hero sorcerers cast");
        Ultimate.set(false);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theMindscapeIsBillysAndTheirsWhoseMagicIsHisGift(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer billy = f.player(3.5, FLOOR, 3.5, Power.BILLY);
        ServerPlayer gifted = f.player(5.5, FLOOR, 3.5);
        ServerPlayer other = f.player(7.5, FLOOR, 3.5);
        ServerPlayer strange = f.player(9.5, FLOOR, 3.5, Power.DOCTOR_STRANGE);
        com.kolodkingal.enhanced.magic.Grants.fromScreen(billy, gifted.getGameProfile().getName(), WitchPath.GREEN.id(), 2, false);
        com.kolodkingal.enhanced.magic.Grants.give(other, WitchPath.GREEN, 2, true);
        check(Magic.isWitch(gifted) && Magic.data(gifted).has(MagicData.BILLYS), "the Demiurge's gift is marked his");
        check(Magic.mindscapeOpen(billy) && Magic.knows(billy, Spell.MINDSCAPE), "Billy himself");
        check(Magic.mindscapeOpen(gifted) && Magic.knows(gifted, Spell.MINDSCAPE), "and the witch he made");
        check(Magic.isWitch(other) && !Magic.mindscapeOpen(other) && !Magic.knows(other, Spell.MINDSCAPE),
                "not a witch made by a command or the Road");
        check(!Magic.mindscapeOpen(strange) && !Magic.knows(strange, Spell.MINDSCAPE), "nor a hero sorcerer");
        check(!com.kolodkingal.enhanced.magic.Realms.toggleMindscape(other), "and the spell itself refuses her");
        com.kolodkingal.enhanced.magic.Grants.give(gifted, WitchPath.POTIONS, 1, true);
        check(!Magic.mindscapeOpen(gifted), "a craft given anew by someone else is no longer Billy's gift");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theChaosBurstStrikesEveryoneNearButHer(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer wanda = f.player(3.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        Zombie near = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, 5, 2, 5);
        Zombie far = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, 12, 2, 12);
        UltimateForms.awaken(wanda, UltimateForm.SCARLET_WITCH);
        helper.onEachTick(() -> UltimateForms.tick(wanda));
        helper.runAfterDelay(UltimateConfig.AWAKEN_TICKS.get() + 3, () -> {
            float mine = wanda.getHealth();
            float a = near.getHealth();
            float b = far.getHealth();
            UltimateForms.key(wanda, false);
            check(near.getHealth() < a || !near.isAlive(), "her signature, the great burst, strikes the one beside her");
            check(far.getHealth() < b || !far.isAlive(), "and the one far off (within " + UltimateConfig.NEXUS_FLASH_RANGE.get() + " blocks)");
            check(wanda.getHealth() >= mine, "never her");
            UltimateForms.key(wanda, true);
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void cosmicSpiderManTheGodkillerAndUltimateTommy(GameTestHelper helper) {
        TestKit.Fixture f = ultimate(helper, true);
        ServerPlayer spider = f.player(3.5, FLOOR, 3.5, Power.SPIDER_MAN);
        ServerPlayer tony = f.player(8.5, FLOOR, 8.5, Power.IRON_MAN);
        ServerPlayer tommy = f.player(3.5, FLOOR, 8.5, Power.TOMMY);
        ServerPlayer[] all = {spider, tony, tommy};
        for (ServerPlayer p : all) {
            UltimateForms.awaken(p, UltimateForm.of(p));
        }
        helper.onEachTick(() -> {
            for (ServerPlayer p : all) {
                UltimateForms.tick(p);
                com.kolodkingal.enhanced.ultimate.BespokeForms.tick(p);
            }
        });
        helper.runAfterDelay(UltimateConfig.AWAKEN_TICKS.get() + 3, () -> {
            var forms = com.kolodkingal.enhanced.ultimate.BespokeForms.class;
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.cosmic(spider) && com.kolodkingal.enhanced.ultimate.BespokeForms.godkiller(tony)
                    && com.kolodkingal.enhanced.ultimate.BespokeForms.tommy(tommy), "the three forms: " + forms.getSimpleName());
            // Cosmic Spider-Man
            check(spider.getArmorValue() >= 20, "Netherite's protection: " + spider.getArmorValue());
            check(spider.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) >= 22.0, "three Hulks' strength");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.flies(spider), "he flies");
            check(Magic.isCaster(spider) && Magic.knows(spider, Spell.TRANSMUTE) && Magic.knows(spider, Spell.DIMENSION_GATE)
                    && !Magic.knows(spider, Spell.UNMAKING), "matter and portals are his spells, no more");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.cosmicEnergy(spider) <= 170.0F, "the Uni-Power's meter");
            var dark = com.kolodkingal.enhanced.registry.ModDamageTypes.source(spider.level(), com.kolodkingal.enhanced.registry.ModDamageTypes.DARK_SPELL, null, null);
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.taken(spider, dark) == 1.5F, "dark magic hurts him half as much again");
            // the Godkiller
            double scale = tony.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            check(Math.abs(scale * 1.8 - 65.0) < 0.01, "the Godkiller stands 65 blocks tall: " + scale * 1.8 + " (scale " + scale + ")");
            // (his eyes are 58.5 blocks up; the game stops a reach at 64)
            double reach = tony.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
            check(reach >= 1.62 * scale + 3.0, "and reaches what stands under his feet: " + reach);
            check(tony.getArmorValue() >= 24, "more than Netherite: " + tony.getArmorValue());
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.taken(tony, tony.damageSources().generic()) < 1.0F
                    && com.kolodkingal.enhanced.ultimate.BespokeForms.taken(tony, dark) > 1.0F, "blows glance off; magic bites a little more");
            check(UltimateForms.damageFactor(tony) >= 2.5F, "its weapons are greater");
            // Ultimate Tommy
            check(tommy.getArmorValue() >= 20, "Netherite's protection");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.taken(tommy, dark) == 0.6F, "spells hurt him less");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.timeProof(tommy) && com.kolodkingal.enhanced.ultimate.BespokeForms.runHunger(tommy) == 0.0F,
                    "time's powers pass him by, his running costs no hunger");
            // through magic shields: his blows, nobody else's
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.piercesShields(tommy.damageSources().playerAttack(tommy))
                    && !com.kolodkingal.enhanced.ultimate.BespokeForms.piercesShields(spider.damageSources().playerAttack(spider)),
                    "Ultimate Tommy's blows go through magic shields");
            ServerPlayer wanda = f.player(8.5, FLOOR, 3.5, Power.SCARLET_WITCH);
            com.kolodkingal.enhanced.hex.Hex hex = com.kolodkingal.enhanced.hex.HexManager.create(wanda);
            check(com.kolodkingal.enhanced.hex.HexManager.judge(hex, tommy) == com.kolodkingal.enhanced.hex.HexManager.Verdict.PASS
                    && com.kolodkingal.enhanced.hex.HexManager.judge(hex, tommy, true) == com.kolodkingal.enhanced.hex.HexManager.Verdict.PASS,
                    "and he passes through the Hex, in and out");
            check(com.kolodkingal.enhanced.hex.HexManager.judge(hex, tony) != com.kolodkingal.enhanced.hex.HexManager.Verdict.PASS,
                    "which still stops another hero");
            com.kolodkingal.enhanced.hex.HexManager.dissolve(wanda.serverLevel(), hex);
            // Cosmic Spider-Man leaves his form with Sneak + the key
            UltimateForms.key(spider, true);
            check(!com.kolodkingal.enhanced.ultimate.BespokeForms.cosmic(spider) && !UltimateForms.active(spider), "Sneak + the key: Spider-Man again");
            Powers.get(tommy).setSlowMo(true);
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.slowedByTommy(tony) || tony.isCreative(), "his speed slows the whole world's players");
            check(com.kolodkingal.enhanced.ultimate.BespokeForms.flightFactor(tony) < 0.5F || tony.isCreative(), "their flight too");
            check(!com.kolodkingal.enhanced.ultimate.BespokeForms.slowedByTommy(tommy), "not him");
            Powers.get(tommy).setSlowMo(false);
            for (ServerPlayer p : all) {
                UltimateForms.end(p, false);
                com.kolodkingal.enhanced.ultimate.BespokeForms.tick(p);
            }
            check(tony.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) == 1.0 && tommy.getArmorValue() < 20,
                    "the forms end: their bodies are their own again");
            helper.succeed();
        });
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd%]");

    private static String placeholders(String text) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            out.append(m.group()).append(' ');
        }
        return out.toString();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void everySpellPathAndFormHasItsWords(GameTestHelper helper) throws java.io.IOException {
        JsonObject en = TestKit.lang("en_us");
        JsonObject he = TestKit.lang("he_il");
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (Spell s : Spell.values()) {
            for (String part : new String[]{"", ".desc", ".chant", ".wish"}) {
                keys.add("spell.enhanced." + s.id() + part);
            }
        }
        for (WitchPath p : WitchPath.values()) {
            keys.add("witch_path.enhanced." + p.id());
            keys.add("witch_path.enhanced." + p.id() + ".desc");
        }
        for (UltimateForm u : UltimateForm.values()) {
            keys.add("ultimate.enhanced." + u.id());
            keys.add("ultimate.enhanced." + u.id() + ".desc");
            keys.add("ultimate.enhanced." + u.id() + ".signature");
        }
        for (String k : new String[]{"key.enhanced.spells", "key.enhanced.ultimate", "key.enhanced.magic_types",
                "message.enhanced.mindscape.closed", "message.enhanced.cosmic.spent", "message.enhanced.cosmic.recharging",
                "hud.enhanced.cosmic.energy", "message.enhanced.tommy.too_hungry", "enhanced.configuration.ultimate",
                "enhanced.configuration.witchcraft", "message.enhanced.nexus.stripped", "message.enhanced.demiurge.magic_type",
                "item.enhanced.cagliostros_grimoire", "commands.enhanced.ultimate.info"}) {
            keys.add(k);
        }
        for (String k : keys) {
            check(en.has(k) && he.has(k), "both languages have " + k);
            check(placeholders(en.get(k).getAsString()).equals(placeholders(he.get(k).getAsString())),
                    "the same placeholders in Hebrew: " + k);
        }
        helper.succeed();
    }
}
