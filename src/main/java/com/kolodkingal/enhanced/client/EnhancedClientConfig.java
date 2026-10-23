package com.kolodkingal.enhanced.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side switches for how the heroes move and look, and how Build Magic shows itself. Lives in
 * config/enhanced-client.toml; nothing here is synced.
 */
public final class EnhancedClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ModConfigSpec.BooleanValue FLIGHT_EFFECTS;
    public static final ModConfigSpec.BooleanValue HIDE_HERO_ARMOUR;
    public static final ModConfigSpec.BooleanValue BOSS_MUSIC;
    public static final ModConfigSpec.BooleanValue BOSS_ATMOSPHERE;
    public static final ModConfigSpec.BooleanValue BUILD_HINTS;
    public static final ModConfigSpec.BooleanValue BUILD_TIPS;
    public static final ModConfigSpec.IntValue BUILD_GHOST_DETAIL;
    public static final ModConfigSpec.BooleanValue BUILD_SEE_THROUGH;
    public static final ModConfigSpec.BooleanValue BUILD_SHOW_OTHERS;
    public static final ModConfigSpec.BooleanValue WEB_ASSIST;
    public static final ModConfigSpec.BooleanValue WEB_ASSIST_PREVIEW;
    public static final ModConfigSpec.ConfigValue<String> QUICKSILVER_SONG;
    public static final ModConfigSpec.BooleanValue IGNORE_SERVER_MUSIC;
    public static final ModConfigSpec.BooleanValue HULK_MODEL;
    public static final ModConfigSpec.BooleanValue DEADPOOL_LINES;
    public static final ModConfigSpec.BooleanValue SHOW_DEATH_MEETING;
    public static final ModConfigSpec.BooleanValue SUIT_UP_OVERLAY;
    public static final ModConfigSpec.BooleanValue IRON_HELMET_HUD;
    public static final ModConfigSpec.BooleanValue ETERNITY_REFLECTIONS;
    public static final ModConfigSpec.BooleanValue ETERNITY_FOG;

    static {
        BUILDER.push("animation");
        ANIMATIONS = BUILDER.comment("Animate the heroes: blended poses, flight, running, Thor's weapon. Off: vanilla poses only.")
                .translation("config.enhanced.animations")
                .define("animations", true);
        REDUCED_MOTION = BUILDER.comment("Calmer animation: hover bob and drift at a third, no trembling hands, jitter or camera shake, and fainter rings.")
                .translation("config.enhanced.reduced_motion")
                .define("reducedMotion", false);
        FLIGHT_EFFECTS = BUILDER.comment("Flight effects: ribbons, rings, wind streaks and shockwaves (trail particles are thinned out when off).")
                .translation("config.enhanced.flight_effects")
                .define("flightEffects", true);
        BUILDER.pop();
        BUILDER.push("appearance");
        HIDE_HERO_ARMOUR = BUILDER.comment("Don't draw the armour of heroes (yours in third person, and everyone else's): it only hides the characters.",
                        "It protects as usual. Elytra, mob heads, carved pumpkins and the hero gear of this mod are always drawn.")
                .translation("config.enhanced.hide_hero_armour")
                .define("hideHeroArmour", true);
        BUILDER.pop();
        // ===== thanos =====
        BUILDER.push("thanos");
        BOSS_MUSIC = BUILDER.comment("Play the music of the fight while Thanos is near. Off: silence.")
                .translation("config.enhanced.boss_music")
                .define("bossMusic", true);
        BOSS_ATMOSPHERE = BUILDER.comment("The violet fog and the dark edges of the screen while Thanos is near.")
                .translation("config.enhanced.boss_atmosphere")
                .define("bossAtmosphere", true);
        BUILDER.pop();
        // ===== 3.1 build =====
        BUILDER.push("build_magic");
        BUILD_HINTS = BUILDER.comment("The strip of mouse and key pictures while building.")
                .translation("config.enhanced.build_hints")
                .define("hints", true);
        BUILD_TIPS = BUILDER.comment("The little animated tips the first times a spell is used.")
                .translation("config.enhanced.build_tips")
                .define("tips", true);
        BUILD_GHOST_DETAIL = BUILDER.comment("The most blocks drawn in a spell's preview (its ghost); a bigger one is drawn as its outside only, then",
                        "as a tinted box.")
                .translation("config.enhanced.build_ghost_detail")
                .defineInRange("ghostDetail", 32768, 0, 262144);
        BUILD_SEE_THROUGH = BUILDER.comment("The box shows faintly through hills and walls.")
                .translation("config.enhanced.build_see_through")
                .define("seeThrough", true);
        BUILD_SHOW_OTHERS = BUILDER.comment("See the boxes of other builders near you.")
                .translation("config.enhanced.build_show_others")
                .define("showOthers", true);
        BUILDER.pop();
        // ===== 3.3 spider =====
        BUILDER.push("spider_man");
        WEB_ASSIST = BUILDER.comment("Web Assist: Spider-Man's swing chooses its own anchor ahead and above (trees count), keeps clear of the",
                        "ground and the leaves, lets go at the best moment and swings on while G is held. Off: the manual swing, aimed",
                        "with the crosshair. The Web Assist key (J) switches it too.")
                .translation("config.enhanced.web_assist")
                .define("webAssist", true);
        WEB_ASSIST_PREVIEW = BUILDER.comment("Show where Web Assist's next web would go (a small white web) while you are in the air or sneaking.")
                .translation("config.enhanced.web_assist_preview")
                .define("webAssistPreview", true);
        BUILDER.pop();
        // ===== 3.5 music =====
        BUILDER.push("music");
        QUICKSILVER_SONG = BUILDER.comment(
                        "What Quicksilver's Walkman plays: theme (his own song, Silver Hours) or sweet_dreams.",
                        "\"Sweet Dreams (Are Made of This)\" is no longer built in: for sweet_dreams, put your own",
                        "copy in config/enhanced/music as sweet_dreams.mp3 (or .ogg or .wav); without one he plays",
                        "Silver Hours. A song of your own there, or the world's, plays instead of his theme.")
                .translation("config.enhanced.quicksilver_song")
                // (A list that may be asked whether it holds null, which List.of may not: a missing value is checked that way.)
                .defineInList("quicksilverSong", "theme", java.util.Arrays.asList("theme", "sweet_dreams"));
        IGNORE_SERVER_MUSIC = BUILDER.comment("Don't download or play the songs a server or world puts in its music folder",
                        "(<world>/serverconfig/enhanced/music); hear the built-in music, or your own, instead.")
                .translation("config.enhanced.ignore_server_music")
                .define("ignoreServerMusic", false);
        BUILDER.pop();

        // 3.6 (lane L7): the Hulk's own body.
        BUILDER.push("hulk");
        HULK_MODEL = BUILDER.comment("Whether the Hulk is drawn with his own giant body (false: the player model",
                "wearing his skin, if another mod's model clashes with it).")
                .define("hulkModel", true);
        BUILDER.pop();

        // 3.7 (lane L3): Deadpool's jokes.
        BUILDER.push("deadpool");
        DEADPOOL_LINES = BUILDER.comment("Whether Deadpool's joke lines are shown (the speech bubbles over his head",
                "and, when you are Deadpool, the action bar).")
                .define("deadpoolLines", true);
        BUILDER.pop();
        // 3.7 (lane L8): the meeting with Death.
        BUILDER.push("death_meeting");
        SHOW_DEATH_MEETING = BUILDER.comment("When you die, meet Death (Rio, the Green Witch) and choose where to",
                        "start again. Off: the plain vanilla death screen.")
                .translation("config.enhanced.show_death_meeting")
                .define("showDeathMeeting", true);
        BUILDER.pop();
        // 3.8 (lanes L3 and L8): Iron Man's suit-up and helmet, Eternity's realm.
        BUILDER.push("iron_man");
        SUIT_UP_OVERLAY = BUILDER.comment("Whether the faceplate closes over the screen's edges when your suit goes on",
                        "(with reduced motion: a short fade).")
                .define("suitUpOverlay", true);
        IRON_HELMET_HUD = BUILDER.comment("Whether a suited Iron Man sees the helmet's frame in first person",
                        "(blue arcs, altitude and speed in flight).")
                .define("ironHelmetHud", true);
        BUILDER.pop();
        BUILDER.push("eternity");
        ETERNITY_REFLECTIONS = BUILDER.comment("Whether Eternity and the players are mirrored in the realm's",
                        "still water (off for slow computers).")
                .define("eternityReflections", true);
        ETERNITY_FOG = BUILDER.comment("Whether the white mist lies on the horizon of Eternity's realm.")
                .define("eternityFog", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Whether the hero animation runs at all (true until the config has loaded). */
    public static boolean animations() {
        return !SPEC.isLoaded() || ANIMATIONS.get();
    }

    public static boolean reducedMotion() {
        return SPEC.isLoaded() && REDUCED_MOTION.get();
    }

    public static boolean flightEffects() {
        return !SPEC.isLoaded() || FLIGHT_EFFECTS.get();
    }

    /** Whether heroes are drawn without their armour (true until the config has loaded). */
    public static boolean hideHeroArmour() {
        return !SPEC.isLoaded() || HIDE_HERO_ARMOUR.get();
    }

    /** Whether the fight against Thanos plays its music (true until the config has loaded). */
    public static boolean bossMusic() {
        return !SPEC.isLoaded() || BOSS_MUSIC.get();
    }

    /** Whether the fog and the dark edges of the screen mark a Thanos near (true until the config has loaded). */
    public static boolean bossAtmosphere() {
        return !SPEC.isLoaded() || BOSS_ATMOSPHERE.get();
    }

    /** Whether build mode shows its strip of mouse and key pictures (true until the config has loaded). */
    public static boolean buildHints() {
        return !SPEC.isLoaded() || BUILD_HINTS.get();
    }

    /** Whether the little animated tips show the first times a spell is used (true until the config has loaded). */
    public static boolean buildTips() {
        return !SPEC.isLoaded() || BUILD_TIPS.get();
    }

    /** The most blocks a spell's ghost draws one by one (32768 until the config has loaded). */
    public static int buildGhostDetail() {
        return SPEC.isLoaded() ? BUILD_GHOST_DETAIL.get() : 32768;
    }

    /** Whether the box shows faintly through hills and walls (true until the config has loaded). */
    public static boolean buildSeeThrough() {
        return !SPEC.isLoaded() || BUILD_SEE_THROUGH.get();
    }

    /** Whether the boxes of other builders near you are drawn (true until the config has loaded). */
    public static boolean buildShowOthers() {
        return !SPEC.isLoaded() || BUILD_SHOW_OTHERS.get();
    }

    /** Whether Spider-Man's own swing is assisted (true until the config has loaded). */
    public static boolean webAssist() {
        return !SPEC.isLoaded() || WEB_ASSIST.get();
    }

    /** Switches Web Assist and saves it (nothing before the config has loaded). */
    public static void setWebAssist(boolean on) {
        if (SPEC.isLoaded()) {
            WEB_ASSIST.set(on);
            WEB_ASSIST.save();
        }
    }

    /** Whether the next anchor of Web Assist is shown (true until the config has loaded). */
    public static boolean webAssistPreview() {
        return !SPEC.isLoaded() || WEB_ASSIST_PREVIEW.get();
    }

    /**
     * Whether the player chose quicksilverSong = sweet_dreams (false until the config has loaded): his own copy of
     * "Sweet Dreams" in config/enhanced/music plays for Quicksilver first; without one, Quicksilver's own song (3.8.4).
     */
    public static boolean sweetDreams() {
        return SPEC.isLoaded() && "sweet_dreams".equals(QUICKSILVER_SONG.get());
    }

    /** Whether the world's music is ignored (false until the config has loaded). */
    public static boolean ignoreServerMusic() {
        return SPEC.isLoaded() && IGNORE_SERVER_MUSIC.get();
    }

    /** Whether the Hulk is drawn with his own body (true until the config has loaded). */
    public static boolean hulkModel() {
        return !SPEC.isLoaded() || HULK_MODEL.get();
    }

    /** Whether Deadpool's joke lines are shown (true until the config has loaded). */
    public static boolean deadpoolLines() {
        return !SPEC.isLoaded() || DEADPOOL_LINES.get();
    }

    /** Whether a death shows the meeting with Death (true until the config has loaded). */
    public static boolean showDeathMeeting() {
        return !SPEC.isLoaded() || SHOW_DEATH_MEETING.get();
    }

    /** Whether the faceplate overlay plays when a suit goes on (true until the config has loaded). */
    public static boolean suitUpOverlay() {
        return !SPEC.isLoaded() || SUIT_UP_OVERLAY.get();
    }

    /** Whether a suited Iron Man sees the helmet's frame in first person (true until the config has loaded). */
    public static boolean ironHelmetHud() {
        return !SPEC.isLoaded() || IRON_HELMET_HUD.get();
    }

    /** Whether the realm of Eternity draws its reflections (true until the config has loaded). */
    public static boolean eternityReflections() {
        return !SPEC.isLoaded() || ETERNITY_REFLECTIONS.get();
    }

    /** Whether the realm of Eternity has its white mist (true until the config has loaded). */
    public static boolean eternityFog() {
        return !SPEC.isLoaded() || ETERNITY_FOG.get();
    }

    private EnhancedClientConfig() {
    }
}
