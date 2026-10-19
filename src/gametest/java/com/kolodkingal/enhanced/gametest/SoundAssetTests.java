package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The sounds of 3.5 and 3.6, read from the files the mod ships (3.5 PLAN 12, L6; 3.6 PLAN 10/17.3): every sound event
 * of Asgard, Heimdall, Odin, Hela and the Asgardians (3.5 Appendix C), the music lane's three songs, and every sound
 * event of 3.6 (with its subtitle in English and Hebrew) is registered, listed in sounds.json and plays files that are
 * real sounds: not the skeleton's 0.2-second placeholder, longer than half a second, Ogg Vorbis, mono at 44.1 kHz. The
 * format is read from the Vorbis identification header and the length from the last page's granule position, as {@code
 * ThanosAssetsTests} does. Nothing here names a client class.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SoundAssetTests {
    private static final String BATCH = "sound_assets";
    /** The md5 of the placeholder file the 3.5 skeleton gave every new sound event. */
    static final String PLACEHOLDER_MD5 = "4397d2524960e0ff29ed281d13d82f90";

    /**
     * The 3.6 skeleton's placeholder files ({@code sfx/<name>.ogg}, the same 0.2-second file as 3.5's): every sound
     * event of 3.6 until the sound lane (L6 of 3.6) makes it. L6 empties this list; the test then holds for every file
     * again.
     */
    static final Set<String> SKELETON_36 = Set.of();
    /**
     * The 3.7 skeleton's placeholder files ({@code sfx/<name>.ogg}, the same 0.2-second file): every sound event of 3.7
     * (PLAN Appendix C) until the sound lane (L7 of 3.7) makes it. L7 empties this list and extends the tests to the
     * 3.7 events.
     */
    static final Set<String> SKELETON_37 = Set.of(
            "healing_down", "healing_rise", "healing_burst", "healing_off", "healing_on", "healing_cut",
            "healing_rescue", "wolverine_snikt", "wolverine_shink", "wolverine_slash", "wolverine_slash_hit_metal",
            "wolverine_spin", "wolverine_pounce", "wolverine_land", "wolverine_berserk", "wolverine_growl",
            "wolverine_sniff", "wolverine_track", "wolverine_climb", "adamantium_clank", "adamantium_injection",
            "wolverine_awaken", "deadpool_katana", "deadpool_parry", "deadpool_deflect", "deadpool_shot",
            "deadpool_reload", "deadpool_ricochet", "deadpool_blink", "deadpool_glitch", "deadpool_drop_in",
            "deadpool_taunt", "deadpool_whistle", "deadpool_smoke", "deadpool_awaken", "dogpool_bark", "dogpool_whine",
            "dogpool_poof", "chimichanga_munch", "school_lab_ready", "secret_shelf_open", "secret_shelf_close",
            "lift_ride", "lift_ding", "x_door_open", "x_door_close", "weapon_x_pod_hum", "weapon_x_pod_done",
            "adamantium_rack_take", "cure_station", "ambient_school_lab", "cerebro_console_on", "cerebro_amplified",
            "cerebro_console_off", "locator_pulse", "locator_tick", "locator_found", "ambient_cottage", "cottage_pot",
            "scratchy_found", "rio_blessing", "nicky_cheer", "agatha_away");
    /** The sound events of PLAN Appendix C (72; {@code music.asgard} plays three files). */
    static final List<String> ASGARD_EVENTS = List.of(
            // Asgard
            "music.asgard", "ambient.asgard.loop", "ambient.asgard.additions", "asgard_falls", "bifrost_mechanism_spin",
            "rune_stone_call", "throne_sit", "coronation", "mural_crack", "seal_break",
            // Heimdall
            "heimdall_slash", "heimdall_lunge", "heimdall_sight_on", "heimdall_sight_off", "heimdall_farsight",
            "heimdall_ward_up", "heimdall_ward_down", "heimdall_call", "bifrost_strike", "hofund_return", "watch_start",
            "watch_win", "watch_lose", "shadow_struck", "music.watch",
            // Odin
            "gungnir_blast", "gungnir_throw", "gungnir_catch", "odinforce_shield_up", "odinforce_shield_down",
            "odinforce_heal", "banish", "odinforce_wave", "raven_caw", "raven_flap", "odin_command", "odinsleep_start",
            "odinsleep_end",
            // Hela
            "hela_arrive", "hela_voice", "necrosword_throw", "necrosword_hit", "blade_wall", "blade_rain",
            "blade_storm", "hela_shadow_step", "hela_channel", "hela_channel_break", "hela_form_on", "hela_form_off",
            "hela_grasp",
            "mjolnir_caught", "hela_defeat", "berserker_rise", "berserker_ambient", "berserker_hurt", "berserker_death",
            "fenris_growl", "fenris_howl", "fenris_bite", "fenris_hurt", "fenris_death", "music.hela_1", "music.hela_2",
            "music.hela_3",
            // the Asgardians
            "npc_talk_low", "npc_talk_mid", "npc_talk_high", "npc_spar_clash", "npc_bow", "npc_down",
            "einherjar_summon");
    /** The music lane's songs of 3.5 (the Road's choice, the speedsters' Walkman themes). */
    static final List<String> MUSIC_EVENTS = List.of("music.road.choice", "music.quicksilver", "music.speed");

    /**
     * The sound events of 3.6 (83, PLAN Appendix C and 16.6): the stones, Doctor Strange, the stones' sites, Vormir,
     * the Hulk and Hydra ({@code time_rewind} is Thanos's, not counted again). {@code mind_beam_loop} and {@code
     * soul_drain_loop} play Vision's and Thanos's files.
     */
    static final List<String> INFINITY_EVENTS = List.of(
            // the stones
            "stone_reject", "stone_slip", "stone_grace_tick", "reality_illusion", "mind_beam_loop", "mind_command",
            "time_slow_on", "time_slow_off", "time_slow_loop", "time_crop", "power_beam_loop", "power_explosion",
            "power_bolt", "space_step", "space_realm_depart", "space_realm_arrive", "soul_drain_loop", "soul_sight",
            "orb_open", "pedestal_take", "gauntlet_insert", "gauntlet_select", "snap_charge", "snap", "snap_dust",
            // Doctor Strange
            "strange_awaken", "strange_whip", "strange_grapple", "strange_shield_up", "strange_shield_down",
            "strange_dome", "strange_bands", "strange_astral_out", "strange_astral_in", "strange_portal_open",
            "strange_portal_close", "strange_portal_pass", "strange_mirror_in", "strange_mirror_out",
            "strange_mirror_loop", "eye_open", "eye_close", "cloak_flap", "sling_ring_save",
            // the stones' sites
            "ambient.sanctum", "end_tunnel_hum",
            // Vormir
            "vormir_wind", "soul_sacrifice", "stonekeeper_voice",
            // the Hulk
            "hulk_transform_out", "hulk_transform_in", "hulk_switch", "hulk_roar", "hulk_smash", "hulk_grab",
            "hulk_slam", "hulk_clap", "hulk_leap", "hulk_land", "hulk_rip", "hulk_throw", "boulder_hit",
            "hulk_charge", "hulk_step", "hulk_hurt", "banner_heartbeat", "gamma_emitter_warmup",
            "gamma_emitter_beam", "gamma_exposure",
            // Hydra
            "hydra_rifle_burst", "hydra_bolt_hit", "hydra_sniper_aim", "hydra_sniper_shot", "hydra_pistol",
            "hydra_baton", "hydra_shield_block", "hydra_bark", "hydra_radio", "hydra_alarm", "hydra_reload",
            "hydra_hurt", "hydra_death", "hydra_blaster");
    /** The 3.6 ambience loops (played as looping sounds while one is there): long, seamless. */
    static final List<String> AMBIENCE_LOOPS_36 = List.of("ambient.sanctum", "end_tunnel_hum", "vormir_wind");

    private static IModFile mod() {
        return ModList.get().getModFileById(Enhanced.MODID).getFile();
    }

    private static Path file(String path) {
        Path file = mod().findResource(("assets/" + Enhanced.MODID + "/" + path).split("/"));
        check(Files.isRegularFile(file), path + " exists");
        return file;
    }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(Files.readString(file(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    static String md5(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The format and length of an Ogg Vorbis file: channels, sample rate and samples, from its headers. */
    record Ogg(int channels, int rate, long samples) {
        double seconds() {
            return rate == 0 ? 0 : samples / (double) rate;
        }
    }

    static Ogg ogg(String name, byte[] data) {
        check(data.length > 100 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S',
                name + " is an Ogg file");
        int id = -1;
        for (int i = 0; i < 120; i++) {
            if (data[i] == 1 && data[i + 1] == 'v' && data[i + 2] == 'o' && data[i + 3] == 'r' && data[i + 4] == 'b'
                    && data[i + 5] == 'i' && data[i + 6] == 's') {
                id = i;
                break;
            }
        }
        check(id >= 0, name + " has a Vorbis identification header");
        int channels = data[id + 11] & 255;
        int rate = (data[id + 12] & 255) | (data[id + 13] & 255) << 8 | (data[id + 14] & 255) << 16
                | (data[id + 15] & 255) << 24;
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

    /** The files an event of sounds.json plays ("enhanced:sfx/heimdall_slash" -> "sounds/sfx/heimdall_slash.ogg"). */
    private static List<String> files(JsonObject sounds, String event) {
        check(sounds.has(event), event + " is in sounds.json");
        List<String> files = new ArrayList<>();
        for (JsonElement entry : sounds.getAsJsonObject(event).getAsJsonArray("sounds")) {
            String name = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString()
                    : entry.getAsString();
            check(name.startsWith(Enhanced.MODID + ":"), event + " plays a file of the mod: " + name);
            files.add("sounds/" + name.substring(Enhanced.MODID.length() + 1) + ".ogg");
        }
        check(!files.isEmpty(), event + " plays a file");
        return files;
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void everySoundOf35IsARealMonoSoundLongerThanHalfASecond(GameTestHelper helper) throws IOException {
        check(ASGARD_EVENTS.size() == 72, "Appendix C has 72 sound events: " + ASGARD_EVENTS.size());
        JsonObject sounds = json("sounds.json");
        JsonObject en = json("lang/en_us.json");
        JsonObject he = json("lang/he_il.json");
        List<String> events = new ArrayList<>(ASGARD_EVENTS);
        events.addAll(MUSIC_EVENTS);
        int count = 0;
        for (String event : events) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(event)),
                    event + " is a registered sound event");
            if (ASGARD_EVENTS.contains(event)) {
                String subtitle = "subtitles.enhanced." + event;
                JsonElement own = sounds.getAsJsonObject(event).get("subtitle");
                check(own != null && own.getAsString().equals(subtitle), event + " has its own subtitle key");
                check(en.has(subtitle) && he.has(subtitle) && !he.get(subtitle).getAsString().isBlank(),
                        event + " has a subtitle in English and in Hebrew");
            }
            for (String path : files(sounds, event)) {
                byte[] data = Files.readAllBytes(file(path));
                check(!md5(data).equals(PLACEHOLDER_MD5), path + " is not the skeleton's placeholder");
                Ogg ogg = ogg(path, data);
                check(ogg.channels() == 1 && ogg.rate() == 44100, path + " is mono at 44.1 kHz: " + ogg);
                check(ogg.seconds() > 0.5, path + " is longer than half a second: " + ogg.seconds());
                count++;
            }
        }
        check(count == 77, "74 files of Appendix C and the music lane's 3: " + count);
        helper.succeed();
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void everySoundOf36IsARealMonoSoundWithItsSubtitles(GameTestHelper helper) throws IOException {
        check(INFINITY_EVENTS.size() == 83 && Set.copyOf(INFINITY_EVENTS).size() == 83,
                "3.6 has 83 sound events: " + INFINITY_EVENTS.size());
        JsonObject sounds = json("sounds.json");
        JsonObject en = json("lang/en_us.json");
        JsonObject he = json("lang/he_il.json");
        int count = 0;
        for (String event : INFINITY_EVENTS) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(event)),
                    event + " is a registered sound event");
            String subtitle = "subtitles.enhanced." + event;
            JsonElement own = sounds.getAsJsonObject(event) == null ? null
                    : sounds.getAsJsonObject(event).get("subtitle");
            check(own != null && own.getAsString().equals(subtitle), event + " has its own subtitle key");
            check(en.has(subtitle) && !en.get(subtitle).getAsString().isBlank(), event + " has an English subtitle");
            check(he.has(subtitle) && he.get(subtitle).getAsString().chars().anyMatch(c -> c >= 0x05D0 && c <= 0x05EA),
                    event + " has a Hebrew subtitle");
            for (String path : files(sounds, event)) {
                byte[] data = Files.readAllBytes(file(path));
                check(!md5(data).equals(PLACEHOLDER_MD5), path + " is not the skeleton's placeholder");
                Ogg ogg = ogg(path, data);
                check(ogg.channels() == 1 && ogg.rate() == 44100, path + " is mono at 44.1 kHz: " + ogg);
                check(ogg.seconds() > 0.5, path + " is longer than half a second: " + ogg.seconds());
                if (AMBIENCE_LOOPS_36.contains(event)) {
                    check(ogg.seconds() >= 15.0, path + " is a long ambience loop: " + ogg.seconds());
                }
                count++;
            }
        }
        check(count == 83, "one file for each 3.6 event: " + count);
        helper.succeed();
    }

    @GameTest(template = TestKit.ARENA, batch = BATCH)
    public static void noSoundFileOfTheModIsThePlaceholder(GameTestHelper helper) throws IOException {
        Path root = mod().findResource("assets", Enhanced.MODID, "sounds");
        check(Files.isDirectory(root), "the mod has its sounds folder");
        List<String> placeholders = new ArrayList<>();
        int count = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.filter(p -> p.getFileName().toString().endsWith(".ogg")).toList()) {
                count++;
                String name = root.relativize(path).toString();
                if (md5(Files.readAllBytes(path)).equals(PLACEHOLDER_MD5)
                        && !SKELETON_36.contains(name.replace("sfx/", "").replace(".ogg", ""))
                        && !SKELETON_37.contains(name.replace("sfx/", "").replace(".ogg", ""))) {
                    placeholders.add(name);
                }
            }
        }
        check(count > 200, "the mod's sound files were found: " + count);
        check(placeholders.isEmpty(), "no file is still the placeholder: " + placeholders);
        helper.succeed();
    }

    private SoundAssetTests() {
    }
}
