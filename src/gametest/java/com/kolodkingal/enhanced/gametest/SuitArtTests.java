package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The suit art of 3.8 (PLAN 7, 14; lane L7): every suit has its texture and emissive texture, every 3.8 item and block
 * item its model and icon (the Mark 5 its suitcase), and the suit-ups' pure timelines hold: every part's reveal climbs
 * from 0 to exactly 1 at the end, the helmet is the last piece on, a take-off is the suit-up backwards and shorter, the
 * Mark 1's plates come on one after another, the nanites spread out of the reactor, the Hulkbuster's frame opens and
 * closes and the Mark 5's suitcase opens and folds away. Nothing here names a client class.
 */
public final class SuitArtTests {
    private static final String BATCH = "suit_art";
    /** Every item of 3.8 (and the four block items): each has a model and an icon of its own. */
    static final List<String> ITEMS = List.of("special_soldier_serum", "vibranium_shield", "falcon_exo_wings",
            "arc_reactor", "nano_arc_reactor", "iron_man_mark_1", "iron_man_mark_3", "iron_man_mark_5",
            "iron_man_mark_50", "iron_man_mark_85", "iron_man_hulkbuster", "suit_display", "tech_case",
            "eternity_altar", "eternity_mirror");
    private static final int STEPS = 400;

    /** A file of the mod's assets, or null if there is none. */
    private static Path file(String path) {
        return Platform.get().modResource("assets/" + Enhanced.MODID + "/" + path).orElse(null);
    }

