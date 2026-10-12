package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the first real game of the integrated 3.0 (a dedicated server and two clients) showed about the Phoenix and
 * Spider-Man that the other tests did not, each pinned by a test that fails on the code it was found in. (A third finding,
 * a held Spider-Man's hand thrusting a web the server never shoots, is the client's alone and was checked in the game.)
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HeroesInGameTests {

    /** The longest title, in characters, that fits a 854-pixel-wide window at the default GUI scale (bold, four times the text). */
    private static final int TITLE_LIMIT = 14;

    private static JsonObject lang(String name) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    /**
     * The title of an awakening is drawn in bold four times the size of the text: "Jean Grey (Phoenix)" was wider than
     * the screen and cut at both ends ("ean Grey (Phoenix"). Every hero's awakening title fits, in both languages, and the
     * Phoenix's awakening really shows the shorter one.
     */
    @GameTest(template = ARENA, batch = "heroes_ingame_titles")
    public static void everyAwakeningTitleFitsTheScreen(GameTestHelper helper) throws IOException {
        List<JsonObject> languages = List.of(lang("en_us"), lang("he_il"));
        for (Power power : Power.values()) {
            if (!power.hasPowers()) {
                continue;
            }
            String key = key(power.awakeningTitle());
            for (JsonObject language : languages) {
                check(language.has(key), power + "'s awakening title " + key + " is translated");
                String text = language.get(key).getAsString();
                check(text.length() <= TITLE_LIMIT, power + "'s awakening title \"" + text + "\" has " + text.length()
                        + " characters (at most " + TITLE_LIMIT + " fit)");
            }
        }
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer jean = f.player(2.5, TestKit.FLOOR, 2.5);
        TestKit.sentPackets(jean);
        Powers.awaken(jean, Power.PHOENIX);
        List<String> titles = TestKit.sentPackets(jean).stream().filter(p -> p instanceof ClientboundSetTitleTextPacket)
                .map(p -> key(((ClientboundSetTitleTextPacket) p).text())).toList();
        check(titles.equals(List.of("power.enhanced.phoenix.title")), "the Phoenix wakes under her short title: " + titles);
        helper.succeed();
    }

    /** Mind Sense once said "Minds nearby: 3 (1 players, 1 hostile)": the counts read right for one as for many. */
    @GameTest(template = ARENA, batch = "heroes_ingame_words")
    public static void mindSenseCountsReadRightForOne(GameTestHelper helper) throws IOException {
        for (String language : List.of("en_us", "he_il")) {
            String line = String.format(lang(language).get("message.enhanced.mind_sense.result").getAsString(), 3, 1, 1);
            check(!line.contains("1 players"), language + ": " + line);
        }
        helper.succeed();
    }

    private HeroesInGameTests() {
    }
}
