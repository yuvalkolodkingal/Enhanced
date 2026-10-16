package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.network.OpenRoadPayload;
import com.kolodkingal.enhanced.network.RoadChoicePayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadVoice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** The Road's text, messages and options: everything that is said is said in English and Hebrew, and the two screens' messages travel intact. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadDataTests {
    static final String BATCH = "road_parkour";

    private static JsonObject lang(String name) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(RoadDataTests.class.getResourceAsStream("/assets/enhanced/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everythingTheRoadSaysIsSaidInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> keys = new java.util.ArrayList<>(List.of("message.enhanced.road.off", "message.enhanced.road.already", "message.enhanced.road.mortals",
                "message.enhanced.road.walked", "message.enhanced.road.full", "message.enhanced.road.failed", "message.enhanced.road.opening", "message.enhanced.road.powers",
                "message.enhanced.road.no_teleport", "message.enhanced.road.no_item", "message.enhanced.road.keep_gene", "message.enhanced.road.leave_ask",
                "message.enhanced.road.trial_title", "message.enhanced.road.checkpoint", "message.enhanced.road.clock", "message.enhanced.road.time_up",
                "message.enhanced.road.won", "message.enhanced.road.announce", "message.enhanced.road.walked_all", "message.enhanced.road.walked_all.hint",
                "screen.enhanced.road.walk", "screen.enhanced.road.walk.desc", "screen.enhanced.road.progress"));
        for (String event : new String[]{RoadVoice.ENTER, RoadVoice.START, RoadVoice.CHECKPOINT, RoadVoice.FALL, RoadVoice.WON, RoadVoice.LEAVE, RoadVoice.BACK}) {
            for (int variant = 0; variant < RoadVoice.VARIANTS; variant++) {
                keys.add(RoadVoice.key(event, variant));
            }
        }
        for (RoadChallenge challenge : RoadChallenges.all()) {
            keys.add("road.enhanced.challenge." + challenge.id());
            keys.add("road.enhanced.challenge." + challenge.id() + ".goal");
        }
        for (String command : new String[]{"status", "inside", "outside", "enter", "enter.none", "leave", "not_in", "reset", "skip", "set", "cells", "layout", "layout.checkpoint",
                "layout.none"}) {
            keys.add("commands.enhanced.road." + command);
        }
        for (String key : keys) {
            check(en.has(key) && !en.get(key).getAsString().isBlank(), "English: " + key);
            check(he.has(key) && !he.get(key).getAsString().isBlank(), "Hebrew: " + key);
            // The same %s in both.
            check(count(en.get(key).getAsString()) == count(he.get(key).getAsString()), key + " takes the same arguments in both languages");
        }
        // The goal of a trial is a subtitle: short enough to be drawn whole.
        for (RoadChallenge challenge : RoadChallenges.all()) {
            String key = "road.enhanced.challenge." + challenge.id() + ".goal";
            check(en.get(key).getAsString().length() <= Powers.SUBTITLE_CHARS && he.get(key).getAsString().length() <= Powers.SUBTITLE_CHARS, key + " fits under a title");
        }
        // The Road does not speak the same line twice in a row.
        for (String event : new String[]{RoadVoice.ENTER, RoadVoice.CHECKPOINT, RoadVoice.FALL}) {
            check(en.get(RoadVoice.key(event, 0)).getAsString() != en.get(RoadVoice.key(event, 1)).getAsString(), "three different lines for " + event);
        }
        helper.succeed();
    }

    private static int count(String text) {
        Matcher matcher = Pattern.compile("%s").matcher(text);
        int n = 0;
        while (matcher.find()) {
            n++;
        }
        return n;
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theScreensMessagesTravelIntact(GameTestHelper helper) {
        for (boolean trials : new boolean[]{true, false}) {
            OpenRoadPayload open = new OpenRoadPayload(new BlockPos(-5, 70, 12), trials, 2, 3);
            ByteBuf buf = Unpooled.buffer();
            OpenRoadPayload.STREAM_CODEC.encode(buf, open);
            check(OpenRoadPayload.STREAM_CODEC.decode(buf).equals(open) && buf.readableBytes() == 0, "the screen's message: " + open);
        }
        for (Power power : new Power[]{Power.NONE, Power.BILLY, Power.TOMMY}) {
            RoadChoicePayload choice = new RoadChoicePayload(new BlockPos(1, 2, 3), power);
            ByteBuf buf = Unpooled.buffer();
            RoadChoicePayload.STREAM_CODEC.encode(buf, choice);
            check(RoadChoicePayload.STREAM_CODEC.decode(buf).equals(choice), "the answer " + power + " (NONE is \"Walk the Road\")");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRoadsOptionsAreInTheServerConfigWithTheirDefaults(GameTestHelper helper) {
        check(WitchesRoadConfig.ENABLED.get() && WitchesRoadConfig.RESUME_ON_LOGIN.get() && WitchesRoadConfig.RESUME_AT_CHECKPOINT.get(), "the trials are on, and resume");
        check(WitchesRoadConfig.MAX_PLAYERS.get() == 32 && WitchesRoadConfig.FALL_DEPTH.get() == 12 && WitchesRoadConfig.LEAVE_CONFIRM_SECONDS.get() == 5, "numbers");
        check(WitchesRoadConfig.PARKOUR_PADS.get() == 40 && WitchesRoadConfig.PARKOUR_DIFFICULTY.get() == 2 && WitchesRoadConfig.PARKOUR_CHECKPOINT_EVERY.get() == 6
                && WitchesRoadConfig.PARKOUR_TIME_LIMIT_SECONDS.get() == 0, "the parkour");
        check(WitchesRoadConfig.BUILD_BLOCKS_PER_TICK.get() == 20000 && WitchesRoadConfig.REMOVE_BLOCKS_PER_TICK.get() == 20000, "the slices");
        check(WitchesRoadConfig.COURSE_SEED.get() == 0L && WitchesRoadConfig.ANNOUNCE.get() && WitchesRoadConfig.VOICE.get() && WitchesRoadConfig.HEAL_AT_CHECKPOINT.get(), "the rest");
        check(RoadChallenges.count() >= 1 && RoadChallenges.get(0).id().equals("parkour"), "trial 1 is the parkour");
        helper.succeed();
    }

    private RoadDataTests() {
    }
}