    /** A PNG's width and height, from its header. */
    private static int[] size(String path) throws IOException {
        Path f = file(path);
        check(f != null && Files.isRegularFile(f), path + " exists");
        byte[] b = Files.readAllBytes(f);
        check(b.length > 24 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G', path + " is a PNG");
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | b[at + 3] & 0xFF;
    }

    private static JsonObject json(String path) throws IOException {
        Path f = file(path);
        check(f != null && Files.isRegularFile(f), path + " exists");
        return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** Every suit: its 128 x 128 texture and its emissive texture; the props' textures. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everySuitHasItsTextures(GameTestHelper helper) throws IOException {
        for (SuitKind kind : SuitKind.VALUES) {
            for (String name : new String[] {kind.id(), kind.id() + "_glow"}) {
                int[] wh = size("textures/entity/iron_man/" + name + ".png");
                check(wh[0] == 128 && wh[1] == 128, name + " is 128 x 128");
            }
        }
        for (String prop : new String[] {"suitcase", "suit_gantry"}) {
            size("textures/entity/iron_man/" + prop + ".png");
        }
        helper.succeed();
    }

    /**
     * Every 3.8 item and block item has an item model of its own whose textures are the mod's own 16 x 16 icons (none
     * of the skeleton's borrowed vanilla ones); the Mark 5 turns into its suitcase by the {@code enhanced:folded}
     * property.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyItemHasItsModelAndIcon(GameTestHelper helper) throws IOException {
        for (String item : ITEMS) {
            JsonObject model = json("models/item/" + item + ".json");
            List<String> textures = textures(model);
            check(!textures.isEmpty(), item + " has an icon");
            for (String texture : textures) {
                check(texture.startsWith(Enhanced.MODID + ":item/"), item + "'s icon " + texture + " is the mod's");
                int[] wh = size("textures/" + texture.substring(Enhanced.MODID.length() + 1) + ".png");
                check(wh[0] == 16 && wh[1] == 16, texture + " is 16 x 16");
            }
        }
        JsonObject mark5 = json("models/item/iron_man_mark_5.json");
        boolean folded = false;
        for (JsonElement o : mark5.getAsJsonArray("overrides")) {
            JsonObject override = o.getAsJsonObject();
            if (override.getAsJsonObject("predicate").has(Enhanced.MODID + ":folded")) {
                String target = override.get("model").getAsString();
                json("models/" + target.substring(Enhanced.MODID.length() + 1) + ".json");
                folded = true;
            }
        }
        check(folded, "the Mark 5 folds into its suitcase");
        helper.succeed();
    }

    /** The textures an item model names (its layers, or those of a separate-transforms model's base and gui). */
    private static List<String> textures(JsonObject model) throws IOException {
        List<String> out = new ArrayList<>();
        if (model.has("textures")) {
            for (var e : model.getAsJsonObject("textures").entrySet()) {
                if (!e.getKey().equals("particle")) {
                    out.add(e.getValue().getAsString());
                }
            }
        }
        if (model.has("perspectives")) {
            JsonObject gui = model.getAsJsonObject("perspectives").getAsJsonObject("gui");
            if (gui != null && gui.has("parent")) {
                String parent = gui.get("parent").getAsString();
                out.addAll(textures(json("models/" + parent.substring(Enhanced.MODID.length() + 1) + ".json")));
            }
        }
        return out;
    }

    /** Every part of every suit goes on from 0 to exactly 1, never back, and comes off from 1 to exactly 0. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRevealsClimbAndEndAtOne(GameTestHelper helper) {
        for (SuitKind kind : SuitKind.VALUES) {
            for (SuitParts part : SuitParts.VALUES) {
                String what = kind.id() + " " + part;
                check(SuitTimelines.reveal(kind, part, 0.0F, true) == 0.0F, what + " starts off");
                check(SuitTimelines.reveal(kind, part, 1.0F, true) == 1.0F, what + " is on at the end");
                check(SuitTimelines.reveal(kind, part, 0.0F, false) == 1.0F, what + " starts on (off)");
                check(SuitTimelines.reveal(kind, part, 1.0F, false) == 0.0F, what + " is off at the end");
                float last = 0.0F;
                float lastDown = 1.0F;
                for (int i = 0; i <= STEPS; i++) {
                    float t = i / (float) STEPS;
                    float r = SuitTimelines.reveal(kind, part, t, true);
                    float d = SuitTimelines.reveal(kind, part, t, false);
                    check(r >= last && r >= 0.0F && r <= 1.0F, what + " never goes back on the way up");
                    check(d <= lastDown && d >= 0.0F && d <= 1.0F, what + " never comes back on the way down");
                    last = r;
                    lastDown = d;
                }
            }
            check(SuitTimelines.lit(kind, 1.0F, true) == 1.0F && SuitTimelines.lit(kind, 0.5F, true) == 0.0F,
                    kind.id() + "'s eyes light at the end");
        }
        TestKit.assertNear(SuitTimelines.progress(110L, 100L, 20, 0.5F), 0.525, 1.0E-6, "the progress");
        check(SuitTimelines.progress(120L, 100L, 20, 0.0F) == 1.0F && SuitTimelines.progress(119L, 100L, 20, 0.0F)
                < 1.0F, "it reaches 1 exactly at the end");
        helper.succeed();
    }

    /** On every suit-up the helmet and its faceplate are the last pieces on (and so the first off). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHelmetIsTheLastPieceOn(GameTestHelper helper) {
        for (SuitKind kind : SuitKind.VALUES) {
            for (SuitParts part : SuitParts.VALUES) {
                if (part.head()) {
                    continue;
                }
                check(SuitTimelines.end(kind, part) <= SuitTimelines.end(kind, SuitParts.HELMET),
                        kind.id() + ": " + part + " is on before the helmet");
            }
            check(SuitTimelines.end(kind, SuitParts.HELMET) <= SuitTimelines.end(kind, SuitParts.FACEPLATE),
                    kind.id() + ": the faceplate closes last");
            for (int i = 0; i <= STEPS; i++) {
                float t = i / (float) STEPS;
                if (SuitTimelines.reveal(kind, SuitParts.FACEPLATE, t, true) >= 1.0F) {
                    for (SuitParts part : SuitParts.VALUES) {
                        check(SuitTimelines.reveal(kind, part, t, true) >= 1.0F, kind.id() + ": " + part
                                + " is on when the faceplate is");
                    }
                }
            }
        }
        helper.succeed();
    }

    /** A take-off is the suit-up played backwards, and shorter. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aTakeOffIsTheSuitUpBackwards(GameTestHelper helper) {
        for (SuitKind kind : SuitKind.VALUES) {
            check(kind.downTicks() < kind.upTicks(), kind.id() + " comes off faster than it goes on");
            for (SuitParts part : SuitParts.VALUES) {
                for (int i = 0; i <= STEPS; i++) {
                    float t = i / (float) STEPS;
                    TestKit.assertNear(SuitTimelines.reveal(kind, part, t, false),
                            SuitTimelines.reveal(kind, part, 1.0F - t, true), 1.0E-5,
                            kind.id() + " " + part + " backwards");
                }
            }
        }
        helper.succeed();
    }

    /** The Mark 1's plates clank on one after another: boots, shins, thighs, torso, arms, the helmet last. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMarkOnesPlatesComeOneAfterAnother(GameTestHelper helper) {
        SuitParts[] order = {SuitParts.RIGHT_BOOT, SuitParts.LEFT_BOOT, SuitParts.RIGHT_SHIN, SuitParts.LEFT_SHIN,
                SuitParts.RIGHT_THIGH, SuitParts.LEFT_THIGH, SuitParts.CHEST, SuitParts.BACK,
                SuitParts.RIGHT_UPPER_ARM, SuitParts.LEFT_UPPER_ARM, SuitParts.RIGHT_FOREARM, SuitParts.LEFT_FOREARM,
                SuitParts.RIGHT_GAUNTLET, SuitParts.LEFT_GAUNTLET, SuitParts.HELMET, SuitParts.FACEPLATE};
        for (int i = 1; i < order.length; i++) {
            check(SuitTimelines.start(SuitKind.MARK_1, order[i]) >= SuitTimelines.end(SuitKind.MARK_1, order[i - 1])
                    - 1.0E-6F, order[i] + " starts after " + order[i - 1] + " is on");
        }
        for (int i = 0; i <= STEPS; i++) {
            float t = i / (float) STEPS;
            int moving = 0;
            for (SuitParts part : SuitParts.VALUES) {
                float r = SuitTimelines.reveal(SuitKind.MARK_1, part, t, true);
                if (r > 0.0F && r < 1.0F) {
                    moving++;
                }
            }
            check(moving <= 1, "one plate at a time (" + moving + " at " + t + ")");
        }
        helper.succeed();
    }

    /**
     * The Mark 50's and Mark 85's nanites flow out of the reactor: the chest first, every segment of a part after the
     * one nearer the reactor, the last segment of each part on with the part, the helmet last.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theNanitesSpreadFromTheReactor(GameTestHelper helper) {
        for (SuitKind kind : new SuitKind[] {SuitKind.MARK_50, SuitKind.MARK_85}) {
            for (SuitParts part : SuitParts.VALUES) {
                check(SuitTimelines.start(kind, SuitParts.CHEST) <= SuitTimelines.start(kind, part),
                        kind.id() + ": the wave starts at the reactor before " + part);
                for (int count = 1; count <= 4; count++) {
                    for (int i = 0; i <= STEPS; i++) {
                        float t = i / (float) STEPS;
                        for (int k = 1; k < count; k++) {
                            check(SuitTimelines.segment(kind, part, k, count, t, true)
                                    <= SuitTimelines.segment(kind, part, k - 1, count, t, true),
                                    kind.id() + " " + part + ": segment " + k + " after " + (k - 1));
                        }
                        float lastSeg = SuitTimelines.segment(kind, part, count - 1, count, t, true);
                        check(lastSeg <= SuitTimelines.reveal(kind, part, t, true) + 1.0E-5F
                                || count == 1, kind.id() + " " + part + ": the last segment ends with the part");
                    }
                    check(SuitTimelines.segment(kind, part, count - 1, count, SuitTimelines.end(kind, part), true)
                            == 1.0F, kind.id() + " " + part + ": all " + count + " segments on at the part's end");
                }
            }
        }
        // the other suits' parts go on whole
        check(SuitTimelines.segment(SuitKind.MARK_3, SuitParts.CHEST, 1, 2, 0.3F, true)
                == SuitTimelines.reveal(SuitKind.MARK_3, SuitParts.CHEST, 0.3F, true), "a Mark 3 part is whole");
        helper.succeed();
    }

    /** The Hulkbuster's frame falls shut, opens, waits, closes; the Mark 5's suitcase snaps open and folds away. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theFrameAndTheSuitcaseOpenAndClose(GameTestHelper helper) {
        check(SuitTimelines.frameOpen(0.0F) == 0.0F && SuitTimelines.frameOpen(29.9F) == 0.0F, "shut as it falls");
        check(SuitTimelines.frameOpen(45.0F) == 1.0F, "open for him to step in");
        check(SuitTimelines.frameOpen(65.0F) == 0.0F && SuitTimelines.frameOpen(90.0F) == 0.0F, "shut round him");
        float last = 0.0F;
        for (float age = 30.0F; age <= 40.0F; age += 0.25F) {
            float o = SuitTimelines.frameOpen(age);
            check(o >= last, "it opens smoothly");
            last = o;
        }
        for (float age = 50.0F; age <= 65.0F; age += 0.25F) {
            float o = SuitTimelines.frameOpen(age);
            check(o <= last, "it closes smoothly");
            last = o;
        }
        float[] start = SuitTimelines.suitcase(0.0F, true);
        float[] middle = SuitTimelines.suitcase(0.5F, true);
        check(start[0] == 0.0F && start[1] == 1.0F, "the suitcase lies shut at his feet");
        check(middle[0] == 1.0F && middle[1] == 0.0F, "it opened and folded away into the suit");
        float[] down = SuitTimelines.suitcase(1.0F, false);
        check(down[0] == 0.0F && down[1] == 1.0F, "a take-off folds the suit back into a shut case");
        helper.succeed();
    }

    private SuitArtTests() {
    }
}
