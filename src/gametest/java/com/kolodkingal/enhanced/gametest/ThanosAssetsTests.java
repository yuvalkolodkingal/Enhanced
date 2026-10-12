package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the Mad Titan's art and sound promise, read from the files the mod ships: the two skins follow the skin rules
 * (nothing outside the used rectangles, an opaque base layer, no half-transparent overlay, the eyes where every look
 * puts them), every box of the boss's model has a painted face, the Gauntlet item is the animated strip the game
 * expects, and every sound of the fight is a registered event with its files, its subtitle in both languages and the
 * right format (mono, 44.1 kHz). Nothing here names a client class: the tests run on a dedicated server.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ThanosAssetsTests {
    private static final String BATCH = "thanos_assets";

    // ---- the files
    private static IModFile mod() {
        return ModList.get().getModFileById(Enhanced.MODID).getFile();
    }

    private static Path file(String path) {
        Path file = mod().findResource(("assets/" + Enhanced.MODID + "/" + path).split("/"));
        check(Files.isRegularFile(file), path + " exists");
        return file;
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream in = Files.newInputStream(file(path))) {
            BufferedImage image = ImageIO.read(in);
            check(image != null, path + " is a readable image");
            return image;
        }
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    // ---- the skin rules
    /** The boxes of a classic player's skin: u, v, width, height, depth, and whether it is a base layer. */
    private static final int[][] PLAYER = {
            {0, 0, 8, 8, 8, 1}, {32, 0, 8, 8, 8, 0},
            {16, 16, 8, 12, 4, 1}, {16, 32, 8, 12, 4, 0},
            {40, 16, 4, 12, 4, 1}, {40, 32, 4, 12, 4, 0},
            {32, 48, 4, 12, 4, 1}, {48, 48, 4, 12, 4, 0},
            {0, 16, 4, 12, 4, 1}, {0, 32, 4, 12, 4, 0},
            {16, 48, 4, 12, 4, 1}, {0, 48, 4, 12, 4, 0},
    };

    /** The rectangles of a box's six faces (x, y, w, h), as the game cuts them from the texture. */
    private static int[][] faces(int u, int v, int w, int h, int d) {
        return new int[][]{
                {u + d, v, w, d}, {u + d + w, v, w, d}, {u, v + d, d, h},
                {u + d, v + d, w, h}, {u + d + w, v + d, d, h}, {u + 2 * d + w, v + d, w, h}};
    }

    /** 0 where no face of the player model uses the pixel, 1 on a base layer, 2 on an overlay. */
    private static int[][] usage() {
        int[][] use = new int[64][64];
        for (int[] box : PLAYER) {
            for (int[] f : faces(box[0], box[1], box[2], box[3], box[4])) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        use[y][x] = box[5] == 1 ? 1 : Math.max(use[y][x], 2);
                    }
                }
            }
        }
        return use;
    }

    private static List<Variant> looks() {
        List<Variant> looks = new ArrayList<>();
        for (Variant look : Variants.of(Power.THANOS)) {
            if (look.skin() != null) {
                looks.add(look);
            }
        }
        return looks;
    }

    private static BufferedImage skin(Variant look) throws IOException {
        // look.skin() is a texture location such as enhanced:textures/entity/suit/thanos_endgame.png
        ResourceLocation skin = look.skin();
        return image(skin.getPath());
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void bothSkinsFollowTheSkinRules(GameTestHelper helper) throws IOException {
        List<Variant> looks = looks();
        check(looks.size() == 2, "Thanos has two skins (and 'your own skin', which has none): " + looks.size());
        int[][] use = usage();
        for (Variant look : looks) {
            BufferedImage skin = skin(look);
            String name = look.id();
            check(skin.getWidth() == 64 && skin.getHeight() == 64, name + " is 64 by 64");
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int a = alpha(skin, x, y);
                    if (use[y][x] == 0) {
                        check(a == 0, name + ": nothing is drawn outside the used rectangles (" + x + "," + y + " has alpha " + a + ")");
                    } else if (use[y][x] == 1) {
                        check(a == 255, name + ": the base layer is opaque (" + x + "," + y + " has alpha " + a + ")");
                    } else {
                        check(a == 0 || a == 255, name + ": the overlay has no half-transparent pixel (" + x + "," + y + " has alpha " + a + ")");
                    }
                }
            }
            check(alpha(skin, 47, 25) == 255, name + " has the classic four-pixel arm (column 47 of the arm's front is opaque)");
            check(alpha(skin, 55, 25) == 255, name + ": the arm's back is the classic width too");
        }
        helper.succeed();
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void theEyesAreWhereEveryLookPutsThemAndTheHelmetLeavesThemClear(GameTestHelper helper) throws IOException {
        for (Variant look : looks()) {
            BufferedImage skin = skin(look);
            String name = look.id();
            // Two eyes, two pixels wide, on columns 9-10 and 13-14 of the face, rows 11-12: a dark pupil on the outside,
            // a blue iris on the inside.
            for (int[] eye : new int[][]{{9, 10}, {14, 13}}) {
                int pupil = skin.getRGB(eye[0], 12);
                int iris = skin.getRGB(eye[1], 12);
                int pr = pupil >> 16 & 255, pg = pupil >> 8 & 255, pb = pupil & 255;
                int ir = iris >> 16 & 255, ig = iris >> 8 & 255, ib = iris & 255;
                check(pr + pg + pb < 150, name + ": the pupil at " + eye[0] + ",12 is dark");
                check(ib > ir + 40 && ib > 180, name + ": the iris at " + eye[1] + ",12 is blue");
            }
            // The overlay over the eyes (the hat layer is 32 columns to the right) must be clear, so that a glow mask
            // that lines up with the eyes works on every look.
            for (int x : new int[]{41, 42, 45, 46}) {
                for (int y : new int[]{11, 12}) {
                    check(alpha(skin, x, y) == 0, name + ": the hat layer leaves the eye pixel " + x + "," + y + " clear");
                }
            }
        }
        helper.succeed();
    }

    // ---- the boss's texture
    /** Every box of the Titan's model: u, v, width, height, depth (ThanosModel.createBodyLayer). */
    private static final int[][] BOSS = {
            {43, 23, 9, 9, 9}, {25, 116, 7, 3, 3}, {41, 75, 10, 3, 10}, {0, 60, 2, 5, 9}, {47, 43, 2, 9, 6}, {64, 43, 2, 9, 6}, {0, 116, 10, 5, 2},
            {0, 23, 14, 12, 7}, {23, 60, 12, 3, 10}, {0, 43, 15, 8, 8}, {74, 103, 15, 3, 8}, {84, 116, 4, 3, 1},
            {0, 0, 7, 15, 7}, {81, 43, 8, 6, 8}, {68, 60, 10, 3, 10}, {0, 103, 9, 3, 9},
            {29, 0, 7, 15, 7}, {64, 89, 10, 2, 10}, {80, 23, 9, 9, 9}, {35, 89, 7, 6, 7}, {0, 75, 10, 3, 10}, {37, 103, 9, 3, 9},
            {58, 0, 7, 13, 7}, {46, 116, 7, 4, 2}, {82, 75, 8, 4, 9}, {87, 0, 7, 13, 7}, {65, 116, 7, 4, 2}, {0, 89, 8, 4, 9},
    };
    /** The sword: grip, guard, two blades, two spines (ThanosSwordModel). */
    private static final int[][] SWORD = {{40, 0, 2, 8, 2}, {0, 24, 8, 3, 3}, {0, 0, 4, 22, 1}, {11, 0, 4, 22, 1}, {22, 0, 2, 20, 2}, {31, 0, 2, 20, 2}};

    private static int opaque(BufferedImage image, int[] f) {
        int n = 0;
        for (int y = f[1]; y < f[1] + f[3]; y++) {
            for (int x = f[0]; x < f[0] + f[2]; x++) {
                if (alpha(image, x, y) == 255) {
                    n++;
                }
            }
        }
        return n;
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void everyBoxOfTheTitanAndOfHisSwordHasAPaintedFace(GameTestHelper helper) throws IOException {
        BufferedImage body = image("textures/entity/thanos.png");
        check(body.getWidth() == 128 && body.getHeight() == 128, "the Titan's texture is 128 by 128");
        // The breastplate's back is clear on purpose: it lies in the plane of the body's back, and two opaque faces in one
        // plane flicker. Every other face of every box is painted in full.
        int[] breastplateBack = faces(0, 43, 15, 8, 8)[5];
        for (int[] box : BOSS) {
            for (int[] f : faces(box[0], box[1], box[2], box[3], box[4])) {
                int painted = opaque(body, f);
                if (f[0] == breastplateBack[0] && f[1] == breastplateBack[1]) {
                    check(painted == 0, "the breastplate's back face is clear (" + painted + " painted)");
                } else {
                    check(painted == f[2] * f[3], "the face at " + f[0] + "," + f[1] + " (" + f[2] + "x" + f[3] + ") is painted in full: " + painted);
                }
            }
        }
        BufferedImage sword = image("textures/entity/thanos_sword.png");
        check(sword.getWidth() == 64 && sword.getHeight() == 32, "the sword's texture is 64 by 32");
        for (int[] box : SWORD) {
            for (int[] f : faces(box[0], box[1], box[2], box[3], box[4])) {
                check(opaque(sword, f) == f[2] * f[3], "the sword's face at " + f[0] + "," + f[1] + " is painted in full");
            }
        }
        // Nothing is painted where no box reads it.
        boolean[][] read = new boolean[128][128];
        for (int[] box : BOSS) {
            for (int[] f : faces(box[0], box[1], box[2], box[3], box[4])) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        read[y][x] = true;
                    }
                }
            }
        }
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                check(read[y][x] || alpha(body, x, y) == 0, "the pixel " + x + "," + y + " is read by a box or left clear");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void theGauntletItemIsAnAnimatedStripAndTheEffectIconsAreDrawn(GameTestHelper helper) throws IOException {
        BufferedImage strip = image("textures/item/infinity_gauntlet.png");
        check(strip.getWidth() == 16 && strip.getHeight() == 64, "the Gauntlet is four frames of 16 by 16: " + strip.getWidth() + "x" + strip.getHeight());
        JsonObject meta = JsonParser.parseString(Files.readString(file("textures/item/infinity_gauntlet.png.mcmeta"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject animation = meta.getAsJsonObject("animation");
        check(animation.get("frametime").getAsInt() == 3 && animation.get("interpolate").getAsBoolean(), "frametime 3, interpolated");
        long different = 0;
        for (int frame = 1; frame < 4; frame++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    if (strip.getRGB(x, y) != strip.getRGB(x, frame * 16 + y)) {
                        different++;
                    }
                }
            }
        }
        check(different > 12, "the gems pulse: the frames differ (" + different + " pixels)");
        for (String icon : List.of("petrified", "soulworld", "time_locked")) {
            BufferedImage image = image("textures/mob_effect/" + icon + ".png");
            check(image.getWidth() == 18 && image.getHeight() == 18, icon + " is an 18 by 18 icon");
            int drawn = 0;
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 18; x++) {
                    if (alpha(image, x, y) > 0) {
                        drawn++;
                    }
                }
            }
            check(drawn > 150 && drawn < 324, icon + " is a drawn shape, not a flat square or an empty icon (" + drawn + " pixels)");
        }
        helper.succeed();
    }

    // ---- the sounds
    /** The sound events of the fight and the files each plays, as the lane registered them. */
    private static final Map<String, List<String>> SOUNDS = Map.ofEntries(
            Map.entry("thanos_summon", List.of("thanos_summon")), Map.entry("thanos_bell", List.of("thanos_bell")),
            Map.entry("thanos_arrive", List.of("thanos_arrive")), Map.entry("thanos_roar", List.of("thanos_roar")),
            Map.entry("thanos_step", List.of("thanos_step_1", "thanos_step_2", "thanos_step_3")),
            Map.entry("thanos_swing", List.of("thanos_swing_1", "thanos_swing_2")),
            Map.entry("thanos_hit", List.of("thanos_hit_1", "thanos_hit_2")),
            Map.entry("thanos_hurt", List.of("thanos_hurt_1", "thanos_hurt_2", "thanos_hurt_3")),
            Map.entry("thanos_death", List.of("thanos_death")), Map.entry("gauntlet_clench", List.of("gauntlet_clench")),
            Map.entry("thanos_stone_surge", List.of("thanos_stone_surge")), Map.entry("thanos_power_blast", List.of("thanos_power_blast")),
            Map.entry("thanos_infinity_charge", List.of("thanos_infinity_charge")), Map.entry("thanos_infinity_beam", List.of("thanos_infinity_beam")),
            Map.entry("thanos_meteor_whistle", List.of("thanos_meteor_whistle")), Map.entry("thanos_barrier_up", List.of("thanos_barrier_up")),
            Map.entry("time_rewind", List.of("time_rewind")), Map.entry("soul_drain", List.of("soul_drain_loop")),
            Map.entry("soul_world", List.of("soul_world")), Map.entry("reality_pop", List.of("reality_pop_1", "reality_pop_2", "reality_pop_3")),
            Map.entry("thanos_theme_1", List.of("thanos_theme_1")), Map.entry("thanos_theme_2", List.of("thanos_theme_2")),
            Map.entry("thanos_theme_3", List.of("thanos_theme_3")), Map.entry("thanos_theme_4", List.of("thanos_theme_4")),
            Map.entry("thanos_theme_5", List.of("thanos_theme_5")));
    private static final Set<String> PREFIXES = Set.of("thanos_", "gauntlet_", "soul_", "time_", "reality_");

    /** The format and length of an Ogg Vorbis file: channels, sample rate and samples, from its headers. */
    private record Ogg(int channels, int rate, long samples) {
    }

    private static Ogg ogg(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        check(data.length > 100 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S', path.getFileName() + " is an Ogg file");
        int id = -1;
        for (int i = 0; i < 120; i++) {
            if (data[i] == 1 && data[i + 1] == 'v' && data[i + 2] == 'o' && data[i + 3] == 'r' && data[i + 4] == 'b' && data[i + 5] == 'i' && data[i + 6] == 's') {
                id = i;
                break;
            }
        }
        check(id >= 0, path.getFileName() + " has a Vorbis identification header");
        int channels = data[id + 11] & 255;
        int rate = (data[id + 12] & 255) | (data[id + 13] & 255) << 8 | (data[id + 14] & 255) << 16 | (data[id + 15] & 255) << 24;
        int last = -1;
        for (int i = data.length - 14; i >= 0; i--) {
            if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') {
                last = i;
                break;
            }
        }
        long samples = 0;
        for (int b = 7; b >= 0; b--) {
            samples = samples << 8 | (data[last + 6 + b] & 255);
        }
        return new Ogg(channels, rate, samples);
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void everySoundOfTheFightIsRegisteredWithItsFilesItsSubtitlesAndTheRightFormat(GameTestHelper helper) throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(file("sounds.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject en = JsonParser.parseString(Files.readString(file("lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject he = JsonParser.parseString(Files.readString(file("lang/he_il.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        check(SOUNDS.size() == 25, "the fight has 25 sound events");
        for (Map.Entry<String, List<String>> sound : SOUNDS.entrySet()) {
            String event = sound.getKey();
            check(PREFIXES.stream().anyMatch(event::startsWith), event + " starts with one of the lane's sound prefixes");
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(event)), event + " is a registered sound event");
            check(sounds.has(event), event + " is in sounds.json");
            JsonObject definition = sounds.getAsJsonObject(event);
            String subtitle = definition.get("subtitle").getAsString();
            check(subtitle.equals("subtitles.enhanced." + event), event + " has its own subtitle key");
            check(en.has(subtitle) && he.has(subtitle) && !he.get(subtitle).getAsString().isBlank(), event + " has a subtitle in English and in Hebrew");
            List<String> files = new ArrayList<>();
            for (JsonElement entry : definition.getAsJsonArray("sounds")) {
                JsonObject o = entry.getAsJsonObject();
                files.add(o.get("name").getAsString());
                boolean theme = event.startsWith("thanos_theme_");
                check(o.has("stream") && o.get("stream").getAsBoolean() == theme || !o.has("stream") && !theme,
                        event + (theme ? ": a theme is streamed" : ": a short sound is not streamed"));
            }
            check(files.equals(sound.getValue().stream().map(f -> Enhanced.MODID + ":sfx/" + f).toList()), event + " plays " + sound.getValue() + ", not " + files);
            for (String name : sound.getValue()) {
                Ogg ogg = ogg(file("sounds/sfx/" + name + ".ogg"));
                check(ogg.channels() == 1 && ogg.rate() == 44100, name + " is mono at 44.1 kHz: " + ogg);
                double seconds = ogg.samples() / (double) ogg.rate();
                check(seconds > 0.2 && seconds < 61.0, name + " has a sensible length: " + seconds);
                if (event.startsWith("thanos_theme_")) {
                    check(Math.abs(seconds - 40.0) < 0.05, name + " is a 40 second loop: " + seconds);
                }
                if (event.equals("soul_drain")) {
                    check(Math.abs(seconds - 2.0) < 0.05, name + " is a 2 second loop: " + seconds);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void everyCueOfTheFightPlaysAnEventOfTheModAndOnlyThreeAreBorrowed(GameTestHelper helper) {
        Set<ThanosSounds> borrowed = Set.of(ThanosSounds.PORTAL, ThanosSounds.DISSOLVE, ThanosSounds.DEFLECT);
        for (ThanosSounds cue : ThanosSounds.values()) {
            ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(cue.event());
            check(id != null && id.getNamespace().equals(Enhanced.MODID), cue + " plays a sound event of the mod: " + id);
            if (borrowed.contains(cue)) {
                check(!SOUNDS.containsKey(id.getPath()), cue + " reuses another hero's sound: " + id);
            } else {
                check(SOUNDS.containsKey(id.getPath()), cue + " has a sound of its own: " + id);
            }
        }
        helper.succeed();
    }
}
