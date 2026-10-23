package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.Telepathy;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroDamage;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.LookCatalog;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.DarkMagic;
import com.kolodkingal.enhanced.protect.Sorcery;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.4 (S0): the built-in heroes' abilities as rows of the catalogue, the kit on the power data (has, acts,
 * cooldowns by ability), the definition's format 2 (JSON, save, wire; the migration of 3.3's files), the rules of 3.4
 * (resources, requires, world powers, the heroes' abilities switched off, Sneak bindings, moves, stances, animation,
 * layers, the budget with Sneak variants), the roles (Traits) of every hero and of kits, the key router (Sneak cells, the
 * release to the binding that went down, the cast marker) and the strength in the damage.
 */
public final class CustomV2Tests {
    private static final String BATCH = "custom_v2";

    /** A hero of every kind of 3.4: hero abilities with and without their variant, a generic variant, moves, layers. */
    static CustomHero sample() {
        CustomHero raw = CustomKitSupport.def()
                .key(1, "thor.hammer").key(2, "flight").key(3, "thor.lightning").key(4, "energy_blast")
                .sneak(4, "energy_blast#burst")
                .with(h -> h.withMove(4, false, "billy_bolt").withMove(4, true, "phoenix_psionic_cast")
                        .withAnim(new CustomHero.Anim("thor", "thor", CustomHero.AUTO, "none"))
                        .withLayers(CustomHero.Layers.NONE_ALL.with(0, new CustomHero.Layer("plain", 0xB01E23, 0, "always"))
                                .with(3, new CustomHero.Layer("glow", 0x9FE8FF, 2, "powers"))
                                .with(4, new CustomHero.Layer("energy", -1, 0, "flying"))))
                .raw()
                .withPowers(List.of(new CustomHero.Pick("flight", 5, "jet", -1), new CustomHero.Pick("thor.lightning", 6, "both", -1, "thor_beam"),
                        new CustomHero.Pick("thor.hammer", 5, "both", -1), new CustomHero.Pick("energy_blast", 4, "bolts", 0xFF4040),
                        new CustomHero.Pick("thor.body", 5, "both", -1)));
        return CustomHeroRules.sanitize(raw, CustomHeroRules.Limits.DEFAULT).hero();
    }

