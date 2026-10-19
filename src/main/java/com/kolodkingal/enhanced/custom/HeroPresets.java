package com.kolodkingal.enhanced.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.power.Power;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Hero Creator's presets (3.4): the heroes to start from on the Start tab, one "like" each built-in hero and six
 * archetypes. Each is a hero file of format 2 in {@code assets/enhanced/hero_presets/<id>.json} with a {@code "preset"}
 * block ({@code "icon"}: the item drawn on an archetype's card; {@code "hero"}: the built-in hero it is like, whose face
 * its card shows). Named {@code preset.enhanced.<id>} (and {@code .desc}). Each fits a budget of 60 points, so a world
 * whose config still has 3.3's budget can use all of them.
 */
public final class HeroPresets {
    /** The heroes' presets, in the order of the heroes. */
    public static final List<String> HEROES = List.of("magneto", "wanda", "quicksilver", "vision", "billy", "tommy", "loki", "thor",
            "phoenix", "spider", "thanos", "ultron", "apocalypse", "agatha", "cyclops", "xavier",
            // 3.5 (one per lane: the heroes of Asgard)
            "heimdall", "odin", "hela",
            // 3.6 (lane L7)
            "hulk");
    /** The archetypes. */
    public static final List<String> ARCHETYPES = List.of("speedster", "flyer_blaster", "tank", "mage", "mind_master", "elemental");
    /** The most points a preset may cost. */
    public static final int MAX_POINTS = 60;

    /**
     * One preset.
     *
     * @param id   its id (the file's name)
     * @param hero the hero it makes
     * @param icon the item of its card (an item id), or empty
     * @param like the built-in hero it is like, or {@link Power#NONE} for an archetype
     */
    public record Preset(String id, CustomHero hero, String icon, Power like) {
        public String nameKey() {
            return "preset.enhanced." + id;
        }

        public String descKey() {
            return nameKey() + ".desc";
        }
    }

    /** Every preset's id: the heroes', then the archetypes. */
    public static List<String> ids() {
        return java.util.stream.Stream.concat(HEROES.stream(), ARCHETYPES.stream()).toList();
    }

    /** The resource path of a preset (under {@code assets/enhanced/}). */
    public static String path(String id) {
        return "hero_presets/" + id + ".json";
    }

    /** A preset read from its file's text, or null when it is not a hero. */
    @Nullable
    public static Preset read(String id, String json) {
        CustomHeroJson.Read read = CustomHeroJson.read(json);
        if (read.hero() == null) {
            return null;
        }
        String icon = "";
        Power like = Power.NONE;
        try {
            JsonElement meta = JsonParser.parseString(json).getAsJsonObject().get("preset");
            if (meta instanceof JsonObject m) {
                icon = m.has("icon") ? m.get("icon").getAsString() : "";
                Power p = m.has("hero") ? Power.byName(m.get("hero").getAsString()) : null;
                like = p == null ? Power.NONE : p;
            }
        } catch (RuntimeException e) {
            // (a preset without a readable block is still a hero)
        }
        return new Preset(id, read.hero(), icon, like);
    }

    private HeroPresets() {
    }
}
