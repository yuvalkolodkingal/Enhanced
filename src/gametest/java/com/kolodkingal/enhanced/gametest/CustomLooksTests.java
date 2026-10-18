package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CatalogSearch;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroItems;
import com.kolodkingal.enhanced.custom.HeroPresets;
import com.kolodkingal.enhanced.custom.KeyLayout;
import com.kolodkingal.enhanced.custom.LookCatalog;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The looks and the editor of 3.4 (lane L6): every style of the five layers has its art, the layers' rules, the 22
 * presets (they read, fit 60 points with no note, use only their hero's abilities, fly in a style, have their words), the
 * search of the Powers tab in English and Hebrew, the Keys tab's Auto layout, the words of the moves and looks; and (R8)
 * "Choose a hero" giving the hero's own items once.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CustomLooksTests {
    private static final String BATCH = "custom_looks";
    private static final String ITEMS = "hero_items";
    private static final String ITEMS_CONFIG = "hero_items_config";

    private static IModFile mod() {
        return ModList.get().getModFileById(Enhanced.MODID).getFile();
    }

    private static Path resource(String path) {
        List<String> parts = new ArrayList<>(List.of("assets", Enhanced.MODID));
        parts.addAll(List.of(path.split("/")));
        return mod().findResource(parts.toArray(new String[0]));
    }

    private static String text(String path) throws IOException {
        try (InputStream in = Files.newInputStream(resource(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject lang(String name) throws IOException {
        try (InputStream in = Files.newInputStream(resource("lang/" + name + ".json"))) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Function<String, String> words(JsonObject lang) {
        return key -> lang.has(key) ? lang.get(key).getAsString() : key;
    }

    // ---- the layers ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyStyleOfEveryLayerHasItsArt(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        int seen = 0;
        for (int kind = 0; kind < CustomHero.Layers.KINDS.size(); kind++) {
            for (String style : LookCatalog.styles(kind)) {
                for (String path : LookCatalog.textures(kind, style)) {
                    seen++;
                    if (!Files.exists(resource(path))) {
                        missing.add(style + ": " + path);
                    }
                }
                boolean geometry = kind == 1 && style.equals("billy_crown");
                check(CustomHero.NONE.equals(style) || geometry || !LookCatalog.textures(kind, style).isEmpty(), "a style has art: " + style);
            }
        }
        check(seen > 30 && missing.isEmpty(), "every texture of a style is there (" + seen + "): " + missing);
        check(LookCatalog.textures(3, "glow").size() == LookCatalog.EYE_ROWS, "one eye mask per row of the face");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theLayersAreJudgedByTheRules(GameTestHelper helper) {
        CustomHero.Layers asked = CustomHero.Layers.NONE_ALL.with(0, new CustomHero.Layer("plain", 0x123456, 3, "flying"))
                .with(1, new CustomHero.Layer("a_crown_nobody_made", -1, 0, "always"))
                .with(3, new CustomHero.Layer("glow", 0x1200FF00, 9, "sometimes"))
                .with(4, new CustomHero.Layer("flames", -1, 0, "powers"));
        CustomHero hero = CustomKitSupport.def().pick("flight", 3).build().withLayers(asked);
        CustomHeroRules.Result r = CustomHeroRules.sanitize(hero, CustomHeroRules.Limits.DEFAULT);
        CustomHero.Layers got = r.hero().layers();
        check(got.cape().style().equals("plain") && got.cape().color() == 0x123456, "the cape keeps its colour");
        check(got.cape().row() == 0 && got.cape().when().equals("always"), "a cape has no row and no 'when'");
        check(got.hat().isNone(), "an unknown hat is taken off");
        check(got.eyes().row() == LookCatalog.EYE_ROWS - 1 && got.eyes().color() == 0x00FF00 && got.eyes().when().equals("always"),
                "the eyes' row is kept on the face, the colour made opaque RGB, an unknown 'when' always: " + got.eyes());
        check(got.aura().when().equals("powers") && got.aura().color() == -1, "the aura while a power is on, in the hero's colour");
        check(r.notes().size() == 1 && r.notes().get(0).key().equals("message.enhanced.hero_creator.rule.layer"), "one note: " + r.notes());
        CustomHeroRules.Limits off = new CustomHeroRules.Limits(90, 10, 10, 16384, java.util.Set.of(), true, true, false, 0.5);
        check(CustomHeroRules.sanitize(hero, off).hero().layers().allNone(), "no layers where the server says so");
        check(LookCatalog.eyeColor("glow") < 0 && LookCatalog.eyeColor("black") == 0 && LookCatalog.eyeColor("lightning") > 0,
                "glowing eyes take the colour, the others have their own");
        for (String tinted : LookCatalog.TINTED) {
            boolean known = false;
            for (int kind = 0; kind < 5; kind++) {
                known |= LookCatalog.styles(kind).contains(tinted);
            }
            check(known, "a tinted style is a style: " + tinted);
        }
        helper.succeed();
    }

    // ---- the presets ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyPresetFitsSixtyPointsWithNoNote(GameTestHelper helper) throws IOException {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> ids = HeroPresets.ids();
        check(ids.size() == 22 && HeroPresets.HEROES.size() == 16 && HeroPresets.ARCHETYPES.size() == 6, "22 presets: " + ids);
        CustomHeroRules.Limits sixty = CustomHeroRules.Limits.DEFAULT.withBudget(HeroPresets.MAX_POINTS);
        for (String id : ids) {
            HeroPresets.Preset preset = HeroPresets.read(id, text(HeroPresets.path(id)));
            check(preset != null, "the preset reads: " + id);
            CustomHero hero = preset.hero();
            CustomHeroRules.Result r = CustomHeroRules.sanitize(hero, sixty);
            check(r.notes().isEmpty(), id + " passes the rules at 60 points with no note: " + r.notes());
            check(CustomHeroRules.cost(r.hero()) <= HeroPresets.MAX_POINTS && !r.hero().powers().isEmpty(),
                    id + " costs at most 60: " + CustomHeroRules.cost(r.hero()));
            boolean isHero = HeroPresets.HEROES.contains(id);
            check(isHero == (preset.like() != Power.NONE), id + " is like a hero only if it is a hero's preset");
            for (CustomHero.Pick pick : hero.powers()) {
                if (isHero && pick.ability() != null) {
                    check(pick.ability().hero == preset.like(), id + " uses only its hero's abilities: " + pick.id());
                }
            }
            if (isHero) {
                check(hero.powers().stream().anyMatch(p -> p.ability() != null), id + " has its hero's abilities");
                check(!HeroAbility.of(preset.like()).isEmpty(), id + "'s hero has abilities");
            } else {
                check(!preset.icon().isEmpty(), id + " has a picture");
            }
            if (hero.has(PowerModule.FLIGHT)) {
                check(!CustomHero.AUTO.equals(hero.anim().flight()), id + " flies in a style");
            }
            for (String key : List.of(preset.nameKey(), preset.descKey())) {
                check(en.has(key) && he.has(key), "the words of " + key);
            }
            for (int kind = 0; kind < 5; kind++) {
                CustomHero.Layer layer = r.hero().layers().get(kind);
                check(layer.isNone() || LookCatalog.styles(kind).contains(layer.style()), id + "'s layers are known");
            }
        }
        // The appendix's own examples.
        HeroPresets.Preset thor = HeroPresets.read("thor", text(HeroPresets.path("thor")));
        check(thor.hero().layers().cape().style().equals("thor") && thor.hero().anim().flight().equals("thor"), "Thor-like: his cape, his flight");
        HeroPresets.Preset agatha = HeroPresets.read("agatha", text(HeroPresets.path("agatha")));
        check(agatha.hero().layers().hat().style().equals("witch_hat"), "Agatha-like: the witch's hat");
        HeroPresets.Preset cyclops = HeroPresets.read("cyclops", text(HeroPresets.path("cyclops")));
        check(cyclops.hero().layers().visor().style().equals("cyclops_classic"), "Cyclops-like: his visor");
        HeroPresets.Preset phoenix = HeroPresets.read("phoenix", text(HeroPresets.path("phoenix")));
        check(phoenix.hero().layers().aura().style().equals("flames") && phoenix.hero().layers().aura().when().equals("powers"),
                "Phoenix-like: flames while a power is on");
        helper.succeed();
    }

    // ---- the search and the Auto keys ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSearchFindsPowersInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        Function<String, String> en = words(lang("en_us"));
        Function<String, String> he = words(lang("he_il"));
        check(CatalogSearch.matches(PowerModule.FLIGHT, "", en), "an empty search finds everything");
        check(CatalogSearch.matches(PowerModule.FLIGHT, "FLI", en), "by a part of the English name, any case");
        check(CatalogSearch.matches(PowerModule.FLIGHT, "wings", en), "by an option's name");
        check(!CatalogSearch.matches(PowerModule.FLIGHT, "lightning bolt", en), "every word must be found");
        String heName = he.apply(PowerModule.TELEPORT.nameKey());
        check(CatalogSearch.matches(PowerModule.TELEPORT, heName, he), "by its Hebrew name: " + heName);
        check(CatalogSearch.matches(PowerModule.TELEPORT, heName.substring(0, Math.min(3, heName.length())), he), "by the start of it");
        check(!CatalogSearch.matches(PowerModule.FLIGHT, heName, he), "and not another power by it");
        check(CatalogSearch.matches(HeroAbility.THOR_LIGHTNING, "thor", he), "a hero's ability by its id, in Hebrew too");
        check(CatalogSearch.matches(HeroAbility.THOR_LIGHTNING, he.apply("power.enhanced.thor"), he), "and by its hero's Hebrew name");
        check(CatalogSearch.fold("מלך").equals(CatalogSearch.fold("מלכ")) && CatalogSearch.fold("Éclair").equals("eclair"),
                "Hebrew end letters and accents do not matter");
        int found = 0;
        for (CatalogEntry e : com.kolodkingal.enhanced.custom.Catalog.all()) {
            if (CatalogSearch.matches(e, "thor", en)) {
                found++;
            }
        }
        check(found >= HeroAbility.of(Power.THOR).size(), "every ability of Thor is found by his name: " + found);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void autoPutsTheHeroesAbilitiesOnTheirOwnKeys(GameTestHelper helper) {
        CustomHero raw = CustomKitSupport.def().raw().withPowers(List.of(new CustomHero.Pick("thor.lightning", 5, "both", -1),
                new CustomHero.Pick("thor.hammer", 5, "both", -1), new CustomHero.Pick("flight", 5, "jet", -1),
                new CustomHero.Pick("energy_blast", 4, "beam", -1), new CustomHero.Pick("durability", 4, "armor", -1),
                new CustomHero.Pick("magneto.shield", 4, "both", -1), new CustomHero.Pick("wanda.chaos_beam", 3, "both", -1)));
        CustomHero laid = KeyLayout.auto(raw, CustomHeroRules.Limits.DEFAULT);
        check(laid.binding(1, false).equals("thor.hammer") && laid.binding(1, true).equals("thor.hammer#alt"),
                "Thor's hammer on R, its Sneak move beside it: " + laid.keys() + " " + laid.sneakKeys());
        check(laid.binding(3, false).equals("thor.lightning") && laid.binding(4, false).equals("magneto.shield"), "the others on their own keys");
        check(laid.binding(2, false).equals("wanda.chaos_beam") || laid.binding(2, false).equals("flight"), "the rest after them");
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            check(!laid.binding(c, false).equals("durability") && !laid.binding(c, true).equals("durability"), "an always-on power takes no key");
        }
        CustomHeroRules.Result r = CustomHeroRules.sanitize(laid, CustomHeroRules.Limits.DEFAULT);
        check(r.notes().isEmpty(), "the layout passes the rules: " + r.notes());
        check(r.hero().keys().equals(laid.keys()) && r.hero().sneakKeys().equals(laid.sneakKeys()), "and the rules change nothing of it");
        check(laid.powers().equals(raw.powers()), "Auto changes only the keys");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void buildMagicTakesNoKeyBox(GameTestHelper helper) {
        // Billy's Build Magic is used with its own Y key: no box holds it, by Auto or by hand (its box would do nothing).
        CustomHero raw = CustomKitSupport.def().raw().withPowers(List.of(new CustomHero.Pick("billy.build_magic", 5, "both", -1),
                new CustomHero.Pick("energy_blast", 4, "beam", -1)));
        check(!HeroAbility.BILLY_BUILD_MAGIC.takesBox() && HeroAbility.THOR_LIGHTNING.takesBox(), "Build Magic is no box's");
        CustomHero laid = KeyLayout.auto(raw, CustomHeroRules.Limits.DEFAULT);
        for (int c = 1; c <= CustomHero.KEYS; c++) {
            check(!laid.binding(c, false).startsWith("billy.build_magic") && !laid.binding(c, true).startsWith("billy.build_magic"),
                    "Auto leaves Build Magic off the boxes: " + laid.keys() + " " + laid.sneakKeys());
        }
        check(laid.binding(1, false).equals("energy_blast"), "and R is the Energy power's: " + laid.keys());
        CustomHero byHand = raw.withKey(2, "billy.build_magic").withBinding(3, true, "billy.build_magic");
        CustomHeroRules.Result r = CustomHeroRules.sanitize(byHand, CustomHeroRules.Limits.DEFAULT);
        check(r.hero().binding(2, false).isEmpty() && r.hero().binding(3, true).isEmpty(),
                "the rules take Build Magic off a box put by hand: " + r.hero().keys() + " " + r.hero().sneakKeys());
        check(r.notes().stream().filter(n -> n.key().equals("message.enhanced.hero_creator.rule.key")).count() == 2,
                "and say so for both boxes: " + r.notes());
        check(r.hero().has(HeroAbility.BILLY_BUILD_MAGIC), "while the power stays picked (Y)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theMovesAndLooksHaveTheirWords(GameTestHelper helper) throws IOException {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> keys = new ArrayList<>();
        AnimCatalog.GESTURES.forEach(g -> keys.add("anim.enhanced.gesture." + g));
        AnimCatalog.STANCES_HOLD.forEach(s -> keys.add("anim.enhanced.stance." + s));
        AnimCatalog.STANCES_TOGGLE.forEach(s -> keys.add("anim.enhanced.stance." + s));
        AnimCatalog.FLIGHT_STYLES.forEach(s -> keys.add("anim.enhanced.flight." + s));
        AnimCatalog.RUN_CHARACTERS.forEach(s -> keys.add("anim.enhanced.run." + s));
        AnimCatalog.SPEED_RUNS.forEach(s -> keys.add("anim.enhanced.speed_run." + s));
        AnimCatalog.IDLES.forEach(s -> keys.add("anim.enhanced.idle." + s));
        for (int kind = 0; kind < 5; kind++) {
            for (String style : LookCatalog.styles(kind)) {
                keys.add("look.enhanced." + CustomHero.Layers.KINDS.get(kind) + "." + style);
            }
        }
        for (CatalogEntry.Family f : CatalogEntry.Family.values()) {
            keys.add("family.enhanced." + f.name().toLowerCase(java.util.Locale.ROOT));
        }
        keys.add("hud.enhanced.custom.sneak");
        List<String> missing = keys.stream().filter(k -> !en.has(k) || !he.has(k)).toList();
        check(missing.isEmpty(), "every move, stance, style and family has its words in both languages: " + missing);
        // Numbers in Hebrew are whole sentences ("45 מתוך 90"), never "45 / 90" turned round.
        check(he.get("screen.enhanced.hero_creator.budget").getAsString().contains("מתוך"), "the budget in Hebrew");
        helper.succeed();
    }

    // ---- R8: "Choose a hero" gives the hero's own items ----

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                n += player.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    @GameTest(template = ARENA, batch = ITEMS)
    public static void choosingThorGivesHisHammerClaimedOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(5.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.choose(thor, Power.THOR, "thor"), "chosen");
        check(count(thor, ModItems.MJOLNIR.get()) == 1 && count(thor, ModItems.STORMBREAKER.get()) == 0, "Mjolnir, once");
        ItemStack hammer = ItemStack.EMPTY;
        for (int i = 0; i < thor.getInventory().getContainerSize(); i++) {
            if (thor.getInventory().getItem(i).is(ModItems.MJOLNIR.get())) {
                hammer = thor.getInventory().getItem(i);
            }
        }
        MjolnirOwner owner = hammer.get(ModDataComponents.OWNER.get());
        check(owner != null && owner.is(thor), "claimed as his");
        check(HeroItems.give(thor, Power.THOR, "thor").isEmpty() && count(thor, ModItems.MJOLNIR.get()) == 1, "never a second one");
        ServerPlayer ragnarok = f.player(9.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.choose(ragnarok, Power.THOR, "ragnarok"), "chosen in Ragnarok's look");
        check(count(ragnarok, ModItems.STORMBREAKER.get()) == 1 && count(ragnarok, ModItems.MJOLNIR.get()) == 0, "Stormbreaker in that look");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = ITEMS)
    public static void theHeadItemsArePutOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cyclops = f.player(5.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.choose(cyclops, Power.CYCLOPS, ""), "chosen");
        check(cyclops.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RUBY_QUARTZ_GLASSES.get()), "Cyclops wears his glasses");
        ServerPlayer magneto = f.player(9.5, FLOOR, 5.5, GameType.CREATIVE);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        check(CustomHeroes.choose(magneto, Power.MAGNETO, ""), "chosen");
        check(magneto.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && count(magneto, ModItems.MAGNETO_HELMET.get()) == 1,
                "a head already covered keeps what it wears; the helmet goes in the bag");
        ServerPlayer loki = f.player(13.5, FLOOR, 5.5, GameType.CREATIVE);
        loki.getInventory().add(new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        check(CustomHeroes.choose(loki, Power.LOKI, ""), "chosen");
        check(count(loki, ModItems.LOKIS_SCEPTER.get()) == 1, "a Scepter he has is not given again");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = ITEMS)
    public static void theTableOfItemsFollowsHowEachHeroAwakens(GameTestHelper helper) {
        check(HeroItems.of(Power.THANOS, "").equals(List.of(ModItems.INFINITY_GAUNTLET.get())), "Thanos: the Gauntlet");
        check(HeroItems.of(Power.ULTRON, "").equals(List.of(ModItems.ULTRON_CORE.get())), "Ultron: his Core");
        check(HeroItems.of(Power.PROFESSOR_X, "old").equals(List.of(ModItems.XAVIER_WHEELCHAIR.get())), "Professor X: the wheelchair");
        check(HeroItems.of(Power.AGATHA, "").equals(List.of(ModItems.DARKHOLD.get())), "Agatha: the Darkhold");
        check(HeroItems.of(Power.QUICKSILVER, "").equals(List.of(ModItems.QUICKSILVER_GOGGLES.get())), "Quicksilver: his goggles");
        for (Power p : List.of(Power.SCARLET_WITCH, Power.VISION, Power.BILLY, Power.TOMMY, Power.PHOENIX, Power.SPIDER_MAN, Power.APOCALYPSE,
                Power.NONE, Power.CUSTOM)) {
            check(HeroItems.of(p, "").isEmpty(), p + ": the items that woke him are used up, so nothing");
        }
        for (String look : HeroItems.STORMBREAKER_LOOKS) {
            check(com.kolodkingal.enhanced.power.Variants.of(Power.THOR).stream().anyMatch(v -> v.id().equals(look)), "a look of Thor: " + look);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = ITEMS_CONFIG)
    public static void theItemsCanBeSwitchedOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CustomHeroConfig.CHOOSE_GIVES_ITEMS, false);
        ServerPlayer agatha = f.player(5.5, FLOOR, 5.5, GameType.CREATIVE);
        check(CustomHeroes.choose(agatha, Power.AGATHA, ""), "chosen");
        check(count(agatha, ModItems.DARKHOLD.get()) == 0, "no Darkhold when the server says so");
        helper.succeed();
    }
}