    // ---- format 2 ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aV2HeroSurvivesItsJsonTheSaveAndTheWire(GameTestHelper helper) {
        CustomHero hero = sample();
        check(CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT).notes().isEmpty(), "the sample is within the rules: "
                + CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT).notes());
        check(hero.binding(1, true).equals("thor.hammer#alt") && hero.binding(4, true).equals("energy_blast#burst"),
                "the hammer's variant was put on Sneak + R, the burst stays on Sneak + B: " + hero.sneakKeys());
        String json = CustomHeroJson.write(hero);
        CustomHeroJson.Read read = CustomHeroJson.read(json);
        check(read.ok() && read.notes().isEmpty(), "reads back cleanly: " + read.notes());
        check(hero.equals(read.hero()), "JSON round trip: " + read.hero() + " vs " + hero);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        check(root.get("version").getAsInt() == 2 && CustomHero.VERSION == 2, "format 2");
        check(root.getAsJsonObject("look").getAsJsonObject("cape").get("style").getAsString().equals("plain"), "layers in the look");
        check(root.getAsJsonObject("moves").get("sneak_b").getAsString().equals("phoenix_psionic_cast"), "moves by binding");
        check(root.getAsJsonObject("anim").get("flight").getAsString().equals("thor"), "the animation");

        Tag saved = CustomHero.CODEC.encodeStart(NbtOps.INSTANCE, hero).getOrThrow();
        check(hero.equals(CustomHero.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow()), "the save round trip");
        ByteBuf buf = Unpooled.buffer();
        CustomHero.STREAM_CODEC.encode(buf, hero);
        check(hero.equals(CustomHero.STREAM_CODEC.decode(buf)) && buf.readableBytes() == 0, "the wire round trip");
        CustomHero big = hero.withSkin(CustomHeroDataTests.png(64, 64, 0x3366CCFF)).withLook(CustomHero.LOOK_SKIN, true);
        ByteBuf p = Unpooled.buffer();
        HeroCreatorPayload.STREAM_CODEC.encode(p, HeroCreatorPayload.apply(big));
        check(p.readableBytes() < 32 * 1024, "the creator's message stays under 32 KiB: " + p.readableBytes());
        check(big.equals(HeroCreatorPayload.STREAM_CODEC.decode(p).hero()), "the creator's message round trip");
        helper.succeed();
    }

    /** The example of 3.3's wiki page, a hand-written file with levels, and a hero saved with a player by 3.3. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void filesAndSavesOf33MigrateToV2(GameTestHelper helper) {
        String wiki = """
                {
                  "format": "enhanced:custom_hero",
                  "version": 1,
                  "name": "Storm Girl",
                  "color": "#3FA9F5",
                  "glow": "#E0F4FF",
                  "look": {"base": "thor/ragnarok", "slim": false},
                  "powers": [
                    {"id": "flight", "strength": 5, "option": "jet"},
                    {"id": "energy_blast", "strength": 6, "option": "beam", "color": "#FF4040"},
                    {"id": "durability", "strength": 5, "option": "armor"},
                    {"id": "healing", "strength": 4, "option": "steady"},
                    {"id": "lightning", "strength": 5, "option": "chain"}
                  ],
                  "keys": ["energy_blast", "flight", "lightning", "", ""]
                }""";
        CustomHeroJson.Read read = CustomHeroJson.read(wiki);
        check(read.ok() && read.notes().isEmpty(), "3.3's example reads with nothing to say: " + read.notes());
        CustomHero h = read.hero();
        check(h.version() == 2, "it is version 2 once read");
        check(h.keys().equals(List.of("energy_blast", "flight", "lightning", "", "")), "its keys are kept");
        check(h.sneakKeys().stream().allMatch(String::isEmpty), "no Sneak bindings");
        check(h.moves().size() == CustomHero.MOVES && h.moves().stream().allMatch(CustomHero.AUTO::equals), "every move automatic");
        check(h.anim().equals(CustomHero.Anim.AUTO_ALL) && h.layers().allNone(), "automatic animation, no layers");
        check(h.powers().stream().allMatch(p -> p.stance().equals(CustomHero.AUTO)), "every stance automatic");
        CustomHeroRules.Result judged = CustomHeroRules.sanitize(h, CustomHeroRules.Limits.DEFAULT);
        check(judged.notes().isEmpty() && judged.hero().equals(h), "and it is a valid hero as it is: " + judged.notes());
        check(JsonParser.parseString(CustomHeroJson.write(h)).getAsJsonObject().get("version").getAsInt() == 2, "written again as 2");

        String levels = "{\"format\":\"enhanced:custom_hero\",\"version\":1,\"name\":\"Old\",\"powers\":[{\"id\":\"fire\",\"level\":2},"
                + "{\"id\":\"flight\",\"level\":1}],\"keys\":[\"fire\",\"flight\"]}";
        CustomHero old = CustomHeroJson.read(levels).hero();
        check(old.pick(PowerModule.FIRE).strength() == 6 && old.pick(PowerModule.FLIGHT).strength() == 3, "levels become strengths");
        check(old.version() == 2 && old.keys().get(0).equals("fire"), "and the file migrates");

        Tag saved33 = StringTag.valueOf(wiki);
        CustomHero fromSave = CustomHero.CODEC.parse(NbtOps.INSTANCE, saved33).getOrThrow();
        check(fromSave.equals(h), "a hero saved with a player by 3.3 loads as format 2");
        helper.succeed();
    }

    /** What 3.3 reads of a 3.4 file: the fields of format 1 are where they were, with the meaning they had. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void a33ReaderFindsTheCoreOfAV2File(GameTestHelper helper) {
        JsonObject root = JsonParser.parseString(CustomHeroJson.write(sample())).getAsJsonObject();
        check(root.get("format").getAsString().equals(CustomHeroJson.FORMAT) && root.get("version").isJsonPrimitive(), "format, version");
        check(root.get("name").getAsString().equals("Kit Tester") && root.get("color").getAsString().startsWith("#"), "name, colours");
        check(root.getAsJsonObject("look").get("base").isJsonPrimitive() && root.getAsJsonObject("look").get("slim").isJsonPrimitive(),
                "look.base and look.slim");
        JsonArray keys = root.getAsJsonArray("keys");
        check(keys.size() == CustomHero.KEYS, "keys is still the array of five");
        List<String> plain = new ArrayList<>();
        keys.forEach(k -> plain.add(k.getAsString()));
        check(plain.equals(List.of("thor.hammer", "flight", "thor.lightning", "energy_blast", "")), "with the plain keys only: " + plain);
        for (var e : root.getAsJsonArray("powers")) {
            JsonObject p = e.getAsJsonObject();
            check(p.get("id").isJsonPrimitive() && p.get("strength").isJsonPrimitive() && p.get("option").isJsonPrimitive(),
                    "every power has id, strength, option: " + p);
        }
        // What 3.3 makes of it: the modules it knows (and the keys that name them) survive its rules.
        List<CustomHero.Pick> known = sample().powers().stream().filter(p -> p.module() != null).toList();
        check(known.size() == 2 && plain.contains("flight") && plain.contains("energy_blast"), "the generic core is intact");
        helper.succeed();
    }

    // ---- the rows ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyKeyOfEveryHeroIsARowOrAFlightKey(GameTestHelper helper) {
        Set<String> ids = new HashSet<>();
        int lastHero = -1;
        for (HeroAbility a : HeroAbility.values()) {
            check(ids.add(a.id()), "ids are unique: " + a.id());
            check(HeroAbility.byId(a.id()) == a && Catalog.byId(a.id().toUpperCase()) == a, a + " is found by its id");
            if (a == HeroAbility.MAGNETO_MAGNETIC_PULL) {
                // (3.5: the creator lane's region of variant rows, appended after the rest, has its own order)
                lastHero = -1;
            }
            check(a.hero.ordinal() >= lastHero, a + " sits in its hero's region, in the order of the heroes");
            lastHero = a.hero.ordinal();
            check(a.id().equals(a.heroId() + "." + a.name) && a.nameKey().equals("hero_module.enhanced." + a.id()), a + " names");
            check(a.home >= 0 && a.home <= 6 && a.costPerPoint >= 1 && a.costPerPoint <= 3, a + " home and cost");
            check(a.passive() == (a.home == 0), a + ": a body and only a body is passive");
            check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(a.icon)), a + "'s icon is an item: " + a.icon);
            for (String needed : a.requires()) {
                check(HeroAbility.byId(needed) != null, a + " requires a row that exists: " + needed);
            }
        }
        for (Power p : Power.values()) {
            if (p == Power.NONE || p == Power.CUSTOM) {
                check(HeroAbility.of(p).isEmpty(), p + " has no rows");
                continue;
            }
            for (int key = 1; key <= 5; key++) {
                HeroAbility row = HeroAbility.at(p, key);
                check((row != null) != (key == p.flightAbility()), p + " key " + key + " is a row or the flight key, never both");
            }
        }
        check(HeroAbility.values().length == 221,
                "71 keys + 9 bodies (L2: Vision's) + Build Magic, 3.5's 15 keys + 3 bodies, the creator lane's 33 "
                + "variant rows + the Scarlet Witch's Build Magic, 3.6's 10 keys + 2 bodies + 9 variant rows, and "
                + "3.7's 10 keys + 2 bodies + 10 variant rows (Wolverine, Deadpool), and 3.8's 22 keys + 5 bodies + 18"
                + " variant rows (Captain America, Red Hulk, Iron Man, Falcon, Love): " + HeroAbility.values().length);
        check(Catalog.all().size()
                == PowerModule.values().length + HeroAbility.values().length + StoneRow.values().length,
                "the catalogue is the three lists (3.6: the stone rows)");
        for (CatalogEntry e : Catalog.all()) {
            check(Catalog.byId(e.id()) == e, e.id() + " is found in the catalogue");
        }
        helper.succeed();
    }

    // ---- the kit ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void forABuiltInHeroTheKitApiAnswersAsBefore(GameTestHelper helper) {
        for (Power p : Power.values()) {
            PowerData data = CustomKitSupport.data(p);
            check(data.kit() == null, p + " has no kit");
            for (Power other : Power.values()) {
                check(data.acts(other) == (other == p), p + " acts only as himself");
            }
            for (HeroAbility a : HeroAbility.values()) {
                boolean own = a.hero == p;
                check(data.has(a) == own, p + " has " + a + " iff it is his");
                int slot = own && a.home >= 1 && a.home <= 5 ? a.home : 0;
                check(data.slot(a) == slot, p + " slot of " + a);
                check(data.bodyScale(a) == 1.0F, "his body is whole");
                if (slot > 0) {
                    data.startCooldown(a, 100, 30);
                    check(data.cooldownUntil(slot) == 130 && !data.isReady(a, 129) && data.isReady(a, 130), a + " on its home key, unscaled");
                } else {
                    check(data.isReady(a, 0), a + " (on no key of his) is always ready");
                }
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCustomKitHasItsAbilitiesOnItsColumnsScaledByStrength(GameTestHelper helper) {
        CustomHero hero = CustomKitSupport.def().pick("thor.lightning", 10).pick("phoenix.burst", 1).pick("thor.body", 8)
                .pick("flight", 3).pick("spider.crawl", 5, "main")
                .key(1, "thor.lightning").sneak(5, "phoenix.burst").key(2, "flight").key(4, "spider.crawl").build();
        PowerData data = CustomKitSupport.data(hero);
        HeroKit kit = data.kit();
        check(data.has(HeroAbility.THOR_LIGHTNING) && !data.has(HeroAbility.THOR_HAMMER), "has what he picked only");
        check(data.acts(Power.THOR) && data.acts(Power.PHOENIX) && data.acts(Power.SPIDER_MAN) && !data.acts(Power.LOKI),
                "acts as the heroes he has abilities of");
        check(kit.sources().equals(EnumSet.of(Power.THOR, Power.PHOENIX, Power.SPIDER_MAN)), "sources: " + kit.sources());
        check(data.slot(HeroAbility.THOR_LIGHTNING) == 1 && data.slot(HeroAbility.PHOENIX_BURST) == 5
                && data.slot(HeroAbility.THOR_BODY) == 0 && data.slot(HeroAbility.THOR_HAMMER) == 0, "slots are his columns");
        data.startCooldown(HeroAbility.THOR_LIGHTNING, 100, 30);
        check(data.cooldownUntil(1) == 120, "strength 10 is x1.5: a 30-tick cooldown is 20: " + data.cooldownUntil(1));
        data.startCooldown(HeroAbility.PHOENIX_BURST, 100, 30);
        check(data.cooldownUntil(5) == 150, "strength 1 is x0.6: 30 ticks become 50: " + data.cooldownUntil(5));
        data.startCooldown(HeroAbility.THOR_BODY, 100, 30);
        data.startCooldown(HeroAbility.THOR_HAMMER, 100, 30);
        check(data.cooldownUntil(2) == 0 && data.cooldownUntil(3) == 0 && data.cooldownUntil(4) == 0, "an ability on no key starts nothing");
        check(data.isReady(HeroAbility.THOR_HAMMER, 0) && !data.isReady(HeroAbility.THOR_LIGHTNING, 119), "ready by ability");
        assertNear(data.bodyScale(HeroAbility.THOR_BODY), 1.3, 1.0E-4, "the body at strength 8");
        assertNear(kit.damageFactor(HeroAbility.THOR_LIGHTNING), 1.5, 1.0E-4, "the damage at strength 10");
        assertNear(kit.damageFactor(HeroAbility.SPIDER_CRAWL), 1.0, 1.0E-4, "nothing to scale: 1");
        check(kit.strength(HeroAbility.SPIDER_CRAWL) == HeroAbility.FIXED_STRENGTH, "a fixed row is at 5");
        check(kit.plain(1).ability() == HeroAbility.THOR_LIGHTNING && kit.sneak(1) == null && kit.binding(1, true) == kit.plain(1),
                "a free Sneak cell does what the key does");
        check(kit.sneak(5).ability() == HeroAbility.PHOENIX_BURST && !kit.sneak(5).isAlt() && kit.plain(5) == null, "a Sneak-only key");
        check(kit.defaultToggles().equals(EnumSet.of(Toggle.WALL_CRAWL)), "crawl starts on, its sense not (main only): " + kit.defaultToggles());
        check(HeroKit.of(CustomKitSupport.def().pick("tommy.speed", 5).pick("spider.crawl", 5).build()).defaultToggles()
                .equals(EnumSet.of(Toggle.SUPER_SPEED, Toggle.WALL_CRAWL, Toggle.SPIDER_SENSE)), "Tommy's run, the crawl and the sense");
        helper.succeed();
    }

    /** Becoming a hero puts its kit on the power data (by identity), with the toggles it starts with. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void becomingInstallsTheKitAndItsToggles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = CustomKitSupport.hero(f, 4, 4, CustomKitSupport.def().pick("tommy.speed", 5).key(2, "tommy.speed").build());
        PowerData data = Powers.get(p);
        check(data.kit() != null && data.kit().hero() == CustomHeroes.hero(p), "the kit of his definition");
        check(data.toggle(Toggle.SUPER_SPEED), "Tommy's run starts on");
        check(CustomHeroes.hero(p).binding(2, true).equals("tommy.speed#alt"), "Sneak + G holds his dash: " + CustomHeroes.hero(p).sneakKeys());
        CustomKitSupport.become(p, CustomKitSupport.def().pick("flight", 5).key(1, "flight").build());
        data = Powers.get(p);
        check(!data.has(HeroAbility.TOMMY_SPEED) && !data.toggle(Toggle.SUPER_SPEED), "a new definition: a new kit, its toggles reset");
        Powers.setPower(p, Power.THOR);
        check(Powers.get(p).kit() == null && Powers.get(p).has(HeroAbility.THOR_HAMMER), "Thor has no kit");
        helper.succeed();
    }

    // ---- the rules ----

    private static CustomHeroRules.Result judge(CustomKitSupport.Def def) {
        return CustomHeroRules.sanitize(def.raw(), CustomHeroRules.Limits.DEFAULT);
    }

    private static List<String> keysOf(CustomHeroRules.Result r) {
        return r.notes().stream().map(CustomHeroRules.Note::key).toList();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRulesKeepOneOwnerPerResourceAndWhatAnAbilityNeeds(GameTestHelper helper) {
        CustomHeroRules.Result clash = judge(CustomKitSupport.def().pick("wanda.chaos_shield", 5).pick("agatha.shield", 5)
                .pick("wanda.chaos_beam", 5).pick("telekinesis", 5).pick("magneto.metal", 5));
        CustomHero h = clash.hero();
        check(h.has(HeroAbility.WANDA_CHAOS_SHIELD) && !h.has(HeroAbility.AGATHA_SHIELD), "one shield: the first");
        check(h.has(HeroAbility.WANDA_CHAOS_BEAM), "abilities of one hero share");
        check(h.has(PowerModule.TELEKINESIS) && !h.has(HeroAbility.MAGNETO_METAL), "one grip: the generic one came first");
        check(keysOf(clash).stream().filter("message.enhanced.hero_creator.rule.resource"::equals).count() == 2, "noted: " + clash.notes());
        CustomHeroRules.Note note = clash.notes().stream().filter(n -> n.key().endsWith("rule.resource")).findFirst().orElseThrow();
        check(note.args()[0].equals(HeroAbility.AGATHA_SHIELD.nameKey()) && note.args()[1].equals(CustomHeroRules.resourceKey(Resource.SHIELD))
                && note.args()[2].equals(HeroAbility.WANDA_CHAOS_SHIELD.nameKey()), "naming both and what they share");
        check(judge(CustomKitSupport.def().pick("energy_blast", 5, "beam").pick("fire", 5, "flamethrower")).notes().isEmpty(),
                "the generic modules share as in 3.3");
        check(!judge(CustomKitSupport.def().pick("energy_blast", 5, "beam").pick("billy.hex_beam", 5)).hero().has(HeroAbility.BILLY_HEX_BEAM),
                "a generic beam and a hero's continuous beam do not");
        check(judge(CustomKitSupport.def().pick("energy_blast", 5, "bolts").pick("billy.hex_beam", 5)).notes().isEmpty(),
                "bolts drive no beam");

        CustomHeroRules.Result alone = judge(CustomKitSupport.def().pick("quicksilver.wall_run", 5));
        check(!alone.hero().has(HeroAbility.QUICKSILVER_WALL_RUN) && keysOf(alone).contains("message.enhanced.hero_creator.rule.requires"),
                "the wall run needs a speed");
        check(judge(CustomKitSupport.def().pick("quicksilver.time", 5).pick("quicksilver.wall_run", 5)).hero()
                .has(HeroAbility.QUICKSILVER_WALL_RUN), "with Quicksilver Time it stays");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theServerSwitchesHeroAbilitiesWorldPowersAndHeroesOff(GameTestHelper helper) {
        CustomKitSupport.Def def = CustomKitSupport.def().pick("thor.bifrost", 5).pick("thor.lightning", 5).pick("loki.daggers", 5)
                .pick("flight", 5);
        CustomHeroRules.Limits d = CustomHeroRules.Limits.DEFAULT;
        CustomHeroRules.Limits noWorld = new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of(), true, false, true, 0.5);
        CustomHeroRules.Result r = CustomHeroRules.sanitize(def.raw(), noWorld);
        check(!r.hero().has(HeroAbility.THOR_BIFROST) && r.hero().has(HeroAbility.THOR_LIGHTNING)
                && keysOf(r).contains("message.enhanced.hero_creator.rule.world"), "world powers off: the Bifrost goes");
        CustomHeroRules.Limits noHeroes = new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of(), false, true, true, 0.5);
        r = CustomHeroRules.sanitize(def.raw(), noHeroes);
        check(r.hero().powers().size() == 1 && r.hero().has(PowerModule.FLIGHT)
                && keysOf(r).stream().filter("message.enhanced.hero_creator.rule.hero_modules"::equals).count() == 3, "heroes' powers off");
        r = CustomHeroRules.sanitize(def.raw(), new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of("thor.*")));
        check(!r.hero().has(HeroAbility.THOR_BIFROST) && !r.hero().has(HeroAbility.THOR_LIGHTNING) && r.hero().has(HeroAbility.LOKI_DAGGERS),
                "thor.* disables all of Thor's");
        r = CustomHeroRules.sanitize(def.raw(), new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of("thor.lightning")));
        check(r.hero().has(HeroAbility.THOR_BIFROST) && !r.hero().has(HeroAbility.THOR_LIGHTNING), "one ability by its id");
        check(CustomHeroRules.sanitize(def.raw(), d).notes().isEmpty(), "and by default all are allowed");
        CustomHero fixed = CustomHeroRules.sanitize(CustomKitSupport.def().pick("vision.disguise", 9).pick("thor.lightning", 0).raw(), d).hero();
        check(fixed.pick(HeroAbility.VISION_DISGUISE).strength() == 5 && fixed.pick(HeroAbility.THOR_LIGHTNING).strength() == 1,
                "a row with nothing to scale is at 5, the rest clamped");
        check(fixed.pick(HeroAbility.THOR_LIGHTNING).option().equals(HeroAbility.BOTH), "a hero row's option is both by default");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sneakCellsFollowTheRules(GameTestHelper helper) {
        // A variant is forced onto the Sneak cell of its plain key.
        CustomHero h = judge(CustomKitSupport.def().pick("thanos.power", 5).key(3, "thanos.power").sneak(3, "flight")).hero();
        check(h.binding(3, true).equals("thanos.power#alt"), "forced: " + h.sneakKeys());
        // ... unless it is switched off: then the cell is free for another.
        h = judge(CustomKitSupport.def().pick("thanos.power", 5, "main").pick("flight", 5).key(3, "thanos.power").sneak(3, "flight")).hero();
        check(h.binding(3, true).equals("flight"), "main only: Sneak + V may hold another: " + h.sneakKeys());
        // A hero ability without its variant may sit on a Sneak cell; one with it on may not.
        h = judge(CustomKitSupport.def().pick("thanos.power", 5, "main").pick("cyclops.leap", 5).sneak(1, "thanos.power").sneak(2, "cyclops.leap"))
                .hero();
        check(h.binding(1, true).equals("thanos.power") && h.binding(2, true).equals("cyclops.leap"), "on Sneak cells: " + h.sneakKeys());
        h = judge(CustomKitSupport.def().pick("thanos.power", 5).sneak(1, "thanos.power")).hero();
        check(h.binding(1, true).isEmpty(), "a variant on could go nowhere: cleared");
        // Another press option of the module on the plain key: yes; a held one, its own one, another key's module: no.
        CustomKitSupport.Def gen = CustomKitSupport.def().pick("energy_blast", 5, "bolts").pick("fire", 5, "fireball")
                .key(1, "energy_blast").key(2, "fire");
        check(judge(gen.sneak(1, "energy_blast#burst")).hero().binding(1, true).equals("energy_blast#burst"), "burst on Sneak + R");
        check(judge(gen.sneak(1, "energy_blast#beam")).hero().binding(1, true).isEmpty(), "a held option is not a variant");
        check(judge(gen.sneak(1, "energy_blast#bolts")).hero().binding(1, true).isEmpty(), "nor its own option");
        check(judge(gen.sneak(1, "fire#flamethrower")).hero().binding(1, true).isEmpty(), "nor another key's module");
        CustomHeroRules.Result twice = judge(gen.sneak(1, "fire"));
        check(twice.hero().binding(1, true).isEmpty() && keysOf(twice).contains("message.enhanced.hero_creator.rule.key"),
                "an action is on one cell only (fire is on G)");
        check(judge(CustomKitSupport.def().pick("thor.body", 5).key(1, "thor.body").sneak(2, "thor.body")).hero().keys().get(0).isEmpty(),
                "a body takes no key");
        check(judge(CustomKitSupport.def().pick("cyclops.leap", 5).key(1, "cyclops.leap#alt")).hero().keys().get(0).isEmpty(),
                "a plain key holds no variant");
        // A generic variant is paid for: half its module, rounded up.
        CustomHero paid = judge(CustomKitSupport.def().pick("energy_blast", 5, "bolts").key(1, "energy_blast").sneak(1, "energy_blast#burst"))
                .hero();
        check(CustomHeroRules.cost(paid) == 10 + 5 && CustomHeroRules.cost(paid.powers()) == 10, "cost with the variant: " + CustomHeroRules.cost(paid));
        CustomHeroRules.Result budget = CustomHeroRules.sanitize(CustomKitSupport.def().pick("energy_blast", 5, "bolts").pick("flight", 2)
                .key(1, "energy_blast").sneak(1, "energy_blast#burst").raw(), CustomHeroRules.Limits.DEFAULT.withBudget(16));
        check(!budget.hero().has(PowerModule.FLIGHT) && budget.hero().binding(1, true).equals("energy_blast#burst"),
                "the variant counts in the budget: 10 + 5 + 4 > 16 drops the last pick: " + budget.hero().powers());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void movesStancesAnimationAndLayersAreChecked(GameTestHelper helper) {
        CustomHero in = CustomKitSupport.def().pick("thor.lightning", 5).pick("thor.thunder", 5).pick("loki.daggers", 5)
                .pick("energy_blast", 5, "beam")
                .with(h -> h.withMove(1, false, "billy_bolt").withMove(2, true, "dance").withMove(3, false, "none")
                        .withAnim(new CustomHero.Anim("thor", "moonwalk", "tommy", "regal"))
                        .withLayers(new CustomHero.Layers(new CustomHero.Layer("thor", -1, 0, "always"), new CustomHero.Layer("tophat", -1, 0, "always"),
                                CustomHero.Layer.NONE_LAYER, new CustomHero.Layer("glow", 0x7F123456, 9, "sometimes"),
                                new CustomHero.Layer("flames", -1, 0, "powers"))))
                .raw();
        List<CustomHero.Pick> picks = List.of(new CustomHero.Pick("thor.lightning", 5, "both", -1, "thor_beam"),
                new CustomHero.Pick("thor.thunder", 5, "both", -1, "beam_hand"), new CustomHero.Pick("loki.daggers", 5, "both", -1, "mag_field"),
                new CustomHero.Pick("energy_blast", 5, "beam", -1, "shield_wanda"));
        CustomHeroRules.Result r = CustomHeroRules.sanitize(in.withPowers(picks), CustomHeroRules.Limits.DEFAULT);
        CustomHero h = r.hero();
        check(h.move(1, false).equals("billy_bolt") && h.move(2, true).equals(CustomHero.AUTO) && h.move(3, false).equals(CustomHero.NONE),
                "moves: known, unknown to auto, none: " + h.moves());
        check(h.pick(HeroAbility.THOR_LIGHTNING).stance().equals("thor_beam"), "a hold stance on a held ability");
        check(h.pick(HeroAbility.THOR_THUNDER).stance().equals(CustomHero.AUTO), "a hold stance on a switch: auto");
        check(h.pick(HeroAbility.LOKI_DAGGERS).stance().equals(CustomHero.AUTO), "a press has no stance");
        check(h.pick(PowerModule.ENERGY_BLAST).stance().equals("shield_wanda"), "a generic held beam may take a hold stance");
        check(h.anim().equals(new CustomHero.Anim("thor", CustomHero.AUTO, "tommy", "regal")), "animation: " + h.anim());
        check(h.layers().cape().style().equals("thor") && h.layers().hat().isNone(), "a known cape stays, an unknown hat goes");
        check(h.layers().eyes().color() == 0x123456 && h.layers().eyes().row() == 3 && h.layers().eyes().when().equals("always"),
                "eyes: opaque colour, row clamped, unknown when to always: " + h.layers().eyes());
        check(h.layers().aura().when().equals("powers"), "a known when stays");
        check(keysOf(r).equals(List.of("message.enhanced.hero_creator.rule.layer")), "only the layer was noted: " + r.notes());
        CustomHeroRules.Limits noLayers = new CustomHeroRules.Limits(90, 10, 10, 16384, Set.of(), true, true, false, 0.5);
        CustomHeroRules.Result off = CustomHeroRules.sanitize(h, noLayers);
        check(off.hero().layers().allNone() && keysOf(off).size() == 3, "layers off: all three go, each noted: " + off.notes());
        check(CustomHeroRules.sanitize(off.hero(), CustomHeroRules.Limits.DEFAULT).notes().isEmpty(), "a judged hero is judged clean");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCataloguesNameRealGesturesStancesAndLooks(GameTestHelper helper) {
        for (String g : AnimCatalog.GESTURES) {
            check(GestureId.valueOf(g.toUpperCase()) != null, "gesture " + g);
        }
        for (String s : AnimCatalog.STANCES_HOLD) {
            check(Stance.valueOf(s.toUpperCase()) != null, "stance " + s);
        }
        for (String s : AnimCatalog.STANCES_TOGGLE) {
            check(Stance.valueOf(s.toUpperCase()) != null, "stance " + s);
        }
        for (int kind = 0; kind < CustomHero.Layers.KINDS.size(); kind++) {
            check(LookCatalog.styles(kind).get(0).equals(CustomHero.NONE), "every layer can be none");
        }
        helper.succeed();
    }

    // ---- the roles ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRolesOfTheHeroesAreTheOldListsAndAKitsArePicked(GameTestHelper helper) {
        // (3.6: Doctor Strange is a sorcerer too.)
        Set<Power> sorcerers = EnumSet.of(Power.LOKI, Power.BILLY, Power.SCARLET_WITCH, Power.AGATHA,
                Power.DOCTOR_STRANGE);
        Set<Power> strong = EnumSet.of(Power.SCARLET_WITCH, Power.PHOENIX, Power.BILLY);
        for (Power p : Power.values()) {
            PowerData data = CustomKitSupport.data(p);
            check(Traits.has(data, Trait.SORCERER) == sorcerers.contains(p) && WardStrength.isSorcerer(p) == sorcerers.contains(p),
                    p + " sorcerer");
            Telepathy.Rank rank = Telepathy.rank(p);
            check(Traits.has(data, Trait.TELEPATH_STRONG) == (rank == Telepathy.Rank.STRONGER) && strong.contains(p) == (rank == Telepathy.Rank.STRONGER)
                    && Traits.has(data, Trait.TELEPATH) == (rank == Telepathy.Rank.TELEPATH), p + " telepath");
            // (3.8: Captain America lifted Mjolnir in Endgame; Love is Thor's daughter.)
            check(Traits.has(data, Trait.WORTHY) == (p == Power.THOR || p == Power.VISION || p == Power.CAPTAIN_AMERICA
                    || p == Power.LOVE), p + " worthy");
            // (3.7: Wolverine's adamantium bones are metal for Magneto.)
            check(Traits.has(data, Trait.METAL_BODY) == (p == Power.VISION || p == Power.ULTRON
                    || p == Power.WOLVERINE), p + " metal");
            check(Traits.has(data, Trait.DARK_MAGIC) == (p == Power.AGATHA), p + " dark magic");
            check(!Traits.has(data, Trait.IRON_LEGS), p + ": the Iron Spider's legs go with his look");
        }
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer witch = CustomKitSupport.hero(f, 2, 2, CustomKitSupport.def().pick("agatha.drain", 5).build());
        check(Sorcery.isSorcerer(witch) && WardStrength.isSorcerer(witch) && DarkMagic.casts(witch), "Agatha's drain: a dark sorcerer");
        ServerPlayer mage = CustomKitSupport.hero(f, 4, 2, CustomKitSupport.def().pick("magic", 5).pick("telepathy", 5).build());
        check(Sorcery.isSorcerer(mage) && !DarkMagic.casts(mage) && Telepathy.rank(mage) == Telepathy.Rank.TELEPATH, "Magic and Telepathy");
        ServerPlayer jean = CustomKitSupport.hero(f, 6, 2, CustomKitSupport.def().pick("phoenix.mind", 5).pick("xavier.mental_blast", 5).build());
        check(Telepathy.rank(jean) == Telepathy.Rank.STRONGER, "the stronger rank wins");
        ServerPlayer god = CustomKitSupport.hero(f, 8, 2, CustomKitSupport.def().pick("thor.hammer", 5).pick("ultron.body", 5).build());
        check(Worthiness.isWorthy(god) && Metal.isMetalBody(god) && !SpiderServer.isIronSpider(god), "worthy and metal");
        ServerPlayer legs = CustomKitSupport.hero(f, 8, 6, CustomKitSupport.def().pick("spider.legs", 5).build());
        check(SpiderServer.isIronSpider(legs) && Metal.isMetalBody(legs) && !Worthiness.isWorthy(legs), "the Iron Spider's legs (so metal)");
        ServerPlayer plain = CustomKitSupport.hero(f, 10, 2, CustomKitSupport.def().pick("flight", 5).build());
        check(!Sorcery.isSorcerer(plain) && Telepathy.rank(plain) == Telepathy.Rank.NONE && !Worthiness.isWorthy(plain)
                && !Metal.isMetalBody(plain) && !SpiderServer.isIronSpider(plain) && !DarkMagic.casts(plain), "a flier is none of them");
        helper.succeed();
    }

    // ---- the strength in the damage ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aHeroAbilitysDamageFollowsItsStrength(GameTestHelper helper) {
        for (int[] sf : new int[][]{{1, 60}, {5, 100}, {10, 150}}) {
            HeroKit kit = HeroKit.of(CustomKitSupport.def().pick("thor.lightning", sf[0]).build());
            assertNear(HeroDamage.factor(ModDamageTypes.LIGHTNING_BEAM, kit), sf[1] / 100.0, 1.0E-4, "lightning at strength " + sf[0]);
            assertNear(HeroDamage.factor(ModDamageTypes.CHAOS_BEAM, kit), 1.0, 1.0E-4, "a type of no ability of his");
        }
        HeroKit thanos = HeroKit.of(CustomKitSupport.def().pick("thanos.power", 2).pick("thanos.soul", 9).build());
        check(HeroDamage.abilityOf(ModDamageTypes.STONE_POWER, thanos) == HeroAbility.THANOS_SOUL, "the stones: his strongest");

        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer strong = CustomKitSupport.hero(f, 3, 3, CustomKitSupport.def().pick("thor.lightning", 10).build());
        ServerPlayer weak = CustomKitSupport.hero(f, 3, 9, CustomKitSupport.def().pick("thor.lightning", 1).build());
        Zombie a = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(8, FLOOR, 3));
        Zombie b = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(8, FLOOR, 9));
        a.setNoAi(true);
        b.setNoAi(true);
        a.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.LIGHTNING_BEAM, strong, strong), 6.0F);
        b.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.LIGHTNING_BEAM, weak, weak), 6.0F);
        float lostA = a.getMaxHealth() - a.getHealth();
        float lostB = b.getMaxHealth() - b.getHealth();
        check(lostB > 0 && lostA > 2.2F * lostB, "strength 10 hits 2.5 times as hard as strength 1: " + lostA + " vs " + lostB);
        helper.succeed();
    }

    // ---- the words ----

    private static JsonObject lang(String name) throws IOException {
        Path path = Platform.get().modResource("assets/" + Enhanced.MODID + "/lang/" + name + ".json").orElseThrow();
        try (InputStream in = Files.newInputStream(path)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theNewNotesHaveTheirWordsInBothLanguages(GameTestHelper helper) throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (String rule : List.of("resource", "requires", "world", "hero_modules", "layer")) {
            keys.add("message.enhanced.hero_creator.rule." + rule);
        }
        for (Resource r : Resource.values()) {
            keys.add(CustomHeroRules.resourceKey(r));
        }
        for (String kind : CustomHero.Layers.KINDS) {
            keys.add("message.enhanced.hero_creator.layer." + kind);
        }
        for (String type : List.of("hero_magic", "hero_mind")) {
            keys.add("death.attack.enhanced." + type);
            keys.add("death.attack.enhanced." + type + ".player");
        }
        for (String language : List.of("en_us", "he_il")) {
            JsonObject l = lang(language);
            for (String key : keys) {
                if (!l.has(key)) {
                    missing.add(language + ": " + key);
                }
            }
        }
        check(missing.isEmpty(), "missing words: " + missing);
        helper.succeed();
    }

    // ---- the key router (its own batch: it puts a spy in place of Cyclops's server for a moment) ----

    /** Records the keys that reach "Cyclops"; his concussion (key 2) starts its cooldown by ability, the rest change nothing. */
    private static final class Spy implements HeroServer {
        final List<String> calls = new ArrayList<>();

        @Override
        public Power power() {
            return Power.CYCLOPS;
        }

        @Override
        public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
            calls.add(ability + (down ? " down" : " up") + (alt ? " alt" : ""));
            if (down && ability == 2) {
                data.startCooldown(HeroAbility.CYCLOPS_CONCUSSION, player.serverLevel().getGameTime(), 40);
            }
        }
    }

    @EnhancedTest(template = ARENA, batch = "custom_v2_router")
    public static void theRouterTakesSneakCellsReleasesWhatWentDownAndMarksCasts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Spy spy = new Spy();
        HeroServer real = HeroServers.register(spy);
        f.onClose(() -> HeroServers.register(real));
        ServerPlayer p = CustomKitSupport.hero(f, 5, 5, CustomKitSupport.def()
                .pick("cyclops.optic_blast", 5).pick("cyclops.concussion", 5).pick("cyclops.leap", 5).pick("energy_blast", 5, "bolts")
                .key(1, "cyclops.optic_blast").key(3, "cyclops.leap").sneak(4, "cyclops.concussion")
                .key(5, "energy_blast").sneak(5, "energy_blast#burst").build());
        CustomState state = CustomHeroes.state(p);
        PowerData data = Powers.get(p);
        int seq = state.castSeq();

        CustomKitSupport.press(p, 1, false, true);
        CustomKitSupport.press(p, 1, false, false);
        CustomKitSupport.press(p, 1, true, true);
        CustomKitSupport.press(p, 1, false, false);
        check(spy.calls.equals(List.of("1 down", "1 up", "1 down alt", "1 up alt")),
                "R plain, then Sneak + R (the blast's variant), released after Sneak: still the variant: " + spy.calls);
        check(state.castSeq() == seq, "keys that changed nothing mark no cast");

        spy.calls.clear();
        long now = p.serverLevel().getGameTime();
        CustomKitSupport.press(p, 4, true, true);
        check(spy.calls.equals(List.of("2 down")), "Sneak + B is the concussion (its home key 2): " + spy.calls);
        check(data.cooldownUntil(4) == now + 40 && data.cooldownUntil(2) == 0, "its cooldown on B, his column, not on G");
        check(state.castSeq() == ((seq + 1) & 0xFF) && state.castColumn() == 4 && state.castSneak(), "a cast of Sneak + B");
        CustomKitSupport.press(p, 4, false, false);

        spy.calls.clear();
        CustomKitSupport.press(p, 2, true, true);
        CustomKitSupport.press(p, 2, true, false);
        check(spy.calls.isEmpty(), "G is free, plain and with Sneak");
        CustomKitSupport.tap(p, 3, true);
        check(spy.calls.equals(List.of("3 down", "3 up")), "a free Sneak cell does what its key does: " + spy.calls);
        check(state.castSeq() == ((seq + 1) & 0xFF), "still one cast");

        CustomKitSupport.tap(p, 5, true);
        check(data.cooldownUntil(5) > now, "Sneak + N fires the burst (its cooldown on N)");
        check(state.castSeq() == ((seq + 2) & 0xFF) && state.castColumn() == 5 && state.castSneak(), "and marks its cast");
        helper.succeed();
    }

    private CustomV2Tests() {
    }
}
