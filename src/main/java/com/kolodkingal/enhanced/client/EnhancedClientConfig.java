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

    private EnhancedClientConfig() {
    }
}
