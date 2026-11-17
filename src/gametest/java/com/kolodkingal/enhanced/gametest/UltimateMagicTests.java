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
                    "Sneak + O ends it: the world is free");
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
        for (String k : new String[]{"key.enhanced.spells", "key.enhanced.ultimate", "enhanced.configuration.ultimate",
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
