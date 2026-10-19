package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CatalogSearch;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.CustomKitSupport.def;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.press;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Hero Creator lists everything (3.5, the creator lane): a table written by hand of every ability, Sneak variant,
 * passive, switch, body and flight each built-in hero of 3.4 has ({@link #AUDIT}, the source of truth: a hero who gains
 * an ability must have a line here), and the tests that each line is a card of the catalogue with its name and line in
 * English and Hebrew, found by the search in both languages, of its hero; that every Sneak variant is a card of its own
 * and works alone (Billy's Hex Barrier); and the rules of the variant rows and of the Scarlet Witch's Build Magic.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CreatorCatalogTests {
    private static final String BATCH = "creator_catalog";

    /**
     * One thing a hero has, and the catalogue entry it is found as ({@code flight}: the Flight power, in the hero's
     * style).
     */
    record Item(String what, String id) {
    }

    /**
     * Every ability of every built-in hero of 3.4, from the heroes' code (their keys, Sneak variants, passives,
     * switches, bodies, flight): what it is, and its card. (Heimdall, Odin and Hela are 3.5's own lanes.)
     */
    static final Map<Power, List<Item>> AUDIT = new EnumMap<>(Power.class);

    static {
        hero(Power.MAGNETO, "metal control: grab and throw metal", "magneto.metal",
                "Magnetic Pull", "magneto.magnetic_pull", "Shrapnel", "magneto.shrapnel", "Disarm", "magneto.disarm",
                "Magnetic Shield", "magneto.shield", "flight", "flight");
        hero(Power.SCARLET_WITCH, "Chaos Beam", "wanda.chaos_beam",
                "telekinesis (tear a block, set down)", "wanda.telekinesis", "Chaos Shield", "wanda.chaos_shield",
                "the Hex and Hex Control", "wanda.hex", "dissolve the Hex", "wanda.hex_dissolve",
                "Build Magic with Instant", "wanda.build_magic", "flight", "flight");
        hero(Power.QUICKSILVER, "Quicksilver Time", "quicksilver.time", "Walkman", "quicksilver.walkman",
                "Super Speed", "quicksilver.speed", "Dash Strike", "quicksilver.dash",
                "Speed Blitz", "quicksilver.blitz", "Pickpocket", "quicksilver.pickpocket",
                "wall and water running", "quicksilver.wall_run", "Pebble Barrage", "quicksilver.barrage");
        hero(Power.VISION, "Mind Stone beam", "vision.mind_beam", "Phase", "vision.phase", "Density", "vision.density",
                "Disguise", "vision.disguise", "synthetic body", "vision.body", "flight", "flight");
        hero(Power.BILLY, "Hex Beam (or Hex Bolt)", "billy.hex_beam", "telekinesis", "billy.telekinesis",
                "Freeze", "billy.freeze", "Hex Barrier", "billy.hex_barrier", "The Noise", "billy.noise",
                "Build Magic (Y)", "billy.build_magic", "flight", "flight");
        hero(Power.TOMMY, "Slow Time", "tommy.time", "wall, ceiling and water running", "tommy.wall_run",
                "Super Speed", "tommy.speed", "Dash Strike", "tommy.dash", "Whirlwind", "tommy.whirlwind",
                "Sonic Boom", "tommy.sonic_boom", "Vibrate", "tommy.vibrate", "Disarm Blitz", "tommy.disarm_blitz",
                "Accelerate", "tommy.accelerate");
        hero(Power.LOKI, "daggers", "loki.daggers", "dagger fan", "loki.dagger_fan", "duplicates", "loki.duplicates",
                "shapeshift", "loki.shapeshift", "vanish", "loki.vanish", "Frost Giant", "loki.frost_giant");
        hero(Power.THOR, "throw and recall the hammer, pin", "thor.hammer", "Mjolnir shatter", "thor.shatter",
                "lightning", "thor.lightning", "God of Thunder", "thor.thunder",
                "Bifrost home and saved places", "thor.bifrost", "Bifrost to where you look", "thor.bifrost_look",
                "Asgardian body", "thor.body", "flight", "flight");
        hero(Power.PHOENIX, "mind reading", "phoenix.mind", "Mind Sense", "phoenix.mind_sense",
                "telekinesis", "phoenix.telekinesis", "mind control", "phoenix.control",
                "Psionic Field", "phoenix.psionic_field", "Phoenix Burst", "phoenix.burst",
                "the Phoenix's fire (body)", "phoenix.body", "flight", "flight");
        hero(Power.SPIDER_MAN, "Web Shot", "spider.web", "Web Burst", "spider.web_burst",
                "web zip and Web Assist (J)", "spider.zip", "web swing", "spider.web_swing",
                "wall crawling", "spider.crawl", "Spider-Sense", "spider.sense", "Iron Spider legs", "spider.legs",
                "Instant Kill", "spider.instant_kill", "Black Eyes", "spider.black_eyes", "spider body", "spider.body");
        hero(Power.THANOS, "Power Stone blast", "thanos.power", "Titan Slam", "thanos.titan_slam",
                "Space Stone step", "thanos.space", "Space Pull", "thanos.space_pull",
                "Reality Stone transmute", "thanos.reality", "Reality Veil", "thanos.reality_veil",
                "Soul Drain", "thanos.soul", "Soul Sight", "thanos.soul_sight", "Time Stone rewind", "thanos.time",
                "Time Lock", "thanos.time_lock", "Titan body", "thanos.body");
        hero(Power.ULTRON, "repulsor fire", "ultron.repulsor", "Overcharge", "ultron.overcharge",
                "gravity pull", "ultron.gravity", "gravity slam", "ultron.slam",
                "upload into a drone, Second Life", "ultron.upload", "Hack", "ultron.hack",
                "sentry drones", "ultron.army", "command the drones", "ultron.command", "machine body", "ultron.body",
                "flight", "flight");
        hero(Power.APOCALYPSE, "sand wave", "apocalypse.sand", "Entomb", "apocalypse.entomb",
                "portals", "apocalypse.portal", "golden shield", "apocalypse.shield",
                "power bestowal", "apocalypse.bestow", "Sandstorm", "apocalypse.sandstorm",
                "Apocalypse's body", "apocalypse.body", "flight", "flight");
        hero(Power.AGATHA, "Purple Beam", "agatha.purple_beam", "Hex Mark", "agatha.hex_mark",
                "Power Drain", "agatha.drain", "witch's shield", "agatha.shield", "Rune Trap", "agatha.runes",
                "poison and dark magic (body)", "agatha.body", "flight", "flight");
        hero(Power.CYCLOPS, "Optic Blast", "cyclops.optic_blast", "Ricochet", "cyclops.ricochet",
                "Concussion", "cyclops.concussion", "Leap", "cyclops.leap", "Rally", "cyclops.rally",
                "Shut Eyes (glasses on)", "cyclops.glasses", "Open Eyes (glasses off)", "cyclops.open_eyes",
                "Cyclops's body", "cyclops.body");
        hero(Power.PROFESSOR_X, "Mental Blast", "xavier.mental_blast", "Hear Thoughts", "xavier.thoughts",
                "Take Over a body", "xavier.take_over", "Freeze Minds", "xavier.freeze_minds",
                "Cerebro", "xavier.cerebro");
    }

    private static void hero(Power hero, String... pairs) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            items.add(new Item(pairs[i], pairs[i + 1]));
        }
        AUDIT.put(hero, items);
    }

    /** The heroes whose own flight key the Flight power's styles stand for. */
    private static final Map<Power, String> FLIGHT_STYLE = new LinkedHashMap<>();

    static {
        FLIGHT_STYLE.put(Power.MAGNETO, "magneto");
        FLIGHT_STYLE.put(Power.SCARLET_WITCH, "wanda");
        FLIGHT_STYLE.put(Power.VISION, "vision");
        FLIGHT_STYLE.put(Power.BILLY, "billy");
        FLIGHT_STYLE.put(Power.THOR, "thor");
        FLIGHT_STYLE.put(Power.PHOENIX, "phoenix");
        FLIGHT_STYLE.put(Power.ULTRON, "ultron");
        FLIGHT_STYLE.put(Power.APOCALYPSE, "apocalypse");
        FLIGHT_STYLE.put(Power.AGATHA, "agatha");
    }

    static Map<String, String> lang(String name) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, String> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, json.get(key).getAsString());
        }
        return map;
    }

    /**
     * Every line of the table is a card: in the catalogue, of its hero, named and described in English and Hebrew, and
     * found by the search box by its own name in either language (and among its hero's cards); every flier's flight is
     * a style of the Flight power. Every row of the sixteen heroes is in the table, so nothing a hero has is left out.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyAbilityOfEveryBuiltInHeroIsACardInBothLanguages(GameTestHelper helper) throws IOException {
        Map<String, String> en = lang("en_us");
        Map<String, String> he = lang("he_il");
        List<String> problems = new ArrayList<>();
        Set<String> listed = new HashSet<>();
        int items = 0;
        for (Map.Entry<Power, List<Item>> hero : AUDIT.entrySet()) {
            for (Item item : hero.getValue()) {
                items++;
                CatalogEntry entry = Catalog.byId(item.id());
                String where = hero.getKey() + " / " + item.what() + " (" + item.id() + ")";
                if (entry == null) {
                    problems.add(where + ": no card");
                    continue;
                }
                listed.add(entry.id());
                if (entry == PowerModule.FLIGHT) {
                    if (!AnimCatalog.FLIGHT_STYLES.contains(FLIGHT_STYLE.get(hero.getKey()))) {
                        problems.add(where + ": no flight style");
                    }
                    continue;
                }
                if (entry.hero() != hero.getKey()) {
                    problems.add(where + ": filed under " + entry.hero());
                }
                for (Map<String, String> lang : List.of(en, he)) {
                    String name = lang.get(entry.nameKey());
                    if (name == null || lang.get(entry.descKey()) == null) {
                        problems.add(where + ": no name or line in " + (lang == en ? "English" : "Hebrew"));
                        continue;
                    }
                    if (!CatalogSearch.matches(entry, name, lang::get)) {
                        problems.add(where + ": the search does not find \"" + name + "\"");
                    }
                    String heroName = lang.get("power.enhanced." + hero.getKey().getSerializedName());
                    if (heroName != null && !CatalogSearch.matches(entry, heroName, lang::get)) {
                        problems.add(where + ": not found by its hero's name " + heroName);
                    }
                }
            }
        }
        for (HeroAbility a : HeroAbility.values()) {
            if (AUDIT.containsKey(a.hero) && !listed.contains(a.id())) {
                problems.add(a.id() + " is a card but not in the table");
            }
        }
        check(problems.isEmpty(), "the catalogue against the heroes: " + problems);
        check(items >= 115, "the table is whole: " + items);
        helper.succeed();
    }

    /**
     * Every Sneak variant of the sixteen heroes is a card of its own: a row acting as its parent's variant, of its hero
     * and key.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everySneakVariantIsACardOfItsOwn(GameTestHelper helper) {
        for (HeroAbility a : HeroAbility.values()) {
            if (!AUDIT.containsKey(a.hero) || a.isVariant()) {
                continue;
            }
            HeroAbility v = a.variant();
            check(!a.hasAlt() || v != null, a.id() + "'s Sneak variant has a card");
            if (v != null) {
                check(v.parent() == a && v.hero == a.hero && v.home == a.home && v.act() == a.alt() && !v.hasAlt()
                        && v.takesBox(),
                        v.id() + " is " + a.id() + "'s variant, on its key, acting as its variant acts");
                check(v.resources(HeroAbility.BOTH).containsAll(a.resources(HeroAbility.BOTH)),
                        v.id() + " holds what its parent holds");
                check(v.traits().equals(a.traits()) && v.world == a.world, v.id() + " gives its parent's roles");
            }
        }
        check(HeroAbility.BILLY_HEX_BARRIER.family() == CatalogEntry.Family.SHIELD,
                "the Hex Barrier is one of the shields' styles");
        check(Catalog.family(CatalogEntry.Family.SHIELD).containsAll(List.of(HeroAbility.WANDA_CHAOS_SHIELD,
                HeroAbility.AGATHA_SHIELD, HeroAbility.APOCALYPSE_SHIELD, HeroAbility.BILLY_HEX_BARRIER)),
                "every shield is on the shields' card");
        check(Catalog.family(CatalogEntry.Family.BEAM).containsAll(List.of(HeroAbility.WANDA_CHAOS_BEAM,
                HeroAbility.VISION_MIND_BEAM, HeroAbility.BILLY_HEX_BEAM, HeroAbility.THOR_LIGHTNING,
                HeroAbility.ULTRON_REPULSOR, HeroAbility.AGATHA_PURPLE_BEAM, HeroAbility.CYCLOPS_OPTIC_BLAST,
                HeroAbility.CYCLOPS_OPEN_EYES)), "every beam is on the beams' card");
        helper.succeed();
    }

    /**
     * A variant row alone: bound as its parent's Sneak variant on its own (plain) key, the parent's code acting for it.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theHexBarrierAloneIsRaisedOnAPlainKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero hero = def().pick("billy.hex_barrier", 5).key(3, "billy.hex_barrier").build();
        check("billy.hex_barrier".equals(hero.binding(3, false)) && hero.binding(3, true).isEmpty(),
                "on plain V, its Sneak cell free");
        ServerPlayer player = CustomKitSupport.hero(f, 10.5, 10.5, hero);
        PowerData data = Powers.get(player);
        HeroKit kit = CustomKitSupport.kit(player);
        HeroKit.Binding b = kit.plain(3);
        check(b != null && b.ability() == HeroAbility.BILLY_FREEZE && b.isAlt()
                && b.picked() == HeroAbility.BILLY_HEX_BARRIER,
                "V is Freeze's variant, picked as the Hex Barrier");
        check(kit.has(HeroAbility.BILLY_FREEZE) && kit.hasVariant(HeroAbility.BILLY_FREEZE) && BillyBarrier.owns(data),
                "the barrier is his");
        press(player, 3, false, true);
        check(data.chaosShieldActive(), "V (no Sneak) raises the barrier");
        helper.runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(player), "complete after its wind-up");
            press(player, 3, false, false);
            check(!data.chaosShieldActive(), "V let go: it comes down");
            long now = helper.getLevel().getGameTime();
            check(data.cooldownUntil(3) > now && data.cooldownUntil(4) <= now,
                    "its recharge is on V, not on Billy's B");
            helper.succeed();
        });
    }

    /**
     * Picked with its parent, a variant row is the variant's card: the parent keeps its main action, its Sneak cell is
     * free; each key's cooldown goes on the key that was pressed.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aVariantRowWithItsParentLeavesItsParentTheMainAction(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        CustomHero hero = def().pick("loki.daggers", 5).key(1, "loki.daggers")
                .pick("loki.dagger_fan", 5).key(2, "loki.dagger_fan").build();
        check(HeroAbility.MAIN.equals(hero.pick(HeroAbility.LOKI_DAGGERS).option()),
                "the daggers keep their main action");
        check(hero.binding(1, true).isEmpty() && "loki.dagger_fan".equals(hero.binding(2, false)),
                "the fan is on G, Sneak + R free");
        ServerPlayer player = CustomKitSupport.hero(f, 10.5, 10.5, hero);
        PowerData data = Powers.get(player);
        CustomKitSupport.tap(player, 2, false);
        long now = helper.getLevel().getGameTime();
        check(data.cooldownUntil(2) > now && data.cooldownUntil(1) <= now, "the fan waits on G, the key that threw it");
        CustomKitSupport.tap(player, 1, false);
        check(data.cooldownUntil(1) > now, "the daggers on R");
        // A needed parent: Command needs the Sentry Army.
        CustomHeroRules.Result alone = CustomHeroRules.sanitize(
                def().pick("ultron.command", 5).key(1, "ultron.command").raw(), CustomHeroRules.Limits.DEFAULT);
        check(alone.hero().pick(HeroAbility.ULTRON_COMMAND) == null && !alone.notes().isEmpty(),
                "Command alone is dropped with a note");
        // A server that switches a row off switches its variant off too.
        CustomHeroRules.Limits off = new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of("billy.freeze"));
        check(!off.allows(HeroAbility.BILLY_HEX_BARRIER), "the barrier goes with its Freeze");
        helper.succeed();
    }

    /**
     * The Scarlet Witch's Build Magic: her numbers (no meter, Instant); with Billy's, one of the two (Resource.BUILD).
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theScarletWitchsBuildMagicIsHers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = CustomKitSupport.hero(f, 10.5, 10.5, def().pick("wanda.build_magic", 5).build());
        Limits limits = Limits.of(player);
        check(limits != null && limits.hero() == Power.SCARLET_WITCH && !limits.billy(), "her numbers");
        CustomHeroRules.Result both = CustomHeroRules.sanitize(
                def().pick("billy.build_magic", 5).pick("wanda.build_magic", 5).raw(), CustomHeroRules.Limits.DEFAULT);
        check(both.hero().has(HeroAbility.BILLY_BUILD_MAGIC) && !both.hero().has(HeroAbility.WANDA_BUILD_MAGIC),
                "one Build Magic: the first");
        check(!HeroAbility.WANDA_BUILD_MAGIC.takesBox(), "its own key, Y, and no box");
        helper.succeed();
    }
}
