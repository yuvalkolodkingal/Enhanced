package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroServer;
import com.kolodkingal.enhanced.custom.HeroLooks;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.SkinCheck;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Hero Creator's data (3.3, F2-F5): the definition of a custom hero, its JSON (written and read back, lenient with
 * what it does not know), its two wire forms, the server's rules (names, strengths, options, budget, how many powers,
 * keys, looks, skins), the checks of a skin, the numbers of every power at every strength, and the names of it all in both
 * languages. Pure: no world is needed, so these run in one quick batch.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CustomHeroDataTests {
    private static final String BATCH = "custom_data";

    /** A valid PNG of {@code w} x {@code h}, every pixel {@code rgba} (8-bit RGBA). */
    static byte[] png(int w, int h, int rgba) {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            for (int y = 0; y < h; y++) {
                raw.write(0);
                for (int x = 0; x < w; x++) {
                    raw.write((rgba >> 24) & 0xFF);
                    raw.write((rgba >> 16) & 0xFF);
                    raw.write((rgba >> 8) & 0xFF);
                    raw.write(rgba & 0xFF);
                }
            }
            Deflater deflater = new Deflater();
            deflater.setInput(raw.toByteArray());
            deflater.finish();
            byte[] buf = new byte[raw.size() + 1024];
            int len = deflater.deflate(buf);
            deflater.end();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(w).putInt(h).put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array());
            byte[] idat = new byte[len];
            System.arraycopy(buf, 0, idat, 0, len);
            chunk(out, "IDAT", idat);
            chunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] body) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(body.length).array());
        byte[] t = type.getBytes(StandardCharsets.ISO_8859_1);
        out.write(t);
        out.write(body);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(body);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    /** A full hero: a skin, five powers with options, strengths and one colour of its own, four keys. */
    static CustomHero sample() {
        List<CustomHero.Pick> picks = List.of(
                new CustomHero.Pick("flight", 4, "jet", -1),
                new CustomHero.Pick("energy_blast", 7, "bolts", 0xFF4040),
                new CustomHero.Pick("durability", 3, "health", -1),
                new CustomHero.Pick("telekinesis", 5, "everything", -1),
                new CustomHero.Pick("force_field", 6, "reflect", 0x40FF80));
        return new CustomHero(CustomHero.VERSION, "Storm Girl", 0x3FA9F5, 0xE0F4FF, CustomHero.LOOK_SKIN, true,
                png(64, 64, 0x3366CCFF), picks, List.of("energy_blast", "flight", "telekinesis", "force_field", ""));
    }

    // ---- JSON ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void aHeroSurvivesItsJsonTheSaveAndTheWire(GameTestHelper helper) {
        CustomHero hero = sample();
        String json = CustomHeroJson.write(hero);
        CustomHeroJson.Read read = CustomHeroJson.read(json);
        check(read.ok() && read.notes().isEmpty(), "the JSON reads back cleanly: " + read.notes());
        check(hero.equals(read.hero()), "JSON round trip: " + read.hero());
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        check(root.get("format").getAsString().equals(CustomHeroJson.FORMAT) && root.get("version").getAsInt() == CustomHero.VERSION,
                "the file says what it is and its version");
        check(root.get("color").getAsString().equals("#3FA9F5"), "colours are written #RRGGBB");
        check(root.getAsJsonArray("powers").get(1).getAsJsonObject().get("strength").getAsInt() == 7, "each power carries its strength");
        check(Base64.getDecoder().decode(root.get("skin").getAsString()).length == hero.skin().length, "the skin is base64 PNG");

        Tag saved = CustomHero.CODEC.encodeStart(NbtOps.INSTANCE, hero).getOrThrow();
        check(hero.equals(CustomHero.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow()), "the save (with the player) round trip");

        ByteBuf buf = Unpooled.buffer();
        CustomHero.STREAM_CODEC.encode(buf, hero);
        check(hero.equals(CustomHero.STREAM_CODEC.decode(buf)) && buf.readableBytes() == 0, "the wire round trip");
        ByteBuf p = Unpooled.buffer();
        HeroCreatorPayload.STREAM_CODEC.encode(p, HeroCreatorPayload.apply(hero));
        check(p.readableBytes() < 32 * 1024, "the creator's message stays under the 32 KiB of a packet: " + p.readableBytes());
        HeroCreatorPayload back = HeroCreatorPayload.STREAM_CODEC.decode(p);
        check(back.action() == HeroCreatorPayload.APPLY && hero.equals(back.hero()), "the creator's message round trip");
        ByteBuf c = Unpooled.buffer();
        HeroCreatorPayload.STREAM_CODEC.encode(c, HeroCreatorPayload.choose(Power.THOR, "ragnarok"));
        HeroCreatorPayload chosen = HeroCreatorPayload.STREAM_CODEC.decode(c);
        check(chosen.action() == HeroCreatorPayload.CHOOSE && chosen.power().equals("thor") && chosen.look().equals("ragnarok"), "choose round trip");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void readingIsLenientAndSaysWhatItDidNotUnderstand(GameTestHelper helper) {
        check(!CustomHeroJson.read("").ok() && !CustomHeroJson.read(null).ok(), "nothing is no hero");
        check(!CustomHeroJson.read("{not json").ok(), "broken JSON is no hero");
        check(!CustomHeroJson.read("[1,2,3]").ok(), "an array is no hero");
        check(!CustomHeroJson.read("{\"format\":\"something_else\",\"name\":\"x\"}").ok(), "another format is no hero");
        check(!CustomHeroJson.read("x".repeat(CustomHeroJson.MAX_CHARS + 1)).ok(), "a huge text is not read at all");
        String hand = "{\"name\":\"Hand Made\",\"color\":\"FF0000\",\"glow\":65280,\"version\":99,\"wings\":true,"
                + "\"powers\":[{\"id\":\"FLIGHT\",\"level\":3},\"fire\",{\"id\":\"teleport\",\"strength\":2,\"option\":\"behind\",\"color\":\"#00FF00\"},42],"
                + "\"look\":\"thor/ragnarok\",\"skin\":\"!!!not base64\",\"keys\":[\"flight\",7]}";
        CustomHeroJson.Read read = CustomHeroJson.read(hand);
        check(read.ok(), "a hand-written hero reads");
        CustomHero h = read.hero();
        check(h.name().equals("Hand Made") && h.color() == 0xFF0000 && h.glow() == 0x00FF00, "colours in any of the three forms");
        check(h.powers().size() == 3, "a power entry that is not an object or a name is skipped: " + h.powers());
        check(h.pick(PowerModule.FLIGHT).strength() == 10, "an old \"level\": 3 reads as strength 10");
        check(h.pick(PowerModule.FIRE).strength() == CustomHeroJson.DEFAULT_STRENGTH, "a bare name gets the default strength");
        check(h.pick(PowerModule.TELEPORT).color() == 0x00FF00 && h.pick(PowerModule.TELEPORT).option().equals("behind"), "options and colours");
        check(h.look().equals("thor/ragnarok") && h.skin().length == 0, "a look written as a string; a broken skin is dropped");
        check(h.keys().size() == CustomHero.KEYS && h.keys().get(0).equals("flight") && h.keys().get(1).isEmpty(), "keys padded to five");
        check(read.notes().containsAll(List.of("message.enhanced.hero_creator.import.newer", "message.enhanced.hero_creator.import.bad_skin",
                "message.enhanced.hero_creator.import.bad_power")), "each misunderstanding is noted: " + read.notes());
        check(CustomHeroJson.fileName("  Storm Girl!! ").equals("storm_girl") && CustomHeroJson.fileName("??").equals("hero")
                && CustomHeroJson.fileName("גיבור על").equals("גיבור_על"), "file names are safe in any script");
        helper.succeed();
    }

    // ---- the rules ----

    private static CustomHero only(CustomHero.Pick... picks) {
        return CustomHero.EMPTY.withName("Test").withPowers(List.of(picks));
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRulesCleanNamesStrengthsOptionsAndRepeats(GameTestHelper helper) {
        CustomHeroRules.Limits limits = CustomHeroRules.Limits.DEFAULT;
        check(CustomHeroRules.cleanName("§cRed§r  Hero\n").equals("Red Hero"), "formatting codes and controls go");
        check(CustomHeroRules.cleanName("A".repeat(40)).length() == CustomHero.MAX_NAME, "names are cut to 24");
        check(CustomHeroRules.sanitize(CustomHero.EMPTY, limits).hero().name().equals(CustomHeroRules.DEFAULT_NAME), "no name: My Hero");
        CustomHeroRules.Result r = CustomHeroRules.sanitize(only(
                new CustomHero.Pick("flight", 0, "jet", -1),
                new CustomHero.Pick("fire", 99, "no_such_option", 0x12345678),
                new CustomHero.Pick("laser_tentacles", 3, "", -1),
                new CustomHero.Pick("flight", 5, "hover", -1)), limits);
        CustomHero h = r.hero();
        check(h.powers().size() == 2, "the unknown power and the repeat go: " + h.powers());
        check(h.pick(PowerModule.FLIGHT).strength() == 1 && h.pick(PowerModule.FLIGHT).option().equals("jet"), "strength 0 is clamped to 1");
        check(h.pick(PowerModule.FIRE).strength() == PowerModule.MAX_STRENGTH, "strength 99 is clamped to 10");
        check(h.pick(PowerModule.FIRE).option().equals("fireball"), "an unknown option is the default");
        check(h.pick(PowerModule.FIRE).color() == 0x345678, "a colour is opaque RGB");
        List<String> keys = r.notes().stream().map(CustomHeroRules.Note::key).toList();
        check(keys.contains("message.enhanced.hero_creator.rule.unknown") && keys.contains("message.enhanced.hero_creator.rule.strength"),
                "and it says so: " + keys);
        CustomHeroRules.Limits four = new CustomHeroRules.Limits(600, 8, 4, 16384, Set.of());
        check(CustomHeroRules.sanitize(only(new CustomHero.Pick("fire", 9, "", -1)), four).hero().pick(PowerModule.FIRE).strength() == 4,
                "a server's lower maxStrength clamps too");
        check(CustomHeroRules.sanitize(sample(), limits).hero().equals(sample()), "a hero within the rules is left as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBudgetAndTheCountDropTheLastPicked(GameTestHelper helper) {
        check(CustomHeroRules.Limits.DEFAULT.budget() == 60 && CustomHeroRules.Limits.DEFAULT.maxPowers() == 8, "the defaults");
        check(PowerModule.PHASING.cost(10) == 30 && PowerModule.DURABILITY.cost(3) == 3 && PowerModule.FLIGHT.cost(5) == 10, "cost = points x strength");
        CustomHero big = only(new CustomHero.Pick("phasing", 10, "", -1), new CustomHero.Pick("time", 10, "", -1),
                new CustomHero.Pick("flight", 10, "", -1), new CustomHero.Pick("durability", 1, "", -1));
        check(CustomHeroRules.cost(big) == 30 + 30 + 20 + 1, "cost of a list: " + CustomHeroRules.cost(big));
        CustomHeroRules.Result r = CustomHeroRules.sanitize(big.withKeys(List.of("phasing", "time", "flight", "", "")), CustomHeroRules.Limits.DEFAULT);
        check(CustomHeroRules.cost(r.hero()) <= 60, "the budget holds: " + CustomHeroRules.cost(r.hero()));
        check(r.hero().has(PowerModule.PHASING) && r.hero().has(PowerModule.TIME) && !r.hero().has(PowerModule.FLIGHT)
                && !r.hero().has(PowerModule.DURABILITY), "the last picked go first: " + r.hero().powers());
        check(r.hero().keys().get(2).isEmpty(), "a key whose power was dropped is cleared");
        check(r.notes().stream().anyMatch(n -> n.key().equals("message.enhanced.hero_creator.rule.budget")), "the budget note");
        List<CustomHero.Pick> nine = new ArrayList<>();
        for (PowerModule m : List.of(PowerModule.values()).subList(0, 9)) {
            nine.add(new CustomHero.Pick(m.id, 1, "", -1));
        }
        CustomHeroRules.Result count = CustomHeroRules.sanitize(only(nine.toArray(CustomHero.Pick[]::new)), CustomHeroRules.Limits.DEFAULT);
        check(count.hero().powers().size() == 8 && !count.hero().has(List.of(PowerModule.values()).get(8)), "at most 8 powers");
        CustomHeroRules.Limits noFire = new CustomHeroRules.Limits(60, 8, 10, 16384, Set.of("fire"));
        check(!CustomHeroRules.sanitize(only(new CustomHero.Pick("fire", 3, "", -1)), noFire).hero().has(PowerModule.FIRE), "a disabled power goes");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void keysOnlyHoldPowersThatTakeAKey(GameTestHelper helper) {
        CustomHero h = only(new CustomHero.Pick("durability", 3, "armor", -1), new CustomHero.Pick("flight", 3, "", -1),
                new CustomHero.Pick("healing", 3, "steady", -1))
                .withKeys(List.of("durability", "flight", "flight", "nonsense", "healing"));
        CustomHero out = CustomHeroRules.sanitize(h, CustomHeroRules.Limits.DEFAULT).hero();
        check(out.keys().equals(List.of("", "flight", "", "", "")), "a passive power, a repeat and an unknown id are cleared: " + out.keys());
        check(out.onKey(2) != null && out.onKey(2).module() == PowerModule.FLIGHT && out.keyOf(PowerModule.FLIGHT) == 2, "the key reads back");
        CustomHero swapped = out.withKey(4, "flight");
        check(swapped.keyOf(PowerModule.FLIGHT) == 4 && swapped.keys().get(1).isEmpty(), "a power is on one key at most");
        check(out.without(PowerModule.FLIGHT).keys().stream().allMatch(String::isEmpty), "removing a power frees its key");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void looksAndSkinsAreChecked(GameTestHelper helper) {
        CustomHeroRules.Limits limits = CustomHeroRules.Limits.DEFAULT;
        byte[] good = png(64, 64, 0xFF0000FF);
        check(SkinCheck.check(good, 16384) == SkinCheck.Verdict.OK, "a 64x64 PNG is a skin");
        check(SkinCheck.check(png(32, 32, 0xFF0000FF), 16384) == SkinCheck.Verdict.WRONG_SIZE, "a 32x32 is not");
        check(SkinCheck.check(png(64, 32, 0xFF0000FF), 16384) == SkinCheck.Verdict.WRONG_SIZE, "nor the old 64x32");
        check(SkinCheck.check(good, 10) == SkinCheck.Verdict.TOO_BIG, "the size limit");
        check(SkinCheck.check("hello world, not a picture at all".getBytes(), 16384) == SkinCheck.Verdict.NOT_PNG, "text is not a PNG");
        check(SkinCheck.check(new byte[0], 16384) == SkinCheck.Verdict.EMPTY, "nothing");
        byte[] broken = good.clone();
        broken[broken.length - 20] ^= 0x55;
        check(SkinCheck.check(broken, 16384) == SkinCheck.Verdict.BROKEN, "a damaged chunk (its CRC)");
        byte[] cut = java.util.Arrays.copyOf(good, good.length - 12);
        check(SkinCheck.check(cut, 16384) == SkinCheck.Verdict.BROKEN, "a file cut short");

        CustomHero wrong = only().withSkin(png(16, 16, 0xFFFFFFFF)).withLook(CustomHero.LOOK_SKIN, true);
        CustomHeroRules.Result r = CustomHeroRules.sanitize(wrong, limits);
        check(r.hero().skin().length == 0 && r.hero().look().equals(CustomHero.LOOK_OWN) && !r.hero().slim(), "a bad skin goes, and the look is own");
        check(r.notes().stream().anyMatch(n -> n.key().equals(SkinCheck.Verdict.WRONG_SIZE.messageKey())), "with the reason");
        CustomHeroRules.Limits small = new CustomHeroRules.Limits(60, 8, 10, 100, Set.of());
        check(CustomHeroRules.sanitize(only().withSkin(good).withLook(CustomHero.LOOK_SKIN, false), small).hero().skin().length == 0,
                "the server's size limit");
        check(CustomHeroRules.sanitize(only().withLook("thor/ragnarok", false), limits).hero().look().equals("thor/ragnarok"), "a known hero look");
        check(CustomHeroRules.sanitize(only().withLook("thor/ragnarok", false), limits).hero().slim() == HeroLooks.byRef("thor/ragnarok").slim(),
                "takes its own arms");
        check(CustomHeroRules.sanitize(only().withLook("thor/no_such", true), limits).hero().look().equals(CustomHero.LOOK_OWN), "an unknown look");
        check(CustomHeroRules.sanitize(only().withLook("custom/x", true), limits).hero().look().equals(CustomHero.LOOK_OWN), "never a custom look");
        helper.succeed();
    }

    /** The heroes to choose from come from the Power enum and the looks from Variants: a new hero appears by itself. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theHeroesToChooseComeFromThePowersAndTheirLooks(GameTestHelper helper) {
        List<Power> heroes = HeroLooks.heroes();
        for (Power power : Power.values()) {
            check(heroes.contains(power) == (power.hasPowers() && power != Power.CUSTOM), power + " is listed iff it is a hero");
            for (Variant v : Variants.of(power)) {
                if (v.skin() != null) {
                    check(HeroLooks.byRef(power.getSerializedName() + "/" + v.id()) != null, v + " can be worn by a custom hero");
                }
            }
        }
        for (Power power : List.of(Power.MAGNETO, Power.SCARLET_WITCH, Power.QUICKSILVER)) {
            check(HeroLooks.of(power).size() == 1 && HeroLooks.of(power).get(0).id().equals(HeroLooks.SUIT), power + "'s single suit");
        }
        helper.succeed();
    }

    // ---- the catalogue and its numbers (F2, F5) ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void thirtyPowersEachWithLogicOptionsAndNumbers(GameTestHelper helper) {
        check(PowerModule.values().length == 30, "thirty powers: " + PowerModule.values().length);
        for (PowerModule m : PowerModule.values()) {
            check(PowerModule.byId(m.id) == m && PowerModule.byId(m.id.toUpperCase()) == m, m + " is found by its id");
            check(CustomHeroServer.logic(m) != null, m + " has its server logic");
            check(!m.options().isEmpty() && m.costPerPoint >= 1 && m.costPerPoint <= 3, m + " has options and a cost");
            for (PowerModule.Option o : m.options()) {
                List<ModuleStats.Stat> stats = ModuleStats.of(m, o.id());
                check(m == PowerModule.SUPER_SENSES && o.id().equals("night") || !stats.isEmpty() || o.act() == PowerModule.Act.NONE,
                        m + "/" + o.id() + " shows its numbers");
                for (ModuleStats.Stat s : stats) {
                    float low = s.value(1);
                    float high = s.value(10);
                    check(Float.isFinite(low) && Float.isFinite(high) && low >= 0, m + " " + s.id() + " is a number");
                    if (s.kind() == ModuleStats.Kind.COOLDOWN || s.id().equals("hunger")) {
                        check(high <= low, m + " " + s.id() + ": a stronger power recharges faster and costs less");
                    } else if (!(m == PowerModule.SHAPESHIFT && o.id().equals("shrink"))) {
                        check(high >= low, m + " " + s.id() + ": a stronger power does more");
                    }
                    float last = s.value(1);
                    for (int st = 2; st <= 10; st++) {
                        float now = s.value(st);
                        check(high >= low ? now >= last - 1.0E-4F : now <= last + 1.0E-4F, m + " " + s.id() + " moves one way with the slider");
                        last = now;
                    }
                }
            }
        }
        assertNear(ModuleStats.BOLT_DAMAGE.value(1), 3.0, 1.0E-4, "a value at strength 1");
        assertNear(ModuleStats.BOLT_DAMAGE.value(10), 12.0, 1.0E-4, "and at 10");
        assertNear(ModuleStats.BOLT_DAMAGE.raw(0), 3.0, 1.0E-4, "the slider is clamped");
        check(ModuleStats.BOLT_COOLDOWN.shown(10).equals("0.3") && ModuleStats.BOLT_DAMAGE.shown(10).equals("6"), "the preview's text");
        check(ModuleStats.CHAIN_JUMPS.value(5) == Math.round(ModuleStats.CHAIN_JUMPS.value(5)), "counts are whole");
        helper.succeed();
    }

    /** The server's damage and cooldown scales reach every number, and the preview shows the scaled ones. */
    @GameTest(template = ARENA, batch = "custom_data_scale")
    public static void theConfigScalesTheNumbers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(CustomHeroConfig.DAMAGE_SCALE, 2.0);
        f.config(CustomHeroConfig.COOLDOWN_SCALE, 0.5);
        f.config(CustomHeroConfig.FLIGHT_SPEED, 0.16);
        assertNear(ModuleStats.BOLT_DAMAGE.value(1), 6.0, 1.0E-4, "damage doubled");
        assertNear(ModuleStats.PUNCH_COOLDOWN.value(10), 10.0, 1.0E-4, "cooldown halved");
        assertNear(ModuleStats.FLIGHT_SPEED.value(1), 0.12, 1.0E-4, "flight speed follows flightSpeed");
        assertNear(ModuleStats.BEAM_RANGE.value(1), 16.0, 1.0E-4, "a range is not scaled");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aBeamOfAnyColourIsAStyle(GameTestHelper helper) {
        BeamStyle style = CustomBeams.style(0x123456, 0xABCDEF);
        check(style.main() == 0x123456 && style.inner() == 0xABCDEF, "the hero's colours");
        check(BeamStyles.byId(style.id()) == style, "registered");
        BeamStyle parsed = CustomBeams.parse("custom_ff0000_00ff00");
        check(parsed != null && parsed.main() == 0xFF0000 && parsed.inner() == 0x00FF00, "made again from its name (what a client does)");
        check(BeamStyles.byId("custom_00ff00_0000ff") != null && CustomBeams.parse("custom_zz") == null && CustomBeams.parse("billy_hex") == null,
                "only its own names");
        helper.succeed();
    }

    // ---- the words ----

    private static JsonObject lang(String name) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        Path path = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        try (InputStream in = Files.newInputStream(path)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyPowerOptionNumberAndMessageHasItsWordsInBothLanguages(GameTestHelper helper) throws IOException {
        List<String> missing = new ArrayList<>();
        for (String language : List.of("en_us", "he_il")) {
            JsonObject l = lang(language);
            List<String> keys = new ArrayList<>(List.of("power.enhanced.custom", "item.enhanced.hero_creator", "beam.enhanced.custom"));
            for (PowerModule m : PowerModule.values()) {
                keys.add(m.nameKey());
                keys.add(m.nameKey() + ".desc");
                for (PowerModule.Option o : m.options()) {
                    keys.add(m.optionKey(o.id()));
                    keys.add(m.optionKey(o.id()) + ".desc");
                    for (ModuleStats.Stat s : ModuleStats.of(m, o.id())) {
                        keys.add(s.nameKey());
                        keys.add(s.unitKey());
                    }
                    keys.add("screen.enhanced.hero_creator.act." + o.act().name().toLowerCase(java.util.Locale.ROOT));
                }
            }
            for (SkinCheck.Verdict v : SkinCheck.Verdict.values()) {
                keys.add(v.messageKey());
            }
            for (String key : keys) {
                if (!l.has(key)) {
                    missing.add(language + ": " + key);
                }
            }
        }
        check(missing.isEmpty(), "missing words: " + missing);
        helper.succeed();
    }

    private CustomHeroDataTests() {
    }
}
