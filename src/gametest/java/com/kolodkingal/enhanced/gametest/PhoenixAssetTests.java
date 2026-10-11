package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Phoenix's art, sounds and words, checked as files: the two skins follow the skin rules (and leave the glowing
 * eyes free), the Ember is an animated strip, the icon of the Possessed effect is an 18 by 18 effect icon, the seven
 * sounds are mono 44.1 kHz Ogg Vorbis of the length their recipe makes, and the Hebrew of her lang block is Hebrew.
 * Nothing here loads a client class.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class PhoenixAssetTests {
    private static final String BATCH = PhoenixTests.QUICK;

    /** One box of the player model: where its faces are on a 64 by 64 skin (slim arms), and whether it is the base layer. */
    private record Part(String name, boolean base, int u, int v, int w, int h, int d) {
        int[][] faces() {
            return new int[][]{{u + d, v, w, d}, {u + d + w, v, w, d}, {u, v + d, d, h}, {u + d, v + d, w, h},
                    {u + d + w, v + d, d, h}, {u + d + w + d, v + d, w, h}};
        }
    }

    private static final List<Part> SLIM = List.of(
            new Part("head", true, 0, 0, 8, 8, 8), new Part("hat", false, 32, 0, 8, 8, 8),
            new Part("body", true, 16, 16, 8, 12, 4), new Part("jacket", false, 16, 32, 8, 12, 4),
            new Part("right arm", true, 40, 16, 3, 12, 4), new Part("right sleeve", false, 40, 32, 3, 12, 4),
            new Part("left arm", true, 32, 48, 3, 12, 4), new Part("left sleeve", false, 48, 48, 3, 12, 4),
            new Part("right leg", true, 0, 16, 4, 12, 4), new Part("right pants", false, 0, 32, 4, 12, 4),
            new Part("left leg", true, 16, 48, 4, 12, 4), new Part("left pants", false, 0, 48, 4, 12, 4));

    // ------------------------------------------------------------------------------------------------------------
    // Skins

    @GameTest(template = ARENA, batch = BATCH)
    public static void bothSkinsFollowTheSkinRulesAndLeaveTheGlowingEyesFree(GameTestHelper helper) throws IOException {
        Map<String, BufferedImage> skins = new LinkedHashMap<>();
        for (String id : List.of("apocalypse", "brand_new_day")) {
            Variant look = Variants.get(Power.PHOENIX, id);
            check(look.slim() && look.cape() == null, id + " has slim arms and no cape");
            BufferedImage skin = image(look.skin().getPath());
            check(skin.getWidth() == 64 && skin.getHeight() == 64, id + " is 64 by 64");
            boolean[][] used = new boolean[64][64];
            int opaque = 0;
            for (Part part : SLIM) {
                for (int[] f : part.faces()) {
                    for (int y = f[1]; y < f[1] + f[3]; y++) {
                        for (int x = f[0]; x < f[0] + f[2]; x++) {
                            used[y][x] = true;
                            int alpha = alpha(skin, x, y);
                            check(!part.base() || alpha == 255, id + ": the base layer (" + part.name() + ") is opaque at " + x + "," + y);
                            check(part.base() || alpha == 0 || alpha == 255, id + ": no half-transparent overlay pixel at " + x + "," + y);
                            if (alpha == 255) {
                                opaque++;
                            }
                        }
                    }
                }
            }
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    check(used[y][x] || alpha(skin, x, y) == 0, id + ": nothing is drawn outside the used rectangles at " + x + "," + y);
                }
            }
            check(opaque > 1300, id + " is a finished skin, not a flat placeholder (" + opaque + " opaque pixels)");
            // The eyes (the glow masks) sit on the head front at x 9..10 and 13..14, y 11..12; the hat layer is drawn
            // half a pixel above the head and the eye layer is not, so the hat must leave those eight pixels clear.
            BufferedImage fire = image("textures/entity/phoenix_eyes.png");
            BufferedImage psychic = image("textures/entity/phoenix_eyes_psychic.png");
            int glow = 0;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    boolean eye = alpha(fire, x, y) != 0;
                    check(eye == (alpha(psychic, x, y) != 0), "the fire eyes and the violet eyes are painted on the same pixels (" + x + "," + y + ")");
                    if (eye) {
                        glow++;
                        check(x >= 8 && x < 16 && y >= 8 && y < 16, "the glowing eyes are on the face");
                        check(alpha(skin, x + 32, y) == 0, id + ": the hat layer is clear over the glowing eye at " + x + "," + y);
                    }
                }
            }
            check(glow == 8, "two eyes of two by two pixels: " + glow);
            skins.put(id, skin);
        }
        // Two different looks: the head, the body and the limbs all differ (they are not one skin twice). A leg
        // overlay may be nearly the same in both looks, so only the base layers are held to a count of their own.
        BufferedImage a = skins.get("apocalypse");
        BufferedImage b = skins.get("brand_new_day");
        int total = 0;
        for (Part part : SLIM) {
            int differing = 0;
            for (int[] f : part.faces()) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if (a.getRGB(x, y) != b.getRGB(x, y)) {
                            differing++;
                        }
                    }
                }
            }
            total += differing;
            check(!part.base() || differing > 10, "the two looks differ in the " + part.name() + " (" + differing + " pixels)");
        }
        check(total > 800, "the two looks are two skins, not one twice (" + total + " differing pixels)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHeroLooksAreRedAndOliveWhereTheFilmsAreRedAndOlive(GameTestHelper helper) throws IOException {
        // A cheap guard against a swapped or redrawn-by-mistake skin: the Apocalypse look is red hair and a blue body,
        // the Brand New Day look an olive hood and a dark jacket with a mustard centre.
        BufferedImage apocalypse = image("textures/entity/suit/phoenix_apocalypse.png");
        BufferedImage day = image("textures/entity/suit/phoenix_brand_new_day.png");
        check(share(apocalypse, 8, 0, 8, 8, c -> red(c) > 150 && green(c) < 110 && blue(c) < 60) > 0.7, "the Apocalypse crown is red");
        check(share(apocalypse, 20, 20, 8, 12, c -> blue(c) > red(c) + 40) > 0.2, "the Apocalypse body is blue");
        check(share(apocalypse, 20, 20, 8, 12, c -> red(c) > 200 && green(c) > 150 && blue(c) < 80) > 0.04, "with the yellow of the stripes");
        check(share(day, 40, 0, 8, 8, c -> green(c) > blue(c) + 20 && red(c) > blue(c) + 20 && green(c) >= red(c) - 10) > 0.6,
                "the Brand New Day hood is olive");
        check(share(day, 20, 20, 8, 12, c -> red(c) > 150 && green(c) > 110 && blue(c) < 110) > 0.2, "with a mustard strip down the body");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Ember and the icon

    @GameTest(template = ARENA, batch = BATCH)
    public static void theEmberIsAnAnimatedStripOfFourFramesAndTheEffectIconIsAnEffectIcon(GameTestHelper helper) throws IOException {
        BufferedImage ember = image("textures/item/phoenix_ember.png");
        check(ember.getWidth() == 16 && ember.getHeight() == 64, "the Ember is a strip of four 16 by 16 frames: " + ember.getWidth() + "x" + ember.getHeight());
        int[] lit = new int[4];
        for (int f = 0; f < 4; f++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    if (alpha(ember, x, f * 16 + y) != 0) {
                        lit[f]++;
                    }
                }
            }
            check(lit[f] > 60 && lit[f] < 200, "frame " + f + " draws a flame, not a block or nothing (" + lit[f] + " pixels)");
        }
        boolean differs = false;
        for (int y = 0; y < 16 && !differs; y++) {
            for (int x = 0; x < 16; x++) {
                differs |= ember.getRGB(x, y) != ember.getRGB(x, 16 + y);
            }
        }
        check(differs, "the frames flicker (the first two differ)");
        JsonObject meta = JsonParser.parseString(text("textures/item/phoenix_ember.png.mcmeta")).getAsJsonObject().getAsJsonObject("animation");
        check(meta.get("frametime").getAsInt() == 2 && meta.get("interpolate").getAsBoolean(), "frametime 2, interpolated");

        BufferedImage icon = image("textures/mob_effect/possessed.png");
        check(icon.getWidth() == 18 && icon.getHeight() == 18, "the effect icon is 18 by 18");
        int drawn = 0;
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 18; x++) {
                drawn += alpha(icon, x, y) != 0 ? 1 : 0;
            }
        }
        check(drawn > 90 && drawn < 260, "the icon is a figure, not a disc or a speck (" + drawn + " pixels)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sounds

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSevenSoundsAreMonoOggVorbisOfTheRightLengthWithEnglishAndHebrewSubtitles(GameTestHelper helper) throws IOException {
        // name -> seconds (the spec's table)
        Map<String, Double> sounds = new LinkedHashMap<>();
        sounds.put("phoenix_charge", 3.0);
        sounds.put("phoenix_burst", 3.2);
        sounds.put("phoenix_blaze_loop", 2.0);
        sounds.put("mind_read", 0.6);
        sounds.put("mind_sense", 1.0);
        sounds.put("mind_control", 0.9);
        sounds.put("psionic_field", 1.4);
        for (Map.Entry<String, Double> e : sounds.entrySet()) {
            byte[] ogg = bytes("sounds/sfx/" + e.getKey() + ".ogg");
            check(ogg.length > 6000, e.getKey() + " is real audio, not a stub (" + ogg.length + " bytes)");
            check(ogg[0] == 'O' && ogg[1] == 'g' && ogg[2] == 'g' && ogg[3] == 'S', e.getKey() + " is an Ogg file");
            // The first page holds the Vorbis identification header: 1, "vorbis", version, channels, sample rate.
            int id = 27 + (ogg[26] & 0xFF); // the page header is 27 bytes, then one byte for every segment
            check(ogg[id] == 1 && ogg[id + 1] == 'v' && ogg[id + 2] == 'o' && ogg[id + 3] == 'r', e.getKey() + " is Vorbis");
            int channels = ogg[id + 11] & 0xFF;
            int rate = (ogg[id + 12] & 0xFF) | (ogg[id + 13] & 0xFF) << 8 | (ogg[id + 14] & 0xFF) << 16 | (ogg[id + 15] & 0xFF) << 24;
            check(channels == 1 && rate == 44100, e.getKey() + " is mono 44.1 kHz (" + channels + " channels, " + rate + " Hz)");
            // The last page's granule position is the length in samples.
            long granule = 0;
            for (int i = ogg.length - 27; i >= 0; i--) {
                if (ogg[i] == 'O' && ogg[i + 1] == 'g' && ogg[i + 2] == 'g' && ogg[i + 3] == 'S') {
                    for (int k = 7; k >= 0; k--) {
                        granule = granule << 8 | (ogg[i + 6 + k] & 0xFF);
                    }
                    break;
                }
            }
            double seconds = granule / 44100.0;
            check(Math.abs(seconds - e.getValue()) < 0.06, e.getKey() + " lasts " + e.getValue() + " s, not " + seconds);
        }
        JsonObject json = JsonParser.parseString(text("sounds.json")).getAsJsonObject();
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        for (String event : List.of("phoenix_charge", "phoenix_burst", "phoenix_blaze", "mind_read", "mind_sense", "mind_control", "psionic_field")) {
            check(json.has(event), "sounds.json has " + event);
            String subtitle = json.getAsJsonObject(event).get("subtitle").getAsString();
            check(subtitle.equals("subtitles.enhanced." + event), event + " has the subtitle key subtitles.enhanced." + event);
            check(en.has(subtitle) && he.has(subtitle), event + "'s subtitle is in both languages");
            check(hebrew(he.get(subtitle).getAsString()), event + "'s Hebrew subtitle is Hebrew: " + he.get(subtitle).getAsString());
        }
        check(json.getAsJsonObject("phoenix_blaze").getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString()
                .equals("enhanced:sfx/phoenix_blaze_loop"), "the blaze event plays the loop");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Words

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHebrewOfHerLangBlockIsHebrewAndNotAnEnglishCopy(GameTestHelper helper) throws IOException {
        List<String> lines = List.of(text("lang/he_il.json").split("\n"));
        List<String> englishLines = List.of(text("lang/en_us.json").split("\n"));
        int from = lines.indexOf(lines.stream().filter(l -> l.contains("\"_lane.phoenix\"")).findFirst().orElseThrow());
        int to = lines.indexOf(lines.stream().filter(l -> l.contains("\"_lane.spider\"")).findFirst().orElseThrow());
        int englishFrom = englishLines.indexOf(englishLines.stream().filter(l -> l.contains("\"_lane.phoenix\"")).findFirst().orElseThrow());
        int englishTo = englishLines.indexOf(englishLines.stream().filter(l -> l.contains("\"_lane.spider\"")).findFirst().orElseThrow());
        check(to - from == englishTo - englishFrom, "the two blocks have the same number of keys: " + (to - from) + " and " + (englishTo - englishFrom));
        JsonObject he = lang("he_il");
        JsonObject en = lang("en_us");
        int keys = 0;
        for (int i = from + 1; i < to; i++) {
            String key = lines.get(i).trim().split("\"")[1];
            keys++;
            check(en.has(key) && he.has(key), key + " is in both languages");
            String hebrewText = he.get(key).getAsString();
            check(hebrew(hebrewText), key + " is Hebrew: " + hebrewText);
            check(!hebrewText.equals(en.get(key).getAsString()), key + " is not the English text");
            // the same % placeholders in the same order, in both languages
            check(placeholders(hebrewText).equals(placeholders(en.get(key).getAsString())), key + " keeps the placeholders: " + hebrewText);
            // the names the spec fixes
            check(!hebrewText.contains("  ") && !hebrewText.startsWith(" ") && !hebrewText.endsWith(" "), key + " has no stray spaces");
        }
        check(keys >= 52, "the whole block is checked (" + keys + " keys)");
        check(he.get("power.enhanced.phoenix").getAsString().equals("ג'ין גריי (הפניקס)"), "her name");
        check(he.get("item.enhanced.phoenix_ember").getAsString().equals("ניצוץ הפניקס"), "the Ember's name");
        check(!he.get("subtitles.enhanced.mind_sense").getAsString().contains("נחשים"), "a mind sensed is not a snake (נחשים)");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------

    private static boolean hebrew(String text) {
        return text.chars().anyMatch(c -> c >= 0x05D0 && c <= 0x05EA);
    }

    private static String placeholders(String text) {
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%(\\d\\$)?[sd%]").matcher(text);
        while (m.find()) {
            out.append(m.group()).append(' ');
        }
        // %1$s %2$s may be reordered in Hebrew; compare as a sorted list
        String[] parts = out.toString().trim().isEmpty() ? new String[0] : out.toString().trim().split(" ");
        java.util.Arrays.sort(parts);
        return String.join(" ", parts);
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static int red(int argb) {
        return argb >> 16 & 0xFF;
    }

    private static int green(int argb) {
        return argb >> 8 & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    /** The share of the opaque pixels of a rectangle for which the test is true. */
    private static double share(BufferedImage image, int x0, int y0, int w, int h, java.util.function.IntPredicate test) {
        int opaque = 0;
        int hits = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int argb = image.getRGB(x, y);
                if (argb >>> 24 == 255) {
                    opaque++;
                    hits += test.test(argb) ? 1 : 0;
                }
            }
        }
        return opaque == 0 ? 0.0 : hits / (double) opaque;
    }

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = PhoenixAssetTests.class.getResourceAsStream("/assets/enhanced/" + path)) {
            check(in != null, path + " is in the mod");
            return in.readAllBytes();
        }
    }

    private static String text(String path) throws IOException {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream in = PhoenixAssetTests.class.getResourceAsStream("/assets/enhanced/" + path)) {
            check(in != null, path + " is in the mod");
            BufferedImage image = ImageIO.read(in);
            check(image != null, path + " is a readable image");
            return image;
        }
    }

    private static JsonObject lang(String name) throws IOException {
        return JsonParser.parseString(text("lang/" + name + ".json")).getAsJsonObject();
    }
}
